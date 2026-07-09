package com.lecturelens.di;

import com.lecturelens.data.repository.CourseRepositoryImpl;
import com.lecturelens.data.repository.LectureRepositoryFacade;
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
 *   CourseRepository        → CourseRepositoryImpl         (Track 2, done)
 *   EmbeddingRepository     → EmbeddingRepositoryImpl      (Track 4, stub)
 *   LectureRepository       → LectureRepositoryFacade      (Tracks 2+3, done)
 *   LlmRepository           → LlmRepositoryImpl            (Track 4)
 *   TranscriptionRepository → TranscriptionRepositoryImpl  (Track 4)
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    abstract CourseRepository bindCourseRepository(CourseRepositoryImpl impl);

    /**
     * INTEGRATION (Tracks 2+3): LectureRepository is ONE frozen interface,
     * so it gets a single binding — the facade delegates reads to Track 2's
     * LectureReadRepositoryImpl and writes to Track 3's
     * LectureWriteRepositoryImpl.
     */
    @Binds
    abstract LectureRepository bindLectureRepository(LectureRepositoryFacade facade);
}
