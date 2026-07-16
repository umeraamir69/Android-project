package com.lecturelens.domain.usecase;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.LectureEntity;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.repository.NotesEntityMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Track 5 — writes a Markdown export of a lecture (title, summary, key terms,
 * action items, full transcript) into app-private {@code files/exports/} so
 * the UI can share it via {@code FileProvider}.
 */
public class ExportLectureUseCase {

    public interface Callback {
        void onExported(@NonNull File markdownFile);

        void onError(@NonNull String message);
    }

    private final Context appContext;
    private final LectureDao lectureDao;
    private final TranscriptDao transcriptDao;
    private final NotesDao notesDao;
    private final NotesEntityMapper notesMapper;
    private final AppExecutors executors;

    @Inject
    public ExportLectureUseCase(@ApplicationContext @NonNull Context appContext,
                                @NonNull LectureDao lectureDao,
                                @NonNull TranscriptDao transcriptDao,
                                @NonNull NotesDao notesDao,
                                @NonNull NotesEntityMapper notesMapper,
                                @NonNull AppExecutors executors) {
        this.appContext = appContext;
        this.lectureDao = lectureDao;
        this.transcriptDao = transcriptDao;
        this.notesDao = notesDao;
        this.notesMapper = notesMapper;
        this.executors = executors;
    }

    @VisibleForTesting
    protected ExportLectureUseCase() {
        this.appContext = null;
        this.lectureDao = null;
        this.transcriptDao = null;
        this.notesDao = null;
        this.notesMapper = null;
        this.executors = null;
    }

    public void execute(long lectureId, @NonNull Callback callback) {
        executors.diskIO().execute(() -> {
            try {
                LectureEntity lecture = lectureDao.getByIdSync(lectureId);
                if (lecture == null) {
                    callback.onError("Lecture not found");
                    return;
                }
                TranscriptEntity transcript = transcriptDao.getTranscriptSync(lectureId);
                NotesEntity notesEntity = notesDao.getNotesSync(lectureId);
                String markdown = buildMarkdown(
                        lecture.title,
                        transcript != null ? transcript.fullText : "",
                        notesMapper.toDomain(notesEntity));

                File dir = new File(appContext.getFilesDir(), "exports");
                if (!dir.exists() && !dir.mkdirs()) {
                    callback.onError("Couldn't create export folder");
                    return;
                }
                File out = new File(dir, "lecture_" + lectureId + ".md");
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        new FileOutputStream(out), StandardCharsets.UTF_8)) {
                    writer.write(markdown);
                }
                callback.onExported(out);
            } catch (IOException e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Export failed");
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Export failed");
            }
        });
    }

    @NonNull
    @VisibleForTesting
    static String buildMarkdown(@NonNull String title,
                                @NonNull String transcriptText,
                                @Nullable com.lecturelens.domain.model.Notes notes) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");

        if (notes != null && !notes.getSummary().trim().isEmpty()) {
            sb.append("## Summary\n\n");
            sb.append(notes.getSummary().trim()).append("\n\n");
        }

        if (notes != null && !notes.getKeyTerms().isEmpty()) {
            sb.append("## Key terms\n\n");
            appendBullets(sb, notes.getKeyTerms());
            sb.append('\n');
        }

        if (notes != null && !notes.getActionItems().isEmpty()) {
            sb.append("## Action items\n\n");
            appendBullets(sb, notes.getActionItems());
            sb.append('\n');
        }

        if (!transcriptText.trim().isEmpty()) {
            sb.append("## Transcript\n\n");
            sb.append(transcriptText.trim()).append('\n');
        }

        return sb.toString();
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
