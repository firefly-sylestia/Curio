# Prompt.md — Request log

## Current request — COMPLETED: floating pill nav bar (phones)

The user: "lets add a floating nav bar in pill size with animation like
first it will just be icons and when that page is active that page gets
its text shown, and switching to other page gives it a smooth collapse
and expand of other, and the active indicator covers the active pill
not just the icon, and change it properly with dynamic theme and in
dark mode as well."

Clarified via ask_user: **Replace it** (no Settings toggle — the floating
pill is the only phone bottom nav) and **Keep the side rail** on wide
windows (tablets/landscape keep `CurioNavigationRail`).

### What shipped (`ui/components/CurioBottomNav.kt` + `CurioNavHost.kt`)
1. **`CurioFloatingNavBar` replaces `CurioBottomBar`** (old M3
   `NavigationBar` composable deleted): a floating 50-radius capsule
   pinned bottom-center above the gesture inset.
2. **Icons-only at rest; active pill expands** — each tab is a 48dp
   pill; the active one springs to 96dp (`animateDpAsState` spring,
   damping 0.75 / StiffnessMediumLow) and its label slides out
   (`AnimatedVisibility` expandHorizontally + fadeIn 160ms); the
   previously active pill collapses the same way (shrinkHorizontally +
   fadeOut 120ms) — the smooth collapse/expand morph.
3. **Indicator covers the whole pill** — the active fill is a capsule
   behind icon AND label (not just the icon).
4. **Theme/dark-mode correct** — pure `colorScheme` tokens:
   surfaceContainerHigh bar + 6dp shadow, secondaryContainer
   indicator, onSecondaryContainer ink, onSurfaceVariant inactive
   icons. Adapts to Curio / AMOLED / Material (dynamic) + dark.
