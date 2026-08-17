# Prompt.md — Request log

## Current request — new Stats page + CI fix

User: "ask me for with suggestions for the new stat page" → ask_user
answers: centerpiece = interactive constellation brain map; sections =
ALL (streak+level, lifetime totals, per-category, quests & badges);
reached from drawer AND Profile; style = observatory; PLUS fix the CI
failure (CurioProgressPill.kt:342 "No parameter with name
'horizontalAlignment'" — v168 wrote a Column param on a Row →
Arrangement.spacedBy(8.dp, Alignment.End)) and push.

StatsScreen.kt (new, CurioRoutes.STATS):
- StatsSkyHeader: celestial band (mirrors drawer sky colors), stars,
  crescent, back pill, title.
- StreakLevelCard: streak + best streak; level + xpProgress bar to next.
- StatsConstellationCard + CategoryConstellation: per-lane stars — two
  lobe arc layout, deterministic jitter (CategoryId.name.hashCode),
  radius by saved count, glow when active this week, tap selects within
  34dp (detectTapGestures), empty tap clears; selection chip shows name /
  count / active-this-week. themedAccent resolved in composition (NOT the
  draw lambda — @Composable).
- LifetimeTotalsCard: 8 counters in a 2-col grid.
- JourneyCard: stages done + medals row (CurioBadgeMedal 40dp) + Quests
  link.
- LanesBreakdownCard: per-lane rows (tap selects in the constellation).
- Data: repo.getAll() grouped by topic.categoryId ∪ categoriesState;
  StreakTracker; CurioQuests counters + xpProgress + levelTitle.

Entry points: drawer map card onClick + "Stats & insights" sub-row;
Profile ProgressAndAchievementsCard onOpenStats pill.

CI FIX ("fix this and push"): StatsScreen.kt "Unresolved reference
'Column'" at all `StatsCard {` sites + the declaration — the shell's
`content: @Composable Column.() -> Unit` used the layout FUNCTION as a
receiver TYPE. Fixed to ColumnScope (import added). LESSON: receiver
scopes are ColumnScope / RowScope / BoxScope, never the composable name.

## Completed request — stats time-range filter from the drawer selector

User: "Make the stats constellation filter to a time range via the 'This
Week' selector on the drawer map".

- New features/stats/StatsRange.kt: StatsRange enum (WEEK/MONTH/ALL),
  StatsRangeState singleton (mutableStateOf, private set — CurioNavTint
  pattern), StatsRangeSelectorPill (DropdownMenu, bold current).
- Drawer map: decorative pill → the live selector (kept the ChevronRight
  stats-page hint). Stats constellation card header: same pill.
- StatsScreen: loads all entries once, filterForRange (capturedAtMillis
  within days), laneCounts/laneRecent/explored derive from the window;
  quest-category union only on All Time (no timestamps); subtitle shows
  the window. In-memory state.

Docs: changelog ADD, AGENTS.md v174d, this file. PUSHED (no "dont push").

## Completed request — new Stats page + CI fix

## Completed request — drawer redesigned as a tiny personal observatory

User pasted a full design spec (celestial header, "Your Curiosity Map"
constellation-brain, 3 clean nav rows, illustrated planetary footer) and
added an SVG at the repo root, then said "ask if not understand the plan".

ask_user answers: (1) map stats = REAL data where the app tracks it
(streak, saves, spins/explores from CurioQuests.lifetimeState), falling
back to the design's numbers (Learned 128 / Questions 98 / Shared 72)
only where no counter exists — Overall = sum of the six; (2) the 3 nav
rows KEEP tap-to-expand sub-rows (collapsed by default) so Topic History
/ Manage Categories / Browse Topics / Support / Replay intro survive.

Implementation (HomeScreen.kt HomeDrawerContent):
1. HERO — rose torn banner → pre-dawn sky: seafoam gradient (dark =
   twilight teal), seeded stars, 3 four-point sparkles, faint dipper
   constellation, punched crescent moon, cream rolling horizon above the
   tear; cream avatar ring; ink steps with the sky.
2. CURIOSITY MAP — pastel card with header + decorative "This Week ˅",
   ConstellationBrain Canvas (22 nodes, gold→blue, glow) + center
   "Overall / Curiosity / N", 7 orbiting MapStat labels.
