# Prompt.md — Request Log

## Current Request (COMPLETED): Fix CI compile errors + reveal tear navbar footprint

**Date:** 2026-08-10

### What the user asked
"fi this and in topic reveal screen the tear doesnt have the buttom paddng or scaffhold that keep the morph animation at level at the same heigh as the navar"

1. Fix the pasted CI compile failure (`compileReleaseKotlin` / `compileDebugKotlin`).
2. Topic Reveal's torn bottom edge should span the same bottom footprint as the bottom navigation bar so the page (and the shared-element hero morph) sits at the same height as with the navbar.

### CI errors triaged
- `AmoledEdgeShine.kt:30:66` (Float vs Density) — **stale**: that file was renamed to `CategoryEdgeShine.kt` in `b8e3b7c`, which imports `AppPreferences` and uses `shape.createOutline(size, layoutDirection, density)` with correct types. No references to the old name remain.
- `CurioSettingsCard.kt:47` — **real**: used `AppPreferences.themeStyleState` / `THEME_STYLE_AMOLED` without importing it.
- `CurioTopicCard.kt:115` — **real**: same missing import.

### Changes made
- `CurioSettingsCard.kt` — added `import com.curio.app.data.AppPreferences` (alphabetically placed).
- `CurioTopicCard.kt` — added `import com.curio.app.data.AppPreferences` (alphabetically placed).
- `TopicRevealScreen.kt` — the reveal's bottom torn strip now spans the FULL navbar footprint: `height = RevealBottomTearHeight + navInset` with `.offset(y = navInset)` (from `WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()`). The torn seam stays at the same height (content bottom − 80dp), but the paper now extends through the gesture-bar inset to the physical screen bottom — exactly where the bottom bar sits on other screens, so the reveal page + morph read at the same level as with the navbar. The scroll-clearance Spacer stays valid (seam position unchanged).

### Validation
- Brace balance OK on all 3 edited files (`scripts/check_braces.js`).
- No stale `amoledEdgeShine` / `AmoledEdgeShine` references anywhere (grep clean).
- All other components that use `AppPreferences` (CurioHeroCard, CurioCategoryCard, CategoryEdgeShine) already import it.
- Code review (deepseek-flash) passed — import ordering, modifier order (`align` + `offset`), inset geometry, and compile-safety rules all verified.

### Notes / follow-ups
- Worth a device check: open a reveal on a gesture-nav device — the torn seam should sit at the navbar's top edge with paper filling to the screen bottom (no gap above the gesture bar).
- The CI log was from an intermediate state; after these fixes the current tree should compile clean.

---

## Prior requests (archive)
- Material theme adopts device colors with category accent shine (`b8e3b7c`, `c151f1d`).
- AMOLED theme polish — pitch-black cards + edge shine + unified heroes (`b351b42`, `e38a6c3`).
- Theme-aware tear strip on the reveal (`ddec939`, `b74c7f5`).
- Pet-led tour completion + reveal torn bottom edge (`5464cd4`, `1265c75`).
- Pet starts at its home, redesigned speech bubble, scaled ride cloud (`51987cc`, `c38c4fd`).
