# Prompt.md — current request log

## Request (complete): v241 in-screen glass + tilt circle fix — COMMITTED & PUSHED

User asked (scoped tightly): re-add in-screen glass for the floating pills and the
Pet Designer bar exactly like the current home nav capsule architecture, keep
everything else as-is, fix the tilt-parallax "perfect circle", do NOT touch the
bottom nav pill. Floating pills (menu / avatar / detail / profile) must read as
refracting, fully CLEAR glass.

### What shipped

1. **Toggle** — Settings → Experiments → "In-screen glass" (`glassInScreenState`,
   default OFF, requires Liquid glass pills). Self-heal disables it too after a
   native crash. `isInScreenGlassActive()` gates everything.
2. **Safe architecture everywhere** (sibling overlay, never self-capture):
   - Home: capture Box wraps page content (`homeGlassBackdrop`); menu/profile
     pills are siblings; `glassOn` re-enabled via local backdrop.
   - Entry Detail: `.layerBackdrop(detailGlassBackdrop)` on the scroll Column;
     back/more pills are siblings of the Column (no wrapper Box needed).
   - Profile: capture on the LazyColumn (hero + list); back/search pills are
     siblings; `ProfileSearchPill` grew a `modifier` param.
   - Pet Designer: capture on the LazyColumn; `PetStudioBottomNav` sits below it,
     takes `glassBackdrop`/`glassOn`; glass branch swaps Surface to Transparent +
     0 elevation with a stadium-shaped capsule.
3. **liquidGlassCapsule extensions**: `backdrop: LayerBackdrop?`, `alwaysClear`,
   `shape: Shape` (CircleShape default; RoundedCornerShape(50) for wide bars so
   they don't ellipse-clip). In-screen pills use alwaysClear + constant 0.45f wash
   (no per-frame effects rebuild).
4. **Tilt circle fix**: `drawGlassTiltEdgeGlow` redrawn as a ~110° TOP-rim light
   arc sliding against tiltX, fading with tilt magnitude. Level phone = nothing
   drawn; never a full ring.

### Verification

- Delimiter-balance check green on all 7 touched Kotlin files.
- Import hygiene checked (liquidGlassCapsule import was missing in
  PetDesignerScreen — caught and added before commit).
- Bottom nav pill capsule untouched (no edits to its render path).

### Follow-up notes

- CI validates compilation (no local Gradle per environment rules).
- If the user later wants the classic frost look instead of clear glass on any
  site, drop that site's `alwaysClear = true`.
