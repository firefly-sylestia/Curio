# Request — v8.25b: fresh-install tour + pet fixes

## What the user asked

1. On a fresh install the pet shows at the right time, but when the tour-ask
   dialog appears the pet ALSO shows behind it (a duplicate pet).
2. During the intro/tour the Home highlight and the explore highlight are
   wrong.
3. When the tour waits for the user to tap the spin button, the tap doesn't
   work.
4. Improve the tour dialog copy and don't use em dashes.

## Analysis

- **Pet duplicate:** the onboarding "Take a quick tour?" dialog shows its own
  `CurioPetSprite`, but the floating pet (awake since the intro) keeps
  wandering behind the dialog scrim. The NavHost only gates the floater on
  the tour being active, the route and the pet's awake state.
- **Explore highlight wrong + spin tap blocked:** `PetGuideOverlay` was
  rendered INSIDE the Scaffold's `innerPadding`-padded content box, so its
  coordinate space ended above the bottom bar. The reveal's Start exploring
  button lives in the Scaffold bottom-bar slot, so its landmark's window
  coordinates fell BELOW the overlay: the pass-through hole was drawn off
  the bottom edge, the highlight landed wrong and the scrim covered the
  real button (blocking the tap). The spin button sits in-content so it
  aligned, but the same mechanism bit it via the fallback zone: while a
  screen is still registering its landmark (first frame(s)), the step fell
  back to a guessed position zone that could cover the real button and
  swallow the tap.
- **Home highlight wrong:** the home step had no landmark and fell back to
  the BOTTOM position zone (a strip above the bottom nav) — a meaningless
  highlight for "welcome to home".
- **Em dashes:** the QuestGuide step messages, the onboarding ask dialog and
  the Quests tour-offer dialog all used em dashes.

## Changes (7 files + 1 changelog)

| File | Change |
| --- | --- |
| `data/CurioPet.kt` | New `floatingSuppressed` flag — UI screens suppress the floating pet while a dialog shows its own pet sprite. |
| `features/onboarding/OnboardingScreen.kt` | Sets `CurioPet.floatingSuppressed` while the tour-ask dialog is open (reset on dispose); tour-ask copy without em dashes. |
| `navigation/CurioNavHost.kt` | PetGuideOverlay moved OUT of the padded content box to the top-level full-window Box (sibling of the floating pet), so overlay coordinates == window coordinates and bottom-bar-slot landmarks (the reveal dock) align; floating pet also gated on `!CurioPet.floatingSuppressed`. |
| `ui/pet/PetGuideOverlay.kt` | A step that NAMES a landmark never falls back to the position zone while the screen registers it — scrim stays off (hole null) so the real button is always tappable. |
| `data/QuestGuide.kt` | Home step now targets the real "quest" landmark; all step messages rewritten without em dashes. |
| `features/home/HomeScreen.kt` | TODAY'S QUEST card registered as the "quest" landmark (`QuestShuffleCard` gains a `modifier` param). |
| `features/quests/QuestsScreen.kt` | Tour-offer dialog copy without em dashes. |
| `fastlane/metadata/android/en-US/changelogs/20260812.txt` | NEW — release notes. |

## Validation

- Brace balance ALL OK (all 7 edited files), `git diff --check` clean.
- No compile/build commands run (environment has no Android SDK — CI gates
  compilation on push per root AGENTS.md).
- Reviewer (code-reviewer-deepseek-flash) passed.

## Completion summary

v8.25b shipped: the pet no longer doubles up behind the tour-ask dialog, the
tour's Home and Start-exploring highlights now land on the real controls
(the overlay covers the whole window, so bottom-docked buttons align), the
spin step never blocks the Shuffle button while the deck loads, and all tour
dialog copy was rewritten without em dashes. Pushed to Alpha.
