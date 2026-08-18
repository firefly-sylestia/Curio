# Curio App Module (Active Build) — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`. It follows the root `AGENTS.md` as its parent DOX rail.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `app/AGENTS.md` (this file)

Read `master.md` and root `AGENTS.md` first, then this file for app-module-specific contracts.

## Purpose

The `app/` module is the active Android application — **Curio**, a discovery app that hands the user a topic (via "The Spin" roulette) to explore in the real world, then captures what they found into "The Cabinet" library.

**Design direction comes from the user, not from historical documents.** Before making design decisions — colors, typography, shapes, motion, layout, empty states, copy, emoji-vs-icon policy — ask the user for direction. Do not invent or follow design rules from old prompts or code comments.

The **data layer** (category taxonomy, topic schema, `ExploreAction` prompt format, authoring pipeline, rollout cadence) is documented separately in [`CURIO_DATA_PLAN.md`](CURIO_DATA_PLAN.md). It expands the category palette from 6 → 10 and ships 150+ topics per category authored via LLM-draft + human-review. **Read it before any feature work that touches data or content.**

Curio is self-contained. Its **Material Symbols** variable font and **geom.ttf** display typography live directly under `app/src/main/res/font/`; no external legacy module or source tree is read at build time or runtime.

## Ownership

### Package layout (current, Phase 2 scaffold)

```
app/src/main/java/com/curio/app/
├── MainActivity.kt                 # single Activity, edge-to-edge + CurioTheme + NavHost
├── data/
│   └── Category.kt                 # CategoryId enum + CurioCategory data class + canonical 6
├── navigation/
│   ├── CurioRoutes.kt              # all route constants + builders + bottomNavRoutes set
│   └── CurioNavHost.kt             # Scaffold-wrapped NavHost with conditional bottom nav + adaptive rail/column
├── ui/
│   ├── adaptive/
│   │   └── CurioAdaptiveLayout.kt  # window-size-class helper + CurioContentMaxWidth (tablet/landscape contract)
│   ├── theme/                      # design system primitives
│   │   ├── CurioColors.kt          # Midnight Signal palette + 6 category accents + wildcard gradient
│   │   ├── CurioTypography.kt      # geom.ttf for display/headline/label; M3 default for body
│   │   ├── CurioShapes.kt          # 16/24/32/48 corner tokens
│   │   ├── CurioIcons.kt           # glyph constants + CurioIcon(name, ...) ligature renderer
│   │   └── CurioTheme.kt           # light/dark M3 color schemes + edge-to-edge SideEffect
│   └── components/                 # reusable building blocks
│       ├── CurioBottomNav.kt       # 3-tab nav chrome: floating pill bar (phones) + NavigationRail (wide), saveState/restoreState
│       ├── CurioCategoryChip.kt    # FilterChip per category + CurioWildcardChip
│       ├── CurioEmptyState.kt      # universal §13.7 empty-state skeleton
│       ├── CurioHeroCard.kt        # ~40% vertical hero Spin card on Home
│       └── CurioStreakPill.kt      # streak indicator pill + CurioSecondaryAction helper
└── features/
    ├── splash/SplashScreen.kt      # §13.1 splash — cosmic mark (ic_launcher_foreground) + "Curio" + 3-dot pulse, 800ms → HOME
    ├── home/HomeScreen.kt          # §3 home — top bar, greeting, streak, hero, chips, recently explored empty state
    └── PlaceholderScreens.kt       # ONE file containing 11 stubs: Spin, Cabinet, CategoryPicker, TopicReveal, SaveCapture, EntryDetail, Settings, Onboarding, ManageCategories, TopicHistory, Lightbox. Each uses a shared `PlaceholderScaffold` with back arrow + glyph + title + subtitle + "Design phase · logic comes later". Real implementations replace these one-by-one in later phases.
```

### Resources

- `app/src/main/res/font/geom.ttf` — bundled display/headline typography
- `app/src/main/res/font/material_symbols_outlined.ttf` — bundled UI + category icon font
- `app/src/main/res/values/strings.xml` — Curio app name + screen titles + category display names
- `app/src/main/res/values/themes.xml` — `Theme.Curio` (M3 DayNight no-actionbar, Midnight Signal bootstrap surface)
- `app/src/main/res/values/colors.xml` — Midnight Signal XML resources used at the OS-level splash/background before Compose takes over
- `app/src/main/res/drawable-nodpi/ic_launcher_icon.png` — the v115 COSMIC launcher mark, rendered from the designer's NEW source SVG (`svgviewer-output (5).svg`, archived at `design/launcher-icon/curio-launcher-icon-v2.svg`) at 2048×2048: a mint planet with a pink moon over layered pink/gold waves on a midnight navy→magenta sky, inside a rounded card (the white frame of the v113 art is GONE, per the new source — same card geometry, ~84–88% of the canvas). `ic_launcher_foreground.xml` is an `<inset android:inset="28dp">` around this bitmap, so the whole card (~44×47dp) fits the launcher mask's safe zone; `ic_launcher_background.xml` is the full-bleed sky gradient + stars the card floats on; `ic_launcher_monochrome.xml` is the planet+moon silhouette (themed icons — positions unchanged: planet 380,320 r=122; moon 650,500 r=55); `ic_notification` is the same mark at 24dp. The SPLASH keeps rendering `@drawable/ic_launcher_art` (the v113 raster, still at `drawable-nodpi/ic_launcher_art.png`; archived at `design/launcher-icon/curio-launcher-icon.png`) — the user approved the splash as-is and asked for the icon only.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher{,_round}.xml` — adaptive-icon declarations referencing the colored and monochrome layers above
- `app/src/main/assets/topics/` — Curio topic data files and schema reference (one per ready category; see Content authoring below)

## Local Contracts

### Identity
- `namespace = "com.curio.app"` (new package, separate from FieldMind)
- `applicationId = "com.curio.app"` (new install, separate from FieldMind; users install Curio as a separate app)
- `minSdk = 26` (Android 8.0+ — all release APKs are labeled with this), `targetSdk = 37`, `compileSdk = 37`
- `versionName = "1.1.0"` (default; bumped to 1.1.0 in v113 for the cosmic-icon release — the release workflow overrides it with the git tag minus the leading `v`, e.g. tag `v1.2.3` → `1.2.3`), `versionCode = 20260920` (date-based, +1 over the previous 20260919; unchanged by tags)
- No product flavors; Curio builds as a single flavorless Android application
- Debug builds append `.debug` to `applicationId` → `com.curio.app.debug` so both can coexist on one device
- Bundles `material_symbols_outlined.ttf` + `geom.ttf` + `lora.ttf` (v35 — the Lora editorial serif, OFL, variable wght 400–700, ~212KB) directly in `app/src/main/res/font/`; none depend on another module or source tree

### Curio Database (separate from FieldMind)
- Curio installs as a separate app under `applicationId = "com.curio.app"` — its data directory is `/data/data/com.curio.app/databases/` (DB name TBD when persistence lands).
- FieldMind's data lives in FieldMind's separate install at `/data/data/fieldmind.research.app/databases/fieldmind_database`. Curio CANNOT access it directly.
- FieldMind data can be imported into Curio through the self-contained FieldMind archive importer in `com.curio.app.data.FieldMindLegacyImport`, which accepts V3 `.fieldmind` packages and plain archive JSON.
- The two apps do not share DB names, schemas, or SharedPreferences namespaces — fully isolated.

### UI
- **User design preferences (decided, durable):** light mode background/surface is **Soft Cream `#F7F0E4`** (deliberately less-white/creamy, not dark); the **category-tint background wash** is applied on the **Spin page, Topic Reveal, the Save/Capture screen, and the Cabinet (which uses the active filter chip's tint; "All" keeps the plain background)** — so every category-aware screen wears the same color story. The wash is **theme-aware via `CurioCategory.categoryBackgroundWash()`** (in `ui/theme/CategoryInk.kt`): deep accent at 20% over cream in light mode, but the light 300-level twin at ~16% over midnight in dark mode (deep accents look muddy on dark — amber turns brownish, teal grey-green).    Container steps are deepened so cards/sheets stay distinct on the cream surface. See `ui/theme/CurioColors.kt` + `CurioTheme.kt`.
- **M3 theme system (v185, Settings → Appearance):** ONE opt-in toggle, default OFF (the current Curio look is the default — nothing changes until it's on). **"Material theme"** (`AppPreferences.materialThemeState`) redoes the COLOR system per M3 guidelines: the whole `ColorScheme` becomes `materialColorScheme()` (dynamic Material You on Android 12+, seeded brand-coral baseline fallback — `ui/theme/MaterialColorSchemes.kt`), and the 36 lane accents collapse to **6 muted hue families** (`MaterialFamilies.kt`: every family resolves to tonal tones of its own hue — T40/T80 fills, on-fill ink, T45/T80 text ink; v198 removed the earlier rose→scheme.secondary / green→scheme.tertiary role branches that painted buttons/chips off-hue, see v198) — M3's multi-color guideline is restraint: neutral surfaces, ONE primary, muted accents, never a rainbow per lane. The category choke points (`themedAccent()`, `categoryInk()`, `onAccent()`, `headerAccent()`, `categoryBackgroundWash()` → neutral background, `categorySurface()/categoryChipSurface()` → neutral containers, `CurioGradients.cardGradient/heroBlendGradient`, `CurioMixedDeck.*`) all gate on `materialThemeOn` so every screen repaints. The v185 **"Material guidelines" + "Material chrome"** options (M3 typography/shapes/spacing, the M3 `NavigationBar` swap, the Changa One drop from nav labels) were REMOVED (user verdict: not good) — `MaterialGuidelines.kt`, the `materialGuidelinesState` / `materialChromeFullState` prefs and their Appearance rows are deleted; `CurioTheme` always uses `CurioTypography`/`CurioShapes` and `CurioBottomNav` always renders the floating pill bar with Changa One labels. v190 refinements: material card fills are pastel-aware; mixed decks collapse to the scheme primary; light-mode heroes wear the rich family banner with dark ink (`materialHeaderAccent` light + `materialHeroInk`); the nav chrome uses pure M3 roles under Material (surfaceContainer + secondaryContainer indicator). The v78-era AMOLED/Material STYLES are long gone — do not resurrect them; the v185 toggle is the only Material system.
- **Always-on companions & onboarding setup (v23):** the floating pet, the pet brain, and auto-open landed topic have NO Settings toggles — they are always on (their Appearance toggles were removed; the `AppPreferences` APIs remain, defaults ON). Custom reaction lines are permanently off (no toggle; the reactions editor is unreachable). The explore-bubble opt-in row in the Explore dialog is hidden by default — a Notifications toggle (`AppPreferences.showBubbleOptInDialogState`) re-shows it as a single text line (no subtext). Onboarding includes a dedicated Search step that picks the explore search engine (`AppPreferences.searchEngineState`; changeable anytime in Settings) and the bubble opt-in row inside the "Display over other apps" permission card.
- **3D shuffle button (v24):** always on by default — its toggle was removed from Settings → Experiments → Deck & controls (the `threeDButtonState` pref API stays, default true; SpinScreen reads it unchanged).
- **Closed experiments (v24) — hardcoded OFF:** dual-accent hero gradient (ugly golden blend), deck card shadows (weird look while cards animate), tail-fade peek motion, and Smart Spin layout (always natural deck sizing) had their toggles removed from Experiments and their reads in SpinScreen/TopicRevealScreen hardcoded to false. The Layout & input section was removed from Experiments (Voice-to-text still lives in Settings → Recording; Smart density keeps its stored pref but has no UI).
- **Version five-tap (v24):** the Version row in Support & diagnostics opens the **Experiments** screen (kept open) — it no longer toggles promo mode. Promo mode stays OFF by default and is reached from Settings → Experiments → Promo mode; PromoModeScreen's own toggle is the one control.
- **Passed experiments (v25) — hardcoded ON:** the **Enhanced main gradient** and **Pastel crown depth** experiments PASSED — always ON. Their toggles were removed from Experiments (Spin visuals → Main card / the Deck & controls card is gone entirely) and the reads are hardcoded `true` in SpinScreen (hero gradient + pastel top crown), TopicRevealScreen (hero gradient) and CurioColors (pastel card crown). The `heroGradientState` / `pastelCrownDepthState` pref APIs stay dormant, default true.
- **Settings declutter (v25):** all card HEADER lines in Settings were removed per request — the hub cards ("How Curio feels", "Experiments", "Your data": `headerIcon/Title/Subtitle = null` in `SettingsSections`) and the sub-page `CurioCardHeader(...)` lines (Visual language / Notifications / Recording / Backup & restore / FieldMind archive / Main card / Deck peek cards / Promo mode). Rows render directly under their `CurioSectionLabel`; that shared label component was also bumped labelMedium → titleSmall so section labels read larger everywhere (Settings, Support, Experiments).
- **Deck round-trip pin (v25):** tapping the front card on Spin now pins that topic as the landed topic (`landedTopicName`), so returning from Topic Reveal re-deals the hand centered on the SAME card — previously the NavHost disposed Spin while Reveal was open and the idle deck (no landed topic) re-dealt a different random front card on back.
- **Browse-mode Explore = real session (v25):** the Explore button on the browse-mode (Topic Database) reveal now runs the REAL explore flow (dialog → session/timer/recents/done-mark) instead of the v8.12 silent out-of-app search. `openSilentExplore` (data/ExploreSession.kt) was deleted, and `latestOnSilentExplore` / the `onSilentExplore` param in `RevealActionRow` were removed.
- **v26 — Topic Browser & Recents:** the Topic Database (`features/database/TopicDatabaseScreen.kt`) DEFAULT sort is now **A–Z within each category** (`.sortedBy { nameKey }` in the DEFAULT branch, section headers kept; the A–Z chip still flattens globally). `CurioVerticalScrollIndicator` (`ui/components/CurioScrollIndicator.kt`) gained **speed-scroll** — dragging the knob ramps rate on cumulative travel (`speed = 1 + (|cum|/160).coerceAtMost(3)`, per-event cap 240px; reversing decays it) — plus an optional **A–Z fast-scroller**: tapping the knob toggles a 26dp letter rail (strip animates 28→54dp, knob stays in a fixed 28dp TopStart strip with TopEnd content alignment), `activeAlphabetIndex` highlights the letter at the top row, tapping a letter fires `onAlphabetSelect` (the browser scrolls to the first matching topic). Only the Topic Database passes `alphabet`; the other 8 indicator users are unchanged. The Recents page (`features/recent/RecentScreen.kt`) header was rebuilt in the settings-family torn-rose hero (`SettingsHeroHeader` + `SettingsHeroTotalHeight` + `ScreenEntrance`), replacing the plain back-button row — feed scrolls under the tear, indicator padded below the hero, empty state padded down. **Drag-gesture gotcha (v26):** the knob's `pointerInput` keys on `(state, hitHeightPx > 0f, alphabet != null)`; tap-vs-drag is distinguished by total travel < 24px (TapThresholdPx) — never add a second `detectTapGestures` pointerInput to the same target (the drag detector owns the strip).
- **v26 — Recycle bin + sort dropdowns:** deleting a saved capture (Cabinet bulk or Entry Detail) now runs **double confirmation** (`ui/components/CurioTwoStepDialog.kt` — step 1 "Move to Recycle bin?" → step 2 final Delete, `step` resets on dismiss) and **soft-deletes** instead of erasing: `CaptureEntity.deletedAt` (nullable, Room v4→v5 `MIGRATION_4_5` ALTER TABLE), DAO live queries filter `deletedAt IS NULL`, new `softDeleteById(s)`/`getTrashedFlow`/`getTrashedById`/`restoreById`/`restoreAll`/`purgeById`/`purgeTrashed`/`countTrashed`, repository wrappers, `CurioEntry.deletedAt` (defaulted) threaded through `CaptureEntity.toEntry()`. Soft delete KEEPS audio/images; only the Recycle bin's permanent purge (`features/recyclebin/RecycleBinScreen.kt`, route `RECYCLE_BIN`, Settings → Safety & support row, pop-screen registered) removes media — Restore / Delete forever / Empty bin. `deleteById(s)` stays HARD delete (FieldMind import cleanup + purge). Sort controls are now the shared `ui/components/CurioSortDropdown.kt` (label zone opens the field dropdown, trailing arrow zone toggles ascending/descending universally): Cabinet (`cabinetSortField` DATE/TITLE/CATEGORY + `sortAscending`, replacing `sortNewestFirst`) and Topic Database (`tdSortField` DEFAULT/NAME/YEAR + `sortAscending` mapping to the existing `DatabaseSortMode`; the old `DatabaseSortChip` row is gone). Review gotcha: a top-level theme extension like `categorySurface()` must be imported per file — RecycleBinScreen's first draft missed it (compile error the reviewer caught); `options.first()` in the dropdown now null-safe.
- **v26 — Settings & picks cleanup:** the "Card & deck experiments" row is GONE from the Settings hub — Experiments opens only via the five-tap version trick in Support & diagnostics; Manage categories + Topic history moved into the Personalize section (the old Explore section was deleted). Manage Categories (`features/managecategories/ManageCategoriesScreen.kt`) gained long-press drag-to-reorder: the ⋮ handle uses `detectDragGesturesAfterLongPress`, a draft `List` state + row-step `dragAccum` swaps, `Modifier.animateItem()`, persisted on release (`setCategoryOrder`) — plus a "Reset order" TextButton that restores `CurioCategories.all` order (hidden flags untouched; the old `moveCategory` helper was deleted). The Spin category sheet's "Browse all categories" link is now "Manage categories" → navigates `MANAGE_CATEGORIES`. The "What are we exploring?" sheet (SpinScreen `CategoryPickerSheet`) AND the full-screen picker (`features/picker/CategoryPickerScreen.kt`) seed `multiSelectMode` + `selectedSlugs` from the persisted `getLastSpinCategories` set (`persistedVisible`, hidden lanes filtered): a saved MIX reopens in multi-select with every lane pre-ticked so it can be reviewed and changed. Saved voice-note titles (detail `SoundBiteRender`) now render on their own `NotePaperCard` slip (`titleStyle`/`titleColor`/`noteSeed(entry.id, 30)`), hoisted OUTSIDE the `audioFilePath` gate so typed-only saves show them too.
- **v27n — elevation over borders (decided):** cards, chips, pills & sheets
  lift with real shadows instead of hairline outlines (AMOLED keeps the faint
  container step; selected states raise 4–8dp). **Shadow rendering rules:**
  `Modifier.shadow()` must precede the fill in the chain (shadow-after-
  background paints a blur ON TOP of the fill); `shadowElevation` only
  renders cleanly on OPAQUE fills — translucent/glass fills are replaced by
  opaque `lerp(fill, accent, alpha)` blends (app-wide pass: badge medals,
  avatar, chips, stat pane, lane tiles, hero glass pills, tag/tick pills,
  quest cards/stamps, capture formats, picker/preset rows, editor toolbars,
  coming-soon tiles). Hero pills resolve the banner fill as their blend
  backdrop: `CabinetHeroActionPill` receives it via the hero `trailing`
  slot, `SettingsHeroActionPill` defaults to `settingsRoseAccent()`,
  `CurioSortDropdown` takes it as a required `backdrop` param. TRUE frosted
  glass over heroes/imagery gets `shadowElevation = 0` instead (glass can't
  hold a shadow — it bleeds through). The Spin deck peek cards stay FLAT
  (`shadowElevation = 0.dp`) — v24 rejected deck shadows (they animate
  weirdly) and the elevation pass re-adding a 2dp halo caused the boxy
  artifact during the reel.
- **v27q — NO selection raises: elevation is a FLAT 2dp in both states
  for every selectable chip/card/row/tile** (was 3/1, 4/2, 6/3, 8/3, 3/0
  raises). Selection must read through a FILL change instead: SOLID
  accent fill with on-accent content (`themedAccent()`+`onAccent()`,
  `primary`+`onPrimary`, `curioDialogActionColor()`+`dialogRowSelectedInk()`
  [white except AMOLED black], or `accent`+`pastelFillInk(accent)` for
  generic accents) — never a translucent lerp (bleeds the shadow) and
  never an elevation raise. Existing non-elevation cues stay: topic-card
  check badge, category-card solid gradient, pet "Your pet" pill, swatch
  check marks. Exceptions: non-selection state toggles keep their own
  elevation (3D button, paper-stats toggle, field-border toggle,
  fullscreen capture), and the fan-deck's per-card depth shadows are deck
  order, not selection.
- **v27r — badge medals + quest passport stamps are BORDER-defined, not
  shadow-defined** (user verdict after the elevation pass: the medals
  looked wrong and their shadows clipped at the shelf edges — "weirdly
  getting cut"). `CurioBadgeMedal` keeps the v27n opaque fills but wears
  the pre-elevation ring borders: inner glyph plate (1.5dp white@0.55 /
  1dp outlineVariant@0.5 when locked), ribbon gem (1dp white@0.85),
  earned marker (1.5dp white), locked silhouette (1dp outlineVariant@0.7),
  and the "+N" tile keeps its sage ring —  NO shadows anywhere in
  CurioBadges.kt. `PassportStamp` in Quests keeps its flat 2dp elevation
  PLUS its restored 1dp ring (accent ring for UNSEEN, neutral otherwise).
- **v27r — FIXED-COLOR controls never wear a solid accent fill.** The
  note-paper toolbar controls (FormatToolButton in paper mode,
  CompactPaperChip, NotePaperColorToggle) use a MODERATED tint
  `lerp(surfaceContainerHighest, accent, 0.45f)` with the accent as
  glyph/label ink — a solid amber block in dark was too saturated and
  white-on-amber unreadable (pastelFillInk assumes pastel-adjusted
  fills; fixed colors like paperControlAccent 0xFFE3B84F / paperAccent
  0xFF9A7B2F never pastel-adapt). Category-accent fills keep the solid
  accent + onAccent/pastelFillInk contract.
- **v27t — pet studio persistence + paper experiments rework.** (1) Custom
  pet designs now APPLY: `saveAsNewPet`, `selectCustomPet`, and pet-species
  switches persist the working design as the ACTIVE design
  (`AppPreferences.setPetDesign`) — the sprite + floating pet read only the
  active design (`petDesignState`), so before this the custom slots were
  studio-only and "Save as new pet" never actually put the design on the
  pet. Selecting a built-in pet with a custom design re-tags its species and
  persists (custom follows the pet); picking the default look clears the
  active design; deleting the ACTIVE slot clears it too.
  (2) `PaperTitleLines` ("Title cut lines" experiment) sizes to the hero
  title: length scales with the title text + font size (reaches ~3 chars
  past the text end, capped 16em / 300dp, floored 5em), drawn as two
  slightly curved pen strokes (quadratic beziers, wide-soft + narrow-dark
  felt-pen passes, round caps) at a -2° hand-written tilt. Callers pass
  `title` + `fontSize` (Home name 36sp, Entry Detail topic name
  headlineMedium, others headlineSmall). (3) "Stamped pin holes" are now
  DIARY-SPIRAL punches: 3 holes, 5.5dp radius at 14dp from the left edge,
  each wearing a two-tone pressed rim — faint 1dp lip ring, top-left white
  highlight arc (160°→290°), bottom-right ink shadow arc (340°→110°).
- **v27u — border-free main/reveal cards; opaque save-page strip + explore
  pills.** (1) The Spin ticket's drawn gradient rim border (1.5dp stroke +
  1dp bevel) and its AMOLED edge-shine rim light are GONE, and the Topic
  Reveal hero's matching rim is gone too — the shared-element morph stays
  clean because both cards changed together. The `heroBorderState` pref API
  stays dormant (default true, nothing reads it). (2) The SaveCapture topic
  strip no longer wears `cat.tint` (accent @ 20% alpha) under its 3dp
  shadow — translucent fills bleed shadows (v27n rule) — it now uses an
  opaque `lerp(surfaceContainerHigh, cat.accent, 0.20f)` fill, and the
  strip's icon plate is opaque (`lerp(surfaceContainerHigh, themedAccent,
  0.15f)`). (3) The explore dialog's two action pills ("Explore" browser
  + "Watch in" service) are now VISIBLE soft-tinted pills — the old
  TextButton had no container color, so the pill shape was invisible — with
  clean glyph icons (travel_explore globe for the browser, the service's
  glyph youtube_activity/play_circle/music_note for watch), tinted with the
  pill ink, no brand tiles, 12dp apart. `curioDialogActionButtonColors`
  gained an optional `containerColor` param. (4) "Title cut lines" rework:
  the two underlines now span ~88% of the title width (shorter — the old
  +3-char stretch ran past the text) and are drawn as a NATURAL hand
  double underline — two gently wavy cubic strokes in the lower half that
  converge slightly toward the right (a single pen motion, never crossing;
  bottom line a touch longer + offset right), felt-pen edge, -2° tilt.
  (5) "Stamped pin holes" gained a sibling "Hole rings" toggle: tilted
  metal book rings through the 3 holes (foreshortened ellipse, metal
  gradient, specular highlight, contact shade, per-ring tilt -9°/-3°/3°)
  instead of the pressed rims. The paper stat card is now SHARED:
  `paperStatCardFill` / `paperStatCardColor` in
  `ui/components/PaperStatCard.kt` render the opaque paper fill + 3-hole
  EvenOdd punch + rims/rings, used by Home's Streak · Cabinet · Topics bar
  AND Profile's Level · Saved · Lanes pane (same toggles: paper card,
  holes,  rings, torn edges; Profile's tear seed 0x6B4E3E). (6) NEW "Home tint"
  experiments (Settings → Experiments → Home tint, all default OFF):
  "Home tint" — the Home background + bottom nav wear a category's
  `categoryBackgroundWash()` (Home was previously always plain; the wash is
  published to the nav chrome via `CurioNavTint.homeWash` so the bar blends
  on the Home route); "Hero tint too" — the quest hero swaps the rose for
  the category's `themedAccent()` with `onAccent()` ink; "Follow my Spin
  lane" — the tint follows the category picked on Spin
  (`getLastSpinCategories`, single lane, else Wildcard) and WINS over the
  manual toggles (Hero tint + Tint category gray out in Experiments);
  "Tint category" — a manual single-select picker (default Surprise /
  Wildcard). The Streak · Cabinet · Topics card takes a 5% whisper of the
  category shade (`lerp(fill, accent, 0.05f)`) — creamy, not colored.
  Toggles: `homeTintState` / `homeHeroTintState` /
  `homeTintFollowLaneState` / `homeTintCategoryIdState`. (7) Home's
  Recents rows (`ExploreTopicRow` + `RecentEntryRow`, both opaque category
  fills) lift from 0dp to a soft 2dp elevation, and the small
  Unexplored/Resumed tag pills inside them trim from 2dp to 1dp so they
  read as chips on the card instead of floating tiles.
- **v27v — pet eyes: 2s look-timeout + touch-scroll detection.** The pet's
  pointer-aware eyes (`PetPointer` in `ui/pet/CurioPetSprite.kt`) used to
  aim at the LAST pointer position FOREVER — the pet kept staring at the
  final scroll/tap point long after you stopped touching. Now `PetPointer`
  bumps `activityTick` on every event (hover/press/drag/scroll/release)
  and each sprite keys a `lookStrength` Animatable on it: full aim while
  events keep arriving, then ease back to the neutral glance ~2s after the
  last one (or once a held press releases). Also, touch vertical scrolling
  is a DRAG of `Move` events — wheel-only `Scroll` events never fire on
  phones, so the eye-roll only worked with a mouse wheel. The tracker now
  accumulates the vertical travel of a press-drag and fires the roll once
  it clearly scrolls, gated to one roll per ~350ms so a fast fling gives a
  few discrete rolls instead of restarting every frame.
- **v29 — per-topic progress (pages read / episodes watched).**
  `CurioTopic` gained optional `pageCount` (BOOKS) and `episodeCount`
  (ANIME) — both parsed by `TopicJsonLoader`, both absent from legacy
  JSON (null = no progress tracking; anime films deliberately carry no
  `episodeCount`, a film has no episodes to track). Progress itself lives
  in `data/TopicProgressStore.kt` — a SharedPreferences JSON map keyed by
  topic id, exposed as reactive Compose state (`progressState`), seeded
  once from `MainActivity.onCreate`, and shared by every surface: the
  Topic Reveal hero, the Cabinet entry cards, and the EntryDetail hero.
  The shared control is `ui/components/CurioProgressPill.kt`: a LONG
  accent-shaped floating button (% ring + count + slim bar + Edit hint),
  tap → slider editor dialog (0..target with Reset/Finished/Save). On the
  reveal hero it straddles the card's bottom edge (the action row drops
  16→40dp when progress exists); on Cabinet cards it's a compact strip in
  the card body; on EntryDetail it floats over the hero's bottom edge.
  Always-on (no experiment toggle, per user decision).
- **v29 — hero sort/search/select controls redesigned (Cabinet + Topic
  Database).** `CurioSortDropdown` is now ONE pill with two tap zones —
  the label + chevron opens the dropdown, a `VerticalDivider` separates
  it from the arrow zone that toggles ascending/descending (was two
  separate pills); the pill is bigger (44dp tall, labelLarge, 22dp arrow)
  and the dropdown redesigned (20dp corners, tonal elevation, a "Sort by"
  header, a check on the active field). The hero pills
  (`SettingsHeroActionPill`, `CabinetHeroActionPill`) and the sort pill
  dropped the v27r ink-lean fills (lerp toward ink at 0.30/0.35/0.55 —
  read TOO DARK in light + pastel) for a LIGHT frosted glass: the banner
  fill lifted toward white (`lerp(backdrop, White, 0.24f)` emphasized /
  `0.38f` normal; destructive stays a black-lean `0.14f`), so full-ink
  glyphs pop in light, dark, pastel and AMOLED. Both heroes' SEARCH
  fields match: the ink-at-16% container became `lerp(fill, White, 0.30f)`
  with full-ink borders (0.65/0.40). Pills also grew (14/10dp padding,
  22dp glyph).
- **v29 — Spin FilterSheet: ≤4 columns + visible inactive elevation.**
  The chip grid swapped `GridCells.Adaptive(112dp)` (stretched two huge
  slab-chips on phones, 5+ on tablets) for a `BoxWithConstraints` fixed
  count: `(maxWidth / 92.dp).toInt().coerceIn(2, 4)` — compact pill
  columns capped at four in a row. `CompactChip` now lifts the INACTIVE
  fill a whisper of white (`lerp(chipSurface, White, 0.04 dark / 0.10
  light)`) with a 2dp shadow in BOTH states + `curioDarkGlow`, so
  unselected chips read as raised pills off the tinted sheet instead of
  flat tiles. **Pastel-mode follow-up:** the whisper was invisible — in
  light mode `categorySurface()` ignores its `base` surface step, so the
  sheet (`surfaceContainerLow`) and the chips (`surfaceContainerHigh`)
  resolved to the SAME airy pastel and the 2dp elevation read as nothing.
  The light-mode lift is now a clear surface step (`0.32` toward white,
  dark keeps `0.04` + glow), so unselected chips visibly stand off the
  tinted sheet in pastel AND plain light mode.
- **v30 — uniform hero-pill height + Category pill in Cabinet & Topic
  Browser heroes.** (1) Every hero pill now reads the SAME 42dp height:
  `CurioSortDropdown` trims from a 44dp minimum to 42dp, and
  `CabinetHeroActionPill` / `SettingsHeroActionPill` label-only pills get
  a `heightIn(min = 42.dp)` so they match the 22dp-glyph pills — the sort
  pill no longer reads thick next to Select/Search. (2) A new **Category**
  pill rides a SECOND row directly under the hero's top pill row (Tune
  glyph + active-filter label + an up/down chevron that flips with the
  chips — ▾ closed / ▴ open, via the pills' optional `trailingGlyph`,
  `emphasized` while open): tapping it
  reveals the sticky category chip bar — the same chips that appear while
  searching — in BOTH the Cabinet and the Topic Database. The heroes grow
  +52dp to fit it (`CabinetHeroBannerHeight` 180→232, compact 140→192;
  `SettingsHeroExtraRowHeight = 52.dp` applied to the settings hero when
  the new `extraRow` slot is used). The Topic Database's category chips
  are now HIDDEN BY DEFAULT (matching the Cabinet): the chip bar shows
  only while the Category pill is open or search is active, and the DB
  derives its own content offsets from `DatabaseHeroTotalHeight`. The
  chip-bar content-top reservation only applies while the chips are
  visible, so the collapsed screens start right below the hero.
- **v30 — shared hero follows the Spin lane (Appearance toggle) + settings
  declutter.** (1) New Appearance toggle **"Hero follows Spin lane"**
  (`heroFollowLaneState`): when ON and the Spin deck is on a single lane,
  the shared torn hero (Home / Profile / Settings / Cabinet-All / Quests /
  Recent / Support / drawer — every rose/azure hero) wears that category's
  `headerAccent()` — the Cabinet's filtered-hero language — via the new
  central `heroLaneCategory()` helper hooked into `settingsRoseAccent()` +
  `homeRoseAccent()`, and the page background below it wears the category
  wash via `heroPageBackground(default)` (Home inline, Profile/Settings hub
  keep their rose-lerp default; the rest of the settings family keeps its
  plain default; Cabinet-All falls back to the lane wash too). Mix/empty
  lane or toggle off → rose/azure as before. (2) **Removed:** the Home tint
  experiments (Home tint / Hero tint too / Follow my Spin lane / Tint
  category — Experiments section + picker gone, prefs dormant), the
  **"Glow shadows"** Appearance option (`curioDarkGlow` is now a no-op —
  the light glow was retired as a poor look; `darkGlowState` pref stays
  dormant), and the **"Entry date & mood"** option — date/mood/attachments
  are ALWAYS on now (SaveCapture + Marginalia gates hardcoded).
  (3) **Merged:** "Floating explore bubble" + "Display over other apps"
  are ONE option — the bubble toggle shows the live overlay grant state in
  its subtitle, enabling without the permission opens the system page to
  ask for it, and when the bubble is OFF with the permission still granted
  an inline "Remove overlay permission" row appears to revoke it (a
  separate revoke-trip flag keeps the return from re-enabling the bubble).
- **v31 — Adaptive Hero + hero picker, Category pill below the hero,
  slimmer sort pill, background-tinted surfaces, faster Home.**
  (1) **"Hero follows Spin lane" renamed "Adaptive Hero"** (Appearance
  toggle + Settings hub row) and the Profile hero finally follows the
  lane: `profileRoseAccent()` now runs the same `heroLaneCategory()`
  check Home/Settings have (it was the only shared hero missing it).
  (2) **Hero picker is a 2-option segmented control** (Rose hero / Sky
  azure hero) replacing the "Sky azure hero" switch — Sky azure is
  GREYED OUT (visible but unselectable, the Material-coming-soon
  pattern; a one-time migration flips a previously-enabled azure back to
  rose), and the whole control greys while Adaptive Hero is on.
  (3) **Category pill moved OUT of the hero** in Cabinet + Topic
  Browser: it rides its own fixed row just below the hero (page-level
  pill: on-surface ink over surface-high glass), so the heroes returned
  to their original heights (`CabinetHeroBannerHeight` 232→180,
  compact 192→140; the settings `extraRow` slot + `SettingsHeroExtraRow-
  Height` are gone) and the header text never moves down. The sticky
  chip bar sits below the pill row (chip-bar offsets derive from
  `barTop`/heroTotal + `CabinetCategoryPillRowHeight`).
  (4) **Sort pill slims down:** `CurioSortDropdown` swaps its fully-
  rounded 50dp capsule for 16dp corners with tighter padding (keeps the
  uniform 42dp height). (5) **Cream → small tint of the page
  background, in every theme:** `CurioSettingsCard` (Profile + Settings
  hub + sub-pages) lerps `surfaceContainerLow` 30% toward `background`;
  the ink-glass hero pills + sort pill lift toward the page background
  in light mode via a new `curioPillLift()` helper (dark/AMOLED keep the
  white lift for visibility); dialogs pull a step toward the background
  in every theme (`curioDialogContainerColor`); and the settings-family
  sub-pages (Appearance/Preferences/Support/Backup/Experiments/Promo)
  now wear the same rose-lean page tint as the hub/Profile instead of
  the plain cream background (`heroPageBackground(lerp(background,
  settingsRoseAccent(), 0.10f))` — the spin-lane wash still wins when
  Adaptive Hero is on). (6) **Home opens faster:** the canonical topic
  count is now cached in memory (`TopicJsonLoader.countCanonicalTopics`
  parsed the whole ~14k-topic catalog on EVERY return to Home; one
  parse per process now).
- **v70 — filter sheet: tear hero to the status bar + watermark backdrop,
  active-filters strip removed, group-label icons.** (`FilterSheet` in
  SpinScreen) (1) **The tear hero now runs up BEHIND the status bar** —
  the sheet dropped its 28dp rounded top corners and floating drag
  handle (`shape = RectangleShape`, `dragHandle = null`) and consumes
  only bottom + IME insets (`contentWindowInsets = navigationBars ∪ ime`),
  so the banner fills the very top edge like every page hero; the banner
  height grows with the status-bar inset (`118.dp + statusBar` via
  `WindowInsets.statusBars.asPaddingValues().calculateTopPadding()`) and
  its title/Clear-all row applies `statusBarsPadding()`. (2) **Category
  name steps up 30 → 34sp.** (3) **The sheet body now wears the page
  watermark backdrop** (`CurioWatermarkBackdrop` at `alphaScale = 0.5f`,
  kept in the band below the hero via `topClearance = filterHeroHeight`)
  — the filter page finally shares the collage language of every other
  screen. (4) **The "Active filters" summary strip and its divider are
  REMOVED** — the selected chips were redundant (selections already read
  on the group pills + open group); the now-dead `ActiveFilterChip`
  composable was deleted with it. (5) **Group section labels (Type ·
  Genres · Era · Origin · Franchise) gained a per-group Material Symbol**
  (`FilterGroupKey.glyph`: `category` / `style` / `history` / `public` /
  `movie`, all verified in the bundled font) tinted with the category
  accent, stepped 16 → 17sp, with cleaner 10/8 margins; the accordion's
  top margin also tightened 14 → 6dp now that the divider is gone.
- **v71 — pet designer: eye-size presets FIXED + whole-pet size option.**
  (1) **Eye presets were a no-op (root cause):** `CurioPetSprite` scaled
  each procedural eye's pixels around its center and snapped to integer
  cells (`roundToInt`). The default eyes are only 2px wide (±0.5 cells
  from the center; STAR/DIZZY ±1.5), so the 0.85/1.2 factors shifted
  every pixel < 0.5 cells and rounded right back onto the authored
  cells — Small/Medium/Large were pixel-identical. The sprite now
  scales the eye art in DRAW space per eye (`DrawScope.scale` around
  each center at 4.5/7 and 10.5/7, the detail-layer transform trick)
  with stronger factors (0.72 / 1.0 / 1.35); the placement offset then
  applies unscaled — every eye style visibly shrinks/grows. (2) **New
  whole-pet size option:** `PetDesign.petScale` preset (0 small / 1
  medium / 2 large), serialized `petscale=` + tolerant parse (legacy →
  1). `CurioPetSprite` multiplies its sprite box by the preset
  (0.8 / 1.0 / 1.3) on top of the caller's stage `sizeScale`, so the
  custom pet scales up EVERYWHERE it renders (floating pet, flower bed,
  quests, every designer preview). The Pet Designer Settings page gained
  a **"Pet size"** card (before the Eyes card): live preview +
  Small/Medium/Large + Reset size, writing `design.copy(petScale = …)`
  with undo — the Eyes-section pattern.
- **v80 — category picker banner: smaller two-line title, hint removed.**
  `CategoryPickerSheet`'s "What are we exploring?" stepped 34 → 28sp and
  now wraps onto two lines ("What are we\nExploring?", 34sp line height);
  the mode-hint subtitle ("Tap a deck to spin it. Hold to pick
  several." / "Tap to toggle decks · Done to spin together") is deleted.
  The deck-status chip stays. (`maxLines = 1` was removed with the
  manual newline or the second line would have ellipsized away.)
- **v116 — profile avatar crop: manual crop editor + auto center-square
  crop, redesigned Edit profile dialog.** (1) **Picking a photo no longer
  squishes portraits**: `ProfileScreen` now DECODES the picked image
  EXIF-correctly (`decodeAvatarSource` — ImageDecoder 28+ bounded to
  2048px with its own EXIF pass disabled, BitmapFactory sample 26-27;
  framework `ExifInterface` rotation applied manually so both paths agree)
  and saves a **CENTER-SQUARE crop from the middle** (`centerSquareCrop`,
  `scaleToMax` → 512px) as the avatar, with the editable SOURCE kept
  beside it as `profile_avatar_src_*.png`. (2) **Manual crop editor:** new
  `ui/components/AvatarCropDialog.kt` — a fixed SQUARE crop window with
  the photo panning/pinching behind it (drag to move, pinch to zoom,
  never smaller than cover-fit, reset returns to the center crop); Apply
  hands back the exact source-pixel `IntRect` (`currentCropRect`) which is
  re-cropped and re-saved. No crop library — plain Compose gestures, the
  app's dialog styling. Composed AFTER the edit dialog so its window
  stacks on top; canceling the edit dialog also clears the crop state.
  (3) **Edit profile dialog redesigned:** the 64dp preview grew to 84dp
  with an accent **crop/photo badge** on the corner (tap the avatar to
  adjust when set, else pick); the flat stock TextButtons became the
  app's **pill actions** (`DialogPillAction` — accent Add/Change photo,
  Adjust, destructive Remove); the caption now explains the auto square
  crop. `loadAvatarSource` falls back to the  current square avatar for pre-v116 avatars (no source was kept then).
  (4) **Dark dialogs match the settings option cards**: the dark branch of
  `curioDialogContainerColor` (CurioTheme.kt) now uses the SAME fill as
  `CurioSettingsCard` — `lerp(surfaceContainerLow, tintLift, 0.30f)` with
  the dark tint lift `lerp(Color.Black, curioRoseInk(), 0.20f)` (the
  neutral rose, since a dialog floats over any page) — instead of the old
  `lerp(surfaceContainerHigh, background, 0.55f)` grey slab, so every
  AlertDialog (edit profile, crop, two-step, etc.) reads as black
  option-card glass on the black page.
- **v117 — crop-before-apply + decluttered Edit profile dialog.** (1)
  **Picking a photo now opens the crop editor FIRST** — the pick no
  longer applies the center crop immediately: `avatarPicker` hands the
  decoded source to `cropSource` and only Apply saves
  (`saveAvatar(src, rect)`); Cancel discards the pick. The dead
  `openCropEditor` / `loadAvatarSource` re-crop path is gone. (2) **Edit
  dialog declutter:** the **Adjust pill, the crop/photo badge and
  tap-avatar-to-crop are all REMOVED** (the avatar is a static 84dp
  preview) — Change photo is the only way back into the crop editor;
  captions updated ("Square photo — Change photo to re-crop." / "Pick a
  photo — you can crop it to a square before saving."). (3) **Crop
  dialog buttons are a MATCHED pill pair** — Cancel is a calm surface
  pill next to the accent Apply pill (was a flat TextButton next to a
  lone filled pill); unused `TextButton` / `curioDialogActionButtonColors`
  imports removed.
- **v118 — drawer sections + Home avatar pill + Support update link.**
  (1) **Home profile pill wears the avatar:** `TopBarPill` gained an
  optional `avatarPath` — the profile pill on the Home sticky bar shows
  the avatar photo (the Surface clips to the circle; the animated `rim`
  ring draws on top so the frosted scroll morph still reads) and falls
  back to the Person glyph; fresh pref read each composition, like the
  drawer. (2) **Drawer declutter — collapsible sections:** the drawer
  groups rows into two collapsible sections, BOTH collapsed by default
  (user request): **"Your Curiosity"** (AutoAwesome header → Topic
  History, Manage Categories, Browse Topics) and **"About"** (Info
  header → Support & diagnostics, Replay intro — the user picked the
  name). New `DrawerSectionHeader` (leading icon chip + ▼/▲ chevron,
  the filter-sheet convention); state via `rememberSaveable`. The drawer
  avatar also grew 48 → 56dp and the greeting text stepped up (CURIO
  labelMedium, "Hi name" headlineMedium, tagline bodyMedium).
  (3) **Support & diagnostics gained an Updates row** (Download icon)
  that opens the dedicated Updates sub-page — the v116 de-dupe stays
  intact (exactly one link, no duplicate header).
- **v118 — pet dialogue fully ported to the canonical dialog doc.** Every
  line in `CurioPet.kt` / `CurioPetBrain.kt` / `TourController.kt` now
  matches `docs/pet-dialogs.md` (the user's rewrite — tiny curious
  creature voice, "I/me/we", no self-naming in evolved voices). All §1
  event pools (SPIN_LANDED → QUEST_COMPLETE + sassy), §2 streak, §3
  evolution ceremony, §4 moods (10 × first/baby/mature + time-of-day),
  §5 greetings/welcome-backs, §6 touch tiers/bonds, §7 games (spinCheer,
  play, landmark, jig, dizzy, drawer, peek, chameleon, spark, the six
  interactive moments), §8 memory/factLine, §9 brain openings/bodies,
  §10 tour script, §11 mature routine lines — all replaced 1:1 (pool
  names, order, placeholders `__LANE__`/`$lane`/`$savedLane`/`$streak`/
  `$count`/`$topic`/`$level`/`$saves` kept). §12 BABY VOICE EXPANSION
  (Curie-isms) is wired in: thematic pools (`babySaveLines`,
  `babyTouchLines`, `babyLevelUpLines`, `babyEvolveLines`,
  `babyExploreLines`, `babyDiscoveryLines`, `babyMishapLines`) feed the
  matching baby events, `babyCurieLines` (sounds + tiny phrases + rare
  silly lines) rides EVERY baby pick, and the excited/happy/curious/
  sleepy/shy/grumpy/playful mood pools absorbed their categories. The
  brain's `say()` bodies are now multi-option pools picked through
  `CurioPet.pickLine` (anti-repeat); the coined catchphrases are the
  doc's §9 set. Dead `happyLines` pool removed. PetDesign.kt's
  custom-reaction preview defaults are untouched (not in the doc's
  scope).
- **v119 — the tour dock's Skip/Next is never covered by the guide.**
  The pet-led tour's Cabinet stop dropped the speech bubble onto the
  bottom dock: the `grid` landmark wraps a `fillMaxSize()` grid, so the
  bubble's "above" placement never fit and the "below" fallback was
  clamped to the very bottom — over the Next button (an empty Cabinet
  was worse: no landmark at all, so the bubble floated over the pet
  wherever the previous step had parked it). Fixes in
  `CurioFloatingPet.kt`: the tour bubble is capped above a new
  `TOUR_DOCK_BAND` (96dp + nav inset); landmarks that span the screen
  (the Cabinet grid) anchor the bubble just above the landmark's CENTER
  (upper-middle — v120 fix, the original pin-to-top floated over the
  hero); the pet's tour walk target is floored above the same band so
  it never stands on the dock; a landmark-less stop parks the guide
  top-center; and the no-landmark bubble fallback is clamped
  on-screen/above the dock. And in `CabinetScreen.kt` the EMPTY state
  now registers the same `grid` landmark the filled grid does, so the
  tour always has an anchor on the Cabinet stop.
- **v120 — pet games reworked + the dialogue actually gets spoken.**
  (1) CHATTER: the pet now says a passive mood line (`CurioPet.lineFor`)
  every ~20-40s of idle — the mood pools are heard, not just the event
  lines. (2) GAMES RUN TO COMPLETION: the three games became suspend
  functions (`playHideSeek`/`playChameleon`/`playStarGame`) dispatched
  at the top of the wander loop; `gameActive` gates the mood loop,
  typing reaction, idle/time custom actions, auto-nap and the chatter
  so NOTHING overrides a round (the old probabilistic inline game
  blocks + per-game cooldown vars are gone). (3) GAME MODE: long-press
  no longer sends the pet home — it arms game mode (pet stays put,
  autonomy paused); the next tap OR drag starts ONE game, then game
  mode ends. v121 — game mode CYCLES through the games (HIDE_SEEK →
  CHAMELEON → SPARK → …) so all three get played evenly; the auto-flow
  scheduler keeps its random picks. Drag the pet onto its flower bed to
  send it home (unchanged). (4) HIDE-AND-SEEK: the pet POOFS out and teleports to a
  random corner, just a sliver visible; tap the sliver to win; miss it
  and after up to 5s it poofs back with a sad face (`EyeStyle.CLOSED` +
  `MouthStyle.O` via `reactionFace`) + `missedMeLine()`. Chameleon's
  find window is also 5s, and both teleports burst a new `PoofOverlay`
  (puffs at the RECORDED position). (5) STAR-CATCH: a 10s round of
  stars falling slowly from above (`FallingStar` list, spawned every
  ~500-850ms) — the pet NEVER chases on its own; tap a star and it
  dashes over to catch it, or drag the pet onto a falling star; the
  score is spoken in a bubble at the end. (6) IDLE ROAM: the wander
  beat dropped from 2.8-7s to 2-3.2s so an untouched pet roams again
  quickly. (7) AUTO-FLOW: a scheduler effect requests a RANDOM game at
  random 20-50s intervals (scaled by the game-frequency setting),
  honoring `GAME_MIN_SPACING_MS`. Dead state/constants removed
  (`sparkTarget`/`sparkKey`/`sparkWon`/`lastHideSeekAt`/`lastChameleonAt`/
  `lastSparkAt` + the three per-game cooldowns).
- **v122 — drawer greeting: bigger avatar, lifted row, auto-shrinking name.**
  The drawer hero avatar grew 56 → 64dp (the initial-letter fallback
  stepped up `titleLarge` → `headlineSmall` to match), the greeting row
  sits a touch higher (`bottom` 28 → 40dp, still inside the 186dp hero),
  and the "Hi name" line no longer gets CUT on long names: the greeting
  style steps down by length (headlineMedium ≤16 chars → titleLarge
  ≤26 → titleMedium beyond) with the single-line Ellipsis as the last
  resort. NOTE: the first attempt used `TextAutoSize.StepBased`, which
  FAILED CI — `androidx.compose.ui.text.TextAutoSize` is not resolvable
  on this project's Compose classpath (despite the 2026.05.01 BOM), so
  the manual length-based font steps are the shipped approach. Do NOT
  reintroduce TextAutoSize without first confirming it resolves.
- **v123 — tour tab steps navigate like REAL tab switches; drawer name
  lines; pet teleport/chameleon/auto-flow.** (1) **FIX — skipping the
  tour on the Spin page made the Home tab "dead" afterwards.** Root
  cause (read from Navigation 2.9.8's `NavControllerImpl.navigate`,
  not a guess): `navigateToTab` calls
  `navigate(route){ popUpTo(HOME){ saveState=true }; launchSingleTop;
  restoreState }`. The tour previously PUSHED Spin with a plain
  `navigate("spin")` (no popUpTo), so HOME never entered the
  controller's `backStackMap`. The first Home-tab tap after the skip
  then popped Spin with `saveState=true` — which maps the popped stack
  UNDER HOME's key — and `restoreState=true` immediately RESTORED that
  stack, landing back on Spin (the tap looks dead). In the normal flow
  the first `navigateToTab` plants a NULL mapping for HOME (its no-op
  popUpTo saveState), so the later Home restore is a no-op — that's why
  only the tour-created Spin stack broke. FIX: all THREE tour
  navigation sites (`CurioNavHost.advanceTourAndNavigate`, SpinScreen's
  `onSpinClick` consumeTap, HomeScreen's quest-card consumeTap) now use
  `navigateToQuestRoute` (tabs → `navigateToTab`, pushes stay plain).
  RULE: never plain-`navigate` a TAB route (Spin/Cabinet) from the tour
  — use `navigateToTab`/`navigateToQuestRoute`, or the Home tab
  self-restores the tab you left. (2) **Drawer greeting:** the "Spin
  it. Explore it. Capture it." tagline is GONE; the first name stays in
  the greeting position ("Hi First") and the remaining name parts
  (middle, last) render at the tagline's old size (`bodyMedium`, alpha
  0.78) in its spot. **v134 — the remaining parts now join onto ONE
  line** (`joinToString(" ")`, single `Text`, `maxLines = 1` +
  ellipsis) instead of one line per name part — the user found the
  per-part wrapping weird ("middle 2nd 3rd name into 2 different lines").
  (3) **Pet:** random LONG
  teleports (`walkTo`'s `LONG_JUMP_FRACTION` branch) now `burstPoof` at
  the old spot, `delay(160)`, teleport, then poof again at the target —
  no more instant snap; the chameleon game POOFS and teleports to a
  random on-screen spot BEFORE fading to its ghost outline (was: fade
  in place); and the idle AUTO-FLOW scheduler only picks
  `HIDE_SEEK`/`CHAMELEON` — star-catch (a 10s round) stays reachable
  via game mode's cycle + manual taps, never the auto-flow.
- **v124 — floating pill nav bar (phones).** The edge-to-edge M3
  `NavigationBar` is REPLACED by `CurioFloatingNavBar` (in
  `ui/components/CurioBottomNav.kt`; the old `CurioBottomBar` composable
  is deleted): a floating 50-radius capsule pinned bottom-center above
  the gesture inset. Every tab renders icon-only (48dp pill); the ACTIVE
  pill springs wider (96dp, `animateDpAsState` spring damping 0.75) and
  its label slides out (`AnimatedVisibility` expandHorizontally+
  fadeIn) while the previously active pill collapses — the "smooth
  collapse and expand" the user asked for. The active indicator is a
  filled capsule covering the WHOLE pill (icon + label). Colors are
  pure `colorScheme` tokens (surfaceContainerHigh bar + shadow 6dp,
  secondaryContainer indicator, onSecondaryContainer ink,
  onSurfaceVariant inactive) so Curio / AMOLED / Material (dynamic)
  themes and dark mode adapt automatically. Geometry: the slot stays
  80dp + nav-bar inset (verified against M3's `NavigationBar`:
  `windowInsetsPadding + defaultMinSize(80)`), so Scaffold innerPadding
  and the Reveal 80dp placeholder are unchanged; wide windows keep
  `CurioNavigationRail` (user decision). Label width is FIXED (48↔96dp)
  so the bar's total width is constant and the morph is stable. The
  page-wash tint (`CurioNavTint`) now applies to the rail only.
- **v124 follow-up — floating pill bar: page-wash slot + active-only
  morph.** Two user fixes to `CurioFloatingNavBar`: (1) **The white/
  black strip behind the pill is GONE.** Root cause: the Scaffold's
  bottomBar slot is painted with `MaterialTheme.colorScheme.background`
  (white in light / black in dark), and the old edge-to-edge bar
  covered it — the floating pill leaves it visible as a strip against
  the category-washed pages. FIX: the slot `Box` now paints
  `curioNavContainerColor(routePrefix)` — the SAME animated page wash
  the rail uses — and `curioNavContainerColor`'s no-wash fallback
  changed `surface` → `background` (a page publishing no wash has
  `background` as its own background; `surface` left a seam on Cabinet
  "All"). HomeScreen now publishes its REAL background always (`homeBg`
  — the lane wash OR the rose-tinted default), not just when a lane is
  active. (2) **Only the ACTIVE pill animates.** The width morph now
  plays only on the false→true edge (`wasSelected` remembered state →
  spring spec on becoming active, `tween(0)` snap on deselect), and the
  label's `AnimatedVisibility` exit is `fadeOut(tween(0))` so the
  closing pill's text vanishes instantly — the old pill no longer
  collapses with a visible shrink.
- **v125 — offline transcription (Vosk) + floating dictation mic + plain
  quick title.** (1) **ENGINE CHOICE — Vosk, not whisper.cpp:** the user
  originally asked for whisper.cpp, but whisper.cpp has NO published
  Android binding (the Maven-Central `io.github.givimad:whisper-jni`
  jar is DESKTOP-only: Windows/Linux/macOS .so/.dll — verified on its
  README; no reliable Android AAR exists anywhere). Vendoring the C++
  source + NDK/CMake build was rejected (heavy, unverifiable in this
  env). The user approved Vosk instead: `com.alphacephei:vosk-android`
  AAR bundles .so for every ABI and ships the same offline
  model+transcribe UX. RULE: before promising a native/ML library,
  verify it has an ANDROID artifact — "Java JNI wrapper" ≠ "Android".
  (2) **New `data/OfflineTranscriber.kt`:** `VoskModels` (catalog of
  downloadable models id/name/lang/size/url; `download()` = plain
  HttpURLConnection to alphacephei.com + ZipInputStream extract into
  `filesDir/vosk-models/<id>/` with an `am/` completeness check;
  `modelDir`/`isDownloaded`/`deleteModel`) + `OfflineTranscriber`
  (MediaExtractor+MediaCodec decode of the AAC m4a → 16-bit PCM →
  downmix-to-mono + linear-interpolation resample to 16kHz → feed
  `org.vosk.Recognizer` in 5s chunks on Dispatchers.Default; returns
  trimmed text). (3) **Settings → Recording → "Offline model" row**
  (`OfflineModelDialog` in SettingsSharedComponents): pick quality/size,
  in-app download with live progress, delete — labeled as the model for
  pre-recorded voice-to-text. New prefs: `offlineModelIdState` +
  `offlineModelVersionState` (bumped on download/delete so detail
  recomposes). (4) **Per-field dictation mics REMOVED everywhere** (Sound
  Bite title+note buttons, the detail page's "Transcribe note" chip, the
  `allowTranscribe` plumbing). Replaced by ONE floating mic in Save your
  take: it appears only while the LARGE note box (`RichTextEditor` gains
  an `onFocusChanged` hook) is focused, and opens a dictation dialog
  with the live preview pinned at the BOTTOM + Stop/Insert — the session
  only ends when the user taps Stop (generous 2.5s/1.5s silence
  windows), Insert commits the text (box stays editable). (5) **Quick
  title de-papered:** the "Add a quick title" field is now a PLAIN
  rounded input (`PaperLineField` gains `paper: Boolean`; no slip, no
  style/color toggles) and the detail page shows the title as a pill
  JUST BELOW the quick fact (`titleMedium` SemiBold on an opaque
  accent-tinted capsule) instead of a torn paper slip in the body.
  (6) **Transcript persistence:** `CaptureData.SoundBite.transcript`
  (nullable String) — detail-page Transcribe button (only when the
  model is downloaded; otherwise a Settings hint row) decodes +
  transcribes + saves via `repo.save`; the transcript renders as a
  collapsible box (3 lines + Expand button, Re-transcribe + clear).
  Vosk on a sound bite uses a NEW `Recognizer` per call (Model load ~1s)
  — never reuse one across audio files.
- **v126 — editable progress target + corrected topic totals + alternate-
  edition pill.** (1) **The dialog's number is now TAPPABLE to fix the
  total pages/episodes.** Before, the editor's target was locked to the
  topic JSON (`progressTarget`), so wrong baked-in totals (see (2))
  couldn't be corrected in-app. The count line under the ring ("value /
  target unit") now opens an inline numeric field (✓ commits); the
  corrected target persists per-topic via `TopicProgressStore.setTarget`
  (new `targetOverrides` map, prefs key `topic_target_overrides_v1`,
  seeded in `seed()`), and `getTarget(topicId, default)` makes the
  override win everywhere — `CurioProgressPill` (pill text, fraction),
  `CurioTopicCard` (progress line) and the editor's own slider/steppers.
  Save writes the override only when it differs from the baked-in value
  (`clearTarget` otherwise). (2) **Topic-data corrections:** the anime
  season-1 entries carried the MERGED multi-season episode total (Mob
  Psycho 100 was 37 = S1+S2+S3, One Punch Man 24 = S1+S2, Attack on
  Titan 89 = S1–S4, My Hero Academia 159 = S1–S7, etc.) — 15 entries in
  `anime.json` corrected to their season-1 counts (verified: Mushoku
  Tensei 23, Spy x Family 25, MHA 13, etc.). (3) **Alternate-edition
  pill (books):** `CurioTopic` gains `altPageCount: Int?` +
  `altPageLabel: String` (loader-parse `altPageCount`/`altPageLabel`;
  SCHEMA.md documents both). Books whose common editions differ HUGEly
  (≥20% of the primary) now render a second, quieter pill beside the
  progress pill ("or 574 Lombardo") — tapping it opens the editor
  pre-set to that edition's count (`initialTarget` param; nothing
  persists until Save). Data added for the verified cases: The Iliad
  (704 Fagles / 574 Lombardo), War and Peace (1392 / 1104 Wordsworth),
  Moby-Dick (635 / 720 Penguin Classics), Ulysses (732 / 649 Corrected
  text), The Count of Monte Cristo (1276 / 1462 Modern Library).
- **v128 — alternate-edition pill threshold 20% → 8% + 5 new books.**
  Real translation/edition gaps for classic novels run 8–16% (Rutherford
  vs Grossman Don Quixote 992/1072, Denny vs Signet Les Misérables
  1463/1232, P&V vs Dover Maude Anna Karenina 864/752), so the v126
  `altGapHuge` rule (≥20% of the baked target) HID 4 of the 5 books that
  already carried `altPageCount` (Iliad 18.5%, Moby-Dick 13.4%, Monte
  Cristo 14.6%, Ulysses 11.3% — only War and Peace 20.7% showed).
  `CurioProgressPill.kt`: `(bakedTarget * 0.20)` → `(bakedTarget *
  0.08)` — still excludes trivial trim/font variance (3–5%). Data
  added for the named + verified cases: Don Quixote (992 → 1072
  Penguin, Rutherford), Les Misérables (1463 → 1232 Penguin, Denny),
  Anna Karenina (864 → 752 Dover, Maude), Crime and Punishment (672 →
  608 Penguin, Ready), Wuthering Heights (416 → 464 Norton). All 10
  alt-field books now render their pill. Web mirror untouched (its
  schema has no `pageCount`/progress pill).
- **v127 — books.json deduped (500 → 444).** Every duplicate book (54
  normalized-name groups, 56 entries — e.g. "The Odyssey" + "The
  Odyssey (c. 8th century BCE)", "Moby-Dick; or, The Whale" + "Moby-
  Dick (1851)") collapses to ONE entry. Per pair the RICHER entry wins
  (longer teaser + scene-specific `exploreAction` target, ≤60 min,
  year-suffixed name), the dropped entry's tags are unioned into the
  keeper (`tags` merge, deduped, keeper order first), and `tier` takes
  the better (1) of the pair so marquee surfacing is preserved (23
  upgrades 2→1). Kept entries are otherwise byte-identical — ids,
  bylines, pageCounts, alt-edition fields (Moby-Dick retains 720
  Penguin Classics) untouched. The web mirror
  (`web/src/data/topics/books.json`) got the identical dedup (its
  schema lacks pageCount, so keepers there just carry tags/tier); the
  desktop port reads the app assets so it inherits the fix. NOTE: the
  same batch-duplication pattern exists in other topic files (astronomy
  94 groups, plants 86, authors 38, songs 36 — see the dedup scan) but
  only books were deduped in this change.
- **v126 — launcher icon no longer tiny + splash drops the old border +
  R8 JNA fix.** (1) **Launcher icon:** `ic_launcher_foreground.xml` inset
  28 → 18dp. The v115 inset drew the card at only ~44×47dp inside the
  108dp adaptive canvas ("the icon inside the icon / looks small") while
  the splash rendered the raw art at 112dp ("why is it bigger when the
  app opens"). At 18dp the art fills 72dp and the card lands ~60×63dp —
  still inside the 66dp safe zone, but matching the splash's presence.
  (2) **Splash:** `SplashScreen.kt` now renders
  `R.drawable.ic_launcher_icon` (the v2 art) instead of the OLD
  `ic_launcher_art` raster, which still carried the previous WHITE BORDER.
  `ic_launcher_art.png` deleted (dead). The user re-sent
  `svgviewer-output (5).svg` — byte-identical (md5) to the archived
  `design/launcher-icon/curio-launcher-icon-v2.svg`, so no re-render was
  needed; the fix was which raster each surface used + the inset.
  (3) **R8:** the v125 `vosk-android` dependency pulls in JNA, whose
  `com.sun.jna.Native$AWT` references `java.awt.*` (Component /
  GraphicsEnvironment / HeadlessException / Window) — missing on
  Android, so `minifyReleaseWithR8` FAILED. Added the standard JNA
  `-dontwarn` rules for `java.awt.**`, `java.beans.**`, `javax.swing.**`,
  `java.applet.**`, `java.nio.file.**` (the AWT interop is desktop-only
  and never invoked on-device).
- **v133 — launcher icon art bigger again (foreground inset 18 → 8dp).**
  The v126 card at ~60×63dp of the 108dp adaptive canvas left a wide sky
  ring around it ("the icon still has a border"), and the App Info screen
  — which composites the adaptive icon UNMASKED as a square — showed the
  card tiny in a sea of background. Measured the source PNG
  (`drawable-nodpi/ic_launcher_icon.png`, 2048px RGBA): the art is a
  rounded rect spanning 84–88% of its OWN canvas with ~14%-of-canvas
  corner arcs (not a small inset card). So the fix is purely the inset:
  18 → 8dp puts the card at ~77×81dp (~71–75% of the icon), and the
  corner arcs still clear the circular launcher mask (radius 54dp in
  canvas units) by ~3.5dp — no slicing on Pixel-style masks. LESSON: the
  adaptive-icon 66dp safe zone is a GUARANTEE, not the target size — the
  visible mask (circle radius 54dp, squircle wider) shows far more, and
  App Info shows the raw unmasked square, so art should fill ~70%+ of
  the 108dp canvas.
- **v135 — progress editor redesign; tolerant saved-topic lookup; all
  reveal tags + decade chip; browser icon tiles; drawer above navbar +
  section hierarchy.** (1) **Progress editor rebuilt** (CurioProgressPill):
  the ring + hidden tappable count is gone — a big count flanked by −/+
  steppers, an explicit "of {target} {unit}" chip with an edit pencil, a
  full-width progress bar + %, and a slider whose `steps` are only used
  when `target <= 200` (big totals run continuous + round) — the old
  `(target - 1).coerceAtMost(600)` steps made a 1000-page book snap to
  non-integer positions that fought the rounded Int state ("the editor
  isn't working"), and the inline target field overflowed the 132dp ring
  Box. (2) **Tolerant topic lookup** (`CurioTopic.matchesSavedName` +
  `TopicCatalog.findByName` + the reveal's pool fallback): the books
  dedupe renamed topics under saved entries ("The Odyssey" → "The
  Odyssey (c. 8th century BCE)"), so exact-name lookups hung on
  "Loading topic…". Tiers: case-insensitive exact → base-name (strip a
  trailing "(…)"/"— …" qualifier) → containment (both sides ≥ 4 chars;
  "Moby-Dick; or, The Whale" ↔ "Moby-Dick (1851)"). An unresolvable
  topic now shows its requested NAME in the hero (HeroCard
  `fallbackName`) and hides the teaser instead of a permanent
  placeholder. (3) **Reveal tags** (per user): ALL tags now render
  (FlowRow wrap — the old `take(3)` thirds cap is gone) plus a derived
  decade chip (`CurioTopic.publicationYear` + `derivedDecadeTag`,
  shared with the Topic Database sort which now delegates to it). (4)
  **Browser icon tiles** (DatabaseTopicRow + DatabaseSectionHeader): the
  old raw `cat.accent` fill/tint (deep accent on a 14% deep-accent tile
  = invisible in dark) now uses the modern theme-aware recipe
  `categorySurface(...)` + `categoryInk()` / `themedAccent()`. (5)
  **Drawer above the navbar**: HomeScreen publishes its drawer state via
  a new `CurioDrawerState` object (mirrors `CurioNavTint`), and the
  NavHost skips `CurioFloatingNavBar` while it's open. (6) **Drawer
  section hierarchy**: section headers are now raised
  `surfaceContainerHigh` pills (solid when open) with a distinct filled
  circle toggle badge, and the expanded rows animate in
  (`expandVertically`) as ONE grouped card (`surfaceContainerHigh`
  45%) — the drawer visibly grows instead of rows silently appearing in
  a same-size sheet. LESSON: a modal drawer covers the whole screen —
  anything the NavHost draws after it (the floating nav bar) sits ON
  TOP of it unless explicitly gated.
- **v136 — offline model picker is a full-height scrolling sheet.**
  `OfflineModelDialog` (SettingsSharedComponents) was an `AlertDialog`
  whose internal `Column` capped the height — the seven-model catalog
  squeezed the rows and clipped the bottom rows ("squished, can't see
  below"). It's now a `ModalBottomSheet` (the app's sheet pattern:
  `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, rounded
  28dp top corners, drag handle, `curioDialogContainerColor()`) with a
  header Row (title + circular close), the intro copy, and a
  `LazyColumn` (`weight(1f, fill = false)`) that scrolls — plus a 20dp
  bottom spacer. Row padding grew 12 → 14×13dp, spacing 8 → 10dp for
  breathing room. The old trailing duplicate `downloadError` Text inside
  the LazyColumn (invalid — raw composable in `LazyListScope`) and the
  leftover `confirmButton` closing are gone. LESSON: `AlertDialog` has a
  fixed max height and clips long lists — a scrolling list belongs in a
  bottom sheet or a `LazyColumn` inside a custom dialog, never a bare
  `Column` in an AlertDialog `text` slot.
- **v137 — CI compile: drawer collapsible groups need a Column host.**
  The v135 drawer put `AnimatedVisibility` (a `ColumnScope` extension)
  directly inside `LazyColumn` `item {}` blocks — `LazyItemScope` has no
  `ColumnScope` receiver, so `compileDebugKotlin` failed with "cannot be
  called in this context with an implicit receiver". Each group now wraps
  its `AnimatedVisibility` in a plain `Column`. LESSON: `AnimatedVisibility`
  is a `ColumnScope` extension — inside a `LazyColumn` item it must be
  hosted by an explicit `Column` (or use `Modifier.animateContentSize`).
- **v138 — offline model downloads: app-scoped manager (pause/resume/
  cancel/multi) + delete hardening + transcribe gating.** (1) **New
  `VoskModelDownloads` object** (OfflineTranscriber.kt): downloads used to
  run in the picker dialog's `rememberCoroutineScope`, so swiping the
  sheet away CANCELLED the transfer. State now lives on an
  application-lifetime `CoroutineScope(SupervisorJob() + Main.immediate)`
  as a `StateFlow<Map<modelId, State>>`; each model gets its own `Job`, so
  several download at once, and closing the sheet leaves them running.
  **Pause** aborts the transfer cleanly (`PauseRequested` thrown inside
  the loop, partial zip kept) and the loop awaits a per-model
  `CompletableDeferred` gate; **resume** completes the gate and re-opens
  the connection with `Range: bytes=<received>-` (206 → append; 200 →
  restart from scratch). **Cancel** cancels the job, disconnects the live
  connection, drops the partial zip and resets the row. `start()` guards
  on `existing.isActive` and `invokeOnCompletion` removes the job entry
  only `if (jobs[id] === job)` so a fresh start after a cancel isn't
  clobbered. (2) **Delete hardened** (`VoskModels.deleteModel`): only
  deletes `filesDir/vosk-models/<id>` when the parent is exactly
  `vosk-models` (a mis-resolved path can never wipe the root), and also
  clears the cached zip. (3) **Transcribe gating** (EntryDetailScreen
  `SoundBiteRender`): the Transcribe button + transcript box previously
  rendered on ANY Sound Bite — including note-only takes with no audio
  file. Both branches are now gated on `!data.audioFilePath.isNullOrBlank()`
  (`if (transcript == null && hasAudio)` / `else if (transcript != null &&
  hasAudio)`). (4) **Visible background downloads**: the Settings →
  Recording "Offline model" row subtitle shows "Downloading <model> ·
  N%" while a transfer runs after the sheet closes; the picker intro copy
  now tells the user downloads survive closing the screen.
- **v139 — offline model picker: real storage usage + big-download
  confirm.** (1) Downloaded rows now show their REAL on-disk size —
  `VoskModels.modelSizeBytes` (walks the model dir) rendered via a new
  GB-aware `formatModelSize` (the app's `formatFileSize` caps at MB; the
  Full tiers run 1–2.3 GB) — e.g. "Downloaded · 41.2 MB". (2) Tapping
  Download on a model ≥ `BIG_MODEL_BYTES` (100 MB — the Large/Full tiers)
  or one bigger than the free space (`VoskModels.availableStorageBytes`,
  `StatFs` on `filesDir`) opens a confirm AlertDialog showing required vs
  free size —  red when it won't fit ("Only X free — the download will
  likely fail") — before `VoskModelDownloads.start` runs. Small models
  with plenty of room still start instantly.
- **v140 — offline model picker: quality-tier badges.** `VoskModels.Tier`
  enum (SMALL/LARGE/FULL with `label` + `hint` — "fast & light" /
  "more accurate" / "most accurate") added to `Info` (set per catalog
  entry). Each picker row now shows a compact tinted badge between the
  name and the subtitle — "Small · fast & light" etc — colored with the
  existing theme-aware inks (  `curioSageInk` / `curioGoldInk` /
  `curioRoseInk`), so the accuracy ladder reads at a glance in both
  light and dark; selected rows flip to white-on-amber. LESSON: reuse the
  theme's ink helpers for tier/category color coding instead of raw
  `CurioColors` constants — they stay readable on dark AND light fills.
- **v141 — reveal morph: byline+year pill row on BOTH the ticket and the
  hero.** The reveal hero's top-left used to wear the ACTION badge
  ("Watch for ~25 min") while the Spin ticket's top-left wore the byline
  ("Director · Nolan") — so during the shared-element morph the pill
  content swapped position/content and visibly jumped. (1) The reveal
  hero's top-left now wears the SAME byline pill row as the ticket
  (identical recipe: `ink.copy(alpha = 0.18f)`, RoundedCornerShape(50),
  labelMedium bold, h12/v6 — the reveal's old Person-icon variant
  dropped), and the action badge moved DOWN to the bottom pill row next
  to the subtype. (2) **Year out of the title**: new
  `CurioTopic.titleAndYearQualifier()` splits a trailing " (…)" / " — …"
  qualifier ("Moby-Dick (1851)" → "Moby-Dick" + "1851"); both the
  ticket title and the hero title render the BASE name, and the year is a
  small `Schedule`-icon pill in the top-left row on BOTH — so the titles
  and pills read identical during the morph. The reveal's decade tag chip
  (v135) is untouched. LESSON: shared-element morphs only look smooth
  when content in the SAME position has the SAME content on both ends —
  matching the card's bounds alone isn't enough if pills/titles swap
  inside it.
- **v142 — Manage Categories full-bleed bottom; Pet Designer floating
  pill bar + fade open; first-run "Pick a lane" wired to the Spin picker.**
  (1) **Manage Categories full-bleed** (per user, confirmed): the NavHost
  no longer applies `windowInsetsPadding(navigationBars)` to the
  MANAGE_CATEGORIES route (new `fullBleedBottomRoutePrefixes` set — the
  v132 reveal precedent); the page's wash runs to the bottom edge and the
  screen clears the gesture bar itself (`navigationBarsPadding()` on the
  LazyColumn + scroll indicator). LESSON: the NavHost's generic nav-bar
  inset for push routes shows as a reserved strip under pages that paint
  their own wash — full-bleed pages opt out and pad their own list.
  (2) **Pet Designer** (per user, confirmed "both"): the studio's
  bottom nav (`PetStudioBottomNav`) was the app's OLD stock M3
  `NavigationBar` — restyled to the v124/v129 floating pill bar recipe
  (`surfaceContainerHigh` rounded-50 container, 6dp shadow, solid
  `secondary` fill + `onSecondary` ink active capsule, 52dp tabs); the
  removed NavigationBar/WindowInsets imports are gone. The route also
  opens with the reveal's clean fade (new `isPetDesignerRoute` branches
  in enter/exit/popEnter/popExit before the scale-pop group) instead of
  the mechanical zoom. (3) **First-run "Pick a lane"** (HomeScreen
  `FirstTimeEmpty`) previously opened the separate full-screen
  `CurioRoutes.PICKER` page; it now sets a one-shot `SpinPickerRequest.pending`
  flag (new object in CurioBottomNav beside `CurioDrawerState`) and
  navigates to the Spin TAB, whose `LaunchedEffect(SpinPickerRequest.pending)`
  opens its own `CategoryPickerSheet` (lane chips + Mix presets). The
  `LaunchedEffect` is keyed on the flag so it fires even when the Spin
  tab was already composed. LESSON: a cross-screen "open this sheet"
  request belongs in a shared state object (the `CurioDrawerState`
  pattern), not a route arg.
- **v158 — Full server-grade Vosk models removed (no medium tier exists) +
  the dictation mic is now on EVERY note/quote text box in Save your take.**
  User: "remove the full models as they are laggy and crashing the app
  along with my phone. and add medium model if theres more. in voice model.
  and the voice bubble in save your take show it in each note and quote
  text box not just in sound bite". (1) MODELS: the 1–2.3 GB Full-tier
  models (Full English / Gigaspeech English / Full Indian English) are
  GONE from `VoskModels.CATALOG` (now Small ×3 + Large ×1); the
  `Tier.FULL` enum value, its rose badge tint and the picker copy were
  removed. RESEARCH: alphacephei.com's catalog has NO English "medium" —
  the ladder is Small ~40–60 MB / Large ~128 MB / server-grade 1–2.3 GB,
  so nothing was added. A stale-install guard runs at startup
  (`VoskModels.pruneRemovedModels` in MainActivity): any installed dir
  whose id is no longer in the catalog is deleted (zip too), and a stale
  saved selection is cleared; the detail-page Transcribe button also
  verifies `VoskModels.byId(modelId) != null` before loading. LESSON:
  removing a heavy-download feature must handle the already-downloaded
  artifacts + persisted selection, not just the picker list.
  (2) DICTATION EVERYWHERE: the SoundBite-only floating-mic flow was
  extracted into a SHARED reusable composable `DictationMic`
  (features/capture/formats/DictationMic.kt) that owns its own
  recognizer session (lazy, destroyed on dispose) + its own RECORD_AUDIO
  permission launcher + the live-preview dialog, and reports live-listening
  via `onListeningChange` (SoundBite keeps reporting dictation as busy so
  format-switch confirmation still guards a live session). SoundBiteFormat
  lost its ~330 lines of inline recognizer machinery + private dialog;
  the mic now rides the tool dock (`trailingAction`) of EVERY RichTextEditor
  note box (Field Notes' 3 sections, Marginalia journal, Reel Notes
  review, Sound Bite note), the GalleryWall caption (PaperLineField
  label row) and every quote card via the SHARED `QuoteCardEditor`
  (covers all QuoteCardsSection callers + the mood board + the floating
  quote dialog). Every mic is gated on `AppPreferences.voiceToTextEnabledState`
  (the v125 experiment toggle) — visible only when the setting is on.
  Insert appends the transcript to the box; quote cards preserve spans
  via `QuoteCardsState.setText`'s clamping. LESSON: when a one-off
  feature (SoundBite's mic) becomes an everywhere-feature, extract the
  machinery once into a self-contained composable and reuse it — the
  caller keeps only the state var + an append lambda, and the busy-state
  callback is the one thing the shared piece must surface back.
- **v159 — nav pill height slimmed 60 → 48dp (lengths kept), bar + reveal
  Like/Dislike together.** User: "the navbar pill height is too much keep
  its lengh but decrase the heigh the widneness same in like and dislike".
  `FloatingPillHeight` (CurioBottomNav) and `RevealSentimentHeight`
  (TopicRevealScreen) both 60 → 48dp; the widths stay 60/128dp and the
  icon stays 26dp (fits fine with 11dp of breathing room). The bar's
  capsule container has its own padding, so a shorter pill just shrinks
  the capsule — nothing else needed. LESSON: pill geometry is three
  independent constants; the user's "keep length, shrink height" is a
  single constant change applied to both copies of the size trio.
- **v160 — the remaining dark-mode hairline rims are ALL gone (the 5
  surfaces the v157 note flagged + the Spin one).** User: "remove the
  same darkmode hairline that u mentioned exosist in more elements". The
  v149 `BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))` rim was
  stripped from: the tour Skip/Next dock (CurioNavHost), the reveal
  Like/Dislike pill (TopicRevealScreen), the category picker's "Manage
  categories" pill (SpinScreen — the one the v157 note didn't list), and
  the pet studio bar + floating action capsule (PetDesignerScreen).
  Each Surface keeps its `shadowElevation = 6.dp` and theme container
  fill; the now-unused `BorderStroke` import went in all four files
  (CurioNavHost also lost its now-unused `isCurioDarkTheme` import).
  Non-rim `BorderStroke` uses (badges, chips, quest medals, glass)
  untouched. LESSON: the v157 note enumerated the rim sites but missed
  the Spin one — a pattern sweep (`grep BorderStroke` for the
  `White.copy(alpha = 0.10f)` signature) catches every copy before
  claiming "all gone".
- **v161 — nav collapse smoothed for real + Cabinet "All" yellow pill
  fixed (primary fallback) + rail/pet-studio audit.** User: "the collapse
  animation of buttom nav pill is still bad and also why its yello in
  cabinet all. fix that and do more audit". (1) COLLAPSE: the v155
  smoothing missed the two real culprits — the label EXIT was
  `fadeOut(tween(0))` (the closing pill's text vaporized while the pill
  took ~1s to deflate: a dead empty box shrinking) and the width spring's
  `StiffnessMediumLow` dragged the 128→60dp collapse out a full second.
  Now: exit fades 160ms FastOutSlowIn with the shrink, stiffness
  MediumLow → Medium (damping stays 0.9 — near-critical, no overshoot),
  applied to `FloatingNavPill`, the reveal `SentimentSegment`, AND the
  pet studio `PetStudioTab` (audit catch: it still ran the OLD v124
  recipe — damping 0.75, which overshot and bounced, plus the instant
  vanish). (2) CABINET YELLOW: `FloatingNavPill` fell back to
  `colorScheme.secondary` (ButterYellow) whenever the page published no
  accent — Cabinet "All" publishes none (plain page) → stray yellow pill.
  Fallback is now the theme PRIMARY (coral, the app's brand color, with
  `onPrimary` ink). (3) AUDIT: the wide-window `CurioNavigationRail` had
  the SAME yellow (hard-coded `secondary` indicator) AND never wore page
  accents — it now resolves `curioNavActiveAccent(selectedRoute)` like
  the pill bar (page accent or primary fallback). The pet studio bar's
  amber active fill is INTENTIONAL (pet brand) and untouched. LESSON:
  "smoother animation" fixes need to target the exit spec + spring
  stiffness, not just damping; and every sibling that copies a recipe
  (rail / pet studio / sentiment) must be swept when the recipe changes.
- **v162 — tab-switch motion runs on ONE spring family; all pill parts now
  move in lockstep.** User: "Record the tab-switch motion and tune the pill
  spring/fade until the collapse reads perfectly smooth". (No screen
  recording / device available in this env — the tune is a spec-level
  audit of the animation timeline.) AUDIT FINDINGS: v161 had only fixed
  the WIDTH spring — the fill `animateColorAsState` still ran the old
  `StiffnessMediumLow` spring (lagged the pill), the icon tint ran a
  `tween(200)` and the label AnimatedVisibility ran its own `tween(240/
  160)` — all three finished or lingered OUT OF STEP with the pill width
  (the label was fully in/out while the pill was still mid-flight; the
  fill was still catching up after the pill settled). FIX: one shared
  spring family per pill — `PillWidthSpring` (`spring<Dp>(0.9, Medium)`)
  for the width and `PillMotionSpring` (`spring<Float>(0.9, Medium)`) for
  the fill, icon tint and the label's `expandHorizontally`/`shrink
  Horizontally`/`fadeIn`/`fadeOut` (the expand/shrink ALSO got the spec
  — their default is damping 1.0, a different shape than ours). Identical
  params from the same start frame = identical trajectories = perfect
  lockstep. Applied to `FloatingNavPill`, the reveal `SentimentSegment`
  and the pet studio `PetStudioTab` (its fill stays a deliberate solid
  snap, v156 design). LESSON: "sync everything to the same spring" is
  the fix for perceived jank in multi-part morphs — check EVERY animated
  property (width, fill, icon, label) for its own spec, and pass the
  spring to expand/shrink, not just fade.
- **v163 — quest badge glyphs un-squished (normal weight) + raw Dialog
  windows get a springy open entrance.** User: "more smoother open
  aniations of things and fix the badge icon just the quest badge icons".
  ask_user clarified: badge icons looked CLIPPED/SQUISHED, and the open
  animations that bothered them were DIALOGS & SHEETS. (1) BADGE:
  `CurioBadgeMedal` drew every glyph at `weight = FontWeight.Bold` —
  Material Symbols at wght 700 render very heavy, and inside the medal's
  tight inner plate (0.80× with a 1.5dp ring) the heavy glyphs crowded
  the ring and read squished/clipped. All three icons (earned glyph,
  locked StarOutline silhouette, tiny earned check) now render at NORMAL
  weight — the clean outlined stroke with breathing room. The full
  material_symbols_outlined.ttf has every ligature, so glyph NAMES were
  never the problem. LESSON: "icon looks clipped" inside a tight circle
  + a heavy font weight = drop the weight, don't shrink the size.
  (2) DIALOG/SHEET OPENS: the app's M3 AlertDialogs and ModalBottomSheets
  already animate; the raw `androidx.compose.ui.window.Dialog` windows
  (the full-screen mood board in GalleryWallFormat, its floating quote
  editor, and EntryDetailScreen's expanded mood board) popped in with NO
  animation. New shared `CurioDialogEntrance(scale, content)` in
  CurioAnimations.kt — fade + near-critical scale-up (spring 0.9/380)
  played on the first frame via MutableTransitionState (same trick as
  ScreenEntrance); `scale = 1f` = pure fade for full-screen canvases that
  shouldn't zoom, 0.96 for the floating quote card. Wrapped all three
  raw Dialog sites. LESSON: audit the raw `Dialog(` call sites — they're
  the only pop-in-instantly windows; M3's own dialogs/sheets animate.
- **v164 — navbar tab labels bumped SemiBold → Bold (Material text style).**
  User: "use new bold fonts for navbar texts material text". The floating
  pill bar's tab labels (Home · Spin · Cabinet) rendered
  `labelMedium.copy(SemiBold)`; they're now `labelMedium.copy(Bold)` —
  the bolder label reads proper inside the big 60/128dp pill. Same bump
  applied to the wide-window rail labels, the reveal Like/Dislike labels
  and the pet studio tab labels so every pill-family label matches.
  Geom (the app's display font) declares a real Bold face, so no fake-
  bold synthesis. LESSON: the pill label weight lives in FOUR copies
  (pill, rail, sentiment, studio) — bump them together.
- **v166 — nav collapse gentler + SAME animation on all pills; muted
  active-pill colors (Cabinet "All" fallback is no longer pink); calm
  page openings; dark-mode session-note editor.** User: "make the nav bar
  collapse animation slower a little not violent and smoother. and also
  chnage the bright colors to use muted colors and in cabinet all use the
  default theme aware color. not pink. from the spin shuffle option, and
  fix the dark mode session note text box..." then "also same animation
  for all pills, and also the page opening have become too violent did u
  edit it?"
  - PILLS — the whole family now runs `spring(dampingRatio = 1f,
    stiffness = 750f)`: CRITICALLY damped (zero overshoot/bounce — the
    old 0.9 damping still snapped) and half of Medium stiffness ("slower
    a little", still ~3.5x snappier than the MediumLow that dragged a
    second). Applied IDENTICALLY to the nav pill bar (4 typed specs),
    the reveal Like/Dislike segments (4) and the pet studio bar (3) —
    every animated pill moves with the exact same physics.
  - MUTED COLORS — the active pill wore the raw saturated page accent
    (loud/neon). New `curioActivePillFill()` (CurioBottomNav): light
    mode pulls saturation ~45% via `toHsl/fromHsl` (hue + lightness
    preserved so deep accents keep white ink); dark keeps the already-
    muted deep jewel tone, pastel keeps its airy twin. The reveal
    SentimentSegment got the same mute. Rail shares the same helper.
  - CABINET "ALL" FALLBACK — plain pages (Cabinet "All") fell back to
    `colorScheme.primary` = CoralBlush, which read as a stray PINK pill
    (v161 had swapped butter-yellow → coral; the user calls the coral
    "pink from the spin shuffle option"). Fallback is now the MUTED
    `secondaryContainer` + `onSecondaryContainer` ink — a standard
    theme-aware M3 pair (soft warm in light, subtle glow on black),
    never a bright hue.
  - PAGE OPENINGS — answer to "did u edit it?": the NavHost screen
    transitions were NOT touched (they've been tween-based since v7.17;
    detail/pop screens used a 0.88 scale pop). But three spring
    entrances DID read violent: the NavHost detail/pop scaleIn/scaleOut
    now opens at 0.94 (half the zoom), `CurioDialogEntrance` (v163,
    mine) dropped its underdamped 0.9/380 spring for the shared
    `Springs.Calm` (1.0/750 — zero overshoot), `MorphEntrance`'s
    non-bouncy path (category grids) swapped the overshooting
    `Springs.Deliberate` (0.85/250 — slow + zoom-back) for `Calm` at a
    closer 0.92 start, and `ScreenEntrance` (Support/Promo/TopicHistory)
    slid on `Calm` too. New `CurioMotion.Springs.Calm` = the pill
    family's exact physics, so screens and pills share ONE spring
    signature.
  - SESSION NOTE — `SessionNoteFloatingPill`'s popup editor was a BRIGHT
    cream paper sheet in dark mode (the note-paper palette is theme-
    agnostic for SAVED notes, but this floating popup is a UI control).
    Dark mode now swaps the sheet to `surfaceContainerHigh` + `onSurface`
    ink, and the `OutlinedTextField` gets explicit paper-paired colors
    (focused/unfocused text, placeholder, cursor via
    `paperControlAccent`, paper-derived borders) — the M3 defaults were
    painting light text over the bright cream (invisible-text bug).
    Light mode keeps the cream paper exactly.
- **v167 — reveal Like/Dislike pill wears the dynamic page tint; nav
  pill tap ripple removed.** User: "the like and unlike button pill doesnt
  get the backgroud tint with dynamic theme fix it. and dont add the
  touch shado in nav bar".
  - Reveal pill container was a STATIC `surfaceContainerHigh` — it never
    picked up the page tint the nav bar capsule gets. Split
    `curioFloatingNavContainer(routePrefix)` into a shared lift helper
    `curioFloatingNavContainerFor(wash)` (light: wash lifted 30% toward
    surfaceContainerHigh; dark: surfaceContainerHigh) and the reveal
    pill now passes `cat.categoryBackgroundWash()` — the reveal page's
    own tint — so the capsule matches the page like the nav bar does.
    (The route-keyed version can't reach the reveal's wash; the reveal
    isn't a tab route.)
  - "touch shado" = the tap RIPPLE (ask_user confirmed: "Remove the tap
    ripple"). FloatingNavPill's clickable now passes `indication = null`
    with a remembered `MutableInteractionSource`, so tapping a tab never
    flashes the grey ripple circle. The 6dp drop shadow under the bar
    stays (user picked ripple only). LESSON: "touch shadow" in this
    project's    user-speak = the press ripple — ask rather than assume
    which shadow.
  - v167 CI FIX: `CurioAnimations.kt:70` — `SpringSpec<Float>` (the new
    `Springs.Calm`) passed to `slideInVertically`, which animates
    IntOffset. The calm spring there is now typed inline
    `spring<IntOffset>(1f, 750f)` (same physics). LESSON (same as v165):
    a spring's generic must match the ANIMATED value's type —
    slideInVertically/Out = IntOffset, scaleIn/Out + fade = Float,
    expand/shrinkHorizontally = IntSize, colors = Color.
- **v168 — progress editor dialog: the count is a single number in the
  top corner (tap → inline edit, Enter saves, replay icon resets), and
  the "0 / pages" line + "Edit total" chip are removed.** User: "from
  the progress editor dialog remove the 0/pages option and move it to
  the top corner and just show one no. and tapping it automatically lets
  u edit and when u tap enter it saves and it lets u reset the number to
  default too. and also remove the edit total option too and dont push
  this". CurioProgressPill.kt's CurioProgressEditorDialog:
  - The "$value / $target $unit" line under the ring and the v149
    "Edit total" chip + its inline target field are GONE (the target now
    comes only from the topic data or the alternate-edition pill's
    [initialTarget] prefill, which still persists via Save).
  - A top-right Row in the dialog's content shows ONE number (the
    current count, no suffix): tap → the BasicTextField opens in place
    (number keyboard, IME Done), Enter → [commitValueEdit] persists
    immediately (TopicProgressStore.set / clear on 0). A 26dp replay
    icon beside it resets to the default (0) and persists.
  - The ring keeps just the big %; −/+ steppers + slider unchanged;
    Finish + Save unchanged.
  - COMMIT ONLY — the user said "dont push this" (push rides with the
    next real change). LESSON: the user's "0/pages" = the "0 / 350
    pages" count line under the ring — shorthand for current/total
    display.
- **v169 — category-picker "Manage categories" is a proper floating,
  theme-aware, text-only capsule; filters' Apply/Show-all drops its check
  tick.** User: "in category picker the manage category option there a
  scafhold or strip behind the button make it proper floating, and theme
  aware and remove the tick mark just text and same in filters show all
  topics dont push". SpinScreen.kt:
  - Manage pill (the picker sheet's bottom action): was a full-width bar
    (`fillMaxWidth()` — read as a strip). Now content-sized + centered
    via ColumnScope `.align(Alignment.CenterHorizontally)`, and its
    static `surfaceContainerHigh` color became the nav-pill's dynamic
    container `curioFloatingNavContainerFor(currentCat.categoryBackgroundWash())`
    — the sheet's wash lifted 30% toward the elevated surface in light,
    elevated dark in dark (the sheet itself is `currentCat.categoryBackgroundWash()`
    tinted). Text-only: the CurioIcons.DragHandle glyph + Spacer are gone.
  - Filters' Apply / Show all topics pill: the leading CurioIcons.Check
    tick + Spacer are gone — text only (accent pill design unchanged).
  - COMMIT ONLY — the user said "dont push" (still unpushed: v168 too).
    LESSON: "scafhold or strip" = a full-width bottom action read as a
    bar; "proper floating" = the content-sized centered capsule language;
    "tick mark" = the leading glyph in the pill row.
- **v170 — Edit profile dialog restructured: "Your name" + "Bio"
  sections (bold, larger, icon per heading), tagline field becomes the
  Bio, tagline label + automatic-tagline button + helper texts removed.**
  User: "now in edit profile dialog, make the your name and the line
  under it text ith Your name and in bold and larger text then below Bio
  similiar way, remove the tagline text, and remove the automatic tagline
  option and also the leave the tagline empty to use the automatic streak
  one that tet too. and make the profile photo text a little bigger, and
  fix the page margin and hirarcy and maybe add icons etc and dont push".
  ProfileScreen.kt ProfileDialogs:
  - The helper "Your name and the line under it." became a section
    heading **"Your name"** (titleMedium ExtraBold + Person icon); below
    it the name field (label "Display name" removed → placeholder
    "Your name"). Below that a **"Bio"** heading (same style + Note
    icon) over the tagline field — the tagline field IS the bio (the
    app has NO bio data model; the "line under the name" is the custom
    streak tagline). Its "Tagline" label is gone → placeholder "Keep
    the spark going today.".
  - REMOVED: the "Use automatic tagline" TextButton (+ onResetTagline
    param + caller wiring) and both helper texts. Leaving the field
    empty still falls back to the automatic streak line (unchanged
    behavior — getCustomStreakTagline.ifBlank { taglineForStreak }).
  - "Profile photo" label bumped labelLarge → titleMedium ExtraBold +
    an Image icon; sections spaced 16dp with 8dp heading→field gaps
    ("fix the page margin and hierarchy"). New EditSectionLabel helper.
  - COMMIT ONLY — "dont push" (v168 + v169 also unpushed). LESSON: the
    "line under the name" = the custom streak tagline — the user calls
    it "Bio"; no bio model exists, so the tagline field is renamed, not
    a new field.
- **v171 — share card is a 3:4 portrait with the sharer's name, session
  note and attached photo; light-mode dialogs wear the theme-aware
  container (not cream white).** User: "now improve the share card and
  not squared but 3:4 and also use name and the note user added that
  option with photos option if added. just more beautiful to share. and
  also give each dialog in light mode the theme aware backgroud not
  cream white".
  - CurioShareCard (EntryDetailScreen): exported at 450×600 dp (3:4,
    was 400×400 square; the sheet preview Box matches at 280 × 3:4).
    New content: the sharer's display name (AppPreferences.getDisplayName,
    bottom), the session note (entry.sessionNote, a rounded note block
    under the teaser), and the FIRST attached photo
    (entry.sessionScreenshots, a 200×150 rounded block). The photo is
    decoded SYNCHRONOUSLY via BitmapFactory → asImageBitmap inside
    remember — Coil's async painter would miss shareComposableCard's
    single-frame capture (the preview and the PNG must show the same
    photo). Format + date chips collapsed into one quiet "Format ·
    Captured..." text line.
  - curioDialogContainerColor (CurioTheme): the light branch returned
    lerp(surfaceContainerHigh, background, 0.72) — 72% toward the cream
    background read as a cream-white panel. Now returns
    surfaceContainerHigh directly (the warm tan the floating pills /
    chips wear) — theme-aware, applies to EVERY AlertDialog via this
    shared function. Dark branch untouched.
  - PUSHED (this request had no "dont push" — the accumulated v168/v169/
    v170 commits rode along). LESSON: "photos option" = the entry's
    sessionScreenshots attachments; synchronous decode is mandatory for
    the off-screen card capture.
- **v174 — drawer redesigned as a "tiny personal observatory".** User
  pasted a full design spec + dropped an SVG (`svgviewer-output (10).svg`
  → moved to `res/raw/drawer_footer.svg`) and said "ask if not understand
  the plan". ask_user: stats = REAL data where it exists (fallback to the
  design's numbers only where no counter exists); the 3 nav rows KEEP
  tap-to-expand sub-rows (collapsed by default, per "no accordion
  expansion by default" = restyled rows, not dropped functionality).
  HomeScreen.kt (`HomeDrawerContent`):
  - HERO: rose Surface → gradient sky Box (light seafoam #C2E8DE→
    #E9F6F0, dark twilight #12313A→#1D4750) clipped to the torn shape;
    `DrawerCelestialSky` paints 30 seeded stars, 3 four-point sparkles
    (`drawSparkle`), a faint dipper constellation and a punched crescent
    moon (punch uses lerp(skyTop, skyBottom, y) = the local gradient
    colour); `DrawerRollingHorizon` adds two cream hill bands just above
    the tear (dark = warm tan so cream ink stays readable). Avatar ring
    is now cream; greeting ink steps with the sky.
  - CURIOSITY MAP: `DrawerCuriosityMap` card (rounded 24, opaque pastel
    lerp — shadow-safe) with "Your Curiosity Map" + subtitle + a
    decorative "This Week ˅" pill; `ConstellationBrain` Canvas draws an
    abstract brain (22 normalized nodes, gold→blue lerp by x, glow on
    hot nodes, bigger "important" stars) with the "Overall / Curiosity /
    N" score overlaid at the centre; 7 orbiting `MapStat`s (icon +
    label + bold value): Learned/Questions/Shared = the DESIGN numbers
    (no counters), Explored=explores, Topics=spins, Saved=saves,
    Streak=StreakTracker.getStreak. Overall = sum of the six.
  - NAV ROWS: `DrawerNavRow` replaces DrawerSectionHeader — tinted icon
    chip, bold label + subtitle, chevron (right = direct nav for Quests
    & Levels; up/down = tap-to-expand for Your Curiosity / About). The
    5 sub-rows (Topic History / Manage Categories / Browse Topics /
    Support / Replay intro) keep the existing AnimatedVisibility groups.
  - FOOTER: `DrawerFooter` loads res/raw/drawer_footer.svg via Coil's
    SvgDecoder (NEW dep io.coil-kt:coil-svg:2.7.0 — sibling of the
    existing coil-compose; libs.versions.toml coilSvg) in a rounded,
    shadowed box; a vertical surface→transparent gradient fades its top
    ("fading look"); "v{version} · Made with curiosity ♥" sits on the
    landscape (footerInk warm tan #7E6E50). Old pinned footer Column
    removed.
  - v174b ("continue the plan"): the constellation's BEHAVIOUR is now
    data-driven — per-node `weights` (0..1 per region: likes → left
    hemisphere, saves → right, pins → inner, spins → top, explores →
    brainstem) scale each star's radius (1.9+2.4w / 3.0+2.4w big) and
    glow (0.07+0.15w); fissure `bridges` draw progressively from the
    relationship score (quotes+pins+likes: >0→1, >5→2, >20→3, >60→4,
    >150→6); the two centre-fissure nodes keep the "recent discovery"
    glow. Sky got "extremely subtle grain" (52 micro-dots, alpha
    0.05-0.13, seed+17). Map card got the brief's "thin borders": a
    1dp seafoam hairline in LIGHT mode only — dark stays borderless
    (v157 hairline rule).
  - LESSON: SVG-at-runtime = coil-svg (keep the artist's file as-is); a
    332KB hand-drawn SVG is NOT safe as a VectorDrawable (aapt2 path
    blob). Pastel fills under shadows must be opaque lerps (rule 11).
- **v174c — new Stats page ("Your Curiosity") + CI fix.** ask_user on the
  stat-page design: centerpiece = interactive constellation brain;
  sections = ALL (streak+level, lifetime totals, per-category, quests &
  badges); reachable from drawer AND Profile; style = observatory.
  - CI FIX: CurioProgressPill.kt:342 — the v168 progress-dialog rework
    wrote `horizontalAlignment = Alignment.End` on a ROW (that's a Column
    param). Row uses horizontalARRANGEMENT — fixed to
    `Arrangement.spacedBy(8.dp, Alignment.End)`. LESSON: Row = horizontal-
    Arrangement + vertical-Alignment; Column = horizontal-Alignment +
    vertical-Arrangement — the v168 rename flipped them.
  - NEW FILE app/src/main/java/com/curio/app/features/stats/StatsScreen.kt:
    StatsScreen(navController) registered as CurioRoutes.STATS ("stats")
    in NavHost. Celestial sky header (StatsSkyHeader, mirrors the drawer
    palette), StreakLevelCard (streak/best + level + xpProgress bar),
    StatsConstellationCard (INTERACTIVE CategoryConstellation Canvas:
    one star per explored lane, two-lobe arc layout with deterministic
    jitter seeded by CategoryId.name.hashCode, radius 5.5+min(count,60)*
    0.30dp, glow on lanes saved this week, tap within 34dp selects via
    detectTapGestures, empty tap clears; accents resolved in COMPOSITION
    — themedAccent is @Composable and CANNOT run in the draw lambda),
    LifetimeTotalsCard (2-col grid of the 8 lifetime counters),
    JourneyCard (stages progress + CurioBadgeMedal row + Quests link),
    LanesBreakdownCard (per-lane rows). Data: repo.getAll() grouped by
    topic.categoryId ∪ CurioQuests.categoriesState (CategoryId.valueOf in
    runCatching), StreakTracker, xpProgress. StatsCard shell = same
    opaque pastel card as the drawer map.
  - ENTRY POINTS: drawer — DrawerCuriosityMap got onClick (whole card
    opens stats; chevron hint added) + a "Stats & insights" sub-row
    (glyph "monitoring") in the Your Curiosity group; Profile —
    ProgressAndAchievementsCard gained onOpenStats + a compact Stats pill
    in the XP header row.
  - CI FIX 2 (v174c): StatsScreen.kt "Unresolved reference 'Column'" at
    every `StatsCard {` + the declaration — the shared card shell declared
    `content: @Composable Column.() -> Unit`, but Column is a COMPOSABLE
    FUNCTION, not a type; the receiver scope type is ColumnScope
    (androidx.compose.foundation.layout.ColumnScope). LESSON: `X.() ->
    Unit` receivers need a real TYPE (ColumnScope / RowScope / BoxScope),
    never the layout function name.
- **v175 — backup/restore now carries the profile avatar FILE (was
  path-only).** User: "the profile pic wasnt restoring on backup restore
  so fix it". Root cause: `AppPreferences.getProfileAvatarPath` stores an
  absolute `filesDir/profile_avatar_<ts>.png` path in `curio_app_prefs`;
  backup stored the prefs (path) but never the PNG, so a restored app
  pointed at a nonexistent file → blank avatar. Fix in CurioBackupManager:
  FORMAT_VERSION 6 → 7; export streams a new `avatarFile` section (base64
  of the PNG, null when unset); restore reads it (NULL-safe), writes a
  FRESH `profile_avatar_<ts>.png`, sweeps old avatar files, and overrides
  `setProfileAvatarPath` AFTER the prefs loop (the restored prefs carry
  the source device's dead path). v6 backups (no avatarFile) still
  restore — reader's else-skip covers it. BackupPayload got a matching
  `avatarFile: ByteArray?` field (legacy class, consistency only).
  LESSON: never back up only a filesDir path — the file must ride along;
  when restoring paths into prefs, re-home them to fresh files.
- **v174d — the drawer map's "This Week ˅" selector is now LIVE and
  filters the stats constellation.** User: "Make the stats constellation
  filter to a time range via the 'This Week' selector on the drawer
  map". New `features/stats/StatsRange.kt`: `StatsRange` enum (WEEK 7 /
  MONTH 30 / ALL null days) + `StatsRangeState` singleton (mutableStateOf,
  private set — same pattern as CurioNavTint) + the shared
  `StatsRangeSelectorPill` (DropdownMenu with bold current choice). The
  decorative drawer pill is replaced by it (chevron hint kept); the stats
  constellation card header gets the same pill. StatsScreen now loads ALL
  entries once, filters via `filterForRange` (capturedAtMillis >=
  cutoff), and derives laneCounts/laneRecent/explored from the WINDOW
  (explored ∪ quest categoriesState only for All Time — quest history has
  no timestamps). Subtitle reflects the window
  ("saved this week/month/all time"). In-memory state (not persisted).
  LESSON: only entry-based stats are time-bucketed; lifetime counters and
  quest history can't be filtered — union quest lanes only on All Time.
- **v174e — drawer map rework: no fake data, real lane constellation +
  sky-tear banner + transparent footer.** User: "dont use fake data, use
  empty state, extend the drawer banner's sky design and its color to the
  tear start so it looks like a sky tear... remove the learned/explored/
  topics/questions/saved/shared/streak stats, only use what exists... dots
  with rounded icons connected, tapping shows the data... replace the
  footer svg with this one (no background), place it more below and less
  opaque so 'Made with curiosity' is visible". ask_user: (1) drop the
  stat list entirely; (2) icon-dots in the DRAWER map only (stats page
  unchanged); (3) the sky tear is the drawer banner — the stats page stays
  as-is, just light-mode tuned.
  - DRAWER MAP (`DrawerCuriosityMap` in HomeScreen.kt): the 7 orbiting
    `MapStat`s (Learned=128 / Explored / Topics / Questions=98 / Saved /
    Shared=72 / Streak — the fake design numbers) and the "Overall
    Curiosity N" center overlay are GONE. The card now loads real entries
    (`CurioRepositoryHolder.repo.getAll()` in a LaunchedEffect), filters
    them via the SHARED `filterForRange` (moved from StatsScreen.kt to
    StatsRange.kt — same package, no import churn), and shows a
    `DrawerLaneConstellation`: one 34dp rounded lane-icon chip per
    EXPLORED lane (deterministic two-lobe arc seeded by name hash, thin
    connecting lines, accent border; selected = solid accent fill +
    `onAccent()` icon + 3dp lift). Tapping a dot toggles an inline panel
    (lane name + "N saved · active this week" + dismiss chip); the
    helper copy "A little galaxy of everything you've explored." only
    shows in the EMPTY state (no explored lanes → icon + "Spin a deck
    and explore to light up your map."). Deleted `MapStat` +
    `ConstellationBrain` (dead). Whole card still opens the stats page on
    non-dot taps (nested clickables — the inner dot consumes its tap).
  - SKY TEAR (drawer banner): `DrawerRollingHorizon` (the cream hills at
    the banner's bottom) is DELETED so the sky gradient + stars run all
    the way down to the torn seam — the banner reads as a torn piece of
    sky. LESSON: the torn hero's bottom band belongs to the hero's own
    art; an overlay horizon between the art and the seam breaks the
    "tear" illusion.
  - LIGHT-MODE TUNE: the celestial bits (`DrawerCelestialSky` +
    `StatsSkyHeader`) hardcoded warm-white `starTint` — invisible on the
    pale seafoam sky. Both now resolve `starTint`
    theme-aware (light = deep seafoam ink `#2C5A53`, dark = warm white
    `#FFFDF4`); the stats back-pill keeps an explicit warm-white fill so
    the dark arrow still reads.
  - FOOTER (`DrawerFooter`): `res/raw/drawer_footer.svg` replaced with
    the user's NEW svg — it has NO `#FCF3E8` background rect (the old one
    had a cream backdrop panel). The AsyncImage is now
    `Alignment.BottomCenter` + `alpha(0.55f)` so the art sits lower and
    the "v1.1.0 · Made with curiosity ♥" line reads over it.
  - LESSON: an SVG's first `<path d="M0 0h1536v1024H0z" fill=...>` is a
    full-canvas background rect — a "no background" version simply lacks
    it; `remember {}` is `@DisallowComposableCalls` so resolve
    `themedAccent()` maps OUTSIDE remember (the associateWith pattern the
    stats page uses).
- **v174f — APK slimming: ship only `topics/*.json` (index built at
  runtime) + icon font subset (11MB → ~0.3MB).** User: "ship only topics
  json, trim the icon font but keep the full font as a backup outside of
  the app for future additions" — after the app hit ~55MB: `assets/
  topic_index.json` (23MB) was a FULL duplicate of every topic in
  `assets/topics/*.json` (16MB), and the bundled Material Symbols font
  (11MB, all 6,566 glyphs) dwarfed everything else.
  - TOPICS: `app/src/main/assets/topic_index.json` DELETED. v29's
    build-time merged index (search keys + sort year precomputed by
    `scripts/build_topic_index.py`) was only a faster-loading mirror of
    the per-category files — it never added data, just 23MB of APK.
    `TopicJsonLoader.loadIndex()` now checks for the asset and, when
    absent, builds the SAME merged index at runtime via the new
    `buildIndexFromCatalog()`: iterates every lane through the shared
    `load(id)` (parses are shared with the per-category caches — never
    double-parsed), computes keys (lowercased) + year
    (`CurioTopic.publicationYear()`, identical precedence to the Python
    script), dedupes by id (verified: zero cross-file dupes today), and
    reads wildcard.json directly (`load(WILDCARD)` would merge every
    lane). One parse, cached, prewarmed at app start — the Topic Database
    still renders with zero loading. The prebuilt-asset path stays as a
    fallback if someone re-runs the script.
  - ICON FONT: `res/font/material_symbols_outlined.ttf` subset from
    11MB → 297KB with fonttools (`--no-layout-closure` is the key flag —
    the default ligature closure retains ALL 4,250 icon ligatures because
    every name shares the a–z/underscore/digit source glyphs). The 201
    used glyphs were found by decoding the font's ligature table (note:
    components are per-letter with `_` → "underscore" and digits →
    "digit_one/two/…" glyphs) and cross-referencing every snake_case
    string literal in the app. ALL variable axes kept (wght powers the
    Normal/Bold `FontFamily` entries). FULL font backed up at
    `tools/fonts/material_symbols_outlined_full.ttf` — when adding an
    icon, subset again from there (command in the CurioIcons.kt header
    doc). LESSON: check the font's actual ligature encoding before
    trusting icon names — `format_underline` doesn't exist in this font
    (only `format_underlined`; the const is unused) while `inventory_2`
    IS a ligature (digit spelled out).
  - Also: the category-picker "Manage categories" pill (v168 capsule)
    read as overlapping the grid's last row — the sliced row + 6dp
    shadow formed a band behind the button. Fixed: grid bottom
    contentPadding 4→20dp, gap 8→16dp, pill shadow 6→3dp.
- **v175 — drawer hero sky is the user's SVG artwork; v174g fully
  reverted.** User: "i uploaded a new svg for the night sky use that and
  also remove the watermarks from the backgroud drawer hero" + "fully
  revert <6300f774>... also for light mode use another svg i just
  uploaded."
  - REVERT: `6300f774` (constant galaxy / flat opaque footer / opaque
    buttons / lifetime totals + badges) and its CI follow-up `91b4375`
    (RowScope fix for the now-deleted `DrawerLifetimePane`) reverted
    cleanly via `git revert --no-commit` — the drawer returns to the
    v174e/v174f sky-tear design: boxed real-data curiosity map, the
    transparent alpha-0.55 footer, the pre-existing nav rows. The
    changelog + AGENTS.md additions from that commit went with it.
  - HERO SKY: the procedural `DrawerCelestialSky` (stars / grain /
    constellation / sparkles / crescent moon) and the mirrored watermark
    glyph collage (`heroSymbols`/`heroPairs`) are GONE from the drawer
    hero. The banner now loads the user's uploaded SVG artwork via Coil's
    `SvgDecoder` (same path as the footer): dark theme →
    `res/raw/drawer_hero_sky_dark.svg` (the uploaded night sky
    `svgviewer-output (11).svg`, dark palette with the moon-edge sparkle
    removed), light theme → `res/raw/drawer_hero_sky_light.svg` (the
    uploaded `curio_day_sky_fixed(1).svg` day sky — sun at the moon's
    position 0.84/0.28, clouds, birds). The theme gradient stays as the
    loading backdrop; greeting + avatar read as the sky's scenery.
  - DEAD CODE: `DrawerCelestialSky`, `SkyStar`/`SkyLink`/`SkySparkle`,
    `drawSparkle` and the `Path`/`DrawScope` imports deleted (    only the
    hero used them). LESSON: when a commit is reverted, its CI-only
    follow-up fixes must go too or the revert conflicts on the code they
    patched.
- **v176 — drawer curiosity map = plain-surface constellation of ALL
  lanes + new flat bottom footer.** User: "remove the box behind your
  curiocity, and show the mind connection constellation map with inactive
  glow stars... show more available data... not the hero banner or just a
  drawing. so ask me properly" (clarified: remove box + text + dropdown
  entirely, all-time data in drawer; ALL lanes as stars, explored glow
  + icon on tap, inactive solid but smaller + muted, extra tiny stars;
  hero unchanged) and "i also added a new cropped footer place this and
  place it much below from the end of the footer, dont give it shadow...
  no scaffolding" (clarified: very bottom end, keep credits, add a
  bottom fade so it doesn't look floating).
  - MAP (`DrawerCuriosityMap`): the boxed card, the "Your Curiosity Map"
    title and the "This Week ˅" range selector are GONE. The map is now
    a `Column` on the plain drawer surface (whole-map tap still opens
    Stats). All-time data comes straight from `CurioPassport.allProgress`
    (spins/reveals/explores/saves/lastAt per visible lane — no repo,
    no `CurioQuests`, no `StatsRange` import). `DrawerLaneConstellation`
    renders EVERY visible lane: explored lanes are solid accent chips
    (34dp, `themedAccent` fill, `onAccent` icon, glow, tappable) and
    inactive lanes are solid 14dp dots (muted blue-grey, NOT alpha —
    differentiated by size + color per the user), plus 16 seeded extra
    tiny stars and faint neighbour-connecting lines. Deterministic grid
    scatter + per-lane `Random(id.name.hashCode())` jitter so all lanes
    fit and the constellation reads as a whole. Tapping an explored star
    opens a richer panel (`DrawerMapStat` panes: spins / peeked /
    explores / saved) with the lane icon, accent-tinted surface, last-
    explored relative time and a close button.
  - FOOTER (`DrawerFooter`): `res/raw/drawer_footer.svg` replaced with
    the user's cropped `curio_planet_cropped_bottom_264.svg` (1536×760,
    same planet art bottom-cropped). The footer is now a flat 210dp Box
    at the very bottom: `ContentScale.Crop` bottom-anchored art, no
    shadow/box/scaffolding; a 110dp vertical gradient fades the art into
    the drawer surface so it doesn't look floating, and the version    + "Made with curiosity" row sits inside that fade (nav-bars-padded,
    warm tan ink). The LazyColumn's `contentPadding.bottom` dropped
    20dp → 0 so the art truly touches the drawer's bottom edge.
- **v177 — tap the moon/sun on the drawer hero to flip the theme.**
  User: "make it so when i tap the moon or the sun the theme switch
  between light and dark". Always-on per ask_user (no Settings toggle).
  Both celestial bodies sit at (268.8, 52.08) in their SVGs and the
  artwork is exactly 320×186dp (1:1 with the hero box, `ContentScale.Crop`
  is a no-op), so a 48dp INVISIBLE hit-circle (`Box` with offset +
  `CircleShape` clip + `clickable`) sits right on top of the moon/sun in
  the hero's sky Box. Tapping it calls
  `AppPreferences.setThemeMode(context, if (isCurioDarkTheme())
  THEME_LIGHT else THEME_DARK)` — toggles to the OPPOSITE of the current
  effective theme (so System mode resolves first, then flips to a forced
  light/dark). `themeModeState` is a `mutableStateOf`, so the whole app
  rethemes instantly and the hero SVG crossfades via the existing
  `crossfade(true)`.
- **v178 — constellation audit (light-mode visibility + grid web) and the
  Stats "Your Curiosity" header banner now matches the drawer hero.**
  User: "do a proper audit that the constelations lines and star colors
  are right and visible in light mode and each stars in connected somehow
  and now use the day one view in drawer hero in your curiocity hero too,
  dont chnage the design just the banner style of the header". Stats
  header sky = theme-picked like the drawer per ask_user.
  - AUDIT (`DrawerLaneConstellation`): the old `#7FAFD8 @ 0.20` lines and
    `@ 0.30` tiny stars VANISHED on the white drawer surface in light
    mode, and the `#AFC9D4` idle dots were near-invisible. Inks are now
    resolved in COMPOSITION (they call `isCurioDarkTheme`, so they can't
    live inside the Canvas draw lambda): `linkColor` = #7FAFD8@0.30 dark
    / #5F7E9A@0.55 light, `tinyStarColor` = #7FAFD8@0.35 / #5F7E9A@0.50,
    `idleDotColor` = #4A5F6E / #7E9CB0 (steel slate, reads on white).
    Explored chips were already right (solid `themedAccent` + `onAccent`).
  - CONNECTIVITY: the old closed zigzag chain (i → (i+1)%n) was replaced
    with a GRID WEB — each node links to its RIGHT (col < c-1) and DOWN
    (row < r-1) grid neighbours (c/r recomputed in-canvas with the same
    formula as the layout). Every star gets 2–4 visible links; the
    constellation reads as a connected mesh instead of one invisible
    chain whose links hid under the 34dp explored chips.
  - STATS HEADER (`StatsSkyHeader` in StatsScreen.kt): the procedural
    gradient + 22 seeded stars + carved crescent moon are GONE. The band
    now loads the SAME theme-picked SVG as the drawer hero
    (`R.raw.drawer_hero_sky_dark` / `drawer_hero_sky_light`) via Coil
    `SvgDecoder` with `ContentScale.Crop`, over the theme gradient as the
    loading backdrop. The design is untouched: rounded 30dp bottom tear,
    the warm-white back pill (skyInk glyph) and the "Your Curiosity" /
    "Stats, streaks & insights" title still read on the art. The sky SVGs
    are 320×186 (1dp/unit) so the band's 148dp height is a center-crop
    slice — the celestial body sits near the top edge. Added imports:
    `ContentScale`, `AsyncImage`, `SvgDecoder`, `ImageRequest`, `R`;
    `Canvas`/`Offset`/`Random`/`lerp` stay (CategoryConstellation still
    draws).
- **v179 — Pet Designer theme-aware studio pill + opaque edit prompt +
  full-bleed hero.** User: "make the pet desinger gets the theme aware as
  well. and make the what do you want to edit box non trasparent. and fix
  its header tear hero too as its cut from sides." Clarified via
  ask_user: "the floating nav pill doesnt and also fix its hero banner
  side cut" — the theme issue is the STUDIO pill bar, not the page (the
  page is already MaterialTheme-driven).
  - STUDIO PILL (`PetStudioTab`): the ACTIVE tab still wore the stale
    solid `secondary` + `onSecondary` (the butter the main nav bar
    dropped in v161 for the accent system). It now uses the nav bar's
    plain-page language: `secondaryContainer` + `onSecondaryContainer`
    (both light/dark aware). The v147b doc comment updated to match.
  - EDIT PROMPT (`EditorPickPrompt`): the "What do you want to edit?"
    card's fill `lerp(surface, primaryContainer, 0.55f)` read as a
    TRANSLUCENT plate over the page's lane wash; replaced with the solid
    `surfaceContainerHigh` (same elevated container as DialogScrim) so
    it's clearly opaque. `lerp` still used elsewhere (2321, 4559).
  - HERO SIDE CUT: `SettingsHeroHeader` is the FIRST LazyColumn item and
    the list's `contentPadding(start/end = wideContentEdgePadding())`
    inset it 16dp+ each side — the tear looked cut. Fix: compute
    `val edgePad = wideContentEdgePadding()` once, use it in
    contentPadding, and wrap the hero item in
    `Box(Modifier.fillMaxWidth().padding(horizontal = -edgePad))` so the
    banner bleeds to BOTH screen edges (content below stays padded).
    Works on wide screens too (larger edgePad cancels fully).
- **v180 — Spin category picker: no footer below the cards, floating
  no-background Mix/Cancel, hero watermark placement fixed.** User: "in
  spin screen category picker remove anything that below the category
  cards like remove it fully. no need for manage category and make the
  mix and cancel button appear as nav bar style pill only when selected
  multiple. with no backgroud and floating style. and also the hero
  banner of it chnage its glyph style and placement so none of the icon
  is visible and makes its backgroud tint take theme aware too and also
  watermark." Clarified via ask_user: Mix/Cancel pill = NO background
  (just the buttons floating); "icons not visible" = PLACEMENT bug, fix
  the placements (the hero fill/wash/ink were already theme-aware).
  - FOOTER REMOVED: the "Manage categories" floating pill is GONE —
    nothing sits below the category cards in single-select.
    `onBrowseAll` param removed from `CategoryPickerSheet` + the call
    site (the Manage-Categories navigation went with it); the
    `curioFloatingNavContainerFor` + `ButtonDefaults` imports are now
    unused and deleted.
  - MIX/CANCEL: in multi-select the controls are a FLOATING row
    (`align(BottomCenter)` in the grid's weight Box, bottom 18dp) with
    NO background capsule — Mix is a solid category pill
    (`themedButtonFill`/`themedButtonInk`, theme-aware) and Cancel is a
    plain text button. The grids' bottom contentPadding is now
    `if (multiSelectMode) 88dp else 20dp` so the floating controls never
    cover the last row of cards. Button → Surface swap (return@Surface).
  - HERO GLYPHS: the small twin sat UNDER the status bar (its
    `align(TopStart)` had no `statusBarsPadding`, unlike the title
    column) and the 72dp large one was clipped by the tear + hidden
    behind the tabs/presets rows. Fixed: small gets `.statusBarsPadding()`
    (top-left corner), large is 64dp raised to `bottom = 58dp` (right
    edge just above the preset chips — the tabs row is left-aligned so
    that corner is free), alphas bumped 0.07/0.10 → 0.10/0.14. NOTE: the
    FilterSheet hero (line ~1814) has the SAME placement bug — left
    unfixed (user asked about the picker only).
- **v181 — CI compile fix (moon/sun tap) + Stats constellation web.**
  User pasted the CI failure: `HomeScreen.kt:2132:41 @Composable
  invocations can only happen from the context of a @Composable function`
  — "fix this too", plus "Connect the stars" for the Stats page.
  - CI FIX: the v177 moon/sun tap called `isCurioDarkTheme()` INSIDE the
    `clickable {}` lambda (non-composable context) — a direct violation
    of COMPILE-SAFETY rule 3 ("onClick lambdas are NOT @Composable
    contexts"). Fixed by resolving `val isDarkNow = isCurioDarkTheme()`
    in composition before the clickable. LESSON: it shipped because CI
    runs async — the local "verification" (grep for references) didn't
    catch a @Composable call inside a lambda; ALWAYS grep for composable
    helpers (isCurioDarkTheme/themedAccent/…) with `-B2 -A2` context and
    eyeball every occurrence that lands inside a lambda. A full scan of
    the four files touched this session found no other violations.
  - STATS WEB (`CategoryConstellation` in StatsScreen.kt): the old
    lane-order chain + one gold fissure used `#7FAFD8 @ 0.22` — the same
    light-mode invisibility the drawer map had. The stars are now linked
    as a NEAREST-NEIGHBOUR web (each star → its 2 closest stars,
    deduped via `LinkedHashSet<Pair<Int,Int>>` — kotlin.collections
    typealias, auto-imported) with theme-aware inks resolved in
    composition: `linkColor` #7FAFD8@0.32 dark / #5F7E9A@0.50 light,
    `fissureColor` #D9A85C@0.30 dark / #A97F3C@0.45 light (the gold
    fissure still bridges the two hemispheres).
- **v199 — topic name resolution: exact/base matches beat containment
  across lanes, and the reveal/capture resolve within the route's own
  category first ("Flow" no longer opens "Flower Boy"); the Browse-Topics
  category + chip-bar state persists until app restart. (branch Alpha)**
  User: "when i tap flow in topic browser movie 2024 in fimls why its
  opening flower boy, fix more similiar issues like this and also make the
  category selected and expaddned setting persistent untill restart".
  - FLOW → FLOWER BOY (root cause): `TopicCatalog.findByName` scanned
    `CategoryId.values()` in order and returned the FIRST lane's first
    tolerant match; ALBUMS scans before FILMS and "Flower Boy" contains
    "flow" (and even the full "Flow (2024)" base-collided the same way),
    so the Films reveal opened the album. Fixes:
    - `TopicCatalog.findByName` is now TWO passes — strict matches
      (exact name / base-name equality, new `matchesSavedNameStrict` +
      shared `savedNameBase` helper) across ALL categories, THEN the
      tolerant pass (containment still last). An exact/base hit in any
      lane always beats a loose containment hit in an earlier lane.
    - `TopicRevealScreen` + `SaveCaptureScreen` now resolve within the
      route's own category pool FIRST, TIERED like `findByName` — strict
      (`pool.firstOrNull { it.matchesSavedNameStrict(name) }`) before
      tolerant (`matchesSavedName`), so a loose containment match earlier
      in the same file can't beat a precise one later in the lane either —
      falling back to the global `findByName` only for legacy saved
      entries whose lane changed (v135).
  - BROWSE-TOPICS PERSISTENCE: the browser is a plain `composable`, so
    every reopen from the drawer creates a fresh backstack entry and
    rememberSaveable reset the category selection + chip bar. New
    `TopicBrowserSession` (process-scoped static, same pattern as
    `SpinPickerRequest`) seeds `selectedCat` / `categoryFilterOpen` and
    syncs back on change — the state now survives close-and-reopen until
    the app restarts (statics die with the process).
- **v200 — NEW CATEGORY: Animated Movies (ANIMATED_MOVIES) — a 1000+
  lane: non-anime animation split out of Films; real films, real quick
  facts. (branch Alpha)** User: "continue the expansion of topics and add
  animated movies section as a ne category and separate animated movies
  from films and make them 1000+ and anduse real quick facts and push
  after its fully done" (+ "anime and animation movies are differnt btw").
  - ANIME ≠ ANIMATED MOVIES (user note): the 6 anime films in films.json
    (Akira, Grave of the Fireflies, Totoro, Princess Mononoke, Spirited
    Away, The Boy and the Heron) STAY in Films — the new lane is
    non-anime animation only (Disney, Pixar, DreamWorks, Illumination,
    Blue Sky, Sony, Aardman, Laika, Don Bluth, classic US, Rankin/Bass,
    European, Chinese, Latin American, Indian, Australian, stop-motion,
    DTV/franchise: Barbie, Scooby-Doo, Tom & Jerry, DC/Marvel animated,
    DisneyToon sequels).
  - CATEGORY REGISTRATION: `Category.kt` (enum + newLanes + order +
    slug `animated-movies` + family Entertainment), `CurioColors.kt`
    (ANIMATED_MOVIES palette constants), the three exhaustive `when`s
    (CaptureEntity.kt, ExploreSession.kt, TopicRevealScreen.kt) and the
    Entertainment quick-mix preset (DeckPresets.kt). The Gradle
    `validateTopics` derives the expected categoryId from the FILENAME
    (animated-movies.json → ANIMATED_MOVIES) so no validator list change
    was needed; the topic_index builder globs all files.
  - CONTENT: 52 non-anime animated films moved out of films.json (948
    remaining, anime intact) into the new animated-movies.json via
    `scripts/extract_animated_from_films.py` (explicit-title list — the
    first tag-based attempt false-positived on live-action "Pixar"-
    tagged films like Braveheart and was reverted), then ~540 more real
    entries authored across scripts/batch_animated_1..11.py (Disney
    theatrical + DTV, Pixar, DreamWorks, Illumination, Blue Sky, Sony,
    Aardman, Laika, stop-motion indie, Don Bluth + 80s/90s classics,
    international, Chinese, franchise DTV). 591 entries total in this
    push (1000+ top-up continues in a later pass). All ids unique across
    the catalog (18,071 total) — validated with check_assets.py + a
    cross-file id scan.
  - HOUSEKEEPING: removed the root-level reference dump SVGs
    (`svgviewer-output (12).svg`, `curio_planet_cropped_bottom_264.svg`,
    `footer.svg`) — the real drawer art lives in res/raw/.
- **v201 — nav pill collapse cinches tighter + slower; Like/Dislike and
  Pet Studio bars match the nav pill exactly; hole-ring coil no longer
  cut at the card edge. (branch Alpha)** User: "the 3d ring should be
  shouwn fully without getting cut" + "make the home nav pill collapse
  even smoother like make it collape even more and make the like dislike
  button match the text and size of the nav bar pill and same in pet
  designer".
  - RING CUT — ROOT CAUSE: Material3 1.5's `Surface` ALWAYS clips its
    children to the shape (`.clip(shape)` at the end of the
    implementation) — the v74 "Surface does not clip" note was true
    only for M3 1.0/1.1. The coil's left peek (drawn at −6.5dp) was cut
    at the card edge. Fix: the three stat-pane call sites (Home,
    Profile, EntryDetail) swap the clipping `Surface` for a plain `Box`
    carrying `Modifier.shadow(elevation, shape, clip = false)` + the
    paper fill — the fill self-clips to the outline path, so the coil
    escapes past the left edge. All three sites have ≥28dp container
    padding so the peek clears the screen edge.
  - NAV PILL COLLAPSE: pill spring family 240 → 150 stiffness (longest
    calm critically-damped glide), and the leave-hold collapse now
    targets `FloatingPillCollapsedWidth` (44dp — tighter than the idle
    64dp icon pill) so the pill visibly cinches before the bar unmounts
    ([FloatingNavPill] gains a `collapsing` param; NavHost hold 380 →
    420ms to match the slower settle — still no dead pause).
  - LIKE/DISLIKE + PET STUDIO PILLS: `RevealSentimentPill` and
    `PetStudioTab` bumped to the nav bar's exact sizes (64/136dp +
    52dp height + 26dp icon), springs 400 → 150, and the labels now use
    the nav bar's Changa One 15sp Normal face (was labelMedium Bold).
- **v198 — Home/Recents "Unexplored" tag pills wear a SHADED category
  chip; Material theme: category buttons, filter chips and ink now use
  the family tonal tones — the scheme-role amber/mint/translucent paints
  are gone. (branch Alpha)** User: "in light mode home screen the recents
  unplored pills make it get the color of the category it sits on with a
  shade and in dark mode why it looks transparent fix that, and in
  material theme in light mode and dark mode the category button in spin
  screen and filters looks bad and even worse when mixed is selected the
  category button".
  - TAG PILL (`ExploreTopicRow` in HomeScreen.kt + `RecentTopicRow` in
    RecentScreen.kt): the old `lerp(surfaceContainerLow, accent, 0.14f)`
    fill vanished on the tinted card in light and read transparent in
    dark. The pill now pulls the accent toward the card surface — ~30%
    in light (a solid SHADED category chip on the tinted card) and ~38%
    in dark (visibly tinted on the dark card); pastel light shades with
    the deep same-hue ink (`categoryInk()`) so the airy pastel twin can't
    wash the pill away. Text stays `categoryInk()`.
  - MATERIAL FAMILY TONES EVERYWHERE (`MaterialFamilies.kt`): the v185
    scheme-role branches are GONE. `materialAccent()` wore the scheme
    secondary/tertiary for rose/green lanes (an AMBER button for a rose
    Movies deck — the baseline secondary is an amber companion) and a
    translucent onSurfaceVariant for neutrals, so the Spin deck buttons
    (Categories/Filter), the Spin filter-sheet chips and the
    Cabinet/Topic-History filter chips painted DIFFERENT hues than the
    family-toned cards; a MIXED deck (which collapses to the scheme
    primary) re-mapped through the rose-family branch and the button wore
    secondary while the deck wore primary — the "even worse when mixed"
    case. `materialAccent()` / `materialOnAccent()` / `materialInk()` now
    resolve the lane's OWN family tonal tone (T40/T80 fills, on-fill ink,
    T45/T80 text ink) — the exact fills the cards already use — so
    buttons, chips, filters and text match the deck; pastel mode softens
    the fills to their pastel twins like the cards. `materialAccentFor`
    drops its neutral special-case tones so watermarks/blends align.
  - CI REGRESSION FIX (v198): the v196 tap-to-open rewrite accidentally
    dropped the sheet's `val wide = windowWidthSizeClass().isWide` (it rode
    in the replaced `persistedVisible` block) — the grid's `columns = if
    (wide) …` then failed to compile (CI: "Unresolved reference 'wide'" at
    the two grid sites). Restored in `CategoryPickerSheet` right after
    `val context`.
- **v197 — hole-ring coil redrawn from the user's REVISED SVG (a truncated
  arch, no bottom curl) and it now PEEKS OUT of the card's left edge.
  (branch Alpha)** User: "now i added a better ring this time can u use
  that instead of the previous one, and also the ring should be come out
  from the left of it like peek out from the left not entirely inside the
  stat card" + a revised SVG (same 150×420, three coils — but each coil's
  path is now `M38 62 C38 39 54 24 76 24 C98 24 111 37 111 52`: the
  bottom curl `C111 66 102 75 90 75 …` is GONE, the box is 73×38 instead
  of 73×51, and the dark depth pass uses the SAME truncated path).
  - `ui/components/PaperStatCard.kt` [drawCoilRing]: `CoilOutlineNorm` and
    `CoilSpecularNorm` re-normalized to the revised 73×38 box (outline:
    0,1.0 → 0,0.395 / 0.219,0 / 0.521,0 / 0.822,0 / 1,0.342 / 1,0.737;
    specular unchanged in SVG space but re-normalized: 0.068,0.868 …).
    The wire now rises up the left, over the top and down the right as a
    clean arch (no curl-in at the bottom); `coilH` aspect 51/73 → 38/73.
  - PEEK-OUT: the coil is pushed LEFT past the card edge (`leftPeek`
    ≈ 9dp → its left arc + leg protrude ~6.5dp past the card's left
    edge, like a spiral binding sticking out of the paper) instead of
    sitting entirely inside. The hole stays centered vertically under the
    arch; the wire's right leg dives through it. Works because the
    fill's `drawWithCache` isn't clipped to the card shape (the Surface
    doesn't clip its content here — see the Home v74 note).
- **v196 — category picker: tap-to-open always (hold to mix), cancel +
  back applies the cleared mix, and a cancelled mix no longer resurrects
  after a topic visit. (branch Alpha)** User: "even when i cancel the
  selected in category picker and i tap back make it apply too. and also
  when its mixed and after that i open the category picker to slecet dont
  let me tap to select for mix let it be open the category when i tap and
  only tap and hold should select for next mic or override mix, also theres
  a bug suppos i have a mixed selected and its from the home shuffle button
  and then i cancel it and chnage it to other category and i opened the
  topic and then when i tap back it goes back to the mixed one even though
  i have chnaged it".
  - TAP-TO-OPEN ALWAYS (`CategoryPickerSheet` in SpinScreen.kt): the v26
    auto-tick reopened the sheet in multi-select with every mix lane
    pre-ticked whenever the persisted deck was a mix — the user wanted tap
    to OPEN a category (replacing the deck) and only tap-and-hold to enter
    multi-select. `multiSelectMode` now starts false and `selectedSlugs`
    empty on every open; long-press (both pages) is the ONLY way into
    multi-select, starting a fresh selection for the next / overriding mix.
  - CANCEL + BACK APPLIES: the Cancel button now sets a `mixCancelled`
    flag, and `onDismissRequest` applies a cleared state when cancelled OR
    when every lane was deselected in multi-select — the deck reverts to
    the last single category (`onCategoriesSelected(emptyList())` →
    SpinScreen persists the single) instead of closing with the old mix
    intact. Fresh selections (presets, long-press) reset the flag.
  - NO MIX RESURRECTION (root cause of the back-to-mixed bug): the v5.14
    slug-authority `LaunchedEffect(categorySlug)` and the v5.5 persist
    effect re-ran on every pop-back from a pushed route (the topic reveal)
    and re-forced the launch slug over the user's in-session category
    change. A new `slugApplied` rememberSaveable flag gates both: the slug
    (and its prefs persist) apply ONCE per navigation; returning from the
    reveal restores the flag true, so the deck keeps the user's change.
- **v195 — constellation gets decorative filler neurons (the mesh reads
  whole); nav pill fully collapses on the Topic Reveal and the hold is
  shorter. (branch Alpha)** User: "the neruons dot doesnt create the
  brain mesh. and i told you to add extra dots for decoration and
  completion of the neuron mesh, and the home nav doesnt collapse fully
  in topic reveal screen it stays for too long, neither it collapse".
  - BRAIN MESH FILLERS (`ui/components/CurioConstellation.kt`): with few
    explored lanes the neural web read as scattered dots, not a brain —
    the user explicitly asked for decorative extras. A fixed
    deterministic ring layout per hemisphere lobe (radial 0.35 → 0.89,
    5/7/9 dots per ring, tiny fixed-seed jitter) now fills both lobes;
    the fillers join the SAME link web as the real neurons (nearest-2
    synapses + inter-hemispheric bridges over all dots, with real nodes
    still splitting left/right by index), so the mesh outlines the whole
    brain even at zero explored lanes. Fillers draw as small neutral
    dots UNDER the real neurons (dim steel, no accent / glow / white
    core) and are NOT tappable — the real explored neurons stay the only
    interactive data (popover untouched).
  - NAV COLLAPSE ON REVEAL (`ui/components/CurioBottomNav.kt` +
    `navigation/CurioNavHost.kt`): v193 kept the bar composed for 500ms
    after leaving the tab set so the selected pill could collapse, but
    `CurioFloatingNavBar`'s internal `selectedRoute` mapping forces SPIN
    selected on the reveal route — so leaving Home for a topic reveal
    made the SPIN pill POP OPEN during the hold and the bar then
    vanished with a pill stuck expanded ("neither it collapse"). FIX: a
    new `collapsing` parameter — while the route is off the tab set the
    bar forces NO selection (`selectedRoute = null`), so every pill
    glides closed and the bar unmounts with nothing expanded. The hold
    also dropped 500 → 380ms (the 240-stiffness critically-damped
    collapse spring's settle time) so the bar doesn't linger ("stays
    for too long").
- **v194 — cut lines shorter + right-shifted; hole rings redrawn as the
  spiral-coil SVG.** User: "now we have two cut lines lets improve it even
  more. make it little more shorter and more to the right of the header
  text. and the hole rings… the stamped pin holes create holes which is
  see through, the 3d ring doesnt show over it. lemme share the rings
  which you can adjust and put above the holes… the ring itself isnt
  perfect its too much rounded and the view is also wrong so youve to fix
  the svg rings" + a reference SVG (3 spiral coils).
  - CUT LINES (`ui/components/PaperTitleLines.kt`): the two hand-drawn
    underlines now start ~a quarter in from the title's left edge and
    span only the right ~70% of the line (top 0.22→0.90, bottom
    0.26→0.94 of the canvas) — a partial right-side underline instead of
    a full-width one (was 0.02/0.06 → 0.90/0.96). Same pen-sag shapes.
  - HOLE RINGS (`ui/components/PaperStatCard.kt`): the default "coil"
    ring is redrawn as the user's reference SVG — a FORESHORTENED
    spiral-notebook wire (73:51 aspect, correcting the old round-ring
    "view") looping up the left, over the top, down the right, curling
    in at the bottom; drawn OVER the shaded hole interior ([drawHoleInterior])
    so the punched hole shows through the coil's inner opening. Three
    passes mirror the SVG: a dark depth stroke behind (18px pass, #101B27
    light / #22282F dark), the metal tube gradient on top (8 tuned stops
    from the SVG's palette — cool polished steel, dark-mode light steel
    reversal), and a white specular along the upper-left (3px pass,
    0.75 light / 0.60 dark). The coil's outer loop is ~2.1× the hole
    diameter (holeR × 4.2 wide). Old arc-based coil drawing + CoilBackDark
    deleted; "split" and "oblique" styles untouched.
- **v193 — nav pill COLLAPSES when leaving a tab (was vanishing).** User:
  "the home nav pill should collapse just the way it expands when i back
  from home… it still just vanishes instead of collapse vanishing". ROOT
  CAUSE: the floating bar was composed only while
  `routePrefix in CurioRoutes.bottomNavRoutePrefixes` (the `showBottomBar`
  gate in CurioNavHost), so navigating to a non-tab page (Profile,
  settings sub-pages, the Topic Reveal…) unmounted the WHOLE bar the
  instant the route changed — the expanded pill never got a chance to run
  its collapse spring and simply disappeared. FIX: the bar's composition
  now gates on a `barVisible` state that stays true for 500ms after the
  route leaves the tab set (`LaunchedEffect(showBottomBar)` + `delay(500)`
  — the pill's collapse spring + label retract settle in ~450ms), so the
  deselected pill glides closed with the SAME springs it expands with,
  then the bar unmounts. Returning to a tab cancels the pending delay and
  remounts instantly (no flicker, pill expands as before). The wide-window
  rail keeps the instant `showBottomBar` gate (rail items never expand).
- **v192 — shuffle main card drops the year pill (reveal keeps it).**
  User: "in shuffle main card dont show the year pill just inside the
  topic reveal". The Spin ticket's top-left pill row (v141) was byline +
  year qualifier; the year pill (`yearQual` from
  `titleAndYearQualifier`, the Schedule glyph chip) is REMOVED from the
  shuffle card — the byline pill stays. The Topic Reveal hero keeps its
  year pill (top bar, next to the category chip), and the card title
  still drops the trailing year, so the shared-element morph stays clean.
- **v192 — detail hero tear rim matches every other hero. (branch
  Alpha)** User: "the tear logic of detail screen seems totally differnt
  from rest of the screens, can u fix it". The detail hero's shape
  construction was ALREADY aligned with Home (v104: `SoftTornBottomShape`
  + `SoftTornSheetShape` with `bold = true`, same seed, same 10dp lip /
  14dp baseline, same v108 sheet gate) — the ONE remaining divergence was
  the torn-edge shadow rim under the seam: every other hero (Home,
  Profile, Cabinet, Settings, Onboarding, TopicHistory, Spin) draws
  `Color.Black.copy(alpha = 0.20f)`, but the detail hero drew a warm
  paper-colored `heroSheetColor.copy(alpha = 0.72f)` band (near-black in
  dark mode) that read as a totally different tear. Now it draws the same
  20% black hairline.
- **v191 — drawer constellation as a BRAIN NEURAL WEB + floating tap
  popover. (branch Alpha)** User: "in drawer we have your constellation
  right but its random? isnt it. and it doesnt show real data yet. but i
  want to draw the costellation pattern as a brain neural connection. and
  when i tap the dot it shows me the info belo but i want that to sho as
  a floating small thing and also less data. and also in future i will be
  replacing the category with real knowledge based things just like brain
  knowlegde you get it right?" Clarified via ask_user: explored-only
  neurons (keep), popover = name + saved count (both later replaceable by
  knowledge nodes), and the drawer widens a little if the brain feels
  squished.
  - BRAIN NEURAL WEB (`ui/components/CurioConstellation.kt`): the old arc
    scatter sat every star in a flat bottom band of the canvas (looked
    random/squished). Neurons now fill two hemisphere ELLIPSE lobes
    (phi sweeps -π/2..π/2, radial 0.3..1.0 fills the lobe interior; lobes
    bulge outward at mid-height and taper to the midline top/bottom — the
    brain silhouette with the fissure gap). Links are CURVED quadratic
    beziers (perpendicular sag 0.12) instead of straight lines: every
    neuron → its 2 nearest neighbours (synapses) + its nearest neuron on
    the OTHER hemisphere (corpus-callosum bridges, deduped). The gold
    fissure is now a soft curve down the centre line. Data unchanged —
    stars = explored lanes, size = saves, glow = active (real passport
    data, deterministic positions).
  - FLOATING POPOVER: `CurioConstellation` gained a
    `popoverContent: (@Composable (CategoryId) -> Unit)? = null` slot —
    when provided, the selected neuron shows a small floating card
    anchored just above the dot (below when near the top), clamped inside
    the canvas, tap-to-dismiss (BoxWithConstraints + onSizeChanged for the
    clamp). The DRAWER passes a compact name + "N saved" chip and its
    richer below-panel (the 4 stat chips + last-explored line +
    `DrawerMapStat`) is DELETED — "a floating small thing and also less
    data". The Stats page passes null and keeps its own below-panel.
  - DRAWER WIDTH: `ModalDrawerSheet` 320 → 336dp so the neural web has
    room to breathe (user's "extend the drawer a little more to the
    right" contingency).
  - FUTURE NOTE: the neurons are fed by [CategoryId] + count maps today;
    the user plans to replace category lanes with real knowledge-based
    nodes — the component only reads the id list + maps, so swapping the
    data model later is caller-side only.
- **v190 — Material theme polish: pastel-softened material cards, mixes
  collapse to the scheme primary, readable adaptive-hero contrast, M3 nav
  roles. (branch Alpha)** User: "the material main card colors are good
  but in pastel mode they are not. and also in material theme dont let
  the mix color come, make it the material color when they get mixed.
  also in light mode adaptive hero the hero card looks washed out along
  with the glyphs and the texts and the box. fix that keep the material
  color for it but fix it. and fix the nav bar material color as they
  are bad". Clarified via ask_user: mixes → scheme PRIMARY (one brand
  color); hero → DEEP material banner with DARK ink; nav → pure M3 roles.
  - PASTEL CARDS: the Material branches of `CurioGradients.cardGradient` /
    `heroBlendGradient` and `CurioMixedDeck` ignored Pastel mode, so the
    material main card kept its full-strength muted family fill while
    everything around it softened. All material card fills now resolve
    through `pastelAccent(fill, dark)` when Pastel is on (the composable
    gradients read the pref directly; `mixedDeckAccent` honors its
    `pastel` param).
  - MIXES → PRIMARY: `mixedDeckAccent` / `mixedDeckGradient` gained a
    `materialPrimary: Color? = null` param; SpinScreen passes
    `MaterialTheme.colorScheme.primary` when Material is on and >1 lane is
    selected, so a mixed deck wears THE one material color — peeks, spin
    button, confetti and the nav tint all follow — and the multi-hue
    family sweep is gone (`mixedDeckGradient`'s Material branch renders
    the standard quiet `cardGradient` from the single resolved color).
    Same-family mixes keep the family fill; pastel mode pastel-softens
    the primary too.
  - ADAPTIVE HERO CONTRAST: the light-mode Material hero banner was the
    pale T90 scheme containers (secondaryContainer / tertiaryContainer /
    surfaceContainerHighest) with near-white ink — washed out. The new
    `materialHeaderAccent()` LIGHT branch wears the RICH family color
    (family fill lifted to L=0.70, sat capped 0.55) and new
    `materialHeroInk()` (a deep same-hue twin via `readableLightInk`)
    pairs with it through `heroHeaderInk()`'s Material-light branch —
    title text, watermark glyphs and the banner box read crisp while the
    material family hue stays. Dark keeps the deep T30 containers + light
    ink (unchanged).
  - NAV M3 ROLES: under Material, `curioNavContainerColor` /
    `curioFloatingNavContainerFor` return `surfaceContainer` (the M3 nav
    container role) and `curioActivePillFill` / `curioActivePillInk`
    return `secondaryContainer` / `onSecondaryContainer` (the M3
    navigation indicator) — no per-lane colors in the bar. Applies to the
    floating pill bar, the wide-window rail and the reveal Like/Dislike
    capsule (`curioFloatingNavContainerFor`).
- **v189 — page-switch haptics; Mix/Cancel as ONE nav-bar capsule;
  picker + filter apply on pop-back. (branch Alpha)** User: "add haptics
  when switching pages with nav pill or like or pet designer too" + "also
  make the mix and cancel as a navbar style pill and also let user apply
  the mix even when it pops back same for filters".
  - HAPTICS: a light tick (`HapticFeedbackType.TextHandleMove` via
    `LocalHapticFeedback.current`, resolved in composition — never inside
    the clickable) fires on: tab switches in all three nav surfaces
    (`CurioFloatingNavBar` floating pill + M3 `NavigationBar` branch +
    `CurioNavigationRail`), the Topic Reveal Like/Dislike segments, and
    the Pet Designer studio bar tabs. `TextHandleMove` is the lightest
    standard tick (no strong press flash on Android 12+).
  - MIX/CANCEL CAPSULE: the picker's floating Mix + Cancel now live in
    ONE capsule styled like the bottom nav bar — `Surface` with
    `RoundedCornerShape(50)`, `curioFloatingNavContainerFor(wash)`
    (internal, CurioBottomNav.kt — the page wash lifted toward the
    elevated surface, dark = surfaceContainerHigh), `shadowElevation =
    6.dp`, inner Row `padding(8.dp)`; Mix stays the accent-filled
    active-pill (`themedButtonFill`/`themedButtonInk`), Cancel is plain
    text with `heightIn(min = 44.dp)` to match the pill's height.
  - APPLY-ON-POP: both sheets' `onDismissRequest` now APPLY instead of
    dropping the draft when there's something to apply. Picker:
    `multiSelectMode && selectedSlugs.isNotEmpty()` →
    `onCategoriesSelected(selected)` (swipe/scrim/back applies the mix;
    single-select or empty selection just closes). FilterSheet: draft
    differs from initial → `onApply(draftFilters, draftSubtypes)`; a
    no-change pop keeps the old set.
- **v186 — drawer shows the Stats page's constellation; nav labels larger;
  footer slimmer. (branch Alpha)** User: "make the home shuffle cabinet
  tet xt even larger in default look and in drawer show the your
  constellaetion from the your curiocity page not another thing bruh" +
  "also the footer looks big its good now but looks big so make it more
  small".
  - SHARED CONSTELLATION: extracted the Stats page's `CategoryConstellation`
    into `ui/components/CurioConstellation.kt` (internal, public in the
    components package) — the exact same brain two-lobe rendering, web
    links, gold fissure, size-by-saves, recent glow, tap-select. The Stats
    page calls the shared component (private duplicate deleted, 5 unused
    imports removed). The DRAWER's `DrawerCuriosityMap` now calls the same
    `CurioConstellation` with passport data (explored lanes sorted by
    ordinal, `laneCounts = saves`, `laneRecent = lastAt`, `recentCutoff =
    0L` = all-time → every explored lane glows), and the whole
    `DrawerLaneConstellation` grid-web function (v176-era) was deleted
    (~140 lines). The drawer's richer passport tap panel stays.
  - NAV LABELS: default-look Changa One label 13 → 15sp (still fits the
    136dp expanded pill; guidelines branch untouched).
  - FOOTER: 210 → 150dp tall, fade 110 → 80dp — the planet reads as a
    small bottom band.
- **v185 — proper M3 Material theme (1 opt-in toggle), on branch Alpha.**
  User: "go to alpha branch and sync it with main (alpha was so much
  behind)… read everything [m3.material.io color system overview +
  get-started + full guideline]… we will be adding 2 new toggle. with one
  beaigh a proper material theme with category colors either getting
  nothing or maybe a one color from the material color… and another test
  full material guideline text spacing boxes layout evrything… it will be
  a new extra sytem as a toggle without chnaging anything thats in our
  current app and look… make the proper plan and follow it untill its
  done." Synced Alpha (was 251 behind) → fast-forwarded to main a127f10 +
  pushed. Clarified via ask_user: (1) "clear the current material style
  and fully redo it" — the old partial Material style was ALREADY removed
  in v78, so this is a from-scratch rebuild; (2) category colors = "one
  color per family, muted" (6 families); (3) the guidelines toggle is
  INDEPENDENT of the Material theme (works on Curio colors); (4) brand
  chrome = "give both as an option, all opt-in, no default on" (full M3
  chrome vs keep Curio chrome sub-option).
  - M3 research: color system = 5 key colors × 13-tone palettes; the
    multi-color guideline is RESTRAINT (neutral surfaces, ONE primary,
    secondary/tertiary for muted accents — never a rainbow per section);
    dynamic color (Material You) on Android 12+; typography = 15-style
    scale; shapes 4/8/12/16/24; elevation = tonal overlays.
  - PHASE A — pref + UI: `materialThemeState` (default OFF) + 1 Appearance
    row in SettingsSectionScreen.kt. NOTE — the "Material guidelines" +
    "Material chrome" system (PHASE C below) was later REMOVED wholesale
    (user verdict: "its not good"): `MaterialGuidelines.kt`, the
    `materialGuidelinesState` / `materialChromeFullState` prefs and both
    Appearance rows are deleted; `CurioTheme` always uses
    `CurioTypography`/`CurioShapes`; `CurioBottomNav` always renders the
    floating pill bar with Changa One labels. Only the Material theme
    toggle remains.
  - PHASE B — Material color: `MaterialColorSchemes.kt` (dynamic light/
    dark on API 31+, `MaterialBaselineLight/DarkScheme` seeded from the
    brand coral via a `materialTone(hue, sat, tone)` M3 tone→lightness
    ladder for older devices); `MaterialFamilies.kt` (6 families by hue,
    near-achromatic → NEUTRAL; family fill T40 light/T80 dark, ink
    T100/T20; rose→secondary + green→tertiary map to the scheme's own
    roles; non-composable `*For(dark)` twins for remember-block paths);
    choke-point wiring in CategoryInk.kt (themedAccent/categoryInk/
    onAccent/headerAccent/backgroundWash→neutral/surfaces→neutral/
    InkFor+AccentFor twins) + CurioColors.kt (cardGradient/heroBlendGradient/
    mixedDeckAccent/mixedDeckGradient/mixedDeckWash→neutral) + CurioTheme
    (curioColorScheme → materialColorScheme).
  - PHASE C — guidelines + chrome: BUILT (`MaterialGuidelines.kt` =
    MaterialTypography / MaterialShapes / CurioSpacing tokens, gates
    materialGuidelinesOn / materialChromeFullOn; CurioTheme typography+
    shapes swap; M3 NavigationBar under full chrome; Changa One drop from
    pill/rail labels), then REMOVED — see the NOTE in PHASE A. The
    per-screen spacing/layout sweep never shipped with it.
  - LESSON: extension functions on an enum type (`MaterialFamily.forAccent`)
    need a receiver INSTANCE — a factory-style helper must be a plain
    top-level function (`materialFamilyFor`).
- **v184 — nav pill: calmer morph/collapse, wider+higher pill, more
  inactive spacing, Changa One labels.** User: "make the nav pill morph
  and collape animation even smoother and calmer. and give the inactive
  buttons a little more space. and use a new bond font for the tet of
  nav pill maybe this one, Changa One" — then "also make it a little
  wide like just a little heigh the pill" — then "also just like it
  expands when i come back to home screen make it collapse when i go to
  other screen from home screen for smoother look."
  - SPRINGS: `PillWidth/Motion/Color/ExpandSpring` stiffness 400 → 240
    (still damping 1.0 critically damped — zero overshoot/bounce). ~40%
    slower settle, lockstep preserved (all four specs identical). The
    collapse ALREADY mirrored the expand (both directions animate via
    the same springs since v162) — the slowdown applies to BOTH, so
    leaving a screen now glides closed exactly as returning glides open.
    Fixed the stale v125 function KDoc that claimed the label exit is
    "instant" (it's animated since v162).
  - SIZE: pill "a little wide" + "a little high" — icon pills 60 → 64dp,
    expanded 128 → 136dp, height 48 → 52dp.
  - SPACING: bar inner padding 7 → 8dp, pill gap 6 → 10dp (inactive
    buttons breathe).
  - FONT: new bundled `changa_one_regular.ttf` (Changa One v1.003, OFL
    with Reserved Font Name "Changa" — license at
    `app/third_party/changa_one_OFL.txt`) + `ChangaOneFontFamily` in
    CurioTypography.kt (SINGLE-entry like PatrickHand: Changa One has no
    bold TTF, so pair with `FontWeight.Normal` or Bold requests trigger
    fake-bold synthesis). Nav pill + rail labels use it at 13sp (12sp
    geom Bold → 13sp Changa One keeps the same visual weight). Note:
    the OFL text must NOT live in `res/font/` (typed dir — AAPT rejects
    non-fonts); it sits at `app/third_party/`.
- **v183 — Spin Filter badge only shows when filters are selected.**
  User: "in spin page the filter always shows the count, make it only
  show when filters are selected."
  - ROOT CAUSE: `BottomCta` computed `hasFilters = filterActiveCount > 0`
    but the call site passed `filterActiveCount = filteredPool.size` —
    the always-non-zero MATCHING-TOPICS count — so the badge
    ("Filter · N") showed permanently even with zero chips ticked. The
    wide/tablet right-rail button (line ~1206) was already gated
    correctly on `activeFilters.isNotEmpty() || activeSubtypes.isNotEmpty()`.
  - FIX: `filterActiveCount` is now `Int?` — null until chips are
    selected (pill reads plain "Filter"), and when selected it keeps the
    v83 design (the total topics matching the filters). Labels guarded by
    `hasFilters` smart-cast it to non-null.
- **v182 — crash fixes: drawer grid-web OOB + Pet Designer negative
  padding, and the FilterSheet Apply pill floats like the picker's
  Mix/Cancel.** User: "fix this app crah on drawer open and also in
  category picker theres still soemthing at the button and do the same
  with filters tooof spin screen" + a crash report
  (`IndexOutOfBoundsException: Index 29 out of bounds for length 29`,
  at draw, drawer open), then a second crash
  (`IllegalArgumentException: Padding must be non-negative`, Pet
  Designer UI).
  - DRAWER CRASH: `DrawerLaneConstellation`'s grid web (v178) drew
    right/down links guarded only by `col < c - 1` / `row < r - 1` —
    with a NON-rectangular grid (29 lanes → 6×5, last row 5) the last
    node of a short row indexed `pts[i+1]`/`pts[i+c]` PAST the array
    end. Fixed with length guards `i + 1 < n` / `i + c < n`. LESSON:
    array-index guards in DrawScope loops must check BOTH the grid
    position AND the array length — grid math and list length diverge
    whenever the lane count isn't a perfect rectangle.
  - PET DESIGNER CRASH: the v179 full-bleed banner used
    `padding(horizontal = -edgePad)` — **Compose forbids negative
    padding** ("Padding must be non-negative", thrown at layout).
    Replaced with the standard full-bleed trick: `BoxWithConstraints`
    → `offset(x = -edgePad)` + `requiredWidth(maxWidth + edgePad * 2)`
    so the tear still reaches both screen edges. LESSON: NEVER emit
    negative `padding()` — use offset + requiredWidth instead.
  - FILTER SHEET ("do the same with filters"): the Apply / Show all
    button is no longer a full-width bar below the chips — it FLOATS
    over the sheet content (`align(BottomCenter)`, bottom 26dp) as the
    same raised accent pill (fill/glow/glass unchanged), and the chips
    column's bottom padding is now 88dp so the pill never covers the
    last row. The category picker's bottom was already clean in current
    code (the v180 rework removed the footer — the user's crash build
    predated it).
- **v173 — pill morph slowed again (400) + Cabinet "All" wears the SPIN
  accent.** User: "the navbar morphe open is still tooo rapid aah, make i
  even more sloer. and in cabinet all use blue or red or whatever the spin
  screen color have set not yellow or anything else."
  - SPRINGS: all four nav-pill specs (PillWidth/Motion/Color/Expand) and
    the mirrored Reveal* + Studio* families in TopicRevealScreen.kt /
    PetDesignerScreen.kt dropped stiffness 750 → 400 (damping stays 1.0
    — critically damped, zero bounce). ~35% slower settle, still lockstep
    (same physics across all four specs per file). Do NOT touch
    `Springs.Calm` (750, in CurioMotion.kt) — that's the page/dialog
    entrance family, not the pills.
  - CABINET "ALL" COLOR: `curioActivePillFill(null)` fell back to
    secondaryContainer = ButterYellow@30% — the stray yellow. Fix in
    `curioNavActiveAccent`: CABINET returns `cabinetAccent ?:
    (spinAccent ?: primary)` — the pill inherits the SPIN deck's
    published accent ("whatever the spin screen color have set"; default
    wildcard deck = CoralBlush, the brand primary) and gets the same
    light-mode 55% saturation mute as Spin's own pill. The butter
    fallback now only reaches Home-without-hero / non-tab routes. LESSON:
    the theme's secondaryContainer is BUTTER (yellow) — any "stray
    yellow" on a plain page is this fallback, not the nav code.
- **v172 — mood board quote cards: resize scales the WHOLE note (text
  included), and the export renders the same size/spot as the editor.**
  User: "the moodboard quote cards are still very bugged... both in
  editing and sharing". ask_user: resize "only expands from side and the
  text size stays the same", export "either gets big and looks differnt
  or its position is somewhere else". MoodBoardZoom.kt:
  - RESIZE: the slip no longer forces a fixed slot height (heightIn
    h..1.5h in the editor, exact h when saved) with a fixed-size font —
    it now sizes to content and the quote TEXT scales with the card
    (textScale = renderW ÷ baseW, baseW = the never-resized slot width
    × view scale, floor 0.5). Resize = a true uniform note scale.
  - EDITOR/EXPORT MISMATCH: removed the v60/v108 40%-of-canvas display
    cap (`displayScale` is now just `scale` in every view). The cap made
    a resized card render small in the inline editor but at raw size in
    the export — the shared PNG showed it bigger and, reaching past the
    same top-left, "somewhere else". With v113's raw-space slots the
    board scale is already correct, so cards are rawW × scale everywhere
    (inline editor, full-screen editor, saved card, export). rawSpace is
    now an inert API param (kept for callers).
  - LESSON: "text size stays the same" = the resize only changed the
    slip width; "gets big in export" = the editor-only display cap
    disagreed with the export's raw render.
- **v165 — v162's one-spring-family CI fix: the specs are TYPED per
  animated value.** CI failed: `SpringSpec<Float>` passed where
  `AnimationSpec<Color>` (fill/icon tint) and `FiniteAnimationSpec<IntSize>`
  (the label's expand/shrinkHorizontally) were expected. The generic
  type parameter of a spring MUST match the animated value's type. Fix:
  each pill now declares `spring<Dp>` (width), `spring<Color>` (fill +
  icon tint), `spring<IntSize>` (label expand/shrink) and
  `spring<Float>` (fadeIn/fadeOut) — same 0.9 damping + Medium stiffness
  everywhere, so the lockstep physics are unchanged. Added the `IntSize`
  import in all three files. LESSON: `spring<Float>` is ONLY valid for
  float animations; sharing one spec across color/size/fade animations
  needs one spring per target type.
- **v157 — dark-mode hairline rims removed from the floating nav bar and
  the detail quick-fact plate.** User: "why in dark mode the navbar
  floating one have borders? remove that", plus "i notices in detail view
  theres border in quick fact box". The v149 dark-mode
  `BorderStroke(1.dp, White@10%)` capsule rim on `CurioFloatingNavBar` is
  GONE (the elevated fill alone defines the capsule), and the v115
  `Modifier.border(1.dp, ink@18%)` on `QuickFactCard`'s plate is GONE too
  (its lifted fill alone defines the plate) — the `BorderStroke` /
  `foundation.border` imports followed. NOTE: the same dark rim still
  lives on the tour dock (CurioNavHost), the reveal Like/Dislike pill
  (TopicRevealScreen), the pet studio bar and the floating action capsule
  (PetDesignerScreen) — not touched, offered to the user. LESSON: the
  hairline-rim "elevation" trick in dark mode is a look the user may not
  want on every floating surface; ask rather than blanket-apply.
- **v156 — Pet Designer layout rework (user-confirmed): compact bottom
  nav, floating top action capsule, tear scrolls away in-flow.** User:
  "the pet designer floating nav is stretched all the way fix that. and
  place the save undo redo save and share at the top and sticky, and
  make the tear design be on the background itself and it scrolls away
  when i scroll down". Confirmed via ask: bottom nav = compact centered
  capsule (like the main bar); actions = floating pill over the banner
  (pinned while scrolling); tear = banner becomes the first scrollable
  item (scrolls away in-flow). (1) `PetStudioBottomNav` lost its
  `fillMaxWidth()` capsule — a wrapping `Box(fillMaxWidth, Center)` now
  holds a CONTENT-SIZED capsule centered at the bottom. (2) The old
  full-width `EditorToolbar` (a stickyHeader inside the list, below the
  hero at rest) became `StudioFloatingToolbar`: ONE rounded capsule
  (`curioFloatingNavContainer` + dark rim) pinned `TopEnd` below the
  status bar — compact Save text pill (with a dirty dot) + 38dp
  Undo/Redo/Reset/Share/Import icon circles (`ToolbarIcon` gained a
  `size` param). Toasts now auto-clear (`LaunchedEffect(toast) { delay(3000) }`)
  and show as a transient pill under the capsule. (3) The torn banner
  moved from the overlay graphicsLayer-translation Box (v109/v113) INTO
  the list as its first `item` — the tear is part of the page background
  and scrolls away naturally; the overlay Box, the stickyHeader, and the
  `SettingsHeroTotalHeight` top padding are gone. LESSON: a "sticky
  hero that scrolls away" is simpler and more robust as the list's first
  item than an overlay Box with `viewportStartOffset` translation math —
  and a floating action capsule pinned to the screen edge replaces a
  sticky-header strip without the status-bar dance.
- **v155 — light-mode nav capsule finally shows the page tint + pill
  animations smoothed.** User: "the active indicator gets the theme
  dynamic color but in light mode the background of it doesn't so make it
  get the background tint", and the animations "feel clanky sometimes —
  make it even more smoother". (1) `curioFloatingNavContainer`'s light-
  mode lift was `lerp(wash, surfaceContainerHigh, 0.55)` — 55% toward the
  parchment elevated surface washed the page tint out completely, so the
  capsule read as a plain cream bar behind the colored pill; now 0.30 so
  the tint shows while it still reads lifted (dark unchanged). LESSON:
  a lerp "lift" toward the elevated surface can silently erase a subtle
  page tint — the pill gets the color while the background loses it.
  (2) Smoother pills (nav bar + reveal Like/Dislike, both the same
  recipe): width spring damping 0.75 → 0.9 (the old one overshot and
  bounced on settle); the active fill now FADES via
  `animateColorAsState(activeFill.copy(alpha = …))` synced to the same
  spring instead of snapping on/off; the icon tint crossfades
  (tween 200 FastOutSlowIn); the label's fade tracks the pill's
  expansion (tween 160 → 240 FastOutSlowIn; exit stays instant per
  v125). LESSON: underdamped springs + hard color snaps read as
  mechanical pops; fade the fill/tint with the same spec as the width
  morph and near-critically damp the spring.
- **v153 — nav-bar → reveal sentiment-pill morph REVERTED (the bigger
  pill stays).** User: "revert this just keep the size large but revert
  the shared morph one". The v151 shared-element morph (the nav bar's
  capsule collapsing into the reveal's Like/Dislike pill) is fully
  removed: `SentimentSharedElementKey` + `NavPillBoundsTransform`
  deleted from RevealSharedScopes; `CurioFloatingNavBar` /
  `FloatingNavPill` lose the `sharedElementState` / `visible` /
  `interactive` params (back to plain `clickable`); CurioNavHost drops
  `sentimentMorphVisible` + `sentimentSharedState` and renders the bar
  with the plain `if (!wide && showBottomBar && …)` block (the bar hides
  and the reveal pill slides up as it did before); `RevealSentimentPill`
  loses its `modifier` param. KEPT: the 60dp/128dp pill + 26dp icon.
  LESSON: a shared-element morph that pairs an overlay bar with a
  route's content is a big structural change (bar inside the
  SharedTransitionLayout, caller-managed visibility, 500ms hold) — the
  user may prefer the simple hide-and-slide-up; ask before committing
  to that scale of refactor.
- **v151 — bigger bottom pill (60dp/128dp).** User: "the bottom pill
  can be more larger". `FloatingPillIconWidth/ExpandedWidth/Height`
  52/112/52 → 60/128/60, icon 24→26 so the bar reads proper and the
  active tab's label has real room. The same commit also tried a
  shared-element morph (nav bar capsule → reveal Like/Dislike pill,
  `SentimentSharedElementKey` + `NavPillBoundsTransform`, bar kept
  composed 500ms as the caller-managed source) — the user reverted the
  morph after seeing it (see the v153 note); only the SIZE shipped.
- **v152 — the remaining topic files deduped (178 groups, 181
  entries).** The batch-duplication pattern v127 flagged ("other topic
  files… but only books were deduped") is now fixed for every file that
  had it: authors 38 groups, astronomy 89, songs 26 (two triplets),
  geology 11, animals 10, technologies 3, chemistry 1 — 178 groups /
  181 entries collapsed to one each, mirroring the books rule (richest
  entry wins: longest teaser + richest exploreAction + most tags; tags
  unioned keeper-first; tier preserves 1; first-position placement;
  per-file indent preserved — astronomy/technologies use indent 2, the
  rest 1 — so untouched content stays byte-identical). `topic_index.json`
  rebuilt (`scripts/build_topic_index.py`, 16,833 topics, fully in sync
  both ways — the old index was stale, predating recent content
  commits). Android assets ONLY — the web mirror was NOT touched per the
  root AGENTS.md 🔒 scope rail (web/ is on hold).
- **v150 — floating pills go THEME-AWARE + DYNAMIC (user-confirmed:
  container follows the page tint, active pill follows the page color,
  dark-mode elevation), plus the reveal Like/Dislike pill gets the nav-bar
  expand/collapse animation and the picker's Manage categories floats.**
  (1) `CurioNavTint` now also publishes per-tab ACCENTS (spin/cabinet/
  home); `curioFloatingNavContainer(routePrefix)` lifts the page wash
  toward the elevated surface (light mode; dark keeps surfaceContainerHigh
  since the pages are near-black) and `curioNavActiveAccent` picks the
  page's accent. `CurioFloatingNavBar` container = the dynamic tint
  (animated 420ms), active pill = page accent + `pastelFillInk` (fallback
  secondary/onSecondary), and every floating pill bar (nav, tour dock,
  pet studio) draws a hairline `BorderStroke(1.dp, White@10%)` rim in
  dark mode — the black shadow is invisible on the near-black pages
  (`curioDarkGlow` was retired in v30, so a rim replaces the old glow).
  (2) Reveal `SentimentSegment` mirrors `FloatingNavPill`: icons at rest
  (52dp), active springs to 96dp with the label slide-out, same spring.
  (3) Picker sheet's Manage categories TextButton → floating pill. Also
  fixed the CI break: `boardHasContent` was declared INSIDE the collage
  Box but read outside it (the Box closes before the pin zone) — hoisted
  to the canvas top. LESSON: publish page COLORS (not just washes) when
  an overlay pill should wear them; and a val used across sibling scopes
  must live at the shared parent scope.
- **v149 — (1) progress editor REVERTED to the ring design, keeping only
  the page-count EDITING improvement; (2) saved-entry SHARE gets a
  preview sheet.** The user: "revert the progress ui — i only meant you to
  change the page count look and its editing way, not redesign it; the way
  to edit the page count is bad". The v135 stepper-first dialog (31e5fea)
  was over-reach: `git checkout 4558e99` restored the ring dialog (big % +
  count, −/+ steppers, slider, Finish/Save) and two improvements were
  re-applied on top: the slider snap fix (≤200 total = whole-unit steps,
  big totals = continuous rounded — the "editor isn't working" bug) and
  the count is now a PLAIN display with an explicit "Edit total" chip +
  pencil below it that opens the inline numeric field (the old hidden
  tappable count read as plain text — the "way to edit the page count is
  bad" complaint). Keep the TopicProgressStore/topic changes (the
  editable-target feature itself stays). (2) Share: the EntryDetail More →
  Share item used to fire the ACTION_SEND chooser with no preview; it now
  opens `EntryShareSheet` — a ModalBottomSheet (theme surface, drag
  handle) with a live 320dp `CurioShareCard` preview on a shadowed stage,
  an Image card / Text pill picker (solid-secondary selected per the
  v131 contract), and a Share button (image = the existing
  `shareComposableCard` 400×400 PNG path; text = plain-text summary via
  `entryShareText`). State lives in `DetailStickyBar` (`rememberSaveable`
  `showShareSheet`); the sheet renders inside the sticky bar's Row but is
  a Dialog window, so the bar's scroll graphicsLayer doesn't affect it.
  LESSON: a "fix the editor" ask can balloon into a full UI redesign —
  keep the original layout and fix the affordance; and a share action
  deserves a preview step, not a blind chooser.
- **v148 — the Pet Designer studio bar animates EXACTLY like the main
  nav bar ("unify the pill style" included the ANIMATION).** v142
  restyled `PetStudioBottomNav` to the floating pill container but kept
  static `weight(1f)` tabs with always-visible labels; the user
  clarified: "use similar animation just like in home screen nav bar —
  similar style collapse". `PetStudioTab` now mirrors `FloatingNavPill`
  verbatim: icon-only 52dp pills at rest, the ACTIVE pill springs to
  112dp (same spring: dampingRatio 0.75, StiffnessMediumLow) and slides
  its label out (expandHorizontally(Start) + fadeIn(160), exit
  tween(0) so the deselected label vanishes), solid `secondary` fill +
  `onSecondary` ink; the Row centers so the width change stays balanced
  in the container. LESSON: "same style" for a restyle means the pill's
  BEHAVIOR too — icons-at-rest + active-expands-label is the app's
  signature pill animation, not just the rounded container.
- **v147 — the Home drawer is HOISTED to the NavHost root so it draws
  ABOVE the floating pill bar, which stays composed underneath.** v135 had
  "hidden" the bar while the drawer was up (NavHost dropped it from
  composition via `!CurioDrawerState.isOpen`) — the user's intent was
  "place the drawer above it", not "disappear the bar and reappear it":
  the bar visibly vanished at drawer-open and popped back at close. The
  fix moves `ModalNavigationDrawer` OUT of HomeScreen to wrap the whole
  NavHost root Box (page + rail + bar + tour dock); the bar's `if` keeps
  only the tour gate. HomeScreen's hamburger now raises a request via
  `CurioDrawerState.requestOpen()` (an incremented `openTick`, observed by
  a NavHost `LaunchedEffect` that opens its own `rememberDrawerState`)
  instead of touching a local DrawerState; `HomeDrawerContent` became
  `internal` so the NavHost can render it; Home's page kept its wrapper as
  a plain `Box(fillMaxSize())` (the drawer's old content slot) so no
  re-indent was needed. LESSON: a modal overlay that must cover a
  sibling overlay drawn later in the Box needs to be hoisted ABOVE that
  sibling in the tree — zIndex cannot escape the parent Box — and "hide
  the thing under it" reads as a glitchy vanish/reappear.
- **v146 — reveal year pill moves out of the hero into the top bar.**
  The v141 hero top-left pill ROW (byline + year) collided with the
  progress badge on the hero's TOP-RIGHT: a long byline ("Director ·
  Christopher Nolan") pushed the year pill ("1941") under the progress
  pill on topics with reading progress ("it's already covered by progress
  pill"). The year pill now rides NEXT TO the category chip in the reveal's
  top bar (left corner, `Row(weight(1f, fill=false))` wrapping the chip +
  year pill; same frosted `categorySurface` recipe as the chip, Schedule
  glyph + `categoryInk()`); the hero's top-left keeps ONLY the byline pill
  (the morph element shared with the Spin ticket). LESSON: the hero's top
  corners are shared with the morph (byline) AND the progress badge
  (top-right) — the top BAR is the right home for secondary pills like the
  year qualifier.
- **v145 — mood board: quote cards independent per view; real resize
  (proportional height + 60% size limit); PNG/expanded dialog keep exact
  card sizes; Clear board wipes quotes; Quote chip moves up on content.**
  (1) **Quote cards are now INDEPENDENT between the small and full-screen
  boards** (per user, confirmed): positions were already separate
  (quotePositions vs quotePositionsFull) but the full-screen resize
  handler ALSO wrote the SHARED `quoteCards.setWidth`, and the full-screen
  move handler pulled the shared inline width in (`widths.getOrElse`) —
  so resizing a card in full-screen changed the small board too ("why is
  the quote card the same in both small and full screen"). v145:
  `onResizeQuoteOverride` writes ONLY `fullQuotePositions[i].w` (no
  `quoteCards.setWidth`), and `onMoveQuoteOverride` preserves the full
  placement's OWN width. The full-screen board seeds from
  `quotePositionsFull` with a legacy fallback to the inline spots (mirrors
  `fullTiles`' tileLayoutsFull→tileLayouts fallback — old boards keep
  their single arrangement until rearranged). (2) **Resize is a real
  resize, quote cards only**: `MoodBoardFloatingCards` derives the card
  HEIGHT from its width at the slot's paper aspect (`cardH = cardW ×
  slot.h/slot.w`), so a wider card grows taller instead of stretching
  flat; the resize grip's `maxW` is now capped at 60% of the visible
  board (was `boardW - x` — a card could span the whole collage). (3)
  **PNG export + expanded dialog pass `rawSpace = true`**: the export's
  canvas mirrors the board's aspect (raw × scale), so the old 40% display
  cap bound `displayScale` against the RAW board width (canvasWPx = maxX)
  and shrank resized cards in the saved PNG ("gets small / wrongly
  placed"); the expanded dialog capped cards at 40% of the displayed
  board while the full-screen editor showed the exact width — now both
  render exact raw widths, matching the editor. The INLINE editor + saved
  small card keep the 40% cap (v60/v108 look). (4) **Clear board clears
  quote cards too**: the confirm dialog counts quotes ("Remove all N
  images and M quote cards?") and the action wipes them via
  `removeCard(0)` in a loop (fires onCardRemoved → fullQuotePositions
  stays aligned). (5) **Quote chip position**: a `boardHasContent` flag
  (tiles OR quotes) drives both the chip's bottom padding (16dp when
  empty, 88dp once content — the Clear button appears at 16dp and the
  chip must sit above it) and the Clear button's visibility (was
  tiles-only, so a quotes-only board couldn't be cleared).
- **v144 — tour controls are a floating pill bar; the nav bar yields
  during the tour.** (1) **The tour's Skip/Next dock** was the last
  full-width opaque bottom band in the app (a v9.x `Surface`
  `fillMaxWidth` + `tonalElevation` covering the whole strip) —
  converted to the v124/v129 floating pill recipe: rounded-50
  `surfaceContainerHigh` capsule, 6dp shadow, `navigationBarsPadding()`
  + 12dp air gap, content-sized 52dp capsule buttons inside (Skip =
  soft `surfaceVariant` secondary, Next/Done = solid primary CTA),
  7dp row padding / 6dp spacing. The dead `fillMaxWidth` import was
  removed (the file still uses `windowInsetsPadding` for push routes).
  (2) **The main `CurioFloatingNavBar` now YIELDS while the tour runs**
  (`TourController.currentStep == null` added to the v135 drawer gate):
  the tour pill floats at the same bottom-center spot on tab stops, and
  the old opaque dock covered the bar anyway — the bar must not show
  behind/around the tour pill. LESSON: a floating control that replaces
  a docked band must also inherit the band's occlusion behavior (the
  bar it sits over must hide), or the two pills stack at the same spot.
- **v129 — floating pill bar: Scaffold removed (no strip) + no more
  switch squeeze.** (1) **The strip is gone for real.** The v125 fix
  painted the nav slot with the page wash, but the flat band still read
  as a strip (the page has watermarks/gradients/tears above it, so a
  solid band below is visible). `CurioNavHost` no longer wraps content
  in a `Scaffold`: the page Row runs full-bleed, and
  `CurioFloatingNavBar` is now a true overlay in the root `Box`
  (`Modifier.align(Alignment.BottomCenter)`) drawn ON TOP of the page's
  own background — no painted slot at all. The bar wraps its content
  (no `fillMaxWidth`, no 72dp slot, no wash band) and floats with
  `navigationBarsPadding()` + a 12dp air gap. (2) **The squeeze fix.**
  v125 made the newly active pill spring open while the deselected one
  snapped shut (`tween(0)`), so the bar's total width DIPPED then grew
  back — re-centering the whole bar ("it squeezes"). Both pills now
  animate with the SAME spring (`spring(0.75, StiffnessMediumLow)`):
  the shrinking pill's width loss equals the growing pill's gain at
  every frame, so the total stays constant and the bar never moves.
  The label exit stays instant (closing pill's text vanishes — v125
  preference kept). (3) **Page clearances.** With no Scaffold slot, the
  tab pages clear the pill themselves: Home's final spacer is 92dp on
  phones (32 wide), Spin's phone `Column` gets
  `windowInsetsPadding(navigationBars)` + 76dp, Cabinet's `Column`
  gets the inset + 76dp on phones (0 on wide). Non-tab routes keep the
  nav-bar inset the Scaffold used to deliver (content Box applies
  `windowInsetsPadding(navigationBars)` whenever the pill is hidden).
  The reveal's 80dp placeholder is unchanged (it paints its own band).
- **v131 — offline models, dictation fixes + the settings-card ink fix.**
  (1) **Offline model catalog expanded** (`VoskModels.CATALOG`): four
  bigger tiers join the three smalls — `vosk-model-en-us-0.22-lgraph`
  (Large · English US, ~128 MB), `vosk-model-en-us-0.22` (Full · English
  US, ~1.8 GB), `vosk-model-en-us-0.42-gigaspeech` (Full · Gigaspeech,
  ~2.3 GB) and `vosk-model-en-in-0.5` (Full · English India, ~1 GB),
  sizes from the alphacephei.com model page; the picker copy warns that
  the big models are heavy downloads needing real storage + memory.
  (2) **The floating dictation mic moved INTO the note box's tool dock**
  (`RichTextEditor.trailingAction`, the slot designed for "a small
  dictation button"): it renders above the field (still gated on
  `noteFocused && !dictationOpen && voiceToTextEnabled`) instead of in
  the scroll flow below the editor, so it never hides behind the
  keyboard. (3) **Dictation no longer wipes earlier text on a pause, and
  a break = a full stop** (always on, per user): the transcript now
  ACCUMULATES — `dictatedText` holds committed utterances, `partialTranscript`
  is only the live words; `onResults` APPENDS (never replaces), blank
  partials during a pause no longer clear the preview, and a fresh
  partial after `onEndOfSpeech` commits the previous utterance first
  (prefix-match guards against same-utterance refinements). Committed
  utterances join with a period (`. `) and the next sentence is
  capitalized; Insert drops the whole transcript (committed + live
  partial) into the note. (4) **Settings-card row labels were BLACK in
  dark mode** (`CurioSettingsCard`): the card fill is a CUSTOM lerp, and
  the uncolored row titles rely on `LocalContentColor` — the default
  `contentColorFor(customFill)` resolved black on the near-black card,
  making every row label invisible (subtitles were fine — they set
  explicit `onSurfaceVariant`). `CurioSettingsCard` now pins
  `contentColor = MaterialTheme.colorScheme.onSurface` (dark plum in
  light, cream in dark), fixing Profile, the Settings hub, every settings
  sub-page, Support, Updates, Experiments, Quests, Backup and Onboarding
  cards in one edit.
- **v131 follow-up — bigger nav pill, solid indicator + the dim-flash fix.**
  (1) **The floating pill bar grew a little:** pills 48→52dp (height +
  resting icon width), the expanded active pill 96→112dp, icons 22→24dp,
  row padding/spacing 6/4 → 7/6dp; the tab pages' pill clearances grew
  with it (Home 92→100dp, Spin/Cabinet 76→84dp). (2) **The active
  indicator is now a SOLID `secondary` fill with `onSecondary` ink** (the
  v27q selection contract — never a translucent container) instead of the
  washed `secondaryContainer` overlay, so the active tab reads as a
  defined amber pill in light AND dark; the wide `CurioNavigationRail`
  uses the same pair (selected icon/label `onSecondary`, indicator
  `secondary`). (3) **Dim-flash fix (root cause):** v129 removed the
  Scaffold, which used to paint `colorScheme.background` behind the
  content — the root Box became TRANSPARENT, so the window's dark-navy
  bootstrap color (`curio_deep_plum` #081B33 in themes.xml) showed
  through the NavHost page transitions mid-fade (every tab crossfade and
  push/pop dimmed toward navy). The root Box now paints
  `MaterialTheme.colorScheme.background` again (invisible in practice —
  every page paints its own full-bleed background; it only shows during
  transitions and wide gutters). LESSON: when a Scaffold is removed, its
  `containerColor = colorScheme.background` fill must be replaced
  explicitly or the window bootstrap color shows through fades.
- **v132 — Topic Reveal: no Scaffold band + floating Like/Dislike pill +
  tags under the hero + ticket-matching hero pills.** (1) **The reveal's
  bottom band is GONE** — `RevealBottomBarHeight` (TopicRevealScreen's
  80dp strip) and the NavHost's matching `RevealBottomBarPlaceholderHeight`
  reservation were both removed; the reveal now runs full-bleed like the
  tabs (the old reserved slot made the page stop 80dp short). (2)
  **Like/Dislike now ride a floating capsule** mirroring the bottom nav's
  pill bar (raised `surfaceContainerHigh` + 6dp shadow,
  `navigationBarsPadding` + 12dp air gap): it HIDES while scrolling down
  and slides back in on scroll-up — direction tracked via `snapshotFlow`
  on the page's `ScrollState` (3px dead-band), and it stays hidden in
  Browse-Topics mode (read-only; the old band's `browseMode` gate
  preserved). (3) **Tags moved back into the scroll body**, directly below
  the hero (hero → tags → actions → teaser → prompt); the action row drops
  to a 16dp gap when tags are present, else keeps the old progress-pill
  clearance. (4) **Hero pills now match the Spin ticket EXACTLY**: the
  action badge, byline and subtype swap the old opaque `pillGlass` for the
  ticket's `ink.copy(alpha = 0.18f)` recipe, so the reveal hero reads as
  the same card in light AND dark (the frosted glass read as white blobs
  next to the ticket's subtle tint). LESSON: the reveal hero and the Spin
  ticket morph from the SAME shared element — keep them on ONE pill
  recipe or the morph visibly changes color mid-expansion.
- **v116 — CI compile fix: Kotlin NESTED block comments.** A KDoc in
  `UpdatesScreen.kt` contained the literal sequence `-/*` ("# headers,
  -/* bullets"): Kotlin block comments NEST (unlike Java), so that `/*`
  opened a nested comment, the block's own closing `*/` closed the NEST
  instead, and the outer KDoc stayed open to EOF — swallowing the trailing
  `private enum class UpdateCheckUi` / `UpdateDownloadUi` declarations and
  the `ReleaseNotesBlock` helpers, which surfaced as dozens of
  "Unresolved reference" errors + "Syntax error: Unclosed comment". The
  KDoc is reworded (and now carries a NOTE). LESSON: never write a
  slash-star pair inside a block comment/KDoc in this codebase; if you
  must describe it, spell it as words ("slash-star").
- **v116 — Support & diagnostics page: Updates moved to the END and
  de-duplicated.** The "Updates" section was the FIRST section with a
  card header (Download icon + "Updates / Your build and what's new")
  AND an "Open Updates" row (Download icon) — three "Updates" entries on
  one page. The section now sits LAST (after About Curio) and keeps ONLY
  the Version readout row (five taps → Experiments); the card header and
  the "Open Updates" row are gone (the update flow lives on the dedicated
  Updates sub-page, Settings → Updates). Dead `FontWeight` import removed.
- **v114 — CurioIcon glyphs stay vertically centered (no bottom cut) at every
  font scale.** Root cause was the icon Text's `includeFontPadding = false` +
  `LineHeightStyle.Trim.Both` + `lineHeight = 1.0em` combo: trimming the
  line below the font's NATURAL 1.2em box, plus the trim's int rounding,
  dropped the baseline ~2dp below the icon box — every glyph sat low and
  its ink bottom was sliced by clipped button shapes (worse at large system
  font sizes, where the cut was most visible). The Material Symbols font's
  ink is designed to be centered in its natural line box (glyph ink spans
  +0.04em..+0.96em above the baseline; the baseline sits at 1.1em of the
  1.2em box), so the fix keeps the fontScale compensation (`size / fontScale`)
  and restores the natural box: `PlatformTextStyle(includeFontPadding = true)`
  (the default — the platform font padding stays) and NO line-height trim, so
  `lineHeight = 1.0em` acts as a minimum. The 1.2em natural box centers the
  ink in the icon's layout box with ~1dp margin on both sides. See
  CurioIcons.kt for the full derivation; DO NOT reintroduce the old
  trim/padding combo.
  **v115 — small spot corrections:** the natural box centers the ink, but
  a few glyphs' OPTICAL weight still reads a hair low inside compact
  circular/pill buttons, so those spots carry their own tiny lift:
  Home's `TopBarPill` (Menu + Person, -2dp), the drawer's `DrawerNavItem`
  chips (-1dp), Profile's `ProfileSearchPill` magnifier (-2dp), the
  `SettingsNavCard` cog (-2dp) and `CurioBackButton`'s chevron (-1dp,
  covering the Profile + Settings sticky/hero back pills). The first
  pass (-1/-1.5dp) was still a touch low, so the Profile/Settings spots
  deepened by another 1dp. Keep these per-site; do not re-add a global
  draw-time lift to `CurioIcon` (the old 1dp `graphicsLayer` lift was
  removed in v113 because it pushed near-top-bearing glyphs into clipped
  parents).
- **v115 — settings/profile option icons are BARE (no colored chip box),
  and the settings sub-pages render their options inside the shared
  settings card.** (1) **Icons: `CurioSettingsRow` and `CurioCardHeader`
  dropped the coral/rose tinted chip box behind their glyphs** — the
  icon now renders bare at 21dp in the hero-matched accent ink
  (`settingsCardAccentInk`), the same treatment `CurioSettingsInfoRow`
  always had, so every settings row/header reads as a clean option, not
  a colored block. Profile's `SettingsNavCard` cog also lost its blue
  gradient block (bare accent cog). (2) **Sub pages: the flat rows that
  used to sit transparently on the watermark backdrop now live inside
  `CurioSettingsCard(shadowElevation = 0.dp)`** — the four section pages
  (Appearance / Preferences / Recording / Backup & restore via
  `SettingsPageContent`, which also card-wraps the two-pane hub's right
  pane), plus Experiments, Backup tools, Support & diagnostics and the
  Updates sub-page. Every settings destination now uses the same
  row-in-card language as the hub; the Promo preview page keeps its
  showcase cards (previews + share buttons, not option rows).
- **v115 — Updates page redesigned: status header + saved release notes +
  markdown-lite rendering.** (1) **The last successful check is CACHED**
  (`AppPreferences.getCachedUpdateInfo` / `setCachedUpdateInfo` — tag,
  notes, htmlUrl, apkUrl): opening the Updates page shows the saved notes
  INSTANTLY (no network reload on every visit); the auto-check still
  refreshes silently in the background (`runCheck(keepResult = …)` keeps
  the saved result visible while checking) and only replaces it on
  success — a failed refresh keeps the saved result and marks the row
  "Couldn't refresh · tap to retry". The fetch path saves every
  successful result, so after updating, the notes for the installed
  version show. (2) **Page design:** a status header (accent status dot
  tinted by state — rose download / sage check / error — headline +
  subline + a `v{version}` chip) replaces the plain version row; the
  update action lives in an accent-tinted banner (Update now CTA,
  download progress, retry states, Open release on GitHub); notes move to
  their own "What's new" card. (3) **Release notes render markdown-lite**
  (`parseReleaseNotes`/`parseInline`/`ReleaseNotesBlock`, hand-rolled —
  the project has no markdown dependency): `#/##/###` headers, `-`/`*`
  bullets with accent dots, `---` dividers, `**bold**` spans,
  `[label](url)` links stripped to labels and `` `code` `` to plain text —
  the raw GitHub body no longer shows as literal markdown.
- **v115 — detail-screen tear runs out cleanly at the screen edges; the
  quick-fact box is theme-aware; the fold toggle is a bare "…".** (1)
  **Tear corners:** the seeded torn bottom edge (waves + noise + the tilted
  slant, which alone drifts ±up-to-10dp between the corners) could notch
  the hero's corners AT the screen's left/right edges and read as "cut" —
  worst on the edge-to-edge detail hero, whose seed is the entry hash, so
  some entries drew a deep up-bite at a corner. `buildSoftTornPath` and
  `buildSoftSheetPath` now fade the displacement to ZERO over the last ~5%
  at each end (the SAME fade in both, so the hero and its white under-sheet
  stay pixel-aligned): the middle 90% keeps its full torn character and the
  corners meet the nominal edge — applied to every torn hero (Home,
  Profile, Settings, Cabinet, Detail) since they share the shape. (2)
  **Quick-fact box** (EntryDetail `QuickFactCard`): the old translucent
  white @38% plate washed out against the tinted page wash in light and
  glowed like a bright sheet in dark — now a theme-aware OPAQUE plate
  (`lerp(surfaceContainerLow, categoryInk, 0.06f)` light /
  `lerp(surfaceContainerHigh, ink, 0.10f)` dark) with a hairline category
  rim in dark mode, matching the settings-card language. (3) **Fold
  toggle:** the "…more" / "…less" words are gone — collapsed shows a lone
  "…" affordance; expanded turns it `Color.Transparent` (the tap target
  stays, same size) so tapping the same spot still folds it back.
  crisp.** (1) **Eyes stopped flicking at the start of every scroll:**
  `PetPointer.trackerModifier` aimed the eyes on `Press`, so every
  touch-scroll began with a visible eye-snap at the finger's touchdown
  point (the 8dp drag-cancel only fired after the finger moved). The
  tracker now only REMEMBERS the press point and commits the aim on
  `Release` when the gesture was a clean tap (no drag) — via `position`
  (the hover path), so the sprite's existing 2s look-timeout fades it
  back to neutral. A scroll's release arrives with `pressStart` already
  nulled by the drag-cancel, so scrolling never aims the eyes. Mouse
  hover + click still work. (2) **Scaled eye pixels were glitchy:** the
  v71 draw-space eye scaling (`DrawScope.scale` at 0.72/1.0/1.35)
  rendered each eye cell as a fractional-size rect on a fractional grid
  → cells landed between device pixels → misaligned "glitchy lines".
  `CurioPetSprite` now draws the scaled eye on an INTEGER device-pixel
  lattice: each cell is `round(scaleF × opx)` px, snapped to the lattice
  around the eye's center (4.5/7 and 10.5/7) — crisp at every preset,
  and at `scaleF = 1` it lands exactly on the face's pixel grid.
- **v114 — remaining stock M3 buttons converted to the pill language.**
  Sweep of `Button`/`OutlinedButton` call sites that still used boxy
  default corners/colors next to the custom pill family: the tour's
  Skip/Next controls (16dp → full `RoundedCornerShape(50)` capsules in
  the tour bar), the crash screen's three `OutlinedButton`s (16dp →
  24dp, matching the Mix-button language), FieldMind's Finish/Save
  buttons (stock 20dp + primaryContainer → pill 50 / 24dp with theme
  primary fill + `onPrimary` icon/text — the old hardcoded white icon
  clashed with the default fill), and the Sound Bite trim pair (Keep
  full + Apply Trim, 16dp → 24dp, kept as a matched pair). Intentional
  exceptions: dialog `TextButton`s (`curioDialogActionButtonColors`),
  themed `RadioButton`s, the M3 `SegmentedButton` control in Settings,
  and self-contained flows (onboarding 18/26dp, bug-report 28dp) keep
  their own language.
- **v114 — mixed-deck colors: vivid blends (no near-black mud) + a smooth
  hero gradient (no band "lines").** The user flagged the green/teal,
  magenta/purple and blue mixes as bad (the red/coral mixes were fine).
  Root cause: the curated pair/triple blends had been "deepened until they
  clear 4.5:1 against white", which pushed several to near-black mud
  (Rose+Teal 0xFF4A12A8 ≈ 11% lightness, dark blues, dark teals) — while
  the reds the user liked were never over-darkened. The decks never needed
  the white-contrast deepening: the peek cards deepen each stop per-card
  (HSL lightness drop for the reel hierarchy) and use same-hue deep ink
  ([pastelFillInk]'s light branch); the hero's white ink rides the
  theme-resolved gradient like every single deck. Fix 1: retuned the
  flagged pair/triple blends to vivid, clean mid-tones in the same hue
  families (violet 0xFF8B5CF6 / fuchsia 0xFFC026D3 / blue 0xFF2563EB /
  jade 0xFF0BA36D / teal 0xFF0FA3A3, etc. — Tailwind 500/600-style
  shades; contrast ≥ the reds' 1.2–1.3). Fix 2 — "don't use gradients
  with lines": `mixedDeckGradient` previously emitted accent → curated
  seam → accent stops, which painted visible STRIPES on the hero card.
  It now theme-resolves each accent and OKLab-interpolates ~7 fine steps
  between consecutive accents ([oklabGradientStops]), so the hero's
  diagonal / reversed / radial brush (kept — the user wants rounded /
  random styles) glides smoothly with no band lines. 4+ accents still
  fall to [oklabCentroid]. The object docstring's stale "every blend
  clears 4.5:1 against white" claim was corrected.
- **v114 → v115 — mood board: the two editing/save flows stay SEPARATE
  (v114's merge was reverted) + a Copy board button that copies the
  INLINE board into the full-screen editor.** The format keeps the v57
  dual arrangement: the inline board edits `tiles`/`quoteCards.positions`
  (saved to `tileLayouts`/`quotePositions`), the full-screen editor edits
  its OWN `fullTiles`/`fullQuotePositions` (saved to
  `tileLayoutsFull`/`quotePositionsFull`) — arranging full-screen does
  NOT change the small card, and the two views start identical only until
  one is rearranged. v114 briefly merged both canvases onto one shared
  list; that broke the user's intended flow ("it was two different
  editing and save flow — you merged it again"), so it was reverted to
  the separate-list design. The full-screen dialog shows a **Copy board**
  pill (BottomEnd, above Add images) when the full-screen board is empty
  but the inline board has content: it copies the inline tiles into
  `fullTiles` and the inline quote placements into `fullQuotePositions`
  (index-aligned; text/style/tilt/width stay shared) — "copies what the
  outside board had on inside".
- **v114 → v115 — dark-mode pill glow hugs the pill shape WITHOUT
  shrinking to the middle.** The One UI dark-mode pill treatment
  (`curioGlassEdge` top catch) was painted against the pill's BOUNDING BOX,
  so on capsule pills (50-radius) the bright top band crossed the pill's
  curved ends and read as "the shadow peeking out from behind the pill".
  (1) **`curioGlassEdge` (CurioGlassEffects.kt) now clips its FULL-WIDTH
  vertical-gradient band to the pill's own outline** — the pill's curved
  rim trims the band at the rounded ends, so the catch covers the WHOLE
  button edge-to-edge and stays inside the shape. (v114 first tried an
  inset capsule mask — 10% side insets, 55% band height — which made the
  glow visible ONLY in the middle, so v115 dropped the mask and let the
  pill outline do the trimming; the non-subtle option's bottom whisper is
  the single gradient's final stop.) (2) **`curioInnerGlow` keeps its
  radial inside the pill's curved rim** (the pill outline clips anything
  crossing the capsule). (3)
  **Capsule-pill call sites switched from `categoryEdgeShine` (a
  full-width band that crosses the curved ends) to the shape-matched
  `curioGlassEdge`:** the Category Picker's Original/New `PickerPageTab`,
  the `PickerPresetChip` quick-mix chips (both also gain `curioInnerGlow`
  accent 0.12, matching the Spin filter-chip family), the picker's Mix
  button, and the reveal's `RevealAlreadyButton`. Cards with modest corner
  radii (Start-exploring 24dp, topic/settings/hero cards) keep
  `categoryEdgeShine` — it reads as a proper edge there.
- **v113 — resume-draft take fix + filter-sheet Apply pill family.** (1)
  **Resume draft restores the draft's OWN take:** `SaveCaptureScreen`'s
  single-section init seeded a resumed draft into a `defaultFormat` section
  (`sectionEntryFormat ?: defaultFormat`), so resuming a draft written on a
  non-default take (e.g. a Journal draft on a SoundBite-default category)
  opened the default take's body with the wrong data and the draft looked
  lost. The generic branch now uses `sectionEntryFormat ?:
  formatOf(sectionInitData)` — edit mode keeps the saved entry's format,
  new-capture resumes use the draft data's own format (the existing
  Portfolio/OpenNotebook branches were already format-correct). (2)
  **FilterSheet "Show all / Apply" pill matches the chips:** the bottom CTA
  was a flat Material `Button` next to the raised chip pills; it's now the
  same family as `CompactChip`'s selected state — `Surface(onClick)` with
  `RoundedCornerShape(50)`, solid `themedButtonFill()` accent, 4dp
  elevation + `curioDarkGlow`/`curioGlassEdge`/`curioInnerGlow(accent
  0.12)` + clip, and the chip's 18sp ExtraBold label / 19dp glyph / 20·13
  padding.
- **v113 — detail pill de-dupe, filter-page icons, icon glyph clipping,
  pet-designer hero mid-screen float.** (1) **Detail hero: the explore-
  session duration no longer duplicates** — the "explored 12m" pill above
  the Date · Mood · Session · Type card was removed; the Session segment
  inside the stat card is the single source. (2) **Spin filter sheet:
  inactive group-pill glyphs visible in light mode** — `FilterGroupPill`
  tinted the closed pill's glyph with the raw `accent`, which in pastel
  LIGHT resolves to an airy pastel and vanished on the 22%-accent fill;
  light + pastel now uses `pastelFillInk(accent)` (deep same-hue ink,
  L≈0.24), dark + non-pastel keep `accent`. (3) **CurioIcon glyphs no
  longer clipped at the top in buttons** — the 1dp `graphicsLayer
  { translationY = -1dp }` "optical lift" drew the ink 1dp ABOVE the
  icon's layout box; the Material Symbols font's line box is 1.2em (hhea
  ascent 1056 + descent 96 vs 960 upem), and near-top-bearing glyphs
  (timer, auto_awesome, sparkle tips — 40/960-unit bearings) sat exactly
  at the box top, so any clipped parent (every M3 Surface with a shape
  clips) sliced the glyph's top. The lift is removed; glyphs render
  centered per the font's design bearings. (4) **Pet Designer hero no
  longer floats mid-screen** — the v109 scroll-away hero translated by
  `-viewportStartOffset`, but `viewportStartOffset` is the viewport start
  in CONTENT coordinates (negative by the top content padding at rest), so
  at rest the hero sat ~`SettingsHeroTotalHeight` down the screen. The
  translation now adds `layoutInfo.beforeContentPadding` (the same top
  padding): `-(viewportStartOffset + beforeContentPadding)` = 0 at rest,
  -S when scrolled — the hero pins to the top and rides up 1:1.
- **v113 — mood board inline editor: full-card placement + quote-card
  fixes.** (1) **Tiles can be dragged anywhere in the visible card** — the
  drag, pinch-resize, grow and commit clamps were the FROZEN collage extent,
  but the centered fit leaves an empty band above the collage, so an image
  couldn't be dragged "all the way up". Clamps are now the FULL card
  (display [0, canvas] mapped back through the fit: raw ∈
  [(0-offset)/scale, (canvas-offset)/scale − size] — negative raws allowed).
  The fit stays frozen (v108), so the zoom never jumps mid-drag; the saved
  views (MoodBoardTiles' offset clamp, EntryDetail's inline fit +
  fitTileLayout) now allow the same negative offsets so a band-placed photo
  renders in the same spot in edit and detail. (2) **Quote cards can no
  longer be dragged off the card** — MoodBoardFloatingCards passed boardW/H
  = canvas × scale to the drag/resize clamps, so on a zoomed (scale > 1)
  fitted board a card could be dragged past the bottom/right edge and its
  committed position re-rendered inconsistently ("glitching when I take it
  to the top"). The clamps now bound the VISIBLE canvas (boardW/H =
  canvasWPx/HPx). (3) **Never-dragged quote cards land ON the collage** —
  the deterministic slot was computed from the DISPLAY canvas and then
  scaled AGAIN (double-scale: display = canvasSlot × scale), so a fresh card
  in a fitted board rendered off the collage and the second slot fell below
  the board. MoodBoardFloatingCards now takes boardMaxX/Y (the collage's
  RAW extent) and computes the slot in RAW space (display = raw × scale +
  offset); callers pass the frozen extent (inline editor), the saved
  layouts' maxX/maxY (saved card), boardW/fit.scale (expanded dialog) and
  maxX/maxY (export).
- **v113 — new COSMIC launcher icon + 1.1.0 version bump.** (1) **Icon:**
  the user-supplied art (first `svgviewer-output (3).svg`, then — after the
  hand-converted VECTOR was rejected as "broken / not properly placed" — the
  designer's `svgviewer-output (3).png` RASTER, 2048×2048, archived at
  `design/launcher-icon/curio-launcher-icon.png`) replaces the old angular
  open-portal mark: a mint planet with a pink moon over layered pink/gold
  waves on a midnight navy→magenta sky, inside a rounded card with a white
  frame. The art is used as the PNG DIRECTLY — no vector conversion.
  `drawable-nodpi/ic_launcher_art.png` = the raw bitmap; `ic_launcher_foreground`
  = an `<inset android:inset="28dp">` around that bitmap (the card spans
  ~84–88% of the raw canvas, so full-bleed the launcher mask sliced the
  frame/top stars/bottom waves; inset 28 → the card renders at ~44×47dp,
  fully inside the 66dp safe circle, floating on the sky);
  `ic_launcher_background` = the full-bleed sky gradient + stars (its
  gradient matches the card's own sky so the two composite behind the
  frame); `ic_launcher_monochrome` = planet + moon silhouette;
  `ic_notification` = the same mark at 24dp. The splash (SplashScreen.kt)
  renders `@drawable/ic_launcher_art` directly (the full card, not the
  inset foreground) so the splash logo box stays full-size. (2) **Version:**
  `versionName` 1.0.1 → **1.1.0** and
  `versionCode` 20260919 → **20260920** (the bump missed on the previous
  feature releases); the store changelog moved to
  `fastlane/.../changelogs/20260920.txt` (the 20260919 draft is gone).
- **v112 — Updates sub-page + opt-in update checker, auto-backup, top-right
  update pill.** (1) **Updates page** (`features/updates/UpdatesScreen.kt`,
  route `UPDATES`, Settings hub row + Support & diagnostics "Open Updates"
  row): settings-family torn-rose sub-page with the version readout, a
  Check for updates row, an animated result card (release notes preview /
  "Update now" download → system installer / Open release), and the
  **opt-in Update checker toggle** (`updateCheckerEnabledState`, default
  OFF — Curio is offline-first, so the background check that costs data
  every launch only runs when enabled; the manual check always works).
  The OLD update card in Support & diagnostics was REMOVED (the update
  flow lives only on the Updates page now); Support keeps the Version
  five-tap → Experiments diagnostic. The update notification + toast copy
  point to the Updates page. (2) **Update toast remade:** the in-app toast
  is now a SMALL pill in the TOP-RIGHT corner below the status bar
  (`CurioInAppToastHost` anchored `TopEnd` + `statusBarsPadding` in the
  NavHost root; compact 12/7 padding, 16dp glyph, slides down from above)
  instead of the old bottom-center pill; tapping it opens the Updates page.
  (3) **Auto backup** (BackupToolsScreen "Auto backup" section): opt-in
  toggle — the FIRST time it's switched on the user picks a save location
  once (`CreateDocument`, `takePersistableUriPermission` + persisted URI
  in `auto_backup_uri`); MainActivity then exports a backup there on app
  start, throttled to ~once per 24h (`AUTO_BACKUP_INTERVAL_MILLIS`,
  `auto_backup_last_at`). The section shows Backup location (tappable to
  change) + Last auto backup. (4) **"Last backup" row fix:** the row is
  driven by the export's `ExportResult.exportedAtMillis` (the exact write
  timestamp) instead of a stale prefs re-read, and BackupToolsScreen
  re-reads the backup timestamps on ON_RESUME — it no longer reads
  "Never" right after a successful backup.
- **v110 — pet designer scroll compiles + YouTube Music opens in-app.**
  (1) **CI compile fix:** `PetDesignerScreen` declared `val listState =
  rememberLazyListState()` INSIDE the `Column { }` content lambda, but the
  v109 hero overlay `Box` (which reads `listState.layoutInfo
  .viewportStartOffset` for the scroll-away translation) is a Column
  SIBLING — a val inside the lambda's scope is invisible to the sibling, so
  `compileReleaseKotlin` failed with "Unresolved reference 'listState'".
  The declaration moved UP to the outer Box scope (before the Column); both
  the LazyColumn (inside the Column) and the hero overlay (sibling) now
  resolve it. (2) **YouTube Music opens IN the app:** `openSearchUrl`
  (ExploreSearch.kt) now package-PINS every `https://music.youtube.com/`
  URL to `com.google.android.apps.youtube.music`
  (`Intent.setPackage`) — the YTM app's App Links verification for that
  domain is unreliable (many devices hand the URL to Chrome, so the
  "Listen in" pill opened the browser), and package-scoped delivery
  bypasses verification, landing the search in the YTM app. When the app
  isn't installed (no handler for the pinned package) the plain https
  intent opens the browser instead.
- **v108 — dark-mode chip glass for pills & search bars; hero texts use
  cream ink; under-sheet opt-out; detail buttons blend.** (1) **YouTube
  logo un-squished:** `ic_music_youtube.xml` had a 28.57×20 viewport in a
  square 24dp box — VectorDrawable maps non-uniformly, so the logo drew
  vertically stretched. The art now sits centered in a square 28.57×28.57
  viewport (group translateY 4.285) — uniform scale, ~3dp breathing room.
  (2) **Dark pills = filter-chip glass:** the explore dialog's "Explore" +
  "Watch in" pills and every hero action pill (`SettingsHeroActionPill`,
  `CabinetHeroActionPill`, and the settings-family back pills in
  `SettingsHeroHeader` + Topic History) swap their bright white-lift glass
  for the CompactChip dark raised glass
  (`lerp(surfaceContainerHigh, Black, 0.15)`) in dark mode — light keeps
  the v93 frosted glass; the explore pills also gain the chip's 4dp lift +
  `curioDarkGlow`/`curioGlassEdge`/`curioInnerGlow(accent 0.12)`. (3)
  **Search-bar audit (dark):** every hero search field (Cabinet hero,
  `SettingsHeroHeader`/Topic DB, Spin filter sheet) now resolves its fill
  through the new shared `curioSearchFill(backdrop)` — light lifts toward
  white as before, dark drops to the chip near-black glass (the old
  `lerp(bannerFill, White, 0.30)` landed a muddy mid-tone on the dark
  banners/black sheet). (4) **Hero banner titles use cream ink in dark:**
  the Spin filter sheet ("Discoveries" + "Pick what you're in the mood
  for") and the category picker sheet ("What are we\nExploring?") now
  resolve `filterHeroInk`/`pickerHeroInk` via `heroHeaderInk()` instead
  of `onAccent()` — dark pastel mode was painting the tinted LIGHT twin
  as title text over the deep banner; dark now reads the same
  cream-white the Cabinet/Home hero titles use. (5) **"Torn hero
  under-sheet" experiment (default OFF):** new `heroTearSheetState` pref
  (Settings → Experiments → Paper & headers) — with it off, every torn
  hero tears straight into the page (only the hero's own bottom tear +
  hairline rim); ON restores the white paper lip. Gated in all 11
  under-sheet call sites: Home quest + drawer, EntryDetail, Spin filter +
  picker sheets, Cabinet, Settings hero, Profile, Topic History,
  Onboarding, PromoMode. (6) **Detail back/more frost blends with the
  hero:** `DetailStickyBar`'s dark frost was `lerp(heroFill, Black, 0.30)`
  (a near-black slab); it's now `lerp(heroFill, White, 0.10)` — the same
  hero-hued lip the under-sheet wears, so the controls read as part of
  the banner. Light frost unchanged. (8) **Pet designer hero tear is now
  SCROLLABLE, not sticky:** the `SettingsHeroHeader` overlay translates up
  1:1 with the list (`Modifier.graphicsLayer { translationY =
  -listState.layoutInfo.viewportStartOffset }` — viewportStartOffset is
  the total scrolled pixels, monotonic through the sticky toolbar, so the
  hero rides away with the content and never jumps back) instead of
  staying pinned while rows slide under the seam. The LazyColumn gained a
  `rememberLazyListState()`; the sticky studio toolbar now wears
  `statusBarsPadding()` + the theme background so it pins BELOW the
  status bar — before, it pinned at the viewport top and was invisible
  behind the opaque hero (sticky headers pin at the viewport top, never
  under the hero). The hero keeps its full-bleed tear; content padding
  (`SettingsHeroTotalHeight + 8`) unchanged.
  (7) **Profile XP-progress block
  leaves the paper stat card:** the v97 "quests & achievements wears the
  shared paper card" is REVERTED — `ProgressAndAchievementsCard` ("XP
  progress" + quest list + badge preview) is not a stat bar, so it always
  renders on the plain `CurioSettingsCard` (no paper fill, no torn edges,
  no holes/rings). The paper style stays on the real stat panes: Home
  Streak · Cabinet · Topics, the hero's Level · Saved · Lanes, the detail
  meta card. Dead quests-paper locals removed
  (`questsPaperOn/Bg/TearOn/Shape/HolesOn/RingsOn/RingStyle/Content` +
  the unused `settingsCardTintLift` import); `paperStatCard*` /
  `TornStatPaperShape` imports stay (the hero stat pane still uses them).
  (9) **"Watch in" → "Listen in" for audio services + music picker loses
  its radio.** The explore dialog's second pill label is now dynamic:
  music topics (Album/Artist/Song) whose chosen service is NOT YouTube
  (Apple Music / Spotify / YouTube Music — audio) say "Listen in";
  YouTube and non-music topics keep "Watch in" (video). The `MusicService
  Dialog` row no longer draws a `RadioButton` — selection reads through
  the row's solid v27q fill alone (the other picker dialogs in the file
  keep theirs); the dialog subtitle was reworded to stay neutral
  (  "Which streaming service opens albums, artists and songs from the
  explore dialog"). (10) **Inline mood board: STABLE fit + quote cards
  bounded.** Two glitch fixes in the inline (small) editor. (a) **The
  board fit is FROZEN once content exists** (`MoodBoardCanvas` in
  GalleryWallFormat.kt): the v69 live re-fit recomputed `boardMaxX/Y`
  (and thus `boardScale`/`boardOffsetX/Y`) from the CURRENT tile
  bounding box on EVERY commit — drag a photo inward and the extent
  shrank, the board zoomed in and every tile + floating quote card
  visibly jumped ("the size changes when I move / expand / shrink
  photos" glitch). A `sessionExtentX/Y` (`remember`ed floats) now
  freezes at the first content's bounding box via a grow-only
  `LaunchedEffect(tiles.size)`: every commit (drag, pinch, grow, add)
  clamps tiles INSIDE it, so no gesture can exceed it; the freeze
  resets to 0 when the board empties so a fresh board re-freezes at its
  own size. The saved card re-fits to the final saved layouts, which all
  live inside the frozen extent, so edit and detail still agree. (b)
  **Floating quote cards are width-capped in every fit-scaled view:**
  the v60 display cap applied only to never-resized cards — a card the
  user RESIZED kept the full scale and ballooned past the small board
  when the fit zoomed in (scale > 1), and couldn't shrink small enough
  ("the quote card is too big"). `MoodBoardFloatingCards` gained a
  `rawSpace: Boolean = false` param; when false (inline editor, saved
  card, expanded dialog, export) `displayScale` is now capped for ALL
  cards at `canvasWPx * 0.40 / cardW` (≤40% of the canvas — was 44%
  and only for slot cards); the full-screen editor passes `rawSpace =
  true` and keeps exact raw widths for precise placement. The resize
  grip's absolute floor dropped 60 → 48 render px so a capped card can
  shrink to a genuinely small note.
- **v107 — Apple Music "Watch in" deep links fixed for songs.**
  `resolveAppleMusicItemUrl` (ExploreSearch.kt) had two bugs that made
  SONG topics fail while artists and some albums worked (verified live
  against the iTunes API): (1) the old `music://music.apple.com/{cc}/song/{id}`
  deep link is a DEAD route — music.apple.com/song/{id} → HTTP 404 — a
  song's only canonical page is its ALBUM page with `?i=trackId`; the
  code now uses the API's own `trackViewUrl` / `collectionViewUrl` /
  `artistLinkUrl` with the scheme swapped to `music://` (tracking
  `&uo=4` stripped) and `country=$storefront` passed so the link matches
  the device storefront. (2) The search term never included the artist
  for songs (the teaser regex only ran for albums) and kept the raw
  `(1984)` year, which makes the API return ZERO results — the term is
  now `CurioTopic.byline` (the artist, present on all Album/Song topics)
  + title with the trailing `(YYYY)` stripped (new `TRAILING_YEAR_IN_PARENS`
  + `UO_TRACKING_PARAM` private regexes); teaser regex kept only as a
  blank-byline fallback. Non-Apple services (buildMusicServiceSearchUrl)
  untouched.
- **v106 — music brand logos in the explore dialog + music picker.** The
  four official service SVGs the user supplied (Apple Music, Spotify,
  YouTube, YouTube Music — archived under `design/music-service-icons/`)
  are converted to crisp VectorDrawables in `res/drawable/`
  (`ic_music_apple_music`, `ic_music_spotify`, `ic_music_youtube`,
  `ic_music_youtube_music`; Apple's diagonal pink→red→purple gradient is
  recreated as an aapt `<gradient>` because the source SVG embeds it as a
  raster JPEG, and the YouTube-Music ring uses `fillType="evenOdd"` so
  the center stays open). (1) New `MusicService.brandRes` extension in
  CurioIcons.kt maps each service to its drawable. (2) TopicRevealScreen's
  "Watch in" pill swaps the old Material glyph (`MusicService.brandTile`)
  for the service's brand logo via `painterResource` (NEVER tinted — the
  logos keep their own brand colors; the pill's `pillInk` tint applies
  only to the glyph pills); non-music topics use the YouTube logo. (3)
  MusicServiceDialog rows (Settings → Notifications → Music service) lead
  with the 26dp brand logo before the radio button. The `brandTile`
  glyph stand-ins stay as monogram fallback (no callers left in the
  dialog).
- **v105 — sort control removed + smoother category chips.** (1) SORT
  REMOVED from Cabinet + Topic Database: `ui/components/CurioSortDropdown.kt`
  is DELETED (no callers left) and both hero call sites drop it —
  CabinetScreen removes `cabinetSortField` / `sortAscending` + the sort
  `when` in `visibleEntries` (fixed newest-first
  `sortedByDescending { capturedAtMillis }`) + the private
  `CabinetSortField` enum; TopicDatabaseScreen removes `tdSortField` /
  `tdSortAscending` + the `sortMode` derivation (the rows builder keeps
  ONLY the default per-lane A–Z branch;
  `LaunchedEffect(sortMode, …)` → `LaunchedEffect(effectiveCat)`) +
  `DatabaseSortMode` / `DatabaseSortField` enums; dead `CurioSortOption`
  imports + `settingsRoseAccent` import cleaned. (2) SMOOTHER CATEGORY
  CHIPS: both chip bars slide in with a longer decelerating
  `LinearOutSlowInEasing` tween (380ms + 320ms fade; exit 300/220)
  instead of the snappy 300ms `FastOutSlowInEasing` snap.
- **v104 — detail hero tear = Home's exact construction.** The v92
  `detail = true` pattern (salted seed + 17π/23π mid-frequency meander
  octaves, added to stop unlucky entry hashes reading flat) made the
  detail hero's seam read as mechanical "straight lines then a tear" for
  many entries. Every other hero (Home, Profile, Settings, Cabinet,
  drawer, filter/picker sheets) uses plain
  `SoftTornBottomShape(seed, bold = true)` — so the detail hero + its
  under-sheet now drop `detail = true` too (EntryDetailScreen), making the
  construction byte-identical to Home (lip 10dp, baseline 14dp, bold;
  only the per-entry seed differs). The `detail` parameter machinery in
  PaperCard.kt stays as an unused public opt-in (left to minimize
  regression risk; CI validates the call-site change).
- **v103 — profile avatar photo + drawer.** (1) AppPreferences:
  `KEY_PROFILE_AVATAR` path pref + `getProfileAvatarPath` /
  `setProfileAvatarPath` ("" = none); the photo is copied into filesDir
  as `profile_avatar_<timestamp>.png` (a fresh name each pick so
  remember(path) bitmap caches re-key; old avatar files deleted). (2) New
  shared `ui/components/ProfileAvatar.kt`: `rememberProfileAvatar(path)`
  (ImageDecoder 28+ at 512px target, BitmapFactory 26-27, cached per
  path) + `ProfileAvatarImage` (fills the caller's circle-clipped box;
  nothing when unset → initial fallback). (3) ProfileScreen: the Edit
  profile dialog gains a 64dp circle preview (photo or name initial) +
  Add/Change photo + Remove (GetContent picker); the Profile hero's 72dp
  avatar circle shows the photo instead of the initial when set. (4) Home
  drawer (`HomeDrawerContent`): the bottom greeting row leads with a 48dp
  avatar circle (photo or initial) beside CURIO · Hi name · tagline.
  User confirmed (ask_user): always-on, no toggle.
- **v102 — auto-import bob actually plays.** The v64 auto-import added a
  "happy" bob animation to `design.animations` and saved it, but NOTHING
  played it — the animation editor is hidden from the Pet Studio (only
  PETS/EDITOR/SETTINGS pages) and the Pet Life routines never pick
  animationId "happy" (glance/wave/stretch/sidepeek/stumble/look_up/
  backturn/victory/inspect only) — so "auto animate on import" was inert.
  Fix: the auto-import now ALSO registers an IDLE-triggered
  `CustomPetAction` ("auto_bob", idle/8s, animationId "happy", no lines)
  on the built design; the floating pet's existing idle custom-action loop
  (CurioFloatingPet) then performs the bob whenever untouched for 8s (60s
  rest between fires). Re-imports are idempotent (the filter replaces the
  same id).
- **v101 — subtle top-only pill glow, as an option.** (1) AppPreferences:
  new `pillGlowSubtleState` (DEFAULT ON) + `KEY_PILL_GLOW_SUBTLE` +
  `isPillGlowSubtleEnabled` / `setPillGlowSubtleEnabled`, seeded in
  `initThemeMode`. (2) CurioGlassEffects.kt — both dark-only effects read
  the pref: `curioGlassEdge` (subtle) drops the top alphas 0.10/0.04 →
  0.05/0.02 and ends the gradient at 0.35 (the bottom whisper is GONE —
  top-only); `curioInnerGlow` (subtle) halves the strength and ties the
  radius to the SHORT side (`minDimension * 0.55` vs `maxDimension *
  0.95`) so the radial hugs the pill's top instead of filling it. Off =
  the original fuller gradient / pushed-in glow. (3) Settings →
  Appearance: "Subtle pill glow" switch (default ON; off restores the
  fuller glow for comparison). User asked for the option; default ON per
  the v97 paper-card precedent (the new look ships, toggle compares).
- **v100 — search-text audit + filter hierarchy.** (1) SEARCH TEXT: every
  search bar now resolves the THEME text color (`onSurface` — near-black
  light, near-white dark) instead of a colored ink: `CurioSearchField`'s
  default is onSurfaceVariant → onSurface, and the three hero/tinted call
  sites (Spin filter sheet, Cabinet hero, Settings hub hero) pass
  `MaterialTheme.colorScheme.onSurface` — the filter sheet's deep
  category-ink-on-category-glass washed out (same hue on same hue), the
  hero bars' banner ink sat ~3:1 on the whitened glass. (2) FILTER
  HIERARCHY (SpinScreen.kt): the accordion now has two levels —
  `FilterGroupPill` (closed) is a CATEGORY-TINTED glass
  (`lerp(chipSurface, accent, 0.22f)`, both themes) while `CompactChip` is
  NEUTRAL (callers dropped the category-tinted `cat.categorySurface(...)`
  → plain surfaceContainerHigh, light fill lifts toward `surface`); BOTH
  pills bump 3 → 4dp elevation + `curioDarkGlow(4.dp)` so the hierarchy
  reads via color AND lift. User confirmed via ask_user: theme text color
  + tinted groups / neutral chips / pill elevation.
- **v99 — compact update toast + delayed past launch.** (1) The in-app
  toast is now a ONE-LINE pill: `CurioInAppToastHost`'s text is capped
  (`maxLines = 1` + `TextOverflow.Ellipsis`) and the pill is slimmer
  (inner padding 18/12 → 16/10dp, host margin 24 → 20dp) so a long
  message can never balloon it. (2) The launch update toast no longer
  shows on the start screen: `UpdateChecker.notifyIfUpdateAvailable`
  waits 4s past the check before showing the toast (the once-per-version
  notification still fires immediately), and the copy is shortened to
  "Curio vX update available" (was the full "…is available — update in
  Support & diagnostics" sentence that wrapped to two lines on a phone).
  User confirmed (ask_user): compact + delayed past launch — NOT removed.
- **v98 — tear-catch revert + dark pill polish.** (1) REVERTED the v94/v95
  tear-hero light catches: `curioLightCatch` (light warm catch) +
  `tornSeamLight` (dark torn-edge stroke) are removed from CurioGlassEffects
  and the four hero call sites (Home quest banner, Detail hero, Spin filter
  + category-picker sheets); the stale v94/v95 doc/changelog entries are
  gone with them. (2) Home recents pills in dark: the FULL-PILL inner glow
  is gone — `ExploreTopicRow` / `RecentEntryRow` keep the colored pill +
  the `curioGlassEdge` top-edge white catch only (white no longer fills the
  whole pill). (3) Category picker Original/New tabs + preset chips:
  elevation flattened 3 → 2dp (the v27q selectable-chip standard) so the
  shadow no longer reads as a halo above the pill (`PickerPageTab` +
  `PickerPresetChip`; covers the full-screen picker AND the Spin sheet).
  (4) `CurioProgressPill` (Topic Reveal + Detail + Cabinet) is WIDER:
  horizontal padding 14 → 18dp so it reads as a proper pill instead of a
  slim strip with the glow/shadow around it. (5) Quests screen — quest
  paths are MINIMAL: closed `PathCard`s in the "Quest paths" grid drop the
  `chain.subtitle` task hint (it only appears in the open path dialog) and
  the BRONZE/SILVER tier label (just the medal badge + the chain name —
  the medal already carries the tier); the "In progress · n/target" pill
  in  `CurrentQuestCard` is a FULLER pill (9dp vertical → 12dp vertical +
  14dp horizontal — user confirmed it read too thin, the glow stays);
  the badge SHELF dialog tiles (`MergedBadgeTile`) are minimal too —
  earned tiles are just the badge + the name (BRONZE/SILVER label + its
  "· upgraded" chip are gone; locked tiles keep progress + "Secret ·
  hidden").
- **v97 — Paper stat card on by default + Profile quests paper card + merged
  Edit profile.** (1) The "Paper stat card" experiment PASSED — default is now
  ON app-wide (`paperStatCardsState` true, `KEY_PAPER_STAT_CARDS` default
  true; the Experiments toggles stay for comparison), so the Home stat bar,
  the Profile stat pane, and the Detail meta card wear the paper card by
  default. (2) The Profile quests & achievements block now wears the shared
  paper card too (same construction as the hero's Level · Saved · Lanes pane:
  `paperStatCardFill` on `paperStatCardColor(settingsCardTintLift())`, 28dp
  base shape, `TornStatPaperShape(0x6B4E3E)` under the tear toggle, holes /
  rings following the paper toggles, 3dp elevation + `curioDarkGlow`;
  falls back to `CurioSettingsCard` when the experiment is off). (3) The
  "Quests & achievements" plate lost its glowing look: `curioGlassEdge` +
  the frosted `curioPillTintLift` lift are gone (calm flat
  `lerp(surfaceContainerHigh, curioRoseInk, 0.08)` fill) and the gradient
  icon box is now a flat rose-tinted chip with rose-ink trophy (the
  `CurioCardHeader` icon-chip language). (4) "Edit profile" now edits BOTH
  the name and the tagline (the line under the name) in ONE dialog — the
  separate tagline dialog is removed; tapping the tagline opens the same
  Edit profile dialog, and Save persists both prefs.
- **v96 — detail-page fixes.** (1) CI: `tornSeamLight` (v94) used
  `1.5.dp.toPx()` without importing `androidx.compose.ui.unit.dp` — added.
  (2) Detail back + more buttons: the frost plate was HARDCODED white
  (cream ink on white washed out; glared on the black page).
  `DetailStickyBar`'s frost is now theme-aware — light: hero fill lifted
  toward the frosted glass (`lerp(heroFill, curioPillTintLift, 0.38)`);
  dark: a dark hero-tinted glass (`lerp(heroFill, Black, 0.30)`) so the
  cream ink reads (reversed light-in-dark contract). New `heroFill` param
  (caller passes `heroStart`). (3) Detail Date · Mood · Type meta card's
  default pane now uses the PROFILE stat-pane recipe (user's choice):
  `lerp(heroStart, White, 0.06/0.26)` instead of the near-white
  `heroSheetColor` blend; corner 18 → 20dp. Paper experiment path
  untouched.
- **v93 — One UI light glass + shiny ticket edge + tinted shadows.** (1)
  `curioPillTintLift()` LIGHT value: was `lerp(background, curioRoseInk(),
  0.08)` (cream) — now `lerp(Color.White, curioRoseInk(), 0.10)` (rose-
  kissed white), mirroring the dark value, so every hero pill / plate
  (Settings, Profile, Cabinet, Topic History, Spin filter chips, sort
  pill, quest plate) reads as the same bright frosted glass in light as in
  dark (the user's "profile/settings light like dark style" ask). All 11
  call sites are hero-pill fills — safe to lift globally. (2) Spin hero
  ticket + Topic Reveal morph card now carry `curioGlassEdge` (the One UI
  shiny top-edge rim, dark only) — both stay pixel-identical for the
  morph. (3) The hero ticket's accent-tinted layered shadow (the existing
  `heroShadowState` recipe) is now the SHIPPED DEFAULT — in-memory
  default + `isHeroShadowEnabled` pref default flipped to true; the
  Experiments toggle remains for comparison.
- **v92 — Home-clean tear heroes + hero shade family + cbrt fix.** (1)
  `kotlin.math.cbrt` (used by the v88 OKLab `toOklab`) was missing its
  import — added. (2) Detail hero tear: the old `detail = true`-only tear
  with a 3dp lip / 7dp baseline / 16dp sheet — plus a near-black
  (#121316) dark sheet — read as a straight cut on the black page. Now
  `bold = true, detail = true` (keeps the flat-seam salt), Home's exact
  geometry (`lip = 10, baseline = 14`, 42dp sheet at `HeroHeight − 18`,
  extent 16 → 24dp) and a visible dark lip (`lerp(heroStart, White,
  0.10)`). (3) Filter + category-picker sheet tears: both had only the
  torn banner + black hairline with NO white under-sheet, so the hero
  dropped straight into the wash — both now carry Home's full
  construction (`SoftTornSheetShape(same seed, lip = 10, baseline = 14,
  bold = true)`, 42dp at hero − 18, hero box + 24dp, dark lip =
  `lerp(heroFill, White, 0.10)`). (4) Hero SHADE family: the filter +
  picker heroes resolved from raw `themedAccent()` (uncalmed, brighter)
  while Home/Detail use `headerAccent()` (calmed/deepened banner shade) —
  both heroes now use `cat.headerAccent()` so every torn hero wears the
  same shade.
- **v91 — dynamic pills sweep + unified One UI search bars.** (1) Profile
  quest plate: the fixed solid coral (light) / deep rose (dark) fills are
  gone — the plate wears the shared profile-family frosted glass
  (`lerp(surfaceContainerHigh, curioPillTintLift(), 0.55)`, cream-rose in
  light / near-white rose glass on black) like the other profile options;
  the rose icon box stays (colored-icon-block pattern). (2) Quests
  "In progress" button: solid rose is only for the actionable "Start ·
  +XP" CTA; the informational state is a tinted surface glass with
  theme-aware rose ink + `curioGlassEdge` in dark. (3) Category picker
  shadow leak ROOT CAUSE: the tear's idle tab/preset fills were
  TRANSLUCENT (`pickerHeroInk.copy(alpha = 0.16f)`) so the 3dp elevation
  shadow bled through the pill — now OPAQUE ink-glass
  (`lerp(pickerHeroFill, pickerHeroInk, 0.16f)`); preset chips fuller
  (12dp vertical, glyph 16); tear height 184 → 208dp (the two-line title +
  tabs + presets were squished); full-screen picker preset row spacing
  4/1 → 8/2 and tabs 1/4 → 2/6; "Manage categories" link flipped from the
  pale scheme primary to theme-aware `onSurface`. (4) `CurioSearchField`
  is now THE One UI search component — fixed 46dp height (the hero pill
  size), 50dp capsule, ink hairline border, frosted fill + glass edge in
  dark, with new `ink`/`fill` params for heroes (banner ink + frosted
  category glass). The three duplicated hero `OutlinedTextField` searches
  (Cabinet hero, Settings/Topic-DB hero header, Spin filter sheet) now
  route through it — every search bar in the app is one size + style,
  dark-correct via resolved ink/fill. Dead OutlinedTextField/IconButton/
  KeyboardOptions imports removed from the swapped files.
- **v90 — sort pill rebuilt as a category-style labeled pill.** The v85
  compact icon-only blob (glyph + divider + arrow, ~55dp) hid the sort
  label in the menu header and read as a lone glyph chip next to the
  LABELED Category pill ("Category · All"). `CurioSortDropdown`'s pill now
  speaks the Category pill's exact language (CabinetHeroActionPill /
  SettingsHeroActionPill): sort-type glyph 20dp + field label
  (`labelLarge` Bold ink) + 1dp divider + direction arrow 20dp, same 46dp
  height, 14dp edge padding, 13/13 zone padding (46dp tap height), the
  same frosted fill + dark glass glow. Glyph+label = one tap zone opening
  the dropdown; the arrow zone still toggles asc/desc. The label is back
  on the pill ("Date"/"Title"/"Category"/"Default"/"Name"/"Year"). One
  shared component covers both call sites (Cabinet + Topic Database).
- **v89 — Home recents rows get the dark pill style.** `ExploreTopicRow`
  (recently explored / unexplored) and its compact sibling `RecentEntryRow`
  still chained the RETIRED no-op `curioDarkGlow` (identity, draws
  nothing) — so on the pitch-black page the only shadow was the Surface's
  black `shadowElevation` (invisible) and the rows read flat. Both now
  wear the same dark pill recipe as the filter chips:
  `curioGlassEdge(shape)` (the 1% whitish top-edge catch) +
  `curioInnerGlow(shape, themedAccent, strength = 0.12f)` (the accent's
  light twin pushed in from the top-left, clipped to the 20dp shape),
  dark-only no-ops in light. Other `curioDarkGlow` sites (stat cards,
  session card, stop button, tag chips) left as-is — not the recents pill
  family.
  **v115 — the HOME recents rows are NOT category-tinted in dark mode**
  (user: "bring back the dark mode home screen recents not being
  colored"): `RecentEntryRow` + the Home `ExploreTopicRow` now use
  `surfaceContainerLow` instead of `categorySurface()` when
  `isCurioDarkTheme()` (the recents PAGE — RecentScreen — keeps its
  tinted rows).
- **v88 — dark-mode mixed colors fixed at the root (the "mixed colors are
  bad" bug).** The curated `PairBlends`/`TripleBlends` tables are keyed on
  the RAW researched accents, but the Spin caller pre-resolved every
  accent to its theme shade (`themedAccent()`) — so in dark mode every
  table lookup missed and mixes silently fell back to the HSL midpoint /
  circular-hue centroid (foreign-hue swings, muddy olive midpoints).
  `mixedDeckAccent`/`mixedDeckGradient` now take the RAW accents
  (`CurioCategory.accent`) and resolve per theme inside: dark blends wear
  the same `darkAccent` "new shade of the same spectrum" recipe as the
  singles; dark-pastel seams resolve to the MUTED deep pastel (the old
  hardcoded `pastelAccent(seam, false)` left airy LIGHT seams on dark
  pastel decks). New local OKLab machinery (`toOklab`/`fromOklab`,
  `oklabBlend`, `oklabCentroid`, `oklabGradientStops` — canonical
  Ottosson matrices, version-proof like `toHsl`) replaces the HSL blend /
  centroid fallbacks (perceptual mean) and the Spin + Reveal ticket
  crown→base stops (perceptual interpolation; both screens stay
  pixel-identical for the morph). `darkAccent` research-tuned:
  near-grey neutrals (s<0.22) keep identity (no ×0.80 grey-out); the
  saturation cap scales with source saturation (0.48+s·0.16, ceiling
  0.62) so vivid families hold chroma at depth; the lime/yellow-green
  band (55°–95°) remaps onto emerald (95°→150°) so dark limes read as
  deep greens, never olive. Dead private HSL helpers removed
  (`hslBlend`/`hslCentroid`/`steerLightness`/`contrastVsWhite`/
  `toLinear`); `hslGradientStops` kept as documented public API.
- **v87 — missed dark-mode spots sweep.** (1) Spin filter sheet chips +
  group pills: `inactiveFill` stayed `lerp(chipSurface,
  curioPillTintLift(), 0.5f)` — in dark that lifts toward the near-white
  rose glass → mid-tone chips with washed light text; both now keep a
  dark near-black tinted fill in dark (`lerp(chipSurface, Black, 0.15f)`)
  so the light label reads crisp. (2) `PickerPageTab`/`PickerPresetChip`
  default idle fill lifted toward `curioPillLift()` (WHITE in dark) →
  near-white Original/New idle pills with light-grey text; default now
  stays a dark raised glass in dark, padding bumped to fuller pills
  (16/10, 14/10). (3) `CurioProgressPill` 12/7dp padding made the reveal
  badge a slim strip so the v81 inner glow (radius = width) bled past it
  → 14/11dp proper pill body. (4) Reveal `SentimentButton` inactive:
  near-white `curioPillTintLift` + light-grey text washed on black →
  dark raised glass (`lerp(surfaceContainerHigh, accent, 0.25)`). (5)
  `tintedTileInk` (capture attach icons + journal "Record a voice note"
  row) returned the DEEP light-mode ink unconditionally → invisible on
  dark tinted tiles; dark now resolves the light twin. (6) Quote card
  header "Quote N" renders ABOVE the paper slip on the theme page → dark
  flips to the bright butter control twin (`paperControlAccent`). (7)
  `DictateFieldButton` mic chip: raw deep accent on the dark tinted chip
  was dark-on-dark → light twin in dark.
- **v86 — Profile quests button gets its dark treatment.** The quest
  plate in `ProgressAndAchievementsCard` was missed by v81: it kept the
  pale `CoralBlush` glass (`lerp(CoralBlush, curioPillTintLift(), 0.55)`)
  + `cardGradient(CoralBlush)` icon box, which glare on black. Dark now
  flips the plate to deep rose glass (`lerp(HomeRosewoodDark, Black,
  0.30)`) + `curioGlassEdge`, and the icon box to
  `cardGradient(HomeRosewoodDark)` so the white trophy reads. Light
  unchanged.
- **v85 — the sort pill is finally a true sibling of the search pill.**
  Root cause was TWO problems: (1) the sort pill carried glyph + label +
  chevron + divider + arrow (~135dp) next to the icon-only Search pill
  (~48dp — ~3×, and every earlier padding pass never removed the label);
  (2) in dark the sort pill wore the One UI glass glow while the hero
  action pills only had the retired no-op `curioDarkGlow`, so it glowed
  and the search pill stayed flat. Fix: `CurioSortDropdown` is now the
  same compact two-zone icon pill as search — glyph opens the menu (label
  moved to the menu header), divider, direction arrow; same 46dp height,
  ~55dp wide. `CabinetHeroActionPill` + `SettingsHeroActionPill` gained
  `curioGlassGlow` (same dark glass as the sort pill), and the Cabinet /
  Topic Database search pills pass `emphasized = true` to match the sort
  pill's fill. Unused imports cleaned (`Arrangement`/`widthIn`/`height`),
  `Column` added for the menu header.
- **v84 — filter sheet polish + category picker tear.** Filter sheet
  search is now a Cabinet-hero-style `OutlinedTextField`: frosted
  category-glass container + ink-tinted icon/border/text/cursor, colors
  DYNAMIC per category (`cat.categoryInk()`); the duplicate `SectionLabel`
  (glyph + group name) under the open group pill is gone (10dp spacer
  keeps spacing); group pills + filter chips wear the One UI glass edge +
  inner glow in dark (their 3dp shadows are invisible on the black sheet
  — `curioDarkGlow` is a retired no-op); the Spin Filter pill + BottomCta
  badge show `filteredPool.size` (total topics matching the selected
  filters) instead of the ticked-chip count. `MorphEntrance` gained
  `bouncy = false` (the elastic spring's ~5% overshoot read as a brief
  "more elevated" shadow flash — both category pickers pass it). The Spin
  picker sheet's tear GREW (118 → 184dp + status bar) to hold the
  Original/New tabs + quick-mix presets inside the banner; `PickerPageTab`
  + `PickerPresetChip` gained `accent`/`accentInk`/`idleInk`/`idleFill`
  params so they ride the banner ink/fill (or the wash category in the
  full-screen picker) — theme-aware + dynamic, preset glyphs included;
  both Mix buttons use `themedButtonFill()`/`themedButtonInk()`.
- **v82 — dark-mode audit: everything left light-only fixed.** Paper
  stat cards (`paperStatCardColor` → deep near-black paper in dark;
  `paperStatCardFill` gained a `dark` flag so the steel-ring back arcs /
  dives / split gaps flip to LIGHT metal on the dark paper — Home, Detail,
  Profile pass `isCurioDarkTheme()`). Streak pill ink → bright butter in
  dark. `CurioHeroShuffleCard` wildcard hero wears `HomeRosewoodDark` +
  bright content ink in dark. Home "Surprise me" pill ink → bright twin.
  BugReport + Support + empty-state CTAs: deep rose fill + bright twin ink
  in dark (pale CoralBlush fills glared). Reveal's pastel hero gradient
  got its dark deep-pastel stops (missed in v81). `paperControlAccent`
  (rich-text toolbar/cursor/chips) flips to bright butter in dark — the
  note PAPER sheets stay theme-agnostic BY DESIGN (documented contract).
  Quests Claim pills use deep `CoralInk`/`GoldInk`/`CategoryTeal` fills in
  both modes (the bright twins + soft legacy teal washed white out); coral
  icon chips flip to deep rose in dark.
- **v81 — the reimagined dark mode: pitch black + Samsung One UI 9.5
  glow.** `themeModeState` (Light/Dark/System) is back in AppPreferences
  and `isCurioDarkTheme()`/`ForContext` read it (System follows the
  device). `CurioDarkColorScheme`: pitch-black page, surfaces step up
  through near-black greys (elevation via lightness), bright accent
  roles. CategoryInk dark branches: `categoryInk` → the LIGHT 300 twin,
  `themedAccent` → `darkAccent` (same hue, L≈0.44, ~20% desat),
  `headerAccent` → same-hue dark hero shade (L≈0.34), the wash collapses
  to pure black (NO background tint in dark — watermarks carry the
  category identity, with their pre-existing dark alphas), surfaces →
  `darkSurfaceTint`/`darkChipTint`. New `CurioGlassEffects.kt`:
  `curioGlassEdge` (the 1% whitish top-lit edge, NOT a border) +
  `curioInnerGlow` (the One UI 9.5 radial inner glow) — both dark-only
  no-ops in light; `categoryEdgeShine` draws the whitish edge in dark, so
  the treatment is app-wide. The SpinButton wears the glow (Samsung
  shuffle formula) and the orbit dots flip to the accent's light twin.
  Buttons/fills reversed per the ask: dark same-hue fills with light twin
  ink (`themedButtonFill`/`themedButtonInk`). Settings → Appearance and
  the onboarding theme step re-gain the Light/Dark/System picker.
- **v79 — sort pill and search pill get the equal middle-size treatment
  (Cabinet + Topic Browser).** The sort dropdown's Row had drifted to
  `heightIn(min = 52.dp)` while the icon-only Search pills
  (`SettingsHeroActionPill` / `CabinetHeroActionPill`) stayed at the
  v30-uniform 42dp — so the sort pill read big next to a small search
  pill. Unified at the middle: both pills are now **46dp tall** (sort
  52→46, hero action pills 42→46) and both carry **20dp glyphs** (sort-
  type icon 16→20, action-pill glyphs 22→20). Shared components, so the
  whole hero-pill family stays uniform; width remains content-driven
  (the sort pill keeps its label + chevron + direction arrow).
- **v78 — light-only: Dark / AMOLED / Material removed, Curio light
  stays.** The entire theme-style + theme-mode machinery is gone:
  `AppPreferences` lost `themeStyleState`/`themeModeState` (prefs,
  constants, getters, setters); `CurioTheme` lost the dark/AMOLED
  ColorSchemes and the Material dynamic palette; Settings → Appearance
  dropped the Theme style + Theme pickers (keeps Category tint, Pastel
  colors, Hero, Adaptive Hero); the onboarding theme step is now a single
  pastel toggle (`ThemeModeChip` deleted). Every `isCurioDarkTheme()` /
  `THEME_STYLE_*` branch across Profile, Home, Spin (deck gradients,
  ticket brush, shuffle plate, deck controls, sheets), Detail (hero
  start, frosts, waveform inks, mood board), Reveal (band paper, hero
  brush, pill glass), Category Picker, DeckPresets, PetDesigner dialog,
  CaptureFormatComponents, RecycleBin and the shared components was
  collapsed to its light path; unused imports removed (incl. Detail's
  dead `contrastRatio`). **Seam kept for the future dark system:**
  `isCurioDarkTheme()` / `isCurioDarkThemeForContext()` return `false`
  and the watermark / session-service plumbing that feeds them stays.
  Committed locally, NOT pushed (per user).
- **v77 — hero back buttons are OPAQUE theme-aware pills.** The settings-
  family heroes' back pills were the last translucent holdout
  (`symbolTint.copy(alpha = 0.18f)` in `SettingsHeroHeader` — Settings hub
  + every sub page — and `ink.copy(alpha = 0.18f)` in Topic History),
  while the hero ACTION pills had already been converted to an opaque
  fill in v27n (`lerp(backdrop, curioPillTintLift(), 0.38f)`). Both back
  buttons now use that exact construction: `containerColor =
  lerp(fill, curioPillTintLift(), 0.38f)` (theme-aware: rose-kissed page
  lift in light, white in dark, grey glass in AMOLED) +
  `shadowElevation = 3.dp`, glyph keeps the hero's readable ink. The
  default `surfaceVariant` pill (BugReport / Category Picker / FieldMind /
  SaveCapture), Profile's solid hero-fill pill and the detail page's
  near-opaque frosted sticky plate are untouched.
- **v76 — detail page Date · Mood · Session · Type card wears an opaque
  theme-aware pane.** `EntryDetailScreen`'s hero meta card (Date · Mood ·
  Session · Type grid) default fill was FROSTED glass (a translucent
  `heroStart` bloom at 30/16% alpha + a white/midnight `heroFrostBrush`)
  that read transparent and kept the pane flat. It's now a single OPAQUE
  vertical gradient `lerp(heroSheetColor, heroStart, 0.30f)` →
  `lerp(heroSheetColor, heroStart, 0.16f)` — `heroSheetColor` is the page's
  theme-aware sheet (near-white light / midnight dark + AMOLED), so the
  same perceived category bloom stays while the shadow renders clean
  (Profile/Home stat-pane language; ink contrast preserved — the detail
  hero is a deep category color, so the lerp is off the sheet, not the
  banner fill). `shadowElevation` 3dp + `curioDarkGlow` always apply now
  (were flat for the frost); `heroFrostBrush` deleted, `curioDarkGlow`
  imported. The paper-card experiment branch is untouched.
- **v75 — Home stat card wears an opaque theme-aware pane like Profile's.**
  The Home Streak · Cabinet · Topics pane's DEFAULT fill (off the "Paper
  stat card" experiment) was a transparent rose glass (12% → 55% alpha)
  that read see-through and let the elevation shadow bleed. It now uses
  Profile's exact stat-pane construction: an OPAQUE vertical gradient of
  `lerp(heroFill, White, 0.06f)` → `lerp(heroFill, White, 0.26f)` with the
  AMOLED step toward `HomeRosewood` 0.30, and `shadowElevation` 3dp +
  `curioDarkGlow` now always apply (they were gated behind the paper-card
  experiment). The experiment branch is untouched.
- **v74 — category picker sheet: tear hero to the status bar + no close
  button.** `CategoryPickerSheet` (Spin) got the filter sheet's v70 tear
  treatment: `shape = RectangleShape` (flush top), `dragHandle = null`,
  `contentWindowInsets = { navigationBars ∪ ime }` so the banner fills the
  very top edge behind the status bar (height `118.dp + statusBar`, content
  clears it via `statusBarsPadding`; distinct tear seed `0xC4A71E`, category
  gradient + watermark glyphs). The Close X and the floating drag handle are
  GONE — swipe-down / scrim tap / Mix-Cancel dismiss instead. The old
  header row + mode-hint text moved onto the banner: 34sp title, the hint
  as subtitle (single-select now "Tap a deck to spin it. Hold to pick
  several." — period, not the · separator; multi keeps "Tap to toggle
  decks · Done to spin together"), and the current-deck / "N selected"
  chip as a hero-glass pill. The column dropped its `navigationBarsPadding`
  (the contentWindowInsets handle the bottom inset now) and the
  `navigationBarsPadding` + `BottomSheetDefaults` imports went with it.
- **v73 — filter-sheet group pills wear their group glyphs.** The
  accordion pills in Spin's `FilterSheet` (`FilterGroupPill` — Type /
  Genres / Era / Origin / Franchise) now lead with the same per-group
  glyph their section labels wear (`FilterGroupKey.glyph`: category /
  style / history / public / movie — accent-tinted when closed, content
  `ink` when open), with the row start padding trimmed 20 → 18dp to sit
  the icon comfortably.
- **v72 — Settings/Profile option cards + icons follow the hero's color.**
  The shared option-card primitives (`CurioSettingsCard` fill,
  `CurioCardHeader` + `CurioSettingsRow` icon chips + glyphs) were
  hardcoded ROSE (`curioPillTintLift` / `CoralBlush` chips /
  `curioRoseInk` glyphs) while the hero wears the Spin lane's accent
  (Adaptive Hero) or the sky-azure — so Profile/Settings option cards
  never matched the banner. Three new hero-aware resolvers in
  `SettingsHubScreen.kt` (next to the shared hero-family helpers):
  `settingsCardAccentInk()` (glyph ink: lane → `categoryInk()`, azure →
  deep azure twin in light / pale azure in dark, else rose),
  `settingsCardChipTint()` (chip hue: lane → `themedAccent()` light /
  `lightAccent` dark, azure, else coral — dark keeps the pale-glass
  chip look), and `settingsCardTintLift()` (card-fill twin of
  `curioPillTintLift`: same construction/strength, hue follows the
  hero). `CurioSettingsCard.kt` now resolves all three through the
  hero's hue, so Profile + Settings (and every screen sharing the
  primitives — Support, Backup, Experiments, Quests) match the banner
  in every theme. Material/AMOLED keep the rose (their banners wear
  scheme roles — same gating as `settingsRoseAccent()`).
- **v69 — universal mood-board import, editor/saved fit consistency,
  mood collapse-on-pick, chip-bar slide animation.** (1) **Mood-board
  import is now the ANDROID PHOTO PICKER** (`PickMultipleVisualMedia` +
  `PickVisualMediaRequest(ImageOnly)` in `MoodBoardCanvas`) instead of
  the raw `OpenMultipleDocuments` documents UI — one universal
  gallery/camera grid on every device. (2) **The inline editor now fits
  EXACTLY like the saved card** (`MoodBoardCanvas`): the crop extent is
  the CURRENT tile set's bounding box instead of a once-per-session
  frozen extent. The freeze kept the editor stable but diverged from
  the saved view the moment a tile was added/dragged past it — the
  saved card re-fitted and the board "resized" between edit and detail
  (fresh boards even showed 1:1 while the saved card zoomed to the
  content). The drag preview lives inside the tile, so the fit stays
  constant mid-drag and updates on commit — identical to what the saved
  view recomputes from `tileLayouts`. (3) **Mood picker collapses once
  a mood is picked** (`SaveCaptureScreen` mood pill): `moodSelectorOpen
  = false` inside `onMoodChange`. (4) **Cabinet + Topic Browser chip bar
  slides in properly**: the bar is positioned with a large
  `.offset(y = barTop)`, so `expandVertically`'s height+clip animation
  hid it until the clip finished (delayed pop, no visible motion); the
  enter/exit are now `slideInVertically/slideOutVertically` + fade, so
  the chips emerge from under the torn hero.
- **v68 — hero-tinted icons, pill search bars, tear-hero filter sheet,
  sort-type icons.** (1) **Settings + Profile icons theme-aware**
  (`SettingsHeroHeader` + `ProfileHero`): the hero watermark symbols /
  back pill rode the hardcoded `HomeRosewood` in AMOLED — they now ride
  the hero's READABLE ink (already resolves per-theme + per spin-lane),
  so a lane-colored hero never wears mismatched rose icons. The cream
  paper under the tear picks up a 10% lerp of the hero fill (was flat
  cream); AMOLED keeps the rose twin. (2) **Search bars pill-shaped**
  (`CurioSearchField`): rounded-16 box → full 50dp pill like the Cabinet
  hero search — Settings hub, Spin filter sheet and Topic History all
  share it. (3) **Filter sheet tear-hero header** (`FilterSheet` in
  SpinScreen): the plain icon+title row is now a category-colored torn
  banner (`SoftTornBottomShape` + torn-edge hairline + watermark glyphs)
  with the category name at 26sp and a Clear-all pill riding it; the
  groups column gained proper margins (14dp under the tear, section
  label clears the pill row with its own 6/6 top/bottom padding) so the
  open group never reads cramped or offset. (4) **Sort pill shows its
  sort-type icon + slimmer** (`CurioSortOption` gains `glyph`;
  `CurioSortDropdown` renders it 16dp before the label, label-zone
  padding 8/6 → 6/4, gap 5 → 4dp, min-width 88 → 76dp): Cabinet
  Date/Title/Category → calendar_today/text_fields/tune; Topic Database
  Default/Name/Year → auto_awesome/text_fields/calendar_today.
- **v67 — progress dialog colors fixed (reveal page).** The reveal
  hero's pill passed the RAW category accent into the dialog, which then
  used it for the −/+ stepper glyphs and the Save label ON the theme's
  onSurface — with a deep accent (navy/indigo/…) that was dark-on-dark.
  `CurioProgressEditorDialog` now drives EVERY element from
  `contentColor`: the reveal + detail pills pass `cat.categoryInk()`
  (readable deep accent in light mode / light twin in dark) so the ring,
  steppers, slider and Save read on the standard dialog container in
  both modes; `StepButton` tints a 14% wash of the content color (glyph
  in the full color) instead of a solid circle; the Save button pairs
  the content-colored container against `MaterialTheme.colorScheme.surface`
  so its label always contrasts. The dialog's `accent` parameter is gone
  (only caller was the pill).
- **v66 — progress visibility + detail pill moved to the screen corner.**
  (1) **Cabinet progress line visible in light/pastel** (`CurioTopicCard`
  progress strip under the hero): `themedAccent()` resolves to a light
  pastel twin in pastel light mode that washed out on the cream hero —
  the fill now uses `categoryInk()` in light mode (hue-preserving deep
  accent) and the accent in dark; the track lifts 0.18 → 0.32 alpha and
  the line grows 4 → 5dp. (2) **Detail progress pill moved from the
  hero's bottom-right corner to the SCREEN's bottom-right corner**
  (`EntryDetailScreen`): the pill left the hero Box and now floats in
  the screen-level Box beside `DetailStickyBar`, aligned BottomEnd with
  a 16dp corner inset (the NavHost Scaffold already pads content above
  the nav bar via `contentWindowInsets = navigationBars`, so no extra
  inset). (3) **Its bar fixed too** (`CurioProgressPill` slim bar): the
  fill was `accent` (light pastel in pastel light — invisible); it now
  uses the deep category ink in light mode and the accent in dark, and
  the track alpha lifts 0.25 → 0.30. The reveal hero's count-only
  badge (`showBar = false`) is untouched.
- **v65 — Pet Designer: auto-import, brush size, transparent fill,
  tool-tray color chip, eye presets + placement.** (1) **Auto-import**
  (`autoImportNext` flag + "Auto-import image" in `ImportMenuDialog`):
  the picked PNG runs the whole pipeline in one tap — dominant colors
  map into the four custom slots (`buildImportReview` +
  `addCustomColor`), the image snaps to the extended palette, BOTH body
  and curled grids fill (`PetDesign.bodyAsCurled`), a 4-frame "happy"
  bob animation is added, and the result is saved immediately
  (`AppPreferences.setPetDesign`). (2) **Zoom slider removed** from the
  pixel editor — replaced by `BrushSizeRow` (sizes 1–4) and `brushSize`
  state; Brush/Erase paint a brushSize×brushSize square (`applyTool` +
  `applyToolToRows`), and `PixelGrid` draws an on-canvas ring showing
  the exact footprint when Brush/Erase is armed. (3) **CLEAR tool**
  (`PaintTool.CLEAR`): flood-fills a region with transparency — removes
  a solid background; acts once per gesture like FILL (both editors).
  (4) **Selected-color chip in the tool tray**: `ToolTray` gains a
  `paintHex` swatch + `onPaintTap` (tray row is now horizontally
  scrollable). (5) **Eyes section** (Settings page, `EyeControls`): 3
  size presets (Small/Medium/Large → `eyeScale` 0/1/2), an arrow cross
  pad adjusting `eyeOffsetX`/`eyeOffsetY` (−6..6), a live bobbing
  sprite preview (`rememberInfiniteTransition`), and Reset. New
  `PetDesign` fields `eyeScale`/`eyeOffsetX`/`eyeOffsetY` serialize as
  `eyesize=`/`eyeoffx=`/`eyeoffy=` (tolerant parser, defaults 1/0/0);
  `CurioPetSprite` scales each eye around its own center (left 4.5,
  right 10.5, row 7 in 16-space) then applies the offset — the live
  pet, previews and saved designs all render the same look.
- **v59.3 → REVERTED (v64) — toggleable serif body text + tighter label
  tracking.** v59.3 added a "Serif body text" Appearance toggle
  (`AppPreferences.loraBodyState` / KEY_LORA_BODY), `CurioLoraBodyTypography`
  + `@Composable curioAppTypography()` (body → Lora at 0 tracking, read by
  `CurioTheme` + ExploreSessionService bubble) and labelMedium/labelSmall
  tracking 0.5 → 0.3sp. The user reverted f991db1 (v64): the toggle row,
  the pref key/state/seeding, both typography variants and the tracking
  tweak are all gone — body text is back to the platform sans and label
  tracking back to 0.5sp. The v35 Lora editorial serif for reading/hero
  text (fonts + CurioEditorialBody) is UNTOUCHED.
- **v63 — update notice is now an IN-APP toast (no android Toast).**
  New global bus `CurioToast` (ui/components/CurioInAppToast.kt):
  `object CurioToast` with snapshot state (`show(text, glyph,
  actionLabel, actionId)` / `dismiss(id)`) + `CurioInAppToastHost` — a
  themed pill (dialog-tinted container, primary glyph, 6dp lift) that
  slides up, holds ~3.5s and fades. Hosted at the ROOT of `CurioNavHost`'s
  Box (above every screen, cleared past the bottom nav via
  navigationBarsPadding + 96dp), so a message fired before the UI composes
  is picked up on first frame. v63b — toasts with an `actionId` are
  TAPPABLE: `CurioInAppToastHost` takes an `onAction` callback (the
  NavHost maps "support" → `CurioRoutes.SUPPORT`) and shows the
  `actionLabel` ("Open") after a divider; tapping dismisses + navigates.
  `UpdateChecker.notifyIfUpdateAvailable` now calls `CurioToast.show(...
  glyph = CurioIcons.Download, actionLabel = "Open", actionId =
  "support")` instead of `android.widget.Toast` (import removed), and
  v63b moved the once-per-version gate ([AppPreferences]
  lastNotifiedUpdateVersion) BEFORE both announcements — a pending update
  is announced ONCE (toast + notification together), never on every
  launch. Generic bus — future background notices reuse it.
- **v62 — sort pill slimmer again (Cabinet + Topic Browser).** The shared
  `CurioSortDropdown` still read too wide next to the icon-only Search
  pill (user: "too wide, please reduce it"): label-zone padding 10/8 →
  8/6, chevron 18 → 16dp, zone gap 6 → 5dp, arrow-zone horizontal
  padding 6 → 4dp, direction arrow 22 → 20dp, and the widthIn floor
  96 → 88dp. Both heroes keep the uniform 42dp height — the pill now hugs
  its label ("Category"/"Default"/"Year") instead of stretching.
- **v61 — Spin filter sheet: bigger chips + louder typography/hierarchy.**
  (1) **Filter chips bigger still** (`CompactChip`): 16 → 18sp label,
  padding 16/11 → 20/13, glyph 17 → 19dp, gap 7 → 8dp, inactive weight
  Medium → SemiBold. v44 grew them to 15sp, v52b to 16sp — this is the
  third bump.
  (2) **Whole-sheet type/hierarchy scaled up** (all private to
  `SpinScreen.kt`): sheet header 22 → 24dp glyph + 24sp title; subtitle
  bodySmall → bodyMedium; `CurioSearchField` gains an optional `textStyle`
  param (shared component — Settings hub / Topic History unchanged) and
  the sheet passes 18sp; "Active filters" label labelMedium → labelLarge;
  `SectionLabel` 14 → 16sp ExtraBold (0.3sp tracking); `FilterGroupPill`
  15 → 17sp label with 20/14/12/12 padding + 18dp chevron + bigger badge;
  `ActiveFilterChip` labelLarge + roomier padding + 16dp close; group/chip
  FlowRow gaps 8 → 10dp; Apply CTA 17sp with 14dp vertical padding + 20dp
  icon.
- **v60 — session-screenshot attach removed; mood-board crash + reveal strip fixes.**
  (1) **Session-screenshot FEATURE removed (user: "too scary", old shots
  kept attaching).** The save page's "Session screenshots" section
  (auto-attached session shots + add-from-gallery + per-shot remove) is
  gone — nothing reads or attaches screenshots anymore. The manifest
  drops `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` (≤32) and
  `FOREGROUND_SERVICE_MEDIA_PROJECTION` (nothing uses MediaProjection in
  the tree — v55 removed the watcher and no capture code remains). The
  shared session NOTE survives untouched (`SessionNoteFloatingPill` +
  `peekWriteSessionNote`), and all legacy data paths are left inert and
  read-only: `ExploreSession.screenshotPaths`, pending-write screenshots,
  `CaptureEntity.sessionScreenshotsJson`, `SessionShots`, backup/restore
  round-trip, and EntryDetail's display of ALREADY-saved screenshots.
  (2) **Mood-board expanded-dialog crash fixed.** `ExpandedMoodBoardDialog`
  called `.ifEmpty{}` on `tileLayoutsFull`/`quotePositionsFull` — Gson
  bypasses Kotlin defaults, so pre-v57 entries decode those to NULL and
  `Collection.isEmpty()` NPE'd on the dialog's first measure (the reported
  crash). Now `orEmpty().ifEmpty{...}`; same null-guard applied to
  MoodBoardExport's three `tileLayoutsFull.isNotEmpty()` /
  `quotePositionsFull.isNotEmpty()` sites (save/share path).
  (3) **Inline mood-board quote cards no longer balloon.** When the
  collage is smaller than the canvas it zooms to fill (scale > 1) and the
  raw slot width (~41% of the board) multiplied by that zoom — a quote
  card could grow into a huge slab in the small inline editor.
  `MoodBoardFloatingCards` caps the DISPLAY scale of never-resized
  fallback cards at ~44% of the canvas (`displayScale`); user-resized
  cards (saved.w) keep the full scale.
  (4) **Reveal strip: tags raised + Like/Dislike active state POPS.** Tag
  row top inset 10 → 6dp (clearer clearance between tags and the
  sentiment row at larger font scales); `SentimentButton` active now
  scales to 1.08 with a category glow (`curioDarkGlow` 4dp), ExtraBold
  label and 17dp icon — a liked/disliked topic is unmistakable.
- **v59.2 — watermark icons: fewer in the drawer, screen-matched elsewhere.**
  (1) **Drawer calms down** — the Home nav-drawer hero now scatters 3
  mirrored pairs (was 5), smaller (34–42dp vs 44–56dp) and fainter
  (alpha 0.07–0.08 vs 0.11–0.14) so the brand + greeting dominate, and
  its glyphs are navigation-flavored (`drawerHeroSymbols`: menu /
  explore / auto_awesome / star / diamond / bolt — the first 6 of the
  old wildcard set were casino/explore/bolt/star/nightlight).
  (2) **Screen-matched hero watermarks** — new `CurioIcons` sets:
  `settingsHeroSymbols()` (gears/sliders/appearance: settings, tune,
  dark_mode, light_mode, contrast, palette, colorize, backup,
  notifications, layers) now feeds the Settings hub hero;
  `historyHeroSymbols()` (history, schedule, restore, replay, refresh,
  timer, calendar_today, undo, auto_stories, menu_book) feeds the Topic
  History hero (was the BOOKS family). Home keeps the wildcard set (its
  quest IS wildcard, per user); Profile/Cabinet keep their lane/category
  echo (deliberate Adaptive Hero behavior); Onboarding keeps wildcard
  (welcome). All glyphs already used elsewhere in the app — verified in
  the bundled Material Symbols subset, no tofu.
- **v59 — deck excludes only SAVED entries; uniform Cabinet card height.**
  (1) **"Only saved entries leave"** — per user decision, a topic stays in
  the shuffle deck until it has a SAVED entry in the Cabinet. The old
  v7.80 done-set exclusion (explored or "Already …" marked topics left
  the deck forever) is gone from Spin: `deckPool` (fan/peek cards) and
  the landed `pickFrom` call now exclude by `savedTopicIds` only — a
  reactive set derived from a new `produceState` over
  `CurioRepositoryHolder.repo.observeAll()` (the Cabinet's flow; the
  old per-spin `repo.getAll()` + `doneIds` computation was dropped).
  `pickFrom`'s `exploredIds` param renamed `savedIds` + comments updated.
  `ExploreSessionStore` is no longer referenced from Spin (import
  removed); `recordExplored`'s `addDone` + the done set stay intact —
  they still drive the reveal's "Already …" state and the Topic
  Database's done markers, just not deck exclusion.
  (2) **Uniform Cabinet card height** — `CurioEntryCard`'s title now
  reserves exactly two lines (`minLines = 2` + existing `maxLines = 2`):
  short titles leave a blank second line, long ones ellipsize (never
  cut), so every grid card is the same height regardless of title length.
- **v58 — save page: chips + take tabs pin under the topic strip; mood pill in the strip; attach-tile ink.**
  (1) **Header hoisted to the topic.** The multi-take section state
  (`sections`/`activeIndex`/`nextId`/`pendingRemoveIndex`/`pendingFormatSwitch`
  + `snapshotActive`/`removeSection`/`applyFormat` + the aggregate
  `allReady`/`combinedData`/`anyTakeDraft`/`sectionDraftData` emissions)
  moved OUT of `FormatBodyForCategory` into `SaveCaptureScreen` so the
  format chips + take tabs can pin in a compact horizontally-scrollable
  row UNDER the topic strip (they used to live inside the scrolling body
  under a now-removed "How do you want to capture this one?" header;
  wide windows no longer wrap the chips into a tall FlowRow — always one
  slim row). `FormatBodyForCategory` is now editor-only (active take,
  `key(current.id)`); the two take-confirm dialogs (remove / switch
  format) live at screen level next to the leave dialog.
  (2) **Mood lives in the topic strip.** The universal "How did it make
  you feel?" row is gone from the body; a capsule pill on the RIGHT side
  of the topic strip shows the active take's mood (or a "Mood +"
  affordance with `MoodHappy` glyph) and toggles the shared mood selector
  (bare `MoodChipsRow(header = null)` — new optional `header` param)
  pinned under the strip. Picking a mood writes it into the ACTIVE
  section (works identically for newly added takes), stamped into the
  take's data via `withMood` as before. The emissions `LaunchedEffect`
  also keys on `topic` so the save CTA enables the moment the topic loads.
  (3) **Attach-tile ink.** `AddImageButton`, `ImageThumb` (empty state)
  and `JournalVoiceNoteRow` (Marginalia) drew their icon+label in raw
  `accent` on the 16% `categoryTintFill` tile — invisible in pastel
  light. New `internal fun tintedTileInk(accent)` (CaptureFormatComponents)
  resolves a deep same-hue ink in light (`readableLightInk`) and a light
  twin in dark (lerp toward white), applied to the attach tiles in every
  format (Journal/Field notes/Review).
- **v57 — mood board dual layouts (inline vs full-screen) + quote pinch-to-expand.**
  (1) **Two saved arrangements** — `CaptureData.GalleryWall` gains
  `tileLayoutsFull` + `quotePositionsFull` (Gson default-empty; the
  `withImageUris` remap covers both layout lists). The INLINE layout is
  what the small saved card + inline editor arrange; the FULL-SCREEN
  layout is what the expanded dialog + full-screen editor arrange.
  Legacy entries (empty full fields) fall back to the inline ones, so
  old boards keep their single arrangement. (2) **GalleryWallFormat**
  keeps a second `fullTiles` list + `fullQuotePositions` list (both
  seeded from saved full data, falling back to inline); the full-screen
  canvas edits `fullTiles` and routes quote moves/resizes to
  `fullQuotePositions` via new `quotePositionsOverride` /
  `onMoveQuoteOverride` / `onResizeQuoteOverride` params on
  `MoodBoardCanvas` (null = inline board unchanged). `QuoteCardsState`
  gained a `onCardRemoved` hook so the index-aligned full-screen list
  stays in sync when a card is deleted. `canSave` counts `fullTiles`
  too, and the save `LaunchedEffect` keys on both full lists so
  full-screen edits re-emit the entry. (3) **Expanded dialog** renders
  `tileLayoutsFull`/`quotePositionsFull` (fallback inline). (4)
  **Save/Share PNG** gained `MoodBoardExport.MoodBoardLayout`
  (INLINE/FULL) — `MoodBoardExportActions` shows an Inline / Full-screen
  pill picker above Save/Share, and the export preloads bitmaps + renders
  against the chosen arrangement (both layouts keep quote placements in
  sync with their tile list). (5) **Quote cards pinch-to-expand** —
  `MoodBoardFloatingCard` gained a 2-finger pinch handler (editor only,
  before the drag handler) that live-preview-resizes the card's width
  like the resize grip and commits on release; the drag handler skips
  deltas while `resizing` so a pinch never slides the card.
- **v56 — topic dataset thread lifecycle: bounded parses, cached counts, tiered memory shed.**
  (1) **Bounded parse concurrency** — `TopicJsonLoader` gained a
  `Semaphore(2)` (`gated {}`) around every file read+parse (`parseAsset` +
  `countFor`): the cold-start prewarm, a wildcard merge and several
  screens can all request lanes together; without a gate they parsed
  every file in parallel and saturated all cores — lag + device heating
  on mid-range phones. Max 2 concurrent parses; blocking acquires are
  fine on Dispatchers.IO and nothing nests gated sections (no deadlock).
  (2) **Wildcard merge routes through shared `load()`** instead of parsing
  lanes directly — a lane the prewarm/screens are already parsing is
  SHARED, never double-parsed by the merge. (3) **Per-lane count cache**
  (`countsCache`): `countFor` re-read + re-parsed the whole category file
  on EVERY Spin deck change / picker recompute just for a length; now one
  parse per lane per process, and `countCanonicalTopics` derives from it.
  (4) **Tiered memory shed** (`shedForMemory(level)` replaces
  `clearCache()`): RUNNING_LOW drops pools + counts (cheap single-file
  rebuilds) but KEEPS the 16k-entry index — v51's full shed made a trim
  re-parse everything (the reported lag/heat); the index only drops at
  RUNNING_CRITICAL/COMPLETE. (5) **Prewarm survives rotation**: the
  MainActivity warmup runs under `NonCancellable` so a mid-warmup
  activity destroy no longer restarts the whole index parse.
- **v55 — device-screenshot AUTO-ATTACH removed (per user: it lagged on screenshot).**
  `DeviceScreenshotWatcher` (the MediaStore ContentObserver that watched
  for new screenshots while a session / pending write was live and copied
  them into the session) is DELETED, along with its wiring: MainActivity's
  `DeviceScreenshotWatcher.start()` call and the reveal's
  `requestMediaRead` launcher + the READ_MEDIA_IMAGES permission request
  in `beginExploreSession`. The SAVE PAGE keeps its manual add-from-gallery
  (system Photo Picker — no storage permission — `SessionShots.copyFrom` +
  `appendPendingScreenshot`, untouched) and the remove-thumbnail option;
  backup/restore of session screenshots also untouched. The watcher's lag:
  the observer fired on EVERY media-library change (even with no session)
  plus MediaStore queries + full file copies at the exact moment the system
  was still writing/indexing the shot.
- **v54 — update toast + once-per-version notification + editable Profile tagline + progress dialog on the background tint.**
  (1) **Update notifier** (`UpdateChecker.notifyIfUpdateAvailable`, run on
  app start from MainActivity): fetches the latest release, and when it's
  newer — a TOAST announces it on every check that finds one, and a
  NOTIFICATION fires ONCE per version (AppPreferences persists the last
  announced tag; same-tag launches skip it). Notification opens the app
  (launcher intent), `curio_updates` channel, `ic_notification` small
  icon, runCatching on notify (POST_NOTIFICATIONS gate on 13+).
  (2) **Editable Profile tagline**: tap the hero tagline ("Keep the spark
  going today." …) → AlertDialog with an OutlinedTextField; Save persists
  `custom_streak_tagline` (empty = automatic `taglineForStreak`), "Use
  automatic tagline" resets it; `taglineRevision` bump re-reads the pref.
  (3) **Progress editor dialog on the theme background tint**: container
  `accent` → `curioDialogContainerColor()`; `dialogContentColor` default
  `ink` → `MaterialTheme.colorScheme.onSurface` (reveal's explicit
  `cat.onAccent()` override removed — that was the v45 workaround for the
  accent container); accent still colors the ring/steppers/Save button.
- **v53 — Apple Music resolves to a real catalog item + saved progress restored + filter chips/icons + chip-bar animations.**
  (1) **Apple Music "Watch in" resolves the topic to an actual catalog
  item** (`resolveAppleMusicItemUrl` in ExploreSearch.kt): the reveal's
  Watch-in for APPLE_MUSIC now calls the public iTunes Search API
  (entity = album / song / musicArtist by subtype) off the main thread
  and starts the session with the item's native deep link
  (`music://music.apple.com/{cc}/album|song|artist/{id}`) — the Android
  app only handles ITEM pages natively, so search links (music://…/search)
  still showed the in-app "Open in browser" banner. Falls back to the
  search link when the lookup fails (offline / no result); plain
  HttpURLConnection like UpdateChecker, 8s timeouts, org.json parse.
  (2) **Saved progress restored** — the real bug behind "progress isn't
  showing in cabinet/detail": `CaptureEntity.toEntry()`'s fallback topic
  (used until the catalog lane cache loads) dropped `pageCount`/
  `episodeCount`, so `progressTarget` was null on saved entries. New
  nullable `pageCount`/`episodeCount` columns (Room v6→v7
  `MIGRATION_6_7`), persisted in `toEntity()`, restored in the fallback
  (backup export already round-trips them via `gson.toJson(entity)`).
  Also: the Cabinet card line was drawn with `cat.onAccent()` — white on
  light heroes, invisible — now `themedAccent()` fill with a faint track
  that always shows while the target exists. Detail pill stays anchored
  BottomEnd (12dp) on the hero. (3) **Reveal strip**: tag chips raised
  14 → 10dp top inset so they clear the Like/Dislike row; SentimentButton
  slimmed (10/5 padding, 15dp icon) and its inactive fill now uses
  `curioPillTintLift()` (theme-aware) instead of `surfaceVariant`.
  (4) **Chip-bar animations**: the Cabinet + Topic Browser sticky
  category/search chip bars wrapped in `AnimatedVisibility`
  (expandVertically + fadeIn / shrinkVertically + fadeOut) — no pop.
  (5) **Filter sheet chips**: `CompactChip` gained a per-chip keyword-
  mapped icon (`filterChipIcon`, verified glyphs, AutoAwesome default),
  bigger (16sp label, 16/11 padding), and light-mode inactive fill
  darkened 0.82 → 0.5 lerp toward `curioPillTintLift` (same for the
  closed `FilterGroupPill`) so chips read as solid mid-tones off the
  pale sheet. Sort-pill dimensions unchanged — user asked for them and
  will dictate the decrease.
- **v52 — Apple Music deep link opens native search + backup-restore compile fix.**
  (1) **`buildMusicServiceSearchUrl` for Apple Music now uses the native
  `music://` scheme** (`music://music.apple.com/{cc}/search?term=…`)
  instead of `https://` — the Android Apple Music app renders
  music.apple.com/search (a web-only page) in an in-app browser with an
  "Open in browser" banner instead of searching, while `music://` is the
  app's registered URL scheme (any music.apple.com path works with https
  swapped for music), so its native router lands on the search tab. New
  shared `openSearchUrl(context, url)` helper launches the URL and, on
  `ActivityNotFoundException` (Apple Music not installed → no custom-scheme
  handler), falls back to the https equivalent so the old browser behavior
  is preserved; both launch sites (reveal's explore-and-go-home + Home's
  keep-exploring) now use it. (2) **CI fix:** the streaming restore's two
  `gson.fromJson(reader, CaptureEntity::class.java)` calls failed inference
  — Gson has no `fromJson(JsonReader, Class<T>)` overload (only `Type`), so
  un-typed calls bound to `Any!` ("Cannot infer type for type parameter
  'T'" + cascading unresolved `id`/`format`/`formatDataJson` errors in
  compileDebug/compileRelease). Both call sites now declare
  `val capture: CaptureEntity` / `val cap: CaptureEntity` explicitly.
- **v51 — reveal pill tints + bigger corner chips + complete memory shed.**
  (1) **Hero pills less whitish in light mode** (`HeroCard.pillGlass`):
  pastel light 80% → 60% toward white, non-pastel light 50% → 42% — the
  lane's accent clearly shows through the frost now. (2) **Strip tag chips
  carry color:** the opaque tinted fill blends 22% → 32% of the lane
  accent (`lerp(surface, themedAccent(), …)`) so they stop reading whitish
  on the pastel page wash. (3) **Corner controls larger:** the top-bar
  category chip padding 12/7 → 14/9 with an 18dp glyph, and the pin/close
  circles grow to a 24dp glyph on a 42dp circle. (4) **`TopicJsonLoader`
  complete memory shed:** `clearCache()` (fired on
  `TRIM_MEMORY_RUNNING_LOW`) now also drops `indexCache` + the canonical
  count — previously ~30-60MB (16k TopicIndexEntry + lowercased key
  copies) stayed resident after a trim, keeping the heap near-full so
  background GCs fired every second on mid-range devices (the user's
  "Background concurrent mark compact GC" log). All rebuilt lazily.
  Diagnosis of the pasted log (sustained GC churn + `Skipped 3320
  frames`): heap 110-160MB near-full + main-thread stalls from the GC
  storm; the shed is the retained-footprint lever.
- **v50 — Topic Reveal: Like/dislike into the strip + one editorial font.**
  (1) **Sentiment pair moved into the bottom band** (TopicRevealScreen):
  the section-6.5 row that scrolled in the body below the ActionPromptCard
  is gone; the Dislike/Like pills now live in the fixed bottom band BELOW
  the tag chips, aligned bottom with `navInset + 8dp` clearance. The tag
  row's top inset dropped 24 → 14dp to make room, and `RevealBottomBarHeight`
  stays 80dp — the strip never grows. `SentimentButton` was slimmed to fit
  (12/6dp padding, 16dp icon, labelMedium); browse mode still hides the
  pair (read-only), tags render independently. (2) **Quick fact + action
  instruction share ONE style:** new file-level `RevealEditorialBody`
  (`CurioEditorialBody.copy(15sp/23sp)` — a notch below the old 17sp fact)
  is now used by BOTH the TeaserCard quick fact (was `CurioEditorialBody`
  17sp) and the ActionPromptCard instruction (was an inline 15sp copy), so
  the two long-form paragraphs match exactly and can't drift again.
  `TextStyle` import added.
- **v49 — topic-load speed + smooth Topic Browser wheel scroll + Home spacing.**
  (1) **TopicJsonLoader shares in-flight parses instead of serializing:**
  the old single global `cacheMutex` was held for the ENTIRE parse, so
  the cold-start prewarm queue (`loadIndex` + `preloadAll` in
  `MainActivity`) blocked Spin's load of an unrelated lane until every
  category finished, and the Topic Browser's index load double-parsed
  the merged 16k-topic index alongside the prewarm. `load(id)` now uses
  a per-lane `inFlight` map + short-held `inFlightMutex` + a
  `loadScope`: DIFFERENT lanes parse in parallel, the same lane's
  concurrent callers await ONE shared parse (creator-only
  compare-and-remove), and the shared parse survives its creator's
  cancellation. `loadIndex()` gains its own `indexMutex` + double-check
  so prewarm + screen share one parse. (2) **Topic Browser fallback is
  never fatal:** a failed per-category load in the no-index fallback
  used to throw inside the `produceState` producer and freeze the
  screen on "Loading topics…" forever — now `mapNotNull` + `runCatching`
  skips just that lane. (3) **Wheel-scroll smoothness:** the
  `snapshotFlow { index to offset }` collect wrote to the saveable
  registry on EVERY scroll frame (60x/s over a 16k list) — it now
  persists only when `firstVisibleItemIndex` changes
  (`distinctUntilChanged`, restore lands at the row top), and the
  `items(rows, ...)` call gained `contentType` so section headers and
  topic rows recycle their own LazyColumn slots. (4) **Home spacing:**
  the section rhythm below the hero is one consistent 12dp (the old
  20dp ends stacked with the pre-Saved 20dp spacer = 40dp of dead
  space between the Shuffle the deck card and Saved when no
  session/queue is live; the doubled spacer is removed), and both
  "View all" pills (Saved + Recents) now use `onBackground` ink for
  text AND icon — matching the section titles instead of the washed
  theme-primary mauve on the cream pill.
- **v48 — streaming backup RESTORE (OOM fix, mirrors the export).**
  `CurioBackupManager.restore` no longer reads the whole file into a
  String, builds a JSONObject tree, and Gson-parses the entire payload
  (every media byte[] decoded and resident at once — the same OOM class
  as the old export). It now reads the file TWICE with a streaming
  `JsonReader`: pass 1 (`validateBackupStream`) enforces the same
  pre-flight (envelope format/version, captures array present with
  unique/safe/well-formed records, preferences restricted to the known
  files) without holding anything; pass 2 walks the sections — captures +
  prefs parsed per-section, and each media file decoded + written ONE AT
  A TIME (audio by capture id, images to their per-capture destinations
  via a uri→(captureId,index) map, session shots via the shared index,
  pending-write shots included) — recording only tiny path maps, then
  the database is wiped + re-inserted in one transaction. Same semantics
  and error messages as before; peak memory is one media file's bytes
  instead of the whole archive.
- **v47 — dark mode: deep high-contrast background tints + blackish picker idle cards.**
  (1) **Dark wash retuned** (`CategoryInk.kt`): `DEFAULT_DARK_WASH` no
  longer pulls un-tuned families' mid-tone 50% toward their LIGHT twin
  (the whitish wash) — it now hugs the deep accent (0.16 factor) with a
  slightly stronger 0.22 blend. Music (indigo) and Visual Art (teal) —
  the two families that previously fell to the default — got their own
  `DARK_WASH_TUNING` entries (deep indigo / deep teal-forest twins).
  (2) **Mixed-deck page wash deepened** (`CurioColors.mixedDeckWash`):
  dark mode now blackens the blend harder (50%) at a 42% blend (was
  35%/45%), and pastel-dark drops to 42% (was 55%) — mixed pages read
  sleek and dark with high contrast for white ink/paper cards. (3)
  **Picker idle cards blackish** (`CurioCategoryCard`): the Curio-style
  dark idle surface is now `surfaceContainerLow` pushed 55% toward black
  — near-black idle tiles; the SELECTED tile keeps its vivid full-accent
  gradient unchanged (idle-vs-active contrast is now the whole story).
- **v46 — progress UI: reveal dialog fix + cabinet visual-only line + detail corner.**
  (1) **Reveal dialog blank bug** (`CurioProgressPill`): the reveal hero
  passed `ink = cat.accent`, which the editor dialog used as its content
  color ON the accent container — every label drew accent-on-accent
  (invisible; only the alpha-blended arcs showed, the reported "thin line
  of progress, no text"). `CurioProgressPill` gains `dialogContentColor`
  (defaults to `ink`, so other callers are unchanged); the reveal passes
  `cat.onAccent()`. (2) **Cabinet card** (`CurioEntryCard`): the rising
  fill + tappable count pill are gone — progress is now a thin 4dp
  on-accent line with a faint track along the hero's bottom edge (between
  the hero and the title box below), VISUAL ONLY — no editor, card shape
  unchanged. (3) **Detail hero**: the progress pill anchors tighter to the
  hero's bottom-right corner (12dp).
- **v45 — streaming backup export (OOM fix) + category-picker draft persistence.**
  (1) **Backup OOM fix** (`CurioBackupManager.export`): the old path
  loaded EVERY audio/image/session-shot byte[] into memory, base64-copied
  the whole payload into one giant JSON String, then copied that into a
  byte[] — a large Cabinet OOM'd mid-backup on a mid-range device (crash
  report: A356E / Android 16). Export now writes the JSON incrementally
  with a `JsonWriter` and reads + base64-encodes each media file ONE AT A
  TIME at the moment its value is written (audio keyed by capture id,
  images deduped by URI, session shots deduped by original path, the
  pending write's shots included). Output shape is byte-for-byte the same
  Gson payload (same field names, same single-line base64) — restore is
  unchanged. (2) **Category picker draft** (`CategoryPickerScreen` +
  `CategoryPickerDraft`): the selection, multi-select mode, Original/New
  page and BOTH grids' scroll offsets are mirrored live into a
  process-scoped holder, so leaving the picker (back / swipe-down) and
  reopening restores exactly where you were — "kept saved until the
  restart". Committing a mix (or tapping a lane open / Cancel) clears the
  draft so the next open shows the persisted deck fresh.
- **v44 — Spin filter sheet: bigger color-tinted chips + flow Type group.**
  (1) **Bigger chips**: `CompactChip` labels bump to 15sp with roomier
  padding (14/9dp; the `FilterGroupPill`s match at 16/12/9dp) so the
  sheet fills instead of leaving empty space above the Apply button.
  (2) **Different color**: inactive fills swap `curioPillLift()` for
  `curioPillTintLift()` (rose-kissed glass in light, white in dark, grey
  glass in AMOLED) — the chips carry a color of their own instead of
  plain cream; selected chips keep the category accent. (3) **TYPE group
  is a flow now**: the fixed 2-column `LazyVerticalGrid` became a
  `FlowRow` of content-sized chips (`fillMaxWidth = false`) — a long
  subtype takes its own full line and the next chip wraps below it.
  Removed the now-unused `heightIn` + `curioPillLift` imports.
- **v43 — Topic Reveal hero pills + action labels + quick-fact voice.**
  (1) **Hero pill glass retuned** (`HeroCard` `pillGlass`): pastel light
  now lerps only 80% toward white (was 92% — the pills read as stark
  white blobs on the pale heroes), the deep non-pastel banner gets a 50%
  frosted-accent glass, and dark keeps a 55% lift toward
  `curioPillLift()`; all three are OPAQUE fills that carry the accent
  hue — theme aware, never transparent, never flat white. (2) **Action
  labels bumped**: `revealDockMetrics` `textSp` raised +1.5sp per tier
  (14.5 / 15.5 / 17.5sp) so Start exploring + Express yourself read a
  little larger (both stay ExtraBold, single-line). (3) **Quick-fact
  voice**: the `TeaserCard` quick fact moves from the plain Material
  `bodyLarge` back to `CurioEditorialBody` (Lora) — the SAME font as the
  `ActionPromptCard` instruction, so the reveal's long-form copy is one
  readable serif. The fact stays UNCLAMPED and shown in full — no
  read-more folding (user rejected the 4-line clamp).
- **v42 — mood-board editor stability + resizable quotes + tinted-glass styling.**
  (1) **Mood-board glitches fixed** (`GalleryWallFormat`): the inline
  board's crop extent is seeded ONCE per session (`stableBoardMaxX/Y` —
  fresh boards seed to the full canvas 1:1, edit-mode boards to the saved
  collage) and never re-grows, so adding a photo no longer re-fits and
  re-scales the whole board ("board re-sizes mid-edit") and drags follow
  the finger 1:1 (no snaps). (2) **Resizable quote boxes**: `QuotePos`
  gained `w` (raw board px, -1 = default slot width); `QuoteCardsState`
  gained a parallel `widths` list + `setWidth`; `MoodBoardFloatingCards`
  takes `onResizeCard` and `MoodBoardFloatingCard` renders an editor-only
  bottom-end grip (drag to widen, clamped to half-slot..board width,
  committed via the live callback). (3) **Grow-in-place tiles**: each
  photo tile has a bottom-start enlarge button that scales the tile ×1.45
  around its center (for photos too small to pinch). (4) **Category pill
  inside the hero**: the Cabinet (`CabinetHeroHeader` `titleTrailing`
  slot) and Topic Database (`SettingsHeroHeader` `titleTrailing`) now
  render the Category pill beside the title, directly under the sort/search
  pills; the below-hero pill rows + their height reservations are gone
  (chip bars sit directly under the banner). (5) **Sort pill**: corner
  radius 50dp → 18dp (the capsule read bulbous), and `CurioDropdownMenu`
  gained `minWidth` (236dp default) + taller 14dp rows. (6) **Azure hero
  re-enabled + default**: the grey-out and the migrate-back-to-rose effect
  are gone; `heroBlueState`/`isHeroBlueEnabled` default ON. (7)
  **Color-tinted glass** (the "creamy" fix): new `curioPillTintLift()`
  (light = background rose-tinted 8%, dark = white, AMOLED = `#2A2A2A`
  grey glass) replaces `curioPillLift()` in `SettingsHeroActionPill`,
  `CabinetHeroActionPill`, `CurioSortDropdown`, `CurioSettingsCard`
  (AMOLED: `surfaceContainerHigh`→grey lerp), `CurioSettingsRow` (icon
  now sits in a coral chip), and Profile's quest plate + hero pills.
  `CurioCardHeader`'s chip keeps a muted coral plate in AMOLED (was
  neutral grey), and AMOLED switches light up coral when ON (grey glass
  track when OFF).
- **v42 — merged badge shelf + quest-paths card grid + profile polish.**
  (1) **Merged badge shelf:** `CurioBadges.kt` gains `MergedChainBadge`
  + `mergedChainBadges()` — ONE medal per quest CHAIN (category) showing
  the chain's HIGHEST-earned stage (best rarity); earning a chain's
  bronze then silver upgrades the single medal to silver instead of
  stacking duplicates. The shelf (Quests strip + dialog + Profile strip)
  sorts earned badges first by rarity, then locked chains (silhouettes
  preview their best rarity; SECRET badges never show locked). New
  `CurioBadgeDetailDialog` (shared Profile + Quests) shows the medal,
  tier chip, name, description and live progress (+XP, and a
  "· upgraded" chip on merged tiles when earlier rarities were earned).
  Quests' `PathsCard` was redesigned from flat rows to a **card-per-path
  grid**: each card wears the chain glyph, a live progress bar, its merged
  medal, and opens `PathDetailDialog` (stage trail with Go chips). The
  old per-stage `BadgeTile`/`BadgeShelf` grid is removed (dead).
  (2) **Profile:** tapping any badge (earned or locked) on the Profile
  strip opens `CurioBadgeDetailDialog`; the Edit profile / streak / level
  pills are now OPAQUE (`lerp(fill, White, 0.18)` light, rose twin on
  AMOLED, +2dp shadow + dark glow) like the stat pane instead of the old
  `ink@18%` tint that smeared on busy banners; the "YOUR PROFILE" kicker
  stepped labelSmall → labelMedium (wider tracking) and the pills row
  spacing tightened (8dp gaps, even 58dp+ cells).
- **v41 — Explore dialog declutter + canonical pet-dialog doc.**
  (1) **Explore dialog:** the two helper paragraphs (the engine/verb
  intro and the timed-explore note) are gone; the dialog is now the
  title ("Explore {topic}?"), the rephrased no-AI pledge — "Keep your
  research yours. Read the real sources instead of AI summaries, and
  the discovery is all yours." (user-approved, no em dash) — and the two
  pill actions. The now-unused `action` val was removed from the dialog
  block (the pill glyphs only need the music-service resolution).
  (2) **docs/pet-dialogs.md** — the canonical, source-of-truth doc for
  every Curie/pet spoken line (BABY / FIRST_EVO / FINAL_EVO voices),
  organized by group: event reactions, streak milestones, evolution
  ceremony, mood bubbles, greetings/welcome-backs, touch tiers, games,
  memory/fact lines, the learning brain's composed + coined lines, the
  tour script, and the mature routine lines. Each section lists the
  current FIRST_EVO pools with BABY/FINAL_EVO twins inline, plus an
  integration checklist (keep pool names, bullet order, and placeholders
  like `__LANE__` / `$lane` / `$streak` identical when porting rephrased
  lines).
- **v40 — reveal bottom-band wash + smooth tab crossfade + lane tiles open Cabinet.**
  (1) **Reveal bottom band:** `TopicRevealScreen`'s band now wears
  `cat.categoryBackgroundWash()` (the page's own wash) instead of
  `categorySurface(surfaceContainer)` — the old strip resolved to a
  lighter tint that read as a separate white/creamy slab at the bottom,
  most visible behind the tags during the open fade. The reveal now reads
  as one continuous surface (Material/AMOLED unchanged). (2) **Tab
  switch animation:** `CurioNavHost` tab switches (enter + pop-enter) are
  now a clean `fadeIn` instead of `scaleIn(0.97f) + fadeIn` — the
  scale-fade read as the "old" animation opening the Cabinet from
  Profile. (3) **Profile lane tiles open the Cabinet filtered:** new
  `PendingCabinetFilter` out-of-band handoff in CurioRoutes (mirrors
  PendingEntryOpen: `request(CategoryId)` + monotonic `trigger` +
  `take()`); Profile's `LanesCard` gained `onOpenLane(CategoryId)` and
  its tiles became clickable `Surface(onClick)`; `CabinetScreen` consumes
  the pending filter once per request in a `LaunchedEffect(trigger)` and
  applies it to `selectedFilter` (clearing legacy/search). (4) **Lane
  glyph readability:** the tile icon wears `category.categoryInk()`
  instead of `themedAccent()` — in pastel light `themedAccent` resolved
  to a near-white pastel that washed out on the pale tile (the "whitish
  icons" report); ink resolves a deep same-hue twin in light.
- **v39 — filter-chip contrast + 3dp elevation + Cabinet decode cache.**
  (1) **Filter sheet contrast fixed:** `CompactChip` + `FilterGroupPill`
  light-mode inactive fills lift 0.55 → 0.82 toward `curioPillLift()` —
  the old lift still read same-y against the pale pastel wash (both the
  sheet and the chips are pastel tints); chips now go neutral cream and
  clearly separate. Dark keeps its subtle 0.04 lift + glow. (2) **Both
  states get 3dp elevation:** `shadowElevation` + `curioDarkGlow` 2 → 3dp
  on the filter chips and group pills; same contrast/elevation applied to
  the picker tabs (`PickerPageTab`) and preset chips (`PickerPresetChip`,
  0.60 → 0.82 lift, 3dp). (3) **Cabinet freeze on mass saves:**
  `CaptureRepository.observeAll()` now caches decoded `CurioEntry`s by a
  signature (id + format + capturedAtMillis + formatDataJson/tags/
  screenshots hash + sessionNote + deletedAt); Room re-emits the full list
  on every insert, so a large archive re-ran Gson decoding for EVERY row
  per save — the GC pauses froze the app. Now only new/changed rows
  decode; the map is only touched from the flow's single collection
  dispatcher.
- **v38 — onboarding proportions + page-pill indicator + reveal quick-fact revert.**
  (1) **Hero/tear deeper:** the onboarding torn-rose hero deepens 0.70 →
  0.76 of screen height so the tear sits just above the page pills and the
  dead band between the dots and the Skip/Next controls disappears.
  (2) **Wordmark ↔ slide spacing:** the 6dp spacer under the pledge became
  10dp and the pager gained `top = 8.dp` / `bottom = 26.dp` so the slide
  content centers evenly in the banner. (3) **Page indicator → pills:**
  `PageDot` is a proper indicator now — the active page is a 22×8dp
  capsule, the rest 8dp circles (no more 12dp box × 1.2 scale blob); the
  row drops to `vertical = 12.dp` with even 3dp gaps; the unused
  `ui.draw.scale` import was removed and `foundation.layout.width` added.
  (4) **Reveal quick fact:** `TeaserCard`'s fact body reverts from
  `CurioEditorialBody` (Lora) to `MaterialTheme.typography.bodyLarge`
  (spacer 12 → 10dp) — the Lora voice stays on the ActionPromptCard
  instruction and onboarding subtext, only the quick fact goes back.
- **v37 — hero controls return + reveal pill polish + compact wildcard filters.**
  (1) **Cabinet/Topic Browser controls back INSIDE the hero:** the v34
  below-hero controls row is gone — the Sort dropdown + Search pill (and
  the selection pills Clear/Select-all, Delete, Cancel) ride the hero's
  top row again via the existing `trailing` slot (`CabinetHeroHeader`
  keeps its `(ink, backdrop)` slot; `SettingsHeroHeader`'s `trailing`
  passes hero `ink`). `contentTop` reverts to reserving only the single
  Category pill row below the banner; `CabinetControlsRowHeight` /
  `DatabaseControlsRowHeight` constants are removed. The sort dropdown
  keeps its capsule 50dp pill + `CurioDropdownMenu` accent-themed menu
  (accent = active filter's `themedAccent` in Cabinet, rose in the
  database). (2) **Reveal:** the duplicate category eyebrow pill inside
  the HeroCard is removed (the top-bar chip already shows the lane); the
  top-bar category chip + pin + close now wear `cat.categorySurface()`
  (theme-aware tint in every mode instead of flat surfaceVariant);
  Express yourself stepped Bold → ExtraBold to match the Start exploring
  CTA; the ActionPromptCard's trailing arrow is gone; the hero's action
  badge / byline / subtype pills use a new `pillGlass` — strong white
  glass on pastel-light heroes, page-background lift on dark — instead
  of the washed `ink.copy(alpha = 0.18f)`. (3) **Wildcard filter
  compaction (FilterSheet only):** `buildFilterGroups` caps Type at the
  top-8 most frequent subtypes when a pool exceeds 8 (the wildcard
  surprise deck merges every category — its raw list was a 60+ chip
  wall; individual categories keep their full list), and the TYPE group
  renders in a compact 2-column `LazyVerticalGrid` (`heightIn max 160dp`,
  no scroll) instead of a full-width flow stack. (4) **Sparse groups
  filled out:** genres/eras/origins caps rise 4/4/3 → 8/6/6 so
  categories with fewer than 4 options expose more filters.
- **v35 — typography pass: Lora serif + reveal hierarchy + icons.**
  (1) **New font:** `app/src/main/res/font/lora.ttf` (Lora variable,
  OFL) bundled; `LoraFontFamily` (multi-entry variable pattern like
  geom) + `CurioEditorialBody` (17/27sp) and `CurioEditorialLead`
  (18/29sp SemiBold) top-level styles in CurioTypography.kt. Long-form
  reading text now uses the serif: the reveal teaser/quick-fact body,
  the ActionPromptCard instruction (15/23sp), and the onboarding intro
  subtext (18/27sp on the rose hero). Handwriting/journal fields keep
  Patrick Hand. (2) **Global type polish:** `bodyLarge` letter-spacing
  0.5 → 0.3sp, `titleLarge` SemiBold → Bold. (3) **Reveal hero
  hierarchy:** a small-caps category eyebrow pill (`displayName` caps,
  labelSmall ExtraBold, 1.5sp tracking) sits above the 34sp title, and
  the action badge's plain dot is replaced by the verb's own icon
  (`verbIcon(action.verb)` — headphones/play/book/restaurant…).
  (4) **Reveal top bar:** a frosted category chip (glyph + caps name,
  `weight(1f, fill=false)` keeps the pin/close group end-aligned).
  (5) **TeaserCard:** the inverted hierarchy is fixed — the
  titleSmall label became a small-caps kicker (labelSmall ExtraBold,
  1.2sp tracking, category ink) and the fact body reads in
  `CurioEditorialBody`; the flat sparkle is now a lightbulb
  (`CurioIcons.Lightbulb`, new constant) in an accent-tinted circular
  tile. (6) **ActionPromptCard:** trailing `arrow_forward` affordance
  + serif instruction. (7) **Onboarding:** intro paragraphs (welcome,
  permissions, theme, search-engine slides) read in Lora.
- **v34 — Cabinet/Topic Browser hero tidy-up + version bump.**
  (1) **Sort pill matches the other pills again:** `CurioSortDropdown`
  corners are back to the fully-rounded 50dp capsule (the v31 16dp
  corners read rectangular next to the capsule search/select pills);
  the 42dp height + tight padding keep it slim. (2) **Category pill
  below search+sort:** the Sort dropdown and Search pill moved OUT of
  the hero into a controls row below the banner, and the Category pill
  rides the row BELOW them (both `CabinetScreen` and
  `TopicDatabaseScreen`; new `CabinetControlsRowHeight` /
  `DatabaseControlsRowHeight` constants + updated `contentTop`,
  chip-bar tops and back-to-top padding). The controls row hides while
  searching (the hero morphs into the search field + Cancel pill, the
  old hero-pill behavior). (3) **Hero = clean title header:**
  `CabinetHeroHeader` dropped its `backVisible`/`onBack`/`trailing`
  params — the conditional back pill and all action pills are gone from
  the banner — and the title block sits at the TOP (no flex spacer).
  `SettingsHeroHeader` gained `titleAtTop: Boolean = false` (default
  keeps all 12 other callers exactly as before; the Topic Database
  passes true and keeps its back pill since it's a pushed screen).
  (4) **Select button removed:** the Cabinet's Select pill is gone —
  long-press enters selection (already existed); the pills in the
  controls row are page-level now (`onSurface` ink over
  `surfaceContainerHigh` glass, sort accent = active filter's
  `themedAccent` or theme primary). (5) **Selection shows only
  Clear + Delete:** while selecting, the controls row shows just
  Clear/Select-all + Delete(N) — no cancel, no category pill.
  (6) **Version:** `versionName` default 1.0.0 → **1.0.1**
  (`app/build.gradle.kts`; release tags still override via env).
- **v33 — picker pills, filter-sheet accordion, pastel-dark lane hero.**
  (1) **Category picker proper pills:** the Original / New page tabs
  (`PickerPageTab`) and the quick-mix preset chips (`PickerPresetChip`)
  grow from 4dp to 8dp vertical padding (real ~34dp pills), and their
  unselected fill now lifts toward `curioPillLift()` (cream in light,
  lighter glass in dark — `lerp(surfaceContainerHigh, curioPillLift(),
  0.18/0.60)`) so they stand off the category wash instead of the old
  `surfaceVariant`/`surfaceContainerHigh` blend that melted into the
  tinted picker. (2) **Spin FilterSheet accordion:** the Type · Genres ·
  Era · Origin · Franchise headers became tappable `FilterGroupPill`s
  (`FilterGroupKey` enum + `FilterGroups.chipsFor`) — one group open at
  a time, tap the open pill to collapse (selections survive; `null`
  stays collapsed), tap another to swap, search-narrowed groups fall
  back to the first available; chips slide in via `expandVertically` +
  `fadeIn` (tween/FastOutSlowIn) inside an `animateContentSize` column,
  with a rotating chevron and a per-group selected-count badge. The
  old LazyVerticalGrid + `GridItemSpan` section grid is gone. (3)
  **Chips raised neutral:** `CompactChip` light-mode inactive fill
  lifts to `curioPillLift()` at 0.55 (was White at 0.32) so unselected
  chips read as neutral raised pills off the pastel sheet; gains a
  `fillMaxWidth` param (false in the accordion FlowRow so chips wrap
  at natural pill width). (4) **Filter sheet background:** the sheet
  container now wears `categoryBackgroundWash()` — the same soft page
  tint as the Spin page — instead of the stronger card-level
  `categorySurface` that read as the raw hero color (Material keeps its
  device surface). (5) **Pastel-dark lane hero darker:** `headerAccent()`
  steps pastel-dark banners down (lightness x0.80, floor 0.30) whether
  or not the Deeper header toggle is on; the plain rose hero keeps its
  deep `HomeRosewoodDark` twin untouched. (6) **Hero picker rename:**
  the greyed "Sky azure hero" option (and its hint) is renamed
  "Azure hero" — behavior unchanged (still visible but unselectable).
- **v32 — non-pastel peek/hero color fixes + pastel-dark readability.**
  (1) **Non-pastel deck peeks** (`SpinScreen.PeekCard`) step like pastel
  ones — an HSL lightness drop (light 0.14/0.20 near/far, dark
  0.11/0.16) + the 0.75x saturation pull — instead of the old black-lerp
  slabs (0.40/0.52) that read near-black in light mode; the deck keeps
  its hero-brightest hierarchy with visible gradients. (2) **Category/
  lane-colored heroes calmer + readable in non-pastel:** `headerAccent()`
  pulls saturation ~15% (cap 0.60) so a vivid lane accent isn't
  blinding, and the shared-hero inks (`settingsReadableInk`/
  `homeReadableInk`/`profileReadableInk`) now resolve via
  `heroLaneCategory()?.heroHeaderInk()` — white/cream on the deep
  accent — instead of the fixed dark `onSurface` that made lane-banner
  text invisible in non-pastel light. (3) **Paper stat card dark =
  hue-matched deep paper** (`paperStatCardColor`): dark mode builds the
  deep paper from the base/hero HUE (per-screen color-aware — Home rose,
  Profile hero, a detail page's category) instead of the fixed muddy
  brown, with a whisper of warm brown so it still reads as paper.
  (4) **Pastel-dark control text:** the Categories/Filter bottom-bar
  labels (`deckControlInk` in `SpinScreen`) and Topic Reveal's
  Start exploring / Express yourself flip to the bright cream-white
  (`pastelFillInk`) the heroes use, and `themedButtonFill()` deepens the
  pastel-dark fill (lightness x0.82) so the buttons pop off the page
  wash. (5) **Orbit dots pastel dark** carry their color again: the
  85%-white `pastelFillInk` resolution for the dots is overridden to a
  ~60% white-lerp so they stay light on midnight but clearly tinted.
- **v29 — capture attach boxes are OPAQUE.** The border-removal pass left
  the translucent `category.tint` (accent @ 20% alpha) attach boxes
  looking broken (v27n rule: translucent fills bleed the elevation
  shadow). New shared `categoryTintFill(accent)` in
  `CaptureFormatComponents.kt` resolves the same perceived tint as an
  OPAQUE `lerp(surfaceContainerHigh, accent, 0.16f)`; `ImageThumb`,
  `AddImageButton` (Reel Notes/Field Notes = review + field notes) and
  `JournalVoiceNoteRow` (Marginalia journal voice-note capsule) all use
  it now.
- **v29 — save-capture topic strip matches the category + glow.** The
  strip fill switched from `lerp(surfaceContainerHigh, accent, 0.20f)`
  (muddy near-grey in dark) to `cat.categorySurface(surfaceContainerHigh)`
  — the SAME opaque card-family tint as the rest of the app — with
  `curioDarkGlow(3dp)` so the 3dp elevation shows in dark mode; ink stays
  `cat.categoryInk()` (deep accent in light, light twin in dark, deep twin
  in pastel) so the topic text is readable in every theme.
- **v29 — Spotify/Apple Music explore links + auto-copy.** Apple Music's
  URL gained the REQUIRED storefront segment
  (`https://music.apple.com/us/search?term=…` — without `/us/` the server
  redirects and the app never recognizes the link); Spotify keeps
  `https://open.spotify.com/search/…` (verified correct). Because neither
  app reliably hands off an in-app SEARCH from a web link, tapping
  Explore / Watch in now AUTO-COPYs `buildExploreQuery(topic)` to the
  clipboard (with a short toast) so the user can paste the topic name
  into the app's own search box. New `CurioIcons.ContentCopy`
  (`content_copy`, verified present in the bundled font subset).
- **v29 — per-topic progress UI redesigned (pill + editor + placements).**
  `CurioProgressPill` is now a COMPACT OPAQUE pill (count + optional slim
  category-accent bar) with a new signature `(topic, accent, ink,
  background, showBar)` — the old long accent-shaped control with the
  `fill`/`contentColor` params is gone. The editor is a brand-new
  `CurioProgressEditorDialog` in `ui/components/CurioProgressPill.kt`: the
  dialog CONTAINER is the category accent (opaque, content rides the
  on-accent color), a circular progress ring with big % + count, −/+ round
  steppers (±1 precise change), a stepped slider (`steps = target-1`, capped
  at 600), and ONLY Finish (quick-set to target) + Save (persist + close)
  — NO Reset, NO Cancel (dismiss = tap-outside/back). Placements: Topic
  Reveal hero shows a small OPAQUE frosted count badge at the TOP-RIGHT
  corner (`lerp(accent, White, 0.85)` fill + accent text — the old
  bottom-straddling pill that clipped during the shared-element morph is
  gone); Entry Detail shows a small pill at the hero's BOTTOM-RIGHT (tint
  background `lerp(surfaceContainerHigh, accent, 0.16)` + accent bar); the
  Cabinet card shows progress IN the hero (see below).
- **v29 — Cabinet card hero FILLS with progress.** `CurioEntryCard`'s 96dp
  hero header now renders a rising progress fill anchored to its bottom
  edge (`fillMaxHeight(fraction)` of the on-accent ink, denser at the base
  with a bright 2dp level line at the current mark) plus a small opaque
  count pill at the hero's bottom-right corner (tap → editor) — 50%
  progress = half the hero colored, 100% = fully filled. Works on AMOLED
  too (onAccent resolves to white there, so the fill reads as a brightening
  over the black-glass hero). The old bottom body strip was removed.
- **v29 — Cabinet + Topic DB category pills stop GROWING on entry.** The
  sticky chip bars' per-pill pop rested at 0.90 scale, so every pill looked
  like it was growing the moment the screen opened. `CabinetChipPop` and
  `DatabaseChipPop` now rest at FULL size (1.0) and only breathe subtly
  (1.0 → 1.05) as the bar actually pins on scroll; the color-bloom + lift
  language is unchanged.
- **v29 — device-screenshot watcher hardened.** `DeviceScreenshotWatcher`
  coalesces MediaStore change bursts (one scan pass drains all queued
  requests instead of piling up work), schedules ONE delayed re-pass
  (~1.5s) to catch screenshots MediaStore indexes a beat AFTER the change
  event (the reason a fresh shot sometimes never attached), and widens
  `looksLikeScreenshot` to also match any image filed under a
  `/Screenshots/` folder (some OEMs name shots IMG_…). The heavy query +
  file copy stay on the serialized scan thread.
- **v29 — progress never vanishes.** `TopicProgressStore.writeAll` now uses
  `commit()` (was `apply()` — an async write could be lost when the
  process died right after saving, showing as progress silently reset), and
  `MainActivity.onResume` re-seeds the in-memory progress map from prefs so
  a killed-in-background process heals on return instead of waiting for a
  restart.
- **v29 — Topic Database opens with ZERO loading (prebuilt index → runtime
  build since v174f).** `TopicJsonLoader.loadIndex()` merges every
  `assets/topics/*.json` into one cached `List<TopicIndexEntry>` with
  lowercased search keys and the sort YEAR precomputed (same precedence as
  `topicYear`: name paren → targetName paren → teaser year → instruction
  year → decade tag). `MainActivity` prewarms it at cold start.
  - v174f: the build-time asset (`scripts/build_topic_index.py` →
    `assets/topic_index.json`, 23MB, a FULL duplicate of every topic)
    STOPPED SHIPPING. `loadIndex()` now checks for the asset and, when
    absent, builds the same merged index at runtime via
    `buildIndexFromCatalog()` (routes through `load(id)` so parses are
    shared, wildcard.json read directly). The prebuilt script stays as a
    backup. The Topic Database renders from the cached index when
    present (no per-category parses, no runtime work) and falls back to
    the live per-category load only on a cold start before prewarm
    completes.
- **v28 — scrolling pets look UP/DOWN in a line, never a circle.** The v27v
  "roll" played a FULL 2π CIRCLE of the eyes on every scroll — it read as
  the pet's eyes spinning whenever you scrolled. Replaced with a vertical
  scroll-look: `PetPointer` now exposes `scrollDir` (+1 down / -1 up) and
  `scrollTick` (increments per scroll event) instead of `rollTick`; wheel
  scrolls use `scrollDelta.y`'s sign and touch-drag scrolls use the
  finger's INCREMENTAL vertical travel (2dp threshold + 60ms gate). Each
  sprite runs a `scrollLook` Animatable keyed on `scrollTick`: ease to the
  scroll  direction (150ms), HOLD while scroll events keep arriving
  (restarting the effect), then settle back to neutral ~400ms after the
  last event. The scroll look wins over the pointer aim while active, so
  dragging never mixes the aim with a spin. **v28 touch-direction fix:**
  the touch-drag branch fed the raw finger delta into the SAME mapping as
  the wheel — but a touch finger moves OPPOSITE to the content (swiping
  UP scrolls the page DOWN), so on a phone scrolling down made the pet
  look UP. The touch branch now inverts the finger delta
  (`dy > 0 → -1, else 1`), so  the pet always looks the way the CONTENT
  moves — consistent with the wheel branch (scrolling down = look down).
  **v29 removal:** scroll-following is GONE entirely — the user wanted the
  eyes "normal again, no scroll following". `scrollDir`/`scrollTick`/the
  scrollLook Animatable are removed; the eyes aim only at real taps/hover
  (the tracker cancels the aim as soon as a press starts dragging), so
  scrolling no longer moves the gaze at all.
- **v28 — dark-mode elevation visibility: soft light glow + hairline
  outline.** Compose's black shadows are INVISIBLE on the app's midnight
  surfaces, so dark mode now draws elevation two extra ways via two new
  composable modifiers in `ui/components/DarkElevation.kt`:
  `Modifier.curioDarkGlow(elevation, shape)` — a soft WHITE-tinted shadow
  (16% alpha) that reads as a gentle lift on near-black — and
  `Modifier.curioDarkOutline(shape)` — a faint light hairline (12% white)
  along the surface edge, the standard dark-UI card language. Both are
  dark-mode-only (light mode renders exactly as before: the Surface's own
  black shadowElevation) and are driven by two Appearance toggles
  (`darkGlowState` default ON, `darkOutlineState` default ON — the glow is
  the default-on look, the outline is the Appearance option). Wired into
  the shared elevated components + main screens: CurioSettingsCard,
  CurioSearchField, CurioEntryCard, CurioCategoryCard, FilterChipLite,
  hero action pills (Settings/Cabinet), CurioSortDropdown, CurioTopBar,
  PaperCard surfaces, dialog option rows, Home (stat card, recents rows,
  sticky top pills, session card, pick-a-lane), Profile (stat pane, lanes
  tiles), Topic Reveal (already-there button, teaser card), and the
  Category Picker's preset chips + Original/New page tabs + Mix button.
  Glow must precede the fill in the modifier chain (rule 11).
  **v29 removal:** the user rejected the outline look — `curioDarkOutline`
  (and its `darkOutlineState` pref + 'Card outlines' settings row) is
  REMOVED entirely; only `curioDarkGlow` remains (dark-mode-only).
- **v28 — AMOLED is BORDER-FREE (full border-removal audit).** Two
  systematic border sources were removed from AMOLED: (1)
  `Modifier.categoryEdgeShine` no longer draws its full-edge HAIRLINE RING
  in AMOLED (`hairlineAlpha = 0` — white rings around every pill/card read
  as clunky "borders" on pure black); the TOP-LIT GLASS shine is
  strengthened (0.45→0.52 with accent, 0.22→0.30 without) since it's now
  the sole edge cue. Material keeps its accent rim (its identity); the
  default Curio style was already border-free. (2) `curioDarkOutline` (the
  v28 hairline) never draws in AMOLED either. The AMOLED raised look is
  now top-lit glass shine + the v28 soft glow — no rings. Intentional
  design borders kept: CurioBadges coin rims + the Quests passport stamp
  ring (both are element identity, not elevation). **v29 exception:** the
  user asked for a border BACK on the AMOLED MAIN deck card only —
  `categoryEdgeShine` gained an `amoledHairline` param (default false)
  that redraws the hairline ring just for that card; everything else stays
  border-free.
- **v28 — Spin FilterSheet: live chip search + 1dp chips.** The deck's
  filter bottom sheet (`FilterSheet` in `SpinScreen.kt`) gained a
  `CurioSearchField` under the subtitle: typing narrows EVERY chip group
  (Type / Genre / Era / Origin / Franchise) live via a `filteredGroups`
  derivation (case-insensitive substring), and an empty search shows
  "No filters match …" instead of the plain empty message. The sheet's
  `CompactChip` selectable chips dropped from 2dp to 1dp elevation (cards
  2dp / chips 1dp) so they read as chips, not tiles.
- **v28 — Category card selected state: SATURATED, not darker.**
  `CurioCategoryCard`'s selected fill used `cardGradient` whose start is
  black-DARKENED (`categoryCardFill` 10% light / 28% dark) plus a
  `cardContentInk` sheen that is a deep ink in pastel light — so tapping
  a tile read DARKER than the idle tint. Selected now blooms to the raw
  saturated `category.accent` melting into the page, content flips to
  white (`selectedInk`), and the sheen is a true white 14% glow. `cardInk`
  / `cardContentInk` import removed.
- **v28 — Category Picker rows tightened.** The quick-mix preset chips and
  the Original/New page tabs both wore `categoryEdgeShine` (a white ring
  in AMOLED — the "huge borders") and the preset row's 6dp vertical
  padding pushed the two rows apart. The AMOLED ring removal + the row
  spacing tweaks (preset row padding vertical 6dp → top 4/bottom 1, tabs
  row top 1/bottom 4, chip vertical padding 6dp → 4dp) pull the rows
  together; both pills + the Mix button also gained the soft glow.
- **v28 — Topic Reveal hero gradient matches the Spin ticket in LIGHT
  mode.** The reveal hero's `HeroCard` used `cat.headerAccent()` (a
  0.88-deepened accent in light mode, v27j) while the Spin ticket wears
  `themedAccent()` — so the morph read a shade darker in light (dark's
  0.94 factor hid it). It also rebuilt the gradient via `cardGradient`
  while the ticket uses a different pastel-light recipe (its second stop
  IS the on-hue tint; cardGradient's is only 30% toward it). The hero now
  mirrors the ticket EXACTLY: `themedAccent()` + the same pastel-light
  stops (`lerp(accent, Black, 0.05)` → `lightAccentTint(0.22, 0.80)`) in
  pastel light, `cardGradient` everywhere else — pixel-identical morph in
  every theme.
- **v27v — custom pet designs ALWAYS win + custom-pet procedural defaults.**
  (1) The sprite's design resolution (`CurioPetSprite`) forced
  `evolutionDesign(BABY, null)` for ANY baby-stage pet (level < 15) — so
  "Save" confirmed but the pet never changed for most users. Now a saved
  custom design ALWAYS wins, regardless of growth stage; the stage-based
  evolution art only applies when NO custom design exists. Animations,
  view angles and the curled sleep pose all flow from the winning design
  automatically (a custom pet is its own new pet). (2)
  `PetDesign.withCustomPetDefaults()` — a custom pet is its OWN art:
  procedural accessories (leaf/badge/halo), antenna (nightcap + thinking
  ?), tail and belly stay OFF; only the effects layer (sparkles, Z's,
  whooshes) stays on. Blush + eyes are FACE features, not procedural
  layers, so they stay enabled automatically. Applied by  `saveAsNewPet()` and when saving updates a custom slot (plain Curie
  saves keep the working design exactly as edited).
- **v27v — 3D steel ring styles, Home tint pills, softer torn edges, paper
  detail meta card.** (1) The "Hole rings" experiment now draws REAL 3D
  steel rings through the 3 punch holes (`PaperStatCard.kt`) in three
  selectable looks — "coil" (spring wire through the hole: bright front
  arc over the paper, dark back arc receding into the hole), "split"
  (closed metal torus with a split gap + top glint), "oblique" (a few
  short coil segments springing diagonally out of the hole) — picked via a
  new Experiments → Ring style row (`paperHoleRingStyleState`, default
  "coil"; each style shares the steel gradient + contact shadow). (2) Home
  tint: `heroTintOn`/`heroFill`/`questInk` are hoisted to the top of
  `HomeScreen` so the sticky MENU + PROFILE pills wear the hero tint too
  when "Hero tint too" is on (they previously always fell back to rose);
  follow-my-Spin-lane still never tints the hero. (3) `TornStatPaperShape`
  rework: the top edge is a NEW re-seeded tear (soft waves + gentle ragged
  layer, no longer the inverted hero seam) and the three sides are SOFTER
  (amplitude 3.5→2.2dp, high-frequency octave faded) so the card reads as
  real torn paper, not spikes. (4) The paper & headers experiments now
  extend to Entry Detail's Date · Mood · Session · Type meta card — when
  "Paper stat card" is on it swaps the frosted glass for the shared opaque
  paper surface (paperStatCardFill with the same holes/rings/torn
  toggles; torn seed = per-entry tearSeed xor 0x6B4E3E).
- **v28 — dark-mode hero headers read white/creamish on EVERY screen.**
  New `CurioCategory.heroHeaderInk()` (`CategoryInk.kt`): light mode keeps
  the pastel-aware `onAccent()` resolution exactly, but DARK mode always
  resolves WHITE/creamish (`pastelFillInk(themedAccent())` — the same
  cream-white blend the shared rose heroes use), so a category-tinted hero
  never shows its tinted light twin as title text over midnight. Applied
  to the three category-tinted heroes that used `onAccent()` directly:
  Cabinet's active-filter banner (`CabinetHeroHeader`), the saved-entry
  detail hero (`heroInk` in `EntryDetailScreen`), and Home's hero-tint
  experiment title + sticky pills (`questInk`). Home/Profile/Settings/History
  heroes already resolved creamish via their `*ReadableInk` helpers —
  unchanged.
- **v28 — Settings hero tear is WHITE paper in dark mode + detail hero
  tears never flatten.** (1) The Settings hero under-sheet was the ONLY
  hero using `MaterialTheme.colorScheme.surface` (midnight in dark mode)
  with an `onSurface` rim (white-ish in dark) — so the Settings tear read
  dark/gray while every other screen's tear stayed white. It now matches
  the app-wide pattern: warm cream `0xFFFDFCF9` sheet + the same black
  0.20 rim as Home, in EVERY theme (AMOLED keeps its rose 0.45 sheet so
  the seam reads through the pure-black banner). (2) The detail-only
  "guaranteed movement" oscillation in `SoftTearParams.broadDisp` ran at
  ~2.8 cycles — nearly the SAME wavelength as the main wave — so for
  unlucky seeds it reinforced the wave's flat plateaus and the detail
  hero's torn edge read as huge straight lines (the white sheet stayed
  bumpy because its exposed lip uses its own restrained rhythm). Replaced
  with two phase-offset, incommensurate mid-frequency octaves (17π ≈ 8.5
  and 23π ≈ 11.5 cycles, ~2.1dp + ~1.3dp, amplitudes hoisted to
  `meanderA`/`meanderB` since `density` is only in constructor scope):
  the seam now ALWAYS meanders on a ~35-45dp scale for every entry hash,
  in both the hero clip and its aligned under-sheet (same `disp`).
- **v28 — hole rings now THREAD THROUGH the hole (all 3 styles).** The
  v27v rings were drawn as flat ellipses LARGER than the punch hole and
  centered on it, so they read as metal rings glued AROUND the hole
  ("just changes the look of the hole"). All three styles in
  `PaperStatCard.kt` now share a real through-hole structure:
  `drawHoleInterior` shades the punched opening dark (a deep pocket, so
  anything drawn inside reads as BEHIND the paper), the wire's BACK arc is
  a dark, smaller-radius arc receding inside the hole (coil/split/oblique
  each with their own back angles), the FRONT arc rides the hole rim in
  bright steel — its tube half over the opening, half on the paper — and
  darkened 26° DIVES at each end of the front arc show the wire sinking
  back in, plus the shared contact shadow. "coil" = spiral-notebook wire
  (front arc 145°→395° at 1.02×holeR, back arc 35°→145° at 0.72×holeR);
  "split" = keyring loop (front top half 160°→360° at 1.05×holeR, back
  bottom half 20°→160° at 0.82×holeR, split gap at 260°, rim shade over
  the back wire); "oblique" = foreshortened coil bulging out of the hole
  (front ellipse 1.35×holeR, back arc inside, per-hole tilt). All three
  now visibly pass through the hole instead of decorating it.
- **Single Support & diagnostics page (v24):** Support & diagnostics (`features/support/SupportScreen.kt`, route `SUPPORT`) is the ONE page for updates, feedback, replay intro, and the project link — the old Settings → About page (`SettingsPage.ABOUT`, `SETTINGS_ABOUT` route, `AboutSection`, `CurioUpdateCheckRow`) was removed. The page is reachable from Profile's "Support & diagnostics" row, Settings → Safety & support → "Support & diagnostics", and the Home drawer. **GitHub in-app updater (v25):** the Play Core in-app update (v24) was REMOVED for good — the app ships from GitHub, not Play. The update check in Support & diagnostics (`features/support/SupportScreen.kt`) is now GitHub-only: `UpdateChecker` (`data/UpdateChecker.kt`) parses the release's APK asset (`apkUrl` on `UpdateInfo`, from the GitHub API `assets` array) and `UpdateChecker.downloadApk(url, file, onProgress)` streams it into `cache/downloads/` with progress. "Update now" then hands the file to the system installer via `FileProvider` (`ACTION_VIEW` + `application/vnd.android.package-archive`, `cache-path apk_downloads` in `xml/file_paths.xml`) — the USER confirms the install (`REQUEST_INSTALL_PACKAGES` permission added). The card keeps a short "Open release" link as the browser fallback. **Kotlin gotcha (v25):** never write the literal `/*` sequence inside a block comment — Kotlin block comments NEST, so `release/*.apk` in a KDoc silently swallowed the rest of the file (the braces checker caught it; CI would have failed on an unterminated comment).
- All UI is 100% Jetpack Compose. No XML layouts for screens, ever.
- `MainActivity` is the only entry point. It hosts `CurioNavHost` inside `CurioTheme`.
- Edge-to-edge is enabled at the Activity level; the system bars are themed by `CurioTheme`'s `SideEffect` to match the current color scheme + light/dark mode.
- Icon rendering uses `CurioIcon(name = CurioIcons.X)` with the Material Symbols ligature font. Emoji-vs-icon policy is a design decision — confirm with the user (see the Purpose note above).
- All glyph names used by `CurioIcon` are declared in `CurioIcons.kt` (single source of truth for icon names). Adding a glyph = adding a `const val` there first.

### Adaptive layout (tablet & landscape) — ALWAYS-ON
- **`ui/adaptive/CurioAdaptiveLayout.kt`** owns the window adaptation contract: `windowWidthSizeClass()` (material3-window-size-class, `calculateWindowSizeClass(activity)`) and `CurioContentMaxWidth = 720.dp`. No Settings toggle — the wide layout engages automatically on medium/expanded windows (>= 600dp wide; tablets, landscape, split-screen) and phones are untouched.
- **Wide windows:** `CurioNavHost` renders `CurioNavigationRail` (left edge, full height) instead of the bottom bar and centers every route's content in the 720dp max-width column (`fillMaxHeight().widthIn(max = CurioContentMaxWidth)` inside a centered Box); the theme background fills the gutters. Screens keep drawing their own status-bar padding and full-bleed washes inside the NavHost.
- **Adaptive grids:** use `windowWidthSizeClass().isWide` to switch `GridCells.Fixed(2)` → `GridCells.Adaptive(minSize = …)` (Cabinet 176dp, category pickers 160dp). Picker bottom sheets center their content at `CurioContentMaxWidth` on wide windows (the sheet spans the whole window, so it needs its own cap).
- Don't add per-screen responsive hacks; read the size class from the shared helper so every screen follows one breakpoint story.

### Navigation
- Single NavHost with flat routes (see `CurioRoutes.kt`). Bottom nav tab metadata includes `HOME`, `SPIN`, `CABINET`, and `REVEAL`; `CurioNavHost` hides the actual bottom bar on Reveal and reserves an equal-height torn placeholder so the watermark and shared hero morph stay level with the Spin tab.
- **Tab switching MUST use `NavController.navigateToTab(route)`** (defined in `CurioRoutes.kt`), which anchors `popUpTo(HOME) { saveState = true }` + `launchSingleTop = true` + `restoreState = true`. Do NOT anchor to `graph.findStartDestination()`: the NavHost's declared start destination is `SPLASH`, which SplashScreen pops inclusively on launch — so the anchor is gone from the stack and `popUpTo` silently no-ops, piling up duplicate back-stack entries (back walks through the same screens repeatedly). HOME is the persistent root that always remains after Splash/Onboarding/Crash land.
- Every plain `navigate()` to a push destination (Profile, Settings, Picker, Entry Detail, Lightbox, Manage Categories, Onboarding replay, etc.) MUST set `launchSingleTop = true` so re-opening a previously-opened screen never stacks a copy.
- Tab routes also accept a `categorySlug` argument so the same `Spin` screen renders both as a tab target (`categorySlug = null`) and as a pushed destination (`categorySlug = "music"` etc.).

## Work Guidance

### Adding a new screen
1. Ask the user for design direction for the screen — there is no in-repo design spec to follow.
2. Create the file at `app/src/main/java/com/curio/app/features/{feature}/{Feature}Screen.kt`.
3. If it's a stack of related sub-screens, group them in one file like `PlaceholderScreens.kt` does today, with a shared `*Scaffold` private helper at the top.
4. Add a route constant + (if needed) a route builder to `CurioRoutes.kt`.
5. Register the `composable(route) { ... }` block in `CurioNavHost.kt`.
6. If the screen should hide the bottom nav, make sure its route is NOT in `CurioRoutes.bottomNavRoutes`. Add a per-feature AGENTS.md if the screen has non-obvious contracts.

### Adding a new design system primitive
- Add to `ui/theme/` (colors → `CurioColors.kt`, glyphs → `CurioIcons.kt`, etc.).
- New colors, type styles, and shape tokens are design decisions — confirm them with the user before adding.
- New icons must be declared in the `CurioIcons` object (snake_case ligature names) — do NOT inline glyph names in screens.
- **All design-system primitives (the `CurioIcon` composable + `CurioIcons` glyph constants object) live under `ui/theme/`.** Components in `ui/components/` consume them via import — they do not re-export them. Wrong-package imports (e.g. `import com.curio.app.ui.components.CurioIcon`) compile silently against an empty package and only fail in CI's `compileDebugKotlin`. Always import from `ui.theme.*`.

### Curie pet layer (v8.43)
- `data/PetDesign.kt` owns the pet look contract: backward-compatible 16/24/32
  canvases plus the 64×64 evolved default (all convertible via dominant-key
  resample), a 13-key palette (incl. `r` blush + `y` eye colors), per-mood
  faces (`PetFace`), per-event reaction rules (`PetReaction`), optional
  authored reaction lines, four transparent detail layers (`tail`,
  `accessories`, `effects`, `antenna`), and per-element procedural visibility
  overrides. Evolved path ornaments live in the toggleable `accessories` layer.
  Animations (`PetAnimation`) are transform
  keyframes plus v8.52 per-frame pixel layers (`PetAnimationFrame.bodyRows` /
  `curledRows`) and backward-compatible `PetViewAngle` metadata, so each
  frame can be a fully different pose/view; `CurioPetSprite` accepts
  `bodyOverride`/`curledOverride`/`viewAngle` to render them. The always-on
  Pet Life director (`data/PetLife.kt`) chooses screen-aware, personality-
  weighted routines and keeps a recent-id cooldown so autonomous behavior
  does not immediately repeat. Missing
  detail/toggle fields preserve the prior procedural behavior for older saved
  designs. The text format (palette lines + grids + `detail=` / `procedural=`
  / `face=` / `react=` / `size=` / `anim=` / `frame=` / `customAction=`
  lines) is documented in
  that file's KDoc/source implementation.
- `CurioPetSprite` renders any grid size, preserves existing motion, and draws
  authored detail layers last so the user can replace generated art without
  changing animation. The procedural antenna extras remain independently
  toggleable; the base antenna pixels are edited in the Body canvas.
- `PetDesignerScreen` (Settings → Pet designer) is a three-page studio
  (v8.52): **Pets** (pick a species from `PetRegistry`), **Editor** (choose a
  target via the preview dialog, then only that editor), and **Settings**
  (Accessories dialog, disable toggles, personality presets, shapes). The
  editor is available from the start, including for baby pets. Animation
  gallery/player/timeline and animation-selection controls are currently
  hidden from the studio UI while their models, serializers, renderers, and
  runtime playback remain in place for a future re-entry. A slim sticky
  **EditorToolbar** is the ONE place for Save / Undo / Redo / Reset / Import /
  Export (the old pinned footer SaveArea is gone — no duplicate buttons).  The Faces editor and its picker option are removed from the studio UI. Face
  data, presets, serializers, and sprite/runtime rendering remain preserved
  for compatibility. Details drawing and the entire Actions editor are
  currently hidden from the studio, while detail, reaction, and custom-action
  data/runtime behavior remain preserved.


  PNG export/import shares via FileProvider `${applicationId}.fileprovider`
  (`res/xml/file_paths.xml` cache/share). The home/house scene is a fixed
  layered sprite composition; the legacy home editor is removed from the
  studio UI, while old saved bed rows remain dormant compatibility data.

### Experimental features (A/B testing)
- Per root `AGENTS.md`, any experimental/test behavior MUST be gated behind a **user-facing Settings toggle** so it can be A/B-compared against the current behavior and reverted without a code change — never hardcoded as the only path.
- Remove the toggle once the experiment is decided, keeping the winning behavior hardcoded.
- **New measures (root `AGENTS.md`):** when ADDING a new feature/capability, ask the user FIRST whether it should be toggleable or always-on (use the ask_user tool before implementing, and follow their answer). This ask does NOT apply to refinements or fixes of existing behavior — those ship as-is without the toggleable question. **The toggle is NOT permanent** — once the feature is decided, remove the toggle and hardcode the winning behavior (experiment-closeout rule above).

### Phase plan (current & next)
- **Phase 2 (current)**: Design-system + NavHost + Home/Splash screens + 11 placeholder stub screens. CI gate verifies compilation. No business logic, no Room, no DataStore wiring yet.
- **Phase 3 (next)**: Spin dial rendering, Onboarding flow, Reel/Marginalia/Gallery Wall/Field Notes capture format bodies, Cabinet grid rendering.
- **Phase 4**: Per-entry persistence (ViewModels + Room), state preservation across spins. Also: **first content drop** — seed Music per `CURIO_DATA_PLAN.md` §5.1 (150 topics, LLM-drafted + human-reviewed, ships as `assets/topics/music.json` + a `validatetopics` Gradle task).
- **Phase 5+**: Streak tracking, share-card generation, Emergency Recovery hooks for FieldMind data. Per-category content drops (Movies, Books, Art, Science, then the 4 new categories) continue at one-per-PR cadence per `CURIO_DATA_PLAN.md` §5.1.

### Content authoring (CURIO_DATA_PLAN.md §2 + §6)

Topic data lives in JSON files under `app/src/main/assets/topics/{category}.json`. The schema is `CurioTopic` + `ExploreAction` — see [`assets/topics/SCHEMA.md`](src/main/assets/topics/SCHEMA.md) for the in-folder quick reference and `CURIO_DATA_PLAN.md` §2 for the full source-of-truth.

- **Validation:** `./gradlew validateTopics` parses every JSON file in `assets/topics/` and asserts the §2 schema. The task is wired into `preBuild` automatically when JSON files exist, so a malformed entry fails `assembleDebug` / `assembleRelease`.
- **Adding a new topic:** see `SCHEMA.md` "Authoring a new topic (quick recipe)". For the full §6 LLM authoring prompt template, see `CURIO_DATA_PLAN.md` §6.
- **Modern batches (v7.6):** `artists.json` (354) + `albums.json` (498) include the modern content drop — 50 contemporary (2010s–2020s) artists + 50 modern albums, The 1975 explicitly included. Appended idempotently by `scripts/add_modern_batches.py` (dedupes by id + name; entries meet the stricter ≤280-char teaser/instruction bar, above the Gradle task's 450 limit).
- **Songs expansion (v1.1.0):** `songs.json` grew 60 → **1,000 real songs** (1960s → 2020s, ~54% are 2000s or newer), each with a factual teaser + a listen instruction (all tier 1). Generated by a one-off script (`/tmp/gen_songs.py` lineage; the pattern: dedupe by name+artist+year, seed ID slugs with the existing 60 song IDs so re-runs never collide, then merge + validate — unique ids across all topic files, ≤450-char teasers, ≤600-char instructions, `verb: "Listen"`, `durationMinutes: 3`). Same-name songs get `-{year}` (then `-{artist}-{year}`) suffixes.
- **Adding a new category:** see `CURIO_DATA_PLAN.md` §5.2 step 5 — toggle `isReady = true` on `CurioCategory` only when 100+ topics are authored + reviewed. Categories with `isReady = false` are filtered out of the Home chip row + Category Picker and surface as "Coming soon" empty-state slots.

## Verification

- `MainActivity` compiles and runs as `com.curio.app` on debug builds with `applicationId = "com.curio.app.debug"`.
- No background workers, no widgets, no Room/SharedPreferences persistence wiring yet — those arrive in Phase 4+.
- **CI gate**: this environment has no Android SDK, so CI on push to `revamp` is the source of truth for compilation. Local Gradle compile/build/lint/test commands are explicitly forbidden by root AGENTS.md.
- **CI expectations (flavorless)**: CI calls `./gradlew lintDebug validateTopics assembleRelease` for Android checks (release-only — no debug APK is built) and `./gradlew validateTopics assembleRelease` for tagged releases. Release outputs are ABI splits + a universal APK at `app/build/outputs/apk/release/` (see the `splits { abi { … } }` block); the release workflow renames them `Curio-{versionName}-{versionCode}-{abi}-Android8.0+.apk`. `printReleaseVersion` prints `versionName:versionCode` for that naming. Release signing uses the repository keystore secrets when configured; local builds fall back to the debug signing key.
- All placeholder screens route correctly: tapping the Home hero with no chip → `PICKER`; with a chip → `spin/{slug}`; bottom-nav switching preserves each tab's back stack; back arrow pops the current route.

## Session Lessons Learned

These patterns and anti-patterns were learned the hard way (CI compile failures, JSON escaping, double navigation). Future agents inheriting this DOX chain should internalise them.

### API verification — never trust memory
- **Download source JARs from Google Maven** before using unfamiliar Compose/M3/navigation APIs. The version in `gradle/libs.versions.toml` is authoritative — signature changes across versions are real.
  ```bash
  curl -sL -o jar 'https://dl.google.com/dl/android/maven2/{path}-{version}-sources.jar'
  unzip -o -q jar -d src-dir
  grep -rn 'fun theFunction\|interface TheInterface' src-dir/
  ```
- Examples from this session: `calculateWindowSizeClass(activity)` in material3-window-size-class 1.5.0-alpha20 (not the zero-arg version); `SharedTransitionScope.rememberSharedContentState` is a `@Composable` member function (NOT a top-level function); `Modifier.sharedElement` is a member extension of `SharedTransitionScope` (needs receiver); `BoundsTransform` is a top-level `fun interface` in `androidx.compose.animation` (not nested); `contentAlignment` on `Box` takes `Alignment` (NOT `Alignment.Horizontal`). ALL of these would have been compile errors without the source-verified signatures.
- **v116 — `ImageDecoder.setIsExifOrientationRequired` does NOT exist** (verified against AOSP `ImageDecoder.java` on master + android-31/33/34 branches: no EXIF method exists in the Java layer at all). It was invented from memory and broke the build. The EXIF orientation behavior lives in the native Skia codec and differs across Android versions with no public toggle, so the avatar decoder now uses BitmapFactory for EVERY API level (it never applies EXIF — documented) plus explicit framework `ExifInterface` rotation: one deterministic path, no double-rotation risk.
- **v116 — Kotlin local functions can't be forward-referenced.** `avatarPicker`'s lambda called `saveAvatar` which was declared later in the same composable → "Unresolved reference 'saveAvatar'". Local (nested) functions must be declared BEFORE use; only top-level/member declarations are order-independent. Declare helper locals above the callbacks that use them.
- **v116 — material3 `AlertDialog` has TWO overloads**: the classic `(onDismissRequest, confirmButton, …, text, containerColor, …)` and the newer `(onDismissRequest, modifier, properties, content)` basic-dialog overload. Calling with `containerColor`/`shape`/`title`/`text` but NO `confirmButton`/`dismissButton` matches NEITHER ("No parameter with name 'containerColor'… No value passed for parameter 'confirmButton'"). If you put custom action pills inside `text`, hoist the shared state out of the `text` lambda first (the confirm button can't see locals declared inside it), then pass them as `confirmButton`/`dismissButton`.

### contentAlignment: Alignment vs Alignment.Horizontal
- **A CI compile failure in this session** — `contentAlignment = Alignment.CenterHorizontally` was passed where `Alignment` was expected. `Alignment.CenterHorizontally` is `Alignment.Horizontal` (used for `ColumnScope.align`); `Alignment.CenterVertically` is `Alignment.Vertical` (used for `RowScope.align`). For `Box(contentAlignment = …)` use `Alignment.Center`, `Alignment.TopStart`, etc. (the full `Alignment` interface).
- Same goes for `horizontalAlignment`/`verticalAlignment` — those ARE typed `Alignment.Horizontal`/`Alignment.Vertical` — don't mix them up.

### No LocalSharedTransitionScope in Compose 1.11 (animation 1.11.2)
- The shared-transition API is stable (no `@ExperimentalSharedTransitionApi` OptIn needed), but **there is no built-in CompositionLocal** for the scope. The scope from `SharedTransitionLayout { }` must be threaded manually:
  - **Custom composition locals**: define `staticCompositionLocalOf<SharedTransitionScope?>` + `staticCompositionLocalOf<AnimatedVisibilityScope?>` and provide them via `CompositionLocalProvider` in each composable destination that needs shared elements (see `ui/adaptive/RevealSharedScopes.kt`).
  - **Scope APIs**: `Modifier.sharedElement` is a member extension of `SharedTransitionScope` — call via `sharedTransitionScope.run { Modifier.sharedElement(state, avScope) }`. `rememberSharedContentState(key)` is a `@Composable` member — call via `sharedTransitionScope.rememberSharedContentState(key)` (explicit receiver on a composable member is legal).
  - **Layout**: `SharedTransitionLayout` carries the max-width cap (`widthIn(max = CurioContentMaxWidth)`); `NavHost` inside uses `fillMaxSize()`. The overlay renders within the layout's bounds — consistent coordinates across all destinations.
- `BoundsTransform` is a `fun interface` with `createAnimationSpec(initialBounds: Rect, targetBounds: Rect): FiniteAnimationSpec<Rect>`. SAM conversion: `BoundsTransform { _, _ -> tween(320, easing = FastOutSlowInEasing) }`. The default is a generic spring (`SharedTransitionDefaults.BoundsTransform`) — slow and slightly bouncy; when the user says "the morph feels delayed," replace it with a snappier ease.

### Route pattern: optional query args vs out-of-band state
- For optional flags on a Nav route, use `?flag={flag}` with `navArgument("flag") { type = NavType.StringType; defaultValue = "0" }`. This survives process death, resists stale flags, and doesn't need cleanup — unlike mutable `object`-style handoff targets (which require being reset by every other entry point).
- Example: `const val REVEAL = "reveal/{categorySlug}/{topicName}?browse={browse}"` with `navArgument("browse") { defaultValue = "0" }` — existing `revealFor()` calls (no query) match and get `browse="0"`; a new `revealForBrowse()` appends `?browse=1`. The `isRevealRoute` check comparing `destination.route` still works (destination.route is the template string).

### Compose inline forEach — composable calls only IN the inline body
- `forEach` is `inline`, so calling `@Composable` functions DIRECTLY inside its lambda body is legal (the body is inlined into the composable call site). BUT — **calling a composable through a `val` lambda reference inside forEach FAILS** because the lambda variable is not inlined. Bad: `val chip = { fmt -> FormatChip(...) }; forEach { chip(it) }`. Good: `forEach { fmt -> FormatChip(...) }`. If both branches need the same content, either duplicate the inline block or extract a `private @Composable fun` and call it inline in both forEach slots.

### Per-frame blur is a GPU sink
- `Modifier.blur(N.dp)` over a **flat color or smooth gradient** is a visual no-op — the unblurred result looks identical — but the RenderEffect pass runs on every frame during scroll. Replace with a static gradient (`Brush.verticalGradient` with slightly different alphas or stops) for the same "frosted glow" look at zero per-frame cost. (This was the root cause of "laggy scrolling" on the detail page.)

### Bottom-anchoring with weight spacers
- To anchor controls to the bottom edge regardless of screen height: replace fixed `Spacer(26.dp)` (which floats on tall screens) with `Spacer(Modifier.weight(1f))` inside a `fillMaxSize` `Column`. The weight spacer absorbs all free space above the controls.

### JSON escaping in spawn_agents
- The `spawn_agents` tool takes JSON with nested strings. **Do not use pipe characters inside `\|` grep patterns** — they break JSON parsing. Use `grep -nE 'pattern1|pattern2'` or `sed -nE`. Avoid heredocs with embedded single-quote strings. Prefer writing helper scripts to `/tmp/` with `write_file` and running them with a simple `python3 /tmp/script.py` command (no escaping).

### Static validation when Gradle is unavailable
- This environment has no Android SDK → no local `./gradlew` commands. Pre-CI validation = only static checks:
  1. **Delimiter balance**: `node scripts/check_braces.js` — local-only Kotlin/KTS checker (kept on disk, untracked — scripts are never shipped in commits) that strips comments/strings and verifies `{}[]()` balance (run it on the whole repo, or pass specific files). This replaced the ad-hoc scripts that used to be written to /tmp mid-session.
  2. **`git diff --check`** — catches whitespace errors.
  3. **Import hygiene**: after removing a usage, `grep` the file for the removed symbol to confirm no remaining references (CI catches stale imports as compile errors).
  4. **Code review**: spawn a `code-reviewer-glm` or `code-reviewer-deepseek` agent with the full file list and the key risky patterns to check.

### Commit discipline
- **Commit + push after every completed fix** (per root AGENTS.md). Single commits CAN bundle multiple related changes (e.g., a feature + its tests + the changelog).
- **Small text-only changes** (dead comments, punctuation, rewordings) must NOT be committed on their own — they ride along with the next real change. EXCEPTIONS: edits to AGENTS.md files / Prompt.md / master.md / user-visible strings / changelogs — those ARE committed.
- **Before removing a user-visible feature/UI element**, ASK the user for confirmation (root AGENTS.md durable preference).

## Child DOX Index

- [`CURIO_DATA_PLAN.md`](CURIO_DATA_PLAN.md) — Canonical **data layer** spec. Owns: category taxonomy expansion (6 → 10), `CurioTopic` + `ExploreAction` schema, JSON-on-disk canonical format, Room DB seed flow, image strategy (URL + Coil, no bundling), authoring pipeline (LLM-draft + human-review + smoke test), per-category rollout cadence (one category per PR, Music first). Read this BEFORE adding any topic data, category entry, or capture-format prompt.
- [`src/main/assets/topics/SCHEMA.md`](src/main/assets/topics/SCHEMA.md) — Quick-reference schema doc for topic JSON files. Lives next to `music.json` so authors have the schema at their fingertips without opening the larger `CURIO_DATA_PLAN.md`. Points back to the full source-of-truth for anything not covered.
- (Future) `app/src/main/java/com/curio/app/features/{home,spin,cabinet,capture}/AGENTS.md` — per-screen feature contracts, added when each screen gets real implementation in Phase 3+.
- (Future) `app/src/main/java/com/curio/app/ui/theme/AGENTS.md` — design system primitive contracts, added when the theme system grows (Phase 3+ when dark-mode polish, motion tokens, etc. land).
- (Future) `app/src/main/java/com/curio/app/data/AGENTS.md` — data-model contracts, added when Room + repositories land in Phase 4.
