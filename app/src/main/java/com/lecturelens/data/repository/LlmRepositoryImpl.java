package com.lecturelens.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.lecturelens.core.Result;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.remote.ApiKeyProvider;
import com.lecturelens.data.remote.GeminiService;
import com.lecturelens.data.remote.GeminiSync;
import com.lecturelens.data.remote.PipelineErrorStore;
import com.lecturelens.data.remote.RemoteCallHelper;
import com.lecturelens.data.remote.UsageLimiter;
import com.lecturelens.data.remote.dto.GeminiCandidate;
import com.lecturelens.data.remote.dto.GeminiGenerateRequest;
import com.lecturelens.data.remote.dto.GeminiGenerateResponse;
import com.lecturelens.data.remote.dto.GeminiPart;
import com.lecturelens.data.remote.dto.NotesJsonPayload;
import com.lecturelens.data.local.dao.CourseDao;
import com.lecturelens.data.local.dao.HandoutDao;
import com.lecturelens.data.local.entity.CourseEntity;
import com.lecturelens.data.local.entity.HandoutEntity;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.LibrarySyncRepository;
import com.lecturelens.domain.repository.LlmRepository;
import com.lecturelens.domain.util.TranscriptChunker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Track 4 — Gemini summarization + Room persistence.
 *
 * <p>Sends <b>one Gemini request at a time</b> (global lock). On HTTP 429 it stops
 * immediately — no model cascade that burns remaining quota.
 */
@Singleton
public class LlmRepositoryImpl implements LlmRepository {

    private static final String TAG = "LlmRepository";
    private static final Gson GSON = new Gson();

    /** Single primary model — matches AI Studio "flash latest" alias. */
    private static final String GEMINI_MODEL = ApiKeyProvider.GEMINI_MODEL_FLASH;

    private static final String QUOTA_MSG =
            "Gemini quota exceeded. Wait about a minute, then tap Retry notes. "
                    + "Or paste a fresh key in Settings (that key is used before local.properties).";

    /** Global mutex so parallel WorkManager jobs never stack Gemini calls. */
    private static final Object GEMINI_LOCK = GeminiSync.LOCK;

    private final GeminiService geminiService;
    private final ApiKeyProvider apiKeys;
    private final NotesDao notesDao;
    private final TranscriptDao transcriptDao;
    private final HandoutDao handoutDao;
    private final CourseDao courseDao;
    private final LectureRepository lectureRepository;
    private final NotesEntityMapper notesMapper;
    private final TranscriptChunker chunker;
    private final PipelineErrorStore errorStore;
    private final UsageLimiter usageLimiter;
    private final LibrarySyncRepository librarySync;

    @Inject
    public LlmRepositoryImpl(@NonNull GeminiService geminiService,
                             @NonNull ApiKeyProvider apiKeys,
                             @NonNull NotesDao notesDao,
                             @NonNull TranscriptDao transcriptDao,
                             @NonNull HandoutDao handoutDao,
                             @NonNull CourseDao courseDao,
                             @NonNull LectureRepository lectureRepository,
                             @NonNull NotesEntityMapper notesMapper,
                             @NonNull TranscriptChunker chunker,
                             @NonNull PipelineErrorStore errorStore,
                             @NonNull UsageLimiter usageLimiter,
                             @NonNull LibrarySyncRepository librarySync) {
        this.geminiService = geminiService;
        this.apiKeys = apiKeys;
        this.notesDao = notesDao;
        this.transcriptDao = transcriptDao;
        this.handoutDao = handoutDao;
        this.courseDao = courseDao;
        this.lectureRepository = lectureRepository;
        this.notesMapper = notesMapper;
        this.chunker = chunker;
        this.errorStore = errorStore;
        this.usageLimiter = usageLimiter;
        this.librarySync = librarySync;
    }

