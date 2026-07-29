package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;

import com.lecturelens.core.Result;
import com.lecturelens.domain.model.RagCitation;

import java.util.List;

/**
 * Stretch RAG — embed lecture chunks and retrieve by cosine similarity.
 */
public interface EmbeddingRepository {

    /** Blocking — call ONLY from a Worker / background thread. */
    @NonNull
    Result<Boolean> indexLecture(long lectureId);

    /** Embed a free-text query. Blocking. */
    @NonNull
    Result<float[]> embedQuery(@NonNull String text);

    /** Top-k similar chunks for a lecture. Blocking. */
    @NonNull
    Result<List<RagCitation>> searchSimilar(long lectureId, @NonNull float[] queryVector, int topK);
}
