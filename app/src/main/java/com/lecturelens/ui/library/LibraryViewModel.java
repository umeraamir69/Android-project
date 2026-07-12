package com.lecturelens.ui.library;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.lecturelens.core.BaseViewModel;
import com.lecturelens.domain.model.Course;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.repository.CourseRepository;
import com.lecturelens.domain.repository.LectureRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Track 2 (Daniel). Combines the course and lecture streams into
 * {@code UiState<List<CourseSection>>}, tracking per-course expand/collapse.
 *
 * Sections default to expanded; {@link #toggleCourse} collapses/expands.
 */
@HiltViewModel
public class LibraryViewModel extends BaseViewModel<List<CourseSection>> {

    /**
     * Synthetic course id for lectures whose course_id matches no course
     * (e.g. recordings started without a course context — upload's nav arg
     * defaults to -1). Rendered as "Uncategorized" by CoursesAdapter so they
     * are never silently dropped from the Library.
     */
    public static final long UNCATEGORIZED_COURSE_ID = -1L;

    private static final int UNCATEGORIZED_COLOR = 0xFF6F7976; // neutral variant

    private final LiveData<List<Course>> courses;
    private final LiveData<List<Lecture>> lectures;

    private final Observer<List<Course>> courseObserver;
    private final Observer<List<Lecture>> lectureObserver;

    private final Set<Long> collapsedCourseIds = new HashSet<>();

    @Nullable private List<Course> latestCourses;
    @Nullable private List<Lecture> latestLectures;

    @Inject
    public LibraryViewModel(@NonNull CourseRepository courseRepository,
                            @NonNull LectureRepository lectureRepository) {
        setLoading();
        courses = courseRepository.observeAll();
        lectures = lectureRepository.observeAll();

        courseObserver = value -> {
            latestCourses = value;
            rebuild();
        };
        lectureObserver = value -> {
            latestLectures = value;
            rebuild();
        };
        // observeForever because ViewModels have no LifecycleOwner; removed
        // in onCleared. Room-backed LiveData works identically here.
        courses.observeForever(courseObserver);
        lectures.observeForever(lectureObserver);
    }

    public void toggleCourse(long courseId) {
        if (!collapsedCourseIds.remove(courseId)) {
            collapsedCourseIds.add(courseId);
        }
        rebuild();
    }

    private void rebuild() {
        if (latestCourses == null || latestLectures == null) {
            return; // still waiting on first emission from one source
        }
        Map<Long, List<Lecture>> byCourse = new HashMap<>();
        for (Lecture lecture : latestLectures) {
            List<Lecture> group = byCourse.get(lecture.getCourseId());
            if (group == null) {
                group = new ArrayList<>();
                byCourse.put(lecture.getCourseId(), group);
            }
            group.add(lecture);
        }
        List<CourseSection> sections = new ArrayList<>(latestCourses.size() + 1);
        for (Course course : latestCourses) {
            List<Lecture> group = byCourse.remove(course.getId());
            sections.add(new CourseSection(
                    course,
                    group != null ? group : new ArrayList<>(),
                    !collapsedCourseIds.contains(course.getId())));
        }
        // Anything left in byCourse has no matching course row — surface it in
        // an "Uncategorized" section instead of dropping it.
        if (!byCourse.isEmpty()) {
            List<Lecture> orphans = new ArrayList<>();
            for (List<Lecture> group : byCourse.values()) {
                orphans.addAll(group);
            }
            orphans.sort((a, b) -> Long.compare(b.getDate(), a.getDate()));
            Course uncategorized = new Course(
                    UNCATEGORIZED_COURSE_ID, "", UNCATEGORIZED_COLOR, 0L);
            sections.add(new CourseSection(
                    uncategorized,
                    orphans,
                    !collapsedCourseIds.contains(UNCATEGORIZED_COURSE_ID)));
        }
        setSuccess(sections);
    }

    @Override
    protected void onCleared() {
        courses.removeObserver(courseObserver);
        lectures.removeObserver(lectureObserver);
        super.onCleared();
    }
}
