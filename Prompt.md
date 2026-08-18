# Current Request — v206: splash redesign (bigger gradient wordmark, warm tagline, bottom ground)

## Status: DONE (committed, NOT pushed — user: "dont push it")

## Request (user, verbatim)
"okay heres the redesigned spalsh screen, make the Curio tet bigger and with gradient basced on the dark or light mode. and at the buttom it have a similiar gradient backgroud of the app backgroud. not full just at the buttom and also discover something that text gets a little bigger too and warmer in dark mode light mode you figure it out."

## What changed — `features/splash/SplashScreen.kt`
1. **Wordmark**: "Curio" 36 → 72sp (same Geom Bold displaySmall), painted with a theme-aware horizontal gradient echoing the cosmic mark: dark = SkyMint → ButterYellow (bright on the dark sky); light = CoralInk → GoldInk (deep, readable on the cream).
2. **Bottom ground**: a bottom-anchored band (bottom 34% of the screen) fading transparent → warmed app-background tone (dark: `lerp(background, CoralBlush, 0.05)`; light: `lerp(background, ButterYellow, 0.14)`). Not full-bleed.
3. **Tagline**: 14 → 18sp, warmer in both themes — parchment #D8CDB4 on dark, warm khaki #7E6E50 on light (replaces cool onBackground @ 0.62).
4. Unchanged: logomark + shimmer, animated halo, 3-dot loader. Brace/paren-balanced.

## Files
- `features/splash/SplashScreen.kt`
- Docs: `app/AGENTS.md` (v206), changelog ADD bullet, this Prompt.md.

## NOTE
Committed locally only — NOT pushed (explicit user instruction for the splash).

## Also in this session (already pushed)
- v205 (`0e638f8`): app-size diet — release APK ~20MB smaller via `ndk.abiFilters` arm-only (x86/x86_64 emulator-only Vosk libs dropped from release; debug keeps all ABIs; release.yml hard guard now expects universal + 2 arm splits).
