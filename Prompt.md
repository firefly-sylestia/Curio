# Prompt — Share card: cabinet icon, chapter progress + custom fact on all styles, fetch-to-show cover, font-resize box, draggable cover

## Request
User pointed at commit 6e76c013 (albums track-list sheet) asking why the
Cabinet icon changed, and listed five follow-ups modelled on the
`v0/book-share-progress-fact` branch:
1. Fix the cabinet icon (font regression).
2. Chapter progress bar should be SEPARATE, with a custom fact added below
   it — the reference branch only did this for the paper style; make it
   work on every card style.
3. Book cover must NOT be visible by default in the share card — tap
   "Fetch" and only then does it appear.
4. Font resizing: the fact BOX shrank/grew with the font size; a long
   paragraph still didn't fit at small fonts. Box footprint must stay
   stable while text size changes.
5. Add drag-to-move for the book cover (like the badge/chip).

## Root causes found
- **Cabinet icon:** the bundled `material_symbols_outlined.ttf` was
  re-subset at some point and the ASCII `2` glyph was dropped, so the
  GSUB ligature rule for `inventory_2` was pruned and the glyph failed to
  form — the tab rendered the literal string `inventory_2`. (Feature branch
  additionally pointed at a never-added `shelves` glyph.) Rebuilt the font
  from the full font keeping HEAD's glyph set + the missing digits; all
  200 referenced icons re-ligate (verified with HarfBuzz).
- **Progress bar + custom fact:** on main, Reading-progress content and the
  custom fact were mutually exclusive content "sources" — picking one hid
  the other, and styles rendered EITHER the chapter widget OR prose.
- **Cover auto-fetch:** a LaunchedEffect auto-loaded the authored cover on
  sheet open (v334 behavior).
- **Font resize box shrink:** fact text used a fixed `maxLines` cap
  (`lines(base, frac)`) while the font itself scaled via `bodyScale` —
  smaller font → shorter box AND same line cap → truncation got worse.
- **Cover not movable:** `ShareCardMove` had offsets only for title/fact/
  meta/badge; the jacket badge was placed with a hardcoded offset and no
  bounds reporting.

## Fixes implemented (TopicShareCard.kt + rebuilt icon font)
1. Cabinet icon — font rebuilt (see above); verified glyph shaping.
2. `TopicShareSheet` splits progress from fact: new `showChapterProgress`
   state; the Reading-progress pill turns the bar ON, Custom fact keeps it
   on (picking anything else turns it off). New `chapterFact` param threads
   a stacked custom-fact Text UNDER the progress widget on ALL eight styles
   (Paper via shared MiddleContent, Vinyl, Collage, Neumorphic, Editorial,
   Minimal, Signature incl. its centred layouts, Custom); blank when no
   custom fact. Progress + fact also flow into the export/share paths.
3. Cover no longer loads on open: `coverFetchRequested` gates the
   LaunchedEffect; the editor's Content panel shows Gallery / Fetch /
   Remove. First Fetch uses the authored URL; failures show "Try again"
   (bumps attempt → keyless providers), a shown cover shows "Refetch";
   Remove re-arms so the next tap is a fresh Fetch.
4. New `fitLines(base, frac, fontScale)` cap replaces the fixed line cap on
   every fact render: line count ÷ font multiplier so the box height stays
   put (smaller text fits MORE lines in the same box, larger fewer).
   Applied to all styles' fact text, the custom fact under the bar, and the
   Editorial drop-cap continuation text.
5. Book-cover drag: `ShareCardMove` gains `coverDx/coverDy` (persisted per
   style in the edits JSON), the jacket reports bounds via a new
   `EditBoundsCallbacks.onCover`, the ArrangeableCard chrome gets a COVER
   selection target (tap to select, coffee grip drags it, clamped to the
   card), and the toolbar's exhaustive `when (sel)` blocks handle COVER
   (no font/format tools for it).

## Verification
- No Gradle build in this environment (forbidden; CI validates on push).
- Brace/paren balance of the working file matches pristine HEAD's signals
  (identical net deltas — checker artifacts only, no new imbalance).
- Diff re-read end-to-end; conventions matched to existing badge/meta
  chrome, MoveHandle signature, dp-unit math, persistence format.
- Changelog (20260921.txt) updated with ADD/FIX bullets.

## Status
Complete. Committed and pushed with the font + changelog. Follow-ups (from
the earlier logcat request, separate): lazy/cached long-note rendering on
the entry Detail page — not part of this request.
