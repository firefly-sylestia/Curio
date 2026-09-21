# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> now back to reader buttom sheet the buttom sheet isnt scrollable and its not able to close
> with swipe so fix these, and then for apperance buttom sheet, use proper pill shapes, instead
> of toggle use proper 2 opton style with animation, in apperance its missing, the text size or
> zoom slider, for epub remove the horizontal page toggle, and add more proper apperance
> settings, and in the 3 dot ui, use proper pill shape grid with just capsulepills in a 6 grid
> with huge icon and a small text below instead of share a passage just share, reading settings
> to settings, tap zones to gestures
> properly understand what the design is then confirm it, its gonna use a similiar design system
> to samsung less text and toggle but more icon based button style and proper visual consistency
> also extend this to settings and suggest more settings and also the tap p zone edit let user
> hide the overlay so they can see what they are doing and only overlay the slider when they
> adjust and hide the overlay when they use the slider so they can see what they are chnaging
> and selecting one tap zone should switch its area, nd also the highligh doesnt work like when
> im zoomed in and i try to ta and hol dto select it doesnt work and the dock that appears after
> i tap and hold well it doesnt have the tools we had before tfor selections.
> https://github.com/firefly-sylestia/Curio/commit/2b01fd06e8b3efad773b1861210737622b87b950 before
> this commit too

## 2. Findings — the reader as it stands

All of this lives in `app/src/main/java/com/curio/app/features/personal/BookReaderScreen.kt`
(8.3k lines) plus `ReaderSettingsScreen.kt`.

### 2.1 The bottom sheet (`ReaderSheetFrame`, ~4050)

- A hand-rolled sheet: scrim + a `Surface` fixed at `maxHeight * 0.5f`, a handle + title that
  are the ONLY drag target (`detectVerticalDragGestures` on the header `Column`), and a body
  `Box.weight(1f)` holding `content()`.
- The body DOES scroll where the caller wraps it in `verticalScroll` (`ReaderPlacesSheet`,
  `ReaderAppearanceSheet`) — but `ReaderMenuSheet` is a bare `Column`, so it clips instead of
  scrolling. Nothing scrolls the SHEET itself and nothing scrolls when the finger is in the
  body: **swipe-to-close only works from the handle/title strip**, which is the "not able to
  close with swipe" report.
- The fixed half-screen height leaves dead space on short content and clips tall content.

### 2.2 The appearance sheet (`ReaderAppearanceSheet`, ~4180)

- Text size: A− / slider / A+ (only for reflowable text; absent for a PDF — that is the
  "missing text size / zoom slider").
- Typeface: three flat pills (Lora / Fraunces / Sans).
- Page: five 14dp-rounded swatch tiles + a `+` tile (not pill/capsule).
- Two `Switch` rows: "Auto-rotate" (flips between AUTO and PORTRAIT — a switch cannot say
  "wide") and "Horizontal pages" (`paged`, i.e. ReaderFlow).
- `ReaderSettingsScreen` repeats all of it as full-width rows plus a three-way orientation
  pill row and the tap-zone switch + "Place the zones" row.

### 2.3 The ⋯ menu (`ReaderMenuSheet`, ~3840)

- Six full-width rounded-50 rows with a small glyph + text + optional trailing: Notes,
  Highlights, Dictionary, **Share a passage**, **Tap zones** (trailing ON/OFF, long-press
  opens the editor), **Reading settings**. The member wants a 6-tile grid of capsule pills —
  big icon, small label under it — and the labels shortened: Share, Gestures, Settings.

### 2.4 Tap zones (`ReaderTapZoneEditor`, ~6645) + `readerZoneActionAt`

- The editor draws all four zone washes + rules over the page, four drag handles, and a
  bottom panel with Reset / Done, an edge chip row, an action chip row, and a always-visible
  Depth slider. The overlay is always on (no way to see the page), the slider is always up,
  and a zone is chosen with chips — not by touching the zone on the page.

### 2.5 Selection / highlight

- Reflowable text: `ReaderParagraphBlock` uses `detectDragGesturesAfterLongPress` → live
  `ReaderSelection` → `ReaderSelectionBar` (inks, note, bookmark, dictionary, more, clear).
- PDF: `PdfPageTextLayer` uses `detectDragGesturesAfterLongPress` in the layer's own space;
  a press more than 1.5 lines from any type (or on a page with no text layer) falls back to
  `onLongPress(page)` → the whole-page **Mark this passage** sheet.
- Presses on the PAGE (not the text layer) also go to `marking` → `ReaderMarkSheet`, which
  has inks + chapter highlight + note + bookmark + remove, but NOT the selection toolbar
  (no dictionary, no share, no more). That is the "dock after tap-and-hold has no selection
  tools".
- Zoomed PDF: the text layer is inside the page's `graphicsLayer` scale/translation while its
  gesture reads `liveWidth` (the UNZOOMED fitted width) and container-space offsets, and the
  outer surface's one-finger pan / double-tap handlers compete for the drag — the reason a
  hold while zoomed selects nothing.

