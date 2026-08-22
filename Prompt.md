# Prompt — Current Request

## Request
Splash screen redesign: "make the splash screen simple and modern and material style. the current one is too bad looking and also too huge. add dynamic animation and add like your curiosity loading something."

## Status: COMPLETE

## Changes (`features/splash/SplashScreen.kt`, full rewrite of the UI body)
- STRIPPED: 280dp animated halo, shimmer sweep, bottom ground band, 144dp logo box + 112dp art, 72sp gradient wordmark, 18sp tagline, 3-dot pulse loader (~200 lines of layered decoration).
- NEW compact M3 composition on plain `colorScheme.background`:
  - 64dp logomark in a 72dp box — entrance scale 0.7→1 (zero-overshoot tween) + endless ±5dp vertical float (1.9s reverse).
  - "Curio" wordmark, `headlineMedium` (Geom via theme), `onSurface`.
  - ONE M3 `LinearProgressIndicator` (148dp) — Material's own loading language.
  - FOUR rotating curiosity lines ("Loading your curiosity…" / "Warming up the topics…" / "Sharpening the shuffle…" / "Opening the cabinet…") crossfaded via `AnimatedContent` every 1.1s.
- Every color is a plain theme role → light/dark/pastel/Material all correct with zero special-casing.
- UNCHANGED: catalog warm-up + CRASH/ONBOARDING/HOME routing, 800ms minimum, 6s cap.

## Verification
- Delimiter balance OK; APIs verified against compose animation 1.11.2 (`AnimatedContent`+`togetherWith` stable, `mutableIntStateOf` used app-wide); no other file references removed symbols; `app_tagline` still used by Onboarding so the resource stays.

## Docs
- app/AGENTS.md ownership descriptor updated (v224 note); changelog 20260920.txt ADD bullet added.
