# Request Log — Book/series notes sheet polish + book cover/rating fetch fixes

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user, paraphrased)
A large batch of fixes around the book notes sheet and the book/series data UI:

1. Inside the bottom sheet, switching between chapters feels laggy.
2. The synopsis is pre-expanded when it should start collapsed.
3. On the topic reveal screen, chapters with no summary still show as empty boxes.
4. Mark-as-read and the per-chapter Like should live as chips on each row (not
   inside the expanded panel); mark-as-read should use an eye/book icon; the
   read state chip needs a solid fill (washed-out today).
5. Book posters don't load on the reveal screen but DO load from the book fetch
   hub — why? Fix it.
6. Add review info inside the bottom sheet; fetched ratings still don't show
   after fetching — is it broken? Show it small.
7. Add a custom per-book rating in a creative, book-themed way (fountain-pen
   nibs, per the user's answer).
8. The series UI should get the same treatment (user: "and also do the series
   ui too, its not done yet").

User answers to the clarifying questions: hide no-summary chapters on the
reveal (keep them in the sheet rows); show fetched reviews/rating small;
pen nibs / fountain pen for the custom rating; mirror everything in the series
UI too.

## What was done

### Sheet performance + layout
- **Lag fix (books + series):** chapter/episode chip taps now drive an INSTANT
  `scrollToItem` (the old `animateScrollToItem` glide over a long list was the
  jank). Tapping a row only toggles local expansion — it no longer calls
  `onSelectChapter`, which used to round-trip through the reveal's
  `selectedChapter` and reset sheet state + re-scroll on every tap.
- **Collapsed synopsis:** `BookNotesSheet` seeds the accordion collapsed
  (`initiallyExpanded = !hasChapters`) no matter how the sheet opened; the
  album + series accordions were already collapsed.

### Reveal screen
- `BookInfoSection` now filters chapters with a blank summary out of the chip
  row (they remain in the sheet's full list).
- The synopsis card's fetched-rating chip now also shows the ratings count
  ("4.2 · 1.2k").

### Per-row chips (books + series)
- Mark-read / Watched is now a circular chip on every row using the
  `FoldedCorner` (auto_stories, open book) icon; read/watched = SOLID accent
  fill with onAccent icon (inverted when the row itself is open), unread =
  muted surface. The leading number/✓ chip also goes solid accent when read.
- New per-chapter Like heart (book name → set of chapter numbers) and
  per-episode Like heart (show name → set of "S1E3" keys) in
  `AppPreferences` (`bookChapterLikesState`, `seriesEpisodeLikesState`,
  `toggleBookChapterLike`, `toggleSeriesEpisodeLike`), rendered with the
  existing `HeartGlyph` (red when liked), one tap away on every row.
- The Mark-read / Watched pill was removed from the expanded panel — it now
  only holds the pages + notes.

### Reviews + custom rating (book sheet)
- A compact "Reviews" card pinned under the progress rail: the fetched Google
  Books average (★ 4.2 · 1.2k ratings, small) plus "YOUR RATING" — a new
  `PenNibRating` control drawing five fountain-pen nibs (Canvas `NibGlyph`),
  tap to set 1–5, tap the current value again to clear
  (`bookCustomRatingsState` / `setBookCustomRating`).

### Book cover + rating fetch fixes (why things didn't show)
- **Covers:** the hub's Google Books provider resolved thumbnail URLs that the
  reveal poster had no way to guess — the reveal only tried the authored URL +
  Open Library. `resolveCoverUrl` now persists the winner per book
  (`bookCoverUrlsState` / `setBookCoverUrl`), `coverCandidates` and
  `BookCoverPoster` include it (after the authored URL, before Open Library),
  and the sheet's cover palette (`fetchCoverSwatches`) uses it too. The share
  card already routes through `coverCandidates` and picks it up.
- **Ratings:** both `fetchRatings` and `fetchRatingFor` had an early-abort
  bug — `return@runCatching null` on the FIRST search hit lacking
  `averageRating`, so later hits with stars were never checked. Both loops now
  `continue`, so far more books get ratings, and the hub "Fetch ratings" pass
  skips already-cached books (no restart-from-zero).

### Files touched
- `app/src/main/java/com/curio/app/data/AppPreferences.kt` — 4 new stores
  (chapter likes, episode likes, custom ratings, resolved cover URLs) + startup
  loads.
- `app/src/main/java/com/curio/app/features/settings/BookCoverFetch.kt` —
  persist resolved cover URLs; fix both rating loops.
- `app/src/main/java/com/curio/app/features/reveal/TopicRevealScreen.kt` —
  BookNotesSheet + EpisodeNotesSheet rework, BookCoverPoster candidates,
  no-summary chip filter, `PenNibRating`/`NibGlyph`, rating count on reveal.

## Verification
Brace/paren balance checked across all three files (the reveal file's
pre-existing +1 paren delta from HEAD is unchanged; my edits are balanced).
Symbols resolve (new AppPreferences APIs, PenNibRating/NibGlyph, FoldedCorner
icon). CI will validate the actual compile.