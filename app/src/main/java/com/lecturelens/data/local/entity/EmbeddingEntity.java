package com.lecturelens.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Stretch RAG — one embedded transcript/notes chunk per row. */
@Entity(
        tableName = "embeddings",
        indices = {@Index("lecture_id")}
)
public class EmbeddingEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "lecture_id")
    public long lectureId;

    @ColumnInfo(name = "chunk_index")
    public int chunkIndex;

    @ColumnInfo(name = "start_ms")
    public long startMs;

    @ColumnInfo(name = "end_ms")
    public long endMs;

    @NonNull
    @ColumnInfo(name = "text")
    public String text = "";

    /** Little-endian float32 vector from Gemini text-embedding-004. */
    @ColumnInfo(name = "vector", typeAffinity = ColumnInfo.BLOB)
    public byte[] vector;
}
