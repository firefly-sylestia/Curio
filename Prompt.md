# Prompt Log — current request

## Request (2026-09-06, active → v380/v381 pushed, v382 committing)

Share-card editor refinement cycle.

- ✅ v380 (`8f8bef52`): hand-placed title owns its spot — drag anywhere
  incl. over/into the quick-fact box, no bounce-back (new persisted
  `titlePlaced`; drag-end folds the auto lift into titleDy).
- ✅ v381 (`3c32ed7c`): pill 9:16 flip = longer box + bigger text
  (factScale 1.18, height ≈ tall budget ×1.4); sparkle pill opaque with
  hairline ring (shadow halo gone); Collage dark-tone-aware layers
  (field/band/pill crushed toward black on Midnight/Ember so light accent
  no longer floods the bottom), torn seam visible on dark paper,
  polaroid caption fixed warm-dark ink, bottom band/footer blended via
  feathered gradients.
- ✅ v382 (THIS COMMIT): **Signature covers are glued.** User confirmed the
  cover "overlaps the badge/title". Signature joined `glueCoverStyle` and
  dropped the generic overlay: `SignatureCard` now receives `coverArt`/
  `coverW`/`coverH` and renders the jacket INSIDE its title block per
  layout (`TitleAndMeta(centered)`; `TitleText`/`MetaText` gained a
  `glued` flag; SIDE stacks the cover centred above the title in the
  narrow left panel; no-cover paths pixel-identical). Cover rides title
  drags/collisions/lift via `glueTitleMove` like Paper/Clean/Editorial/
  Minimal. Only Custom still uses the overlay pocket.

### Next (user-declared)
- Signature BACKGROUND treatment round (user: "then we will do the
  background of signature card styles") — separate upcoming pass, likely
  needs the design direction discussed.
- Watch for any CI result after these pushes (user said earlier "no need
  to watch the cl" — CI still runs on push).
