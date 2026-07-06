package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.repository.LectureRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Track 2 (Daniel) — READ side of {@link LectureRepository}.
 *
 * IN-MEMORY STUB — backs the Library UI until Track 1 lands Room. Reads will
 * delegate to {@code LectureDao} once it exists.
 *
 * Writes belong to Track 3 (Adeniyi, {@code LectureWriteRepositoryImpl});
 * they throw here so accidental use is caught in dev. Once both halves are
 * real, Tracks 2+3 bind a combined impl per the RepositoryModule note.
 */
@Singleton
public class LectureReadRepositoryImpl implements LectureRepository {

    private final MutableLiveData<List<Lecture>> liveLectures =
            new MutableLiveData<>(Collections.unmodifiableList(DevSeed.lectures()));

    @Inject
    public LectureReadRepositoryImpl() {
    }

    // ---- Reads (Track 2) ----

    @NonNull
    @Override
    public LiveData<List<Lecture>> observeAll() {
        return liveLectures;
    }

    @NonNull
    @Override
    public LiveData<List<Lecture>> observeByCourse(long courseId) {
        MediatorLiveData<List<Lecture>> filtered = new MediatorLiveData<>();
        filtered.addSource(liveLectures, lectures -> {
            List<Lecture> match = new ArrayList<>();
            for (Lecture lecture : lectures) {
                if (lecture.getCourseId() == courseId) {
                    match.add(lecture);
                }
            }
            filtered.setValue(Collections.unmodifiableList(match));
        });
        return filtered;
    }

    @NonNull
    @Override
    public LiveData<Lecture> observeById(long id) {
        MediatorLiveData<Lecture> found = new MediatorLiveData<>();
        found.addSource(liveLectures, lectures -> {
            for (Lecture lecture : lectures) {
                if (lecture.getId() == id) {
                    found.setValue(lecture);
                    return;
                }
            }
        });
        return found;
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
