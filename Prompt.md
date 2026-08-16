# Prompt.md — Request log

## Current request — CI cache/artifact cleanup + stop PR JAR uploads

### What was asked
"in pr my push my deployment cache is piling up for the .jar builds and
other things, can u delete the old cache and make the expiry shorter, and
also dont upload jar file in pr push from now on"

### What was found
- **Caches:** 164 GitHub Actions cache entries (~4.05 GB) on `main` —
  `gradle/actions/setup-gradle` keys the Gradle User Home cache with the
  commit SHA, so every push writes fresh `gradle-home-v2|Linux-X64|…`
  entries that linger until GitHub's 7-day no-access eviction.
- **Artifacts:** ~30 `curio-desktop-jar-*` artifacts (4.3 MB each,
  14-day retention) — one per PR/push commit; lint reports also at
  14 days; APKs already at 1 day.

### What was done
1. **Deleted old caches via API:** removed entries not accessed in the
   last day (164 → 86, ~4 GB freed). All `curio-desktop-jar-*` artifacts
   deleted; lint-report artifacts older than 3 days deleted (85).
2. **`android.yml`:** desktop JAR upload gated with
   `if: github.event_name != 'pull_request'` and retention 14 → 1 day;
   lint-report retention 14 → 1 day; added a `cache-cleanup` job (push +
   workflow_dispatch only) that deletes cache entries not accessed in 2
   days so the SHA-keyed Gradle caches stop accumulating.
3. **DOX pass:** updated `.github/AGENTS.md` (1-day artifact retention,
   cache-cleanup job, desktop CI JAR policy) and root `AGENTS.md`
   (desktop CI paragraph; also corrected stale claims that native
   Windows installers build on PR/push — they're tag-only + manual
   dispatch).

### Validation
YAML parsed clean (node `yaml`); `git diff --check` clean. Committed +
 pushed (`7ca2a2b`). No Gradle locally (env rule) — CI on push. Note:
GitHub cache retention is fixed at 7 days of no access (not configurable
per repo), so the 2-day prune job is what makes cache expiry effectively
shorter. Web app untouched.


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
7. **(follow-up, item 6)** Profile XP-progress block: the v97 paper-card
   wrapping of `ProgressAndAchievementsCard` ("XP progress" + quests +
   badge preview) is reverted — it's not a stat bar, so it always renders
   on the plain `CurioSettingsCard`. The paper style stays on the real
   stat panes (Home Streak · Cabinet · Topics, hero Level · Saved · Lanes,
   detail meta card). Dead quests-paper locals + the now-unused
   `settingsCardTintLift` import removed.
8. **(follow-up, item 7)** Pet designer hero tear is now SCROLLABLE, not
   sticky: the overlaid `SettingsHeroHeader` translates up 1:1 with the
   list via `graphicsLayer { translationY = -listState.layoutInfo
   .viewportStartOffset }` (total scrolled pixels — monotonic through the
   sticky toolbar, so no jump-back). The LazyColumn gained a
   `rememberLazyListState()`; the sticky studio toolbar now has
   `statusBarsPadding()` + theme background so it pins below the status
   bar (sticky headers pin at the viewport top — previously it pinned
   invisibly behind the opaque hero). Full-bleed tear + content padding
   unchanged.
9. **(follow-up, item 8)** Explore dialog pill label is now dynamic:
   music topics with a non-YouTube service (Apple Music / Spotify /
   YouTube Music — audio) say "Listen in"; YouTube and non-music topics
   keep "Watch in". The `MusicServiceDialog` row dropped its
   `RadioButton` (selection reads via the solid v27q fill); the dialog
   subtitle was reworded neutrally.
10. **(follow-up, mood board)** Inline small mood board: two glitch fixes.
    (a) **Frozen fit** — the v69 live re-fit recomputed the board scale
    from the current tile bounding box on every commit, so dragging a
    photo inward (or shrinking it) made the whole board zoom/resize
    ("the size changes when I move / expand / shrink photos").
    `MoodBoardCanvas` now freezes a grow-only `sessionExtentX/Y` at the
    first content's bounding box (`LaunchedEffect(tiles.size)`, reset to
    0 when the board empties); every commit clamps tiles inside it, so
    the scale never changes mid-session. (b) **Quote-card width cap** —
    the v60 display cap only bound never-resized cards; a card the user
    RESIZED kept full scale and ballooned past the small board (and
    couldn't shrink small enough). `MoodBoardFloatingCards` gained
    `rawSpace: Boolean = false`; fit-scaled views (inline editor, saved
    card, expanded dialog, export) now cap ALL cards at ≤40% of the
    canvas, while the full-screen editor passes `rawSpace = true` for
    exact raw widths. Resize-grip floor lowered 60 → 48 render px.

### Validation
`git diff --check` clean; brace counts balanced in all changed files; no
leftover bright-glass search fills or `searchGlass` refs; all 11 under-sheet
clip sites sit inside the new gate. No Gradle locally (env rule) — CI on
push. Web app untouched (Android-only ask).

## Follow-up — CI compile failure + YouTube Music link (v110)

### What was asked
"fix this also the youtube music link is not opening inside the app" —
with a CI log showing `compileReleaseKotlin`/`compileDebugKotlin` FAILED:
`PetDesignerScreen.kt:1174:33 Unresolved reference 'listState'`.

### Root causes
1. **Compile error (scope bug):** the v109 pet-designer fix declared
   `val listState = rememberLazyListState()` INSIDE the `Column { }`
   content lambda (line 606), but the hero overlay `Box` that reads
   `listState.layoutInfo.viewportStartOffset` (line 1174) is a Column
   SIBLING in the outer Box scope — a val inside a lambda is invisible to
   the sibling, so the reference was unresolved.
2. **YouTube Music opens the browser:** `buildMusicServiceSearchUrl`
   returns a plain `https://music.youtube.com/search?q=` URL; the YTM app's
   App Links verification for that domain is unreliable, so on many devices
   ACTION_VIEW hands the URL to Chrome instead of the app.

### What was done
1. `PetDesignerScreen.kt`: moved the `listState` declaration UP to the
   outer Box scope (before the Column) — both the LazyColumn (inside the
   Column) and the hero overlay (sibling) resolve it. CI should pass.
2. `ExploreSearch.kt` `openSearchUrl`: `https://music.youtube.com/` URLs
   are now package-PINNED to `com.google.android.apps.youtube.music` via
   `Intent.setPackage` — package-scoped delivery bypasses App Links
   verification, so the search lands in the YTM app. Falls back to the
   plain https intent (browser) when the app isn't installed (the pinned
   start throws `ActivityNotFoundException`). Apple Music's `music://`
   fallback logic untouched.

### Validation
Braces balanced in both files; `git diff --check` clean; the LazyColumn
still receives `state = listState` and the overlay still reads it — both in
scope now. No Gradle locally (env rule) — CI on push.

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
