# Prompt — FieldMind/Curio request log

## Active request: Fix blank Topic Reveal page (regression from af10023)

### Symptom (user-confirmed)
After commit `af10023` (PR #3, Alpha branch), opening the Topic Reveal screen shows a
blank page — the hero card, topic name, tags, teaser and action prompt are all gone.
The ONLY visible elements are the two dock buttons ("Already watched" / "Start
exploring"), which render in the Scaffold bottomBar slot. User pointed at `af10023`
as the culprit.

### Diagnosis
`af10023` changed exactly two layers:
1. **NavHost** — added `Scaffold(containerColor = revealWash ?: background)` where
   `revealWash` is recomputed per composition from the back-stack category arg via a
   `@Composable` wash helper. This is the ONLY page-level delta.
2. **Dock visuals** — transparent dock/buttons, swapped order, undo labels,
   `heightIn(80.dp) + windowInsetsPadding(navigationBars)`.

The dock layer is mechanically ruled out (different composition layer, identical
layout math to the working 6c9b8a6 dock). The symptom (everything inside
`SharedTransitionLayout` invisible, dock outside it visible) matches a
frozen/disrupted shared-element route transition: while a shared transition is
active, Compose pauses ALL animations inside the layout — a stuck morph leaves the
hero + `RevealContentEntrance` content invisible forever. Painting the Scaffold
container with a dynamically-computed color exactly at the moment the route
transition begins is the only plausible trigger.

### Fix (implemented, staged)
1. **CurioNavHost.kt** — removed the `revealWash` computation, the
   `categoryBackgroundWash` import, and the `containerColor` param (Scaffold back to
   its constant default background). Documented the regression in a v8.5 comment.
2. **TopicRevealScreen.kt** — `RevealActionDock` Surface now paints
   `cat.categoryBackgroundWash()` (the SAME wash the reveal page paints) instead of
   `Color.Transparent`. Visually identical to the requested 100%-transparent dock
   (buttons remain fully transparent), but also covers the nav-bar strip so the
   cream band behind the dock never shows — without touching the NavHost.

Kept from af10023 (user-requested design): transparent buttons, Already/Undo LEFT +
Start exploring RIGHT, category undo labels, nav-inset-safe dock.

### Validation
- Brace balance equal, `git diff --check` clean, no leftover `revealWash` refs,
  imports verified (CurioCategories + MaterialTheme still used in NavHost).
- Code review passed. Notes: containerColor theory is a hypothesis → user must
  verify on-device; if the blank persists, next suspects are the hero
  `key(resolved?.id)` remount re-registering the shared element mid-morph and the
  placeholder→dock innerPadding delta (both pre-existing in 6c9b8a6, which worked).

### Completion summary
- Fix is committed locally as `4d70eba` on branch `work`.
- Static validation passed: delimiter balance for `CurioNavHost.kt` and `TopicRevealScreen.kt`; `git diff --check` clean.
- Local Gradle build/test/lint commands were intentionally not run because root `AGENTS.md` forbids Android compile/build/lint/test commands in this environment.
- User should verify on-device that Topic Reveal now renders the hero/content and that the bottom dock still appears visually transparent over the category wash.
