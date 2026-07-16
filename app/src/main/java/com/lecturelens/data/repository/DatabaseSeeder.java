package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.lecturelens.data.local.dao.CourseDao;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.CourseEntity;
import com.lecturelens.data.local.entity.LectureEntity;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.local.entity.TranscriptSegmentEntity;
import com.lecturelens.domain.model.LectureStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Track 1 — seeds a demo course + lecture (with a short transcript, so search
 * and the lecture view have real rows) the first time a user signs in
 * (WORK_BREAKDOWN, Track 1 Auth). Replaces the in-memory DevSeed stub.
 *
 * <p>Synchronous — call on {@code AppExecutors.diskIO()}. Idempotent: no-op
 * unless the courses table is empty.
 */
@Singleton
public class DatabaseSeeder {

    private static final Gson GSON = new Gson();

    private final CourseDao courseDao;
    private final LectureDao lectureDao;
    private final TranscriptDao transcriptDao;
    private final NotesDao notesDao;

    @Inject
    public DatabaseSeeder(@NonNull CourseDao courseDao,
                          @NonNull LectureDao lectureDao,
                          @NonNull TranscriptDao transcriptDao,
                          @NonNull NotesDao notesDao) {
        this.courseDao = courseDao;
        this.lectureDao = lectureDao;
        this.transcriptDao = transcriptDao;
        this.notesDao = notesDao;
    }

    /**
     * Test seam — lets a JVM test subclass override {@link #seedIfEmpty()}
     * without DAOs. Subclasses using this must not call {@code super.seedIfEmpty()}.
     */
    @VisibleForTesting
    protected DatabaseSeeder() {
        this.courseDao = null;
        this.lectureDao = null;
        this.transcriptDao = null;
        this.notesDao = null;
    }

    /** @return true if seeding ran (DB was empty). */
    public boolean seedIfEmpty() {
        if (courseDao.count() > 0) {
            return false;
        }

        long now = System.currentTimeMillis();

        CourseEntity course = new CourseEntity();
        course.name = "CP-670 · Mobile App Development";
        course.color = 0xFF1F4D00;
        course.createdAt = now - TimeUnit.DAYS.toMillis(30);
        long courseId = courseDao.insert(course);

        // Lecture 1 — READY, with transcript + notes so Track 5 can demo fully.
        LectureEntity ready = new LectureEntity();
        ready.courseId = courseId;
        ready.title = "Week 6 — Activities & Lifecycle (demo)";
        ready.date = now - TimeUnit.DAYS.toMillis(1);
        ready.audioPath = null; // demo row: no audio on disk
        ready.durationMs = TimeUnit.MINUTES.toMillis(3);
        ready.status = LectureStatus.READY.name();
        long readyId = lectureDao.insert(ready);

        TranscriptEntity transcript = new TranscriptEntity();
        transcript.lectureId = readyId;
        transcript.fullText = "Welcome back. Today we cover the activity lifecycle: "
                + "onCreate, onStart, onResume, and what configuration changes do to your state. "
                + "Remember: never hold a view reference past onDestroyView in a fragment.";
        transcript.language = "en-US";
        transcript.modelUsed = "seed";
        transcriptDao.replaceTranscript(transcript, demoSegments(readyId));

        NotesEntity notes = new NotesEntity();
        notes.lectureId = readyId;
        notes.summary = "Activity lifecycle callbacks run in a fixed order. "
                + "Configuration changes recreate the activity, so persist UI state "
                + "and avoid leaking view references from fragments.";
        notes.keyTermsJson = GSON.toJson(Arrays.asList(
                "lifecycle", "onCreate", "onDestroyView", "configuration change"));
        notes.actionItemsJson = GSON.toJson(Arrays.asList(
                "Review the activity lifecycle diagram",
                "Never hold a View past onDestroyView"));
        notesDao.insert(notes);

        // Lecture 2 — RECORDED, exercises the status badge path.
        LectureEntity recorded = new LectureEntity();
        recorded.courseId = courseId;
        recorded.title = "Week 7 — Persistence with Room (demo)";
        recorded.date = now - TimeUnit.HOURS.toMillis(2);
        recorded.audioPath = null;
        recorded.durationMs = TimeUnit.MINUTES.toMillis(2);
        recorded.status = LectureStatus.RECORDED.name();
        lectureDao.insert(recorded);

        return true;
    }

    @NonNull
    private static List<TranscriptSegmentEntity> demoSegments(long lectureId) {
        String[] texts = {
                "Welcome back. Today we cover the activity lifecycle.",
                "onCreate runs once; onStart and onResume run every time you return.",
                "Configuration changes destroy and recreate the activity.",
                "Never hold a view reference past onDestroyView in a fragment."
        };
        List<TranscriptSegmentEntity> segments = new ArrayList<>(texts.length);
        long start = 0;
        for (String text : texts) {
            TranscriptSegmentEntity s = new TranscriptSegmentEntity();
            s.lectureId = lectureId;
            s.startMs = start;
            s.endMs = start + 45_000;
            s.text = text;
            segments.add(s);
            start += 45_000;
        }
        return segments;
    }
}
