# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "also along with +- for chapter chnage and page chnage make it possible to update the
> progress with the progress bar too, dont show the knob always, but it appears when user
> touches it, also the flow animation is bad of it, can u chnage it and then some more
> improvement to pdf epub reader, also btw the auto roation wasnt working in pdf or ebud with
> the option"

Confirmed with the member via `ask_user`:

1. Make **the book card's gauge** (`BookDetailScreen`) draggable, knob only on touch.
2. Rework **both** the gauge fill/sheen animation and the reader Scrolling↔Pages switch.
3. Add **a real Auto-rotate option** (for both PDF and EPUB).
4. Reader improvements: **better chapter & outline handling** + **smoother page turns/scrolling**.

## 2. Findings

- `ReadingGauge` (BookDetailScreen) was read-only: an 11dp `Canvas` track, a spring fill
  (0.88/380), and an idle sheen whose band had HARD edges and restarted at the left every
  2.8s. The card's chapter/page `ProgressTile`s drive `chapterMove`/`pageMove` overlays.
- The reader presented as a NavHost route in `MainActivity` (no orientation lock in the
  manifest), so rotation is the window's. The only control was `ReaderLook.pageUpright`,
  a lone Switch shown ONLY for `content is ReaderContent.Pages`.
- The reader's flow switch swapped `ReaderFlow` in one frame; both `HorizontalPager`s
  composed only the visible page.
- `ReaderContentsSection` listed chapters flat, with no indication of the current chapter.

## 3. What was built (v418)

- **The gauge is a control.** `ReadingGauge` takes an optional `onScrub: (Float) -> Unit`;
  a 28dp touch node (track drawn inside) handles horizontal drag + press via two
  `pointerInput` blocks. The card maps the 0..1 fraction to a page (`setPageTo`) or chapter
  (`setChapterTo`) — the same overlay setters the tiles use. A knob is drawn in the Canvas
  and only fades/scales in while `scrubbing` (`knobAlpha`/`knobScale`), never at rest.
- **The fill tracks the finger** (zero-length tween while scrubbing, spring 0.90/320
  otherwise) and the **sheen no longer jumps** — its ends fade to `Color.Transparent`, so
  the restart is invisible (band 0.34, 3.4s).
- **Real auto-rotate.** `ReaderOrientation` enum (`AUTO`/`PORTRAIT`/`LANDSCAPE`) replaces
  `pageUpright`; the ACTIVITY takes `orientation.requested()`, applied to BOTH kinds of
  book. The \"page\" (ink) sheet now shows an AUTO-ROTATE three-way control for every book.
- **Flow switch settles.** The reading surface fades + lifts 16dp over 230ms on `flowKey`
  change, applied with `graphicsLayer` to ONE instance (the two pagers are never composed
  together). Both `HorizontalPager`s pass `beyondViewportPageCount = 1` (smoother turns).
- **Chapter highlight.** `ReaderContentsSection` takes `atIndex` and tints the entry at or
  before the live place.

## 4. Verification

- Brace/paren balance 0/0 on both files; no `pageUpright`/`showUpright` refs remain.
- Compose BOM `2026.05.01`, so `beyondViewportPageCount` and `colorStops`-gradients exist.
- No Gradle in this environment — CI validates the compile.

## 5. Open notes

- The reader's own \"Places\" progress bar was deliberately left read-only (the member
  chose the book-card gauge only); easy to extend via the same `onScrub` hook if wanted.
- `pageUpright` state was replaced outright, not migrated (the reader's look is process
  state, not stored), so a member who had it on starts at AUTO.

---

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — this request is logged above as §1–§5)
