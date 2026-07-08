package com.lecturelens.data.remote;

import androidx.annotation.NonNull;

import com.lecturelens.BuildConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Track 4 — cloud credentials and project metadata for Retrofit clients.
 */
@Singleton
public class ApiKeyProvider {

    public static final String GCP_PROJECT_ID = "859176545805";
    public static final String GEMINI_MODEL_FLASH = "gemini-2.0-flash";
    public static final String STT_MODEL = "long";

    @Inject
    public ApiKeyProvider() {
    }

    @NonNull
    public String getSpeechToTextApiKey() {
        return nullToEmpty(BuildConfig.STT_API_KEY);
    }

    @NonNull
    public String getGeminiApiKey() {
        return nullToEmpty(BuildConfig.GEMINI_API_KEY);
    }

    @NonNull
    public String getGcpProjectId() {
        return GCP_PROJECT_ID;
    }

    public boolean hasSpeechToTextKey() {
        return !getSpeechToTextApiKey().isEmpty();
    }

    public boolean hasGeminiKey() {
        return !getGeminiApiKey().isEmpty();
    }

    @NonNull
    private static String nullToEmpty(@NonNull String value) {
        return value != null ? value : "";
    }
}
