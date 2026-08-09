# Request — Polish Spin landing motion and stabilize pet Faces/reveal layout

## User request
Smooth the bottom navigation color transition, keep the premium Spin landing treatment but remove the circular shockwave animation and its setting, fade deck peek cards after the reel midpoint, improve shuffle/dot glow visibility, fix the Topic Reveal morph shift after removing its bottom placeholder, and address the Faces editor crash/OOM when opening evolved 64×64 pets.

## Analysis and plan
- Remove only the Spin premium shockwave ring and its user-facing experiment preference; preserve the other landing layers.
- Thread reel progress through the deck so peek cards fade after 50% of the shuffle.
- Keep the existing nav and dot/glow behavior while smoothing the nav container color transition.
- Preserve the removed Topic Reveal bottom UI while restoring its former invisible transition geometry locally to the Reveal route.
- Keep the interactive face-painting board at the design’s full resolution, but downsample decorative mood previews to bound memory on 256MB devices.

## Completed changes
- Removed the Spin shockwave ring rendering path and removed its obsolete AppPreferences/Experiments setting plumbing.
- Added `shuffleProgress` through `SpinDeckSection` → `Carousel` → `PeekCard`; peek cards remain visible through the first half and fade out during the second half.
- Smoothed `CurioBottomBar`/rail container color changes with a 420ms `animateColorAsState` transition.
- Kept the Topic Reveal bottom action UI and placeholder removed; added an invisible 80dp Reveal-only clearance in `CurioNavHost` so the shared hero keeps the same morph viewport geometry without showing a bottom scaffold.
- Converted the Faces editor to a Canvas path and downsampled non-interactive mood picker previews to at most 16×16. The active painting board remains full-resolution and saved designs are unchanged.
- Cleaned Spin parameter formatting after threading `shuffleProgress`.

## Validation
- `node scripts/check_braces.js` passed: 125 files checked.
- `git diff --check` passed.
- Focused symbol audit found no stale Spin ring preference/rendering references. The remaining `ringProgress` in `CurioConfetti.kt` belongs to the unrelated confetti effect and was intentionally left unchanged.
- Gradle compile/build/lint/test commands were not run because local Android builds are forbidden by the repository contract; CI remains the compile source of truth.
- Code review reported no critical blocker in the current patch.

## Status
Changes are intentionally still uncommitted. Per the user’s instruction, stop here and ask for approval before commit/push.
