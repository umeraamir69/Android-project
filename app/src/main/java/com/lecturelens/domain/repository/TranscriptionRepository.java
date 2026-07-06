package com.lecturelens.domain.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.lecturelens.core.Result;
import com.lecturelens.domain.model.Transcript;
import com.lecturelens.domain.model.TranscriptSegment;

import java.io.File;
import java.util.List;

/**
 * FROZEN Day 0 contract — signature changes require a team sync.
 *
 * Implementation: Track 4 ({@code TranscriptionRepositoryImpl}, Google Cloud
 * Speech-to-Text v2 via Retrofit). An on-device implementation may be added
 * in Phase 2 behind this same interface.
 */
public interface TranscriptionRepository {

    /**
     * Blocking cloud transcription — call ONLY from a Worker thread
     * (TranscribeWorker). Persists transcript + segments to Room before
     * returning.
     *
     * @param audio        recorded/imported audio file (M4A/AAC)
     * @param languageCode BCP-47, e.g. "en-US" (see WorkerKeys.KEY_LANGUAGE)
     */
    @NonNull
    Result<Transcript> transcribe(@NonNull File audio, @NonNull String languageCode);

    @NonNull
    LiveData<Transcript> observeTranscript(long lectureId);

    @NonNull
    LiveData<List<TranscriptSegment>> observeSegments(long lectureId);
}
