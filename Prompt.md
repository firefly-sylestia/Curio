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

## 10. Request — the eye view's ink, the PDF's prints and waves, the bar's letter reach

**The ask (member):** *"still in journal eye view the text writing have dark black texts,
and in pdf export the photos are not visible as they are in the preview of journal. and the
waves are also not visible as it is in journal eye view. also is it possible to make the pdf
interactable. also the select tools doesnt hihglight whats selecting and why theres onl row
selection i also want letter by letter too. push the previous one"*

**The previous one** was already pushed (`2d40d542`, the star map) before this batch started;
the red-build fix (`34065a99`) and the gesture removal (`d87efad1`) are in `origin/main` too.

### 1. The eye view's black text — ROOT CAUSE FOUND (the same class as the reader's v422 ruling)

Not a theme fault at all, and not the quote / link / mood inks that were fixed in `8338e1f8`.
The read view's `Text` set a `TextStyle` with **no colour**, and `personalAnnotated` coloured
only the FLAGGED runs — so a plain word carried no colour and fell through to
`LocalContentColor`, which outside a `Surface` is **Compose's own default, `Color.Black`**.
The writing page passes `color = ink` in its `bodyStyle` (that is why the pen was always
fine), and the reading page does not (that is why the eye was black — in every theme, and
invisible only where the page is dark). Two layers of fix: a `SpanStyle(color = ink)` over the
whole string in `personalAnnotated`, and `color = ink` on all three read-view styles.

### 2. The PDF's prints and waves

Both were the sheet's SECOND DRAWING of a block the page owns:

