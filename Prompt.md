# Prompt.md — current request log

## Request: v328 — search speed + theming + cover hub + book share-card chapters

User direction (paraphrased):

> Topic browser search results are slow / show loading — make it lazy and
> cap at 50 topics. The category picker isn't theme-aware (light/dark).
> Mixes still have color-contrast problems in both themes. In the book
> cover fetch, show ALL covers there too, and cache both stars (average +
> count). Add book share-card content: a chapter-progress element and a
> custom-text chapter-review card. Selecting can be done from the
> bottom-sheet category and a link tool from share.

## Ask-answers that shaped the work

- **Search target = Browse Topics screen** (full-catalog fuzzy search was
  re-running on every keystroke → the "loading" feel).
- **Category UI wrong = the Category panel rows** in Cabinet + Topic
  Browser: checked rows had NO fill (only the tiny checkbox showed
  selection) and the panel surface read off (used the Curio LIGHT scheme's
  warm-tan `surfaceContainerHigh` even under dark-adjacent styles).
- **Mix contrast = whole-card cohesion** (cover plate + chips).
- **Share-card chapter features**: chapter progress + written review with a
  chapter chip, both with an in-editor chapter picker; chapters come from
  the topic's `chapters` (BookNotes sheet). Reading progress is ALSO
  tracked from the synopsis bottom-sheet chapter view.

## Completed

1. **Browse Topics search fast + capped.** `TopicDatabaseScreen`: the
   full-catalog fuzzy scan keyed a DEBOUNCED needle (`typedNeedle` +
   `LaunchedEffect` + 200ms delay, stale runs ignored) and search results
   are capped at the best 50 (`SEARCH_RESULT_CAP`); browse mode still
   pages all topics at PAGE_SIZE.
2. **Category picker panels theme-aware.** `CabinetCategoryPanel` +
   `DatabaseCategoryPanel` surfaces now use `curioDialogContainerColor()`
   (proper elevated surface in light, lifted dark surface at night), and
   CHECKED rows get a visible theme-aware category-tinted fill
   (`lerp(panelBase, accent, 0.22f)` — light accent wash in light, deeper
   accent lift in dark) with bolded label. Legacy checkbox rows match.
3. **Mix cards — solid identity pairs.** `namedMixTone` → `namedMixIdentity`
   returning `(tone, on-tone ink)`: light = deep tone + white glyph; dark =
   tone lifted 0.70 toward white + near-black glyph. Cover plate + lane
   chips are now SOLID tone fills with the on-tone glyph (was translucent
   0.42/0.22 blends that sank in light and washed out in dark).
4. **Book cover hub — all covers + both stars.** Prefs gained
   `bookRatingsCount` (+`setBookRatingWithCount`) and the hub gained an
   "All covers — N books" LazyRow (`CoverTile`: fixed 80×112 slot, the
   AsyncImage IS the poster with `fillMaxSize`, cached ★ + count under the
   title, tap → that book's reveal). `BookCoverFetch.fetchRatings` and
   `fetchRatingFor` now parse + cache `ratingsCount` too (new
   `BookStars(average, count)` return); the reveal hero shows
   "★ 4.2 · 1.2k" via a new `compactCount` helper.
5. **Reading progress in Book Notes.** New prefs
   `book_reading_progress` (name → highest chapter read; only moves
   forward). The CHAPTERS tab shows an "X of N chapters read" rail + thin
   progress bar; chapter chips tick with a ✓ once read; each chapter's
   detail card has a "Mark CH N as read" pill (haptics confirm).
6. **Book share-card contents.** `TopicShareSheet` gained an optional
   `bookChapters` param (threaded from the reveal caller for BOOKS). Two
   new content options when chapters exist: **Reading progress** ("I'm N
   of M chapters in", chip row WRITES the BookNotes pref so card + reader
   stay in sync) and **Chapter review** (custom-text field, review tagged
   with a "CH N · title" chip via the same chapter chip row).
7. Docs: changelog v328 entries (top of 20260921.txt).

## Follow-up: v329 — Reading progress is now a VISUAL widget on the card

User clicked "Show chapter progress on the share card as a visual progress
element instead of text". Implemented in `TopicShareCard.kt`:

- New public `ChapterProgressUi(read, total)` + a shared
  `ChapterProgressBlock(fill, track, ink)` composable: book glyph + caption
  ("3 of 12 chapters" / "Finished · all N" / "N chapters ahead of you")
  over a rounded 5dp track whose fill width = read/total.
- `TopicShareCard` gained `chapterProgress: ChapterProgressUi? = null` and
  every style's quick-fact TEXT is replaced by the widget when it's set:
  Paper (MiddleContent/FrostPane), Vinyl (cream box), Collage (white on
  sage field), Neumorphic (white on dark plate), Editorial (3-way branch
  skips the drop-cap), Minimal, Signature (shared `BodyText` helper — no
  ruled lines), Custom. Each passes its own surface inks for contrast.
  Bounds reporting kept (`onFact`) so the inline-edit box + move grip sit
  on the widget.
- Sheet: `progressForCard` computed when the Reading-progress content is
  active and passed to the carousel preview + single-style preview + the
  Save/Share export callsites (export = preview, both render the widget).
  The Edit-text circle hides for this content; the inert typing field gets
  the caption so no "Edit the quick fact…" placeholder shows over the bar.
- Other `TopicShareCard` callers (ShareHub, reveal, entry detail) omit the
  param → default null → unchanged behavior.

## Verification

- `git diff --check` clean; braces balanced (932/932) in TopicShareCard.kt.
- Grep/read-verified all 8 style swaps + dispatch + 4 sheet callsites carry
  `chapterProgress`/`progressForCard`; `ChapterProgressUi` public (the
  public TopicShareCard exposes it); no other callers pass positionally.
- No Gradle commands run (project DOX forbids them here) — CI validates.

## NEXT

Push this follow-up for CI. Then return to the signature-card campaign (one
category at a time, no SVG without permission) — awaiting the user's pick +
design for the next category.
