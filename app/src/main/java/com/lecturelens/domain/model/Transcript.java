package com.lecturelens.domain.model;

import androidx.annotation.NonNull;

/** FROZEN Day 0 contract — field changes require a team sync. */
public class Transcript {

    private final long lectureId;
    @NonNull private final String fullText;
    @NonNull private final String language;   // BCP-47, e.g. "en-US"
    @NonNull private final String modelUsed;  // e.g. "latest_long"

    public Transcript(long lectureId,
                      @NonNull String fullText,
                      @NonNull String language,
                      @NonNull String modelUsed) {
        this.lectureId = lectureId;
        this.fullText = fullText;
        this.language = language;
        this.modelUsed = modelUsed;
    }

    public long getLectureId() {
        return lectureId;
    }

    @NonNull
    public String getFullText() {
        return fullText;
    }

    @NonNull
    public String getLanguage() {
        return language;
    }

    @NonNull
    public String getModelUsed() {
        return modelUsed;
    }
}
