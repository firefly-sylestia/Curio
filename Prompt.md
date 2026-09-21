# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> in vertical pages the pdf zoom is really buggy its scrolling when i try to zoom and it
> scroll so fast when i try zoom that like 10 pages it scrolls by, fix it please, when the
> pdf is verticaly stacked, and then the upper back button with the pdf namr of book make
> it floating pill style that floating at the top and same for the buttom tools make it
> floating, a proper pill shape not thin. proper pill with 5 buttons, one with appearncae
> with A- A+ witha slider to adjust the text size, below the font option only 3 in a row
> then below 5 differnt backgroud color the paper, sepia, night white and 2 more, and then
> belo 2 toggle with one auto rotate and another ith the horizontal option, then in the
> next button it shows 3 line which opens up the chapters content keep the bookmark button
> per chapter with the progress lets separate the bookmarks again and it will be 3rd option
> with bookmarks, and another with a 3 dot to show the menu of search, notes, highlithts
> disctionary, share, settings, each arounded pills, with proper ui and logic and settings
> gets its own screen, oh and the pages in the middle of the doc pill when tapping the
> pages it opens the page scrubber, and holding the page pins the page count as a small
> ounter in the corner, and make the ui smooth stbale buttom sheet which smoothly collapse
> or anything the height stays half of the screen, also put the search icon at the top
> right corner with proper floating top search with next previous button hihglighting the
> results on the pdf, use buttons and icons with less text and dont use erm dahses

Confirmed with the member via `ask_user` before implementing:

- **Reader settings** — a FULL-SCREEN page INSIDE the reader (reader's own style, not the
  settings family), also reachable from the settings side: for now the door is wired on the
  Dev page.
- **Page inks** — Gray is the sixth, plus a **`+` tile that reveals more tuned papers**
  (mint, rose, amber, slate).
- **Dictionary** — in-app **Wiktionary** (keyless). A single-word selection shows ONLY the
  dictionary icon; the selection bar becomes a **wide floating capsule** (Samsung-like),
  clean and smooth.
- **Tap zones** move into the **⋯ menu** (with its long-press placement editor); **Search is
  removed from the ⋯ menu** because the top-right search door is the one search.
- **Ship mode** — always-on (the redesign replaces the old chrome; no Settings toggle).

## 2. Findings

**The zoom bug is an anchor measured from the top of the file instead of from the sheet
under the finger.** `readerZoomDocument` (and `readerDoubleTapDocument`) compensated with

```
down.dispatchRawDelta(documentOffsetAt(down, focus.y) * (ratio - 1f))
```

and `documentOffsetAt` returned `index · sheetHeight + into` — the distance from the TOP OF
THE FILE. But a lazy column PRESERVES `(firstVisibleItemIndex, scrollOffset)` across a
relayout, and every sheet grows by `ratio`, so the relayout ALREADY moves the viewport by
`index · sheetHeight · (ratio − 1)` in document space. The compensation therefore *doubles*
the jump, and it is proportional to **how far into the book you are** — which is exactly the
report ("like 10 pages it scrolls by"): zooming 5% while 300 pages in threw away ~15 pages.

The arithmetic for holding the point under the fingers:

```
needed delta = (ratio − 1) · <the finger's offset INSIDE the sheet it is over>
```

(`into` alone). Page padding and the 16dp gaps do not scale with the zoom and cancel in the
difference, so the index-proportional term is not just stale — it is spurious. The
horizontal compensation (`focus.x · (ratio − 1)`) was correct and stays: a raw
`ScrollState` value is not auto-adjusted when its content grows.

The reader's chrome at the time of the request: a full-width opaque HEAD (back + title), a
floating PAGE BAR pill above the foot, and a full-width opaque FOOT (flow toggle, tap-zones
toggle, position line, search, palette, bookmark/places) — plus sheets drawn as custom
bottom surfaces with no drag and no fixed height, and a search sheet rather than a floating
bar.