3. NAV ROWS — DrawerNavRow (tinted icon chip + label + subtitle +
   chevron): Quests & Levels direct (chevron-right), Your Curiosity +
   About expandable (collapsed by default); the 5 sub-rows stay.
4. FOOTER — res/raw/drawer_footer.svg (moved from repo root) via NEW
   io.coil-kt:coil-svg:2.7.0 dependency, rounded + shadowed, top fades
   into the surface, "v{version} · Made with curiosity ♥" on the
   landscape; old pinned footer removed.

CONTINUATION ("continue the plan") — the brief's behavior concept, grain
and thin borders:
1. Constellation is DATA-DRIVEN now: per-node weights from real lifetime
   counters (likes→left hemisphere, saves→right, pins→inner, spins→top,
   explores→brainstem) scale radius + glow; fissure bridges appear
   progressively from relationshipScore = quotes+pins+likes (>0→1,
   >5→2, >20→3, >60→4, >150→6 bridges); the 2 centre-fissure nodes keep
   the "recent discovery" glow.
2. Sky grain: 52 ultra-faint micro-dots (alpha 0.05-0.13, seeded
   HOME_DRAWER_TEAR_SEED+17) drawn under the stars.
3. Map card "thin border": 1dp seafoam hairline (0xFF9FCFC3 @45%) in
   LIGHT mode only — dark stays borderless (v157 hairline rule).

Docs: changelog ADD lines, AGENTS.md v174b note, this file. PUSH.

## Completed request — drawer redesigned as a tiny personal observatory

## Completed request — pill morph slower again + Cabinet "All" wears the Spin accent

User: "the navbar morphe open is still tooo rapid aah, make i even more
sloer. and in cabinet all use blue or red or whatever the spin screen
color have set not yellow or anything else." (no "dont push" → push).

1. SPRINGS: all four nav-pill specs in CurioBottomNav.kt
   (PillWidth/PillMotion/PillColor/PillExpand) + the mirrored
   Reveal* family (TopicRevealScreen.kt) + Studio* family
   (PetDesignerScreen.kt) dropped stiffness 750 → 400, damping stays
   1.0 (critically damped — zero bounce, just slower). All pills stay
   in lockstep (same physics per file). Springs.Calm (CurioMotion.kt,
   page/dialog entrances) untouched.
2. CABINET "ALL" YELLOW: `curioActivePillFill(null)` returned
   secondaryContainer = ButterYellow@30% — the stray yellow. Fix in
   `curioNavActiveAccent`: CABINET → `cabinetAccent ?: (spinAccent ?:
   primary)`. Cabinet "All" now inherits the Spin deck's published
   accent (default wildcard deck = CoralBlush, the coral brand) with
   the same light-mode 55% saturation mute as Spin's own pill; coral
   primary as the never-opened-Spin fallback. Butter fallback now only
   reaches Home-without-hero / non-tab routes. The wide-window rail
   shares the function, so it matches too.

Docs: changelog FIX lines, AGENTS.md v173, this file. Push.

## Completed request — mood board quote cards: resize scales the whole note; export matches the editor

User: "the moodboard quote cards are still very bugged. like aah its
anpoyong both in editing and sharing.". ask_user: editing = resize
"only expands from side and the text size stays the same"; sharing =
"either gets big and looks differnt or its position is somewhere else".

MoodBoardZoom.kt fixes (shared by editor + saved views + export):
1. RESIZE — the slip sized to a fixed slot height with a fixed-size font,
   so widening the card only stretched the paper. Now the slip sizes to
   its content and the quote text scales with the card
   (textScale = renderW / baseW, baseW = never-resized slot width × view
   scale, floor 0.5) — a true uniform note scale. Removed the editor's
   heightIn(h..1.5h) and the saved views' fixed height(h).
2. EDITOR/EXPORT MISMATCH — removed the v60/v108 40%-of-canvas display
   cap: displayScale is now just `scale` everywhere. The cap made a
   resized card small in the inline editor but raw-sized in the export
   ("gets big"), and the bigger card past the same top-left read as
   "somewhere else". Cards are rawW × scale in every view now. rawSpace
   param is inert (kept for API compat; export + GalleryWall still pass
   it).

