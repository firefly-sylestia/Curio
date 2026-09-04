# Prompt — Tablet-wide redesign (editorial, no torn) + new Spin layout + tablet features

## Request (user, 2026-09-04)
"Completely redesigning the layout in tablet mode — even for wide screens.
Fully redesigning, and NOT using torn style in tablet UI, totally different
layout, additional tablet-targeted features, and a new layout for the
shuffle page too."

## Design decisions (user-confirmed)
1. **Style: Clean editorial.** Flat surfaces, large type hierarchy, thin
   dividers, quiet spacing — no heavy card chrome, no tears, no ragged
   edges. Palette (rose/ink + category colors) stays the same.
2. **Scope: everything incl. secondary.** Main tabs (Home, Spin, Cabinet,
   Profile) + Topic Browser, Reveal/Detail, Settings — full sweep.
3. **Spin (shuffle) layout anchor:** the deck is currently VERTICAL (front
   ticket with peeks stacked behind). On tablet it becomes SIDEWAYS — a
   horizontal hand/fan across the wide stage — and the rest of the Spin
   page is redesigned coherently around that.
4. **Tablet-targeted features: ALL of them** (master–detail panes,
   multi-column grids, drag & drop, keyboard + hover, split-view companion
   info) — user asked for my ranking of which are best/most functional
   (delivered in chat) and to add all.
5. **Ship mode: always-on**, like the current wide layout (>=600dp wide →
   new design; replaces today's wide mode outright; no Settings toggle).

## My feature ranking (delivered to user)
Most value → least, all being added:
1. **Master–detail panes** — biggest tablet win: list + live preview
   (Cabinet list/open entry, Browser results/reveal, Recent/detail).
2. **Multi-column grids** — turns the narrow phone rows (Cabinet shelves,
   Browser, Stats) into true tablet spreads.
3. **Split-view companion pane** — related topics/lanes/notes beside the
   current reading surface (Reveal, Home hero, Detail).
4. **Keyboard + hover** — cheap, high-functionality: arrow nav, hover
   reveals, context menus for keyboard/tablet-trackpad users.
5. **Drag & drop** — most powerful but needs the most design care; rolls
   out after 1–4 have settled (Cabinet reorder, card moves, etc.).

## Editorial tablet design tokens (Phase 0 — new `ui/tablet/` package)
- **Surfaces:** page = flat tinted canvas (current theme background);
  cards become quiet `surfaceContainerLow/High` panels with
  `RoundedCornerShape(16–20)` tops, hairline `1dp` dividers; NO torn
  shapes anywhere in wide mode.
- **Headers:** large page title (display/headline), thin rule under the
  title bar instead of torn hero banners; section headers = small caps
  label + hairline.
- **Type:** lean on the existing type scale, bumped one step on wide
  (hero titles to displayLarge, section titles to titleLarge).
- **Rail chrome:** existing NavigationRail; nav rail content restyled
  editorial (flat selected indicator, no capsules/shadows).

## Phase plan (each phase = commit; wide-only so phones untouched)
- **Phase 1 — Foundation + Spin (shuffle) pilot.** New tablet editorial
  tokens/primitives (`ui/tablet/`); Spin's wide layout fully redesigned:
  horizontal deck hand (ticket + 2 peeks fanned sideways, scaled to the
  stage), editorial header strip, side browse/queue panel, controls and
  reveal redesigned to match, watermark gutters retained. Phone Spin
  untouched.
- **Phase 2 — Home.** Editorial wide layout: title strip + stat row
  (streak/cabinet/recent as flat panels), lane chips, companion "continue
  exploring" pane.
- **Phase 3 — Cabinet + Recent.** Master–detail (list ↔ open entry) +
  multi-column grid; drag & drop reorder.
- **Phase 4 — Topic Browser.** Multi-column results + master–detail
  (row → reveal pane) + keyboard/hover.
- **Phase 5 — Reveal/Detail.** Editorial content column, no torn sheet;
  companion "related" pane; big cover layout.
- **Phase 6 — Settings/Profile + polish sweep.** Editorial sections,
  grids; hover/keyboard everywhere; tear references removed from wide
  paths (phone paths untouched).

## Status
- Crash fix (pre-design): share card exported a fetched cover as a Coil
  HARDWARE bitmap into a software capture canvas →
  "Software rendering doesn't support hardware bitmaps". Fixed with
  `.allowHardware(false)` + memory-cache DISABLED on the cover fetch
  (MoodBoardExport recipe). COMMITTED + PUSHED (`cd1533b8`).
- **Phase 1 DONE (Spin) — committed locally, NOT pushed (user review).**
  Pure-stage + tucked-hand decisions implemented in `SpinScreen.kt`:
  - Wide branch rebuilt: editorial page header (deck identity + pool
    subtitle, hairline rule — no tear), deck stage fills the rest.
  - `horizontal` param threads SpinDeckSection → Carousel → PeekCard.
    Peek strips re-fan LEFT/RIGHT (xOff ∓73/∓129dp scaled, tips-up arc
    yOff ∓5/∓12, rotation ±2.4°/±5°) tucked under the 143dp-half hero;
    content rides the outer edge; wipes run along the horizontal axis;
    vertical geometry byte-identical (phone untouched).
  - Horizontal scale = min(width wideFit, height headroom/600dp capped
    1.4) so short landscape windows compress instead of clipping.
  - Category/Filter pills flat (shadowElevation 0).
  Numbers are tunable against device screenshots.
- Next: Phase 2 Home editorial wide layout.

## Tuning knobs (Phase 1)
- `xOff` ±73/±129 (PeekCard) — sliver width per side.
- `yOff`/rotation ∓5/∓12, ±2.4/±5 — hand arc.
- Carousel horizontal box height 470dp; header paddings;
  `stageHeadroom` −236dp & 600dp divisor / 1.4 cap in the wide branch.
