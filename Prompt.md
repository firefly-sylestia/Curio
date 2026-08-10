# Prompt.md — Request Log

## Current Request (COMPLETED): Material theme → coming soon; AMOLED main card accent restored

**Date:** 2026-08-10

### What the user asked
Grey out the Material theme option and mark it "coming soon". Also: the AMOLED main Spin card used to have a beautiful, sleek category-color accent — check if it's still there and add it back.

### Changes made
- **SettingsSectionScreen.kt** — `CompactSegmentedRow` gained `disabledIndices: Set<Int> = emptySet()` + `disabledHint: String? = null` (defaults keep the Theme row and any other call site unchanged). The Theme style row now disables the Material segment (`disabledIndices = setOf(2)`) and shows a small clock-glyph + "Material theme · coming soon" caption in onSurfaceVariant. M3's disabled SegmentedButton greys the segment automatically; the Material style code path itself is untouched (it simply can no longer be picked). Note: a user who already has Material selected keeps it until they pick Curio/AMOLED — left self-resolving (no silent theme reset).
- **SpinScreen.kt (HeroTicketCard)** — the AMOLED main ticket now wears the same black-glass CATEGORY SHINE as the settings cards / deck pills: `Modifier.categoryEdgeShine(RoundedCornerShape(30.dp), accent)` on the clipped Box (accent hairline around the edge + a soft 18dp accent band at the top). The Surface hairline border in AMOLED carries the deck accent at a restrained 0.35 alpha (down from the uncommitted 0.55 — the edge shine is the primary accent carrier, so the card stays sleek rather than stacking two loud rims). This was previously uncommitted, which is why the user didn't see it on device.
- **fastlane 20260919.txt** — added a store-changelog bullet (Material on hold + AMOLED card accent).

### Validation
- Brace check + git diff --check clean; code review passed (segment disable + caption compile-safe; edge shine clipped/gated to AMOLED, non-AMOLED themes untouched).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

### Parked (previous request, still pending)
Adding missing tags to scientists/discoveries: `scripts/enrich_science_tags.py` is written and dry-run validated (word-boundary field matching, element-isolation rule, CE/BCE era parsing, ~180 curated origin/era overrides). Remaining: full-data audit run (the full 501-scientist set has ~60 more names with no era signal that need overrides), then run the script against the real JSON and commit.

## Current Request (COMPLETED): Home hero stat shows total topics instead of recents count

**Date:** 2026-08-10

### What the user asked
On the Home page, change the stat that shows the recent count to show the total number of topics the app has.

### Changes made
- **TopicCatalog.kt** — new `totalTopicCount()`: sync sum of the ten non-wildcard lanes' cached pool sizes (the catalog is warmed during splash, so it's ready on the Home's first frame; an uncached lane contributes 0 until loaded). Wildcard excluded — it only mirrors the canonical lanes.
- **HomeScreen.kt** — the hero stat bar's third segment now shows `TopicCatalog.totalTopicCount()` with label "Topics" and the AutoAwesome glyph (was the recent-feed size, "Recent", History glyph). The recents feed variables stay used by the Recents section below. Works in promo mode too (shows the real total).
- **PromoMode.kt** — `topicTotal()` now delegates to `TopicCatalog.totalTopicCount()` (was a duplicated cached sum) so the two can never drift.

### Validation
- Brace check + git diff --check clean; code review passed (no dead code, imports verified, DRY consolidation).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): AMOLED unselected Categories/Filter pills → pure black

**Date:** 2026-08-10

### What the user asked
Make the unselected Categories/Filter pills pure black in AMOLED instead of dark grey.

### Changes made
- **SpinScreen.kt** — `deckControlSurface()` now returns pure `Color.Black` for the AMOLED style (unselected pills were falling through to `categorySurface(...)` → the dark-grey `surfaceContainerHigh`). Both pill variants (`DeckControlButton` horizontal + `VerticalDeckButton` extra-compact) share this helper, so one edit covers every unselected pill.
- `deckControlBorder()` adds an AMOLED branch — a quiet 1dp accent hairline (`categoryInk()` at 0.28 alpha, the light accent in dark) so the pure-black pills stay distinct from the pure-black Spin page (without it they'd be invisible). Selected pills were already black with the accent rim from the earlier AMOLED pass.
- Material / Curio branches untouched.

### Validation
- Brace check + git diff --check clean; code review passed (both variants covered, no new imports, Material/Curio unchanged).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): AMOLED Home quest banner → pure black with rose accent

