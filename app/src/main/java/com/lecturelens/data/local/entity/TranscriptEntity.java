package com.lecturelens.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * TEMP — Track 4 stub entity until Track 1's {@code LectureLensDatabase} lands.
 */
@Entity(tableName = "transcripts")
public class TranscriptEntity {

    @PrimaryKey
    @ColumnInfo(name = "lecture_id")
    public long lectureId;

    @NonNull
    @ColumnInfo(name = "full_text")
    public String fullText = "";

    @NonNull
    @ColumnInfo(name = "language")
    public String language = "";

    @NonNull
    @ColumnInfo(name = "model_used")
    public String modelUsed = "";
}
