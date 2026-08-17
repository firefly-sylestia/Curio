# Prompt.md — Request log

## Current request — revert the nav-bar → sentiment-pill shared morph, keep the bigger bottom pill

User (GitHub commit `55ebc74` "feat: bigger bottom pill (60dp/128dp) + nav bar morphs into the reveal Like/Dislike pill via a shared element"): "revert this just keep the size large but revert the shared morph one".

### What that commit (plus its CI-fix follow-up `05e1154`) changed
1. **Bigger bottom pill** — `FloatingPillIconWidth/ExpandedWidth/Height` 52/112/52 → 60/128/60, icon 24→26. **→ KEEP.**
2. **Shared-element morph** — `SentimentSharedElementKey` ("nav-pill-sentiment") + `NavPillBoundsTransform` (320ms tween); the nav bar moved INSIDE the SharedTransitionLayout and stays composed 500ms after the reveal opens (`sentimentMorphVisible` LaunchedEffect) as the caller-managed source (`sharedElementWithCallerManagedVisibility`, pills non-interactive via `interactive = showBottomBar`); the reveal's `RevealSentimentPill` is the route-scoped target (`sharedElement` via a new `modifier` param). **→ REVERT.**

### Revert plan (done)
- **RevealSharedScopes.kt**: deleted `SentimentSharedElementKey` + `NavPillBoundsTransform`.
- **CurioBottomNav.kt**: `CurioFloatingNavBar` back to `(navController, modifier)`; `FloatingNavPill` back to plain `clickable(onClick)`; dropped the `SharedTransitionScope` / `LocalRevealSharedScope` / `NavPillBoundsTransform` imports and the `sharedModifier` block; **kept 60/128/60 + 26dp**.
- **CurioNavHost.kt**: dropped `sentimentMorphVisible` + `sentimentSharedState` and the `SentimentSharedElementKey` import; the bar renders with the plain pre-morph block (`if (!wide && showBottomBar && TourController.currentStep == null) { CurioFloatingNavBar(...) }`) back outside the SharedTransitionLayout.
- **TopicRevealScreen.kt**: dropped the morph state + `sharedElement` modifier at the sentiment pill call; `RevealSentimentPill` lost its `modifier` param; dropped the two imports.
- **Docs**: app/AGENTS.md — new v153 note (morph reverted, size kept), v151/v152 entries rewritten to match; changelog FIX line now only mentions the bigger pill.

### Verification
`git diff` of the four code files against the pre-morph parent shows ONLY the size change (60/128/60 + 26dp icon) — the morph is fully gone, braces/structure restored. No Gradle build in this environment (CI validates on push).

### Git state
Real code change → commit + push. The earlier docs-only commit (workflow rules) is still unpushed and rides along with this push per the docs-commit rule.

## Earlier completed request — workflow/instruction changes (commit only, no push)

User asked for three durable rules added to the root AGENTS.md, committed but NOT pushed ("dont push this just commit"):

1. **git pull first** — General Workflow now starts at step 0: run `git pull` before the first work of any session.
2. **Ask before deleting/replacing anything** — the durable preference in "ASK WHEN UNSURE" now explicitly covers deleting/replacing/overwriting ANY file, data entry, or content (topic JSON entries, strings, assets, docs).
3. **Text/docs changes commit but never push alone** — the old "SMALL TEXT-ONLY CHANGES — DO NOT PUSH" section became "TEXT-ONLY / DOCS CHANGES — COMMIT, BUT PUSH ONLY WITH THE NEXT REAL CHANGE".

Committed locally per the user's explicit instruction; rides along with the next real change (this revert).
