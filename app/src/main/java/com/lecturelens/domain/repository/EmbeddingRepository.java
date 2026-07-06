package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;

import com.lecturelens.core.Result;

/**
 * FROZEN Day 0 contract — STRETCH feature (RAG semantic search).
 *
 * MVP binds a no-op stub ({@code EmbeddingRepositoryImpl}, Track 4) so the
 * pipeline can enqueue an EmbeddingsWorker unconditionally; the stub returns
 * success without doing anything.
 */
public interface EmbeddingRepository {

    /** Blocking — call ONLY from a Worker thread. No-op in MVP. */
    @NonNull
    Result<Void> indexLecture(long lectureId);
}
