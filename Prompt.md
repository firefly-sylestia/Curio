# Current Request — Proper Material 3 theme system (2 new opt-in toggles)

## Status: curioSpacing sweep pass 1 DONE (30 call sites) — committed + pushed

## m3-layout-sweep (latest) — user: "Run the curioSpacing token sweep on
the biggest screens (Pet Designer, Entry Detail, Home, Spin) — replace
the large page paddings and gaps with the CurioSpacing tokens".
- Converted the BIG page paddings/gaps in the 4 biggest screens to
  `curioSpacing(brand, m3)`: PetDesignerScreen 13, EntryDetailScreen 10,
  HomeScreen 2, SpinScreen 5 = 30 sites (was 0). Mapping: 18/20/22/26 →
  lg (24), 28/36 → xl (32), 40 → xxl (48); 24/32 already are M3 tokens.
  Default look byte-identical; m3 args apply only under the guidelines
  toggle. Added CurioSpacing/curioSpacing imports to all 4 files.
- Remaining: step 2 `curioCorner` per-screen card/dialog shapes (PetDesigner
  77, EntryDetail 48, Home 31, Spin 29, Promo 27, Quests 24, Reveal 22,
  Profile 17), step 3 micro-value audit.

## m3-layout-sweep (earlier) — user: "go back to m3 layout branch and fix
this [CI] and suggest follow ups for remaining material things do an audit
after fix and push".
- Fixed the two CI compile errors on this branch (same ones fixed on Alpha
  earlier): re-added `kotlin.random.Random` to HomeScreen (deck pick still
  uses it) and dropped the orphan `@Composable` in StatsScreen (deletion
  splice left two stacked annotations → not repeatable).
- Audit refreshed: features layer still has 403 `RoundedCornerShape(`,
  496 `.padding(`, 469 fixed sizes, 2463 `N.dp` refs; shared chrome is
  converted (16 `curioCorner(` sites); `CurioSpacing` tokens DEFINED but
  0 call sites. Worst corner files: PetDesignerScreen (77),
  EntryDetailScreen (48), HomeScreen (31), SpinScreen (29), Promo (27),
  Quests (24), Reveal (22), Profile (17). Sweep order: (1) curioSpacing
  page paddings, (2) curioCorner card/dialog shapes, (3) micro audit.

## v187 (earlier) — full-layout M3 audit (branch `m3-layout-sweep`)
- Created the branch (from Alpha), added it to `.github/workflows/android.yml`
  triggers, pushed → CI builds it (280028b).
- AUDIT: ~1900 hardcoded layout values in app/src/main/java — 484
  RoundedCornerShape / 560 .padding / 470 .spacedBy / 394 fixed sizes.
- TOKENS: `curioCorner(curio, m3)` + `curioSpacing(brand, m3)` in
  MaterialGuidelines.kt — brand value normally, M3 token under the
  guidelines toggle.
- SWEPT the shared component layer: CurioSettingsCard, CurioCategoryCard,
  CurioCategoryChip(+Wildcard), CurioTopicCard, CurioHeroCard,
  CurioStreakPill, CurioEmptyState, CurioProgressPill, PaperCard — card
  corners now flip to M3 shape tokens under the guidelines toggle; pills
  (50dp) stay brand. Settings card inner padding converts to tokens.
- REMAINS: per-screen hardcoded paddings/radii (60+ feature files) —
  pattern established, sweep continues incrementally; CurioDialogShape
  left (24 ≈ M3 extraLarge 28).

## v186 — drawer/nav/footer tweaks (committed + pushed to Alpha 7aab245 + c03ce50 CI fix)

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
