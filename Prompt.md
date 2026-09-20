# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "and also from your lane page remove the lane map. use better graph compact and less
> complex graph for category stats. also a bug in reading progress the +- only accets one
> tap click and doesnt work even when i try to chnage the pages chapters or book legth
> count, the ta an dhold works i hear the haptics but the count only goes 1 and no more wait
> it goes forward but after so long or when i scroll a little and tap again it gos ahead..
> also for color theme of adaptive hero dont gray out the paper option"

Three parts: (a) replace the Stats page's lane grid with a compact graph, (b) fix the broken
`+`/`−` steppers on a book's reading progress, (c) stop greying out the Paper option under
the Adaptive Hero theme. (The "device in theme option" icon from the same week's earlier
batch already shipped — `CurioIcons.Contrast`, commit `3770d967`.)

## 2. What the code actually looked like (findings)

### (a) The lane map on Stats
- `features/stats/StatsScreen.kt` → `LaneMapCard` drew `CurioLaneGrid(columns = 4,
  tileHeight = 78.dp)` — the interactive tile grid — plus a `CurioLaneDetailStrip` and a
  `Cabinet` door per tile.
- That is a *navigation* surface wearing a statistics label: 24 tiles, a reveal strip and a
  door each, on a card whose job is to show the shape of the data.
- After the v413 drawer chart work the `CurioLaneGrid` composable had **no caller left**
  anywhere in the app (the drawer draws its own `DrawerLaneStarMap`), so this was the last
  use of the grid *shape*.

### (b) The reading-progress steppers — the real bug
Two independent faults, both in `features/personal/BookDetailScreen.kt`:

1. **`TileStepButton` captured its callback ONCE.** The gesture lives in
   `pointerInput(Unit) { detectTapGestures(…) }`, whose block runs exactly once — so the
   `onClick` it closed over was the one handed in on the FIRST composition, and every step
   after that computed from the chapter/page/total the card was showing back then. Tapping
   `+` moved 0 → 1 and then re-proposed "start + 1" forever. This is precisely the reported
   symptom: the count sticks at one, and "it goes ahead ... when i scroll a little and tap
   again" because scrolling the `LazyColumn` item out of view and back rebuilds the card
   from scratch, which re-captures a fresh closure.
2. **The card printed the PERSISTED value only.** Every step had to survive a database
   write *and* a flow round trip before the number moved, so a hold that ticked twenty times
   still displayed one step. The haptics fired (so the button felt alive) while the number
   looked dead.

### (c) Paper under Adaptive Hero
- `SettingsSectionScreen.kt` computed `paperApplies = !dark && (Curio || Azure)` — v412 had
  put Adaptive Hero in the greyed-out set on the assumption that it paints its own page.
- It does not: `curioColorScheme()` only early-returns for **Material** and **Pantone**. A
  lane tints the PAGE WASH (`heroPageBackground()` → `categoryBackgroundWash()`) and the torn
  hero; the card ladder underneath is still the scheme's own cream/white pair. So the row was
  disabled while the control still had a real, visible effect.

## 3. What was done

### (a) `LaneStatsGraph` replaces the grid (`features/stats/StatsScreen.kt`)
- The card is titled **"Lane stats"** and draws a **ranked horizontal bar list**: one row per
  lane — glyph, name, the count, and one bar — sorted by knowledge, strongest first.
- **Bars are relative to the strongest lane, never to 100**, so the chart reads as "where is
  it concentrated" the moment there is any data. Each bar grows in with one
  `animateFloatAsState` (520ms, `FastOutSlowInEasing`).
- **Cut to `LANE_BARS_SHOWN = 7`** with a counted tail line ("+N more lanes in your
  Cabinet"), so the card's height never depends on how many lanes a reader has met — a
  36-row bar chart is a list, not a chart.
- A row is still a **tap that selects** (tap again to clear). The `CurioLaneDetailStrip`
  readout and the single `Cabinet` door now render **only for the lane that is selected**,
  so the card names a lane once instead of 24 times. `PendingCabinetFilter.request` +
  `navigateToTab` handoff is unchanged.
- Imports: added `clickable`, `lerp`, `animateFloatAsState`/`tween`/`FastOutSlowInEasing`;
  dropped the now-unused `CurioLaneGrid` import. `LaneGridItem` / `laneGridItems` /
  `CurioLaneDetailStrip` are still the shared lane DATA — only the shape changed.

### (b) The steppers actually step (`features/personal/BookDetailScreen.kt`)
- **`rememberUpdatedState(onClick)`** in `TileStepButton`, with `step.value()` called from
  both the hold loop and the trailing tap. The gesture is NOT restarted on a new callback —
  that would drop the press the finger is still holding.
- **Three optimistic overlays in `ProgressCard`** (`chapterMove` / `pageMove` / `totalMove`,
  each cleared by a `LaunchedEffect` keyed on the persisted value it shadows). Every step
  reads the OVERLAY, never the persisted value, so a hold piles onto itself the way the finger
  expects and the number moves the instant the button is pressed; the moment the book's own
  value changes, the overlay steps aside for it — the card shows either the truth or the truth
  in flight, never a stale read.
- `stepChapter` / `stepPage` / `stepTotal` are delta functions that clamp, skip no-ops, set
  the overlay and then fire the write. `BookLengthRow` was switched from an absolute
  `onTotal(newValue)` to the same delta `onStep(±1)` so all three controls share one path.
- The gauge's `fraction` is computed from the overlays too, so the bar moves WITH the finger.

### (c) Paper stays live under Adaptive Hero (`features/settings/SettingsSectionScreen.kt`)
- `paperApplies` now includes `AppPreferences.COLOR_THEME_LANE`, the disabled hint reads "The
  paper follows the Curio rose, Azure and Adaptive Hero themes.", and the v412 comment block
  was corrected to name only Material and Pantone as the themes that paint their own page
  pair. Dark mode and those two stay greyed out.

Docs + notes: `app/AGENTS.md` (the `CurioLaneGrid.kt` bullet rewritten to "shared DATA, each
surface draws its own shape", the Stats-page four-instruments bullet updated for
`LaneStatsGraph`, and a new "Which themes keep the Paper row LIVE" bullet); three bullets
appended to the `20260922` changelog (one ADD, two FIX).

## 4. Decisions

- **"Less complex" was read as fewer surfaces, not a different metric.** The grid carried
  tile + ring + meter + readout + door ×24; the bar list carries glyph + name + count + bar
  ×7, and the readout/door are promoted to the one selected lane. Nothing became
  unreachable — selecting still works and the Cabinet door still opens filtered.
- **Relative bars, not an absolute scale.** A share-of-total scale would leave a
  seven-lane reader's chart looking nearly empty; relative-to-strongest is the honest read of
  "where is my knowledge concentrated".
- **The optimistic overlay is the deliberate fix for the hold, not just a nicety.** The stale
  callback alone explains the stuck-at-one count, but with a write round trip per tick a hold
  would still *display* one step at a time. Both halves were needed to make the button behave
  as the member described.
- **`CurioLaneGrid` (the composable) is now unreferenced.** The house rule is to ask before
  deleting anything, so it is left in place, unused, rather than removed silently — flagged
  here and in `app/AGENTS.md`.

## 5. Status

- Implemented; brace/paren balance verified (0/0) on all three edited files.
- No Gradle available in this environment — CI validates the compile.
- `CurioLaneGrid` composable left unreferenced pending the member's call on removing it.
- Known, pre-existing (not introduced here, not fixed): under **Material + pastel mode** a
  single lane keeps the device colour without the category accent foot.

---

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — this request is logged above as §1–§4)
