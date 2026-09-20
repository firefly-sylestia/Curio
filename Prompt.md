# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "now the drawer map is good, fine ig but still not beautiful and geometric enough and it
> doesnt cover the drawer a little more. also push the previous"

Two parts: push the held commits (done — `8032b37b..56d318bb`), then redesign the Home
drawer's curiosity map.

## 2. What the code actually looked like (findings)

- The drawer's map is `DrawerLaneStarMap` in `HomeScreen.kt`: a 188dp panel
  (`DrawerStarMapHeight`) on `surfaceContainerHigh`, filled by a `Canvas`.
- Star positions came from `starScatter(count)` — a **phyllotaxis** (sunflower) scatter in
  UNIT space (0..1), multiplied by `size.width` / `size.height` separately in the canvas, so
  the layout stretched into an ellipse on a wide panel.
- Hairlines came from `starLinks(stars)` — every star joined to its **two nearest**
  neighbours (O(n²)), i.e. a mesh, not a structure.
- The tap test worked in the same unit space with a `min(width, height)`-derived reach — a
  different mapping from the painter's, which is why it could drift.
- Nothing geometric was drawn: the panel was an empty tinted rectangle with a cloud of dots.

## 3. What was done

All in `app/src/main/java/com/curio/app/features/home/HomeScreen.kt`:

- **`DrawerStarMapHeight` 188dp → 254dp**, so the chart covers more of the drawer.
- **`StarSlot(angle, radius)` replaced the unit-space `Offset` scatter**, with
  `starPoint(slot, hub, unitPx)` as the ONE placement function — and it multiplies the
  radius by the SHORTER side, so the orbits are true circles in a wide drawer.
- **`starLattice(count)` replaced `starScatter`:** 1–3 orbits by lane count at radii evenly
  spaced 0.30–0.92 of the half-height, capacity proportional to radius (even spacing on
  every orbit), stars spread evenly by angle, odd orbits half a step out of phase. Still
  deterministic, and knowledge still never moves a star.
- **`starRingLinks` replaced `starLinks`:** each orbit is a CLOSED POLYGON through its own
  stars (chord between neighbours, last back to the first) — geometry instead of a mesh.
- **The chart itself is now drawn** under the stars: one faint circle per orbit actually
  used, 12 radial spokes, and a small hub — all opaque `lerp(panel, muted, 0.16f)` hairlines.
- **The hit test moved to pixel space** through the same `starPoint`, with a 30dp halo, so a
  tap can no longer miss the star it looks like it hit.
- `sqrt` import dropped with the phyllotaxis; the stale `[starLinks]` KDoc reference fixed.

Docs + notes: a new `### v414 — the drawer chart: a lattice, and more of the drawer` section
in `app/AGENTS.md` and one ADD bullet in the 20260922 changelog.

## 4. Decisions

- **The user's "geometric" was read as structure, not as a different metaphor:** the stars
  stay stars and the lanes stay a constellation, but they now sit on countable orbits and
  readable spokes with a drawn chart under them, instead of an unstructured cloud.
- **"Cover the drawer a little more" was read as height** (188 → 254dp). The map already
  fills the drawer's width; if they meant edge-to-edge bleed, that is a separate change and
  would need the drawer's horizontal padding to give way.
- Radius is scaled by the SHORTER side so orbits stay round — the old unit-space mapping
  could not express that, which is why the slot type was introduced rather than patching the
  scatter.
- No ask_user round here: the direction was short but the two clear asks (bigger, more
  geometric) had a defensible reading, and the change is internal to one composable.

## 5. Status

- Implemented; brace/paren balance verified (0/0 before and after).
- The previously held commits are **pushed** (`8032b37b..56d318bb`).
- No pending follow-up.
