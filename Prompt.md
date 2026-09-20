# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "the drawer graph is bad like othe previewus version was atleast better, do something
> about it plaes its too symmetric"

The member reversed the v414 direction on the Home drawer's lane star map: the ring-and-spoke
lattice reads as too symmetric, and the earlier phyllotaxis scatter was better.

## 2. Findings

- `DrawerLaneStarMap` (HomeScreen.kt) drew: `starLattice(count)` → 1–3 orbits at even
  radii, even angles per orbit, half-step phase on odd orbits; plus a grid of orbit
  circles, `STAR_CHART_SPOKES` (12) spokes and a hub; plus `starRingLinks` (a closed
  polygon per orbit).
- Exactly that regularity was the complaint. `StarSlot(angle, radius)` + `starPoint(...)`
  are the placement/hit-test pair and are worth keeping.

## 3. What was built (v419)

- **`starScatter(count)`** replaces `starLattice`: the i-th star at the GOLDEN ANGLE
  (2.3999632 rad) × i, radius `0.20 + 0.74 * sqrt((i + 0.55) / count)`, with a
  deterministic 0..7 hashed wobble (`i * 2654435761L and 7`) on both angle and radius.
  Deterministic; knowledge still only changes size/brightness.
- **The grid is gone** — no orbit circles, no spokes, no hub; `STAR_CHART_SPOKES` and
  `TWO_PI` deleted. A faint **`starDust(STAR_DUST_COUNT = 46)`** field (deterministic LCG,
  unit space) gives the panel depth.
- **`starLinks`** (nearest neighbour, unit-space `getDistanceSquared`) replaces
  `starRingLinks`, so hairlines read as loose constellations.
- `StarSlot` and `starPoint` are unchanged, so the tap target still matches the paint.

## 4. Verification

- Brace/paren balance 0/0 on `HomeScreen.kt`; no remaining `starLattice` / `starRingLinks`
  / `TWO_PI` / `STAR_CHART_SPOKES` / `orbits` references; `cos` / `sin` already imported.
- No Gradle in this environment — CI validates the compile.

## 5. Open notes

- `DrawerStarMapHeight` stays 254dp (v414); only the arrangement changed.
- If the scatter is now TOO loose, the wobble constants (`0.055` angle, `0.011` radius)
  and the `0.20 + 0.74 * sqrt(...)` spread are the two dials to tune.

---

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — this request is logged above as §1–§5)
