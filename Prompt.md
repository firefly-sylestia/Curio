# Request Log — Book-cover providers: iTunes first + LibraryThing

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user)
"add i tunes provider for books and make that first and then fallback other,
and also add, lirary thing too" — follow-up to the v355 notes-sheet cleanup
(already pushed as 9aa243ce).

## Ask / decisions
- User picked iTunes as the FIRST provider; the others fall back after it.
- LibraryThing added too — it is ISBN-based and key-gated, so it degrades
  gracefully when no key is configured (row hidden, lookups fall through).

## What was done
### BookCoverFetch.kt
- `BookCoverProvider` enum reordered BEST-FIRST: **ITUNES → GOOGLE_BOOKS →
  OPEN_LIBRARY → LIBRARY_THING** (new entries: ITUNES "Keyless ebook
  search", LIBRARY_THING "ISBN covers · free key").
- `itunesThumbnail(title, author)`: keyless `itunes.apple.com/search?term=
  …&entity=ebook&limit=10`, relevance-scored via `matchScore` (copied from
  AlbumArtFetch), artwork token `100x100bb` → `600x600bb`.
- `libraryThingCover(title, author)`: requires `BuildConfig.
  LIBRARY_THING_API_KEY`; resolves ISBN via keyless Google Books
  `industryIdentifiers` (`resolveIsbn`), then
  `covers.librarything.com/devkey/{key}/large/isbn/{isbn}`. Null without
  key/ISBN → caller falls through.
- `resolveCoverUrl` gains the two new provider branches.

### TopicRevealScreen.kt (BookCoverPoster)
- Live INPUT-SIDE fallback now cascades **iTunes → Google Books →
  LibraryThing** (first non-null wins) instead of Google Books alone.

### Defaults / hub (AppPreferences.kt, BookCoverHubScreen.kt)
- `getBookCoverProvider` default → "ITUNES" (state seed updated too).
- Hub `provider` resolution defaults to ITUNES; a stored LIBRARY_THING pick
  with no key silently falls back to iTunes.
- Provider picker hides the LibraryThing row when the key is blank
  (BuildConfig.LIBRARY_THING_API_KEY), so no keyless user sees a
  guaranteed-fail source.

### Build / CI
- `app/build.gradle.kts`: new optional `LIBRARY_THING_API_KEY` BuildConfig
  field (env → escaped string, mirrors GOOGLE_BOOKS_API_KEY).
- `.env.example`: LIBRARY_THING_API_KEY documented.
- `.github/workflows/android.yml`: secret passed to the build env.

### Docs
- app/AGENTS.md: **v356** versioned entry.
- fastlane changelog 20260921.txt: 2 ADD bullets at the top.
- Prompt.md: this log.

## Notes
- iTunes covers are the DEFAULT source but only take effect for fresh
  installs / unknown stored values (existing users keep their saved pick —
  they can switch in the hub, where iTunes now lists first).
- LibraryThing returns a default placeholder image for cover-less books
  (same as Open Library's default-cover behavior) — accepted as a cached
  cover rather than a failure.

## Verification
- All edited regions re-read after replacement; BookCoverFetch braces
  balanced; `BuildConfig.LIBRARY_THING_API_KEY` added to build.gradle.kts
  and referenced via the fully-qualified path (matches the existing
  GOOGLE_BOOKS_API_KEY pattern). CI will validate the real compile.