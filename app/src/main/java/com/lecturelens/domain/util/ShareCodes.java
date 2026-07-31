package com.lecturelens.domain.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/** Shared share-code alphabet / length (no I/O). */
public final class ShareCodes {

    public static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final int LENGTH = 6;

    private ShareCodes() {
    }

    @NonNull
    public static String normalize(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.US);
    }

    public static boolean isValidFormat(@Nullable String raw) {
        String code = normalize(raw);
        if (code.length() != LENGTH) {
            return false;
        }
        for (int i = 0; i < code.length(); i++) {
            if (ALPHABET.indexOf(code.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }
}
