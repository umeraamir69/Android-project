package com.lecturelens.ui.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.lecturelens.core.UiState;
import com.lecturelens.data.audio.AudioFileFactory;
import com.lecturelens.data.audio.AudioRecorder;
import com.lecturelens.domain.usecase.RecordLectureUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

/**
 * State-machine tests for {@link UploadViewModel}. LiveData is made synchronous by
 * {@link InstantTaskExecutorRule}; capture, timer and save are all faked so the
 * test runs on a plain JVM.
 */
public class UploadViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private FakeRecorder fakeRecorder;
    private long[] clock;
    private ManualTicker ticker;
    private FakeUseCase useCase;
    private UploadViewModel vm;

    @Before
    public void setUp() {
        fakeRecorder = new FakeRecorder();
        clock = new long[]{0L};
        AudioRecorder recorder = new AudioRecorder(fakeRecorder, () -> clock[0]);
        ticker = new ManualTicker();
        useCase = new FakeUseCase();
        AudioFileFactory files = () -> new File("/tmp/lecture.m4a");
        SavedStateHandle handle = new SavedStateHandle(
                Collections.singletonMap(UploadViewModel.ARG_COURSE_ID, 5L));
        vm = new UploadViewModel(recorder, useCase, files, ticker, handle);
    }

    @Test
    public void startsIdle() {
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.Idle);
    }

    @Test
    public void record_thenGrant_movesToRecording() {
        vm.onRecordClicked();
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.CheckingPermission);
        vm.onPermissionResult(true);
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.Recording);
    }

    @Test
    public void record_thenDeny_showsDenied_thenRetryAndCancel() {
        vm.onRecordClicked();
        vm.onPermissionResult(false);
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.PermissionDenied);
        vm.onPermissionRetry();
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.CheckingPermission);
        vm.onPermissionCancel();
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.Idle);
    }

    @Test
    public void pauseThenResume() {
        vm.onRecordClicked();
        vm.onPermissionResult(true);
        vm.onPauseClicked();
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.Paused);
        vm.onResumeClicked();
        // resume restarts the ticker; fire one tick to publish a Recording state
        clock[0] = 1_000L;
        fakeRecorder.amplitude = 5000;
        ticker.fire();
        RecordingState state = vm.getRecordingState().getValue();
        assertTrue(state instanceof RecordingState.Recording);
        assertEquals(5000, ((RecordingState.Recording) state).amplitude);
    }

    @Test
    public void tick_updatesElapsed() {
        vm.onRecordClicked();
        vm.onPermissionResult(true);
        clock[0] = 3_000L;
        ticker.fire();
        RecordingState state = vm.getRecordingState().getValue();
        assertEquals(3_000L, ((RecordingState.Recording) state).elapsedMs);
    }

    @Test
    public void stop_promptsForTitle_thenSaves() {
        vm.onRecordClicked();
        vm.onPermissionResult(true);
        clock[0] = 2_000L;
        useCase.succeedWith = 77L;
        vm.onStopClicked();
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.Naming);
        vm.onTitleConfirmed("Week 8 notes");
        RecordingState state = vm.getRecordingState().getValue();
        assertTrue(state instanceof RecordingState.Saved);
        assertEquals(77L, ((RecordingState.Saved) state).lectureId);
        assertEquals("Week 8 notes", useCase.lastTitle);
    }

    @Test
    public void stop_onSaveError_returnsIdle_andEmitsError() {
        vm.onRecordClicked();
        vm.onPermissionResult(true);
        useCase.failWith = "disk full";
        vm.onStopClicked();
        vm.onTitleSkipped();
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.Idle);
        UiState<Long> ui = vm.getUiState().getValue();
        assertTrue(ui instanceof UiState.Error);
        assertEquals("disk full", ((UiState.Error<Long>) ui).message);
    }

    @Test
    public void import_flow() {
        vm.onImportClicked();
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.Importing);
        useCase.succeedWith = 12L;
        vm.onImported("/tmp/imported.m4a", 60_000L);
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.Naming);
        vm.onTitleConfirmed("Imported lecture");
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.Saved);
        assertEquals("Imported lecture", useCase.lastTitle);
    }

    @Test
    public void import_cancelled_returnsIdle() {
        vm.onImportClicked();
        vm.onImportCancelled();
        assertTrue(vm.getRecordingState().getValue() instanceof RecordingState.Idle);
    }

    // ---- Fakes ----

    private static final class FakeRecorder implements AudioRecorder.Recorder {
        int amplitude;
        @Override public void prepareAndStart(@NonNull File output) { }
        @Override public void pause() { }
        @Override public void resume() { }
        @Override public void stop() { }
        @Override public void release() { }
        @Override public int getMaxAmplitude() { return amplitude; }
    }

    /** Ticker that stores the callback so the test fires ticks deterministically. */
    private static final class ManualTicker implements Ticker {
        private Runnable onTick;
        @Override public void start(@NonNull Runnable onTick, long intervalMs) { this.onTick = onTick; }
        @Override public void stop() { this.onTick = null; }
        void fire() { if (onTick != null) onTick.run(); }
    }

    /** Use-case double using the protected test constructor; never calls super. */
    private static final class FakeUseCase extends RecordLectureUseCase {
        Long succeedWith;
        String failWith;
        String lastTitle;

        @Override
        public void execute(@NonNull Request request, @NonNull Callback callback) {
            lastTitle = request.title;
            if (failWith != null) {
                callback.onError(failWith);
            } else {
                callback.onSaved(succeedWith != null ? succeedWith : 1L);
            }
        }
    }
}
