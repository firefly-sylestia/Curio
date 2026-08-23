## v250: press ghost fix + iOS tab glide

User: touching the blob showed DUPLICATE text/icons over it (also Pet
Designer); tab switches snapped instead of gliding.

1. Ghost fix: the pill's sample went back to PAGE-ONLY. The v246 combined
   sample (page + hidden tab-row copy) re-introduced blurred ghost labels
   under the v247 crisp overlay whenever the fill faded on press - a double
   image, worse in classic mode. With the overlay guaranteeing visible ink,
   the sample no longer needs the tab row at all. rememberCombinedBackdrop
   import removed; hidden row now unsampled (harmless).
2. Glide: DampedDragAnimation.animateToValue gains an optional AnimationSpec;
   tab bar passes spring(0.82, 380) for tap switches and drag release -
   ~350ms iOS-style glide with gentle settle instead of the default 1000-
   stiffness snap.

Balance-checked both files; CI validates.

## v249: classic active indicator experiment

User asked for the previous liquid-glass style active indicator (transparent,
always-refracting, pre-v247) as an Experiments option. Added
`glassClassicIndicatorState` (default OFF = current solid white/black pill):
state + key + is/set + load in AppPreferences, an Experiments switch row, and
a branch in CurioLiquidGlassTabBar's pill recipe (always-on blur + 24dp lens +
full highlight + press-gated-only shadow + fully transparent surface when ON;
solid fill and gentle press-glass when OFF). Crisp ink overlay stays in both
modes. Balance-checked; CI validates.

## Addendum (v248): mood-board quote slip still max-sized

User: the quote card is ALWAYS at the max — not fixed by the spare-line pass.
Root cause: the floating card's Box forced `.width(renderW)` and NotePaperCard
did `fillMaxWidth()`, so every slip stretched to the full slot/resize width no
matter how short the quote. Fix: Box now `widthIn(max = renderW)` (slot width
or user resize = MAXIMUM, not fixed) and NotePaperCard wraps content with a
96dp floor for tappability. Height already wrapped; drag/resize mechanics
unchanged. File: MoodBoardZoom.kt (+widthIn import).

## Request: v247 - solid idle blob, gentle press refraction, Apple press feel (COMPLETE, pushed)

User (after v246 build): home active indicator is good, but (1) its refraction
is too high, (2) make the IDLE active pill SOLID white (light) / black (dark)
instead of transparent/reflective - while keeping the text - and restore the
blob functionality from commit d442219, (3) better touch interaction with
proper Apple-like animation.

Also fixed the v246 CI failures first (pushed 17c693b): broken `import import`
line in LiquidGlassPills.kt, IntOffset imported from the wrong package
(geometry -> unit), and a duplicate `modifier =` argument on Cabinet's grid.

## v247 implementation

1. Solid idle blob (CurioLiquidGlassTabBar.kt): onDrawSurface draws White/Black
   at alpha 1f - pressProgress; quiet resting shadow lifts the solid pill.
2. Refraction tamed: indicator-only press-gated recipe from d442219 -
   blur*xProgress, lens(10dp*p, 14dp*p, adaptive), highlight on press. Bar
   capsule keeps its always-on recipe.
3. Crisp ink overlay: third tab-row copy renders ABOVE the solid pill via new
   LocalLiquidGlassTabOverlay; items strip clickables there so touches fall
   through to the real tabs and the blob drag handlers below.
4. Apple-style press feel (LiquidGlassPills.kt): asymmetric spring - fast
   crisp press-in (stiffness 900 / damping 0.85), soft underdamped release
   (380 / 0.55) with one gentle overshoot.

Verified: balance-checked both files; CI validates compilation.

# Prompt.md — current request log

## Request: v246 — chip-bar glass, blob visibility, press feel, icon centering (COMPLETE, pushed)

User's batched asks across the session:

1. **Floating category pill in Cabinet + Topic Database → liquid glass**, with
   **one theme-only ink** (no per-category colors).
2. **Active tab icon + label vanished under the indicator** — only where the
   blob sat there was no icon/text.
3. **Touch press effect on capsules returned** — pill shrinks toward its
   middle while held + refraction blooms at the corners (from previous
   commits).
4. **Icon centering** in the search / back / home drawer-menu / avatar pills —
   still off regardless of font size.
5. **Moodboard quotes**: don't remove them; height grows with the text and
   keeps only one extra line of space (was stretching fully by height).

## What shipped

1. **Cabinet + Topic Database sticky chip bars are liquid glass now**
   (`CabinetScreen.kt`, `TopicDatabaseScreen.kt`). Each screen's scrolling
   list records a LOCAL `LayerBackdrop`; the chips are sibling overlays that
   sample it with `liquidGlassCapsule(alwaysClear = true)` — the same
   crash-safe architecture as every other in-screen pill. Labels use ONE
   theme ink: `Color.White` in dark, `Color.Black` in light, no per-category
   colors. Fixed two missing commas my interrupted script left behind.

2. **Active-tab content visible under the blob again**
   (`CurioLiquidGlassTabBar.kt`). Root cause of the vanish: v244 pointed the
   indicator's `drawBackdrop` at the page-only capture, which paints blurred
   page OVER the visible tab row sitting beneath it in z-order. Fix: restore
   the combined sample `rememberCombinedBackdrop(page, tabsBackdrop)` but
   make the hidden tab-row copy UNTINTED (removed its accent ColorFilter) —
   so icons/labels refract through the pill while the ink stays pure
   black/white (the old category-color ghost came from the tint, not from
   sampling).

3. **Press feel on floating capsules** (`LiquidGlassPills.kt`).
   `liquidGlassCapsule` gains an optional `interactionSource`: a spring
   Animatable drives (a) ~4% shrink toward the middle while held via
   `graphicsLayer`, and (b) lens refraction deepening ×(1 + 0.45·press).
   Wired on Home menu + avatar pills (`TopBarPill` new `pillInteraction`
   param), Profile back + search pills (`CurioBackButton` +
   `ProfileSearchPill` new params), Detail back + more pills. Call sites
   hoist one `MutableInteractionSource` shared by click + capsule.

4. **Measured icon centering** (`CurioIcons.kt`). `CurioIcon` now reads the
   glyph's real ink bounds from the text layout (`getBoundingBox`) and
   offsets by the delta between line-box center and ink center — every glyph
   self-centers at any font scale. Removed ALL `curioGlyphInkNudge` call
   sites (HomeScreen ×4, ProfileScreen ×2, SpinScreen ×2, CurioTopBar ×1)
   since they would double-correct; helper kept defined.

5. **Moodboard quote slip** (`MoodBoardZoom.kt`): keeps wrap-to-text height
   (two preview lines max) and adds ONE spare ruled line below the last text
   line (bottom padding 8→24dp). No fixed tall box, no full-board stretch.

## Verification

- Balance-checked all touched files (braces/parens green).
- CI validates compilation on push.
- Follow-ups to watch: chip-bar legibility over busy content; blob sample
  alignment during fast drags.

## Notes

- The moodboard quote REMOVAL request was superseded by this fix per user.
- `web/package-lock.json` user change untouched and uncommitted.
