# Prompt Log — current request

## Request (2026-09-11, in progress — Cabinet loading skeleton + settings rail label + collections redraw)

User (queued prompt): "the cabinet loading is still kinda bugged the saved
entryies now open fast but cabinet opening is bad, also use a skeleton system
maybe for accurate amount exactly the number of saved entries are there like
that..and redraw the collections cutsom ones and curying now one do it
properly and also the settings nav rail the indicator istelf does goes fast
and its better but the text disappers for a momemt for the acive indicator
and it slowly comes to a rest so ix that". Earlier in the same thread: "do
what u can do then after doing it ask me questions and doubts".

**Diagnosis**

1. **Cabinet opening.** The V2 Cabinet home treated "entries is still empty"
   as "you have nothing", so a cold open flashed the
   suggestions/empty state and then swapped in the real shelves — the same
   class of bug the classic Cabinet already guarded with `entriesReady`, but
   V2 had no such gate for its home/shelf levels. And the loading state it
   did have was four generic full-width boxes, nothing like the 2-column
   card grid.
2. **Settings rail.** The active chip's label is cream; the chip painted NO
   fill of its own (the accent pill was only the shared-element overlay, and
   that instance is hidden while the glide runs) → cream on the pale
   frosted tile until the pill landed. The 0.9-damped spring also overshot,
   so the pill visibly "came to rest" after arriving.
