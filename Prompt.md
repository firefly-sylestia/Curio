# Prompt.md — Request log

## Current request — COMPLETED: offline model downloads (pause/resume/cancel/multi, survive dismissal), delete hardening, transcribe gating

The user reported: (1) model downloads have no pause option; (2) tapping cancel /
getting out of the picker UI cancels the download; (3) no multi-download; (4) deleting
one model deletes all; (5) the detail page shows the Transcribe option even when a saved
take has no voice note. They also pasted a CI compile error from the v135 drawer commit
(AnimatedVisibility inside LazyColumn items) and asked to fix it too.

### CI fix (pushed first, `f9a9029`)
`AnimatedVisibility` is a `ColumnScope` extension; the v135 drawer called it directly
inside `LazyColumn` `item {}` blocks (`LazyItemScope` has no ColumnScope receiver) →
"cannot be called in this context with an implicit receiver" on compileDebug/Release.
Fixed by wrapping each collapsible group (`curiosityGroup`, `aboutGroup`) in a plain
`Column` (HomeScreen.kt). LESSON recorded in AGENTS.md v137.

### 1–4 — `VoskModelDownloads` manager (OfflineTranscriber.kt, v138)
Root cause of 2: downloads ran in the picker dialog's `rememberCoroutineScope`, so
dismissing the sheet cancelled the coroutine mid-transfer. New app-scoped manager:
- `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` — application lifetime;
  closing the sheet (or leaving the screen) leaves downloads running.
- `StateFlow<Map<modelId, State>>` (status Idle/Downloading/Paused/Failed, progress,
  error); rows observe it via `collectAsState()`.
- **Pause**: `PauseRequested` thrown inside the transfer loop keeps the partial zip;
  the loop awaits a per-model `CompletableDeferred` gate.
- **Resume**: completes the gate; re-opens the connection with `Range: bytes=<received>-`
  (206 → append; if the server ignores Range and returns 200 → restart from scratch).
- **Cancel** (per-row X): cancels the job, disconnects the live connection (map of
  open HttpURLConnections), deletes the partial zip, resets to Idle.
- **Multi-download**: each model has its own Job — several download concurrently.
- `start()` no-ops only when an ACTIVE job exists; `invokeOnCompletion` removes the
  jobs entry only `if (jobs[id] === job)` so a restart after cancel isn't clobbered.

### 4 — delete hardening (`VoskModels.deleteModel`)
Code read showed per-model deletion (dir = `vosk-models/<id>`), but hardened it anyway:
only `deleteRecursively()` when the parent dir is exactly `vosk-models` (can never wipe
the shared root), and also delete the cached zip so a re-download starts clean. Note for
the user: no cross-model mechanism existed in the code — if it reproduced, it was likely
the cancel-on-dismiss + partial state confusion; the rewrite + guard eliminates both.

### 5 — transcribe gating (EntryDetailScreen `SoundBiteRender`)
The Transcribe button + transcript box rendered for ANY Sound Bite, including note-only
takes with no audio file. Both branches now gate on `!data.audioFilePath.isNullOrBlank()`
(`if (transcript == null && hasAudio)` / `else if (transcript != null && hasAudio)`).

### UI
- Picker rows: while downloading — % text + Pause ⇄ Resume (PlayArrow) + Cancel (X);
  on failure — per-row error text + Retry; downloaded — In use/Downloaded + Delete.
- Settings → Recording "Offline model" row subtitle shows "Downloading <model> · N%"
  while a background transfer runs; picker intro copy mentions downloads survive closing.

### Verification
No Gradle build in this environment (project rule — CI validates on push). Suggested
on-device checks: start a Large-model download, close the sheet (progress continues in
the Settings row), pause/resume it, start a second model concurrently, cancel one, delete
a downloaded model (others stay), and open a note-only saved Sound Bite (no Transcribe).
