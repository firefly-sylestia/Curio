# Prompt.md — current request log

## Request (complete): v227 — Live Update notification + liquid-glass pills + cabinet strip

User decisions (via ask_user): live notif = Android 16 Live Update; glass scope = ALL three pills
(bottom nav, reveal category/favorite bar, pet-studio bar); toggle home = Experiments screen for now;
cabinet strip = real bug to find and remove properly.

### Done
1. **CI hotfix** (`fcf862f`): Roboto Flex OFL license moved out of `res/font/` → `app/third_party/roboto_flex_OFL.txt`
   (resource merger only accepts font binaries there).
2. **Cabinet strip root cause found**: the grid Column reserved `navigationBars + 84dp` clearance, so the
   LazyVerticalGrid CLIPPED every card at a hard horizontal line exactly at the capsule's top — entries cut
   above a plain band the pill sat on. Home/Reveal don't clip like this. Fix: removed the Column reservation,
   moved clearance into the grid's `contentPadding.bottom` (24 + 84 + navBars) — entries scroll full-bleed
   UNDER the floating pill; only the last row lifts clear.
3. **Android 16 Live Update**: running explore sessions on API 36+ post via
   `NotificationCompat.ProgressStyle` (accent Segment = durationMinutes max, progress = elapsed mins,
   IconCompat tracker). Paused / pre-16 keep BigTextStyle. Verified compat API against core 1.18.0 sources.
4. **Liquid glass pills** (Experiments toggle "Liquid glass pills", default OFF):
   - New dep `io.github.kyant0:backdrop:1.0.6` (Apache-2.0 — safe; vFlow's own glue is GPL so we wrote our own).
   - `ui/components/LiquidGlassPills.kt`: CurioGlassPills.backdrop + Modifier.liquidGlassCapsule(container).
   - NavHost: rememberLayerBackdrop + SideEffect publish; `.layerBackdrop()` marked ONLY on the content
     wrapper Box so overlay pills never record themselves.
   - Call sites glass-ified: CurioFloatingNavBar, RevealCategoryFavoriteBar, PetDesigner studio bar
     (Surface → Transparent + 0 elevation when active).

### Verification
Delimiter balance OK on all 9 touched Kotlin files; imports checked (SideEffect, Color, glass helpers);
compat ProgressStyle signatures verified from core-1.18.0 sources jar; backdrop AAR minSdk 21 merges fine.
Gradle forbidden here — CI on push is the compile source of truth.
