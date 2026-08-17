# Prompt.md — Request log

## Current request — fix CI compile error + connect the stars on the Stats page (DONE)

User: "lets improve more of the your curiosity page" → ask_user answered
"Connect the stars" + pasted the CI failure: `HomeScreen.kt:2132:41
@Composable invocations can only happen from the context of a @Composable
function` — "fix this too".

### Changes
- **CI FIX (HomeScreen.kt:2132)**: the v177 moon/sun tap called
  `isCurioDarkTheme()` INSIDE the `.clickable {}` lambda — a @Composable
  call in a non-composable context (COMPILE-SAFETY rule 3 violation).
  Fixed: `val isDarkNow = isCurioDarkTheme()` resolved in composition
  before the clickable; the lambda reads `isDarkNow`. Grepped all four
  files touched this session (`-B2 -A2` around isCurioDarkTheme /
  themedAccent / themedButton* / headerAccent / heroHeaderInk) — no other
  violations.
- **STATS WEB (StatsScreen.kt `CategoryConstellation`)**: the old
  lane-order chain + single gold fissure used `#7FAFD8 @ 0.22` — the same
  light-mode invisibility the drawer map had. Stars are now linked as a
  NEAREST-NEIGHBOUR web: every star connects to its 2 closest stars
  (deduped pairs via `LinkedHashSet<Pair<Int,Int>>`, kotlin.collections
  typealias — auto-imported), with theme-aware inks resolved in
  composition: `linkColor` #7FAFD8@0.32 dark / #5F7E9A@0.50 light,
  `fissureColor` #D9A85C@0.30 dark / #A97F3C@0.45 light (gold fissure
  still bridges the two hemispheres).
- Changelog (20260920.txt) + app/AGENTS.md v181 notes added (incl. the
  LESSON: CI runs async, so grep-based checks must explicitly eyeball
  composable helpers inside lambdas).

No compile/test possible in this env (CI validates on push) — edits are
brace-balanced, no imports changed, theme colors resolved in composition.

## Previous request — Spin category picker: no footer, floating no-background Mix/Cancel, hero watermark placement (DONE, shipped 0eeebed)

Removed the Manage categories pill (and `onBrowseAll` + its navigation +
now-unused imports); Mix/Cancel float over the grid as nav-bar-style
controls with no background capsule (grid gains bottom clearance in
multi-select); hero watermark glyphs repositioned (small twin was under
the status bar — added statusBarsPadding — large one was tear-clipped/
hidden behind presets — raised to bottom 58dp, stronger alpha).

## Previous request — Pet Designer theme-aware studio pill, opaque edit prompt, full-bleed hero (DONE, shipped b454c67)

`PetStudioTab` active pill → secondaryContainer/onSecondaryContainer;
`EditorPickPrompt` → solid surfaceContainerHigh; hero full-bleed via
negative edge-padding wrapper.

## Previous request — constellation audit + Stats hero banner matches the drawer (DONE, shipped 2bf548d)

Theme-aware constellation inks + right/down grid web in the drawer map;
StatsSkyHeader loads the theme-picked sky SVG (design unchanged).

## Previous request — tap the moon/sun on the drawer hero to flip the theme (DONE, shipped 1aadf44)

Invisible 48dp hit-circle at (268.8, 52.08); setThemeMode flips to the
opposite effective theme. Always-on.

## Previous request — drawer curiosity map + new flat cropped footer (DONE, shipped ccf2fae)

Plain-surface constellation of ALL lanes + new cropped planet footer flat
at the bottom edge with a fade + credits.

## Previous request — drawer hero sky = user's SVG artwork; revert 6300f774 (DONE, shipped bff5809)

Reverted `6300f774` + `91b4375`; deleted procedural DrawerCelestialSky +
watermark glyphs; hero loads the user's SVGs via Coil. Also `04efc1e`
(chore: icon-font backup + SVG generator committed, package.json stubs
deleted).
