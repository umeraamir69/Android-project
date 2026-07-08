package com.lecturelens.processing.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.lecturelens.core.WorkerKeys;

/**
 * PLACEHOLDER (stretch) — owned by Track 4. Optional final stage of the chain
 * built by {@code PipelineOrchestrator}; included only when embeddings are
 * enabled. No-op stub for now (EmbeddingRepository is a stretch feature).
 */
public class EmbeddingsWorker extends Worker {

    public EmbeddingsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long lectureId = getInputData().getLong(WorkerKeys.KEY_LECTURE_ID, -1L);
        // TODO(Track 4, stretch): compute + persist embeddings. No-op stub.
        return Result.success(new Data.Builder()
                .putLong(WorkerKeys.KEY_LECTURE_ID, lectureId)
                .build());
    }
}
