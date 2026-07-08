package com.lecturelens.data.repository;

import androidx.annotation.NonNull;

import com.lecturelens.domain.model.Course;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DEV-ONLY seed data shared by the in-memory repository stubs so course ids
 * line up across {@link CourseRepositoryImpl} and
 * {@link LectureReadRepositoryImpl}.
 *
 * TODO(Track 2): delete once Track 1 lands Room + the sign-in seeding
 * described in WORK_BREAKDOWN.md — the DAO-backed impls replace all of this.
 */
final class DevSeed {

    static final long COURSE_CP670 = 1L;
    static final long COURSE_MATH301 = 2L;
    static final long COURSE_HIST220 = 3L;

    private DevSeed() {
    }

    @NonNull
    static List<Course> courses() {
        long now = System.currentTimeMillis();
        List<Course> courses = new ArrayList<>();
        courses.add(new Course(COURSE_CP670, "CP-670 · Mobile App Development",
                0xFF1F4D00, now - TimeUnit.DAYS.toMillis(30)));
        courses.add(new Course(COURSE_MATH301, "MATH-301 · Linear Algebra",
                0xFF2E5D4D, now - TimeUnit.DAYS.toMillis(28)));
        courses.add(new Course(COURSE_HIST220, "HIST-220 · Modern History",
                0xFF56624B, now - TimeUnit.DAYS.toMillis(21)));
        return courses;
    }

    @NonNull
    static List<Lecture> lectures() {
        long now = System.currentTimeMillis();
        List<Lecture> lectures = new ArrayList<>();
        lectures.add(new Lecture(1L, COURSE_CP670, "Week 5 — Fragments & Navigation",
                now - TimeUnit.DAYS.toMillis(8), "/dev/null/week5.m4a",
                TimeUnit.MINUTES.toMillis(48), LectureStatus.READY));
        lectures.add(new Lecture(2L, COURSE_CP670, "Week 6 — Activities & Lifecycle",
                now - TimeUnit.DAYS.toMillis(1), "/dev/null/week6.m4a",
                TimeUnit.MINUTES.toMillis(52), LectureStatus.TRANSCRIBING));
        lectures.add(new Lecture(3L, COURSE_MATH301, "Eigenvalues & Eigenvectors",
                now - TimeUnit.DAYS.toMillis(3), "/dev/null/eigen.m4a",
                TimeUnit.MINUTES.toMillis(75), LectureStatus.SUMMARIZING));
        lectures.add(new Lecture(4L, COURSE_MATH301, "Orthogonality & Least Squares",
                now - TimeUnit.HOURS.toMillis(2), "/dev/null/ortho.m4a",
                TimeUnit.MINUTES.toMillis(60), LectureStatus.RECORDED));
        lectures.add(new Lecture(5L, COURSE_HIST220, "The Interwar Years",
                now - TimeUnit.DAYS.toMillis(5), "/dev/null/interwar.m4a",
                TimeUnit.MINUTES.toMillis(90), LectureStatus.FAILED));
        return lectures;
    }
}
