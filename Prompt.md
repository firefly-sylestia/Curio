# Prompt.md — the current request

The running log of what was asked, what was found, what changed, and what is
still open. Replace it when a new request starts; add a completion summary
when one finishes. No tool attribution, no em dashes in user-visible strings.

## 1. The request

Merge `ci/workflow-redesign` into `main` and work on `main` from there, then
finish everything still open, one item at a time:

1. Vertical zoom: a small shift when a page is zoomed or unzoomed.
2. The reading progress is not live.
3. Switching a page with the page-switch pill hides the tools (it should only
   hide when the page is touched).
4. Horizontal mode in the PDF and EPUB readers, with a text-size slider for
   EPUB. Auto-detected, toggleable off, and it only affects the PDF.
5. The waiting work: photo rows (join an existing pair into a three, and switch
   sides), the zoomed-page swipe glitch, the home door lists feeling empty on a
   first open, and the fetched posters/covers lost after a restart.
6. Double tap to unzoom, the same as double tap to zoom.

## 2. The merge

`ci/workflow-redesign` was four commits ahead of `main` with `main` holding
nothing extra, so it was a clean fast-forward. Work continues on `main`.

## 3. What changed (v406)

### Reading — the reader, `features/personal/BookReaderScreen.kt`

- **Zoom, unzoom and the swipe, in one rule (`pinchToZoom`).** A gesture now
  has ONE owner from its first move to the finger-lift:
  - Two fingers consume from the FIRST event. The changes used to be consumed
    only once the pinch had something to report (`zoom != 1f || pan !=
    Offset.Zero`), so the column underneath took the opening frame of every
    zoom and the page moved before the pinch did.
  - A one-finger pan on a magnified page claims the gesture on its first move
    and keeps it. The drag used to be handed back MID-GESTURE the moment the
    page reached its edge, so the pager (whose slop the consumption had already
    cancelled) took over with the finger's whole accumulated travel and jumped
    to the next page in a flash — the member's "glitched preview".
  - A page ALREADY at its edge when the gesture starts takes nothing at all, so
    the surface underneath owns the entire swipe: the page turns on ANOTHER
    swipe, which is exactly what was asked for.
  - `declined` and `ownsTheDrag` are both per-gesture, reset at the down.
- **The shift (`readerZoomThisPage` + the two draw lambdas).** The page was given
  up at `1.02`, which threw away a live pan while the page was still 2%
  magnified, so an unzoom snapped it back to its centre. The reset is now at
  `1.001`: at exactly 1× the pan is already zero, because `readerZoomedPan`
  clamps the travel to the page's own room and a page at 1× has none. The drawn
  translation is also no longer gated on `z > 1.02f` — that dropped the pan in
  one frame while the page was still scaled.
- **The progress is LIVE (`ReaderLivePlace`).** The places sheet read the STORED
  auto-bookmark, and that row is written only once a scroll settles (up to
  900ms), so the card could name a place already left. Every surface now reports
  what it is showing as it moves: `onBlockShown` (scroll list and the PAGED text
  reader, the latter via a new `liveTextBlock`), `onPageShown` for the PDF
  column, and `pagerState.currentPage` for the PDF pager. The sheet takes
  `live: ReaderLivePlace?` and falls back to the stored mark only when no
  surface has reported a place yet.
- **A text jump is ASKED FOR.** `jumpToMark` moved `listState` directly, so a
  mark, "Continue reading" or the bookmarks list did nothing visible in the
  PAGED flow. It now sets `pendingBlock`, which whichever surface is showing
  takes and clears (the PDF side already worked this way via `pendingPage`).
  A new `onContinueAt` carries the live index from the progress card.
- **The tools stay while the bar is used.** The auto-hide countdown now keys on
  `askedByReader` and does not run while a turn asked for at the bar is in
  flight; when it settles, the countdown restarts from full. It moved below the
  `askedByReader` declaration to do it.
- **A type-size slider (EPUB only).** `ReaderInkSheet` takes `showType`, and a
  reflowable book gets a `Slider` over the very value the pinch writes
  (`ReaderLook.textScale`, 0.8–2.6), said out loud as a percentage. A PDF is a
  picture of a page, so its own size stays the pinch's.
