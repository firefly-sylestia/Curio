# Prompt Log — current request

## Request (2026-09-06, active → v380 + v381 pushed, Signature-cover open)

Share-card editor refinement cycle continues.

- ✅ v380 (`8f8bef52`): hand-placed title owns its spot — drag the title
  anywhere incl. over/inside the quick-fact box; no bounce-back. New
  persisted `ShareCardMove.titlePlaced`; drag-end folds any prior auto
  lift into titleDy (no snap); lift effect skips hand-placed titles but
  still rescues never-dragged titles when a slider-grown fact reaches
  them. Reset layout clears the flag.
- ✅ v381 (THIS COMMIT): sparkle-pill refinements + Collage dark-tone pass.
  - Pill 9:16 flip: longer box + BIGGER text (factScale 1.18, height ≈
    tall budget ×1.4 clamped 1.6–3.2) so the tall canvas reads larger.
  - AutoLayoutPill opaque + hairline ring, shadow gone (translucent +
    elevation = the "solid fill glitch" halo).
  - Collage: dark-tone branch — field/band/pill lerp accent/accentDark
    toward black then mute so layers darken top→bottom on Midnight/Ember
    (light accent no longer floods the bottom, white fact text readable);
    tornEdge pulled toward accent so the seam reads on near-black paper;
    polaroid caption fixed warm-dark ink; bottom band/footer blended via
    a feathered gradient zone (no hard wavy cuts).
- Open: **Signature designs with a book/album cover look bad** ("default
  view of some with the cover") — the generic overlay cover parks at
  TopStart (18/36) while `layoutMove` shifts the whole title right by
  coverSideShift and squeezes width; on signature layouts (badge first,
  centered/overlay variants) this can cover the badge/title/crest zone.
  NOT YET FIXED — need a screenshot/description or user pick of the fix.
  User also said the signature BACKGROUND gets its own dedicated round
  afterwards.

### Next steps
1. Ask the user precisely what is wrong with the Signature + cover default
   (which look, what overlaps/what looks off) before editing its layout.
2. Implement; docs; commit; push.
