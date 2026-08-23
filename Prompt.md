# Prompt.md — current request log

## Request: v246 — chip-bar glass, blob visibility, press feel, icon centering (COMPLETE, pushed)

User's batched asks across the session:

1. **Floating category pill in Cabinet + Topic Database → liquid glass**, with
   **one theme-only ink** (no per-category colors).
2. **Active tab icon + label vanished under the indicator** — only where the
   blob sat there was no icon/text.
3. **Touch press effect on capsules returned** — pill shrinks toward its
   middle while held + refraction blooms at the corners (from previous
   commits).
4. **Icon centering** in the search / back / home drawer-menu / avatar pills —
   still off regardless of font size.
5. **Moodboard quotes**: don't remove them; height grows with the text and
   keeps only one extra line of space (was stretching fully by height).

## What shipped

1. **Cabinet + Topic Database sticky chip bars are liquid glass now**
   (`CabinetScreen.kt`, `TopicDatabaseScreen.kt`). Each screen's scrolling
   list records a LOCAL `LayerBackdrop`; the chips are sibling overlays that
   sample it with `liquidGlassCapsule(alwaysClear = true)` — the same
   crash-safe architecture as every other in-screen pill. Labels use ONE
   theme ink: `Color.White` in dark, `Color.Black` in light, no per-category
   colors. Fixed two missing commas my interrupted script left behind.

2. **Active-tab content visible under the blob again**
   (`CurioLiquidGlassTabBar.kt`). Root cause of the vanish: v244 pointed the
   indicator's `drawBackdrop` at the page-only capture, which paints blurred
   page OVER the visible tab row sitting beneath it in z-order. Fix: restore
   the combined sample `rememberCombinedBackdrop(page, tabsBackdrop)` but
   make the hidden tab-row copy UNTINTED (removed its accent ColorFilter) —
   so icons/labels refract through the pill while the ink stays pure
   black/white (the old category-color ghost came from the tint, not from
   sampling).

3. **Press feel on floating capsules** (`LiquidGlassPills.kt`).
   `liquidGlassCapsule` gains an optional `interactionSource`: a spring
   Animatable drives (a) ~4% shrink toward the middle while held via
   `graphicsLayer`, and (b) lens refraction deepening ×(1 + 0.45·press).
   Wired on Home menu + avatar pills (`TopBarPill` new `pillInteraction`
   param), Profile back + search pills (`CurioBackButton` +
   `ProfileSearchPill` new params), Detail back + more pills. Call sites
   hoist one `MutableInteractionSource` shared by click + capsule.

4. **Measured icon centering** (`CurioIcons.kt`). `CurioIcon` now reads the
   glyph's real ink bounds from the text layout (`getBoundingBox`) and
   offsets by the delta between line-box center and ink center — every glyph
   self-centers at any font scale. Removed ALL `curioGlyphInkNudge` call
   sites (HomeScreen ×4, ProfileScreen ×2, SpinScreen ×2, CurioTopBar ×1)
   since they would double-correct; helper kept defined.

5. **Moodboard quote slip** (`MoodBoardZoom.kt`): keeps wrap-to-text height
   (two preview lines max) and adds ONE spare ruled line below the last text
   line (bottom padding 8→24dp). No fixed tall box, no full-board stretch.

## Verification

- Balance-checked all touched files (braces/parens green).
- CI validates compilation on push.
- Follow-ups to watch: chip-bar legibility over busy content; blob sample
  alignment during fast drags.

## Notes

- The moodboard quote REMOVAL request was superseded by this fix per user.
- `web/package-lock.json` user change untouched and uncommitted.
