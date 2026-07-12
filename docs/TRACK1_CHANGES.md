# Track 1 — Database, Auth & Settings: File-by-File Breakdown

**Commit:** `3362a04` on `track1-database-auth` · 32 files, +1523 / −361
**Author:** Zeeshan Mahmood · **Replaces:** the in-memory `DevSeed` / `UploadTempDatabase` temporaries left by Tracks 2–4

Use this as the PR description. Reviewer focus points are marked ⚠.

---

## 1. Database layer (unblocks Tracks 2, 3, 5)

| File | Status | What was done |
|---|---|---|
| `data/local/LectureLensDatabase.java` | **new** | The real, persistent Room DB (v1, `lecturelens.db`). Registers all six entities and exposes the five DAOs. Version-history comment explains why we restart at v1 (the old v2 was in-memory; nothing persisted). |
| `data/local/entity/CourseEntity.java` | **new** | `courses` table per arch doc §3.3 (id, name, color, created_at). Was missing entirely — Track 2's course list ran on a fake list. |
| `data/local/entity/TranscriptFtsEntity.java` | **new** | ⚠ `transcripts_fts` — external-content FTS4 over `transcript_segments.text`. Room auto-generates sync triggers, so segments become searchable the moment Track 4's `TranscribeWorker` persists them. **No IndexingWorker is needed for keyword search.** FTS rowid == segment id (that's how search joins back to timestamps). |
| `data/local/dao/CourseDao.java` | **new** | `insert`, `observeAll` (LiveData), `count()` (sync, for the seeder's is-empty check). |
| `data/local/dao/SearchDao.java` | **new** | FTS4 `MATCH` query joining `transcripts_fts → transcript_segments → lectures`, returning lecture id/title, segment start-time, and a `snippet()` with `<b>` highlight markers. LiveData variant for Track 5's UI + sync variant for tests. Callers append `*` for prefix search. |
| `data/local/dao/LectureDao.java` | modified | Added the read side Track 2 was waiting for: `observeAll` / `observeByCourse` / `observeById` (LiveData, `date DESC`) + `count()`. Track 3's write methods untouched. |
| `data/local/SearchHit.java` | **new** | Result row POJO for `SearchDao` (lectureId, segmentId, startMs, lectureTitle, snippet). |
| `di/DatabaseModule.java` | rewritten | Was an empty skeleton. Now provides the singleton `LectureLensDatabase` (`Room.databaseBuilder`) and all five DAOs. ⚠ Uses `fallbackToDestructiveMigration()` as dev policy until v1 ships — swap for `addMigrations(...)` once real user data exists. |
| `di/UploadModule.java` | rewritten | Removed the temporary in-memory DB + DAO `@Provides` and the `PermissiveConsentGate` binding (both marked "DELETE when Track 1 lands"). Keeps Track 3's `WorkManager`, `AudioRecorder`, `AudioFileFactory` wiring. |
| `di/UploadTempDatabase.java` | **deleted** | Temp in-memory DB, superseded. |

## 2. Repositories (stub → DAO-backed)

| File | Status | What was done |
|---|---|---|
| `data/repository/LectureReadRepositoryImpl.java` | rewritten | Read half of `LectureRepository`, now `Transformations.map` over `LectureDao` LiveData. ⚠ Behavior change: `observeById` now emits `null` for a missing lecture (the old stub never emitted — screens could hang on Loading). Observers must null-check. |
| `data/repository/CourseRepositoryImpl.java` | rewritten | DAO-backed; class name + Hilt binding unchanged (as Track 2's TODO planned). `insert` is synchronous — diskIO only. |
| `data/repository/LectureEntityMapper.java` | **new** | `LectureEntity ↔ Lecture` mapping; unknown status strings degrade to `FAILED` instead of crashing. |
| `data/repository/DatabaseSeeder.java` | **new** | Idempotent (`courses.count() == 0` guard) demo seed: 1 course, a READY lecture **with transcript + 4 segments** (so Lecture View and FTS search have real rows), and a RECORDED lecture (exercises the status badge). Run on first sign-in. Protected no-arg ctor is a test seam (same pattern as `RecordLectureUseCase`). |
| `data/repository/DevSeed.java` | **deleted** | In-memory fake data, superseded by the seeder. |
| `data/repository/LectureRepositoryFacade.java` | comment only | Javadoc updated: both halves are now DAO-backed; inserts appear live in the Library. |

## 3. Auth + consent

| File | Status | What was done |
|---|---|---|
| `domain/repository/CredentialsStore.java` | **new** | Frozen-layer interface: email, API key, consent flag, `isSignedIn()`. ⚠ **Extends `ConsentGate`**, so one implementation answers both the auth screens and Track 3's pre-enqueue consent check. All methods hit disk — diskIO only. |
| `data/auth/SecureKeyStore.java` | **new** | `CredentialsStore` on **EncryptedSharedPreferences** (AES256-GCM master key), per arch doc §5. Prefs created lazily + double-checked locking. |
| `di/AuthModule.java` | **new** | Binds both `ConsentGate` and `CredentialsStore` to `SecureKeyStore`. |
| `data/consent/PermissiveConsentGate.java` | **deleted** | ⚠ The always-true stub is gone. `RecordLectureUseCase` now enforces real consent: **without the consent box ticked, recordings are saved locally but the cloud pipeline is not enqueued** (arch doc §1.1). |
| `data/remote/ApiKeyProvider.java` | modified | Key resolution seam agreed for Tracks 1+4: the user's key from `CredentialsStore` wins; `local.properties` → BuildConfig key remains the developer fallback. Constants (project id, models) untouched. |
| `ui/auth/LoginViewModel.java` | **new** | Validation (email must contain `@`; key non-empty), trims input, persists via diskIO, runs `DatabaseSeeder`, exposes `prefill` / field errors / `loading` / `signedIn` LiveData. Consent optional at sign-in. |
| `ui/auth/LoginFragment.java` | rewritten | Placeholder → real form. Observes field errors into `TextInputLayout.setError`, prefills once, navigates on success (nav action pops login off the back stack). |
| `res/layout/fragment_login.xml` | rewritten | Headline, email + API-key `TextInputLayout`s (password toggle on the key), consent `MaterialCheckBox` + explanatory note, continue button; ScrollView so it survives small screens/keyboards. |

## 4. Settings

| File | Status | What was done |
|---|---|---|
| `ui/settings/SettingsViewModel.java` | **new** | Loads current email/key/consent off-main; `saveApiKey` (validated, snackbar signal), `setCloudConsent`. Javadoc notes revocation stops *future* enqueues, not in-flight work. |
| `ui/settings/SettingsFragment.java` | rewritten | Placeholder → real screen: signed-in-as line, key edit + Save, consent `MaterialSwitch` (listener attached only after initial state so restore doesn't re-write the store), Theme Showcase button kept. |
| `res/layout/fragment_settings.xml` | rewritten | Layout for the above + divider before the design-tool section. |

## 5. Core, resources, tests

| File | Status | What was done |
|---|---|---|
| `core/AppExecutors.java` | modified | ⚠ Frozen-contract *internals* only (API unchanged): `mainThread()` is now lazily created. The eager `Handler(Looper.getMainLooper())` crashed JVM unit tests; lazy creation lets tests subclass with direct executors. |
| `res/values/strings.xml` | modified | Added Login + Settings sections; removed the two now-dead placeholders (`placeholder_login`, `placeholder_settings`). Other tracks' strings untouched. |
| `app/src/test/.../ui/auth/LoginViewModelTest.java` | **new** | 5 JVM tests: prefill, invalid email, empty key, persist+trim+seed+signal, consent-optional. Uses fakes (no Mockito) + `InstantTaskExecutorRule`. |
| `app/src/androidTest/.../data/local/DatabaseSmokeTest.java` | **new** | In-memory `LectureLensDatabase`: schema builds, seeder idempotent, lecture write round-trip, and FTS4 `searchSync("lifecycle*")` returns highlighted snippets. |
| `.idea/deploymentTargetSelector.xml` | accidental | IDE state that slipped in with `git add -A`. Recommend: `git rm --cached` + gitignore entry (see manual steps). |

---

## Manual steps (Zeeshan)

1. **Build + test locally** (not possible in the sandbox this was authored in):
   `Gradle Sync` → `./gradlew assembleDebug testDebugUnitTest`, and on an emulator `./gradlew connectedDebugAndroidTest` for the DB smoke test.
2. **Drop the IDE file** (optional but recommended):
   `git rm --cached .idea/deploymentTargetSelector.xml`, add `.idea/deploymentTargetSelector.xml` to `.gitignore`, amend or commit.
3. **Set your git identity** in this clone so IDE commits get the right author:
   `git config user.name "Zeeshan Mahmood"` · `git config user.email "zeeshanmahmood08@gmail.com"`
4. **Dev API keys:** either copy `local.properties.example` values (`STT_API_KEY`, `GEMINI_API_KEY`) into `local.properties`, or just sign in with a real key in-app — the in-app key now takes precedence.
5. **Push + PR:** `git push -u origin track1-database-auth`. CI (lint + assembleDebug) runs automatically on the PR.

## Notes for the team (put in the PR description)

- **Consent is now enforced.** To test the record → transcribe flow you must tick the consent checkbox at login (or flip the switch in Settings). Unticked = recording saved, no upload.
- **Data now persists** across app restarts. First sign-in seeds a demo course with a searchable transcript; delete app data to re-trigger seeding.
- **Track 5 (Aaron):** `SearchDao.search("<query>*")` gives lecture title, segment timestamp (`startMs`), and an HTML-highlighted snippet — everything the search screen and the `seekMs` nav arg need.
- **Track 2 (Daniel):** `observeById` emits `null` for missing lectures — null-check in the observer.
- **Track 4 (Muhammad):** `ApiKeyProvider` now prefers the user's stored key; BuildConfig fallback unchanged. No changes needed on your side.
