package com.lecturelens.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/**
 * Track 1 — one keyword-search result row (SearchDao), carrying everything
 * Track 5's search UI needs: which lecture, where in the audio, and a
 * highlighted snippet.
 */
public class SearchHit {

    @ColumnInfo(name = "lectureId")
    public long lectureId;

    @ColumnInfo(name = "segmentId")
    public long segmentId;

    @ColumnInfo(name = "startMs")
    public long startMs;

    @NonNull
    @ColumnInfo(name = "lectureTitle")
    public String lectureTitle = "";

    /** Matched excerpt with <b>…</b> markers from FTS4 snippet(). */
    @NonNull
    @ColumnInfo(name = "snippet")
    public String snippet = "";
}