## 3. Design proposed (to confirm before building)

A **Samsung-Notes/Books-style reader chrome**: fewer words, icon-first controls, one capsule
language everywhere, and animated segmented controls instead of switches.

1. **Sheet** — keep the reader's own paper/typography, but: body scrolls (wrap every sheet's
   body in a scroll, and make the drag belong to the sheet: a downward drag anywhere drags
   the sheet once its content is at the top), swipe-down-anywhere closes, height caps at ~60%
   instead of a fixed half so short content is not half-empty paper.
2. **Appearance** — capsule section pills; a **Text size** slider for reflowable books and a
   **Zoom** slider for a PDF (both present, contextual); typeface as three capsule pills;
   paper as capsule swatches; the two switches replaced by **animated segmented pills**
   (2-option for reading mode / book flow, 3-option for how the page stands).
3. **The ⋯ grid** — a 6-tile grid of capsule tiles, big glyph + small label:
   Notes · Highlights · Dictionary · Share · Gestures · Settings.
4. **Gestures editor** — an eye toggle to hide the zone overlay so the page is visible, the
   depth slider only shown while adjusting (and the overlay hidden while it is used), and a
   tap inside a zone on the page selects that zone.
5. **Selection** — long-press-to-sweep must work while a PDF is zoomed, and the long-press
   dock (the mark sheet) must carry the full selection toolbar (inks, note, bookmark,
   dictionary, share, more, clear).
6. **Reading settings** — same capsule/segmented language, plus the extra settings the member
   asked me to suggest (see the confirmation questions).

## 4. Confirmed, then built

The design was put to the member BEFORE any edit (four questions) and all four answers are in the
build:

1. **Epub flow** — "keep it as an animated 2-option segment in both places": the `Horizontal
   pages` switch is gone from the appearance sheet AND the settings page, replaced by an animated
   `ReaderSegmentRow` (Scrolling / Pages).
2. **New settings** — all of them: line spacing, page margins, text alignment, paragraph spacing,
   keep the screen awake, night dim, and **remembering them across restarts**.
3. **Sheet height** — "wrap content, cap at ~60% of the screen", with the body the one scroll and a
   swipe from anywhere collapsing it.
4. **Gesture select** — "tapping a zone on the page selects that edge" (`zoneEdgeAt`).

What landed, file by file:

- `BookReaderScreen.kt` — `ReaderSheetFrame` (wrap + cap + one body scroll + `NestedScrollConnection`
  pull + 150ms settle); `ReaderLook`'s six new fields and `ReaderLookStore` (prefs, `reader_look_v434`);
  the persistence/keep-awake effects and the night-dim wash in the reader body; the rebuilt
  `ReaderAppearanceSheet` (zoom for a PDF, capsules, segments, the layout rows); the ⋯ menu as a
  six-tile capsule grid (`ReaderMenuTile`); `ReaderSegmentRow` / `ReaderSliderRow` / `ReaderAlignRow` /
  `ReaderAlignGlyph` / `ReaderEyeGlyph` / `ReaderSegment`; the rebuilt `ReaderTapZoneEditor` (eye,
  on-demand depth that hides the washes, tap-to-pick); `ReaderTouch.selecting` + the `zoomed =` gates;
  the mark dock's dictionary + Share doors (`dictionarySeed`); leading/margins/spacing/alignment applied
  in `ReaderParagraphBlock`, both text surfaces and `paginateBlocks`/`pagedTextStyle`.
- `ReaderSettingsScreen.kt` — the complete twin of the appearance sheet, on the same components, with
  the three-way orientation segment and a Gestures section.
- `CurioIcons.kt` — `Subject`, `AutoStories`, `Bedtime`, `Nightlight` (each checked against the bundled
  font's glyph table; `crop_portrait` and every `format_align_*` are NOT in it, which is why the
  alignment and the eye are drawn).

## Checks run

- No Gradle command: this environment forbids compile / build / lint (`AGENTS.md`).
- Research was read-only: the reader file's sheet frame, chrome pills, ⋯ sheet, appearance
  sheet, zones editor, selection bar, settings page and both selection gesture paths.

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- **§26 — the reader chrome pass (done, this session).** Sheet scroll + swipe-close from
  anywhere; appearance as capsule/segmented controls with text size AND a PDF's zoom; the ⋯ menu
  as a six-capsule grid (Share / Gestures / Settings); the Gestures editor's eye, on-demand depth
  and tap-a-zone-to-select; zoomed long-press selection; the hold dock carrying the dictionary and
  Share; six new look settings, persisted. Design confirmed by four questions before any edit.
- **§25 — the double tap, and the tools' appear/disappear (done).**
- **§24 — the journal dock pass (done).**
- **§23 — the reader redesign + the vertical-PDF zoom (done).**
- (empty slot)
