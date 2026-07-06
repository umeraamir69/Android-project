package com.lecturelens.core;

/**
 * FROZEN Day 0 contract — WorkManager {@code Data} keys shared by the
 * pipeline orchestrator (Track 3) and the workers (Track 4).
 *
 * Any change after Day 0 is a joint PR between Tracks 1, 3 and 4.
 */
public final class WorkerKeys {

    /** long — id of the lectures row being processed. */
    public static final String KEY_LECTURE_ID = "lecture_id";

    /** String — absolute path of the recorded/imported audio file. */
    public static final String KEY_AUDIO_PATH = "audio_path";

    /** String — BCP-47 language code for STT; default "en-US". */
    public static final String KEY_LANGUAGE = "language";

    /** String — human-readable failure reason (worker output on failure). */
    public static final String KEY_ERROR_MSG = "error_msg";

    /** int 0–100 — published via setProgressAsync, observed by UploadViewModel. */
    public static final String PROGRESS_PERCENT = "progress";

    private WorkerKeys() {
        // No instances.
    }
}
