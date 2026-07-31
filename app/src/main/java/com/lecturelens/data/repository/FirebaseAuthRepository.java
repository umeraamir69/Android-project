package com.lecturelens.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lecturelens.domain.model.AuthUser;
import com.lecturelens.domain.repository.AuthRepository;
import com.lecturelens.domain.repository.CredentialsStore;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class FirebaseAuthRepository implements AuthRepository {

    private final FirebaseAuth firebaseAuth;
    private final CredentialsStore credentials;
    private final MutableLiveData<AuthUser> userLive = new MutableLiveData<>();

    @Inject
    public FirebaseAuthRepository(@NonNull FirebaseAuth firebaseAuth,
                                  @NonNull CredentialsStore credentials,
                                  @ApplicationContext @NonNull Context context) {
        this.firebaseAuth = firebaseAuth;
        this.credentials = credentials;
        firebaseAuth.addAuthStateListener(auth -> publish(auth.getCurrentUser()));
        publish(firebaseAuth.getCurrentUser());
    }

    @Nullable
    @Override
    public AuthUser getCurrentUser() {
        return map(firebaseAuth.getCurrentUser());
    }

    @Override
    public boolean isSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @NonNull
    @Override
    public LiveData<AuthUser> observeUser() {
        return userLive;
    }

    @Override
    public void signInWithEmailPassword(@NonNull String email,
                                        @NonNull String password,
                                        @NonNull Callback callback) {
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(r -> {
                    mirrorEmail(r.getUser());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(msg(e)));
    }

    @Override
    public void createAccount(@NonNull String email,
                              @NonNull String password,
                              @NonNull Callback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(r -> {
                    mirrorEmail(r.getUser());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(msg(e)));
    }

    @Override
    public void signOut() {
        firebaseAuth.signOut();
        publish(null);
    }

    private void mirrorEmail(@Nullable FirebaseUser user) {
        if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
            credentials.setEmail(user.getEmail());
        }
    }

    private void publish(@Nullable FirebaseUser user) {
        userLive.postValue(map(user));
    }

    @Nullable
    private static AuthUser map(@Nullable FirebaseUser user) {
        if (user == null) {
            return null;
        }
        return new AuthUser(user.getUid(), user.getEmail(), user.getDisplayName());
    }

    @NonNull
    private static String msg(@NonNull Exception e) {
        String m = e.getMessage();
        return m != null && !m.isEmpty() ? m : "Sign-in failed";
    }
}