    @NonNull
    @Override
    public Result<Notes> summarize(long lectureId, @NonNull String transcriptText) {
        if (!apiKeys.hasGeminiKey()) {
            return fail(lectureId,
                    "Gemini API key is missing. Add it in Settings or local.properties.");
        }
        if (!usageLimiter.canCallGemini()) {
            return fail(lectureId,
                    "Daily Gemini quota reached. Try On-device mode in Settings, or wait until tomorrow.");
        }
        if (transcriptText.trim().isEmpty()) {
            return fail(lectureId, "Transcript is empty — cannot summarize.");
        }

        lectureRepository.updateStatus(lectureId, LectureStatus.SUMMARIZING);
        errorStore.clear(lectureId);

        try {
            String handoutContext = buildHandoutContext(lectureId);
            NotesJsonPayload payload;
            if (chunker.needsMapReduce(transcriptText)) {
                payload = mapReduceSummarize(transcriptText, handoutContext);
            } else {
                payload = callGeminiForNotes(
                        buildSinglePassPrompt(transcriptText, handoutContext));
            }

            if (payload == null || payload.summary == null || payload.summary.trim().isEmpty()) {
                return fail(lectureId, "Gemini returned empty notes.");
            }

            // Normalize markdown style in summary for UI rendering.
            String summary = normalizeMarkdown(payload.summary.trim());

            Notes notes = new Notes(
                    lectureId,
                    summary,
                    safeList(payload.keyTerms),
                    safeList(payload.actionItems));

            notesDao.insert(notesMapper.toEntity(notes));
            applyAiTitleAndCategory(lectureId, payload);
            usageLimiter.recordGeminiCall();
            lectureRepository.updateStatus(lectureId, LectureStatus.READY);
            errorStore.clear(lectureId);
            librarySync.pushLecture(lectureId);
            return Result.success(notes);
        } catch (QuotaExceededException e) {
            Log.e(TAG, "Gemini quota exceeded", e);
            return fail(lectureId, QUOTA_MSG);
        } catch (IOException e) {
            Log.e(TAG, "Gemini I/O error", e);
            String msg = e.getMessage() != null ? e.getMessage() : "";
            // Transient 5xx — leave status alone so WorkManager can retry once.
            if (msg.startsWith(RemoteCallHelper.CODE_RETRY)) {
                return Result.error(msg);
            }
            return fail(lectureId, friendlyIoMessage(msg));
        } catch (Exception e) {
            Log.e(TAG, "Summarization failed", e);
            return fail(lectureId, friendlyIoMessage(
                    e.getMessage() != null ? e.getMessage() : "Summarization failed."));
        }
    }

    @NonNull
    private Result<Notes> fail(long lectureId, @NonNull String message) {
        String clean = sanitizeUserMessage(message);
        Log.e(TAG, clean);
        errorStore.put(lectureId, clean);
        lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
        return Result.error(clean);
    }

    @NonNull
    @Override
    public LiveData<Notes> observeNotes(long lectureId) {
        return NotesEntityMapper.mapNotesLiveData(notesDao.observeNotes(lectureId));
    }

    @NonNull
    public Result<Notes> summarizeForLecture(long lectureId) {
        TranscriptEntity transcript = transcriptDao.getTranscriptSync(lectureId);
        if (transcript == null || transcript.fullText.isEmpty()) {
            return fail(lectureId, "Transcript not found for lecture " + lectureId);
        }
        return summarize(lectureId, transcript.fullText);
    }

