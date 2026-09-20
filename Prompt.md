# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> so in vertical pinch to zoom, its inaccuarte zoom, it zooming from the corner, not
> zooming where i am zooming, also the side click to chnage page isnt working like doesnt
> work work on tapping the side of the screen not the page but side of screen. or buttom
> of the screen, let user adjust the positon or area by tap an holding the button. also
> the minimal play button isnt accurate in journal voice note, and the ribbon style isnt
> greaat, beats design is bad too, and for waves the progress small dot doesnt properly
> follow the waves, also when there is not much sound in wave the wave isnt subtle so its
> not clear differnt. also for the waves add the play button as pill option too. and then
> in journal in tools add a full text copy option but for that it opens up the copy paste
> cut select all tool in the floating tool which stays above the tool when its active as
> floating tools, and it hels user to copy and select all in the page, and also a export
> format for the journal as pdf or supported format with exact view in file

Confirmed with the member via `ask_user`:

- **Reader taps:** full-height side edges turn the page (left = back, right = on), the top
  band scrolls back, the bottom band goes on.
- **Zone adjustment:** "all of the above" — sliders, a per-edge action choice, AND a
  drag-to-place overlay.
- **Voice looks:** fix MINIMAL's mark, redraw RIBBON (a clean single stroke, not a closed
  mirror shape) and BEADS (even, resting beads on an obvious sound scale).
- **Play as a pill:** a NEW look in the picker.
- **Export:** PDF, a text export keeping the bold, and a Markdown file.
- **New measures:** always-on (no Settings toggles).

## 2. Findings

- `readerTapStep` answered only inside 72dp CORNERS, so a tap on a left/right edge at
  mid-height returned 0 — the page's own tap. That is the whole of "the side click …
  doesnt work on tapping the side of the screen".
- The scrolling PDF's tap handler lives on the SHEET, whose frame is the DOCUMENT's, and
  it passed `size` = the sheet's size — so a zone test against it was measured against a
  frame that can be four times the screen. Any zone must be tested in the SURFACE's own
  coordinates and size.
- `readerZoomDocument` grew the layout with no compensation, so a sheet grew from its own
  top-left and the pinched point slid away — "it zooming from the corner".
- `reach(level) = level * 0.86 + 0.14` in every wave drawing: a whisper and a shout drew
  nearly the same height.
- The progress bead was `points.minByOrNull { |x - head| }` — a VERTEX of the drawing, so
  between two peaks it sat off the ink the spline actually draws.
- `VoiceDrawnControl` drew a 32%×44% wedge (taller than wide) inside a ring at 34% inset.
- RIBBON was a closed, filled shape; BEADS took each bead's own radius out of its step and
  cut the heard run at a plain fraction of the width — which is why beads and progress
  disagreed.

## 3. What was built (v424)

**Reader** (`features/personal/BookReaderScreen.kt`)

- `ReaderLook` gained `zoneLeft/Right/Top/Bottom` (`ReaderZoneAction`), four depths, and
  `zonesEditing`. `readerZoneActionAt(at, size)` tests the SIDES first (whole height), then
  the head and the foot between them; the defaults are the member's own reading.
- `scrollPage(step)` is the "screenful" half of a zone (`scrolls`), `stepPage(step)` the
  turning half.
- The tap is said in the SURFACE's coordinates: the reader's Box publishes
  `surfaceOrigin` + `surfaceSize`, `PdfScrollReader` translates each sheet's tap
  (`at + where - surfaceOrigin`) and the column's own (`at.x - across.value`).
- `ReaderTapZoneEditor` — hold the Crop switch: the four edges are drawn where they are,
  each boundary is a draggable `ReaderZoneHandle`, and the panel carries the four edges'
  actions, a depth slider, Reset and Done. It takes every tap; the chrome stands down.
- `readerZoomDocument(zoom, drag, focus, down, across)` anchors the document zoom on the
  fingers by `focus * (ratio - 1)` per step; `readerDoubleTapDocument(at, …)` does the same
  at one point.

**Voice notes** (`features/personal/PersonalVoice.kt`)

- `voiceReach(level, bandHalf)` = the square of the level over a 0.08 floor, used by every
  wave (pulse, bars, bubble, ribbon, beads).
- `pulsePointAt(points, x)` evaluates the drawn spline, and the head bead is placed on it.
- RIBBON is one round-capped stroke out along the voice and back down its mirror; BEADS
  are 26 even slots measured from each centre over a drawn wire, sized `min + (room-min)*l²`.
- `PILL` is the seventh look (`drawsPulse` picks the pulse, `VoicePillControl` the pill);
  `VoiceDrawnControl` now measures its mark (room-based height, wider than tall, a 6%
  optical lift, the stroke taken out of the size).

**Journal** (`features/personal/PersonalCanvas.kt`, new `PersonalExport.kt`)

- The dock's Copy tool asks via `PersonalEditorState.requestPageTextMenu()`; the canvas
  selects the page and raises the platform bar above the tools
  (`pageToolbar.showMenu(rect = the canvas's own foot, …)`, Copy + Select all).
- The dock's Export menu writes PDF / Text / Markdown through `rememberPersonalExporter`,
  off the UI thread; files land in `cacheDir/exports/` (new `personal_exports` path in
  `res/xml/file_paths.xml`) and leave via Android's share sheet. The PDF is drawn with
  `PdfDocument` on the journal's own paper, ink and accent: real `StaticLayout`
  paragraphs split across pages by line, marker pens as leading-margin spans, photos with
  captions, voice notes with their wave and clock, and a foot that names and numbers the
  page.

## 4. Verification

- Brace/paren/bracket balance 0/0 on every touched Kotlin file: `BookReaderScreen.kt`
  (2814/938/124), `PersonalVoice.kt` (789/241/64), `PersonalCanvas.kt` (2369/677/274),
  `PersonalExport.kt` (349/126/15).
- Every `PdfRun` page break goes through `want` / `breakPage` / `ensurePage`, so no block
  can spin between two pages and a full page is never followed by a blank one.
- No Gradle in this environment — CI compiles it.

## 5. Open notes

- The platform's floating bar is anchored at the page's foot (root coordinates). Android
  decides whether the popup lands above or below that anchor; on a full-height canvas it
  should sit just above the tools. If a device shows it over the dock instead, the anchor
  is the one line to change.
- Cut and Paste are deliberately not offered by the page Copy tool: both need a caret, and
  a page-wide selection has none. Paste already exists as the three-finger swipe and the
  field's own bar.
- `ReaderLook` is still process-only (never persisted), like the ink and the flows — an
  edge is cheap to place again, and this keeps the file honest about what it is.
- The reader's zones are drawn over the page, not the chrome: with the chrome up its own
  buttons (and the page bar) answer first, which is intended.

---

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — the v424 request is logged above as §1–§5)
