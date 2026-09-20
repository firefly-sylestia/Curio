# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> also is the form only visible to online users? also why it doesnt appear in home
> automatially it only appears after i open it in settings, also the theme of
> jadeorchid,ocean,sand,emeber,in dark mode the problem is their card colors in settings
> profile and many places, some are too bright in dark mode only even the hero color its
> too bright looking only in dark mode again. please fix it overally. only in dark mode,
> keep the colors muted fix the settings and profie buttom cards where settings otpions
> etc reside. and then update the incursion ui, it looking bad and doesnt matches the app
> style at all, and also add cover fetching to them series fetching, use the buttom sheet
> we use in topic reveal to show its details with proper infomation fetch and while
> keeping its old informations. also the activation overlay it shows upgrdae it, in books
> add manga support too manhwa etc, and then lets expand more providers more was to fetch
> scrape data in app. also in book detail screen if ive not added the pdf for page or
> suppose it using epub for pages in detail screen it doesnt show the pages count from
> it,so fix it, also for epub in the page swpie the nsid eby side pages they are kind of
> bad view and also lagging on pinc to zoom in epub fix it as well, and still the title
> detection for pubs are bad.

Confirmed with the member earlier via `ask_user`:

- **Sequence:** Colours → Incursion → Books.
- **Feedback form:** it must greet whoever opens the app the moment a form is published.
- **Incursion:** keep the TWO-COLUMN shape, fix the look so it matches the app; fix the
  unlock announcement's wording (it says "upgrade" — there is no upgrade in Incursion).
- **Books:** a bottom sheet like the topic reveal for a series' details (fetching proper
  information while keeping what is already stored); cover fetching for series; manga +
  manhwa support; more providers/scraping sources; full kind + link reading.
- **Dark colours:** "Mute cards + hero + accents" — dark mode only.

## 2. Findings

**The feedback form is online-only for two reasons, and both are load-bearing.**

- `FeedbackFormState.refresh` returns unless `AppPreferences.isOnlineModeEnabled` AND
  `OnlineAccount.state.session?.accessToken != null` — the form is published on the
  server and read with the member's own token. A member with Online mode off (or signed
  out) never sees a form at all. That is not a bug; it is the only way the form can reach
  the app without a bundled copy.
- **Why Home missed it:** Home's `LaunchedEffect(Unit) { FeedbackFormState.refresh(ctx) }`
  runs at first composition — usually BEFORE `OnlineAccount` has restored its session
  from storage. `refresh` bails on the null token, `fetched` stays false so it *could*
  retry, but nothing else asks until Support/Settings composes its own
  `LaunchedEffect(Unit)`. That is exactly "it only appears after I open it in Settings".

**Dark named themes are too bright because their fills carry daylight chroma.**

- `darkScheme()` built the page at HSL 0.36 chroma and the card ladder at 0.34 → 0.24,
  topping out at lightness 0.30 — while the app's OWN dark ladder is a neutral
  `#121212…#2C2C2C`. A named dark card therefore sits at more than twice the luminance of
  the scheme the app was tuned against, and a saturated one at that — "some are too bright
  in dark mode" on a page of settings cards.
- The dark hero was HSL 0.50 chroma at 0.33 — a lit, saturated slab.
- The dark accent was measured at 3.79–4.05:1 against the top card step — under the 4.5
  bar on the very surface it is usually drawn on.

## 3. What was built

**The form greets you on Home (v425)** — `MainActivity` seeds `OnlineAccount.restore(this)`
with the rest of the app's state, and `FeedbackFormState.refresh` parks ONE read on
`snapshotFlow { session?.accessToken }` when it is asked before the account is ready, then
runs it the moment one arrives. Two screens needed no change at all. (Answer to the
member's own question: yes, a form is only visible with Online mode ON and a signed-in
session — it is published on the server and read with the member's own token, so a member
with Online mode off never sees one. That is the design, not an oversight.)

