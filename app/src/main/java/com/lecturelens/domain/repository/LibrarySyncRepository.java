package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;

/** Firestore sync of courses / lectures / notes / transcript text (no audio). */
public interface LibrarySyncRepository {

    interface Callback {
        void onDone();

        void onError(@NonNull String message);
    }

    /** Push local library for the signed-in user. No-op if signed out. */
    void pushAll(@NonNull Callback callback);

    /** Pull remote library into Room. No-op if signed out. */
    void pullAll(@NonNull Callback callback);

    /** Push one lecture after it becomes READY. */
    void pushLecture(long lectureId);
}
