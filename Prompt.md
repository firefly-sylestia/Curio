# Current Request — Detail hero tear reads the same as every other screen

## Status: DONE (committed + pushed to Alpha)

## Request (user, verbatim)
"the tear logic of detail screen seems totally differnt from rest of the screens, can u fix it"

## What was found
- "Detail screen" = the saved-entry `EntryDetailScreen` (the Topic Reveal has no tear).
- The detail hero's shape construction was ALREADY aligned with Home (v104): `SoftTornBottomShape` + `SoftTornSheetShape` with `bold = true`, same tearSeed for both, same 10dp lip / 14dp baseline, same v108 `heroTearSheetState` gate, same banner height/lip offsets.
- The ONE remaining divergence was the **torn-edge shadow rim** under the seam: every other hero (Home, Profile, Cabinet, Settings, Onboarding, TopicHistory, Spin) draws `Color.Black.copy(alpha = 0.20f)`, but the detail hero drew `heroSheetColor.copy(alpha = 0.72f)` — a warm paper-colored band (near-black in dark mode) that made the seam read as a totally different tear.

## What changed (v192)
- `app/src/main/java/com/curio/app/features/detail/EntryDetailScreen.kt` — the torn-edge shadow rim under the detail hero now draws the same `Color.Black.copy(alpha = 0.20f)` hairline as every other screen (the old warm 72% paper band is gone). `heroSheetColor` stays in use (sheet + meta card), so no unused-variable fallout.

## Docs
- `app/AGENTS.md` — v192 entry.
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — 1 FIX bullet.
- This Prompt.md.

## Verification
- Static only (no Gradle here — CI validates on push). Confirmed via code_search that every other torn hero uses `Color.Black.copy(alpha = 0.20f)` and the detail hero is now the only place the rim color is referenced at that call site.
