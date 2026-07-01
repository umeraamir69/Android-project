# LectureLens — Work Breakdown

**Team size:** 5 · **Target:** everyone works in parallel from Day 0 kickoff
**Stack:** Java + Android Views (Fragments, ViewBinding). No Kotlin, no Compose.
**Related:** `LectureLens_Architecture.md`

---

## Team

| #   | Name                    | Track                             |
| --- | ----------------------- | --------------------------------- |
| 1   | **Zeeshan Mahmood**     | **Foundation, Data Layer & Auth** |
| 2   | Daniel Monday-Ogidi     | Library UI                        |
| 3   | Adeniyi Ridwan Adetunji | Record + Upload Pipeline          |
| 4   | Muhammad Umer Amir      | Transcription + Notes (Cloud)     |
| 5   | Aaron Gullraiz Cecil    | Lecture View + Search             |

Tracks 2–5 are swappable — pick based on interest / prior Android experience.

---

## Approach — why this splits cleanly

The architecture is already layered (Presentation → Domain → Data → Processing). We split by **feature vertical**, with the foundation owner also delivering Auth as an early user-facing surface:

- **Track 1** (Zeeshan) owns everything shared — DI, DB, Navigation Component, base classes, ViewBinding setup — plus Auth. Auth is a natural fit because it's the first screen users see and it touches every foundation piece (theme, nav host, secure storage, Hilt).
- **Tracks 2–5** each own one screen (or two related screens) end-to-end: Fragment + ViewModel + use case + repository method.
- Everyone codes against **interfaces agreed on Day 0**, so nobody waits on anyone else's implementation. Missing implementations get an in-memory stub until the real one lands.

Because each track owns different packages / different files, merge conflicts are minimal. Because everyone consumes only interfaces from other tracks, integration is a matter of swapping stubs for real bindings.

### Technology choices (Java-friendly)

| Concern         | Choice                                     | Why                                                                                            |
| --------------- | ------------------------------------------ | ---------------------------------------------------------------------------------------------- |
| UI              | Fragments + XML layouts + **ViewBinding**  | Matches the existing repo; ViewBinding removes `findViewById` boilerplate.                     |
| Navigation      | **Navigation Component** (`nav_graph.xml`) | XML-based, works cleanly with Java + Fragments.                                                |
| DI              | **Hilt**                                   | Fully supports Java annotations (`@AndroidEntryPoint`, `@Inject`, `@HiltViewModel`).           |
| Async           | **Executors + LiveData** (+ WorkManager)   | No Kotlin coroutines. Room DAOs return `LiveData<T>`; writes run on `Executors.newSingleThreadExecutor()`. |
| State to UI     | `LiveData<UiState>` in each ViewModel      | Fragments observe with `viewLifecycleOwner`.                                                   |
| `Result<T>`     | Abstract class with `Success/Error/Loading` subclasses | Java 11 has no sealed classes; the abstract-class pattern is the standard workaround. |

---

## Day 0 (sync, ~2 hours)

Before anyone writes real code, the whole team agrees on the following in one call. Once these are in `main`, everyone can go parallel.

1. **Add core dependencies + enable ViewBinding.** Track 1 updates `libs.versions.toml` and `app/build.gradle.kts` with Hilt, Room, WorkManager, Retrofit + OkHttp + Moshi (or Gson), Navigation Component, Fragment KTX (Java-compatible), Lifecycle, ExoPlayer. Enables `buildFeatures.viewBinding = true`. **No Kotlin migration** — the module stays Java.
2. **Freeze the domain models** — `Course`, `Lecture`, `Transcript`, `TranscriptSegment`, `Notes`, `LectureStatus` enum. See §3.3 of the arch doc.
3. **Freeze the repository interfaces** — `CourseRepository`, `LectureRepository`, `TranscriptionRepository`, `LlmRepository`, `EmbeddingRepository`. Method signatures only.
4. **Freeze the navigation graph** — `nav_graph.xml` with fragments for `login`, `library`, `upload`, `lecture` (arg: `lectureId`), `search`, `settings`.
5. **Freeze `Result<T>`** — Java abstract class with `Result.Success<T>`, `Result.Error<T>`, `Result.Loading<T>` static nested types.
6. **Freeze WorkManager `Data` keys** — one shared `WorkerKeys.java` class (owned by Track 1) with `KEY_LECTURE_ID`, `KEY_AUDIO_PATH`, etc. Tracks 3 and 4 both consume it.

