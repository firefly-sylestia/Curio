# Prompt.md — Request log

## Current request — drawer hero sky = user's SVG artwork; revert 6300f774 (DONE)

User: "i uploaded a new svg for the night sky use that and also remove the
watermarks from the backgroud drawer hero" → then, via ask_user: "i want you
to fully revert that <github.com/firefly-sylestia/Curio/commit/6300f774>
this commit revert it fully. also for light mode use another svg i just
uploaded."

### Clarifications (ask_user)
1. Theme use: dark mode shows the uploaded night-sky SVG; light mode shows the
   separately uploaded day-sky SVG (`curio_day_sky_fixed(1).svg`) — i.e. the
   hero sky is ALWAYS artwork now, theme-picked.
2. Galaxy panel question skipped — the full revert removes the constant-galaxy
   panel anyway (drawer returns to the boxed real-data curiosity map).

### Changes
- Reverted `6300f774` ("constant galaxy, flat opaque footer, opaque buttons,
  lifetime totals + badges") and its CI follow-up `91b4375` (RowScope fix for
  the now-deleted `DrawerLifetimePane`) via `git revert --no-commit` — drawer
  back to the v174e/v174f sky-tear design. Changelog + AGENTS.md additions
  from that commit reverted with it.
- Drawer hero (HomeScreen.kt `HomeDrawerContent`): procedural
  `DrawerCelestialSky` (stars/grain/constellation/sparkles/moon) and the
  mirrored watermark glyph collage (`heroSymbols`/`heroPairs`) DELETED. The
  banner now loads the user's SVG via Coil `SvgDecoder` (same path as the
  footer): dark → `res/raw/drawer_hero_sky_dark.svg` (uploaded
  `svgviewer-output (11).svg` — dark palette, moon-edge sparkle removed),
  light → `res/raw/drawer_hero_sky_light.svg` (uploaded
  `curio_day_sky_fixed(1).svg` — day sky, sun at the moon's 0.84/0.28
  position, clouds, birds). Theme gradient kept as the loading backdrop;
  greeting + avatar unchanged on top.
- Dead code removed: `DrawerCelestialSky`, `SkyStar`/`SkyLink`/`SkySparkle`,
  `drawSparkle`, and the `Path`/`DrawScope` imports (only the hero used them).
- Changelog (20260920.txt) + app/AGENTS.md v175 notes updated.

No compile/test possible in this env (CI validates on push) — changes follow
COMPILE-SAFETY rules (no sed, checked references/imports, ImageRequest in
remember keyed by res id).
