# Prompt.md — current request log

## Request (ACTIVE): Fix glitched hold-to-edit indicators on the share card

User: "the tap to hold to edit in glitched the indicators are showing
wrong and not properly also when they gets out of the screen theres no way
to fix it without reset, also use proper colors for them"

Three concrete bugs in the `ArrangeableCard` inline-edit overlay
(app/src/main/java/com/curio/app/ui/components/TopicShareCard.kt):

1. **M handle desync** — the handle's x was fixed at `0.88cw` and only y
   tracked `metaDy`; dragging M horizontally moved the info rows but the
   handle stayed put ("indicators showing wrong").
2. **Unbounded drags** — T/F/M offsets and resize fracs had no bounds, so
   boxes/handles could be pushed off the card and were unrecoverable
   without Reset. Drags were also unclamped at the card edges.
3. **Colors** — handle labels were hard-coded white (invisible on light
   accents) and the edge tabs hung off the box corners instead of
   centering on the edges; crop outlines were faint (0.45 alpha).

### Fixes (TopicShareCard.kt)

- **Clamp every drag to the card**: T and F offsets clamp so the box stays
  fully on-card (`coerceIn` on the Dx/Dy ranges computed from current
  box size); resize fracs cap so the RIGHT/BOTTOM edge never leaves the
  card (`maxW`/`maxH` derived from the live offset). Handles can no
  longer be lost off-screen.
- **M handle tracks both axes**: x = `0.88cw + metaDx`, y = `0.90ch +
  metaDy`, clamped so the handle itself stays visible (18dp pad).
- **Contrast-aware ink**: `MoveHandle` computes `accent.luminance()`; light
  accents get dark ink (`0xFF1B1B1F`) + a soft black ring, dark accents
  keep white ink + a white ring — letters always read against the card.
- **Edge tabs centered**: `ResizeEdge` offsets by `y - h/2` too, and RIGHT
  tabs now sit at the vertical middle of the box edge (`+ titleH/2` /
  `+ factH/2`).
- **Crop outlines stronger**: 0.45 → 0.60 alpha for the title + fact
  boxes so the region reads clearly.

### Progress
- [x] Imports: `androidx.compose.ui.graphics.luminance`.
- [x] T/F/M drag clamping + M handle dual-axis tracking.
- [x] Resize caps (width + height, title + fact).
- [x] MoveHandle contrast ink + ring; ResizeEdge centering; outline alphas.
- [x] Balance verified (code-balance delta 0/0/0 vs HEAD).
- [x] Changelog FIX bullet added.
- [ ] Prompt.md completion summary (this file).
- [ ] Commit & push (CI validates compile on push).

### Verification status
CI validates compilation on push (this environment forbids Gradle builds) —
watch the run after pushing. Code-balance check vs HEAD passed (parens
+30/+30, braces 0/0, brackets 0/0).