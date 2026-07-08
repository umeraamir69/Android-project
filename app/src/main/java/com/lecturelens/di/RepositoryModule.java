package com.lecturelens.di;

import com.lecturelens.data.repository.LectureWriteRepositoryImpl;
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
    // TODO(all tracks): add @Binds methods, alphabetized.

    // Track 3 — write half of LectureRepository.
    // INTEGRATION (Tracks 2+3): LectureRepository is ONE frozen interface. This
    // temporary binding points it at the write-only impl (reads throw). When
    // Track 2's LectureReadRepositoryImpl lands, REPLACE this single line with a
    // facade binding:
    //     @Binds LectureRepository bind(LectureRepositoryFacade facade);
    // where the facade @Injects both halves and delegates reads→Track 2,
    // writes→LectureWriteRepositoryImpl. Do it in one joint PR to avoid a
    // duplicate-binding error.
    @Binds
    abstract LectureRepository bindLectureRepository(LectureWriteRepositoryImpl impl);
}