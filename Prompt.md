# Prompt — Topic Browser: slow search, two-tier (exact + smart) results, Also-in accuracy, cover toggle

## Request
1. Search results are still slow in Topic Browser — is it the "smart"
   (fuzzy) search? Asked: do the exact-word search; only when it yields
   nothing fall back to the smart search, and show both searches.
   (Clarified with the user: **show both — Exact matches on top, extra
   typo-tolerant "Similar matches" below as their own section**.)
2. "Also in" pills are inaccurate — the category being searched sometimes
   doesn't show; arrange so whoever has the matching TITLE shows first.
3. Book covers are loading even when the fetch toggle is OFF — fix.

## Root causes
1. **Slow search** — per settled query the screen ran TWO independent
   full-catalog scans (catHitCounts + rows) and, inside each, the fuzzy
   pass re-split every topic's name/byline/subtype with a Regex on every
   call (thousands of topics × 3 fields × per scan). Sorting also ignored
   how directly the TITLE answered the query once fuzzy hits were mixed in.
2. **Also-in** — pills were the top-6 lanes by raw hit count, so lanes
   owning an exact title match (count 1) lost to high-count fuzzy lanes.
3. **Covers** — `BookCoverPoster` always fetched (authored URL then the
   Open Library fallback) and the reveal's ★ rating hit Google Books
   unconditionally; `isBookFetchEnabled` was never consulted outside the
   bulk hub.

## Changes
1. **TopicDatabaseScreen.kt** — search is now ONE catalog scan per settled
   needle (`SearchPass` produceState replaces the double produceState):
   - word lists pre-split ONCE per topic at index build (`IndexedTopic`
     nameWords/bylineWords/subtypeWords); fuzzy checks run against the
     retained lists, no per-query Regex splits.
   - matches split into `exact` (substring) and `similar` (typo) groups;
     both sorted title-first via a new `titleRank` (exact title →
     startsWith → title contains → fuzzy title → other-field match), then
     lane-mention priority, then name. Rows render under two labelled
     dividers: "Exact matches (N)" then "Similar matches (N)"
     (`DatabaseRow.groupHeader` + `SearchGroupHeaderRow`); pagination and
     content types updated to carry the dividers.
   - per-lane totals AND exact-title counts accumulate in the same pass →
     the filter-panel chip counts and the "Also in" pills come from
     `SearchPass.laneHits` / `laneExactTitles` (pills sorted: lanes with a
     title match first, then by hit count, capped 8).
2. **TopicRevealScreen.kt** — cover + rating consent:
   - `BookCoverPoster` reads `AppPreferences.bookFetchEnabledState` and
     sets `ImageRequest.networkCachePolicy(DISABLED)` when the toggle is
     off — Coil serves only cached covers and never reaches the network.
   - the reveal's on-demand ★ rating fetch is gated on the same toggle
     (it hits Google Books).
3. Changelog updated (Topic Browser search groups + Also-in + cover
   consent + the earlier perf bullets).

## Verification
- No Gradle in this environment (forbidden; CI validates on push).
- Brace/paren balance: both edited files report zero NEW imbalance vs the
  HEAD baseline (TopicRevealScreen has a clean 0 delta; TopicDatabaseScreen
  symmetric +56 parens/+2 braces/+3 brackets).
- Full diff reviewed; greps confirm no stale `catHitCounts` /
  `fuzzyContains(` / old `RankedHit` shape references remain.

## Status
Complete. Committed and pushed. Notes for the follow-up: `topicsByCat` is
only a produceState invalidation key (kept harmless); fuzzy scan still
runs on every settled query per the user's "show both searches" choice —
the pre-split word lists keep that pass cheap.
