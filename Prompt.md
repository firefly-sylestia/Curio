# Request — Fix failures from the previous CI run

## User request
- Check the previous CI run and fix its failure.

## Context
- The prior tutorial-removal change was pushed as `bb9c27c`.
- Failed CI run: `31315836634` (`:app:compileDebugKotlin` and `:app:compileReleaseKotlin`).

## Completed
- Confirmed the exact compiler errors from CI:
  - `HomeScreen.kt:219` — obsolete `bedSize` named argument.
  - `CurioPetCompanion.kt:180` — obsolete `bedSize` named argument.
  - `PetDesignerScreen.kt:4447` — unresolved `modifier` reference inside `PixelGrid`.
- Replaced the renamed home-scene arguments with `homeSize`, preserving the existing `52.dp` and `74.dp` layouts.
- Added `modifier: Modifier = Modifier` to `PixelGrid`, matching its existing modifier usage and keeping all current call sites compatible.

## Validation
- `node scripts/check_braces.js` passed: 123 files checked.
- `git diff --check` passed.
- No `bedSize` references remain in app source.
- Final blocker-only code review found no compile, import, or behavior blockers.
- Local Gradle compile/build/lint/test commands were not run per repository rules; CI remains the Android/Kotlin compile gate.

## Follow-up
- Commit and push the CI fix, then monitor the new GitHub Actions run for any remaining errors.

## Prior completed request
The tutorial implementation was removed while preserving the separate “The Tour” Quest chain, progress, XP, and badges. See the preceding commit `bb9c27c` and its history for that completed work.
