package com.lecturelens.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** One handout / file included in a cloud share packet. */
public final class SharedHandout {

    @NonNull public final String displayName;
    @NonNull public final String mimeType;
    @NonNull public final String extractedText;
    /** Firebase Storage (or other) download URL; empty when text-only. */
    @NonNull public final String downloadUrl;

    public SharedHandout(@Nullable String displayName,
                         @Nullable String mimeType,
                         @Nullable String extractedText,
                         @Nullable String downloadUrl) {
        this.displayName = displayName != null ? displayName.trim() : "";
        this.mimeType = mimeType != null && !mimeType.isEmpty() ? mimeType : "application/octet-stream";
        this.extractedText = extractedText != null ? extractedText : "";
        this.downloadUrl = downloadUrl != null ? downloadUrl.trim() : "";
    }

    public boolean hasFile() {
        return !downloadUrl.isEmpty();
    }
}
