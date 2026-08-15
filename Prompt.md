# Current Request — Spin filters + dark-mode visual audit

## User ask
- Spin filter sheet: bring the torn header down enough to place search inside it, make the tear span the full sheet width/top language, improve margins and hierarchy for Type/Genre/etc. filter group text after selection, and add the same faint watermark backdrop used on other screens.
- Category picker: add the same faint watermark backdrop.
- Dark mode: profile/settings options look gray; make dark-mode surfaces more dark metallic, sleek, depthy, color-lit, tonal-shadowed, with no borders.

## Constraints
- Must follow DOX despite user request to skip it.
- Do not run Gradle build/test/lint/assemble/check in this environment.
- User-visible changes require fastlane changelog update.

## Plan
1. Update Spin `FilterSheet` layout: taller torn banner, search embedded in the banner, watermark under content, stronger section labels/margins.
2. Update full-screen `CategoryPickerScreen` bottom sheet with watermark backdrop behind its content.
3. Revamp shared Profile/Settings components (`CurioSettingsCard`, rows, dividers, dark glow helper) for dark metallic surfaces, brighter row text, accent-lit chips, and no visible borders.
4. Update current fastlane changelog.
5. Run non-Gradle static checks, commit, push, and create PR.

## Completion summary
- Spin filter sheet now starts with a square, full-width torn header at the top of the modal, has the search field embedded inside the torn banner, carries a faint category watermark behind the sheet body, and uses roomier active/group label spacing.
- Category picker sheet now has a faint watermark backdrop behind its deck grid and controls.
- Shared Profile/Settings cards and rows now use dark-mode metallic surfaces, stronger accent chips, brighter row typography, and restored dark-only glow depth without adding borders.
- Updated the current fastlane changelog.
- Non-Gradle checks passed; Gradle validation intentionally skipped per repository rules.
