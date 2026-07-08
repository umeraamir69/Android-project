package com.lecturelens.ui.upload;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.lecturelens.core.BaseViewModel;
import com.lecturelens.data.audio.AudioFileFactory;
import com.lecturelens.data.audio.AudioRecorder;
import com.lecturelens.domain.usecase.RecordLectureUseCase;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Track 3 — drives the Upload screen.
 *
 * <p>Two channels:
 * <ul>
 *   <li><b>{@link #getRecordingState()}</b> — the capture machine from
 *       {@code diagrams/02_audio_recording_state.puml}: Idle → CheckingPermission →
 *       Recording ⇄ Paused → Saving → Saved, plus PermissionDenied and Importing.
 *       This carries the live timer + waveform amplitude while recording.</li>
 *   <li><b>inherited {@code getUiState()}</b> ({@link BaseViewModel}&lt;Long&gt;) —
 *       used only as the one-shot <i>error</i> surface for save failures
 *       ({@code setError}); the positive terminal is {@link RecordingState.Saved}
 *       which carries the new lecture id to navigate to. (T = {@link Long} so the
 *       team can move navigation onto {@code setSuccess(lectureId)} later if
 *       preferred.)</li>
 * </ul>
 *
 * <p>Framework-light for testability: capture goes through {@link AudioRecorder}
 * (its own {@code Recorder} seam), the timer loop through {@link Ticker}, and the
 * target file through {@link AudioFileFactory} — so the whole machine runs on a
 * plain JVM with fakes.
 */
@HiltViewModel
public class UploadViewModel extends BaseViewModel<Long> {

    /** Nav argument key (see nav_graph.xml upload destination). */
    public static final String ARG_COURSE_ID = "courseId";

    /** Timer/waveform refresh cadence while recording. */
    private static final long TICK_INTERVAL_MS = 100L;

    private final AudioRecorder recorder;
    private final RecordLectureUseCase recordLecture;
    private final AudioFileFactory fileFactory;
    private final Ticker ticker;
    private final long courseId;

    private final MutableLiveData<RecordingState> recordingState =
            new MutableLiveData<>(RecordingState.idle());

    @Inject
    public UploadViewModel(@NonNull AudioRecorder recorder,
                           @NonNull RecordLectureUseCase recordLecture,
                           @NonNull AudioFileFactory fileFactory,
                           @NonNull SavedStateHandle savedStateHandle) {
        this(recorder, recordLecture, fileFactory, new HandlerTicker(), savedStateHandle);
    }

    @VisibleForTesting
    public UploadViewModel(@NonNull AudioRecorder recorder,
                           @NonNull RecordLectureUseCase recordLecture,
                           @NonNull AudioFileFactory fileFactory,
                           @NonNull Ticker ticker,
                           @NonNull SavedStateHandle savedStateHandle) {
        this.recorder = recorder;
        this.recordLecture = recordLecture;
        this.fileFactory = fileFactory;
        this.ticker = ticker;
        Long arg = savedStateHandle.get(ARG_COURSE_ID);
        this.courseId = arg != null ? arg : -1L;
    }

    @NonNull
    public LiveData<RecordingState> getRecordingState() {
        return recordingState;
    }

    // ---- Record path ----

    /** Tap Record. Idle → CheckingPermission (Fragment then requests the permission). */
    public void onRecordClicked() {
        if (recordingState.getValue() instanceof RecordingState.Idle) {
            recordingState.postValue(RecordingState.checkingPermission());
        }
    }

    /** Result of the RECORD_AUDIO request. */
    public void onPermissionResult(boolean granted) {
        if (granted) {
            startRecording();
        } else {
            recordingState.postValue(RecordingState.permissionDenied());
        }
    }

    /** Retry from PermissionDenied → re-request. */
    public void onPermissionRetry() {
        recordingState.postValue(RecordingState.checkingPermission());
    }

    /** Cancel from PermissionDenied → back to Idle. */
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

    /** Tap Stop → finalize + save + enqueue. */
    public void onStopClicked() {
        if (recorder.getState() == AudioRecorder.State.IDLE) {
            return; // nothing recording
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
        save(result.file.getAbsolutePath(), result.durationMs);
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

    /** Tap Import. Idle → Importing (Fragment then launches the SAF picker). */
    public void onImportClicked() {
        recordingState.postValue(RecordingState.importing());
    }

    /** SAF file copied + validated by the Fragment. */
    public void onImported(@NonNull String audioPath, long durationMs) {
        save(audioPath, durationMs);
    }

    /** Picker cancelled or file invalid. */
    public void onImportCancelled() {
        recordingState.postValue(RecordingState.idle());
    }

    // ---- Shared save ----

    private void save(@NonNull String audioPath, long durationMs) {
        recordingState.postValue(RecordingState.saving());
        setLoading();

        RecordLectureUseCase.Request request = new RecordLectureUseCase.Request(
                courseId,
                defaultTitle(),
                audioPath,
                durationMs,
                System.currentTimeMillis(),
                null /* language → orchestrator default */);

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
        // Release native recorder resources if the screen dies mid-capture.
        if (recorder.getState() != AudioRecorder.State.IDLE) {
            recorder.cancel();
        }
    }
}
