# Prompt.md — current request log

## Request: Revert topic.db, harden JSON loading, fix reveal morph jitter

User request (verbatim):

> "i want you to revert the topic.db chnage from the recent commits, and lets make the json loading alot more bettr, surviving app resrat, and always shwing all of the topics without reloading everytime, and also fixing the topic reveal morph animation somehow it looks jittery now"

### Analysis

**Recent commits (Sep 2):**
- PR #66 (merged `182bc5f2`, branch `v0/prebuilt-topic-sqlite`): bundled a 19MB
  `app/src/main/assets/topics.db` SQLite asset, added `TopicAssetStore.kt`
  (raw SQLite reads), switched `TopicRepository.init()` to open the asset and
  `getTopicsForCategory` to query it, and extended `scripts/build_topics_db.py`.
- PR #67 (`v0/fix-topic-db-reveal`): "persist topic database and smooth reveal
  morph" — changed `RevealBoundsTransform` to a critically-damped 650/0.98
  spring (explicitly to kill a "one-frame snap/judder") and dropped the reveal's
  JSON fallback. **Already reverted** at HEAD via `5ad796d7` (merged `d2bc9c1e`).

**Current broken state (HEAD):**
- `topics.db` still bundled; `TopicRepository.init()` opens it via
  `TopicAssetStore` and marks initialized WITHOUT populating Room → the splash
  skips the JSON warm-up, but `TopicJsonLoader.load()` finds Room empty and
  re-parses JSON lazily per screen anyway → "0 topics" on Home, "Loading…"
  flashes, and the reveal can blank on cold start.
- `RevealBoundsTransform` = spring(0.92, 800) → first-frame snap + slight
  overshoot = the "jittery" reveal morph.

**Plan:**
1. **Revert topic.db (PR #66 scope):** delete `assets/topics.db` +
   `TopicAssetStore.kt`; restore `TopicRepository`'s Room-from-JSON flow and
   drop the bundled-import plumbing (`importBundledRoomDatabase*`,
   `getNullableInt`, unused imports); revert `scripts/build_topics_db.py`
   schema additions.
2. **JSON loading that survives restarts:** Room is the persistent cache —
   first launch imports JSON→Room once; every later launch warms the loader
   cache straight from Room (no JSON re-parse). Gate the JSON re-sync on a
   persisted app-version marker (runs once per app update, not per launch).
   MainActivity awaits init before the index/pool warm-up so warm starts never
   re-parse in the race window.
3. **Reveal morph fix:** restore the critically-damped
   `spring(dampingRatio = 0.98f, stiffness = 650f)` bounds transform.
4. Docs: changelog bullets, `Prompt.md` summary, `app/AGENTS.md` DOX pass.

### What shipped (this turn)

1. **topic.db reverted.** `assets/topics.db` (19MB) and
   `data/TopicAssetStore.kt` deleted. `TopicRepository` restored to the
   Room-from-JSON design: `init()` populates Room from the JSON assets when
   empty, `getTopicsForCategory` reads Room, and the bundled-import plumbing
   (`importBundledRoomDatabase*`, cursor helper, SQLite imports) is gone.
   `scripts/build_topics_db.py` reverted to its pre-experiment schema.

2. **JSON loading — persistent, no per-launch reload.**
   - First launch: JSON parses once into Room's `topics` table.
   - Every later launch: `init()` warms `TopicJsonLoader` counts + pools from
     Room before any screen reads them → all topics present instantly ("0
     topics"/"Loading…" flashes gone), zero JSON re-parse.
   - Content re-sync now runs only when the app version code changes
     (`AppPreferences.getTopicCatalogSyncVersion` marker + `BuildConfig.VERSION_CODE`),
     not on every restart (the old `syncCatalogFromJson` re-read every lane
     every launch).
   - `MainActivity` awaits `TopicRepository.init()` before the
     `loadIndex()`/`preloadAll()` warm-up closes the race that re-parsed JSON
     on warm starts.

3. **Reveal morph jitter fixed.** `RevealBoundsTransform` back to the
   critically-damped `spring(dampingRatio = 0.98f, stiffness = 650f)` — no
   first-frame snap or under-damped settle on the ticket→hero expansion (the
   smoothing PR #67 shipped got reverted with the topic.db work; the spring fix
   is restored independently, keeping the reveal's JSON fallback).

### Docs

- Changelog (`fastlane/metadata/android/en-US/changelogs/20260921.txt`) updated.
- `app/AGENTS.md` Curio Database section updated (Room topic cache flow; no
  bundled topics.db).

### Verification

- No Gradle build in this environment (CI compiles on push).
- Imports audited by hand (removed `TopicAssetStore`, `SQLiteDatabase`, `File`;
  added `BuildConfig`); no external callers of the removed APIs found via
  code_search.