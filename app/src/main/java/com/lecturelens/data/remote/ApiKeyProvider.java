package com.lecturelens.data.remote;

import androidx.annotation.NonNull;

import com.lecturelens.BuildConfig;
import com.lecturelens.domain.repository.CredentialsStore;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Track 4 — cloud credentials for Retrofit clients.
 *
 * <p><b>testing branch:</b> prefer {@code local.properties} → BuildConfig keys
 * so STT and Gemini each get the correct key. Login's single stored key is only
 * a fallback when BuildConfig is empty (production Track 1 UX).
 */
@Singleton
public class ApiKeyProvider {

    public static final String GCP_PROJECT_ID = "859176545805";
    /** Alias that tracks the current free-tier Flash model for AI Studio keys. */
    public static final String GEMINI_MODEL_FLASH = "gemini-flash-latest";
    public static final String STT_MODEL = "latest_long";

    private final CredentialsStore credentials;

    @Inject
    public ApiKeyProvider(@NonNull CredentialsStore credentials) {
        this.credentials = credentials;
    }

    @NonNull
    public String getSpeechToTextApiKey() {
        String fromBuild = nullToEmpty(BuildConfig.STT_API_KEY);
        if (!fromBuild.isEmpty()) {
            return fromBuild;
        }
        return nullToEmpty(credentials.getApiKey());
    }

    /**
     * Prefer {@code GEMINI_API_KEY} from local.properties when set (testing),
     * otherwise the key saved in Settings / Login.
     */
    @NonNull
    public String getGeminiApiKey() {
        String fromBuild = nullToEmpty(BuildConfig.GEMINI_API_KEY);
        if (!fromBuild.isEmpty()) {
            return fromBuild;
        }
        return nullToEmpty(credentials.getApiKey());
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
    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
