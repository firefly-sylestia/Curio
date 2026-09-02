# Prompt.md — current request log

## Request: share-card fact box sliders + Topic Browser auto-scroll/Also-in + mix-name pill + morphing hold pills

User request (verbatim, condensed):

> In commit 6cdb38b0 the cursor is accurate now, but the quick-fact text
> box height and width change isn't working anymore — sometimes it's just
> moving up and down, and it doesn't even change the width anymore in any
> signature — fix it. Add the auto scroll to top in Topic Browser. Tapping
> the "Also in" category chips just selects them / adds them as filters
> instead of switching — fix that too. Your mixes' name doesn't show in the
> Shuffle page's category pill — it says "Mix" even when I set a name — fix
> that. The tap-and-hold options should not be dialogs — just a small pill
> with a beautiful morph animation; and change the 3-dot-style dropdown for
> tap and hold to a circular pill button look, no text needed.

### What shipped (this turn)

**A. Share-card fact sliders (`TopicShareCard.kt`):** the Quick-fact
Height slider felt dead because `lines()` ROUNDED the line count — a
12-line fact needed +8% before anything changed, then the grid re-flowed
("moving up and down"). It now CEILs, so every tick changes the line count
instantly, and the height sliders step chunkier (42 → 26 steps, ~8%/tick)
so each notch visibly grows/shrinks the fact box. Width keeps its
continuous `fillMaxWidth(frac)` per style (Paper's FrostPane narrows via
the v229c `.then(modifier)` chain with fill applied outside).

**B. Topic Browser (`TopicDatabaseScreen.kt`):** the floating back-to-top
arrow now `animateScrollToItem(0)` (smooth auto-scroll, was an instant
jump); flipping a page via the liquid-glass page nav also auto-scrolls to
the top. "Also in" pills now SWITCH: taps run `commitCats { setOf(id) }`
(replacing the selection) instead of stacking the lane into the
multi-select set; SearchSuggestionRow doc updated.

**C. Mix name on the Spin pill (`SpinScreen.kt` + `AppPreferences.kt`):**
applying a NAMED mix stamps `AppPreferences.lastMixNameState`
(`KEY_LAST_MIX_NAME`), set in the picker's `onApplyMix`/editor-`onSave`
paths and cleared on surprise / unnamed multi-select / single-lane picks.
The deck pills (header pill + both BottomCta buttons) read
`deckPillLabel(mixName, mixedCount, cat)` — mix name → "Mixed · N" → lane
name.

**D. Morphing hold pills (`NewCategoryPicker.kt`,
`NewCategoryPickerBrowse.kt`):** the dialog-style centered option panels
are gone everywhere. Shared `HoldActionsPill`/`HoldAction` (internal): a
small rounded capsule that MORPHS in (spring `Animatable` 0.6→1 + fade)
holding only CIRCULAR ICON buttons (no text). Rewrites: `CategoryOptionPill`
(Pin/Spin/Remove circles), `MixOptionPill` (Edit/Delete circles), and the
Browse page — the Edit/Delete DropdownMenu on mix rows is gone (rows hoist
`mixHoldTarget` to screen scope for the full-scrim pill) and
`BrowseOptionPill` is the same circle-icon pill.

Files: `TopicShareCard.kt`, `TopicDatabaseScreen.kt`, `AppPreferences.kt`,
`SpinScreen.kt`, `NewCategoryPicker.kt`, `NewCategoryPickerBrowse.kt` +
`app/AGENTS.md` (v319 bullet) + changelog.

CI compiles on push; all six files brace/paren-balanced and imports
hand-audited (animation core + graphicsLayer + CircleShape added to the
picker; the picker/browse share the internal pill in-package).