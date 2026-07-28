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
import com.lecturelens.processing.worker.EmbeddingsWorker;
import com.lecturelens.processing.worker.SummarizeWorker;
import com.lecturelens.processing.worker.TranscribeWorker;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Track 3 — builds and enqueues the processing chain for one lecture:
 * {@code TranscribeWorker → SummarizeWorker → EmbeddingsWorker (stretch)}.
 *
 * <p>Stage contract is via {@link WorkerKeys} {@link Data} (frozen Day 0, shared
 * with Track 4). The chain is enqueued as <b>unique work per lecture</b> with
 * {@link ExistingWorkPolicy#REPLACE}, so re-recording or a Retry supersedes any
 * in-flight run for that lecture rather than duplicating it.
 *
 * <p>Cloud stages require connectivity ({@link NetworkType#CONNECTED}); WorkManager
 * parks the chain until the network returns, matching the "wait [offline]" edge in
 * {@code diagrams/01_lecture_processing_state.puml}.
 */
@Singleton
public class PipelineOrchestrator {

    /** Unique-work name prefix; the lecture id is appended. */
    public static final String UNIQUE_NAME_PREFIX = "pipeline_lecture_";

    /** Default STT language when the caller doesn't specify one. */
    public static final String DEFAULT_LANGUAGE = "en-US";

    private final WorkManager workManager;

    /** Stretch feature — off in the MVP (EmbeddingRepository is a no-op stub). */
    private final boolean embeddingsEnabled;

    @Inject
    public PipelineOrchestrator(@NonNull WorkManager workManager) {
        this(workManager, false);
    }

    @VisibleForTesting
    public PipelineOrchestrator(@NonNull WorkManager workManager, boolean embeddingsEnabled) {
        this.workManager = workManager;
        this.embeddingsEnabled = embeddingsEnabled;
    }

    /**
     * Enqueue the transcription → summarization (→ embeddings) chain for a lecture
     * whose audio has been saved.
     *
     * @param lectureId row id returned by {@code LectureRepository.insert(...)}.
     * @param audioPath absolute path of the saved audio file.
     * @param language  BCP-47 code, or {@code null} for {@link #DEFAULT_LANGUAGE}.
     */
    public void enqueue(long lectureId, @NonNull String audioPath, @Nullable String language) {
        // Kill any stalled / retrying chain for this lecture before starting fresh.
        cancelLecture(lectureId);

        Data input = buildTranscribeInput(lectureId, audioPath, language);
        Constraints network = networkConstraints();

        OneTimeWorkRequest transcribe = new OneTimeWorkRequest.Builder(TranscribeWorker.class)
                .setInputData(input)
                .setConstraints(network)
                .addTag("transcribe")
                .build();

        // Explicit lectureId so Summarize still works even if chain output is empty.
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

        if (embeddingsEnabled) {
            chain = chain.then(new OneTimeWorkRequest.Builder(EmbeddingsWorker.class)
                    .setConstraints(network)
                    .build());
        }

        chain.enqueue();
    }

    /**
     * Notes-only retry when transcription already succeeded — avoids burning STT
     * quota and replaying a finished transcribe stage.
     */
    public void enqueueSummarizeOnly(long lectureId) {
        cancelLecture(lectureId);
        Constraints network = networkConstraints();
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

    /** Cancel in-flight / queued pipeline work for one lecture. */
    public void cancelLecture(long lectureId) {
        workManager.cancelUniqueWork(uniqueName(lectureId));
    }

    /** Cancel every pipeline job (e.g. after rotating the API key). */
    public void cancelAll() {
        workManager.cancelAllWorkByTag("transcribe");
        workManager.cancelAllWorkByTag("summarize");
    }

    /** Unique-work name for a lecture. Package-visible for tests. */
    @NonNull
    public static String uniqueName(long lectureId) {
        return UNIQUE_NAME_PREFIX + lectureId;
    }

    /** Input {@link Data} for the first (transcribe) stage. Package-visible for tests. */
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
    private static Constraints networkConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }
}
