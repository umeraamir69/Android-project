package com.lecturelens.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.lecturelens.data.local.entity.NotesEntity;

/**
 * TEMP — Track 4 DAO until Track 1 absorbs into {@code LectureLensDatabase}.
 */
@Dao
public interface NotesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NotesEntity notes);

    @Query("SELECT * FROM notes WHERE lecture_id = :lectureId LIMIT 1")
    LiveData<NotesEntity> observeNotes(long lectureId);

    @Query("SELECT * FROM notes WHERE lecture_id = :lectureId LIMIT 1")
    NotesEntity getNotesSync(long lectureId);
}
