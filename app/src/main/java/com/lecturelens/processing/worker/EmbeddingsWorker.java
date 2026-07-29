package com.lecturelens.processing.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.lecturelens.core.WorkerKeys;
import com.lecturelens.di.WorkerEntryPoint;
import com.lecturelens.domain.repository.EmbeddingRepository;

import dagger.hilt.android.EntryPointAccessors;

/** Stretch — indexes lecture chunks for RAG after notes are ready. */
public class EmbeddingsWorker extends Worker {

    public EmbeddingsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long lectureId = getInputData().getLong(WorkerKeys.KEY_LECTURE_ID, -1L);
        if (lectureId < 0) {
            return Result.failure();
        }
        EmbeddingRepository repo = EntryPointAccessors.fromApplication(
                getApplicationContext(), WorkerEntryPoint.class).embeddingRepository();
        com.lecturelens.core.Result<Boolean> indexed = repo.indexLecture(lectureId);
        Data.Builder out = new Data.Builder().putLong(WorkerKeys.KEY_LECTURE_ID, lectureId);
        if (indexed instanceof com.lecturelens.core.Result.Error) {
            // Non-fatal — notes are already READY.
            out.putString(WorkerKeys.KEY_ERROR_MSG,
                    ((com.lecturelens.core.Result.Error<?>) indexed).message);
        }
        return Result.success(out.build());
    }
}
