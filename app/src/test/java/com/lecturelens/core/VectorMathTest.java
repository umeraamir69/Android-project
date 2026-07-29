package com.lecturelens.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VectorMathTest {

    @Test
    public void cosine_identicalVectors_isOne() {
        float[] a = {1f, 0f, 0f};
        float[] b = {1f, 0f, 0f};
        assertEquals(1f, VectorMath.cosineSimilarity(a, b), 1e-5);
    }

    @Test
    public void cosine_orthogonal_isZero() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};
        assertEquals(0f, VectorMath.cosineSimilarity(a, b), 1e-5);
    }

    @Test
    public void roundTrip_bytes() {
        float[] original = {0.1f, -0.5f, 2f};
        float[] restored = VectorMath.fromBytes(VectorMath.toBytes(original));
        assertEquals(original.length, restored.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], restored[i], 1e-6);
        }
    }

    @Test
    public void formatTimestamp() {
        assertEquals("0:05", VectorMath.formatTimestamp(5000));
        assertEquals("1:01", VectorMath.formatTimestamp(61_000));
        assertTrue(VectorMath.formatTimestamp(0).startsWith("0:"));
    }
}
