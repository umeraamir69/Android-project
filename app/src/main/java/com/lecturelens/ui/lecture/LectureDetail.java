package com.lecturelens.ui.lecture;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.model.TranscriptSegment;

import java.util.Collections;
import java.util.List;

/** Combined lecture-screen payload for {@link LectureViewModel}. */
public final class LectureDetail {

    @NonNull public final Lecture lecture;
    @NonNull public final List<TranscriptSegment> segments;
    @Nullable public final Notes notes;

    public LectureDetail(@NonNull Lecture lecture,
                         @NonNull List<TranscriptSegment> segments,
                         @Nullable Notes notes) {
        this.lecture = lecture;
        this.segments = Collections.unmodifiableList(segments);
        this.notes = notes;
    }

    public boolean hasAudioPath() {
        String path = lecture.getAudioPath();
        return path != null && !path.trim().isEmpty();
    }

    public boolean hasNotesContent() {
        if (notes == null) {
            return false;
        }
        return !notes.getSummary().trim().isEmpty()
                || !notes.getKeyTerms().isEmpty()
                || !notes.getActionItems().isEmpty();
    }
}
