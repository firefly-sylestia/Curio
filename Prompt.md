# Request Log — Album LISTEN pill: Apple Music opens the real album (not a search)

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user)
"for album open links in apple music it's searching only, same in play in for
albums too from explore, but for song it perfectly opens the song inside the
album, so can't we do the same for albums too, and maybe for other services
too"

## Findings
- The album sheet's LISTEN pill (`albumListenUrl`) built SEARCH deep links
  for every service — Apple Music included. That is the "searching only"
  surface.
- Songs deep-link because the Explore dialog resolves Apple Music items via
  `resolveAppleMusicItemUrl` (iTunes lookup → `trackViewUrl`, album page +
  `?i=trackId`).
- Explore ALREADY resolves albums for Apple Music (entity=album →
  `collectionViewUrl`; verified live: returns
  `https://music.apple.com/us/album/revolver-2022-mix/1642995371`); albums
  only "search from explore" when the chosen music service isn't Apple
  Music (default = YouTube Music) — no code change needed there.
- Other services have NO keyless album-ID lookup: Spotify Web API needs
  OAuth client-credentials; Deezer now requires login/app registration;
  YouTube Music / Amazon Music expose none. Apple Music (via the iTunes
  Search API) is the only keyless album deep link.

## What was done (TopicRevealScreen.kt, AlbumNotesSheet)
- LISTEN → Apple Music: `onClick` now launches a coroutine that calls
  `resolveAppleMusicItemUrl(topic)` (iTunes lookup → native `music://…/album/
  {id}`) and falls back to the search link only when the lookup misses.
- Added `val scope = rememberCoroutineScope()` to the sheet (import already
  present).
- Spotify / YouTube Music / Amazon Music / Deezer entries unchanged (search).

## Docs
- app/AGENTS.md: **v357** entry (incl. the note that Spotify deep links
  could be added behind optional client-id/secret secrets later).
- fastlane changelog 20260921.txt: 1 FIX bullet at the top.
- Prompt.md: this log.

## Verification
- Braces balanced after edit; the change only touches the LISTEN pill's
  onClick (plus the new scope val) — no signature/import changes. CI will
  validate the real compile.