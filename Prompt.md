# Prompt.md — Request Log

## Current Request (COMPLETED): Topic Reveal bottom tear / watermark / morph stability

**Date:** 2026-08-10

### What the user asked
Fix the Topic Reveal page bottom tear/placeholder so hiding the bottom navbar does not make the watermark or the shared hero morph animation shift downward.

### Root cause
`CurioNavHost` hid the actual bottom bar on Reveal, so Scaffold `innerPadding` only reserved the system navigation-bar inset there. Normal tab pages reserve the app bottom nav footprint plus the system nav inset. Padding only `CurioWatermarkBackdrop` in `TopicRevealScreen` did not stabilize the whole destination/shared-transition content bounds, so the watermark coordinate space and the shared hero target could still stretch downward.

### Changes made
- `CurioNavHost.kt` now detects Reveal routes and, on compact bottom-nav layouts, adds a navbar-height placeholder (`RevealBottomBarPlaceholderHeight = 80.dp`) to the NavHost content padding without rendering the actual bottom bar.
- `TopicRevealScreen.kt` lets the watermark backdrop fill the stabilized content bounds and paints the torn strip down through the reserved 80dp slot plus the system nav inset.
- `CurioRoutes.kt` and `app/AGENTS.md` were updated so future agents know Reveal is bottom-nav-adjacent for metadata/selection, but the actual bar is hidden and replaced by a same-height torn placeholder.
- `fastlane/metadata/android/en-US/changelogs/20260918.txt` notes the visual stability fix and stays under the 500-character store limit.

### Validation
- `node scripts/check_braces.js app/src/main/java/com/curio/app/navigation/CurioNavHost.kt app/src/main/java/com/curio/app/navigation/CurioRoutes.kt app/src/main/java/com/curio/app/features/reveal/TopicRevealScreen.kt` — OK.
- `git diff --check` — OK.
- `wc -c fastlane/metadata/android/en-US/changelogs/20260918.txt` — 352 chars.
- Code review subagent found no Compose layout or Kotlin compile blockers; suggested doc/comment cleanup and named placeholder constant, both addressed.
- Gradle compile/build/lint/test were not run because root AGENTS.md forbids local Gradle commands in this environment.
