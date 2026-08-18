# Current Request — v202: constellation redrawn as a human brain

## Status: DONE (committed, push follows)

## Request (user, verbatim)
"also the mesh is too much and why it doesnt look like a brain like the human brain design it should follow that and the dots should be random not some in left and some in right."

## What changed (v202) — `ui/components/CurioConstellation.kt`
The old design: two side-by-side ellipses (generic blobs), filler dots in rigid per-lobe rings, ~114 links (2-nearest + a cross-bridge per dot), gold midline fissure.

1. **Human-brain silhouette** — `BRAIN_SILHOUETTE`, the classic anatomy side profile (frontal pole → cerebrum dome → occipital pole → cerebellum bump), drawn as a faint outline (`drawBrainOutline`, quadratic curves through midpoints) so the shape reads as a brain instantly.
2. **Random dots everywhere** — all 16 decorative fillers + every real lane neuron are scattered RANDOMLY inside the silhouette via seeded rejection sampling (`randomInBrain` / `pointInBrain`). The per-lobe rings and left/right flag are gone. Real neurons keep per-id deterministic spots, stay tappable, keep saved-count sizing + recent glow.
3. **Light web** — nearest-neighbour graph (one synapse per dot): 13–32 links vs the old ~114 (~70% cut). Gold fissure removed with the two-lobe layout.

## Validation
- Silhouette x-monotone (no self-intersection), area fill 77%, rejection acceptance ~76%.
- Link counts simulated for 3/8/16/30 explored lanes → 13/16/23/32 links.
- Brace-balanced; unused trig imports (PI/cos/sin) removed.

## Files
- `ui/components/CurioConstellation.kt` — the redesign.
- Docs: `app/AGENTS.md` (v202), changelog FIX bullet, this Prompt.md.

## Next steps (queued)
- (user may want to see it on-device before the 1000+ animated-movies top-up continues)
