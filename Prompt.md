# Request Log — Spotify album/track/artist deep links (optional keys) + album-match confirmation

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user)
Two parts:
1. "also the album open opens the same inside the bottom sheet right?" —
   confirming the LISTEN deep link opens the SAME album shown in the sheet.
2. "and yes app spotify deep links too" — add Spotify deep links.

## Answers / decisions
- Yes, the deep link opens the album that matches the sheet's artist +
  title (top iTunes/Spotify hit). It hands off to the external music app
  (like songs) — not inside Curio. Caveat: it can land on a different
  EDITION of the same album when the exact one isn't on the service (e.g.
  "Revolver (2022 Mix)" vs the 1966 original) since the catalog has no
  stored service ID.
- Spotify deep links implemented behind OPTIONAL `SPOTIFY_CLIENT_ID` +
  `SPOTIFY_CLIENT_SECRET` (client-credentials flow — app-level token, no
  user auth). Unset → null → search links stay. Same pattern as the other
  optional keys (GOOGLE_BOOKS_API_KEY / LIBRARY_THING_API_KEY).

## What was done
### ExploreSearch.kt
- `resolveSpotifyItemUrl(topic)` (suspend): POST accounts.spotify.com/api/
  token with Basic id:secret → access_token (no caching — per lookup, like
  the Apple path); then api.spotify.com/v1/search?q=artist+title&type=
  album|track|artist&limit=5, best match via `spotifyMatchScore`
  (exact title+artist=3 …), returns https://open.spotify.com/{type}/{id}.
- `spotifyAppToken(...)` + `spotifyMatchScore(...)` private helpers.

### TopicRevealScreen.kt
- LISTEN pill: the Spotify entry now resolves like Apple Music
  (`resolveSpotifyItemUrl(topic)` ?: search) inside the sheet's scope.
- Explore "Listen in": the resolve-then-search branch now covers Spotify
  too (albums/tracks/artists deep-link when keys are set).

### Build / CI
- `app/build.gradle.kts`: SPOTIFY_CLIENT_ID + SPOTIFY_CLIENT_SECRET
  BuildConfig fields (env → escaped, mirrors the other optional keys).
- `.env.example`: documented.
- `.github/workflows/android.yml`: both secrets passed to the build env.

### Docs
- app/AGENTS.md: **v358** entry.
- fastlane changelog 20260921.txt: ADD bullet at the top.
- Prompt.md: this log.

## Verification
- Braces balanced; the resolve funcs mirror the existing
  resolveAppleMusicItemUrl structure (HTTP patterns + JSON accessors);
  Base64 via android.util.Base64 (fully qualified, no import added). CI
  will validate the real compile.