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

Also carried over from the previous message: CI failed on
`RadialHoldMenu.kt` (positionInRoot unresolved, launch deprecation,
RenderEffect type mismatch) and a reported "solid strip behind the Cancel
button during mix" in the classic picker page — the strip question is still
OPEN (asked at the end of this turn).

## Completed

- **CI fix (`RadialHoldMenu.kt`)** — three compile errors fixed: added the
  `androidx.compose.ui.input.pointer.positionInRoot` import; the long-press
  `launch` now resolves against the pointerInput CoroutineScope
  (`this@pointerInput.launch`); `buildGooRenderEffect()` now builds a
  `androidx.compose.ui.graphics.RenderEffect` (createChainEffect /
  createBlurEffect / createColorFilterEffect + ColorFilter.colorMatrix)
  instead of the android.graphics one the GraphicsLayer no longer accepts.
- **Deepen removed** — `ExperimentsScreen` "Deepen signature card elements"
  toggle row deleted; `AppPreferences` KEY + state + get/set deleted; the
  `signatureDesignDetailed` branch in `SignatureCard` deleted; the whole
  ~1325-line `signatureDesignDetailed` function spliced out (verified 0
  references). Signature cards now pick classic vs `signatureDesign` only.
- **Adjust tool** — new `CurioIcons.Contrast` tool pill in the editor with a
  panel of two `Slider` rows (Saturation, Contrast; 0.5–1.5, neutral 1.0,
  % readout). `TopicShareCard` gained `saturation`/`contrast` params that
  thread a single `graphicsLayer { colorFilter =
  ColorFilter.colorMatrix(adjustColorMatrix(...)) }` (luma-weighted
  saturation × pivot-0.5 contrast, `ColorMatrix.timesAssign`) into the card
  modifier chain — preview and export (real View.draw pass) both adjust.
  Reset-all also clears the sliders. 4 call sites wired (2 previews +
  Save + Share).
- Docs: changelog (REMOVE Deepen + ADD Adjust), app/AGENTS.md (v324 bullet
  incl. the signature-redesign contract), this file.

## Verification

- `git diff --check` passes; 0 references to deepen APIs; 4/4 call sites
  carry the new params; `contrast` glyph present in the icons font; splice
  seam read back clean.
- No Gradle commands run (project DOX forbids them here) — CI validates.

## Open questions (asked at the end of this turn)

1. The solid strip behind the Cancel button during mix (classic picker
   page) — which element is it? (Options: the solid "Mix · N" capsule in
   the bottom row under the floating Cancel pill / the floating Cancel pill
   itself / the old picker's Mix button beside Cancel / other.)
2. Which signature category to redesign FIRST (one at a time, per the
   contract).