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
    @Nullable public final String ownerName;
    @Nullable public final String university;
    @Nullable public final String professor;
    @NonNull public final List<SharedHandout> handouts;
    public final long createdAtMs;

    public SharedNotesPacket(@NonNull String shareCode,
                             @NonNull String title,
                             @NonNull String summary,
                             @NonNull List<String> keyTerms,
                             @NonNull List<String> actionItems,
                             @NonNull String transcript,
                             @Nullable String ownerEmail,
                             long createdAtMs) {
        this(shareCode, title, summary, keyTerms, actionItems, transcript,
                ownerEmail, null, null, null, Collections.emptyList(), createdAtMs);
    }

    public SharedNotesPacket(@NonNull String shareCode,
                             @NonNull String title,
                             @NonNull String summary,
                             @NonNull List<String> keyTerms,
                             @NonNull List<String> actionItems,
                             @NonNull String transcript,
                             @Nullable String ownerEmail,
                             @Nullable String ownerName,
                             @Nullable String university,
                             @Nullable String professor,
                             long createdAtMs) {
        this(shareCode, title, summary, keyTerms, actionItems, transcript,
                ownerEmail, ownerName, university, professor,
                Collections.emptyList(), createdAtMs);
    }

    public SharedNotesPacket(@NonNull String shareCode,
                             @NonNull String title,
                             @NonNull String summary,
                             @NonNull List<String> keyTerms,
                             @NonNull List<String> actionItems,
                             @NonNull String transcript,
                             @Nullable String ownerEmail,
                             @Nullable String ownerName,
                             @Nullable String university,
                             @Nullable String professor,
                             @Nullable List<SharedHandout> handouts,
                             long createdAtMs) {
        this.shareCode = shareCode;
        this.title = title;
        this.summary = summary;
        this.keyTerms = List.copyOf(keyTerms);
        this.actionItems = List.copyOf(actionItems);
        this.transcript = transcript;
        this.ownerEmail = ownerEmail;
        this.ownerName = ownerName;
        this.university = university;
        this.professor = professor;
        this.handouts = handouts != null
                ? List.copyOf(handouts)
                : Collections.emptyList();
        this.createdAtMs = createdAtMs;
    }

    @NonNull
    public static SharedNotesPacket empty(@NonNull String code) {
        return new SharedNotesPacket(
                code, "", "", Collections.emptyList(), Collections.emptyList(),
                "", null, 0L);
    }
}
