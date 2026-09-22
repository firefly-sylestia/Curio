# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session) — §37

> the 3 dot for reader its too empty spae and not proper spaced, fix its weird look, add at
> sunset customisation to be able to set the tiem, the zoom lock is bad it also locks the
> touches fix it, and also the side tap gesture its slow doesnt work faster receives one ta
> and doesnt work anymore and doesnt work sometimes fix it, imrpove the discoonary that it
> suggest work explanation from the selected para, make the word detection better it detects
> the work even theres a comma or something or a mis type. and still the buttom sheet closing
> is bad, and the highlight dock is bad fix it too. weird shado and doesnt match the dock,
> also theupper header animation is clanky

Nine things, all on the reading surface:

1. **The ⋯ menu** — too much empty space, badly spaced; fix its weird look.
2. **Night dim "at sunset"** — the member must be able to SET THE TIME.
3. **The motion lock** — it also locks the touches.
4. **The side tap zones** — slow, one tap then dead, sometimes nothing at all.
5. **The dictionary** — suggest word explanations from the selected paragraph.
6. **Word detection** — survive a comma, a possessive, a typo.
7. **The bottom sheet's close** — still bad.
8. **The highlight dock** — a weird shadow, and it does not match the dock.
9. **The header's animation** — clanky.

Touched: `features/personal/BookReaderScreen.kt` (11,145 lines), `ReaderDictionary.kt`
(the lookup itself), `ReaderSettingsScreen.kt` (the second surface that sets the dim).
`web/` and `desktop/` untouched, per root `AGENTS.md`.

## 2. What was actually wrong (found by reading, not by guessing)

1. **⋯ menu** — v437 gave EVERY reader sheet a 45% floor of the screen
   (`ReaderSheetFrame.minHeightFraction`). A floor is right for a LIST (it grows, and two
   kept marks would otherwise read as a broken panel) but the ⋯ grid is six FIXED tiles of
   the same height whatever happens, so 45% was two thirds empty paper. The tiles' gutter
   was 8dp, which nearly closed once their labels were the widest thing in the row.
2. **The dim** — v440's "At sunset" was the phone's own dark theme (`isCurioDarkTheme`).
   That answers `dimAuto = true/false` and nothing else; the member asked for the time.
3. **The motion lock** — the guard consumed EVERY event in which any finger had moved at
   all (`it.position != it.previousPosition`). A finger that taps or holds is never
   perfectly still, so the first pixel of jitter was consumed; a consumed move cancels
   `detectTapGestures` (and its pending long press), which is how the chrome could not be
   brought back, the mark dock could not open and a sweep reported nothing on a locked page.
4. **The side taps** — the zones were answered by the reading surface's own
   `detectTapGestures`, which ALSO owns the double tap (`readerDoubleTapZoom`). A detector
   waiting to see whether a second tap follows cannot answer the first one until the
   double-tap window passes (⇒ "slow"), and a second tap inside that window was read as the
   FIRST HALF of a double tap (⇒ "receives one tap and doesnt work anymore"). Where a child
   claimed the gesture first, nobody heard it (⇒ "doesnt work sometimes").
5. **The dictionary** — the sheet had a single word field and nothing else: a swept
   PASSAGE had no word in it, and the hold dock seeded it by picking the passage's first
   letter-bearing token, which was the reader guessing which word the member meant.
6. **Word detection** — `define()` asked Wiktionary for the selection VERBATIM, so
   `"Einstein,"` or `"word."` was asked for as a headword that does not exist, and a typo
   was a dead end with no spelling offered.
7. **Sheet close** — the only door out was DISTANCE (108dp of deliberate dragging), so the
   gesture everyone makes on a sheet (a quick downward flick) dragged a few millimetres and
   sprang back. The distance was also a long way to pull for a panel a few rows tall.
8. **The highlight dock** — its fill was `palette.surface` (F5F0E8) floating over
   `palette.paper` (FBF6EC), about two per cent apart, so the only part of the capsule the
   eye could see was a shadow spread over pale paper on every side. It is the same defect
   v441 fixed on the page slider, one surface over.
9. **The header** — its exit carried THREE transitions at all times (fade + drift +
   `shrinkHorizontally` toward the right edge), so merely HIDING the chrome collapsed the
   name capsule into the corner while it was leaving upward.

## 3. Decisions, all confirmed before editing

