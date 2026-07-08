package com.lecturelens.processing.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.lecturelens.core.WorkerKeys;

/**
 * PLACEHOLDER — owned by Track 4 (Muhammad). Track 3 ships this thin stub only
 * so {@code PipelineOrchestrator} has a real {@link Worker} class to chain and
 * the record → enqueue path is demonstrable before the cloud work lands.
 *
 * <p>Contract Track 3 depends on (do not change without a joint PR):
 * <ul>
 *   <li>Reads {@link WorkerKeys#KEY_LECTURE_ID} and {@link WorkerKeys#KEY_AUDIO_PATH}
 *       (plus optional {@link WorkerKeys#KEY_LANGUAGE}) from input {@link Data}.</li>
 *   <li>Forwards {@code KEY_LECTURE_ID} in its output so the next stage receives it.</li>
 *   <li>On failure, returns {@link Result#failure(Data)} with
 *       {@link WorkerKeys#KEY_ERROR_MSG} set.</li>
 * </ul>
 *
 * <p>Real implementation: Cloud Speech-to-Text v2 longRunningRecognize + LRO
 * polling, persist transcript/segments on {@code AppExecutors.diskIO()},
 * update {@code lectures.status} TRANSCRIBING → TRANSCRIBED (WORK_BREAKDOWN §4).
 */
public class TranscribeWorker extends Worker {

    public TranscribeWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long lectureId = getInputData().getLong(WorkerKeys.KEY_LECTURE_ID, -1L);
        String audioPath = getInputData().getString(WorkerKeys.KEY_AUDIO_PATH);
        if (lectureId < 0 || audioPath == null) {
            return Result.failure(new Data.Builder()
                    .putString(WorkerKeys.KEY_ERROR_MSG, "Missing lectureId/audioPath")
                    .build());
        }

        // TODO(Track 4): real transcription. Stub succeeds immediately.
        setProgressAsync(new Data.Builder().putInt(WorkerKeys.PROGRESS_PERCENT, 100).build());

        Data output = new Data.Builder()
                .putLong(WorkerKeys.KEY_LECTURE_ID, lectureId)
                .build();
        return Result.success(output);
    }
}
