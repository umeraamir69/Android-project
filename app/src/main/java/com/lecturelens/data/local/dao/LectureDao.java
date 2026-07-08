package com.lecturelens.data.local.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.lecturelens.data.local.entity.LectureEntity;

/**
 * STUB — owned by Track 1 (Zeeshan). Track 3 declares only the write methods
 * {@code LectureWriteRepositoryImpl} calls; Track 1 will add the read queries
 * (observeAll / observeByCourse / observeById returning {@code LiveData}) that
 * Track 2 needs, plus FTS wiring, when the real DB lands.
 *
 * <p>All methods are synchronous — callers dispatch on {@code AppExecutors.diskIO()}
 * or run inside a Worker (threading discipline, WORK_BREAKDOWN risks).
 */
@Dao
public interface LectureDao {

    /** @return the auto-generated row id. */
    @Insert
    long insert(LectureEntity lecture);

    @Query("UPDATE lectures SET status = :status WHERE id = :id")
    void updateStatus(long id, String status);

    @Query("UPDATE lectures SET audio_path = :audioPath WHERE id = :id")
    void updateAudioPath(long id, @Nullable String audioPath);

    @Query("SELECT id FROM lectures WHERE audio_path = :audioPath LIMIT 1")
    long findIdByAudioPath(@NonNull String audioPath);
}
