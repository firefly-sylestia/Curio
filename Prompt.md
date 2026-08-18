# Current Request — v204: PetStudio sp import fix + Save CTA tick removed, push everything

## Status: DONE (committed + PUSHED — user: "fix this too then push everything")

## Request (user, verbatim)
CI: "PetDesignerScreen.kt:1533:39 Unresolved reference 'sp'" + "fix this too then push everything. and also remove the tick from the save your entry button."

## What changed
1. **Compile fix** — the v201 PetStudio label change (`fontSize = 15.sp`) was missing `import androidx.compose.ui.unit.sp` in PetDesignerScreen.kt; added. CI caught it because v201–v203 were sitting unpushed.
2. **Save CTA tick removed** — SaveCaptureScreen's Save entry / Save changes button lost the leading `CurioIcons.Check` + Spacer; now text-only (matches Manage / Apply pills). `CurioIcon` still used at 12 sites.
3. **PUSHED everything** — v201 (ring/pill), v202 (constellation), v203 (back pill + drawer footer), v204 all went up together.

## Files
- `features/petdesigner/PetDesignerScreen.kt` (import)
- `features/capture/SaveCaptureScreen.kt` (tick removal)
- Docs: `app/AGENTS.md` (v204), changelog FIX bullet, this Prompt.md.

---

# Previous — v203: drawer footer pinned to the bottom + visible credits

## Status: DONE (committed, NOT pushed — user: "dont push this")

## Request (user, verbatim)
"the footer is still sitting like floating above the buttom part of the draer page. and also the v1.10 made with curiocity text sint visible"

## What changed
1. **Footer floating** — `DrawerFooter` was the LAST LAZYCOLUMN ITEM in the drawer sheet, so it floated above the bottom whenever the list content was shorter than the sheet. Fix: removed the `item("footer")`, added a pinned `Box(Modifier.align(Alignment.BottomCenter))` at the drawer-sheet level (drawn after the list so rows scroll under its fade), and set the list's bottom contentPadding to `DrawerFooterHeight` (150dp, a new shared constant) so the last row never hides behind the footer.
2. **Credits invisible** — the fixed khaki `#7E6E50` vanished on the near-black surface in dark mode. `DrawerFooter` ink is now theme-aware: warm parchment `#C9BC9D` in dark, khaki in light.

## Files
- `features/home/HomeScreen.kt` (HomeDrawerContent + DrawerFooter)
- Docs: `app/AGENTS.md` (v203), changelog FIX bullet, this Prompt.md.

## NOTE
Committed locally only — NOT pushed (user: "dont push this"; the v203 back-pill commit is also queued unpushed).

---

# Previous — v203: remove the back pill on Your Curiosity page

## Status: DONE (committed, NOT pushed — user: "dont push this")

## Request (user, verbatim)
"fix the back button in your cusriocity page. maybe jut remove the back button. dont push this"

## What changed
- The stats page is a plain NavHost destination (`composable(CurioRoutes.STATS)`), so the system back gesture/button already pops it. The custom cream circle pill in `StatsSkyHeader` (Surface + ArrowBack, calling `popBackStack()`) was a redundant second path.
- Removed: the pill, the `onBack` param from `StatsSkyHeader`, the call-site arg, and the `horizontalArrangement` (only one child left). `CircleShape` import stays (used by the range pill). Brace-balanced.
- Confirmed via ask_user: "Remove it entirely".

## Files
- `features/stats/StatsScreen.kt`
- Docs: `app/AGENTS.md` (v203), changelog FIX bullet, this Prompt.md.

## NOTE
Committed locally only — NOT pushed (explicit user instruction).

---

# Previous — v202: constellation redrawn as a human brain

## Status: DONE (committed + pushed, 7aa4a25)

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
