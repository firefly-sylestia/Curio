# Prompt.md — current request log

## Request: Haptics everywhere + bulletproof topic loading + cache book covers

User request (verbatim):

> "add haptics in alot of things, staifying haptics, and improve the json loading as
> sometimes when i open a topic theres nothing in app room db maybe which saves the
> loaded and explored topics and always keeps them stay loaded and never reload them
> or something, youd know the solutin, even from the explored topics or saved, can u
> save them locally, also the new image fetch for books make it cache and"

### What shipped (this turn)

1. **Satisfying haptics** (hoisted `val haptics = LocalHapticFeedback.current` per
   screen — never read inside click lambdas):
   - TopicRevealScreen: **Start exploring / Express yourself** (`KeyboardTap`),
     **Pin** toggle (`TextHandleMove`); favourite pill already ticked.
   - SaveCaptureScreen: Save CTA → **Confirm**.
   - Share sheet (TopicShareCard): **Save + Share → Confirm**, **Reset + Done →
     TextHandleMove**.
   - CabinetScreen: opening an entry → `KeyboardTap`.
   - HomeScreen: hero/first-run Spin CTAs → `KeyboardTap`.
   - Existing strong haptics kept: spin wheel ratchet + landing Confirm, bottom-nav
     ticks, quest completes, constellation, pet designer, reveal favourite.

2. **Topic loading — the reveal can never come up empty + explored topics stick.**
   - `TopicRevealScreen` resolution chain: Room `findTopic` → on-demand parse of the
     lane's JSON via `TopicJsonLoader.load` (caches + persists into Room) → last
     resort `TopicCatalog.findByNameAcrossAll` (saved wildcard curiosities/renamed
     topics). The old chain only consulted the warm in-memory cache, which could be
     empty at cold start → blank "Loading topic…" reveal.
   - New `TopicRepository.rememberTopic(context, topic)` upserts every **resolved /
     explored** topic into `cached_topics` (the same durable table saves write), so
     loaded/explored topics are kept locally forever and never re-parsed.

3. **Book cover (image) caching.** `MainActivity.onCreate` installs a shared Coil 2.7
   `ImageLoader` with an explicit memory cache (22%) + disk cache
   (`cacheDir/curio_image_cache`, 3%), `respectCacheHeaders(false)` (servers'
   no-cache headers can't bust it) and SvgDecoder — covers download once and hit
   disk on later visits/restarts.

### Docs

- Changelog (`20260921.txt`) — 3 new bullets on top (haptics, reveal-never-blanks,
  image caching).
- `app/AGENTS.md` — Curio Database section extended (`rememberTopic`), plus new
  bullets for the Coil cache and the haptics placement contract.

### Verification

- Braces balanced in all 7 touched Kotlin files (checked per file).
- `TopicCatalog` + `matchesSavedName*` were already imported in TopicRevealScreen.
- Haptics imports (`HapticFeedbackType`, `LocalHapticFeedback`) added where missing
  (SaveCapture/Cabinet/Home/TopicShareCard).
- CI compiles on push (no Gradle in this environment).