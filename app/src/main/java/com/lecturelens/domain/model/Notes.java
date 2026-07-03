package com.lecturelens.domain.model;

import androidx.annotation.NonNull;

import java.util.List;

/** FROZEN Day 0 contract — field changes require a team sync. */
public class Notes {

    private final long lectureId;
    @NonNull private final String summary;          // Markdown
    @NonNull private final List<String> keyTerms;
    @NonNull private final List<String> actionItems;

    public Notes(long lectureId,
                 @NonNull String summary,
                 @NonNull List<String> keyTerms,
                 @NonNull List<String> actionItems) {
        this.lectureId = lectureId;
        this.summary = summary;
        this.keyTerms = List.copyOf(keyTerms);
        this.actionItems = List.copyOf(actionItems);
    }

    public long getLectureId() {
        return lectureId;
    }

    @NonNull
    public String getSummary() {
        return summary;
    }

    @NonNull
    public List<String> getKeyTerms() {
        return keyTerms;
    }

    @NonNull
    public List<String> getActionItems() {
        return actionItems;
    }
}
