# Prompt.md — current request log

## Request (ACTIVE): Category picker overhaul — no category accents, tap-hold option pill, mixes grid, etc.

User: (paraphrased — see the full original in the session)

A large set of changes to the NEW category picker
(`NewCategoryPicker.kt` + `NewCategoryPickerBrowse.kt`):

1. Category buttons look bad with category accents + creamy design →
   STOP using category accent colors on tiles (border, icon, fills).
   Use neutral theme roles instead, in both dark and light mode.
2. Tap-and-hold a category → show an option pill (Pin / Unpin) instead of
   directly pinning. Same in the Pinned tab and the Pinned row in the sheet.
3. Browse page in-page bottom nav (Browse/Mixes/Pins) capsules are fully
   transparent in liquid-glass mode → fill with SOLID color while keeping
   the edge style.
4. In Mixes, the 3-dot (More) currently opens Edit only → offer Delete OR
   Edit (two options).
5. In the mix editor (create/edit), tapping a category gives NO selection
   indication (bad UX) → show a clear selected state on the tile.
6. Don't use category accent for the border OR the icon (both themes).
7. Tapping back from the Browse page → return to the category picker SHEET
   (not exit entirely). (Currently `onBrowse` closes the sheet then pushes
   PICKER; back pops PICKER → Spin with sheet closed.)
8. Pinned category chips → make them taller/wider pills.
9. "Your mixes": show only 5 with an Expand button; use a 2-col GRID
   instead of rows; below the mixes show "most used categories" or good
   "fun to explore" categories (up to 10) — user can customize.
10. The OLD category picker → show it inside the SAME bottom sheet by
    swiping (a pager between new + classic).
11. Remove the "Now spinning" area; use that space for "Continue
    exploring" with recently explored categories.
12. In the bottom sheet, don't use a cross (close) button.
13. Fix button colors in the category page; in liquid-glass mode the
    mixes/pins buttons are transparent → fix.
14. Properly add tap-and-hold actions; properly fix create mix.

### Clarifications needed (asked the user)
- Item 7 (back from Browse → sheet): the sheet is owned by SpinScreen.
  Browse is a pushed route (PICKER). Returning to the sheet means
  re-opening SpinScreen's sheet on back. Confirm.
- Item 9 ("most used" / "fun to explore"): source = recently spun
  categories (a new pref log) vs. a curated static "fun" list vs. user
  picks? And "user can customize" — reorder/hide those suggestions, or
  pick which categories appear?
- Item 10 (swipe to classic picker): a HorizontalPager inside the sheet
  (swipe left = classic multi-select picker). Confirm scope — just the
  classic `CategoryPickerContent`, or a lighter variant?

### Files in scope
- `app/src/main/java/com/curio/app/features/picker/NewCategoryPicker.kt`
- `app/src/main/java/com/curio/app/features/picker/NewCategoryPickerBrowse.kt`
- (calls) `app/src/main/java/com/curio/app/features/spin/SpinScreen.kt`
- (theme helpers) `app/src/main/java/com/curio/app/ui/theme/CategoryInk.kt`
- (prefs) `app/src/main/java/com/curio/app/data/AppPreferences.kt`

### Decisions (from the ask)
- Back from Browse → re-open Spin's picker sheet (via SpinPickerRequest).
- Suggestions: hybrid (recently-spun via CurioPassport first, then curated
  up to 10).
- Customize: add/remove lanes (tap-hold to remove, "+ Add" tile → sheet).
- Classic picker: HorizontalPager in the SAME sheet; user picks default
  page (classic = page 1 by default); both pages share the bottom action
  row (Surprise me · Create mix · Browse).

### What shipped (this turn)
- **AppPreferences**: `pickerDefaultPageState` (Int, default 0=classic) +
  `pickerSuggestionsState` (List<CategoryId>) + accessors
  (get/set/add/removePickerSuggestion) + `defaultSuggestions` (10 lanes).
