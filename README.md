# LectureLens

**CP-670 / Master's Android course project**

Android lecture companion: **record → transcribe → structured notes → Ask AI → search → export / share**.

| | |
|---|---|
| **Package** | `com.lecturelens` |
| **Language** | Java (Views + ViewBinding, no Compose) |
| **Min SDK** | 29 (Android 10+) |
| **Demo guide** | [`docs/DEMO_GUIDE.md`](docs/DEMO_GUIDE.md) |
| **Architecture** | [`docs/LectureLens_Architecture.md`](docs/LectureLens_Architecture.md) |

---

## Team

| Name | Role / track |
|------|----------------|
| **Muhammad Umer Amir** | Transcription + notes (cloud), RAG, integration |
| **Zeeshan Mahmood** | Foundation, data layer & auth |
| **Daniel Monday-Ogidi** | Library UI |
| **Adeniyi Ridwan Adetunji** | Record + upload pipeline |
| **Aaron Gullraiz Cecil** | Lecture view + search |

---

## Demo credentials (for markers)

> Fill these before submission and keep this table updated.

| Field | Value |
|-------|--------|
| **Email** | `________________@______.com` |
| **Password** | `________________` |
| **How to create** | App → **Create account** (username + university required) |

APK (local build): run `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`  
Demo walkthrough script: **[`docs/DEMO_GUIDE.md`](docs/DEMO_GUIDE.md)**

---

## Features

### Core product
- **Record / import** lecture audio (foreground mic service)
- **Cloud pipeline** — Google Speech-to-Text → Gemini notes → RAG embeddings (WorkManager)
- **Ask AI (RAG)** — embeddings + cosine search + Gemini answers with timestamp citations; prior chat used as context
- **Library** — courses/categories (with professor), lectures, filters, add/rename/delete
- **Lecture view** — Player (ExoPlayer), Transcript (seek), Notes (summary / key terms / actions)
- **Handouts** — attach images/PDF/docs; OCR / extract; open & delete
- **Search** — FTS + notes/chat; filters; autocomplete
- **Share / export** — Markdown, PDF, Word, WhatsApp/text, **6-character Firebase share codes** (notes + handout files)
- **Home dashboard** — stats, shortcuts, recent lectures, import shared code

### Auth & profile
- Firebase **email/password** sign-in
- **Create account** screen with student profile (username, university, program, etc.)
- Cloud consent + optional API key in Settings

### Course / rubric extras
- Section **Fragments** + dedicated **Activities** (Library, Search, Upload, Settings, Lecture)
- **ListView** lists on Home / Library / Search
- **AsyncTask** wrappers for background work (`BgAsyncTask`)
- Progress bars, buttons, EditTexts, Toast + Snackbar + dialogs
- **Help** menu — authors, version, how-to
- **French** UI (`Settings → App language → Français`)
- **Animations** — screen enter, navigation transitions, list stagger, button press
- Firebase Auth / Firestore sync / Storage (encouraged)

### Privacy & robustness
- Processing modes: Cloud / On-device / Auto
- On-device extractive notes & Ask AI when offline or selected
- Room persistence + non-destructive migrations (v7+)
- Release minify + ProGuard keep rules
- Usage limits / rate-limit handling for cloud APIs

---

## Screenshots

Add device screenshots under `docs/screenshots/` and link them here after the demo recording.

| Screen | File |
|--------|------|
| Home | `docs/screenshots/home.png` |
| Library | `docs/screenshots/library.png` |
| Notes / Ask AI | `docs/screenshots/notes.png` |
| Search | `docs/screenshots/search.png` |

---

## Setup (Android Studio)

1. Clone this repository and open the root folder in **Android Studio** (JDK 17).
2. Ensure Firebase config exists at **`app/google-services.json`** (package `com.lecturelens`).
3. Add keys to **`local.properties`** (gitignored — never commit):

```properties
sdk.dir=/path/to/Android/sdk
STT_API_KEY=your_speech_to_text_key
GEMINI_API_KEY=your_ai_studio_key
# Optional long-audio path:
# GCS_BUCKET=your-bucket
# GCS_OAUTH_TOKEN=ya29....
```

4. Firebase Console (project must match `google-services.json`):
   - Enable **Email/Password** authentication
   - Enable **Firestore** + collection `shared_notes` for share codes
   - Enable **Storage** — sample rules: [`docs/firebase-storage.rules`](docs/firebase-storage.rules)
5. Build & run:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Install debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Architecture

```
ui/  →  domain/ (use cases, models, repos)  →  data/ + processing/workers
```

| Concern | Choice |
|---------|--------|
| UI | Fragments, XML, ViewBinding, Navigation Component |
| DI | Hilt |
| Local DB | Room (+ FTS4, embeddings) |
| Background | WorkManager: `TranscribeWorker → SummarizeWorker → EmbeddingsWorker` |
| Auth | Firebase Auth |
| Cloud AI | Google STT + Gemini |
| Sync | Firestore library sync |

Routers: `TranscriptionRouter` / `LlmRouter` (cloud vs on-device).

More detail: [`docs/LectureLens_Architecture.md`](docs/LectureLens_Architecture.md) · work split: [`docs/WORK_BREAKDOWN.md`](docs/WORK_BREAKDOWN.md)

---

## Repository layout (useful docs)

| Path | Description |
|------|-------------|
| [`docs/DEMO_GUIDE.md`](docs/DEMO_GUIDE.md) | Video script + full feature checklist for markers |
| [`docs/LectureLens_Architecture.md`](docs/LectureLens_Architecture.md) | System architecture |
| [`docs/WORK_BREAKDOWN.md`](docs/WORK_BREAKDOWN.md) | Team tracks |
| [`docs/firebase-storage.rules`](docs/firebase-storage.rules) | Suggested Storage security rules |
| `app/` | Android application module |

---

## Marking / demo talking points

1. End-to-end **mobile AI pipeline** (record → STT → Gemini → RAG Ask AI).  
2. Answers **grounded in the lecture** with citations, not a generic chatbot.  
3. Classmates can **import notes via share codes**.  
4. **Privacy path**: consent + on-device mode.  
5. Course extras: Help, French, ListView, AsyncTask, Activities, animations.

---

## License

Course project — educational use only. Do not publish API keys or production Firebase credentials.
