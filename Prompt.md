# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "Add page-turn motion polish to the PDF pager (a subtle slide/curl)" — a follow-up to the
> v418 reader work, chosen from the suggested next steps.

## 2. Findings

- The PDF reader is the second `HorizontalPager` in `BookReaderScreen.kt` (`PageReader`).
  Its pages are **flush and full-bleed** (`pageSpacing = 0.dp`), so a swipe was two stills
  swapping with nothing travelling between them.
- Each page box already carries its own pinch/zoom `graphicsLayer` (owned by the page that
  asked for the zoom); the outer page `Box` was plain.

## 3. What was built (v419)

- Each PDF page now wears an outer `graphicsLayer` driven by its own distance from the
  settle point: a **0.10-of-a-width slide**, a **9° `rotationY`** about the page's OUTER
  edge (`transformOrigin`), a **4.5% shrink**, a light fade (`0.22` at full offset) and a
  long `cameraDistance` (`24f * density`, vs the default 8× which curls too sharply).
- The offset is read **inside the layer lambda** (`pagerState.currentPage - page +
  currentPageOffsetFraction`), never hoisted into composition — so a swipe invalidates the
  layer, not the composition. Hoisting it would recompose every page on every frame.

## 4. Verification

- Brace/paren balance 0/0 on `BookReaderScreen.kt`; no leftover composition-level
  `pageOffset`. New imports: `TransformOrigin`, `kotlin.math.abs`.
- No Gradle in this environment — CI validates the compile.

## 5. Open notes

- The effect is a DRAW transform only: layout, the pinch-zoom and the marks are untouched.
- The reflowable TEXT pager was left as-is (the ask named the PDF pager). The same layer
  drops in there if wanted.

---

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — this request is logged above as §1–§5)
