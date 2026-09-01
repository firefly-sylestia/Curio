# Prompt.md — current request log

## Request (ACTIVE): Share-card hold-to-edit glitches — dead side adjuster, flaky height tab, footer moves

User: "sometimes the height adjuster for text and title doesnt work sometimes
it works when maybe handle issue, and the side adjustment isnt working now it
was working better previously, also dont let the footer move, just the author
year move."

> NOTE: the share-hub / detail-share / moodboard-quote feature from an
> earlier request is PARKED (user: "dont push this but push the cl fix").
> Nothing for it was committed; design answers received (full swap +
> share-as-text everywhere; hub Share button opens the sheet + floating
> button; moodboard quote option in save + share) are recorded in the
> conversation. Resume with TopicShareSheet work when asked.

### What shipped (this turn) — all in `ui/components/TopicShareCard.kt`

**1. Side (width) adjuster fixed — root causes**
- The scale tab clamped fractions to 0.3..1.6 while the renderer caps at
  1.0 — drags past full width did nothing (dead zone). Tab clamps now match
  the renderer (0.3..1f).
- Delta divided by the box's OWN reported width, which re-measured mid-drag
  and changed the feel; now card-relative (`dx / cw`, `dy / ch`) so drags
  track the finger 1:1 across the card.
- Paper's fact sits in `FrostPane`, whose internal `fillMaxWidth()` was
  chained AFTER the resize modifier — it overrode the resized width entirely,
  so the width adjuster did nothing on the MAIN style. FrostPane now applies
  `Modifier.fillMaxWidth().then(modifier)` (caller's fraction narrows it).

**2. Height adjuster fixed — root causes**
- The bottom tab was only 6dp thick (~20px) — fingers missed it (the
  "sometimes works" flakiness). Both tabs are now big luminous bars
  (14 x 46 / 46 x 14 dp) with luminance-based contrast rings.
- `lines()` FLOORED line counts: dragging 2.5 lines showed 2, so partial
  drags looked dead. Now rounds to the nearest line — each half-line of
  travel changes the visible count.

**3. Footer is now FIXED — only author/year moves**
- Every footer / colophon / credit row across all 9 styles used to ALSO call
  `onMeta`, and since they render after the author/year row they overwrote
  `metaRect` — the M handle anchored to and dragged the footer. All footer
  rows now render without `moveMeta`/`onMeta`; only the byline / author /
  year rows report bounds, so M moves exactly those and the "via Curio"
  footer never budges.

**4. Handles can never leave the card**
- Resize tabs clamp their DISPLAY position ~6dp inside the card (a grown box
  can't push a tab off-screen); the M handle clamps inside too.
- M-handle drag clamps got a floor on the upper bound (full-width meta rows
  made `coerceIn` throw mid-drag) — now safe in both axes.

### Progress
- [x] FrostPane modifier order fix (Paper width).
- [x] Tab clamp ranges unified with renderer (side dead zone gone).
- [x] Tab sizes + contrast rings (grabbable).
- [x] `lines()` rounds instead of flooring (height visibly responds).
- [x] Footers/colophons/credits no longer report onMeta (M = author/year only).
- [x] Tab/handle display clamps + safe M drag bounds.
- [x] Balance verified (0/0/0 vs HEAD), changelog bullet added.
- [x] Commit; CI validates compile on push (this env forbids Gradle).