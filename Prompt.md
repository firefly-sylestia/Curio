# Prompt Log — current request

## Request (2026-09-06, active → shipped as v378)

One giant mixed batch:

1. Book-notes sheet: the chapter NUMBER text, the read (mark-read) toggle
   icon and the note ENLARGE icon still don't look right; remove the "big
   divider" and the "2 of 4 chapters read" progress sitting in it; make
   the "Chapter" first-letter capital.
2. Album track-list sheet: remove the cross/✕ close button.
3. Share-card editor — drag the TITLE over/above the quick fact and it
   jitters "crazy glitchy"; collision should only ever push from the FACT
   box side, never fight a title drag.
4. Full-screen editor: add a RESET button that resets ONLY the layout
   (not the text edits).
5. Book share-card colors default to a dark "midnight" — Auto should give
   books their own golden category tone.
6. Closing the share-card bottom sheet during editing is jumpy/glitchy
   and afterwards tools stop switching / only up-down swipes work.
7. Smart fit still isn't using the tool text adjustments — the text-size
   slider still reads 1×.
8. Collage smart auto-fit: quick-fact box should sit a little higher +
   text smaller; Paper: don't shrink text so much — expand the box height.
9. Full-screen preview is still inaccurate — "a little more zoomed and
   cut from below".
10. Bold sometimes doesn't show on the card; the box outlines glitch in
    the full-screen editor.
11. Double-tap text to edit it inline.
12. Remove the whole-box adjuster + its corner icon.

## Ask answers (batch 1, from earlier in the turn)

- Book icons = colors/contrast problem (not layout).
- "Big divider" = the accent hairline under the drag handle
  (`NotesSheetTopHairline`); the N-of-M progress stays as the rail LABEL.
- Share-sheet dismiss while editing = keep it blocked but smooth.
- Capitalize: chapter row titles + "N chapters" rail + reveal chips.
- Midnight-default question = it's the share card (Auto tone).

## Completion (v378 — one commit)

**TopicRevealScreen.kt (book/album sheets)**
- Removed `NotesSheetTopHairline` from BookNotesSheet; the progress rail
  is now one capitalized label ("2 of 4 Chapters read" / "4 Chapters") —
  the 4dp bar and duplicate "N / M" counter are gone.
- Contrast: on open (accent-tinted) rows the number disc text, the
  Mark-read FoldedCorner icon and the note Enlarge chip use sheet INK;
  read discs deepen to accent 0.30 with ink numerals; chapter row titles
  capitalize defensively.
- AlbumNotesSheet ✕ close Surface removed (v355 no-close model).

**TopicShareCard.kt (share editor)**
- Title drags never trigger auto-lift (`titleGrabbed` gate) → the "crazy
  glitchy" title-over-fact jitter is gone; collision lift only fires for
  fact-box growth into a parked title.
- `ShareCardMove.resetLayout()` (positions/crops/lifts only) + a "Layout"
  pill in the full-screen top bar.
- `paletteFor` Auto = nearest of the four LIGHT base tones to the topic
  accent (RGB distance) — books land on Golden Ochre, never a dark
  "midnight" tone.
- `sheetGesturesEnabled = !editMode` on the ModalBottomSheet → no
  half-dismiss spring while editing; Done/back exits first.
- Size slider (sheet + full screen) shows the RENDERED fact scale with
  smart fit folded in and writes the base back through the fit (WYSIWYG);
  Reset restores base 1×.
- Fit budgets: PAPER expands height (2.0/0.96) instead of shrinking text;
  COLLAGE shrinks long text to 0.70–0.75 inside its band.
- Full-screen preview: density-only zoom (`Density(density*zoom,
  fontScale)`) — sp text was scaling zoom² (the "zoomed + cut from
  below" bug); dp and sp now scale once.
- Corner whole-box grip (`CornerResizeHandle` + 4 call sites) removed —
  Crop sliders own sizing; also kills the "box outline glitch".
- Double-tap the fact text arms inline editing (`combinedClickable` +
  `onRequestInlineFactEdit` → sheet `requestFactInlineEdit()`), matching
  the Edit-text tool, in the sheet AND full screen.

## Follow-ups / notes

- Bold-not-showing and the outline glitch were traced to the same
  full-screen over-zoom (fixed); if bold still misses on a specific path
  after the density fix, reproduce it on-device and reopen.
- Collage "box sits a little higher": tuned via the band budget (more
  text shrink keeps long facts from ballooning down); a literal upward
  nudge of the collage body needs an on-device look.
- Docs updated (changelog top, app/AGENTS.md v378). CI watched after push.
