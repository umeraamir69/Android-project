package com.lecturelens.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.local.entity.TranscriptSegmentEntity;
import com.lecturelens.data.repository.NotesEntityMapper;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.model.SharedNotesPacket;
import com.lecturelens.domain.repository.CloudShareRepository;
import com.lecturelens.domain.repository.LectureRepository;

import java.util.Collections;

import javax.inject.Inject;

/**
 * Fetches a Firebase share code and saves it locally as a lecture (SHARED)
 * with notes + transcript so it appears in Home / Library.
 */
public class ImportSharedNotesUseCase {

    public interface Callback {
        void onImported(long lectureId, @NonNull String title);

        void onError(@NonNull String message);
    }

    private final CloudShareRepository cloudShareRepository;
    private final LectureRepository lectureRepository;
    private final NotesDao notesDao;
    private final TranscriptDao transcriptDao;
    private final NotesEntityMapper notesMapper;
    private final AppExecutors executors;

    @Inject
    public ImportSharedNotesUseCase(@NonNull CloudShareRepository cloudShareRepository,
                                    @NonNull LectureRepository lectureRepository,
                                    @NonNull NotesDao notesDao,
                                    @NonNull TranscriptDao transcriptDao,
                                    @NonNull NotesEntityMapper notesMapper,
                                    @NonNull AppExecutors executors) {
        this.cloudShareRepository = cloudShareRepository;
        this.lectureRepository = lectureRepository;
        this.notesDao = notesDao;
        this.transcriptDao = transcriptDao;
        this.notesMapper = notesMapper;
        this.executors = executors;
    }

    public void execute(@Nullable String code, @NonNull Callback callback) {
        cloudShareRepository.fetchByCode(code != null ? code : "",
                new CloudShareRepository.FetchCallback() {
                    @Override
                    public void onFetched(@NonNull SharedNotesPacket packet) {
                        executors.diskIO().execute(() -> {
                            try {
                                long id = persist(packet);
                                callback.onImported(id, packet.title);
                            } catch (Exception e) {
                                callback.onError(e.getMessage() != null
                                        ? e.getMessage()
                                        : "Couldn't save shared notes");
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        callback.onError(message);
                    }
                });
    }

    private long persist(@NonNull SharedNotesPacket packet) {
        String title = packet.title == null || packet.title.trim().isEmpty()
                ? "Shared notes " + packet.shareCode
                : packet.title.trim();
        Lecture lecture = new Lecture(
                0L,
                -1L,
                title,
                packet.createdAtMs > 0L ? packet.createdAtMs : System.currentTimeMillis(),
                null,
                0L,
                LectureStatus.SHARED);
        long lectureId = lectureRepository.insert(lecture);

        Notes notes = new Notes(
                lectureId,
                packet.summary != null ? packet.summary : "",
                packet.keyTerms,
                packet.actionItems);
        NotesEntity notesEntity = notesMapper.toEntity(notes);
        notesDao.insert(notesEntity);

        String transcriptText = packet.transcript != null ? packet.transcript.trim() : "";
        if (!transcriptText.isEmpty()) {
            TranscriptEntity transcript = new TranscriptEntity();
            transcript.lectureId = lectureId;
            transcript.fullText = transcriptText;
            transcript.language = "und";
            transcript.modelUsed = "shared";
            TranscriptSegmentEntity segment = new TranscriptSegmentEntity();
            segment.lectureId = lectureId;
            segment.startMs = 0L;
            segment.endMs = 0L;
            segment.text = transcriptText;
            segment.speakerTag = 0;
            transcriptDao.replaceTranscript(transcript, Collections.singletonList(segment));
        }
        return lectureId;
    }
}
