package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;

import com.lecturelens.domain.model.SharedNotesPacket;

/**
 * Cloud share of lecture notes (Firebase Firestore + Storage for handout files).
 * Codes are short, human-readable tokens other users can open in the app.
 */
public interface CloudShareRepository {

    interface PublishCallback {
        void onPublished(@NonNull String shareCode);

        void onError(@NonNull String message);
    }

    interface FetchCallback {
        void onFetched(@NonNull SharedNotesPacket packet);

        void onError(@NonNull String message);
    }

    void publish(@NonNull SharedNotesPacket packet, @NonNull PublishCallback callback);

    void fetchByCode(@NonNull String shareCode, @NonNull FetchCallback callback);

    /** Pre-allocate a share code before uploading handout files. */
    @NonNull
    String allocateShareCode();
}
