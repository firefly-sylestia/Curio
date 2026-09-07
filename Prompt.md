# Prompt Log — current request

## Request (2026-09-07, active → v3xx10 committing)

Third share-card batch on top of the pushed v3xx9 commit: sticker
imports/rotation, tool placement, sparkle info-row snap, fav tweaks.

**What the user asked + what was done:**

1. **\"Why is it only selected sticker / limited stickers\" + import PNG
   cutouts.** The emoji picker was the only sticker source. The sticker
   panel now has an **Import PNG cutout** button (`stickerPickerLauncher`
   via GetContent): the picked image is re-encoded to PNG under
   `context.filesDir/stickers/` (`importStickerPng` — transparency
   preserved) and dropped on the card. `ShareSticker` gained
   `imagePath: String?` + `rotation: Float`; the card layer and
   `StickerEditOverlay` render an image sticker as an aspect-preserving
   bitmap (width = sizeFrac × card width, decoded once per path via
   `decodeStickerBitmap` + a `ConcurrentHashMap` cache, downscaled to
   ≤1024px) instead of the emoji glyph. Both fields persist in the
   stickers JSON. Imported stickers drag / resize / re-stack / delete /
   save exactly like emoji stickers and ride onto the exported PNG.

2. **Rotate stickers.** A Rotation slider (−180°..180° with a 0° reset
   pill) in the sticker panel AND a two-finger twist on the card (the
   `detectTransformGestures` rotation delta, normalized via
   `normDegrees` so the slider thumb stays valid). Rotation renders in
   the sheet preview, the full-screen editor and the export.

3. **Text editing at the bottom.** The full-screen editor's Text /
   Stickers / Polaroid panels moved from under the top bar to BELOW the
   card (bottom of the dialog Column) — tools sit under the thumb. (Pure
   relocation of the existing panel blocks, verified with a brace check.)

4. **Icon-only toolbar pills.** `ToolWithCaption` no longer renders the
   tiny caption text — the edit toolbar is pure 44dp icon pills.

5. **Sparkle still put the info below the fact → real snap fix.** The
   meta collision logic is REPLACED by a snap-to-title: on the sparkle
   tap the info rows lift so their top meets the title's bottom (gap ≤
   2dp), so they always end up between the title and the quick fact,
   touching the title. `runAutoLayout`'s negative meta travel widened to
   −240dp so ONE tap brings a far-drifted strip all the way up.

6. **Fav List/Rows icons wrong.** The raw `view_agenda` / `view_module`
   strings are NOT in the bundled icon subset (verified: 0 occurrences
   in the font) so they rendered as literal text. Swapped to the
   verified `drag_handle` (List) and `grid_view` (Rows) glyphs.

7. **Tap favorites → auto-open its crop tool.** Selecting FAVTRACKS in
   the sheet's edit mode now sets `toolOpen = \"box\"` so the strip's
   sizing controls (width / songs / List-Rows) open automatically (both
   pager + single-style ArrangeableCard handlers).

**Notes / out of scope:** no build possible here (CI validates). The
imported-sticker decode + rotation are device-verify candidates; the
panels' move is layout-only. web/ and desktop/ untouched.

**Open question for the user (ask at end):** whether \"limited stickers\"
also means wanting a bigger emoji set / multi-select, and whether the
sparkle's new snap-to-title should also apply when the user deliberately
hand-placed the info row lower.