**Day 0 deliverable:** one PR from Track 1 with empty interfaces + models + `nav_graph.xml` + `Result.java` + `WorkerKeys.java`. Everyone branches off that.

---

## Day 0.5 — Package rename (Ridwan)

**Owner:** Adeniyi Ridwan Adetunji · **Duration:** half a day · **Blocks:** every other track's Day 1 branch

Rename the base package from `com.example.andoirdproject` (with typo) to **`com.lecturelens`** before anyone branches off Day 0. If this slips past Day 0, every track has to resolve rename conflicts on top of their WIP.

**Scope**

- Rename the Java package `com.example.andoirdproject` → `com.lecturelens` across all sources (Android Studio → right-click package → Refactor → Rename → check "Rename package").
- Move the folder structure `app/src/main/java/com/example/andoirdproject/` → `app/src/main/java/com/lecturelens/` (Refactor handles this).
- Update `namespace` and `applicationId` in `app/build.gradle.kts`.
- Update `AndroidManifest.xml` — the `.MainActivity` / `.ThemeShowcaseActivity` shortcuts resolve against the new namespace automatically, but confirm.
- Update `androidTest/` + `test/` package folders too (`ExampleInstrumentedTest.java`, `ExampleUnitTest.java`).
- Grep for stray string references: `git grep andoirdproject` should return zero results after the refactor.
- Optionally rename `settings.gradle.kts` project name from `"Andoird Project"` → `"LectureLens"`.

**Done when**

- `./gradlew assembleDebug` succeeds.
- `git grep andoirdproject` returns nothing.
- App installs and the Theme Showcase still opens from `MainActivity`.
- PR is small — pure rename, no logic changes, no formatting drift — so review is a rubber stamp and merges cleanly.

**Coordination:** land this **before** Track 1 opens branches for other tracks to base off. If Day 0 finishes Monday, this lands Tuesday morning.

After this merges, every file path in the sections below lives under `com.lecturelens/…` instead of `com.example.andoirdproject/…`.

---

## Track 1 — Foundation, Data Layer & Auth (Zeeshan)

**Note:** This track is deliberately heavier than the others because most of it is **front-loaded infra** — it lands in week 1 to unblock the team, then tapers into the smaller Auth deliverable in week 2–3.

**Package:** `di/`, `data/local/`, `data/repository/` (auth), `navigation/`, `core/`, `ui/auth/`

**Scope**

_Foundation (week 1)_

- Add dependencies + enable ViewBinding in `app/build.gradle.kts`. Update `libs.versions.toml`. **Keep the module Java** — no Kotlin migration.
- **Hilt setup** — `@HiltAndroidApp` on `LectureLensApp`, `@AndroidEntryPoint` on the single `MainActivity` and every fragment, `@HiltViewModel` on ViewModels.
- **Room DB (Java)** — entities (Course, Lecture, Transcript, TranscriptSegment, Notes) as Java classes with `@Entity`, DAOs (Course, Lecture, Transcript, Search) returning `LiveData<T>` for reads and `void`/`long` for writes, `LectureLensDatabase` abstract class, migrations skeleton, **FTS4 virtual table** (`@Fts4`) for transcript search.
- **Navigation Component** — single `MainActivity` hosting a `NavHostFragment`; `nav_graph.xml` with all six destinations as empty placeholder fragments. Other tracks replace their placeholder with the real fragment.
- **`Result<T>`** abstract class + `BaseViewModel` exposing a common `LiveData<UiState>`.
- Shared executors — `AppExecutors` singleton providing `diskIO()`, `networkIO()`, `mainThread()` for use everywhere. Provided via Hilt.
- Shared `WorkerKeys.java` for WorkManager `Data` keys.
- Theme already exists as XML (`Theme.AndoirdProject` + `colors.xml`) — no work needed.

_Auth (weeks 2–3)_

- **LoginFragment** + **LoginViewModel** — email + Google API-key entry, consent to cloud processing (per arch doc §1.1). Uses ViewBinding.
- **Settings fragment (stub)** — API-key edit + consent revocation.
- `SecureKeyStore` — EncryptedSharedPreferences (`androidx.security.crypto`) for API key.
- On sign-in success, seed a demo course + lecture into Room via `AppExecutors.diskIO()` so Tracks 2/5 have something to render during dev.

