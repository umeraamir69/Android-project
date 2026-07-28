package com.lecturelens.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Persisted Ask AI chat turn for a lecture. */
@Entity(
        tableName = "chat_messages",
        indices = {@Index("lecture_id")}
)
public class ChatMessageEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "lecture_id")
    public long lectureId;

    @NonNull
    @ColumnInfo(name = "role")
    public String role = "user";

    @NonNull
    @ColumnInfo(name = "text")
    public String text = "";

    @NonNull
    @ColumnInfo(name = "citations_json")
    public String citationsJson = "[]";

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
