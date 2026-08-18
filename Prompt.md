# Current Request — Remove the "Material guidelines" + "Material chrome" options, push everything

## Status: DONE (committed + pushed to Alpha)

## Request (user, verbatim-ish)
"remove the material guidelines options and the material chrome its not
good. and also push everything"

## Interpretation (confirmed by the ask → no ask needed: explicit removal)
- Remove the **"Material guidelines"** Appearance toggle and its
  **"Material chrome"** sub-option (the v185 system that layered full M3
  typography/shapes/spacing on the CURRENT style, plus the M3
  NavigationBar swap + Changa One font drop from nav labels).
- KEEP the **"Material theme"** toggle — it redoes only the COLOR system
  and was not mentioned for removal.
- Push all local commits (branch was 5 ahead of origin/Alpha) along with
  this change.

## What was removed (wholesale — no dormant prefs, per the user's verdict)
- `app/src/main/java/com/curio/app/ui/theme/MaterialGuidelines.kt` —
  DELETED (gates `materialGuidelinesOn` / `materialChromeFullOn`,
  `MaterialTypography`, `MaterialShapes`, `CurioSpacing` tokens).
- `AppPreferences.kt` — removed `materialGuidelinesState` /
  `materialChromeFullState` state vars, `KEY_MATERIAL_GUIDELINES` /
  `KEY_MATERIAL_CHROME_FULL` keys, the 4 get/set functions and their
  `initThemeMode` seeds. `materialThemeState` + `KEY_MATERIAL_THEME` stay.
- `CurioTheme.kt` — `typography = CurioTypography` / `shapes =
  CurioShapes` always (the guidelines swap is gone).
- `CurioBottomNav.kt` — deleted the `if (materialChromeFullOn)` M3
  `NavigationBar` branch (+ its 3 imports + the 2 gate imports); both
  nav label sites (pill + rail) always use the Changa One style.
- `SettingsSectionScreen.kt` — removed the "Material guidelines" row and
  the conditional "Material chrome" row (the "Material theme" row stays).

## Docs
- `app/AGENTS.md` — v185 M3-theme UI bullet rewritten to the single
  toggle; the v185 section records the guidelines+chrome system as
  BUILT then REMOVED (PHASE C note).
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — the ADD
  bullet edited to describe only "Material theme" (the guidelines feature
  never shipped in a released build, so no REMOVE note per the fastlane
  contract).
- This Prompt.md.

## Verification
- Static only (no Gradle in this env — CI validates on push). Grep sweep
  confirms no remaining references to the removed symbols anywhere in
  `app/src`; `git diff` reviewed (5 files, −203/+12, plus the deleted
  file and docs).
