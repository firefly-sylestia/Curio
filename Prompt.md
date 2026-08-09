# Request — Reimagine the pet experience

## User request
- Try something new for the pet experience because the pet's interactions feel repetitive and users may get bored.
- Add many more animations, reactions, fun/creative interactions, and more viewing angles.
- User approved: always-on rollout; screen-aware routines; personality routines; toy/play moments; a much larger animation library; multiple viewpoints; authored-pixel-view direction.

## Implemented — Pet Life runtime foundation
- Added `app/src/main/java/com/curio/app/data/PetLife.kt` with:
  - `PetViewAngle` values: front, three-quarter, side, back, looking up/down, and curled.
  - Screen-aware routine catalogs for Home, Spin, Quests, Reveal, Capture, Cabinet, Profile, and fallback routes.
  - Personality-weighted selection for Cuddly, Bouncy, Explorer, and Sparky personas.
  - Recent routine-id exclusion window to prevent immediate repetition.
- Extended `PetAnimationFrame` with backward-compatible `view` metadata.
- Added `v=` animation-frame serialization/parsing; old saved animation text preserves built-in frame viewpoints when the field is absent.
- Added nine built-in Pet Life animations: look around, wave, stretch, side peek, stumble, look up, turn around, victory, and inspect.
- Wired `CurioFloatingPet` to choose and play Pet Life routines around current-screen landmarks, drawers, edge peeks, and autonomous play moments.
- Custom actions cancel ambient Pet Life routines so the two scenes do not overlap.
- Added recent-routine memory and routine completion/reset handling.
- Wired runtime and timeline previews through `viewAngle`.
- Added visible BACK-view cues: the front face/mouth/blush are suppressed and a spine/scarf-nape silhouette is drawn.
- Preserved existing saved designs and old animation call sites through defaults.

## Validation
- `node scripts/check_braces.js` passed: 124 files checked.
- `git diff --check` passed.
- Audited all `PetAnimationFrame` constructors and `view` call sites.
- Final blocker-only review found no Kotlin compile blockers.
- Local Gradle compile/build/lint/test commands were not run per repository rules; CI remains the Android/Kotlin compile gate.

## Known continuation
- The runtime now uses authored viewpoint metadata and a distinct BACK treatment, but the Pet Studio does not yet expose a dedicated viewpoint-painting editor for authoring true side/back pixel grids. Continue with that editor pass after CI confirms this runtime foundation.
