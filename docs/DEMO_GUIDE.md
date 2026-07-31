# LectureLens — Demo Guide & Feature List

Use this document to plan the **demo video** and live presentation.  
App package: `com.lecturelens` · Version: see Help menu (Settings / any toolbar **?**)

Also see: root [`README.md`](../README.md) · [`SUBMISSION.md`](SUBMISSION.md)

---

## Suggested demo account (fill in before submit)

| Field | Value |
|-------|--------|
| Email | ______________________________ |
| Password | ______________________________ |
| Notes | Create via **Create account** in the app, or use an existing Firebase Auth user. |

Include this table in your written submission if the professor requires username/password.

---

## Demo video outline (~4–7 minutes)

Record on a **real phone** if possible (emulator network can fail).

### 1. Opening (15–20 s)
- Show the **app icon** on the home screen → launch LectureLens  
- Mention: *“LectureLens turns lecture audio into searchable notes with AI.”*

### 2. Sign up / Sign in (30–45 s)
- Open **Create account**  
- Enter email, password, **username**, **university** (and optional program)  
- Check cloud consent → **Create account**  
- Or sign in with the demo email/password  

### 3. Home dashboard (30 s)
- Show **stats** (lectures / categories / ready)  
- Show **shortcut icons**: Record, Library, Search, Shared  
- Point out **Help (?)** — authors, version, instructions  
- Open a **recent lecture** from the ListView  

### 4. Record / Import (45–60 s)
- Home → **Record** (or Library FAB)  
- Start recording briefly → Stop → save  
- Or **Import** an audio file  
- Show **progress** while the pipeline runs (transcribe → notes)  

### 5. Lecture detail (60–90 s)
- **Player** tab — play / seek  
- **Transcript** tab — tap a segment to seek  
- **Notes** tab — summary, **key-term chips**, action items  
- **Ask AI** — ask a question; show chat + answer  
- Toolbar: **Add handout**, **Share / Export**, **Help**  

### 6. Library (30–45 s)
- Expandable **ListView** of categories + lectures  
- **Add category** (name + professor)  
- Open a lecture → back  
- Optional: rename / delete / move (long-press or menus)  

### 7. Search (30 s)
- Search a keyword from the lecture  
- Show filters (All / Transcript / Notes / Chat)  
- Tap a result → opens lecture (with seek when transcript hit)  

### 8. Share & import (30–45 s)
- From lecture → **Share** → **In-app share code**  
- Copy the 6-character code  
- Home → **Shared** → paste code → notes appear with Shared badge  

### 9. Settings & French (30–40 s)
- Profile fields (name, university, etc.)  
- Theme / processing mode  
- **App language → Français** → UI switches  
- Switch back to English if needed  
- Sign out (optional end)  

### 10. Closing (10 s)
- *“That’s LectureLens — record, notes, Ask AI, search, share, French UI.”*

---

## Full feature list (what we built)

### Auth & profile
- Email / password **sign in**
- **Create account** screen with student profile (username, university, program, student ID, full name)
- Cloud consent checkbox
- Sign out in Settings
- Student profile saved for exports / shared notes attribution

### Home
- Greeting + signed-in email
- Stats: lecture count, categories, ready count
- Processing / failed banners
- Shortcut grid (animated): Record, Library, Search, Shared
- Recent lectures **ListView** (tap → detail)
- Import shared notes by 6-character code (dialog + EditText)
- Help button
- Progress bar while importing

### Library
- Courses / categories with professor
- Lectures grouped under categories (**ListView**)
- Expand / collapse
- Status filter chips (All, Ready, Failed, Processing, Shared, Recorded)
- Add / rename / delete category
- Rename / move lecture
- FAB → record
- Toolbar: Search, Add category, Settings, Help
- Progress bar while loading
- Toast / Snackbar / dialogs for errors and confirms

