package com.lecturelens.domain.usecase;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.export.NotesPdfWriter;
import com.lecturelens.data.local.dao.CourseDao;
import com.lecturelens.data.local.dao.HandoutDao;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.CourseEntity;
import com.lecturelens.data.local.entity.HandoutEntity;
import com.lecturelens.data.local.entity.LectureEntity;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.prefs.UserSettingsStore;
import com.lecturelens.data.remote.HandoutStorageUploader;
import com.lecturelens.data.repository.NotesEntityMapper;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.model.SharedHandout;
import com.lecturelens.domain.model.SharedNotesPacket;
import com.lecturelens.domain.model.UserProfile;
import com.lecturelens.domain.repository.CloudShareRepository;
import com.lecturelens.domain.repository.CredentialsStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Builds lecture notes for share / export (Markdown, PDF, Word-friendly HTML,
 * plain text, WhatsApp) and publishes them to Firebase for in-app share codes.
 */
public class ExportLectureUseCase {

    public interface Callback {
        void onExported(@NonNull ExportResult result);

        void onError(@NonNull String message);
    }

    public interface CloudCallback {
        void onPublished(@NonNull String shareCode);

        void onError(@NonNull String message);
    }

    private final Context appContext;
    private final LectureDao lectureDao;
    private final TranscriptDao transcriptDao;
    private final NotesDao notesDao;
    private final CourseDao courseDao;
    private final HandoutDao handoutDao;
    private final NotesEntityMapper notesMapper;
    private final AppExecutors executors;
    private final CloudShareRepository cloudShareRepository;
    private final CredentialsStore credentialsStore;
    private final UserSettingsStore userSettings;
    private final HandoutStorageUploader handoutStorageUploader;

    @Inject
    public ExportLectureUseCase(@ApplicationContext @NonNull Context appContext,
                                @NonNull LectureDao lectureDao,
                                @NonNull TranscriptDao transcriptDao,
                                @NonNull NotesDao notesDao,
                                @NonNull CourseDao courseDao,
                                @NonNull HandoutDao handoutDao,
                                @NonNull NotesEntityMapper notesMapper,
                                @NonNull AppExecutors executors,
                                @NonNull CloudShareRepository cloudShareRepository,
                                @NonNull CredentialsStore credentialsStore,
                                @NonNull UserSettingsStore userSettings,
                                @NonNull HandoutStorageUploader handoutStorageUploader) {
        this.appContext = appContext;
        this.lectureDao = lectureDao;
        this.transcriptDao = transcriptDao;
        this.notesDao = notesDao;
        this.courseDao = courseDao;
        this.handoutDao = handoutDao;
        this.notesMapper = notesMapper;
        this.executors = executors;
        this.cloudShareRepository = cloudShareRepository;
        this.credentialsStore = credentialsStore;
        this.userSettings = userSettings;
        this.handoutStorageUploader = handoutStorageUploader;
    }

    @VisibleForTesting
    protected ExportLectureUseCase() {
        this.appContext = null;
        this.lectureDao = null;
        this.transcriptDao = null;
        this.notesDao = null;
        this.courseDao = null;
        this.handoutDao = null;
        this.notesMapper = null;
        this.executors = null;
        this.cloudShareRepository = null;
        this.credentialsStore = null;
        this.userSettings = null;
        this.handoutStorageUploader = null;
    }

    /** Default: Markdown file (backward compatible). */
    public void execute(long lectureId, @NonNull Callback callback) {
        execute(lectureId, ExportFormat.MARKDOWN, false, callback);
    }

