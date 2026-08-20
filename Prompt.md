# Prompt — Current Request

## Request
"whatever i dont like the brain constellation. lets use a real constellation map that symbolize curiosity. research and tell me. and we will be giving it the space style look properly blended and beautiful and not those messes and bright glows. fully redesign"

## Status: DONE ✅

## What changed (v210)

Replaced the entire brain neural web constellation with the real **Corvus (The Crow)** star pattern — Apollo placed the crow in the sky because its curiosity led it to seek forbidden knowledge.

### Corvus star positions (real astronomical coordinates, normalized)
- **Gienah** (ε Corvi) — mag 2.65, brightest — upper-left anchor
- **Kraz** (β Corvi) — mag 2.65 — upper-right anchor
- **Algorab** (δ Corvi) — mag 2.95 — lower-left anchor
- **Minkar** (ν Corvi) — mag 3.00 — lower-right anchor

These form the constellation's characteristic **quadrilateral** (the "sail" shape).

### Space aesthetic
- **Background**: deep void with faint radial nebula gradient (purple wash in dark mode, lavender in light)
- **Constellation lines**: thin gossamer links between the four anchor stars (not a dense mesh)
- **Stars**: small bright points with soft halos, sized by knowledge (sqrt ramp, capped at 7dp)
- **Background stars**: 40 dim scattered points for depth
- **No brain silhouette, no filler dots, no neural web, no garish glows**

### Removed
- `BRAIN_SILHOUETTE` polygon and `pointInBrain()` ray-casting
- `randomInBrain()` rejection sampling
- `brainFillerDots()` decorative fillers
- `drawBrainOutline()` smooth curve renderer
- `drawCurvedLink()` quadratic bezier links (replaced with straight gossamer lines)
- `Path` and `Stroke` imports (no longer needed)

### Files touched
- `CurioConstellation.kt` — complete rewrite (same API, no caller changes needed)

## Remaining work
None — this request is complete.
