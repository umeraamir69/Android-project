package com.lecturelens.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Lightweight (non-encrypted) prefs for appearance and transcription defaults.
 */
@Singleton
public class UserSettingsStore {

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private static final String PREFS = "lecturelens_user_settings";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_LANGUAGE = "stt_language";

    private final SharedPreferences prefs;

    @Inject
    public UserSettingsStore(@ApplicationContext @NonNull Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public String getThemeMode() {
        return prefs.getString(KEY_THEME, THEME_SYSTEM);
    }

    public void setThemeMode(@NonNull String mode) {
        prefs.edit().putString(KEY_THEME, mode).apply();
    }

    /** Maps stored theme to {@link AppCompatDelegate} night mode. */
    public int nightModeFlag() {
        switch (getThemeMode()) {
            case THEME_LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case THEME_DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    public void applyTheme() {
        AppCompatDelegate.setDefaultNightMode(nightModeFlag());
    }

    @NonNull
    public String getSttLanguage() {
        return prefs.getString(KEY_LANGUAGE, "en-US");
    }

    public void setSttLanguage(@NonNull String languageCode) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }
}
