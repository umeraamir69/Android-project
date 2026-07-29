# LectureLens

Android lecture companion: **record → transcribe → structured notes → search / Ask AI / export / share**.

Master's course project (CP-670) with Clean Architecture (Java), Hilt, Room, WorkManager, Google Cloud Speech-to-Text, Gemini, and Firebase Auth.

## Features

- **Record / import** lecture audio (foreground service)
- **Cloud pipeline** — Google STT → Gemini notes (map-reduce for long lectures) → RAG embeddings
- **Ask AI (RAG)** — embed query, cosine retrieval, Gemini answer with **timestamp citations**
- **Firebase Auth** — Google Sign-In, email/password, passwordless email link
- **Library sync** — Firestore mirror of courses / lectures / notes / transcript text
- **On-device mode** — SpeechRecognizer + extractive local notes (Settings → Processing mode)
- **FTS search**, ExoPlayer ↔ transcript seek, handout OCR, export (MD/PDF/DOC), share codes **(notes + handout files)**
- **UsageLimiter** + OkHttp **RateLimitInterceptor** for daily cloud caps / 429 backoff

## Screenshots

Add device screenshots under `docs/screenshots/` and link them here.

## Setup

1. Clone and open in Android Studio (JDK 17).
2. Place Firebase config at `app/google-services.json` (package `com.lecturelens`).
3. Add keys to **`local.properties`** (gitignored):

```properties
sdk.dir=...
STT_API_KEY=your_speech_to_text_key
GEMINI_API_KEY=your_ai_studio_key
# Required for Google Sign-In (Firebase Console → Google provider → Web client ID)
FIREBASE_WEB_CLIENT_ID=xxxxx.apps.googleusercontent.com
```

4. Firebase Console:
   - Enable **Email/Password**, **Email link**, **Google**
   - Enable **Storage** (handouts + shared files)
   - Allow authenticated writes to `users/{uid}/**` and `shared/{code}/**` (download URLs use tokens)
   - Firestore collection `shared_notes` for share codes
   - Add your **debug SHA-1** / SHA-256 to the Android app
   - Re-download `google-services.json` (must include `oauth_client` entries)
5. Optional long-audio GCS path: `GCS_BUCKET` + `GCS_OAUTH_TOKEN`

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Architecture

```
ui/ → domain/ (use cases, models, repos) → data/ + processing/workers
```

- **DI:** Hilt  
- **DB:** Room (+ FTS4, embeddings table for RAG)  
- **Background:** WorkManager `TranscribeWorker → SummarizeWorker → EmbeddingsWorker`  
- **Auth:** Firebase Auth (`AuthRepository`)  
- **Routers:** `TranscriptionRouter` / `LlmRouter` (cloud vs on-device)

See [`docs/LectureLens_Architecture.md`](docs/LectureLens_Architecture.md).

## Portfolio talking points

- End-to-end mobile AI pipeline with WorkManager + quota/consent gates  
- RAG Q&A with local vector store (cosine over Gemini `text-embedding-004`)  
- Multi-method Firebase Auth + Firestore library sync  
- Privacy path: on-device processing mode without cloud STT/LLM  

## License

Course project — educational use.
