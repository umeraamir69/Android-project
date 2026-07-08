package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.entity.LectureEntity;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.repository.LectureRepository;

import java.util.List;

import javax.inject.Inject;

/**
 * Track 3 — <b>write half</b> of the frozen {@link LectureRepository}.
 *
 * <p>Writes ({@link #insert}, {@link #updateStatus}, {@link #updateAudioPath}) are
 * real and synchronous — callers must run them on {@code AppExecutors.diskIO()} or
 * inside a Worker (enforced by {@code RecordLectureUseCase} and the workers).
 *
 * <p><b>Reads are not implemented here.</b> The read half is Track 2's
 * {@code LectureReadRepositoryImpl}. See {@code di/RepositoryModule} for the
 * INTEGRATION step where Tracks 2 + 3 replace two separate bindings with a single
 * facade that delegates reads → Track 2 and writes → this class. Until that lands,
 * the read methods below throw; the Upload screen never calls them.
 */
public class LectureWriteRepositoryImpl implements LectureRepository {

    private final LectureDao dao;

    @Inject
    public LectureWriteRepositoryImpl(@NonNull LectureDao dao) {
        this.dao = dao;
    }

    // ---- Writes (Track 3) ----

    @Override
    public long insert(@NonNull Lecture lecture) {
        return dao.insert(toEntity(lecture));
    }

    @Override
    public void updateStatus(long id, @NonNull LectureStatus status) {
        dao.updateStatus(id, status.name());
    }

    @Override
    public void updateAudioPath(long id, @Nullable String audioPath) {
        dao.updateAudioPath(id, audioPath);
    }

    // ---- Reads (Track 2 — see RepositoryModule facade TODO) ----

    @NonNull
    @Override
    public LiveData<List<Lecture>> observeAll() {
        throw new UnsupportedOperationException(
                "reads live in LectureReadRepositoryImpl (Track 2); bind via facade");
    }

    @NonNull
    @Override
    public LiveData<List<Lecture>> observeByCourse(long courseId) {
        throw new UnsupportedOperationException(
                "reads live in LectureReadRepositoryImpl (Track 2); bind via facade");
    }

    @NonNull
    @Override
    public LiveData<Lecture> observeById(long id) {
        throw new UnsupportedOperationException(
                "reads live in LectureReadRepositoryImpl (Track 2); bind via facade");
    }

    // ---- Mapping ----

    /**
     * Domain → entity. A fresh insert uses id {@code 0}; Room auto-generates the
     * real id and {@link #insert} returns it (Room ignores {@code 0} PKs on insert).
     */
    @NonNull
    private static LectureEntity toEntity(@NonNull Lecture lecture) {
        LectureEntity e = new LectureEntity();
        e.id = lecture.getId();
        e.courseId = lecture.getCourseId();
        e.title = lecture.getTitle();
        e.date = lecture.getDate();
        e.audioPath = lecture.getAudioPath();
        e.durationMs = lecture.getDurationMs();
        e.status = lecture.getStatus().name();
        return e;
    }
}