**Date:** 2026-08-10

### What the user asked
Make the Home quest banner pure black with the rose accent in AMOLED (the last hero still wearing the grey-coral tint).

### Changes made
- **HomeScreen.kt** — `homeRoseAccent()` AMOLED branch now returns pure `Color.Black` (was `lerp(surfaceContainerHigh, primary, 0.16f)`), matching the Profile/Settings heroes. Text stays white via the existing `homeReadableInk` → AMOLED `onSurface`.
- The quest hero carries the rose accent on the black plate: new `symbolTint` (`CurioColors.HomeRosewood` in AMOLED, `questInk` otherwise) tints the watermark symbols, and the stat pane's AMOLED gradient is a rose glow (0.30 alpha → subtle rose wash over black) instead of the black-on-black wash.
- **Cascade (consistent with the pure-black style):** QuestShuffleCard's casino button + eyebrow, the drawer hero, and the sticky top-bar pills all share `homeRoseAccent()` → black, all ink already resolves white/readable.
- **FirstTimeEmpty fix** — the "Surprise me" button hardcoded `DeepPlum` ink, which vanished on the now-black AMOLED plate; it now uses `homeReadableInk(roseAccent)` (white in AMOLED, readable deep rose in light pastel, onSurface in light non-pastel). All `CurioColors.DeepPlum` references in HomeScreen are gone.

### Validation
- Brace check + git diff --check clean; code review passed (cascade ink readability, no unused imports).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Spin orbit dots white in non-pastel light + loop skips

**Date:** 2026-08-10

### What the user asked
In non-pastel light mode the spin button's outer small dots look white while animating, and the orbit animation feels like it skips / isn't a real loop. Fix if confirmed.

### Diagnosis (confirmed from code)
- **White dots:** `OrbitRing`'s dot color used `pastelFillInk(color)`, which returns **white whenever pastel mode is OFF** — so non-pastel light mode lit the 10 orbit dots as a bright white necklace (the layered bloom read even whiter). Pastel mode returns deep colored ink, which is why only non-pastel showed it.
- **Skipping loop:** the dot ROTATION was already a seamless 0→360° loop, but the shimmer phase was keyed to the raw rotation angle (`sin(rotRad * 1.4 + i * 1.15)`), so the brightness wave spun around the ring at **1.4× the physical rotation** — a strobe-like moiré that fought the dots and read as skipping/stuttering, never a smooth loop.

### Changes made
- **SpinScreen.kt (OrbitRing)** — dot color now has a non-pastel LIGHT branch that deepens the accent via `deepHueInk(color)` (deep same-hue ink) instead of white; Material-light still uses device onSurface; dark/pastel unchanged (white / light-tint / deep-hue respectively).
- **SpinScreen.kt (OrbitRing)** — shimmer phase is now keyed to each dot's ABSOLUTE angle (`absAngle = a + rotRad`), so the brightness wave travels WITH the ring; the pattern is rotation-periodic, so the 360° wrap is invisible and the loop reads as one continuous orbit.
- **CategoryInk.kt** — `deepHueInk` promoted private → internal (shared with SpinScreen's orbit dots; no other callers changed).

### Validation
- Brace check + git diff --check clean; code review passed (HSL math, branch ordering, wrap seamlessness all verified).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Full Material theme revamp (last chance)

**Date:** 2026-08-10

### What the user asked
The Material theme is still ugly — category cards, the header, all of it — and needs a full revamp for pastel + non-pastel + dark mode colors, or the style will be removed.

### Changes made
- **CurioTheme.kt** — `calmMaterialColorScheme` rebuilt as a HUE-LOCKED palette: the device wallpaper's identity is kept as a single hue (from `dynamic.primary` via `toHsl`), and every scheme role is BUILT from that hue with Curio-tuned saturation/lightness (`fromHsl`); secondary/tertiary are the same hue family offset ±38°. No more raw dynamic colors (brown wallpapers rendered dull olive-grey). Light: near-white surfaces with a hue whisper, deep vivid primary, airy primary-container. Dark: deep tinted midnight from the same hue, bright readable primary, light same-hue container ink.
- **CurioColors.kt** — Material no longer gets device-color + faint category "whisper" gradients; `cardGradient`/`heroBlendGradient`/`mixedDeckGradient` all wear the SAME rich category gradients as Curio/AMOLED (material identity lives in the hue-locked scheme surfaces/heroes, not desaturated cards). Removed the now-dead `materialDeviceStop` + `floorForWhiteInk`/`WhiteInkLightnessFloor`.
- **CategoryInk.kt** — `themedButtonFill()` = true category accent (`themedAccent()`) everywhere; `themedButtonInk()`/`cardContentInk()`/`onAccent()` Material branches use the pastel-aware ink (white on deep, deep same-hue ink on airy pastels, light-tinted in dark pastel) with a `deepHueInk` guard for pale accents (wildcard coral) off pastel mode — fixes the review-found white-on-pink wildcard regression. `categoryBorder()` adds a quiet Material accent hairline when the tint toggle is off.
- **CurioCategoryCard.kt** — Material idle tiles lerp `surfaceContainerHigh` toward the category's themed accent (0.12 dark / 0.14 light) instead of plain device-grey; selected cards keep the full rich gradient + accent edge shine.

### Validation
- Brace check + git diff --check clean; code review passed (wildcard pale-accent ink fix applied; HSL scheme contrast checked).
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): AMOLED — pure-black Profile/Settings heroes + black accent buttons

