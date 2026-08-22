# Prompt — Current Request

## Request (7 parts, Android app only) — Status: COMPLETE
1. **"Material hero tears"** Appearance option (default OFF, greys out while Material theme off): torn shared heroes wear `primaryContainer`/`onPrimaryContainer` instead of rose/azure. Shared gate `materialHeroTearsOn()` in SettingsHubScreen.kt, checked first in settings/home/profile accent + readable-ink resolvers.
2. **Five Spin-visuals experiments concluded ON:** Main card shadow, Nav-style buttons, Top-lit deck cards, Tinted deck edges, Roomier deck titles — Experiments toggles + "Spin visuals" section removed; SpinScreen reads hardcoded true; pref APIs dormant with defaults flipped true.
3. **Reveal category pill** expands to its measured name width (TextMeasurer) instead of fixed 200dp.
4. **Reveal favorite pill** plays the entry expand animation when opening an already-favorited topic (`favoriteRevealed` mirrors the category pill).
5. **Cabinet nav strip gone:** Cabinet publishes its real page bg (filter wash OR adaptive-hero lane wash) so the floating capsule no longer paints plain background behind a lane-washed page.
6+7. **Progress editor corner redesigned:** shows/edits the TOTAL ("N pages"), hairline-bordered numeric field while editing, solid TICK button commits (Enter too); replay/reset removed.

## Files touched
AppPreferences.kt · ExperimentsScreen.kt · SpinScreen.kt · SettingsSectionScreen.kt · SettingsHubScreen.kt · HomeScreen.kt · ProfileScreen.kt · TopicRevealScreen.kt · CabinetScreen.kt · CurioProgressPill.kt · app/AGENTS.md · changelogs/20260920.txt

## Verification
- Delimiter-balance check passed on all 10 Kotlin files.
- Grep hygiene: no leftover `editingValue`/`valueText`/`resetValue`/`commitValueEdit`/`Replay` refs in CurioProgressPill; no `heroShadowState|peek*State|navPillButtonsState` reads left outside AppPreferences.
- Gradle builds are forbidden here; CI validates compilation on push.

## Notes / gotchas
- str_replace failed on multi-line blocks again this session (same as last) — all edits applied via assertion-guarded Python scripts under tools/, each deleted after use.
