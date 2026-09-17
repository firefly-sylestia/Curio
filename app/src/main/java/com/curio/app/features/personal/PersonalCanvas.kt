package com.curio.app.features.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.curio.app.data.PersonalAlign
import com.curio.app.data.PersonalBlock
import com.curio.app.data.PersonalDoc
import com.curio.app.data.PersonalMarker
import com.curio.app.data.newBlockId
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.WritingFontFamily
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * v387 — THE WRITING CANVAS (journals + chapter reviews share it).
 *
 * The body is a list of BLOCKS: a text block is one paragraph the writer can
 * style, a photo block is a picture they dropped in at the caret. Everything
 * the toolbar does is anchored on the FOCUSED block / the selection inside it,
 * which is how "bold just this line" works without a full document model:
 *
 *  · no selection  → the tool applies to that block (the line the caret is on)
 *  · a selection   → the tool applies to exactly those characters
 *  · an empty line → the tool arms and the next characters typed carry it
 *
 * Enter SPLITS the paragraph instead of inserting a newline, so a block stays
 * "one line" for alignment purposes and the caret flow matches a normal
 * editor (there is no marker syntax anywhere: the styling lives beside the
 * text, never inside it).
 */

/** The quote size inside the canvas' 17sp body and the read-only 16sp body —
 *  a quoted line reads as a quotation, a touch smaller than the prose. */
private val QUOTE_BODY_SIZE = 16.sp
private val QUOTE_VIEW_SIZE = 15.sp

/** A TITLE line and a SMALL line, in the editor and in the read-only view. */
private val TITLE_BODY_SIZE = 24.sp
private val TITLE_VIEW_SIZE = 22.sp
private val SMALL_BODY_SIZE = 13.5.sp
private val SMALL_VIEW_SIZE = 12.5.sp

/**
 * THE QUOTE'S OWN COLOUR — COFFEE, never the app's accent (user decision: a
 * quotation has to read as ink on paper, and an accent-tinted quote looked
 * like a highlight someone forgot to finish). The dark theme takes the milky
 * coffee twin, because the deep one would vanish into a dark page.
 */
@Composable
internal fun personalQuoteColor(): Color = Color(0xFF9A6A43)

/** The rule beside a quoted block: the same coffee, at rule strength. */
@Composable
internal fun personalQuoteRule(): Color = personalQuoteColor().copy(alpha = 0.85f)

// ────────────────────────────────────────────────────────────────────────────
// Style → pixels
// ────────────────────────────────────────────────────────────────────────────

/** True when EVERY visible character of the block carries the flag (and
 *  there is at least one): the whole LINE is that style, so it gets the block
 *  treatment — the coffee rule for a quote, the drawn dot for a bullet, a
 *  bigger (or smaller) body for the title and small formats. */
internal fun personalBlockCarries(text: String, mask: IntArray, flag: Int): Boolean {
    var seen = false
    for (i in text.indices) {
        if (text[i].isWhitespace()) continue
        if (mask.getOrElse(i) { 0 } and flag == 0) return false
        seen = true
    }
    return seen
}

/** Kept as the quote's own name — everything else calls the general one. */
internal fun personalBlockIsQuote(text: String, mask: IntArray): Boolean =
    personalBlockCarries(text, mask, FLAG_QUOTE)

/**
 * v389 — how far along a checklist is: `done to total`, counting only rows with
 * words in them (an empty row the writer has not filled in is not a task yet).
 * Decoded from the STORED runs, so the to-do page's count, the journal list's
 * "3 of 7 done" line and a book chapter's checklist all read the same document
 * the same way.
 */
internal fun PersonalDoc.checklistProgress(): Pair<Int, Int> {
    var done = 0
    var total = 0
    blocks.forEach { block ->
        if (block.isPhoto || block.text.isBlank()) return@forEach
        val mask = runsToMask(block.text.length, block.runs)
        if (!personalBlockCarries(block.text, mask, FLAG_CHECKBOX)) return@forEach
        total++
        if (block.checked) done++
    }
    return done to total
}

// ────────────────────────────────────────────────────────────────────────────
// List furniture (v389)
// ────────────────────────────────────────────────────────────────────────────

/** The marker's box: the glyph width every list line reserves. */
internal val PERSONAL_MARKER_SIZE = 18.dp

/** The air between the marker and the first word. */
internal val PERSONAL_MARKER_GAP = 10.dp

/** The whole lead-in — what a list line indents its TEXT by, so a wrapped line
 *  lines up under the first word instead of under the marker. */
internal val PERSONAL_MARKER_LEAD = PERSONAL_MARKER_SIZE + PERSONAL_MARKER_GAP

/**
 * v389 — ONE BULLET RENDERER for the whole family: the editor, every read-only
 * view (journal previews, book reviews, chapter pages) and the tool dock's own
 * menu all draw through this, so a star list reads as a star list everywhere.
 *
 * [lineHeight] is the FIRST line's height in pixels and the marker centres on
 * it. The old drawing hard-coded a vertical offset per style (8 / 11 / 12 / 15 /
 * 18dp), which is why a box never sat level with its words at another font
 * scale, and why the editor's box and the read-only box were two different
 * sizes with two different ticks.
 */