Docs: changelog FIX line, AGENTS.md v172, this file. Pushed (no "dont
push" this time).

## Earlier completed request — share card 3:4 portrait + name/note/photo; dialogs theme-aware in light mode

User: "now improve the share card and not squared but 3:4 and also use
name and the note user added that option with photos option if added.
just more beautiful to share. and also give each dialog in light mode the
theme aware backgroud not cream white" (no "dont push" this time — pushed).

1. CurioShareCard (EntryDetailScreen): export cardSize 400×400 →
   450×600 (3:4); preview Box 320 square → 280 × 3:4 aspect. Card now
   shows: the sharer's display name (AppPreferences.getDisplayName,
   bottom), the session note (entry.sessionNote — rounded note block
   with a note icon), and the FIRST attached photo
   (entry.sessionScreenshots — 200×150 rounded block). Photo decodes
   SYNCHRONOUSLY (BitmapFactory → asImageBitmap in remember) because
   shareComposableCard captures a single frame — Coil's async painter
   would miss it. Format + date chips collapsed to one "Format ·
   Captured…" text line (declutter).
2. curioDialogContainerColor (CurioTheme) light branch: was
   lerp(surfaceContainerHigh, background, 0.72) = cream white. Now
   surfaceContainerHigh directly (theme-aware, matches pill/chip
   language). Affects every AlertDialog.

Docs: changelog FIX lines, AGENTS.md v171, this file. PUSHED (the held
v168/v169/v170 commits rode along on this push).

## Earlier completed request — Edit profile dialog restructure: Your name + Bio sections, tagline removed — commit only, NO push

User: "now in edit profile dialog, make the your name and the line under
it text ith Your name and in bold and larger text then below Bio similiar
way, remove the tagline text, and remove the automatic tagline option and
also the leave the tagline empty to use the automatic streak one that tet
too. and make the profile photo text a little bigger, and fix the page
margin and hirarcy and maybe add icons etc and dont push."

ProfileScreen.kt ProfileDialogs:
- Helper text "Your name and the line under it." → section heading
  "Your name" (titleMedium ExtraBold + Person icon); name field label
  "Display name" removed (placeholder "Your name").
