# Prompt.md — current request log

## Request (complete): scroll crash + crash-log UI + classic-style glass tabs (v233)

User: still crashing while scrolling (probably the floating pill); make the crash
detector show the native crash log; the expanded glass nav pill doesn't follow the
old look (side text); improve the active indicator.

### Root causes / analysis
1. Scroll crash: Home menu/profile pills and detail back/more pills are INSIDE the
   NavHost capture subtree AND rebuild `drawBackdrop` on every scroll frame
   (`washAlpha = lerp(..., frostShift)` changes per frame → modifier chain teardown/
   rebuild + full-subtree re-record per frame). Same native-crash class as Pet
   Designer. Fix: `glassOn = false` at those sites (code kept, imports cleaned);
   classic solid→frost morph restored; live glass only on the bottom-nav overlay.
2. Crash log display: pipeline already complete after v232 (checkNativeCrash →
   persistCrash → pending flag → Splash routes to CRASH screen → log shown) — no
   extra wiring needed; verified SplashScreen + CurioCrashScreen read paths.
3. Glass tabs now follow the classic pill language: inactive icon-only; active
   springs to 136dp with label sliding out BESIDE the icon (Changa One 15sp,
   accent ink crossfade via curioActivePillInk). Item container Column→Row.
4. Indicator: per-tab measured widths (LocalLiquidGlassTabMetrics, tabWidthsPx +
   version counter) replace the even-split assumption everywhere — indicator box,
   drag math, RTL canDrag, specular highlight position — plus a constant faint
   accent wash so it reads as active at rest.

### Status
DONE — committed & pushed. If any further native crash occurs with glass ON, the
reporter captures it and self-heals (toggles off) so it can never loop silently.
