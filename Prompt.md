# Request — UI polish: reveal dock small-screen fix, detail-style pop animations, Topic History redesign

## Part 1 — Topic Reveal action dock cut off on small screens (DONE)

- `TopicRevealScreen.kt` — the dock stays a fixed 80dp (the reveal morph's reserved
  slot depends on it). Root cause: button labels wrapped to 2 lines and vertical
  paddings + nav-bar inset left too little room, so content spilled below the visible
  area. Fix: `BoxWithConstraints` with a `compact` flag (maxWidth < 440dp ||
  maxHeight < 44dp) tightening paddings/typography on phones only; both button texts
  get `maxLines = 1` + ellipsis. Tablets/wide keep the original metrics. Committed
  `0db21b7`.

## Part 2 — Detail-style center pop for more screens (DONE)

- User selected (ask_user): Save/Capture (+edit routes), Profile, Quests, Settings
  hub + all pages, Pet Designer, Topic History, Manage Categories, Recents,
  Support/Bug Report, Topic Database, and a subtle scale-fade for Home/Spin/Cabinet
  tab switches. Excluded: Lightbox, Category Picker (kept slide), Reveal (own morph),
  boot gates.
- `CurioNavHost.kt` — added `popScreenRoutePrefixes` + `isPopScreenRoute`; the
  selected screens share the detail page's center pop (scaleIn 0.88 + fadeIn,
  scaleOut 0.88 + fadeOut, underlying screen dims over the same 450ms). Tab switches
  now `scaleIn(0.97) + fadeIn` / matched fadeOut. A pop screen that opens a non-pop
  push (e.g. Settings → Lightbox) also shrinks out. Committed `468869a`.

## Part 3 — Topic History redesign (DONE)

- User direction: search & filter, polish the list, match the header style.
- `TopicHistoryScreen.kt` — header now matches the shared push-screen style
  (ExtraBold title + muted subtitle, like Recents); added the shared search field
  (matches topic names AND category display names) + a horizontally scrolling
  single-select `CurioCategoryChip` filter row (only categories present in history);
  day-group headers show entry counts; a "No matches — Clear filters" empty state
  (inside ScreenEntrance like the list); list spacing breathing-roomier.
- `ui/components/CurioSearchField.kt` (NEW) — the canonical search box (rounded
  surface + magnifier + live query + one-tap clear), extracted from the Settings
  hub's private copy; `SettingsHubScreen.kt` now uses it (private `SettingsSearchField`
  deleted, orphaned imports removed). Committed `6f6609f`.

## Part 4 — CI fix: AGP 9 split toggle rename (DONE)

- CI failed on `app/build.gradle.kts:118` — `Unresolved reference 'isEnabled'` in
  `splits { abi { … } }`. Verified against the actual `gradle-api-9.2.1` sources jar
  (Google Maven): AGP 9 renamed the `Split` toggle from `isEnabled` to `isEnable`
  (`interface Split { var isEnable: Boolean }`); `isUniversalApk` / `reset()` /
  `include()` are unchanged. Fixed to `isEnable = true` and pushed (`6389d60`).

## Part 5 — CI compile errors after the AGP 9 fix (DONE)

- Once the `isEnable` fix let CI past configuration, compilation surfaced errors
  that had been accumulating unverified since commit 3425486 (every intervening CI
  run died at the config stage):
  1. `SettingsHubScreen.kt` — `Unresolved reference 'clip'` at 132/140: the
     search-field refactor wrongly deleted `import androidx.compose.ui.draw.clip`
     while `.clip()` was still used. Re-added (other removed imports verified
     genuinely unused).
  2. `CurioNavHost.kt` — the confusing `substringBefore` "none of the following
     candidates" (138:79) + syntax errors (147:52+) were BOTH caused by ONE bug:
     the KDoc line `edit-*/{...}, and settings/*` contains a literal `*/`, which
     prematurely closed the block comment; the parser then swallowed the `{...}`
     as a trailing lambda ("actual type is '() -> Unit', but 'String' was
     expected") and derailed. **Reproduced empirically** with
     `kotlin-compiler-embeddable-2.3.0` (from the Gradle cache) on a scratch
     file: the buggy version emitted the exact CI errors, the reworded version
     compiled clean. Reworded to `the edit-* family, and settings sub-pages all
     match by prefix.`
- Note: `scripts/check_braces.js` cannot catch this class of bug — a premature
  `*/` followed by balanced `{...}` tokens keeps brace counts even.

## Validation

- Kotlin delimiter balance OK on every edited file; `git diff --check` clean;
  no stale references to deleted private functions; code-reviewed after each part
  (reviewer fixes applied: config-time version capture, post-rename assertion,
  pop-screen shrink-out, shared search component, ScreenEntrance wrap).
- Part 5 validated by compiling a faithful reproduction with the cached Kotlin
  compiler (buggy version reproduces CI errors exactly; fixed version compiles).
- Gradle builds remain CI-only per DOX rules; CI is the compile gate.

## Notes

- CI workflow release work (release-only PR builds + per-ABI split release APKs)
  was completed and pushed earlier this session (commit 3425486).
- Per the user's "don't push" instruction, the repo `scripts/check_braces.js`
  addition + `app/AGENTS.md` reference remain uncommitted in the working tree.
- Unrelated working-tree changes (`docs/app/QUEST_AND_PET_REDESIGN_SPEC.md` deletion,
  untracked `docs/plans/`) remain untouched and out of commits.

## Part 6 — Spin deck swipe direction fix (DONE)

- `SpinScreen.kt` `Carousel` — the deck-swipe mapping fired the OPPOSITE cycle on
  release: swipe LEFT → `onCycle(1)` (advance), swipe RIGHT → `onCycle(-1)`
  (back). The fan is a vertical reel (peeks above/below the hero), so a
  horizontal swipe controlling it read inverted to users. Flipped to follow the
  gesture: right → +1 (next card from the bottom peek), left → −1 (previous from
  the top peek), with an inline comment documenting the mapping.

## Completion

Parts 1-3 committed and pushed (`0db21b7`, `468869a`, `6f6609f`); Part 4 pushed
(`6389d60`); Part 5 (clip import + KDoc `*/` fix) pushed (`2f7bbaf`); Part 6
(swipe direction flip) pushed as a new commit.
