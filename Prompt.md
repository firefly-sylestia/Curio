# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 0. THE CURRENT REQUEST — §40 — the dock's panels, the reader's sheets, the dictionary's three doors, and the sky

> the bulletpoint and highlight so tapping it again opens the collapsed options, and then i need
> to tap that last cross to close it, and theres one at the first to dismiss the picked, so make
> the close button the first one both dismiss and deselect the pick, and also the collapse auto
> closes when i start typing or i tap the page. also for dictionary use both the provider add show
> it a a badge option to switch between, also the buttom sheet can be scrollable and a little up.
> also the text tap and hold selection is bad, like when the line end and i copy 2 line then the
> both lines are touching each other with no space. also the online dictionary is bad, add a
> downloadable dictionary inside the app in the dictionary bottom sheet, but when the dictionary is
> opened from the 3 dot one it [is] more longer and let user search any word, and also fix the
> search box hiding below the keyboard for dictionary. and then for the drawer star map graph,
> animate it with star twinkle, and animate every time it closes and opens, with beautiful mesh
> like animation dont change the design, just beautifully animate it, also fix the light glow of
> the category tint when one is selected, and dont grow the dot too much. also the reader dropdown
> closes fast and good when taping outside but the swipe down to close is buggy it stays as an
> overlay for some time fix it.

**Decisions confirmed with the member before editing** (the ask round): the offline dictionary is
**a full one** (~100k headwords, one download, searched offline); the badge order is
**Offline · Wiktionary · Free, offline first once downloaded**; and the glued-lines copy report is
**the reader's long-press selection**, not the journal's copy box.

**Files:** `ReaderOfflineDictionary.kt` (new), `BookReaderScreen.kt` (the frame, the sheet),
`BookPdfText.kt` (the join), `PersonalCanvas.kt` (the dock's panels), `HomeScreen.kt` +
`CurioNavHost.kt` (the sky). `web/` and `desktop/` untouched.

### 0.1 What was built

1. **The dock's panels** — the marker's "No marker" and the bullet's "Remove list" now clear the
   pick AND close the panel; the trailing cross is gone from those two (it survives on format,
   alignment and export, whose first control is a real choice); and a panel closes itself when the
   focused line changes (which is what tapping the page does) or its text changes (typing).
2. **The reader's sheets** — the frame takes the IME inset on its root box (so a bottom-aligned
   sheet lifts clear of the keyboard) and it settles a body drag the instant no finger is left
   down, on the same flick the head reads; the debounce stays only as a fallback.
3. **A passage over a line break** — `PdfPageText` joins glyphs with a space where the GLYPHS'
   geometry says the page broke the line (a moved baseline, or a real gap), never after a hyphen,
   and never doubling a space that is already there. The page's own `text` and `textBetween`
   share one builder, so the copy, the context line, a share and a stored highlight all agree.
4. **The dictionary's three doors** — a badge row (Offline · Wiktionary · Free) with the offline
   file first once it is there, the sheet re-asking on a door switch (the online doors memoise, the
   offline one is a local stream), a taller panel for the ⋯ menu's search mode `0.62f` vs `0.55f`,
   and the download row (progress, licence, Remove) inside the sheet.
5. **The offline dictionary** — `ReaderOfflineDictionary`: Webster's 1913 (public domain, ~9MB,
   ~86,000 headwords) streamed from the source's own JSON with `android.util.JsonReader`, a
   `.part` file renamed into place only when whole, alphabetical early stop, and `null` for "no
   dictionary" against `emptyList()` for "no such headword".
6. **The sky** — one `reveal` progress driven by the drawer's own state: the panel eases and fades
   in, every hairline runs out from its star (the mesh draws itself), both reverse on close; a
   `withFrameNanos` twinkle runs only while the drawer is open and is read inside the draw block;
   the picked aura wears the lane's own tint at deeper mixes and tighter radii, and the dot grows
   18% instead of 50%.

### 0.2 Checks run

- **No Gradle command** (root `AGENTS.md` forbids it here); CI validates on push.
- `android.util.JsonReader`/`JsonToken` are framework APIs; `DictionaryDoor` keeps the existing
  `ReaderDictionarySource` and its two-option settings row untouched (no `when` had to change);
  `withFrameNanos` and the four badge-row imports were added after checking what the file already
  imported; the download callback writes Compose state (thread-safe) and reports progress in the
  row's own text rather than adding a progress-bar API that this Material version may not carry.
- A string/comment-aware bracket-balance pass over all six touched files: balanced.

### 0.3 Still open

- **The offline file is ~9MB, not the 20–30MB guessed** — Webster's 1913 is the full dictionary at
  that size, which is a better download rather than a smaller dictionary.
