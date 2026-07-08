package com.lecturelens.ui.upload;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

/** Main-thread {@link Ticker} backed by a {@link Handler}. */
public final class HandlerTicker implements Ticker {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable loop;

    @Override
    public void start(@NonNull Runnable onTick, long intervalMs) {
        stop();
        loop = new Runnable() {
            @Override
            public void run() {
                onTick.run();
                handler.postDelayed(this, intervalMs);
            }
        };
        handler.post(loop);
    }

    @Override
    public void stop() {
        if (loop != null) {
            handler.removeCallbacks(loop);
            loop = null;
        }
    }
}
