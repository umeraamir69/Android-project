package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.repository.LectureRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * INTEGRATION (Tracks 2+3) — the combined {@link LectureRepository} binding
 * planned in {@code di/RepositoryModule}: delegates reads to Track 2's
 * {@link LectureReadRepositoryImpl} and writes to Track 3's
 * {@link LectureWriteRepositoryImpl}.
 *
 * <p>Both halves are DAO-backed against Track 1's {@code LectureLensDatabase},
 * so inserts from the record flow appear live in the Library list.
 */
@Singleton
public class LectureRepositoryFacade implements LectureRepository {

    private final LectureReadRepositoryImpl reads;
    private final LectureWriteRepositoryImpl writes;

    @Inject
    public LectureRepositoryFacade(@NonNull LectureReadRepositoryImpl reads,
                                   @NonNull LectureWriteRepositoryImpl writes) {
        this.reads = reads;
        this.writes = writes;
    }

    // ---- Reads (Track 2) ----

    @NonNull
    @Override
    public LiveData<List<Lecture>> observeAll() {
        return reads.observeAll();
    }

    @NonNull
    @Override
    public LiveData<List<Lecture>> observeByCourse(long courseId) {
        return reads.observeByCourse(courseId);
    }

    @NonNull
    @Override
    public LiveData<Lecture> observeById(long id) {
        return reads.observeById(id);
    }

    // ---- Writes (Track 3) ----

    @Override
    public long insert(@NonNull Lecture lecture) {
        return writes.insert(lecture);
    }

    @Override
    public void updateStatus(long id, @NonNull LectureStatus status) {
        writes.updateStatus(id, status);
    }

    @Override
    public void updateAudioPath(long id, @Nullable String audioPath) {
        writes.updateAudioPath(id, audioPath);
    }
}
