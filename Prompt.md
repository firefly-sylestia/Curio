# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request

> "you can merge this to the main branch and all commits nand push. skip the previous work, and
> redesing the chapter and progress view in the book view and redesign the way i save im on etc,
> also i am not able to chnage the pages update from there, also when marked finished they should
> be automatically finished too. and when unmark restroing the previous marked. also home abut a
> white app bacgroud as the main, but cream cards reverse of what we did, and add soft borders
> where youve added borders for cards in profile and settings also give proper space and all. and
> bring back icons for online mode reycle bin, updates and support and diagnostics. and then we
> will be introducing new accent colors."

### Decisions already taken with the member (ask_user, earlier in the session)

1. **The paper flip is APP-WIDE and REVERSIBLE** — "app wide and as a toggle to reverse it",
   which is why `Appearance ▸ Paper` exists instead of a hardcoded swap.
2. **Finishing a book closes its chapters too**, and the place the member had reached is
   remembered so un-finishing restores it ("All chapters done + remember my place").
3. **A hand-set page is persisted on the BOOK** ("Persist it on the book, don't touch the
   reader").
4. **The chapter list shows its state** ("Auto state: passed / reading / written").

### Merge / push

`main` already carried the drawer + curiosity commit (`301fac2b`, pushed) when this request
started, so there was nothing left to merge — the branch `feat/drawer-and-curiosity-redesign`
and `main` point at the same commit. The new work was committed straight onto `main` and
pushed with it.

## 2. What the code actually looked like (findings)

- **The page stepper was writing everything EXCEPT the page.** `onPage` in
  `BookDetailScreen` saved a whole book row (`pageCount = filePageCount`, a moved
  `currentChapter`) and never a page — while the number the card DISPLAYED came from the
  reader's `ReaderMarkKind.POSITION` row. So a hand move either did nothing visible or
  changed the chapter under the member's fingertip. That is the reported bug, exactly.
- **There was no column for a hand-set page.** `PersonalBookEntity` had `currentChapter`
  and `pageCount` only; the reader's position row was the sole answer to "what page".
- **"Mark finished" only set a timestamp.** `setFinished(bookId, finished)` wrote
  `finishedAtMillis` and nothing else, so a finished book kept its mid-book progress and
  un-finishing had nothing to restore (it just left the old number alone).
- **The card was two strips plus three unlabelled steppers.** A page bar AND a chapter tick
  row, each answering "how far" in its own units, over three bare numbers ("I'm on 12") with
  no word saying which number was which.
- **The chapter rows said nothing about where they sat** — the same row at chapter two and
  at chapter forty, so the run could only be found by counting.
- **v408 left ~25 hardcoded `Color.White` light-branch card fills.** They were the card
  colour when the page was cream; on a white page they would vanish. A literal cannot follow
  a paper switch, so each became a scheme token.
- **The four "front door" settings rows had no icon tile** (`SettingsOptionRow(plain = true)`
  skipped it) — the member wants the icons back without losing the three-line copy.

## 3. What was done

### Books (`data/PersonalEntity.kt`, `data/PersonalDao.kt`, `data/CurioDatabase.kt`, `features/personal/BookDetailScreen.kt`)

- **Schema (migration 19→20, `MIGRATION_19_20`):** `personal_books` gains `currentPage`
  (the book row's own page mark), `chapterBeforeFinish` and `pageBeforeFinish` (the remembered
  place, `-1` = nothing kept — an already-finished book answers honestly instead of guessing).
- **`setPage(bookId, page)`** is the column-scoped write a hand move makes; the reader's
  position row is never touched. `ProgressCard`'s `lastPage` is now "the most recent answer":
  the book row's page when it was written after the reader's row, else the reader's — so a
  move sticks in either direction and reading on takes the card back over.
- **`setFinished` is read-modify-write:** finishing stashes the place and closes the list
  (`currentChapter = totalChapters`, `currentPage = pageCount`); "Reading again" restores the
  stashed pair verbatim. `saveChapterNote`'s progress nudge is guarded on `!isFinished`.
- **`ProgressCard` rebuilt:** "READING PROGRESS" label, the chapter name in Fraunces, the page
  line, ONE gauge (the file's pages, else the chapter run) with the fraction as a percent,
  the chapter ticks with "Chapter N of M · M pages", then a hairline and three
  `ProgressStepperRow`s ("I'm on chapter" / "Page of N" / "The book has N chapters"). The
  controls are hidden while finished, because a finished book has read all of it.
- **`ChapterCard` wears its state** (`ChapterReadState` → read / reading now / to read): the
  number's fill, the accent side rule (writing OR reading now) and the subtitle's own word.
- **The call site** passes `progressPage` (the most-recent-wins page) and writes a hand page
  through `setPage` + `setProgress(chapterForPage(page, chapters))` instead of a whole-row save.

### The paper flip (`ui/theme/CurioTheme.kt`, `data/AppPreferences.kt`, ~13 feature files)

- **Two light schemes, one switch.** `CurioWhitePageLightScheme` (NEW DEFAULT: white page,
  cream cards, `surfaceContainerLow` = a card / `surfaceContainer` = a block inside one /
  `surfaceContainerHigh` = a pill inside one) and `CurioCreamPageLightScheme` (the v408 pair,
  kept verbatim). `curioColorScheme()` reads `AppPreferences.paperCreamCardsState`. Dark mode
  is untouched — its black page with plates stepping up is the same rule.
- **`Appearance ▸ Paper`** (`CompactSegmentedRow`, "White page" / "Cream page") +
  a `SettingsDeepRow` so the hub search finds it. `KEY_PAPER_CREAM_CARDS`, the state, the
  getter/setter and the `initThemeMode` seed follow the existing pref pattern.
- **Card fills resolve through the scheme.** 25 sites where the light branch said
  `Color.White` (cards, rows, pills, chat bubbles) now read `surfaceContainerLow`, and the two
  nested ones (a clear button on a row, a pin button on a card) read `surfaceContainer`.
- **Soft edges + room:** light `outlineVariant` 0.11 (cream-page scheme) / 0.10 (white-page
  scheme), `SettingsOptionCard` padding 17/8, `CurioSettingsCard` padding 16/14,
  `CurioSettingsRow` vertical 15.
- **Icons back on the four front-door rows:** `SettingsOptionRow` always renders its icon tile
  now; `plain` means "roomy, three-line copy" and nothing else.

### Docs & release notes

- `app/AGENTS.md`: the card-ladder section is rewritten for v409 (the flip, the switch, the
  "cards resolve through the scheme, never a literal" rule, the soft edge values), the settings
  hub's `plain` bullet no longer claims the icon is gone, and a new v409 books block records
  the page mark, the most-recent-wins rule, the finish/restore contract and the chapter states.
- `fastlane/metadata/android/en-US/changelogs/20260922.txt`: ADD/FIX block at the top.

## 4. Still open

- **The accent-colour pass has not started** — the member's words were "and then we will be
  introducing new accent colors", i.e. the next request, not this one.
- **Nothing here is device-verified.** No Gradle in this environment: every change is
  `node scripts/check_braces.js`-checked, import-swept and diff-reviewed; **CI is the compile
  check**.
- Worth a device pass: the white-page/cream-card pair on a lane wash (Home/Profile/Cabinet),
  the soft hairline at 0.10 on a cream card, and the reading card's rhythm (the gauge, the
  ticks, three stepper rows).
- The `Paper` switch is a member-facing option, not an experiment with an end date; if the
  member later settles on one paper, the losing scheme and the switch come out together.

---

## User prompts

Status: **done and pushed on `main`** (`69e6c6f5`, `aa820f20`, plus this docs commit).

> "you can merge this to the main branch and all commits nand push. skip the previous work, and
> redesing the chapter and progress view in the book view and redesign the way i save im on etc,
> also i am not able to chnage the pages update from there, also when marked finished they should
> be automatically finished too. and when unmark restroing the previous marked. also home abut a
> white app bacgroud as the main, but cream cards reverse of what we did, and add soft borders
> where youve added borders for cards in profile and settings also give proper space and all. and
> bring back icons for online mode reycle bin, updates and support and diagnostics. and then we
> will be introducing new accent colors."

Earlier prompts in this session, all done and pushed: the theme/wallpaper subtle-by-default pass
and mood-board/hero watermark tuning (v407); the pet-hub hold and the recents hold (v407); the
Cabinet animations, empty-state, open book and cover/artist loading (v407); the AGPL licence,
the plain-list Settings hub and the white-card ladder (v408); the library/app-wide redundancy
audit (v408); the drawer + curiosity redesign and the lane grid (v409); the screen-complexity
audit (v409, recommendations only — nothing deleted).

<!-- Next user prompt goes here. This section is never cleared — the pending prompt and its
status stay at the top, and the empty slot below is where the next instruction lands. -->
