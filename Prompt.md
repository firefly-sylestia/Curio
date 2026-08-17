# Prompt.md — Request log

## Current request — reveal Like/Dislike pill dynamic tint + remove the nav pill tap ripple

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
