package com.lecturelens.domain.model;

import androidx.annotation.NonNull;

/** FROZEN Day 0 contract — field changes require a team sync. */
public class Course {

    private final long id;
    @NonNull private final String name;
    private final int color;        // ARGB int, rendered by course tag chips
    private final long createdAt;   // epoch millis

    public Course(long id, @NonNull String name, int color, long createdAt) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
