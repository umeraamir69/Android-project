package com.lecturelens.data.remote;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Honors HTTP 429 Retry-After with a short sleep + single retry.
 */
public final class RateLimitInterceptor implements Interceptor {

    private static final int MAX_RETRIES = 2;

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        int attempt = 0;
        while (response.code() == 429 && attempt < MAX_RETRIES) {
            long waitMs = parseRetryAfterMs(response);
            response.close();
            try {
                TimeUnit.MILLISECONDS.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during rate-limit backoff", e);
            }
            response = chain.proceed(chain.request());
            attempt++;
        }
        return response;
    }

    private static long parseRetryAfterMs(@NonNull Response response) {
        String header = response.header("Retry-After");
        if (header != null) {
            try {
                return Math.min(30_000L, Long.parseLong(header.trim()) * 1000L);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 1500L * (1L + response.request().url().hashCode() % 3);
    }
}
