# Current Request — nav pill polish (v184): calmer morph/collapse, bigger pill, more spacing, Changa One font

## Status: DONE — committed and pushed

## Request
1. "make the nav pill morph and collape animation even smoother and calmer. and give the inactive buttons a little more space. and use a new bond font for the tet of nav pill maybe this one, Changa One" (https://fonts.google.com/specimen/Changa+One)
2. "also make it a little wide like just a little heigh the pill"
3. "also just like it expands when i come back to home screen make it collapse when i go to other screen from home screen for smoother look."

## Changes (all in `app/src/main/java/com/curio/app/ui/components/CurioBottomNav.kt`)
- **Springs**: `PillWidth/Motion/Color/ExpandSpring` stiffness 400 → 240, damping stays 1.0 (critically damped — zero overshoot/bounce). All four specs identical → v162/v165 lockstep preserved. ~40% slower settle.
- **Collapse**: verified it already mirrors the expand — the outgoing pill's width shrinks + label slides back into the icon on the SAME springs as the incoming pill's growth (since v162; the v125 "instant label exit" was long gone). The 240 stiffness slows BOTH directions. Fixed the stale v125 function KDoc claiming the label exit is instant.
- **Size**: icon pills 60 → 64dp, expanded 128 → 136dp, height 48 → 52dp ("a little wide, a little high").
- **Spacing**: bar inner padding 7 → 8dp, pill gap 6 → 10dp (inactive buttons breathe).
- **Font**: bundled `changa_one_regular.ttf` (Changa One v1.003, OFL — verified TTF magic + name table) + `ChangaOneFontFamily` (single-entry, like PatrickHand — Changa One has no bold TTF, so labels use `FontWeight.Normal` to avoid fake-bold synthesis). Nav pill labels at 13sp (12sp geom Bold → 13sp Changa One keeps visual weight); rail labels match. License at `app/third_party/changa_one_OFL.txt` (NOT in res/font — typed dir rejects non-fonts).

## Files changed
- app/src/main/java/com/curio/app/ui/components/CurioBottomNav.kt (springs, dims, spacing, labels, imports, KDoc)
- app/src/main/java/com/curio/app/ui/theme/CurioTypography.kt (ChangaOneFontFamily)
- app/src/main/res/font/changa_one_regular.ttf (new)
- app/third_party/changa_one_OFL.txt (new — license)
- fastlane changelog (ADD bullet), app/AGENTS.md (v184 entry), Prompt.md

## Follow-ups
- CI validates the compile (new font resource + imports). Changa One is wide — watch that the expanded 136dp pill still fits "Shuffle"/"Cabinet" at 13sp on device; easy to tweak later.