**The night is muted (v425)** — `NamedThemes.darkScheme()` / `pageFor(true)` / `heroFor(true)`
/ `accentFor(true)`:

| | before | after |
|---|---|---|
| page | 0.36 chroma @ 0.11 | 0.30 @ 0.10 |
| card ladder | 0.34→0.24 @ 0.14…0.30 | 0.24→0.16 @ 0.135…0.255 |
| hero | 0.50 @ 0.33 | 0.36 @ 0.30 |
| accent | 0.40 @ 0.69 | 0.30 @ 0.70 |
| second/third fills | 0.46/0.44 @ 0.42/0.44 | 0.36/0.34 @ 0.34/0.36 |

Measured with a Python replica of `fromHsl` + Compose's luminance/`contrast`:
`onHero` 5.5:1 (Jade, was 4.2 — under the bar), accent on the TOP card step 4.64:1
(Orchid, worst) to 5.08:1 (Jade); the old 0.40-chroma accent measured 3.79–4.05 there.
Dark `onSecondary`/`onTertiary` are now `readableOn(...)` (white wins at ≥4.97:1).
Light mode is untouched.

**Incursion (v425)** — `statusInk` is one @Composable tone resolver (six hues, the app's
day/night tone discipline); the head plate, phase headers and rows are cards
(`surfaceContainerLow` + `curioTintOn` + `curioCardShadow`), the chips lost their
`BorderStroke`, the tiles lost their top colour bar (status = a whisper in the fill + a
dot), the filter chips take their ink from `curioFillInk`, and `IncursionDetailSheet` is a
`ModalBottomSheet`. `IncursionUnlockReveal` is a Curio card in the theme's ink with
rewritten copy ("The watching order is yours") — it read as an app-store notice.

**A file answers its own page count (v425)** — `epubPageCount` (the EPUB 3 page-list) +
`documentPageCount` as the one door; used by the detail page's own length, the shelf's
attach drop, and `documentChapters`' chapter ranges. `BookDetailScreen`'s
`filePageCount` no longer bails on a non-PDF.

## 4. Still open (the rest of this request — BOOKS)

**DONE (v426, this session):** manga / manhwa / manhua / comics / light novels as shelf
KINDS — `PersonalKinds` + `personal_books.kind` (`MIGRATION_20_21`, default `book`), a kind
picker in `AddBookSheet` asked BEFORE the search, `MangaFetch` (keyless cascade: AniList →
MangaDex → Jikan → Kitsu), the kind worn on the shelf card's cover, and `BookEnrichment`
bailing out for a comics kind so a manga is never "enriched" with a book's facts. Choices
confirmed with the member: always-on (no Settings toggle), all five kinds, those four
keyless sources (Comic Vine/publishers are the keyed doors, left out), and TMDB stays the
source for the film/series basics it already fetches.

**DONE (v426):**

- **Title detection for files** — `detectBookFromFileName` hardened: a round bracket is now
  judged by what it names (publisher, imprint, edition, format, site, year), a zero-padded
  leading index comes off (but `07-Ghost` keeps its name), and a dash-separated tail that
  names a publisher is dropped. Shapes were validated against the parser in Python before
  committing. (`6f5023b5`)
- **EPUB pinch lag + the empty page** — the pages were remembered on `ReaderLook.textScale`,
  so every pinch event re-measured every block in the book (the stutter). The member chose
  "live magnify, settle after": nothing is re-made while the fingers are down — the page is
  scaled in the DRAW phase (`graphicsLayer`, layout size untouched so `paginateBlocks`'
  `room` is never wrong) and the type size is written once in `pinchToZoom`'s new `onEnd`.
  (`696a1c91`). The member's answer to the *view* half of that complaint was "the apps own
  title shows in a empty page": a heading is big type with margins of its own, so the
  paragraph it NAMES was what tipped the pair over and the break landed between the two.
  `paginateBlocks` now holds the break while a page holds nothing but a heading.