    public void execute(long lectureId,
                        @NonNull ExportFormat format,
                        boolean preferWhatsApp,
                        @NonNull Callback callback) {
        executors.diskIO().execute(() -> {
            try {
                LectureEntity lecture = lectureDao.getByIdSync(lectureId);
                if (lecture == null) {
                    callback.onError("Lecture not found");
                    return;
                }
                TranscriptEntity transcript = transcriptDao.getTranscriptSync(lectureId);
                NotesEntity notesEntity = notesDao.getNotesSync(lectureId);
                Notes notes = notesMapper.toDomain(notesEntity);
                String transcriptText = transcript != null ? transcript.fullText : "";
                String professor = professorFor(lecture);
                UserProfile profile = userSettings.getProfile();
                String attributionMd = profile.attributionMarkdown(professor);
                String attributionPlain = profile.attributionPlain(professor);
                String markdown = buildMarkdown(lecture.title, transcriptText, notes, attributionMd);
                String plain = buildPlainText(lecture.title, transcriptText, notes, attributionPlain);

                File dir = new File(appContext.getFilesDir(), "exports");
                if (!dir.exists() && !dir.mkdirs()) {
                    callback.onError("Couldn't create export folder");
                    return;
                }

                ExportResult result;
                switch (format) {
                    case TEXT:
                        result = ExportResult.text(plain, lecture.title, preferWhatsApp);
                        break;
                    case PDF: {
                        File out = new File(dir, "lecture_" + lectureId + ".pdf");
                        NotesPdfWriter.write(out, lecture.title, plain);
                        result = ExportResult.file(out, "application/pdf", lecture.title);
                        break;
                    }
                    case DOC: {
                        File out = new File(dir, "lecture_" + lectureId + ".doc");
                        writeUtf8(out, buildWordHtml(
                                lecture.title, transcriptText, notes, attributionPlain));
                        // Word opens HTML saved with a .doc extension.
                        result = ExportResult.file(out, "application/msword", lecture.title);
                        break;
                    }
                    case MARKDOWN:
                    default: {
                        File out = new File(dir, "lecture_" + lectureId + ".md");
                        writeUtf8(out, markdown);
                        result = ExportResult.file(out, "text/markdown", lecture.title);
                        break;
                    }
                }
                callback.onExported(result);
            } catch (IOException e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Export failed");
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Export failed");
            }
        });
    }

