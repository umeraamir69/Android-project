package com.lecturelens.data.local.dao;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.lecturelens.data.local.entity.CourseEntity;

import java.util.List;

/**
 * Track 1 — courses. Reads return LiveData (main-safe); writes are
 * synchronous — dispatch on {@code AppExecutors.diskIO()}.
 */
@Dao
public interface CourseDao {

    /** @return the auto-generated row id. */
    @Insert
    long insert(CourseEntity course);

    @Query("UPDATE courses SET name = :name WHERE id = :id")
    void updateName(long id, @NonNull String name);

    @Query("DELETE FROM courses WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM courses ORDER BY created_at ASC")
    LiveData<List<CourseEntity>> observeAll();

    @Query("SELECT * FROM courses WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    CourseEntity findByNameSync(@NonNull String name);

    /** Synchronous — used by DatabaseSeeder's is-empty check. */
    @Query("SELECT COUNT(*) FROM courses")
    int count();
}
