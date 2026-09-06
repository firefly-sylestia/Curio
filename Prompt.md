# Request Log — advance share-card adapt system

## Status: in progress (implementation started)

## The request (user)
Long list for the share-card editor: smart auto-adjust for long texts
(box grows + moves up, default ON), fact box height must expand much more
on the tall card, corner-drag whole-box resize, grouped move (fact drags
title+info along, each still separately draggable), more (darker) color
tones, auto text color, sat/contrast must only hit the background (never
text/polaroid), fix handles that stop working when overlapping another
box, inline editing for custom fact / chapter progress / chapter review
(no toolbar text box), editable chapter title for chapter review, and
after pushing: ask the user for a testing review.

## User decisions (ask_user)
1. Dark tones: **available immediately** (no level gate).
2. Chapter review title: **separate edit field** next to the chapter
   picker (chip stays above the review; review text edited inline).
3. Auto-fit vs manual edits: **manual edits win** — once the user moves
   or resizes a box, auto-fit stops adjusting that box.

## Implementation map (TopicShareCard.kt + AppPreferences)
- `AppPreferences.shareAutoFitState` (default true) + getter/setter.
- Auto-fit computed INSIDE TopicShareCard at render (so export matches):
  length → autoHeightFrac + autoDy (negative), applied to fact box; autoDy
  also nudges title + meta (consistent with the grouped-move request).
  Disabled per fact box once the user manually moved/resized it.
- Fact height: slider 0.35f..5f (was ..2.5f), `lines` max 28→64,
  `fitLines` max 48→80, inline field maxLines 24→60.
- Corner resize: new corner grip on the selected title/fact/meta box that
  scales width+height together; "Whole box" slider in the Box-size panel.
- Grouped move: the fact MoveHandle adds its applied delta to title + meta
  offsets too (title→meta grouping already exists).
- Tones: add `unlockLevel: Int?` to ShareCardPalette (null = always);
  pool = tones with null level or <= current level; ~8 new dark tones
  (null level) with auto-derived light ink.
- Sat/contrast: remove whole-card colorFilter; thread the matrix into each
  style and apply it to the BACKGROUND layers only.
- Handles: collect all MoveHandle/corner specs, draw them LAST in the
  overlay so a handle always wins touch over an overlapping box.
- Inline editing: chapter_review field binds customText only (chip is the
  prefix in the rendered text) + field shifted down one line to sit on the
  review text; remove the toolbar OutlinedTextField for custom/review;
  add a "Chapter title" override field in the Content panel (persisted
  per share); "Edit text" pill shows for custom fact even under progress.
- Auto-fit toggle switch in the Text-size panel (default ON).

## Fact formats — IMPLEMENTATION (TopicShareCard.kt)

Action carried out in this session: render three new fact-body formats per
style via central `renderFact(prefix, body, ...)` + the already-existing
Editorial drop-cap machinery.

**Decisions**
- STANDARD: same body, but now always `ParagraphStyle.lineSpacing = -4.sp`
  (word spacing unchanged). Text stays wrapped to full card width.
- BOOK PAGE: two columns. Columns are sized by ON-SCREEN width, not px
  imports (so export and preview both pick full card width = the card's
  own width at 4f density; no µornMagnifier needed). Hand-wrap split at
  the ON-SCREEN middle into `prefixText | middleColumn | restText`.
- EDITORIAL: if empty or " STANDARD". else the existing EditorialCard
  `editorialFact(...)` block (drop cap or first-word-big) replacing the
  usual body block.

**Renders**
- Renders exactly on paragraph content, not on topic title.
- Renders on request show (share preview) but adapte to text length on
  quick fact.
- Uses inline field only (no toolbar text box) after pushing.

## Pending

- Audiobook-only cover add-on: poster beside title + author, placed only
  when the cover is placed (books/albums/series); no compiler in env.
- Smart auto-adjust variations toggle (per style).

## Followup after push

After pushing: ask the user for a testing review (previous commit) and
fix without stopping.