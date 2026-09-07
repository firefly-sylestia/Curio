# Prompt Log — current request

## Request (2026-09-07, active → v3xx8 committing)

Combined share-card + settings batch on top of the pushed v384b commit.
No blocking questions needed: every item was a concrete fix or a
relocation the user explicitly asked for. (The sticker feature is opt-in
per card by design — a card with no stickers renders exactly as before —
so no toggleable/always-on ask applied.)

**What the user asked + what was done:**

1. **Emoji stickers in the share card (full-screen editor only).** The
   full-screen editor's top bar gains a **Stickers** button (between
   Layout and Text) that opens an inline panel: a 48-emoji picker row +
   size slider + **To front / To back** (z-order) + Delete for the
   selected sticker. Stickers are `ShareSticker(emoji, x, y, sizeFrac)`
   with card-FRACTION positions/sizes so the sheet preview, the full-screen
   editor and the exported PNG all match. `TopicShareCard` renders the
   sticker layer on top of every style (new `stickers` param threaded
   through all 5 call sites incl. both exports); the interactive edit
   layer (`StickerEditOverlay`, shown only while the sticker tool is open,
   drawn over the ArrangeableCard chrome) makes each sticker tap-to-select
   / drag-to-move, clamped inside the card. List order = stacking order.
   Persisted per topic via `saveShareCardEdits` (`stickers` JSON array),
   cleared by Reset-all and on full-screen close.

2. **Book browser in Settings.** The horizontal "All covers" LazyRow
   strip (CoverTile) is REMOVED from the Book covers & ratings hub. A new
   **Book browser** screen (`features/settings/BookBrowserScreen.kt`,
   route `SETTINGS_BOOK_BROWSER`, NavHost-registered) lists every book as
   a scrollable line-by-line row — cover thumbnail, name, author · year,
   cached ★ rating + count, chevron → opens the book's reveal. Reached
   from a new "Book browser" row in Experiments → Content tools.

3. **Share-picture fix.** The Share dialog's "Include a link" mode posted
   TEXT ONLY (caption + raw URL). It now ALSO attaches the card PNG via
   `shareComposableCard` with `shareText`, so the picture is never lost
   to a bare link post.

4. **Telegram hidden-link format.** The link text now uses `[Topic](url)`
   syntax — Telegram renders the topic name as the tap target with the URL
   hidden behind it (the format from the user's `[1, 2, 3]` citation
   example). Applied in the include-link share body; helper copy updated.

5. **Sparkle (auto-layout) repair fixes.**
   - **Info row between title and fact:** `metaLift` is now SIGNED — a
     grown/dragged quick-fact box covering the author/year rows LIFTS THE
     ROWS UP back above the fact instead of dumping them below it; when
     the lifted rows have no room the TITLE moves up a little for them
     (`titleForMeta` from the measured title→meta gap).
   - **Out-of-card clamp:** anything whose measured card-local rect hangs
     off the card (left/top < 0 or right/bottom > card) is pulled back
     INSIDE on the same tap (never re-centred) via new
     `fixTitleX/Y … fixFavX/Y` deltas applied to each element's own dx/dy.
     `autoLayoutPlan` gained `cardW`/`cardH` (the 280dp preview box).

6. **Handle (grip) fix.** `MoveHandle` now sits at `zIndex(10f)`, so it
   ALWAYS wins the touch: a selected box that overlaps its neighbour is
   drawn at zIndex 2 and used to steal the handle's drags (the "handle
   doesn't work when boxes overlap" bug). Tapping/dragging the grip always
   moves its box now.

**Out of scope / open:** sticker editing is full-screen-only by design
(the bottom-sheet preview and exports just RENDER the stickers — no
interaction there); exact on-device sticker sizing (emoji font metrics)
needs device verification — CI validates compilation only. The web/ and
desktop/ ports were not touched (Android-only scope).