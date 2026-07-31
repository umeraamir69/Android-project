package com.lecturelens.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.prefs.UserSettingsStore;
import com.lecturelens.data.repository.DatabaseSeeder;
import com.lecturelens.domain.model.UserProfile;
import com.lecturelens.domain.repository.AuthRepository;
import com.lecturelens.domain.repository.CredentialsStore;
import com.lecturelens.domain.repository.LibrarySyncRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CreateAccountViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final CredentialsStore credentials;
    private final UserSettingsStore userSettings;
    private final DatabaseSeeder seeder;
    private final LibrarySyncRepository librarySync;
    private final AppExecutors executors;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> emailError = new MutableLiveData<>();
    private final MutableLiveData<String> passwordError = new MutableLiveData<>();
    private final MutableLiveData<String> usernameError = new MutableLiveData<>();
    private final MutableLiveData<String> universityError = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> signedIn = new MutableLiveData<>(false);

    @Inject
    public CreateAccountViewModel(@NonNull AuthRepository authRepository,
                                  @NonNull CredentialsStore credentials,
                                  @NonNull UserSettingsStore userSettings,
                                  @NonNull DatabaseSeeder seeder,
                                  @NonNull LibrarySyncRepository librarySync,
                                  @NonNull AppExecutors executors) {
        this.authRepository = authRepository;
        this.credentials = credentials;
        this.userSettings = userSettings;
        this.seeder = seeder;
        this.librarySync = librarySync;
        this.executors = executors;
    }

    @NonNull public LiveData<Boolean> getLoading() { return loading; }
    @NonNull public LiveData<String> getEmailError() { return emailError; }
    @NonNull public LiveData<String> getPasswordError() { return passwordError; }
    @NonNull public LiveData<String> getUsernameError() { return usernameError; }
    @NonNull public LiveData<String> getUniversityError() { return universityError; }
    @NonNull public LiveData<String> getStatusMessage() { return statusMessage; }
    @NonNull public LiveData<Boolean> getSignedIn() { return signedIn; }

    public void createAccount(@Nullable String email,
                              @Nullable String password,
                              @Nullable String username,
                              @Nullable String fullName,
                              @Nullable String university,
                              @Nullable String program,
                              @Nullable String studentId,
                              boolean consent) {
        boolean ok = true;
        String cleanEmail = email == null ? "" : email.trim();
        String cleanPassword = password == null ? "" : password;
        String cleanUsername = username == null ? "" : username.trim();
        String cleanUniversity = university == null ? "" : university.trim();

        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            emailError.setValue("Enter a valid email");
            ok = false;
        } else {
            emailError.setValue(null);
        }
        if (cleanPassword.length() < 6) {
            passwordError.setValue("Password must be at least 6 characters");
            ok = false;
        } else {
            passwordError.setValue(null);
        }
        if (cleanUsername.isEmpty()) {
            usernameError.setValue("Enter a username");
            ok = false;
        } else {
            usernameError.setValue(null);
        }
        if (cleanUniversity.isEmpty()) {
            universityError.setValue("Enter your university");
            ok = false;
        } else {
            universityError.setValue(null);
        }
        if (!ok) {
            return;
        }

        UserProfile profile = new UserProfile(
                cleanUsername,
                fullName,
                "",
                cleanUniversity,
                program,
                studentId);

        loading.setValue(true);
        statusMessage.setValue(null);
        authRepository.createAccount(cleanEmail, cleanPassword, new AuthRepository.Callback() {
            @Override
            public void onSuccess() {
                executors.diskIO().execute(() -> {
                    credentials.setCloudConsent(consent);
                    userSettings.setProfile(profile);
                    seeder.seedIfEmpty();
                    librarySync.pullAll(new LibrarySyncRepository.Callback() {
                        @Override
                        public void onDone() {
                            librarySync.pushAll(new LibrarySyncRepository.Callback() {
                                @Override
                                public void onDone() {
                                    loading.postValue(false);
                                    signedIn.postValue(true);
                                }

                                @Override
                                public void onError(@NonNull String message) {
                                    loading.postValue(false);
                                    signedIn.postValue(true);
                                }
                            });
                        }

                        @Override
                        public void onError(@NonNull String message) {
                            loading.postValue(false);
                            signedIn.postValue(true);
                        }
                    });
                });
            }

            @Override
            public void onError(@NonNull String message) {
                loading.postValue(false);
                statusMessage.postValue(message);
            }
        });
    }
}
