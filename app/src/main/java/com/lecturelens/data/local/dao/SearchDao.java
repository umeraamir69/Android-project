package com.lecturelens.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.lecturelens.data.local.SearchHit;

import java.util.List;

/**
 * Flexible search: FTS transcript + LIKE over notes / chat.
 */
@Dao
public interface SearchDao {

    @Query("SELECT s.lecture_id AS lectureId, "
            + "       s.id AS segmentId, "
            + "       s.start_ms AS startMs, "
            + "       l.title AS lectureTitle, "
            + "       snippet(transcripts_fts, '<b>', '</b>', '…') AS snippet, "
            + "       'TRANSCRIPT' AS sourceType, "
            + "       'Transcript' AS sourceLabel "
            + "FROM transcripts_fts "
            + "JOIN transcript_segments AS s ON s.id = transcripts_fts.rowid "
            + "JOIN lectures AS l ON l.id = s.lecture_id "
            + "WHERE transcripts_fts MATCH :ftsQuery "
            + "ORDER BY l.date DESC, s.start_ms ASC "
            + "LIMIT 80")
    List<SearchHit> searchTranscriptSync(String ftsQuery);

    @Query("SELECT n.lecture_id AS lectureId, "
            + "       0 AS segmentId, "
            + "       -1 AS startMs, "
            + "       l.title AS lectureTitle, "
            + "       n.summary AS snippet, "
            + "       'NOTES' AS sourceType, "
            + "       'Notes summary' AS sourceLabel "
            + "FROM notes AS n "
            + "JOIN lectures AS l ON l.id = n.lecture_id "
            + "WHERE lower(n.summary) LIKE :like "
            + "LIMIT 40")
    List<SearchHit> searchNotesSummarySync(String like);

    @Query("SELECT n.lecture_id AS lectureId, "
            + "       0 AS segmentId, "
            + "       -1 AS startMs, "
            + "       l.title AS lectureTitle, "
            + "       n.key_terms_json AS snippet, "
            + "       'KEY_TERM' AS sourceType, "
            + "       'Key term' AS sourceLabel "
            + "FROM notes AS n "
            + "JOIN lectures AS l ON l.id = n.lecture_id "
            + "WHERE lower(n.key_terms_json) LIKE :like "
            + "LIMIT 40")
    List<SearchHit> searchKeyTermsSync(String like);

    @Query("SELECT n.lecture_id AS lectureId, "
            + "       0 AS segmentId, "
            + "       -1 AS startMs, "
            + "       l.title AS lectureTitle, "
            + "       n.action_items_json AS snippet, "
            + "       'ACTION' AS sourceType, "
            + "       'Action item' AS sourceLabel "
            + "FROM notes AS n "
            + "JOIN lectures AS l ON l.id = n.lecture_id "
            + "WHERE lower(n.action_items_json) LIKE :like "
            + "LIMIT 40")
    List<SearchHit> searchActionItemsSync(String like);

    @Query("SELECT c.lecture_id AS lectureId, "
            + "       c.id AS segmentId, "
            + "       -1 AS startMs, "
            + "       l.title AS lectureTitle, "
            + "       c.text AS snippet, "
            + "       'CHAT' AS sourceType, "
            + "       CASE WHEN c.role = 'user' THEN 'Your question' ELSE 'AI answer' END AS sourceLabel "
            + "FROM chat_messages AS c "
            + "JOIN lectures AS l ON l.id = c.lecture_id "
            + "WHERE lower(c.text) LIKE :like "
            + "ORDER BY c.created_at DESC "
            + "LIMIT 40")
    List<SearchHit> searchChatSync(String like);

    @Query("SELECT title FROM lectures "
            + "WHERE lower(title) LIKE :prefix "
            + "ORDER BY date DESC LIMIT 8")
    List<String> suggestLectureTitles(String prefix);

    @Query("SELECT text FROM chat_messages "
            + "WHERE role = 'user' AND lower(text) LIKE :prefix "
            + "ORDER BY created_at DESC LIMIT 8")
    List<String> suggestChatQuestions(String prefix);

    /** Kept for older callers / tests that expect LiveData FTS. */
    @Query("SELECT s.lecture_id AS lectureId, "
            + "       s.id AS segmentId, "
            + "       s.start_ms AS startMs, "
            + "       l.title AS lectureTitle, "
            + "       snippet(transcripts_fts, '<b>', '</b>', '…') AS snippet, "
            + "       'TRANSCRIPT' AS sourceType, "
            + "       'Transcript' AS sourceLabel "
            + "FROM transcripts_fts "
            + "JOIN transcript_segments AS s ON s.id = transcripts_fts.rowid "
            + "JOIN lectures AS l ON l.id = s.lecture_id "
            + "WHERE transcripts_fts MATCH :query "
            + "ORDER BY l.date DESC, s.start_ms ASC "
            + "LIMIT 100")
    LiveData<List<SearchHit>> search(String query);

    @Query("SELECT s.lecture_id AS lectureId, "
            + "       s.id AS segmentId, "
            + "       s.start_ms AS startMs, "
            + "       l.title AS lectureTitle, "
            + "       snippet(transcripts_fts, '<b>', '</b>', '…') AS snippet, "
            + "       'TRANSCRIPT' AS sourceType, "
            + "       'Transcript' AS sourceLabel "
            + "FROM transcripts_fts "
            + "JOIN transcript_segments AS s ON s.id = transcripts_fts.rowid "
            + "JOIN lectures AS l ON l.id = s.lecture_id "
            + "WHERE transcripts_fts MATCH :query "
            + "ORDER BY l.date DESC, s.start_ms ASC "
            + "LIMIT 100")
    List<SearchHit> searchSync(String query);
}
