# Request — Remove tutorial implementation, keep the Tour quest

## User request
- Remove the tutorial/tour implementation entirely for now.
- Keep the Quest feature.
- Clarified decision: keep the separate “The Tour” Quest chain, its stages, progress, XP, and badges; remove only the tutorial experience and implementation around it.

## Completed
- Removed the onboarding “Take a quick tour?” prompt.
- Removed the Quests-page first-quest tutorial offer and “Take the tour” CTA behavior; quest navigation now opens the selected quest normally.
- Removed the Guided tour Settings entry and preference state/storage.
- Removed QuestGuide startup restoration, NavHost auto-navigation runner, navigation lock, overlay rendering, and reveal/action wait hooks.
- Deleted the standalone `QuestGuide.kt` state machine and `PetGuideOverlay.kt` UI implementation.
- Removed tutorial-only floating-pet suppression and pointing-paw pose; restored the normal onboarding floating-pet guard.
- Preserved the separate `CurioQuests` “The Tour” chain and its existing badge IDs/progress mappings.

## Validation
- `node scripts/check_braces.js` passed: 123 files checked.
- `git diff --check` passed.
- Static audits found no tutorial implementation symbols or tutorial UI strings in app source.
- The “The Tour” Quest chain remains present with all six stages.
- Local Gradle compile/build/lint/test commands were not run per repository rules; CI remains the Android/Kotlin compile gate.
- Final code review found and resolved the onboarding floating-pet guard regression.
