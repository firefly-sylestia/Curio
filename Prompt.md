# Prompt.md — Request log

## Current request — drawer curiosity-map rework: no fake data, sky-tear banner, new footer (DONE, pushed)

User: "dont use fake data, use empty state, extend the drawer banner's
sky design and its color to the tear start so it looks like a sky tear.
In your curiosity map, don't show the helper text 'a little galaxy of
everything you've explored' when there's data. Make the gallery proper,
remove the learned/explored/topics/questions/saved/shared/streak stats,
only use what exists, don't show them always. Make the dots have the
rounded icon connected, tapping shows the data. Make the gallery stretch
properly. Replace the footer svg with this one (no background), place it
more below and less opaque so 'Made with curiosity' is visible."

### Clarifications (ask_user)
1. Drop the 7 orbiting stat chips entirely (Learned/Explored/Topics/Questions/
   Saved/Shared/Streak + Overall overlay) — the map card = constellation +
   range selector + empty state.
2. Rounded lane-icon dots, connected, tap-to-show-data: the DRAWER map only
   (the stats page constellation stays as-is).
3. "Sky tear" = the DRAWER banner (extend the sky to the torn seam); the
   stats page is kept as-is, just tuned for light mode.

### Changes
- `DrawerCuriosityMap` (HomeScreen.kt): fake `MapStat`s + `ConstellationBrain`
  deleted. Card now loads real entries (repo.getAll in LaunchedEffect), filters
  via the SHARED `filterForRange` (moved from StatsScreen.kt → StatsRange.kt),
  and renders `DrawerLaneConstellation` — one rounded 34dp lane-icon chip per
  explored lane (two-lobe arc, thin lines, tap → inline lane stats panel).
  Helper copy only shows in the empty state. Deleted MapStat/ConstellationBrain.
- Sky tear: `DrawerRollingHorizon` (cream hills) deleted — sky now runs to the
  torn seam.
- Light-mode tune: `DrawerCelestialSky` + `StatsSkyHeader` starTint is
  theme-aware (light = deep seafoam ink #2C5A53, dark = warm white); stats
  back-pill keeps explicit warm-white fill.
- Footer: `res/raw/drawer_footer.svg` replaced with the user's new SVG (no
  `#FCF3E8` background rect); AsyncImage → Alignment.BottomCenter + alpha 0.55
  so the version + "Made with curiosity" line reads.
- Changelog + app/AGENTS.md v174e notes updated.

No compile/test possible in this env (CI validates on push) — the changes
follow the COMPILE-SAFETY rules (themedAccent outside remember, ColumnScope
receivers, no TextAutoSize, etc.).