- New "Bio" heading (same style + Note icon) over the tagline field —
  the tagline field IS the bio (no bio data model; the "line under the
  name" is the custom streak tagline). Its "Tagline" label removed
  (placeholder "Keep the spark going today.").
- Removed: "Use automatic tagline" TextButton + onResetTagline param +
  caller wiring; both helper texts. Empty field still falls back to the
  automatic streak line (unchanged).
- "Profile photo" label labelLarge → titleMedium ExtraBold + Image icon;
  sections 16dp apart with 8dp heading→field gaps; new EditSectionLabel
  helper.

Docs: changelog FIX line, AGENTS.md v170, this file. COMMITTED ONLY —
"dont push" (v168/v169 also unpushed).

## Earlier completed request — category picker Manage pill: floating + theme-aware + text-only; filters Show-all tick removed — commit only, NO push

User: "in category picker the manage category option there a scafhold or
strip behind the button make it proper floating, and theme aware and
remove the tick mark just text and same in filters show all topics dont
push".

SpinScreen.kt:
- Manage pill (picker sheet bottom action): was full-width
  (fillMaxWidth = the strip). Now content-sized + centered
  (.align(CenterHorizontally)) and its static surfaceContainerHigh is
  the nav-pill dynamic container
  curioFloatingNavContainerFor(currentCat.categoryBackgroundWash())
  (sheet is wash-tinted; light lifts 30%, dark = elevated dark).
  Text-only — CurioIcons.DragHandle glyph removed.
- Filters Apply / Show all topics: CurioIcons.Check tick removed —
  text only (accent pill unchanged).

Docs: changelog FIX lines, AGENTS.md v169, this file. COMMITTED ONLY —
"dont push" (v168 progress-dialog commit is also still unpushed).

## Earlier completed request — progress editor dialog: single-count top corner + remove "0/pages" and "Edit total" — commit only, NO push

User: "from the progress editor dialog remove the 0/pages option and move
it to the top corner and just show one no. and tapping it automatically
lets u edit and when u tap enter it saves and it lets u reset the number
to default too. and also remove the edit total option too and dont push
this".

CurioProgressPill.kt — CurioProgressEditorDialog reworked:
- Removed the "$value / $target $unit" line under the ring (the user's
  "0/pages") and the v149 "Edit total" chip + its inline target field.
- Top-right Row in the dialog content shows ONE number (the current
  count): tap → BasicTextField opens in place (number keyboard, IME
  Done); Enter → commitValueEdit persists immediately (set / clear on
  0); a 26dp replay icon beside it resets to default (0) + persists.
- Ring keeps just the big %; steppers/slider/Finish/Save unchanged.
- The total now comes only from the topic data or the alt-edition pill's
  initialTarget prefill (Save still persists target overrides).

Docs: changelog FIX line, AGENTS.md v168, this file. COMMITTED ONLY —
user said "dont push this".

## Earlier completed request — reveal Like/Dislike pill dynamic tint + remove the nav pill tap ripple

User: "the like and unlike button pill doesnt get the backgroud tint with
 dynamic theme fix it. and dont add the touch shado in nav bar".

1. Reveal pill container was a STATIC surfaceContainerHigh. Split
   `curioFloatingNavContainer(routePrefix)` → shared lift helper
   `curioFloatingNavContainerFor(wash)` (light: wash lifted 30% toward
   surfaceContainerHigh; dark: surfaceContainerHigh) in CurioBottomNav;
   RevealSentimentPill now receives `container =
   curioFloatingNavContainerFor(cat.categoryBackgroundWash())` so the
   capsule wears the reveal page's own dynamic tint like the nav bar.
2. "touch shado" — ask_user confirmed it's the tap RIPPLE. FloatingNavPill
   clickable now passes indication = null + a remembered
   MutableInteractionSource (no ripple on tab taps; the 6dp drop shadow
   under the bar stays — user picked ripple only).

Docs: changelog FIX lines, AGENTS.md v167, this file. Committed + pushed.
CI fix after push: `CurioAnimations.kt:70` — Springs.Calm (SpringSpec<Float>)
passed to slideInVertically (animates IntOffset). Now typed inline
`spring<IntOffset>(1f, 750f)` — same physics. (Same per-target-typing
lesson as v165.) Committed + pushed.

## Earlier completed request — calmer nav collapse (same for all pills), muted colors (Cabinet "All" not pink), dark-mode session note, calmer page openings

User: "make the nav bar collapse animation slower a little not violent and
smoother. and also chnage the bright colors to use muted colors and in
cabinet all use the default theme aware color. not pink. from the spin
shuffle option, and fix the dark mode session note text box and text color
visibility in save your take, its bright in dark mode and before doing it
do a pull" + "also same animation for all pills, and also the page opening
have become too violent did u edit it?"

git pull first (fast-forward to 5eb2330). Changes:

1. PILLS — one spring family everywhere:
   `spring(dampingRatio = 1f, stiffness = 750f)` — CRITICALLY damped
   (zero overshoot = not violent) at half Medium stiffness (slower a
   little). Applied IDENTICALLY to the nav pill bar (CurioBottomNav,
   4 typed specs), reveal Like/Dislike (TopicRevealScreen, 4) and pet
   studio bar (PetDesignerScreen, 3). No other animated pills exist
   (tour dock is static; grep for expandHorizontally found only the
   three families).

2. MUTED COLORS — new `curioActivePillFill()`/`curioActivePillInk()`
   (CurioBottomNav): light mode pulls saturation ~45% via toHsl/fromHsl
   (hue + lightness preserved — deep accents keep white ink); dark keeps
   the deep jewel tone, pastel keeps the airy twin. Reveal
   SentimentSegment got the same mute. Rail shares the helpers.

3. CABINET "ALL" FALLBACK — was colorScheme.primary (CoralBlush = the
   "pink from the spin shuffle option"). Now secondaryContainer +
   onSecondaryContainer (standard theme-aware M3 pair, muted soft warm).

4. PAGE OPENINGS — answer: NO, the NavHost screen transitions were not
   touched (tween-based since v7.17). But softened what reads violent:
   NavHost detail/pop scaleIn+scaleOut 0.88 → 0.94 (half the zoom);
   CurioDialogEntrance (v163) 0.9/380 spring → Springs.Calm (1.0/750);
   MorphEntrance non-bouncy (category grids) Deliberate (0.85/250) →
   Springs.Calm at 0.92 start; ScreenEntrance slide → Springs.Calm.
   Added CurioMotion.Springs.Calm = the pill family's exact physics.

5. SESSION NOTE (dark) — SessionNoteFloatingPill popup: dark mode swaps
   the cream paper sheet for surfaceContainerHigh + onSurface ink, and
   the OutlinedTextField gets explicit paper-paired colors (text,
   placeholder, cursor via paperControlAccent, paper borders) — M3
   defaults painted light text over the bright cream (invisible text).
   Light mode keeps the cream paper.

Docs: changelog FIX lines (4 new), AGENTS.md v166 entry, this file.
Committed + pushed. CI validates.

## Earlier completed request — CI fix: typed springs for the pill motion

CI failed on v162: SpringSpec<Float> passed where AnimationSpec<Color>
(fill/icon tint) and FiniteAnimationSpec<IntSize> (label expand/shrink)
were expected. Each pill now declares one spring per target type —
spring<Dp> (width), spring<Color> (fill + icon), spring<IntSize>
(expand/shrink), spring<Float> (fades) — same physics, so the lockstep
is unchanged. IntSize imported in all three files. Committed + pushed.

## Earlier completed request — bolder navbar tab labels

User: "use new bold fonts for navbar texts material text". The floating
pill bar's tab labels (and the rail, reveal Like/Dislike, pet studio)
were labelMedium SemiBold — now labelMedium Bold (Geom has a real Bold
face). Four copies bumped together. Docs: changelog FIX line, AGENTS.md
v164, this file. Committed + pushed.

## Earlier completed request — smoother dialog/sheet opens + quest badge icon fix

User: "more smoother open aniations of things and fix the badge icon just
the quest badge icons". ask_user clarified: badge = CLIPPED/SQUISHED;
open animations = DIALOGS & SHEETS.

1. Badge icons (CurioBadges.kt): CurioBadgeMedal drew glyphs at Bold
   (wght 700) — heavy strokes crowded the medal's inner ring and read
   squished. All three icons (earned glyph, locked silhouette, earned
   check) now render at NORMAL weight. (Glyph NAMES were fine — the font
   is the full Material Symbols TTF.)
2. Dialog/sheet opens: the raw `Dialog()` windows popped in instantly.
   New shared `CurioDialogEntrance` (CurioAnimations.kt): fade + spring
   scale (0.9/380) on the first frame. Wrapped the GalleryWall full-
   screen board (scale 1f = pure fade), GalleryWall floating quote
   editor (0.96), EntryDetail expanded mood board (1f). M3 AlertDialogs
   / ModalBottomSheets already animate — untouched.

Docs: changelog FIX lines, AGENTS.md v163, this file. Committed + pushed.

## Earlier completed request — tune the pill tab-switch motion to one spring family (v162)

User: "Record the tab-switch motion and tune the pill spring/fade until
the collapse reads perfectly smooth". No device/screen-recording possible
in this env (no Gradle, no emulator) — did a spec-level audit instead.

Audit found the real residual jank: v161 only fixed the WIDTH spring. The
fill still ran the old MediumLow spring (lagged), the icon tint ran
tween(200) and the label ran its own tween(240/160) — all out of step
with the pill width (label finished ~3x early; fill still catching up
after the pill settled).

Fix — one spring family per pill, identical params (0.9 damping, Medium
stiffness) for EVERY property:
- CurioBottomNav: `PillWidthSpring` (spring<Dp>) + `PillMotionSpring`
  (spring<Float>) used by width, fill, icon tint and label enter/exit;
  expand/shrink get the spec too (their default is damping 1.0).
- TopicRevealScreen SentimentSegment: same (Reveal*Spring vals).
- PetDesignerScreen PetStudioTab: same (Studio*Spring vals); its fill
  stays a deliberate solid snap (v156).

Removed FastOutSlowInEasing import from CurioBottomNav (unused now;
tween still used for the container fade). Committed + pushed (CI
validates — no local Gradle).

## Earlier completed request — CI compile fix + pushed EVERYTHING

User pasted CI failure: `Unresolved reference 'align'` at
PetDesignerScreen.kt:1516. Root cause: the v156 StudioFloatingToolbar pins
itself with `Modifier.align(Alignment.TopEnd)` inside a plain @Composable
function — `align` is a BoxScope extension, so it never compiled (the whole
unpushed stack was only now reaching CI). Fix: declared
`private fun BoxScope.StudioFloatingToolbar(...)` (caller renders it inside
the screen's root Box) + added the BoxScope import. The compiler reported
no other errors, so the rest of the accumulated stack is sound.

User said "fix and push all" — pushed the ENTIRE accumulated stack:
d6bda78 (pet designer rework), a8a381b (rims), f5be32c (models +
dictation), 67a1c9a (slimmer pills), 2d7d84f (remaining rims), dcbff81
(collapse + yellow fix), b8aaff7 (this fix). origin/main = b8aaff7, tree
clean. CI re-validates on this push.

## Earlier completed request — nav collapse smoothing (real fix) + Cabinet "All" yellow pill + audit

User: "the collapse animation of buttom nav pill is still bad and also why
its yello in cabinet all. fix that and do more audit".

1. Collapse: the label exit was fadeOut(tween(0)) (vaporize) + the width
   spring dragged ~1s (StiffnessMediumLow). Fixed: exit 160ms
   FastOutSlowIn glide with the shrink; stiffness MediumLow → Medium
   (damping 0.9 kept). Applied to FloatingNavPill, reveal SentimentSegment
   AND pet studio PetStudioTab (audit catch — it still ran the old
   damping-0.75 + instant-vanish recipe).
2. Cabinet yellow: active pill fell back to colorScheme.secondary (butter)
   on pages that publish no accent (Cabinet "All"). Fallback → primary
   (coral) + onPrimary ink in FloatingNavPill.
3. Audit: CurioNavigationRail had the same yellow (hard-coded secondary)
   AND never wore page accents — now resolves curioNavActiveAccent like
   the pill bar (page accent or primary fallback). Pet studio's amber is
   intentional (untouched).

Docs: changelog FIX lines, AGENTS.md v161, this file. Committed only —
user still holds the push.

## Earlier completed request — remove the remaining dark-mode hairline rims (all 5 sites)

User: "remove the same darkmode hairline that u mentioned exosist in more
elements" — the v157 follow-up.

Removed `BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))` from:
1. Tour Skip/Next dock (CurioNavHost.kt)
2. Reveal Like/Dislike pill (TopicRevealScreen.kt)
3. "Manage categories" pill (SpinScreen.kt — the extra one not in the v157 list)
4. Pet studio bar (PetDesignerScreen.kt)
5. Pet Designer floating action capsule (PetDesignerScreen.kt)

Each Surface keeps its 6dp shadow + theme fill. `BorderStroke` imports
removed from all four files; CurioNavHost also lost its now-unused
`isCurioDarkTheme` import. Intentional tinted BorderStroke uses (badges,
chips, quest medals, glass gradients) untouched. Docs: changelog FIX line,
AGENTS.md v160, this file. Committed only — user still holds the push.

## Earlier completed request — slim the nav pill + reveal Like/Dislike pill (60 → 48dp tall, lengths kept)

User: "the navbar pill height is too much keep its lengh but decrase the
heigh the widneness same in like and dislike".

- `FloatingPillHeight` (CurioBottomNav.kt): 60 → 48dp (v159 comment).
- `RevealSentimentHeight` (TopicRevealScreen.kt): 60 → 48dp with it.
- Widths unchanged (icon 60dp / expanded 128dp), icon 26dp unchanged.
- Docs: changelog FIX line, AGENTS.md v159, this file.
- Committed only — user still holds the push.

## Earlier completed request — remove Full Vosk models + dictation mic on every note/quote box — commit only, NO push

User: "remove the full models as they are laggy and crashing the app along
with my phone. and add medium model if theres more. in voice model. and the
voice bubble in save your take show it in each note and quote text box not
just in sound bite".

### Part 1 — models (done)
- Removed the 3 Full server-grade models (Full English ~1.8 GB, Gigaspeech
  English ~2.3 GB, Full Indian English ~1 GB) from `VoskModels.CATALOG`;
  dropped `Tier.FULL` (enum + rose badge tint + picker copy).
- RESEARCHED medium: alphacephei.com's catalog has NO English "medium" —
  ladder is Small ~40–60 MB / Large ~128 MB / server-grade 1–2.3 GB, so
  nothing was added (told the user).
- Startup prune (`MainActivity` → `VoskModels.pruneRemovedModels`): deletes
  installed dirs/zips whose id left the catalog, clears a stale saved
  selection; detail Transcribe button also guards `byId(modelId) != null`.

### Part 2 — dictation everywhere (done)
- NEW shared `DictationMic` (features/capture/formats/DictationMic.kt):
  owns recognizer (lazy, destroyed on dispose), RECORD_AUDIO permission
  flow, live-preview dialog; reports live-listening via `onListeningChange`.
- SoundBiteFormat refactored onto it (~330 lines of inline recognizer +
  private DictationDialog removed); dictation still counts as busy for the
  format-switch guard via a local `dictating` state.
- Mic wired into the tool dock (`trailingAction`) of EVERY note box:
  FieldNotes ×3, Marginalia journal, ReelNotes review, SoundBite note,
  GalleryWall caption (PaperLineField label row) + every quote card via
  the shared `QuoteCardEditor` (covers all formats + mood board + floating
  quote dialog). All gated on `AppPreferences.voiceToTextEnabledState`.
- Insert appends the transcript; quote cards preserve spans via
  `QuoteCardsState.setText` clamping.

### Docs
Changelog (ADD line rewritten for Large-only ladder + dictation-everywhere;
picker badge FIX line dropped the Full tier; REMOVE-style note for the Full
models; the stale "mics are gone" FIX line rewritten), AGENTS.md v158 note,
this file.

### Git state
Committed only — user still holds the push (pet designer rework d6bda78 and
rim removal a8a381b are also unpushed). CI validates on push.

## Earlier completed request — remove dark-mode hairline rims (floating nav bar + detail quick-fact box) — commit only, NO push

- `CurioFloatingNavBar` (CurioBottomNav.kt): removed the v149 dark-mode
  `BorderStroke(1.dp, White@10%)` capsule rim. `BorderStroke` import removed.
- `QuickFactCard` (EntryDetailScreen.kt): removed the v115 dark-mode
  `Modifier.border(1.dp, ink@18%)` plate rim. `foundation.border` import removed.
- Same rim still exists on the tour dock, reveal Like/Dislike pill, pet
  studio bar + floating action capsule (offered to the user).

## Earlier completed request — Pet Designer layout rework (compact nav, floating top actions, tear scrolls away) — commit only, NO push

User-confirmed: bottom nav = compact centered capsule; actions = floating
pill pinned while scrolling; tear = banner becomes the first scrollable item.

1. `PetStudioBottomNav`: dropped `fillMaxWidth()` — content-sized capsule
   centered at the bottom.
2. `EditorToolbar` → `StudioFloatingToolbar`: one rounded capsule pinned
   TopEnd below the status bar (Save pill + dirty dot, Undo/Redo/Reset/
   Share/Import circles; `ToolbarIcon` gained a `size` param). Toasts
   auto-clear after 3s.
3. Torn banner moved in-flow as the list's first item; overlay Box +
   stickyHeader + `SettingsHeroTotalHeight` top padding gone.

## Earlier completed request — light-mode nav capsule tint + smoother pill animations

- `curioFloatingNavContainer` light-mode lift 0.55 → 0.30 lerp so the page
  tint shows through the capsule (dark unchanged).
- Smoother pills (nav bar + reveal Like/Dislike): width spring damping 0.9,
  active fill fades via animateColorAsState synced to the spring, icon tint
  crossfades (200ms FastOutSlowIn), label fade 240ms FastOutSlowIn.

## Earlier completed request — reveal Like/Dislike pill matches the bigger 60dp nav-bar pill

- `RevealSentimentIconWidth/ExpandedWidth/Height` 52/96/48 → 60/128/60dp,
  segment icon 20 → 26dp, inner Row padding/spacing 7/6dp — identical to the
  nav bar pills.

## Earlier completed request — revert the nav-bar → sentiment-pill shared morph, keep the bigger bottom pill

Commit `55ebc74` added bigger pills (60/128/60 + 26dp) + a shared-element
morph (SentimentSharedElementKey / NavPillBoundsTransform). Reverted the
morph, kept the size. Code files match the pre-morph parent except size;
docs updated (AGENTS.md v153, changelog FIX line).

## Earlier completed request — workflow/instruction changes (commit only, no push)

Added to root AGENTS.md: git pull first, ask before deleting/replacing
anything, text/docs changes commit but push only with the next real change.
