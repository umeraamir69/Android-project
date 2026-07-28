package com.lecturelens.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/** Float vector helpers for RAG cosine search. */
public final class VectorMath {

    private VectorMath() {
    }

    public static float cosineSimilarity(@NonNull float[] a, @NonNull float[] b) {
        int n = Math.min(a.length, b.length);
        if (n == 0) {
            return 0f;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0f;
        }
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }

    @NonNull
    public static byte[] toBytes(@NonNull float[] values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : values) {
            buf.putFloat(v);
        }
        return buf.array();
    }

    @NonNull
    public static float[] fromBytes(@Nullable byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return new float[0];
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] out = new float[bytes.length / 4];
        for (int i = 0; i < out.length; i++) {
            out[i] = buf.getFloat();
        }
        return out;
    }

    @NonNull
    public static String formatTimestamp(long ms) {
        long totalSec = Math.max(0, ms) / 1000L;
        long m = totalSec / 60L;
        long s = totalSec % 60L;
        return String.format(Locale.US, "%d:%02d", m, s);
    }
}
