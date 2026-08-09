# Request — Correct browser and reveal actions; finish the focused animation fix

## User request
Remove the Explore and Express Yourself buttons that were mistakenly added to each Topic Database/browser row. Keep the existing Explore action inside Topic Reveal, and re-enable Express Yourself there, including when a topic is opened from the database browser.

## Completed changes in this turn
- Topic Database rows no longer receive `onExplore` or `onExpress` callbacks and no longer render nested Explore / Express Yourself chips.
- Topic Database rows still open the read-only `revealForBrowse` route when the row is tapped.
- Topic Reveal keeps its Explore action; in browse mode it remains a silent Explore action.
- Topic Reveal Express Yourself is enabled in browse mode and navigates to the capture screen as the explicit write path.
- Updated Topic Reveal comments to match the browse-mode behavior.

## Also preserved in the current uncommitted working tree from the prior request
- Removed the full-screen transparent tour-control wrapper and Reveal-only bottom clearance padding so the underlying content is not covered.
- Peek cards remain fully opaque during travel; outgoing card content fades only in the tail of its own exit transition.
- The broader Material/AMOLED theme, hero, catalog cache/sort, and splash warm-up patch remains uncommitted pending user approval to push.

## Validation
- Static brace check passed: 125 files checked.
- `git diff --check` passed.
- Browser action symbol audit passed: no `openSilentExplore`, `onExplore`, `onExpress`, or stale `LocalContext` references remain in TopicDatabaseScreen.
- Gradle compile/build/lint/test commands were not run because local Android builds are forbidden by the repository contract; CI remains authoritative.

## Validation and release status
- Full repository-safe static validation passed: `node scripts/check_braces.js` checked 125 files and `git diff --check` reported no issues.
- Focused audits passed: no Topic Database row action callbacks/UI remain; Reveal Express Yourself is enabled and routes to capture; PeekCard no longer uses global alpha; tour controls no longer use a full-screen transparent wrapper; changed call signatures match.
- Code review found no critical blocker. CI remains required for Kotlin/Compose compilation because local Gradle builds are forbidden by the repository contract.
- User explicitly authorized full validation followed by commit and push.

## Status
Validated and ready to commit/push.
