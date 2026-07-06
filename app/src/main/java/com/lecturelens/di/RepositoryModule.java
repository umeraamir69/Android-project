package com.lecturelens.di;

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
}
