package com.lecturelens.data.repository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.lecturelens.domain.model.Notes;

import org.junit.Test;

public class LocalExtractiveSummarizerTest {

    @Test
    public void summarize_prefersCuePhrasesAndBuildsKeyTerms() {
        String transcript = "Welcome everyone. Today we will cover recursion. "
                + "Important: remember the base case stops the recursion. "
                + "For example, factorial of zero is one. "
                + "Homework is due Friday on chapter five. "
                + "The exam will include tree traversals and memoization techniques. "
                + "In other words, store previous answers to avoid recomputation.";
        Notes notes = LocalExtractiveSummarizer.summarize(42L, transcript);
        assertTrue(notes.getSummary().contains("On-device summary"));
        assertFalse(notes.getKeyTerms().isEmpty());
        assertFalse(notes.getActionItems().isEmpty());
    }

    @Test
    public void answerLocally_usesQuestionKeywordsAndChat() {
        String answer = LocalExtractiveSummarizer.answerLocally(
                "What is the base case?",
                "Recursion needs a base case. Trees have leaves. Sorting is O(n log n).",
                "USER: Tell me about recursion\nASSISTANT: Recursion calls itself.\n");
        assertTrue(answer.toLowerCase().contains("base case")
                || answer.toLowerCase().contains("recursion"));
        assertTrue(answer.contains("On-device"));
    }
}
