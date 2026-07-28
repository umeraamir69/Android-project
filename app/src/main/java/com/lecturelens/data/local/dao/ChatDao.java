package com.lecturelens.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.lecturelens.data.local.entity.ChatMessageEntity;

import java.util.List;

@Dao
public interface ChatDao {

    @Insert
    long insert(ChatMessageEntity message);

    @Query("SELECT * FROM chat_messages WHERE lecture_id = :lectureId ORDER BY created_at ASC, id ASC")
    LiveData<List<ChatMessageEntity>> observeByLecture(long lectureId);

    @Query("SELECT * FROM chat_messages WHERE lecture_id = :lectureId ORDER BY created_at ASC, id ASC")
    List<ChatMessageEntity> getByLectureSync(long lectureId);

    @Query("SELECT * FROM chat_messages WHERE lecture_id = :lectureId ORDER BY created_at DESC, id DESC LIMIT :limit")
    List<ChatMessageEntity> getRecentSync(long lectureId, int limit);

    @Query("DELETE FROM chat_messages WHERE lecture_id = :lectureId")
    void deleteByLecture(long lectureId);
}
