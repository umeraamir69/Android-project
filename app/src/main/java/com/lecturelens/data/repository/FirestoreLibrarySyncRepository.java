package com.lecturelens.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.lecturelens.core.AppExecutors;
import com.lecturelens.core.Logger;
import com.lecturelens.data.local.dao.CourseDao;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.CourseEntity;
import com.lecturelens.data.local.entity.LectureEntity;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.domain.model.AuthUser;
import com.lecturelens.domain.repository.AuthRepository;
import com.lecturelens.domain.repository.LibrarySyncRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FirestoreLibrarySyncRepository implements LibrarySyncRepository {

    private static final String TAG = "LibrarySync";

    private final FirebaseFirestore firestore;
    private final AuthRepository authRepository;
    private final CourseDao courseDao;
    private final LectureDao lectureDao;
    private final NotesDao notesDao;
    private final TranscriptDao transcriptDao;
    private final AppExecutors executors;
    private final Logger logger;

    @Inject
    public FirestoreLibrarySyncRepository(@NonNull FirebaseFirestore firestore,
                                          @NonNull AuthRepository authRepository,
                                          @NonNull CourseDao courseDao,
                                          @NonNull LectureDao lectureDao,
                                          @NonNull NotesDao notesDao,
                                          @NonNull TranscriptDao transcriptDao,
                                          @NonNull AppExecutors executors,
                                          @NonNull Logger logger) {
        this.firestore = firestore;
        this.authRepository = authRepository;
        this.courseDao = courseDao;
        this.lectureDao = lectureDao;
        this.notesDao = notesDao;
        this.transcriptDao = transcriptDao;
        this.executors = executors;
        this.logger = logger;
    }

    @Override
    public void pushAll(@NonNull Callback callback) {
        AuthUser user = authRepository.getCurrentUser();
        if (user == null) {
            callback.onDone();
            return;
        }
        String uid = user.uid;
        executors.diskIO().execute(() -> {
            try {
                List<CourseEntity> courses = courseDao.getAllSync();
                for (CourseEntity c : courses) {
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("name", c.name);
                    doc.put("color", c.color);
                    doc.put("createdAt", c.createdAt);
                    doc.put("professor", c.professor != null ? c.professor : "");
                    firestore.collection("users").document(uid)
                            .collection("courses").document(String.valueOf(c.id))
                            .set(doc, SetOptions.merge());
                }
                List<LectureEntity> lectures = lectureDao.getAllSync();
                for (LectureEntity lecture : lectures) {
                    pushLectureSync(uid, lecture);
                }
                callback.onDone();
            } catch (Exception e) {
                logger.e(TAG, "pushAll failed", e);
                callback.onError(e.getMessage() != null ? e.getMessage() : "Sync push failed");
            }
        });
    }

    @Override
    public void pullAll(@NonNull Callback callback) {
        AuthUser user = authRepository.getCurrentUser();
        if (user == null) {
            callback.onDone();
            return;
        }
        String uid = user.uid;
        firestore.collection("users").document(uid).collection("courses").get()
                .addOnSuccessListener(courseSnap -> executors.diskIO().execute(() -> {
                    try {
                        for (QueryDocumentSnapshot doc : courseSnap) {
                            String name = doc.getString("name");
                            if (name == null || name.isEmpty()) {
                                continue;
                            }
                            if (courseDao.findByNameSync(name) != null) {
                                continue;
                            }
                            CourseEntity entity = new CourseEntity();
                            entity.name = name;
                            Long color = doc.getLong("color");
                            entity.color = color != null ? color.intValue() : 0xFF1565C0;
                            Long created = doc.getLong("createdAt");
                            entity.createdAt = created != null ? created : System.currentTimeMillis();
                            String professor = doc.getString("professor");
                            entity.professor = professor != null ? professor : "";
                            courseDao.insert(entity);
                        }
                        firestore.collection("users").document(uid).collection("lectures").get()
                                .addOnSuccessListener(lecSnap -> executors.diskIO().execute(() -> {
                                    try {
                                        for (QueryDocumentSnapshot doc : lecSnap) {
                                            importLectureIfMissing(doc);
                                        }
                                        callback.onDone();
                                    } catch (Exception e) {
                                        callback.onError(msg(e));
                                    }
                                }))
                                .addOnFailureListener(e -> callback.onError(msg(e)));
                    } catch (Exception e) {
                        callback.onError(msg(e));
                    }
                }))
                .addOnFailureListener(e -> callback.onError(msg(e)));
    }

    @Override
    public void pushLecture(long lectureId) {
        AuthUser user = authRepository.getCurrentUser();
        if (user == null) {
            return;
        }
        String uid = user.uid;
        executors.diskIO().execute(() -> {
            LectureEntity lecture = lectureDao.getByIdSync(lectureId);
            if (lecture == null) {
                return;
            }
            try {
                pushLectureSync(uid, lecture);
            } catch (Exception e) {
                Log.w(TAG, "pushLecture failed", e);
            }
        });
    }

    private void pushLectureSync(@NonNull String uid, @NonNull LectureEntity lecture) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("courseId", lecture.courseId);
        doc.put("title", lecture.title);
        doc.put("date", lecture.date);
        doc.put("durationMs", lecture.durationMs);
        doc.put("status", lecture.status);
        NotesEntity notes = notesDao.getNotesSync(lecture.id);
        if (notes != null) {
            doc.put("summary", notes.summary);
            doc.put("keyTermsJson", notes.keyTermsJson);
            doc.put("actionItemsJson", notes.actionItemsJson);
        }
        TranscriptEntity transcript = transcriptDao.getTranscriptSync(lecture.id);
        if (transcript != null) {
            doc.put("transcriptText", transcript.fullText);
            doc.put("language", transcript.language);
        }
        firestore.collection("users").document(uid)
                .collection("lectures").document(String.valueOf(lecture.id))
                .set(doc, SetOptions.merge());
    }

    private void importLectureIfMissing(@NonNull QueryDocumentSnapshot doc) {
        String title = doc.getString("title");
        if (title == null || title.isEmpty()) {
            return;
        }
        Long date = doc.getLong("date");
        long dateVal = date != null ? date : 0L;
        for (LectureEntity existing : lectureDao.getAllSync()) {
            if (title.equals(existing.title) && existing.date == dateVal) {
                return;
            }
        }
        LectureEntity lecture = new LectureEntity();
        Long courseId = doc.getLong("courseId");
        lecture.courseId = courseId != null ? courseId : -1L;
        lecture.title = title;
        lecture.date = dateVal;
        Long duration = doc.getLong("durationMs");
        lecture.durationMs = duration != null ? duration : 0L;
        String status = doc.getString("status");
        lecture.status = status != null ? status : "SHARED";
        lecture.audioPath = null;
        long id = lectureDao.insert(lecture);

        String summary = doc.getString("summary");
        if (summary != null && !summary.isEmpty()) {
            NotesEntity notes = new NotesEntity();
            notes.lectureId = id;
            notes.summary = summary;
            String kt = doc.getString("keyTermsJson");
            notes.keyTermsJson = kt != null ? kt : "[]";
            String ai = doc.getString("actionItemsJson");
            notes.actionItemsJson = ai != null ? ai : "[]";
            notesDao.insert(notes);
        }
        String transcriptText = doc.getString("transcriptText");
        if (transcriptText != null && !transcriptText.isEmpty()) {
            TranscriptEntity t = new TranscriptEntity();
            t.lectureId = id;
            t.fullText = transcriptText;
            String lang = doc.getString("language");
            t.language = lang != null ? lang : "en-US";
            t.modelUsed = "cloud_sync";
            transcriptDao.insertTranscript(t);
        }
    }

    @NonNull
    private static String msg(@Nullable Exception e) {
        if (e != null && e.getMessage() != null) {
            return e.getMessage();
        }
        return "Sync failed";
    }
}