- **A tap on the SAME line's whitespace** does not close a dock panel (only a focus MOVE or
  typing does): the editor has no page-tap signal of its own yet; if the member wants that exact
  case, the page's tap handler is where the signal would come from.

---

# (previous session, kept for the state it records)

## 0. §39 — the Edit profile page, as a full screen

> this ia the desin specification of edit profile and instead of dialog box make it a full screen
> with this style kee the backgorud and color theme aware and kee the backgroud plain thi sis the
> modified only and ask question of anythign else, + the spec pasted below.

**The spec, in one line each:** editorial, calm, minimal, tactile; theme-aware plain background;
no gradients, no glassmorphism, no decorative cards; a small back button, a large quiet
"Edit profile" title and one supporting sentence; the picture centred with a small camera disc
overlapping it and exactly two compact actions under it; NAME and BIO as OPEN FIELDS with a
subtle bottom border (never a box in a box); an ACCOUNT section of flat EMAIL (muted/locked) and
USERNAME rows with a small supporting terms line; ONE tappable PRIVACY row; Cancel + Save
changes fixed at the foot (quiet + solid berry, same height, same radius); one typeface with a
calm hierarchy (nothing every-heading-bold); and an 8dp spacing base (8/16/24/32/40) where
important elements get more SPACE rather than another card.

**Decisions, all confirmed with the member before editing** (the ask round for this request):

- **Signed OUT** → one calm row ("Sign in to Curio") that opens the existing account surface
  (`CurioRoutes.SETTINGS_ONLINE`), never a form squatting inside the page.
- **Sign out** lives at the BOTTOM of this screen (row + the existing confirm dialog).
- **The picture**: exactly TWO actions (Add/Change photo · Use my blob / Use my photo). Tapping
  the picture itself expands it over the page, and the ⋯ in that expansion offers removal
  (the deletion door the dialog used to have).
- **Terms** → the SAME dialog the account card already shows (one `private` → `internal` flip on
  `CurioTermsDialog`, no second copy of the text).
- **A real route** (`profile/edit`), not an overlay: the page is a page, and Cancel simply
  leaves.
- **The handle folds into Save changes** and is checked AS IT IS TYPED (the same rules the
  server enforces, stated before the press) — no separate "Save username" button.

**Files:** new `features/profile/ProfileEditScreen.kt` (the page + the avatar pipeline moved out
of `ProfileScreen.kt`), `navigation/CurioRoutes.kt`, `navigation/CurioNavHost.kt`,
`features/settings/CurioAccountComponents.kt` (the terms dialog's visibility),
`features/profile/ProfileScreen.kt` (the old `ProfileDialogs` + its helpers retired; all four
entry points navigate). `web/` and `desktop/` untouched, per root `AGENTS.md`.

**Plan:** routes → the new page → the dialog's exit → verification (imports, references, bracket
balance) → DOX + changelog + commit/push.

## 0.1 What was built

1. **The route** — `CurioRoutes.PROFILE_EDIT = "profile/edit"` and a plain `composable` in
   `CurioNavHost`. It sits behind the profile page's own prefix, so the shell treats it as a
   pop screen (the same arrival Profile itself has) and shows no bottom bar; it inherits the
   nav host's `Push` clock, so nothing new was added for its opening.
2. **`features/profile/ProfileEditScreen.kt` (new)** — the spec's page: back disc, a 31sp
   Medium title, one 15sp sentence; the 104dp picture with a 34dp accent camera disc on its
   corner and the two quiet actions under it; **open fields** (a hairline, never a box —
   `EditOpenField`) for NAME and BIO and for the prefixed USERNAME; a muted, locked EMAIL row;
   the handle's one-line notice; the terms as a supporting line; a plain tappable PRIVACY row;
   a quiet SIGN OUT row; and Cancel / Save changes (52dp, 22dp radius, one each) held at the
   foot. `EditSpace` holds the 8/16/24/32/40 ruler. The avatar pipeline
   (`decodeAvatarSource`, `centerSquareCrop`, `scaleToMax`, `saveAvatar`, `removePhoto`) moved
   here from `ProfileScreen`. Tapping the picture expands it over a **blurred** page
   (`Modifier.blur` on an animated veil) with the ⋯ that removes the photo; `BackHandler`
   closes the expansion before the page.
3. **Saving** — `commit()`: name + bio written locally and mirrored best-effort; a changed
   handle validated from the same rule the server uses, then claimed (`SocialApi.updateUsername`)
   with the terms accepted through the shared dialog first (`pendingClaim`). A refusal is shown
   on the page; a success pops.
4. **The dialog retired** — `ProfileDialogs`, `EditSectionLabel`, `DialogPillAction` and the
   avatar helpers are gone from `ProfileScreen.kt` (390 + 75 lines), all three "Edit profile"
   doors navigate, the sign-out confirm moved with the page, and the screen keeps only the
   avatar path it draws (re-read on entry and on `ON_RESUME`, so the hero can never lag an edit).
