package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.gson.Gson;
import com.lecturelens.core.Result;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.remote.ApiKeyProvider;
import com.lecturelens.data.remote.GeminiService;
import com.lecturelens.data.remote.RemoteCallHelper;
import com.lecturelens.data.remote.dto.GeminiCandidate;
import com.lecturelens.data.remote.dto.GeminiGenerateRequest;
import com.lecturelens.data.remote.dto.GeminiGenerateResponse;
import com.lecturelens.data.remote.dto.GeminiPart;
import com.lecturelens.data.remote.dto.NotesJsonPayload;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.LlmRepository;
import com.lecturelens.domain.util.TranscriptChunker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Track 4 — Gemini summarization + Room persistence.
 */
@Singleton
public class LlmRepositoryImpl implements LlmRepository {

    private static final Gson GSON = new Gson();

    private final GeminiService geminiService;
    private final ApiKeyProvider apiKeys;
    private final NotesDao notesDao;
    private final TranscriptDao transcriptDao;
    private final LectureRepository lectureRepository;
    private final NotesEntityMapper notesMapper;
    private final TranscriptChunker chunker;

    @Inject
    public LlmRepositoryImpl(@NonNull GeminiService geminiService,
                             @NonNull ApiKeyProvider apiKeys,
                             @NonNull NotesDao notesDao,
                             @NonNull TranscriptDao transcriptDao,
                             @NonNull LectureRepository lectureRepository,
                             @NonNull NotesEntityMapper notesMapper,
                             @NonNull TranscriptChunker chunker) {
        this.geminiService = geminiService;
        this.apiKeys = apiKeys;
        this.notesDao = notesDao;
        this.transcriptDao = transcriptDao;
        this.lectureRepository = lectureRepository;
        this.notesMapper = notesMapper;
        this.chunker = chunker;
    }

    @NonNull
    @Override
    public Result<Notes> summarize(long lectureId, @NonNull String transcriptText) {
        if (!apiKeys.hasGeminiKey()) {
            return Result.error("Gemini API key is missing. Add GEMINI_API_KEY to local.properties.");
        }
        if (transcriptText.trim().isEmpty()) {
            return Result.error("Transcript is empty — cannot summarize.");
        }

        lectureRepository.updateStatus(lectureId, LectureStatus.SUMMARIZING);

        try {
            NotesJsonPayload payload;
            if (chunker.needsMapReduce(transcriptText)) {
                payload = mapReduceSummarize(transcriptText);
            } else {
                payload = callGeminiForNotes(buildSinglePassPrompt(transcriptText));
            }

            if (payload == null || payload.summary == null || payload.summary.trim().isEmpty()) {
                lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
                return Result.error("Gemini returned empty notes.");
            }

            Notes notes = new Notes(
                    lectureId,
                    payload.summary.trim(),
                    safeList(payload.keyTerms),
                    safeList(payload.actionItems));

            notesDao.insert(notesMapper.toEntity(notes));
            lectureRepository.updateStatus(lectureId, LectureStatus.READY);
            return Result.success(notes);
        } catch (RetryableRemoteException e) {
            return Result.error(RemoteCallHelper.CODE_RETRY + ": " + e.getMessage());
        } catch (IOException e) {
            lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
            return Result.error("Network error during summarization.", e);
        } catch (Exception e) {
            lectureRepository.updateStatus(lectureId, LectureStatus.FAILED);
            return Result.error(e.getMessage() != null ? e.getMessage() : "Summarization failed.", e);
        }
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
            return Result.error("Transcript not found for lecture " + lectureId);
        }
        return summarize(lectureId, transcript.fullText);
    }

    @NonNull
    private NotesJsonPayload mapReduceSummarize(@NonNull String transcriptText)
            throws IOException, RetryableRemoteException {
        List<String> chunks = chunker.chunk(transcriptText);
        List<String> partials = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkPrompt = String.format(Locale.US,
                    "Summarize lecture chunk %d of %d as concise study notes. "
                            + "Return JSON with keys summary, keyTerms, actionItems only.\n\n%s",
                    i + 1, chunks.size(), chunks.get(i));
            NotesJsonPayload partial = callGeminiForNotes(chunkPrompt);
            if (partial != null && partial.summary != null) {
                partials.add(partial.summary);
            }
        }
        String combined = String.join("\n\n", partials);
        String reducePrompt = "Merge these partial lecture summaries into one cohesive set of "
                + "study notes. Return JSON with keys summary (markdown), keyTerms (array), "
                + "actionItems (array). Do not invent content beyond the input.\n\n" + combined;
        return callGeminiForNotes(reducePrompt);
    }

    @NonNull
    private static String buildSinglePassPrompt(@NonNull String transcriptText) {
        return "You are a study-notes assistant. From the lecture transcript below, produce "
                + "structured study notes. Return JSON only with keys: summary (markdown string), "
                + "keyTerms (string array), actionItems (string array). "
                + "Do not invent content beyond the transcript. Temperature discipline: be factual.\n\n"
                + transcriptText;
    }

    @NonNull
    private NotesJsonPayload callGeminiForNotes(@NonNull String prompt)
            throws IOException, RetryableRemoteException {
        Response<GeminiGenerateResponse> response = geminiService.generateContent(
                ApiKeyProvider.GEMINI_MODEL_FLASH,
                apiKeys.getGeminiApiKey(),
                new GeminiGenerateRequest(prompt)).execute();

        if (!response.isSuccessful()) {
            String message = RemoteCallHelper.readErrorBody(response);
            if (RemoteCallHelper.isRetryableHttpCode(response.code())) {
                throw new RetryableRemoteException(message);
            }
            throw new IOException(message);
        }

        GeminiGenerateResponse body = response.body();
        String text = extractResponseText(body);
        if (text.isEmpty()) {
            throw new IOException("Gemini returned an empty body.");
        }
        return GSON.fromJson(text, NotesJsonPayload.class);
    }

    @NonNull
    private static String extractResponseText(@NonNull GeminiGenerateResponse response) {
        if (response.candidates == null || response.candidates.isEmpty()) {
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

    private static final class RetryableRemoteException extends Exception {
        RetryableRemoteException(@NonNull String message) {
            super(message);
        }
    }
}
