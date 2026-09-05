# Request Log — meta-row move clamp, series synopsis data, book-cover fetch fixes

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user)
"during card editing the auhor year etc that info move is bad, like its
good it can be moved with the title and then separate too but the separate
one is bad like it have restriction to move to too much to the sides while
others dont so fix it, also i dont see the new series synopsis style
layout inside, also then the book fetching in experiment fix so when i tap
fetch it fetches fromt the start when the books already have the cover and
when i exit the page during fetch it cancels,and resrt from star, also
many book covers are not getting fetchd, like heaven a handful of dusts, a
perfect spy and many more so can we fix it"

## Answers / decisions (asked user)
1. Series synopsis: **Enrich series data** (author synopsis + episodes for
   a batch of popular series so the layout appears for them).
2. Book fetch: **Skip covered + survive exit** (already-covered books are
   skipped; the fetch keeps running if you leave the page; re-tapping
   resumes where it left off).

## What was done
### TopicShareCard.kt
- META handle clamp: removed the `mPad = 18f` padding restriction — the
  author/year row now clamps edge-to-edge (`-bx, cw - bx - m.width`)
  exactly like title/fact/badge/cover; base-rect clamp preserved.

### BookCoverFetch.kt + BookCoverHubScreen.kt + AppPreferences.kt
- **Root cause found:** ALL 796 books carry an authored `imageUrl`, so
  `resolveCoverUrl` short-circuited to it and the providers never ran; and
  Open Library serves a 1x1 GIF (HTTP 200) for missing covers, so dead
  URLs (A Handful of Dust, A Perfect Spy...) counted as "success".
- New `resolveVerifiedCoverUrl` + `loadsRealImage` (Coil decode, >= 40px
  short edge): per-book candidates = stored-verified → authored →
  provider cascade (chosen first, then iTunes → Google Books → Open
  Library → LibraryThing); first real cover wins + remembered.
- New persisted `bookCoverDoneState` (KEY_BOOK_COVER_DONE): "Fetch all
  covers" skips verified books; re-tap resumes, not restarts.
- New `BookCoverFetchSession` object: process-lifetime scope + Compose
  progress state. Hub no longer uses rememberCoroutineScope — leaving the
  page doesn't cancel; re-entry shows live progress; Cancel works.
- Hub "Retry failed" per-row also routes through the session.

### TopicRevealScreen.kt
- `BookCoverPoster.onSuccess` now checks decoded size: tiny/placeholder
  successes bump the candidate index like a 404 (Open Library 1x1 GIF
  previously won with no onError). Candidate order is verified-first to
  match `coverCandidates`.

### Series data batch 2 (tools/enrich_series_batch2.py)
- Synopsis + full episode lists for **Sherlock (13), Squid Game (9), The
  Last of Us (9), Severance (9), Wednesday (8)** — 10 series total now
  render the reveal series card + episode-list sheet (was 5).
- No em/en dashes; episode entries validated (season/number/title/summary).

### Docs
- app/AGENTS.md: **v360** entry.
- fastlane changelog 20260921.txt: FIX/ADD bullets at the top.
- Prompt.md: this log.

## Verification
- Braces/parens balanced (TopicShareCard + TopicRevealScreen +1 paren
  deltas are pre-existing at HEAD — verified via git stash).
- series.json parsed + episode schema validated; 10 series with data.
- `coil.ImageLoader` type confirmed against MainActivity usage. CI will
  validate the real compile.