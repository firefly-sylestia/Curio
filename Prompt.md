# Request — Fix intermittent blank shuffle page

## User request
The Spin/shuffle card page sometimes rendered with nothing, unlike before.

## Diagnosis
`HeroTicketCard` returned immediately when the shared-transition composition locals were temporarily unavailable during navigation or destination restoration:
- `LocalRevealSharedScope.current ?: return`
- `LocalRevealVisibilityScope.current ?: return`

That early return removed the visible hero card for the frame, making the shuffle page appear blank. The shared transition is optional presentation polish; the deck must remain visible even when its scope is not ready.

## Fix completed
- `HeroTicketCard` now keeps rendering when either shared-transition local is unavailable.
- The shared-element modifier is applied only when both scopes and the remembered shared state are available; otherwise the card uses its normal non-morphing modifier for that frame.
- Intentional empty/filter behavior was preserved; no fake topic or loading card was added.

## Validation
- `node scripts/check_braces.js` passed.
- `git diff --check` passed.
- Focused review found no critical nullability or scope blockers.
- Local Gradle compile/build/lint/test commands were not run because repository instructions forbid local Android builds; CI remains authoritative.

## Status
The targeted Spin fix is ready to commit and push.
