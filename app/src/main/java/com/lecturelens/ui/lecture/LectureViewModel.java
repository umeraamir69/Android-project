package com.lecturelens.ui.lecture;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.core.BaseViewModel;
import com.lecturelens.data.prefs.UserSettingsStore;
import com.lecturelens.data.remote.PipelineErrorStore;
import com.lecturelens.domain.model.ChatMessage;
import com.lecturelens.domain.model.Course;
import com.lecturelens.domain.model.Handout;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.model.QaAnswer;
import com.lecturelens.domain.model.RagCitation;
import com.lecturelens.domain.model.TranscriptSegment;
import com.lecturelens.domain.repository.ConsentGate;
import com.lecturelens.domain.repository.CourseRepository;
import com.lecturelens.domain.repository.HandoutRepository;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.LlmRepository;
import com.lecturelens.domain.repository.NotesQaRepository;
import com.lecturelens.domain.repository.TranscriptionRepository;
import com.lecturelens.domain.usecase.ExportFormat;
import com.lecturelens.domain.usecase.ExportLectureUseCase;
import com.lecturelens.domain.usecase.ExportResult;
import com.lecturelens.processing.PipelineOrchestrator;

import java.io.File;
import java.util.ArrayList;
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
    private final PipelineErrorStore errorStore;
    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final PipelineOrchestrator orchestrator;
    private final ConsentGate consentGate;
    private final UserSettingsStore userSettings;
    private final AppExecutors executors;
    private final NotesQaRepository notesQaRepository;
    private final HandoutRepository handoutRepository;

    private final LiveData<Lecture> lectureLive;
    private final LiveData<List<TranscriptSegment>> segmentsLive;
    private final LiveData<Notes> notesLive;
    private final LiveData<List<Course>> coursesLive;
    private final LiveData<List<Handout>> handoutsLive;
    private final LiveData<List<ChatMessage>> chatLive;

    private final Observer<Lecture> lectureObserver;
    private final Observer<List<TranscriptSegment>> segmentsObserver;
    private final Observer<Notes> notesObserver;
    private final Observer<List<Course>> coursesObserver;

    @Nullable private Lecture latestLecture;
    @Nullable private List<TranscriptSegment> latestSegments;
    @Nullable private Notes latestNotes;
    @Nullable private List<Course> latestCourses;
    private boolean lectureResolved;

    private final MutableLiveData<Integer> activeSegmentIndex = new MutableLiveData<>(-1);
    private final MutableLiveData<ExportResult> exportResult = new MutableLiveData<>();
    private final MutableLiveData<String> cloudShareCode = new MutableLiveData<>();
    private final MutableLiveData<String> messageEvent = new MutableLiveData<>();
    private final MutableLiveData<String> aiAnswer = new MutableLiveData<>();
    private final MutableLiveData<List<RagCitation>> aiCitations = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> aiLoading = new MutableLiveData<>(false);

    private long pendingSeekMs = -1L;

    @Inject
    public LectureViewModel(@NonNull SavedStateHandle savedStateHandle,
                            @NonNull LectureRepository lectureRepository,
                            @NonNull CourseRepository courseRepository,
                            @NonNull TranscriptionRepository transcriptionRepository,
                            @NonNull LlmRepository llmRepository,
                            @NonNull ExportLectureUseCase exportLectureUseCase,
                            @NonNull PipelineErrorStore errorStore,
                            @NonNull PipelineOrchestrator orchestrator,
                            @NonNull ConsentGate consentGate,
                            @NonNull UserSettingsStore userSettings,
                            @NonNull AppExecutors executors,
                            @NonNull NotesQaRepository notesQaRepository,
                            @NonNull HandoutRepository handoutRepository) {
        Long id = savedStateHandle.get("lectureId");
        this.lectureId = id != null ? id : -1L;
        Long seek = savedStateHandle.get("seekMs");
        this.pendingSeekMs = seek != null ? seek : -1L;
        this.exportLectureUseCase = exportLectureUseCase;
        this.errorStore = errorStore;
        this.lectureRepository = lectureRepository;
        this.courseRepository = courseRepository;
        this.orchestrator = orchestrator;
        this.consentGate = consentGate;
        this.userSettings = userSettings;
        this.executors = executors;
        this.notesQaRepository = notesQaRepository;
        this.handoutRepository = handoutRepository;

        setLoading();

        lectureLive = lectureRepository.observeById(lectureId);
        segmentsLive = transcriptionRepository.observeSegments(lectureId);
        notesLive = llmRepository.observeNotes(lectureId);
        coursesLive = courseRepository.observeAll();
        handoutsLive = handoutRepository.observeHandouts(lectureId);
        chatLive = notesQaRepository.observeChat(lectureId);

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
        coursesObserver = value -> latestCourses = value;
        lectureLive.observeForever(lectureObserver);
        segmentsLive.observeForever(segmentsObserver);
        notesLive.observeForever(notesObserver);
        coursesLive.observeForever(coursesObserver);
    }

    @NonNull
    public LiveData<Integer> getActiveSegmentIndex() {
        return activeSegmentIndex;
    }

    @NonNull
    public LiveData<ExportResult> getExportResult() {
        return exportResult;
    }

    @NonNull
    public LiveData<String> getCloudShareCode() {
        return cloudShareCode;
    }

    @NonNull
    public LiveData<String> getMessageEvent() {
        return messageEvent;
    }

    @NonNull
    public LiveData<String> getAiAnswer() {
        return aiAnswer;
    }

    @NonNull
    public LiveData<List<RagCitation>> getAiCitations() {
        return aiCitations;
    }

    @NonNull
    public LiveData<Boolean> getAiLoading() {
        return aiLoading;
    }

    @NonNull
    public LiveData<List<Handout>> getHandouts() {
        return handoutsLive;
    }

    @NonNull
    public LiveData<List<ChatMessage>> getChatMessages() {
        return chatLive;
    }

    public void askAboutNotes(@Nullable String question) {
        aiLoading.setValue(true);
        notesQaRepository.ask(lectureId, question != null ? question : "",
                new NotesQaRepository.Callback() {
                    @Override
                    public void onAnswer(@NonNull QaAnswer answer) {
                        aiLoading.postValue(false);
                        aiAnswer.postValue(answer.text);
                        aiCitations.postValue(answer.citations);
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        aiLoading.postValue(false);
                        messageEvent.postValue(message);
                    }
                });
    }

    public void clearChat() {
        notesQaRepository.clearChat(lectureId);
        aiAnswer.setValue(null);
        aiCitations.setValue(Collections.emptyList());
    }

    public void onCitationTapped(long startMs) {
        onSegmentTapped(startMs);
    }

    public void addHandoutFile(@NonNull File file,
                               @NonNull String mimeType,
                               @Nullable String displayName) {
        handoutRepository.addHandoutFile(lectureId, file, mimeType, displayName,
                new HandoutRepository.HandoutCallback() {
                    @Override
                    public void onAdded(@NonNull Handout handout) {
                        String msg = handout.remoteUrl != null
                                ? "Handout saved and uploading to cloud…"
                                : "Handout scanned and saved";
                        messageEvent.postValue(msg);
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        messageEvent.postValue(message);
                    }
                });
    }

    public void addHandoutImage(@NonNull File imageFile, @NonNull String mimeType) {
        addHandoutFile(imageFile, mimeType, imageFile.getName());
    }

    public void deleteHandout(long handoutId) {
        handoutRepository.deleteHandout(handoutId);
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
        share(ExportFormat.MARKDOWN, false);
    }

    public void share(@NonNull ExportFormat format, boolean preferWhatsApp) {
        exportLectureUseCase.execute(lectureId, format, preferWhatsApp,
                new ExportLectureUseCase.Callback() {
                    @Override
                    public void onExported(@NonNull ExportResult result) {
                        exportResult.postValue(result);
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        messageEvent.postValue(message);
                    }
                });
    }

    public void shareToCloud() {
        exportLectureUseCase.publishToCloud(lectureId, new ExportLectureUseCase.CloudCallback() {
            @Override
            public void onPublished(@NonNull String shareCode) {
                cloudShareCode.postValue(shareCode);
            }

            @Override
            public void onError(@NonNull String message) {
                messageEvent.postValue(message);
            }
        });
    }

    /**
     * Re-runs processing for this lecture.
     * <ul>
     *   <li>If a transcript already exists → notes only (no STT replay).</li>
     *   <li>Otherwise → full Transcribe → Summarize chain.</li>
     * </ul>
     */
    public void retranscribe() {
        Lecture lecture = latestLecture;
        if (lecture == null) {
            messageEvent.postValue("Lecture not found");
            return;
        }
        String path = lecture.getAudioPath();
        if (path == null || path.trim().isEmpty() || !new File(path).isFile()) {
            messageEvent.postValue("No audio file to transcribe.");
            return;
        }
        if (!consentGate.hasCloudConsent()) {
            messageEvent.postValue("Turn on cloud consent in Settings to re-transcribe.");
            return;
        }
        final String audioPath = path;
        final boolean hasTranscript = latestSegments != null && !latestSegments.isEmpty();
        final String language = userSettings.getSttLanguage();
        executors.diskIO().execute(() -> {
            errorStore.clear(lectureId);
            if (hasTranscript) {
                lectureRepository.updateStatus(lectureId, LectureStatus.SUMMARIZING);
                orchestrator.enqueueSummarizeOnly(lectureId);
                messageEvent.postValue("Retrying notes with your current API key…");
            } else {
                lectureRepository.updateStatus(lectureId, LectureStatus.TRANSCRIBING);
                orchestrator.enqueue(lectureId, audioPath, language);
                messageEvent.postValue("Re-transcription started…");
            }
        });
    }

    /** Renames this lecture (shown in Library and the toolbar title). */
    public void renameTitle(@NonNull String rawTitle) {
        String title = rawTitle.trim();
        if (title.isEmpty()) {
            return;
        }
        executors.diskIO().execute(() -> lectureRepository.updateTitle(lectureId, title));
    }

    /** Real categories for the move picker (excludes Uncategorized). */
    @NonNull
    public List<Course> getCoursesSnapshot() {
        if (latestCourses == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(latestCourses);
    }

    /** Moves this lecture to a category, or Uncategorized when {@code courseId == -1}. */
    public void moveToCourse(long courseId, @NonNull String categoryLabel) {
        executors.diskIO().execute(() -> {
            lectureRepository.updateCourseId(lectureId, courseId);
            messageEvent.postValue("Moved to " + categoryLabel);
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
        setSuccess(new LectureDetail(
                latestLecture, latestSegments, notes, errorStore.get(lectureId)));
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
        coursesLive.removeObserver(coursesObserver);
        super.onCleared();
    }
}
