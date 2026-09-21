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
   tests `!editor.pageEditBarOpen`. **The cut-everything dead end is still open**: the member's
   answer was *"Always one empty line to type in"* — the editor must guarantee an editable block
   after a cut empties the page. **TODO (not done in this session).**
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

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

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
- **§26 — the reader chrome pass (done).** Sheet scroll + swipe-close from anywhere; appearance
  as capsule/segmented controls with text size AND a PDF's zoom; the ⋯ menu as a six-capsule
  grid (Share / Gestures / Settings); the Gestures editor's eye, on-demand depth and
  tap-a-zone-to-select; zoomed long-press selection; the hold dock carrying the dictionary and
  Share; six new look settings, persisted. Design confirmed by four questions before any edit.
- **§25 — the double tap, and the tools' appear/disappear (done).**
- **§24 — the journal dock pass (done).**
- **§23 — the reader redesign + the vertical-PDF zoom (done).**
- (empty slot)
