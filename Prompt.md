# Prompt.md — Request Log

## Current Request (COMPLETED): Route chain-quest stage rewards through addXp

**Date:** 2026-08-11

**What was asked:** Route chain-quest stage rewards through `addXp` so level-ups and evolutions never get missed (fixes the known quirk where `checkAll` grants stage XP outside the level/evolution detection path).

**Changes made (CurioQuests.kt):**
- `checkAll(context)` → `awardChainStages(): Int` — awards any satisfied chain stages, returns the total XP granted, and NO LONGER persists (callers own the write).
- `addXp` — calls `awardChainStages()` BEFORE reading the after-state, so stage XP is folded into level/evolution detection. Real XP (amount > 0) keeps the full reaction chain (evolved > level-up > xp-earned); a 0-XP refresh (amount == 0) only speaks when a level or growth tier was actually crossed, so it can't stomp an earlier event like a streak milestone.
- `onStreakRecorded` — fires `noteStreakMilestone` (if new best) then routes through `addXp(context, 0)` (was `write` + `checkAll`), so chain XP from a streak record is detected and everything persists via addXp's write.
- Seed/restore path — calls `awardChainStages()` + explicit `write(context)` (no pet reactions on restore).

**Validation:** brace balance OK, `git diff --check` clean, code review passed, no remaining `checkAll` refs. Gradle build left to CI.

## Current Request (COMPLETED): Dedicated quest-complete trigger in the Pet Designer

**Date:** 2026-08-11

**What was asked:** Add a dedicated quest-complete reaction trigger in the Pet Designer so users can customize it.

