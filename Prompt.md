# Prompt.md — current request log

## Request: album covers + Genius links + fix the album-reveal compile error

Three asks from the user:
1. Fix the CI compile failure in the album track-list sheet
   (`PaddingValues(horizontal = …, bottom = …)` mixed two overloads —
   `app/src/main/java/com/curio/app/features/reveal/TopicRevealScreen.kt:3713`).
2. Author real album covers into the catalog (like books).
3. Add Genius links for albums.

User answers (ask_user): **album-level Genius links only** for now (per-track
links deferred until the official Genius API token is added); resolve Genius
links by **slug construction now, official API later** (example env var
documented); **author iTunes artwork URLs** into `albums.json`.

## Implementation

### Fix (pushed separately as `6e76c013`)
- `TopicRevealScreen.kt` — `PaddingValues(horizontal = 20.dp, bottom = 8.dp)`
  → `PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp)` (the compile
  error; committed + pushed alone so CI validated it).

### Data authoring — `data/topics/albums.json` (via `tools/enrich_albums_art_genius.py`)
- **`imageUrl`** on 929/1000 albums — keyless iTunes Search API artwork
  (resized `600x600bb`), MusicBrainz + Cover Art Archive fallback via a
  no-download curl `-I` probe (following CAA's 307 redirect was downloading
  full images and hanging the run).
- **`geniusUrl`** on 1000/1000 albums — canonical
  `https://genius.com/albums/<artist-slug>/<album-slug>` (slug rule: lowercase,
  strip punctuation, `&`→and, spaces→hyphens). Official Genius API validation
  is wired in but dormant — set `GENIUS_API_TOKEN` (a *client access token*
  from genius.com/api-clients, exported in a local un-committed `.env` /
  shell env — `.env` is gitignored) to verify each URL and prefer the API's
  canonical URL; without the token the constructed slug is kept.
- Batching: the tool reads the FULL file each run and rewrites it; run in
  200-album batches with `--offset N --limit 200 --apply`
  (`tools/run_album_enrich.sh` wraps the 5 batches). This env kills detached
  processes between tool calls, so batches must run synchronously.

### App — data layer (Room v12 → v13)
- `CurioTopic.geniusUrl: String?` (albums only) + `TopicJsonLoader` parse.
- `TopicEntity` / `CachedTopicEntity` — `geniusUrl` TEXT column + Gson/plain
  round-trip; `CurioDatabase` v13 + `MIGRATION_12_13` (both tables).
- `TopicDao.updateContent` + `TopicRepository` hydration now carry geniusUrl;
  album hydration also triggers when `geniusUrl` is blank (so already-seeded
  rows pick up the new field from the bundled JSON).

### Reveal UI (`TopicRevealScreen.kt`)
- `AlbumCoverPoster` gained `imageUrl: String?` — prefers the authored cover
  (929 albums load instantly, no lookup); falls back to `AlbumArtFetch`
  (on-the-fly iTunes/MB) only for albums without one.
- `AlbumNotesSheet` header — a **GENIUS pill** (OpenInNew glyph + label) next
  to Close when `geniusUrl` is present; opens the album's Genius page via
  `openSearchUrl`.

### Icon font
- `CurioIcons.OpenInNew = "open_in_new"` added; `material_symbols_outlined.ttf`
  regenerated from `tools/fonts/material_symbols_outlined_full.ttf` with
  fontTools (all 198 existing PUA codepoints + U+E895, `--no-layout-closure`).
  Verified: 281 existing ligature rules preserved (0 lost), `OPEN_IN_NEW`
  rule added (glyph `uniE895`), cmap strictly additive.

### Docs + changelog
- `app/src/main/assets/topics/SCHEMA.md` — `tracks` (v332) + `geniusUrl`
  (v333) documented.
- `fastlane/.../changelogs/20260921.txt` — ADD bullet: album covers + Genius
  links.

## Verification
- albums.json: 1000 entries, 0 schema errors (validateTopics-style checks),
  0 malformed URLs; 929 covers + 1000 Genius links; diff is strictly
  imageUrl/geniusUrl additions (+ trailing comma) — 2-space indent preserved.
- Genius pages can't be curl-verified from here (Cloudflare 403 = bot block,
  not 404); slug URLs match Genius' canonical album slug format.
- Font regeneration verified lossless (ligature-set superset, cmap additive).
- No Gradle locally (project rule) — CI compiles on push.

## Follow-ups
- When the user adds a `GENIUS_API_TOKEN`, rerun
  `tools/enrich_albums_art_genius.py` (all 1000, no limit) to validate each
  geniusUrl and replace slugs with official URLs where they differ.
- Per-track Genius links (the original ask) need a `geniusUrl` per track in
  the JSON + a row action in `AlbumNotesSheet` — deferred until the token
  makes 12k lookups practical.
- 71 albums still lack authored covers (obscure/electronic long tail) — they
  fall back to the on-the-fly resolver; can be re-probed later.