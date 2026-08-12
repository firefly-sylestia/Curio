# Prompt.md — Research & Analysis Tracking

## Current Request (COMPLETE): Topic Browser scroll speed + A–Z fast-scroller + alphabetical default + Recents header

**Date:** 2026-08-12

**What was asked:**
1. Topic Browser scroll is "too slow" — tackle the lag and make it speed-scroll (the more you drag, the faster it gets).
2. Show topics alphabetically by default per category.
3. When tapping the scroll knob, show the alphabet, changing as you scroll.
4. The Recents screen header doesn't match the UI — make it match.

**Changes (3 files + docs):**
- **`ui/components/CurioScrollIndicator.kt`** — speed-scroll: the knob's drag now ramps on cumulative travel (`speed = 1 + (|cum|/160).coerceAtMost(3)`, per-event cap 180→240px; reversing the drag decays the ramp back to the gentle 2.5x crawl). New optional **A–Z fast-scroller**: params `alphabet: List<String>?`, `activeAlphabetIndex: Int?`, `onAlphabetSelect: (String) -> Unit`. Tapping the knob (total travel < 24px = tap, distinguished via a `dragTotalPx` accumulator) toggles a 26dp letter rail on the strip's outer edge; the strip animates 28→54dp and the knob stays in a fixed 28dp TopStart strip (with `contentAlignment = TopEnd` restored so the knob hugs the strip's right edge). Active letter highlights (primary, bold, 12sp); tapping a letter fires `onAlphabetSelect`. Rail only renders when `alphabet != null` — the other 8 indicator users are untouched.
- **`features/database/TopicDatabaseScreen.kt`** — DEFAULT sort is now **A–Z within each category** (`.sortedBy { it.nameKey }` in the DEFAULT rows branch, stable so ties keep file order; section headers kept; the A–Z chip still flattens globally). Wired the fast-scroller: `alphabetLetters` = A–Z, `activeAlphabetIndex` derived via `remember(rows) { derivedStateOf { ... } }` reading `listState.firstVisibleItemIndex` and walking to the first topic row (`name.first().uppercaseChar()`), `onAlphabetSelect` scrolls to the first row whose topic name starts with the letter (`scrollToItem` in a scope.launch).
- **`features/recent/RecentScreen.kt`** — header rebuilt in the settings-family torn-rose hero: `SettingsHeroHeader("Recents", …)` + `SettingsHeroTotalHeight` content padding + `ScreenEntrance`; the plain back-button/status-bar row (and `CurioBackButton`/`statusBarsPadding` imports) removed; feed scrolls under the tear; indicator padded below the hero; empty state padded down by the hero height. Matches Manage Categories / Topic Database / Topic History.

**Validation:** braces (3 files) + `git diff --check` clean; imports verified; no unused imports left in RecentScreen. Code-reviewer agent glitched (no findings delivered) → the change was reviewed manually: caught and fixed a layout regression (strip Box lost `contentAlignment = TopEnd`, which would have drawn the knob at the strip's left edge) and a smart-cast issue (rail `if (railOpen)` → `if (railOpen && alphabet != null)`).

**Interpretations made:** "alphabetically by default per category" = DEFAULT mode sorts A–Z within each category while keeping section headers (the global A–Z chip still flattens); speed-scroll = acceleration on cumulative drag distance, not a proportional knob-position map (the earlier "slow-only" constraint is superseded); the alphabet rail applies to the Topic Database only.

**Next:** none pending.
