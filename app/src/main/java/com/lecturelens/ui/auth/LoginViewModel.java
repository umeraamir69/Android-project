package com.lecturelens.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lecturelens.BuildConfig;
import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.repository.DatabaseSeeder;
import com.lecturelens.domain.repository.CredentialsStore;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Track 1 — login: email + Google API key + cloud-processing consent.
 *
 * <p><b>testing branch:</b> if {@code local.properties} has STT/Gemini keys,
 * the API-key field may be left empty — cloud calls use BuildConfig keys.
 * Consent still matters: without it the record pipeline is not enqueued.
 */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    /** Stored values used to prefill the form. */
    public static final class Prefill {
        @NonNull public final String email;
        @NonNull public final String apiKey;
        public final boolean consent;

        Prefill(@NonNull String email, @NonNull String apiKey, boolean consent) {
            this.email = email;
            this.apiKey = apiKey;
            this.consent = consent;
        }
    }

    private final CredentialsStore credentials;
    private final DatabaseSeeder seeder;
    private final AppExecutors executors;

    private final MutableLiveData<Prefill> prefill = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> emailError = new MutableLiveData<>();
    private final MutableLiveData<String> apiKeyError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> signedIn = new MutableLiveData<>(false);

    @Inject
    public LoginViewModel(@NonNull CredentialsStore credentials,
                          @NonNull DatabaseSeeder seeder,
                          @NonNull AppExecutors executors) {
        this.credentials = credentials;
        this.seeder = seeder;
        this.executors = executors;
        // Encrypted prefs are disk-backed — load off the main thread.
        executors.diskIO().execute(() -> {
            String storedKey = credentials.getApiKey();
            // Prefill a hint when local.properties keys are present.
            if (storedKey.isEmpty() && hasLocalDevKeys()) {
                storedKey = "(using local.properties keys)";
            }
            prefill.postValue(new Prefill(
                    credentials.getEmail().isEmpty() ? "tester@lecturelens.dev" : credentials.getEmail(),
                    storedKey,
                    true /* default consent on for easier device testing */));
            if (credentials.isSignedIn()) {
                signedIn.postValue(true);
            }
        });
    }

    @NonNull
    public LiveData<Prefill> getPrefill() {
        return prefill;
    }

    @NonNull
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /** Null = clear the field error. */
    @NonNull
    public LiveData<String> getEmailError() {
        return emailError;
    }

    @NonNull
    public LiveData<String> getApiKeyError() {
        return apiKeyError;
    }

    @NonNull
    public LiveData<Boolean> getSignedIn() {
        return signedIn;
    }

    public void signIn(@Nullable String email, @Nullable String apiKey, boolean consent) {
        String cleanEmail = email == null ? "" : email.trim();
        String cleanKey = apiKey == null ? "" : apiKey.trim();
        if ("(using local.properties keys)".equals(cleanKey)) {
            cleanKey = "";
        }

        boolean valid = true;
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            emailError.setValue("Enter a valid email");
            valid = false;
        } else {
            emailError.setValue(null);
        }
        // testing: allow empty login key when BuildConfig has STT + Gemini keys
        if (cleanKey.isEmpty() && !hasLocalDevKeys()) {
            apiKeyError.setValue("Enter your Google API key (or add keys to local.properties)");
            valid = false;
        } else {
            apiKeyError.setValue(null);
        }
        if (!valid) {
            return;
        }

        loading.setValue(true);
        final String keyToStore = cleanKey;
        final boolean consentToStore = consent;
        executors.diskIO().execute(() -> {
            credentials.setEmail(cleanEmail);
            // Empty means ApiKeyProvider uses BuildConfig STT + Gemini keys.
            credentials.setApiKey(keyToStore);
            credentials.setCloudConsent(consentToStore);
            seeder.seedIfEmpty();
            loading.postValue(false);
            signedIn.postValue(true);
        });
    }

    private static boolean hasLocalDevKeys() {
        return BuildConfig.STT_API_KEY != null && !BuildConfig.STT_API_KEY.isEmpty()
                && BuildConfig.GEMINI_API_KEY != null && !BuildConfig.GEMINI_API_KEY.isEmpty();
    }
}
