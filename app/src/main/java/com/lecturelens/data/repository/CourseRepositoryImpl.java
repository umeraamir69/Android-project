package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lecturelens.domain.model.Course;
import com.lecturelens.domain.repository.CourseRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Track 2 (Daniel).
 *
 * IN-MEMORY STUB — backs the Library UI until Track 1 lands Room. The class
 * name and Hilt binding are final; only the internals change when
 * {@code CourseDao} exists.
 *
 * TODO(Track 2): replace the in-memory list with CourseDao once Track 1's
 * Room PR merges (reads become {@code dao.observeAll()}, insert becomes
 * {@code dao.insert(entity)}).
 */
@Singleton
public class CourseRepositoryImpl implements CourseRepository {

    private final Object lock = new Object();
    private final List<Course> courses = new ArrayList<>(DevSeed.courses());
    private final MutableLiveData<List<Course>> liveCourses =
            new MutableLiveData<>(Collections.unmodifiableList(new ArrayList<>(courses)));
    private long nextId;

    @Inject
    public CourseRepositoryImpl() {
        long maxId = 0;
        for (Course course : courses) {
            maxId = Math.max(maxId, course.getId());
        }
        nextId = maxId + 1;
    }

    @NonNull
    @Override
    public LiveData<List<Course>> observeAll() {
        return liveCourses;
    }

    @Override
    public long insert(@NonNull Course course) {
        synchronized (lock) {
            long id = nextId++;
            courses.add(new Course(id, course.getName(), course.getColor(),
                    course.getCreatedAt()));
            liveCourses.postValue(Collections.unmodifiableList(new ArrayList<>(courses)));
            return id;
        }
    }
}