5. **`CurioTermsDialog`** is `internal` now instead of `private` — one dialog, two callers, no
   second copy of the disclosure text.

## 0.2 Checks run

- **No Gradle command** (root `AGENTS.md` forbids compile/build/lint here); CI validates on push.
- Every API checked against its real definition before use: `CurioTermsDialog`'s signature,
  `CurioContentFilter`'s package (`data`), `SocialApi.updateUsername/updateDisplayName/updateBio`,
  `settingsRoseAccent`/`settingsReadableInk`/`settingsCardAccentInk` (public in
  `SettingsHubScreen.kt`), `CurioIcons.Screenshot` (= "photo_camera"), `AvatarCropDialog`'s
  three params, `SocialConfirmDialog`'s named params, `CurioMotion.Durations`, and
  `popScreenRoutePrefixes`' prefix matching (which is what makes `profile/edit` arrive like
  Profile). The module globally opts into `ExperimentalMaterial3Api`, so `Surface(onClick = …)`
  needs nothing.
- **A string/comment-aware bracket-balance pass** over all five touched Kotlin files: balanced.
- **The unused-import sweep** over `ProfileScreen.kt` dropped the 42 imports the retired dialog
  and its helpers owned; every remaining import is referenced (checked symbol by symbol, with
  `getValue`/`setValue` excluded as delegated names).
- Grepped the whole module for the retired names (`ProfileDialogs`, `showNameDialog`,
  `nameInput`, `taglineInput`, `cropSource`, `saveAvatar`, `removeAvatar`, `confirmingSignOut`,
  `decodeAvatarSource`, …) — no reference anywhere survives, and nothing else in the app opened
  that dialog.

## 0.3 Still open

- **The ⋯ in the expanded picture** offers "Remove photo" (and the blob switch when there is no
  photo, so the expansion is never a dead end). If the member wants more there — "Save to
  device", sharing — the menu is the place for it.
- **The bio is one line**, exactly as the dialog had it: the hero draws it as a single line, so
  a multi-line bio would need the hero to grow a rule first.
- **No SQL change and no migration**: everything here is UI, navigation, preference reads and
  one existing network call.

---

# (previous session, kept for the state it records)

## 1. The request (previous session) — §38

> fix the reading progress accidental touch and in journal the pain the page remove that
> option, and then mak ethe coloring smart so that chnaging color automatically adjusts the
> text color as well so the today the date pill or anything else doesnt get the weird
> unredable text, also i think dont color the today are,a, and also for the copy arrow the
> behaviror is unexpected fix it, also for the hihgligh selecter remove the frst x and when
> tappin git again the color it should deselect, and then the menu the drawer menu icon on
> home screen its gteeing the profile pic so fix that.

> continue

