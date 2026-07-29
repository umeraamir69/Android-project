package com.lecturelens.data.repository;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.lecturelens.core.Result;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.local.entity.TranscriptSegmentEntity;
import com.lecturelens.data.prefs.UserSettingsStore;
import com.lecturelens.data.remote.PipelineErrorStore;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.Transcript;
import com.lecturelens.domain.model.TranscriptSegment;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.TranscriptionRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.content.Intent;
import android.os.Bundle;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Phase 2 router — cloud Google STT vs on-device SpeechRecognizer / offline stub.
 */
@Singleton
public class TranscriptionRouter implements TranscriptionRepository {

    private final TranscriptionRepository cloud;
    private final UserSettingsStore settings;
    private final Context appContext;
    private final LectureDao lectureDao;
    private final TranscriptDao transcriptDao;
    private final LectureRepository lectureRepository;
    private final TranscriptEntityMapper mapper;
    private final PipelineErrorStore errorStore;

    @Inject
    public TranscriptionRouter(@NonNull @Named("cloudStt") TranscriptionRepository cloud,
                               @NonNull UserSettingsStore settings,
                               @ApplicationContext @NonNull Context context,
                               @NonNull LectureDao lectureDao,
                               @NonNull TranscriptDao transcriptDao,
                               @NonNull LectureRepository lectureRepository,
                               @NonNull TranscriptEntityMapper mapper,
                               @NonNull PipelineErrorStore errorStore) {
        this.cloud = cloud;
        this.settings = settings;
        this.appContext = context.getApplicationContext();
        this.lectureDao = lectureDao;
        this.transcriptDao = transcriptDao;
        this.lectureRepository = lectureRepository;
        this.mapper = mapper;
        this.errorStore = errorStore;
    }

    @NonNull
    @Override
    public Result<Transcript> transcribe(@NonNull File audio, @NonNull String languageCode) {
        if (shouldUseOnDevice()) {
            return transcribeOnDevice(audio, languageCode);
        }
        return cloud.transcribe(audio, languageCode);
    }

    @NonNull
    @Override
    public LiveData<Transcript> observeTranscript(long lectureId) {
        return cloud.observeTranscript(lectureId);
    }

    @NonNull
    @Override
    public LiveData<List<TranscriptSegment>> observeSegments(long lectureId) {
        return cloud.observeSegments(lectureId);
    }

    private boolean shouldUseOnDevice() {
        String mode = settings.getProcessingMode();
        if (UserSettingsStore.MODE_ON_DEVICE.equals(mode)) {
            return true;
        }
        if (UserSettingsStore.MODE_AUTO.equals(mode)) {
            return !isOnline();
        }
        return false;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @NonNull
    private Result<Transcript> transcribeOnDevice(@NonNull File audio, @NonNull String languageCode) {
        long lectureId = lectureDao.findIdByAudioPath(audio.getAbsolutePath());
        if (lectureId <= 0L) {
            return Result.error("No lecture row found for this audio file.");
        }
        lectureRepository.updateStatus(lectureId, LectureStatus.TRANSCRIBING);
        errorStore.clear(lectureId);

        long durationMs = probeDurationMs(audio);
        String recognized = trySpeechRecognizer(languageCode);
        String fullText;
        if (recognized != null && !recognized.trim().isEmpty()) {
            fullText = recognized.trim();
        } else {
            fullText = String.format(Locale.US,
                    "[On-device] Privacy mode transcript placeholder for %s (%.0fs). "
                            + "Switch Processing mode to Cloud in Settings for Google STT.",
                    audio.getName(),
                    durationMs / 1000.0);
        }

        List<TranscriptSegment> segments = new ArrayList<>();
        segments.add(new TranscriptSegment(0, lectureId, 0, Math.max(durationMs, 1000L), fullText, 0));

        Transcript transcript = new Transcript(lectureId, fullText, languageCode, "on_device");
        TranscriptEntity entity = mapper.toEntity(transcript);
        List<TranscriptSegmentEntity> segEntities = mapper.toSegmentEntities(lectureId, segments);
        transcriptDao.replaceTranscript(entity, segEntities);
        lectureRepository.updateStatus(lectureId, LectureStatus.TRANSCRIBED);
        errorStore.clear(lectureId);
        return Result.success(transcript);
    }

    @Nullable
    private String trySpeechRecognizer(@NonNull String languageCode) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            return null;
        }
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Handler main = new Handler(Looper.getMainLooper());
        main.post(() -> {
            SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(appContext);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) {
                    recognizer.destroy();
                    latch.countDown();
                }
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> list = results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);
                    if (list != null && !list.isEmpty()) {
                        result.set(list.get(0));
                    }
                    recognizer.destroy();
                    latch.countDown();
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            try {
                recognizer.startListening(intent);
            } catch (Exception e) {
                recognizer.destroy();
                latch.countDown();
            }
        });
        try {
            latch.await(8, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result.get();
    }

    private static long probeDurationMs(@NonNull File audio) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(audio.getAbsolutePath());
            String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) {
                return Long.parseLong(dur);
            }
        } catch (Exception ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
        return Math.max(1000L, audio.length() * 8L / 32L);
    }
}
