package com.lecturelens.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * STUB — owned by Track 1 (Zeeshan), who will absorb/align this with the real
 * Room schema (entities + migrations + FTS). Track 3 declares the minimum it
 * needs so {@code LectureWriteRepositoryImpl} + {@code LectureDao} compile and
 * the record → persist path is exercisable before Track 1's DB lands.
 *
 * <p>Columns mirror the frozen {@code domain.model.Lecture} and arch doc §3.3
 * {@code lectures} table: id, course_id (FK), title, date, audio_path,
 * duration_ms, status. {@code status} is stored as the enum name (String) to
 * avoid a TypeConverter in the stub; Track 1 may switch to an int ordinal +
 * converter — a mapper change only, not a contract change.
 */
@Entity(tableName = "lectures")
public class LectureEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "course_id")
    public long courseId;

    @NonNull
    @ColumnInfo(name = "title")
    public String title = "";

    @ColumnInfo(name = "date")
    public long date;

    @Nullable
    @ColumnInfo(name = "audio_path")
    public String audioPath;

    @ColumnInfo(name = "duration_ms")
    public long durationMs;

    @NonNull
    @ColumnInfo(name = "status")
    public String status = "";
}
