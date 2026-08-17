# Prompt.md — Request log

## Current request — COMPLETED: bigger nav pill + solid indicator + the page-switch "dim flash"

The user: "increas the pill size a little and its legth and improve its indicator
color. also why switching between pages now shows a dim look like dim flash or something
or maybe something else. but it wasnt like this before? fix it"

### 1 — Bigger pill + longer (CurioBottomNav)
The floating pill bar grew a little: `FloatingPillHeight` 48 → 52dp,
`FloatingPillIconWidth` 48 → 52dp, `FloatingPillExpandedWidth` 96 → 112dp, icons
22 → 24dp, row padding 6 → 7dp, spacing 4 → 6dp. The tab pages' pill clearances grew
with it so content never hides under the bigger bar: Home's final spacer 92 → 100dp,
Spin's phone Column bottom padding 76 → 84dp, Cabinet's 76 → 84dp (0 wide).

### 2 — Indicator color (CurioBottomNav)
The active pill used the translucent `secondaryContainer` overlay + `onSecondaryContainer`
ink (washed out, especially in dark). Per the app's own v27q selection contract (SOLID
accent fill + on-accent content, never a translucent lerp), the active pill is now a SOLID
`secondary` fill with `onSecondary` ink — a defined amber capsule with dark ink in light,
bright amber with dark-brown ink in dark. The wide `CurioNavigationRail` uses the same
pair (selected icon/label `onSecondary`, indicator `secondary`) for coherence. Stale
v124 doc comment updated.

### 3 — The dim flash on page switches (CurioNavHost)
Root cause found in the v129 Scaffold removal: the M3 `Scaffold` paints
`containerColor = colorScheme.background` behind its content. After v129 replaced it
with a bare root `Box`, the root became TRANSPARENT — and the Activity's window
background is the hardcoded dark-navy bootstrap `curio_deep_plum` (#081B33, themes.xml).
Every NavHost page transition (tab crossfades + push/pop center-pop fades) leaves both
pages semi-transparent mid-fade, so the navy window flashed through — the "dim flash"
the user saw (it existed before too in dark mode, but the Scaffold's black background
hid it; in light mode it was a clear new regression).

Fix: the root Box paints `MaterialTheme.colorScheme.background` again
(`Modifier.background(...)` before the pet pointer tracker). Invisible in practice —
every page paints its own full-bleed background; it only shows during transitions and
wide gutters, restoring the pre-v129 look. LESSON (recorded in AGENTS.md v131 follow-up):
removing a Scaffold must replace its containerColor fill explicitly.

### Files touched
- `app/src/main/java/com/curio/app/navigation/CurioNavHost.kt` — root background (dim-flash fix)
- `app/src/main/java/com/curio/app/ui/components/CurioBottomNav.kt` — pill sizes + solid indicator (bar + rail)
- `app/src/main/java/com/curio/app/features/home/HomeScreen.kt` — clearance 92 → 100dp
- `app/src/main/java/com/curio/app/features/spin/SpinScreen.kt` — clearance 76 → 84dp
- `app/src/main/java/com/curio/app/features/cabinet/CabinetScreen.kt` — clearance 76 → 84dp
- `app/AGENTS.md` — v131 follow-up bullet
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — FIX bullets

Not done: no Gradle build here (env forbids it) — CI validates on push.
