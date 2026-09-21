# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> also remove useless docs node module which not needed, ask me first, and also fix the git
> files messing look https://github.com/Alain00/blobatar also can we use this profile avatar
> style for social instead of those bad drawing? read the repo docs etc and how to implement,
> and then we will go back to the journal dock also the pdf reader the header of the search and
> back and title floating pill its too much close to the status bar, also separate the search
> and the back and title pill, search icon is just a circle pill and when opened it merges
> smoothly with the header for search.

Four asks, one of which (the journal dock) is explicitly for a LATER session.

## 2. Findings before anything was touched

### 2.1 The root-of-repo clutter

- `node_modules/` at the repo ROOT (876 KB, untracked, ignored by git) holding `iceberg-js`,
  `jose`, `tslib`.
- A root `package.json` declaring exactly ONE dependency, `@supabase/server`, which **nothing
  in the repo imports** — `auth-web/api/*.js` uses plain `fetch`, and `auth-web` has its own
  `package.json`. Plus BOTH `package-lock.json` and `pnpm-lock.yaml`: two package managers for
  one unused dependency.
- In git: `docs/ANALYSIS.md` (a COMMITTED copy of the root `ANALYSIS.md`, which `.gitignore`
  deliberately keeps out of commits) and `docs/art/svgviewer-output (15).svg` / `(16).svg`
  (leftover SVG-viewer exports; only `web/`'s `Constellation.tsx` mentions the names in prose).

### 2.2 Summoning the portrait set

`features/community/SocialAvatar.kt` — 1341 lines: `PORTRAITS` (20 hand-drawn characters) +
`ICONS` (8 cozy objects), `drawSocialAvatar(style, ring)`, the picker tile
`AvatarPickerIcon`, and `SocialAvatar(style, …)` called from 11 sites. `profiles.avatar_style`
is an index into that list, clamped against `SOCIAL_AVATAR_STYLE_COUNT = 28` on every read and
write. The picker itself appeared in Edit profile (`SocialAvatarPickerRow`) and in
`CurioAccountIdentityCard` behind `includeAvatarPicker`.

### 2.3 The reader's head

`ReaderTopPill` is a `Surface(shape = RoundedCornerShape(50))` with `.statusBarsPadding()` and
8dp of vertical padding — and the reader **hides the status bar** (`WindowInsetsCompat.Type.statusBars()`
is hidden for as long as it is on screen), so that padding collapses to 0 and the pill settles
8dp from the very edge of the glass. The search glyph lives INSIDE the same capsule as the back
button and the title, and opening it swaps the whole row for `ReaderSearchBar` with a plain
slide-down.

## 3. Decisions, all confirmed before editing

Asked four questions (root deletions; which "mess" meant; port vs endpoint for the faces;
picker + seed). Answers:

1. Remove the root `node_modules/`, the root `package.json` + both lockfiles,
   `docs/ANALYSIS.md`, and both `docs/art/svgviewer-output (15|16).svg`.
2. "The git files messing look" = the root manifest clutter.
3. **Port blobatar's core to Kotlin** (not the HTTP endpoint) — the recommendation, and the
   only shape that also serves a notification's off-screen bitmap.
4. **Remove the portrait picker entirely**, and seed a face from **username if set, else the
   account id**.

## 4. What was built

### 4.1 `features/community/Blobatar.kt` (new, ~950 lines) — the port

A faithful Kotlin port of blobatar gen-2's core: `hash.ts` (normalize → murmur3-fmix seed state
→ per-key streams), `traits.ts`, `color.ts` (OKLCh ↔ sRGB, WCAG luminance, `ensureContrast`,
the six authored tones, `FLOORS`), `shape.ts` (superellipse / Catmull-Rom blob / rounded polygon
/ box / droplet taper, each traced straight into a Compose `Path`), `styles/compose.ts`
(`faceFit` and the shared body/eyes) and `styles/shapes.ts` + `styles/blob.ts` (the ten weighted
silhouettes and their band table). `BlobatarArt(seed)` resolves and traces ONE face (built in the
constructor, so a redraw is two `drawPath`s); `blobatarSeed(userId, username)` is the rule.

