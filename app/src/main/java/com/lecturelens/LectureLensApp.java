package com.lecturelens;

import android.app.Application;

import androidx.work.WorkManager;

import com.lecturelens.data.prefs.UserSettingsStore;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class LectureLensApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Apply saved light/dark/system preference before first Activity draws.
        new UserSettingsStore(this).applyTheme();
        try {
            WorkManager wm = WorkManager.getInstance(this);
            wm.cancelAllWorkByTag("transcribe");
            wm.cancelAllWorkByTag("summarize");
        } catch (Exception ignored) {
            // WorkManager may not be ready in some test contexts.
        }
    }
}
