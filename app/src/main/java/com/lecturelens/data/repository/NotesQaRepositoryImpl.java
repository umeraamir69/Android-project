package com.lecturelens.data.repository;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.core.Result;
import com.lecturelens.core.VectorMath;
import com.lecturelens.data.local.dao.ChatDao;
import com.lecturelens.data.local.dao.HandoutDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.ChatMessageEntity;
import com.lecturelens.data.local.entity.HandoutEntity;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.remote.ApiKeyProvider;
import com.lecturelens.data.remote.GeminiService;
import com.lecturelens.data.remote.GeminiSync;
import com.lecturelens.data.remote.HandoutStorageUploader;
import com.lecturelens.data.remote.RemoteCallHelper;
import com.lecturelens.data.remote.dto.GeminiCandidate;
import com.lecturelens.data.remote.dto.GeminiGenerateRequest;
import com.lecturelens.data.remote.dto.GeminiGenerateResponse;
import com.lecturelens.data.remote.dto.GeminiGenerationConfig;
import com.lecturelens.data.remote.dto.GeminiPart;
import com.lecturelens.domain.model.ChatMessage;
import com.lecturelens.domain.model.Handout;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.model.QaAnswer;
import com.lecturelens.domain.model.RagCitation;
import com.lecturelens.domain.repository.ConsentGate;
import com.lecturelens.domain.repository.EmbeddingRepository;
import com.lecturelens.domain.repository.HandoutRepository;
import com.lecturelens.domain.repository.NotesQaRepository;

import org.json.JSONArray;

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
 * RAG-first notes Q&A + handout OCR via Gemini.
 */
@Singleton
public class NotesQaRepositoryImpl implements NotesQaRepository, HandoutRepository {

    private static final String TAG = "NotesQa";
    private static final String MODEL = ApiKeyProvider.GEMINI_MODEL_FLASH;
    private static final int MAX_CONTEXT_CHARS = 24000;
    private static final int TOP_K = 6;

    private final GeminiService geminiService;
    private final ApiKeyProvider apiKeys;
    private final NotesDao notesDao;
    private final TranscriptDao transcriptDao;
    private final HandoutDao handoutDao;
    private final ChatDao chatDao;
    private final NotesEntityMapper notesMapper;
    private final ConsentGate consentGate;
    private final EmbeddingRepository embeddingRepository;
    private final HandoutStorageUploader handoutStorageUploader;
    private final AppExecutors executors;

    @Inject
    public NotesQaRepositoryImpl(@NonNull GeminiService geminiService,
                                 @NonNull ApiKeyProvider apiKeys,
                                 @NonNull NotesDao notesDao,
                                 @NonNull TranscriptDao transcriptDao,
                                 @NonNull HandoutDao handoutDao,
                                 @NonNull ChatDao chatDao,
                                 @NonNull NotesEntityMapper notesMapper,
                                 @NonNull ConsentGate consentGate,
                                 @NonNull EmbeddingRepository embeddingRepository,
                                 @NonNull HandoutStorageUploader handoutStorageUploader,
                                 @NonNull AppExecutors executors) {
        this.geminiService = geminiService;
        this.apiKeys = apiKeys;
        this.notesDao = notesDao;
        this.transcriptDao = transcriptDao;
        this.handoutDao = handoutDao;
        this.chatDao = chatDao;
        this.notesMapper = notesMapper;
        this.consentGate = consentGate;
        this.embeddingRepository = embeddingRepository;
        this.handoutStorageUploader = handoutStorageUploader;
        this.executors = executors;
    }

    @Override
    public void ask(long lectureId, @NonNull String question, @NonNull Callback callback) {
        executors.diskIO().execute(() -> {
            try {
                String q = question.trim();
                if (q.isEmpty()) {
                    throw new IOException("Type a question about these notes.");
                }
                insertChat(lectureId, ChatMessage.ROLE_USER, q, "[]");
                QaAnswer answer = askSync(lectureId, q);
                insertChat(lectureId, ChatMessage.ROLE_ASSISTANT, answer.text,
                        citationsToJson(answer.citations));
                callback.onAnswer(answer);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Ask AI failed");
            }
        });
    }

