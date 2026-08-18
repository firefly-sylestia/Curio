# Current Request — Drawer constellation as a brain neural web + floating tap popover

## Status: DONE (committed + pushed to Alpha)

## Request (user, paraphrased)
"in drawer we have your constellation right but its random? isnt it. and
it doesnt show real data yet. but i want to draw the costellation pattern
as a brain neural connection. and when i tap the dot it shows me the info
belo but i want that to sho as a floating small thing and also less data.
and also in future i will be replacing the category with real knowledge
based things just like brain knowlegde you get it right?"

## Clarified via ask_user (user's answers)
1. **Nodes**: explored-only (keep the neuron dots) — and confirmed the
   future plan to replace explored-lane neurons with knowledge-based nodes.
2. **Popover**: name + saved count (later replaced by name + something) —
   and if the brain looks squished, extend the drawer a little to the right.

## Facts established
- The drawer constellation was ALREADY fed by real passport data (explored
  lanes = explores/saves > 0, size = saves, glow = recent). The
  "random/squished" feel came from the layout: the old arc scatter put
  every star in a flat bottom band of the canvas.

## What changed (v191)
- **Brain neural web** (`ui/components/CurioConstellation.kt`, shared with
  Stats): neurons now fill two hemisphere ellipse lobes (brain silhouette
  with the fissure gap); links are curved quadratic beziers — each neuron
  → 2 nearest neighbours + nearest other-hemisphere neuron (corpus
  callosum bridges); the gold fissure is a soft centre curve. Data + tap
  selection unchanged.
- **Floating popover**: new `popoverContent` slot on CurioConstellation —
  a small floating card anchored to the selected neuron (above, or below
  near the top), clamped inside the canvas, tap-to-dismiss. The DRAWER
  passes a compact name + "N saved" chip and its richer below-panel
  (4 stat chips + last-explored line + `DrawerMapStat`) is deleted. Stats
  page passes null → keeps its own panel.
- **Drawer width**: `ModalDrawerSheet` 320 → 336dp (the user's
  "extend the drawer a little more to the right" contingency).
- **Future-proofing**: the component reads only an id list + count maps —
  swapping category lanes for knowledge nodes later is caller-side only.

## Docs
- `app/AGENTS.md` — v191 entry (inserted before v190).
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — 2 FIX bullets.
- This Prompt.md.

## Verification
- Static only (no Gradle here — CI validates on push). Checked that all
  imports in HomeScreen.kt that were candidates for removal
  (AnimatedVisibility/expandVertically/shrinkVertically/fadeIn/fadeOut/
  formatElapsed) are still used elsewhere; `DrawerMapStat` deleted.
