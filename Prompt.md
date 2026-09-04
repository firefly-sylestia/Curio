# Request Log — book chapter audit + cover-art sheet palettes

## Status: implementation complete — committed & pushed (CI pending)

## The request (user, paraphrased)
1. Re-analyse the book chapter names/counts again properly (many were wrong); tell how
   many are wrong and list them in a `.txt`.
2. Re-read the DOX (master.md + AGENTS.md chain) — stop putting the Codebuff tool footer
   on commit messages; plain conventional-format messages only.
3. The book/album notes-sheet background colours from the cover art are bad; derive a FULL
   palette from the cover (background + cards + chips + text, each element its own role)
   instead of one dominant tint; keep the category fallback when no cover is fetched.

## What was done
### 1. Chapter audit → `books-chapter-audit.txt` (committed)
Analysed `data/topics/books.json` (796 books, 5,418 chapter entries):
- SECTION A — 20 books with CONFIRMED WRONG chapter counts (web-verified real structures,
  e.g. War and Peace 361, Don Quixote 126, Great Expectations 59, The Two Towers 19, …).
- SECTION B — 530 books with ≤5 "chapters" (part-level / fabricated divisions for novels;
  essays/poetry legitimately have few).
- SECTION C — 49 books with bare "Chapter N" placeholder titles.
- SECTION D — 142 books using the `chapterNumber` schema variant instead of `number`
  (app falls back to positional numbering so they render, but data is split-brained).
- SECTION E — 8 duplicate chapter titles + 5 mixed-format books.
- 657 of 796 (~83%) flagged overall; the audit also lists count-verified-correct books.

### 2. DOX / commit hygiene
Re-read master.md + root AGENTS.md + app/AGENTS.md. Commit messages are now plain
conventional format with NO tool/Codebuff footers (this commit follows that rule).

### 3. Full cover-art palettes for the notes sheets
- `CoverPalette.kt`: new `CoverSwatches` (all six androidx-Palette slots) +
  `fetchCoverSwatches` (192px decode); `fetchCoverSwatch` kept as a wrapper.
- `CategoryInk.kt`: new `CoverSheetPalette` (container / surface / surfaceHigh / surfaceAlt /
  accent / onAccent / ink / onSurface / onSurfaceVariant / onSurfaceAlt) +
  `CurioCategory.notesSheetPalette()` — classic album-art recipe (vibrant pops as accent,
  dark shades anchor the bg, light shades lift cards, text tones from wash lightness),
  dark + light recipes, same Material-theme / tint-wash gates as before.
- `TopicRevealScreen.kt`: BookNotesSheet + AlbumNotesSheet fetch the swatches and map every
  colour role (container, cards, chips, tabs, progress bar, read-chips, close button, text,
  listen/genius pills, track hearts) to the palette with per-element category fallbacks;
  AlbumSynopsisAccordion now takes the resolved colours. No cover / fetch off / Material
  theme → exactly the old behaviour.

## Notes / follow-ups
- The audit is a report only — the actual chapter-data repair (real counts, titles, schema
  unify) is a large data task left for a follow-up request.
- `notesSheetContainerColorForCover` is now dead in CategoryInk (kept; harmless).
- CI validates compile; the palette math is only visible on-device.