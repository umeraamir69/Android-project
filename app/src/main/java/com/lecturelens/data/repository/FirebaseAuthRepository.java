package com.lecturelens.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.lecturelens.domain.model.AuthUser;
import com.lecturelens.domain.repository.AuthRepository;
import com.lecturelens.domain.repository.CredentialsStore;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class FirebaseAuthRepository implements AuthRepository {

    private static final String PREFS = "lecturelens_auth_pending";
    private static final String KEY_PENDING_EMAIL = "pending_email";
    private static final String CONTINUE_URL =
            "https://lecturelense.firebaseapp.com/finishSignIn";

    private final FirebaseAuth firebaseAuth;
    private final CredentialsStore credentials;
    private final SharedPreferences prefs;
    private final MutableLiveData<AuthUser> userLive = new MutableLiveData<>();

    @Inject
    public FirebaseAuthRepository(@NonNull FirebaseAuth firebaseAuth,
                                  @NonNull CredentialsStore credentials,
                                  @ApplicationContext @NonNull Context context) {
        this.firebaseAuth = firebaseAuth;
        this.credentials = credentials;
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
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
    public void signInWithGoogleIdToken(@NonNull String idToken, @NonNull Callback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(r -> {
                    mirrorEmail(r.getUser());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(msg(e)));
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
    public void sendPasswordlessEmail(@NonNull String email, @NonNull Callback callback) {
        String clean = email.trim();
        ActionCodeSettings settings = ActionCodeSettings.newBuilder()
                .setUrl(CONTINUE_URL)
                .setHandleCodeInApp(true)
                .setAndroidPackageName("com.lecturelens", true, null)
                .build();
        firebaseAuth.sendSignInLinkToEmail(clean, settings)
                .addOnSuccessListener(unused -> {
                    savePendingEmail(clean);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(msg(e)));
    }

    @Override
    public boolean isSignInWithEmailLink(@Nullable String link) {
        return link != null && firebaseAuth.isSignInWithEmailLink(link);
    }

    @Override
    public void completePasswordlessSignIn(@NonNull String email,
                                           @NonNull String emailLink,
                                           @NonNull Callback callback) {
        firebaseAuth.signInWithEmailLink(email.trim(), emailLink)
                .addOnSuccessListener(r -> {
                    prefs.edit().remove(KEY_PENDING_EMAIL).apply();
                    mirrorEmail(r.getUser());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(msg(e)));
    }

    @Override
    public void savePendingEmail(@NonNull String email) {
        prefs.edit().putString(KEY_PENDING_EMAIL, email.trim()).apply();
    }

    @Nullable
    @Override
    public String getPendingEmail() {
        return prefs.getString(KEY_PENDING_EMAIL, null);
    }

    @Override
    public void signOut() {
        firebaseAuth.signOut();
        publish(null);
    }

    /** Prefer deep-link data string when present. */
    @Nullable
    public static String extractLinkFromUri(@Nullable Uri uri) {
        return uri == null ? null : uri.toString();
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