**Date:** 2026-08-10

### What the user asked
In AMOLED, don't give Profile and Settings that tint — make them pure black with the accent. Make the header black too with an accent of the color. Also change the Spin button and the category picker button colors.

### Changes made
- **SettingsHubScreen.kt / ProfileScreen.kt** — `settingsRoseAccent()` / `profileRoseAccent()` AMOLED now return pure `Color.Black` (was `lerp(surfaceContainerHigh, primary, 0.16f)` grey-coral tint).
- Hero headers carry the rose accent on the black plate: watermark symbols + back pill tinted `CurioColors.HomeRosewood` in AMOLED (new `symbolTint`); Profile's stat-bar gradient pane uses a rose 30% pane in AMOLED. Titles/content stay white.
- **SpinScreen.kt** — SpinButton plate is pitch-black in AMOLED (`plateTint`): the category accent moves to the orbit ring, edge-shine rim and the 3D sheen (faint accent-tinted highlight instead of the white cap); the dice stays white. Selected Categories/Filter deck pills (`DeckControlButton` + `VerticalDeckButton`) are pitch black with the accent rim in AMOLED instead of the bright accent fill.
- **CategoryPickerScreen.kt** — the Mix button's AMOLED content flips to white (`onSurface`) — `curioButtonColors` already forces the plate black, but the old `onPrimary` (deep maroon) vanished on it.
- Cascade: Onboarding/Cabinet share `settingsRoseAccent()`, so their heroes go black in AMOLED too (consistent with the pure-black style).

### Validation
- Brace check + git diff --check clean.
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

## Current Request (COMPLETED): Topic catalog not loaded from startup (0 counts / spurious loading)

**Date:** 2026-08-10

### What the user asked
The dataset doesn't load reliably: the category picker shows 0 topics per category (counts only appear after opening a category), and some places show a loading state they shouldn't. Topics should be available and loaded from startup.

### Root cause
Regression from `9ead01a`/`180cbfd`: splash navigation was NOT held hostage by parsing — a concurrent preload raced the 800ms auto-dismiss, so Home rendered with a half-warm cache. `CurioCategoryCard` read only `TopicJsonLoader.cached()?.size ?: 0` inside a latched `remember`, so uncached lanes showed "0 topics" until reopened; Topic Database flashed "Loading topics…" while lanes warmed.

### Changes made
- **SplashScreen.kt** — splash now HOLDS navigation until the canonical catalog is warm: preload runs on `Dispatchers.Default` while the 800ms branding plays, then `withTimeoutOrNull(6s) { warmCatalog.join() }` before navigating (hard cap; per-lane `load` rethrows `CancellationException` so the timeout actually aborts). Individual lane failures are swallowed so one broken asset never blocks the rest.
- **CurioCategoryCard.kt** — topic count is now a `produceState` (seeded from cache, reloads on demand, cancellation-aware) instead of a latched `remember` — cards can never pin a stale "0 topics" after e.g. an `onTrimMemory` cache clear.
- Topic Database / Spin: no changes needed — with the warm cache their loading states only trigger for genuine work.

### Validation
- Brace check + git diff --check clean; code review passed.
- Gradle compile/build/lint/test were not run because the repository explicitly forbids local Gradle commands in this environment.

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
