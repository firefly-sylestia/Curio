# Request — Remove neural model work and fix overlapping pet dialogue

## User request
Remove the neural pet model/training integration and delete the local model artifacts. Keep the existing Curio pet and tour behavior, then fix the pet showing one dialogue immediately followed by another.

## Completed changes
- Reverted the neural brain integration commits, restoring the pre-model app state.
- Deleted the untracked local model/training directories and model ZIP.
- Added a single-bubble reaction queue in `CurioFloatingPet.kt`.
- Reactions now wait their full animation/reading window before the next queued line starts, instead of replacing the currently visible line and restarting the bubble effect.
- Tour dialogue remains independent and continues to take precedence over pet reactions.

## Validation
- `git diff --check` passed.
- `node scripts/check_braces.js` passed.
- Reviewed the remaining `reaction =` assignments: only the queue intake and queue lifecycle use them.
- Gradle was not run locally because the repository forbids local Android builds; CI remains the source of truth.

## Follow-up
If CI reports a Kotlin-specific issue, address it in a focused follow-up without reintroducing the removed neural model work.
