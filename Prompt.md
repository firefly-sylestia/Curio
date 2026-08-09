# Request — Reveal bottom nav only from the main card

## User request
Keep the bottom navigation bar on the Topic Reveal page when it is opened from the Spin main card, but hide it when the reveal is opened from the topic browser (Browse Topics database).

## Changes completed
- Added `isBrowseRevealRoute(entry)` in `CurioNavHost.kt`: true when the destination is `CurioRoutes.REVEAL` with the `browse` argument equal to `"1"` (the read-only browse mode used only by `revealForBrowse`, which the topic browser navigates with).
- Gated `showBottomBar` so browse-mode Reveal hides the bar while every other Reveal (main-card, Home recents, pinned, Topic History, Recent, tour) keeps it.
- Guarded `isTabSwitch` so browse-mode Reveal is not treated as a tab crossfade — it transitions as a pushed page.

## Validation
- `node scripts/check_braces.js` passed: 125 files checked.
- `git diff --check` passed.
- Focused review confirmed the helper, bar gating, and tab-switch guard are correct and compile-safe, with the `browse` argument defaulting to `"0"` and set to `"1"` only by the topic-browser path.
- Local Gradle compile/build/lint/test commands were not run because repository instructions forbid local Android builds; CI remains authoritative.

## Status
Implementation and static validation are complete. Per the user's standing preference, ask for confirmation before committing and pushing. Also still uncommitted from the prior request: the deck-peek animation default restoration (option-gated tail fade).
