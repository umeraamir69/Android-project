package com.lecturelens.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * FROZEN Day 0 contract — changes require a team sync (see WORK_BREAKDOWN.md).
 *
 * Sealed-style result carrier for every repository/use-case return.
 * Java has no sealed classes at this language level, so the standard
 * abstract-class + private-constructor + static-nested-subclass pattern
 * is used: the only possible subtypes are {@link Success}, {@link Error}
 * and {@link Loading}.
 */
public abstract class Result<T> {

    private Result() {
        // Sealed: only the nested subclasses below can exist.
    }

    /** Operation finished; {@link #data} holds the value. */
    public static final class Success<T> extends Result<T> {
        @NonNull public final T data;

        private Success(@NonNull T data) {
            this.data = data;
        }
    }

    /** Operation failed; {@link #message} is user-displayable via StringResolver later. */
    public static final class Error<T> extends Result<T> {
        @NonNull public final String message;
        @Nullable public final Throwable cause;

        private Error(@NonNull String message, @Nullable Throwable cause) {
            this.message = message;
            this.cause = cause;
        }
    }

    /** Operation in flight. */
    public static final class Loading<T> extends Result<T> {
        private Loading() {
        }
    }

    // ---- Factories ----

    @NonNull
    public static <T> Result<T> success(@NonNull T data) {
        return new Success<>(data);
    }

    @NonNull
    public static <T> Result<T> error(@NonNull String message) {
        return new Error<>(message, null);
    }

    @NonNull
    public static <T> Result<T> error(@NonNull String message, @Nullable Throwable cause) {
        return new Error<>(message, cause);
    }

    @NonNull
    public static <T> Result<T> loading() {
        return new Loading<>();
    }

    // ---- Convenience ----

    public boolean isSuccess() {
        return this instanceof Success;
    }

    public boolean isError() {
        return this instanceof Error;
    }

    public boolean isLoading() {
        return this instanceof Loading;
    }
}
