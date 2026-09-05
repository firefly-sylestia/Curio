# Request Log — album deep-link accuracy + roadmap (synopsis batch, series enrichment)

## Status: album link fix complete — committing; next: book synopsis batch, then series enrichment

## The request (user)
"the direct albumn open links are not accurate and it gives no result
like oens blank apple music, so any way to fix" — plus instruction to
then do the book synopsis fix ("many books dont have proper synopsis,
start with 30 per batch, proper web searched sunopsis detailed, no need
to ask") and then series enrichment.

## Diagnosis (verified live against the iTunes API)
`resolveAppleMusicItemUrl` took `results[0]` of a `limit=1` album search
with NO relevance check. The search API's ranking is unreliable for
famous catalogs:
- "Nirvana Nevermind" → real album NOT in top 25; top hits are tribute
  albums and "Nevermind - Single" by Dennis Lloyd (another artist).
- "Pink Floyd The Dark Side of the Moon" → real album NOT in top 25; top
  hit is "Jenny of Oldstones" by a band NAMED "The Dark Side of the Moon".
- "Pink Floyd …" with the album search also surfaced "The Wall" first.
So LISTEN opened wrong albums (or blank pages) — the user's report.

## Fix (ExploreSearch.kt, v364)
- Album path: resolve artist ID (musicArtist search, exact name) → pull
  the artist's OWN catalog (`/lookup?id=…&entity=album&limit=200`) →
  best title match with `appleAlbumScore` gate >= 25 (35 exact+artist,
  30 exact, 25 containment+artist), tie-break `trackCount > 0` (preorders
  render blank). Verified live: Nevermind 35, Dark Side 35, Sgt. Pepper
  35, Led Zeppelin IV "(Remastered)" 25.
- Fallback: SCORED search (limit=10, same gate); null → caller's search
  link (never a wrong deep link).
- Song/artist paths untouched (songs verified working). Both call sites
  (reveal "Listen in" + album sheet LISTEN pill) use this resolver.
- Spotify: field-scoped quoted query (`album:"…" artist:"…"`) + gate >= 2
  (was > 0) so same-title other-artist items can't win.

## Next (user-ordered, no further questions)
1. Book synopsis fix — first batch of 30 books whose synopses are
   missing/short/weak; web-research and write detailed synopses.
2. Series enrichment — continue batches (synopsis + episode lists).