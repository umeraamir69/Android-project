package com.lecturelens.data.remote;

import androidx.annotation.NonNull;

import com.lecturelens.BuildConfig;
import com.lecturelens.domain.repository.CredentialsStore;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Track 4 — cloud credentials and project metadata for Retrofit clients.
 *
 * <p>Key resolution (agreed Tracks 1+4): the user's key from
 * {@link CredentialsStore} (entered at login, stored encrypted) wins; the
 * {@code local.properties} → BuildConfig key is the developer fallback so the
 * pipeline still works on dev builds without signing in. Only called from
 * network/worker threads, so the encrypted-prefs disk read is safe here.
 */
@Singleton
public class ApiKeyProvider {

    public static final String GCP_PROJECT_ID = "859176545805";
    public static final String GEMINI_MODEL_FLASH = "gemini-2.0-flash";
    public static final String STT_MODEL = "long";

    private final CredentialsStore credentials;

    @Inject
    public ApiKeyProvider(@NonNull CredentialsStore credentials) {
        this.credentials = credentials;
    }

    @NonNull
    public String getSpeechToTextApiKey() {
        String userKey = credentials.getApiKey();
        return !userKey.isEmpty() ? userKey : nullToEmpty(BuildConfig.STT_API_KEY);
    }

    @NonNull
    public String getGeminiApiKey() {
        String userKey = credentials.getApiKey();
        return !userKey.isEmpty() ? userKey : nullToEmpty(BuildConfig.GEMINI_API_KEY);
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
