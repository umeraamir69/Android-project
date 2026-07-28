package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;

/**
 * Ask Gemini questions grounded only in a lecture's notes + transcript.
 */
public interface NotesQaRepository {

    interface Callback {
        void onAnswer(@NonNull String answer);

        void onError(@NonNull String message);
    }

    /**
     * Blocking — call from a background thread.
     * Answers must come only from this lecture's material.
     */
    void ask(long lectureId, @NonNull String question, @NonNull Callback callback);
}
