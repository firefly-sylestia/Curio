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

## Part 7 — Weekly quests (DONE)

- User asked how much of the Quest & Pet Redesign spec was implemented, and
  what happened to weekly quests. Audit result: ~90% shipped (Phases A–E:
  daily-first IA, First Journey tutorial, category passport, reward moments,
  pet with all 6 growth stages). Weekly/challenge quests were NEVER built —
  the spec §8.1/#6 + §8.2/#6 marked them optional/deferred, and commit
  8753d75 only RECORDED a "weekly chain" as a planned v8.28 hook in Prompt.md.
- User chose (ask_user): weekly quests ship **always-on** (no toggle), and I
  design the shape.
- Implemented v8.42 (then made DYNAMIC per user follow-up — a fixed weekly
  trio felt too predictable):
  - `CurioQuests.kt` — `WeeklyKind` (SPIN/EXPLORE/SAVE/LANES/QUOTE/PIN/LIKE/
    PROFILE), `WeeklyQuest`, private `WeeklyPool` (two tiers per kind),
    `weeklyQuestsFor(weekKey)` picks 3 quests of DIFFERENT kinds (rotating
    3-kind window across the 8 kinds + alternating tier) deterministic per
    week; Monday-4AM ISO week rollover (`currentWeekKey`/`ensureWeekly`),
    `bumpWeekly` generalized to all kinds (fed from spin/explore/save/quote/
    pin/like/profile hooks; explore also feeds the distinct-lane set),
    `weeklyProgress`/`isWeeklyDone`/`claimWeekly` (validates against
    `weeklyQuestsFor(currentWeekKey())`); new prefs keys
    `weekly_date/progress/lanes/awarded` in seed/write.
  - `QuestsScreen.kt` — "This week's quests" `WeeklyCard` right under the
    daily stack (always-on), teal accent + `CalendarToday` header ("New
    goals every Monday"), rows with progress bars + Claim pill (same rhythm
    as dailies, claimed rows animate out); level-up banner + haptic +
    celebrate on claim like dailies.
  - Store changelog 20260906.txt updated.

## Part 8 — Real pet AI: on-device learning brain (DONE)

- User rejected cloud/LLM pet AI and asked for OUR OWN smart model: a local,
  personalized pet that "gets smarter and learns over time and develops its own
  things". User chose (ask_user): Settings toggle, default ON; mostly the pet's
  PERSONALITY rather than dialogue content.
- Implemented v8.43 as `data/CurioPetBrain.kt` (NEW) — a fully on-device model
  (no network, no LLM), gated by `AppPreferences.petBrainEnabledState`:
  - Observation: `observeActivity` (per spoken screen visit — time-of-day
    histogram + trait decay after idle), `observeExplore`, `observeLevelUp`,
    `observeXp`, `observeTouch`, `observePlay`.
  - Traits: persistent 5-dimension vector (CURIOSITY/PLAYFULNESS/WARMTH/
    ENERGY/NIGHT_OWL, 0..1, JSON in prefs) nudged by real behavior, decayed on
    long idle; `dominantTrait` picks the voice.
  - Preferences: `favoriteLane` from passport engagement (>25% share = learned
    lane); activity histogram drives NIGHT_OWL and the morning/night coining.
  - Catchphrases: `maybeCoin` (≤1/day, cap 8) — the pet COINS its own lines
    from strong recurring patterns (night owl, morning ritual, a dominant
    lane, streak ≥7, saves ≥10, playful, curious, warm) and remembers them
    forever; they surface ~30% of the time.
  - Voice: `say()` — one-sentence, fully GROUNDED lines (level, streak, saves,
    lane, time of day — never invented topic facts, spec §10.6/10.7); returns
    null when off/too young so the classic library answers.
- Wiring: `CurioPet.bubbleFor` observes + tries `CurioPetBrain.say()` first
  with classic `lineFor` fallback; all five note hooks
  (noteTouch/notePlay/noteXpEarned/noteLevelUp/noteLaneExplored) feed the
  brain. NOTE: brain function named `say()` (not `lineFor`) — same-name
  overload with identical params but different return types is illegal in
  Kotlin.
- Settings: "Pet brain" toggle row in Appearance (default ON), mirroring the
  Curie/Floating pet rows (key `pet_brain_enabled`).
- Store changelog 20260906.txt updated (kept ≤500 chars).
- Code review fixes applied: coined sayings surfaced in the pet tap dialog
  (`TapInfo.coinedSayings` + "Its own sayings: N" line in CurioPetCompanion),
  all brain lines normalized to one sentence (spec §10.7), and familiar lines
  (streak/saves/"Hey you") gated behind the FRIEND bond tier like the classic
  library (v8.29 rule).

## Part 9 — Reveal dock: un-squish buttons on phones + stop it lingering on exit (DONE)

- User: on the Topic Reveal screen the "Start exploring" / "Already watched"
  buttons still look very small and squished, and the dock lingers for a
  moment when switching away.
- Root cause 1 (squish): `compact = maxWidth < 440.dp || maxHeight < 44.dp`
  fired on EVERY phone, but its metrics were tuned for the worst case
  (~32dp content = 80dp dock minus the 3-button-nav inset). Gesture phones
  have ~48-56dp of room yet got the absolute-minimum treatment.
- Root cause 2 (linger): the reveal's exit transition runs ~450ms, and the
  dock lives OUTSIDE the animated content (Scaffold bottom slot, kept
  registered during exit so the morph reserve never swaps — v8.5/v8.37), so
  it stayed fully opaque until the destination disposed.
- Fix in `TopicRevealScreen.kt` (v8.44):
  - Two independent squeezes: `compact = maxWidth < 440.dp` (horizontal
    only) + `tight = maxHeight < 48.dp` (three-button-nav). Three tiers:
    tight (icon 16 / 14sp / 2dp pad — unchanged minimum), compact phone
    (icon 18 / titleMedium 16sp / 8dp vertical padding — roomy, was 16dp
    icon + 2dp pad), tablet (original generous metrics). Height math
    verified per tier (26dp / 40dp / 52dp vs 32 / 48 / 56 available).
  - `dockLeaving` state: the dock fades to 0 alpha over 200ms (alpha ONLY —
    still occupies the exact 80dp slot, so innerPadding + shared-element
    morph stay untouched) whenever the user leaves: close ✕, system back,
    Explore-now → Home, and both Write-about-it dialogs.

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
