package com.lecturelens.di;

import com.lecturelens.data.repository.CourseRepositoryImpl;
import com.lecturelens.data.repository.EmbeddingRepositoryImpl;
import com.lecturelens.data.repository.LectureRepositoryFacade;
import com.lecturelens.data.repository.LlmRepositoryImpl;
import com.lecturelens.data.repository.NotesQaRepositoryImpl;
import com.lecturelens.data.repository.TranscriptionRepositoryImpl;
import com.lecturelens.domain.repository.CourseRepository;
import com.lecturelens.domain.repository.EmbeddingRepository;
import com.lecturelens.domain.repository.HandoutRepository;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.LlmRepository;
import com.lecturelens.domain.repository.NotesQaRepository;
import com.lecturelens.domain.repository.TranscriptionRepository;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Repository bindings — KEEP ENTRIES ALPHABETIZED to minimize merge conflicts.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    abstract CourseRepository bindCourseRepository(CourseRepositoryImpl impl);

    @Binds
    abstract EmbeddingRepository bindEmbeddingRepository(EmbeddingRepositoryImpl impl);

    @Binds
    abstract HandoutRepository bindHandoutRepository(NotesQaRepositoryImpl impl);

    @Binds
    abstract LectureRepository bindLectureRepository(LectureRepositoryFacade facade);

    @Binds
    abstract LlmRepository bindLlmRepository(LlmRepositoryImpl impl);

    @Binds
    abstract NotesQaRepository bindNotesQaRepository(NotesQaRepositoryImpl impl);

    @Binds
    abstract TranscriptionRepository bindTranscriptionRepository(TranscriptionRepositoryImpl impl);
}
