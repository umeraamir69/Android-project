package com.lecturelens.data.repository;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.lecturelens.core.Result;
import com.lecturelens.data.audio.AudioToLinear16;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.remote.ApiKeyProvider;
import com.lecturelens.data.remote.GcsAudioUploader;
import com.lecturelens.data.remote.PipelineErrorStore;
import com.lecturelens.data.remote.RemoteCallHelper;
import com.lecturelens.data.remote.SpeechToTextService;
import com.lecturelens.data.remote.SttResponseMapper;
import com.lecturelens.data.remote.dto.SttAudioContent;
import com.lecturelens.data.remote.dto.SttOperation;
import com.lecturelens.data.remote.dto.SttRecognizeRequest;
import com.lecturelens.data.remote.dto.SttRecognizeResponse;
import com.lecturelens.data.remote.dto.SttRecognitionConfig;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.Transcript;
import com.lecturelens.domain.model.TranscriptSegment;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.TranscriptionRepository;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Track 4 — Google Cloud Speech-to-Text v1 + Room persistence.
 *
 * <p>Long lectures beyond the ~60s / 10MB inline sync cap are handled by:
 * <ol>
 *   <li>GCS upload + {@code longrunningrecognize} when {@code GCS_BUCKET} +
 *       {@code GCS_OAUTH_TOKEN} are set in local.properties</li>
 *   <li>Otherwise: chunked sync {@code recognize} (~55s PCM slices) with
 *       timestamp stitching — works with API-key-only testing builds</li>
 * </ol>
 */
@Singleton
public class TranscriptionRepositoryImpl implements TranscriptionRepository {

    private static final String TAG = "TranscriptionRepo";
    private static final long MAX_INLINE_BYTES = 10L * 1024L * 1024L;
    /** Sync recognize supports ~60s; keep a cushion under the limit. */
    private static final long SYNC_CHUNK_MS = 55_000L;
    private static final int LRO_POLL_MAX = 90;
    private static final String EMPTY_SPEECH_MSG =
            "No speech detected in the recording. On the emulator: "
                    + "Extended Controls (⋯) → Microphone → enable host audio input, "
                    + "then record again while speaking.";

    private final SpeechToTextService speechService;
    private final ApiKeyProvider apiKeys;
    private final TranscriptDao transcriptDao;
    private final LectureDao lectureDao;
    private final LectureRepository lectureRepository;
    private final TranscriptEntityMapper mapper;
    private final PipelineErrorStore errorStore;
    private final GcsAudioUploader gcsUploader;

    @Inject
    public TranscriptionRepositoryImpl(@NonNull SpeechToTextService speechService,
                                       @NonNull ApiKeyProvider apiKeys,
                                       @NonNull TranscriptDao transcriptDao,
                                       @NonNull LectureDao lectureDao,
                                       @NonNull LectureRepository lectureRepository,
                                       @NonNull TranscriptEntityMapper mapper,
                                       @NonNull PipelineErrorStore errorStore,
                                       @NonNull GcsAudioUploader gcsUploader) {
        this.speechService = speechService;
        this.apiKeys = apiKeys;
        this.transcriptDao = transcriptDao;
        this.lectureDao = lectureDao;
        this.lectureRepository = lectureRepository;
        this.mapper = mapper;
        this.errorStore = errorStore;
        this.gcsUploader = gcsUploader;
    }

    @NonNull
    @Override
    public Result<Transcript> transcribe(@NonNull File audio, @NonNull String languageCode) {
        if (!apiKeys.hasSpeechToTextKey()) {
            return fail(-1L, "Speech-to-Text API key is missing. Add STT_API_KEY to local.properties.");
        }
        if (!audio.exists() || audio.length() == 0L) {
            return fail(-1L, "Audio file is missing or empty.");
        }

        long lectureId = lectureDao.findIdByAudioPath(audio.getAbsolutePath());
        if (lectureId <= 0L) {
            return fail(-1L, "No lecture row found for this audio file.");
        }

        lectureRepository.updateStatus(lectureId, LectureStatus.TRANSCRIBING);
        errorStore.clear(lectureId);

        try {
            AudioToLinear16.PcmAudio pcm = AudioToLinear16.convert(audio);
            if (isNearlySilent(pcm.pcmLittleEndian)) {
                return fail(lectureId, EMPTY_SPEECH_MSG);
            }

            SttRecognitionConfig config = new SttRecognitionConfig(
                    "LINEAR16", pcm.sampleRateHz, languageCode);
            config.model = ApiKeyProvider.STT_MODEL;

            final String fullText;
            final List<TranscriptSegment> segments;

            boolean needsLongPath = needsLongPath(pcm);
            if (needsLongPath && gcsUploader.isConfigured()) {
                Log.i(TAG, "Long audio → GCS + longRunningRecognize");
                SttRecognizeResponse body = recognizeViaGcs(pcm, config);
                fullText = SttResponseMapper.extractFullText(body);
                segments = SttResponseMapper.toSegments(lectureId, body);
            } else if (needsLongPath) {
                Log.i(TAG, "Long audio → chunked sync recognize (no GCS configured)");
                ChunkedResult chunked = recognizeChunked(lectureId, pcm, config);
                fullText = chunked.fullText;
                segments = chunked.segments;
            } else {
                SttRecognizeResponse body = recognizeInline(pcm.pcmLittleEndian, config);
                fullText = SttResponseMapper.extractFullText(body);
                segments = SttResponseMapper.toSegments(lectureId, body);
            }

            if (fullText == null || fullText.isEmpty()) {
                return fail(lectureId, EMPTY_SPEECH_MSG);
            }

            Transcript transcript = new Transcript(
                    lectureId,
                    fullText,
                    languageCode,
                    ApiKeyProvider.STT_MODEL);

            TranscriptEntity entity = mapper.toEntity(transcript);
            transcriptDao.replaceTranscript(entity, mapper.toSegmentEntities(lectureId, segments));
            lectureRepository.updateStatus(lectureId, LectureStatus.TRANSCRIBED);
            errorStore.clear(lectureId);
            return Result.success(transcript);
        } catch (IOException e) {
            Log.e(TAG, "Transcription I/O error", e);
            return fail(lectureId, e.getMessage() != null
                    ? e.getMessage()
                    : "Could not decode audio for transcription.");
        } catch (Exception e) {
            Log.e(TAG, "Transcription failed", e);
            return fail(lectureId, e.getMessage() != null ? e.getMessage() : "Transcription failed.");
        }
    }

