package com.lecturelens.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

/** Cloud share packet includes handout files for other users. */
public class SharedNotesPacketTest {

    @Test
    public void packet_includesHandouts() {
        SharedHandout file = new SharedHandout(
                "quiz.pdf",
                "application/pdf",
                "Question 1…",
                "https://example.com/quiz.pdf");
        SharedNotesPacket packet = new SharedNotesPacket(
                "ABC123",
                "Week 3",
                "Summary",
                Collections.singletonList("lifecycle"),
                Collections.emptyList(),
                "Transcript",
                "a@b.com",
                "Ada",
                "Concordia",
                "Dr. Turing",
                List.of(file),
                100L);

        assertEquals(1, packet.handouts.size());
        assertTrue(packet.handouts.get(0).hasFile());
        assertEquals("quiz.pdf", packet.handouts.get(0).displayName);
        assertEquals("https://example.com/quiz.pdf", packet.handouts.get(0).downloadUrl);
    }

    @Test
    public void handout_withoutUrl_isTextOnly() {
        SharedHandout textOnly = new SharedHandout("scan", "image/jpeg", "OCR text", "");
        assertFalse(textOnly.hasFile());
        assertEquals("OCR text", textOnly.extractedText);
    }

    @Test
    public void emptyPacket_hasNoHandouts() {
        assertTrue(SharedNotesPacket.empty("X").handouts.isEmpty());
    }
}
