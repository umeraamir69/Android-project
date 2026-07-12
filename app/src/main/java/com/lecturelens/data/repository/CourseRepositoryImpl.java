package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.lecturelens.data.local.dao.CourseDao;
import com.lecturelens.data.local.entity.CourseEntity;
import com.lecturelens.domain.model.Course;
import com.lecturelens.domain.repository.CourseRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Track 2 (Daniel) — DAO-backed since Track 1's Room DB landed (previously an
 * in-memory DevSeed stub). Class name and Hilt binding unchanged, as planned.
 */
@Singleton
public class CourseRepositoryImpl implements CourseRepository {

    private final CourseDao dao;

    @Inject
    public CourseRepositoryImpl(@NonNull CourseDao dao) {
        this.dao = dao;
    }

    @NonNull
    @Override
    public LiveData<List<Course>> observeAll() {
        return Transformations.map(dao.observeAll(), CourseRepositoryImpl::toDomain);
    }

    /** Synchronous — call on {@code AppExecutors.diskIO()}. */
    @Override
    public long insert(@NonNull Course course) {
        return dao.insert(toEntity(course));
    }

    // ---- Mapping ----

    @NonNull
    private static List<Course> toDomain(@NonNull List<CourseEntity> entities) {
        List<Course> courses = new ArrayList<>(entities.size());
        for (CourseEntity e : entities) {
            courses.add(new Course(e.id, e.name, e.color, e.createdAt));
        }
        return Collections.unmodifiableList(courses);
    }

    @NonNull
    private static CourseEntity toEntity(@NonNull Course course) {
        CourseEntity e = new CourseEntity();
        e.id = course.getId(); // 0 on fresh insert → Room auto-generates
        e.name = course.getName();
        e.color = course.getColor();
        e.createdAt = course.getCreatedAt();
        return e;
    }
}
