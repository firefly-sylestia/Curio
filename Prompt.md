# Request — v8.26b: CI compile fix + app-wide em-dash sweep

## What the user asked

1. Fix the CI compile failures from the last push (they pasted the Gradle
   error log).
2. Remove em dashes from the rest of the user-facing copy app-wide
   (onboarding slides, quest descriptions, badges, etc.).

## Part 1 — CI compile fix (root causes)

- `CurioNavHost.kt` — `Modifier.background(background)` in the reveal
  bottom-strip placeholder (v8.25) was missing the
  `androidx.compose.foundation.background` import. The unresolved extension
  cascaded into the confusing `Comparator.then` / "unresolved background" /
  "cannot infer T" errors. Fixed by adding the import.
- `CurioFloatingPet.kt` — the throw-momentum glide (v8.26) called `launch`
  inside `onDragEnd`, a plain lambda with no CoroutineScope receiver, so it
  resolved to the deprecated top-level `launch`. Fixed by capturing the
  pointerInput scope (`val inputScope = this`) and launching via
  `inputScope.launch { ... }`.

Pushed as `e617bc9` before starting the copy sweep.

## Part 2 — em-dash sweep (user asked; scope = UI copy ONLY)

Asked via ask_user: ~10,000 em dashes in the topic content JSONs are
editorial data — user chose UI copy only, so `assets/topics/*.json` is
untouched.

Swapped every em dash in user-facing Kotlin/XML strings for commas, colons,
or periods (context-appropriate). 35 files, 130 changes:

- Pet dialogue (`data/CurioPet.kt`, `ui/pet/*`, home pet contentDescriptions)
- Quest descriptions + badges (`data/CurioQuests.kt`, `features/quests`)
- Onboarding slides + permission copy (`features/onboarding`)
- Reveal/spin/cabinet/profile/recent/capture/crash/settings/support/promo/
  bugreport screens, nav host session dialogs, notifications
  (`ExploreReminderReceiver`, `ExploreSessionService`), update-check rows,
  `strings.xml`, share-text builders (`data/CaptureData.kt`), fallback quote
  (`data/TopicCatalog.kt`)
- `docs/PET_DIALOGUE.txt` synced to the new pet lines.

LEFT AS-IS (not user-facing): code comments, Log.w lines, exception
messages, icon-name comments (`CurioIcons.kt`), the topic JSON content.

## Validation

- `git diff --check` clean; brace balance OK (flagged files are pre-existing
  false positives of the naive checker — identical at HEAD).
- Re-scan confirms 0 em dashes left in user-facing Kotlin/XML string
  literals (remaining 43 are comments/logs only).
- No compile/build commands run (no Android SDK here — CI gates on push).

## Completion summary

CI compile fixed (background import + scoped glide launch, pushed e617bc9);
then the app-wide UI-copy em-dash sweep (35 files, all user-facing strings)
pushed as the follow-up. Topic content JSONs untouched per scope choice.
