# Current Request — Brain mesh fillers + nav pill full collapse on Topic Reveal

## Status: DONE (committed + pushed to Alpha)

## Request (user, verbatim)
"the neruons dot doesnt create the brain mesh. and i told you to add extra dots for decoration and completion of the neuron mesh, and the home nav doesnt collapse fully in topic reveal screen it stays for too long, neither it collapse"

## What changed (v195)

### 1. Brain mesh filler neurons (`ui/components/CurioConstellation.kt`)
- User asked (earlier, via ask_user) for explored-only interactive neurons PLUS decorative extras to complete the mesh. With few explored lanes the web read as scattered dots.
- NEW `brainFillerDots()`: deterministic fixed-seed ring layout per hemisphere lobe (radial 0.35 → 0.89, 5/7/9 dots per ring) filling both lobes on the same lobe-ellipse math as the real neurons.
- Fillers join the SAME link web — nearest-2 synapses + inter-hemispheric bridges computed over ALL dots (real nodes still split left/right by index) — so the mesh outlines the whole brain even at zero explored lanes.
- Fillers draw as small neutral steel dots UNDER the real neurons (dim, no accent/glow/white core); NOT tappable; the real explored neurons stay the only interactive data (popover untouched).

### 2. Nav pill full collapse on the Topic Reveal (`ui/components/CurioBottomNav.kt` + `navigation/CurioNavHost.kt`)
- ROOT CAUSE: v193 kept the bar composed 500ms after leaving the tab set, but `CurioFloatingNavBar`'s internal `selectedRoute` maps the reveal route → SPIN ("keep Shuffle selected"). So Home → reveal made the SPIN pill POP OPEN during the hold, and the bar vanished with a pill stuck expanded.
- FIX: new `collapsing` param on `CurioFloatingNavBar`; NavHost passes `collapsing = !showBottomBar`. While off the tab set, `selectedRoute = null` → every pill glides closed, bar unmounts with nothing expanded.
- Hold shortened 500 → 380ms (the 240-stiffness critically-damped collapse spring's settle time) so the bar doesn't linger.

## Docs
- `app/AGENTS.md` — v195 entry.
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — 2 FIX bullets.
- This Prompt.md.

## Verification
- Static only (no Gradle here — CI validates on push). `CurioFloatingNavBar` has a single call site (CurioNavHost) so the new param can't break other callers; `curioNavActiveAccent(null)` already handles null. Fillers are remembered once (fixed seed) and share the existing `sqDist`/`norm`/`drawCurvedLink` helpers — no new imports needed.
