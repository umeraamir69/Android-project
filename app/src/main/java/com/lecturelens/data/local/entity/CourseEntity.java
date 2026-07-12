package com.lecturelens.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Track 1 — {@code courses} table per arch doc §3.3:
 * id, name, color, created_at.
 */
@Entity(tableName = "courses")
public class CourseEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    /** ARGB int rendered by course tag chips / the course dot. */
    @ColumnInfo(name = "color")
    public int color;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
