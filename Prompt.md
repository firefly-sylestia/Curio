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

## Current request — v144: tour controls → floating pill bar

User picked the "guided-tour Skip/Next dock" follow-up: convert the tour's full-width
opaque bottom dock into the app's floating pill bar.

### Done (CurioNavHost.kt only)
- The tour `Surface` dropped `fillMaxWidth()` + `tonalElevation` and is now a rounded-50
  `surfaceContainerHigh` capsule with 6dp shadow, `navigationBarsPadding()` + 12dp air
  gap (the CurioFloatingNavBar recipe). Buttons are content-sized 52dp capsules inside
  (Skip = soft `surfaceVariant` secondary, Next/Done = solid primary CTA); row padding
  7dp / spacing 6dp. The full-screen tap-to-advance layer is untouched.
- `CurioFloatingNavBar` now YIELDS while the tour runs (`TourController.currentStep ==
  null` added to the v135 drawer gate) — the old opaque dock covered the bar, so the bar
  must not float behind/around the tour pill on tab stops.
- Removed the now-dead `fillMaxWidth` import.

### Verification
No Gradle build here (CI validates on push). On-device: start the tour from the Home
pet offer — Skip/Next float as a pill above the gesture bar with the page visible around
them, and the main nav pill is hidden on tab stops (no stacked pills).

### CI fix — v145
compileDebug/Release failed on the v141 morph commit with TWO bugs (both real, both
still in HEAD): (1) `titleAndYearQualifier` is a top-level extension in
`CurioTopic.kt` — the Spin ticket and reveal hero called it WITHOUT the import
(Unresolved reference at all four call sites); (2) `yearQual` destructured as
`String?` and the `isNullOrBlank()` guard does NOT smart-cast, so
`Text(text = yearQual)` was ambiguous between the String and AnnotatedString
overloads (the compiler can't pick for a nullable). Fixed: added
`import com.curio.app.data.titleAndYearQualifier` to SpinScreen.kt +
TopicRevealScreen.kt, and `text = yearQual.orEmpty()` at both year-pill Text call
sites (the guard already guarantees non-blank, so orEmpty is lossless).
