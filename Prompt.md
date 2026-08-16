# Prompt.md — Request log

## Current request — Updates sub-page + update-checker toggle, last-backup fix, auto-backup, top-right toast pill

### What was asked
1. (Earlier) In-app updates without Google Play: delta-patch "small bug fixes
   updated directly". **RESOLVED: delta patching is dropped for now** ("its
   too much work isnt it, lets leave that for now") — the existing
   full-APK GitHub-release updater stays.
2. Do the rest of the UI change: a **new Updates sub-page with its own UI**
   (dedicated screen, not the Support & diagnostics card) plus a **toggle to
   turn off the update checker** (opt-in — the app is offline, checking
   takes data).
3. **Fix "last backup not showing"** (user reports it reads "Never" after a
   successful backup) and add an **auto-backup option** (decision: "Pick a
   folder once" — choose a save location once, auto-backup writes there
   periodically without asking again).
4. **Remake the in-app update toast** — remove the current bottom pill and
   make it a **small pill at the TOP-RIGHT corner**.

### Design decisions (from ask_user)
- Delta patches: NOT building (user called it off).
- Auto backup: "Pick a folder once" — persist the chosen CreateDocument URI
  (with takePersistableUriPermission), then export to it automatically
  (throttled, e.g. once per day) without re-asking.
- "Last backup not showing": user says it reads "Never" after a successful
  backup — the export DOES write `curio_backup_meta.last_backup_at`, but the
  UI read is a one-shot `remember { mutableStateOf(lastBackupAtMillis(...)) }`
  and is only updated after the manual launcher path. Fix: return the
  timestamp in ExportResult, always refresh the row from prefs on screen
  entry/resume, and surface auto-backups too.
- Toast: TOP-RIGHT small pill (decision: "Top-right").

### What exists today
- `data/UpdateChecker.kt` — GitHub releases check (plain HttpURLConnection),
  `notifyIfUpdateAvailable()` on app start (toast + once-per-version
  notification), `downloadApk()` with progress, `isNewer()` tag compare.
- `features/support/SupportScreen.kt` — the "Updates" card (version row +
  five-tap Experiments trick, Check for updates row, `UpdateResultCard` with
  release notes + download progress + system installer handoff).
- `ui/components/CurioInAppToast.kt` — `CurioToast` bus + `CurioInAppToastHost`
  (bottom pill, ~3.5s, tappable "Open" action → Support).
- `navigation/CurioNavHost.kt` — toast host aligned `BottomCenter` + 96dp;
  "support" action → `CurioRoutes.SUPPORT`.
- `data/CurioBackupManager.kt` — `export()` writes `curio_backup_meta`
  (`last_backup_at` / `last_backup_count`) AFTER the streaming write;
  `lastBackupAtMillis()` reads it back.
- `features/settings/BackupToolsScreen.kt` — "Back up now" (CreateDocument),
  "Restore from backup", "Last backup" info row (one-shot read),
  FieldMind legacy import.
- `data/AppPreferences.kt` — `KEY_LAST_NOTIFIED_UPDATE` exists; toggles follow
  the `xxxState` reactive + `isXxx`/`setXxx` pattern, seeded in
  `initThemeMode(context)`.

### Plan
1. `AppPreferences`: `KEY_UPDATE_CHECKER_ENABLED` (opt-in, default OFF —
   offline app, checking costs data) + `updateCheckerEnabledState`/
   `isUpdateCheckerEnabled`/`setUpdateCheckerEnabled`; auto-backup prefs:
   `KEY_AUTO_BACKUP_ENABLED`, `KEY_AUTO_BACKUP_URI`, `KEY_AUTO_BACKUP_LAST_AT`
   (+ reactive state where useful).
2. `MainActivity`: gate `UpdateChecker.notifyIfUpdateAvailable` on the
   toggle; on start (if auto-backup enabled + URI set + not run in the last
   ~24h) run `CurioBackupManager.export` to the saved URI in the background.
3. New `features/updates/UpdatesScreen.kt`: settings-family screen
   (SettingsHeroHeader + heroPageBackground + watermark backdrop) with
   version readout, check-for-updates row, result card (release notes,
   download progress, install handoff), and the update-checker toggle.
   Route `CurioRoutes.UPDATES` + NavHost registration + Settings hub row
   ("Updates" under Safety & support) + deep-search index entry.
4. `CurioBackupManager.export`: return the timestamp in `ExportResult`
   (`exportedAtMillis`) so the UI can display it without a stale re-read.
5. `BackupToolsScreen`: refresh `lastBackupAt` on entry + after export
   (use the returned timestamp); add the auto-backup toggle row (enabled +
   "Choose file" / "Auto-backup file" row showing the saved URI) with
   CreateDocument + takePersistableUriPermission; auto-backup status shown
   through the same "Last backup" row.
6. Toast remake: `CurioInAppToastHost` → small pill at TOP-RIGHT, status-bar
   inset, compact padding; NavHost alignment `TopEnd` +
   `statusBarsPadding` + top/end margins; action still opens Support (or the
   new Updates page).
7. DOX: app/AGENTS.md bullet, fastlane changelog, commit + push.

### Validation
`git diff --check`; YAML untouched (no workflow edits this round); no Gradle
locally (env rule) — CI on push.
