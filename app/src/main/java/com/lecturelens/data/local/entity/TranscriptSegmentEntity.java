package com.lecturelens.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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

    /** STT diarization speaker tag; 0 = unknown / not provided. */
    @ColumnInfo(name = "speaker_tag", defaultValue = "0")
    public int speakerTag;
}
