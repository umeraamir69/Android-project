package com.lecturelens.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.domain.repository.CredentialsStore;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Track 1 — settings: API-key edit + cloud-consent revocation
 * (WORK_BREAKDOWN Track 1 Auth). Revoking consent stops future pipeline
 * enqueues (RecordLectureUseCase consults the same ConsentGate); it does not
 * cancel work already in flight.
 */
@HiltViewModel
public class SettingsViewModel extends ViewModel {

    /** Current stored settings, loaded off the main thread. */
    public static final class State {
        @NonNull public final String email;
        @NonNull public final String apiKey;
        public final boolean consent;

        State(@NonNull String email, @NonNull String apiKey, boolean consent) {
            this.email = email;
            this.apiKey = apiKey;
            this.consent = consent;
        }
    }

    private final CredentialsStore credentials;
    private final AppExecutors executors;

    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<String> apiKeyError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> keySaved = new MutableLiveData<>(false);

    @Inject
    public SettingsViewModel(@NonNull CredentialsStore credentials,
                             @NonNull AppExecutors executors) {
        this.credentials = credentials;
        this.executors = executors;
        executors.diskIO().execute(() -> state.postValue(new State(
                credentials.getEmail(),
                credentials.getApiKey(),
                credentials.hasCloudConsent())));
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    @NonNull
    public LiveData<String> getApiKeyError() {
        return apiKeyError;
    }

    /** Flips to true after each successful key save; reset via {@link #ackKeySaved}. */
    @NonNull
    public LiveData<Boolean> getKeySaved() {
        return keySaved;
    }

    public void ackKeySaved() {
        keySaved.setValue(false);
    }

    public void saveApiKey(@Nullable String apiKey) {
        String clean = apiKey == null ? "" : apiKey.trim();
        if (clean.isEmpty()) {
            apiKeyError.setValue("Enter your Google API key");
            return;
        }
        apiKeyError.setValue(null);
        executors.diskIO().execute(() -> {
            credentials.setApiKey(clean);
            keySaved.postValue(true);
        });
    }

    public void setCloudConsent(boolean granted) {
        executors.diskIO().execute(() -> credentials.setCloudConsent(granted));
    }
}
