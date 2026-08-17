# Prompt.md — Request log

## Current request — COMPLETED: mood board full-screen + quote-card fixes (v145)

The user asked (across two messages, refined): (1) fix the full-screen mood board so
what you see while editing is what you get when saved; (2) fix quote-card rendering in
the saved PNG (cards came out small and misplaced); (3) add quote-card RESIZING (not
just expanding) with a size limit — confirmed "the resize is for quote cards only";
(4) explain why the quote card looked the same in both the small and full-screen boards
— confirmed: make them INDEPENDENT per view; (5) Clear board should also clear quote
cards; (6) on an empty full-screen board the Quote chip should sit at the Clear-board
spot, moving up once content is added.

### Root causes found
- **Same in both views**: positions were already separate (quotePositions vs
  quotePositionsFull) but the full-screen RESIZE handler also wrote the SHARED
  `quoteCards.setWidth`, and the full-screen MOVE handler pulled the shared inline
  width in — so resizing in full-screen changed the small board.
- **PNG cards small/misplaced**: `MoodBoardShareCard`'s canvas mirrors the board's
  aspect (raw × scale), but `MoodBoardFloatingCards` got `rawSpace=false`, so the v60
  40% display cap bound `displayScale` against the RAW board width (`canvasWPx = maxX`)
  — on the 4–8× export canvas a resized card rendered at ~raw width instead of × scale.
  Same cap shrank cards in the expanded dialog against the editor's exact widths.
- **Resize stretched flat**: `MoodBoardFloatingCard` always used `slot.h` for the
  height regardless of the resized width; `maxW` allowed spanning the whole board.

### Changes (commit —)
- MoodBoardZoom.kt: `cardH = cardW × (slot.h/slot.w)` (height follows width at the
  paper aspect — never-resized cards keep exact slot dims); resize grip `maxW` capped
  at 60% of the visible board (still respecting `boardW - x`).
- GalleryWallFormat.kt: full-screen `onResizeQuoteOverride` writes ONLY
  `fullQuotePositions[i].w` (no shared setWidth); `onMoveQuoteOverride` preserves the
  full placement's own width. Full-screen seeding keeps the legacy fallback to inline
  spots (mirrors fullTiles). Clear board now also wipes all quote cards
  (`removeCard(0)` loop) with updated confirm text; `boardHasContent` (tiles OR quotes)
  drives the Clear button visibility and the Quote chip's bottom padding (16dp empty /
  88dp with content).
- MoodBoardExport.kt + EntryDetailScreen.kt (expanded dialog): `rawSpace = true` so
  quote cards render at exact raw width × board fit, matching the full-screen editor.
  Inline editor + saved small card keep the 40% display cap (v60/v108 look).

### Verification
No Gradle build here (CI validates on push). On-device: (1) arrange quotes in
full-screen, resize one, confirm the small board is untouched; (2) Save/Share the PNG —
quote cards at their true size and spot; (3) open the expanded saved board — same;
(4) resize a quote card — whole note scales, capped at 60% of the board; (5) Clear
board on a board with images + quotes — confirm text mentions both, all wiped;
(6) empty full-screen board — Quote chip at bottom; add an image — chip moves up.

## Current request — v149: (1) revert the progress UI redesign, keep only better page-count editing; (2) new saved-entry share UI with preview

The user: "lets add a proper new share for saved entries with preview and a new share
ui, also revert the progress ui and i only meant you to change the page count look and
its editing way. no redesign it, the way to edit the page count is bad thats what i
meant not the progress ui".

### (1) Progress revert (CurioProgressPill.kt)
The v135 stepper-first dialog (31e5fea) was over-reach. Restored the pre-redesign
ring dialog via `git checkout 4558e99` (big % + count inside a 132dp ring, −/+ steppers,
slider, Finish + Save only), then re-applied two improvements:
- Slider snap fix KEPT: ≤200 total → whole-unit steps; big totals → continuous
  (rounded) — the "editor isn't working" bug.
- Page-count editing IMPROVED: the count is now a plain display and an explicit
  "Edit total" chip + pencil under it opens the inline numeric field (the old hidden
  tappable count read as plain text — the actual complaint).
The editable-target FEATURE (TopicProgressStore.setTarget + alt-edition pill) stays.

### (2) Share sheet (EntryDetailScreen.kt)
EntryDetail's More → Share used to fire the chooser with no preview. Now:
- `showShareSheet` state in DetailStickyBar; Share item opens `EntryShareSheet`
  (ModalBottomSheet, theme surface, drag handle).
