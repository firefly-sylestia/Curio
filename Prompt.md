# Request — Addictive shuffle choreography + correct swipe sequence (v8.40)

## Completed

Focused only on `SpinScreen.kt` animation and deck interaction. No z-index,
layout, card ordering, colors, shapes, or visual card design were changed.

### Shuffle choreography

- The hero now has one clean heartbeat per reel tick: a tiny press-down,
  lift/anticipation, then a controlled settle. The prior two pulse springs
  restarted 110ms apart and overlapped with the hero content reel, producing
  simultaneous, strange motion.
- The hero topic content handoff starts 90ms after the heartbeat begins, so
  the card motion leads and the new topic arrives as a readable release.
- Peek cards now animate strictly one at a time in this order:
  **top outer (`-2`) → top inner (`-1`) → bottom inner (`+1`) → bottom outer
  (`+2`)**. Each card has its own delay; no two peek cards share a wave.
- The per-card timings remain below the 340ms fastest shuffle tick so the
  cascade completes before the next reel tick.

### Swipe sequence

- The existing user-selected direction is preserved: **left swipe → nearest
  visible bottom peek (`+1`)**, right swipe → nearest visible top peek (`-1`).
- The shuffle loop no longer resets `cycleIndex` from a separate tick counter
  starting at hand index 1. It now advances the current hand position, so
  manual swipes and subsequent shuffle ticks stay on the same visible topic
  sequence instead of jumping to a top topic.

## Files changed

- `app/src/main/java/com/curio/app/features/spin/SpinScreen.kt`
- `app/build.gradle.kts` → versionCode 20260830
- `fastlane/metadata/android/en-US/changelogs/20260830.txt`
- `app/AGENTS.md` motion contract note retained; no new ownership boundary
- `Prompt.md`

## Validation

- SpinScreen delimiter balance: BALANCED.
- `git diff --check`: clean.
- Only SpinScreen was changed before release metadata.
- Protected-scope diff check found no z-index/layout/design changes.
- Final code review found no concrete animation, sequencing, or compile blocker.
- No Gradle build was run because repository DOX rules forbid local Android
  builds; CI on push is the compile gate.
