# Prompt Log — current request

## Request (2026-09-07, active → v3xx9 committing)

Second share-card batch on top of the pushed v3xx8 commit: favorites
color/placement/collision fixes, collage cleanup, the polaroid rework,
and sticker pinch-to-resize.

**What the user asked + what was done:**

1. **Signature favorites text colour + overlap.** The plain-type strip on
   Signature wore the TONE's palette.ink, which clashes with signature
   scenes (near-black on Mario's red / Pikachu's yellow). It now wears
   the DESIGN's own body ink — `sigFavInk` = `signatureDesign(...).bodyColor`
   computed in the shared favorites block, threaded through
   `FavoriteTracksBadge` → `BoxedFavStrip` via a new `inkOverride` param
   (null = palette ink for Custom/others). The Signature slot ALSO moved
   from the bottom corner (where it overlapped the bottom-anchored quick
   fact) up to just below the title/author block (TopStart 138/130dp).

2. **Collage favorites colour + placement.** Raised the slot a little
   more (112/98dp) so the strip parks just under the title/author rows,
   fully on the cream top paper (clear of the tear seam + the pill/fact),
   and bumped the plain-type alphas (label 0.66, body 0.92, heart 0.95).

3. **Favorites collision direction fix.** `bottomOverlap(upper, lower)`
   ASSUMES the first box is above the second; the old fav/fact calls fed
   boxes in the wrong order per style and produced giant false positives
   that shoved the strip/fact around on sparkle taps. New order-guarded
   `pokeAbove` helper; `favOverFact` (fav above fact, Collage/Signature
   placement) now pushes the FACT down, and the fav-only lift is
   order-guarded so the bottom-corner styles (Paper/Vinyl) never
   false-trigger.

4. **Big category icon removed from the middle of the Collage card.** The
   80dp center glyph in `Watermark` is now optional (`center` param,
   default true); the Collage card passes `center = false` (corner set
   stays).

5. **Polaroid rework (Collage).**
   - Movable: new `ShareCardResizeTarget.POLAROID` + `onPolaroid` bounds
     callback + selectable box + MoveHandle grip in the ArrangeableCard
     chrome (mirrors cover/fav).
   - New move fields `polaroidDx/Dy/Scale/Style/Filter` persisted per
     style (Reset layout clears position + scale, keeps style/filter).
   - 5 STYLES (`PolaroidLook`: Classic · Retro · Sunglow · Vintage ·
     Dashed — frame/tape/tilt/finish, Dashed wears a dotted hairline) and
     5 PHOTO FILTERS (None · Noise grain · Nostalgia sepia · B&W · Warm
     with overlays/vignette via sepiaMatrix / grayscaleMatrix / warmMatrix).
   - New full-screen **Polaroid** button (Collage card only) + panel
     (style chips, filter chips, Print-size slider); the sheet's Box tool
     also gained a Polaroid-size slider when the polaroid is selected.
   - Tape now PEERS out past the white frame (offset y = −5dp) and is
     drawn LAST (sits ON the photo) — real washi-sticker look.
   - Frame ADAPTS to the photo's aspect (landscape = wide/short, portrait
     = tall, capped at 46% of card height; no photo = classic 1.18 print);
     the photo contain-fits the window.

6. **Sticker pinch-to-resize.** `StickerEditOverlay` now uses ONE
   `detectTransformGestures` recognizer per sticker — tap selects, drag
   moves, two-finger pinch scales (new `onResize` wiring clamps
   0.08–0.6 width-fraction; the Size slider stays for fine control).

**Notes / out of scope:** no build possible in this env (CI validates).
web/ and desktop/ untouched. The fav-above-fact sparkle math changed
order semantics — worth a quick on-device look at Paper/Vinyl sparkle
(should be unchanged) and Collage/Signature (fact pushed down, not the
strip). Sticker pinch + polaroid gesture feel need device verification.