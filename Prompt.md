# Prompt.md — current request log

## Request (complete): vFlow-exact liquid glass nav + update toast → dialog

1. **Full liquid-glass nav (vFlow port)** — user asked for the exact vFlow look AND touch/drag.
   Ported four files from ChaoMixian/vFlow into `ui/components/liquidglass/` with GPL-2.0 attribution
   headers (DragGestureInspector, DampedDragAnimation, InteractiveHighlight, CurioLiquidGlassTabBar).
   `CurioFloatingNavBar` now renders the full tab bar (equal-width tabs, ONE refracting capsule, hidden
   accent-tinted layer so the pill refracts colored icons, DRAGGABLE active pill with velocity
   squash/stretch, press-scale, inner shadow, API-33+ specular highlight) when the Experiments toggle is
   ON; classic pill row otherwise. Reveal/pet-studio keep the simple frosted capsule. LICENSE NOTE: these
   ports are GPL-2.0-derived — flagged to the user in the summary.

2. **Update toast → dialog** — corner toast fully removed (`CurioInAppToast.kt` deleted). UpdateChecker
   raises `CurioUpdatePrompt.pending` (global state, once-per-version gate kept); NavHost renders a themed
   AlertDialog ("Curio vX is available" / body / Open Updates / Later) at the root.

Verification: delimiter balance OK on all touched files; imports verified (CurioUpdatePrompt added to
NavHost; stale CurioToast/CurioIcons references gone); changelog cleaned (duplicate line dropped, toast
bullet replaced by dialog bullet). CI validates compile on push.
