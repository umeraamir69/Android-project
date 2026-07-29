package com.lecturelens.processing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;

import com.lecturelens.core.WorkerKeys;
import com.lecturelens.data.prefs.UserSettingsStore;
import com.lecturelens.processing.worker.EmbeddingsWorker;
import com.lecturelens.processing.worker.SummarizeWorker;
import com.lecturelens.processing.worker.TranscribeWorker;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Track 3 — builds and enqueues the processing chain for one lecture:
 * {@code TranscribeWorker → SummarizeWorker → EmbeddingsWorker (stretch)}.
 */
@Singleton
public class PipelineOrchestrator {

    public static final String UNIQUE_NAME_PREFIX = "pipeline_lecture_";
    public static final String DEFAULT_LANGUAGE = "en-US";

    private final WorkManager workManager;
    private final UserSettingsStore userSettings;
    private final boolean embeddingsEnabled;

    @Inject
    public PipelineOrchestrator(@NonNull WorkManager workManager,
                                @NonNull UserSettingsStore userSettings) {
        this(workManager, userSettings, true);
    }

    @VisibleForTesting
    public PipelineOrchestrator(@NonNull WorkManager workManager,
                                @NonNull UserSettingsStore userSettings,
                                boolean embeddingsEnabled) {
        this.workManager = workManager;
        this.userSettings = userSettings;
        this.embeddingsEnabled = embeddingsEnabled;
    }

    public void enqueue(long lectureId, @NonNull String audioPath, @Nullable String language) {
        cancelLecture(lectureId);

        Data input = buildTranscribeInput(lectureId, audioPath, language);
        Constraints network = pipelineConstraints();
        boolean cloudEmbeddings = embeddingsEnabled && !isOnDevicePreferred();

        OneTimeWorkRequest transcribe = new OneTimeWorkRequest.Builder(TranscribeWorker.class)
                .setInputData(input)
                .setConstraints(network)
                .addTag("transcribe")
                .build();

        Data summarizeInput = new Data.Builder()
                .putLong(WorkerKeys.KEY_LECTURE_ID, lectureId)
                .build();
        OneTimeWorkRequest summarize = new OneTimeWorkRequest.Builder(SummarizeWorker.class)
                .setInputData(summarizeInput)
                .setConstraints(network)
                .addTag("summarize")
                .build();

        WorkContinuation chain = workManager
                .beginUniqueWork(uniqueName(lectureId), ExistingWorkPolicy.REPLACE, transcribe)
                .then(summarize);

        if (cloudEmbeddings) {
            Data embedInput = new Data.Builder()
                    .putLong(WorkerKeys.KEY_LECTURE_ID, lectureId)
                    .build();
            chain = chain.then(new OneTimeWorkRequest.Builder(EmbeddingsWorker.class)
                    .setInputData(embedInput)
                    .setConstraints(new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .addTag("embeddings")
                    .build());
        }

        chain.enqueue();
    }

    public void enqueueSummarizeOnly(long lectureId) {
        cancelLecture(lectureId);
        Constraints network = pipelineConstraints();
        Data summarizeInput = new Data.Builder()
                .putLong(WorkerKeys.KEY_LECTURE_ID, lectureId)
                .build();
        OneTimeWorkRequest summarize = new OneTimeWorkRequest.Builder(SummarizeWorker.class)
                .setInputData(summarizeInput)
                .setConstraints(network)
                .addTag("summarize")
                .build();
        workManager.enqueueUniqueWork(
                uniqueName(lectureId),
                ExistingWorkPolicy.REPLACE,
                summarize);
    }

    public void cancelLecture(long lectureId) {
        workManager.cancelUniqueWork(uniqueName(lectureId));
    }

    public void cancelAll() {
        workManager.cancelAllWorkByTag("transcribe");
        workManager.cancelAllWorkByTag("summarize");
        workManager.cancelAllWorkByTag("embeddings");
    }

    @NonNull
    public static String uniqueName(long lectureId) {
        return UNIQUE_NAME_PREFIX + lectureId;
    }

    @NonNull
    @VisibleForTesting
    static Data buildTranscribeInput(long lectureId, @NonNull String audioPath,
                                     @Nullable String language) {
        return new Data.Builder()
                .putLong(WorkerKeys.KEY_LECTURE_ID, lectureId)
                .putString(WorkerKeys.KEY_AUDIO_PATH, audioPath)
                .putString(WorkerKeys.KEY_LANGUAGE, language != null ? language : DEFAULT_LANGUAGE)
                .build();
    }

    @NonNull
    private Constraints pipelineConstraints() {
        Constraints.Builder builder = new Constraints.Builder();
        if (!isOnDevicePreferred()) {
            builder.setRequiredNetworkType(NetworkType.CONNECTED);
        }
        return builder.build();
    }

    private boolean isOnDevicePreferred() {
        return UserSettingsStore.MODE_ON_DEVICE.equals(userSettings.getProcessingMode());
    }
}
