package com.lecturelens.domain.usecase;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.local.dao.HandoutDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.HandoutEntity;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.local.entity.TranscriptSegmentEntity;
import com.lecturelens.data.repository.NotesEntityMapper;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.model.SharedHandout;
import com.lecturelens.domain.model.SharedNotesPacket;
import com.lecturelens.domain.repository.CloudShareRepository;
import com.lecturelens.domain.repository.LectureRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches a Firebase share code and saves it locally as a lecture (SHARED)
 * with notes + transcript + handout files so it appears in Home / Library.
 */
public class ImportSharedNotesUseCase {

    public interface Callback {
        void onImported(long lectureId, @NonNull String title);

        void onError(@NonNull String message);
    }

    private final Context appContext;
    private final CloudShareRepository cloudShareRepository;
    private final LectureRepository lectureRepository;
    private final NotesDao notesDao;
    private final TranscriptDao transcriptDao;
    private final HandoutDao handoutDao;
    private final NotesEntityMapper notesMapper;
    private final AppExecutors executors;
    private final OkHttpClient httpClient;

    @Inject
    public ImportSharedNotesUseCase(@ApplicationContext @NonNull Context appContext,
                                    @NonNull CloudShareRepository cloudShareRepository,
                                    @NonNull LectureRepository lectureRepository,
                                    @NonNull NotesDao notesDao,
                                    @NonNull TranscriptDao transcriptDao,
                                    @NonNull HandoutDao handoutDao,
                                    @NonNull NotesEntityMapper notesMapper,
                                    @NonNull AppExecutors executors,
                                    @NonNull OkHttpClient httpClient) {
        this.appContext = appContext;
        this.cloudShareRepository = cloudShareRepository;
        this.lectureRepository = lectureRepository;
        this.notesDao = notesDao;
        this.transcriptDao = transcriptDao;
        this.handoutDao = handoutDao;
        this.notesMapper = notesMapper;
        this.executors = executors;
        this.httpClient = httpClient;
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

    private long persist(@NonNull SharedNotesPacket packet) throws Exception {
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

        persistHandouts(lectureId, packet);
        return lectureId;
    }

    private void persistHandouts(long lectureId, @NonNull SharedNotesPacket packet) {
        if (packet.handouts.isEmpty()) {
            return;
        }
        File dir = new File(appContext.getFilesDir(), "handouts");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Couldn't create handouts folder");
        }
        int i = 0;
        for (SharedHandout shared : packet.handouts) {
            i++;
            String localPath = "";
            String remoteUrl = shared.downloadUrl;
            if (shared.hasFile()) {
                try {
                    String ext = extensionFor(shared.mimeType, shared.displayName);
                    File out = new File(dir, "shared_" + lectureId + "_" + i
                            + "_" + System.currentTimeMillis() + ext);
                    downloadToFile(shared.downloadUrl, out);
                    localPath = out.getAbsolutePath();
                } catch (Exception ignored) {
                    // Keep remote URL + OCR text even if download fails.
                }
            }
            HandoutEntity entity = new HandoutEntity();
            entity.lectureId = lectureId;
            entity.imagePath = localPath;
            entity.mimeType = shared.mimeType;
            entity.displayName = shared.displayName.isEmpty()
                    ? ("Handout " + i)
                    : shared.displayName;
            entity.extractedText = shared.extractedText;
            entity.remoteUrl = remoteUrl.isEmpty() ? null : remoteUrl;
            entity.createdAt = System.currentTimeMillis();
            handoutDao.insert(entity);
        }
    }

    private void downloadToFile(@NonNull String url, @NonNull File dest) throws Exception {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Couldn't download shared file ("
                        + response.code() + ")");
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Empty shared file");
            }
            try (InputStream in = body.byteStream();
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                }
            }
        }
    }

    @NonNull
    private static String extensionFor(@NonNull String mime, @NonNull String displayName) {
        String lower = displayName.toLowerCase(Locale.US);
        if (lower.contains(".")) {
            return lower.substring(lower.lastIndexOf('.'));
        }
        if (mime.contains("png")) {
            return ".png";
        }
        if (mime.contains("webp")) {
            return ".webp";
        }
        if (mime.contains("pdf")) {
            return ".pdf";
        }
        if (mime.contains("wordprocessingml") || mime.contains("docx")) {
            return ".docx";
        }
        if (mime.contains("msword")) {
            return ".doc";
        }
        if (mime.startsWith("text/")) {
            return ".txt";
        }
        if (mime.startsWith("image/")) {
            return ".jpg";
        }
        return ".bin";
    }
}