    @NonNull
    @Override
    public LiveData<List<ChatMessage>> observeChat(long lectureId) {
        return Transformations.map(chatDao.observeByLecture(lectureId), entities -> {
            List<ChatMessage> out = new ArrayList<>();
            if (entities == null) {
                return out;
            }
            for (ChatMessageEntity e : entities) {
                out.add(new ChatMessage(e.id, e.lectureId, e.role, e.text, e.citationsJson, e.createdAt));
            }
            return out;
        });
    }

    @Override
    public void clearChat(long lectureId) {
        executors.diskIO().execute(() -> chatDao.deleteByLecture(lectureId));
    }

    private void insertChat(long lectureId,
                            @NonNull String role,
                            @NonNull String text,
                            @NonNull String citationsJson) {
        ChatMessageEntity row = new ChatMessageEntity();
        row.lectureId = lectureId;
        row.role = role;
        row.text = text;
        row.citationsJson = citationsJson;
        row.createdAt = System.currentTimeMillis();
        chatDao.insert(row);
    }

    @NonNull
    private static String citationsToJson(@Nullable List<RagCitation> citations) {
        JSONArray arr = new JSONArray();
        if (citations == null) {
            return arr.toString();
        }
        for (RagCitation c : citations) {
            arr.put(c.startMs);
        }
        return arr.toString();
    }

    @NonNull
    private QaAnswer askSync(long lectureId, @NonNull String question) throws IOException {
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

        Result<float[]> embedResult = embeddingRepository.embedQuery(q);
        if (embedResult instanceof Result.Success) {
            float[] vector = ((Result.Success<float[]>) embedResult).data;
            Result<List<RagCitation>> search =
                    embeddingRepository.searchSimilar(lectureId, vector, TOP_K);
            if (search instanceof Result.Success) {
                List<RagCitation> hits = ((Result.Success<List<RagCitation>>) search).data;
                if (!hits.isEmpty()) {
                    return askWithRag(q, hits);
                }
            }
        }

        // Fallback: full-context prompt when embeddings are missing.
        return QaAnswer.plain(askWithFullContext(lectureId, q));
    }

