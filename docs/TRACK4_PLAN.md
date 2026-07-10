# Track 4 — Transcription + Notes (Cloud)

**Owner:** Muhammad Umer Amir  
**Branch:** `track4-transcription-notes` (based on `track3-record-upload`)  
**Stack:** Java, Retrofit, WorkManager, Room (temp stubs until Track 1 lands)

---

## Starting point

This branch includes:

- Day 0 foundation (`main`) — frozen models, repository interfaces, `WorkerKeys`, Hilt skeleton
- Track 3 (`track3-record-upload`) — record/upload UI, `PipelineOrchestrator`, stub workers

Track 4 replaces the stub workers and adds the cloud processing layer.

---

## Goal (done when)

Given a lecture id + audio file on disk:

1. `TranscribeWorker` calls Google Speech-to-Text, persists `transcripts` + `transcript_segments`, sets status `TRANSCRIBED`
2. `SummarizeWorker` calls Gemini, persists `notes`, sets status `READY`
3. Verified end-to-end with a ≤5 min M4A recording

---

## Worker contract (frozen — do not change)

| Key | Type | Direction |
|-----|------|-----------|
| `KEY_LECTURE_ID` | long | input + output |
| `KEY_AUDIO_PATH` | String | input (transcribe only) |
| `KEY_LANGUAGE` | String | input (transcribe only, default `en-US`) |
| `KEY_ERROR_MSG` | String | output on failure |
| `PROGRESS_PERCENT` | int 0–100 | progress via `setProgressAsync` |

Chain: `PipelineOrchestrator` enqueues `TranscribeWorker` → `SummarizeWorker` (→ `EmbeddingsWorker` stretch, off by default).

---

## Implementation phases

### Phase 1 — Scaffold + temp persistence (PR 1)

**Goal:** Replace worker stubs with real structure; compile and run chain with fakes.

| File | Action |
|------|--------|
| `data/local/entity/TranscriptEntity.java` | New (temp — Track 1 absorbs) |
| `data/local/entity/TranscriptSegmentEntity.java` | New (temp) |
| `data/local/entity/NotesEntity.java` | New (temp) |
| `data/local/dao/TranscriptDao.java` | New (temp) |
| `data/local/dao/NotesDao.java` | New (temp) |
| `di/Track4TempDatabase.java` | New — in-memory Room, like Track 3's `UploadTempDatabase` |
| `di/Track4Module.java` | New — provides temp DAOs + API key holder |
| `data/repository/TranscriptionRepositoryImpl.java` | New — skeleton |
| `data/repository/LlmRepositoryImpl.java` | New — skeleton |
| `data/repository/EmbeddingRepositoryImpl.java` | New — no-op stub |
| `domain/usecase/TranscribeAudioUseCase.java` | New |
| `domain/usecase/GenerateNotesUseCase.java` | New |
| `domain/util/TranscriptChunker.java` | New |
| `processing/worker/TranscribeWorker.java` | Replace stub — inject use case, Hilt `@HiltWorker` |
| `processing/worker/SummarizeWorker.java` | Replace stub |
| `di/RepositoryModule.java` | Add `@Binds` for Track 4 repos (alphabetized) |

**Tests:** Unit tests for `TranscriptChunker`, use cases with mocked repos.

---

### Phase 2 — Network layer (PR 2)

**Goal:** Wire Retrofit + OkHttp; API key from env / debug build config.

| File | Action |
|------|--------|
| `di/NetworkModule.java` | Fill skeleton — OkHttp, Retrofit, interceptors |
| `data/remote/SpeechToTextService.java` | Retrofit interface (STT v2) |
| `data/remote/GeminiService.java` | Retrofit interface (Gemini REST) |
| `data/remote/dto/*` | Request/response DTOs |
| `data/remote/ApiKeyProvider.java` | Read key from `BuildConfig` or `SecureKeyStore` when Track 1 lands |

**Note:** Start with Gemini API (AI Studio) + Speech-to-Text REST. Use API key in header for MVP; OAuth/service account is a later upgrade.

**Gradle:** Add `buildConfigField` for debug API key (never commit real keys).

---

### Phase 3 — Speech-to-Text (PR 3)

**Goal:** Real transcription for short audio (≤1 min sync, then LRO for longer).

Flow in `TranscriptionRepositoryImpl.transcribe()`:

1. Update lecture status → `TRANSCRIBING` (via `LectureRepository.updateStatus`)
2. Read M4A file from `KEY_AUDIO_PATH`
3. Call STT — for MVP: `recognize` with inline audio (base64) for short files; `longRunningRecognize` + poll for >1 min
4. Map response → `Transcript` + `List<TranscriptSegment>` (start/end ms from word timings)
5. Persist to Room on `AppExecutors.diskIO()` (blocking — caller is Worker thread)
6. Update status → `TRANSCRIBED`
7. Return `Result.success(transcript)`

