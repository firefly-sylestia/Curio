# Request — UI polish: shuffle deck cascade + reveal text / nav flash / cabinet morph (v8.36)

## Completed (v8.36)

All items shipped (per user answers via ask_user):

1. **Shuffle deck — peeks BEHIND the main card.** The hero wrapper now
   carries `zIndex(10f)` (peeks fan at 2/5), so the front card draws on top
   instead of being overlapped by the peek cards.
2. **Swipe anywhere on the deck.** The horizontal-drag detector was hoisted
   from the front card to the whole deck Box — dragging on the peek cards
   or the hero rotates the fan. Front-card taps still open the topic.
3. **Two-pulse cascade (user-chosen).** Per shuffle tick: the TOP pair of
   peeks wipes together and the main card pulses with it, then the BOTTOM
   pair wipes (PeekWaveGroupGapMs = 110) and the main card pulses again.
   The hero's own content swap joins the first group (heroDelay = 0).
4. **Reveal long titles never cut (user-chosen "auto-grow").** The hero
   card measures how much of the title spills past the 3-line fold (px,
   from `onTextLayout` — tracks real line height incl. font scaling),
   latches it after 420ms of stability (so the shared-element morph isn't
   fought), and animates the card taller to fit.
5. **No more creamy nav-bar flash on reveal close.** The NavHost now keeps
   the bottom strip reserved while a reveal/detail destination is on top
   OR still popping (previous back-stack entry check). The dock / wash
   placeholder stays during the whole close transition, so the real bar
   never swaps in mid-fade. The reveal category wash resolves from the
   current-or-exiting reveal entry's args.
6. **Smooth Cabinet→Detail morph.** New `CabinetBoundsTransform` (near-
   critical spring) replaces the 320ms reveal tween for cabinet cards;
   the Detail route joined the reserve set (its own wash spacer registers
   from EntryDetailScreen, mirroring the reveal dock) so innerPadding never
   changes mid-morph; and the content below the hero blooms in with a
   delayed fade + rise (`DetailContentEntrance`) instead of popping while
   the card expands.

## Files changed

- `features/spin/SpinScreen.kt` — deck swipe hoist, hero zIndex, two-phase
  cascade (PeekWaveGroupGapMs), double hero pulse, hero swap delay 0.
- `features/reveal/TopicRevealScreen.kt` — auto-growing hero
  (RevealHeroBaseHeight + measured overflow px).
- `navigation/CurioNavHost.kt` — reserve covers reveal+detail top AND
  popping; revealCat from current-or-previous entry; reserveBackground;
  reserve-first bottomBar branch order; detail call site registers wash.
- `features/detail/EntryDetailScreen.kt` — wash spacer registration,
  CabinetBoundsTransform, DetailContentEntrance below the hero.
- `features/cabinet/CabinetScreen.kt` — CabinetBoundsTransform; cabinet
  wash stays published (mirrors spin) for the detail first-frame fallback.
- `ui/adaptive/RevealSharedScopes.kt` — new CabinetBoundsTransform spring.
- `app/build.gradle.kts` → 20260825; new store changelog.

## User decisions (asked via ask_user)

1. **Cascade pattern:** "Two pulses per tick" — top pair wipes + hero
   pulse, then bottom pair wipes + hero pulse.
2. **Swipe scope:** "Anywhere on the deck" — whole fan responds.
3. **Reveal text:** "Auto-grow the hero" — long titles expand the card
   (no Read-more, no cut).

## Validation

- Delimiter balance on all 6 touched Kotlin files (block-comment aware);
  `git diff --check` clean; no leftover PeekWaveStaggerMs /
  RevealBoundsTransform refs in the edited files.
- Import hygiene: added `kotlinx.coroutines.delay` (reveal), `animateDpAsState`,
  `AnimatedVisibility`/`MutableTransitionState`/`fadeIn`/`slideInVertically`
  (detail), `WindowInsets`/`navigationBars`/`windowInsetsPadding` (detail,
  matching TopicRevealScreen's packages); removed now-unused
  `DisposableEffect` import (cabinet).
- code-reviewer-glm: fixed the one actionable finding — hero growth was a
  fixed 38.dp/line, which clips under accessibility font scaling; now
  derived from the measured text height (getLineBottom of line 2).
- No Gradle build in this environment (repo rule) — CI on push is the gate.

## Notes

- The detail wash spacer height (80dp + inset inside) exactly matches the
  reserved bar height, so placeholder → spacer swaps never change
  Scaffold innerPadding.
- One-frame fallback color on detail open uses `CurioNavTint.cabinetWash`
  (kept published) or surface; the detail page's own spacer registers a
  frame later.
