package com.lecturelens.domain.usecase;

import androidx.annotation.NonNull;

import com.lecturelens.core.Result;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.Transcript;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.TranscriptionRepository;

import java.io.File;

import javax.inject.Inject;

/**
 * Track 4 — orchestrates cloud transcription for one saved lecture audio file.
 */
public class TranscribeAudioUseCase {

    private final TranscriptionRepository transcriptionRepository;
    private final LectureRepository lectureRepository;

    @Inject
    public TranscribeAudioUseCase(@NonNull TranscriptionRepository transcriptionRepository,
                                  @NonNull LectureRepository lectureRepository) {
        this.transcriptionRepository = transcriptionRepository;
        this.lectureRepository = lectureRepository;
    }

    @NonNull
    public Result<Transcript> execute(long lectureId,
                                        @NonNull File audioFile,
                                        @NonNull String languageCode) {
        Result<Transcript> result = transcriptionRepository.transcribe(audioFile, languageCode);
        if (result instanceof Result.Error) {
            Result.Error<Transcript> error = (Result.Error<Transcript>) result;
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