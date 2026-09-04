# Request Log — Series reveal UI + keyless TVMaze/iTunes posters + per-category cover fetching

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user, paraphrased)
Start the **web series** feature: series topics should behave like books and
albums — real info in the JSON and a reveal UI plus a sheet showing it. Also
add **keyless series poster fetching** so the show poster can be resolved
from cover-art providers without an API key. Do those two together.

## Clarifications from the user
- Series sheet: **album-style one scroll**, synopsis collapse at the top,
  **episodes below as their own collapsible rows**, with **like (heart) +
  watched (read) buttons** — the same pattern as the book sheet's
  favorite heart + read toggles.
- Poster fetch: **keyless** — no API key — matched to books/albums (which
  already have existing keyless flows).
- Cover fetching: user asked for separate toggles per category (books /
  albums / series) switchable independently in Settings.

## What shipped (commit `3ba0da9e`)

### 1. Series reveal section + episode-list sheet
- New `SeriesInfoSection` on the reveal (behind `contentUiReady` like the
  book + album sections): a poster card labeled EPISODES with season + count
  meta, the poster, creator (byline), the authored synopsis preview (or a
  short episode-title preview when no synopsis exists), and a "View the
  episode list →" affordance.
- `EpisodeNotesSheet` (album-style, one `ModalBottomSheet`):
  - Header: poster + "SERIES NOTES" label + topic title + "Created by <byline>",
    a **favorite heart for the whole series** (`AppPreferences` series
    favorites, heart per show name) + close.
  - Pinned **watched-progress rail** above the list (N of M episodes, accent
    bar).
  - The synopsis accordion **reused from `BookSynopsisAccordion`** with the
    label "ABOUT THIS SERIES".
  - Episodes **grouped by season** — a "SEASON N" subheader with per-season
    watched count, then every episode as an expandable row (episode number
    chip, title, summary, Watched / Undo pill).
- Persisted series data: `AppPreferences` now has **series favorites**
  (toggle per show name) and **series watched progress** (one "S1E3" key
  per episode, stored as show → set of keys), both reactive so toggles + the
  progress rail update in the sheet like the book/album equivalents.
- Episode identity: `SeriesEpisode.key() = "S${season}E${number}"` (a small
  extension the sheet + watched store use for the key format).

### 2. Keyless series poster fetching (`SeriesPosterFetch.kt`)
- Compact standalone file modeled on `AlbumArtFetch`/`BookCoverFetch`.
- **TVMaze first** (`api.tvmaze.com/singlesearch/shows?q=…`, poster from
  `image.original` / `image.medium`, no key), **iTunes Search fallback**
  (`media=tvShow`, `entity=tvSeason`, artwork upscaled 100→600px — same
  proven pattern as albums).
- Same memo pattern: `ConcurrentHashMap` key = stripped show title, so
  reopening never re-queries; Coil disk cache holds the bytes.
- Authored `imageUrl` still wins; the keyless resolver only runs when the
  **series cover-fetch toggle is ON**.

### 3. Per-category cover-fetch toggles
- `AppPreferences`: new per-category keys (`KEY_ALBUM_FETCH_ENABLED`,
  `KEY_SERIES_FETCH_ENABLED`) + getters/setters + reactive states
  (`albumFetchEnabledState`, `seriesFetchEnabledState`) beside the existing
  book toggle; series favorites + series watched states added.
- Reveal gating: every poster/path now checks its **own category toggle**
  before touching the network — book poster (`BookCoverPoster`-side paths
  already did via the book toggle), the new series poster,
  and the series sheet's palette lookup. Albums' existing `AlbumArtFetch`
  path now gates on `albumFetchEnabledState` too (was always-on before). The
  episode sheet's cover-swatch lookup uses `seriesFetchEnabledState`.
- Settings UI (`UserExperimentsScreen.kt`): one "Cover fetching" section
  with three `ExperimentSwitchRow` toggles in series — Books → Albums →
  Series — all OFF by default (no downloads without explicit consent).

### 4. Reused existing UI pieces correctly
- `BookSynopsisAccordion` gained an optional `label` param ("ABOUT THIS BOOK"
  default, "ABOUT THIS SERIES" from the series sheet) instead of a second
  nearly-identical accordion.
- `AlbumCoverPoster` and `AlbumNotesSheet` got the album-toggle gate
  (already had no gating; now OFF by default mirrors books/series).

### 5. Structural sanity
- New file + 4 edited files, a single `SeriesEpisode.key()` extension remains
  correctly scoped to the reveal sheet path; imports resolve; no leftover
  dangling references.

## Not yet done
- Web-research + author the next series batches with synopsis + full episode
  lists (199 series in `series.json`; first 6 were written earlier in the
  conversation and keep their structures).
- Series watch-on/stream actions inside the sheet (Apple Music / Spotify /
  YouTube Music / Amazon / Deezer don't apply to TV series — would need a
  "Watch" pill pointing to the right video service; separate ask).
- General "share this series" card drawing on the series favorite + episode
  data once the series share card path exists.

## Version note
versionName 1.1.1, versionCode 20260921.
