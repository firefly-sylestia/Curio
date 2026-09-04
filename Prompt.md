# Prompt — Tablet UI: revert the editorial redesign to the mobile design; torn header not sticky in landscape

## Request (user, 2026-09-04)
"the tablet ui overhall is bad — from topic browser the lists shows in 2
grid its bad, the open topic also doesnt open, and the in shuffle page the
category picker and filter isnt there, and also the non tear design is bad
also the home stat card so fix all of them do web research and use the
mobile design as the start and just dont make the tear header sticky in
landscape tablet, properly research and fix everything."

## Interpretation (user-confirmed)
The previous session shipped a full "clean editorial / no-tear" tablet
redesign across 7 commits (8920a88c..29eeee78: Spin stage rebuild, Home /
Cabinet / Recents / Settings editorial headers, Topic Browser 2-up
master-detail grid + reveal pane). The user rejects all of it. Direction:
1. **Revert the redesign wholesale** — tablet/wide returns to the MOBILE
   design (torn heroes, phone layouts). This fixes: Browser 2-grid → back
   to the single-column list; Browser tap → opens the topic again (rows
   navigate straight to the reveal); Spin's Category + Filter pills are
   present under the deck on wide; torn heroes restored everywhere
   ("non-tear design is bad"); Home stat row back in the torn hero
   ("home stat card").
2. **One wide-only deviation, user-confirmed via ask_user "Scroll away
   everywhere" (incl. Browser + Cabinet search/filter headers):** on WIDE
   windows (>=600dp — landscape tablet) the torn header is NOT sticky — it
   scrolls away with the content. Phones keep the pinned glass hero
   (v263 "RESTORED STICKY HERO") byte-for-byte.

Web research grounding: NN/g (sticky headers eat viewport space; worst on
short landscape viewports) + single-column vs forced multi-column list
readability for dense text rows — both support the user's asks.

## What changed (all Android `app/` only)

### A. Revert to mobile baseline (`git checkout cd1533b8` on 20 files)
Restored 19 Kotlin screens + the store changelog to the pre-redesign
state (cd1533b8 = parent of the redesign; phones were already
byte-identical there, so this is a clean full revert). Files: SpinScreen,
HomeScreen, CabinetScreen, TopicDatabaseScreen, RecentScreen,
ManageCategoriesScreen, OutfitShopScreen, PetDesignerScreen,
QuestsScreen, RecycleBinScreen, BackupToolsScreen, ExperimentsScreen,
SettingsHubScreen, SettingsSectionScreen, ShareHubScreen,
UserExperimentsScreen, PromoModeScreen, SupportScreen, UpdatesScreen.
No redesign symbols (SettingsWideHeroHeight, settingsHeroContentTopHeight,
RecentWideHeader, HomeEditorial*, DatabaseWideRow/Pane, buildWideRows)
left anywhere — grep-verified. Commit range cd1533b8..HEAD was exactly
the 7 redesign commits (+Prompt.md), so restoring = clean revert.

### B. Torn hero scrolls away on wide (16 screens)
Shared rule (see app/AGENTS.md "Torn heroes on WIDE windows scroll away"):
list/grid top contentPadding `SettingsHeroTotalHeight` → `if (wide) 0.dp
else …`; an `if (wide) item(key="hero")` leads the scroll content; the
pinned overlay call wraps in `if (!wide)`. In-list heroes pass
`glassBackdrop = null` (opaque back pill). On wide the list's
`layerBackdrop` is skipped wherever the hero sits inside the captured node
(self-sample cycle) and inner-pill liquid-glass gates add `!wide`.

Simple screens (title-only heroes, duplicated small hero calls):
Experiments, UserExperiments, BackupTools, SettingsSection, Support,
PromoMode, Quests, Updates, OutfitShop (imports isWide +
windowWidthSizeClass added), ManageCategories, Recents (non-empty branch;
empty branch untouched — its own in-flow hero already leads the page),
Recycle Bin (same), ShareHub (grid: `item(key="hero", span={…})`, `wide`
pre-existed).
Empty-state branches left as-is where they already own an in-flow hero
(Recents/Recycle Bin empty columns) or nothing scrolls.

Complex screens (hero carries search/category/sort controls + filter UI):
- **Topic Browser** (`TopicDatabaseScreen.kt`): hero call extracted into a
  local `heroFor(backdrop)` @Composable lambda + `heroGlassOn` gate; wide
  list = hero item + (when filter UI open) a `filter-ui` item reusing
  DatabaseCategoryPanel / ActiveFilterChips with a new `restTop = 0.dp`
  param (0 = flush under the in-list hero; phone overlay keeps its pinned
  rest top). Phone overlay + pinned filter AnimatedVisibility gated
  `if (!wide)`; `contentTop` resolves 0 on wide (list padding, scroll
  indicator); back-to-top arrow offset 90.dp on wide; page-nav glass
  gated; grid/list layer gate adds `&& !wide`.
- **Cabinet** (`CabinetScreen.kt`): same treatment — `cabinetHeroFor`
  lambda + `cabinetHeroGlassOn`; wide grid = hero item + filter-ui item
  (CabinetCategoryPanel / CabinetActiveFilterChips with `barTop = 0.dp`);
  pinned filter bar gated phone-only; empty-state branch wraps the hero at
  the top of its Column on wide; layer gate adds `&& !wide`;
  cabinetTitle/cabinetSubtitle hoisted above the root Box.
- **Pet Designer** (`PetDesignerScreen.kt`): wide list = hero first item;
  pinned overlay phone-only; the floating StudioFloatingToolbar already
  pins itself under the status bar and is designed to stay while the hero
  scrolls (v156 comment) — untouched.
- **Profile**: hero already scrolls as the LazyColumn's first item — no
  change. **WidgetEditor**: hero in a plain Column, nothing scrolls behind
  it — no change. **SettingsHub two-pane (wide)**: hero is an in-flow top
  bar above the panes (nothing scrolls under it) — no change.

Phone paths verified behavior-identical: every gated expression collapses
to the old value when `!wide`, and brace/paren balance is 0/0/0 vs the
cd1533b8 baseline across all 16 edited files (script-verified).

## Verification
- No Gradle build possible in this environment (CI validates on push).
- Structure checks run: brace/paren/bracket balance delta 0 across the 16
  edited files; no leftover redesign symbols; imports for
  `windowWidthSizeClass`/`isWide` present in every edited file; one `wide`
  val per file.
- Watch CI on the push; if it reports NEW errors in untouched files, the
  previous fix may be incomplete (VERIFY-ONE-CYCLE).

## Follow-ups
- Tuning numbers (back-to-top 90.dp, wide in-list hero at default banner
  size) are knobs against device screenshots.
- The wide in-list hero sits inside the list's `wideContentEdgePadding`
  column (not full-bleed) — the mobile look, just centered; revisit if the
  tear reads too narrow on very wide windows.
