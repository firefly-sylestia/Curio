# Current Request — Proper Material 3 theme system (2 new opt-in toggles)

## Status: v186 drawer/nav/footer tweaks DONE (committed + pushed to Alpha) — see below

## v186 (latest) — user: "make the home shuffle cabinet tet xt even
larger in default look and in drawer show the your constellaetion from
the your curiocity page not another thing" + "footer looks big… make it
more small".
- Extracted the Stats `CategoryConstellation` → shared
  `ui/components/CurioConstellation.kt`; Stats page + drawer both call it.
  Drawer passes passport data (explored sorted, saves = size, lastAt =
  recent, cutoff 0 = all-time). Deleted the drawer's grid-web
  `DrawerLaneConstellation` (~140 lines) + 5 unused Stats imports.
- Nav labels 13 → 15sp (default look). Footer 210 → 150dp, fade 110 → 80dp.
- Committed + pushed to Alpha (branch).

## Earlier (v185) — M3 theme system, committed + pushed to Alpha 2295d17
and 56c0986. Phases A–C done; the per-screen spacing/layout sweep + CI
validation remain.

## Request (user, paraphrased)
"Go to alpha branch, sync with main (alpha was 251 behind — done, fast-forwarded + pushed). Then: read the full M3 guidelines (m3.material.io color system + get-started + full guideline). Add 2 new toggles: (1) a proper Material theme — category colors per M3 multi-color guideline; (2) a test 'full Material guideline' — text spacing boxes layout everything. New EXTRA system as toggles, opt-in only, without changing the current app look. Make a proper plan and follow it until done."

## Clarified via ask_user (user's answers)
1. **Style picker**: "clear the current material style and fully redo it from beginning with the guidelines" — the old partial Material style is ALREADY gone (removed in v78; only stale comments remain). So: build the proper M3 Material theme from scratch as an opt-in toggle.
2. **Category colors**: "One color per family, muted" — collapse the 36 lanes into ~6 hue families, each mapped to muted M3-aligned tones from the scheme (secondary/tertiary/container roles). Lanes are not identical, but no raw per-lane hues.
3. **Toggle 2**: INDEPENDENT of the Material theme — applies M3 typography/shape/spacing/layout on the CURRENT Curio style too.
4. **Brand chrome**: "give both as an option, all opt-in, no default on" — a sub-option: Full M3 chrome (M3 NavigationBar, tonal heroes) vs Keep Curio brand chrome (floating pill nav, tear heroes, category cards stay). Both toggles OFF by default = current app unchanged.

## M3 guideline answers (from reading m3.material.io + developer.android.com M3 docs)
- Color system = 5 key colors (primary, secondary, tertiary, neutral, error) × 13-tone palettes; ~45 roles. Surfaces neutral, ONE primary for key actions, secondary/tertiary for restrained accents. Multi-color products: neutralize — the guideline for many colors is restraint, not a color per section.
- Dynamic color (Material You): derive the scheme from the wallpaper (Android 12+).
- Typography: 15-style type scale (display/headline/title/body/label × large/medium/small), Roboto defaults.
- Shapes: extraSmall 4 / small 8 / medium 12 / large 16 / extraLarge 24 (Curio currently 8/16/24/32/48).
- Elevation: tonal color overlays (surfaceContainer ladder) + shadows.

## Architecture
### Toggle 1 — "Material theme" (Settings → Appearance switch, default OFF)
- `MaterialColorSchemes.kt`: light/dark ColorScheme — dynamic (API 31+) with a seeded brand-coral baseline fallback (built from a proper M3-style tonal palette via the existing toHsl/fromHsl helpers, M3 tone→role mapping).
- `MaterialFamilies.kt`: 36 lane accents → 6 hue families (rose/red, orange/amber, gold/yellow, green/teal, blue/indigo, purple/violet); each family resolves to a muted M3-aligned color from the active scheme (secondary / tertiary / primary-container roles).
- Wire the CHOKE POINTS so every screen repaints: `CurioCategory.themedAccent()`, `categoryInk()`, `onAccent()`, `headerAccent()`, `categoryBackgroundWash()`, `categorySurface()`, `categoryChipSurface()`, `CurioGradients.cardGradient/heroBlendGradient`, `CurioMixedDeck.*`, watermark ink. When `materialThemeState` is on, these resolve through the scheme (family accents, neutral surfaces, single primary).
### Toggle 2 — "Material guidelines" (default OFF, independent of toggle 1)
- `MaterialTypography` = default M3 type scale (Roboto); `MaterialShapes` = M3 4/8/12/16/24; spacing token layer (4/8/12/16/24/32/48dp) applied to shared components; tonal elevation.
- CurioTheme switches typography/shapes when on. Works on Curio colors too.
### Chrome sub-option (when guidelines on): full M3 chrome vs keep Curio brand chrome.## Progress (branch Alpha — synced to main a127f10, then 2295d17)
- A. ✅ 3 prefs (default OFF) + Settings → Appearance rows (chrome row gated on guidelines).
- B. ✅ MaterialColorSchemes.kt (dynamic API31+ / seeded baseline via materialTone ladder), MaterialFamilies.kt (6 muted families; rose→secondary, green→tertiary; non-composable twins), choke-point wiring (CategoryInk + CurioColors gradients/mixed-deck + CurioTheme scheme swap).
- C. ✅ MaterialGuidelines.kt (M3 Typography/Shapes/CurioSpacing + gates), CurioTheme typography+shapes swap, M3 NavigationBar under full chrome, Changa One font drop from nav labels under guidelines.
- D. ✅ AGENTS.md (v185 entry + replaced stale theme-styles bullet), changelog, this Prompt.md.
- REMAINING: per-screen hardcoded padding/radius sweep (the "layout everywhere" follow-up — tokens exist, conversion is incremental); CI validation of the push; confirm the seeded baseline fallback tones on a non-dynamic device.

## Files (planned)
- app/src/main/java/com/curio/app/data/AppPreferences.kt (3 prefs + setters)
- app/src/main/java/com/curio/app/features/settings/SettingsSectionScreen.kt (Appearance rows)
- app/src/main/java/com/curio/app/ui/theme/MaterialColorSchemes.kt (NEW)
- app/src/main/java/com/curio/app/ui/theme/MaterialFamilies.kt (NEW)
- app/src/main/java/com/curio/app/ui/theme/CurioTheme.kt (scheme/typography/shape switching)
- app/src/main/java/com/curio/app/ui/theme/CategoryInk.kt (choke-point gating)
- app/src/main/java/com/curio/app/ui/theme/CurioColors.kt (gradients/mixed-deck gating)
- app/AGENTS.md, fastlane changelog, Prompt.md

## Verification
- Static only (no Gradle here): check_braces.js, git diff --check, import-hygiene greps; CI validates compile on push.
