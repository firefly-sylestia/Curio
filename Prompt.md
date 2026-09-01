# Prompt.md — current request log

## Request (ACTIVE): Polish the Curio pet — bugs, animations, games, dialogs

User: "polish the curie pet even more, fix bugs if any, and add more
animations, games for it too, and add more dialogs."

> NOTE: the share-hub / detail-share / moodboard-quote feature from the
> previous request is PARKED (user: "dont push this but push the cl fix").
> Nothing for it was committed; design answers received (full swap +
> share-as-text everywhere; hub Share button opens the sheet + floating
> button; moodboard quote option in save + share) are recorded in the
> conversation. Resume with TopicShareSheet work when asked.

### What shipped (this turn)

**1. New game — POP!** (App: ui/pet/CurioFloatingPet.kt)
- `PetGame.POP` joins HIDE_SEEK / CHAMELEON / SPARK. Game-mode cycles via
  `PetGame.entries.size` so it's reachable by the long-press cycle; the
  idle auto-flow still only picks hide/chameleon (short rounds only).
- 10s round of bubbles floating UP across the lower half; tap one (or drag
  the pet into it) → pet dashes over and pops it (squish+celebrate).
  ~1 in 6 bubbles is PRICKLY (darker, white ×): popping it costs a point,
  winces, and can say a line. 6+ pops → confetti + a delighted line.
- New `BubbleView` (breathing translucent orb, shine) and `VictoryConfetti`
  (keyed 1.2s burst of colored squares) composables.

**2. Bug fix — stuck game state after navigation** (CurioFloatingPet.kt)
- The interrupted-game self-heal reset offScreen/hide/chameleon flags but
  NOT `gameActive`/`gameMode`; after a mid-game screen change the pet would
  silently stop ever starting auto-games. Both flags now reset with the
  heal (v263 note added).

**3. More animations**
- VictoryConfetti now pops on ALL big wins: hide-and-seek found, chameleon
  found, star score > 0, bubble score ≥ 6 (`confettiKey++` at each win).
- Bubbles breathe (wobble scale) so the round reads as alive.

**4. More dialogs** (App: data/CurioPet.kt)
- +~20 fresh lines across the first-evo game pools (spark chase, find-me,
  found, caught-it, got-away, peek-win, missed).
- 3 new per-stage line fns for POP!: `popPromptLine()`, `popNiceLine(count)`,
  `popPrickleLine()` (BABY / FIRST_EVO / FINAL_EVO voices).

### Progress
- [x] PetGame.POP + Bubble data + state + playBubbleGame + dispatch.
- [x] BubbleView + VictoryConfetti composables + render blocks (bubbles,
      confetti at the pet's spot).
- [x] Win-path confetti bumps + bubble render/tap wiring.
- [x] gameActive/gameMode heal bug fix.
- [x] Dialogue pools extended + 3 new POP! line fns.
- [x] Balance verified (CurioFloatingPet +163/+163 braces 52/52; CurioPet
      +27/+27 braces 3/3, brackets 0/0) — fully matched vs HEAD.
- [ ] Changelog bullets.
- [ ] Commit & push (CI validates compile on push).

### Verification status
CI validates compilation on push (this environment forbids Gradle builds) —
watch the run after pushing. String-stripped balance vs HEAD is exact on
both files, and all new identifiers (popPromptLine/popNiceLine/
popPrickleLine, PetGame.POP, playBubbleGame, BubbleView, VictoryConfetti)
are defined and referenced.