**Retry:** On HTTP 429/5xx, return `Result.retry()` from worker (WorkManager backoff).

---

### Phase 4 — Gemini summarization (PR 4)

**Goal:** Generate notes from transcript text.

Flow in `LlmRepositoryImpl.summarize()`:

1. Update status → `SUMMARIZING`
2. `GenerateNotesUseCase` checks transcript length:
   - Short (≤ ~3k tokens): single Gemini call
   - Long: `TranscriptChunker` → map (flash) → reduce (pro) per arch doc §3.2
3. Parse JSON response → `Notes` (summary markdown, keyTerms, actionItems)
4. Persist to Room
5. Update status → `READY` (skip `INDEXING` when embeddings disabled)
6. Return `Result.success(notes)`

**Prompt:** Study-notes assistant, temperature 0.2, structured JSON output.

---

### Phase 5 — Integration + polish (PR 5)

- Wire `TranscribeWorker` / `SummarizeWorker` to use cases via Hilt `@HiltWorker`
- Observe `WorkManager` progress in `UploadViewModel` (optional — cloud upload progress bar)
- Error path: set lecture status `FAILED`, forward `KEY_ERROR_MSG`
- Manual test: record 5 min → verify DB tables + status `READY`
- Remove temp DB when Track 1's `LectureLensDatabase` lands (joint PR with Zeeshan)

---

## File tree (final)

```
app/src/main/java/com/lecturelens/
├── data/
│   ├── remote/
│   │   ├── SpeechToTextService.java
│   │   ├── GeminiService.java
│   │   ├── ApiKeyProvider.java
│   │   └── dto/
│   │       ├── RecognizeRequest.java
│   │       ├── RecognizeResponse.java
│   │       ├── OperationResponse.java
│   │       └── GeminiGenerateRequest.java
│   └── repository/
│       ├── TranscriptionRepositoryImpl.java
│       ├── LlmRepositoryImpl.java
│       └── EmbeddingRepositoryImpl.java      (no-op)
├── domain/
│   ├── usecase/
│   │   ├── TranscribeAudioUseCase.java
│   │   └── GenerateNotesUseCase.java
│   └── util/
│       └── TranscriptChunker.java
├── processing/worker/
│   ├── TranscribeWorker.java               (replace stub)
│   └── SummarizeWorker.java                (replace stub)
└── di/
    ├── NetworkModule.java                    (fill skeleton)
    ├── Track4Module.java                     (temp, remove at integration)
    └── RepositoryModule.java                 (add @Binds)
```

---

## Dependencies & blockers

| Dependency | Owner | Status | Workaround |
|------------|-------|--------|------------|
| `WorkerKeys` | Track 1 | ✅ on branch | — |
| `PipelineOrchestrator` | Track 3 | ✅ on branch | — |
| `LectureDao` (writes) | Track 3 temp | ⚠️ in-memory | Extend temp DB for transcript/notes tables |
| `LectureLensDatabase` (full) | Track 1 | ❌ skeleton | Temp Room DB for Track 4 dev |
| `SecureKeyStore` | Track 1 | ❌ not built | `BuildConfig.API_KEY` for dev |
| Google API keys | Team | ❓ | Start paperwork; use AI Studio + GCP console |

---

## API setup checklist

1. **Google AI Studio** — Gemini API key (for summarization)
2. **Google Cloud Console** — enable Speech-to-Text API
3. Store keys in `local.properties` → `buildConfigField` (gitignored)
4. Never commit keys to git

---

## Suggested PR order

| PR | Title | Depends on |
|----|-------|------------|
| 1 | Track 4: scaffold repos, use cases, temp DB | — |
| 2 | Track 4: NetworkModule + Retrofit services | PR 1 |
| 3 | Track 4: TranscribeWorker + STT integration | PR 2 + API key |
| 4 | Track 4: SummarizeWorker + Gemini integration | PR 3 |
| 5 | Track 4: map-reduce + error handling + tests | PR 4 |

---

## Testing strategy

| Layer | Tool | What to test |
|-------|------|--------------|
| `TranscriptChunker` | JUnit | Splits at token budget, preserves order |
| Use cases | JUnit + Mockito | Status updates, repo calls, error paths |
| Repositories | JUnit + fake HTTP (MockWebServer) | STT/Gemini request/response parsing |
| Workers | WorkManager test harness | Input validation, retry on 429 |
| E2E | Manual on device | Record → process → verify DB |

---

## Coordination

- **Adeniyi (Track 3):** Worker I/O contract is frozen; ping if you need new `WorkerKeys`
- **Zeeshan (Track 1):** When Room DB lands, migrate off `Track4TempDatabase` + `UploadTempDatabase`
- **Aaron (Track 5):** Can develop lecture view against seed/fake data; needs real transcripts after PR 3
