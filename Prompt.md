# Prompt.md — Request log

## Current request — COMPLETED: offline model picker storage usage + big-download confirm (v139)

Follow-up to the download-manager work: the user picked the "Storage info" suggestion —
show installed-model storage usage in the picker and warn about storage requirements
before big downloads.

### Changes
- **`VoskModels` (OfflineTranscriber.kt)** — three new helpers:
  - `modelSizeBytes(context, id)` — real on-disk usage of a downloaded model (walks the
    model dir; 0 when not installed).
  - `availableStorageBytes(context)` — free bytes via `StatFs(filesDir)` (runCatching,
    0 on failure).
  - `formatModelSize(bytes)` — B / KB / MB / **GB** formatter. The existing
    `formatFileSize` (EntryDetailScreen) caps at MB, but the Full tiers are 1–2.3 GB.
- **Picker rows (SettingsSharedComponents `OfflineModelDialog`)**:
  - The trailing "In use"/"Downloaded" label now shows real usage: "In use · 41.2 MB".
  - Download button: a model ≥ `BIG_MODEL_BYTES` (100 MB — the Large/Full tiers) OR one
    larger than the free space opens a confirm AlertDialog first (required vs free size,
    red when it won't fit — "Only X free — the download will likely fail. Free up space
    first."); confirming calls `VoskModelDownloads.start`. Small models with room still
    start instantly. `pendingBigDownload` state re-added (mutableStateOf/setValue imports
    restored after v138 removed them).
- No change to the manager itself — pause/resume/cancel/multi and background-survival
  from v138 stand.

### Verification
No Gradle build in this environment (project rule — CI validates on push). On-device:
open Settings → Recording → Offline model; a downloaded row should show real usage; tap
Download on a Large/Full model → confirm dialog with sizes (red when low); small models
still start immediately.
