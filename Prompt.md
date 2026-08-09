# Request — Fix CI Kotlin compilation failures

## User request
Fix the reported release/debug CI compilation errors in `CurioNavHost.kt`, `CurioCategoryCard.kt`, and `CurioTheme.kt`.

## Fixes completed
- `CurioNavHost.kt`: added the missing comma after `.padding(innerPadding)` before `contentAlignment`.
- `CurioNavHost.kt`: kept the tour controls as a direct child of the existing root `Box`, restoring `Modifier.align(Alignment.BottomCenter)` scope without reintroducing a full-screen transparent wrapper.
- `CurioCategoryCard.kt`: repaired the malformed `background` conditional so selected cards use `Brush.verticalGradient(gradient)` and idle cards use `SolidColor(idleSurface)`.
- `CurioTheme.kt`: imported `androidx.compose.ui.graphics.lerp` for the Material surface-tone helper.

## Validation
- `node scripts/check_braces.js` passed: 125 files checked.
- `git diff --check` passed.
- Focused review found no critical compile or scope issues.
- The affected symbols now resolve statically: `contentAlignment`, `SolidColor`, `lerp`, and BoxScope `align`.
- Local Gradle compile/build/lint/test commands were not run because the repository explicitly forbids local Android builds; CI remains authoritative.

## Status
Compile fix validated and committed/pushed in this turn.
