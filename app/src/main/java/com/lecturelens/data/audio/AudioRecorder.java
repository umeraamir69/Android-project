package com.lecturelens.data.audio;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.File;
import java.io.IOException;

/**
 * Track 3 — self-contained {@link MediaRecorder} wrapper.
 *
 * <p>Captures lecture audio as <b>M4A / AAC, 16 kHz mono</b> so ExoPlayer can
 * play it on emulators (AMR-WB decode is broken on many AVD images). Speech-to-Text
 * v1 receives a LINEAR16 PCM conversion of this file.
 *
 * <p><b>No Hilt.</b> This class is deliberately framework-light so it can be
 * unit-tested on a plain JVM: all real {@link MediaRecorder} interaction sits
 * behind the {@link Recorder} seam, so a test injects a fake and asserts the
 * state machine + call ordering without touching {@code android.jar} stubs.
 *
 * <p>Not thread-safe — drive it from a single thread (the ViewModel /
 * {@code RecordingService} main thread is fine; amplitude polling reads are
 * cheap and lock-free).
 *
 * <p>State machine (subset of {@code diagrams/02_audio_recording_state.puml}):
 * <pre>
 *   IDLE --start()--> RECORDING <--resume()-- PAUSED
 *                        |  \--pause()--> PAUSED
 *                        \--stop()--> IDLE (file returned)
 * </pre>
 */
public class AudioRecorder {

    /** AAC sample rate. Cloud STT accepts 16 kHz mono cleanly (arch §1.2). */
    public static final int SAMPLE_RATE_HZ = 16_000;
    /** Mono — lectures are single-source; halves the upload vs. stereo. */
    public static final int CHANNEL_COUNT = 1;
    /** AAC-LC at 16 kHz mono; 32 kbps is transparent for speech. */
    public static final int BIT_RATE_BPS = 32_000;

    /** Recorder lifecycle as seen by callers. */
    public enum State { IDLE, RECORDING, PAUSED }

    /**
     * Seam over the parts of {@link MediaRecorder} this class uses. The real
     * implementation is {@link MediaRecorderRecorder}; tests supply a fake.
     */
    public interface Recorder {
        /** Configure for M4A/AAC 16 kHz mono, target {@code output}, prepare, and start. */
        void prepareAndStart(@NonNull File output) throws IOException;

        void pause();

        void resume();

        /** Stop capture and finalize the file. */
        void stop();

        /** Free native resources. Safe to call more than once. */
        void release();

        /** Peak amplitude since the previous call, {@code 0..32767}; 0 when idle. */
        int getMaxAmplitude();
    }

    /** Injectable clock so duration accounting is testable. */
    public interface TimeSource {
        long nowMillis();
    }

    private final Recorder recorder;
    private final TimeSource clock;

    private State state = State.IDLE;
    private File outputFile;

    // Duration accounting that survives pause/resume gaps.
    private long segmentStartedAtMs;   // when the current RECORDING segment began
    private long accumulatedMs;        // completed RECORDING segments before the current one

    /**
     * Production constructor — wires a real {@link MediaRecorder}.
     *
     * @param context any context; used to construct the recorder on API 31+.
     */
    public AudioRecorder(@NonNull Context context) {
        this(new MediaRecorderRecorder(context.getApplicationContext()),
                System::currentTimeMillis);
    }

    /**
     * Advanced/test constructor — supply a custom {@link Recorder} (e.g. a fake in
     * unit tests, or an alternate capture backend) and clock.
     */
    @VisibleForTesting
    public AudioRecorder(@NonNull Recorder recorder, @NonNull TimeSource clock) {
        this.recorder = recorder;
        this.clock = clock;
    }

    /**
     * Begin capturing to {@code output} (created/overwritten by the recorder).
     *
     * @throws IllegalStateException if not {@link State#IDLE}.
     * @throws IOException           if the recorder cannot prepare/start.
     */
    public void start(@NonNull File output) throws IOException {
        if (state != State.IDLE) {
            throw new IllegalStateException("start() only valid from IDLE, was " + state);
        }
        recorder.prepareAndStart(output);
        this.outputFile = output;
        this.accumulatedMs = 0L;
        this.segmentStartedAtMs = clock.nowMillis();
        this.state = State.RECORDING;
    }

