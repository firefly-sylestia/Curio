# Prompt.md — Request log

## Current request — dark-mode polish: icons, pills, search bars, tear, detail buttons (v108)

### What was asked
A 5-part fix list (all visual, dark-mode heavy):
1. The YouTube icon is squished; the explore dialog's "Watch in" + "Explore
   browser" buttons don't match the button style in dark mode — match the
   filter chips' dark style.
2. Audit EVERY search bar's background/theme in dark mode (clarified via
   ask_user: purely the theme look, behavior untouched — "make sure every
   search bar have proper good background and theme in dark mode"); plus the
   light-colored texts "What are we exploring?", "Discoveries" and "Pick
   what you're in the mood for" read wrong in dark mode.
3. The Category + Search pills in Cabinet and Topic Browser should match the
   filter chips' pill style.
4. Remove the extra tear layer — keep only the hero's own bottom tear — but
   keep it as an opt-in option to compare.
5. The detail page's back + 3-dot buttons don't blend with the hero color.

### Root causes
1. **Squished YouTube icon:** `ic_music_youtube.xml` declares a 24×24dp box
   but a 28.57×20 viewport — VectorDrawable maps the viewport NON-uniformly
   into the bounds, so the logo drew vertically stretched (~1.43×). The
   other three brand drawables have square viewports already.
2. **Dark pill mismatch:** the explore dialog pills used
   `lerp(dialogContainer, actionInk, 0.14)` and the hero action pills used
   `lerp(backdrop, curioPillTintLift(), 0.24/0.38)` — in dark that is the
   bright white-lift glass, while the filter chips (CompactChip) wear the
   dark raised glass `lerp(surfaceContainerHigh, Black, 0.15)` + 4dp lift +
   One UI edge/glow. Two families, same screen → mismatch.
3. **Search-bar backgrounds:** all three hero search fields passed
   `fill = lerp(bannerFill, White, 0.30)`; on the dark banners / black
   sheet that lands a muddy mid-tone instead of the chip glass.
4. **Washed banner titles:** the filter sheet + category picker sheets
   resolved their hero ink from `cat.onAccent()`. With pastel colors ON
   (the default), dark mode `onAccent()` returns the tinted LIGHT twin —
   pastel title text over the deep banner. The designed dark hero ink is
   `heroHeaderInk()` (cream-white via `pastelFillInk`), which the
   Cabinet/Home heroes already use.
5. **Extra tear layer:** every torn hero (Home quest + drawer, Detail,
   Cabinet, Settings/Profile/Topic History, Spin filter + picker sheets,
   Onboarding, PromoMode) draws a white paper under-sheet
   (`SoftTornSheetShape`) below the hero's own bottom tear.
6. **Detail buttons:** `DetailStickyBar`'s dark frost was
   `lerp(heroFill, Black, 0.30)` — a near-black plate that reads as a slab
   on the hero instead of part of it.

### What was done
1. `ic_music_youtube.xml`: square 28.57×28.57 viewport with the art in a
   `<group android:translateY="4.285">` — uniform scale, ~3dp breathing
   room, never tinted.
2. Explore dialog pills (TopicRevealScreen): dark pillFill → the chip
   glass; both TextButtons gain `shadow(4dp)` + `curioDarkGlow` +
   `curioGlassEdge` + `curioInnerGlow(cat.themedAccent(), 0.12)` (no-ops in
   light). Hero action pills (`SettingsHeroActionPill`,
   `CabinetHeroActionPill`) + the settings-family back pills
   (`SettingsHeroHeader`, Topic History) swap their dark fill to the chip
   glass; light mode untouched. (Shared components, so every settings-family
   hero pill follows in dark.)
3. New shared `curioSearchFill(backdrop)` in CurioSearchField.kt: light =
   the old white lift, dark = the chip near-black glass. Applied to the
   Cabinet hero, `SettingsHeroHeader` (Topic Browser + settings hub) and
   the Spin filter sheet search fields. Default-fill fields (Settings hub /
   Topic History pages) were already near-black and stay.
4. Spin FilterSheet + CategoryPickerSheet hero inks →
   `heroHeaderInk()` (cream-white in dark; light identical to before).
5. New `heroTearSheetState` pref (default OFF) + Experiments row "Torn
   hero under-sheet"; gated all 11 under-sheet call sites (Home quest +
   drawer, EntryDetail, Spin filter + picker, Cabinet, Settings, Profile,
   Topic History, Onboarding, PromoMode). The hero's own bottom tear +
   hairline rim stay.
6. `DetailStickyBar` dark frost → `lerp(heroFill, White, 0.10)` (the same
   hero-hued lip the under-sheet wears); light frost unchanged.

### Validation
`git diff --check` clean; brace counts balanced in all changed files; no
leftover bright-glass search fills or `searchGlass` refs; all 11 under-sheet
clip sites sit inside the new gate. No Gradle locally (env rule) — CI on
push. Web app untouched (Android-only ask).

## Prior — Apple Music deep links fail for songs (v107)

### What was asked
"the apple music link works for artists and some albums but it doesnt
workfor songs".

### Root cause (verified live against the iTunes API)
TWO bugs in `resolveAppleMusicItemUrl` (ExploreSearch.kt):
1. **Dead deep-link route for songs.** `music://music.apple.com/{cc}/song/{id}`
   → HTTP 404; a song's canonical page is its ALBUM page with `?i=trackId`
   (the API's `trackViewUrl`).
2. **Query never included the artist for songs** and kept the raw `(YYYY)`
   year → zero API results.

### What was done
Rewrote `resolveAppleMusicItemUrl`: use the API's own canonical URL with the
scheme swapped to `music://` (tracking `&uo=4` stripped), term = byline +
title with trailing year stripped, `country=$storefront` passed. Non-Apple
services untouched.
