# Request Log — Web-series info (like books/albums) + album-style book sheet

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user, paraphrased)
Start adding info for WEB SERIES the way books and albums were enriched: a
real synopsis plus episode data. Also adapt the book notes bottom sheet to the
album layout: one scrolling sheet with the synopsis in a collapse at the top
and the chapters below it as their own accordion rows, with a like (heart) and
read action.

## Clarifications from the user
- Episode data = synopsis + FULL real episode lists (not a small sample), in
  batches researched with the web.
- Each episode carries: number + title + a short summary.
- Sheet layout: album-style ONE sheet (no Synopsis | Chapters tabs).
- Actions: a book-level favorite heart (like the album hearts) + keep the
  per-chapter read toggles with progress.

## What shipped
1. Data model + plumbing (committed `a0cddbb2`):
   - `SeriesEpisode(season, number, title, summary)` + `CurioTopic.episodes`
     parsed from JSON (`episodes` array) in TopicJsonLoader; null-safe,
     hydrated only for SERIES topics.
   - Room: `episodes` TEXT columns on `topics` + `cached_topics`,
     DAO `updateContent` includes it, v14 migration adds both columns, and
     the repository hydration maps rows back to `SeriesEpisode` lists.
2. First web-researched series batch (`a0cddbb2`): Chernobyl (5), Band of
   Brothers (10), The Queen's Gambit (7), Watchmen (9), Fleabag (12) = 43
   episodes with real one-line summaries from Wikipedia; human tone, no em
   dashes. Script: `tools/enrich_series_batch1.py` (kept for future batches).
3. Book notes sheet redesigned album-style (this commit):
   - `BookNotesSheet` is now ONE `ModalBottomSheet`: header (cover, title,
     byline, fetched rating, favorite heart + close), pinned reading-progress
     rail, then a single LazyColumn with `BookSynopsisAccordion`
     (About-this-book, Read/Hide) on top and every chapter as an expandable
     row (pages, notes, Mark-read/Undo pill) below.
   - The old `mode` seed still decides what opens pre-expanded (synopsis card
     → synopsis open; chapter chip → that chapter open + scrolled to).
   - New `AppPreferences` book-favorites store (heart per book title,
     reactive state, mirrors album hearts) + header heart toggle; chapter
     read semantics unchanged (chapter N read when N <= progress).

## Not yet done (next batches / later work)
- More series batches with web research (199 series total in series.json).
- Series reveal UI (episode chips + episode-list sheet mirroring album/book
  track-list sheet) once more shows are enriched.
- Show/season favorites mirroring album hearts if wanted.

## Version note
versionName 1.1.1, versionCode 20260921.
