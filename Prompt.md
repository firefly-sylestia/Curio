# Prompt.md — current request log

## Request: Category picker UX refinement (auto-select bug, page-1 modes, tinted selection, live state fixes)

User direction (verbatim, lightly cleaned): "we will be refining and fixing things, starting with the category picker, it has a really bad user experience — when selecting things to mix it sometimes auto-selects 2; remove the presets of science etc from page 1 and show the Curio, Knowledge and Mix options; use the classic category picker's category-tint style when selecting things in the new picker; also it was less dark/creamy in light mode; on page 2 (the new picker) show 6 your-mixes instead of 5 then 'show all' if more; in Continue exploring below that, remove the 'hold to remove' text, add a tap-and-hold action; fix the update when going to add / selecting-or-unselecting — it doesn't update, tapping back also doesn't update, only closing the picker and reopening updates it; same when creating a mix — fix these."

Clarifications asked (ask_user):
1. "Auto-select 2" — user's answer: "opening page 1 and when holding to start a mix it selects 2, same can be in other too so analyse" → the hold-to-start-a-mix path was double-selecting.
2. Light-mode creaming — user picked "Tiles + panels" (not the sheet background).

### Analysis — root causes found (all in the new picker, `features/picker/`)

- **"Holding to start a mix selects 2"** — `ClassicPickerPage` (new picker page 1) seeded `selectedSlugs` from the persisted deck (`getLastSpinCategories`) and `multiSelectMode` from `persistedVisible.size > 1` (the old v26 model the user had already rejected in v196 for the classic sheet). With a single-lane deck, the lane was already IN the set but not rendered (multiSelect off); the first long-press switched multiSelect on → the persisted lane + the held lane both lit up. Same bug existed in the classic `CategoryPickerContent` (draft ?: persistedVisible seeding).
- **Mix editor "doesn't update until reopen" + batch "auto-select 2"** — `MixEditorSheet` toggled a remembered `MutableSet` IN PLACE and wrote the SAME instance back (`selected = selected.apply { if (!add(slug)) remove(slug) }`). `mutableStateOf`'s structural equality sees the same instance → NO recomposition → ticks and the Save-label count never updated live; when an unrelated recomposition finally ran, several taps appeared at ONCE (the "it selects 2 sometimes" perception).
- **Add/Remove in Continue exploring "only updates on reopen"** — `AddSuggestionSheet` and `ContinueExploringSection` snapshotted `pickerSuggestionsState` with `remember`, so writes never reflected until the sheet was disposed and recreated. PLUS unchecking a DEFAULT suggestion was a no-op (`removePickerSuggestion` wrote against an empty user list, so defaults kept showing).
- **Preset chips vs modes** — page 1 showed Science/Entertainment/Arts & Stories/History & Ideas preset chips; the user wants the classic picker's Curio / Knowledge / Mix mode tabs instead.

### What shipped (this turn)

- **Page 1 rebuilt as a Curio / Knowledge / Mix mode picker** (`ClassicPickerPage`): mode tabs (classic tint style: `washCat.themedAccent()` selected) with grouped tap-to-open Curio/Knowledge decks and the Mix multi-select grid + Mix/Cancel row. Preset chips removed from the new picker ONLY (`CategoryPickerContent` classic keeps them). Reused `PickerMode` + `curioModeGroups`/`knowledgeModeGroups`/`PickerGroup` (made internal in `CategoryPickerScreen.kt`).
- **Clean start** — page 1 opens `multiSelectMode = false` + empty selection (v196 model: tap opens a lane, hold is the ONLY way into multi-select → holding selects exactly one). Applied to the classic `CategoryPickerContent` too (mid-session `CategoryPickerDraft` restore kept).
- **Category-tint selection** — `NewPickerTile` selected state = `themedAccent()` fill + `onAccent()` ink (icon, label, check badge, accent-ink ring, icon-plate tint) — the classic `PickerIconTile` style — everywhere selection renders.
- **Cream light mode** — new `newPickerIdleFill()` (classic cream-pill recipe `lerp(base, curioPillLift(), 0.82f)` in light; unchanged in dark) applied to every new-picker tile/pill/panel (sheet + Browse page).
- **Your mixes: 6 visible** then "Show all" (was 5); threshold + "Show less" updated.
- **Continue exploring** — "hold to remove" hint text removed; holding a lane opens a Remove pill (`CategoryOptionPill` gained optional `onRemove`); section reads `pickerSuggestionsState` reactively (live).
- **Live-state fixes** — `MixEditorSheet` uses an immutable `Set` (recomposes every tap); `AddSuggestionSheet` reads reactive state + toggles the EFFECTIVE list so unchecking defaults works; `AppPreferences.removePickerSuggestion` seeds from the effective list (defaults) before removing.
- **Fixes the mix grid at 6**: `mixes.take(6)` / `mixes.size > 6`.

Verification: compile/build/lint forbidden in this env (CI validates on push). Braces/parens/brackets balanced via script for the 3 picker files; import set verified for `Color`/`lerp`/`curioPillLift`/`isCurioDarkTheme`/`themedAccent`/`onAccent`/`GridItemSpan`; nested-lazy-grid crash rule respected (page-1 grids are top-level in the pager page; continue-exploring stays manual chunked rows).

### Progress
- [x] Ask clarifying questions (auto-select location + light creaming scope).
- [x] Page 1: mode tabs + clean start (removed preset chips + persisted-deck seeding).
- [x] Category-tint selected tiles + cream-lift idle fills (sheet + Browse).
- [x] Mixes 6 + Show all; Continue-exploring remove pill + live reads; Add sheet + removePickerSuggestion no-op fixes; MixEditorSheet immutable set.
- [x] Classic `CategoryPickerContent` clean start (same auto-select-2 root cause).
- [x] Changelog (20260921.txt) + DOX pass (app/AGENTS.md v3xx7).
- [x] Commit & push.