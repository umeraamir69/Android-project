package com.lecturelens.data.audio;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * Supplies a fresh, unique target file for a recording. Seam so
 * {@code UploadViewModel} needs no {@code Context} and stays JVM-testable.
 */
public interface AudioFileFactory {
    @NonNull
    File newRecordingFile();
}
