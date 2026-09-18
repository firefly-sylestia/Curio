# Prompt Log — current request

## Request (2026-09-18, batch Z5 — prints in rows, and the carry that says where)

Verbatim: "do the attachment inside a paragraph, aand more than one photo pair, also make the
animation of it good, with proper hover preview same pass for voice recorder in animation, and proper
preview of photo stacing with snap."

Context: the user's earlier answer to the layout question was "truly inline: a thumbnail sitting
between the words, with the text wrapping around it; a grid 2*2 but for 3 dont make it one big and
all see how we can do portraight and then 2 small can be fit to the other side kinda stylish, but this
should be auto adjust, but user can do any size chnages etc manually, also for portraight add one more
small portraight view too", and for the drag: "same, plus a magnified thumbnail of the photo under the
finger".

**Status: three of the four landed in this batch; the fourth is BLOCKED ON A DECISION (see below).**

### 1. More than one photo pair — the row of prints (v396, done)

Consecutive prints used to pair two-at-a-time, one pair only. They now group into a RUN of up to four
consecutive prints and the run lays itself out by how many arrived (`PersonalPrintArrangement`):

- two — even halves of the measure (a pair);
- three — one upright frame with the other two stacked beside it (whatever size ASKED to stand up
  takes the tall slot; with none asking, the first does, so the shape is never explained away);
- four — a square.

A `PAGE` print leaves a row entirely (a page-wide picture is its own row), and the size menu gained
`SMALL_PORTRAIT` — the small print that stands up — for the picture that belongs in a row rather than
on a page of its own. Editor and read view share the arrangement, so a pair cannot come apart when the
member stops writing.

### 2. The carry animation, and the hover preview (v397, done)

Both in `features/personal/PersonalTodoRow.kt` (the drag state + the shells) and the canvas's block
wrapper:

- **The landing ghost.** The drop indicator was a 2dp accent line, which said "somewhere around
  here". It is now a band of the CARRIED BLOCK'S OWN MEASURED HEIGHT (`PersonalRowDragState
  .carriedHeight`), translucent accent with the rule drawn solid on the edge it lands on — at the TOP
  of the target on the way down and at its BOTTOM on the way up (`goingDown`), so the preview is always
  on the side the block is travelling towards. This is ONE pass for both kinds of block: a print being
  stacked among prints and a voice note between paragraphs show the same preview, sized to what is in
  hand.
- **The held shape.** `PersonalMovableBlock` takes `heldScale` / `heldTilt` / `heldLift` from its
  caller: a print lifts at 1.08 with a −2.5° lean and a 16dp shadow (a photograph in a hand), a voice
  note at 1.03 with no tilt and 12dp (a card). The lift is ANIMATED through a 0..1 `raise`, so the
  block rises into the hand and settles back down instead of popping between two sizes — the tilt
  rides the same value.
- **The snap.** A drop used to call `reset()` in the same frame, which zeroed the travel: the block
  teleported the last few pixels into its new slot while every row it had pushed aside snapped back at
  once. Now `commit()` clears the travelled range (so the rows' own shift target is 0 and a spring
  carries them home — `shiftFor` returns early for `fromIndex < 0`, which is what stops the first row
  taking one more step in the direction the block came from) and `settle(id)` animates whatever travel
  is left to zero with a stiff spring before letting go of the block. Only if it is still the same
  block, so a second pick-up during a settle is never clobbered.

### 3. The attachment inside a paragraph — NOT DONE, and why

The visual the user wants is a thumbnail BETWEEN THE WORDS with the text wrapping around it. In this
editor the prose row is a `BasicTextField(value: TextFieldValue, onValueChange = …)` — the LEGACY
string API — and that API has no inline content: Compose's `addInlineContent` (which is what makes a
composable sit in a text flow and the text wrap around it) lives on the NEW `TextFieldState` API
(`TextFieldBuffer.addInlineContent`). The read-only side is easy (`BasicText(inlineContent = …)` with
a `Placeholder`), but the editor is where the member types, and there the only routes are:

- **A — migrate the prose field to `TextFieldState`** (the supported route). The prose block becomes
  a real text field with inline placeholders. Cost: `PersonalEditorState` holding the writing as a
  plain `String` + a per-character mask `IntArray`, with split/merge/Enter/backspace/select-all/undo
  all indexed against that string. Every one of those touches has to be re-expressed against a
  `TextFieldBuffer`. Largest single change to the writing page since it was written.
- **B — the display transform**: keep the stored text, and show the field a mapped string (the marker
  character replaced by a run of spaces the width of the thumbnail, so the field's own layout wraps
  around it), with a real↔display index map on every edit, selection and composition range, and the
  thumbnail drawn over the reserved run from the field's `TextLayoutResult`. Contained to blocks that
  actually hold an attachment, but it sits on the typing path — and a wrong index there corrupts
  someone's words, which no CI run can catch.

Everything else on this page is already in place for it (the block model splits a paragraph around an
attachment dropped at the caret, and the two views share one renderer), so the work is the typing
path, not the model. Asking before writing it.

## Follow-ups from earlier batches (still open, unchanged)

- Inline bold/italic inside an EPUB paragraph (`stripMarkup` drops every tag).
- Series/anime episode data is mostly fetched rather than authored.
- More than one photo pair is now supported in a row; a page with more than four consecutive prints
  starts a second row.
- The to-do page's own tick box, the reader's sheet and the avatars shipped in earlier batches.
