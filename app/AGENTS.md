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
│       ├── CurioBottomNav.kt       # 3-tab M3 NavigationBar with saveState/restoreState
│       ├── CurioCategoryChip.kt    # FilterChip per category + CurioWildcardChip
│       ├── CurioEmptyState.kt      # universal §13.7 empty-state skeleton
│       ├── CurioHeroCard.kt        # ~40% vertical hero Spin card on Home
│       └── CurioStreakPill.kt      # streak indicator pill + CurioSecondaryAction helper
└── features/
    ├── splash/SplashScreen.kt      # §13.1 splash — angular signal-portal mark + "Curio" + 3-dot pulse, 800ms → HOME
    ├── home/HomeScreen.kt          # §3 home — top bar, greeting, streak, hero, chips, recently explored empty state
    └── PlaceholderScreens.kt       # ONE file containing 11 stubs: Spin, Cabinet, CategoryPicker, TopicReveal, SaveCapture, EntryDetail, Settings, Onboarding, ManageCategories, TopicHistory, Lightbox. Each uses a shared `PlaceholderScaffold` with back arrow + glyph + title + subtitle + "Design phase · logic comes later". Real implementations replace these one-by-one in later phases.
```

### Resources

- `app/src/main/res/font/geom.ttf` — bundled display/headline typography
- `app/src/main/res/font/material_symbols_outlined.ttf` — bundled UI + category icon font
- `app/src/main/res/values/strings.xml` — Curio app name + screen titles + category display names
- `app/src/main/res/values/themes.xml` — `Theme.Curio` (M3 DayNight no-actionbar, Midnight Signal bootstrap surface)
- `app/src/main/res/values/colors.xml` — Midnight Signal XML resources used at the OS-level splash/background before Compose takes over
- `app/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml` — angular open-portal launcher mark with midnight background, electric-blue frame, mint aperture, orange spark, and themed monochrome mask
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher{,_round}.xml` — adaptive-icon declarations referencing the colored and monochrome layers above
- `app/src/main/assets/topics/` — Curio topic data files and schema reference (one per ready category; see Content authoring below)

## Local Contracts

### Identity
- `namespace = "com.curio.app"` (new package, separate from FieldMind)
- `applicationId = "com.curio.app"` (new install, separate from FieldMind; users install Curio as a separate app)
- `minSdk = 26` (Android 8.0+ — all release APKs are labeled with this), `targetSdk = 37`, `compileSdk = 37`
- `versionName = "1.0.1"` (default, bumped by 0.1 in v34 per user request; the release workflow overrides it with the git tag minus the leading `v`, e.g. tag `v1.2.3` → `1.2.3`), `versionCode = 20260919` (date-based; unchanged by tags)
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
- **Theme styles (Settings → Appearance):** three mutually exclusive styles — **Curio** (default: warm cream palette + category tints), **AMOLED** (forced dark, pure-black surfaces, tints off), **Material** (the device's Material You dynamic palette for surfaces/backgrounds/controls; category accents stay the true researched colors so cards/heroes/gradients stay vivid, tints off). Persisted as `AppPreferences.themeStyleState`; the wash helpers gate on `AppPreferences.tintWashEffective()`. All category-accent fills/ink read `themedAccent()` so the Material style shades them app-wide.
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
- **v29 — Topic Database opens with ZERO loading (prebuilt index).**
  `scripts/build_topic_index.py` (LOCAL tool, gitignored per the
  `/scripts/*.py` rule — run `python3 scripts/build_topic_index.py` after
  ANY topic edit) merges every `assets/topics/*.json` into ONE
  `app/src/main/assets/topic_index.json` (`{"version":1,"topics":[…]}`, ~0.8MB
  APK delta after asset compression) with the lowercased search keys and
  the sort YEAR precomputed at BUILD time (same precedence as the DB's
  `topicYear`: name paren → targetName paren → teaser year → instruction
  year → decade tag). `TopicJsonLoader.loadIndex()` parses it once
  (reusing `parseTopic`, cached, `cachedIndex()` sync accessor) and
  `MainActivity` prewarms it in the background at cold start. The Topic
  Database renders from the index when present (grouped per lane, wildcard
  lane = wildcard.json originals only) — no per-category parses, no runtime
  lowercase/year work — and gracefully falls back to the live per-category
  load when the asset is missing. Scaling note for 20k+: the same
  precompute-at-build idea is the path — a SQLite/FTS5 or FlatBuffers index
  would keep instant queries at any size (see Prompt.md).
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
