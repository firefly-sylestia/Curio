# Prompt.md — current request log

## Request: Instant reveal for first-time topics + Cabinet category-picker revamp

User request (verbatim, condensed):

> Still, in the Topic Reveal, NEW topics are slow loading as well — fix them
> too, so it doesn't take them ~1 sec to show the info. Also apply the same
> category picker (the v314 Topic Database panel + multi-select treatment)
> in the Cabinet screen. (We'll go back to "save your take" next — it's
> getting a redesign; the topic-browser refinements continue here.)

### Root causes found

1. **Slow first-time reveal.** `TopicRepository.findTopic` fetched the
   WHOLE lane from Room and scanned + mapped every row in Kotlin on every
   reveal open. First-frame seeding only consulted the per-lane cache, so a
   never-opened topic whose lane cache wasn't resident had to wait on that
   async full-lane fetch — and when Room's copy was stale (topics added to
   the JSON between app updates without a version bump, so the version-gated
   sync never ran), `TopicJsonLoader.load()`'s Room fast path served the
   stale rows and the reveal could never see the new topic at all.
2. **Cabinet single-select chip bar.** The Cabinet still had the old sticky
   every-lane chip bar + single `CategoryId` filter, unlike the Topic
   Database's v314 panel + multi-select.

### What shipped (this turn)

**A. Reveal speed (`TopicDao.kt`, `TopicRepository.kt`, `TopicRevealScreen.kt`):**
- `TopicDao.findByCategoryAndName` — indexed SQL `categoryId = ? AND name =
  ? [NOCASE] LIMIT 1`; `findTopic` uses it instead of the whole-lane scan.
- `resolveRevealTopic()` seeds `resolved` on the FIRST frame from the warm
  lane cache AND the prewarmed merged index (`cachedIndex()`, which
  survives lane-cache trims and carries wildcard.json originals). Plain
  function (NOT @Composable — it runs inside `remember {}`).
- `TopicRepository.refreshLaneFromAssets(context, id)` — parses the
  bundled JSON directly (bypassing the Room fast-path mask),
  REPLACE-upserts the whole lane back into Room, returns the fresh pool.
  The reveal's final fallback uses it for canonical lanes (WILDCARD keeps
  the shared merged `load()`).
- Content-incomplete-row hydration is deduped once per topic per process
  (`hydratedIds`) so the full-lane JSON parse never repeats.

**B. Cabinet category picker (`CabinetScreen.kt`):**
- State: comma-joined enum-name `String` → `Set<CategoryId>` (rotation/tab
  safe, no Saver), single `commitFilters` mutator, `CategoryIdSaver` gone.
- The sticky every-lane chip bar is DELETED (`CabinetStickyChipBar`,
  `CabinetChipPop`, `FilterChipLite` dead code removed); searching alone
  shows no category chips.
- Hero Category pill opens `CabinetCategoryPanel` (collapsed by default):
  its own tiny search box filters the category list, accent-checkbox
  multi-select with per-lane entry counts, a tertiary Legacy row (always
  reachable so it can be toggled off), Clear all + Done.
- `CabinetActiveFilterChips` renders exactly the selected lanes (+ Legacy),
  each removable with one tap; filtering = text AND (no categories selected
  OR entry category in set).
- Single selected lane keeps the page wash + hero subtitle; multi-select
  uses the neutral wash + "Showing N categories"; no-match empty states
  split into single-lane (spin CTA) vs generic clear-filters.
- Constants: `CabinetChipBarHeight` kept for the chips row, new
  `CabinetFilterPanelHeight` reserved when the panel is open; the grid's
  glass-capture + content-top reservation now key on `filterUiVisible`.
- Imports cleaned (removed unused Saver/Brush/LocalDensity/LazyRow/
  itemsIndexed/LayerBackdrop; added clickable, horizontal/verticalScroll,
  rememberScrollState, size, Checkbox(Defaults), TextButton, TextAlign,
  TextOverflow, categorySurface, pastelFillInk).

### Verification

No Gradle builds allowed in this environment (CI compiles on push).
Hand-audited: balanced delimiters in all four files (brace/paren/bracket),
no stale references to removed symbols, imports matched to usage, Material3
APIs are basic (Checkbox/TextButton/Surface — safe on the Compose BOM),
lambda-rule compliance (no @Composable calls inside click/remember
lambdas), SQL valid for Room @Query.

### Follow-ups (next request: "save your take" redesign)

- The Cabinet's category panel could later learn typo-tolerant search like
  the Topic Database's (currently substring-only) — deferred as out of
  scope.
- Wildcard lane fallback still shares `TopicJsonLoader.load()` (merge
  semantics) — fine, noted.
- `ANALYSIS.md` at repo root is a pre-existing unrelated untracked file —
  not committed.
