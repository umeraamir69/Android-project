package com.lecturelens.core;

import androidx.annotation.NonNull;

/**
 * FROZEN Day 0 contract.
 *
 * Screen-level state observed by Fragments:
 * {@code viewModel.getUiState().observe(getViewLifecycleOwner(), this::render)}.
 * Mirrors {@link Result} but is UI-facing (Error carries only a displayable
 * message, never a Throwable).
 */
public abstract class UiState<T> {

    private UiState() {
    }

    public static final class Loading<T> extends UiState<T> {
        private Loading() {
        }
    }

    public static final class Success<T> extends UiState<T> {
        @NonNull public final T data;

        private Success(@NonNull T data) {
            this.data = data;
        }
    }

    public static final class Error<T> extends UiState<T> {
        @NonNull public final String message;

        private Error(@NonNull String message) {
            this.message = message;
        }
    }

    @NonNull
    public static <T> UiState<T> loading() {
        return new Loading<>();
    }

    @NonNull
    public static <T> UiState<T> success(@NonNull T data) {
        return new Success<>(data);
    }

    @NonNull
    public static <T> UiState<T> error(@NonNull String message) {
        return new Error<>(message);
    }
}
