# Prompt.md — Research & Analysis Tracking

## Current Request (COMPLETE): 5-part cleanup — paper title, hidden Experiments, drag reorder, Manage categories link, mixed-selection persistence

**Date:** 2026-08-12

**What was asked:**
1. "In voice note title the saved the title doesnt get the paper style" — the saved voice-note title should render on the note-paper slip like the editor shows.
2. Experiments is still visible in Settings — it should be hidden; keep Manage categories and Topic history "inside appearance".
3. Manage Categories: add drag-to-change-position + a reset option.
4. In the Spin "What are we exploring?" sheet, change "Browse all categories" → "Manage categories".
5. Selecting a mix and reopening doesn't show the mixed selection (only the first fixed category) — and you should be able to change the mix from that view without re-selecting.

**Changes (5 files + docs):**
- **EntryDetailScreen** (`SoundBiteRender`): saved title now renders inside its own `NotePaperCard` slip using the saved `titleStyle`/`titleColor` (fallbacks `paperStyle`/`CREAM`) and a stable `noteSeed(entry.id, 30)`. Hoisted OUTSIDE the `audioFilePath` gate so typed-only voice-note saves show the title too.
- **SettingsHubScreen**: "Card & deck experiments" row removed (Experiments is now reachable ONLY via the five-tap version trick in Support & diagnostics); Manage categories + Topic history moved into the Personalize card; the now-empty Explore section deleted.
- **ManageCategoriesScreen**: real long-press drag-to-reorder on the ⋮ handle — `detectDragGesturesAfterLongPress`, draft `List` state (`remember(items)`), `dragAccum` row-step (76dp) swap loops, `Modifier.animateItem()` (verified present in the resolved foundation 1.12.0-alpha03 dex), lifted-row visuals (zIndex + graphicsLayer scale), order persisted on release. Plus a "Reset order" TextButton next to the helper text restoring `CurioCategories.all` order (hidden flags untouched). Old `moveCategory` helper deleted; steppers still work (shift draft + persist immediately).
- **SpinScreen**: the sheet's "Browse all categories" link is now "Manage categories" (DragHandle icon) and navigates `MANAGE_CATEGORIES`. `CategoryPickerSheet` seeds `multiSelectMode` + `selectedSlugs` from persisted `getLastSpinCategories` (filtered to visible) — a saved mix reopens in multi-select, pre-ticked, so it can be reviewed/changed.
- **CategoryPickerScreen** (full-screen picker, still reachable from Home): same seeding via `rememberSaveable` initializers.

**Validation:** braces (5 files) + `git diff --check` clean; no leftover "Browse all categories"/`moveCategory`/`CurioRoutes.PICKER` refs in edited files; imports confirmed in scope (LocalContext, NotePaperCard/notePaperInk/noteSeed, all new drag imports); code review passed — follow-up applied: title slip moved out of the audio gate.

**Product interpretations made (flag if wrong):** "inside appearance" = the Personalize section (next to the Appearance row), not the Appearance page itself; "reset" resets order only, not hidden lanes.

**Next:** none pending.
