package com.lecturelens.core;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** Resolves user-facing strings for ViewModels / repositories. */
@Singleton
public class StringResolver {

    private final Context context;

    @Inject
    public StringResolver(@ApplicationContext @NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    public String get(@StringRes int resId) {
        return context.getString(resId);
    }

    @NonNull
    public String get(@StringRes int resId, @NonNull Object... args) {
        return context.getString(resId, args);
    }

    @NonNull
    public String orDefault(@Nullable String message, @StringRes int fallbackRes) {
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        return get(fallbackRes);
    }
}
