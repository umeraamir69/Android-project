package com.lecturelens.ui.library;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.core.BaseViewModel;
import com.lecturelens.core.UiState;
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
 * Combines course + lecture streams into expandable {@link CourseSection}s,
 * and handles create / rename / delete course plus move / rename lecture.
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

    /** Palette cycled when the user adds a new category. */
    private static final int[] COURSE_COLORS = {
            0xFF1F4D00,
            0xFF0B57D0,
            0xFF8B1A1A,
            0xFF7A5900,
            0xFF5B2C6F,
            0xFF0E6655,
    };

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final AppExecutors executors;

    private final LiveData<List<Course>> courses;
    private final LiveData<List<Lecture>> lectures;

    private final Observer<List<Course>> courseObserver;
    private final Observer<List<Lecture>> lectureObserver;

    private final Set<Long> collapsedCourseIds = new HashSet<>();
    @NonNull private LibraryFilter statusFilter = LibraryFilter.ALL;

    @Nullable private List<Course> latestCourses;
    @Nullable private List<Lecture> latestLectures;

    @Inject
    public LibraryViewModel(@NonNull CourseRepository courseRepository,
                            @NonNull LectureRepository lectureRepository,
                            @NonNull AppExecutors executors) {
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.executors = executors;
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
        courses.observeForever(courseObserver);
        lectures.observeForever(lectureObserver);
    }

    public void setStatusFilter(@NonNull LibraryFilter filter) {
        if (statusFilter == filter) {
            return;
        }
        statusFilter = filter;
        rebuild();
    }

    @NonNull
    public LibraryFilter getStatusFilter() {
        return statusFilter;
    }

    /** True if at least one visible section is currently expanded. */
    public boolean hasExpandedSection() {
        UiState<List<CourseSection>> state = getUiState().getValue();
        if (!(state instanceof UiState.Success)) {
            return false;
        }
        for (CourseSection section : ((UiState.Success<List<CourseSection>>) state).data) {
            if (section.isExpanded() && !section.getLectures().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void toggleCourse(long courseId) {
        boolean currentlyExpanded = !collapsedCourseIds.contains(courseId);
        if (currentlyExpanded) {
            collapsedCourseIds.add(courseId);
        } else {
            // Accordion: only one category open at a time.
            collapseAllKnownIds();
            collapsedCourseIds.remove(courseId);
        }
        rebuild();
    }

    /** Expand all if nothing open; otherwise collapse all. */
    public void toggleExpandCollapse() {
        if (hasExpandedSection()) {
            collapseAll();
        } else {
            expandAll();
        }
    }

    /** Opens every category section. */
    public void expandAll() {
        collapsedCourseIds.clear();
        rebuild();
    }

    /** Closes every category section. */
    public void collapseAll() {
        collapseAllKnownIds();
        rebuild();
    }

    private void collapseAllKnownIds() {
        collapsedCourseIds.clear();
        if (latestCourses != null) {
            for (Course course : latestCourses) {
                collapsedCourseIds.add(course.getId());
            }
        }
        collapsedCourseIds.add(UNCATEGORIZED_COURSE_ID);
        if (latestLectures != null) {
            for (Lecture lecture : latestLectures) {
                collapsedCourseIds.add(lecture.getCourseId());
            }
        }
    }

    /** Creates a new category/course. Empty names are ignored. */
    public void addCourse(@NonNull String rawName, @Nullable String professor) {
        String name = rawName.trim();
        if (name.isEmpty()) {
            return;
        }
        String prof = professor != null ? professor.trim() : "";
        executors.diskIO().execute(() -> {
            int colorIndex = latestCourses == null ? 0 : latestCourses.size() % COURSE_COLORS.length;
            Course course = new Course(
                    0L, name, COURSE_COLORS[colorIndex], System.currentTimeMillis(), prof);
            courseRepository.insert(course);
        });
    }

    public void addCourse(@NonNull String rawName) {
        addCourse(rawName, "");
    }

    public void renameCourse(long courseId, @NonNull String rawName, @Nullable String professor) {
        if (courseId == UNCATEGORIZED_COURSE_ID) {
            return;
        }
        String name = rawName.trim();
        if (name.isEmpty()) {
            return;
        }
        String prof = professor != null ? professor.trim() : "";
        executors.diskIO().execute(() -> courseRepository.updateDetails(courseId, name, prof));
    }

    public void renameCourse(long courseId, @NonNull String rawName) {
        renameCourse(courseId, rawName, "");
    }

    /**
     * Deletes a category and moves its lectures to Uncategorized.
     * Uncategorized itself cannot be deleted.
     */
    public void deleteCourse(long courseId) {
        if (courseId == UNCATEGORIZED_COURSE_ID) {
            return;
        }
        executors.diskIO().execute(() -> {
            lectureRepository.clearCourseId(courseId);
            courseRepository.delete(courseId);
        });
    }

    public void moveLecture(long lectureId, long courseId) {
        executors.diskIO().execute(() -> lectureRepository.updateCourseId(lectureId, courseId));
    }

    public void renameLecture(long lectureId, @NonNull String rawTitle) {
        String title = rawTitle.trim();
        if (title.isEmpty()) {
            return;
        }
        executors.diskIO().execute(() -> lectureRepository.updateTitle(lectureId, title));
    }

    /** Snapshot of real courses (excludes Uncategorized) for move-to pickers. */
    @NonNull
    public List<Course> getCoursesSnapshot() {
        if (latestCourses == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(latestCourses);
    }

    private void rebuild() {
        if (latestCourses == null || latestLectures == null) {
            return;
        }
        Map<Long, List<Lecture>> byCourse = new HashMap<>();
        for (Lecture lecture : latestLectures) {
            if (!statusFilter.matches(lecture.getStatus())) {
                continue;
            }
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
            // When filtering, hide empty categories.
            if (statusFilter != LibraryFilter.ALL && (group == null || group.isEmpty())) {
                continue;
            }
            sections.add(new CourseSection(
                    course,
                    group != null ? group : new ArrayList<>(),
                    !collapsedCourseIds.contains(course.getId())));
        }
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
