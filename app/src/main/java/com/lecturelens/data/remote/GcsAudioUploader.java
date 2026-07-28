package com.lecturelens.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lecturelens.BuildConfig;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Optional GCS media upload for long-running STT.
 * Enabled when {@code GCS_BUCKET} + {@code GCS_OAUTH_TOKEN} are set in local.properties.
 */
@Singleton
public class GcsAudioUploader {

    private static final String TAG = "GcsAudioUploader";
    private static final MediaType LINEAR16 =
            MediaType.parse("application/octet-stream");

    private final OkHttpClient client;

    @Inject
    public GcsAudioUploader(@NonNull OkHttpClient client) {
        this.client = client;
    }

    public boolean isConfigured() {
        return !nullToEmpty(BuildConfig.GCS_BUCKET).isEmpty()
                && !nullToEmpty(BuildConfig.GCS_OAUTH_TOKEN).isEmpty();
    }

    /**
     * Uploads LINEAR16 PCM bytes and returns a {@code gs://} URI.
     */
    @NonNull
    public String uploadLinear16(@NonNull byte[] pcm, int sampleRateHz) throws IOException {
        String bucket = BuildConfig.GCS_BUCKET.trim();
        String token = BuildConfig.GCS_OAUTH_TOKEN.trim();
        String objectName = String.format(Locale.US,
                "lecturelens/%s_%d.raw",
                UUID.randomUUID(), sampleRateHz);
        String url = String.format(Locale.US,
                "https://storage.googleapis.com/upload/storage/v1/b/%s/o"
                        + "?uploadType=media&name=%s",
                bucket, objectName);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(pcm, LINEAR16))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                Log.e(TAG, "GCS upload HTTP " + response.code() + ": " + body);
                throw new IOException("GCS upload failed (HTTP " + response.code() + "). "
                        + "Check GCS_OAUTH_TOKEN / bucket permissions.");
            }
        }
        return "gs://" + bucket + "/" + objectName;
    }

    public void deleteQuietly(@Nullable String gsUri) {
        if (gsUri == null || !gsUri.startsWith("gs://")) {
            return;
        }
        try {
            String withoutScheme = gsUri.substring("gs://".length());
            int slash = withoutScheme.indexOf('/');
            if (slash <= 0) {
                return;
            }
            String bucket = withoutScheme.substring(0, slash);
            String objectName = withoutScheme.substring(slash + 1);
            String url = String.format(Locale.US,
                    "https://storage.googleapis.com/storage/v1/b/%s/o/%s",
                    bucket, objectName.replace("/", "%2F"));
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + BuildConfig.GCS_OAUTH_TOKEN.trim())
                    .delete()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                Log.d(TAG, "GCS delete " + response.code() + " for " + gsUri);
            }
        } catch (Exception e) {
            Log.w(TAG, "GCS delete failed for " + gsUri, e);
        }
    }

    @NonNull
    private static String nullToEmpty(@Nullable String value) {
        return value != null ? value : "";
    }
}
