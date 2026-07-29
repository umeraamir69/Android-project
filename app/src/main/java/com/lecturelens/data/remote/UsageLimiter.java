package com.lecturelens.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Daily caps for cloud STT minutes and Gemini calls (arch doc §8).
 */
@Singleton
public class UsageLimiter {

    public static final int MAX_AUDIO_MINUTES_PER_DAY = 60;
    public static final int MAX_GEMINI_CALLS_PER_DAY = 80;

    private static final String PREFS = "lecturelens_usage";
    private static final String KEY_DAY = "day_key";
    private static final String KEY_AUDIO_MS = "audio_ms";
    private static final String KEY_GEMINI = "gemini_calls";

    private final SharedPreferences prefs;

    @Inject
    public UsageLimiter(@ApplicationContext @NonNull Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized boolean canTranscribeAudio(long durationMs) {
        rollDayIfNeeded();
        long used = prefs.getLong(KEY_AUDIO_MS, 0L);
        long projected = used + Math.max(0L, durationMs);
        return projected <= MAX_AUDIO_MINUTES_PER_DAY * 60_000L;
    }

    public synchronized void recordAudio(long durationMs) {
        rollDayIfNeeded();
        long used = prefs.getLong(KEY_AUDIO_MS, 0L);
        prefs.edit().putLong(KEY_AUDIO_MS, used + Math.max(0L, durationMs)).apply();
    }

    public synchronized boolean canCallGemini() {
        rollDayIfNeeded();
        return prefs.getInt(KEY_GEMINI, 0) < MAX_GEMINI_CALLS_PER_DAY;
    }

    public synchronized void recordGeminiCall() {
        rollDayIfNeeded();
        int used = prefs.getInt(KEY_GEMINI, 0);
        prefs.edit().putInt(KEY_GEMINI, used + 1).apply();
    }

    @NonNull
    public synchronized String statusSummary() {
        rollDayIfNeeded();
        long audioMin = prefs.getLong(KEY_AUDIO_MS, 0L) / 60_000L;
        int gemini = prefs.getInt(KEY_GEMINI, 0);
        return String.format(Locale.US,
                "Today: %d/%d audio min, %d/%d Gemini calls",
                audioMin, MAX_AUDIO_MINUTES_PER_DAY, gemini, MAX_GEMINI_CALLS_PER_DAY);
    }

    private void rollDayIfNeeded() {
        String today = dayKey();
        String stored = prefs.getString(KEY_DAY, "");
        if (!today.equals(stored)) {
            prefs.edit()
                    .putString(KEY_DAY, today)
                    .putLong(KEY_AUDIO_MS, 0L)
                    .putInt(KEY_GEMINI, 0)
                    .apply();
        }
    }

    @NonNull
    private static String dayKey() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }
}
