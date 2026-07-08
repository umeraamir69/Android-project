package com.lecturelens.ui.upload;

import androidx.annotation.NonNull;

/**
 * Tiny periodic-callback seam so {@link UploadViewModel}'s timer/waveform loop
 * is unit-testable without a Looper. Production impl: {@link HandlerTicker}.
 */
public interface Ticker {
    /** Begin invoking {@code onTick} every {@code intervalMs}. Replaces any prior loop. */
    void start(@NonNull Runnable onTick, long intervalMs);

    /** Stop invoking; safe to call when not started. */
    void stop();
}
