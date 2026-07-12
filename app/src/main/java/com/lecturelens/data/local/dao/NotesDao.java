package com.lecturelens.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.lecturelens.data.local.entity.NotesEntity;

/**
 * Notes reads/writes for {@code LectureLensDatabase}. Written by Track 4's
 * summarization pipeline; {@code observeNotes} backs Track 5's notes tab.
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
