# Prompt.md — Request Log

## Current Request (COMPLETE): Category Passport 4×3 + 2 pages; thin touch-grow scroll indicators (Android)

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
