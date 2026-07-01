# LectureLens — UML Diagrams (PlantUML source)

These `.puml` files describe the most important flows and state machines in LectureLens. They're written in **PlantUML** so each one renders to a PNG/SVG via any of:

- **VS Code** — install the [PlantUML extension](https://marketplace.visualstudio.com/items?itemName=jebbs.plantuml), then `Alt+D` on an open `.puml` file.
- **IntelliJ / Android Studio** — built-in PlantUML support via the Diagrams plugin.
- **Web (no install)** — paste contents into [plantuml.com/plantuml](https://www.plantuml.com/plantuml/uml/).

**Style note.** The sequence diagrams (04, 05, 06) follow classic-UML conventions: named instances (`obj : Class`, shown underlined), explicit `new` / `create` arrows, `<<system>>` actors for external services (Cloud Storage, Cloud Speech-to-Text, Gemini), `loop [guard]` combined fragments, and self-call arrows for internal steps. `skinparam style strictuml` is set on each file to lock the rendering to this look.

- **CLI** — `plantuml diagrams/*.puml` produces a PNG next to each source file.

## Index

| #   | File                               | Type             | What it shows                                                                                                               |
| --- | ---------------------------------- | ---------------- | --------------------------------------------------------------------------------------------------------------------------- |
| 01  | `01_lecture_processing_state.puml` | State machine    | `lectures.status` lifecycle: RECORDED → TRANSCRIBING → TRANSCRIBED → SUMMARIZING → INDEXING → READY (or FAILED with retry). |
| 02  | `02_audio_recording_state.puml`    | State machine    | Upload-screen capture lifecycle: permission check, record/pause/resume/stop, import path, save.                             |
| 03  | `03_app_navigation_activity.puml`  | Activity diagram | Top-level UI navigation: Login → Library → {LectureView, Search, Upload, Settings}.                                         |
| 04  | `04_record_lecture_sequence.puml`  | Sequence diagram | Happy-path end-to-end: tap-record → MediaRecorder → WorkManager chain → GCS + Cloud STT + Gemini map-reduce → notes ready.  |
| 05  | `05_search_sequence.puml`          | Sequence diagram | Keyword search across the library using SQLite FTS4, jump to matched timestamp.                                             |
| 06  | `06_rag_qa_sequence.puml`          | Sequence diagram | _(stretch)_ Natural-language question answering with embedding retrieval + Gemini grounded generation + citations.          |
| 06  | `06_rag_qa_sequence.puml`          | Sequence diagram | _(stretch)_ Natural-language question answering with embedding retrieval + Gemini grounded generation + citations.          |

## How they relate to the architecture doc

- **State diagrams (01, 02)** are the authoritative definitions of the enum values and transitions referenced in §3.3 (Room schema) and §3.4 (Processing pipeline) of `LectureLens_Architecture.md`.
- **Activity diagram (03)** maps directly to the UI flow described in §3.1 and the proposal's "User Interface (UI/UX)" paragraph.
- **Sequence diagrams (04–06)** flesh out the data flows in §4 of the architecture doc.

## Rendering everything at once

From the project root:

```bash
# install once
brew install plantuml      # macOS
# or: sudo apt install plantuml

# render all diagrams as PNG
plantuml -tpng diagrams/*.puml

# or SVG (sharper, scales well in slides/docs)
plantuml -tsvg diagrams/*.puml
```

The generated images land next to each `.puml` file with the same base name.