    private static boolean needsLongPath(@NonNull AudioToLinear16.PcmAudio pcm) {
        long durationMs = durationMs(pcm);
        String base64 = Base64.encodeToString(pcm.pcmLittleEndian, Base64.NO_WRAP);
        return durationMs > SYNC_CHUNK_MS || base64.length() > MAX_INLINE_BYTES;
    }

    private static long durationMs(@NonNull AudioToLinear16.PcmAudio pcm) {
        if (pcm.sampleRateHz <= 0) {
            return 0L;
        }
        // 16-bit mono → 2 bytes per sample
        long samples = pcm.pcmLittleEndian.length / 2L;
        return samples * 1000L / pcm.sampleRateHz;
    }

    @NonNull
    private SttRecognizeResponse recognizeInline(@NonNull byte[] pcm,
                                                 @NonNull SttRecognitionConfig config)
            throws IOException {
        String base64 = Base64.encodeToString(pcm, Base64.NO_WRAP);
        if (base64.length() > MAX_INLINE_BYTES) {
            throw new IOException("Audio chunk is too large for inline Speech-to-Text.");
        }
        Response<SttRecognizeResponse> response = speechService.recognize(
                apiKeys.getSpeechToTextApiKey(),
                new SttRecognizeRequest(config, base64)).execute();
        return requireRecognizeBody(response);
    }

    @NonNull
    private ChunkedResult recognizeChunked(long lectureId,
                                           @NonNull AudioToLinear16.PcmAudio pcm,
                                           @NonNull SttRecognitionConfig config)
            throws IOException {
        int bytesPerMs = Math.max(1, (pcm.sampleRateHz * 2) / 1000);
        int chunkBytes = (int) (SYNC_CHUNK_MS * bytesPerMs);
        // Align to 2-byte samples
        chunkBytes -= chunkBytes % 2;
        if (chunkBytes < 2) {
            chunkBytes = pcm.pcmLittleEndian.length;
        }

        StringBuilder fullText = new StringBuilder();
        List<TranscriptSegment> allSegments = new ArrayList<>();
        byte[] data = pcm.pcmLittleEndian;
        int offset = 0;
        int chunkIndex = 0;
        while (offset < data.length) {
            int end = Math.min(offset + chunkBytes, data.length);
            if ((end - offset) % 2 != 0) {
                end--;
            }
            if (end <= offset) {
                break;
            }
            byte[] slice = Arrays.copyOfRange(data, offset, end);
            long timeOffsetMs = (offset / 2L) * 1000L / Math.max(1, pcm.sampleRateHz);
            Log.i(TAG, String.format(Locale.US,
                    "STT chunk %d bytes=%d offsetMs=%d", chunkIndex, slice.length, timeOffsetMs));

            SttRecognizeResponse body = recognizeInline(slice, config);
            String piece = SttResponseMapper.extractFullText(body);
            if (!piece.isEmpty()) {
                if (fullText.length() > 0) {
                    fullText.append(' ');
                }
                fullText.append(piece);
            }
            allSegments.addAll(SttResponseMapper.toSegments(lectureId, body, timeOffsetMs));
            offset = end;
            chunkIndex++;
        }
        return new ChunkedResult(fullText.toString().trim(), allSegments);
    }

