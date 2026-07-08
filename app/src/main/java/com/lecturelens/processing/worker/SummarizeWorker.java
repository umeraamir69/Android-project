package com.lecturelens.processing.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.lecturelens.core.WorkerKeys;

/**
 * PLACEHOLDER — owned by Track 4 (Muhammad). Chained after {@link TranscribeWorker}
 * by {@code PipelineOrchestrator}. Thin stub so the chain is demonstrable.
 *
 * <p>Real implementation: Gemini map-reduce summarization + key-term/action-item
 * extraction, persist notes on {@code AppExecutors.diskIO()}, update status
 * SUMMARIZING → (INDEXING → READY handled by the stretch EmbeddingsWorker,
 * or READY here when embeddings are skipped).
 */
public class SummarizeWorker extends Worker {

    public SummarizeWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long lectureId = getInputData().getLong(WorkerKeys.KEY_LECTURE_ID, -1L);
        if (lectureId < 0) {
            return Result.failure(new Data.Builder()
                    .putString(WorkerKeys.KEY_ERROR_MSG, "Missing lectureId")
                    .build());
        }

        // TODO(Track 4): real summarization. Stub succeeds immediately.
        setProgressAsync(new Data.Builder().putInt(WorkerKeys.PROGRESS_PERCENT, 100).build());

        return Result.success(new Data.Builder()
                .putLong(WorkerKeys.KEY_LECTURE_ID, lectureId)
                .build());
    }
}
