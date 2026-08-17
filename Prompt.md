# Prompt.md — Request log

## Current request — COMPLETED: reveal morph pills, pet designer pill bar + fade, manage-categories full-bleed, first-run pick-a-lane wiring

The user asked (refined across several messages): (1) make the topic reveal open smoothly
by morphing the pill properly and unifying its style/animation; (2) show a topic's year as
a pill instead of inside the shuffle-card title; (3) swap the reveal hero's action pill and
byline so the byline ("Director / Author") stays in the top corner in BOTH the ticket and
the reveal (reveal-only change); (4) apply the same style in the Pet Designer (user
confirmed: pill style AND smoother open); (5) make the Manage Categories page (opened from
the Spin picker's "Manage categories" button) match the Scaffold-removed full-bleed
treatment (user confirmed); (6) wire the first-run "Pick a lane" to the Spin screen's
category picker.

### Part 1 — reveal morph (v141, pushed fee1fb8)
- `CurioTopic.titleAndYearQualifier()` — splits a trailing " (…)" / " — …" qualifier.
- Ticket + hero BOTH show the base title (no year) and a top-left pill ROW: byline +
  year pill (Schedule icon), identical recipe (ink@18%, shape 50, labelMedium bold,
  h12/v6) → the pills and titles read identical during the shared-element morph.
- The reveal hero's action pill ("Watch for ~25 min") moved DOWN to the bottom pill row
  next to the subtype.

### Part 2 — v142 (this commit)
- **Manage Categories full-bleed**: NavHost drops the nav-bar inset for the
  MANAGE_CATEGORIES route (`fullBleedBottomRoutePrefixes`); the page pads its own
  LazyColumn + scroll indicator with `navigationBarsPadding()` and the wash runs to the
  bottom edge — no reserved strip (the reveal's v132 precedent).
- **Pet Designer**: `PetStudioBottomNav` restyled from the stock M3 NavigationBar to the
  app's floating pill bar (rounded-50 surfaceContainerHigh container, solid secondary
  active capsule + onSecondary ink, 52dp tabs); unused NavigationBar/WindowInsets
  imports removed. Route opens with the reveal's clean fade (`isPetDesignerRoute`
  branches in enter/exit/popEnter/popExit before the scale-pop group).
- **First-run "Pick a lane"**: Home's FirstTimeEmpty now sets `SpinPickerRequest.pending`
  (new one-shot object beside `CurioDrawerState`) and navigates to the Spin tab;
  SpinScreen's `LaunchedEffect(SpinPickerRequest.pending)` opens its own
  `CategoryPickerSheet` (lane chips + Mix presets) instead of the separate PICKER page.

### Verification
No Gradle build in this environment (project rule — CI validates on push). On-device:
open a year-qualified topic (Moby-Dick) from the deck (byline + year pill top-left on
both, action pill bottom); open the Pet Designer (floating pill bar + fade open); open
Manage Categories from the Spin picker (full-bleed bottom); fresh install → Home "Pick a
lane" opens the Spin sheet.

### CI fix — v143
compileDebug/Release failed on OfflineTranscriber.kt with a CASCADE from two real bugs:
(1) `val states = _states` was declared BEFORE `private val _states = ...` (property
initializers run in declaration order → "Variable '_states' must be initialized");
(2) `VoskModelDownloads` (a separate top-level object) referenced `Info` / `modelsDir`
unqualified — they're nested in `VoskModels`, so `start`/`downloadWithPause` now take
`VoskModels.Info` and extraction uses `VoskModels.modelsDir`. Also replaced
`zipFile.outputStream(received > 0)` with an explicit `FileOutputStream(zipFile, append)`
(the kotlin.io Boolean overload resolution was ambiguous in CI).