Three questions were asked (the rest had one reading each):

1. **"At sunset" with a settable time** → the member chose **two times, FROM / UNTIL**
   (not a preset list, not a single time).
2. **The dictionary from a selected passage** → **chips + a context line** (chips for the
   words, and the sentence the answered word stood in quoted beside the meanings).
3. **The ⋯ sheet** → *"scrink it but dont make it too lose to th buttom"* — a smaller
   panel, still standing clear of the foot of the glass.

No new feature was added toggleable-or-not: every one of the nine is a fix or a refinement
of behaviour that already shipped, except the dim's FROM/UNTIL pair, which lives inside the
existing "At sunset" mode rather than as a new capability of its own (root `AGENTS.md`'s
"ask: toggleable or not" applies to ADDING a measure).

## 4. What was built

### 4.1 `ReaderDictionary.kt` — the lookup

- **`headword(raw)`** — one function that turns what the sweep actually caught into the
  word to ask for: any run of `EDGE_PUNCTUATION` off both ends (both quote families, both
  dash families, brackets, the sentence's punctuation — never the hyphen or the apostrophe,
  which are letters' business INSIDE a word) plus the possessive tail (`Einstein's` →
  `Einstein`, with a three-letter stem minimum so `it's`/`he's` keep their own page).
  `define()` now asks for `headword(word)`, so a comma or a possessive can no longer make a
  real word look unknown.
- **`suggest(term)`** — Wiktionary's own `opensearch` (the same keyless family the
  definitions come from, so no second service to trust) for the spellings nearest what was
  asked. Word-shaped answers only, the word asked for is never echoed back, and misses are
  memoised like the senses are.
- **`wordsIn(passage)`** — the words a passage is worth asking about: sentence-split
  (`SENTENCE`), five letters up, `STOP_WORDS` excluded, never the same word twice, capped at
  `MAX_SUGGESTIONS`. Each carries the sentence it stood in (`ReaderDictionaryWord`).

### 4.2 `BookReaderScreen.kt` — the reader

- **The dictionary sheet** offers the passage's words as chips and quotes the sentence
  (`readerContextFor`). A miss now asks for the nearest spellings and takes the first that
  HAS a definition, labelled under "Did you mean" so the member can see which word answered.
  A one-word selection still arrives ready to look up; a PASSAGE arrives with the field
  EMPTY, because seeding one of its words would be the reader guessing.
- **The motion lock wears in** — nothing is consumed until the finger has travelled the
  touch slop (the same rule the magnified page already used), so on a locked page a tap
  raises the tools, a hold opens the dock and a sweep can report a word, while a real drag or
  a second finger is still swallowed whole (consuming is what keeps the page still — an
  ignored drag would be taken by the column or pager underneath).
- **`Modifier.readerZoneTaps`** — the side zones answered by their own gesture handler,
  placed INNERMOST in the chain so it sees the finger lift before the surface's double-tap
  detector, and it CONSUMES the up it answered so that wait is cancelled: one tap, one page
  turn, never a zoom. Three guards make it a tap and only a tap: a consumed down (a real
  control) is skipped, a press past the long-press threshold is the sweep's, and a gesture
  something else consumed mid-flight never reaches the lift. Wired into all four surfaces
  (the reflowed text, the PDF column, the PDF page, the paged flow), each with the space its
  own taps are measured in.
- **The sheet's close** — the finger's own travel per millisecond is measured on the head's
  drag AND in the body's nested scroll (`flickPeak`/`flickAt`, plain arrays: nothing draws
  them), and a throw past `dismissFling` (620dp/s) shuts the sheet without the pull having to
  reach the distance; the distance itself is 96dp now (was 108). A pull that does not reach
  either springs straight back on the exit clock, and a re-entrancy guard means the flick and
  the settle after it cannot both dismiss the same sheet. **No `onPostFling` override** — the
  velocity handler is a second, engine-versioned way to learn what the finger's clock already
  says.
- **The ⋯ menu** passes `minHeightFraction = 0.30f` (still a real panel, clear of the foot),
  the tile rows get 14dp of air with a wider 10dp gutter between tiles and a breath under the
  last row.
- **The highlight dock** wears the reader's own pill body — an OPAQUE
  `lerp(surface, ink, 0.06f)` fill with a hairline `lerp(surface, ink, 0.16f)` edge, the same
  28dp radius and 8dp lift the page slider wears (v441). Being opaque, the shadow cannot bleed
  through it; `animateContentSize` deleted with it (the bar is full width and its height never
  changes, so it cost a layout pass per frame for nothing).
- **The header** has one exit per reason: the sideways shrink (toward the End) belongs to the
  SEARCH opening alone — which is what makes the bar read as growing out of the corner the
  search icon lives in — and every other exit (a tap on the page hiding the chrome) is the
  plain pill leave. Both clock on the ENTER clock so the head and the search bar are one
  movement rather than two panels changing places. The foot and the head are now written as
  `CurioMotion.pillArrive()` / `pillLeave()` tokens instead of their numbers.
- **The dim's window** — `ReaderLook.dimFromMinute` / `dimUntilMinute` (minutes since
  midnight, default 20:00 → 06:00) with `dimWindowContains()` reading a window that runs over
  midnight as the normal evening case and equal ends as "all day". The reader ticks its clock
  every 30s while the mode is on (`LaunchedEffect(ReaderLook.dimAuto)`), because a member
  reading at 19:59 with the dim due at 20:00 would otherwise keep a bright page for minutes.
  Both new fields are in `rememberKey()` and in the store (`reader_dim_from`/`reader_dim_until`).
- **`ReaderClockRow` / `ReaderClockDialog`** — one row, one label (`readerClockLabel`, "20:00")
  and one Material `TimePicker` dialog, `internal` in this file so the appearance sheet and
  reading settings wear the same pair and can never disagree about what "at sunset" means.

### 4.3 `ReaderSettingsScreen.kt`

The same `ReaderClockRow` pair under the same segment, with the clock dialog hoisted into the
screen's composable scope (it is a Dialog, not a panel in the column).

## 5. Checks run

- **No Gradle command**: this environment forbids compile / build / lint (root `AGENTS.md`).
  Validation is CI on push.
- Verified by reading the real definitions before use: `CurioMotion.pillArrive/pillLeave`
  (`fromTop`), `ReaderSheetFrame`'s parameter list, `ReaderZoneAction`, `readerZoneActionAt`
  and every call site of `readerZoneTaps`; confirmed `CurioIcons.Schedule` is a real bundled
  glyph (it is in `historyHeroSymbols()`), that `TimePicker`/`rememberTimePickerState` need no
  local `@OptIn` (the module opts into `ExperimentalMaterial3Api` in `build.gradle.kts`), and
  that `waitForUpOrCancellation`'s package is right (`RichTextEditor.kt` imports the same one).
- **Every new import checked against the file's own use**; `Velocity` was removed with the
  `onPostFling` override that used it, `SystemClock` added for the flick's clock.
- **A bracket-balance pass over all three files** (string/comment aware): zero unbalanced
  brackets, which is the one syntax fault a large hand-edit can hide.
- Progress persisted in Prompt.md (this file) and to be recorded in `app/AGENTS.md`.

## 6. Still open

- **The journal's paper following its colour by default** — the "Paint the page too" switch is
  still deliberate; making it the default needs the member's word.
- **Read-aloud is done (§35)**; the dictionary provider question from §34 is closed by §37's
  own answer (Wiktionary stays, and it now suggests spellings and reads the passage).
- **No SQL change and no migration anywhere in §37** — every change is UI, gesture, dictionary
  or motion, and the two new look fields are SharedPreferences keys like the rest of `ReaderLook`.

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- **§37 — the nine reader fixes, above (DONE, v442).** The ⋯ sheet sized to its own tiles; the
  dim's FROM/UNTIL window with one shared clock row and picker; the motion lock's wear-in so a
  locked page still hears a tap, a hold and a sweep; the side taps answered by their own
  innermost handler (fast, repeatable, and never read as a double tap); the dictionary
  suggesting the passage's words as chips with the sentence quoted, plus spelling suggestions
  on a miss; `headword()` so a comma, a possessive or a stray quote no longer hides a real
  word; the sheet shutting on a flick as well as a distance, with a longer pull needed nowhere;
  the highlight dock wearing the reader's own opaque pill body instead of a shadow over
  near-identical paper; and the header's exit reduced to one motion per reason, written as
  `CurioMotion` tokens. **No SQL, no migration.** See §4 above and the v442 section of
  `app/AGENTS.md`.
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
