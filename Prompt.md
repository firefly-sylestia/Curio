# Request — Pet home spawn, speech bubble redesign, ride-cloud scale

## User request
1. When the app opens, the pet is at the screen corner instead of its home — it should start from its home (confirmed: float beside its house on the Home screen).
2. Redesign the speech bubble shape (the old one is "bad").
3. Scale "the cloud" to the pet's size (confirmed: the little pixel ride-cloud the pet stands on while walking, which was 80×32dp — wider than the 72dp pet).

## Changes completed
- `CurioFloatingPet.kt`:
  - New settle-at-home effect: the moment the Home screen's "bed" landmark is measured, the pet SNAPS beside its house (floor-aligned, on the side away from the screen edge, facing it) instead of sitting at the bottom-right corner — no corner flash. One-shot per appearance via a `settledAtHome` remember flag (resets when the overlay leaves/re-enters composition, e.g. coming out of the house); skipped during the tour and while dragged. Added the `CurioRoutes` import.
  - Ride cloud scaled to the pet: `CLOUD_W` 80→56dp, `CLOUD_H = CLOUD_W * 0.4f` (22.4dp) so the 20×8 pixel grid keeps square cells (~¾ of the pet's width).
- `CurioPetCompanion.kt`: redesigned `PetSpeechBubble` — replaced the rotated-square diamond tail (`TailDiamond`, removed with the now-unused `rotate` import) with a soft curved Path-based tail (`CurvedBubbleTail`, quadratic beziers, rounded tip, 16×14dp Canvas) and squishier asymmetric corners (20dp with an 8dp corner on the tail side). Added `Canvas`/`fillMaxSize`/`Path` imports. Quests-screen call site unchanged.

## Validation
- `node scripts/check_braces.js` passed on both files.
- Code review confirmed compile-safety (imports balanced, Dp math valid, `hypot`/`Rect`/`mutableStateOf`/`delay` already imported) and led to switching the initial glide (which left the pet at the corner for the first frames) to a snap-to-home so the pet is never seen at the corner.
- Local Gradle compile/build/lint/test commands were not run because repository instructions forbid local Android builds; CI remains authoritative.

## Status
Committed and pushed: `51987cc feat: pet starts at its home, redesigned speech bubble, scaled ride cloud` (branch Alpha).
