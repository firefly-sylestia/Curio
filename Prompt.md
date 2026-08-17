# Prompt.md — Request log

## Current request — drawer curiosity map = plain-surface constellation of ALL lanes + new flat cropped footer (DONE)

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
- `DrawerCuriosityMap` (HomeScreen.kt): the boxed card, the "Your
  Curiosity Map" title and the "This Week ˅" selector are GONE. The map
  is a Column on the plain drawer surface (whole-map tap still opens
  Stats). Data comes straight from `CurioPassport.allProgress` — all-time
  per-lane spins/reveals/explores/saves/lastAt (no repo, no
  `CurioQuests`, no `StatsRange` imports).
- `DrawerLaneConstellation`: EVERY visible lane is a star. Explored lanes
  = solid `themedAccent` chips (34dp) with their `onAccent` icon + glow,
  tappable → opens a richer panel (lane icon, accent-tinted surface,
  last-explored relative time, `DrawerMapStat` panes for spins / peeked /
  explores / saved, close button). Inactive lanes = solid 14dp dots
  (muted blue-grey, no alpha) — differentiated by size + color. 16 seeded
  extra tiny stars + faint neighbour-connecting lines. Deterministic grid
  scatter + `Random(id.name.hashCode())` jitter; 196dp tall so all 36
  lanes fit.
- `DrawerFooter`: `res/raw/drawer_footer.svg` replaced with the user's
  cropped `curio_planet_cropped_bottom_264.svg` (1536×760). Footer is a
  flat 210dp Box at the very bottom — `ContentScale.Crop` bottom-anchored
  art, no shadow/box/scaffolding; a 110dp vertical gradient fades the art
  into the drawer surface and the version + "Made with curiosity" row
  (warm tan ink, nav-bars-padded) sits inside the fade. LazyColumn
  `contentPadding.bottom` 20dp → 0dp so the art touches the bottom edge.
- Imports: `CurioQuests` → `CurioPassport`; removed `StatsRange*`,
  `filterForRange`, and the now-unused `Modifier.alpha`. Changelog
  (20260920.txt) + app/AGENTS.md v176 notes updated.

No compile/test possible in this env (CI validates on push) — changes
follow COMPILE-SAFETY rules (no sed, checked every import/reference,
`Dp * Float` ordering per the earlier CI lesson, accents resolved in
composition not inside Canvas).

## Previous request — drawer hero sky = user's SVG artwork; revert 6300f774 (DONE, shipped)

User: "i uploaded a new svg for the night sky use that and also remove the
watermarks from the backgroud drawer hero" → then, via ask_user: "i want you
to fully revert that <github.com/firefly-sylestia/Curio/commit/6300f774>
this commit revert it fully. also for light mode use another svg i just
uploaded."

### Clarifications (ask_user)
1. Theme use: dark mode shows the uploaded night-sky SVG; light mode shows the
   separately uploaded day-sky SVG (`curio_day_sky_fixed(1).svg`) — the hero
   sky is ALWAYS artwork now, theme-picked.
2. Galaxy panel question skipped — the full revert removes the constant-galaxy
   panel anyway (drawer returns to the boxed real-data curiosity map).

### Changes
- Reverted `6300f774` (constant galaxy, flat opaque footer, opaque buttons,
  lifetime totals + badges) and its CI follow-up `91b4375` (RowScope fix for
  the deleted `DrawerLifetimePane`) via `git revert --no-commit` — drawer back
  to the v174e/v174f sky-tear design. Changelog + AGENTS.md additions from
  that commit reverted with it.
- Drawer hero: procedural `DrawerCelestialSky` (stars/grain/constellation/
  sparkles/moon) and the mirrored watermark glyph collage
  (`heroSymbols`/`heroPairs`) DELETED. The banner now loads the user's SVG via
  Coil `SvgDecoder`: dark → `res/raw/drawer_hero_sky_dark.svg` (uploaded
  `svgviewer-output (11).svg`), light → `res/raw/drawer_hero_sky_light.svg`
  (uploaded `curio_day_sky_fixed(1).svg`). Theme gradient kept as the loading
  backdrop; greeting + avatar unchanged.
- Dead code removed: `DrawerCelestialSky`, `SkyStar`/`SkyLink`/`SkySparkle`,
  `drawSparkle`, `Path`/`DrawScope` imports.
- Committed as `bff5809` (revert + hero SVGs) and `04efc1e` (chore: committed
  the full icon-font backup `tools/fonts/material_symbols_outlined_full.ttf`
  + `tools/gen_drawer_banner_svg.py`; deleted the empty package.json /
  package-lock.json stubs — NOT pushed, rides with the next real change).
