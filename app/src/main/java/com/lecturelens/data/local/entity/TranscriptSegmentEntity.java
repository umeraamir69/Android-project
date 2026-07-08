package com.lecturelens.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * TEMP — Track 4 stub entity until Track 1's {@code LectureLensDatabase} lands.
 */
@Entity(tableName = "transcript_segments")
public class TranscriptSegmentEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "lecture_id")
    public long lectureId;

    @ColumnInfo(name = "start_ms")
    public long startMs;

    @ColumnInfo(name = "end_ms")
    public long endMs;

    @NonNull
    @ColumnInfo(name = "text")
    public String text = "";
}
