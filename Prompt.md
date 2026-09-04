# Request Log — Topic Browser load + category-switch latency vs reference commit

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user, paraphrased)
The topic-browser scroll lag is gone, but compared to the referenced commit
(d9a376d2) the initial topic LOAD takes longer, and switching categories shows
the new topics with a delay. Fix if possible; explain the reason first. Also
dump versionCode and versionName.

## Version (asked by the user)
versionName = 1.1.1 (tag-driven via env; default "1.1.1" in build), versionCode = 20260921
(app/build.gradle.kts lines 63-64).

## Analysis / root causes
1. Data: topic count grew only 4% since d9a376d2 (20,015 → 20,877) but bytes
   grew 34% (18.5 → 24.9 MB) — albums.json 0.95 → 3.24 MB (per-track arrays +
   synopses) and books.json ~2.6 MB (chapter arrays + synopses) landed AFTER
   the reference commit, so every cold parse of those lanes is 3-4× heavier.
2. Code — cold open built the pipeline up to THREE times:
   - catalog was produced by a per-category produceState, then SWAPPED to a
     merged-index derivation the moment loadIndex() landed → two catalog
     identities, and indexedTopics + the row build ran for EACH source.
   - indexedTopics' produceState seeded itself with a 20k map from the cached
     index and then IMMEDIATELY rebuilt the same 20k objects in its block
     (produceState always runs its block) → the seed was pure duplicate work
     on every open, warm or cold.
   - parseAndCache awaited a full Room deleteCategory + insertAll (entities
     embed chapter/track JSON) on the render path of the cold open.
3. Category-switch delay: every switch re-ran indexedTopics.associateBy (20k)
   + per-lane filter + sort over the whole catalog on Dispatchers.Default,
   while the OLD rows stayed on screen — so the new list appeared only after a
   full-catalog rebuild (a visible beat on big lanes).

## Changes
TopicDatabaseScreen.kt:
- Catalog now derives ONCE from the loader memory cache (remember keyed on
  visibleCategories + a fill-generation). A LaunchedEffect fills only the
  lanes the cache is missing (deduped with the app-start prewarm via the
  loader's shared in-flight parse) and bumps the generation once. No
  index-source swap, no second identity. catalogFilled guards the loading
  note so a lane that fails to parse can't hold "Preparing topics…" forever
  (v49 skip semantics preserved).
- indexedTopics builds its 20k entries ONCE per catalog identity (empty seed);
  the old duplicate seed build and the useIndex source branches are gone.
- Browse-mode row build uses the pre-grouped topicsByCat lists instead of a
  fresh associateBy + filter + sort over all topics per switch — a lane
  switch now walks only the selected lanes' prebuilt rows. The id map moved
  into the search branch (only paid when a query settles).
- Removed the now-unused CatalogState data class + TopicIndexEntry import.

TopicJsonLoader.kt:
- The Room mirror (deleteCategory + insertAll) after an asset parse now runs
  fire-and-forget on the loader scope instead of blocking parseAndCache, so a
  cold browser open renders rows the moment the JSON is parsed; Room catches
  up in the background (reloadFromAssets callers still get awaited writes).

## Validation
Structural checks (brace/paren balance) clean; CI compiles on push (local env
forbids Gradle). Behavioral spot-checks on-device recommended next build:
cold-open load time, lane switch latency, search toggle with WILDCARD.
