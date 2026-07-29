package com.lecturelens.data.repository;

import androidx.annotation.NonNull;

import com.lecturelens.core.Result;
import com.lecturelens.core.VectorMath;
import com.lecturelens.data.local.dao.EmbeddingDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.EmbeddingEntity;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.local.entity.TranscriptSegmentEntity;
import com.lecturelens.data.remote.ApiKeyProvider;
import com.lecturelens.data.remote.GeminiService;
import com.lecturelens.data.remote.GeminiSync;
import com.lecturelens.data.remote.RemoteCallHelper;
import com.lecturelens.data.remote.UsageLimiter;
import com.lecturelens.data.remote.dto.GeminiEmbedRequest;
import com.lecturelens.data.remote.dto.GeminiEmbedResponse;
import com.lecturelens.domain.model.RagCitation;
import com.lecturelens.domain.repository.ConsentGate;
import com.lecturelens.domain.repository.EmbeddingRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

@Singleton
public class EmbeddingRepositoryImpl implements EmbeddingRepository {

    private static final String EMBED_MODEL = "text-embedding-004";
    private static final int EMBED_CHUNK_CHARS = 1500;
    private static final float MIN_SCORE = 0.25f;

    private final GeminiService geminiService;
    private final ApiKeyProvider apiKeys;
    private final EmbeddingDao embeddingDao;
    private final TranscriptDao transcriptDao;
    private final NotesDao notesDao;
    private final ConsentGate consentGate;
    private final UsageLimiter usageLimiter;

    @Inject
    public EmbeddingRepositoryImpl(@NonNull GeminiService geminiService,
                                   @NonNull ApiKeyProvider apiKeys,
                                   @NonNull EmbeddingDao embeddingDao,
                                   @NonNull TranscriptDao transcriptDao,
                                   @NonNull NotesDao notesDao,
                                   @NonNull ConsentGate consentGate,
                                   @NonNull UsageLimiter usageLimiter) {
        this.geminiService = geminiService;
        this.apiKeys = apiKeys;
        this.embeddingDao = embeddingDao;
        this.transcriptDao = transcriptDao;
        this.notesDao = notesDao;
        this.consentGate = consentGate;
        this.usageLimiter = usageLimiter;
    }

