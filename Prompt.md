# Prompt.md — Request Log

## Current Request (COMPLETED): AMOLED theme polish — pitch-black cards, edge shine, buttons, unified heroes

**Date:** 2026-08-10

### What the user asked
"Also let's fix the amoled theme — the cards look great, how about make it proper pitch black and give the corner a little of the shade line a shine, and also expand that to buttons as well. And category browser too."

Clarifications gathered via ask_user:
- "Category browser" = the **category picker grid**.
- Buttons should be **pitch black with the category-colored shine**.
- Shine = **hairline all around PLUS a brighter top-edge** ("Both").
- Bonus fix: Home/Profile hero colors should match Settings' AMOLED hero treatment (Settings' was already good).

### Changes made (commit `b351b42`, pushed to Alpha)

**New file: `app/src/main/java/com/curio/app/ui/components/AmoledEdgeShine.kt`**
- `Modifier.amoledEdgeShine(shape, accent?)` — "black glass" edge: faint 1dp hairline light border all around + brighter 1.4dp top-edge shine fading over an 18dp band (via `drawWithCache` + `clipRect`). Accent-tinted when a category color is passed; **no-op outside the AMOLED style**.
- `curioButtonColors(...)` — button containers become `Color.Black` in AMOLED (light content preserved), pass-through otherwise.

**Pitch-black card fills (AMOLED only):**
- `CurioGradients.cardGradient` — base changed from `surfaceContainerHigh` grey to pure `Color.Black` (kept the quiet category bloom).
- `CurioCategoryCard` idle — `Color.Black` + accent-tinted shine.
- `CurioTopicCard` (Cabinet grid) — `Color.Black` + shine (replaces the old grey lift).
- `CurioSettingsCard` — `Color.Black` + shine.
- `CurioHeroShuffleCard` — white shine (no accent).

**Buttons:**
- `RevealStartButton` (topic reveal CTA) — pitch-black pill in AMOLED, category accent becomes the edge shine.
- Picker "Mix" button (multi-select row) — pitch-black + shine.
- SpinButton deliberately left alone (3D sphere with its own highlight cap — flat edge shine would fight it).

**Unified hero colors (Home/Profile now match Settings):**
- `homeRoseAccent` / `profileRoseAccent` — added the same Material (`primary`) and AMOLED (`lerp(surfaceContainerHigh, primary, 0.16f)`) branches Settings already had.
- `homeReadableInk` / `profileReadableInk` — mirror `settingsReadableInk` (Material → onPrimary, AMOLED → onSurface).

### Validation
- Brace balance OK on all 10 touched files (`scripts/check_braces.js`).
- Import audit: removed now-unused `ButtonDefaults` imports from TopicRevealScreen + CategoryPickerScreen; confirmed `Color`, `RoundedCornerShape`, `lerp`, and same-package `amoledEdgeShine`/`curioButtonColors` resolution.
- Code reviewer spawn failed to return (tool hiccup) — substituted a manual compile-safety review (drawWithCache scope, Brush overloads, Shape.createOutline, modifier order, Dp math).
- Gradle builds are CI-only per repo rules.

### Notes / follow-ups
- Worth a device check in AMOLED: shine intensity (hairline 0.10–0.26 alpha, top 0.22–0.45 alpha), and that Cabinet topic cards still read as boxes now that the grey lift is gone.
- The `amoledEdgeShine` modifier is reusable for any future AMOLED surface.

### Follow-up fix (commit `ddec939`): theme-aware tear strip
- The reveal's bottom torn paper strip no longer glares in dark mode: light mode keeps the warm `CreamWhite` sheet, but dark mode uses `cat.categorySurface()` — the deep category-tinted card tone (near-black `#0A0A0A` in AMOLED), so the strip reads as dark paper matching the washed page.

---

## Prior requests (archive)
- Pet-led tour completion + reveal torn bottom edge (`5464cd4`, `1265c75`).
- Pet starts at its home, redesigned speech bubble, scaled ride cloud (`51987cc`, `c38c4fd`).
