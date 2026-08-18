# Current Request — Cut lines shorter + right-shifted; hole rings redrawn as the spiral-coil SVG

## Status: DONE (committed + pushed to Alpha)

## Request (user, paraphrased)
"now we have two cut lines lets improve it even more. make it little more
shorter and more to the right of the header text. and the hole rings. so i
think the problem is stamped pin holes so as it creates holes which is see
through the 3d ring doesnt show over it. lemme share the rings which you
can adjust and put above the holes to give it a good look and tune the
ring colors on your own. the ring itself isnt perfect its too much rounded
and the view is also wrong so youve to fix the svg rings" + pasted SVG
reference (3 spiral coils, 150×420).

## What changed (v194)
- **Cut lines** (`ui/components/PaperTitleLines.kt`): the two hand-drawn
  underlines now start ~a quarter in from the title's left edge and span
  only the right ~70% of the line — top stroke 0.22→0.90w, bottom
  0.26→0.94w (was 0.02/0.06 → 0.90/0.96). Shorter + more to the right of
  the header text, same pen-sag shapes and -2° tilt.
- **Hole rings** (`ui/components/PaperStatCard.kt`, the default "coil"
  style): redrawn as the user's reference SVG — a FORESHORTENED
  spiral-notebook wire (73:51 aspect) looping up the left, over the top,
  down the right, curling in at the bottom. Drawn OVER the shaded hole
  interior so the punched hole shows through the coil's inner opening
  ("put above the holes"). Three SVG passes:
  1. dark depth stroke behind (18px pass) — #101B27 light / #22282F dark;
  2. metal tube gradient on top (8 stops tuned from the SVG palette, cool
     polished steel; dark mode flips to light steel);
  3. white specular along the upper-left (3px pass, 0.75 light / 0.60
     dark).
  Coil outer loop = holeR × 4.2 (~2.1× the hole diameter). Old arc-based
  coil + CoilBackDark deleted; "split" / "oblique" styles untouched.

## Docs
- `app/AGENTS.md` — v194 entry.
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — 2 FIX bullets.
- This Prompt.md.

## Verification
- Static only (no Gradle here — CI validates on push). The coil paths are
  transcribed 1:1 from the SVG (normalized to the 73×51 box, verified
  point-by-point); the specular line likewise. No leftover references to
  CoilBackDark; all imports (Path/Brush/Stroke/StrokeCap) already present.
