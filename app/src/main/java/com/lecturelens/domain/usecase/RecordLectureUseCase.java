package com.lecturelens.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.repository.ConsentGate;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.processing.PipelineOrchestrator;

import androidx.annotation.VisibleForTesting;

import javax.inject.Inject;

/**
 * Track 3 — turns a finished recording (or imported file) into a persisted
 * lecture and kicks off cloud processing.
 *
 * <p>Steps, all on {@link AppExecutors#diskIO()} (writes are synchronous —
 * threading discipline, WORK_BREAKDOWN risks):
 * <ol>
 *   <li>Insert a {@link LectureStatus#RECORDED} row with the audio path; keep the
 *       row id.</li>
 *   <li>If {@link ConsentGate#hasCloudConsent()} — enqueue the transcribe →
 *       summarize (→ embeddings) chain via {@link PipelineOrchestrator}. Without
 *       consent the row is still saved (viewable, re-processable later) but audio
 *       never leaves the device (arch §1.1).</li>
 *   <li>Report the row id (or an error) through {@link Callback}.</li>
 * </ol>
 *
 * <p>The callback fires on the disk-IO thread; UI callers hop to the main thread
 * via {@code LiveData.postValue}.
 */
public class RecordLectureUseCase {

    /** Result sink. Invoked off the main thread. */
    public interface Callback {
        void onSaved(long lectureId);

        void onError(@NonNull String message);
    }

    /** Immutable input for a save. */
    public static final class Request {
        public final long courseId;
        @NonNull public final String title;
        @NonNull public final String audioPath;
        public final long durationMs;
        public final long dateMillis;
        @Nullable public final String language;

        public Request(long courseId,
                       @NonNull String title,
                       @NonNull String audioPath,
                       long durationMs,
                       long dateMillis,
                       @Nullable String language) {
            this.courseId = courseId;
            this.title = title;
            this.audioPath = audioPath;
            this.durationMs = durationMs;
            this.dateMillis = dateMillis;
            this.language = language;
        }
    }

    private final LectureRepository lectureRepository;
    private final PipelineOrchestrator orchestrator;
    private final ConsentGate consentGate;
    private final AppExecutors executors;

    @Inject
    public RecordLectureUseCase(@NonNull LectureRepository lectureRepository,
                                @NonNull PipelineOrchestrator orchestrator,
                                @NonNull ConsentGate consentGate,
                                @NonNull AppExecutors executors) {
        this.lectureRepository = lectureRepository;
        this.orchestrator = orchestrator;
        this.consentGate = consentGate;
        this.executors = executors;
    }

    /**
     * Test seam: lets a unit test subclass override {@link #execute} with a
     * synchronous fake without standing up Room/WorkManager. Subclasses using this
     * constructor must not call {@code super.execute(...)}.
     */
    @VisibleForTesting
    protected RecordLectureUseCase() {
        this.lectureRepository = null;
        this.orchestrator = null;
        this.consentGate = null;
        this.executors = null;
    }

    public void execute(@NonNull Request request, @NonNull Callback callback) {
        executors.diskIO().execute(() -> {
            try {
                Lecture lecture = new Lecture(
                        0L,                       // Room auto-generates the id
                        request.courseId,
                        request.title,
                        request.dateMillis,
                        request.audioPath,
                        request.durationMs,
                        LectureStatus.RECORDED);

                long lectureId = lectureRepository.insert(lecture);

                if (consentGate.hasCloudConsent()) {
                    orchestrator.enqueue(lectureId, request.audioPath, request.language);
                }

                callback.onSaved(lectureId);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null
                        ? e.getMessage()
                        : "Couldn't save the recording");
            }
        });
    }
}
