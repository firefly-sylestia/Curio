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

**DONE (v426b, art & music widening, this session):**

- **`MuseumFetch.kt` — the Cleveland Museum of Art (keyless).** `work()` for the
  artwork sheet, `worksBy()` for a maker's own list, `makerBio()` for the museum's prose
  about a maker Wikipedia has no article for. Wired into `ArtworkFetch.artwork()` (now
  Met ∥ Cleveland ∥ Wikipedia, merged Met → Cleveland → Wikipedia) and `worksBy` (Cleveland
  appended after the Met's, deduped). Verified live: `{info, data:[…]}`, `_web.jpg` 200
  image/jpeg 232 KB. **Its `artist=` parameter does NOT filter** (asked for Monet, got
  Copley/Bellows/Eakins) — filtering happens here, on the `creators[]` line.
- **`SongArtFetch` — the album side's second door.** MusicBrainz RECORDING search → the
  release it sits on → Cover Art Archive `front-500`, behind MusicBrainz's own 1/second +
  real-User-Agent rules, asked only when iTunes found nothing.

**Doors checked and REJECTED, with the reason (so nobody re-tries them):**

- **Art Institute of Chicago** — keyless and lovely, but its `iiif_url` images answer **403
  to a non-browser client** (the same id the API returned; 843px and 400px alike).
- **Deezer** — its search endpoints are **blocked anonymously**: `data: []` with
  `total: 83` (verified on `/search/album?q=…`, `/search?q=…`). Single-resource reads
  (`/album/{id}`) still work, but a search door needs search.
- **Audius** — works keyless, but it is an independent-artist catalogue: "daft punk"
  answered with other people's remixes, so it would put the wrong cover on a mainstream
  album. A wrong cover is worse than none.
- **LRCLIB** — keyless and working (lyrics + duration). NOT wired: lyrics are a NEW kind of
  content on the song sheet, so it needs the member's word rather than arriving as a side
  effect of an artwork pass. Offered as a follow-up.

**STILL PENDING from the member's own message:**

1. **More API KEYS for art / music / other lanes** — needs their pick of which keyed source
   to wire (Rijksmuseum, Smithsonian, Harvard, Europeana, Discogs, Last.fm are all keyed;
   `TMDB_API_KEY`, `GOOGLE_BOOKS_API_KEY`, `LIBRARY_THING_API_KEY`, `COMIC_VINE_API_KEY`
   are already plumbed).
2. **"Enable bottom sheet for more things in category"** — ambiguous; asked.
3. **The feedback form should show whether or not Online mode is on** — and this one has a
   hard constraint behind it: `supabase/schema.sql`'s `ff_select_live_or_team` policy is
   `for select to authenticated`, so a live form is readable **only by a signed-in
   member** — which only Online mode produces. There is no local copy of a form; it is
   published on the server. So "show it regardless" is a real decision rather than a
   tweak: (a) read it with the publishable key behind a PUBLIC read policy on live forms,
   (b) sign in anonymously just to read it, or (c) keep Online mode as the gate and offer
   an explicit "check for a new form" action that works with it off. Asked.

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

## 6. Follow-up (this session) — the red CI lint

The member pasted the failed `:app:lintRelease` log: `WrongConstant` at
`PersonalExport.kt:446` — `setJustificationMode` must take `LineBreaker` constants,
not `Layout` ones (same value, 1, inlined either way, but lint insists on the
`LineBreaker` spelling). Fixed by importing `android.graphics.text.LineBreaker` and
switching the call. Committed and pushed.

## 7. Request (this session) — the named themes' dark mode, again

> lets work on those theme again, in profile the xp progress setings etc backgroud is
> good, but inside the settings the apprenace and its sub pages settings backgroud dis
> still bad in those new theme fix it please in dark mode
> also in dark mode in many themes the text visiblity is bad, like in dark mode the
> texts have black or dark color, can u do a full audit and tell what are they.
> ask me if youre confused or doubts

Asked via `ask_user` (a wrong guess costs a full CI cycle):

- **Which part of the Settings/Appearance screen is the bad background** → **the big card
  behind the rows** (not the page, not the theme capsule, not the sheets).
- **Where the black/dark text is** → "one place i noticed in journal, the texts are black
  colors in dark mode check in all theme, and more similar dark texts black text colors".
- **Scope** → **all dark themes** (not the five named ones only).
- **The audit** → list it, then fix in the same pass.

### Findings

- **The Settings plate really is the odd one out, measured.** Profile's card resolves to
  `lerp(surfaceContainerLow, settingsCardTintLift, 0.30f)`; `SettingsOptionCard` asked for
  `surfaceContainerHigh` — the *pill inside a card* rung. On the app's own neutral greys the
  two are close enough to pass (L 0.141 vs 0.097), but a theme whose ladder carries colour
  made it loud: under a named theme at night the option card measured **L 0.225 against
  Profile's 0.158 — 1.94x the luminance**, and 2.0–2.9x the same card in the app's own dark
  scheme. So the member's report is exact, and so is the reason Profile looks right.
- **A fixed-dark-in-the-journal scan.** Every hard-coded dark literal in the module was
  swept (24 hits) — all but one are the *light* branch of a `if (dark) … else …` pair. The
  journal's real offenders are three ink helpers that never got a night twin:
  `personalQuoteColor()` (its own doc promised a milky twin and returned the light coffee),
  `personalAnnotateLinks`' hard-coded `#5C3A20` link ink (~1.6:1 on the journal's `#17130F`
  paper), and `personalMoodInk`'s darker moods (Heavy `#6E6A72` at ~2.3:1).
