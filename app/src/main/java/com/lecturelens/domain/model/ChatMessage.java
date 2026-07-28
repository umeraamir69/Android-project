package com.lecturelens.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** One turn in the per-lecture Ask AI chat (user or assistant). */
public final class ChatMessage {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    public final long id;
    public final long lectureId;
    @NonNull public final String role;
    @NonNull public final String text;
    /** JSON array of citation timestamps, or empty. */
    @NonNull public final String citationsJson;
    public final long createdAt;

    public ChatMessage(long id,
                       long lectureId,
                       @NonNull String role,
                       @NonNull String text,
                       @Nullable String citationsJson,
                       long createdAt) {
        this.id = id;
        this.lectureId = lectureId;
        this.role = role;
        this.text = text;
        this.citationsJson = citationsJson != null ? citationsJson : "[]";
        this.createdAt = createdAt;
    }

    public boolean isUser() {
        return ROLE_USER.equals(role);
    }
}
