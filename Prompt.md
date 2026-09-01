# Prompt.md — current request log

## Request: Scrap the scene-heavy signature designs for 14 categories → minimal Editorial-quality treatment + new fonts + picker crash fix

User direction (three follow-ups after the first signature redesign shipped):
1. "for those 7 category redesign why u didnt used ne fonts cool fonts mathing the style, now the next 7 and this time properly research style and design and use new fonts download more maybe" → the first 7 categories only re-paired ALREADY-BUNDLED fonts; the next 7 (Biology, Books, Chemistry, Directors, Discoveries, Economics, Films) must get genuinely NEW downloaded fonts.
2. "when i asked for a redesign you should have removed all of the desings etc and use editorial minimal etc for quality nd redesign without using so many things on the backgroud design" → the signature backgrounds are still too busy; scrap the scene-heavy designs entirely for Editorial/Minimal-level cleanliness.
3. "by everything i ment scarp the current siganture design for the ones i asked entirely, also fix this crash too" (same infinite-height crash trace) → the crash is from the NEW category picker.

### What shipped (this turn)

- **7 new OFL fonts downloaded + licensed** — BioRhyme (Biology), Fraunces
  (Books), Oxanium (Chemistry), Limelight (Directors), Rye (Discoveries),
  Space Grotesk (Economics), Anton (Films). TTFs in `res/font/`,
  `FontFamily` vals in `CurioTypography.kt` (single-entry for Rye/Anton/
  Limelight, variable-weight entries for the rest), licenses in
  `app/third_party/`. All verified as valid TrueType via `file`.
- **Signature scenes SCRAPPED for all 14 categories** (first 7 + second 7)
  in BOTH `signatureDesign` (normal) and `signatureDesignDetailed`
  (Deepen) — `TopicShareCard.kt` lines ~2631–5965. Every branch is now:
  flat vertical gradient + `signatureHairlineFrame` (NEW DrawScope helper:
  inset 4.5% rounded-rect outline, 1f stroke — it was referenced but never
  defined, now added beside `drawStar`) + ONE tiny category crest
  top-right. Deepen differs only by a soft radial accent glow (no extra
  objects). Normal and Deepen configs are now IDENTICAL per category
  (bg/gradient/font/colors/layout/badge — fixed 11 drifting hex/size
  mismatches, incl. FILMS titleSize 34→36).
- **Category-picker crash fix** — `ContinueExploringSection` nested a
  `LazyVerticalGrid` inside a `LazyColumn` item (lazy items are measured
  with infinite max height → crash). Replaced with manual chunked rows
  (3/4 cols, `NewPickerTile`/`AddSuggestionTile` gained a `modifier`
  param, trailing `Spacer(weight(1f))` pads short rows). The earlier
  pager/mix-editor `weight(1f, fill = false)` fixes stay.
- Untouched: `signatureDesignClassic` (kept), Paper/Clean/Collage/
  Editorial/Minimal/Vinyl, all non-target categories' existing scenes,
  web/ + desktop/.

### Progress
- [x] Download 7 new OFL fonts + declare FontFamily vals + licenses.
- [x] Scrap scene backgrounds → minimal hairline treatment, 14 categories, normal + Deepen.
- [x] Define missing `signatureHairlineFrame` helper.
- [x] Align normal == Deepen configs (0 mismatches across font/colors/layout/badge).
- [x] Fix picker crash (nested LazyVerticalGrid → chunked rows).
- [x] Verify classic/base styles untouched (hunk-range check: none in 2034–2630).
- [x] Syntax sanity (braces 0, brackets 0, parens ±1 pre-existing).
- [x] DOX pass (AGENTS.md v3xx5 + v3xx5b, changelog, Prompt.md).
- [ ] Commit & push.
