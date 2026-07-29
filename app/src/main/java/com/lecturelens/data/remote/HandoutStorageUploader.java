package com.lecturelens.data.remote;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.lecturelens.domain.model.AuthUser;
import com.lecturelens.domain.repository.AuthRepository;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Uploads lecture handouts to Firebase Storage.
 * Private: {@code users/{uid}/lectures/{lectureId}/handouts/…}
 * Shared: {@code shared/{shareCode}/handouts/…} (readable via download URL token).
 */
@Singleton
public class HandoutStorageUploader {

    public interface Callback {
        void onUploaded(@NonNull String downloadUrl);

        void onSkipped(@NonNull String reason);

        void onError(@NonNull String message);
    }

    private final FirebaseStorage storage;
    private final AuthRepository authRepository;

    @Inject
    public HandoutStorageUploader(@NonNull FirebaseStorage storage,
                                  @NonNull AuthRepository authRepository) {
        this.storage = storage;
        this.authRepository = authRepository;
    }

    public void upload(long lectureId,
                       long handoutId,
                       @NonNull File file,
                       @Nullable String mimeType,
                       @NonNull Callback callback) {
        AuthUser user = authRepository.getCurrentUser();
        if (user == null || user.uid.isEmpty()) {
            callback.onSkipped("Sign in to sync handouts to the cloud");
            return;
        }
        if (!file.exists()) {
            callback.onError("Handout file missing");
            return;
        }
        String name = file.getName();
        StorageReference ref = storage.getReference()
                .child("users")
                .child(user.uid)
                .child("lectures")
                .child(String.valueOf(lectureId))
                .child("handouts")
                .child(handoutId + "_" + name);
        putAsync(ref, file, mimeType, callback);
    }

    /**
     * Blocking upload into the share bucket so other users can download via URL.
     * Call from a background thread. Returns null if skipped (not signed in).
     */
    @Nullable
    public String uploadSharedBlocking(@NonNull String shareCode,
                                       @NonNull File file,
                                       @Nullable String mimeType,
                                       @NonNull String objectName) throws Exception {
        AuthUser user = authRepository.getCurrentUser();
        if (user == null || user.uid.isEmpty()) {
            return null;
        }
        if (!file.exists()) {
            throw new IllegalStateException("Handout file missing");
        }
        String safeCode = shareCode.trim().toUpperCase(Locale.US);
        StorageReference ref = storage.getReference()
                .child("shared")
                .child(safeCode)
                .child("handouts")
                .child(objectName);
        StorageMetadata.Builder meta = new StorageMetadata.Builder();
        if (mimeType != null && !mimeType.isEmpty()) {
            meta.setContentType(mimeType);
        }
        Tasks.await(ref.putFile(Uri.fromFile(file), meta.build()), 120, TimeUnit.SECONDS);
        Uri uri = Tasks.await(ref.getDownloadUrl(), 60, TimeUnit.SECONDS);
        return uri != null ? uri.toString() : null;
    }

    private void putAsync(@NonNull StorageReference ref,
                          @NonNull File file,
                          @Nullable String mimeType,
                          @NonNull Callback callback) {
        StorageMetadata.Builder meta = new StorageMetadata.Builder();
        if (mimeType != null && !mimeType.isEmpty()) {
            meta.setContentType(mimeType);
        }
        ref.putFile(Uri.fromFile(file), meta.build())
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Upload failed");
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> callback.onUploaded(uri.toString()))
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Firebase Storage upload failed"));
    }
}
