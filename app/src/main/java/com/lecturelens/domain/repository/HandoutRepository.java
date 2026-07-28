package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.lecturelens.domain.model.Handout;

import java.io.File;
import java.util.List;

/** Lecture handout photos + OCR text for fuller AI context. */
public interface HandoutRepository {

    interface HandoutCallback {
        void onAdded(@NonNull Handout handout);

        void onError(@NonNull String message);
    }

    @NonNull
    LiveData<List<Handout>> observeHandouts(long lectureId);

    void addHandoutImage(long lectureId,
                         @NonNull File imageFile,
                         @NonNull String mimeType,
                         @NonNull HandoutCallback callback);
}
