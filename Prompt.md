# Prompt.md — Request log

## Current request — constellation audit (light-mode visibility + connectivity) + Stats hero banner matches the drawer (DONE)

User: "do a proper audit that the constelations lines and star colors are
right and visible in light mode and each stars in connected somehow and
now use the day one view in drawer hero in your curiocity hero too, dont
chnage the design just the banner style of the header."

### Clarifications (ask_user)
- Stats header sky: theme-picked like the drawer (night sky in dark, day
  sky in light) — answered via ask_user.

### Changes
- **Audit — `DrawerLaneConstellation` (HomeScreen.kt):** the old links
  (`#7FAFD8 @ 0.20`) and tiny stars (`@ 0.30`) vanished on the white
  drawer surface in light mode; idle dots `#AFC9D4` were near-invisible.
  Inks are now resolved in COMPOSITION (they call `isCurioDarkTheme`,
  which can't run inside the Canvas draw lambda): `linkColor` =
  #7FAFD8@0.30 dark / #5F7E9A@0.55 light; `tinyStarColor` = #7FAFD8@0.35
  / #5F7E9A@0.50; `idleDotColor` = #4A5F6E dark / #7E9CB0 light (steel
  slate, reads on white). Explored chips already correct.
- **Connectivity:** the closed zigzag chain (i → (i+1)%n, links hidden
  under the 34dp chips) is replaced by a GRID WEB — each node connects to
  its RIGHT (col < c-1) and DOWN (row < r-1) grid neighbours, with c/r
  recomputed in-canvas using the same formula as the layout. Every star
  gets 2–4 visible links; the constellation reads as a connected mesh.
- **Stats header — `StatsSkyHeader` (StatsScreen.kt):** the procedural
  gradient + 22 seeded stars + carved crescent moon are gone. The band
  loads the SAME theme-picked SVG as the drawer hero
  (`R.raw.drawer_hero_sky_dark` / `drawer_hero_sky_light`) via Coil
  `SvgDecoder`, `ContentScale.Crop`, over the theme gradient as the
  loading backdrop. Design unchanged: rounded 30dp bottom tear, warm-white
  back pill (skyInk glyph), "Your Curiosity" / "Stats, streaks & insights"
  title. The 320×186 SVG (1dp/unit) center-crops into the 148dp band.
  Added imports: ContentScale, AsyncImage, SvgDecoder, ImageRequest, R.
  `Canvas`/`Offset`/`Random`/`lerp` stay (CategoryConstellation still
  draws).
- Changelog (20260920.txt) + app/AGENTS.md v178 notes added.

No compile/test possible in this env (CI validates on push) — changes
follow COMPILE-SAFETY rules (theme inks resolved in composition, no sed,
verified every import/reference, `Dp * Float` ordering preserved).

## Previous request — tap the moon/sun on the drawer hero to flip the theme (DONE, shipped 1aadf44)

User: "make it so when i tap the moon or the sun the theme switch between
light and dark". Always-on per ask_user. Added an invisible 48dp hit-circle
at (268.8, 52.08) in the drawer hero's sky Box (both celestial bodies sit
there in the 1:1 320×186dp SVGs); tap calls
`AppPreferences.setThemeMode(context, if (isCurioDarkTheme()) THEME_LIGHT
else THEME_DARK)` — flips to the opposite of the current effective theme.
Changelog + AGENTS.md v177 notes added.

## Previous request — drawer curiosity map = plain-surface constellation of ALL lanes + new flat cropped footer (DONE, shipped ccf2fae)

User: "and then remove the box behind your curiocity, and show the mind
connection constellation map with inactive glow stars. and make the icons
in the active star as non transparent. and in the constellation map view
show more avalable data. and not the hero banner or just a drawing. so ask
me properly for clarification." and "i also added a new cropped footer
place this and place it much below from the end of the footer, and dont
give it shadow or anything just the footer at the end no scaffholding as
well" (with the uploaded `curio_planet_cropped_bottom_264.svg`).

- `DrawerCuriosityMap`: box/title/"This Week ˅" selector gone; Column on
  the plain drawer surface; data from `CurioPassport.allProgress` (all-time
  spins/reveals/explores/saves/lastAt).
- `DrawerLaneConstellation`: EVERY visible lane is a star; explored =
  solid `themedAccent` chips + `onAccent` icon + glow, tappable → richer
  panel; inactive = solid 14dp muted dots; 16 seeded extra stars; 196dp.
- `DrawerFooter`: new cropped planet SVG, flat at the bottom edge, bottom
  fade + credits inside; LazyColumn bottom padding 20dp → 0dp.
- Changelog + AGENTS.md v176 updated. Committed `ccf2fae`, pushed (carried
  `04efc1e`).

## Previous request — drawer hero sky = user's SVG artwork; revert 6300f774 (DONE, shipped bff5809)

Reverted `6300f774` + `91b4375`; deleted procedural `DrawerCelestialSky` +
watermark glyphs; hero loads the user's SVGs via Coil (dark →
`drawer_hero_sky_dark.svg`, light → `drawer_hero_sky_light.svg`). Changelog
+ AGENTS.md v175 updated. Committed `bff5809`; also `04efc1e` (chore:
icon-font backup + SVG generator committed, empty package.json stubs
deleted).
