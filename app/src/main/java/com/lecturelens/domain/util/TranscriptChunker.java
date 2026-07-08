package com.lecturelens.domain.util;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

/**
 * Splits long transcript text into chunks for Gemini map-reduce summarization.
 */
public class TranscriptChunker {

    @Inject
    public TranscriptChunker() {
    }
    /** ~3k tokens × 4 chars — single-pass limit for map-reduce gate. */
    public static final int SINGLE_PASS_CHAR_LIMIT = 12_000;

    /** Target chunk size for map stage (~2k tokens). */
    public static final int CHUNK_CHAR_LIMIT = 8_000;

    @NonNull
    public List<String> chunk(@NonNull String text) {
        if (text.length() <= CHUNK_CHAR_LIMIT) {
            return Collections.singletonList(text);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_CHAR_LIMIT, text.length());
            if (end < text.length()) {
                int breakAt = findBreakIndex(text, start, end);
                if (breakAt > start) {
                    end = breakAt;
                }
            }
            String piece = text.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            start = end;
        }
        return chunks.isEmpty() ? Collections.singletonList(text) : chunks;
    }

    public boolean needsMapReduce(@NonNull String text) {
        return text.length() > SINGLE_PASS_CHAR_LIMIT;
    }

    private static int findBreakIndex(@NonNull String text, int start, int end) {
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                return i + 1;
            }
        }
        int space = text.lastIndexOf(' ', end);
        return space > start ? space : end;
    }
}
