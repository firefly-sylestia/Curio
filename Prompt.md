# Current Request — Nav pill should collapse (not vanish) when leaving a tab

## Status: DONE (committed + pushed to Alpha)

## Request (user, paraphrased)
"when i open a page away from home screen like suppose profile screen from
home screen the home nav pill should collapse just the way it expands when
i back from home and we worked on it but its not working it still just
vanishes instead of collapse vanishing."

## Root cause
- `CurioNavHost` composed the floating pill bar only while
  `showBottomBar` was true (`routePrefix in CurioRoutes.bottomNavRoutePrefixes
  && !isRevealRoutePrefix`). Navigating to ANY non-tab route (Profile,
  settings sub-pages, Topic Reveal, …) flipped that gate false → the whole
  bar unmounted the instant the route changed → the expanded Home pill
  never ran its collapse spring, it just disappeared.
- Tab→tab switches worked (the bar stays composed, pills animate); the
  broken case was tab→non-tab.

## Fix (v193)
- New `barVisible` state in CurioNavHost: `LaunchedEffect(showBottomBar)`
  sets it true immediately when on a tab, and after a `delay(500)` when
  leaving (the pill's collapse spring + label retract settle in ~450ms —
  `PillWidthSpring`/`PillExpandSpring` = critical damping, stiffness 240).
  The bar composition gate changed from `showBottomBar` to `barVisible`.
- During the 500ms the bar stays composed with the new (non-tab) route:
  the previously-selected pill's `selected` flips false and it glides
  closed with the SAME springs it expands with, then the bar unmounts.
- Returning to a tab cancels the pending delay and remounts instantly
  (no flicker; the pill expands as before).
- The wide-window rail keeps the instant `showBottomBar` gate (rail items
  never expand/collapse).

## Docs
- `app/AGENTS.md` — v193 entry.
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — 1 FIX bullet.
- This Prompt.md.

## Verification
- Static only (no Gradle here — CI validates on push). `delay`,
  `remember`, `mutableStateOf`, `getValue`/`setValue`, `LaunchedEffect`
  all already imported in CurioNavHost.kt.
