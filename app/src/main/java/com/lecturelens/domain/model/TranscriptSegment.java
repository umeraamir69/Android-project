package com.lecturelens.domain.model;

import androidx.annotation.NonNull;

/** FROZEN Day 0 contract — field changes require a team sync. */
public class TranscriptSegment {

    private final long id;
    private final long lectureId;
    private final long startMs;
    private final long endMs;
    @NonNull private final String text;

    public TranscriptSegment(long id,
                             long lectureId,
                             long startMs,
                             long endMs,
                             @NonNull String text) {
        this.id = id;
        this.lectureId = lectureId;
        this.startMs = startMs;
        this.endMs = endMs;
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public long getLectureId() {
        return lectureId;
    }

    public long getStartMs() {
        return startMs;
    }

    public long getEndMs() {
        return endMs;
    }

    @NonNull
    public String getText() {
        return text;
    }
}