## 3. What was built

**The zoom (the bug):** `documentOffsetAt` answers the finger's distance below the
FIRST VISIBLE item's top edge, and both `readerZoomDocument` and
`readerDoubleTapDocument` feed that to `dispatchRawDelta`. The index-proportional
term is gone, so a pinch holds the point under the fingers instead of adding a
second, deeper jump on top of the one the relayout already makes.

**The reader chrome, rebuilt (`BookReaderScreen.kt`):**

- `ReaderTopPill` — back, the book's name in the member's own reading type, and the
  search door at the end; the head is no longer a band across the page.
- `ReaderBottomPill` — five buttons: Appearance, Contents, **Pages** (the count, in
  the middle), Bookmarks, ⋯.
- `ReaderAppearanceSheet` — A−/slider/A+ for the text size, three typefaces in a row
  (`ReaderTypeFace`: Lora / Fraunces / the writing hand, applied to the whole page),
  five papers in a row (`ReaderSkin.primary`) + a `+` tile that unfolds the tuned ones
  (mint, rose, amber, slate — with a real `out = a·in + ink` PDF filter each), and the
  two switches: Auto-rotate and Horizontal pages.
- `ReaderScrubberSheet` (a tap on Pages) + `ReaderPinnedPage` (a HOLD on Pages pins the
  count in the corner, outside the chrome, so it survives the tools hiding).
- `ReaderSearchBar` — floating, in the head's row, with the field, the find count,
  next/previous and the cross; swept on a 320ms pause, the first find stepped onto at
  once, and every occurrence washed on the PDF pages (the one being stood on washed
  harder). Search is removed from the ⋯ menu.
- `ReaderPlacesMode` — the contents / bookmarks / notes / highlights sheets are ONE
  layout with a mode, so the chapter rows keep their own bookmark and their progress.
- `ReaderMenuSheet` — Notes, Highlights, Dictionary, Share, Tap zones (with the
  placement editor on a hold) and Reading settings, each a rounded pill.
- `ReaderSelectionBar` — a single word offers ONLY the dictionary; anything longer gets
  the wide floating capsule toolbar (one row, full radius, icons only).
- `ReaderSheetFrame` — hand-built half-height sheet, tweened in, draggable shut by its
  handle, scrolls inside itself, and swallows a tap so the scrim cannot shut it.

**New files:** `ReaderDictionary.kt` (keyless Wiktionary, memoised, 4s/5s budget) and
`ReaderSettingsScreen.kt` (`ReaderSettingsScreen` + `ReaderSettingsRoute`) — the reading
settings page in the READER's palette, opened over the book from the ⋯ menu and wired as
`CurioRoutes.READER_SETTINGS` behind a door on the Dev page.

**One judgement call worth naming:** the member asked for the Pages button "in the middle"
AND for Bookmarks to be the "3rd option", which cannot both be true in a five-button pill;
the middle button wins, because the behaviour they described for it (tap for the scrubber,
hold to pin the count) is the one that only works where the thumb naturally rests.

## 4. Still open

- Nothing beyond this request's own scope. The reader's own settings page is wired on the
  Dev page for now, as asked; moving that door into the app's Settings hub is a one-line
  change when the member wants it.

## Checks run

- No Gradle command was run: this environment forbids compile/build/lint (`AGENTS.md`), so
  the change is validated by CI on push plus a careful manual read of every touched API
  (Compose BOM 2026.05.01; `ModalBottomSheet` + `rememberModalBottomSheetState` are in the
  BOM and already used elsewhere in the app).

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- **§23 — the reader redesign + the vertical-PDF zoom (done, this session).** Superseded the
  old §21/§22 note in this slot (those commits were pushed on the member's later instruction;
  nothing is pending from them). The next items the member named for the source work —
  **Openverse, Art Institute of Chicago, OpenAlex + Crossref, NASA image library,
  iNaturalist** — are still NOT built.
- (empty slot)
