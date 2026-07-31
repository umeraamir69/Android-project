package com.lecturelens.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.core.BgAsyncTask;
import com.lecturelens.data.prefs.UserSettingsStore;
import com.lecturelens.domain.model.AuthUser;
import com.lecturelens.domain.model.UserProfile;
import com.lecturelens.domain.repository.AuthRepository;
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
        @NonNull public final String appLocale;
        @NonNull public final String processingMode;
        @NonNull public final UserProfile profile;

        State(@NonNull String email,
              @NonNull String apiKey,
              boolean consent,
              @NonNull String themeMode,
              @NonNull String language,
              @NonNull String appLocale,
              @NonNull String processingMode,
              @NonNull UserProfile profile) {
            this.email = email;
            this.apiKey = apiKey;
            this.consent = consent;
            this.themeMode = themeMode;
            this.language = language;
            this.appLocale = appLocale;
            this.processingMode = processingMode;
            this.profile = profile;
        }
    }

    private final CredentialsStore credentials;
    private final UserSettingsStore userSettings;
    private final AuthRepository authRepository;
    private final AppExecutors executors;
    private final PipelineOrchestrator orchestrator;

    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<String> apiKeyError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> keySaved = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> profileSaved = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> signedOut = new MutableLiveData<>(false);

    @Inject
    public SettingsViewModel(@NonNull CredentialsStore credentials,
                             @NonNull UserSettingsStore userSettings,
                             @NonNull AuthRepository authRepository,
                             @NonNull AppExecutors executors,
                             @NonNull PipelineOrchestrator orchestrator) {
        this.credentials = credentials;
        this.userSettings = userSettings;
        this.authRepository = authRepository;
        this.executors = executors;
        this.orchestrator = orchestrator;
        reload();
    }

    private void reload() {
        BgAsyncTask.run(() -> {
            AuthUser user = authRepository.getCurrentUser();
            String email = user != null && user.email != null && !user.email.isEmpty()
                    ? user.email
                    : credentials.getEmail();
            String displayHint = user != null && user.displayName != null ? user.displayName : "";
            UserProfile profile = userSettings.getProfile();
            if (profile.fullName.isEmpty() && !displayHint.isEmpty()) {
                profile = new UserProfile(
                        profile.username,
                        displayHint,
                        profile.dateOfBirth,
                        profile.university,
                        profile.program,
                        profile.studentId);
            }
            state.postValue(new State(
                    email,
                    credentials.getApiKey(),
                    credentials.hasCloudConsent(),
                    userSettings.getThemeMode(),
                    userSettings.getSttLanguage(),
                    userSettings.getAppLocale(),
                    userSettings.getProcessingMode(),
                    profile));
        });
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

    @NonNull
    public LiveData<Boolean> getProfileSaved() {
        return profileSaved;
    }

    @NonNull
    public LiveData<Boolean> getSignedOut() {
        return signedOut;
    }

    public void ackKeySaved() {
        keySaved.setValue(false);
    }

    public void ackProfileSaved() {
        profileSaved.setValue(false);
    }

    public void saveProfile(@Nullable String username,
                            @Nullable String fullName,
                            @Nullable String dob,
                            @Nullable String university,
                            @Nullable String program,
                            @Nullable String studentId) {
        UserProfile profile = new UserProfile(
                username, fullName, dob, university, program, studentId);
        BgAsyncTask.run(() -> {
            userSettings.setProfile(profile);
            profileSaved.postValue(true);
        });
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

    public void setProcessingMode(@NonNull String mode) {
        userSettings.setProcessingMode(mode);
    }

    public void setAppLocale(@NonNull String languageTag) {
        userSettings.setAppLocale(languageTag);
    }

    public void signOut() {
        authRepository.signOut();
        signedOut.setValue(true);
    }
}
