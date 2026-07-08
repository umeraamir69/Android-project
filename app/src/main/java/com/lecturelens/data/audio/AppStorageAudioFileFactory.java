package com.lecturelens.data.audio;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Writes recordings to app-private storage ({@code filesDir/recordings}); no
 * runtime storage permission needed. Track 1 may relocate this if a shared
 * cache/cleanup policy is introduced.
 */
public final class AppStorageAudioFileFactory implements AudioFileFactory {

    private final Context context;

    @Inject
    public AppStorageAudioFileFactory(@ApplicationContext Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public File newRecordingFile() {
        File dir = new File(context.getFilesDir(), "recordings");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return new File(dir, "lecture_" + System.currentTimeMillis() + ".m4a");
    }
}
