package com.lecturelens.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** FROZEN Day 0 contract — field changes require a team sync. */
public class Lecture {

    private final long id;
    private final long courseId;
    @NonNull private final String title;
    private final long date;             // epoch millis
    @Nullable private final String audioPath; // null until recording is saved
    private final long durationMs;
    @NonNull private final LectureStatus status;

    public Lecture(long id,
                   long courseId,
                   @NonNull String title,
                   long date,
                   @Nullable String audioPath,
                   long durationMs,
                   @NonNull LectureStatus status) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.date = date;
        this.audioPath = audioPath;
        this.durationMs = durationMs;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public long getCourseId() {
        return courseId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public long getDate() {
        return date;
    }

    @Nullable
    public String getAudioPath() {
        return audioPath;
    }

    public long getDurationMs() {
        return durationMs;
    }

    @NonNull
    public LectureStatus getStatus() {
        return status;
    }
}
