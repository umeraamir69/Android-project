package com.lecturelens.di;

import com.lecturelens.domain.repository.EmbeddingRepository;
import com.lecturelens.domain.usecase.GenerateNotesUseCase;
import com.lecturelens.domain.usecase.TranscribeAudioUseCase;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt entry point for {@link androidx.work.Worker} classes (no {@code @HiltWorker} needed).
 */
@EntryPoint
@InstallIn(SingletonComponent.class)
public interface WorkerEntryPoint {

    TranscribeAudioUseCase transcribeAudioUseCase();

    GenerateNotesUseCase generateNotesUseCase();

    EmbeddingRepository embeddingRepository();
}
