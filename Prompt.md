# Current Request — Fix Kotlin compile failure and unify dark mode

## User ask
- Fix `BottomSheetDefaults` unresolved reference in `SpinScreen.kt`.
- Expand the dark metallic treatment from Profile and Settings to the entire app.
- Use one unified dark palette across Curio, AMOLED, and Material dark styles.

## Implementation
- Added the Material3 `BottomSheetDefaults` import required by the existing modal-sheet drag handle.
- Added shared dark metallic surface and ink tokens to `CurioColors`.
- Updated the core dark `ColorScheme` to use the shared metallic tokens.
- Unified AMOLED dark and Material dark mode onto the same readable metallic surface scheme while preserving Material dynamic light mode.
- Updated shared settings/profile cards, chips, and dialog/pill helpers to derive from the unified dark surface language instead of AMOLED-only black branches.
- Preserved light mode and note-paper surfaces, which intentionally represent physical paper.

## Validation
- Static searches completed for the failing symbol, theme branches, and remaining hard-coded dark surfaces.
- Gradle compile/build/lint/test commands were intentionally not run because repository instructions prohibit them in this environment.
- Remaining feature-specific black treatments are decorative/material-specific rendering paths and were not removed without a targeted visual requirement.

## Completion summary
The compile failure is addressed by importing the supported Material3 sheet defaults API. Dark mode now uses a shared metallic foundation across all theme styles, with stronger tonal layers and consistent content contrast inherited by screens and shared components. Changes are ready for repository commit and push.
