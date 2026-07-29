package com.lecturelens.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/** Result of a local export ready to share via FileProvider or as plain text. */
public final class ExportResult {

    public enum Kind {
        FILE,
        TEXT
    }

    @NonNull public final Kind kind;
    @Nullable public final File file;
    @Nullable public final String mimeType;
    @Nullable public final String text;
    @NonNull public final String title;
    public final boolean preferWhatsApp;

    private ExportResult(@NonNull Kind kind,
                         @Nullable File file,
                         @Nullable String mimeType,
                         @Nullable String text,
                         @NonNull String title,
                         boolean preferWhatsApp) {
        this.kind = kind;
        this.file = file;
        this.mimeType = mimeType;
        this.text = text;
        this.title = title;
        this.preferWhatsApp = preferWhatsApp;
    }

    @NonNull
    public static ExportResult file(@NonNull File file,
                                    @NonNull String mimeType,
                                    @NonNull String title) {
        return new ExportResult(Kind.FILE, file, mimeType, null, title, false);
    }

    @NonNull
    public static ExportResult text(@NonNull String text,
                                    @NonNull String title,
                                    boolean preferWhatsApp) {
        return new ExportResult(Kind.TEXT, null, "text/plain", text, title, preferWhatsApp);
    }
}
