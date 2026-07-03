package com.lecturelens.di;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * SKELETON — Track 1 fills this in week 1 when Room lands.
 *
 * Will provide: LectureLensDatabase (Room.databaseBuilder, @Singleton) and
 * each DAO (CourseDao, LectureDao, TranscriptDao, SearchDao) via
 * {@code @Provides} methods reading off the database instance.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {
    // TODO(Track 1, week 1): provide LectureLensDatabase + DAOs.
}
