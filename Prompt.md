# Prompt.md — current request log

## Request (complete): glass polish batch (v233) — committed & pushed

User asked four things on top of the vFlow glass nav:

1. **Light-mode active-indicator text hard to read** → the draggable indicator's
   constant 0.14 accent wash was too faint a bed on bright pages. Now theme-aware:
   light 0.30 / dark 0.16 (hoisted `isCurioDarkTheme()` in CurioLiquidGlassTabBar).
2. **Pill too frosty — make it glassy like the touch blob, as a separate option** →
   new pref `glassClarityState` (`glass_clear_style`) + Experiments row "Clear glass".
   When ON: blur 8dp→2dp and container wash cut to ~35% in `liquidGlassCapsule` and
   all three tab-bar layers (main capsule, hidden tinted copy, active pill).
3. **Parallax tilt doesn't work — the EDGE GLOW should move, not the glass** →
   v231's whole-capsule translation removed. New `drawGlassTiltEdgeGlow()`
   (LiquidGlassPills.kt): white rim stroke with a radial gradient whose bright spot
   slides against `CurioGlassParallax.tiltX/Y`. Applied in `liquidGlassCapsule`'s
   capture-guard drawWithContent + on the tab bar's main capsule and active pill.
   Reads tilt snapshot state inside draw → per-tick draw invalidation, no recomposition.
4. **Menu/profile avatar glyph off-center on some phones (and elsewhere)** → fixed-dp
   nudges only center at fontScale 1.0 (CurioIcon shrinks glyphs below 1.0 but the
   nudge stayed). New `Modifier.curioGlyphInkNudge(dp)` scales by
   `fontScale.coerceAtMost(1f)`; replaced at all 9 call sites (Home menu/profile pill,
   Home casino/stat chips ×3, Profile rows ×2, Spin die/glyphs ×2, CurioTopBar).

Files: AppPreferences.kt, LiquidGlassPills.kt, CurioLiquidGlassTabBar.kt,
ExperimentsScreen.kt, CurioIcons.kt, HomeScreen.kt, ProfileScreen.kt,
SpinScreen.kt, CurioTopBar.kt + changelog 20260920.txt + app/AGENTS.md (v233).

Verification: balance check OK on all 9 Kotlin files; no stale `offset(y = (-…))`
nudges remain; unused graphicsLayer import removed from LiquidGlassPills.kt.
Note: str_replace flaked twice on HomeScreen/ProfileScreen ("file does not exist" /
"not found" on plain-ASCII strings that verifiably existed) — fell back to the
session's python-patch pattern and verified with grep afterward.

Follow-ups / notes:
- The scroll-morph glass on Home/detail top-bar pills stays OFF (v232 crash class);
  clear-glass + edge glow apply wherever liquid glass is actually live today.
- web/package-lock.json has a pre-existing local modification — left untouched.
