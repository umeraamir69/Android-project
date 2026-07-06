package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.lecturelens.core.Result;
import com.lecturelens.domain.model.Notes;

/**
 * FROZEN Day 0 contract — signature changes require a team sync.
 *
 * Implementation: Track 4 ({@code LlmRepositoryImpl}, Gemini). Map-reduce
 * chunking for long transcripts lives in GenerateNotesUseCase, not here.
 */
public interface LlmRepository {

    /**
     * Blocking summarization call — call ONLY from a Worker thread
     * (SummarizeWorker). Persists Notes to Room before returning.
     */
    @NonNull
    Result<Notes> summarize(long lectureId, @NonNull String transcriptText);

    @NonNull
    LiveData<Notes> observeNotes(long lectureId);
}
