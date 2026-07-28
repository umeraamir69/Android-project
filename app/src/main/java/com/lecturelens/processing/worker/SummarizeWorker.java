package com.lecturelens.processing.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.lecturelens.core.WorkerKeys;
import com.lecturelens.di.WorkerEntryPoint;
import com.lecturelens.domain.usecase.GenerateNotesUseCase;

import dagger.hilt.android.EntryPointAccessors;

/**
 * Track 4 — Gemini summarization worker chained after {@link TranscribeWorker}.
 */
public class SummarizeWorker extends Worker {

    public SummarizeWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public androidx.work.ListenableWorker.Result doWork() {
        long lectureId = getInputData().getLong(WorkerKeys.KEY_LECTURE_ID, -1L);
        if (lectureId < 0) {
            return WorkerResultMapper.failure("Missing lectureId");
        }

        setProgressAsync(new Data.Builder().putInt(WorkerKeys.PROGRESS_PERCENT, 10).build());

        GenerateNotesUseCase useCase = EntryPointAccessors.fromApplication(
                getApplicationContext(), WorkerEntryPoint.class).generateNotesUseCase();

        com.lecturelens.core.Result<com.lecturelens.domain.model.Notes> result = useCase.execute(lectureId);

        setProgressAsync(new Data.Builder().putInt(WorkerKeys.PROGRESS_PERCENT, 100).build());
        androidx.work.ListenableWorker.Result mapped =
                WorkerResultMapper.fromDomainResult(lectureId, result);
        // At most one automatic retry for transient 5xx — never loop on quota.
        if (mapped instanceof androidx.work.ListenableWorker.Result.Retry
                && getRunAttemptCount() >= 1) {
            return WorkerResultMapper.failure(
                    "Notes generation failed after a retry. Tap Retry notes.");
        }
        return mapped;
    }
}
