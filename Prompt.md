# Request — Restore Express Yourself and clarify Explore providers

## User request
Re-enable the Express Yourself action in Topic Browser, make the Topic Reveal Express Yourself button wider and easier to tap, change the Explore dialog actions to explicit Google and YouTube choices for every category, and reduce the pet's sustained idle animation work because the app was heating.

## Analysis and plan
- Add a tracked Express Yourself action to each Topic Browser row while preserving the existing silent Explore action.
- Use explicit Google and YouTube search actions in the Topic Reveal dialog rather than category-dependent or duplicate provider wording.
- Keep both provider buttons available for every category.
- Give the Topic Reveal writing action a full weighted half-row and a 52dp touch target.
- Reduce always-running idle animation channels without removing the pet or its visible idle/bink/interaction behavior.
- Do not run Gradle locally; Android builds are forbidden by the project DOX contract and CI remains authoritative.

## Completed changes
- Topic Browser rows now expose both Explore and Express yourself actions. Express Yourself records the explored topic consistently and navigates to the capture screen; Explore remains silent and opens the search page without recording.
- Topic Reveal's provider dialog now presents Explore in Google, Explore in YouTube, and Not now directly, for every category.
- Topic Reveal's Express yourself control is a full weighted action area with a 52dp height and centered label/icon, improving its hit target.
- Pet idle rendering now uses two animated channels (body bob and blink); breathing, glance, and ear-flick phases share the bob phase instead of running three additional independent infinite animations. Removed stale animation specs.
- Removed stale Reveal action metrics and fixed the Reveal action-button parameter mismatch found during audit.

## Validation
- `node scripts/check_braces.js` passed: 125 files checked.
- `git diff --check` passed.
- Stale-symbol audit passed: no old provider dialog state, obsolete Reveal padding fields, or removed pet animation specs remain.
- Focused review found no critical compile or interaction blocker.
- Gradle compile/build/lint/test commands were not run because local Android builds are forbidden by the repository contract; CI remains the compile source of truth.
- Device thermal behavior cannot be measured in this environment; the idle-channel reduction is a conservative mitigation, not a claim of measured thermal resolution.

## Status
This second patch is intentionally uncommitted and unpushed. Ask the user for approval before commit/push.
