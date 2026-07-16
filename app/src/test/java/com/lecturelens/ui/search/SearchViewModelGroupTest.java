package com.lecturelens.ui.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.lecturelens.data.local.SearchHit;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/** Track 5 — search result grouping dry-run (UC-4). */
public class SearchViewModelGroupTest {

    @Test
    public void groupByLecture_insertsHeaders() {
        SearchHit a1 = hit(1L, 10L, 0L, "Lecture A", "one");
        SearchHit a2 = hit(1L, 11L, 1000L, "Lecture A", "two");
        SearchHit b1 = hit(2L, 20L, 0L, "Lecture B", "three");

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

    private static SearchHit hit(long lectureId, long segmentId, long startMs,
                                 String title, String snippet) {
        SearchHit hit = new SearchHit();
        hit.lectureId = lectureId;
        hit.segmentId = segmentId;
        hit.startMs = startMs;
        hit.lectureTitle = title;
        hit.snippet = snippet;
        return hit;
    }
}
