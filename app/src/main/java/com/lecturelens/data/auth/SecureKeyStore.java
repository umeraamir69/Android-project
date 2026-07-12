package com.lecturelens.data.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.lecturelens.domain.repository.CredentialsStore;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Track 1 — {@link CredentialsStore} on EncryptedSharedPreferences
 * (WORK_BREAKDOWN, Track 1 Auth; arch doc §5). Also the authoritative
 * {@code ConsentGate} consulted by Track 3 before any cloud enqueue.
 *
 * <p>Prefs are created lazily and all accessors touch disk — keep calls on
 * {@code AppExecutors.diskIO()} or a Worker thread.
 */
@Singleton
public class SecureKeyStore implements CredentialsStore {

    private static final String PREFS_FILE = "lecturelens_secure_prefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_CLOUD_CONSENT = "cloud_consent";

    private final Context context;
    private volatile SharedPreferences prefs;

    @Inject
    public SecureKeyStore(@ApplicationContext @NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public String getEmail() {
        return prefs().getString(KEY_EMAIL, "");
    }

    @Override
    public void setEmail(@NonNull String email) {
        prefs().edit().putString(KEY_EMAIL, email).apply();
    }

    @NonNull
    @Override
    public String getApiKey() {
        return prefs().getString(KEY_API_KEY, "");
    }

    @Override
    public void setApiKey(@NonNull String apiKey) {
        prefs().edit().putString(KEY_API_KEY, apiKey).apply();
    }

    @Override
    public boolean hasCloudConsent() {
        return prefs().getBoolean(KEY_CLOUD_CONSENT, false);
    }

    @Override
    public void setCloudConsent(boolean granted) {
        prefs().edit().putBoolean(KEY_CLOUD_CONSENT, granted).apply();
    }

    @Override
    public boolean isSignedIn() {
        return !getEmail().isEmpty() && !getApiKey().isEmpty();
    }

    @NonNull
    private SharedPreferences prefs() {
        SharedPreferences local = prefs;
        if (local == null) {
            synchronized (this) {
                local = prefs;
                if (local == null) {
                    local = create();
                    prefs = local;
                }
            }
        }
        return local;
    }

    @NonNull
    private SharedPreferences create() {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Couldn't create encrypted prefs", e);
        }
    }
}
