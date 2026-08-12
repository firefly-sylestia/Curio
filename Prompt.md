# Prompt.md — Request Log

## Current Request (COMPLETE): Session time on entries + detail view (Android)

**Date:** 2026-08-12

**What was asked:** "In entries add the session time too alongside the topic bar. And show in detail view too."

**What was built:**
- **Data**: `CurioEntry.sessionTimeMillis: Long = 0L` (0 = none recorded). Room `CaptureEntity` gains the column; DB version 3→4 with `MIGRATION_3_4` (`ALTER TABLE captures ADD COLUMN sessionTimeMillis INTEGER NOT NULL DEFAULT 0`), registered with `fallbackToDestructiveMigration(false)`. Gson backups round-trip safely (old backups decode 0).
- **Recording**: `SaveCaptureScreen` stamps the pause-aware explore-session elapsed time at save, matched to the topic (categoryId + topicName). **Critical integration fix found by review**: both "write about it" flows (the done-dialog in `CurioNavHost` and Home's `CurrentlyExploringCard`) CLEAR the session before navigating, so a live read would always be 0 — added a v17 handoff in `ExploreSessionStore` (`handoffWriteSession` → `peekWriteSessionMillis`/`clearWriteSessionHandoff`): the ending flows stash the elapsed time pre-clear; the save page peeks it and clears it only on save SUCCESS (retry-safe); a still-running session (Recents path) is read live as fallback. Edit re-saves preserve the original field via copy().
- **Card**: `CurioEntryCard` meta row shows a Timer glyph + short duration ("12m") alongside the "Today/yesterday" label when a session was recorded.
- **Detail**: `heroDateTinyLabel` appends " · explored 12m" to the hero's frosted Date segment tiny line.
- **Shared helper**: `formatSessionShort(millis)` ("45s"/"12m"/"1h 24m") next to `formatElapsed`.

**Validation:** No Gradle build locally (project rule — CI validates on push; env has no Android SDK). Reviewed by code-reviewer-deepseek-flash — its catch (session cleared before save) was fixed via the handoff; its robustness note (handoff lost on failed save) was fixed by peek-then-clear-on-success. Known minor edges (accepted): in-memory handoff is lost on process death between end-session and save; a stale handoff lingers if the user leaves the capture screen unsaved (keyed to exact topic, so blast radius is tiny).

## Previous Requests

### Pet games frequency + long-distance teleport (Android) — COMPLETE
- Per-game cooldowns (hide-and-seek 40s, camouflage 40s, spark 30s) + 25s global gap — camouflage was starved because all games shared one clock the spark kept resetting (the chameleon game never reset it at all).
- Hide-and-seek: caught peek no longer stuck half-off-screen; duplicate win-line speech removed.
- Long-distance moves (>55% of the larger screen dimension) teleport (snap + landing squish) instead of sliding fast; spark chase still dashes.

### Category Passport 4×3 + 2 pages; thin touch-grow scroll indicators (Android) — COMPLETE
- `CurioScrollIndicator.kt` (new, foundation 1.12 ScrollIndicatorState) — thin 3dp overlay knob that grows on touch, drag-to-scroll, theme-ink colored.
- Passport = 4×3 per page, 2 pages via HorizontalPager + dots.
- Indicators on 8 screens: Topic Database, Profile, Quests, Settings hub + sections, Cabinet, Manage Categories, Topic History, Recent.

### AMOLED cleanup on Profile + Settings (Android) — COMPLETE
- `CurioSettingsCard` drops `tonalElevation` to `0.dp` in AMOLED (was 3.dp → Material3 tonal overlay washed the pitch-black cards with the coral scheme primary).
- `CompactSwitchRow` switches + reminder-hour chips go pitch-black glass (black track/knob + hairline rim) in AMOLED.

[See earlier request logs in git history]

**Date:** 2026-08-12

**What was asked:** "The hide and seek of the pet… it's not doing the camouflage that often… make it teleport when going long distance instead of a quick move."

**Root causes found (app/src/main/java/com/curio/app/ui/pet/CurioFloatingPet.kt):**
1. **Camouflage starved** — all three pet games (hide-and-seek, chameleon/camouflage, spark) shared ONE `lastGameAt` cooldown clock. The fast spark game (35s) and hide-and-seek (45s) reset it constantly, and the chameleon game **never reset it at all** — so the 60s camouflage gate almost never elapsed → camouflage ran once every few minutes.
2. **Hide-and-seek bugs** — (a) catching the pet mid-peek left it stuck half-off-screen at the edge; (b) the win line was spoken TWICE (tap handler `speakNow` + game loop `queueReaction`); (c) same shared-clock starvation.
3. **Long-distance movement** — `walkTo` interpolated in a fixed number of steps, so long wanders skimmed across the screen in big quick steps (the "quick move").

**Changes made:**
- **Per-game cooldowns**: `lastGameAt` (25s global min spacing between ANY two games) + `lastHideSeekAt` (40s) + `lastChameleonAt` (40s) + `lastSparkAt` (30s). Chameleon now sets its own clock AND the shared clock; base chances 0.05→0.06 for hide-and-seek and camouflage. Camouflage now runs roughly every 40–75s instead of every few minutes.
- **Hide-and-seek catch fix**: caught pet snaps fully back into view; duplicate win-line speech removed.
- **Long-distance teleport**: `walkTo` teleports (snap + landing squish) when the journey exceeds 55% of the screen's larger dimension; short hops still walk. The spark chase keeps its inline dash (it must be raceable); hide-and-seek/chameleon exits now blink off-screen.

**Validation:** No Gradle build locally (project rule — CI validates on push). Reviewed by code-reviewer-deepseek-flash: compile-safe (hypot/maxOf/stdlib, closure captures in scope, all gates updated consistently); flagged-and-accepted side effects: far landmark pokes may blink to the button, and game exit comments updated to match the blink motion. Tuning knobs are the five new `*_MS`/`LONG_JUMP_FRACTION` constants at the top of the file.

## Previous Requests

### Category Passport 4×3 + 2 pages; thin touch-grow scroll indicators (Android) — COMPLETE
- `CurioScrollIndicator.kt` (new, foundation 1.12 ScrollIndicatorState) — thin 3dp overlay knob that grows on touch, drag-to-scroll, theme-ink colored.
- Passport = 4×3 per page, 2 pages via HorizontalPager + dots.
- Indicators on 8 screens: Topic Database, Profile, Quests, Settings hub + sections, Cabinet, Manage Categories, Topic History, Recent.

### AMOLED cleanup on Profile + Settings (Android) — COMPLETE
- `CurioSettingsCard` drops `tonalElevation` to `0.dp` in AMOLED (was 3.dp → Material3 tonal overlay washed the pitch-black cards with the coral scheme primary).
- `CompactSwitchRow` switches + reminder-hour chips go pitch-black glass (black track/knob + hairline rim) in AMOLED.
- Scope: Profile + Settings confirmed by user; shared-card side effect on Quests/Onboarding disclosed (matches component's pitch-black intent).

[See earlier request logs in git history]

**Date:** 2026-08-12

**What was asked:** (1) Make the Category Passport show 4×3 per page and split it into 2 pages. (2) Add a side scroll indicator in the Topic Browser and "the places where it needs". (3) The indicator must be thin when idle, only grow when the user touches it to move, never move on-screen content (overlay), and its handle/knob must match the UI.

**What was built:**

1. **New shared component** `app/src/main/java/com/curio/app/ui/components/CurioScrollIndicator.kt` — `CurioVerticalScrollIndicator(state, onScrollBy, modifier)`:
   - Built on foundation 1.12's **new** `ScrollIndicatorState` (the classic `VerticalScrollbar`/`ScrollbarStyle` API was **removed** in this foundation version — verified via javap on the cached 1.12.0-alpha03 artifact; `scrollIndicatorState` is a cached member property on `LazyListState`/`LazyGridState`/`ScrollState`, confirmed via a `private final` backing field, so it's safe to key `pointerInput` on).
   - **Overlay only** — caller aligns it in the screen's Box (`align(CenterEnd).fillMaxHeight()`), so it never shifts layout.
   - **Thin when idle (3dp @ 0.30 alpha), grows to 9dp @ 0.80 alpha when touched** (animateDp/FloatAsState, 140ms tween).
   - **Drag-to-scroll** — `pointerInput` maps knob travel to scroll distance via `onScrollBy` (`listState.scrollBy(it)`); hidden entirely when content fits the viewport.

2. **Passport pager** `QuestsScreen.kt` (`PassportCard`) — `cats.chunked(12)` → 4×3 grid per page (`chunked(4)` rows), `HorizontalPager` + `rememberPagerState(pageCount = { pages.size })`; 21 visible lanes → 2 pages; dot indicator below when >1 page.

3. **Indicators wired into 8 scroll areas** (all `listState`/`gridState.scrollIndicatorState` + `scrollBy`, top padding clears heroes/pinned bars):
   - Topic Database (the browser), Profile, Quests, Settings hub (grid), Settings sections
   - Cabinet (grid), Manage Categories, Topic History (list wrapped in a Box so the strip matches the list region), Recent (same wrap-in-Box pattern)

**Validation:** No Gradle compile/build locally (project rule — CI validates on push; env has no Android SDK). API usage verified against the cached foundation 1.12.0-alpha03 artifact (javap: `ScrollIndicatorState` interface = `scrollOffset`/`contentSize`/`viewportSize`; `weight` is scope-free in this version, which the TopicHistory/Recent Box wraps rely on). Reviewed by code-reviewer-deepseek-flash — flagged `pointerInput(state)` key stability (cleared: instance is a cached `final` field) and RecentScreen's hardcoded 88dp top padding (fixed by adopting the wrap-in-Box pattern). Known minor edge case (accepted): if the user hides categories, passport page row counts can diverge (e.g. 12/8), causing a small pager height change on swipe — both pages are 3 rows at the default 21 lanes.

## Previous Requests

### AMOLED cleanup on Profile + Settings (Android) — COMPLETE
- `CurioSettingsCard` drops `tonalElevation` to `0.dp` in AMOLED (was 3.dp → Material3 tonal overlay washed the pitch-black cards with the coral scheme primary).
- `CompactSwitchRow` switches + reminder-hour chips go pitch-black glass (black track/knob + hairline rim) in AMOLED.
- Scope: Profile + Settings confirmed by user; shared-card side effect on Quests/Onboarding disclosed (matches component's pitch-black intent).

[See earlier request logs in git history]
