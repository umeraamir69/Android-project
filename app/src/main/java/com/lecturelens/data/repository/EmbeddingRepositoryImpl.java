package com.lecturelens.data.repository;

import androidx.annotation.NonNull;

import com.lecturelens.core.Result;
import com.lecturelens.domain.repository.EmbeddingRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Track 4 stretch stub — embeddings are a no-op in MVP. */
@Singleton
public class EmbeddingRepositoryImpl implements EmbeddingRepository {

    @Inject
    public EmbeddingRepositoryImpl() {
    }

    @NonNull
    @Override
    public Result<Void> indexLecture(long lectureId) {
        @SuppressWarnings("ConstantConditions")
        Result<Void> ok = Result.success(null);
        return ok;
    }
}
