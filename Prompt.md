# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request

> "somehow the book covers are not loading in the book page now, and also the look up is slow,
> what happend the look up should do look up in open library first check all, also some times
> the series or anime data is already shown in preview but it loads again when the page opens"

### Clarified with the member (ask_user, this session)

1. **Where the covers are missing:** "all of it, my book shelf mainly — it should fetch."
   So the fix is not only "carry the cover along at creation" but "resolve a missing cover".
2. **The series/anime reload is:** opening the **episode sheet** from a card that had already
   shown its episode chips.
3. **Look-up UX:** keep it **silent** (no visible "checking…" note). Only the tapped pill reports.

## 2. What the code actually looked like (findings)

- **The shelf bridge created a book with no cover at all.** `TopicRevealScreen`'s bridge (v408,
  "the reveal's book sheet is the book page now") wrote `PersonalBookEntity(title, author,
  catalogId)` — **no `coverUrl`, no `pageCount`, no `totalChapters`.** So a book opened from a
  book topic landed on the shelf/book page wearing the code-generated plate, and its progress
  card had nothing to count against. That is the reported "covers are not loading".
- **`BookCover` painted ONLY the row's `coverUrl`** (`rememberAsyncImagePainter(coverUrl)`), so
  art the app had already downloaded (the Cabinet's `cover_cache`) was invisible to the shelf,
  the book page, Home and the Cabinet's personal shelf.
- **The look-up asked Open Library three separate times for the same title:**
  `openLibraryChapters` (title search + editions read + sometimes a work read),
  `openLibraryPages` (a SECOND search), `openLibraryDescription` (a THIRD search + its own work
  read) — up to five sequential round trips on the critical path of a first open. And a book
  whose `catalogId` had a real chapter list in the topic JSON was *still* sent to Open Library
  for one.
- **The episode sheet seeded itself from `topic.episodes` only.** The anime card fetches the
  guide to draw its chips (`AnimeEpisodeFetcher.fetchAll`), then `EpisodeNotesSheet` opened on
  the authored list (usually empty) and fetched the very same list again — memoised, so fast,
  but it rendered "No episode guide yet." first. `SeriesEpisodeFetcher` kept only the raw JSON,
  so there was nothing in the sheet's own shape to seed from.

## 3. What was done

### Covers (the member's "it should fetch")

- **`TopicRevealScreen`'s shelf bridge** now carries the topic's own `imageUrl`, `pageCount` and
  chapter count (all already in hand — no lookup, no network).
- **`BookCoverWarmup` (new, `features/personal/`)** is the ONE resolver for a book with no
  cover: the catalogue's own cover by `catalogId` (local, adopted even with fetching OFF), else
  `CabinetCoverCache.ensureLocalCover` — the v407 verified-bytes cascade (persisted → authored →
  iTunes → Open Library) — and the URL is written onto the **book's row** through the new
  column-scoped `PersonalDao.setCoverUrl` / `PersonalRepository.setCoverUrl`. Callers bump
  `CabinetCoverCache.version` once per batch.
- **`BookShelfScreen`** warms every coverless book on the shelf (quietly, on IO, one at a time);
  **`BookDetailScreen`** warms the one book it opened.
- **`BookCover`** renders the art the app already has (`CabinetCoverCache.localCoverFile`, read
  through the cache's version) before the row's `coverUrl`, with the generated cover still
  underneath as the loading/error state.

### The look-up (one Open Library visit)

- **`BookEnrichment.openLibraryPass`** replaces `openLibraryChapters` / `openLibraryPages` /
  `openLibraryDescription`: ONE title search (`key,title,number_of_pages_median`) and ONE work
  read, only when the description or a table of contents is wanted, plus at most one editions
  read for the table. `richestEditionTable` keeps the v389d "richest table wins, page ranges
  preferred, work as the second door" rule.
- **A catalog book's chapter list closes the chapter question** — `wantChapters` is false when
  `catalogId` resolves to chapters in the topic JSON (they are what the book page reads back).
- Crossref is asked only when Open Library had no table at all; Google Books still only when its
  key is configured.

### The episode sheet (no reload)

- `SeriesEpisodeFetcher` gained a `seriesCache` (the MAPPED list, beside the raw JSON memo) and
  `cached(name)`; `AnimeEpisodeFetcher` gained `cached(name)`.
- `EpisodeNotesSheet` seeds `episodes` from that cache (authored episodes still win) and only
  runs the provider fetch when nothing is known; a **provider's** list is never run through
  `enrich` any more (that merge is for the authored list — its season/number match is keyed to
  TVMaze's own numbering). The anime card seeds its chip row the same way.

## 4. Still open / worth a device pass

- **Nothing here is device-verified.** No Gradle in this environment: `node scripts/check_braces.js`
  passes (282 files), the diff was import-swept and read back; **CI is the compile check**.
- Worth watching on device: the shelf's first open with several coverless books (the warm pass is
  sequential on IO; `missedThisRun` stops it re-searching inside a session), the book page's first
  frame for a book that gets its cover fetched mid-open, and whether the reveal's episode chips now
  stay put when the sheet opens.
- The look-up's own consent gate is unchanged (`bookFetchEnabledState` for metadata, the merged
  `coverFetchEnabledState` for covers) — with fetching off, only the catalogue's own cover is
  adopted, and the look-up reports "off in Settings" exactly as before.
- Pre-v410 shelf books with a blank `coverUrl` get their cover on the next shelf/book-page visit;
  there is no migration and none is needed (the row is written when the art is found).

---

## User prompts

Status: **done — committed and pushed on `main`.**

> "somehow the book covers are not loading in the book page now, and also the look up is slow,
> what happend the look up should do look up in open library first check all, also some times
> the series or anime data is already shown in preview but it loads again when the page opens"

Clarified with the member in the same round (cover scope / which reload / silent look-up), all
three reports fixed as described above. Earlier prompts in this session are recorded in git
history (`69e6c6f5`, `aa820f20`, `532a3dc3`, `b3c4c73f`, `301fac2b` and the ones before them).

<!-- Next user prompt goes here. This section is never cleared — the pending prompt and its
status stay at the top, and the empty slot below is where the next instruction lands. -->
