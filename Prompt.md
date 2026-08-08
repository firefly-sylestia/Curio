# Request — v8.22: first-launch tutorial + boot-gate pet + tour redesign

v8.21 (`9fb7360`, pushed) shipped the dizzy-on-drag / hearts / de-AI /
auto-open-ON batch. This request is the guided-tour + boot-flow overhaul.

## What the user asked

1. The pet should stay at its house during startup (no floating pet on the
   splash), come out of its bed on its own during the intro, and then ASK
   about the tutorial the first time the app opens.
2. Redesign the tutorial: the dialog must come FROM the pet's bubble, the
   highlight must cover the REAL buttons (some were wrong), the highlighted
   button must stay tappable, and the instructions were getting cut off.

## Changes (6 files + 1 deleted)

| File | Change |
| --- | --- |
| `data/QuestGuide.kt` | `Step` gains `targetLandmark: String?` — 6 steps now name the exact landmark they highlight (`daily`, `spin`, `deck`, `start-exploring`, `save`, `grid`). |
| `ui/pet/PetGuideOverlay.kt` | REWRITE. The pass-through window is now the step's landmark's REAL bounds (via `PetLandmarks.forScreen(routePrefix)` — snapshot state, so it snaps into place the moment the screen registers; falls back to the old position zones). The pet + bubble sit BESIDE the window — below it when the target is high on the screen, above when low — never covering the button. The dialog is now `GuideSpeechBubble`: a real speech bubble with a tail aimed at the pet, title, message (maxLines 4 — instructions no longer cut at 2), progress dots, action, skip, close. |
| `navigation/CurioNavHost.kt` | Passes `screen = routePrefix` + `targetLandmark = step.targetLandmark` to the overlay; the floating pet is suppressed on the splash + crash routes (pet stays at its house during startup). |
| `features/reveal/TopicRevealScreen.kt` | The Start exploring button is wrapped in `PetLandmark("start-exploring", FUN, "reveal")` so the tour highlights its real bounds. |
| `features/onboarding/OnboardingScreen.kt` | The pet comes OUT of its bed automatically when the intro composes (`CurioPet.wake()` + `comeOut()`); finishing the intro asks "Take a quick tour?" the very first time (pet sprite in the dialog; accept → land on Home then `QuestGuide.start()`; decline/dismiss → marks offered). |
| `ui/components/QuestGuideToast.kt` | DELETED — fully orphaned after the speech-bubble redesign (its only caller was PetGuideOverlay; verified no other refs). |

## Review fixes applied

1. **Dead code** — `QuestGuideToast.kt` (pill + `GuidePointer`) had no
   remaining callers; removed.
2. **Race** — the tour-ask confirm used to `QuestGuide.start()` then
   navigate Home; reordered to navigate Home FIRST, then start the tour, so
   the NavHost runner picks up step 1 from a clean stack.

## Validation

- Brace balance ALL OK (5 edited files), `git diff --check` clean.
- Reviewer (code-reviewer-deepseek-flash) passed after the two fixes.

## Completion summary

v8.22 shipped: the pet stays at its house through the splash, comes out on
its own during onboarding and asks for the guided tour on first launch; the
tour itself now highlights the real buttons (window = landmark bounds, so
the highlighted control stays tappable), talks through a speech bubble that
points at the pet, and no longer cuts off instructions. Pushed to Alpha.
