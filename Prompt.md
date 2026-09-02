# Prompt.md — current request log

## Request: compact CH chapter chips + a real book-cover hub (providers, failed retry, keyless ratings)

User request (verbatim, condensed):

> In the bottom sheet of synopsis and chapters, instead of saying
> "chapter" just say "CH 1, CH 2" — better and more compact. Add more
> better book cover providers with an option to select which, plus a
> failed-fetched-books option — make book-cover fetching a hub of its own.
> If possible, add rating fetching too without any API — add it.

## Follow-up (v320b/v320c): CI fixes + fetch OPT-OUT

- v320b: `NewCategoryPicker.kt:1407` — the morph-in pill's `Animatable`
  was named `alpha`, shadowing the `graphicsLayer` receiver's `alpha` var
  ("'val' cannot be reassigned / Float vs Animatable"). Renamed to
  `popScale`/`popAlpha`.
- v320c (second CI run): (1) `BookCoverFetch.kt` used the wrong Coil
  package — this project is Coil 2 (`coil.*`), not `com.coil.*`:
  restored `coil.Coil`/`coil.request.ImageRequest`/`CachePolicy` imports
  and dropped the fully-qualified refs (also clears the follow-on
  "Cannot infer 'it'" listener errors). (2) `BookCoverHubScreen.kt:70`:
  `TopicJsonLoader.load` is suspend — the book count now loads via
  `produceState` instead of a plain `remember {}`. (3)
  `BookCoverHubScreen.kt:446`: `Modifier.weight` needs RowScope —
  `StatCell` now takes a `modifier` param and the caller passes
  `Modifier.weight(1f)` inside the stats Row.

Also made book fetching OPT-OUT by default:
`AppPreferences.bookFetchEnabledState` (`KEY_BOOK_FETCH_ENABLED`, default
false) — nothing downloads and no Google lookup runs until the user flips
the toggle at the top of the hub. Hub actions, per-row retries and the
engine entry points all respect the gate; the Experiments row subtitle
shows "OFF · open the hub to turn fetching on".

### What shipped (this turn)

**A. Chapter chips → "CH N" (`TopicRevealScreen.kt`):** the reveal page's
`BookChapterChip`, the book-notes sheet header, and the in-sheet chapter
pills all read compact `CH 3` (was "Ch. 3") — sheet tabs still say
"Chapters · N".

**B. Book covers & ratings HUB (new `BookCoverHubScreen.kt` +
`BookCoverFetch.kt` rework):** the Experiments "Book covers" row now
navigates to a dedicated screen (`CurioRoutes.SETTINGS_BOOK_COVER`,
registered in `CurioNavHost`):
- **Provider picker** (`BookCoverFetch.BookCoverProvider`): Open Library
  title covers (the reveal's own fallback) or Google Books — keyless
  `intitle:+inauthor:` volume search returning the first cover thumbnail.
  Selection persists (`KEY_BOOK_COVER_PROVIDER`).
- **Stats card**: book count, failed covers, rated books.
- **Actions**: Fetch all covers · Retry failed (N) · Fetch ratings
  (keyless) — one-by-one with a live progress bar + Cancel.
- **Failed-books list**: every failed cover is persisted
  (`KEY_BOOK_COVER_FAILED` / `bookCoverFailedState`) so retries survive
  restarts; each row has its own Retry pill and a bulk "Retry failed"
  reads only those books. `fetchAll(context, provider, onlyFailed, cb)`
  keeps the old engine loop (disk-cache only, polite 150ms pacing).
- **Ratings without any API**: `fetchRatings` queries the public Google
  Books JSON endpoint (no key, 120ms pacing) and stores each first-hit
  `volumeInfo.averageRating` in a name→rating map
  (`KEY_BOOK_RATINGS` / `bookRatingsState`).

**C. Reveal rating chips:** the synopsis card header and the book-notes
sheet header show a compact ★ rating ("★ 4.2") when a fetched rating
exists for that book.

Files: `BookCoverHubScreen.kt` (new), `BookCoverFetch.kt`,
`AppPreferences.kt`, `CurioRoutes.kt`, `CurioNavHost.kt`,
`UserExperimentsScreen.kt`, `TopicRevealScreen.kt` + `app/AGENTS.md`
(v320 bullet) + changelog. INTERNET permission already present; all seven
files brace/paren-balanced and imports hand-audited (Coil APIs fully
qualified in the engine; keyless HTTP via `HttpURLConnection`).