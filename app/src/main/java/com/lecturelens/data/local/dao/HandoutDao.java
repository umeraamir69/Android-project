package com.lecturelens.data.local.dao;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.lecturelens.data.local.entity.HandoutEntity;

import java.util.List;

@Dao
public interface HandoutDao {

    @Insert
    long insert(HandoutEntity handout);

    @Query("SELECT * FROM handouts WHERE lecture_id = :lectureId ORDER BY created_at DESC")
    LiveData<List<HandoutEntity>> observeByLecture(long lectureId);

    @Query("SELECT * FROM handouts WHERE lecture_id = :lectureId ORDER BY created_at DESC")
    List<HandoutEntity> getByLectureSync(long lectureId);

    @Query("DELETE FROM handouts WHERE id = :id")
    void deleteById(long id);
}
