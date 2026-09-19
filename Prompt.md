# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request

> "now in the drawer menu of home screen, the drawer constellation i want you to redesign it
> remove the quests and levels button remove your curiotcity button, the stats insights manage
> category etc, and remove the about from the drawer we will be reimagining the full drawer,
> with new redesigned view with proper your brain stats view, also in your curioity page theres
> too much info. too much texts also remove the your lanes from your curioity page, and lets
> redesign it beautifully. also for the costellastion nremove the buttom 3 stars and make the
> main constellaion bigger and it should not be a svg anymore or drawing, but proper ui
> inetarctive style and make it clean. in a new branch, and do more screen aduits with duplicate
> things and redundance thingies"

Branch: `feat/drawer-and-curiosity-redesign` (started before the edit; `git pull` ran first).

### Decisions taken with the member (ask_user, before any edit)

1. **Drawer content** → *brain panel only*. Hero + a redesigned "your brain" panel + footer.
   No navigation rows survive.
2. **Constellation shape** → *drop it, use an interactive lane grid* (both surfaces). No more
   Canvas, no star tables, no painted sky.
3. **The old experiment switch** → *remove it, hardcode the new drawer* (`Drawer constellation`,
   and with it `3D star zoom` — both were switches for a canvas that no longer exists).
4. **Stats page trims** → "remove unnecessary texts and make it beautiful to look the progress"
   (free rein): cut the six tip paragraphs, cut the duplicated lane list, merge the two
   level/stage cards, keep the meters.

## 2. What the code actually looked like (findings)

- **The drawer (`HomeDrawerContent`) was three navigation trees under a stats map**: a
  `DrawerNavRow` for Quests & Levels, a collapsible "Your Curiosity" group (Topic History /
  Manage Categories / Browse Topics), a collapsible "About" group (Support & diagnostics /
  Replay intro), and a top slot that swapped between `DrawerCuriosityMap` (the Canvas star map)
  and `DrawerMaterialStatStrip` (a three-pane stat strip) on `drawerConstellationState`.
- **The constellation was a painted Canvas**: 11 stars of a Big Dipper + Polaris, its own
  nebula/starfield painters, twinkle/pulse animations, pinch-zoom + parallax, and a floating
  popover — 887 lines of `CurioConstellation.kt` plus two prefs. "Remove the bottom 3 stars"
  = Polaris, Kochab, Pherkad (the three below the dipper, the ones joined by dashed lines).
- **Every removed door still exists elsewhere** (verified by grep before deleting): Topic
  history + Manage categories are `SettingsHubScreen` rows, Browse topics is Home's own door,
  Quests is on Profile and on the Stats page, Support + Replay intro are Settings rows under
  Safety & support. Nothing lost its only way in.
- **`PendingCabinetFilter`** (the out-of-band handoff that opens the Cabinet pre-filtered to one
  lane) had **no callers left** after the earlier Profile "Your lanes" removal — dead API in
  `CurioRoutes.kt` with a live consumer in `CabinetScreen`. Revived as the lane map's door.
- **The Stats page was six cards of prose**: streak card + journey card (two cards about the same
  level number), a constellation card with a header + three chips + a range pill, a brain card
  printing SIX science-tip paragraphs, a lifetime grid nesting seven sub-cards with its own
  subtitle, and the "Your lanes" list repeating the constellation underneath it.

## 3. What was done

### The lane map — real UI (`ui/components/CurioLaneGrid.kt`, new)
- `LaneGridItem` + `laneGridItems(knowledge)` derive the items from the member's real knowledge
  (`LaneKnowledge.score`): explored lanes first by knowledge, then the rest in the member's own
  lane order, hidden lanes excluded (reads `CurioCategories.visible` in composition so a
  Manage-categories change re-lays the grid).
- `CurioLaneGrid` lays them out as tiles (icon, name, a knowledge bar scaled to the member's
  strongest lane) with a real ripple, an animated accent ring on the selected lane, and the
  bar growing in on first paint. `columns`/`tileHeight` per caller (4 columns both places).
- `CurioLaneDetailStrip` is the selected lane's own line. It is passed as `detail` and renders
  **under the selected tile's own row**, so a tap near the foot of a long grid never drops the
  readout off-screen.
- **`CurioConstellation.kt` deleted** (887 lines), with `starZoom3dState` / `KEY_STAR_ZOOM_3D`,
  `drawerConstellationState` / `KEY_DRAWER_CONSTELLATION` and their getters/setters.

