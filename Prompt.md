# Request — v8.30: pet reacts to the user's touch, not phantom auto-opens

## What the user asked

When they OPEN the main card (tap the deck's landed card), the pet says the
"it auto-opened" line ("It opened itself, sneaky!") — which is wrong when
the user actually tapped it. Make the pet interact with the user's touches.

## Root cause

`TopicRevealScreen` fired `CurioPet.Event.REVEAL_OPEN` on EVERY reveal open,
and `eventLine(REVEAL_OPEN)` picked its lines from the auto-open PREFERENCE
(default ON), not the real cause — so a user-tap open claimed "it opened
itself".

## Fix (pushed)

- **CurioPet.kt** — `Event.REVEAL_OPEN` split into:
  - `REVEAL_TAPPED` — the user opened the card: "You picked it!", "Ooh,
    good choice!", "That one called to you!", "Nice pick!", "It knew
    you'd tap it!" (reacts to the touch).
  - `REVEAL_AUTO` — only the spin's true auto-open: "There it is!",
    "It opened itself, sneaky!", "Ta-da! A new tale!", etc.
  - New transient `pendingRevealAuto` flag + `markRevealAuto()` /
    `consumeRevealAuto()` (set right before the auto-navigation, consumed
    and cleared in the reveal's LaunchedEffect).
- **SpinScreen.kt** — `CurioPet.markRevealAuto()` right before the
  auto-open navigate.
- **TopicRevealScreen.kt** — the open effect consumes the flag and fires
  `REVEAL_AUTO` vs `REVEAL_TAPPED` accordingly (passport noteReveal and
  QuestGuide.onWait unchanged).

## Validation

No stale `REVEAL_OPEN` references (only a comment); brace balance +
`git diff --check` pass; code review done. CI on push is the compile gate.

## Parked v8.28 hooks spec (user picks, build later)

1. Topic of the day (Home) — gold must-see card, deterministic rotation.
2. Come-back teaser (Home) — rotating mix: pet missed you + what's waiting
   + streak warning.
3. Spin streak combo — XP multiplier up to 2x + "Spin Storm" meter.
4. Rare card moments — ~1 in 20, pet sniffs out / telegraphs.
5. Mystery card slot + smooth scrollable viewed-cards stack behind the
   landed topic (UX-first).
6. Streak freeze (7-day milestones) + revival (XP scaled by streak).
7. Weekly rotating themed chain (e.g. "Explorer Week").
8. User's own Pet/Cabinet/Profile ideas, to share later.

Always-on unless the user asks for a toggle.
