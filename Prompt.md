# Prompt.md — current request

## The ask

1. **The caption's own tools and a date — the full feature** (done, pushed as
   `c0ac876c`).
2. **"fix this"** — the CI failure pasted with the request
   (`PersonalTodoRow.kt:438`), already fixed by `8ef16daf` (the run pasted was
   `6d293e1b`'s, before that fix).
3. **The pinned-title audit** — the bar appears when the heading is still on
   screen, names a random heading, goes missing, names a heading that was
   removed, and taps land at the top of the page instead of at the heading; the
   member also wants its view reimagined and matching.

## The pinned title: root causes found

- **The reports were in the wrong coordinate space.** Both reporters used
  `boundsInParent()` — the place inside each line's own small wrapper — so every
  heading on every page reported a top of ~0 and a bottom of its line height. The
  host then added a `canvasTop` that never matched and compared against a scroll.
  Everything the member saw follows from that: a heading "gone by" before it had
  gone, the wrong heading chosen (`maxByOrNull` over equal numbers = whichever the
  map happened to iterate last), the bar missing, and a tap that ran `top` (≈0)
  into `animateScrollTo` — the top of the page.
- **A heading never stopped being one.** Removing the title flag (or the line)
  left its entry in the map for ever, so the bar could keep naming it.
- **Both sides shared one map.** The reading half and the writing half hold the
  same block ids in different boxes with different scrolls, and the reading side
  was judged against the WRITING page's scroll while the reader was looking at
  the reading page's.
- **The tap could not reach the reading side's scroll**, which is the caller's
  own `rememberScrollState()` inside the read view.

## What shipped (v402)

- The report contract is now documented as four rules on
  `LocalPersonalTitleReport`: **window coordinates**, **the scroll it was taken
  at**, **blank label = not a place anymore**, **`writing` = which side**.
- Both reporters use `boundsInWindow()` and send `writing`; a line that stops
  being a title (or leaves the page) clears its own entry from a
  `DisposableEffect(isTitle, id)`.
- `PersonalSectionLine` carries `writing` + `scroll` with `liveTop` / `liveBottom`.
- `PersonalPage` and `BookReviewScreen` judge the pin in window space against the
  writing area's own `boundsInWindow().top`, keep one entry per side, and read
  only the side being shown; the tap moves the scroll that is on screen by
  `liveTop - areaTop`, clamped to the scroll's range.
- `LocalPersonalPinScrollHolder`: a read view drops its own `ScrollState` in
  (`JournalEditorScreen`, `TodoScreen`, `TopicNoteScreen`), so the bar can take a
  reader back to a heading it is naming.
- `PersonalPinnedLine` redrawn: an accent medallion, a micro-label naming the kind
  of place (`caption` — "Heading" / "Chapter"), its name, an accent chevron, and a
  hairline of the page's accent. The medallion's fill is a blend, not a
  translucent accent (root rail rule 11).

## Open questions for the member (from the caption work)

1. **"The date is always on"** — read as *the date tool is always in the dock*.
   The other reading is *every new caption is stamped with today by itself*.
2. **The separator** — the request said `dd:mm:yyyy`; labels write `14/03/2026`
   because a colon reads as a clock time. Easy to switch.
3. **The faces** — the seven offered are the app's own bundled ones; a face made
   just for labels is a font file plus one enum entry.