**Files (illustrative)**

```
app/src/main/java/com/lecturelens/
├── LectureLensApp.java                   (@HiltAndroidApp)
├── MainActivity.java                     (single-activity, @AndroidEntryPoint, NavHostFragment)
├── core/
│   ├── Result.java                       (abstract; Success/Error/Loading nested)
│   ├── UiState.java
│   ├── BaseViewModel.java
│   ├── AppExecutors.java
│   └── WorkerKeys.java                   (shared with Tracks 3, 4)
├── data/local/
│   ├── LectureLensDatabase.java
│   ├── entity/{Course,Lecture,Transcript,Segment,Notes}Entity.java
│   ├── dao/{Course,Lecture,Transcript,Search}Dao.java
│   └── SecureKeyStore.java
├── di/
│   ├── DatabaseModule.java
│   ├── NetworkModule.java                (skeleton — Track 4 fills provider)
│   ├── ExecutorsModule.java
│   └── RepositoryModule.java             (bindings — other tracks add theirs)
└── ui/auth/
    ├── LoginFragment.java
    └── LoginViewModel.java

app/src/main/res/navigation/nav_graph.xml
```

**Depends on:** nothing.
**Blocks:** everyone until Day 0 PR lands.
**Done when:**

- App compiles, empty nav graph runs, tapping placeholder destinations navigates without crashing.
- DB migrations pass a smoke test; Hilt graph resolves in a unit test.
- Login persists API key to EncryptedSharedPreferences and navigates to Library.
- Seed course + lecture visible from any consumer of `CourseDao` / `LectureDao`.

---

## Track 2 — Library UI (Daniel)

**Package:** `ui/library/`, `data/repository/`

**Scope**

- **LibraryFragment** — course list (RecyclerView), expandable to lecture cards, per-lecture status badge, tap → nav action to `lecture` destination with `lectureId` arg.
- `CoursesAdapter` + `LecturesAdapter` (or a single `ConcatAdapter`) using ViewBinding.
- **LibraryViewModel** exposing `LiveData<UiState>` (Loading / Success(courses) / Error).
- **CourseRepositoryImpl** backed by `CourseDao`.
- **LectureRepositoryImpl** — _read_ side only: `LiveData<List<Lecture>> observeAll()`, `LiveData<List<Lecture>> observeByCourse(long courseId)`, `LiveData<Lecture> getById(long id)`. Write side lives in Track 3.
- Status badge view (custom `AppCompatTextView` with tinted backgrounds pulled from theme roles).

**Files**

```
ui/library/
├── LibraryFragment.java
├── LibraryViewModel.java
├── CoursesAdapter.java
├── LecturesAdapter.java
└── StatusBadgeView.java
res/layout/
├── fragment_library.xml
├── item_course_header.xml
└── item_lecture_card.xml
data/repository/CourseRepositoryImpl.java
data/repository/LectureRepositoryImpl.java        (read methods)
```

**Depends on:** Track 1 Day 0 PR. Uses `CourseDao` / `LectureDao` from Track 1.
**Coordinates with Track 3** on `LectureRepositoryImpl.java` (read vs. write). Recommend splitting into `LectureReadRepositoryImpl` + `LectureWriteRepositoryImpl` to avoid file-level conflicts.
**Done when:** Library fragment renders seeded courses/lectures, shows status badges, tap navigates to a placeholder lecture destination.

---

## Track 3 — Record + Upload Pipeline (Adeniyi)

**Package:** `ui/upload/`, `data/audio/`, `data/repository/`, `processing/`

**Scope**

- **UploadFragment** — record button, waveform, elapsed timer, import-from-device path, upload progress. ViewBinding.
- **UploadViewModel** — recording state machine (`Idle → Recording → Paused → Saved`) exposed as `LiveData<RecordingState>`.
- **AudioRecorder** wrapper around `MediaRecorder` (M4A/AAC, 16 kHz mono per arch doc §1.2).
- **`RecordingService`** — foreground service so the mic keeps running when screen sleeps. Handles `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`.
- **RecordLectureUseCase** — creates the `Lecture` row (status `RECORDED`) on `AppExecutors.diskIO()`, writes audio path, enqueues `TranscribeWorker` via the orchestrator.
- **LectureRepositoryImpl** _write_ methods (`insert`, `updateStatus`, `updateAudioPath`).
- **PipelineOrchestrator** — WorkManager chain builder: `TranscribeWorker` → `SummarizeWorker` → `EmbeddingsWorker` (stretch). Uses `WorkerKeys` from Track 1. Track 4 implements the actual workers.