- **A print.** The page: a frame `fraction × personalPrintHeight(size)` with the photograph
  CROPPED into it. The sheet: decode the whole file, fit it inside a box, draw it whole — so
  a Small print or a portrait frame left the journal as a different picture at an aspect and
  a height the page never gave it. Now the frame, the 7dp pad, the centre crop and the label
  inside the frame are all the page's, and the decode returns the photograph UPRIGHT
  (platform `ExifInterface`, as the page's own Coil path already did).
- **A voice note.** The page draws a hand-drawn pulse (or the look the note wears); the sheet
  drew a filled pill and a bar chart. `drawVoicePulse` / `drawVoiceWave` are `internal` now
  and the sheet draws them through a `CanvasDrawScope` at `Density(PDF_UNITS_PER_SP)`, so the
  page's own dp geometry lands at the sheet's own scale. The control follows the look, the
  clock takes Fraunces at the page's alpha.

**Not a parity item (the member's own question):** a PDF cannot be made interactive here.
`android.graphics.pdf.PdfDocument` writes a flat sheet — no annotations, no link rectangles,
no scripts — so tap-to-tick, tap-to-play and text selection cannot exist in the file. The
interactive version of a page is the app's own read view; the PDF is its printed form. Doing
more would mean a PDF library (a new dependency) for links only, which would still not make
a tick box live. Left as it is, and said plainly rather than half-built.

### 3. The text bar's reach — LETTER MODE, AND IT IS DRAWN

- The bar's reach was rows only, which cannot pick a clause out of a line (the member:
  "why theres only row selection i also want letter by letter too"). LETTER MODE adds a
  window of characters inside the reach's FRONT row — one row on purpose, because a
  character range across rows IS those rows. The arrows move the far end a letter at a time,
  All letters takes the whole line, and Cut / Copy / Paste branch on the mode (Cut in letter
  mode edits the line's own text and mask, remembering the removed window for Undo).
- **The reach is now visible**, which is what it was missing: a picked row wears the page's
  selection wash, and the picked letters are a `SpanStyle(background = …)` span added LAST in
  `personalAnnotated` so the letters being picked are always the ones on show.

### Verification

- Brace/paren balance 0/0/0 on the three touched files, with a character scanner that knows
  Kotlin strings, raw strings, char literals and both comment kinds (a regex strip gives
  false alarms — it did, on `PersonalVoice.kt`).
- No Gradle in this environment — CI compiles it (per `AGENTS.md`).

### Still owed (from the previous batch)

**The Dev settings source-fetch lab** ("add a api test fetching in dev settings", answered
"Every content source") is NOT built — the gesture removal and the star map landed in that
batch, this one carried the four items above. It is a new surface, so it needs the
new-feature question first: whether a fetch lab should exist as a Dev-page section or stay
out of the app entirely.

## 11. Request — the PDF lays out print stacks and rows as the journal does

**The ask (member):** *"Make the PDF draw print stacks and rows as the journal lays them out"*

**What the sheet did:** every print was drawn ALONE, at the full measure, whatever row it
stood in on the page — so a pair came out as two stacked rows, a three as three full-width
pictures, and a print beside its line as the picture and then the line under it.

**What it does now** — the page's own grouping and the page's own shapes, in
`PersonalExport.kt`:

- `exportPrintPlan` runs the same pass the canvas runs: a run of prints up to
  `PRINT_ROW_LIMIT`, stepping over blank rows that are nobody's place, and a lone SMALL
  print taking the line under it as its companion (`rows` / `rowCells` / `beside`). The
  sheet has no caret, so it takes the READ VIEW's version of the air rule — the caret is
  not a fact a file can carry, and which pictures are ONE ROW is the one thing the passes
  must never disagree about.
- `drawExportPrintRow` builds the three shapes `PersonalPrintArrangement` builds: a pair
  side by side; a three as the upright frame — whichever member's size asked to stand,
  taken at least as wide as the widest print beside it — with the other two stacked in a
  column that keeps each print's OWN share; and a four as two lines of two, each line
  weighing itself as the page's Column of Rows does. The gap is the canvas' own
  `PRINT_ROW_GAP`, and a cell's height is its own size's picture plus `PDF_PRINT_BAND` and
  its label's room.
- `drawExportPrintBeside` is the pair the page draws for a lone SMALL print: the print
  takes `printBesideShare(size)` of the measure, its line takes the rest, 10dp between —
  and the line is laid out at THAT column's width, so it wraps where the page wraps it.
- The row is decided and drawn as ONE block (it fits the sheet or starts the next one),
  and `drawExportLines` places a line's cells where the row decided and moves the sheet
  once, by the line's own height — that is what `PdfRun.setCursor` exists for.
- Four canvas members became `internal` for this, so the sheet cannot invent its own
  numbers: `PRINT_ROW_LIMIT`, `PRINT_ROW_GAP`, `printBesideShare` (and, from the previous
  pass, `personalPrintHeight` / `PersonalPhotoSize` were already reachable).

**Not a parity item, and named rather than half-built:** a BUBBLE voice look's tinted
bubble, and the voice strip's tap-to-seek — nothing on paper can be scrubbed.

### Verification

- Brace/paren balance 0/0/0 on both touched files (the character scanner, not a regex).
- No Gradle in this environment — CI compiles it (per `AGENTS.md`).

### Still owed

**The Dev settings source-fetch lab** (asked two prompts ago, answered "Every content
source") is still NOT built — it is a new surface, so it needs the new-feature question
(Dev-page section, or out of the app) before it is written.

## 12. Request — the Dev settings source-fetch lab, for every content source

**The ask (member):** *"Build the Dev settings source-fetch lab for every content source"*
(the original wording was *"add a api test fetching in dev settings"*, answered
"Every content source" — it had been owed for two batches and is NOW BUILT).

**Answers taken before implementing** (ask_user): **A section on the Dev page** (no switch of
its own — the Dev page is already the gated surface) · **Both, on one screen** (the summary
first, the raw payload folded under it) · **Fixed test queries only** (no editable query box).

**What changed** — `features/settings/SourceFetchLab.kt` (new) + one `item { … }` in the Dev
page (`ExperimentsScreen.kt`), first on the page:

- **It asks the ENDPOINTS, not the app's fetchers.** Every fetcher returns a parsed type and
  throws the payload away, so a lab built on them could only ever say "it worked". Each entry
  builds the SAME URL the app's own file builds and names that file (`where`) so drift is
  findable: `ArtworkSheet.kt` (Met, Wikipedia), `MuseumFetch.kt` (Cleveland), `AlbumArtFetch` /
  `SongArtFetch` (iTunes, MusicBrainz, Cover Art Archive), `ExploreSearch.kt` (Spotify),
  `BookEnrichment.kt` / `BookCoverFetch.kt` (Open Library, its covers, Google Books,
  LibraryThing, Crossref), `StandardEbooksFetch.kt`, `ComicVineFetch.kt`, `SeriesEpisodeFetcher`
  / `SeriesPosterFetch` (TVmaze), `TmdbFetch.kt`, `MangaFetch.kt` (AniList GraphQL POST,
  Jikan, Kitsu, MangaDex).
- **Twenty sources**, grouped Artwork / Music / Books / Screen / Anime & manga, each with one
  fixed query the service is known to answer (a famous painting, album, book, series, manga).
- **A run reports** the HTTP status, the milliseconds, the bytes, the content type, a one-line
  reading of the payload (`describePayload` — counts, the first array's shape and the first
  result's own field names, a source's own `errors[]` for GraphQL, "an HTML page, not an
  answer" when a door is blocked) and the body itself, folded under it (20 000 chars, then a
  truncation note). An image door counts bytes instead of keeping a megabyte of noise.
- **A keyed door says so**: `keyName` names the `BuildConfig` value the app itself would use,
  and a build without it shows "Needs X — not set in this build" with the button disabled —
  a 401 would be a fact about the build, not about the source. Spotify runs the client-credentials
  token step `ExploreSearch` runs. Google Books is keyless here (its key is optional there).
- **Run all** is sequential with a 400 ms pause (MusicBrainz asks for one request a second),
  and every probe is on demand: nothing persists, nothing fires by itself.

**Noted, on purpose:** the lab duplicates URLs rather than reaching into the fetchers' private
builders, which is the honest cost of showing raw payloads; the `where` on every entry and the
`app/AGENTS.md` entry are what keep that duplication checkable.

### Verification

- Brace/paren balance 0/0/0 on both touched files (the character scanner).
- An import/usage sweep over the new file: every symbol used is imported or same-package
  (`SettingsSectionHeading`, `SettingsOptionCard`, `CurioSettingsDivider`); Kotlin builtins
  (`ByteArray`, `Charsets`, `Regex`) and `android.util.Base64` (fully qualified) need none.
- No Gradle in this environment — CI compiles it (per `AGENTS.md`).

## 13. Request — print stacks and rows in the PDF (re-sent); the frame's own paper

**The ask (member):** *"Make the PDF draw print stacks and rows as the journal lays them
out"* — the SAME words as section 11, re-sent after that pass had already shipped
(`9f810e37`). So this pass began by checking what §11 left undone rather than rebuilding it.

**What §11 had already done (verified in the tree, not from memory):** `exportPrintPlan`
still runs the read view's own grouping pass (a run up to `PRINT_ROW_LIMIT`, stepping over
nobody's blank rows, a lone SMALL print taking the line under it), and `drawExportPrintRow`
still builds the page's three shapes — the pair, the three with its upright frame and its
stacked column, the four as two lines of two — from the canvas' own `PRINT_ROW_GAP`,
`printBesideShare`, `personalPrintHeight` and `PersonalPhotoSize`. The rows were in place.

**What was still wrong — a print's HEIGHT.** A print on the page is a picture inside paper
that the page draws the pads for (`renderPrint`): 7dp above the picture, 5dp between the
picture and its label, 5dp of band left under the label, 2dp of frame at the bottom — and
the band is **ALWAYS there**, empty or not (v400: the blank band carries a non-breaking
space, so an uncaptioned print is still the print the member saw). The sheet:

- kept ONE of those five bands and drew the picture flush at its cell's top, so any print
  stood **14dp shorter** on paper than on the page;
- returned **no band at all** for a print with nothing written under it — a quarter of a
  Small print's height gone;
- and because a ROW's line is as tall as its tallest cell, every row of prints measured
  short with it — which is exactly what "as the journal lays them out" fails on.

**What changed** (`PersonalExport.kt` only, 45 insertions / 23 deletions):

- `PDF_PRINT_FRAME` = `PDF_PRINT_PAD` + `PDF_PRINT_BAND` ×2 + `PDF_PRINT_EDGE` (the new 2dp
  lip) — one name for the paper a print's frame adds, with the page's own pads written out
  in its comment.
- `drawExportCell` insets the picture by `PDF_PRINT_PAD` (the frame's top pad) and places
  the label from that inset picture's foot, so a print is drawn where the page draws it.
- `exportCaptionRoom` **always reserves the label's line** (a stamp adds its own), and
  `drawExportPrint` reads its band from that same helper instead of a second `when` of its
  own — so a lone print and a cell of a row cannot measure differently.
- `exportCellHeight` and the beside pair's row height both go through `PDF_PRINT_FRAME` /
  `exportCellHeight`, so the rows inherit the correction.

**Not changed, and why:** the sheet still has no frame *rectangle* (its paper IS the page's
paper, so the frame shows only as the pads), and the caption's line box is still the sheet's
1.7 × label size approximation of the page's text line — that one is shared with every text
block on the sheet and is a separate question from the frame.

### Verification

- Brace/paren balance 0/0/0 on the touched file (the character scanner, not a regex).
- `grep` sweep: no leftover references to the removed `hasCaption` / `captionRoom` locals,
  and every `PDF_PRINT_BAND` / `PDF_PRINT_FRAME` use is one of the five intended ones.
- No Gradle in this environment — CI compiles it (per `AGENTS.md`).

### CI fix on top (the same file)

`compileReleaseKotlin` failed at `PersonalExport.kt:1359` — "actual type is
android.graphics.Canvas, but androidx.compose.ui.graphics.Canvas was expected": the voice
note's wave (v427, the pass before this one) handed the PAGE's canvas to a
`CanvasDrawScope`, and the two canvas TYPES are different — the page's own drawing is a
Compose `DrawScope`, a `PdfDocument` page is an `android.graphics.Canvas`. Compose's
`Canvas` is imported as `ComposeCanvas` and the wave is drawn into an `ImageBitmap` at the
sheet's own measure (`Canvas(image)` + `CanvasDrawScope`, the bridge
`SocialNotifications`' avatar cache already uses) and blitted onto the page — no API asked
for that the page does not already draw with.

### Note for the member

§11's row runner had already been pushed when this arrived, so if the rows are STILL not
what the journal shows on their device, the useful thing to report is which shape they are
looking at (a pair, a three, a four, or a print beside a line) — the shapes are one code
path each and each can be checked on its own.

## 14. Request — the Incursion full redesign (+ the PDF/print question, + a CI fix)

**The ask (member):** *"so the pdf exactly shows the images as they are on journal with caption
dates ? now do the incursion full ui redesign with better ui matching the app also other features
for it and instead of essential tab add personal tab where [the] status shows, also use proper
icons in that ui. proper description etch etc."* — plus the CI error pasted with it.

**CI first, because the build was red:** `compileReleaseKotlin` failed at
`PersonalExport.kt:1359` — *"actual type is `android.graphics.Canvas`, but
`androidx.compose.ui.graphics.Canvas` was expected"*. The voice note's wave (the pass before this
one) handed the PAGE's canvas to a `CanvasDrawScope`; the two canvas types are different. Fixed by
importing Compose's `Canvas` as `ComposeCanvas` and drawing the wave into an `ImageBitmap` at the
sheet's own measure (`Canvas(image)` + `CanvasDrawScope` — the bridge `SocialNotifications`'
avatar cache already uses), then blitting it. Pushed as `4cbf569a`.

**The PDF/print question, answered:** yes for geometry and yes for the caption dates. The frame,
the crop, the size's own share and height, the pads, the label inside the frame and its stamp all
come from the page now, and the always-present band is drawn. Two honest caveats: the caption's
LINE BOX is still the sheet's 1.7 × label-size approximation of Compose's text metrics, and the
sheet draws no frame rectangle or shadow (its paper IS the page's paper, so the frame shows only
as the pads).

**Incursion — answers taken before implementing** (ask_user): the Personal tab is **a status desk
+ the six-state tally + the derived figures** ("all 3") · features: **posters, Next up, one search
across lines, your own note** (NOT hide-watched, NOT share) · **Personal tab last** (Essentials
loses its destination) · **no new toggle** — and none was needed: the app already has one switch
for fetched artwork (`AppPreferences.coverFetchEnabledState`, the Experiments page's "Cover
fetching"), which the posters ride.

**What was built:**

- `features/incursion/IncursionPoster.kt` (new) — `IncursionPosters.resolve` (TMDB by the row's own
  `tmdbId`, then the keyless `FilmPosterFetch` / `SeriesPosterFetch`, memoised per `storageKey`
  WITH misses) + `IncursionPosterPlate` (the poster, or the app's drawn plate: the order + a kind
  glyph). Consent-gated, resolved per composed row.
- `features/reveal/TmdbFetch.kt` — `posterUrlById(id, isShow)` with its own id cache.
- `data/IncursionStore.kt` — `noteState` / `note()` / `setNote()` (one line per title, blank
  removes, `NOTE_LIMIT` 160, its own prefs key), and `Status.shortLabel` beside `Status.label`.
- `features/incursion/IncursionPersonal.kt` (new) — the desk: whole-order progress, a bar per line,
  next up per line, the six-state tally, the figures (hours in / hours left / essentials / longest
  run). Read-only; `minutesOf` counts a series as per-episode runtime × episodes covered.
- `features/incursion/IncursionScreen.kt` — `PERSONAL` replaces `ESSENTIALS` as a destination (with
  real icons: reel / play / spark / person), one search across all three lines (`StudioBand` heads),
  `nextUp` in the head, posters on rows and tiles, and the sheet rebuilt on the poster with one
  scrolling chip row, `ABOUT`, `WATCH FIRST`, `YOUR NOTE` (debounced 500 ms) and Done.
- `features/settings/UserExperimentsScreen.kt` — the Cover fetching switch's description now names
  the Incursion art it also covers.

### What is still owed / named, not done

- `IncursionSurfaces.kt` (the unlock reveal and Home's door) was left alone: nothing in this request
  asked for it, and it already wears the v425 card language.
- The desk's figures are counts of the statuses only — no per-week history, because the store keeps
  no timestamps. Adding them would be a data change, not a UI one.

### Verification

- Brace/paren balance 0/0/0 on all five touched files (the character scanner, not a regex).
- Symbol sweep: no references left to `IncursionDestination.ESSENTIALS`, `statusInk(` (renamed
  `incursionStatusInk`), or the old `statusInkFor`; every new file's imports are used.
- No Gradle in this environment — CI compiles it (per `AGENTS.md`).

## 15. Request — the PDF's prints went missing, and a journal's own colour

**The ask (member):** *"the pdf doesnt export exactly as the images as shown with stacks and now
there is no images at all so fix it, make the journal theme[-]not[-]aware and it have its own
theme if user wants to change and its strip or [the] door for that particular journal gets that
color and by default they follow the theme only the changed color stays as it looks"*.

**1. The prints, first (shipped as `3a48c9f7`).** A photograph whose bytes could not be read was
silently DROPPED from the sheet — the decode's failure path returned early and drew nothing at
all, which is exactly "there is no images at all" for a page of photos (a `content://` or file
URI that the sheet's own bare `BitmapFactory` decode cannot open, a large file, a path handed
over as `file://`). Now: the decode retries smaller and accepts a bare path as well as a
`file://` one, and a picture that still cannot be read is drawn with the page's own PLATE rather
than vanishing — a print the page has is a print the sheet has.

**2. A journal's own colour — answers taken first** (ask_user): the picker offers **the app's
own hues + a wheel** · what wears it: **Home's journal chips, the journal list's spine, and the
palette button in the page's tools** · and the theme stays the default.

What was built (v428):

- `data/PersonalEntity.kt` — `PersonalNoteEntity.accentArgb: Int = 0` (**0 means follow the
theme**, which is the honest backfill and the reason no page changed colour on the update),
  `hasOwnAccent`.
- `data/CurioDatabase.kt` — version **22** + `MIGRATION_21_22` (`INTEGER NOT NULL DEFAULT 0`),
  registered with the rest.
- `data/PersonalDao.kt` — `setNoteAccent(id, argb, now)` (column-scoped: a colour is not a reason
  to rewrite a page's words) + the repository's `setNoteAccent`.
- `features/personal/JournalAccent.kt` (new) — `journalDoorAccent(argb)` (the theme's accent ink,
  or the stored ARGB), `journalHueChoices()` (the app's own hues, each read through
  `CurioNamedTheme.heroFor` so a swatch IS the colour the app paints with), and
  `JournalAccentSheet`: the swatches, an HSV wheel (sweep gradient + saturation falloff + a value
  wash) with a brightness slider, applying LIVE — the page's own debounced writer is what
  persists a drag as ONE write.
- `features/personal/PersonalCanvas.kt` — the dock's palette door (it WEA*RS* the colour in hand,
  and only appears when the page can keep an answer) + `journalAccent` / `onJournalAccent` params.
- `features/personal/PersonalPage.kt` — `PersonalPageMeta.accentArgb` and `writePage` passing it,
  which is the load-bearing half: the writer REBUILDS the whole entity from the meta, so a field
  left out there is a field erased on the next keystroke.
- `features/personal/JournalEditorScreen.kt` — the page's own state, loaded in `onLoaded`, handed
  to the core, and the setter (the topic-note, chapter and book-review pages pass nothing, so they
  never grow a door that could not store its answer).
- `features/personal/JournalListScreen.kt` / `PersonalHome.kt` — the row's spine and Home's chip
  wear `journalDoorAccent`.

### Verification

- Brace/paren balance 0/0/0 on every touched file (the character scanner, not a regex).
- Sweep for every construction site of `PersonalNoteEntity` (three): the two book paths build a
  new row and keep the theme default; the page's writer now carries the colour.
- No Gradle in this environment — CI compiles it (per `AGENTS.md`).

## 16. Request — the bar's four arrows (no mode switch, built from the bottom)

**The ask (member):** *"also while youre at journal, fix the copy paste etc ui also proper arrow up
down left right arrow and no more letter row option but the arrows do the work, also dont let user
select things starting from bottom too"* — where the last clause was already answered once as
*"let user select things from bottom proper tool of how it should behave"* (§10), so it is read as
"a selection must be BUILDABLE from the bottom", which is what this pass implements.

**What changed** (`PersonalCanvas.kt`):

- `pageLetterMode` is **gone** — the two units are the two AXES. ↑ ↓ rows, ← → letters, and the
  letters switch themselves on at the first ← or → (`pageLettersPicked`).
- `PersonalEditorState.nudgePageRows(up)` replaces `growPageSelection` / `shrinkPageSelection`: a
  fresh reach is ONE ROW (the caret's, else the page's last), the arrow pressed FIRST decides
  which way it grows (`pageGrowsUp`), the other gives a row back, and at a single row it WALKS
  that row — so the foot of a page can be moved without a fifth control and down is never a dead
  end at the page's end.
- `nudgePageLetters(more)` replaces `growPageChar` / `shrinkPageChar` / `selectAllPageLetters`:
  the first press opens a one-character window (at the caret, else the anchor row's end for an
  up-growing reach and its start for a down-growing one), and `selectPageLineLetters()` is the
  bar's "Line".
- The ANCHOR row is `pageAnchorIndex()` — the reach's foot when it grew upward, its head when it
  grew downward — and `pageRowCharRange` / `cutPageLetters` / `pageSelectionText` all read it
  through that one function. `selectWholePage()` draws itself as a bottom-up reach, so letters
  after All rows come from the last row.
- The bar is TWO ROWS: the row arrows with the count beside them, then the letter arrows with
  Line / Cut / Copy / Paste / Undo. The count is one sentence ("3 of 12 rows" and, only once
  reached into, "· 5 of 24 letters"), the arrows are real icons (`PageArrowChip` — `ArrowUpward` /
  `ArrowDownward` / `ArrowBack` / `ArrowForward`), and **every arrow dims when its axis has
  nowhere to go** (`canNudgePageRows` / `canNudgePageLetters`).

### Verification

- Repo-wide sweep: no reference to `pageLetterMode`, `togglePageLetterMode`,
  `growPageSelection` / `shrinkPageSelection`, `growPageChar` / `shrinkPageChar` or
  `selectAllPageLetters` is left anywhere, and `app/AGENTS.md`'s v427 entry was rewritten as the
  v427 → v428 contract.
- Brace/paren balance 0/0/0 on the touched file.
- No Gradle in this environment — CI compiles it (per `AGENTS.md`).

## 17. Request — Incursion again, the posters, the watch button, the journal's stacks, the studio dock

**The ask (member):** *"the incursion ui is still bad, the profile page doesnt even open. and the posters
are not loading for films and in incursion movies or series, and for series in incursion use what the
series category buttom sheet uses with episode guide. also add watch icon button in rows and grid. only
watch button also for the journal photo stacking sometimes they unstack and also when they are separated
with a text or maybe i added one very later, they dont stack, similar for voice note they dont move that
better then fix the glitchy dock in save your take, its a little glitcy"* — plus a follow-up while the
batch was being planned: *"also tmbd read its docs for better implemetayipn for apps i think there are
two keys."*

**Answers taken before implementing** (ask_user): the page that does not open is **Incursion's Personal
tab** · the watch button is **one tap marks it Watched, tap again to undo** · stacks join **"only when
they end up next to each other"** (dropping onto a row joins it; a picture added later joins the row it
lands beside) · the studio dock **"jumps or flickers when the keyboard opens/closes"**.

### 1 · The Personal tab — the bug, found

`destinationId = destination.takeIf { id -> studioDestinations.any { it.id == id } }` — the validity check
listed **studio ids only**, so `"personal"` failed it and every tap on Personal was answered with
**Marvel**: `onPersonal` could never be true and the desk never composed. Fixed to check all the tabs
the nav bar draws (which also covers a `rememberSaveable` rotation), and the desk is now asked about
BEFORE the list/filter branches — it used to draw only in the one state where a search was empty AND no
filter was on, so a filter turned the desk into the filtered-empty page. The filter row is not drawn on
the Personal tab at all (a desk has no rows to filter; a SEARCH still is, and is answered across lines).

### 2 · The posters — both causes measured against the live endpoints

- **Films: iTunes' movie search is DEAD.** `itunes.apple.com/search?term=…&media=movie` answers
  `{"resultCount":0,"results":[]}` for every title, every storefront and every entity spelling, while
  `media=music` on the same endpoint still answers — so the door that closed is Apple's FILM catalogue,
  not the search. `FilmPosterFetch` therefore had one dead leg and TVMaze (television, not cinema)
  behind it. The new primary door is **Wikipedia's own article image**: a film article leads with its
  poster, the REST summary endpoint serves it, and it needs no key. FilmPosterFetch now tries
  `Title (Year film)`, `Title (Year)`, `Title (film)`, `Title`, then ONE search whose first two hits are
  tried the same way — measured over the X-Men lane's films: 8/10 on the guesses alone, and every miss in
  that sample was a 429 from my own test script, not a real gap.
- **Series: the rows name their season.** `"WandaVision S1"`, `"Loki S2"`, `"The Gifted S1"` — a name no
  catalogue has, so TVMaze answered nothing for any of them (measured: 14/14 hit once the season came
  off, including the `&` → `and` spelling TVMaze files). One shared `stripNaming` (film door, series
  door, episode guide) strips `(\d{4})` **and** `S<n>` / `season <n>` in a loop, "Loki S2 (2023)" included.
- **TMDB's two keys, per its docs.** Its application-authentication page: v3 is `?api_key=`, or the
  **API Read Access Token** as `Authorization: Bearer …` — *"valid across both the v3 and v4 methods"*.
  `TmdbFetch` now reads `TMDB_READ_TOKEN` first and falls back to `TMDB_API_KEY`, sends the token as the
  header, and recognises a JWT pasted into the key field (starts `eyJ`). New `buildConfigField`, both
  workflows export the secret, `.env.example` and `.github/AGENTS.md` document it.

### 3 · The watch button, the guide, the stacks

- **Watch button** on rows and grid tiles: one tap = Watched, tap again = back to not watched, with a
  `visibility` / `check` glyph (both ligatures verified present in the bundled Material Symbols subset).
- **Episode guide** on a series' sheet, from the reveal's own `SeriesEpisodeFetcher` (TMDB first when a
  key is set, TVMaze keyless behind it), seeded from its cache, 12 rows then "Show all N", grouped by
  season when the show has more than one.
- **The unstacking, found:** `printRowGapSteppable` refused to step over a blank line **while the caret
  was in it** — and that is exactly where the caret lands after almost any picture is added. Two prints
  with only air between them were therefore ONE row in the eye view and TWO lone prints in the pen view,
  which breaks the project's own rule that the two passes must never disagree. The caret no longer breaks
  a row; the one gap holding the caret is left visible and drawn under the row.

### Not done, and why (named, not silently dropped)

- **The voice note's movement.** No diagnosis I can stand behind from here; the drag is a measured-slot
  machine (`PersonalRowDragState.advanceBy`) that v400 already re-derived. Needs one detail: what fails
  (will not pick up / jumps on the way / the guide line lands off), given the grip already accepts a
  plain drag.
- **The studio dock's keyboard jump.** `SaveCaptureScreen` has the screen's IME inset applied **once**,
  on the floating note pill (`SessionNoteFloatingPill`, inside the scroll area), while the window is
  `adjustResize` — the two can disagree, which is the classic jump. Changing it blind risks hiding the
  pill behind the keys, so it wants one detail too: does it jump TOO FAR (double inset) or STUTTER.

### Verification

- Brace/paren balance 0/0/0 on every touched file (the character scanner, not a regex).
- Every poster lookup above was measured against the live endpoints with `curl`, not recalled.
- No Gradle in this environment — CI compiles it (per `AGENTS.md`).

## 18. Request — TMDB: read its docs for a better implementation ("i think there are two keys")

**The ask (member):** *"also tmbd read its docs for better implemetayipn for apps i think there are two
keys."* — a re-send of the follow-up §17 already carried, so this pass started by **verifying what the
docs actually say against what the tree does**, not by rebuilding the two-credential work.

### What the docs say (read, not recalled)

- **Authentication** (`developer.themoviedb.org/docs/authentication-application`): *"Version 3 is
  controlled by either a single query parameter, `api_key`, or by using your access token as a Bearer
  token"*, and the read token *"has the added benefit of being a single authentication process that you
  can use across both the v3 and v4 methods"*. **The member was right: two credentials.**
- **FAQ** (`/docs/faq`) — the part that was NOT implemented anywhere: *"Our API is free to use for
  non-commercial purposes **as long as you attribute TMDB** as the source of the data and/or images."*
  and *"You shall place the following notice prominently on your application: 'This product uses the TMDB
  API but is not endorsed or certified by TMDB.' … the attribution must be within your application's
  'About' or 'Credits' type section."* Plus: the logo is required, only an approved one may be used, and
  *"should not be modified in color, aspect ratio, flipped or rotated"*; the link must point to
  `https://www.themoviedb.org`.
- **Logos & attribution** (`themoviedb.org/about/logos-attribution`): the five approved SVGs, with the
  brand colours (`#0d253f`, `#01b4e4`, `#90cea1`).
- **Rate limiting**: the legacy limit is disabled; ~40 req/s remains, and a 429 should be respected.
  `getJson` already degrades a non-200 to "ask the next provider", so nothing to change.
- **Image basics**: base_url + size + path. The hardcoded `image.tmdb.org/t/p/w500` is the documented
  shape; left alone.

### What was already right (verified, unchanged)

`TmdbFetch` reads `TMDB_READ_TOKEN` first and falls back to `TMDB_API_KEY`, sends the token as
`Authorization: Bearer …` (and never alongside `api_key`), recognises a JWT pasted into the key field,
and both workflows export both secrets. No change needed — §17 got the two keys right.

### The three real gaps this pass closed

1. **Attribution — missing entirely.** TMDB's terms make it a condition of the free API, and the app
   shows TMDB artwork and facts on the film sheets and on Incursion rows. **Asked, then built:** the
   member chose *a row in Settings → Support → About Curio* (the app's Credits section — the place the
   FAQ names) *and* the official logo. The row carries the notice **verbatim**, `plain = true` so it can
   never be cut off, links to `themoviedb.org`, and is drawn **only when `TmdbFetch.isConfigured`** — a
   keyless build never asks TMDB anything, so there is nothing to attribute. The mark is TMDB's
   **unmodified** approved "Primary short (blue)" SVG, vendored byte-identical as `R.raw.tmdb_logo` and
   drawn through Coil's `SvgDecoder`; `SettingsOptionRow` gained an optional `logoRes` and
   `SettingsOptionIconTile` a brand-mark branch that **tints nothing** (the branding rules forbid
   recolouring it).
2. **The Dev source lab would have lied about TMDB.** Its probe built `?api_key=` from
   `BuildConfig.TMDB_API_KEY` and gated on that field alone — so a build holding only the read token read
   as *"Needs TMDB_API_KEY — not set in this build"* while the film sheets in it were resolving posters
   fine. `TmdbFetch.apiKey` / `readToken` are now `internal` (one resolution, one place), `keyOf` returns
   whichever credential the app would really send, the row's new `keyLabel` names both fields, and the
   probe authenticates the way the app does (Bearer when a token exists, `?api_key=` otherwise).
3. **The CI run summary only reported `TMDB_API_KEY`.** `build-summary.sh` now uses an `add_provider`
   helper that takes one-or-more vars per provider, so TMDB counts as present when EITHER secret is
   exported, and the label still prints once.

### CI fix on top (the journal-colour pass's own bug)

`compileReleaseKotlin` failed at `JournalListScreen.kt:285` — *"@Composable invocations can only happen
from the context of a @Composable function"*: §15's spine drew `color = journalDoorAccent(journal.accentArgb)`
**inside `drawBehind`**, which is a DRAW pass, not a composable one. Its own doc comment even says the two
read views must agree, and the chip beside it in `PersonalHome.kt` was already right — `JournalChip` hoists
`val own = journalDoorAccent(...)` in the composable scope and captures it in the lambda. `JournalRow` now
does the same (`val spine = …` above the `Surface`), and that rule is worth keeping in mind for every
colour a spine, a chip or a door wears: **read it in the composable, capture it in the draw.** The mangled
indentation that edit left behind is gone with it.

### Verification

- Both credentials, the attribution sentence and the logo rules were read from TMDB's own pages (above).
- `bash -n .github/scripts/build-summary.sh` → syntax OK.
- Brace/paren balance 0/0/0 on every touched Kotlin file (the character scanner, not a regex).
- Every other call site of a @Composable colour helper was checked for the same mistake: `JournalChip`
  (`PersonalHome.kt`) hoists its colour, and `PersonalCanvas`'s `accent: Color = personalAccent()` is a
  composable function's DEFAULT PARAMETER (legal, and pre-existing).
- No Gradle here — CI compiles it (per `AGENTS.md`).

## 19. Request — the align tool, the scrolled bars, the sketchbook Signature, the post's card editor

**The ask (member):** *"now collapse the alignments into one option they show in that tool row for
journal, add remeber state to the scrolled tool bar, now for signature desings for share card, remove
the classic or siganture for all except games and films, and now make the design like collections style
like hand drawn doodles got it, now do one unique per category for signature cards, and then for the post
share card show the topic reveal share card buttom sheet editor and the editing layout ration stays
intact in each scren of post dont push this yet"*

**Answers taken before implementing** (ask_user): the doodle look is a **sketchbook page** ("cream/kraft
paper, wobbly hand-drawn ink frame, scribbled hatching, an arrow or two, washi-tape corners and a
hand-lettered title — a drawn page, one per category"); Games and Films keep **both** the doodle and the
Classic (*"Classic only for Games + Films"*); and the post's card editor **opens from the post card
screen** (*"the post preview itself gets the ratio switch, and the editor sheet is opened from the post
card screen"*).

### What was built

1. **One align tool** — the dock's four alignment buttons (left / centre / right / justify) became one
   button wearing the focused line's own alignment, with the four choices in the menu behind it
   (`PersonalAlign.toAlignKind()`, `MarkerMenuLabel` rows, a check on the current one).
2. **Scrolled tool bars remember** — the dock's row, the text bar's two arrow rows and the caption row
   each own a `rememberSaveable(saver = ScrollState.Saver)` state instead of a fresh
   `rememberScrollState()` inside the row, and the dock's is hoisted ABOVE its own bar swap — which is
   the part that actually mattered: a row wider than the phone was resetting the moment the dock
   changed bars.
3. **The sketchbook Signature** — new `ui/components/SignatureSketchbook.kt` (914 lines): a shared pen
   (`sketch` / `sketchRect` / `sketchCircle` / `hatch` / `tape` / `sketchArrow` / `sketchPaper`), a
   deterministic `jitter` so the drawing never shimmers and the PNG matches the editor, **one motif per
   lane** (38 of them, none shared), and a per-category page table (paper, pen, marker, layout, doodle)
   with the FAMILY as the second key for a lane whose name it does not know. `TopicShareCard.kt`'s
   `signatureDesign(...)` lost its 74,940-character `when` (the thirty-eight printed scenes) and is now
   a delegator; `SignatureDesign` / `SignatureLayout` became `internal`, and `signatureHairlineFrame`
   went with the layouts that used it.
4. **Classic only for Games and Films** — `signatureClassicAvailable(name)` is the single gate, read by
   the card's own render, by the editor's Classic toggle, and by the Share Hub, whose grid is now built
   per picked category (`hubDesigns(classicSignature = …)`) and finds its per-category heading from the
   first override cell rather than a hard-coded index.
5. **The post's card editor** — `TopicShareSheet` gained `initialAspect` + `onAspectChanged`; the
   post preview wears the shape switch and an **Edit card** door, and the sheet is composed in the
   composer's own column (never inside a lazy item, where a sheet dies when its item scrolls away), so
   one post keeps one shape on the composer, the wall and its card screen.

### Not pushed, on purpose

The member said *"dont push this yet"* — so this batch is committed locally and left unpushed. It is
NOT verified by CI yet: this environment cannot compile (see `AGENTS.md`), and every claim above is
backed by reading the code and by the brace/paren balance checker, nothing more.

### Verification

- Brace/paren balance 0/0/0 on all six touched files (the character scanner, not a regex).
- The 74,940-character deletion was done by brace-matched replacement (anchor + a depth-counting scan,
  string- and comment-aware), then re-read: the delegator sits where the old `when` did, the two types
  it needs are `internal`, and `signatureHairlineFrame` is referenced nowhere.
- Every call site of the two things this pass changed shape (the sheet's aspect, the hub's design list)
  was re-read after the edit.

## 20. Request — the dead keys, OMDb, Google Books, the episode guides, the watch mark, the drawer's lines, the PDF's zoom

**The ask (member):** *"these api keys are not working, library thing, it gives http 403 text/html
error, tmdb no answer 24060 ms, jokan my animelist http 504 application/json, these erorr, also add
omdb key for fethcing if any doeasnt fetch, the wacth button in incursion ui is not right, wire
google book fetching for chapters etc if nothing resturns use more fallbacks, for incursion ui the
movie posters decsription doesnt fetch, use all the avalabel pai also i added cmoicvine api too mybe
use that for incursio with more fallbacks, the series for incusrion also doesnt load episode guides
use the same on the category series uses with its ui, all other api works so add proper fallback for
things that didnt fetch for artworks et everything, also do what else we can fetch from the api that
we are not fetching. then in drawer ake the pattern straight lines a little curvy just a littlr, and
for pdf in vertical scrolling the zoom is still a little inaccurate and glitchy fix it its zooming at
the top."*

Asked and answered: **the watch button's mark** — a play triangle was the wrong idea; the member
chose **an eye** (outline when unwatched, filled/lit once watched).

### Findings (measured, not assumed)

- **LibraryThing's covers host refuses apps.** `covers.librarything.com` is behind a Cloudflare JS
  challenge: every request, any User-Agent, answers `403 text/html` ("Just a moment…") — a native
  client cannot pass it and no key changes that. The same ISBN through
  `covers.openlibrary.org/b/isbn/…` answers `200 image/jpeg`.
- **The 24-second TMDB stall was arithmetic, not a key.** `TmdbFetch.facts()` is a film search, a
  show search and a detail read; `showEpisodes()` adds a season each; every read allowed 8s to
  connect and 8s to answer. Three reads deep that is the member's own measurement.
- **Jikan answers `504` from its own gateway** (MyAnimeList's), so it is a door that may not answer
  at all — it needs a short budget and real doors behind it, not a retry loop.
- **`SeriesEpisodeFetcher.fetchForAny` ran its two doors in SEQUENCE** (keyed first, each with its
  own long budget first), which is why a series' guide "doesn't load" in Incursion.
- **The scrolling PDF's anchor was right only for the first sheet** (see the app/AGENTS.md note):
  `focus` alone omits every sheet above the one being pinched.

### What was built

1. **OMDb** — new `features/reveal/OmdbFetch.kt` (plot, poster, rating, runtime, genres, IMDb id)
   + `OMDB_API_KEY` in `app/build.gradle.kts`, BOTH workflows, `.env.example`, `build-summary.sh`,
   `.github/AGENTS.md`, and an `omdb` row in the Dev source lab.
2. **Wikipedia as ONE shared door** — new `features/reveal/WikipediaSummary.kt` (page, prose, lead
   image, `Kind` bracket rules), used by Incursion's rows AND the shelf's book enrichment.
3. **Incursion's own chain** — new `features/incursion/IncursionSources.kt`: `record()` (description
   + facts) and `artwork()` (last-resort posters), staged and raced, consent-gated, memoised with
   misses; the sheet draws the fetched description and its facts as pills; `IncursionPosters`
   reaches the new nets after its keyless pair; `ComicVineFetch.movie(query)` makes the comics key
   useful for a film line.
4. **Episode guides** — `fetchForAny(title, season)` races TMDB and TVMaze under short budgets, a
   season hint reads one season instead of four, and Incursion passes the row's own season.
5. **Fail fast everywhere it matters** — TMDB 3.5s/5s + a whole-read cap, TVMaze 4s/5s, the Jikan
   doors 4s/5s/7s-call, every Incursion door on a 6s cap, every raced door on a 7s cap.
6. **Books** — `googleBooksVolume` answers chapters AND page count AND blurb from one read;
   `WikipediaSummary` is the last net for a book's about-text; ISBN covers are served by Open
   Library's ISBN door with LibraryThing's host named honestly (hub label + source lab note).
7. **The watch mark is an eye** (drawn: hairline arcs + pupil, or a filled almond with the pupil
   punched out in the button's fill).
8. **The drawer's constellations bow** — shallow quadratic joins, `min(span*0.10, 6dp)`, side fixed
   by the pair's indices.
9. **The scrolling PDF's zoom** — `sheetsAbove` (= `page * <one sheet's magnified height>`) threaded
   into `readerZoomDocument` and `readerDoubleTapDocument`; the anchor is `(sheetsAbove + focus.y) *
   (ratio - 1)`, so padding and inter-sheet gaps drop out by construction.

### Follow-up (the member's own CI log + one more ask)

- **CI failed on `3f35aca3`:** `TopicShareCard.kt:8036 Unresolved reference 'LaunchedEffect'` — the
  ratio-reporting effect added with the post's own card editor used the bare name, where this file's
  own convention is the QUALIFIED `androidx.compose.runtime.LaunchedEffect(...)` (it has no runtime
  import for it, and 25 other call sites spell it out). Fixed to match; the file now has no bare-name
  reference left (checked).
- **"also fetching artworks, painting fetching proper fail safe too"** — the art lane had the same two
  holes: a work neither museum holds was re-asked of all three doors on every open, and every door was
  on an 8s+8s budget with up to eight SEQUENTIAL record reads inside a single one of them. See the
  v429b note in `app/AGENTS.md`: a confirmed miss is remembered (a FAILED door is not), the budgets are
  4s/5s, the Met's record reads run together, the direct-slug Wikipedia read falls back to the shared
  search door with `Kind.ART`, and the lead image is the thumbnail on purpose.

### Verification

- Brace/paren balance 0/0/0 on all eleven touched Kotlin files (character scanner, string- and
  comment-aware), plus the five touched by the follow-up.
- Every changed call site re-read after its edit (the sheet's guide call, the poster chain, both
  zoom anchors, the lab's rows, the cover provider).
- **CI compiles it — that is the only build gate in this environment** (no Gradle allowed here).

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — §20 and its follow-up (the CI fix + the art lane's fail-safes) are pushed)
