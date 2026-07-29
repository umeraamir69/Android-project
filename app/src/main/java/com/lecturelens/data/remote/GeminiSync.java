package com.lecturelens.data.remote;

/** Global lock so all Gemini HTTP calls (notes, Q&A, OCR) stay one-at-a-time. */
public final class GeminiSync {
    public static final Object LOCK = new Object();

    private GeminiSync() {
    }
}
