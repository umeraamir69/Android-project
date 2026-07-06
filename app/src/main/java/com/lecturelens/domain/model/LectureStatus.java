package com.lecturelens.domain.model;

/**
 * FROZEN Day 0 contract — lecture pipeline lifecycle.
 *
 * Authoritative source: diagrams/01_lecture_processing_state.puml.
 * (Note: INDEXING is in the diagram but was missing from arch doc §3.3;
 * the Day 0 decision froze the 7-value version below.)
 */
public enum LectureStatus {
    RECORDED,
    TRANSCRIBING,
    TRANSCRIBED,
    SUMMARIZING,
    INDEXING,
    READY,
    FAILED
}