    @NonNull
    private QaAnswer askWithRag(@NonNull String question, @NonNull List<RagCitation> hits)
            throws IOException {
        StringBuilder ctx = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            RagCitation c = hits.get(i);
            ctx.append("[").append(i + 1).append("] ")
                    .append(VectorMath.formatTimestamp(c.startMs))
                    .append(" — ")
                    .append(c.snippet)
                    .append("\n\n");
        }
        String prompt = "You are LectureLens study assistant using retrieved lecture chunks.\n"
                + "Answer ONLY using the CHUNKS below. If insufficient, reply exactly: "
                + "\"I couldn't find that in these lecture notes.\"\n"
                + "Cite sources like [1], [2] matching chunk numbers.\n"
                + "Format with markdown.\n\nCHUNKS:\n" + ctx + "QUESTION:\n" + question;
        String answer = callPlainGemini(prompt);
        if (answer.contains("I couldn't find that in these lecture notes")) {
            return QaAnswer.plain(answer);
        }
        return new QaAnswer(answer, hits);
    }

    @NonNull
    private String askWithFullContext(long lectureId, @NonNull String q) throws IOException {
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
                out.add(toHandout(e));
            }
            return out;
        });
    }

    @Override
    public void addHandoutFile(long lectureId,
                               @NonNull File file,
                               @NonNull String mimeType,
                               @Nullable String displayName,
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
                String mime = mimeType == null || mimeType.isEmpty() ? guessMime(file) : mimeType;
                String name = displayName != null && !displayName.trim().isEmpty()
                        ? displayName.trim()
                        : file.getName();
                String extracted = extractHandoutText(file, mime);
                HandoutEntity entity = new HandoutEntity();
                entity.lectureId = lectureId;
                entity.imagePath = file.getAbsolutePath();
                entity.mimeType = mime;
                entity.displayName = name;
                entity.extractedText = extracted;
                entity.createdAt = System.currentTimeMillis();
                long id = handoutDao.insert(entity);
                Handout handout = toHandout(entity);
                // Room assigns id on insert — rebuild with generated id.
                handout = new Handout(id, lectureId, entity.imagePath, mime, name,
                        extracted, null, entity.createdAt);
                callback.onAdded(handout);
                uploadHandoutCloud(lectureId, id, file, mime);
            } catch (Exception e) {
                Log.e(TAG, "Handout OCR failed", e);
                callback.onError(e.getMessage() != null ? e.getMessage() : "Couldn't read handout");
            }
        });
    }

    @Override
    public void deleteHandout(long handoutId) {
        executors.diskIO().execute(() -> handoutDao.deleteById(handoutId));
    }

    private void uploadHandoutCloud(long lectureId,
                                    long handoutId,
                                    @NonNull File file,
                                    @NonNull String mime) {
        handoutStorageUploader.upload(lectureId, handoutId, file, mime,
                new HandoutStorageUploader.Callback() {
                    @Override
                    public void onUploaded(@NonNull String downloadUrl) {
                        executors.diskIO().execute(() ->
                                handoutDao.updateRemoteUrl(handoutId, downloadUrl));
                    }

                    @Override
                    public void onSkipped(@NonNull String reason) {
                        Log.i(TAG, "Handout cloud skip: " + reason);
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        Log.w(TAG, "Handout cloud upload failed: " + message);
                    }
                });
    }

    @NonNull
    private static Handout toHandout(@NonNull HandoutEntity e) {
        return new Handout(
                e.id,
                e.lectureId,
                e.imagePath,
                e.mimeType != null ? e.mimeType : "image/jpeg",
                e.displayName != null ? e.displayName : "",
                e.extractedText,
                e.remoteUrl,
                e.createdAt);
    }

    @NonNull
    private String extractHandoutText(@NonNull File file, @NonNull String mimeType)
            throws IOException {
        if (mimeType.startsWith("text/")) {
            return new String(readAll(file), java.nio.charset.StandardCharsets.UTF_8).trim();
        }
        return ocrDocument(file, mimeType);
    }

    @NonNull
    private String ocrDocument(@NonNull File imageFile, @NonNull String mimeType) throws IOException {
        byte[] bytes = readAll(imageFile);
        if (bytes.length > 18 * 1024 * 1024) {
            throw new IOException("File is too large to scan (max ~18 MB).");
        }
        String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
        String mime = mimeType == null || mimeType.isEmpty() ? "image/jpeg" : mimeType;
        String prompt = mime.contains("pdf")
                ? "Extract ALL readable text from this lecture PDF / handout. "
                + "Preserve structure with markdown: headings, lists, tables as text. "
                + "Return only the extracted text, no commentary."
                : "Extract ALL readable text from this lecture handout / quiz / slide / document. "
                + "Preserve structure with markdown: headings, **bold** where printed bold, "
                + "and - bullet lists. If handwriting is unclear, note [unclear]. "
                + "Return only the extracted text, no commentary.";
        List<GeminiPart> parts = new ArrayList<>();
        parts.add(new GeminiPart(prompt));
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
                throw new IOException("No text found in that file.");
            }
            sleepBrief();
            return text;
        }
    }

    @NonNull
    private static String guessMime(@NonNull File file) {
        String name = file.getName().toLowerCase(Locale.US);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (name.endsWith(".txt")) {
            return "text/plain";
        }
        if (name.endsWith(".doc")) {
            return "application/msword";
        }
        if (name.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "image/jpeg";
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
