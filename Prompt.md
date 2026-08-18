# Current Request — v207: drawer footer float + About-behind-footer, coil left-end hook, stats sun/moon, smoother nav collapse, Like/Dislike height

## Status: DONE (committed, NOT pushed)

## Request (user, verbatim)
"why the footer is floating now and when the about gets expanded its behind the footer. also the 3d hole is good. now mak the left end the side its out curve a little so it looks seemless connected also in your curiocity page place the drawing of sun and moon a little below the start bar just the moon and the sun not the whole drawing. and only in your curiocity page as a separate maybe. and the collapse of home nav pil can be more smoother, and the like and dislike size still doesnt match with home nav pill, like its height"

## What changed
1. **Drawer footer** — v203 pinned the footer as an OVERLAY over the list tail; expanded sections slid under its fade and it read as floating. Now `HomeDrawerContent` wraps the rows in a Column: the list sits in a weight(1f) Box ABOVE the footer, which is in normal flow pinned to the sheet bottom — never floats, rows never hide behind it.
2. **Coil left end** — prepended a small hook (moveTo 0.16,1.34 → cubic into 0,1.0) to `CoilOutlineNorm` in PaperStatCard.kt, so the protruding left end curves like a wire wrapping around the card edge.
3. **Stats sun/moon** — new private `StatsCelestialBody` on the Your Curiosity page only: gold sun + glow in light, cream crescent carved by the sky mid-tone in dark (mirrors the SVG construction), floating just below the status bar (TopEnd). Not interactive.
4. **Nav collapse smoother** — pill family 150 → 120 stiffness; NavHost hold 420 → 460ms; Reveal + Pet Studio springs follow to 120 (lockstep).
5. **Like/Dislike height** — sentiment capsule Row padding 7 → 8dp, gap 6 → 10dp → capsule 68dp tall, exactly the nav bar's capsule.

## Files
- `features/home/HomeScreen.kt` (drawer Column restructure)
- `ui/components/PaperStatCard.kt` (coil hook)
- `features/stats/StatsScreen.kt` (StatsCelestialBody)
- `ui/components/CurioBottomNav.kt`, `navigation/CurioNavHost.kt`, `features/reveal/TopicRevealScreen.kt`, `features/petdesigner/PetDesignerScreen.kt` (springs + capsule padding)
- Docs: `app/AGENTS.md` (v207), changelog FIX bullets, this Prompt.md.

## NOTE
Committed locally only — NOT pushed. The v206 splash commit is also queued unpushed (user: "dont push it" for the splash).

---

# Previous — v206: splash redesign (bigger gradient wordmark, warm tagline, bottom ground)

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