**Files**

```
ui/upload/
├── UploadFragment.java
├── UploadViewModel.java
└── RecordingIndicatorView.java
res/layout/fragment_upload.xml
data/audio/
├── AudioRecorder.java
└── RecordingService.java
data/repository/LectureRepositoryImpl.java        (write methods — coordinate with Track 2)
domain/usecase/RecordLectureUseCase.java
processing/PipelineOrchestrator.java              (WorkManager chain builder)
```

**Depends on:** Track 1 Day 0 PR (`LectureDao`, `WorkerKeys`, `AppExecutors`).
**Coordinates with Track 2** on `LectureRepositoryImpl.java`.
**Coordinates with Track 4** on the worker chain contract (input/output `Data` keys).
**Done when:** app records 5 minutes of audio, saves M4A to app storage, inserts a `RECORDED` lecture row, enqueues the transcription chain (Track 4 stubs return `Result.success()` for now).

---

## Track 4 — Transcription + Notes (Cloud) (Muhammad)

**Package:** `data/remote/`, `data/repository/`, `domain/usecase/`, `processing/worker/`

**Scope**

- **TranscriptionRepositoryImpl** calling **Google Cloud Speech-to-Text v2** via Retrofit (Java interface with `Call<T>` returns). Handle long audio via async recognize + LRO polling. Persist transcript + segments to Room on `AppExecutors.diskIO()`.
- **LlmRepositoryImpl** calling **Gemini** (Google AI SDK) for summarization + note generation.
- **TranscribeAudioUseCase**, **GenerateNotesUseCase** (with map-reduce for long transcripts per arch doc §3.2).
- **TranscriptChunker** — splits transcript by token budget for map-reduce.
- **Workers:** `TranscribeWorker`, `SummarizeWorker` extending `androidx.work.Worker` (synchronous `doWork()` — network calls block on `.execute()` inside the worker thread). Chained by Track 3's orchestrator. Read `Data` keys from `WorkerKeys`.
- Retry/backoff via `Result.retry()` from workers on 429s and transient failures; wire `NetworkModule` skeleton left by Track 1 with an OkHttp `Interceptor` for the API key + a `HttpLoggingInterceptor` in debug builds.
- **EmbeddingRepository** stub (no-op, stretch feature).

**Files**

```
data/remote/
├── SpeechToTextService.java              (Retrofit interface)
├── GeminiService.java                    (SDK wrapper)
└── dto/{RecognizeRequest,RecognizeResponse,GeminiRequest}.java
data/repository/
├── TranscriptionRepositoryImpl.java
├── LlmRepositoryImpl.java
└── EmbeddingRepositoryImpl.java          (no-op stub)
domain/usecase/
├── TranscribeAudioUseCase.java
└── GenerateNotesUseCase.java
domain/util/TranscriptChunker.java
processing/worker/
├── TranscribeWorker.java
└── SummarizeWorker.java
di/NetworkModule.java                     (fill Track 1 skeleton)
```

**Depends on:** Track 1 Day 0 PR + `WorkerKeys` + `NetworkModule` skeleton + `AppExecutors`.
**Coordinates with Track 3** on worker `Data` keys.
**Blocks on Google API creds** — start paperwork on Day 0.
**Done when:** given a lecture id whose audio file exists, the worker chain populates `transcripts`, `transcript_segments`, and `notes` tables, and updates `lectures.status` to `READY`. Verified with a 5-minute WAV.

---

## Track 5 — Lecture View + Search (Aaron)

**Package:** `ui/lecture/`, `ui/search/`, `core/player/`, `domain/usecase/`

**Scope**

