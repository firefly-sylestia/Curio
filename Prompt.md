# Prompt.md — Request log

## Current request — Spin category picker: no footer, floating no-background Mix/Cancel, hero watermark placement (DONE)

User: "ykw in spin screen category picker remove anything that below the
category cards like remove it fully. no need for manage category and make
the mix and cancel button appear as nav bar style pill only when selected
multiple. with no backgroud and floating style. and also the hero banner
of it chnage its glyph style and placement snon eo fthe icon is visible
and makes its backgroud tint take theme aware too and also watermark"

### Clarifications (ask_user)
- Mix/Cancel pill: NO background — just the buttons floating over the
  grid, no capsule fill.
- Hero glyphs: "the ions are not visible due to the placement so fix the
  icon placings" — a PLACEMENT bug, not a removal. (The hero fill/wash/
  ink were already theme-aware via headerAccent / categoryBackgroundWash /
  heroHeaderInk.)

### Changes (SpinScreen.kt `CategoryPickerSheet`)
- **Footer removed**: the "Manage categories" floating pill is gone —
  nothing sits below the category cards in single-select. Removed the
  `onBrowseAll` param (signature + call site + its Manage-Categories
  navigation) and the now-unused `curioFloatingNavContainerFor` +
  `ButtonDefaults` imports.
- **Mix/Cancel floating**: in multi-select the controls are a floating
  Row (`align(BottomCenter)`, bottom 18dp) inside the grid's weight Box —
  no background capsule. Mix is a solid category pill (themedButtonFill +
  themedButtonInk; Button → Surface swap, `return@Surface`), Cancel is a
  plain TextButton. Grids' bottom contentPadding is now `if
  (multiSelectMode) 88dp else 20dp` so the floating controls never cover
  the last card row.
- **Hero watermark placement**: the small twin was UNDER the status bar
  (its `align(TopStart)` had no `statusBarsPadding` — the title column
  had it) and the 72dp large one was clipped by the tear + hidden behind
  the tabs/presets rows. Fixed: small gets `.statusBarsPadding()`
  (top-left corner), large is 64dp raised to `bottom = 58dp` (right edge
  just above the preset chips — the tabs row is left-aligned, so that
  corner is empty), alphas bumped 0.07/0.10 → 0.10/0.14.
- Changelog (20260920.txt) + app/AGENTS.md v180 notes added. NOTE: the
  FilterSheet hero (line ~1814) has the SAME placement bug — left unfixed
  (user asked about the picker only).

No compile/test possible in this env (CI validates on push) — brace
balance verified via awk (80/80), imports/references checked, `Dp * Float`
ordering preserved.

## Previous request — Pet Designer theme-aware studio pill, opaque edit prompt, full-bleed hero (DONE, shipped b454c67)

User: "make the pet desinger gets the theme aware as well..." Clarified:
"the floating nav pill doesnt and also fix its hero banner side cut".
- `PetStudioTab` active pill: stale `secondary`/`onSecondary` (the butter
  the nav bar dropped) → `secondaryContainer`/`onSecondaryContainer`.
- `EditorPickPrompt`: lerp tint → solid `surfaceContainerHigh`.
- Hero side cut: `SettingsHeroHeader` is the first list item, inset by
  `wideContentEdgePadding()`; fixed with
  `Box(Modifier.fillMaxWidth().padding(horizontal = -edgePad))` so the
  banner bleeds full-width.

## Previous request — constellation audit + Stats hero banner matches the drawer (DONE, shipped 2bf548d)

Theme-aware constellation inks (light-mode visibility) + right/down grid
web links in `DrawerLaneConstellation`; `StatsSkyHeader` now loads the
same theme-picked sky SVG as the drawer hero (design unchanged).

## Previous request — tap the moon/sun on the drawer hero to flip the theme (DONE, shipped 1aadf44)

Invisible 48dp hit-circle at (268.8, 52.08); `setThemeMode` flips to the
opposite of the current effective theme. Always-on.

## Previous request — drawer curiosity map + new flat cropped footer (DONE, shipped ccf2fae)

Plain-surface constellation of ALL lanes + new cropped planet footer flat
at the bottom edge with a fade + credits.

## Previous request — drawer hero sky = user's SVG artwork; revert 6300f774 (DONE, shipped bff5809)

Reverted `6300f774` + `91b4375`; deleted procedural `DrawerCelestialSky` +
watermark glyphs; hero loads the user's SVGs via Coil. Also `04efc1e`
(chore: icon-font backup + SVG generator committed, empty package.json
stubs deleted).