    @NonNull
    @Override
    public Result<Boolean> indexLecture(long lectureId) {
        if (!consentGate.hasCloudConsent()) {
            return Result.error("Cloud consent required to index embeddings");
        }
        if (!apiKeys.hasGeminiKey()) {
            return Result.error("Gemini API key missing");
        }
        if (!usageLimiter.canCallGemini()) {
            return Result.error("Daily Gemini quota reached — embeddings skipped");
        }
        try {
            List<Chunk> chunks = buildChunks(lectureId);
            if (chunks.isEmpty()) {
                return Result.error("Nothing to embed for this lecture");
            }
            List<EmbeddingEntity> rows = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Chunk c = chunks.get(i);
                float[] vector = embedSync(c.text);
                EmbeddingEntity row = new EmbeddingEntity();
                row.lectureId = lectureId;
                row.chunkIndex = i;
                row.startMs = c.startMs;
                row.endMs = c.endMs;
                row.text = c.text;
                row.vector = VectorMath.toBytes(vector);
                rows.add(row);
                sleepBrief();
            }
            embeddingDao.deleteByLecture(lectureId);
            embeddingDao.insertAll(rows);
            usageLimiter.recordGeminiCall();
            return Result.success(Boolean.TRUE);
        } catch (Exception e) {
            return Result.error(e.getMessage() != null ? e.getMessage() : "Embedding failed");
        }
    }

    @NonNull
    @Override
    public Result<float[]> embedQuery(@NonNull String text) {
        if (!apiKeys.hasGeminiKey()) {
            return Result.error("Gemini API key missing");
        }
        try {
            return Result.success(embedSync(text));
        } catch (Exception e) {
            return Result.error(e.getMessage() != null ? e.getMessage() : "Embed query failed");
        }
    }

    @NonNull
    @Override
    public Result<List<RagCitation>> searchSimilar(long lectureId,
                                                   @NonNull float[] queryVector,
                                                   int topK) {
        List<EmbeddingEntity> rows = embeddingDao.getByLectureSync(lectureId);
        if (rows.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        List<RagCitation> scored = new ArrayList<>();
        for (EmbeddingEntity row : rows) {
            float score = VectorMath.cosineSimilarity(queryVector, VectorMath.fromBytes(row.vector));
            if (score >= MIN_SCORE) {
                scored.add(new RagCitation(row.startMs, row.endMs, row.text, score));
            }
        }
        scored.sort(Comparator.comparingDouble((RagCitation c) -> c.score).reversed());
        if (scored.size() > topK) {
            scored = new ArrayList<>(scored.subList(0, topK));
        }
        return Result.success(scored);
    }

    @NonNull
    private float[] embedSync(@NonNull String text) throws IOException {
        synchronized (GeminiSync.LOCK) {
            Response<GeminiEmbedResponse> response = geminiService.embedContent(
                    EMBED_MODEL,
                    apiKeys.getGeminiApiKey(),
                    new GeminiEmbedRequest(text)).execute();
            if (!response.isSuccessful()) {
                throw new IOException(friendlyHttp(response.code(),
                        RemoteCallHelper.readErrorBody(response)));
            }
            GeminiEmbedResponse body = response.body();
            if (body == null || body.embedding == null || body.embedding.values == null
                    || body.embedding.values.isEmpty()) {
                throw new IOException("Empty embedding response");
            }
            List<Float> values = body.embedding.values;
            float[] out = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                Float v = values.get(i);
                out[i] = v != null ? v : 0f;
            }
            return out;
        }
    }

    @NonNull
    private List<Chunk> buildChunks(long lectureId) {
        List<Chunk> out = new ArrayList<>();
        List<TranscriptSegmentEntity> segments = transcriptDao.getSegmentsSync(lectureId);
        if (segments != null && !segments.isEmpty()) {
            StringBuilder buf = new StringBuilder();
            long start = segments.get(0).startMs;
            long end = segments.get(0).endMs;
            for (TranscriptSegmentEntity seg : segments) {
                String piece = seg.text != null ? seg.text.trim() : "";
                if (piece.isEmpty()) {
                    continue;
                }
                if (buf.length() + piece.length() + 1 > EMBED_CHUNK_CHARS && buf.length() > 0) {
                    out.add(new Chunk(buf.toString().trim(), start, end));
                    buf.setLength(0);
                    start = seg.startMs;
                }
                if (buf.length() == 0) {
                    start = seg.startMs;
                }
                if (buf.length() > 0) {
                    buf.append(' ');
                }
                buf.append(piece);
                end = seg.endMs;
            }
            if (buf.length() > 0) {
                out.add(new Chunk(buf.toString().trim(), start, end));
            }
        } else {
            TranscriptEntity transcript = transcriptDao.getTranscriptSync(lectureId);
            if (transcript != null && transcript.fullText != null && !transcript.fullText.isEmpty()) {
                // Reuse map-reduce chunker with a smaller local limit via manual split.
                String text = transcript.fullText;
                int start = 0;
                while (start < text.length()) {
                    int endIdx = Math.min(start + EMBED_CHUNK_CHARS, text.length());
                    String piece = text.substring(start, endIdx).trim();
                    if (!piece.isEmpty()) {
                        out.add(new Chunk(piece, 0, 0));
                    }
                    start = endIdx;
                }
            }
        }
        NotesEntity notes = notesDao.getNotesSync(lectureId);
        if (notes != null && notes.summary != null && !notes.summary.isEmpty()) {
            out.add(new Chunk("NOTES SUMMARY:\n" + notes.summary.trim(), 0, 0));
        }
        return out;
    }

    private static void sleepBrief() {
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @NonNull
    private static String friendlyHttp(int code, @NonNull String raw) {
        if (code == 429 || raw.toLowerCase(Locale.US).contains("quota")) {
            return "Gemini quota exceeded while embedding.";
        }
        return "Embedding request failed (HTTP " + code + ").";
    }

    private static final class Chunk {
        final String text;
        final long startMs;
        final long endMs;

        Chunk(String text, long startMs, long endMs) {
            this.text = text;
            this.startMs = startMs;
            this.endMs = endMs;
        }
    }
}
