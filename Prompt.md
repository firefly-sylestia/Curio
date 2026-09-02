# Prompt.md — current request log

## Request: Cabinet chips parity, picker refinement batch (washed-out mix accents, page-1 Mix/Surprise + floating Cancel, Curio/Knowledge persistence, 2-col filter panels)

User request (verbatim, condensed):

> In Cabinet, when selecting a category it doesn't show the same selected
> category chip as Topic Browser (clarified: the Cabinet's selected-category
> chip doesn't show as chips like the browser's, in any condition). Fix the
> new category picker — your mixes' category accent colors look washed out,
> so fix it. When something is selected, use the category ACTIVE accent in
> the Continue-exploring ones too, and in the page-1 picker activate it
> when tapping and holding something for a mix. The surprise mix should
> change to Mix when selecting multiple in the page-1 category picker
> (clarified: the extra button above Surprise me — the Mix row — and the
> Mix label should show in the Surprise-mix position when multi-selecting).
> The cancel pill should float. Also make Curio and Knowledge persistent
> too (clarified: persist BOTH the mode tab AND per-tab scroll positions).
> In "Your mixes" there's enough space to show the category icons instead
> of "+N" — do that, and fix its height. The category picker in both the
> Cabinet and Topic Browser should be a 2-grid list instead of one list.

### What shipped (this turn)

**A. Cabinet chip parity (`CabinetScreen.kt`):** the active-filter chip
row is now pixel-identical to Topic Browser's `ActiveFilterChips` — same
`labelLarge` typography (dropped the Cabinet-only Bold) and the same
modifier chain order (padding → offset → horizontalScroll), so the
removable pills read as the same family in every state.

**B. Picker mixes (`NewCategoryPicker.kt`):** `NewMixCard` drops the
washed-out 16%-alpha lead plate (now a `lerp`(surface, accent, 0.42)
blend) and the 16dp/16%-alpha dots for real **lane icon chips** (20dp
accent tiles, up to 5 + "+N" for huge mixes); the cell grows 114 → 122dp.
Continue-exploring tiles wear the **category active accent** when their
lane is in the current deck (deckIds threaded into
`ContinueExploringSection`).

**C. Page-1 (classic) picker progressive-mix flow:** while multi-selecting
the Wildcard tile relabels "Surprise mix" → "Mix" (it now toggles into the
pending mix), the SHARED bottom capsule swaps "Surprise me" → "Mix · N"
and applies the pending selection (page reports count + apply closure via
`onMixStatus`), and the Mix row's flat Cancel TextButton is now a
**floating raised pill** (Close glyph + label).

**D. Persistence — "make Curio and Knowledge persistent too":** page 0's
mode tab (Curio/Knowledge/Mix) survives sheet close + app restart
(`KEY_PICKER_PAGE0_MODE` + `pickerPage0ModeState`, seeded in
`initThemeMode`); each tab owns its own `rememberLazyGridState` persisted
debounced per-tab (`KEY_PICKER_PAGE0_TAB_SCROLL_<tab>`), replacing the
single page-0 scroll key (old accessors deleted).

**E. Two-column filter panels:** both `DatabaseCategoryPanel`
(`TopicDatabaseScreen.kt`) and `CabinetCategoryPanel`
(`CabinetScreen.kt`) render their checkbox lists in a
`LazyVerticalGrid(GridCells.Fixed(2))` at the same 260/276dp max-height
footprint; the Cabinet keeps Legacy as a full-span `GridItemSpan` row.

Files: `NewCategoryPicker.kt`, `AppPreferences.kt`, `CabinetScreen.kt`,
`TopicDatabaseScreen.kt` + `app/AGENTS.md` (v318 bullet) + changelog.

CI compiles on push; hand-audited delimiters, imports and API usage across
the four files (brace-balanced, new prefs keys/accessors mirrored the
existing PickerScrollPos pattern).