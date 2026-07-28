package com.lecturelens.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.prefs.UserSettingsStore;
import com.lecturelens.domain.repository.CredentialsStore;
import com.lecturelens.processing.PipelineOrchestrator;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends ViewModel {

    public static final class State {
        @NonNull public final String email;
        @NonNull public final String apiKey;
        public final boolean consent;
        @NonNull public final String themeMode;
        @NonNull public final String language;

        State(@NonNull String email,
              @NonNull String apiKey,
              boolean consent,
              @NonNull String themeMode,
              @NonNull String language) {
            this.email = email;
            this.apiKey = apiKey;
            this.consent = consent;
            this.themeMode = themeMode;
            this.language = language;
        }
    }

    private final CredentialsStore credentials;
    private final UserSettingsStore userSettings;
    private final AppExecutors executors;
    private final PipelineOrchestrator orchestrator;

    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<String> apiKeyError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> keySaved = new MutableLiveData<>(false);

    @Inject
    public SettingsViewModel(@NonNull CredentialsStore credentials,
                             @NonNull UserSettingsStore userSettings,
                             @NonNull AppExecutors executors,
                             @NonNull PipelineOrchestrator orchestrator) {
        this.credentials = credentials;
        this.userSettings = userSettings;
        this.executors = executors;
        this.orchestrator = orchestrator;
        reload();
    }

    private void reload() {
        executors.diskIO().execute(() -> state.postValue(new State(
                credentials.getEmail(),
                credentials.getApiKey(),
                credentials.hasCloudConsent(),
                userSettings.getThemeMode(),
                userSettings.getSttLanguage())));
    }

    @NonNull
    public LiveData<State> getState() {
        return state;
    }

    @NonNull
    public LiveData<String> getApiKeyError() {
        return apiKeyError;
    }

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
            apiKeyError.setValue("Enter your Gemini API key");
            return;
        }
        apiKeyError.setValue(null);
        executors.diskIO().execute(() -> {
            credentials.setApiKey(clean);
            orchestrator.cancelAll();
            keySaved.postValue(true);
        });
    }

    public void setCloudConsent(boolean granted) {
        executors.diskIO().execute(() -> credentials.setCloudConsent(granted));
    }

    public void setThemeMode(@NonNull String mode) {
        userSettings.setThemeMode(mode);
        userSettings.applyTheme();
    }

    public void setSttLanguage(@NonNull String languageCode) {
        userSettings.setSttLanguage(languageCode);
    }
}
