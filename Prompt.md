# Prompt.md — Request log

## Current request — tap the moon/sun on the drawer hero to flip the theme (DONE)

User: "make it so when i tap the moon or the sun the theme switch between
light and dark"

### Clarifications (ask_user)
- Always-on per the user (no Settings toggle) — new-feature question asked
  per AGENTS.md, answered "Always-on".

### Changes
- HomeScreen.kt, drawer hero sky Box (`HomeDrawerContent`): a 48dp
  INVISIBLE hit-circle (`Box` with `.offset(x = 268.8.dp - 24.dp,
  y = 52.08.dp - 24.dp).size(48.dp).clip(CircleShape).clickable {...}`)
  sits exactly on the moon/sun. Both celestial bodies live at (268.8,
  52.08) in their SVGs and the artwork is 320×186dp — 1:1 with the hero
  box, so `ContentScale.Crop` maps positions directly.
- The tap calls `AppPreferences.setThemeMode(context, if
  (isCurioDarkTheme()) THEME_LIGHT else THEME_DARK)` — flips to the
  opposite of the current EFFECTIVE theme (System resolves first, then
  forces light/dark). `themeModeState` is a `mutableStateOf`, so the app
  rethemes instantly and the hero SVG crossfades (`crossfade(true)`).
- Changelog (20260920.txt) ADD bullet + app/AGENTS.md v177 notes added.

No compile/test possible in this env (CI validates on push) — the change
is a pure-modifier addition (no new imports needed: AppPreferences,
isCurioDarkTheme, clip, CircleShape, clickable, offset, size all already
in use in the file; `context` in scope from HomeDrawerContent).

## Previous request — drawer curiosity map = plain-surface constellation of ALL lanes + new flat cropped footer (DONE, shipped ccf2fae)

User (two messages): "and then remove the box behind your curiocity, and
show the mind connection constellation map with inactive glow stars. and
make the icons in the active star as non transparent. and in the
constellation map view show more avalable data. and not the hero banner
or just a drawing. so ask me properly for clarification." and "i also
added a new cropped footer place this and place it much below from the
end of the footer, and dont give it shadow or anything just the footer at
the end no scaffholding as well" (with the uploaded
`curio_planet_cropped_bottom_264.svg`).

### Clarifications (ask_user, 2 rounds)
1. Map: remove the box ENTIRELY plus the title text and the dropdown —
   always all-time data in the drawer.
2. Lanes: ALL lanes as stars; explored lanes get the glow + icon and show
   data when tapped; inactive lanes solid too but differentiated by SIZE
   + COLOR (not transparency); add a few extra tiny stars; hero unchanged.
3. Map sits on the PLAIN drawer surface (no panel/sky-gradient slice);
   only explored lanes respond to taps.
4. Footer: at the very bottom end of the drawer, keep the version +
   "Made with curiosity" line, and add a bottom fade so the art doesn't
   look like it's floating.

### Changes
- `DrawerCuriosityMap`: boxed card, title and "This Week ˅" selector
  GONE; Column on the plain drawer surface (whole-map tap still opens
  Stats); data straight from `CurioPassport.allProgress` (all-time
  spins/reveals/explores/saves/lastAt — no repo, no `CurioQuests`, no
  `StatsRange`).
- `DrawerLaneConstellation`: EVERY visible lane is a star; explored =
  solid `themedAccent` chips (34dp) + `onAccent` icon + glow, tappable →
  richer panel (icon, accent surface, last-explored time, `DrawerMapStat`
  panes, close button); inactive = solid 14dp muted dots (no alpha);
  16 seeded extra stars + neighbour lines; deterministic grid scatter +
  `Random(id.name.hashCode())` jitter; 196dp tall.
- `DrawerFooter`: `drawer_footer.svg` replaced with the user's cropped
  `curio_planet_cropped_bottom_264.svg` (1536×760); flat 210dp Box at the
  very bottom (Crop bottom-anchored, no shadow/box), 110dp bottom fade
  into the surface, version + "Made with curiosity" inside the fade;
  LazyColumn bottom padding 20dp → 0dp.
- Imports: `CurioQuests` → `CurioPassport`; removed `StatsRange*`,
  `filterForRange`, unused `Modifier.alpha`. Changelog + AGENTS.md v176
  notes updated. Committed `ccf2fae` and pushed (carried the unpushed
  `04efc1e` chore).

## Previous request — drawer hero sky = user's SVG artwork; revert 6300f774 (DONE, shipped bff5809)

User: "i uploaded a new svg for the night sky use that and also remove the
watermarks from the backgroud drawer hero" → then, via ask_user: "i want you
to fully revert that <6300f774>... also for light mode use another svg i just
uploaded." Reverted `6300f774` + its CI follow-up `91b4375`; deleted
procedural `DrawerCelestialSky` + watermark glyphs; hero now loads the
user's SVGs via Coil (dark → `drawer_hero_sky_dark.svg`, light →
`drawer_hero_sky_light.svg`). Changelog + AGENTS.md v175 updated.
Committed `bff5809`; also `04efc1e` (chore: full icon-font backup +
SVG generator committed, empty package.json stubs deleted — not pushed on
its own, rode up with ccf2fae).
