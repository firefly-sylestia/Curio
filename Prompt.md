# Request — Track Tour speech bubble to exact landmarks

## User request
- Make the Tour speech bubble track the exact landmark position instead of using the pet's current bubble placement.
- Continue and fix the CI failure.

## Implemented
- Tour bubbles in `CurioFloatingPet` now anchor to the registered `PetLandmarks.Landmark.bounds`.
- Landmark bounds are published in window coordinates; the floating overlay converts them into its own local coordinate space before positioning the bubble.
- The bubble is horizontally centered on the landmark and placed above it when there is room, otherwise below it, with screen clamping.
- Actual measured tour bubble dimensions are used for placement; measurement resets when the tour step changes.
- Overlay-origin changes restart pet-to-landmark movement so rotation/inset/layout changes do not leave stale coordinates.
- Normal reaction bubbles retain their existing pet-relative placement and sizing.

## CI/static validation
- `git diff --check` passed.
- `node scripts/check_braces.js` passed (125 files).
- Local Gradle/Android compilation was not run because root DOX explicitly forbids local Gradle builds; CI remains the source of truth.
- The current branch had no checked-in CI log artifact; the observed failure context was audited through current source/imports.

## Notes
- The unrelated untracked `pet_brain_emotional_v3.zip` and the previously created untracked Python training package remain untouched and are not part of this UI fix.
