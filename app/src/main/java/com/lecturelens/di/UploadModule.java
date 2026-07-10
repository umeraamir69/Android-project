package com.lecturelens.di;

import android.content.Context;

import androidx.room.Room;
import androidx.work.WorkManager;

import com.lecturelens.data.audio.AppStorageAudioFileFactory;
import com.lecturelens.data.audio.AudioFileFactory;
import com.lecturelens.data.audio.AudioRecorder;
import com.lecturelens.data.consent.PermissiveConsentGate;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.domain.repository.ConsentGate;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Track 3 (Adeniyi) — DI wiring owned by the Record/Upload vertical. New file, so
 * it doesn't merge-conflict with Track 1's modules.
 *
 * <ul>
 *   <li>{@link WorkManager} — for {@code PipelineOrchestrator}.</li>
 *   <li>{@link AudioRecorder} — constructed with the app context; unscoped so each
 *       {@code UploadViewModel} gets its own recorder.</li>
 *   <li>{@link ConsentGate} → {@link PermissiveConsentGate} (temporary).</li>
 *   <li><b>TEMP</b> {@link UploadTempDatabase} + {@link LectureDao} — in-memory DB
 *       so the graph resolves before Track 1's Room DB lands. REMOVE both temp
 *       @Provides (and delete UploadTempDatabase) once Track 1's DatabaseModule
 *       provides LectureDao.</li>
 * </ul>
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class UploadModule {

    @Binds
    abstract AudioFileFactory bindAudioFileFactory(AppStorageAudioFileFactory impl);

    @Binds
    abstract ConsentGate bindConsentGate(PermissiveConsentGate impl);

    @Provides
    @Singleton
    static WorkManager provideWorkManager(@ApplicationContext Context context) {
        return WorkManager.getInstance(context);
    }

    @Provides
    static AudioRecorder provideAudioRecorder(@ApplicationContext Context context) {
        return new AudioRecorder(context);
    }

    // ---- TEMP (Track 1 replaces) ----

    @Provides
    @Singleton
    static UploadTempDatabase provideTempDatabase(@ApplicationContext Context context) {
        return Room.inMemoryDatabaseBuilder(context, UploadTempDatabase.class).build();
    }

    @Provides
    static LectureDao provideLectureDao(UploadTempDatabase db) {
        return db.lectureDao();
    }

    @Provides
    static TranscriptDao provideTranscriptDao(UploadTempDatabase db) {
        return db.transcriptDao();
    }

    @Provides
    static NotesDao provideNotesDao(UploadTempDatabase db) {
        return db.notesDao();
    }
}