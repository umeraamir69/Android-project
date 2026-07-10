package com.lecturelens.domain.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TranscriptChunkerTest {

    private final TranscriptChunker chunker = new TranscriptChunker();

    @Test
    public void shortText_returnsSingleChunk() {
        String text = "Hello world.";
        List<String> chunks = chunker.chunk(text);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
        assertFalse(chunker.needsMapReduce(text));
    }

    @Test
    public void longText_splitsIntoMultipleChunks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            sb.append("Sentence number ").append(i).append(". ");
        }
        String text = sb.toString();
        List<String> chunks = chunker.chunk(text);
        assertTrue(chunks.size() > 1);
        assertTrue(chunker.needsMapReduce(text));
    }
}
