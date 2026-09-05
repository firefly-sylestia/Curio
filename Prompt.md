# Request Log — Book/series notes-sheet cleanup (no close, rating under author, tick-free read state)

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user, paraphrased)
- Add an instruction to NEVER put a cross/close button in a bottom sheet.
- Remove the cross close button from the book synopsis (book-notes) bottom sheet.
- Remove the "Your rating" row; put the rating just below the author name;
  use a DIFFERENT icon for the rating (not the star).
- Remove the "tap a chapter to read its note" hint.
- Add an instruction not to add useless notes (filler hint copy).
- Remove the arrow icon from the chapter chips (rows).
- The read-status state shown for chapters is bad — the user dislikes the
  active state with the tick icon.
- When a chapter is expanded AND read in dark mode it looks bad — fix it.
- Question: what other book-cover-fetch APIs are available?

## Ask (ask_user, 4 questions — answers recorded)
1. Rating source under author name: **Both** (Google average + my own rating).
2. Rating icon: **Award ribbon** (`CurioIcons.WorkspacePremium`).
3. Series sheet scope: **Yes, both sheets** (book + series stay in sync).
4. Read state: **Soft accent tint + number** (no tick; disc keeps the number).

## What was done (TopicRevealScreen.kt)
- **Close button removed** from BookNotesSheet + SeriesNotesSheet (swipe/back
  dismisses; heart stays in the header).
- **"REVIEWS & YOUR RATING" card deleted** (both the Google reviews block and
  the BookRatingPicker row); `BookRatingPicker` + `BookGlyph` composables
  deleted with it. `bookRatingVisibleState` pref API stays dormant.
- **Header rating, below author name:** one award-ribbon glyph + "4.2 ·
  yours 4 / 5" (Google avg + custom pick; only what exists).
- **Star → WorkspacePremium** on the reveal hero rating chip and the
  synopsis-card rating chip too (consistent icon everywhere).
- **"tap a chapter/episode to read its notes" suffix removed** from both
  progress rails ("N chapters" / "N episodes" only).
- **Tick gone:** leading chip always shows the chapter/episode number; a
  read/watched row tints the disc soft accent (18% fill + accent number +
  accent ring). Mark-read FoldedCorner = solid accent fill + onAccent icon +
  onAccent rim in EVERY state (fixes the open+read dark-mode punch-hole).
- **▼/▲ chevron removed** from chapter/episode rows (rows still expand on tap).

## Docs
- app/AGENTS.md: durable rules "No close buttons in bottom sheets" + "No
  filler hint copy" (UI section) + versioned **v355** entry.
- fastlane changelog 20260921.txt: 4 concise FIX bullets at the top (no
  REMOVE — the rating picker never reached a pushed release).
- Prompt.md: this log.

## Book cover API research (answer in the reply)
- Open Library Covers API (current fallback, keyless, title/ISBN covers)
- Google Books API (current, keyless or keyed with GOOGLE_BOOKS_API_KEY)
- Alternatives: Open Library Search API, ISBNdb (keyed, ISBN-based),
  OpenBD (Japan), LibraryThing (keyed), Internet Archive / Book Cover
  Archive (via Cover Art Archive or archive.org), Apple iTunes Search
  (keyless, ebooks), Wikipedia/Wikidata (via Special:FilePath / Commons),
  HathiTrust (no key, slow), Amazon (no official API), CovertArt / MusicBrainz
  (albums only), NovelGraphs/MyAnimeList (manga).

## Verification
- Brace/paren balance + all edit regions re-read after replacement; no
  leftover BookGlyph/BookRatingPicker/tap-hint/tick/chevron references in
  TopicRevealScreen.kt. CI will validate the real compile.