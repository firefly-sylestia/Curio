# Request Log — book chapter data repair (names / numbers / progress)

## Status: implementation complete — committed & pushed (CI pending)

## The request (user, paraphrased)
- The chapter audit missed many books with wrong chapter names.
- Many books still show wrong chapter numbers in reading progress; grouped
  entries ("Chapters 3-5") count as one chapter.
- Heaven (Kawakami) has the wrong chapter data too.
- Fix all.

## Root causes (found by analysis)
1. **Moby-Dick**: `number` was offset one below the title's own chapter number
   from ch. 57 on (duplicates at 56 and 90) — progress/jump-by-number broke.
2. **Grouped entries**: several books stored whole ranges as one entry
   (Divine Comedy 4 cantos-groups, Dead Souls "Part I: Chapters 1-5", Lady
   Susan "Letters I–V", The 48 Laws "Laws 1-12", Neruda "Poems 1–10",
   Roman Stories "Stories 1–3", Inferno canto groups) — progress counted them
   as ONE chapter each.
3. **Heaven**: the novel has 9 unnumbered/untitled chapters; the data had 10
   with invented titles ("The First Note", "Whale Park"…).
4. **20 Section-A classics** (web-verified last session) had 3-15 coarse
   beat-level entries instead of their real chapter counts.
5. **Schema split**: 989 entries across 142 books used `chapterNumber`
   instead of `number` (loader fell back to positional, so rendering worked
   but the data was split-brained).
6. **Display**: the reader showed the positional index ("CH 5 · Chapter 1"
   for Frankenstein / Graveyard Book) because titles carried the real label.

## What was done
### data/topics/books.json (5,418 -> 6,790 chapter entries)
- Moby-Dick renumbered to its own titles (135 + Epilogue = 136).
- Grouped ranges split into individual entries:
  The Divine Comedy 4→100 cantos, Inferno 7→34, Dead Souls 4→15,
  Lady Susan 4→42 (41 letters + Conclusion), The 48 Laws of Power 4→48,
  Roman Stories 3→8, Neruda 2→21 (20 poems + the song).
- Heaven → 9 chapters ("Chapter 1..9", accurate plot summaries kept, last two
  merged), invented titles/pages dropped.
- Section-A expansions to real verified counts (Two Towers 19, Fellowship 22,
  Return of the King 22, Lucky Jim 25, Grapes of Wrath 30, Wuthering Heights
  34, Madame Bovary 35, Tom Sawyer 35, Jane Eyre 38, Huckleberry Finn 43,
  Tale of Two Cities 45, Crime and Punishment 48, Dandelion Wine 51, Oliver
  Twist 53, Emma 55, Great Expectations 59, Game of Thrones 73, Color Purple
  90 letters, Don Quixote 126, War and Peace 361).
- Schema unified (`chapterNumber` → `number`, 989 entries) + every book's
  chapter numbers are sequential ints 1..N. Validated: 796 topics, 0 errors.

### App (TopicRevealScreen.kt)
- `chapterDisplayLabel()` derives the real label from the title ("Chapter 57",
  "Letter I", "Part II", "Canto 1 (Inferno)", "Epilogue"…) and the chapter
  chip / sheet header / Mark-read button use it instead of the raw position,
  fixing Frankenstein/Graveyard-Book-style offset displays everywhere.

### Audit / docs
- books-chapter-audit.txt: appended a "v340 — FIXES APPLIED" section.
- tools/fix_book_chapters.py committed (rerunnable).
- Store changelog updated (2 FIX bullets).

## Notes / follow-ups
- Still open: ~530 books whose chapters are part/volume-level (Part I/II/III,
  Book I-XII…) — real divisions but coarse; expanding needs per-book
  research. Books with genuinely TITLED chapters (Game of Thrones, Don
  Quixote, The Color Purple) now have correct counts but plain "Chapter N"
  titles (real names need per-book sources).