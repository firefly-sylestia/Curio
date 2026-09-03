# Prompt — Share-card chapter toggle, progress slider, custom-fact fixes, stars, cover, info-row wrap

## Request
Big share-card batch (v334), plus fixing the CI compile error from the
previous Genius-pill commit (already fixed + pushed as `8808683d`):
1. Chapter read toggle — tapping Mark-read must be undoable (progress both ways).
2. Chapter progress chip separate; track REAL chapters (not merged); slider
   instead of +/- buttons; the on-card progress widget movable + theme-aware
   with proper progress display.
3. Custom fact usable as chapter review; fix duplicate-text bug when tapping
   the custom-fact box; fix the box disappearing after clearing typed text.
4. Book star fetch: stars never showed on the card; bulk fetch restarted from
   the beginning every time.
5. Book cover: auto-add on the share card, properly placed, user-overridable
   (gallery / refetch with another provider / remove).
6. Author/artist/year info below title: wrap to 2nd line when long (was cut),
   plus width/height size adjustments.
7. Tapping the quick fact auto-converts it into a custom fact.
8. Fix the CI compile error and push, then continue.

## Decisions
- Progress now writes `AppPreferences.setBookReadingProgressExact` (goes
  backward; zero removes the entry) — reveal toggle + share-card slider both
  use it so Book Notes stays in sync.
- Chapter widget on the card already renders in the fact slot via
  `ChapterProgressBlock` (theme-aware per style, movable via the fact move) —
  verified, no change needed.
- Stars: the reveal hands the fetched `bookRating` into the sheet
  (`bookRating`/`bookRatingCount` params); the quick-fact content carries the
  rounded rating so the card's star row renders. No re-fetch in the sheet.
- Cover: sheet auto-loads from the topic's authored `imageUrl` (attempt 0);
  "Refetch" bumps `coverAttempt` → skips the authored URL and hits the
  keyless providers (Open Library first via `coverCandidates`). Gallery picker
  writes `bookCover`; Remove clears it. Card renders a `BookCoverBadge`
  (44×66 dp jacket, spine + sheen) top-right on every style EXCEPT Collage,
  which feeds the polaroid photo slot (`userPhoto ?: bookCover`).
- Info row: `ShareCardMove` gained `metaWidthFrac`/`metaHeightFrac`;
  `moveMeta` applies the width crop; every style's meta Text now uses
  `lines(2, move.metaHeightFrac, max = 2)` so long byline/year wraps to a
  second line by default and the box tool offers "Info width"/"Info lines"
  sliders when the Info row is selected (META joined `isSizable`).

## Changes
- `AppPreferences.kt` — new `setBookReadingProgressExact` (both directions).
- `TopicRevealScreen.kt` — Mark-read → toggle (Undo glyph when read,
  `ch.number - 1` on un-tap); share-sheet call passes `bookImageUrl`,
  `bookRating`, `bookRatingCount`.
- `BookCoverFetch.kt` — `fetchRatings` skips already-cached books (resume,
  not restart).
- `TopicShareCard.kt` — progress slider (replaces chapter chips) with
  dual-direction writes; `cardFactText`/`factFieldText` unified (custom +
  chapter review render LIVE customText; empty stays empty; routeFactChange
  writes customText for those, editedFact otherwise); tap-fact auto-convert;
  `bookCover` param + `BookCoverBadge` + cover controls row (Change/Refetch/
  Remove) in the content panel; quick-fact rating for book topics;
  metaWidthFrac/metaHeightFrac + 2-line wrap on all meta sites + save/restore
  + box-tool sliders.
- Changelog updated (6 new bullets on top).

## Verification
- Bracket/paren balance checked vs HEAD on all 4 touched files — symmetric
  deltas, balanced.
- Icons verified: Refresh, Close, PhotoLibrary, Undo, Check all exist.
- Imports verified: roundToInt, Offset/Size/Brush, shadow/clip/Alignment.
- No Gradle locally (project rule) — CI compiles on push; watch the Actions
  run for TopicShareCard compile + validateTopics.

## Follow-ups
- Watch the CI run for this push; if it fails, fix and re-push.
- ShareHub / EntryDetail sheet callers keep defaults (no cover/rating there) —
  can thread bookImageUrl/bookRating there later if wanted.