Seven things across three surfaces (the book page, the journal, the reader's dock and Home),
plus the one the member added while answering the questions: the dictionary "wasnt working".

1. **The reading-progress bar** — an accidental touch moves it (and stole the scroll).
2. **The journal's "Paint the page too"** — remove the option.
3. **The journal's colouring** — the ink on anything the member's colour fills must adjust
   itself, so nothing comes out unreadable.
4. **The Today/date area** — does not take the page's colour.
5. **The copy box's ← / → arrows** — the behaviour is unexpected.
6. **The highlight dock's ×** — remove it, and the applied colour should deselect.
7. **Home's menu pill** — it must not wear the member's picture.

Touched: `features/personal/BookDetailScreen.kt` (the gauge), `PersonalTheme.kt`,
`JournalAccent.kt`, `PersonalPage.kt`, `JournalEditorScreen.kt`, `PersonalCanvas.kt`,
`ReaderDictionary.kt`, `BookReaderScreen.kt`, `data/PersonalEntity.kt` (a dead helper),
`features/home/HomeScreen.kt`. `web/` and `desktop/` untouched, per root `AGENTS.md`.

## 2. What was actually wrong (found by reading, not by guessing)

1. **The progress bar** — `ReadingGauge`'s handler consumed the DOWN and treated ANY movement
   as a scrub (`if (change.positionChanged()) moved = true`). A finger running down the page
   that passed over the gauge therefore moved the reading place AND, because the down was
   consumed, stopped the page from scrolling: one false move cost both gestures.
2. **"Paint the page too"** — v429 added it, v439 withdrew it, v440b restored it. The switch
   lived in `JournalAccentSheet`, and the paper it painted came from
   `JournalPagePaint.painted` through `journalPaper()` (a 0.20 light / 0.28 dark tint) with
   `journalInk()`'s own contrast branch answering for the result.
3. **The unreadable text** — `journalOn(fill)` was `settingsReadableInk(fill)`, and
   `settingsReadableInk` answers from the THEME (a named theme's `onPrimary` pair, the pastel
   flag, the light/dark branch) and **never looks at `fill` at all**. So the one case
   `journalOn` exists for — text on a colour the member picked — was the one case it got
   wrong. The colour sheet's own tick had the same defect (`settingsReadableInk(color)`), and
   `PersonalCanvas.readableOnFill` was the only place in the journal with the right rule.
4. **The Today pill** — `JournalTopBar` was handed the page's `accentArgb` (v437: "the day IS
   the page's own title") and filled the day capsule with it, so a deep page colour left the
   date's ink at whatever the theme happened to be — the unreadable pill.
5. **The letter arrows** — `nudgePageLetters(more)`: **→ WALKED** the window along the row
   once it reached the row's end (`TextRange(range.min - 1, range.max)`) and **← SHRANK** it
   from the right (walking the other way at a single letter). Between them the two arrows
   could never hand the member more than the one character they started with.
6. **The highlight dock** — its colours were a 35% wash of themselves whether the passage wore
   them or not, so a marked passage's own colour was indistinguishable from the three it did
   not wear, a second press re-wrote the same mark, and the only way out of a mark from the
   dock was the × that the member asked to be rid of.
7. **Home's menu pill** — `TopBarPill` drew the member's face whenever
   `hasOwnPicture(avatarPath)` was true, and **a member wearing their blob as their picture
   has no photo path at all**, so `hasOwnPicture(null)` is TRUE for them: the drawer's
   hamburger, handed no path, drew their face instead of its own glyph.
8. **The dictionary** — every word Wiktionary does not carry answers **404**, the old getters
   turned any non-200 into `null`, and `null` is the sheet's own word for UNREACHABLE. A rare
   word, a name or a misspelling was therefore reported as a dead network, and v442's spelling
   suggestions could never run at all (they are only asked for after an EMPTY answer, which
   the 404 path never produced). The sheet's single nullable list also carried "not asked yet"
   and "unreachable" at once, so its first frame flashed "The dictionary could not be
   reached." whatever the word was.

## 3. Decisions, all confirmed before editing

Seven questions were asked and answered across the session ("Two times — from / until",
"Chips + context line", "scrink it but dont make it too lose th buttom", "Never paint the
page", "A scroll over the bar must not move it", "kee adding letter never walk shrink", "The
× in the selection dock", "The hamburger pill shows my photo").

- **The journal's paper**: **never paint the page** — the switch goes and the paper is the
  theme's (the member's own answer to "what should removing it do").
- **The Today area**: keeps the theme's colour (the member's own instruction).
- **The progress bar**: a scroll over it must not move it.
- **The letter arrows**: only add — never walk, never shrink.
- **The dock's ×**: removed, and the applied colour is what deselects.

No new feature here is toggleable-or-not: six of the seven are fixes, and the seventh
(removing "Paint the page too") is a removal the member asked for by name — root
`AGENTS.md`'s "ask before deleting" rule is satisfied by their own words plus the answer they
chose when asked what removing it should do.

## 4. What was built

### 4.1 The reading-progress bar (`BookDetailScreen.kt`)

The gauge now **wears in** like the reader's own magnified page: `awaitFirstDown` takes the
press and consumes nothing, and nothing is taken until the finger crosses the touch slop
**SIDEWAYS** (`dx > slop && dx > dy`). A finger that crosses it DOWNWARD has announced it is
scrolling, so the handler leaves the whole gesture to the page. The knob is lit by the CLAIM,
not by the touch (a scroll over the bar shows nothing at all), and a press that never travels
is still a tap that seeks on the release.

### 4.2 The journal (`PersonalTheme.kt`, `JournalAccent.kt`, `PersonalPage.kt`,
`JournalEditorScreen.kt`, `PersonalCanvas.kt`, `data/PersonalEntity.kt`)

- **The page never paints its paper.** `JournalPagePaint` lost its `painted` flag;
  `journalPaper()` is the theme's parchment with the app's accent whisper whatever colour the
  page was given; `journalInk()` is simply the theme's `onSurface`; the page's own box is the
  plain `background` again. The colour still reaches the page's doors (`journalDoorAccent`)
  and the tint its own controls wear (`journalPaperRaised`).
- **The switch is gone** from `JournalAccentSheet` (with its `painted`/`onPainted` params and
  the now-dead plumbing through `PersonalWritingPage` and `PersonalToolDock`), and the dead
  `PersonalNoteEntity.paintsOwnAccent` with it. **`pagePainted` is still READ and still
  WRITTEN** through `PersonalPageMeta` — the writer rebuilds the whole row from the meta, so
  dropping it would erase a member's earlier answer from their own file on the next
  keystroke. No SQL, no migration.
- **The day keeps the theme's colour.** `JournalTopBar` no longer takes `accentArgb` at all
  and asks for `journalDoorAccent(JOURNAL_ACCENT_THEME)`.
