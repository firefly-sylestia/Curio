# Prompt.md — Request log

## Current request — COMPLETED: Updates page, backup fixes, toast, mood board, new icon + 1.1.0

All four requests from this session are done, committed and pushed
(`886de9d` mood board, `98206cf` icon + version, and `eb60c5b` for the
Updates/backup/toast batch).

### 1. Updates sub-page + update-checker toggle + toast pill
- New `features/updates/UpdatesScreen.kt` (route `UPDATES`, Settings hub
  row, Support "Open Updates" link): version readout, Check for updates
  with release-notes card, APK download → system installer, and the
  **opt-in Update checker toggle** (`updateCheckerEnabledState`, default
  OFF — the data-costing background check only runs when enabled; the
  manual check always works).
- `MainActivity` gates `UpdateChecker.notifyIfUpdateAvailable` on the
  toggle.
- **Old update card REMOVED from Support & diagnostics** (user confirmed)
  — Support keeps the version five-tap → Experiments diagnostic and links
  to the Updates page; notification/toast copy now point to the Updates
  page.
- **Toast remade**: `CurioInAppToastHost` is now a small pill at the
  TOP-RIGHT corner (TopEnd + statusBarsPadding in the NavHost root,
  compact 12/7 padding, 16dp glyph, slides down); tap opens the Updates
  page. `UPDATES` added to `popScreenRoutePrefixes` for the center-pop.

### 2. Last-backup fix + auto backup
- `CurioBackupManager.export` returns `ExportResult.exportedAtMillis`;
  BackupToolsScreen drives the "Last backup" row from it + re-reads on
  ON_RESUME (no more "Never" after a backup).
- **Auto backup** (BackupToolsScreen "Auto backup" section): opt-in toggle
  — first flip asks for a save location once (CreateDocument +
  takePersistableUriPermission, persisted URI); MainActivity exports there
  on start, throttled to ~once per 24h; the section shows Backup location
  (tappable to change) + Last auto backup.

### 3. Mood board inline editor (v113)
- **Tiles reach the top**: drag/pinch/grow/commit clamps were the frozen
  collage extent, but the centered fit left an untouchable band above the
  collage. Clamps are now the FULL visible card (display → raw: negative
  raws allowed); the fit stays frozen so the zoom never jumps mid-drag;
  saved views (MoodBoardTiles, EntryDetail inline fit + fitTileLayout)
  allow the same negatives so band-placed photos render in the same spot
  in edit and detail.
- **Quote cards**: drag/resize clamps bound the visible canvas (were
  canvas × scale — draggable off the card / glitching at the top);
  never-dragged slot computed in RAW board space (was double-scaled off
  the collage) via new `boardMaxX/Y` params passed by all four callers
  (inline editor, saved card, expanded dialog, export).

### 4. New cosmic icon + 1.1.0 (v113)
- The user-supplied `svgviewer-output (3).svg` (mint planet + pink moon +
  gold waves on a midnight sky, rounded white-framed card) replaces the
  old angular open-portal mark: `ic_launcher_background` (full-bleed sky +
  stars), `ic_launcher_foreground` (whole scene — also the splash art),
  `ic_launcher_monochrome` (planet+moon silhouette), `ic_notification`
  (24dp mark). Source archived at `design/launcher-icon/curio-launcher-icon.svg`.
- **Version bump (missed previously):** `versionName` 1.0.1 → **1.1.0**,
  `versionCode` 20260919 → **20260920**; store changelog moved to
  `fastlane/.../changelogs/20260920.txt` (20260919 draft deleted).

### Validation
XML validated with ElementTree; `git diff --check` clean; no Gradle locally
(env rule) — CI validates on push. All three commits pushed to `main`.

## Follow-ups / notes
- The update checker toggle ships OFF by default (opt-in) — per the
  experiment rule, once the feature is settled the toggle should be
  removed and the winning behavior hardcoded.
- Auto-backup reuses ONE persisted document URI (overwrites the same file
  each run); "pick a folder" semantics were implemented as "pick a
  document once" (CreateDocument) — revisit if a true folder flow is
  wanted.
