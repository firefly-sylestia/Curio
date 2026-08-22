# Prompt — Current Request

## Request
Drawer constellation: "more material style, fix in light mode as not visible properly, more larger, move around, tapping the star doesnt properly centres it, not visually appealing — fix it properly and keep it material style."

## Status: COMPLETE

## Root causes found
- Light-mode invisibility: stars were near-white (`0xFFeef5fa`) and lines pale blue (`0xFFc7d9e8`) on the cream drawer surface.
- Centering: the auto-zoom effect used a hardcoded ±80px guess and lived outside the layout scope (no real size); the popover also ignored the zoom transform.

## Changes
- `CurioConstellation.kt`: new `materialInk` param — theme-role lines (`onSurfaceVariant`, thicker), explored stars in `primary` @ 1.45×, dim unexplored dots; 7s sine twinkle on all stars + expanding pulse ring on the selected star; auto-center `LaunchedEffect` relocated inside BoxWithConstraints using real wPx/hPx with exact pivot math (`t = -2·(p−c)`); popover placement applies the layer transform so the card tracks the visual star. Stats page untouched (materialInk=false).
- `HomeScreen.kt` DrawerCuriosityMap: passes materialInk, height 280→320dp.
- Docs: AGENTS.md v224 note, changelog bullet.

## Verification
- Delimiter balance OK on both files; single LaunchedEffect(selected…) confirmed; Stats path unaffected (defaults).
