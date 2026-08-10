# Prompt.md — Request Log

## Current Request (COMPLETED): Topic Reveal plain bottom band

**Date:** 2026-08-10

### What the user asked
On the Topic Reveal page: remove the tear style from the bottom (keep it plain and theme-aware) and move the tags down a little.

### Changes made
- **TopicRevealScreen.kt** — removed the torn seam entirely: dropped the `SoftTornBottomShape` clip + 180° rotation from the bottom strip so it's now a flat, theme-aware band (unchanged `bandPaper`: Curio category surface / Material surfaceContainer / AMOLED surface). Removed the now-unused `REVEAL_BOTTOM_TEAR_SEED` constant and `SoftTornBottomShape`/`graphicsLayer` imports; renamed `RevealBottomTearHeight` → `RevealBottomBarHeight` (same 80dp footprint) and `tearPaper`/`tearInk` → `bandPaper`/`bandInk`.
- Tags row moved down a little: top inset 16 → 24dp inside the bottom band; comments updated to describe the plain band.
- **CurioNavHost.kt** — comment-only: reveal references now say "plain bottom band" instead of "torn paper edge/sheet".

### Validation
- Grep-verified: no stale references to the removed/renamed symbols anywhere; `graphicsLayer`/`SoftTornBottomShape` unused in the reveal file.
- Code review passed (imports, rename consistency, band geometry math unchanged).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Pet speech bubbles skip on interaction

**Date:** 2026-08-10

### What the user asked
When interacting with the floating pet (tapping it, or doing other things in the app), the pet doesn't skip its current dialog to react — the bubble stays and cycles through all queued lines. It should skip in some places (not always): direct interactions should dismiss/skip the current line and answer immediately.

### Changes made
- **CurioFloatingPet.kt** — added `speakNow(line?)`: interrupts whatever bubble is showing (skips it via re-keying the bubble lifecycle) and clears the queued backlog. A null line dismisses the bubble silently (the pet's motion is the reaction).
- Taps now call `speakNow` (with or without a line) instead of `queueReaction` — the pet answers the tap immediately and drops queued chatter.
- Drag end ("Home sweet home!", dizzy line) and long-press ("Home sweet home!") also use `speakNow`.
- `fireReaction`'s event lines (spin landed, reveal, explore, save, play, level-up) now `speakNow` — real user-driven events skip the current bubble instead of queuing behind it (null lines leave the bubble alone).
- `queueReaction` (ambient wander/peek/games/typing/custom action chatter) is now CAPPED to the latest 2 lines, so the pet can never cycle through a long backlog of stale lines; it repeats the last one or two then falls quiet.
- Tour dialogue is untouched (separate `tourStep?.dialogue` path, never interrupted).

### Validation
- git diff --check clean.
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

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
