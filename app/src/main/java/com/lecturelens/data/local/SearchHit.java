package com.lecturelens.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/**
 * One search result — transcript segment, notes, action item, or chat turn.
 */
public class SearchHit {

    public static final String SOURCE_TRANSCRIPT = "TRANSCRIPT";
    public static final String SOURCE_NOTES = "NOTES";
    public static final String SOURCE_KEY_TERM = "KEY_TERM";
    public static final String SOURCE_ACTION = "ACTION";
    public static final String SOURCE_CHAT = "CHAT";

    @ColumnInfo(name = "lectureId")
    public long lectureId;

    /** Segment id, chat message id, or 0 for notes-wide hits. */
    @ColumnInfo(name = "segmentId")
    public long segmentId;

    /** Audio seek position; -1 when not applicable. */
    @ColumnInfo(name = "startMs")
    public long startMs;

    @NonNull
    @ColumnInfo(name = "lectureTitle")
    public String lectureTitle = "";

    @NonNull
    @ColumnInfo(name = "snippet")
    public String snippet = "";

    @NonNull
    @ColumnInfo(name = "sourceType")
    public String sourceType = SOURCE_TRANSCRIPT;

    @NonNull
    @ColumnInfo(name = "sourceLabel")
    public String sourceLabel = "Transcript";

    public SearchHit() {
    }
}
