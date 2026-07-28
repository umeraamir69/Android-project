package com.lecturelens.ui.upload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.lecturelens.core.BaseViewModel;
import com.lecturelens.data.audio.AudioFileFactory;
import com.lecturelens.data.audio.AudioRecorder;
import com.lecturelens.data.prefs.UserSettingsStore;
import com.lecturelens.domain.model.Course;
import com.lecturelens.domain.repository.CourseRepository;
import com.lecturelens.domain.usecase.RecordLectureUseCase;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Track 3 — drives the Upload screen (record / import + category selection).
 */
@HiltViewModel
public class UploadViewModel extends BaseViewModel<Long> {

    /** Nav argument key (see nav_graph.xml upload destination). */
    public static final String ARG_COURSE_ID = "courseId";

    /** Same sentinel as Library — lectures with no course. */
    public static final long UNCATEGORIZED_COURSE_ID = -1L;

    /** Timer/waveform refresh cadence while recording. */
    private static final long TICK_INTERVAL_MS = 100L;

    private final AudioRecorder recorder;
    private final RecordLectureUseCase recordLecture;
    private final AudioFileFactory fileFactory;
    private final UserSettingsStore userSettings;
    private final Ticker ticker;

    private final LiveData<List<Course>> courses;
    private final MutableLiveData<Long> selectedCourseId;
    private final MutableLiveData<String> selectedLanguage;
    private final MutableLiveData<RecordingState> recordingState =
            new MutableLiveData<>(RecordingState.idle());

    @Inject
    public UploadViewModel(@NonNull AudioRecorder recorder,
                           @NonNull RecordLectureUseCase recordLecture,
                           @NonNull AudioFileFactory fileFactory,
                           @NonNull UserSettingsStore userSettings,
                           @NonNull CourseRepository courseRepository,
                           @NonNull SavedStateHandle savedStateHandle) {
        this(recorder, recordLecture, fileFactory, userSettings, new HandlerTicker(),
                courseRepository.observeAll(), savedStateHandle);
    }

    @VisibleForTesting
    public UploadViewModel(@NonNull AudioRecorder recorder,
                           @NonNull RecordLectureUseCase recordLecture,
                           @NonNull AudioFileFactory fileFactory,
                           @NonNull Ticker ticker,
                           @NonNull SavedStateHandle savedStateHandle) {
        this(recorder, recordLecture, fileFactory, null, ticker,
                new MutableLiveData<>(Collections.emptyList()), savedStateHandle);
    }

    private UploadViewModel(@NonNull AudioRecorder recorder,
                            @NonNull RecordLectureUseCase recordLecture,
                            @NonNull AudioFileFactory fileFactory,
                            @Nullable UserSettingsStore userSettings,
                            @NonNull Ticker ticker,
                            @NonNull LiveData<List<Course>> courses,
                            @NonNull SavedStateHandle savedStateHandle) {
        this.recorder = recorder;
        this.recordLecture = recordLecture;
        this.fileFactory = fileFactory;
        this.userSettings = userSettings;
        this.ticker = ticker;
        this.courses = courses;
        Long arg = savedStateHandle.get(ARG_COURSE_ID);
        long initial = arg != null ? arg : UNCATEGORIZED_COURSE_ID;
        this.selectedCourseId = new MutableLiveData<>(initial);
        String lang = userSettings != null ? userSettings.getSttLanguage() : "en-US";
        this.selectedLanguage = new MutableLiveData<>(lang);
    }

    @NonNull
    public LiveData<RecordingState> getRecordingState() {
        return recordingState;
    }

    @NonNull
    public LiveData<List<Course>> getCourses() {
        return courses;
    }

    @NonNull
    public LiveData<Long> getSelectedCourseId() {
        return selectedCourseId;
    }

    public void selectCourse(long courseId) {
        selectedCourseId.setValue(courseId);
    }

    public long currentCourseId() {
        Long value = selectedCourseId.getValue();
        return value != null ? value : UNCATEGORIZED_COURSE_ID;
    }

    @NonNull
    public LiveData<String> getSelectedLanguage() {
        return selectedLanguage;
    }

    public void selectLanguage(@NonNull String languageCode) {
        selectedLanguage.setValue(languageCode);
        if (userSettings != null) {
            userSettings.setSttLanguage(languageCode);
        }
    }

    @NonNull
    public String currentLanguage() {
        String value = selectedLanguage.getValue();
        return value != null && !value.isEmpty() ? value : "en-US";
    }

