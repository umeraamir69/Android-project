package com.lecturelens.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.lecturelens.data.local.entity.EmbeddingEntity;

import java.util.List;

@Dao
public interface EmbeddingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EmbeddingEntity> rows);

    @Query("DELETE FROM embeddings WHERE lecture_id = :lectureId")
    void deleteByLecture(long lectureId);

    @Query("SELECT * FROM embeddings WHERE lecture_id = :lectureId ORDER BY chunk_index ASC")
    List<EmbeddingEntity> getByLectureSync(long lectureId);

    @Query("SELECT COUNT(*) FROM embeddings WHERE lecture_id = :lectureId")
    int countForLecture(long lectureId);
}
