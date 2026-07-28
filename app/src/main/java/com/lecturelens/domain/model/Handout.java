package com.lecturelens.domain.model;

import androidx.annotation.NonNull;

/** Local handout photo + OCR text for a lecture. */
public final class Handout {

    public final long id;
    public final long lectureId;
    @NonNull public final String imagePath;
    @NonNull public final String extractedText;
    public final long createdAt;

    public Handout(long id,
                   long lectureId,
                   @NonNull String imagePath,
                   @NonNull String extractedText,
                   long createdAt) {
        this.id = id;
        this.lectureId = lectureId;
        this.imagePath = imagePath;
        this.extractedText = extractedText;
        this.createdAt = createdAt;
    }
}
