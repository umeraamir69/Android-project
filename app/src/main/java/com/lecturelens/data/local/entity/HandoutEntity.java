package com.lecturelens.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Professor handout / quiz photo attached to a lecture (OCR text stored). */
@Entity(tableName = "handouts")
public class HandoutEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "lecture_id")
    public long lectureId;

    @NonNull
    @ColumnInfo(name = "image_path")
    public String imagePath = "";

    @NonNull
    @ColumnInfo(name = "extracted_text")
    public String extractedText = "";

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
