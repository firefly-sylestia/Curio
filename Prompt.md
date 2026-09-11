# Prompt Log — current request

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
