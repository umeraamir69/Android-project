package com.lecturelens.data.repository;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.lecturelens.core.Result;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.remote.ApiKeyProvider;
import com.lecturelens.data.remote.RemoteCallHelper;
import com.lecturelens.data.remote.SpeechToTextService;
import com.lecturelens.data.remote.SttResponseMapper;
import com.lecturelens.data.remote.dto.SttRecognizeRequest;
import com.lecturelens.data.remote.dto.SttRecognizeResponse;
import com.lecturelens.data.remote.dto.SttRecognitionConfig;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.Transcript;
import com.lecturelens.domain.model.TranscriptSegment;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.TranscriptionRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Track 4 — Google Cloud Speech-to-Text v2 + Room persistence.
 */
@Singleton
public class TranscriptionRepositoryImpl implements TranscriptionRepository {

    private static final long MAX_INLINE_BYTES = 10 * 1024 * 1024; // 10 MB MVP cap

    private final SpeechToTextService speechService;
    private final ApiKeyProvider apiKeys;
    private final TranscriptDao transcriptDao;
    private final LectureDao lectureDao;
    private final LectureRepository lectureRepository;
    private final TranscriptEntityMapper mapper;

    @Inject
    public TranscriptionRepositoryImpl(@NonNull SpeechToTextService speechService,
                                       @NonNull ApiKeyProvider apiKeys,
                                       @NonNull TranscriptDao transcriptDao,
                                       @NonNull LectureDao lectureDao,
                                       @NonNull LectureRepository lectureRepository,
                                       @NonNull TranscriptEntityMapper mapper) {
        this.speechService = speechService;
        this.apiKeys = apiKeys;
        this.transcriptDao = transcriptDao;
        this.lectureDao = lectureDao;
        this.lectureRepository = lectureRepository;
        this.mapper = mapper;
    }

    @NonNull
    @Override
    public Result<Transcript> transcribe(@NonNull File audio, @NonNull String languageCode) {
        if (!apiKeys.hasSpeechToTextKey()) {
            return Result.error("Speech-to-Text API key is missing. Add STT_API_KEY to local.properties.");
        }
        if (!audio.exists() || audio.length() == 0L) {
            return Result.error("Audio file is missing or empty.");
        }
        if (audio.length() > MAX_INLINE_BYTES) {
            return Result.error("Audio file is too large for inline transcription in MVP.");
        }

        long lectureId = lectureDao.findIdByAudioPath(audio.getAbsolutePath());
        if (lectureId <= 0L) {
            return Result.error("No lecture row found for this audio file.");
        }

        lectureRepository.updateStatus(lectureId, LectureStatus.TRANSCRIBING);

        try {
            String base64 = encodeFileBase64(audio);
            SttRecognitionConfig config = SttRecognitionConfig.forLanguage(languageCode);
            config.model = ApiKeyProvider.STT_MODEL;
            SttRecognizeRequest request = new SttRecognizeRequest(config, base64);

            Response<SttRecognizeResponse> response = speechService.recognize(
                    apiKeys.getGcpProjectId(),
                    apiKeys.getSpeechToTextApiKey(),
                    request).execute();

            if (!response.isSuccessful()) {
                String message = RemoteCallHelper.readErrorBody(response);
                if (RemoteCallHelper.isRetryableHttpCode(response.code())) {
                    return Result.error(RemoteCallHelper.CODE_RETRY + ": " + message);
                }
                lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
                return Result.error(message);
            }

            SttRecognizeResponse body = response.body();
            if (body == null) {
                lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
                return Result.error("Speech-to-Text returned an empty response.");
            }

            String fullText = SttResponseMapper.extractFullText(body);
            if (fullText.isEmpty()) {
                lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
                return Result.error("Speech-to-Text returned no transcript text.");
            }

            List<TranscriptSegment> segments = SttResponseMapper.toSegments(lectureId, body);
            Transcript transcript = new Transcript(
                    lectureId,
                    fullText,
                    languageCode,
                    ApiKeyProvider.STT_MODEL);

            TranscriptEntity entity = mapper.toEntity(transcript);
            transcriptDao.replaceTranscript(entity, mapper.toSegmentEntities(lectureId, segments));
            lectureRepository.updateStatus(lectureId, LectureStatus.TRANSCRIBED);
            return Result.success(transcript);
        } catch (IOException e) {
            lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
            return Result.error("Network error during transcription.", e);
        } catch (Exception e) {
            lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
            return Result.error(e.getMessage() != null ? e.getMessage() : "Transcription failed.", e);
        }
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

    @NonNull
    private static String encodeFileBase64(@NonNull File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            int read = in.read(bytes);
            if (read != bytes.length) {
                throw new IOException("Could not read the full audio file.");
            }
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}
