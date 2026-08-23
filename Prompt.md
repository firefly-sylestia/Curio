# Prompt.md — current request log

## Request (complete): v237 — un-smudge the indicator + kill the perfect-circle ring

User reported (pointing at 2569b76 as the last-good state): (1) a perfect-circle
artifact still visible in Home pills, Pet Designer and Topic Reveal; (2) the active
indicator AND touch blob became blurrier; (3) active indicator text smudged;
(4) light-mode text/icon still not dark enough.

### Root causes (both mine, from v235)
1. **Smudge**: I added an always-on `blur(4/8dp)` to the draggable indicator's
   backdrop sample. At rest that sample is EXACTLY aligned under the real tab
   content — drawing it raw is invisible; blurring it created the accent halo
   that smudged label + indicator. REVERTED: press optics are press-scaled only.
2. **Perfect circle**: fixed-height lens refraction bands (24/14dp) wrapped small
   ROUND capsules entirely and folded a ring into their center. Now size-capped:
   `rh = minOf(baseHeight, size.minDimension * 0.16f)` in `liquidGlassCapsule` and
   the tab bar's main capsule. Small pills get a thin edge bend; tall surfaces
   keep the full refraction.
3. Ink darker still: light-mode glass active ink is now
   `fromHsl(hsl.h, s>=0.50, min(l*0.42, 0.24))` in both CurioBottomNav and the
   Pet Designer glass tab branch.

Files: CurioLiquidGlassTabBar.kt, LiquidGlassPills.kt, CurioBottomNav.kt,
PetDesignerScreen.kt + changelog + app/AGENTS.md (v237). Balance checks OK ×4.
CI validates compilation on push.

Lesson recorded: on this glass stack, NEVER blur a sample that sits pixel-aligned
under real content, and never use fixed-px refraction bands without capping to
pill size.
