# Request Log — notes-sheet cover colors accurate + top glow follows the palette

## Status: complete — committing & pushing (CI will validate)

## The request (user)
"the color extraction and applying in the bottom sheet of series books and
album synopsis are not very accurate and also that top style like a glow
that doesnt change color too. so can u fix it, in app"

## Root causes found (empirical, via a Palette simulation on real covers)

1. **Wash hue came from the DARK swatch.** `notesSheetPalette` keyed the
   sheet container off `darkVibrant ?: darkMuted ?: ...` — on most covers
   the dark swatch is near-grey (low saturation), and a near-grey's HSL
   hue is numerically noisy. Result: colorful covers washed out to neutral
   (e.g. On the Road's tan/grey-blue art rendered a #ECECEE grey sheet).
2. **The tint was too faint to read.** Light-mode container saturation was
   capped at 0.28 at 0.93 lightness — a whisper of cream, so the cover hue
   barely showed even when the hue was right.
3. **Near-black covers degenerated.** Covers dominated by black (Open
   Library dark covers, e.g. The Midnight Mass Murders) returned
   achromatic swatches → the sheet went grey with a pure-black accent
   (invisible on dark, muddy in light).
4. **The top "glow" never changed color.** `NotesSheetTopHairline` hardcoded
   `cat.themedAccent()` — the static category accent — so the hairline glow
   under the drag handle stayed category-colored while the cover-tinted
   sheet changed around it.

## Fix

- `data/CoverPalette.kt` — extraction: 192→256px decode, and
  `Palette.Builder(...).maximumColorCount(24)` (default 16 merges a
  cover's hues into a muddy average).
- `ui/theme/CategoryInk.kt` — `notesSheetPalette` rebuilt:
  - Accent = vibrant-family pick, kept only when it carries real hue
    (saturation ≥ 0.14); otherwise the most-saturated swatch; if ALL are
    achromatic → return null → category wash (no more grey sheets / black
    accents).
  - Accent lightness pulled into a usable band (floor 0.22→0.30, light-mode
    ceiling 0.72→0.60) so progress bars/selected rows/pills stay visible.
  - Wash hue now from the ACCENT (the cover's hue carrier), wash
    saturation raised (`(s·0.55).coerceIn(0.16, 0.42)` light /
    `≤0.45` dark), lightness 0.93→0.90 light — the cover hue actually reads.
- `features/reveal/TopicRevealScreen.kt` — `NotesSheetTopHairline` takes
  the sheet's RESOLVED accent (cover-derived, category fallback); all three
  sheets (book / album / series) pass their `accent`.

Verified with a Python Palette simulation on real book/album/series covers:
colorful covers now tint the sheet with their own hue (tan, periwinkle,
rose), black/minimal covers fall back to the category wash, and accents are
never invisible.

## Docs
- app/AGENTS.md: v363 entry (cover-color accuracy pass).
- fastlane changelog 20260921.txt: FIX bullet at the top.
- Prompt.md: this log.