- **LectureViewFragment** — three tabs via `TabLayout` + `ViewPager2`: `PlayerTabFragment`, `TranscriptTabFragment`, `NotesTabFragment`. ExoPlayer for audio; transcript RecyclerView auto-scrolls to the current segment via a periodic `Handler.postDelayed` reading `player.getCurrentPosition()`; notes rendered as a `RecyclerView` of typed rows (heading / bullet / key-term chip).
- **SearchFragment** — query box, results grouped by lecture, snippet with match highlighted. Uses FTS4 table set up by Track 1.
- **SearchLecturesUseCase**, **ExportLectureUseCase** (Markdown export to a shareable file via `FileProvider`).
- ViewModels: `LectureViewModel`, `SearchViewModel` exposing `LiveData<UiState>`.
- **AudioPlaybackController** — ExoPlayer wrapper with tap-a-segment-to-seek support.

**Files**

```
ui/lecture/
├── LectureViewFragment.java
├── LectureViewModel.java
├── PlayerTabFragment.java
├── TranscriptTabFragment.java
├── NotesTabFragment.java
├── TranscriptAdapter.java
└── NotesAdapter.java
res/layout/
├── fragment_lecture_view.xml
├── fragment_player_tab.xml
├── fragment_transcript_tab.xml
├── fragment_notes_tab.xml
├── item_transcript_segment.xml
└── item_notes_row.xml
ui/search/
├── SearchFragment.java
├── SearchViewModel.java
└── SearchResultsAdapter.java
res/layout/fragment_search.xml
core/player/AudioPlaybackController.java
domain/usecase/
├── SearchLecturesUseCase.java
└── ExportLectureUseCase.java
```

**Depends on:** Track 1 Day 0 PR + FTS4 table + `TranscriptDao` + `SearchDao`. Consumes read-only from `LectureRepository` and `TranscriptionRepository`. No dependency on Track 3–4 _implementations_ — can develop against seed data.
**Done when:** picking any lecture from Library opens the player + transcript + notes; tapping a transcript segment seeks the player; search returns hits with snippets; export writes a shareable `.md` file.

---

## Integration checkpoints

- **End of week 1** — Day 0 PR merged; each track scaffolded against seeded data + stub repos.
- **End of week 2** — Tracks 2 + 5 rendering real DB data; Track 3 recording + persisting; Track 4 workers hit Google APIs with a hard-coded audio file; Track 1 Auth complete.
- **End of week 3** — full pipeline works end-to-end for a lecture ≤ 5 min.
- **End of week 4** — long-lecture map-reduce + FTS search; polish + edge cases (permissions denied, no network, API quota).

Merge to `main` behind PR review; each track keeps a long-lived branch until week 2 sync.

---

## Parallelization risks (watch these)

- **`LectureRepositoryImpl.java` conflict.** Tracks 2 (reads) and 3 (writes) both touch it. Split into `LectureReadRepositoryImpl` + `LectureWriteRepositoryImpl` on Day 0 to avoid file-level merges.
- **Package rename must land first.** Day 0.5 rename to `com.lecturelens` (Ridwan) blocks every other Day 1 branch. If it slips, coordinate a merge window instead of letting tracks rebase on top of a rename.
- **`RepositoryModule.java` merges.** Everyone adds their `@Binds`. Alphabetize entries and expect small conflicts.
- **`nav_graph.xml` merges.** Every UI track adds a destination. Alphabetize destinations and add one action per PR to minimize interleaved edits.
- **Worker input/output keys.** `WorkerKeys.java` frozen on Day 0 by Track 1 — Tracks 3 and 4 both consume it. Any change is a joint PR.
- **Threading discipline.** In Java-with-LiveData, forgetting to hop off the main thread for DB writes is the classic bug. Enforce: all writes go through `AppExecutors.diskIO()`, all network through `AppExecutors.networkIO()` (or WorkManager). Code review checks for this.
- **Google API keys.** Track 4 blocks on the team getting Google Cloud creds. Start paperwork on Day 0.
- **Foundation slippage.** If Track 1 slips, every UI track slips. If it looks at risk by end of Day 1, drop `Settings` fragment + `Result.Loading` state first — bare theme + placeholders are enough to unblock others.
- **Track 1 workload.** Foundation + Data + Auth is heavier than any single other track. Front-loaded design is intentional; if the Auth portion looks tight in week 2, hand `LoginFragment` UI to Track 2 (Daniel) and keep the auth data + `SecureKeyStore` on Track 1.

---

## When you need to sync

- Contract changes (repository interface, model, `WorkerKeys`, nav destination arguments) → PR to Track 1's branch, discussed in a 15-min sync.
- Anything else — normal async code review.
