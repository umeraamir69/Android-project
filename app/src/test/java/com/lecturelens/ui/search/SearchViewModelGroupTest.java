package com.lecturelens.ui.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.lecturelens.data.local.SearchHit;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/** Search result grouping + source-type filters. */
public class SearchViewModelGroupTest {

    @Test
    public void groupByLecture_insertsHeaders() {
        SearchHit a1 = hit(1L, 10L, 0L, "Lecture A", "one", SearchHit.SOURCE_TRANSCRIPT);
        SearchHit a2 = hit(1L, 11L, 1000L, "Lecture A", "two", SearchHit.SOURCE_NOTES);
        SearchHit b1 = hit(2L, 20L, 0L, "Lecture B", "three", SearchHit.SOURCE_CHAT);

        List<SearchResultsAdapter.ListItem> items =
                SearchViewModel.groupByLecture(Arrays.asList(a1, a2, b1));

        assertEquals(5, items.size());
        assertTrue(items.get(0) instanceof SearchResultsAdapter.HeaderItem);
        assertEquals("Lecture A",
                ((SearchResultsAdapter.HeaderItem) items.get(0)).title);
        assertTrue(items.get(1) instanceof SearchResultsAdapter.HitItem);
        assertTrue(items.get(2) instanceof SearchResultsAdapter.HitItem);
        assertTrue(items.get(3) instanceof SearchResultsAdapter.HeaderItem);
        assertEquals("Lecture B",
                ((SearchResultsAdapter.HeaderItem) items.get(3)).title);
    }

    @Test
    public void applyFilter_all_keepsEverything() {
        List<SearchHit> hits = mixedHits();
        assertEquals(5, SearchViewModel.applyFilter(hits, "ALL").size());
    }

    @Test
    public void applyFilter_transcript_only() {
        List<SearchHit> filtered = SearchViewModel.applyFilter(mixedHits(), "TRANSCRIPT");
        assertEquals(1, filtered.size());
        assertEquals(SearchHit.SOURCE_TRANSCRIPT, filtered.get(0).sourceType);
    }

    @Test
    public void applyFilter_notes_includesKeyTermsAndActions() {
        List<SearchHit> filtered = SearchViewModel.applyFilter(mixedHits(), "NOTES");
        assertEquals(3, filtered.size());
        for (SearchHit hit : filtered) {
            assertTrue(SearchHit.SOURCE_NOTES.equals(hit.sourceType)
                    || SearchHit.SOURCE_KEY_TERM.equals(hit.sourceType)
                    || SearchHit.SOURCE_ACTION.equals(hit.sourceType));
        }
    }

    @Test
    public void applyFilter_chat_only() {
        List<SearchHit> filtered = SearchViewModel.applyFilter(mixedHits(), "CHAT");
        assertEquals(1, filtered.size());
        assertEquals(SearchHit.SOURCE_CHAT, filtered.get(0).sourceType);
    }

    @Test
    public void applyFilter_nullSourceType_treatedAsTranscript() {
        SearchHit hit = hit(1L, 1L, 0L, "A", "x", null);
        List<SearchHit> filtered =
                SearchViewModel.applyFilter(Arrays.asList(hit), "TRANSCRIPT");
        assertEquals(1, filtered.size());
    }

    private static List<SearchHit> mixedHits() {
        return Arrays.asList(
                hit(1L, 1L, 0L, "A", "t", SearchHit.SOURCE_TRANSCRIPT),
                hit(1L, 2L, -1L, "A", "n", SearchHit.SOURCE_NOTES),
                hit(1L, 3L, -1L, "A", "k", SearchHit.SOURCE_KEY_TERM),
                hit(1L, 4L, -1L, "A", "a", SearchHit.SOURCE_ACTION),
                hit(1L, 5L, -1L, "A", "c", SearchHit.SOURCE_CHAT)
        );
    }

    private static SearchHit hit(long lectureId, long segmentId, long startMs,
                                 String title, String snippet, String sourceType) {
        SearchHit hit = new SearchHit();
        hit.lectureId = lectureId;
        hit.segmentId = segmentId;
        hit.startMs = startMs;
        hit.lectureTitle = title;
        hit.snippet = snippet;
        hit.sourceType = sourceType;
        return hit;
    }
}
