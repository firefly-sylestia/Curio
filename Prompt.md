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
- **Phase 1 DONE (Spin)** — committed `8920a88c`, then PUSHED on user's
  "push and continue" (`cd1533b8..8920a88c`).
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
- **Phase 2 DONE (Home) — committed locally `170d43c1`, NOT pushed**
  (awaiting user; Phase 1 was pushed on their "push and continue").
  In `HomeScreen.kt`: `catalogTotal` hoisted beside totalSaved; the torn
  quest-hero wrapper collapses on wide and renders the new
  `HomeEditorialHeader` (BoxScope, comfortable 800dp column): greeting
  kicker + ExtraBold name, Streak · Cabinet · Topics as quiet flat
  panels (`HomeEditorialStat`, surfaceContainerLow, rounded 18, no
  elevation, same nav as the phone stats), hairline rule. Phone hero
  byte-identical (same brace/paren balance as HEAD). Body sections and
  sticky pills unchanged below the header.
- **Phase 3 DONE (Cabinet + Recent) — committed locally, NOT pushed.**
  - `CabinetScreen.kt`: `CabinetHeroHeader` goes editorial on wide
    (`compact` == wide): banner backing turns transparent + square, torn
    under-sheet/shadow/watermark symbols phone-only, ink resolves to
    theme on-surface colors over the wash, search glass sits on a quiet
    panel tint; new `CabinetEditorialHeaderHeight = 148.dp` feeds
    `heroTotal` so the filter panel/chips align under the actual header.
    Phone hero byte-identical (heroInk/heroWash resolve to the old
    values when !compact). Grid stays Adaptive(176dp) wide.
  - `RecentScreen.kt`: both torn `SettingsHeroHeader` uses branch wide →
    new `RecentWideHeader` (flat back pill + identity row, hairline-free)
    on the wash; list top padding uses `RecentWideHeaderHeight = 124.dp`
    wide. Phone path untouched. (SettingsHeroHeader itself is shared with
    Topic Database / History → Phase 4/6 redesigns it once.)
  - Note: true master–detail in-pane previews + persistent drag-reorder
    were deferred — entries open through the torn EntryDetail route
    (Phase 5) and reorder needs a new persistent order field + migration
    (Phase 6 conversation).
- **Phase 1 Spin tune (user feedback) — this commit.** The tablet Spin page
  was rebuilt per the user's follow-up: (1) the editorial title header
  (identity + pool count + hairline) is REMOVED — "the deck is bad again,
  don't give that header", so the deck IS the page; (2) the deck is
  SMALLER on tablets: `wideFit` cap 1.6 → 1.12 and the height fit is now
  `stageFit = ((maxHeight − 250dp)/470dp)` capped at 1.1 (was a fixed
  headroom ÷ 600 capped 1.4) — the hand sits near phone scale instead of
  blowing up; (3) the Category/Filter pills became FLOATING buttons
  bottom-center (`padding bottom 30dp`) instead of an inline row under
  the orb, and the deck stage reserves `bottom = 96dp` so short
  landscape windows never clip the pills or the spin orb. Phone branch
  untouched (balance 0/0 vs HEAD). Pushed.
- **Phase 4 DONE (Topic Browser + shared settings hero)** — this commit.
  - `TopicDatabaseScreen.kt` wide layout rebuilt as master–detail:
    `ScreenEntrance → Row { master Box(weight 1f) + reveal pane(344dp) }`.
    - Multi-column grid: `displayRows` = `buildWideRows(paginatedRows)`
      merges consecutive topic rows into two-up `DatabaseWideRow` pairs
      (keys `p-{a}|{b}` / `t-{k}`; sections/group headers stay
      full-width); phones map rows 1:1 → single-cell slots, byte-
      identical. `DatabaseTopicRowSlot` renders one or two rows; taps
      route through `onTopicTap`.
    - Reveal pane: `DatabaseRevealPaneSlot` — placeholder ("Select a
      topic") or `DatabaseRevealPane` (category chip, ExtraBold title,
      byline · subtype, explored badge, hairline, teaser, synopsis, tag
      FlowRow chips, Open-topic CTA → revealForBrowse). Pane state
      `paneTopic` cleared on needle/cats change; top-aligned to
      `contentTop`.
    - Overlays (scroll indicator, back-to-top, page nav, filter
      panel/chips) moved INSIDE the master Box so they never draw over
      the pane; master end padding 12dp on wide; back-to-top + panel
      offsets use `settingsHeroContentTopHeight()`.
  - Shared settings-hero pass (`SettingsHubScreen.kt`):
    `SettingsHeroHeader` branches wide → flat editorial header
    (`SettingsWideHeroHeight = 148.dp`, surfaceContainerHigh fill,
    onSurface ink, hairline rule; tear/sheet/shadow/symbols phone-only).
    New `@Composable settingsHeroContentTopHeight()` = wide
    height or `SettingsHeroTotalHeight`; ALL 13 settings-family
    consumers updated (Backup, UserExperiments, Experiments,
    SettingsSection, ShareHub, SettingsHub, Updates, Outfits,
    ManageCategories, PetDesigner, Quests, PromoMode, Support,
    RecycleBin; Recent keeps its own wide branch). Phones untouched.
  - Verification: 15 files brace/paren-balanced 0/0/0 vs HEAD, no
    negative nesting; depth walk confirms ScreenEntrance → Row → master
    Box → overlays → pane → closes; stale const/import refs gone;
    new symbols wired (displayRows/wide build/panes).
  - Follow-ups deferred to Phase 5/6: torn EntryDetail/Reveal heroes,
    keyboard/hover, drag-reorder (needs DB order field + migration
    conversation).

## Tuning knobs (Phase 1)
- `xOff` ±73/±129 (PeekCard) — sliver width per side.
- `yOff`/rotation ∓5/∓12, ±2.4/±5 — hand arc.
- Carousel horizontal box height 470dp; header paddings;
  `stageHeadroom` −236dp & 600dp divisor / 1.4 cap in the wide branch.
