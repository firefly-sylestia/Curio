# Request Log — advance share-card adapt system

## Status: implementation completed (pushed)

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
4. (v370) Floating edit box opens from the Edit-text TOOL (not auto on
   tap); single tap on the box selects it; quick double-tap enters edit
   mode; keep the Edit-text pill; custom fact + chapter review editable
   alone AND stacked under reading progress.
5. (v370) Smart layout: **always-on, manual-wins** — no per-style toggle.

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

## v370 — Floating edit box + double-tap + smart layout + cover-placement

- Floating edit box: bigger box above the card (no dark overlay), opened
  from the Edit-text TOOL when factEditMode is true AND the selected fact
  is editable (custom fact / chapter review / quick fact without progress).
  The on-card field is still the caret seat; the floating box binds the
  same editFact / customText so typing here is identical.
- Double-tap to edit: on the on-card fact box, a SINGLE tap selects it for
  moving (grip appears), a QUICK DOUBLE tap enters edit mode (floating box
  opens + keyboard). Implemented with one `detectTapGestures(onTap,
  onDoubleTap)` on the select layer (the field stays inert until armed).
- Edit-text pill stays in the toolbar; shows for quick fact, custom fact AND
  chapter review (even under progress).
- Custom fact / chapter review re-editable: the source panel hint now points
  to the floating box; the custom fact is editable alone AND stacked under
  reading progress; the chapter review text is editable in the floating box
  too (binds customText).
- Smart layout (always-on, manual-wins): `SmartLayout.adjust(...)` runs at
  render and nudges elements when the fact is long (>120 chars) OR a cover
  is placed. Fixes: COLAGUE gap between title block and middle section;
  NEUMORPHIC title kept off the fact area; EDITORIAL headline auto-shrunk
  (titleScale 0.82) for long facts; title/meta/fact clamped inside the card
  frame; when a cover is placed, titleWidthFrac → 0.62 and factWidthFrac
  → 0.80 so the cover (LEFT of title+author) + title + author fit without
  the title leaving the card.
- Cover placement (v370): BOOK/ALBUM/SERIES cover sits to the LEFT of the
  title + author per style (per category corner pocket), taken OUT of the
  Collage polaroid (that slot is the user photo only). Cover renders as a
  60×90dp poster thumbnail beside the title/author. When placed, the smart
  layer narrows the title + fact boxes so nothing goes off-card. Collage
  only shows a placed cover (move.coverDx != 0) as a separate left badge.
- Grouped move — badge: the FACT handle now also moves the category BADGE
  with the fact block (title + meta + badge travel together); badge still
  has its own grip.

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

- Smart auto-adjust variations toggle (per style) — user asked for "multiple
  variation smart adjustment per style with toggle"; deferred until the
  always-on smart layout is reviewed.

## Followup after push

After pushing: ask the user for a testing review (previous commit) and
fix without stopping.