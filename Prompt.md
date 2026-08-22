# Prompt.md — current request log

## Request (complete): Pet Designer crash follow-up + native crash reporting (v232)

User: opening Pet Designer still crashes even though the Topic Reveal screen and Home
don't — and the in-app crash detector doesn't pick the crash up.

### Analysis
- Structurally, Reveal's category/favorite bar and the Pet Designer studio bar are
  IDENTICAL: both are `liquidGlassCapsule` pills inside the NavHost's captured page Box,
  both covered by the v228 `isCapturingBackdrop` self-capture guard. No Popups, no extra
  layerBackdrop/recordLayer sites in Pet Designer. So "why only Pet Designer" has no
  remaining code-level answer we can see — some device-specific path (Samsung A35 /
  Android 16 / Vulkan / libhwui) still recurses natively on that screen.
- The crash detector missed it because SIGSEGV on RenderThread kills the process inside
  libc; Java's UncaughtExceptionHandler never runs. Only ApplicationExitInfo can see it.

### Changes
1. `CurioCrashReporter.checkNativeCrash()` — called from `init()` (API 30+). Reads the
   previous process exit via `getHistoricalProcessExitReasons`: SIGNALED exits and CRASH
   exits without our pending flag count as native crashes → persisted into the same
   crash history + pending-crash flow (crash screen now shows them) and into the same
   loop window (repeated native deaths trip safe mode). Dedup via last-seen timestamp.
2. Self-heal: if liquid glass was enabled at death, both glass toggles auto-OFF before
   UI comes up; noted in the persisted log. Invisible crash-loops can't persist.
3. `PetDesignerScreen` studio bar: glass disabled for now (always solid elevated fill)
   pending a real tombstone from the new reporter. Everything else unchanged.

### Status
DONE — committed & pushed. Next step when a native crash log shows up in-app: use its
reason/signal/description to pin the exact render-node cycle and re-enable studio-bar
glass with a targeted fix.
