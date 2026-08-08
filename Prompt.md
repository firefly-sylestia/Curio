# Request — v8.27: richer daily quests, pinned badges, fun reward moments + CI fix

## What the user asked

1. Daily quests had only 3 quests and not much XP — make them more.
2. Improve the quest system further: pinned badges, and make it actually
   fun with animations for things.
3. (Later message) Fix a CI compile error (CurioPet JVM signature clash),
   then continue the quest work.

## Decisions (asked via ask_user)

- **5 dailies/day**: three CORE quests shown first; claimed core quests hide
  (animate out); once all three are claimed, the two BONUS quests take over
  with a gold "Bonus quests unlocked!" reveal. User chose: "5 per day and
  show it as bonus after the 3 completes hide the completed and show that."
- **Pinned badges**: earned medal badges on the Profile achievements card AND
  an earned-badge strip on the Quests page (locked ones shown as
  silhouettes, per spec §4.1). Badge shelf dialog kept behind the strip.
- **Always-on** (no Settings toggle).

## Implementation

### CI fix (pushed separately as `7efcbcc`, then `e617bc9` prior)
- `CurioFloatingPet.kt` — the throw-glide `launch` ran on a plain lambda; the
  previous `inputScope = this` fix was wrong for Compose 2026.05
  (PointerInputScope is no longer a CoroutineScope). Now uses
  `rememberCoroutineScope()`.
- This turn: `CurioPet.kt` — `var floatingSuppressed by mutableStateOf(false)
  private set` already generates a JVM `setFloatingSuppressed(Boolean)`, so
  the manual `fun setFloatingSuppressed(...)` clashed. Renamed the manual
  function to `suppressFloating(...)`; updated `OnboardingScreen.kt` (2
  callers) and the KDoc reference.

### Data layer (`data/CurioQuests.kt`)
- `DailyQuest` gains `bonus: Boolean` (v8.27).
- DailyPool: 8 core quests (one per role; warm-up/discovery/creation picked
  deterministically, never same-kind on one day) + 7 bonus quests
  (25-40 XP). Core rewards raised ~50%: warm-up 15-20, discovery 30,
  creation 15-25. A full day now pays ~120-140 XP (was ~45).
- `dailyQuestsFor` returns core trio + two bonus (picked, never repeated).

### Shared badge components (`ui/components/CurioBadges.kt`, NEW)
- `badgeGlyph(stage)` — every stage wears its own glyph (fallback by kind).
- `chainBadgeColor(chainId)` — per-chain medal color.
- `CurioBadgeMedal(stage, medalSize)` — round medal, gradient + gold check +
  white glyph when earned; silhouette when locked.
- `CurioBadgeStrip(earnedLimit, lockedPreview, onViewAll, emptyText)` —
  earned medals first, "+N" tile when more, locked silhouettes after.

### Quests screen (`features/quests/QuestsScreen.kt`)
- DailyCard: header "N of 5 done · Resets at 4 AM"; claimed core rows animate
  out; bonus rows take over when coreDone with gold sparkle line; per-claim
  "+N XP" chip floats up; "All done today!" when all 5 claimed.
- DailyQuestRow: animated mini progress bar, gold accent + BONUS tag +
  sparkle glyph for bonus quests, pulsing Claim pill when ready.
- Live badge-unlock toast (medal + pet hop) when a chain badge earns while
  the page is open; level-up now also rains ConfettiBurst.
- PathsCard gains an on-page CurioBadgeStrip (earned first, +N → shelf
  dialog).

### Profile (`features/profile/ProfileScreen.kt`)
- ProgressAndAchievementsCard's inline chip FlowRow replaced with the shared
  CurioBadgeStrip (earned medals pinned, locked silhouettes, tap → Quests).
  Removed the now-unused `FlowRow` import.

### Docs
- `docs/app/QUEST_AND_PET_REDESIGN_SPEC.md` §5.1 — v8.27 amendment (5 quests,
  bonus unlocks, reward raise).
- QuestsScreen header KDoc: "three daily quests" → five (3 core + 2 bonus).
- New store changelog `20260815.txt`.

## Validation
- Brace balance + `git diff --check` pass on all 6 touched Kotlin files.
- No stale `setFloatingSuppressed` references; `FlowRow` gone from Profile.
- No em dashes in the new user-facing strings.
- CI on push is the compile gate (no Android SDK in this workspace).