    @NonNull
    private NotesJsonPayload mapReduceSummarize(@NonNull String transcriptText,
                                                @NonNull String handoutContext)
            throws IOException, QuotaExceededException {
        List<String> chunks = chunker.chunk(transcriptText);
        List<String> partials = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkPrompt = String.format(Locale.US,
                    "Summarize lecture chunk %d of %d as concise study notes. "
                            + "Use markdown: **bold** for key phrases, - for bullets. "
                            + "Return JSON with keys summary, keyTerms, actionItems only.\n\n%s",
                    i + 1, chunks.size(), chunks.get(i));
            NotesJsonPayload partial = callGeminiForNotes(chunkPrompt);
            if (partial != null && partial.summary != null) {
                partials.add(partial.summary);
            }
        }
        String combined = String.join("\n\n", partials);
        String reducePrompt = "Merge these partial lecture summaries into one cohesive set of "
                + "study notes. Use markdown formatting: **bold** for important terms, "
                + "- for bullet lists (not * for bullets). "
                + "Return JSON with keys: summary (markdown), keyTerms (array), "
                + "actionItems (array), title (short lecture title), category (course name). "
                + "Do not invent content beyond the input.\n\n"
                + combined
                + (handoutContext.isEmpty() ? "" : "\n\nHANDOUT TEXT:\n" + handoutContext);
        return callGeminiForNotes(reducePrompt);
    }

    @NonNull
    private static String buildSinglePassPrompt(@NonNull String transcriptText,
                                                @NonNull String handoutContext) {
        return "You are a study-notes assistant for LectureLens. From the lecture transcript "
                + "(and any handout text), produce structured study notes.\n"
                + "Formatting rules for summary markdown:\n"
                + "- Use **double asterisks** for bold key terms\n"
                + "- Use lines starting with '- ' for bullet points (never bare * bullets)\n"
                + "- Keep paragraphs clear and concise\n"
                + "Return JSON only with keys:\n"
                + "  summary (markdown string),\n"
                + "  keyTerms (string array),\n"
                + "  actionItems (string array),\n"
                + "  title (short descriptive lecture title, max 8 words),\n"
                + "  category (suggested course/category name, 1-4 words).\n"
                + "Do not invent content beyond the transcript/handouts.\n\n"
                + "TRANSCRIPT:\n" + transcriptText
                + (handoutContext.isEmpty() ? "" : "\n\nHANDOUT / QUIZ TEXT:\n" + handoutContext);
    }

    @NonNull
    private String buildHandoutContext(long lectureId) {
        List<HandoutEntity> handouts = handoutDao.getByLectureSync(lectureId);
        if (handouts == null || handouts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (HandoutEntity h : handouts) {
            if (h.extractedText != null && !h.extractedText.trim().isEmpty()) {
                sb.append(h.extractedText.trim()).append("\n\n");
            }
        }
        return sb.toString().trim();
    }

    private void applyAiTitleAndCategory(long lectureId, @NonNull NotesJsonPayload payload) {
        if (payload.title != null && !payload.title.trim().isEmpty()) {
            String title = payload.title.trim().replaceAll("[\"']", "");
            if (title.length() > 80) {
                title = title.substring(0, 80).trim();
            }
            lectureRepository.updateTitle(lectureId, title);
        }
        if (payload.category == null || payload.category.trim().isEmpty()) {
            return;
        }
        String categoryName = payload.category.trim().replaceAll("[\"']", "");
        if (categoryName.length() > 40) {
            categoryName = categoryName.substring(0, 40).trim();
        }
        CourseEntity existing = courseDao.findByNameSync(categoryName);
        long courseId;
        if (existing != null) {
            courseId = existing.id;
        } else {
            CourseEntity created = new CourseEntity();
            created.name = categoryName;
            created.color = 0xFF1F4D00;
            created.createdAt = System.currentTimeMillis();
            courseId = courseDao.insert(created);
        }
        lectureRepository.updateCourseId(lectureId, courseId);
    }

    @NonNull
    static String normalizeMarkdown(@NonNull String summary) {
        // Convert " * item" style lines to "- item" for consistent bullets.
        String[] lines = summary.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.matches("^\\*(?!\\*)\\s+.+")) {
                sb.append("- ").append(trimmed.substring(1).trim());
            } else {
                sb.append(line);
            }
            if (i < lines.length - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * One HTTP call under a global lock. Never cascades to other models on 429.
     */
    @NonNull
    private NotesJsonPayload callGeminiForNotes(@NonNull String prompt)
            throws IOException, QuotaExceededException {
        synchronized (GEMINI_LOCK) {
            Response<GeminiGenerateResponse> response = geminiService.generateContent(
                    GEMINI_MODEL,
                    apiKeys.getGeminiApiKey(),
                    new GeminiGenerateRequest(prompt)).execute();

            if (!response.isSuccessful()) {
                String raw = RemoteCallHelper.readErrorBody(response);
                Log.e(TAG, "Gemini HTTP " + response.code() + " model=" + GEMINI_MODEL
                        + ": " + truncateForLog(raw));
                if (response.code() == 429 || isQuotaMessage(raw)) {
                    throw new QuotaExceededException();
                }
                if (RemoteCallHelper.isRetryableHttpCode(response.code())) {
                    // 5xx only — do not retry 429 via WorkManager.
                    throw new IOException(RemoteCallHelper.CODE_RETRY + ": "
                            + "Gemini temporarily unavailable. Will retry.");
                }
                throw new IOException(friendlyHttpMessage(response.code(), raw));
            }

            GeminiGenerateResponse body = response.body();
            String text = extractResponseText(body);
            if (text.isEmpty()) {
                throw new IOException("Gemini returned an empty body.");
            }
            try {
                NotesJsonPayload parsed = GSON.fromJson(unwrapJson(text), NotesJsonPayload.class);
                if (parsed == null) {
                    throw new IOException("Could not parse Gemini JSON notes.");
                }
                // Brief pause so bursty free-tier quotas don't trip on the next call.
                try {
                    TimeUnit.MILLISECONDS.sleep(400);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return parsed;
            } catch (JsonSyntaxException e) {
                throw new IOException("Gemini returned invalid JSON notes.", e);
            }
        }
    }

    @NonNull
    static String unwrapJson(@NonNull String raw) {
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNl = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNl >= 0 && lastFence > firstNl) {
                text = text.substring(firstNl + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        return text;
    }

    @NonNull
    private static String extractResponseText(@Nullable GeminiGenerateResponse response) {
        if (response == null || response.candidates == null || response.candidates.isEmpty()) {
            return "";
        }
        GeminiCandidate candidate = response.candidates.get(0);
        if (candidate.content == null || candidate.content.parts == null
                || candidate.content.parts.isEmpty()) {
            return "";
        }
        GeminiPart part = candidate.content.parts.get(0);
        return part.text != null ? part.text.trim() : "";
    }

    @NonNull
    private static List<String> safeList(List<String> values) {
        return values != null ? values : Collections.emptyList();
    }

    private static boolean isQuotaMessage(@Nullable String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.US);
        return lower.contains("resource_exhausted")
                || lower.contains("quota")
                || lower.contains("rate limit");
    }

    @NonNull
    private static String friendlyHttpMessage(int code, @NonNull String raw) {
        if (code == 400) {
            return "Gemini rejected the request. Try a shorter lecture or re-transcribe.";
        }
        if (code == 401 || code == 403) {
            return "Gemini API key was rejected. Paste a valid key in Settings.";
        }
        if (code == 404) {
            return "Gemini model not available for this key. Check the key in Settings.";
        }
        return "Notes generation failed (HTTP " + code + "). Try again in a moment.";
    }

    @NonNull
    private static String friendlyIoMessage(@Nullable String message) {
        if (message == null || message.isEmpty()) {
            return "Notes generation failed. Try again.";
        }
        if (isQuotaMessage(message) || message.contains("429")) {
            return QUOTA_MSG;
        }
        if (message.startsWith(RemoteCallHelper.CODE_RETRY)) {
            return message;
        }
        return sanitizeUserMessage(message);
    }

    /** Never surface raw JSON / huge API bodies in the UI. */
    @NonNull
    static String sanitizeUserMessage(@NonNull String message) {
        String trimmed = message.trim();
        if (trimmed.length() > 220 || trimmed.contains("{") || trimmed.contains("\"error\"")) {
            if (isQuotaMessage(trimmed) || trimmed.contains("429")) {
                return QUOTA_MSG;
            }
            return "Notes generation failed. Tap Retry notes to try again.";
        }
        return trimmed;
    }

    @NonNull
    private static String truncateForLog(@NonNull String raw) {
        return raw.length() <= 400 ? raw : raw.substring(0, 400) + "…";
    }

    private static final class QuotaExceededException extends Exception {
        QuotaExceededException() {
            super(QUOTA_MSG);
        }
    }
}
