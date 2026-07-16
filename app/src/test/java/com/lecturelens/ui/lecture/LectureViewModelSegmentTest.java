package com.lecturelens.ui.lecture;

import static org.junit.Assert.assertEquals;

import com.lecturelens.domain.model.TranscriptSegment;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Track 5 — active-segment index dry-run (UC-2 / UC-3). */
public class LectureViewModelSegmentTest {

    private static final List<TranscriptSegment> SEGMENTS = Arrays.asList(
            new TranscriptSegment(1L, 10L, 0L, 45_000L, "a"),
            new TranscriptSegment(2L, 10L, 45_000L, 90_000L, "b"),
            new TranscriptSegment(3L, 10L, 90_000L, 135_000L, "c"));

    @Test
    public void empty_returnsMinusOne() {
        assertEquals(-1, LectureViewModel.findActiveSegmentIndex(
                Collections.emptyList(), 1000L));
    }

    @Test
    public void insideFirstSegment() {
        assertEquals(0, LectureViewModel.findActiveSegmentIndex(SEGMENTS, 0L));
        assertEquals(0, LectureViewModel.findActiveSegmentIndex(SEGMENTS, 44_999L));
    }

    @Test
    public void insideMiddleSegment() {
        assertEquals(1, LectureViewModel.findActiveSegmentIndex(SEGMENTS, 45_000L));
        assertEquals(1, LectureViewModel.findActiveSegmentIndex(SEGMENTS, 60_000L));
    }

    @Test
    public void pastEnd_clampsToLast() {
        assertEquals(2, LectureViewModel.findActiveSegmentIndex(SEGMENTS, 200_000L));
    }

    @Test
    public void negativePosition_returnsMinusOne() {
        assertEquals(-1, LectureViewModel.findActiveSegmentIndex(SEGMENTS, -5L));
    }
}
