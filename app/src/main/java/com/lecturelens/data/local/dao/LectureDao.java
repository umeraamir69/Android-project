package com.lecturelens.data.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.lecturelens.data.local.entity.LectureEntity;

import java.util.List;

/**
 * Track 1 — lectures. Reads return LiveData (main-safe, consumed by Track 2's
 * read repository); writes are synchronous — dispatch on
 * {@code AppExecutors.diskIO()} or run inside a Worker.
 */
@Dao
public interface LectureDao {

    // ---- Writes (Track 3 write repository) ----

    /** @return the auto-generated row id. */
    @Insert
    long insert(LectureEntity lecture);

    @Query("UPDATE lectures SET status = :status WHERE id = :id")
    void updateStatus(long id, String status);

    @Query("UPDATE lectures SET audio_path = :audioPath WHERE id = :id")
    void updateAudioPath(long id, @Nullable String audioPath);

    @Query("UPDATE lectures SET course_id = :courseId WHERE id = :id")
    void updateCourseId(long id, long courseId);

    @Query("UPDATE lectures SET title = :title WHERE id = :id")
    void updateTitle(long id, @NonNull String title);

    /** Moves every lecture in a course to Uncategorized ({@code courseId = -1}). */
    @Query("UPDATE lectures SET course_id = -1 WHERE course_id = :courseId")
    void clearCourseId(long courseId);

    @Query("SELECT id FROM lectures WHERE audio_path = :audioPath LIMIT 1")
    long findIdByAudioPath(@NonNull String audioPath);

    // ---- Reads (Track 2 read repository) ----

    @Query("SELECT * FROM lectures ORDER BY date DESC")
    LiveData<List<LectureEntity>> observeAll();

    @Query("SELECT * FROM lectures WHERE course_id = :courseId ORDER BY date DESC")
    LiveData<List<LectureEntity>> observeByCourse(long courseId);

    @Query("SELECT * FROM lectures WHERE id = :id LIMIT 1")
    LiveData<LectureEntity> observeById(long id);

    /** Synchronous — Track 5 export / workers. Call on diskIO. */
    @Query("SELECT * FROM lectures WHERE id = :id LIMIT 1")
    @Nullable
    LectureEntity getByIdSync(long id);

    /** Synchronous — used by DatabaseSeeder's is-empty check. */
    @Query("SELECT COUNT(*) FROM lectures")
    int count();
}