    // ---- Record path ----

    public void onRecordClicked() {
        if (recordingState.getValue() instanceof RecordingState.Idle) {
            recordingState.postValue(RecordingState.checkingPermission());
        }
    }

    public void onPermissionResult(boolean granted) {
        if (granted) {
            startRecording();
        } else {
            recordingState.postValue(RecordingState.permissionDenied());
        }
    }

    public void onPermissionRetry() {
        recordingState.postValue(RecordingState.checkingPermission());
    }

    public void onPermissionCancel() {
        recordingState.postValue(RecordingState.idle());
    }

    public void onPauseClicked() {
        if (recorder.getState() == AudioRecorder.State.RECORDING) {
            recorder.pause();
            ticker.stop();
            recordingState.postValue(RecordingState.paused(recorder.getElapsedMs()));
        }
    }

    public void onResumeClicked() {
        if (recorder.getState() == AudioRecorder.State.PAUSED) {
            recorder.resume();
            startTicker();
        }
    }

    public void onStopClicked() {
        if (recorder.getState() == AudioRecorder.State.IDLE) {
            return;
        }
        ticker.stop();
        final AudioRecorder.Result result;
        try {
            result = recorder.stop();
        } catch (RuntimeException e) {
            setError("Recording failed to finalize");
            recordingState.postValue(RecordingState.idle());
            return;
        }
        promptForTitle(result.file.getAbsolutePath(), result.durationMs);
    }

    private void startRecording() {
        File output = fileFactory.newRecordingFile();
        try {
            recorder.start(output);
        } catch (IOException | RuntimeException e) {
            setError("Couldn't start recording");
            recordingState.postValue(RecordingState.idle());
            return;
        }
        recordingState.postValue(RecordingState.recording(0L, 0));
        startTicker();
    }

    private void startTicker() {
        ticker.start(() -> {
            if (recorder.getState() == AudioRecorder.State.RECORDING) {
                recordingState.postValue(
                        RecordingState.recording(recorder.getElapsedMs(), recorder.getMaxAmplitude()));
            }
        }, TICK_INTERVAL_MS);
    }

    // ---- Import path ----

    public void onImportClicked() {
        recordingState.postValue(RecordingState.importing());
    }

    public void onImported(@NonNull String audioPath, long durationMs) {
        promptForTitle(audioPath, durationMs);
    }

    public void onImportCancelled() {
        recordingState.postValue(RecordingState.idle());
    }

    public void onTitleConfirmed(@NonNull String rawTitle) {
        RecordingState state = recordingState.getValue();
        if (!(state instanceof RecordingState.Naming)) {
            return;
        }
        RecordingState.Naming naming = (RecordingState.Naming) state;
        String title = rawTitle.trim();
        if (title.isEmpty()) {
            title = naming.suggestedTitle;
        }
        save(title, naming.audioPath, naming.durationMs);
    }

    public void onTitleSkipped() {
        RecordingState state = recordingState.getValue();
        if (!(state instanceof RecordingState.Naming)) {
            return;
        }
        RecordingState.Naming naming = (RecordingState.Naming) state;
        save(naming.suggestedTitle, naming.audioPath, naming.durationMs);
    }

    // ---- Shared save ----

    private void promptForTitle(@NonNull String audioPath, long durationMs) {
        recordingState.postValue(RecordingState.naming(audioPath, durationMs, defaultTitle()));
    }

    private void save(@NonNull String title, @NonNull String audioPath, long durationMs) {
        recordingState.postValue(RecordingState.saving());
        setLoading();

        RecordLectureUseCase.Request request = new RecordLectureUseCase.Request(
                currentCourseId(),
                title,
                audioPath,
                durationMs,
                System.currentTimeMillis(),
                currentLanguage());

        recordLecture.execute(request, new RecordLectureUseCase.Callback() {
            @Override
            public void onSaved(long lectureId) {
                recordingState.postValue(RecordingState.saved(lectureId));
            }

            @Override
            public void onError(@NonNull String message) {
                setError(message);
                recordingState.postValue(RecordingState.idle());
            }
        });
    }

    @NonNull
    private static String defaultTitle() {
        return "Lecture " + java.text.DateFormat
                .getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                .format(new java.util.Date());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ticker.stop();
        if (recorder.getState() != AudioRecorder.State.IDLE) {
            recorder.cancel();
        }
    }
}
