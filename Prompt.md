# Request Log — catalog staleness, browser loading, picker apply, Spin deck

## Status: implementation complete — committed & pushed pending CI

## The request (user, paraphrased)
1. Topics that were removed/overridden in the data still show on device — duplicate old
   listings linger even though the JSON no longer has them.
2. Topic Browser has loading lag when loading topics.
3. Category picker: category switching should NOT apply instantly on tap — apply when the
   picker is closed (user picked "Apply when I close the panel" when asked).
4. Spin shuffle deck: use smart loading with a small pool; always show cards while data is
   loading; when the full data loads, the shuffle picks from that again during spin.

## Root causes found
- **Stale rows:** `TopicRepository.syncCatalogFromJson` (the only cross-release sync) only did
  `insertMissing` + content `backfillContent` — it NEVER deleted rows that vanished from the
  shipped JSON, so removed/renamed/deduped topics lived in Room forever. It also only ran when
  `versionCode` changed, so dev/CI builds that keep the same versionCode never re-synced at all.
  `TopicJsonLoader.parseAndCache` and `refreshLaneFromAssets` also only upserted. Room's `topics`
  table is a pure mirror of the bundled JSON (user data lives in `cached_topics`/captures), so a
  per-lane replace is safe.
- **Browser flash-lag:** `searchPass` was a `produceState` that RESTARTED with an empty list on
  every key change (category filter, settled search, done-set) → a visible "No topics match"
  flash + re-derivation between every switch.
- **Picker timing:** the panel's checkboxes committed to the live filter on EVERY tap, so the
  whole 16k-topic filter pass re-ran behind the open panel per tap.
- **Spin deck:** the pool `produceState` showed a "Gathering the deck…" hint while the full
  lane pool loaded (cold cache / memory shed), leaving the fan empty instead of dealing
  immediately.

## Changes made
- `TopicRepository`: catalog sync now MIRRORS each lane (delete category + insertAll) after a
  successful asset parse — removals/renames/dedupes leave Room. Trigger widened: runs when the
  versionCode changed OR the APK's `lastUpdateTime` changed (new `AppPreferences`
  `last_catalog_sync_update_ms` stamp) — covers same-versionCode installs. Fresh import stamps
  both. `refreshLaneFromAssets` now also deletes+inserts. Added `sampleTopics(context, ids)`
  (small Room random sample per lane; WILDCARD samples every canonical lane) + private
  `packageLastUpdateTime`.
- `TopicDao`: added `getRandomTopics(categoryId, limit)` (ORDER BY RANDOM() LIMIT n — no lane
  mapping).
- `TopicJsonLoader`: `parseAndCache`'s Room write mirrors the lane for canonical categories
  (WILDCARD merge still upserts).
- `SpinScreen`: when the cache seed is empty, the pool is seeded immediately with
  `TopicRepository.sampleTopics` so cards render on early frames; the full loaded pool replaces
  the seed when it lands and the fan re-deals (keyed on the loaded pool) — spins draw from the
  full catalog.
- `TopicDatabaseScreen`: category panel is now STAGED — `pendingCats` edited by the checkboxes,
  committed once on Done; closing via the pill discards. Also converted `searchPass` from
  `produceState` (empty-list restart) to a retained `mutableStateOf` + `LaunchedEffect`, so the
  previous rows stay on screen while the new set computes on `Dispatchers.Default`;
  `searchPassReady` gates only the first build ("Preparing topics…" instead of a one-frame
  "No topics match").

## Not changed (deliberate)
- Version-gated + install-gated sync location unchanged (background, once per install).
- `insertMissing`/`backfillContent` DAO methods left in place (unused now, harmless API).

## Validation
No Gradle builds in this environment (CI compiles on push). Verified: brace/paren balance on all
six edited files, imports present, no leftover references, phone + wide paths share the same
pending/commit lambdas. fastlane changelog + Prompt.md updated.

## Follow-ups if CI fails
Fix in one cycle from the CI log.
