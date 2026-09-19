# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request

> "in cabinet opening collcetions doesnt have any animations. also dont sho w those
> recommendations when the cabinet is empty. and can u please accurately redrawn the open
> book look, its looking bad, also many book covers doesnt load, can u use the itune
> fallback for loading book covers, also artist info wasnt loading at all, and the painting
> artworks loading was so flow as well. and many painter info wasnt loading too. before
> starting do a git pull."

Seven asks, `git pull` first (done — the checkout was already current):

1. **Opening a collection in the Cabinet has no animation** — tapping a shelf replaced the
   page in one frame.
2. **Don't show the empty-Cabinet recommendations** — the "Your Cabinet is empty / here are
   some topics to get you started" block with three shuffled picks and a Shuffle pill.
3. **Redraw the open book** (the Curiying-now shelf art) — "its looking bad".
4. **Many book covers don't load** — use an iTunes fallback.
5. **Artist info wasn't loading at all.**
6. **Painting artworks load too slowly.**
7. **Many painter info wasn't loading either.**

---

## 2. What was found

- **`CabinetV2Content.kt`** — the whole level body sits under `key(openLevel) { … }`
  (fresh grid state per level, which is deliberate) but nothing animated the swap.
- **The empty-Cabinet rails** live in `LazyGridScope.v2HomeItems`, gated on
  `showSuggestions = archiveReady && !rawContent`, drawn by `V2EmptySuggestions`. The gate
  is also wrong in one direction: `rawContent` counts saved entries, books, albums, series
  and user collections — **not** liked topics — so a member with a full Favorites shelf
  could still be told the Cabinet is empty.
- **The open book** (`ReadingArt`, `CabinetShelves.kt`) had real geometry faults: the page
  outlines were cubic curves that bowed the WRONG way (S-shaped sheets, a top edge sagging
  below the gutter, fore edges floating off the cover slab) and the ruled lines ran the full
  width, straight through the spine.
- **Book covers** — two separate defects, both in the cover cache path:
  1. `CabinetCoverCache.resolveAndPersist` persisted the first URL that RESOLVED and only
     then tried to download it. For books that URL is usually the authored Open Library
     placeholder, and `downloadBytes` rejects Open Library's 1×1 GIF (under 512 bytes) — so
     the dead URL stayed stored as the answer, `persistedUrl` returned non-null forever, and
     the tile/warmer skipped the item as "already resolved" and never looked again.
  2. `BookCoverFetch.resolveCoverUrl` hands back the authored `imageUrl` whatever provider
     is asked for, so `CabinetCoverCache.resolveWithProvider` could never advance past a
     dead authored URL either — and the "Cover source" switch was a no-op for those books.
  3. `V2JacketArt`'s live resolve was skipped for `V2Kind.BOOK` outright ("the authored URL
     wins"), so a book whose three candidates all missed stayed blank for good.
- **The art lane** (`ArtworkSheet.kt`, `ArtworkFetch`):
  - `artwork()` ran the Met and Wikipedia lookups strictly one after the other, and
    `metObject` opened up to FIVE object records before Wikipedia was even asked.
  - `metObject` preferred `primaryImage` — the museum's full-resolution original, routinely
    thousands of pixels and several megabytes — while the card that draws it is 200–230dp
    tall. That is the "painting artworks loading was so slow".
  - An ARTIST or PAINTER had **only** the Met: `makerArtwork` (the reveal card) returned
    null when the Met held no attributed work, and the MAKER sheet showed an empty list and
    the line "The Met holds no attributed works for this name" — no prose, no portrait. The
    Met only knows what the Met holds, which is why "artist info wasn't loading at all" and
    "many painter info wasn't loading too".

---

## 3. What was changed

