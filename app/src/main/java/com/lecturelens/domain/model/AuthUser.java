package com.lecturelens.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Signed-in Firebase user snapshot for UI. */
public final class AuthUser {

    @NonNull public final String uid;
    @Nullable public final String email;
    @Nullable public final String displayName;

    public AuthUser(@NonNull String uid,
                    @Nullable String email,
                    @Nullable String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
    }
}
