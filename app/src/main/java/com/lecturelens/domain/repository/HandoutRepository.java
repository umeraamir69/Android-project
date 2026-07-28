package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.lecturelens.domain.model.Handout;

import java.io.File;
import java.util.List;

/** Lecture handout photos / PDFs / docs + OCR text. */
public interface HandoutRepository {

    interface HandoutCallback {
        void onAdded(@NonNull Handout handout);

        void onError(@NonNull String message);
    }

    @NonNull
    LiveData<List<Handout>> observeHandouts(long lectureId);

    void addHandoutFile(long lectureId,
                        @NonNull File file,
                        @NonNull String mimeType,
                        @Nullable String displayName,
                        @NonNull HandoutCallback callback);

    /** @deprecated use {@link #addHandoutFile} */
    @Deprecated
    default void addHandoutImage(long lectureId,
                                 @NonNull File imageFile,
                                 @NonNull String mimeType,
                                 @NonNull HandoutCallback callback) {
        addHandoutFile(lectureId, imageFile, mimeType, imageFile.getName(), callback);
    }

    void deleteHandout(long handoutId);
}
