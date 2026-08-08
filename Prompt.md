# Request — v8.29: pet intro + dynamic bond + tour interaction fixes

## What the user asked

1. The pet should NOT show during the onboarding intro; it should introduce
   itself inside the tutorial instead.
2. Early pet dialogue acts like best friends from the start — it should be
   dynamic and warm up over time.
3. During the tutorial, clicking what the tour says to click doesn't work
   (e.g. spin the deck). They want: the dialog gets out of the way, the pet
   says "go ahead, spin it", the step HOLDS until the user really does the
   action (no next step), the user can't navigate away, and the pet REACTS
   when they do it.

## Root causes found

- The tour speech bubble was an ENABLED clickable Surface even on wait
  steps, so wherever it overlapped a target button it ate the tap; and its
  pop animation started at 0 with `if (stepIndex > 0)` — the FIRST step's
  bubble was invisible.
- Onboarding woke the pet (wake()/comeOut()) so it floated behind the intro
  slides, and the tour-ask dialog showed its own pet sprite.
- Dialogue pools were uniformly warm ("Best friends!", "Past my bedtime…
  but for you, I'll stay", "Mine now… I mean, ours!") with no familiarity
  scaling.
- No back-button lock during the tour.

## Fix (pushed)

- **QuestGuide.kt** — new step 0 "Meet Curio" (CENTER, route "") where the
  pet introduces itself; wait-step copy rewritten as direct commands
  ("Go ahead, spin it! I'll wait right here." / "Go on, tap the card to
  open it." / "When you're ready, tap Start exploring below." / "Go on,
  save what you found. It'll be yours to keep!").
- **PetGuideOverlay.kt** — bubble Surface now `enabled = actionEnabled`
  (disabled surfaces never consume taps → the real button stays tappable
  during waits); `pop = remember { Animatable(1f) }` fixes the invisible
  first bubble; wait steps use a compact 300dp bubble.
- **CurioPet.kt** — new `Bond` (STRANGER <3, ACQUAINTANCE 3-5, FRIEND 6-11,
  CLOSE 12+) from level; `isWarm()` gates the warm morning/afternoon/
  evening/night twins, the SAVE line "Mine now… I mean, ours!", and the
  touch tier-3 "Best friends!" / "You're my favorite!".
- **CurioNavHost.kt** — `BackHandler(enabled = QuestGuide.active)` swallows
  back mid-tour (the X still exits); onboarding route excluded from the
  floating pet.
- **OnboardingScreen.kt** — removed the wake/comeOut (pet stays home during
  intro); tour-ask dialog has no pet sprite + neutral text; dropped the
  unused CurioPetSprite import.

## Validation

Brace balance + `git diff --check` pass on all 5 files; no unused imports
left. CI on push is the compile gate. Code review done.

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