- **Smart ink.** `journalInkOn(fill)` is the measurement
  (`fill.luminance() > 0.55f` → the journal's near-black, else its cream) as a PLAIN function
  (a draw pass and a gesture want it as well as a composition); `journalOn(fill)` is it as a
  composable; `PersonalCanvas.readableOnFill` delegates to it; the colour sheet's tick asks
  it instead of the theme role.

### 4.3 The copy box's arrows (`PersonalCanvas.kt`)

Both arrows only ever ADD: → takes the letter to the RIGHT of the window, ← the letter to its
LEFT — never walking and never shrinking. Either arrow may OPEN a fresh window (← used to
refuse, "nothing to give back"), `canNudgePageLetters` is live for both on an empty reach and
dead only at the row's own edge, and the pills' labels are "One more letter to the left" /
"…to the right". A wrong reach is started over by a row arrow, "Line" or "Select all", all
of which reset `pageCharRange`; the box's own cross stays.

### 4.4 The dictionary (`ReaderDictionary.kt`, `BookReaderScreen.kt`)

- **The status is kept.** `Fetch(code, body)` replaces the two old getters' nullable String
  (one shared `fetch()` with the same 4s / 6s budget), and `define()` answers **404 →
  `emptyList()`** ("no such headword" — the answer that puts the suggestions on screen),
  anything else non-200 or a thrown call → `null` (unreachable), and a term that is not a word
  at all → `emptyList()` (the contract's own line, which the old code contradicted by
  returning `null` and drawing "could not be reached" for `1234`).
- **Three states, not one nullable list.** `ReaderLookup.Idle` / `.Answer(senses)` /
  `.Unreachable`. The sheet flashes nothing while it opens (it used to flash "The dictionary
  could not be reached." on its first frame, and a blank field sat on that message for good),
  and an empty field now draws nothing under the input.
- **The miss path stops at the first `null`** — the source is not answering, so four more
  neighbours would only buy four more timeouts before the same sentence.

### 4.5 The highlight dock (`BookReaderScreen.kt`)

The passage's own ink is resolved in the screen (`marks.firstOrNull { it.isHighlight && … }`)
and handed to the bar as `appliedInk`; that swatch is drawn as TAKEN (opaque, a 2dp rim, a
`Check` tinted with the ink that reads on it — `journalInkOn`), and pressing it **REMOVES the
mark** (`deleteReaderMark`) instead of writing it again. With that switch in the row the ×
("Clear the selection") is redundant and gone: the ⋯ door is the way to everything else, and
a tap on the page puts the dock away (`tapPage` clears a selection first).

### 4.6 Home's menu pill (`HomeScreen.kt`)

`TopBarPill` gained `showingPicture` (default false); only the profile pill passes true, so
only it ever resolves a face, and a pill handed no path draws its own glyph.

## 5. Checks run

- **No Gradle command**: this environment forbids compile / build / lint (root `AGENTS.md`).
  Validation is CI on push.
- Every API verified against its real definition in the repo before use:
  `viewConfiguration.touchSlop` (`GalleryWallFormat.kt` already uses it, so it needs no new
  import — only `kotlin.math.abs` was added to `BookDetailScreen.kt`), `CurioIcons.Check`
  (`ReaderMarkSheet` already draws it inside a colour swatch), `ReaderMarkEntity.colorKey`,
  `deleteReaderMark` (already used by the marks sheet), `PersonalPageMeta.pagePainted` (kept),
  and `hasOwnPicture`'s exact semantics in `ProfileAvatar.kt`.
- **A bracket-balance pass over all ten changed files** (string/comment aware, the one syntax
  fault a large hand-edit hides): zero unbalanced brackets.
- **Every removed import checked for its other references**: `luminance` out of
  `PersonalCanvas.kt` (its only use was `readableOnFill`, which now delegates),
  `settingsReadableInk` and `rememberCurioControlTick` out of `JournalAccent.kt`.
- The v443 block is in `app/AGENTS.md`, and the release notes are in
  `fastlane/metadata/android/en-US/changelogs/20260922.txt` — with the stale "Paint the page
  too" bullets dropped, because the feature never reached a release and the notes are edited
  in place.

## 6. Still open

- **How a letter reach is taken back.** The member asked for "keep adding letter never walk
  shrink", so neither arrow gives a letter back; a wrong reach is started over by a row
  arrow, "Line" or "Select all". If they want a way to take one letter back without the
  window moving, that is one more control on the panel.
- **The journal's paper following its colour** is settled as "never", and the stored
  `pagePainted` column is deliberately left in place (a Room column is a migration).
- **No SQL change and no migration anywhere in §38** — every change is UI, gesture, colour or
  network, and the two look fields that DID need a store were §37's.

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- **§41 — download polish, Edit profile in the reader's floating pills, and the dictionary data question (PARTLY DONE, v446).**
  **Done:** the offline dictionary's download row carries a real 4dp progress bar under its percentage
  (two boxes, the reader's own ink and accent); Edit profile was re-skinned onto the reader's chrome
  objects — a round way-back and a floating `Edit profile` capsule at the head, Cancel / Save changes
  as floating pills of the same 50dp height at the foot, and the spec's supporting sentence moved into
  the body; and the page gained real badges (`EditBadge`) — ACCOUNT says `Curio` / `Signed out`, the
  locked EMAIL says `Locked`, and the username's one state line is a badge in error or accent.
  **Still open (not built):** (1) the dictionary's own PAGE — a route opened from the reader's ⋯ menu
  and one more entry from Home's `+` sheet, taller/standalone, allowed to search any word; (2) the
  data swap the member asked for ("wordnet and fuller please") — the two sources were checked and
  **both are ZIP releases, not the single raw file the current Webster's 1913 door needs**
  (`globalwordnet/english-wordnet`'s `english-wordnet-*-json.zip`; `fluhus/wordnet-to-json`'s release
  asset; the full OPTED 1913 is a 183k-article dump). A parser for either must be written against the
  real bytes, which is its own pass: the honest answer to *"is the current better or are there more
  better ones"* is that **Webster's 1913 (shipped) is still the cleanest licence-safe single file**,
  WordNet/OEWN is the one that carries modern vocabulary (attribution required, zip release), and
  Wordset has the nicest data but **states no licence**, which is why it was not shipped.
- **§40 — the dock's panels, the reader's sheets, the dictionary's three doors, and the sky (DONE, v445).**
  The marker and bullet panels close themselves (the first cross takes the pick off the line AND shuts
  the panel; typing or moving lines puts it away); a reader sheet rides above the keyboard and its
  swipe-down settles the instant the finger leaves; a passage over a line break keeps its words apart
  (the copy, the context line, a highlight); the dictionary carries all three doors as a badge row
  with a **downloadable offline dictionary** (Webster's 1913, public domain, ~9MB, streamed and
  searched locally) and a taller panel when the ⋯ menu opens it to search any word; and the drawer's
  star map twinkles, draws itself in and out with the panel, wears its own category tint when a lane
  is picked, and no longer swells the dot. See §0–§0.3 above and the v445 section of `app/AGENTS.md`.
- **§39 — Edit profile as a full screen, to the member's own design spec (DONE, v444).** The
  identity editor is a PAGE now (`profile/edit`, `ProfileEditScreen`) instead of a dialog: a
  small way back over a large quiet title and one supporting sentence, the picture centred with
  a small camera disc and exactly two compact actions under it, NAME and BIO as OPEN FIELDS with
  a hairline under them, a flat ACCOUNT section (muted locked EMAIL, prefixed USERNAME), ONE
  tappable privacy row, sign out at the bottom, and Cancel / Save changes held at the foot — all
  on a plain theme background with the spec's 8dp ruler and no cards inside cards. The handle is
  claimed with **Save changes** and checked as it is typed, the terms are the SAME dialog the
  account card shows, tapping the picture expands it (with the ⋯ that removes the photo) over a
  blurred page, and a signed-out account is one calm row. What the dialog could do — the photo
  and its crop editor, the blob, the name, the bio, the handle, Privacy, signing out — all
  survives the move; the old `ProfileDialogs` and its helpers are gone. **No SQL, no migration.**
  See §0–§0.3 above and the v444 section of `app/AGENTS.md`.
- **§38 — the seven fixes across the book page, the journal, the reader's dock and Home
  (DONE, v443).** The progress gauge wears in sideways so a scroll over it neither moves it nor
  steals the page's scroll; the journal's "Paint the page too" is gone and the paper is the
  theme's again; the day pill keeps the theme's colour; ink on any colour the member picks is
  measured (`journalInkOn`, so nothing comes out unreadable); the copy box's letter arrows only
  ever ADD a letter on their own side; the reader's highlight dock draws the passage's own
  colour as taken and pressing it takes the highlight back, with the × gone; and Home's menu
  pill no longer wears the member's picture. Plus the one that surfaced while answering the
  questions: the dictionary treated the source's own 404 as "could not be reached", so a
  misspelled or rare word read as a dead network and the spelling suggestions could never run
  (the sheet now tells "not asked", "no such word" and "the source is down" apart, and stops
  asking a source that has stopped answering). **No SQL, no migration** — the stored
  `pagePainted` column stays, the UI simply no longer offers it. See §1–§6 above and the v443
  section of `app/AGENTS.md`.
- **§37 — the nine reader fixes (DONE, v442, pushed as `fe22bfdc`).** The ⋯ sheet sized to its
  own tiles; the dim's FROM/UNTIL window with one shared clock row and picker; the motion
  lock's wear-in so a locked page still hears a tap, a hold and a sweep; the side taps answered
  by their own innermost handler (fast, repeatable, and never read as a double tap); the
  dictionary suggesting the passage's words as chips with the sentence quoted, plus spelling
  suggestions on a miss; `headword()` so a comma, a possessive or a stray quote no longer hides
  a real word; the sheet shutting on a flick as well as a distance, with a longer pull needed
  nowhere; the highlight dock wearing the reader's own opaque pill body instead of a shadow
  over near-identical paper; and the header's exit reduced to one motion per reason, written as
  `CurioMotion` tokens. **No SQL, no migration.** See the v442 section of `app/AGENTS.md`.
- **§36 — the journal's open animation, and the page slider's arrows + shadow (DONE, v441, pushed as `03bd2e44` and the v441 commit).**
  1. **"the animation open nimation of journal is clanky and the pass u did for motion i
     think that also cause this" — the member was right about the second half and right
     for the wrong reason about the first.** The journal was NOT the cause: every plain
     forward navigation (the journal editor, a chapter, a book, a profile) falls into the
     nav host's generic branch, which glided the new page in over `Durations.Deliberate`
     — **500ms** — with the outgoing page drifting for the same half second. That branch
     has its own token now (`Durations.Push` 260ms / `.Pop` 220ms), the travel untouched
     (1/6 in, 1/8 out), and `Deliberate` goes back to what it was written for. The part
     the member was right about: v439's pill clock was 220/140 where the furniture it
     replaced was 180/120 — **slower by 40ms** — so it is 190/130 now. One clock, one
     tempo set, no second clock for the journal.
  2. **"page slider ui is bad with tha weird shadow — and next and previous button doesnt
     work on rapid click only goes 1 and stops working" — one root, two symptoms.**
     (a) **The arrows (`stepFrom`, `stepLedger`):** every arrow and zone computed its
     target from a place that is only true once the turn has FINISHED
     (`pagerState.currentPage`, `shownPage`, `listState.firstVisibleItemIndex`,
     `textPager.currentPage`), so four quick taps all computed the SAME next page and
     re-asked for the turn already in flight — one page, then dead until it settled.
     `stepLedger` remembers the hop; a tap steps from the destination while the reader is
     still on either END of it. **The two-end match is the safety property, not an
     oversight — a step hop is one page, so no integer lies strictly between its ends,**
     while a range rule would let a chapter or mark jump that lands inside an old run of
     taps resume from the run's end. Declared ABOVE `stepPage` (a local function cannot
     reach a local declared later in its own body — the file says so twice already).
     (b) **The same defect one layer up:** `ReaderHoldButton` is built once with
     `pointerInput(Unit)`, so it would have kept calling the closure it was first built
     with forever; its `step` is read through `rememberUpdatedState` now.
     (c) **The shadow:** the pill's fill was `surface` F5F0E8 over `paper` FBF6EC — two per
     cent apart — so the only visible part of the capsule WAS its shadow. Opaque
     `lerp(surface, ink, 0.06f)` fill + hairline edge + the pinned page's 8dp lift,
     `animateContentSize` deleted (it animated nothing and cost a layout pass per frame),
     and the count in a **fixed 72dp right-aligned slot** so the track never resizes under
     the member's finger while they drag.
  3. **No SQL change and no migration anywhere in §36** (UI, gesture and motion tokens only).
- **§35 — the four bugs from the crash report, the journal's page colour, and read-aloud (DONE, pushed as `1fcc1d00`/`621c7207`).**
  1. **THE CRASH, FOUND IN THE CODE (`drawVoicePulse`, `MIN_WAVE_HEIGHT_PX`).** The
     report — *"Cannot coerce value to an empty range: maximum -0.9 is less than minimum
     0.9"*, thrown during `dispatchDraw` — is the voice note's WAVE: the stroke is floored
     at 1.8dp so `bandTop` is exactly 0.9, and `bandBottom` is `size.height - halfStroke -
     depthDrop`, so on a canvas with no height the band CLOSES AND INVERTS and
     `coerceIn(bandTop, bandBottom)` throws on the empty range. A row that has not been
     measured yet reports exactly that size for a frame. Two guards now: a canvas under
     12px is left alone, and the band can never close.
  2. **THE PDF IS BACK IN COOLER (`renderPdfPage`).** v439 rendered pages into `RGB_565`
     to save the alpha channel — **`PdfRenderer.Page.render` accepts nothing but
     ARGB_8888** — so "Cooler" stopped rendering pages at all (member: *"pdf isnt loading
     now in cooler"*). ARGB_8888 always now; the savings that are real (the 1.5× upscale
     cap and `beyondViewportPageCount = 0`) stay. Never make that config conditional again.
  3. **THE JOURNAL OPENS ON THE TAP (`rememberJournalDoor`, `todayEntryId`).** v440's door
     awaited a database query before navigating (*"journal opening is clanky too"*). The
     list already holds every journal it draws, so today's page is handed in from memory.
  4. **THE SHEETS ARE PANELS AGAIN, AND THEIR SCRIM IS DRAWN (`ReaderSheetFrame`).** Every
     reader sheet keeps 45% of the screen (the ⋯ grid and the dictionary were the two
     named), and the scrim no longer fades through `graphicsLayer` — a full-screen
     offscreen layer re-blended every frame of every arrival and departure, which is a
     real part of "clunky and not smooth". **No motion restriction was added anywhere:**
     the only animator-scale check in the app is the blob faces obeying the phone's own
     "remove animations" switch, which cannot touch app motion.
  5. **THE JOURNAL PAGE TAKES ITS COLOUR AGAIN.** `git revert` of `147a2516` (the v439
     withdrawal), resolved by hand in `PersonalPage.kt` — the page paints itself as it did
     at `e869bac5`, and the "Paint the page too" switch is back in the colour sheet.
  6. **READ-ALOUD, FINISHED (`ReaderSpeaker`, the speak pill, `speakSpeed`/`speakVoice`).**
     Reads the visible page and follows on (blocks for a reflowed book, page text for a
     PDF), with a speed slider and a voice picker in reading settings. The engine is
     prepared on the first tap and released when the reader closes.
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
  **§34b — the additions the member then ticked (DONE EXCEPT TWO).**
  Reader: **night dim on a schedule** (`ReaderLook.dimAuto` — "At sunset" = the phone's own
  dark theme, which is what a phone set to automatic switches at sunset; Android has no
  sunset to ask for and computing one needs the location, which this app holds for nothing
  else — **superseded by §37's FROM/UNTIL window**); **pages left in the chapter** in the
  progress card (a book with pages answers from `ReaderOutlineEntry.page`, a reflowed book
  from `TextPagedReader`, and a page-less flow says nothing rather than a made-up zero).
  Journal: **filter by mood / colour / length** (mood and colour are columns; the length
  counts are taken once per (list, bucket) on `Dispatchers.Default` and only while a length
  is on — a word count decodes a document); **a writing goal with its own evening nudge**
  (`AppPreferences.getJournalGoal`, `JournalGoalReminderScheduler` + `JournalGoalReminderReceiver`
  + manifest, and the day's rail in the journals head); **"open today's page"**
  (`rememberJournalDoor` — today's page if the day was written, a new one if it was blank, or
  the last page opened; the editor records the last id as a page loads). **No SQL change and
  no migration anywhere in §34.**
  **STILL OPEN:** *read-aloud with a speed and voice picker* — **done in §35**.
  *dictionary provider choice* — **closed by §37** (Wiktionary stays; it now suggests
  spellings and reads the selected passage). Also closed: which part "the highlight pill
  selector in a pdf" means — §37 answered it (the dock after a selection, fixed above).
- **§32 — "continue and still the pdf reader buttom sheet close is weirdly slow ... do the motion token set and one arrival for every floating pill ... and in pdf reader, a high charge save turns on" (DONE, v439).** Built: the sheet close now travels its OWN height (the "weirdly slow" was 60%-of-screen travel on a 200dp sheet, not the clock — see `app/AGENTS.md` v439); the pill clock added to `CurioMotion` and spent across the reader's and journal's floating furniture; the back button is its own 50dp circle pill; press feedback on the reader's two control builders; and **low power reading** (`ReaderLook.lowPower`, on by default, a real "Power" row in the reader's settings — RGB_565 pages, a 1.5× upscale cap, `beyondViewportPageCount = 0`, and `cacheDir/book-images` pruned as the reader closes). **Also fixed the red build that was pushed as `147a2516`**: `PersonalPage.kt:722` had an orphan `else MaterialTheme.colorScheme.background` left by v438's edit — the file's own paper is `journalPaper()`. No SQL change for anything in either round.

- **§31 — the reader's polish round two (PARTLY DONE).** Done from it: back
  no longer exits the reader (`BackHandler`), the settings head wears the reader's top
  floor, the night dim covers the tools, the ⋯ tiles are a glyph-only capsule with the
  name outside it, an empty marks list is an em dash, one word gets the whole selection
  bar back, and the page slider has the foot to itself. **Committed as `2db57fc0` and
  deliberately NOT pushed** (the member's instruction). Not built: the motion-lock pill +
  removing the Zoom slider, the copy box's restyle to the dock's language, the motion
  token set / one-arrival / press-feedback passes, one empty state everywhere, and the
  two zoomed-gesture bugs. **(All of those landed in v439–v442.)**
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