- **The systemic class: `primary` and `tertiary` are FILLS in a named scheme.** The app's
  own dark scheme puts the bright coral in `primary` and the bright mint in `tertiary`, so
  `tint = MaterialTheme.colorScheme.primary` reads in both modes there; a named theme puts
  its deep HERO fill in `primary` (L 0.30) and a dark tone in `tertiary`, so every glyph,
  label and icon-tint asking those roles as INK lands at ~1.6–1.9:1. That is "in many themes
  the texts have black or dark color".

### What was built (v426)

- `curioSettingsCardFill()` (`ui/components/CurioSettingsCard.kt`) — the Profile card's fill
  named once, and `SettingsOptionCard` asks it in dark mode too, so a Settings sub-page and
  a Profile card are the same plate in every theme.
- `curioAccentInk()` / `curioTertiaryInk()` (`ui/theme/CurioTheme.kt`) — `primary`/`tertiary`
  as INK. Both return the scheme role untouched for every non-named theme (so no other theme
  moved a pixel) and the named theme's own accent tones under a named one. Applied to the 17
  icon-ink sites (`CurioStreakPill`, `StatsScreen`, `SocialComponents`, `SettingsHubScreen`,
  `TextHistory`, `CurioNavHost`, `TopicHistoryScreen`, `TopicRevealScreen`, `TopicShareCard`)
  and the 7 `tertiary`-as-ink sites (`TextHistory` ×2, `CabinetScreen` ×3,
  `CabinetV2Content` ×2).
- The journal's night inks: the quote takes `#C09263` at night, `personalAnnotateLinks`
  takes its link ink as a parameter (`personalQuoteDeepColor()`), and `personalMoodInk` is
  `@Composable` with an L 0.62 night floor.

### Verification

- Brace/paren/bracket balance 0/0/0 on all 14 touched files; import/use pairs checked per
  file (every `curioAccentInk`/`curioTertiaryInk` call site has its import).
- No Gradle in this environment — CI compiles it (per `AGENTS.md`).

### Still open (listed, not swept — each needs a judgement call)

- `color = MaterialTheme.colorScheme.primary` — 71 sites, a MIXTURE of fills and ink;
  PetDesignerScreen's 26 are deliberately its own palette.
- `secondary` as a selected-state ink/fill in `ShareHubScreen` (408/551/565) and
  `TopicShareCard` (12695/12710) — a design question (what does "selected" mean on a named
  theme), not a role swap.
- The 12 `outlineVariant`-as-ink hits are mostly borders and dividers — correct usage, left
  alone.

## 8. Request (this session) — the pickers, the journal's editing, the PDF

