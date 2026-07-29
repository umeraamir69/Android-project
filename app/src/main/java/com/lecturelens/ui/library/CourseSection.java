package com.lecturelens.ui.library;

import androidx.annotation.NonNull;

import com.lecturelens.domain.model.Course;
import com.lecturelens.domain.model.Lecture;

import java.util.List;
import java.util.Objects;

/**
 * One expandable row group in the Library list: a course header plus its
 * lectures. Built by {@link LibraryViewModel}; purely a UI grouping — not a
 * domain model.
 */
public final class CourseSection {

    @NonNull private final Course course;
    @NonNull private final List<Lecture> lectures;
    private final boolean expanded;

    public CourseSection(@NonNull Course course,
                         @NonNull List<Lecture> lectures,
                         boolean expanded) {
        this.course = course;
        this.lectures = lectures;
        this.expanded = expanded;
    }

    @NonNull
    public Course getCourse() {
        return course;
    }

    @NonNull
    public List<Lecture> getLectures() {
        return lectures;
    }

    public boolean isExpanded() {
        return expanded;
    }

    /** Content comparison for DiffUtil (domain models don't define equals). */
    public boolean contentEquals(@NonNull CourseSection other) {
        if (expanded != other.expanded
                || course.getId() != other.course.getId()
                || !course.getName().equals(other.course.getName())
                || !course.getProfessor().equals(other.course.getProfessor())
                || course.getColor() != other.course.getColor()
                || lectures.size() != other.lectures.size()) {
            return false;
        }
        for (int i = 0; i < lectures.size(); i++) {
            if (!lectureContentEquals(lectures.get(i), other.lectures.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean lectureContentEquals(@NonNull Lecture a, @NonNull Lecture b) {
        return a.getId() == b.getId()
                && a.getCourseId() == b.getCourseId()
                && a.getTitle().equals(b.getTitle())
                && a.getDate() == b.getDate()
                && Objects.equals(a.getAudioPath(), b.getAudioPath())
                && a.getDurationMs() == b.getDurationMs()
                && a.getStatus() == b.getStatus();
    }
}
