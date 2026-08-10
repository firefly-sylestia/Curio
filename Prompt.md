# Request — Complete the pet-led tour + torn paper edge on Topic Reveal

## User request
1. In the tour, remove the "Express yourself" tap-as-next behavior: instead, TELL the user they can use Express yourself, but keep the button inert during the tour — they advance via Next, and the tour finishes properly after everything.
2. Add more tour stops so the tour shows Cabinet → Topic Browser → Profile → Quests → Settings ("and everything").
3. On Topic Reveal (from the Spin main card), hide the bottom nav and add a torn paper edge at navbar height instead (the tear REPLACES the nav; "the tear in opp" = torn at the top edge of a bottom strip).

## Changes completed
- `TourController.kt`: tour steps are now Home shuffle → Spin → Reveal (express-yourself tell-only stop) → Cabinet → Topic Browser → Profile → Quests → Settings. Added `isLastStep` ("Done" label on the final stop). The express-yourself step dialogue now tells the user the note stays closed and to tap Next.
- `CurioNavHost.kt`: `showBottomBar` now excludes ALL reveal routes (routePrefix "reveal") — the reveal never shows the bottom bar. Tour controls label the final step "Done"; on Done the tour deactivates and pops the whole tour stack back to HOME (clean finish, no deep back stack). Updated stale comments.
- `TopicRevealScreen.kt`: added a bottom torn paper edge at navbar height (80dp, `SoftTornBottomShape` flipped 180° so it tears UP, `CurioColors.CreamWhite`, fixed seed) that replaces the bottom nav; bumped the scroll's trailing spacer so content clears the seam. "Express yourself" and Explore buttons are inert while `TourController.active`.
- `TopicDatabaseScreen.kt`: wrapped the search field in a `PetLandmark` (id "search", screen "database") so the tour's Browse-Topics stop points at it.
- `SettingsHubScreen.kt`: wrapped the Appearance row in a `PetLandmark` (id "appearance", screen "settings") for the tour's Settings stop.

## Validation
- `node scripts/check_braces.js` passed on all 5 edited files.
- Code review confirmed compile-safety (imports, BoxScope `.align`, modifier order, unused-import removal for `offset`/`SoftTornSheetShape`) and flagged only the shared-hero morph height change (expected consequence of the tear replacing the nav — verify on device).
- Local Gradle compile/build/lint/test commands were not run because repository instructions forbid local Android builds; CI remains authoritative.

## Status
Committed and pushed: `5464cd4 feat: complete pet-led tour and add reveal torn bottom edge` (branch Alpha).
