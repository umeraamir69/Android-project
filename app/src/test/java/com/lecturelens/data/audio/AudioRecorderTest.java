package com.lecturelens.data.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-JVM tests for the {@link AudioRecorder} state machine and duration
 * accounting. No Android framework, no Robolectric: the real MediaRecorder is
 * replaced by {@link FakeRecorder} and time by {@link FakeClock}.
 */
public class AudioRecorderTest {

    private FakeRecorder fake;
    private FakeClock clock;
    private AudioRecorder recorder;
    private File out;

    @Before
    public void setUp() {
        fake = new FakeRecorder();
        clock = new FakeClock(1_000L);
        recorder = new AudioRecorder(fake, clock);
        out = new File("/tmp/lecture.m4a");
    }

    @Test
    public void startsIdle() {
        assertEquals(AudioRecorder.State.IDLE, recorder.getState());
        assertFalse(recorder.isRecording());
    }

    @Test
    public void start_movesToRecording_andStartsRecorder() throws IOException {
        recorder.start(out);
        assertEquals(AudioRecorder.State.RECORDING, recorder.getState());
        assertTrue(recorder.isRecording());
        assertEquals(1, fake.startCount);
        assertSame(out, fake.lastOutput);
    }

    @Test
    public void start_fromNonIdle_throws() throws IOException {
        recorder.start(out);
        assertThrows(IllegalStateException.class, () -> recorder.start(out));
    }

    @Test
    public void pauseResume_transitionsCorrectly() throws IOException {
        recorder.start(out);
        recorder.pause();
        assertEquals(AudioRecorder.State.PAUSED, recorder.getState());
        assertEquals(1, fake.pauseCount);
        recorder.resume();
        assertEquals(AudioRecorder.State.RECORDING, recorder.getState());
        assertEquals(1, fake.resumeCount);
    }

    @Test
    public void pause_fromIdle_throws() {
        assertThrows(IllegalStateException.class, () -> recorder.pause());
    }

    @Test
    public void resume_fromRecording_throws() throws IOException {
        recorder.start(out);
        assertThrows(IllegalStateException.class, () -> recorder.resume());
    }

    @Test
    public void stop_fromIdle_throws() {
        assertThrows(IllegalStateException.class, () -> recorder.stop());
    }

    @Test
    public void stop_returnsFileAndDuration_andReleases() throws IOException {
        clock.set(1_000L);
        recorder.start(out);
        clock.advance(5_000L);            // 5s of recording
        AudioRecorder.Result result = recorder.stop();

        assertSame(out, result.file);
        assertEquals(5_000L, result.durationMs);
        assertEquals(AudioRecorder.State.IDLE, recorder.getState());
        assertEquals(1, fake.stopCount);
        assertEquals(1, fake.releaseCount);
    }

    @Test
    public void elapsed_excludesPausedGaps() throws IOException {
        clock.set(0L);
        recorder.start(out);
        clock.advance(3_000L);            // record 3s
        recorder.pause();
        clock.advance(10_000L);           // paused 10s — must not count
        recorder.resume();
        clock.advance(2_000L);            // record 2s more
        AudioRecorder.Result result = recorder.stop();

        assertEquals(5_000L, result.durationMs);
    }

    @Test
    public void getElapsedMs_liveWhileRecording() throws IOException {
        clock.set(0L);
        recorder.start(out);
        clock.advance(1_500L);
        assertEquals(1_500L, recorder.getElapsedMs());
    }

    @Test
    public void maxAmplitude_zeroUnlessRecording() throws IOException {
        fake.amplitude = 12_000;
        assertEquals(0, recorder.getMaxAmplitude());   // IDLE
        recorder.start(out);
        assertEquals(12_000, recorder.getMaxAmplitude());
        recorder.pause();
        assertEquals(0, recorder.getMaxAmplitude());    // PAUSED
    }

    @Test
    public void cancel_releasesAndReturnsToIdle_fromAnyState() throws IOException {
        recorder.start(out);
        recorder.cancel();
        assertEquals(AudioRecorder.State.IDLE, recorder.getState());
        assertTrue(fake.releaseCount >= 1);
    }

    // ---- Fakes ----

    private static final class FakeRecorder implements AudioRecorder.Recorder {
        int startCount, pauseCount, resumeCount, stopCount, releaseCount;
        int amplitude;
        File lastOutput;
        final List<String> calls = new ArrayList<>();

        @Override
        public void prepareAndStart(@NonNull File output) {
            startCount++;
            lastOutput = output;
            calls.add("start");
        }

        @Override public void pause() { pauseCount++; calls.add("pause"); }

        @Override public void resume() { resumeCount++; calls.add("resume"); }

        @Override public void stop() { stopCount++; calls.add("stop"); }

        @Override public void release() { releaseCount++; calls.add("release"); }

        @Override public int getMaxAmplitude() { return amplitude; }
    }

    private static final class FakeClock implements AudioRecorder.TimeSource {
        private long now;

        FakeClock(long start) { this.now = start; }

        void set(long v) { this.now = v; }

        void advance(long delta) { this.now += delta; }

        @Override public long nowMillis() { return now; }
    }
}
