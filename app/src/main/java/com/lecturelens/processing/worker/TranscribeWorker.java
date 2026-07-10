package com.lecturelens.processing.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.lecturelens.core.WorkerKeys;
import com.lecturelens.di.WorkerEntryPoint;
import com.lecturelens.domain.usecase.TranscribeAudioUseCase;
import com.lecturelens.processing.PipelineOrchestrator;

import java.io.File;

import dagger.hilt.android.EntryPointAccessors;

/**
 * Track 4 — Cloud Speech-to-Text worker chained by {@link PipelineOrchestrator}.
 */
public class TranscribeWorker extends Worker {

    public TranscribeWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public androidx.work.ListenableWorker.Result doWork() {
        long lectureId = getInputData().getLong(WorkerKeys.KEY_LECTURE_ID, -1L);
        String audioPath = getInputData().getString(WorkerKeys.KEY_AUDIO_PATH);
        String language = getInputData().getString(WorkerKeys.KEY_LANGUAGE);
        if (lectureId < 0 || audioPath == null) {
            return WorkerResultMapper.failure("Missing lectureId/audioPath");
        }
        if (language == null || language.isEmpty()) {
            language = PipelineOrchestrator.DEFAULT_LANGUAGE;
        }

        setProgressAsync(new Data.Builder().putInt(WorkerKeys.PROGRESS_PERCENT, 10).build());

        TranscribeAudioUseCase useCase = EntryPointAccessors.fromApplication(
                getApplicationContext(), WorkerEntryPoint.class).transcribeAudioUseCase();

        com.lecturelens.core.Result<com.lecturelens.domain.model.Transcript> result = useCase.execute(
                lectureId,
                new File(audioPath),
                language);

        setProgressAsync(new Data.Builder().putInt(WorkerKeys.PROGRESS_PERCENT, 100).build());
        return WorkerResultMapper.fromDomainResult(lectureId, result);
    }
}
