# Current Request — Home/Recents tag pills shaded + Material theme buttons/filters use family tones

## Status: DONE (committed + pushed to Alpha) — plus a CI regression fix: the v196 tap-to-open rewrite dropped `val wide` from `CategoryPickerSheet` (CI: "Unresolved reference 'wide'" at the grid sites) — restored after `val context` in SpinScreen.kt.

## Request (user, verbatim)
"in light mode home screen the recents unplored pills make it get the color of the category it sits on with a shade and in dark mode why it looks transparent fix that, and in material theme in light mode and dark mode the category button in spin screen and filters looks bad and even worse when mixed is selected the category button"

## What changed (v198)

### 1. Tag pills (Home `ExploreTopicRow` + Recents `RecentTopicRow`)
The old fill `lerp(surfaceContainerLow, accent, 0.14f)` vanished on the tinted card in light and read transparent in dark. Now the pill pulls the accent toward the card surface:
- Light: ~30% → a solid SHADED category chip on the tinted card (the user's "color of the category it sits on, with a shade").
- Dark: ~38% → visibly tinted on the dark card (the "transparent" fix).
- Pastel light: shades with the deep same-hue ink (`category.categoryInk()`) so the airy pastel twin can't wash the pill away.
- Text stays `category.categoryInk()` (deep accent light / light twin dark / family ink under Material).

### 2. Material theme — category buttons + filters (root cause)
`MaterialFamilies.kt` v185 scheme-role branches are GONE:
- `materialAccent()` wore scheme `secondary`/`tertiary` for rose/green lanes (the baseline secondary is an AMBER companion → a Movies deck got an amber button while its card was rose-family) and a translucent `onSurfaceVariant` for neutrals. The Spin deck buttons (Categories/Filter), Spin filter-sheet chips and Cabinet/Topic-History filter chips all painted off-hue vs the family-toned cards.
- Mixed decks collapse to the scheme primary → the button re-mapped primary through the rose branch and wore secondary while the deck wore primary ("even worse when mixed").
- Fix: `materialAccent()` / `materialOnAccent()` / `materialInk()` now resolve the lane's OWN family tonal tone (T40/T80 fills, on-fill ink, T45/T80 text ink) — the exact fills `CurioGradients.cardGradient` already uses — so buttons, chips, filters and text match the deck. Pastel mode softens the fills to their pastel twins like the cards. `materialAccentFor` drops its neutral special-case tones so watermarks/blends align.

## Files
- `ui/theme/MaterialFamilies.kt` — materialAccent / materialOnAccent / materialInk / materialAccentFor.
- `features/home/HomeScreen.kt` + `features/recent/RecentScreen.kt` — tag pill fill (+ imports in Recent).

## Docs
- `app/AGENTS.md` — v198 entry + v185 bullet updated (family tones for all six families).
- `fastlane/metadata/android/en-US/changelogs/20260920.txt` — 3 FIX bullets.
- This Prompt.md.

## Verification
- Static only (no Gradle here — CI validates on push). Trace check: mixed-deck button = `materialFamilyFor(primary).fill(dark)` === `cardGradient(primary)`'s material branch → deck card and button now match; single lanes match too.
