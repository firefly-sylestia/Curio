# Prompt.md — current request log

## Request (complete): Pet Designer crash — RenderThread stack overflow (v228)

User reported Pet Designer crashing on open with a native crash: SIGSEGV
(SEGV_ACCERR) on RenderThread, "stack pointer is not in a rw map; likely due to
stack overflow", 512 frames alternating between
`RenderNode::prepareTreeImpl` and its child-traversal lambda — an infinitely
deep render-node tree.

### Root cause

The v227 liquid-glass experiment: `Modifier.layerBackdrop(navGlassBackdrop)`
on the NavHost's page-content Box records that subtree into a GraphicsLayer —
the library draws the content FIRST, then re-invokes the draw chain inside
`recordLayer`. Glass pills living INSIDE the captured subtree (Pet Designer
studio bar, Topic Reveal category/favorite bar) therefore drew a second time
during the record pass and sampled `navGlassBackdrop` itself — drawing the
GraphicsLayer into its own recording = cyclic render node → HWUI recursed in
`prepareTreeImpl` until the RenderThread stack overflowed. The bottom tab bar
was immune (sibling overlay of the captured Box).

### Fix

- `LiquidGlassPills.kt`: added `CurioGlassPills.isCapturingBackdrop`
  (@Volatile, UI-thread-only draw-phase flag); new `curioGlassCaptureDraw()`
  ContentDrawScope extension sets it around `drawContent()`.
- `CurioNavHost.kt`: `rememberLayerBackdrop(onDraw = { curioGlassCaptureDraw() })`.
- `liquidGlassCapsule`: outer `drawWithContent` guard — during a capture pass
  it skips the backdrop node entirely and paints a plain translucent rounded
  capsule instead. Both in-screen pill sites are fixed via the shared helper.

Verification: delimiter balance OK; Gradle is forbidden here so CI on push is
the compile source of truth.

Docs: store changelog 20260920.txt FIX bullet + app/AGENTS.md v228 entry.