> redesign the theme pickers, and redesign the 2 option choosing things, also in journal
> the copy tools selects all but i wanted it to open a tool ith cut copy paste undo tool,
> also the pdf isnt accurate, it doesnt show exactly as the journal view have, also a bug
> when a voice note is of differnt line or moves up once its not comong down the text
> again also its very difficult to insert texts in between images, also similiar bug to
> photo of a differnt line fails to merge with a older line also in stack those sizes
> options are not accurate, fix the size accuracy in stacks of 2 3 4 when user picks a
> size or style

Asked via `ask_user`:

- **"The 2 option choosing things"** = the two two-option rows on the Appearance page:
  **Glyph backdrop** (Subtle / Deep) and **Paper** (White page / Cream page).
- **The pickers' redesign**: "more compact & premium", "the missing motion / touch feel",
  and **"too many texts and em dashes"** — so: tighter rows, real press/reveal motion,
  and less copy (and stop scattering em dashes through the subtitles).
- **The Copy bar**: Cut / Copy / Paste / an extra **Select all** button, and choosing Cut or
  Copy **starts a selection** with arrow tools to grow or shrink it, then the action
  confirms — not Android's plain select-all bar.
- **Sequence**: all of it, in whatever order I judge right, committing each as it lands.

### Findings (all located — this is the working list)

1. **The theme pickers** — `SettingsSectionScreen.kt`: `ColorThemeRow` → `ColorThemeSheet`
   (each row a three-swatch preview + label + hint) and `ThemeModeSwitch` (the
   Light/Dark/System capsule). Subtitles carry the em dashes and the length.
2. **The two 2-option rows** — `SettingsSectionScreen.AppearanceSection` calls
   `CompactSegmentedRow(CurioIcons.Wallpaper, "Glyph backdrop", …)` and
   `CompactSegmentedRow(CurioIcons.Contrast, "Paper", …)`; the component itself lives in
   the settings shared components.
3. **The Copy tool** — `PersonalCanvas.kt`: the dock button calls
   `state.requestPageTextMenu()` (line ~5153), the state sets `pageTextMenuRequest`
   (~1677) and the canvas raises **Android's own** floating bar (`showMenu`), i.e. Copy +
   Select all only. `pageText()` (~1685) already knows how to read the whole page, and
   `pageSelected` / `toggle(flag)` (~1695) already handle page-wide state — so Cut, Paste
   and Undo have a place to hang, and the arrow-selection needs a range on top of
   `pageSelected`.
4. **The PDF** — `PersonalExport.kt` draws its own `PdfDocument` (`StaticLayout`
   paragraphs, marker pens, photos, voice waves, a numbered foot). "Doesn't show exactly
   as the journal view" is a layout-parity gap between that drawing and what
   `PersonalCanvas`/`PersonalPhotoBlock`/`PersonalVoice` draw on screen.
5. **The merge bugs** — `PersonalCanvas.kt`: `mergeWithPrevious` (line ~2543) returns
   false whenever the block ABOVE is a photo or a voice note, and the row builder
   (line ~2829) only forms a run of **consecutive** prints (`PRINT_ROW_LIMIT` = 4) plus the
   lone-SMALL-beside-writing case (`besideSkips`). So a print or a voice note that ends up
   on its own line with a text row between it and its run never rejoins it — that is the
   "fails to merge with an older line" report, in both the photo and the voice-note form.
6. **Text between images** — same row builder: a run of prints is drawn as ONE unit by its
   first member (`printRows`), so there is no tap target between two prints of a row; the
   run has to be broken before text can be slipped in.
7. **Stack sizes** — `PersonalPrintArrangement` (line ~3823) with `PRINT_ROW_LIMIT = 4`:
   each cell's WIDTH is its size's `fraction` as a Compose weight while each cell's HEIGHT
   is `personalPrintHeight(size)` — two independent readings of the same size. In a THREE
   that inverts the shape (a PORTRAIT tall frame at 0.54 against a stacked column of
   0.44 + 0.44 = 0.88 gets **38%** of the measure, so the "one upright frame with two
   stacked beside it" comes out with the two stacked prints WIDER than the frame), and in
   a FOUR the two lines weight themselves independently (PAGE 1.0 + HALF 0.62 → 62/38 on
   one line, SMALL + SMALL → 50/50 on the next), so "the same size" gives different widths
   per line. The row's heights also never agree (`PORTRAIT` 232 vs `HALF` 128 +
   `SMALL` 100 + an 8dp gap = 236 beside it).

