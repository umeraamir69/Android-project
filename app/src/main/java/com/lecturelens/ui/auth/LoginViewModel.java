package com.lecturelens.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.repository.DatabaseSeeder;
import com.lecturelens.domain.repository.AuthRepository;
import com.lecturelens.domain.repository.CredentialsStore;
import com.lecturelens.domain.repository.LibrarySyncRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Firebase Auth login: email/password. */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final CredentialsStore credentials;
    private final DatabaseSeeder seeder;
    private final LibrarySyncRepository librarySync;
    private final AppExecutors executors;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> emailError = new MutableLiveData<>();
    private final MutableLiveData<String> passwordError = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> signedIn = new MutableLiveData<>(false);

    @Inject
    public LoginViewModel(@NonNull AuthRepository authRepository,
                          @NonNull CredentialsStore credentials,
                          @NonNull DatabaseSeeder seeder,
                          @NonNull LibrarySyncRepository librarySync,
                          @NonNull AppExecutors executors) {
        this.authRepository = authRepository;
        this.credentials = credentials;
        this.seeder = seeder;
        this.librarySync = librarySync;
        this.executors = executors;
        if (authRepository.isSignedIn()) {
            signedIn.setValue(true);
        }
    }

    @NonNull
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    @NonNull
    public LiveData<String> getEmailError() {
        return emailError;
    }

    @NonNull
    public LiveData<String> getPasswordError() {
        return passwordError;
    }

    @NonNull
    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    @NonNull
    public LiveData<Boolean> getSignedIn() {
        return signedIn;
    }

    public void signInWithPassword(@Nullable String email,
                                   @Nullable String password,
                                   boolean consent) {
        if (!validateEmailPassword(email, password, true)) {
            return;
        }
        loading.setValue(true);
        statusMessage.setValue(null);
        authRepository.signInWithEmailPassword(email.trim(), password, afterAuth(consent));
    }

    public void createAccount(@Nullable String email,
                              @Nullable String password,
                              boolean consent) {
        if (!validateEmailPassword(email, password, true)) {
            return;
        }
        if (password.trim().length() < 6) {
            passwordError.setValue("Password must be at least 6 characters");
            return;
        }
        loading.setValue(true);
        statusMessage.setValue(null);
        authRepository.createAccount(email.trim(), password, afterAuth(consent));
    }

    @NonNull
    private AuthRepository.Callback afterAuth(boolean consent) {
        return new AuthRepository.Callback() {
            @Override
            public void onSuccess() {
                executors.diskIO().execute(() -> {
                    credentials.setCloudConsent(consent);
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
        };
    }

    private boolean validateEmail(@Nullable String email) {
        String clean = email == null ? "" : email.trim();
        if (clean.isEmpty() || !clean.contains("@")) {
            emailError.setValue("Enter a valid email");
            return false;
        }
        emailError.setValue(null);
        return true;
    }

    private boolean validateEmailPassword(@Nullable String email,
                                          @Nullable String password,
                                          boolean requirePassword) {
        boolean ok = validateEmail(email);
        String pwd = password == null ? "" : password;
        if (requirePassword && pwd.isEmpty()) {
            passwordError.setValue("Enter your password");
            ok = false;
        } else {
            passwordError.setValue(null);
        }
        return ok;
    }
}
