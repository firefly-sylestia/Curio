# Prompt.md — current request log

## Request (complete): in-screen liquid glass restored behind its own toggle (v234) — committed & pushed

User asked: what else can we do with liquid glass, and can we bring it back to the
Pet Designer studio bar + floating top-bar pills — properly implemented, as a
SEPARATE toggle.

### Root-cause insight that makes it safe this time
The v228/v232 crashes were not mysterious: in-screen pills sampled the NavHost's
WHOLE-PAGE capture, and that capture's record pass re-draws the page INCLUDING the
pills — the pill drew the sample layer into the layer being recorded → cyclic
render node → RenderThread SIGSEGV. The bottom bar never crashed because it is a
SIBLING OVERLAY of the captured Box.

The proper fix: give each in-screen site its OWN local `rememberLayerBackdrop()`
over a wrapper Box containing only what sits BEHIND the pill, pill outside the
subtree. Self-capture becomes impossible by construction; the global-capture v228
guard stays as belt-and-braces.

### What shipped
1. New pref `glassInScreenState` (`glass_in_screen`) + Experiments row
   "In-screen glass" (OFF) + `isInScreenGlassActive()` gate (main toggle AND this,
   API ≥ 31). Crash-reporter self-heal now disables it too.
2. `liquidGlassCapsule(container, washAlpha, backdrop: LayerBackdrop? = null)` —
   explicit local backdrop; null falls back to global capture.
3. Pet Designer: wrapper captures watermark+list; studio bar pinned below via
   weight-spacer Column (list bottom padding 16→96dp); `PetStudioBottomNav`
   branches solid fill vs glass capsule on `(glassOn, glassBackdrop)`.
4. Home: wrapper captures watermark+scroll column (`homeScroll` hoisted out so
   the sticky-bar sibling block still reads it); menu/profile pills restored to
   the v230 scroll morph → real glass, sampling the local capture. Profile pill
   still keeps classic morph when an avatar photo is set.
5. EntryDetail: same wrapper pattern; `DetailStickyBar(glassBackdrop = …)`; back/
   more pills restored.

Files: AppPreferences.kt, LiquidGlassPills.kt, HomeScreen.kt,
EntryDetailScreen.kt, PetDesignerScreen.kt, ExperimentsScreen.kt,
CurioCrashReporter.kt + changelog 20260920.txt + app/AGENTS.md (v234).

Verification: balance check OK ×7; no `glassOn = false` stubs remain; all new
symbols imported. CI validates compilation on push.

### Ideas for later (answered in chat, not implemented)
- Drag-to-shrink glass sheets / expandable FABs; glass sliders & switches;
  header→toolbar glass morph on scroll; glass side-rail for wide windows;
  chromatic-aberration lens variant (library flag exists); glass toast/snackbar.
