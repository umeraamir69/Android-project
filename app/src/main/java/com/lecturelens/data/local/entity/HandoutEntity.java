package com.lecturelens.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Handout photo / PDF / doc attached to a lecture. */
@Entity(tableName = "handouts")
public class HandoutEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "lecture_id")
    public long lectureId;

    /** Local absolute path (kept as image_path for older rows). */
    @NonNull
    @ColumnInfo(name = "image_path")
    public String imagePath = "";

    @NonNull
    @ColumnInfo(name = "mime_type")
    public String mimeType = "image/jpeg";

    @NonNull
    @ColumnInfo(name = "display_name")
    public String displayName = "";

    @NonNull
    @ColumnInfo(name = "extracted_text")
    public String extractedText = "";

    @Nullable
    @ColumnInfo(name = "remote_url")
    public String remoteUrl;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