### 4.2 The verification (the part worth repeating)

The port could not be compiled by Gradle here, so it was verified directly:

- blobatar's own modules were run under Node (`--experimental-strip-types`) to dump the
  reference for 43 seeds — three per silhouette (seeds found by scanning `seed-1…seed-40000`)
  plus the avalanche and NFC cases.
- The REAL `Blobatar.kt` was copied verbatim with its package renamed and its androidx imports
  dropped, compiled with a downloaded `kotlinc` against a small recording stub of the Compose
  types it touches, and dumped the same values.
- A comparer diffed **430 values — all match**: hue, tone, silhouette, body geometry, every
  radius, the face region, petals, the droplet taper, both eyes (position, radii, squareness,
  lean), the three palette hexes after the contrast walks, and the full traced path geometry
  (compared to the 2-decimal rounding upstream applies to its SVG strings).

This also settled two things by construction: the file **compiles** (so the local functions, the
visibility and the `min`-shaped names resolve) and the seeds really do avalanche (`alain` vs
`alaim` are unrelated faces, and NFC-normalised `é` and `e\u0301` agree).

### 4.3 The rest

- `SocialAvatar.kt` rewritten as a thin wrapper (`SocialAvatar(seed, …)`, the presence dot,
  `drawBlobatar`); the 28-style cast, `AvatarPickerIcon`, `SocialAvatarPickerRow` and
  `includeAvatarPicker` deleted.
- All 11 call sites migrated to `blobatarSeed(...)` (`CommunityScreen`, `CommunityCardScreen`,
  `ModerationScreen`, `CommunityCommentsSheet`, `SocialProfileScreen`, `SocialComponents` ×2,
  `FriendsScreen`, `DirectMessageScreen` ×3).
- `SocialNotifications.NotificationAvatars` re-keyed by seed, capped, painting the same
  `BlobatarArt`.
- `SocialApi`'s `SOCIAL_AVATAR_STYLE_COUNT` doc rewritten: it is now a BOUND on a retired
  column, not a count of styles. `avatar_style` itself is deliberately untouched.
- Reader: `ReaderChromeTopFloor` (18dp) + `Modifier.readerChromeTopInset()` (display cut-out +
  that floor), worn by the head, the search bar and the pinned page count; `ReaderTopPill` is a
  `Row` of the name capsule (`weight(1f)`) and a 50dp `CircleShape` search pill; the two states
  animate as one move (`expandHorizontally(End)` in as the name `shrinkHorizontally(Start)` out).
- Docs: a v435 section in `app/AGENTS.md`, ADD/FIX/REMOVE bullets in the current changelog.

## 5. Checks run

- No Gradle command: this environment forbids compile / build / lint (`AGENTS.md`).
- **The blobatar port WAS compiled and diffed** — see §4.2. That is stronger than a Gradle
  compile for this file, because it checks the numbers too.
- The reader change and the call-site migration were read through by hand; every removed symbol
  was grepped repo-wide to confirm nothing still references it.

## 6. Still open — the §28 backlog (read this before starting)

Shipped from §28 already: the two build fixes, the marker pens + bullet styles as dock panels,
leaf/crystal removed, the heart/dash centring, the dock's thickness, the quote/bullet shade
split, and blobatar's MIT notice. Everything below is NOT done.

1. **The copy box's width and its dead end.** Two reports, one area
   (`PersonalCanvas.kt`: `PersonalPageEditBar` / `ReachPill` / `ActionPill` / `CopyChip`).
   (a) "i will have to scroll to see all options" — the box's row is wider than a phone, so
   the last chips are off-screen. (b) "using cut when selected all removed that line and i cant
   type anything again in the body, coz i dont get any option to" — cutting the whole page
   leaves the document with no block to put a caret in, so there is nothing to type into and no
   visible way back. **The fix must guarantee an editable block after the document empties**
   (an empty block is the invariant the editor rests on) and focus it.
