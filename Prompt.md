# Prompt — Current Request

## Request
Drawer top slot rework (Android app only):
1. Keep the drawer constellation as PATTERN ONLY — no painted background (just lines + stars on the drawer surface).
2. Stats page constellation stays EXACTLY as-is (full deep-space sky).
3. Drawer constellation OFF by default, behind an Experiments toggle.
4. Default drawer shows a small unique Material-style stat screen instead.

## Status: COMPLETE

## Changes
- `ui/components/CurioConstellation.kt`: new `plainBackground: Boolean = false` param — when true, skips the opaque page fill + nebula/starfield sky, drawing only constellation lines + stars. Stats page call unchanged (default false).
- `data/AppPreferences.kt`: `drawerConstellationState` (default OFF), `KEY_DRAWER_CONSTELLATION`, getter/setter, seeded in initThemeMode.
- `features/settings/ExperimentsScreen.kt`: "Drawer constellation" toggle in the Constellation section.
- `features/home/HomeScreen.kt`: drawer's curiosity-map item gated on the toggle (ON → `DrawerCuriosityMap` with `plainBackground = true`; OFF → new `DrawerMaterialStatStrip` — small tonal M3 card, "YOUR CURIOSITY" caption, three panes: day streak · level · saved, tap → STATS).

## Verification
- Delimiter-balance check passed on all 4 touched files (Gradle builds are forbidden here; CI validates compilation on push).

## Notes / gotchas
- The str_replace tool failed to match multi-line blocks in this session — edits to HomeScreen.kt / AGENTS.md / changelog were applied via a verified Python script instead (each replacement asserted exactly one match).
