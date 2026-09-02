# Prompt.md — current request log

## Request: Improve book notes, chapter UX, reveal speed + book morph

User request (verbatim, condensed):

> improve the book notes dialog box (the dialog looks too slim and is bad
> looking) and show the book cover in the dialog box too. In Topic Reveal the
> synopsis is scrollable — fix that first. The chapters box is too tall even
> though it only shows 2-line previews — fix that too. The chapter description
> dialog should instead be a bottom sheet: opening a chapter shows ALL
> chapters in the chip row, the opened chapter is shown, and you can switch
> chapters within the bottom sheet. Elsewhere (shuffle main card detail) the
> quick fact + metadata are ready instantly, but the reveal takes a second to
> show them even after I've opened it once — fix that. The morph animation is
> smooth when a book has no synopsis/chapters but laggy when it does — fix
> that too.

### What shipped (this turn) — all in `features/reveal/TopicRevealScreen.kt`

1. **Instant reveal** — `resolved` is now SEEDED synchronously from the warm
   in-memory loader cache on the first frame
   (`TopicJsonLoader.cached(cat.id)` → strict/saved-name match inside
   `remember(topicName, cat.id)`), so an already-warmed/seen topic shows its
   quick fact + metadata immediately; the async Room→JSON→cross-lane chain
   still runs to enrich + `rememberTopic`-persist, and `init()` is skipped
   via `TopicRepository.isInitialized()` on warm starts.
2. **Smooth book morph** — the whole `BookInfoSection` is gated on a
   `bookUiReady` flag (fires ~380ms after entry, matching the
   critically-damped spring settle), so the poster Coil decode + chapter
   LazyRow never compete with the shared-element card expansion frames.
3. **Book notes bottom sheet** — `RevealDetailDialog` (the slim centered
   dialog) is DELETED. New `BookNotesSheet` (ModalBottomSheet: containerColor
   `curioDialogContainerColor()`, 28dp top corners, 880dp max width) covers
   BOTH cases: header = book cover (`BookCoverPoster`, same URL + Open
   Library fallback as `BookCoverFetch.coverUrlFor` so the disk-cache key
   matches the Settings bulk fetch) + BOOK NOTES eyebrow + title/byline +
   close pill. SYNOPSIS mode = full synopsis body; CHAPTERS mode = EVERY
   chapter in a LazyRow chip row (active accent-filled, auto-scrolled into
   view), tapping any chip switches the reader in-place (screen-level
   `selectedChapter` state; the sheet instance never dismisses mid-switch).
4. **Page fixes** — the synopsis card's fixed-height inner scroll box is gone
   (full text, card grows, poster top-aligned beside it); chapter chips
   shrank 156→118dp (title 1 line, padding 12→10).

### Docs

- Changelog (`20260921.txt`) — 4 new top bullets (sheet, synopsis/chips,
  instant reveal, smooth morph).
- `app/AGENTS.md` — UI section: **v315 — book notes bottom sheet + instant
  reveal + smooth book morph** bullet after the v141 morph bullet.

### Verification

- Imports added: ModalBottomSheet, rememberModalBottomSheetState,
  BottomSheetDefaults, heightIn, widthIn, rememberLazyListState,
  CurioContentMaxWidth, BookCoverFetch (KDoc ref); removed the now-unused
  `androidx.compose.ui.window.Dialog`.
- Token-balanced after stripping strings/comments (1058/1058, depth 0 —
  the raw `(`/`)` skew is pre-existing comment prose).
- `cat.onAccent()`, `curioDialogContainerColor()`, `CurioContentMaxWidth`
  all confirmed to already be in use in the codebase.
- CI compiles on push (no Gradle in this environment).