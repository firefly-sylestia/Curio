# Prompt.md — Request log

## Current request — COMPLETED: scope rail + CI fix + topic-file dedupe

User (two messages): (1) "why the duplicate books got separated again —
I thought you will merge them so the old saved entry don't get corrupt";
(2) "separate web and desktop work, don't do it until I mention it, add
it in your instructions"; then confirmed "yes, dedupe all affected files"
and pasted a CI failure (shared-element API errors in TopicRevealScreen +
CurioBottomNav).

### 1. Scope rail (pushed `e665449`)
- Root `AGENTS.md` now has a 🔒 Scope section: **no edits/builds under
  `web/` or `desktop/` unless the user explicitly asks** — including data
  mirrors (Android data fixes apply to `app/src/main/assets/` ONLY). Both
  section headers marked ⛔ ON HOLD. Prompt.md rules captured.

### 2. Duplicate-books answer
- `books.json` was NOT re-separated (still 444, zero dup names/ids; the
  web mirror clean too). The real dups were in files never deduped —
  same entity twice under tier-1/tier-2 ids (authors 38, astronomy 89,
  songs 26, geology 11, animals 10, technologies 3, chemistry 1).

### 3. CI fix (v152)
- `SharedContentState` is a NESTED interface — `SharedTransitionScope.SharedContentState`
  (top-level import unresolved in animation 1.11). `sharedElementWithCallerManagedVisibility`
  param renamed `state` → `sharedContentState` (since Compose 1.8). The
  reveal pill referenced scope vars that only existed in `HeroCard` —
  now pulled via `LocalRevealSharedScope` / `LocalRevealVisibilityScope`
  + `rememberSharedContentState(SentimentSharedElementKey)` in the pill's
  own composable, null-guarded (plain pill fallback). Dead state removed
  from HeroCard.

### 4. Dedupe (Android assets ONLY — web mirror untouched per scope rail)
- 178 groups / 181 entries collapsed across authors (500→461),
  astronomy (1000→911), songs (1000→972, 2 triplets), geology (1000→989),
  animals (1016→1006), technologies (1000→997), chemistry (1011→1010).
- Rule mirrors the books dedupe: richest entry wins (longest teaser +
  richest exploreAction + most tags), tags unioned keeper-first, tier
  preserves 1 (marquee), survivor at first position, per-file indent
  preserved (astronomy/technologies = 2, rest = 1) so untouched content
  stays byte-identical. Script: `/tmp/dedupe_topics.py`.
- `topic_index.json` rebuilt via `scripts/build_topic_index.py` —
  16,833 topics, fully in sync both directions (the old index was stale,
  predating recent content commits).
- Saved entries are safe: survivors keep the same name, and the v135
  tolerant name matcher resolves old names.

### Commits
- `fix:` CI — shared-element API (CurioBottomNav + TopicRevealScreen)
- `data:` topic dedupe + index rebuild
- (scope rail already pushed as `e665449`)

### Verification
No Gradle build here (CI validates on push). On-device: open a topic
from Spin and confirm the nav pill morphs into the Like/Dislike pill
(and morphs back on close); browse Authors/Astronomy/Songs and confirm
no duplicate cards; Topic Database search shows each topic once.
