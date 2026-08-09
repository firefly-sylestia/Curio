# Request — Reimagine The Tour around Curie

## User request
- Redesign The Tour so Curie is the guide: it walks to the real control, speaks in a speech bubble, and asks the user to tap that control.
- No overlay/scrim; the user can still interact with the app.
- Next and Skip should be floating controls below the speech bubble, not embedded in the bubble.
- Tour should cover Home and other screens, and the pet should move to the next task automatically after the user taps the demonstrated control.
- Do not open or perform the task in the tour; show the surrounding UI only.
- Do not save entries, explore topics, or use undo while touring. Replace “Already watched” with an “Express yourself” writing action.
- Normal Explore offers explicit Google and YouTube choices.
- Onboarding completion offers the user a choice to take the tour; accepting wakes Curie and starts it from Home.

## Confirmed decisions
- A demonstrated tap is a safe demo tap: it advances the transient Tour and suppresses the underlying mutation, navigation, browser launch, save, explore, and undo action.
- Start with a Home-first Tour.
- The tour reveal is read-only browse mode and uses the real David Bowie topic for stable landmark placement.
- The tour is transient and is not persisted.

## Implemented
- `TourController` owns the transient offer, steps, safe landmark tap consumption, and route handoff.
- Onboarding completion calls `TourController.offer()`; Home displays “Take a tiny tour?” / “Maybe later”.
- Accepting the offer wakes Curie and lets the existing entrance/movement animation guide it to the registered Home/Spin/Reveal landmarks.
- Home shuffle and Spin controls consume safe Tour taps before their normal side effects.
- Reveal registers `express-yourself` and `start-exploring` landmarks; tour taps do not write, open capture, launch a browser, or record reveal activity.
- Reveal normal writing action is “Express yourself” with no done/undo state.
- Normal Explore now has a provider choice dialog with Google and YouTube buttons; URLs share the existing query builder.
- Tour guidance remains scrim-free; only Skip and Next are added as floating bottom controls.

## Validation
- `node scripts/check_braces.js` passed.
- `git diff --check` passed.
- Gradle is forbidden locally by repository instructions; CI remains the compilation source of truth.

## Remaining closeout
- Review latest diff once more for generated/partial files.
- Update release note if required by the active version metadata.
- Commit and push the related Tour changes; do not include unrelated `pet_brain_emotional_v3.zip`.
