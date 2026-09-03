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
- Docs: changelog (REMOVE Deepen, ADD Adjust, FIX mix strip), app/AGENTS.md
  (v324 bullet incl. redesign contract + strip fix), this file.

## Verification

- `git diff --check` passes; 0 deepen refs; 4/4 call sites carry the new
  params; contrast glyph in the icons font; splice seam read back clean.
- No Gradle commands run (project DOX forbids them here) — CI validates.

## NEXT — Books signature design (awaiting the user's description)

The user picked **Books** as the first category to redesign. Awaiting their
design description. Contract: implement ONE category, commit, then ask for
the next. No SVG without permission — icons/symbols/existing art only.