internal fun DrawScope.drawPersonalMarker(
    marker: PersonalMarker,
    ink: Color,
    lineHeight: Float
) {
    val size = PERSONAL_MARKER_SIZE.toPx()
    val cx = size / 2f
    val cy = lineHeight / 2f
    val r = size * 0.46f
    when (marker) {
        PersonalMarker.DOT -> drawCircle(
            color = ink,
            radius = size * 0.17f,
            center = Offset(cx, cy)
        )
        PersonalMarker.RING -> drawCircle(
            color = ink,
            radius = size * 0.23f,
            center = Offset(cx, cy),
            style = Stroke(width = size * 0.14f)
        )
        PersonalMarker.DASH -> drawRoundRect(
            color = ink,
            topLeft = Offset(0f, cy - size * 0.055f),
            size = Size(size * 0.92f, size * 0.11f),
            cornerRadius = CornerRadius(size * 0.055f)
        )
        PersonalMarker.STAR, PersonalMarker.SPARK -> {
            // A four-point sparkle (SPARK is the same shape with a tighter
            // waist). Straight edges only, so it stays crisp at 18dp.
            val w = r * if (marker == PersonalMarker.SPARK) 0.155f else 0.212f
            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + w, cy - w)
                lineTo(cx + r, cy)
                lineTo(cx + w, cy + w)
                lineTo(cx, cy + r)
                lineTo(cx - w, cy + w)
                lineTo(cx - r, cy)
                lineTo(cx - w, cy - w)
                close()
            }
            drawPath(path, color = ink)
        }
        PersonalMarker.CRYSTAL -> {
            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r * 0.76f, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r * 0.76f, cy)
                close()
            }
            drawPath(
                path = path,
                color = ink,
                style = Stroke(width = size * 0.135f, join = StrokeJoin.Round)
            )
            // The facet: one rule across the middle, so the shape reads as a
            // cut stone rather than a plain lozenge.
            drawLine(
                color = ink,
                start = Offset(cx - r * 0.5f, cy),
                end = Offset(cx + r * 0.5f, cy),
                strokeWidth = size * 0.10f,
                cap = StrokeCap.Round
            )
        }
        PersonalMarker.ARROW -> {
            val path = Path().apply {
                moveTo(cx - r * 0.40f, cy - r * 0.52f)
                lineTo(cx + r * 0.42f, cy)
                lineTo(cx - r * 0.40f, cy + r * 0.52f)
            }
            drawPath(
                path = path,
                color = ink,
                style = Stroke(
                    width = size * 0.15f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        PersonalMarker.LEAF -> {
            val path = Path().apply {
                moveTo(cx, cy - r)
                cubicTo(cx + r * 0.95f, cy - r * 0.35f, cx + r * 0.35f, cy + r * 0.95f, cx, cy + r)
                cubicTo(cx - r * 0.35f, cy + r * 0.95f, cx - r * 0.95f, cy - r * 0.35f, cx, cy - r)
                close()
            }
            drawPath(path, color = ink)
        }
        PersonalMarker.HEART -> {
            // Two lobes and a point — circles plus a triangle, so the shape is
            // exact at any size (hand-tuned curves drift the moment the marker
            // is drawn at another scale).
            val lobe = r * 0.46f
            drawCircle(color = ink, radius = lobe, center = Offset(cx - r * 0.40f, cy - r * 0.30f))
            drawCircle(color = ink, radius = lobe, center = Offset(cx + r * 0.40f, cy - r * 0.30f))
            val path = Path().apply {
                moveTo(cx - r * 0.84f, cy - r * 0.22f)
                lineTo(cx + r * 0.84f, cy - r * 0.22f)
                lineTo(cx, cy + r)
                close()
            }
            drawPath(path, color = ink)
        }
        PersonalMarker.BOLT -> {
            val path = Path().apply {
                moveTo(cx + r * 0.18f, cy - r)
                lineTo(cx - r * 0.62f, cy + r * 0.12f)
                lineTo(cx - r * 0.10f, cy + r * 0.12f)
                lineTo(cx - r * 0.18f, cy + r)
                lineTo(cx + r * 0.62f, cy - r * 0.12f)
                lineTo(cx + r * 0.10f, cy - r * 0.12f)
                close()
            }
            drawPath(path, color = ink)
        }
    }
}

/**
 * v389 — the checklist box, drawn identically in the editor and in every
 * read-only view.
 *
 * OPEN: a NEUTRAL hairline outline in the theme's own ink — never the pale
 * accent, which read as a smudge on a light page (what the member called out).
 * DONE: the DEEP accent as a solid fill with a page-coloured tick, so finished
 * rows are the page's one strong accent. The box is centred on [lineHeight], so
 * it sits level with the first line's words, and the tick is ONE path with
 * round caps and joins — two butt-capped hairlines meeting at a sharp corner
 * was the "broken tick" inside the box.
 */
internal fun DrawScope.drawPersonalCheckbox(
    checked: Boolean,
    outline: Color,
    fill: Color,
    onFill: Color,
    lineHeight: Float
) {
    val size = PERSONAL_MARKER_SIZE.toPx()
    val top = ((lineHeight - size) / 2f).coerceAtLeast(0f)
    val corner = CornerRadius(size * 0.30f)
    if (!checked) {
        drawRoundRect(
            color = outline,
            topLeft = Offset(0f, top),
            size = Size(size, size),
            cornerRadius = corner,
            style = Stroke(width = size * 0.085f)
        )
        return
    }
    drawRoundRect(
        color = fill,
        topLeft = Offset(0f, top),
        size = Size(size, size),
        cornerRadius = corner
    )
    val tick = Path().apply {
        moveTo(size * 0.24f, top + size * 0.53f)
        lineTo(size * 0.42f, top + size * 0.70f)
        lineTo(size * 0.77f, top + size * 0.30f)
    }
    drawPath(
        path = tick,
        color = onFill,
        style = Stroke(width = size * 0.135f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Renders one block's text with its per-character flags applied.
 *
 * A quoted run is no longer a highlight wash: it is the blockquote a reader
 * expects — a touch smaller, in the quote ink, with the rule drawn beside the
 * block by the caller. Bold / italic / underline still stack on top of it, so
 * "quote the line and embolden the first words" reads exactly as written.
 */
internal fun personalAnnotated(
    text: String,
    mask: IntArray,
    ink: Color,
    quoteInk: Color,
    quoteSize: TextUnit,
    titleSize: TextUnit = TextUnit.Unspecified,
    smallSize: TextUnit = TextUnit.Unspecified
): AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    append(text)
    var i = 0
    while (i < text.length) {
        val flags = mask.getOrElse(i) { 0 }
        var j = i + 1
        while (j < text.length && mask.getOrElse(j) { 0 } == flags) j++
        if (flags != 0) {
            val decorations = ArrayList<TextDecoration>(2)
            if (flags and FLAG_UNDERLINE != 0) decorations.add(TextDecoration.Underline)
            if (flags and FLAG_STRIKE != 0) decorations.add(TextDecoration.LineThrough)
            addStyle(
                SpanStyle(
                    color = if (flags and FLAG_QUOTE != 0) quoteInk else ink,
                    // The quote changes the TEXT, never the page: smaller, in
                    // the quote ink — the rule beside the block is what makes
                    // it read as a quotation. A title goes the other way (the
                    // display serif, bigger), and small steps down again.
                    fontSize = when {
                        flags and FLAG_QUOTE != 0 -> quoteSize
                        flags and FLAG_TITLE != 0 -> titleSize
                        flags and FLAG_SMALL != 0 -> smallSize
                        else -> TextUnit.Unspecified
                    },
                    fontFamily = if (flags and FLAG_TITLE != 0) FrauncesFontFamily else null,
                    fontWeight = when {
                        flags and FLAG_BOLD != 0 -> FontWeight.Bold
                        flags and FLAG_TITLE != 0 -> FontWeight.SemiBold
                        else -> null
                    },
                    fontStyle = if (flags and FLAG_ITALIC != 0) FontStyle.Italic else null,
                    textDecoration = when (decorations.size) {
                        0 -> null
                        1 -> decorations.first()
                        else -> TextDecoration.combine(decorations)
                    }
                ),
                i, j
            )
        }
        i = j
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Editor state
// ────────────────────────────────────────────────────────────────────────────

/** Where the caret should land after a structural change (a photo insert or
 *  a paragraph split) — the canvas consumes it once. */
internal data class PersonalCaret(val blockId: String, val index: Int)

/**
 * The canvas' brain: the block list, each block's text + style mask, and the
 * toolbar's live state. Deliberately NOT a Compose UI class — a screen can
 * drive it (auto-save, "add a photo", programmatic focus) without touching
 * the field.
 */
internal class PersonalEditorState(initial: PersonalDoc) {

    private val order: SnapshotStateList<String> = mutableStateListOf()
    private val blocks: SnapshotStateMap<String, PersonalBlock> = mutableStateMapOf()
    private val masks: SnapshotStateMap<String, IntArray> = mutableStateMapOf()
    private val selections: SnapshotStateMap<String, TextRange> = mutableStateMapOf()
    private val compositions: SnapshotStateMap<String, TextRange?> = mutableStateMapOf()

    /** Called with the whole document after every change (auto-save). */
    var onDocChanged: (PersonalDoc) -> Unit = {}

    /** The block the keyboard is in — the target of every tool. */
    var focusedId by mutableStateOf<String?>(null)
        private set
    var focusRequestToken by mutableIntStateOf(0)
        private set

    fun requestFocusOnEmptyLine() {
        focusRequestToken++
    }

    fun armCheckboxOnEmptyLine() {
        armed = (armed and (FLAG_BULLET or FLAG_CHECKBOX).inv()) or FLAG_CHECKBOX
    }

    /** Tools switched on with nothing to apply them to (an empty line). */
    var armed by mutableIntStateOf(0)

    /**
     * v389 — a CHECKLIST page keeps making rows: Enter at the END of a row
     * starts the next line as a row already, and Enter on an EMPTY row ends the
     * list (the way every checklist behaves). Off by default, because on a
     * journal day or a review the line after a checklist line is prose — the
     * canvas' long-standing rule that an ARMED tool never crosses the break.
     * Set from [PersonalWritingPage]'s `checklistFirst`, so only the to-do page
     * turns it on.
     */
    var keepsChecklistRows: Boolean = false
        private set

    private var caret by mutableStateOf<PersonalCaret?>(null)

    /**
     * Rebuilds the whole canvas from a document that arrived from the store
     * (`LaunchedEffect(entryId)` load). Called only when the SCREEN changes,
     * never on an echo of the writer's own keystrokes — replacing the blocks
     * mid-typing would drop the caret.
     */
    fun replace(document: PersonalDoc) {
        order.clear()
        blocks.clear()
        masks.clear()
        selections.clear()
        compositions.clear()
        focusedId = null
        armed = 0
        caret = null
        val source = if (document.blocks.isEmpty()) listOf(PersonalBlock(id = newBlockId()))
        else document.blocks
        source.forEach { block ->
            val id = block.id.ifBlank { newBlockId() }
            val normalised = block.copy(id = id)
            order.add(id)
            blocks[id] = normalised
            masks[id] = runsToMask(normalised.text.length, normalised.runs)
        }
    }

    /** The caret the canvas should move to (read by every block, consumed by
     *  the one it names). */
    val pendingCaret: PersonalCaret? get() = caret

    /** Called by the block that took the caret, so no other block re-asks. */
    fun consumeCaret(id: String) {
        if (caret?.blockId == id) caret = null
    }

    init {
        val source = if (initial.blocks.isEmpty()) listOf(PersonalBlock(id = newBlockId()))
        else initial.blocks
        source.forEach { block ->
            val id = block.id.ifBlank { newBlockId() }
            val normalised = block.copy(id = id)
            order.add(id)
            blocks[id] = normalised
            masks[id] = runsToMask(normalised.text.length, normalised.runs)
        }
        if (order.isEmpty()) {
            val block = PersonalBlock(id = newBlockId())
            order.add(block.id)
            blocks[block.id] = block
            masks[block.id] = emptyMask(0)
        }
    }

    // ── Reads ──────────────────────────────────────────────────────────

    val blockIds: List<String> get() = order

    fun block(id: String): PersonalBlock? = blocks[id]

    fun text(id: String): String = blocks[id]?.text.orEmpty()

    fun mask(id: String): IntArray = masks[id] ?: emptyMask(text(id).length)

    fun photo(id: String): String? = blocks[id]?.photo

    fun caption(id: String): String = blocks[id]?.caption.orEmpty()

    fun align(id: String): PersonalAlign = blocks[id]?.align ?: PersonalAlign.START

    /** A checklist line's tick — stored with the block (v389). */
    fun checked(id: String): Boolean = blocks[id]?.checked == true

    /** The line's bullet marker (the default dot when it never picked one). */
    fun marker(id: String): PersonalMarker = blocks[id]?.markerStyle ?: PersonalMarker.DOT

    fun selection(id: String): TextRange? = selections[id]

    fun composition(id: String): TextRange? = compositions[id]

    /** True when nothing has been written yet (the screen's save gate). */
    fun isEmpty(): Boolean = doc().isEmpty

    /** True when ANY block carries words — the canvas shows its "Write…" hint
     *  only while the whole page is still blank. */
    fun hasText(): Boolean = blocks.values.any { it.text.isNotBlank() }

    /** The whole document as it will be stored. */
    fun doc(): PersonalDoc = PersonalDoc(
        order.mapNotNull { id ->
            blocks[id]?.let { block ->
                block.copy(
                    text = block.text,
                    runs = maskToRuns(mask(id)),
                    align = block.align
                )
            }
        }
    )

    // ── Edits ──────────────────────────────────────────────────────────

    /** The writer typed / pasted / deleted inside one block. */
    fun onFieldChange(id: String, value: TextFieldValue) {
        val old = blocks[id] ?: return
        val newText = value.text
        if (newText != old.text) {
            masks[id] = maskAfterEdit(old.text, newText, mask(id), armed)
            blocks[id] = old.copy(text = newText)
            // An armed tool has now been used: what follows continues in the
            // style just typed, so the button stops being "pending".
            if (armed != 0) armed = 0
        }
        selections[id] = value.selection
        compositions[id] = value.composition
        onDocChanged(doc())
    }

    fun onFocusChanged(id: String, focused: Boolean) {
        if (focused) focusedId = id
        else if (focusedId == id) focusedId = null
    }

    /** The flags of whatever the focused tool bar would act on right now —
     *  drives which buttons read as switched on. */
    fun activeFlags(): Int {
        val id = focusedId ?: return armed
        val selection = selections[id]
        // No selection: the LINE decides, exactly like the tap would — a tool
        // lights only when the whole line already carries it, so what the dock
        // shows is what another tap on that button would do.
        val range = if (selection != null && !selection.collapsed) {
            selection.min to selection.max
        } else {
            0 to text(id).length
        }
        val blockMask = mask(id)
        var flags = 0
        ALL_FLAGS.forEach { flag ->
            if (maskCovers(blockMask, range.first, range.second, flag)) flags = flags or flag
        }
        return flags or armed
    }

    /**
     * Toggles one tool on the focused block: the selection when there is one,
     * the whole line otherwise. An empty line arms the tool instead, so the
     * first words typed arrive already styled.
     */
    fun cycleListStyle() {
        val id = focusedId ?: return
        val current = activeFlags()
        val next = when {
            current and FLAG_BULLET != 0 -> FLAG_CHECKBOX
            current and FLAG_CHECKBOX != 0 -> FLAG_BULLET
            else -> FLAG_BULLET
        }
        toggleListStyle(next)
    }

    fun toggleListStyle(flag: Int) {
        val id = focusedId ?: return
        val mask = mask(id)
        val selection = selections[id]
        val range = if (selection != null && !selection.collapsed) selection.min to selection.max else 0 to text(id).length
        if (range.second <= range.first) {
            armed = if (armed and flag != 0) armed and flag.inv() else (armed and (FLAG_BULLET or FLAG_CHECKBOX).inv()) or flag
        } else {
            val current = maskCovers(mask, range.first, range.second, flag)
            var updated = maskApply(mask, range.first, range.second, flag, !current)
            val other = if (flag == FLAG_BULLET) FLAG_CHECKBOX else FLAG_BULLET
            if (!current) updated = maskApply(updated, range.first, range.second, other, false)
            masks[id] = updated
            armed = armed and (FLAG_BULLET or FLAG_CHECKBOX).inv()
        }
        onDocChanged(doc())
    }

    /**
     * v389 — ticks / un-ticks a checklist line. The tick is stored WITH THE
     * BLOCK, so it survives a reload, an app switch and the read-only views;
     * before this it lived in the row's own widget state and a page forgot
     * everything the member had finished.
     */
    fun setChecked(id: String, value: Boolean) {
        val block = blocks[id] ?: return
        if (block.checked == value) return
        blocks[id] = block.copy(checked = value)
        onDocChanged(doc())
    }

    /**
     * v389 — what the dock's MARKER MENU applies: gives the focused line a
     * bullet in [marker]'s style, or takes the list off it altogether with
     * `null`. A marker belongs to a bulleted line, so picking one clears the
     * checklist flag and "No list" clears both — the same one-list-per-line
     * rule the two list tools already followed.
     */
    fun applyMarker(marker: PersonalMarker?) {
        val id = focusedId ?: order.firstOrNull() ?: return
        val block = blocks[id] ?: return
        val text = block.text
        val listFlags = FLAG_BULLET or FLAG_CHECKBOX
        if (text.isEmpty()) {
            // Nothing to mark yet: arm the tool so the first words typed arrive
            // as the list the writer asked for.
            armed = if (marker == null) armed and listFlags.inv()
            else (armed and listFlags.inv()) or FLAG_BULLET
        } else {
            var updated = mask(id)
            updated = maskApply(updated, 0, text.length, FLAG_CHECKBOX, false)
            updated = maskApply(updated, 0, text.length, FLAG_BULLET, marker != null)
            masks[id] = updated
            armed = armed and listFlags.inv()
        }
        blocks[id] = block.copy(marker = marker?.key.orEmpty())
        onDocChanged(doc())
    }

    /** The focused line's marker — the dock's bullet button wears it. */
    fun markerOfFocused(): PersonalMarker {
        val id = focusedId ?: order.firstOrNull() ?: return PersonalMarker.DOT
        return marker(id)
    }

    fun toggle(flag: Int) {
        val id = focusedId ?: return
        val blockMask = mask(id)
        val selection = selections[id]
        val range = if (selection != null && !selection.collapsed) {
            selection.min to selection.max
        } else {
            0 to text(id).length
        }
        if (range.second <= range.first) {
            armed = armed xor flag
            onDocChanged(doc())
            return
        }
        val on = !maskCovers(blockMask, range.first, range.second, flag)
        masks[id] = maskApply(blockMask, range.first, range.second, flag, on)
        armed = armed and flag.inv()
        onDocChanged(doc())
    }

    /** Left / centre for the focused block (a paragraph is the unit a line
     *  tool can point at). */
    fun setAlign(align: PersonalAlign) {
        val id = focusedId ?: order.firstOrNull() ?: return
        val block = blocks[id] ?: return
        blocks[id] = block.copy(align = align)
        onDocChanged(doc())
    }

    /**
     * Drops a photo in at the caret: the block splits into what came before,
     * the picture, and what comes after (a fresh empty block when the
     * paragraph ended there). This is what "photos in the same canvas" means —
     * the picture sits exactly where the writer was standing.
     */
    fun insertPhoto(uri: String) {
        val id = focusedId ?: order.lastOrNull() ?: return
        val block = blocks[id] ?: return
        val caretIndex = (selections[id]?.start ?: block.text.length)
            .coerceIn(0, block.text.length)
        val head = PersonalBlock(
            id = newBlockId(),
            text = block.text.take(caretIndex),
            runs = maskToRuns(mask(id).copyOfRange(0, caretIndex)),
            align = block.align
        )
        val tailText = block.text.drop(caretIndex)
        val tailMask = mask(id).copyOfRange(caretIndex, block.text.length)
        val tail = PersonalBlock(
            id = newBlockId(),
            text = tailText,
            runs = maskToRuns(tailMask),
            align = block.align
        )
        val photoBlock = PersonalBlock(id = newBlockId(), photo = uri)
        val index = order.indexOf(id)
        if (index < 0) return
        // The old block is replaced in place, so the page never jumps.
        order[index] = head.id
        blocks.remove(id)
        masks.remove(id)
        selections.remove(id)
        compositions.remove(id)
        blocks[head.id] = head
        masks[head.id] = runsToMask(head.text.length, head.runs)
        order.add(index + 1, photoBlock.id)
        blocks[photoBlock.id] = photoBlock
        masks[photoBlock.id] = emptyMask(0)
        if (tailText.isNotEmpty()) {
            order.add(index + 2, tail.id)
            blocks[tail.id] = tail
            masks[tail.id] = runsToMask(tail.text.length, tail.runs)
            caret = PersonalCaret(tail.id, 0)
        } else {
            val empty = PersonalBlock(id = newBlockId())
            order.add(index + 2, empty.id)
            blocks[empty.id] = empty
            masks[empty.id] = emptyMask(0)
            caret = PersonalCaret(empty.id, 0)
        }
        focusedId = caret?.blockId
        onDocChanged(doc())
    }

    fun setCaption(id: String, caption: String) {
        val block = blocks[id] ?: return
        blocks[id] = block.copy(caption = caption)
        onDocChanged(doc())
    }

    /** Removes a photo block (and its caption). */
    fun removeBlock(id: String) {
        val index = order.indexOf(id)
        if (index < 0) return
        order.remove(id)
        blocks.remove(id)
        masks.remove(id)
        selections.remove(id)
        compositions.remove(id)
        if (order.isEmpty()) {
            val block = PersonalBlock(id = newBlockId())
            order.add(block.id)
            blocks[block.id] = block
            masks[block.id] = emptyMask(0)
        }
        if (focusedId == id) focusedId = order.getOrNull(index.coerceAtMost(order.size - 1))
        onDocChanged(doc())
    }

    /**
     * Enter: the paragraph splits at the caret and the caret lands at the
     * start of the new one. The style of the characters travels with them (the
     * mask is cut in two), but an ARMED tool does not cross the break: a new
     * line is plain prose unless a tool is armed — or unless this is a
     * checklist page, which arms its own rows (see [keepsChecklistRows]).
     */
    fun splitAtCaret(id: String) {
        val block = blocks[id] ?: return
        // Read the row's own flags BEFORE the split rewrites the mask: a
        // checklist page needs to know whether the line being left was a row.
        val wasChecklistRow = personalBlockCarries(block.text, mask(id), FLAG_CHECKBOX)
        val caretIndex = (selections[id]?.start ?: block.text.length)
            .coerceIn(0, block.text.length)
        val index = order.indexOf(id)
        if (index < 0) return
        val head = block.copy(
            text = block.text.take(caretIndex),
            runs = maskToRuns(mask(id).copyOfRange(0, caretIndex))
        )
        val tail = PersonalBlock(
            id = newBlockId(),
            text = block.text.drop(caretIndex),
            runs = maskToRuns(mask(id).copyOfRange(caretIndex, block.text.length)),
            align = if (block.text.drop(caretIndex).isEmpty()) block.align else PersonalAlign.START
        )
        masks[id] = runsToMask(head.text.length, head.runs)
        blocks[id] = head
        order.add(index + 1, tail.id)
        blocks[tail.id] = tail
        masks[tail.id] = runsToMask(tail.text.length, tail.runs)
        selections[tail.id] = TextRange(0)
        focusedId = tail.id
        caret = PersonalCaret(tail.id, 0)
        // A checklist page (see [keepsChecklistRows]): Enter after a row with
        // words in it makes the NEXT row; Enter on an empty row ends the list.
        if (keepsChecklistRows && wasChecklistRow && tail.text.isEmpty() && head.text.isNotBlank()) {
            armed = FLAG_CHECKBOX
        }
        onDocChanged(doc())
    }

    /** Focus + caret request the canvas consumes on its next frame. */
    fun requestCaret(id: String, index: Int = 0) {
        caret = PersonalCaret(id, index)
    }
}

// -----------------------------------------------------------------------------
// Canvas
// -----------------------------------------------------------------------------

/**
 * The writing surface. It does NOT scroll: the caller owns the scroll
 * container, so the journal's header (date, mood, title) and the toolbar can
 * frame the page exactly as the design wants.
 */
@Composable
internal fun PersonalCanvas(
    state: PersonalEditorState,
    modifier: Modifier = Modifier,
    ink: Color = MaterialTheme.colorScheme.onSurface,
    accent: Color = personalAccent(),
    // The tapped thumbnail's bounds ride along with its URI: the page's own
    // overlay grows the picture out of the spot it was tapped in (see
    // PersonalPhotoOverlay), which a bare URI cannot say.
    onOpenPhoto: (String, Rect?) -> Unit = { _, _ -> },
    enabled: Boolean = true
) {
    Column(
        modifier = modifier.clickable(enabled = enabled) { state.requestFocusOnEmptyLine() },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        state.blockIds.forEach { id ->
            val block = state.block(id) ?: return@forEach
            if (block.isPhoto) {
                PersonalPhotoBlock(
                    uri = block.photo.orEmpty(),
                    caption = state.caption(id),
                    ink = ink,
                    accent = accent,
                    enabled = enabled,
                    onCaption = { state.setCaption(id, it) },
                    onRemove = { state.removeBlock(id) },
                    onOpen = { bounds -> onOpenPhoto(block.photo.orEmpty(), bounds) }
                )
            } else {
                PersonalTextBlock(id = id, state = state, ink = ink, accent = accent, enabled = enabled)
            }
        }
    }
}

@Composable
private fun PersonalTextBlock(
    id: String,
    state: PersonalEditorState,
    ink: Color,
    accent: Color,
    enabled: Boolean
) {
    val text = state.text(id)
    val mask = state.mask(id)
    val align = state.align(id)
    val quoteRule = personalQuoteRule()
    val quoteInk = personalQuoteColor().copy(alpha = 0.92f)
    val bulletInk = personalAccentInk()
    val isQuote = personalBlockIsQuote(text, mask)
    // A line that IS a title (or a small note) is set by the BLOCK, so a
    // heading really is bigger writing and not just a bolder word.
    val isTitle = personalBlockCarries(text, mask, FLAG_TITLE)
    val isSmall = personalBlockCarries(text, mask, FLAG_SMALL)
    val isBullet = personalBlockCarries(text, mask, FLAG_BULLET)
    val isCheckbox = personalBlockCarries(text, mask, FLAG_CHECKBOX)
    // v389 — the list furniture's own metrics: the marker centres on the FIRST
    // line's height, which is what puts a box level with the words it labels.
    val lineHeight = if (isTitle) 34.sp else if (isSmall) 22.sp else 29.sp
    // The tick the writer actually made is on the BLOCK now, not in this row's
    // widget state, so a reload cannot lose it.
    val checked = state.checked(id)
    val markerFill = personalAccentInk()
    val markerOnFill = MaterialTheme.colorScheme.surface
    val markerOutline = ink.copy(alpha = 0.42f)
    // ONE hint for the whole page: the empty-line "Write…" on every new
    // paragraph read as a page full of the word "write".
    val showHint = text.isEmpty() && !state.hasText()
    val focusRequester = remember(id) { FocusRequester() }
    LaunchedEffect(state.focusRequestToken) {
        if (state.focusRequestToken > 0 && text.isEmpty() && state.blockIds.firstOrNull { state.text(it).isEmpty() } == id) {
            focusRequester.requestFocus()
        }
    }
    val value = TextFieldValue(
        annotatedString = personalAnnotated(
            text, mask, ink, quoteInk, QUOTE_BODY_SIZE,
            titleSize = if (isTitle) TextUnit.Unspecified else TITLE_BODY_SIZE,
            smallSize = if (isSmall) TextUnit.Unspecified else SMALL_BODY_SIZE
        ),
        selection = (state.selection(id) ?: TextRange(text.length))
            .let { if (it.max > text.length) TextRange(text.length) else it }
            .let { if (it.min < 0) TextRange(0) else it },
        composition = state.composition(id)
    )
    val alignOf = if (align == PersonalAlign.CENTER) TextAlign.Center else TextAlign.Start
    val bodyStyle = when {
        isTitle -> TextStyle(
            fontFamily = FrauncesFontFamily,
            fontSize = TITLE_BODY_SIZE,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            textAlign = alignOf
        )
        isSmall -> TextStyle(
            fontFamily = WritingFontFamily,
            fontSize = SMALL_BODY_SIZE,
            lineHeight = 22.sp,
            color = ink,
            textAlign = alignOf
        )
        else -> TextStyle(
            fontFamily = WritingFontFamily,
            fontSize = 17.sp,
            lineHeight = 29.sp,
            color = ink,
            textAlign = alignOf
        )
    }
    BasicTextField(
        value = value,
        onValueChange = { state.onFieldChange(id, it) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            // A quoted line wears the coffee rule down its side, a bulleted
            // line wears a drawn dot (both on the block's own height, so they
            // grow with the writing).
            .then(
                when {
                    isQuote -> Modifier
                        .drawBehind {
                            val barWidth = 3.dp.toPx()
                            drawRoundRect(
                                color = quoteRule,
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(barWidth / 2f)
                            )
                        }
                        .padding(start = 13.dp)
                    // v389 — both list styles draw through the ONE shared
                    // renderer (see drawPersonalCheckbox / drawPersonalMarker)
                    // and indent by the same lead, so the editor and the
                    // read-only views can never disagree about a checklist row
                    // again.
                    isCheckbox -> Modifier
                        .drawBehind {
                            drawPersonalCheckbox(
                                checked = checked,
                                outline = markerOutline,
                                fill = markerFill,
                                onFill = markerOnFill,
                                lineHeight = lineHeight.toPx()
                            )
                        }
                        .clickable(enabled = enabled) { state.setChecked(id, !checked) }
                        .padding(start = PERSONAL_MARKER_LEAD)
                    isBullet -> Modifier
                        .drawBehind {
                            drawPersonalMarker(
                                marker = state.marker(id),
                                ink = bulletInk,
                                lineHeight = lineHeight.toPx()
                            )
                        }
                        .padding(start = PERSONAL_MARKER_LEAD)
                    else -> Modifier
                }
            )
            .focusRequester(focusRequester)
            .onFocusChanged { state.onFocusChanged(id, it.isFocused) }
            .onPreviewKeyEvent { event ->
                // Enter makes a NEW line (a block), so a line tool can point
                // at the line the caret is on. Shift+Enter keeps the plain
                // newline inside the paragraph.
                if (
                    enabled &&
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter) &&
                    !event.isShiftPressed
                ) {
                    state.splitAtCaret(id)
                    true
                } else {
                    false
                }
            },
        textStyle = bodyStyle,
        cursorBrush = SolidColor(personalAccentInk()),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default
        ),
        decorationBox = { inner ->
            Box {
                if (showHint) {
                    Text(
                        text = "Write…",
                        style = bodyStyle.copy(color = ink.copy(alpha = 0.32f))
                    )
                }
                inner()
            }
        }
    )
    // Structural edits (a photo dropped in, a paragraph split) hand the caret
    // to a block that did not exist a frame ago — the field has to ask for
    // focus itself, in its own composition scope.
    val pendingCaret = state.pendingCaret
    LaunchedEffect(pendingCaret, id) {
        if (pendingCaret?.blockId == id) {
            focusRequester.requestFocus()
            state.onFocusChanged(id, true)
            state.consumeCaret(id)
        }
    }
}

@Composable
private fun PersonalPhotoBlock(
    uri: String,
    caption: String,
    ink: Color,
    accent: Color,
    enabled: Boolean,
    onCaption: (String) -> Unit,
    onRemove: () -> Unit,
    onOpen: (Rect?) -> Unit
) {
    // The preview is deliberately SMALL (it is a note in a page, not a
    // gallery) and its bounds are what the page's overlay grows out of.
    var bounds by remember(uri) { mutableStateOf<Rect?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .onGloballyPositioned { bounds = it.boundsInWindow() }
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = enabled) { onOpen(bounds) }
        ) {
            androidx.compose.foundation.Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(172.dp)
            )
            if (enabled) {
                Surface(
                    onClick = onRemove,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.42f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CurioIcon(
                            CurioIcons.Close,
                            "Remove photo",
                            tint = Color.White,
                            size = 15.dp
                        )
                    }
                }
            }
        }
        if (enabled) {
            BasicTextField(
                value = caption,
                onValueChange = onCaption,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = WritingFontFamily,
                    fontSize = 13.sp,
                    color = ink.copy(alpha = 0.72f)
                ),
                cursorBrush = SolidColor(personalAccentInk()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                decorationBox = { inner ->
                    Box {
                        if (caption.isEmpty()) {
                            Text(
                                "Add a caption",
                                style = TextStyle(
                                    fontFamily = WritingFontFamily,
                                    fontSize = 13.sp,
                                    color = ink.copy(alpha = 0.34f)
                                )
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Read-only rendering (the book page's review cards, journal previews)
// ────────────────────────────────────────────────────────────────────────────

/** Renders a saved document without any editing chrome. */
@Composable
internal fun PersonalDocView(
    doc: PersonalDoc,
    modifier: Modifier = Modifier,
    ink: Color = MaterialTheme.colorScheme.onSurface,
    accent: Color = personalAccent(),
    onOpenPhoto: (String, Rect?) -> Unit = { _, _ -> }
) {
    val quoteRule = personalQuoteRule()
    val quoteInk = personalQuoteColor().copy(alpha = 0.92f)
    val bulletInk = personalAccentInk()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        doc.blocks.forEach { block ->
            if (block.isPhoto) {
                // A saved page shows the picture SMALL — it is a page of
                // writing, not a gallery — and hands its bounds to the
                // overlay so tapping it grows out of exactly here.
                var bounds by remember(block.photo) { mutableStateOf<Rect?>(null) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { bounds = it.boundsInWindow() }
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenPhoto(block.photo.orEmpty(), bounds) }
                ) {
                    androidx.compose.foundation.Image(
                        painter = rememberAsyncImagePainter(block.photo.orEmpty()),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(156.dp)
                    )
                    if (block.caption.isNotBlank()) {
                        Text(
                            block.caption,
                            style = TextStyle(
                                fontFamily = WritingFontFamily,
                                fontSize = 13.sp,
                                color = ink.copy(alpha = 0.62f)
                            ),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else if (block.text.isNotBlank()) {
                val text = block.text
                val mask = runsToMask(text.length, block.runs)
                val isQuote = personalBlockIsQuote(text, mask)
                val isTitle = personalBlockCarries(text, mask, FLAG_TITLE)
                val isSmall = personalBlockCarries(text, mask, FLAG_SMALL)
                val isBullet = personalBlockCarries(text, mask, FLAG_BULLET)
                val isCheckbox = personalBlockCarries(text, mask, FLAG_CHECKBOX)
                // v389 — the same metrics and the same renderers as the editor
                // (this view draws a checklist row that the editor ticked).
                val lineHeight = if (isTitle) 31.sp else if (isSmall) 21.sp else 27.sp
                val markerFill = personalAccentInk()
                val markerOnFill = MaterialTheme.colorScheme.surface
                val markerOutline = ink.copy(alpha = 0.42f)
                val alignOf = if (block.align == PersonalAlign.CENTER) TextAlign.Center
                else TextAlign.Start
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            when {
                                isQuote -> Modifier
                                    .drawBehind {
                                        val barWidth = 3.dp.toPx()
                                        drawRoundRect(
                                            color = quoteRule,
                                            size = Size(barWidth, size.height),
                                            cornerRadius = CornerRadius(barWidth / 2f)
                                        )
                                    }
                                    .padding(start = 13.dp)
                                isCheckbox -> Modifier
                                    .drawBehind {
                                        drawPersonalCheckbox(
                                            checked = block.checked,
                                            outline = markerOutline,
                                            fill = markerFill,
                                            onFill = markerOnFill,
                                            lineHeight = lineHeight.toPx()
                                        )
                                    }
                                    .padding(start = PERSONAL_MARKER_LEAD)
                                isBullet -> Modifier
                                    .drawBehind {
                                        drawPersonalMarker(
                                            marker = block.markerStyle,
                                            ink = bulletInk,
                                            lineHeight = lineHeight.toPx()
                                        )
                                    }
                                    .padding(start = PERSONAL_MARKER_LEAD)
                                else -> Modifier
                            }
                        )
                ) {
                    Text(
                        text = personalAnnotated(
                            text, mask, ink, quoteInk, QUOTE_VIEW_SIZE,
                            titleSize = if (isTitle) TextUnit.Unspecified else TITLE_VIEW_SIZE,
                            smallSize = if (isSmall) TextUnit.Unspecified else SMALL_VIEW_SIZE
                        ),
                        style = when {
                            isTitle -> TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontSize = TITLE_VIEW_SIZE,
                                lineHeight = 31.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = alignOf
                            )
                            isSmall -> TextStyle(
                                fontFamily = WritingFontFamily,
                                fontSize = SMALL_VIEW_SIZE,
                                lineHeight = 21.sp,
                                textAlign = alignOf
                            )
                            else -> TextStyle(
                                fontFamily = WritingFontFamily,
                                fontSize = 16.sp,
                                lineHeight = 27.sp,
                                textAlign = alignOf
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Toolbar
// ────────────────────────────────────────────────────────────────────────────

/**
 * The tool dock — it rides ABOVE the keyboard (the caller pins it to the
 * bottom of an `imePadding()` column), so the tools are always under the
 * writer's thumb while the words stay above the keys.
 */
@Composable
internal fun PersonalToolDock(
    state: PersonalEditorState,
    onPickPhoto: () -> Unit,
    showJournalTools: Boolean = true,
    modifier: Modifier = Modifier,
    surface: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    val active = state.activeFlags()
    // The dock wears the app's own accent (the same one Home's hero uses), not
    // a hard rose — a member on the azure/hero-lane theme sees THEIR accent.
    val accent = personalAccent()
    val accentInk = personalAccentInk()
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surface,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                // Nine tools in a fixed row overflowed a narrow phone and cut
                // the last icons in half; the row scrolls, so every tool is
                // always reachable and nothing is clipped.
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            PersonalToolButton(
                label = "Bold",
                active = active and FLAG_BOLD != 0,
                accent = accentInk, ink = ink,
                onClick = { state.toggle(FLAG_BOLD) }
  ) {
  CurioIcon(CurioIcons.FormatBold, null, size = 20.dp)
  }

            PersonalToolButton(
                label = "Italic",
                active = active and FLAG_ITALIC != 0,
                accent = accentInk, ink = ink,
                onClick = { state.toggle(FLAG_ITALIC) }
            ) {
  CurioIcon(CurioIcons.FormatItalic, null, size = 20.dp)
            }
            PersonalToolButton(
                label = "Underline",
                active = active and FLAG_UNDERLINE != 0,
                accent = accentInk, ink = ink,
                onClick = { state.toggle(FLAG_UNDERLINE) }
            ) {
  CurioIcon(CurioIcons.FormatUnderline, null, size = 20.dp)
            }
            PersonalToolButton(
                label = "Strikethrough",
                active = active and FLAG_STRIKE != 0,
                accent = accentInk, ink = ink,
                onClick = { state.toggle(FLAG_STRIKE) }
            ) {
                StrikeGlyph()
            }
  if (showJournalTools) PersonalToolButton(
  label = "Large bold text",
                active = active and FLAG_TITLE != 0,
                accent = accentInk, ink = ink,
                onClick = { state.toggle(FLAG_TITLE) }
            ) {
                CurioIcon(CurioIcons.FormatText, null, size = 20.dp)
            }
            PersonalToolButton(
                label = "Todo checkbox",
                active = active and FLAG_CHECKBOX != 0,
                accent = accentInk,
                ink = ink,
                onClick = { state.toggleListStyle(FLAG_CHECKBOX) }
            ) {
                CurioIcon(CurioIcons.TaskAlt, null, size = 19.dp)
            }
            PersonalToolButton(
                label = "Small text",
                active = active and FLAG_SMALL != 0,
                accent = accentInk, ink = ink,
                onClick = { state.toggle(FLAG_SMALL) }
            ) {
  CurioIcon(CurioIcons.TextDecrease, null, size = 20.dp)
            }
            // v389 — THE MARKER MENU. The bullet tool opens a small anchored
            // menu of list styles instead of toggling one hard-coded dot: the
            // first row takes the list OFF the line, the rest give it that
            // marker (stored per line — see PersonalBlock.marker). The button
            // itself wears the focused line's own marker, so the dock always
            // echoes what the line is wearing.
            Box {
                var markerMenuOpen by remember { mutableStateOf(false) }
                val focusedMarker = state.markerOfFocused()
                PersonalToolButton(
                    label = "Bullet style",
                    active = active and FLAG_BULLET != 0,
                    accent = accentInk, ink = ink,
                    onClick = { markerMenuOpen = true }
                ) {
                    MarkerGlyph(focusedMarker)
                }
                DropdownMenu(
                    expanded = markerMenuOpen,
                    onDismissRequest = { markerMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { MarkerMenuLabel("No list") },
                        leadingIcon = {
                            CurioIcon(CurioIcons.Close, null, tint = ink, size = 18.dp)
                        },
                        trailingIcon = {
                            if (active and (FLAG_BULLET or FLAG_CHECKBOX) == 0) {
                                CurioIcon(CurioIcons.Check, null, tint = accentInk, size = 18.dp)
                            }
                        },
                        onClick = {
                            state.applyMarker(null)
                            markerMenuOpen = false
                        }
                    )
                    PersonalMarker.entries.forEach { marker ->
                        DropdownMenuItem(
                            text = { MarkerMenuLabel(marker.label) },
                            leadingIcon = { MarkerGlyph(marker) },
                            trailingIcon = {
                                if (
                                    active and FLAG_BULLET != 0 &&
                                    focusedMarker == marker
                                ) {
                                    CurioIcon(CurioIcons.Check, null, tint = accentInk, size = 18.dp)
                                }
                            },
                            onClick = {
                                state.applyMarker(marker)
                                markerMenuOpen = false
                            }
                        )
                    }
                }
            }
            if (showJournalTools) PersonalToolButton(
                label = "Quote",
                active = active and FLAG_QUOTE != 0,
                accent = accentInk, ink = ink,
                onClick = { state.toggle(FLAG_QUOTE) }
            ) {
                CurioIcon(CurioIcons.FormatQuote, null, size = 18.dp)
            }
            PersonalToolButton(
                label = "Align left",
                active = state.alignOfFocused() == PersonalAlign.START,
                accent = accentInk, ink = ink,
                onClick = { state.setAlign(PersonalAlign.START) }
            ) {
                AlignGlyph(center = false)
            }
            PersonalToolButton(
                label = "Align centre",
                active = state.alignOfFocused() == PersonalAlign.CENTER,
                accent = accentInk, ink = ink,
                onClick = { state.setAlign(PersonalAlign.CENTER) }
            ) {
                AlignGlyph(center = true)
            }
            if (showJournalTools) PersonalToolButton(
                label = "Add a photo",
                active = false,
                accent = accentInk, ink = ink,
                onClick = onPickPhoto
            ) {
                CurioIcon(CurioIcons.Image, null, size = 18.dp)
            }
        }
    }
}

/** A reliable strike glyph independent of the bundled font subset. */
@Composable
private fun StrikeGlyph() {
    val contentColor = LocalContentColor.current
    Text(
        text = "S",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.drawBehind {
            drawLine(
                color = contentColor,
                start = Offset(1.dp.toPx(), size.height * 0.56f),
                end = Offset(size.width - 1.dp.toPx(), size.height * 0.56f),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    )
}

/**
 * The marker as the DOCK draws it: the very renderer the page uses, so the
 * menu's preview is literally the glyph the line will wear (v389 — it used to
 * be a separate dot-and-rules drawing that matched nothing).
 */
@Composable
private fun MarkerGlyph(marker: PersonalMarker) {
    val ink = LocalContentColor.current
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        drawPersonalMarker(marker = marker, ink = ink, lineHeight = size.height)
    }
}

/** The marker menu's row label — the writing face, so the menu belongs to the
 *  page it edits. */
@Composable
private fun MarkerMenuLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = WritingFontFamily),
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** The alignment tools draw their own glyph (three rules), so the dock never
 *  depends on a font subset that has no align icons. */
@Composable
private fun AlignGlyph(center: Boolean) {
    val ink = LocalContentColor.current
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.8f.dp.toPx()
        val width = size.width
        val gaps = listOf(1f, 0.72f, 1f, 0.72f)
        gaps.forEachIndexed { index, fraction ->
            val y = size.height * (0.22f + index * 0.19f)
            val lineWidth = width * fraction
            val x = if (center) (width - lineWidth) / 2f else 0f
            drawLine(
                color = ink,
                start = Offset(x, y),
                end = Offset(x + lineWidth, y),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PersonalToolButton(
    label: String,
    active: Boolean,
    accent: Color,
    ink: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) accent.copy(alpha = 0.24f) else Color.Transparent,
        modifier = Modifier
            .size(36.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides if (active) accent else ink) {
                Box(
                    modifier = Modifier.semantics { contentDescription = label },
                    contentAlignment = Alignment.Center
                ) { content() }
            }
        }
    }
}

/** The focused block's alignment (the dock's lit state). */
private fun PersonalEditorState.alignOfFocused(): PersonalAlign {
    val id = focusedId ?: blockIds.firstOrNull() ?: return PersonalAlign.START
    return align(id)
}
