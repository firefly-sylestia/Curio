# Prompt.md — current request log

## Request (ACTIVE): Hold-to-edit indicators still wrong; height resizes don't extend text

User (repeat + more detail): "the tap to hold to edit in glitched the
indicators are showing wrong and not properly also the height isnt even
changing the text box height as the texts are not extending rather they
are moving up and down. also when they gets out of the screen theres no
way to fix it without reset, also use proper colors for them."

### Real root causes found in TopicShareCard.kt

1. **Indicators in the wrong place**: the overlay drew its boxes at FIXED
   card fractions (title at 0.08w/0.06h, fact at 0.62h, M at 0.88w/0.90h).
   Only Paper roughly matched; Vinyl (body y≈214dp), Neumorphic
   (bottom-anchored), Collage (title top-left) etc. never aligned → "showing
   wrong and not properly".
2. **Height didn't extend text**: `moveFact`/`moveTitle` applied height as
   `fillMaxHeight(frac.coerceIn(0.2f, 1f)).clipToBounds()` — growth was
   forbidden (cap 1f), and fillMaxHeight re-measures the parent Column
   which re-arranges children → the visible text physically jumped up/down
   instead of the box growing.
3. **Off-screen loss**: no real way to pull a handle back had it been
   dragged out (previous turn added clamps; the fundamental misalignment
   remained).

### Fixes (all in TopicShareCard.kt)

- **Bounds-driven overlay**: new `EditBoundsCallbacks` (onTitle/onFact/
  onMeta) — every card style attaches `onGloballyPositioned` right where
  its moveTitle/moveFact/moveMeta modifier lands and reports
  `boundsInWindow()`. `ArrangeableCard` keeps a bounds hub (card origin +
  3 rects) and the edit overlay draws the outline boxes, T/F/M handles,
  edge tabs and the typing field EXACTLY on the reported rects — every
  style, live. Since bounds already include the move offsets, dragging a
  handle moves both the card text and its indicator with zero desync.
- **Height now really extends text**: removed the fillMaxHeight/clip from
  moveTitle/moveFact; added `lines(base, frac, max)` helper and scaled
  every title/body `maxLines` (~20 sites) by titleHeightFrac/factHeightFrac
  (0.35–2.5 → fewer or more lines show, text stays anchored).
- **Resize tabs**: RIGHT/BOTTOM delta denominators use the box's CURRENT
  reported bounds (width/height dp), so drag feel is scale-proportional;
  fracs clamp 0.3–1.6 (width) / 0.35–2.5 (height); offsets clamp so boxes
  and handles can never leave the card.
- **M handle**: anchors to the meta row's real bounds (footer/colophon for
  most styles, byline where no footer) and stays inside the card.
- Colors: contrast-aware handle ink/ring (kept from prior pass).

### Progress
- [x] Helpers (EditBoundsCallbacks class, lines()), moveTitle/moveFact
      height-clip removed.
- [x] TopicShareCard + 9 style signatures thread `callbacks`; attaches at
      every move anchor (36 sites); maxLines scaled everywhere.
- [x] ArrangeableCard bounds hub + bounds-based overlay (title/fact/meta
      zones, typing field over the real fact).
- [x] Balance verified (parens +15/+15, braces +8/+8, brackets 0/0).
- [x] Changelog bullet updated.
- [ ] Prompt.md (this file).
- [ ] Commit & push (CI validates compile on push).

### Verification status
CI validates compilation on push (this environment forbids Gradle builds) —
watch the run. Code-balance vs HEAD verified clean; zero bare
moveFact/moveTitle/moveMeta sites left.