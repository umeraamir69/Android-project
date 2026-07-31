package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.lecturelens.domain.model.AuthUser;

/**
 * Firebase Auth identity — email/password.
 */
public interface AuthRepository {

    interface Callback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    @Nullable
    AuthUser getCurrentUser();

    boolean isSignedIn();

    @NonNull
    LiveData<AuthUser> observeUser();

    void signInWithEmailPassword(@NonNull String email,
                                 @NonNull String password,
                                 @NonNull Callback callback);

    void createAccount(@NonNull String email,
                       @NonNull String password,
                       @NonNull Callback callback);

    void signOut();
}
