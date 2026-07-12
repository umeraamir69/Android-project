package com.lecturelens.core;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * FROZEN Day 0 contract — the only sanctioned threads outside WorkManager.
 *
 * Threading discipline (see WORK_BREAKDOWN.md "Parallelization risks"):
 * all DB writes go through {@link #diskIO()}, all direct network calls
 * through {@link #networkIO()} (or a WorkManager Worker), and UI updates
 * through {@link #mainThread()} or {@code LiveData.postValue()}.
 *
 * <p>{@link #mainThread()} is created lazily: constructing the Handler needs a
 * Looper, which doesn't exist in JVM unit tests — lazy creation lets tests
 * subclass this with direct executors without touching android.os.
 *
 * Provided as a singleton via {@code di/ExecutorsModule}.
 */
public class AppExecutors {

    private final Executor diskIO;
    private final Executor networkIO;
    private volatile Executor mainThread;

    public AppExecutors() {
        // Single thread => DB writes are serialized, no write races.
        this.diskIO = Executors.newSingleThreadExecutor();
        this.networkIO = Executors.newFixedThreadPool(3);
    }

    @NonNull
    public Executor diskIO() {
        return diskIO;
    }

    @NonNull
    public Executor networkIO() {
        return networkIO;
    }

    @NonNull
    public Executor mainThread() {
        Executor local = mainThread;
        if (local == null) {
            synchronized (this) {
                local = mainThread;
                if (local == null) {
                    local = new MainThreadExecutor();
                    mainThread = local;
                }
            }
        }
        return local;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainHandler.post(command);
        }
    }
}