- **NewCategoryPicker.kt** (rewritten): NEUTRAL `NewPickerTile`
  (surfaceContainerHigh fill, onSurfaceVariant icon, secondaryContainer
  selected + primary check — NO category accent anywhere); tap-and-hold
  option pill (Pin/Unpin · Spin) via `CategoryOptionPill`; NO close
  button; `HorizontalPager` (page 0 = `ClassicPickerPage` self-contained
  multi-select grid, page 1 = `NewPickerPage`); `NewPickerPage` has
  Pinned (taller `NewPinnedPill`), Your mixes (2-col grid, max 5 +
  Show all/less via `NewSecondaryOutline`), Continue exploring
  (`ContinueExploringSection` — CurioPassport spin counts + curated,
  add/remove via `AddSuggestionSheet`/`AddSuggestionTile`); shared bottom
  action row (Surprise me · Create · Browse) with neutral
  `NewPrimaryCapsule`/`NewPickerCircle`; `MixEditorSheet` shows clear
  neutral selection. `NewMixRow`/`NewPickerChip` removed.
- **NewCategoryPickerBrowse.kt** (rewritten): SOLID-fill
  `NewPickerTabCapsule` (surfaceContainerHigh/secondaryContainer +
  curioGlassEdge, never transparent); `BrowseMixRow` with 3-dot
  DropdownMenu (Edit + Delete); `BrowseOptionPill` for tap-hold on
  Browse/Pins; back button sets `SpinPickerRequest.pending = true` so
  back re-opens the Spin sheet; NEUTRAL pins rows.
- DOX: app/AGENTS.md v3xx2 bullet + fastlane changelog updated.

### Progress
- [x] Ask clarifying questions.
- [x] Implement (picker overhaul).
- [x] DOX pass (AGENTS.md + changelog).
- [ ] Commit & push.

## Request (ACTIVE → DONE): theme reveal in liquid-glass + Material toggle + Editorial wrap

User: "in liquid glass mode the theme switch transition animation
doesnt work and also when turning on the material theme the animation
doesnt work. so the share card for editorial the text wrapping is bad
and its overlapping with each other so fix it."

### What shipped (this turn)
- **ThemeTransition.kt**: the reveal now captures the old frame via a
  Compose `GraphicsLayer` (hardware `record` + `toImageBitmap`) instead
  of `View.drawToBitmap` (which forces a software pass that drops
  RenderEffect/liquid-glass blurs and returned a blank frame → the
  reveal silently skipped). `CurioThemeTransitionState` keeps the view
  fallback for hosts without a layer. New `switchVisualThemeWithReveal`
  forces the reveal for non-mode visual changes (Material on/off,
  hero tears).
- **SettingsSectionScreen.kt**: the Material theme + Material hero
  tears toggles now wrap in a positioned Box and call
  `switchVisualThemeWithReveal` so toggling them plays the circular
  reveal from the switch (was an instant apply).
- **TopicShareCard.kt (EditorialCard)**: drop-cap layout rebuilt so
  the big initial lives in a fixed-width column (`initialW + gap`,
  top-aligned, lineHeight = 3× body line) beside a 3-line wrap column
  at the measured narrow width, then the rest full-width below. Fixes
  the overlap (was `alignByBaseline` with mismatched heights → the
  full-width rest text collided with the wrapped block).
- Changelog updated.

### Notes / risk
- Could NOT compile (env forbids Gradle) — CI validates. Watch:
  `GraphicsLayer.record` every frame has a perf cost (same pattern as
  the legacy blur capture, accepted). If it regresses scroll perf, gate
  recording to only when `state.isAnimating` is pending — but then the
  pre-flip frame must be recorded on-demand at `startTransition`.
- The capture Box wraps the NavHost; glass pills inside sample a
  different `LayerBackdrop` (navGlassBackdrop), so no cyclic record.

### Notes / risk
- Could NOT compile (env forbids Gradle) — CI will validate. Watch for:
  the `ClassicPickerPage` grid fills via `weight(1f)` inside a Column in
  a pager page (bounded by the pager's `weight(1f, fill=false)` in the
  sheet Column) — should render; if the grid collapses, the pager page
  height needs an explicit fillMaxHeight.
- The classic full-screen `CategoryPickerScreen`/`CategoryPickerContent`
  is untouched (still used by the classic-toggle path + the Spin classic
  branch).
