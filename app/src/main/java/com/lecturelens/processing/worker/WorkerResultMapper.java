package com.lecturelens.processing.worker;

import androidx.annotation.NonNull;

import androidx.work.Data;

import com.lecturelens.core.Result;
import com.lecturelens.core.WorkerKeys;
import com.lecturelens.domain.usecase.RemoteRetryMarkers;

/**
 * Maps domain {@link Result} instances to WorkManager worker results.
 */
public final class WorkerResultMapper {

    private WorkerResultMapper() {
    }

    @NonNull
    public static androidx.work.ListenableWorker.Result failure(@NonNull String message) {
        return androidx.work.ListenableWorker.Result.failure(new Data.Builder()
                .putString(WorkerKeys.KEY_ERROR_MSG, message)
                .build());
    }

    @NonNull
    public static androidx.work.ListenableWorker.Result success(long lectureId) {
        return androidx.work.ListenableWorker.Result.success(new Data.Builder()
                .putLong(WorkerKeys.KEY_LECTURE_ID, lectureId)
                .build());
    }

    @NonNull
    public static <T> androidx.work.ListenableWorker.Result fromDomainResult(
            long lectureId,
            @NonNull Result<T> result) {
        return fromDomainResult(lectureId, result, 0, null);
    }

    /**
     * @param runAttemptCount WorkManager attempt count; when {@code >= 1} and the domain
     *                        result is retryable, returns a permanent failure instead of retry
     *                        (avoids RestrictedApi instanceof checks on Result.Retry).
     * @param exhaustedMessage message used when retries are exhausted; ignored if null
     */
    @NonNull
    public static <T> androidx.work.ListenableWorker.Result fromDomainResult(
            long lectureId,
            @NonNull Result<T> result,
            int runAttemptCount,
            String exhaustedMessage) {
        if (result instanceof Result.Success) {
            return success(lectureId);
        }
        if (result instanceof Result.Error) {
            String message = ((Result.Error<T>) result).message;
            if (message.startsWith(RemoteRetryMarkers.CODE_RETRY)) {
                if (runAttemptCount >= 1) {
                    String failMsg = exhaustedMessage != null
                            ? exhaustedMessage
                            : stripRetryPrefix(message);
                    return failure(failMsg);
                }
                return androidx.work.ListenableWorker.Result.retry();
            }
            return failure(stripRetryPrefix(message));
        }
        return failure("Unexpected in-flight result in worker.");
    }

    @NonNull
    private static String stripRetryPrefix(@NonNull String message) {
        String prefix = RemoteRetryMarkers.CODE_RETRY + ": ";
        if (message.startsWith(prefix)) {
            return message.substring(prefix.length());
        }
        return message;
    }
}
