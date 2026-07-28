package com.lecturelens.data.repository;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.local.dao.HandoutDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.HandoutEntity;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.remote.ApiKeyProvider;
import com.lecturelens.data.remote.GeminiService;
import com.lecturelens.data.remote.GeminiSync;
import com.lecturelens.data.remote.RemoteCallHelper;
import com.lecturelens.data.remote.dto.GeminiCandidate;
import com.lecturelens.data.remote.dto.GeminiGenerateRequest;
import com.lecturelens.data.remote.dto.GeminiGenerateResponse;
import com.lecturelens.data.remote.dto.GeminiGenerationConfig;
import com.lecturelens.data.remote.dto.GeminiPart;
import com.lecturelens.domain.model.Handout;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.repository.ConsentGate;
import com.lecturelens.domain.repository.HandoutRepository;
import com.lecturelens.domain.repository.NotesQaRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Grounded notes Q&A + handout OCR via Gemini (same API key / lock as summarization).
 */
@Singleton
public class NotesQaRepositoryImpl implements NotesQaRepository, HandoutRepository {

    private static final String TAG = "NotesQa";
    private static final String MODEL = ApiKeyProvider.GEMINI_MODEL_FLASH;
    private static final int MAX_CONTEXT_CHARS = 24000;

    private final GeminiService geminiService;
    private final ApiKeyProvider apiKeys;
    private final NotesDao notesDao;
    private final TranscriptDao transcriptDao;
    private final HandoutDao handoutDao;
    private final NotesEntityMapper notesMapper;
    private final ConsentGate consentGate;
    private final AppExecutors executors;

    @Inject
    public NotesQaRepositoryImpl(@NonNull GeminiService geminiService,
                                 @NonNull ApiKeyProvider apiKeys,
                                 @NonNull NotesDao notesDao,
                                 @NonNull TranscriptDao transcriptDao,
                                 @NonNull HandoutDao handoutDao,
                                 @NonNull NotesEntityMapper notesMapper,
                                 @NonNull ConsentGate consentGate,
                                 @NonNull AppExecutors executors) {
        this.geminiService = geminiService;
        this.apiKeys = apiKeys;
        this.notesDao = notesDao;
        this.transcriptDao = transcriptDao;
        this.handoutDao = handoutDao;
        this.notesMapper = notesMapper;
        this.consentGate = consentGate;
        this.executors = executors;
    }

    @Override
    public void ask(long lectureId, @NonNull String question, @NonNull Callback callback) {
        executors.diskIO().execute(() -> {
            try {
                callback.onAnswer(askSync(lectureId, question));
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Ask AI failed");
            }
        });
    }

    @NonNull
    private String askSync(long lectureId, @NonNull String question) throws IOException {
        if (!consentGate.hasCloudConsent()) {
            throw new IOException("Turn on cloud consent in Settings to ask AI.");
        }
        if (!apiKeys.hasGeminiKey()) {
            throw new IOException("Gemini API key is missing. Add it in Settings.");
        }
        String q = question.trim();
        if (q.isEmpty()) {
            throw new IOException("Type a question about these notes.");
        }

        NotesEntity notesEntity = notesDao.getNotesSync(lectureId);
        Notes notes = notesMapper.toDomain(notesEntity);
        TranscriptEntity transcript = transcriptDao.getTranscriptSync(lectureId);
        List<HandoutEntity> handouts = handoutDao.getByLectureSync(lectureId);

        StringBuilder context = new StringBuilder();
        if (notes != null) {
            context.append("SUMMARY:\n").append(notes.getSummary()).append("\n\n");
            if (!notes.getKeyTerms().isEmpty()) {
                context.append("KEY TERMS:\n");
                for (String t : notes.getKeyTerms()) {
                    context.append("- ").append(t).append('\n');
                }
                context.append('\n');
            }
            if (!notes.getActionItems().isEmpty()) {
                context.append("ACTION ITEMS:\n");
                for (String a : notes.getActionItems()) {
                    context.append("- ").append(a).append('\n');
                }
                context.append('\n');
            }
        }
        if (transcript != null && !transcript.fullText.trim().isEmpty()) {
            context.append("TRANSCRIPT:\n")
                    .append(trimContext(transcript.fullText.trim()))
                    .append("\n\n");
        }
        if (!handouts.isEmpty()) {
            context.append("HANDOUT / QUIZ TEXT (from photos):\n");
            for (HandoutEntity h : handouts) {
                if (h.extractedText != null && !h.extractedText.trim().isEmpty()) {
                    context.append(h.extractedText.trim()).append("\n\n");
                }
            }
        }
        if (context.length() == 0) {
            throw new IOException("No notes or transcript yet to ask about.");
        }

        String prompt = "You are LectureLens study assistant.\n"
                + "Answer ONLY using the lecture material below.\n"
                + "If the answer is not in the material, reply exactly: "
                + "\"I couldn't find that in these lecture notes.\"\n"
                + "Do not use outside knowledge. Do not invent facts.\n"
                + "Format with markdown: use **bold** for key terms, and - for bullet lists.\n\n"
                + "MATERIAL:\n" + context + "\nQUESTION:\n" + q;

        return callPlainGemini(prompt);
    }

