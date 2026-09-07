# Prompt Log — current request

## Request (2026-09-07, active → v384 follow-up committing)

Share-card + notes-sheet follow-up on top of the pushed v384 smart-layout
commit (`644097c9`). Big combined request; asked 3 design questions first
(answers: count slider in the strip tool; List/Rows toggle = ALL designs;
no-fact favorites expansion = default, not a toggle).

**What the user asked + what was done:**

1. **Link share** — remove the Link icon; fold its caption dialog into the
   DEFAULT Share action. Done: the Share button opens one dialog with the
   caption field + an "Include a link" switch (link on → your words + the
   topic's link share as text; link off → the picture shares with the
   caption attached or none). The old Link pill button is deleted.

2. **Info-row "reappear" fix** — the sparkle's info-row repair didn't work
   when the fact box covered the rows (offset estimates missed box growth).
   Done: `ArrangeableCard` reports live card-local bounds (title/fact/meta/
   fav) via a new `onMeasuredBounds` callback → sheet state →
   `autoLayoutPlan` now computes REAL overlaps (with horizontal check) and
   pushes title / fact / info rows / favorites clear (new `favLift` too).

3. **Favorites overhaul** (albums):
   - Dynamic width: rows hug the longest song title (fillMaxWidth/weight
     removed in BoxedFavStrip + EditorialFavStrip).
   - Collage + Signature/Custom: `noBox = true` (plain type in the card's
     ink, no surface/border). Collage favSlot moved to the free middle
     (above the category pill + quick fact, below title/info); Signature
     raised clear of the fact/footer.
   - Strip tool: SONGS count slider (1..all, `favCount` on the move,
     persisted/loaded) + LIST/ROWS toggle (global `albumFavRows` pref in
     AppPreferences, `FlowRow` chips for rows mode).
   - No-fact: strip auto-expands (all songs, 1.3× type, wider) — `noFact`
     derived from blank fact at the card level.
   - Sparkle repair now includes the favorites strip (`favLift` → favDy).

4. **Dark-mode notes sheets** (books/albums/series): `ChapterNoteField` is
   now fully palette-aware (bg/placeholder/text/Expand chip derive from the
   sheet ink/accent instead of raw Material scheme colors) — fixes the
   "Add a note" box in dark mode. Page text + chapter/track/episode numbers
   already used palette-aware ink/onSurface/onSurfaceVariant vals.

**Out of scope / open:** exact visual placement of the Collage/Signature
favorites needs device verification (couldn't run the app; CI validates
compilation only). The album/series sheets' page text + numbers were
already palette-aware — if still "not fine" on device, the wash/ink recipe
in `notesSheetPalette` is the next lever. `view_agenda`/`view_module`
glyphs assumed present in the bundled symbol font (missing glyph = blank
icon, not a crash).