package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;

/**
 * Track 1 — user credentials + consent, persisted encrypted on-device
 * (arch doc §5 "Security & privacy"). Extends {@link ConsentGate} so the
 * same implementation answers Track 3's pre-enqueue consent check.
 *
 * <p>All methods hit EncryptedSharedPreferences (disk) — call on
 * {@code AppExecutors.diskIO()}, not the main thread.
 */
public interface CredentialsStore extends ConsentGate {

    @NonNull
    String getEmail();

    void setEmail(@NonNull String email);

    /** Google API key entered at login; empty when unset. */
    @NonNull
    String getApiKey();

    void setApiKey(@NonNull String apiKey);

    void setCloudConsent(boolean granted);

    /** True once an email + API key have been stored. */
    boolean isSignedIn();
}
