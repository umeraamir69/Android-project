package com.lecturelens.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Thin logging abstraction (arch doc §5 Telemetry). */
public interface Logger {

    void d(@NonNull String tag, @NonNull String message);

    void i(@NonNull String tag, @NonNull String message);

    void w(@NonNull String tag, @NonNull String message);

    void e(@NonNull String tag, @NonNull String message, @Nullable Throwable t);
}
