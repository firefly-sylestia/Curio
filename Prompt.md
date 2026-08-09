# Request — Stabilize tour Explore navigation and reduce topic-loading OOM risk

## User request
Fix the reported Android OutOfMemoryError and the tour Explore navigation regression only. Do not change the removed neural model/ONNX work.

## Analysis and plan
- Keep the tour Explore guide removed; Explore taps during an active tour must dismiss the tour without opening a dialog, browser, capture task, or stale next route.
- Remove the confirmed Topic Reveal bottom Scaffold slot and its NavHost plumbing.
- Avoid eager catalog parsing at splash, promo mode, and Topic Database startup.
- Prevent duplicate concurrent topic parses and keep wildcard catalog construction on demand.

## Completed changes
- Removed the tour `google-youtube` step and made Explore, silent Explore, and Already taps safely end an active tour without side effects.
- Removed the Topic Reveal bottom-slot callbacks, wash strip, hidden reservation, and related NavHost plumbing.
- Removed splash-time `preloadAll()`.
- Changed topic loading to serialize cache miss parsing, excluded wildcard from `preloadAll()`, and added lightweight canonical JSON counting for promo artwork.
- Changed promo mode, Topic Database, and sample-entry loading to load only required categories.
- Updated stale Topic Reveal/MainActivity documentation.

## Validation
- Gradle compile/build/lint/test commands are forbidden in this workspace; CI is the compilation source of truth.
- Run `git diff --check`, `node scripts/check_braces.js`, and focused symbol/import audits before commit.
- Local Gradle verification is intentionally not run per the repository Android environment rule.

## Follow-up
- CI should verify Kotlin/Compose compilation and the release memory behavior on the reported device class.

---

# Previous request — Remove neural model work and fix overlapping pet dialogue

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
