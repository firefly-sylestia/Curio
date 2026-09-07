# Prompt Log — current request

## Request (2026-09-07, active — full-screen polish shipped; Cabinet v2 next)

User asked for two things in order:

### 1) Full-screen editor / polaroid / sticker fixes (DONE — pushing now)
> "in full screen editor the top aspect ratio layout reset sticker etc have
> full text i want icon only and the text and polaroid option place them at
> the bottom they are overlapping each other and also fix the polaroid
> itself in case its outline box as its getting cut and the size adjuster
> dont have length too and stickers need the tapping the outside to
> deselect and also its option make it smooth to open and close"

Shipped in v3xx13:
- **Icon-only top bar** — Close · Aspect · Layout · Stickers · Polaroid ·
  Text pills dropped their labels (compact 40dp circles), right cluster is
  horizontally scrollable → no more crowding/overlap. Panels already live
  BELOW the card in the dialog Column (verified structure).
- **Polaroid cut fix** — the POLAROID selection border drew tight on the
  frame rect, slicing the tilted corners + tape that peers past the top;
  the tap box no longer carries the border and the SELECTED outline floats
  OUTSIDE the print (8dp padded).
- **Size-adjuster length** — render clamp capped the print at 36% of card
  width (dead past scale ≈1.06); widened to 44% width / 55% height cap.
- **Sticker tap-outside deselects** — bottom-most full-size tap layer inside
  `StickerEditOverlay` (armed only while a sticker is selected) so
  sibling hit-testing still routes sticker taps to the sticker.
- **Smooth open/close** — Text/Stickers/Polaroid bottom panels animate with
  `AnimatedVisibility` (fade + expand / shrink).

### 2) Cabinet v2 (NEXT — not started yet)
User direction so far: **experimental toggle**; will host **liked books** +
**saved things**; **no more Save on the home screen**; books/albums/series
show their **cover in the hero style** of the card (half book, not
stretched); a **blur/glass text bar** instead of solid; "proper thoughtful
features and customisation". Also "properly plan its implementation and
features". Clarifying questions were asked after this push (see below).

Remaining backlog from earlier batches: multi-select stickers, sparkle
info-row always snaps, quick-fact tap-out edit reset, wider text-history
coverage.
