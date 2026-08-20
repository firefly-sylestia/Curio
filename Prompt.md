# Prompt — Current Request

## Request
"instead of glow colorful dots use the star as the dots itself. and dont increase the size, and use the north star constellation with the question and the pole star. make the backgroud colorfully beautiful galaxy looking, and in light mode it should keep its color and just in the edges it should blend a little"

## Status: IN PROGRESS

## What's changed
- **v211** — CurioConstellation.kt: full rewrite from Corvus to **Ursa Minor** (North Star / Little Dipper) constellation with **Polaris** as the guiding pole star.
- Stars are now drawn as **4-pointed star shapes** (diamond-star glyphs) instead of circles.
- Background: **colorful galaxy aesthetic** — layered nebula washes (purple, teal-blue, magenta, gold dust near Polaris).
- **Light mode**: galaxy palette stays vibrant but **edges fade** to meet the page background (top/bottom/left/right gradient blends).
- **Dark mode**: full-bleed deep space galaxy.
- Anchor stars: Polaris is larger and warmer (golden), the other 6 Ursa Minor stars are cooler (blue-white).
- Lane stars remain category-accent colored, sized by knowledge (sqrt ramp, capped at 7dp).
- Background scatter stars are now tiny star glyphs, not circles.

## Remaining work
- Commit + push the constellation redesign
