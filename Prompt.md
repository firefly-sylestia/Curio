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

- **CI fix (`RadialHoldMenu.kt`)** — `positionInRoot` import, launch scoped
  to the pointerInput CoroutineScope, Compose `RenderEffect` wrapper.
- **Deepen removed** — Experiments toggle, pref API, `SignatureCard` branch
  and the ~1325-line `signatureDesignDetailed` all deleted (0 references).
- **Adjust tool** — Contrast tool pill + Saturation/Contrast sliders
  (0.5–1.5), threaded through `TopicShareCard` as a single `graphicsLayer`
  color filter (preview + export both adjust); Reset clears them.
- **Mix strip fix** (user: "the cancel pill sits on a solid strip — the
  strip draws on top of the mix button and the old mix button place which
  we removed"): the floating Apply "Mix · N" pill is restored in
  `ClassicPickerPage` next to the floating Cancel pill
  (`Arrangement.spacedBy(8.dp, Alignment.End)`), and the shared bottom
  row's solid "Mix · N" capsule is hidden while mixing on the CLASSIC page
  (`!mixing || pagerState.currentPage != 0`) — it stays on the new page.
- **v325 — share-card editor safety net (user report).** (1) Back cancels
  the Customise editor first, then (second press) exits the sheet
  (`BackHandler(enabled = editMode)` in `TopicShareSheet`). (2) Edits are
  now PERSISTENT across sheet dismissals — `persistEdits()` saves the
  current move/text/scale state on Save/Share AND on dismissal, so an
  accidental exit resumes where you left off; leaving the TOPIC REVEAL
  screen clears the topic's edits (`clearShareCardEdits` in a
  `DisposableEffect(floatingTopic)` in `TopicRevealScreen`). (3) The
  "Edit text" circle moved NEXT to the floating Done button (shown when
  the quick fact is selected). (4) The move knob shrinks and fades while
  dragging so it no longer covers the text. (5) **RadialHoldMenu re-ported
  for Compose BOM 2026.05** (second CI failure — this generation removed
  `PointerInputChange.positionInRoot()` and the gesture scope is
  `@RestrictsSuspension`): hold timer runs on a `rememberCoroutineScope`
  coroutine; root coords = `change.position` + `LayoutCoordinates.
  positionInRoot()` via `onGloballyPositioned`/`rememberUpdatedState`;
  the goo merge is now `Modifier.blur(18.dp)` (no more android.graphics
  RenderEffect chain).
- Docs: changelog (v325 FIXes + radial-menu compile fix), app/AGENTS.md
  (v325 bullet), this file.

## Verification

- `git diff --check` passes; 0 deepen refs; 4/4 call sites carry the new
  params; contrast glyph in the icons font; splice seam read back clean;
  RadialHoldMenu grep clean of `positionInRoot`(pointer) / `RenderEffect`
  / restricted-scope `launch`; persistence + clear hooks verified by grep
  (Save, Share, dismiss, reveal-exit).
- No Gradle commands run (project DOX forbids them here) — CI validates.

## NEXT — the category after Books (awaiting the user's pick + design)

**BOOKS IS DONE** (v326). User direction: "research the book palette
yourself, and the design should be like a book cover with margins and
ruled lines, facts properly adjusted with lines." Implemented in
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