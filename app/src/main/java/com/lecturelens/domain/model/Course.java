package com.lecturelens.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Course with optional professor used for notes attribution. */
public class Course {

    private final long id;
    @NonNull private final String name;
    private final int color;        // ARGB int, rendered by course tag chips
    private final long createdAt;   // epoch millis
    @NonNull private final String professor;

    public Course(long id, @NonNull String name, int color, long createdAt) {
        this(id, name, color, createdAt, "");
    }

    public Course(long id,
                  @NonNull String name,
                  int color,
                  long createdAt,
                  @Nullable String professor) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.createdAt = createdAt;
        this.professor = professor != null ? professor.trim() : "";
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

    @NonNull
    public String getProfessor() {
        return professor;
    }
}
