# Prompt.md — Request log

## Current request — drawer: constant galaxy (box removed), flat opaque footer, opaque buttons, lifetime totals + badges (DONE, pushed)

User: "lets fix the drawer, can u crop the footer from the button a little
more and don give it transparecny or shadow or border. and then remove the
box for your curiocity galaxy, and show a contantt galaxy always only
starts showing the point when something gets added. and also make the
buttons non transparect, and also add more things like spinning, explores
likes etc from the lifetime totals data. also badge collected. etc"

### Interpretation (no ask_user needed — all four asks were concrete)
1. Footer: crop the illustration a little more (shorter), fully opaque — no
   transparency fade, no shadow, no rounded border panel.
2. Curiosity galaxy: remove the card box (border + shadow + elevated
   surface); the galaxy (sky + stars) is ALWAYS visible; the lane POINTS
   only appear once a lane has something added.
3. Buttons non-transparent: the drawer's tappable rows/controls get opaque
   fills.
4. More stats: show spins / explores / likes / etc. from the lifetime
   totals data (`CurioQuests.lifetimeState`) plus the earned badge count.

### Changes
- `DrawerFooter` (HomeScreen.kt): the 188dp shadowed/rounded box, alpha
  0.55, and the fade overlay are GONE. The SVG is now a flat opaque
  AsyncImage (160dp, `Alignment.BottomCenter` crop — top sky trimmed) with
  no alpha/shadow/clip, and the "v1.1.0 · Made with curiosity ♥" row moved
  BELOW the image onto the plain surface (credits stay readable over an
  opaque illustration). Removed the now-unused `Modifier.alpha` import.
- `DrawerCuriosityMap`: the bordered/shadowed card Surface is removed — the
  map is a flat Column (whole column `clickable` → Stats page; dots / range
  selector still consume their own taps). Title renamed "Your Curiosity
  Galaxy". A constant galaxy panel (rounded 24dp sky gradient +
  `DrawerCelestialSky` stars/sparkles/moon — no border, no shadow) ALWAYS
  shows; `DrawerLaneConstellation` dots render only when `explored` is
  non-empty. Helper copy + empty-state column deleted (the galaxy IS the
  empty state). Selected-lane panel + dismiss chip fills → opaque lerps.
- NEW `DrawerLifetimeStrip` + `DrawerLifetimePane`: 3×3 grid of opaque
  panes from lifetimeState — Spins, Explores, Saved, Quotes, Pins, Likes,
  Dislikes, Daily + Badges (`allStages().count { isStageDone(it) }`).
- Buttons opaque: `DrawerNavItem` rows `Color.Transparent` →
  `lerp(surfaceContainerHigh, iconTint, 0.06f)`; both collapsible group
  cards `surfaceContainerHigh.copy(alpha = 0.45f)` → opaque
  `lerp(surface, surfaceContainerHigh, 0.45f)`; the SHARED
  `StatsRangeSelectorPill` (StatsRange.kt, also on the stats page) →
  opaque `surfaceContainerHigh`.
- Version tag: v174g (v174f was taken by the APK-slimming commit).
- Docs: app/AGENTS.md v174g entry + fastlane changelog drawer bullets.

No compile/test possible in this env (CI validates on push) — the changes
follow the COMPILE-SAFETY rules (opaque lerp fills for anything under a
shadow, `themedAccent` outside remember, no new Material3 APIs).
