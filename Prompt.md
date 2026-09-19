# Prompt Log — current request

## Request (2026-09-19, batch Z6 — the attachment inside a paragraph, and the asks queued behind it)

Verbatim: "do the attachment inside a paragraph, aand more than one photo pair, also make the
animation of it good, with proper hover preview same pass for voice recorder in animation, and proper
preview of photo stacing with snap." — followed by "epub emphasis now, also in redrawn avatars why
theres a line above shoulder also the poses are wierd fix it, and for th eleaf icon it sbad chnage it
also why the mon is C how about chnage the leaf to crecent moon also some avatars are weird looking
with the eyes, some are fine but some are weird and also that smile in each of them, also still in the
todo the chekcbox cyive state is glitchy when i deselct it i mesn its not selected it stil l makes the
next line in enter automatic reselect, it should keep the active state when its selected not when i
desecelt it. fix this glchy behavior. also for series the buttom sheet ui is beautifu and the text
colro is right too, and its look up is also so fast, can u make the anime use the same api as the
first same as series, and also make the anime ui similair to series".

**Status: the paragraph attachment shipped (see §3). The queued asks below it are NOT started.**

### 1. More than one photo pair — the row of prints (v396, shipped earlier)

Consecutive prints group into a RUN of up to four and the run lays itself out by how many arrived
(`PersonalPrintArrangement`): two as even halves, three as one upright frame with the other two
stacked beside it, four as a square. A `PAGE` print leaves a row entirely, and the size menu has
`SMALL_PORTRAIT` for the picture that belongs in a row. Editor and read view share the arrangement.

### 2. The carry animation, and the hover preview (v397, shipped earlier)

In `features/personal/PersonalTodoRow.kt` plus the canvas's block wrapper: a landing GHOST measured
from the carried block (the height of what is in hand, at the top of the target going down and at its
bottom going up); a held shape (a print lifts at 1.08 with a −2.5° lean and a deeper shadow, a voice
note rises evenly as a card) animated in and out rather than popping; and a snap that commits the
order, glides the rows home on a spring and slides the carried block's remaining travel to zero before
it lets go.

### 3. The attachment inside a paragraph — SHIPPED (v398, route B)

**The member's answer, after being shown both routes:** take the **VisualTransformation** route — the
one that leaves typing alone — and the attachment takes **its own print size** (the same five sizes a
standalone print has), so a `PAGE`-sized attachment inside a paragraph takes the measure and the words
carry on below it.

The first written draft used `SpanStyle(lineHeight = …)` to make the line grow. **That does not compile**:
`SpanStyle` carries no line height (verified against the AndroidX source — "to set paragraph level
styling such as line height, see `ParagraphStyle`"). The reservation therefore asks for its room
through its own FONT SIZE, converted by a MEASURED ratio (one probe line at 100sp in the paragraph's
own face), with the space count taken at that same size because a bigger font makes a wider space too.
A tenth again as tall on purpose: a picture lapping over the line above it reads worse than a line with
a little air in it.

What landed (`features/personal/PersonalCanvas.kt`, `data/PersonalDoc.kt`, `ChapterNoteBridge.kt`):

- **The model.** `PERSONAL_INLINE_MARK` (`\uFFFC`) is the one character that means "an attachment sits
  here", and `PersonalBlock.inlineRefs: List<String>` names the blocks in mark order. Serialized as
  `"inr"`, omitted when empty, so every page written before this version encodes byte-for-byte as it
  did and an older note decodes with nothing inside it.
- **The editor.** One `VisualTransformation` + `OffsetMapping` per attachment-bearing paragraph (every
  other paragraph gets `VisualTransformation.None`): each mark becomes a run of non-breaking spaces at
  the size the line needs, with the caret, selection, composition, Enter, backspace and undo all mapped
  back into the paragraph's own coordinates. The print itself is drawn over the run from the
  `TextLayoutResult` — the reservation's REAL room, read back off the bounding box, centred in the line.
  A tap opens the picture; the ✕ (or deleting the mark) takes it out of the sentence and puts it back on
  a line of its own, which is also the recovery: nothing is ever lost.
- **Where a drop goes.** `landsInsideParagraph()` = there is a caret, the line has words, and the caret
  is not at offset 0. A picture or a finished recording dropped there lands in the sentence; on a blank
  line (or at the very start of one) it keeps the line-of-its-own behaviour it always had.
- **The read view.** `BasicText(inlineContent = …)` fed by `personalInlineAnnotated`, which adds the
  string annotation Foundation's inline content is read through
  (`androidx.compose.foundation.text.inlineContent`) on each mark's own range — nothing moves, so every
  span, link and tap lands where it did. One key per picture (`"$PERSONAL_INLINE_MARK$n"`).
- **Both passes** (editor and read view) skip a held attachment when they lay out blocks and when they
  group prints, so a picture inside a sentence is never drawn twice and never opens a row of prints.
- **The chapter note** (`ChapterNoteBridge`) is words only: the mark is taken out of the text it stands
  in and the block that carries it is left out of the join, so a note gains no stray character and no
  blank line.

### 4. Queued but NOT started (the member's next asks, in their own order)

Their own answer to the API question has not been asked yet — the anime ask below is the one that wants
a decision, so ask before writing it.

1. **EPUB emphasis.** Inline `<b>`/`<i>`/`<em>`/`<strong>` inside an EPUB paragraph: `stripMarkup`
   drops every tag, so emphasis never survives. Needs the current chapter's paragraphs to carry their
   runs into the reader's spans (the reader already draws bold/italic spans from `mustRead`/marks).
2. **The redrawn avatars** (`features/community/SocialAvatar.kt`): a line above the shoulders in some
   portraits, poses that read oddly, the EYES and the SMILE on some of them, the **leaf** icon being
   bad and the **moon** reading as a "C" — the member wants the leaf changed, and the moon redrawn as
   a crescent. (The 20 portraits + 8 icons are the v397-era redraw; this is a correction pass.)
3. **The to-do checkbox cycle.** When a row is NOT selected the next Enter re-selects the box, so the
   armed state lingers instead of ending with the selection. The box's own rule lives in
   `PersonalEditorState.setChecked` / `lineStartsNewRow` / the v393e blank-row rule — the carry-over of
   `armed` across Enter is the suspect.
4. **Anime on the series rail.** The member wants anime to use the SAME API the series sheet uses
   (which they say is fast and correct) and the same UI as the series sheet. The series sheet reads
   TVMaze + Jikan through the shared sheet; anime currently goes through its own path. **Ask which one
   the series sheet actually reads from** before rewriting the anime path, and whether the anime
   episode list should keep the watched marks/likes it has now.

## Follow-ups from earlier batches (still open, unchanged)

- Series/anime episode data is mostly fetched rather than authored.
- A page with more than four consecutive prints starts a second row (by design).
- The reader's places sheet, the to-do tick box and the avatar redraw shipped in earlier batches; the
  avatar correction pass is queued above.
