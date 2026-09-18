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

### 3a. The member's answer (recorded, so the next session starts from it)

**Route A — rebuild the prose field on Compose's new `TextFieldState` API.** And the inline
attachment takes ITS OWN PRINT SIZE: the same five sizes a standalone print has (`PAGE`, `HALF`,
`PORTRAIT`, `SMALL`, `SMALL_PORTRAIT`), so a `PAGE`-sized attachment inside a paragraph takes the
given measure and the words carry on below it. That is the member's own choice of consequence, and
the flow must simply grow to hold it.

### 3b. What the API actually offers (verified against the docs, not memory)

This project is on Compose BOM `2026.05.01` (foundation `1.11.2`), well past the 1.8 release that
introduced inline content in a text FIELD, so the pieces exist:

- `TextFieldBuffer.addStyle(spanStyle, start, end)` — **only permitted inside an `OutputTransformation`**
  unless `ComposeFoundationFlags.isBasicTextFieldStyledTextEnabled` is on; it is the supported way to
  paint per-character marks in the new field, and the tracked-range form
  (`addStyle(spanStyle, range, ExpandPolicy) -> TrackedRange<SpanStyle>`, with `spanStyle`,
  `textRange`, `expandPolicy` as mutable properties and `getSpanStyles(range)` to read back)
  REPLACES the per-character `IntArray` mask this page has used for marks.
- `InlineTextContent(placeholder: Placeholder, children: @Composable (String) -> Unit)` — the placeholder
  is what reserves the room in the text line ("different from a regular composable, a Placeholder is
  also needed for text layout to reserve space"), which is exactly the printing-frame-shaped hole an
  attached print needs, at whatever size the print's own key says.
- `TextFieldState` + `TextFieldBuffer` (replace/insert/delete/placeCursor*/selectAll,
  `originalText`/`originalSelection`/`revertAllChanges`, `ChangeList`) is the editing model the field is
  driven by now.

### 3c. The migration, in the order it should be done

1. **The state, not the view.** One `TextFieldState` per prose block id, created from the block's
   stored text; the DOC stays the source of truth and the existing plumbing (`onFieldChange`,
   `mask`, `selection`, split/merge, undo, save) is fed from a snapshot sync, so nothing outside this
   field has to change in the same step. Marks move to an `OutputTransformation` `.addStyle(…)` pass
   built from the block's mask runs — the mask itself can stay the stored shape until step 4.
2. **The inline content.** `inlineRefs: List<String>` on `PersonalBlock` (serialized with the rest),
   the attachment inserted at the caret as its placeholder, and the placeholder's `children` drawing
   the print at its own size — the same `PersonalPhotoBlock`/voice strip the standalone row draws, so
   a print looks like itself inline or not.
3. **The read view.** `BasicText(inlineContent = …)` with one `InlineTextContent` per placeholder, so
   the page reads back exactly what was written (the read view and the editor must share the
   placeholder's size rule or an inline print will jump on switch).
4. **Then the marks.** Fold the mask into the field's own tracked spans (step 1's output pass becomes
   the real store), which is what makes a mark survive inside an attachment-bearing paragraph without
   a parallel index table.

Each step is a CI cycle of its own: the new-API opt-ins, the flag name and the placeholder-measure
rule cannot be confirmed from here, only by the compiler.

## Follow-ups from earlier batches (still open, unchanged)

- Inline bold/italic inside an EPUB paragraph (`stripMarkup` drops every tag).
- Series/anime episode data is mostly fetched rather than authored.
- More than one photo pair is now supported in a row; a page with more than four consecutive prints
  starts a second row.
- The to-do page's own tick box, the reader's sheet and the avatars shipped in earlier batches.