### Record / Import (Upload)
- Live recording with timer / waveform-style indicator
- Pause / resume / stop
- Import audio from device
- Category picker + lecture title
- STT language picker
- Permission handling
- Progress while saving
- Navigates to lecture when saved
- Help menu
- Toast + dialogs

### Lecture view
- Tabs: **Player**, **Transcript**, **Notes**
- ExoPlayer playback + seek from transcript
- Status / pipeline timeline when processing
- Rename / move lecture
- Re-transcribe / retry notes
- Export / share sheet:
  - Share as text
  - WhatsApp
  - Markdown / PDF / Word
  - **Firebase share code**
- Add handout (image, PDF, Word, text)
- Handout list: open / delete
- Progress indicators
- Help menu

### Notes & Ask AI
- Summary (markdown), key terms (wrapping chips), action items
- Full **Ask AI chat** (persisted per lecture)
- Prior chat sent as context to Gemini
- RAG retrieval with timestamp citations (when embeddings available)
- Clear chat
- On-device extractive Ask AI when cloud off / on-device mode

### Search
- Search across transcript, notes, key terms, actions, Ask AI chat
- Autocomplete suggestions
- Filter chips
- Results **ListView** → lecture detail
- Progress bar
- Help, Toast / Snackbar / dialog

### Settings
- Profile edit + save
- Gemini API key (optional fallback; prefers `local.properties`)
- Cloud consent switch
- Processing mode: Cloud / On-device / Auto
- Theme: System / Light / Dark
- STT language
- **App UI language: English / Français**
- Theme showcase (design samples)
- Help menu

### Cloud & AI pipeline
- Google Cloud Speech-to-Text (sync + optional GCS long audio)
- Gemini structured notes (map-reduce for long lectures)
- Embeddings + local cosine RAG (Room vector store)
- WorkManager: Transcribe → Summarize → Embeddings
- Firestore library sync (courses, lectures, notes, transcript, chat, handout metadata)
- Firebase Storage for handouts / shared files
- Usage limits / rate limiting helpers

### Persistence & offline
- Room database (courses, lectures, transcripts, notes, handouts, chat, embeddings, FTS)
- Non-destructive migrations from DB v7+
- Lectures remain readable offline after processing

### Course rubric extras
- Multiple **Fragments** per section
- Dedicated **Activities** for Library, Search, Upload, Settings, Lecture
- **ListView** on Home / Library / Search
- **AsyncTask** wrapper (`BgAsyncTask`) for DB/network-style work
- Progress bars, 2+ buttons, EditTexts per section
- Toast + Snackbar + custom dialogs
- Help dialog: authors, version, instructions
- **French** localization (`values-fr`)
- **Animations**: screen enter, nav transitions, list fall-down, shortcut stagger, button press scale

### Authors (Help dialog)
Muhammad Umer Amir, Zeeshan Mahmood, Daniel Monday-Ogidi, Adeniyi Ridwan Adetunji, Aaron Gullraiz Cecil

---

## Talking points for marks (Complexity / Originality / Usability)

1. **End-to-end AI pipeline** on mobile (record → STT → Gemini notes → RAG Ask AI).  
2. **Grounded Q&A** with citations, not a generic chatbot.  
3. **Share codes** so classmates import notes + handouts.  
4. **Privacy path**: on-device / consent / processing mode.  
5. **Polish**: French UI, animations, Help, Material design, custom icon.

---

## Build APK for submission

```bash
./gradlew assembleDebug
```

APK path:
`app/build/outputs/apk/debug/app-debug.apk`

(Or use Android Studio → **Build → Build APK(s)**.)

Do **not** publicly share an APK that embeds real API keys from `local.properties`.

---

## Pre-demo checklist

- [ ] Demo account created; email/password written above  
- [ ] Fresh APK installed on a phone  
- [ ] At least one READY lecture with notes  
- [ ] Ask AI returns an answer  
- [ ] Share code works once  
- [ ] French language switch shown once  
- [ ] Help dialog shown once  
- [ ] Video recorded with clear audio  

---

*Generated for LectureLens course submission / demo video planning.*
