package com.lecturelens.domain.usecase;

import androidx.annotation.NonNull;

import com.lecturelens.core.Result;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.LlmRepository;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.TranscriptEntity;

import javax.inject.Inject;

/**
 * Track 4 — loads the persisted transcript for a lecture and generates notes.
 */
public class GenerateNotesUseCase {

    private final LlmRepository llmRepository;
    private final TranscriptDao transcriptDao;
    private final LectureRepository lectureRepository;

    @Inject
    public GenerateNotesUseCase(@NonNull LlmRepository llmRepository,
                                @NonNull TranscriptDao transcriptDao,
                                @NonNull LectureRepository lectureRepository) {
        this.llmRepository = llmRepository;
        this.transcriptDao = transcriptDao;
        this.lectureRepository = lectureRepository;
    }

    @NonNull
    public Result<Notes> execute(long lectureId) {
        TranscriptEntity transcript = transcriptDao.getTranscriptSync(lectureId);
        if (transcript == null || transcript.fullText.trim().isEmpty()) {
            lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
            return Result.error("Transcript not found for lecture " + lectureId);
        }
        Result<Notes> result = llmRepository.summarize(lectureId, transcript.fullText);
        if (result instanceof Result.Error) {
            Result.Error<Notes> error = (Result.Error<Notes>) result;
            if (!isRetryError(error.message)) {
                lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
            }
        }
        return result;
    }

    private static boolean isRetryError(@NonNull String message) {
        return message.startsWith(RemoteRetryMarkers.CODE_RETRY);
    }
}
