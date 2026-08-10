# Prompt.md — Request Log

## Current Request (COMPLETED): Material theme adopts device colors with the category accent shine

**Date:** 2026-08-10

### What the user asked
"Fix the material theme — many things don't get the material colors — the category button etc, the peek cards — fix them."

Clarifications gathered via ask_user:
- Scope (multi-select, all chosen): **category buttons, peek cards + deck, picker category cards, spin button** — "with the category accent shine".
- The "Material card blends" experiment: **always-on in Material, remove the toggle** (experiment concluded).

### Changes made (commit `b8e3b7c`, pushed to Alpha)

**Renamed + generalized: `AmoledEdgeShine.kt` → `CategoryEdgeShine.kt`**
- `Modifier.amoledEdgeShine` → `Modifier.categoryEdgeShine(shape, accent?)` — now active in **both** AMOLED (black-glass, unchanged) and **Material** (the category accent shines at the edge as a colored rim light on the device-colored surfaces; alphas tuned up slightly since Material surfaces are mid-tone). All 8 call sites updated.
- `curioButtonColors` kept (AMOLED → black, pass-through otherwise).

**Toggle removal (experiment concluded):**
- `materialCardBlendsState` var + KEY + getter/setter + load line removed from `AppPreferences`.
- "Material card blends" row removed from `ExperimentsScreen`.
- `cardGradient` Material branch now unconditional (always device-palette blend).
- `EntryDetailScreen` hero blend gate simplified.

**Deck / peek cards now Material:**
- New shared `materialDeviceStop(accent, dark, pastel, factor)` (top-level internal in `CurioColors.kt`) — the device primaryContainer + category whisper, used by both `cardGradient` (two-stop) and `mixedDeckGradient` (multi-accent sweeps + seams), so **mixed decks no longer glow in raw category colors**.
- Peek cards carry the accent rim shine.

**Category buttons now Material:**
- New `themedButtonFill()` / `themedButtonInk()` in `CategoryInk.kt` — Material style wears the device `primary` / `onPrimary` (like the Mix button and Home hero already did); Curio/AMOLED keep the category accent.
- Applied to: `DeckControlButton`, `VerticalDeckButton` (selected state + accent rim), the Filter sheet's Apply button, `RevealStartButton`, and the **SpinButton** (tint resolves to Material primary; new `shineAccent` param keeps the category rim).
- Picker Mix button already used primary — now also gets the Material shine.

### Validation
- Brace balance OK on all 13 files (`scripts/check_braces.js`).
- No stale references: `amoledEdgeShine`, `materialCardBlends`, `materialWhisperStop` all gone (grep clean).
- Code review (deepseek-flash) passed; fixed its one actionable item — extracted the duplicated whisper math into the shared `materialDeviceStop` helper.
- Import audit: all imports still used; `categoryEdgeShine` import placed alphabetically.
- Gradle builds are CI-only per repo rules.

### Notes / follow-ups
- Worth a device check in Material: (1) deck button selected = device primary with the category rim; (2) peek/deck cards read as device palette with a faint category trace (mixed decks especially); (3) the spin button on device primary.
- AMOLED deck control pills were never part of the black pass — they keep their grey `surfaceContainerHigh` fill and now catch the white rim in AMOLED too; if you want them pitch-black like the reveal/picker buttons, that's a small follow-up.

---

## Prior requests (archive)
- AMOLED theme polish — pitch-black cards + edge shine + unified heroes (`b351b42`, `e38a6c3`).
- Theme-aware tear strip on the reveal (`ddec939`, `b74c7f5`).
- Pet-led tour completion + reveal torn bottom edge (`5464cd4`, `1265c75`).
- Pet starts at its home, redesigned speech bubble, scaled ride cloud (`51987cc`, `c38c4fd`).
