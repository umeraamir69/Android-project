package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;

import java.util.List;

/**
 * FROZEN Day 0 contract — signature changes require a team sync.
 *
 * Split implementations to avoid file-level merge conflicts
 * (see WORK_BREAKDOWN.md risks):
 *  - reads  → Track 2, {@code LectureReadRepositoryImpl}
 *  - writes → Track 3, {@code LectureWriteRepositoryImpl}
 *
 * Reads return LiveData (main-safe). Writes are synchronous — callers must
 * dispatch on {@code AppExecutors.diskIO()} or run inside a Worker.
 */
public interface LectureRepository {

    // ---- Reads (Track 2) ----

    @NonNull
    LiveData<List<Lecture>> observeAll();

    @NonNull
    LiveData<List<Lecture>> observeByCourse(long courseId);

    @NonNull
    LiveData<Lecture> observeById(long id);

    // ---- Writes (Track 3) ----

    /** @return row id of the inserted lecture. Call on diskIO(). */
    long insert(@NonNull Lecture lecture);

    /** Call on diskIO() or from a Worker. */
    void updateStatus(long id, @NonNull LectureStatus status);

    /** Call on diskIO() or from a Worker. */
    void updateAudioPath(long id, @Nullable String audioPath);

    /** Assign lecture to a course, or {@code -1} for Uncategorized. Call on diskIO(). */
    void updateCourseId(long id, long courseId);

    /** Call on diskIO(). */
    void updateTitle(long id, @NonNull String title);

    /** Moves all lectures in {@code courseId} to Uncategorized. Call on diskIO(). */
    void clearCourseId(long courseId);
}
