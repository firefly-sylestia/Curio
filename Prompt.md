# Prompt — Current Request

## Request
"the 3d svg ring we added above can you make it a differentotion and remove those hole rings and ring style and just make that 3d spirng to apply even withouth the stamped pin holes option"

## Status: DONE ✅

## What changed (v209)

The 3D spiral-notebook coil ring is now always drawn on paper stat cards, regardless of whether pin holes are enabled. Previously it was tied to the "Hole rings" experiment toggle and the ring-style picker (coil/split/oblique).

### Removed
- `paperHoleRingsState` and `paperHoleRingStyleState` from AppPreferences
- `KEY_PAPER_HOLE_RINGS` and `KEY_PAPER_HOLE_RING_STYLE` prefs
- "Hole rings" experiment toggle from ExperimentsScreen
- Ring style picker dialog (coil/split/oblique) from ExperimentsScreen
- `drawSplitRing()`, `drawObliqueCoil()`, `drawPressedRim()`, `drawRingContactShadow()` from PaperStatCard
- `steelGradient`, `SplitBackDark`, `DiveMetalDark` dead color constants
- Unused imports: `AlertDialog`, `Surface`, `TextButton`, `FontWeight`, `Arrangement`, `Size`, `rotate`

### Changed
- `paperStatCardFill()` signature: removed `ringsOn` and `ringStyle` params
- With holes on: holes punched + coil threads through them
- Without holes: coil sits on the card edge as a standalone decoration
- Call sites (HomeScreen, EntryDetailScreen, ProfileScreen) simplified

### Files touched
- `PaperStatCard.kt` — core change
- `AppPreferences.kt` — removed ring prefs
- `ExperimentsScreen.kt` — removed toggle + picker
- `HomeScreen.kt`, `EntryDetailScreen.kt`, `ProfileScreen.kt` — call site updates

## Remaining work
None — this request is complete.