**Changes made:**
- **PetDesign.kt** — `PetReactionEvents.QUEST_COMPLETE` const added to `ALL` (auto-renders a "Quest complete" card in the Built-in reactions grid, auto-included in `PetDefinition.actionEventIds`) + `label` / `trigger` ("When you claim a daily or weekly quest") / `defaultLine` ("Quest done!"). `DEFAULT_REACTIONS` gains a distinct default rule (HOP + happy eyes, smile, blush, sparkles — different from REVEAL's star eyes). `PetActionTrigger.QUEST_COMPLETE = "questcomplete"` added to `ALL` + `label`, so users can author custom actions that fire on quest claims (plain chip in the trigger picker; no param special-case needed).
- **CurioFloatingPet.kt** — event-reaction mapping: QUEST_COMPLETE now uses its own `PetReactionEvents.QUEST_COMPLETE` rule (was REVEAL); custom-actions `when`: quest claims fire the dedicated `PetActionTrigger.QUEST_COMPLETE` trigger (was LEVEL_UP), while STREAK_MILESTONE keeps riding LEVEL_UP.
- **Compatibility** — `reactionFor` falls back to `DEFAULT_REACTIONS`, so existing saved designs + evolved designs get the new default automatically; the "questcomplete" action kind is forward/backward tolerant (unknown kinds never fire, per the trigger's doc).
- **docs/PET_DIALOGUE.txt** — section 18 notes the designer customization; **fastlane changelog** bullet added.

**Validation:** brace balance OK, `git diff --check` clean, code review passed (only cosmetic nits; the custom-action behavior change — quest claims no longer fire LEVEL_UP actions — is the intended dedicated-trigger behavior). Gradle build left to CI.

## Current Request (COMPLETED): Two-voice pet dialogue rework (baby vs evolved) + motion-first tuning

**Date:** 2026-08-11

**What was asked:** Rework the dialogue: baby should talk limited/telegraphic with its own personality (not like the adult), play should be less words / more reaction, fix the tap-spin loop, spinning should react more / talk less, and the evolved form should have a distinct voice — grounded in human natural-language research.

**Research:** Web research on child telegraphic speech (18-24mo: 1-3 word utterances, content words only, no articles/auxiliaries, exclamation + onomatopoeia led), pet-directed speech (parentese), and natural character-dialogue principles (short bursts, contractions, sensory grounding, idiolect consistency).

**Changes made:**
- **CurioPet.kt — v14 BABY voice (telegraphic)**: ~20 baby pools (10 moods, 11 events, 3 tap tiers, spin cheer, peek/chameleon/spark games, morning, 3 welcome tiers, sassy) + `babyMoodLine` / `babyEventLine` / `babyTouchLine` / `babyStreakLine`. Every line source routes on `currentStage() == Stage.BABY`: `eventLine` (baby sassy + babyEventLine), `lineFor` (block body), `touchReaction` (block body), `spinCheer`, `peekLine`, `chameleonLine`, `sparkLine`, `morningGreeting`, `welcomeBackLine` (baby pools per absence tier), and `bubbleFor` bypasses `CurioPetBrain.say` for the baby (learning continues via observeActivity; only speech is routed). The evolved forms keep the full existing library (now explicitly the "evolved register").
- **CurioFloatingPet.kt**: (a) tap tier-3 spin fix — `spinKey++` only when `tapStreak == 3` (the first tap reaching the celebration tier), so rapid taps never restart a spin loop; (b) spin cheer — `celebrateKey++` always, `queueReaction(spinCheer)` only ~45% (react more, talk less); (c) chameleon + spark games — motion always, line only ~35%; (d) Pet Life routine lines suppressed for the baby (motion only) so it never utters adult routine sentences.
- **docs/PET_DIALOGUE.txt** — new section 20 (VOICE SYSTEM: baby telegraphic rules, evolved natural-language rules, motion-first principles); **fastlane changelog** bullet added.

**Validation:** brace balance OK, `git diff --check` clean, code review passed (found + fixed: routine lines leaked the adult voice to the baby; when-branch indentation after block conversions). Gradle build left to CI.

## Current Request (COMPLETED): Quest-completion + streak-milestone pet reactions

**Date:** 2026-08-11

**What was asked:** Add quest-completion and streak-milestone pet reactions so the pet celebrates those too.

**Changes made:**
- **CurioPet.kt** — Event enum gained `QUEST_COMPLETE` + `STREAK_MILESTONE`; `noteQuestComplete(context)` (persists `KEY_LAST_QUEST_AT`, fires event); `noteStreakMilestone(context)` (persists `KEY_LAST_STREAK_AT`, fires event; day count read from `CurioQuests.bestStreakState` at speak time); `streakMilestoneLine(streak)` with flame-day pools (1/3/7/14/30 get their own bigger lines, other new-best days get `$streak`-aware "still glowing" lines); new `questCompleteLines` pool; `eventLine` branches for both (now exhaustive over 11 values).
- **CurioQuests.kt** — `claimDaily` + `claimWeekly` fire `noteQuestComplete` **before** `addXp` so a claim that coincidentally crosses a level/growth tier lets the bigger moment (level-up / evolution ceremony) win instead of being swallowed; `onStreakRecorded` fires `noteStreakMilestone` inside the new-best branch (once per new best, never on same-day re-records or post-gap resets).
- **CurioFloatingPet.kt** — event-reaction mapping: QUEST_COMPLETE → `PetReactionEvents.REVEAL` (sparkle hop), STREAK_MILESTONE → `PetReactionEvents.LEVEL_UP` (spin); custom-actions `when` fires LEVEL_UP actions for both (combined `A, B ->` case).
- **docs/PET_DIALOGUE.txt** — sections 18 (quest complete) + 19 (streak milestone); **fastlane 20260919 changelog** bullet extended.

**Validation:** brace balance OK, `git diff --check` clean, code review passed (fixed two findings: quest line could swallow a coincidental evolution ceremony → moved hook before addXp; unused `streak` param removed). Known design notes: custom LEVEL_UP actions now play on 4 reward moments (deliberate — only reward-style trigger exists); write-only `KEY_LAST_QUEST_AT`/`KEY_LAST_STREAK_AT` match the existing `KEY_LAST_EVOLVE_AT` pattern. Gradle build left to CI.

## Current Request (COMPLETED): Return-after-absence welcome + evolution ceremony lines

**Date:** 2026-08-11

**What was asked:** Add the return-after-absence welcome and evolution ceremony lines.

**Changes made:**
- **CurioPet.kt** — new `Event.EVOLVE`; `noteEvolved(context)` (persists `KEY_LAST_EVOLVE_AT`, feeds `CurioPetBrain.observeLevelUp`, fires the EVOLVE event); `evolutionCeremonyLine()` (path-flavored ceremony lines via `currentStage()`/`currentEvoPath()`: Blaze/Tide/Bloom pools for the first evolution, a final-form pool, fallback); `welcomeBackLine(context)` gated on new `KEY_LAST_SEEN_AT` — first-ever appearance records the timestamp quietly, ≥1 day away returns a tiered pool line (1d / 3d / 7d), consumed once per absence; three new line pools (no em dashes per PET_DIALOGUE notes).
- **CurioQuests.kt `addXp`** — computes `stageBefore`/`stageAfter` via `evolutionStage(level, path).first`; crossing a growth tier fires `noteEvolved` *instead of* `noteLevelUp` (no double-speak, no PROUD interference) — catches the Level-25 final form.
- **PetDesignerScreen.kt** — `onPathChosen` fires `noteEvolved` after `setEvoPath` (the Level-7 first evolution; ceremony plays while the confetti `EvolutionAnimation` runs).
- **CurioFloatingPet.kt** — new `LaunchedEffect(CurioPet.awake)` queues the welcome-back line on first appearance (consumed once per absence, nap/wake cycles return null); event-reaction mapping maps EVOLVE → `PetReactionEvents.LEVEL_UP` (reuses the celebratory hop/face rule) while `eventLine` speaks the ceremony line; custom-actions `when` fires LEVEL_UP custom actions for EVOLVE (the ultimate level-up).
- **docs/PET_DIALOGUE.txt** — new sections 16 (return welcome) and 17 (evolution ceremony); **fastlane 20260919 changelog** bullet added.

**Validation:** brace balance OK, `git diff --check` clean, code review passed (exhaustiveness/scope verified; unused `stage` param removed per review). Known minor: `checkAll` stage-reward XP bypasses `addXp`, so a chain reward crossing Level 25 misses both the ceremony and level-up reaction (pre-existing quirk for level-ups; not a regression). Gradle build left to CI.

## Current Request (COMPLETED): Fix pet behavior double-fire bugs

**Date:** 2026-08-11

**What was asked:** Fix the four double-fire pet bugs: single TOUCH path, silent playful dart, grumpy threshold, level-up double-speak.

**Changes made:**
- **CurioPet.kt — `notePlay(context, react: Boolean = true)`**: `react=false` still counts the play, sets `KEY_LAST_PLAY_AT` (drives BOUNCY/PLAYFUL moods) and feeds `CurioPetBrain.observePlay`, but skips `reactTo(Event.PLAY)` so the generic play reaction can't clobber a line the play itself already carries.
- **CurioFloatingPet.kt — all 4 play sites now silent**: the post-tap dart (was overwriting the tap's gated boop line with "Wheee!" after every tap), the chameleon game, the spark-catch game, and the Pet Life routine (each queues its own line).
- **CurioFloatingPet.kt — single TOUCH path**: removed `Event.TOUCH` from the event-reaction `when` (added `else -> null` + `if (event != null)` guard). The tap handler was already the full owner (gated 40% line, tiered boop/play-bow/celebration motion, hearts, TAP custom actions); re-firing made every tap speak. `seenEvents` still increments so no reprocessing.
- **CurioPet.kt — GRUMPY threshold 6h → 45 min**: 6h was unreachable (auto-nap at 8 min idle), so the pet only ever sulked after being woken from a long absence. Now it's a real mood during a quiet/petting-heavy stretch.
- **CurioFloatingPet.kt — mood loop fires EXCITED only**: PROUD is fully owned by the LEVEL_UP event reaction (line + custom LEVEL_UP actions fire once there); the mood loop re-firing made the pet double-speak ~1.2s after a level-up and double-fire custom actions. EXCITED (new lane) has no event of its own, so the mood loop stays its only voice (keeps its pre-existing custom-action firing).

**Validation:** brace balance OK (126 files), `git diff --check` clean, code review passed (type-check path verified via smart-cast; level-up-while-asleep regression negligible since earning XP requires interaction that keeps the pet awake; dormant PLAY event/mapping left as harmless scaffolding). Gradle build left to CI.

## Current Request (COMPLETED): stop shipping authoring scripts in commits

**Date:** 2026-08-11

**What was asked:** Don't ship .py files (and per follow-up, .js files) in commits.

**Changes made:**
- Untracked 20 scripts (`git rm --cached`, kept on disk): all 17 `.py` (batch/enrich/validate authoring tools) + 3 `.js` (`check_braces.js`, `merge_topic_batches.js`, `validate_topics.js`). `.sh` setup script and `topics_inventory.txt` stay tracked.
- `.gitignore`: added `/scripts/*.py` + `/scripts/*.js` (anchored to scripts/ so no legit files elsewhere get hidden) with a note explaining the rule.
- **CI rewiring:** removed the redundant `python3 scripts/validate_topics.py` step from `.github/workflows/android.yml` — both CI workflows already run the self-contained Gradle `validateTopics` task (explicitly + via `preBuild`), so topic validation coverage is unchanged.
- Doc updates: `.github/AGENTS.md` (validation bullets now reference the Gradle task + untracked-scripts note), `.github/PULL_REQUEST_TEMPLATE.md`, `CONTRIBUTING.md`, `app/AGENTS.md` (check_braces note).

**Validation:** staged set = 20 deletions + 6 doc/config edits; `git diff --cached --check` clean; no leftover workflow python refs; code review passed (CI parity confirmed, .gitignore scoping safe). Provenance notes in SCHEMA.md/app-AGENTS.md intentionally left (historical, accurate). Gradle build left to CI.

## Current Request (COMPLETED): release tags drive the build versionName

**Date:** 2026-08-11

**What was asked:** When a release tag is pushed, the release build should use the tag's version (without the leading `v`) instead of the hardcoded `1.0.0`.

**Changes made:**
- **app/build.gradle.kts** — new `envReleaseVersion` val reads the `RELEASE_VERSION` env var, strips a leading `v` (`removePrefix`), and falls back to `"1.0.0"`; `versionName = envReleaseVersion ?: "1.0.0"`. Declared before the `android` block (safe at configuration time). `versionCode` stays date-based (store changelogs are keyed to it).
- **.github/workflows/release.yml** — job-level `env: RELEASE_VERSION: ${{ github.ref_name }}` so every Gradle invocation in the job (assembleRelease AND printReleaseVersion) sees the tag; loosened the version-parse grep to `^[0-9][0-9a-zA-Z.-]*:[0-9]+$` so prerelease tags (`v1.2.3-alpha` → `1.2.3-alpha`) still parse while stray warning lines can't match.
- **.github/AGENTS.md + app/AGENTS.md** — documented the tag-driven versionName contract.

**Behavior:** tag `v1.2.3` → APK versionName `1.2.3`, APK names `Curio-1.2.3-{versionCode}-{abi}-…`. Local dev + PR CI don't set `RELEASE_VERSION` → default `1.0.0`. No local Gradle run (repo forbids builds); YAML/diff validated; code review passed (regex tightened, same-day versionCode note flagged for future Play distribution).

## Current Request (COMPLETED): AMOLED revert (main card + hero colors) + franchise filter + versionCode fix

**Date:** 2026-08-11

**What was asked:** (1) Revert the spin-screen change that changed the main card gradient (linked c5500fd) but NOT the spin button / category button colors; (2) revert the hero colors (linked 153970c4); (3) review two PR comments: the franchise filter `.take(4)` hiding lower-count franchises, and the store notes sitting in 20260919.txt while versionCode was still 20260918.

**Commit survey finding:** c5500fd's SpinScreen changes were only the spin button + deck pills (both KEPT per the user's instruction). The main-card changes actually live in b351b42 (fill went black) and 6d2f135 (accent edge-shine + border). User chose: keep the black-glass main card but with a SUBTLE category shine tint ("not full, a little").

**Changes made:**
- **CategoryEdgeShine.kt** — added `intensity: Float = 1f` param (clamped 0..1) scaling hairline/top-shine alphas; default keeps every other call site identical.
- **SpinScreen.kt** — AMOLED main card edge shine at intensity 0.55f and border accent alpha 0.35 → 0.16 (quiet category tint on black glass). Removed the `.take(4)` cap on the franchise filter row — all 8 FranchiseTags (MCU, Star Wars, DC, Harry Potter, LOTR, Pixar, Studio Ghibli, Disney) are now always exposed; eras/genres caps untouched.
- **HomeScreen.kt** — reverted 153970c4's hero colors: `homeRoseAccent()` AMOLED back to grey-coral `lerp(surfaceContainerHigh, primary, 0.16f)`, watermark symbols back to `questInk` (symbolTint removed), stat pane back to the plain heroFill gradient, "Surprise me" button ink back to `CurioColors.DeepPlum`.
- **app/build.gradle.kts** + **app/AGENTS.md** — versionCode 20260918 → 20260919 (matches the existing 20260919.txt store notes).
- **Changelog 20260919.txt** — main-card line updated to describe the quiet shine.

**Validation:** braces + git diff --check clean; spin button / deck pills confirmed untouched (0 diff matches); code review passed (intensity clamp added, franchise render path verified wrapping, no stray 20260918 refs left). Gradle build left to CI per repo rules.

## Current Request (COMPLETED): exploration-only instructions + per-category shades

**Date:** 2026-08-11

**What was asked:** Some explore instructions told users to MAKE things (cook, play, fold, build) instead of exploring. Also: each new category should get its own distinct shade, Artists vs Albums should differ just a little, and every new color needs light/dark/pastel variants.

**User answers (ask_user):**
- Food → read/watch history + origin; Games → fun fact + read + watch on YouTube, "if they like it they can play something like that".
- Scope: ALL categories.
- Shades: clearly distinct hues for new categories; artists/albums just a little.

**What shipped (commit 9eb550a → follow-up):**
- `scripts/rewrite_explore_instructions.py` — idempotent (verb-guarded) rewrite of **170 instructions**: 77 food (Cook→Read, history + origin + watch), 65 games (Play→Watch, fun fact + YouTube + optional similar game), ~28 strays across painters/scientists/wildcard/internet (Try/Make/Design/Draw/Solve/Fold/Craft/Build/Write/Play/Cook → Read/Watch/Look at). SCHEMA.md verb contract updated to forbid making verbs.
- CurioColors.kt — per-category accents: CategoryAlbum #5F4DCB (subtle indigo twin for Albums vs Artists #4338CA), CategorySong #0E7490 cyan, CategorySeries #BE185D magenta, CategoryManga #5B21B6 deep violet, CategoryManhwa #9333EA orchid — each with ink twin + 20% tint; pastel auto-derives from accent hue, dark washes per existing family tuning.
- Category.kt — ALBUMS/SONGS/SERIES/MANGA/MANHWA entries use the new tokens.
- CaptureEntity.kt fallback verbs GAMES→Watch, FOOD→Read; ExploreSession reflectionQuestion "Finished playing?"→"Finished watching?", "Done cooking?"→"Done reading?".
- Validation: 6480 topics / 0 errors; braces + diff clean; JSON diffs scoped to exploreAction only (trailing newline restored).

**Notes:** Mixed-deck curated blend tables only cover the six original accents — new shades fall back to runtime HSL blends (acceptable; revisit if a Songs+Series deck looks off). Idempotency guard = `verb != Cook/Play` early return.

### What the user asked
Expand the topic database with fun stuff (franchises etc.), add an MCU-like franchise filter (not too many), add new categories — Anime, Songs, Manga, Manhwa — and suggest more.

### Decisions (asked via ask_user)
User picked **10 categories total** (Anime, Songs, Manga, Manhwa + Games, TV Series, Mythology & Legends, Sports, Food & Drink, Internet Culture), **~60 topics each this drop** (idempotent scripts make topping up to 100 trivial), and franchises **MCU, Pixar, Studio Ghibli, Harry Potter** (plus the existing Star Wars/DC/LOTR/Disney tags already in `FranchiseTags`).

### Code changes
- **Category.kt** — 10 new `CategoryId` values + routeSlugs + defaultOrder; 6 new `CategoryFamily` values (ANIME_COMICS, GAMES, MYTHOLOGY, SPORTS, FOOD, INTERNET); exhaustive `CategoryFamily.of()` updated; 10 new `CurioCategory` entries (icon glyphs font-verified against the bundled `material_symbols_outlined.ttf`).
- **CurioColors.kt** — new accent/ink/tint pairs: Violet (anime/comics), Fuchsia (games), Emerald (sports), Orange (mythology), Red (food), Blue (internet).
- **CurioIcons.kt** — `heroWatermarkSymbols()` gained branches for the 6 new families (only exhaustive `when(family)`).
- **CategoryInk.kt** — `DARK_WASH_TUNING` gained tuned entries for the 6 new families (dark washes stay jewel-toned over midnight; map has safe fallback).
- **SpinScreen.kt** — `FranchiseTags` set (MCU, Star Wars, DC, Harry Potter, Lord of the Rings, Pixar, Studio Ghibli, Disney); `FilterGroups` + `buildFilterGroups` + `FilterSheet` gained a **Franchise** row.
- **CaptureEntity.kt** — fixed the exhaustive `when (categoryId)` verb fallback (was the #1 CI compile breaker — new enum values made it non-exhaustive).
- **ExploreSession.kt** — fixed the exhaustive `when (categoryId)` in `reflectionQuestion()` with new-category branches.
- Byline-label `when`s in TopicReveal/Spin have `else -> null` — safe untouched.

### Data changes
- ~640 hand-curated topics across 10 new JSON files: anime 61, manga 75, manhwa 64, songs 60, series 60, games 65, mythology 60, sports 59, food 77, internet 61. Every topic has the quality bar (quirky teaser + personalized explore instruction) and full tags.
- **Franchise tags** — `scripts/add_franchise_tags.py` tagged 76 films (MCU/Star Wars/DC/Harry Potter/LOTR/Pixar/Disney); anime already carried Studio Ghibli.
- **Parked enrichment DONE** — `scripts/enrich_science_tags.py` ran: 591 tags added (scientists 370 + discoveries 221); no untagged topics remain in either lane.
- **SCHEMA.md** — 21 files, new subtypes/verbs (Play, Cook), franchise-tag note.
- **validate_topics.py** — EXPECTED_CATEGORIES now includes the 10 new slugs.
- New idempotent batch scripts: `batch_anime_comics.py`, `batch_comics_topup.py`, `batch_songs_series.py`, `batch_media_topup.py`, `batch_culture_topup.py`, `add_franchise_tags.py`.

### Validation
- `validate_topics.py`: 21 files / 6,480 topics / 6,480 unique ids / **0 errors**. check_assets.py ALL VALID. Braces + `git diff --check` clean. Code review passed (exhaustive-when sweep caught the two fixed files).
- Gradle compile/build/lint/test not run locally (environment forbids it) — CI validates on push.

## Current Request (COMPLETED): Material theme → coming soon; AMOLED main card accent restored

**Date:** 2026-08-10

### What the user asked
Grey out the Material theme option and mark it "coming soon". Also: the AMOLED main Spin card used to have a beautiful, sleek category-color accent — check if it's still there and add it back.

### Changes made
- **SettingsSectionScreen.kt** — `CompactSegmentedRow` gained `disabledIndices: Set<Int> = emptySet()` + `disabledHint: String? = null` (defaults keep the Theme row and any other call site unchanged). The Theme style row now disables the Material segment (`disabledIndices = setOf(2)`) and shows a small clock-glyph + "Material theme · coming soon" caption in onSurfaceVariant. M3's disabled SegmentedButton greys the segment automatically; the Material style code path itself is untouched (it simply can no longer be picked). Note: a user who already has Material selected keeps it until they pick Curio/AMOLED — left self-resolving (no silent theme reset).
- **SpinScreen.kt (HeroTicketCard)** — the AMOLED main ticket now wears the same black-glass CATEGORY SHINE as the settings cards / deck pills: `Modifier.categoryEdgeShine(RoundedCornerShape(30.dp), accent)` on the clipped Box (accent hairline around the edge + a soft 18dp accent band at the top). The Surface hairline border in AMOLED carries the deck accent at a restrained 0.35 alpha (down from the uncommitted 0.55 — the edge shine is the primary accent carrier, so the card stays sleek rather than stacking two loud rims). This was previously uncommitted, which is why the user didn't see it on device.
- **fastlane 20260919.txt** — added a store-changelog bullet (Material on hold + AMOLED card accent).

### Validation
- Brace check + git diff --check clean; code review passed (segment disable + caption compile-safe; edge shine clipped/gated to AMOLED, non-AMOLED themes untouched).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

### Parked (previous request, still pending)
Adding missing tags to scientists/discoveries: `scripts/enrich_science_tags.py` is written and dry-run validated (word-boundary field matching, element-isolation rule, CE/BCE era parsing, ~180 curated origin/era overrides). Remaining: full-data audit run (the full 501-scientist set has ~60 more names with no era signal that need overrides), then run the script against the real JSON and commit.

## Current Request (COMPLETED): Home hero stat shows total topics instead of recents count

**Date:** 2026-08-10

### What the user asked
On the Home page, change the stat that shows the recent count to show the total number of topics the app has.

### Changes made
- **TopicCatalog.kt** — new `totalTopicCount()`: sync sum of the ten non-wildcard lanes' cached pool sizes (the catalog is warmed during splash, so it's ready on the Home's first frame; an uncached lane contributes 0 until loaded). Wildcard excluded — it only mirrors the canonical lanes.
- **HomeScreen.kt** — the hero stat bar's third segment now shows `TopicCatalog.totalTopicCount()` with label "Topics" and the AutoAwesome glyph (was the recent-feed size, "Recent", History glyph). The recents feed variables stay used by the Recents section below. Works in promo mode too (shows the real total).
- **PromoMode.kt** — `topicTotal()` now delegates to `TopicCatalog.totalTopicCount()` (was a duplicated cached sum) so the two can never drift.

### Validation
- Brace check + git diff --check clean; code review passed (no dead code, imports verified, DRY consolidation).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): AMOLED unselected Categories/Filter pills → pure black

**Date:** 2026-08-10

### What the user asked
Make the unselected Categories/Filter pills pure black in AMOLED instead of dark grey.

### Changes made
- **SpinScreen.kt** — `deckControlSurface()` now returns pure `Color.Black` for the AMOLED style (unselected pills were falling through to `categorySurface(...)` → the dark-grey `surfaceContainerHigh`). Both pill variants (`DeckControlButton` horizontal + `VerticalDeckButton` extra-compact) share this helper, so one edit covers every unselected pill.
- `deckControlBorder()` adds an AMOLED branch — a quiet 1dp accent hairline (`categoryInk()` at 0.28 alpha, the light accent in dark) so the pure-black pills stay distinct from the pure-black Spin page (without it they'd be invisible). Selected pills were already black with the accent rim from the earlier AMOLED pass.
- Material / Curio branches untouched.

### Validation
- Brace check + git diff --check clean; code review passed (both variants covered, no new imports, Material/Curio unchanged).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): AMOLED Home quest banner → pure black with rose accent

**Date:** 2026-08-10

### What the user asked
Make the Home quest banner pure black with the rose accent in AMOLED (the last hero still wearing the grey-coral tint).

### Changes made
- **HomeScreen.kt** — `homeRoseAccent()` AMOLED branch now returns pure `Color.Black` (was `lerp(surfaceContainerHigh, primary, 0.16f)`), matching the Profile/Settings heroes. Text stays white via the existing `homeReadableInk` → AMOLED `onSurface`.
- The quest hero carries the rose accent on the black plate: new `symbolTint` (`CurioColors.HomeRosewood` in AMOLED, `questInk` otherwise) tints the watermark symbols, and the stat pane's AMOLED gradient is a rose glow (0.30 alpha → subtle rose wash over black) instead of the black-on-black wash.
- **Cascade (consistent with the pure-black style):** QuestShuffleCard's casino button + eyebrow, the drawer hero, and the sticky top-bar pills all share `homeRoseAccent()` → black, all ink already resolves white/readable.
- **FirstTimeEmpty fix** — the "Surprise me" button hardcoded `DeepPlum` ink, which vanished on the now-black AMOLED plate; it now uses `homeReadableInk(roseAccent)` (white in AMOLED, readable deep rose in light pastel, onSurface in light non-pastel). All `CurioColors.DeepPlum` references in HomeScreen are gone.

### Validation
- Brace check + git diff --check clean; code review passed (cascade ink readability, no unused imports).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Spin orbit dots white in non-pastel light + loop skips

**Date:** 2026-08-10

### What the user asked
In non-pastel light mode the spin button's outer small dots look white while animating, and the orbit animation feels like it skips / isn't a real loop. Fix if confirmed.

### Diagnosis (confirmed from code)
- **White dots:** `OrbitRing`'s dot color used `pastelFillInk(color)`, which returns **white whenever pastel mode is OFF** — so non-pastel light mode lit the 10 orbit dots as a bright white necklace (the layered bloom read even whiter). Pastel mode returns deep colored ink, which is why only non-pastel showed it.
- **Skipping loop:** the dot ROTATION was already a seamless 0→360° loop, but the shimmer phase was keyed to the raw rotation angle (`sin(rotRad * 1.4 + i * 1.15)`), so the brightness wave spun around the ring at **1.4× the physical rotation** — a strobe-like moiré that fought the dots and read as skipping/stuttering, never a smooth loop.

### Changes made
- **SpinScreen.kt (OrbitRing)** — dot color now has a non-pastel LIGHT branch that deepens the accent via `deepHueInk(color)` (deep same-hue ink) instead of white; Material-light still uses device onSurface; dark/pastel unchanged (white / light-tint / deep-hue respectively).
- **SpinScreen.kt (OrbitRing)** — shimmer phase is now keyed to each dot's ABSOLUTE angle (`absAngle = a + rotRad`), so the brightness wave travels WITH the ring; the pattern is rotation-periodic, so the 360° wrap is invisible and the loop reads as one continuous orbit.
- **CategoryInk.kt** — `deepHueInk` promoted private → internal (shared with SpinScreen's orbit dots; no other callers changed).

### Validation
- Brace check + git diff --check clean; code review passed (HSL math, branch ordering, wrap seamlessness all verified).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Full Material theme revamp (last chance)

**Date:** 2026-08-10

### What the user asked
The Material theme is still ugly — category cards, the header, all of it — and needs a full revamp for pastel + non-pastel + dark mode colors, or the style will be removed.

### Changes made
- **CurioTheme.kt** — `calmMaterialColorScheme` rebuilt as a HUE-LOCKED palette: the device wallpaper's identity is kept as a single hue (from `dynamic.primary` via `toHsl`), and every scheme role is BUILT from that hue with Curio-tuned saturation/lightness (`fromHsl`); secondary/tertiary are the same hue family offset ±38°. No more raw dynamic colors (brown wallpapers rendered dull olive-grey). Light: near-white surfaces with a hue whisper, deep vivid primary, airy primary-container. Dark: deep tinted midnight from the same hue, bright readable primary, light same-hue container ink.
- **CurioColors.kt** — Material no longer gets device-color + faint category "whisper" gradients; `cardGradient`/`heroBlendGradient`/`mixedDeckGradient` all wear the SAME rich category gradients as Curio/AMOLED (material identity lives in the hue-locked scheme surfaces/heroes, not desaturated cards). Removed the now-dead `materialDeviceStop` + `floorForWhiteInk`/`WhiteInkLightnessFloor`.
- **CategoryInk.kt** — `themedButtonFill()` = true category accent (`themedAccent()`) everywhere; `themedButtonInk()`/`cardContentInk()`/`onAccent()` Material branches use the pastel-aware ink (white on deep, deep same-hue ink on airy pastels, light-tinted in dark pastel) with a `deepHueInk` guard for pale accents (wildcard coral) off pastel mode — fixes the review-found white-on-pink wildcard regression. `categoryBorder()` adds a quiet Material accent hairline when the tint toggle is off.
- **CurioCategoryCard.kt** — Material idle tiles lerp `surfaceContainerHigh` toward the category's themed accent (0.12 dark / 0.14 light) instead of plain device-grey; selected cards keep the full rich gradient + accent edge shine.

### Validation
- Brace check + git diff --check clean; code review passed (wildcard pale-accent ink fix applied; HSL scheme contrast checked).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): AMOLED — pure-black Profile/Settings heroes + black accent buttons

**Date:** 2026-08-10

### What the user asked
In AMOLED, don't give Profile and Settings that tint — make them pure black with the accent. Make the header black too with an accent of the color. Also change the Spin button and the category picker button colors.

### Changes made
- **SettingsHubScreen.kt / ProfileScreen.kt** — `settingsRoseAccent()` / `profileRoseAccent()` AMOLED now return pure `Color.Black` (was `lerp(surfaceContainerHigh, primary, 0.16f)` grey-coral tint).
- Hero headers carry the rose accent on the black plate: watermark symbols + back pill tinted `CurioColors.HomeRosewood` in AMOLED (new `symbolTint`); Profile's stat-bar gradient pane uses a rose 30% pane in AMOLED. Titles/content stay white.
- **SpinScreen.kt** — SpinButton plate is pitch-black in AMOLED (`plateTint`): the category accent moves to the orbit ring, edge-shine rim and the 3D sheen (faint accent-tinted highlight instead of the white cap); the dice stays white. Selected Categories/Filter deck pills (`DeckControlButton` + `VerticalDeckButton`) are pitch black with the accent rim in AMOLED instead of the bright accent fill.
- **CategoryPickerScreen.kt** — the Mix button's AMOLED content flips to white (`onSurface`) — `curioButtonColors` already forces the plate black, but the old `onPrimary` (deep maroon) vanished on it.
- Cascade: Onboarding/Cabinet share `settingsRoseAccent()`, so their heroes go black in AMOLED too (consistent with the pure-black style).

### Validation
- Brace check + git diff --check clean.
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Topic catalog not loaded from startup (0 counts / spurious loading)

**Date:** 2026-08-10

### What the user asked
The dataset doesn't load reliably: the category picker shows 0 topics per category (counts only appear after opening a category), and some places show a loading state they shouldn't. Topics should be available and loaded from startup.

### Root cause
Regression from `9ead01a`/`180cbfd`: splash navigation was NOT held hostage by parsing — a concurrent preload raced the 800ms auto-dismiss, so Home rendered with a half-warm cache. `CurioCategoryCard` read only `TopicJsonLoader.cached()?.size ?: 0` inside a latched `remember`, so uncached lanes showed "0 topics" until reopened; Topic Database flashed "Loading topics…" while lanes warmed.

### Changes made
- **SplashScreen.kt** — splash now HOLDS navigation until the canonical catalog is warm: preload runs on `Dispatchers.Default` while the 800ms branding plays, then `withTimeoutOrNull(6s) { warmCatalog.join() }` before navigating (hard cap; per-lane `load` rethrows `CancellationException` so the timeout actually aborts). Individual lane failures are swallowed so one broken asset never blocks the rest.
- **CurioCategoryCard.kt** — topic count is now a `produceState` (seeded from cache, reloads on demand, cancellation-aware) instead of a latched `remember` — cards can never pin a stale "0 topics" after e.g. an `onTrimMemory` cache clear.
- Topic Database / Spin: no changes needed — with the warm cache their loading states only trigger for genuine work.

### Validation
- Brace check + git diff --check clean; code review passed.
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Topic Reveal plain bottom band

**Date:** 2026-08-10

### What the user asked
On the Topic Reveal page: remove the tear style from the bottom (keep it plain and theme-aware) and move the tags down a little.

### Changes made
- **TopicRevealScreen.kt** — removed the torn seam entirely: dropped the `SoftTornBottomShape` clip + 180° rotation from the bottom strip so it's now a flat, theme-aware band (unchanged `bandPaper`: Curio category surface / Material surfaceContainer / AMOLED surface). Removed the now-unused `REVEAL_BOTTOM_TEAR_SEED` constant and `SoftTornBottomShape`/`graphicsLayer` imports; renamed `RevealBottomTearHeight` → `RevealBottomBarHeight` (same 80dp footprint) and `tearPaper`/`tearInk` → `bandPaper`/`bandInk`.
- Tags row moved down a little: top inset 16 → 24dp inside the bottom band; comments updated to describe the plain band.
- **CurioNavHost.kt** — comment-only: reveal references now say "plain bottom band" instead of "torn paper edge/sheet".

### Validation
- Grep-verified: no stale references to the removed/renamed symbols anywhere; `graphicsLayer`/`SoftTornBottomShape` unused in the reveal file.
- Code review passed (imports, rename consistency, band geometry math unchanged).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Pet speech bubbles skip on interaction

**Date:** 2026-08-10

### What the user asked
When interacting with the floating pet (tapping it, or doing other things in the app), the pet doesn't skip its current dialog to react — the bubble stays and cycles through all queued lines. It should skip in some places (not always): direct interactions should dismiss/skip the current line and answer immediately.

### Changes made
- **CurioFloatingPet.kt** — added `speakNow(line?)`: interrupts whatever bubble is showing (skips it via re-keying the bubble lifecycle) and clears the queued backlog. A null line dismisses the bubble silently (the pet's motion is the reaction).
- Taps now call `speakNow` (with or without a line) instead of `queueReaction` — the pet answers the tap immediately and drops queued chatter.
- Drag end ("Home sweet home!", dizzy line) and long-press ("Home sweet home!") also use `speakNow`.
- `fireReaction`'s event lines (spin landed, reveal, explore, save, play, level-up) now `speakNow` — real user-driven events skip the current bubble instead of queuing behind it (null lines leave the bubble alone).
- `queueReaction` (ambient wander/peek/games/typing/custom action chatter) is now CAPPED to the latest 2 lines, so the pet can never cycle through a long backlog of stale lines; it repeats the last one or two then falls quiet.
- Tour dialogue is untouched (separate `tourStep?.dialogue` path, never interrupted).

### Validation
- git diff --check clean.
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Material theme buttons, pastel dialogs, reveal footer polish

**Date:** 2026-08-10

### What the user asked
Fix the Spin button and category button in the Material theme (light + dark), make the Material theme fully Material (nothing foreign left), make pastel-light dialogs match the screen tint + card shape (like the Topic Reveal dialog) with darker readable text, and simplify the Topic Reveal bottom strip: plain no-design tear, theme-aware, tags a little lower, no off-screen overflow on small screens, footer height unchanged.

### Changes made
- **CurioTheme.kt** — added shared dialog theme: `CurioDialogShape` (24dp card-matching), `curioDialogContainerColor()` (light mode blends toward the cream background so dialogs melt into pastel pages; Material/dark keep scheme surfaces), `curioDialogActionColor()` (deep same-hue rose ink in light for readable buttons; device primary in Material/dark), `curioDialogActionButtonColors()`.
- **SpinScreen.kt** — Material style: Spin dice glyph uses device onPrimary in dark (was white-on-light = invisible), orbit dots use onSurface in light (white dots vanished on the wash), Categories/Filter selected label pairs with the icon's themedButtonInk (was mismatched onPrimaryContainer), unselected pills wear device surfaceContainerHigh + outlineVariant instead of category tint; FilterSheet + CategoryPickerSheet wear device surfaceContainerLow in Material.
- **TopicRevealScreen.kt** — bottom strip now uses the plain `SoftTornBottomShape(seed)` (no bold/detail lip), stays theme-aware, footer height unchanged (80dp); tag chips moved down (10→16dp inset) so they clear the seam and never run off small screens; all 3 reveal dialogs use the shared dialog theme.
- **Dialog pass (24 AlertDialogs, 12 files)** — every AlertDialog now passes `containerColor = curioDialogContainerColor()` + `shape = CurioDialogShape`, and action TextButtons use the readable deep-rose ink; AudioQualityDialog radio/border also use it; filled Save-and-switch buttons use `curioDialogActionColor()`.

### Validation
- All 24 AlertDialog sites updated (grep counts verified), imports verified per file, no duplicate shape params, git diff --check clean.
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Theme-aware Topic Reveal footer

**Date:** 2026-08-10

### What the user asked
Make the newly added Topic Reveal bottom torn strip useful without increasing its height. The strip and tear should be opaque and theme-aware across Curio, AMOLED, and Material styles, with category tint support. Move topic tags into the top of the footer if possible.

### Changes made
- Kept the existing fixed 80dp footer geometry and reserved navigation inset unchanged.
- Made the torn strip fully opaque and selected its surface from the active appearance: category surface for Curio, Material surface container for Material, and AMOLED surface for AMOLED.
- Reused the same resolved surface for the torn edge so the seam remains visually continuous in each theme.
- Moved the existing topic tags from the reveal body into a compact single-line footer row, capped at three tags with ellipsis-safe text and no height expansion.
- Preserved existing reveal actions and interactions.

### Validation
- Brace checker passed for TopicRevealScreen.kt.
- git diff --check passed.
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.
- Review the diff before commit/push.

### Follow-up
Refine the footer tear so it projects slightly farther and improve the footer tags into clearer pill chips without increasing the fixed height.

## Current Request (IN PROGRESS): Refine Topic Reveal tear and tags

**Date:** 2026-08-10

### Changes made
- Increased the tear's visible irregularity using the existing detail geometry mode while preserving the fixed footer height.
- Strengthened footer tag pills with a clearer category-tinted fill and outline.

### Validation
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.
- Run `git diff --check` and the lightweight source checks before commit/push.

## Current Request (IN PROGRESS): Refine explore dialog and expression action

**Date:** 2026-08-10

### Changes made
- Removed the redundant `Not now` action from the Explore dialog; outside-tap/back dismissal remains available.
- Grouped Google and YouTube choices together in the dialog action area.
- Added a theme-aware outlined pill surface to `Express yourself`, including disabled-state contrast.

### Validation
- Run `git diff --check` and lightweight source checks.
- Do not run Gradle build/lint/test commands per repository rules.
