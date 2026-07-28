package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.lecturelens.domain.model.AuthUser;

/**
 * Firebase Auth identity — Google, email/password, and passwordless email link.
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

    void signInWithGoogleIdToken(@NonNull String idToken, @NonNull Callback callback);

    void signInWithEmailPassword(@NonNull String email,
                                 @NonNull String password,
                                 @NonNull Callback callback);

    void createAccount(@NonNull String email,
                       @NonNull String password,
                       @NonNull Callback callback);

    void sendPasswordlessEmail(@NonNull String email, @NonNull Callback callback);

    boolean isSignInWithEmailLink(@Nullable String link);

    void completePasswordlessSignIn(@NonNull String email,
                                    @NonNull String emailLink,
                                    @NonNull Callback callback);

    /** Email saved when sending a magic link (needed when the link opens the app). */
    void savePendingEmail(@NonNull String email);

    @Nullable
    String getPendingEmail();

    void signOut();
}
