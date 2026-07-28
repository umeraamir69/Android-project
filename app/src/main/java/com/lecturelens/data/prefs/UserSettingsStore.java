package com.lecturelens.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.lecturelens.domain.model.UserProfile;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Lightweight (non-encrypted) prefs for appearance, transcription, and student profile.
 */
@Singleton
public class UserSettingsStore {

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private static final String PREFS = "lecturelens_user_settings";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_LANGUAGE = "stt_language";
    private static final String KEY_PROCESSING = "processing_mode";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_DOB = "date_of_birth";
    private static final String KEY_UNIVERSITY = "university";
    private static final String KEY_PROGRAM = "program";
    private static final String KEY_STUDENT_ID = "student_id";

    public static final String MODE_CLOUD = "cloud";
    public static final String MODE_ON_DEVICE = "on_device";
    public static final String MODE_AUTO = "auto";

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

    @NonNull
    public String getProcessingMode() {
        return prefs.getString(KEY_PROCESSING, MODE_CLOUD);
    }

    public void setProcessingMode(@NonNull String mode) {
        prefs.edit().putString(KEY_PROCESSING, mode).apply();
    }

    @NonNull
    public UserProfile getProfile() {
        return new UserProfile(
                prefs.getString(KEY_USERNAME, ""),
                prefs.getString(KEY_FULL_NAME, ""),
                prefs.getString(KEY_DOB, ""),
                prefs.getString(KEY_UNIVERSITY, ""),
                prefs.getString(KEY_PROGRAM, ""),
                prefs.getString(KEY_STUDENT_ID, ""));
    }

    public void setProfile(@NonNull UserProfile profile) {
        prefs.edit()
                .putString(KEY_USERNAME, profile.username)
                .putString(KEY_FULL_NAME, profile.fullName)
                .putString(KEY_DOB, profile.dateOfBirth)
                .putString(KEY_UNIVERSITY, profile.university)
                .putString(KEY_PROGRAM, profile.program)
                .putString(KEY_STUDENT_ID, profile.studentId)
                .apply();
    }
}
