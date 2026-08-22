# Prompt.md — current request log

## Request (complete): liquid-glass scroll morph on top-bar pills (v230) — COMMITTED, NOT PUSHED

User clarified "that blue" = the liquid-glass blur. Ask: expand the glass to
the floating top-bar buttons — Home drawer-menu pill, profile pill (only when
no avatar photo), and the EntryDetail back/more pills — where the resting
color stays solid exactly like the hero, and the scrolled endpoint becomes a
real liquid-glass pill. Parallax-on-tilt was asked as a QUESTION ONLY — user
said don't build it; answered in the summary, not implemented.

Also first: pushed the stranded CI fix `ab42002` (dialog states declared
before the permission launcher that captures them).

### Changes

- `LiquidGlassPills.kt`: `liquidGlassCapsule` gains `washAlpha: Float = 0.40f`
  (the translucent wash over the refracted backdrop) so callers can drive the
  glass strength with scroll.
- `HomeScreen.kt`: menu + profile pills animate fills independently — when the
  experiment is on their background lerps hero→TRANSPARENT and a
  `liquidGlassCapsule(heroPillBg, washAlpha lerp(0.92→0.45))` takes over past
  1% frostShift (Surface elevation drops, glass draws its own shadow).
  Profile keeps the CLASSIC morph while an avatar photo is set.
- `EntryDetailScreen.kt` DetailStickyBar: classic path now rests at the exact
  solid hero fill (lift moved into frostShift); scrolled back/more buttons
  swap `heroFrostPlate` for `liquidGlassCapsule(heroFill, …)` under the
  experiment.

Safety: all three pill sites sit INSIDE the NavHost capture subtree — covered
by the v228 self-capture guard (plain capsule during record passes).

Verification: delimiter balance OK on all three files; no stale `pillBg`
refs; CI on push compiles (push pending per user).

Docs: changelog ADD bullet + app/AGENTS.md v230 entry.

## Parallax question (answered only, intentionally NOT implemented)

Yes it's feasible: Android's hardware sensors (ROTATION_VECTOR / GYROSCOPE)
can feed a parallax offset into the backdrop sampling transform — kyant0's
backdrop supports a `layerBlock` GraphicsLayerScope transform per draw, so the
glass could subtly counter-shift its refraction/lens highlight against device
tilt like iOS liquid glass. Costs: a sensor listener lifecycle, ~1 extra
transform per frame while glass is on screen, and tuning so it doesn't fight
the draggable pill. Deferred at the user's request.
