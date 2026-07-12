package com.lecturelens.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.local.entity.TranscriptSegmentEntity;

import java.util.List;

/**
 * Transcript reads/writes for {@code LectureLensDatabase}. Writes are used by
 * Track 4's transcription pipeline; the {@code observe*} reads back Track 5's
 * lecture view.
 */
@Dao
public interface TranscriptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTranscript(TranscriptEntity transcript);

    @Insert
    void insertSegments(List<TranscriptSegmentEntity> segments);

    @Query("DELETE FROM transcript_segments WHERE lecture_id = :lectureId")
    void deleteSegments(long lectureId);

    @Query("SELECT * FROM transcripts WHERE lecture_id = :lectureId LIMIT 1")
    LiveData<TranscriptEntity> observeTranscript(long lectureId);

    @Query("SELECT * FROM transcripts WHERE lecture_id = :lectureId LIMIT 1")
    TranscriptEntity getTranscriptSync(long lectureId);

    @Query("SELECT * FROM transcript_segments WHERE lecture_id = :lectureId ORDER BY start_ms ASC")
    LiveData<List<TranscriptSegmentEntity>> observeSegments(long lectureId);

    @Transaction
    default void replaceTranscript(TranscriptEntity transcript,
                                   List<TranscriptSegmentEntity> segments) {
        deleteSegments(transcript.lectureId);
        insertTranscript(transcript);
        if (!segments.isEmpty()) {
            insertSegments(segments);
        }
    }
}
