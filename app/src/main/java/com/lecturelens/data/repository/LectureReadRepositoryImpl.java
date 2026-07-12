package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.repository.LectureRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Track 2 (Daniel) — READ side of {@link LectureRepository}, DAO-backed since
 * Track 1's Room DB landed (previously an in-memory DevSeed stub).
 *
 * Writes belong to Track 3 ({@code LectureWriteRepositoryImpl}); both halves
 * are bound together by {@code LectureRepositoryFacade}.
 */
@Singleton
public class LectureReadRepositoryImpl implements LectureRepository {

    private final LectureDao dao;

    @Inject
    public LectureReadRepositoryImpl(@NonNull LectureDao dao) {
        this.dao = dao;
    }

    // ---- Reads (Track 2) ----

    @NonNull
    @Override
    public LiveData<List<Lecture>> observeAll() {
        return Transformations.map(dao.observeAll(), LectureEntityMapper::toDomain);
    }

    @NonNull
    @Override
    public LiveData<List<Lecture>> observeByCourse(long courseId) {
        return Transformations.map(dao.observeByCourse(courseId), LectureEntityMapper::toDomain);
    }

    @NonNull
    @Override
    public LiveData<Lecture> observeById(long id) {
        // Emits null when the lecture doesn't exist — observers must
        // null-check rather than waiting forever (old stub never emitted).
        return Transformations.map(dao.observeById(id), LectureEntityMapper::toDomain);
    }

    // ---- Writes (Track 3 — not implemented here) ----

    @Override
    public long insert(@NonNull Lecture lecture) {
        throw new UnsupportedOperationException(
                "Lecture writes are Track 3 (LectureWriteRepositoryImpl)");
    }

    @Override
    public void updateStatus(long id, @NonNull LectureStatus status) {
        throw new UnsupportedOperationException(
                "Lecture writes are Track 3 (LectureWriteRepositoryImpl)");
    }

    @Override
    public void updateAudioPath(long id, @Nullable String audioPath) {
        throw new UnsupportedOperationException(
                "Lecture writes are Track 3 (LectureWriteRepositoryImpl)");
    }
}
