package com.lecturelens.ui.library;

import com.lecturelens.domain.model.LectureStatus;

/** Library list status filter. */
public enum LibraryFilter {
    ALL,
    READY,
    FAILED,
    PROCESSING,
    SHARED,
    RECORDED;

    public boolean matches(LectureStatus status) {
        switch (this) {
            case ALL:
                return true;
            case READY:
                return status == LectureStatus.READY;
            case FAILED:
                return status == LectureStatus.FAILED;
            case SHARED:
                return status == LectureStatus.SHARED;
            case RECORDED:
                return status == LectureStatus.RECORDED;
            case PROCESSING:
                return status == LectureStatus.TRANSCRIBING
                        || status == LectureStatus.TRANSCRIBED
                        || status == LectureStatus.SUMMARIZING
                        || status == LectureStatus.INDEXING;
            default:
                return true;
        }
    }
}
