package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lecturelens.data.local.entity.LectureEntity;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Track 1 — LectureEntity ↔ frozen domain model. */
final class LectureEntityMapper {

    private LectureEntityMapper() {
    }

    @Nullable
    static Lecture toDomain(@Nullable LectureEntity e) {
        if (e == null) {
            return null;
        }
        return new Lecture(e.id, e.courseId, e.title, e.date, e.audioPath,
                e.durationMs, statusOf(e.status));
    }

    @NonNull
    static List<Lecture> toDomain(@Nullable List<LectureEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<Lecture> lectures = new ArrayList<>(entities.size());
        for (LectureEntity e : entities) {
            lectures.add(toDomain(e));
        }
        return Collections.unmodifiableList(lectures);
    }

    /** Unknown/legacy status strings degrade to FAILED rather than crashing. */
    @NonNull
    private static LectureStatus statusOf(@NonNull String name) {
        try {
            return LectureStatus.valueOf(name);
        } catch (IllegalArgumentException unknown) {
            return LectureStatus.FAILED;
        }
    }
}
