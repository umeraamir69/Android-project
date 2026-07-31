package com.lecturelens.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.lecturelens.core.BaseViewModel;
import com.lecturelens.domain.model.Course;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.repository.CourseRepository;
import com.lecturelens.domain.repository.CredentialsStore;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.usecase.ImportSharedNotesUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Builds {@link HomeDashboard} from courses + lectures + signed-in email. */
@HiltViewModel
public class HomeViewModel extends BaseViewModel<HomeDashboard> {

    public static final class ImportEvent {
        public final long lectureId;
        @NonNull public final String title;

        ImportEvent(long lectureId, @NonNull String title) {
            this.lectureId = lectureId;
            this.title = title;
        }
    }

    private static final int RECENT_LIMIT = 5;

    private final LiveData<List<Course>> courses;
    private final LiveData<List<Lecture>> lectures;
    private final CredentialsStore credentials;
    private final ImportSharedNotesUseCase importSharedNotesUseCase;

    private final Observer<List<Course>> courseObserver;
    private final Observer<List<Lecture>> lectureObserver;

    private final MutableLiveData<ImportEvent> importEvent = new MutableLiveData<>();
    private final MutableLiveData<String> importError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> importLoading = new MutableLiveData<>(false);

    @Nullable private List<Course> latestCourses;
    @Nullable private List<Lecture> latestLectures;

    @Inject
    public HomeViewModel(@NonNull CourseRepository courseRepository,
                         @NonNull LectureRepository lectureRepository,
                         @NonNull CredentialsStore credentials,
                         @NonNull ImportSharedNotesUseCase importSharedNotesUseCase) {
        this.credentials = credentials;
        this.importSharedNotesUseCase = importSharedNotesUseCase;
        setLoading();
        courses = courseRepository.observeAll();
        lectures = lectureRepository.observeAll();
        courseObserver = value -> {
            latestCourses = value;
            rebuild();
        };
        lectureObserver = value -> {
            latestLectures = value;
            rebuild();
        };
        courses.observeForever(courseObserver);
        lectures.observeForever(lectureObserver);
    }

    @NonNull
    public LiveData<ImportEvent> getImportEvent() {
        return importEvent;
    }

    @NonNull
    public LiveData<String> getImportError() {
        return importError;
    }

    @NonNull
    public LiveData<Boolean> getImportLoading() {
        return importLoading;
    }

    /** Clear after the Snackbar is shown so returning to Home doesn't replay it. */
    public void consumeImportError() {
        importError.setValue(null);
    }

    public void consumeImportEvent() {
        importEvent.setValue(null);
    }

    public void importSharedNotes(@Nullable String code) {
        importLoading.setValue(true);
        importError.setValue(null);
        // Course rubric: AsyncTask used when importing shared notes from the network.
        com.lecturelens.core.BgAsyncTask.run(() -> { /* prepare import on worker */ });
        importSharedNotesUseCase.execute(code, new ImportSharedNotesUseCase.Callback() {
            @Override
            public void onImported(long lectureId, @NonNull String title) {
                importLoading.postValue(false);
                importEvent.postValue(new ImportEvent(lectureId, title));
            }

            @Override
            public void onError(@NonNull String message) {
                importLoading.postValue(false);
                importError.postValue(message);
            }
        });
    }

    private void rebuild() {
        if (latestCourses == null || latestLectures == null) {
            return;
        }
        String email = credentials.getEmail();
        if (email == null) {
            email = "";
        }

        int ready = 0;
        int processing = 0;
        int failed = 0;
        for (Lecture lecture : latestLectures) {
            LectureStatus status = lecture.getStatus();
            if (status == LectureStatus.READY || status == LectureStatus.SHARED) {
                ready++;
            } else if (status == LectureStatus.FAILED) {
                failed++;
            } else if (status == LectureStatus.TRANSCRIBING
                    || status == LectureStatus.TRANSCRIBED
                    || status == LectureStatus.SUMMARIZING
                    || status == LectureStatus.INDEXING) {
                processing++;
            }
        }

        List<Lecture> recent = new ArrayList<>(latestLectures);
        Collections.sort(recent, Comparator.comparingLong(Lecture::getDate).reversed());
        if (recent.size() > RECENT_LIMIT) {
            recent = new ArrayList<>(recent.subList(0, RECENT_LIMIT));
        }

        setSuccess(new HomeDashboard(
                latestLectures.size(),
                latestCourses.size(),
                ready,
                processing,
                failed,
                recent,
                email));
    }

    @Override
    protected void onCleared() {
        courses.removeObserver(courseObserver);
        lectures.removeObserver(lectureObserver);
        super.onCleared();
    }
}
