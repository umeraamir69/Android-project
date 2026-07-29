package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.lecturelens.domain.model.ChatMessage;
import com.lecturelens.domain.model.QaAnswer;

import java.util.List;

/**
 * Ask Gemini questions grounded in a lecture's notes + transcript (RAG when indexed).
 * Chat turns are persisted per lecture.
 */
public interface NotesQaRepository {

    interface Callback {
        void onAnswer(@NonNull QaAnswer answer);

        void onError(@NonNull String message);
    }

    void ask(long lectureId, @NonNull String question, @NonNull Callback callback);

    @NonNull
    LiveData<List<ChatMessage>> observeChat(long lectureId);

    void clearChat(long lectureId);
}
