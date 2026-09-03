# Prompt.md — current request log

## Request: album track lists on Topic Reveal + sheet restyle + keyless album-art fetch

User flow across the album workstream: albums.json gained real per-track lists
(number/title/duration, 999/1000 albums, ~12,375 tracks — from the earlier
track-extraction session), then the reveal needed an ALBUMS info section that
"mirrors books exactly", plus two asks answered via ask_user:
- Sheet background: **category-tinted wash + top hairline** (both the book
  notes sheet and the new album track-list sheet looked like a foreign
  neutral panel over the category-washed reveal page).
- Album art: **iTunes Search API first, MusicBrainz + Cover Art Archive
  fallback** — both keyless; **reveal/sheet on-the-fly fetch only** (no
  Settings bulk hub like books).

## Implementation

### Data layer — `tracks` threaded end to end (Room persisted)
- `CurioTopic.kt` — new `AlbumTrack(number, title, duration)` model +
  `CurioTopic.tracks: List<AlbumTrack>?` (albums only, null default).
- `TopicJsonLoader.kt` — parses the `tracks` array (optInt/optString with
  defaults, empty array → null).
- `TopicEntity.kt` / `CachedTopicEntity.kt` — `tracks` TEXT column (JSON
  string), Gson round-trip both directions, mirrors chapters pattern.
- `CurioDatabase.kt` — version 11 → 12 + `MIGRATION_11_12` (ALTER TABLE adds
  `tracks TEXT NOT NULL DEFAULT ''` to `topics` and `cached_topics`).
- `TopicDao.kt` — `backfillContent` + `updateContent` gained the tracks arg.
- `TopicRepository.kt` — catalog sync backfills tracks; reveal hydration now
  also triggers for ALBUMS rows whose tracks are blank.

### Keyless album-art resolver — `features/reveal/AlbumArtFetch.kt` (new)
- iTunes Search API (`entity=album`, term = album + artist): picks the best
  title/artist match, upscales `100x100bb` → `600x600bb`.
- Fallback: MusicBrainz release-group search (proper UA, 1 req/s politeness)
  → Cover Art Archive `/release-group/{id}/front-500` status probe.
- In-process memo keyed `album|artist` ("" = known miss) so reopens never
  re-query; Coil's disk cache holds the bytes.

### Reveal UI (`TopicRevealScreen.kt`)
- `AlbumInfoSection` (below the hero for ALBUMS w/ tracks, gated on the same
  post-morph `contentUiReady` delay the book section uses): TRACKLIST card
  (artwork + artist + first-5 track preview + runtime/count + "View the full
  track list →") plus a scrollable track-chip row for jumps.
- `AlbumNotesSheet` — full-height ModalBottomSheet mirroring `BookNotesSheet`:
  artwork + album/artist header, runtime line, LazyColumn of every track
  (number/title/duration) with the opened track scrolled to + highlighted.
- `AlbumCoverPoster` — on-the-fly resolve via `AlbumArtFetch` with a tinted
  Album-glyph placeholder tile while loading / on miss.
- `NotesSheetTopHairline` shared by both notes sheets.

### Sheet restyle — `CategoryInk.kt`
- `CurioCategory.notesSheetContainerColor()`: category-tinted elevated wash
  (dark = near-black hue at 0.27 lightness, light = airy accent pastel) with
  Material theme + the manual tint toggle falling back to the neutral dialog
  container. `BookNotesSheet` + `AlbumNotesSheet` both use it, plus the top
  hairline. (Renamed `bookUiReady` → `contentUiReady` since albums use it.)

### Changelog
- `fastlane/.../changelogs/20260921.txt` (current versionCode) — two ADD
  bullets added at the top: Albums TRACKLIST on reveal + keyless art fetch,
  and the category-tinted notes sheets + hairline.

## Verification
- Balance-checked every touched Kotlin file vs HEAD (brace/paren deltas all
  zero after stripping strings/comments).
- Albums JSON spot-checked: all 1000 have tracks arrays; every track carries
  number/title/duration (Gson-safe for the non-null String field).
- Both keyless endpoints live-probed with curl before coding (iTunes returns
  resizable artwork; MusicBrainz + CAA front-500 307s to the image).
- No Gradle locally (project rule) — CI compiles on push.

## Follow-ups
- Albums still carry no authored `imageUrl`; if authored art lands later, the
  poster should prefer `topic.imageUrl` before the resolver (cheap add).
- If per-track detail (lyrics / "genius link") is wanted later, that's a JSON
  schema addition (new field per track) + a row action in AlbumNotesSheet.
