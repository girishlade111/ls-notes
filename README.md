# LS Notes

**A premium, artistic, offline-first local note-taking application for Android.**

LS Notes is a full-featured, single-device note-taking app built entirely on modern Android
tooling — **Kotlin**, **Jetpack Compose (Material 3)**, and **Room**. Every note, notebook,
tag, sketch, and revision lives in a local SQLite database on your device. No account,
no cloud, no sync, no tracking. Your notes are private by design.

```
                ┌────────────────────────────────────────────┐
                │           LS Notes (Android App)           │
                └────────────────────────────────────────────┘
```

---

## Table of Contents

- [Highlights](#highlights)
- [Features](#features)
- [Note Types](#note-types)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Building & Running](#building--running)
- [Signing](#signing)
- [Testing](#testing)
- [Import / Export](#import--export)
- [Home Screen Widgets](#home-screen-widgets)
- [Settings](#settings)
- [Security & Privacy](#security--privacy)
- [FAQ](#faq)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Highlights

| | |
|---|---|
| **Offline-first** | 100% local storage. Works with zero network access. |
| **Rich note types** | 11 distinct note types from checklists to sketches to tables. |
| **Organization** | Notebooks, tags, pinning, favorites, archive, trash. |
| **Private Safe** | Passcode + biometric-protected vault for sensitive notes. |
| **Version history** | Automatic and manual snapshots, with one-tap restore. |
| **Import & export** | Markdown, TXT, JSON, Evernote ENEX, PDF, HTML, ZIP. |
| **Home screen widgets** | Pinned-notes list and quick-action creation bar. |
| **Theming** | Light / Dark / System themes, custom accent color, per-note colors. |

---

## Features

### Note Management
- Create, edit, delete, duplicate, and restore notes.
- **11 note types** (see [Note Types](#note-types)).
- Pin favorites to the top, mark notes as favorites.
- Archive notes to declutter without deleting.
- Soft-delete to Trash with timestamp; restore or permanently delete.
- Search across titles and content with filterable results.

### Organization
- **Notebooks** — group notes; create, rename, and manage notebooks with
  configurable deletion behavior (delete contents / move contents).
- **Tags** — lightweight, comma-separated tagging; colored chips in grid view.
- **Views** — switch between Grid (visual, color-coded cards) and List views.
- **Sorting** — sort by created date, updated date, title, or default order,
  ascending/descending.

### Editor
- **Basic Editor** — plain focused writing, optional spell check.
- **Advanced Editor** — rich formatting toolbar (bold, italic, headings,
  bullet/numbered lists, quote blocks, code blocks, etc.).
- Per-note **font family** and **font size** overrides.
- Inline creation and editing of **checklists** with sub-items (indentation).
- **Drawing canvas** with blank / dotted / ruled / grid paper styles,
  stroke colors, width, and eraser, saved as a sketch image.
- Attach **photos, audio recordings, and arbitrary files** per note.
- **Tables** — insert `N × M` tables and edit cells inline.
- **Smart Cards / Link Bookmarks** — capture a URL with title, description,
  optional icon and cover image, categorized by data type.
- **Code notes** with monospaced formatting for source snippets.

### Version History
- Automatic snapshots are saved when editing is paused/closed.
- **Manual snapshots** can be taken at any time ("Save Version").
- Full **history screen** per note: browse revisions, preview older versions,
  and **restore** any previous version (with an automatic pre-restore backup).
- Duplicate any historical version into a brand-new note.

### Private Safe
- Locked vault section for private notes (passcode-protected).
- Optional **biometric authentication** (fingerprint / face) on unlock.
- Auto-lock after a configurable idle period (default 1 minute).
- Hidden from widgets, search, and the recent list.

### Import / Export
- **Export** a single note or all notes to:
  - `PDF`, `Plain Text (.txt)`, `Markdown (.md)`, `HTML (.html)`,
    `JSON (.json)`, or `ZIP` packages.
- **Import** from:
  - Markdown (`.md`), plain text (`.txt`), LS Notes backup (`JSON`),
    and **Evernote ENEX** exports.
- **Full backup / restore** — dump the entire database to
  `ls_notes_backup_<timestamp>.json` and restore it later.

### Home Screen Widgets
- **Pinned Notes widget** — live list of your pinned notes on the home screen,
  taps open the note directly.
- **Quick Action widget** — buttons for new note, search, microphone, drawing,
  checklist, and pin quick actions.
- Both widgets deep-link back into the app (`singleTop` activity + intent handling).

---

## Note Types

| Type | Description |
|---|---|
| `TEXT` | Standard rich-text note. |
| `CHECKLIST` | Interactive checkbox list with indented sub-items. |
| `PHOTO` | Note with attached image(s). |
| `AUDIO` | Note with attached audio recording(s). |
| `SKETCH` | Hand-drawn canvas note (paper style, colors, eraser). |
| `FILE` | Note with arbitrary file attachments. |
| `SMART_CARD` | URL bookmark card with title, description, icon, and cover. |
| `LINK_BOOKMARK` | Lightweight URL bookmark. |
| `CODE` | Code-snippet note with monospaced styling. |
| `TABLE` | Structured table note with resizable rows/columns. |
| `DOCUMENT_SCAN` | Note intended for document-style content. |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 (Compose BOM 2024.09.00), Material Icons |
| Navigation | Manual section-based navigation (navigation-compose available) |
| Persistence | Room 2.7.0 (SQLite), KSP codegen, DataStore Preferences |
| Networking | Retrofit 2.12.0 + OkHttp 4.10.0 + Moshi 1.15.2 |
| AI (optional) | Firebase AI (`firebase-ai`) via Firebase BoM 34.15.0 |
| Security | Firebase App Check (reCAPTCHA), AndroidX Biometric 1.1.0 |
| Images | Coil 2.7.0 (+ Coil Compose) |
| Permissions | Accompanist Permissions 0.37.3 |
| Testing | JUnit 4, Robolectric 4.16.1, Roborazzi 1.59.0 (screenshot tests), Espresso 3.7.0, Compose UI Tests, coroutines-test |
| Build | AGP 9.1.1, Gradle (configuration cache + build cache on) |
| Secrets | Google Secrets Gradle Plugin 2.0.1 (`.env` / `.env.example`) |

### Minimum Requirements
- **minSdk**: 24 (Android 7.0 Nougat)
- **targetSdk / compileSdk**: 36
- **JDK**: 11+ (Java 11 source/target compatibility)
- **Gradle**: requires a Gradle distribution compatible with AGP 9.1.1
  (use the included Gradle wrapper)

---

## Architecture

Clean, layered architecture with unidirectional data flow:

```
┌────────────────────────────────────────────────────────────────────┐
│  UI Layer — Jetpack Compose                                       │
│  Screens (MainScreen, NoteEditorScreen, ...) + Components          │
└──────────────────────────────┬─────────────────────────────────────┘
                               │ StateFlows / StateFlow + events
┌──────────────────────────────▼─────────────────────────────────────┐
│  ViewModels (presentation logic)                                   │
│  NotesViewModel (app-wide state, navigation, filters)              │
│  NoteViewModel (single-note editing, history snapshots)            │
└──────────────────────────────┬─────────────────────────────────────┘
                               │ suspend calls / flows
┌──────────────────────────────▼─────────────────────────────────────┐
│  Repository Layer                                                  │
│  LsNotesRepository, NoteRepository, NoteImportService              │
└──────────────────────────────┬─────────────────────────────────────┘
                               │
┌──────────────────────────────▼─────────────────────────────────────┐
│  Data Layer — Room (SQLite) + SharedPreferences/DataStore          │
│  DAOs: NoteDao, NotebookDao, TagDao, NoteHistoryDao                │
│  LocalFileManager, SettingsManager, LocalFileManager               │
└────────────────────────────────────────────────────────────────────┘
```

Key design decisions:

- **StateFlow-based state management** — the UI observes immutable state flows;
  all mutations go through the ViewModel (`collectAsState` in Compose).
- **Singleton Room database** (`LsNotesDatabase.getInstance`) with
  `fallbackToDestructiveMigration()`.
- **Repository facade** — `LsNotesRepository` wraps all DAOs and settings,
  exposes high-level operations used by ViewModels and widgets.
- **Atomic imports** — multi-file import runs inside a single database
  transaction.
- **Widgets bridge** — `PinnedNotesWidgetProvider.sendRefresh()` notifies the
  app; `handleWidgetIntent()` in `NotesViewModel` dispatches
  `WidgetNavigationEvent` (open note / create note / search) to the UI.
- **Biometric helper** — `BiometricAuthManager` wraps AndroidX Biometric for
  the Private Safe unlock flow.

---

## Project Structure

```
ls-notes/
├── .env.example                  # GEMINI_API_KEY placeholder (Secrets plugin)
├── build.gradle.kts              # Root build script (plugin declarations)
├── settings.gradle.kts           # Repositories, root project "LS Notes"
├── gradle.properties             # JVM args, caching, config-cache settings
├── gradle/
│   └── libs.versions.toml        # Version catalog (single source of truth)
├── metadata.json                 # App metadata (AI Studio descriptor)
└── app/
    ├── build.gradle.kts          # App module: SDK, build types, signing,
    │                             #   secrets plugin, dependencies
    ├── proguard-rules.pro        # Release ProGuard rules
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/
        │   │   ├── MainActivity.kt              # singleTop activity, theme, intent routing
        │   │   ├── di/DatabaseModule.kt         # Manual DI wiring
        │   │   ├── data/
        │   │   │   ├── model/                   # Note, Notebook, Tag, NoteHistory,
        │   │   │   │                            #   enums (sort order, theme, view mode...)
        │   │   │   ├── local/                   # DAOs, Room DB, SettingsManager,
        │   │   │   │                            #   LocalFileManager (backups)
        │   │   │   └── repository/              # LsNotesRepository, NoteRepository,
        │   │   │                                #   NoteImportService (MD/TXT/JSON/ENEX)
        │   │   ├── ui/
        │   │   │   ├── components/              # NoteCard, ExportDialog, ImportDialog,
        │   │   │   │                            #   DrawingCanvas, RichFormattingToolbar,
        │   │   │   │                            #   PrivateSafeAuthDialog, NoteInfoSheet,
        │   │   │   │                            #   NoteHistoryDialog, ExportBottomSheet
        │   │   │   ├── screens/                 # AllNotes, Notebooks, ManageNotebooks,
        │   │   │   │                            #   Tags, Pinned, Recent, Archive,
        │   │   │   │                            #   PrivateSafe, Trash, Settings,
        │   │   │   │                            #   ImportExport, NoteEditor, NoteHistory
        │   │   │   ├── theme/                   # Color, Type, Theme (dynamic theming)
        │   │   │   └── viewmodel/               # NotesViewModel, NoteViewModel
        │   │   ├── util/BiometricAuthManager.kt
        │   │   └── widget/                      # PinnedNotesWidgetProvider,
        │   │                                    #   QuickActionWidgetProvider,
        │   │                                    #   PinnedNotesRemoteViewsService
        │   └── res/                             # Layouts for widgets, drawables, values,
        │                                        #   xml (widget info, backup rules)
        ├── test/                                # JVM unit + Robolectric + Roborazzi
        │                                        #   screenshot tests
        └── androidTest/                         # Instrumented tests (Espresso, Compose)
```

---

## Database Schema

Room database `ls_notes_database` (version **4**, 4 entities):

### `notes`
| Column | Type | Notes |
|---|---|---|
| `id` | Long | Primary key, auto-generated |
| `title` | String | |
| `content` | String | |
| `type` | String (enum) | One of 11 note types |
| `notebookId` | Long? | FK → `notebooks.id` (SET_NULL on delete) |
| `notebookName` | String | Denormalized display name |
| `tagsCsv` | String | Comma-separated tag names |
| `colorHex` | String | Card color |
| `isPinned`, `isFavorite`, `isArchived`, `isPrivate`, `isInTrash` | Boolean | State flags |
| `trashedTimestamp` | Long | |
| `createdTimestamp`, `updatedTimestamp` | Long | |
| `checklistJson`, `attachmentsJson` | String | JSON payloads |
| `sketchPath` | String? | |
| `smartCardMetaJson`, `tableDataJson` | String? | |
| `fontName`, `fontSizeSp` | String? / Float? | Editor overrides |

Indexed on: `notebookId`, `isPinned`, `isFavorite`, `isArchived`, `isPrivate`,
`isInTrash`, `updatedTimestamp`, `createdTimestamp`.

### `notebooks`, `tags`, `note_history`
- **Notebooks** — notebook definitions referenced by notes.
- **Tags** — tag metadata for chip rendering.
- **Note history** — full version snapshots (`content`, `changeSummary`,
  `timestamp`) of each note for the restore flow.

**Settings** are stored in `SharedPreferences` (`ls_notes_settings`):
theme mode, accent color, default note color mode/color, time display mode,
view mode, editor mode, editor font/size, spell check, Private Safe passcode,
and auto-lock minutes.

---

## Getting Started

### Prerequisites
- **Android Studio** (latest stable, e.g. Meerkat/Narwhal or newer) with
  Android SDK **36**
- **JDK 17+** (AGP 9.x requires a recent JDK; the project targets Java 11 bytecode)
- An Android device or emulator (API 24+)
- *(Optional)* A Gemini API key for AI features

### 1. Clone
```bash
git clone https://github.com/<your-org>/ls-notes.git
cd ls-notes
```

### 2. Open in Android Studio
`File → Open…` → select the project root. Android Studio will pick up the
Gradle wrapper and version catalog automatically. The first sync downloads
all dependencies (AGP 9.1.1, Compose, Room, etc.).

### 3. Configure environment (optional)
Create a `.env` file next to `.env.example`:

```bash
# .env  (used by the Secrets Gradle Plugin)
GEMINI_API_KEY=your_gemini_api_key_here
```

> The `secrets` Gradle plugin reads `.env` for local builds and falls back to
> `.env.example` when no `.env` exists. If you leave `GEMINI_API_KEY`
> commented out, the key is simply **not packaged** in the APK — the app
> still builds and runs. In AI Studio, keys are injected at runtime via the
> Secrets panel instead.

> **Note:** Despite the `.env.example` comment implying an AI Studio workflow,
> this project is fully buildable without any network or Firebase
> configuration. `google-services.json` is optional
> (`googleServices.missing.passthrough=true`).

### 4. Run
Select the `app` run configuration and press **Run** (▶), or from CLI:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Environment Variables

| Variable | Used for | Required? |
|---|---|---|
| `GEMINI_API_KEY` | Gemini AI API calls (injected by Secrets plugin) | No (optional AI features) |
| `KEYSTORE_PATH` | Release signing keystore path (defaults to `my-upload-key.jks` in repo root) | Only for release builds |
| `STORE_PASSWORD` | Release keystore password | Only for release builds |
| `KEY_PASSWORD` | Release key password | Only for release builds |

Debug builds sign automatically with the checked-in `debug.keystore`
(`android` / `androiddebugkey`).

---

## Building & Running

```bash
# Debug APK (fast, default)
./gradlew assembleDebug

# Release APK (requires signing env vars or my-upload-key.jks)
./gradlew assembleRelease

# Install on connected device/emulator
./gradlew installDebug

# Clean build
./gradlew clean

# All checks: tests + lint
./gradlew check
```

### Build configuration highlights
- `org.gradle.configuration-cache=true` and `org.gradle.caching=true` — fast
  incremental builds.
- `kotlin.compiler.execution.strategy=in-process` — avoids the "could not
  connect to Kotlin compile daemon" failure mode.
- `compileSdk` uses minor-API-level pinning (`release(36) { minorApiLevel = 1 }`).
- Release builds: minification **disabled** by default (`isMinifyEnabled = false`),
  standard ProGuard rules included for when you enable it.

---

## Signing

### Debug
Automatic — uses the repo-root `debug.keystore` with well-known credentials
(`android` / `androiddebugkey`).

### Release
The release build type points at a keystore resolved like this:

| If set | Location |
|---|---|
| `KEYSTORE_PATH` env var | That path |
| Otherwise | `<project-root>/my-upload-key.jks` |

Passwords come from `STORE_PASSWORD` and `KEY_PASSWORD` (no defaults).
Generate a key with:

```bash
keytool -genkeypair -v -keystore my-upload-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

---

## Testing

The project has three test layers:

### 1. JVM unit tests
```bash
./gradlew testDebugUnitTest
```
Plain JUnit tests for pure logic.

### 2. Robolectric + Roborazzi screenshot tests
```bash
./gradlew testDebugUnitTest --tests "*Robolectric*" --tests "*Screenshot*"
```
- **Robolectric** runs logic against a simulated Android environment on the JVM.
- **Roborazzi** records Compose UI screenshots and verifies them for visual
  regressions (`GreetingScreenshotTest`).

### 3. Instrumented tests (device/emulator)
```bash
./gradlew connectedAndroidTest
```
Espresso + Compose UI test runner (`ExampleInstrumentedTest`).

> Roborazzi render output and configuration live in the `app` module's
> Roborazzi plugin; record mode via `-Proborazzi.test.record=true`.

---

## Import / Export

### Export
Available per-note and bulk (`Import/Export` section):

| Format | Extension | Purpose |
|---|---|---|
| PDF | `.pdf` | Printable/shareable document |
| Plain text | `.txt` | Universal plain content |
| Markdown | `.md` | Portable, git-friendly |
| HTML | `.html` | Web-renderable document |
| JSON | `.json` | Machine-readable, LS Notes-native |
| ZIP | `.zip` | Packaged bundle for bulk export |

### Import
`Import/Export → Import`, multi-select supported:

| Source | Format |
|---|---|
| Evernote exports | `.enex` (XML parsed offline) |
| Markdown | `.md` |
| Plain text | `.txt` |
| LS Notes backup | `.json` |

All parsing is performed **100% offline** by `NoteImportService` (content
detection → parse → transactional insert). Imported notes land in an
"Imported Notes" notebook by default. Auto-detected note types are converted
to structured `Note` rows (including evidence of ENEX metadata handling).

### Backup / Restore
- **Backup:** writes `ls_notes_backup_<timestamp>.json` via
  `LocalFileManager` — a complete snapshot of all tables.
- **Restore:** import the JSON backup through the import flow.

---

## Home Screen Widgets

### 1. Pinned Notes
`PinnedNotesWidgetProvider` + `PinnedNotesRemoteViewsService`:
- Displays your pinned notes in a scrollable `ListView` (RemoteViews).
- Custom fallback tap → `ACTION_REFRESH_WIDGET` broadcast → update.
- Tap any row → deep-link opens the note in-app (via
  `handleWidgetIntent`, launch-mode `singleTop`).

### 2. Quick Action Bar
`QuickActionWidgetProvider` — instant creation shortcuts:
- ➕ New note
- 🔍 Search
- 🎤 Audio note
- ✏️ Sketch
- ☑️ Checklist
- 📌 Quick-pin actions

Both widgets are declared in `AndroidManifest.xml` with their
`appwidget-provider` XML configs (`res/xml/widget_*.xml`).

---

## Settings

All settings are persisted locally and applied live:

| Category | Options |
|---|---|
| Appearance | Theme: **Light / Dark / System**; **Accent color** (default `#7C4DFF`) |
| Note colors | **Random / Theme-driven / Choose default color** |
| Time display | Created / Edited / Both / Hidden |
| View mode | **Grid / List** (default per app) |
| Editor | **Basic / Advanced** mode, font family (default Roboto), font size (default 16sp), spell check on/off |
| Privacy | Private Safe **passcode**, **biometric unlock**, **auto-lock** period (minutes) |

---

## Security & Privacy

- **Local-only storage** — notes, sketches, attachments, and history never
  leave the device in normal use.
- **Private Safe** — passcode gate plus optional biometric verification
  (AndroidX Biometric); auto-locks after idle timeout.
- **Trash & version history** act as safety nets against accidental loss.
- **Firebase App Check** (reCAPTCHA) is wired in to protect any online
  Firebase usage from abuse.
- The `namespace` is generic (`com.example`) unless changed — consider
  renaming it for a real release: app ID `com.aistudio.lsnotes.app`.

> ⚠️ **Release hygiene:** before shipping, update `applicationId`/`namespace`,
> replace the placeholder launcher assets, and ensure your release keystore
> is stored safely (keystore + passwords are NOT committed to the repo).

---

## FAQ

**Do I need an account?**
No. LS Notes is fully offline — no account, no sync, no servers.

**Is my data backed up?**
Not automatically. Use the built-in JSON backup/restore to snapshot your
notes, or enable Android cloud backup (`allowBackup=true` with
`backup_rules.xml` / `data_extraction_rules.xml` already configured).

**Will the app work without internet?**
Yes — everything except optional Gemini AI features works fully offline.

**Does it need a Gemini API key?**
Only for AI-powered features. The app builds, installs, and runs with no key.

**Can I migrate from Evernote?**
Yes — import `.enex` exports directly using the import flow.

**Why does the database use destructive migrations?**
`fallbackToDestructiveMigration()` is for rapid development; schema changes
drop and recreate tables. Before shipping production data, replace it with
proper migration objects in `LsNotesDatabase`.

**Can I reorder or rename notebooks?**
Notebooks can be created, renamed, and managed from the
*Manage Notebooks* screen, with configurable delete behavior.

---

## Roadmap

Ideas already scaffolded in the codebase (commented-out dependencies):

- [ ] **Camera integration** — CameraX photo capture for PHOTO notes.
- [ ] **Firestore sync** — cloud sync/backup (`firebase-firestore`).
- [ ] **Auth** — Firebase Auth + Google Sign-In via Credential Manager
      (4 dependencies already mapped in the version catalog).
- [ ] **Location capture** — attach current location to notes
      (`play-services-location`).
- [ ] **Gemini AI** — summarize, tag, and generate note content with
      `firebase-ai` once a key is configured.
- [ ] Real database migrations, release minification, and app icon polish.

---

## Contributing

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-thing`).
3. Follow existing conventions (state flows, repository facade, tabs).
4. Add/update tests:
   - JVM logic → `app/src/test`
   - UI/screens → Roborazzi screenshot tests
   - Instrumented → `app/src/androidTest`
5. Open a pull request describing the change.

```bash
# Before opening a PR, verify:
./gradlew testDebugUnitTest connectedAndroidTest lint
```

---

## License

This project is provided for evaluation/development purposes. Ensure you own
or have rights to all assets before distributing. *(Add your chosen license
here, e.g. MIT, before public release.)*