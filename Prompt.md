# Current Request — Topic resolution fixes ("Flow" → "Flower Boy") + Browse-Topics persistence

## Status: DONE (committed + pushed to Alpha)

## Request (user, verbatim)
"when i tap flow in topic browser movie 2024 in fimls why its opening flower boy, fix more similiar issues like this and also make the category selected and expaddned setting persistent untill restart"

## What changed (v199)

### 1. "Flow" → "Flower Boy" resolution bug (root cause)
`TopicCatalog.findByName` scanned `CategoryId.values()` in order and returned the FIRST lane's first tolerant match. ALBUMS scans before FILMS, and "Flower Boy" contains "flow" (4-char containment tier) — so the Films reveal for "Flow" (films.json has "Flow (2024)") opened the ALBUMS topic. Even the full name "Flow (2024)" base-collided the same way.
- `TopicCatalog.findByName` → TWO passes: strict (exact name / base-name, new `matchesSavedNameStrict` + shared `savedNameBase` helper) across ALL categories first, then the tolerant pass (containment last). An exact/base hit in any lane always beats a loose containment hit in an earlier lane.
- `TopicRevealScreen` + `SaveCaptureScreen` → resolve within the route's own category pool FIRST (`pool.firstOrNull { it.matchesSavedName(name) }`), falling back to the global `findByName` only for legacy saved entries whose lane changed (v135).

### 2. Browse-Topics persistence until restart
The browser route is a plain `composable` — every reopen from the drawer creates a fresh backstack entry, so rememberSaveable reset the category selection + chip bar. New `TopicBrowserSession` (process-scoped static, same pattern as `SpinPickerRequest`) seeds `selectedCat` / `categoryFilterOpen` and syncs back on change — survives close-and-reopen until the app restarts (statics die with the process).

## Files
- `data/TopicCatalog.kt` — two-pass findByName, matchesSavedNameStrict, savedNameBase.
- `features/reveal/TopicRevealScreen.kt` — category-first resolution.
- `features/capture/SaveCaptureScreen.kt` — category-first resolution (+ import).
- `features/database/TopicDatabaseScreen.kt` — TopicBrowserSession + seed/sync.

## Docs
- `app/AGENTS.md` — v199 entry.
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — 2 FIX bullets.
- This Prompt.md.

## Verification
- Static only (no Gradle here — CI validates on push). Trace check: reveal/films/Flow → Films pool matchesSavedName("Flow") → "Flow (2024)" base-name match ✓; findByName("Flow (2024)") strict pass → Films "Flow (2024)" exact ✓ (albums "Flower Boy" only matches in the tolerant pass, never reached first).
