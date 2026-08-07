# Prompt — Curio request log

## Active request: v8.15 — pet-guided tour + quest-navigation fix (uncommitted)

Working tree (5 files — 1 new overlay + 4 edits; NOT pushed — user wants to
be asked before pushing):

**A. Quest navigation fix (the Home-tab dead tap)**
- `CurioRoutes.navigateToTab` now pops back to the HOME root explicitly when
  the current route is NOT an exact tab route ("home"/"spin"/"cabinet").
  Scenario: Quests (pushed over Home) → passport stamp or discovery daily
  "Go" pushes "spin/{slug}" on top → tapping the Home tab ran
  popUpTo(HOME)+launchSingleTop, and once HOME was the top after the pop,
  singleTop cancelled the navigate — the tap looked dead (user had to back
  out to Quests first). The explicit pop guarantees every tab tap from a
  pushed screen lands. Normal tab switches (exact tab roots) are unchanged.

**B. Pet-guided tour (replaces the floating pill overlay)**
- NEW `ui/pet/PetGuideOverlay.kt`: the Curio pet itself guides the First
  Journey. A dim SCRIM covers the screen and BLOCKS every other button, with
  a pass-through WINDOW (the hole) over the step's target zone — the real
  button there stays tappable (pointerInput consumes taps only outside the
  hole; EvenOdd path + pulsing accent ring draw the window). The pet hops in
  beside the window (springy appear per step), wears its new `pointing` pose
  (raised wiggling coral paw + WIDE eyes + open mouth), and aims a pulsing
  coral arrow into the window. Its speech card reuses QuestGuideToast
  (pointer = null) with title/message/dots/action/skip/close. Pet + card
  never overlap the hole. Geometry: BOTTOM/LOWER = bottom strip (Shuffle /
  reveal dock / Save) with the pet above pointing DOWN; TOP = band below the
  settings hero with the pet below pointing UP; CENTER (final) = no scrim.
- `CurioPetSprite` gains `pointing: Boolean` (paw drawn on the facing side,
  mirrored by the flip layer; gated `!sleeping && !dragged && !moving`).
- `CurioNavHost`: overlay call replaces the toast block
  (heroTopOffset = SettingsHeroTotalHeight); floating pet hidden while the
  tour is active to avoid a duplicate pet.
- `QuestGuide`: step 5 (Start exploring) position TOP → BOTTOM so the pet
  points at the reveal's bottom action dock.

**Validation** — balance + whitespace clean; review applied (no concrete
compile/runtime issues; minor notes: step-3 tab bar sits inside the bottom
hole but self-heals via the non-hold runner; TOP steps can clip on very
short screens). CI runs on push.

## Done previously (pushed)

- **v8.14** (`555b3af`) — pet home/sleep + time-of-day diorama, curled sleep
  pose + nightcap + startle, 4 AM daily rollover (CurioQuests + StreakTracker),
  Resets-at-4-AM UI, noteSpinning rename (JVM clash fix).
- **v8.13** (`04beae0`) — smarter pet (passport-aware leastExploredLane),
  medal badges, silent-explore +5 XP + wildcard passport peek fix.
- **CI fix** (`d0669a5`) — LocalContext hoisted out of a lambda;
  `blushing` declared after excited/proud/playing.
