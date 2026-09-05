# Request Log — album deep links, book synopsis batch 1, series batch 3

## Status: all three done — committing & pushing (CI will validate)

## The request (user)
"the direct albumn open links are not accurate and it gives no result
like oens blank apple music, so any way to fix ... after that do series
enrichment and before that do the book synopsis fix as many books dont
have proper synopsis and start with 30 per bath proper web searched
sunopsis detailed. no need to ask me anything first"

## 1. Album deep links (ExploreSearch.kt, v364) — DONE
Root cause (verified live against the iTunes API): `resolveAppleMusicItemUrl`
took `results[0]` of a `limit=1` album search with no relevance check, and
the search API's ranking is unreliable for famous catalogs. The real
"Nevermind" (Nirvana) and "The Dark Side of the Moon" (Pink Floyd) don't
appear in the top 25 search hits at all, so deep links opened tribute
albums, same-title singles by other artists, or a different album ("The
Wall"), which reads as a wrong or blank Apple Music page.
- Album path now: artist ID (musicArtist search) → artist's OWN catalog
  (/lookup?id=…&entity=album&limit=200) → best title match with a strict
  score gate (appleAlbumScore >= 25: 35 exact+artist, 30 exact, 25
  containment+exact artist), tie-break trackCount > 0. Verified live:
  Nevermind 35, Dark Side 35, Sgt. Pepper 35, Led Zeppelin IV 25.
- Fallback: scored search (limit=10, same gate); null → caller's search
  link. Song/artist paths untouched (songs verified working). Both call
  sites (reveal Listen in + album sheet LISTEN) fixed.
- Spotify: field-scoped quoted query (album:"…" artist:"…") + gate >= 2
  (was > 0). Committed + pushed (6d090115).

## 2. Book synopsis batch 1 (tools/enrich_book_synopses_batch1.py) — DONE
All 796 books had synopses, but the 30 shortest were thin blurbs. Rewrote
them with detailed web-verified synopses (1003-1317 chars, house style,
no em/en dashes): Predictably Irrational, Nudge, Tribe of Mentors,
Meditations, Ego Is the Enemy, A Suitable Boy, The Thursday Murder Club,
Stillness Is the Key, The Tao of Pooh, The Tao Te Ching, The Midnight
Mass Murders, The Graveyard Book, The Art of Loving, So Good They Can't
Ignore You, Cannery Row, Beyond Good and Evil, Wild, Mere Christianity,
Narrative of Frederick Douglass, Small Gods, Norse Mythology, The Martian
Chronicles, Algorithms to Live By, Less, Our Town, The Invention of
Morel, The Road Less Traveled, Anxious People, Tarzan of the Apes, The
Elegant Universe. Diff is exactly 30 synopsis fields (books.json format:
2-space indent, no trailing newline). Rerunnable; FORCE=1 to redo.

## 3. Series enrichment batch 3 (tools/enrich_series_batch3.py) — DONE
Synopsis + first-season episodes for Breaking Bad (7), Stranger Things
(8), Game of Thrones (10), The Wire (13), The Sopranos (13), titles
verified against episode guides. 15 shows total now carry episode data.
Next batches available: Seinfeld, Twin Peaks, The X-Files, Lost, The
Office (US), Friends, The Good Place, Black Mirror, etc.

## Docs
- app/AGENTS.md: v364 (album links) + v365 (book synopses + series 3).
- fastlane changelog 20260921.txt: album FIX bullet + book/series ADD
  bullets.
- Prompt.md: this log.

## Verification
- ExploreSearch.kt balanced; album fix validated against the live iTunes
  API (artist-catalog scoring reproduced in python).
- books.json + series.json JSON-valid; changed entries exactly the batch
  ids; no em/en/curly chars in authored text. CI will validate the app.