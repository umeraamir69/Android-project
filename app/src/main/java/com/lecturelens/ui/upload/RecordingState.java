package com.lecturelens.ui.upload;

import androidx.annotation.NonNull;

/**
 * Track 3 — capture state for the Upload screen, exposed by
 * {@link UploadViewModel#getRecordingState()} as {@code LiveData<RecordingState>}.
 *
 * <p>Sealed-style (same pattern as {@code core/Result} and {@code core/UiState}):
 * an abstract class with a private constructor and a fixed set of static nested
 * subtypes. Models {@code diagrams/02_audio_recording_state.puml}:
 *
 * <pre>
 *   Idle → CheckingPermission → Recording ⇄ Paused → Saving → Saved
 *   CheckingPermission → PermissionDenied → (retry|cancel)
 *   Idle → Importing → Saving
 * </pre>
 *
 * <p>Save <i>failures</i> are surfaced separately through the inherited
 * {@code BaseViewModel} channel ({@code UiState.Error}) as a one-shot message,
 * per the diagram's "Saving → Idle on failure (toast shown)" edge; on failure
 * the state returns to {@link #idle()}.
 */
public abstract class RecordingState {

    private RecordingState() {
    }

    /** No capture in progress; mic permission not yet checked. */
    public static final class Idle extends RecordingState {
        private Idle() { }
    }

    /** Runtime permission request in flight (tap Record). */
    public static final class CheckingPermission extends RecordingState {
        private CheckingPermission() { }
    }

    /** RECORD_AUDIO denied; UI offers Retry / Cancel. */
    public static final class PermissionDenied extends RecordingState {
        private PermissionDenied() { }
    }

    /** Actively capturing. Carries live values for the timer + waveform. */
    public static final class Recording extends RecordingState {
        public final long elapsedMs;
        /** Peak amplitude {@code 0..32767} for the level meter. */
        public final int amplitude;

        private Recording(long elapsedMs, int amplitude) {
            this.elapsedMs = elapsedMs;
            this.amplitude = amplitude;
        }
    }

    /** Capture paused; file handle kept open. */
    public static final class Paused extends RecordingState {
        public final long elapsedMs;

        private Paused(long elapsedMs) {
            this.elapsedMs = elapsedMs;
        }
    }

    /** SAF picker → copying/validating an imported file. */
    public static final class Importing extends RecordingState {
        private Importing() { }
    }

    /** Audio ready; user is naming the lecture before save. */
    public static final class Naming extends RecordingState {
        @NonNull public final String audioPath;
        public final long durationMs;
        @NonNull public final String suggestedTitle;

        private Naming(@NonNull String audioPath, long durationMs, @NonNull String suggestedTitle) {
            this.audioPath = audioPath;
            this.durationMs = durationMs;
            this.suggestedTitle = suggestedTitle;
        }
    }

    /** Persisting the lecture row + enqueuing the pipeline. */
    public static final class Saving extends RecordingState {
        private Saving() { }
    }

    /** Terminal success: row inserted, pipeline enqueued. Navigate onward. */
    public static final class Saved extends RecordingState {
        public final long lectureId;

        private Saved(long lectureId) {
            this.lectureId = lectureId;
        }
    }

    // ---- Singletons for stateless variants ----

    private static final Idle IDLE = new Idle();
    private static final CheckingPermission CHECKING_PERMISSION = new CheckingPermission();
    private static final PermissionDenied PERMISSION_DENIED = new PermissionDenied();
    private static final Importing IMPORTING = new Importing();
    private static final Saving SAVING = new Saving();

    // ---- Factories ----

    @NonNull
    public static RecordingState idle() {
        return IDLE;
    }

    @NonNull
    public static RecordingState checkingPermission() {
        return CHECKING_PERMISSION;
    }

    @NonNull
    public static RecordingState permissionDenied() {
        return PERMISSION_DENIED;
    }

    @NonNull
    public static RecordingState recording(long elapsedMs, int amplitude) {
        return new Recording(elapsedMs, amplitude);
    }

    @NonNull
    public static RecordingState paused(long elapsedMs) {
        return new Paused(elapsedMs);
    }

    @NonNull
    public static RecordingState importing() {
        return IMPORTING;
    }

    @NonNull
    public static RecordingState naming(@NonNull String audioPath,
                                        long durationMs,
                                        @NonNull String suggestedTitle) {
        return new Naming(audioPath, durationMs, suggestedTitle);
    }

    @NonNull
    public static RecordingState saving() {
        return SAVING;
    }

    @NonNull
    public static RecordingState saved(long lectureId) {
        return new Saved(lectureId);
    }
}
