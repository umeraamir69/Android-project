package com.lecturelens.di;

import android.content.Context;

import androidx.work.WorkManager;

import com.lecturelens.data.audio.AppStorageAudioFileFactory;
import com.lecturelens.data.audio.AudioFileFactory;
import com.lecturelens.data.audio.AudioRecorder;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Track 3 (Adeniyi) — DI wiring owned by the Record/Upload vertical.
 *
 * <ul>
 *   <li>{@link WorkManager} — for {@code PipelineOrchestrator}.</li>
 *   <li>{@link AudioRecorder} — unscoped so each {@code UploadViewModel} gets its
 *       own recorder.</li>
 * </ul>
 *
 * <p>History: the temporary in-memory Room DB + DAO providers and the
 * {@code PermissiveConsentGate} binding that lived here were removed when
 * Track 1 landed the real {@code DatabaseModule} and {@code AuthModule}
 * (consent now comes from {@code SecureKeyStore}).
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class UploadModule {

    @Binds
    abstract AudioFileFactory bindAudioFileFactory(AppStorageAudioFileFactory impl);

    @Provides
    @Singleton
    static WorkManager provideWorkManager(@ApplicationContext Context context) {
        return WorkManager.getInstance(context);
    }

    @Provides
    static AudioRecorder provideAudioRecorder(@ApplicationContext Context context) {
        return new AudioRecorder(context);
    }
}
