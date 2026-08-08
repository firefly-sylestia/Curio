# Request — v8.28: locked bonus-quest silhouettes (done) + parked hooks spec

## What the user asked

Show the two bonus quests as hidden locked silhouettes with "??" before the
core trio is done, so players can see there's more coming.

## Implementation (pushed)

`features/quests/QuestsScreen.kt` — `DailyCard` restructure:

- The core list and bonus pair now stay composed as a CROSS-FADING pair of
  `AnimatedVisibility` nodes: while the trio is open
  (`visible = !coreDone`) the core rows render, followed by the two bonus
  quests as locked silhouettes; when `coreDone` flips, the core group fades
  out while the real gold bonus rows pop in (fade + scale). No abrupt swap.
- New `BonusLockedRow(quest, coreRemaining)` composable: same row rhythm as
  a real bonus quest but dimmed — silhouette icon box (surfaceVariant +
  border, dimmed `AutoAwesome` glyph), "Bonus quest" title, a hint counting
  down ("Complete N more core quests to unlock" / "Complete the last core
  quest to unlock"), and a mystery "?? XP" reward tag.
- KDoc updated; store changelog `20260816.txt`.

Validation: brace balance + `git diff --check` pass. CI on push is the
compile gate.

## Parked v8.28 hooks spec (user picks, build later — from earlier rounds)

1. **Topic of the day (Home)** — gold "must-see" card; deterministic
   rotation through the whole catalog, no repeats until cycle done.
2. **Come-back teaser (Home)** — short rotating mix: pet missed you +
   what's waiting (ready quests/gift) + streak warning.
3. **Spin streak combo (Spin)** — consecutive spins stack an XP multiplier
   up to 2x AND fill a "Spin Storm" meter that pays when full.
4. **Rare card moments (Spin)** — ~1 in 20 spins, rare topic with sparkle +
   bonus XP; the pet occasionally sniffs out / telegraphs one.
5. **Mystery card slot + viewed-cards stack (Spin/Reveal)** — third
   face-down card that flips on landing; PLUS a smooth scrollable stack of
   previously viewed cards behind the landed topic to explore instead
   (UX-first polish).
6. **Streak freeze & revival** — freezes earned at 7-day milestones;
   revival costs XP scaled by streak length.
7. **Weekly rotating special chain (Quests)** — one themed chain per week
   (e.g. "Explorer Week") with its own reward and badge.
8. **Pet / Cabinet / Profile hooks** — user has own ideas, to share later.

Always-on unless the user asks for a toggle.
