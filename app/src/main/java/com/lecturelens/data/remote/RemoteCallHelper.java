package com.lecturelens.data.remote;

import androidx.annotation.NonNull;

import java.io.IOException;

import retrofit2.Response;

/**
 * Classifies Retrofit HTTP failures for WorkManager retry decisions.
 */
public final class RemoteCallHelper {

    public static final String CODE_RETRY = "RETRY";

    private RemoteCallHelper() {
    }

    public static boolean isRetryableHttpCode(int code) {
        return code == 429 || code >= 500;
    }

    @NonNull
    public static String readErrorBody(@NonNull Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                if (!body.isEmpty()) {
                    return "HTTP " + response.code() + ": " + body;
                }
            }
        } catch (IOException ignored) {
            // fall through
        }
        return "HTTP " + response.code() + " " + response.message();
    }
}
