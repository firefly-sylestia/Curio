# Prompt.md — Request log

## Current request — Pet Designer theme-aware studio pill, opaque edit prompt, full-bleed hero (DONE)

User: "make the pet desinger gets the theme aware as well. and make the
what do you want to edit box non trasparent. and fix its header tear hero
too as its cut from sides."

### Clarifications (ask_user)
- "the floating nav pill doesnt and also fix its hero banner side cut" —
  the theme issue is the STUDIO bottom pill bar, not the page (the page is
  already MaterialTheme-driven).

### Changes (PetDesignerScreen.kt)
- **Studio pill (`PetStudioTab`)**: the ACTIVE tab wore the stale solid
  `secondary` + `onSecondary` (the butter the main nav bar dropped in
  v161). Now uses the nav bar's plain-page language —
  `secondaryContainer` + `onSecondaryContainer` (light/dark aware). The
  v147b doc comment updated to match.
- **Edit prompt (`EditorPickPrompt`)**: the "What do you want to edit?"
  card's fill `lerp(surface, primaryContainer, 0.55f)` read as a
  translucent plate over the page's lane wash — replaced with the solid
  `surfaceContainerHigh` (same elevated container as DialogScrim) so it's
  clearly opaque. `lerp` still used elsewhere (2321, 4559).
- **Hero side cut**: `SettingsHeroHeader` is the FIRST LazyColumn item and
  the list's `contentPadding(start/end = wideContentEdgePadding())`
  inset it 16dp+ each side. Fix: compute `val edgePad =
  wideContentEdgePadding()` once, use it in contentPadding, and wrap the
  hero item in `Box(Modifier.fillMaxWidth().padding(horizontal =
  -edgePad))` so the banner bleeds to both screen edges (content below
  stays padded; works on wide screens too).
- Changelog (20260920.txt) + app/AGENTS.md v179 notes added.

No compile/test possible in this env (CI validates on push) — changes are
small modifier/color swaps; verified no imports changed (`Box`,
`fillMaxWidth`, `PaddingValues` already in use; `surfaceContainerHigh` /
`secondaryContainer` / `onSecondaryContainer` are standard M3 scheme
members already used in this file).

## Previous request — constellation audit + Stats hero banner matches the drawer (DONE, shipped 2bf548d)

User: "do a proper audit that the constelations lines and star colors are
right and visible in light mode and each stars in connected somehow and
now use the day one view in drawer hero in your curiocity hero too, dont
chnage the design just the banner style of the header." (Stats header sky =
theme-picked like the drawer per ask_user.)

- `DrawerLaneConstellation`: theme-aware inks (dark #7FAFD8@0.30 lines /
  #7FAFD8@0.35 tiny stars / #4A5F6E idle dots; light #5F7E9A@0.55 lines /
  @0.50 tiny stars / #7E9CB0 dots — resolved in composition since
  isCurioDarkTheme can't run in Canvas). Replaced the zigzag chain with a
  RIGHT+DOWN grid web so every star gets 2–4 visible links.
- `StatsSkyHeader` (StatsScreen.kt): procedural gradient + 22 stars +
  carved moon GONE; now loads the same theme-picked SVG as the drawer
  hero (R.raw.drawer_hero_sky_dark/light) via Coil over the theme
  gradient. Design unchanged (rounded tear, back pill, title).
- Changelog + AGENTS.md v178 notes added.

## Previous request — tap the moon/sun on the drawer hero to flip the theme (DONE, shipped 1aadf44)

Always-on per ask_user. Invisible 48dp hit-circle at (268.8, 52.08) in the
drawer hero's sky Box; tap calls `AppPreferences.setThemeMode(context, if
(isCurioDarkTheme()) THEME_LIGHT else THEME_DARK)`. Changelog + AGENTS.md
v177 notes added.

## Previous request — drawer curiosity map + new flat cropped footer (DONE, shipped ccf2fae)

Plain-surface constellation of ALL lanes (explored glow + tap data,
inactive solid smaller dots), new cropped planet footer flat at the bottom
edge with a fade + credits. Changelog + AGENTS.md v176 updated.

## Previous request — drawer hero sky = user's SVG artwork; revert 6300f774 (DONE, shipped bff5809)

Reverted `6300f774` + `91b4375`; deleted procedural `DrawerCelestialSky` +
watermark glyphs; hero loads the user's SVGs via Coil (dark →
`drawer_hero_sky_dark.svg`, light → `drawer_hero_sky_light.svg`).
Changelog + AGENTS.md v175 updated. Also `04efc1e` (chore: icon-font
backup + SVG generator committed, empty package.json stubs deleted).