    /** @throws IllegalStateException if not {@link State#RECORDING}. */
    public void pause() {
        if (state != State.RECORDING) {
            throw new IllegalStateException("pause() only valid from RECORDING, was " + state);
        }
        recorder.pause();
        accumulatedMs += clock.nowMillis() - segmentStartedAtMs;
        state = State.PAUSED;
    }

    /** @throws IllegalStateException if not {@link State#PAUSED}. */
    public void resume() {
        if (state != State.PAUSED) {
            throw new IllegalStateException("resume() only valid from PAUSED, was " + state);
        }
        recorder.resume();
        segmentStartedAtMs = clock.nowMillis();
        state = State.RECORDING;
    }

    /**
     * Stop capture and return the finalized recording.
     *
     * @return the file passed to {@link #start(File)}, now closed and playable.
     * @throws IllegalStateException if IDLE (nothing recording).
     */
    @NonNull
    public Result stop() {
        if (state == State.IDLE) {
            throw new IllegalStateException("stop() called with no active recording");
        }
        if (state == State.RECORDING) {
            accumulatedMs += clock.nowMillis() - segmentStartedAtMs;
        }
        recorder.stop();
        recorder.release();
        File file = outputFile;
        long durationMs = accumulatedMs;
        reset();
        return new Result(file, durationMs);
    }

    /**
     * Abandon the current recording without producing a usable file. Releases
     * native resources; the (possibly partial) output file is left on disk for
     * the caller to delete. Safe to call from any state.
     */
    public void cancel() {
        if (state != State.IDLE) {
            try {
                recorder.stop();
            } catch (RuntimeException ignored) {
                // stop() can throw if start() never captured a frame; ignore on cancel.
            }
        }
        recorder.release();
        reset();
    }

    /** Peak amplitude {@code 0..32767} for the waveform; 0 unless RECORDING. */
    public int getMaxAmplitude() {
        return state == State.RECORDING ? recorder.getMaxAmplitude() : 0;
    }

    /** Elapsed captured time in ms, excluding paused gaps. */
    public long getElapsedMs() {
        if (state == State.RECORDING) {
            return accumulatedMs + (clock.nowMillis() - segmentStartedAtMs);
        }
        return accumulatedMs;
    }

    @NonNull
    public State getState() {
        return state;
    }

    public boolean isRecording() {
        return state == State.RECORDING;
    }

    private void reset() {
        state = State.IDLE;
        outputFile = null;
        segmentStartedAtMs = 0L;
        accumulatedMs = 0L;
    }

    /** Result of a completed recording. */
    public static final class Result {
        @NonNull public final File file;
        public final long durationMs;

        public Result(@NonNull File file, long durationMs) {
            this.file = file;
            this.durationMs = durationMs;
        }
    }

    /**
     * Real {@link Recorder} backed by {@link MediaRecorder}. Kept package-private
     * and free of app logic so the interesting behaviour stays in the testable
     * {@link AudioRecorder} above.
     */
    static final class MediaRecorderRecorder implements Recorder {
        private final Context context;
        private MediaRecorder mr;

        MediaRecorderRecorder(@NonNull Context context) {
            this.context = context;
        }

        @Override
        public void prepareAndStart(@NonNull File output) throws IOException {
            MediaRecorder r = newMediaRecorder();
            r.setAudioSource(MediaRecorder.AudioSource.MIC);
            // AAC/M4A — plays on emulator ExoPlayer; STT gets a LINEAR16 conversion.
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            r.setAudioSamplingRate(SAMPLE_RATE_HZ);
            r.setAudioChannels(CHANNEL_COUNT);
            r.setAudioEncodingBitRate(BIT_RATE_BPS);
            r.setOutputFile(output.getAbsolutePath());
            r.prepare();
            r.start();
            this.mr = r;
        }

        @SuppressWarnings("deprecation")
        @NonNull
        private MediaRecorder newMediaRecorder() {
            // The no-arg constructor is deprecated on API 31+ in favour of the
            // Context overload; branch to avoid the deprecation on modern devices.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return new MediaRecorder(context);
            }
            return new MediaRecorder();
        }

        @Override
        public void pause() {
            mr.pause();
        }

        @Override
        public void resume() {
            mr.resume();
        }

        @Override
        public void stop() {
            mr.stop();
        }

        @Override
        public void release() {
            if (mr != null) {
                mr.release();
                mr = null;
            }
        }

        @Override
        public int getMaxAmplitude() {
            return mr != null ? mr.getMaxAmplitude() : 0;
        }
    }
}