- Sheet: title, live 320dp `CurioShareCard` preview on a shadowed stage, Image card /
  Text pill picker (solid-secondary selected per v131), full-width Share button.
- Image → existing `shareComposableCard` 400×400 PNG; Text → plain-text summary via
  `entryShareText` (name, teaser, category · format · captured date, Curio footer).

## Earlier completed request (v148)
Pet Designer studio bar gets the nav-bar collapse animation

The user clarified the "unify the pill style" ask: "when i meant to unify the pill
style i also meant the animation i meant use similiar animation just like in home
screen nav bar use similiar stle collapse". v142 had restyled the studio bar to the
floating pill CONTAINER but kept static `weight(1f)` tabs with always-visible labels.
`PetStudioTab` now mirrors `FloatingNavPill` (CurioBottomNav.kt) verbatim:
- Pills rest icon-only at 52dp; the ACTIVE pill springs to 112dp (same spring:
  dampingRatio 0.75, StiffnessMediumLow) and slides its label out
  (expandHorizontally(Start) + fadeIn(160)); deselected label vanishes instantly
  (exit tween(0)). Solid `secondary` fill + `onSecondary` ink, same as the main bar.
- The studio Row now CENTERS its content-sized pills (was weight(1f)), so the total
  width change stays balanced in the floating container.
Added animation imports (AnimatedVisibility, expand/shrinkHorizontally, fadeIn/Out,
Spring, animateDpAsState, spring). Brace balance verified.

## Earlier completed request (v147)
Drawer floats ABOVE the nav bar (bar stays composed)

The user: "why the navbar disappears in drawer, i meant you to place the drawer above
it not disappear the nav bar itself and reappear." v135 had hidden the floating pill
bar while the drawer was open (`!CurioDrawerState.isOpen` gate in CurioNavHost) — the
bar visibly vanished at drawer-open and popped back at close, which read as a glitch.
The intent was the drawer sliding OVER the bar, bar composed underneath.

### Root cause
`ModalNavigationDrawer` lived inside HomeScreen (a NavHost route); the floating bar
is drawn AFTER the NavHost content in the root Box, so it painted OVER the drawer.
zIndex can't escape the parent Box — the drawer had to be HOISTED above the bar.

### Changes (commit —)
- CurioNavHost.kt: owns `rememberDrawerState` + `rememberCoroutineScope`; a
  `LaunchedEffect(CurioDrawerState.openRequest)` opens it and a second effect
  publishes `drawerState.isOpen` back to `CurioDrawerState`. The root Box (page +
  rail + floating bar + tour dock) is now wrapped in `ModalNavigationDrawer` with
  `HomeDrawerContent` as its sheet; the bar's visibility `if` dropped the
  `!CurioDrawerState.isOpen` gate (keeps only the tour gate).
- HomeScreen.kt: drawer state/effects/wrapper removed — the page keeps a plain
  `Box(fillMaxSize())` in the drawer's old slot (no re-indent); the hamburger calls
  `CurioDrawerState.requestOpen()`; `HomeDrawerContent` is now `internal`;
  dead imports dropped (DrawerValue, ModalNavigationDrawer, rememberDrawerState,
  DisposableEffect, rememberCoroutineScope).
- CurioBottomNav.kt: `CurioDrawerState` gains `requestOpen()` (increments a private
  `openTick` exposed as `openRequest`) — doc updated to v147 behavior.

### Verification
Brace/paren balance checked for both files; no Gradle build here (CI validates on
push). On-device: (1) open the drawer from Home — the bar stays composed and the sheet
+ scrim slide over it (no vanish/pop on close); (2) drawer navigation still closes the
sheet then routes; (3) tour pill still floats (bar hides only during the tour).

## Earlier completed request (v146)
Reveal year pill → top bar: "don't place the year pill in reveal hero top corner —
it's already covered by progress pill, place it above alongside category at the top
but to the left corner." The v141 hero top-left pill ROW (byline + year) collided
with the progress badge at the hero's TOP-RIGHT on long-byline topics. Fixed in
TopicRevealScreen.kt: the category chip's `weight(1f, fill=false)` now wraps a Row
holding the chip + the year pill (frosted `categorySurface`, Schedule glyph,
`categoryInk()`) so "1851" sits next to the category at the top-left; the hero's
top-left row is reduced to the byline pill ONLY (the morph element).

### Earlier completed request (v144)
Tour Skip/Next dock → floating pill bar (rounded-50 surfaceContainerHigh capsule,
12dp air gap), main nav bar yields during the tour. Plus v143 CI compile fix: import
`titleAndYearQualifier` in Spin/Reveal + `yearQual.orEmpty()` for the Text overload.
