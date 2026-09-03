# Prompt.md — current request log

## Request: Signature card redesign campaign + Deepen removal + Adjust tool

User direction (v324):

> Now we will properly design the signature category styles: remove the
> Deepen option — keep only default and classic as they are now. Instead of
> Deepen we will use a saturation/contrast editor that adjusts the card, in
> the tools. Then we will edit one-by-one each category's signature design —
> the user describes the design per category. Rules: NO drawing/SVG without
> permission; use icons/symbols and any already-ready drawings (icon font
> glyphs, existing card art) as background symbols/icons. Ask ONE category
> at a time — finish it, commit, then ask for the next.

## Completed

- **v327 — one big batch (user: \"fix these and push everything\"):**
  1. **Book covers visible again** — `BookCoverPoster` restored to the
     ce892baa form the user confirmed works: the `AsyncImage` IS the root
     (no gradient Box, no `matchParentSize`); callers' size/shadow
     modifier is applied directly to the image. The gradient-wrapper
     experiments (7ba3d7d2…) painted over / swallowed the cover — gone.
  2. **Star rating VIEW added** — `BookCoverFetch.fetchRatingFor(name,
     author)` (keyless Google Books `intitle:` query → first
     `averageRating`); the reveal hero (BOOKS only) shows a ★ chip next to
     the author pill, fetching on demand and caching via
     `AppPreferences.setBookRating`. The rating fetch previously had no
     visible view.
  3. **Pet outfit unequip** — tapping an EQUIPPED outfit removes it
     (`setEquippedOutfit(null)`); pill reads \"Equipped · tap to remove\".
  4. **Editor floating cluster** — while editing, the Edit-text circle
     (quick fact selected) and the Reset circle float NEXT to the Done
     button (BottomEnd Row) instead of hiding mid-scroll in the tool row;
     the tool row no longer carries Edit-text/Reset/Done.
  5. **Collage tone** — `CollageCard` colors now derive from the picked
     `ShareCardPalette` (paper/field/band/ink), so the Tone tool
     customizes Collage too.
  6. **Radial menu fixes** — discs 46→36dp (were \"giant balls\"); on
     release the ring LINGERS 420ms then fades out 260ms (`dismissAlpha`
     on the overlay root) instead of vanishing instantly; goo blobs now
     animate to overlay-LOCAL ring positions (`centerPx` conversion) —
     they were morphing from local `cp` to ROOT `p`, landing offset from
     the crisp discs and breaking the circular opening.
- **CI fixes (`RadialHoldMenu.kt`)** — v325: `positionInRoot` import +
  scope-scoped launch + Compose RenderEffect wrapper; then the `.value`
  unwrap bug (`rememberUpdatedState` + `by` delegate); then the full
  re-port to Compose BOM 2026.05 (`positionInRoot()` removed, restricted
  gesture scope, RenderEffect factories changed): hold timer on a
  `rememberCoroutineScope`, root coords via `onGloballyPositioned`, goo
  merge via `Modifier.blur(18.dp)`.
- **Deepen removed** — Experiments toggle, pref API, `SignatureCard` branch
  and the ~1325-line `signatureDesignDetailed` all deleted (0 references).
- **Adjust tool** — Contrast tool pill + Saturation/Contrast sliders
  (0.5–1.5), threaded through `TopicShareCard` as a single `graphicsLayer`
  color filter (preview + export both adjust); Reset clears them.
- **Mix strip fix** — the floating Apply \"Mix · N\" pill restored in
  `ClassicPickerPage` next to Cancel; the solid bottom-row capsule hidden
  while mixing on the classic page.
- **v325 — share-card editor safety net.** Back cancels the editor first,
  then exits the sheet; edits persist across accidental dismissals
  (`persistEdits()` on Save/Share + dismiss) and clear on leaving the
  topic reveal screen; Edit-text circle next to the floating Done button;
  move knob shrinks/fades while dragging.
- Docs: changelog (v327/v326/v325 entries), app/AGENTS.md (v327 bullet +
  v326 Books bullet), this file.

## Verification

- `git diff --check` passes; grep-verified: 0 deepen refs, poster restored
  to ce892baa form, star chip + fetch wired (BOOKS only), unequip path in
  `buy()`, floating cluster + palette-derived collage colors,
  radial menu constants/dismissAlpha/centerPx conversions.
- No Gradle commands run (project DOX forbids them here) — CI validates.

## NEXT — Books committed locally (ad4067eb), PUSHED together with v327;
then the category after Books (awaiting the user's pick + design)

**BOOKS IS DONE** (v326). User direction: \"research the book palette
yourself, and the design should be like a book cover with margins and
ruled lines, facts properly adjusted with lines.\" Implemented in
`signatureDesign` BOOKS entry: classic cloth hardcover — oxblood leather
gradient, gold-foil double border (margins), left spine band + gold hinge
rules, real `menu_book` icon crest top-right (new optional `crest`/
`crestTint` fields — no more hand-drawn paths), faint `auto_stories`
watermark bottom-right, and gold ruled lines BEHIND the quick fact at the
body's own line-height (`bodyRuleColor` → `drawBehind`), so the facts sit
exactly on the lines.

Contract: implement ONE category, COMMIT but DO NOT PUSH (user reviews
each before the next). No SVG without permission — icons/symbols/existing
art only.