    public void publishToCloud(long lectureId, @NonNull CloudCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                LectureEntity lecture = lectureDao.getByIdSync(lectureId);
                if (lecture == null) {
                    callback.onError("Lecture not found");
                    return;
                }
                TranscriptEntity transcript = transcriptDao.getTranscriptSync(lectureId);
                NotesEntity notesEntity = notesDao.getNotesSync(lectureId);
                Notes notes = notesMapper.toDomain(notesEntity);
                String transcriptText = transcript != null ? transcript.fullText : "";
                UserProfile profile = userSettings.getProfile();
                String professor = professorFor(lecture);
                String shareCode = cloudShareRepository.allocateShareCode();
                List<SharedHandout> sharedHandouts = buildSharedHandouts(lectureId, shareCode);
                SharedNotesPacket packet = new SharedNotesPacket(
                        shareCode,
                        lecture.title,
                        notes != null ? notes.getSummary() : "",
                        notes != null ? notes.getKeyTerms() : Collections.emptyList(),
                        notes != null ? notes.getActionItems() : Collections.emptyList(),
                        transcriptText,
                        credentialsStore.getEmail(),
                        profile.displayName().isEmpty() ? null : profile.displayName(),
                        profile.university.isEmpty() ? null : profile.university,
                        professor.isEmpty() ? null : professor,
                        sharedHandouts,
                        System.currentTimeMillis());
                cloudShareRepository.publish(packet, new CloudShareRepository.PublishCallback() {
                    @Override
                    public void onPublished(@NonNull String publishedCode) {
                        callback.onPublished(publishedCode);
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        callback.onError(message);
                    }
                });
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Cloud share failed");
            }
        });
    }

    @NonNull
    private List<SharedHandout> buildSharedHandouts(long lectureId, @NonNull String shareCode) {
        List<HandoutEntity> rows = handoutDao.getByLectureSync(lectureId);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<SharedHandout> out = new ArrayList<>();
        int index = 0;
        for (HandoutEntity row : rows) {
            index++;
            String name = row.displayName != null && !row.displayName.isEmpty()
                    ? row.displayName
                    : new File(row.imagePath).getName();
            String mime = row.mimeType != null ? row.mimeType : "application/octet-stream";
            String url = row.remoteUrl != null ? row.remoteUrl.trim() : "";
            File local = new File(row.imagePath);
            if (local.exists()) {
                try {
                    String objectName = index + "_" + sanitizeFileName(name);
                    String sharedUrl = handoutStorageUploader.uploadSharedBlocking(
                            shareCode, local, mime, objectName);
                    if (sharedUrl != null && !sharedUrl.isEmpty()) {
                        url = sharedUrl;
                    }
                } catch (Exception e) {
                    // Keep existing remoteUrl / text-only if share upload fails.
                }
            }
            out.add(new SharedHandout(name, mime, row.extractedText, url));
        }
        return out;
    }

    @NonNull
    private static String sanitizeFileName(@NonNull String name) {
        String cleaned = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleaned.isEmpty()) {
            return "handout.bin";
        }
        return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
    }

    private static void writeUtf8(@NonNull File out, @NonNull String content) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(out), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    @NonNull
    private String professorFor(@NonNull LectureEntity lecture) {
        if (lecture.courseId <= 0L) {
            return "";
        }
        CourseEntity course = courseDao.getByIdSync(lecture.courseId);
        return course != null && course.professor != null ? course.professor.trim() : "";
    }

    @NonNull
    @VisibleForTesting
    static String buildMarkdown(@NonNull String title,
                                @NonNull String transcriptText,
                                @Nullable Notes notes) {
        return buildMarkdown(title, transcriptText, notes, "");
    }

    @NonNull
    @VisibleForTesting
    static String buildMarkdown(@NonNull String title,
                                @NonNull String transcriptText,
                                @Nullable Notes notes,
                                @Nullable String attributionMarkdown) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        sb.append("*Lecture notes from LectureLens*\n\n");
        if (attributionMarkdown != null && !attributionMarkdown.trim().isEmpty()) {
            sb.append(attributionMarkdown.trim()).append("\n\n");
        }

        if (notes != null && !notes.getSummary().trim().isEmpty()) {
            sb.append("## Summary\n\n");
            sb.append(notes.getSummary().trim()).append("\n\n");
        } else {
            sb.append("## Summary\n\n");
            sb.append("_No summary available yet._\n\n");
        }

        if (notes != null && !notes.getKeyTerms().isEmpty()) {
            sb.append("## Key terms\n\n");
            appendBullets(sb, notes.getKeyTerms());
            sb.append('\n');
        } else {
            sb.append("## Key terms\n\n");
            sb.append("_None listed._\n\n");
        }

        if (notes != null && !notes.getActionItems().isEmpty()) {
            sb.append("## Action items\n\n");
            appendBullets(sb, notes.getActionItems());
            sb.append('\n');
        } else {
            sb.append("## Action items\n\n");
            sb.append("_None listed._\n\n");
        }

        sb.append("## Transcript\n\n");
        if (!transcriptText.trim().isEmpty()) {
            sb.append(transcriptText.trim()).append("\n\n");
        } else {
            sb.append("_No transcript available yet._\n\n");
        }

        sb.append("---\n\n");
        sb.append(exportFooterMarkdown());
        return sb.toString();
    }

    @NonNull
    static String buildPlainText(@NonNull String title,
                                 @NonNull String transcriptText,
                                 @Nullable Notes notes) {
        return buildPlainText(title, transcriptText, notes, "");
    }

    @NonNull
    static String buildPlainText(@NonNull String title,
                                 @NonNull String transcriptText,
                                 @Nullable Notes notes,
                                 @Nullable String attributionPlain) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append("Lecture notes from LectureLens\n\n");
        if (attributionPlain != null && !attributionPlain.trim().isEmpty()) {
            sb.append(attributionPlain.trim()).append("\n\n");
        }

        sb.append("SUMMARY\n");
        if (notes != null && !notes.getSummary().trim().isEmpty()) {
            sb.append(notes.getSummary().trim()).append("\n\n");
        } else {
            sb.append("No summary available yet.\n\n");
        }

        sb.append("KEY TERMS\n");
        if (notes != null && !notes.getKeyTerms().isEmpty()) {
            for (String term : notes.getKeyTerms()) {
                sb.append("• ").append(term).append('\n');
            }
            sb.append('\n');
        } else {
            sb.append("None listed.\n\n");
        }

        sb.append("ACTION ITEMS\n");
        if (notes != null && !notes.getActionItems().isEmpty()) {
            for (String item : notes.getActionItems()) {
                sb.append("• ").append(item).append('\n');
            }
            sb.append('\n');
        } else {
            sb.append("None listed.\n\n");
        }

        sb.append("TRANSCRIPT\n");
        if (!transcriptText.trim().isEmpty()) {
            sb.append(transcriptText.trim()).append("\n\n");
        } else {
            sb.append("No transcript available yet.\n\n");
        }

        sb.append("——————————————\n");
        sb.append(exportFooterPlain());
        return sb.toString().trim();
    }

    @NonNull
    static String buildWordHtml(@NonNull String title,
                                @NonNull String transcriptText,
                                @Nullable Notes notes) {
        return buildWordHtml(title, transcriptText, notes, "");
    }

    @NonNull
    static String buildWordHtml(@NonNull String title,
                                @NonNull String transcriptText,
                                @Nullable Notes notes,
                                @Nullable String attributionPlain) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset=\"utf-8\"><title>")
                .append(escapeHtml(title))
                .append("</title></head><body>");
        sb.append("<h1>").append(escapeHtml(title)).append("</h1>");
        sb.append("<p><em>Lecture notes from LectureLens</em></p>");
        if (attributionPlain != null && !attributionPlain.trim().isEmpty()) {
            sb.append("<p>")
                    .append(escapeHtml(attributionPlain.trim()).replace("\n", "<br/>"))
                    .append("</p>");
        }

        sb.append("<h2>Summary</h2>");
        if (notes != null && !notes.getSummary().trim().isEmpty()) {
            sb.append("<p>")
                    .append(escapeHtml(notes.getSummary().trim()).replace("\n", "<br/>"))
                    .append("</p>");
        } else {
            sb.append("<p><em>No summary available yet.</em></p>");
        }

        sb.append("<h2>Key terms</h2>");
        if (notes != null && !notes.getKeyTerms().isEmpty()) {
            sb.append("<ul>");
            for (String term : notes.getKeyTerms()) {
                sb.append("<li>").append(escapeHtml(term)).append("</li>");
            }
            sb.append("</ul>");
        } else {
            sb.append("<p><em>None listed.</em></p>");
        }

        sb.append("<h2>Action items</h2>");
        if (notes != null && !notes.getActionItems().isEmpty()) {
            sb.append("<ul>");
            for (String item : notes.getActionItems()) {
                sb.append("<li>").append(escapeHtml(item)).append("</li>");
            }
            sb.append("</ul>");
        } else {
            sb.append("<p><em>None listed.</em></p>");
        }

        sb.append("<h2>Transcript</h2>");
        if (!transcriptText.trim().isEmpty()) {
            sb.append("<p>")
                    .append(escapeHtml(transcriptText.trim()).replace("\n", "<br/>"))
                    .append("</p>");
        } else {
            sb.append("<p><em>No transcript available yet.</em></p>");
        }

        sb.append("<hr/>");
        sb.append("<p style=\"color:#56624B;font-size:12px;\">")
                .append(escapeHtml(exportFooterPlain()).replace("\n", "<br/>"))
                .append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    @NonNull
    static String exportFooterPlain() {
        return "Exported from LectureLens\n"
                + "Turn lecture audio into searchable notes\n"
                + "https://lecturelense.firebaseapp.com";
    }

    @NonNull
    static String exportFooterMarkdown() {
        return "_Exported from **LectureLens**_  \n"
                + "Turn lecture audio into searchable notes  \n"
                + "https://lecturelense.firebaseapp.com\n";
    }

    @NonNull
    private static String escapeHtml(@NonNull String raw) {
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static void appendBullets(@NonNull StringBuilder sb, @NonNull List<String> items) {
        for (String item : items) {
            if (item == null || item.trim().isEmpty()) {
                continue;
            }
            sb.append("- ").append(item.trim()).append('\n');
        }
    }
}
