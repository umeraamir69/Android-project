package com.lecturelens.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Handout photo / PDF / doc attached to a lecture (local path + OCR + optional cloud URL). */
public final class Handout {

    public final long id;
    public final long lectureId;
    @NonNull public final String localPath;
    @NonNull public final String mimeType;
    @NonNull public final String displayName;
    @NonNull public final String extractedText;
    @Nullable public final String remoteUrl;
    public final long createdAt;

    public Handout(long id,
                   long lectureId,
                   @NonNull String localPath,
                   @NonNull String mimeType,
                   @NonNull String displayName,
                   @NonNull String extractedText,
                   @Nullable String remoteUrl,
                   long createdAt) {
        this.id = id;
        this.lectureId = lectureId;
        this.localPath = localPath;
        this.mimeType = mimeType != null ? mimeType : "";
        this.displayName = displayName != null ? displayName : "";
        this.extractedText = extractedText != null ? extractedText : "";
        this.remoteUrl = remoteUrl;
        this.createdAt = createdAt;
    }

    /** @deprecated use {@link #localPath} */
    @Deprecated
    @NonNull
    public String getImagePath() {
        return localPath;
    }

    public boolean isImage() {
        return mimeType.startsWith("image/");
    }

    public boolean isPdf() {
        return "application/pdf".equalsIgnoreCase(mimeType);
    }
}
