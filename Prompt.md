# Prompt — Curio request log

## Active request: finish remaining spec phases + declutter the Quests page

User asked to continue the Quest & Pet Redesign spec (docs/app/QUEST_AND_PET_REDESIGN_SPEC.md),
then interrupted with: "remove the unnecessary quest card boards — many things in quest
boards feel like placeholders with no real interactions, it feels bland."

### This pass (committed to `Alpha`, PR #3)

**Phase B — First Journey tutorial (real-action core loop, spec §7):**
- `data/QuestGuide.kt` rewritten: 9-step tour Home → Quests → Spin → open the landed
  topic → Start exploring → Capture/Save → Cabinet → Reward & pet growth → done.
  New `Wait.REVEAL`; action-wait steps (`hold = true`) never yank the user mid-flow,
  they advance the moment the real action happens (spec §7.3). Persistence:
  `seed(context)` in MainActivity onCreate, `persist(context)` from the NavHost
  runner — the tour survives process death.
- `CurioNavHost.kt` tour runner is now hold-aware (only guides hold-step users back
  when parked on a bottom-nav tab) and persists every step change.
- `QuestGuideToast.kt` gains `actionEnabled` — wait steps show "Do this to continue",
  muted colors, no-op action.
- `TopicRevealScreen.kt` fires `QuestGuide.onWait(Wait.REVEAL)` when a reveal opens.

**Daily quests (spec §5/§6.2):** `CurioQuests` adds `DailyKind.DISCOVERY`; each day
now picks warm-up + discovery + creation (discovery names the passport's least-engaged
lane on the DailyCard and its Go chip routes straight to that lane's Spin deck;
dropped when no lane target). `onExplore` completes the discovery daily only when the
explored lane IS the passport's least-engaged lane. `DailyPool.first` → `firstOrNull`
(robust).

**Phase D — level-up celebration:** a claim that crosses a level raises a skippable
banner (pet hops, PROUD mood) that auto-dismisses after ~2.5s.

**Quests declutter (user request):** page is now hero → daily quests → recommended
quest → category passport → ONE "Quest paths" card. All per-chain cards merged into
tappable rows (tap to expand the stage trail; the next actionable stage carries a Go
chip); the permanent badge grid moved behind a single "Badge shelf" row that opens a
dialog. Nothing on the page is a dead display board.

### Validation
- String-aware brace/paren balance: ALL OK on all touched files (the old
  `/tmp/check_balance2.py` miscounts strings containing `//` — replaced with
  `/tmp/check_balance3.py`).
- `git diff --check` clean. Code review applied: `delay` import + banner autodismiss
  verified, `onWait(REVEAL)` confirmed inside LaunchedEffect, BadgeTile takes
  modifier, DISCOVERY bump gated, no orphaned imports, `firstQuestId` still used,
  stray `result` symlink deleted. CI validates the real build on push.

### Still open (later phases)
- Phase F: pet per-screen reactions (Spin watches the wheel, reveal points at
  Explore, Cabinet celebrates) — optional.
- Passport read hoisting (leastEngaged called in DailyCard composition; harmless,
  SharedPreferences is memory-cached).
