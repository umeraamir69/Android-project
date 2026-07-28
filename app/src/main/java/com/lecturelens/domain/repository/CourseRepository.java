package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.lecturelens.domain.model.Course;

import java.util.List;

/**
 * FROZEN Day 0 contract — signature changes require a team sync.
 *
 * Reads return LiveData (Room-backed, main-safe). Writes are synchronous —
 * callers must dispatch on {@code AppExecutors.diskIO()}.
 *
 * Implementation: Track 2 ({@code CourseRepositoryImpl} backed by CourseDao).
 */
public interface CourseRepository {

    @NonNull
    LiveData<List<Course>> observeAll();

    /** @return row id of the inserted course. Call on diskIO(). */
    long insert(@NonNull Course course);

    /** Call on diskIO(). */
    void rename(long id, @NonNull String name);

    /**
     * Deletes the course row. Callers should move its lectures to Uncategorized
     * first. Call on diskIO().
     */
    void delete(long id);
}
