package com.lecturelens.core;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Course-rubric AsyncTask wrapper (AsyncTask is deprecated but required by the assignment).
 * Used for DB / network / save work off the main thread.
 */
@SuppressWarnings("deprecation")
public abstract class BgAsyncTask<Result> extends AsyncTask<Void, Void, Result> {

    public interface Callback<Result> {
        void onResult(@Nullable Result result);

        void onError(@NonNull Exception error);
    }

    @Nullable private Exception error;
    @Nullable private final Callback<Result> callback;

    protected BgAsyncTask(@Nullable Callback<Result> callback) {
        this.callback = callback;
    }

    @Nullable
    protected abstract Result runInBackground() throws Exception;

    @Override
    protected final Result doInBackground(Void... voids) {
        try {
            return runInBackground();
        } catch (Exception e) {
            error = e;
            return null;
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        if (callback == null) {
            return;
        }
        if (error != null) {
            callback.onError(error);
        } else {
            callback.onResult(result);
        }
    }

    /** Fire-and-forget helper. */
    public static void run(@NonNull Runnable background) {
        new BgAsyncTask<Void>(null) {
            @Nullable
            @Override
            protected Void runInBackground() {
                background.run();
                return null;
            }
        }.execute();
    }

    public static <T> void run(@NonNull CallableTask<T> background,
                               @NonNull Callback<T> callback) {
        new BgAsyncTask<T>(callback) {
            @Nullable
            @Override
            protected T runInBackground() throws Exception {
                return background.call();
            }
        }.execute();
    }

    public interface CallableTask<T> {
        @Nullable
        T call() throws Exception;
    }
}
