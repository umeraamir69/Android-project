package com.lecturelens.ui.lecture;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;

import com.lecturelens.core.BaseViewModel;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.model.TranscriptSegment;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.LlmRepository;
import com.lecturelens.domain.repository.TranscriptionRepository;
import com.lecturelens.domain.usecase.ExportLectureUseCase;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Track 5 — loads lecture + segments + notes and tracks the active transcript
 * segment from ExoPlayer position. Export is delegated to
 * {@link ExportLectureUseCase}.
 */
@HiltViewModel
public class LectureViewModel extends BaseViewModel<LectureDetail> {

    private final long lectureId;
    private final ExportLectureUseCase exportLectureUseCase;

    private final LiveData<Lecture> lectureLive;
    private final LiveData<List<TranscriptSegment>> segmentsLive;
    private final LiveData<Notes> notesLive;

    private final Observer<Lecture> lectureObserver;
    private final Observer<List<TranscriptSegment>> segmentsObserver;
    private final Observer<Notes> notesObserver;

    @Nullable private Lecture latestLecture;
    @Nullable private List<TranscriptSegment> latestSegments;
    @Nullable private Notes latestNotes;
    private boolean lectureResolved;

    private final MutableLiveData<Integer> activeSegmentIndex = new MutableLiveData<>(-1);
    private final MutableLiveData<File> exportFile = new MutableLiveData<>();
    private final MutableLiveData<String> messageEvent = new MutableLiveData<>();

    private long pendingSeekMs = -1L;

    @Inject
    public LectureViewModel(@NonNull SavedStateHandle savedStateHandle,
                            @NonNull LectureRepository lectureRepository,
                            @NonNull TranscriptionRepository transcriptionRepository,
                            @NonNull LlmRepository llmRepository,
                            @NonNull ExportLectureUseCase exportLectureUseCase) {
        Long id = savedStateHandle.get("lectureId");
        this.lectureId = id != null ? id : -1L;
        Long seek = savedStateHandle.get("seekMs");
        this.pendingSeekMs = seek != null ? seek : -1L;
        this.exportLectureUseCase = exportLectureUseCase;

        setLoading();

        lectureLive = lectureRepository.observeById(lectureId);
        segmentsLive = transcriptionRepository.observeSegments(lectureId);
        notesLive = llmRepository.observeNotes(lectureId);

        lectureObserver = value -> {
            lectureResolved = true;
            latestLecture = value;
            rebuild();
        };
        segmentsObserver = value -> {
            latestSegments = value != null ? value : Collections.emptyList();
            rebuild();
        };
        notesObserver = value -> {
            latestNotes = value;
            rebuild();
        };

        lectureLive.observeForever(lectureObserver);
        segmentsLive.observeForever(segmentsObserver);
        notesLive.observeForever(notesObserver);
    }

    @NonNull
    public LiveData<Integer> getActiveSegmentIndex() {
        return activeSegmentIndex;
    }

    @NonNull
    public LiveData<File> getExportFile() {
        return exportFile;
    }

    @NonNull
    public LiveData<String> getMessageEvent() {
        return messageEvent;
    }

    public long getLectureId() {
        return lectureId;
    }

    /** Peek without clearing — used to highlight before the player is ready. */
    public long peekPendingSeekMs() {
        return pendingSeekMs;
    }

    /** Consume once after the player is prepared (search jump / deep link). */
    public long consumePendingSeekMs() {
        long value = pendingSeekMs;
        pendingSeekMs = -1L;
        return value;
    }

    public void onSegmentTapped(long startMs) {
        onPlaybackPosition(startMs);
    }

    public void onPlaybackPosition(long positionMs) {
        List<TranscriptSegment> segments = latestSegments;
        if (segments == null || segments.isEmpty()) {
            activeSegmentIndex.postValue(-1);
            return;
        }
        int index = findActiveSegmentIndex(segments, positionMs);
        Integer current = activeSegmentIndex.getValue();
        if (current == null || current != index) {
            activeSegmentIndex.postValue(index);
        }
    }

    public void export() {
        exportLectureUseCase.execute(lectureId, new ExportLectureUseCase.Callback() {
            @Override
            public void onExported(@NonNull File markdownFile) {
                exportFile.postValue(markdownFile);
            }

            @Override
            public void onError(@NonNull String message) {
                messageEvent.postValue(message);
            }
        });
    }

    private void rebuild() {
        if (!lectureResolved || latestSegments == null) {
            return;
        }
        if (latestLecture == null) {
            setError("Lecture not found");
            return;
        }
        // Notes LiveData may emit an empty Notes(0L,…) before the first real
        // row — treat empty summary+lists as null for UI empty-state.
        Notes notes = latestNotes;
        if (notes != null
                && notes.getSummary().isEmpty()
                && notes.getKeyTerms().isEmpty()
                && notes.getActionItems().isEmpty()) {
            notes = null;
        }
        setSuccess(new LectureDetail(latestLecture, latestSegments, notes));
    }

    /**
     * Returns the index of the segment covering {@code positionMs}, or the
     * last segment if past the end, or {@code -1} if none.
     */
    @VisibleForTesting
    static int findActiveSegmentIndex(@NonNull List<TranscriptSegment> segments,
                                      long positionMs) {
        if (segments.isEmpty() || positionMs < 0L) {
            return -1;
        }
        for (int i = 0; i < segments.size(); i++) {
            TranscriptSegment s = segments.get(i);
            if (positionMs >= s.getStartMs() && positionMs < s.getEndMs()) {
                return i;
            }
        }
        TranscriptSegment last = segments.get(segments.size() - 1);
        if (positionMs >= last.getEndMs()) {
            return segments.size() - 1;
        }
        // Before the first segment
        if (positionMs < segments.get(0).getStartMs()) {
            return 0;
        }
        return -1;
    }

    @Override
    protected void onCleared() {
        lectureLive.removeObserver(lectureObserver);
        segmentsLive.removeObserver(segmentsObserver);
        notesLive.removeObserver(notesObserver);
        super.onCleared();
    }
}