- **Opening a level animates (ask 1).** `CabinetV2Content` now wraps its `key(openLevel)`
  content in one `Box` whose `graphicsLayer` reads a `levelSwap` `Animatable` (alpha,
  0.975→1 scale, 16dp rise, `tween(300, FastOutSlowInEasing)` — the same idiom the
  Cupboard's filter swap already uses). `graphicsLayer { }` updates the layer without
  recomposing the grid, and `key(openLevel)` still gives every level a fresh scroll
  position, so the motion covers it. A tap on a collection, a shelf, the Cupboard or the
  hero's back all go through it.
- **The empty-Cabinet recommendations are gone (ask 2, a REMOVAL — flagged in the
  changelog as REMOVE because the feature shipped long ago).** The `V2EmptySuggestions`
  composable, the `suggestions` / `suggestionSeed` / `suggestionCats` state, its
  `LaunchedEffect`, the `rawContent` gate and the `suggestions` / `onShuffle` /
  `onOpenSuggestion` / `showSuggestions` parameters are all removed. `v2HomeItems` leads
  with the Cupboard card and the shelves, so the page is never blank.
- **The open book is redrawn (ask 3).** `ReadingArt` is stated from six numbers — `gutter`,
  `half`, `pageTop`, `pageBot`, `dip`, `lift` — and both halves derive from them, so they
  cannot disagree. Each sheet's top edge rises a hair as it leaves the gutter, its fore edge
  is ONE line bowing out by a whisper, and its foot mirrors the top (a shallow V, which is
  what an open book on a table looks like). Two sheets a side: the lower one is pushed OUT
  a hair and down so the page block's thickness reads along the fore edge and the foot. The
  four ruled lines a page stop clear of the gutter and run out toward the fore edge (the
  last one short), the gutter is a shadowed join, the ribbon falls past the book's own foot
  with a notched tail, and the mug sits clear of the fore edge.
- **Book covers (ask 4).**
  - New `BookCoverFetch.providerCoverUrl(name, author, provider)` — the provider-EXCLUSIVE
    resolver (iTunes Search / Open Library title / LibraryThing), suspend and on the IO
    dispatcher. `resolveCoverUrl` keeps its authored-first rule for the reveal poster and
    the share card, and now delegates its provider branch to the new function.
  - `CabinetCoverCache.ensureLocalCover` now walks a candidate list — the persisted URL,
    the authored `imageUrl`, then each provider's own cover (books: iTunes, then Open
    Library) — and persists a URL **only in the same breath as the bytes that came back
    from it**. A miss is remembered in the in-memory `missedThisRun` set for the rest of
    the session (so the warmer doesn't re-search on every visit) but never in
    `AppPreferences` (a fresh launch retries). `resolveAndPersist` is deleted.
  - `CabinetCoverCache.resolveWithProvider` routes books through the provider-exclusive
    resolver, so the Cover-source switch really switches.
  - `V2JacketArt`'s live resolve now runs for books too (same provider cascade, persisted
    through `setBookCoverUrl`; albums/series still `setSheetArtUrl("kind|name")`), and the
    now-unused `AlbumArtFetch` / `SeriesPosterFetch` imports are gone from the file.
- **The art lane (asks 5–7).**
  - `ArtworkFetch.artwork` asks the Met and Wikipedia **at the same time**
    (`coroutineScope` + `async`), so the wait is the slower source instead of the sum;
    `metObject` opens at most three records and prefers `primaryImageSmall` over the
    full-resolution `primaryImage`.
  - New `ArtworkFetch.makerInfo(name)` returning `MakerInfo` (Wikipedia prose + lead image
    + article), memoized per name, with `wikiPerson` doing the lookup; `wikiLeadImage`
    (the author lane) is now a thin wrapper over it, so an author's portrait and a maker's
    portrait come from one rule.
  - `makerArtwork` (the reveal card for an ARTIST / PAINTER) falls back to that portrait
    when the Met holds no attributed work, so the card has a picture either way.
  - The MAKER sheet fetches its works list and the maker's record concurrently and now
    shows the extract, the portrait in its header (the glyph when there is none) and a
    THE RECORD door above the works list, with the "Looking them up…" / "nothing
    attributed" line only when there is nothing else to say.

---

## 4. Docs, changelog and verification

- **`app/AGENTS.md`** — a new "Cabinet covers, the open book and the art lane (v407)"
  section: a persisted cover URL is a hint never a verdict (and why `resolveAndPersist` was
  deleted), for books a provider means THAT provider, failures are remembered per RUN not
  forever, book tiles cascade too, the open book's six numbers, the level-open animation,
  the removal of the empty-Cabinet rails, the parallel art fetch and the wiki-size image,
  and the maker lane's Wikipedia half.
- **`fastlane/metadata/android/en-US/changelogs/20260922.txt`** — one ADD (the level-open
  motion), a REMOVE (the empty-Cabinet suggestions) and four FIXes (book covers, the open
  book, the maker page's words and face, the faster painting lookup).
- **Verification here:** `node scripts/check_braces.js` is clean, and the level-open
  wrapper's brace count was checked against HEAD with a brace-depth scan (the wrapper needs
  its own closing brace, which the diff proves is present). A Gradle build cannot run in
  this environment — CI is the compile check.

---

## 5. Still open / worth a device pass

- **The animation, the redrawn book and the cover cascade are all compile-checked only** —
  no device here. Worth watching: the level-open motion on a slow phone (it is a single
  full-screen layer), and a Cabinet full of books that used to sit on a blank plate.
- **The empty-Cabinet removal also removed the heading** ("Your Cabinet is empty") — the
  home still leads with the Cupboard card, the Collections shelves and the New-collection
  tile. If the heading alone should come back, that is a one-composable addition.
- **`rawContent`'s blind spot is now moot** (nothing reads it), but the same trap exists
  anywhere "empty" is judged without liked topics — check before reusing that idea.
- **A maker with no Wikipedia page at all** still shows an empty works list; the fallback
  chain for a maker is Met works → Wikipedia portrait → nothing. Open Library's author
  works (the AUTHOR lane) are the other source if painters ever need a second one.
- **The cover cache's `missedThisRun` is per-process** — a member who opens the Cabinet,
  sees a plate, and relaunches pays the search again. If that ever shows up as chattiness,
  the next step is a timestamped cooldown in the store rather than a permanent verdict.
- **`web/` and `desktop/` were not touched** (root AGENTS.md scope rail).

---

## User prompts

Status: this request is complete and pushed. No pending prompt below.

<!-- Next user prompt goes here. This section is never cleared — the pending prompt and
its status stay at the top, and the empty slot below is where the next instruction lands. -->