5. **Geometry preserved** — the slot is 80dp + nav-bar inset (verified
   against M3's `NavigationBar` = `windowInsetsPadding +
   defaultMinSize(80)`), so Scaffold innerPadding and the Reveal 80dp
   placeholder are unchanged; the pill floats 12dp above the inset.
   Fixed expanded width (48↔96dp) keeps the bar's total width constant
   so the morph is stable. `CurioNavTint` wash now applies to the rail
   only (the pill is token-driven).

### Validation
`git diff --check` clean; brace balance checked; unused-import sweep
clean (setValue false-positive — needed by `by mutableStateOf`
delegates in CurioNavTint); no remaining `CurioBottomBar` references
(grep); M3 NavigationBar footprint verified from the 1.5.0-alpha20
source jar. No Gradle locally (env rule) — CI validates on push.

## Previous request — COMPLETED: tour skip on Spin no longer breaks Home; drawer name lines; pet poof teleport + chameleon + auto-flow

The user: "when the tour gets skipped on spin page the navigation to
home doesnt work after that, so fix it. and in drawer remove that spin
it explore it capture it line and instead use that space with the same
font size in that line for the longer name, show the first name in the
current position and then 2nd middle 3rd name in bottom, and then fix
the pet behaviour, when it randomly teleports give it the puff
animation not instant, and make its idle auto game mode to be limited
to only hide and seek and camoflage and before it camoflage make it
puff and teleport to somewhere."

Clarified (2nd message): the bug is ONLY on the Spin page — the Skip/Next
dock covers the bottom bar during the tour, the dock is gone after skip,
but the Home tab still appears dead. "look at the code itself and figure
out whats wrong, add this in your instructions."

### 1 — Tour skip on Spin → Home tab dead (root cause + fix)
Root cause (verified against Navigation 2.9.8's `NavControllerImpl`
source, not guessed): `navigateToTab` calls
`navigate(route) { popUpTo(HOME) { saveState = true }; launchSingleTop
= true; restoreState = true }`. The tour previously PUSHED the Spin tab
with a plain `navigate("spin") { launchSingleTop = true }` (no
popUpTo), so HOME was never entered into the controller's `backStackMap`.
The first Home-tab tap after the skip then popped Spin with
`saveState=true` — and `executePopOperations` maps the popped stack
UNDER THE popUpTo DESTINATION'S KEY (HOME) when HOME isn't already
mapped — and `restoreState = true` immediately RESTORED that saved
[spin] stack, landing back on Spin (the tap looks dead). In the normal
flow the FIRST `navigateToTab(SPIN)` plants a NULL mapping for HOME
(its no-op popUpTo saveState), so the later Home restore is a no-op —
which is exactly why the bug only reproduced on the tour-created Spin
stack (and why "any other screen" was fine: every other tour stop is a
push or has `below != HOME`, so `navigateToTab` runs the explicit
`popBackStack(HOME)` first).

Fix: all THREE tour navigation sites now use `navigateToQuestRoute`
(tab routes → `navigateToTab`, pushes stay plain):
- `CurioNavHost.advanceTourAndNavigate`
- `SpinScreen` `onSpinClick` (consumeTap("spin"))
- `HomeScreen` quest-card (consumeTap("quest"))

This makes the tour's Spin/Cabinet steps land with the same
popUpTo-saveState contract as real tab switches, so the later Home tap
restores nothing and navigates. RULE recorded in app/AGENTS.md (v123):
never plain-`navigate` a TAB route from the tour.

### 2 — Drawer greeting
The "Spin it. Explore it. Capture it." tagline is removed. The first
name stays in the greeting position ("Hi First"), and the remaining
name parts (middle, last) render one per line at the tagline's old size
(`bodyMedium`, alpha 0.78) in the tagline's spot — a long name reads on
its own lines instead of one ellipsized greeting.

### 3 — Pet behavior
- **Random teleports poof:** `walkTo`'s `LONG_JUMP_FRACTION` branch now
  `burstPoof(pos)`, `delay(160)`, teleports, then `burstPoof(pos)` at
  the target — no more instant snap (squish landing kept).
- **Idle auto-flow limited:** the auto-flow scheduler only picks
  `PetGame.HIDE_SEEK` / `PetGame.CHAMELEON`; star-catch (a 10s round)
  stays reachable via game mode's cycle + manual taps, never the
  auto-flow.
- **Chameleon:** the pet now POOFS and teleports to a random on-screen
  spot BEFORE fading to its ghost outline (was: fade in place).

### Validation
`git diff --check` clean; brace balance checked; PetGame enum
(HIDE_SEEK/CHAMELEON/SPARK) verified; navigateToQuestRoute imports
added to SpinScreen + HomeScreen (CurioNavHost same-package); no other
plain tab pushes in the app (grep sweep — the three tour sites were the
only offenders). No Gradle locally (env rule) — CI validates on push.

## Previous request — COMPLETED: drawer avatar bigger + lifted + auto-shrinking name

All of this session's work is done, committed and pushed.

The user: "also in drawer increase the avatar size a little bit and move
it a little above and when wi add a longer name the namame gets cut so
do something about it with maybe compact name adjustment it gets small
or something."

### What shipped (`HomeScreen.kt` drawer hero)
1. **Avatar 56 → 64dp** (the initial-letter fallback stepped up
   `titleLarge` → `headlineSmall` to match the bigger circle).
2. **Row lifted a touch** — the greeting row's bottom padding 28 → 40dp
   (still well inside the 186dp hero; the tear stays put).
3. **Long names no longer cut** — the "Hi name" line's style steps down
   by length: headlineMedium ≤ 16 chars → titleLarge ≤ 26 → titleMedium
   beyond, with the single-line Ellipsis as the last resort.

### CI fix — TextAutoSize does NOT resolve on this classpath
First attempt used `TextAutoSize.StepBased`; CI failed with "Unresolved
reference 'TextAutoSize'" at the import AND the call site (both debug
and release compile). `androidx.compose.ui.text.TextAutoSize` is not
resolvable on this project's Compose classpath despite the 2026.05.01
BOM. Replaced with the manual length-based font step-down above (no new
API); verified brace balance + grep (no residual TextAutoSize refs
besides the explanatory comment).

### Validation
Brace balance depth 0; `git diff --check` clean; hero-geometry check
(64dp avatar + 40dp lift fits the 186dp hero); no Gradle locally (env
rule) — CI validates compile on push.

## Previous request — COMPLETED: pet games reworked + dialogue actually spoken (`c448645`, + `27bb64b` cycle, + `9273c5c` bubble fix)

All of this session's work is done, committed and pushed.

The user: "Also according to the dialogs many things are not like i havent
seen it speak the dialogs, and also when it plays a game why it gets
overide with another idle ation. let it complete the game, like if its
hiding let it hide and stays in place for upto 5 seconds and then it
poofs back ith its dialog, same with hide and seek and the star catch.
and instead of putting it back into home when i tap and hold it make it
enter game mode, and it stays on game mode and finishes after one game
is played. and only touch interactions ill happen but it will remeber to
play the game, and the star game make it play for 5 secs with contant
stars popping u randomly and it doesnt go up to the star on itself make
the star fall slowly from above and when user taps the star then the pet
goes and cathes it or drags the pet to catch it and when its done sho
the score. and how about let it run for 10 sec. and when user doesnt tap
it for 2 or 3 sec then let it roam around idle and on its on and let the
automatic game flow on its own too with random time and random games,"

Clarified via ask_user: game mode → "Pet waits for your touch first"
(long-press arms it; the next tap or drag starts ONE game); star score →
speech bubble after the round; hide-and-seek → the pet poofs and goes to
a corner with just a little visible, tap to find, sad face +
disappointment on fail, poof animations both ways; chatter → Chatty
(every ~20-40s idle).

### What shipped (all in `CurioFloatingPet.kt`)
1. **Chatter** — a new effect speaks `CurioPet.lineFor(mood, …)` every
   ~20-40s idle (EXCITED/PROUD excluded — their events own them).
2. **Games complete** — the three games became suspend fns
   (`playHideSeek` / `playChameleon` / `playStarGame`) dispatched at the
   top of the wander loop; `gameActive` gates the mood loop, typing
   reaction, idle/time custom actions, auto-nap and chatter, so nothing
   can override a round. The old probabilistic inline game blocks + the
   per-game cooldown state/constants were deleted.
3. **Game mode** — long-press no longer sends the pet home (drag it onto
   its flower bed instead): it arms game mode (pet stays put, autonomy
   paused), the next tap or drag queues `gameRequest`, ONE random game
   plays, then game mode ends.
4. **Hide-and-seek** — poof out where it stands, teleport to a random
   corner (a tappable sliver), up to 5s to be found; found = victory;
   missed = poof back + sad face (`PetFace(EyeStyle.CLOSED,
   MouthStyle.O)` via `reactionFace`) + `missedMeLine()`. Chameleon's
   find window also 5s; both teleports burst the new `PoofOverlay` at
   the recorded position.
5. **Star catch** — a 10s round (`starRound`/`stars`/`starScore`/
   `starCatchTarget`): stars spawn every ~500-850ms near the top and
   fall slowly; the pet never chases on its own — tap a star and it
   dashes over to catch it, or drag the pet onto a falling star; the
   score is spoken in a bubble at the end.
6. **Idle roam + auto flow** — wander beat shortened 2.8-7s → 2-3.2s, and
   a scheduler effect requests a RANDOM game at random 20-50s intervals
   (scaled by the game-frequency setting, honoring GAME_MIN_SPACING_MS).

### Follow-up — game mode cycles the games (`c448645`)
User: "Make game mode pick the game by a cycle instead of random, so all
three get played evenly." Game mode (tap or drag after a long-press)
now picks via `nextGameModeGame()` — a `cycleGameIndex` that walks
HIDE_SEEK → CHAMELEON → SPARK → … The auto-flow scheduler keeps its
random picks ("random time and random games" stays true); only game
mode cycles.

### Validation
Brace balance depth 0; dead-ref sweep clean (sparkTarget/sparkWon/
sparkKey/lastHideSeekAt/lastChameleonAt/lastSparkAt + the three per-game
cooldown constants removed); `git diff --check` clean; `.entries`
confirmed against Kotlin 2.3.21; no Gradle locally (env rule) — CI
validates compile on push.

## Previous request — COMPLETED: pet dialogue fully ported to the canonical dialog doc (`6f7c9cb`)

All of this session's work is done, committed and pushed.

The user: "i added the new pet dialogs and also more dialogs, imtegrate
and replace all the current dialogs with those. dont miss any." — with
the rewritten `docs/pet-dialogs.md` attached (same content as the
working-tree file). Source of truth: every pool in the doc, ported 1:1
into `CurioPet.kt`, `CurioPetBrain.kt`, `TourController.kt` (pool names,
order, and the `__LANE__` / `$lane` / `$savedLane` / `$streak` / `$count` /
`$topic` / `$level` / `$saves` placeholders kept verbatim).

### Sections ported (none missed)
- **§1 events** — SPIN_LANDED (lane + generic), REVEAL_TAPPED, REVEAL_AUTO,
  EXPLORE, SAVE (lane + warm + default), TOUCH, PLAY, LEVEL_UP, QUEST_COMPLETE,
  sassy bursts — FIRST_EVO pools + BABY + FINAL (babyEventLine /
  matureEventLine).
- **§2 streaks** (streakMilestoneLine / babyStreakLine / matureStreakLine),
  **§3 evolution** (path-flavored ceremony + no-path + final + BABY/FINAL).
- **§4 moods** — PROUD (now live-level inline), EXCITED, HAPPY
  (morning/afternoon/evening/night + warm twins), CURIOUS (`__LANE__` +
  new fallback "Want to find something new today?"), FOCUSED, BOUNCY, SHY,
  GRUMPY, PLAYFUL, SLEEPY — first/baby/mature pools all replaced.
- **§5 greetings** (morningGreeting + welcome-back day/3-day/week pools,
  all voices), **§6 touch** (CLOSE/FRIEND/stranger/tier2/tier1 + baby/mature).
- **§7 games** — spinCheer, playInitiation, landmarkLine, jigLine,
  dizzyLine, drawerLine, peekLine, chameleonLine, sparkLine + the six
  interactive moments (findMe/foundMe/caughtIt/gotAway/peekWin/missedMe).
- **§8 factLine** — hatch day (5), active streak (5), weekly keepsakes (5),
  season (2 each), weekday/weekend (2 each), last-saved-topic (5).
- **§9 brain** — openings per trait, bodies per mood (now multi-option
  pools via `CurioPet.pickLine` — anti-repeat applies), coined catchphrases
  = the doc's 8 lines mapped to the coin conditions (`laneLabel` var
  removed).
- **§10 tour** — all 8 steps' dialogue + nextHint.
- **§11 matureRoutineLine** — all 36 routine lines.
- **§12 BABY VOICE EXPANSION (Curie-isms)** — new pools
  `babySaveLines` / `babyTouchLines` / `babyLevelUpLines` / `babyEvolveLines` /
  `babyExploreLines` / `babyDiscoveryLines` / `babyMishapLines` feed the
  matching baby events; `babyCurieLines` (sounds + tiny phrases + rare
  silly, ~130 lines) rides EVERY baby pick; excited/happy/curious/
  sleepy/shy/grumpy/playful mood pools absorbed their doc categories.

Also removed the dead `happyLines` pool (old text, unreferenced).
PetDesign.kt's custom-reaction preview defaults were left untouched (not
in the doc's scope).

### Validation
Brace balance depth 0 on all three files (python brace check); grep
sweeps confirm every old pool phrase is gone and distinctive lines from
every doc section are present; `git diff --check` clean; no Gradle
locally (env rule) — CI validates compile on push.

### Follow-up — tour dock no longer covered by the guide bubble
User (before the port was pushed): "dont push it the cabinet dialog is
covering the next button in tour so fix that first then go ahead and
push." Root cause: the tour's Cabinet stop dropped the speech bubble
onto the bottom dock — the `grid` landmark wraps a `fillMaxSize()`
grid, so the bubble's "above" placement never fit and the "below"
fallback clamped to the very bottom (over Next); an empty Cabinet was
worse (no landmark at all → the bubble floated over the pet wherever
the reveal step had parked it, near the bottom). Fixes in
`CurioFloatingPet.kt`: new `TOUR_DOCK_BAND` (96dp + nav inset); the
bubble's top is capped above it, landmarks that span the screen (the
Cabinet grid) anchor the bubble just above the landmark's CENTER
(upper-middle — follow-up fix: the original pin-to-top floated over
the hero, "too above lol"), the pet's tour-walk target is floored
above the band, a landmark-less stop parks the pet top-center, and
the no-landmark bubble fallback is clamped on-screen/above the dock.
`CabinetScreen.kt` now registers the `grid` landmark on the empty
branch too, so the tour always has an anchor. Committed and pushed
together with the port.

## Previous request — COMPLETED: Home avatar pill + drawer sections + Support update link (`a3a9f94`)

All of this session's work is done, committed and pushed.

The user: "show the avatr in home screen profile icon, and in drawer make
the profile avatar a little bigger and also the text. make the topic
history manage category and topic browser together with inline collapse
under Your Curiosity, and merge Support and diagnostics and replay into a
section dont know hat to call give me suggestion."

Clarified via ask_user: section name → the user picked **About** (and
added: "also in support and disnostics page add the update link to open
the update page"); collapse default → **both collapsed**.

### 1 — Home top-bar profile pill shows the avatar
`TopBarPill` gained an optional `avatarPath`: the profile pill on the
Home sticky bar shows the avatar photo (the Surface clips it to the
circle; the animated `rim` border ring draws on top so the frosted
scroll morph still reads) and falls back to the Person glyph. Fresh pref
read per composition, same as the drawer.

### 2 — Drawer avatar + text bigger
Avatar 48 → 56dp; CURIO labelSmall → labelMedium; "Hi name"
headlineSmall → headlineMedium; the tagline bodySmall → bodyMedium.

### 3 — Drawer collapsible sections (both collapsed by default)
- **"Your Curiosity"** (AutoAwesome header) → Topic History, Manage
  Categories, Browse Topics.
- **"About"** (Info header) → Support & diagnostics, Replay intro.
New `DrawerSectionHeader` composable (leading icon chip + ▼ collapsed /
▲ open chevron — the filter-sheet convention); state via
`rememberSaveable` so rotation keeps it.

### 4 — Support & diagnostics update link
An "Updates" row (Download icon) at the top of the page's Updates card
opens the dedicated Updates sub-page (Settings → Updates); the v116
de-dupe stays intact — exactly one link, no duplicate header.

### Validation
`git diff --check` clean; imports added (`border`, `rememberSaveable`)
and verified used; no leftover glyph-only pill path; no Gradle locally
(env rule) — CI validates compile on push. Pushed to `main`.

## Previous request — COMPLETED: profile avatar crop editor + auto center-square crop + Edit profile dialog redesign

All of this session's work is done, committed and pushed (`0f846cd`).

The user: "the edit profile dialog is boring also i cant crop images and
portraight images get squished so give cropped fucntion and auto crop
from middle by default".

### 1 — No more squished portraits (auto center-square crop)
The old picker COPIED the raw bytes — a tall portrait photo got
stretched into the square avatar. Now `ProfileScreen` DECODES the pick
EXIF-correctly (`decodeAvatarSource`: ImageDecoder 28+ bounded to
2048px with its own EXIF pass disabled, BitmapFactory sample 26-27; the
framework `ExifInterface` rotation is applied manually so both API
paths agree) and saves a **CENTER-SQUARE crop from the middle**
(`centerSquareCrop` → `scaleToMax` 512px) as the avatar. The editable
SOURCE is saved beside it as `profile_avatar_src_*.png` so the crop
editor can re-frame the ORIGINAL photo (not the cropped square).

### 2 — Manual crop editor (new `AvatarCropDialog.kt`)
No crop library (project is dependency-light): a fixed SQUARE crop
window with the photo panning and pinching behind it (drag to move,
pinch to zoom — never smaller than cover-fit, up to 5×, Reset returns
to the auto center crop), rule-of-thirds grid + corner brackets in the
dialog accent. Apply hands back the exact source-pixel `IntRect`
(`currentCropRect`, clamped before use) which `saveAvatar` re-crops and
re-saves; Cancel discards. The ImageBitmap is hoisted out of the Canvas
draw block (`remember(bitmap) { bitmap.asImageBitmap() }`) so gestures
don't allocate per frame. The crop dialog is composed AFTER the edit
dialog so its window stacks on top; closing the edit dialog also clears
`cropSource` (no stray crop window). `loadAvatarSource` falls back to
the current square avatar for pre-v116 avatars (no source was kept).

### 3 — Edit profile dialog redesigned (no longer boring)
The 64dp preview grew to **84dp with an accent crop/photo badge** on the
corner (tap the avatar to adjust when set, else pick); the flat stock
TextButtons became the app's **pill actions** (`DialogPillAction` —
50% radius: accent Add/Change photo, Adjust, destructive Remove); the
caption now explains the auto square crop ("Square-cropped from the
middle — tap to adjust.").

### Follow-up — dark-mode dialogs match the settings option-card glass (`aa1fbb0`)
User: "make the dark mode dialog black background follow the theme style,
make it be like settings option background". The dark branch of
`curioDialogContainerColor` (CurioTheme.kt) was a flat grey slab
(`lerp(surfaceContainerHigh, background, 0.55f)` ≈ 0xFF0D0D0D); it now
uses the EXACT `CurioSettingsCard` construction —
`lerp(surfaceContainerLow, tintLift, 0.30f)` with the dark tint lift
`lerp(Color.Black, curioRoseInk(), 0.20f)` (neutral rose — a dialog
floats over any page, so no lane accent) — so every AlertDialog (edit
profile, crop, two-step, …) reads as the same near-black option-card
glass as the Settings cards. Light mode untouched. AGENTS.md v116 bullet
+ changelog updated.

### Follow-up — Edit profile dialog declutter + crop-before-apply (`d58cbc0`)
User: "the proflie editing dialog is bad the adjust button shoul not be
there, tapping the pfp does thee same neither the crop icoon. and when i
add the photo it should show the crop dialog before applying and its
buttons are bad".

1. **Crop before apply:** the pick no longer saves immediately —
   `avatarPicker` decodes the source into `cropSource` (the crop dialog
   opens on top of the edit dialog) and only Apply saves
   (`saveAvatar(src, rect)`); Cancel discards the pick. The now-dead
   `openCropEditor` + `loadAvatarSource` re-crop path (and its
   `onCropAvatar` param) were deleted.
2. **Dialog declutter:** the **Adjust pill, the avatar corner
   crop/photo badge and tap-avatar-to-crop are all gone** — the 84dp
   avatar is a static preview, and the pill row is just accent
   Change/Add photo + destructive Remove. Captions updated ("Square
   photo — Change photo to re-crop." / "Pick a photo — you can crop it
   to a square before saving.").
3. **Crop dialog buttons:** Cancel is now a calm surface pill matched
   to the accent Apply pill (was a flat TextButton next to a lone
   filled pill) — the app's pill dialog language. Unused
   `TextButton` / `curioDialogActionButtonColors` imports removed from
   AvatarCropDialog.kt.

### Validation
`git diff --check` clean; grep sweep confirms no leftover
`onCropAvatar` / `openCropEditor` / `loadAvatarSource` / badge refs; no
Gradle locally (env rule) — CI validates compile on push. Pushed to
`main`.

## Follow-ups / notes
- The update-checker toggle ships OFF by default (opt-in) — once settled,
  remove the toggle and hardcode the winner (experiment rule).
- Auto-backup reuses ONE persisted document URI; "pick a folder" is
  implemented as "pick a document once" — revisit if a true folder flow is
  wanted.
- The launcher icon: if the user later wants the card BIGGER, the inset can
  drop to ~24dp (card ≈ 48×51dp, frame corners just inside the 66dp circle)
  — but 28dp is the safe default across circle/squircle masks.