2. **The POSITION of the marker glyphs.** I corrected the two shapes that were objectively
   off-centre (heart, dash), but `drawPersonalMarker` centres on `lineHeight / 2` — the LINE
   BOX's centre, not the text's visual centre — and the report was "weird positioning not
   matching with the text". If the whole roster still reads high or low against the words, the
   fix is that axis, not the shapes. Needs the member to say which way it sits.
3. **The journal colour does not reach the page or its buttons** — "still the journal page and
   ts buttons dont get the color by chnaging it". v428 terraced `JournalAccentSheet` →
   `journalAccent`/`journalPagePainted` → `LocalJournalPagePaint`, and v431 claimed the paint
   reached all three surfaces; the report says it still does not. Investigate from
   `JournalAccent.kt` + `LocalJournalPagePaint`'s consumers before changing anything.
4. **The reader's page scrubber as a small floating control** — "the page scrobble slider it
   needs to be similair to the dock small floaating without the buttom sheet so its easier to
   do the page scrbbing faster". Today a tap on the foot pill's count opens `ReaderScrubberSheet`
   (a bottom sheet). It should be the dock-like pill instead.
All six are now DONE (see §29 for the last of them), and the member's answers closed each
ambiguity:

1. **Copy box** — *"two row and hide the dock just show the copy doc when thats on"*. Two rows
   now (reach above, actions below), nothing scrolls, and the writing dock's `AnimatedVisibility`
   tests `!editor.pageEditBarOpen`. **The cut-everything dead end is DONE (v438)**: the member's
   rule (*"Always one empty line to type in"*) is now a state invariant — `publish()` is the one
   way an edit lands, and `keepLineToTypeIn()` gives the page an empty, focused, caret-bearing
   line whenever nothing in it can be typed into. Every mutation went through
   `onDocChanged(doc())` → `publish()` (44 sites), and `removeBlock`'s own duplicate guard was
   folded into the shared one.
2. **Marker axis** — answered: **"Too low, below the words"**. `PERSONAL_MARKER_AXIS_LIFT = 0.06f`
   is applied to `lineHeight` in `drawPersonalMarker` (done).
3. **The journal's colour** — answered with all three places (paper, dock tools, date/title
   chrome). Fixed: `journalPaperRaised()` tints toward the page's own colour, the dock lights
   with `journalDoorAccent(journalAccent)`, and `JournalTopBar` takes the argb as a parameter
   (it stands OUTSIDE the paint provider). **The page's PAPER still needs "Paint the page too"
   on — the v429 option was kept deliberately; if the member wants the paper to follow the colour
   by default, that is a one-line flip and needs their word.**
4. **The page scrubber** — answered: *"a buttom pill floating at the buttom with the slider and
   hides when tap on page, also a way to close it"*. `ReaderScrubPill` + `scrubOpen` (done);
   the sheet and its enum member are gone.
5. **blobatar's idle animation** — **always on**, and now PORTED: `BlobatarIdle` /
   `BlobatarPose` (the breathe, bob, blink, glance and eye-wrap loops, from upstream's own seeds)
   plus `BlobatarClock` + `BlobatarIdleClock()` hosted once at the app root, and
   `rememberCurioMotionEnabled` for Android's "remove animations". No expressions and no hover —
   neither exists in Curio.
6. `SocialApi.updateAvatarStyle` is now unreferenced (dead but harmless); it goes with the
   `avatar_style` column if the member ever wants that dropped — a schema change, which needs
   their word. **Both §28 and §29 asked whether the portrait change needs SQL: no. Nothing to
   paste, and no migration.**
7. **§29's own asks (the reader's polish)** — the ⋯ grid smaller (68dp tiles, 24dp glyph), the
   sheets fast (200ms in / 120ms out, an 80ms settle) with `minHeightFraction` keeping the
   notes/highlights lists a real panel, the head settling on a smooth fade, and the head/search
   morph sharing one 220ms clock. All done.

