# Prompt.md — Research & Analysis Tracking

## Current Request (COMPLETE): Recycle bin + double-confirm delete + sort dropdowns

**Date:** 2026-08-12

**What was asked:**
1. Deleting a saved capture should be double-confirmation.
2. Deletes should go to a recycle bin, recoverable from Settings.
3. The Cabinet sort option should be a dropdown with different sorts, and its arrow should be a universal ascending/descending toggle.
4. Same dropdown in "other sorting options" (Topic Browser).

**Changes:**
- **Data layer:** `CaptureEntity.deletedAt: Long?` (nullable); Room v4→v5 with `MIGRATION_4_5` (`ALTER TABLE captures ADD COLUMN deletedAt INTEGER`); all live DAO queries now filter `deletedAt IS NULL`; new `softDeleteById(s)` / `getTrashedFlow` / `getTrashedById` / `restoreById` / `restoreAll` / `purgeById` / `purgeTrashed` / `countTrashed`; repository wrappers; `CurioEntry.deletedAt` defaulted field threaded through `CaptureEntity.toEntry()`. `deleteById(s)` stays a HARD delete (FieldMind import cleanup).
- **Two-step delete:** new `ui/components/CurioTwoStepDialog.kt` — step 1 "Move to Recycle bin?" → step 2 final "Delete …?" (error-styled), step resets on every dismiss. Wired into Cabinet bulk delete and Entry Detail; both now call `softDelete*` and NO LONGER delete media (media is only removed by the Recycle bin's permanent purge).
- **Recycle bin:** new `features/recyclebin/RecycleBinScreen.kt` (settings-family hero + watermark + list; Restore / Delete forever / Empty bin; purge removes audio + images), route `RECYCLE_BIN` (registered in NavHost + pop-screen list), Settings → Safety & support → "Recycle bin" row.
- **Sort dropdowns:** new `ui/components/CurioSortDropdown.kt` — label zone opens the field dropdown, the trailing arrow is its own tap zone toggling ascending/descending universally. Cabinet: `sortNewestFirst` → `cabinetSortField` (DATE/TITLE/CATEGORY) + `sortAscending`, sort pill replaced. Topic Database: `sortMode` → `tdSortField` (DEFAULT/NAME/YEAR) + `tdSortAscending` mapped onto the existing `DatabaseSortMode`; the old `DatabaseSortChip` row removed.

**Validation:** braces (14 files) + `git diff --check` clean; leftover-ref greps clean. Code review caught a compile blocker — `categorySurface()` (top-level theme extension) was not imported in RecycleBinScreen — fixed, plus the unused `Spacer` import removed and `options.first()` made null-safe.

**Interpretations:** "other sorting options" = the Topic Browser sort row (the app's only other sort); recycle-bin purge keeps a single confirm since the two-step applies to the initial delete.

## CI fix (same day)

SupportScreen.kt failed CI: (1) the download/retry lambdas referenced `info` (a param of `UpdateResultCard`) from the caller's scope — unresolved; fixed to `updateInfo?.let { downloadAndInstall(it) }`. (2) The indeterminate progress lambda returned `null` (`Float?`) but `LinearProgressIndicator(progress = { Float })` needs non-null in this Material3 version — split into the no-progress indeterminate overload when `downloadIndeterminate` is true vs the determinate overload with `progress = { downloadProgress.coerceIn(0f, 1f) }`. Pushed as a dedicated fix commit.

**Next:** none pending.