### Done — committed as `4684a687` and pushed

1. **The merge bugs.** Both row passes (the editor's and the read view's) now step over a
   blank row that is nobody's place (`printRowGapSteppable` — no words, no voice note, no
   caret, no selection), so a print that ended up a line away from its row rejoins it, and
   the blank air is left out of the drawing. The two passes still agree.
2. **The stack sizes.** A three's upright frame now leads (it takes at least the widest
   print beside it, where a Portrait frame used to get 34% against 0.62 + 0.44), each
   stacked print takes its OWN share of its column, and a print beside the writing takes
   the share its own size says (`printBesideShare`, capped at half) instead of a fixed 42%
   — editor and read view both.
3. **The text bar.** `PersonalEditorState` gained the page-selection state
   (`pageRange`, `selectWholePage`, `grow`/`shrink`, `pageSelectionText`, `cut`, `copy`,
   `pastePageText`, `undoPageEdit` with a stack of closures, and `RemovedPageRow` so a cut
   can be undone whole); `PersonalPageEditBar` + `PageTextChip` render it in the dock's own
   row (the dock's Crossfade gained the `pageEditBarOpen` branch, the copy door toggles it).
   Undo is the BAR's own last actions, and the code says so out loud.
4. **The pickers (first half).** The two 2-option rows wear the app's own sliding capsule
   (a row that can disable an option falls back to `SettingsOptionSegmentedRow`, where
   those states work); the Color theme sheet lost its second sentence, its em dash and its
   spare air.

### Still open from this batch — both landed after it

- **The pickers' second half** — DONE in the next commit: the live row's wash animates in
  (`animateColorAsState`) instead of snapping, and the picker's rows squish deeper than a
  section row's.
- **The PDF** — DONE in the next commit (the print's own size is the box it is drawn in,
  and the old clamp at `1f` so a low-resolution picture printed as a stamp is gone) and
  finished by §9 below (its type, leading and labels are the canvas' now).

## 9. Request (this session) — the PDF's type, from the canvas

> Line the PDF's type up with the journal view as well — body size, leading and the caption
> face taken from the canvas instead of the export's own numbers

Then, asked and answered: **make the PDF a facsimile of the journal column** — same measure,
bigger type, more pages. So the sheet's scale is the page's own measure (see below), not the
printable one it shipped with.

### Findings

- **The export carried its own type table.** `PersonalExport.kt` had `PDF_BODY_SIZE = 30f`,
  a heading as `body × 1.45`, a small line as `body × 0.84`, ONE leading ratio for every
  size (`PDF_LINE_SPACING = 1.42f`), a paragraph gap of `body × 0.72`, and every print's
  caption in Lora at `body × 0.74` — none of it read from the page it was drawing. Against
  the canvas' own read-back numbers (body 16sp on a 27sp line = **1.6875**, title 22/31,
  small 12.5/21, quote 15sp, the label 13sp × the label's own size, 8dp between rows) the
  body and the small print were close by luck and the LEADING was set a fifth too tight.
- **None of those numbers had a name.** They were literals in the canvas' read view
  (`16.sp`, `27.sp`, `22.sp`, `31.sp`, `12.5.sp`, `21.sp`, `13.sp`, `spacedBy(8.dp)`), so
  nothing could take them from there without them being copied — which is exactly how the
  export's own table drifted.

### What was built (v427)

- **`PersonalCanvas.kt` — the page's type is one table.** `BODY_BODY_SIZE/LINE`,
  `BODY_VIEW_SIZE/LINE`, `TITLE_VIEW_SIZE/LINE`, `SMALL_VIEW_SIZE/LINE`,
  `QUOTE_VIEW_SIZE`, `CAPTION_VIEW_SIZE`, `VIEW_ROW_GAP` (internal, so the writer, the
  reader and the exporter all read the same numbers), and the canvas' own two surfaces now
  take them instead of their literals — so the table IS the source, not a copy of it.
- **`PersonalExport.kt` — the sheet IS the column.** The scale is derived, not chosen:
  `PDF_UNITS_PER_SP = (PDF_PAGE_WIDTH − 2 × PDF_MARGIN) / PDF_PAGE_MEASURE_DP`, where
  `PDF_PAGE_MEASURE_DP = 316f` is the journal's own column (a 360dp phone, less
  `JournalReadView`'s 22dp gutters) — **one canvas dp to one sheet unit (×3.228)**. Every
  size, every leading ratio (`BODY_VIEW_LINE / BODY_VIEW_SIZE` and the title's and the
  small line's own) and the paragraph gap (`VIEW_ROW_GAP`) comes from the canvas through
  that one number; the sheet keeps only what is about PAPER (its A4 edges, the margin, the
  foot, a picture's height cap).
- **The marks are the page's marks too.** A list line's box is `PERSONAL_MARKER_SIZE`
  (18dp) and its lead-in `PERSONAL_MARKER_LEAD` (28dp) at the sheet's measure instead of
  0.46 of the first LINE's height with a fixed 56-unit margin, the strokes are the canvas'
  own ratios (outline 0.085, tick 0.135, dot 0.17), a CHECKED box is the accent fill with a
  **paper** tick and a waiting one a 42% ink outline (`drawPersonalCheckbox`'s two states),
  the mark is centred on the LINE as the page centres it, and the quote's rule takes the
  canvas' `QUOTE_RULE_WIDTH` (3dp) and `QUOTE_LEAD` (13dp) — both newly named in the
  canvas so its two quote panels and the sheet ask for one number.
- **A quoted phrase is the page's quotation now.** Every quoted run is set at
  `QUOTE_VIEW_SIZE` (15sp) inside the block's own line, which is how the page sets it — a
  line that is a quotation throughout comes out as one, and a line with a quoted phrase in
  it keeps its prose and shrinks exactly that phrase (where the sheet set the whole line at
  the body's size with only a rule beside it). The leading stays the line's own.
- **The label is the print's own.** A new `PdfFonts` resolves the sheet's four run faces
  plus the six a print's label can wear (`PersonalCaptionFace` → `lora`,
  `patrick_hand_regular`, `playfair_display`, `space_mono`, `bebas_neue`, `space_grotesk`,
  resolved once per file), the label's size is `personalCaptionSizeSp(CAPTION_VIEW_SIZE, …)`
  — so the member's Small / Standard / Large finally reaches paper — and its STAMP rides
  under the words when the print carries one. Both lines are cut to the PRINT's own measure
  (one line, then an ellipsis), as the page cuts them.

### Done — committed as `adcd4012` (the type from the canvas) and `341ca04c` (the facsimile)

### Verification

- Brace/paren/bracket balance 0/0/0 on both touched files; every new constant checked for a
  live call site (none left dangling) and every helper the export calls checked for its
  import (`TextUtils`, `TextUnit` added).
- What the sheet now sets, in its own units (scale 3.2278): body **51.6** on 1.6875 (was
  30 on 1.42), title 71 on 1.409, small 40.3 on 1.68, a quoted run 48.4 inside its line,
  the air between rows 25.8, a marker box 58.1 with a 90.4 lead-in, a label 42 (its stamp
  31.9). A line pitch of 87 units means ~17 lines a sheet — the "more pages" asked for,
  and on A4 the body prints at ~24.8pt (the old scale printed ~14.4pt). The old printable
  scale was the same constant at 1.875 (a 544dp measure), so the whole choice is one
  number.
- No Gradle in this environment — CI compiles it (per `AGENTS.md`).

### Noted, not changed

- The canvas' two sides disagree about one number: the PEN sizes a label by the frame it
  sits in (13sp under a page-wide print, 10sp under a small one) while the EYE reads every
  label at 13sp. The sheet mirrors the EYE (a sheet of paper is the page read), so a Small
  print with a Large label ellipsizes as fast on paper as it does on screen. Left as it is
  because it is the page's own behaviour, not the export's.
- The voice note's geometry on paper (a pill `body × 2.3`, its clock at `body × 0.85`) is
  still the sheet's own — asked for as body / leading / caption face, so untouched.

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none)
