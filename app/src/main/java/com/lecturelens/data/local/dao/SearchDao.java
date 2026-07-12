package com.lecturelens.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.lecturelens.data.local.SearchHit;

import java.util.List;

/**
 * Track 1 — FTS4 keyword search over transcript segments (arch doc §4.2).
 * Consumed by Track 5's SearchLecturesUseCase.
 *
 * <p>{@code MATCH} accepts FTS4 query syntax; for a plain user query, append
 * {@code *} for prefix matching (the use case / ViewModel decides).
 */
@Dao
public interface SearchDao {

    @Query("SELECT s.lecture_id AS lectureId, "
            + "       s.id AS segmentId, "
            + "       s.start_ms AS startMs, "
            + "       l.title AS lectureTitle, "
            + "       snippet(transcripts_fts, '<b>', '</b>', '…') AS snippet "
            + "FROM transcripts_fts "
            + "JOIN transcript_segments AS s ON s.id = transcripts_fts.rowid "
            + "JOIN lectures AS l ON l.id = s.lecture_id "
            + "WHERE transcripts_fts MATCH :query "
            + "ORDER BY l.date DESC, s.start_ms ASC "
            + "LIMIT 100")
    LiveData<List<SearchHit>> search(String query);

    /** Synchronous variant — smoke tests and background use. */
    @Query("SELECT s.lecture_id AS lectureId, "
            + "       s.id AS segmentId, "
            + "       s.start_ms AS startMs, "
            + "       l.title AS lectureTitle, "
            + "       snippet(transcripts_fts, '<b>', '</b>', '…') AS snippet "
            + "FROM transcripts_fts "
            + "JOIN transcript_segments AS s ON s.id = transcripts_fts.rowid "
            + "JOIN lectures AS l ON l.id = s.lecture_id "
            + "WHERE transcripts_fts MATCH :query "
            + "ORDER BY l.date DESC, s.start_ms ASC "
            + "LIMIT 100")
    List<SearchHit> searchSync(String query);
}
