package com.lecturelens.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** Notes package shared via Firebase for in-app import by code. */
public final class SharedNotesPacket {

    @NonNull public final String shareCode;
    @NonNull public final String title;
    @NonNull public final String summary;
    @NonNull public final List<String> keyTerms;
    @NonNull public final List<String> actionItems;
    @NonNull public final String transcript;
    @Nullable public final String ownerEmail;
    public final long createdAtMs;

    public SharedNotesPacket(@NonNull String shareCode,
                             @NonNull String title,
                             @NonNull String summary,
                             @NonNull List<String> keyTerms,
                             @NonNull List<String> actionItems,
                             @NonNull String transcript,
                             @Nullable String ownerEmail,
                             long createdAtMs) {
        this.shareCode = shareCode;
        this.title = title;
        this.summary = summary;
        this.keyTerms = List.copyOf(keyTerms);
        this.actionItems = List.copyOf(actionItems);
        this.transcript = transcript;
        this.ownerEmail = ownerEmail;
        this.createdAtMs = createdAtMs;
    }

    @NonNull
    public static SharedNotesPacket empty(@NonNull String code) {
        return new SharedNotesPacket(
                code, "", "", Collections.emptyList(), Collections.emptyList(),
                "", null, 0L);
    }
}
