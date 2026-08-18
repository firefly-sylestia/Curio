# Current Request — Category picker: tap-to-open, cancel+back applies, no mix resurrection + CI fix for the constellation

## Status: DONE (committed + pushed to Alpha)

## Request (user, verbatim)
"even when i cancel the selected in category picker and i tap back make it apply too. and also when its mixed and after that i open the category picker to slecet dont let me tap to select for mix let it be open the category when i tap and only tap and hold should select for next mic or override mix, also theres a bug suppos i have a mixed selected and its from the home shuffle button and then i cancel it and chnage it to other category and i opened the topic and then when i tap back it goes back to the mixed one even though i have chnaged it. so fix that."

Then: CI failed on the v195 constellation commit — `@Composable invocations can only happen from the context of a @Composable function` at CurioConstellation.kt:185:35 — "fix this too".

## What changed (v196)

### 1. Tap-to-open always (`CategoryPickerSheet` in SpinScreen.kt)
- The v26 auto-tick reopened the sheet in multi-select with every mix lane pre-ticked whenever the persisted deck was a mix. Now `multiSelectMode` starts false and `selectedSlugs` empty on every open; tap OPENS a category (replacing the deck), and long-press (both Original/New pages) is the ONLY way into multi-select, starting a fresh selection for the next / overriding mix.

### 2. Cancel + back applies the cleared mix
- Cancel button sets a `mixCancelled` flag; `onDismissRequest` applies a cleared state (`onCategoriesSelected(emptyList())`) when cancelled OR when every lane was deselected in multi-select — SpinScreen reverts to the last single category and persists it. Fresh selections (presets, long-press) reset the flag. A pop in single-select mode (never entered multi-select) still just closes.

### 3. No mix resurrection (the Home-shuffle back bug)
- Root cause: the v5.14 slug-authority `LaunchedEffect(categorySlug)` and the v5.5 persist effect re-ran on every pop-back from a pushed route (the topic reveal) and re-forced the launch slug over the user's in-session category change. A new `slugApplied` rememberSaveable flag gates both: the slug (and its prefs persist) apply ONCE per navigation; returning from the reveal restores the flag true so the deck keeps the user's change.

### 4. CI fix — constellation filler color (`ui/components/CurioConstellation.kt`)
- v195 called `isCurioDarkTheme()` (a @Composable) inside the Canvas draw lambda for the new decorative filler dots. Moved `fillerColor` resolution to composition next to `linkColor`/`fissureColor` (which already follow that pattern).

## Docs
- `app/AGENTS.md` — v196 entry (picker fixes).
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — 4 FIX bullets.
- This Prompt.md.

## Verification
- CI reported only the one constellation error (fixed); the picker changes are static-safe (no new @Composable misuse: the sheet edits are plain remember/mutableState + callbacks). Committed and pushed; CI will validate.
