package com.lecturelens.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Track 5 — FTS query sanitization dry-run. */
public class SearchLecturesUseCaseTest {

    @Test
    public void blankQuery_returnsEmpty() {
        assertEquals("", SearchLecturesUseCase.toFtsQuery(null));
        assertEquals("", SearchLecturesUseCase.toFtsQuery("   "));
        assertEquals("", SearchLecturesUseCase.toFtsQuery("***"));
    }

    @Test
    public void singleToken_getsPrefixStar() {
        assertEquals("lifecycle*", SearchLecturesUseCase.toFtsQuery("lifecycle"));
        assertEquals("life*", SearchLecturesUseCase.toFtsQuery("Life"));
    }

    @Test
    public void multiToken_eachPrefixed() {
        assertEquals("activity* lifecycle*",
                SearchLecturesUseCase.toFtsQuery("activity lifecycle"));
    }

    @Test
    public void stripsFtsOperators() {
        String q = SearchLecturesUseCase.toFtsQuery("life* \"OR\" (cycle)");
        assertTrue(q.contains("life*"));
        assertTrue(q.contains("or*") || q.contains("cycle*"));
        assertTrue(!q.contains("\""));
        assertTrue(!q.contains("("));
    }
}
