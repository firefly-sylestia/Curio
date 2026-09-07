# Prompt Log — current request

## Request (2026-09-07, active → v384 committing)

Share-card editor refinement — the smart/auto-layout (sparkle) pill.

**What the user asked:**
1. The sparkle ("smart") should ALSO fit/fix the **info rows (author/year)**
   when manual edits leave them overlapped — not just title + fact.
2. They believed commit `05b501c` (PR #94) had MORE smart-layout
   arrangements and a merge decreased them; restore it. (Checked: that
   merge only changed an import + a comment; the pill had always cycled 4
   arrangements. User answered: **add MORE arrangements**.)
3. Title/quick-fact overlapping still behaved "magnetic" (auto-fixed) in
   manual edit. Wanted: **let it overlap when done manually; only fix on
   sparkle tap** (user confirmed: turn off BOTH the drag collision-push AND
   the slider auto-lift).

**Changes (all in `TopicShareCard.kt`, versioned v384):**
- `ShareAutoLayoutPlan` gained `metaLift` (info-row repair) + `factDropCap`.
- `autoLayoutPlan` now repairs THREE overlaps from drag offsets — lift the
  title (≤72dp), push the fact down (≤72dp), push the info rows down
  (≤72dp) — and cycles **7 arrangements** (fit → condensed → book columns →
  editorial drop cap → maximal space → tall 9:16 → standard) via
  `attempt % 7`.
- `runAutoLayout`: applies `metaDy += metaLift`, `factDropCap`, gates on
  `dropCapChanges`/`metaLiftChanges`, lookahead widened to `repeat(12)`.
- Removed the slider **auto-lift** `LaunchedEffect` and the
  **collision-push** in the fact drag (free manual overlap — only the
  sparkle repairs). Dropped now-dead `titleGrabbed`/`factGrabbed`/`touches`.
- Changelog updated (`fastlane/.../changelogs/20260921.txt`).

**Out of scope / open:** web/desktop untouched (scope: Android only). The
meta repair is offset-estimated like the existing title/fact repair (does
not cover a grown fact box extending down over the info rows on
bottom-anchored styles — that would need measured bounds; noted for a
follow-up if the user hits it).