    @NonNull
    private SttRecognizeResponse recognizeViaGcs(@NonNull AudioToLinear16.PcmAudio pcm,
                                                 @NonNull SttRecognitionConfig config)
            throws IOException, InterruptedException {
        String gsUri = gcsUploader.uploadLinear16(pcm.pcmLittleEndian, pcm.sampleRateHz);
        try {
            SttRecognizeRequest request = new SttRecognizeRequest(
                    config, SttAudioContent.fromUri(gsUri));
            Response<SttOperation> start = speechService.longRunningRecognize(
                    apiKeys.getSpeechToTextApiKey(), request).execute();
            if (!start.isSuccessful()) {
                throw new IOException(friendlySttMessage(
                        start.code(), RemoteCallHelper.readErrorBody(start)));
            }
            SttOperation operation = start.body();
            if (operation == null || operation.name == null || operation.name.isEmpty()) {
                throw new IOException("Speech-to-Text returned an empty long-running operation.");
            }
            String opName = operation.name;
            for (int i = 0; i < LRO_POLL_MAX; i++) {
                if (operation.done) {
                    break;
                }
                TimeUnit.SECONDS.sleep(2);
                Response<SttOperation> poll = speechService.getOperation(
                        opName, apiKeys.getSpeechToTextApiKey()).execute();
                if (!poll.isSuccessful()) {
                    throw new IOException(friendlySttMessage(
                            poll.code(), RemoteCallHelper.readErrorBody(poll)));
                }
                operation = poll.body();
                if (operation == null) {
                    throw new IOException("Speech-to-Text operation poll returned empty.");
                }
            }
            if (!operation.done) {
                throw new IOException("Speech-to-Text timed out waiting for long-running results.");
            }
            if (operation.error != null) {
                String msg = operation.error.message != null
                        ? operation.error.message
                        : "Long-running STT failed.";
                throw new IOException(msg);
            }
            if (operation.response == null) {
                throw new IOException("Speech-to-Text finished with an empty transcript response.");
            }
            return operation.response;
        } finally {
            gcsUploader.deleteQuietly(gsUri);
        }
    }

    @NonNull
    private SttRecognizeResponse requireRecognizeBody(
            @NonNull Response<SttRecognizeResponse> response) throws IOException {
        if (!response.isSuccessful()) {
            String message = RemoteCallHelper.readErrorBody(response);
            Log.e(TAG, "STT HTTP " + response.code() + ": " + message);
            if (RemoteCallHelper.isRetryableHttpCode(response.code())) {
                throw new IOException(RemoteCallHelper.CODE_RETRY
                        + ": STT temporarily unavailable");
            }
            throw new IOException(friendlySttMessage(response.code(), message));
        }
        SttRecognizeResponse body = response.body();
        if (body == null) {
            throw new IOException("Speech-to-Text returned an empty response.");
        }
        return body;
    }

    @NonNull
    private Result<Transcript> fail(long lectureId, @NonNull String message) {
        Log.e(TAG, message);
        if (lectureId > 0L) {
            errorStore.put(lectureId, message);
            lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
        }
        return Result.error(message);
    }

    @NonNull
    private static String friendlySttMessage(int code, @NonNull String raw) {
        String lower = raw.toLowerCase(Locale.US);
        if (code == 400 || lower.contains("invalid") || lower.contains("diarization")) {
            return "Speech-to-Text rejected this audio config. Tap Re-transcribe.";
        }
        if (code == 401 || code == 403) {
            return "STT API key was rejected. Check STT_API_KEY in local.properties.";
        }
        if (code == 429 || lower.contains("quota") || lower.contains("resource_exhausted")) {
            return "Speech-to-Text quota exceeded. Wait a minute, then Re-transcribe.";
        }
        return "Transcription failed (HTTP " + code + "). Tap Re-transcribe.";
    }

    static boolean isNearlySilent(@NonNull byte[] pcmLittleEndian) {
        if (pcmLittleEndian.length < 2) {
            return true;
        }
        ByteBuffer buffer = ByteBuffer.wrap(pcmLittleEndian).order(ByteOrder.LITTLE_ENDIAN);
        int peak = 0;
        while (buffer.remaining() >= 2) {
            peak = Math.max(peak, Math.abs(buffer.getShort()));
        }
        return peak < 500;
    }

    @NonNull
    @Override
    public LiveData<Transcript> observeTranscript(long lectureId) {
        return TranscriptEntityMapper.mapTranscriptLiveData(transcriptDao.observeTranscript(lectureId));
    }

    @NonNull
    @Override
    public LiveData<List<TranscriptSegment>> observeSegments(long lectureId) {
        return TranscriptEntityMapper.mapSegmentsLiveData(transcriptDao.observeSegments(lectureId));
    }

    private static final class ChunkedResult {
        @NonNull final String fullText;
        @NonNull final List<TranscriptSegment> segments;

        ChunkedResult(@NonNull String fullText, @NonNull List<TranscriptSegment> segments) {
            this.fullText = fullText;
            this.segments = segments;
        }
    }
}
