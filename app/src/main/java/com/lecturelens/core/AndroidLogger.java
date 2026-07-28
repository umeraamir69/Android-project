package com.lecturelens.core;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AndroidLogger implements Logger {

    @Inject
    public AndroidLogger() {
    }

    @Override
    public void d(@NonNull String tag, @NonNull String message) {
        Log.d(tag, message);
    }

    @Override
    public void i(@NonNull String tag, @NonNull String message) {
        Log.i(tag, message);
    }

    @Override
    public void w(@NonNull String tag, @NonNull String message) {
        Log.w(tag, message);
    }

    @Override
    public void e(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
        Log.e(tag, message, t);
    }
}
