# Prompt.md — Request Log

## Current Request (COMPLETED): Material theme buttons, pastel dialogs, reveal footer polish

**Date:** 2026-08-10

### What the user asked
Fix the Spin button and category button in the Material theme (light + dark), make the Material theme fully Material (nothing foreign left), make pastel-light dialogs match the screen tint + card shape (like the Topic Reveal dialog) with darker readable text, and simplify the Topic Reveal bottom strip: plain no-design tear, theme-aware, tags a little lower, no off-screen overflow on small screens, footer height unchanged.

### Changes made
- **CurioTheme.kt** — added shared dialog theme: `CurioDialogShape` (24dp card-matching), `curioDialogContainerColor()` (light mode blends toward the cream background so dialogs melt into pastel pages; Material/dark keep scheme surfaces), `curioDialogActionColor()` (deep same-hue rose ink in light for readable buttons; device primary in Material/dark), `curioDialogActionButtonColors()`.
- **SpinScreen.kt** — Material style: Spin dice glyph uses device onPrimary in dark (was white-on-light = invisible), orbit dots use onSurface in light (white dots vanished on the wash), Categories/Filter selected label pairs with the icon's themedButtonInk (was mismatched onPrimaryContainer), unselected pills wear device surfaceContainerHigh + outlineVariant instead of category tint; FilterSheet + CategoryPickerSheet wear device surfaceContainerLow in Material.
- **TopicRevealScreen.kt** — bottom strip now uses the plain `SoftTornBottomShape(seed)` (no bold/detail lip), stays theme-aware, footer height unchanged (80dp); tag chips moved down (10→16dp inset) so they clear the seam and never run off small screens; all 3 reveal dialogs use the shared dialog theme.
- **Dialog pass (24 AlertDialogs, 12 files)** — every AlertDialog now passes `containerColor = curioDialogContainerColor()` + `shape = CurioDialogShape`, and action TextButtons use the readable deep-rose ink; AudioQualityDialog radio/border also use it; filled Save-and-switch buttons use `curioDialogActionColor()`.

### Validation
- All 24 AlertDialog sites updated (grep counts verified), imports verified per file, no duplicate shape params, git diff --check clean.
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Theme-aware Topic Reveal footer

**Date:** 2026-08-10

### What the user asked
Make the newly added Topic Reveal bottom torn strip useful without increasing its height. The strip and tear should be opaque and theme-aware across Curio, AMOLED, and Material styles, with category tint support. Move topic tags into the top of the footer if possible.

### Changes made
- Kept the existing fixed 80dp footer geometry and reserved navigation inset unchanged.
- Made the torn strip fully opaque and selected its surface from the active appearance: category surface for Curio, Material surface container for Material, and AMOLED surface for AMOLED.
- Reused the same resolved surface for the torn edge so the seam remains visually continuous in each theme.
- Moved the existing topic tags from the reveal body into a compact single-line footer row, capped at three tags with ellipsis-safe text and no height expansion.
- Preserved existing reveal actions and interactions.

### Validation
- Brace checker passed for TopicRevealScreen.kt.
- git diff --check passed.
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.
- Review the diff before commit/push.

### Follow-up
Refine the footer tear so it projects slightly farther and improve the footer tags into clearer pill chips without increasing the fixed height.

## Current Request (IN PROGRESS): Refine Topic Reveal tear and tags

**Date:** 2026-08-10

### Changes made
- Increased the tear's visible irregularity using the existing detail geometry mode while preserving the fixed footer height.
- Strengthened footer tag pills with a clearer category-tinted fill and outline.

### Validation
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.
- Run `git diff --check` and the lightweight source checks before commit/push.

## Current Request (IN PROGRESS): Refine explore dialog and expression action

**Date:** 2026-08-10

### Changes made
- Removed the redundant `Not now` action from the Explore dialog; outside-tap/back dismissal remains available.
- Grouped Google and YouTube choices together in the dialog action area.
- Added a theme-aware outlined pill surface to `Express yourself`, including disabled-state contrast.

### Validation
- Run `git diff --check` and lightweight source checks.
- Do not run Gradle build/lint/test commands per repository rules.
