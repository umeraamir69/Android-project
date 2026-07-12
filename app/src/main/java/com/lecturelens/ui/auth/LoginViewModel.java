package com.lecturelens.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.repository.DatabaseSeeder;
import com.lecturelens.domain.repository.CredentialsStore;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Track 1 — login: email + Google API key + cloud-processing consent
 * (WORK_BREAKDOWN Track 1 Auth). Consent is optional at sign-in; without it
 * recordings are saved locally but never uploaded (arch doc §1.1).
 *
 * <p>On success the store is written, the demo course/lecture is seeded on
 * first run, and {@link #getSignedIn()} fires (login is popped from the back
 * stack, so re-emission after navigation is moot).
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
            prefill.postValue(new Prefill(
                    credentials.getEmail(),
                    credentials.getApiKey(),
                    credentials.hasCloudConsent()));
            // Already signed in → skip the login form and go straight to the
            // Library (the nav action pops login off the back stack). Sign-out
            // isn't a feature yet; when it is, it just clears the store.
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

        boolean valid = true;
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            emailError.setValue("Enter a valid email");
            valid = false;
        } else {
            emailError.setValue(null);
        }
        if (cleanKey.isEmpty()) {
            apiKeyError.setValue("Enter your Google API key");
            valid = false;
        } else {
            apiKeyError.setValue(null);
        }
        if (!valid) {
            return;
        }

        loading.setValue(true);
        executors.diskIO().execute(() -> {
            credentials.setEmail(cleanEmail);
            credentials.setApiKey(cleanKey);
            credentials.setCloudConsent(consent);
            seeder.seedIfEmpty();
            loading.postValue(false);
            signedIn.postValue(true);
        });
    }
}
