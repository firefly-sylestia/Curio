# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "now for the mepty staes of the my shelf door and pages door instead of new buun show like
> today, yesterday or day before date and say write down somethign about them, and for books
> show 3 book suggestions. dont push it yet"

Home's two door rows (`Pages` and `My shelf`), when their list is empty, stop offering a
"New"-style chip and offer subjects instead: the last three days for Pages, three books for
the shelf. **Committed but NOT pushed** — the member's own instruction.

The previous request (the fifteen hidden journal gestures and the gauge's chapter divisions)
was already committed and pushed as `00d8fac0` before this one started; the tree was clean on
arrival.

## 2. What the code actually looked like (findings)

- `PersonalHome.kt` → `PersonalChipsRow` builds both door rows. Each door, when its list was
  empty, emitted ONE `EmptyDoorChip`:
  - Pages: `glyph = Add, label = "No pages yet", caption = "Start your first one"` →
    `onWrite` (Home's writing sheet).
  - My shelf: `glyph = Add, label = "No books yet", caption = "Open the shelf"` →
    navigate to `CurioRoutes.BOOKS` — the same shelf the row already is.
- `EmptyDoorChip` is `CHIP_WIDTH` 96dp × `CHIP_HEIGHT` 118dp: a 34dp accent disc, a label and
  a two-line caption — so a replacement chip has an exact shape to match.
- The journal page seeds its day with `startOfToday()` (`JournalEditorScreen` line ~117) and
  overrides it from the stored row when one exists; the route carries an entry id and nothing
  else, and `PendingCabinetFilter` was the house pattern for out-of-band handoff.
- `startOfToday()` / `shiftDay(millis, days)` are `internal` in `JournalEditorScreen.kt` (same
  package), and a journal day is a calendar day at local midnight.
- `BookCatalog` (Curio's own `BOOKS` lane, ~800 curated books) already exposes `library()`,
  `search()`, `book()`, `bestMatch()` — and every `Hit` carries the real cover, the page
  count and the REAL chapter list. It had no "give me a few" door.
- Creating a book from the shelf's add flow is `addBook(...)` — a LOCAL fun inside
  `BookShelfScreen` (it needs that screen's scope, context and file-import path), so it is
  not reusable; `TopicRevealScreen` and `IsbnScannerScreen` each write their own
  `PersonalBookEntity` + `saveBook` inline instead.
- `BookChip` reads only `title`, `author` and `coverUrl` off the entity it is given, so a
  throwaway entity can draw a book that is not on the shelf yet.

## 3. What was done

### Pages door — the last three days
- The empty row now leads with a new `EmptyDoorLead` — "Nothing here yet" over "Write down
  something about one of these days." — sized and centred like a chip (158dp × 118dp), no
  button of its own.
- Then three `EmptyDoorChip`s from `emptyDayChips()`: **Today**, **Yesterday** and the day
  before (named by its WEEKDAY, e.g. "Tuesday", caption the date, e.g. "18 September"), each
  with the `CalendarToday` glyph.
- Each chip stashes its own local midnight in **`PendingJournalDay`** (new object in
  `CurioRoutes.kt`, modelled on `PendingCabinetFilter`) and navigates to
  `journalEditor(PERSONAL_NEW)`; the journal's date seed now reads
  `PendingJournalDay.take() ?: startOfToday()`, so the page opens ON the day the chip said.
  A saved page still overrides it with the date it was written on.

### My shelf door — three book suggestions
- The empty row leads the same way ("Nothing here yet" / "Pick one to start with.") and then
  draws **three real books** from the catalog with the ordinary `BookChip` cover chip.
- New `BookCatalog.suggestions(count = 3)`: shuffles the catalog with a **seed from the
  current day**, so the three are stable all day (a strip that reshuffles under a finger
  reads as a glitch) and fresh tomorrow.
- Tapping one calls the new `shelveSuggestion(hit)` — a `PersonalBookEntity` with the
  catalog's title, author, cover, page count, `catalogId` and `chapterCount`, stamped with
  `createdAtMillis`/`updatedAtMillis` so it sorts as the newest book — then navigates to the
  book's own page. A `shelving` flag guards the write, since a double tap would shelve the
  same book twice; the empty state lasts exactly as long as that write (both rows read the
  same flow).
- The suggestion list is read only while the shelf IS empty (`produceState(…, books.isEmpty())`),
  so a member with books never pays for the catalog parse.

### Cleanup
- `PersonalChipsRow`'s `onWrite` parameter had exactly one caller — the chip that was just
  replaced — so it is deleted along with the argument at Home's call site, rather than left
  behind as a dead one. The writing sheet keeps its real door: Home's floating "+"
  (`PersonalCreateLauncher` → `writeSheetOpen`).

Docs + notes: a new bullet in `app/AGENTS.md` (what an empty row offers, `PendingJournalDay`,
the day-seeded suggestions, the guard flag, the deleted parameter) and a changelog group
(two ADD, one FIX).

## 4. Decisions

- **"Day before" is named by its weekday.** The member wrote "day before"; at a 96dp chip
  with `labelLarge` type, "Day before yesterday" either wraps or shrinks, and the weekday plus
  the date under it says the same thing more precisely. Flagged here in case they want the
  literal words.
- **The lead-in line is not a button.** The sentence the member asked for ("write down
  something about them") is a lead-in to the three days, and the chips under it are the
  actions — a text that also navigated would be a second, invisible door.
- **Three days, not "the last three days you have nothing for".** Days are a fixed, learnable
  set (Today / Yesterday / the day before) and match how a diary is written backwards from;
  scanning the store for empty days would make the row's meaning depend on data.
- **Suggestions come from Curio's own catalog, not Open Library.** It is offline, curated, and
  every book arrives with a real chapter list and page count — which is what makes a
  one-tap suggestion become a *complete* book on the shelf rather than a stub to enrich later.
- **Day-seeded, not per-composition random.** Stable under a finger, new each day.
- **The `onWrite` parameter was removed rather than kept.** A dead parameter on a shared
  composable is worse than a smaller signature; the write sheet's primary door is untouched.
- No ask_user: the member's instruction was explicit about both rows and about the push.

## 5. Status

- Implemented; brace/paren balance verified (0/0) on every edited file. No Gradle in this
  environment, so CI validates the compile.
- **Committed, NOT pushed** (member: "dont push it yet") — the tree is one commit ahead.
- Open: push when the member asks; CI then validates it together with the next change.

---

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none — this request is logged above as §1–§4)
