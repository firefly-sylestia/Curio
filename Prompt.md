# Prompt — Current Request

## Request
Splash follow-up: "too small", "the icon itself moving looks bad try something else", "load the topics in the splash screen so it's ready."

## Status: COMPLETE

## Changes (`features/splash/SplashScreen.kt`)
- BIGGER: logomark 64 → 88dp art (100dp box), wordmark headlineMedium → displaySmall, bar 148 → 180dp / 8dp tall rounded, spacing opened up.
- Motion swap: the vertical bobbing is GONE — the logo now BREATHES (slow 2.3s scale pulse 1.0→1.035 combined with the entrance scale via graphicsLayer; no positional movement).
- Topics loaded ON the splash: the bar is DETERMINATE and wired to the real catalog warm-up — `warmedLanes` increments per parsed lane (background thread snapshot write is safe), `shownProgress` animates to it, and it's forced to 100% right before navigation. Warm-up/routing logic unchanged (800ms min, ~6s cap).

## Verification
- Delimiter balance OK; `progress = { }` lambda overload matches project-wide usage (material3 1.5.0-alpha20); no unused imports.