    @NonNull
    @Override
    public LiveData<List<Handout>> observeHandouts(long lectureId) {
        return Transformations.map(handoutDao.observeByLecture(lectureId), list -> {
            List<Handout> out = new ArrayList<>();
            if (list == null) {
                return out;
            }
            for (HandoutEntity e : list) {
                out.add(new Handout(e.id, e.lectureId, e.imagePath, e.extractedText, e.createdAt));
            }
            return out;
        });
    }

    @Override
    public void addHandoutImage(long lectureId,
                                @NonNull File imageFile,
                                @NonNull String mimeType,
                                @NonNull HandoutCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                if (!consentGate.hasCloudConsent()) {
                    callback.onError("Turn on cloud consent in Settings to scan handouts.");
                    return;
                }
                if (!apiKeys.hasGeminiKey()) {
                    callback.onError("Gemini API key is missing. Add it in Settings.");
                    return;
                }
                String extracted = ocrImage(imageFile, mimeType);
                HandoutEntity entity = new HandoutEntity();
                entity.lectureId = lectureId;
                entity.imagePath = imageFile.getAbsolutePath();
                entity.extractedText = extracted;
                entity.createdAt = System.currentTimeMillis();
                long id = handoutDao.insert(entity);
                callback.onAdded(new Handout(id, lectureId, entity.imagePath, extracted, entity.createdAt));
            } catch (Exception e) {
                Log.e(TAG, "Handout OCR failed", e);
                callback.onError(e.getMessage() != null ? e.getMessage() : "Couldn't read handout");
            }
        });
    }

    @NonNull
    private String ocrImage(@NonNull File imageFile, @NonNull String mimeType) throws IOException {
        byte[] bytes = readAll(imageFile);
        String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
        String mime = mimeType == null || mimeType.isEmpty() ? "image/jpeg" : mimeType;
        List<GeminiPart> parts = new ArrayList<>();
        parts.add(new GeminiPart(
                "Extract ALL readable text from this lecture handout / quiz / slide photo. "
                        + "Preserve structure with markdown: headings, **bold** where printed bold, "
                        + "and - bullet lists. If handwriting is unclear, note [unclear]. "
                        + "Return only the extracted text, no commentary."));
        parts.add(GeminiPart.image(mime, b64));
        synchronized (GeminiSync.LOCK) {
            Response<GeminiGenerateResponse> response = geminiService.generateContent(
                    MODEL,
                    apiKeys.getGeminiApiKey(),
                    new GeminiGenerateRequest(parts, GeminiGenerationConfig.plainText())).execute();
            if (!response.isSuccessful()) {
                String raw = RemoteCallHelper.readErrorBody(response);
                throw new IOException(friendlyHttp(response.code(), raw));
            }
            String text = extractText(response.body());
            if (text.isEmpty()) {
                throw new IOException("No text found in that image.");
            }
            sleepBrief();
            return text;
        }
    }

    @NonNull
    private String callPlainGemini(@NonNull String prompt) throws IOException {
        synchronized (GeminiSync.LOCK) {
            Response<GeminiGenerateResponse> response = geminiService.generateContent(
                    MODEL,
                    apiKeys.getGeminiApiKey(),
                    GeminiGenerateRequest.plainText(prompt)).execute();
            if (!response.isSuccessful()) {
                String raw = RemoteCallHelper.readErrorBody(response);
                throw new IOException(friendlyHttp(response.code(), raw));
            }
            String text = extractText(response.body());
            if (text.isEmpty()) {
                throw new IOException("AI returned an empty answer.");
            }
            sleepBrief();
            return text;
        }
    }

    @NonNull
    private static String extractText(@Nullable GeminiGenerateResponse response) {
        if (response == null || response.candidates == null || response.candidates.isEmpty()) {
            return "";
        }
        GeminiCandidate c = response.candidates.get(0);
        if (c.content == null || c.content.parts == null || c.content.parts.isEmpty()) {
            return "";
        }
        GeminiPart part = c.content.parts.get(0);
        return part.text != null ? part.text.trim() : "";
    }

    @NonNull
    private static String trimContext(@NonNull String text) {
        if (text.length() <= MAX_CONTEXT_CHARS) {
            return text;
        }
        return text.substring(text.length() - MAX_CONTEXT_CHARS);
    }

    @NonNull
    private static byte[] readAll(@NonNull File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int read = in.read(buf);
            if (read < buf.length) {
                byte[] slim = new byte[Math.max(read, 0)];
                System.arraycopy(buf, 0, slim, 0, slim.length);
                return slim;
            }
            return buf;
        }
    }

    private static void sleepBrief() {
        try {
            TimeUnit.MILLISECONDS.sleep(350);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @NonNull
    private static String friendlyHttp(int code, @NonNull String raw) {
        if (code == 429 || raw.toLowerCase(Locale.US).contains("quota")) {
            return "Gemini quota exceeded. Wait a minute, then try again.";
        }
        if (code == 401 || code == 403) {
            return "Gemini API key was rejected. Check Settings.";
        }
        return "Cloud AI request failed (HTTP " + code + ").";
    }
}