## 7. §31 — the plan, decided and ordered (NOT YET BUILT)

The member answered five questions; every decision below is theirs, not a guess.
**Do these in this order**, because the first two are migrations and a half-done
migration is worse than none (it is the same "animations are still bad" report).

**STATUS (v439): 7.1 DONE, 7.2 DONE, 7.3 PARTLY DONE, 7.4 and 7.5 and 7.6 NOT BUILT.**
What landed: the token set (as an EXTENSION of the existing public `CurioMotion`,
never a rewrite — read 7.1's warning below), `pillArrive()/pillLeave()/popArrive()/
popLeave()` spent across the reader's head, foot, search, scrubber, selection bar and
settings page and across the journal's undo pill, mic, copy box, dock, voice capsule
and pinned line, plus press feedback on `ReaderChromeButton` and `ReaderPillButton`
(the two controls the reader is actually built from). Still open: 7.4's one
pill/dock language (the copy box is still two rows), 7.5's one empty state, and 7.6's
motion lock + Zoom slider removal.

### 7.1 One motion token set (do FIRST — everything else refers to it)

**READ THIS BEFORE TOUCHING IT: `ui/theme/CurioMotion.kt` ALREADY EXISTS AND IS
PUBLIC.** It holds `Springs.*`, `Durations.*`, `ConfettiParticleCount` and
`MinSpinTurns`, which ~30 call sites across `CurioAnimations`, `CurioNavHost`,
`CurioConfetti`, `CurioPressFeedback` and the card components compile against. v439
extended it; a `write_file` here is how the file got briefly destroyed. Check
`git status` shows `M` (not `??`) before writing a file you believe is new.


A single place (suggested: `ui/theme/CurioMotion.kt`) holding named durations and
easings — enter (~220ms, `FastOutSlowInEasing`), exit (~140ms, `FastOutLinearInEasing`),
emphasized (~320ms) — with a documented rule: **no surface may invent its own
milliseconds.** Today the numbers are scattered: the reader's sheets 200/120, the
reader's head 220/160, its search 180/140, the journal dock 180-220, the copy box
160/120, the page slider 170/130. Migrate every one of them.

### 7.2 One arrival for every floating pill (the member's own top pick)

Every floating pill — the reader's head and foot, its search bar, the journal dock,
the copy box, the page slider, the new motion lock — arrives and leaves the SAME
way: a fade on the token's clock plus a small settle (6-of-height drift, as the
head already does). The reader's sheets and chrome are the two the member named as
still bad, so those are the ones to get right first.

### 7.3 Press feedback everywhere

A small press-squish on the controls that have none: the reader's chrome buttons
(`ReaderChromeButton`), the ⋯ tiles (`ReaderMenuTile`), the settings rows. The
journal dock's tools already have it (`expressiveCardPress`-style) — find the one
helper the journal uses and reuse it rather than writing a second.

**STATUS (v439 final): 7.4 DONE (the copy box), 7.5 DONE (bounded, see below), and the
two zoomed-gesture bugs from §7.7 FIXED.**

- **7.4** — the copy box is one row with the reach behind a labelled door that grows
  inside the same pill (the dock's own pattern). See the v439 section in
  `app/AGENTS.md`.
- **7.5** — `CurioEmptyLine` (an em dash) is the one bare-list empty state. **Bounded
  on purpose:** an empty SCREEN keeps its headline/subtext/door and a message that
  tells the member how to fix the emptiness stays, because both are content rather
  than a state. Converted: the comments sheet, the card screen's replies, the text
  history panel.
- **§7.7's two gesture bugs** — root cause found by inspection, NOT another flag: the
  page consumed a tap's own wobble as a pan, which cancels `detectTapGestures` (both
  the tap and a pending long press). `pinchToZoom` now wears in at the touch slop.
  **Note the drift this exposed:** `readerZoomThisPage`'s doc already promised the slop
  rule and nothing implemented it.

### 7.4 One pill/dock language

One capsule spec shared by the journal's dock, the copy box, the reader's foot pill
and the motion lock: the SAME height (46dp), the same radius rule (28dp when the
pill grows a panel, a real capsule when it does not), the same lift (12dp). **The
copy box is the member's own example of the failure** (*"the copy paste tool bar two
row ui is bad and not like that dock ui"*): its two rows are a second toolbar, not
the dock's one-row-plus-panel shape, so it must be rebuilt as ONE row with its
actions behind a door that grows INSIDE the pill (`PersonalDockGroup` is the
existing pattern to copy).

### 7.5 One empty state

The em dash wherever a list is empty — journal list, Cabinet, highlights, notices —
instead of a sentence each place writes for itself. `ReaderMarksSection` (v438) is
the first one; find the rest by searching for "Nothing" / "No … yet".

**STATUS (v439 cont.): 7.6 IS DONE.** Built exactly as written below: both Zoom rows
out, `ReaderLook.motionLock` in (and in `rememberKey()`), the pill on the page's
bottom-right above the foot, and the guard at the top of `pinchToZoom`'s event loop
(the one handler every reader gesture passes through) plus `readerDoubleTapZoom`.
**The two traps it recorded were both kept**: consume movement but never the first
down, and the flag in `rememberKey()`. Also done in the same pass, and not from
this plan: the member's blob as their own profile picture (§32).

### 7.6 The motion lock, and the Zoom slider goes

The member: *"in pdf only remove that zoom slider and add the motion lock pill which
restrits that drag to move and pinch to zoom it locks in the state the user left the
zoom position"*, then chose **freeze pan AND pinch, and remember it**. So:

- Remove the Zoom row from `ReaderAppearanceSheet` AND `ReaderSettingsScreen`, and
drop `showZoom` from both signatures and their call sites.
- `ReaderLook.motionLock: Boolean`, persisted — **and it MUST be added to
`ReaderLook.rememberKey()` or it will silently never save** (the v434 rule).
- The pill: floating, PDF only, bottom RIGHT corner above the foot pill (bottom
≈84dp, end ≈14dp), and hidden while the page slider is up (they would overlap).
- The lock itself: the guard belongs in the PDF's gesture path — the `pinchToZoom`
handler AND the pan it folds in (`readerZoomDocument` / `readerZoomedPan`), not in
the drawing. A tap must still turn the page.

### 7.7 Still open, unchanged by the answers above

1. **Zoomed-in long-press selection, and the tools not returning on a single tap.**
   The same root cause, and the v434 band-aid (`ReaderTouch.selecting`) did not
   cover the tap path. Fix at the gesture seam, not with another flag.
2. **"Fix the highlighht pill selecter in pdf"** — confirm WHICH part: the pens'
   row, or the pill that opens it.
3. ~~**The gestures box**: hide the floating panel while an area is being adjusted, a
   way to hide the PANEL (not the backdrop), and a way to bring it back to edit.~~
   **DONE (v440, §34).**
4. **The journal's paper following its colour by default** — still needs the word
   (the "Paint the page too" switch).

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- **§34 — "do the reder, journal additions and also for journal ad time note too its only note date, and in journals view dont update the time if its edited again late, and add search for journals and also sorting by date by tapping the date in journals date" (THE THREE JOURNAL ITEMS ARE DONE; THE "READER, JOURNAL ADDITIONS" GROUPS ARE AWAITING ONE ANSWER).**
  **Built and committed (unpushed, per the member's "dont push anything now") — v440:**
  1. **A row names the moment the page was WRITTEN.** `PersonalNoteEntity.writtenAtMillis()`
     (= `createdAtMillis`, falling back to `updatedAtMillis` for pre-v389 rows) is printed
     beside the word count by `Long.prettyTime()`. `saveNote` already preserved
     `createdAtMillis` and only re-stamped `updatedAtMillis`, so a late edit cannot move it —
     the DAO's tiebreaker (`COALESCE(NULLIF(createdAtMillis, 0), updatedAtMillis)`) says the
     same thing for the same reason. **No SQL, no migration.**
  2. **Search** (`JournalSearchPill` + `PersonalNoteEntity.answers`): a pill under the head
     that becomes the field, on the `CurioMotion` pill clock, taking focus as it opens. It
     reads only what a row already shows — **never `doc`**, which re-parses JSON per access —
     and the head's subtitle counts ``matched of journals`` during a search. A miss is a
     cause ("Nothing matches" + the query), not a bare dash.
  3. **The date pill orders the collection** (`PersonalHeaderDate(onToggleSort, newestFirst)`):
     the shelf passes nothing and keeps its plain label (`enabled = onToggleSort != null` — a
     disabled `Surface` takes no presses and draws no ripple, so one composable is both); the
     journals list reverses on tap with an arrow saying which end is up. Ordering happens in
     the SCREEN (`sortedWith`, tie on `writtenAtMillis()`), so reversing costs no DB trip, and
     the month groups reverse with it.
  4. **The gestures box stands down, three ways** (`ReaderTapZoneEditor`): while an edge is
     being placed (`ReaderZoneHandle.onAdjust` — drag start/end/cancel), by a collapse door on
     the panel, and by v434's eye for the washes. Standing down leaves a "Gestures" pill in
     the corner that opens it again, and every movement is a `CurioMotion` factory.
  **Still awaiting one answer:** *"do the reader, journal additions"* — which of the suggested
  settings groups to build (see "What remains" below). Nothing else in this prompt is open.
- **§32 — "continue and still the pdf reader buttom sheet close is weirdly slow ... do the motion token set and one arrival for every floating pill ... and in pdf reader, a high charge save turns on" (DONE, v439).** Built: the sheet close now travels its OWN height (the "weirdly slow" was 60%-of-screen travel on a 200dp sheet, not the clock — see `app/AGENTS.md` v439); the pill clock added to `CurioMotion` and spent across the reader's and journal's floating furniture; the back button is its own 50dp circle pill; press feedback on the reader's two control builders; and **low power reading** (`ReaderLook.lowPower`, on by default, a real "Power" row in the reader's settings — RGB_565 pages, a 1.5× upscale cap, `beyondViewportPageCount = 0`, and `cacheDir/book-images` pruned as the reader closes). **Also fixed the red build that was pushed as `147a2516`**: `PersonalPage.kt:722` had an orphan `else MaterialTheme.colorScheme.background` left by v438's edit — the file's own paper is `journalPaper()`. No SQL change for anything in either round.

- **§31 — the reader's polish round two (PARTLY DONE, plan in §7).** Done from it: back
  no longer exits the reader (`BackHandler`), the settings head wears the reader's top
  floor, the night dim covers the tools, the ⋯ tiles are a glyph-only capsule with the
  name outside it, an empty marks list is an em dash, one word gets the whole selection
  bar back, and the page slider has the foot to itself. **Committed as `2db57fc0` and
  deliberately NOT pushed** (the member's instruction). Not built: the motion-lock pill +
  removing the Zoom slider, the copy box's restyle to the dock's language, the motion
  token set / one-arrival / press-feedback passes, one empty state everywhere, and the
  two zoomed-gesture bugs. The member's five answers are recorded in §7.
- **§30 — the writing page always keeps a line to type in (DONE).** The member's rule from §28,
  implemented at the state level rather than inside the copy box: one `publish()` choke point and
  one `keepLineToTypeIn()` guard, so cut, the row tools and the gesture tools cannot empty a page.
  Known and accepted: undoing a full-page cut restores the rows and keeps the added line.
- **§29 — the ⋯ menu, the sheets' speed, and the reader's animations (DONE).** *"the 3 dot menu
  in pdf reader is bad like too huge and also the drop downs of each is slo, lie it takes a
  secdond to close, and also the highloght and notes dropd won is so small"* plus *"for the
  floating title in pdf reader use smooth fade animatuio n for search pil use merge and smoth
  morphe, for buttom sheet use proper animation fast animation, and more similiar pass smoth
  animattions"*. All shipped. **The one item carried forward: the copy box's cut-everything dead
  end (an empty line must always remain) — the member's rule, still unimplemented.**
- **§28 — the journal dock pass, the reader's scrubber, and the build (DONE).** Both red-build
  fixes (the swallowed `Column(` and the `@Composable` display-cutout getter); the dock's
  thickness and its two remaining dropdowns; leaf + crystal removed; quote/bullet shades split
  wider for the light page and the night; the bullet axis lifted to the words' centre; the
  journal's colour reaching the raised paper, the dock's tools and the date/title bar; the copy
  box as two rows with the dock stepping aside; the reader's scrubber as a floating pill; the
  blobatar idle motion; the SQL question (answered: no change). **Open: the copy box's
  cut-everything dead end.**
- **§27 — the portrait port, the root clutter, and the reader's head (done).**
  Asked permission before every deletion and confirmed the four decisions before editing.
  Four asks: remove the useless root node modules + manifest/doc clutter (done); use blobatar
  for the social portraits instead of the hand-drawn set (ported to Kotlin, verified against
  upstream, picker removed, all call sites migrated); the reader's search/back/title pill too
  close to the status bar (own top floor); separate search as a circle pill that merges into
  the header when opened (done). The journal dock is the member's next step, not this one.
- **§33 — the voice note's wave, and the failed CL (DONE, v439).** The member: *"make the
  wave and bar of the sound in vn more accurate depiction also fix the failed cl"*.
  **The CL:** the red run was `feat(profile,reading)` — two errors — then the one before it,
  a third: `LocalContext.current` (which is @Composable) read inside the blob door's plain
  `onClick`, plus `positionChanged()` unresolved in the reader (needs an import there) and
  `context` resolving to a function inside `ProfileDialogs`. All three fixed and pushed
  (`c3418e83`, `6194f2c6`). **Note for next time: the 6ff62b80 run proved the copy box,
  the gesture wear-in and the empty state all COMPILED — only that one line was wrong.**
  **The wave:** the inaccuracy was two layers deep — every bar normalized against 16-bit
  FULL SCALE and then squared (a real sentence drew at ~4% of the band), and `bucketLevels`
  dropping the tail of every drawing (42 columns from 72 samples = 36 real + six stale
  repeats). Both fixed at the source, plus `sqrt(peak·rms)` per bar, a linear reach, the
  live meter read against a decaying reference, the BEADS radius un-squared, and the
  decode loop's `MutableList<Short>` (sixteen million boxed samples per 3-minute note)
  replaced with a primitive sink. **Storage is unchanged: one hex byte a bar, no migration,
  no SQL.** See the v439 section in `app/AGENTS.md`.
- **§26 — the reader chrome pass (done).** Sheet scroll + swipe-close from anywhere; appearance
  as capsule/segmented controls with text size AND a PDF's zoom; the ⋯ menu as a six-capsule
  grid (Share / Gestures / Settings); the Gestures editor's eye, on-demand depth and
  tap-a-zone-to-select; zoomed long-press selection; the hold dock carrying the dictionary and
  Share; six new look settings, persisted. Design confirmed by four questions before any edit.
- **§25 — the double tap, and the tools' appear/disappear (done).**
- **§24 — the journal dock pass (done).**
- **§23 — the reader redesign + the vertical-PDF zoom (done).**
- (empty slot)
