package com.lecturelens.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * In-memory last pipeline error per lecture (testing UX — no Room migration).
 */
@Singleton
public class PipelineErrorStore {

    private final ConcurrentHashMap<Long, String> errors = new ConcurrentHashMap<>();

    @Inject
    public PipelineErrorStore() {
    }

    public void put(long lectureId, @NonNull String message) {
        errors.put(lectureId, shorten(message));
    }

    public void clear(long lectureId) {
        errors.remove(lectureId);
    }

    @Nullable
    public String get(long lectureId) {
        String message = errors.get(lectureId);
        return message == null ? null : shorten(message);
    }

    /** Keep UI banners short — never store raw API JSON dumps. */
    @NonNull
    static String shorten(@NonNull String message) {
        String trimmed = message.trim();
        String lower = trimmed.toLowerCase(Locale.US);

        if (lower.contains("no speech") || lower.contains("emulator")) {
            return trimmed.length() <= 280 ? trimmed
                    : "No speech detected. Enable the emulator mic or import a file.";
        }
        if (lower.contains("quota") || trimmed.contains("429")
                || lower.contains("resource_exhausted")) {
            return "API quota exceeded. Wait a minute, then retry. "
                    + "Or update the key in local.properties / Settings.";
        }
        if (lower.contains("api key") || lower.contains("permission")
                || lower.contains("403") || lower.contains("401")) {
            return "Cloud API key was rejected. Check STT_API_KEY / Gemini key.";
        }
        if (lower.contains("speech") || lower.contains("transcrib")
                || lower.contains("audio") || lower.contains("stt")) {
            if (trimmed.length() <= 220 && !trimmed.contains("{")) {
                return trimmed;
            }
            return "Transcription failed. Tap Re-transcribe to try again.";
        }
        if (trimmed.length() > 220 || trimmed.contains("{") || trimmed.contains("\"error\"")) {
            return "Processing failed. Tap retry to try again.";
        }
        return trimmed;
    }
}
