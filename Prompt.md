# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> "in book view the reading progress now there are alot of duplicates, just below where t
> says reading progress it says chapter name and page number and then below th eprogress
> and then below that again the chapter out of and pages on the side then below the actuli
> better im on chapter and page keep the last one with the +- button removethe top 2 and
> mak eit one beautiful progress view"

Follow-up answer to the layout question: the card should be **label + gauge + tiles**
(drop the headline and the count row under the bar); the "This book has" stepper stays
but its **view, style and design** should change.

## 2. What the code actually looked like (findings)

`BookDetailScreen.kt` ▸ `ProgressCard` stated the member's place **three times**:

1. **The headline** (under "READING PROGRESS"): the chapter name over "Page 87 of 312"
   (or "Chapter 5 of 40" / "Every chapter closed"). Same fact as the tiles, same units.
2. **The row under the gauge**: "Chapter 5 of 40" on the left and "312 pages" on the
   right — the same two counts again.
3. **The two `ProgressTile`s**: "I'm on chapter" (chapter NAME + "5 of 40") and "Page"
   ("87" / "of 312"), each with its `[−] [+]` buttons — the one the member called "the
   actual better".

Plus a fourth control at the bottom: `ChapterStepper` under the words "This book has",
which borrowed the same chevron-stepper shape as the tiles and read like a third place
you are.

## 3. What was done

All in `app/src/main/java/com/curio/app/features/personal/BookDetailScreen.kt`:

- **The headline column is gone.** The head is now one `Row`: the "READING PROGRESS"
  label on the left (`weight(1f)`) and the single action pill ("Mark finished" /
  "Reading again") on the right. `chapterLabel` and its `when` are deleted with it.
- **The count row under the gauge is gone.** The gauge is followed by the finished line
  only when `finished` ("Every chapter closed" — the one place that state is now said,
  the gauge being full and the action reading "Reading again").
- **The two tiles stay** unchanged in shape and wiring — they are the only places the
  chapter and the page are named, and they carry the `[−] [+]` buttons.
- **`BookLengthRow` replaces the "This book has" row** — a quiet footer rail: an
  uppercase `BOOK LENGTH` label (quiet ink, same 1.1sp tracking as the other labels)
  against one compact pill holding the count between two round `TileStepButton`s
  (`+`/`−`, clamp 0…999). It no longer borrows the tiles' layout, so the setting reads
  as a setting rather than a place you are.

Docs + notes: `app/AGENTS.md` (the v411 card bullet now records the v412 removal and the
footer rail), `fastlane/metadata/android/en-US/changelogs/20260922.txt` (one FIX bullet).

## 4. Decisions

- One `ask_user` round was used because "the top 2" and the fate of the book-length row
  were genuinely ambiguous; the answers fixed both (label+gauge+tiles; keep the length
  control but restyle it).
- No settings toggle: this is a refinement of an existing surface, not a new feature.
- Nothing else in the card's data flow was touched — `onChapter`, `onPage`, `onTotal`
  and `onFinished` still write exactly what they wrote before.

## 5. Status

- Implementation done; brace/paren balance of the file verified (0/0). CI to confirm
  compile on push.
- No pending follow-up.
