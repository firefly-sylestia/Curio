# Prompt.md — Request log

## Current request — light-mode nav capsule tint + smoother pill animations

User: "the active indicator gets the theme dynamic color but in light mode the
background of it doesn't so make it get the background tint. and also the
animations feels clanky sometimes make it even more smother".

### Background tint (light mode)
`curioFloatingNavContainer` lifted the page wash 55% toward the light theme's
parchment `surfaceContainerHigh`, which erased the tint — the capsule read as a
plain cream bar behind the colored active pill. Now `lerp(wash, surfaceContainerHigh, 0.30)`
so the page tint shows through while still lifted above the page. Dark mode
unchanged (pages are near-black; wash adds nothing). Affects the bottom nav,
tour dock and pet studio bars (all share the container function).

### Smoother animations (nav bar pills + reveal Like/Dislike segments)
- Width spring damping 0.75 → 0.9 (near-critical — no settle bounce).
- Active fill fades via `animateColorAsState(activeFill.copy(alpha = …))`
  synced to the width spring instead of snapping on/off.
- Icon tint crossfades (tween 200 FastOutSlowIn).
- Label fade tween 160 → 240 FastOutSlowIn so it tracks the pill's expansion
  (exit stays instant per v125).

Docs: changelog FIX line, AGENTS.md v155 note, Prompt.md. Committed + pushed.

## Earlier completed request — reveal Like/Dislike pill matches the bigger 60dp nav-bar pill

Follow-up to the morph revert: "Make the reveal Like/Dislike pill match the
bigger 60dp nav-bar pill size."

- `RevealSentimentIconWidth` 52→60dp, `RevealSentimentExpandedWidth` 96→128dp,
  `RevealSentimentHeight` 48→60dp, segment icon 20→26dp; the pill's inner Row
  now uses the nav bar's exact padding/spacing (7dp / 6dp). The reveal
  sentiment segments are now identical in geometry to the nav bar pills.
- Changelog FIX line updated. Committed + pushed.

## Earlier completed request — revert the nav-bar → sentiment-pill shared morph, keep the bigger bottom pill

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
