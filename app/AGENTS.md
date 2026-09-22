# Curio App Module (Active Build) — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`. It follows the root `AGENTS.md` as its parent DOX rail.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `app/AGENTS.md` (this file)

Read `master.md` and root `AGENTS.md` first, then this file for app-module-specific contracts.

## Purpose

The `app/` module is the active Android application — **Curio**, a discovery app that hands the user a topic (via "The Spin" roulette) to explore in the real world, then captures what they found into "The Cabinet" library.

**Design direction comes from the user, not from historical documents.** Before making design decisions — colors, typography, shapes, motion, layout, empty states, copy, emoji-vs-icon policy — ask the user for direction. Do not invent or follow design rules from old prompts or code comments.

The **data layer** (category taxonomy, topic schema, `ExploreAction` prompt format, authoring pipeline, rollout cadence) is documented separately in [`CURIO_DATA_PLAN.md`](CURIO_DATA_PLAN.md). It expands the category palette from 6 → 10 and ships 150+ topics per category authored via LLM-draft + human-review. **Read it before any feature work that touches data or content.**

Curio is self-contained. Its **Material Symbols** variable font and **geom.ttf** display typography live directly under `app/src/main/res/font/`; no external legacy module or source tree is read at build time or runtime.

## Ownership

### Package layout (current, Phase 2 scaffold)

```
app/src/main/java/com/curio/app/
├── MainActivity.kt                 # single Activity, edge-to-edge + CurioTheme + NavHost
├── data/
│   └── Category.kt                 # CategoryId enum + CurioCategory data class + canonical 6
├── navigation/
│   ├── CurioRoutes.kt              # all route constants + builders + bottomNavRoutes set
│   └── CurioNavHost.kt             # Scaffold-wrapped NavHost with conditional bottom nav + adaptive rail/column
├── ui/
│   ├── adaptive/
│   │   └── CurioAdaptiveLayout.kt  # window-size-class helper + CurioContentMaxWidth (tablet/landscape contract)
│   ├── theme/                      # design system primitives
│   │   ├── CurioColors.kt          # Midnight Signal palette + 6 category accents + wildcard gradient
│   │   ├── CurioTypography.kt      # geom.ttf for display/headline/label; M3 default for body
│   │   ├── CurioShapes.kt          # 16/24/32/48 corner tokens
│   │   ├── CurioIcons.kt           # glyph constants + CurioIcon(name, ...) ligature renderer
│   │   └── CurioTheme.kt           # light/dark M3 color schemes + edge-to-edge SideEffect
│   └── components/                 # reusable building blocks
│       ├── CurioBottomNav.kt       # 3-tab nav chrome: floating pill bar (phones) + NavigationRail (wide), saveState/restoreState
│       ├── CurioCategoryChip.kt    # FilterChip per category + CurioWildcardChip
│       ├── CurioEmptyState.kt      # universal §13.7 empty-state skeleton
│       ├── CurioHeroCard.kt        # ~40% vertical hero Spin card on Home
│       └── CurioStreakPill.kt      # streak indicator pill + CurioSecondaryAction helper
└── features/
    ├── splash/SplashScreen.kt      # §13.1 splash — v224 SIMPLE/MODERN/MATERIAL redesign, v224b
    │                               # sizing+warm-up pass: 88dp BREATHING logomark (slow scale pulse —
    │                               # positional bobbing was rejected by the user), displaySmall Geom
    │                               # wordmark, a DETERMINATE 180dp LinearProgressIndicator wired to the
    │                               # real catalog warm-up (+1 per parsed lane, forced to 100% before handoff
    │                               # so topics are READY when the splash exits), and four rotating
    │                               # curiosity loading lines via AnimatedContent every ~1.1s. Warm-up logic
    │                               # unchanged (800ms min, ~6s cap); all colors are plain theme roles.
    ├── home/HomeScreen.kt          # §3 home — top bar, greeting, streak, hero, chips, recently explored empty state
    └── PlaceholderScreens.kt       # ONE file containing 11 stubs: Spin, Cabinet, CategoryPicker, TopicReveal, SaveCapture, EntryDetail, Settings, Onboarding, ManageCategories, TopicHistory, Lightbox. Each uses a shared `PlaceholderScaffold` with back arrow + glyph + title + subtitle + "Design phase · logic comes later". Real implementations replace these one-by-one in later phases.
```

### Resources

- `app/src/main/res/font/geom.ttf` — bundled display/headline typography
- `app/src/main/res/font/material_symbols_outlined.ttf` — bundled UI + category icon font
- `app/src/main/res/values/strings.xml` — Curio app name + screen titles + category display names
- `app/src/main/res/values/themes.xml` — `Theme.Curio` (M3 DayNight no-actionbar, Midnight Signal bootstrap surface)
- `app/src/main/res/values/colors.xml` — Midnight Signal XML resources used at the OS-level splash/background before Compose takes over
- `app/src/main/res/drawable-nodpi/ic_launcher_icon.png` — the v115 COSMIC launcher mark, rendered from the designer's NEW source SVG (`svgviewer-output (5).svg`, archived at `design/launcher-icon/curio-launcher-icon-v2.svg`) at 2048×2048: a mint planet with a pink moon over layered pink/gold waves on a midnight navy→magenta sky, inside a rounded card (the white frame of the v113 art is GONE, per the new source — same card geometry, ~84–88% of the canvas). `ic_launcher_foreground.xml` is an `<inset android:inset="28dp">` around this bitmap, so the whole card (~44×47dp) fits the launcher mask's safe zone; `ic_launcher_background.xml` is the full-bleed sky gradient + stars the card floats on; `ic_launcher_monochrome.xml` is the planet+moon silhouette (themed icons — positions unchanged: planet 380,320 r=122; moon 650,500 r=55); `ic_notification` is the same mark at 24dp. The SPLASH keeps rendering `@drawable/ic_launcher_art` (the v113 raster, still at `drawable-nodpi/ic_launcher_art.png`; archived at `design/launcher-icon/curio-launcher-icon.png`) — the user approved the splash as-is and asked for the icon only.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher{,_round}.xml` — adaptive-icon declarations referencing the colored and monochrome layers above
- `app/src/main/assets/topics/` — Curio topic data files and schema reference (one per ready category; see Content authoring below)

## Local Contracts

### Identity
- `namespace = "com.curio.app"` (new package, separate from FieldMind)
- `applicationId = "com.curio.app"` (new install, separate from FieldMind; users install Curio as a separate app)
- `minSdk = 26` (Android 8.0+ — all release APKs are labeled with this), `targetSdk = 37`, `compileSdk = 37`
- `versionName = "1.1.0"` (default; bumped to 1.1.0 in v113 for the cosmic-icon release — the release workflow overrides it with the git tag minus the leading `v`, e.g. tag `v1.2.3` → `1.2.3`), `versionCode = 20260922` (date-based, +1 over the previous 20260921; unchanged by tags)
- No product flavors; Curio builds as a single flavorless Android application
- Debug builds append `.debug` to `applicationId` → `com.curio.app.debug` so both can coexist on one device
- Bundles `material_symbols_outlined.ttf` + `geom.ttf` + `lora.ttf` (v35 — the Lora editorial serif, OFL, variable wght 400–700, ~212KB) directly in `app/src/main/res/font/`; none depend on another module or source tree

### Curio Database (separate from FieldMind)
- Curio installs as a separate app under `applicationId = "com.curio.app"` — its data directory is `/data/data/com.curio.app/databases/`; the Room database is `curio_database` (captures + topics + cached-topics tables).
- FieldMind's data lives in FieldMind's separate install at `/data/data/fieldmind.research.app/databases/fieldmind_database`. Curio CANNOT access it directly.
- FieldMind data can be imported into Curio through the self-contained FieldMind archive importer in `com.curio.app.data.FieldMindLegacyImport`, which accepts V3 `.fieldmind` packages and plain archive JSON.
- The two apps do not share DB names, schemas, or SharedPreferences namespaces — fully isolated.
- **Topic catalog (v348 — the bundled JSON IS the read path; Room is the mirror):** `TopicJsonLoader.load()` parses the bundled `assets/topics/*.json` (one parse per lane, deduped through the in-flight map and bounded by the parse gate) and caches the pools in memory. Room's `topics` table is populated from that JSON once (`TopicRepository.init`, re-synced on version/install-time change) but is now only a FALLBACK for a build whose assets carry no JSON — its read (row mapping + per-row Gson decode of chapters/tracks/episodes) was slower than the parse it replaced and was masking shipped content. `warmLoaderFromRoom` warms COUNTS only. `MainActivity` runs TWO independent warmups at startup: `TopicRepository.init()` (Room, own launch) and the loader prewarm `loadIndex()` + `preloadAll()` (NonCancellable — no longer queued behind the import, which had delayed the browser's warm index by a full JSON→Room pass on every launch that touched the DB). The browser (`TopicDatabaseScreen`) builds its rows through `buildIndexedTopics()`, which takes the search keys + sort year from the warm merged index when it holds the topic (no per-open lowercasing/year parsing — the v1.1.1-beta5 warm-seed behaviour, restored) and falls back to deriving them from the topic itself. **Reveal never blanks:** `TopicRevealScreen` resolves the loader first, then Room (`TopicRepository.findTopic` still hydrates stale rows from JSON on demand), and every resolved topic is upserted into `cached_topics` via `TopicRepository.rememberTopic`. **Cabinet warm start (v348):** `MainActivity` also collects `CaptureRepository.observeLight()` once at startup so `peekLight()` seeds the Cabinet on its very first open instead of showing a skeleton. **WILDCARD is a LANE, not a merge (v385):** `load(CategoryId.WILDCARD)` parses `wildcard.json` ALONE — the old merge parsed every lane behind the two-parse gate, which is what made the Wildcard deck sit on "Gathering the deck…" for seconds and pulled real-lane topics ("Bowie") into the surprise pool. The deck, `TopicRevealScreen`'s wildcard fallback, `TopicRepository.sampleTopics` (WILDCARD samples its OWN lane first, falling back to a few picks per lane only for a database mirrored before the change) and `countFor(WILDCARD)` (its own file's count, never the canonical total) all follow it. **Parse and mirror cost (v385):** `cleanText`'s four Regexes are compiled ONCE at file scope (they were built per call: ~16 compilations for every topic parsed), the Room mirror in `parseAndCache` SKIPS a lane whose Room row count already equals the parsed count (every restart used to rewrite the whole ~14k-row catalog on the same IO threads that were parsing), and `sampleTopics` is no longer gated on `isInitialized()` — it asks the moment the deck composes, which is exactly when the seed was skipped before. That seed now also clears Spin's `poolLoading`, so the loading card can only appear on a genuine first launch (nothing to sample yet), never over a restart with a full database. **Browse cost (v387):** `TopicDatabaseScreen` builds nothing on the composition thread. Browse rows read the loader's parsed lane lists DIRECTLY (browse needs neither `IndexedTopic`s nor an id map — the 16k index objects, the `associateBy` id map and the per-lane `groupBy` are SEARCH-ONLY, built in a background pass the moment the search field opens and dropped when it closes, and a row's order is its lane's own file order). The last browse row set is cached (`BrowseRowsCache`: lane signature + done-set size) so reopening the page — a reveal round-trip, a tab switch — paints the previous rows on the FIRST frame and the loading slot can never cover them; the background rebuild still runs and refreshes the cache. The scroll indicator's `activeAlphabetIndex` and the back-to-top flag are read inside a tiny `BoxScope.ScrollScopedRead` instead of the screen body, because a `derivedStateOf` read registers its recomposition WHERE IT IS READ and reading it in the body re-ran the whole page (every remembered derivation plus a fresh emit of the `LazyColumn`) on every row crossing. `browserLoading`'s index wait is search-only for the same reason: holding it during a browse would have hidden the catalog it no longer depends on. **The index no longer holds the topics (v389):** `TopicIndexEntry` used to carry the full `CurioTopic`, so a warm index pinned all ~16k topics — `shedForMemory(RUNNING_LOW)` cleared the per-lane pools while the index still referenced every topic and the trim freed almost nothing (the "background memory climbs sharply" symptom). The entry now carries IDENTITY (id + lane) plus the display text and the precomputed search keys, so dropping the pools is a real release, and a screen that needs the topic object itself resolves it from that lane's pool: `TopicJsonLoader.topicForEntry` (suspend, shared parse), `cachedTopicFor` (sync, resident only) and `topicById` — used by the composer's picker and kept draft and by the Share Hub's picked topic, all off the UI thread — while the reveal's SYNCHRONOUS `resolveRevealTopic` matches on the entry with `savedNameMatches` (the String form of the topic matchers, one implementation) and calls `warmLane` so a trimmed pool refills for the next frame instead of stalling composition. The startup prewarm (`loadIndex()` + `preloadAll()`) therefore STAYS — removing it (an earlier attempt on `v0/reduce-book-lookup-memory`) just moved the same parse onto the busy path of every screen and left Home's Topics stat, the reveal's first-frame resolve and `TopicCatalog.findByName` reading a cache nothing had filled. Two supporting changes: Home's Topics stat reads the warm count (`TopicJsonLoader.cachedCanonicalCount`) before summing resident pools, so a memory trim can no longer make it read 0, and a trim KEEPS the per-lane counts (a handful of Ints; re-counting ten files to reclaim them was pure loss).
- **Image fetch caching:** `MainActivity.onCreate` installs a shared Coil `ImageLoader` (Coil 2.7) with an explicit memory cache (22%) + disk cache (`cacheDir/curio_image_cache`, 3%) and `respectCacheHeaders(false)` — book covers and any network image download once and hit disk on later visits/restarts. SvgDecoder registered app-wide.
- **Book covers — bulk one-by-one fetch (v314):** Settings → Safety & support → "Book covers" row (`features/settings/BookCoverFetch.kt`, `BookCoverFetchRow` — an INLINE action row special-cased off `BookCoverFetch.ROUTE` in the hub's grid/search/two-pane renderers so it can never navigate) tap-fetches every unique cover URL (authored `imageUrl`, else the reveal's exact Open Library `-M.jpg` fallback — same URL = same cache key) sequentially into the shared disk cache, `memoryCachePolicy DISABLED` on the bulk pass, ~150ms politeness gap, live "Fetching 12 / 301…" counter + progress bar, completion "✓ N covers cached · M failed". Reveal posters then render instantly/offline.
- **Haptics:** satisfying haptics are localized per-screen (`val haptics = LocalHapticFeedback.current` hoisted in composition — never read inside click lambdas). Confirm on completions (save capture, share-card Save/Share, spin landing, quest complete); KeyboardTap on action buttons (Start exploring, Express yourself, opening Cabinet entries); TextHandleMove ticks on toggles (pin, reveal favourite, share-card Reset/Done). The wheel's escalating ratchet lives in SpinScreen.

### Personal writing (v387) — journals, books and their own store
- **`features/personal/` + `data/Personal*.kt` own the member's own writing.** A journal page, a book's chapter review and the book's own note are NOT captures: a capture is a reaction to a TOPIC, these have no topic at all, so they never touch `captures`, never borrow `CurioEntry`/`CaptureFormat`, never open the saved-entry detail view and never ride a `TopicShareCard`. They live in their OWN tables (`personal_notes`, `personal_books`, Room v15 via `MIGRATION_14_15`) behind `PersonalRepositoryHolder.repo` (installed in `MainActivity` beside `CurioRepositoryHolder`). `personal_notes.bookId == null` ⇔ a journal entry; `bookId` set ⇔ that book's review of `chapterIndex`.
- **The body is a BLOCK document (`PersonalDoc`), not a string.** A block is a paragraph (`text` + `PersonalRun` styling + `PersonalAlign`) or an attached PHOTO, so a picture stands in the writing as a block among blocks. (v398 tried the other reading — an attachment living INSIDE a paragraph, held as an object-replacement mark in that paragraph's own text — and the member took it back out: "remove the inline photo and keep what it was like before". An attachment is a block on its own line again, with its own carry, its own sizes and its own caption; the mark character, `PersonalBlock.inlineRefs` and the inline drawing pass are gone, which is why `chapterNoteText`/`chapterNoteSpans` can join blocks with a plain newline again.) `PersonalDocCodec` encodes it to `personal_notes.bodyJson` through nullable mirrors — Gson bypasses Kotlin constructor defaults, so a hand-written JSON contract is what keeps an older note readable by a newer build and vice versa. `preview` (the plain-text projection) is recomputed inside `PersonalRepository.saveNote`, so a stored preview can never disagree with the words.
- **Styling is a per-character BITMASK, and the tools are the LAST thing that touched it (`PersonalRuns.kt`).** Bold / italic / underline / strike / quote are bits in an `IntArray` the same length as the block's text — an edit is then a copy (head + freshly typed characters stamped with the style at the caret + tail) instead of range arithmetic, which is where rich-text editors grow bugs. `maskToRuns`/`runsToMask` are the only bridge to the stored shape. Tools apply to the SELECTION when there is one and to the whole line otherwise (user decision), and an empty line ARMS the tool so the first words typed carry it. Enter SPLITS the paragraph (a block ≈ a line, which is what makes alignment a line tool); Shift+Enter keeps a plain newline. Alignment lives on the block (`PersonalAlign`), rendered through the field's `textAlign`.
- **The three personal pages and a chapter review share `PersonalCanvas.kt`** — the canvas, the read-only `PersonalDocView`, and the `PersonalToolDock` that rides ABOVE the keyboard (the screen is an `imePadding()` column with the dock as its last child). The dock draws its own glyphs for B / I / U / S and alignment (styles + Canvas rules) rather than depending on the Material Symbols subset, which has no strikethrough or align icons; the quote and photo tools use `CurioIcons.FormatQuote` / `CurioIcons.Image`. **v389 — that dock is now the app's shared full-screen editing dock.** `RichTextEditor` gained `RichTextToolbarMode.DOCK`: instead of a head strip with a `Format` toggle that unfolds a second toolbar, the editor draws the JOURNAL's own dock (`RichTextDock` + `RichTextDockButton` — 22dp surface, 6dp lift, one scrolling row, every tool its own 36dp circular button, the active one filled with the accent at 24%) at the FOOT of the field, and the head strip appears only when it still has something of its own to say (paper tools, a trailing action, the text history). The IMAGE tool is deliberately absent — a rich-text field holds text. Underline rides along: the model has carried `TextSpan.underline` since v379 and the share card's selection bar could set it, but no toolbar ever offered it, so `applyUnderline()` / `hasUnderlineAt()` and the armed `pendingUnderline` were added with the same manners as the other flags (a caret arms, a selection is one-shot). Wired at exactly two call sites — the Share Hub's full-screen card editor (`TopicShareCard`) and the book sheet's chapter-note expand (`TopicRevealScreen`) — because every capture format keeps its compact strip (MAIN / TOGGLE). Both of those surfaces are now the JOURNAL'S PAGE too (the page background the journal writes on), so writing a note from a book and writing a journal page are one surface, and both pass `dockPinned = true`: in DOCK mode the editor then scrolls the WRITING inside itself (`Box(weight(1f).verticalScroll(...)) { fieldArea() }`) and keeps the dock BELOW it, while the call site gives the editor a WEIGHTED height instead of wrapping it in a scroll of its own — which is what holds the dock at the foot of the screen, the way the journal's own dock holds still. `dockPinned` is inert in every other mode, so the eight capture formats are untouched. **v406 — THE DOCK IS A POPUP ABOVE THE KEYBOARD, NOT A CHILD AT THE FIELD'S FOOT.** At the foot of the field it was the one place an IME can reach: a note low on the page put its tools under the keyboard the moment the member tapped in to write (member's report: "the tool that shows under the notes in save your take its hiding behind the keyboard when the note is too below … make it open as overlay which shows above keyboard not belo the note"). DOCK mode now composes a `Popup(alignment = BottomCenter, offset = IntOffset(0, -dockLift), PopupProperties(focusable = false))` where `dockLift = maxOf(WindowInsets.ime, WindowInsets.navigationBars)` bottom in px, the content capped at `560.dp` and centred, entering with a vertical slide. Never move it back inside the field, and never read the lift from anything but those two insets: the ime inset already contains the nav bar, so the max is the right amount and can never double count. `MainActivity` calls `enableEdgeToEdge()`, which is what keeps the ime inset real instead of consumed by `adjustResize`.
- **v406 — TWO EXPERIMENTS BECAME THE DEFAULT.** `AppPreferences.isCaptureStudioEnabled` defaults to `true` (it shipped opt-in, so a fresh install got a Save-your-take page without the studio the page is about), and the merged cover consent behind `isCoverFetchEnabled` now defaults to ON when NO choice has ever been stored (a stored legacy choice still wins, so anyone who turned the old per-kind switches off keeps them off). Both switches stay in Settings; a default is not a removal. **The dock's two new tools are model-level.** `TextSpan` gained `alignKey` ("start" / "center" / "end" / "justify") and `fontKey` ("default" / "book" / "writing" / "display"), both stored as KEYS rather than ordinals — readable JSON, and a re-ordered table can never silently re-point an old run — with `null` meaning inherit, which is what every legacy run carries (Gson leaves a missing field null, and the Kotlin default is null too). `buildRichAnnotated` renders the family as a span style and the ALIGNMENT as a **paragraph style** (the text stack aligns whole lines, so a run reaching into a line sets that line), which means the saved-entry views and the card export follow for free; `extractRichSpans` reads both back (family from the span styles, alignment from the paragraph styles) so spans survive the AnnotatedString round-trips; `merged()` and `rebaseSpans` carry them (rebase now COPIES instead of rebuilding positionally, so a shifted run keeps every attribute). `setSpanAlign` / `setSpanFont` use the same split-at-the-edges shape as the flag toggles, and both doors apply to the SELECTION, or to the paragraph under the caret when nothing is selected — so "centre this line" is one tap. The two glyphs are DRAWN (`RichAlignGlyph`, `RichFontGlyph`), as the journal's own dock draws its alignment marks, because the bundled icon subset has none.
- **The social avatars are COZY MINIMAL, and drawn from zero (v395).** `features/community/SocialAvatar.kt` now holds twenty PORTRAITS (ten men, ten women) followed by eight ICONS (crescent moon, sun, star, cloud, rainbow, mountain, cup, heart) — 28 styles, the count `SOCIAL_AVATAR_STYLE_COUNT` must equal. The old cast was the accumulation of four passes (construction → detail → illustration → pose) and each pass added shapes to the SAME drawing, which is exactly why it read as one bust in twenty-eight hats at avatar size (user request: "the current avatars are so bad, like genuinely so bad, no detail and all … fully redraw them from scratch … also add some icon style avatar, like moon icon, or yk emoji icon, kind of cozy minimal style avatars"). Nothing was carried over. The drawing is now: one WARM DISC (a radial gradient lit top-left, one finishing light), one BUST (one shoulder path with a collar, drawn once for the whole cast), one HEAD (an oval, two ears, a blush, one ink contour), a FACE from a small named vocabulary — five gazes (`FACE_OPEN` / `HAPPY` / `SLEEPY` / `WINK` / `CALM`) and four mouths (`MOUTH_SMILE` / `GRIN` / `SOFT` / `OPEN`) — and ONE SILHOUETTE per row (a `kind`, `KIND_*` 0–19, drawn in two phases: `drawHairBack` behind the head, `drawHairFront` over it, plus exactly one `drawProp`). Every mass is a filled path with one ink contour (`fill`), every line a round-capped stroke (`bar`), every shape a `dot` / `oval` / `ring` / `arc` / `slab` / `leaf` / `flower` / `star`, and everything is measured on ONE 100×100 grid (`u`, and `Path.scaled(u)` for the unit-scale point lists), so a portrait is the same portrait at 26dp in a bubble and at 96dp on a profile. The pose is two numbers per row: `tilt` (the head, about the base of the neck) and `lean` (the whole character, off the disc's centre).

  **v398 — the correction pass on that redraw.** Four things were wrong in the first drawing and each is now a rule: (1) the BUST carries NO ink contour and the collar sits from the shoulder line DOWN (`drawBust`) — the stroked shoulder path was a dark rule running across the disc a few units above the shoulders, and the collar band floated above the line it belonged to ("theres a line above shoulder"); (2) the tilts are all within four degrees (`PORTRAITS`) — eight degrees walks a head off its own collar and reads as a bent neck rather than a pose ("the poses are wierd"); (3) every gaze is a shape that means something (`drawFace`) — an open eye with its own catch-light, a closed smiling eye, a lidded eye whose lid is ON the eye, a wink and a looking-down pair, where the first pass drew straight bars ACROSS the eyes and flat ovals with a bar floating above them ("some avatars are weird looking with the eyes"); and the four mouths are four different shapes (a grin with its own lip line and corners, a short quiet arc, an open o with a highlight, a plain smile) instead of one arc at four sizes ("that smile in each of them"); (4) the LEAF tile is a RAINBOW (`ICON_RAINBOW`) — the leaf read badly and the member asked for a crescent moon, which is now what the moon tile IS: a real crescent built by subtracting one oval from another (`Path.op(..., PathOperation.Difference)`), because the first pass's 280° stroked arc was a broken ring that read as the letter C ("why the mon is C"). `drawSocialAvatar` is still the one entry point — the composable, the picker tile and `NotificationAvatars.of` (the notification wallpaper's off-screen bitmap) all draw through it, so there is never a lookalike.
- **A run of prints is ONE ROW, and a row is made by DROPPING a print against a print (v403).** The drawing pass groups CONSECUTIVE print blocks (`PRINT_ROW_LIMIT` 4) into a row and draws it once — two, the upright frame with two stacked beside it, or a square, with each CELL taking its own size's height and share (v400). Three rules keep a drop from fighting that pass, and all three live in `PersonalEditorState`: (1) **`printDropIndex`** snaps a carried print to the run it was dropped on (the run's first cell coming down, its last going up) instead of between its members, because the members after the first are SKIPPED by the drawing pass and have no measurement for the finger's step count to land on — that snap is what makes "drop a picture onto another one" actually build the pair, and a print already in (or already touching) the run is left exactly where it is; (2) **`normaliseRowSizesAt`** (called from `moveBlock` BEFORE its single `onDocChanged`, so a grouping drop is one change to undo) turns a PAGE print into HALF once it stands beside another print — PAGE is where a print ARRIVES and means the whole measure, which is the one thing a cell cannot be (user request: "the page style should auto adjust when im holding and trying to put two images together"); a print ALONE keeps the size it has and a size the member PICKED is never touched; (3) the landing hover is the block's own dashed room, drawn as a half-width CELL when a print is coming down on a print, with no colour line anywhere ("stop using lines for preview"); (4) **`insertAtCaret` does not KEEP the empty piece its split leaves** when a picture arrives directly under a picture (`keepHead` — the seam had no words of the member's and stood exactly where the row needed an edge, which is why two pictures added one after another never grouped and had to be dragged together; a piece WITH words, a picture added at the top of the page and every voice note keep the empty line they always got). The read view (`PersonalDocView`) runs the same grouping rule with PAGE included, so the two passes can never disagree about which prints are one row.
- **A page's subject does not scroll away, and a new page keeps its identity.** `PersonalWritingPage` gained `pinnedHead` — a slot UNDER the top bar and OUTSIDE the writing scroll (the journal's own `aboveCanvas` rides inside it, which is right for a field and wrong for the thing the page is about). The topic note puts its `TopicHead` there, so the "choose the topic" door can never sit under the fold (user report: "the choose a topic area gets hidden as it's not on top the page"). And the page's `entryId` — minted on first composition for a new page — is now `rememberSaveable`, with the topic note's own topic/category/date/picker state alongside it: navigating away (the topic page from the header, a settings trip) DISPOSES the composition, so a merely-`remember`ed id was re-minted on the way back, which saved a second row and forgot the topic that had just been picked (user report: "i tap the look the topic from the header and i press back the previous topic gets saved and it asks me again to choose a new").
- **"Select all" on a page means the PAGE, not the line.** A journal family page is MANY text fields (one per paragraph), so Android's own Select all could only ever reach the field the caret sat in — a member reported it as a bug ("when i do select all it only selects one line … its same for all journals book review chapter review and all"). `PersonalCanvas` now WRAPS the platform toolbar (`LocalTextToolbar`) with an implementation that keeps its look and every other action and re-points exactly two of them: Select all sets `PersonalEditorState.pageSelected`, and Copy copies the whole page (`pageText()`, clipboard) while that is on. Every row then wears the selection wash, the dock's tools apply to ALL rows (`toggle` / `setAlign` branch on `pageSelected`), and typing clears the mode. Nothing else about the platform toolbar changed, which is why cut / paste / the per-word menu still behave exactly as Android made them. **v417 — AND THE DOCK SHOWS IT.** `activeFlags()` read only the FOCUSED row, and `selectPage` parks the caret at the end of the page — so a page-wide selection lit the tools for one line while the whole page was selected (member: "with the gesture the select all works but it doesnt show the tool … i want similar select all for android select all too"). With `pageSelected` on it now answers for the PAGE: a flag is active when EVERY writing row carries it — the same rule `toggle` uses to decide what a page-wide tap means — so Select all (the gesture, the platform's own, and Ctrl+A) light the tools for the whole page in every journal, book review and note.
- **A checklist row is tickable while READING, and a to-do list previews as itself.** `PersonalDocView` takes the box's own tap (only the marker's lead square — a tap on the words stays a read) and asks `LocalPersonalCheckToggle`, which `PersonalWritingPage` provides from the store it already owns, so the tick saves through the page's normal debounce and no page had to thread a callback down (a view renders a document; it must never edit one). The to-do page reads its rows bigger (`rowSize`, see `ROW_VIEW_SIZE`), and `ChecklistPreview` (TodoScreen) is the journey list's own preview of a checklist — its rows with their ticks — instead of its text run into a paragraph where a finished row and an unfinished one read alike. The Home "+" sheet's to-do door wears that same drawn box (`TodoGlyph`) instead of the `task_alt` icon.
- **NOTHING IS EVER SAVED BY A BUTTON.** `JournalEditorScreen` writes the entry 700ms after the writer stops, again on `Lifecycle.Event.ON_STOP` (so an app switch cannot lose the page) and once more on the way out; `BookDetailScreen` does the same for the open chapter's review and for the book's own note, and writing about chapter N moves `currentChapter` to N (progress follows the writing). The screens' save indicator is a pulsing dot, never a sentence.
- **Where they show.** Home's shelf slot (which used to hold the Saved shelf — bookmarked quotes + pinned topics now live in Topic History) is `PersonalChipsRow`: fixed-geometry chips (96×118dp — they never grow) of the newest journals and books, each chip opening its own page, wearing the app's accent tokens rather than the capture paper's creams. **Each row's DOOR is pinned** (`PinnedDoorRow`): the Pages / My shelf chip rides an opaquely filled overlay at its row's LEFT EDGE — the fill is the page's own background, because the chips scroll BESIDE it and slide UNDER it and a translucent scrim would ghost them through the card — and `DOOR_GUTTER` reserves that strip so the first chip starts clear of it. The door itself wears the accent (an OPAQUE `lerp` wash of `surfaceContainer` → `personalAccent()`, an accent hairline, the accent's deep shade as ink, 6dp shadow), so the way into the full list never scrolls away and never reads as a third chip. A book chip's footer names the book in 10sp on ONE line. The floating **`+`** (`PersonalCreateLauncher`) opens `CreateEntrySheet` (a journal page / a book) and hides while the page is scrolled DOWN, returning when the finger goes up or the page reaches the top. Saved-capture rows are filtered out of Home's recents (`RecentFeedItem.SavedEntry`), and `JournalListScreen` / `BookShelfScreen` are the family's own collection views (a page per day; covers with the name and progress under them). The book shelf adds books through `openlibrary.org/search.json` (OkHttp, 8s timeouts) with a manual door always available, and `BookCover` draws a generated cover under the artwork so a shelf is never a row of empty frames. **v410 — and the artwork is FETCHED for a book that came in without one.** `BookCover` renders the art the app already holds (`CabinetCoverCache.localCoverFile` for the title, re-read on the cache's version) before the row's own `coverUrl`, and both the shelf and the book page resolve a missing cover through `BookCoverWarmup` (catalogue URL for a book Curio knows, else the verified provider cascade) and write it onto the book's row — see the v410 section at the foot of this file. **The Cabinet's PERSONAL shelf is the collection's door** (`SHELF_LEVEL_PERSONAL`, `CabinetPersonalShelf.kt`): it maps `V2ShelfId.PERSONAL` to a level of its own that lists the journals and the books in those same small views (each opening its own page), and the shelf card's count is journals + books. Its saved collection members are NOT lost — a `Saved in Personal` door at the foot opens the collection's own grid, and the Cabinet hero's search filters the writing like any other shelf.
- **Take studio (v387):** the rail is a rail again — one pill per take, the active one filled with its own remove cross, and an **Add take** door at the END that opens the same format picker; `CaptureStudio` gained `onSelectTake` (the caller snapshots the outgoing take first, like every other take switch). The picker is still the only thing that decides what a take IS.

### Voice notes in a page, and the page's own manners (v389; LOOKS added v421)
- **v421 — A NOTE HAS LOOKS (`PersonalVoiceStyle`), stored PER RECORDING.** **v423 — SIX entries**: `HAND` (the drawn pulse, a DRAWN play mark, the clock in Fraunces — the default), `BARS` (columns with a filled disc), `BUBBLE` (MIRRORED BARS inside a tinted bubble whose foot corner is cut, with a disc — the voice-message look; the hand-drawn line v421 put inside the bubble was "not that good"), `MINIMAL` (one flat line and a dot with an outlined mark), `RIBBON` (the envelope and its own mirror as ONE closed shape, with a disc) and `BEADS` (a dot per moment on the centre line, each as big as the sound it stands for, with a disc). **v423 — AND THE PROGRESS RIDES THE DRAWING.** The head of the heard run was a straight bar across the band, which is not part of a hand-drawn line and read as a knob bolted onto it (it also sat at the end of the live recorder's strip, which is what made that meter look wrong); it is a BEAD of the wave's own ink, placed on the wave point the head has reached, and it is drawn ONLY while `progress < 0.995f` — a note played to its end and a recording in progress wear no knob at all. The heard run is the same drawing cut at the head in every look (`clipRect(right = played)`), never a bar filling up beside it. **The dispatch is `style != HAND` → `drawVoiceWave`, HAND → `drawVoicePulse`** (in the note's own Canvas AND in `VoiceStylePreview`) — a new look draws itself there and takes `else` for its control, so no `when` has to be exhaustive to compile. A look pairs a WAVE DRAWING with a PLAY TREATMENT on purpose — the member asked for both ("the voice note looks adds differnt waves and button styles"), and a hand-drawn wave beside a Material disc was the exact mismatch they reported. `PersonalBlock.audioStyle` carries the key, codec key `ast`, OMITTED at the default so an older note encodes byte-for-byte as it did and an unknown key reads as `HAND`. The picker is a control ON the note (the Tune mark beside the ✕) that opens `PersonalVoiceStyleSheet`, and it exists ONLY in the editor — a read-only view renders the document, it does not edit it. The two drawing passes live in this file: `drawVoicePulse` and `drawVoiceWave`.
- **THE DRAWING MEASURES FROM A BAND, AND THAT IS THE CLIPPING FIX.** Everything is laid out inside a rectangle inset by HALF A STROKE (and by the depth pass's own drop at the foot), so a peak can never touch the canvas edge. Before v421 the strip was measured from the canvas itself — the centre was `height / 2`, a peak could reach within 2% of the top and the round-capped stroke was drawn on that point, so a loud passage ran off the top and the first point at x = 0 had its cap sliced by the left edge (member: "if the waves are too big it cut from the top, also fromt the start it looks cut"). **Never draw a wave from the raw canvas edges again**, and keep the playhead inside the band too.
- **THE LIVE METER IS THE SAME DRAWING (`LiveVoiceWave`).** The recording capsule used the bar-chart meter the member had already retired on the note itself, so the capsule was a preview of a different widget than the note it was making. It now rolls the same history through `drawVoicePulse`, wears a 22dp card instead of a 50%-radius pill, and sets its clock in Fraunces.
- **A page can be TALKED into: `PersonalVoice.kt`.** The writing page carries a floating mic of its own — and it lives in the page's WRITING box, not the dock's: bottom-right of the writing area, 18/16dp clear of the tool row, appearing with a fade + scale. That is not decoration, it is the bug fix: a button placed on the dock's top edge (`.offset(y = -26.dp)`) hangs OUTSIDE its parent's bounds, and Compose never hit-tests what lies outside a parent, so only a sliver of the disc was tappable and the control read as dead. Tapping it runs `rememberRecordPermission` — the ONE door to `RECORD_AUDIO`, which launches Android's own request and starts recording from the GRANTED callback; the earlier version read the permission and returned a Boolean, and the caller's `if (!ask()) start()` both launched the dialog AND built a MediaRecorder with no permission, so the very first tap failed silently (`Could not start recording` while the dialog was still up) — which is why the mic looked dead. A refusal that Android will not ask about again puts the same dialog up with a button to Android's own page for Curio. It also wears `personalAccentInk()` as its fill, not the airy accent: a pale disc on a pale page reads as disabled. Tapping it starts a recording, the tool dock steps aside and a RECORDING CAPSULE takes the page's bottom — pulsing dot, the live meter (`LiveWaveform` fed by the recorder's real `maxAmplitude`), the clock, pause, ✗ to throw it away and ✓ to keep it. Keeping it lands a voice BLOCK in the page exactly like a photo does (`PersonalEditorState.insertVoice` → `insertAtCaret`, the photo's own path) and leaves a fresh empty line under it, so a page you talked into is still writable (user request: "below we can still add notes"). `PersonalBlock.audio` / `audioSeconds` / `audioBars` carry it (codec keys `aud` / `aus` / `aub`, all omitted on every other block kind, so older pages decode and encode exactly as before).
- **The waveform is STORED, not decoded (`PersonalAudioBars`).** Bars are extracted ONCE from the finished file (`WaveformExtractor`) and kept as one hex byte per bar (72 bars = 144 characters in the document), because decoding AAC through MediaCodec while a page scrolls is a stutter. The block draws them with the played part in accent, a playhead line, and TAP OR DRAG ANYWHERE on the strip to seek — the whole waveform is the scrubber (user request: "easy to pick a certain time stamp"). The read-only views (`PersonalDocView`, so a journal page, a chapter review and a book's note alike) draw the same bar.
- **A recording OUTLIVES its screen (`PersonalVoiceRecording`).** It is a singleton, not page state: the mic's session, the clock and the note it belongs to live there, and leaving the page is not allowed to decide anything on its own — the core's `BackHandler` and the page's own back button both run a guard that asks ("Still recording": keep recording / keep the note / discard), which is why `PersonalWritingPage`'s `header` now gets a fourth parameter, its GUARDED `onBack`, and each screen passes `onExit` instead of popping directly. A kept-alive recording is announced by `PersonalVoicePill` at the app's ROOT (next to the floating pet), which hides itself while its own page is composed (`PersonalVoiceRecording.onScreenNoteId`) and taps through to `session.returnRoute`.
- **The page's own manners, fixed with it:** the "Write…" hint belongs to a PRISTINE page (first block only — after Enter two empty paragraphs read as two placeholders), a tap anywhere on the blank part of the page (below the last line, above the tools) hands the caret to the LAST line (`PersonalEditorState.focusLastLine`, both on the canvas and on the writing column), and a page's photo is decoded FOR ITS BOX (`PersonalPagePhoto`: `Scale.FILL` at the composable's measured pixel size + `FilterQuality.High`) instead of Coil's FIT decode being scaled back up by `ContentScale.Crop`, which is what made the in-page preview blurry.
- **A LINE'S TOOLS BELONG TO THE LINE — including across Enter.** `PersonalEditorState.splitAtCaret` carries the line's OWN flags (`lineFlags` — every visible character's mask ANDed) into the new line, and an ARMED tool where the line is still empty; before this, Enter gave the new line to plain prose and the dock's button went dark, which read as the editor dropping the tool mid-sentence. The to-do page keeps its manner: Enter on an EMPTY row ends the list. The other half is `mergeWithPrevious`: BACKSPACE at the start of a line (or on an empty one) takes it back into the line above — words join, masks concatenate, the caret lands where the Enter was pressed — and it is wired in `PersonalTextBlock`'s `onPreviewKeyEvent`, which reads the live text and selection there rather than captured values because it runs between two compositions. Enter used to be one-way, so a stray keypress left a paragraph that could never be removed.
- **The marker tool is a ONE-TAP tool with a menu behind it.** A tap gives the focused line the FIRST `PersonalMarker` (the dot); the tap AFTER that — with the line already bulleted — opens the menu of styles, and the row that takes a list off a line reads **Remove list**. Bullet marks are `personalBulletColor()` (deep coffee, milky on a dark theme) and never the theme accent: a rose dot on a rose theme looked like a selection. The to-do tool draws `TodoGlyph` — the page's own rounded box and tick, filled when the line is already a row — instead of the bundled struck-through task icon.
- **A QUOTE IS A PANEL, AND A RUN OF QUOTES IS ONE PANEL.** A quoted line is drawn with `personalQuoteWash()` (a whisper of the coffee) under `personalQuoteRule()`, and it reaches into the gap beside it (`QUOTE_JOIN_EDITOR` / `QUOTE_JOIN_VIEW` are half the gap: 6dp in the editor, 8dp in the read-only views) wherever its neighbour is also a quoted line — so a quotation typed over several Enters reads as one continuous block instead of a stack of bars. `PersonalDocView` computes those neighbours once per render (`isQuoteRun`), and both list markers and quote furniture come from ONE renderer each, so the editor and every read-only view cannot disagree. The social PULL-QUOTE (`SocialPullQuote`, the composer's preview AND the wall post — one composable) no longer takes an accent: its rule, mark, dash and credit are `personalQuoteDeepColor()`, theme-aware deep coffee, because a quotation in the member's own accent read as a highlight somebody had selected.
- **The personal family's heads wear the DATE.** The journals list and the book shelf lost their `Write today` / `Add a book` pills — the floating `+` (and the empty state's own action) already open those doors — and `PersonalHeaderDate` (today, from `System.currentTimeMillis`) sits where the pill was. A journal ROW's title is capped at two lines, and the journal read view no longer repeats the date it is showing in the bar above.

### The family's heads roll up, and one switch for every page (v389)
- **The book page stops saying its title twice.** `PersonalHeader` gained `titleRevealed` (default true, so the journals list and the shelf are untouched): the head's title is a ROLL-UP that stays out of the way until the page's own title has gone under it, driven by the page's `LazyListState` (`firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 26.dp`) — the ONE reliable signal, because a child's `onGloballyPositioned` is re-reported on LAYOUT and a scroll is not a layout of that child. The line is always laid out (only its ink moves: `graphicsLayer { alpha; translationY }`), so nothing below the head can jump when the title arrives. **v421 — AND THE IDLE LINE IS ITS OWN LAYOUT.** `idleTitle` (the book page's "Your shelf") used to be the rolled line's exact slot: the same `headlineSmall`, with the subtitle's line left in place but transparent to hold the height — so the words sat ABOVE the head's centre and read as a small grey ghost of the title they stand in for (member: "the your shelf text isnt properly bigger and not properly aligned"). The idle state is now ONE line at `headlineMedium`, centred inside a box whose height is EXACTLY the rolled two-line slot (32sp + 16sp resolved in dp through `LocalDensity`, so a larger system font scale grows the box with the words instead of clipping them). The head therefore keeps one height and the idle words line up with the back pill. Keep those two facts — one height, centred — if the slot's type ever changes.
- **Roll-up titles where the scroll is the page's own.** `PersonalWritingPage` gained `onScroll: ((Int) -> Unit)?` (a `snapshotFlow` over its writing column's scroll, reported rather than handed out), and the journal's top bar takes the day's TITLE once the title field's own place in the CONTENT (`boundsInParent`, which does not change as the page scrolls) minus that offset has passed the top (`titleTop + titleHeight - scrollY <= 0`). It fades into the bar's empty middle with `Modifier.weight(1f, fill = false)`, so the bar never changes height and never pushes a line of the page.
- **THE PINNED CHAPTER (the book review).** A review's chapter markers are TITLE lines, so the page can say where it is by itself: both doc views report every title block's place in the scrolling content (`onTitlePosition` — `PersonalCanvas` for the writing side, `PersonalDocView` for the reading side, both `boundsInParent().top`), `BookReviewScreen` subtracts the ACTIVE side's scroll offset, and the marker whose line has gone above the top is the current chapter — so while a marker is still on screen the bar names the chapter before it. The bar is a floating pill at the page's top edge with an `AnimatedContent` label (a change of chapter reads as a change), and it does not exist until a marker has been scrolled past.
- **THE SOURCE-FETCH LAB LIVES ON THE DEV PAGE (`features/settings/SourceFetchLab.kt`, v427).** The member asked for "a api test fetching in dev settings" and, asked which sources, "Every content source"; asked where it should live, *A section on the Dev page* — so it is an always-visible section of `ExperimentsScreen` (FIRST on the page, because it is the one section there that answers a question instead of changing the app), with no switch of its own: the Dev page is already the gated surface. Three rulings hold it:
  - **IT ASKS THE ENDPOINTS, NOT THE FETCHERS.** The app's own doors (`AlbumArtFetch`, `MangaFetch`, `BookEnrichment`, …) return PARSED types and throw the payload away, so a lab built on them could say only "it worked"; each entry here builds the SAME URL the app's own file builds — which is why every `SourceFetchSource` names its `where` (the file whose URL it copies) — and shows the status, the time, the bytes, a one-line reading of the payload (`describePayload`: counts, array shapes, the first result's own fields, or the source's own `errors[]`) and the body itself, folded under it (the member's own choice: *"Both, on one screen"*). **A URL that drifts from its file is the bug this section exists to make findable — check the `where` first when a probe disagrees with the app.**
  - **A KEYED DOOR SAYS SO INSTEAD OF FAILING.** `keyName` names the `BuildConfig` value the app itself would use (`TMDB_API_KEY`, `COMIC_VINE_API_KEY`, `LIBRARY_THING_API_KEY`, `GOOGLE_BOOKS_API_KEY` — optional there, so no `keyName` — `SPOTIFY_CLIENT_ID`/`_SECRET`); `keyPresent` gates the button and the row says "Needs X — not set in this build", because a 401 from a keyless build is a fact about the BUILD, not about the source. Spotify's probe runs the same client-credentials token step `ExploreSearch` runs. **v428 — A DOOR MAY ACCEPT MORE THAN ONE CREDENTIAL.** TMDB takes either its v3 key or its API Read Access Token, so `keyOf("TMDB_API_KEY")` returns whichever one `TmdbFetch` would actually send, the row's `keyLabel` names both fields in the not-set message, and the probe authenticates the way `TmdbFetch` does (the token as `Authorization: Bearer …`, the key as `?api_key=`) rather than always as a query parameter — a lab that read only the key told a token-only build the source was unusable while the film sheets in it were resolving posters fine.
  - **ONE FIXED QUERY PER SOURCE, AND PACED.** The queries are what each service is known to answer (the member's choice: *"Fixed test queries only"*), and **Run all** fires them SEQUENTIALLY with a 400 ms pause — MusicBrainz asks for one request a second, and a lab that gets its own key rate-limited is a lab that lied about the source. Nothing is persisted and nothing fires on its own; the results are the section's own composition state and are gone when the page is left. Twenty sources, grouped Artwork / Music / Books / Screen / Anime & manga, each one added as a new `SourceFetchSource` entry — never as a second fetch path.
- **THE v413 HIDDEN WRITING GESTURES WERE REMOVED (v427).** Fifteen gesture-only writing tools, each with its own switch on the Dev page — gone, on the member's verdict ("not good, we will do it better way later"), and gone WHOLE: the catalogue (`JournalGestureTool` / `JournalGesture`), the multi-finger recogniser (`PersonalGestures.kt` — the file itself), its attachment to `PersonalCanvas`, the `combinedClickable` that used to ride the page's blank space (which is the plain `.clickable { editor.focusLastLine() }` it was before), the page's own undo RING (`UndoDepth` / `UndoIntervalMs` in `PersonalPage.kt` — it existed for the undo gesture alone, so nothing else ever popped it), and `AppPreferences`' `journalGestureToolsState` / `KEY_JOURNAL_GESTURES` / `journalGestureTools` / `setJournalGestureToolEnabled`. A future version starts from a clean slate: a hidden tool is only worth hiding if the button it replaces is worse, and these were fifteen at once with no way to see which were being used. **What STAYS, because it was never the gestures' own:** `PersonalEditorState`'s public API (`restoreRemovedRow`, `setAlign`, `moveBlock`, `cycleListStyle`, `selectPage` / `clearPageSelection`, `insertTitleLine`, `insertPlainText`, `applyMarker`, `applyFont`, `removeRow`) — the dock, the to-do page and the page-selection bar use every one of them — and `removeRow` is still the door that hands a removed line to the floating Undo pill. The page's undo is NOT missed: the page bar's own Undo (see the export/page-bar entry above) keeps its own history of what the bar itself did.
- **A double tap on the reading side hands the pen back** (`PersonalWritingPage`'s read branch, `BookReviewScreen`, `ChapterScreen` — all three put the detector UNDER the view, so a child that consumes its own tap keeps it). **The page's blank space always starts the writing**: `focusLastLine`'s `focusedId`/`caret` pair cannot say WHEN (the caret is consumed the moment the line honours it), so a tap on the blank part of a page whose caret was already in that line changed nothing; `PersonalEditorState.tapTarget` / `tapTick` are the proof that a finger landed, and the line it names takes focus and the keyboard (`LocalSoftwareKeyboardController.show`) whether or not it already had them.
- **ONE SWITCH, A MOVE.** `PersonalModeSwitch` draws a single lit fill that TRAVELS between its two halves (`animateFloatAsState` + `offset`) while both glyphs tint through the same slide, instead of two buttons swapping their backgrounds. The page's own mode swap is a cross-fade with a few dp of upward travel on both sides (no horizontal slide, no size transform), because the keyboard is what actually moves: the swap now agrees with the inset rising under it instead of sliding against it. The book page's writing-only rows (look-up, download, the blurb) and its read-only margins FOLD with the switch rather than appearing. `BookReviewScreen` and `ChapterScreen` also wear the switch now (their "Write/Edit/Done" pills are gone — leaving the pen still saves first).
- **HOLD THE READ PILL for the FILE.** `BookReadPill` is a `combinedClickable`: the tap reads (or asks for a file), the hold opens the page's file menu — the book's own document state plus "Choose a PDF" / "Choose an EPUB", both feeding the same `documentPicker` the first-time flow uses (`BookFiles.import` still copies into the app's storage).

### Chapter pages, the shelf bridge & the shade's doors (v389)
- **A chapter is a PAGE (`ChapterScreen`, route `books/{bookId}/chapter/{chapter}`).** Writing a chapter review used to grow a canvas INSIDE the shelf's chapter list. The row now carries the review's preview inline and opens the chapter's own page — the journal's shape on purpose: what the app knows about the chapter (its catalog name, pages, summary) then the review, READ first (`PersonalDocView`), with the writing behind one tap. No mood row (a book has no mood) and no format dock until the member writes; the dock slides in from the keyboard while the read chrome steps out (`Crossfade` body + `slideInVertically` dock). Auto-save is the journal's contract (700ms debounce, `ON_STOP` flush, flush on the way out) through `PersonalRepository.saveChapterNote`.
- **One book, two screens, ONE store (user decision).** A book added from Curio's own lane carries its topic id (`personal_books.catalogId`), and `PersonalRepository.bookForCatalog` is the bridge: the topic page's Book Notes sheet (`TopicRevealScreen`) reads and writes that book's chapter notes as `personal_notes` rows — the SAME rows the shelf's chapter page shows — so a note taken while reading the topic IS that chapter's review, and a review written on the shelf shows up in the sheet. `ChapterNoteBridge.kt` is the only converter (text ↔ blocks; bold/italic/underline the same on both sides; the note's HIGHLIGHT carried as the canvas' QUOTE; per-letter size dropped, since the canvas has no such idea). A book the catalog does not have keeps its notes in `AppPreferences.bookChapterNotes` exactly as before, and the sheet's writes are debounced (400ms) so a keystroke never writes a row. The sheet's own `chapterNotes`/`chapterNoteSpans` locals shadow the converters, so those are imported ALIASED (`docToText` / `docToSpans`).
- **Auto-fetch (`BookEnrichment`).** Opening a book fills in what it is missing without asking anyone: the catalog first (`BookCatalog.bestMatch` — an EXACT, punctuation-insensitive title match with the author as the tie-breaker, never a fuzzy guess, because a wrong binding would file the member's reviews under a different book), then **ONE Open Library visit** (`openLibraryPass`, v410) that answers all three questions at once — the median page count the search reports, the work's description, and the table of contents (the fullest edition's, else the work's) — from a single title search plus a single work read. It used to be THREE separate visits (a search per question, plus a second work read) on the critical path of every first open, which is the "the look up is slow" the member reported; and a book the CATALOG has (`catalogId` + a chapter list in the topic JSON) now pays no chapter request at all, because Curio is already holding that list. The catalog's synopsis is shown as "About this book"; a non-catalog book uses the Open Library description instead, labelled as such. `enrich` now returns an `EnrichReport` (what it learned + whether consent is needed) so the book page can say it out loud instead of silently doing nothing. The "Download help" pill opens a format chooser (PDF / EPUB) and searches the web with the file-type the member picked — a "hidden extension" syntax the app writes for them.
- **`PersonalPhotoOverlay.kt` — the page's own image viewer.** A photo used to leave the page for a Lightbox ROUTE. Now the overlay grows the picture out of the spot it was tapped in (the overlay is hosted by the nav route, the screen passes its `PersonalPhotoOverlayState` down through `onOpenPhoto` lambdas), morphing from the thumbnail's bounds to the fitted destination with a spring, then settling into pinch-zoom and pan; back or tap closes it with the reverse move. The canvas thumbnails are deliberately SMALL (172dp editor / 156dp read, `FilterQuality.High`, `ContentScale.Crop`) so a photo in a page is a photo in a page, not a gallery. `CurioIcon` gained `includeFontPadding = false` and a clamped ink-centre (no more bottom clipping inside pills).
- **`WritingFontFamily` exists for ONE reason.** `LoraFontFamily` declares four entries over one variable file, so a Bold request matches an "exact" descriptor whose glyphs are the regular face — bold silently rendered as regular in the canvas while italic (never declared, therefore synthesised) worked. The writing surfaces use the single-entry `WritingFontFamily`, which is what lets fake-bold/oblique kick in (same doctrine as `PatrickHandFontFamily`). A quote is no longer a highlight WASH: quoted runs are a touch smaller in the quote ink, and a whole-quoted block gets the accent RULE down its side (`personalQuoteRule`) in the editor and in `PersonalDocView` alike. The canvas shows its "Write…" hint only while the whole page is blank (every empty paragraph used to say "write").
- **The personal family wears the THEME's accent.** `PersonalTheme.kt` is the single source (`personalAccent()` = the settings hero accent, plus `personalOnAccent()` and `personalIconTint()` for a glyph on an accent wash, which needs the deeper ink in light mode). The pages used `colorScheme.primary` — a fixed rose whatever hero the member picked. Every personal surface (journals, shelf, book page, chapter page, the `+` sheet, the tool dock) pads the status bar now — they were the only new surfaces that reached under it — and the tool dock is 36dp a button in a horizontally scrollable row so nine tools can never be cut off on a narrow phone.
- **The "+" sheet has four doors:** a journal page, a book, a note on a topic, and a to-do list. The last two are their OWN screens (v389) — a note on a topic is not a day, so it no longer opens asking how the DAY felt, and a checklist is not a day either:
  - **One editor core, three page HEADS.** `PersonalWritingPage` (in `PersonalPage.kt`) owns everything that must not drift between pages — the entry id, the document, the debounced save, the `ON_STOP` flush, the photo picker, the read/write crossfade and the dock — and each screen passes its own `header`, `aboveCanvas` and `readView`. `JournalEditorScreen` (a date bar, a mood pill, `journal/{entryId}`), `TopicNoteScreen` (`notes/topic/{entryId}` — the topic as a card at the head, with a way through to its reveal page) and `TodoScreen` (`todo/{entryId}` — a name, a progress line, rows) are therefore heads plus a body, and the journal editor no longer takes topic or `kind` route params at all (`journalEditor(entryId)` is the whole builder). New pages still store the day they were started in `dateMillis`, because the journals list groups pages by date (a page with no date lands in January 1970).
  - **ONE router for the family: `personalRouteFor(note)`** (in `PersonalPage.kt`) — the journal list, Home's chips, the Cabinet's Personal shelf and the note page's own head all ask it which screen a row is, so a checklist can never open in the journal editor. The note page's topic is picked ON the page (a bottom sheet over `TopicJsonLoader.loadIndex()`, catalog results plus a typed name for anything Curio does not carry) and stored as the row's `topicId` / `topicName` / `categoryId`.
  - **A tick and a marker are STORED with the block (v389).** `PersonalBlock.checked` and `PersonalBlock.marker` go through `PersonalDocCodec` (`"ck"` / `"mk"`, both omitted at their defaults, so every older page encodes byte-for-byte as it did). The tick used to live in the row's own widget state: a reopened checklist had forgotten everything the member finished. A checklist page also keeps being one — `PersonalWritingPage`'s `checklistFirst` sets `PersonalEditorState.keepsChecklistRows`, so Enter at the end of a row starts the next row and Enter on an empty row ends the list (the canvas' rule that an ARMED tool never crosses a break stays in force everywhere else). The bullet tool is now a MARKER MENU (`PersonalMarker`: dot, ring, dash, star, spark, crystal, arrow, leaf, heart, bolt) whose previews and page glyphs are the SAME renderers (`drawPersonalMarker` / `drawPersonalCheckbox`), centred on the line's own height with one shared lead, and an unchecked box is a neutral ink hairline rather than the pale accent (a light-mode checklist no longer reads as a purple smudge).
  - **A to-do ROW answers two gestures of its own (`PersonalTodoRow.kt`).** A list is a running ORDER, so on a checklist page (`state.keepsChecklistRows` — the to-do page only, never a checklist inside a journal day) every row can be LONG-PRESS-DRAGGED to a new place and SWIPED SIDEWAYS off the list. Both come from `PersonalRowDragState`, hoisted ONCE in `PersonalCanvas` so the carried row and the rows it passes share one gesture: the finger's pixel travel lands in `graphicsLayer` (a deferred state read — following a finger never recomposes a text field), and only crossing a slot changes `steps`, which is what the neighbours spring on. The rows that make room use `snap()` the moment the drag ends, because the drop COMMITS `moveBlock(from, to)` in that same frame — the order IS the stored document, so the natural position already moved by exactly the distance the shift was holding and an animated shift would move it twice. The canvas' blocks are also `key(id)`-wrapped now; without that, a reordered list would hand each slot's remembered state to whatever slid into it. THE STEP COST IS THE HEIGHT OF THE ROW BEING PASSED: `dragBy` walks the list and charges each crossing the neighbour's own MEASURED height (`drag.measure` from every row's layout pass, keyed by row id) — never less than the carried row's own slot. One uniform `stride` for the whole list is what made a reorder of WRAPPED (two-line) rows land a place early or late. And the two detectors no longer share a gesture: the long-press drag claims it and the horizontal swipe stands down while `drag.isDragging`, because both used to fire on ONE finger — a reorder with a sideways wobble in it also dragged the row off the list (user report: "the todo rearrange works but also sometimes buggy"). A swipe past `SWIPE_AWAY_FRACTION` calls `removeRow`, which HOLDS the whole `PersonalRemovedRow` (words, style, tick, marker and place) for `PersonalUndoPill` — a floating "Row removed · Undo" chip inside the writing area that self-dismisses after 5 s (`PersonalWritingPage`), so the swipe can never be the one irreversible action; undo also drops the blank placeholder the page had to leave behind when the list would otherwise be empty.
  - **A print's caption is its own LABEL (v401).** `PersonalBlock` carries four fields beside `caption` — `captionDateMillis` (local midnight of the day the label stands for, 0 = none), `captionFace`, `captionSize` and `captionOrder` (codec keys `cdt` / `cfc` / `csz` / `cor`, each omitted at its default so every older page encodes exactly as it did). A date is stored as a DATE and not as text because the ORDER has to stay re-writable: `PersonalCaptionLabel.kt` owns the vocabulary — `PersonalCaptionFace` (the app's own bundled faces; `""` is the print's own, which is how every caption read before this), `PersonalCaptionLabelSize` (a multiplier on whatever the frame gives the label; `""` = standard), `PersonalCaptionDateOrder` (day first / month first / year first) and `PersonalCaptionDates`, which keeps the APP-WIDE order in snapshot state (`curio_personal_writing` prefs, read from composition, so one change re-writes every label that did not override it) — a caption's own `captionOrder` wins where it is set. The date is drawn on its own line UNDER the caption, never as words inside it, and the writing view and the read view ask the same formatter. The dock follows the caret: `PersonalEditorState.captionFocusedId` is set by the caption field's own `onFocusChanged`, and `PersonalToolDock` CROSSFADES to `PersonalCaptionTools` (the date, the face, the size, and one tap back to the writing tools) because bold, the marker, the bullet list and the alignment tools can do nothing to a caption. Never draw a caption with a hardcoded family or size again — ask `personalCaptionFace` / `personalCaptionSizeSp`.
  - **One topic search for every chooser: `data/TopicSearch.kt`.** The ranking the community composer grew (name prefix → name word → name substring → byline/subtype → tag → teaser, shortest name first) is `searchTopicIndex` now, asked by the composer, the Share Hub's picker (which used to sort a substring filter alphabetically, so a weak hit could outrank a name match) and the note page.
- **"Curiying now" counts the member's OWN shelf books, not just its saved members.** The shelf's number came from `seededById["shelf:currently-reading"].members.size` alone, so a book ADDED to the shelf and part-way through was invisible in the count and in the grid (the reported wrong number). `readingBooks` (shelf books not finished) is added to the count by TITLE against the saved members — so a hearted book that is also on the shelf is never counted twice — and `v2ReadingNowItems` (CabinetPersonalShelf.kt) emits those books as a "Your books" section inside that shelf level. It is ADDITIVE on purpose: the collection's saved members keep their grid above and their door below.
- **A book page is READ-FIRST, and the whole book has ONE page.** `BookDetailScreen` carries the family's eye/pen switch (`PersonalModeSwitch` in its `PersonalHeader` `action` slot): a book with writing opens READING (the look-up and download pills and the blurb field are the WRITING side and stay out of the way), a book with nothing written opens with the pen down. The seed KEEPS FOLLOWING the shelf (`notes` + `book`) until the member touches the switch (`modeTouched`) — deliberately not a once-only seed on the first `notes` emission: `notes` is a FLOW that emits an empty list first, so seeding there read "nothing written" before anything had been read and a book WITH writing opened as the book view (user report). It also asks for WRITING, not for a row (`notes.none { !it.doc.isEmpty }`): a review opened and left empty is nothing to read back, so the book opens with the pen down exactly like a journal page — "similar to journal opening in view, do the same for saved books too". While writing, "About this book" is `SynopsisCard(collapsed = true)` — a folded line that opens on tap, never hidden. `BookReviewScreen` (`books/{bookId}/review`, `CurioRoutes.BOOK_REVIEW`) is the BOOK's own review: a `personal_notes` row with `chapterIndex = null` written through `PersonalRepositoryHolder.repo.saveBookReview` (a column-aware writer that deliberately does NOT move the reading progress — a whole-book review is not a chapter you finished), with autosave + `ON_STOP` + the leaving flush the chapter page was missing. While writing it offers ONE floating door the dock cannot: **Add chapter** → a menu of the book's OWN chapter list (`chapterNames`, the same list the fold matches on) whose pick calls `PersonalEditorState.insertTitleLine(label)` — a title line that already carries its words, and an ARMED, empty one when the book has no chapter list (or the member asks for a marker of their own). Both book pages' WRITING column also carries the journal's blank-space tap (`.clickable { editor.focusLastLine() }` on the scrolling column), so a tap anywhere in the gaps hands the caret to the last line and the keyboard follows; without it the page sat dead with the caret nowhere (user report). And the writing side of an EXISTING personal page is entered deliberately: `PersonalWritingPage`'s load seeds the pen back down only when the page is still EMPTY (`decoded.isEmpty && !checklistFirst`) — a page with writing opens on the eye, which is what "opening a journal opens it on the eye page" means in practice. The read view passes `PersonalDocView(afterTitle = …)` (PersonalCanvas.kt) so each chapter's own review folds in under the marker that names it — matched by number ("Chapter 7", "7") or by the chapter's real name.
- **A book's own FILE is its own column (`documentPath`, migration 17 → 18) and lives in the app's storage.** `BookFiles` (`features/personal/BookFiles.kt`) COPIES a picked PDF/EPUB/text into `filesDir/books/<bookId>.<ext>` and the row keeps the PATH; `BookReaderScreen` reads that path (falling back to a legacy `content://` handle parked in `coverUrl`) through `PdfRenderer` / the EPUB zip / plain text. Two things this fixes at once: the document used to be stored in `coverUrl`, which is the COVER's column — so the shelf tried to paint a PDF as a picture and showed a broken cover — and a `content://` handle dies with the picker's permission, so an imported book stopped opening. Attaching is a COLUMN-SCOPED write (`PersonalDao.setDocument`: `documentPath` only) because the import path used to name the book after the picked file; **a file must never write a title, a blurb, a chapter list or a progress mark.** The book page's floating `BookReadPill` is the one door: it says "Read" when a file is wired and "Read a file" when not, and the old plain `Read` text button that sat beside "Download help" (and only appeared when a `content://` handle happened to be in the cover column) is gone.
- **The reader is a READING SURFACE: the chrome retires, the text runs on, and a passage can be marked.** `BookReaderScreen` (opened by `BookReadPill`) hides its header and footer on one tap and brings them back on the next — the words get the whole screen while you read — and an EPUB or text scrolls as ONE continuous run of paragraphs (the file's own `h1`/`h2`/`h3` become in-flow chapter heads in the type), not a page at a time; a PDF stays a page at a time but renders ONE page on demand instead of every page up front, so a long PDF opens at once and stops pinning the phone's memory. Position is remembered per BOOK (`PersonalDao.setReadingPosition`) and the open lands there. Marks live in their own `personal_read_marks` table (migration 18 → 19) keyed by book id PLUS the document path, so re-wiring a book to another file starts that file's own marks instead of smearing the old ones onto it: a HOLD on a passage raises the ink bar (four inks, deep coffee through the family's palette), a NOTE writes onto the mark, and a BOOKMARK drops with no selection at all. The reader lists every mark (its own words, its note, its ink) and tapping one jumps back to it — reading a mark never edits the book row.
**v426 — AND THE SHELF HOLDS MANGA, MANHWA, MANHUA, COMICS AND LIGHT NOVELS.** `PersonalKinds.kt` is the one model: `personal_books.kind` (TEXT, `MIGRATION_20_21`, default `book`, so every existing row reads as a book) carries the kind, `label(kind)` is what the shelf and the page say, `isComics(kind)` answers whether the COMICS sources own its metadata, and `idOf` is the only door a stored value takes back in (an unknown id reads as a book rather than crashing the shelf). **A kind is chosen BEFORE the search in `AddBookSheet`**, because it changes the sources: a book (or a comic) goes to Curio's own catalog and Open Library, while manga/manhwa/manhua/light novel go to `MangaFetch` — asked IN ORDER (AniList → MangaDex → Jikan → Kitsu, all keyless), answering `null` only when every source was unreachable so the sheet can tell "nothing found" from "offline". **v426b adds the KEYED door and widens the routing**: `asksComicSources(kind)` (manga family **+ `COMIC`**) is the question every routing decision now asks, `isComicBook(kind)` is the Western half, and `ComicVineFetch` is asked **FIRST for a Comic and LAST for a manga** — it is the only source of the five that has ever held a volume of *Watchmen* or *Saga* (see the v426b node below). A comics row keeps its kind on the row, and `BookEnrichment.enrich` sends one to `comicsPass` instead of the books doors — a books catalogue has never heard of a manga, so it must never be "enriched" with another book's facts. **The shelf card wears the kind on its own cover** (a plain book wears nothing: the absence is the label).

- **The shelf is THREE across, and sectioned.** `BookShelfScreen`'s grid is `GridCells.Fixed(3)` with `READING` / `FINISHED` heads (`ShelfSectionHead`, the DAO already sorts finished last), and the card says one thing well — the title, a 3dp rail and where the member is — because three across leaves no room for a paragraph under every cover. A finished book wears an accent tick on its cover.
- **A book the catalog does not have gets REAL chapters (`PersonalChapter` + `personal_books.chaptersJson`, migration 16→17).** Open Library keeps a table of contents per EDITION, so `BookEnrichment`'s Open Library pass looks the work up by title (the matched title must normalise to the one being looked up, or the contents belong to another book), keeps every edition's table with `>= 3` rows (fewer is a "Contents" line) and takes the FULLEST one — page ranges preferred — with the WORK's own table as the second door (v410; both used to live in per-question helpers, and the work was only asked when an edition had nothing), then stores the names plus the page range each spans when the edition gives one. `rememberBookChapters` is the ONE resolver both the book page and the chapter page read: the catalog's list when the book came from the lane, else the learned one, else nothing (numbered rows). Room is at **v17** with `MIGRATION_16_17` (that column plus `personal_notes.kind` / `topicId` / `topicName` / `categoryId`, all non-null defaults), so an existing library reads exactly as it did before. **v425 — a FILE answers its own page count whichever kind it is:** `documentPageCount` (`BookChapters.kt`) is the one door every attach path goes through — a PDF through PDFBox (`pdfPageCount`), an EPUB through `epubPageCount` (the printed page-list a typeset book carries, read from the same `epubPageList` the reader uses), 0 when the file cannot say — because the paths used to ask a PDF alone, so a book whose own copy is an EPUB adopted its contents with no length at all and its progress card stayed on "Set how long the book is".
- **`Mark finished` was a NULL in a NOT NULL column.** The DAO's `setFinished` wrote `updatedAtMillis = :at`, and un-finishing passes `at = null` — SQLite rejects the whole UPDATE, the throw was swallowed by the caller's `runCatching`, and the pill appeared to do nothing. The stamp is its own `now` parameter now. Read this before "fixing" a silent write: a `runCatching` around a DAO call hides exactly this class of failure.
- **The app's own accent, not "whatever primary is".** A surface that is CURIO's — a page, a shelf, a picker, a settings row — wears `settingsRoseAccent()` (+ `settingsReadableInk` for ink on it), never `MaterialTheme.colorScheme.primary`: `primary` is the fixed rose whatever hero the member picked, which is why an azure/Material/lane hero still met rose buttons. Swept in v389 across the Cabinet V2 content + personal shelf, the recycle bin, Stats, Manage categories, the category picker, the book-cover hub, the Settings page/account components, FieldMind, the Topic Database, the composer and Spin. What deliberately KEEPS the framework roles: `primaryContainer`/`onPrimaryContainer` (Material-hero-tears semantics), the reveal page (a topic wears its CATEGORY accent), `PetDesignerScreen` (its own playground palette), reaction colours, and the shared `ui/components/*` primitives, whose `accent` defaults callers override.

- **The shade's own doors (`SocialNotifications` + `SocialNotificationReceiver`).** A message notification is a `MessagingStyle` conversation whose large icon is the sender's REAL drawn portrait (`NotificationAvatars.of` draws the `SocialAvatar` art into an off-screen bitmap) and carries three actions that work with the app in the background (the receiver restores the session itself and `goAsync()`s the network work): **Reply** — a `RemoteInput` message box that sends what was typed as a reply to the LAST message, so the thread never has to be opened; **Like** — the app's own one-tap reaction on that last message; **Mute/Unmute** — `AppPreferences.mutedConversations`, per conversation and device-side, where a muted conversation still counts its unread so unmuting never fires a backlog. `DirectMessageScreen` cancels a conversation's entry when the thread opens.

### Incursion — the hidden viewing order (v389)
- **What it is.** `features/incursion/IncursionScreen.kt` (route `CurioRoutes.INCURSION`) is a page with NO door: not in the drawer, not in Settings, not a tab, and deliberately absent from `bottomNavRoutePrefixes` because it brings its OWN four-way bar at its own foot (**Marvel · Sony · X-Men · Personal** — the fourth was Essentials until v427, see the Personal-tab entry below). It lists every Marvel, Sony and X-Men title in the order they are meant to be watched, marks upstream's essentials, and keeps a status on each row (not watched / watching / plan to watch / watched / on hold / dropped).
- **The data is imported, never authored.** `scripts/import_incursion.mjs` fetches `mcuData.js`, `sonyData.js` and `xmenData.js` from the `mcu-viewing-order` project, imports them as modules (only `new Set(` and `Object.freeze(` are neutralised — the values are read out by their own exported names, so nothing is transcribed) and writes `app/src/main/assets/incursion/{marvel,sony,xmen}.json` (163 entries, ~89KB). **Never hand-edit those JSON files** — re-run the script when upstream moves. `trailerData.js` is deliberately never fetched: Curio wants viewing order, essentials and statuses, not promo material.
- **`data/IncursionCatalog.kt`** is the model + loader (Gson, cached in an `AtomicReference`, read once per process). It stamps each entry's `storageKey` (`"<studio>:<id>"`) on the way in, because entry ids come from three independent tables and collide by construction — Marvel 1 is Captain America, X-Men 1 is X-Men. **Every status read/write must be keyed by `storageKey`, never by `id`.** `order` is the viewing sequence and is NOT a list index (it repeats across seasons, and `id` is not dense); screens sort by it and read it as a number.
- **`data/IncursionStore.kt`** holds the two facts: the UNLOCK (`offer` — the phrase `i love you 3000`, matched lowercased with whitespace collapsed, accepted once per device) and the STATUSES (one JSON map in `curio_prefs`, reactive via `statusState`, seeded by `IncursionStore.seed(this)` in `MainActivity.onCreate`). Nothing here touches Room, the Cabinet, the streak or topic progress — a viewing order is its own shelf, and marking Iron Man watched says nothing about a Curio topic about Iron Man.
- **The lock lives in the shared search field, and that is the whole design.** `IncursionStore.offer` is called from `CurioSearchField`'s `onValueChange`, so "type it anywhere" is a fact about the app rather than a list of call sites: that component is what every search surface wears. Adding a search field therefore adds the lock for free — **do not re-implement the phrase check on a screen.** The check is a length test plus one comparison, and it returns immediately once unlocked.
- **The announcement is composed at the ROOT** (`IncursionUnlockReveal`, in `MainActivity`'s `setContent`, beside `CurioNavHost`) because the door it opens is on Home and the member is usually somewhere else. It sets `justUnlocked`, shows for ~3.2s (or until tapped) and consumes the flag itself. The DOOR is `IncursionHomeButton` — Home only, above the writing `+`, animated in and out with it, and it draws nothing at all until `IncursionStore.unlocked` is true.
- **BULK BY PHASE is the reason the page exists.** A group header (`GroupHeader`, shared by the list AND the grid — the grid emits it as a full-span `GridItemSpan(maxLineSpan)` row so a grid can never dissolve the phases the bulk action needs) carries a `Mark all` menu offering all six statuses plus a destructive `Clear this phase` (which asks first). `IncursionStore.setGroupStatus` writes once for the whole group. The header's own `Tune` pill does the same for everything currently listed, and clearing from there always confirms. **A bulk action must go through `setGroupStatus`, never a loop of `setStatus`** (one prefs write, one recomposition).
- **Two views, one behaviour.** `grid` is a saveable flag toggled from the header; v427 — the LIST leads with the row's own POSTER (40×56, the order kept beside the title so the sequence survives the art arriving) and the grid leads with the poster at the tile's whole width (168dp) over the order, the title and the meta line, with the status still a whisper in the tile's fill plus a DOT beside the order, because a chip's words are illegible at tile size. Both open the same `IncursionDetailSheet` — a `ModalBottomSheet` (the app's ONE detail language; it was an `AlertDialog`), carrying the poster, the title with its meta line, the current-state and Essential chips, the `ABOUT` synopsis, the `WATCH FIRST` prerequisite, the six-way status chips and the member's own note (see the v427 sheet entry below).
- **THE PERSONAL TAB REPLACED ESSENTIALS AS A DESTINATION (v427).** The member: *"instead of essential tab add personal tab where [the] status shows"* — so `IncursionDestination.ESSENTIALS` is GONE and `PERSONAL` sits where it was (last). Essentials survives exactly where it belongs now that it is not a place: the `ESSENTIALS` filter chip in `IncursionFilterRow`, the star on every essential row, and `IncursionCatalog.essentials(studios)`, which the desk counts. **Do not re-add an Essentials destination** — the filter and the star already answer the question it answered.
- **THE DESK IS `features/incursion/IncursionPersonal.kt`, and it READS.** `IncursionPersonalDesk(studios, statuses, onOpen)` takes the catalog and the statuses as plain values (the screen reads `IncursionStore.statusState` in composition and hands it down, so a status change repaints) and draws: the whole order's progress, one bar per line, what each line wants next, the six-state tally and the derived figures (hours in, hours left, essentials done, longest run). Its guards: `minutesOf` counts a series row as its per-episode runtime × the episodes it covers (stated count first, else the `epStart..epEnd` range, else ONE episode — never a guess), a row with no runtime counts for nothing, and **"next up" steps over `DROPPED`** because giving up on a title is a decision. The desk never writes a status; the only thing it can do is open a title's sheet.
- **ONE SEARCH, EVERY LINE (v427).** A non-blank query answers across all three lines at once (`sections` is built from every studio, and rows are grouped by line with a `StudioBand` head). The old behaviour scoped the search to the open tab, so finding a title meant knowing which of three lists it was in. Browsing still follows the tab; **a search does not** — do not put the query behind `activeStudio` again. The header says `Every line` while searching, and the field's placeholder says `Search all three lines`.
- **NEXT UP (v427).** `nextUp` in the screen and the desk's own list use the SAME rule — the first row that is neither `WATCHED` nor `DROPPED`, in the page's current order — so the pill in the head and the desk can never name different titles. The head's pill (`IncursionHeader`'s `nextUp` / `onOpenNext`) is a tap into that title's sheet, not a new control.
- **ART IS FETCHED THROUGH THE APP'S OWN CONSENT (v427).** `IncursionPosters.resolve` tries **TMDB by the row's own `tmdbId`** first (`TmdbFetch.posterUrlById`, the kind from the row's `type`, the other catalogue only as a fallback), then the keyless pair (`FilmPosterFetch` / `SeriesPosterFetch`), memoised per `storageKey` WITH misses. `IncursionPosterPlate` asks for nothing while `AppPreferences.coverFetchEnabledState` is false — that switch (the Experiments page's "Cover fetching") is the toggle this feature rides, **so never add a second switch for Incursion art**, and every plate must keep its drawn fallback (the order + a kind glyph) so a member with fetching off still sees a finished page. Resolution happens inside `LaunchedEffect` per composed row, so a lazy list asks only for what was scrolled to.
- **THE SHEET IS THE TITLE'S OWN PAGE (v427).** `IncursionDetailSheet` opens on the POSTER (94×140 beside the title, with `INCURSION · #<order>` as the eyebrow, the meta line, the current-state chip and the Essential chip), then `ABOUT` (the synopsis), `WATCH FIRST` (the prerequisite), the six states as ONE scrolling chip row (they were six stacked full-width rows, which pushed the words off the sheet's first screen), `YOUR NOTE` and Done. The note is `IncursionStore.setNote` — one line, `NOTE_LIMIT` characters, a blank removing the entry — and the field writes it **after a 500 ms pause, never per keystroke**.
- **The state has ONE name for a chip and one for a menu (v427).** `Status.label` is how a state is OFFERED ("Plan to watch"), `Status.shortLabel` is how it is TALLIED and chipped ("Planned"); both live on the enum in `data/IncursionStore.kt`. The chip used to carry its own third mapping — **do not add another `when` over the six states.**
- **The page wears the app's own language (v425).** `statusInk` is the ONE tone resolver for the six facts: each keeps its own hue but is read through the app's tone discipline (a deeper ink by day, a lifted but muted one at night) instead of six fixed hexes that were chosen against a flat light page and therefore shouted on a named theme's night; a chip FILLED with one of them takes its ink from `curioFillInk`. Every part of the page is a card — `surfaceContainerLow` mixed with `curioTintOn`, lifted by `curioCardShadow`, and **never** a `BorderStroke`, because the app has no drawn card edges — including the head plate that carries the blurb and the page's one progress bar. The announcement (`IncursionUnlockReveal`) is a Curio card in the theme's own ink, not white words on a black scrim.
- **A Gson note that has bitten before:** Gson builds these data classes through Unsafe, so Kotlin defaults do NOT apply — every non-nullable field must exist in the JSON (the importer guarantees `id`/`order`/`group`/`type`/`title`/`essential` on every row) and an unnamed `group` would silently become 0 and fall out of the phase list.

### Online layer (Supabase) — account + Online Mode
- **Package:** `data/supabase/` owns the whole online layer — `SupabaseClient` (a hand-rolled OkHttp REST/Auth client; **no supabase-kt dependency**), `CurioSecureStore` + `SupabaseSessionStore` (the Keystore-sealed session — tokens are NEVER written in the clear) and `OnlineAccount` (the observable state the UI reads). Its child AGENTS.md carries the local contract.
- **Credentials:** `BuildConfig.SUPABASE_URL` + `SUPABASE_PUBLISHABLE_KEY` (falls back to `SUPABASE_ANON_KEY`) come from the build environment, and both `android.yml` and `release.yml` export either key name. The **service-role key is never read, stored or exported** — only public client credentials ship in the APK.
- **Offline-first:** Room stays the source of truth for captures and Online Mode is a local preference (`AppPreferences.isOnlineModeEnabled`, default OFF) that flips off on sign-out and gates every online action. Only text metadata may ever sync — images, audio and screenshots never upload.
- **UI:** the account page is `features/settings/OnlineModeScreen` (route `CurioRoutes.SETTINGS_ONLINE`), reached from the hub's *Your data & privacy* group, the settings nav rail ("Online") and the hub search. It wears the settings-family design language (torn hero + `SettingsHeroHeader`, frosted `SettingsOptionCard`s, Playfair `SettingsSectionHeading`s) like every other sub-page.
- **Errors:** account failures render through `onlineAuthMessage` — rate limits, unconfirmed emails and bad credentials stay actionable; anything unrecognised collapses to one generic line so a raw response body can never reach the UI.
- **Email links belong to the account site (`auth-web/`, root AGENTS.md).** Sign-up sends `redirect_to` (GoTrue's own query parameter, built by `SupabaseClient.signUp(email, password, confirmationRedirect)` from `OnlineAccount.confirmationRedirect()`, which reads `BuildConfig.CURIO_AUTH_SITE_URL` + `/confirm`). Without it Supabase falls back to the project's Site URL, whose default `http://localhost:3000` is the "the confirmation link opens localhost" report. The sign-in card's **"Forgot your password?"** row opens `<site>/reset` in the browser for the same reason: a reset link has to land somewhere a password field can live. Both are hidden on a build where `CURIO_AUTH_SITE_URL` is empty, so no build ever offers a door that goes nowhere.
- **Community (24-hour text cards):** `data/supabase/CommunityApi.kt` is the only door to `community_cards` / `community_reactions` / `community_reports`, and `features/community/CommunityScreen.kt` (route `CurioRoutes.COMMUNITY`, reached from the Online mode page's *Community* row) is the wall: cards are rebuilt with the app's own `TopicShareCard` from stored text/topic/style data, so nothing media-backed can ever be posted or shown. A card carries a poster caption (≤180 chars, shown above the card) and can be opened in its own view (`CommunityCardScreen`, route `community/{cardId}`) where the full card, its remaining life, its share-as-image action. Replies (`CommunityCommentsSheet` + `CommunityApi.comments/comment/likeComment/unlikeComment/deleteComment`) open in one shared bottom sheet from the wall or the card view — drag handle, swipe/back to close, no close cross, composer above the keyboard — because a weighted scrollable inside a sheet is measured with an infinite maximum height and crashes, the sheet caps its list with `heightIn`. A reply is capped at 400 chars and cascades off the card, so none can outlive it, and replies BRANCH to ANY DEPTH: `community_comments.parent_id` (with the `curio_pin_comment_parent` trigger refusing a self-reply or a parent on another card) lets a reply answer a reply, and **`branchRenderList()` is the ONE renderer of that tree** — a depth-first walk that emits every reply exactly once at its real depth, lets each reply fold its OWN answers behind its own `More` door (`BRANCH_PREVIEW` shown, any level expandable), keeps the indent bounded (`MAX_REPLY_INDENT` steps, a hairline thread rule carrying the rest) and promotes only a reply whose parent is genuinely absent from the page to the top level, so nothing a member wrote is ever hidden and nothing is ever drawn twice. The sheet renders that list in ONE lazy pass keyed on `BranchRender.key` (a reply and its own door are consecutive rows — splitting them into two `items()` calls stranded every door at the bottom of the sheet), `CommunityReplyRow` is three lines (name · age · actions, the reply's own body between them), and its actions are the heart (`CommunityApi.likeComment` → `community_comment_reactions`, §4c — a reply heart cannot live in `community_reactions`, whose key is card + member), Reply on ANY reply including your own, then the owner's (Edit/Remove) or the reader's (Add friend / Report / moderator Remove) ICON-ONLY set. Tapping Send inserts the row the server returns (no thread re-read needed) and scrolls it into view. `CommunityApi.comments` asks for the NEWEST page and re-sorts it in memory — `order=asc` with a limit returned the OLDEST replies, which is what made a just-written reply fall out of the window and "vanish". Posting has THREE kinds (`kind`: CARD / NOTE / QUOTE): a topic CARD is seeded from the chosen topic's own quick fact (`CurioTopic.teaser`, via `quickFactOf`) and rendered with the share-card renderer, while a NOTE and a QUOTE render through `SocialTextPost` (a quote also carries its credit in `byline` and shares as text, not as card art). **`CommunityCardCanvas` lays the card out DIRECTLY at the width its row offers** (the card editor's own 280dp base), capped at the card's design width (405dp / 450dp) — never a layer-scaled miniature and never a crop: the card's own smart fit sizes the title, fact box and text for the size it is handed, so every surface shows the whole card crisp and a row never reserves height the art does not use. The wall claims `FEED_CARD_WIDTH` (0.78f) of its row, the composer preview 0.85f, the card's own page 1f. A card expires 24 hours after posting (`expires_at` defaults server-side, reads filter `expires_at > now()`), entry requires a signed-in account with Online Mode on — and the same gate is enforced by RLS in [`supabase/schema.sql`](../supabase/schema.sql), which is the security boundary (public key only, policies for `authenticated` only, no `anon` policy, no service-role key anywhere). Identity columns default to `auth.uid()` server-side, so the client never sends one.
- **Social identity is resolved LIVE, never read off the row:** `community_cards.author_handle` / `community_comments.author_handle` are stamped at INSERT time by a DB trigger (a modified client must not be able to forge a handle), so the stored handle FREEZES the poster's name at post time. Every read therefore enriches the rows through `CommunityApi.withAuthors` / `withCommentAuthors`, which call `SocialApi.people(token, ids)` once per screen and fill `authorName` + `authorAvatar`. The UI always renders `card.authorLabel` (live username, then the snapshot) and `card.authorAvatar` — never `authorHandle` directly. A profile that is no longer readable (Online Mode off) leaves the snapshot in place rather than failing the feed.
- **Member profiles:** `features/community/SocialProfileScreen.kt` (route `CurioRoutes.SOCIAL_PROFILE`, `person/{userId}`) is every social identity's destination — a card's author row, a reply's author, a friend row and the conversation header all open it. It reads only the public half of `profiles` (username, avatar style) plus the member's live cards (`CommunityApi.cardsByAuthor`), and offers Add friend / Message / Ban-or-tier (moderators, via the same `ModerationBanDialog` the moderation page uses) / nothing when it is your own page. The page is a `LazyVerticalGrid` (2 preview columns on a phone, 3 on a wide window): **the identity block lives INSIDE the hero tear (v388, user decision — "put the profile inside the header tears... by expanding the tear")** — the tear extends by `profileTearHeight`, the portrait, name, @handle, the three counts (posts / likes / replies, computed from the SAME `cards` list the grid renders so the page can never claim a number it is not showing), the bio and the action pills are drawn on the banner with hero ink, and there is no "a member of the community" line any more. A moderator's view also carries the member's **moderation history** (`CommunityApi.memberHistory`, the member can read their own) as quiet rows under the identity, and one's own Profile tab shows the same record (ProfileScreen). Then one square preview tile per post. A `KIND_CARD` tile previews the TOPIC, never card art: the tile wears the lane's accent wash (the lifted twin in dark mode), that lane's glyph in a chip and the topic name — a share card squeezed into a square is a cropped thumbnail nobody can read, and the full card has its own page one tap away. A note or quote tile keeps its words (with the credit under a quote). A preview is only a door, so counts and actions stay on the post's own page. Blocking moved behind a ⋮ `CurioDropdownMenu` (rare and irreversible, so it is never under a reading finger), and **presence is deliberately NOT drawn on a profile** — Curio's activity status is only ever shown inside a direct chat. The page keeps `SettingsHeroHeader` as a pinned overlay on a phone and as a spanning grid item when wide. It is wrapped in `SocialSharedScope` (the social family's own scope in `CurioNavHost`) — the community routes are NOT settings destinations any more: the card view no longer mounts `SettingsNavRail`, and only the app's established torn hero is shared between the two families.
- **Moderation (`features/community/ModerationScreen.kt`, route `CurioRoutes.MODERATION`):** the old `ReportedScreen` is gone — the Social tab's own Moderation door (shown only to a team member) opens the control room, which has two halves. **The queue** holds every report whatever it names: `community_reports` now carries `card_id` / `comment_id` / `target_user` with a `community_reports_one_target` CHECK (exactly one) and one partial unique index per kind, so a member can report a post, a reply AND a member, and re-reporting the same thing REFRESHES their own row (`curio_file_report`) instead of failing on the old `(card_id, reporter)` unique constraint — that constraint is dropped, which is the fix for "it doesn't let me report again". Reported content is re-read through `CommunityApi.cardsByIds` / `commentsByIds` (the admin select policies keep a reported card readable even after its own 24 hours), and every decision goes through `curio_handle_report` — `dismiss`, `remove_content`, `hide_author`, `reopen` — each asking for its own permission and recording a `moderation_actions` row. **The team** half lists `community_admins`, whose rows carry `role` (`owner` | `admin`) + five independent switches (`can_delete_posts`, `can_delete_replies`, `can_handle_reports`, `can_manage_admins`, `can_ban_members`); a moderator is added by `@username` (`SocialApi.findByUsername`) and their switches edited in `ModerationPermissionsDialog`. `@jugnu`'s row is seeded as the OWNER (`role = 'owner'`) by `schema.sql` and is protected in SQL: `curio_set_community_admin` refuses to touch or create an owner and `curio_remove_community_admin` refuses to remove one — the community can never lock itself out of its own controls. `curio_admin_can('posts'|'replies'|'reports'|'admins'|'bans')` is the single permission test every policy and function calls (the owner is always true), and the client's `CommunityAdminRow.allows(...)` only decides what to OFFER. **Bans are a FOUR-TIER LADDER with its own clock** (`profiles.ban_kind` in `''|content|read_only|social|account` + `banned_until`, v388 — user decision "four levels, picked per ban"): *content* hides their posts and refuses the wall (the classic ban, friends still work), *read_only* leaves the wall readable but refuses every write (posting, reactions, profile edits) AND the social side, *social* keeps the wall and pauses friends/requests/messages, *account* locks everything and hides the content. `banned` stays the boolean older clients read (= content/account tiers); `curio_ban_kind()` is the one lapsed-aware tier read, and **enforcement is a BEFORE trigger (`curio_ban_guard`) on every writable table** — a security-definer RPC bypasses RLS by construction, so a tier that only lived in the policies would be decoration; the trigger fires on both paths and a lapsed timed ban reads as no ban (no cron). `curio_can_post` / `curio_can_social` are the per-side tests the guard and the policies share. **The ban list is the moderation page's third tab** (`CommunityApi.bans` → `curio_moderate_list_bans`, gated by the 'bans' permission): live bans first then lifted/lapsed ones as quiet rows, each with tier chip, clock line, reason, ban-setter, and Change-tier / Lift actions; `curio_moderate_ban_member` / `curio_moderate_lift_ban` / `curio_moderate_member_history` are the only write doors (reason required, owner unbanable, self-ban refused). `SocialApi.moderationStatus` reads the member's OWN row + active tier so the app can say WHY (the wall prints that notice instead of letting a post fail with a raw server error), and a moderator can Ban / Change tier / Lift from a profile's ⋮ too. **No destructive move is a bare tap:** every removal and hide goes through `ModerationReasonDialog` (a reason + an optional note), and member-facing reports use `ReportTargetDialog` with `CommunityReportReasons.CONTENT` (post/reply) or `.MEMBER` — a reason is required, the note is free text. A reply's own row offers Report to a member and Remove (with reason) to a moderator (`CommunityCommentsSheet.canModerateReplies`), and a post's page offers the same moderator removal beside its report flag. `CommunityApi.myAdminRow`/`admins`/`setAdmin`/`removeAdmin`/`handleReport`/`removeCardWithReason`/`removeCommentWithReason`/`deleteHandledReports` and `SocialApi.hideMember`/`moderationStatus`/`findByUsername` are the only doors — the same rules live in [`supabase/schema.sql`](../supabase/schema.sql) §5a/§6b, which is the security boundary.

  **v403 — the queue is a work list and the ban list is a record.** Four things were wrong with reading them, and each is now a rule:

  1. **A decision refreshes what it changed.** The first read (`load`) is guarded by `loaded` because it also verifies the team row and reads the roster, so every action that called it back got NOTHING: a report that had just been dismissed or resolved stayed in the list until the screen was reopened. `refreshQueue` (re-read + `absorbQueue`) and `refreshTeam(quiet)` are the re-readable halves, and every write goes through one of them (`applyBan` / `applyLift` / `applyReportAction` / the queue's own ban path → queue; `removeAdmin` / permissions save → roster).
  2. **The row has a PAIR, not a row of buttons.** Remove (destructive) and Dismiss (accent) are the two decisions, each keeping its `ModerationReasonDialog` sheet — the member's own choice ("keep the sheet for both") — and acting on the AUTHOR is `ModerationQuietAction`, a plain label beside them, because a row where every action shouts is a row where none is read. The queue's head carries the one action that is about the LIST rather than a report: `Clear N handled`, shown only when something IS handled, behind `SocialConfirmDialog` → `CommunityApi.deleteHandledReports` → `curio_moderate_delete_reports()` (which deletes `status <> 'open'` only, so a sweep can never drop a report nobody has read, and answers with the count so the notice can say what happened).
  3. **The ban list is one door per row.** `ModerationBanRow` shows the tier chip, one `Open` (filled while a tier is in force, outlined on a lapsed ban) and a quiet `Profile`; Change tier and Lift live INSIDE the sheet `Open` opens, instead of as buttons on the list. A lapsed ban passes `currentKind = ""` so the sheet opens ready to SET a tier rather than to "change" one nobody is under, and the lift target is held as a `(userId, label)` pair — `lifting` used to be a row of the ban list, which the queue has never read, so "Lift the ban instead" from a reported member silently did nothing.
  4. **Every moderation sheet shows the server's refusal inside itself.** `ModerationReasonDialog` and `ModerationBanDialog` both take `error`, printed under the dialog's own title (the one part nothing can scroll away), and each sheet opens on a clean slate (`error = null` at the call site). A refusal used to be written on the page BEHIND the open dialog — which is exactly what made a ban the database turned down read as a dead button (user report: "i am not able to ban any members the ban button isnt working"). The ban sheet's reasons are also wrapping CHIPS now (`BanReasonChip` + `FlowRow`) instead of eight full-width rows, so the required reason is on screen with the tier and the clock, and a disabled Ban button says what it is waiting for ("Pick a reason").
- **Sharing into the community:** the wall's posting door is the floating "Post" button (there is no header button), which opens `features/community/CommunityPostScreen.kt` — a FULL-SCREEN creation destination (the old `CommunityComposerSheet` bottom sheet was deleted; one composer only). The composer PICKS the topic from the catalog (`TopicJsonLoader.loadIndex()`, falling back to the warm lane pools) instead of taking a typed name — the lane, glyph and accent come from the chosen topic, so a card can never disagree with the topic it is about. Note (default) / Topic / Quote share one flow, ordered TOPIC FIRST when a topic is chosen (the chooser leads the flow, and it is COLLAPSED until tapped — v385; it used to spring open on entering Topic, which put a long wall of results between the writer and everything else). Its search FILTERS BEFORE IT RANKS (`searchTopics` + `topicMatchRank`, v385: name prefix, then a word of the name, then a name substring, then byline/subtype, then a tag, then the teaser; shortest name first inside each band; 8 results, 5 suggestions with no query) — the old pass sorted the whole 16k-entry index on every keystroke and then ordered what was left alphabetically, so a teaser- or tag-only hit arrived as an alphabetical near-miss and every result carried two lines of teaser. The index is seeded from `TopicJsonLoader.cachedIndex()` so the field answers on the first keystroke, and each row names its LANE so two topics with the same name are told apart. The preview is ALWAYS ON (v349, user decision) and renders through the canonical `CommunityCardCanvas` at the wall's own card size; a topic post carries a **Card | Note presentation toggle in the preview header** — Note ships the post as plain text with the topic name above the words (no card art, no look controls), Card ships the share card and reveals the look controls (paper/vinyl/minimal, shape, text size — card-only by design). The caption is offered to EVERY kind (the wall prints it above the body), a quote keeps its credit field, and a draft is rebuilt by the wall's own `onPost` pipeline — never a lookalike renderer (the preview card uses this device's real identity). **Drafts and deleted posts are LOCAL (`data/SocialPostArchive.kt`, v388, user decision):** an unfinished composer entry is kept per-kind in a private app file and offered back ONCE per kind switch (`draftOffered`), cleared on post and on sign-out; a post you delete is remembered in the same archive, offered at the FOOT of the wall as a small "Deleted posts" door where it can be re-posted through the normal pipeline (`repostOf`) or forgotten — it never goes back to the server until the member re-posts it. Chevron aliases for the picker live in `ui/theme/CurioIconAliases.kt` (mapped to glyphs the bundled subset already has — do NOT delete `CurioIcons.Favorite` there; the reply hearts use it).
- **The wall's look (v388, user decision):** post cards wear the app's rose ACCENT family, not the capture paper creams — the airier row (12dp padding, 36dp portraits, 8dp seams) is the shipped one; the compact A/B that rode the same commit was reverted (v389) and its `communityRoomyWall` Experiments switch removed with it; the floating Post pill hides while the wall scrolls DOWN and returns when the finger goes up or the page reaches the top (the same `snapshotFlow` scroll-slope pattern as Home's `+`). A QUOTE renders through `SocialPullQuote` (SocialComponents.kt) on BOTH the composer preview and the wall — one composable, one shape: an accent rule carrying the block, an oversized opening mark, the serif face, and the credit tied on by a short dash — so what the writer previewed is what the wall shows.
- **Follows (v389, user pick after the ladder shipped):** `member_follows` (follower/followed, PK pair, block-outranks-follow insert policy) is the Follow button's whole server side behind `CommunityApi.follow/unfollow/followingIds` — the wall's **Following chip** filters the feed to `authorId in followingIds` client-side (ids read once per open, off = Everyone), and a member profile's second hero pill flips optimistically and rolls back on a refused call. A quote-repost (the fourth post KIND `REPOST`) shipped beside it in the same pass and was **reverted on the member's call (v389)**: there is no `quote_source_id` / `quote_words` column, no Repost pill, no quoted-post nesting, and posting is back to the THREE kinds above.
- **Social avatars:** Community identity uses only `profiles.avatar_style` (0–27) and the code-drawn `features/community/SocialAvatar.kt` renderer — see the v395 redraw bullet above for the art itself (twenty cozy-minimal portraits, then eight cozy-minimal icons, one shared grid and one ink). Users choose from 28 local canvas styles in Online Mode; no avatar bitmap, URI, upload API, or media column belongs in the online layer. The drawing is CLIPPED to the disc (`clipPath`) so a bust that reaches the frame's bottom can never show square corners outside the circle, and every shade is DERIVED from that row's own garment/skin/hair tones, so the whole 28-row cast still costs 28 small integers per build and nothing is uploaded.
- **The portrait COUNT is a contract in four places.** `SOCIAL_AVATAR_STYLE_COUNT` (`data/supabase/SocialApi.kt`) is the single bound every read/write clamps to (`SocialApi` people reads, `SocialPeopleCache`, the inbox people cache, the feed cards' `authorAvatar`) and `supabase/schema.sql`'s `profiles_avatar_style_range` check must move with it (`between 0 and 27` today). Adding a style to the app's `PORTRAITS` / `ICONS` lists without widening that check makes the pick fail the write; widening the check without bumping `SOCIAL_AVATAR_STYLE_COUNT` silently truncates the pick at the cache boundary. The picker's `items(...)` range reads the same constant — never hardcode `0..15` anywhere again.
- **Curio's social language — `features/community/SocialComponents.kt` is the ONLY place social chrome is defined.** Box cards (`SocialCard`), pill actions (`SocialPill` / `SocialIconPill`, tones ACCENT / NEUTRAL / DESTRUCTIVE), person cards (`SocialPersonCard`), the one dense row every list uses (`SocialSidebarRow` — a portrait with its live dot, a name, a subtitle, an optional stamp, an optional unread badge and an optional long press; a CONVERSATION and a CONTACT are the same row so the inbox and the contacts sidebar cannot drift) with `SocialLetterHeader` as a section's quiet seam and `socialLetterOf` as the single place a name becomes a letter, empty cards (`SocialEmptyCard`), day rules, the frosted social search pill, the reaction palette (`SocialReactions` — the emoji IS the palette entry and the stored kind) and the text-post surface (`SocialTextPost`, the body of a NOTE or a QUOTE). Caches live here too, and all of them are built on `SocialCache` (`data/supabase/SocialCache.kt`: one JSON file per entry under `filesDir/curio_social_cache/`, a warm in-memory layer in front of it, a TTL per kind, and oldest-first eviction past each kind's ceiling): `SocialMessageCache` (threads), `SocialPeopleCache` (resolved identity per account, read on the FIRST frame so a name is never a placeholder), `SocialFeedCache` (the last wall page, expired cards dropped on read), `SocialInboxCache` (the thread list + pending requests + friends), and `SocialCommentsCache` (one card's replies). Never put a social cache in `SharedPreferences` again: a whole-store blob means one write re-serializes everything and nothing ever expires. Every social surface (wall, Friends, a profile, a conversation) composes from this kit, so a name, a portrait and a set of actions can never drift between screens. A new social row/card that hand-rolls its own `Surface` + `TextButton` is a bug.
- **`SocialRelation` decides the pills, never the list a row happens to live in.** `NONE` / `OUTGOING` / `INCOMING` / `FRIEND` / `SELF` is derived once per screen from `friends` + `requests` and passed into `SocialPersonCard`, which is what makes "Add friend" impossible on someone who is already a friend, already asked, or is you. Adding a person action means adding it to this enum's `when`, not to a screen.
- **Account surfaces are shared: `features/settings/CurioAccountComponents.kt`.** `CurioAuthCard` (two modes — Sign in / Create account — with ONE primary action, local validation before the network, and the server's own answer shown in a message box) and `CurioAccountIdentityCard` (username with its rules stated up front and the server verdict shown, plus the portrait picker over all `SOCIAL_AVATAR_STYLE_COUNT` styles). Online mode and Edit profile both render these, so the two doors can never disagree about what an account is or what a taken username says. Sign-in and the username live in **Edit profile** now; the Community row was removed from Profile.
- **FRIENDS and CHATS are two screens, not one list (user decision).** "Who are my people" and "what was said" are different questions, so `FriendsScreen` (route `CurioRoutes.FRIENDS`) is the CONTACTS SIDEBAR — a `SocialSearchField`, incoming requests first (the only thing here with a deadline), outgoing asks, then the friends list filed A–Z under a `SocialLetterHeader` — and `ChatsScreen` (route `CurioRoutes.CHATS`, reached from the wall's action row and from Friends) is the INBOX: the conversation list, newest first, with the last line, a stamp and an unread badge. The contacts sidebar CLOSES with a **"Start a chat" strip** (`RecentFriendTile`, a horizontal row of the newest friendships — `SocialApi.friends` already orders by `responded_at desc`, so the people a new chat is most likely to be with are the ones just added; A–Z above stays the order to LOOK someone up in, which is exactly why the strip exists at the bottom). Tapping a friend row — or a face in that strip — opens the CONVERSATION with them (the thing a contact row is for) and a long press on a row offers Remove; tapping a chat row opens the thread and a long press offers the two deletions. The wall's doors — Chats (`CurioIcons.BubbleChart`), Friends (`CurioIcons.Hub`, changed from the `notes` pad, which read as "writing" rather than "people") and You — ride the HERO BANNER ITSELF (v386): `SettingsHeroHeader` gained a `footer` slot plus `footerHeight`, which EXTENDS the torn banner (`bannerHeight + footerHeight`) and is mirrored by the public `settingsHeroTotalHeight(footerHeight)` the wall reserves, so the row paints with the header instead of appearing once the account check landed (as a list item under "Last 24 hours" it was missing every time the wall opened). `CommunityDoorTile` wears `settingsHeroPillFill()` — the SAME opaque hero-pill glass as `SettingsHeroActionPill` (v27n: an opaque lerp, never a translucent fill, or the elevation shadow bleeds through as a smudge) — and the row is visible to everyone, signed out included, because both destinations carry their own "Sign in…" card. Both screens sit behind the same gate (Online Mode on + signed in) and both use `PullToRefreshBox`; neither has a Refresh button. The INBOX also carries the **compose button** (bottom-right, v385, only while that gate is open) — it raises a FRIEND PICKER sheet (`NewChatSheet`: `SocialApi.friends` rendered with the shared `SocialSidebarRow`, read ONCE per opening, with one door into Friends for anyone not a friend yet). A conversation that does not exist yet used to mean a detour through Friends; the list reserves 72dp under its last row so the button never covers a chat.
- **Deleting a chat has two shapes and the sheet says which is which.** "Delete for me" = `SocialApi.hideConversation` (`dm_conversation_hidden`, owner-only) + `SocialMessageCache.forget` (the SERVER marker alone leaves the deleted chat opening from this device's own copy). The thread returns by itself when the other person writes, because `threads()` only hides a conversation whose newest message is older than `hidden_at` — never hide it locally forever, or a reply would be swallowed. "Delete for both of us" = `SocialApi.deleteConversation` → the §5h `curio_delete_dm_conversation` function, which removes the pair's messages, both hidden markers and the conversation row; its failure IS surfaced (it is permanent), unlike hiding and presence, which are best-effort. `SocialInboxCache` is written through `replaceThreads` / `replaceContacts` so one screen's half never blanks the other's.
- **The live dot is a fact, not decoration.** `CurioPerson.isActiveNow` (visible activity + a stamp inside five minutes) is the ONLY input: `SocialAvatar(online = …)` draws a green dot cut out of the portrait with a ring of page colour. The inbox resolves presence through `SocialApi.presenceOf` (the opt-in `PERSON_COLUMNS_PRIVACY` read, cached in-process for `PRESENCE_TTL_MS` = 60s so a 20s tick does not re-ask) and folds it into the identity with `CurioPerson.withPresence`, because `threads()` must not spend two requests per row. A member who hid activity, or whose stamp is unknown, gets no dot — never a grey one.
- **Messages: the device's copy first, then the server.** `SocialMessageCache` (in `SocialComponents.kt`, on `SocialCache`'s `thread` kind, newest 200 lines per conversation, TEXT ONLY) renders a thread instantly on entry; the network then MERGES into it (see the 24-hour rule below). `mine` is recomputed from the signed-in id on read (never stored), so switching accounts can never mislabel a bubble. A cached thread with no signal must stay readable — never blank a loaded thread because a later call failed. Signing out deletes the whole cache directory (`OnlineAccount.signOut` → `SocialCache.clear`, plus the old `curio_social_cache` prefs file for upgraded devices), so the next account cannot read the previous one's threads or names.
- **A DM bubble's gestures are Instagram-shaped (decided):** a DOUBLE-TAP is the heart (the palette's first emoji, toggled), a HOLD raises a floating action sheet attached to the bubble (emoji palette, Copy, Edit for my plaintext rows, Remove for mine, Reply — NO dialog, no scrim; a second hold drops it), and a horizontal SWIPE answers the message (the bubble leans, then the composer's reply banner arms the next send with `replyTo`). Replies are TRUE threaded rows: `dm_messages.reply_to` (schema §5e) with the quote drawn INSIDE the bubble (`ReplyQuoteRow`), one level deep enforced by the `curio_check_dm_reply` trigger. MY bubbles wear the brand rose (`curioDialogActionColor()`), theirs stay a neutral raised surface — a translucent or rose-tinted receiver bubble is wrong.
- **A bubble's SIDE is its own alignment, and the swipe is HORIZONTAL-ONLY.** `Arrangement.End` for mine / `Arrangement.Start` for theirs is the only thing that positions a bubble: a weighted spacer before a received bubble ate the free space and pushed every short reply of theirs to the RIGHT. The reply gesture uses `detectHorizontalDragGestures` (never a plain drag detector), so a vertical drag falls through to the thread and a long message stays scrollable — the detector that consumed every drag is what made a message you had to scroll past impossible to scroll past. The quote inside a bubble inks itself from the bubble (`mine` ⇒ white, theirs ⇒ `onSurfaceVariant`), never hardcoded white, and a parent outside the 24-hour window renders "This message is no longer available" instead of the quote silently vanishing. The action sheet's destructive chip is a SOLID `error` fill with `onError` ink — a 12%-alpha pill reads as disabled, not as the one irreversible action.
- **Editing is a SERVER FUNCTION, never a table write (§5d2 `curio_edit_comment` / `curio_edit_dm_message`).** Both edits used to be direct writes and both read to a member as "editing is broken": a PUT on a filtered route answered `column pgrst_body.id does not exist`, and a PATCH the row policies did not expose answered 204 while changing nothing (the app painted a success over an unchanged row). The functions check the author/sender, re-run the public-text rule (replies only — a DM is unfiltered by design), stamp `edited_at` themselves and RAISE on refusal, so a success means the row really changed; the row's ID never moves, so reactions and answers stay attached. `migration_state` is `plaintext` OR `legacy` for an editable row (`legacy` is a pre-encryption-opt-in plain row — the table's CHECK guarantees a body and no ciphertext; `CurioDirectMessage.editableText` is the one test), and a legacy row RENDERS its body instead of a "re-encryption required" placeholder. **The edit controls are ON by default (v386, user decision):** `AppPreferences.isSocialTextEditingEnabled` reads `KEY_SOCIAL_TEXT_EDITING` with a `true` default, so Edit shows on your own messages and replies out of the box; the Experiments switch still turns it OFF and an explicit off wins, so the read sites (`DirectMessageScreen`'s bubble actions, `CommunityCommentsSheet`) need no change — only the untouched default moved.
- **Messages live 24 HOURS on the server and stay on the DEVICE (decided).** `dm_messages` has a read window (`created_at > now() - interval '24 hours'`) and a `curio_purge_expired_messages()` sweep, so the server holds a message for a day and no longer — an expired message is unreadable even to its own participants. What you RECEIVED is yours: `SocialCache.TTL_THREAD_MS` is a YEAR, and opening a conversation MERGES the server's window into the device's copy instead of replacing it, which is what makes "gone from the server" and "gone from Curio" two different things. **The SERVER's copy wins on a shared id and is merged in FIRST (`fresh + known`, distinct by `id`)** — it is the only copy that knows about an edit made on another device, and the only one carrying `edited_at`/`reply_to` at all; cached-first was why an edit showed as "edited" and then lost its marker on the next open. Never go back to `messages = fresh` (that is the bug where a day-old thread silently lost its older lines), and never render the merge as a growth risk — it is `distinctBy { id }` on a bounded thread. `SocialMessageCache` stores the WHOLE row shape (`i`/`s`/`b`/`t`/`r`/`e`/`p`: id, sender, body, created, read, edited, reply-to) — dropping `e`/`p` made a cached row shadow the server's own and lose the marker and every quote.
- **Every social write passes `CurioContentFilter` (`data/CurioContentFilter.kt`).** It is the ONE gate for profanity, explicit sexual content, real slurs and harassment, and it is deliberately written to survive the tricks: `fold()` strips accents, maps look-alikes (`0→o`, `3→e`, `@→a`, `$→s`, `|→i`) and drops every character it does not recognise, then two views — `squash` (all separators gone, so `f u c k` and `f.u.c.k` collapse) and `collapse` (runs squeezed, so `fuuuuck` collapses) — are matched with `UNAMBIGUOUS`/`SLURS` anywhere and `NAME_LIKE` (`ass`, `sex`, `cock`, `cum`, `tit` …, words that hide inside innocents like "class", "Essex", "cocktail", "title") as WHOLE words after `mergedWords()` fuses consecutive single letters. It runs in the API layer (`CommunityApi.comment/post`, `SocialApi.updateUsername`, `updateDisplayName`, `updateBio`) so no screen can post around it, and it is surfaced in the composers (the send control sleeps and the reason is printed) plus a durable `NAME_WARNING` on the account form. **A DIRECT MESSAGE IS EXEMPT (user decision):** `SocialApi.send` never calls the filter (and `dm_messages` carries no text CHECK) — two friends having a private conversation write what they like, and RLS (the two participants, friends only) is what protects a thread. Only the PUBLIC surfaces are gated. **The same fold exists in SQL as `curio_normalize_text` / `curio_text_is_clean`, CHECKed on every social table** — a modified client is refused by the database. The two folds must stay in step: SQL mirrors Kotlin's letter map through `translate()` where a target is one character, carries the multi-character ones (ß→ss, æ→ae, œ→oe, þ→th) as an explicit `replace()` chain in the same order, and owns an accented-letter map for the precomposed letters Kotlin removes by decomposing to NFD and dropping the marks (so `nïgger` folds to `nigger` on BOTH sides, not to `ngger` in SQL) — a fold that drifts between the layers is a hole a modified client can spell through, and homophone spellings such as `phuck`, `fvck`, `fack` are listed EXPLICITLY for the same reason. The lexicon is a MODERATION list: mild words (damn, hell, crap, suck, butt) and identity vocabulary are deliberately NOT blocked — an over-eager filter is recoverable, a leak is not.
- **The optional social tab is called SOCIAL, not Community (decided).** `CurioBottomNavItems.Social` labels the bar's optional tab `Social` and draws `CurioIcons.Public` (a globe — verified present in the bundled subset as glyph 26; `hub`'s connected dots read as "a feature", not "people"). The ROUTE stays `CurioRoutes.COMMUNITY` and the files stay `features/community/*` — renaming a route is saveable churn for no user-visible gain. Copy that a member can READ says "Social" (the Settings → Online mode switch, the hub row, the notifications switch, the empty states); internal identifiers keep their `community` names.
- **REALTIME drives the live surfaces; the timers are only a safety net (`data/supabase/SupabaseRealtime.kt`).** One hand-rolled phoenix-protocol WebSocket over the OkHttp client the app already ships (NO realtime SDK, per this package's dependency contract): `watch(owner, token, watches, onChange)` opens a single `realtime:curio` channel, sends `phx_join` with `postgres_changes` bindings plus the access token (that token is what makes the channel RLS-aware — never drop it), heartbeats every 25s, reconnects with backoff on transport loss, and REFUSES to loop on a join the server rejected (RLS, or a table missing from the `supabase_realtime` publication) — it records `lastProblem` and stops until a new token arrives or a screen changes its bindings. A `phx_close` (the server closing the CHANNEL, usually an expired token) and a `phx_error` are both treated as a LOST link — without the `phx_close` branch `isLinked` stays true while nothing arrives, which is precisely "realtime looks connected but nothing is live". **any change to the UNION of every owner's bindings re-joins** (`bindingSignature()` — a screen that re-declares a different set, AND a screen that had never watched before: the DM thread used to add its bindings after the inbox had already opened the socket, `connect()` returned early, and the open conversation heard nothing at all). Direct messages ride TWO owners on purpose — `dm:<userId>` for the message bindings (the only ones that can mean "a row you hold was revised", which is what the revision sweep is for) and `dm:<userId>:live` for the typing/reaction bindings, so a peer's keystroke does not buy a full page read. The token is kept fresh by `OnlineAccount.keepSessionFresh` (refresh ~5 min before the JWT `exp`, retried rather than signing out) — a new token flows into `watch()` from the screens' own effects and rebuilds the channel as the new RLS context. **A push is a HINT, never the data:** `onChange` fires a callback that refetches through the normal REST path, so RLS still decides what is visible and a tampered frame cannot inject a row. `isLinked` is read by each screen to choose its cadence: the fast fallback tick while the socket is down, and a slow safety tick once it is up. Owner keys are `dm:<userId>`, `inbox` and `wall`; always `unwatch` in `onDispose`, and `SupabaseRealtime.reset()` on sign-out (a channel authenticated by a dead token must not stay open). The schema side is §5g (publication membership + `replica identity full` on the tables whose filtered UPDATES/DELETES are matched against the old row — `dm_messages`, `friend_requests`, `dm_reactions`).
- **An OPEN conversation is LIVE.** `DirectMessageScreen` reacts to its own four SERVER-filtered bindings — `dm_messages` `sender=eq.<other>` INSERT for their line, `dm_messages` `recipient=eq.<other>` UPDATE for my read receipt, `dm_typing` `sender=eq.<other>` INSERT/UPDATE for their "typing…" row, and `dm_reactions` `user_id=eq.<other>` INSERT/UPDATE/DELETE for their reactions (the row's own primary key carries the reactor, which is what makes the filter match even on a delete) — by pulling `SocialApi.messagesSince(token, other, me, newestConfirmed)`: ONE small read covering every way a thread can move, because only a new message moves `created_at`. It asks after `created_at`, `edited_at` AND `read_at`, per DIRECTION (PostgREST takes one top-level `or=`, so the pair scope is repeated inside each branch), the window backs off a second for clock skew and is truncated to whole seconds (the value sits inside the logic tree, where a fractional dot would read as another separator), and the anchor ignores optimistic `local-` bubbles because those carry the phone's clock. `pullDelta` then merges the result IN PLACE (`applyMoved`: a changed row is replaced, an arrival appended) instead of re-reading the page, which is what makes edits and receipts land live; a full page read (`rereadPage`) is reserved for a realtime revision hint and for a delta the server refused (a DELETE has no stamp to find, and the thread must never depend on one query shape) — plus `refreshLiveBits` (one `isTyping` read and one whole reactions re-read for the visible ids; whole, because a REMOVED reaction has no row to merge from). An arrival clears the typing row. The message timer runs at `LIVE_TICK_MS` (**1.2s**, the whole mechanism when realtime is down) and backs off to `SAFETY_TICK_MS` (**4s**) once `SupabaseRealtime.isLinked` — 20s was indistinguishable from "it never updates, I have to reopen the chat"; the "is typing…" tick follows the same rule (`TYPING_TICK_MS` 1.5s, `TYPING_SAFETY_TICK_MS` 5s) and still has to clear a writer that vanished without deleting its row. The inbox (`ChatsScreen`) watches a message addressed to me (`INSERT`/`UPDATE`/`DELETE`, so an unsend or an edit cannot leave a stale last line) and refreshes the conversation list WITHOUT touching the loading state (`CHATS_TICK_MS` **5s**, safety 30s), while the contacts screen (`FriendsScreen`) watches my friend-request rows in BOTH directions — a request waiting on me, and my own ask being answered or cancelled (`FRIENDS_TICK_MS` 5s, safety 30s). The wall watches `community_cards` and refreshes quietly behind a 400ms settle window so a burst of posts is one rebuild. Never go back to "load once on entry" — that is the bug where a received message only appeared after leaving the screen. **The composer is MULTI-ROW (v385, user decision):** Enter BREAKS THE LINE (`singleLine = false`, `ImeAction.Default`, up to five rows, then the field scrolls inside its `heightIn(min = 52.dp, max = 140.dp)` pill) and the round button is the only way to send — `ImeAction.Send` turned the key people press at the end of a sentence into an accidental send. The quoted parent drawn inside an answer is ONE line tall and HUGS its words (`ReplyQuoteRow`: no `fillMaxWidth`, `widthIn(max = 208.dp)`): a fill-width child forces its parent to that width, so every reply balloon had been stretched to the full thread width around one small line of quoted text.
- **Reading a thread IS the read receipt, per ROW.** `markThreadRead` (entering a conversation, and every delta that brings an arrival) stamps `read_at` on the server FIRST and mirrors it locally only on success, paced by `READ_STAMP_RETRY_MS` so a failed stamp is retried without hammering a dead network; the inbox badge and the sender's second tick are both read from that one column, so a stamp that never happened left a conversation you had read flagged unread. Ticks are PER ROW (`receipt != null` on that bubble — never an inference from the newest row), and `SocialApi.parseMessages` reads `read_at` through `blankOrNull`: `optString` answers the STRING `"null"` for a JSON null, which became `0L` and made EVERY sent message draw a double tick the moment it was sent.
- **A send costs ONE request and shows the REAL row (v: instant sends).** `SocialApi.sendPlaintext` posts with `Prefer: return=representation` and returns the created `CurioDirectMessage`, so the optimistic bubble is replaced by the server's own row (real id, server clock, receipt column) with no page re-read — the old path re-read 200 rows plus reactions on every tap. A project that returns no representation keeps the `sentShadow` stand-in until a pull finds the row. On a TRANSPORT failure (`CommunityError.transport`, an IOException family) the send is reconciled instead of buried: `confirmDelivered` asks the delta once whether the row actually landed (a read timeout is OUR deadline, not the server's refusal) and, if it did, treats it as the success it was — that ambiguity is what made a slow send look lost until the chat was reopened. Rest reads stay `return=minimal`.
- **No server jargon on screen (`communityMessage`).** A stale session ("JWT expired", "invalid claim") and a missing server function ("Could not find the function public.curio_edit_dm_message") are CONDITIONS with an action, and both arrive as plain `IllegalStateException`s: they must be mapped BEFORE the pass-through of already-written text, or the member reads the mechanism instead of the fix. `SupabaseClient.executeBody` handles the first centrally — a 401 on a request that carried a token refreshes the session and re-sends it ONCE (`OnlineAccount.refreshForRetry`, blocking, on the REST layer's own IO thread: it reuses a token another call already minted, answers `Unreachable` when the device cannot reach the server, `Refused` when the session is genuinely over) — and the REST timeouts are deliberately generous (connect 15s, read/write 30s) because OkHttp's 10s default fired while a send was still on the wire.
- **Session tokens are sealed, never plain (`data/supabase/CurioSecureStore.kt` + `SupabaseSessionStore.kt`).** The access and refresh tokens are AES/GCM-encrypted under an Android Keystore key (fresh IV per write, base64 `iv:ciphertext` in `curio_secure_store` prefs). Never write a token to a plain pref, never log one, and never add a plaintext fallback: if a device cannot create the vault key, `save` stores NOTHING and the session lasts the running process. A pre-encryption session is migrated into the vault once and the plaintext copy deleted; both session files and the social cache blob stay out of cloud backup and device transfer.
- **Notifications are the app's own job (`SocialNotifications` + `SocialNotificationWatcher`, mounted in MainActivity).** There is no push service: while the app is alive the watcher polls the inbox (**8s**) and the wall (**30s**) and announces the DIFFERENCE from its previous tick (the first tick is a baseline, so launching Curio never notifies about mail that was already waiting). A THIRD loop (5 min) publishes the member's own presence through `SocialPresence.publish` — not a notification, and gated on Online Mode + a session alone; it no-ops whenever `AppPreferences.isActivityHidden` is on. Both loops are gated on `onlineMode && signedIn && AppPreferences.socialNotificationsState` (default ON, owned by the Notifications page's "Messages and community" row). A message notification carries the sender and their line and OPENS THAT THREAD (`PendingDirectMessageOpen`); a post notification opens the wall (`PendingCommunityOpen`). Neither can fire without the POST_NOTIFICATIONS grant.
- **`dm_typing` + `dm_reactions` are optional, additive schema.** `SocialApi.setTyping` / `isTyping` / `reactions` / `react` / `clearReaction` degrade quietly: `isTyping` answers false and reactions simply do not render when the tables are missing (a project that has not re-pasted `supabase/schema.sql`). `isTyping` keys off a SERVER-stamped `updated_at` (a `curio_stamp_typing` trigger, so a wrong phone clock cannot make someone look permanently typing) and treats a row older than `TYPING_FRESH_MS` as expired. Reaction `kind` is the EMOJI character itself (the previous build's icon names are mapped by `SocialReactions.emojiFor`, so old rows still render) — a reaction is never an upload, which is why the table needs no storage bucket and no renderer keeps a mapping table.
- **A name and a handle are two different things (decided):** the DISPLAY name (`profiles.display_name`, mirrored from `AppPreferences` on sign-in and on every Edit-profile save via `SocialApi.updateDisplayName`) LEADS everywhere social — the wall's author row, a reply, a member profile, a friend row and the conversation header — and the `@username` reads on the line BENEATH it (`CurioPerson.handleLabel`, `CommunityCard.authorHandleLabel`, `CommunityComment.authorHandleLabel`). The username stays the stable identity people find you by and how they search for you. Never print a handle where a display name exists, and never collapse the two back into one field. The BIO (`profiles.bio`, ≤160 chars, null = not written) is the member's own public line: `SocialApi.updateBio` mirrors the local Edit-profile Bio field on sign-in and on every save, it rides only the wide `PERSON_COLUMNS_PRIVACY` read, and `SocialProfileScreen` renders it under the name and renders NOTHING when it is blank — never a placeholder.
- **Social privacy is a real, server-enforced surface (`supabase/schema.sql` §5f + `features/settings/PrivacyScreen.kt`, route `CurioRoutes.SETTINGS_PRIVACY`).** Three member-owned decisions: `profiles.profile_visibility` (`public` | `friends` — the discoverable SELECT policy exposes the row to accepted friends only when it is `friends`, see `curio_are_friends`), `profiles.hide_activity` + `profiles.last_active_at` (hiding CLEARS the stamp in the same write through `SocialApi.updatePrivacy`, so a hidden member has nothing on the server to read — presence is a courtesy line, never a record, and `CurioPerson.presenceLabel` returns null for a hidden, unknown or stale stamp), and `member_blocks` (one row per block, `curio_is_blocked(a,b)` as a security-definer test). A block is enforced in the policies themselves — profiles, `community_cards`, `community_comments`, `community_reactions`, `friend_requests` and `dm_messages` in BOTH directions — and blocking lives on the member's profile page while the Privacy page lists who is blocked and lifts it. Every privacy switch writes its LOCAL pref first (so the choice holds offline) and mirrors to the profile row; the privacy columns are a SECOND opt-in select (`SocialApi.profile` → `PERSON_COLUMNS_PRIVACY`) that falls back to `PERSON_COLUMNS` when a project has not been re-pasted, because a missing column must never blank a profile. Never add a privacy column to `PERSON_COLUMNS` itself.

### UI
- **User design preferences (decided, durable):** light mode background/surface is **Soft Cream `#F7F0E4`** (deliberately less-white/creamy, not dark); the **category-tint background wash** is applied on the **Spin page, Topic Reveal, the Save/Capture screen, and the Cabinet (which uses the active filter chip's tint; "All" keeps the plain background)** — so every category-aware screen wears the same color story. The wash is **theme-aware via `CurioCategory.categoryBackgroundWash()`** (in `ui/theme/CategoryInk.kt`): deep accent at 20% over cream in light mode, but the light 300-level twin at ~16% over midnight in dark mode (deep accents look muddy on dark — amber turns brownish, teal grey-green).    Container steps are deepened so cards/sheets stay distinct on the cream surface. See `ui/theme/CurioColors.kt` + `CurioTheme.kt`.
- **No close buttons in bottom sheets (user):** bottom sheets dismiss by
  swipe-down/back only — NEVER put a cross/close icon in one (the category
  picker + album sheets are the model; the book/series notes sheets had
  theirs removed in v355).
- **No filler hint copy (user):** never annotate an obvious affordance with
  hint text — "tap a chapter to read its notes", "Rating hidden · tap the
  book to show it" and "keyless Google Books rating" are all gone; if a
  control is self-evident, add no caption.
- **Torn heroes on WIDE windows scroll away (user, tablet redesign verdict):** the tablet is the MOBILE design (torn heroes, phone layouts, single-column lists — the editorial "no tear" tablet redesign was fully reverted). The one wide-only deviation: a torn hero is **NOT sticky on wide windows** (>=600dp — landscape tablet). Every screen whose torn banner used to pin over its scrolling list (Settings family + ShareHub + Recents + Recycle Bin + Manage Categories + Quests + Support + Promo Mode + Updates + Outfit Shop + Pet Designer, plus Topic Database and Cabinet with their search/filter UI) now renders the hero as the **first item of the scroll content** on wide (`top = if (wide) 0.dp else SettingsHeroTotalHeight` + an `if (wide) item(key = "hero")` at the list head + the pinned overlay wrapped in `if (!wide)`), so the tear rides away with the rows and landscape regains the full viewport height. In-list heroes pass `glassBackdrop = null` (opaque pills — nothing pinned behind to refract), and wide disables the list's `layerBackdrop` + inner-pill liquid glass where the hero is inside the captured node (no self-sample). Phone paths are untouched (sticky torn hero + reserved `SettingsHeroTotalHeight`). Shared helpers: settings-family screens use `SettingsHeroHeader`; Topic Database/Cabinet keep their own heroes but follow the same placement rule. Pet Designer's floating studio toolbar stays pinned (it already floats above the scrolling hero).
- **M3 theme system (v185, Settings → Appearance):** ONE opt-in toggle, default OFF (the current Curio look is the default — nothing changes until it's on). **"Material theme"** (`AppPreferences.materialThemeState`) redoes the COLOR system per M3 guidelines: the whole `ColorScheme` becomes `materialColorScheme()` (dynamic Material You on Android 12+, seeded brand-coral baseline fallback — `ui/theme/MaterialColorSchemes.kt`), and the 36 lane accents collapse to **6 muted hue families** (`MaterialFamilies.kt`: every family resolves to tonal tones of its own hue — T40/T80 fills, on-fill ink, T45/T80 text ink; v198 removed the earlier rose→scheme.secondary / green→scheme.tertiary role branches that painted buttons/chips off-hue, see v198) — M3's multi-color guideline is restraint: neutral surfaces, ONE primary, muted accents, never a rainbow per lane. The category choke points (`themedAccent()`, `categoryInk()`, `onAccent()`, `headerAccent()`, `categoryBackgroundWash()` → neutral background, `categorySurface()/categoryChipSurface()` → neutral containers, `CurioGradients.cardGradient/heroBlendGradient`, `CurioMixedDeck.*`) all gate on `materialThemeOn` so every screen repaints. The v185 **"Material guidelines" + "Material chrome"** options (M3 typography/shapes/spacing, the M3 `NavigationBar` swap, the Changa One drop from nav labels) were REMOVED (user verdict: not good) — `MaterialGuidelines.kt`, the `materialGuidelinesState` / `materialChromeFullState` prefs and their Appearance rows are deleted; `CurioTheme` always uses `CurioTypography`/`CurioShapes` and `CurioBottomNav` always renders the floating pill bar with Changa One labels. v190 refinements: material card fills are pastel-aware; mixed decks collapse to the scheme primary; light-mode heroes wear the rich family banner with dark ink (`materialHeaderAccent` light + `materialHeroInk`); the nav chrome uses pure M3 roles under Material (surfaceContainer + secondaryContainer indicator). The v78-era AMOLED/Material STYLES are long gone — do not resurrect them; the v185 toggle is the only Material system.
- **Always-on companions & onboarding setup (v23):** the floating pet, the pet brain, and auto-open landed topic have NO Settings toggles — they are always on (their Appearance toggles were removed; the `AppPreferences` APIs remain, defaults ON). Custom reaction lines are permanently off (no toggle; the reactions editor is unreachable). The explore-bubble opt-in row in the Explore dialog is hidden by default — a Notifications toggle (`AppPreferences.showBubbleOptInDialogState`) re-shows it as a single text line (no subtext). Onboarding includes a dedicated Search step that picks the explore search engine (`AppPreferences.searchEngineState`; changeable anytime in Settings) and the bubble opt-in row inside the "Display over other apps" permission card.
- **3D shuffle button (v24):** always on by default — its toggle was removed from Settings → Experiments → Deck & controls (the `threeDButtonState` pref API stays, default true; SpinScreen reads it unchanged).
- **Closed experiments (v24) — hardcoded OFF:** dual-accent hero gradient (ugly golden blend), deck card shadows (weird look while cards animate), tail-fade peek motion, and Smart Spin layout (always natural deck sizing) had their toggles removed from Experiments and their reads in SpinScreen/TopicRevealScreen hardcoded to false. The Layout & input section was removed from Experiments (Voice-to-text still lives in Settings → Recording; Smart density keeps its stored pref but has no UI).
- **Version five-tap (v24):** the Version row in Support & diagnostics opens the **Experiments** screen (kept open) — it no longer toggles promo mode. Promo mode stays OFF by default and is reached from Settings → Experiments → Promo mode; PromoModeScreen's own toggle is the one control.
- **Capture studio (v3xx52, Settings → Experiments → Capture → "Take studio"; v406 default ON):** the Save-your-take page's second SHELL, in `features/capture/CaptureStudio.kt`. It is presentation only — `SaveCaptureScreen` keeps the state, the save pipeline and every dialog, and composes either the classic chrome or the studio (`AppPreferences.captureStudioState`): the studio draws its own top bar, a tinted topic hero (medallion + topic + session duration + an inline `MoodChipsRow`), a canvas that hosts the SAME `FormatBodyForCategory` / `TagEditorRow` / `SessionNoteFloatingPill` the classic page uses, a take rail + tools/save dock on the bottom tray, and a `CaptureToolsSheet` (format grid + mood + tags). The recording pulse reads the take's existing `CaptureSectionState.busy`. Never fork the save rules into the studio — pass the classic page's own guards as callbacks (that is why the studio's params are a callback list, not a second implementation), and keep the paper notes untouched.
- **Passed experiments (v25) — hardcoded ON:** the **Enhanced main gradient** and **Pastel crown depth** experiments PASSED — always ON. Their toggles were removed from Experiments (Spin visuals → Main card / the Deck & controls card is gone entirely) and the reads are hardcoded `true` in SpinScreen (hero gradient + pastel top crown), TopicRevealScreen (hero gradient) and CurioColors (pastel card crown). The `heroGradientState` / `pastelCrownDepthState` pref APIs stay dormant, default true.
- **Settings declutter (v25):** all card HEADER lines in Settings were removed per request — the hub cards ("How Curio feels", "Experiments", "Your data": `headerIcon/Title/Subtitle = null` in `SettingsSections`) and the sub-page `CurioCardHeader(...)` lines (Visual language / Notifications / Recording / Backup & restore / FieldMind archive / Main card / Deck peek cards / Promo mode). Rows render directly under their `CurioSectionLabel`; that shared label component was also bumped labelMedium → titleSmall so section labels read larger everywhere (Settings, Support, Experiments).
- **Deck round-trip pin (v25):** tapping the front card on Spin now pins that topic as the landed topic (`landedTopicName`), so returning from Topic Reveal re-deals the hand centered on the SAME card — previously the NavHost disposed Spin while Reveal was open and the idle deck (no landed topic) re-dealt a different random front card on back.
- **Browse-mode Explore = real session (v25):** the Explore button on the browse-mode (Topic Database) reveal now runs the REAL explore flow (dialog → session/timer/recents/done-mark) instead of the v8.12 silent out-of-app search. `openSilentExplore` (data/ExploreSession.kt) was deleted, and `latestOnSilentExplore` / the `onSilentExplore` param in `RevealActionRow` were removed.
- **v26 — Topic Browser & Recents:** the Topic Database (`features/database/TopicDatabaseScreen.kt`) DEFAULT sort is now **A–Z within each category** (`.sortedBy { nameKey }` in the DEFAULT branch, section headers kept; the A–Z chip still flattens globally). `CurioVerticalScrollIndicator` (`ui/components/CurioScrollIndicator.kt`) gained **speed-scroll** — dragging the knob ramps rate on cumulative travel (`speed = 1 + (|cum|/160).coerceAtMost(3)`, per-event cap 240px; reversing decays it) — plus an optional **A–Z fast-scroller**: tapping the knob toggles a 26dp letter rail (strip animates 28→54dp, knob stays in a fixed 28dp TopStart strip with TopEnd content alignment), `activeAlphabetIndex` highlights the letter at the top row, tapping a letter fires `onAlphabetSelect` (the browser scrolls to the first matching topic). Only the Topic Database passes `alphabet`; the other 8 indicator users are unchanged. The Recents page (`features/recent/RecentScreen.kt`) header was rebuilt in the settings-family torn-rose hero (`SettingsHeroHeader` + `SettingsHeroTotalHeight` + `ScreenEntrance`), replacing the plain back-button row — feed scrolls under the tear, indicator padded below the hero, empty state padded down. **Drag-gesture gotcha (v26):** the knob's `pointerInput` keys on `(state, hitHeightPx > 0f, alphabet != null)`; tap-vs-drag is distinguished by total travel < 24px (TapThresholdPx) — never add a second `detectTapGestures` pointerInput to the same target (the drag detector owns the strip).
- **v26 — Recycle bin + sort dropdowns:** deleting a saved capture (Cabinet bulk or Entry Detail) now runs **double confirmation** (`ui/components/CurioTwoStepDialog.kt` — step 1 "Move to Recycle bin?" → step 2 final Delete, `step` resets on dismiss) and **soft-deletes** instead of erasing: `CaptureEntity.deletedAt` (nullable, Room v4→v5 `MIGRATION_4_5` ALTER TABLE), DAO live queries filter `deletedAt IS NULL`, new `softDeleteById(s)`/`getTrashedFlow`/`getTrashedById`/`restoreById`/`restoreAll`/`purgeById`/`purgeTrashed`/`countTrashed`, repository wrappers, `CurioEntry.deletedAt` (defaulted) threaded through `CaptureEntity.toEntry()`. Soft delete KEEPS audio/images; only the Recycle bin's permanent purge (`features/recyclebin/RecycleBinScreen.kt`, route `RECYCLE_BIN`, Settings → Safety & support row, pop-screen registered) removes media — Restore / Delete forever / Empty bin. `deleteById(s)` stays HARD delete (FieldMind import cleanup + purge). Sort controls are now the shared `ui/components/CurioSortDropdown.kt` (label zone opens the field dropdown, trailing arrow zone toggles ascending/descending universally): Cabinet (`cabinetSortField` DATE/TITLE/CATEGORY + `sortAscending`, replacing `sortNewestFirst`) and Topic Database (`tdSortField` DEFAULT/NAME/YEAR + `sortAscending` mapping to the existing `DatabaseSortMode`; the old `DatabaseSortChip` row is gone). Review gotcha: a top-level theme extension like `categorySurface()` must be imported per file — RecycleBinScreen's first draft missed it (compile error the reviewer caught); `options.first()` in the dropdown now null-safe.
- **v26 — Settings & picks cleanup:** the "Card & deck experiments" row is GONE from the Settings hub — Experiments opens only via the five-tap version trick in Support & diagnostics; Manage categories + Topic history moved into the Personalize section (the old Explore section was deleted). Manage Categories (`features/managecategories/ManageCategoriesScreen.kt`) gained long-press drag-to-reorder: the ⋮ handle uses `detectDragGesturesAfterLongPress`, a draft `List` state + row-step `dragAccum` swaps, `Modifier.animateItem()`, persisted on release (`setCategoryOrder`) — plus a "Reset order" TextButton that restores `CurioCategories.all` order (hidden flags untouched; the old `moveCategory` helper was deleted). The Spin category sheet's "Browse all categories" link is now "Manage categories" → navigates `MANAGE_CATEGORIES`. The "What are we exploring?" sheet (SpinScreen `CategoryPickerSheet`) AND the full-screen picker (`features/picker/CategoryPickerScreen.kt`) seed `multiSelectMode` + `selectedSlugs` from the persisted `getLastSpinCategories` set (`persistedVisible`, hidden lanes filtered): a saved MIX reopens in multi-select with every lane pre-ticked so it can be reviewed and changed. Saved voice-note titles (detail `SoundBiteRender`) now render on their own `NotePaperCard` slip (`titleStyle`/`titleColor`/`noteSeed(entry.id, 30)`), hoisted OUTSIDE the `audioFilePath` gate so typed-only saves show them too.
- **v3xx — NEW category picker ("Category Mix Studio") is the DEFAULT;
  classic picker is a toggle.** The old glass-pill picker
  (`CategoryPickerScreen` / `CategoryPickerContent` / `PickerIconTile` /
  `PickerPageTab` / `PickerPresetChip`) is untouched but demoted behind
  `AppPreferences.classicPickerEnabledState` (OFF = new unless enabled;
  toggle lives in the visible Settings → Experiments screen under a
  "Category picker" section — "Classic category picker"). New pieces (premium-minimal style, NO glow pills / NO
  saturated fills; hairline liquid-glass edge everywhere, real
  `liquidGlassCapsule` on action capsules when the Liquid glass experiment
  is on):
  - `NewCategoryPickerSheet` (Spin page's inline sheet + the PICKER
    route's quick sheet): header "Pick your mix", Pinned row (long-press
    to unpin, tap to spin that lane), Your mixes list (saved named mixes
    with Spin/Spinning, long-press to delete), Now spinning deck summary,
    bottom action row = **Surprise me** (shuffles a 4-6 lane random
    mini-mix, ~1-in-4 chance of the full Wildcard surprise) + **+** (mix
    editor) + **Browse** (opens the full page).
  - `CategoryPickerBrowseScreen` (the PICKER route, full screen): top bar
    + in-page bottom nav with three tabs — **Browse** (all-lanes grid, tap
    to spin, long-press to pin), **Mixes** (create/apply/edit/delete named
    mixes), **Pins** (pinned lanes, unpin / tap to spin).
  - `MixEditorSheet` (nested bottom sheet): name field + multi-select
    grid; saving also applies the mix to the deck (re-deal happens on
    Spin). Edit/rename keeps the mix's stable `createdAtMillis` id.
  - Persistence: `NamedMix(name, laneIds, createdAtMillis)` in
    AppPreferences (`savedMixesState` + get/save/addOrReplace/delete,
    JSON array); pinned lanes reuse the existing
    `getPinnedCategories`/`togglePinnedCategory` (defaults Wildcard +
    Artists/Films/Books/Scientists); starter mixes seeded ONCE from the
    old `deckPresets` (Science/Entertainment/Arts & Stories/History &
    Ideas) via `seedStarterMixes` + `pickerMixesSeededState`.
  - **Icon font re-subset:** `material_symbols_outlined.ttf` regenerated
    from `tools/fonts/material_symbols_outlined_full.ttf` adding ligature
    glyphs `shuffle`, `grid_view`, `apps` (
    --text-file + --unicodes-file merge of the old subset's rlig ligature
    names + cmap codepoints; verified 0 lost codepoints and 0 lost rlig
    names: 250→253 cps, 277→280 names). New constants in CurioIcons:
    Shuffle / GridView / Apps / PushPin.
- **v3xx2 — category picker overhaul (neutral + tap-hold options +
  pager).** The new picker drops category accents everywhere and adds a
  classic/new pager:
  - **NEUTRAL tiles:** `NewPickerTile` uses `surfaceContainerHigh` fills,
    `onSurfaceVariant` icons, and a `secondaryContainer` selected fill +
    `primary` check — NO category accent for fills, borders or icons in
    EITHER theme. The same neutral treatment applies to `BrowseMixRow`,
    the Pins tab rows, the Mixes 3-dot menu, and the in-page bottom-nav
    capsules (`surfaceContainerHigh` idle / `secondaryContainer` selected,
    hairline `curioGlassEdge` preserved — NEVER fully transparent, even in
    liquid-glass mode).
  - **Tap-and-hold → option pill:** holding a category (Browse grid,
    Pinned row, Pins tab) surfaces a centered overlay with **Pin/Unpin** +
    **Spin** instead of pinning directly. The pill is a SOLID surface (no
    glass transparency). Pinned pills are taller/wider (12dp vertical
    padding).
  - **No close button:** the sheet has no cross — swipe down or back to
    close.
  - **HorizontalPager:** page 0 = a self-contained classic-style
    multi-select grid (preset chips + Mix/Cancel row, neutral tiles),
    page 1 = the new picker. The user's chosen page persists as the
    default (`pickerDefaultPageState`, default 0 = classic). Both pages
    share the bottom action row (Surprise me · Create mix · Browse).
  - **Your mixes:** a 2-column grid, max 5 visible + Show all/Show less.
  - **Continue exploring** replaces "Now spinning": the user's most-spun
    categories (`CurioPassport.allProgress` spin counts) first, then
    curated "fun to explore" lanes up to 10. The user can add/remove via a
    tap-and-hold → remove and an "+ Add" tile → `AddSuggestionSheet`
    (`pickerSuggestionsState`, falls back to `defaultSuggestions`).
  - **Mix editor** shows a CLEAR neutral selected state on tiles
    (`secondaryContainer` + `primary` check).
  - **Back from Browse** (PICKER route) re-opens the Spin picker sheet via
    `SpinPickerRequest.pending = true` (the Browse back button sets it;
    SpinScreen's existing consumer reopens the sheet).
  - **Mixes 3-dot** offers both Edit and Delete (was edit-only).
  - Persistence: `pickerDefaultPageState` (Int) + `pickerSuggestionsState`
    (List<CategoryId>) + `getPickerSuggestions`/`setPickerSuggestions`/
    `addPickerSuggestion`/`removePickerSuggestion` + `defaultSuggestions`.
- **v3xx3 — signature share cards REDESIGNED for 7 categories (both normal
  and Deepen).** The classic base styles (Paper/Clean/Collage/Editorial/
  Minimal/Vinyl) and the `signatureDesignClassic` fallback are untouched;
  the 7 categories the user called out (Animals, Animated Films, Anime,
  Artists, Artworks, Astronomy, Authors) get fresh typographic identities
  and cleaner minimal scenes in BOTH `signatureDesign` and
  `signatureDesignDetailed`:
  - **Artists** — warm concert-poster: single amber spotlight cone on a
    dark stage, hairline floor + glow pools, tall **Bebas Neue** poster
    title (POSTER layout; detailed adds a light rig, crowd silhouettes,
    sound-wave arcs, sparkle dust).
  - **Animals** — naturalist field note: sage botanical sprig + seed dots
    on cool sage paper, **Lora** serif title (CENTERED; detailed adds a
    forest clearing with sprig cluster, paw-print trail, grass, fireflies).
  - **Animated Films/Movies** — storybook pastel: thin five-band rainbow
    arc + placed sparkles on a soft lavender wash, **Corben** rounded
    display title (CENTERED; detailed adds a film-frame cel with sprocket
    dots, cloud puffs, confetti).
  - **Anime** — rising-sun poster: vermilion sun + ground line + brush
    stroke on cool paper white, **Maven Pro** title (CENTERED; detailed
    adds sun rays, a torii-gate silhouette, falling sakura petals).
  - **Artworks** — quiet gallery: one thin framed abstract + floor
    hairline on cool gallery white, **Cormorant Garamond** title
    (STANDARD; detailed adds two spotlight cones + a second ink-line
    piece).
  - **Authors** — literary manuscript: faint ruled lines + red margin +
    flourish on cool manuscript paper, **Playfair Display** title
    (STANDARD; detailed adds inkwell + quill under a desk-lamp glow).
  - **Astronomy** — star chart: constellation + thin ringed planet +
    coordinate ticks on deep navy, **Space Mono** title (BOTTOM; detailed
    adds nebula glows, sparse starfield, shooting star).
  - The old dark scenes for these categories (indigo spotlight, moonlit
    forest, rainbow arc, sakura, gallery wall, writing desk, nebula
    galaxy) were replaced outright; `signatureDesignClassic` keeps the
    legacy family designs. Note the detailed Animated-Films branch now
    matches `"ANIMATED FILMS" || "ANIMATED MOVIES"` (the old branch keyed
    only on the legacy name and fell through to the fallback).
- **v3xx4 — picker crash fix, mixes grid polish, Editorial overlap fix,
  no-cream signature backgrounds.** Follow-up to v3xx3 (user report):
  - **Crash fix:** opening the category picker crashed with "Vertically
    scrollable component was measured with an infinity maximum height
    constraints" — the classic/new `HorizontalPager` in
    `NewCategoryPickerSheet` and the `MixEditorSheet` category grid used
    `Modifier.weight(1f, fill = false)`, which measures the child with an
    INFINITE max height; the pages' LazyColumn / LazyVerticalGrid passed
    it through and crashed. Both now use `weight(1f)` (fill = true) so
    the sheet's bounded height reaches the scrollables.
  - **Your mixes grid:** `NewMixCard` cells are a uniform 122dp height
    with the Spin pill bottom-anchored (was ragged per-teaser heights),
    and the 3-dot menu is an M3 `DropdownMenu` popup (always on top)
    instead of the inline `DropdownMenuSurface`/AnimatedVisibility that
    shoved the row when expanded — that surface + the now-unused
    animation imports were deleted.
  - **Editorial overlap fix:** the drop-cap body in `EditorialCard`
    rendered its wrap-row + full-width rest text as siblings in a
    `BoxWithConstraints`, which STACKS children at the same slot — the
    rest text drew ON TOP of the wrapped block ("quick fact text
    overlapping itself"). The pair now lives inside a `Column` so they
    lay out top-to-bottom.
  - **No-cream signature backgrounds:** per user direction, the v3xx3
    signature scenes dropped their warm cream/beige fills for cool
    paper-white tones (Animals `EFF3F0` sage paper, Anime `F5F6F8` cool
    white, Artworks `ECEFF2` gallery white, Authors `F1F3F6` manuscript
    paper) in BOTH normal and Deepen. Classic styles (Paper/Editorial/
    Minimal and `signatureDesignClassic`) keep their cream — the user
    likes those.
- **v3xx5 — signature scenes SCRAPPED for 14 categories → quiet minimal
  hairline treatment + 7 new fonts.** Per user direction ("use editorial
  minimal for quality, redesign without so many things on the background"),
  the scene-heavy signature backgrounds (lamp+bookshelf, marquee lights,
  concert hall, forest clearing, nebula, etc.) were removed ENTIRELY for
  the 14 redesign categories — the first 7 (Animals, Animated Films,
  Anime, Artists, Artworks, Astronomy, Authors) and the second 7
  (Biology, Books, Chemistry, Directors, Discoveries, Economics, Films):
  - **Minimal treatment (normal + Deepen):** flat vertical gradient +
    `signatureHairlineFrame` (new DrawScope helper: inset 4.5% rounded-
    rect outline, 1f stroke) + ONE tiny category crest top-right
    (spotlight / paw / star / sun / frame / quill / helix / open-book /
    hexagon / clapperboard / compass / arrow / film-strip). Deepen keeps
    the same layout and only adds a soft radial accent glow — no extra
    objects.
  - **Second batch fonts (new OFL downloads):** BioRhyme (Biology),
    Fraunces (Books), Oxanium (Chemistry), Limelight (Directors),
    Rye (Discoveries), Space Grotesk (Economics), Anton (Films) — TTFs in
    `res/font/`, FontFamily vals in CurioTypography.kt, licenses in
    `app/third_party/`. The first batch re-paired existing bundled fonts
    (Bebas Neue, Lora, Corben, Maven Pro, Cormorant Garamond, Playfair
    Display, Space Mono).
  - **Normal == Deepen configs:** every one of the 14 categories now uses
    the SAME bg/gradient/font/colors/layout/badge in both functions (was
    drifting warm/cool hexes and sizes); the only difference is the glow.
  - `signatureDesignClassic` (2034–2630) is untouched; non-target
    categories (Food/Geology/History/Internet/Language/Manga/Manhwa/
    Mathematics/Mythology/Painters/Plants/Psychology/Quotes/Scientists/
    Albums/Songs/Series/Games/Sports/Technologies and topic variants)
    keep their existing scene designs.
- **v3xx8 — share-card stickers + link-share fix + sparkle repair rework +
  Book browser.** Per user request (combined batch):
  - **EMOJI STICKERS (full-screen editor only):** the full-screen editor's
    top bar gains a **Stickers** button (between Layout and Text; the
    AutoAwesome glyph) that opens an inline panel — a horizontal emoji
    picker (48 curated emojis in `stickerEmojis`) plus, when a sticker is
    selected, a size slider, **To front / To back** (z-order) and Delete.
    Stickers are `ShareSticker(emoji, x, y, sizeFrac)` — positions/sizes
    are card FRACTIONS so the preview, the sheet and the exported PNG all
    match. The card's sticker layer renders on TOP of every style inside
    `TopicShareCard` (new `stickers` param threaded through all 5 call
    sites in the sheet + exports); the interactive edit layer
    (`StickerEditOverlay`, drawn over the ArrangeableCard chrome only in
    full screen while the tool is open) makes each sticker tap-to-select /
    drag-to-move, clamped inside the card. List order = stacking order
    (later = on top). Persisted per topic via `saveShareCardEdits` (a
    `stickers` JSON array; restored on reopen) and cleared by Reset-all.
  - **LINK-SHARE FIX + Telegram hidden link:** the Share dialog's
    "Include a link" mode previously posted TEXT ONLY (caption + raw URL)
    — it now ALSO attaches the card PNG via `shareComposableCard` with
    `shareText`, so the picture is never lost. The link text uses
    Telegram's hidden-link syntax `[displayTopic](linkUrl)` (the URL hides
    behind the topic name, like `[1, 2, 3]` citation links).
  - **Sparkle repair rework (`autoLayoutPlan`/`runAutoLayout`):** the INFO
    rows (byline/author/year) now get a SIGNED `metaLift` — a grown/
    dragged fact covering them lifts the rows UP back between the title
    and the quick fact (negative lift) instead of dumping them below it;
    when the lifted rows have no room the TITLE moves up a little for them
    (`titleForMeta` from the measured title→meta gap). NEW out-of-card
    clamp: anything whose measured card-local rect hangs off the card
    (left/top < 0 or right/bottom > card) is pulled back inside on the
    same tap (never re-centred) via `fixTitleX/Y … fixFavX/Y` deltas
    applied to each element's own dx/dy. `autoLayoutPlan` gained
    `cardW`/`cardH` (the 280dp preview box).
  - **MoveHandle zIndex fix:** the edit-mode grip now sits at `zIndex(10f)`
    so it ALWAYS wins the touch — a selected box that overlaps its
    neighbours is drawn at zIndex 2 and used to steal the handle's drags
    ("the handle doesn't work when boxes overlap"); the tap/drag now
    always reaches the handle and moves the box.
  - **Book browser:** the Book covers & ratings hub's horizontal
    "All covers" LazyRow strip (CoverTile) is REMOVED; a new
    **Book browser** screen (`features/settings/BookBrowserScreen.kt`,
    route `SETTINGS_BOOK_BROWSER`, registered in the NavHost) lists every
    book as a scrollable line-by-line row — cover thumbnail, name,
    author · year, cached ★ rating + count, chevron — tapping opens the
    book's reveal. Reached from a new "Book browser" row under
    Experiments → Content tools, next to "Book covers & ratings".
- **v3xx9 — favorites fixes + collage polaroid rework + sticker pinch.**
  Per user request:
  - **SIGNATURE favorites ink + placement:** the plain-type strip on
    Signature now wears the DESIGN's own body ink (`sigFavInk` —
    `signatureDesign(...).bodyColor` computed in the shared favorites
    block and threaded via a new `inkOverride` param on
    `FavoriteTracksBadge`/`BoxedFavStrip`), so the tone's palette.ink can
    never clash with a signature scene (near-black ink on Mario's red was
    unreadable). The Signature slot ALSO moved from the bottom corner
    (where it overlapped the bottom-anchored quick fact) UP to just below
    the title/author block (TopStart 138/130dp). Custom keeps its bottom
    pocket.
  - **COLLAGE favorites:** raised a little more (112/98dp) so the strip
    parks just under the title/author rows fully on the cream top paper
    (clear of the tear seam), with stronger alphas (label 0.66, body
    0.92, heart 0.95).
  - **Favorites collision direction fix:** `bottomOverlap(upper, lower)`
    ASSUMES the first box is above the second — the old fav/fact calls
    fed boxes in the wrong order and produced giant false positives that
    shoved the strip/fact around on sparkle taps. New `pokeAbove` guard
    (order-checked) + `favOverFact` now pushes the FACT down when a
    top-placed strip (Collage/Signature) grows into it, and the fav-only
    lift is order-guarded so the bottom-corner styles (Paper/Vinyl)
    never false-trigger.
  - **Collage center watermark removed:** `Watermark` gained `center =
    false` and the Collage card passes it — the 80dp category glyph that
    floated in the middle of the card is gone (corner set stays).
  - **POLAROID rework (Collage):** the instant-print is now a movable /
    scalable element — new `ShareCardResizeTarget.POLAROID` + `onPolaroid`
    bounds callback + selectable box + MoveHandle grip in the
    ArrangeableCard chrome (mirrors cover/fav). New move fields
    `polaroidDx/Dy/Scale/Style/Filter` (persisted in the per-style move
    JSON; Reset layout clears position+scale, keeps style/filter). The
    frame ADAPTS to the photo's aspect (landscape print = wide/short,
    portrait = tall, capped at 46% of card height; no photo = classic
    1.18 print), the tape PEERS out past the white frame (drawn last, so
    it sits ON the photo), and 5 STYLES (`PolaroidLook`: Classic · Retro
    · Sunglow · Vintage · Dashed — frame/tape/tilt/finish, Dashed wears a
    dotted hairline) + 5 PHOTO FILTERS (None · Noise grain · Nostalgia
    sepia · B&W · Warm with overlays/vignette via `sepiaMatrix` /
    `grayscaleMatrix` / `warmMatrix`) are picked from a new full-screen
    **Polaroid** button (Collage card only) panel, plus a Print-size
    slider (also wired into the sheet's Box tool when the polaroid is
    selected).
  - **Sticker pinch-to-resize:** `StickerEditOverlay` now uses ONE
    `detectTransformGestures` recognizer per sticker — tap selects, drag
    moves, and a two-finger PINCH scales the emoji (new `onResize`
    wiring clamps 0.08–0.6 width-fraction; the Size slider stays for fine
    control). Hint texts updated.
- **v3xx10 — sticker imports + rotation, bottom tool panels, icon-only
  toolbar, sparkle info-row snap, fav tweaks.** Per user request:
  - **IMPORT PNG cutouts:** the sticker panel gains an **Import PNG
    cutout** button (`stickerPickerLauncher`, GetContent) — the picked
    image is re-encoded to PNG under `context.filesDir/stickers` via
    `importStickerPng` (transparency preserved) and dropped on the card
    as an image sticker. `ShareSticker` gained `imagePath: String?` and
    `rotation: Float`; the card layer + `StickerEditOverlay` render an
    image sticker as an aspect-preserving bitmap (width = sizeFrac ×
    card width, decoded once per path through `decodeStickerBitmap` + a
    `ConcurrentHashMap` cache, downscaled to ≤1024px) instead of the
    emoji glyph. Both fields persist in the stickers JSON.
  - **STICKER ROTATION:** a Rotation slider (−180°..180°, 0° reset pill)
    in the panel AND two-finger twist on the card (the transform
    recognizer's rotation delta, normalized via `normDegrees`).
  - **Tool panels at the BOTTOM:** the full-screen editor's Text /
    Stickers / Polaroid panels moved from under the top bar to BELOW the
    card (bottom of the dialog Column) — the tools sit under the thumb.
  - **Icon-only toolbar:** `ToolWithCaption` no longer renders its tiny
    caption text — the edit toolbar is pure icon pills (Text · Size ·
    Crop · Fit · Font · Color · Adjust · Align · Format · Content have
    no text under the icons).
  - **Sparkle info-row SNAP:** the meta collision logic is replaced by a
    snap-to-title: on the sparkle tap the info rows lift so their top
    meets the title's bottom (gap ≤ 2dp), guaranteeing they sit BETWEEN
    the title and the quick fact and TOUCHING the title — no more
    author/year below the fact. `runAutoLayout`'s negative meta travel
    widened (−240dp) so one tap brings a far-drifted strip all the way
    up.
  - **Fav List/Rows icons fixed:** the raw `view_agenda` / `view_module`
    strings aren't in the bundled icon subset (they rendered as literal
    text); swapped to the verified `drag_handle` (List) and `grid_view`
    (Rows) glyphs.
  -    **Fav auto-crop:** selecting the favorites strip (FAVTRACKS) now
    auto-opens the Crop (box) tool in the sheet's edit toolbar.
- **v3xx11 — CI compile fixes + icon-glyph repairs.** The v3xx10 push broke
  CI in three spots: the polaroid photo-filter matrices were raw
  `FloatArray`s (now wrapped in `androidx.compose.ui.graphics.ColorMatrix`),
  a `photoH.toPx()` call on a Float (now `photoH.dp.toPx()`), and
  `detectTransformGestures` was referenced fully-qualified without an
  import (now imported + called unqualified). Icon fixes: the Underline
  pill's glyph name `format_underline` doesn't exist in the Material
  Symbols catalog (it rendered as literal text) — `CurioIcons.Format-
  Underline` now points at the real `format_underlined`, and the bundled
  icon font was re-subset (pyftsubset, layout-features=rlig) to ADD
  `format_underlined` + `link` (the share-link-preview icon) with ZERO
  icons lost (verified by ligature-set diff).
- **v3xx12 — TEXT HISTORY (global, persistent, survives reset).** Per user
  request: every text edit in the share-card editor family now feeds a
  GLOBAL text-history feed that is never cleared by Reset layout, field
  switches, or leaving the topic. New file `ui/components/TextHistory.kt`:
  - `TextHistoryStore` — SharedPreferences-backed JSON (`curio_text_history`,
    cap 300, dedupe consecutive repeats + blanks) with `record / snapshot /
    setPinned / delete / clearAll`.
  - `rememberTextHistoryCapture(ctx, field, text, resetKey)` — captures when
    typing PAUSES (~1.3s debounce cancelled by the next keystroke), at every
    10th word boundary (immediate), and on editor dispose (final state);
    resetKey (topic·activeId) resets memory across cards/fields so content
    switching never snapshots itself.
  - `TextHistoryPill` + `TextHistoryBrowser` — the same corner pill and
    centered overlay everywhere: sheet tools column (top-right), the
    full-screen editor's top bar and the Enlarge writing sheet. The browser
    lists newest-first with pinned floats, per-entry field label + time
    (Just now / Xm / Xh / d MMM · HH:mm), preview (tap = full selectable
    view), and Pin / Copy / Restore-into-active-field / Delete actions with
    a two-tap Clear.
  - TopicShareSheet feeds the FACT text (label by activeId: Custom fact /
    Chapter review / Reading progress / Quote / Quick fact) + the polaroid
    photo caption; Restore routes through `routeFactChange`.
- **v3xx13 — full-screen editor polish: icon-only top bar, polaroid cut +
  size fixes, sticker deselect + smooth panels.** Per user request ("icon
  only … overlapping … polaroid gets cut … size adjuster no length …
  stickers tapping outside … smooth to open and close"):
  - **ICON-ONLY top bar:** the full-screen editor's Close · Aspect · Layout ·
    Stickers · Polaroid · Text buttons dropped their text labels (Close /
    ratio / Layout / Stickers / Polaroid / Text) for compact 40dp circle
    icons (descriptions ride the icon semantics) and the right cluster is
    now `horizontalScroll`-able — the pills no longer crowd/overlap on
    narrow screens. (Tool panels already live BELOW the card.)
  - **Polaroid no longer "cut" by its outline:** the editor's POLAROID
    selection border used to draw tight on the reported frame rect, slicing
    the tilted corners and the tape that peers past the top. The tap box
    now carries no border and the SELECTED outline floats OUTSIDE the print
    (8dp padded rounded rect).
  - **Print-size slider has real travel:** the render clamp capped the print
    at 36% of card width (dead past scale ≈ 1.06); widened to 44% (and the
    tall cap 46% → 55% of card height), so the whole slider range actually
    grows the print.
  - **Sticker tap-outside deselect:** while the sticker tool is open and a
    sticker is selected, tapping the card outside every sticker deselects it
    (a full-size tap layer sits BOTTOM-most inside `StickerEditOverlay`, so
    sibling hit-testing still routes sticker taps to the sticker; with
    nothing selected the layer stays transparent to the card chrome).
  - **Smooth panels:** the Text / Stickers / Polaroid bottom panels now
    open/close with a fade + vertical expand (`AnimatedVisibility` +
    `fadeIn/expandVertically/fadeOut/shrinkVertically`) instead of popping
    instantly.
- **v3xx5b — picker crash fix (nested lazy grid).** The new picker STILL
  crashed on open (same "infinity maximum height" message) —
  `ContinueExploringSection` rendered a `LazyVerticalGrid` inside a
  `LazyColumn` item, and lazy items are measured with infinite max
  height. Replaced with manual chunked rows (3/4 cols via `chunked`,
  `NewPickerTile`/`AddSuggestionTile` gained a `modifier` param,
  trailing `Spacer(weight(1f))` pads short rows). The grid-less rows are
  fine because the section caps at ≤11 tiles.
- **v3xx6 — first-7 signature watermarks: tiny drawn crests → unique
  icon glyphs + LANGUAGE bubbles scrapped.** Per user direction ("use
  letter or symbol or icons instead of drawing things… don't just use a
  letter in every design, be unique and creative per category"), the
  tiny drawn crests (spotlight / paw / star / sun / frame / quill) on
  the FIRST-7 categories (Animals, Animated Films, Anime, Artists,
  Artworks, Astronomy, Authors) are replaced by a giant faint
  Material-Symbols glyph watermark rendered over the background
  (bottom-right, −6° tilt) — one UNIQUE icon per category, all verified
  ligatures in the bundled font subset: brush (Artists), pets (Animals),
  movie_filter (Animated Films), auto_awesome (Anime), museum
  (Artworks), edit_note (Authors), nightlight (Astronomy). Implemented
  as `SignatureDesign.watermark: String?` — a Minimal-style
  giant-faint-glyph watermark, rendered by `SignatureCard` via
  `CurioIcon` (not a drawn letter, so no two categories collide). The
  same icon renders in BOTH normal and Deepen. LANGUAGE (normal +
  Deepen) drops its chat bubbles / calligraphy strokes entirely for a
  minimal gradient + hairline frame — its many-language words overlay
  (言語/Sprache/langue/…) already renders at the composable level, so
  that IS the background decoration now. Album and all other categories
  untouched; classic untouched.
- **v3xx7 — category picker UX refinement (overrides parts of v3xx2):**
  user: "category picker … really bad user experience … holding to start a
  mix it selects 2 … remove that presets of science etc from page 1 and
  show the Curio Knowledge and Mix options … use the category tint style
  when selecting … less dark creamy in light mode … instead of 5 show 6
  your mixes … hold to remove remove that text, add tap and hold action …
  when going to add and selecting or unselecting things it doesnt update …
  when closing the picker and reopening thats when it updates, same in
  when creating a mix".
  - **Page 1 = Curio / Knowledge / Mix mode picker** (was preset chips +
    flat grid). `ClassicPickerPage` now hosts the same `PickerMode` tabs
    and grouped tap-to-open decks as the classic picker (the private
    `PickerGroup` / `curioModeGroups` / `knowledgeModeGroups` became
    internal to share them); the Science/Entertainment/Arts & Stories/
    History & Ideas preset chips are GONE from the new picker (the classic
    picker's `CategoryPickerContent` keeps them). Mix mode holds the
    multi-select grid + Mix/Cancel row.
  - **Clean start (kills "it auto-selects 2"):** page 1 opens with
    `multiSelectMode = false` and an EMPTY selection — the old seeding
    from `getLastSpinCategories` lit up the previous deck's lanes the
    moment you held a tile to start a mix (e.g. the persisted single lane
    + the held lane = 2). The v196 model (tap opens a lane; hold is the
    ONLY way into multi-select) is now applied to page 1 AND to the
    classic `CategoryPickerContent` (draft mid-session restore kept); the
    legacy Spin `CategoryPickerSheet` already had it. `CategoriesPicker-
    Draft` seeding changed accordingly.
  - **Selection = classic category tint:** `NewPickerTile` selected state
    is now `themedAccent()` fill + `onAccent()` ink (icon, label, check
    badge, accent-ink ring + icon-plate tint) — the classic `PickerIcon-
    Tile` style — instead of the neutral secondaryContainer. Applies
    everywhere selection renders (page-1 Mix, mix editor, Add sheets).
  - **Cream light mode:** new `newPickerIdleFill()` helper (classic
    cream-pill recipe: `lerp(base, curioPillLift(), 0.82f)` in light,
    unchanged in dark) is the idle fill for every new-picker tile/pill/
    panel — `NewPickerTile`, `NewPinnedPill`, `NewMixCard`, `AddSuggestion-
    Tile` (from surfaceContainerLow), `NewPickerCircle`, `NewSecondary-
    Outline`, the mixes "New" pill, `NewPickerTabCapsule`, `BrowseMixRow`,
    Pins rows — so the picker reads creamy in light mode, not dark tan.
  - **Your mixes: 6 visible** then "Show all" (was 5).
  - **Continue exploring:** the "hold to remove" hint text is REMOVED and
    holding a lane now opens a Remove pill (`CategoryOptionPill` gained an
    optional `onRemove` — errorContainer Remove action) that actually
    removes the lane live. The section reads `pickerSuggestionsState`
    REACTIVELY (no remember snapshot) so every change shows instantly.
  - **Live updates everywhere (stale-state fixes):** (1) `MixEditorSheet`
    selection is now an IMMUTABLE `Set<CategoryId>` — the old in-place
    `MutableSet` toggle wrote the SAME instance back, so structural
    equality never triggered recomposition and ticks only appeared when
    the editor reopened (the batch-show was the "auto-select 2" the user
    saw); (2) `AddSuggestionSheet` reads the reactive state and toggles
    against the EFFECTIVE list (defaults seeded), so adding AND unchecking
    work instantly; (3) `AppPreferences.removePickerSuggestion` seeds from
    the effective list so removing a default suggestion actually removes
    it (was a no-op against an empty user list); mix save/delete already
    recomposed via `savedMixesState`.
- **v3xx12 — Your-mixes cards refined (colour identity + explicit actions).** User:
  "the your mixes options can be much more refined". `NewMixCard` upgrades:
  (1) **Lead-lane cover** — the 42dp plate is tinted with the mix's first lane's
  `themedAccent()` (16% wash + accent glyph), so every mix has a colour identity
  instead of identical grey plates. (2) **Active indicator** — the mix whose
  `laneIds` equal the current deck (`deckIds` at the call site) gets a 1.5dp ring
  in the lead accent (never outlineVariant — no dark-mode white ring) + an
  "Active" label on the teaser line. (3) **Explicit Edit/Delete buttons** — the
  hidden 3-dot dropdown is GONE; footer now has a 28dp Edit (neutral) + Delete
  (error-tint) pair, with the lane dots capped (≤4 lanes: all four 16dp dots,
  >4: three dots + "+N") so the row fits the narrowest 2-col cell (~136dp inner
  on a 360dp phone). Card tap = spin only (no long-press menu). Height 114dp (was
  112) + 12dp air. (4) **Section polish** — header shows "Your mixes · N"; empty
  state is now a "Build your first mix" CTA (`NewSecondaryOutline`) instead of
  bare text (with zero mixes there was previously no visible way to create one
  on page 2).
- **v3xx13 — picker: borders GONE entirely, mix actions behind hold, page +
  scroll persistence.** User: "in dark mode the continue-exploring / browse /
  page-1 categories all have that white border — remove it, I don't want any
  border at all"; "in your mixes don't show any 3-dot or edit/delete button,
  only on tap and hold, and remove that New button (there's one at the bottom
  already)"; "make page 1/2 remember state persistent, and even the scroll —
  page should stay default when the user switches it just as it is now".
  (1) **Borders removed in BOTH themes:** `NewPickerTile` drops the
  selected/pinned ring, `AddSuggestionTile` drops its outline ring, and
  `NewMixCard` drops the Active ring — selection now reads ONLY through the
  solid category-tint fill, pinned through the pin badge, and the playing mix
  through the "Active" label; the `androidx.compose.foundation.border` import
  is gone from `NewCategoryPicker.kt`. (v3xx10/v3xx11 had already killed the
  DARK-mode outlines — the user wanted zero borders anywhere.) (2) **Mix
  actions behind tap-and-hold:** `NewMixCard` loses its explicit Edit/Delete
  footer buttons (REVERSES v3xx12) and gains `onLongClick` → a new centered
  `MixOptionPill` overlay (Edit · Delete, styled like `CategoryOptionPill`)
  wired through `NewPickerPage.onMixOption` + a sheet-level
  `mixOptionTarget`; `BrowseMixRow` (Browse Mixes tab) drops its always-
  visible 3-dot trigger too (its long-press DropdownMenu was already wired).
  (3) **Header "New" pill removed** from the Your mixes label row — the
  bottom action row's + already creates mixes; the zero-mixes "Build your
  first mix" empty-state CTA stays (user picked "keep it"). (4) **Page +
  scroll persistence:** the `pickerDefaultPageState` default-page behavior is
  UNCHANGED (intended feature); NEW — each pager page's scroll persists:
  `classicScroll` / `newScroll` `LazyListState`s are hoisted into
  `NewCategoryPickerSheet` (page flips keep position live), restored on open
  via `runCatching { scrollToItem(...) }` (saved index may exceed item count
  after hidden-lane changes), and saved debounced (300ms `snapshotFlow` +
  `drop(1)`) to new `KEY_PICKER_PAGE0_SCROLL` / `KEY_PICKER_PAGE1_SCROLL`
  ("index:offset") behind `AppPreferences.PickerScrollPos`
  get/set helpers — survives closing the picker AND app restarts.
- **v3xx15 — Cabinet v2 liked-row fixes + polaroid on every style, no photo
  required.** (1) **Kind-aware liked resolution** (`findLikedTopic` in
  CabinetV2Content.kt): liked books/series/albums search their CANONICAL lane
  first (BOOKS/ALBUMS/SERIES — where the reveal hearts live) before the
  global `TopicCatalog.findByName`, which walks lanes in enum order and
  strict base-name-matched "Animal Farm" (the book) to "Animal Farm (1954)"
  (the animated film) because ANIMATED_MOVIES precedes BOOKS. (2) **Rows
  always open:** `V2Liked.open()` no longer silently no-ops when the topic
  hasn't resolved yet (cold start before the lane pools warm) — it falls
  back to the kind's canonical lane slug + name, and the reveal's
  Room-backed per-category resolution finds the real topic. (3) **Label
  contrast:** the liked-row kind/category label now uses `categoryInk()`
  (theme-aware deep/light twin) instead of the raw accent, which blended
  into the `categorySurface`-tinted row. (4) **Polaroid on all styles, no
  photo needed:** the non-Collage print gate dropped `userPhoto != null`
  (render + the sheet/full-screen Polaroid TOOL buttons are now always
  available) — the print shows whenever `move.polaroidOnCard` is on, and
  without a photo it renders the designed empty frame (camera hint, tap to
  add photo) exactly like Collage; the sizing block null-guards `userPhoto`.
- **v3xx16 — Cabinet v2 COLLECTIONS (the 5.1 plan) + UI consistency + Recents
  tap-to-open.** (1) **Collections data layer** — `AppPreferences` gains
  `CurioCollection` / `CurioCollectionMember` (kind TOPIC = pinned topic via
  categoryName=CategoryId.name + refName; ENTRY = saved entry by stable Room
  id), persisted as a JSON array under `KEY_CABINET_COLLECTIONS`, reactive
  via `collectionsState`, with `get/saveCabinetCollections`,
  `addOrReplaceCollection` (upsert by id), `deleteCollection`. (2) **v2
  restyle** — CabinetV2Content now wears the classic torn Cabinet hero
  (`CabinetHeroHeader`/`CabinetHeroActionPill`/`CabinetHeroBannerHeight*`
  made `internal` in CabinetScreen.kt) with liquid-glass pills + hero search;
  the grid clears the fixed banner (`contentTop`). (3) **Collections home**
  — an Everything card (3×2 `V2CoverCollage` of `V2Liked` cover plates) +
  one card per collection + New tile; empty Cabinet shows 3 suggested
  discoveries + Shuffle (existing v2 behavior). (4) **Collection detail** —
  members render saved entries as `CurioEntryCard`s and pinned topics as
  `V2LikedRow`s; long-press a member → `CurioHoldPill` Move up/down/Remove
  (`moveMember`/`removeMember` — reorder changes the folder's cover); Add
  multi-select sheet (`V2AddEntriesSheet`); kebab → rename/delete. (5)
  **Create from moodboard** — the create sheet lists GalleryWall captures;
  picking one creates a collection pre-filled with that entry. (6) **Reveal
  File-to** — TopicRevealScreen long-presses the top bar
  (`detectTapGestures(onLongPress)` on the header Row) → `CurioHoldPill` →
  `FileToCollectionSheet` (check-marked already-filed collections, create on
  the spot, no duplicates). (7) **Recents tap-to-open** — RecentScreen +
  Home preview rows now default-tap into the REVEAL page (topic stays open);
  the write/save/open-entry actions moved to long-press → `CurioHoldPill`
  (`optionItem`/`recentOption` state), with `removeExplored` on the
  destructive action. (8) **Book browser restyle** — BookBrowserScreen wears
  the settings-family torn rose hero (`SettingsHeroHeader` + `SearchOff`
  empty state + `CurioVerticalScrollIndicator` + hero search), matching
  Recents / Manage Categories.
- **v3xx17 — experiment removals + app-wide header style.** (1) **Classic
  picker removed** — `CategoryPickerContent`'s route (`CurioRoutes`
  import + registration in CurioNavHost), the `KEY_CLASSIC_PICKER` pref +
  `classicPickerEnabledState` + SpinScreen branch are gone; the new picker
  is the only picker (`CategoryPickerScreen.kt` itself stays — it hosts
  `PickerMode`, which NewCategoryPicker still uses). (2) **Promo mode
  removed fully** — `PromoMode.kt`/`PromoModeScreen.kt` deleted, `PROMO`
  route gone, `KEY_PROMO_MODE`/`promoModeState`/`setPromoModeEnabled` gone,
  demo branches stripped from Home (stats, recents preview, View-all gate),
  Profile (streak/saved/xp), Quests (xp), Cabinet (entries + long-press
  gate); `TopicCatalog.sampleEntries()` stays as the harmless fallback for
  `sample-*` ids in EntryDetail/SaveCapture. (3) **Blur experiments
  removed** — `legacyGlassBlurState`/`customBlurEngineState` + their prefs
  and the `LegacyGlassBlur.kt` + `CurioBlur.kt` files deleted; the
  NavHost's legacy snapshotter plumbing and LiquidGlassPills' legacy
  capture import gone; pre-Android-12 pills always serve the static
  `fauxGlassCapsule` veil and the glass widget always uses system blur
  (the `getWallpaper` custom-blur path deleted from GlassWidgetProvider).
  (4) **Glass widget lab removed** — `GlassWidgetLabScreen.kt` +
  `GLASS_WIDGET_LAB` route + the clock (`AnalogClockWidgetProvider` +
  `glass_analog_*` res) and streak-circle (`FireWidgetProvider` +
  `fire_widget_*` res) home-screen widgets deleted from the manifest;
  the tile widget (`GlassWidgetProvider`) + editor
  (`GlassWidgetEditorScreen`) stay. (5) **Subtle pill glow hardcoded** —
  `curioGlassEdge`/`curioInnerGlow` read `subtle = true` directly,
  `KEY_PILL_GLOW_SUBTLE` + toggle plumbing removed from both experiments
  screens. (6) **Live explore notification always on** —
  `isLiveNotificationsEnabled()` now returns `true` (the persistent
  chronometer notification shows whenever sessions run + permission
  granted); the toggle rows, `KEY_LIVE_NOTIFICATIONS_ENABLED` and the
  NavHost's bring-the-bubble-back fallback are gone. (7) **Glass toolbar
  header style** — new `AppPreferences.HeaderStyle` (TORN default /
  GLASS), `KEY_HEADER_STYLE` + `get/setHeaderStyle`, toggled from a
  "Glass toolbar header" switch in BOTH experiments screens (Headers
  section). The new `CurioGlassToolbar` composable
  (ui/components/CurioGlassToolbar.kt) is a content-height liquid-glass
  bar (rose-tinted `lerp(surfaceContainerHigh, settingsRoseAccent)`,
  1.6× `blurMultiplier` frost, bottom-rounded capsule, own back pill +
  optional trailing pills / morph-open search / titleTrailing / content
  slot) and replaces the torn banner in `SettingsHeroHeader`,
  `CabinetHeroHeader`, `HomeScreen`'s quest hero and `ProfileHero` (Spin
  untouched). Height reservations became style-aware: `SettingsHeroTotalHeight`
  (160dp glass), `CabinetHeroBannerHeight`/`Compact`/`SheetExtent`
  (160/160/0 glass), `ProfileHeroTotalHeight` (230dp glass).
  **SAFETY:** the Home toolbar deliberately passes NO glassBackdrop — it
  is the first item of the scroll Column INSIDE `homeGlassBackdrop`'s
  capture subtree, so sampling it would be the v228 self-capture cycle;
  it falls back to the simulated-glass recipe. Settings/Cabinet heroes
  keep real glass via their v263 sibling-overlay capture (hero drawn
  OUTSIDE the recorded grid).
- **v3xx18 — Home/Profile glass headers MORPH with scroll.** The Home and
  Profile glass toolbars are no longer static content-height bars: new
  `CurioGlassToolbarMorph` (ui/components/CurioGlassToolbar.kt) is a
  PINNED collapsing header (sibling overlay OUTSIDE the local glass
  capture — Home's `homeGlassBackdrop`, Profile's `profileGlassBackdrop` —
  so it samples the REAL backdrop, fixing the old in-capture simulated-
  glass fallback). At the top it is the full bar (menu/back pill + title
  + subtitle + avatar + the stat row); scrolling collapses it smoothly
  (FastOutSlowIn, height lerps from the measured full height down to
  `HomeCompactHeaderHeight`/`ProfileCompactHeaderHeight` = 54dp, full
  content fades out rising while the compact row — avatar + display name
  ("Curious Explorer") — fades in; `Modifier.layout` measures the natural
  height once and reports the animated height, `clipToBounds` trims).
  Home's floating menu/avatar pills and Profile's pinned Back/Settings
  pills are HIDDEN in the glass style (the morph bar carries its own
  menu/back + avatar/settings pills); `HomeScreen`'s scroll hero slot
  became a `Spacer(HomeCompactHeaderHeight)` and `ProfileHero`'s glass
  branch a `Spacer(ProfileCompactHeaderHeight)` for the collapsed-bar
  clearance (content flows beneath the pinned bar). Scroll progress is
  the existing `stickyProgress` (90dp threshold) on both screens.
- **v3xx19 — morph-header refinement (user follow-up).** (1) The FULL
  (not-scrolled) state is now MORE EXPANDED and shows the stats as a
  proper stat CARD — the torn hero's rose-gradient pane (`curioDarkGlow`
  + `shadow(clip=false)` + opaque `lerp(container, White, 0.06→0.26)`
  vertical gradient, 20dp rounded) wraps the `content` slot, and the
  title row breathes (top 14 / bottom 8). (2) The COMPACT bar now carries
  glass pills beside the name: the STREAK pill (fire + days → opens
  Quests) and the EDIT pill (Profile only → opens the Edit-profile
  dialog), built on the same rose pill glass as the leading pill
  (`streakCount`/`onStreakClick`/`onEditClick` params). `trailing`
  (Profile's Settings pill) now rides the FULL row only — the collapsed
  bar keeps avatar + name + streak + edit.
- **v3xx20 — Cabinet FOLDERS (the JSX redesign, user-directed).** The
  experimental Cabinet v2 view is rebuilt to the `Curio_Cabinet_Reimagined.jsx`
  design (user's ask; scope answered: replace v2 behind the same toggle,
  keep the torn rose hero, saved entries keep `CurioEntryCard`, "Add
  something new" → Spin): (1) **Home order** — a JSX-style **Everything
  card** (frosted icon tile + title + circular arrow + a media rail of the
  user's REAL liked book/album/series jacket art + a "+" slot + item
  count; tap → Everything library) → **Saved entries** section (classic
  `CurioEntryCard` grid, tap opens, long-press multi-select batch delete
  unchanged) → **Collections** section. (2) **Built-in shelves** — 7
  JSX-style shelf cards (`features/cabinet/CabinetShelves.kt`, new):
  `builtInShelves` (Favorites · Currently Reading · Want to Read · Saved
  entries · Completed · Notes · Personal), each with a pastel tone
  (light/dark pairs), a glyph, an item count and hand-drawn decorative
  art (`V2ShelfArt` — star+mountain, open book, stacked books, hills,
  paper stack, window, photos — Canvas + glyphs, theme-aware). The three
  VIRTUAL shelves (Favorites = liked books/series/albums, Saved entries =
  all captures, Notes = note-format captures) are computed live
  (`shelf:*` levels via `v2VirtualShelfItems`); the four STARTER shelves
  (Currently Reading / Want to Read / Completed / Personal) are seeded
  ONCE as ordinary editable `CurioCollection`s (ids `shelf:*`, new
  `KEY_CABINET_SHELVES_SEEDED` + `seedCabinetShelves` + state sync), so
  add-captures / reveal File-to / rename / delete all work on them.
  USER collections cycle a tone+art palette and keep the long-press
  rename/delete pill. (3) **Everything library rebuilt** — the old
  collapsible section lists are gone; the page now matches the JSX:
  Filter + Sort pills (DropdownMenus), a grid/list view toggle, a type
  filter rail (`TYPE_FILTERS`: All · Books · Albums · Series · Notes ·
  Moodboards · Reviews, mapped to real kinds/formats via
  `entryInType`/`likedShownForType`), a Recent rail (newest 6 captures,
  `LazyRow` of `CurioEntryCard`), an All Items grid/list (entries as
  `CurioEntryCard`, likes as new `V2LikedTileCard` jacket tiles /
  `V2LikedRow` in list mode), and an "Add something new" button
  (`V2AddSomethingButton` → `navigateToTab(SPIN)`). Multi-select batch
  delete is preserved on the library page; select-all scope is now
  level-aware (`visibleIds`). Deleted from the old v2: `V2CollectionCard`
  (3×2 collage), `V2CoverCollage`, `V2PlainHeader`, `V2SectionHeader`
  (collapsible sections) and the formatFilter/availableFormats chips.
- **v3xx21 — Home header revert (morph = Profile-only) + pet fix + share-
  card NO-overlap / NO-cut fit.** (1) **Home reverted** (user direction
  2026-09-08: the morph belonged to PROFILE — "why did you implement the
  profile look in home screen, apply that in profile and keep home as it
  was"): the pinned `CurioGlassToolbarMorph` is GONE from `HomeScreen`;
  Home is back to the pre-morph glass state — the STATIC content-height
  `CurioGlassToolbar` as the first scroll item (glass style) or the torn
  rose hero, plus the always-floating menu/avatar pills (`stickyProgress`
  morph). The pinned full-width bar was covering the flower-bed pet while
  it scrolled — the revert fixes that too. PROFILE keeps the morph
  (v3xx18/19 unchanged). (2) **Share-card smart fit — no-clip guarantee**
  (TopicShareCard.kt): `autoFitShape` now sizes the fact TEXT so the
  WHOLE fact always fits its box — past the design's text floor the type
  keeps shrinking (down to `FactFitHardFloor` 0.5×) instead of
  ellipsizing; the box-growth caps were reverted to their validated
  heights (Paper 1.85/1.8, mid-flow else 1.45/1.4) so the box never
  touches the footer/title/card edge — v3xx31 later raised the 9:16 caps
  and added the real geometry bounds (real-width measurement, free-
  middle clamp, maxLines capacity bound) that supersede these static
  caps (see v3xx31). Fit math is per-style:
  `factBoxBaseLines(style, aspect)` (each style's natural line capacity)
  and `factWrapFactor(style)` (Vinyl's narrow 220dp pane wraps ~1.15×
  the canonical 252dp width), with a 0.85 safety margin over the
  measured wrap count. (3) **Sparkle: dragged-over title returns to the top** — a title
  the user MANUALLY dragged into the quick fact (or info rows,
  `move.titlePlaced`) is RESET to its natural spot on the sparkle tap
  (`resetTitleY` on `ShareAutoLayoutPlan`; commit zeroes `titleDx`/
  `titleDy` and clears `titlePlaced`) instead of the old minimal-lift
  guess; only the prospective grown-fact lift applies on top, and the
  title-vs-fact overlap is excluded from the fact push. (4) **Collage
  polaroid** — `PolaroidPrint(shadow = false)` on the Collage: the dark
  `shadowElevation` blur behind the TILTED cream print read as a
  "background showing behind the strip" (preview AND export); the tape +
  tilt keep the scrapbook depth. Other styles keep the shadow.
- **v3xx32 — Inline fact-editor caret ACCURACY (user follow-up 2026-09-08:
  "the cursor in the inline editor is still wrong when I tap to edit — it's
  very inaccurate, due to the hidden text small — fix it in both full
  screen and bottom sheet").** Root cause: the invisible typing field's
  style came from the card's `onFactStyle` report, which fired only inside
  `onGloballyPositioned` — i.e. when the fact box's BOUNDS move. When the
  smart fit shrinks the text inside a box that is clipped at its maxLines
  cap (the node's bounds stay put), the report went STALE — the field
  kept the old (bigger) size while the visible glyphs rendered smaller,
  so the caret and tap-to-position landed off the visible text. Fix:
  every style now re-reports its fact style on EVERY composition
  (`LaunchedEffect(style) { callbacks.onFactStyle(style) }` after the
  style val in Vinyl / Collage / Clean / Editorial (`bodyStyle`) /
  Minimal / Signature / Custom / MiddleContent's `qStyle` + `frostStyle`)
  — the field always uses the CURRENT rendered size (family, size, line
  height, align, format tweaks). Second divergence fixed: the field now
  seeds its `TextFieldValue` with the SAME annotated runs the card renders
  (`buildRichAnnotated(factFieldText, unshifted cardFactSpans, marker)`)
  so bold/italic/highlight and the enlarge-editor's per-run FONT SIZES
  wrap identically in the field (the caret stays on the glyphs even on
  sized runs); a span-only change (format toggle) refreshes the annotation
  while preserving the live selection. Both the bottom-sheet preview and
  the full-screen editor share the one ArrangeableCard field, so both are
  fixed by these two changes.
- **v3xx31 — Share-card smart-fit OVERHAUL (user follow-up 2026-09-08:
  "the spark pill struggles with longer texts — it doesn't increase the
  box height fully / place them properly / shrink the text, especially in
  the Clean layout 9:16; the fit + font-size decrease should happen
  within the slider, not hidden").** `TopicShareCard.kt` — five concrete
  bugs fixed + the solver got real geometry: (1) **REAL-WIDTH wrap
  measurement** — `rememberFactWrapLines` now measures at
  `FactWrapMeasureWidth` = 252dp (the 280dp-base preview's content
  width) instead of the DESIGN width (405/450dp): the old measurement
  under-counted wraps by ~1.5× (previews render at 280dp; Clean's 30/26
  padding leaves 224dp), so long facts got CUT — worst on Clean 9:16.
  Vinyl's pane factor drops 1.7× → 1.15× (it was relative to the design
  width). (2) **FREE-MIDDLE height clamp** — new `factAvailHeightDp` +
  `factLineHeightDp` per style+aspect bound the grown box's rendered
  height (realWraps × lineH × scale) to the gap between the header and
  the footer credit, so the "via Curio" footer can never be pushed or
  hidden behind the bottom design again; past the hard floor the box
  simply can't fit that aspect. (3) **maxLines capacity bound** in
  `fitScaleFor` (base·h ÷ (eff·1.2)): the render's maxLines cap now
  always ≥ the real wrap count through the mid-range (the plain model +
  margin still cut by a line or two). (4) **9:16 budgets raised to use
  the tall canvas** — Clean/NEUMORPHIC 1.7→2.9, Minimal 1.6→2.7, Paper
  2.0→2.6, Vinyl/Signature/Custom 1.5→2.2, Editorial 1.4→1.8, Collage
  1.3→1.5 (3:4 caps stay validated; the clamp is the real bound).
  `autoFitGrowByWrap` top end rises to 3.2×. (5) **Fit visible in the
  sliders** — `FactFitHardFloor` 0.45→0.5 (the Text-size slider's floor),
  so the rendered text size is always on the thumb; the Fact-height
  sliders (sheet Crop + full-screen Box) no longer divide back through
  the stale fit (that collapsed the box ~2.4×→1× on the first drag tick
  and popped the text back to full size) — the first tick captures the
  rendered height AND the fit's text shrink into `move.factScale` (the
  grip-seed recipe), later ticks map 1:1. (6) **AUTO 9:16 detection** —
  `runAutoLayout` computes `autoTall`: when even the hard-floor scale
  can't fit the 3:4 free middle, the very next sparkle tap jumps
  straight to the tall plan (`attempt = 5`), and attempt 5 now carries
  the REAL tall fit (`autoFitShape` on PORTRAIT — taller box + correct
  tall text) instead of the old 1.4×/1.18× guess.
- **v3xx30 — Cabinet card style REVERT (user follow-up 2026-09-08).**
  The user's original request was to REDRAW the collection card art, not
  redesign the cards — the v3xx27 full-card whisper-alpha art read as
  INVISIBLE on the cards ("I can see the design only while creating the
  collection"), so `V2ShelfCard` is back to the JSX "CollectionCard"
  layout the user liked: tone fill + frosted icon tile + title/item
  count, with the REDRAWN art as a VISIBLE foot strip (86dp, full
  alpha — the responsive scenes scale down to the strip exactly like the
  old ones). Kept from v3xx27: the redrawn scenes (incl. PEAK +
  MINIMAL_*), the anchored ⋮ DropdownMenu (onRename/onDelete — the user
  asked for a real dropdown, not the centre overlay), and the
  create-collection style picker. The `.alpha` import is gone from
  CabinetShelves.kt.
- **v3xx29 — Settings hub REDESIGN (user follow-up 2026-09-08; the
  CurioSettings_Redesign-3.jsx look — committed, NOT pushed per user
  instruction).** The phone hub (`SettingsHubScreen.kt`) is rebuilt to
  match the JSX exactly while the app's own header (SettingsHeroHeader /
  CurioGlassToolbar) stays untouched: (1) a **nav rail** — All Settings /
  Appearance / Pet designer / Preferences / Recording / Categories /
  Topic history / Share hub / Experiments / Backup / Support — as a
  horizontal chip rail (the JSX desktop sidebar's mobile twin; active
  chip = JSX brown #815947). (2) A **JSX search box** (rounded white,
  magnifier + clear); the existing deep row index + search results UI
  still run underneath. (3) **Tone CARDS in 5 groups** (Personalize ✦,
  How it works ✧, Organize your world ≡, Share & explore ◇, Your data &
  privacy ◈ — the last one slots Backup & restore + Book covers where
  the user said "data etc gets the book fetching etc"): every card is a
  pastel-gradient surface (10 tones, light pastel + deep dark twin),
  blob + texture-dot Canvas art, frosted icon tile, round arrow,
  title/subtitle capped at ~76% width, and a decorative foot visual
  (swatches / pet / compass / wave / card stack / photos / share /
  flask / cloud / image). (4) Secondary horizontal cards (Recycle bin,
  Updates, Help & feedback) + the "Same curiosity, new horizons."
  footer note in Playfair. Data model: `SettingsDesignGroup/Card`,
  `SettingsNavEntry`, `settingsToneGradient`, `SettingsCardVisual`,
  `SettingsDesignCardView`, `SettingsNavRail`, `SettingsJsxSearchField`,
  `SettingsSecondaryCardView`, `SettingsFooterNote`. The search index +
  tablet two-pane still consume the underlying row model untouched; the
  Appearance card keeps its PetLandmark. Removed from the hub face: the
  flat CurioSettingsRow list (rows live on in search + two-pane).
- **v3xx30 — settings nav rail on EVERY settings-family page (user
  follow-up 2026-09-08).** The hub's JSX nav rail now rides every
  settings screen — the 10 rail destinations (Appearance, Pet designer,
  Preferences, Recording, Manage categories, Topic history, Share hub,
  Experiments, Backup, Support) PLUS the drill-in tool pages (Book
  covers, Book browser, Recycle bin, Updates, Widget editor).
  `SettingsNavRail` is now a SHARED composable (`active: String?`): the
  OPEN page is rotated into the SECOND slot right after "All Settings"
  (highlighted), so where you are sits next to the way back; drill-ins
  pass null (no highlight, fixed order). Switching REPLACES the current
  page via the shared `navigateToSettingsSection` (popUpTo the hub +
  launchSingleTop) — the hub is always one back-press away and the
  visited-sections stack never grows. The rail is the FIRST scroll item
  below the hero (the hub's exact placement), phones + wide alike; the
  hub itself keeps its rail (active "all").
- **v3xx31 — settings card foot visuals REDRAWN (user follow-up
  2026-09-08).** `SettingsCardVisual` + `SettingsDesignCardView` in
  SettingsHubScreen.kt: (1) the visual now renders BEFORE the text
  Column, so the art sits BEHIND the title/subtitle (it used to draw on
  top of long subtitles). (2) Every art is re-laid-out to fit the 92×62
  visual box with the card's rounded bottom-right corner kept clear
  (nothing cut): pet redrawn minimal + accurate (ears/head/eyes/nose/
  body all inside), Manage-categories card stack + Topic-history
  polaroids are proper fanned shapes that never clip, share bubble icon
  removed (mini card only), Experiments → the `science` flask glyph,
  Backup & restore → the `backup` cloud glyph, Book covers → the `image`
  glyph (icons preferred over extra drawing per user), Preferences
  compass polished (mountains + smaller ring + solid/pale needle). (3)
  The visual box nudged closer to the corner (padding 12/8 → 8/7dp).
  (4) Card texture: the white diagonal sheen drawLine is GONE — replaced
  with the JSX `cardTexture` bubble-dots style: two outlined circles +
  a six-dot speckle (white, 0.30).
- **v3xx32 — settings sub-pages unified via SHARED components (user
  follow-up 2026-09-08).** New `features/settings/SettingsPageComponents.kt`
  is the ONE visual language for every settings-family page, mirroring the
  hub's secondary-card look: `SettingsSectionHeading` (glyph + Playfair
  serif label + short rule — replaces `CurioSectionLabel` in the settings
  family), `SettingsOptionCard` (frosted white / raised-dark glass,
  rounded 20), `SettingsOptionRow` (frosted 40dp icon tile + title +
  subtitle + chevron), `SettingsOptionSwitchRow`, `SettingsOptionSegmentedRow`,
  `SettingsOptionInfoRow` and `SettingsOptionDivider` (hairline inset 53dp
  to the tile column). Applied across the whole family: SettingsSectionScreen
  (Appearance/Preferences/Recording/Data — the private CompactSwitchRow /
  CompactSegmentedRow helpers now DELEGATE to the shared rows and gained
  icon tiles; Theme→dark_mode, tint→palette, pastel→auto_awesome,
  material→layers, hero-tears→auto_stories, hero→image, adaptive→refresh,
  curie→pets, sessions→travel_explore, bubble→bubble_chart, reminder→
  notifications), BackupToolsScreen (incl. the Auto-backup toggle row),
  ExperimentsScreen + UserExperimentsScreen (labels/cards/rows — the
  ExperimentSwitchRow wrapper delegates to the shared switch row and keeps
  its disabled alpha), SupportScreen (Version row became a shared option
  row), UpdatesScreen (Update-checker toggle became a shared switch row),
  ShareHubScreen labels, and the hub's own search-results labels/cards/
  rows. Only verified-in-subset glyphs are used (CurioIcons constants).
  Custom content (chips, status header, release notes) keeps its own
  layout inside the glass cards.
- **v3xx35 — settings polish: hub cards, rail position + quick tools,
  three pages in the design language (user follow-up 2026-09-09).**
  (1) **Hub cards** — `SettingsDesignCardView`: the title + subtitle now
  sit at the TOP of the card right under the corner icon (the old
  `Spacer(weight)` pushed them to the bottom) and run FULL width (the
  old 76%/80% caps cut long subtitles); a `bigTitle` flag on the card
  bumps Appearance + Pet designer to titleLarge 20sp (only those two);
  the Experiments / Backup & restore / Book covers visuals are real
  drawings now, not lone icons: a glass flask with lavender liquid +
  bubbles + ✦/✧ sparkles, a cloud under a soft sun with an upload arrow,
  and an open book whose right page carries a sun-over-hill cover scene.
  (2) **Nav rail** — `SettingsNavRail` no longer rotates the open page
  into slot 2: the rail keeps its FIXED order, highlights the page in
  its natural slot and `animateScrollToItem`s to it (stays where you
  are). New `navController` param + a `SettingsQuickTools` row under the
  chips: the ACTIVE page's key deep settings as frosted pills (Theme /
  Category tint / Pastel / Adaptive Hero on Appearance; Search engine /
  Sessions / Pet games / Shuffle reminder on Preferences; Audio quality /
  Voice-to-text / Offline model on Recording; Backup / Recycle bin /
  Book covers / Updates on Backup; a frequent set everywhere else),
  navigating via `SettingsHighlightTarget` deep highlight. All 17 rail
  call sites pass `navController`. (3) **Design-language fixes** —
  ManageCategoriesScreen: the lanes list is now ONE frosted
  `SettingsOptionCard` with `SettingsOptionDivider` hairlines (the
  Recording look; the old flat rows + CurioSettingsDivider are gone),
  a Playfair "Your lanes" heading, and the reorder-lock notice is a
  frosted card with the warm settings icon tile. BookCoverHubScreen:
  the plain header + flat `surface` Column became the settings chrome
  (torn-rose hero + watermark backdrop + sticky glass hero), cards are
  frosted (master switch / providers / stats / progress / failed rows)
  with warm icon tiles + Playfair section headings. RecycleBinScreen:
  summary + trashed rows are frosted with rounded 13dp accent tiles
  (settings tile shape), and "RECENTLY DELETED" is a Playfair heading +
  rule.
- **v3xx33/34 — Cabinet shelf art REDRAWN, "Curiying now" rename +
  shelf LOGIC, Favorites redesign + redesigned Add sheet (user follow-up
  2026-09-09).** (1) **Shelf art** — the five seeded-shelf arts in
  CabinetShelves.kt are redrawn to actually read as what they are,
  bottom-anchored (nothing floats/clips): Favorites = warm heart + halo
  (`ConstellationArt` redrawn), Curiying now = connected open book with a
  spine line + ribbon (`ReadingArt`), Want to Read = three upright book
  spines (`BooksArt`), Completed = summit scene + solid ground band
  (`PeakArt`), Notes = cream notepad with folded corner + ruled lines +
  pencil (`NotesArt`); minimal arts' baselines lowered to the bottom edge.
  (2) **Rename + logic** — "Currently Reading" is renamed **"Curiying
  now"** (shelf id stays `shelf:currently-reading`; `seedCabinetShelves`
  renames an installed shelf once) and now holds books, series AND albums:
  new `AppPreferences.toggleShelfTopic`/`isTopicInShelf` persist a TOPIC
  member in/out of a seeded shelf (creating the shelf on the fly), and
  `CabinetShelfToggleChips` (TopicRevealScreen) puts the Curiying-now /
  Want-to-read pills in ALL THREE reveal sheets (book/album/series) while
  `V2ShelfToggleChips` (CabinetV2Content) puts the same toggles in the
  liked-row ⋮ cover-source sheet. (3) **Favorites redesign** — the virtual
  Favorites shelf wears the collection-detail header language (title +
  count + emphasized **Add** pill via `v2VirtualShelfItems.onAdd`),
  opening the Add sheet in favorites mode. (4) **Add sheet redesign** —
  `V2AddEntriesSheet` is replaced by `V2AddToShelfSheet` keyed on a sealed
  `AddTarget` (Collection | Favorites): a frosted search field over
  favorites + saved captures + one `TopicCatalog.findByName` fallback, a
  "From favorites" quick-pick (one-tap topic members; favorites mode
  bookmark-saves/un-saves via `toggleLikedFavorite`), and the saved-
  captures multi-pick (format tile + checkbox). (5) **Add-sheet lag fix**
  — the sheet works off a lightweight `AddOption` projection (id + name +
  subtitle + `formatGlyph`) computed ONCE in the parent (never the full
  CurioEntry payloads), and the idle capture list is capped at
  `ADD_OPTION_CAP` = 100 rows — search finds the rest.
- **v3xx35 — pet outfit shop REMOVED + Cabinet liked tiles de-plated (user
  follow-up 2026-09-09).** (1) **Full pet-shop removal** — the user chose
  full removal: `OutfitShopScreen.kt` + `PetOutfits.kt` deleted, the
  `OUTFIT_SHOP` route + NavHost registration + the Profile/Quests entry
  rows gone, and the whole sparkle economy stripped: `sparklesState` /
  `addSparkles` / `spendSparkles`, owned/equipped outfit + owned game
  state + persistence (`KEY_SPARKLES` / `KEY_OWNED_OUTFITS` /
  `KEY_EQUIPPED_OUTFIT` / `KEY_OWNED_GAMES`), the quest/streak sparkle
  payouts in CurioQuests, the equipped-outfit overlay in CurioPetSprite,
  and the OUTFIT/GAME rewards in LevelRewards (RewardKind is now PALETTE
  + LANE_ORDER only). Quest XP, levels and the palette/lane rewards are
  untouched; Home's quest toast no longer says "+sparkles". (2) **Liked
  tiles de-plated** — `V2LikedTileCard` drops the tinted Surface wash and
  the 8dp frame (art sits edge-to-edge, no outline), and `V2JacketArt`'s
  accent gradient only renders as the LOADING placeholder (transparent
  once a cover is on screen), with the book spine/sheen overlays gated on
  an image being present — kills the "background behind the album" look
  on Everything + every shelf grid.
- **v3xx36 — Everything preview de-tinted + shelf art REFINED (user
  follow-up 2026-09-09).** (1) **Everything preview de-tint** — the
  Everything card's horizontally-scrolling cover rail dropped its
  category-tint gradient plate: covers render edge-to-edge (`V2JacketArt`
  shows its own accent placeholder only while a cover loads), matching
  the de-plated shelf grids. (2) **Completed not cut at the sides** —
  `PeakArt`'s far/near ridge polygons and the ground band now run a
  little PAST both canvas edges (`-0.08w … 1.08w`) so the scene reads as
  continuing mountains instead of shapes chopped by the frame (the old
  polygons had vertical seams at x=0 and x=w). (3) **Notes refined** —
  `NotesArt` gains a second sheet peeking out bottom-right (notepad
  stack), a darker dog-ear FLAP triangle under the fold line, a small
  heart doodle at the sheet's foot, and a proper pencil (thick body +
  pink eraser cap + dark lead tip). (4) **Want to Read refined** —
  `BooksArt` gains two title ticks near the top of every spine and a soft
  grounding shadow oval under the row.
- **v3xx28 — Cabinet per-item colors + fresh grid per level (user
  follow-up 2026-09-08).** (1) **Extracted cover colors** — liked
  books/albums/series rows, tiles and the Everything preview now use the
  DOMINANT COLOR of each cover's cached art (not the category accent) for
  the accent dot/label, the jacket plate gradient and a subtle card
  surface tint. `CabinetCoverCache.dominantCoverColor(context, kind,
  name, fallback)` downsamples the cached `.img` (~24px), bucket-
  quantizes RGB and caches the winner ARGB forever in a static map
  (`kind|name` key); callers re-key on `version.intValue` so tiles that
  composed before their cover landed re-extract when the warmer saves
  the file. Fallback = category accent while bytes aren't on disk.
  Reviews (`V2ReviewTileCard`) borrow the REVIEWED media's extracted
  color via `reviewCoverKind(name)` (book/album/series decided by which
  art store already holds that name) and the 1dp OUTLINE is gone — the
  color tints the card instead. (2) **Grid per level** — `key(openLevel)`
  wraps the LazyVerticalGrid and `rememberLazyGridState()` moved inside
  it: the single shared scroll position was making a collection open
  MID-list and page switches visibly jump (the glitch); every level now
  opens from the TOP.
- **v3xx27 — collection card DESIGN pass (user follow-up
  2026-09-08).** (1) **Shelf cards now carry their art** — the drawn
  scene fills the WHOLE card as a whisper-alpha background, not just a
  foot strip ("the box designs itself… drawn elements not the icon"):
  Favorites wears the glowing star-map CONSTELLATION, Currently Reading
  an open-book scene, Want to Read stacked spines, Saved a photo
  collage, Completed the FULL redesign (sun-arc summit + planted flag +
  bird — `PEAK`), Notes a slip-stack with a pen scribble, Personal a
  moonlit window with a sill plant. Plus four MINIMAL_* scenes (sun,
  rings, wave, dots) borrowing the Minimal share card's sparse line
  language for plenty of variety. Every scene is drawn with proportional
  Canvas geometry so it scales from foot strip to full-card background.
  (2) **New-collection sheet style picker** — the sheet now lets you
  pick a card style: tone swatches (expanded palette of 9), live art
  previews (all 13 scenes on their tone fills) and icon chips (12
  glyphs), each independently optional — left on Auto the card cycles
  the palette like before; tap a selected option again to reset to
  Auto. `CurioCollection` gains `tone`/`art`/`icon` (indices into the
  Cabinet palettes, -1/null = auto; backward-compatible JSON) and the
  home grid renders each collection's custom style. (3) **⋮ dropdowns
  are anchored now** — the collection card's ⋮ (V2ShelfCard) and the
  collection-detail ⋮ (V2DetailHeader) open a DropdownMenu right under
  the dots (Rename / Add captures / Delete); the old centre-screen
  `CurioHoldPill` overlay for collections is gone (PillTarget.Collection
  removed; the member long-press pill stays). (4) **Dead hint text
  removed** — "· tap a member to open it" (hero subtitle) and "·
  long-press a member for more" (detail header) are gone.
- **v3xx26 — editor caret / box-outline accuracy (user follow-up
  2026-09-08; the CI fix rode separately in `3f466b5c`).** The inline
  quick-fact field (ArrangeableCard's transparent BasicTextField — shared
  by the bottom-sheet preview AND the full-screen card editor) used
  `heightIn(min = f.height.dp)` with `maxLines = 60`, so the field grew
  past the visible fact box: the caret could sit BELOW the visible
  glyphs and the selection outline (the `.border` on the fieldModifier)
  extended past the box ("cursor/text position wrong and misleading",
  "the box outline misleads"). The field is now clamped to the measured
  box height (`heightIn(min = max = f.height.dp)`); the smart fit
  re-measures on every keystroke and grows the box, so caret + outline
  always hug the visible text. Root-cause note: the reported
  `onFactStyle` already includes the smart-fit text scale (styles render
  with `effectiveBodyScale`), so the caret metrics were otherwise
  glyph-exact — the unbounded height was the mismatch.
- **v3xx25 — share-card smart fit refinements (user follow-up
  2026-09-08).** (1) **HEIGHT-FIRST fit** (`autoFitShape`) — the fact box
  now grows toward the style's FULL `factFitBudget` cap whenever
  `autoFitGrowByWrap` > 1 and the TEXT sizes to fit the grown box
  (S² ≤ base·h/eff, floored at `FactFitHardFloor`); the old text-first
  solver kept the box at its natural height and only shrank the type, so
  a longer fact never LOOKED taller even when the card had room below
  ("make its height maximum with longer text… height increase in
  accordance with the bottom area"). The no-clip guarantee holds. (2)
  **Fact-box auto-move** (`resetFact` on `ShareAutoLayoutPlan` +
  `runAutoLayout` commit) — a quick-fact box the user manually dragged
  (`factDx`/`factDy` ≠ 0) ONTO the title / info rows / favorites strip, or
  off the card edge, is RESET to its natural spot on the sparkle tap
  (the title-reset twin). (3) **Badge guard** — when the category pill's
  measured rect is Zero (first composition), `titleLiftCap` falls back to
  the title's own natural top `(t.top - 4dp)` instead of unbounded, so
  the sparkle can never shove the title up over the category icon. (4)
  **Footers smaller** — the "via Curio" credit text drops 10sp → 8sp at
  lower alpha (both the rose-bulb and white-credit footers) and the bulb
  mark 12dp → 10dp; the footers stay FIXED (only the author/year info
  rows move). Not addressed this round: the inline/full-screen editor
  caret-vs-text alignment and the fact-box outline fidelity (both follow
  from the bounds-hub metrics — separate pass).
- **v3xx24 — liked-media cover CACHE + Cabinet Everything rework (user
  direction 2026-09-08).** (1) **`CoverCache.kt`** — a separate,
  always-on cover store for liked books/albums/series: the resolved URL
  is persisted per topic (reusing `bookCoverUrlsState` /
  `sheetArtUrlsState` so reveal + cards agree) and the IMAGE BYTES
  download once to `filesDir/cover_cache/<kind>-<name>.img`. An
  always-on warmer in `CabinetV2Content` resolves every unwarmed liked
  item on Cabinet open (throttled to 30/recompose); `V2JacketArt` loads
  the local file FIRST (keyed on `CoverCache.version` so tiles re-check
  the moment a cover lands) and its live resolve cascades BOTH
  providers (books iTunes → Open Library, albums iTunes → MusicBrainz,
  series TVMaze → iTunes) and persists the winner. The liked-tile ⋮
  opens `V2CoverSourceSheet` — an explicit provider switch that
  re-resolves, persists and re-caches ("if you didn't like that one").
  (2) **AppPreferences likedAt** — `KEY_LIKED_AT` ("kind|name" → epoch
  ms) written by `toggleBookFavorite` / `toggleSeriesFavorite` /
  `toggleAlbumFavTrack`, feeding the Everything Recent rail. (3)
  **Everything preview card** — theme-aware (`surfaceContainerHigh`
  tokens; the hardcoded cream wash is gone), ONE chevron (the duplicate
  arrow + the '+' slot are removed), and a horizontally SCROLLABLE cover
  rail of real cached covers (was a fixed 5-slot row). (4) **Everything
  page** — always `GridCells.Fixed(3)` on phones (the list/grid toggle
  is deleted — the grid replaced the list; `viewMode` state and
  `V2ViewTogglePill` are gone); grouped per-kind sections with distinct
  tiles: `V2MediaTileCard` (BOOK portrait jacket / ALBUM square + vinyl
  disc / SERIES poster), `V2ReviewTileCard` (ReelNotes get an OUTLINED
  card: quote mark, star rating, preview), notes/moodboards keep
  `CurioEntryCard`. `V2FilterRail` gained `available: Set<String>` and
  only lists types with content; the Recent rail (`V2RecentCell` sealed
  Entry/Liked) merges recent captures with recently liked media; media
  sorts by likedAt (Recent) or name (A–Z). (5) **Back handling** —
  `BackHandler` walks selection → search → open collection / Everything
  / virtual shelf before leaving the Cabinet. (6) **Collection ⋮** —
  `V2ShelfCard` gained `onMoreClick` (a real tappable ⋮ button opening
  the same rename/delete pill); the Completed shelf icon is
  `CurioIcons.Check` (the task_alt glyph read squished in the frosted
  tile).
- **v3xx23 — text history everywhere + bottom-sheet/tree browser + pin
  fix; Home anchored hold menu; keyboard-aware full-screen editors;
  one-shot rich-text tools (user follow-up 2026-09-08).** (1) **Text
  history now covers every field** (TextHistory.kt + TopicShareCard.kt):
  the share-card TITLE joins the captured fields (`editedTitle ?: ""` —
  blank skips) alongside quick fact / custom fact / chapter review /
  quote / photo caption; the chapter-note Enlarge editor (TopicRevealScreen
  `noteEditorChapter`) gained a `TextHistoryPill` in its header +
  `rememberTextHistoryCapture` + a `TextHistoryBrowser` that restores into
  the same AppPreferences slot. The pill sits in the host headers which
  now `imePadding()` above the keyboard. (2) **Dedupe + move-to-top**
  (`TextHistoryStore.record`): a repeat of an OLDER entry no longer stacks
  a duplicate — the existing entry MOVES to the top with a fresh ts
  (pinned rides along); exact repeats of the field's latest snapshot still
  skip. (3) **Browser = ModalBottomSheet** (was a centered Dialog) with a
  drag handle + a **List / Tree toggle**: Tree groups snapshots into
  `HistoryBranch`es keyed on shared opening paragraphs (`splitParagraphs` /
  `commonPrefixLen`) — the trunk renders once, each version node shows
  only its CHANGED paragraphs (removed struck through, added in
  semi-bold) behind a small connector; List mode is the old feed. (4)
  **PIN FIX** — `HistoryRowAction(e.pinned, e.pinned, …)` disabled the
  pin on UNPINNED entries (only unpin ever worked): now `enabled = true`
  so any snapshot can be pinned. (5) **Home hold menu** — the recents
  rows (`ExploreTopicRow` / `RecentEntryRow`) now take `hold:
  HoldSession?` and attach `radialHoldMenu` (the category picker's
  gesture: real press position, scroll-cancel, long-press timer) before
  `combinedClickable`; the old centered `CurioHoldPill` is replaced by
  `RadialHoldMenuOverlay` rendered at the SCREEN level (sibling of the
  page background — a fillMaxSize scrim can't live inside the scroll
  flow), anchored at the held spot with Edit / Open-saved-entry / Remove
  HoldActions. `RadialHoldMenuOverlay` + `HoldAction` became public for
  this. (6) **Keyboard-aware full-screen editors** — the share-card
  full-screen editor root, the Enlarge writing sheet and the chapter-note
  Enlarge dialog all got `imePadding()` on their root Column/Box (content
  lifts above the keyboard; the card re-zooms into the visible space)
  and the text area is now a `weight(1f).verticalScroll` wrapper around
  the RichTextEditor (weight inside a scrollable Column is illegal, so
  the editor itself lost its weight). (7) **One-shot rich-text tools**
  (RichTextEditor.kt, shared by the capture formats): applying B/I/
  highlight/size to a SELECTION no longer arms the sticky `pending*`
  flags (the toolbar never stays lit after one change); tapping a tool
  with a collapsed caret still arms it for the next typed characters.
- **v3xx2x — text history EVERYWHERE + restore modes + tree redesign
  (user follow-up 2026-09-09).** (1) **Every editor joins the feed** —
  `RichTextEditor` gained `historyField` / `historyResetKey` params: when
  set it runs `rememberTextHistoryCapture` itself, shows a compact
  `TextHistoryPill` in its tool dock and hosts its own `TextHistoryBrowser`
  (restore writes back through `onRichTextChange`, rich spans kept for
  add modes via `rebaseSpans`, cleared for replace). Wired into every
  capture format: FieldNotes' three sections (What I observed / What
  surprised me / What I want to learn next), Marginalia journal (My
  thoughts), ReelNotes (Film review), SoundBite (Soundbite note + Quick
  title via `PaperLineField.historyField`, which got the same pill +
  capture + browser in its label row), GalleryWall caption and every
  Quote card. The Save-your-take shared session note (`SessionNoteFloatingPill`)
  gained its own pill in the popup header + capture + browser (restores
  cap at the 240-char note limit). Existing hosts (TopicShareCard,
  TopicRevealScreen) keep their own pills. (2) **Restore modes** —
  `TextHistoryBrowser` takes `currentText` and its `onRestore` callback
  now passes a `TextHistoryRestoreMode` (REPLACE / ADD_TOP / ADD_BOTTOM):
  an empty field restores instantly; a field with text opens the
  settings-style chooser (frosted rows with warm icon tiles: Add above /
  Add below / Replace). (3) **Tree redesign** — the confusing
  paragraph-branch tree is replaced by field → session grouping
  (`buildHistoryTree`): snapshots group by FIELD then by EDIT SESSION
  (gap > 20 min starts a new one); each field is a settings-style card
  (✦ glyph + Playfair heading + snapshot count + rule, frosted 20dp card
  with hairline-divided sessions), each version a node with dot +
  connector, time, a +/− line-change badge and the FULL text (2 lines) —
  plus the shared `HistoryActionsRow`. List rows + tree cards share the
  frosted surface language of the Settings sub-pages.
- **v3xx22 — CI fix + dark-mode sheet icons + full series UI (user
  follow-up).** (1) **CI fix** (CabinetShelves.kt — the pasted
  `compileDebug/ReleaseKotlin` failure): the seven shelf-art composables
  (`StarArt`/`ReadingArt`/`BooksArt`/`MountainArt`/`NotesArt`/
  `WindowArt`/`PhotosArt`) are now `BoxScope.` extensions — their
  top-level `.align(...)` calls need the outer Box's scope, and `V2ShelfArt`
  already wraps them in a `Box(modifier)` — and the book-spine width is
  `(22 + i * 8).dp` (was `22.dp + i * 8.dp` = Int × Dp mismatch). (2)
  **Dark-mode sheet action icons** (TopicRevealScreen.kt): new
  `sheetActionIconTone(ink, variant, alpha)` — in dark mode the
  unselected sheet actions (favorite hearts on book chapters / album
  tracks / series episodes, the read/watched toggles, the note chips, the
  album LISTEN dropdown glyphs) resolve the full-strength cover-ink twin
  (0.88 lightness) instead of `onSurfaceVariant` + dimmed alphas that
  vanished into the 0.20–0.27 cover-tinted dark washes; light mode keeps
  `variant` exactly as before. The chapter "Add a note" field icons
  (note glyph, Expand chip, Share chip) lift to full alpha in dark mode
  too. (3) **Series UI — every series topic shows a section**:
  `SeriesInfoSection` no longer early-returns on an empty episode guide —
  every SERIES reveal renders the poster + synopsis card (they used to
  show NOTHING); the count meta, episode-title preview and "View the
  episode list →" footer are gated on `hasEpisodes`. (4) **Episode
  chips**: a `SeriesEpisodeChips` row (S1E1 key + title chips, mirroring
  the album TRACKS chips) jumps the episode-list sheet straight to an
  episode (`onEpisodeClick` → `selectedSeriesEpisode` +
  `showSeriesSheet`, the sheet's `episode` param pre-expands it). (5)
  **Series icons, not books**: the reveal poster card + chips wear
  `CurioIcons.Movies` (clapperboard) and the series notes accordion its
  own `CurioIcons.Movie` — a series never wears the book glyph.
- **v355 — book/series notes sheets: no close button, no hint copy, rating
  below the author, tick-free read state.** User: "never add cross close
  button in a bottom sheet… remove it from the book synopsis sheet… remove
  the your rating row and just put the rating just below author name and
  use a different icon for rating… remove that tap a chapter to read its
  note hint… don't add useless notes… remove that arrow icon from chapter
  chips… when i tap read the read status is really bad, i don't like the
  active state with the tick icon… when expanded to read in dark mode it's
  a little bad, fix it".
  - **No cross close:** the ✕ button is GONE from the book + series notes
    sheets (swipe-down/back dismisses) — the category-picker and album
    sheets were already the no-close model.
  - **Rating below the author:** the "REVIEWS & YOUR RATING" card is
    REMOVED; the sheet header shows the fetched Google Books average AND
    the user's own rating ("4.2 · yours 4 / 5") just below the author
    name, under ONE award-ribbon glyph (`CurioIcons.WorkspacePremium`,
    which also replaced the ★ on the reveal hero chip + synopsis-card
    chip). `BookRatingPicker`/`BookGlyph` were deleted with the card; the
    `bookRatingVisibleState` pref API stays dormant.
  - **No hint copy:** the "tap a chapter/episode to read its notes" suffix
    is gone from the progress rail (just "N chapters"); the "keyless
    Google Books rating" and "Rating hidden · tap the book to show it"
    labels are gone too.
  - **Read state, no tick:** the leading chip always shows the
    chapter/episode NUMBER — a read/watched row tints the disc softly in
    the accent (18% fill + accent number + accent ring) instead of the
    loud ✓; the row keeps its solid-accent border + surfaceAlt fill. The
    Mark-read FoldedCorner toggle is a SOLID accent disc + onAccent icon
    in EVERY state — the old open+read flip to an onAccent disc read as a
    hole punched in the accent row in dark mode (fixed).
  - **No chevron:** the ▼/▲ expand arrow is removed from the
    chapter/episode rows (rows still expand on tap).
- **v356 — book-cover providers: iTunes FIRST + LibraryThing (keyed).**
  User: "add i tunes provider for books and make that first and then
  fallback other, and also add librarything too".
  - `BookCoverProvider` enum reordered BEST-FIRST: **ITUNES →
    GOOGLE_BOOKS → OPEN_LIBRARY → LIBRARY_THING**;
    `AppPreferences.getBookCoverProvider` default is now `ITUNES` (fresh
    installs / unknown stored values).
  - **iTunes** (`itunesThumbnail`): keyless
    `itunes.apple.com/search?term=…&entity=ebook`, upscales the 100px
    artwork token to 600px, picks the best title+author match via the
    shared `matchScore` heuristic (same as the album resolver).
  - **Standard Ebooks** (`standardEbooksCover`, v426b): keyless, and the
    door that answers for a CLASSIC — its OPDS feed's own search
    (`standardebooks.org/feeds/opds/all?query=…&per-page=5`) with the same
    strict matching `StandardEbooksFetch` uses for a blurb. iTunes' ebook
    search is a shop that often has no nineteenth-century novel at all, and
    Open Library's title endpoint serves a 1×1 placeholder for many, so
    this is the keyless source that produces a REAL cover for one.
  - **LibraryThing** (`libraryThingCover`): ISBN-gated + key-gated —
    resolves the ISBN via a keyless Google Books volume search
    (`industryIdentifiers`), then
    `covers.librarything.com/devkey/{key}/large/isbn/{isbn}`; needs
    `LIBRARY_THING_API_KEY` (optional BuildConfig via env/secret,
    `.env.example` + `android.yml` wired). No key = the hub row is hidden
    and lookups fall through to the keyless providers.
  - **Reveal poster live fallback** now cascades iTunes → Google Books →
    LibraryThing (only when keyed) instead of Google Books alone; v426b
    put **Standard Ebooks** between iTunes and LibraryThing, so a classic
    that the shop does not carry resolves keyless on the reveal itself.
  - Hub provider picker follows the enum order (iTunes first); a stored
    LIBRARY_THING pick with no key configured silently falls back to
    iTunes.
- **v357 — album LISTEN pill: Apple Music deep-links to the REAL album.**
  User: "for album open links in apple music it's searching only… but for
  song it perfectly opens the song inside the album, so can't we do the
  same for albums too, and maybe for other services too".
  - The album sheet's LISTEN → Apple Music entry now resolves the album via
    `resolveAppleMusicItemUrl(topic)` (iTunes lookup, entity=album →
    `collectionViewUrl` → `music://…/album/{id}` — the same native deep
    link songs already use) and falls back to the search link only when
    the lookup misses. Runs off a `rememberCoroutineScope` in
    `AlbumNotesSheet`.
  - Other services stay search deep links: Spotify needs OAuth
    (client-credentials), Deezer now requires login/app registration, and
    YouTube Music / Amazon Music expose no keyless album-ID lookup — there
    is no keyless album deep link for them. (Spotify album deep links
    could be added behind optional CLIENT_ID/CLIENT_SECRET secrets if ever
    wanted.)
- **v358 — Spotify deep links (optional keys).** User: "yes app spotify deep
  links too" (follow-up to v357). `resolveSpotifyItemUrl(topic)` in
  ExploreSearch.kt runs the Spotify client-credentials flow — POST
  `accounts.spotify.com/api/token` with Basic `id:secret` → access token,
  then `api.spotify.com/v1/search?q=…&type=album|track|artist` — and
  returns `https://open.spotify.com/{type}/{id}` for the best title+artist
  match. Requires optional `SPOTIFY_CLIENT_ID` + `SPOTIFY_CLIENT_SECRET`
  BuildConfig values (env/secret, `.env.example` + `android.yml` wired);
  unset = null → callers keep the search link. Wired into BOTH the album
  sheet's LISTEN pill (Spotify entry) and the Explore "Listen in" flow for
  Spotify (albums/tracks/artists).
- **v359 — share-card favorite-tracks strip: Minimal rule moves with the
  fact; Collage + Signature strip tokens + placement fixed.** User: "for the
  minimal card style there's a line above the quick fact or custom fact
  which should move along with the move, also the favorite tracks box isn't
  matching in collage, and signature styles so fix it, and properly place
  them so that they don't overlap".
  - **Minimal fact group:** the thick accent rule above the quick/custom
    fact is now INSIDE the same `moveFact(move)` group as the gap + fact
    text (one `Column`), so the rule travels with the box on drag and stays
    glued while the box resizes — it can no longer desync and float where
    the fact used to be.
  - **Collage strip:** the bottom is a dark band under the torn seam, so
    the strip is now a translucent DARK slip (black 34% + white hairline
    border + white serif type + gold-tape heart) instead of the
    warm-white tone box that clashed with the band; reads on the band AND
    anywhere the user drags it.
  - **Signature/Custom strip:** backgrounds vary per category (paper-white
    and dark scenes alike), so the strip is a dark stamp pill (black 55% +
    white border + white type + accent heart) instead of a tone-palette box
    that clashed with the card's own colors.
  - **Placement:** Collage and Signature/Custom strips raised from
    ~40/58dp to **bottom 80dp (classic 84dp)** so they clear the collage's
    torn-seam footer wave and the signature footer line — still movable via
    the editor's F-handle.
- **v360 — share-card meta row moves edge-to-edge; book-cover fetch
  verifies real covers, skips covered, survives exit; series data batch
  2.** User: "during card editing the author year etc that info move is
  bad… the separate one is bad like it have restriction to move to too much
  to the sides while others don't so fix it, also i don't see the new
  series synopsis style layout inside, also then the book fetching in
  experiment fix so when i tap fetch it fetches from the start when the
  books already have the cover and when i exit the page during fetch it
  cancels and restart from start, also many book covers are not getting
  fetched like a handful of dust, a perfect spy and many more".
  - **Meta row clamp (share card):** the author/year info row used a padded
    clamp (`mPad = 18f`) that kept it ~18px off every card edge while
    title/fact/badge/cover could travel to the true edges. It now clamps
    edge-to-edge exactly like the other elements (base-rect clamp, so the
    range never shrinks as the row travels); the magnet/alignment helpers
    unchanged.
  - **Book-cover fetch — placeholder-aware verification:** the bulk fetch
    now VERIFIES a cover decodes to a real image (>= 40px short edge)
    instead of trusting "it loaded". Open Library serves a 1x1 GIF with
    HTTP 200 for missing covers, so dead authored URLs ("A Handful of
    Dust", "A Perfect Spy" and many more — every one of the 796 books has
    an authored imageUrl that short-circuited the providers) silently
    counted as successes. `fetchAll` now builds candidate URLs per book
    (stored-verified first, then authored, then the provider cascade:
    chosen provider first, then iTunes → Google Books → Open Library →
    LibraryThing) and the first candidate that decodes at real size wins
    and is remembered via `setBookCoverUrl`.
  - **Skip covered + resume:** new persisted `bookCoverDoneState` (book
    names whose covers VERIFIED) — "Fetch all covers" skips those and only
    runs the remaining/failed books, so re-tapping resumes where the last
    run stopped instead of restarting at book #1.
  - **Survives leaving the page:** the fetch job + progress state moved out
    of the screen into `BookCoverFetchSession` (a process-lifetime
    `CoroutineScope` in BookCoverFetch.kt). Leaving the hub no longer
    cancels the run; re-entering shows the live progress and Cancel still
    works. Hub + per-row retry route through the session.
  - **Reveal poster:** `BookCoverPoster` also skips tiny/placeholder
    successes (its `onSuccess` checks decoded size, bumping the candidate
    index like a 404 would) and its candidate order is now verified-first,
    so a hub-fetched real cover beats a dead authored URL on the reveal
    too. `coverCandidates` ordering updated the same way (verified stored
    URL first; authored second; Open Library title guess last).
  - **Series data batch 2** (`tools/enrich_series_batch2.py`, mirrors
    batch 1): synopsis + full episode lists added for **Sherlock, Squid
    Game, The Last of Us, Severance, Wednesday** — the reveal's series card
    (poster + synopsis preview + episode-list sheet) previously only
    rendered for the 5 batch-1 shows; now 10 shows carry the layout.
- **v370 — share-card smart auto-fit + corner whole-box scale (default
  ON).** `TopicShareCard.kt` auto-grows the fact box for long quick/custom
  facts and lifts/shrinks the title, per style:
  - **Smart auto-fit (`smartAutoFitDelta`, was `shareAutoFitDelta`):**
    default ON (`AppPreferences.shareAutoFitState`); intensity presets
    Balanced/Compact/Airy per style (`ShareCardMove.autoFitIntensity`);
    once the user moves/resizes the fact box, auto-fit hands it over
    ("manual wins") and the first-grab seed carries height/offsets/title
    scale over so the box never jumps. **Per-style clamps** (this pass):
    the fact never rises where something sits above it — Collage (pill
    above fact, title hugs the top edge: no nudge at all), Editorial
    (masthead rules + byline: neither title nor fact rises; fact grows
    down to the colophon); Clean/Minimal facts are bottom-anchored and
    grow up naturally while the title lifts a little (24–28dp) to clear
    them; Paper/Vinyl/Signature/Custom get a modest ≤14dp clamp. **Long
    titles shrink** (`autoTitleScale`, per-style thresholds, ~0.70–0.95×)
    when the fact needs the room — never shrunk if the user placed the
    title. Auto-fit now watches the CUSTOM fact too
    (`maxOf(quickLen, chapterFact.length)`).
  - **Corner whole-box grip (`CornerResizeHandle`):** scales width AND
    height together from the selected box's bottom-right corner — title
    and fact already had it, now also on the info row (meta) and the
    favorite-tracks strip; "Whole box" slider shares the same math and the
    fact sliders reach 6x (grip to 8x) so tall 9:16 cards can expand.
  - **Long-text font floors raised** on Collage (10→10.5–11sp), Clean
    (8→8.5–9.5sp), Editorial (8.5→9–10.5sp) and Minimal (8.5→9–11sp) so
    the expanded box keeps the text readable.
- **v371 — share-card fact formats + writing box + album/series covers +
  chapter-note sharing + collision-push + real cover colours.**
  - **Fact formats (quick AND custom fact, `ShareCardMove.factFormat` /
    `factDropCap`):** every style's fact renders through `FactBody`;
    Condensed tightens line-height/word spacing, Book page splits into
    two justified columns (`BookPageText`, half the text per column,
    word-boundary breaks) and Editorial sets a large drop-cap initial
    (first letter or first word, `EditorialDropCapBlock`). Restores by
    name from saved edits.
  - **Writing box below the tools** (quick / custom / chapter review) with
    an **Enlarge** button opening a full white writing sheet — gives the
    custom fact and chapter review a real input, not just the inline
    caret. In the Book Notes sheet the chapter-note field got the same
    treatment: an EXPAND full-white dialog (2000 chars, shares the same
    AppPreferences slot) and a SHARE button that opens the share card
    pre-seeded as a Chapter review (`TopicShareSheet.seedReviewText` /
    `seedReviewChapter`, which WIN over restored edits).
  - **Album + series covers in the share editor:** `isAlbumTopic` /
    `isSeriesTopic` flags (wired at all three TopicShareSheet call sites:
    reveal, Share Hub, entry detail) enable the Fetch / Refetch / Remove
    row (gallery stays book-only); albums resolve via
    `AlbumArtFetch.resolveArtworkUrl` (iTunes → MusicBrainz), series via
    `SeriesPosterFetch.resolvePosterUrl` (TVMaze → iTunes). With a cover
    present the card switches to COVER-SIDE layout (`COVER_SIDE_SHIFT`):
    the cover anchors LEFT like the synopsis page, the title shifts right
    and wraps ~26% narrower and shrinks ~0.9× so nothing overlaps.
  - **Collision-push fact drag:** dragging the fact box no longer always
    drags the title + info row along — they move ONLY when the fact
    actually touches them (within a 4dp gap, `touches()`), so moving the
    fact away leaves them put and moving it into them pushes them cleanly.
  - **Real cover colours (`extractCoverSwatches`):** the androidx Palette
    median-cut guess is replaced by a direct pixel-vote HSL histogram on
    the decoded artwork — `CoverSwatches.dominant` is the true majority
    colour and `notesSheetPalette` keys the sheet wash off it (vibrant
    still drives the accent). Album/series notes sheets now feed the
    RESOLVED artwork/poster URL into the extractor (they used the empty
    authored imageUrl, so they always fell back to the category tint).
    Expanded chapter/episode rows are a soft accent tint + border instead
    of a solid accent slab.
  - **Google Books removed as a cover source:** `BookCoverProvider`
    dropped GOOGLE_BOOKS everywhere (hub picker, reveal live-fallback,
    share-sheet cascade) — books now fall back iTunes → Open Library;
    the keyless Google Books RATINGS + ISBN lookups stay untouched.
  - **Fact-font auto-shrink (`ShareAutoFitDelta.factScale`):** the
    auto-adjuster now scales the FACT font down as the text grows
    (`autoFactScale`, per-style floors 0.86–0.90, starts ~150 chars) on
    top of each style's built-in length curve (which alone only kicks in
    at 180–350+ chars). Applied via `effectiveBodyScale = bodyScale *
    autoFit.factScale * move.factScale` in TopicShareCard, so the export
    and the inline typing field (which follows `liveFactStyle`) match.
    The Balanced growth curve's low end was lowered (starts at 90 chars
    with 1.15×) so medium facts get room earlier. `factScale` is captured
    into `ShareCardMove` on the first grab (seed) and counts as "touched",
    so the handoff doesn't make the text jump back to full size or clip.
- **v372 — full-screen text editor + corner whole-box ZOOM + selection
  chrome hiding + rich-text-lite (underline/highlight).** User: "the
  corner expand button only behaves as a width/height button not as an
  enlarge for the whole box along with the text, also remove the selected
  outline when editing inline, and add a full screen button for the share
  card with just text format and text editing features in full screen with
  only one pill and dropdown style… background color would be category
  tint" (answers: box + text zoom together; hide chrome in TEXT-edit mode
  only — the handle returns when exiting; full screen shows the card
  itself large, whole-text per-element tools in ONE menu).
  - **Corner drag = true ZOOM (`ShareCardMove.factZoom`):** dragging the
    fact's bottom-right corner now scales the box AND the fact font
    together (photo-zoom, 0.5–4×) instead of only growing the line budget
    (which was capped at the card width, so full-width facts visibly did
    nothing). `effectiveBodyScale` multiplies `move.factZoom` so the
    preview, export and typing caret all scale together; persisted per
    style and cleared by Reset. `factZoom != 1f` counts as "touched" so
    auto-fit hands over.
  - **Selection chrome hides while typing:** in `ArrangeableCard`, when
    `factEditMode` is on the fact's border goes transparent, the
    tap-to-select layer is skipped, and the `FACT` case skips the
    MoveHandle + CornerResizeHandle entirely — the caret shows where you
    type and nothing fights the keyboard; all chrome returns when text
    editing ends. (The edit tool pill toggles `factEditMode` on/off.)
  - **Full-screen editor (`fullscreenEdit`, `TopicShareSheet`):** a
    Full screen button (secondary pill, sits next to Customise) opens a
    `Dialog(usePlatformDefaultWidth = false)` painting the whole display
    in `lerp(surface, accent, 0.12f)` (category-tint wash). The card is
    rendered LARGE and centered (`BoxWithConstraints`, aspect preserved)
    via the SAME `ArrangeableCard`/`TopicShareCard` pair as the sheet
    pager, so inline text editing is precise and the export matches the
    full-screen preview exactly. Floating tools are minimal: Close pill
    (left) + one Text pill (right) opening a single `DropdownMenu` with
    EVERY text tool in one place — font (13), size slider, B/I/U +
    highlight swatch row (5 presets, tap again to clear), alignment, and
    the fact format + drop-cap pickers. The menu reads the current
    `selectedResizeTarget` (title vs fact) and arms fact editing from the
    same "Edit fact text" pill as the sheet.
  - **Rich-text-lite per element:** `ShareCardMove` gained `factUnderline`
    / `factHighlight` and `titleUnderline` / `titleHighlight` (Color?),
    applied in `factBodyStyle` / `titleStyle` via `TextDecoration` and
    `background`, persisted with the move (`toArgb` / `Color(it)`). This
    is whole-element formatting (like Bold/Italic), not per-word spans.
- **v373 — cover shapes + independent Whole-box scale + full-screen fixes.**
  User: "the size of the book cover was perfect in share card in that
  commit [ea47f1b] … album covers are square not rectangular and its
  stretching it to rectangular so fix that … the full screen button it
  looks transparent and doesnt match the customise button look … keep the
  full screen button when editing too in customise … inside the full
  screen edit the share card preview … looks stretched and not accurate
  of what it was looking before in the bottom sheet … add the dimension
  change button … add the box size editor … the whole box should not
  depend on the width or height its separate and independent"
  (ask answers: keep the side layout with the SMALL cover; album square;
  series stays 2:3).
  - **Cover sizes + no album stretch:** `TopicShareCard` gained
    `isSquareCover` (sheet passes `isAlbumTopic` at ALL call sites incl.
    the Save/Share export lambdas): books/series render the 2:3 jacket at
    the old perfect 44×66, albums render square 66×66 — square art is no
    longer squeezed into the 92×136 rectangle. The v370b side layout
    STAYS, but the title shift/width-crop/title-shrink are now DERIVED
    from the cover's real width (`coverW.value + 16f`, aspect-relative
    width factor, mild title shrink) instead of the fixed 108f/0.74/0.9
    tuned for the 92dp cover, so the smaller jacket hugs the title.
  - **Whole-box independence (`titleBoxScale` / `factBoxScale` /
    `favBoxScale`):** the "Whole box" slider no longer borrows the height
    fraction as its backing value — each element carries its OWN scale
    (1f default, persisted/parsed) applied in a new `boxScaledMove` that
    multiplies BOTH width and height fractions (on top of auto-fit, width
    still clamps at 1f). Dragging width/height no longer yanks the
    Whole-box thumb; the corner-grip base math divides by the scale; the
    scales count as "touched" so smart auto-fit hands over; Reset clears
    them with the rest of the moves.
  - **Full screen button:** restyled to MATCH the Customise pill
    (`surfaceContainerHigh` + `onSurfaceVariant` — the old
    `secondaryContainer` chip read as transparent) and stays visible
    while editing (Customise itself still hides mid-edit).
  - **Full-screen preview = exact zoom of the sheet card:** the card now
    renders at the sheet's own 280dp base inside a
    `CompositionLocalProvider(LocalDensity provides Density(density*zoom,
    fontScale*zoom))` so text sizes, spacing and placements scale
    TOGETHER (the old approach laid the dp content out in a much bigger
    box, so text stayed tiny and `SpaceBetween` re-spread the layout).
    Drag deltas convert through the same scaled density, so persisted
    offsets stay in card-local dp.
  - **Full screen tools:** a Dimensions pill (AspectRatio glyph + live
    `aspect.label` 3:4/9:16) sits next to the Text pill and toggles the
    aspect; the Text dropdown gained a Box size section (width / height /
    whole-box `SizeSliderColumn`s for the selected title or fact) and the
    menu Column is now vertically scrollable.
- **v374 — smart fit rework (whole-card, slider channels only) + auto-tone
  fix + quick-fact-under-progress.** User: "why have you placed the smart
  auto fit in there [the size tool] and also the auto fit densities remove
  them … the smart fit should be differnt toggle … consideres the entire
  share card no just the quick fact box … it should use the sliders size
  etc for adjustments not its own differnt size logic … it will use the
  quick fact text size and decrase it and it will incrase the fact height
  and fact wiidth … smart collide detection so in automatic defult card
  desotn go outsite of the card … the auto colors … when the level unlocked
  color is availabe the auto color is picking that which should not happen
  … make quick fact work in the same way [as custom fact under progress]"
  - **Smart fit = own toggle, slider channels only.** The old
    `ShareAutoFitDelta` (dy/titleDy/titleScale/factScale + the intensity
    presets + hidden `autoFactScale`/`autoTitleScale` floors) is GONE. The
    new delta is `heightFrac` / `widthFrac` / `textScale` — applied through
    the SAME channels the user's sliders drive: `effectiveMove
    .factHeightFrac`/`.factWidthFrac` and `effectiveBodyScale` (the
    bodyScale the Size slider sets). `autoFitGrow(len)` is ONE length curve
    (no presets; `autoFitIntensity` removed from `ShareCardMove` +
    persist/parse) and `factFitBudget(style, aspect)` is the WHOLE-CARD
    collision budget — max box-height × + min text-scale per design
    (Collage's fixed band barely grows; Editorial's byline→colophon slot a
    little; bottom-anchored Clean/Minimal grow up into the free middle;
    Paper/Vinyl/Signature/Custom mid-flow). Past the cap the box stops
    growing and the TEXT shrinks by exactly the overflow ratio (clamped to
    the style floor) — nothing else on the card is moved or shrunk, so
    nothing leaves the card. Manual box edits still win; the first-grab
    seed captures the fit's textScale into `move.factScale` so the handoff
    doesn't pop.
  - **UI:** the Smart auto-fit switch + intensity pills are REMOVED from
    the Size tool; Smart fit is its own toolbar tool (glyph
    `photo_size_select_large`, verified in the bundled icon font) whose
    panel holds just the on/off switch.
  - **Auto-tone fix:** `paletteFor`'s automatic rotation now cycles ONLY
    the always-available base tones (`unlockLevel == null`); a level-locked
    premium tone never appears automatically — only when explicitly picked
    in the Tone tool (the override index still maps into the unlocked pool).
  - **Quick fact under Reading progress:** `chapterFactForCard` now returns
    `editedFact ?: quick.text` when the quick fact is active with progress
    on (same stacking as the custom fact), and the content pills keep
    progress ON when picking the Quick fact instead of turning it off.
- **v375 — notes-sheet palette memory + selected-state contrast + rich-text
  fact editing (Save-your-take style) + full-screen editor fixes.** User:
  "in book albumn etc buttom sheet, colors, so the page number albumn number
  icon isnt visible when selected and also the like button and also the book
  icon and same for albumns series and when selected its even more bad … and
  also the color pallete should be remeberstae like even when after resrart
  it goes back to defakt and switches after a second when it should be
  instant … the enlarged text box text editing … add the save you text style
  format text editing with highlights etc which stays when sharing too … in
  the full screen card editor the highligh bold italic etc are inside the
  tool box when they should show as floating when selecting the text and
  only apply to them if text are selected bnot entirely always … same for
  the full screen text editor too" (+ the crash/glitch report from
  `11e566a5`: infinite-constraint crash + slider/swipe fight in full
  screen).
  - **Palette INSTANT + remembered:** cover swatches cache per artwork URL
    and albums/series persist their resolved artwork URL
    (`AppPreferences` `KEY_COVER_SWATCH_CACHE` / `KEY_SHEET_ART_URLS` +
    `coverSwatchesToArgbs`/`coverSwatchesFromArgbs` in CoverPalette.kt).
    All three notes sheets seed colors synchronously on first composition
    (no default-then-switch flash after restart) and only refresh the
    cache in the background.
  - **Selected-state contrast:** `notesSheetPalette`'s `onAccent` is keyed
    to HSL lightness (≥0.52 → dark ink) instead of linear luminance, so
    pale light-cover accents get readable ink on solid-accent rows/chips;
    hearts/number chips/icons on open (tinted) rows use the palette ink /
    full-strength `onAccent` instead of vanishing accent-on-accent.
  - **Rich-text fact editing:** `TopicShareCard` gained a `factSpans`
    (`List<TextSpan>`) param threaded through EVERY style → `FactBody`
    (+ the BOOK two-column + EDITORIAL drop-cap splitters re-slice runs via
    `richSlice`), rendered via `buildRichAnnotated` with the amber
    `ShareFactMarker`. Sheet state holds `editedFactSpans`/`customSpans`
    (persisted via `spansToJson`/`spansFromJson`), `routeFactChange`
    rebases on inline typing, the ENLARGE dialog and the chapter-note
    dialog host `RichTextEditor`, and Save/Share exports pass the spans.
    Chapter-review cards shift spans past the "CH n · title" chip prefix
    (`reviewChipPrefixLen` + `shiftSpans`) so runs land on the note text.
  - **Full-screen selection formatting:** the full-screen card's fact field
    keeps a REAL `TextFieldValue` selection (`richFactTfv`,
    `richFactTools` on the ArrangeableCard call) and floats a non-focusable
    `Popup` B / I / highlight bar anchored to the live selection caret;
    taps toggle `RichFlag`s over exactly [s,e) via `toggleFactSelectionFormat`
    (`spansFullyCovered`/`toggleSpanFlag` made internal in RichTextEditor.kt).
    Whole-element toggles remain in the Text panel (keep both).
  - **Crash/glitch fixes (`11e566a5`):** the full-screen Text dropdown is
    now an INLINE bounded-height panel (`heightIn(max=300)` + scroll)
    under the top bar in a Dialog COLUMN layout — no more DropdownMenu
    verticalScroll under infinite height (the reported crash) and no popup
    scrollable stealing slider/swipe drags.
- **v376 — GLUED covers (cover rides the title block on Paper/Vinyl/Clean/
  Editorial/Minimal) + auto title-lift collision.** User: "the cover
  position isnt right in share card, in paper design its top left corner
  and not to the side of the title and author and also the space can be
  decrase, in vinyls the text quick fact should move a little down and the
  cover for albumn and the title itself should be a little down too do it
  doesnt verlap on category pill, in editorial its near perfect, the cover
  should move a little don matching the title starting point and the title
  can move a little closer to the cover, same for minimal too … clean a
  similiar the positioning is good the ittle move closer … the cover and
  albumn should move together also when the quick fact gets moved along
  with title the cover should move too by collide logic … in the auto text
  the collision should work when the height of the quick fact gets tuned
  manually and the title moves up automatically along with cover if present
  and it comes don if with the slider it gets lowered and same for width
  chnages too" — ask answer: "Glue cover to the title block; title drag
  moves cover too and also the title move during collision moves it too;
  Saved lift".
  - **GLUED cover plumbing:** `TopicShareCard` computes `glueCoverStyle`
    (Paper/Vinyl/Neumorphic/Editorial/Minimal) and passes `gluedCover`
    (artwork, `coverW`/`coverH`) into those five card styles; the old
    side-layout overlay + `coverSideShift`/`coverTitleWidthFactor`/
    `coverTitleScale` layoutMove stays ONLY for Signature/Custom (and the
    no-cover per-style corner pockets stay for the overlay styles). The new
    `GluedCover` composable renders `BookCoverBadge` inside the style's own
    title block as the leading item of a Row that carries the title's move
    (`glueTitleMove` — drag offset + auto lift only, NEVER font scale/width
    crop: `titleSize` keeps scale+width on the text), so the jacket always
    sits exactly beside the title wherever that design's flow puts it, and
    rides every title drag / collision push / auto lift. The cover keeps
    its OWN fine-position offset (`coverDx`/`coverDy`) inside the group.
  - **Per-style placement:** Paper (MiddleContent) → cover | title+author
    Row beside the headline area with a snug `CoverTitleGap` (12dp);
    Vinyl → cover | title+byline Row, title block lowered (18dp spacer
    with cover) so it clears the category pill, quick fact nudged down
    (14dp spacer); Editorial → cover | headline+deck Row aligned to the
    headline start (topPad 5); Minimal → cover | title+byline Row (topPad
    4); Clean/Neumorphic → cover beside the CENTERED title block
    (`CenterStart`, CenterVertically).
  - **Auto title-lift collision (`move.titleLift`, dp):** inside
    `ArrangeableCard`'s edit overlay a `LaunchedEffect` (editMode,
    non-quote) watches the MEASURED title/fact rects + the fact box
    fractions; when a manually grown fact box (height/width/whole-box
    sliders or the corner grip) would draw over the title, it computes the
    needed lift from the title's UN-lifted base (measured bottom + current
    lift → converges in one step), clamps to the card's top edge and calls
    `onMove(move.copy(titleLift = …))`. `moveTitle`/`titleShift`/
    `glueTitleMove` apply the lift as an upward offset everywhere (sheet
    preview, export) and `titleLift` is parsed/persisted in the move JSON
    (0 = none). Lowering the box drops the needed lift → the title settles
    back. The TITLE drag handle's clamp math adds the lift back so the
    natural base stays correct.
- **v378 — notes-sheet polish + share-editor collision/fit/zoom fixes.**
  User (big mixed batch): "in book buttom sheet of synopsis still the
  number text and the read icon isnt looking right and also the enlarge
  icon. also remove that big divider and the 2 out of 4 chapter read show
  progress in that devider and make the Chapter 1st letter capital. and in
  albumn buttom seet remove the cross buttom. and during card editing when
  i move the title and make it or try to put it above the quik fact it
  starts to glitchy and crazy glitchy repeated so fix it, that collison
  logic only works when i move the quick fact not the title also in full
  screen editor add a reset button to reset the layout and yes the reset
  button only resets the layout not the texts chnages also by default the
  book colors are that darkmidnight color can u fix it it should use its
  own ategory tint auto color the golden color maybe. also that weird
  glitchy animationhappens when i try to close the buttom sheet of share
  card during editing… and the smart fit still isnt usin the tool text
  adjustments but rather uses its own coz hen i check the text size its
  still at 1. and also in collage the quick fact box can be move a little
  up in smart auto fit and making its text smaller too. so fix it too, and
  in paper the text gets too much small even though the height can be
  expanded to fit the text… and in the full screen editor the preview of
  the card is still not accurate now it look sa little more zommed and cut
  from below… and also the bold option sometimes doesnt show in card and
  the boxes outline is having a glithin in full screen editor fix it it
  looks inaccurate, and to edit the text add the double tap to edi tin
  inline mode. and also remove that whole box adjuster and its icon from
  the orner too".
  - **Book/album notes sheets.** The book sheet's `NotesSheetTopHairline`
    accent rule under the drag handle is GONE; the reading-progress rail
    is ONE capitalized label ("2 of 4 Chapters read" / "4 Chapters") —
    the 4dp progress bar and duplicate "N / M" counter are removed.
    Chapter row titles render with `replaceFirstChar` caps; on the
    accent-tinted OPEN rows the number disc (read rows), the Mark-read
    FoldedCorner toggle and the note Enlarge chip switch to the sheet INK
    (accent-on-accent washed out). The album sheet's ✕ close `Surface` is
    removed (v355 no-close model, like book/series).
  - **Title drags never auto-lift.** New `titleGrabbed` state in
    `ArrangeableCard`: while the TITLE handle drags, the v376 auto-lift
    `LaunchedEffect` returns early (and it's a key, so a pending run
    cancels). The lift exists ONLY for fact-box growth — a title dragged
    over/above the fact follows the finger; the old effect recomputed a
    counter-lift every frame and fought the drag ("crazy glitchy").
  - **Full-screen Reset Layout.** `ShareCardMove.resetLayout()` extension
    zeroes positions / width·height fractions / whole-box scales /
    `titleLift` but PRESERVES every text edit (fonts, aligns,
    bold/italic/underline/highlight, `titleScale`/`factScale`/
    `factZoom`, fact format + drop cap). A "Layout" pill in the
    full-screen top bar calls `updateMove(move.resetLayout())`.
  - **Auto tone matches the topic.** `paletteFor(accent, null)` no longer
    does `accent.hashCode() % size` over ALL always-available tones
    (which included the dark Onyx/Noir/Wine/Deep Sea/Cocoa variants — the
    unexplained "midnight" default). Auto picks the closest of the four
    LIGHT base tones (Warm Rose / Soft Sage / Golden Ochre / Deep Indigo)
    by RGB distance to the topic accent, so books' golden accent lands on
    Golden Ochre. Explicit Tone picks unchanged.
  - **Sheet gestures off while editing.** `ModalBottomSheet` now passes
    `sheetGesturesEnabled = !editMode`: while customising, the sheet
    cannot be dragged at all (no blocked-dismiss spring-back that froze
    the tools/swipes); Done/back drops edit mode and normal swipe-close
    returns. `confirmValueChange` + `onDismissRequest` guards stay.
  - **Smart fit visible in the size slider.** The quick-fact Size slider
    (sheet panel + full screen) now shows `bodyScale × fit.textScale ×
    factScale × factZoom` — the RENDERED size — instead of raw `bodyScale`
    (which read 1× while the card sat at 0.8×). Dragging writes the base
    back through the fit (WYSIWYG); Reset restores base 1× + factScale/
    factZoom 1.
  - **Fit budgets.** PAPER gets its own `factFitBudget` arm (portrait 2.0
    height cap / 0.96 text floor) so long facts EXPAND the box instead of
    shrinking type to 0.8×; COLLAGE keeps a small cap but its text floor
    drops to 0.70–0.75 so long facts shrink inside the band.
  - **Full-screen preview zoom bug.** The density trick multiplied BOTH
    density AND fontScale by the zoom → sp text scaled TWICE (zoom²),
    reading oversized and cutting off below. The provider now scales
    DENSITY only (`Density(density * zoom, fontScale)`), so dp AND sp
    scale once and the preview matches the 280dp sheet card exactly. This
    also explains the "bold doesn't show / glitchy box" reports: the
    chrome and the bar lived in the over-zoomed coordinate space.
  - **Corner whole-box grip removed.** `CornerResizeHandle` (the
    bottom-right corner icon that scaled the whole box) is deleted along
    with its four call sites (title/fact/meta/fav) — the Crop tool's
    width/height/whole-box sliders own sizing, and the stray corner icon
    is what read as a "box outline glitch" in full screen. `factZoom` /
    whole-box scales stay (persisted + slider-driven).
  - **Double-tap inline editing.** A `combinedClickable` on the fact
    tap-to-select layer fires `onRequestInlineFactEdit` (new
    `ArrangeableCard` param) on DOUBLE-TAP; the sheet's
    `requestFactInlineEdit()` arms the transparent field exactly like the
    Edit-text tool (auto-converting the default quick fact to a custom
    fact), so editing is a double-tap away in the sheet and full screen.
- **v379 — full-screen text-tool polish: icon-only B/I/U, floating-bar
  Underline, justify + book column-gap, permanent tool captions.** User:
  "in full scren text editor the icon for b i and underline is weird fix
  it. also add underline in tool bar too also full screen the highliter in
  the buttom sheet of that text editor is bad remove it. and also add
  justify aling format too abd in bok page fact layout add space adjust
  between that. and also show the hint text for tool nme below always in
  the tool bar. and fix some more functinal issues properly analyse it…".
  - **Underline is a first-class rich-text flag.** `TextSpan` gains
    `underline: Boolean = false` (CaptureData) and every rich-text
    codec round-trips it: `buildRichAnnotated` renders it as a text
    decoration, `extractRichSpans` reads it back, `merged()`/
    `rebaseSpans()` preserve it, the card's JSON `spansToJson`/
    `spansFromJson` write/read a `"u"` key, and `richSlice` carries it
    into the two-column book split. A dedicated `toggleSpanUnderline` /
    `spansUnderlineCovered` pair in RichTextEditor toggles the flag on a
    selection WITHOUT touching bold/italic/highlight (Save-your-take's
    dock keeps its RichFlag-only toolbar).
  - **Floating bar.** The selection bar in `ArrangeableCard` (B / I /
    highlight) gains a U button driven by the new `onToggleFactUnderline`
    channel; the bar widened 132→172dp to fit four tools.
  - **Whole-element format.** The sheet's Format tool shows whole-element
    Bold / Italic + Underline (title + fact only — meta/badge carry no
    underline field); the full-screen Text panel's B/I/U became ICON-ONLY
    round `EditToolPill`s (the old label pills doubled glyph + letter),
    and the full-screen whole-element Highlight SWATCH ROW is removed
    (highlight lives only on the floating bar over a live selection).
  - **Justify.** The full-screen Text panel's Align row adds Justify
    (the sheet's Align tool already had it).
  - **Book-page column gap.** `ShareCardMove.factGutter` (multiplier, 1f =
    12dp) is persisted/parsed/reset alongside the other layout state;
    `FactBody`/`BookPageText` thread `gutterFrac` through to the
    12dp·frac Spacer between the two columns. Sliders appear under the
    fact-layout pickers (BOOK format only) in BOTH the sheet's Align tool
    and the full-screen Text panel.
  - **Tool captions are permanent.** `ToolWithCaption` lost its `show`
    gate — every toolbar pill (Text · Size · Crop · Fit · Font · Color ·
    Adjust · Align · Format · Content, plus the live ratio / Signature
    state labels) shows its tiny name under the icon AT ALL TIMES.
  - **v379c — the selection bar lives on every editing surface.** The two
    bottom-sheet `ArrangeableCard` calls (pager + single-style) now pass
    `richFactTools = true` + the fact spans + the format/underline toggle
    channels exactly like the full-screen dialog — so a live text
    selection on the card floats B / I / U / highlight over the letters
    in the sheet preview too. Quotes + reading progress stay plain (no
    spans; the bar's enable states gate on the non-null callbacks). The
    rich field's seed/comment blocks were updated to match.
  - **v379b CI fix.** The selection bar's visibility gate became
    `format != null || underline != null`, which silently killed Kotlin's
    smart-cast of `onFormatFactSelection`; the B / I / highlight buttons
    now safe-invoke (`?.invoke`) and gate on their own enable state.
  - **v379d — size-channel cleanup, dark-premium Paper ink, AUTO-LAYOUT
    pill.** User: "remove the orphaned factZoom… Reset Layout restores
    smart fit… the smart fit should also use the text box height
    adjuster… add a spark round pill floating in a card corner to auto
    smart-fit… nothing gets overlapped, the user's edits stay but it
    adjusts… if too much text the card can automatically become 9:16…
    tap again for another arrangement… fixing its colour in dark mode
    (some share-fact text is still dark)…".
    - **factZoom deleted.** Nothing wrote it since the v378 corner-grip
      removal; it only multiplied the render + divided the size-slider
      write-backs. Parsing now folds a legacy `factZoom` into
      `factScale` (same channel), and the field + all render/divisor/
      persistence uses are gone — one invisible factor fewer.
    - **Reset Layout clears the fit seed.** `resetLayout()` no longer
      carries `factScale` forward: that field is only ever written by
      the smart-fit handoff seed or the auto-layout pill (the Size tool
      drives `bodyScale`), so keeping it left auto-shrunk text on a
      default box AND permanently disabled smart fit (the seed reads as
      "manually touched"). Reset now returns natural auto-fit behaviour.
    - **Box-height thumb is honest.** The sheet Crop tool + full-screen
      Box section show `factHeightFrac × fit.heightFrac` (the height the
      card really renders, matching the text thumb since v378) and write
      the base back through the fit on drag.
    - **Cover title-shrink folded into the slider.** Signature / Custom
      cards with a side cover multiply the title by `coverTitleScale`
      (~0.9–0.97) at render; the Title-size thumbs (sheet + full screen)
      now display the effective size and write the base back.
    - **Paper fact/quote ink.** `qStyle`/`frostStyle` in `MiddleContent`
      copied `MaterialTheme.typography` and therefore inherited the APP
      theme's `onSurface` — dark-on-dark on dark premium palettes (Paper
      backgrounds follow the tone) and wrong in dark mode. Both styles
      now carry explicit `palette.ink`.
    - **AUTO-LAYOUT sparkle pill.** `AutoLayoutPill` floats at each
      card's top-end in BOTH modes (resting + editing). Tap calls
      `runAutoLayout()` in the sheet: `autoLayoutPlan()` (per style /
      aspect / current text length) commits a whole-card fit into the
      per-style move — box height + whole-fact text ride the SAME
      channels the sliders drive (`factHeightFrac`, `factScale`, format),
      so the export matches and Reset Layout clears it. Attempts cycle
      standard fit →      condensed → book columns (long facts) → tall 9:16 when a 3:4 card
      overflows a fully-fitted budget; attempts that would change nothing
      are auto-skipped so every tap does something visible. Manual
      title/fact/cover position drags are never overwritten.
    - **v379e — text-first fit, re-fit toggle, lift unit fix, dead-title-
      height, Whole-box slider gone.** User: "the whole box slider is
      still showing, remove it; by default the fact height should be
      100% and the text size shrinks by length; toggling smart fit ON
      again should fix the box after manual edits; the title still
      glitches up and down against the quick-fact box; hide Title height
      when the title fits one line; the quick-fact lines look different
      between full screen and the sheet…"
      - `autoFitShape()` replaces the box-first fit: TEXT-FIRST — the
        fact box stays at its full height and `textScale` shrinks
        inversely with the length curve; the box grows only when that
        shrink passes the design's text floor (then capped by the
        budget). `smartAutoFitDelta` is the toggle/touched gate around
        it; `autoLayoutPlan` layers the pill attempts on it.
      - The Smart-fit switch now CLEARS a manual box on re-enable
        (`factWidthFrac/factHeightFrac/factScale/factBoxScale → 1`, the
        position drags stay) so the fit can "fix the box" after manual
        edits; panel copy rewritten.
      - **titleLift unit bug**: the collision lift was computed in PX
        (boundsInWindow rects) but applied as DP in `moveTitle`
        (`(titleDy - titleLift).dp`) — on a 3× screen the title was
        shoved ~3× too far, clamped against the card top, and bounced
        back (the "glitchy up and down"). The lift effect now converts
        the measured px overlap to DP before writing, adds a 0.5dp
        dead-zone, and is guarded by BOTH `titleGrabbed` and a new
        `factGrabbed` (the fact handle's live push owns the drag; the
        measured lift must not fight it).
      - Whole-box sliders removed from the Crop tool and the full-screen
        Box section (title/fact/fav rows + their v373 comments); the
        scales stay on the model for Reset/persistence but the UI is
        width/height only.
      - Title-height slider renders only when the displayed title
        (`editedTitleOrDisplay`) exceeds ~22 chars — it caps wrapped
        lines, so it is hidden for short one-line titles instead of
        reading as a dead control.
      - **frostInk auto-contrast.** User: "the text colours for dark
        background cards are all black now, use white so they don't look
        bad". `ShareCardPalette.frostInk()` returns near-white when the
        fact pane's BLENDED colour is dark (bgMid lerped 35% toward
        FrostPane's white overlay — the effective backdrop on premium
        dark tones) and near-black when it's light; Paper's quote +
        frost styles now use it instead of palette.ink/theme onSurface.
        Title/meta already used palette.ink (light on the dark tones).
    - **v380 — hand-placed title owns its spot (overlap freedom).** User:
      "the title-fact collision glitch is still there; if I move the title
      onto/inside the quick-fact box I should be able to, no problem."
      New persisted `ShareCardMove.titlePlaced` flag: the auto-lift
      effect (v376/v379e) returns early for a hand-placed title, so a
      drag that parks the title over the fact stays put — the lift only
      ever rescues a title that was NEVER dragged (natural or nudged
      aside by the FACT handle's push), i.e. slider-grown fact boxes. A
      title drag end FOLDS any prior lift into `titleDy` (`dy -= lift`,
      `lift = 0`) so the title freezes exactly where the finger left it
      with no snap; Reset layout clears the flag (back to automatic).
      Old saves parse `titlePlaced` as false → unchanged behaviour.
- **v381 — pill 9:16 flip reads bigger; opaque sparkle pill; Collage dark
  tones + blended bottom.** (Pill ask answers + collage fixes; Signature
  default-with-cover deferred — open question with the user.)
  - The pill's tall plan (3:4 box-capped → 9:16) now commits a LONGER
    fact box + LARGER text too (`heightFrac ≈ tallBudget×1.4 in
    [1.6,3.2]`, `factScale 1.18`) so the flip buys readability; the old
    flip kept the 3:4 box/text on the tall canvas (wasted height).
  - `AutoLayoutPill` is now an OPAQUE surface with a 1dp ring and NO
    shadow elevation — the translucent fill + elevation painted a soft
    dark halo over busy cards ("solid fill glitch").
  - **Collage dark-tone pass.** On dark premium palettes (Midnight,
    Ember…) the collage used the LIGHT accent raw for the lower field →
    the lightest colour sat at the card's bottom and the white fact text
    vanished on it. New `darkTone` (bgBase luminance < 0.55) branch:
    field/band/pill = accent/accentDark LERPED TOWARD BLACK (0.58–0.62)
    then muted so layers always darken top→bottom; `tornEdge` pulls
    toward the accent so the seam reads against near-black paper;
    polaroid caption = fixed warm-dark ink (palette.ink is near-white on
    dark tones → invisible on the white polaroid). Light tones unchanged.
  - Collage bottom tear blended: the sin-edged band + solid footer wedge
    became ONE feathered zone — low-amplitude wave path + vertical
    gradient whose top starts TRANSPARENT (no hard line) and a footer
    wave filled with a transparent→deep gradient (soft melt).
- **v383 — Link share + fact box grows past the design column.** Two
  user asks: (1) "when sharing the topic add a deep link style share": the
  share sheet's actions row gained a **Link** pill that opens a small
  caption editor (pre-seeded with the topic name); posting it shares an
  ACTION_SEND text = your message + the URL. The URL comes from
  `com.curio.app.data.shareLinkForTopic`: albums/artists/songs build the
  search URL of the music service picked in Settings (re-read at share
  time), everything else the topic's Google search. Callers (reveal,
  entry detail, Share Hub) pass it via the new `TopicShareSheet.shareLinkUrl`
  param; the caption dialog shows the link being sent, then dismisses the
  sheet. (2) "the box width can be expanded": the Fact-width slider now
  runs 0.3x–1.2x (was capped 1.0). `moveFact` renders through a custom
  layout: at <= 1x it measures exactly like the old fillMaxWidth (child
  wraps naturally, placed at 0) so no card changes; past 1x it measures
  the pane at columnWidth × frac and recentres the overhang, letting the
  box eat the design's side gutters that used to sit empty. Render-path
  caps in `effectiveMove`/`boxScaledMove` rose 1f → 1.2f so the setting
  survives smart-fit/box-scale folding; the untouched auto-fit seed still
  clamps at 1x. Also folded in: the v382 CI compile fix (Signature SIDE
  centred its cover with `Modifier.align` inside a nested Box, which the
  compiler rejected — now `Box(contentAlignment = Alignment.Center)`).
- **v382 — Signature covers are GLUED (no more cover over the badge/title).**
  User: "the signature styles are bad with the cover — it overlaps the
  badge/title". Signature joined the glued-cover set (`glueCoverStyle` + the
  cover flows as `gluedCover` into SignatureCard) and dropped off the
  generic top-left overlay (`coverSlot` now serves Custom only; Signature
  no longer gets the synthetic coverSideShift title offset via
  `layoutMove`). Inside SignatureCard: `TitleText`/`MetaText` gained a
  `glued` flag (title uses `titleSize`, meta skips `titleShift` — the Row
  owns the title drag via `glueTitleMove`), and a new `TitleAndMeta(centered)`
  renders the jacket + title + meta as one Row wherever the design's flow
  puts the title (STANDARD / BOTTOM left-flow, CENTERED / OVERLAY /
  POSTER centred; SIDE stacks the cover centred above the title in the
  narrow left panel). No-cover paths are untouched. Signature background
  treatment is a separate upcoming user round.
- **v377 — share-card editor declutter: design switching via the card,
  tool captions, No-fact eye-cross, fact layout under Align.** User: "the
  style button should only show when signature style is active and tapping
  it should switch between the 2 differnt signature style no need for the
  design options below in tool bar, the ratio of 3:4 9:12 dimention
  chnage make it chnage without closing the other tool if its open, and
  remove that tap a thing to select swipe for another design text, and
  instead show a small text per tool, like ratio, font, style, crop,
  color, these hint text below tools when selected, the layout f standard
  condenced book page etc move them inside the alingment tool, and the
  content one make the no fact just eye cross icon so it hides the fact
  box with just icon no text" — ask answers: tool name captions under the
  OPEN tool only; No-fact eye-cross in the panel AND the bottom toggle;
  ratio caption always under its icon.
  - **Design switching = the card carousel only.** The Style/Design tool
    panel is GONE (`toolOpen == "style"` arm deleted; the now-unused
    `setStyle` + `scope` removed). Designs change by swiping the
    HorizontalPager carousel. The toolbar shows a Style toggle ONLY while
    the current design is SIGNATURE: one tap flips `classicDesign`
    between the two Signature looks instantly (no panel), and its caption
    under the icon reads the ACTIVE variant (Current/Classic). The design
    label + dots above the carousel stay.
  - **Per-tool captions (`ToolWithCaption`).** Each toolbar pill can carry
    a TINY name under it while its panel is open (Text / Size / Crop /
    Fit / Font / Color / Adjust / Align / Format / Content) — the label
    moves as the user switches tools and nothing shows when nothing is
    open. The RATIO pill keeps an ALWAYS-ON caption showing the active
    size (`aspect.label`: "3:4"/"9:16"), and its toggle no longer closes
    an open tool panel (the old `toolOpen = null` is removed). The "Tap a
    thing to select · swipe for another design" hint text is deleted.
  - **Fact LAYOUT presets live under Alignment.** Standard / Condensed /
    Book page / Editorial + the Editorial-only Drop cap row moved out of
    the Bold/Italic ("format") tool into the ALIGN tool's panel (rendered
    when the fact is selected, under the alignment pills); the full-screen
    editor's adjacent section header renamed "Fact layout" for parity.
  - **No fact = eye-cross icon only.** New `CurioIcons.VisibilityOff`
    glyph added to the bundled Material Symbols subset via fontTools rlig
    surgery ON the existing subset font (+1 rlig ligature name, 0 lost, 0
    cmap changes, ~300 bytes — the full-font pyftsubset path explodes the
    glyph closure on Material Symbols' first-letter ligature coverage, so
    the glyph outline + rlig record were copied straight into the current
    font). The Content panel's "No fact" option is now an icon-ONLY 38dp
    `IconPill` (eye-cross, no words) and the bottom content toggle shows
    just the eye-cross + chevron when No fact is active (the "No fact"
    text is hidden).
  - **Copy trims:** the Smart-fit panel's paragraph and the Adjust panel's
    footer line each shortened to one line.
- **v361 — keyed defaults + Clear-covers button; CI fix for the reveal
  poster.** User: "fix it, and also i added spotify key and library thing
  api as well… does the api is used in the apk build from pr, use that by
  default, also in book fetching add a button to clear all book covers so
  testing other provider is easy" (the CI failure was the v360 reveal
  poster's Coil listener using the wrong signature).
  - **CI fix:** `BookCoverPoster`'s `AsyncImage.onSuccess` used Coil 2.7's
    (request, result) lambda; 2.7's AsyncImage callback takes a single
    `AsyncImagePainter.State.Success` (drawable lives on `state.result`).
    Fixed — placeholder skip works again and the build compiles.
**v426b — THE ART AND MUSIC SHEETS GAIN A DOOR EACH.** `MuseumFetch.kt` is the **Cleveland Museum of Art** (keyless, no key and no registration at all): `work(title, artist)` for the artwork sheet, `worksBy(maker, limit)` for an artist or painter's own list, and `makerBio(maker)` for the museum's own prose about a maker Wikipedia has no article for. `ArtworkFetch` asks all **three** sources together now (Met ∥ Cleveland ∥ Wikipedia, so the wait is the slowest rather than their sum) and merges Met → Cleveland → Wikipedia in that order; `worksBy` appends Cleveland's attributed rows after the Met's (deduped by title) — which is what stops an artist's page being empty just because the Met holds nothing by them. **Verified live before wiring:** Cleveland's endpoint answers `{info, data:[…]}`, its `_web.jpg` came back 200 `image/jpeg` 232 KB, and its **`artist=` parameter does NOT filter** (asked for Monet, answered Copley, Bellows and Eakins) — so every maker lookup here filters rows itself by the `creators[]` line, taking the name before the bracket. **The Art Institute of Chicago is deliberately NOT wired:** keyless, but its `iiif_url` images answer **403 to a non-browser client** (same id the API itself returned, 843px and 400px alike), and a source whose pictures cannot be fetched is worse than one that is never asked. **`SongArtFetch` gained the album side's second door:** a MusicBrainz RECORDING search (a release-group's title is the album's, so the album door cannot answer for a track) → the release it sits on → Cover Art Archive `front-500`, asked only when iTunes found nothing, behind MusicBrainz's own 1 request/second + real-User-Agent rules. Deezer's search is **blocked anonymously** (`data: []` with a non-zero `total`, verified) and Audius is an indie catalogue that answers a mainstream query with other people's remixes, so neither is used — see `Prompt.md`.

**v426b — THE COMIC VINE DOOR, AND THE PUBLIC-DOMAIN ONE.** Two new sources, and one honest deletion. **(1) `ComicVineFetch.kt`** is the keyed comics door: `comicvine.gamespot.com/api/search/?resources=volume` read with `field_list` (only the six fields a shelf row wears), answered into `MangaFetch.Hit` by `asHit()`, with a 1.1s `pace()` because the free key allows 1 request/second and 200/resource/hour, and a JSON-level check (`status_code == 1`) because this API answers an invalid key with **HTTP 401 and a perfectly good body** — reading the HTTP code would have called every failure "unreachable". `BuildConfig.COMIC_VINE_API_KEY`, optional, `.env.example` + both workflows wired; unset = `available` is false and no request is ever made. **Marvel's own developer API is DISCONTINUED** (its keys answer nothing; DC never published one) and its scheme wanted a private key in the APK, so there is no Marvel/DC key to add: Comic Vine carries both publishers, named on every volume. **(2) `StandardEbooksFetch.kt`** is keyless and answers for classics: an Atom OPDS feed read for `title` / `author` / `summary` / `cover.jpg`, with strict title matching (normalised, length-ratio guarded) because a full-text feed search will happily answer a query with four other books — verified against the live feed (five classics resolved with real covers, a manga title correctly returned nothing). It is a `BookCoverFetch.BookCoverProvider` row (`STANDARD_EBOOKS`) and the description door in `BookEnrichment` when everything else left the about-text blank. **(3) The by-hand gap is closed:** a comics row added by hand (typed, or read out of a file name) never went through a search, so `BookEnrichment.comicsPass` now asks `MangaFetch` for its own kind and fills only what is EMPTY (cover, author, synopsis, length — never overwriting what the member has). A `COMIC` searched in the shelf also keeps its kind now (it used to be saved as a plain book) and falls back to Open Library only when the comics sources answered nothing, so a keyless build loses nothing.

  - **Keyed providers are used by default (yes, in PR builds too):** the
    GitHub Actions workflow already passes `LIBRARY_THING_API_KEY`,
    `SPOTIFY_CLIENT_ID`/`SPOTIFY_CLIENT_SECRET` (and GOOGLE_BOOKS_API_KEY)
    as env into the PR/push build step, which bakes them into BuildConfig
    — so the APK built from a PR includes them (note: fork PRs don't get
    secrets; same-repo PRs and pushes do). New default: when the
    LibraryThing key is configured and no provider was ever picked,
    `getBookCoverProvider` now returns LIBRARY_THING instead of ITUNES, so
    a keyed install gets the highest-quality ISBN covers immediately.
    Spotify deep links were already on-by-default when the keys are set
    (resolveSpotifyItemUrl returns non-null → deep link; null → search).
  - **Clear all covers (hub):** new "Clear all covers" button in the
    Book-covers hub with a confirm dialog — `BookCoverFetch.clearAllCovers`
    wipes the stored/verified/failed cover records
    (`AppPreferences.clearBookCovers`) AND clears the shared Coil disk
    cache, so the next "Fetch all covers" re-resolves every book from
    scratch (the old provider's verified URLs would otherwise keep winning
    the candidate order). Ratings are preserved. Enabled only when there is
    something to clear.
- **v362 — personal chapter notes; chapter roadmap logged.** User: "the
  chapter like have no use, so any suggestion? what can we do" → chose
  **personal chapter notes** for now + logged the bigger chapter ideas in
  ANALYSIS.md (which is now gitignored — it's a local working doc, not
  committed/pushed). Album-link year-matching was explicitly declined
  ("nah keep it like that now") and the iTunes-API-key question answered
  (see below).
  - **Personal chapter notes:** the expanded chapter panel in the
    book-notes sheet gains a quiet one-line note field (edit_note glyph,
    "Add a note…" placeholder) that saves as you type — blank text
    removes the note. Stored per book → chapter number in
    `AppPreferences.bookChapterNotesState`
    (KEY_BOOK_CHAPTER_NOTES: JSON object of objects), so notes survive
    re-grouping and restarts; reactive, so the field reflects the saved
    note instantly. Capped at 240 chars. Styled for both the open (accent
    wash) and closed (surface) chapter panels.
  - **Chapter roadmap (ANALYSIS.md §10):** Chapter → related topics
    (per-chapter `relatedTopics` schema field + chip row; data batch after
    schema) and synopsis enrichment ("some books synopsis doesn't feel
    like synopsis" — rewrite pass over books.json) are planned; chapter
    search / continue-where-you-left-off / share-a-chapter are later ideas.
  - **iTunes API key answer:** the iTunes Search API is already KEYLESS and
    free (rate-limited ~20 calls/min per Apple's docs) — there is no free
    API key to add. The only keyed Apple option is the paid Apple Music API
    (MusicKit, $99/yr Apple Developer Program) which is overkill for album
    deep links. So album links stay keyless; no BuildConfig plumbing
    needed.
- **v367 — book synopsis QUALITY pass (connected prose).** User: "analyse
  the books properly i feel some synopsis doesnt feel connected like i
  ont know, proper synopsis". Audit of the 60 rewritten synopses found
  the problem: the non-fiction ones were LISTY, sentences of the form
  "he shows X; he explores Y; he examines Z" with colon-catalogues of
  topics (Predictably Irrational, Nudge, Ego Is the Enemy, Stillness Is
  the Key, The Tipping Point, Algorithms to Live By, The 48 Laws of
  Power, So Good They Can't Ignore You), and the shortest pre-existing
  synopses in the catalog (320-494 chars) had the same disconnected
  feel. `tools/enrich_book_synopses_quality1.py` rewrote 32 synopses as
  flowing, connected prose: each opens with a human hook (Ariely's
  bandage-burn story, Duckworth's classroom question, Tolle's night of
  despair), develops the argument through natural transitions instead of
  catalogues, and closes by tying the whole together. 8 of my own
  listy rewrites + 24 of the worst short ones (The Dispossessed, The
  Princess Bride, The Jungle, Sister Carrie, Uncle Tom's Cabin, Love in
  the Time of Cholera, Gideon the Ninth, The Souls of Black Folk, Ball
  Lightning, The Tale of Genji, The Four Agreements, 12 Rules for Life,
  The Dark Forest, Solaris, The Lion the Witch and the Wardrobe, The
  Last Unicorn, The Power of Now, The Prophet, Homo Deus, The Art of
  War, Outliers, Grit, The Midnight Library, Quiet). 92 books total now
  carry quality synopses; the quality bar going forward is connected
  prose,  never topic catalogues.
- **v368 — notes-sheet cover colors accurate; top glow follows the
  palette.** User: "the color extraction and applying in the bottom sheet
  of series books and album synopsis are not very accurate and also that
  top style like a glow that doesnt change color too. so can u fix it, in
  app". Root causes (diagnosed with a Python Palette simulation on real
  covers): (1) `notesSheetPalette` keyed the sheet wash off the DARK
  swatch — near-grey on most covers, and a near-grey's HSL hue is
  numerically noisy, so colorful covers washed out to neutral; (2) the
  light-mode wash was capped at 0.28 saturation / 0.93 lightness — a
  whisper of cream; (3) near-black covers (Open Library dark art) returned
  achromatic swatches → grey sheet + invisible black accent; (4)
  `NotesSheetTopHairline` hardcoded `cat.themedAccent()`, so the top glow
  never changed color. Fixes: `fetchCoverSwatches` decodes at 256px and
  quantizes 24 buckets (the default 16 merges a cover's hues into a muddy
  average); `notesSheetPalette` keeps the vibrant-family accent only when
  it carries real hue (sat >= 0.14, else the most-saturated swatch, else
  null → category wash), pulls accent lightness into a usable band (floor
  0.22→0.30, light-mode ceiling 0.72→0.60) so progress bars/selected
  rows/pills stay visible, and drives the wash hue from the ACCENT with a
  stronger visible tint (light `(s·0.55)` in [0.16, 0.42] at 0.90
  lightness; dark cap 0.45 at 0.20); the hairline now takes the sheet's
  RESOLVED accent in all three sheets (book / album / series). Verified
  with the simulation: On the Road now wears its tan, The Little Prince a
  visible periwinkle, and black/minimal covers fall back to the category
  wash.
- **v366 — book synopsis batch 2 + series batch 4.** Continuation of
  v365 (user: "yup go ahead"). `tools/enrich_book_synopses_batch2.py`
  rewrote the next 30 shortest synopses (Up from Slavery, Influence,
  Little House on the Prairie, Freakonomics, The Ocean at the End of the
  Lane, My Antonia, The Awakening, The 48 Laws of Power, Watership Down,
  The Republic, The Tombs of Atuan, Lessons in Chemistry, Ethan Frome,
  The Tipping Point, Pedro Paramo, Tom Sawyer, Assassin's Apprentice, All
  Systems Red, Sapiens, Of Mice and Men, Rendezvous with Rama, etc.) into
  detailed web-verified synopses (1073-1327 chars, no em/en dashes),
  60 books now enriched. `tools/enrich_series_batch4.py` added synopsis
  + first-season episodes to The Good Place (13), Ted Lasso (10), The
  Mandalorian (8), Succession (10) and True Detective (8), titles
  verified against episode guides, 20 shows now carry episode data. Also
  fixed the CI failure from v364: the album resolver's three new block-
  body helpers (resolveArtistId, bestAlbumFromCatalog,
  bestAlbumFromSearch) ended in a bare expression (null / bestUrl)
  instead of an explicit `return`, which fails Kotlin compilation
  ("Missing return statement").
- **v365 — book synopsis enrichment batch 1 + series enrichment batch 3.**
  User: "after that do series enrichment and before that do the book
  synopsis fix as many books dont have proper synopsis and start with 30
  per bath proper web searched sunopsis detailed. no need to ask me
  anything first". All 796 books had synopses, but the shortest were thin
  one-paragraph blurbs ("doesn't feel like a synopsis").
  - **Books (batch 1 of 30):** `tools/enrich_book_synopses_batch1.py`
    rewrote the 30 shortest synopses (Nudge, Predictably Irrational,
    Meditations, A Suitable Boy, The Graveyard Book, Small Gods, Wild,
    Our Town, The Invention of Morel, Tarzan of the Apes, The Elegant
    Universe, etc.) into detailed, web-verified synopses in the house
    style (author + context, then a real plot/content walkthrough,
    1003-1317 chars, no em/en dashes). Episode-level data was verified
    against live searches (e.g. A Suitable Boy's four families and
    suitors; Our Town's three acts; Anxious People's cashless-bank
    setup; The Wire's S1 title order). Diff is exactly 30 synopsis
    fields (books.json is 2-space indent, no trailing newline; the tool
    matches that format).
  - **Series (batch 3):** `tools/enrich_series_batch3.py` added
    synopsis + first-season episodes to Breaking Bad (7), Stranger
    Things (8), Game of Thrones (10), The Wire (13) and The Sopranos
    (13), titles verified against episode guides. 15 shows now carry
    episode data; batches 1 (Chernobyl, Band of Brothers, The Queen's
    Gambit, Watchmen, Fleabag, Freaks and Geeks) and 2 (Sherlock,
    Squid Game, The Last of Us, Severance, Wednesday) were earlier.
    More batches can follow (Seinfeld, Twin Peaks, The X-Files, Lost,
    The Office, Friends, etc.).
- **v364 — album deep links resolve through the ARTIST'S REAL catalog.**
  User: "the direct albumn open links are not accurate and it gives no
  result like oens blank apple music, so any way to fix". Root cause
  (verified live against the iTunes API): `resolveAppleMusicItemUrl` took
  `results[0]` from a `limit=1` album search with NO relevance check, and
  the search API's ranking is unreliable for famous catalogs — the real
  "Nevermind" (Nirvana) and "The Dark Side of the Moon" (Pink Floyd)
  don't appear in the top 25 hits AT ALL, so deep links opened tribute
  albums, same-title singles by other artists, or a different album
  entirely ("The Wall"), which reads as a wrong or blank page.
  - **New album path (ExploreSearch.kt):** resolve the ARTIST ID via a
    `entity=musicArtist` search (exact name match), pull the artist's OWN
    album catalog (`/lookup?id={artistId}&entity=album&limit=200`), and
    pick the best title match with a strict score gate
    (`appleAlbumScore`: 35 = exact title + exact artist, 30 = exact
    title, 25 = containment-fuzzy + exact artist; reject < 25),
    preferring `trackCount > 0` (a trackless preorder renders as a blank
    page). Verified: Nevermind 35 ✓, Dark Side of the Moon 35 ✓, Sgt.
    Pepper 35 ✓, Led Zeppelin IV → "(Remastered)" 25 ✓.
  - **Fallback:** a SCORED search (`limit=10`, same >= 25 gate) replaces
    blind `results[0]` for albums whose artist name differs from the
    byline; when nothing clears the gate the resolver returns null and
    the caller falls back to the plain SEARCH link — a search is always
    better than a wrong album. Song/artist paths are untouched (songs
    were verified working; the song `trackViewUrl` route is preserved).
    Both the reveal's "Listen in" and the album sheet's LISTEN pill use
    this resolver, so both are fixed.
  - **Spotify (same build):** the query is now field-scoped and quoted
    (`album:"..." artist:"..."` / `track:"..."` / `artist:"..."`) and
    the match gate rises from score > 0 to >= 2 (reject weak fuzzy-only
    hits → search fallback), so a same-title item by another artist can't
    win.
- **v363 — fetch is provider-EXCLUSIVE; Clear also wipes memory cache.**
  User: "the clear all covers doesnt work … i feel like the fetching of
  covers is still uses the old api … when i select the provider and tap
  fetch then it should only fetch from that for the fetch button not for
  the fallbacks." Two root causes, both in `BookCoverFetch`:
  - **Provider-only fetch:** `resolveVerifiedCoverUrl` (used by the bulk
    fetch) previously built candidates as stored-URL → authored imageUrl
    → chosen provider → cascade of ALL providers. So the fetch button was
    never a pure provider test — books with real authored covers kept
    their identical image no matter which provider was selected (authored
    always won first), which is exactly why everything looked "the same
    old API" after clearing + re-fetching with a new provider. Now the
    fetch considers ONLY the chosen provider's resolved URL(s) (iTunes
    search / Google Books search / Open Library title URL / LibraryThing
    ISBN URL); a book that provider can't serve is marked failed.
    Verification (`loadsRealImage`, 40px-min short edge) still applies,
    and the reveal poster / share card / hub tiles keep their own
    authored-first fallback via `coverCandidates` (the "auto loading" the
    user said is fine).
  - **Clear now clears BOTH Coil caches:** `clearAllCovers` also calls
    `memoryCache?.clear()` — disk-only clearing left the decoded covers
    serving instantly from memory, so the hub's strip looked unchanged and
    "Clear" seemed broken.
- **v323 — picker hold actions become a gooey RADIAL menu; share-card
  Tone tool + 6 new tones; pet shop toys/games; quest fixes.** (1)
  **Radial hold menu** (`features/picker/RadialHoldMenu.kt`): the old
  single morphing pill is replaced by a fluid ring — gooey blobs (a
  blur+alpha-contrast `RenderEffect` chain on API 31+, soft circles below)
  well out of the PRESS POINT, settle into a ring of crisp glass discs, and
  stay live-switchable: drag anywhere (the nearest disc highlights with hit
  slop), release over a disc to pick, release over nothing to cancel. NO
  dark scrim — the menu exists only while the finger is down. The gesture
  lives on each tile as `Modifier.radialHoldMenu(HoldSession(onOpen,
  onMove, onEnd, onTap))` (own-press with the system long-press timeout,
  consumes the pointer once open so the clickable underneath never fires);
  the visuals live in `RadialHoldMenuOverlay`, which the sheet/screen
  renders at the held anchor (clamped inside). Wiring: `NewPickerTile` /
  `NewPinnedPill` / `NewMixCard` / `ContinueExploringSection` (sheet) and
  `BrowseTabContent` / `MixesTabContent` / `PinsTabContent` / `BrowseMixRow`
  (Browse page) pass a `HoldSession`; `CategoryOptionPill` / `MixOptionPill`
  (now `internal`, shared with Browse) render `RadialHoldMenuOverlay` with
  the shared `holdCursor`/`holdEnd` sheet state. The classic page's
  hold-to-multi-select is NOT a radial menu — `NewPickerTile` keeps a plain
  `onLongClick` for it. The old `HoldActionsPill` is now dead code (keep or
  delete with care — nothing references it). (2) **Share-card Tone tool**
  (`TopicShareSheet`): a Palette tool pill opens a swatch row of every tone
  the player has UNLOCKED (`unlockedToneCount = 4 + LevelRewards.
  unlockedPaletteCount`); `TopicShareCard.toneIndex: Int?` picks one (null
  = Auto rotation), `ShareCardPalette` gained a `name`; Save/Share export
  the picked tone. (3) **Six new tones** added to `curatedTones` (Ocean 12,
  Rose Gold 18, Moss 25, Storm 35, Pearl 45, Sunburst 50) + matching
  `LevelRewards` entries; `RewardKind.GAME` unlocks pet games (12 Ball
  fetch, 25 Star catch, 35 Bubble storm). (4) **Pet shop toys** —
  `PetOutfits.Games` (id/name/glyph/price/levelRequired/rewardId/tagline),
  `AppPreferences.getOwnedGames/buyGame` (`ownedGamesState`), shop section
  with Buy → Play (`CurioPet.notePlay` + haptic); 4 new outfits (Polka
  Bowtie, Sun Hat, Curio Glasses, Tail Puff). (5) **Quest fixes** — the
  Home "Today's quests" strip (`HomeDailyStrip`) is REMOVED; the "Bonus
  quests unlocked!" line hides once both bonus quests are claimed; the
  "Try a new lane" daily PINS its lane at the 4 AM rollover
  (`CurioQuests.dailyLaneState` + `dailyDiscoveryLane()`, stored by
  `CategoryId.name`) so the shown lane and the completion check always
  agree (the old live `leastEngaged()` re-resolution could re-aim the
  quest away mid-day).
- **v327 — reveal poster restored, star-rating view, pet-outfit
  unequip, editor floating cluster, Collage tone, radial menu fixes.**
  (1) **Book covers**: `BookCoverPoster` restored to the ce892baa form
  the user confirmed renders correctly — the `AsyncImage` IS the root
  (`modifier.clip(RoundedCornerShape(8.dp))`, size/shadow passed in by
  callers, `ContentScale.Crop`, `onError` walks `coverCandidates`); the
  gradient-Box + `matchParentSize` wrapper is GONE (it painted over or
  swallowed the cover). (2) **Star rating VIEW**: `BookCoverFetch.
  fetchRatingFor(name, author)` — keyless Google Books `intitle:` query,
  returns the first `averageRating`; the reveal hero (BOOKS only) shows a
  ★ chip next to the author pill, fetching on demand when
  `AppPreferences.bookRatingsState[bookName]` is missing and caching via
  `setBookRating`. (3) **Pet shop unequip**: tapping an EQUIPPED outfit
  now removes it (`setEquippedOutfit(context, null)`) — the pill label is
  "Equipped · tap to remove"; owned-outfit tap equips. (4) **Editor
  floating cluster**: while `editMode`, the Edit-text circle (when
  `selectedResizeTarget == FACT`) and Reset circle float NEXT to the
  Done button (`Modifier.align(BottomEnd)` Row) instead of hiding
  mid-scroll in the tool row; the scrollable tool row no longer holds
  Edit-text/Reset/Done. (5) **Collage tone**: `CollageCard` now derives
  its paper/field/band/ink colors from the picked `ShareCardPalette`
  (`topCream = palette.bgBase`, `bottomSage = palette.accent`, `bottomDark
  = palette.accentDark`, `inkDark = palette.ink`, `tornEdge = palette.bgMid`,
  `sagePill = palette.accentDark`) — the Tone tool now customizes Collage.
  (6) **Radial menu**: discs 46→36dp (`DISC_DP`), release-over-nothing
  LINGERS (420ms) then fades out (260ms `dismissAlpha` on the overlay
  root) instead of vanishing instantly, and the goo-blob layer converts
  ring positions to overlay-LOCAL coords before animating (`centerPx`)
  — the blobs were morphing from local `cp` to ROOT `p`, landing offset
  from the crisp discs and breaking the circle.
- **v330 — share-card editor: per-design layouts + bottom-bar editing;
  picker hold actions rebuilt compact.** (1) **Per-style moves**: the
  editor's single shared `ShareCardMove` became `movesByStyle`
  (`Map<ShareCardStyle, ShareCardMove>`; each style in the pager renders
  its OWN saved move via a derived `move` + `updateMove` helper), and
  persistence nests a per-style `moves` object under the topic
  (`AppPreferences.saveShareCardEdits` now takes a JSONObject built by
  the sheet; legacy flat saves load as one move applied to every style).
  (2) **Editing UX**: pager swiping works while editing when NOTHING is
  selected (tap empty card space — a full-size clickable deselect box
  behind the element overlays — to deselect, then swipe); the FACT move
  grip hides while `factEditMode` so it never covers the typed text; the
  floating Edit-text/Reset/Done cluster over the card is GONE — while
  editing the bottom action row becomes content pills (Quick fact / No
  fact / sources / + Custom fact) + Reset + Done, and Done restores
  Save/Share/Text (the Edit-text tool moved back into the toolbar row,
  shown when the fact is selected). (3) **Picker hold menu**:
  `RadialHoldMenuOverlay` rewritten from the gooey drag-to-pick radial
  ring into a compact icon+label pill card that springs in ABOVE the
  finger and STAYS OPEN after release (tap an option, tap the full-size
  scrim to dismiss, or let the ~6s idle auto-dismiss fire) —
  `radialHoldMenu`/`HoldSession`/`HoldAction` unchanged; the goo-blob
  canvas, ripples and ring geometry helpers are gone.
- **v331 — baseline profile + glass-snapshot coalescing (logcat heat/GC
  analysis follow-ups).** (1) `app/src/main/baseline-prof.txt` — a
  manually-authored starter HRF profile (the log showed the JIT compiling
  single giant composables at up to 7.7 MB each; ART now AOT-compiles
  those methods at install via ProfileInstaller). Rules target the startup
  path (MainActivity, crash reporter, splash, data-layer init), the nav
  host + bottom bar + glass pipeline, the hot tab screens, the giant
  share-card / picker composables, and hot libs (Room, Gson, OkHttp,
  Coil). `androidx.profileinstaller:1.4.1` is now declared EXPLICITLY in
  the catalog + app deps (Compose already pulled it transitively — the
  "Skipping profile installation" logcat line proved the receiver ran —
  but the docs require the explicit dep for the profile to install). AGP
  bundles `src/main/baseline-prof.txt` automatically and rewrites source
  symbols through the R8 mapping on minified release builds. (2)
  `LegacyGlassBlur` (pre-Android-12 app-side blur engine) coalesced:
  `SNAPSHOT_INTERVAL_MS` 125→200 (~5/s) and `SNAPSHOT_MAX_DIM` 160→128
  (~36% smaller readback+blur pass; ~60% less per-second allocation).
  NOTE — the modern (API 31+) kyant `LayerBackdrop` cannot be throttled
  from app code: `LayerBackdropModifier` re-records the full page on
  EVERY draw and `recordLayer`/`layerCoordinates`/`onDraw` are `internal`
  to the library — a record-side throttle needs a fork or upstream knob;
  the idle frameRate churn in the log traces to always-animating pet /
  constellation layers re-invalidating the page, which re-triggers the
  capture. (3) **POST-CI FIX (v332)**: the first draft of the profile put
  flags on CLASS rules (`HSPLcom/...;`), which profgen rejects ("Class
  rules don't support flags") and broke `expandReleaseArtProfileWildcards`
  on CI. HRF truth: flags H/S/P are only valid on METHOD rules
  (`HSPLcom/foo/Bar;->**(**)**: the L is the descriptor prefix), class
  rules take none (`Lcom/foo/Bar;`), and package-wide AOT is the wildcard
  method rule `<pkg>/**->**(**)**`. The file was rewritten to that shape.
- **v332 — share-card editor: content toggle + Customise gating; app-wide
  copy polish (em dashes → middot/prose).** (1) **Content toggle pill**: the
  edit-mode bottom bar (where Save/Share/Text sit) now shows ONE toggle pill
  that labels the card's current content (e.g. "Quick fact", "No fact",
  "Custom fact", a source, star rating, reading progress…) and, when
  tapped, OPENS the content options (Quick fact / No fact / saved sources /
  + Custom fact) in the SAME "source" tool panel the toolbar's Content tool
  opens — they're back where they were before v330 (v330 had spread the
  content pills directly across the bottom bar, which the user rejected).
  The toggle highlights while the panel is open and toggles it shut.
  (2) **Customise pill gated**: the floating Customise button now renders
  ONLY when `!editMode` (it previously hovered over the card mid-edit; the
  bottom bar owns Reset/Done/the content toggle while editing). (3)
  **Copy pass**: rephrased ~160 user-facing strings app-wide to drop the
  em-dash tic and read in the app's premium register — card meta rows and
  `metaSeparator`s join with " · " (was " — "), quote attributions and
  footers lose their leading dash ("— Author" → "Author"; "via Curio —
  Stay curious" → "via Curio · Stay curious"; "Name — Curio" → "Name ·
  Curio"; the stray "~ Stay Curious" is gone), tool headings read
  "$selName font"/"$selName format", full-sentence hints/empty states use
  period- or semicolon-joined prose instead of dashes, and the share-card
  editor hint copy ("Tap a thing to select · swipe for another design")
  is consistent. Scope: UI strings only (never JSON content, chapter/page
  ranges, name-qualifier parsing, or log/diagnostic text). (4)
  **Hold-menu labels shortened**: `CategoryOptionPill` / `MixOptionPill`
  rows use short verbs ("Spin", "Remove", "Edit", "Delete") instead of
  long "Spin <lane>" / "Remove from Continue exploring" / "Edit · <mix
  name>" phrases — the menu is anchored on the row you held, so the
  subject is visible and the card stays compact (also dropped the now-unused
  `name` param from `MixOptionPill`).
- **v326 — signature redesign campaign BEGINS: Books.** The
  per-category `signatureDesign` rework (one category per turn, commit but
  DON'T push — user reviews each before the next) starts with BOOKS, now a
  classic cloth hardcover: oxblood leather gradient + gold-foil double
  border (the "margins"), a left spine band with gold hinge rules, a REAL
  icon crest (`crest`/`crestTint` fields → top-right `menu_book` glyph,
  tilted like a foil stamp — replaces the old hand-drawn `Path` crest),
  giant faint `auto_stories` watermark bottom-right, and gold ruled lines
  BEHIND the quick fact (`bodyRuleColor` field → `Modifier.drawBehind` on
  the body Text, spaced at the body's own line-height so the facts sit ON
  the lines). Both new fields are optional (default null) so every other
  category is untouched. Design contract: icons/symbols/existing art ONLY
  — no SVG/path drawing without the user's permission.
- **v325 — share-card editor safety net: back-cancels-edit, edit
  persistence, quick-fact "Edit text" placement, knob-on-text fix;
  RadialHoldMenu ported off removed Compose APIs.** (1) **Back now
  cancels the editor first, then exits** (`BackHandler(enabled =
  editMode)` inside `TopicShareSheet` cancels the editor on the first
  press; the next press hits the sheet's own back handler and dismisses
  it — back was previously swallowed while editing). (2) **Edit
  persistence**: `TopicShareSheet.persistEdits()` writes the current
  move/text/scale state via `AppPreferences.saveShareCardEdits` on
  Save/Share AND on dismissal (`onDismissRequest` persists before
  dismissing), so an accidental exit resumes where you left off; leaving
  the TOPIC REVEAL screen clears the topic's edits
  (`DisposableEffect(floatingTopic)` → `AppPreferences.
  clearShareCardEdits`), so the next share of that topic starts clean.
  (3) **"Edit text" pill moved NEXT to the floating Done button**
  (previously it hid mid-scroll in the tool row) — shown when the quick
  fact is the selected element. (4) **Move knob no longer covers the
  text**: `MoveHandle` shrinks (30→22dp, glyph 18→13dp) and fades
  (alpha 0.55) while dragging so the text it moves stays readable.
  (5) **RadialHoldMenu ported for Compose BOM 2026.05** (CI compile
  errors: this generation removed `PointerInputChange.
  positionInRoot()` and made the `awaitEachGesture` scope
  `@RestrictsSuspension`): the long-press timer now runs on a
  `rememberCoroutineScope()` coroutine (never inside the restricted
  scope), root coordinates are `change.position` (node-local) +
  the node's `LayoutCoordinates.positionInRoot()` captured via
  `onGloballyPositioned` and read fresh through `rememberUpdatedState`;
  the goo merge is now a plain `Modifier.blur(18.dp)` layer (the old
  android.graphics `RenderEffect` chain no longer type-checks) —
  overlapping soft blobs still blur into one liquid-looking whole on
  every API level.
- **v324 — Deepen REMOVED; share-card Adjust tool (saturation/contrast);
  signature redesign contract.** (1) **Deepen is gone** (user direction —
  "just keep default and classic option"): the Experiments toggle
  ("Deepen signature card elements"), the `detailedSignatureElementsState`
  pref API, and the entire `signatureDesignDetailed` function (~1325
  lines) are DELETED; `SignatureCard` now picks only classic vs
  `signatureDesign`. (2) **Adjust tool**: `TopicShareSheet` gained an
  Adjust tool pill (CurioIcons.Contrast) with Saturation/Contrast sliders
  (0.5–1.5, neutral 1.0); `TopicShareCard` takes `saturation`/`contrast`
  params and threads a single `graphicsLayer { colorFilter =
  ColorFilter.colorMatrix(adjustColorMatrix(sat, con)) }` into the card
  modifier chain (preview + export both adjusted — export is a real
  `View.draw` pass, so the layer filter is captured). Reset-all also
  resets the sliders. (3) **Signature redesign contract (v324+):** we are
  reworking `signatureDesign` per category, ONE category per turn — the
  user describes the design, we implement + commit, then ask for the next.
  Rules: NO drawing/SVG without the user's explicit permission — use only
  icons, symbols, and EXISTING ready-made drawings (e.g. the `CurioIcons`
  font subset, `Watermark` glyphs, `topicVariant`/`CustomCard` art) as
  background symbols/icons. (4) **Mix strip fix** (user: "solid strip
  behind the cancel button during mix"): the floating Apply "Mix · N"
  pill is BACK in `ClassicPickerPage`'s multi-select row beside Cancel
  (`Arrangement.spacedBy(8.dp, Alignment.End)`), and the shared bottom
  row's solid "Mix · N" capsule is HIDDEN while mixing on the CLASSIC
  page (`!mixing || pagerState.currentPage != 0`) — it stays on the new
  page so apply-from-there still works.
- **v313 — Topic Browser revamp, pick 1: category-filtered search, dynamic
  chips, one-category browse.** User: "in topic browser let user change
  category and act that category as filters for the search, so it doesn't
  always stay on and show all categories when one category is selected;
  show smart suggestion in a small pill if a result has from another
  category; make the category chips dynamic — the number shows if it has
  that search result and if it doesn't have that the category should
  hide; in the list when changing category not in search the old category
  gets shown in the list too — only 1 should be shown with a button like
  an arrow, and then that category at the top only". Clarified via
  ask_user: browse list = one category + top arrow bar; suggestions = pill
  row above results, tap switches the filter; dynamic chips = search-only
  (browse keeps totals); pill action = switch to that category. (1)
  **Search respects the lane filter** — the SEARCH-mode rows builder in
  `features/database/TopicDatabaseScreen.kt` now skips lanes that aren't
  `effectiveCat` (it used to return EVERY lane's matches regardless of the
  active chip). (2) **Dynamic chips** — new off-thread `catHitCounts`
  produceState (over `catalog` + hoisted `indexById` + `matches`) feeds
  `chips`/`allChipsCount`; the sticky chip bar takes precomputed chips now
  (was `catalog` passthrough + `totalTopics`); while searching, chips show
  live hit counts and zero-hit lanes HIDE; browsing keeps full per-lane
  totals; the "All" chip always shows (count = total matches while
  searching). (3) **One category + top arrow bar (browse)** — with a lane
  active (not searching) the list renders ONLY that lane, topped by a new
  `DatabaseCategoryTopBar` ("← Films · 342 topics", whole pill = back-to-
  All); section headers only appear while All is selected. (4) **"Also
  in" suggestion pills (search)** — searching inside a lane adds a
  `SearchSuggestionRow` of small per-lane pills above the results (and
  above the empty state when the lane has no matches); tapping a pill
  switches `selectedCat` to that lane keeping the query. The section-
  header count rows, empty-state copy and pagination are untouched.
- **v314 — Topic Browser: category-panel + multi-select + typo-tolerant
  search.** User: "when I'm searching it shows 2 category pickers — hide the
  category chips and only show category options in the panel; revamp the
  picker: text search mentions a category name → prioritize it; smarter
  search showing typo results; category panel collapsed by default with its
  own tiny search box inside; multi-select via checkboxes in a Set;
  active-filter chips removable with one tap; filter passes if it matches
  text AND (no categories selected OR its category is in the set)" (JSX
  reference provided). (1) **Chips during search are GONE** — the
  `LaunchedEffect(searchActive)` auto-open and the sticky every-lane
  `DatabaseStickyChipBar`/`DatabaseChipPop`/`DatabaseFilterChip` are
  deleted; searching shows no category bar. (2) **Category panel** — the
  hero Category pill toggles a collapsed-by-default `DatabaseCategoryPanel`
  (own tiny `CurioSearchField` filtering the category list, accent
  `Checkbox` multi-select rows with per-lane counts, Clear all + Done;
  `DatabaseFilterPanelHeight = 352.dp` reserved while open). v386 — that
  panel applies LIVE: a tap commits `commitCats` there and then (the staged
  pending-set the screen used to keep between open and Done is GONE — a pick
  made in the panel could be silently dropped by closing with the pill
  instead of Done), and Done now simply collapses the panel, matching the
  Cabinet's own `CabinetCategoryPanel`. (3) **Active-
  filter chips row** — `ActiveFilterChips` shows exactly the selected lanes
  (each chip = one tap to remove, plus Clear all) whenever the selection is
  non-empty; pill label reads "Categories · N" / "Category · All" and the
  pill emphasizes while a selection is active. (4) **Multi-select** — the
  filter is `selectedCats: Set<CategoryId>` round-tripped through a
  comma-joined `rememberSaveable` string + `TopicBrowserSession.selectedSlugs`
  (enum names, replaces `selectedSlug`/`chipBarOpen`); filter applies in
  search AND browse; one selected lane keeps the `DatabaseCategoryTopBar`,
  several show section headers for just those lanes. (5) **Typo-tolerant
  matching** — `matchLevel()` returns 0 strong (substring) / 1 fuzzy
  (tokenized Levenshtein over name/byline/subtype, tolerance 0–2 by token
  length) / null; `catHitCounts` + rows use it, ranking fuzzy hits below
  strong ones. (6) **Category-mention priority** — `priorityCats` derives
  from the query vs each lane's displayName (contains/fuzzy); mentioned
  lanes' hits sort first in search results. "Also in" pills now TOGGLE a
  lane in the active set. The glass `layerBackdrop` capture now records
  whenever liquid glass is on (hero pills refract in every state).
- **v3xx11 — picker polish: tick removed, option-pill overlay fixed, dark white-dot gone.**
  User: "remove the tick when selecting … in dark mode the category options still have
  white borders … in new picker the pinned ones doesn't show tap and hold actions".
  (1) **Check tick removed** — `NewPickerTile` no longer draws the 18dp `catInk` check
  badge on selected tiles; the classic category-tint fill + bold label alone carry
  selection. In dark mode that pastel circle was exactly the "white dot/border" still
  visible on selected tiles. (2) **Option-pill overlay placement fixed** — in
  `NewCategoryPickerSheet` the `CategoryOptionPill` overlays were COLUMN SIBLINGS of
  the picker content inside the bottom sheet, so their `.fillMaxSize()` scrim only
  filled the leftover space BELOW the picker (≈0 when the sheet was full-height) and
  holding a Pinned pill (or any hold → option/remove pill) could show nothing. Both
  overlays now render INSIDE the picker Box (last children), covering the whole sheet.
  (3) dark-mode tile borders stay light-only (v3xx10); no other stroke/ring renderers
  exist in the picker files (verified: no `drawRoundRect`/`Stroke`/`borderTint`).
- **v3xx10 — theme reveal in liquid-glass mode + mix-card Spin pill + dark-mode borders.**
  User: "in liquid glass toggle on, switching between theme doesn't play that transition
  animation; also in the new picker your mixes remove the spin and spinning pill; in dark
  mode remove that weird white border around category options, in both page 1 and 2".
  (1) **Theme reveal now plays with Liquid glass ON** — `CurioThemeTransitionState.startTransition`
  captures the REAL window frame FIRST via `PixelCopy` (`windowFrame()` in
  `ui/theme/ThemeTransition.kt`, API 26+): it reads the hardware-composited pixels
  (glass blur included) instead of re-running the Compose draw chain. The old first path
  (`captureLayer.record { drawContent() }` → `GraphicsLayer.toImageBitmap`) re-invoked the
  whole app draw every frame, NESTED inside the kyant `layerBackdrop` record pass; over the
  glass backdrop it read back blank on some devices and the reveal silently skipped to the
  instant flip (the v269 blank guard). Fallback chain unchanged: PixelCopy → layer → view →
  instant. (2) **Your-mixes Spin/Spinning pill removed** — `NewMixCard` drops the inline
  pill; the whole card is the spin target (tap applies the mix); the now-unused `active`
  param and its call-site expression removed. (3) **Dark-mode tile borders dropped** —
  `outlineVariant` in the dark scheme is a pale cream (`EDE7DC` @10%) and the 1.5dp rings
  read as whitish edges on the near-black tiles; `NewPickerTile` (idle/pinned/selected ring)
  and `AddSuggestionTile` now draw their border in LIGHT mode only (selection still reads
  via the solid category-tint fill + check — classic tint style untouched).
- **v3xx9 — capture image thumbs open the Lightbox + dead-picker cleanup.**
  Audit follow-up ("fix 1 and 2"): (1) **Dead image tap fixed** —
  `ImageThumb` taps in ReelNotes / Marginalia / FieldNotes (hosted by
  `SaveCaptureScreen` and `OpenNotebookFormat`'s sub-formats) now navigate
  to the full-screen LIGHTBOX via a new `onImageTap` hook threaded from
  `SaveCaptureScreen` (`navController.navigate(CurioRoutes.lightbox(url))`
  with `launchSingleTop`; photo-picker URIs ride `LightboxTarget`
  byte-for-byte). The three dead/empty onClick lambdas (one a literal
  "TODO Phase 4") are gone; null-placeholder slots skip. (2) **Dead code
  deleted:** the unused legacy `CategoryPickerSheet` (~480 lines) in
  SpinScreen, the unused `PickerPageTab` in CategoryPickerScreen, and
  the orphaned `getRecentCategories`/`noteRecentCategory` +
  `KEY_RECENT_CATEGORIES` in AppPreferences (Continue Exploring uses
  `CurioPassport.allProgress`, not the recent list — the API had zero
  callers). NOTE: the historical v26/v196 entries describing the legacy
  Spin `CategoryPickerSheet` describe REMOVED code — the live picker
  surfaces are the new picker (default) and the classic toggle
  (`CategoryPickerContent`).
- **v3xx8 — page-2 mixes cards redesigned + pinned hints removed.** User:
  "remove the hold for options hint for pinned … redesign the your mixes
  cards looks they are bad". (1) The Pinned section label hint ("hold
  for options") is gone, and the Browse Pins tab row subtitle ("Hold for
  options · tap to spin") is gone too — holding still opens the option
  pill, the hint text is just removed. (2) `NewMixCard` is redesigned as
  a compact "mix stamp" (112dp, was 122dp slabs): a leading 38dp lane
  plate (first lane's glyph, Tune fallback), name (ExtraBold) + one-line
  `mixTeaser`, a footer row of up to four 18dp lane-composition dots
  (tinted per lane via `categoryInk()` at 16% alpha + glyph) with a
  "+N" overflow chip, and an INLINE Spin pill (no more floating
  bottom-anchored pill). 3-dot menu (Edit/Delete) unchanged.
- **v27n — elevation over borders (decided):** cards, chips, pills & sheets
  lift with real shadows instead of hairline outlines (AMOLED keeps the faint
  container step; selected states raise 4–8dp). **Shadow rendering rules:**
  `Modifier.shadow()` must precede the fill in the chain (shadow-after-
  background paints a blur ON TOP of the fill); `shadowElevation` only
  renders cleanly on OPAQUE fills — translucent/glass fills are replaced by
  opaque `lerp(fill, accent, alpha)` blends (app-wide pass: badge medals,
  avatar, chips, stat pane, lane tiles, hero glass pills, tag/tick pills,
  quest cards/stamps, capture formats, picker/preset rows, editor toolbars,
  coming-soon tiles). Hero pills resolve the banner fill as their blend
  backdrop: `CabinetHeroActionPill` receives it via the hero `trailing`
  slot, `SettingsHeroActionPill` defaults to `settingsRoseAccent()`,
  `CurioSortDropdown` takes it as a required `backdrop` param. TRUE frosted
  glass over heroes/imagery gets `shadowElevation = 0` instead (glass can't
  hold a shadow — it bleeds through). The Spin deck peek cards stay FLAT
  (`shadowElevation = 0.dp`) — v24 rejected deck shadows (they animate
  weirdly) and the elevation pass re-adding a 2dp halo caused the boxy
  artifact during the reel.
- **v27q — NO selection raises: elevation is a FLAT 2dp in both states
  for every selectable chip/card/row/tile** (was 3/1, 4/2, 6/3, 8/3, 3/0
  raises). Selection must read through a FILL change instead: SOLID
  accent fill with on-accent content (`themedAccent()`+`onAccent()`,
  `primary`+`onPrimary`, `curioDialogActionColor()`+`dialogRowSelectedInk()`
  [white except AMOLED black], or `accent`+`pastelFillInk(accent)` for
  generic accents) — never a translucent lerp (bleeds the shadow) and
  never an elevation raise. Existing non-elevation cues stay: topic-card
  check badge, category-card solid gradient, pet "Your pet" pill, swatch
  check marks. Exceptions: non-selection state toggles keep their own
  elevation (3D button, paper-stats toggle, field-border toggle,
  fullscreen capture), and the fan-deck's per-card depth shadows are deck
  order, not selection.
- **v27r — badge medals + quest passport stamps are BORDER-defined, not
  shadow-defined** (user verdict after the elevation pass: the medals
  looked wrong and their shadows clipped at the shelf edges — "weirdly
  getting cut"). `CurioBadgeMedal` keeps the v27n opaque fills but wears
  the pre-elevation ring borders: inner glyph plate (1.5dp white@0.55 /
  1dp outlineVariant@0.5 when locked), ribbon gem (1dp white@0.85),
  earned marker (1.5dp white), locked silhouette (1dp outlineVariant@0.7),
  and the "+N" tile keeps its sage ring —  NO shadows anywhere in
  CurioBadges.kt. `PassportStamp` in Quests keeps its flat 2dp elevation
  PLUS its restored 1dp ring (accent ring for UNSEEN, neutral otherwise).
- **v27r — FIXED-COLOR controls never wear a solid accent fill.** The
  note-paper toolbar controls (FormatToolButton in paper mode,
  CompactPaperChip, NotePaperColorToggle) use a MODERATED tint
  `lerp(surfaceContainerHighest, accent, 0.45f)` with the accent as
  glyph/label ink — a solid amber block in dark was too saturated and
  white-on-amber unreadable (pastelFillInk assumes pastel-adjusted
  fills; fixed colors like paperControlAccent 0xFFE3B84F / paperAccent
  0xFF9A7B2F never pastel-adapt). Category-accent fills keep the solid
  accent + onAccent/pastelFillInk contract.
- **v27t — pet studio persistence + paper experiments rework.** (1) Custom
  pet designs now APPLY: `saveAsNewPet`, `selectCustomPet`, and pet-species
  switches persist the working design as the ACTIVE design
  (`AppPreferences.setPetDesign`) — the sprite + floating pet read only the
  active design (`petDesignState`), so before this the custom slots were
  studio-only and "Save as new pet" never actually put the design on the
  pet. Selecting a built-in pet with a custom design re-tags its species and
  persists (custom follows the pet); picking the default look clears the
  active design; deleting the ACTIVE slot clears it too.
  (2) `PaperTitleLines` ("Title cut lines" experiment) sizes to the hero
  title: length scales with the title text + font size (reaches ~3 chars
  past the text end, capped 16em / 300dp, floored 5em), drawn as two
  slightly curved pen strokes (quadratic beziers, wide-soft + narrow-dark
  felt-pen passes, round caps) at a -2° hand-written tilt. Callers pass
  `title` + `fontSize` (Home name 36sp, Entry Detail topic name
  headlineMedium, others headlineSmall). (3) "Stamped pin holes" are now
  DIARY-SPIRAL punches: 3 holes, 5.5dp radius at 14dp from the left edge,
  each wearing a two-tone pressed rim — faint 1dp lip ring, top-left white
  highlight arc (160°→290°), bottom-right ink shadow arc (340°→110°).
- **v27u — border-free main/reveal cards; opaque save-page strip + explore
  pills.** (1) The Spin ticket's drawn gradient rim border (1.5dp stroke +
  1dp bevel) and its AMOLED edge-shine rim light are GONE, and the Topic
  Reveal hero's matching rim is gone too — the shared-element morph stays
  clean because both cards changed together. The `heroBorderState` pref API
  stays dormant (default true, nothing reads it). (2) The SaveCapture topic
  strip no longer wears `cat.tint` (accent @ 20% alpha) under its 3dp
  shadow — translucent fills bleed shadows (v27n rule) — it now uses an
  opaque `lerp(surfaceContainerHigh, cat.accent, 0.20f)` fill, and the
  strip's icon plate is opaque (`lerp(surfaceContainerHigh, themedAccent,
  0.15f)`). (3) The explore dialog's two action pills ("Explore" browser
  + "Watch in" service) are now VISIBLE soft-tinted pills — the old
  TextButton had no container color, so the pill shape was invisible — with
  clean glyph icons (travel_explore globe for the browser, the service's
  glyph youtube_activity/play_circle/music_note for watch), tinted with the
  pill ink, no brand tiles, 12dp apart. `curioDialogActionButtonColors`
  gained an optional `containerColor` param. (4) "Title cut lines" rework:
  the two underlines now span ~88% of the title width (shorter — the old
  +3-char stretch ran past the text) and are drawn as a NATURAL hand
  double underline — two gently wavy cubic strokes in the lower half that
  converge slightly toward the right (a single pen motion, never crossing;
  bottom line a touch longer + offset right), felt-pen edge, -2° tilt.
  (5) "Stamped pin holes" gained a sibling "Hole rings" toggle: tilted
  metal book rings through the 3 holes (foreshortened ellipse, metal
  gradient, specular highlight, contact shade, per-ring tilt -9°/-3°/3°)
  instead of the pressed rims. The paper stat card is now SHARED:
  `paperStatCardFill` / `paperStatCardColor` in
  `ui/components/PaperStatCard.kt` render the opaque paper fill + 3-hole
  EvenOdd punch + rims/rings, used by Home's Streak · Cabinet · Topics bar
  AND Profile's Level · Saved · Lanes pane (same toggles: paper card,
  holes,  rings, torn edges; Profile's tear seed 0x6B4E3E). (6) NEW "Home tint"
  experiments (Settings → Experiments → Home tint, all default OFF):
  "Home tint" — the Home background + bottom nav wear a category's
  `categoryBackgroundWash()` (Home was previously always plain; the wash is
  published to the nav chrome via `CurioNavTint.homeWash` so the bar blends
  on the Home route); "Hero tint too" — the quest hero swaps the rose for
  the category's `themedAccent()` with `onAccent()` ink; "Follow my Spin
  lane" — the tint follows the category picked on Spin
  (`getLastSpinCategories`, single lane, else Wildcard) and WINS over the
  manual toggles (Hero tint + Tint category gray out in Experiments);
  "Tint category" — a manual single-select picker (default Surprise /
  Wildcard). The Streak · Cabinet · Topics card takes a 5% whisper of the
  category shade (`lerp(fill, accent, 0.05f)`) — creamy, not colored.
  Toggles: `homeTintState` / `homeHeroTintState` /
  `homeTintFollowLaneState` / `homeTintCategoryIdState`. (7) Home's
  Recents rows (`ExploreTopicRow` + `RecentEntryRow`, both opaque category
  fills) lift from 0dp to a soft 2dp elevation, and the small
  Unexplored/Resumed tag pills inside them trim from 2dp to 1dp so they
  read as chips on the card instead of floating tiles.
- **v27v — pet eyes: 2s look-timeout + touch-scroll detection.** The pet's
  pointer-aware eyes (`PetPointer` in `ui/pet/CurioPetSprite.kt`) used to
  aim at the LAST pointer position FOREVER — the pet kept staring at the
  final scroll/tap point long after you stopped touching. Now `PetPointer`
  bumps `activityTick` on every event (hover/press/drag/scroll/release)
  and each sprite keys a `lookStrength` Animatable on it: full aim while
  events keep arriving, then ease back to the neutral glance ~2s after the
  last one (or once a held press releases). Also, touch vertical scrolling
  is a DRAG of `Move` events — wheel-only `Scroll` events never fire on
  phones, so the eye-roll only worked with a mouse wheel. The tracker now
  accumulates the vertical travel of a press-drag and fires the roll once
  it clearly scrolls, gated to one roll per ~350ms so a fast fling gives a
  few discrete rolls instead of restarting every frame.
- **v29 — per-topic progress (pages read / episodes watched).**
  `CurioTopic` gained optional `pageCount` (BOOKS) and `episodeCount`
  (ANIME) — both parsed by `TopicJsonLoader`, both absent from legacy
  JSON (null = no progress tracking; anime films deliberately carry no
  `episodeCount`, a film has no episodes to track). Progress itself lives
  in `data/TopicProgressStore.kt` — a SharedPreferences JSON map keyed by
  topic id, exposed as reactive Compose state (`progressState`), seeded
  once from `MainActivity.onCreate`, and shared by every surface: the
  Topic Reveal hero, the Cabinet entry cards, and the EntryDetail hero.
  The shared control is `ui/components/CurioProgressPill.kt`: a LONG
  accent-shaped floating button (% ring + count + slim bar + Edit hint),
  tap → slider editor dialog (0..target with Reset/Finished/Save). On the
  reveal hero it straddles the card's bottom edge (the action row drops
  16→40dp when progress exists); on Cabinet cards it's a compact strip in
  the card body; on EntryDetail it floats over the hero's bottom edge.
  Always-on (no experiment toggle, per user decision).
- **v29 — hero sort/search/select controls redesigned (Cabinet + Topic
  Database).** `CurioSortDropdown` is now ONE pill with two tap zones —
  the label + chevron opens the dropdown, a `VerticalDivider` separates
  it from the arrow zone that toggles ascending/descending (was two
  separate pills); the pill is bigger (44dp tall, labelLarge, 22dp arrow)
  and the dropdown redesigned (20dp corners, tonal elevation, a "Sort by"
  header, a check on the active field). The hero pills
  (`SettingsHeroActionPill`, `CabinetHeroActionPill`) and the sort pill
  dropped the v27r ink-lean fills (lerp toward ink at 0.30/0.35/0.55 —
  read TOO DARK in light + pastel) for a LIGHT frosted glass: the banner
  fill lifted toward white (`lerp(backdrop, White, 0.24f)` emphasized /
  `0.38f` normal; destructive stays a black-lean `0.14f`), so full-ink
  glyphs pop in light, dark, pastel and AMOLED. Both heroes' SEARCH
  fields match: the ink-at-16% container became `lerp(fill, White, 0.30f)`
  with full-ink borders (0.65/0.40). Pills also grew (14/10dp padding,
  22dp glyph).
- **v29 — Spin FilterSheet: ≤4 columns + visible inactive elevation.**
  The chip grid swapped `GridCells.Adaptive(112dp)` (stretched two huge
  slab-chips on phones, 5+ on tablets) for a `BoxWithConstraints` fixed
  count: `(maxWidth / 92.dp).toInt().coerceIn(2, 4)` — compact pill
  columns capped at four in a row. `CompactChip` now lifts the INACTIVE
  fill a whisper of white (`lerp(chipSurface, White, 0.04 dark / 0.10
  light)`) with a 2dp shadow in BOTH states + `curioDarkGlow`, so
  unselected chips read as raised pills off the tinted sheet instead of
  flat tiles. **Pastel-mode follow-up:** the whisper was invisible — in
  light mode `categorySurface()` ignores its `base` surface step, so the
  sheet (`surfaceContainerLow`) and the chips (`surfaceContainerHigh`)
  resolved to the SAME airy pastel and the 2dp elevation read as nothing.
  The light-mode lift is now a clear surface step (`0.32` toward white,
  dark keeps `0.04` + glow), so unselected chips visibly stand off the
  tinted sheet in pastel AND plain light mode.
- **v30 — uniform hero-pill height + Category pill in Cabinet & Topic
  Browser heroes.** (1) Every hero pill now reads the SAME 42dp height:
  `CurioSortDropdown` trims from a 44dp minimum to 42dp, and
  `CabinetHeroActionPill` / `SettingsHeroActionPill` label-only pills get
  a `heightIn(min = 42.dp)` so they match the 22dp-glyph pills — the sort
  pill no longer reads thick next to Select/Search. (2) A new **Category**
  pill rides a SECOND row directly under the hero's top pill row (Tune
  glyph + active-filter label + an up/down chevron that flips with the
  chips — ▾ closed / ▴ open, via the pills' optional `trailingGlyph`,
  `emphasized` while open): tapping it
  reveals the sticky category chip bar — the same chips that appear while
  searching — in BOTH the Cabinet and the Topic Database. The heroes grow
  +52dp to fit it (`CabinetHeroBannerHeight` 180→232, compact 140→192;
  `SettingsHeroExtraRowHeight = 52.dp` applied to the settings hero when
  the new `extraRow` slot is used). The Topic Database's category chips
  are now HIDDEN BY DEFAULT (matching the Cabinet): the chip bar shows
  only while the Category pill is open or search is active, and the DB
  derives its own content offsets from `DatabaseHeroTotalHeight`. The
  chip-bar content-top reservation only applies while the chips are
  visible, so the collapsed screens start right below the hero.
- **v30 — shared hero follows the Spin lane (Appearance toggle) + settings
  declutter.** (1) New Appearance toggle **"Hero follows Spin lane"**
  (`heroFollowLaneState`): when ON and the Spin deck is on a single lane,
  the shared torn hero (Home / Profile / Settings / Cabinet-All / Quests /
  Recent / Support / drawer — every rose/azure hero) wears that category's
  `headerAccent()` — the Cabinet's filtered-hero language — via the new
  central `heroLaneCategory()` helper hooked into `settingsRoseAccent()` +
  `homeRoseAccent()`, and the page background below it wears the category
  wash via `heroPageBackground(default)` (Home inline, Profile/Settings hub
  keep their rose-lerp default; the rest of the settings family keeps its
  plain default; Cabinet-All falls back to the lane wash too). Mix/empty
  lane or toggle off → rose/azure as before. (2) **Removed:** the Home tint
  experiments (Home tint / Hero tint too / Follow my Spin lane / Tint
  category — Experiments section + picker gone, prefs dormant), the
  **"Glow shadows"** Appearance option (`curioDarkGlow` is now a no-op —
  the light glow was retired as a poor look; `darkGlowState` pref stays
  dormant), and the **"Entry date & mood"** option — date/mood/attachments
  are ALWAYS on now (SaveCapture + Marginalia gates hardcoded).
  (3) **Merged:** "Floating explore bubble" + "Display over other apps"
  are ONE option — the bubble toggle shows the live overlay grant state in
  its subtitle, enabling without the permission opens the system page to
  ask for it, and when the bubble is OFF with the permission still granted
  an inline "Remove overlay permission" row appears to revoke it (a
  separate revoke-trip flag keeps the return from re-enabling the bubble).
- **v31 — Adaptive Hero + hero picker, Category pill below the hero,
  slimmer sort pill, background-tinted surfaces, faster Home.**
  (1) **"Hero follows Spin lane" renamed "Adaptive Hero"** (Appearance
  toggle + Settings hub row) and the Profile hero finally follows the
  lane: `profileRoseAccent()` now runs the same `heroLaneCategory()`
  check Home/Settings have (it was the only shared hero missing it).
  (2) **Hero picker is a 2-option segmented control** (Rose hero / Sky
  azure hero) replacing the "Sky azure hero" switch — Sky azure is
  GREYED OUT (visible but unselectable, the Material-coming-soon
  pattern; a one-time migration flips a previously-enabled azure back to
  rose), and the whole control greys while Adaptive Hero is on.
  (3) **Category pill moved OUT of the hero** in Cabinet + Topic
  Browser: it rides its own fixed row just below the hero (page-level
  pill: on-surface ink over surface-high glass), so the heroes returned
  to their original heights (`CabinetHeroBannerHeight` 232→180,
  compact 192→140; the settings `extraRow` slot + `SettingsHeroExtraRow-
  Height` are gone) and the header text never moves down. The sticky
  chip bar sits below the pill row (chip-bar offsets derive from
  `barTop`/heroTotal + `CabinetCategoryPillRowHeight`).
  (4) **Sort pill slims down:** `CurioSortDropdown` swaps its fully-
  rounded 50dp capsule for 16dp corners with tighter padding (keeps the
  uniform 42dp height). (5) **Cream → small tint of the page
  background, in every theme:** `CurioSettingsCard` (Profile + Settings
  hub + sub-pages) lerps `surfaceContainerLow` 30% toward `background`;
  the ink-glass hero pills + sort pill lift toward the page background
  in light mode via a new `curioPillLift()` helper (dark/AMOLED keep the
  white lift for visibility); dialogs pull a step toward the background
  in every theme (`curioDialogContainerColor`); and the settings-family
  sub-pages (Appearance/Preferences/Support/Backup/Experiments/Promo)
  now wear the same rose-lean page tint as the hub/Profile instead of
  the plain cream background (`heroPageBackground(lerp(background,
  settingsRoseAccent(), 0.10f))` — the spin-lane wash still wins when
  Adaptive Hero is on). (6) **Home opens faster:** the canonical topic
  count is now cached in memory (`TopicJsonLoader.countCanonicalTopics`
  parsed the whole ~14k-topic catalog on EVERY return to Home; one
  parse per process now).
- **v70 — filter sheet: tear hero to the status bar + watermark backdrop,
  active-filters strip removed, group-label icons.** (`FilterSheet` in
  SpinScreen) (1) **The tear hero now runs up BEHIND the status bar** —
  the sheet dropped its 28dp rounded top corners and floating drag
  handle (`shape = RectangleShape`, `dragHandle = null`) and consumes
  only bottom + IME insets (`contentWindowInsets = navigationBars ∪ ime`),
  so the banner fills the very top edge like every page hero; the banner
  height grows with the status-bar inset (`118.dp + statusBar` via
  `WindowInsets.statusBars.asPaddingValues().calculateTopPadding()`) and
  its title/Clear-all row applies `statusBarsPadding()`. (2) **Category
  name steps up 30 → 34sp.** (3) **The sheet body now wears the page
  watermark backdrop** (`CurioWatermarkBackdrop` at `alphaScale = 0.5f`,
  kept in the band below the hero via `topClearance = filterHeroHeight`)
  — the filter page finally shares the collage language of every other
  screen. (4) **The "Active filters" summary strip and its divider are
  REMOVED** — the selected chips were redundant (selections already read
  on the group pills + open group); the now-dead `ActiveFilterChip`
  composable was deleted with it. (5) **Group section labels (Type ·
  Genres · Era · Origin · Franchise) gained a per-group Material Symbol**
  (`FilterGroupKey.glyph`: `category` / `style` / `history` / `public` /
  `movie`, all verified in the bundled font) tinted with the category
  accent, stepped 16 → 17sp, with cleaner 10/8 margins; the accordion's
  top margin also tightened 14 → 6dp now that the divider is gone.
- **v71 — pet designer: eye-size presets FIXED + whole-pet size option.**
  (1) **Eye presets were a no-op (root cause):** `CurioPetSprite` scaled
  each procedural eye's pixels around its center and snapped to integer
  cells (`roundToInt`). The default eyes are only 2px wide (±0.5 cells
  from the center; STAR/DIZZY ±1.5), so the 0.85/1.2 factors shifted
  every pixel < 0.5 cells and rounded right back onto the authored
  cells — Small/Medium/Large were pixel-identical. The sprite now
  scales the eye art in DRAW space per eye (`DrawScope.scale` around
  each center at 4.5/7 and 10.5/7, the detail-layer transform trick)
  with stronger factors (0.72 / 1.0 / 1.35); the placement offset then
  applies unscaled — every eye style visibly shrinks/grows. (2) **New
  whole-pet size option:** `PetDesign.petScale` preset (0 small / 1
  medium / 2 large), serialized `petscale=` + tolerant parse (legacy →
  1). `CurioPetSprite` multiplies its sprite box by the preset
  (0.8 / 1.0 / 1.3) on top of the caller's stage `sizeScale`, so the
  custom pet scales up EVERYWHERE it renders (floating pet, flower bed,
  quests, every designer preview). The Pet Designer Settings page gained
  a **"Pet size"** card (before the Eyes card): live preview +
  Small/Medium/Large + Reset size, writing `design.copy(petScale = …)`
  with undo — the Eyes-section pattern.
- **v80 — category picker banner: smaller two-line title, hint removed.**
  `CategoryPickerSheet`'s "What are we exploring?" stepped 34 → 28sp and
  now wraps onto two lines ("What are we\nExploring?", 34sp line height);
  the mode-hint subtitle ("Tap a deck to spin it. Hold to pick
  several." / "Tap to toggle decks · Done to spin together") is deleted.
  The deck-status chip stays. (`maxLines = 1` was removed with the
  manual newline or the second line would have ellipsized away.)
- **v116 — profile avatar crop: manual crop editor + auto center-square
  crop, redesigned Edit profile dialog.** (1) **Picking a photo no longer
  squishes portraits**: `ProfileScreen` now DECODES the picked image
  EXIF-correctly (`decodeAvatarSource` — ImageDecoder 28+ bounded to
  2048px with its own EXIF pass disabled, BitmapFactory sample 26-27;
  framework `ExifInterface` rotation applied manually so both paths agree)
  and saves a **CENTER-SQUARE crop from the middle** (`centerSquareCrop`,
  `scaleToMax` → 512px) as the avatar, with the editable SOURCE kept
  beside it as `profile_avatar_src_*.png`. (2) **Manual crop editor:** new
  `ui/components/AvatarCropDialog.kt` — a fixed SQUARE crop window with
  the photo panning/pinching behind it (drag to move, pinch to zoom,
  never smaller than cover-fit, reset returns to the center crop); Apply
  hands back the exact source-pixel `IntRect` (`currentCropRect`) which is
  re-cropped and re-saved. No crop library — plain Compose gestures, the
  app's dialog styling. Composed AFTER the edit dialog so its window
  stacks on top; canceling the edit dialog also clears the crop state.
  (3) **Edit profile dialog redesigned:** the 64dp preview grew to 84dp
  with an accent **crop/photo badge** on the corner (tap the avatar to
  adjust when set, else pick); the flat stock TextButtons became the
  app's **pill actions** (`DialogPillAction` — accent Add/Change photo,
  Adjust, destructive Remove); the caption now explains the auto square
  crop. `loadAvatarSource` falls back to the  current square avatar for pre-v116 avatars (no source was kept then).
  (4) **Dark dialogs match the settings option cards**: the dark branch of
  `curioDialogContainerColor` (CurioTheme.kt) now uses the SAME fill as
  `CurioSettingsCard` — `lerp(surfaceContainerLow, tintLift, 0.30f)` with
  the dark tint lift `lerp(Color.Black, curioRoseInk(), 0.20f)` (the
  neutral rose, since a dialog floats over any page) — instead of the old
  `lerp(surfaceContainerHigh, background, 0.55f)` grey slab, so every
  AlertDialog (edit profile, crop, two-step, etc.) reads as black
  option-card glass on the black page.
- **v117 — crop-before-apply + decluttered Edit profile dialog.** (1)
  **Picking a photo now opens the crop editor FIRST** — the pick no
  longer applies the center crop immediately: `avatarPicker` hands the
  decoded source to `cropSource` and only Apply saves
  (`saveAvatar(src, rect)`); Cancel discards the pick. The dead
  `openCropEditor` / `loadAvatarSource` re-crop path is gone. (2) **Edit
  dialog declutter:** the **Adjust pill, the crop/photo badge and
  tap-avatar-to-crop are all REMOVED** (the avatar is a static 84dp
  preview) — Change photo is the only way back into the crop editor;
  captions updated ("Square photo — Change photo to re-crop." / "Pick a
  photo — you can crop it to a square before saving."). (3) **Crop
  dialog buttons are a MATCHED pill pair** — Cancel is a calm surface
  pill next to the accent Apply pill (was a flat TextButton next to a
  lone filled pill); unused `TextButton` / `curioDialogActionButtonColors`
  imports removed.
- **v118 — drawer sections + Home avatar pill + Support update link.**
  (1) **Home profile pill wears the avatar:** `TopBarPill` gained an
  optional `avatarPath` — the profile pill on the Home sticky bar shows
  the avatar photo (the Surface clips to the circle; the animated `rim`
  ring draws on top so the frosted scroll morph still reads) and falls
  back to the Person glyph; fresh pref read each composition, like the
  drawer. (2) **Drawer declutter — collapsible sections:** the drawer
  groups rows into two collapsible sections, BOTH collapsed by default
  (user request): **"Your Curiosity"** (AutoAwesome header → Topic
  History, Manage Categories, Browse Topics) and **"About"** (Info
  header → Support & diagnostics, Replay intro — the user picked the
  name). New `DrawerSectionHeader` (leading icon chip + ▼/▲ chevron,
  the filter-sheet convention); state via `rememberSaveable`. The drawer
  avatar also grew 48 → 56dp and the greeting text stepped up (CURIO
  labelMedium, "Hi name" headlineMedium, tagline bodyMedium).
  (3) **Support & diagnostics gained an Updates row** (Download icon)
  that opens the dedicated Updates sub-page — the v116 de-dupe stays
  intact (exactly one link, no duplicate header).
- **v118 — pet dialogue fully ported to the canonical dialog doc.** Every
  line in `CurioPet.kt` / `CurioPetBrain.kt` / `TourController.kt` now
  matches `docs/pet-dialogs.md` (the user's rewrite — tiny curious
  creature voice, "I/me/we", no self-naming in evolved voices). All §1
  event pools (SPIN_LANDED → QUEST_COMPLETE + sassy), §2 streak, §3
  evolution ceremony, §4 moods (10 × first/baby/mature + time-of-day),
  §5 greetings/welcome-backs, §6 touch tiers/bonds, §7 games (spinCheer,
  play, landmark, jig, dizzy, drawer, peek, chameleon, spark, the six
  interactive moments), §8 memory/factLine, §9 brain openings/bodies,
  §10 tour script, §11 mature routine lines — all replaced 1:1 (pool
  names, order, placeholders `__LANE__`/`$lane`/`$savedLane`/`$streak`/
  `$count`/`$topic`/`$level`/`$saves` kept). §12 BABY VOICE EXPANSION
  (Curie-isms) is wired in: thematic pools (`babySaveLines`,
  `babyTouchLines`, `babyLevelUpLines`, `babyEvolveLines`,
  `babyExploreLines`, `babyDiscoveryLines`, `babyMishapLines`) feed the
  matching baby events, `babyCurieLines` (sounds + tiny phrases + rare
  silly lines) rides EVERY baby pick, and the excited/happy/curious/
  sleepy/shy/grumpy/playful mood pools absorbed their categories. The
  brain's `say()` bodies are now multi-option pools picked through
  `CurioPet.pickLine` (anti-repeat); the coined catchphrases are the
  doc's §9 set. Dead `happyLines` pool removed. PetDesign.kt's
  custom-reaction preview defaults are untouched (not in the doc's
  scope).
- **v119 — the tour dock's Skip/Next is never covered by the guide.**
  The pet-led tour's Cabinet stop dropped the speech bubble onto the
  bottom dock: the `grid` landmark wraps a `fillMaxSize()` grid, so the
  bubble's "above" placement never fit and the "below" fallback was
  clamped to the very bottom — over the Next button (an empty Cabinet
  was worse: no landmark at all, so the bubble floated over the pet
  wherever the previous step had parked it). Fixes in
  `CurioFloatingPet.kt`: the tour bubble is capped above a new
  `TOUR_DOCK_BAND` (96dp + nav inset); landmarks that span the screen
  (the Cabinet grid) anchor the bubble just above the landmark's CENTER
  (upper-middle — v120 fix, the original pin-to-top floated over the
  hero); the pet's tour walk target is floored above the same band so
  it never stands on the dock; a landmark-less stop parks the guide
  top-center; and the no-landmark bubble fallback is clamped
  on-screen/above the dock. And in `CabinetScreen.kt` the EMPTY state
  now registers the same `grid` landmark the filled grid does, so the
  tour always has an anchor on the Cabinet stop.
- **v120 — pet games reworked + the dialogue actually gets spoken.**
  (1) CHATTER: the pet now says a passive mood line (`CurioPet.lineFor`)
  every ~20-40s of idle — the mood pools are heard, not just the event
  lines. (2) GAMES RUN TO COMPLETION: the three games became suspend
  functions (`playHideSeek`/`playChameleon`/`playStarGame`) dispatched
  at the top of the wander loop; `gameActive` gates the mood loop,
  typing reaction, idle/time custom actions, auto-nap and the chatter
  so NOTHING overrides a round (the old probabilistic inline game
  blocks + per-game cooldown vars are gone). (3) GAME MODE: long-press
  no longer sends the pet home — it arms game mode (pet stays put,
  autonomy paused); the next tap OR drag starts ONE game, then game
  mode ends. v121 — game mode CYCLES through the games (HIDE_SEEK →
  CHAMELEON → SPARK → …) so all three get played evenly; the auto-flow
  scheduler keeps its random picks. Drag the pet onto its flower bed to
  send it home (unchanged). (4) HIDE-AND-SEEK: the pet POOFS out and teleports to a
  random corner, just a sliver visible; tap the sliver to win; miss it
  and after up to 5s it poofs back with a sad face (`EyeStyle.CLOSED` +
  `MouthStyle.O` via `reactionFace`) + `missedMeLine()`. Chameleon's
  find window is also 5s, and both teleports burst a new `PoofOverlay`
  (puffs at the RECORDED position). (5) STAR-CATCH: a 10s round of
  stars falling slowly from above (`FallingStar` list, spawned every
  ~500-850ms) — the pet NEVER chases on its own; tap a star and it
  dashes over to catch it, or drag the pet onto a falling star; the
  score is spoken in a bubble at the end. (6) IDLE ROAM: the wander
  beat dropped from 2.8-7s to 2-3.2s so an untouched pet roams again
  quickly. (7) AUTO-FLOW: a scheduler effect requests a RANDOM game at
  random 20-50s intervals (scaled by the game-frequency setting),
  honoring `GAME_MIN_SPACING_MS`. Dead state/constants removed
  (`sparkTarget`/`sparkKey`/`sparkWon`/`lastHideSeekAt`/`lastChameleonAt`/
  `lastSparkAt` + the three per-game cooldowns).
- **v122 — drawer greeting: bigger avatar, lifted row, auto-shrinking name.**
  The drawer hero avatar grew 56 → 64dp (the initial-letter fallback
  stepped up `titleLarge` → `headlineSmall` to match), the greeting row
  sits a touch higher (`bottom` 28 → 40dp, still inside the 186dp hero),
  and the "Hi name" line no longer gets CUT on long names: the greeting
  style steps down by length (headlineMedium ≤16 chars → titleLarge
  ≤26 → titleMedium beyond) with the single-line Ellipsis as the last
  resort. NOTE: the first attempt used `TextAutoSize.StepBased`, which
  FAILED CI — `androidx.compose.ui.text.TextAutoSize` is not resolvable
  on this project's Compose classpath (despite the 2026.05.01 BOM), so
  the manual length-based font steps are the shipped approach. Do NOT
  reintroduce TextAutoSize without first confirming it resolves.
- **v123 — tour tab steps navigate like REAL tab switches; drawer name
  lines; pet teleport/chameleon/auto-flow.** (1) **FIX — skipping the
  tour on the Spin page made the Home tab "dead" afterwards.** Root
  cause (read from Navigation 2.9.8's `NavControllerImpl.navigate`,
  not a guess): `navigateToTab` calls
  `navigate(route){ popUpTo(HOME){ saveState=true }; launchSingleTop;
  restoreState }`. The tour previously PUSHED Spin with a plain
  `navigate("spin")` (no popUpTo), so HOME never entered the
  controller's `backStackMap`. The first Home-tab tap after the skip
  then popped Spin with `saveState=true` — which maps the popped stack
  UNDER HOME's key — and `restoreState=true` immediately RESTORED that
  stack, landing back on Spin (the tap looks dead). In the normal flow
  the first `navigateToTab` plants a NULL mapping for HOME (its no-op
  popUpTo saveState), so the later Home restore is a no-op — that's why
  only the tour-created Spin stack broke. FIX: all THREE tour
  navigation sites (`CurioNavHost.advanceTourAndNavigate`, SpinScreen's
  `onSpinClick` consumeTap, HomeScreen's quest-card consumeTap) now use
  `navigateToQuestRoute` (tabs → `navigateToTab`, pushes stay plain).
  RULE: never plain-`navigate` a TAB route (Spin/Cabinet) from the tour
  — use `navigateToTab`/`navigateToQuestRoute`, or the Home tab
  self-restores the tab you left. (2) **Drawer greeting:** the "Spin
  it. Explore it. Capture it." tagline is GONE; the first name stays in
  the greeting position ("Hi First") and the remaining name parts
  (middle, last) render at the tagline's old size (`bodyMedium`, alpha
  0.78) in its spot. **v134 — the remaining parts now join onto ONE
  line** (`joinToString(" ")`, single `Text`, `maxLines = 1` +
  ellipsis) instead of one line per name part — the user found the
  per-part wrapping weird ("middle 2nd 3rd name into 2 different lines").
  (3) **Pet:** random LONG
  teleports (`walkTo`'s `LONG_JUMP_FRACTION` branch) now `burstPoof` at
  the old spot, `delay(160)`, teleport, then poof again at the target —
  no more instant snap; the chameleon game POOFS and teleports to a
  random on-screen spot BEFORE fading to its ghost outline (was: fade
  in place); and the idle AUTO-FLOW scheduler only picks
  `HIDE_SEEK`/`CHAMELEON` — star-catch (a 10s round) stays reachable
  via game mode's cycle + manual taps, never the auto-flow.
- **v124 — floating pill nav bar (phones).** The edge-to-edge M3
  `NavigationBar` is REPLACED by `CurioFloatingNavBar` (in
  `ui/components/CurioBottomNav.kt`; the old `CurioBottomBar` composable
  is deleted): a floating 50-radius capsule pinned bottom-center above
  the gesture inset. Every tab renders icon-only (48dp pill); the ACTIVE
  pill springs wider (96dp, `animateDpAsState` spring damping 0.75) and
  its label slides out (`AnimatedVisibility` expandHorizontally+
  fadeIn) while the previously active pill collapses — the "smooth
  collapse and expand" the user asked for. The active indicator is a
  filled capsule covering the WHOLE pill (icon + label). Colors are
  pure `colorScheme` tokens (surfaceContainerHigh bar + shadow 6dp,
  secondaryContainer indicator, onSecondaryContainer ink,
  onSurfaceVariant inactive) so Curio / AMOLED / Material (dynamic)
  themes and dark mode adapt automatically. Geometry: the slot stays
  80dp + nav-bar inset (verified against M3's `NavigationBar`:
  `windowInsetsPadding + defaultMinSize(80)`), so Scaffold innerPadding
  and the Reveal 80dp placeholder are unchanged; wide windows keep
  `CurioNavigationRail` (user decision). Label width is FIXED (48↔96dp)
  so the bar's total width is constant and the morph is stable. The
  page-wash tint (`CurioNavTint`) now applies to the rail only.
- **v124 follow-up — floating pill bar: page-wash slot + active-only
  morph.** Two user fixes to `CurioFloatingNavBar`: (1) **The white/
  black strip behind the pill is GONE.** Root cause: the Scaffold's
  bottomBar slot is painted with `MaterialTheme.colorScheme.background`
  (white in light / black in dark), and the old edge-to-edge bar
  covered it — the floating pill leaves it visible as a strip against
  the category-washed pages. FIX: the slot `Box` now paints
  `curioNavContainerColor(routePrefix)` — the SAME animated page wash
  the rail uses — and `curioNavContainerColor`'s no-wash fallback
  changed `surface` → `background` (a page publishing no wash has
  `background` as its own background; `surface` left a seam on Cabinet
  "All"). HomeScreen now publishes its REAL background always (`homeBg`
  — the lane wash OR the rose-tinted default), not just when a lane is
  active. (2) **Only the ACTIVE pill animates.** The width morph now
  plays only on the false→true edge (`wasSelected` remembered state →
  spring spec on becoming active, `tween(0)` snap on deselect), and the
  label's `AnimatedVisibility` exit is `fadeOut(tween(0))` so the
  closing pill's text vanishes instantly — the old pill no longer
  collapses with a visible shrink.
- **v125 — offline transcription (Vosk) + floating dictation mic + plain
  quick title.** (1) **ENGINE CHOICE — Vosk, not whisper.cpp:** the user
  originally asked for whisper.cpp, but whisper.cpp has NO published
  Android binding (the Maven-Central `io.github.givimad:whisper-jni`
  jar is DESKTOP-only: Windows/Linux/macOS .so/.dll — verified on its
  README; no reliable Android AAR exists anywhere). Vendoring the C++
  source + NDK/CMake build was rejected (heavy, unverifiable in this
  env). The user approved Vosk instead: `com.alphacephei:vosk-android`
  AAR bundles .so for every ABI and ships the same offline
  model+transcribe UX. RULE: before promising a native/ML library,
  verify it has an ANDROID artifact — "Java JNI wrapper" ≠ "Android".
  (2) **New `data/OfflineTranscriber.kt`:** `VoskModels` (catalog of
  downloadable models id/name/lang/size/url; `download()` = plain
  HttpURLConnection to alphacephei.com + ZipInputStream extract into
  `filesDir/vosk-models/<id>/` with an `am/` completeness check;
  `modelDir`/`isDownloaded`/`deleteModel`) + `OfflineTranscriber`
  (MediaExtractor+MediaCodec decode of the AAC m4a → 16-bit PCM →
  downmix-to-mono + linear-interpolation resample to 16kHz → feed
  `org.vosk.Recognizer` in 5s chunks on Dispatchers.Default; returns
  trimmed text). (3) **Settings → Recording → "Offline model" row**
  (`OfflineModelDialog` in SettingsSharedComponents): pick quality/size,
  in-app download with live progress, delete — labeled as the model for
  pre-recorded voice-to-text. New prefs: `offlineModelIdState` +
  `offlineModelVersionState` (bumped on download/delete so detail
  recomposes). (4) **Per-field dictation mics REMOVED everywhere** (Sound
  Bite title+note buttons, the detail page's "Transcribe note" chip, the
  `allowTranscribe` plumbing). Replaced by ONE floating mic in Save your
  take: it appears only while the LARGE note box (`RichTextEditor` gains
  an `onFocusChanged` hook) is focused, and opens a dictation dialog
  with the live preview pinned at the BOTTOM + Stop/Insert — the session
  only ends when the user taps Stop (generous 2.5s/1.5s silence
  windows), Insert commits the text (box stays editable). (5) **Quick
  title de-papered:** the "Add a quick title" field is now a PLAIN
  rounded input (`PaperLineField` gains `paper: Boolean`; no slip, no
  style/color toggles) and the detail page shows the title as a pill
  JUST BELOW the quick fact (`titleMedium` SemiBold on an opaque
  accent-tinted capsule) instead of a torn paper slip in the body.
  (6) **Transcript persistence:** `CaptureData.SoundBite.transcript`
  (nullable String) — detail-page Transcribe button (only when the
  model is downloaded; otherwise a Settings hint row) decodes +
  transcribes + saves via `repo.save`; the transcript renders as a
  collapsible box (3 lines + Expand button, Re-transcribe + clear).
  Vosk on a sound bite uses a NEW `Recognizer` per call (Model load ~1s)
  — never reuse one across audio files.
- **v126 — editable progress target + corrected topic totals + alternate-
  edition pill.** (1) **The dialog's number is now TAPPABLE to fix the
  total pages/episodes.** Before, the editor's target was locked to the
  topic JSON (`progressTarget`), so wrong baked-in totals (see (2))
  couldn't be corrected in-app. The count line under the ring ("value /
  target unit") now opens an inline numeric field (✓ commits); the
  corrected target persists per-topic via `TopicProgressStore.setTarget`
  (new `targetOverrides` map, prefs key `topic_target_overrides_v1`,
  seeded in `seed()`), and `getTarget(topicId, default)` makes the
  override win everywhere — `CurioProgressPill` (pill text, fraction),
  `CurioTopicCard` (progress line) and the editor's own slider/steppers.
  Save writes the override only when it differs from the baked-in value
  (`clearTarget` otherwise). (2) **Topic-data corrections:** the anime
  season-1 entries carried the MERGED multi-season episode total (Mob
  Psycho 100 was 37 = S1+S2+S3, One Punch Man 24 = S1+S2, Attack on
  Titan 89 = S1–S4, My Hero Academia 159 = S1–S7, etc.) — 15 entries in
  `anime.json` corrected to their season-1 counts (verified: Mushoku
  Tensei 23, Spy x Family 25, MHA 13, etc.). (3) **Alternate-edition
  pill (books):** `CurioTopic` gains `altPageCount: Int?` +
  `altPageLabel: String` (loader-parse `altPageCount`/`altPageLabel`;
  SCHEMA.md documents both). Books whose common editions differ HUGEly
  (≥20% of the primary) now render a second, quieter pill beside the
  progress pill ("or 574 Lombardo") — tapping it opens the editor
  pre-set to that edition's count (`initialTarget` param; nothing
  persists until Save). Data added for the verified cases: The Iliad
  (704 Fagles / 574 Lombardo), War and Peace (1392 / 1104 Wordsworth),
  Moby-Dick (635 / 720 Penguin Classics), Ulysses (732 / 649 Corrected
  text), The Count of Monte Cristo (1276 / 1462 Modern Library).
- **v128 — alternate-edition pill threshold 20% → 8% + 5 new books.**
  Real translation/edition gaps for classic novels run 8–16% (Rutherford
  vs Grossman Don Quixote 992/1072, Denny vs Signet Les Misérables
  1463/1232, P&V vs Dover Maude Anna Karenina 864/752), so the v126
  `altGapHuge` rule (≥20% of the baked target) HID 4 of the 5 books that
  already carried `altPageCount` (Iliad 18.5%, Moby-Dick 13.4%, Monte
  Cristo 14.6%, Ulysses 11.3% — only War and Peace 20.7% showed).
  `CurioProgressPill.kt`: `(bakedTarget * 0.20)` → `(bakedTarget *
  0.08)` — still excludes trivial trim/font variance (3–5%). Data
  added for the named + verified cases: Don Quixote (992 → 1072
  Penguin, Rutherford), Les Misérables (1463 → 1232 Penguin, Denny),
  Anna Karenina (864 → 752 Dover, Maude), Crime and Punishment (672 →
  608 Penguin, Ready), Wuthering Heights (416 → 464 Norton). All 10
  alt-field books now render their pill. Web mirror untouched (its
  schema has no `pageCount`/progress pill).
- **v127 — books.json deduped (500 → 444).** Every duplicate book (54
  normalized-name groups, 56 entries — e.g. "The Odyssey" + "The
  Odyssey (c. 8th century BCE)", "Moby-Dick; or, The Whale" + "Moby-
  Dick (1851)") collapses to ONE entry. Per pair the RICHER entry wins
  (longer teaser + scene-specific `exploreAction` target, ≤60 min,
  year-suffixed name), the dropped entry's tags are unioned into the
  keeper (`tags` merge, deduped, keeper order first), and `tier` takes
  the better (1) of the pair so marquee surfacing is preserved (23
  upgrades 2→1). Kept entries are otherwise byte-identical — ids,
  bylines, pageCounts, alt-edition fields (Moby-Dick retains 720
  Penguin Classics) untouched. The web mirror
  (`web/src/data/topics/books.json`) got the identical dedup (its
  schema lacks pageCount, so keepers there just carry tags/tier); the
  desktop port reads the app assets so it inherits the fix. NOTE: the
  same batch-duplication pattern exists in other topic files (astronomy
  94 groups, plants 86, authors 38, songs 36 — see the dedup scan) but
  only books were deduped in this change.
- **v126 — launcher icon no longer tiny + splash drops the old border +
  R8 JNA fix.** (1) **Launcher icon:** `ic_launcher_foreground.xml` inset
  28 → 18dp. The v115 inset drew the card at only ~44×47dp inside the
  108dp adaptive canvas ("the icon inside the icon / looks small") while
  the splash rendered the raw art at 112dp ("why is it bigger when the
  app opens"). At 18dp the art fills 72dp and the card lands ~60×63dp —
  still inside the 66dp safe zone, but matching the splash's presence.
  (2) **Splash:** `SplashScreen.kt` now renders
  `R.drawable.ic_launcher_icon` (the v2 art) instead of the OLD
  `ic_launcher_art` raster, which still carried the previous WHITE BORDER.
  `ic_launcher_art.png` deleted (dead). The user re-sent
  `svgviewer-output (5).svg` — byte-identical (md5) to the archived
  `design/launcher-icon/curio-launcher-icon-v2.svg`, so no re-render was
  needed; the fix was which raster each surface used + the inset.
  (3) **R8:** the v125 `vosk-android` dependency pulls in JNA, whose
  `com.sun.jna.Native$AWT` references `java.awt.*` (Component /
  GraphicsEnvironment / HeadlessException / Window) — missing on
  Android, so `minifyReleaseWithR8` FAILED. Added the standard JNA
  `-dontwarn` rules for `java.awt.**`, `java.beans.**`, `javax.swing.**`,
  `java.applet.**`, `java.nio.file.**` (the AWT interop is desktop-only
  and never invoked on-device).
- **v133 — launcher icon art bigger again (foreground inset 18 → 8dp).**
  The v126 card at ~60×63dp of the 108dp adaptive canvas left a wide sky
  ring around it ("the icon still has a border"), and the App Info screen
  — which composites the adaptive icon UNMASKED as a square — showed the
  card tiny in a sea of background. Measured the source PNG
  (`drawable-nodpi/ic_launcher_icon.png`, 2048px RGBA): the art is a
  rounded rect spanning 84–88% of its OWN canvas with ~14%-of-canvas
  corner arcs (not a small inset card). So the fix is purely the inset:
  18 → 8dp puts the card at ~77×81dp (~71–75% of the icon), and the
  corner arcs still clear the circular launcher mask (radius 54dp in
  canvas units) by ~3.5dp — no slicing on Pixel-style masks. LESSON: the
  adaptive-icon 66dp safe zone is a GUARANTEE, not the target size — the
  visible mask (circle radius 54dp, squircle wider) shows far more, and
  App Info shows the raw unmasked square, so art should fill ~70%+ of
  the 108dp canvas.
- **v135 — progress editor redesign; tolerant saved-topic lookup; all
  reveal tags + decade chip; browser icon tiles; drawer above navbar +
  section hierarchy.** (1) **Progress editor rebuilt** (CurioProgressPill):
  the ring + hidden tappable count is gone — a big count flanked by −/+
  steppers, an explicit "of {target} {unit}" chip with an edit pencil, a
  full-width progress bar + %, and a slider whose `steps` are only used
  when `target <= 200` (big totals run continuous + round) — the old
  `(target - 1).coerceAtMost(600)` steps made a 1000-page book snap to
  non-integer positions that fought the rounded Int state ("the editor
  isn't working"), and the inline target field overflowed the 132dp ring
  Box. (2) **Tolerant topic lookup** (`CurioTopic.matchesSavedName` +
  `TopicCatalog.findByName` + the reveal's pool fallback): the books
  dedupe renamed topics under saved entries ("The Odyssey" → "The
  Odyssey (c. 8th century BCE)"), so exact-name lookups hung on
  "Loading topic…". Tiers: case-insensitive exact → base-name (strip a
  trailing "(…)"/"— …" qualifier) → containment (both sides ≥ 4 chars;
  "Moby-Dick; or, The Whale" ↔ "Moby-Dick (1851)"). An unresolvable
  topic now shows its requested NAME in the hero (HeroCard
  `fallbackName`) and hides the teaser instead of a permanent
  placeholder. (3) **Reveal tags** (per user): ALL tags now render
  (FlowRow wrap — the old `take(3)` thirds cap is gone) plus a derived
  decade chip (`CurioTopic.publicationYear` + `derivedDecadeTag`,
  shared with the Topic Database sort which now delegates to it). (4)
  **Browser icon tiles** (DatabaseTopicRow + DatabaseSectionHeader): the
  old raw `cat.accent` fill/tint (deep accent on a 14% deep-accent tile
  = invisible in dark) now uses the modern theme-aware recipe
  `categorySurface(...)` + `categoryInk()` / `themedAccent()`. (5)
  **Drawer above the navbar**: HomeScreen publishes its drawer state via
  a new `CurioDrawerState` object (mirrors `CurioNavTint`), and the
  NavHost skips `CurioFloatingNavBar` while it's open. (6) **Drawer
  section hierarchy**: section headers are now raised
  `surfaceContainerHigh` pills (solid when open) with a distinct filled
  circle toggle badge, and the expanded rows animate in
  (`expandVertically`) as ONE grouped card (`surfaceContainerHigh`
  45%) — the drawer visibly grows instead of rows silently appearing in
  a same-size sheet. LESSON: a modal drawer covers the whole screen —
  anything the NavHost draws after it (the floating nav bar) sits ON
  TOP of it unless explicitly gated.
- **v136 — offline model picker is a full-height scrolling sheet.**
  `OfflineModelDialog` (SettingsSharedComponents) was an `AlertDialog`
  whose internal `Column` capped the height — the seven-model catalog
  squeezed the rows and clipped the bottom rows ("squished, can't see
  below"). It's now a `ModalBottomSheet` (the app's sheet pattern:
  `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, rounded
  28dp top corners, drag handle, `curioDialogContainerColor()`) with a
  header Row (title + circular close), the intro copy, and a
  `LazyColumn` (`weight(1f, fill = false)`) that scrolls — plus a 20dp
  bottom spacer. Row padding grew 12 → 14×13dp, spacing 8 → 10dp for
  breathing room. The old trailing duplicate `downloadError` Text inside
  the LazyColumn (invalid — raw composable in `LazyListScope`) and the
  leftover `confirmButton` closing are gone. LESSON: `AlertDialog` has a
  fixed max height and clips long lists — a scrolling list belongs in a
  bottom sheet or a `LazyColumn` inside a custom dialog, never a bare
  `Column` in an AlertDialog `text` slot.
- **v137 — CI compile: drawer collapsible groups need a Column host.**
  The v135 drawer put `AnimatedVisibility` (a `ColumnScope` extension)
  directly inside `LazyColumn` `item {}` blocks — `LazyItemScope` has no
  `ColumnScope` receiver, so `compileDebugKotlin` failed with "cannot be
  called in this context with an implicit receiver". Each group now wraps
  its `AnimatedVisibility` in a plain `Column`. LESSON: `AnimatedVisibility`
  is a `ColumnScope` extension — inside a `LazyColumn` item it must be
  hosted by an explicit `Column` (or use `Modifier.animateContentSize`).
- **v138 — offline model downloads: app-scoped manager (pause/resume/
  cancel/multi) + delete hardening + transcribe gating.** (1) **New
  `VoskModelDownloads` object** (OfflineTranscriber.kt): downloads used to
  run in the picker dialog's `rememberCoroutineScope`, so swiping the
  sheet away CANCELLED the transfer. State now lives on an
  application-lifetime `CoroutineScope(SupervisorJob() + Main.immediate)`
  as a `StateFlow<Map<modelId, State>>`; each model gets its own `Job`, so
  several download at once, and closing the sheet leaves them running.
  **Pause** aborts the transfer cleanly (`PauseRequested` thrown inside
  the loop, partial zip kept) and the loop awaits a per-model
  `CompletableDeferred` gate; **resume** completes the gate and re-opens
  the connection with `Range: bytes=<received>-` (206 → append; 200 →
  restart from scratch). **Cancel** cancels the job, disconnects the live
  connection, drops the partial zip and resets the row. `start()` guards
  on `existing.isActive` and `invokeOnCompletion` removes the job entry
  only `if (jobs[id] === job)` so a fresh start after a cancel isn't
  clobbered. (2) **Delete hardened** (`VoskModels.deleteModel`): only
  deletes `filesDir/vosk-models/<id>` when the parent is exactly
  `vosk-models` (a mis-resolved path can never wipe the root), and also
  clears the cached zip. (3) **Transcribe gating** (EntryDetailScreen
  `SoundBiteRender`): the Transcribe button + transcript box previously
  rendered on ANY Sound Bite — including note-only takes with no audio
  file. Both branches are now gated on `!data.audioFilePath.isNullOrBlank()`
  (`if (transcript == null && hasAudio)` / `else if (transcript != null &&
  hasAudio)`). (4) **Visible background downloads**: the Settings →
  Recording "Offline model" row subtitle shows "Downloading <model> ·
  N%" while a transfer runs after the sheet closes; the picker intro copy
  now tells the user downloads survive closing the screen.
- **v139 — offline model picker: real storage usage + big-download
  confirm.** (1) Downloaded rows now show their REAL on-disk size —
  `VoskModels.modelSizeBytes` (walks the model dir) rendered via a new
  GB-aware `formatModelSize` (the app's `formatFileSize` caps at MB; the
  Full tiers run 1–2.3 GB) — e.g. "Downloaded · 41.2 MB". (2) Tapping
  Download on a model ≥ `BIG_MODEL_BYTES` (100 MB — the Large/Full tiers)
  or one bigger than the free space (`VoskModels.availableStorageBytes`,
  `StatFs` on `filesDir`) opens a confirm AlertDialog showing required vs
  free size —  red when it won't fit ("Only X free — the download will
  likely fail") — before `VoskModelDownloads.start` runs. Small models
  with plenty of room still start instantly.
- **v140 — offline model picker: quality-tier badges.** `VoskModels.Tier`
  enum (SMALL/LARGE/FULL with `label` + `hint` — "fast & light" /
  "more accurate" / "most accurate") added to `Info` (set per catalog
  entry). Each picker row now shows a compact tinted badge between the
  name and the subtitle — "Small · fast & light" etc — colored with the
  existing theme-aware inks (  `curioSageInk` / `curioGoldInk` /
  `curioRoseInk`), so the accuracy ladder reads at a glance in both
  light and dark; selected rows flip to white-on-amber. LESSON: reuse the
  theme's ink helpers for tier/category color coding instead of raw
  `CurioColors` constants — they stay readable on dark AND light fills.
- **v141 — reveal morph: byline+year pill row on BOTH the ticket and the
  hero.** The reveal hero's top-left used to wear the ACTION badge
  ("Watch for ~25 min") while the Spin ticket's top-left wore the byline
  ("Director · Nolan") — so during the shared-element morph the pill
  content swapped position/content and visibly jumped. (1) The reveal
  hero's top-left now wears the SAME byline pill row as the ticket
  (identical recipe: `ink.copy(alpha = 0.18f)`, RoundedCornerShape(50),
  labelMedium bold, h12/v6 — the reveal's old Person-icon variant
  dropped), and the action badge moved DOWN to the bottom pill row next
  to the subtype. (2) **Year out of the title**: new
  `CurioTopic.titleAndYearQualifier()` splits a trailing " (…)" / " — …"
  qualifier ("Moby-Dick (1851)" → "Moby-Dick" + "1851"); both the
  ticket title and the hero title render the BASE name, and the year is a
  small `Schedule`-icon pill in the top-left row on BOTH — so the titles
  and pills read identical during the morph. The reveal's decade tag chip
  (v135) is untouched. LESSON: shared-element morphs only look smooth
  when content in the SAME position has the SAME content on both ends —
  matching the card's bounds alone isn't enough if pills/titles swap
  inside it.
- **v315 — book notes bottom sheet + instant reveal + smooth book morph.**
  (User: the book-notes dialog is too slim, add the cover, fix the
  scrollable synopsis, shrink the chapter boxes, use a bottom sheet with
  all chapters switchable, reveal takes a sec despite prior visits, morph
  is laggy on books with synopsis/chapters.) (1) **Instant reveal** —
  `resolved` is SEEDED from `TopicJsonLoader.cached(cat.id)` synchronously
  in `remember(topicName, cat.id)` (strict → saved-name match), so a
  warmed lane renders its quick fact + metadata on the FIRST frame; the
  async Room/JSON chain still runs to enrich + `rememberTopic` persist,
  and `init()` is skipped when `TopicRepository.isInitialized()`.
  (2) **Smooth book morph** — the whole `BookInfoSection` is gated on a
  `bookUiReady` flag (fires ~380ms after entry), so the poster Coil decode
  + chapter `LazyRow` compose only AFTER the shared-element spring settles
  and never stall its frames. (3) **Book notes bottom sheet** —
  `RevealDetailDialog` is DELETED; `BookNotesSheet` (ModalBottomSheet,
  curioDialogContainerColor, 28dp top corners, max width
  `CurioContentMaxWidth`) replaces it for BOTH synopsis and chapters: a
  header row with the book cover (`BookCoverPoster` — same URL + fallback
  as `BookCoverFetch.coverUrlFor`, so the cache key matches the Settings
  bulk fetch) + title/byline + close pill; SYNOPSIS mode shows the full
  text; CHAPTERS mode shows EVERY chapter in a `LazyRow` chip row (active
  chapter accent-filled, `animateScrollToItem` to the opened one) and
  tapping a chip switches the reader in-place (`selectedChapter` screen
  state, sheet instance persists). (4) **Page fixes** — the synopsis card's
  fixed-height inner `verticalScroll` box is gone (full text, card grows,
  poster top-aligned); chapter chips shrank 156→118dp (title 1 line).
- **v316 — reveal instant for FIRST-time topics + Cabinet category-panel
  multi-select (the Topic Database's v314 treatment).** (User: "still in
  the new topics in topic reveal they are slow loading as well, so fix
  them too … and apply the same category picker in cabinet screen as
  well".) (1) **Reveal resolution is now indexed + parse-free where
  possible:** `TopicDao.findByCategoryAndName` (SQL `WHERE categoryId = ?
  AND name = ? [NOCASE] LIMIT 1`) replaces `findTopic`'s old whole-lane
  fetch + Kotlin scan (every row mapped per reveal open); `resolved` is
  SEEDED on the first frame from the warm lane cache AND the prewarmed
  MERGED INDEX (`resolveRevealTopic`: lane cache → `cachedIndex()`, which
  survives lane-cache trims and carries wildcard.json originals) — so ANY
  topic that has ever been loaded renders instantly, and the index fallback
  catches topics whose lane cache was shed. (2) **Stale-Room unmasking:**
  topics added to the JSON between app updates are absent from Room, and
  `TopicJsonLoader.load()`'s Room fast path would then NEVER see them (the
  reveal fell through to a blank). New `TopicRepository.refreshLaneFromAssets`
  parses the bundled asset DIRECTLY (bypassing the Room mask),
  REPLACE-upserts the whole lane back into Room, and the reveal's fallback
  uses it for canonical lanes (WILDCARD keeps the shared merged `load()`);
  hydration of content-incomplete rows is also deduped once per id per
  process (`hydratedIds`). (3) **Cabinet v314 picker** (`CabinetScreen`):
  the sticky every-lane chip bar + its `CategoryIdSaver` single-select are
  GONE — the hero Category pill toggles a collapsed-by-default PANEL
  (`CabinetCategoryPanel` — tiny in-panel search filtering the category
  list, accent `Checkbox` multi-select into a comma-joined enum-name
  `Set<CategoryId>` via `commitFilters`, per-lane entry counts, a
  tertiary-checkbox Legacy row, Clear all + Done); only the ACTIVE lanes
  render as removable one-tap chips (`CabinetActiveFilterChips` + Legacy),
  and searching alone shows NO category chips. Filtering = text AND (no
  categories selected OR entry's category in the set); single lane keeps
  the wash + subtitle, multi-select uses the neutral wash + "N categories".
  Dead `CabinetStickyChipBar`/`CabinetChipPop`/`FilterChipLite` deleted.
- **v318 — picker/panel refinement batch (mix accent washout, per-tab
  persistence, 2-col filter panels):** (1) **Cabinet** — the active-filter
  chip row is now pixel-identical to the Topic Browser's row: same
  `labelLarge` typography (the Bold weight was dropped) and the same
  modifier order (padding → offset → horizontalScroll) so the removable
  chips read as the same pill family; the Cabinet panel + Topic Database
  panel both render their checkbox lists as a **TWO-column
  `LazyVerticalGrid`** (same 260/276dp max-height footprint, roughly half
  the scroll depth; the Cabinet keeps Legacy as a full-span
  `GridItemSpan` row). (2) **New picker — mixes**: `NewMixCard` drops the
  washed-out 16%-alpha lead plate (now a proper `lerp` accent blend) and
  the tiny 16dp dots (16% alpha) for real **lane ICON chips** (20dp
  accent tiles, up to 5 + "+N" for huge mixes), and the cell grows from
  114 → 122dp so the icon row fits; Continue-exploring tiles now wear the
  **category ACTIVE accent** when their lane is in the current deck.
  (3) **Classic page (page 1)** — the Wildcard tile relabels
  "Surprise mix" → "Mix" while multi-selecting (it toggles the lane, no
  longer surprises), the SHARED bottom row's primary capsule swaps
  "Surprise me" → "Mix · N" (applying the pending selection; the page
  reports its count + an apply closure via `onMixStatus`), and the Mix
  row's flat "Cancel" TextButton is now a FLOATING raised pill
  (Close glyph + label). (4) **Persistence ("Curio and Knowledge stay
  persistent too")** — page 0's mode TAB (Curio/Knowledge/Mix) survives
  sheet closes + app restarts (`KEY_PICKER_PAGE0_MODE` +
  `pickerPage0ModeState`, seeded in `initThemeMode`); each tab owns its
  own `rememberLazyGridState` persisted debounced per-tab
  (`KEY_PICKER_PAGE0_TAB_SCROLL_<tab>`), replacing the single page-0
  scroll key (`getPickerPage0Scroll`/`setPickerPage0Scroll` deleted).
- **v319 — share-fact sliders + DB auto-scroll/Also-in switch + mix-name
  pill + morphing hold pills.** (1) **Share-card editor** — the quick-fact
  box's HEIGHT slider felt dead because `lines()` ROUNDED the line count
  (a 12-line fact needed +8% before anything visibly changed, then the
  grid re-flowed "up and down"); it now CEILs so every tick changes the
  line count, and the height sliders step chunkier (steps 42 → 26,
  ~8%/tick) so each notch visibly grows/shrinks the fact box; WIDTH keeps
  its continuous `fillMaxWidth(frac)` per style (Paper's FrostPane
  narrows via the v229c `.then(modifier)` chain).
  (2) **Topic Browser** — the floating back-to-top arrow now
  `animateScrollToItem(0)` (smooth auto-scroll instead of an instant
  jump), flipping a PAGE via the liquid-glass page nav also auto-scrolls
  to the top; tapping an "Also in" pill now SWITCHES the filter to that
  single lane (`commitCats { setOf(id) }`) instead of stacking it into
  the multi-select set. (3) **Spin page** — the deck category pill shows
  the APPLIED named mix's NAME (was always "Mixed · N"): applied mixes
  stamp `AppPreferences.lastMixNameState` (`KEY_LAST_MIX_NAME`, cleared
  on surprise/unnamed/single picks), and the pill reads
  `deckPillLabel(name, count, cat)` across the header + both bottom-bar
  buttons. (4) **Tap-and-hold options** — the dialog-style centered
  panels are GONE everywhere: `HoldActionsPill` (shared, internal) is a
  small rounded capsule that MORPHS in (spring `Animatable` scale
  0.6→1 + fade) holding only CIRCULAR ICON buttons (no text):
  Pin/Spin/Remove (`CategoryOptionPill` rewrite), mix Edit/Delete
  (`MixOptionPill` rewrite), and the Browse page's old Edit/Delete
  DROPDOWN + `BrowseOptionPill` dialog are replaced with the same pill
  (mix rows hoist a `mixHoldTarget` to the screen for the full-screen
  scrim).
- **v320 — compact CH chips + book covers & ratings HUB.** (1) **Chapter
  chips** — the reveal page chip, the book-notes sheet header, and the
  in-sheet chapter pills all read compact `CH N` (was "Ch. N").
  (2) **Book covers & ratings hub** — the Experiments "Book covers" row
  now OPENS a dedicated screen (`CurioRoutes.SETTINGS_BOOK_COVER` →
  `BookCoverHubScreen`, registered in `CurioNavHost`): a provider picker
  (`BookCoverFetch.BookCoverProvider` — Open Library title covers or a
  KEYLESS Google Books title+author lookup), a stats card (books / failed
  covers / rated), actions for "Fetch all covers", "Retry failed (N)"
  and "Fetch ratings (keyless)", a live progress bar + Cancel, and a
  failed-books list with per-row Retry. `BookCoverFetch` is now the
  engine: `resolveCoverUrl` (topic imageUrl first, then the provider),
  `fetchAll(context, provider, onlyFailed, onProgress)` persists the
  failed book names (`KEY_BOOK_COVER_FAILED`/`bookCoverFailedState`, so
  retries survive restarts), and `fetchRatings(context, onProgress)` hits
  the keyless Google Books JSON endpoint (averageRating from
  `volumeInfo`) storing a name→rating map (`KEY_BOOK_RATINGS`/
  `bookRatingsState`). The provider choice persists
  (`KEY_BOOK_COVER_PROVIDER`). The old inline `BookCoverFetchRow` is
  gone (replaced by a `CurioSettingsRow` that navigates to the hub).
  (3) **Reveal ratings** — the synopsis card header and the book-notes
  sheet header show a compact ★ rating chip when the hub's keyless
  Google Books rating exists for that book.
  (4) **Opt-OUT by default (v320b)** — bulk fetching is gated behind
  `AppPreferences.bookFetchEnabledState` (`KEY_BOOK_FETCH_ENABLED`,
  default false): nothing downloads and no Google lookup runs until the
  user flips the toggle at the top of the hub. All hub actions, per-row
  retries, and the engine entry points respect the gate; the Experiments
  row subtitle surfaces the OFF state. ALSO fixed a CI compile error in
  `HoldActionsPill` (`NewCategoryPicker.kt`): the morph-in Animatable was
  named `alpha`, which shadowed the `graphicsLayer` receiver's `alpha`
  property ("val cannot be reassigned / Float vs Animatable" at
  line 1407) — renamed to `popScale`/`popAlpha`.
- **v321 — XP economy: level REWARDS (outfits, share palettes, lane
  order), real-loop dailies, Home quest progress, pet shop.** (1) **Level
  rewards catalog** (`data/LevelRewards.kt`) — levels now UNLOCK
  concrete things instead of just names: 4 premium share-card tones
  (Midnight L2 · Forest L8 · Lavender L15 · Ember L30), 4 pet outfits
  (Explorer Scarf L3 · Scholar Coat L10 · Curio Crown L20 · Galaxy
  Drifter L40), and custom lane order (L5). The Quests level card + pet
  hero footer show "Next unlock at Level N", and the level-up banner
  lists everything newly unlocked by crossing the level.
  (2) **Custom lane order is now a REWARD** (user chose gate): the
  drag-reorder + steppers in Manage Categories stay locked until Level 5
  with a "Custom order locked" notice (hiding lanes stays open to
  everyone). (3) **Real-loop daily quests** — the pool now mirrors the
  app's actual loop: "Reveal 2 new topics" (REVEAL kind, fed by a new
  `CurioQuests.noteReveal` hook beside `CurioPassport.noteReveal`),
  "Save a discovery" (retitled), and "Capture with voice once" (VOICE
  kind, fed by `onSave` when the format is `SoundBite`), plus bonus
  versions (Reveal 4 / Voice twice). (4) **Quest progress on Home** — a
  compact `HomeDailyStrip` under the quest block shows the day's three
  CORE dailies with live progress bars, tap → Quests. (5) **Pet reacts
  on Home** — a quest claim sets `CurioPet.pendingQuestNudge`
  (`consumeQuestNudge`), which the Home flower bed consumes to show a
  one-shot "Quest done! +sparkles ✨" bubble; play remains available through
  the pet's own interactions (`notePlay`, feeding the PLAY daily + persona).
  (6) **Pet outfit
  shop** (`data/PetOutfits.kt` + `features/outfits/OutfitShopScreen.kt`,
  route `OUTFIT_SHOP`, registered in NavHost + center-pop list) —
  pure-cosmetic accessory layers (16×16 art, merged onto the sprite's
  `accessories` detail layer at render time — never mutates saved art),
  funded by SPARKLES: `AppPreferences` sparkle wallet
  (`KEY_SPARKLES`/`sparklesState` + get/add/spend) earned from daily
  claims (+2), weekly claims (+5) and new streak milestones (+5). The
  shop (Quests entry card + Profile row) shows the wallet, per-outfit
  level/price/owned/equipped states, a Buy/Equip pill, a live sprite
  preview, and a next-unlock hint; equipping overlays the outfit
  everywhere the sprite renders.
- **v322 — picker polish (one mix button, hold-pills at the finger,
  per-tab colors, back reopens the picker) + share-editor rework
  (icon-only circular toolbar, tap-to-select, fonts for every element,
  3:4 default).** (User: remove the second mix button in the Spin picker;
  make the mix cards shorter, drop the "scientist · films" text since
  the icons already say it; morphing hold-pills must pop in AT the
  tap-and-hold spot instead of dead-center; Back from the Browse screen
  must re-open the Spin picker; Browse/Mixes/Pins tabs need DIFFERENT
  colors, opaque active states and a bit more size; the share editor
  goes icon-only — every tool is a circular pill, font-size has its own
  icon + dropdown, box-size a crop icon + one small overlay, the
  Customise button now ENTERS edit mode (like hold) instead of opening
  a panel, the style button reveals a design row, aspect toggles
  between the two (3:4 is the default), a new font icon with MORE fonts,
  fact alignments plus a format button with more alignments; edit mode
  no longer lights up everything — tap a thing to select it and its
  circular option pills appear; fonts apply to title/fact/meta/badge;
  Paper's chip must move WITHOUT the bulb; Paper's fact width must be
  editable.) (1) **Picker** — the Classic page's in-page "Mix · N"
  capsule is GONE (the shared bottom row's capsule already applies the
  pending selection; the floating Cancel pill stays, right-aligned);
  `NewMixCard` is 96dp (was 122) with the lane-teaser text removed
  (icon chips carry the composition); `HoldActionsPill` takes an
  `anchor` Offset — every long-press (pinned pill, mix card, continue-
  exploring tile, Browse grid, Mixes row, Pins row) reports the held
  tile's center via `onGloballyPositioned`, and the pill pops in ABOVE
  that spot, clamped to the screen; Browse gets a `BackHandler` that
  pops back AND sets `SpinPickerRequest.pending` (system back now
  re-opens the Spin picker sheet like the arrow does); the three in-page
  tabs wear their OWN accent (`BrowseTab.accent()` rose/gold/sage, ink
  on solid fill), the active tab fills SOLID opaque, and capsules grew
  46→54dp. Saved mix cards now use one stable dark identity tone per mix
  (rather than mixed category tints) and show a selected border/fill when
  active. (2) **Share editor** — `ShareCardMove` grows per-element
  format fields (`titleFont/Align/Bold/Italic`, `factBold/Italic`,
  `metaFont/Bold/Italic`, `badgeFont/Bold/Italic`) applied through new
  `titleStyle`/`metaStyle`/`badgeStyle` wrappers (plus `factBodyStyle`
  bold/italic) threaded through all 8 card styles — the font/format
  tools now act on ANY selected element; `ShareCardResizeTarget` gains
  NONE/META/BADGE and the editor is selection-based: nothing lights up
  on hold, tapping title/fact/meta/badge selects it (faint outline) and
  shows its coffee grip; the Customise overlay panel + labeled sliders
  are replaced by a circular icon toolbar (Design/Aspect/Size/Box/Font/
  Align/Format/Content/Reset/Done) where each tool opens ONE small
  panel — Design shows the design row (+ Current/Classic for Signature),
  Aspect INSTANTLY toggles 3:4 ↔ 9:16 (default is now 3:4/CLASSIC),
  Size/Box act on the selected element, Font lists 13 families
  (`shareFonts`), Align adds Justify (Left/Center/Right/Justify),
  Format toggles Bold/Italic, Content holds the source list + custom
  fact + collage photo/caption + vinyl song; the floating button
  toggles edit mode (Customise ⇄ Done). (3) **Paper fixes** — the
  category chip now moves ALONE (the decorative bulb stays anchored
  top-right; also applied to Vinyl's badge row), and the fact width is
  editable: `moveFact` applies `fillMaxWidth` unconditionally and
  `FrostPane` no longer carries a redundant outer full-width fill, so
  the crop actually narrows Paper's pane.
- **v323 — share-editor refinements (user report) + cover z-order fix.**
  (1) **Tool panels stay open** — picking an option inside a tool (Design /
  Text size / Box size / Font / Alignment / Format) no longer collapses the
  panel (the `toolOpen = null` in every option pill is gone); tap the tool
  icon again to close it, so the user can keep switching options mid-edit.
  (2) **Quick-fact select-only-first** — the fact's `BasicTextField` is now
  `enabled = factEditMode` (INERT by default, so a tap can never hijack the
  selection into text editing); an invisible tap layer over the field selects
  the box for moving (grip appears) WITHOUT opening the keyboard, and a new
  conditional **Edit text** tool pill (shown whenever FACT is selected) arms
  the field and focuses it via a `FocusRequester`
  (`LaunchedEffect(factEditMode)` requests focus). Tapping Title / Info row /
  Category chip calls `LocalFocusManager.clearFocus()` BEFORE switching the
  selection, and the field's `onFocusChanged(false)` drops `factEditMode`, so
  text editing never sticks and the keyboard never lingers over the toolbar.
  (3) **No accidental sheet dismissal while editing** —
  `TopicShareSheet`'s `ModalBottomSheetState` now uses
  `confirmValueChange = { if (editMode) it == SheetValue.Expanded else true }`
  (blocks swipe / drag-handle collapse) and `onDismissRequest = { if
  (!editMode) onDismiss() }` (blocks back button + scrim taps); Save /
  Share / Share-as-text still dismiss normally, and Done re-enables
  dismissal.
  (4) **Book covers render again** — the v317/v320 gradient placeholder was
  a LATER `Box` sibling of the `AsyncImage` in `BookCoverPoster`, so it
  painted ON TOP of every loaded cover ("just a gradient showing" on the
  reveal page + book-notes sheet); it now lives on the outer Box's own
  `Modifier.background`, BEHIND the image, so covers show again and the
  gradient only appears while loading / when all candidates fail.
- **v317 — merged book-notes sheet (Synopsis | Chapters tabs, expands to
  top) + "Also in" from All + share-card editor overhaul + cover fetch
  under Experiments.** (User: synopsis should show only 5 lines on the
  reveal page with the FULL text in the bottom sheet; merge chapters +
  synopsis into ONE sheet that isn't small and expands to the top; the
  Topic Browser hides "Also in" when searching from All; share sheet:
  drop the "Share this topic" header while editing, haptics on the
  sliders, the Paper quick-fact box must change height as you type and
  the caret must sit on the real glyphs, T/F/M/B colored handles → ONE
  coffee move grip with a darker coffee border, the chip grip must ride
  the ACTUAL pill (it sat at a guessed corner), the bulb icon must move
  with the chip, rule lines above quick facts must travel with the fact
  box, title + info must move as one when adjacent; book-cover fetch
  needs a Cancel and lives in Experiments.) (1) **Reveal** —
  `BookSynopsisCard` clamps the page preview to `maxLines = 5` (teaser +
  "Read the full synopsis →"); `BookNotesSheet` is now ONE tall sheet
  (body `fillMaxHeight(0.92f)`, expand-to-top, inner body scrolls) hosting
  BOTH sections behind segmented Synopsis | Chapters pills — the tab it
  opens on mirrors what you tapped, and switching happens in place. The
  `BookChapterChip` 118dp 2-line preview boxes stay on the page.
  (2) **Topic Browser** — the "Also in" suggestion row now ALSO renders
  when searching from ALL (it was gated on a lane being selected): from
  All it surfaces the lanes the flat results came from (top 6 by hit
  count). (3) **Share-card editor** — sheet header + style label + dots
  hide while editing; `SizeSliderColumn` haptics tick per snap step
  (thumb crossing) + confirm on release (haptics captured in the
  COMPOSABLE scope, not inside the Slider's plain callbacks); the Paper
  + Vinyl fact fields report the GLYPH BOX (inner Text, inside the
  FrostPane/cream padding) so the overlay `BasicTextField` (now
  `maxLines = 24` + `heightIn(min)` auto-grow) types exactly on the
  letters with a matching caret; `EditBoundsCallbacks.onFactStyle` lets
  every style report the TextStyle it actually used so the invisible
  field wraps/advances identically; the old per-box T/F/M/B letter
  handles are GONE — one `MoveHandle` (DragHandle icon, `CoffeeChromeDeep`
  circle + white border) + `CoffeeChrome`/`CoffeeChromeDeep` box borders;
  the badge grip anchors to the reported PILL bounds (full-width chip
  rows report the pill Surface, not the row — a full-width rect would
  zero the drag clamp), and the pill + bulb move as ONE group (moveBadge
  on the row, onBadge on the pill) so the bulb rides the chip; accent
  rules above quick facts (Vinyl underline, Editorial hairline, Minimal
  rule) get `factShift(move)` so they travel with the F drag; meta rows
  under titles get `titleShift(move)` so title + byline move together.
  (4) **Settings** — `BookCoverFetchRow` moved from the Safety & support
  hub into `UserExperimentsScreen` (new "Content tools" section) with a
  Cancel pill that cancels the in-flight job (cached stays); dead
  hub special-case branches + ROUTE const deleted.
- **v142 — Manage Categories full-bleed bottom; Pet Designer floating
  pill bar + fade open; first-run "Pick a lane" wired to the Spin picker.**
  (1) **Manage Categories full-bleed** (per user, confirmed): the NavHost
  no longer applies `windowInsetsPadding(navigationBars)` to the
  MANAGE_CATEGORIES route (new `fullBleedBottomRoutePrefixes` set — the
  v132 reveal precedent); the page's wash runs to the bottom edge and the
  screen clears the gesture bar itself (`navigationBarsPadding()` on the
  LazyColumn + scroll indicator). LESSON: the NavHost's generic nav-bar
  inset for push routes shows as a reserved strip under pages that paint
  their own wash — full-bleed pages opt out and pad their own list.
  (2) **Pet Designer** (per user, confirmed "both"): the studio's
  bottom nav (`PetStudioBottomNav`) was the app's OLD stock M3
  `NavigationBar` — restyled to the v124/v129 floating pill bar recipe
  (`surfaceContainerHigh` rounded-50 container, 6dp shadow, solid
  `secondary` fill + `onSecondary` ink active capsule, 52dp tabs); the
  removed NavigationBar/WindowInsets imports are gone. The route also
  opens with the reveal's clean fade (new `isPetDesignerRoute` branches
  in enter/exit/popEnter/popExit before the scale-pop group) instead of
  the mechanical zoom. (3) **First-run "Pick a lane"** (HomeScreen
  `FirstTimeEmpty`) previously opened the separate full-screen
  `CurioRoutes.PICKER` page; it now sets a one-shot `SpinPickerRequest.pending`
  flag (new object in CurioBottomNav beside `CurioDrawerState`) and
  navigates to the Spin TAB, whose `LaunchedEffect(SpinPickerRequest.pending)`
  opens its own `CategoryPickerSheet` (lane chips + Mix presets). The
  `LaunchedEffect` is keyed on the flag so it fires even when the Spin
  tab was already composed. LESSON: a cross-screen "open this sheet"
  request belongs in a shared state object (the `CurioDrawerState`
  pattern), not a route arg.
- **v158 — Full server-grade Vosk models removed (no medium tier exists) +
  the dictation mic is now on EVERY note/quote text box in Save your take.**
  User: "remove the full models as they are laggy and crashing the app
  along with my phone. and add medium model if theres more. in voice model.
  and the voice bubble in save your take show it in each note and quote
  text box not just in sound bite". (1) MODELS: the 1–2.3 GB Full-tier
  models (Full English / Gigaspeech English / Full Indian English) are
  GONE from `VoskModels.CATALOG` (now Small ×3 + Large ×1); the
  `Tier.FULL` enum value, its rose badge tint and the picker copy were
  removed. RESEARCH: alphacephei.com's catalog has NO English "medium" —
  the ladder is Small ~40–60 MB / Large ~128 MB / server-grade 1–2.3 GB,
  so nothing was added. A stale-install guard runs at startup
  (`VoskModels.pruneRemovedModels` in MainActivity): any installed dir
  whose id is no longer in the catalog is deleted (zip too), and a stale
  saved selection is cleared; the detail-page Transcribe button also
  verifies `VoskModels.byId(modelId) != null` before loading. LESSON:
  removing a heavy-download feature must handle the already-downloaded
  artifacts + persisted selection, not just the picker list.
  (2) DICTATION EVERYWHERE: the SoundBite-only floating-mic flow was
  extracted into a SHARED reusable composable `DictationMic`
  (features/capture/formats/DictationMic.kt) that owns its own
  recognizer session (lazy, destroyed on dispose) + its own RECORD_AUDIO
  permission launcher + the live-preview dialog, and reports live-listening
  via `onListeningChange` (SoundBite keeps reporting dictation as busy so
  format-switch confirmation still guards a live session). SoundBiteFormat
  lost its ~330 lines of inline recognizer machinery + private dialog;
  the mic now rides the tool dock (`trailingAction`) of EVERY RichTextEditor
  note box (Field Notes' 3 sections, Marginalia journal, Reel Notes
  review, Sound Bite note), the GalleryWall caption (PaperLineField
  label row) and every quote card via the SHARED `QuoteCardEditor`
  (covers all QuoteCardsSection callers + the mood board + the floating
  quote dialog). Every mic is gated on `AppPreferences.voiceToTextEnabledState`
  (the v125 experiment toggle) — visible only when the setting is on.
  Insert appends the transcript to the box; quote cards preserve spans
  via `QuoteCardsState.setText`'s clamping. LESSON: when a one-off
  feature (SoundBite's mic) becomes an everywhere-feature, extract the
  machinery once into a self-contained composable and reuse it — the
  caller keeps only the state var + an append lambda, and the busy-state
  callback is the one thing the shared piece must surface back.
- **v159 — nav pill height slimmed 60 → 48dp (lengths kept), bar + reveal
  Like/Dislike together.** User: "the navbar pill height is too much keep
  its lengh but decrase the heigh the widneness same in like and dislike".
  `FloatingPillHeight` (CurioBottomNav) and `RevealSentimentHeight`
  (TopicRevealScreen) both 60 → 48dp; the widths stay 60/128dp and the
  icon stays 26dp (fits fine with 11dp of breathing room). The bar's
  capsule container has its own padding, so a shorter pill just shrinks
  the capsule — nothing else needed. LESSON: pill geometry is three
  independent constants; the user's "keep length, shrink height" is a
  single constant change applied to both copies of the size trio.
- **v160 — the remaining dark-mode hairline rims are ALL gone (the 5
  surfaces the v157 note flagged + the Spin one).** User: "remove the
  same darkmode hairline that u mentioned exosist in more elements". The
  v149 `BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))` rim was
  stripped from: the tour Skip/Next dock (CurioNavHost), the reveal
  Like/Dislike pill (TopicRevealScreen), the category picker's "Manage
  categories" pill (SpinScreen — the one the v157 note didn't list), and
  the pet studio bar + floating action capsule (PetDesignerScreen).
  Each Surface keeps its `shadowElevation = 6.dp` and theme container
  fill; the now-unused `BorderStroke` import went in all four files
  (CurioNavHost also lost its now-unused `isCurioDarkTheme` import).
  Non-rim `BorderStroke` uses (badges, chips, quest medals, glass)
  untouched. LESSON: the v157 note enumerated the rim sites but missed
  the Spin one — a pattern sweep (`grep BorderStroke` for the
  `White.copy(alpha = 0.10f)` signature) catches every copy before
  claiming "all gone".
- **v161 — nav collapse smoothed for real + Cabinet "All" yellow pill
  fixed (primary fallback) + rail/pet-studio audit.** User: "the collapse
  animation of buttom nav pill is still bad and also why its yello in
  cabinet all. fix that and do more audit". (1) COLLAPSE: the v155
  smoothing missed the two real culprits — the label EXIT was
  `fadeOut(tween(0))` (the closing pill's text vaporized while the pill
  took ~1s to deflate: a dead empty box shrinking) and the width spring's
  `StiffnessMediumLow` dragged the 128→60dp collapse out a full second.
  Now: exit fades 160ms FastOutSlowIn with the shrink, stiffness
  MediumLow → Medium (damping stays 0.9 — near-critical, no overshoot),
  applied to `FloatingNavPill`, the reveal `SentimentSegment`, AND the
  pet studio `PetStudioTab` (audit catch: it still ran the OLD v124
  recipe — damping 0.75, which overshot and bounced, plus the instant
  vanish). (2) CABINET YELLOW: `FloatingNavPill` fell back to
  `colorScheme.secondary` (ButterYellow) whenever the page published no
  accent — Cabinet "All" publishes none (plain page) → stray yellow pill.
  Fallback is now the theme PRIMARY (coral, the app's brand color, with
  `onPrimary` ink). (3) AUDIT: the wide-window `CurioNavigationRail` had
  the SAME yellow (hard-coded `secondary` indicator) AND never wore page
  accents — it now resolves `curioNavActiveAccent(selectedRoute)` like
  the pill bar (page accent or primary fallback). The pet studio bar's
  amber active fill is INTENTIONAL (pet brand) and untouched. LESSON:
  "smoother animation" fixes need to target the exit spec + spring
  stiffness, not just damping; and every sibling that copies a recipe
  (rail / pet studio / sentiment) must be swept when the recipe changes.
- **v162 — tab-switch motion runs on ONE spring family; all pill parts now
  move in lockstep.** User: "Record the tab-switch motion and tune the pill
  spring/fade until the collapse reads perfectly smooth". (No screen
  recording / device available in this env — the tune is a spec-level
  audit of the animation timeline.) AUDIT FINDINGS: v161 had only fixed
  the WIDTH spring — the fill `animateColorAsState` still ran the old
  `StiffnessMediumLow` spring (lagged the pill), the icon tint ran a
  `tween(200)` and the label AnimatedVisibility ran its own `tween(240/
  160)` — all three finished or lingered OUT OF STEP with the pill width
  (the label was fully in/out while the pill was still mid-flight; the
  fill was still catching up after the pill settled). FIX: one shared
  spring family per pill — `PillWidthSpring` (`spring<Dp>(0.9, Medium)`)
  for the width and `PillMotionSpring` (`spring<Float>(0.9, Medium)`) for
  the fill, icon tint and the label's `expandHorizontally`/`shrink
  Horizontally`/`fadeIn`/`fadeOut` (the expand/shrink ALSO got the spec
  — their default is damping 1.0, a different shape than ours). Identical
  params from the same start frame = identical trajectories = perfect
  lockstep. Applied to `FloatingNavPill`, the reveal `SentimentSegment`
  and the pet studio `PetStudioTab` (its fill stays a deliberate solid
  snap, v156 design). LESSON: "sync everything to the same spring" is
  the fix for perceived jank in multi-part morphs — check EVERY animated
  property (width, fill, icon, label) for its own spec, and pass the
  spring to expand/shrink, not just fade.
- **v163 — quest badge glyphs un-squished (normal weight) + raw Dialog
  windows get a springy open entrance.** User: "more smoother open
  aniations of things and fix the badge icon just the quest badge icons".
  ask_user clarified: badge icons looked CLIPPED/SQUISHED, and the open
  animations that bothered them were DIALOGS & SHEETS. (1) BADGE:
  `CurioBadgeMedal` drew every glyph at `weight = FontWeight.Bold` —
  Material Symbols at wght 700 render very heavy, and inside the medal's
  tight inner plate (0.80× with a 1.5dp ring) the heavy glyphs crowded
  the ring and read squished/clipped. All three icons (earned glyph,
  locked StarOutline silhouette, tiny earned check) now render at NORMAL
  weight — the clean outlined stroke with breathing room. The full
  material_symbols_outlined.ttf has every ligature, so glyph NAMES were
  never the problem. LESSON: "icon looks clipped" inside a tight circle
  + a heavy font weight = drop the weight, don't shrink the size.
  (2) DIALOG/SHEET OPENS: the app's M3 AlertDialogs and ModalBottomSheets
  already animate; the raw `androidx.compose.ui.window.Dialog` windows
  (the full-screen mood board in GalleryWallFormat, its floating quote
  editor, and EntryDetailScreen's expanded mood board) popped in with NO
  animation. New shared `CurioDialogEntrance(scale, content)` in
  CurioAnimations.kt — fade + near-critical scale-up (spring 0.9/380)
  played on the first frame via MutableTransitionState (same trick as
  ScreenEntrance); `scale = 1f` = pure fade for full-screen canvases that
  shouldn't zoom, 0.96 for the floating quote card. Wrapped all three
  raw Dialog sites. LESSON: audit the raw `Dialog(` call sites — they're
  the only pop-in-instantly windows; M3's own dialogs/sheets animate.
- **v164 — navbar tab labels bumped SemiBold → Bold (Material text style).**
  User: "use new bold fonts for navbar texts material text". The floating
  pill bar's tab labels (Home · Spin · Cabinet) rendered
  `labelMedium.copy(SemiBold)`; they're now `labelMedium.copy(Bold)` —
  the bolder label reads proper inside the big 60/128dp pill. Same bump
  applied to the wide-window rail labels, the reveal Like/Dislike labels
  and the pet studio tab labels so every pill-family label matches.
  Geom (the app's display font) declares a real Bold face, so no fake-
  bold synthesis. LESSON: the pill label weight lives in FOUR copies
  (pill, rail, sentiment, studio) — bump them together.
- **v166 — nav collapse gentler + SAME animation on all pills; muted
  active-pill colors (Cabinet "All" fallback is no longer pink); calm
  page openings; dark-mode session-note editor.** User: "make the nav bar
  collapse animation slower a little not violent and smoother. and also
  chnage the bright colors to use muted colors and in cabinet all use the
  default theme aware color. not pink. from the spin shuffle option, and
  fix the dark mode session note text box..." then "also same animation
  for all pills, and also the page opening have become too violent did u
  edit it?"
  - PILLS — the whole family now runs `spring(dampingRatio = 1f,
    stiffness = 750f)`: CRITICALLY damped (zero overshoot/bounce — the
    old 0.9 damping still snapped) and half of Medium stiffness ("slower
    a little", still ~3.5x snappier than the MediumLow that dragged a
    second). Applied IDENTICALLY to the nav pill bar (4 typed specs),
    the reveal Like/Dislike segments (4) and the pet studio bar (3) —
    every animated pill moves with the exact same physics.
  - MUTED COLORS — the active pill wore the raw saturated page accent
    (loud/neon). New `curioActivePillFill()` (CurioBottomNav): light
    mode pulls saturation ~45% via `toHsl/fromHsl` (hue + lightness
    preserved so deep accents keep white ink); dark keeps the already-
    muted deep jewel tone, pastel keeps its airy twin. The reveal
    SentimentSegment got the same mute. Rail shares the same helper.
  - CABINET "ALL" FALLBACK — plain pages (Cabinet "All") fell back to
    `colorScheme.primary` = CoralBlush, which read as a stray PINK pill
    (v161 had swapped butter-yellow → coral; the user calls the coral
    "pink from the spin shuffle option"). Fallback is now the MUTED
    `secondaryContainer` + `onSecondaryContainer` ink — a standard
    theme-aware M3 pair (soft warm in light, subtle glow on black),
    never a bright hue.
  - PAGE OPENINGS — answer to "did u edit it?": the NavHost screen
    transitions were NOT touched (they've been tween-based since v7.17;
    detail/pop screens used a 0.88 scale pop). But three spring
    entrances DID read violent: the NavHost detail/pop scaleIn/scaleOut
    now opens at 0.94 (half the zoom), `CurioDialogEntrance` (v163,
    mine) dropped its underdamped 0.9/380 spring for the shared
    `Springs.Calm` (1.0/750 — zero overshoot), `MorphEntrance`'s
    non-bouncy path (category grids) swapped the overshooting
    `Springs.Deliberate` (0.85/250 — slow + zoom-back) for `Calm` at a
    closer 0.92 start, and `ScreenEntrance` (Support/Promo/TopicHistory)
    slid on `Calm` too. New `CurioMotion.Springs.Calm` = the pill
    family's exact physics, so screens and pills share ONE spring
    signature.
  - SESSION NOTE — `SessionNoteFloatingPill`'s popup editor was a BRIGHT
    cream paper sheet in dark mode (the note-paper palette is theme-
    agnostic for SAVED notes, but this floating popup is a UI control).
    Dark mode now swaps the sheet to `surfaceContainerHigh` + `onSurface`
    ink, and the `OutlinedTextField` gets explicit paper-paired colors
    (focused/unfocused text, placeholder, cursor via
    `paperControlAccent`, paper-derived borders) — the M3 defaults were
    painting light text over the bright cream (invisible-text bug).
    Light mode keeps the cream paper exactly.
- **v167 — reveal Like/Dislike pill wears the dynamic page tint; nav
  pill tap ripple removed.** User: "the like and unlike button pill doesnt
  get the backgroud tint with dynamic theme fix it. and dont add the
  touch shado in nav bar".
  - Reveal pill container was a STATIC `surfaceContainerHigh` — it never
    picked up the page tint the nav bar capsule gets. Split
    `curioFloatingNavContainer(routePrefix)` into a shared lift helper
    `curioFloatingNavContainerFor(wash)` (light: wash lifted 30% toward
    surfaceContainerHigh; dark: surfaceContainerHigh) and the reveal
    pill now passes `cat.categoryBackgroundWash()` — the reveal page's
    own tint — so the capsule matches the page like the nav bar does.
    (The route-keyed version can't reach the reveal's wash; the reveal
    isn't a tab route.)
  - "touch shado" = the tap RIPPLE (ask_user confirmed: "Remove the tap
    ripple"). FloatingNavPill's clickable now passes `indication = null`
    with a remembered `MutableInteractionSource`, so tapping a tab never
    flashes the grey ripple circle. The 6dp drop shadow under the bar
    stays (user picked ripple only). LESSON: "touch shadow" in this
    project's    user-speak = the press ripple — ask rather than assume
    which shadow.
  - v167 CI FIX: `CurioAnimations.kt:70` — `SpringSpec<Float>` (the new
    `Springs.Calm`) passed to `slideInVertically`, which animates
    IntOffset. The calm spring there is now typed inline
    `spring<IntOffset>(1f, 750f)` (same physics). LESSON (same as v165):
    a spring's generic must match the ANIMATED value's type —
    slideInVertically/Out = IntOffset, scaleIn/Out + fade = Float,
    expand/shrinkHorizontally = IntSize, colors = Color.
- **v168 — progress editor dialog: the count is a single number in the
  top corner (tap → inline edit, Enter saves, replay icon resets), and
  the "0 / pages" line + "Edit total" chip are removed.** User: "from
  the progress editor dialog remove the 0/pages option and move it to
  the top corner and just show one no. and tapping it automatically lets
  u edit and when u tap enter it saves and it lets u reset the number to
  default too. and also remove the edit total option too and dont push
  this". CurioProgressPill.kt's CurioProgressEditorDialog:
  - The "$value / $target $unit" line under the ring and the v149
    "Edit total" chip + its inline target field are GONE (the target now
    comes only from the topic data or the alternate-edition pill's
    [initialTarget] prefill, which still persists via Save).
  - A top-right Row in the dialog's content shows ONE number (the
    current count, no suffix): tap → the BasicTextField opens in place
    (number keyboard, IME Done), Enter → [commitValueEdit] persists
    immediately (TopicProgressStore.set / clear on 0). A 26dp replay
    icon beside it resets to the default (0) and persists.
  - The ring keeps just the big %; −/+ steppers + slider unchanged;
    Finish + Save unchanged.
  - COMMIT ONLY — the user said "dont push this" (push rides with the
    next real change). LESSON: the user's "0/pages" = the "0 / 350
    pages" count line under the ring — shorthand for current/total
    display.
- **v169 — category-picker "Manage categories" is a proper floating,
  theme-aware, text-only capsule; filters' Apply/Show-all drops its check
  tick.** User: "in category picker the manage category option there a
  scafhold or strip behind the button make it proper floating, and theme
  aware and remove the tick mark just text and same in filters show all
  topics dont push". SpinScreen.kt:
  - Manage pill (the picker sheet's bottom action): was a full-width bar
    (`fillMaxWidth()` — read as a strip). Now content-sized + centered
    via ColumnScope `.align(Alignment.CenterHorizontally)`, and its
    static `surfaceContainerHigh` color became the nav-pill's dynamic
    container `curioFloatingNavContainerFor(currentCat.categoryBackgroundWash())`
    — the sheet's wash lifted 30% toward the elevated surface in light,
    elevated dark in dark (the sheet itself is `currentCat.categoryBackgroundWash()`
    tinted). Text-only: the CurioIcons.DragHandle glyph + Spacer are gone.
  - Filters' Apply / Show all topics pill: the leading CurioIcons.Check
    tick + Spacer are gone — text only (accent pill design unchanged).
  - COMMIT ONLY — the user said "dont push" (still unpushed: v168 too).
    LESSON: "scafhold or strip" = a full-width bottom action read as a
    bar; "proper floating" = the content-sized centered capsule language;
    "tick mark" = the leading glyph in the pill row.
- **v170 — Edit profile dialog restructured: "Your name" + "Bio"
  sections (bold, larger, icon per heading), tagline field becomes the
  Bio, tagline label + automatic-tagline button + helper texts removed.**
  User: "now in edit profile dialog, make the your name and the line
  under it text ith Your name and in bold and larger text then below Bio
  similiar way, remove the tagline text, and remove the automatic tagline
  option and also the leave the tagline empty to use the automatic streak
  one that tet too. and make the profile photo text a little bigger, and
  fix the page margin and hirarcy and maybe add icons etc and dont push".
  ProfileScreen.kt ProfileDialogs:
  - The helper "Your name and the line under it." became a section
    heading **"Your name"** (titleMedium ExtraBold + Person icon); below
    it the name field (label "Display name" removed → placeholder
    "Your name"). Below that a **"Bio"** heading (same style + Note
    icon) over the tagline field — the tagline field IS the bio (the
    app has NO bio data model; the "line under the name" is the custom
    streak tagline). Its "Tagline" label is gone → placeholder "Keep
    the spark going today.".
  - REMOVED: the "Use automatic tagline" TextButton (+ onResetTagline
    param + caller wiring) and both helper texts. Leaving the field
    empty still falls back to the automatic streak line (unchanged
    behavior — getCustomStreakTagline.ifBlank { taglineForStreak }).
  - "Profile photo" label bumped labelLarge → titleMedium ExtraBold +
    an Image icon; sections spaced 16dp with 8dp heading→field gaps
    ("fix the page margin and hierarchy"). New EditSectionLabel helper.
  - COMMIT ONLY — "dont push" (v168 + v169 also unpushed). LESSON: the
    "line under the name" = the custom streak tagline — the user calls
    it "Bio"; no bio model exists, so the tagline field is renamed, not
    a new field.
- **v171 — share card is a 3:4 portrait with the sharer's name, session
  note and attached photo; light-mode dialogs wear the theme-aware
  container (not cream white).** User: "now improve the share card and
  not squared but 3:4 and also use name and the note user added that
  option with photos option if added. just more beautiful to share. and
  also give each dialog in light mode the theme aware backgroud not
  cream white".
  - CurioShareCard (EntryDetailScreen): exported at 450×600 dp (3:4,
    was 400×400 square; the sheet preview Box matches at 280 × 3:4).
    New content: the sharer's display name (AppPreferences.getDisplayName,
    bottom), the session note (entry.sessionNote, a rounded note block
    under the teaser), and the FIRST attached photo
    (entry.sessionScreenshots, a 200×150 rounded block). The photo is
    decoded SYNCHRONOUSLY via BitmapFactory → asImageBitmap inside
    remember — Coil's async painter would miss shareComposableCard's
    single-frame capture (the preview and the PNG must show the same
    photo). Format + date chips collapsed into one quiet "Format ·
    Captured..." text line.
  - curioDialogContainerColor (CurioTheme): the light branch returned
    lerp(surfaceContainerHigh, background, 0.72) — 72% toward the cream
    background read as a cream-white panel. Now returns
    surfaceContainerHigh directly (the warm tan the floating pills /
    chips wear) — theme-aware, applies to EVERY AlertDialog via this
    shared function. Dark branch untouched.
  - PUSHED (this request had no "dont push" — the accumulated v168/v169/
    v170 commits rode along). LESSON: "photos option" = the entry's
    sessionScreenshots attachments; synchronous decode is mandatory for
    the off-screen card capture.
- **v174 — drawer redesigned as a "tiny personal observatory".** User
  pasted a full design spec + dropped an SVG (`svgviewer-output (10).svg`
  → moved to `res/raw/drawer_footer.svg`) and said "ask if not understand
  the plan". ask_user: stats = REAL data where it exists (fallback to the
  design's numbers only where no counter exists); the 3 nav rows KEEP
  tap-to-expand sub-rows (collapsed by default, per "no accordion
  expansion by default" = restyled rows, not dropped functionality).
  HomeScreen.kt (`HomeDrawerContent`):
  - HERO: rose Surface → gradient sky Box (light seafoam #C2E8DE→
    #E9F6F0, dark twilight #12313A→#1D4750) clipped to the torn shape;
    `DrawerCelestialSky` paints 30 seeded stars, 3 four-point sparkles
    (`drawSparkle`), a faint dipper constellation and a punched crescent
    moon (punch uses lerp(skyTop, skyBottom, y) = the local gradient
    colour); `DrawerRollingHorizon` adds two cream hill bands just above
    the tear (dark = warm tan so cream ink stays readable). Avatar ring
    is now cream; greeting ink steps with the sky.
  - CURIOSITY MAP: `DrawerCuriosityMap` card (rounded 24, opaque pastel
    lerp — shadow-safe) with "Your Curiosity Map" + subtitle + a
    decorative "This Week ˅" pill; `ConstellationBrain` Canvas draws an
    abstract brain (22 normalized nodes, gold→blue lerp by x, glow on
    hot nodes, bigger "important" stars) with the "Overall / Curiosity /
    N" score overlaid at the centre; 7 orbiting `MapStat`s (icon +
    label + bold value): Learned/Questions/Shared = the DESIGN numbers
    (no counters), Explored=explores, Topics=spins, Saved=saves,
    Streak=StreakTracker.getStreak. Overall = sum of the six.
  - NAV ROWS: `DrawerNavRow` replaces DrawerSectionHeader — tinted icon
    chip, bold label + subtitle, chevron (right = direct nav for Quests
    & Levels; up/down = tap-to-expand for Your Curiosity / About). The
    5 sub-rows (Topic History / Manage Categories / Browse Topics /
    Support / Replay intro) keep the existing AnimatedVisibility groups.
  - FOOTER: `DrawerFooter` loads res/raw/drawer_footer.svg via Coil's
    SvgDecoder (NEW dep io.coil-kt:coil-svg:2.7.0 — sibling of the
    existing coil-compose; libs.versions.toml coilSvg) in a rounded,
    shadowed box; a vertical surface→transparent gradient fades its top
    ("fading look"); "v{version} · Made with curiosity ♥" sits on the
    landscape (footerInk warm tan #7E6E50). Old pinned footer Column
    removed.
  - v174b ("continue the plan"): the constellation's BEHAVIOUR is now
    data-driven — per-node `weights` (0..1 per region: likes → left
    hemisphere, saves → right, pins → inner, spins → top, explores →
    brainstem) scale each star's radius (1.9+2.4w / 3.0+2.4w big) and
    glow (0.07+0.15w); fissure `bridges` draw progressively from the
    relationship score (quotes+pins+likes: >0→1, >5→2, >20→3, >60→4,
    >150→6); the two centre-fissure nodes keep the "recent discovery"
    glow. Sky got "extremely subtle grain" (52 micro-dots, alpha
    0.05-0.13, seed+17). Map card got the brief's "thin borders": a
    1dp seafoam hairline in LIGHT mode only — dark stays borderless
    (v157 hairline rule).
  - LESSON: SVG-at-runtime = coil-svg (keep the artist's file as-is); a
    332KB hand-drawn SVG is NOT safe as a VectorDrawable (aapt2 path
    blob). Pastel fills under shadows must be opaque lerps (rule 11).
- **v174c — new Stats page ("Your Curiosity") + CI fix.** ask_user on the
  stat-page design: centerpiece = interactive constellation brain;
  sections = ALL (streak+level, lifetime totals, per-category, quests &
  badges); reachable from drawer AND Profile; style = observatory.
  - CI FIX: CurioProgressPill.kt:342 — the v168 progress-dialog rework
    wrote `horizontalAlignment = Alignment.End` on a ROW (that's a Column
    param). Row uses horizontalARRANGEMENT — fixed to
    `Arrangement.spacedBy(8.dp, Alignment.End)`. LESSON: Row = horizontal-
    Arrangement + vertical-Alignment; Column = horizontal-Alignment +
    vertical-Arrangement — the v168 rename flipped them.
  - NEW FILE app/src/main/java/com/curio/app/features/stats/StatsScreen.kt:
    StatsScreen(navController) registered as CurioRoutes.STATS ("stats")
    in NavHost. Celestial sky header (StatsSkyHeader, mirrors the drawer
    palette), StreakLevelCard (streak/best + level + xpProgress bar),
    StatsConstellationCard (INTERACTIVE CategoryConstellation Canvas:
    one star per explored lane, two-lobe arc layout with deterministic
    jitter seeded by CategoryId.name.hashCode, radius 5.5+min(count,60)*
    0.30dp, glow on lanes saved this week, tap within 34dp selects via
    detectTapGestures, empty tap clears; accents resolved in COMPOSITION
    — themedAccent is @Composable and CANNOT run in the draw lambda),
    LifetimeTotalsCard (2-col grid of the 8 lifetime counters),
    JourneyCard (stages progress + CurioBadgeMedal row + Quests link),
    LanesBreakdownCard (per-lane rows). Data: repo.getAll() grouped by
    topic.categoryId ∪ CurioQuests.categoriesState (CategoryId.valueOf in
    runCatching), StreakTracker, xpProgress. StatsCard shell = same
    opaque pastel card as the drawer map.
  - ENTRY POINTS: drawer — DrawerCuriosityMap got onClick (whole card
    opens stats; chevron hint added) + a "Stats & insights" sub-row
    (glyph "monitoring") in the Your Curiosity group; Profile —
    ProgressAndAchievementsCard gained onOpenStats + a compact Stats pill
    in the XP header row.
  - CI FIX 2 (v174c): StatsScreen.kt "Unresolved reference 'Column'" at
    every `StatsCard {` + the declaration — the shared card shell declared
    `content: @Composable Column.() -> Unit`, but Column is a COMPOSABLE
    FUNCTION, not a type; the receiver scope type is ColumnScope
    (androidx.compose.foundation.layout.ColumnScope). LESSON: `X.() ->
    Unit` receivers need a real TYPE (ColumnScope / RowScope / BoxScope),
    never the layout function name.
- **v175 — backup/restore now carries the profile avatar FILE (was
  path-only).** User: "the profile pic wasnt restoring on backup restore
  so fix it". Root cause: `AppPreferences.getProfileAvatarPath` stores an
  absolute `filesDir/profile_avatar_<ts>.png` path in `curio_app_prefs`;
  backup stored the prefs (path) but never the PNG, so a restored app
  pointed at a nonexistent file → blank avatar. Fix in CurioBackupManager:
  FORMAT_VERSION 6 → 7; export streams a new `avatarFile` section (base64
  of the PNG, null when unset); restore reads it (NULL-safe), writes a
  FRESH `profile_avatar_<ts>.png`, sweeps old avatar files, and overrides
  `setProfileAvatarPath` AFTER the prefs loop (the restored prefs carry
  the source device's dead path). v6 backups (no avatarFile) still
  restore — reader's else-skip covers it. BackupPayload got a matching
  `avatarFile: ByteArray?` field (legacy class, consistency only).
  LESSON: never back up only a filesDir path — the file must ride along;
  when restoring paths into prefs, re-home them to fresh files.
- **v174d — the drawer map's "This Week ˅" selector is now LIVE and
  filters the stats constellation.** User: "Make the stats constellation
  filter to a time range via the 'This Week' selector on the drawer
  map". New `features/stats/StatsRange.kt`: `StatsRange` enum (WEEK 7 /
  MONTH 30 / ALL null days) + `StatsRangeState` singleton (mutableStateOf,
  private set — same pattern as CurioNavTint) + the shared
  `StatsRangeSelectorPill` (DropdownMenu with bold current choice). The
  decorative drawer pill is replaced by it (chevron hint kept); the stats
  constellation card header gets the same pill. StatsScreen now loads ALL
  entries once, filters via `filterForRange` (capturedAtMillis >=
  cutoff), and derives laneCounts/laneRecent/explored from the WINDOW
  (explored ∪ quest categoriesState only for All Time — quest history has
  no timestamps). Subtitle reflects the window
  ("saved this week/month/all time"). In-memory state (not persisted).
  LESSON: only entry-based stats are time-bucketed; lifetime counters and
  quest history can't be filtered — union quest lanes only on All Time.
- **v174e — drawer map rework: no fake data, real lane constellation +
  sky-tear banner + transparent footer.** User: "dont use fake data, use
  empty state, extend the drawer banner's sky design and its color to the
  tear start so it looks like a sky tear... remove the learned/explored/
  topics/questions/saved/shared/streak stats, only use what exists... dots
  with rounded icons connected, tapping shows the data... replace the
  footer svg with this one (no background), place it more below and less
  opaque so 'Made with curiosity' is visible". ask_user: (1) drop the
  stat list entirely; (2) icon-dots in the DRAWER map only (stats page
  unchanged); (3) the sky tear is the drawer banner — the stats page stays
  as-is, just light-mode tuned.
  - DRAWER MAP (`DrawerCuriosityMap` in HomeScreen.kt): the 7 orbiting
    `MapStat`s (Learned=128 / Explored / Topics / Questions=98 / Saved /
    Shared=72 / Streak — the fake design numbers) and the "Overall
    Curiosity N" center overlay are GONE. The card now loads real entries
    (`CurioRepositoryHolder.repo.getAll()` in a LaunchedEffect), filters
    them via the SHARED `filterForRange` (moved from StatsScreen.kt to
    StatsRange.kt — same package, no import churn), and shows a
    `DrawerLaneConstellation`: one 34dp rounded lane-icon chip per
    EXPLORED lane (deterministic two-lobe arc seeded by name hash, thin
    connecting lines, accent border; selected = solid accent fill +
    `onAccent()` icon + 3dp lift). Tapping a dot toggles an inline panel
    (lane name + "N saved · active this week" + dismiss chip); the
    helper copy "A little galaxy of everything you've explored." only
    shows in the EMPTY state (no explored lanes → icon + "Spin a deck
    and explore to light up your map."). Deleted `MapStat` +
    `ConstellationBrain` (dead). Whole card still opens the stats page on
    non-dot taps (nested clickables — the inner dot consumes its tap).
  - SKY TEAR (drawer banner): `DrawerRollingHorizon` (the cream hills at
    the banner's bottom) is DELETED so the sky gradient + stars run all
    the way down to the torn seam — the banner reads as a torn piece of
    sky. LESSON: the torn hero's bottom band belongs to the hero's own
    art; an overlay horizon between the art and the seam breaks the
    "tear" illusion.
  - LIGHT-MODE TUNE: the celestial bits (`DrawerCelestialSky` +
    `StatsSkyHeader`) hardcoded warm-white `starTint` — invisible on the
    pale seafoam sky. Both now resolve `starTint`
    theme-aware (light = deep seafoam ink `#2C5A53`, dark = warm white
    `#FFFDF4`); the stats back-pill keeps an explicit warm-white fill so
    the dark arrow still reads.
  - FOOTER (`DrawerFooter`): `res/raw/drawer_footer.svg` replaced with
    the user's NEW svg — it has NO `#FCF3E8` background rect (the old one
    had a cream backdrop panel). The AsyncImage is now
    `Alignment.BottomCenter` + `alpha(0.55f)` so the art sits lower and
    the "v1.1.0 · Made with curiosity ♥" line reads over it.
  - LESSON: an SVG's first `<path d="M0 0h1536v1024H0z" fill=...>` is a
    full-canvas background rect — a "no background" version simply lacks
    it; `remember {}` is `@DisallowComposableCalls` so resolve
    `themedAccent()` maps OUTSIDE remember (the associateWith pattern the
    stats page uses).
- **v174f — APK slimming: ship only `topics/*.json` (index built at
  runtime) + icon font subset (11MB → ~0.3MB).** User: "ship only topics
  json, trim the icon font but keep the full font as a backup outside of
  the app for future additions" — after the app hit ~55MB: `assets/
  topic_index.json` (23MB) was a FULL duplicate of every topic in
  `assets/topics/*.json` (16MB), and the bundled Material Symbols font
  (11MB, all 6,566 glyphs) dwarfed everything else.
  - TOPICS: `app/src/main/assets/topic_index.json` DELETED. v29's
    build-time merged index (search keys + sort year precomputed by
    `scripts/build_topic_index.py`) was only a faster-loading mirror of
    the per-category files — it never added data, just 23MB of APK.
    `TopicJsonLoader.loadIndex()` now checks for the asset and, when
    absent, builds the SAME merged index at runtime via the new
    `buildIndexFromCatalog()`: iterates every lane through the shared
    `load(id)` (parses are shared with the per-category caches — never
    double-parsed), computes keys (lowercased) + year
    (`CurioTopic.publicationYear()`, identical precedence to the Python
    script), dedupes by id (verified: zero cross-file dupes today), and
    reads wildcard.json directly (`load(WILDCARD)` would merge every
    lane). One parse, cached, prewarmed at app start — the Topic Database
    still renders with zero loading. The prebuilt-asset path stays as a
    fallback if someone re-runs the script.
  - ICON FONT: `res/font/material_symbols_outlined.ttf` subset from
    11MB → 297KB with fonttools (`--no-layout-closure` is the key flag —
    the default ligature closure retains ALL 4,250 icon ligatures because
    every name shares the a–z/underscore/digit source glyphs). The 201
    used glyphs were found by decoding the font's ligature table (note:
    components are per-letter with `_` → "underscore" and digits →
    "digit_one/two/…" glyphs) and cross-referencing every snake_case
    string literal in the app. ALL variable axes kept (wght powers the
    Normal/Bold `FontFamily` entries). FULL font backed up at
    `tools/fonts/material_symbols_outlined_full.ttf` — when adding an
    icon, subset again from there (command in the CurioIcons.kt header
    doc). LESSON: check the font's actual ligature encoding before
    trusting icon names — `format_underline` doesn't exist in this font
    (only `format_underlined`; the const is unused) while `inventory_2`
    IS a ligature (digit spelled out).
  - Also: the category-picker "Manage categories" pill (v168 capsule)
    read as overlapping the grid's last row — the sliced row + 6dp
    shadow formed a band behind the button. Fixed: grid bottom
    contentPadding 4→20dp, gap 8→16dp, pill shadow 6→3dp.
- **v175 — drawer hero sky is the user's SVG artwork; v174g fully
  reverted.** User: "i uploaded a new svg for the night sky use that and
  also remove the watermarks from the backgroud drawer hero" + "fully
  revert <6300f774>... also for light mode use another svg i just
  uploaded."
  - REVERT: `6300f774` (constant galaxy / flat opaque footer / opaque
    buttons / lifetime totals + badges) and its CI follow-up `91b4375`
    (RowScope fix for the now-deleted `DrawerLifetimePane`) reverted
    cleanly via `git revert --no-commit` — the drawer returns to the
    v174e/v174f sky-tear design: boxed real-data curiosity map, the
    transparent alpha-0.55 footer, the pre-existing nav rows. The
    changelog + AGENTS.md additions from that commit went with it.
  - HERO SKY: the procedural `DrawerCelestialSky` (stars / grain /
    constellation / sparkles / crescent moon) and the mirrored watermark
    glyph collage (`heroSymbols`/`heroPairs`) are GONE from the drawer
    hero. The banner now loads the user's uploaded SVG artwork via Coil's
    `SvgDecoder` (same path as the footer): dark theme →
    `res/raw/drawer_hero_sky_dark.svg` (the uploaded night sky
    `svgviewer-output (11).svg`, dark palette with the moon-edge sparkle
    removed), light theme → `res/raw/drawer_hero_sky_light.svg` (the
    uploaded `curio_day_sky_fixed(1).svg` day sky — sun at the moon's
    position 0.84/0.28, clouds, birds). The theme gradient stays as the
    loading backdrop; greeting + avatar read as the sky's scenery.
  - DEAD CODE: `DrawerCelestialSky`, `SkyStar`/`SkyLink`/`SkySparkle`,
    `drawSparkle` and the `Path`/`DrawScope` imports deleted (    only the
    hero used them). LESSON: when a commit is reverted, its CI-only
    follow-up fixes must go too or the revert conflicts on the code they
    patched.
- **v176 — drawer curiosity map = plain-surface constellation of ALL
  lanes + new flat bottom footer.** User: "remove the box behind your
  curiocity, and show the mind connection constellation map with inactive
  glow stars... show more available data... not the hero banner or just a
  drawing. so ask me properly" (clarified: remove box + text + dropdown
  entirely, all-time data in drawer; ALL lanes as stars, explored glow
  + icon on tap, inactive solid but smaller + muted, extra tiny stars;
  hero unchanged) and "i also added a new cropped footer place this and
  place it much below from the end of the footer, dont give it shadow...
  no scaffolding" (clarified: very bottom end, keep credits, add a
  bottom fade so it doesn't look floating).
  - MAP (`DrawerCuriosityMap`): the boxed card, the "Your Curiosity Map"
    title and the "This Week ˅" range selector are GONE. The map is now
    a `Column` on the plain drawer surface (whole-map tap still opens
    Stats). All-time data comes straight from `CurioPassport.allProgress`
    (spins/reveals/explores/saves/lastAt per visible lane — no repo,
    no `CurioQuests`, no `StatsRange` import). `DrawerLaneConstellation`
    renders EVERY visible lane: explored lanes are solid accent chips
    (34dp, `themedAccent` fill, `onAccent` icon, glow, tappable) and
    inactive lanes are solid 14dp dots (muted blue-grey, NOT alpha —
    differentiated by size + color per the user), plus 16 seeded extra
    tiny stars and faint neighbour-connecting lines. Deterministic grid
    scatter + per-lane `Random(id.name.hashCode())` jitter so all lanes
    fit and the constellation reads as a whole. Tapping an explored star
    opens a richer panel (`DrawerMapStat` panes: spins / peeked /
    explores / saved) with the lane icon, accent-tinted surface, last-
    explored relative time and a close button.
  - FOOTER (`DrawerFooter`): `res/raw/drawer_footer.svg` replaced with
    the user's cropped `curio_planet_cropped_bottom_264.svg` (1536×760,
    same planet art bottom-cropped). The footer is now a flat 210dp Box
    at the very bottom: `ContentScale.Crop` bottom-anchored art, no
    shadow/box/scaffolding; a 110dp vertical gradient fades the art into
    the drawer surface so it doesn't look floating, and the version    + "Made with curiosity" row sits inside that fade (nav-bars-padded,
    warm tan ink). The LazyColumn's `contentPadding.bottom` dropped
    20dp → 0 so the art truly touches the drawer's bottom edge.
- **v177 — tap the moon/sun on the drawer hero to flip the theme.**
  User: "make it so when i tap the moon or the sun the theme switch
  between light and dark". Always-on per ask_user (no Settings toggle).
  Both celestial bodies sit at (268.8, 52.08) in their SVGs and the
  artwork is exactly 320×186dp (1:1 with the hero box, `ContentScale.Crop`
  is a no-op), so a 48dp INVISIBLE hit-circle (`Box` with offset +
  `CircleShape` clip + `clickable`) sits right on top of the moon/sun in
  the hero's sky Box. Tapping it calls
  `AppPreferences.setThemeMode(context, if (isCurioDarkTheme())
  THEME_LIGHT else THEME_DARK)` — toggles to the OPPOSITE of the current
  effective theme (so System mode resolves first, then flips to a forced
  light/dark). `themeModeState` is a `mutableStateOf`, so the whole app
  rethemes instantly and the hero SVG crossfades via the existing
  `crossfade(true)`.
- **v178 — constellation audit (light-mode visibility + grid web) and the
  Stats "Your Curiosity" header banner now matches the drawer hero.**
  User: "do a proper audit that the constelations lines and star colors
  are right and visible in light mode and each stars in connected somehow
  and now use the day one view in drawer hero in your curiocity hero too,
  dont chnage the design just the banner style of the header". Stats
  header sky = theme-picked like the drawer per ask_user.
  - AUDIT (`DrawerLaneConstellation`): the old `#7FAFD8 @ 0.20` lines and
    `@ 0.30` tiny stars VANISHED on the white drawer surface in light
    mode, and the `#AFC9D4` idle dots were near-invisible. Inks are now
    resolved in COMPOSITION (they call `isCurioDarkTheme`, so they can't
    live inside the Canvas draw lambda): `linkColor` = #7FAFD8@0.30 dark
    / #5F7E9A@0.55 light, `tinyStarColor` = #7FAFD8@0.35 / #5F7E9A@0.50,
    `idleDotColor` = #4A5F6E / #7E9CB0 (steel slate, reads on white).
    Explored chips were already right (solid `themedAccent` + `onAccent`).
  - CONNECTIVITY: the old closed zigzag chain (i → (i+1)%n) was replaced
    with a GRID WEB — each node links to its RIGHT (col < c-1) and DOWN
    (row < r-1) grid neighbours (c/r recomputed in-canvas with the same
    formula as the layout). Every star gets 2–4 visible links; the
    constellation reads as a connected mesh instead of one invisible
    chain whose links hid under the 34dp explored chips.
  - STATS HEADER (`StatsSkyHeader` in StatsScreen.kt): the procedural
    gradient + 22 seeded stars + carved crescent moon are GONE. The band
    now loads the SAME theme-picked SVG as the drawer hero
    (`R.raw.drawer_hero_sky_dark` / `drawer_hero_sky_light`) via Coil
    `SvgDecoder` with `ContentScale.Crop`, over the theme gradient as the
    loading backdrop. The design is untouched: rounded 30dp bottom tear,
    the warm-white back pill (skyInk glyph) and the "Your Curiosity" /
    "Stats, streaks & insights" title still read on the art. The sky SVGs
    are 320×186 (1dp/unit) so the band's 148dp height is a center-crop
    slice — the celestial body sits near the top edge. Added imports:
    `ContentScale`, `AsyncImage`, `SvgDecoder`, `ImageRequest`, `R`;
    `Canvas`/`Offset`/`Random`/`lerp` stay (CategoryConstellation still
    draws).
- **v179 — Pet Designer theme-aware studio pill + opaque edit prompt +
  full-bleed hero.** User: "make the pet desinger gets the theme aware as
  well. and make the what do you want to edit box non trasparent. and fix
  its header tear hero too as its cut from sides." Clarified via
  ask_user: "the floating nav pill doesnt and also fix its hero banner
  side cut" — the theme issue is the STUDIO pill bar, not the page (the
  page is already MaterialTheme-driven).
  - STUDIO PILL (`PetStudioTab`): the ACTIVE tab still wore the stale
    solid `secondary` + `onSecondary` (the butter the main nav bar
    dropped in v161 for the accent system). It now uses the nav bar's
    plain-page language: `secondaryContainer` + `onSecondaryContainer`
    (both light/dark aware). The v147b doc comment updated to match.
  - EDIT PROMPT (`EditorPickPrompt`): the "What do you want to edit?"
    card's fill `lerp(surface, primaryContainer, 0.55f)` read as a
    TRANSLUCENT plate over the page's lane wash; replaced with the solid
    `surfaceContainerHigh` (same elevated container as DialogScrim) so
    it's clearly opaque. `lerp` still used elsewhere (2321, 4559).
  - HERO SIDE CUT: `SettingsHeroHeader` is the FIRST LazyColumn item and
    the list's `contentPadding(start/end = wideContentEdgePadding())`
    inset it 16dp+ each side — the tear looked cut. Fix: compute
    `val edgePad = wideContentEdgePadding()` once, use it in
    contentPadding, and wrap the hero item in
    `Box(Modifier.fillMaxWidth().padding(horizontal = -edgePad))` so the
    banner bleeds to BOTH screen edges (content below stays padded).
    Works on wide screens too (larger edgePad cancels fully).
- **v180 — Spin category picker: no footer below the cards, floating
  no-background Mix/Cancel, hero watermark placement fixed.** User: "in
  spin screen category picker remove anything that below the category
  cards like remove it fully. no need for manage category and make the
  mix and cancel button appear as nav bar style pill only when selected
  multiple. with no backgroud and floating style. and also the hero
  banner of it chnage its glyph style and placement so none of the icon
  is visible and makes its backgroud tint take theme aware too and also
  watermark." Clarified via ask_user: Mix/Cancel pill = NO background
  (just the buttons floating); "icons not visible" = PLACEMENT bug, fix
  the placements (the hero fill/wash/ink were already theme-aware).
  - FOOTER REMOVED: the "Manage categories" floating pill is GONE —
    nothing sits below the category cards in single-select.
    `onBrowseAll` param removed from `CategoryPickerSheet` + the call
    site (the Manage-Categories navigation went with it); the
    `curioFloatingNavContainerFor` + `ButtonDefaults` imports are now
    unused and deleted.
  - MIX/CANCEL: in multi-select the controls are a FLOATING row
    (`align(BottomCenter)` in the grid's weight Box, bottom 18dp) with
    NO background capsule — Mix is a solid category pill
    (`themedButtonFill`/`themedButtonInk`, theme-aware) and Cancel is a
    plain text button. The grids' bottom contentPadding is now
    `if (multiSelectMode) 88dp else 20dp` so the floating controls never
    cover the last row of cards. Button → Surface swap (return@Surface).
  - HERO GLYPHS: the small twin sat UNDER the status bar (its
    `align(TopStart)` had no `statusBarsPadding`, unlike the title
    column) and the 72dp large one was clipped by the tear + hidden
    behind the tabs/presets rows. Fixed: small gets `.statusBarsPadding()`
    (top-left corner), large is 64dp raised to `bottom = 58dp` (right
    edge just above the preset chips — the tabs row is left-aligned so
    that corner is free), alphas bumped 0.07/0.10 → 0.10/0.14. NOTE: the
    FilterSheet hero (line ~1814) has the SAME placement bug — left
    unfixed (user asked about the picker only).
- **v181 — CI compile fix (moon/sun tap) + Stats constellation web.**
  User pasted the CI failure: `HomeScreen.kt:2132:41 @Composable
  invocations can only happen from the context of a @Composable function`
  — "fix this too", plus "Connect the stars" for the Stats page.
  - CI FIX: the v177 moon/sun tap called `isCurioDarkTheme()` INSIDE the
    `clickable {}` lambda (non-composable context) — a direct violation
    of COMPILE-SAFETY rule 3 ("onClick lambdas are NOT @Composable
    contexts"). Fixed by resolving `val isDarkNow = isCurioDarkTheme()`
    in composition before the clickable. LESSON: it shipped because CI
    runs async — the local "verification" (grep for references) didn't
    catch a @Composable call inside a lambda; ALWAYS grep for composable
    helpers (isCurioDarkTheme/themedAccent/…) with `-B2 -A2` context and
    eyeball every occurrence that lands inside a lambda. A full scan of
    the four files touched this session found no other violations.
  - STATS WEB (`CategoryConstellation` in StatsScreen.kt): the old
    lane-order chain + one gold fissure used `#7FAFD8 @ 0.22` — the same
    light-mode invisibility the drawer map had. The stars are now linked
    as a NEAREST-NEIGHBOUR web (each star → its 2 closest stars,
    deduped via `LinkedHashSet<Pair<Int,Int>>` — kotlin.collections
    typealias, auto-imported) with theme-aware inks resolved in
    composition: `linkColor` #7FAFD8@0.32 dark / #5F7E9A@0.50 light,
    `fissureColor` #D9A85C@0.30 dark / #A97F3C@0.45 light (the gold
    fissure still bridges the two hemispheres).
- **v199 — topic name resolution: exact/base matches beat containment
  across lanes, and the reveal/capture resolve within the route's own
  category first ("Flow" no longer opens "Flower Boy"); the Browse-Topics
  category + chip-bar state persists until app restart. (branch Alpha)**
  User: "when i tap flow in topic browser movie 2024 in fimls why its
  opening flower boy, fix more similiar issues like this and also make the
  category selected and expaddned setting persistent untill restart".
  - FLOW → FLOWER BOY (root cause): `TopicCatalog.findByName` scanned
    `CategoryId.values()` in order and returned the FIRST lane's first
    tolerant match; ALBUMS scans before FILMS and "Flower Boy" contains
    "flow" (and even the full "Flow (2024)" base-collided the same way),
    so the Films reveal opened the album. Fixes:
    - `TopicCatalog.findByName` is now TWO passes — strict matches
      (exact name / base-name equality, new `matchesSavedNameStrict` +
      shared `savedNameBase` helper) across ALL categories, THEN the
      tolerant pass (containment still last). An exact/base hit in any
      lane always beats a loose containment hit in an earlier lane.
    - `TopicRevealScreen` + `SaveCaptureScreen` now resolve within the
      route's own category pool FIRST, TIERED like `findByName` — strict
      (`pool.firstOrNull { it.matchesSavedNameStrict(name) }`) before
      tolerant (`matchesSavedName`), so a loose containment match earlier
      in the same file can't beat a precise one later in the lane either —
      falling back to the global `findByName` only for legacy saved
      entries whose lane changed (v135).
  - BROWSE-TOPICS PERSISTENCE: the browser is a plain `composable`, so
    every reopen from the drawer creates a fresh backstack entry and
    rememberSaveable reset the category selection + chip bar. New
    `TopicBrowserSession` (process-scoped static, same pattern as
    `SpinPickerRequest`) seeds `selectedCat` / `categoryFilterOpen` and
    syncs back on change — the state now survives close-and-reopen until
    the app restarts (statics die with the process).
- **v200 — NEW CATEGORY: Animated Movies (ANIMATED_MOVIES) — a 1000+
  lane: non-anime animation split out of Films; real films, real quick
  facts. (branch Alpha)** User: "continue the expansion of topics and add
  animated movies section as a ne category and separate animated movies
  from films and make them 1000+ and anduse real quick facts and push
  after its fully done" (+ "anime and animation movies are differnt btw").
  - ANIME ≠ ANIMATED MOVIES (user note): the 6 anime films in films.json
    (Akira, Grave of the Fireflies, Totoro, Princess Mononoke, Spirited
    Away, The Boy and the Heron) STAY in Films — the new lane is
    non-anime animation only (Disney, Pixar, DreamWorks, Illumination,
    Blue Sky, Sony, Aardman, Laika, Don Bluth, classic US, Rankin/Bass,
    European, Chinese, Latin American, Indian, Australian, stop-motion,
    DTV/franchise: Barbie, Scooby-Doo, Tom & Jerry, DC/Marvel animated,
    DisneyToon sequels).
  - CATEGORY REGISTRATION: `Category.kt` (enum + newLanes + order +
    slug `animated-movies` + family Entertainment), `CurioColors.kt`
    (ANIMATED_MOVIES palette constants), the three exhaustive `when`s
    (CaptureEntity.kt, ExploreSession.kt, TopicRevealScreen.kt) and the
    Entertainment quick-mix preset (DeckPresets.kt). The topic_index
    builder globs all files.
  - VALIDATOR FIX (v200.1): the Gradle `validateTopics` derived the
    expected categoryId as the bare uppercased FILENAME, so the first
    hyphenated slug tripped CI — `animated-movies.json` expected
    `ANIMATED-MOVIES` but entries carry the enum name `ANIMATED_MOVIES`.
    The derivation now maps hyphens → underscores
    (`uppercase().replace("-", "_")`) — single-word filenames
    (films.json → FILMS) are unaffected. The dev-time
    `scripts/validate_topics.js` got the same hyphen→underscore mapping
    and `animated-movies` added to its EXPECTED_CATEGORIES list. (All
    prior files were single-word, so this never surfaced before.)
  - CONTENT: 52 non-anime animated films moved out of films.json (948
    remaining, anime intact) into the new animated-movies.json via
    `scripts/extract_animated_from_films.py` (explicit-title list — the
    first tag-based attempt false-positived on live-action "Pixar"-
    tagged films like Braveheart and was reverted), then ~540 more real
    entries authored across scripts/batch_animated_1..11.py (Disney
    theatrical + DTV, Pixar, DreamWorks, Illumination, Blue Sky, Sony,
    Aardman, Laika, stop-motion indie, Don Bluth + 80s/90s classics,
    international, Chinese, franchise DTV). 591 entries total in this
    push (1000+ top-up continues in a later pass). All ids unique across
    the catalog (18,071 total) — validated with check_assets.py + a
    cross-file id scan.
  - HOUSEKEEPING: removed the root-level reference dump SVGs
    (`svgviewer-output (12).svg`, `curio_planet_cropped_bottom_264.svg`,
    `footer.svg`) — the real drawer art lives in res/raw/.
- **v210 — CONSTELLATION REDESIGN: replaced the brain neural mesh with
  the real Corvus (The Crow) constellation — Apollo placed the crow in
  the sky because its curiosity led it to seek forbidden knowledge, the
  perfect emblem for Curio. Four anchor stars (Gienah, Kraz, Algorab,
  Minkar) form the characteristic quadrilateral; explored lane stars are
  scattered around the pattern. SPACE AESTHETIC: deep void background
  with faint nebula wash, gossamer constellation lines (thin, dim),
  small bright stars with soft halos — no brain silhouette, no dense
  mesh, no garish glows. Background stars add depth. Removed: brain
  silhouette outline, filler dots, nearest-neighbour neural web,
  corpus-callosum bridges, gold fissure.
- **v208 — CURIO BRAIN STATS: a real science-based cognitive model
  replaces the constellation's save-count stars (user: "a fresh system of
  my on, i want a real science based stats system that will help user
  improve their brain in a certain ways and it shows the knowledge based
  on the category user explored and the amout of wrting user does etc
  etc, and this will be rplaced the category stars glow from
  costelation").** New `data/BrainStats.kt`:
  - PER-LANE KNOWLEDGE (`LaneKnowledge` + `laneKnowledge`): how much
    knowledge you BUILT in each lane — explores + saves + words written
    in that lane (`score = explores*30 + saves*40 + words/20 + spins*2`),
    with `lastAt` driving the recent glow. Both the drawer and the Your
    Curiosity page feed the SHARED `CurioConstellation` from this
    (`laneCounts = knowledge scores`, `laneRecent = lastAt`) so they can
    never drift; the drawer's floating popover now shows "N knowledge"
    instead of "N saved".
  - THE BRAIN PROFILE (`BrainProfile` + `BrainDimension`): six cognitive
    dimensions, each mapped to a real learning-science mechanism —
    Knowledge (breadth×depth of explored domains), Memory (saves+pins+
    quotes, retrieval-practice effect), Expression (words written,
    generation effect), Focus (explores + daily quests), Consistency
    (best streak, spacing effect), Curiosity (spins + lanes sampled).
    Each scored 0–100 from REAL data (passport counters, saved captures,
    lifetime counters, streak) with a level label (Awakening → Mastered)
    and a science-based improvement tip.
  - WORD COUNT: `CaptureData.wordCount()` — real words across every text
    field (journal, review, notes, field notes, captions, quotes),
    recursive through portfolios and the wildcard notebook; voice
    transcripts NOT counted (machine-transcribed, not user writing).
  - UI: a new `BrainProfileCard` on the Your Curiosity page (six
    color-coded dimension bars + levels + tips + a words-written line);
    constellation star SIZE is now sqrt-scaled knowledge (the old linear
    `min(count,60)` pinned everything at max once scores ran past 60).
  - CI FIX (v208b): `CaptureData`'s subclasses are NESTED — the
    `wordCount()` `when` branches needed `CaptureData.` qualification
    (`is CaptureData.SoundBite` etc.); the splash wordmark's gradient
    `brush` moved from the `Text(...)` param (doesn't exist) into
    `style.copy(brush = …)`.
  - v208f — four follow-ups (user: "see the svg its inverted of what its
    in the app youre plaing it wrngly fix it. and why th elike and dislike
    pill now staying longer make it vanish like before just when i tap
    back from the reveal screen… make the costeellation dots smaller they
    are too big give it a size limit… why my drawer footer is floating??
    please fix it and the collapsed options is sghowing behind the
    drawer, fix it, and also cut the footer from button 44 units the
    footer svg cut it from buttom and place it properly"):
    - RING MIRROR: `CoilOutlineNorm`/`CoilSpecularNorm` in PaperStatCard
      now MATCH the SVG's own `matrix(-1,0,0,1,0,0)` (the app rendered
      the coil inverted vs the author's art): wire starts bottom-RIGHT
      inside the hole, arches over the top, and the LEFT leg dives below
      the box past the card's left edge to an open round-capped end.
    - SENTIMENT PILL VANISH: the NavHost's SentimentPillHost overlay is
      now gated on `isRevealRoutePrefix` — the pill disappears the
      moment you tap back (route flips before the screen's exit
      transition ends; the old gate waited for full dispose and the pill
      lingered).
    - CONSTELLATION DOTS: radius ramp retuned `5.5+sqrt×7` capped 60 →
      `4.5+sqrt×2.4` capped 12dp (24dp across max) — the dots were
      ballooning to ~120dp; now score 0→4.5dp … 10+→12dp.
    - DRAWER FOOTER: (a) the footer Box now wears `navigationBarsPadding`
      (the sheet is edge-to-edge with zeroed insets, so the footer's
      bottom band hid behind the gesture bar and the planet read as
      floating — now the art sits flush above the gesture bar; the
      credits' own navBarPadding removed); (b) `drawer_footer.svg`
      viewBox cut 44 units off the bottom (760 → 716) per the user's
      explicit "cut the footer svg from the bottom 44 units". The v207
      Column/weight structure (footer in normal flow below the list) is
      confirmed intact — expanded sections scroll above the footer, never
      behind it.
  - NAV→SENTIMENT HANDOFF (v208e): user (v208d attempt rejected): "no
    bro the like and dislike starting time was fine i just asked you to
    tune the navpil home one to sync properly… place the like and dislike
    pill z index above the home nav pill… keep it overlap". So the pill
    KEEPS its natural entrance; the NAV pill syncs TO it:
    - `FloatingNavCollapseHoldMillis` retuned 460 → 240ms (the pill's
      220ms slide + a hair) — the bar vanishes right as the pill lands.
    - Z-INDEX: the pill is portaled into the NavHost's own overlay via
      the new `SentimentPillHost` (CurioRoutes.kt, out-of-band like
      LightboxTarget): the reveal registers its pill composable in a
      `SideEffect` (+ `DisposableEffect` clears it on route leave) and
      the NavHost composes the slot AFTER the floating bar — so the
      Like/Dislike draws ON TOP of the collapsing nav pill during the
      overlap. The slot's wrapper Box has no pointer input (touches pass
      through); the pill's scroll hide/show is captured by the lambda.
  - RING SVG v3 (v208c): user supplied `svgviewer-output (15).svg` —
    `CoilOutlineNorm` now matches it exactly: the wire starts at the
    box's bottom-LEFT corner (the v207 left hook is GONE), rises up,
    arches over, and the RIGHT leg DIVES below the box through the hole
    to an open round-capped end at (0.712, 1.342) — replacing the old
    blunt stop at (1.0, 0.737). Verified numerically: start protrudes
    6.6dp past the card's left edge, the dive threads the hole, and the
    open end stays inside the card. Colors untouched (user will tweak
    later). The dump SVG was removed from the repo.
- **v207 — drawer footer in normal flow (no float / no rows behind it),
  coil left-end hook, standalone sun/moon on the stats page, even
  smoother nav collapse, Like/Dislike exact capsule height. (branch
  Alpha, NOT pushed — the v206 splash commit is queued unpushed per
  user: "dont push it")** User: "why the footer is floating now and when
  the about gets expanded its behind the footer. also the 3d hole is
  good. now mak the left end the side its out curve a little so it looks
  seemless connected also in your curiocity page place the drawing of
  sun and moon a little below the start bar just the moon and the sun
  not the whole drawing. and only in your curiocity page as a separate
  maybe. and the collapse of home nav pil can be more smoother, and the
  like and dislike size still doesnt match with home nav pill, like its
  height".
  - FOOTER: v203 pinned the footer as an OVERLAY over the list tail, so
    expanded sections (About) slid UNDER its fade ("behind the footer")
    and it read as floating over the empty reserve. HomeDrawerContent
    now wraps the rows in a Column — the list sits in a weight(1f) Box
    ABOVE the footer, which is in normal flow pinned to the sheet's
    bottom: it can never float and rows never hide behind it.
  - COIL: the protruding LEFT end of the hole-ring coil now curves
    (a small hook dipping down-outward before rising into the arch —
    new moveTo/cubic prepended to CoilOutlineNorm) so the wire reads as
    wrapping around the card edge instead of ending blunt.
  - STATS SUN/MOON: a standalone decorative `StatsCelestialBody` on the
    Your Curiosity page only — just the celestial body (gold sun with
    soft glow in light; cream crescent carved by the local sky mid-tone
    in dark, mirroring the SVG construction), floating just below the
    status bar (TopEnd + statusBarsPadding + 14dp). Not interactive
    (the drawer's body stays the theme toggle).
  - NAV COLLAPSE: pill family 150 → 120 stiffness (calmest glide yet);
    the NavHost leave-hold extends 420 → 460ms to match. Reveal
    sentiment + Pet Studio springs follow to 120 to stay in lockstep.
  - LIKE/DISLIKE HEIGHT: the sentiment capsule's Row padding 7 → 8dp and
    gap 6 → 10dp (mirroring the nav bar), so the whole capsule (52dp
    segments + 16dp = 68dp) matches the nav bar's capsule exactly.
- **v206 — splash redesign: bigger gradient wordmark, warm tagline, warm
  ground band at the bottom. (branch Alpha, NOT pushed — user: "dont
  push it")** User: "make the Curio tet bigger and with gradient basced
  on the dark or light mode. and at the buttom it have a similiar
  gradient backgroud of the app backgroud. not full just at the buttom
  and also discover something that text gets a little bigger too and
  warmer in dark mode light mode you figure it out."
  - WORDMARK: "Curio" 36 → 72sp (same Geom Bold displaySmall family),
    now painted with a theme-aware horizontal GRADIENT echoing the
    cosmic mark — dark: bright SkyMint → ButterYellow on the dark sky;
    light: deep CoralInk → GoldInk on the cream (readable deep tones).
  - BOTTOM GROUND: a bottom-anchored band (bottom 34% of the screen)
    fading transparent → a warmed app-background tone (dark: background
    lerped 5% toward CoralBlush; light: 14% toward ButterYellow), so the
    splash reads grounded instead of a flat void. Not full-bleed.
  - TAGLINE: 14 → 18sp and WARMER in both themes — parchment
    #D8CDB4 on dark, warm khaki #7E6E50 on light (replaces the cool
    onBackground @ 0.62).
  - Everything else (logomark + shimmer, animated halo, 3-dot loader)
    unchanged. Brace/paren-balanced.
- **v205 — app-size diet: the 40MB was Vosk, not the topics. (branch
  Alpha)** User: "the app size is still 40mb and why. dont tell me its
  the topic ik its alot but still not alot to make it 40mb" (+ "its not
  the release the pr builds im talking about" — the PR/push CI artifact).
  - DIAGNOSIS: Vosk (offline ASR, `com.alphacephei:vosk-android:0.3.47`,
    11.7MB AAR) ships a ~10MB `libvosk.so` PER ABI, and Android stores
    `.so` UNCOMPRESSED (mmap). The universal release APK (the PR/push
    CI artifact and the release universal) bundled ALL FOUR ABIs
    (armeabi-v7a, arm64-v8a, x86, x86_64) ≈ 38MB of native libs — that
    is the 40MB. The 17MB of topics compress to ~6MB in the APK; code
    ~8-10MB; fonts/icon ~1MB.
  - FIX: the `release` buildType now sets `ndk.abiFilters =
    [armeabi-v7a, arm64-v8a]` — x86/x86_64 are emulator-only legacy,
    every real device since ~2017 is arm64. Universal release APK drops
    ~20MB (→ ~22-25MB total). DEBUG builds keep all four ABIs so x86_64
    emulator testing still works. The `splits.abi` include list is
    unchanged; the release.yml hard guard now expects only
    `universal armeabi-v7a arm64-v8a`.
  - NOT DONE (deferred): the in-app updater still downloads the first
    `.apk` asset (the universal) — a follow-up could match
    `Build.SUPPORTED_ABIS` to the per-ABI asset (~10-12MB updates).
- **v204 — compile fix (PetStudio `sp` import) + Save CTA tick removed.
  (branch Alpha)** User: CI failure "PetDesignerScreen.kt:1533:39
  Unresolved reference 'sp'" + "fix this too then push everything. and
  also remove the tick from the save your entry button".
  - The v201 PetStudio label change (`fontSize = 15.sp`) was missing the
    `androidx.compose.ui.unit.sp` import — added. (CI caught it because
    the v201..v203 commits were sitting UNPUSHED; this push carries
    v201–v204.)
  - Save CTA (SaveCaptureScreen): the leading check tick
    (`CurioIcons.Check` + Spacer) is gone — the button is text-only
    ("Save entry" / "Save changes"), matching the Manage / Apply pills.
    `CurioIcon` still used elsewhere (12 sites) so its import stays.
  - Brace/paren-balanced; PUSHED with v201–v203 in one go.
- **v203 — Your Curiosity page: back pill REMOVED (system back already
  works); drawer footer PINNED to the bottom + theme-aware credits ink.
  (branch Alpha)** User: "fix the back button in your cusriocity page.
  maybe jut remove the back button" (+ confirmed via ask_user: remove
  entirely), then "the footer is still sitting like floating above the
  buttom part of the draer page. and also the v1.10 made with curiocity
  text sint visible".
  - BACK PILL: the stats page is a plain NavHost destination
    (`composable(CurioRoutes.STATS)`), so the system back gesture/button
    pops it — the custom cream circle pill in [StatsSkyHeader] was a
    redundant second path calling `popBackStack()`. Removed the pill
    (Surface + ArrowBack icon + the `onBack` param + call-site arg); the
    header Row keeps just the title/subtitle column. `CircleShape` stays
    (used by the range pill).
  - FOOTER FLOATING: the footer was the LAST LAZYCOLUMN ITEM, so it
    floated above the drawer's bottom whenever the list content was
    shorter than the sheet. It's now PINNED: removed the `item("footer")`
    from the list and added a `Box(Modifier.align(Alignment.BottomCenter))`
    at the drawer-sheet level (drawn after the list so rows scroll under
    its fade); the list's bottom contentPadding = `DrawerFooterHeight`
    (150dp, the shared constant) so the last row never hides behind it.
  - CREDITS INVISIBLE: the fixed khaki `#7E6E50` vanished on the
    near-black surface in dark mode. `DrawerFooter` ink is now
    theme-aware — warm parchment `#C9BC9D` in dark, khaki in light.
  - COMMITTED BUT NOT PUSHED (user: "dont push this").
- **v202 — curiosity constellation REDRAWN as a human-brain side profile:
  random dot scatter (no left/right partition), light nearest-neighbour
  web. (branch Alpha)** User: "also the mesh is too much and why it doesnt
  look like a brain like the human brain design it should follow that and
  the dots should be random not some in left and some in right".
  - The old design was two side-by-side ellipses (generic blobs) with
    filler dots in rigid per-lobe rings, ~114 links (2-nearest + a
    cross-bridge per dot), and a gold midline fissure.
  - NEW: `BRAIN_SILHOUETTE` — the classic anatomy side profile (frontal
    pole, smooth cerebrum dome, occipital pole, cerebellum bump), drawn
    as a faint outline (`drawBrainOutline`, quadratic curves through the
    midpoints) so the shape reads as a brain instantly.
  - EVERY dot (16 decorative fillers + the real lane neurons) is now
    scattered RANDOMLY inside the silhouette via seeded rejection
    sampling (`randomInBrain` / `pointInBrain`, ~77% acceptance) — the
    per-lobe rings and left/right flag are gone. Real neurons stay
    per-id deterministic (stable as lanes are added), tappable, with
    saved-count sizing + recent glow. Fillers are NOT tappable.
  - The web is now a NEAREST-NEIGHBOUR graph (one synapse per dot):
    13–32 links depending on explored count vs the old ~114 — a ~70%
    cut ("the mesh is too much"). The gold midline fissure is gone with
    the two-lobe layout.
  - Verified: silhouette is x-monotone (no self-intersection), fill
    ratio 77%, link counts simulated (3/8/16/30 explored → 13/16/23/32
    links). Brace-balanced.
- **v201 — nav pill collapse cinches tighter + slower; Like/Dislike and
  Pet Studio bars match the nav pill exactly; hole-ring coil no longer
  cut at the card edge. (branch Alpha)** User: "the 3d ring should be
  shouwn fully without getting cut" + "make the home nav pill collapse
  even smoother like make it collape even more and make the like dislike
  button match the text and size of the nav bar pill and same in pet
  designer".
  - RING CUT — ROOT CAUSE: Material3 1.5's `Surface` ALWAYS clips its
    children to the shape (`.clip(shape)` at the end of the
    implementation) — the v74 "Surface does not clip" note was true
    only for M3 1.0/1.1. The coil's left peek (drawn at −6.5dp) was cut
    at the card edge. Fix: the three stat-pane call sites (Home,
    Profile, EntryDetail) swap the clipping `Surface` for a plain `Box`
    carrying `Modifier.shadow(elevation, shape, clip = false)` + the
    paper fill — the fill self-clips to the outline path, so the coil
    escapes past the left edge. All three sites have ≥28dp container
    padding so the peek clears the screen edge.
  - NAV PILL COLLAPSE: pill spring family 240 → 150 stiffness (longest
    calm critically-damped glide), and the leave-hold collapse now
    targets `FloatingPillCollapsedWidth` (44dp — tighter than the idle
    64dp icon pill) so the pill visibly cinches before the bar unmounts
    ([FloatingNavPill] gains a `collapsing` param; NavHost hold 380 →
    420ms to match the slower settle — still no dead pause).
  - LIKE/DISLIKE + PET STUDIO PILLS: `RevealSentimentPill` and
    `PetStudioTab` bumped to the nav bar's exact sizes (64/136dp +
    52dp height + 26dp icon), springs 400 → 150, and the labels now use
    the nav bar's Changa One 15sp Normal face (was labelMedium Bold).
- **v198 — Home/Recents "Unexplored" tag pills wear a SHADED category
  chip; Material theme: category buttons, filter chips and ink now use
  the family tonal tones — the scheme-role amber/mint/translucent paints
  are gone. (branch Alpha)** User: "in light mode home screen the recents
  unplored pills make it get the color of the category it sits on with a
  shade and in dark mode why it looks transparent fix that, and in
  material theme in light mode and dark mode the category button in spin
  screen and filters looks bad and even worse when mixed is selected the
  category button".
  - TAG PILL (`ExploreTopicRow` in HomeScreen.kt + `RecentTopicRow` in
    RecentScreen.kt): the old `lerp(surfaceContainerLow, accent, 0.14f)`
    fill vanished on the tinted card in light and read transparent in
    dark. The pill now pulls the accent toward the card surface — ~30%
    in light (a solid SHADED category chip on the tinted card) and ~38%
    in dark (visibly tinted on the dark card); pastel light shades with
    the deep same-hue ink (`categoryInk()`) so the airy pastel twin can't
    wash the pill away. Text stays `categoryInk()`.
  - MATERIAL FAMILY TONES EVERYWHERE (`MaterialFamilies.kt`): the v185
    scheme-role branches are GONE. `materialAccent()` wore the scheme
    secondary/tertiary for rose/green lanes (an AMBER button for a rose
    Movies deck — the baseline secondary is an amber companion) and a
    translucent onSurfaceVariant for neutrals, so the Spin deck buttons
    (Categories/Filter), the Spin filter-sheet chips and the
    Cabinet/Topic-History filter chips painted DIFFERENT hues than the
    family-toned cards; a MIXED deck (which collapses to the scheme
    primary) re-mapped through the rose-family branch and the button wore
    secondary while the deck wore primary — the "even worse when mixed"
    case. `materialAccent()` / `materialOnAccent()` / `materialInk()` now
    resolve the lane's OWN family tonal tone (T40/T80 fills, on-fill ink,
    T45/T80 text ink) — the exact fills the cards already use — so
    buttons, chips, filters and text match the deck; pastel mode softens
    the fills to their pastel twins like the cards. `materialAccentFor`
    drops its neutral special-case tones so watermarks/blends align.
  - CI REGRESSION FIX (v198): the v196 tap-to-open rewrite accidentally
    dropped the sheet's `val wide = windowWidthSizeClass().isWide` (it rode
    in the replaced `persistedVisible` block) — the grid's `columns = if
    (wide) …` then failed to compile (CI: "Unresolved reference 'wide'" at
    the two grid sites). Restored in `CategoryPickerSheet` right after
    `val context`.
- **v197 — hole-ring coil redrawn from the user's REVISED SVG (a truncated
  arch, no bottom curl) and it now PEEKS OUT of the card's left edge.
  (branch Alpha)** User: "now i added a better ring this time can u use
  that instead of the previous one, and also the ring should be come out
  from the left of it like peek out from the left not entirely inside the
  stat card" + a revised SVG (same 150×420, three coils — but each coil's
  path is now `M38 62 C38 39 54 24 76 24 C98 24 111 37 111 52`: the
  bottom curl `C111 66 102 75 90 75 …` is GONE, the box is 73×38 instead
  of 73×51, and the dark depth pass uses the SAME truncated path).
  - `ui/components/PaperStatCard.kt` [drawCoilRing]: `CoilOutlineNorm` and
    `CoilSpecularNorm` re-normalized to the revised 73×38 box (outline:
    0,1.0 → 0,0.395 / 0.219,0 / 0.521,0 / 0.822,0 / 1,0.342 / 1,0.737;
    specular unchanged in SVG space but re-normalized: 0.068,0.868 …).
    The wire now rises up the left, over the top and down the right as a
    clean arch (no curl-in at the bottom); `coilH` aspect 51/73 → 38/73.
  - PEEK-OUT: the coil is pushed LEFT past the card edge (`leftPeek`
    ≈ 9dp → its left arc + leg protrude ~6.5dp past the card's left
    edge, like a spiral binding sticking out of the paper) instead of
    sitting entirely inside. The hole stays centered vertically under the
    arch; the wire's right leg dives through it. Works because the
    fill's `drawWithCache` isn't clipped to the card shape (the Surface
    doesn't clip its content here — see the Home v74 note).
- **v196 — category picker: tap-to-open always (hold to mix), cancel +
  back applies the cleared mix, and a cancelled mix no longer resurrects
  after a topic visit. (branch Alpha)** User: "even when i cancel the
  selected in category picker and i tap back make it apply too. and also
  when its mixed and after that i open the category picker to slecet dont
  let me tap to select for mix let it be open the category when i tap and
  only tap and hold should select for next mic or override mix, also theres
  a bug suppos i have a mixed selected and its from the home shuffle button
  and then i cancel it and chnage it to other category and i opened the
  topic and then when i tap back it goes back to the mixed one even though
  i have chnaged it".
  - TAP-TO-OPEN ALWAYS (`CategoryPickerSheet` in SpinScreen.kt): the v26
    auto-tick reopened the sheet in multi-select with every mix lane
    pre-ticked whenever the persisted deck was a mix — the user wanted tap
    to OPEN a category (replacing the deck) and only tap-and-hold to enter
    multi-select. `multiSelectMode` now starts false and `selectedSlugs`
    empty on every open; long-press (both pages) is the ONLY way into
    multi-select, starting a fresh selection for the next / overriding mix.
  - CANCEL + BACK APPLIES: the Cancel button now sets a `mixCancelled`
    flag, and `onDismissRequest` applies a cleared state when cancelled OR
    when every lane was deselected in multi-select — the deck reverts to
    the last single category (`onCategoriesSelected(emptyList())` →
    SpinScreen persists the single) instead of closing with the old mix
    intact. Fresh selections (presets, long-press) reset the flag.
  - NO MIX RESURRECTION (root cause of the back-to-mixed bug): the v5.14
    slug-authority `LaunchedEffect(categorySlug)` and the v5.5 persist
    effect re-ran on every pop-back from a pushed route (the topic reveal)
    and re-forced the launch slug over the user's in-session category
    change. A new `slugApplied` rememberSaveable flag gates both: the slug
    (and its prefs persist) apply ONCE per navigation; returning from the
    reveal restores the flag true, so the deck keeps the user's change.
- **v195 — constellation gets decorative filler neurons (the mesh reads
  whole); nav pill fully collapses on the Topic Reveal and the hold is
  shorter. (branch Alpha)** User: "the neruons dot doesnt create the
  brain mesh. and i told you to add extra dots for decoration and
  completion of the neuron mesh, and the home nav doesnt collapse fully
  in topic reveal screen it stays for too long, neither it collapse".
  - BRAIN MESH FILLERS (`ui/components/CurioConstellation.kt`): with few
    explored lanes the neural web read as scattered dots, not a brain —
    the user explicitly asked for decorative extras. A fixed
    deterministic ring layout per hemisphere lobe (radial 0.35 → 0.89,
    5/7/9 dots per ring, tiny fixed-seed jitter) now fills both lobes;
    the fillers join the SAME link web as the real neurons (nearest-2
    synapses + inter-hemispheric bridges over all dots, with real nodes
    still splitting left/right by index), so the mesh outlines the whole
    brain even at zero explored lanes. Fillers draw as small neutral
    dots UNDER the real neurons (dim steel, no accent / glow / white
    core) and are NOT tappable — the real explored neurons stay the only
    interactive data (popover untouched).
  - NAV COLLAPSE ON REVEAL (`ui/components/CurioBottomNav.kt` +
    `navigation/CurioNavHost.kt`): v193 kept the bar composed for 500ms
    after leaving the tab set so the selected pill could collapse, but
    `CurioFloatingNavBar`'s internal `selectedRoute` mapping forces SPIN
    selected on the reveal route — so leaving Home for a topic reveal
    made the SPIN pill POP OPEN during the hold and the bar then
    vanished with a pill stuck expanded ("neither it collapse"). FIX: a
    new `collapsing` parameter — while the route is off the tab set the
    bar forces NO selection (`selectedRoute = null`), so every pill
    glides closed and the bar unmounts with nothing expanded. The hold
    also dropped 500 → 380ms (the 240-stiffness critically-damped
    collapse spring's settle time) so the bar doesn't linger ("stays
    for too long").
- **v194 — cut lines shorter + right-shifted; hole rings redrawn as the
  spiral-coil SVG.** User: "now we have two cut lines lets improve it even
  more. make it little more shorter and more to the right of the header
  text. and the hole rings… the stamped pin holes create holes which is
  see through, the 3d ring doesnt show over it. lemme share the rings
  which you can adjust and put above the holes… the ring itself isnt
  perfect its too much rounded and the view is also wrong so youve to fix
  the svg rings" + a reference SVG (3 spiral coils).
  - CUT LINES (`ui/components/PaperTitleLines.kt`): the two hand-drawn
    underlines now start ~a quarter in from the title's left edge and
    span only the right ~70% of the line (top 0.22→0.90, bottom
    0.26→0.94 of the canvas) — a partial right-side underline instead of
    a full-width one (was 0.02/0.06 → 0.90/0.96). Same pen-sag shapes.
  - HOLE RINGS (`ui/components/PaperStatCard.kt`): the default "coil"
    ring is redrawn as the user's reference SVG — a FORESHORTENED
    spiral-notebook wire (73:51 aspect, correcting the old round-ring
    "view") looping up the left, over the top, down the right, curling
    in at the bottom; drawn OVER the shaded hole interior ([drawHoleInterior])
    so the punched hole shows through the coil's inner opening. Three
    passes mirror the SVG: a dark depth stroke behind (18px pass, #101B27
    light / #22282F dark), the metal tube gradient on top (8 tuned stops
    from the SVG's palette — cool polished steel, dark-mode light steel
    reversal), and a white specular along the upper-left (3px pass,
    0.75 light / 0.60 dark). The coil's outer loop is ~2.1× the hole
    diameter (holeR × 4.2 wide). Old arc-based coil drawing + CoilBackDark
    deleted; "split" and "oblique" styles untouched.
- **v193 — nav pill COLLAPSES when leaving a tab (was vanishing).** User:
  "the home nav pill should collapse just the way it expands when i back
  from home… it still just vanishes instead of collapse vanishing". ROOT
  CAUSE: the floating bar was composed only while
  `routePrefix in CurioRoutes.bottomNavRoutePrefixes` (the `showBottomBar`
  gate in CurioNavHost), so navigating to a non-tab page (Profile,
  settings sub-pages, the Topic Reveal…) unmounted the WHOLE bar the
  instant the route changed — the expanded pill never got a chance to run
  its collapse spring and simply disappeared. FIX: the bar's composition
  now gates on a `barVisible` state that stays true for 500ms after the
  route leaves the tab set (`LaunchedEffect(showBottomBar)` + `delay(500)`
  — the pill's collapse spring + label retract settle in ~450ms), so the
  deselected pill glides closed with the SAME springs it expands with,
  then the bar unmounts. Returning to a tab cancels the pending delay and
  remounts instantly (no flicker, pill expands as before). The wide-window
  rail keeps the instant `showBottomBar` gate (rail items never expand).
- **v192 — shuffle main card drops the year pill (reveal keeps it).**
  User: "in shuffle main card dont show the year pill just inside the
  topic reveal". The Spin ticket's top-left pill row (v141) was byline +
  year qualifier; the year pill (`yearQual` from
  `titleAndYearQualifier`, the Schedule glyph chip) is REMOVED from the
  shuffle card — the byline pill stays. The Topic Reveal hero keeps its
  year pill (top bar, next to the category chip), and the card title
  still drops the trailing year, so the shared-element morph stays clean.
- **v192 — detail hero tear rim matches every other hero. (branch
  Alpha)** User: "the tear logic of detail screen seems totally differnt
  from rest of the screens, can u fix it". The detail hero's shape
  construction was ALREADY aligned with Home (v104: `SoftTornBottomShape`
  + `SoftTornSheetShape` with `bold = true`, same seed, same 10dp lip /
  14dp baseline, same v108 sheet gate) — the ONE remaining divergence was
  the torn-edge shadow rim under the seam: every other hero (Home,
  Profile, Cabinet, Settings, Onboarding, TopicHistory, Spin) draws
  `Color.Black.copy(alpha = 0.20f)`, but the detail hero drew a warm
  paper-colored `heroSheetColor.copy(alpha = 0.72f)` band (near-black in
  dark mode) that read as a totally different tear. Now it draws the same
  20% black hairline.
- **v191 — drawer constellation as a BRAIN NEURAL WEB + floating tap
  popover. (branch Alpha)** User: "in drawer we have your constellation
  right but its random? isnt it. and it doesnt show real data yet. but i
  want to draw the costellation pattern as a brain neural connection. and
  when i tap the dot it shows me the info belo but i want that to sho as
  a floating small thing and also less data. and also in future i will be
  replacing the category with real knowledge based things just like brain
  knowlegde you get it right?" Clarified via ask_user: explored-only
  neurons (keep), popover = name + saved count (both later replaceable by
  knowledge nodes), and the drawer widens a little if the brain feels
  squished.
  - BRAIN NEURAL WEB (`ui/components/CurioConstellation.kt`): the old arc
    scatter sat every star in a flat bottom band of the canvas (looked
    random/squished). Neurons now fill two hemisphere ELLIPSE lobes
    (phi sweeps -π/2..π/2, radial 0.3..1.0 fills the lobe interior; lobes
    bulge outward at mid-height and taper to the midline top/bottom — the
    brain silhouette with the fissure gap). Links are CURVED quadratic
    beziers (perpendicular sag 0.12) instead of straight lines: every
    neuron → its 2 nearest neighbours (synapses) + its nearest neuron on
    the OTHER hemisphere (corpus-callosum bridges, deduped). The gold
    fissure is now a soft curve down the centre line. Data unchanged —
    stars = explored lanes, size = saves, glow = active (real passport
    data, deterministic positions).
  - FLOATING POPOVER: `CurioConstellation` gained a
    `popoverContent: (@Composable (CategoryId) -> Unit)? = null` slot —
    when provided, the selected neuron shows a small floating card
    anchored just above the dot (below when near the top), clamped inside
    the canvas, tap-to-dismiss (BoxWithConstraints + onSizeChanged for the
    clamp). The DRAWER passes a compact name + "N saved" chip and its
    richer below-panel (the 4 stat chips + last-explored line +
    `DrawerMapStat`) is DELETED — "a floating small thing and also less
    data". The Stats page passes null and keeps its own below-panel.
  - DRAWER WIDTH: `ModalDrawerSheet` 320 → 336dp so the neural web has
    room to breathe (user's "extend the drawer a little more to the
    right" contingency).
  - FUTURE NOTE: the neurons are fed by [CategoryId] + count maps today;
    the user plans to replace category lanes with real knowledge-based
    nodes — the component only reads the id list + maps, so swapping the
    data model later is caller-side only.
- **v190 — Material theme polish: pastel-softened material cards, mixes
  collapse to the scheme primary, readable adaptive-hero contrast, M3 nav
  roles. (branch Alpha)** User: "the material main card colors are good
  but in pastel mode they are not. and also in material theme dont let
  the mix color come, make it the material color when they get mixed.
  also in light mode adaptive hero the hero card looks washed out along
  with the glyphs and the texts and the box. fix that keep the material
  color for it but fix it. and fix the nav bar material color as they
  are bad". Clarified via ask_user: mixes → scheme PRIMARY (one brand
  color); hero → DEEP material banner with DARK ink; nav → pure M3 roles.
  - PASTEL CARDS: the Material branches of `CurioGradients.cardGradient` /
    `heroBlendGradient` and `CurioMixedDeck` ignored Pastel mode, so the
    material main card kept its full-strength muted family fill while
    everything around it softened. All material card fills now resolve
    through `pastelAccent(fill, dark)` when Pastel is on (the composable
    gradients read the pref directly; `mixedDeckAccent` honors its
    `pastel` param).
  - MIXES → PRIMARY: `mixedDeckAccent` / `mixedDeckGradient` gained a
    `materialPrimary: Color? = null` param; SpinScreen passes
    `MaterialTheme.colorScheme.primary` when Material is on and >1 lane is
    selected, so a mixed deck wears THE one material color — peeks, spin
    button, confetti and the nav tint all follow — and the multi-hue
    family sweep is gone (`mixedDeckGradient`'s Material branch renders
    the standard quiet `cardGradient` from the single resolved color).
    Same-family mixes keep the family fill; pastel mode pastel-softens
    the primary too.
  - ADAPTIVE HERO CONTRAST: the light-mode Material hero banner was the
    pale T90 scheme containers (secondaryContainer / tertiaryContainer /
    surfaceContainerHighest) with near-white ink — washed out. The new
    `materialHeaderAccent()` LIGHT branch wears the RICH family color
    (family fill lifted to L=0.70, sat capped 0.55) and new
    `materialHeroInk()` (a deep same-hue twin via `readableLightInk`)
    pairs with it through `heroHeaderInk()`'s Material-light branch —
    title text, watermark glyphs and the banner box read crisp while the
    material family hue stays. Dark keeps the deep T30 containers + light
    ink (unchanged).
  - NAV M3 ROLES: under Material, `curioNavContainerColor` /
    `curioFloatingNavContainerFor` return `surfaceContainer` (the M3 nav
    container role) and `curioActivePillFill` / `curioActivePillInk`
    return `secondaryContainer` / `onSecondaryContainer` (the M3
    navigation indicator) — no per-lane colors in the bar. Applies to the
    floating pill bar, the wide-window rail and the reveal Like/Dislike
    capsule (`curioFloatingNavContainerFor`).
- **v189 — page-switch haptics; Mix/Cancel as ONE nav-bar capsule;
  picker + filter apply on pop-back. (branch Alpha)** User: "add haptics
  when switching pages with nav pill or like or pet designer too" + "also
  make the mix and cancel as a navbar style pill and also let user apply
  the mix even when it pops back same for filters".
  - HAPTICS: a light tick (`HapticFeedbackType.TextHandleMove` via
    `LocalHapticFeedback.current`, resolved in composition — never inside
    the clickable) fires on: tab switches in all three nav surfaces
    (`CurioFloatingNavBar` floating pill + M3 `NavigationBar` branch +
    `CurioNavigationRail`), the Topic Reveal Like/Dislike segments, and
    the Pet Designer studio bar tabs. `TextHandleMove` is the lightest
    standard tick (no strong press flash on Android 12+).
  - MIX/CANCEL CAPSULE: the picker's floating Mix + Cancel now live in
    ONE capsule styled like the bottom nav bar — `Surface` with
    `RoundedCornerShape(50)`, `curioFloatingNavContainerFor(wash)`
    (internal, CurioBottomNav.kt — the page wash lifted toward the
    elevated surface, dark = surfaceContainerHigh), `shadowElevation =
    6.dp`, inner Row `padding(8.dp)`; Mix stays the accent-filled
    active-pill (`themedButtonFill`/`themedButtonInk`), Cancel is plain
    text with `heightIn(min = 44.dp)` to match the pill's height.
  - APPLY-ON-POP: both sheets' `onDismissRequest` now APPLY instead of
    dropping the draft when there's something to apply. Picker:
    `multiSelectMode && selectedSlugs.isNotEmpty()` →
    `onCategoriesSelected(selected)` (swipe/scrim/back applies the mix;
    single-select or empty selection just closes). FilterSheet: draft
    differs from initial → `onApply(draftFilters, draftSubtypes)`; a
    no-change pop keeps the old set.
- **v186 — drawer shows the Stats page's constellation; nav labels larger;
  footer slimmer. (branch Alpha)** User: "make the home shuffle cabinet
  tet xt even larger in default look and in drawer show the your
  constellaetion from the your curiocity page not another thing bruh" +
  "also the footer looks big its good now but looks big so make it more
  small".
  - SHARED CONSTELLATION: extracted the Stats page's `CategoryConstellation`
    into `ui/components/CurioConstellation.kt` (internal, public in the
    components package) — the exact same brain two-lobe rendering, web
    links, gold fissure, size-by-saves, recent glow, tap-select. The Stats
    page calls the shared component (private duplicate deleted, 5 unused
    imports removed). The DRAWER's `DrawerCuriosityMap` now calls the same
    `CurioConstellation` with passport data (explored lanes sorted by
    ordinal, `laneCounts = saves`, `laneRecent = lastAt`, `recentCutoff =
    0L` = all-time → every explored lane glows), and the whole
    `DrawerLaneConstellation` grid-web function (v176-era) was deleted
    (~140 lines). The drawer's richer passport tap panel stays.
  - NAV LABELS: default-look Changa One label 13 → 15sp (still fits the
    136dp expanded pill; guidelines branch untouched).
  - FOOTER: 210 → 150dp tall, fade 110 → 80dp — the planet reads as a
    small bottom band.
- **v185 — proper M3 Material theme (1 opt-in toggle), on branch Alpha.**
  User: "go to alpha branch and sync it with main (alpha was so much
  behind)… read everything [m3.material.io color system overview +
  get-started + full guideline]… we will be adding 2 new toggle. with one
  beaigh a proper material theme with category colors either getting
  nothing or maybe a one color from the material color… and another test
  full material guideline text spacing boxes layout evrything… it will be
  a new extra sytem as a toggle without chnaging anything thats in our
  current app and look… make the proper plan and follow it untill its
  done." Synced Alpha (was 251 behind) → fast-forwarded to main a127f10 +
  pushed. Clarified via ask_user: (1) "clear the current material style
  and fully redo it" — the old partial Material style was ALREADY removed
  in v78, so this is a from-scratch rebuild; (2) category colors = "one
  color per family, muted" (6 families); (3) the guidelines toggle is
  INDEPENDENT of the Material theme (works on Curio colors); (4) brand
  chrome = "give both as an option, all opt-in, no default on" (full M3
  chrome vs keep Curio chrome sub-option).
  - M3 research: color system = 5 key colors × 13-tone palettes; the
    multi-color guideline is RESTRAINT (neutral surfaces, ONE primary,
    secondary/tertiary for muted accents — never a rainbow per section);
    dynamic color (Material You) on Android 12+; typography = 15-style
    scale; shapes 4/8/12/16/24; elevation = tonal overlays.
  - PHASE A — pref + UI: `materialThemeState` (default OFF) + 1 Appearance
    row in SettingsSectionScreen.kt. NOTE — the "Material guidelines" +
    "Material chrome" system (PHASE C below) was later REMOVED wholesale
    (user verdict: "its not good"): `MaterialGuidelines.kt`, the
    `materialGuidelinesState` / `materialChromeFullState` prefs and both
    Appearance rows are deleted; `CurioTheme` always uses
    `CurioTypography`/`CurioShapes`; `CurioBottomNav` always renders the
    floating pill bar with Changa One labels. Only the Material theme
    toggle remains.
  - PHASE B — Material color: `MaterialColorSchemes.kt` (dynamic light/
    dark on API 31+, `MaterialBaselineLight/DarkScheme` seeded from the
    brand coral via a `materialTone(hue, sat, tone)` M3 tone→lightness
    ladder for older devices); `MaterialFamilies.kt` (6 families by hue,
    near-achromatic → NEUTRAL; family fill T40 light/T80 dark, ink
    T100/T20; rose→secondary + green→tertiary map to the scheme's own
    roles; non-composable `*For(dark)` twins for remember-block paths);
    choke-point wiring in CategoryInk.kt (themedAccent/categoryInk/
    onAccent/headerAccent/backgroundWash→neutral/surfaces→neutral/
    InkFor+AccentFor twins) + CurioColors.kt (cardGradient/heroBlendGradient/
    mixedDeckAccent/mixedDeckGradient/mixedDeckWash→neutral) + CurioTheme
    (curioColorScheme → materialColorScheme).
  - PHASE C — guidelines + chrome: BUILT (`MaterialGuidelines.kt` =
    MaterialTypography / MaterialShapes / CurioSpacing tokens, gates
    materialGuidelinesOn / materialChromeFullOn; CurioTheme typography+
    shapes swap; M3 NavigationBar under full chrome; Changa One drop from
    pill/rail labels), then REMOVED — see the NOTE in PHASE A. The
    per-screen spacing/layout sweep never shipped with it.
  - LESSON: extension functions on an enum type (`MaterialFamily.forAccent`)
    need a receiver INSTANCE — a factory-style helper must be a plain
    top-level function (`materialFamilyFor`).
- **v184 — nav pill: calmer morph/collapse, wider+higher pill, more
  inactive spacing, Changa One labels.** User: "make the nav pill morph
  and collape animation even smoother and calmer. and give the inactive
  buttons a little more space. and use a new bond font for the tet of
  nav pill maybe this one, Changa One" — then "also make it a little
  wide like just a little heigh the pill" — then "also just like it
  expands when i come back to home screen make it collapse when i go to
  other screen from home screen for smoother look."
  - SPRINGS: `PillWidth/Motion/Color/ExpandSpring` stiffness 400 → 240
    (still damping 1.0 critically damped — zero overshoot/bounce). ~40%
    slower settle, lockstep preserved (all four specs identical). The
    collapse ALREADY mirrored the expand (both directions animate via
    the same springs since v162) — the slowdown applies to BOTH, so
    leaving a screen now glides closed exactly as returning glides open.
    Fixed the stale v125 function KDoc that claimed the label exit is
    "instant" (it's animated since v162).
  - SIZE: pill "a little wide" + "a little high" — icon pills 60 → 64dp,
    expanded 128 → 136dp, height 48 → 52dp.
  - SPACING: bar inner padding 7 → 8dp, pill gap 6 → 10dp (inactive
    buttons breathe).
  - FONT: new bundled `changa_one_regular.ttf` (Changa One v1.003, OFL
    with Reserved Font Name "Changa" — license at
    `app/third_party/changa_one_OFL.txt`) + `ChangaOneFontFamily` in
    CurioTypography.kt (SINGLE-entry like PatrickHand: Changa One has no
    bold TTF, so pair with `FontWeight.Normal` or Bold requests trigger
    fake-bold synthesis). Nav pill + rail labels use it at 13sp (12sp
    geom Bold → 13sp Changa One keeps the same visual weight). Note:
    the OFL text must NOT live in `res/font/` (typed dir — AAPT rejects
    non-fonts); it sits at `app/third_party/`.
- **v183 — Spin Filter badge only shows when filters are selected.**
  User: "in spin page the filter always shows the count, make it only
  show when filters are selected."
  - ROOT CAUSE: `BottomCta` computed `hasFilters = filterActiveCount > 0`
    but the call site passed `filterActiveCount = filteredPool.size` —
    the always-non-zero MATCHING-TOPICS count — so the badge
    ("Filter · N") showed permanently even with zero chips ticked. The
    wide/tablet right-rail button (line ~1206) was already gated
    correctly on `activeFilters.isNotEmpty() || activeSubtypes.isNotEmpty()`.
  - FIX: `filterActiveCount` is now `Int?` — null until chips are
    selected (pill reads plain "Filter"), and when selected it keeps the
    v83 design (the total topics matching the filters). Labels guarded by
    `hasFilters` smart-cast it to non-null.
- **v182 — crash fixes: drawer grid-web OOB + Pet Designer negative
  padding, and the FilterSheet Apply pill floats like the picker's
  Mix/Cancel.** User: "fix this app crah on drawer open and also in
  category picker theres still soemthing at the button and do the same
  with filters tooof spin screen" + a crash report
  (`IndexOutOfBoundsException: Index 29 out of bounds for length 29`,
  at draw, drawer open), then a second crash
  (`IllegalArgumentException: Padding must be non-negative`, Pet
  Designer UI).
  - DRAWER CRASH: `DrawerLaneConstellation`'s grid web (v178) drew
    right/down links guarded only by `col < c - 1` / `row < r - 1` —
    with a NON-rectangular grid (29 lanes → 6×5, last row 5) the last
    node of a short row indexed `pts[i+1]`/`pts[i+c]` PAST the array
    end. Fixed with length guards `i + 1 < n` / `i + c < n`. LESSON:
    array-index guards in DrawScope loops must check BOTH the grid
    position AND the array length — grid math and list length diverge
    whenever the lane count isn't a perfect rectangle.
  - PET DESIGNER CRASH: the v179 full-bleed banner used
    `padding(horizontal = -edgePad)` — **Compose forbids negative
    padding** ("Padding must be non-negative", thrown at layout).
    Replaced with the standard full-bleed trick: `BoxWithConstraints`
    → `offset(x = -edgePad)` + `requiredWidth(maxWidth + edgePad * 2)`
    so the tear still reaches both screen edges. LESSON: NEVER emit
    negative `padding()` — use offset + requiredWidth instead.
  - FILTER SHEET ("do the same with filters"): the Apply / Show all
    button is no longer a full-width bar below the chips — it FLOATS
    over the sheet content (`align(BottomCenter)`, bottom 26dp) as the
    same raised accent pill (fill/glow/glass unchanged), and the chips
    column's bottom padding is now 88dp so the pill never covers the
    last row. The category picker's bottom was already clean in current
    code (the v180 rework removed the footer — the user's crash build
    predated it).
- **v173 — pill morph slowed again (400) + Cabinet "All" wears the SPIN
  accent.** User: "the navbar morphe open is still tooo rapid aah, make i
  even more sloer. and in cabinet all use blue or red or whatever the spin
  screen color have set not yellow or anything else."
  - SPRINGS: all four nav-pill specs (PillWidth/Motion/Color/Expand) and
    the mirrored Reveal* + Studio* families in TopicRevealScreen.kt /
    PetDesignerScreen.kt dropped stiffness 750 → 400 (damping stays 1.0
    — critically damped, zero bounce). ~35% slower settle, still lockstep
    (same physics across all four specs per file). Do NOT touch
    `Springs.Calm` (750, in CurioMotion.kt) — that's the page/dialog
    entrance family, not the pills.
  - CABINET "ALL" COLOR: `curioActivePillFill(null)` fell back to
    secondaryContainer = ButterYellow@30% — the stray yellow. Fix in
    `curioNavActiveAccent`: CABINET returns `cabinetAccent ?:
    (spinAccent ?: primary)` — the pill inherits the SPIN deck's
    published accent ("whatever the spin screen color have set"; default
    wildcard deck = CoralBlush, the brand primary) and gets the same
    light-mode 55% saturation mute as Spin's own pill. The butter
    fallback now only reaches Home-without-hero / non-tab routes. LESSON:
    the theme's secondaryContainer is BUTTER (yellow) — any "stray
    yellow" on a plain page is this fallback, not the nav code.
- **v172 — mood board quote cards: resize scales the WHOLE note (text
  included), and the export renders the same size/spot as the editor.**
  User: "the moodboard quote cards are still very bugged... both in
  editing and sharing". ask_user: resize "only expands from side and the
  text size stays the same", export "either gets big and looks differnt
  or its position is somewhere else". MoodBoardZoom.kt:
  - RESIZE: the slip no longer forces a fixed slot height (heightIn
    h..1.5h in the editor, exact h when saved) with a fixed-size font —
    it now sizes to content and the quote TEXT scales with the card
    (textScale = renderW ÷ baseW, baseW = the never-resized slot width
    × view scale, floor 0.5). Resize = a true uniform note scale.
  - EDITOR/EXPORT MISMATCH: removed the v60/v108 40%-of-canvas display
    cap (`displayScale` is now just `scale` in every view). The cap made
    a resized card render small in the inline editor but at raw size in
    the export — the shared PNG showed it bigger and, reaching past the
    same top-left, "somewhere else". With v113's raw-space slots the
    board scale is already correct, so cards are rawW × scale everywhere
    (inline editor, full-screen editor, saved card, export). rawSpace is
    now an inert API param (kept for callers).
  - LESSON: "text size stays the same" = the resize only changed the
    slip width; "gets big in export" = the editor-only display cap
    disagreed with the export's raw render.
- **v165 — v162's one-spring-family CI fix: the specs are TYPED per
  animated value.** CI failed: `SpringSpec<Float>` passed where
  `AnimationSpec<Color>` (fill/icon tint) and `FiniteAnimationSpec<IntSize>`
  (the label's expand/shrinkHorizontally) were expected. The generic
  type parameter of a spring MUST match the animated value's type. Fix:
  each pill now declares `spring<Dp>` (width), `spring<Color>` (fill +
  icon tint), `spring<IntSize>` (label expand/shrink) and
  `spring<Float>` (fadeIn/fadeOut) — same 0.9 damping + Medium stiffness
  everywhere, so the lockstep physics are unchanged. Added the `IntSize`
  import in all three files. LESSON: `spring<Float>` is ONLY valid for
  float animations; sharing one spec across color/size/fade animations
  needs one spring per target type.
- **v157 — dark-mode hairline rims removed from the floating nav bar and
  the detail quick-fact plate.** User: "why in dark mode the navbar
  floating one have borders? remove that", plus "i notices in detail view
  theres border in quick fact box". The v149 dark-mode
  `BorderStroke(1.dp, White@10%)` capsule rim on `CurioFloatingNavBar` is
  GONE (the elevated fill alone defines the capsule), and the v115
  `Modifier.border(1.dp, ink@18%)` on `QuickFactCard`'s plate is GONE too
  (its lifted fill alone defines the plate) — the `BorderStroke` /
  `foundation.border` imports followed. NOTE: the same dark rim still
  lives on the tour dock (CurioNavHost), the reveal Like/Dislike pill
  (TopicRevealScreen), the pet studio bar and the floating action capsule
  (PetDesignerScreen) — not touched, offered to the user. LESSON: the
  hairline-rim "elevation" trick in dark mode is a look the user may not
  want on every floating surface; ask rather than blanket-apply.
- **v156 — Pet Designer layout rework (user-confirmed): compact bottom
  nav, floating top action capsule, tear scrolls away in-flow.** User:
  "the pet designer floating nav is stretched all the way fix that. and
  place the save undo redo save and share at the top and sticky, and
  make the tear design be on the background itself and it scrolls away
  when i scroll down". Confirmed via ask: bottom nav = compact centered
  capsule (like the main bar); actions = floating pill over the banner
  (pinned while scrolling); tear = banner becomes the first scrollable
  item (scrolls away in-flow). (1) `PetStudioBottomNav` lost its
  `fillMaxWidth()` capsule — a wrapping `Box(fillMaxWidth, Center)` now
  holds a CONTENT-SIZED capsule centered at the bottom. (2) The old
  full-width `EditorToolbar` (a stickyHeader inside the list, below the
  hero at rest) became `StudioFloatingToolbar`: ONE rounded capsule
  (`curioFloatingNavContainer` + dark rim) pinned `TopEnd` below the
  status bar — compact Save text pill (with a dirty dot) + 38dp
  Undo/Redo/Reset/Share/Import icon circles (`ToolbarIcon` gained a
  `size` param). Toasts now auto-clear (`LaunchedEffect(toast) { delay(3000) }`)
  and show as a transient pill under the capsule. (3) The torn banner
  moved from the overlay graphicsLayer-translation Box (v109/v113) INTO
  the list as its first `item` — the tear is part of the page background
  and scrolls away naturally; the overlay Box, the stickyHeader, and the
  `SettingsHeroTotalHeight` top padding are gone. LESSON: a "sticky
  hero that scrolls away" is simpler and more robust as the list's first
  item than an overlay Box with `viewportStartOffset` translation math —
  and a floating action capsule pinned to the screen edge replaces a
  sticky-header strip without the status-bar dance.
- **v155 — light-mode nav capsule finally shows the page tint + pill
  animations smoothed.** User: "the active indicator gets the theme
  dynamic color but in light mode the background of it doesn't so make it
  get the background tint", and the animations "feel clanky sometimes —
  make it even more smoother". (1) `curioFloatingNavContainer`'s light-
  mode lift was `lerp(wash, surfaceContainerHigh, 0.55)` — 55% toward the
  parchment elevated surface washed the page tint out completely, so the
  capsule read as a plain cream bar behind the colored pill; now 0.30 so
  the tint shows while it still reads lifted (dark unchanged). LESSON:
  a lerp "lift" toward the elevated surface can silently erase a subtle
  page tint — the pill gets the color while the background loses it.
  (2) Smoother pills (nav bar + reveal Like/Dislike, both the same
  recipe): width spring damping 0.75 → 0.9 (the old one overshot and
  bounced on settle); the active fill now FADES via
  `animateColorAsState(activeFill.copy(alpha = …))` synced to the same
  spring instead of snapping on/off; the icon tint crossfades
  (tween 200 FastOutSlowIn); the label's fade tracks the pill's
  expansion (tween 160 → 240 FastOutSlowIn; exit stays instant per
  v125). LESSON: underdamped springs + hard color snaps read as
  mechanical pops; fade the fill/tint with the same spec as the width
  morph and near-critically damp the spring.
- **v153 — nav-bar → reveal sentiment-pill morph REVERTED (the bigger
  pill stays).** User: "revert this just keep the size large but revert
  the shared morph one". The v151 shared-element morph (the nav bar's
  capsule collapsing into the reveal's Like/Dislike pill) is fully
  removed: `SentimentSharedElementKey` + `NavPillBoundsTransform`
  deleted from RevealSharedScopes; `CurioFloatingNavBar` /
  `FloatingNavPill` lose the `sharedElementState` / `visible` /
  `interactive` params (back to plain `clickable`); CurioNavHost drops
  `sentimentMorphVisible` + `sentimentSharedState` and renders the bar
  with the plain `if (!wide && showBottomBar && …)` block (the bar hides
  and the reveal pill slides up as it did before); `RevealSentimentPill`
  loses its `modifier` param. KEPT: the 60dp/128dp pill + 26dp icon.
  LESSON: a shared-element morph that pairs an overlay bar with a
  route's content is a big structural change (bar inside the
  SharedTransitionLayout, caller-managed visibility, 500ms hold) — the
  user may prefer the simple hide-and-slide-up; ask before committing
  to that scale of refactor.
- **v151 — bigger bottom pill (60dp/128dp).** User: "the bottom pill
  can be more larger". `FloatingPillIconWidth/ExpandedWidth/Height`
  52/112/52 → 60/128/60, icon 24→26 so the bar reads proper and the
  active tab's label has real room. The same commit also tried a
  shared-element morph (nav bar capsule → reveal Like/Dislike pill,
  `SentimentSharedElementKey` + `NavPillBoundsTransform`, bar kept
  composed 500ms as the caller-managed source) — the user reverted the
  morph after seeing it (see the v153 note); only the SIZE shipped.
- **v152 — the remaining topic files deduped (178 groups, 181
  entries).** The batch-duplication pattern v127 flagged ("other topic
  files… but only books were deduped") is now fixed for every file that
  had it: authors 38 groups, astronomy 89, songs 26 (two triplets),
  geology 11, animals 10, technologies 3, chemistry 1 — 178 groups /
  181 entries collapsed to one each, mirroring the books rule (richest
  entry wins: longest teaser + richest exploreAction + most tags; tags
  unioned keeper-first; tier preserves 1; first-position placement;
  per-file indent preserved — astronomy/technologies use indent 2, the
  rest 1 — so untouched content stays byte-identical). `topic_index.json`
  rebuilt (`scripts/build_topic_index.py`, 16,833 topics, fully in sync
  both ways — the old index was stale, predating recent content
  commits). Android assets ONLY — the web mirror was NOT touched per the
  root AGENTS.md 🔒 scope rail (web/ is on hold).
- **v150 — floating pills go THEME-AWARE + DYNAMIC (user-confirmed:
  container follows the page tint, active pill follows the page color,
  dark-mode elevation), plus the reveal Like/Dislike pill gets the nav-bar
  expand/collapse animation and the picker's Manage categories floats.**
  (1) `CurioNavTint` now also publishes per-tab ACCENTS (spin/cabinet/
  home); `curioFloatingNavContainer(routePrefix)` lifts the page wash
  toward the elevated surface (light mode; dark keeps surfaceContainerHigh
  since the pages are near-black) and `curioNavActiveAccent` picks the
  page's accent. `CurioFloatingNavBar` container = the dynamic tint
  (animated 420ms), active pill = page accent + `pastelFillInk` (fallback
  secondary/onSecondary), and every floating pill bar (nav, tour dock,
  pet studio) draws a hairline `BorderStroke(1.dp, White@10%)` rim in
  dark mode — the black shadow is invisible on the near-black pages
  (`curioDarkGlow` was retired in v30, so a rim replaces the old glow).
  (2) Reveal `SentimentSegment` mirrors `FloatingNavPill`: icons at rest
  (52dp), active springs to 96dp with the label slide-out, same spring.
  (3) Picker sheet's Manage categories TextButton → floating pill. Also
  fixed the CI break: `boardHasContent` was declared INSIDE the collage
  Box but read outside it (the Box closes before the pin zone) — hoisted
  to the canvas top. LESSON: publish page COLORS (not just washes) when
  an overlay pill should wear them; and a val used across sibling scopes
  must live at the shared parent scope.
- **v149 — (1) progress editor REVERTED to the ring design, keeping only
  the page-count EDITING improvement; (2) saved-entry SHARE gets a
  preview sheet.** The user: "revert the progress ui — i only meant you to
  change the page count look and its editing way, not redesign it; the way
  to edit the page count is bad". The v135 stepper-first dialog (31e5fea)
  was over-reach: `git checkout 4558e99` restored the ring dialog (big % +
  count, −/+ steppers, slider, Finish/Save) and two improvements were
  re-applied on top: the slider snap fix (≤200 total = whole-unit steps,
  big totals = continuous rounded — the "editor isn't working" bug) and
  the count is now a PLAIN display with an explicit "Edit total" chip +
  pencil below it that opens the inline numeric field (the old hidden
  tappable count read as plain text — the "way to edit the page count is
  bad" complaint). Keep the TopicProgressStore/topic changes (the
  editable-target feature itself stays). (2) Share: the EntryDetail More →
  Share item used to fire the ACTION_SEND chooser with no preview; it now
  opens `EntryShareSheet` — a ModalBottomSheet (theme surface, drag
  handle) with a live 320dp `CurioShareCard` preview on a shadowed stage,
  an Image card / Text pill picker (solid-secondary selected per the
  v131 contract), and a Share button (image = the existing
  `shareComposableCard` 400×400 PNG path; text = plain-text summary via
  `entryShareText`). State lives in `DetailStickyBar` (`rememberSaveable`
  `showShareSheet`); the sheet renders inside the sticky bar's Row but is
  a Dialog window, so the bar's scroll graphicsLayer doesn't affect it.
  LESSON: a "fix the editor" ask can balloon into a full UI redesign —
  keep the original layout and fix the affordance; and a share action
  deserves a preview step, not a blind chooser.
- **v148 — the Pet Designer studio bar animates EXACTLY like the main
  nav bar ("unify the pill style" included the ANIMATION).** v142
  restyled `PetStudioBottomNav` to the floating pill container but kept
  static `weight(1f)` tabs with always-visible labels; the user
  clarified: "use similar animation just like in home screen nav bar —
  similar style collapse". `PetStudioTab` now mirrors `FloatingNavPill`
  verbatim: icon-only 52dp pills at rest, the ACTIVE pill springs to
  112dp (same spring: dampingRatio 0.75, StiffnessMediumLow) and slides
  its label out (expandHorizontally(Start) + fadeIn(160), exit
  tween(0) so the deselected label vanishes), solid `secondary` fill +
  `onSecondary` ink; the Row centers so the width change stays balanced
  in the container. LESSON: "same style" for a restyle means the pill's
  BEHAVIOR too — icons-at-rest + active-expands-label is the app's
  signature pill animation, not just the rounded container.
- **v147 — the Home drawer is HOISTED to the NavHost root so it draws
  ABOVE the floating pill bar, which stays composed underneath.** v135 had
  "hidden" the bar while the drawer was up (NavHost dropped it from
  composition via `!CurioDrawerState.isOpen`) — the user's intent was
  "place the drawer above it", not "disappear the bar and reappear it":
  the bar visibly vanished at drawer-open and popped back at close. The
  fix moves `ModalNavigationDrawer` OUT of HomeScreen to wrap the whole
  NavHost root Box (page + rail + bar + tour dock); the bar's `if` keeps
  only the tour gate. HomeScreen's hamburger now raises a request via
  `CurioDrawerState.requestOpen()` (an incremented `openTick`, observed by
  a NavHost `LaunchedEffect` that opens its own `rememberDrawerState`)
  instead of touching a local DrawerState; `HomeDrawerContent` became
  `internal` so the NavHost can render it; Home's page kept its wrapper as
  a plain `Box(fillMaxSize())` (the drawer's old content slot) so no
  re-indent was needed. LESSON: a modal overlay that must cover a
  sibling overlay drawn later in the Box needs to be hoisted ABOVE that
  sibling in the tree — zIndex cannot escape the parent Box — and "hide
  the thing under it" reads as a glitchy vanish/reappear.
- **v146 — reveal year pill moves out of the hero into the top bar.**
  The v141 hero top-left pill ROW (byline + year) collided with the
  progress badge on the hero's TOP-RIGHT: a long byline ("Director ·
  Christopher Nolan") pushed the year pill ("1941") under the progress
  pill on topics with reading progress ("it's already covered by progress
  pill"). The year pill now rides NEXT TO the category chip in the reveal's
  top bar (left corner, `Row(weight(1f, fill=false))` wrapping the chip +
  year pill; same frosted `categorySurface` recipe as the chip, Schedule
  glyph + `categoryInk()`); the hero's top-left keeps ONLY the byline pill
  (the morph element shared with the Spin ticket). LESSON: the hero's top
  corners are shared with the morph (byline) AND the progress badge
  (top-right) — the top BAR is the right home for secondary pills like the
  year qualifier.
- **v145 — mood board: quote cards independent per view; real resize
  (proportional height + 60% size limit); PNG/expanded dialog keep exact
  card sizes; Clear board wipes quotes; Quote chip moves up on content.**
  (1) **Quote cards are now INDEPENDENT between the small and full-screen
  boards** (per user, confirmed): positions were already separate
  (quotePositions vs quotePositionsFull) but the full-screen resize
  handler ALSO wrote the SHARED `quoteCards.setWidth`, and the full-screen
  move handler pulled the shared inline width in (`widths.getOrElse`) —
  so resizing a card in full-screen changed the small board too ("why is
  the quote card the same in both small and full screen"). v145:
  `onResizeQuoteOverride` writes ONLY `fullQuotePositions[i].w` (no
  `quoteCards.setWidth`), and `onMoveQuoteOverride` preserves the full
  placement's OWN width. The full-screen board seeds from
  `quotePositionsFull` with a legacy fallback to the inline spots (mirrors
  `fullTiles`' tileLayoutsFull→tileLayouts fallback — old boards keep
  their single arrangement until rearranged). (2) **Resize is a real
  resize, quote cards only**: `MoodBoardFloatingCards` derives the card
  HEIGHT from its width at the slot's paper aspect (`cardH = cardW ×
  slot.h/slot.w`), so a wider card grows taller instead of stretching
  flat; the resize grip's `maxW` is now capped at 60% of the visible
  board (was `boardW - x` — a card could span the whole collage). (3)
  **PNG export + expanded dialog pass `rawSpace = true`**: the export's
  canvas mirrors the board's aspect (raw × scale), so the old 40% display
  cap bound `displayScale` against the RAW board width (canvasWPx = maxX)
  and shrank resized cards in the saved PNG ("gets small / wrongly
  placed"); the expanded dialog capped cards at 40% of the displayed
  board while the full-screen editor showed the exact width — now both
  render exact raw widths, matching the editor. The INLINE editor + saved
  small card keep the 40% cap (v60/v108 look). (4) **Clear board clears
  quote cards too**: the confirm dialog counts quotes ("Remove all N
  images and M quote cards?") and the action wipes them via
  `removeCard(0)` in a loop (fires onCardRemoved → fullQuotePositions
  stays aligned). (5) **Quote chip position**: a `boardHasContent` flag
  (tiles OR quotes) drives both the chip's bottom padding (16dp when
  empty, 88dp once content — the Clear button appears at 16dp and the
  chip must sit above it) and the Clear button's visibility (was
  tiles-only, so a quotes-only board couldn't be cleared).
- **v144 — tour controls are a floating pill bar; the nav bar yields
  during the tour.** (1) **The tour's Skip/Next dock** was the last
  full-width opaque bottom band in the app (a v9.x `Surface`
  `fillMaxWidth` + `tonalElevation` covering the whole strip) —
  converted to the v124/v129 floating pill recipe: rounded-50
  `surfaceContainerHigh` capsule, 6dp shadow, `navigationBarsPadding()`
  + 12dp air gap, content-sized 52dp capsule buttons inside (Skip =
  soft `surfaceVariant` secondary, Next/Done = solid primary CTA),
  7dp row padding / 6dp spacing. The dead `fillMaxWidth` import was
  removed (the file still uses `windowInsetsPadding` for push routes).
  (2) **The main `CurioFloatingNavBar` now YIELDS while the tour runs**
  (`TourController.currentStep == null` added to the v135 drawer gate):
  the tour pill floats at the same bottom-center spot on tab stops, and
  the old opaque dock covered the bar anyway — the bar must not show
  behind/around the tour pill. LESSON: a floating control that replaces
  a docked band must also inherit the band's occlusion behavior (the
  bar it sits over must hide), or the two pills stack at the same spot.
- **v129 — floating pill bar: Scaffold removed (no strip) + no more
  switch squeeze.** (1) **The strip is gone for real.** The v125 fix
  painted the nav slot with the page wash, but the flat band still read
  as a strip (the page has watermarks/gradients/tears above it, so a
  solid band below is visible). `CurioNavHost` no longer wraps content
  in a `Scaffold`: the page Row runs full-bleed, and
  `CurioFloatingNavBar` is now a true overlay in the root `Box`
  (`Modifier.align(Alignment.BottomCenter)`) drawn ON TOP of the page's
  own background — no painted slot at all. The bar wraps its content
  (no `fillMaxWidth`, no 72dp slot, no wash band) and floats with
  `navigationBarsPadding()` + a 12dp air gap. (2) **The squeeze fix.**
  v125 made the newly active pill spring open while the deselected one
  snapped shut (`tween(0)`), so the bar's total width DIPPED then grew
  back — re-centering the whole bar ("it squeezes"). Both pills now
  animate with the SAME spring (`spring(0.75, StiffnessMediumLow)`):
  the shrinking pill's width loss equals the growing pill's gain at
  every frame, so the total stays constant and the bar never moves.
  The label exit stays instant (closing pill's text vanishes — v125
  preference kept). (3) **Page clearances.** With no Scaffold slot, the
  tab pages clear the pill themselves: Home's final spacer is 92dp on
  phones (32 wide), Spin's phone `Column` gets
  `windowInsetsPadding(navigationBars)` + 76dp, Cabinet's `Column`
  gets the inset + 76dp on phones (0 on wide). Non-tab routes keep the
  nav-bar inset the Scaffold used to deliver (content Box applies
  `windowInsetsPadding(navigationBars)` whenever the pill is hidden).
  The reveal's 80dp placeholder is unchanged (it paints its own band).
- **v131 — offline models, dictation fixes + the settings-card ink fix.**
  (1) **Offline model catalog expanded** (`VoskModels.CATALOG`): four
  bigger tiers join the three smalls — `vosk-model-en-us-0.22-lgraph`
  (Large · English US, ~128 MB), `vosk-model-en-us-0.22` (Full · English
  US, ~1.8 GB), `vosk-model-en-us-0.42-gigaspeech` (Full · Gigaspeech,
  ~2.3 GB) and `vosk-model-en-in-0.5` (Full · English India, ~1 GB),
  sizes from the alphacephei.com model page; the picker copy warns that
  the big models are heavy downloads needing real storage + memory.
  (2) **The floating dictation mic moved INTO the note box's tool dock**
  (`RichTextEditor.trailingAction`, the slot designed for "a small
  dictation button"): it renders above the field (still gated on
  `noteFocused && !dictationOpen && voiceToTextEnabled`) instead of in
  the scroll flow below the editor, so it never hides behind the
  keyboard. (3) **Dictation no longer wipes earlier text on a pause, and
  a break = a full stop** (always on, per user): the transcript now
  ACCUMULATES — `dictatedText` holds committed utterances, `partialTranscript`
  is only the live words; `onResults` APPENDS (never replaces), blank
  partials during a pause no longer clear the preview, and a fresh
  partial after `onEndOfSpeech` commits the previous utterance first
  (prefix-match guards against same-utterance refinements). Committed
  utterances join with a period (`. `) and the next sentence is
  capitalized; Insert drops the whole transcript (committed + live
  partial) into the note. (4) **Settings-card row labels were BLACK in
  dark mode** (`CurioSettingsCard`): the card fill is a CUSTOM lerp, and
  the uncolored row titles rely on `LocalContentColor` — the default
  `contentColorFor(customFill)` resolved black on the near-black card,
  making every row label invisible (subtitles were fine — they set
  explicit `onSurfaceVariant`). `CurioSettingsCard` now pins
  `contentColor = MaterialTheme.colorScheme.onSurface` (dark plum in
  light, cream in dark), fixing Profile, the Settings hub, every settings
  sub-page, Support, Updates, Experiments, Quests, Backup and Onboarding
  cards in one edit.
- **v131 follow-up — bigger nav pill, solid indicator + the dim-flash fix.**
  (1) **The floating pill bar grew a little:** pills 48→52dp (height +
  resting icon width), the expanded active pill 96→112dp, icons 22→24dp,
  row padding/spacing 6/4 → 7/6dp; the tab pages' pill clearances grew
  with it (Home 92→100dp, Spin/Cabinet 76→84dp). (2) **The active
  indicator is now a SOLID `secondary` fill with `onSecondary` ink** (the
  v27q selection contract — never a translucent container) instead of the
  washed `secondaryContainer` overlay, so the active tab reads as a
  defined amber pill in light AND dark; the wide `CurioNavigationRail`
  uses the same pair (selected icon/label `onSecondary`, indicator
  `secondary`). (3) **Dim-flash fix (root cause):** v129 removed the
  Scaffold, which used to paint `colorScheme.background` behind the
  content — the root Box became TRANSPARENT, so the window's dark-navy
  bootstrap color (`curio_deep_plum` #081B33 in themes.xml) showed
  through the NavHost page transitions mid-fade (every tab crossfade and
  push/pop dimmed toward navy). The root Box now paints
  `MaterialTheme.colorScheme.background` again (invisible in practice —
  every page paints its own full-bleed background; it only shows during
  transitions and wide gutters). LESSON: when a Scaffold is removed, its
  `containerColor = colorScheme.background` fill must be replaced
  explicitly or the window bootstrap color shows through fades.
- **v132 — Topic Reveal: no Scaffold band + floating Like/Dislike pill +
  tags under the hero + ticket-matching hero pills.** (1) **The reveal's
  bottom band is GONE** — `RevealBottomBarHeight` (TopicRevealScreen's
  80dp strip) and the NavHost's matching `RevealBottomBarPlaceholderHeight`
  reservation were both removed; the reveal now runs full-bleed like the
  tabs (the old reserved slot made the page stop 80dp short). (2)
  **Like/Dislike now ride a floating capsule** mirroring the bottom nav's
  pill bar (raised `surfaceContainerHigh` + 6dp shadow,
  `navigationBarsPadding` + 12dp air gap): it HIDES while scrolling down
  and slides back in on scroll-up — direction tracked via `snapshotFlow`
  on the page's `ScrollState` (3px dead-band), and it stays hidden in
  Browse-Topics mode (read-only; the old band's `browseMode` gate
  preserved). (3) **Tags moved back into the scroll body**, directly below
  the hero (hero → tags → actions → teaser → prompt); the action row drops
  to a 16dp gap when tags are present, else keeps the old progress-pill
  clearance. (4) **Hero pills now match the Spin ticket EXACTLY**: the
  action badge, byline and subtype swap the old opaque `pillGlass` for the
  ticket's `ink.copy(alpha = 0.18f)` recipe, so the reveal hero reads as
  the same card in light AND dark (the frosted glass read as white blobs
  next to the ticket's subtle tint). LESSON: the reveal hero and the Spin
  ticket morph from the SAME shared element — keep them on ONE pill
  recipe or the morph visibly changes color mid-expansion.
- **v116 — CI compile fix: Kotlin NESTED block comments.** A KDoc in
  `UpdatesScreen.kt` contained the literal sequence `-/*` ("# headers,
  -/* bullets"): Kotlin block comments NEST (unlike Java), so that `/*`
  opened a nested comment, the block's own closing `*/` closed the NEST
  instead, and the outer KDoc stayed open to EOF — swallowing the trailing
  `private enum class UpdateCheckUi` / `UpdateDownloadUi` declarations and
  the `ReleaseNotesBlock` helpers, which surfaced as dozens of
  "Unresolved reference" errors + "Syntax error: Unclosed comment". The
  KDoc is reworded (and now carries a NOTE). LESSON: never write a
  slash-star pair inside a block comment/KDoc in this codebase; if you
  must describe it, spell it as words ("slash-star").
- **v116 — Support & diagnostics page: Updates moved to the END and
  de-duplicated.** The "Updates" section was the FIRST section with a
  card header (Download icon + "Updates / Your build and what's new")
  AND an "Open Updates" row (Download icon) — three "Updates" entries on
  one page. The section now sits LAST (after About Curio) and keeps ONLY
  the Version readout row (five taps → Experiments); the card header and
  the "Open Updates" row are gone (the update flow lives on the dedicated
  Updates sub-page, Settings → Updates). Dead `FontWeight` import removed.
- **v114 — CurioIcon glyphs stay vertically centered (no bottom cut) at every
  font scale.** Root cause was the icon Text's `includeFontPadding = false` +
  `LineHeightStyle.Trim.Both` + `lineHeight = 1.0em` combo: trimming the
  line below the font's NATURAL 1.2em box, plus the trim's int rounding,
  dropped the baseline ~2dp below the icon box — every glyph sat low and
  its ink bottom was sliced by clipped button shapes (worse at large system
  font sizes, where the cut was most visible). The Material Symbols font's
  ink is designed to be centered in its natural line box (glyph ink spans
  +0.04em..+0.96em above the baseline; the baseline sits at 1.1em of the
  1.2em box), so the fix keeps the fontScale compensation (`size / fontScale`)
  and restores the natural box: `PlatformTextStyle(includeFontPadding = true)`
  (the default — the platform font padding stays) and NO line-height trim, so
  `lineHeight = 1.0em` acts as a minimum. The 1.2em natural box centers the
  ink in the icon's layout box with ~1dp margin on both sides. See
  CurioIcons.kt for the full derivation; DO NOT reintroduce the old
  trim/padding combo.
  **v115 — small spot corrections:** the natural box centers the ink, but
  a few glyphs' OPTICAL weight still reads a hair low inside compact
  circular/pill buttons, so those spots carry their own tiny lift:
  Home's `TopBarPill` (Menu + Person, -2dp), the drawer's `DrawerNavItem`
  chips (-1dp), Profile's `ProfileSearchPill` magnifier (-2dp), the
  `SettingsNavCard` cog (-2dp) and `CurioBackButton`'s chevron (-1dp,
  covering the Profile + Settings sticky/hero back pills). The first
  pass (-1/-1.5dp) was still a touch low, so the Profile/Settings spots
  deepened by another 1dp. Keep these per-site; do not re-add a global
  draw-time lift to `CurioIcon` (the old 1dp `graphicsLayer` lift was
  removed in v113 because it pushed near-top-bearing glyphs into clipped
  parents).
- **v115 — settings/profile option icons are BARE (no colored chip box),
  and the settings sub-pages render their options inside the shared
  settings card.** (1) **Icons: `CurioSettingsRow` and `CurioCardHeader`
  dropped the coral/rose tinted chip box behind their glyphs** — the
  icon now renders bare at 21dp in the hero-matched accent ink
  (`settingsCardAccentInk`), the same treatment `CurioSettingsInfoRow`
  always had, so every settings row/header reads as a clean option, not
  a colored block. Profile's `SettingsNavCard` cog also lost its blue
  gradient block (bare accent cog). (2) **Sub pages: the flat rows that
  used to sit transparently on the watermark backdrop now live inside
  `CurioSettingsCard(shadowElevation = 0.dp)`** — the four section pages
  (Appearance / Preferences / Recording / Backup & restore via
  `SettingsPageContent`, which also card-wraps the two-pane hub's right
  pane), plus Experiments, Backup tools, Support & diagnostics and the
  Updates sub-page. Every settings destination now uses the same
  row-in-card language as the hub; the Promo preview page keeps its
  showcase cards (previews + share buttons, not option rows).
- **v115 — Updates page redesigned: status header + saved release notes +
  markdown-lite rendering.** (1) **The last successful check is CACHED**
  (`AppPreferences.getCachedUpdateInfo` / `setCachedUpdateInfo` — tag,
  notes, htmlUrl, apkUrl): opening the Updates page shows the saved notes
  INSTANTLY (no network reload on every visit); the auto-check still
  refreshes silently in the background (`runCheck(keepResult = …)` keeps
  the saved result visible while checking) and only replaces it on
  success — a failed refresh keeps the saved result and marks the row
  "Couldn't refresh · tap to retry". The fetch path saves every
  successful result, so after updating, the notes for the installed
  version show. (2) **Page design:** a status header (accent status dot
  tinted by state — rose download / sage check / error — headline +
  subline + a `v{version}` chip) replaces the plain version row; the
  update action lives in an accent-tinted banner (Update now CTA,
  download progress, retry states, Open release on GitHub); notes move to
  their own "What's new" card. (3) **Release notes render markdown-lite**
  (`parseReleaseNotes`/`parseInline`/`ReleaseNotesBlock`, hand-rolled —
  the project has no markdown dependency): `#/##/###` headers, `-`/`*`
  bullets with accent dots, `---` dividers, `**bold**` spans,
  `[label](url)` links stripped to labels and `` `code` `` to plain text —
  the raw GitHub body no longer shows as literal markdown.
- **v115 — detail-screen tear runs out cleanly at the screen edges; the
  quick-fact box is theme-aware; the fold toggle is a bare "…".** (1)
  **Tear corners:** the seeded torn bottom edge (waves + noise + the tilted
  slant, which alone drifts ±up-to-10dp between the corners) could notch
  the hero's corners AT the screen's left/right edges and read as "cut" —
  worst on the edge-to-edge detail hero, whose seed is the entry hash, so
  some entries drew a deep up-bite at a corner. `buildSoftTornPath` and
  `buildSoftSheetPath` now fade the displacement to ZERO over the last ~5%
  at each end (the SAME fade in both, so the hero and its white under-sheet
  stay pixel-aligned): the middle 90% keeps its full torn character and the
  corners meet the nominal edge — applied to every torn hero (Home,
  Profile, Settings, Cabinet, Detail) since they share the shape. (2)
  **Quick-fact box** (EntryDetail `QuickFactCard`): the old translucent
  white @38% plate washed out against the tinted page wash in light and
  glowed like a bright sheet in dark — now a theme-aware OPAQUE plate
  (`lerp(surfaceContainerLow, categoryInk, 0.06f)` light /
  `lerp(surfaceContainerHigh, ink, 0.10f)` dark) with a hairline category
  rim in dark mode, matching the settings-card language. (3) **Fold
  toggle:** the "…more" / "…less" words are gone — collapsed shows a lone
  "…" affordance; expanded turns it `Color.Transparent` (the tap target
  stays, same size) so tapping the same spot still folds it back.
  crisp.** (1) **Eyes stopped flicking at the start of every scroll:**
  `PetPointer.trackerModifier` aimed the eyes on `Press`, so every
  touch-scroll began with a visible eye-snap at the finger's touchdown
  point (the 8dp drag-cancel only fired after the finger moved). The
  tracker now only REMEMBERS the press point and commits the aim on
  `Release` when the gesture was a clean tap (no drag) — via `position`
  (the hover path), so the sprite's existing 2s look-timeout fades it
  back to neutral. A scroll's release arrives with `pressStart` already
  nulled by the drag-cancel, so scrolling never aims the eyes. Mouse
  hover + click still work. (2) **Scaled eye pixels were glitchy:** the
  v71 draw-space eye scaling (`DrawScope.scale` at 0.72/1.0/1.35)
  rendered each eye cell as a fractional-size rect on a fractional grid
  → cells landed between device pixels → misaligned "glitchy lines".
  `CurioPetSprite` now draws the scaled eye on an INTEGER device-pixel
  lattice: each cell is `round(scaleF × opx)` px, snapped to the lattice
  around the eye's center (4.5/7 and 10.5/7) — crisp at every preset,
  and at `scaleF = 1` it lands exactly on the face's pixel grid.
- **v114 — remaining stock M3 buttons converted to the pill language.**
  Sweep of `Button`/`OutlinedButton` call sites that still used boxy
  default corners/colors next to the custom pill family: the tour's
  Skip/Next controls (16dp → full `RoundedCornerShape(50)` capsules in
  the tour bar), the crash screen's three `OutlinedButton`s (16dp →
  24dp, matching the Mix-button language), FieldMind's Finish/Save
  buttons (stock 20dp + primaryContainer → pill 50 / 24dp with theme
  primary fill + `onPrimary` icon/text — the old hardcoded white icon
  clashed with the default fill), and the Sound Bite trim pair (Keep
  full + Apply Trim, 16dp → 24dp, kept as a matched pair). Intentional
  exceptions: dialog `TextButton`s (`curioDialogActionButtonColors`),
  themed `RadioButton`s, the M3 `SegmentedButton` control in Settings,
  and self-contained flows (onboarding 18/26dp, bug-report 28dp) keep
  their own language.
- **v114 — mixed-deck colors: vivid blends (no near-black mud) + a smooth
  hero gradient (no band "lines").** The user flagged the green/teal,
  magenta/purple and blue mixes as bad (the red/coral mixes were fine).
  Root cause: the curated pair/triple blends had been "deepened until they
  clear 4.5:1 against white", which pushed several to near-black mud
  (Rose+Teal 0xFF4A12A8 ≈ 11% lightness, dark blues, dark teals) — while
  the reds the user liked were never over-darkened. The decks never needed
  the white-contrast deepening: the peek cards deepen each stop per-card
  (HSL lightness drop for the reel hierarchy) and use same-hue deep ink
  ([pastelFillInk]'s light branch); the hero's white ink rides the
  theme-resolved gradient like every single deck. Fix 1: retuned the
  flagged pair/triple blends to vivid, clean mid-tones in the same hue
  families (violet 0xFF8B5CF6 / fuchsia 0xFFC026D3 / blue 0xFF2563EB /
  jade 0xFF0BA36D / teal 0xFF0FA3A3, etc. — Tailwind 500/600-style
  shades; contrast ≥ the reds' 1.2–1.3). Fix 2 — "don't use gradients
  with lines": `mixedDeckGradient` previously emitted accent → curated
  seam → accent stops, which painted visible STRIPES on the hero card.
  It now theme-resolves each accent and OKLab-interpolates ~7 fine steps
  between consecutive accents ([oklabGradientStops]), so the hero's
  diagonal / reversed / radial brush (kept — the user wants rounded /
  random styles) glides smoothly with no band lines. 4+ accents still
  fall to [oklabCentroid]. The object docstring's stale "every blend
  clears 4.5:1 against white" claim was corrected.
- **v114 → v115 — mood board: the two editing/save flows stay SEPARATE
  (v114's merge was reverted) + a Copy board button that copies the
  INLINE board into the full-screen editor.** The format keeps the v57
  dual arrangement: the inline board edits `tiles`/`quoteCards.positions`
  (saved to `tileLayouts`/`quotePositions`), the full-screen editor edits
  its OWN `fullTiles`/`fullQuotePositions` (saved to
  `tileLayoutsFull`/`quotePositionsFull`) — arranging full-screen does
  NOT change the small card, and the two views start identical only until
  one is rearranged. v114 briefly merged both canvases onto one shared
  list; that broke the user's intended flow ("it was two different
  editing and save flow — you merged it again"), so it was reverted to
  the separate-list design. The full-screen dialog shows a **Copy board**
  pill (BottomEnd, above Add images) when the full-screen board is empty
  but the inline board has content: it copies the inline tiles into
  `fullTiles` and the inline quote placements into `fullQuotePositions`
  (index-aligned; text/style/tilt/width stay shared) — "copies what the
  outside board had on inside".
- **v114 → v115 — dark-mode pill glow hugs the pill shape WITHOUT
  shrinking to the middle.** The One UI dark-mode pill treatment
  (`curioGlassEdge` top catch) was painted against the pill's BOUNDING BOX,
  so on capsule pills (50-radius) the bright top band crossed the pill's
  curved ends and read as "the shadow peeking out from behind the pill".
  (1) **`curioGlassEdge` (CurioGlassEffects.kt) now clips its FULL-WIDTH
  vertical-gradient band to the pill's own outline** — the pill's curved
  rim trims the band at the rounded ends, so the catch covers the WHOLE
  button edge-to-edge and stays inside the shape. (v114 first tried an
  inset capsule mask — 10% side insets, 55% band height — which made the
  glow visible ONLY in the middle, so v115 dropped the mask and let the
  pill outline do the trimming; the non-subtle option's bottom whisper is
  the single gradient's final stop.) (2) **`curioInnerGlow` keeps its
  radial inside the pill's curved rim** (the pill outline clips anything
  crossing the capsule). (3)
  **Capsule-pill call sites switched from `categoryEdgeShine` (a
  full-width band that crosses the curved ends) to the shape-matched
  `curioGlassEdge`:** the Category Picker's Original/New `PickerPageTab`,
  the `PickerPresetChip` quick-mix chips (both also gain `curioInnerGlow`
  accent 0.12, matching the Spin filter-chip family), the picker's Mix
  button, and the reveal's `RevealAlreadyButton`. Cards with modest corner
  radii (Start-exploring 24dp, topic/settings/hero cards) keep
  `categoryEdgeShine` — it reads as a proper edge there.
- **v113 — resume-draft take fix + filter-sheet Apply pill family.** (1)
  **Resume draft restores the draft's OWN take:** `SaveCaptureScreen`'s
  single-section init seeded a resumed draft into a `defaultFormat` section
  (`sectionEntryFormat ?: defaultFormat`), so resuming a draft written on a
  non-default take (e.g. a Journal draft on a SoundBite-default category)
  opened the default take's body with the wrong data and the draft looked
  lost. The generic branch now uses `sectionEntryFormat ?:
  formatOf(sectionInitData)` — edit mode keeps the saved entry's format,
  new-capture resumes use the draft data's own format (the existing
  Portfolio/OpenNotebook branches were already format-correct). (2)
  **FilterSheet "Show all / Apply" pill matches the chips:** the bottom CTA
  was a flat Material `Button` next to the raised chip pills; it's now the
  same family as `CompactChip`'s selected state — `Surface(onClick)` with
  `RoundedCornerShape(50)`, solid `themedButtonFill()` accent, 4dp
  elevation + `curioDarkGlow`/`curioGlassEdge`/`curioInnerGlow(accent
  0.12)` + clip, and the chip's 18sp ExtraBold label / 19dp glyph / 20·13
  padding.
- **v113 — detail pill de-dupe, filter-page icons, icon glyph clipping,
  pet-designer hero mid-screen float.** (1) **Detail hero: the explore-
  session duration no longer duplicates** — the "explored 12m" pill above
  the Date · Mood · Session · Type card was removed; the Session segment
  inside the stat card is the single source. (2) **Spin filter sheet:
  inactive group-pill glyphs visible in light mode** — `FilterGroupPill`
  tinted the closed pill's glyph with the raw `accent`, which in pastel
  LIGHT resolves to an airy pastel and vanished on the 22%-accent fill;
  light + pastel now uses `pastelFillInk(accent)` (deep same-hue ink,
  L≈0.24), dark + non-pastel keep `accent`. (3) **CurioIcon glyphs no
  longer clipped at the top in buttons** — the 1dp `graphicsLayer
  { translationY = -1dp }` "optical lift" drew the ink 1dp ABOVE the
  icon's layout box; the Material Symbols font's line box is 1.2em (hhea
  ascent 1056 + descent 96 vs 960 upem), and near-top-bearing glyphs
  (timer, auto_awesome, sparkle tips — 40/960-unit bearings) sat exactly
  at the box top, so any clipped parent (every M3 Surface with a shape
  clips) sliced the glyph's top. The lift is removed; glyphs render
  centered per the font's design bearings. (4) **Pet Designer hero no
  longer floats mid-screen** — the v109 scroll-away hero translated by
  `-viewportStartOffset`, but `viewportStartOffset` is the viewport start
  in CONTENT coordinates (negative by the top content padding at rest), so
  at rest the hero sat ~`SettingsHeroTotalHeight` down the screen. The
  translation now adds `layoutInfo.beforeContentPadding` (the same top
  padding): `-(viewportStartOffset + beforeContentPadding)` = 0 at rest,
  -S when scrolled — the hero pins to the top and rides up 1:1.
- **v113 — mood board inline editor: full-card placement + quote-card
  fixes.** (1) **Tiles can be dragged anywhere in the visible card** — the
  drag, pinch-resize, grow and commit clamps were the FROZEN collage extent,
  but the centered fit leaves an empty band above the collage, so an image
  couldn't be dragged "all the way up". Clamps are now the FULL card
  (display [0, canvas] mapped back through the fit: raw ∈
  [(0-offset)/scale, (canvas-offset)/scale − size] — negative raws allowed).
  The fit stays frozen (v108), so the zoom never jumps mid-drag; the saved
  views (MoodBoardTiles' offset clamp, EntryDetail's inline fit +
  fitTileLayout) now allow the same negative offsets so a band-placed photo
  renders in the same spot in edit and detail. (2) **Quote cards can no
  longer be dragged off the card** — MoodBoardFloatingCards passed boardW/H
  = canvas × scale to the drag/resize clamps, so on a zoomed (scale > 1)
  fitted board a card could be dragged past the bottom/right edge and its
  committed position re-rendered inconsistently ("glitching when I take it
  to the top"). The clamps now bound the VISIBLE canvas (boardW/H =
  canvasWPx/HPx). (3) **Never-dragged quote cards land ON the collage** —
  the deterministic slot was computed from the DISPLAY canvas and then
  scaled AGAIN (double-scale: display = canvasSlot × scale), so a fresh card
  in a fitted board rendered off the collage and the second slot fell below
  the board. MoodBoardFloatingCards now takes boardMaxX/Y (the collage's
  RAW extent) and computes the slot in RAW space (display = raw × scale +
  offset); callers pass the frozen extent (inline editor), the saved
  layouts' maxX/maxY (saved card), boardW/fit.scale (expanded dialog) and
  maxX/maxY (export).
- **v113 — new COSMIC launcher icon + 1.1.0 version bump.** (1) **Icon:**
  the user-supplied art (first `svgviewer-output (3).svg`, then — after the
  hand-converted VECTOR was rejected as "broken / not properly placed" — the
  designer's `svgviewer-output (3).png` RASTER, 2048×2048, archived at
  `design/launcher-icon/curio-launcher-icon.png`) replaces the old angular
  open-portal mark: a mint planet with a pink moon over layered pink/gold
  waves on a midnight navy→magenta sky, inside a rounded card with a white
  frame. The art is used as the PNG DIRECTLY — no vector conversion.
  `drawable-nodpi/ic_launcher_art.png` = the raw bitmap; `ic_launcher_foreground`
  = an `<inset android:inset="28dp">` around that bitmap (the card spans
  ~84–88% of the raw canvas, so full-bleed the launcher mask sliced the
  frame/top stars/bottom waves; inset 28 → the card renders at ~44×47dp,
  fully inside the 66dp safe circle, floating on the sky);
  `ic_launcher_background` = the full-bleed sky gradient + stars (its
  gradient matches the card's own sky so the two composite behind the
  frame); `ic_launcher_monochrome` = planet + moon silhouette;
  `ic_notification` = the same mark at 24dp. The splash (SplashScreen.kt)
  renders `@drawable/ic_launcher_art` directly (the full card, not the
  inset foreground) so the splash logo box stays full-size. (2) **Version:**
  `versionName` 1.0.1 → **1.1.0** and
  `versionCode` 20260919 → **20260920** (the bump missed on the previous
  feature releases); the store changelog moved to
  `fastlane/.../changelogs/20260920.txt` (the 20260919 draft is gone).
- **v112 — Updates sub-page + opt-in update checker, auto-backup, top-right
  update pill.** (1) **Updates page** (`features/updates/UpdatesScreen.kt`,
  route `UPDATES`, Settings hub row + Support & diagnostics "Open Updates"
  row): settings-family torn-rose sub-page with the version readout, a
  Check for updates row, an animated result card (release notes preview /
  "Update now" download → system installer / Open release), and the
  **opt-in Update checker toggle** (`updateCheckerEnabledState`, default
  OFF — Curio is offline-first, so the background check that costs data
  every launch only runs when enabled; the manual check always works).
  The OLD update card in Support & diagnostics was REMOVED (the update
  flow lives only on the Updates page now); Support keeps the Version
  five-tap → Experiments diagnostic. The update notification + toast copy
  point to the Updates page. (2) **Update toast remade:** the in-app toast
  is now a SMALL pill in the TOP-RIGHT corner below the status bar
  (`CurioInAppToastHost` anchored `TopEnd` + `statusBarsPadding` in the
  NavHost root; compact 12/7 padding, 16dp glyph, slides down from above)
  instead of the old bottom-center pill; tapping it opens the Updates page.
  (3) **Auto backup** (BackupToolsScreen "Auto backup" section): opt-in
  toggle — the FIRST time it's switched on the user picks a save location
  once (`CreateDocument`, `takePersistableUriPermission` + persisted URI
  in `auto_backup_uri`); MainActivity then exports a backup there on app
  start, throttled to ~once per 24h (`AUTO_BACKUP_INTERVAL_MILLIS`,
  `auto_backup_last_at`). The section shows Backup location (tappable to
  change) + Last auto backup. (4) **"Last backup" row fix:** the row is
  driven by the export's `ExportResult.exportedAtMillis` (the exact write
  timestamp) instead of a stale prefs re-read, and BackupToolsScreen
  re-reads the backup timestamps on ON_RESUME — it no longer reads
  "Never" right after a successful backup.
- **v110 — pet designer scroll compiles + YouTube Music opens in-app.**
  (1) **CI compile fix:** `PetDesignerScreen` declared `val listState =
  rememberLazyListState()` INSIDE the `Column { }` content lambda, but the
  v109 hero overlay `Box` (which reads `listState.layoutInfo
  .viewportStartOffset` for the scroll-away translation) is a Column
  SIBLING — a val inside the lambda's scope is invisible to the sibling, so
  `compileReleaseKotlin` failed with "Unresolved reference 'listState'".
  The declaration moved UP to the outer Box scope (before the Column); both
  the LazyColumn (inside the Column) and the hero overlay (sibling) now
  resolve it. (2) **YouTube Music opens IN the app:** `openSearchUrl`
  (ExploreSearch.kt) now package-PINS every `https://music.youtube.com/`
  URL to `com.google.android.apps.youtube.music`
  (`Intent.setPackage`) — the YTM app's App Links verification for that
  domain is unreliable (many devices hand the URL to Chrome, so the
  "Listen in" pill opened the browser), and package-scoped delivery
  bypasses verification, landing the search in the YTM app. When the app
  isn't installed (no handler for the pinned package) the plain https
  intent opens the browser instead.
- **v108 — dark-mode chip glass for pills & search bars; hero texts use
  cream ink; under-sheet opt-out; detail buttons blend.** (1) **YouTube
  logo un-squished:** `ic_music_youtube.xml` had a 28.57×20 viewport in a
  square 24dp box — VectorDrawable maps non-uniformly, so the logo drew
  vertically stretched. The art now sits centered in a square 28.57×28.57
  viewport (group translateY 4.285) — uniform scale, ~3dp breathing room.
  (2) **Dark pills = filter-chip glass:** the explore dialog's "Explore" +
  "Watch in" pills and every hero action pill (`SettingsHeroActionPill`,
  `CabinetHeroActionPill`, and the settings-family back pills in
  `SettingsHeroHeader` + Topic History) swap their bright white-lift glass
  for the CompactChip dark raised glass
  (`lerp(surfaceContainerHigh, Black, 0.15)`) in dark mode — light keeps
  the v93 frosted glass; the explore pills also gain the chip's 4dp lift +
  `curioDarkGlow`/`curioGlassEdge`/`curioInnerGlow(accent 0.12)`. (3)
  **Search-bar audit (dark):** every hero search field (Cabinet hero,
  `SettingsHeroHeader`/Topic DB, Spin filter sheet) now resolves its fill
  through the new shared `curioSearchFill(backdrop)` — light lifts toward
  white as before, dark drops to the chip near-black glass (the old
  `lerp(bannerFill, White, 0.30)` landed a muddy mid-tone on the dark
  banners/black sheet). (4) **Hero banner titles use cream ink in dark:**
  the Spin filter sheet ("Discoveries" + "Pick what you're in the mood
  for") and the category picker sheet ("What are we\nExploring?") now
  resolve `filterHeroInk`/`pickerHeroInk` via `heroHeaderInk()` instead
  of `onAccent()` — dark pastel mode was painting the tinted LIGHT twin
  as title text over the deep banner; dark now reads the same
  cream-white the Cabinet/Home hero titles use. (5) **"Torn hero
  under-sheet" experiment (default OFF):** new `heroTearSheetState` pref
  (Settings → Experiments → Paper & headers) — with it off, every torn
  hero tears straight into the page (only the hero's own bottom tear +
  hairline rim); ON restores the white paper lip. Gated in all 11
  under-sheet call sites: Home quest + drawer, EntryDetail, Spin filter +
  picker sheets, Cabinet, Settings hero, Profile, Topic History,
  Onboarding, PromoMode. (6) **Detail back/more frost blends with the
  hero:** `DetailStickyBar`'s dark frost was `lerp(heroFill, Black, 0.30)`
  (a near-black slab); it's now `lerp(heroFill, White, 0.10)` — the same
  hero-hued lip the under-sheet wears, so the controls read as part of
  the banner. Light frost unchanged. (8) **Pet designer hero tear is now
  SCROLLABLE, not sticky:** the `SettingsHeroHeader` overlay translates up
  1:1 with the list (`Modifier.graphicsLayer { translationY =
  -listState.layoutInfo.viewportStartOffset }` — viewportStartOffset is
  the total scrolled pixels, monotonic through the sticky toolbar, so the
  hero rides away with the content and never jumps back) instead of
  staying pinned while rows slide under the seam. The LazyColumn gained a
  `rememberLazyListState()`; the sticky studio toolbar now wears
  `statusBarsPadding()` + the theme background so it pins BELOW the
  status bar — before, it pinned at the viewport top and was invisible
  behind the opaque hero (sticky headers pin at the viewport top, never
  under the hero). The hero keeps its full-bleed tear; content padding
  (`SettingsHeroTotalHeight + 8`) unchanged.
  (7) **Profile XP-progress block
  leaves the paper stat card:** the v97 "quests & achievements wears the
  shared paper card" is REVERTED — `ProgressAndAchievementsCard` ("XP
  progress" + quest list + badge preview) is not a stat bar, so it always
  renders on the plain `CurioSettingsCard` (no paper fill, no torn edges,
  no holes/rings). The paper style stays on the real stat panes: Home
  Streak · Cabinet · Topics, the hero's Level · Saved · Lanes, the detail
  meta card. Dead quests-paper locals removed
  (`questsPaperOn/Bg/TearOn/Shape/HolesOn/RingsOn/RingStyle/Content` +
  the unused `settingsCardTintLift` import); `paperStatCard*` /
  `TornStatPaperShape` imports stay (the hero stat pane still uses them).
  (9) **"Watch in" → "Listen in" for audio services + music picker loses
  its radio.** The explore dialog's second pill label is now dynamic:
  music topics (Album/Artist/Song) whose chosen service is NOT YouTube
  (Apple Music / Spotify / YouTube Music — audio) say "Listen in";
  YouTube and non-music topics keep "Watch in" (video). The `MusicService
  Dialog` row no longer draws a `RadioButton` — selection reads through
  the row's solid v27q fill alone (the other picker dialogs in the file
  keep theirs); the dialog subtitle was reworded to stay neutral
  (  "Which streaming service opens albums, artists and songs from the
  explore dialog"). (10) **Inline mood board: STABLE fit + quote cards
  bounded.** Two glitch fixes in the inline (small) editor. (a) **The
  board fit is FROZEN once content exists** (`MoodBoardCanvas` in
  GalleryWallFormat.kt): the v69 live re-fit recomputed `boardMaxX/Y`
  (and thus `boardScale`/`boardOffsetX/Y`) from the CURRENT tile
  bounding box on EVERY commit — drag a photo inward and the extent
  shrank, the board zoomed in and every tile + floating quote card
  visibly jumped ("the size changes when I move / expand / shrink
  photos" glitch). A `sessionExtentX/Y` (`remember`ed floats) now
  freezes at the first content's bounding box via a grow-only
  `LaunchedEffect(tiles.size)`: every commit (drag, pinch, grow, add)
  clamps tiles INSIDE it, so no gesture can exceed it; the freeze
  resets to 0 when the board empties so a fresh board re-freezes at its
  own size. The saved card re-fits to the final saved layouts, which all
  live inside the frozen extent, so edit and detail still agree. (b)
  **Floating quote cards are width-capped in every fit-scaled view:**
  the v60 display cap applied only to never-resized cards — a card the
  user RESIZED kept the full scale and ballooned past the small board
  when the fit zoomed in (scale > 1), and couldn't shrink small enough
  ("the quote card is too big"). `MoodBoardFloatingCards` gained a
  `rawSpace: Boolean = false` param; when false (inline editor, saved
  card, expanded dialog, export) `displayScale` is now capped for ALL
  cards at `canvasWPx * 0.40 / cardW` (≤40% of the canvas — was 44%
  and only for slot cards); the full-screen editor passes `rawSpace =
  true` and keeps exact raw widths for precise placement. The resize
  grip's absolute floor dropped 60 → 48 render px so a capped card can
  shrink to a genuinely small note.
- **v107 — Apple Music "Watch in" deep links fixed for songs.**
  `resolveAppleMusicItemUrl` (ExploreSearch.kt) had two bugs that made
  SONG topics fail while artists and some albums worked (verified live
  against the iTunes API): (1) the old `music://music.apple.com/{cc}/song/{id}`
  deep link is a DEAD route — music.apple.com/song/{id} → HTTP 404 — a
  song's only canonical page is its ALBUM page with `?i=trackId`; the
  code now uses the API's own `trackViewUrl` / `collectionViewUrl` /
  `artistLinkUrl` with the scheme swapped to `music://` (tracking
  `&uo=4` stripped) and `country=$storefront` passed so the link matches
  the device storefront. (2) The search term never included the artist
  for songs (the teaser regex only ran for albums) and kept the raw
  `(1984)` year, which makes the API return ZERO results — the term is
  now `CurioTopic.byline` (the artist, present on all Album/Song topics)
  + title with the trailing `(YYYY)` stripped (new `TRAILING_YEAR_IN_PARENS`
  + `UO_TRACKING_PARAM` private regexes); teaser regex kept only as a
  blank-byline fallback. Non-Apple services (buildMusicServiceSearchUrl)
  untouched.
- **v106 — music brand logos in the explore dialog + music picker.** The
  four official service SVGs the user supplied (Apple Music, Spotify,
  YouTube, YouTube Music — archived under `design/music-service-icons/`)
  are converted to crisp VectorDrawables in `res/drawable/`
  (`ic_music_apple_music`, `ic_music_spotify`, `ic_music_youtube`,
  `ic_music_youtube_music`; Apple's diagonal pink→red→purple gradient is
  recreated as an aapt `<gradient>` because the source SVG embeds it as a
  raster JPEG, and the YouTube-Music ring uses `fillType="evenOdd"` so
  the center stays open). (1) New `MusicService.brandRes` extension in
  CurioIcons.kt maps each service to its drawable. (2) TopicRevealScreen's
  "Watch in" pill swaps the old Material glyph (`MusicService.brandTile`)
  for the service's brand logo via `painterResource` (NEVER tinted — the
  logos keep their own brand colors; the pill's `pillInk` tint applies
  only to the glyph pills); non-music topics use the YouTube logo. (3)
  MusicServiceDialog rows (Settings → Notifications → Music service) lead
  with the 26dp brand logo before the radio button. The `brandTile`
  glyph stand-ins stay as monogram fallback (no callers left in the
  dialog).
- **v105 — sort control removed + smoother category chips.** (1) SORT
  REMOVED from Cabinet + Topic Database: `ui/components/CurioSortDropdown.kt`
  is DELETED (no callers left) and both hero call sites drop it —
  CabinetScreen removes `cabinetSortField` / `sortAscending` + the sort
  `when` in `visibleEntries` (fixed newest-first
  `sortedByDescending { capturedAtMillis }`) + the private
  `CabinetSortField` enum; TopicDatabaseScreen removes `tdSortField` /
  `tdSortAscending` + the `sortMode` derivation (the rows builder keeps
  ONLY the default per-lane A–Z branch;
  `LaunchedEffect(sortMode, …)` → `LaunchedEffect(effectiveCat)`) +
  `DatabaseSortMode` / `DatabaseSortField` enums; dead `CurioSortOption`
  imports + `settingsRoseAccent` import cleaned. (2) SMOOTHER CATEGORY
  CHIPS: both chip bars slide in with a longer decelerating
  `LinearOutSlowInEasing` tween (380ms + 320ms fade; exit 300/220)
  instead of the snappy 300ms `FastOutSlowInEasing` snap.
- **v104 — detail hero tear = Home's exact construction.** The v92
  `detail = true` pattern (salted seed + 17π/23π mid-frequency meander
  octaves, added to stop unlucky entry hashes reading flat) made the
  detail hero's seam read as mechanical "straight lines then a tear" for
  many entries. Every other hero (Home, Profile, Settings, Cabinet,
  drawer, filter/picker sheets) uses plain
  `SoftTornBottomShape(seed, bold = true)` — so the detail hero + its
  under-sheet now drop `detail = true` too (EntryDetailScreen), making the
  construction byte-identical to Home (lip 10dp, baseline 14dp, bold;
  only the per-entry seed differs). The `detail` parameter machinery in
  PaperCard.kt stays as an unused public opt-in (left to minimize
  regression risk; CI validates the call-site change).
- **v103 — profile avatar photo + drawer.** (1) AppPreferences:
  `KEY_PROFILE_AVATAR` path pref + `getProfileAvatarPath` /
  `setProfileAvatarPath` ("" = none); the photo is copied into filesDir
  as `profile_avatar_<timestamp>.png` (a fresh name each pick so
  remember(path) bitmap caches re-key; old avatar files deleted). (2) New
  shared `ui/components/ProfileAvatar.kt`: `rememberProfileAvatar(path)`
  (ImageDecoder 28+ at 512px target, BitmapFactory 26-27, cached per
  path) + `ProfileAvatarImage` (fills the caller's circle-clipped box;
  nothing when unset → initial fallback). (3) ProfileScreen: the Edit
  profile dialog gains a 64dp circle preview (photo or name initial) +
  Add/Change photo + Remove (GetContent picker); the Profile hero's 72dp
  avatar circle shows the photo instead of the initial when set. (4) Home
  drawer (`HomeDrawerContent`): the bottom greeting row leads with a 48dp
  avatar circle (photo or initial) beside CURIO · Hi name · tagline.
  User confirmed (ask_user): always-on, no toggle.
- **v102 — auto-import bob actually plays.** The v64 auto-import added a
  "happy" bob animation to `design.animations` and saved it, but NOTHING
  played it — the animation editor is hidden from the Pet Studio (only
  PETS/EDITOR/SETTINGS pages) and the Pet Life routines never pick
  animationId "happy" (glance/wave/stretch/sidepeek/stumble/look_up/
  backturn/victory/inspect only) — so "auto animate on import" was inert.
  Fix: the auto-import now ALSO registers an IDLE-triggered
  `CustomPetAction` ("auto_bob", idle/8s, animationId "happy", no lines)
  on the built design; the floating pet's existing idle custom-action loop
  (CurioFloatingPet) then performs the bob whenever untouched for 8s (60s
  rest between fires). Re-imports are idempotent (the filter replaces the
  same id).
- **v101 — subtle top-only pill glow, as an option.** (1) AppPreferences:
  new `pillGlowSubtleState` (DEFAULT ON) + `KEY_PILL_GLOW_SUBTLE` +
  `isPillGlowSubtleEnabled` / `setPillGlowSubtleEnabled`, seeded in
  `initThemeMode`. (2) CurioGlassEffects.kt — both dark-only effects read
  the pref: `curioGlassEdge` (subtle) drops the top alphas 0.10/0.04 →
  0.05/0.02 and ends the gradient at 0.35 (the bottom whisper is GONE —
  top-only); `curioInnerGlow` (subtle) halves the strength and ties the
  radius to the SHORT side (`minDimension * 0.55` vs `maxDimension *
  0.95`) so the radial hugs the pill's top instead of filling it. Off =
  the original fuller gradient / pushed-in glow. (3) Settings →
  Appearance: "Subtle pill glow" switch (default ON; off restores the
  fuller glow for comparison). User asked for the option; default ON per
  the v97 paper-card precedent (the new look ships, toggle compares).
- **v100 — search-text audit + filter hierarchy.** (1) SEARCH TEXT: every
  search bar now resolves the THEME text color (`onSurface` — near-black
  light, near-white dark) instead of a colored ink: `CurioSearchField`'s
  default is onSurfaceVariant → onSurface, and the three hero/tinted call
  sites (Spin filter sheet, Cabinet hero, Settings hub hero) pass
  `MaterialTheme.colorScheme.onSurface` — the filter sheet's deep
  category-ink-on-category-glass washed out (same hue on same hue), the
  hero bars' banner ink sat ~3:1 on the whitened glass. (2) FILTER
  HIERARCHY (SpinScreen.kt): the accordion now has two levels —
  `FilterGroupPill` (closed) is a CATEGORY-TINTED glass
  (`lerp(chipSurface, accent, 0.22f)`, both themes) while `CompactChip` is
  NEUTRAL (callers dropped the category-tinted `cat.categorySurface(...)`
  → plain surfaceContainerHigh, light fill lifts toward `surface`); BOTH
  pills bump 3 → 4dp elevation + `curioDarkGlow(4.dp)` so the hierarchy
  reads via color AND lift. User confirmed via ask_user: theme text color
  + tinted groups / neutral chips / pill elevation.
- **v99 — compact update toast + delayed past launch.** (1) The in-app
  toast is now a ONE-LINE pill: `CurioInAppToastHost`'s text is capped
  (`maxLines = 1` + `TextOverflow.Ellipsis`) and the pill is slimmer
  (inner padding 18/12 → 16/10dp, host margin 24 → 20dp) so a long
  message can never balloon it. (2) The launch update toast no longer
  shows on the start screen: `UpdateChecker.notifyIfUpdateAvailable`
  waits 4s past the check before showing the toast (the once-per-version
  notification still fires immediately), and the copy is shortened to
  "Curio vX update available" (was the full "…is available — update in
  Support & diagnostics" sentence that wrapped to two lines on a phone).
  User confirmed (ask_user): compact + delayed past launch — NOT removed.
- **v98 — tear-catch revert + dark pill polish.** (1) REVERTED the v94/v95
  tear-hero light catches: `curioLightCatch` (light warm catch) +
  `tornSeamLight` (dark torn-edge stroke) are removed from CurioGlassEffects
  and the four hero call sites (Home quest banner, Detail hero, Spin filter
  + category-picker sheets); the stale v94/v95 doc/changelog entries are
  gone with them. (2) Home recents pills in dark: the FULL-PILL inner glow
  is gone — `ExploreTopicRow` / `RecentEntryRow` keep the colored pill +
  the `curioGlassEdge` top-edge white catch only (white no longer fills the
  whole pill). (3) Category picker Original/New tabs + preset chips:
  elevation flattened 3 → 2dp (the v27q selectable-chip standard) so the
  shadow no longer reads as a halo above the pill (`PickerPageTab` +
  `PickerPresetChip`; covers the full-screen picker AND the Spin sheet).
  (4) `CurioProgressPill` (Topic Reveal + Detail + Cabinet) is WIDER:
  horizontal padding 14 → 18dp so it reads as a proper pill instead of a
  slim strip with the glow/shadow around it. (5) Quests screen — quest
  paths are MINIMAL: closed `PathCard`s in the "Quest paths" grid drop the
  `chain.subtitle` task hint (it only appears in the open path dialog) and
  the BRONZE/SILVER tier label (just the medal badge + the chain name —
  the medal already carries the tier); the "In progress · n/target" pill
  in  `CurrentQuestCard` is a FULLER pill (9dp vertical → 12dp vertical +
  14dp horizontal — user confirmed it read too thin, the glow stays);
  the badge SHELF dialog tiles (`MergedBadgeTile`) are minimal too —
  earned tiles are just the badge + the name (BRONZE/SILVER label + its
  "· upgraded" chip are gone; locked tiles keep progress + "Secret ·
  hidden").
- **v97 — Paper stat card on by default + Profile quests paper card + merged
  Edit profile.** (1) The "Paper stat card" experiment PASSED — default is now
  ON app-wide (`paperStatCardsState` true, `KEY_PAPER_STAT_CARDS` default
  true; the Experiments toggles stay for comparison), so the Home stat bar,
  the Profile stat pane, and the Detail meta card wear the paper card by
  default. (2) The Profile quests & achievements block now wears the shared
  paper card too (same construction as the hero's Level · Saved · Lanes pane:
  `paperStatCardFill` on `paperStatCardColor(settingsCardTintLift())`, 28dp
  base shape, `TornStatPaperShape(0x6B4E3E)` under the tear toggle, holes /
  rings following the paper toggles, 3dp elevation + `curioDarkGlow`;
  falls back to `CurioSettingsCard` when the experiment is off). (3) The
  "Quests & achievements" plate lost its glowing look: `curioGlassEdge` +
  the frosted `curioPillTintLift` lift are gone (calm flat
  `lerp(surfaceContainerHigh, curioRoseInk, 0.08)` fill) and the gradient
  icon box is now a flat rose-tinted chip with rose-ink trophy (the
  `CurioCardHeader` icon-chip language). (4) "Edit profile" now edits BOTH
  the name and the tagline (the line under the name) in ONE dialog — the
  separate tagline dialog is removed; tapping the tagline opens the same
  Edit profile dialog, and Save persists both prefs.
- **v96 — detail-page fixes.** (1) CI: `tornSeamLight` (v94) used
  `1.5.dp.toPx()` without importing `androidx.compose.ui.unit.dp` — added.
  (2) Detail back + more buttons: the frost plate was HARDCODED white
  (cream ink on white washed out; glared on the black page).
  `DetailStickyBar`'s frost is now theme-aware — light: hero fill lifted
  toward the frosted glass (`lerp(heroFill, curioPillTintLift, 0.38)`);
  dark: a dark hero-tinted glass (`lerp(heroFill, Black, 0.30)`) so the
  cream ink reads (reversed light-in-dark contract). New `heroFill` param
  (caller passes `heroStart`). (3) Detail Date · Mood · Type meta card's
  default pane now uses the PROFILE stat-pane recipe (user's choice):
  `lerp(heroStart, White, 0.06/0.26)` instead of the near-white
  `heroSheetColor` blend; corner 18 → 20dp. Paper experiment path
  untouched.
- **v93 — One UI light glass + shiny ticket edge + tinted shadows.** (1)
  `curioPillTintLift()` LIGHT value: was `lerp(background, curioRoseInk(),
  0.08)` (cream) — now `lerp(Color.White, curioRoseInk(), 0.10)` (rose-
  kissed white), mirroring the dark value, so every hero pill / plate
  (Settings, Profile, Cabinet, Topic History, Spin filter chips, sort
  pill, quest plate) reads as the same bright frosted glass in light as in
  dark (the user's "profile/settings light like dark style" ask). All 11
  call sites are hero-pill fills — safe to lift globally. (2) Spin hero
  ticket + Topic Reveal morph card now carry `curioGlassEdge` (the One UI
  shiny top-edge rim, dark only) — both stay pixel-identical for the
  morph. (3) The hero ticket's accent-tinted layered shadow (the existing
  `heroShadowState` recipe) is now the SHIPPED DEFAULT — in-memory
  default + `isHeroShadowEnabled` pref default flipped to true; the
  Experiments toggle remains for comparison.
- **v92 — Home-clean tear heroes + hero shade family + cbrt fix.** (1)
  `kotlin.math.cbrt` (used by the v88 OKLab `toOklab`) was missing its
  import — added. (2) Detail hero tear: the old `detail = true`-only tear
  with a 3dp lip / 7dp baseline / 16dp sheet — plus a near-black
  (#121316) dark sheet — read as a straight cut on the black page. Now
  `bold = true, detail = true` (keeps the flat-seam salt), Home's exact
  geometry (`lip = 10, baseline = 14`, 42dp sheet at `HeroHeight − 18`,
  extent 16 → 24dp) and a visible dark lip (`lerp(heroStart, White,
  0.10)`). (3) Filter + category-picker sheet tears: both had only the
  torn banner + black hairline with NO white under-sheet, so the hero
  dropped straight into the wash — both now carry Home's full
  construction (`SoftTornSheetShape(same seed, lip = 10, baseline = 14,
  bold = true)`, 42dp at hero − 18, hero box + 24dp, dark lip =
  `lerp(heroFill, White, 0.10)`). (4) Hero SHADE family: the filter +
  picker heroes resolved from raw `themedAccent()` (uncalmed, brighter)
  while Home/Detail use `headerAccent()` (calmed/deepened banner shade) —
  both heroes now use `cat.headerAccent()` so every torn hero wears the
  same shade.
- **v91 — dynamic pills sweep + unified One UI search bars.** (1) Profile
  quest plate: the fixed solid coral (light) / deep rose (dark) fills are
  gone — the plate wears the shared profile-family frosted glass
  (`lerp(surfaceContainerHigh, curioPillTintLift(), 0.55)`, cream-rose in
  light / near-white rose glass on black) like the other profile options;
  the rose icon box stays (colored-icon-block pattern). (2) Quests
  "In progress" button: solid rose is only for the actionable "Start ·
  +XP" CTA; the informational state is a tinted surface glass with
  theme-aware rose ink + `curioGlassEdge` in dark. (3) Category picker
  shadow leak ROOT CAUSE: the tear's idle tab/preset fills were
  TRANSLUCENT (`pickerHeroInk.copy(alpha = 0.16f)`) so the 3dp elevation
  shadow bled through the pill — now OPAQUE ink-glass
  (`lerp(pickerHeroFill, pickerHeroInk, 0.16f)`); preset chips fuller
  (12dp vertical, glyph 16); tear height 184 → 208dp (the two-line title +
  tabs + presets were squished); full-screen picker preset row spacing
  4/1 → 8/2 and tabs 1/4 → 2/6; "Manage categories" link flipped from the
  pale scheme primary to theme-aware `onSurface`. (4) `CurioSearchField`
  is now THE One UI search component — fixed 46dp height (the hero pill
  size), 50dp capsule, ink hairline border, frosted fill + glass edge in
  dark, with new `ink`/`fill` params for heroes (banner ink + frosted
  category glass). The three duplicated hero `OutlinedTextField` searches
  (Cabinet hero, Settings/Topic-DB hero header, Spin filter sheet) now
  route through it — every search bar in the app is one size + style,
  dark-correct via resolved ink/fill. Dead OutlinedTextField/IconButton/
  KeyboardOptions imports removed from the swapped files.
- **v90 — sort pill rebuilt as a category-style labeled pill.** The v85
  compact icon-only blob (glyph + divider + arrow, ~55dp) hid the sort
  label in the menu header and read as a lone glyph chip next to the
  LABELED Category pill ("Category · All"). `CurioSortDropdown`'s pill now
  speaks the Category pill's exact language (CabinetHeroActionPill /
  SettingsHeroActionPill): sort-type glyph 20dp + field label
  (`labelLarge` Bold ink) + 1dp divider + direction arrow 20dp, same 46dp
  height, 14dp edge padding, 13/13 zone padding (46dp tap height), the
  same frosted fill + dark glass glow. Glyph+label = one tap zone opening
  the dropdown; the arrow zone still toggles asc/desc. The label is back
  on the pill ("Date"/"Title"/"Category"/"Default"/"Name"/"Year"). One
  shared component covers both call sites (Cabinet + Topic Database).
- **v89 — Home recents rows get the dark pill style.** `ExploreTopicRow`
  (recently explored / unexplored) and its compact sibling `RecentEntryRow`
  still chained the RETIRED no-op `curioDarkGlow` (identity, draws
  nothing) — so on the pitch-black page the only shadow was the Surface's
  black `shadowElevation` (invisible) and the rows read flat. Both now
  wear the same dark pill recipe as the filter chips:
  `curioGlassEdge(shape)` (the 1% whitish top-edge catch) +
  `curioInnerGlow(shape, themedAccent, strength = 0.12f)` (the accent's
  light twin pushed in from the top-left, clipped to the 20dp shape),
  dark-only no-ops in light. Other `curioDarkGlow` sites (stat cards,
  session card, stop button, tag chips) left as-is — not the recents pill
  family.
  **v115 — the HOME recents rows are NOT category-tinted in dark mode**
  (user: "bring back the dark mode home screen recents not being
  colored"): `RecentEntryRow` + the Home `ExploreTopicRow` now use
  `surfaceContainerLow` instead of `categorySurface()` when
  `isCurioDarkTheme()` (the recents PAGE — RecentScreen — keeps its
  tinted rows).
- **v88 — dark-mode mixed colors fixed at the root (the "mixed colors are
  bad" bug).** The curated `PairBlends`/`TripleBlends` tables are keyed on
  the RAW researched accents, but the Spin caller pre-resolved every
  accent to its theme shade (`themedAccent()`) — so in dark mode every
  table lookup missed and mixes silently fell back to the HSL midpoint /
  circular-hue centroid (foreign-hue swings, muddy olive midpoints).
  `mixedDeckAccent`/`mixedDeckGradient` now take the RAW accents
  (`CurioCategory.accent`) and resolve per theme inside: dark blends wear
  the same `darkAccent` "new shade of the same spectrum" recipe as the
  singles; dark-pastel seams resolve to the MUTED deep pastel (the old
  hardcoded `pastelAccent(seam, false)` left airy LIGHT seams on dark
  pastel decks). New local OKLab machinery (`toOklab`/`fromOklab`,
  `oklabBlend`, `oklabCentroid`, `oklabGradientStops` — canonical
  Ottosson matrices, version-proof like `toHsl`) replaces the HSL blend /
  centroid fallbacks (perceptual mean) and the Spin + Reveal ticket
  crown→base stops (perceptual interpolation; both screens stay
  pixel-identical for the morph). `darkAccent` research-tuned:
  near-grey neutrals (s<0.22) keep identity (no ×0.80 grey-out); the
  saturation cap scales with source saturation (0.48+s·0.16, ceiling
  0.62) so vivid families hold chroma at depth; the lime/yellow-green
  band (55°–95°) remaps onto emerald (95°→150°) so dark limes read as
  deep greens, never olive. Dead private HSL helpers removed
  (`hslBlend`/`hslCentroid`/`steerLightness`/`contrastVsWhite`/
  `toLinear`); `hslGradientStops` kept as documented public API.
- **v87 — missed dark-mode spots sweep.** (1) Spin filter sheet chips +
  group pills: `inactiveFill` stayed `lerp(chipSurface,
  curioPillTintLift(), 0.5f)` — in dark that lifts toward the near-white
  rose glass → mid-tone chips with washed light text; both now keep a
  dark near-black tinted fill in dark (`lerp(chipSurface, Black, 0.15f)`)
  so the light label reads crisp. (2) `PickerPageTab`/`PickerPresetChip`
  default idle fill lifted toward `curioPillLift()` (WHITE in dark) →
  near-white Original/New idle pills with light-grey text; default now
  stays a dark raised glass in dark, padding bumped to fuller pills
  (16/10, 14/10). (3) `CurioProgressPill` 12/7dp padding made the reveal
  badge a slim strip so the v81 inner glow (radius = width) bled past it
  → 14/11dp proper pill body. (4) Reveal `SentimentButton` inactive:
  near-white `curioPillTintLift` + light-grey text washed on black →
  dark raised glass (`lerp(surfaceContainerHigh, accent, 0.25)`). (5)
  `tintedTileInk` (capture attach icons + journal "Record a voice note"
  row) returned the DEEP light-mode ink unconditionally → invisible on
  dark tinted tiles; dark now resolves the light twin. (6) Quote card
  header "Quote N" renders ABOVE the paper slip on the theme page → dark
  flips to the bright butter control twin (`paperControlAccent`). (7)
  `DictateFieldButton` mic chip: raw deep accent on the dark tinted chip
  was dark-on-dark → light twin in dark.
- **v86 — Profile quests button gets its dark treatment.** The quest
  plate in `ProgressAndAchievementsCard` was missed by v81: it kept the
  pale `CoralBlush` glass (`lerp(CoralBlush, curioPillTintLift(), 0.55)`)
  + `cardGradient(CoralBlush)` icon box, which glare on black. Dark now
  flips the plate to deep rose glass (`lerp(HomeRosewoodDark, Black,
  0.30)`) + `curioGlassEdge`, and the icon box to
  `cardGradient(HomeRosewoodDark)` so the white trophy reads. Light
  unchanged.
- **v85 — the sort pill is finally a true sibling of the search pill.**
  Root cause was TWO problems: (1) the sort pill carried glyph + label +
  chevron + divider + arrow (~135dp) next to the icon-only Search pill
  (~48dp — ~3×, and every earlier padding pass never removed the label);
  (2) in dark the sort pill wore the One UI glass glow while the hero
  action pills only had the retired no-op `curioDarkGlow`, so it glowed
  and the search pill stayed flat. Fix: `CurioSortDropdown` is now the
  same compact two-zone icon pill as search — glyph opens the menu (label
  moved to the menu header), divider, direction arrow; same 46dp height,
  ~55dp wide. `CabinetHeroActionPill` + `SettingsHeroActionPill` gained
  `curioGlassGlow` (same dark glass as the sort pill), and the Cabinet /
  Topic Database search pills pass `emphasized = true` to match the sort
  pill's fill. Unused imports cleaned (`Arrangement`/`widthIn`/`height`),
  `Column` added for the menu header.
- **v84 — filter sheet polish + category picker tear.** Filter sheet
  search is now a Cabinet-hero-style `OutlinedTextField`: frosted
  category-glass container + ink-tinted icon/border/text/cursor, colors
  DYNAMIC per category (`cat.categoryInk()`); the duplicate `SectionLabel`
  (glyph + group name) under the open group pill is gone (10dp spacer
  keeps spacing); group pills + filter chips wear the One UI glass edge +
  inner glow in dark (their 3dp shadows are invisible on the black sheet
  — `curioDarkGlow` is a retired no-op); the Spin Filter pill + BottomCta
  badge show `filteredPool.size` (total topics matching the selected
  filters) instead of the ticked-chip count. `MorphEntrance` gained
  `bouncy = false` (the elastic spring's ~5% overshoot read as a brief
  "more elevated" shadow flash — both category pickers pass it). The Spin
  picker sheet's tear GREW (118 → 184dp + status bar) to hold the
  Original/New tabs + quick-mix presets inside the banner; `PickerPageTab`
  + `PickerPresetChip` gained `accent`/`accentInk`/`idleInk`/`idleFill`
  params so they ride the banner ink/fill (or the wash category in the
  full-screen picker) — theme-aware + dynamic, preset glyphs included;
  both Mix buttons use `themedButtonFill()`/`themedButtonInk()`.
- **v82 — dark-mode audit: everything left light-only fixed.** Paper
  stat cards (`paperStatCardColor` → deep near-black paper in dark;
  `paperStatCardFill` gained a `dark` flag so the steel-ring back arcs /
  dives / split gaps flip to LIGHT metal on the dark paper — Home, Detail,
  Profile pass `isCurioDarkTheme()`). Streak pill ink → bright butter in
  dark. `CurioHeroShuffleCard` wildcard hero wears `HomeRosewoodDark` +
  bright content ink in dark. Home "Surprise me" pill ink → bright twin.
  BugReport + Support + empty-state CTAs: deep rose fill + bright twin ink
  in dark (pale CoralBlush fills glared). Reveal's pastel hero gradient
  got its dark deep-pastel stops (missed in v81). `paperControlAccent`
  (rich-text toolbar/cursor/chips) flips to bright butter in dark — the
  note PAPER sheets stay theme-agnostic BY DESIGN (documented contract).
  Quests Claim pills use deep `CoralInk`/`GoldInk`/`CategoryTeal` fills in
  both modes (the bright twins + soft legacy teal washed white out); coral
  icon chips flip to deep rose in dark.
- **v81 — the reimagined dark mode: pitch black + Samsung One UI 9.5
  glow.** `themeModeState` (Light/Dark/System) is back in AppPreferences
  and `isCurioDarkTheme()`/`ForContext` read it (System follows the
  device). `CurioDarkColorScheme`: pitch-black page, surfaces step up
  through near-black greys (elevation via lightness), bright accent
  roles. CategoryInk dark branches: `categoryInk` → the LIGHT 300 twin,
  `themedAccent` → `darkAccent` (same hue, L≈0.44, ~20% desat),
  `headerAccent` → same-hue dark hero shade (L≈0.34), the wash collapses
  to pure black (NO background tint in dark — watermarks carry the
  category identity, with their pre-existing dark alphas), surfaces →
  `darkSurfaceTint`/`darkChipTint`. New `CurioGlassEffects.kt`:
  `curioGlassEdge` (the 1% whitish top-lit edge, NOT a border) +
  `curioInnerGlow` (the One UI 9.5 radial inner glow) — both dark-only
  no-ops in light; `categoryEdgeShine` draws the whitish edge in dark, so
  the treatment is app-wide. The SpinButton wears the glow (Samsung
  shuffle formula) and the orbit dots flip to the accent's light twin.
  Buttons/fills reversed per the ask: dark same-hue fills with light twin
  ink (`themedButtonFill`/`themedButtonInk`). Settings → Appearance and
  the onboarding theme step re-gain the Light/Dark/System picker.
- **v79 — sort pill and search pill get the equal middle-size treatment
  (Cabinet + Topic Browser).** The sort dropdown's Row had drifted to
  `heightIn(min = 52.dp)` while the icon-only Search pills
  (`SettingsHeroActionPill` / `CabinetHeroActionPill`) stayed at the
  v30-uniform 42dp — so the sort pill read big next to a small search
  pill. Unified at the middle: both pills are now **46dp tall** (sort
  52→46, hero action pills 42→46) and both carry **20dp glyphs** (sort-
  type icon 16→20, action-pill glyphs 22→20). Shared components, so the
  whole hero-pill family stays uniform; width remains content-driven
  (the sort pill keeps its label + chevron + direction arrow).
- **v78 — light-only: Dark / AMOLED / Material removed, Curio light
  stays.** The entire theme-style + theme-mode machinery is gone:
  `AppPreferences` lost `themeStyleState`/`themeModeState` (prefs,
  constants, getters, setters); `CurioTheme` lost the dark/AMOLED
  ColorSchemes and the Material dynamic palette; Settings → Appearance
  dropped the Theme style + Theme pickers (keeps Category tint, Pastel
  colors, Hero, Adaptive Hero); the onboarding theme step is now a single
  pastel toggle (`ThemeModeChip` deleted). Every `isCurioDarkTheme()` /
  `THEME_STYLE_*` branch across Profile, Home, Spin (deck gradients,
  ticket brush, shuffle plate, deck controls, sheets), Detail (hero
  start, frosts, waveform inks, mood board), Reveal (band paper, hero
  brush, pill glass), Category Picker, DeckPresets, PetDesigner dialog,
  CaptureFormatComponents, RecycleBin and the shared components was
  collapsed to its light path; unused imports removed (incl. Detail's
  dead `contrastRatio`). **Seam kept for the future dark system:**
  `isCurioDarkTheme()` / `isCurioDarkThemeForContext()` return `false`
  and the watermark / session-service plumbing that feeds them stays.
  Committed locally, NOT pushed (per user).
- **v77 — hero back buttons are OPAQUE theme-aware pills.** The settings-
  family heroes' back pills were the last translucent holdout
  (`symbolTint.copy(alpha = 0.18f)` in `SettingsHeroHeader` — Settings hub
  + every sub page — and `ink.copy(alpha = 0.18f)` in Topic History),
  while the hero ACTION pills had already been converted to an opaque
  fill in v27n (`lerp(backdrop, curioPillTintLift(), 0.38f)`). Both back
  buttons now use that exact construction: `containerColor =
  lerp(fill, curioPillTintLift(), 0.38f)` (theme-aware: rose-kissed page
  lift in light, white in dark, grey glass in AMOLED) +
  `shadowElevation = 3.dp`, glyph keeps the hero's readable ink. The
  default `surfaceVariant` pill (BugReport / Category Picker / FieldMind /
  SaveCapture), Profile's solid hero-fill pill and the detail page's
  near-opaque frosted sticky plate are untouched.
- **v76 — detail page Date · Mood · Session · Type card wears an opaque
  theme-aware pane.** `EntryDetailScreen`'s hero meta card (Date · Mood ·
  Session · Type grid) default fill was FROSTED glass (a translucent
  `heroStart` bloom at 30/16% alpha + a white/midnight `heroFrostBrush`)
  that read transparent and kept the pane flat. It's now a single OPAQUE
  vertical gradient `lerp(heroSheetColor, heroStart, 0.30f)` →
  `lerp(heroSheetColor, heroStart, 0.16f)` — `heroSheetColor` is the page's
  theme-aware sheet (near-white light / midnight dark + AMOLED), so the
  same perceived category bloom stays while the shadow renders clean
  (Profile/Home stat-pane language; ink contrast preserved — the detail
  hero is a deep category color, so the lerp is off the sheet, not the
  banner fill). `shadowElevation` 3dp + `curioDarkGlow` always apply now
  (were flat for the frost); `heroFrostBrush` deleted, `curioDarkGlow`
  imported. The paper-card experiment branch is untouched.
- **v75 — Home stat card wears an opaque theme-aware pane like Profile's.**
  The Home Streak · Cabinet · Topics pane's DEFAULT fill (off the "Paper
  stat card" experiment) was a transparent rose glass (12% → 55% alpha)
  that read see-through and let the elevation shadow bleed. It now uses
  Profile's exact stat-pane construction: an OPAQUE vertical gradient of
  `lerp(heroFill, White, 0.06f)` → `lerp(heroFill, White, 0.26f)` with the
  AMOLED step toward `HomeRosewood` 0.30, and `shadowElevation` 3dp +
  `curioDarkGlow` now always apply (they were gated behind the paper-card
  experiment). The experiment branch is untouched.
- **v74 — category picker sheet: tear hero to the status bar + no close
  button.** `CategoryPickerSheet` (Spin) got the filter sheet's v70 tear
  treatment: `shape = RectangleShape` (flush top), `dragHandle = null`,
  `contentWindowInsets = { navigationBars ∪ ime }` so the banner fills the
  very top edge behind the status bar (height `118.dp + statusBar`, content
  clears it via `statusBarsPadding`; distinct tear seed `0xC4A71E`, category
  gradient + watermark glyphs). The Close X and the floating drag handle are
  GONE — swipe-down / scrim tap / Mix-Cancel dismiss instead. The old
  header row + mode-hint text moved onto the banner: 34sp title, the hint
  as subtitle (single-select now "Tap a deck to spin it. Hold to pick
  several." — period, not the · separator; multi keeps "Tap to toggle
  decks · Done to spin together"), and the current-deck / "N selected"
  chip as a hero-glass pill. The column dropped its `navigationBarsPadding`
  (the contentWindowInsets handle the bottom inset now) and the
  `navigationBarsPadding` + `BottomSheetDefaults` imports went with it.
- **v73 — filter-sheet group pills wear their group glyphs.** The
  accordion pills in Spin's `FilterSheet` (`FilterGroupPill` — Type /
  Genres / Era / Origin / Franchise) now lead with the same per-group
  glyph their section labels wear (`FilterGroupKey.glyph`: category /
  style / history / public / movie — accent-tinted when closed, content
  `ink` when open), with the row start padding trimmed 20 → 18dp to sit
  the icon comfortably.
- **v72 — Settings/Profile option cards + icons follow the hero's color.**
  The shared option-card primitives (`CurioSettingsCard` fill,
  `CurioCardHeader` + `CurioSettingsRow` icon chips + glyphs) were
  hardcoded ROSE (`curioPillTintLift` / `CoralBlush` chips /
  `curioRoseInk` glyphs) while the hero wears the Spin lane's accent
  (Adaptive Hero) or the sky-azure — so Profile/Settings option cards
  never matched the banner. Three new hero-aware resolvers in
  `SettingsHubScreen.kt` (next to the shared hero-family helpers):
  `settingsCardAccentInk()` (glyph ink: lane → `categoryInk()`, azure →
  deep azure twin in light / pale azure in dark, else rose),
  `settingsCardChipTint()` (chip hue: lane → `themedAccent()` light /
  `lightAccent` dark, azure, else coral — dark keeps the pale-glass
  chip look), and `settingsCardTintLift()` (card-fill twin of
  `curioPillTintLift`: same construction/strength, hue follows the
  hero). `CurioSettingsCard.kt` now resolves all three through the
  hero's hue, so Profile + Settings (and every screen sharing the
  primitives — Support, Backup, Experiments, Quests) match the banner
  in every theme. Material/AMOLED keep the rose (their banners wear
  scheme roles — same gating as `settingsRoseAccent()`).
- **v69 — universal mood-board import, editor/saved fit consistency,
  mood collapse-on-pick, chip-bar slide animation.** (1) **Mood-board
  import is now the ANDROID PHOTO PICKER** (`PickMultipleVisualMedia` +
  `PickVisualMediaRequest(ImageOnly)` in `MoodBoardCanvas`) instead of
  the raw `OpenMultipleDocuments` documents UI — one universal
  gallery/camera grid on every device. (2) **The inline editor now fits
  EXACTLY like the saved card** (`MoodBoardCanvas`): the crop extent is
  the CURRENT tile set's bounding box instead of a once-per-session
  frozen extent. The freeze kept the editor stable but diverged from
  the saved view the moment a tile was added/dragged past it — the
  saved card re-fitted and the board "resized" between edit and detail
  (fresh boards even showed 1:1 while the saved card zoomed to the
  content). The drag preview lives inside the tile, so the fit stays
  constant mid-drag and updates on commit — identical to what the saved
  view recomputes from `tileLayouts`. (3) **Mood picker collapses once
  a mood is picked** (`SaveCaptureScreen` mood pill): `moodSelectorOpen
  = false` inside `onMoodChange`. (4) **Cabinet + Topic Browser chip bar
  slides in properly**: the bar is positioned with a large
  `.offset(y = barTop)`, so `expandVertically`'s height+clip animation
  hid it until the clip finished (delayed pop, no visible motion); the
  enter/exit are now `slideInVertically/slideOutVertically` + fade, so
  the chips emerge from under the torn hero.
- **v68 — hero-tinted icons, pill search bars, tear-hero filter sheet,
  sort-type icons.** (1) **Settings + Profile icons theme-aware**
  (`SettingsHeroHeader` + `ProfileHero`): the hero watermark symbols /
  back pill rode the hardcoded `HomeRosewood` in AMOLED — they now ride
  the hero's READABLE ink (already resolves per-theme + per spin-lane),
  so a lane-colored hero never wears mismatched rose icons. The cream
  paper under the tear picks up a 10% lerp of the hero fill (was flat
  cream); AMOLED keeps the rose twin. (2) **Search bars pill-shaped**
  (`CurioSearchField`): rounded-16 box → full 50dp pill like the Cabinet
  hero search — Settings hub, Spin filter sheet and Topic History all
  share it. (3) **Filter sheet tear-hero header** (`FilterSheet` in
  SpinScreen): the plain icon+title row is now a category-colored torn
  banner (`SoftTornBottomShape` + torn-edge hairline + watermark glyphs)
  with the category name at 26sp and a Clear-all pill riding it; the
  groups column gained proper margins (14dp under the tear, section
  label clears the pill row with its own 6/6 top/bottom padding) so the
  open group never reads cramped or offset. (4) **Sort pill shows its
  sort-type icon + slimmer** (`CurioSortOption` gains `glyph`;
  `CurioSortDropdown` renders it 16dp before the label, label-zone
  padding 8/6 → 6/4, gap 5 → 4dp, min-width 88 → 76dp): Cabinet
  Date/Title/Category → calendar_today/text_fields/tune; Topic Database
  Default/Name/Year → auto_awesome/text_fields/calendar_today.
- **v67 — progress dialog colors fixed (reveal page).** The reveal
  hero's pill passed the RAW category accent into the dialog, which then
  used it for the −/+ stepper glyphs and the Save label ON the theme's
  onSurface — with a deep accent (navy/indigo/…) that was dark-on-dark.
  `CurioProgressEditorDialog` now drives EVERY element from
  `contentColor`: the reveal + detail pills pass `cat.categoryInk()`
  (readable deep accent in light mode / light twin in dark) so the ring,
  steppers, slider and Save read on the standard dialog container in
  both modes; `StepButton` tints a 14% wash of the content color (glyph
  in the full color) instead of a solid circle; the Save button pairs
  the content-colored container against `MaterialTheme.colorScheme.surface`
  so its label always contrasts. The dialog's `accent` parameter is gone
  (only caller was the pill).
- **v66 — progress visibility + detail pill moved to the screen corner.**
  (1) **Cabinet progress line visible in light/pastel** (`CurioTopicCard`
  progress strip under the hero): `themedAccent()` resolves to a light
  pastel twin in pastel light mode that washed out on the cream hero —
  the fill now uses `categoryInk()` in light mode (hue-preserving deep
  accent) and the accent in dark; the track lifts 0.18 → 0.32 alpha and
  the line grows 4 → 5dp. (2) **Detail progress pill moved from the
  hero's bottom-right corner to the SCREEN's bottom-right corner**
  (`EntryDetailScreen`): the pill left the hero Box and now floats in
  the screen-level Box beside `DetailStickyBar`, aligned BottomEnd with
  a 16dp corner inset (the NavHost Scaffold already pads content above
  the nav bar via `contentWindowInsets = navigationBars`, so no extra
  inset). (3) **Its bar fixed too** (`CurioProgressPill` slim bar): the
  fill was `accent` (light pastel in pastel light — invisible); it now
  uses the deep category ink in light mode and the accent in dark, and
  the track alpha lifts 0.25 → 0.30. The reveal hero's count-only
  badge (`showBar = false`) is untouched.
- **v65 — Pet Designer: auto-import, brush size, transparent fill,
  tool-tray color chip, eye presets + placement.** (1) **Auto-import**
  (`autoImportNext` flag + "Auto-import image" in `ImportMenuDialog`):
  the picked PNG runs the whole pipeline in one tap — dominant colors
  map into the four custom slots (`buildImportReview` +
  `addCustomColor`), the image snaps to the extended palette, BOTH body
  and curled grids fill (`PetDesign.bodyAsCurled`), a 4-frame "happy"
  bob animation is added, and the result is saved immediately
  (`AppPreferences.setPetDesign`). (2) **Zoom slider removed** from the
  pixel editor — replaced by `BrushSizeRow` (sizes 1–4) and `brushSize`
  state; Brush/Erase paint a brushSize×brushSize square (`applyTool` +
  `applyToolToRows`), and `PixelGrid` draws an on-canvas ring showing
  the exact footprint when Brush/Erase is armed. (3) **CLEAR tool**
  (`PaintTool.CLEAR`): flood-fills a region with transparency — removes
  a solid background; acts once per gesture like FILL (both editors).
  (4) **Selected-color chip in the tool tray**: `ToolTray` gains a
  `paintHex` swatch + `onPaintTap` (tray row is now horizontally
  scrollable). (5) **Eyes section** (Settings page, `EyeControls`): 3
  size presets (Small/Medium/Large → `eyeScale` 0/1/2), an arrow cross
  pad adjusting `eyeOffsetX`/`eyeOffsetY` (−6..6), a live bobbing
  sprite preview (`rememberInfiniteTransition`), and Reset. New
  `PetDesign` fields `eyeScale`/`eyeOffsetX`/`eyeOffsetY` serialize as
  `eyesize=`/`eyeoffx=`/`eyeoffy=` (tolerant parser, defaults 1/0/0);
  `CurioPetSprite` scales each eye around its own center (left 4.5,
  right 10.5, row 7 in 16-space) then applies the offset — the live
  pet, previews and saved designs all render the same look.
- **v59.3 → REVERTED (v64) — toggleable serif body text + tighter label
  tracking.** v59.3 added a "Serif body text" Appearance toggle
  (`AppPreferences.loraBodyState` / KEY_LORA_BODY), `CurioLoraBodyTypography`
  + `@Composable curioAppTypography()` (body → Lora at 0 tracking, read by
  `CurioTheme` + ExploreSessionService bubble) and labelMedium/labelSmall
  tracking 0.5 → 0.3sp. The user reverted f991db1 (v64): the toggle row,
  the pref key/state/seeding, both typography variants and the tracking
  tweak are all gone — body text is back to the platform sans and label
  tracking back to 0.5sp. The v35 Lora editorial serif for reading/hero
  text (fonts + CurioEditorialBody) is UNTOUCHED.
- **v63 — update notice is now an IN-APP toast (no android Toast).**
  New global bus `CurioToast` (ui/components/CurioInAppToast.kt):
  `object CurioToast` with snapshot state (`show(text, glyph,
  actionLabel, actionId)` / `dismiss(id)`) + `CurioInAppToastHost` — a
  themed pill (dialog-tinted container, primary glyph, 6dp lift) that
  slides up, holds ~3.5s and fades. Hosted at the ROOT of `CurioNavHost`'s
  Box (above every screen, cleared past the bottom nav via
  navigationBarsPadding + 96dp), so a message fired before the UI composes
  is picked up on first frame. v63b — toasts with an `actionId` are
  TAPPABLE: `CurioInAppToastHost` takes an `onAction` callback (the
  NavHost maps "support" → `CurioRoutes.SUPPORT`) and shows the
  `actionLabel` ("Open") after a divider; tapping dismisses + navigates.
  `UpdateChecker.notifyIfUpdateAvailable` now calls `CurioToast.show(...
  glyph = CurioIcons.Download, actionLabel = "Open", actionId =
  "support")` instead of `android.widget.Toast` (import removed), and
  v63b moved the once-per-version gate ([AppPreferences]
  lastNotifiedUpdateVersion) BEFORE both announcements — a pending update
  is announced ONCE (toast + notification together), never on every
  launch. Generic bus — future background notices reuse it.
- **v62 — sort pill slimmer again (Cabinet + Topic Browser).** The shared
  `CurioSortDropdown` still read too wide next to the icon-only Search
  pill (user: "too wide, please reduce it"): label-zone padding 10/8 →
  8/6, chevron 18 → 16dp, zone gap 6 → 5dp, arrow-zone horizontal
  padding 6 → 4dp, direction arrow 22 → 20dp, and the widthIn floor
  96 → 88dp. Both heroes keep the uniform 42dp height — the pill now hugs
  its label ("Category"/"Default"/"Year") instead of stretching.
- **v61 — Spin filter sheet: bigger chips + louder typography/hierarchy.**
  (1) **Filter chips bigger still** (`CompactChip`): 16 → 18sp label,
  padding 16/11 → 20/13, glyph 17 → 19dp, gap 7 → 8dp, inactive weight
  Medium → SemiBold. v44 grew them to 15sp, v52b to 16sp — this is the
  third bump.
  (2) **Whole-sheet type/hierarchy scaled up** (all private to
  `SpinScreen.kt`): sheet header 22 → 24dp glyph + 24sp title; subtitle
  bodySmall → bodyMedium; `CurioSearchField` gains an optional `textStyle`
  param (shared component — Settings hub / Topic History unchanged) and
  the sheet passes 18sp; "Active filters" label labelMedium → labelLarge;
  `SectionLabel` 14 → 16sp ExtraBold (0.3sp tracking); `FilterGroupPill`
  15 → 17sp label with 20/14/12/12 padding + 18dp chevron + bigger badge;
  `ActiveFilterChip` labelLarge + roomier padding + 16dp close; group/chip
  FlowRow gaps 8 → 10dp; Apply CTA 17sp with 14dp vertical padding + 20dp
  icon.
- **v60 — session-screenshot attach removed; mood-board crash + reveal strip fixes.**
  (1) **Session-screenshot FEATURE removed (user: "too scary", old shots
  kept attaching).** The save page's "Session screenshots" section
  (auto-attached session shots + add-from-gallery + per-shot remove) is
  gone — nothing reads or attaches screenshots anymore. The manifest
  drops `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` (≤32) and
  `FOREGROUND_SERVICE_MEDIA_PROJECTION` (nothing uses MediaProjection in
  the tree — v55 removed the watcher and no capture code remains). The
  shared session NOTE survives untouched (`SessionNoteFloatingPill` +
  `peekWriteSessionNote`), and all legacy data paths are left inert and
  read-only: `ExploreSession.screenshotPaths`, pending-write screenshots,
  `CaptureEntity.sessionScreenshotsJson`, `SessionShots`, backup/restore
  round-trip, and EntryDetail's display of ALREADY-saved screenshots.
  (2) **Mood-board expanded-dialog crash fixed.** `ExpandedMoodBoardDialog`
  called `.ifEmpty{}` on `tileLayoutsFull`/`quotePositionsFull` — Gson
  bypasses Kotlin defaults, so pre-v57 entries decode those to NULL and
  `Collection.isEmpty()` NPE'd on the dialog's first measure (the reported
  crash). Now `orEmpty().ifEmpty{...}`; same null-guard applied to
  MoodBoardExport's three `tileLayoutsFull.isNotEmpty()` /
  `quotePositionsFull.isNotEmpty()` sites (save/share path).
  (3) **Inline mood-board quote cards no longer balloon.** When the
  collage is smaller than the canvas it zooms to fill (scale > 1) and the
  raw slot width (~41% of the board) multiplied by that zoom — a quote
  card could grow into a huge slab in the small inline editor.
  `MoodBoardFloatingCards` caps the DISPLAY scale of never-resized
  fallback cards at ~44% of the canvas (`displayScale`); user-resized
  cards (saved.w) keep the full scale.
  (4) **Reveal strip: tags raised + Like/Dislike active state POPS.** Tag
  row top inset 10 → 6dp (clearer clearance between tags and the
  sentiment row at larger font scales); `SentimentButton` active now
  scales to 1.08 with a category glow (`curioDarkGlow` 4dp), ExtraBold
  label and 17dp icon — a liked/disliked topic is unmistakable.
- **v59.2 — watermark icons: fewer in the drawer, screen-matched elsewhere.**
  (1) **Drawer calms down** — the Home nav-drawer hero now scatters 3
  mirrored pairs (was 5), smaller (34–42dp vs 44–56dp) and fainter
  (alpha 0.07–0.08 vs 0.11–0.14) so the brand + greeting dominate, and
  its glyphs are navigation-flavored (`drawerHeroSymbols`: menu /
  explore / auto_awesome / star / diamond / bolt — the first 6 of the
  old wildcard set were casino/explore/bolt/star/nightlight).
  (2) **Screen-matched hero watermarks** — new `CurioIcons` sets:
  `settingsHeroSymbols()` (gears/sliders/appearance: settings, tune,
  dark_mode, light_mode, contrast, palette, colorize, backup,
  notifications, layers) now feeds the Settings hub hero;
  `historyHeroSymbols()` (history, schedule, restore, replay, refresh,
  timer, calendar_today, undo, auto_stories, menu_book) feeds the Topic
  History hero (was the BOOKS family). Home keeps the wildcard set (its
  quest IS wildcard, per user); Profile/Cabinet keep their lane/category
  echo (deliberate Adaptive Hero behavior); Onboarding keeps wildcard
  (welcome). All glyphs already used elsewhere in the app — verified in
  the bundled Material Symbols subset, no tofu.
- **v59 — deck excludes only SAVED entries; uniform Cabinet card height.**
  (1) **"Only saved entries leave"** — per user decision, a topic stays in
  the shuffle deck until it has a SAVED entry in the Cabinet. The old
  v7.80 done-set exclusion (explored or "Already …" marked topics left
  the deck forever) is gone from Spin: `deckPool` (fan/peek cards) and
  the landed `pickFrom` call now exclude by `savedTopicIds` only — a
  reactive set derived from a new `produceState` over
  `CurioRepositoryHolder.repo.observeAll()` (the Cabinet's flow; the
  old per-spin `repo.getAll()` + `doneIds` computation was dropped).
  `pickFrom`'s `exploredIds` param renamed `savedIds` + comments updated.
  `ExploreSessionStore` is no longer referenced from Spin (import
  removed); `recordExplored`'s `addDone` + the done set stay intact —
  they still drive the reveal's "Already …" state and the Topic
  Database's done markers, just not deck exclusion.
  (2) **Uniform Cabinet card height** — `CurioEntryCard`'s title now
  reserves exactly two lines (`minLines = 2` + existing `maxLines = 2`):
  short titles leave a blank second line, long ones ellipsize (never
  cut), so every grid card is the same height regardless of title length.
- **v58 — save page: chips + take tabs pin under the topic strip; mood pill in the strip; attach-tile ink.**
  (1) **Header hoisted to the topic.** The multi-take section state
  (`sections`/`activeIndex`/`nextId`/`pendingRemoveIndex`/`pendingFormatSwitch`
  + `snapshotActive`/`removeSection`/`applyFormat` + the aggregate
  `allReady`/`combinedData`/`anyTakeDraft`/`sectionDraftData` emissions)
  moved OUT of `FormatBodyForCategory` into `SaveCaptureScreen` so the
  format chips + take tabs can pin in a compact horizontally-scrollable
  row UNDER the topic strip (they used to live inside the scrolling body
  under a now-removed "How do you want to capture this one?" header;
  wide windows no longer wrap the chips into a tall FlowRow — always one
  slim row). `FormatBodyForCategory` is now editor-only (active take,
  `key(current.id)`); the two take-confirm dialogs (remove / switch
  format) live at screen level next to the leave dialog.
  (2) **Mood lives in the topic strip.** The universal "How did it make
  you feel?" row is gone from the body; a capsule pill on the RIGHT side
  of the topic strip shows the active take's mood (or a "Mood +"
  affordance with `MoodHappy` glyph) and toggles the shared mood selector
  (bare `MoodChipsRow(header = null)` — new optional `header` param)
  pinned under the strip. Picking a mood writes it into the ACTIVE
  section (works identically for newly added takes), stamped into the
  take's data via `withMood` as before. The emissions `LaunchedEffect`
  also keys on `topic` so the save CTA enables the moment the topic loads.
  (3) **Attach-tile ink.** `AddImageButton`, `ImageThumb` (empty state)
  and `JournalVoiceNoteRow` (Marginalia) drew their icon+label in raw
  `accent` on the 16% `categoryTintFill` tile — invisible in pastel
  light. New `internal fun tintedTileInk(accent)` (CaptureFormatComponents)
  resolves a deep same-hue ink in light (`readableLightInk`) and a light
  twin in dark (lerp toward white), applied to the attach tiles in every
  format (Journal/Field notes/Review).
- **v57 — mood board dual layouts (inline vs full-screen) + quote pinch-to-expand.**
  (1) **Two saved arrangements** — `CaptureData.GalleryWall` gains
  `tileLayoutsFull` + `quotePositionsFull` (Gson default-empty; the
  `withImageUris` remap covers both layout lists). The INLINE layout is
  what the small saved card + inline editor arrange; the FULL-SCREEN
  layout is what the expanded dialog + full-screen editor arrange.
  Legacy entries (empty full fields) fall back to the inline ones, so
  old boards keep their single arrangement. (2) **GalleryWallFormat**
  keeps a second `fullTiles` list + `fullQuotePositions` list (both
  seeded from saved full data, falling back to inline); the full-screen
  canvas edits `fullTiles` and routes quote moves/resizes to
  `fullQuotePositions` via new `quotePositionsOverride` /
  `onMoveQuoteOverride` / `onResizeQuoteOverride` params on
  `MoodBoardCanvas` (null = inline board unchanged). `QuoteCardsState`
  gained a `onCardRemoved` hook so the index-aligned full-screen list
  stays in sync when a card is deleted. `canSave` counts `fullTiles`
  too, and the save `LaunchedEffect` keys on both full lists so
  full-screen edits re-emit the entry. (3) **Expanded dialog** renders
  `tileLayoutsFull`/`quotePositionsFull` (fallback inline). (4)
  **Save/Share PNG** gained `MoodBoardExport.MoodBoardLayout`
  (INLINE/FULL) — `MoodBoardExportActions` shows an Inline / Full-screen
  pill picker above Save/Share, and the export preloads bitmaps + renders
  against the chosen arrangement (both layouts keep quote placements in
  sync with their tile list). (5) **Quote cards pinch-to-expand** —
  `MoodBoardFloatingCard` gained a 2-finger pinch handler (editor only,
  before the drag handler) that live-preview-resizes the card's width
  like the resize grip and commits on release; the drag handler skips
  deltas while `resizing` so a pinch never slides the card.
- **v56 — topic dataset thread lifecycle: bounded parses, cached counts, tiered memory shed.**
  (1) **Bounded parse concurrency** — `TopicJsonLoader` gained a
  `Semaphore(2)` (`gated {}`) around every file read+parse (`parseAsset` +
  `countFor`): the cold-start prewarm, a wildcard merge and several
  screens can all request lanes together; without a gate they parsed
  every file in parallel and saturated all cores — lag + device heating
  on mid-range phones. Max 2 concurrent parses; blocking acquires are
  fine on Dispatchers.IO and nothing nests gated sections (no deadlock).
  (2) **Wildcard merge routes through shared `load()`** instead of parsing
  lanes directly — a lane the prewarm/screens are already parsing is
  SHARED, never double-parsed by the merge. (3) **Per-lane count cache**
  (`countsCache`): `countFor` re-read + re-parsed the whole category file
  on EVERY Spin deck change / picker recompute just for a length; now one
  parse per lane per process, and `countCanonicalTopics` derives from it.
  (4) **Tiered memory shed** (`shedForMemory(level)` replaces
  `clearCache()`): RUNNING_LOW drops pools + counts (cheap single-file
  rebuilds) but KEEPS the 16k-entry index — v51's full shed made a trim
  re-parse everything (the reported lag/heat); the index only drops at
  RUNNING_CRITICAL/COMPLETE. (5) **Prewarm survives rotation**: the
  MainActivity warmup runs under `NonCancellable` so a mid-warmup
  activity destroy no longer restarts the whole index parse.
- **v55 — device-screenshot AUTO-ATTACH removed (per user: it lagged on screenshot).**
  `DeviceScreenshotWatcher` (the MediaStore ContentObserver that watched
  for new screenshots while a session / pending write was live and copied
  them into the session) is DELETED, along with its wiring: MainActivity's
  `DeviceScreenshotWatcher.start()` call and the reveal's
  `requestMediaRead` launcher + the READ_MEDIA_IMAGES permission request
  in `beginExploreSession`. The SAVE PAGE keeps its manual add-from-gallery
  (system Photo Picker — no storage permission — `SessionShots.copyFrom` +
  `appendPendingScreenshot`, untouched) and the remove-thumbnail option;
  backup/restore of session screenshots also untouched. The watcher's lag:
  the observer fired on EVERY media-library change (even with no session)
  plus MediaStore queries + full file copies at the exact moment the system
  was still writing/indexing the shot.
- **v54 — update toast + once-per-version notification + editable Profile tagline + progress dialog on the background tint.**
  (1) **Update notifier** (`UpdateChecker.notifyIfUpdateAvailable`, run on
  app start from MainActivity): fetches the latest release, and when it's
  newer — a TOAST announces it on every check that finds one, and a
  NOTIFICATION fires ONCE per version (AppPreferences persists the last
  announced tag; same-tag launches skip it). Notification opens the app
  (launcher intent), `curio_updates` channel, `ic_notification` small
  icon, runCatching on notify (POST_NOTIFICATIONS gate on 13+).
  (2) **Editable Profile tagline**: tap the hero tagline ("Keep the spark
  going today." …) → AlertDialog with an OutlinedTextField; Save persists
  `custom_streak_tagline` (empty = automatic `taglineForStreak`), "Use
  automatic tagline" resets it; `taglineRevision` bump re-reads the pref.
  (3) **Progress editor dialog on the theme background tint**: container
  `accent` → `curioDialogContainerColor()`; `dialogContentColor` default
  `ink` → `MaterialTheme.colorScheme.onSurface` (reveal's explicit
  `cat.onAccent()` override removed — that was the v45 workaround for the
  accent container); accent still colors the ring/steppers/Save button.
- **v53 — Apple Music resolves to a real catalog item + saved progress restored + filter chips/icons + chip-bar animations.**
  (1) **Apple Music "Watch in" resolves the topic to an actual catalog
  item** (`resolveAppleMusicItemUrl` in ExploreSearch.kt): the reveal's
  Watch-in for APPLE_MUSIC now calls the public iTunes Search API
  (entity = album / song / musicArtist by subtype) off the main thread
  and starts the session with the item's native deep link
  (`music://music.apple.com/{cc}/album|song|artist/{id}`) — the Android
  app only handles ITEM pages natively, so search links (music://…/search)
  still showed the in-app "Open in browser" banner. Falls back to the
  search link when the lookup fails (offline / no result); plain
  HttpURLConnection like UpdateChecker, 8s timeouts, org.json parse.
  (2) **Saved progress restored** — the real bug behind "progress isn't
  showing in cabinet/detail": `CaptureEntity.toEntry()`'s fallback topic
  (used until the catalog lane cache loads) dropped `pageCount`/
  `episodeCount`, so `progressTarget` was null on saved entries. New
  nullable `pageCount`/`episodeCount` columns (Room v6→v7
  `MIGRATION_6_7`), persisted in `toEntity()`, restored in the fallback
  (backup export already round-trips them via `gson.toJson(entity)`).
  Also: the Cabinet card line was drawn with `cat.onAccent()` — white on
  light heroes, invisible — now `themedAccent()` fill with a faint track
  that always shows while the target exists. Detail pill stays anchored
  BottomEnd (12dp) on the hero. (3) **Reveal strip**: tag chips raised
  14 → 10dp top inset so they clear the Like/Dislike row; SentimentButton
  slimmed (10/5 padding, 15dp icon) and its inactive fill now uses
  `curioPillTintLift()` (theme-aware) instead of `surfaceVariant`.
  (4) **Chip-bar animations**: the Cabinet + Topic Browser sticky
  category/search chip bars wrapped in `AnimatedVisibility`
  (expandVertically + fadeIn / shrinkVertically + fadeOut) — no pop.
  (5) **Filter sheet chips**: `CompactChip` gained a per-chip keyword-
  mapped icon (`filterChipIcon`, verified glyphs, AutoAwesome default),
  bigger (16sp label, 16/11 padding), and light-mode inactive fill
  darkened 0.82 → 0.5 lerp toward `curioPillTintLift` (same for the
  closed `FilterGroupPill`) so chips read as solid mid-tones off the
  pale sheet. Sort-pill dimensions unchanged — user asked for them and
  will dictate the decrease.
- **v52 — Apple Music deep link opens native search + backup-restore compile fix.**
  (1) **`buildMusicServiceSearchUrl` for Apple Music now uses the native
  `music://` scheme** (`music://music.apple.com/{cc}/search?term=…`)
  instead of `https://` — the Android Apple Music app renders
  music.apple.com/search (a web-only page) in an in-app browser with an
  "Open in browser" banner instead of searching, while `music://` is the
  app's registered URL scheme (any music.apple.com path works with https
  swapped for music), so its native router lands on the search tab. New
  shared `openSearchUrl(context, url)` helper launches the URL and, on
  `ActivityNotFoundException` (Apple Music not installed → no custom-scheme
  handler), falls back to the https equivalent so the old browser behavior
  is preserved; both launch sites (reveal's explore-and-go-home + Home's
  keep-exploring) now use it. (2) **CI fix:** the streaming restore's two
  `gson.fromJson(reader, CaptureEntity::class.java)` calls failed inference
  — Gson has no `fromJson(JsonReader, Class<T>)` overload (only `Type`), so
  un-typed calls bound to `Any!` ("Cannot infer type for type parameter
  'T'" + cascading unresolved `id`/`format`/`formatDataJson` errors in
  compileDebug/compileRelease). Both call sites now declare
  `val capture: CaptureEntity` / `val cap: CaptureEntity` explicitly.
- **v51 — reveal pill tints + bigger corner chips + complete memory shed.**
  (1) **Hero pills less whitish in light mode** (`HeroCard.pillGlass`):
  pastel light 80% → 60% toward white, non-pastel light 50% → 42% — the
  lane's accent clearly shows through the frost now. (2) **Strip tag chips
  carry color:** the opaque tinted fill blends 22% → 32% of the lane
  accent (`lerp(surface, themedAccent(), …)`) so they stop reading whitish
  on the pastel page wash. (3) **Corner controls larger:** the top-bar
  category chip padding 12/7 → 14/9 with an 18dp glyph, and the pin/close
  circles grow to a 24dp glyph on a 42dp circle. (4) **`TopicJsonLoader`
  complete memory shed:** `clearCache()` (fired on
  `TRIM_MEMORY_RUNNING_LOW`) now also drops `indexCache` + the canonical
  count — previously ~30-60MB (16k TopicIndexEntry + lowercased key
  copies) stayed resident after a trim, keeping the heap near-full so
  background GCs fired every second on mid-range devices (the user's
  "Background concurrent mark compact GC" log). All rebuilt lazily.
  Diagnosis of the pasted log (sustained GC churn + `Skipped 3320
  frames`): heap 110-160MB near-full + main-thread stalls from the GC
  storm; the shed is the retained-footprint lever.
- **v50 — Topic Reveal: Like/dislike into the strip + one editorial font.**
  (1) **Sentiment pair moved into the bottom band** (TopicRevealScreen):
  the section-6.5 row that scrolled in the body below the ActionPromptCard
  is gone; the Dislike/Like pills now live in the fixed bottom band BELOW
  the tag chips, aligned bottom with `navInset + 8dp` clearance. The tag
  row's top inset dropped 24 → 14dp to make room, and `RevealBottomBarHeight`
  stays 80dp — the strip never grows. `SentimentButton` was slimmed to fit
  (12/6dp padding, 16dp icon, labelMedium); browse mode still hides the
  pair (read-only), tags render independently. (2) **Quick fact + action
  instruction share ONE style:** new file-level `RevealEditorialBody`
  (`CurioEditorialBody.copy(15sp/23sp)` — a notch below the old 17sp fact)
  is now used by BOTH the TeaserCard quick fact (was `CurioEditorialBody`
  17sp) and the ActionPromptCard instruction (was an inline 15sp copy), so
  the two long-form paragraphs match exactly and can't drift again.
  `TextStyle` import added.
- **v49 — topic-load speed + smooth Topic Browser wheel scroll + Home spacing.**
  (1) **TopicJsonLoader shares in-flight parses instead of serializing:**
  the old single global `cacheMutex` was held for the ENTIRE parse, so
  the cold-start prewarm queue (`loadIndex` + `preloadAll` in
  `MainActivity`) blocked Spin's load of an unrelated lane until every
  category finished, and the Topic Browser's index load double-parsed
  the merged 16k-topic index alongside the prewarm. `load(id)` now uses
  a per-lane `inFlight` map + short-held `inFlightMutex` + a
  `loadScope`: DIFFERENT lanes parse in parallel, the same lane's
  concurrent callers await ONE shared parse (creator-only
  compare-and-remove), and the shared parse survives its creator's
  cancellation. `loadIndex()` gains its own `indexMutex` + double-check
  so prewarm + screen share one parse. (2) **Topic Browser fallback is
  never fatal:** a failed per-category load in the no-index fallback
  used to throw inside the `produceState` producer and freeze the
  screen on "Loading topics…" forever — now `mapNotNull` + `runCatching`
  skips just that lane. (3) **Wheel-scroll smoothness:** the
  `snapshotFlow { index to offset }` collect wrote to the saveable
  registry on EVERY scroll frame (60x/s over a 16k list) — it now
  persists only when `firstVisibleItemIndex` changes
  (`distinctUntilChanged`, restore lands at the row top), and the
  `items(rows, ...)` call gained `contentType` so section headers and
  topic rows recycle their own LazyColumn slots. (4) **Home spacing:**
  the section rhythm below the hero is one consistent 12dp (the old
  20dp ends stacked with the pre-Saved 20dp spacer = 40dp of dead
  space between the Shuffle the deck card and Saved when no
  session/queue is live; the doubled spacer is removed), and both
  "View all" pills (Saved + Recents) now use `onBackground` ink for
  text AND icon — matching the section titles instead of the washed
  theme-primary mauve on the cream pill.
- **v48 — streaming backup RESTORE (OOM fix, mirrors the export).**
  `CurioBackupManager.restore` no longer reads the whole file into a
  String, builds a JSONObject tree, and Gson-parses the entire payload
  (every media byte[] decoded and resident at once — the same OOM class
  as the old export). It now reads the file TWICE with a streaming
  `JsonReader`: pass 1 (`validateBackupStream`) enforces the same
  pre-flight (envelope format/version, captures array present with
  unique/safe/well-formed records, preferences restricted to the known
  files) without holding anything; pass 2 walks the sections — captures +
  prefs parsed per-section, and each media file decoded + written ONE AT
  A TIME (audio by capture id, images to their per-capture destinations
  via a uri→(captureId,index) map, session shots via the shared index,
  pending-write shots included) — recording only tiny path maps, then
  the database is wiped + re-inserted in one transaction. Same semantics
  and error messages as before; peak memory is one media file's bytes
  instead of the whole archive.
- **v47 — dark mode: deep high-contrast background tints + blackish picker idle cards.**
  (1) **Dark wash retuned** (`CategoryInk.kt`): `DEFAULT_DARK_WASH` no
  longer pulls un-tuned families' mid-tone 50% toward their LIGHT twin
  (the whitish wash) — it now hugs the deep accent (0.16 factor) with a
  slightly stronger 0.22 blend. Music (indigo) and Visual Art (teal) —
  the two families that previously fell to the default — got their own
  `DARK_WASH_TUNING` entries (deep indigo / deep teal-forest twins).
  (2) **Mixed-deck page wash deepened** (`CurioColors.mixedDeckWash`):
  dark mode now blackens the blend harder (50%) at a 42% blend (was
  35%/45%), and pastel-dark drops to 42% (was 55%) — mixed pages read
  sleek and dark with high contrast for white ink/paper cards. (3)
  **Picker idle cards blackish** (`CurioCategoryCard`): the Curio-style
  dark idle surface is now `surfaceContainerLow` pushed 55% toward black
  — near-black idle tiles; the SELECTED tile keeps its vivid full-accent
  gradient unchanged (idle-vs-active contrast is now the whole story).
- **v46 — progress UI: reveal dialog fix + cabinet visual-only line + detail corner.**
  (1) **Reveal dialog blank bug** (`CurioProgressPill`): the reveal hero
  passed `ink = cat.accent`, which the editor dialog used as its content
  color ON the accent container — every label drew accent-on-accent
  (invisible; only the alpha-blended arcs showed, the reported "thin line
  of progress, no text"). `CurioProgressPill` gains `dialogContentColor`
  (defaults to `ink`, so other callers are unchanged); the reveal passes
  `cat.onAccent()`. (2) **Cabinet card** (`CurioEntryCard`): the rising
  fill + tappable count pill are gone — progress is now a thin 4dp
  on-accent line with a faint track along the hero's bottom edge (between
  the hero and the title box below), VISUAL ONLY — no editor, card shape
  unchanged. (3) **Detail hero**: the progress pill anchors tighter to the
  hero's bottom-right corner (12dp).
- **v45 — streaming backup export (OOM fix) + category-picker draft persistence.**
  (1) **Backup OOM fix** (`CurioBackupManager.export`): the old path
  loaded EVERY audio/image/session-shot byte[] into memory, base64-copied
  the whole payload into one giant JSON String, then copied that into a
  byte[] — a large Cabinet OOM'd mid-backup on a mid-range device (crash
  report: A356E / Android 16). Export now writes the JSON incrementally
  with a `JsonWriter` and reads + base64-encodes each media file ONE AT A
  TIME at the moment its value is written (audio keyed by capture id,
  images deduped by URI, session shots deduped by original path, the
  pending write's shots included). Output shape is byte-for-byte the same
  Gson payload (same field names, same single-line base64) — restore is
  unchanged. (2) **Category picker draft** (`CategoryPickerScreen` +
  `CategoryPickerDraft`): the selection, multi-select mode, Original/New
  page and BOTH grids' scroll offsets are mirrored live into a
  process-scoped holder, so leaving the picker (back / swipe-down) and
  reopening restores exactly where you were — "kept saved until the
  restart". Committing a mix (or tapping a lane open / Cancel) clears the
  draft so the next open shows the persisted deck fresh.
- **v44 — Spin filter sheet: bigger color-tinted chips + flow Type group.**
  (1) **Bigger chips**: `CompactChip` labels bump to 15sp with roomier
  padding (14/9dp; the `FilterGroupPill`s match at 16/12/9dp) so the
  sheet fills instead of leaving empty space above the Apply button.
  (2) **Different color**: inactive fills swap `curioPillLift()` for
  `curioPillTintLift()` (rose-kissed glass in light, white in dark, grey
  glass in AMOLED) — the chips carry a color of their own instead of
  plain cream; selected chips keep the category accent. (3) **TYPE group
  is a flow now**: the fixed 2-column `LazyVerticalGrid` became a
  `FlowRow` of content-sized chips (`fillMaxWidth = false`) — a long
  subtype takes its own full line and the next chip wraps below it.
  Removed the now-unused `heightIn` + `curioPillLift` imports.
- **v43 — Topic Reveal hero pills + action labels + quick-fact voice.**
  (1) **Hero pill glass retuned** (`HeroCard` `pillGlass`): pastel light
  now lerps only 80% toward white (was 92% — the pills read as stark
  white blobs on the pale heroes), the deep non-pastel banner gets a 50%
  frosted-accent glass, and dark keeps a 55% lift toward
  `curioPillLift()`; all three are OPAQUE fills that carry the accent
  hue — theme aware, never transparent, never flat white. (2) **Action
  labels bumped**: `revealDockMetrics` `textSp` raised +1.5sp per tier
  (14.5 / 15.5 / 17.5sp) so Start exploring + Express yourself read a
  little larger (both stay ExtraBold, single-line). (3) **Quick-fact
  voice**: the `TeaserCard` quick fact moves from the plain Material
  `bodyLarge` back to `CurioEditorialBody` (Lora) — the SAME font as the
  `ActionPromptCard` instruction, so the reveal's long-form copy is one
  readable serif. The fact stays UNCLAMPED and shown in full — no
  read-more folding (user rejected the 4-line clamp).
- **v42 — mood-board editor stability + resizable quotes + tinted-glass styling.**
  (1) **Mood-board glitches fixed** (`GalleryWallFormat`): the inline
  board's crop extent is seeded ONCE per session (`stableBoardMaxX/Y` —
  fresh boards seed to the full canvas 1:1, edit-mode boards to the saved
  collage) and never re-grows, so adding a photo no longer re-fits and
  re-scales the whole board ("board re-sizes mid-edit") and drags follow
  the finger 1:1 (no snaps). (2) **Resizable quote boxes**: `QuotePos`
  gained `w` (raw board px, -1 = default slot width); `QuoteCardsState`
  gained a parallel `widths` list + `setWidth`; `MoodBoardFloatingCards`
  takes `onResizeCard` and `MoodBoardFloatingCard` renders an editor-only
  bottom-end grip (drag to widen, clamped to half-slot..board width,
  committed via the live callback). (3) **Grow-in-place tiles**: each
  photo tile has a bottom-start enlarge button that scales the tile ×1.45
  around its center (for photos too small to pinch). (4) **Category pill
  inside the hero**: the Cabinet (`CabinetHeroHeader` `titleTrailing`
  slot) and Topic Database (`SettingsHeroHeader` `titleTrailing`) now
  render the Category pill beside the title, directly under the sort/search
  pills; the below-hero pill rows + their height reservations are gone
  (chip bars sit directly under the banner). (5) **Sort pill**: corner
  radius 50dp → 18dp (the capsule read bulbous), and `CurioDropdownMenu`
  gained `minWidth` (236dp default) + taller 14dp rows. (6) **Azure hero
  re-enabled + default**: the grey-out and the migrate-back-to-rose effect
  are gone; `heroBlueState`/`isHeroBlueEnabled` default ON. (7)
  **Color-tinted glass** (the "creamy" fix): new `curioPillTintLift()`
  (light = background rose-tinted 8%, dark = white, AMOLED = `#2A2A2A`
  grey glass) replaces `curioPillLift()` in `SettingsHeroActionPill`,
  `CabinetHeroActionPill`, `CurioSortDropdown`, `CurioSettingsCard`
  (AMOLED: `surfaceContainerHigh`→grey lerp), `CurioSettingsRow` (icon
  now sits in a coral chip), and Profile's quest plate + hero pills.
  `CurioCardHeader`'s chip keeps a muted coral plate in AMOLED (was
  neutral grey), and AMOLED switches light up coral when ON (grey glass
  track when OFF).
- **v42 — merged badge shelf + quest-paths card grid + profile polish.**
  (1) **Merged badge shelf:** `CurioBadges.kt` gains `MergedChainBadge`
  + `mergedChainBadges()` — ONE medal per quest CHAIN (category) showing
  the chain's HIGHEST-earned stage (best rarity); earning a chain's
  bronze then silver upgrades the single medal to silver instead of
  stacking duplicates. The shelf (Quests strip + dialog + Profile strip)
  sorts earned badges first by rarity, then locked chains (silhouettes
  preview their best rarity; SECRET badges never show locked). New
  `CurioBadgeDetailDialog` (shared Profile + Quests) shows the medal,
  tier chip, name, description and live progress (+XP, and a
  "· upgraded" chip on merged tiles when earlier rarities were earned).
  Quests' `PathsCard` was redesigned from flat rows to a **card-per-path
  grid**: each card wears the chain glyph, a live progress bar, its merged
  medal, and opens `PathDetailDialog` (stage trail with Go chips). The
  old per-stage `BadgeTile`/`BadgeShelf` grid is removed (dead).
  (2) **Profile:** tapping any badge (earned or locked) on the Profile
  strip opens `CurioBadgeDetailDialog`; the Edit profile / streak / level
  pills are now OPAQUE (`lerp(fill, White, 0.18)` light, rose twin on
  AMOLED, +2dp shadow + dark glow) like the stat pane instead of the old
  `ink@18%` tint that smeared on busy banners; the "YOUR PROFILE" kicker
  stepped labelSmall → labelMedium (wider tracking) and the pills row
  spacing tightened (8dp gaps, even 58dp+ cells).
- **v41 — Explore dialog declutter + canonical pet-dialog doc.**
  (1) **Explore dialog:** the two helper paragraphs (the engine/verb
  intro and the timed-explore note) are gone; the dialog is now the
  title ("Explore {topic}?"), the rephrased no-AI pledge — "Keep your
  research yours. Read the real sources instead of AI summaries, and
  the discovery is all yours." (user-approved, no em dash) — and the two
  pill actions. The now-unused `action` val was removed from the dialog
  block (the pill glyphs only need the music-service resolution).
  (2) **docs/pet-dialogs.md** — the canonical, source-of-truth doc for
  every Curie/pet spoken line (BABY / FIRST_EVO / FINAL_EVO voices),
  organized by group: event reactions, streak milestones, evolution
  ceremony, mood bubbles, greetings/welcome-backs, touch tiers, games,
  memory/fact lines, the learning brain's composed + coined lines, the
  tour script, and the mature routine lines. Each section lists the
  current FIRST_EVO pools with BABY/FINAL_EVO twins inline, plus an
  integration checklist (keep pool names, bullet order, and placeholders
  like `__LANE__` / `$lane` / `$streak` identical when porting rephrased
  lines).
- **v40 — reveal bottom-band wash + smooth tab crossfade + lane tiles open Cabinet.**
  (1) **Reveal bottom band:** `TopicRevealScreen`'s band now wears
  `cat.categoryBackgroundWash()` (the page's own wash) instead of
  `categorySurface(surfaceContainer)` — the old strip resolved to a
  lighter tint that read as a separate white/creamy slab at the bottom,
  most visible behind the tags during the open fade. The reveal now reads
  as one continuous surface (Material/AMOLED unchanged). (2) **Tab
  switch animation:** `CurioNavHost` tab switches (enter + pop-enter) are
  now a clean `fadeIn` instead of `scaleIn(0.97f) + fadeIn` — the
  scale-fade read as the "old" animation opening the Cabinet from
  Profile. (3) **Profile lane tiles open the Cabinet filtered:** new
  `PendingCabinetFilter` out-of-band handoff in CurioRoutes (mirrors
  PendingEntryOpen: `request(CategoryId)` + monotonic `trigger` +
  `take()`); Profile's `LanesCard` gained `onOpenLane(CategoryId)` and
  its tiles became clickable `Surface(onClick)`; `CabinetScreen` consumes
  the pending filter once per request in a `LaunchedEffect(trigger)` and
  applies it to `selectedFilter` (clearing legacy/search). (4) **Lane
  glyph readability:** the tile icon wears `category.categoryInk()`
  instead of `themedAccent()` — in pastel light `themedAccent` resolved
  to a near-white pastel that washed out on the pale tile (the "whitish
  icons" report); ink resolves a deep same-hue twin in light.
- **v39 — filter-chip contrast + 3dp elevation + Cabinet decode cache.**
  (1) **Filter sheet contrast fixed:** `CompactChip` + `FilterGroupPill`
  light-mode inactive fills lift 0.55 → 0.82 toward `curioPillLift()` —
  the old lift still read same-y against the pale pastel wash (both the
  sheet and the chips are pastel tints); chips now go neutral cream and
  clearly separate. Dark keeps its subtle 0.04 lift + glow. (2) **Both
  states get 3dp elevation:** `shadowElevation` + `curioDarkGlow` 2 → 3dp
  on the filter chips and group pills; same contrast/elevation applied to
  the picker tabs (`PickerPageTab`) and preset chips (`PickerPresetChip`,
  0.60 → 0.82 lift, 3dp). (3) **Cabinet freeze on mass saves:**
  `CaptureRepository.observeAll()` now caches decoded `CurioEntry`s by a
  signature (id + format + capturedAtMillis + formatDataJson/tags/
  screenshots hash + sessionNote + deletedAt); Room re-emits the full list
  on every insert, so a large archive re-ran Gson decoding for EVERY row
  per save — the GC pauses froze the app. Now only new/changed rows
  decode; the map is only touched from the flow's single collection
  dispatcher.
- **v3xx53 — a text filter with teeth, the SOCIAL tab, and 24-hour messages.**
  (1) **`data/CurioContentFilter.kt`** is the app-level gate for profanity,
  explicit sexual content, slurs and harassment, plus its SQL twin
  (`curio_normalize_text` / `curio_text_is_clean` + CHECKs, §5h of
  `supabase/schema.sql`) so a modified client can't post around it. It is
  wired into every PUBLIC social write (card, note, quote, reply, username,
  display name, bio — direct messages are deliberately exempt, per the user)
  and surfaced in the composers + a permanent ban warning on the account form; the lexicon is careful to keep ordinary vocabulary (damn, hell, crap,
  body parts used plainly, identity words) usable — see the contract bullet in
  §Online layer for the fold, the two views and the whole-word rule.
  (2) **The bar's optional tab is SOCIAL now** — `CurioBottomNavItems.Social`,
  label `Social`, glyph `CurioIcons.Public` (a globe, verified in the bundled
  font subset), with the Settings switch, hub row, notification switch and
  empty states saying the same thing. The route and the `features/community/*`
  file names are unchanged on purpose.
  (3) **Messages live 24 hours on the server and stay on the device:**
  `dm_messages` gained a 24-hour read window + `curio_purge_expired_messages()`,
  `SocialCache.TTL_THREAD_MS` went 14 days → a year, and opening a thread
  MERGES (`distinctBy { id }`) the server's window into what the phone already
  has, so losing a line to the sweep is impossible while still never showing a
  stale copy as current.
- **v3xx52 — 28 portraits, redrawn shelf art, and a saved entry that stops
  disappearing.** (1) **The soft avatar set** — `SocialAvatar.kt` grew from 16
  to 28 portraits (waves, high ponytail, twin braids, hime cut, bob with a bow,
  half-up bun, flower crown, star clips, pixie with a heart clip, waves with
  glasses, afro puff, braided crown) and every face was redrawn: an iris in a
  white almond with a lash line and a catch-light, soft brows, a tiny nose, a
  hair sheen, and round glasses with temple arms. The count is now
  the single `SOCIAL_AVATAR_STYLE_COUNT` constant (see the avatar bullet in
  §Online layer): never reintroduce a hardcoded `0..15`. New top-level drawing
  helpers (`avatarFlower`, `avatarBow`, `fivePointStarPath`, `heartClipPath`)
  take resolved `Offset`/px values, because `p`/`r` only exist inside the
  Canvas lambda. (2) **Shelf art fills its surface** — `shelfScene` no longer
  letterboxes every scene into a fixed 1.25 plate; the scene box takes the
  surface's own aspect clamped to `1.15…1.65`, so the card's foot strip is
  actually used. Favorites / Curiying now / Want to Read were redrawn again
  inside it (one glossy rose heart with a gold star; a clean open book with a
  steaming mug; a hero hardcover with two leaning books). (3) **A saved entry
  never "disappears"** — `EntryDetailScreen` used to `popBackStack()` after a
  flat 400ms whenever its row had not arrived yet, and `SaveCaptureScreen`'s
  edit mode used to abort when its async prefill was still in flight, so a
  cold Room read bounced the user out of an entry that was in the Cabinet all
  along (or claimed it was "no longer available"). The page now waits for the
  first REAL emission with a 6s stall guard, and the edit save re-reads the
  row on its own coroutine. Any screen that resolves a row asynchronously
  MUST gate its "missing" verdict on a completed read, never on a timer.
- **v3xx51 — the control tick reaches every switch.** `rememberCurioControlTick()`
  (CurioPressFeedback) now also wraps the switches outside the settings family
  — Bug report's crash-log toggle, Onboarding's reminder / explore-bubble /
  pastel toggles, the Pet designer's element + face/reaction toggles, the
  reveal dialog's bubble opt-in, and the share sheet's four (polaroid-on-card
  ×2, long-fact auto-fit, include-a-link). The helper is a `@Composable`
  factory, so it must be hoisted to the enclosing composable body and used
  INSIDE the non-composable `onCheckedChange` lambda (`{ tick { … } }`) —
  never called from within that lambda (root AGENTS.md rule 3).
- **v3xx50 — the Cabinet's SKELETON system + the settings rail's vanishing
  active label.** (1) **Skeleton system** (`ui/components/CurioSkeleton.kt`):
  `CurioEntrySkeletonCard` is a 20dp-radius / 96dp-header placeholder that
  matches `CurioEntryCard`'s shape, with a shimmer sweep whose animated value
  is read in the DRAW phase (a sweeping skeleton never recomposes);
  `CabinetEntrySkeletonGrid(count, topInset, wide)` lays `count` of them out
  in the SAME grid the real cards use (2 columns on phones, adaptive on wide,
  the grid's own paddings/gaps). The count is the LAST KNOWN saved-entry
  count, persisted by the new `AppPreferences.getCabinetEntryCount` /
  `setCabinetEntryCount` (key `cabinet_entry_count`) — so a cold open paints
  exactly as many placeholders as the archive it is about to show, capped at
  12 so a huge archive can't compose hundreds of cards for one frame. (2)
  **No more empty flash on a cold open:** `CabinetV2Content` tracks
  `archiveReady` (false until the first `observeLight()` emission), gates its
  `showSuggestions` on it, and holds the home's Saved-entries slot (and the
  Saved entries / Notes shelves) with `v2SkeletonItems` until then; the
  classic `CabinetScreen` swaps its four wide boxes for
  `CabinetEntrySkeletonGrid`. Both screens remember the count for the next
  launch. (3) **Settings rail:** the active chip now paints its OWN fill the
  instant it becomes active (`SettingsRailAccent`, shared with the gliding
  shared-element pill) — the shared element only exists in the transition
  overlay (the arriving chip's own instance is hidden while the glide runs),
  so with a transparent chip the active label sat cream-on-near-white until
  the pill landed ("the text disappears for a moment for the active
  indicator"). The bounds spring is critically damped (1.0 / 420) so the
  highlight lands and STOPS instead of visibly settling.
- **v3xx49 — the collapse clock runs the header's real collapsible distance
  (Home + Profile).** A fixed 90dp clock (`StickyBarThreshold` /
  `ProfilePillThreshold`) drove a header that gives back ~122dp (Home) / ~186dp
  (Profile) of reserved space, so the page slid up faster than the finger for
  the first 90dp and then snapped back to 1:1 — the reported "jump/flicker at
  the collapse point". Progress is now `scroll / (fullReserve − compact − inset)`
  in GLASS mode, so `d(reserve)/d(scroll) = −1` and the content below the
  header stays exactly under the finger while the header collapses in place,
  reaching the compact floor precisely when the clock hits 1. Torn-paper mode
  keeps the 90dp clock: there it only drives the floating pills' pop/frost
  morph.
- **v3xx48 — the glass header really collapses, the reveal is faster +
  tappable, the Cupboard regains its size ladder and its switch animation,
  the text-history CURRENT pill is fixed (user 2026-09-11).**
  (1) **`CurioGlassToolbarMorph` — CLIP BEFORE the size-reporting layout.**
  `.layout { layout(w, lerp(full, compact + inset, eased)) }` reports the
  animated height but measures its child at the CONTENT's natural height, so
  a `clipToBounds()` placed INSIDE it clipped against the full hero and did
  nothing: the glass kept painting its whole expanded area ("the glass
  extended area stays in its initial size where the stats was") while only the
  reservation shrank. The clip now WRAPS the layout node, so it is exactly
  the animated height from y=0 (the status-bar strip stays covered) and the
  full content is trimmed as the bar collapses.
  (2) **Screen reveal (`CurioRevealNav` + `ThemeTransition`).** 440ms → 300ms
  and the settle beat 32 → 20ms ("make it more faster"), and the frozen frame
  NO LONGER swallows input: the overlay's consuming `pointerInput` is gone, so
  the overlay draws above the destination without claiming a hit and quick
  taps land on the screen that is really there ("mid transition i cant tap
  anything").
  (3) **Cupboard wall (`buildCupboardShelves`).** The size ladder is back as
  SHELF HEIGHTS — 0.50 / 0.28 / 0.34 / 0.21 of the wall width, cycling, so the
  smallest shelf is ~0.34x the feature one and the wall keeps the big/small
  contrast the old 3x…0.5x tiers had ("it doesnt have that 3x sizes or smaller
  one like .5x") while each shelf still fills the width exactly. A filter
  switch also FADES the wall into its new arrangement again (`wallSwap`
  Animatable, 0.25 → 1 over 280ms, per-shelf alpha + a whisper of scale) —
  packed shelves can't slide covers to new slots the way the old grid's
  per-cover lazy items did, so the swap needed its own motion.
  (4) **`HistoryCurrentPill` + the list-view meta row.** The pill wears the
  theme accent (primary wash + 28% rim, extra-bold tracked label) instead of
  two hardcoded browns, and refuses to wrap; the row's TIME is flexible
  (`weight(1f, fill = false)`) so the fixed delta + pill can never be pushed
  past the row's rounded clip and cut off ("the current pill is broken and
  looks off").
- **v3xx47 — the app-wide TOUCH feedback is a pressed LOOK, not a ripple
  (`ui/theme/CurioPressIndication.kt`, NEW; wired in `CurioTheme`).** The user
  meant the TOUCH highlight, not the selected state: "remove weird selection
  highlights … the touch highlight, for all around the app … maybe with
  animation or a pressed look" — Material's expanding ripple reads as a
  foreign blob on this app's rounded cards, pills and rows. `CurioPressIndication`
  is an `IndicationNodeFactory` whose node paints the element into an offscreen
  layer and tints it with `BlendMode.SrcAtop` (press in 90ms, out 260ms,
  `onSurface` at 0.14 alpha), so the wash lands ONLY on pixels the element
  already drew and therefore follows its own shape — no ripple geometry to size
  against, nothing bleeding past a card's corners. Provided once at the theme
  root via `LocalIndication`, so every `clickable` / `selectable` / Material
  surface / indication-reading control wears it, bottom sheets included.
  KNOWN TRADE-OFF: an element that paints NO background (a bare `TextButton`
  label) only tints its glyphs — Material's ripple used to add a circle there.
  At rest the node draws straight through (no layer, no cost).
- **v3xx46 — the shared PRESS FEEDBACK + haptics primitive
  (`ui/components/CurioPressFeedback.kt`, NEW; user 2026-09-11: "Press
  feedback … and more haptics all over the over").** `Modifier
  .curioPressClickable(...)` is a drop-in for `Modifier.clickable(onClick)`:
  it owns a `MutableInteractionSource`, reads `collectIsPressedAsState()`,
  scales the surface on `CurioMotion.Springs.Press` (quick snap-back, no
  rubbery overshoot) and fires ONE light haptic on the DOWN edge only
  (`LaunchedEffect(pressed)`, `HapticFeedbackType.TextHandleMove` — the same
  tick the nav pills use). The ripple is carried over from
  `LocalIndication.current`, so no surface loses its press wash, and
  `Modifier.scale` is a draw-only transform — neighbours never reflow while
  a surface squishes. Scale is a parameter because the same 0.94 reads as a
  press on a chip and a jump on a full-width card: chips 0.96, settings rows
  0.975, Home's saved/pinned rows 0.975, secondary cards 0.98, the big hub
  cards 0.985. Applied to `SettingsOptionRow` (so EVERY settings-family row
  ticks), the hub's `SettingsDesignCardView` / `SettingsSecondaryCardView`,
  `SettingsQuickTools` chips, and Home's `SavedEntryRow` / `PinnedTopicRow`.
  Surfaces that already own a haptic or their own press handling were left
  alone (the nav pills, the v244 settings search pill's `indication = null`
  interaction source).
  **Extended the same cycle:** `rememberCurioPressSource()` is the press half
  for surfaces that OWN their gesture — Material3's clickable `Surface` and
  `combinedClickable` cards that only accept an `interactionSource`; hand the
  returned source to that parameter and the returned modifier to the surface's
  chain (both halves are required — the surface must observe the SAME source).
  Used by `V2ShelfCard` (CabinetShelves, `combinedClickable`) and Profile's
  `SettingsNavCard` (clickable `Surface`). `rememberCurioControlTick()` wraps
  an ON/OFF control's callback with the one tick — wired into
  `SettingsOptionSwitchRow` (which every settings + Experiments switch is
  built on, so it covers the whole family), Book covers' fetch switch and
  Manage categories' per-lane visibility switch. NOTE: sheets do NOT tick on
  open — that needs a shared sheet wrapper (there are ~60 `ModalBottomSheet`
  call sites), and ticking only the handful that were edited would read as
  inconsistent.
- **v3xx45 — SCREEN REVEAL experiment, packed Cupboard shelves, the .jsx
  Text-history tree, plain caption box + cover-true album sheet (user
  2026-09-11: "similar to the dark mode and light mode transition cant we
  use that for like settings or profile open … make it a toggle and also
  make it faster a little", "the album colors are fully different", "the
  text history doesnt match the concept .jsx", "the changes view is bad the
  compare is the better one", "the add a caption field is note paper style
  change it to just a text box").**
  (1) **Screen reveal (`navigation/CurioRevealNav.kt`, NEW).** The
  light/dark flip's feathered circular iris now also opens SCREENS, gated by
  a Settings ▸ Experiments toggle (`AppPreferences.screenRevealEnabledState`,
  default OFF). It CANNOT wrap the ~120 `navigate()` call sites, so the
  frame is captured optimistically on pointer-DOWN
  (`Modifier.trackRevealTaps()` on the NavHost root Box, Initial pass only —
  no hit-test change) and stashed in `CurioRevealNav`;
  `NavController.addOnDestinationChangedListener` then plays it via
  `CurioThemeTransitionState.startTransitionWithFrame(frame, center)` — a
  non-suspend arm of the SAME transition machinery the theme flip uses
  (`captureFrameNow()` is the new grab-only snapshot). No fresh frame
  (back/pin/deep-link/experiment off/failed capture) = the normal page
  transitions run untouched, so it can never wedge navigation. Shared-
  element routes (`reveal`, `pet-designer`) opt out entirely — their
  hand-tuned morph is better than an iris. While the reveal owns a
  navigation `CurioRevealHost.suppressDefaultTransition` makes the NavHost
  return `EnterTransition.None`/`ExitTransition.None` so the iris is the
  ONLY motion. Faster than the flip: `revealDurationMs` 440 (vs 680) and
  `revealSettleDelayMs` 32 (vs 80), both restored in
  `finishTransition()` so the next theme flip keeps its own timings.
  Frames are reused within 700ms and recycled aggressively.
  (2) **Cupboard wall = packed SHELVES (`CabinetV2Content.kt`).** No span
  tweak can fix a grid line: it shares ONE height, so a tall book beside
  two small albums always left a hole (the reported "space left below
  those 2"). The wall is now `BoxWithConstraints` → `buildCupboardShelves()`
  packing covers into rows at a COMMON height (each cover's width = height x
  its own aspect, so widths + gaps sum to exactly the measured width), one
  `LazyColumn` item per shelf. The old 8-column `LazyVerticalGrid` span
  cycle + `mediaTierIndex` are gone.
  (3) **Text history = the .jsx tree (`ui/components/TextHistory.kt`).**
  New `LineageRail`: one continuous rail, every version its own NODE CARD,
  sessions + versions rendered NEWEST FIRST (the CURRENT version leads with
  its badge), and each node's tap opens the word-level COMPARE against the
  snapshot chronologically before it (`onComparePair`, resolved from a
  `prevOf` map built across session boundaries). The inline "changes" diff
  list + its +/− word counts are GONE — compare IS the changes view.
  (4) **Caption + album/book sheets.** `PaperLineField(paper = false)` is a
  genuinely plain box now (theme `surface` + 1dp `outlineVariant` hairline,
  not the papery `surfaceVariant` wash); `ChapterNoteField`'s placeholder
  drops its 0.85-alpha fade; `AlbumNotesSheet` keys its palette off the
  AUTHORED cover (`topic.imageUrl`) first — the same art `AlbumCoverPoster`
  shows — so the sheet can no longer be tinted from a different image.
- **v3xx44 — settings rail highlight lands with the tap + text-history
  tree badges (user test feedback: "the active indicator in settings top
  rail too slow and feels broken").** (1) **`SettingsRailBoundsTransform`
  (SettingsHubScreen.kt) 0.8/140 → 0.9/320** (~350ms → ~200ms settle):
  the highlight arrives with the tap and still visibly travels instead of
  snapping. (2) **The rail no longer moves AFTER composition:** the old
  `LaunchedEffect(active)` + `scrollToItem` centring correction snapped
  the whole header sideways one frame after every section switch, right
  under the gliding pill — that is the "feels broken". The row now
  composes ALREADY centred: the rail geometry is fixed (82dp chip +
  7dp gap), so `railInitialOffset` is computed from
  `LocalConfiguration.screenWidthDp` and handed in as
  `initialFirstVisibleItemScrollOffset`. (3) **TextHistory.kt tree:** each
  version node's +/− badge now compares against the FIELD's previous
  snapshot walked ACROSS session boundaries (a running `prevEntry` in
  `HistoryFieldCard`) — a session's first node used to report its whole
  text as "+N"; the version connector Box is a fixed `height(18.dp)`
  instead of `fillMaxHeight()` (which measured 0 in an unbounded
  LazyColumn item, so the stub/dot drew outside their own bounds).
- **v3xx43 — Favorites = liked TOPICS, Everything → CUPBOARD, stable
  wall sizes, instant Cabinet, glass headers to the status bar + a real
  collapse (user 2026-09-10).**
  (1) **Duplicate shelf labels gone** — `V2DetailHeader` (CabinetV2Content)
  dropped its name/count block (the pinned hero already prints the
  collection's name + item count) and lost its `name`/`count` params; it
  is now the actions strip (Add + kebab) with `Arrangement.End`.
  `v2VirtualShelfItems` lost its `v-head` section header entirely (same
  duplicate on Saved entries / Notes) and its unused `onAdd` param.
  (2) **Favorites is the LIKED-TOPIC shelf** — new `likedTopics` in
  `CabinetV2Content` reads `AppPreferences.topicSentimentsState`
  (SENTIMENT_LIKE, key `CATEGORY:topicId`) and resolves each id against
  the warm lane pools (`TopicJsonLoader.cached`, guarded by a
  `catalogReady` poll with a 3s cap), deduped + name-sorted. The level
  renders through the new `v2LikedTopicItems` / `V2LikedTopicRow` — a
  category-tinted glyph tile + name/byline/lane row that opens the
  topic's real reveal (skeleton rows while the pools warm, doodle empty
  state after). `shelfCounts[FAVORITES]` and the hero subtitle now count
  liked topics; `V2ShelfId.FAVORITES` no longer shares the media wall.
  (3) **Everything → CUPBOARD** — hero title + subtitle, the home card's
  title/subtitle/count (liked media only, no captures), its empty rail
  copy, the no-match state and the open-everything content description
  all say Cupboard now; `heroSubtitle` reads "Books · albums · series"
  and the home fallback line reads "Collections · Cupboard · your
  keepsakes". The internal level key stays `"everything"` (no state
  churn); its copy and card count are media-only.
  (4) **Stable wall sizes** — `mediaTierIndex` maps every liked item
  (`KIND|NAME`) to its position in the FULL name-sorted media list, and
  `v2EverythingMasonryItems` takes that map so a cover's size tier comes
  from its stable position, never its rank in the filtered list:
  filtering the wall re-flows it without any cover changing size.
  (5) **Cupboard Add = catalog search** — new `V2CupboardAddSheet`
  (`showCupboardAdd`): a search field over the BOOKS / ALBUMS / SERIES
  lane pools (sync, warm cache), rows via the existing
  `AddTopicPickRow`, tapping toggles the item's media favorite via
  `toggleLikedFavorite` — so pinning IS liking and the wall updates
  live (an empty query lists what is on the wall, doubling as the
  manager). The old Add-to-Favorites sheet mode (`AddTarget.Favorites`)
  is now unreachable — liking topics happens on the reveal.
  (6) **Instant Cabinet** — `CaptureRepository` keeps
  `lightSnapshot` (`@Volatile`, written by `observeLight`) and exposes
  `peekLight()`; `CabinetScreen` + `CabinetV2Content` seed their
  `produceState` from it, and `CabinetScreen` gains `entriesReady`
  (flipped by the first real DB emission) so a cold first frame shows
  quiet card skeletons instead of the "Your Cabinet is empty" doodle.
  (7) **Glass headers reach the status bar** —
  `CurioGlassToolbar` / `CurioGlassToolbarMorph`
  (ui/components/CurioGlassToolbar.kt) moved `statusBarsPadding()` from
  the bar to its CONTENT (the leading Row / the full Column / the
  compact Row), so the capsule fills the status-bar strip; the morph
  bar's collapsed height is now `compactH + WindowInsets.statusBars`
  (`getTop`), so the compact row is never clipped. Home + Profile
  reserve `compact + inset` at full collapse.
  (8) **The morph bar really collapses** — the reservation spacer was
  static (Home 200dp / Profile 264dp), which left the page looking
  permanently expanded. `HomeScreen` hoists
  `homeStickyProgress` above the scroll content and reserves
  `lerp(HomeGlassToolbarFullHeight, HomeCompactHeaderHeight + inset,
  ease)`; `ProfileScreen` hoists `profileStickyProgress` the same way
  and passes `reserveHeight` into `ProfileHero` (new param, default
  `ProfileHeroTotalHeight`), so the list rises as the bar shrinks.
  (9) **Queued polish** — the GalleryWall caption is a PLAIN text box
  (`PaperLineField(paper = false)`; the per-field style/color values
  still feed the board's new quote cards + the saved payload),
  `ChapterNoteField` (book sheet's add-note box) takes the sheet's
  `surfaceHigh` / `onSurface` / `onSurfaceVariant` and wears a 1dp
  hairline instead of ink-alpha washes (the dark-mode gray-smudge fix),
  and `AlbumCoverPoster` gained `resolvedUrl` — the album notes sheet
  seeds the poster with its palette's own artwork URL, so the poster
  and the sheet colors can never disagree (the "album colors are fully
  different" bug).
- **v3xx42 — Shelf-art final pass + nav rail highlight GLIDE + back
  mid-animation fix (user 2026-09-10: "curying now the book itself is
  bad just the book… saved entries properly redesign… completed keep it
  minimal… custom ones do something unique… help and feedback from
  settings… top nav bar isn't smooth, tapping goes solid colour without
  that highlight… back mid-animation still stays in that page").**
  (1) **CabinetShelves.kt arts:** ReadingArt's BOOK redrawn — dark COVER
  slab (rounded, own spine crease) under THREE stepped page layers per
  side (peek at the outer edge), typed paragraph lines per page (left
  justified / right first-line indent), spine join + crease shadow,
  knotted ribbon with V-tail; mug kept (nudged to 0.86w). PhotosArt
  redesigned as a COLLAGE — one big front polaroid (sun + two hills +
  birds, drawn unrotated in the photo window) with angled washi tape +
  a golden `fiveStar` mark, moon-night and heart prints behind,
  grounded by the shared shadow. PeakArt kept MINIMAL — one clean
  summit + snow tip + planted flag + thin ground band (sun/ridge/
  birds/badge removed). The four custom MINIMAL_* scenes are now
  UNIQUE: hot-air BALLOON (envelope, band + seam, ropes, basket,
  cloud), ringed PLANET (globe + atmosphere line + front/back ring
  arcs + moon + twinkles), SAILBOAT (hull, mast, sail, pennant, waves,
  wind puff), KITE (diamond + spars + bow tail + wavy line to a tiny
  hand + cloud). (2) **SettingsHubScreen.kt:** the CHAT (Help &
  feedback) doodle is a support-chat WINDOW (header bar dots + title
  line, incoming white ? bubble, outgoing message bubble with ink
  lines, paper plane flying out). `SettingsRailBoundsTransform` spring
  goes 0.95/500 → **0.8/140** (the stiffness-500 pill settled ~150ms
  — under half the 450ms page fade — so taps read as a solid snap;
  now it visibly glides); rail centering is INSTANT (`scrollBy` after
  one frame, replacing the ~300ms `animateScrollBy` that dragged the
  shared-element pill's target bounds mid-morph). (3) **CurioNavHost
  transitions:** settings-family enter/exit/popEnter/popExit drop the
  `scaleIn/scaleOut(0.985, Springs.Calm)` (stiffness 750 — instant
  pop) for pure `fadeIn/fadeOut(tween(Morph))` — the pill morph is the
  only motion, and tween fades converge cleanly when the user pops
  BACK mid-transition (a re-targeted spring left the old page stuck on
  All Settings).
- **v3xx41 — Everything wall: uniform covers + NO seam, and the Text
  history concept feature set (user 2026-09-10: "remove the background
  fill… covers keep the shape they are in… 2x/3x/.5x in both width and
  height… implement all the features of text history from the JSX").**
  (1) **Everything masonry (CabinetV2Content.kt)** — the seam plate is
  REMOVED (the grid's old background fill boxed every inter-cover gap;
  covers now sit on the page's own surface) and the size tiers are
  UNIFORM scales: each cover keeps its own aspect (book 0.667 / album
  1 / series 0.72) and the tier widens + tallens it together. The grid
  runs on `StaggeredGridCells.Fixed(8)` base columns with
  `spanUnits = (tier * 2).toInt().coerceIn(1, 8)` so 3x → 6 spans, 2x →
  4, 1.5x → 3, 1x → 2 and 0.5x → 1 — a REAL half-size cover (the old
  4-column grid could never render 0.5x). (2) **Text history concept
  (TextHistory.kt, from TextHistory (3).jsx)** — (a) SEARCH: frosted
  `HistorySearchBox` pill (BasicTextField + placeholder + clear)
  matching text or field label, live in BOTH views; (b) FILTER chips
  (All / Edits / Initial / Pinned) — Initial = each field's first
  snapshot, Edits = the rest, computed per field; tree is rebuilt from
  the filtered set and a `SearchOff` no-results state offers Clear
  filters; (c) COMPARE: a `Layers` action on every row arms a banner
  ("tap another snapshot's compare"), the second pick opens
  `CompareVersionsDialog` with a WORD-LEVEL LCS diff (`diffTokens`,
  backtracked longest-common-subsequence) — removed words strike in red
  (errorContainer), added words warm-highlight (tertiaryContainer), two
  side-by-side `CompareVersionCard`s + removed/added word-count chips;
  (d) COLLAPSIBLE groups: `HistoryFieldCard` headings toggle
  `collapsedFields` with a KeyboardArrow chevron + `AnimatedVisibility`;
  (e) CURRENT pill (`HistoryCurrentPill`) on each field's newest
  snapshot + word-delta badges (`wordDeltaBadge`: "+3 words" / "Original")
  in list rows and tree nodes; (f) the full-text preview gained stats
  (words · characters) + a changes note (vs the previous snapshot of
  the field) + Compare / Restore actions; (g) the header count reads
  "X of Y snapshots" while filtering.
- **v3xx40 — Cabinet doodle rebalance + settings hub polish (user
  follow-up 2026-09-10, 9-item batch).** (1) **Doodle empty state** —
  `CurioDoodleEmptyState` drops the leaf SPRIG (it read as a lopsided
  tree): the book stack breathes wider, gains a spine tick on the bottom
  book, and the note card leans against the stack with a corner fold.
  (2) **Shelf arts rebalanced** (CabinetShelves.kt) — every scene's hero
  is bigger + CENTRED with a shared `groundShadow` helper (dark ink at
  0.10/0.18 alpha, tied to the card foot) replacing the far-off filler
  dots: `ReadingArt`'s book grew (half 0.36w) with a spine crease line +
  a rounded mug (saucer, handle, taller S-steam), `PeakArt`'s near peak
  is TALLER with the flag planted ON the summit + a zigzag-hem snow cap
  + thinner ground band, `PhotosArt` fans three prints from a shared
  base point (bigger 0.34w prints, photo inner-shadow hairline, angled
  tape, a paperclip detail), the four MINIMAL_* customs re-centred (sun
  rises from a horizon with hills, rings gain a second orbit dot, wave
  gets two drops, dots drift along a drawn arc), `StarArt` centred at
  0.38u. (3) **Per-card texture RANDOMIZED** — `SettingsCardTexture`
  seeds 3 outlined bubbles + 5 speckle dots from the card id
  (`kotlin.random.Random(seed.hashCode())`, remembered per id), so every
  settings card wears a different-but-stable scatter instead of the
  same six dots. (4) **Rail auto-centre** — `SettingsNavRail`'s
  `LaunchedEffect(active)` glides the row (`animateScrollBy`, calm
  spring) so the ACTIVE chip sits mid-viewport, not glued to the left
  edge (skips when drift < 10% of the viewport; one frame settle first).
  (5) **Quick tools popUpTo** — the rail's quick-tool chips now navigate
  with `popUpTo(SETTINGS) { inclusive = false }` like the rail chips, so
  deep pages REPLACE each other instead of stacking back-presses. (6)
  **Backup doodle REDRAWN** — the CLOUD visual is an open ARCHIVE BOX
  now (lid + label plate + two file folders peeking out + a curved
  restore arrow looping back in): the old four-lobe cloud + up arrow
  was the Backup icon drawn bigger. (7) **Manage Categories drag
  visibility** — the dragged row swaps to a LIFTED CARD shell
  (`shadow(6dp)` → clip → opaque surfaceContainerHighest / #F7F1E6 fill
  → 1.5dp primary outline, full alpha, zIndex 1) so it never ghosts
  into the rows it slides over. (8) **Footer note** — the ✦✧✦ dots line
  is gone; the light panel is now a `lerp(background, settingsRoseAccent(),
  0.07f)` blend with a hairline border (was the hard beige #E9DFD4
  block). (9) **Secondary cards = card doodles** — Recycle bin /
  Updates / Help & feedback traded the plain white rows for compact
  members of the big-card family: tone gradients (STEEL/SAGE/LAVENDER),
  frosted tile + round arrow + three NEW visuals (`TRASH` = ribbed can
  + tilted lid + paper ball, `REFRESH` = two chase arcs around a version
  chip, `CHAT` = question bubble + reply bubble with ink lines).
- **v3xx37 — Cabinet lag fix: light entry projection + batched cover
  warmer (user follow-up 2026-09-09, logcat showed repeated 15–52MB
  GCs + 89/43 skipped frames while viewing the Cabinet).** (1) **Light
  list flow** — `CaptureDao.getLightFlow()` selects ONLY the light
  columns (id/topic identity/format/capturedAtMillis/title/tags/legacy/
  sessionTimeMillis/pageCount/episodeCount) plus a `CASE WHEN format =
  'ReelNotes' THEN formatDataJson END AS formatDataJsonLight`, and
  `CaptureRepository.observeLight()` maps rows → `CurioEntry` with its
  own signature decode cache. The old `SELECT *` re-read + re-allocated
  every payload JSON blob (`formatDataJson`/`sessionNote`/
  `sessionScreenshotsJson`) as fresh Strings on EVERY flow emission —
  megabytes of churn per DB write on a large archive, GC pause after GC
  pause. The Cabinet screens (CabinetScreen + CabinetV2Content) and
  Spin's saved-topic-ids collector now use the light flow; detail pages
  keep the full `observeById`. `CaptureEntityLight.toEntry()` shares
  `fallbackTopicFor` (extracted from `CaptureEntity.toEntry()`) and
  keeps full fidelity for ReelNotes reviews (rating + text render from
  the carried payload). KNOWN TRADEOFF: multi-section Portfolio takes
  with a non-ReelNotes first section decode to an empty Portfolio in the
  GRID only, so their card shows the single-glyph badge instead of the
  stacked one (the payload is never read for those rows — the stacked
  badge needs it); detail pages are unaffected.
  (2) **Batched cover warmer** — the Cabinet warmer now runs whole on
  Dispatchers.IO, pre-warms EVERY liked cover's dominant color (so
  composition-time `dominantCoverColor` hits the cache instead of
  decoding a bitmap on the main thread), downloads missing covers with
  `ensureLocalCover(..., bumpVersion = false)` and bumps
  `CabinetCoverCache.version` ONCE after the batch — the old per-save
  bump recomposed every version-keyed tile per download (30 downloads =
  30 grid-wide recompositions + 30 main-thread decodes on first open).
  `dominantColorCache` is now a `ConcurrentHashMap` (written by the IO
  warmer, read by composition).
- **v38 — onboarding proportions + page-pill indicator + reveal quick-fact revert.**
  (1) **Hero/tear deeper:** the onboarding torn-rose hero deepens 0.70 →
  0.76 of screen height so the tear sits just above the page pills and the
  dead band between the dots and the Skip/Next controls disappears.
  (2) **Wordmark ↔ slide spacing:** the 6dp spacer under the pledge became
  10dp and the pager gained `top = 8.dp` / `bottom = 26.dp` so the slide
  content centers evenly in the banner. (3) **Page indicator → pills:**
  `PageDot` is a proper indicator now — the active page is a 22×8dp
  capsule, the rest 8dp circles (no more 12dp box × 1.2 scale blob); the
  row drops to `vertical = 12.dp` with even 3dp gaps; the unused
  `ui.draw.scale` import was removed and `foundation.layout.width` added.
  (4) **Reveal quick fact:** `TeaserCard`'s fact body reverts from
  `CurioEditorialBody` (Lora) to `MaterialTheme.typography.bodyLarge`
  (spacer 12 → 10dp) — the Lora voice stays on the ActionPromptCard
  instruction and onboarding subtext, only the quick fact goes back.
- **v37 — hero controls return + reveal pill polish + compact wildcard filters.**
  (1) **Cabinet/Topic Browser controls back INSIDE the hero:** the v34
  below-hero controls row is gone — the Sort dropdown + Search pill (and
  the selection pills Clear/Select-all, Delete, Cancel) ride the hero's
  top row again via the existing `trailing` slot (`CabinetHeroHeader`
  keeps its `(ink, backdrop)` slot; `SettingsHeroHeader`'s `trailing`
  passes hero `ink`). `contentTop` reverts to reserving only the single
  Category pill row below the banner; `CabinetControlsRowHeight` /
  `DatabaseControlsRowHeight` constants are removed. The sort dropdown
  keeps its capsule 50dp pill + `CurioDropdownMenu` accent-themed menu
  (accent = active filter's `themedAccent` in Cabinet, rose in the
  database). (2) **Reveal:** the duplicate category eyebrow pill inside
  the HeroCard is removed (the top-bar chip already shows the lane); the
  top-bar category chip + pin + close now wear `cat.categorySurface()`
  (theme-aware tint in every mode instead of flat surfaceVariant);
  Express yourself stepped Bold → ExtraBold to match the Start exploring
  CTA; the ActionPromptCard's trailing arrow is gone; the hero's action
  badge / byline / subtype pills use a new `pillGlass` — strong white
  glass on pastel-light heroes, page-background lift on dark — instead
  of the washed `ink.copy(alpha = 0.18f)`. (3) **Wildcard filter
  compaction (FilterSheet only):** `buildFilterGroups` caps Type at the
  top-8 most frequent subtypes when a pool exceeds 8 (the wildcard
  surprise deck merges every category — its raw list was a 60+ chip
  wall; individual categories keep their full list), and the TYPE group
  renders in a compact 2-column `LazyVerticalGrid` (`heightIn max 160dp`,
  no scroll) instead of a full-width flow stack. (4) **Sparse groups
  filled out:** genres/eras/origins caps rise 4/4/3 → 8/6/6 so
  categories with fewer than 4 options expose more filters.
- **v35 — typography pass: Lora serif + reveal hierarchy + icons.**
  (1) **New font:** `app/src/main/res/font/lora.ttf` (Lora variable,
  OFL) bundled; `LoraFontFamily` (multi-entry variable pattern like
  geom) + `CurioEditorialBody` (17/27sp) and `CurioEditorialLead`
  (18/29sp SemiBold) top-level styles in CurioTypography.kt. Long-form
  reading text now uses the serif: the reveal teaser/quick-fact body,
  the ActionPromptCard instruction (15/23sp), and the onboarding intro
  subtext (18/27sp on the rose hero). Handwriting/journal fields keep
  Patrick Hand. (2) **Global type polish:** `bodyLarge` letter-spacing
  0.5 → 0.3sp, `titleLarge` SemiBold → Bold. (3) **Reveal hero
  hierarchy:** a small-caps category eyebrow pill (`displayName` caps,
  labelSmall ExtraBold, 1.5sp tracking) sits above the 34sp title, and
  the action badge's plain dot is replaced by the verb's own icon
  (`verbIcon(action.verb)` — headphones/play/book/restaurant…).
  (4) **Reveal top bar:** a frosted category chip (glyph + caps name,
  `weight(1f, fill=false)` keeps the pin/close group end-aligned).
  (5) **TeaserCard:** the inverted hierarchy is fixed — the
  titleSmall label became a small-caps kicker (labelSmall ExtraBold,
  1.2sp tracking, category ink) and the fact body reads in
  `CurioEditorialBody`; the flat sparkle is now a lightbulb
  (`CurioIcons.Lightbulb`, new constant) in an accent-tinted circular
  tile. (6) **ActionPromptCard:** trailing `arrow_forward` affordance
  + serif instruction. (7) **Onboarding:** intro paragraphs (welcome,
  permissions, theme, search-engine slides) read in Lora.
- **v34 — Cabinet/Topic Browser hero tidy-up + version bump.**
  (1) **Sort pill matches the other pills again:** `CurioSortDropdown`
  corners are back to the fully-rounded 50dp capsule (the v31 16dp
  corners read rectangular next to the capsule search/select pills);
  the 42dp height + tight padding keep it slim. (2) **Category pill
  below search+sort:** the Sort dropdown and Search pill moved OUT of
  the hero into a controls row below the banner, and the Category pill
  rides the row BELOW them (both `CabinetScreen` and
  `TopicDatabaseScreen`; new `CabinetControlsRowHeight` /
  `DatabaseControlsRowHeight` constants + updated `contentTop`,
  chip-bar tops and back-to-top padding). The controls row hides while
  searching (the hero morphs into the search field + Cancel pill, the
  old hero-pill behavior). (3) **Hero = clean title header:**
  `CabinetHeroHeader` dropped its `backVisible`/`onBack`/`trailing`
  params — the conditional back pill and all action pills are gone from
  the banner — and the title block sits at the TOP (no flex spacer).
  `SettingsHeroHeader` gained `titleAtTop: Boolean = false` (default
  keeps all 12 other callers exactly as before; the Topic Database
  passes true and keeps its back pill since it's a pushed screen).
  (4) **Select button removed:** the Cabinet's Select pill is gone —
  long-press enters selection (already existed); the pills in the
  controls row are page-level now (`onSurface` ink over
  `surfaceContainerHigh` glass, sort accent = active filter's
  `themedAccent` or theme primary). (5) **Selection shows only
  Clear + Delete:** while selecting, the controls row shows just
  Clear/Select-all + Delete(N) — no cancel, no category pill.
  (6) **Version:** `versionName` default 1.0.0 → **1.0.1**
  (`app/build.gradle.kts`; release tags still override via env).
- **v33 — picker pills, filter-sheet accordion, pastel-dark lane hero.**
  (1) **Category picker proper pills:** the Original / New page tabs
  (`PickerPageTab`) and the quick-mix preset chips (`PickerPresetChip`)
  grow from 4dp to 8dp vertical padding (real ~34dp pills), and their
  unselected fill now lifts toward `curioPillLift()` (cream in light,
  lighter glass in dark — `lerp(surfaceContainerHigh, curioPillLift(),
  0.18/0.60)`) so they stand off the category wash instead of the old
  `surfaceVariant`/`surfaceContainerHigh` blend that melted into the
  tinted picker. (2) **Spin FilterSheet accordion:** the Type · Genres ·
  Era · Origin · Franchise headers became tappable `FilterGroupPill`s
  (`FilterGroupKey` enum + `FilterGroups.chipsFor`) — one group open at
  a time, tap the open pill to collapse (selections survive; `null`
  stays collapsed), tap another to swap, search-narrowed groups fall
  back to the first available; chips slide in via `expandVertically` +
  `fadeIn` (tween/FastOutSlowIn) inside an `animateContentSize` column,
  with a rotating chevron and a per-group selected-count badge. The
  old LazyVerticalGrid + `GridItemSpan` section grid is gone. (3)
  **Chips raised neutral:** `CompactChip` light-mode inactive fill
  lifts to `curioPillLift()` at 0.55 (was White at 0.32) so unselected
  chips read as neutral raised pills off the pastel sheet; gains a
  `fillMaxWidth` param (false in the accordion FlowRow so chips wrap
  at natural pill width). (4) **Filter sheet background:** the sheet
  container now wears `categoryBackgroundWash()` — the same soft page
  tint as the Spin page — instead of the stronger card-level
  `categorySurface` that read as the raw hero color (Material keeps its
  device surface). (5) **Pastel-dark lane hero darker:** `headerAccent()`
  steps pastel-dark banners down (lightness x0.80, floor 0.30) whether
  or not the Deeper header toggle is on; the plain rose hero keeps its
  deep `HomeRosewoodDark` twin untouched. (6) **Hero picker rename:**
  the greyed "Sky azure hero" option (and its hint) is renamed
  "Azure hero" — behavior unchanged (still visible but unselectable).
- **v32 — non-pastel peek/hero color fixes + pastel-dark readability.**
  (1) **Non-pastel deck peeks** (`SpinScreen.PeekCard`) step like pastel
  ones — an HSL lightness drop (light 0.14/0.20 near/far, dark
  0.11/0.16) + the 0.75x saturation pull — instead of the old black-lerp
  slabs (0.40/0.52) that read near-black in light mode; the deck keeps
  its hero-brightest hierarchy with visible gradients. (2) **Category/
  lane-colored heroes calmer + readable in non-pastel:** `headerAccent()`
  pulls saturation ~15% (cap 0.60) so a vivid lane accent isn't
  blinding, and the shared-hero inks (`settingsReadableInk`/
  `homeReadableInk`/`profileReadableInk`) now resolve via
  `heroLaneCategory()?.heroHeaderInk()` — white/cream on the deep
  accent — instead of the fixed dark `onSurface` that made lane-banner
  text invisible in non-pastel light. (3) **Paper stat card dark =
  hue-matched deep paper** (`paperStatCardColor`): dark mode builds the
  deep paper from the base/hero HUE (per-screen color-aware — Home rose,
  Profile hero, a detail page's category) instead of the fixed muddy
  brown, with a whisper of warm brown so it still reads as paper.
  (4) **Pastel-dark control text:** the Categories/Filter bottom-bar
  labels (`deckControlInk` in `SpinScreen`) and Topic Reveal's
  Start exploring / Express yourself flip to the bright cream-white
  (`pastelFillInk`) the heroes use, and `themedButtonFill()` deepens the
  pastel-dark fill (lightness x0.82) so the buttons pop off the page
  wash. (5) **Orbit dots pastel dark** carry their color again: the
  85%-white `pastelFillInk` resolution for the dots is overridden to a
  ~60% white-lerp so they stay light on midnight but clearly tinted.
- **v29 — capture attach boxes are OPAQUE.** The border-removal pass left
  the translucent `category.tint` (accent @ 20% alpha) attach boxes
  looking broken (v27n rule: translucent fills bleed the elevation
  shadow). New shared `categoryTintFill(accent)` in
  `CaptureFormatComponents.kt` resolves the same perceived tint as an
  OPAQUE `lerp(surfaceContainerHigh, accent, 0.16f)`; `ImageThumb`,
  `AddImageButton` (Reel Notes/Field Notes = review + field notes) and
  `JournalVoiceNoteRow` (Marginalia journal voice-note capsule) all use
  it now.
- **v29 — save-capture topic strip matches the category + glow.** The
  strip fill switched from `lerp(surfaceContainerHigh, accent, 0.20f)`
  (muddy near-grey in dark) to `cat.categorySurface(surfaceContainerHigh)`
  — the SAME opaque card-family tint as the rest of the app — with
  `curioDarkGlow(3dp)` so the 3dp elevation shows in dark mode; ink stays
  `cat.categoryInk()` (deep accent in light, light twin in dark, deep twin
  in pastel) so the topic text is readable in every theme.
- **v29 — Spotify/Apple Music explore links + auto-copy.** Apple Music's
  URL gained the REQUIRED storefront segment
  (`https://music.apple.com/us/search?term=…` — without `/us/` the server
  redirects and the app never recognizes the link); Spotify keeps
  `https://open.spotify.com/search/…` (verified correct). Because neither
  app reliably hands off an in-app SEARCH from a web link, tapping
  Explore / Watch in now AUTO-COPYs `buildExploreQuery(topic)` to the
  clipboard (with a short toast) so the user can paste the topic name
  into the app's own search box. New `CurioIcons.ContentCopy`
  (`content_copy`, verified present in the bundled font subset).
- **v29 — per-topic progress UI redesigned (pill + editor + placements).**
  `CurioProgressPill` is now a COMPACT OPAQUE pill (count + optional slim
  category-accent bar) with a new signature `(topic, accent, ink,
  background, showBar)` — the old long accent-shaped control with the
  `fill`/`contentColor` params is gone. The editor is a brand-new
  `CurioProgressEditorDialog` in `ui/components/CurioProgressPill.kt`: the
  dialog CONTAINER is the category accent (opaque, content rides the
  on-accent color), a circular progress ring with big % + count, −/+ round
  steppers (±1 precise change), a stepped slider (`steps = target-1`, capped
  at 600), and ONLY Finish (quick-set to target) + Save (persist + close)
  — NO Reset, NO Cancel (dismiss = tap-outside/back). Placements: Topic
  Reveal hero shows a small OPAQUE frosted count badge at the TOP-RIGHT
  corner (`lerp(accent, White, 0.85)` fill + accent text — the old
  bottom-straddling pill that clipped during the shared-element morph is
  gone); Entry Detail shows a small pill at the hero's BOTTOM-RIGHT (tint
  background `lerp(surfaceContainerHigh, accent, 0.16)` + accent bar); the
  Cabinet card shows progress IN the hero (see below).
- **v29 — Cabinet card hero FILLS with progress.** `CurioEntryCard`'s 96dp
  hero header now renders a rising progress fill anchored to its bottom
  edge (`fillMaxHeight(fraction)` of the on-accent ink, denser at the base
  with a bright 2dp level line at the current mark) plus a small opaque
  count pill at the hero's bottom-right corner (tap → editor) — 50%
  progress = half the hero colored, 100% = fully filled. Works on AMOLED
  too (onAccent resolves to white there, so the fill reads as a brightening
  over the black-glass hero). The old bottom body strip was removed.
- **v29 — Cabinet + Topic DB category pills stop GROWING on entry.** The
  sticky chip bars' per-pill pop rested at 0.90 scale, so every pill looked
  like it was growing the moment the screen opened. `CabinetChipPop` and
  `DatabaseChipPop` now rest at FULL size (1.0) and only breathe subtly
  (1.0 → 1.05) as the bar actually pins on scroll; the color-bloom + lift
  language is unchanged.
- **v29 — device-screenshot watcher hardened.** `DeviceScreenshotWatcher`
  coalesces MediaStore change bursts (one scan pass drains all queued
  requests instead of piling up work), schedules ONE delayed re-pass
  (~1.5s) to catch screenshots MediaStore indexes a beat AFTER the change
  event (the reason a fresh shot sometimes never attached), and widens
  `looksLikeScreenshot` to also match any image filed under a
  `/Screenshots/` folder (some OEMs name shots IMG_…). The heavy query +
  file copy stay on the serialized scan thread.
- **v29 — progress never vanishes.** `TopicProgressStore.writeAll` now uses
  `commit()` (was `apply()` — an async write could be lost when the
  process died right after saving, showing as progress silently reset), and
  `MainActivity.onResume` re-seeds the in-memory progress map from prefs so
  a killed-in-background process heals on return instead of waiting for a
  restart.
- **v29 — Topic Database opens with ZERO loading (prebuilt index → runtime
  build since v174f).** `TopicJsonLoader.loadIndex()` merges every
  `assets/topics/*.json` into one cached `List<TopicIndexEntry>` with
  lowercased search keys and the sort YEAR precomputed (same precedence as
  `topicYear`: name paren → targetName paren → teaser year → instruction
  year → decade tag). `MainActivity` prewarms it at cold start.
  - v174f: the build-time asset (`scripts/build_topic_index.py` →
    `assets/topic_index.json`, 23MB, a FULL duplicate of every topic)
    STOPPED SHIPPING. `loadIndex()` now checks for the asset and, when
    absent, builds the same merged index at runtime via
    `buildIndexFromCatalog()` (routes through `load(id)` so parses are
    shared, wildcard.json read directly). The prebuilt script stays as a
    backup. The Topic Database renders from the cached index when
    present (no per-category parses, no runtime work) and falls back to
    the live per-category load only on a cold start before prewarm
    completes.
- **v28 — scrolling pets look UP/DOWN in a line, never a circle.** The v27v
  "roll" played a FULL 2π CIRCLE of the eyes on every scroll — it read as
  the pet's eyes spinning whenever you scrolled. Replaced with a vertical
  scroll-look: `PetPointer` now exposes `scrollDir` (+1 down / -1 up) and
  `scrollTick` (increments per scroll event) instead of `rollTick`; wheel
  scrolls use `scrollDelta.y`'s sign and touch-drag scrolls use the
  finger's INCREMENTAL vertical travel (2dp threshold + 60ms gate). Each
  sprite runs a `scrollLook` Animatable keyed on `scrollTick`: ease to the
  scroll  direction (150ms), HOLD while scroll events keep arriving
  (restarting the effect), then settle back to neutral ~400ms after the
  last event. The scroll look wins over the pointer aim while active, so
  dragging never mixes the aim with a spin. **v28 touch-direction fix:**
  the touch-drag branch fed the raw finger delta into the SAME mapping as
  the wheel — but a touch finger moves OPPOSITE to the content (swiping
  UP scrolls the page DOWN), so on a phone scrolling down made the pet
  look UP. The touch branch now inverts the finger delta
  (`dy > 0 → -1, else 1`), so  the pet always looks the way the CONTENT
  moves — consistent with the wheel branch (scrolling down = look down).
  **v29 removal:** scroll-following is GONE entirely — the user wanted the
  eyes "normal again, no scroll following". `scrollDir`/`scrollTick`/the
  scrollLook Animatable are removed; the eyes aim only at real taps/hover
  (the tracker cancels the aim as soon as a press starts dragging), so
  scrolling no longer moves the gaze at all.
- **v28 — dark-mode elevation visibility: soft light glow + hairline
  outline.** Compose's black shadows are INVISIBLE on the app's midnight
  surfaces, so dark mode now draws elevation two extra ways via two new
  composable modifiers in `ui/components/DarkElevation.kt`:
  `Modifier.curioDarkGlow(elevation, shape)` — a soft WHITE-tinted shadow
  (16% alpha) that reads as a gentle lift on near-black — and
  `Modifier.curioDarkOutline(shape)` — a faint light hairline (12% white)
  along the surface edge, the standard dark-UI card language. Both are
  dark-mode-only (light mode renders exactly as before: the Surface's own
  black shadowElevation) and are driven by two Appearance toggles
  (`darkGlowState` default ON, `darkOutlineState` default ON — the glow is
  the default-on look, the outline is the Appearance option). Wired into
  the shared elevated components + main screens: CurioSettingsCard,
  CurioSearchField, CurioEntryCard, CurioCategoryCard, FilterChipLite,
  hero action pills (Settings/Cabinet), CurioSortDropdown, CurioTopBar,
  PaperCard surfaces, dialog option rows, Home (stat card, recents rows,
  sticky top pills, session card, pick-a-lane), Profile (stat pane, lanes
  tiles), Topic Reveal (already-there button, teaser card), and the
  Category Picker's preset chips + Original/New page tabs + Mix button.
  Glow must precede the fill in the modifier chain (rule 11).
  **v29 removal:** the user rejected the outline look — `curioDarkOutline`
  (and its `darkOutlineState` pref + 'Card outlines' settings row) is
  REMOVED entirely; only `curioDarkGlow` remains (dark-mode-only).
- **v28 — AMOLED is BORDER-FREE (full border-removal audit).** Two
  systematic border sources were removed from AMOLED: (1)
  `Modifier.categoryEdgeShine` no longer draws its full-edge HAIRLINE RING
  in AMOLED (`hairlineAlpha = 0` — white rings around every pill/card read
  as clunky "borders" on pure black); the TOP-LIT GLASS shine is
  strengthened (0.45→0.52 with accent, 0.22→0.30 without) since it's now
  the sole edge cue. Material keeps its accent rim (its identity); the
  default Curio style was already border-free. (2) `curioDarkOutline` (the
  v28 hairline) never draws in AMOLED either. The AMOLED raised look is
  now top-lit glass shine + the v28 soft glow — no rings. Intentional
  design borders kept: CurioBadges coin rims + the Quests passport stamp
  ring (both are element identity, not elevation). **v29 exception:** the
  user asked for a border BACK on the AMOLED MAIN deck card only —
  `categoryEdgeShine` gained an `amoledHairline` param (default false)
  that redraws the hairline ring just for that card; everything else stays
  border-free.
- **v28 — Spin FilterSheet: live chip search + 1dp chips.** The deck's
  filter bottom sheet (`FilterSheet` in `SpinScreen.kt`) gained a
  `CurioSearchField` under the subtitle: typing narrows EVERY chip group
  (Type / Genre / Era / Origin / Franchise) live via a `filteredGroups`
  derivation (case-insensitive substring), and an empty search shows
  "No filters match …" instead of the plain empty message. The sheet's
  `CompactChip` selectable chips dropped from 2dp to 1dp elevation (cards
  2dp / chips 1dp) so they read as chips, not tiles.
- **v28 — Category card selected state: SATURATED, not darker.**
  `CurioCategoryCard`'s selected fill used `cardGradient` whose start is
  black-DARKENED (`categoryCardFill` 10% light / 28% dark) plus a
  `cardContentInk` sheen that is a deep ink in pastel light — so tapping
  a tile read DARKER than the idle tint. Selected now blooms to the raw
  saturated `category.accent` melting into the page, content flips to
  white (`selectedInk`), and the sheen is a true white 14% glow. `cardInk`
  / `cardContentInk` import removed.
- **v28 — Category Picker rows tightened.** The quick-mix preset chips and
  the Original/New page tabs both wore `categoryEdgeShine` (a white ring
  in AMOLED — the "huge borders") and the preset row's 6dp vertical
  padding pushed the two rows apart. The AMOLED ring removal + the row
  spacing tweaks (preset row padding vertical 6dp → top 4/bottom 1, tabs
  row top 1/bottom 4, chip vertical padding 6dp → 4dp) pull the rows
  together; both pills + the Mix button also gained the soft glow.
- **v28 — Topic Reveal hero gradient matches the Spin ticket in LIGHT
  mode.** The reveal hero's `HeroCard` used `cat.headerAccent()` (a
  0.88-deepened accent in light mode, v27j) while the Spin ticket wears
  `themedAccent()` — so the morph read a shade darker in light (dark's
  0.94 factor hid it). It also rebuilt the gradient via `cardGradient`
  while the ticket uses a different pastel-light recipe (its second stop
  IS the on-hue tint; cardGradient's is only 30% toward it). The hero now
  mirrors the ticket EXACTLY: `themedAccent()` + the same pastel-light
  stops (`lerp(accent, Black, 0.05)` → `lightAccentTint(0.22, 0.80)`) in
  pastel light, `cardGradient` everywhere else — pixel-identical morph in
  every theme.
- **v27v — custom pet designs ALWAYS win + custom-pet procedural defaults.**
  (1) The sprite's design resolution (`CurioPetSprite`) forced
  `evolutionDesign(BABY, null)` for ANY baby-stage pet (level < 15) — so
  "Save" confirmed but the pet never changed for most users. Now a saved
  custom design ALWAYS wins, regardless of growth stage; the stage-based
  evolution art only applies when NO custom design exists. Animations,
  view angles and the curled sleep pose all flow from the winning design
  automatically (a custom pet is its own new pet). (2)
  `PetDesign.withCustomPetDefaults()` — a custom pet is its OWN art:
  procedural accessories (leaf/badge/halo), antenna (nightcap + thinking
  ?), tail and belly stay OFF; only the effects layer (sparkles, Z's,
  whooshes) stays on. Blush + eyes are FACE features, not procedural
  layers, so they stay enabled automatically. Applied by  `saveAsNewPet()` and when saving updates a custom slot (plain Curie
  saves keep the working design exactly as edited).
- **v27v — 3D steel ring styles, Home tint pills, softer torn edges, paper
  detail meta card.** (1) The "Hole rings" experiment now draws REAL 3D
  steel rings through the 3 punch holes (`PaperStatCard.kt`) in three
  selectable looks — "coil" (spring wire through the hole: bright front
  arc over the paper, dark back arc receding into the hole), "split"
  (closed metal torus with a split gap + top glint), "oblique" (a few
  short coil segments springing diagonally out of the hole) — picked via a
  new Experiments → Ring style row (`paperHoleRingStyleState`, default
  "coil"; each style shares the steel gradient + contact shadow). (2) Home
  tint: `heroTintOn`/`heroFill`/`questInk` are hoisted to the top of
  `HomeScreen` so the sticky MENU + PROFILE pills wear the hero tint too
  when "Hero tint too" is on (they previously always fell back to rose);
  follow-my-Spin-lane still never tints the hero. (3) `TornStatPaperShape`
  rework: the top edge is a NEW re-seeded tear (soft waves + gentle ragged
  layer, no longer the inverted hero seam) and the three sides are SOFTER
  (amplitude 3.5→2.2dp, high-frequency octave faded) so the card reads as
  real torn paper, not spikes. (4) The paper & headers experiments now
  extend to Entry Detail's Date · Mood · Session · Type meta card — when
  "Paper stat card" is on it swaps the frosted glass for the shared opaque
  paper surface (paperStatCardFill with the same holes/rings/torn
  toggles; torn seed = per-entry tearSeed xor 0x6B4E3E).
- **v28 — dark-mode hero headers read white/creamish on EVERY screen.**
  New `CurioCategory.heroHeaderInk()` (`CategoryInk.kt`): light mode keeps
  the pastel-aware `onAccent()` resolution exactly, but DARK mode always
  resolves WHITE/creamish (`pastelFillInk(themedAccent())` — the same
  cream-white blend the shared rose heroes use), so a category-tinted hero
  never shows its tinted light twin as title text over midnight. Applied
  to the three category-tinted heroes that used `onAccent()` directly:
  Cabinet's active-filter banner (`CabinetHeroHeader`), the saved-entry
  detail hero (`heroInk` in `EntryDetailScreen`), and Home's hero-tint
  experiment title + sticky pills (`questInk`). Home/Profile/Settings/History
  heroes already resolved creamish via their `*ReadableInk` helpers —
  unchanged.
- **v28 — Settings hero tear is WHITE paper in dark mode + detail hero
  tears never flatten.** (1) The Settings hero under-sheet was the ONLY
  hero using `MaterialTheme.colorScheme.surface` (midnight in dark mode)
  with an `onSurface` rim (white-ish in dark) — so the Settings tear read
  dark/gray while every other screen's tear stayed white. It now matches
  the app-wide pattern: warm cream `0xFFFDFCF9` sheet + the same black
  0.20 rim as Home, in EVERY theme (AMOLED keeps its rose 0.45 sheet so
  the seam reads through the pure-black banner). (2) The detail-only
  "guaranteed movement" oscillation in `SoftTearParams.broadDisp` ran at
  ~2.8 cycles — nearly the SAME wavelength as the main wave — so for
  unlucky seeds it reinforced the wave's flat plateaus and the detail
  hero's torn edge read as huge straight lines (the white sheet stayed
  bumpy because its exposed lip uses its own restrained rhythm). Replaced
  with two phase-offset, incommensurate mid-frequency octaves (17π ≈ 8.5
  and 23π ≈ 11.5 cycles, ~2.1dp + ~1.3dp, amplitudes hoisted to
  `meanderA`/`meanderB` since `density` is only in constructor scope):
  the seam now ALWAYS meanders on a ~35-45dp scale for every entry hash,
  in both the hero clip and its aligned under-sheet (same `disp`).
- **v28 — hole rings now THREAD THROUGH the hole (all 3 styles).** The
  v27v rings were drawn as flat ellipses LARGER than the punch hole and
  centered on it, so they read as metal rings glued AROUND the hole
  ("just changes the look of the hole"). All three styles in
  `PaperStatCard.kt` now share a real through-hole structure:
  `drawHoleInterior` shades the punched opening dark (a deep pocket, so
  anything drawn inside reads as BEHIND the paper), the wire's BACK arc is
  a dark, smaller-radius arc receding inside the hole (coil/split/oblique
  each with their own back angles), the FRONT arc rides the hole rim in
  bright steel — its tube half over the opening, half on the paper — and
  darkened 26° DIVES at each end of the front arc show the wire sinking
  back in, plus the shared contact shadow. "coil" = spiral-notebook wire
  (front arc 145°→395° at 1.02×holeR, back arc 35°→145° at 0.72×holeR);
  "split" = keyring loop (front top half 160°→360° at 1.05×holeR, back
  bottom half 20°→160° at 0.82×holeR, split gap at 260°, rim shade over
  the back wire); "oblique" = foreshortened coil bulging out of the hole
  (front ellipse 1.35×holeR, back arc inside, per-hole tilt). All three
  now visibly pass through the hole instead of decorating it.
- **Single Support & diagnostics page (v24):** Support & diagnostics (`features/support/SupportScreen.kt`, route `SUPPORT`) is the ONE page for updates, feedback, replay intro, and the project link — the old Settings → About page (`SettingsPage.ABOUT`, `SETTINGS_ABOUT` route, `AboutSection`, `CurioUpdateCheckRow`) was removed. The page is reachable from Profile's "Support & diagnostics" row, Settings → Safety & support → "Support & diagnostics", and the Home drawer. **GitHub in-app updater (v25):** the Play Core in-app update (v24) was REMOVED for good — the app ships from GitHub, not Play. The update check in Support & diagnostics (`features/support/SupportScreen.kt`) is now GitHub-only: `UpdateChecker` (`data/UpdateChecker.kt`) parses the release's APK asset (`apkUrl` on `UpdateInfo`, from the GitHub API `assets` array) and `UpdateChecker.downloadApk(url, file, onProgress)` streams it into `cache/downloads/` with progress. "Update now" then hands the file to the system installer via `FileProvider` (`ACTION_VIEW` + `application/vnd.android.package-archive`, `cache-path apk_downloads` in `xml/file_paths.xml`) — the USER confirms the install (`REQUEST_INSTALL_PACKAGES` permission added). The card keeps a short "Open release" link as the browser fallback. **Kotlin gotcha (v25):** never write the literal `/*` sequence inside a block comment — Kotlin block comments NEST, so `release/*.apk` in a KDoc silently swallowed the rest of the file (the braces checker caught it; CI would have failed on an unterminated comment). **v428 — THE CARD'S ONE LEGAL CREDIT: TMDB'S ATTRIBUTION.** TMDB's terms make attribution a condition of the free API (*"You shall place the following notice prominently on your application"*), and its FAQ names the place: *"the attribution must be within your application's 'About' or 'Credits' type section"* — so the notice lives in the About Curio card, beside the Kyant and GitHub credits, as a row whose sentence is kept **verbatim**: *"This product uses the TMDB API but is not endorsed or certified by TMDB."* It is drawn ONLY when `TmdbFetch.isConfigured` (a build with neither credential never asks TMDB for anything, so there is nothing to attribute) and its tap goes to **https://www.themoviedb.org**, the link TMDB's branding rules require. The row is the ONLY surface that shows a brand mark instead of a Curio glyph: `SettingsOptionRow(logoRes = …)` puts `R.raw.tmdb_logo` — TMDB's **unmodified** approved "Primary short (blue)" SVG from `themoviedb.org/about/logos-attribution`, vendored byte-identical (its filename carries the asset's own SHA) — in the 40dp tile through Coil's `SvgDecoder`, and that tile **tints nothing**, because the rules forbid recolouring, reshaping and rotating the mark. `logoRes` is orthogonal to `icon` (either may be null), and `plain = true` is what lets the required sentence run to three lines instead of being cut. If the app ever shows TMDB data from another surface, the notice moves with it — the credit is a fact about the data being shown, never a decoration.
- **v223 — drawer top slot: constellation experiment + Material stat
  strip (default).** The drawer's first slot under the hero is now gated:
  `AppPreferences.drawerConstellationState` (Experiments → Constellation →
  "Drawer constellation", default OFF, seeded in initThemeMode) shows the
  full `DrawerCuriosityMap` only when ON. When OFF (the default) a new
  small pure-Material stat strip renders instead (`DrawerMaterialStatStrip`
  + `DrawerMaterialStatPane` in HomeScreen.kt): one tonal M3 card
  (`surfaceContainerLow`, 18dp corners, 1dp shadow) with a tiny "YOUR
  CURIOSITY" caption and three divider-separated panes — day streak
  (`StreakTracker`), level (`CurioQuests.levelForXp(xpState)`), saved count
  (`repo.getAll()` via produceState) — all in plain M3 roles (primary /
  onSurface / onSurfaceVariant); tapping either slot still opens STATS.
  Both slots share `CurioConstellation.plainBackground: Boolean = false`
  — the DRAWER passes `true` so the map paints ONLY the star pattern
  (lines + stars, no opaque page fill, no nebulae/starfield sky); the
  Stats page keeps the default false (full deep-space sky unchanged).
- **v223 — Material hero tears + spin experiments concluded + reveal/cabinet/
  progress fixes.** (1) NEW Appearance option **"Material hero tears"**
  (`AppPreferences.materialHeroTearsState`, KEY_MATERIAL_HERO_TEARS, default
  OFF, seeded in initThemeMode; row GREYS OUT while Material theme is off):
  when ON together with `materialThemeState`, the shared torn heroes wear
  `colorScheme.primaryContainer` (+ `onPrimaryContainer` ink) instead of the
  rose/azure default or a lane. The gate lives in ONE shared helper
  `materialHeroTearsOn()` (SettingsHubScreen.kt) checked FIRST in
  `settingsRoseAccent()` / `homeRoseAccent()` / `profileRoseAccent()` and
  their three readable-ink twins. (2) The five Spin-visuals experiments
  CONCLUDED with the new look ON — Main card shadow, Nav-style buttons,
  Top-lit deck cards, Tinted deck edges, Roomier deck titles: their
  Experiments toggles (and the whole "Spin visuals" section) are REMOVED and
  the SpinScreen reads are hardcoded `true`; the pref APIs stay dormant
  (defaults flipped true). (3) Topic Reveal's floating bar: the category
  pill's expanded width now FITS the name (TextMeasurer-measured label +
  icon/padding slack) instead of a fixed 200dp, and the favorite pill plays
  the SAME entry animation as the category pill (starts collapsed,
  springs open via `favoriteRevealed`) so an already-favorited topic no
  longer sits pre-expanded. (4) Cabinet publishes its REAL page background
  to the nav chrome — `cabinetWash` falls back to
  `heroLaneCategory()?.categoryBackgroundWash()` like the page's own
  `.background()` — killing the plain-background strip behind the floating
  nav pills on Cabinet-"All"+Adaptive Hero (Home never had it because it
  publishes homeBg). (5) Progress editor corner control REDESIGNED: it now
  shows and edits the TOTAL ("328 pages"), not the progress number (ring %
  + steppers own progress); tap opens an inline numeric field with a hairline
  border, and while editing the trailing button is a solid TICK
  (`CurioIcons.Check`, contentColor fill / surface ink) that commits
  (Enter too); the replay/reset-to-zero button is gone.
- **v231 — nav-bar squish fix + glass parallax tilt + quote slip bounds + constellation centering**
  - CurioLiquidGlassTabBar: dropped `width(IntrinsicSize.Min)` + `weight(1f)` (intrinsic-min collapsed tabs, cutting icons/labels); tabs are content-sized with a 64dp floor.
  - New Experiments toggle "Glass parallax tilt": `CurioGlassParallax` gravity listener (TYPE_GRAVITY, low-pass, dead-zone) drives a counter-tilt sway in `liquidGlassCapsule`'s graphicsLayer; listener runs only while enabled.
  - MoodBoard quote slips: default slot width 240→180px cap, resize ceiling 60%→42% of board, textScale hard-capped at 1.6× (degenerate baseW could stretch a slip over the full board height).
  - Constellation tap-centering: targets now use the letterboxed 1400-viewBox mapping (`ox + x*s`) instead of `nx*w`, and pixel offsets are divided by density before animation (double-scale overshoot).


- **v233 — scroll-crash fix (in-page glass off) + crash-log UI + classic-style glass tabs**
  - Scroll native-crash class eliminated: Home menu/profile pills and detail back/more
    pills sat INSIDE the capture subtree AND rebuilt drawBackdrop per scroll frame
    (per-frame washAlpha). Glass handoff disabled there (`glassOn = false`, code kept);
    classic solid-hero → frost morph restored. Live glass remains ONLY on the bottom-
    nav overlay (sibling of the capture Box — never crashed).
  - Native crashes surface end-to-end: checkNativeCrash → persistCrash → pending flag →
    Splash routes to CRASH screen → log displayed (no extra wiring needed).
  - Glass tabs follow the classic pill language: inactive icon-only, active springs to
    FloatingPillExpandedWidth with side label (Changa One 15sp) + accent ink crossfade;
    item API takes an index and reports measured width via LocalLiquidGlassTabMetrics.
  - Draggable indicator tracks REAL per-tab widths (tabWidthsPx + version counter,
    offsetOfFraction/widthAtFraction replace the even-split math incl. RTL + drag +
    specular highlight) and wears a constant faint accent wash so it reads at rest.
- **v233 — clear-glass option + parallax edge glow + light-mode indicator ink + glyph centering**
  - NEW PREF `glassClarityState` (`glass_clear_style`, Experiments → "Clear glass", OFF):
    when ON the glass drops its frost — blur 8dp→2dp and the container wash cut to ~35%
    in `liquidGlassCapsule` AND all three `CurioLiquidGlassTabBar` layers — so the bar
    reads like the bright press-blob refraction instead of milky glass.
  - PARALLAX v2: the v231 whole-capsule translation is GONE (imperceptible). Tilt now
    drives an EDGE GLOW instead: `LiquidGlassPills.drawGlassTiltEdgeGlow()` draws a white
    rim stroke with a radial gradient whose bright spot slides against
    `CurioGlassParallax.tiltX/Y` — applied in `liquidGlassCapsule`'s capture-guard
    drawWithContent and on the tab bar's main capsule + draggable active pill. Reads
    snapshot tilt state inside draw → per-sensor-tick draw invalidation, zero recomposition.
  - Light-mode active-indicator contrast: the constant 0.14 accent wash gave the active
    ink nothing to read against; now theme-aware (light 0.30 / dark 0.16) via a hoisted
    `isCurioDarkTheme()`.
  - Glyph centering: fixed-dp optical nudges (`offset(y = (-2f).dp)` etc.) mis-centered on
    fontScale ≠ 1 devices (CurioIcon shrinks the glyph below 1.0 but the nudge stayed).
    New `Modifier.curioGlyphInkNudge(dp)` scales the nudge by `fontScale.coerceAtMost(1f)`;
    replaced at all 9 call sites (Home menu/profile pill, Home casino/stat chips,
    Profile rows ×2, Spin die/glyphs ×2, CurioTopBar).
- **v232 — Pet Designer crash: glass off + native-crash reporter + self-heal**
  - Pet Designer still SIGSEGV'd natively (RenderThread stack overflow, cyclic render
    node) on some devices even with the v228 guard; Reveal's identical in-subtree pill
    is fine. Root cause not yet reproducible from code alone.
  - `PetDesignerScreen` studio bar: liquid-glass path disabled (solid elevated fill
    always) pending a real tombstone. Reveal/Home/detail glass unchanged.
  - `CurioCrashReporter.checkNativeCrash()` (called from `init`, API 30+): reconstructs
    native deaths via `ActivityManager.getHistoricalProcessExitReasons` — SIGNALED and
    unhandled CRASH exits land in the same history/pending/loop-window flow so the
    crash screen finally shows them and repeated native deaths trip safe mode.
  - Self-heal: if the liquid-glass experiment was ON at death, both glass toggles are
    auto-disabled before the UI comes up (noted in the persisted log).
- **v230 — liquid-glass scroll morph on the top-bar pills**
  - Home menu/profile pills and EntryDetail back/more pills: resting look is unchanged SOLID hero fill; once scrolled past the threshold the flat frost endpoint is replaced by `liquidGlassCapsule` (refraction + blur), with `washAlpha` easing 0.92→0.45 so the handoff doesn't pop.
  - Profile pill keeps the classic morph while an avatar photo is set; detail's classic path now also starts at the exact hero fill (lift applied through frostShift instead of baked into the rest color).
- **v229 — Live Update promotion fix + notification permission checker**
  - The v227 `ProgressStyle` alone never promoted: the service now calls `setRequestPromotedOngoing(true)` + `setStyledByProgress(true)` + `setShortCriticalText` (the exact LiveBridge recipe) so Android 16 can render the status-bar chip / lock-screen live activity.
  - Topic Reveal explore flow: when POST_NOTIFICATIONS is permanently denied (no rationale after denial), an app-styled checker dialog offers Open settings (ON_RESUME continues the pending session) or Start anyway — previously the runtime prompt silently no-op'd and the session ran with no visible timer.
- **v228 — liquid-glass self-capture crash fix**
  - Root cause of the Pet Designer RenderThread SIGSEGV (stack overflow in `RenderNode.prepareTreeImpl`): `layerBackdrop` records the page subtree AFTER drawing it, so glass pills INSIDE the captured Box (Pet Designer studio bar, Topic Reveal bar) re-drew during the record pass and sampled their own GraphicsLayer — a cyclic render node.
  - Fix: the NavHost's `rememberLayerBackdrop(onDraw = { curioGlassCaptureDraw() })` sets `CurioGlassPills.isCapturingBackdrop` for the record pass; `liquidGlassCapsule` wraps its backdrop node with a `drawWithContent` guard that paints a plain translucent capsule while capturing. Bottom tab bar unaffected (sibling overlay).
- **v227b/d — full liquid-glass nav port + update dialog**
  - `ui/components/liquidglass/` — four files ADAPTED FROM vFlow
    (github.com/ChaoMixian/vFlow, **GPL-2.0-or-later**; attribution headers
    in every file — this makes those parts of Curio GPL-derived):
    DragGestureInspector (raw non-consuming drags), DampedDragAnimation
    (critically-damped value spring + lagging velocity spring for
    squash/stretch + press-progress), InteractiveHighlight (API-33+
    RuntimeShader specular sheen that follows the finger), and
    CurioLiquidGlassTabBar (three stacked layers: visible refracting
    capsule, invisible accent-tinted tab row recorded into a second
    backdrop so the pill refracts COLORED icons, draggable active pill).
  - CurioFloatingNavBar renders the full tab bar when the Experiments
    toggle is ON (Android 12+; lens needs 33+ where it self-guards);
    classic expanding-pill row otherwise. Reveal/pet-studio keep the
    simpler frosted capsule.
  - The corner update TOAST is fully removed (CurioInAppToast.kt
    deleted): UpdateChecker now raises CurioUpdatePrompt.pending (global
    state, once-per-version gate unchanged) and the NavHost renders a
    themed AlertDialog — Open Updates / Later.
- **v227c — auto-backup frequency + detail entrance delay fix**
  - Auto backup: `AppPreferences.getAutoBackupFrequencyDays` (1/3/7, default 1) picked via chips in
    BackupToolsScreen; `MainActivity.runAutoBackupIfDue()` honors the chosen interval and is called from
    BOTH onCreate and onResume (the old onCreate-only hook never fired in warm processes). Due-date gate
    keeps repeat calls as no-ops.
  - `EntryDetailScreen.DetailContentEntrance`: the 200ms delay was paced to the removed Cabinet→Detail
    shared morph (v8.38 replaced it with a center pop-up), so quick fact + body appeared late. Now
    tween(260) with no delay.
- **v241 — in-screen glass (experiment) + tilt light-arc fix.**
  - New toggle: Settings → Experiments → "In-screen glass" (`AppPreferences.glassInScreenState`,
    default OFF; needs Liquid glass pills). `isInScreenGlassActive()` in LiquidGlassPills.kt gates it.
  - Architecture (the crash-safe one): each in-screen site captures a LOCAL LayerBackdrop that contains
    ONLY what sits behind the pill — Home: capture Box around page content, pills are sibling overlay;
    Entry Detail + Profile: `.layerBackdrop(...)` hung directly on the scroll Column / LazyColumn, sticky
    pill Row is its sibling; Pet Designer: LazyColumn captures, `PetStudioBottomNav` sits below it.
    The pill is never inside its own sampled subtree, so the v228 self-capture cycle is impossible by
    construction (the global guard stays as belt-and-braces). Self-heal also disables this toggle.
  - `liquidGlassCapsule` grew `backdrop: LayerBackdrop?` (null = NavHost global capture), `alwaysClear`
    (force the clear recipe), and `shape: Shape` (default CircleShape; wide bars pass
    RoundedCornerShape(50) so they don't ellipse-clip). In-screen floating pills use alwaysClear=true,
    constant washAlpha=0.45f (no per-frame effects rebuilds).
  - Parallax tilt cue redrawn: `drawGlassTiltEdgeGlow` strokes a ~110° TOP-RIM light arc that slides with
    tiltX and fades in with tilt magnitude — the old full white circle (visible on any tilted phone) is gone.

- **v228b — share-card edit precision pass** (`ui/components/TopicShareCard.kt`). (1)
  **4 box-size sliders instead of 2 target-conditional ones**: edit mode now shows Title width /
  Title height / Fact width / Fact height, each with an explicit label, a live percent readout
  and snap steps (width 1%, height ~5% — they drive whole-line counts) so the user always knows
  which dimension is being edited and can hit exact sizes (`SizeSliderColumn`). (2) **Typing caret
  accuracy**: the transparent quick-fact BasicTextField dropped its 6dp content inset and now
  types with card-matching Lora metrics (`factFieldStyle` in TopicShareSheet, ~11sp × bodyScale,
  1.5× leading) so the caret sits exactly on the visible text — the old 14sp/20sp bodyMedium
  wrapped whole lines away from the card's own 9–12sp layout. (3) **Editorial drop cap = 2 wrap
  lines**: the measured wrap beside the 2× initial is 2 lines (was 3) and the letter is top-aligned
  via `LineHeightStyle.Alignment.Top`.
- **v228 — share-card text formats + Editorial drop cap + info-row moves + title no-edit**
  (`ui/components/TopicShareCard.kt`). (1) **MS Word-style formats**: `ShareCardMove` grew
  `factFont: FontFamily?` + `factAlign: TextAlign?` (and `metaDx/metaDy` for info rows);
  `factBodyStyle(base, move)` threads the format over every style's fact body (all 8 styles incl.
  MiddleContent/quote text) — because `move` already flows into the export lambdas, the fonts &
  alignment bake into the saved PNG with zero extra plumbing. Customise panel gained "Fact font"
  (Serif/Sans/Type/Display/Elegant → null/Sora/SpaceMono/Playfair/DMSerif) and "Fact alignment"
  (Left/Center/Right) pills under the quick-fact size slider. (2) **Editorial drop cap**: `rememberTextMeasurer` +
  `TextLayoutInput` (BOM 2026.05; the `constraints`+`maxLines` combo parses ONLY as named args via
  TextLayoutInput, NOT the legacy measure override) finds how much body fits in the first 2 lines
  beside the 2× initial; that chunk renders beside it, the rest continues full-width below
  (single-char bodies fall back to plain text). `LineHeightStyle.Alignment.Top` pins the letter to
  the TOP of its 2-line box so it starts level with the first text line (a 2× line height alone
  would vertically center the glyph). (3) **Info rows movable, never editable**: `moveMeta`
  offsets byline/year/footer/colophon in every style; edit mode gained an "M" MoveHandle (bottom
  right) driving metaDx/metaDy. (4) **Title no longer type-editable**: the title BasicTextField is
  now a move/crop outline box only (T handle + edges remain); the quick-fact field keeps typing;
  ArrangeableCard dropped editTitle/onTitleChange. (5) Fix: the single-style preview branch didn't
  pass `move` — edit adjustments were invisible for single-style categories; now preview == export.
- **v227 — Android 16 Live Update + liquid-glass pills experiment + cabinet full-bleed grid**
  - `ExploreSessionService.liveNotification`: RUNNING sessions on API 36+ post a genuine Live Update via
    `NotificationCompat.ProgressStyle` (one accent-colored Segment of durationMinutes defines the max,
    progress = elapsed minutes, tracker icon = ic_notification). Paused / pre-16 keep the BigTextStyle path.
  - Liquid glass (experiment, OFF by default): new `ui/components/LiquidGlassPills.kt` —
    `CurioGlassPills.backdrop` (NavHost-published LayerBackdrop via SideEffect, the CurioNavTint handoff
    pattern) + `Modifier.liquidGlassCapsule(container)` (vibrancy + blur(8) + lens(24), Highlight.Default
    rim, Shadow.Default, 40%-alpha container wash). Gate: `isLiquidGlassPillsActive()` = pref ON && API ≥ 31.
    Dependency: `io.github.kyant0:backdrop:1.0.6` (Apache-2.0). NavHost marks ONLY the content-wrapper Box
    with `.layerBackdrop(...)` so overlay pills never record themselves. Call sites: CurioFloatingNavBar,
    RevealCategoryFavoriteBar, PetDesigner studio bar — each swaps its Surface to Transparent + 0 elevation
    when active. Toggle: Settings → Experiments → "Liquid glass pills".
  - Cabinet strip root cause: the grid Column reserved navigationBars + 84dp clearance, so the
    LazyVerticalGrid CLIPPED every card at a hard horizontal line exactly at the capsule top ("the strip").
    Fix: clearance moved into the grid's contentPadding bottom (24 + 84 + navBars) — entries scroll
    full-bleed under the floating pill, only the last row lifts clear.
- **v226 — explore sessions round-trip + Sans Flex voice**
  - `MainActivity.onDestroy` auto-pauses the active explore session when the app truly closes (`!isChangingConfigurations`), re-arming the service so the shade flips to Paused. Rotation/fold skips it.
  - Cancelled sessions are stashed (`ExploreSessionStore.stashCancelledSession` / `resumeCancelledSession`) and surface as a recovery row on Home (`CancelledExploreRow`, gated on no active session). The done-dialog confirm-cancel, Home stop button and the bubble's new Cancel all stash first.
  - `ExploreBubbleContent` grew an `onCancel` param (Delete glyph in the expanded control row); only call site is the service, which mirrors the notification-Cancel teardown.
  - Notification staleness: the live notif drops its collapsed content text while running — the shade chronometer is the timer. Bubble-only quiet notif dropped its elapsed line too. Paused keeps a frozen readout.
  - Done prompt fires once per session (`dialogDismissedFor` keyed by startMillis). Home CTA renamed "Express yourself".
- **v224 — drawer curiosity map: MATERIAL ink + centering fix.**
  (1) `CurioConstellation` gained `materialInk: Boolean = false` (drawer
  passes true; the Stats deep-space page keeps the SVG palette): theme-role
  lines (`onSurfaceVariant` @ ~0.55, thicker 3.4-unit stroke), EXPLORED
  lane stars in `primary` at 1.45× size with dim (0.40-alpha) unexplored
  dots — the old near-white `0xFFeef5fa` stars were INVISIBLE on the cream
  drawer surface in light mode. A 7s sine `twinklePhase` animates every
  star's radius (±10%, phase-staggered) and the selected star wears an
  expanding `selPulse` ring in primary. Drawer map height 280 → 320dp.
  (2) TAP-CENTER FIX: the auto-zoom `LaunchedEffect` moved INSIDE
  `BoxWithConstraints` (it now reads the real `wPx`/`hPx` instead of a
  hardcoded ±80px guess) and cancels the 2× scale about the layer's center
  pivot exactly: `offset = -2 * (star - center)`. (3) The selection popover
  now applies the same layer transform (scale about center + translation)
  to the star's position, so the card tracks the VISUAL star while the map
  zooms instead of sitting at the un-zoomed spot.
- All UI is 100% Jetpack Compose. No XML layouts for screens, ever.
- `MainActivity` is the only entry point. It hosts `CurioNavHost` inside `CurioTheme`.
- Edge-to-edge is enabled at the Activity level; the system bars are themed by `CurioTheme`'s `SideEffect` to match the current color scheme + light/dark mode.
- Icon rendering uses `CurioIcon(name = CurioIcons.X)` with the Material Symbols ligature font. Emoji-vs-icon policy is a design decision — confirm with the user (see the Purpose note above).
- All glyph names used by `CurioIcon` are declared in `CurioIcons.kt` (single source of truth for icon names). Adding a glyph = adding a `const val` there first.

### Glyph watermarks — very subtle by default (v407)
- **One depth knob for every decorative glyph watermark.** `AppPreferences.glyphBackdropDeepState` (Appearance → "Glyph backdrop" → Subtle / Deep; `KEY_GLYPH_BACKDROP_DEEP`, default OFF) is read through `glyphWatermarkDepthScale()` in `ui/components/CurioWatermarkBackdrop.kt` (Subtle = `GlyphBackdropSubtleScale = 0.32f`, Deep = 1f). Every surface multiplies its OWN soft alphas by it; none of them owns a second switch.
- **What it covers:** `CurioWatermarkBackdrop` (the page-wide collage — the 11 category glyphs tinted with their own accents, the lower-band mode for pages with a hero, and a per-screen `alphaScale` that rides ON TOP of the depth), `CurioMoodBoardBackdrop` (base alphas), the SEVEN torn heroes' mirrored symbol pairs (Home quest hero, Profile, Cabinet, Settings hub, Topic History, Onboarding, the saved-entry hero — each scales inside its private `*HeroSymbol` composable), the Spin filter hero's two banner glyphs, and `CurioTopicCard`'s `MiniHeroWatermark` (its own doc: the torn-hero watermark scaled down).
- **Never raise the base alphas to compensate** — e.g. `watermarkAlpha`'s values, the `*HeroPair` alpha lists, the mood board's `baseAlpha`, `MiniHeroGlyph`'s 0.14–0.20, Spin's 0.10/0.07: those ARE the deep look that read as a distracting scatter behind flat content. Quiet a surface by scaling it with `glyphWatermarkDepthScale()`, never by editing its table.
- **Not covered (deliberate):** the share-card watermarks (`TopicShareCard`) and the deck ticket's single large category symbol — card ART and an export design, not page/hero decoration.
- Adding a new hero or backdrop: one shared component per job. Never add a second scatter component.

### Hold-to-act gestures — the patient hold (v407)
- **`ui/components/CurioPatientHold.kt` owns the app's hold timing:** `CurioHoldMillis = 2_000L` and `CurioPatientHold { }`, which provides a `ViewConfiguration` whose `longPressTimeoutMillis` is `max(platform, 2s)` — a device with a longer native hold never gets a shorter one.
- **Why it exists:** at the platform's ~500ms, a scroll that starts ON a row armed that row's option pill on the way past (the member's report on Recents). The framework's own cancellation is half the fix and stays in place — a scroll consumes the gesture and cancels the pending press — so with a 2s window a swipe can never reach it.
- **Used by** the Recents feed rows (`RecentScreen`), the pet's home (`CurioPetHome`) and **Home's own recents preview** (`HomeScreen`'s `recentPreview` rows — v407 follow-up: they open their options through the picker's anchored radial gesture, which reads `LocalViewConfiguration.longPressTimeoutMillis` from ITS OWN subtree, so those rows were still on the platform's ~500ms and armed the menu on a resting finger mid-scroll).
- **Two hold mechanisms, one timeout.** A hold that opens something must play `HapticFeedbackType.LongPress` itself: Compose plays none for `combinedClickable`, and a hand-rolled timer gets nothing from the platform. `combinedClickable` rows fire it in their handler (hoisted as `val haptics = LocalHapticFeedback.current`); **`Modifier.radialHoldMenu` fires it inside the gesture** (it owns the timer), so its call sites — Home's recents rows, the Spin picker's tiles and browse rows — must NOT add a second tick on open.
- Adding a new hold-to-act row: wrap its list or page in `CurioPatientHold` (the radial gesture inherits it with no code change) and fire the LongPress haptic in the handler for `combinedClickable` rows only. Do not write a second timeout mechanism, and do not move `RecentScreen`'s provider down to the rows.
- **Known gap:** the Spin picker's tiles and browse rows (`NewCategoryPicker*`) wear this hold for their haptic but still use the platform's timeout — wrapping those sheets in `CurioPatientHold` is the next step if their hold ever reads as jumpy.

### The card ladder — a WHITE page with CREAM cards (v409; the reverse is one switch away)
- **v421 — three light ladders, and which theme wears which.** `CurioSilverLightScheme` (a white page, a SOFT SILVER-GRAY card ladder, `#F3F4F6 → #DBDEE3`) is what **Curio rose and Azure** paint; `CurioWhitePageLightScheme` (white page, cream cards) and `CurioCreamPageLightScheme` (cream page, white cards) are the pair **Adaptive Hero** switches between; the **Material** theme paints its own. A named theme paints its own page pair entirely and never reaches any of these.
- **LIGHT MODE'S PAGE IS WHITE (`CurioWhitePageLightScheme`, the default for Adaptive Hero) and every CARD, TILE, SHEET, TAB, FIELD and DIALOG is CREAM**, stepped deeper as it nests: `surfaceContainerLowest` = white (a dialog — the scrim separates it), `surfaceContainerLow` = **A CARD**, `surfaceContainer` = a block inside a card, `surfaceContainerHigh` = a pill/chip inside one, `surfaceContainerHighest` = the anchor step. `background` = white, `surfaceVariant` = cream.
- **`AppPreferences.paperCreamCardsState` (`Appearance ▸ Paper`: "White page" / "Cream page") chooses which surface leads.** OFF selects `CurioCreamPageLightScheme` — the pre-v409 pair the member had first asked for: the cream page (`CurioColors.SoftCream`) with WHITE cards, the ladder climbing above the page. Both are the same rule (a card separates from its page by LIGHTNESS, never by a tint of it) read in two directions; nothing outside `CurioTheme.kt` has to know which one is on. It is a visible Appearance option on purpose — do not bury it behind a hidden flag, and do not delete either scheme while the switch exists.
- **Which themes have the Paper choice (v421 — ADAPTIVE HERO ALONE).** The flip applies whenever `curioColorScheme()` reaches its last line. **Curio rose and Azure return early now: they wear `CurioSilverLightScheme`** — a white page with a SOFT SILVER-GRAY card ladder — and the member's ruling is that cream is what made the two default themes read heavy and that the paper flip was one decision too many on the themes a member lands on ("remove that paper cream color and its option of paper from the default rose and azure and introduce, soft silverish gray"). So the Appearance row is shown ONLY when it can act: light mode AND `colorThemeState == COLOR_THEME_LANE`; every other theme does not draw the row at all (a control that can never do anything is furniture, and the old greyed-out-with-a-hint row was exactly that on the defaults). Adaptive Hero keeps it for the v413 reason below, and the stored `paperCreamCardsState` is always kept, so returning to Adaptive Hero restores exactly what was picked.
- **Why Adaptive Hero keeps its Paper row (v413).** A lane only tints the PAGE WASH (`heroPageBackground()` → `categoryBackgroundWash()`) and the torn hero — the card ladder underneath is still the scheme's own cream/white pair, so the flip has a real, visible effect there (member: "for color theme of adaptive hero dont gray out the paper option"). It was in the greyed-out set until v413 and that was wrong.
- **v414 — the light ladder is pitched deeper and the shadow step went to 5dp.** v411's cream was a shade too subtle to read as a card at all (member: "do somethng bout the background and the card color issues, and use elevation"); the White-page scheme's container steps are re-pitched (`surfaceContainerLow #FAF2E0` → `Highest #E8D8B3`) and `Modifier.curioCardShadow`'s default elevation is 5dp. The FILL gives the card its plane, the SHADOW gives it its lift. Keep both in step when tuning.
- **Dark mode is not flipped.** Its page is pitch black and its plates step UP through `#121212 → #161616 → #1C1C1C → #242424 → #2C2C2C`; that is the same rule in the dark's own language.
- **Why the flip:** v408 put white cards on the member's cream page; the member's direction is "home about a white app background as the main, but cream cards reverse of what we did". The complaint that started all of this was cards dissolving into the page ("the background and the cards and tabs some were blending too much … the journals in the home screen the preview of them get the background color and blends") — the fix is the SEPARATION, not which colour wins.
- **Cards resolve their fill through the SCHEME, never through a literal.** The v408 sweep turned ~25 light-branch card fills into `Color.White` literals; v409 made them `MaterialTheme.colorScheme.surfaceContainerLow` (a card) or `surfaceContainer` (a block INSIDE a card), because a literal cannot follow the Paper switch. If you add a card, take a ladder token on both branches — `if (dark) surfaceContainerHigh else Color.White` is the shape to avoid.
- **Never tint a card back toward the page to "soften" it.** `settingsCardTintLift()` (light branch) is WHITE with 8% of the hero ink, not the page background — it was the page itself, which is what made `CurioSettingsCard`'s 30% lerp resolve to paler cream on cream.
- **Translucent white is not a card fill.** `Color.White.copy(alpha = 0.68f)` over the page resolved to very nearly the page on a wash; every one of them is an OPAQUE scheme step. If you need a card, take a ladder token; if you need glass, use the liquid-glass components.
- **Separations are formulaic and SOFT (v409):** `outline` / `outlineVariant` are a whisper of plum (light 0.20 / 0.10, dark 0.26 / 0.14 — the v408 0.22/0.11 pre-flip pair was the member's "soft borders" ask, answered by pitching the light pair one step quieter), dividers halve them, and an opaque card that needs an edge takes `border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)` on its `Surface` — or, for a `Modifier` chain, the `border` goes AFTER the `background` (a border earlier in the chain is painted over by the fill).
- **Modifier order still rules (see rule 11):** shadow → clip → background → border → padding.

### Settings hub — a PLAIN LIST, rail only inside sections (v408)
- `SettingsHubScreen`'s phone branch renders `SettingsSections` as: `SettingsSectionHeading` per section, then ONE `SettingsOptionCard` holding that section's rows (an `item` per section, `GridCells.Fixed(1)`, everything full-span). The designed 2-up cards and their whole machinery (`SettingsDesignTone/Visual/Card/Group`, `settingsDesignGroups`, `settingsSecondaryCards`, `settingsToneGradient`, `settingsCardInk`, `SettingsCardTexture`, `SettingsDesignCardView`, `SettingsSecondaryCardView`, `SettingsCardVisual`) are DELETED — do not reintroduce them. `settingsCardChipTint()` / `settingsCardTintLift()` survive and are shared with the option cards, pills and dialogs.
- **The rail (`settingsNavRail`, `SettingsNavRail`) lives on the SECTION pages only.** There is no `"all"` entry any more — the hub has no rail of its own, and its back pill is the way out. `navigateToSettingsSection` still tolerates a null route for any future hub-targeting entry.
- **`SettingsRowEntry.plain = true`** (Online mode, Recycle bin, Updates, Support & diagnostics) renders a roomier row — taller padding and copy free to wrap to three lines (`SettingsOptionRow(plain = …)` → `SettingsOptionCopy(subtitleMaxLines = 3)`). Its divider goes flush (`SettingsOptionDivider(startInset = 0.dp)`) instead of the 53dp icon inset. Those four are exactly the rows whose one-line subtitles the old designed cards truncated. **v409 — `plain` no longer drops the icon tile:** the member asked for the icons back, so `SettingsOptionRow` always renders `SettingsOptionIconTile` and `plain` means "roomy copy" and nothing else.
- **`PetLandmark(id = "appearance", …)` wraps the Appearance ROW** — it used to sit on the designed Appearance card, and the pet's Settings poke plus the tour's stop point at that id. Moving or renaming the row means moving the landmark with it.
- The wide two-pane hub (`SettingsTwoPaneHub`) is untouched by all of this; the search results and the deep row index still render through `SettingsOptionRow`.

### Books — progress from the FILE, and the file's name (v408)
- **The reveal's book sheet is gone.** Tapping a book's synopsis card or a chapter chip on a topic navigates to `BookDetailScreen`; the topic's shelf book is resolved (or shelved, additively) by catalog id first, off the main thread, and the sheet flags are consumed by that effect. `BookNotesSheet`, `BookNotesMode`, `ChapterNoteField` and the `pendingChapterShare` seed are deleted — do not bring back a second, smaller copy of the book page.
- **The reader's position is the truth about "where I am".** One `ReaderMarkKind.POSITION` row per book + file, `sourceKey` = the document PATH, `positionIndex` = a PDF page (0-based) or an EPUB block index, plus `positionFraction`. `BookDetailScreen` observes it through `PersonalDao.observeReaderPosition(bookId, sourceKey)` (v408 — the mark queries all exclude `kind = 'position'`, so it needed its own flow) and computes `lastPageOf(mark, isPdf)` (page + 1 for a PDF, 0 otherwise — never invent pages for a reflowable book).
- **`ProgressCard` shows two clocks and never runs backwards:** the page bar + "page N of M" (the file's `pdfPageCount`) and the chapter (the file's own `documentChapters` names, with `chapterForPage(page, chapters)` mapping a page into its chapter range). A HAND edit writes the book ROW only (`setProgress` for the chapter, `setPage` for a hand-set page — v409, see the bullet below) and **never touches the position row**, so the member's real reading place cannot be yanked by editing the card. Keep it that way.
- **v409 — THE PROGRESS CARD IS ONE GAUGE OVER THREE LABELLED CONTROLS, and the finish state is a real state.** `PersonalBookEntity` grew `currentPage`, `chapterBeforeFinish` and `pageBeforeFinish` (migration 19→20, `MIGRATION_19_20`). `ProgressCard` draws one bar — the file's own PAGES when the reader has pages (a PDF), else the chapter run — with the same fraction said once as a percent, the chapter ticks, and three `ProgressStepperRow`s (chapter / page / length) that each write what they say; they are HIDDEN while the book is finished, because a finished book has read all of it and "Reading again" is the control that opens the chapters back up.
- **A hand move of the PAGE lands on the book row, and the most recent answer wins.** `setPage(bookId, page)` writes `currentPage`; `BookDetailScreen` shows `handPage` when the book row was written after the reader's position row, else the reader's page — so a hand move sticks in EITHER direction while reading on takes the card back over. The old code saved a whole row (`pageCount` + chapter) and never the page itself, which is why the stepper looked broken (member: "i am not able to change the pages update from there"). **The position row is still the reader's alone — a hand move must never write it**, and `chapterForPage` moves the chapter with a page that lands in a later chapter's range.
- **`setFinished` is read-modify-write, not a column-scoped UPDATE:** finishing stashes `currentChapter`/`currentPage` into `chapterBeforeFinish`/`pageBeforeFinish` and closes the list (`currentChapter = totalChapters`, `currentPage = pageCount`); "Reading again" restores the stashed pair verbatim (-1 = nothing kept, so an old finished book falls back to the progress it already had). `saveChapterNote`'s progress nudge is guarded on `!book.isFinished` so writing about a chapter cannot re-open a closed book.
- **The chapter list wears the run** (`ChapterReadState` → read / reading now / to read): the number's fill, the side rule and the subtitle's own word. It is what makes "Mark finished" visibly close every chapter at once.
- **File imports are named from the file, then confirmed.** `BookShelfScreen`'s `importLauncher` reads `BookFiles.displayName(context, uri)` (the provider's `OpenableColumns.DISPLAY_NAME` — `lastPathSegment` is an opaque id like `msf:1000000042`) through `detectBookFromFileName`, then shows `BookImportConfirm` with editable title/author before anything is written. `addBook` adopts the file's own facts (`documentChapters` + `pdfPageCount` → `adoptDocumentFacts`) on every import, not just on a later attach.

### Redundancy — one door per destination, one statement per idea (v408 audit)
- **A destination gets ONE door per screen.** The audit found and removed the duplicates: Home's
drawer offered `STATS` from the curiosity-map card AND from a "Stats & insights" row inside the
fold-out group (the row is gone — the card keeps it, because it is the thing you look at before
you tap it; v409 flattened the rest of that menu away, so the "YOUR BRAIN" row is now the panel's
only door — see the lane-grid section below); Profile offered `SETTINGS` from a header pill AND from its `SettingsNavCard` (the
member kept the card and the BOTH header pills were deleted — `glassSettingsPill` in the glass
branch and the whole `ProfileSearchPill` composable in the classic one); the Cabinet's
Curiying-now shelf opened `BOOKS` from its heading as well as the personal shelf's (its heading
is a LABEL now: `PersonalShelfHeading(onClick = null)` draws no click and no chevron, so a
heading never promises a door it does not have).
- **Do not remove a HEADING's door by passing an empty lambda** — that leaves a chevron and a
click that does nothing. Make the click nullable (the `PersonalShelfHeading` pattern).
- **Copy states one idea once.** Profile's "Your lanes" card was deleted (the hero's stat strip
already counts the lanes; arranging them is Settings ▸ Manage categories) — and v409 deleted the
Stats page's own "Your lanes" list for the same reason, since the lane map above it already showed
that lane and its knowledge (the map's tiles ARE the breakdown now). Home's drawer subtitles were
re-cut so "Quests & Levels" and "Your Curiosity" no longer both promised stats — and v409 removed
that whole row menu, subtitles and all. The Online mode page's sync switch is no longer titled
with the page's own name.
- **Checked and NOT redundant (do not "fix" these):** the settings rail on its 19 settings-family
screens (that IS the family's nav); the wide-window rail and the bottom pill bar never co-render
(`CurioNavHost` picks one); `LiquidGlassPageNav` is an in-page page turner, not a tab strip; no tab
screen prints its own name. Repeated title/subtitle literals across a screen's phone and
two-pane branches are one copy per branch, not a duplicate on screen.

### The drawer and the lane grid — no more painted constellation (v409)
- **The navigation drawer is the BRAIN PANEL and nothing else.** `HomeDrawerContent`'s LazyColumn holds exactly two items: `DrawerBrainPanel` (the "YOUR BRAIN" card + the lane grid) and `DrawerFooter`. The row menu is gone for good: `DrawerNavRow`, `DrawerNavItem`, the collapsible "Your Curiosity" group (Topic History / Manage categories / Browse topics), the "Quests & Levels" row, the whole "About" group (Support & diagnostics / Replay intro) and both `rememberSaveable` expansion flags are DELETED. Nothing lost a door — Topic history and Manage categories are Settings rows, Browse topics is Home's own browse pill, Quests is Profile's progress card AND the Stats progress card, Support & Replay intro are Settings rows under Safety & support. **Do not re-add a navigation menu to the drawer**; a door belongs on the surface that owns it.
- **The "YOUR BRAIN" row is the drawer panel's ONE door** (`onOpenStats` → `CurioRoutes.STATS`). The lane tiles only SELECT, so the drawer can never offer two doors to one destination again.
- **`ui/components/CurioLaneGrid.kt` is the shared LANE DATA, not the drawing.** `LaneGridItem` + `laneGridItems(knowledge)` build the items (explored lanes first by knowledge, then the rest in the member's own lane order, hidden lanes excluded) and BOTH lane surfaces read them, so the drawer and the Stats page can never disagree about which lanes exist or what they hold. `CurioLaneDetailStrip` is the selected lane's own line (`name · saved · knowledge` + an action slot) and is the one piece both surfaces still draw. `CurioConstellation.kt` (the Canvas star map, its star tables, the nebula/starfield painters and the 3D zoom) is DELETED. **v413: each surface draws its own shape from that data** — the drawer is a PAINTED chart (`DrawerLaneStarMap` — a golden-angle star scatter drawn on the drawer's own page, no plate, in `HomeScreen.kt`) because a map is what a drawer wants, and the Stats page is a bar list (`LaneStatsGraph`) because a chart is what a statistics page wants. The `CurioLaneGrid` composable itself has no caller left (nothing to remove without asking — left in place, unreferenced). Do not reintroduce a second item model or a second `laneGridItems`: the DATA is what must stay single, not the shape.
- **The constellation's two Experiments switches are gone** (`starZoom3dState` / `KEY_STAR_ZOOM_3D`, `drawerConstellationState` / `KEY_DRAWER_CONSTELLATION`, and `AppPreferences.isStarZoom3dEnabled` / `setStarZoom3dEnabled` / `isDrawerConstellationEnabled` / `setDrawerConstellationEnabled`). The Experiments screen's old "Constellation" section is now **Navigation** and holds only the nav-bar "Classic active indicator" row. Those experiments concluded, so the winning path is hardcoded — do not re-gate them.
- **The Stats page ("Your Curiosity") is four instruments in `features/stats/StatsScreen.kt`:** `ProgressCard` (streak + level/XP + journey stages + medals + the Quests door — it REPLACES the old separate `StreakLevelCard` and `JourneyCard`, which were two cards about the same number), `BrainCard` (the six `brainProfile` dimensions as meters, with the tip printed for the WEAKEST dimension only — it used to print six paragraphs), `LaneMapCard` (v413: `LaneStatsGraph` — the ranked horizontal BAR LIST, knowledge per lane, strongest first, cut to `LANE_BARS_SHOWN` = 7 so the card's height never depends on how many lanes were met, bars scaled against the strongest lane and grown in with one `animateFloatAsState`; a row is a tap that selects, and the readout strip + single `Cabinet` door render only for the SELECTED lane, through `PendingCabinetFilter.request` + `navigateToTab` — it used to be the interactive 24-tile grid, which was a navigation surface wearing a statistics label), and `LifetimeTotalsCard` (compact counter panes). `StatsConstellationCard`, `LanesBreakdownCard` ("Your lanes" — the list that repeated the map) and `StatsSummaryChip` are DELETED, and `StatsCard`'s shell is the app-wide WHITE card (`surfaceContainerLowest` + an `outlineVariant` hairline), not the old seafoam lerp.
- **Audit follow-up (v409):** a fresh sweep for duplicate copy and duplicate doors found only the legitimate patterns — per-row navigations (`revealFor` / `socialProfile` / `directMessage` once per list item) and the phone/two-pane or empty/list BINARY branches, which are one copy per branch rather than two on screen. Nothing else was co-visible duplication.

### Complexity audit — the heavy screens and the unreachable code (v409)
Measured across every `.kt` in the module: LOC · `@Composable` count · state slots · `LaunchedEffect` count · the longest single composable body. These are RECOMMENDATIONS, recorded so the next session starts from the numbers instead of an impression.

| file | LOC | heaviest composable |
|---|---|---|
| `ui/components/TopicShareCard.kt` | 12,722 | `TopicShareSheet` — 3,445 lines in ONE body |
| `features/reveal/TopicRevealScreen.kt` | 7,092 | `TopicRevealScreen` — 1,716 |
| `features/petdesigner/PetDesignerScreen.kt` | 5,937 | `PetDesignerScreen` — 1,195 |
| `features/personal/BookReaderScreen.kt` | 5,744 | `BookReaderScreen` — 912, with 55 `LaunchedEffect`s |
| `features/personal/PersonalCanvas.kt` | 5,524 | `PersonalToolDock` — 399 |
| `features/spin/SpinScreen.kt` | 4,565 | `SpinScreen` — 1,122 |
| `features/detail/EntryDetailScreen.kt` | 4,512 | — |
| `features/cabinet/CabinetV2Content.kt` | 4,324 | `CabinetV2Content` — 1,093 |
| `features/home/HomeScreen.kt` | 3,484 | `HomeScreen` — 1,458 |
| `ui/pet/CurioFloatingPet.kt` | 2,914 | 71 state slots · 44 effects |

**1. The share sheet repeats ONE contract six times.** Six sibling composables in `TopicShareCard.kt` carry the same parameter block (`display`, `aspect`, `palette`, `factText`, `sharerName`, `categoryName`, `categoryGlyph`, `modifier`, `ratingStars`) — the design catalog is six copies of one signature. A spec type (or one design interface) would make it one contract with six implementations, and is the single biggest lever on the worst file in the app.

**2. The settings drill-in scaffold is copy-pasted into 22 files.** Every settings-family page writes out `PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = if (wide) 0.dp else SettingsHeroTotalHeight, bottom = 24.dp)` by hand — plus its own header/toolbar branch and `SettingsNavRail`. Only SEVEN of those blocks are still byte-identical; the rest have drifted (added comments, an extra offset). That drift is exactly how a tablet layout goes inconsistent screen by screen. One shared page scaffold (or, at minimum, one `settingsDrillContentPadding(wide, …)` helper) is the fix; migrate every caller, do not add a 23rd copy.

**3. Four sibling art/poster fetchers share a body.** `features/reveal/{SongArt,FilmPoster,SeriesPoster,AnimePoster}Fetch.kt` are four near-identical provider-chain lookups (~100–160 lines each). One generic fetcher over a provider list would collapse them.

**4. ~1,300 lines of UNREACHABLE private code (17 declarations).** Each name appears exactly once in the module — its own declaration — verified by name-count, so none of it is called. Two kinds, and the difference matters before anyone deletes anything:
- **PARKED UI (648 lines)** — deliberately hidden, by its own comments (`"Animation selection and preview remain implemented in the data/runtime layer but are hidden here while the animation UI is being refined"`): `AnimationTimelineEditor` (366), `AnimationPlayerDialog` (141), `ActionPreview` (64), `AnimationGalleryCard` (34), `LabeledChips` (29), `nudgeDetailRows` (14). Removing these throws away work the author may intend to re-enable.
- **VESTIGIAL (648 lines)** — superseded or left behind: `CurioShareCard` (219, the share hub owns this now), `V2MediaTileCard` (133) + `V2ReviewTileCard` (103) (older Cabinet V2 tiles), `SavedQuoteRow` (54) + `PinnedTopicRow` (52) (older Home rows), `NewChip` (35), `StudioRailPulse` (23), `chapterRangeLabel` (21, superseded with the v408 book progress), `CustomiseLabel` (4), `moodFromName` (2), `contrastingInk` (2).
- **Do not delete any of it without the member's word** — the root rule puts a code path on the ask-first list. The list is the audit's output; the deletion is a separate, approved change.

**5. Checked and BENIGN — do not "fix":** `PaperCard.kt`'s five `private var cachedSize: Size? = null` + `cachedOutline` pairs are memoization INSIDE each `Shape` class (per-instance, not file-level globals — the path is rebuilt only when the size changes); the per-row navigations (`revealFor`/`socialProfile`/`directMessage` once per item); the phone-vs-wide and empty-vs-list branches; the per-branch title/subtitle literals. Also benign: the five two-header pairs (`CabinetScreen`, `SettingsHubScreen`, `StatsScreen`, `TopicHistoryScreen` carry a glass toolbar AND a classic hero) — that is the dual header STYLE, not a duplicate surface.

### Adaptive layout (tablet & landscape) — ALWAYS-ON
- **`ui/adaptive/CurioAdaptiveLayout.kt`** owns the window adaptation contract: `windowWidthSizeClass()` (material3-window-size-class, `calculateWindowSizeClass(activity)`) and `CurioContentMaxWidth = 720.dp`. No Settings toggle — the wide layout engages automatically on medium/expanded windows (>= 600dp wide; tablets, landscape, split-screen) and phones are untouched.
- **Wide windows:** `CurioNavHost` renders `CurioNavigationRail` (left edge, full height) instead of the bottom bar and centers every route's content in the 720dp max-width column (`fillMaxHeight().widthIn(max = CurioContentMaxWidth)` inside a centered Box); the theme background fills the gutters. Screens keep drawing their own status-bar padding and full-bleed washes inside the NavHost.
- **Adaptive grids:** use `windowWidthSizeClass().isWide` to switch `GridCells.Fixed(2)` → `GridCells.Adaptive(minSize = …)` (Cabinet 176dp, category pickers 160dp). Picker bottom sheets center their content at `CurioContentMaxWidth` on wide windows (the sheet spans the whole window, so it needs its own cap).
- Don't add per-screen responsive hacks; read the size class from the shared helper so every screen follows one breakpoint story.

### Navigation
- Single NavHost with flat routes (see `CurioRoutes.kt`). Bottom nav tab metadata includes `HOME`, `SPIN`, `CABINET`, and `REVEAL`; `CurioNavHost` hides the actual bottom bar on Reveal and reserves an equal-height torn placeholder so the watermark and shared hero morph stay level with the Spin tab.
- **Tab switching MUST use `NavController.navigateToTab(route)`** (defined in `CurioRoutes.kt`), which anchors `popUpTo(HOME) { saveState = true }` + `launchSingleTop = true` + `restoreState = true`. Do NOT anchor to `graph.findStartDestination()`: the NavHost's declared start destination is `SPLASH`, which SplashScreen pops inclusively on launch — so the anchor is gone from the stack and `popUpTo` silently no-ops, piling up duplicate back-stack entries (back walks through the same screens repeatedly). HOME is the persistent root that always remains after Splash/Onboarding/Crash land.
- Every plain `navigate()` to a push destination (Profile, Settings, Picker, Entry Detail, Lightbox, Manage Categories, Onboarding replay, etc.) MUST set `launchSingleTop = true` so re-opening a previously-opened screen never stacks a copy.
- Tab routes also accept a `categorySlug` argument so the same `Spin` screen renders both as a tab target (`categorySlug = null`) and as a pushed destination (`categorySlug = "music"` etc.).

## Work Guidance

### Adding a new screen
1. Ask the user for design direction for the screen — there is no in-repo design spec to follow.
2. Create the file at `app/src/main/java/com/curio/app/features/{feature}/{Feature}Screen.kt`.
3. If it's a stack of related sub-screens, group them in one file like `PlaceholderScreens.kt` does today, with a shared `*Scaffold` private helper at the top.
4. Add a route constant + (if needed) a route builder to `CurioRoutes.kt`.
5. Register the `composable(route) { ... }` block in `CurioNavHost.kt`.
6. If the screen should hide the bottom nav, make sure its route is NOT in `CurioRoutes.bottomNavRoutes`. Add a per-feature AGENTS.md if the screen has non-obvious contracts.

### Adding a new design system primitive
- Add to `ui/theme/` (colors → `CurioColors.kt`, glyphs → `CurioIcons.kt`, etc.).
- New colors, type styles, and shape tokens are design decisions — confirm them with the user before adding.
- New icons must be declared in the `CurioIcons` object (snake_case ligature names) — do NOT inline glyph names in screens.
- **All design-system primitives (the `CurioIcon` composable + `CurioIcons` glyph constants object) live under `ui/theme/`.** Components in `ui/components/` consume them via import — they do not re-export them. Wrong-package imports (e.g. `import com.curio.app.ui.components.CurioIcon`) compile silently against an empty package and only fail in CI's `compileDebugKotlin`. Always import from `ui.theme.*`.

### Curie pet layer (v8.43)
- `data/PetDesign.kt` owns the pet look contract: backward-compatible 16/24/32
  canvases plus the 64×64 evolved default (all convertible via dominant-key
  resample), a 13-key palette (incl. `r` blush + `y` eye colors), per-mood
  faces (`PetFace`), per-event reaction rules (`PetReaction`), optional
  authored reaction lines, four transparent detail layers (`tail`,
  `accessories`, `effects`, `antenna`), and per-element procedural visibility
  overrides. Evolved path ornaments live in the toggleable `accessories` layer.
  Animations (`PetAnimation`) are transform
  keyframes plus v8.52 per-frame pixel layers (`PetAnimationFrame.bodyRows` /
  `curledRows`) and backward-compatible `PetViewAngle` metadata, so each
  frame can be a fully different pose/view; `CurioPetSprite` accepts
  `bodyOverride`/`curledOverride`/`viewAngle` to render them. The always-on
  Pet Life director (`data/PetLife.kt`) chooses screen-aware, personality-
  weighted routines and keeps a recent-id cooldown so autonomous behavior
  does not immediately repeat. Missing
  detail/toggle fields preserve the prior procedural behavior for older saved
  designs. The text format (palette lines + grids + `detail=` / `procedural=`
  / `face=` / `react=` / `size=` / `anim=` / `frame=` / `customAction=`
  lines) is documented in
  that file's KDoc/source implementation.
- `CurioPetSprite` renders any grid size, preserves existing motion, and draws
  authored detail layers last so the user can replace generated art without
  changing animation. The procedural antenna extras remain independently
  toggleable; the base antenna pixels are edited in the Body canvas.
- `PetDesignerScreen` (Settings → Pet designer) is a three-page studio
  (v8.52): **Pets** (pick a species from `PetRegistry`), **Editor** (choose a
  target via the preview dialog, then only that editor), and **Settings**
  (Accessories dialog, disable toggles, personality presets, shapes). The
  editor is available from the start, including for baby pets. Animation
  gallery/player/timeline and animation-selection controls are currently
  hidden from the studio UI while their models, serializers, renderers, and
  runtime playback remain in place for a future re-entry. A slim sticky
  **EditorToolbar** is the ONE place for Save / Undo / Redo / Reset / Import /
  Export (the old pinned footer SaveArea is gone — no duplicate buttons).  The Faces editor and its picker option are removed from the studio UI. Face
  data, presets, serializers, and sprite/runtime rendering remain preserved
  for compatibility. Details drawing and the entire Actions editor are
  currently hidden from the studio, while detail, reaction, and custom-action
  data/runtime behavior remain preserved.


  PNG export/import shares via FileProvider `${applicationId}.fileprovider`
  (`res/xml/file_paths.xml` cache/share). The home/house scene is a fixed
  layered sprite composition; the legacy home editor is removed from the
  studio UI, while old saved bed rows remain dormant compatibility data.
- **v407 — the pet's home is the door to turning the pet OFF.** `CurioPetHome`
  (`ui/pet/CurioFlowerBed.kt`) takes a HOLD that plays the long-press haptic and
  opens a confirm dialog calling `AppPreferences.setPetEnabled(context, false)`;
  a tap keeps its old meanings (wake / come out / check in), and the hold is only
  offered while the pet is ON. Because `combinedClickable` reads
  `LocalViewConfiguration`, the 2s timeout is supplied by the CALLERS (Home's bed
  and the Quests hero) wrapping the scene in `CurioPatientHold` — a new call site
  must do the same. The Appearance "Curie" switch stays the other door.

### Experimental features (A/B testing)
- Per root `AGENTS.md`, any experimental/test behavior MUST be gated behind a **user-facing Settings toggle** so it can be A/B-compared against the current behavior and reverted without a code change — never hardcoded as the only path.
- Remove the toggle once the experiment is decided, keeping the winning behavior hardcoded.
- **New measures (root `AGENTS.md`):** when ADDING a new feature/capability, ask the user FIRST whether it should be toggleable or always-on (use the ask_user tool before implementing, and follow their answer). This ask does NOT apply to refinements or fixes of existing behavior — those ship as-is without the toggleable question. **The toggle is NOT permanent** — once the feature is decided, remove the toggle and hardcode the winning behavior (experiment-closeout rule above).

### Phase plan (current & next)
- **Phase 2 (current)**: Design-system + NavHost + Home/Splash screens + 11 placeholder stub screens. CI gate verifies compilation. No business logic, no Room, no DataStore wiring yet.
- **Phase 3 (next)**: Spin dial rendering, Onboarding flow, Reel/Marginalia/Gallery Wall/Field Notes capture format bodies, Cabinet grid rendering.
- **Phase 4**: Per-entry persistence (ViewModels + Room), state preservation across spins. Also: **first content drop** — seed Music per `CURIO_DATA_PLAN.md` §5.1 (150 topics, LLM-drafted + human-reviewed, ships as `assets/topics/music.json` + a `validatetopics` Gradle task).
- **Phase 5+**: Streak tracking, share-card generation, Emergency Recovery hooks for FieldMind data. Per-category content drops (Movies, Books, Art, Science, then the 4 new categories) continue at one-per-PR cadence per `CURIO_DATA_PLAN.md` §5.1.

### Content authoring (CURIO_DATA_PLAN.md §2 + §6)

Topic data lives in JSON files under `app/src/main/assets/topics/{category}.json`. The schema is `CurioTopic` + `ExploreAction` — see [`assets/topics/SCHEMA.md`](src/main/assets/topics/SCHEMA.md) for the in-folder quick reference and `CURIO_DATA_PLAN.md` §2 for the full source-of-truth.

- **Validation:** `./gradlew validateTopics` parses every JSON file in `assets/topics/` and asserts the §2 schema. The task is wired into `preBuild` automatically when JSON files exist, so a malformed entry fails `assembleDebug` / `assembleRelease`.
- **Adding a new topic:** see `SCHEMA.md` "Authoring a new topic (quick recipe)". For the full §6 LLM authoring prompt template, see `CURIO_DATA_PLAN.md` §6.
- **Modern batches (v7.6):** `artists.json` (354) + `albums.json` (498) include the modern content drop — 50 contemporary (2010s–2020s) artists + 50 modern albums, The 1975 explicitly included. Appended idempotently by `scripts/add_modern_batches.py` (dedupes by id + name; entries meet the stricter ≤280-char teaser/instruction bar, above the Gradle task's 450 limit).
- **Songs expansion (v1.1.0):** `songs.json` grew 60 → **1,000 real songs** (1960s → 2020s, ~54% are 2000s or newer), each with a factual teaser + a listen instruction (all tier 1). Generated by a one-off script (`/tmp/gen_songs.py` lineage; the pattern: dedupe by name+artist+year, seed ID slugs with the existing 60 song IDs so re-runs never collide, then merge + validate — unique ids across all topic files, ≤450-char teasers, ≤600-char instructions, `verb: "Listen"`, `durationMinutes: 3`). Same-name songs get `-{year}` (then `-{artist}-{year}`) suffixes.
- **Adding a new category:** see `CURIO_DATA_PLAN.md` §5.2 step 5 — toggle `isReady = true` on `CurioCategory` only when 100+ topics are authored + reviewed. Categories with `isReady = false` are filtered out of the Home chip row + Category Picker and surface as "Coming soon" empty-state slots.

## Verification

- `MainActivity` compiles and runs as `com.curio.app` on debug builds with `applicationId = "com.curio.app.debug"`.
- No background workers, no widgets, no Room/SharedPreferences persistence wiring yet — those arrive in Phase 4+.
- **CI gate**: this environment has no Android SDK, so CI on push to `revamp` is the source of truth for compilation. Local Gradle compile/build/lint/test commands are explicitly forbidden by root AGENTS.md.
- **CI expectations (flavorless)**: CI calls `./gradlew lintDebug validateTopics assembleRelease` for Android checks (release-only — no debug APK is built) and `./gradlew validateTopics assembleRelease` for tagged releases. Release outputs are ABI splits + a universal APK at `app/build/outputs/apk/release/` (see the `splits { abi { … } }` block); the release workflow renames them `Curio-{versionName}-{versionCode}-{abi}-Android8.0+.apk`. `printReleaseVersion` prints `versionName:versionCode` for that naming. Release signing uses the repository keystore secrets when configured; local builds fall back to the debug signing key.
- All placeholder screens route correctly: tapping the Home hero with no chip → `PICKER`; with a chip → `spin/{slug}`; bottom-nav switching preserves each tab's back stack; back arrow pops the current route.

## Session Lessons Learned

These patterns and anti-patterns were learned the hard way (CI compile failures, JSON escaping, double navigation). Future agents inheriting this DOX chain should internalise them.

### API verification — never trust memory
- **Download source JARs from Google Maven** before using unfamiliar Compose/M3/navigation APIs. The version in `gradle/libs.versions.toml` is authoritative — signature changes across versions are real.
  ```bash
  curl -sL -o jar 'https://dl.google.com/dl/android/maven2/{path}-{version}-sources.jar'
  unzip -o -q jar -d src-dir
  grep -rn 'fun theFunction\|interface TheInterface' src-dir/
  ```
- Examples from this session: `calculateWindowSizeClass(activity)` in material3-window-size-class 1.5.0-alpha20 (not the zero-arg version); `SharedTransitionScope.rememberSharedContentState` is a `@Composable` member function (NOT a top-level function); `Modifier.sharedElement` is a member extension of `SharedTransitionScope` (needs receiver); `BoundsTransform` is a top-level `fun interface` in `androidx.compose.animation` (not nested); `contentAlignment` on `Box` takes `Alignment` (NOT `Alignment.Horizontal`). ALL of these would have been compile errors without the source-verified signatures.
- **v116 — `ImageDecoder.setIsExifOrientationRequired` does NOT exist** (verified against AOSP `ImageDecoder.java` on master + android-31/33/34 branches: no EXIF method exists in the Java layer at all). It was invented from memory and broke the build. The EXIF orientation behavior lives in the native Skia codec and differs across Android versions with no public toggle, so the avatar decoder now uses BitmapFactory for EVERY API level (it never applies EXIF — documented) plus explicit framework `ExifInterface` rotation: one deterministic path, no double-rotation risk.
- **v116 — Kotlin local functions can't be forward-referenced.** `avatarPicker`'s lambda called `saveAvatar` which was declared later in the same composable → "Unresolved reference 'saveAvatar'". Local (nested) functions must be declared BEFORE use; only top-level/member declarations are order-independent. Declare helper locals above the callbacks that use them.
- **v116 — material3 `AlertDialog` has TWO overloads**: the classic `(onDismissRequest, confirmButton, …, text, containerColor, …)` and the newer `(onDismissRequest, modifier, properties, content)` basic-dialog overload. Calling with `containerColor`/`shape`/`title`/`text` but NO `confirmButton`/`dismissButton` matches NEITHER ("No parameter with name 'containerColor'… No value passed for parameter 'confirmButton'"). If you put custom action pills inside `text`, hoist the shared state out of the `text` lambda first (the confirm button can't see locals declared inside it), then pass them as `confirmButton`/`dismissButton`.

### contentAlignment: Alignment vs Alignment.Horizontal
- **A CI compile failure in this session** — `contentAlignment = Alignment.CenterHorizontally` was passed where `Alignment` was expected. `Alignment.CenterHorizontally` is `Alignment.Horizontal` (used for `ColumnScope.align`); `Alignment.CenterVertically` is `Alignment.Vertical` (used for `RowScope.align`). For `Box(contentAlignment = …)` use `Alignment.Center`, `Alignment.TopStart`, etc. (the full `Alignment` interface).
- Same goes for `horizontalAlignment`/`verticalAlignment` — those ARE typed `Alignment.Horizontal`/`Alignment.Vertical` — don't mix them up.

### No LocalSharedTransitionScope in Compose 1.11 (animation 1.11.2)
- The shared-transition API is stable (no `@ExperimentalSharedTransitionApi` OptIn needed), but **there is no built-in CompositionLocal** for the scope. The scope from `SharedTransitionLayout { }` must be threaded manually:
  - **Custom composition locals**: define `staticCompositionLocalOf<SharedTransitionScope?>` + `staticCompositionLocalOf<AnimatedVisibilityScope?>` and provide them via `CompositionLocalProvider` in each composable destination that needs shared elements (see `ui/adaptive/RevealSharedScopes.kt`).
  - **Scope APIs**: `Modifier.sharedElement` is a member extension of `SharedTransitionScope` — call via `sharedTransitionScope.run { Modifier.sharedElement(state, avScope) }`. `rememberSharedContentState(key)` is a `@Composable` member — call via `sharedTransitionScope.rememberSharedContentState(key)` (explicit receiver on a composable member is legal).
  - **Layout**: `SharedTransitionLayout` carries the max-width cap (`widthIn(max = CurioContentMaxWidth)`); `NavHost` inside uses `fillMaxSize()`. The overlay renders within the layout's bounds — consistent coordinates across all destinations.
- `BoundsTransform` is a `fun interface` with `createAnimationSpec(initialBounds: Rect, targetBounds: Rect): FiniteAnimationSpec<Rect>`. SAM conversion: `BoundsTransform { _, _ -> tween(320, easing = FastOutSlowInEasing) }`. The default is a generic spring (`SharedTransitionDefaults.BoundsTransform`) — slow and slightly bouncy; when the user says "the morph feels delayed," replace it with a snappier ease.

### Route pattern: optional query args vs out-of-band state
- For optional flags on a Nav route, use `?flag={flag}` with `navArgument("flag") { type = NavType.StringType; defaultValue = "0" }`. This survives process death, resists stale flags, and doesn't need cleanup — unlike mutable `object`-style handoff targets (which require being reset by every other entry point).
- Example: `const val REVEAL = "reveal/{categorySlug}/{topicName}?browse={browse}"` with `navArgument("browse") { defaultValue = "0" }` — existing `revealFor()` calls (no query) match and get `browse="0"`; a new `revealForBrowse()` appends `?browse=1`. The `isRevealRoute` check comparing `destination.route` still works (destination.route is the template string).

### v389 — the reader's PDF text layer, and the writing rules
- **A PDF page is a picture.** `android.graphics.pdf.PdfRenderer` draws it and knows nothing about the words on it, so PDF text (selection, search, highlighting) comes from `com.tom-roush:pdfbox-android`, used ONLY in `features/personal/BookPdfText.kt` — lazily, one page at a time, on `Dispatchers.IO`, in a 6-page LRU. It is NEVER on the book's opening path: PdfRenderer still renders the page exactly as before, and that is a hard rule (the maintainer's own objection was "why does the pdf open slower"). Glyph positions come back in the PDF's own POINTS, which PdfRenderer also reports a page's size in, so the render scale cancels out of any glyph-to-screen ratio.
- **Zoom is two fingers, and a magnified page owns the drag.** `Modifier.pinchToZoom` in `BookReaderScreen.kt` starts a zoom on the second pointer and consumes the pinch from its first event; on the ONE-finger path it claims the drag **only while the page owns a zoom** (`zoomed()`), which is v403 — the page must answer "could I move?" from the page's own room, not from a count of events, or the scroll underneath takes the slop first (the v399 "panning a zoomed PDF page no longer turns it" report). A page at rest claims nothing, because there the single-finger drag IS the scroll or the page turn. Whatever the page does not take is left unconsumed, and that is the whole hand-back rule. A reflowed book zooms by TYPE size (`ReaderLook.textScale`) and answers `Offset.Zero` for every pan (it re-lays out; there is nothing to pan); a PDF zooms by `graphicsLayer` scale/pan with the pager's handler moved ONTO the page (`Modifier.fillMaxSize()` on `HorizontalPager`, the gesture on each page's own Box) — a deeper node handles an event first in the main pass, which is what stops the pager turning the page a pinch is magnifying.
- **A PDF zoom is ANCHORED, and only the page you pinched is magnified (v399, corrected v404).** The gesture reports its focal point (`event.calculateCentroid(useCurrent = false)`), and `readerZoomedPan(box, drawn, from, to, focus, pan, drag)` solves the page's own transform for the new pan: `next = pan * ratio + drag + (focus - centre) * (1 - ratio)`. **The drag is folded in by `readerZoomedPan` itself, never by the caller.** v403 passed `focus + drag` instead, which cancels exactly out at a constant zoom (`ratio == 1`) — so every one-finger pan answered "no room", was left unconsumed, and the column scrolled or the pager turned the page while the member was trying to move a magnified page. At `ratio == 1` the formula must reduce to `pan + drag`, and it does; the `(focus - centre) * (1 - ratio)` half is the anchoring (the point under the fingers must not slide out). `readerDrawnPage(box, aspect)` is the letterboxed page inside its frame, which is what the room for a pan is measured against; the answer is what the gesture TOOK, and `Offset.Zero` hands the drag back to the scroll or the pager. In the SCROLLING reader the zoom belongs to ONE page (`ReaderLook.pdfZoomPage`, `-1` = every page, the paged flow) and the page's BOX never changes size — the magnification is drawn inside a clipped frame, so the pages above and below cannot move, and a zoomed page is panned to its own edge before the column scrolls on. **v422 — IN THE COLUMN THE ZOOM IS THE DOCUMENT'S, AND THE BOXES DO GROW.** The member's ruling on v399's one-sheet window was that a magnified page among its neighbours "feels wrong", so a pinch in the scrolling flow now lays EVERY sheet out at `ReaderLook.pdfZoom` through `readerZoomDocument` (`pdfZoomPage = -1` is the document, which is the sentinel the paged flow leaves free because its frame is the screen). The column is `.width(pageWidth * docZoom + 28.dp)` on purpose — a lazy list needs a bounded width — and rides a `horizontalScroll` above it, and the two-finger pan is dispatched to those two scrolls (`dispatchRawDelta`); a single finger returns `Offset.Zero`, so a drag still scrolls and a swipe still turns the page. Nothing is anchored and nothing needs to be: the lazy column keeps the sheet the member was on where they left it. Where v395 went wrong was growing the boxes *about the fingers* with no scroll carrying the extra room; here the growth IS the layout. `readerDoubleTapDocument()` is the one-point twin. **Do not bring the per-page window back to the scrolling flow** — `readerZoomThisPage` is the PAGED flow's rule now (its frame is the screen and its one page is the only page there is). **And a magnified page still selects**: the text layer measures the frame it is given, so no zoom maths reaches it.
- **v422 — the reader's taps, its words and its quiet ink.** (1) `ReaderLook.tapZones` (default on) puts `readerTapStep(at, size, corner)` behind ONE `onSurfaceTap: (Offset, IntSize) -> Unit`, threaded through all four reading surfaces: a 72dp corner of either side turns the page back/forward, the head and foot bands (16% of the surface) move the reading on — a turn where the book has pages, a screenful where it scrolls (`stepPage`) — and a tap anywhere else is the chrome's, the way it always was. The switch is a `CurioIcons.Crop` button in the reader's foot that wears the accent while the zones are on. (2) A PDF page's words are read for EVERY sheet on screen: `visiblePages` is a `snapshotFlow` of `layoutInfo.visibleItemsInfo` piped through `distinctUntilChanged`, never `firstVisibleItemIndex` — the column shows a page and a half, so the sheet under the finger is frequently the second one, and a page with no text layer falls back to the old "mark this page" press. (3) An EPUB's page bar says the BOOK's own printed page (`printedPageAt(liveTextBlock)`, the last book page at or before the place) and renders no label at all when the book prints none — never the reader's own screenful count, which moved with the type size. `printedPages`/`printedPageAt` must stay ABOVE `pageBar` (a local function cannot reach a local declared later in the same body). (4) **No reader ink below 0.75 for a word or a glyph.** The chrome's quiet words sat at 0.5–0.7 of the ink and measured as low as **2.83:1** on the sepia page (the foot's label, a page's own number); 0.75 clears 4.5:1 on every skin. Anything quieter than that is a FILL (a hairline, a mark), never text. A gesture handler outlives the composition that armed it, so `zoomed` and the tap handlers must read the live state — `pinchToZoom` takes a `key` for the page's shape, because a page whose aspect arrives with its render would otherwise keep measuring against nothing.
- **A PDF jump is ASKED FOR, never performed on the pager (v399).** A PDF has two reading surfaces (a pager and a column) holding two different scroll states, so `pendingPage` (the page-`pendingBlock` already was for text) is taken by whichever surface is showing — `jumpToMark`, the contents sheet, a search find, the page bar's arrows and "Continue reading" all go through it. The scrolling column reports the page it is showing (`onPageShown` → `shownPage`) so the bar can name it and ask for the next one; the column previously had no page bar at all.
- **A mark says its own place.** `ReaderMarksSection` derives it from the CONTENT, not from the index: a PDF's mark reads "Page N" (never the reader's own `Section N`, which is a fact about how the file was split) with the outline entry it sits under, a reflowed book's reads the chapter when the file numbers one and the section otherwise. The list is sorted by position so it reads in the book's order, each kind wears its own glyph on a wash of its own colour (a highlight in the ink it was made with, `readerHighlighter(colorKey).ink`), and the date is `readerMarkWhen` (Today / Yesterday / "3 Sep").
- **A PDF page wears the reader's ink** through `readerPdfFilter(palette.inkKey)` — a `ColorMatrix` per skin (sepia tint, night inversion, a paper softening, `null` for white). `ReaderPalette` carries `inkKey` for exactly this reason: a bitmap cannot follow a colour, only a choice.
- **EPUB markup becomes markers before it is stripped** (`epubBlocks`): `<img>` and `<h1..h6>` are replaced with `\u0000`-delimited markers so the document's own ORDER survives in one pass. Pictures are copied out of the zip into `cacheDir/book-images` and dropped if they will not decode.
- **The dock is an INPUT STYLE when nothing is selected.** `PersonalEditorState.armed` is the tools switched ON and `armedOff` the tools switched OFF; `toggle(flag)` sets those instead of rewriting the line, and `activeFlags()` reports the style at the CARET. `armedOff` is the only way to write plain words inside a bold run, so do not clear it on every keystroke without re-checking `maskAfterEdit`'s `cleared` parameter. A TITLE never crosses an Enter: `splitBlock` strips `FLAG_TITLE` from the tail.
- **A newline in a field is a NEW BLOCK.** `onFieldChange` routes any `\n` to `splitOnNewlines`, which calls `splitBlock` once per newline (max 64). This is what makes a pasted paragraph editable line by line, and what makes the platform's Select all behave. Shift+Enter is still the plain in-paragraph newline — the key handler consumes it first.
- **Pinned headings** are shared: `PersonalPinnedLine` plus `LocalPersonalTitleReport` (a `staticCompositionLocalOf` in `PersonalCanvas.kt`), because a writing page owns the writing half but the READING half is the caller's `readView` lambda. "Gone by" means the heading's BOTTOM has gone past the top — its top edge is not enough. **v402 — THE REPORT IS IN WINDOW COORDINATES, AND THAT IS THE WHOLE CONTRACT.** A title line reports `boundsInWindow()` (never `boundsInParent()`, which reports a line's place inside its own little wrapper and made every heading on every page claim a top of zero — the source of "a random heading shows", "it shows before it has gone" and "it doesn't show all of them"), the boolean `writing` says which side of the eye / pen switch reported it, and a BLANK label means "this id is not a place any more" so the host clears it (a line whose title flag was taken off, or that left the page, used to stay pinned for ever — hence "something random shows even when i remove the title"). The host keeps ONE ENTRY PER SIDE (`"w:$id"` / `"r:$id"`) because both halves hold the same block ids in different boxes with different scrolls, and reads only the side it is showing; `PersonalSectionLine.liveTop/liveBottom(scrollNow)` shift the stored window numbers by how far that side has travelled since the report was taken, because a report happens on LAYOUT, not on every scroll frame. "Scrolled past" is then a single comparison against `areaTop` — the writing area's own `boundsInWindow().top` — and a tap moves the scroll that is on screen by `liveTop - areaTop`, so it always lands ON the heading ("tapping it scrolls all the way to the top" was the old offset maths, and while READING it moved the writing page's scroll, not the reader's). A read view DROPS its own `ScrollState` into `LocalPersonalPinScrollHolder` (see `JournalEditorScreen` / `TodoScreen` / `TopicNoteScreen`) — the host owns the writing side's, and nobody else can see a read view's.

### Compose inline forEach — composable calls only IN the inline body
- `forEach` is `inline`, so calling `@Composable` functions DIRECTLY inside its lambda body is legal (the body is inlined into the composable call site). BUT — **calling a composable through a `val` lambda reference inside forEach FAILS** because the lambda variable is not inlined. Bad: `val chip = { fmt -> FormatChip(...) }; forEach { chip(it) }`. Good: `forEach { fmt -> FormatChip(...) }`. If both branches need the same content, either duplicate the inline block or extract a `private @Composable fun` and call it inline in both forEach slots.

### The style mask carries MORE than flags now (v389)

`PersonalRuns.kt`'s per-character mask began as one bit per boolean tool. It now
carries two more AXES above those bits, in the same int:

- bits 9–11 — the MARKER PEN (`HIGHLIGHT_BITS`), one of `PERSONAL_HIGHLIGHT_KEYS`
- bits 12–13 — the FACE (`FONT_BITS`), one of `PERSONAL_FONT_KEYS`

This is not decoration. `maskToRuns` merges neighbouring characters by comparing
their ints, so two words written with different pens (or set in different faces)
are ALREADY two runs and two words that agree are already one — neither axis
needs merging logic of its own, and every edit that copies the mask (a split, a
merge, a paste) carries both for free.

Consequences worth knowing before touching this:

1. `ALL_FLAGS_MASK` is flags-ONLY, and so is `lineFlags`. If two names appear to
   mean the same thing, they do not — the mask is the toolbar's OR, the array is
   its ORDER (see the comment on `ALL_FLAGS_MASK`).
2. `flags != 0` no longer means "some tool is on": a marker-only or font-only
   stretch is nonzero too, which is why `personalAnnotated` paints its background
   inside that branch rather than in one of its own.
3. `PersonalRun` stores a KEY (`"rose"`, `"mono"`), never a colour or a file. An
   unknown key degrades to none. The codec writes `g`/`f` only when set, so a
   note written before either axis is byte-for-byte the note it was.
4. A pen or a face chosen with the caret alone is an INPUT STYLE, and the
   override is `Int?` — `null` is "unchanged" and `0` is "take it off". A plain
   `Int` cannot say both, which is why `maskAfterEdit` takes nullable overrides.

### The writing dock is a typing instrument, not furniture (v389)

`PersonalToolDock` (the journal's) and `RichTextDock` (the full-screen rich-text
editors') are the same dock in shape, tokens and manners — a member who has
written on a page knows where everything is on the other. `RichTextDock` is now
FOCUS-gated: it rises out of the field's foot when the field takes focus and
folds away when focus leaves, because a formatting row parked under every
completed note on "Save your take" was never a tool, it was furniture. The blur
has a grace period (`DOCK_BLUR_GRACE_MS`) since a tap on a dock button can blur
the field for an instant; only a blur that sticks hides it.

Companion rule: a tap on the page must actually END the typing. Tapping blank
space does not clear focus in Compose, so the save page puts a tap detector on
its scroller, under every card — children consume their own taps first, so it
only ever catches taps that mean "not in any of these".

### Per-frame blur is a GPU sink
- `Modifier.blur(N.dp)` over a **flat color or smooth gradient** is a visual no-op — the unblurred result looks identical — but the RenderEffect pass runs on every frame during scroll. Replace with a static gradient (`Brush.verticalGradient` with slightly different alphas or stops) for the same "frosted glow" look at zero per-frame cost. (This was the root cause of "laggy scrolling" on the detail page.)

### The reader's zoom owns its whole gesture (v406)
- `BookReaderScreen.kt` — `pinchToZoom` decides **one owner per gesture**. Two
  fingers consume from the FIRST event: the changes used to be consumed only
  once the pinch had something to report, so the column took the opening frame
  and the page moved before the pinch did. A one-finger pan on a magnified page
  claims the gesture on its first move and keeps it until the finger lifts — the
  old mid-gesture hand-back let the pager start with the finger's whole
  accumulated travel and jump a page in a flash (the "glitched preview"). A page
  ALREADY at its edge takes nothing, so the surface owns the entire swipe and
  the page turns on ANOTHER swipe.
- `readerZoomThisPage` gives a page up only at 1×. At 1× the pan is zero by
  construction (`readerZoomedPan` clamps the travel to the page's own room, which
  is nothing at 1×), so there is nothing left to jump. Resetting at 1.02 threw
  away a live pan and snapped the page back to its centre.
- Never gate the drawn translation on a zoom threshold (`if (z > 1.02f) pan
  else 0f`) — that drops the pan in a single frame while the page is still
  scaled, which is a visible shift.
- Reading progress is LIVE: every surface reports the block/page it is showing as
  it moves (`onBlockShown`, `onPageShown`, `listState.firstVisibleItemIndex`,
  `pagerState.currentPage`) and `ReaderPlacesSheet` takes a `ReaderLivePlace`. The
  stored auto-bookmark is a 700–900ms-debounced WRITE, not a read model, so it is
  only the fallback.
- A jump is always ASKED FOR (`pendingBlock` / `pendingPage`), never performed on
  the hoisted list — performing it moves an off-screen surface in the other flow.
- The chrome's auto-hide must not run while the chrome is being USED: a turn from
  the page bar sets `askedByReader`, and the countdown waits for it to settle.
- Art caches: `AppPreferences.sheetArtUrlsState` is filled in `initThemeMode`
  (called from `onCreate`, before `setContent`), but a write from ANOTHER surface
  lands after a card composes — so the cached URL must key BOTH the `remember`
  seed and the `LaunchedEffect`, or the card re-fetches art the app already has.
- **Orientation (v418).** The app declares no `screenOrientation` anywhere, so
  the reader already follows the device's rotation. `ReaderLook.orientation` is
  a real three-way `ReaderOrientation` (`AUTO` / `PORTRAIT` / `LANDSCAPE`), and
  it is applied to the ACTIVITY (via `Context.findActivity()`) for BOTH kinds of
  book — v406's `pageUpright` was a lone switch and only for a PDF, which is why
  the member found no auto-rotation in either format. Always restore
  `SCREEN_ORIENTATION_UNSPECIFIED` on dispose. The control lives in the "page"
  (ink) sheet, labelled AUTO-ROTATE, and is offered for every book.
- **Block drag has TWO axes now.** `PersonalRowDragState.dragBy(amountX,
  amountY, …)` — the vertical travel steps the list, the horizontal travel is
  INTENT only (`takeLeftCell`) and chooses which end of a print row a dropped
  print takes. `PersonalEditorState.printDropIndex(from, to, takeLeft)` and the
  landing ghost in the drawing pass must read the SAME `takeLeftCell`, or the
  cell shown in the air is not the cell that is taken.
- **Home's doors need an empty row.** A `LazyRow` draws nothing when its source
  is empty, so a first-time Home collapsed both door strips to the door alone.
  Each door emits chips at `CHIP_WIDTH` × `CHIP_HEIGHT` when its list is empty,
  so the strip keeps its shape and offers the first step.
- **v413 — AND WHAT AN EMPTY ROW OFFERS IS A SUBJECT, NOT A BUTTON.** The
  empty state used to be ONE `EmptyDoorChip` that said what was missing and
  opened a door to somewhere else ("No pages yet / Start your first one" → the
  writing sheet; "No books yet / Open the shelf" → the shelf this row already
  is). The Pages row now leads with `EmptyDoorLead` ("Nothing here yet / Write
  down something about one of these days.") and three `EmptyDoorChip`s from
  `emptyDayChips()` — **Today, Yesterday and the day before, the third named by
  its WEEKDAY** (at a chip's width "Day before yesterday" would wrap or shrink;
  the date under it says when) — each opening the journal ON ITS OWN DAY. The
  shelf row leads the same way and then offers **three real books from
  `BookCatalog.suggestions(3)`**, drawn with the ordinary `BookChip` cover and
  shelved on tap by `shelveSuggestion` (the catalog's page count, its REAL
  chapter list and its `catalogId`, so the book page can read them back
  offline). **The day rides out of band: `PendingJournalDay`** (see
  `CurioRoutes.kt` — a route argument would have to be threaded through every
  caller of `journalEditor`), consumed once in the journal's date seed, where a
  SAVED page still overrides it with the date it was written on. `suggestions`
  is **seeded by the day** (same three all day, fresh tomorrow) and is only read
  while the shelf is actually empty; a `shelving` flag guards the write, since a
  double tap would otherwise shelve the same book twice. `PersonalChipsRow`'s
  `onWrite` parameter is GONE with its only caller — the writing sheet keeps its
  real door, Home's floating "+".
- **What's New is GATED, not scheduled (v406).** It used to be a
  `LaunchedEffect(Unit)` that navigated to the `WHATS_NEW` route 700ms after the
  app settled, and the boot routes (SPLASH, ONBOARDING) counted as a quiet start
  — so a fresh install could be shown the highlights DURING the intro. The gate
  lives at the top of `CurioNavHost` and is keyed on `currentRoute`,
  `TourController.offerPending` and `TourController.active`: intro complete
  (`CurioOnboardingState.isComplete`) AND tour offer answered AND past
  SPLASH/ONBOARDING. It shows a `WhatsNewSheet` (a `ModalBottomSheet`) over
  Home and stamps `whatsNewSeenVersion` whoever way it is left. The full screen
  stays for Settings ▸ Updates and the sheet's "See all".
- **The welcome order is intro → tour offer → What's New.** `TourController`
  is offered by `finishOnboarding`; the sheet must never race it, hence the
  `offerPending` / `active` guards.
- **`ModerationBanDialog`'s reasons collapse.** `reasonsOpen` is false by
  default and set true by the "Change" button; picking a reason closes it, so
  eight wrapping chips become one chip plus "Change" the moment a decision is
  made (the note field and the Ban button are below them).

### Bottom-anchoring with weight spacers
- To anchor controls to the bottom edge regardless of screen height: replace fixed `Spacer(26.dp)` (which floats on tall screens) with `Spacer(Modifier.weight(1f))` inside a `fillMaxSize` `Column`. The weight spacer absorbs all free space above the controls.

### JSON escaping in spawn_agents
- The `spawn_agents` tool takes JSON with nested strings. **Do not use pipe characters inside `\|` grep patterns** — they break JSON parsing. Use `grep -nE 'pattern1|pattern2'` or `sed -nE`. Avoid heredocs with embedded single-quote strings. Prefer writing helper scripts to `/tmp/` with `write_file` and running them with a simple `python3 /tmp/script.py` command (no escaping).

### Static validation when Gradle is unavailable
- This environment has no Android SDK → no local `./gradlew` commands. Pre-CI validation = only static checks:
  1. **Delimiter balance**: `node scripts/check_braces.js` — local-only Kotlin/KTS checker (kept on disk, untracked — scripts are never shipped in commits) that strips comments/strings and verifies `{}[]()` balance (run it on the whole repo, or pass specific files). This replaced the ad-hoc scripts that used to be written to /tmp mid-session.
  2. **`git diff --check`** — catches whitespace errors.
  3. **Import hygiene**: after removing a usage, `grep` the file for the removed symbol to confirm no remaining references (CI catches stale imports as compile errors).
  4. **Code review**: spawn a `code-reviewer-glm` or `code-reviewer-deepseek` agent with the full file list and the key risky patterns to check.

### Commit discipline
- **Commit + push after every completed fix** (per root AGENTS.md). Single commits CAN bundle multiple related changes (e.g., a feature + its tests + the changelog).
- **Small text-only changes** (dead comments, punctuation, rewordings) must NOT be committed on their own — they ride along with the next real change. EXCEPTIONS: edits to AGENTS.md files / Prompt.md / master.md / user-visible strings / changelogs — those ARE committed.
- **Before removing a user-visible feature/UI element**, ASK the user for confirmation (root AGENTS.md durable preference).

## What's New page (v403)

- **`features/updates/WhatsNewScreen.kt` owns the release highlights.**
  `WHATS_NEW_RELEASES` is a hand-authored list of `WhatsNewRelease`
  (`versionCode` + `versionName` + `headline` + `WhatsNewItem`s), newest
  first, and each item carries a glyph, a title, one line of detail and an
  optional `route` rendered as a **Take me there** pill. The copy ships in
  the APK (offline first, no network, no markdown parsing) and a release
  with no authored entry falls back to a plain "on their way" card, so the
  route never opens an empty page.
- **It opens ITSELF once per version.** `CurioNavHost`'s `LaunchedEffect`
  compares `AppPreferences.getWhatsNewSeenVersion` (key
  `whats_new_seen_version`) with `BuildConfig.VERSION_CODE` and pushes
  `CurioRoutes.WHATS_NEW` after a short settle delay, but only from a quiet
  start (splash / onboarding / home), so a deep link or a first-run flow is
  never interrupted. The screen stamps the seen version on OPEN, so backing
  out still counts as seen; **Settings ▸ Updates** keeps a `What's New` row
  as the way back at any time. `WHATS_NEW` is a member of
  `settingsFamilyRoutePrefixes` (shared chrome + crossfade).
- **When a release ships user-visible work, add its entry here too** (same
  commit as the changelog bullet), and keep the copy free of em dashes:
  the highlights read as plain sentences.

## Feedback forms (v403)

- **The server is `supabase/schema.sql` §6f** (`feedback_forms`,
  `feedback_answers`, `feedback_skips`, `curio_publish_feedback_form`,
  `curio_close_feedback_form`, `curio_feedback_tally`).
- **Answers are anonymous by construction, and that is a hard contract.**
  Neither answer table carries a user, device, session or account column, and
  §8's self-check now FAILS the paste if one is ever added. Members may INSERT
  an answer (or a skip) while the form is live and may never SELECT one back;
  reading is the team's, behind `curio_admin_can('forms')`. A member's
  "already answered" and "never show me" states are LOCAL only
  (`AppPreferences.feedbackAnsweredIds` / `feedbackHiddenIds`), because
  enforcing one-answer-per-member on the server is exactly what would make an
  answer traceable.
- **The team's switch is `community_admins.can_manage_forms`**, surfaced in the
  app as `CommunityAdminRow.canManageForms` and `allows("forms")`.
  `curio_set_community_admin` takes it as its eighth argument (the old
  seven-argument function is DROPPED first, so a stale overload cannot silently
  forget the permission).
- **Cadence lives on the server**: one publication per 14 days, tests never
  count, the owner may override, and publishing closes whatever was live. A
  TEST publishes as `status = 'live' AND is_test = true`, which the RLS policy
  hides from every non-team reader, so the team walks the real member UI
  without the walkthrough reaching the members.
- **The app side is `data/supabase/FeedbackApi.kt`** (models, ceilings, cadence
  constant, REST + RPC) and `features/feedback/FeedbackFormScreen.kt`
  (`FeedbackFormState`, the standout `FeedbackFormCard`, the answering
  `FeedbackFormSheet`). The sheet is mounted ONCE at the NavHost root, so
  Home's card and Support's card and row all share one sheet.
- **Where it shows:** Home's card under the deck, Support's card at the top of
  the page plus a row inside the Feedback card, and nothing at all when no form
  is live. A member who is signed out is told to turn Online mode on rather
  than being shown a form they cannot send.

## Cabinet covers, the open book and the art lane (v407)

- **A persisted cover URL is a HINT, never a verdict.**
  `CabinetCoverCache.ensureLocalCover` walks a candidate list (the persisted
  URL, the topic's authored `imageUrl`, then each provider's own cover) and
  writes a URL to the store ONLY in the same breath as the bytes that came
  back from it (`downloadBytes` requires more than 512 bytes, so Open
  Library's 1x1 GIF placeholder fails). The old path persisted the first URL
  that RESOLVED — usually an authored placeholder — and then failed to
  download it, so the item read as "already resolved" forever and kept its
  blank plate (the reported "many book covers doesnt load"). Never
  reintroduce a resolve-then-persist step; `resolveAndPersist` was deleted for
  exactly that reason.
- **For books, a provider means THAT provider.**
  `BookCoverFetch.providerCoverUrl` (suspend, IO dispatcher) is the
  provider-exclusive resolver — iTunes Search, Open Library's title cover, or
  LibraryThing. `BookCoverFetch.resolveCoverUrl` keeps its authored-first rule
  for the reveal poster and the share card only. Routing the cover-source
  switch through the authored-first rule is what made "Open Library" a no-op
  for every book that carried an `imageUrl`.
- **Failures are remembered per RUN, not forever.**
  `CabinetCoverCache.missedThisRun` keeps the Cabinet's warmer from
  re-searching the same fruitless item on every visit inside a session, while
  a fresh launch retries (a miss may just have been the network). Never move
  a miss into `AppPreferences` — a stored failure is what left covers blank.
- **Book tiles cascade too.** `V2JacketArt`'s live resolve runs for books as
  well (iTunes then Open Library); it used to skip `V2Kind.BOOK` entirely,
  which is the other half of the blank-cover report. Books persist through
  `setBookCoverUrl`, albums and series through `setSheetArtUrl("kind|name")`.
- **The open book (`ReadingArt` in CabinetShelves.kt) is drawn from six
  numbers**: `gutter`, `half` (each page's fore edge from the gutter),
  `pageTop`, `pageBot`, `dip` (how far a sheet sinks INTO the gutter) and
  `lift` (how far its fore edge leaves the table). Both halves derive from
  them, so they cannot disagree — do not hand-place points in that path
  again. The ruled lines stop clear of the gutter (the binding margin) and
  the ribbon falls past the book's own foot.
- **Opening a Cabinet level ANIMATES.** `CabinetV2Content` wraps its
  `key(openLevel)` content in one `Box` whose `graphicsLayer` reads
  `levelSwap` (alpha + 0.975 to 1 scale + 16dp rise, tween 300): the layer
  updates without recomposing the grid, and the level's own scroll position is
  created fresh by `key(openLevel)`, so the motion covers it. Every level
  change — a collection, a shelf, the Cupboard, back home — goes through it.
- **The empty-Cabinet suggestion rails are REMOVED** (user request, 2026-09-19).
  There is no "Your Cabinet is empty" block, no shuffled picks and no Shuffle
  pill: `v2HomeItems` leads with the Cupboard card and the shelves.
  `V2EmptySuggestions`, the suggestion state and its effect, and the
  `showSuggestions` / `suggestions` / `onShuffle` / `onOpenSuggestion`
  parameters are gone. Do not add a second empty state to a page that is never
  blank.
- **The art lane asks both sources AT ONCE.** `ArtworkFetch.artwork` runs the
  Met and Wikipedia lookups in parallel (`coroutineScope` + `async`); they are
  independent, and running them back to back made a painting's sheet wait for
  the sum. `metObject` opens at most THREE records and prefers
  `primaryImageSmall` — the web-size image — over `primaryImage`, the museum's
  full-resolution original that is routinely several megabytes behind a 230dp
  band.
- **A maker is a person, so the maker lane has a Wikipedia half.**
  `ArtworkFetch.makerInfo(name)` returns `MakerInfo` (prose + lead image +
  article); it is what makes an ARTIST or PAINTER page say something when the
  Met holds no attributed work. `makerArtwork` falls back to that portrait for
  the reveal card, and the MAKER sheet shows the extract, the portrait in its
  header and a THE RECORD door above the works list. `wikiLeadImage` (the
  author lane) is a thin wrapper over the same `wikiPerson` lookup now.

## Covers that land, one Open Library visit, and a sheet that opens FULL (v410)

- **A book shelved from a topic reveal carries its own facts.**
  `TopicRevealScreen`'s shelf bridge used to create the book with nothing but
  its title, author and `catalogId` — so the shelf, the book page and the
  Cabinet's personal shelf all drew a code-generated plate for a book Curio
  knows everything about (user report: "the book covers are not loading in the
  book page now"). The topic's own `imageUrl`, `pageCount` and chapter count
  come along now: no lookup, no network. If you add a door that creates a
  shelf book, give it the topic's own facts too.
- **A missing cover is FETCHED, in one place: `BookCoverWarmup`.**
  `ensureCover(context, book)` resolves art for a book whose `coverUrl` is
  blank — the CATALOG's own cover for a book with a `catalogId` (local, and
  adopted even with cover fetching OFF), else `CabinetCoverCache.ensureLocalCover`
  (the v407 verified-bytes cascade: persisted URL → authored URL → iTunes →
  Open Library), and the URL is written onto the book's ROW only after its
  bytes arrived (`PersonalRepository.setCoverUrl`, a column-scoped write, so a
  book can be mid-read while its artwork lands). Callers bump
  `CabinetCoverCache.version` ONCE per batch. The shelf warms every coverless
  book; the book page warms the one it opened.
- **`BookCover` prefers the art the app already has.** The cached file for the
  title (read through `CabinetCoverCache.version`, so a cover landing while the
  plate is on screen appears at once), then the row's `coverUrl`, with the
  generated cover underneath as the loading/error state. Never draw a shelf
  plate from `coverUrl` alone again — the row can be blank while the bytes are
  already on disk.
- **The look-up is ONE Open Library visit, and the catalog closes the chapter
  question.** See the Auto-fetch and chapter bullets under Local Contracts.
  The rules that must not regress: one title search carries every field the
  pass needs (`key,title,number_of_pages_median`); the work is read at most
  once, and only when the description or a table of contents is wanted; a book
  whose `catalogId` has a chapter list never asks for one; Crossref is asked
  only after Open Library came back empty, and Google Books only when
  `GOOGLE_BOOKS_API_KEY` is set.
- **A sheet opens with the episode list its preview already earned.**
  `SeriesEpisodeFetcher.cached(name)` and `AnimeEpisodeFetcher.cached(name)`
  return the list those fetchers already resolved (the MAPPED one —
  `seriesCache` beside the raw JSON memo), and `EpisodeNotesSheet` seeds its
  `episodes` state from it (authored episodes still win) instead of opening on
  an empty list and flashing "No episode guide yet." for a guide the reveal
  card was already showing (user report: "sometimes the series or anime data is
  already shown in preview but it loads again when the page opens"). The anime
  card seeds its chip row the same way. A PROVIDER's own list is never run
  through `enrich` — that merge is for the authored list, and re-merging by
  season/number would let another show's air dates land on its rows.

## One color-theme door, one home hero, a star map, and the reader's page (v411)

### The color theme is ONE choice

- **v414 — THE THREE PANTONE THEMES WERE REMOVED.** `PantoneThemes.kt` is
  deleted, `activePantoneTheme()` / `activePantoneThemeNow()` are gone, and
  every branch they fed is back to the app's own palette (the schemes, the
  shared heroes, `CategoryInk.kt`'s resolvers, the reveal pill, the notification
  tint, the category card's selected crown, `curioRoseInk` / `curioGoldInk` /
  `curioSageInk`). `COLOR_THEMES` is `CURIO` / `AZURE` / `MATERIAL` / `LANE`, and
  a stored `pantone-*` id migrates to `COLOR_THEME_CURIO` on read
  (`LEGACY_PANTONE_PREFIX` in `AppPreferences.kt`). Everything below that still
  mentions Pantone is history — do not re-add it.
- **`AppPreferences.colorThemeState` is the one fact** (`COLOR_THEME_CURIO` /
  `_AZURE` / `_MATERIAL` / `_LANE`). `setColorTheme` writes it AND keeps the three legacy
  switches (`materialThemeState`, `heroBlueState`, `heroFollowLaneState`) in
  step, because every other surface still reads those — the legacy keys are not
  dead, they are one choice expressed the way the rest of the app already asks
  for it. `getColorTheme` DERIVES the value from them when nothing has been
  stored, so an existing member keeps the look they had.
- **The Appearance page shows ONE row** (`ColorThemeRow` → `ColorThemeSheet`,
  in `SettingsSectionScreen.kt`): each theme is a row with a three-swatch
  preview and its Pantone reference. The Material / Hero / Adaptive Hero
  switches are GONE from the page, and their deep-search keys now point at
  `appearance-color-theme`. Pastel colors and Category tint stay on the page.
- **`PantoneTheme` (`ui/theme/PantoneThemes.kt`) owns the three Pantone
  palettes** — page, hero, ink — each with a LIGHT scheme and a DARK twin built
  from the same three numbers (`schemeFor(dark)`), plus the tone ladder
  (`pageFor`, `heroFor`, `accentFor`, `onHeroFor`, `pageInkFor`, `cardInkFor`)
  because two of the three Pantone inks cannot carry body text on their own
  page. **v413 rebuilt the ladder — read the "Pantone Tone Ladder" section
  below before changing any tone.**
- **TWO RULES THAT MUST NOT REGRESS in these themes.** (1) **No transparency:**
  every colour in the scheme is opaque — a faded role is a `lerp` mix, never
  `copy(alpha = …)` — and any app code that tints by alpha asks `curioTintOn(base,
  tint, alpha)`, which resolves SOLIDLY under a Pantone theme. (2) **No card
  borders:** cards ask `curioCardEdgeColor(fill)`, which answers `fill` itself
  under a Pantone theme (an edge painted in the card's own colour — no border to
  see, no border code path to branch) and the theme's `outlineVariant`
  everywhere else. `outlineVariant` in a Pantone scheme is therefore the DIVIDER
  hairline on a hero-filled card, not the card edge.
- The shared heroes read the Pantone palette first (`settingsRoseAccent`,
  `settingsReadableInk`, `settingsCardAccentInk` in `SettingsHubScreen.kt`), so
  a Pantone theme paints the torn heroes, the option cards and their icons in
  its own three colours.

### v420 — the five NAMED THEMES (Jade, Orchid, Ocean, Sand, Ember)

- **`CurioNamedTheme` (`ui/theme/NamedThemes.kt`) is a FULL theme, not an accent
  swap.** Five entries, each built from ONE hue: they paint the page, the card
  ladder, the hero/button fill, the second and third fills, the ink and the
  error colour, and each ships a LIGHT scheme and a DARK twin (`schemeFor(dark)`).
  Every tone is `tone(hue, sat, light)` — a reading of that one hue with the
  theme's `second`/(`+38°`) and `third`/(`−46°`) adjacent hues for depth. No
  second colour family is ever introduced.
- **THE DARK TWIN IS A DEEP JEWEL (member's choice).** Not a neutral near-black:
  the page is a calm, deep dark of the theme's own hue (`tone(hue, 0.36, 0.11)`),
  cards step up it (`0.30` hold, 0.15 → 0.27), and the hero is a DEEP jewel tone
  (`tone(hue, 0.50, 0.33)`) carrying a PALE ink. **v421 moved the hero from
  0.56 down to 0.33 and the ink is why** — at 0.56 the pale ink landed around
  2:1 on hue 158, which is exactly why the member's own list ran "jade is the
  worst, then orchid, ocean, sand, ember" and why the whole dark twin felt off.
  `readableOn` tries white first and the dark body ink second, and the dark body
  ink is a BRIGHT tone — so in dark mode the hero fill must be deep enough for
  white to clear 4.5:1 on its own. Do not "brighten" this hero back up.
- **THE LIGHT SCHEME FOLLOWS THE APP'S OWN RULE.** An airy page
  (`tone(hue, 0.38, 0.93)`), a near-white card ladder that climbs ABOVE it
  (0.995 → 0.86), an AIRY hero (`tone(hue, 0.46, 0.66)`) with the theme's own
  DEEP ink on it, and a near-black body ink carrying the hue
  (`tone(hue, 0.30, 0.18)`). **v421 — the hero used to be DEEP (0.40) and the
  member called it back** ("the hero feels too deep in light mode"): beside
  Curio's own pastel rose banner a 0.40 slab read as a heavy block, so the light
  hero is now the airy reading and the deep ink does the reading, exactly as the
  app's own rose banner does. Contrast bars: body ink ≥ 4.5:1 on the page and the
  deepest card, hero ink ≥ 4.5:1 on the hero (via `readableOn`), accent ink ≥
  3:1 on the page — re-measure with `contrast` if a tone moves. The dark accent
  ink is `tone(hue, 0.40, 0.69)` — **v423, because 0.78 cleared the contrast bar and read as NEON on a deep page** ("those new accnets are still bad only is dark mode its too bright"); 0.69 with the chroma eased still clears 4.5:1 there, and the night's `secondary`/`tertiary` fills are muted with it (0.42 / 0.44 of lightness). **v423 also made the card ladders STRIDES WITH VARIED CHROMA** (light 0.14→0.30, dark 0.34→0.24 climbing, lightness 0.995→0.85 / 0.14→0.30) — five steps at one chroma 0.03 of lightness apart is why every settings card read as the same shade of the same theme; at 0.72 it came out around 4.2:1 on the deepest
  card step, which is what "the button and texts blend" was.
- **HOW IT IS WIRED.** `AppPreferences.NAMED_ID_PREFIX` (`named-`) + five ids in
  `COLOR_THEMES`; `namedThemeId()` reads the stored id WITHOUT the data layer
  importing the ui type (the same trick the retired Pantone prefix used).
  `curioColorScheme()` returns `named.schemeFor(isCurioDarkTheme())` before the
  paper flip. The shared heroes answer it first: `settingsRoseAccent` /
  `homeRoseAccent` / `profileRoseAccent` → `MaterialTheme.colorScheme.primary`,
  their ink helpers → `onPrimary`, `settingsCardAccentInk` → `accentFor`,
  `settingsCardChipTint` → `primary`, `curioRoseInk` → `accentFor`. A named id
  is neither Material nor a lane nor azure, so `setColorTheme` clears the three
  legacy switches exactly as Curio rose does.
- **A NAMED THEME'S PAGE WINS over the category wash.** `CategoryInk`'s page /
  surface resolvers (`categoryBackgroundWash`, `categorySurface`,
  `categoryChipSurface`, `categorySurfaceMoodBoard`) return the scheme's own
  background/ladder and `notesSheetContainerColor` its dialog surface — so the
  theme's page is what the member sees, not a lane tint. Lane ACCENTS
  (`categoryInk`, `themedAccent`, `headerAccent`) are deliberately NOT collapsed:
  lane chips keep their identity on a themed page.
- **The sheet lists them** (`SettingsSectionScreen.colorThemeChoices`), each row
  previewing its own page/hero/ink in the current mode — read live off
  `pageFor`/`heroFor`/`accentFor`, so a re-tune adds no second source of truth;
  `colorThemeLabel` names them. A named theme paints its own page pair, so it is
  outside the Paper row entirely (see the card-ladder section: that row is
  Adaptive Hero's alone as of v421).

### v421 — the named themes re-pitched, and the dark rose muted

Three member reports landed at once and all three were the same mistake read
from different sides: a hero tone was chosen for how it looked in isolation
rather than for the ink it has to carry.

- **LIGHT HEROES WERE TOO DEEP, DARK HEROES TOO LIGHT.** Fixed in
  `NamedThemes.kt` — see the two hero bullets in the v420 section above for the
  numbers and the reason. The dials to re-tune are `heroFor`, `accentFor`,
  `pageFor` and the two ladders; the light hero's ink flips to the theme's deep
  ink whenever it gets airy enough that white stops reading, which is intended.
- **THE DARK CURIO ROSE IS MUTED.** `CurioColors.HomeRosewoodDark` is `#713842`
  (was `#7D2C3B`) and the pastel-dark rose branch in `settingsRoseAccent` /
  `homeRoseAccent` / `profileRoseAccent` takes a ~0.37 hold instead of ~0.59.
  The night banner was carrying the light hero's vibrancy onto a black page,
  where it read as neon (member: "in dark mode the curio rose is too vibrant in
  dark mode maybe mute it"). **All three of those resolvers carry the same
  branch — change all three together, they are one value in three files.**
- **The accent ink finally clears its bar.** `CurioNamedTheme.accentFor(dark)`
  is `tone(hue, 0.46, 0.78)`, which reads ≥ 5:1 on the deepest card step; the
  places the member named (Home's stat figures and their glyphs, Profile's stat
  pane) sit on exactly those steps.

### v426 — ONE card plate for Profile + Settings, and the named themes' ink

Two faults that only showed under a named theme, because both are cases where a
scheme role means something different there than in the app's own schemes.

- **THE SETTINGS OPTION CARD WEARS THE PROFILE CARD'S PLATE.**
  `SettingsOptionCard` (`SettingsPageComponents.kt`) asked for
  `surfaceContainerHigh` in dark mode — the "pill inside a card" rung — while
  `CurioSettingsCard` asked for the scheme's card rung carrying a whisper of the
  hero accent. On the app's own neutral greys the two were close enough to pass;
  under a named theme at night the option card measured **L 0.225 against
  Profile's 0.158 — 1.94x the luminance** (and 2.0–2.9x the same card in the
  app's own dark scheme), so every Settings sub-page read as a lit panel beside
  Profile's quiet glass (member: "in profile the xp progress settings etc
  background is good, but inside the settings the appearance and its sub pages
  background is still bad … fix it please in dark mode"). Both families answer
  **`curioSettingsCardFill()`** (`ui/components/CurioSettingsCard.kt`) now — one
  value, so the two screens cannot drift apart again. If a settings surface looks
  too bright or too flat at night, the dial is that function, not the call site.
- **`primary` AND `tertiary` ARE FILLS IN A NAMED SCHEME, NOT INKS.** The app's
  own dark scheme puts the BRIGHT coral in `primary` and the bright mint
  everywhere in `tertiary`, so `tint = MaterialTheme.colorScheme.primary` reads
  in both modes there. A named theme puts its **deep hero fill** in `primary`
  (L 0.30 — deliberately, see the v421/v425 notes) and a dark tone in
  `tertiary`, so every glyph, label and glyph-tint that asked those roles as INK
  landed at ~1.6–1.9:1 on the theme's own dark page — the member's "in dark mode
  in many themes the text visibility is bad … the texts have black or dark
  color". Ink now asks **`curioAccentInk()`** / **`curioTertiaryInk()`**
  (`CurioTheme.kt`), which hand back `primary`/`tertiary` untouched for every
  non-named theme (so nothing outside the five moved) and the named theme's own
  `accentFor` / `onTertiaryContainer` under a named one. A FILL still keeps
  `primary` — the hero, a bar, a selected pill want the deep tone.
- **THE JOURNAL'S COFFEE HAD NO NIGHT TWIN.** `personalQuoteColor()` promised
  "the milky coffee twin" in its own doc and returned the light coffee
  unconditionally, and `personalAnnotateLinks` hard-coded the deep coffee
  (`#5C3A20`, ~1.6:1 on the journal's dark paper) for every URL — the journal's
  half of "the texts have black or dark color". The quote takes `#C09263` at
  night, the link ink is passed IN (it runs outside composition — the caller
  resolves `personalQuoteDeepColor()`), and `personalMoodInk` is `@Composable`
  with a night floor of L 0.62 so Heavy's slate and Tired's mauve stop sitting at
  ~2.3:1. The journal's paper is `#17130F`, so this is the one screen where those
  inks live.
- **STILL OPEN (deliberately not swept).** These are LISTED, not fixed — each
  needs a judgement call, and none of them is a one-line role swap:
  `color = MaterialTheme.colorScheme.primary` appears in 71 places and is a
  MIXTURE of fills and ink (PetDesignerScreen's 26 are its own playground palette
  on purpose — leave them); `secondary` is asked as a selected-state ink/fill in
  `ShareHubScreen` (408/551/565) and `TopicShareCard` (12695/12710) — the right
  answer there is the design question "what does selected mean on a named
  theme", not a role swap; and the 12 `outlineVariant`-as-ink hits are mostly
  borders and dividers, which is correct usage.

### The Pantone tone ladder (v413) — RETIRED in v414, history only

- **RETIRED.** `PantoneThemes.kt` no longer exists, so nothing below is live
  guidance. The app's own card ladder lives in `CurioTheme.kt`. Kept as a record
  of the v413 design only.
- **WHAT WAS WRONG (member: "all 3 are bad, dont use too deep colors for the
  cards or background … use more shades palette per pantone theme").** v412
  built the card ladder by DEEPENING THE HERO five times: on Pantone Terracotta
  (hero 2350 U) that made every sheet, settings card and journal block a deep
  brick red, each nested step darker still — and on the two pale heroes (Cream's
  P 109-10 U, Lime's P 163-8 C) it walked into grey-olive mud. The hero fill
  was being asked to be both the page's furniture and the page's accent.
- **THE FIX: THE LADDER IS THE PAGE'S OWN HUE AND IT CLIMBS.** The page keeps
  its Pantone number; every step above it is a near-white reading of the page's
  hue — `surfaceContainerLowest` 0.975, `Low` 0.945, `Mid` 0.915, `High` 0.882,
  `Highest` 0.845 — so a card separates by LIGHTNESS (the app's own rule) and
  nothing in the app is ever a dark block. The night ladder is the same idea
  downward: page 0.075, then 0.10 / 0.125 / 0.15 / 0.175 / 0.20, small steps
  because dark-mode steps must be small.
- **THE NIGHT SIDE IS FITTED TO THE APP'S OWN DARK LANGUAGE (v417).** The dark
  twin's page was 0.115 — a warm brown-grey that read as a dim light theme
  rather than a night one (member: "do a better work best fitted for dark mode
  too"). The page is a near-black of its own hue now (0.075, sat 0.22), the
  ladder above it starts at 0.10 and steps 0.025 a rung — the stride the app's
  own dark scheme uses (`#121212` → `#2C2C2C`) — and the hairlines came down
  with it (`outline` 0.26, `outlineVariant` 0.16): on a page this dark the
  container steps and the hairlines ARE the separation, because no shadow
  exists there. The bright roles are untouched and stay the app's dark
  language — a pale hero fill with a deep ink on it, a bright pale ink for
  accents, a bright gold/sage. Every pair was re-measured on the new ladder.
- **THE HERO NUMBER IS THE ACCENT AGAIN, WITH A CONTAINER TWIN.** `primary` is
  the Pantone hero (the banner, a button, a selected rail), `primaryContainer`
  is its PALE twin (0.90, what a selected chip or tinted rail wears) and the
  deep hero reading (0.28) is what reads on both — so a chip and the button
  beside it are finally tellable apart. `secondary` is the INK number at accent
  depth (0.40) with its own container twin, `tertiary` is the PAGE number at
  accent depth — three real, separable fills instead of three copies of the
  hero.
- **THE DERIVED ROLES ARE PICKED, NOT GUESSED.** `warm` is the most SATURATED
  number in the amber band 15°–70° (Terracotta's page and ink both qualify and
  the ink is the real amber, so the duller page must not win); `cool` is the
  coolest number; `goldInkFor` = the warm one at ink depth; `sageInkFor` = the
  cool one's hue walked FORWARD (through green) toward 250° by ≤45° at low
  saturation — walking the short way round made Pantone Terracotta's "sage" a
  second brick, olive is the one cool-family ink a warm brief can honestly give;
  `errorFor` = the warm one's hue walked toward red by ≤35° (`alert()`), because
  a delete button painted in Lime's indigo reads as decoration.
- **THE SCHEMES ARE COMPLETE.** `errorContainer` / `onErrorContainer` exist
  (they used to fall through to Material's baseline PINK — the one colour in a
  Pantone scheme that was not from the brief) and `scrim` is the theme's own
  near-black.
- **EVERY PAIR WAS MEASURED BEFORE IT WAS WRITTEN DOWN**, in both schemes: body,
  muted, container, gold, sage and error inks clear 4.5:1 on their own page AND
  on the deepest card step; the three container twins clear it against their own
  contents; white clears it on the light error fill. The one named margin: Lime's
  indigo accent is 4.26:1 on the deepest nested step (above the 3:1 bar icons and
  large labels are held to), which is why the accent stays the real Pantone ink
  instead of deepening into a colour indistinguishable from body text. If a tone
  changes, re-measure the pairs — `contrast()` is `internal` in the same file.
- **UNCHANGED, DELIBERATELY:** the lane/category fills still resolve to the hero
  number (`themedAccent`) — a lane card is a fill, not a plate, and it is the one
  place the palette is allowed to be saturated (the app's own lane cards are
  700-level deep too). The 36-lane hue collapse, the no-alpha and no-card-border
  rules, and the shared-hero branches all still hold.

### v419 — the drawer chart is a SKY again (the v414 lattice was too symmetric)

- **IT COVERS MORE OF THE DRAWER.** `DrawerStarMapHeight` 188dp → **254dp**
  (v414) — at the old height the chart read as a strip wedged between the brain
  stats and the lane readout; that part still holds. **v422: 254 → 320dp** (see
  the star-map section — the height is the only dial that sizes the pattern).
- **POSITION: a golden-angle scatter with a hashed wobble.** `starScatter(count)`
  replaces `starLattice` — the member reversed v414: "the drawer graph is bad …
  the previous version was at least better … its too symmetric". The i-th star
  sits at the GOLDEN ANGLE (2.3999632 rad) times i, its radius grows with
  `sqrt((i + 0.55) / count)` so the disc fills evenly, and a deterministic 0..7
  hash (`i * 2654435761L and 7`) wobbles the angle and radius so no two
  neighbours align. Still deterministic — the same lanes always land in the same
  places — and knowledge still never MOVES a star (size and brightness only), so
  the map stays the landmark the member learns.
- **NO GRID.** The v414 astrolabe (one circle per orbit, `STAR_CHART_SPOKES` =
  12 spokes, a hub) was the symmetry the member rejected, so it is GONE — and so
  are those constants and `TWO_PI`. A faint `starDust(STAR_DUST_COUNT = 46)`
  field (a deterministic LCG in unit space) gives the panel its depth instead.
- **HAIRLINES ARE LOCAL AGAIN.** `starRingLinks` is GONE; `starLinks` joins each
  star to its NEAREST neighbour (unit-space `getDistanceSquared`), so the sky
  reads as loose constellations, never rings or a regular mesh.
- **SLOTS STAY.** `StarSlot(angle, radius)` is unchanged, and
  `starPoint(slot, hub, unitPx)` is still the ONE placement function the canvas
  and the hit test both call (pixel space, radius × the SHORTER side) — so the
  scatter stays round in a wide drawer AND a tap can never miss the star it
  looks like it hit.

### v413 — the Material deck wears the device colour

- **THE MATERIAL SHUFFLE DECK IS THE MEMBER'S WALLPAPER TONE.** `MaterialTheme`
  identity IS the device palette, so the whole deck FAMILY resolves from
  `MaterialTheme.colorScheme.primary`: `SpinScreen` resolves it once as
  `materialDeckFill` and passes it to `CurioMixedDeck.mixedDeckAccent` (new
  `materialDevice` parameter — the deck accent, which the peek slabs' deepening,
  the spin button and the confetti all derive from) and to
  `CurioMixedDeck.mixedDeckGradient` (same new parameter).
- **SINGLE LANE: DEVICE COLOUR FOR 70%, THE LANE'S ACCENT FOR 30%.**
  `CurioGradients.materialDeckBlend(device, accent)` returns TEN evenly-spaced
  stops — the first seven hold the device colour (positions 0.00–0.667) and the
  last three ramp through OKLab into the lane's own family accent, so a plain
  `Brush.verticalGradient` renders the split with a BLENDED seam. No stop-position
  plumbing anywhere: the hold is expressed by repeating a colour across evenly
  spaced stops. The hero ticket brushes through `Brush.verticalGradient(gradient)`
  (a new `materialThemeOn` branch in `HeroTicketCard`'s `ticketBrush`, before the
  diagonal crown/base sweep).
- **MIXED DECKS ARE THE FULL DEVICE COLOUR, STYLE-VARIED.**
  `materialPrimary != null` is the mixed signal; the stops become
  `[device, device deepened at the foot]` and the EXISTING per-deck brush
  (`CurioMixedDeck.mixedDeckHeroBrush` — diagonal, reversed diagonal, radial,
  keyed off the deck's category set) lays it out, so different mixes still read
  differently without a second palette.
- **THE CARD'S WORDS RESOLVE AGAINST THE DEVICE FILL.** `HeroTicketCard`'s ink
  and `PeekCard`'s ink take a `materialThemeOn` branch through
  `curioFillInk(gradient.first())` / `curioFillInk(cardStops.first())` — white
  where white reads, a deep same-hue ink where it does not — instead of the
  lane's family on-fill, which can vanish on a light wallpaper primary.
- Gated by the Material theme itself (an opt-in Appearance choice), so no extra
  experiment toggle: picking Material is picking this.

### v412 — the gauge, the voice wave, the mood pills, the System glyph

- **THE READING GAUGE ANIMATES, AND IT IS A CONTROL (v418).** `ReadingGauge`
  resolves its fill through `animateFloatAsState` and prints the figure from the
  SAME animated value, so a held stepper counts up smoothly instead of the bar
  snapping many times a second. The spec is a zero-length `tween` WHILE
  `scrubbing` (the fill must sit under the finger), else spring 0.90/320. When
  `onScrub` is non-null the bar is draggable/tappable: the card maps the 0..1
  fraction to a page (when the file has pages) or a chapter and writes it through
  the same overlay setters the tiles use (`setPageTo` / `setChapterTo`).
  **v421 — ONE GESTURE, AND A STABLE KEY.** This node used to carry TWO
  detectors (`detectHorizontalDragGestures` + `detectTapGestures`), each keyed on
  `onScrub` — and `ProgressCard` mints a fresh lambda on every recomposition, so
  the first scrub wrote the new place, the card recomposed, `pointerInput`
  RESTARTED and cancelled the gesture in flight: only the tap survived, because
  its seek runs on the DOWN before any recomposition (member: "the pill directly
  teleports to where i touch and also when i try to drag it it does nothing, it
  doesnt move … only tap works"). It is ONE `awaitEachGesture` keyed `Unit`,
  reading the callback through a `rememberUpdatedState` (`scrubNow`), and it
  seeks from the first MOVEMENT rather than from the down — a press that never
  moves is still a tap and seeks on the release. **Never key a pointerInput on a
  callback a caller re-creates; key it on `Unit` and read the callback live.**
  The
  KNOB is drawn inside the Canvas and only fades in for the touch (`knobAlpha` /
  `knobScale`), never at rest — the touch target is a 28dp node with the 11dp
  track drawn inside it. The `percent` parameter is GONE — the figure is derived
  inside now, so the two can never disagree. The fill is a
  `Brush.horizontalGradient(accent → lerp(accent, White, 0.30f))` and a slow
  `rememberInfiniteTransition` sheen (3.4s, `clipRect`ed to the FILL so it can
  only travel along the progress) drifts across it at rest. Every stop is an
  opaque `lerp` — no alpha on a fill. **v418 — the band's ends fade to
  TRANSPARENT (`Brush.horizontalGradient(colorStops = …)`), so its restart at
  the left edge is invisible; the old hard-edged band jumped** (member: "the flow
  animation is bad of it").
- **THE MOOD PILLS (and their neighbours) ARE OPAQUE.** "How did the day feel"
  wore a 20% TINT of the mood ink, so the journal's page showed through the
  collapsed pill and the picked chip. All three surfaces now use
  `lerp(<the surface it sits on>, tint, alpha)` — the mood pill and chip against
  `journalPaperRaised()` / `journalPaper()`, and the same treatment swept across
  the family: `TopicNoteScreen` (the topic row, its glyph disc, the open-topic
  pill, the note disc), `PersonalHome` (both discs + `NewChip`),
  `JournalListScreen`'s empty-disc, `BookShelfScreen`'s selected scan door,
  `BookDetailScreen`'s "Mark finished" pill, and the two empty-state discs in
  `ChapterScreen` / `BookReviewScreen`. **The rule: a fill is mixed into the
  surface it sits on, never alpha-laid over it.**
- **THE SYSTEM SEGMENT WEARS `CurioIcons.Contrast`.** `HalfCelestialGlyph` (the
  hand-drawn half-sun/half-moon) was deleted: at 20dp it read as a smudge, and
  the bundled `contrast` font glyph is the same half-lit circle drawn properly —
  which is what the onboarding's System chip already wore (member: "for the
  device in theme option change the icon please it looks bad").

### v412 — a Pantone theme paints the WHOLE app ("no other colors") — RETIRED in v414

- **RETIRED.** All the branches described here were deleted in v414 (see "The
  color theme is ONE choice" above). History only.
- **THE BUG: Home and Profile had their OWN copies of the hero resolver.**
  `homeRoseAccent()` / `profileRoseAccent()` (and `homeReadableInk` /
  `profileReadableInk`) fell straight through to rose / azure / the last Spin
  lane and never called `activePantoneTheme()` — only `settingsRoseAccent()`
  did, which is why Settings and the Cabinet hero wore the Pantone colours
  while Home's banner and Profile's did not. Each now takes the Pantone branch
  FIRST (`heroFor` / `onHeroFor`), before the lane, the azure and the rose-wood
  — the same ordering `settingsRoseAccent` has always used. Home's page
  background also skips the lane wash under a Pantone theme.
- **THE ROLES THE THREE NUMBERS NEVER NAMED are now derived, not borrowed.**
  `PantoneTheme` grew `secondaryFor`, `tertiaryFor`, `goldInkFor`,
  `sageInkFor` and `errorFor`. The schemes use them, and `curioGoldInk()` /
  `curioSageInk()` answer them first, so the streak flame, XP and mastery icons
  stop wearing the brand butter/sage. The scheme `error` is no longer
  `CurioColors.WarmCoralRed`. **(v413 re-derived all five — gold now comes off
  the warmest number, sage off the cool hue, and the fills off their own
  number's accent depth; see "The Pantone tone ladder" above.)**
- **THE 36 LANE ACCENTS RESOLVE TO THE PANTONE PALETTE TOO.** `CategoryInk.kt`
  takes an `activePantoneTheme()` branch at the top of `categoryInk`,
  `themedAccent`, `headerAccent`, `readableAccentInk`, `onAccent`,
  `heroHeaderInk`, plus `categoryBackgroundWash` (→ the scheme background: NO
  lane page wash), `categorySurface` / `categoryChipSurface` /
  `categorySurfaceMoodBoard` (→ the passed `base`, i.e. the scheme ladder),
  `notesSheetContainerColor` (→ the Pantone dialog surface) and the
  cover-artwork paths (`notesSheetContainerColorForCover`, `notesSheetPalette`
  → null, so the sheets fall back to the Pantone surfaces). The non-composable
  twins `categoryInkFor` / `themedAccentFor` use `activePantoneThemeNow()`
  (added beside `activePantoneTheme`, for the `remember` calculation lambdas the
  watermark map is built in). Consequence, deliberate: under a Pantone theme
  the lanes no longer carry a per-category hue — that identity is exactly what
  the member asked to drop ("all of them get the colors no other colors").
- **The few RAW-accent call sites were closed too:** the category card's
  selected crown (`saturated`), the reveal hero's `CurioProgressPill`
  (accent + ink + frosted background) and `ExploreSessionService`'s
  notification tint.
- **NOTHING was made transparent or bordered by this pass** — the two Pantone
  rules above still hold, and every new derived role is an opaque `lerp`/HSL
  reading of a page, hero or ink number.

### Home: one hero, with the greeting and the quest inside it

- **The daily quest is INSIDE the torn banner** — `QuestShuffleCard` is called
  from inside the hero's own Column in `HomeScreen.kt`; the below-hero block and
  its 26dp spacer are gone. `HomeQuestHeroHeightPortrait` is 380dp (was 300) and
  landscape 296dp, which is what holds it. `QuestShuffleCard(plate, ink,
  copyInk, …)` takes every colour as a parameter now: the plate is a paper-white
  disc (`lerp(heroFill, Color.White, 0.88f)`) wearing the banner's own ink,
  because a pastel-rose disc would vanish into its own hero.
- **The greeting is one fixed line.** `homeGreeting()` returns "Welcome back"
  (the time-of-day word is gone), and the name under it is the star — 40sp
  ExtraBold against a 26sp SemiBold greeting, so the pair reads as one sentence.
  The hero and the glass header use the same string, so the two header styles
  can never greet differently.
- The "A fresh mix of ideas, picked for you" line under Today's quest is gone,
  and the eyebrow/title pair is `labelLarge` over `headlineMedium`.

### The drawer's lanes are a star map (the grid is gone)

- `DrawerLaneStarMap` replaces the lane grid in the drawer: one star per lane,
  phyllotaxis-scattered by lane COUNT (`starScatter`) so a star never moves when
  knowledge changes, sized and brightened by knowledge, coloured by the lane's
  own accent, and joined to its nearest neighbour (`starLinks`). Stars are
  TAPPABLE (a 30dp halo, nearest star wins) and the readout under the map is
  `CurioLaneDetailStrip`. `CurioLaneGrid` still serves the Stats page — only the
  drawer changed.
- **v422 — NO PLATE, AND THE PAGE IS THE BASE.** The map used to carry its own
  `surfaceContainerHigh` panel (fill + 18dp clip) and every tone in the drawing
  was an opaque `lerp` FROM that panel — which is what made the halos look like
  plate-tinted blobs. The panel is gone (the pattern is the whole thing, drawn
  straight on the drawer) and **`page` = `MaterialTheme.colorScheme.surface` is
  the base for the dust, the hairlines and all three of a star's steps**. If the
  drawer's container colour ever changes, this base changes with it.
- **v427 — THE FIELD FOLLOWS THE BOX ON BOTH AXES (this supersedes v422's "the
  height is the only dial").** `starPoint(slot, hub, width, height)` multiplies
  the polar radius by HALF OF EACH AXIS, so the scatter fills the sky it is
  given instead of being capped by the shorter side. v422's note was only half
  true: the drawer sheet is a fixed 336dp wide
  (`ModalDrawerSheet(modifier = Modifier.width(336.dp))`), so its map measures
  304dp ACROSS and the shorter side was always the WIDTH — a taller sky added
  empty paper above and below the stars and moved not one of them. With the axes
  separate, `DrawerStarMapHeight` 320 → 372dp is real: the sky reads longer and
  the stars spread into it. The GLYPHS are untouched by any of this (every star
  radius is in dp), so a star stays a perfect circle however the box is shaped.
- **v427 — NO COUNTER UNDER THE SKY.** The "N of N lanes explored" line is gone
  (member: "remove the 1 out of 338 lanes explored") — a counter under a map is a
  meter wearing a caption, and the stars already say who is lit. Under the map is
  the tapped star's readout, or nothing.
- **v427 — A STAR'S LIT SIZE IS ONE FUNCTION (`corePxOf`), AND ITS GLOW IS FOUR
  OPAQUE STEPS.** `bornOf`/`corePxOf` (inside the painter) are what the stars AND
  the joins read, which is what lets a hairline stop at the edge of the halo it
  runs into. The bloom is a wide field (3.2× the core at 0.10 of the accent), a
  mid ring (2.0× at 0.22), an inner ring (1.35× at 0.44) and the hot core (0.97)
  — three flat steps read as a dot with a circle around it. The picked star grows
  1.5× and every step brightens (0.20 / 0.40 / 0.64), and an UNEXPLORED lane
  lights too (at the smallest lit size) so a tap on a lane you have not started
  still answers. There is no orbit ring: a circle drawn around a star read as
  chrome rather than as the star responding. An untouched lane is a SOLID dim
  point, never a hollow `Stroke` circle.
- **v427 — A JOIN WEARS THE TWO LANES' OWN COLOURS AND STOPS AT THE GLOW.** Each
  hairline is `lerp(page, lerp(a.accent, b.accent, 0.5f), …)` drawn TWICE — a
  2.6dp faint pass under a 1dp crisp one (opaque mixes, never alpha, like every
  other tone on this canvas) — brightened when the picked lane is one of its ends,
  trimmed by `HaloTrimFactor` (1.35, the mid ring's own radius) plus 2dp at each
  end, and DROPPED outright when its span exceeds 0.15 of the box's TWO AXES
  ADDED (a width-only cap would drop the vertical constellations now that the
  field is taller than it is wide): with no
  orbits left a lone star's nearest neighbour can be halfway across the map, and
  one such line ruins the chart. **A flat grey hairline is what the member
  rejected** — grey over a coloured sky reads as wire.
- Nothing on that canvas is transparent (every colour is an opaque `lerp`), and
  the light-up is a ONE-SHOT `Animatable`, never an infinite transition: the
  drawer is composed while it is closed, so an idle twinkle would spend the
  battery on a surface nobody is looking at.

### The reader owns the page it is showing

- **`BookReaderScreen` writes its live page back onto the book row** —
  `repo.setPage(bookId, livePlace.index + 1)`, debounced by
  `ReaderPageMarkDebounceMs`, for `ReaderContent.Pages` only (a reflowable
  book's position is a block index, not a page of anything). The book page's
  progress card picks between the row's `currentPage` and the reader's position
  by "the most recent write wins", and that rule broke whenever anything ELSE
  wrote the row (a cover, a blurb, the document): writing the page from the
  reader keeps the two answers identical, so reading always wins.
- **A chapter move carries the page with it** — `chapterStartPage(chapter,
  chapters)` in `BookDetailScreen.kt` is the reverse of `chapterForPage`, so the
  two steppers agree in BOTH directions; a chapter with no `pageStart` leaves
  the page alone rather than inventing one.
- **`ReaderHoldButton` is the page bar's arrow**: a tap turns one page, holding
  past `PageTurnHoldDelayMs` repeats every `PageTurnHoldRepeatMs` until the
  finger lifts. One `detectTapGestures` owns both gestures, so a hold can never
  also fire the tap that ended it.

### v411, pass two — one flat card language, the journal's own colours, one progress bar

- **NO CARD BORDER, IN ANY THEME.** `curioCardEdgeColor(fill)` now answers `fill`
  unconditionally (it used to be the Pantone-only rule), and the soft warm
  `Modifier.curioCardShadow(shape, elevation)` — a DeepPlum-tinted low-alpha
  shadow, applied BEFORE the fill, a no-op in dark where the theme's glow does
  the work — is what separates a card from its page. Its default step is 5dp
  since v414 (was 3dp). `outlineVariant` still draws DIVIDERS. Applied at: `CurioSettingsCard`, `SettingsOptionCard`,
  `StatsCard`, `StatsDoorChip`, the settings rail tab / quick-tool chip / search
  card, the lane tile (selected only), and the journal's own cards.
- **THE CREAM IS SUBTLER.** `CurioWhitePageLightScheme`'s card ladder was pitched
  lighter (a card is `#FCF7EC` on the white page); the steps still climb evenly,
  which is what keeps nesting legible without a hairline.
- **THE JOURNAL HAS ITS OWN COLOURS** (`PersonalTheme.kt`): `journalPaper()` (a
  warm parchment carrying a whisper of the member's accent, so it follows a
  Pantone theme, a lane-following hero or the rose without knowing which),
  `journalPaperRaised()`, `journalInk()` and `journalRule()`. Used by the journal
  list rows, the empty card, the editor's mood pill and chips, and the canvas's
  prints — a print and the page under it are one paper.
- **ONE WAVE IN A VOICE NOTE.** `PersonalVoiceBar` drew the voice's envelope
  TWICE (a mirrored pair of strokes, which is the "2 wave" the member called
  out); it is ONE stroke now, bucketed to ~18 steps (the loudest sample of each
  bucket wins) so a rise and a fall every 3dp cannot read as a fuzzy band, with a
  soft under-stroke for depth. **v412 made the stroke a CURVE:** the points are
  collected first and joined with `cubicTo` segments whose control points sit at
  the midpoint between steps, so every peak is rounded rather than a sawtooth
  corner, and the line, its depth pass and its played part all read at a heavier
  weight (0.11 of the strip, ink 0.72) — at 0.085/0.60 the wave was a hairline
  scribble on parchment (member: "the voice note graph in journal it looks so
  bad"). The strip stays backgroundless and shadowless (v404's call — a
  recording is part of the writing, not a card in it).
- **THE BOOK'S PROGRESS CARD: ONE BAR, TWO TILES.** The card drew a page gauge
  AND a chapter tick row — two bars answering "how far" in two units. It is
  `ReadingGauge` now: one bar whose fill is the page fraction, with the chapter
  openings as notches INSIDE the track (`chapterPages` is the 1-based page each
  chapter opens at; **v413 made the marks a TARGET, not a cap** —
  `GaugeMarkTarget` 16, with a STRIDE so a 300-chapter file draws an even
  rhythm instead of a solid block, a THINNING (1.6dp → 0.7dp) and a SOFTENING
  (mixed toward the fill as the count climbs). Before this it was one mark per
  chapter at a fixed 1.5dp with an all-or-nothing 80-mark cap, so a long book's
  track read as a picket fence and past the cap it drew no divisions at all
  (member: "the progress view division by chapter when chapters are too much it
  look ugly the progress breaks with no fluidity continuation"); the single
  `MAX_GAUGE_NOTCHES` constant is gone). **v417 — A DIVISION PER CHAPTER THE CARD
  IS STATING.** The notches were placed from the FILE's outline alone, so a book
  with no file (or an outline carrying names but no page ranges) drew none at
  all and a PDF kept the chapters it arrived with however many the member then
  set (member: "the chapter reading progress not showing accurate chapter counts
  when i add or remove chapter it doesnt update"). `gaugeMarks` has two answers
  now: PAGE-PLACED when the file's ranges agree with the count on screen, else
  EVEN divisions of that count — and the count passed in is `shownTotal`, the
  number the card is stating, so a held length stepper re-spaces the divisions
  as it runs. The swap is a 240ms CROSSFADE (`GaugeMarkSettleMs`, `Animatable` +
  `drawGaugeMarks`), so divisions settle into their new places instead of the bar
  flickering (member: "the animation is bad of it fix it"). The three
  identical stepper rows became two `ProgressTile`s — the things you change while
  reading, with the chapter tile NAMING the chapter — plus the book's length as a
  quiet footer rail (`BookLengthRow`: an uppercase "BOOK LENGTH" label against one
  compact stepper pill). **v412 then removed the two blocks that said the place a
  second and third time** — the chapter-name/"Page N of M" headline and the
  "Chapter X of Y · N pages" row under the bar — because the tiles already carry
  both counts (member: "there are a lot of duplicates … remove the top 2 … keep the
  last one with the +- button … make it one beautiful progress view"). The card is
  now: label + one action, the gauge, the two tiles, the footer rail. The finished
  state keeps its single "Every chapter closed" line under the full gauge. **The
  `TileStepButton` steppers repeat while held** — a tap moves one step, a hold past
  `TileStepHoldDelayMs` starts an ACCELERATING repeat (`TileStepRepeatStartMs` down
  to `TileStepRepeatMinMs`, shaving `TileStepRepeatAccelMs` a tick), one press away
  from 300 pages or 100 chapters — and every step (tap and tick alike) plays a
  `HapticFeedbackType.TextHandleMove` tick. Owned by one `detectTapGestures`
  (`onPress` + `tryAwaitRelease`), never beside a `Surface(onClick)`, so a hold can
  never also fire the tap that ended it — the same construction as `ReaderHoldButton`.
  **v418 — the gauge is draggable (see "THE READING GAUGE ANIMATES, AND IT IS A
  CONTROL" above) and the reader's chapter CONTENTS light the row the member is
  in.** `ReaderContentsSection` takes `atIndex` and tints the entry at or before
  the live place, so the contents read as an index to where you are rather than a
  flat list (member: "better chapter & outline handling").
- **v418 — the reader's flow switch settles, and its pages turn smoother.**
  Scrolling ↔ Pages used to swap in one frame; the reading surface now fades and
  lifts 16dp over 230ms when the flow (or the format, on first load) changes
  (`flowKey` + an `Animatable`, applied with `graphicsLayer` on ONE instance, so
  the two pagers are never composed at once). Both `HorizontalPager`s pass
  `beyondViewportPageCount = 1`, so a turn's neighbour is already laid out instead
  of painting from scratch mid-slide (member: "smoother page turns / scrolling").
- **v419 — THE PDF PAGE TURN HAS MOTION.** Each PDF page wears a `graphicsLayer`
  that reads its own distance from the settle point and applies a small slide
  (0.10 of a width), a 9° `rotationY` about its OUTER edge (`transformOrigin`),
  a 4.5% shrink, a light fade and a long `cameraDistance` (24× density — the
  default 8× curls too sharply). Deliberately small: a turn should read as paper
  being carried across, not a spinning card. The offset is read INSIDE the layer
  lambda (`currentPageOffsetFraction`), so a swipe invalidates the layer and not
  the composition — do not hoist it into composition, that recomposes every page
  on every frame.
  The press also drives a `0.9f` scale for touch feedback.
- **THE PANTONE ACCENT IS APP-WIDE.** `curioRoseInk()` and
  `settingsCardChipTint()` now answer the Pantone palette first, so the icon
  chips, plate tints and ink accents across the app come back in the member's
  three colours. Deliberately NOT recoloured: the gold/mint brand inks and the 36
  lane accents — a Pantone number is a page, a hero and an ink, not a replacement
  for every hue.
- **What's New prints the build's own version.** The release entry said
  `v1.1.1 · build 20260922` over a 1.3.0 build; a `WhatsNewRelease.versionName`
  must match `versionName` in `app/build.gradle.kts`.

## v424 — the reader's zones, the voice's ink, and a page that can leave

- **THE READER'S TAP ZONES ARE THE MEMBER'S, AND THEY ARE THE SIDES.** `tapZones`
  gates them; the places live in `ReaderLook` too (`zoneLeft` / `zoneRight` /
  `zoneTop` / `zoneBottom` as a `ReaderZoneAction`, and `zoneLeftDepth` and
  friends as a share of the surface). `readerZoneActionAt(at, size)` answers in
  that order: the SIDES first, standing the whole HEIGHT (a tap on the side of
  the SCREEN is what a reader reaches for — the v422 corners-only version left a
  mid-height edge falling through to the page), then the head and the foot
  between them. `ReaderZoneAction` is BACK / FORWARD / SCROLL_BACK /
  SCROLL_FORWARD / OFF: TURN and SCROLL are separate on purpose — a scrolling
  flow has both a column to move (`scrollPage`, a screenful) and pages to turn
  (`stepPage`), and which one an edge asks for is the member's choice. Defaults:
  left = BACK, right = FORWARD, top = SCROLL_BACK, bottom = FORWARD.
- **THE TAP IS SAID IN THE SURFACE'S OWN COORDINATES.** The zones belong to the
  screen, but a tap inside the scrolling PDF arrives in a SHEET's frame — so
  `PdfScrollReader` takes `viewport` + `surfaceOrigin` (captured on the reader's
  own Box) and each sheet translates its tap (`at + where - surfaceOrigin`); the
  column's own handler takes off the horizontal pan instead. **Never test a zone
  against a node's local point or size** — a magnified sheet is wider than the
  screen and its frame is the document's, not the phone's.
- **AND A HOLD ON THE SWITCH PLACES THEM.** `ReaderChrome`'s Crop button is a
  `clickable(onClick, onLongClick)`; the hold raises `ReaderLook.zonesEditing`,
  which composes `ReaderTapZoneEditor` over the page (the chrome stands down
  while it is up, and the overlay takes every tap so placing a line can never
  turn a page). The editor draws each edge where it is, gives every boundary a
  draggable `ReaderZoneHandle` (that IS the depth), lets the chosen edge wear the
  stronger wash, and keeps the four edges' actions, a depth slider and a Reset in
  its own panel. A zone is placed by looking at the page it governs — never move
  it into Settings.
- **THE SCROLLING PDF'S PINCH GROWS THE FILE ABOUT THE FINGERS.**
  `readerZoomDocument(zoom, drag, focus, down, across)` corrects by arithmetic
  that needs no measurement: a sheet's size is proportional to the zoom, so the
  point under the fingers sits at `focus * z` from the sheet's start and the
  document is scrolled by `focus * (z' - z)` in both directions, per pinch step.
  `readerDoubleTapDocument(at, down, across)` is the same rule at one point. The
  v422 layout-is-the-zoom model stands; the anchor is what makes it feel like a
  zoom rather than a corner grow.
- **A VOICE NOTE'S INK FOLLOWS THE VOICE (`voiceReach`).** Every wave used to
  read its samples nearly straight (a share of the band with an eighth-grade
  floor), so a quiet passage drew like a loud one; the reach is now the SQUARE of
  the level over `VOICE_FLOOR` (0.08). Deepen the curve rather than adding a
  second dial — the pulse, the bars, the bubble, the ribbon and the beads all
  read the same function.
- **THE PROGRESS BEAD SITS ON THE CURVE (`pulsePointAt`).** The pulse's segments
  put their control points at the midpoint between two vertices, which makes a
  segment's x LINEAR in its parameter and its y the two vertices blended by
  `(1-t)²(1+2t)` / `t²(3-2t)`; evaluating that is how the head rides the ink.
  Never snap the head to a vertex again — that is the "dot does not follow the
  wave" report.
- **THE SHAPE LOOKS (v424).** `RIBBON` is ONE round-capped STROKE that runs out
  along the voice and back down its own mirror (it was a closed, filled mirror
  shape, which read as a blob). `BEADS` are EVEN — one slot per bead, measured
  from its own centre, so nothing shifts when a passage gets loud, with the size
  carrying the sound on one scale over a drawn wire; the heard run cuts where the
  fraction says, which is what makes progress and beads agree.
- **`PILL` IS A LOOK, NOT A SHAPE EVERY LOOK NEEDS.** `PersonalVoiceStyle` has
  seven entries now, `drawsPulse` (HAND + PILL) picks the pulse drawing, and the
  play treatment is chosen per look (`VoicePillControl` for PILL). A new look is
  a new KEY plus a drawing plus a control — an existing key is never renamed.
  `VoiceDrawnControl` measures its mark (height from the room it has, a wider
  than tall triangle, a hair of optical lift, the stroke taken out of the size),
  which is what "the minimal play button isnt accurate" was.
- **A PAGE CAN COPY ITSELF, THROUGH ANDROID'S OWN BAR.** The dock's Copy tool
  only asks: `PersonalEditorState.requestPageTextMenu()` sets a flag, and
  `PersonalCanvas` answers it — `selectPage()`, then
  `pageToolbar.showMenu(rect = the canvas's own foot, …)` with Copy and Select
  all. The canvas publishes its bounds in root coordinates (`pageBounds`) because
  the platform's floating bar is anchored there, which is what puts the bar ABOVE
  the tools that opened it. Cut and Paste stay with the field's own bar — they
  need a caret, and a page-wide selection has none.
- **AND A PAGE CAN LEAVE (`features/personal/PersonalExport.kt`).** Always-on,
  chosen from the dock's export menu: **PDF** drawn with Android's own
  `PdfDocument` (A4 at 150dpi, the journal's paper/ink/accent, `StaticLayout`
  paragraphs split across pages by line, marker pens as leading-margin spans,
  photos with their own labels, voice notes with their wave and clock, a foot that
  names and numbers the page) so the words are REAL TEXT in the file; **Text**; and
  **Markdown** with the styling kept. Files land in `cacheDir/exports/` (the
  `personal_exports` path in `res/xml/file_paths.xml` is what makes them
  shareable) and leave through Android's sheet. The colours are resolved in the
  composition (`rememberPersonalExporter`) and the write happens off the UI
  thread. A new format is a new `PersonalExportFormat` entry plus its writer —
  never a second exporter.- **AND THE SHEET IS A FACSIMILE OF THE PAGE (v427).** Every size and every
  leading in `PersonalExport.kt` is read from the canvas' own type table —
  `BODY_VIEW_SIZE` / `BODY_VIEW_LINE` (16sp on 27sp), `TITLE_VIEW_SIZE` /
  `TITLE_VIEW_LINE`, `SMALL_VIEW_SIZE` / `SMALL_VIEW_LINE`, `QUOTE_VIEW_SIZE`,
  `VIEW_ROW_GAP` (8dp) — and so is every mark: a list line's box is the canvas'
  `PERSONAL_MARKER_SIZE` / `PERSONAL_MARKER_GAP`, its strokes are the canvas'
  ratios (a 0.085 outline, a 0.135 tick, a 0.17 dot), a checked box is the
  accent fill with a PAPER tick, a waiting one soft ink, and a quote's rule is
  `QUOTE_RULE_WIDTH` / `QUOTE_LEAD`. One number decides the scale,
  `PDF_UNITS_PER_SP` = the sheet's text column over `PDF_PAGE_MEASURE_DP` — the
  journal's own column (316dp: a 360dp phone less the read view's 22dp gutters),
  **one canvas dp to one sheet unit**, so a line breaks where the page breaks it
  and an entry takes as many sheets as its words need (the member's own decision:
  "make the PDF a facsimile of the journal column"). Every quoted run is set at
  `QUOTE_VIEW_SIZE` inside the block's own line (as the page sets it, so a quoted
  phrase keeps its prose and shrinks itself), and a print's LABEL is the print's
  own: `PdfFonts` resolves each `PersonalCaptionFace` to its bundled file (the
  sheet's four run faces, then `lora` / `patrick_hand_regular` / `playfair_display`
  / `space_mono` / `bebas_neue` / `space_grotesk`), the size is
  `personalCaptionSizeSp(CAPTION_VIEW_SIZE, …)` so the member's
  Small / Standard / Large reaches paper, and the stamp rides under the words —
  both lines cut to the PRINT's own measure (one line, then an ellipsis), which is
  how the page cuts them. The sheet keeps only what is about PAPER: its A4 edges,
  the margin, the foot that names and numbers it, and how tall a picture may be.
  **Never add a size, a line height, a mark or a caption face to that file that
  the canvas does not have**: the export had its own table (a heading as body ×
  1.45, one 1.42 leading for every size, every caption in Lora, a box sized from
  the line) and that is precisely what drifted from the page it was drawing.
- **…AND THE TWO DRAWINGS THE PAGE OWNS ARE DRAWN BY THE PAGE (v427).** A print
  and a voice note are the two blocks the sheet used to DRAW ITS OWN VERSION of,
  and each had drifted where the member could see it. Both are the page's now:
  - **A PRINT IS A FRAME, NOT A FIT.** On the page a print is `size.fraction` of
    the measure WIDE and `personalPrintHeight(size)` TALL, and the photograph is
    **CROPPED** into it (`ContentScale.Crop`, `PersonalPagePhoto`). The sheet
    decoded the whole file and FITTED it inside the box, so a print the member
    had set to Small or to a portrait frame left the journal as a small complete
    picture at an aspect and a height nothing on the page had given it (their
    "in pdf export the photos are not visible as they are in the preview of
    journal"). `drawExportPhoto` asks for the size's own frame (plus
    `PDF_PRINT_PAD`, the canvas' 7dp), draws the picture with `drawBitmap(src,
    dst)` over a centre-cropped `Rect` that covers the box, and puts the label
    inside that frame — and `decodeExportBitmap(context, uri, w, h)` samples
    against BOTH targets, because a cropped picture must be sharp along its
    height too. **It also returns the photograph UPRIGHT** (`exportUpright`,
    platform `android.media.ExifInterface`, the same treatment
    `AdaptiveImageGallery` uses): the page paints through Coil, which reads EXIF,
    while a bare `BitmapFactory` decode does not — so a sideways shot printed
    lying down.
  - **A VOICE NOTE IS THE PAGE'S OWN STROKE.** `drawVoicePulse` and
    `drawVoiceWave` in `PersonalVoice.kt` are `internal` for exactly this: the
    sheet draws the note through a `CanvasDrawScope` sized at `Density(
    PDF_UNITS_PER_SP)` (so the page's own dp geometry lands at the sheet's own
    scale), translated to the strip's place, with `progress = 0f`. The control
    follows the look (`style.drawsPulse` / `MINIMAL` = the drawn mark in ink, no
    disc; the rest wear the accent fill with the paper mark) and the clock is
    `fonts.display` for the drawn looks at the page's own alpha. **Never
    re-invent the wave, the bars or the pulse on the sheet** — that lookalike (a
    filled pill and a bar chart) is what the member reported as "the waves are
    also not visible as it is in journal eye view".

  - **A ROW OF PRINTS IS THE PAGE'S OWN ROW (v427, second pass).** `exportPrintPlan`
    runs the page's own grouping pass over the document — a run of prints up to
    `PRINT_ROW_LIMIT`, stepping over blank rows that are nobody's place, and a
    lone SMALL print taking the line under it as its companion — and
    `drawExportPrintRow` builds the same SHAPES `PersonalPrintArrangement`
    builds: a pair side by side, a three as the upright frame (whichever member's
    size asked to stand, at least as wide as the widest print beside it) with the
    other two stacked in a column that keeps each print's OWN share, and a four as
    two lines of two (each line weighing itself, as the page's Column of Rows
    does). The gap is the canvas' `PRINT_ROW_GAP`, the beside share is the
    canvas' `printBesideShare`, a cell's height is `exportCellHeight` — the frame's
    own paper (`PDF_PRINT_FRAME`) plus its picture plus its label's room, the SAME
    reading the lone-print path uses, so a cell and a print can never measure
    differently — and the row is drawn as ONE block: it
    either fits the sheet or it starts the next one. `drawExportLines` places a
    line's cells where the row decided and moves the sheet once, by the line's own
    height (`PdfRun.setCursor` exists for that; `advance` can only add) — a
    per-print cursor move cannot put the second half of a pair on the first's line.
    The beside pair draws its line with `exportLayout` at the column's own width and
    clips it to the row, so the words wrap where the page wraps them. **The sheet
    must keep asking the canvas for every one of these numbers** (`PRINT_ROW_GAP`,
    `PRINT_ROW_LIMIT`, `printBesideShare`, `personalPrintHeight`, `PersonalPhotoSize`)
    — they are `internal` for exactly this.

  - **AND THE PAPER THE FRAME ADDS IS THE PAGE'S OWN (v427, third pass).** A print
    is a picture with a band under it, and the PAGE draws every pad that holds
    them: `renderPrint` is a `padding(start = 7.dp, end = 7.dp, top = 7.dp,
    bottom = 2.dp)` box around a caption column of `padding(top = 5.dp, bottom =
    5.dp)` — and the band is **ALWAYS there**, empty or not (v400: the blank band
    carries a non-breaking space rather than nothing, so a print nobody has
    captioned is still the print the member saw). The sheet kept ONE of those five
    bands, returned NO band at all for an uncaptioned print, and drew the picture
    flush at its cell's top — so a print stood 14dp short on paper and an
    uncaptioned one a quarter short, and every ROW (whose line is as tall as its
    tallest cell) stood short with it. `PDF_PRINT_FRAME` = the pads, the picture is
    inset by `PDF_PRINT_PAD`, and `exportCaptionRoom` always reserves the label's
    line. **Never make a print's height depend on whether it has been captioned** —
    that is the page's rule, and paper has to obey it or a row of prints measures
    differently on the two sides.

  Still the sheet's own, and known: a BUBBLE voice look's tinted bubble, and the
  voice strip's tap-to-seek (nothing on paper can be scrubbed — see the member's own
  question about an interactive PDF). Those are the next parity pass, not a licence
  to draw a second version of anything.
- **A WORD IN THE READING VIEW OWNS ITS COLOUR (v427).** The eye view's writing
  came out black in every theme, and only where the page was dark, because of one
  thing: `PersonalDocView`'s `Text` set a `TextStyle` with NO colour, and
  `personalAnnotated` only coloured the FLAGGED runs — so a plain word carried no
  colour at all and fell through to `LocalContentColor`, which outside a
  `Surface` is **Compose's own default, `Color.Black`**. The writing page passed
  a colour of its own (`bodyStyle`), which is why the fault only ever showed on
  the reading side (the member: "still in journal eye view the text writing have
  dark black texts"). Two rules hold it shut: `personalAnnotated` lays a
  `SpanStyle(color = ink)` over the WHOLE string before the flags, and every
  read-view `TextStyle` sets `color = ink` as well. **A read-only surface must
  never rely on an inherited ink** — `LocalContentColor` is black unless a
  `Surface` says otherwise.
- **THE PAGE BAR'S REACH HAS TWO AXES AND NO MODE (v427 → v428).** v427 gave
  the bar ROWS (`pageRange`, `pageRowPicked`) and a LETTER MODE
  (`pageLetterMode`, `pageCharRange`, one row only — a character range across
  rows IS those rows) with a switch to choose between them. The member: *"proper
  arrow up down left right arrow and no more letter row option but the arrows do
  the work ... dont let user select things starting from bottom"* (clarified:
  *"let user select things from bottom proper tool of how it should behave"*).
  So the switch is GONE and the two units are the two AXES: ↑ ↓ rows, ← →
  letters, letters switching themselves on at the first ← or →.
  `PersonalEditorState.nudgePageRows(up)` builds the reach FROM THE MEMBER'S OWN
  ROW (the caret's, else the page's foot), the arrow pressed FIRST setting which
  way it grows (`pageGrowsUp`), the other giving a row back and walking one row
  at a single row; `nudgePageLetters(more)` opens a one-character window at the
  caret (else the row's end for an upward reach, its start for a downward one).
  The ANCHOR row — where the letter window lives — is `pageAnchorIndex()`: the
  reach's foot when it grew upward, its head when it grew downward. All rows
  draws itself as a bottom-up reach, so letters after it come from the last row.
  `pageSelectionText`, `cutPageSelection` / `cutPageLetters` and
  `pastePageText` / `pastePageLetters` branch on `pageLettersPicked` (the window
  holds more than nothing). **Every arrow DIMS when its axis has nowhere to go**
  (`canNudgePageRows` / `canNudgePageLetters`) — a control that answers a press
  with nothing teaches a member to stop pressing it. **The reach is DRAWN, or the
  bar is lying**: a picked row wears the page's selection wash, and the picked
  letters are a `SpanStyle(background = selectionWash)` span added LAST in
  `personalAnnotated` (the member's "the select tools doesnt highlight whats
  selecting").
- **AIR BETWEEN TWO PRINTS IS AIR, EVEN WITH THE CARET IN IT (v428).** A print
  row may step over the blank lines between its prints (`printRowGapSteppable`),
  and the editor used to REFUSE to step over the one the caret was resting in —
  so two pictures with nothing but a blank line between them were one row in the
  EYE view and two lone prints in the PEN view, which is where the caret lands by
  itself after almost any picture is added (member: *"sometimes they unstack"*).
  Since v428 the caret does not break a row: the grouping pass claims the gap
  either way, and the ONE gap that holds the caret (or a selection) is left
  VISIBLE and drawn in place under the row instead of being hidden with the
  rest. **The two passes must agree about which pictures are one row** — that is
  the rule the change protects, and it is why the fix is in the grouping pass and
  not in the caret.
- **A PAGE CARRIES ITS OWN COLOUR, AND 0 MEANS THE THEME (v428).**
  `PersonalNoteEntity.accentArgb` (`personal_notes.accentArgb`, migration
  21→22, `INTEGER NOT NULL DEFAULT 0`) is a JOURNAL's own colour. **0 is not
  black and not a palette index — it means "follow the app's theme"**, which is
  the honest backfill for every row that already exists and the reason no page
  changed colour on the update (the member: *"by default they follow the theme
  only the changed colour stays as it looks"*). Read it through
  `journalDoorAccent(argb)` (theme accent INK, or the stored ARGB) and the picker
  is `JournalAccentSheet` — the app's own hues (`journalHueChoices`, each read
  through `CurioNamedTheme.heroFor` so a swatch IS the colour the app paints
  with) plus an HSV wheel and a brightness slider; it applies LIVE and the page's
  own debounced writer persists it. WHAT WEARS IT — the doors, never the page: the
  journal list's spine, Home's journal chips, and the palette button in the
  page's tools. Set it only where it can be KEPT: `onJournalAccent` is null on
  the topic-note, chapter-review and book-review pages, so those never grow a
  door that could not store the answer, and the colour rides
  `PersonalPageMeta.accentArgb` because the writer rebuilds the whole entity from
  the meta — **a field left out there is a field ERASED on the next keystroke**.


### v428 — the sketchbook signature, one align tool, and the post's own card editor

- **THE SIGNATURE SHARE CARD IS A SKETCHBOOK PAGE, ONE PER CATEGORY (`ui/components/SignatureSketchbook.kt`, new).** The member: *"make the design like collections style like hand drawn doodles"* + *"do one unique per category for signature cards"*. The thirty-eight printed scenes `signatureDesign` used to return (hairline frames, quiet type, a faint watermark glyph) are GONE — `TopicShareCard.kt`'s `signatureDesign(categoryName, family)` is a four-line delegator to `sketchbookDesign(...)`, and the drawings live in the new file: warm paper, a **wobbly pen outline drawn twice**, pencil hatching for shading, **washi tape** over a corner, a margin star, an arrow pointing at the drawing, and **ONE motif per lane** ([SketchMotif] — 38 drawings, none shared). Three rules keep it honest: (1) **the wobble is deterministic** — `jitter(seed, index)` is a 32-bit hash, not a `Random`, so the page the editor shows is the page the PNG contains, frame for frame; (2) **hatching is the fill** — never a rectangle of flat colour where a sketcher would shade; (3) **the hand is the type** — title and footer are `PatrickHandFontFamily`, the fact stays in `GeomFontFamily` (a page you cannot read is not a nice page). The page is keyed on the category NAME (`sketchPageFor`) with the FAMILY as the second key (`sketchFamilyPage` — a renamed or brand-new lane lands on its family's page, not the grey fallback), and `SignatureDesign` / `SignatureLayout` are `internal` now so the sketchbook can name them. `signatureHairlineFrame` was deleted with its layouts; the classic scenes (`signatureDesignClassic`) are untouched.
- **THE CLASSIC SIGNATURE EXISTS ONLY FOR GAMES AND FILMS.** The member: *"remove the classic or signature for all except games and films"*. `signatureClassicAvailable(categoryName)` (in `TopicShareCard.kt`) is the ONE gate, read in three places that must never disagree: what `TopicShareCard` RENDERS (so a card saved by an older build cannot paint a lane that no longer offers the classic), whether the editor draws its Classic toggle at all, and `ShareHubScreen`'s grid — which is now built per picked category (`hubDesigns(classicSignature = …)`, replacing the fixed `HubDesigns` list, so the grid never offers a design the editor would refuse) and finds its "Per-category signature designs" heading from the first cell that carries a `categoryOverrideId` instead of a hard-coded index. A sheet opened from a stale classic cell falls back to the lane's own design rather than opening on a variant with no way back. Strictly GAMES and FILMS: Animated Films keeps its own doodle page and no classic.
- **THE JOURNAL'S FOUR ALIGNMENT BUTTONS ARE ONE TOOL.** *"collapse the alignments into one option they show in that tool row"* — `PersonalToolDock` now draws one alignment button wearing the FOCUSED LINE's own alignment (`PersonalAlign.toAlignKind()`, the one mapping to the glyph's `AlignKind`), lit for anything but plain left, with the four choices in the menu behind it (the bullet tool's tap-then-menu habit, `MarkerMenuLabel` rows, a check on the current one).
- **A SCROLLED TOOL BAR REMEMBERS WHERE IT WAS LEFT.** The dock's tool row, the text bar's two arrow rows and the caption row all used `rememberScrollState()` INSIDE the row — so a row wider than the phone snapped back to its first tool the moment the dock swapped bars (or the page was reopened) and the member had to find the same tool twice. Each bar now owns a `rememberSaveable(saver = ScrollState.Saver)` state, and the dock's is hoisted ABOVE its own bar swap (the text bar / caption tools branch), which is what makes the place survive the swap and a rotation alike.
- **THE POST'S CARD IS EDITED BY THE REVEAL'S OWN SHEET.** The member: *"for the post share card show the topic reveal share card bottom sheet editor and the editing layout ratio stays intact in each screen of post"*, and asked where it opens: *the post card screen*. `LivePostPreview` now carries the card's own strip — the shape switch ("3:4" ↔ "9:16") and one **Edit card** door — and the composer opens `TopicShareSheet` (the reveal's editor) on the post's topic, family, style and RATIO. The sheet gained `initialAspect` + `onAspectChanged`: the ratio is handed in, reported back on every change (`LaunchedEffect(aspect)` — the toolbar toggle or a restored edit alike), and written to the composer's own `aspect`, which is what the draft stores — so the composer, the wall and the card screen show ONE shape. The sheet is composed in the composer's own column, never inside a lazy item: a sheet opened from an item is dismissed the moment that item scrolls away.

### v429 — every door that answers: OMDb, Wikipedia, Comic Vine, and no door that stalls

The member, after a round of dead providers: *"these api keys are not working, library thing, it gives http 403 text/html error, tmdb no answer 24060 ms, jokan my animelist http 504 … also add omdb key for fetching if any doesnt fetch … use all the avalabel api also i added comicvine api too mybe use that for incursion with more fallbacks"*. Four rules and three new doors came out of it.

- **A DOOR IN A CHAIN GETS A SHORT BUDGET, AND EVERY CALLER NAMES ONE.** The 24-second stall was arithmetic, not a bad key: TMDB's `facts()` is three reads and `showEpisodes()` adds a season each on top of that, at 8s connect + 8s read EACH. `TmdbFetch` now runs 3.5s/5s with the whole `facts()` read capped at `FACTS_BUDGET_MS`; `SeriesEpisodeFetcher` and `IncursionSources` cap every door they call at their own budget; the Jikan doors (`AnimePosterFetch`, `AnimeEpisodeFetcher`, `MangaFetch`) are 4s/5s/7s-call where they were 8s with a retry. **A fallback that answers after the member has left the screen is not a fallback.**
- **DOORS THAT RACE.** `SeriesEpisodeFetcher.fetchForAny` asks TMDB and TVMaze **at once** and prefers TMDB when it has something, so the keyless door that actually answers is never waiting behind a dead keyed one — the reason a series' episode guide "doesn't load" in Incursion. It also takes `season: Int?`, and `TmdbFetch.showEpisodes(title, season)` reads ONE season when the caller knows which one (an Incursion row says so), cached separately (`seasonCache`, key `"loki#S2"`) so a hinted read can never truncate the whole-show answer.
- **OMDb IS THE SECOND KEYED DOOR FOR A FILM OR A SHOW (`features/reveal/OmdbFetch.kt`, new; key `OMDB_API_KEY`).** One request, answers by NAME, states a plot, a real poster, the IMDb rating, the runtime and the genres. It sits after TMDB and before the keyless pair for artwork, and behind the Incursion sheet's own description chain. Keyless builds ask nothing.
- **WIKIPEDIA IS ONE DOOR, SHARED (`features/reveal/WikipediaSummary.kt`, new).** A page's prose and its lead image, with the search-then-best-hit matching and the bracket rule (`Kind.FILM` must not answer with the novel, `Kind.BOOK` must not answer with the film) in ONE place — Incursion's row enrichment and the shelf's book enrichment both go through it. A disambiguation page is refused, and every answer and miss is memoised.
- **INCURSION'S OWN CHAIN (`features/incursion/IncursionSources.kt`, new).** `record(entry)` = the row's description and facts: stage one races TMDB and OMDb; stage two (only if stage one was empty) races Wikipedia and Comic Vine. `artwork(entry)` = OMDb's poster, Wikipedia's lead image and Comic Vine's cover, raced, reached only after a TMDB id and the whole keyless chain have failed a row. Both are gated on `AppPreferences.coverFetchEnabledState` (the canonical consent gate), memoised per storage key with misses included, and `ComicVineFetch.movie(query)` is what makes the comics key useful for a film line.
- **LIBRARYTHING'S HOST REFUSES APPS (MEASURED), SO OPEN LIBRARY SERVES THE ISBN COVER.** `covers.librarything.com` sits behind a Cloudflare JS challenge: every native request is `403 text/html` with "Just a moment…", whatever the User-Agent, and no key changes that. `BookCoverFetch.libraryThingCover` keeps its ISBN resolution (a keyless Google Books read) and its key gate, and returns `covers.openlibrary.org/b/isbn/{isbn}-L.jpg`; the hub's row is labelled `ISBN covers` / "LibraryThing's host blocks apps · served by Open Library". The source lab still probes LibraryThing's own URL, honestly, so a 403 can be seen rather than described.
- **GOOGLE BOOKS ANSWERS EVERY QUESTION IT CAN, AND WIKIPEDIA IS THE LAST NET FOR A BOOK.** `googleBooksVolume` reads one volume for its table of contents, its page count and its blurb (the pass asks each only while it is still missing); after Standard Ebooks, a still-blank about-text falls to `WikipediaSummary.extract(..., Kind.BOOK)`. Keyless Google Books is still not asked (its anonymous quota answers `429`).
- **AN EYE, NOT A PLAY TRIANGLE (Incursion's watch button).** The mark is drawn (a font glyph went missing once — `visibility` is not in the bundled subset), and it is an eye: two hairline arcs with a pupil when unwatched, the same almond filled in the page's accent with the pupil punched out in the button's own fill once watched. A play mark read as an instruction to PLAY a title the button's job is to say has been SEEN.
- **THE DRAWER'S CONSTELLATION LINES LEAN (v429).** `DrawerLaneStarMap`'s joins are shallow quadratic arcs — the control point is pushed off the midpoint's perpendicular by `min(span * 0.10, 6dp)`, its side fixed by the pair's own indices so the drawing is identical on every recomposition and through the whole light-up.
- **THE SCROLLING PDF'S ZOOM IS ANCHORED IN DOCUMENT TERMS.** v424 held the fingers' place with `focus` alone, which is the whole anchor only for the FIRST sheet: what sits between a finger and the top of the file is every sheet before it, and on page 30 the correction was short by the height of the 29 above it (member: *"its zooming at the top"*). `readerZoomDocument` and `readerDoubleTapDocument` now carry `sheetsAbove` (= `page * <one sheet's magnified height>`, in pixels), and the anchor is `(sheetsAbove + focus.y) * (ratio - 1)`; padding and the gaps between sheets drop out by construction because they do not scale with the zoom.

### v429b — the art lane's own fail-safes (the same rules, applied to what is painted)

Asked for in the same breath as the API fixes — *"also fetching artworks, painting fetching proper fail safe too tell anything if missed anything"* — and the art lane had the same two holes the screen doors had:

- **A MISS IS REMEMBERED, BUT ONLY A REAL ONE (`ArtworkFetch.misses`).** A work neither museum holds was re-asked of the Met, Cleveland and Wikipedia on EVERY open — the sheet re-ran the whole chain to arrive at the same nothing. Only a **confirmed** miss is kept (all three doors reached their catalogue and each said "not here"); if any door FAILED — a timeout, a dead line — it is not remembered and the next open tries again, which is the app's own rule from the shelf (*"a lookup that failed is no longer remembered as done"*). The set is per-process; a restart asks once more.
- **EVERY DOOR IN THE LANE IS ON A SHORT BUDGET.** `ArtworkFetch.getJson`, `MuseumFetch.getJson` and `AuthorWorksFetch.httpGet` were all 8s+8s; they are 4s/5s now, because the Met, Cleveland and Wikipedia are asked AT ONCE for a work and no one of them may hold the sheet.
- **THE RECORD READS INSIDE A DOOR RUN TOGETHER TOO.** `metObject` opened up to three object records in a row and `worksBy` up to eight, all inside a door that is itself one of three running in parallel — 24 and 64 seconds of worst-case latency for a single source. Both read their ids with `async` now (order preserved: the same record still wins), so the door costs one round trip.
- **THE BARE TITLE IS NOT ALWAYS THE ARTICLE (`ArtworkFetch.wikiSummary`).** The read was one direct slug, which answers only when the work's name IS the article's name; a great many paintings are filed with a disambiguator ("Nighthawks (painting)", "Guernica (Picasso)") and answered nothing, so the sheet fell back to whichever museum held one. It now asks the shared [WikipediaSummary] door with `Kind.ART` when the direct read comes up empty.
- **AND THE LEAD IMAGE IS THE THUMBNAIL, DELIBERATELY.** `WikipediaSummary.leadImage` prefers the REST summary's ~320px thumbnail over the full original: every caller draws a row or a card, so the original is megabytes for detail nothing displays.

### v430 — the page paints its own paper, and the Incursion list can be ordered

- **A JOURNAL'S PAPER CAN TAKE ITS OWN COLOUR (`PersonalNoteEntity.pagePainted`, `personal_notes.pagePainted`, migration 22→23).** v428 gave a page its own colour, but only its DOORS wore it — the list's spine, Home's chips, the palette button — while the page stayed the theme's parchment. The member: *"the journal page color also needs to chnage with the color chnage, add an option for that to turn on"*, and, asked where the option belongs, *"in the color sheet, and its per journal stored with the page"*. So it is a per-page flag, stored beside the colour it is about, default `0` (false) — the honest backfill: no page was painting its paper before the column existed, so none starts to. The switch lives in `JournalAccentSheet` ("Paint the page too"), draws only when the page HAS a colour of its own AND a setter to keep the answer (`onJournalPagePainted`), and rides `PersonalPageMeta.pagePainted` through the page's debounced writer so flipping it saves on the same clock a typed word does. **Read through `JournalPagePaint` / `LocalJournalPagePaint`**, provided once per page by `PersonalWritingPage` around all three surfaces that paint paper (the reading side, the writing canvas, the dock — and the dock is where the EXPORT is resolved, so a file a page leaves as wears the same paper it wears).
- **A PAINTED PAGE ANSWERS FOR ITS OWN READABILITY.** `journalPaper()` takes the page's colour at a REAL tint (0.20 light / 0.28 dark) instead of the theme's whisper (0.05 / 0.10), and `journalInk()` stops being `MaterialTheme.colorScheme.onSurface`: the theme's ink is measured against the theme's surfaces, not against a colour off a wheel, so when the paper and the theme's ink are BOTH light (or both dark) the page's own readable ink stands in. The one thing a journal must never do is become unreadable because it was made pretty. A page with no colour of its own (`JOURNAL_ACCENT_THEME`, or `painted = false`) keeps the theme's parchment and the theme's ink exactly as before.
- **AN INCURSION POSTER IS THE FIRST DOOR TO ANSWER, NOT THE FIRST ONE ASKED (`IncursionPoster.kt`).** The member, after three passes of posters: *"in incursion page the series etc movie poster still doesnt load fast, why not fetch uses all services and whover gives the first success wins … movies dont even load and it should load when im only in the list without opening"*. The chain was SEQUENTIAL (TMDB by id → the row's own door → the last net), so a film's keyless leg ran to completion before TVMaze or OMDb were even asked — and the film's keyless leg was the slow one. Now every door starts TOGETHER and `firstSuccess` takes the **first answer**, cancelling the rest: TMDB by id (both kinds), the row's own door ([SeriesPosterFetch] for a show, [FilmPosterFetch] for a film), the row's kind's OTHER door (upstream files the same title as both), and the last net ([IncursionSources.artwork]). It is deliberately NOT `IncursionSources.race`, which awaits every door in PREFERENCE order — right for a description, wrong for a plate. The whole race is bounded (`POSTER_BUDGET_MS` = 12s, each door `DOOR_BUDGET_MS` = 7s), and **the page WARMS its first 24 rows** (`WARM_ROWS`, 4 at a time) as soon as the open list settles, so a row scrolled to already has its URL — the other half of *"it should load when im only in the list without opening"*.
- **A FILM'S KEYLESS POSTER DOOR IS TWO REQUESTS, NOT SEVEN (`FilmPosterFetch.wikipediaPoster`).** The Wikipedia leg tried four article names, then a search, then two more reads — up to seven round trips at 8s + 8s each, run one after another inside a door that was itself one leg of a chain. That arithmetic is why *"movies doesnt even fetch ever only series does"* while series (TVMaze, one short keyless request) answered fine. It now asks the shared [WikipediaSummary] door FIRST — one search, one summary, memoised for the whole app, with the film/doc bracket rule — and keeps the name guesses only as the backup for what that door refuses. Every keyless read in the file dropped from 8s/8s to 4s/5s.
- **A ROW THAT IS ONE SEASON SHOWS THAT SEASON (the Incursion sheet's guide).** *"also its not accurately by season"*: the row reads "Loki S2", but the keyless door answers with the WHOLE show, so the sheet opened on season one's episodes under a title that says S2. When `entry.season` is set and the guide carries that season, the list IS that season (and the "SEASON n" headings disappear with it, because a single-season list needs none). A guide that does not carry the season falls back to the whole thing rather than showing nothing.
- **THE SCROLLING PDF'S PINCH IS THE WHOLE SCREEN, AND ITS ANCHOR IS THE LIST'S OWN LAYOUT (`BookReaderScreen`).** The member: *"why the zoom is based on pages it should be for the hole screen i mean the pin to zoom gesture is working when its inside one page, and the glitch is it weirdly scrolls"*. Two faults, one gesture: it was armed per SHEET (so it did nothing in the 16dp air between two of them), and its anchor was a value read in the COMPOSITION that armed it — `sheetsAbove = page · height · zoom` — which `pointerInput(key)` never refreshes, so the entire pinch ran with the first frame's number while the file kept growing and the correction fell further behind with every event. The handler now lives on the COLUMN (every point of the screen is the document's) and the anchor is `documentOffsetAt(listState, viewportY)` — read from the list's own `layoutInfo` at the moment of each event, so it needs no assumption that pages share a shape, no page number, and no stale capture. The constant parts of the column (its padding, the gaps between sheets) cancel in the difference by construction, which is exactly what a page-shaped calculation kept getting wrong. The double tap follows the same anchor, and both taps in the air and taps on a sheet reach it (the sheet's own handler translates its point into the surface's space).
- **INCURSION CAN BE ORDERED, AND A NON-VIEWING ORDER DROPS ITS PHASE HEADERS.** The member: *"for incursion ui, make it more better, add sorting etc."* `IncursionSort` (`ORDER` / `TITLE` / `YEAR` / `RUNTIME`, with a per-order `flipLabel` — A–Z ↔ Z–A, newest ↔ oldest, longest ↔ shortest, and none at all for the viewing order) is applied **where the rows are gathered** (`sections`), so the list, the grid, the head's progress bar and "Next up" all see the same rows in the same order; a sorted page whose progress counted a differently-ordered list is how two numbers about one shelf start disagreeing. Fact orders PARTITION first, so a row with no year yet (an announced title) and a row with no stated runtime sink to the END instead of pretending to be year 0. `flat = true` then tells the list and the grid to drop the PHASE headers — "Phase 2" is a claim about the viewing order, and A–Z rows filed under it would be a list sorted by title and captioned as though it were not (the search's LINE bands stay: they say where a row came from, which is true in any order). The control is a pill in the head whose glyph is **`drag_handle`, a MEASURED choice**: the bundled Material Symbols subset has no `sort`, `swap_vert`, `sort_by_alpha`, `low_priority` or `filter_list` in its name table (see [safeGlyphName]'s byte-probe note), so a `sort` pill would have drawn the word "sort".

### v431 — the reader's own pills, and a zoom that holds its place

- **THE SCROLLING PDF'S ZOOM ANCHOR WAS MEASURED FROM THE TOP OF THE FILE (`BookReaderScreen.documentOffsetAt`).** The member: *"in vertical pages the pdf zoom is really buggy its scrolling when i try to zoom and it scroll so fast when i try zoom that like 10 pages it scrolls by"*. v430 answered `index · sheetHeight + into` — the distance from the top of the FILE — and compensated with `that · (ratio - 1)`. A lazy column PRESERVES `(firstVisibleItemIndex, scrollOffset)` across a relayout, and every sheet grows by `ratio`, so the relayout ALREADY carries the viewport forward by `index · sheetHeight · (ratio - 1)`: the compensation landed on top of a move that had happened, doubling the jump, and it was proportional to how deep into the book the member was — ten pages, exactly as reported. `documentOffsetAt` now answers the finger's distance below the FIRST VISIBLE item's top edge (the one landmark a relayout keeps still), which is the whole of what the difference needs; padding and the 16dp gaps between sheets cancel in it by construction, and the sheet's INDEX must never appear in it. **The rule to keep: an anchor for a zoom that re-lays the content out is measured from what the relayout HOLDS, never from the top of the content.**
- **THE READER'S CHROME IS TWO FLOATING PILLS (`ReaderTopPill` / `ReaderBottomPill`).** The opaque full-width head and foot bands are gone. The head is the way out, the book's name and the search door at its end; the foot is five buttons — Appearance, Contents, Pages (the count, in the middle), Bookmarks, ⋯ — each a glyph or an icon-only button, with the reader's own type on the title.
- **APPEARANCE IS ONE SHEET (`ReaderAppearanceSheet`).** A−/A+ discs either side of the type slider, the three typefaces in a row ([ReaderTypeFace] — the app's own Lora, Fraunces and writing families, applied to the WHOLE page so a heading stays a heading by its size and weight), five page colours in a row, a `+` tile that unfolds the tuned papers, and the two switches (Auto-rotate, Horizontal pages). **`ReaderSkin` gained GRAY plus the extras** (MINT, ROSE, AMBER, SLATE); `readerPalette` and `readerPdfFilter` must be extended together for any new skin, and the extras use a real paper map (`out = a·in + ink`, `a = (paper − ink)/255`) so a PDF page keeps its own contrast on a new sheet.
- **SHEETS ARE HALF THE SCREEN AND SMOOTH (`ReaderSheetFrame`).** Hand-built rather than Material 3's `ModalBottomSheet` on purpose: the reader runs with the system bars hidden, and a dialog-backed sheet brings its own window back over the page. The height is FIXED at half the screen (a list scrolls inside it), it rises on a tween with the scrim, and the handle + title are the DRAG TARGET so the body is left to the content's own scroll. `appear` and `drag` are read in a deferred `offset`/`graphicsLayer` lambda, so a drag is a layout pass and never a recomposition of the sheet.
- **ONE DOOR PER QUESTION.** The v395 merged places sheet is now [ReaderPlacesMode] — ONE layout with a mode — so Contents, Bookmarks, Notes and Highlights cannot drift apart, and the contents rows still see every mark (which is how a chapter knows it is already bookmarked; every chapter row keeps its own bookmark AND its progress). `ReaderSheet.INK/PLACES/SEARCH` are gone with their sheets; `ReaderSearchSheet` is deleted.
- **THE PAGES BUTTON IS BOTH THINGS.** A tap opens [ReaderScrubberSheet] (a slider across the whole book, with the old hold-to-turn [ReaderHoldButton] arrows either side); a HOLD pins [ReaderPinnedPage] — a small counter in the corner, drawn OUTSIDE the chrome so it survives the chrome leaving, and a tap on it takes the pin out. `ReaderScrubber` (was `ReaderPageBar`) is built ONCE in the reader body — pages for a PDF in either flow, the pager's own pages for a reflowed book read as pages, and its SECTIONS for one read as a scroll.
- **THE SEARCH IS THE TOP-RIGHT DOOR, AND IT IS A BAR.** `ReaderSearchBar` sits in the head's row while a search is up (the head pill stands down), with the field, the find count, next/previous and the cross; the query is swept on a 320ms pause (a PDF's words cost a parse per PAGE), the first find steps onto its place as soon as it exists, and the arrows move through the finds through the same `pendingPage`/`pendingBlock` asks every other jump uses. **On a PDF every occurrence on every visible page is washed** (`PdfPageTextLayer` takes `query` + `queryCurrent`; the current find is washed harder) — the member asked for the results highlighted ON the pdf. Search is REMOVED from the ⋯ menu ("remove the search as search already got the optin in top right").
- **THE ⋯ MENU IS ROUNDED PILLS.** Notes, Highlights, Dictionary, Share a passage, Tap zones (the switch moved off the foot, with a long-press for its placement editor) and Reading settings. Sharing (`shareReaderPlace`) sends the passage if words are selected, the book's name and the place — never a link to a file on the phone.
- **A SINGLE WORD IS A LOOKUP (`ReaderSelectionBar`).** One word and the bar shows ONLY the dictionary icon; a longer sweep gets the wide floating capsule toolbar (one row, full radius, one lift, icons only, `animateContentSize`) the member asked for. The two-row panel with the quoted passage is gone.
- **THE DICTIONARY IS IN THE APP (`ReaderDictionary.kt`, new).** Keyless Wiktionary `page/definition/<word>` — one request, memoised answers AND misses, a short budget (4s/5s), `en` only, markup stripped, and only word-shaped terms asked for (letters, hyphen, apostrophe). `null` means "could not be reached"; an empty list means "no such word" and the sheet draws them differently.
- **READING SETTINGS ARE A PAGE OF THEIR OWN (`ReaderSettingsScreen.kt`, new; `CurioRoutes.READER_SETTINGS`).** Full screen in the READER's paper and type, never the settings family ("dont use settings style use the reader style ui for it"): the paper row, the type row, the THREE-WAY orientation (which a switch cannot say), Horizontal pages, and the tap zones with their placement door. It opens OVER the book from the ⋯ menu, and the same composable is a destination of its own behind `ReaderSettingsRoute` — wired on the Dev page for now ("wire it in dev exp for now") and visible from the reader either way. **If a second surface ever needs it, keep the route a door to this one composable.**

### v432 — a double tap lands where it was tapped, and a pinch is never a tap

- **A ZOOM THAT IS ONE BIG STEP CANNOT BE CORRECTED WITH A PIXEL OFFSET (`BookReaderScreen.ReaderZoomAsk`, `readerDoubleTapDocument`, `zoomAskOf`).** v431's fix was right for a PINCH and wrong for a double tap, because only a pinch arrives as many small steps. A lazy column holds `(firstVisibleItemIndex, scrollOffset)`, and `scrollOffset` is a pixel count INTO that sheet — a number that only means a place at the zoom it was measured at: at 2.2x a sheet is 2.2 screens tall, so its own offset can be LONGER than the whole sheet becomes back at 1x, the list has to ROLL it into the sheet above, and the member landed a page or three from the word they tapped (*"the double tap zoom and double tap again to unzoom is kinda buggy"*). What crosses the zoom now is the tapped point's **SHARE of its own sheet** (`ReaderZoomAsk`: index, fraction, the sheet's old height, the tap's viewport y, the ratio), and the scroll is corrected in a `LaunchedEffect` AFTER the relayout — it waits for the sheet to report a NEW height (a frame or two, three at most) before `scrollToItem(index, fraction · newSize − viewportY)`. **The rule to keep: a correction that depends on the layout's own numbers must be made after the layout it depends on exists — never computed from the layout it is about to invalidate. The sideways half stays synchronous (a scroll state is a plain number that settles itself).**
- **A PINCH IS NOT A TAP (`ReaderTouch`).** A pinch ends with both fingers lifting within a frame of each other, and Android delivers that as ONE event with every change up — which is exactly what `detectTapGestures` reads as a TAP, so every zoom ended by tapping the page it was made on: the tools came and went with the gesture, and a lift near the side of the screen turned the page instead of putting the chrome back (*"its appear and disapper of the tools"*). `ReaderTouch.multi` is set on the second finger down inside `pinchToZoom` and cleared on the NEXT gesture's `awaitFirstDown(requireUnconsumed = false)` — the one place that hears every gesture in the reader — and every tap the reader answers asks first: `onSurfaceTap` (so the tap ZONES are covered too), the chrome's own background tap, and both double-tap doors. Mid-gesture panic is impossible by construction: the pinch consumes every multi-finger event (which is also what cancels the tap detectors' long-press path), and the flag self-heals on the next down even if a surface is disposed mid-gesture.
- **THE CHROME ARRIVES AND LEAVES WITH A MOVE.** The head pill settles down from above and the foot pill rises from below (`slideInVertically`/`slideOutVertically` at 240/200ms with a 180/150ms fade) — a bare cross-fade read as a flinch rather than a tool arriving.
- **AND IT NEVER LEAVES ON ITS OWN (v432 REMOVED v406's 4.2s COUNTDOWN).** A timer cannot tell "the member has stopped using me" from "the member is reading the page I am over", and the tools vanishing mid-sentence is what that guess looks like (*"its appear and disapper of the tools"*). The chrome leaves only when it is TOLD to: `tapPage()`, a scroll the member made (`onScrolled`, guarded by `askedByReader` so a turn or jump the READER asked for keeps it), a selection, or a jump from a mark/chapter. **Do not reintroduce a countdown — if the tools need to be quieter, make them quieter, not self-dismissing.**

### v433 — the journal dock collapses, and the copy box floats

- **THE JOURNAL DOCK'S GROUPS ARE EXPANDING DOORS, NOT DROPDOWNS (`PersonalCanvas.PersonalDockGroup`, the dock's `openGroup`).** The member: *"collapse the B I U s and font into one toggle and no dont make it drop down but the option smoothly expands in that dock when its tapped, and similiar grouping for other tools keep this collapse style"*. One group is open at a time (`openGroup`, an `animateContentSize` on the dock row does the expanding); tapping a group's door toggles it, tapping another moves the opening. The groups: STYLE (B / I / U / S / size − +), FORMAT (the alignments, moved OFF their dropdown — "for format chnage it from dropdown to this collapse style"), EXPORT (copy + download, "advance for the copy and download" means this group holds them both), and the bullet stays its own menu ("but not for the bullet point"). Every door is an icon, no labels.
- **THE COPY BOX FLOATS AND IS MADE OF PILLS (`PersonalCanvas.ReachPill` / `ActionPill` / `CopyChip`).** The member: *"the copy floating layout, make the arrow proper pills in the corner hide the voice note opyion when copy tools are on, and add a cross button to close the option box, dont show the nothing picker or 6 out of 6 row text its no need, also instead of all rows use text select all or just the icon of select all, also fix the line selection, without all select i cant select only word by word, fix it, and instead of cut copy paste use its icon, and for undo the undo icon"*. So: a floating card over the page with a ✕ (the dock's copy door toggles it too), the reach arrows are four round pills (rows ↑↓, letters ←→, dimming at their axis' end), select-all is a single chip (the old ALL ROWS pill and the "x out of 6" caption are gone; the bundled glyph subset has no `select_all`, so the chip is the word), the clipboard actions are ICONS (`content_copy`, the subset's `cut`, and `undo` — measured against the name table, see [safeGlyphName]; the subset has no `content_paste`), the scope picker row and the voice-note door are hidden while the box is up, and the box floats so the page's own text keeps its place behind it.
- **WORD-BY-WORD SELECTION WORKS WITH NO ROW PICKED (`PersonalEditorState.nudgePageLetters` / `nudgePageRows`).** The letter window used to open only after a ROW had been chosen first, so without "select all" there was no way to take just a word. The letter axis now opens on its OWN first press — the current row is where the caret is, which is a row — and the row axis only narrows what the letters reach into.
- **"PAINT THE PAGE TOO" PAINTS THE PAGE (`PersonalPage.journalPaper` / `journalInk` via `LocalJournalPagePaint`).** The v430 flag existed but the reading/writing surfaces still asked the theme for their paper, so the switch did nothing the eye could see ("also fix the paint the page too not working"). All three paper-painting surfaces now read `LocalJournalPagePaint`, and the dock's export resolves the page's paint for a shared file.
- **A LIT TOOL READS AS LIT.** The selected-state was a washed fill the member called bad ("the selected highlight color of the tool it looks bad"); every toggle in the dock now has one selected style — filled disc, `onPrimary` glyph, real pressed animation — built ONCE (`PersonalToolButton`) so no tool drifts.
- **ONE CAPSULE STYLE FOR THE PAGE'S THREE PILLS.** The Today pill, the eye/pen switch and the mood capsule shared nothing but their roundedness and each was a different thinness ("now they ae too thing, use one unified capsule style"). `JournalCapsule` (in `PersonalTheme.kt`) is THE capsule: full radius, one height, one border, one fill; all three wear it.
- **THE SETTINGS SUB-PAGES LOST THEIR QUICK ROW.** "from the settings sub pages remove the quick row and its usggestions" — the QUICK TOOLS band under the chips on every sub-page and its deep-link suggestions are gone from `SettingsHubScreen`; the hub's own rows are untouched.

### v434 — the reader's sheets scroll, its look is remembered, and the dock is capsule tiles

- **A SHEET WRAPS ITS CONTENT, SCROLLS ITS OWN BODY, AND SHUTS ON A SWIPE FROM ANYWHERE (`ReaderSheetFrame`).** The member: *"the buttom sheet isnt scrollable and its not able to close with swipe so fix these"*, then chose "wrap content, cap at ~60%". The height is `heightIn(max = maxHeight * 0.6f)` — a short sheet is no longer half-empty paper — and the body is the ONE scroll in the sheet: **callers must pass plain content and must NOT wrap it in their own `verticalScroll`** (a scroll inside a scroll is a scroll nobody can use; `ReaderPlacesSheet`, `ReaderAppearanceSheet` and `ReaderDictionarySheet` all had to give theirs up). A downward drag anywhere moves the sheet through a `NestedScrollConnection` (`pull`): the body takes the drag while it can still scroll up and only the LEFTOVER moves the sheet. The leftover settles on a 150ms `debounce` because `NestedScrollConnection.onStop` is not in the API this reader builds against — **if you ever need a drag-end, check that call first**; the handle/title strip keeps its own `detectVerticalDragGestures`.
- **THE READER'S LOOK IS REMEMBERED (`ReaderLookStore`).** The member picked "Remember all of these across restarts". One prefs file (`curio_prefs`, the app's own), one key per field, a `reader_look_v434` mark so a fresh install keeps the object's defaults; loaded once by a `LaunchedEffect` on the way into a reader and written by a `snapshotFlow { ReaderLook.rememberKey() }` that is `distinctUntilChanged()` + `debounce(400)`, so a slider drag is one write. **`rememberKey()` must list every persisted field or a new setting silently never saves.**
- **THE LOOK'S NEW SETTINGS.** `ReaderLook` gained `lineSpacing`, `pageMargin`, `paraSpacing`, `justify`, `keepScreenOn` and `dim`; `ReaderAlign` (RAGGED / JUSTIFIED) is drawn by `ReaderAlignGlyph` because the bundled font has **none** of the `format_align_*` ligatures. They are applied in `ReaderParagraphBlock` (leading, alignment, paragraph gap), the two text surfaces' side padding (`ReaderLook.pageMargin`), and `paginateBlocks`/`pagedTextStyle` — **a page is MEASURED at the leading and margins it will be DRAWN at**, so any new look setting that changes a measurement must reach `paginateBlocks` too, and the `pages` `remember` keys in `TextPagedReader`. `keepScreenOn` is `LocalView.current.keepScreenOn` (a `DisposableEffect`, so it cannot outlive the reading) and `dim` is a pointer-transparent black wash drawn over the paper but UNDER the chrome and every sheet.
- **APPEARANCE IS CAPSULES AND SEGMENTS, AND A PDF HAS A ZOOM ROW (`ReaderAppearanceSheet`).** The member: *"use proper pill shapes, instead of toggle use proper 2 opton style with animation … its missing, the text size or zoom slider … add more proper apperance settings"*. `ReaderSwitchRow` is GONE; `ReaderSegmentRow` is the animated segmented pill (thumb slid in the LAYOUT phase via `offset`, label ink via `animateColorAsState`) and `ReaderSliderRow` is the one A−/slider/A+ shape every size-like setting wears (type, PDF zoom, leading, margins, paragraph gap, dim). A reflowable book gets Text size + Typeface + the layout rows; a PDF gets **Zoom** in that slot instead.
- **THE ⋯ MENU IS SIX CAPSULE TILES (`ReaderMenuSheet` / `ReaderMenuTile`).** The member: *"use proper pill shape grid with just capsulepills in a 6 grid with huge icon and a small text below instead of share a passage just share, reading settings to settings, tap zones to gestures"*. Two rows of three, a 30dp glyph over a small name; a count is a corner badge and the Gestures tile wears a lit dot while the zones are on. **The names are Share / Gestures / Settings** — do not restore the longer ones.
- **THE GESTURES EDITOR (`ReaderTapZoneEditor`).** The member: *"let user hide the overlay so they can see what they are doing and only overlay the slider when they adjust and hide the overlay when they use the slider … and selecting one tap zone should switch its area"*. `showOverlay` is an eye toggle (drawn — `ReaderEyeGlyph`, because the subset has `visibility_off` but no `visibility`), the depth slider lives behind a capsule and hides the washes while it is dragged (`draggingDepth`), and a tap on the page picks the edge it landed in (`zoneEdgeAt`, the same arithmetic as `readerZoneActionAt`). The GRIPS stay visible whatever the washes do. The panel is titled **Gestures** and the on/off lives in Reading settings, not on the tile.
- **A SWEEP OUTRANKS THE ZOOM'S PAN, AND THE HOLD DOCK HAS THE SELECTION TOOLS (`ReaderTouch.selecting`, `ReaderMarkSheet`).** The member: *"the highligh doesnt work like when im zoomed in and i try to ta and hol dto select it doesnt work and the dock that appears after i tap and hold well it doesnt have the tools we had before for selections"*. A magnified PDF's `pinchToZoom(zoomed = …)` claims every one-finger drag from its first move, which cancelled the text layer's long-press sweep before it could report a word; `ReaderTouch.selecting` is set the moment the sweep's long press fires and the pan stands down for the rest of the gesture (cleared on drag end/cancel). And the mark dock now offers the dictionary (seeded through `dictionarySeed`) and Share on the passage, so a passage chosen by holding a page has the same doors a sweep does.
- **READING SETTINGS IS THE APPEARANCE SHEET'S COMPLETE TWIN (`ReaderSettingsScreen`).** Same components, same order of questions, plus the three-way orientation (a segment, which is why the sheet no longer tries to say it with a switch) and the Gestures section. `showZoom` was added; `onEditTapZones` became `onGestures`.

### v435 — a member's face is derived, and the reader's head clears the glass

- **THE SOCIAL PORTRAIT IS DERIVED, NOT CHOSEN (`features/community/Blobatar.kt`).** The member: *"can we use this profile avatar style for social instead of those bad drawing?"* — pointing at **[blobatar](https://github.com/Alain00/blobatar)**, whose gen-2 core this file is a faithful Kotlin port of. `BlobatarArt(seed)` turns ANY string into a face: one of ten weighted silhouettes, two capsule eyes fitted inside it, and a tone from six authored swatches in OKLCh. `SocialAvatar(seed, …)` takes a **seed**, not a style — build it with `blobatarSeed(userId, username)` (username if set, else account id, which is the member's chosen rule).
- **THE PORT IS VERIFIED AGAINST UPSTREAM, AND THAT IS THE CONTRACT (do not "simplify" it).** Every constant, range, band edge, tone threshold and Bézier control offset is carried over unchanged and the arithmetic runs in `Double` exactly as JavaScript numbers do; only the final path coordinates narrow to `Float`. It was diffed against blobatar's own renderer over 43 seeds (three per silhouette plus the avalanche and NFC cases) across 430 values — hue, tone, silhouette, body geometry, every radius, the face, petals, the droplet taper, both eyes and the full traced path geometry — and **all match**. Three guarantees carry the whole library and each is easy to break by tidying: the murmur3 finalizer in `finalize` (without it "alain" and "alaim" are the same face), the hash-once seed state with per-key streams (trait keys are an **append-only namespace** — adding one cannot disturb an existing face), and the OKLCh contrast walks in `ensureContrast`/`FLOORS` (the eyes clear 4.5:1, which is what makes a face legible at 26dp). Changing a **band edge or a `pick` array** does not tune the set, it re-rolls every face in the app. The face is drawn with **NO clipping** — the 0..100 viewBox IS the disc — and that is measured, not assumed: over 20,000 seeds the furthest any part of any face reaches from the centre is **48.30 of the disc's 50** (the worst case is an `organic` body). Re-run that scan if a band edge, a `body.r` range or a `sun.dist`-style range moves.
- **THE 28-PORTRAIT CAST AND THE PICKER ARE GONE.** `SocialAvatar.kt` is now a thin wrapper (the composable, the presence dot, `drawBlobatar`) and `AvatarPickerIcon` / `SocialAvatarPickerRow` / `includeAvatarPicker` no longer exist. **`profiles.avatar_style` is deliberately LEFT IN PLACE** — no schema change, no migration, reads still clamp against `SOCIAL_AVATAR_STYLE_COUNT`, which now documents itself as a bound on a retired set rather than a count of styles. Dropping a column is irreversible and needs the member's word; a small unused integer is not a mess.
- **ONE RENDERER SERVES THE LIST AND THE SHADE (`SocialNotifications.NotificationAvatars`).** A notification's large icon is an `android.graphics.Bitmap` painted from a receiver with no composition and no network — which is exactly why the face is derived locally instead of fetched from `blobatar.dev`. The cache is keyed by **seed** and capped in size (a member's handle is not ours to trust), and it paints through the same `BlobatarArt` the canvas does. **Never add a notification-only drawing: a second renderer is the drift this whole design avoids.**
- **THE READER'S HEAD STATES ITS OWN TOP FLOOR (`Modifier.readerChromeTopInset`).** The reader HIDES the system status bar, so `statusBarsPadding()` collapses to zero under it and the head pill used to settle 8dp from the very edge of the glass (member: *"the header of the search and back and title floating pill its too much close to the status bar"*). The chrome does not ask the hidden bar for room: the inset is `WindowInsets.displayCutout.only(Top)` plus `ReaderChromeTopFloor` (18dp). The search bar and the pinned page count wear the same modifier so all three land on one line. **Any new top chrome in this reader must use it, not `statusBarsPadding()`.**
- **THE SEARCH IS ITS OWN ROUND PILL AND GROWS OUT OF IT (`ReaderTopPill`).** The member: *"separate the search and the back and title pill, search icon is just a circle pill and when opened it merges smoothly with the header for search"*. The head is a `Row` now — the name capsule takes `weight(1f)` and a 50dp `CircleShape` search pill sits beside it with an 8dp gap — and its two states animate as ONE move: the search bar `expandHorizontally(expandFrom = End)` as the name capsule `shrinkHorizontally(shrinkTowards = Start)`, so the action reads as the search opening rather than two panels swapping.

### v436 — the dock's panels, a smaller bullet roster, and blobatar's notice

- **EVERY MULTI-CHOICE TOOL IN THE DOCK OPENS IN THE DOCK (`PersonalDockGroup`).** v433 collapsed format and alignment into panels inside the pill; v436 added `HIGHLIGHT` and `BULLET`, which were the last two Material `DropdownMenu`s floating over the page. **The fast path is kept deliberately**: with no pen/list on the line yet, the door's FIRST tap still applies the first pen / the first marker, and only the second tap opens the panel — so do not "simplify" a door into an always-open panel. `openGroup` belongs to the dock (one panel at a time) and a caption takes the row over, which clears it.
- **A DELETED BULLET FALLS BACK, IT DOES NOT REFUSE (`PersonalMarker`).** `CRYSTAL` and `LEAF` are gone from the enum (the member's "remove leaf and crystal from the bulletpoints"), so a page already written with one resolves through `fromKey` to `null` and is drawn with the FIRST marker instead. That is the deliberate cost of deleting a style: the list stays a list. Never delete a marker key without checking `fromKey`'s callers.
- **A MARKER CENTRES ON `lineHeight / 2`, SO EVERY SHAPE MUST BE CENTRED ON ITS OWN MASS.** The heart's lobes sat at `cy − 0.30r` and its point at `cy + r`, which put the shape's middle 0.12r BELOW every other marker's axis, and the dash was drawn from `x = 0` at 0.92 of the box's width — 4% off-centre. Both are centred now. **When adding a marker, draw it around `cy` and `size / 2` and nothing else; the roster's whole point is that every mark sits on the same axis.**
- **THE DOCK IS THE READER'S PILL (`PersonalToolDock`).** 42dp tools with 9dp of air in the row, a 28dp radius and a 12dp lift. The radius is fixed rather than a full capsule on purpose: the pill GROWS a panel under its row, and a 50% radius on a two-row pill would arc through the first and last tool of that panel.
- **blobatar'S MIT NOTICE IS A LICENCE OBLIGATION, NOT A CREDIT (`res/raw/blobatar_license.txt`).** The member faces are a Kotlin port of blobatar's generation-2 core (see `features/community/Blobatar.kt`), and MIT requires the copyright and permission notice to ship with a substantial portion of the software. The row lives in Support → About Curio's credits card beside TMDB's and Kyant's, and opens the bundled notice verbatim. **Do not replace the row with a link to the repository** — a link is not the notice.

### v437 — the faces are alive, the page's colour reaches its chrome, and the reader's sheets are fast

- **THE SOCIAL FACES ANIMATE, AND THE PORT IS EVALUATED, NOT DECLARED (`BlobatarIdle`, `BlobatarPose`, `BlobatarIdleClock`).** Upstream ships its idle motion as a CSS layer (`motion.css` + `animate="always"`) which a Compose surface has no stylesheet for, so the loops — breathe, bob, blink, glance and the eye wrap — are evaluated from upstream's own numbers and seeds instead. **The seeds are the load-bearing part**: breathe and bob draw INDEPENDENT phase offsets (sharing one locks the whole grid into one drift), blink and the glance draw their own periods and offsets, and the glance's direction is a magnitude plus a sign drawn separately. Easing is the smoothstep standing in for `ease-in-out` (the difference is below one device pixel at a 2.2% scale), while the blink and glance windows are LINEAR exactly as upstream — a glance must SNAP between fixations. **Only the ambient layer is ported**: there are no expressions and no hover in Curio, so the pose channels, the tremor, the seesaw and the colour morph have nothing to act on and must not be invented.
- **ONE FRAME CLOCK, AND NOTHING RE-COMPOSES.** `BlobatarClock` holds a single elapsed-milliseconds state; `BlobatarIdleClock()` is the host composed ONCE at the app root (`MainActivity`), and it runs only while `BlobatarClock.watchers` is above zero, so an app with no faces on screen is exactly as idle as it was before this layer existed. Each `SocialAvatar` reads the clock **inside its `Canvas` draw lambda**, which is tracked by the draw phase — a tick re-draws the face and re-composes nothing. **Do not move that read into composition, and do not give an avatar a `rememberInfiniteTransition` of its own**: per-avatar animations are exactly the cost this shape exists to avoid.
- **THE PLATFORM'S OWN MOTION SWITCH IS OBEYED (`rememberCurioMotionEnabled`).** Android's "Remove animations" zeroes the animator scale — the same flag `CurioFloatingPet` reads — and a face that animated through it would be ignoring an accessibility switch. It is checked per avatar (so no clock watcher is registered) and by the host (so no loop starts).
- **THE PAGE'S COLOUR REACHES ITS OWN CHROME.** The member: *"still the journal page and ts buttons dont get the tehe color by chnaging it"*. Three surfaces were still wearing the THEME's accent whatever colour the page had been given: `journalPaperRaised()` (the fill every page-level capsule wears) was lerped toward `personalAccentInk()` instead of the page's own colour; `PersonalToolDock` lit every tool with `personalAccentInk()` rather than `journalDoorAccent(journalAccent)`; and `JournalTopBar` used `personalAccent()` / `personalAccentInk()` / `personalOnAccent()` outright. The bar is composed in the HEADER slot, **outside** the page's `JournalPagePaint` provider, so it cannot read the colour from the composition local — it is HANDED the argb. `journalOn(fill)` is the readable ink for an arbitrary fill (the theme's "on accent" answer does not hold for a colour picked off a wheel). **The page's PAPER still takes the colour only while "Paint the page too" is on** — that is the v429 option, kept deliberately; do not flip its default without the member's word.
- **THE COPY BOX IS TWO ROWS AND THE DOCK STEPS ASIDE FOR IT (`PersonalPageEditBar`).** The member: *"for the copy tools i will have to scroll to see all options so fix that"* and *"two row and hide the dock just show the copy doc when thats on"*. The single horizontally-scrolling row was how cut/copy/paste/undo/close went off-screen on a phone. The split follows what the two halves ARE: HOW MUCH is in hand (the four reach arrows, Select all, Line) above, WHAT TO DO with it (cut, copy, paste, undo, and the cross pushed to the right corner by a `weight(1f)` spacer) below. Nothing scrolls, so nothing can be off-screen. `PersonalWritingPage`'s dock `AnimatedVisibility` now also tests `!editor.pageEditBarOpen` — **the box IS the page's bottom while it is open**.
- **A MARKER'S AXIS IS THE WORDS' CENTRE, NOT THE LINE BOX'S (`PERSONAL_MARKER_AXIS_LIFT`).** The member: *"fix its weird positioning not matching with the text"*, then "Too low, below the words". `drawPersonalMarker` centres on `lineHeight * (0.5f − 0.06f)`: a line box carries the descender space a line of words rarely uses plus half the leading, so its geometric middle sits BELOW where the eye reads the words' centre. The lift is a FRACTION of the line on purpose — the page writes at several sizes, and a fixed dp nudge that fixed the default would hang a mark high on a large one. It is ONE constant so the roster cannot drift apart again.
- **THE PAGE SLIDER IS A FLOATING PILL, NOT A SHEET (`ReaderScrubPill`, `scrubOpen`).** The member: *"the page scrobble slider it needs to be similair to the dock small floaating without the buttom sheet so its easier todo the page scrbbing faster … a buttom pill floating at the buttom with the slider and hides when tap on page, also a way to close it"*. `ReaderSheet.SCRUBBER` is GONE from the enum: a sheet is a modal panel with a scrim that covers the page, which is the opposite of what scrubbing through a book wants. It is a flag on the reading surface (`scrubOpen`), drawn in the root Box above the foot pill, with the two hold-to-turn arrows, the count following the THUMB (not the settled page) and a cross. **A tap on the page closes it before the chrome toggles** — that is done in BOTH `tapPage()` and the root Box's own tap handler, because a tap that never reaches `onSurfaceTap` (a margin, a magnified frame) still has to put it away.
- **THE READER'S SHEETS ARE FAST, AND A LIST KEEPS A PANEL'S FLOOR (`ReaderSheetFrame`).** The member: *"the drop downs of each is slo, lie it takes a secdond to close"* and *"the highloght and notes dropd won is so small"*. Every sheet is 200ms in and **120ms out** (`FastOutLinearInEasing` — the eye needs the sheet gone, not a performance of it going), the drag settle lost its 150ms wait for an 80ms one, and `ReaderSheetFrame` gained `minHeightFraction` — `ReaderPlacesSheet` passes **0.45f** so Notes / Highlights / Bookmarks / Contents never collapse to a two-row strip that reads as broken. **A sheet that genuinely has one line to say (the dictionary, the mark sheet) must keep the default 0 and keep wrapping.**
- **THE ⋯ GRID IS SMALLER, AND THE HEAD/SEARCH MORPH SHARES ONE CLOCK (`ReaderMenuTile`, `ReaderTopPill`).** The member: *"the 3 dot menu in pdf reader is bad like too huge"* — tiles are **68dp** under a 24dp glyph (they were 94/30), with 8dp of air. And *"for the floating title in pdf reader use smooth fade animation"* + *"for search pil use merge and smooth morphe"*: the head now settles six-of-its-height over a longer `LinearOutSlowInEasing` fade instead of travelling half of it, and the head's exit and the search bar's entry both run **220ms on one easing** — when they disagreed (200 vs 300) the row had neither for a tenth of a second and the morph read as two panels swapping. The head still shrinks toward the **End** on its way out, so the search reads as growing out of the circle pill.

### v438 — the page always keeps a line to type in

- **`publish()` IS THE ONE WAY AN EDIT IS PUBLISHED, AND IT KEEPS A WRITABLE LINE (`PersonalEditorState`).** The member: *"using cut when selected all removed that line and i cant type anything again in the body, coz i dont get any option to"*, and their rule for it: **"Always one empty line to type in"**. An empty document is the one state the writing surface cannot come back from — a BLOCK is what a caret lives in and what the keyboard is attached to, so with none left there is nothing to tap into and nothing to type in. `keepLineToTypeIn()` adds one empty, focused, caret-bearing block when `order` holds no block the member can type in (`!isPhoto && audio == null`, so a page of nothing but a photo and a voice note gets the line too), and **every mutation now ends at `publish()` instead of `onDocChanged(doc())`** — an edit path that calls `onDocChanged(doc())` directly is a bug, whatever it does. That is not a new rule so much as a rule that was already half-written: `removeBlock` carried its own copy of the guard while cut, the row tools and the gesture tools carried none.
- **THE GUARD CANNOT MAKE AN EMPTY PAGE NON-EMPTY, AND THAT MATTERS.** `PersonalDoc.isEmpty` is `blocks.none { it.isPhoto || it.text.isNotBlank() }` — content, not block count — so a page the member cleared is still empty for `shouldWrite`, and the auto-added line cannot make the app save a blank page forever. The decoder's own rule (`if (blocks.isEmpty()) listOf(newBlock())`) is the same invariant at the other end of the pipe: **a body can never decode or be edited into zero blocks.**
- **THE READER ANSWERS THE BACK GESTURE (`BackHandler` in `BookReaderScreen`).** The member: *"backing from settings exits the reader"*. Reading settings is drawn OVER the reading from INSIDE this screen (it is not a route) and the reader registered no back handler at all, so the system's back went to the nav stack and popped the READER out from under the settings page. One handler now closes the innermost thing that is open — settings, the zones editor, a sheet, the page slider — and is enabled only while something IS open, so with nothing up back still leaves the book. **Any new overlay this screen grows must be added to that handler.**
- **THE SETTINGS HEAD USES THE READER'S OWN TOP FLOOR (`Modifier.readerChromeTopInset`, now `internal`).** Same bug as v435's head pill, one page over: settings is drawn under a reader that HIDES the status bar, so `statusBarsPadding()` collapsed to zero and its head sat on the glass (member: *"in settings the header is again over the status bar"*). One floor, both heads — never `statusBarsPadding()` inside this reader.
- **THE NIGHT DIM IS OVER THE CHROME NOW (v434's note is superseded).** The member: *"the night dim should also work on the buttons etc"*. It used to be drawn UNDER the chrome; a dimmed page under undimmed white pills is the one arrangement that makes the tools the brightest thing in a dark room. It is drawn over the page AND the whole chrome (head, search bar, foot pill, pinned count) and under the things a member works in — the selection bar, the mark dock, the zones editor and every sheet — because those are drawn after it in the Box or outside it.
- **THE ⋯ TILES ARE A PILL WITH THE GLYPH IN IT AND THE NAME UNDER IT (`ReaderMenuTile`).** The member: *"the 3 dot in pdf buttom sheet still looks bad with huge buttons, and keep the button inside the pil keep the text out of it"*. A 46dp capsule holds the 22dp glyph and nothing else; the name is a label BELOW it, outside the fill. **The name must never go back inside the pill** — that is what made the tile read as a control with two controls in it.
- **AN EMPTY MARKS LIST IS AN EM DASH (`ReaderMarksSection`).** The member: *"highliths and notes empty stat eis bad dot use erm dash"*. The sheet's own title already says which list is empty, so the sentence that used to stand there was read again on every opening to learn the same thing.
- **ONE WORD GETS THE WHOLE SELECTION BAR (v431's cut-down is reverted).** v431 showed the dictionary ALONE for a single word; the member's answer now: *"the hihglight screen dock only have disconary option and nothing else … see it had many hihglith options etc"*. One word is the commonest thing a reader highlights, so the five pens, the note, the bookmark, the dictionary, the ⋯ door and the cross are one row for every selection — the `singleWord` branch is gone, and the dictionary was already the row's own door. **Do not reintroduce a branch that hides the pens.**
- **THE PAGE SLIDER HAS THE FOOT TO ITSELF (`ReaderChrome(footHidden = …)`).** The member: *"the page slider is bad too it should hide the dock hen the slider shows"*. With both up, the page's bottom carried two rows of controls and the count button that OPENED the slider sat underneath it.
- **KNOWN AND ACCEPTED: undoing a full-page cut keeps the line.** The undo closure restores the removed rows, and the empty line the guard added stays at the foot — which is the same state the member would reach by pressing Enter at the end of the page. Removing it would mean the undo path knowing which block the guard minted, and an invisible trailing line is not worth that coupling.

### v439 — one clock for the floating furniture, the way out gets its own pill, and reading stops heating the phone

- **ONE MOTION SET, AND IT IS `CurioMotion`'S SECOND HALF (`ENTER_MS`, `EXIT_MS`, `Enter`, `Exit`, `Soften`, `settle`, `pillArrive()`, `pillLeave()`, `popArrive()`, `popLeave()`).** The member's own pick: *"the motion token set and one arrival for every floating pill and expand that style to more"*. `CurioMotion` already held the app's SPRINGS (how a thing that is already moving settles); it now also holds the PILL CLOCK (how a thing that appears comes and goes), which is what the reading and writing surfaces' floating furniture needed and did not have. **The rule is enforceable, not aspirational: a duration written as a literal next to a pill is a bug.** The factories exist so the token set can be SPENT in one step — a surface that hand-builds `fadeIn(tween(220, …)) + slideInVertically(…)` will eventually hand-build it slightly differently, which is exactly how the app arrived at ~200ms beside 180ms beside 160ms and a report of *"the animations are still bad"*.
- **`v437`'s SHEET TIMINGS ARE SUPERSEDED.** The reader's sheets were 200ms in / 120ms out as literal numbers; they are `CurioMotion.ENTER_MS` / `EXIT_MS` now. The 45% floor on Notes / Highlights / Bookmarks / Contents stands.
- **A SHEET LEAVES BY ITS OWN HEIGHT (`ReaderSheetFrame`, `measuredHeight`, `travel`).** The member: *"still the pdf reader buttom sheet close is weirdly slow looking"*. It travelled `(1 - appear) * capPx`, and `capPx` is **60% of the SCREEN** — so a five-row sheet 200dp tall slid more than twice its own height to leave, and every one of those pixels below it is empty travel the eye reads as slowness. It is measured (`onSizeChanged`) and leaves by exactly its own height, with the cap kept only as the one-frame fallback before there is a height to travel by. **When someone says a transition feels slow, check the DISTANCE before the duration.**
- **REGRESSION, AND THE REASON THE MOTION SITES ARE WORTH A GREP: `CurioMotion.kt` WAS OVERWRITTEN AND RESTORED.** Adding the pill clock briefly replaced the whole file, which would have taken `Springs.*`, `Durations.*`, `ConfettiParticleCount` and `MinSpinTurns` out from under ~30 call sites across `CurioAnimations`, `CurioNavHost`, `CurioConfetti`, `CurioPressFeedback` and the card components. **`ui/theme/CurioMotion.kt` is a PUBLIC token file other screens compile against — extend it, never rewrite it, and check `git status` for `M` rather than `??` before writing a file you believe is new.**
- **THE WAY OUT IS ITS OWN PILL (`ReaderTopPill`).** The member: *"for pill use pill for the back button its own pill"*. The way out was the first glyph INSIDE the name capsule, so the one control a member reaches for without looking shared its fill with a label and the two had to be the same width whatever each needed. It is a 50dp circle of its own, the same object as the search door at the other end: out, the book's name, search — three pills of one family, and the name (the only LABEL in the row) takes the room the two circles leave and ellipsises inside it.
- **THE READER'S CONTROLS PRESS BACK (`ReaderChromeButton`, `ReaderPillButton`, `curioPressClickable`).** The member's own pick: *"Press feedback everywhere"*. A reader's chrome answered a tap with nothing but the thing it did — on a surface that hides its own chrome, that is a tap a member makes twice because the first one looked like it missed. `ReaderChromeButton` is the reader's ONE control (head, search bar and foot are all built from it), so one change lands everywhere; it and the foot's glyphs now use the app's own press helper rather than a second one written here.
- **A VOICE NOTE'S WAVE NOW DEPICTS THE SOUND (`WaveformExtractor`, `voiceReach`, `LiveVoiceWave`).** The member: *"make the wave and bar of the sound in vn more accurate depiction"*. The fault was one layer below the drawing, and it was there twice. **Every bar was normalized against 16-bit FULL SCALE** (peak / 32767) — a phone mic recording normal speech peaks near 0.10–0.35 of that, so every bar landed in the bottom fifth of the range — and then `voiceReach` SQUARED it, which is the v424 "not clear differnt" fix compensating for exactly this bug. A normal sentence drew at about 4% of the band: a flat line with a few ticks in it. Three changes: **(1) every bar is now relative to the recording's OWN loudest moment** (the peak of the note is 1.0, everything else proportional) — the storage contract does not change, still one hex byte a bar, so no old note is invalidated and no migration is needed; **(2) a bar is the geometric mean of its window's peak and RMS** (`sqrt(peak · rms)`) rather than a bare peak — RMS alone hides a consonant, peak alone is the fuzzy band of near-maximum columns a fast passage used to draw; **(3) `voiceReach` is LINEAR again — the height IS the amplitude** — because the double compression is gone. **A genuinely silent file is NOT amplified** (`SILENCE_PEAK`): honest silence draws a flat line. The LIVE METER had the same fault from the same cause (`maxAmplitude / 32767`) and is now read against a decaying reference that follows the loudest thing the member has said, with a floor so a quiet room is not drawn as speech. **If a wave ever reads too subtle again, fix the extractor's normalization, never add another exponent to `voiceReach`** — a curve over un-normalized data is what produced two rounds of this. Also in the same pass: the decode loop no longer appends to a `MutableList<Short>` (which BOXED every sample — sixteen million heap objects for a three-minute stereo note, at exactly the moment the member pressed stop); `PcmSink` grows a `ShortArray`. `WaveformExtractor.extract` is used by the capture sound bite and the entry detail screen too, so all three now share the relative scale. **And the bucketing was wrong too** (`bucketLevels`): it split with a CEILING division and repeated the previous bucket for every one past the end, so the bars look (42 columns from 72 samples) drew data in 36 columns and six stale repeats, the ribbon in 24 of 30, the bubble in 24 of 34 — a wave with a flat stale tail whose time axis was compressed into the left of the drawing, which is both inaccurate AND wrong about when everything happened. Each bucket now takes its own proportional span, so the whole recording is drawn exactly once at whatever resolution the strip can show. **Never go back to a per-bucket ceiling count.** The export uses these same two drawing functions, so a printed note follows every one of these fixes for free.
- **A TAP AND A HOLD SURVIVE A MAGNIFIED PAGE (`pinchToZoom`'s wear-in, `travelled`/`claimedPan`).** The member, twice: *"when im zoomed in and i try to tap and hold to select it doesnt work"* and *"when zoomed in the tools doesnt appear when i tap once"*. **Neither was a broken detector, and neither was a missing flag** — a finger that taps or holds is not perfectly still, and the page panned (and CONSUMED) those few pixels from its first event. One consumed move is enough to cancel `detectTapGestures`, which drops a tap the moment a tracked change is consumed and does the same to a pending long press — so on a magnified page a tap could not raise the tools and a hold could never reach the sweep. The sweep's own stand-down (v434's `ReaderTouch.selecting`) is only set **once the long press FIRES** — and it never fired. The page now **wears in** like every other scrollable surface: it takes nothing until the finger has travelled the touch slop. A real drag crosses the slop on the same event the pager's own wait would have used, and the child wins there because a descendant's handler sees the event first and its consumption cancels the parent's wait — so **v403's guarantee (the page keeps the drag instead of handing it to the pager mid-slide) is intact.** Note the drift this exposed: `readerZoomThisPage`'s own doc already promised "nothing is consumed until the finger has travelled past a share of the touch slop" and **nothing implemented it** — the rule belongs in the GESTURE, which is where it is now, not in the zoom arithmetic.
- **THE COPY BOX IS THE DOCK'S SHAPE NOW (`PersonalPageEditBar`, the reach door, `reachOpen`).** The member: *"the copy paste tool bar two row ui is bad and not like that dock ui"*. v437's two stacked rows answered "I have to scroll to see all options" and created a worse problem: a box holding two toolbars read as a second toolbar. It is **one row with the reach behind a door that grows INSIDE the same pill** — the dock's own pattern (`animateContentSize` on the Column, `pillArrive`/`pillLeave` on the panel, `PersonalDockGroup` for the reference shape) — with the actions OUT in the row, because a member who has to open a door to reach Cut has been given an extra tap for the commonest thing they came to do. **The door is LABELLED with the quantity** ("Select", "Line", "3 rows", "14 letters") because a reach has no glyph in the bundled subset and the quantity is the one thing a member needs before they cut; `offerReach` also OPENS the panel, so the whole-page offer is visible to adjust rather than applied invisibly.
- **ONE EMPTY STATE, AND `CurioEmptyLine` IS IT (`ui/components/CurioEmptyLine.kt`).** The member's pick from five unification passes. The rule is narrow and deliberate: **a BARE LIST inside a surface that already names itself says nothing but a dash** (the comments sheet under "Replies", the history panel under "Text history", the reader's marks sheet), while **an EMPTY SCREEN keeps its headline, subtext and door** ("No conversations yet" with a door to start one is the screen's only affordance) and **a message that tells the member how to FIX the emptiness is an instruction, not a state** ("Lookups are off in Settings — turn them on…") and stays. Read the component's own note before converting another one.
- **THE MEMBER MAY WEAR THEIR BLOB AS THEIR OWN PICTURE (`AppPreferences.isProfileAvatarBlob`, `CurioMemberAvatar`, `hasOwnPicture`).** The member: *"let user set that blob as their pfp in app profile too"*. The app's profile had exactly one kind of picture — a photo cropped on this device — while Social drew every member a face derived from their handle, so the two halves of the same app showed the same member as two different people. The blob is offered **beside** the photo, never instead of it: it is a stored choice (`profile_avatar_blob`, reactive like the path beside it), and turning it off puts the exact photo that was there straight back. Two rules: **the seed is the handle, exactly as Social derives it** (`blobatarSeed(null, AppPreferences.getUsername(context))` — never the display name, or the same member would wear two faces), and **every surface must ask `hasOwnPicture(path)` before choosing between the picture and its own initial** — a member with a blob and no photo HAS a picture, and a fallback that only tested the photo's path would leave them looking at a letter while their blob existed. The call sites are the profile hero, the edit dialog's preview, the profile row, and Home's drawer hero and two identity rows; the initial is the one part that differs between them, so it stays written where it is rather than being guessed at in the shared composable.
- **THE ZOOM SLIDER IS GONE AND THE MOTION LOCK REPLACED IT (`ReaderLook.motionLock`, `ReaderMotionLockPill`, `ReaderAppearanceSheet`, `ReaderSettingsScreen`).** The member: *"in pdf only remove that zoom slider and add the motion lock pill which restrits that drag to move and pinch to zoom it locks in the state the user left the zoom position"*, then chose **freeze pan AND pinch, and remember it**. A slider was the wrong control for a PDF page anyway — the page is a picture, the pinch sizes it, and what the member wants afterwards is for it to STAY. **The lock is enforced in the GESTURE PATH, never in the drawing** — one guard at the top of `pinchToZoom`'s event loop, which is the one handler EVERY gesture in the reader passes through (so no surface can be forgotten), plus one line in `readerDoubleTapZoom`. Two rules there are load-bearing: it **consumes** movement and pinches (ignoring them would leave the drag to be claimed by the scrolling column or the pager underneath, so the page would move anyway), and it **must never consume the first down** — a tap is a down and an up with nothing in between, and `detectTapGestures` needs that down unconsumed, so consuming it would make the page untappable and take the page turns and the chrome with it. The pill floats above the foot's RIGHT corner, is a PDF's alone, hides while the page slider is up (it obeys the same `footHidden` the foot does) and comes and goes with the chrome — a member reading with the tools away is reading, not adjusting. **`motionLock` MUST stay in `ReaderLook.rememberKey()`** or it saves every field except this one (the v434 rule).
- **LOW POWER READING, ON FROM THE START, AND IT IS A REAL TOGGLE (`ReaderLook.lowPower`, `ReaderSettingsScreen` "Power").** The member: *"in pdf reader, a high charge save turns on which makes the app cache and background usage very low in reder so the phone doesnt heat"*. Reading is the one screen a member sits on for an hour, and it was spending more than the words cost: **every PDF page was a full-screen ARGB_8888 bitmap up to THREE times the screen's width** (pixels that are never displayed, held while the page is on screen) and **the pager kept its neighbour composed** (`beyondViewportPageCount = 1` — a second full render, a second text extraction and a second set of marks for a page that may never be turned to). On: pages are **RGB_565** (a page of a book is opaque — an alpha channel is a quarter of the bitmap spent on nothing) and the upscale stops at **1.5×**, `beyondViewportPageCount` is **0**, and the reader **prunes `cacheDir/book-images` as it closes** (only the reader's own folder; the book re-copies what it needs next time, which is why that folder is a cache). **It stays a two-option row rather than a switch** because the choice is which way the reader spends, not whether to do a thing — and "it got blurrier and I could not turn it off" is not a trade a reading app gets to make for someone. Off-screen pruning happens on `Dispatchers.IO` and must never be moved onto the composition thread.
### v445 — an offline dictionary, a badge row, and the sky that twinkles

- **THE DICTIONARY HAS A DOOR THAT LIVES ON THE PHONE (`ReaderOfflineDictionary`, the sheet's badge row).** The member: *"the online dictionary is bad, add a downloadable dictionary inside the app in the dictionary bottom sheet … when the dictionary is opened from the 3 dot one [it should be] more longer and let user search any word"*, and, asked what to download, **a full offline dictionary, offline first once downloaded**. The source is **Webster's Unabridged 1913** (public domain, ~9MB, ~86,000 headwords) fetched from `matthewreagan/WebstersEnglishDictionary`'s JSON conversion — chosen because it can be kept for good with no per-lookup licence, it is a dictionary rather than a word list, and it fits on a phone connection. Three rules are load-bearing: **the bytes land in a `.part` and are only renamed into place when the whole file is there** (an interrupted download must never be searchable); **the lookup STREAMS the JSON with `android.util.JsonReader`** rather than parsing it (a 9MB object read into heap is tens of megabytes for one word), and stops early because the data is alphabetical; and **a missing file answers `null`, not `emptyList()`** — "there is no dictionary" and "there is no such word" are still different answers (the v443 rule). Nothing is fetched until the member taps Download, and the same row is where it is removed. The **two online doors are no longer a setting you cannot see**: the sheet carries a badge row (Offline · Wiktionary · Free, offline first once its file is there) and shows the answer of the door you are on, so a word one door does not carry is one tap away rather than a trip to Settings.
- **A SHEET RIDES ABOVE THE KEYBOARD, AND ITS SWIPE REALLY ENDS (`ReaderSheetFrame`).** Two member reports, one frame. *"Fix the search box hiding below the keyboard for dictionary"*: the frame took the IME inset on its own root box, so a bottom-aligned sheet lifts clear of the keys — and the wrap/cap floors are measured against what is LEFT, so a tall sheet never measures itself into them. *"The swipe down to close is buggy it stays as an overlay for some time"*: a body drag has no "end" of its own (a nested scroll reports travel, then stops), so the only thing that ever settled the sheet was the debounce timer — the panel SAT half-way down for the whole wait after the finger was gone. The frame now watches the raw pointer stream and settles the instant no finger is left down, on the same flick the head already read. **A bottom sheet must always have a pointer-up hook; a debounce is a fallback, never the mechanism.**
- **A LINE ENDS IN A SPACE (`PdfPageText.joined`, `textBetween`).** The member: *"the text ta and hold selection is bad, like when the line end and i copy 2 line then both lines are touching each other with no space"*. Glyphs arrive in drawing order and each carries its own text, and the extractor materialises a space only where the page DREW one — which a line break is not — so joining them with nothing produced `…the end of the lineand then…` in everything that reads a selection (the copy, the dictionary's context line, a share, a stored highlight's words). A break is now decided by the GLYPHS' OWN GEOMETRY: a baseline that moves, or a real gap to the right, is a space — **except after a hyphen**, because `under-`/`standing` is one word and a space there would invent a word the page never printed.
- **THE DOCK'S PANELS CLOSE THEMSELVES (`PersonalToolDock`, `PersonalDockGroup`).** The member: *"the bulletpoint and highlight … I need to tap that last cross to close it, and there's one at the first to dismiss the picked, so make the close button the first one both dismiss and deselect the pick, and also the collapse auto closes when I start typing or I tap the page"*. The marker's "No marker" and the bullet's "Remove list" now **also close the panel** (taking the pen off the line IS the end of what the panel was open for), the trailing cross survives only where the panel opens with a real choice (format, alignment, export), and a panel closes itself when **the focused line changes** (which is what tapping the page does) or **its text changes** (typing). Applying a marker or a list style writes the MASK, not the words, so the panel the member is choosing from is never closed out from under the choice they just made.
- **THE SKY TWINKLES AND ARRIVES WITH THE DRAWER (`DrawerLaneStarMap`, `HomeDrawerContent(open)`).** The member: *"animate it with star twinkle, and animate every time it closes and opens, with beautiful mesh like animation don't change the design, just beautifully animate it, also fix the light glow of the category tint when one is selected, and dont grow the dot too much"*. One progress (`reveal`) is driven by the DRAWER'S OWN STATE (`targetValue != Closed`, threaded from `CurioNavHost`): the panel eases up a hair as it fades in, the stars and every hairline follow it (the hairline runs out from its own star toward the one it joins, so the mesh DRAWS itself), and the whole thing reverses on the way out. The twinkle is a `withFrameNanos` clock that **only runs while the drawer is open** (a sky nobody is looking at must not spend the battery) and whose float is read INSIDE the draw block, so it costs draw passes and never a recomposition. The picked aura is the lane's OWN tint now — deeper mixes and tighter radii (0.34/0.58/0.82 at 2.6/1.7/1.2 of the core instead of the unpicked steps at 3.2/2.0/1.35) — because the old picked bloom was the same pale mix, only wider, which read as a LIGHTER star rather than a lit one; and the dot barely grows (1.18× instead of 1.5×, 3.05dp instead of 3.6dp).

### v444 — Edit profile is a PAGE, and it is the member's own spec

- **THE IDENTITY EDITOR IS A SCREEN, NOT A DIALOG (`ProfileEditScreen`, `CurioRoutes.PROFILE_EDIT`, `CurioNavHost`).** The member: *"instead of dialog box make it a full screen with this style"*, with a written specification (editorial · calm · minimal · tactile; theme-aware plain background; **no gradients, no glass, no decorative cards**; a small way back over a large quiet title and one supporting sentence; the picture centred with a small camera disc on its corner and exactly two compact actions beneath; NAME and BIO as **open fields with a hairline under them** rather than boxes inside boxes; one flat ACCOUNT section; ONE tappable privacy row; Cancel / Save changes held at the foot, quiet and solid, same height and radius; nothing every-heading-bold; an **8dp spacing base where important things get more SPACE instead of another card**). Every number on the page is in one place — `EditSpace` (8/16/24/32/40) — so the ruler is the spec's and not re-invented per row, and the section headings are small, spaced and `onSurfaceVariant` because the spec's rule is that Curio should read quiet, not corporate. The page is a PLAIN route behind the profile page's own prefix (`profile/edit`), which is what gives it the same arrival the profile page has and no bottom bar; the dialog, its `ProfileDialogs` composable and its two pill helpers are gone, and every "Edit profile" door on the profile page (the torn hero, the glass bar's action pill, the compact identity bar) navigates here.
- **THE HANDLE IS CLAIMED WITH SAVE CHANGES, AND THE TERMS ARE THE ONE DIALOG THAT ALREADY EXISTS (`commit()`, `claim()`, `CurioTermsDialog`).** The member, asked how the old card's self-contained username field should fold in: *"fold it into save changes, and the username claim auto checks"*. So the field is an ordinary open field that validates AS IT IS TYPED against the same rule the server enforces (`[a-z0-9_]{3,24}` plus `CurioContentFilter.problem`) and shows one line only while it has something to say — the rule being broken, the server's verdict, or why a claim would be refused (signed out). **Save changes writes the name and the bio first (local, then mirrored best-effort, so an offline save is never blocked) and only then claims a CHANGED handle** — the one part of this page that has to wait on the server, which is why it is the only part that holds the save open and the only part whose refusal is reported on the page instead of swallowed. A first claim needs the terms accepted, and the terms are **the same dialog the account card shows** (`CurioTermsDialog` is `internal` now — never a second copy of the disclosure text); the claim finishes the moment it is accepted (`pendingClaim`).
- **THE PICTURE MOVED WITH THE PAGE, AND THE PROFILE SCREEN KEEPS ONLY WHAT IT DRAWS (`saveAvatar`, `decodeAvatarSource`, `centerSquareCrop`, `scaleToMax`, `avatarPath`).** The whole photo pipeline — pick → EXIF-corrected bounded decode → centre-square crop → the crop editor's rect → both files saved — lives on the editor now, and `ProfileScreen` keeps only the avatar PATH the hero wears, re-read on every entry into the page (and on `ON_RESUME`), so a picture added one screen away is on the hero the moment the member comes back. **What the dialog could do, the page still does:** pick/change the photo, wear the blob instead (`AppPreferences.setProfileAvatarBlob`), **remove the photo** (the ⋯ in the expanded picture — tapping the picture is what opens it), review the terms, open Privacy, and sign out (bottom of the page, with the existing confirm dialog). Signed OUT, the ACCOUNT section is **one calm row** to `CurioRoutes.SETTINGS_ONLINE` rather than a form squatting inside a page.

### v443 — a scroll is not a scrub, the dictionary's 404, the journal's paper, and the dock's own switch

- **A SCROLL OVER A CONTROL IS NOT THE CONTROL (`ReadingGauge`'s wear-in, `claimed`).** The member: *"fix the reading progress accidental touch"*, and, asked what should stop it, they chose **"a scroll over the bar must not move it"**. The gauge consumed the DOWN and treated ANY movement as a scrub, so a finger running down the page that happened to pass over it (a) moved the reading place and (b) — because the down was consumed — **stopped the page from scrolling either**. One false move cost both gestures. It now **wears in** like the reader's own magnified page (`pinchToZoom`'s `travelled`): nothing is taken and nothing is consumed until the finger has crossed the touch slop **SIDEWAYS** (`dx > slop && dx > dy`), and a finger that crosses it DOWNWARD has announced that it is scrolling, so the bar steps out of the whole gesture and never moves. The knob is lit by the CLAIM, not by the touch, so a scroll over the bar shows nothing at all. **A press that never travels is still a tap** and still seeks on the release. Rule: a control that lives inside a scrollable takes NOTHING until the axis is decided — consuming the down is what makes it steal the scroll it sits in.
- **A 404 IS AN ANSWER, AND AN EMPTY FIELD IS NOT A FAILURE (`ReaderDictionary.Fetch`, `ReaderLookup`, `ReaderDictionarySheet`).** The member: *"also the dictionary wasnt working"*, and it was, in one specific way: **every word Wiktionary does not carry answers HTTP 404**, the old getters turned any non-200 into `null`, and `null` is the sheet's own word for UNREACHABLE. So a rare word, a name or a misspelling was reported to the member as a dead network — and the v442 spelling suggestions could **never run at all**, because they are only asked for after an EMPTY answer, which the 404 path never produced. The GET now keeps its STATUS (`Fetch(code, body)`, code 0 = the call never happened) and `define()` answers **404 → `emptyList()`** ("no such headword": a real answer, the one that puts "Did you mean" on screen), **anything else non-200 or a thrown call → `null`** (unreachable), and a term that is not a word at all → `emptyList()` (nothing was asked, nothing was found — the contract's own line). The sheet carries **three states instead of one nullable list** (`ReaderLookup.Idle` / `.Answer(senses)` / `.Unreachable`): one `List?` meant both "not asked yet" AND "unreachable", so the first frame of every sheet flashed *"The dictionary could not be reached."* and a blank field sat on that message for good. **Never collapse "not asked", "no such word" and "the source is down" into one value again.** The miss path also stops at the first `null` — the source is not answering, so four more neighbours would only buy four more timeouts before the same sentence.
- **THE PAGE NEVER PAINTS ITS PAPER, AND THE OPTION IS GONE (`JournalPagePaint`, `journalPaper`, `JournalAccentSheet`).** The member: *"in journal the paint the page remove that option"*, and, asked what removing it should do, **never paint the page**. This box has been both ways — v429 grew "Paint the page too", v439 withdrew it, v440b restored it — and the answer is now the other one for good. `JournalPagePaint` lost its `painted` flag, `journalPaper()` is the theme's parchment with the app's accent whisper (0.10 dark / 0.05 light) whatever colour the page was given, and `journalInk()` is simply the theme's `onSurface` because the paper it was written to answer for cannot happen any more. What a member's colour DOES reach is unchanged: the page's DOORS (`journalDoorAccent` — Home's chips, the list's spine, the palette door) and the tint the page's own controls wear (`journalPaperRaised`). **`PersonalNoteEntity.pagePainted` stays stored and stays written** — the writer rebuilds the whole row from `PersonalPageMeta`, so dropping the field would erase a member's earlier answer from their own file on the next keystroke; the app simply no longer offers it. A Room column is a migration, never a UI change.
- **THE DAY IS CHROME, AND IT KEEPS THE THEME'S COLOUR (`JournalTopBar`).** The member: *"dont color the today area"*, after changing a page's colour and finding the pill hard to read against it. v437 had handed this bar the page's own colour so the day pill and its date steppers wore it; the bar now asks for `journalDoorAccent(JOURNAL_ACCENT_THEME)` — the measured theme accent, i.e. exactly what an uncoloured journal's pill has always been — and no longer takes an `accentArgb` at all.
- **A COLOUR SOMEBODY CHOSE IS MEASURED, NOT GUESSED (`journalInkOn`, `journalOn`).** The member: *"make the coloring smart so that changing color automatically adjusts the text color as well so the date pill or anything else doesnt get the weird unredable text"*. `journalOn(fill)` **forwarded to `settingsReadableInk(fill)`, which answers from the THEME** (a named theme's own `onPrimary` pair, the pastel flag, the light/dark branch) **and never looks at `fill` at all** — so the one case the function exists for was the one case it got wrong, and a member's dark colour under the theme's dark ink was a pill nobody could read. The measurement is now `journalInkOn(fill)` — a PLAIN function (it is asked for from a draw pass and a gesture as well as from a composition): **`fill.luminance() > 0.55f` → the journal's near-black, else its cream.** `PersonalCanvas.readableOnFill` — which already had the right rule and was the one place that did — delegates to it now, and the colour sheet's own tick asks it instead of the theme. **There is one rule for ink on an arbitrary fill and this is it; never answer a chosen fill from a theme role.**
- **BOTH LETTER ARROWS ONLY EVER ADD (`PersonalEditorState.nudgePageLetters`, `canNudgePageLetters`).** The member: *"for the copy arrow the behaviror is unexpected fix it"* → *"kee adding letter never walk shrink"*. → used to **WALK** the window along the row once it reached the row's end, and ← **SHRANK** it from the right (and walked it the other way at a single letter) — so between them the two arrows could never hand the member more than the one character they started with, which is exactly the report they filed: *"side arrows … selecting only one letter at a time"*. Each arrow now does the one thing it says: **→ takes the letter to the RIGHT of the window, ← the letter to its LEFT**, and the window only grows. Either arrow may OPEN a fresh window (← used to refuse — "nothing to give back" — which left half the control dead once both only add), so `canNudgePageLetters` is live for both on an empty reach, and an arrow is dead only at the row's own edge. A wrong reach is started over by a ROW arrow, "Line" or "Select all", all of which reset `pageCharRange` — never by an arrow that silently moved it. The `ReachPill` descriptions changed with it ("One more letter to the left" / "…to the right"); the box's own cross STAYS.
- **THE READER'S DOCK IS ITS OWN SWITCH, AND ITS × IS GONE (`ReaderSelectionBar`, `appliedInk`).** The member: *"for the hihgligh selecter remove the frst x and when tappin git again the color it should deselect"*. The bar's four inks were a 35% wash of themselves whether the passage wore them or not, so a marked passage's own colour looked exactly like the three it did not, and a second press re-wrote the same mark. **The ink in hand is drawn as TAKEN** (opaque, a 2dp rim, a `Check` in the ink that reads on it) and **pressing it REMOVES the highlight** — the caller (`BookReaderScreen`) resolves the passage's own mark from `marks` and deletes it instead of writing; any other ink is a new mark on the path it always was. With that in the row the × ("Clear the selection") is redundant and gone: the ⋯ door is the way to everything else, and a tap on the page puts the dock away (`tapPage` clears a selection first). The making sheet's own colours (`ReaderMarkSheet`) already worked this way — the selection bar now matches it.
- **ONLY THE PICTURE PILL WEARS A PICTURE (`TopBarPill.showingPicture`, `hasOwnPicture`).** The member: *"the drawer menu icon on home screen is getting the profile pic"*. The pill drew the member's face whenever `hasOwnPicture(avatarPath)` was true, and **a member wearing their BLOB as their picture has no photo path at all** (`AppPreferences.profileAvatarBlobState`) — so `hasOwnPicture(null)` is TRUE for them and the drawer's hamburger, which was handed no path, drew their face instead of its own glyph. **The path is not the test**: the pill now SAYS which of the two it is (`showingPicture = true` from the profile pill alone), which is the same correction v439 made at the other end of this rule ("every surface must ask `hasOwnPicture` before choosing between the picture and its own initial") — ask it where a picture is WANTED, never assume it where a glyph is.

### v442 — nine fixes on the reading surface: the taps, the lock, the lookup, the sheet and the dock

- **A LOCKED PAGE STILL HEARS A TAP, A HOLD AND A SWEEP (`pinchToZoom`'s `held`).** The member: *"the zoom lock is bad it also locks the touches fix it"*. The motion lock consumed EVERY event in which any finger had moved at all (`it.position != it.previousPosition`), and a finger that taps or holds is never perfectly still — so the first pixel of jitter was consumed, and a consumed move cancels `detectTapGestures` (which drops a tap the moment a tracked change is consumed, and does the same to a pending long press). On a locked page the chrome could not come back, the mark dock could not open and a sweep reported nothing. **The lock wears in like every other surface this file already has: nothing is consumed until the finger has travelled the touch slop**, so a tap, a hold and a selection are left alone while a real drag or a second finger is still swallowed whole. Consuming is what keeps a locked page still — a drag merely IGNORED would be taken by the column or the pager underneath and the page would move anyway. The two traps v439 recorded still stand: never consume the first down, and keep the flag in `ReaderLook.rememberKey()`.
- **THE SIDE TAP HAS ITS OWN HANDLER, AND IT CONSUMES THE UP IT ANSWERED (`Modifier.readerZoneTaps`).** The member, one breath, three symptoms: *"the side tap gesture its slow doesnt work faster receives one ta and doesnt work anymore and doesnt work sometimes fix it"*. One cause, and it was never the zone arithmetic: the zones were answered by the reading surface's own `detectTapGestures`, which ALSO owns the double tap (`readerDoubleTapZoom`) — a detector waiting to see whether a second tap follows cannot answer the first until the double-tap window has passed (**slow**), and a second tap inside that window was read as the FIRST HALF of a double tap and zoomed instead of turning the page (**one tap and then nothing**); where a child claimed the gesture first, nobody heard it at all (**sometimes**). The zone handler is placed **INNERMOST** in its chain, so it processes the lift before the surface's own detector, and it **CONSUMES that up** — which is what cancels the double-tap wait: one tap, one page turn, never a zoom. Three guards make it a tap and only a tap: a down a real control already consumed is skipped (`awaitFirstDown()`'s own `requireUnconsumed`), a press held past the long-press threshold is the SWEEP's (dropped on the timeout, not on the lift), and a gesture something else consumed mid-flight never reaches the lift (`waitForUpOrCancellation` answers null). It is wired into all four surfaces — the reflowed text, the PDF column, the PDF page and the paged flow — each passing the space its taps are really measured in (a PDF column rides a horizontal scroll, a sheet's frame is the DOCUMENT's ruler), and its `key` is deliberately NOT the translation function: a handler keyed on a value that moves while the finger is down would cancel a tap in flight, which is its own "doesnt work sometimes".
- **A SHEET LEAVES ON A FLICK TOO (`flickPeak`/`flickAt`, `dismissFling`, the 96dp pull).** The member, again: *"still the buttom sheet closing is bad"*. Two things were wrong and neither was the clock: **a flick did not count** — the only door out was DISTANCE (108dp of deliberate dragging), so the gesture everyone actually makes on a sheet dragged a few millimetres and sprang back — and the distance was a long way to pull for a panel a few rows tall (96dp now). The speed is read from the finger's OWN CLOCK (the nested scroll reports travel and no velocity) and the PEAK is what counts, because a throw is fastest in its last frame; the head's drag measures its own. **There is deliberately no `onPostFling` override here** — the velocity handler is a second, engine-versioned way to learn what the finger's clock already says, and the reader does not need two. The speed lives in plain `floatArrayOf`/`longArrayOf` holders, not Compose state: nothing draws it, and a state write per frame of a drag is a recomposition for a number the eye never sees.
- **THE DICTIONARY READS THE PASSAGE (`ReaderDictionary.headword`, `.suggest`, `.wordsIn`, `readerContextFor`).** The member: *"imrpove the discoonary that it suggest work explanation from the selected para, make the word detection better it detects the work even theres a comma or something or a mis type"*, and, asked how, "chips + context line". Three changes, one file: **(1) `headword()`** strips any run of punctuation off BOTH EDGES (both quote families, both dash families, brackets, the sentence's punctuation) plus the possessive tail — so `"Einstein,"` is asked for as `Einstein` — while never touching the hyphen or the apostrophe INSIDE a word, which are letters' business (`well-known`, `don't`); the stem minimum is three letters so `it's`/`he's` keep their own page. `define()` asks for the headword, so a comma can no longer make a real word look unknown. **(2) `suggest()`** asks Wiktionary's own `opensearch` — the same keyless family the definitions come from, so no second service to trust — for the nearest spellings, offers only word-shaped answers, never echoes the ask back as a suggestion, and memoises misses like the senses. **(3) `wordsIn()`** offers a PASSAGE's notable words (a sentence split, five letters up, `STOP_WORDS` out, never twice) with the sentence each stood in, which the sheet draws as chips and quotes beside the meanings. The sheet's `initial` is deliberately EMPTY for a passage: seeding one of its words would be the reader guessing which word the member meant.
- **THE HIGHLIGHT DOCK WEARS THE READER'S OWN PILL BODY (`ReaderSelectionBar`).** The member: *"the highlight dock is bad fix it too. weird shado and doesnt match the dock"*. Its fill was `palette.surface` (F5F0E8) over `palette.paper` (FBF6EC) — about two per cent apart — so the only part of the capsule the eye could see was a shadow spread over pale paper on every side: the exact smudge v441 fixed on the page slider, one surface over. It is an **OPAQUE** `lerp(surface, ink, 0.06f)` body with a hairline `lerp(surface, ink, 0.16f)` edge, the same 28dp radius and 8dp lift the slider wears; being opaque, the shadow cannot bleed through (the AGENTS rule about `color.copy(alpha = …)` under an elevation). `animateContentSize` is gone with it — the bar is full width and its height never changes, so it animated nothing and asked for a layout pass on every arrival.
- **THE HEAD HAS ONE EXIT PER REASON (`ReaderChrome`, `CurioMotion.pillArrive`/`pillLeave`).** The member: *"theupper header animation is clanky"*. Its exit carried THREE transitions at all times — fade, drift and a sideways `shrinkHorizontally` toward the End — so simply HIDING the chrome (a tap on the page, the commonest thing in the reader) collapsed the name capsule into the corner WHILE it was leaving upward: two motions for one intent, and the one gesture where the head should just go away was where it looked busiest. **The shrink belongs to exactly ONE moment — the search opening**, where it is what makes the bar read as growing out of the corner the search icon lives in — so the exit is chosen by whether the search is opening, and that branch runs on the ENTER clock (the clock the bar arrives on), making the hand-off one movement rather than two panels changing places. The head and the foot are written as `pillArrive()`/`pillLeave()` tokens now instead of their numbers.
- **THE ⋯ SHEET IS SIZED TO ITS OWN TILES (`ReaderSheetFrame.minHeightFraction = 0.30f`, `ReaderTileRow`).** The member: *"the 3 dot for reader its too empty spae and not proper spaced, fix its weird look"*, then: *"scrink it but dont make it too lose to th buttom"*. v437's 45% floor is right for a LIST — a list grows, and a floor is what stops two kept marks reading as a broken panel — but the ⋯ grid is six FIXED tiles and is the same height whatever happens, so 45% of the screen was two thirds empty paper. **A floor is for a list, never for a fixed grid**; the tiles also got a real gutter (10dp; at 8dp two 46dp pills nearly touched once their labels were the widest thing in the row), 14dp between the rows, and a breath under the last one.
- **THE NIGHT DIM'S WINDOW IS THE MEMBER'S (`ReaderLook.dimFromMinute`/`dimUntilMinute`, `dimWindowContains`, `ReaderClockRow`/`ReaderClockDialog`).** The member: *"add at sunset customisation to be able to set the tiem"*, and chose **two times, from/until**. v440 answered the schedule with the phone's dark theme (no location permission ever needed) but that answers only "is it dark out", so the page a member is reading at 19:00 in winter stayed bright. **A window that RUNS OVER MIDNIGHT is the normal case** (on at 20:00, off at 06:00), so `dimWindowContains` reads it as `>= from || < until`, and **equal ends mean "all day"** rather than a zero-length window that would silently switch the dim off. Both new fields are in `rememberKey()` AND in the store (`reader_dim_from`/`reader_dim_until` — the v434 rule), and the reader TICKS its clock every 30s while the mode is on: a member reading at 19:59 with the dim due at 20:00 would otherwise keep a bright page until something else recomposed the screen, which with the chrome gone can be minutes. The row, the label and the picker are `internal` HERE and worn by both surfaces (the appearance sheet and reading settings), so the two can never disagree about what "at sunset" means.

### v441 — the page slider keeps up with a fast hand, and reads as a pill

- **A STEP IS TAKEN FROM THE PAGE THE LAST STEP WAS SENT TO (`stepFrom`, `stepLedger`).** The member: *"next and previous button doesnt work on rapid click only goes 1 and stops working"*. Every arrow and zone computed its target from a place that is only true once the turn has FINISHED — `pagerState.currentPage`, `shownPage`, `listState.firstVisibleItemIndex`, `textPager.currentPage` — so four quick taps all computed the SAME next page and then re-asked for the turn the first one had already started, which is why the arrows answered once and then went dead until they settled. `stepLedger` holds the hop the reader is on (where the last step started, and where it was sent) and a tap steps from the DESTINATION whenever the reader is still standing on either END of that hop — settled or in flight. **The two-end match is the whole safety property and must not be widened into a range:** a step hop is one page, so there is no integer strictly between its ends, while a range would let a chapter or mark jump that happens to land inside an old run of taps silently resume from the run's own end. Anything else the member does — a scrub, a chapter, a mark, their own scroll — matches neither end, so a stale hop can never send the arrows somewhere they are not. It is a plain `IntArray` (nothing in composition reads it) and it is declared ABOVE `stepPage`, because a local function in Kotlin cannot reach a local declared later in its own body. **The same defect lives one layer up and is fixed with it: `ReaderHoldButton` was built once with `pointerInput(Unit)` and would have kept calling the closure it was first built with forever** — its `step` is read through `rememberUpdatedState` now, the same remedy this file already uses for hoisted state.
- **THE PAGE SLIDER IS A PILL, NOT A SHADOW (`ReaderScrubPill`).** The member: *"page slider ui is bad with tha weird shadow"*. Its fill was `palette.surface` (F5F0E8) floating over `palette.paper` (FBF6EC) — about two per cent apart — so the only part of the capsule the eye could see was its shadow, spread over pale paper on every side. The fill is an **OPAQUE** `lerp(surface, ink, 0.06f)` with a hairline `lerp(surface, ink, 0.16f)`, the lift is the pinned page's 8dp rather than the reader's largest number, and `animateContentSize` is gone (the pill is full width and its height never changes, so it animated nothing but a layout pass per frame of every arrival). **The count is a FIXED 72dp slot, right-aligned** — it used to be whatever width its digits needed beside a slider that held the `weight`, so every time the number gained a digit the track narrowed, the thumb moved with it, and the page under the member's own finger changed for no reason they could see. A control whose readout resizes the control is a bug in every slider, not just this one.

### v440 — the journal becomes findable, and the gestures box can get out of the way

- **A JOURNAL ROW NAMES THE MOMENT IT WAS WRITTEN, AND THAT MOMENT NEVER MOVES (`PersonalNoteEntity.writtenAtMillis`, `Long.prettyTime`).** The member, in one breath: *"for journal ad time note too its only note date"* and *"in journals view dont update the time if its edited again late"*. The two halves are one rule: **a list of days shows when each day was MADE, not when it was last touched.** `createdAtMillis` is stamped once by `saveNote` and preserved on every later write, while `updatedAtMillis` moves on every keystroke — so a page written at nine in the morning and corrected at midnight would otherwise claim midnight, and the whole list would reshuffle itself around whichever page was edited last. `saveNote` already had the right contract (it only fills `createdAtMillis` when it is 0), so this is UI and ORDER only. **No SQL change and no migration:** `writtenAtMillis()` falls back to `updatedAtMillis` for rows written before v389 stamped a creation time, and the DAO's tiebreaker is `COALESCE(NULLIF(createdAtMillis, 0), updatedAtMillis)` for exactly the same reason — a row with no creation stamp must still sort by the only stamp it has. The time rides the metadata line beside the word count rather than the date column: the day is what the column is for.
- **SEARCH IS A PILL THAT BECOMES A FIELD (`JournalSearchPill`, `JournalListScreen`), AND IT NEVER DECODES A DOCUMENT (`PersonalNoteEntity.answers`).** The member: *"add search for journals"*. Closed it is one quiet pill under the head — a collection nobody is searching should still read as a list of days rather than as a toolbar — and tapping it makes the pill BE the field (the same trick the reader's own search plays on its head, on the same `CurioMotion` pill clock), taking focus because a member who tapped it has already said they are going to type. `answers()` reads **only what a row already shows** — title, stored `preview`, topic name, mood label, and the page's own date line — and **`doc` is deliberately not touched**: `PersonalDoc` re-parses its JSON on every access, so a search that read bodies would parse every page in the collection on every keystroke (see `JournalRow`'s own note, which reads `doc` once per version behind a `remember`). A search that stutters on a few hundred days is worse than no search. The empty result is **not** a bare dash: a filtered list has a CAUSE, and "Nothing matches" with the query and a count of how many of how many pages is the member's only evidence the search is working (see `CurioEmptyLine`'s rule).
- **THE DATE PILL ORDERS THE LIST (`PersonalHeaderDate(onToggleSort, newestFirst)`).** The member: *"sorting by date by tapping the date in journals date"*. The head's date is the one date a journal is about, so it is the natural door for the order of the collection: a tap reverses it and the pill's own arrow says which end is at the top (down = newest first, which is the default and what the list has always been). The shelf's header leaves `onToggleSort` null and keeps the plain label it always had — which is why the pill uses `enabled = onToggleSort != null` rather than an `if`: **a disabled `Surface` takes no presses and draws no ripple, so one composable serves both a label and a door.** The order is applied in the SCREEN (`sortedWith`, tied on `writtenAtMillis()`) rather than in a second DAO query, so reversing it costs no database trip — and the month grouping follows the sorted list, so the months reverse with the days (`groupBy` keeps insertion order).
- **A PAGE PUSH HAS ITS OWN CLOCK, AND IT IS NOT `Deliberate` (`CurioMotion.Durations.Push` / `.Pop`, the nav host's four generic branches).** The member: *"the animation open animation of journal is clanky"*. **The journal was not the cause** — every plain forward navigation (the journal editor, a chapter, a book, a profile) falls into the nav host's generic branch, and that branch glided the new page in over **500ms** with the outgoing page drifting for the same half-second behind it. Half a second is the app *thinking*, and it is what a member reads as clunky. The travel was never wrong (1/6 of the width in, 1/8 out): only the tempo. **260ms in, 220ms back**, with `Deliberate` left for what it was written for (a change worth watching — never a screen appearing). Rule: when someone says a screen's ANIMATION is clunky, measure the DURATION of the generic branch before touching the screen.
- **THE PILL CLOCK GOT CRISPER (`ENTER_MS` 220 → 190, `EXIT_MS` 140 → 130).** The member, after living with one clock: the furniture still read soft. A tool that appears UNDER THE THUMB (the journal's dock and copy box, the reader's selection bar) is not a thing the eye needs to watch arrive — the member has already decided to use it, and every millisecond past ~200 is a beat they feel as the app catching up. **The curves and the one-clock rule are unchanged; only the tempo moved.** If it is ever wrong again, move these two numbers — never add a second clock for one surface.
- **FILTERING, AND ONE COUNT THAT MUST BE PAID FOR ONCE (`JournalLength`, `JOURNAL_ANY_COLOUR`, `lengths`, `JournalFilterChip`).** The member's own pick: *"Filter the list by mood, colour or length"*. A filter door sits at the END of the search's own row — not on a row of its own, because a second strip of chrome over a list of days is exactly the "two stacked rows" they rejected in the writing dock — and opens three capsule rows, each led by its own "any" chip so the way out is where the way in was. **Mood and colour are COLUMNS the row already carries** (`mood`, `accentArgb`), so they cost nothing. **Length is not**, and this is the load-bearing note: a word count lives inside the page's own document, and `PersonalDoc` re-parses its JSON on every access — so the counts are taken **once per (list, chosen bucket), on `Dispatchers.Default`, and only while a length filter is actually on**. Never move that count into a row or a recomposition: a few hundred days is a few hundred JSON parses, which is a stutter the member would feel on every keystroke of the search beside it. `JOURNAL_ANY_COLOUR` is `-1` because **0 is a real choice** ("Theme" — a page that follows the app's accent, see `JOURNAL_ACCENT_THEME`), and a filter whose "any" is 0 could never select it.
- **THE NIGHT DIM CAN FOLLOW THE PHONE (`ReaderLook.dimAuto`, `nightDim`).** The member: *"Night dim on a schedule (auto at sunset, not just manual)"*. **Android has no sunset to ask for**, and computing one needs the LOCATION — a permission this app holds for nothing else — while the phone already knows: the system's dark theme is what a phone set to automatic switches at sunset, and `isCurioDarkTheme()` is that switch (system-dark when the member's theme is "System", their own choice when they forced one). The row is a two-way choice ("Dim always" / "At sunset") and **the default is "Dim always"**, so every member who had a dim keeps exactly the dim they had. It is read in the COMPOSABLE scope (`isCurioDarkTheme` is @Composable — never inside a lambda) and it is in `rememberKey()` and the store, or it would save every other look field and silently forget this one.
- **PAGES LEFT IN THE CHAPTER, FROM WHICHEVER SURFACE KNOWS IT (`pagesLeft`, `pagesLeftInChapter`, `onSectionPagesLeft`).** The member's own pick from the settings list. There are TWO ways to know it and the code says so rather than pretending one: a book with pages answers from its own outline (`ReaderOutlineEntry.page` — the chapter's pages run from its first page to the page the next chapter opens on, or the book's last page), while a reflowed book's pages are the paged reader's own slicing, so **`TextPagedReader` reports the count UP** (it is the only place holding the page ranges) by finding the last range that holds a block of the current section. A flow with no pages leaves it `null` and the card **says nothing** — a made-up zero is worse than a missing line.
- **A WRITING GOAL, AND A NUDGE THAT IS ITS OWN ALARM (`AppPreferences.getJournalGoal`, `JournalGoalReminderScheduler`, `JournalGoalReminderReceiver`, the journals head's rail).** The member: *"Word count goal with a daily reminder"*. **0 words means no goal** — and switching the goal off disarms the nudge with it, because a nudge about a goal the member no longer has is what makes people turn a notification off for good. The goal is a PREFERENCE, never a column: the pages it judges are stored exactly as before, so **no SQL change and no migration**. **The receiver deliberately does NOT read the database** — a broadcast can arrive in a cold process and the personal store is initialized by `MainActivity` and fails loudly when it has not been (see `PersonalRepositoryHolder`) — so the nudge names the goal it reads synchronously from preferences and lets the APP show the day's progress. It is a SEPARATE alarm with its own request code and receiver, so cancelling one nudge cannot cancel the other; and `DailyReminderBootReceiver` re-arms it, because a reboot drops every alarm and a goal that stopped nudging after a restart reads as a broken setting.
- **THE JOURNAL'S DOOR HAS TWO HONEST POSITIONS (`AppPreferences.isJournalOpenToday`, `AppPreferences.getLastJournalId`, `rememberJournalDoor`).** The member: *"'First page of the day' preference (today's page vs the last one you opened)"*. **Today is the default and the behaviour that already existed**, so nobody's habit changes. In that mode the door opens the day ALREADY WRITTEN if there is one — tapping "+" to add a line to this morning's page used to make a SECOND page for the same day — and only writes a new page when the day is blank. The lookup is the query the list already collects (`observeJournals`), so nothing new reads the database, and the id is recorded by the EDITOR as a page loads — never when one is created, which has no id until it is saved. The door's coroutine is launched from an onClick, so the `LocalContext` is hoisted above it (the v439 rule) and the read is dispatched.
- **THE GESTURES BOX CAN STAND DOWN, THREE WAYS (`ReaderTapZoneEditor`'s `panelUp`/`adjustingZone`/`washesUp`, `ReaderZoneHandle.onAdjust`).** The member: *"the gesture box hide that when adjusting area and a way to hide that box not the backgroud thing, and a way to make it appear again to edit"*. `v434`'s eye only ever reached the WASHES — the box itself stayed across the foot of the screen, which is exactly where the member is looking while aiming an edge at real lines of text. So the panel answers to three things now: the eye (the washes), **a finger placing an edge** (`ReaderZoneHandle` reports drag start/end/cancel as `onAdjust` — the box leaves for the drag and comes back on release), and **a collapse door** on the panel itself, which puts it away for as long as the member wants the page to themselves. **A hidden box with no way back is a trap**, so standing down leaves a small "Gestures" pill in the bottom-right corner that opens it again — and every one of those movements is `CurioMotion.pillArrive()` / `pillLeave()` / `popArrive()` / `popLeave()`, never a local duration. The GRIPS still stay through all of it: they are the handles being placed, not the thing covering the page.

## Child DOX Index

- [`CURIO_DATA_PLAN.md`](CURIO_DATA_PLAN.md) — Canonical **data layer** spec. Owns: category taxonomy expansion (6 → 10), `CurioTopic` + `ExploreAction` schema, JSON-on-disk canonical format, Room DB seed flow, image strategy (URL + Coil, no bundling), authoring pipeline (LLM-draft + human-review + smoke test), per-category rollout cadence (one category per PR, Music first). Read this BEFORE adding any topic data, category entry, or capture-format prompt.
- [`src/main/assets/topics/SCHEMA.md`](src/main/assets/topics/SCHEMA.md) — Quick-reference schema doc for topic JSON files. Lives next to `music.json` so authors have the schema at their fingertips without opening the larger `CURIO_DATA_PLAN.md`. Points back to the full source-of-truth for anything not covered.
- (Future) `app/src/main/java/com/curio/app/features/{home,spin,cabinet,capture}/AGENTS.md` — per-screen feature contracts, added when each screen gets real implementation in Phase 3+.
- (Future) `app/src/main/java/com/curio/app/ui/theme/AGENTS.md` — design system primitive contracts, added when the theme system grows (Phase 3+ when dark-mode polish, motion tokens, etc. land).
- [`src/main/java/com/curio/app/data/supabase/AGENTS.md`](src/main/java/com/curio/app/data/supabase/AGENTS.md) — the online layer's contract: Supabase client, session store, Online Mode state and the credential rules (public keys only).
- (Future) `app/src/main/java/com/curio/app/data/AGENTS.md` — data-model contracts, added when Room + repositories land in Phase 4.