- **Double tap to unzoom already existed.** `readerDoubleTapZoom` resets to 1×
  and clears the pan when the page already owns a zoom, and it is armed on both
  surfaces (the column's frame and the pager's page). Nothing was added; if it
  is not working on the device that is a separate bug (the tap not reaching the
  page while magnified) and needs a reproduction.

### Reading — the page's own standing (`pageUpright`)

`ReaderLook.pageUpright` (default off) is the reader's one orientation choice.
Off is "turn with the phone": the app declares no `screenOrientation` anywhere,
so rotation is already allowed and a wide PDF page is read the way a wide page
wants — the member's chosen "horizontal mode" (just allow landscape, auto
detected). On keeps a PDF's pages UPRIGHT, applied on the ACTIVITY through
`context.findActivity()` and only while the content is `ReaderContent.Pages`, so
a reflowable book is never held to one way up. The switch lives in the page's
ink sheet (`showUpright`), beside the EPUB type-size slider (`showType`), and
`requestedOrientation` is restored to `SCREEN_ORIENTATION_UNSPECIFIED` on
dispose.

### Home — a door's row with nothing in it (`EmptyDoorChip`)

A first-time Home had two doors and two rows of nothing: a `LazyRow` draws no
chip until a journal or a book exists, so each strip collapsed to the door alone
(the member's "those place feels empty", and their answer that it is NOTHING at
all rather than a late fill). Each door now emits an `EmptyDoorChip` at
`CHIP_WIDTH` x `CHIP_HEIGHT` when its list is empty — "No pages yet / Start your
first one" wired to `onWrite`, "No books yet / Open the shelf" wired to
`CurioRoutes.BOOKS` — so the strip keeps its shape and offers the first step.

### Writing — the other side of a print row

`PersonalRowDragState` gained a horizontal axis. `dragBy` now takes `amountX`
and keeps it as INTENT only (`travelX`), never as a slot: a page lays its blocks
out in one column, so sideways travel has no slot to move to. `takeLeftCell`
answers "which end of a print row does the carried print take" — a REAL sideways
drag (`SIDE_INTENT_PX = 40f`) chooses the end, and the vertical direction remains
the fallback, so a plain downward carry onto a pair behaves exactly as before.

Both halves of the drop read that ONE value: `printDropIndex(from, to,
            takeLeft)` picks the cell, and the landing ghost draws the cell. The ghost
used to draw the OPPOSITE end from the one the drop chose (a downward drop
landed on the row's head while the dashed room was drawn at its tail), which is
one of the ways a pair "sometimes upgroups". Joining an existing pair into a
three already worked through `printDropIndex` (a print arriving from elsewhere
is snapped to the run and inserted at its head or just past its tail), and the
side switch is what was actually missing.

### Reveal — the fetched art survives (`sheetArtUrlsState`)

Nine art sites (ArtworkSheet, AuthorWorks, and the album / film-variant / anime
/ film / song / kind sheets in TopicRevealScreen) seeded themselves from the
cache but keyed the `remember` and the `LaunchedEffect` on the TOPIC alone. The
map is filled in `initThemeMode` at app start, but a URL another surface
resolves after a card composed was therefore never noticed, and the art was
fetched again on the next visit. Each site now reads the cached URL once and
uses it as a KEY for both the seed and the effect.

## 4. Still open — needs the member

- **Journal entries being slow.** Needs a measurement (list open vs page load):
  is it the list opening, or the page loading?
- **The photo row's "it doesn't let me join a pair".** The drop logic and the
  side now agree, so the next step is a reproduction on the device: if a print
  still refuses to become a three, the suspect is `rowDrag.targetIndex` (the
  slot the finger is pointing at) rather than the insertion itself.
- **CI is unconfirmed for both commits of this session** (`e61c244c` and the one
  after it), and neither has been pushed.

## 5. Notes

- Nothing in this batch has been CI-verified (no compiler in this workspace);
  validation was `scripts/check_braces.js` and manual call-site arithmetic.
- The store changelog (`20260922.txt`) and `app/AGENTS.md` carry the same
  changes in the same commit.
