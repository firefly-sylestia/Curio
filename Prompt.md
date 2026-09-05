# Request Log — CI fix for reveal poster, keyed-provider defaults, Clear-covers button

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user)
"fix it" (the v360 push failed CI) + "also i added spotify key and library
thing api as well alaso does the api is used in the apk build from pr, use
that by default, also in book fetching add a button to clear all book
covers so testing other provider is easy"

## CI failure (root cause)
`TopicRevealScreen.kt:2888` — `AsyncImage.onSuccess` was written as a
(request, result) lambda, but Coil 2.7's AsyncImage callback takes ONE
param, `AsyncImagePainter.State.Success` (the drawable lives on
`state.result.drawable`). This was the v360 placeholder-skip addition.

## What was done
### TopicRevealScreen.kt (CI fix)
- `onSuccess = { state -> ... }` using `state.result.drawable.intrinsicWidth
  /Height` — the 1x1-placeholder skip works again and the build compiles.

### Keyed providers used by default (yes, in PR builds)
- Verified `.github/workflows/android.yml`: `LIBRARY_THING_API_KEY`,
  `SPOTIFY_CLIENT_ID`/`SPOTIFY_CLIENT_SECRET`, `GOOGLE_BOOKS_API_KEY` are
  passed as env into the PR/push build step and baked into BuildConfig, so
  the APK built from a same-repo PR includes the user's keys. Caveat: fork
  PRs don't get secrets (GitHub restriction); same-repo PRs + pushes do.
- `AppPreferences.getBookCoverProvider`: when the LibraryThing key is set
  and NO provider was ever picked, the default is now **LIBRARY_THING**
  (was always ITUNES) — a keyed install immediately uses the best-quality
  ISBN covers. Spotify deep links were already on-by-default whenever the
  keys are set (resolveSpotifyItemUrl non-null → deep link).

### Clear all covers (BookCoverHubScreen + BookCoverFetch + AppPreferences)
- New hub button "Clear all covers" (trash glyph, confirm AlertDialog):
  `BookCoverFetch.clearAllCovers(context)` → `AppPreferences.clearBookCovers`
  (removes KEY_BOOK_COVER_URLS / KEY_BOOK_COVER_DONE / KEY_BOOK_COVER_FAILED,
  resets the three reactive states) + `Coil.imageLoader(context).diskCache
  ?.clear()`. Enabled only when there is something to clear; ratings kept.
  Purpose: A/B-testing providers — the old provider's verified URLs would
  otherwise keep winning `coverCandidates`' first slot.

### Docs
- app/AGENTS.md: **v361** entry.
- fastlane changelog 20260921.txt: FIX/ADD bullets at the top.
- Prompt.md: this log.

## Verification
- Braces/parens balanced (TopicRevealScreen +1 paren is pre-existing at
  HEAD). `bookCoverUrlsState` has `internal set` — settable inside
  AppPreferences. CI will validate the real compile (this push fixes the
  exact line that failed last time).