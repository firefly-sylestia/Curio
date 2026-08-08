# Request — v8.26: pet polish (cloud, eyes, throw momentum, bubble, dialog)

## What the user asked

1. Make the ride cloud even smaller.
2. Change "I'm reading this." to something else.
3. Change the orangish excited eyes to a more natural, different look.
4. Make throwing the pet dynamic: it should keep a little momentum after a
   fling (not too much).
5. The dialog/bubble change isn't smooth (pops) — make it smooth.
6. Reactions (dizzy, etc.) should last ~2-3 seconds.
7. Export the pet's dialog as a txt so the user can improve the copy later —
   keep a note of what each line is FOR so it's easy to edit.

## Analysis

- **Cloud:** `CLOUD_W/H` were 96×42dp with a 16-col pixel grid whose 20-char
  rows got clipped on the right (lopsided). Shrunk to 80×32dp, grid widened
  to 20 so the full art renders, and it's tucked under the feet (y offset
  0.66 → 0.72 of pet height).
- **Eyes:** the EXCITED `EyeStyle.STAR` used `gold` (0xFFFFD97D) which reads
  orangish on the cream body. Swapped to a natural warm brown `starEye`
  (0xFF7A4E2E — the ink family, one step lighter); sparkles/antenna keep gold.
- **Throw momentum:** `detectDragGestures` gives no velocity, so the
  pointerInput now tracks a rolling blended px/s velocity; on release, if
  the speed clears 350dp/s it launches a short friction glide (capped at
  620dp/s, decay 8/s, stops at 26dp/s ≈ max ~75dp slide) — "a little, not
  too much". Glide job is cancelled on drag start/cancel.
- **Smooth bubble:** new `bubbleAnim` Animatable — bubble fades + rises in
  (180ms), holds ~2.3s, fades out (180ms), then clears. The bubble Box got a
  graphicsLayer alpha + 8dp rise.
- **Reaction timing:** bubble hold 1500 → 2300ms; dizzy recovery 1600 →
  2500ms (2-3s reactions).
- **Line swap:** curious-poke list: "I'm reading this." → "Let me read this!".
- **Dialog txt:** new `docs/PET_DIALOGUE.txt` — every pet + tour line grouped
  by purpose (mood bubbles, event reactions, spin cheer, touch tiers,
  landmark pokes, jig, dizzy, drawer, morning greeting, home drop, check-in
  dialog, tour steps, onboarding ask) with code locations and editing notes.

## Changes

| File | Change |
| --- | --- |
| `ui/pet/CurioFloatingPet.kt` | Smaller cloud (80×32, 20-col grid, tucked under feet); animated bubble in/out; throw-momentum glide with velocity tracking; bubble hold 2.3s; dizzy recovery 2.5s. |
| `ui/pet/CurioPetSprite.kt` | STAR (excited) eyes now natural warm brown instead of gold. |
| `data/CurioPet.kt` | "I'm reading this." → "Let me read this!". |
| `docs/PET_DIALOGUE.txt` | NEW — full dialog reference with purpose labels + code locations. |
| `fastlane/metadata/android/en-US/changelogs/20260813.txt` | NEW — release notes. |

## Validation

- Brace balance ALL OK (3 edited files), `git diff --check` clean, diffs
  visually reviewed.
- No compile/build commands run (environment has no Android SDK — CI gates
  compilation on push per root AGENTS.md).

## Completion summary

v8.26 shipped: smaller tucked cloud, natural brown excited eyes, a little
throw momentum after flings, smooth fade/rise speech bubbles, 2.3s bubble
hold + 2.5s dizzy recovery, "Let me read this!" line, and a full
docs/PET_DIALOGUE.txt reference for future copy edits.
