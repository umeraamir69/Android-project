package com.lecturelens.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Fts4;

/**
 * Track 1 — {@code transcripts_fts} virtual table (arch doc §3.3) powering
 * keyword search.
 *
 * <p>External-content FTS4 over {@link TranscriptSegmentEntity#text}: Room
 * generates the triggers that keep this table in sync with
 * {@code transcript_segments} automatically, so no IndexingWorker is needed
 * for keyword search — segments are searchable the moment Track 4 persists
 * them. The FTS rowid equals the segment's {@code id}, which is how
 * {@code SearchDao} joins back to timestamps.
 */
@Fts4(contentEntity = TranscriptSegmentEntity.class)
@Entity(tableName = "transcripts_fts")
public class TranscriptFtsEntity {

    @NonNull
    @ColumnInfo(name = "text")
    public String text = "";
}
