# Prompt Log — current request

## Request (2026-09-19, batch Z7 — the PDF reader's zoom, its pages and its marks)

Verbatim: "book reader pdf vertical pages zoom are still inaccurate when i pinch zoom in the middle
the top part of the previous page zooms in, and theyre not marked as page numbers are they? and still
in the scrolling the page chnages when im swiping arounnd while zooming fix it please. it should page
chnage only when it reaches the page end and then on anomather sipe it does or maybe something better
and some more book reading features, also the places in this book progress is beautiful, now the your
marks are kind of really bad vie can u chnage its look, push it all"

Status: IMPLEMENTED (this log covers what shipped; CI is the compile check).

### 1. The pinch that magnified the wrong page (v399)

`PdfScrollReader` grew EVERY page's box with the zoom (the v395 fix for two pages overlapping), so a
pinch in the middle of the column also pushed the page above down the flow — the page the member's
fingers were holding slid out of the window and they were left looking at the page above it.

Now the box never changes size. Only the page you PINCHED is magnified, inside its own clipped frame
(`clipToBounds` on the item), so the layout is identical at 1× and at 4× and no neighbour can move.
Rule-shaped in the code: `ReaderLook.pdfZoomPage` (`-1` = every page, which is what the paged reader
wants — its frame is the screen).

### 2. The zoom is anchored at the fingers

`pinchToZoom` now reports the gesture's focal point (`event.calculateCentroid(useCurrent = false)` —
where the fingers HELD, not where they are), and the new `readerZoomedPan(box, drawn, from, to,
focus, pan)` solves the page's own transform for the new pan: the point under the fingers stays under
them. A plain drag is the same call at a constant zoom (the focus cancels out), so a pinch and a pan
can no longer disagree about where the page sits. `readerDrawnPage(box, aspect)` is the letterboxed
page inside its frame — the thing room for a pan is measured against, so a drag the page cannot take
is handed back to the scroll/pager (`Offset.Zero` = not consumed). `pinchToZoom` also gained a `key`
because a gesture handler outlives the composition that armed it: a page whose aspect arrives with its
render would otherwise keep measuring against nothing.

### 3. A page turn only after the page's own end

In the column the page now takes a vertical drag while it has room in that direction and gives it
back at its edge, so the next swipe scrolls on — the rule the member asked for. The drag claim reads
the live `pdfZoomPage` (never a captured composition value).

### 4. Page numbers, and a page bar that turns pages

- Every page in the scrolling reader wears its own number in the corner, drawn OUTSIDE the zoom (it
  cannot be carried off the screen by a magnify).
- The scrolling flow now HAS the page bar (it had none): the column reports the page it is showing
  (`onPageShown` → `shownPage`) and the bar names it and asks for the next one.
- Double tap zooms a PDF page at the point that was tapped (`readerDoubleTapZoom`), in both flows.

### 5. Jumps actually move a PDF (the quiet big one)

`jumpToMark` called `pagerState.scrollToPage` — the PAGED reader's pager. In the scrolling flow (what
a PDF opens in) "Continue reading", every mark and every contents row moved an OFF-SCREEN pager, so
nothing appeared to happen. A PDF jump is now ASKED FOR (`pendingPage`, the page-`pendingBlock`
already was for text) and whichever surface is showing takes it and clears it.

### 6. The marks sheet redrawn

`ReaderMarksSection` derives a mark's place from the CONTENT: a PDF's mark says "Page N" — it used to
fall back to "Section N", the reader's own block numbering, which is a fact about how the file was
split (this is what "theyre not marked as page numbers are they?" was) — with the outline entry it
sits under, and a reflowed book's says the chapter when the file numbers one. The list is sorted by
position (it reads in the book's order), each kind wears its own glyph on a wash of its own colour (a
highlight in the ink it was made with), the passage is set as a quotation in the book's serif and the
note as an aside, with the date in the corner (`readerMarkWhen`: Today / Yesterday / "3 Sep").

### Files

- `app/src/main/java/com/curio/app/features/personal/BookReaderScreen.kt` — all of the above.
- `app/AGENTS.md` — the reader's zoom/jump/mark contracts as rules for the next agent.
- `fastlane/metadata/android/en-US/changelogs/20260922.txt` — the release notes for this batch.

## Queued next (the member's follow-up, not started)

Verbatim: "now for the photo side by side 3 grid 4 grid, so in side by side now i cant chnge its sizes
like yes page size isnt possible but i cant chnage between small portraifght etc in side by side now
als same for 3 together, let the flixibility to pick differnt size of that photo, also in 3 grid they
look great in  while editing but when i chnage to eye view they get collaped so something the 2 which
are over each other also ykw keep the buttom strip here i write caption for them even if theres no
captaion keep it in preview and also show dates in dd:mm:yyyy or user can switch also give its own
differnt font choices in tools when im editing caption, only show those tools hen ive caption opened
and hide other tools which the caption doesnt support, and make the tools appear back when i go back
to writin gin canvas smoothly, fix them also make the animation preview while holding them better with
stack preview too, also still the voice note drag and move is kinda off the previe guide shows way to
the top when the voice note im holding is below so fix its accuracy and also instead of that color line
use propere preview and smooth animations."

Broken into the things to do (PersonalCanvas.kt, PersonalTodoRow.kt, PersonalPrintArrangement):

1. **A print in a row keeps its own size.** Side by side, three-in-a-row and four-in-a-row currently
   pin the size (a row is laid out from the run's own shape), so `Small portrait`/`Portrait` are not
   reachable from the size menu once a print is in a row — let a print pick any size and have the row
   re-arrange around it instead of refusing.
2. **Three in a row collapses in the READ view.** The editor lays a 3-run out as one upright frame
   with the other two stacked beside it; the eye then draws them collated instead. Editor and read
   view must share the same arrangement rule (the v394 pair already does).
3. **The caption strip stays.** Keep the wide bottom border on a print in a row — and show it in the
   preview even when no caption has been written yet, so it can be tapped and filled.
4. **Dates** (dd:mm:yyyy, with a switch to another order) and the print's own hand-written caption.
5. **The dock changes with the caption.** While a caption is opened, show only the tools a caption
   supports (its own font choices, the date) and hide the rest; put the writing tools back smoothly
   when the canvas has the caret again.
6. **The carry gesture's preview.** A better held-block animation with a stack preview of what is
   landing; the voice note's drop guide currently reports a place far above the note being held (the
   row measurement is off) — fix the accuracy and replace the flat colour line with a proper preview
   of the block, smoothly animated.
