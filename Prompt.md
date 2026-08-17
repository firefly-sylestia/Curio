# Prompt.md — Request log

## Current request — IN PROGRESS (part 1 done): reveal morph pill unification + year pill

The user asked (three messages, clarifying as they went): (1) make the topic reveal open
smooth by morphing the pill properly and unifying its style/animation; (2) show the year
as a pill instead of inside the shuffle-card title; (3) replace the reveal's "Watch for /
Listen for" with "Director / Author" and vice versa — clarified: the swap is reveal-only,
keeping the byline at the top corner in BOTH ticket and reveal for a smooth morph.

### Part 1 — DONE (v141)
- **`CurioTopic.titleAndYearQualifier()`** — splits a trailing " (…)" / " — …" qualifier:
  "Moby-Dick (1851)" → ("Moby-Dick", "1851"); "The Odyssey (c. 8th century BCE)" →
  ("The Odyssey", "c. 8th century BCE").
- **Spin ticket (SpinScreen)**: the title renders the BASE name (no year); the top-left
  corner is now a pill ROW — byline ("Director · Nolan") + year pill (Schedule icon +
  "1851"), same recipe as before (ink@18%, shape 50, labelMedium bold, h12/v6).
- **Reveal hero (TopicRevealScreen HeroCard)**: the top-left action badge was REPLACED by
  the same byline + year pill row (identical recipe — dropped the reveal's old Person
  icon variant); the title renders the base name; the action pill ("Watch for ~25 min")
  moved DOWN to the bottom pill row (left slot, weight(1f, fill=false)) next to the
  subtype. Progress badge at top-right unchanged; the v135 decade tag chip untouched.
- Net effect: the pill row and title are now IDENTICAL at the same position on both ends
  of the shared-element morph → the pills stay put while the card grows.

### Remaining parts (from the earlier message, NOT yet started)
- Apply the same pill style in the Pet Designer.
- Make "Manage category" in the category-explore option match the Scaffold-removed
  full-bleed treatment.
- Wire the first-run "Pick a lane" to the Spin screen's category picker.

### Verification
No Gradle build in this environment (project rule — CI validates on push). On-device:
open a year-qualified topic (e.g. Moby-Dick) from the deck — title should read
"Moby-Dick" with "1851" pill top-left on ticket AND reveal, byline pill at top-left in
both, action pill bottom-right area of the hero.
