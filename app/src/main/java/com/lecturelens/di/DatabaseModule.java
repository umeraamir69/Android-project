package com.lecturelens.di;

import android.content.Context;

import androidx.room.Room;

import com.lecturelens.data.local.LectureLensDatabase;
import com.lecturelens.data.local.dao.ChatDao;
import com.lecturelens.data.local.dao.CourseDao;
import com.lecturelens.data.local.dao.EmbeddingDao;
import com.lecturelens.data.local.dao.HandoutDao;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.SearchDao;
import com.lecturelens.data.local.dao.TranscriptDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Track 1 — provides the persistent {@link LectureLensDatabase} and its DAOs.
 * Replaces the temporary in-memory providers that lived in UploadModule.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public LectureLensDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, LectureLensDatabase.class, LectureLensDatabase.NAME)
                // Dev policy until v1 ships: schema changes wipe rather than
                // migrate. Replace with addMigrations(...) once real user data
                // exists — see version history in LectureLensDatabase.
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    public CourseDao provideCourseDao(LectureLensDatabase db) {
        return db.courseDao();
    }

    @Provides
    public LectureDao provideLectureDao(LectureLensDatabase db) {
        return db.lectureDao();
    }

    @Provides
    public NotesDao provideNotesDao(LectureLensDatabase db) {
        return db.notesDao();
    }

    @Provides
    public SearchDao provideSearchDao(LectureLensDatabase db) {
        return db.searchDao();
    }

    @Provides
    public TranscriptDao provideTranscriptDao(LectureLensDatabase db) {
        return db.transcriptDao();
    }

    @Provides
    public HandoutDao provideHandoutDao(LectureLensDatabase db) {
        return db.handoutDao();
    }

    @Provides
    public EmbeddingDao provideEmbeddingDao(LectureLensDatabase db) {
        return db.embeddingDao();
    }

    @Provides
    public ChatDao provideChatDao(LectureLensDatabase db) {
        return db.chatDao();
    }
}