3. **Collections redraw — AMBIGUOUS.** "custom ones and curying now one do it
   properly" has several readings (the collection pages' member layout, the
   collection CARDS on the Cabinet home, the empty state, or the untracked
   `Curio_WantToRead_EmptyState.jsx` concept). Per the root AGENTS.md
   ask-when-unsure rule (and the user's own "then ask me questions"), the
   redraw is queued as a question instead of guessed at.

**Shipped (5 code files + docs, this commit):**

1. **`ui/components/CurioSkeleton.kt` (NEW)** — the app-wide skeleton
   system: `CurioEntrySkeletonCard` (20dp radius, 96dp hero header, two text
   bars, shimmer read in the draw phase so it never recomposes) and
   `CabinetEntrySkeletonGrid(count, topInset, wide)` which mirrors the real
   grid's columns/paddings/gaps.
2. **`AppPreferences.kt`** — `getCabinetEntryCount` / `setCabinetEntryCount`
   (key `cabinet_entry_count`): the last known saved-entry count, so the
   skeleton is sized to the real archive on the next cold open.
3. **`CabinetV2Content.kt`** — `archiveReady` (false until the first
   `observeLight()` emission) gates `showSuggestions`, and the home's
   Saved-entries slot plus the Saved entries / Notes shelves hold
   `v2SkeletonItems(skeletonCount)` until then; the count is persisted for
   the next launch.
4. **`CabinetScreen.kt`** (classic) — the four wide boxes became
   `CabinetEntrySkeletonGrid`, sized from the persisted count.
5. **`SettingsHubScreen.kt`** — the active chip paints `SettingsRailAccent`
   in its own layer (identical to the gliding overlay, so the glide is
   unchanged once it lands) and the bounds spring is critically damped
   (1.0 / 420) so the highlight lands and stops.

**Open questions for the user (asked at the end of this turn):** what
"redraw the collections ... do it properly" should change; whether the
built-in starter shelves should keep their destructive "Delete collection"
menu entry (deleting one currently loses its members for good); whether the
collection pages' member list should become the concept's row list.

**Status:** committing + pushing now; CI validates.

## Request (2026-09-11, completed — screen-reveal transitions toggle, packed Cupboard shelves, .jsx text-history tree, caption box + cover-true album sheet)

User (queued prompt + ask_user answers): test results "Haven't built it yet";
the album sheet's colours are wrong "from the album's real cover art"; the
Cupboard wall leaves empty space ("the first image is good and then to its
side there are 2 things placed but then theres space left below those 2 so
properly fix the spaces"); text history should match the concept `.jsx`
("Tree view layout, Version cards' content, Replace Changes with Compare,
also make the current text at the top … show its changes in the top"); the
caption field "is note paper style change it to just a text box"; the book
sheet's add-note box + text colours are still wrong in dark mode; and
"similar to the dark mode and light mode transition cant we use that for
like settings or profile open, and the screen changes in that transition
style, make it a toggle and also make it faster a little" (plus a wider
animations/haptics wish-list logged as pending).

**Shipped (7 code files + docs, this commit):**

1. **`navigation/CurioRevealNav.kt` (NEW) + `ThemeTransition.kt` +
   `CurioNavHost.kt` + `AppPreferences.kt` + `ExperimentsScreen.kt` —
   SCREEN REVEAL experiment (Settings ▸ Experiments, default OFF). The
   theme flip's feathered iris now opens screens too: the frame is captured
   on pointer-DOWN (`trackRevealTaps()`), stashed, and played from the
   NavController's destination listener via the new non-suspend
   `startTransitionWithFrame`; while it owns a navigation the NavHost
   returns `EnterTransition.None`/`ExitTransition.None` so the iris is the
   only motion. 440ms (vs the flip's 680ms) + a 32ms settle, both restored
   after the reveal. Shared-element routes (`reveal`, `pet-designer`) opt
   out; no fresh frame = the normal transitions, untouched.
2. **`CabinetV2Content.kt` — Cupboard wall packed into shelves.**
   `BoxWithConstraints` measures the wall; `buildCupboardShelves()` packs
   covers into rows at one shared height (width = height x the cover's own
   aspect), so widths + gaps fill the width exactly and no hole is ever
   left. The 8-column grid span cycle + `mediaTierIndex` are gone.
3. **`ui/components/TextHistory.kt` — the `.jsx` tree.** `LineageRail`: one
   rail, each version a node card, newest first (current at the top with
   its badge), sessions newest-first, and a node tap opens the word-level
   COMPARE against the version before it — the inline changes list and its
   +/− counts are removed.
4. **`CaptureFormatComponents.kt`** — `PaperLineField(paper = false)` is a
   plain text box (theme `surface` + hairline outline).
5. **`TopicRevealScreen.kt`** — the book sheet's "Add a note…" placeholder
   drops its 0.85-alpha fade (dark-mode readability); `AlbumNotesSheet`
   keys its palette off the AUTHORED cover first, so the sheet matches the
   poster's art.

**Scope:** Android app only (web/ + desktop/ untouched; the user's untracked
concept `.jsx` files were left untracked).

**Status:** committed + pushed; CI validates.

**CI fix (second commit, `4b774680`):** four compile errors in the first
push — a missing `LazyListScope` import in `CabinetV2Content` (which cascaded
into unresolved `item` / `animateItem` plus the tween/spring inference
failures inside the item lambda), `Int * Dp` in the settings rail's initial
offset, a delegated `paletteUrl` that cannot smart-cast for the album sheet's
swatch-cache write, and an `Int` constant assigned to the `Long`
`revealSettleDelayMs`.

**Press feedback + haptics (third commit, v3xx46):** new
`ui/components/CurioPressFeedback.kt` — `Modifier.curioPressClickable` squishes
on `CurioMotion.Springs.Press`, fires one light haptic on the DOWN edge only,
and keeps the ripple via `LocalIndication.current`; the scale is a parameter so
a chip (0.96) and a full-width card (0.985) both read as a press. Applied to
`SettingsOptionRow` (every settings-family row), the hub's tone + secondary
cards, the quick-tool chips, and Home's saved / pinned rows.

**Fourth commit (v3xx47, NOT pushed — user said "dont push this"):** the user
clarified that by "selection highlight" they meant the **TOUCH highlight**, not
the selected state ("for all around the app not the pressed or selected but the
touch … maybe with animation or a pressed look"). New
`ui/theme/CurioPressIndication.kt` — an `IndicationNodeFactory` that paints the
touched element into an offscreen layer and tints it with `BlendMode.SrcAtop`
(in 90ms, out 260ms, `onSurface` @ 0.14), so the press wash only lands on the
pixels the element already drew and follows its own rounded shape. Provided at
the `CurioTheme` root via `LocalIndication`, replacing Material's ripple
app-wide (bottom sheets included). Trade-off: a background-less label (a bare
`TextButton`) now only tints its glyphs instead of getting a circle.

**Fifth commit (v3xx48):** the user pushed the local commits and asked for:
reveal faster + tappable mid-transition; the glass header reaching the status
bar (Home + Profile, glitchy on Profile) and its extended area actually
collapsing ("it stays in its initial size where the stats was") — also without
liquid glass; the Cupboard's 3x/0.5x size variety back; the category-switch
animation back; and the text-history list-view CURRENT pill fixed.
Shipped: (1) `CurioGlassToolbarMorph` clip moved OUTSIDE the size-reporting
`.layout {}` so the glass is trimmed to the animated height instead of
painting the full hero; (2) screen reveal 440→300ms, settle 20ms, and the
frozen frame no longer consumes pointer events; (3) `buildCupboardShelves`
cycles four shelf heights (0.50/0.28/0.34/0.21 × wall width) and a filter
switch fades the wall (`wallSwap` Animatable) since packed shelves can't slide
covers to new slots; (4) `HistoryCurrentPill` wears the theme accent with a
rim and never wraps, and the list row's time is flexible so the pill can't be
clipped.

**Still pending (user's wish-list):** the rest of the animations pass — motion
specific to navigation and bottom sheets beyond the page transitions already
refined, further glass-header morph polish (it collapses correctly now; more
needs their eyes on it), and haptics beyond the surfaces above (the nav bar
already ticks). The user's build result for this batch is still outstanding.

## Request (2026-09-11, completed — settings rail highlight speed/feel + text-history tree badges)

User test results (ask_user): "All landed well" for the Cabinet/header batch;
next up selected: **Animations refinement pass**, **Text history tree**, and the
concrete report "the active indicator in settings top rail too slow and feels
broken".

**Shipped (2 code files + docs, this commit):**

1. **SettingsHubScreen.kt — the rail highlight lands with the tap.**
   `SettingsRailBoundsTransform` 0.8/140 → **0.9/320** (~350ms → ~200ms
   settle: responsive, still a visible glide rather than the old stiffness-500
   instant snap). The "feels broken" part was the POST-COMPOSITION centring
   correction: `LaunchedEffect(active)` + `scrollToItem` snapped the whole
   header sideways one frame after every section switch, right under the
   gliding pill. The rail geometry is fixed (82dp chip + 7dp gap), so the
   centred offset is now computed up front from
   `LocalConfiguration.screenWidthDp` and passed as
   `initialFirstVisibleItemScrollOffset` — the row composes already centred and
   nothing scrolls after composition (the effect is gone).
2. **TextHistory.kt — tree refinements.** (a) A version node's +/− badge now
   compares against the FIELD's previous snapshot across session boundaries
   (running `prevEntry` in `HistoryFieldCard`) — a new session's first node
   used to report its whole text as "+N". (b) The version connector Box is a
   fixed 18dp box instead of `fillMaxHeight()`, which measured 0 height inside
   an unbounded LazyColumn item (the stub + dot drew outside their own
   bounds).

**Scope:** Android app only (web/ + desktop/ untouched).

**Status:** committed + pushed; CI validates.

**Remaining:** the broader ANIMATIONS pass needs specifics — I fixed the
highest-signal animation complaint (the rail). More text-history tree
refinements are queued for the user's next specifics.

## Request (2026-09-11, completed — Favorites = liked topics, Everything → Cupboard, stable wall, instant Cabinet, glass header to the top + a real collapse)

User (ACTIVE 2026-09-10, after a CI failure was pasted in):

1. COLLECTIONS: the shelf name is already in the page header but the
   card/shelf repeats it below → remove the duplicate; AUDIT every collection.
2. FAVORITES vs EVERYTHING are basically the same — Favorites should show the
   FAVORITE TOPICS (the topics you liked), not the saved entries; Everything
   shows only books / series / albums.
3. RENAME Everything → "Cupboard" and design the page properly.
4. The Cupboard wall SHIFTS its size (tiles resize when it reflows) — fix.
5. Fix the workflow too (add / save flow into the renamed page).
6. CABINET saved entries still doesn't load INSTANTLY — it shows a loading
   state; fix the loading properly.
7. PROFILE + HOME glass header: the top doesn't reach the status bar — fix.
8. Glass header morph-collapse: the expanded look doesn't COLLAPSE — fix.
9. Commit and push everything.

Queued follow-up (same file, later): "use ask user for test results … more
refinements in animations and all more refinements in the text history tree
too … the add a caption field is note paper style change it to just a text box
… in dark mode the book bottom sheet is still inaccurate colors the box and
text the add note one … and the albums colors are fully different fix them."

Answered by the user before implementing (ask_user): Favorites = topics you
liked (the reveal heart); Cupboard's Add = search the whole catalog; the book
box = the book sheet's "add note" field; the albums complaint = the ALBUM
BOTTOM SHEET's colors.

**Shipped (7 code files + docs, this commit):**

1. **CabinetV2Content.kt**
   - **No duplicate shelf names:** `V2DetailHeader` lost its name/count block
     (the pinned hero already prints the collection name + item count) and its
     `name`/`count` params — it is the actions strip now (Add pill + kebab,
     end-aligned). `v2VirtualShelfItems` dropped its `v-head` section header
     entirely (same duplication on Saved entries / Notes) and its unused
     `onAdd` param. Audited the rest: the home page's section headers
     ("Saved entries", "Collections") label page SECTIONS, not the page title,
     so they stay.
   - **Favorites = the liked-TOPIC shelf:** new `likedTopics` reads
     `AppPreferences.topicSentimentsState` (SENTIMENT_LIKE, key
     `CATEGORY:topicId`), resolves each id against the warm lane pools
     (`TopicJsonLoader.cached`, behind a `catalogReady` poll capped at ~3s so
     a broken install can't hold skeletons forever), dedupes + name-sorts.
     Rendered by the new `v2LikedTopicItems` / `V2LikedTopicRow` (category
     glyph tile + name / byline / lane, opening the topic's real reveal).
     `shelfCounts[FAVORITES]` + the hero subtitle now count liked topics.
   - **Everything → CUPBOARD:** hero title/subtitle ("Books · albums ·
     series"), the home card (title, "Every book, album and series you keep",
     liked-only count, empty-rail copy, a11y description), the no-match state
     and the home fallback line ("Collections · Cupboard · your keepsakes").
     The internal level key stays `"everything"` to avoid saveable churn.
   - **Stable wall sizes:** `mediaTierIndex` maps `KIND|NAME` → the item's
     position in the FULL name-sorted media list; `v2EverythingMasonryItems`
     takes it and derives each cover's tier from that stable position instead
     of its filtered rank — filtering re-flows the wall without resizing a
     single cover.
   - **Cupboard Add = catalog search:** new `V2CupboardAddSheet`
     (`showCupboardAdd`) searches the BOOKS / ALBUMS / SERIES lane pools
     synchronously and toggles the picked media's favorite (`toggleLikedFavorite`)
     — pinning IS liking, so the wall updates live; an empty query lists what
     is on the wall (the sheet doubles as the manager). The old
     Add-to-Favorites sheet mode is now unreachable (the reveal heart owns
     topic liking).
2. **CaptureRepository.kt** — `lightSnapshot` (`@Volatile`, written from
   `observeLight`) + `peekLight()` for a synchronous first frame.
3. **CabinetScreen.kt** — `produceState` seeded from `peekLight()` plus
   `entriesReady` (flipped by the first real DB emission); a cold first frame
   renders quiet card skeletons instead of the "Your Cabinet is empty" doodle.
4. **CabinetV2Content.kt** — same `peekLight()` seeding.
5. **CurioGlassToolbar.kt** — the glass reaches the status bar:
   `statusBarsPadding()` moved off the bar and onto its content (the leading
   Row, the morph bar's full Column, the compact Row); the morph bar's
   collapsed target is now `compactH + WindowInsets.statusBars.getTop()` so
   the compact row is never clipped.
6. **HomeScreen.kt / ProfileScreen.kt** — the morph header really collapses:
   the collapse clock is hoisted above the scroll content
   (`homeStickyProgress` / `profileStickyProgress`) and drives the reservation
   spacer too (`lerp(full, compact + inset, eased)`), so the page rises with
   the bar; `ProfileHero` gained a `reserveHeight` param (default
   `ProfileHeroTotalHeight`).
7. **Queued polish** — GalleryWall caption is a plain text box
   (`PaperLineField(paper = false)`; the per-field paper values still feed the
   board's quote cards + the saved payload); `ChapterNoteField` wears the
   sheet's `surfaceHigh` / `onSurface` / `onSurfaceVariant` + a 1dp hairline
   instead of ink-alpha washes; `AlbumCoverPoster` gained `resolvedUrl` and the
   album notes sheet seeds it from its palette URL so the poster and the
   sheet's colors can never disagree.

**Scope:** Android app only (web/ + desktop/ untouched).

**Status:** committed + pushed; CI validates.

**Remaining (user's own follow-up list):** more animations refinements, more
Text-history TREE refinements, and the test results they will report.

## Request (2026-09-10, completed — shelf art final pass: book/saved/completed/customs + help&feedback; nav rail highlight glide + back-glitch fix)

User (queued after the Everything/TextHistory push): "redraw cabinets
collections curying now the book itself is bad just the book, the saved
entries properly redesign that, completed too keep it minimal, and all the
custom ones do something unique, and the help and feedback one from
settings, and also the top nav bar that smooth transition animation isn't
that smooth when i tap something it goes solid color without that
highlight so fix that and a glitch when if mid animation i tap back and
go to all settings it still stays in that page it was open so make it a
little bit smooth."

**Shipped (3 code files + docs, that commit):**

1. **CabinetShelves.kt — art final pass:** the Curiying-now BOOK redrawn
   (dark cover slab under three stepped page layers, typed paragraph lines,
   spine crease + knotted ribbon; mug kept), SAVED ENTRIES became a collage
   (big front polaroid + washi tape + star mark + two tilted prints behind),
   COMPLETED kept minimal (one summit + snow tip + planted flag), and the
   custom arts became unique scenes (balloon, planet, sailboat, kite).
2. **SettingsHubScreen.kt — Help & feedback + rail animation:** the chat
   doodle became a support-chat window; `SettingsRailBoundsTransform`
   0.95/500 → 0.8/140 so the highlight visibly glides; rail centering is
   instant (`scrollBy` after one frame).
3. **CurioNavHost.kt — settings-family transitions are pure tweens:**
   enter/exit/popEnter/popExit dropped the `scaleIn/scaleOut` spring for
   `fadeIn/fadeOut(tween(Morph))`, fixing both the solid-colour jump and the
   back-mid-animation stuck page.

**Status:** committed + pushed.

## Request (2026-09-10, completed — Everything wall: no seam + uniform covers; Text history gets the JSX concept feature set)

User: "the cabinets arrangement isn't good — remove the background fill, and
the covers are getting different shapes but I want them to have the shape they
are in and arrange in that shape… they can be 2x 3x .5x etc but in both width
and height… also you see this jsx — it has better text history features and
options — implement all of the features of text history properly in detail."

**Shipped (2 files + docs):** the Everything wall lost its seam plate and the
tiers became uniform scales on an 8-column grid (0.5x → 3x in both
dimensions); TextHistory.kt gained search, filter chips, word-level COMPARE,
collapsible field groups, CURRENT pills, +/− word deltas and preview stats.

**Status:** committed + pushed.

## Archive

Older completed request logs (2026-09-08 → 2026-09-10) were trimmed from this
file on 2026-09-10 to keep it short. They live in git history
(`git log -p -- Prompt.md`) if anything needs revisiting.

## next prompt

_No pending prompt._ (Both the ACTIVE list and the follow-up queued for this
cycle are logged above. The next user instruction goes here — it is never
cleared by an agent; the completed one moves up into the request log.)
