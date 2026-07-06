package com.lecturelens.di;

import com.lecturelens.data.repository.CourseRepositoryImpl;
import com.lecturelens.data.repository.LectureReadRepositoryImpl;
import com.lecturelens.domain.repository.CourseRepository;
import com.lecturelens.domain.repository.LectureRepository;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * SKELETON — every track adds its own {@code @Binds} here as
 * implementations land. KEEP ENTRIES ALPHABETIZED to minimize merge
 * conflicts (see WORK_BREAKDOWN.md risks).
 *
 * Expected final shape (abstract class + @Binds abstract methods):
 *   CourseRepository        → CourseRepositoryImpl         (Track 2)
 *   EmbeddingRepository     → EmbeddingRepositoryImpl      (Track 4, stub)
 *   LectureRepository reads → LectureReadRepositoryImpl    (Track 2)
 *   LectureRepository writes→ LectureWriteRepositoryImpl   (Track 3)
 *   LlmRepository           → LlmRepositoryImpl            (Track 4)
 *   TranscriptionRepository → TranscriptionRepositoryImpl  (Track 4)
 *
 * Note: LectureRepository is ONE interface; Tracks 2+3 coordinate on a
 * single @Binds once both impls exist (delegating facade), or bind a
 * combined impl composed of the read/write halves.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    abstract CourseRepository bindCourseRepository(CourseRepositoryImpl impl);

    /**
     * Temporary: read-only stub carries the whole interface until Track 3's
     * write impl lands, then Tracks 2+3 swap in the combined impl together.
     */
    @Binds
    abstract LectureRepository bindLectureRepository(LectureReadRepositoryImpl impl);
}