**DONE (v426b — wider providers and scraping doors, this session):** the member picked
this item first and chose "wire the key fields now, add them as optional keys".

- **`ComicVineFetch.kt`** — the KEYED comics door, `COMIC_VINE_API_KEY` (optional: env +
  `buildConfigField` + `.env.example` + both workflows). Asked FIRST for a `COMIC` and LAST
  for a manga. Its `status_code` is read from the JSON, not the HTTP code (an invalid key
  answers **401 with a valid body** — verified live), and requests are paced 1.1s apart for
  the free key's own 1/second rule. Returns a `Volume` turned into `MangaFetch.Hit`.
- **Marvel and DC keys: deliberately NOT wired.** Marvel's developer API was DISCONTINUED
  (announced 2025; its keys answer nothing by 2026) and DC never published one; Marvel's
  scheme also wanted a private key inside the APK. Comic Vine carries both publishers.
- **`StandardEbooksFetch.kt`** — keyless, the door that answers for a CLASSIC: its OPDS feed
  (`/feeds/opds/all?query=…&per-page=5`) read for title/author/summary/cover. **Verified
  live**: Middlemarch, Moby-Dick, Great Expectations, Crime and Punishment and The Odyssey
  all resolved with the right author and a real cover, and a manga title correctly returned
  nothing. Added as a `BookCoverFetch.BookCoverProvider` (STANDARD_EBOOKS), to the reveal's
  live fallback, and as `BookEnrichment`'s description door of last resort.
- **The by-hand gap** — `BookEnrichment.comicsPass` asks `MangaFetch` for a comics row's own
  kind and fills ONLY what is empty (cover, author, synopsis, length). `PersonalKinds`
  gained `isComicBook` / `asksComicSources`, which every routing decision now asks; a
  `COMIC` searched in the shelf keeps its kind (it used to be saved as a plain book) and
  falls back to Open Library only when the comics sources answered nothing.

**REMAINING:**

1. **Series fetching + covers, and a detail sheet for a series** — the member wants a
   series' own covers fetched and its details opened in the SAME bottom sheet the topic
   reveal uses, with the lookup adding information while the stored ones stay.

## 5. Work log

**Phase 0 — THE FAILED CI (before anything else).** The member's push carried a red build that
had nothing to do with the v425 work: `BookReaderScreen.kt` and `PersonalCanvas.kt` called
`coords.positionInRoot()` without importing it, which in this Compose version (BOM 2026.05.01)
is a TOP-LEVEL extension in `androidx.compose.ui.layout` (`PersonalPage.kt` imports it for the
same call, and `TopicShareCard` imports `positionInWindow` the same way). Two more real errors
sat in the same Box: a stray COMMA after the `pointerInput { }` block (which split the modifier
chain into a second argument — hence the "'infix' modifier is required on … onGloballyPositioned"
and the cascade of `surfaceSize`/`surfaceOrigin`/`@Composable invocation` errors), and
`Modifier.clickable(onLongClick = …)`, which is `combinedClickable`. Every
`boundsIn*`/`positionIn*` call in the module was then checked against its imports — these two
files were the only ones missing one.

- **Phase 1 — the form and the night.** Done, committed, pushed (`f3604631`).
- **Phase 2 — Incursion.** Done, committed, pushed (`33db4480`).
- **Phase 3 — the file's own page count (Books).** Done, committed, pushed (`94e66bf9`).
- **Phase 4 — comics kinds, sources, title detection, EPUB pinch + the empty page.** Done,
  committed, pushed (`bfb002ce`, `6f5023b5`, `696a1c91`, and the empty-page fix with them).
- **DOX pass** — `app/AGENTS.md`: the Incursion section's tile description corrected (the
  status bar is gone), the page's v425 language added, and the file/page-count contract
  added to the chapters bullet. Committed with this file; NOT pushed (docs-only).

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none)
