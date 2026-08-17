# Prompt.md — Request log

## Current request — COMPLETED: progress editor, books-dedupe loading, reveal tags, browser icons, drawer

The user reported (two messages): (1) the progress editor "isn't working" and its
layout is "bad looking"; (2) the books dedupe made OLD saved entries show the topic as
"Loading…"; (3) the Topic Browser shows extra info (e.g. the aardvark's "earth pig")
that the reveal doesn't surface — show hidden info as tags (user picked "all tags +
year chips" after the data analysis); (4) browse-topic category icons still use the old
look and its shade is invisible in dark mode; (5) the Home drawer must open ABOVE the
floating navbar; (6) drawer sections need hierarchy backgrounds, a distinct collapse
indicator, and expanding a section must visibly change the drawer size.

### 1 — Progress editor (CurioProgressPill.kt)
Reasons it felt broken/ugly: the slider used `steps = (target - 1).coerceAtMost(600)`
— a 1000-page book snapped to non-integer step positions (1000/601) that fought the
rounded Int value state (thumb jitter / "isn't working"), and the hidden tappable count
+ inline field overflowed the 132dp ring Box. REDESIGNED: big count flanked by −/+
steppers, an explicit "of {target} {unit}" chip with an edit pencil (inline number
field stays), a full-width progress bar + %, and a slider that uses whole-unit `steps`
only when `target <= 200` (continuous + round otherwise). The ring is gone. Dead
Canvas/Stroke imports removed.

### 2 — Books dedupe → "Loading topic…" (TopicCatalog + reveal)
The dedupe (620ae50) collapsed year-less books into canonical "Name (Year)" entries
("The Odyssey" → "The Odyssey (c. 8th century BCE)", "Moby-Dick; or, The Whale" →
"Moby-Dick (1851)"), so saved entries referencing the OLD names failed the reveal's
exact `name == topicName` lookup. Added `CurioTopic.matchesSavedName` (case-insensitive
exact → base-name with trailing "(…)" / "— …" stripped → containment, both sides ≥ 4
chars), used by `TopicCatalog.findByName` AND the reveal's per-pool fallback. A truly
unresolvable topic now shows its requested NAME (HeroCard `fallbackName`) and hides the
teaser instead of a permanent "Loading topic…".

### 3 — Hidden info → reveal tags (user chose "all tags + year chips")
Data analysis: NO hidden structured fields exist (no subtitle/aka anywhere; "earth
pig" is prose inside the aardvark's teaser, shown by both surfaces). Implemented the
chosen option: the reveal's TagsRow now shows ALL the topic's tags (FlowRow wrap — the
old `take(3)` thirds cap is gone) plus a derived decade chip via the new
`CurioTopic.publicationYear()` / `derivedDecadeTag()` helpers (year from name "(1941)"
→ explore target → teaser → instruction → decade tag; skips a duplicate decade tag).
The Topic Database's private `topicYear` now delegates to `publicationYear` (DRY).

### 4 — Topic Database category icons (TopicDatabaseScreen.kt)
The topic-row icon tiles used the OLD raw `cat.accent` fill + tint — a deep accent on
a 14% deep-accent tile is invisible in dark mode. Now the modern theme-aware recipe:
`cat.categorySurface(surfaceContainerLow)` tile + `cat.categoryInk()` glyph; the
section-header dot uses `cat.themedAccent()`.

### 5 — Drawer above the floating navbar (HomeScreen + CurioNavHost + CurioBottomNav)
The floating pill bar is drawn by the NavHost AFTER the NavHost content, so it sat ON
TOP of the drawer's scrim/sheet. New `CurioDrawerState` object (mirrors CurioNavTint):
HomeScreen publishes `drawerState.isOpen` (LaunchedEffect + onDispose reset), the
NavHost gates `CurioFloatingNavBar` on `!CurioDrawerState.isOpen`.

### 6 — Drawer section hierarchy + indicator + size (HomeScreen)
`DrawerSectionHeader` is now a RAISED `surfaceContainerHigh` pill (solid when open,
opaque lerp blend when closed — no translucent-under-shadow), with a distinct filled
circle toggle badge (primary-tinted ▲ when open). The expanded rows are ONE grouped
item: `AnimatedVisibility(expandVertically)` with a soft grouped card
(`surfaceContainerHigh` @45%) — the drawer visibly grows when a section opens instead
of rows silently appearing in a same-size sheet.

### Files touched
- `app/src/main/java/com/curio/app/ui/components/CurioProgressPill.kt` — editor redesign
- `app/src/main/java/com/curio/app/data/CurioTopic.kt` — publicationYear + derivedDecadeTag
- `app/src/main/java/com/curio/app/data/TopicCatalog.kt` — matchesSavedName + tolerant findByName
- `app/src/main/java/com/curio/app/features/reveal/TopicRevealScreen.kt` — tolerant lookup, hero fallback name, teaser gating, TagsRow (all tags + decade, FlowRow)
- `app/src/main/java/com/curio/app/features/database/TopicDatabaseScreen.kt` — icon tiles + section dot + shared year helper
- `app/src/main/java/com/curio/app/features/home/HomeScreen.kt` — drawer publish, grouped animated sections, header redesign
- `app/src/main/java/com/curio/app/ui/components/CurioBottomNav.kt` — CurioDrawerState
- `app/src/main/java/com/curio/app/navigation/CurioNavHost.kt` — nav bar gated on drawer open
- `app/AGENTS.md` — v135 bullet; `fastlane/.../changelogs/20260920.txt` — FIX bullets

Not done: no Gradle build here (env forbids it) — CI validates on push.