### The drawer (HomeScreen)
- `HomeDrawerContent`'s LazyColumn is now `item("brain")` + `item("footer")`.
- Deleted: `DrawerNavRow`, `DrawerNavItem`, `DrawerCuriosityMap`, `DrawerMaterialStatStrip`,
  `DrawerMaterialStatPane`, both expansion flags, and the now-orphaned imports.
- `DrawerBrainPanel`: a "YOUR BRAIN" card (the panel's ONE door → `CurioRoutes.STATS`) holding
  streak · level · total knowledge with the XP bar and "N XP to level N+1", then a
  "YOUR LANES / N of M explored" line over the lane grid. Tiles only select.

### The Stats page ("Your Curiosity")
- Four instruments: `ProgressCard` (streak + level/XP + journey stages + medals + one Quests
  door — it replaces `StreakLevelCard` AND `JourneyCard`), `BrainCard` (six meters, the tip
  printed for the WEAKEST dimension only), `LaneMapCard` (the lane grid + the range pill + a
  Cabinet door on the selected lane through `PendingCabinetFilter.request` + `navigateToTab`),
  `LifetimeTotalsCard` (compact counter panes).
- Deleted: `StatsConstellationCard`, `LanesBreakdownCard` ("Your lanes"), `StatsSummaryChip`,
  `JourneyCard`/`StreakLevelCard` (merged). `StatsCard`'s shell is now the app-wide WHITE card
  (`surfaceContainerLowest` + an `outlineVariant` hairline) instead of the old seafoam lerp.
- `StatsRangeSelectorPill` stays (it is the page's window filter); its doc comments were
  corrected — the drawer no longer shares that state.

### Settings
- `ExperimentsScreen`: the "Constellation" section heading is now "Navigation" and holds only
  the nav-bar "Classic active indicator" row; the two star-map switches are gone.

### The audit (requested again)
- Re-ran the two automated passes over `features/` (repeated UI-copy literals per file; repeated
  `CurioRoutes` destinations per file) and read every hit. Result: **no new co-visible
  duplication.** The hits are (a) per-list-item navigations (`revealFor`/`socialProfile`/
  `directMessage` once per row) and (b) the header/empty-state/phone-vs-wide BINARY branches —
  one copy per branch, never two on one screen. Recorded in `app/AGENTS.md` so a later session
  does not "fix" them.
- Doors re-verified after removing the drawer menu: every destination it carried is still
  reachable (list in §2).

### Docs & release notes
- `app/AGENTS.md`: new section "The drawer and the lane grid — no more painted constellation
  (v409)" (the drawer contract, the lane-grid component contract, the deleted component, the
  removed switches, the four Stats instruments, the audit result), and the v408 redundancy
  section updated so it no longer points at rows that no longer exist.
- `fastlane/metadata/android/en-US/changelogs/20260922.txt`: REMOVE/ADD/FIX block at the top.

## 4. Still open

- **Nothing here is device-verified.** No Gradle in this environment: every change is
  `node scripts/check_braces.js`-checked, import-swept and diff-reviewed; **CI is the compile
  check**.
- Worth a device pass: the drawer panel's rhythm (the "YOUR BRAIN" card and the lane tiles are
  both the `surfaceContainerHigh` chip rung, separated by gaps and the grid's own borders), the
  tile label ellipsis at ~70dp (4 columns), and the lane bar's grow-in.
- `CurioColors.DustyBlue` is the knowledge stat's tint in the drawer; if the new panel wants a
  single accent for all three stat panes, that is one constant.

---

## User prompts

Status: **done and pushed** on `feat/drawer-and-curiosity-redesign` (the branch's own commits;
`main` is untouched). The prompt below is the one this file describes; the empty slot under it
is where the next instruction lands.

> "now in the drawer menu of home screen, the drawer constellation i want you to redesign it
> remove the quests and levels button remove your curiotcity button, the stats insights manage
> category etc, and remove the about from the drawer we will be reimagining the full drawer,
> with new redesigned view with proper your brain stats view, also in your curioity page theres
> too much info. too much texts also remove the your lanes from your curioity page, and lets
> redesign it beautifully. also for the costellastion nremove the buttom 3 stars and make the
> main constellaion bigger and it should not be a svg anymore or drawing, but proper ui
> inetarctive style and make it clean. in a new branch, and do more screen aduits with duplicate
> things and redundance thingies"

<!-- Next user prompt goes here. This section is never cleared — the pending prompt and its
status stay at the top, and the empty slot below is where the next instruction lands. -->
