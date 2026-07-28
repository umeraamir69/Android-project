package com.lecturelens.domain.model;

import androidx.annotation.NonNull;

/** FROZEN Day 0 contract — speakerTag added for STT diarization (0 = unknown). */
public class TranscriptSegment {

    private final long id;
    private final long lectureId;
    private final long startMs;
    private final long endMs;
    @NonNull private final String text;
    /** Google STT speaker tag; 0 when diarization did not assign a speaker. */
    private final int speakerTag;

    public TranscriptSegment(long id,
                             long lectureId,
                             long startMs,
                             long endMs,
                             @NonNull String text) {
        this(id, lectureId, startMs, endMs, text, 0);
    }

    public TranscriptSegment(long id,
                             long lectureId,
                             long startMs,
                             long endMs,
                             @NonNull String text,
                             int speakerTag) {
        this.id = id;
        this.lectureId = lectureId;
        this.startMs = startMs;
        this.endMs = endMs;
        this.text = text;
        this.speakerTag = Math.max(0, speakerTag);
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

    public int getSpeakerTag() {
        return speakerTag;
    }
}
