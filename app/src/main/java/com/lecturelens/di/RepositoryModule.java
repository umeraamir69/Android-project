package com.lecturelens.di;

import com.lecturelens.data.repository.EmbeddingRepositoryImpl;
import com.lecturelens.data.repository.LectureWriteRepositoryImpl;
import com.lecturelens.data.repository.LlmRepositoryImpl;
import com.lecturelens.data.repository.TranscriptionRepositoryImpl;
import com.lecturelens.domain.repository.EmbeddingRepository;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.LlmRepository;
import com.lecturelens.domain.repository.TranscriptionRepository;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Repository bindings — alphabetized to reduce merge conflicts.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    abstract EmbeddingRepository bindEmbeddingRepository(EmbeddingRepositoryImpl impl);

    // INTEGRATION (Tracks 2+3): replace with LectureRepositoryFacade when reads land.
    @Binds
    abstract LectureRepository bindLectureRepository(LectureWriteRepositoryImpl impl);

    @Binds
    abstract LlmRepository bindLlmRepository(LlmRepositoryImpl impl);

    @Binds
    abstract TranscriptionRepository bindTranscriptionRepository(TranscriptionRepositoryImpl impl);
}
