# Prompt — FieldMind/Curio request log

## Active request: Curio pet + Quests redesign (spec v8.5)

### What was asked
Create a pixelated cute unique pet for the app with its AI and animations, plus a
full Quests redesign (past spec: daily-first IA, category passport, First Journey
tutorial, reward polish). User decisions (ask_user): **whole spec, phased** ·
pet **toggleable, default ON** · placement **Quests hero + Home corner** ·
**spark-spirit** design approved · **local rule-based AI** (no server).

### Delivered this pass (Phases E, A, C + Phase D core)

**Phase E — the pet (headline ask):**
- `data/CurioPet.kt` — the pet's BRAIN: 6 growth stages (Hatchling Spark → Curio
  Sage) derived from existing CurioQuests XP/saves/explored-lanes; 5 moods
  (PROUD/EXCITED/HAPPY/CURIOUS/SLEEPY) from activity timestamps (never shaming);
  rule-based dialogue engine ("local AI") — one passive sentence per screen visit,
  one-shot bubble cooldown, tap check-in dialog (mood + next quest + growth hint).
- `ui/pet/CurioPetSprite.kt` — the pixel sprite, rendered 100% in Compose (16×16
  pixel-grid Canvas — no bitmaps): round cream spark-spirit, gold star-tipped
  antenna, category-accent scarf, big eyes/cheeks. Animations: idle bob, periodic
  blink, sleep breathing + floating Z's, excited wiggle + sparkle eyes, one-shot
  celebration hop (keyed), stage-gated accessories (sprout leaf / satchel / book /
  aura / gold halo), theme-aware ink (light twin in dark). Param named
  `spriteSize` (not `size`) per project compile-safety rule #7.
- `ui/pet/CurioPetCompanion.kt` — PetSpeechBubble (readable Text, one-liner) +
  CurioPetHeroCard (pet in an XP ring, level + growth line, XP bar, next-up quest,
  tap → check-in dialog with "Go to quest").
- `AppPreferences.petEnabledState` (default ON) + **Appearance settings toggle**
  "Curio pet" (off = classic layout, pet fully hidden).
- CurioQuests XP hooks feed the pet's mood timestamps (level-up → PROUD).

**Phase A — Quests IA:**
- Quests screen now: **pet hero (or classic level card when toggled off) → daily
  quests FIRST → recommended next quest card (retitled "RECOMMENDED NEXT") →
  category passport → quest paths (chains COLLAPSED by default, expandable) →
  badge shelf** (spec §3 + §4.1).
- Daily claim → pet celebration hop + confirm haptic (Phase D core).

**Phase C — category passport:**
- `data/CurioPassport.kt` — per-category spins/reveals/explores/saves counters +
  last-explored; stamps UNSEEN/PEEKED/EXPLORED/MASTERED; `leastEngaged()` for
  discovery; own SharedPreferences file (needs CurioBackupManager listing).
- Hooks at the real action sites: SpinScreen settle (noteSpin), TopicRevealScreen
  open (noteReveal), CurioQuests.onExplore (noteExplore), SaveCaptureScreen new
  save (noteSave).
- Passport stamps UI on Quests: 2-row grid, tappable → spins that lane
  (`CurioRoutes.spinWithCategory`); mastered = sage, unseen = enticing accent.

**Home pet corner:** small pet at the head of the "TODAY'S QUEST" summary
(QuestShuffleCard `pet` param), same moods, never tappable (spec §10.3).

### Not yet done (later phases)
- **Phase B** — deep "First Journey" coach-bubble tutorial with real-action waits
  through reveal→explore→capture→save. The existing QuestGuide already walks real
  screens and waits on the real spin; the 10-step loop tour is a follow-up.
- **Phase D extras** — XP-chip flight animation + full level-up celebration overlay
  (pet hop + proud mood + haptic already in).
- Passport + pet prefs registered in `CurioBackupManager` (curio_pet +
  curio_passport) so stamps/pet state ship with the user's backup.

### Validation
- Brace balance equal on all 12 touched files (script's "UNBALANCED" label is a
  quirk — pristine files print the same with equal counts), `git diff --check`
  clean, every added import used, `spriteSize` rename applied at all call sites.
- No Gradle builds in this environment (project rule) — CI validates on push.

### Next
- Code review, commit + push to Alpha (updates PR #3).
- User builds/tests; then Phase B + Phase D extras as the next pass.
