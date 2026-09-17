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
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
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

/**
 * THE PANEL BEHIND A QUOTED LINE — the same coffee, at a whisper (user
 * decision: "Coffee panel"). A quotation is a passage SOMEONE ELSE wrote, so it
 * reads as a panel laid onto the page rather than as the member's own voice; the
 * rule alone left that to one 3dp bar.
 */
@Composable
internal fun personalQuoteWash(): Color =
    personalQuoteColor().copy(alpha = if (isCurioDarkTheme()) 0.18f else 0.085f)

/**
 * THE QUOTE'S INK WHERE IT IS DRAWN OUTSIDE A PAGE — the same coffee family,
 * one shade deeper and theme-aware, for the social pull-quote (which is BOTH the
 * composer's preview and the post on the wall — one composable, see
 * SocialPullQuote). A quotation is never drawn in the member's accent: on a rose
 * or an azure theme the rule and the credit read as a highlight somebody had
 * selected rather than as somebody else's words (user request).
 */
@Composable
internal fun personalQuoteDeepColor(): Color =
    if (isCurioDarkTheme()) Color(0xFFC09263) else Color(0xFF5C3A20)

/**
 * v389 — THE BULLET'S OWN COLOUR: DEEP COFFEE, never the theme's accent.
 *
 * A list marker is typography, not a highlight: a rose dot on a rose-accent
 * theme made every bullet look like something the member had just selected (user
 * request: "make the bulletpoint colors darker coffe deep color not the theme
 * accent"). It wears the same coffee family as a quotation — the deep twin on a
 * light page, the milky one on a dark page so it cannot vanish.
 */
@Composable
internal fun personalBulletColor(): Color =
    if (isCurioDarkTheme()) Color(0xFFB08255) else Color(0xFF6E4A2E)

/**
 * HOW FAR A QUOTE PANEL REACHES INTO THE GAP BESIDE IT: half of it, so two
 * quoted lines typed over one Enter meet in the middle and read as ONE panel
 * instead of a stack of bars. The editor sets its blocks 6dp apart, the read-only
 * views 8dp — each half is the world it belongs to.
 */
private val QUOTE_JOIN_EDITOR = 3.dp
private val QUOTE_JOIN_VIEW = 4.dp

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
 * v389 — a row the writer swiped away, held by the editor so the floating Undo
 * pill can put it back: the block itself (words, style, tick and marker) plus
 * the place it stood in the list.
 */
internal data class PersonalRemovedRow(val block: PersonalBlock, val index: Int)

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

    /**
     * v389 — "start writing here". A tap anywhere on the blank part of a page
     * (under the last line, in the gap above the tools) hands the caret to the
     * LAST line at its end, which is what a writer means by tapping the empty
     * space in their own page. Before this the only way in was to find the one
     * empty line's own "Write…" placeholder and tap exactly on it (user
     * report). It never ADDS a line — the pen puts you where you left off.
     *
     * The caret rides [caret] (not the focus token): the token's rule is "the
     * first empty line", which is the right answer when a photo just landed but
     * the wrong one for a tap at the end of the writing.
     */
    fun focusLastLine() {
        val id = order.lastOrNull { block ->
            val b = blocks[block]
            b != null && !b.isPhoto && b.audio == null
        } ?: return
        focusedId = id
        caret = PersonalCaret(id, text(id).length)
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

    /**
     * v389 — THE ROW THE WRITER JUST SWIPED AWAY.
     *
     * A to-do row is `keepsChecklistRows`' business, and swiping one off the
     * list is the fastest way to say "not this anymore" — so it has to be the
     * fastest way to say "actually, yes" too. The whole row (its words, its
     * style, its tick, its marker and its PLACE in the list) is held here for
     * the floating Undo pill to put back exactly as it was. Held against the
     * editor rather than the screen because the pill watches the editor.
     */
    var lastRemovedRow by mutableStateOf<PersonalRemovedRow?>(null)
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
        insertAtCaret { PersonalBlock(id = newBlockId(), photo = uri) }
    }

    /**
     * v389 — a finished VOICE NOTE lands at the caret exactly like a picture:
     * what came before, the recording (its file, its length, its waveform), and
     * a fresh empty line to carry on in. The empty line is the point — a page
     * you talked into still has to be writable under what you said (user
     * request: "below we can still add notes").
     */
    fun insertVoice(voice: RecordedVoice) {
        insertAtCaret {
            PersonalBlock(
                id = newBlockId(),
                audio = voice.path,
                audioSeconds = voice.seconds,
                audioBars = voice.bars
            )
        }
    }

    /** The one place a BLOCK lands between a head and a tail (see
     *  [insertPhoto] / [insertVoice]). */
    private fun insertAtCaret(makeBlock: () -> PersonalBlock) {
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
        val photoBlock = makeBlock()
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

    // ── The to-do page's own gestures (v389) ───────────────────────────

    /**
     * v389 — REORDER: the row at [from] and the row at [to] trade places.
     *
     * A to-do list is read top-to-bottom, so its ORDER is part of its meaning —
     * and because [order] IS the stored document's order, a dragged list is
     * saved the way it reads, with no separate rank column to fall out of sync.
     * The move is a real list edit (not a draft), so the drag can move a row
     * more than one step and every step already happened where the finger put
     * it.
     */
    fun moveBlock(from: Int, to: Int) {
        if (from == to) return
        if (from !in order.indices || to !in order.indices) return
        val id = order.removeAt(from)
        order.add(to, id)
        onDocChanged(doc())
    }

    /**
     * v389 — SWIPE AWAY: the row leaves the page and is HELD ([lastRemovedRow])
     * so the floating Undo pill can put it back exactly where it was.
     *
     * The row keeps its id, so restoring it rebuilds the same block with the
     * same tick and marker — the pill undoes the swipe, not the writing on the
     * row. The one thing it does not bring back is a fresh blank line the
     * removal had to leave behind when the list would otherwise be empty: undo
     * restores what was there, not the placeholder the page needed.
     */
    fun removeRow(id: String) {
        val index = order.indexOf(id)
        val block = blocks[id]
        if (index < 0 || block == null) return
        order.remove(id)
        blocks.remove(id)
        masks.remove(id)
        selections.remove(id)
        compositions.remove(id)
        lastRemovedRow = PersonalRemovedRow(block, index)
        if (order.isEmpty()) {
            val fresh = PersonalBlock(id = newBlockId())
            order.add(fresh.id)
            blocks[fresh.id] = fresh
            masks[fresh.id] = emptyMask(0)
        }
        if (focusedId == id) focusedId = order.getOrNull(index.coerceAtMost(order.size - 1))
        onDocChanged(doc())
    }

    /** The Undo pill: puts the swiped row back, at the place it was swiped from. */
    fun restoreRemovedRow() {
        val removed = lastRemovedRow ?: return
        lastRemovedRow = null
        val block = removed.block
        // The page can never be EMPTY, so swiping a list's last row leaves a
        // fresh blank line behind. Undo drops it again.
        val placeholder = order.singleOrNull()
        val placeholderBlock = placeholder?.let { blocks[it] }
        if (
            placeholder != null && placeholderBlock != null &&
            placeholderBlock.text.isEmpty() && !placeholderBlock.isPhoto && placeholderBlock.audio == null
        ) {
            order.remove(placeholder)
            blocks.remove(placeholder)
            masks.remove(placeholder)
            selections.remove(placeholder)
            compositions.remove(placeholder)
        }
        val index = removed.index.coerceIn(0, order.size)
        order.add(index, block.id)
        blocks[block.id] = block
        masks[block.id] = runsToMask(block.text.length, block.runs)
        onDocChanged(doc())
    }

    /** Dismisses the Undo pill without putting anything back. */
    fun clearRemovedRow() {
        lastRemovedRow = null
    }

    /**
     * The flags that cover EVERY visible character of a line — the tools that
     * are really ON for that whole line (which is also what the dock lights /
     * what [activeFlags] reports for a caret with no selection). A line with no
     * visible characters has none.
     */
    private fun lineFlags(text: String, mask: IntArray): Int {
        var flags = ALL_FLAGS
        var seen = false
        for (i in text.indices) {
            if (text[i].isWhitespace()) continue
            seen = true
            flags = flags and mask.getOrElse(i) { 0 }
        }
        return if (seen) flags else 0
    }

    /** True when the block at [id] is a quoted line (the quote panel's run). */
    fun isQuoteLine(id: String): Boolean {
        val block = blocks[id] ?: return false
        if (block.isPhoto || block.isAudio) return false
        return personalBlockCarries(block.text, mask(id), FLAG_QUOTE)
    }

    /**
     * Enter: the paragraph splits at the caret and the caret lands at the start
     * of the new one.
     *
     * v389 — THE LINE'S TOOLS CROSS THE BREAK. Before this the styles were cut
     * in two (so the words after the caret kept theirs) but the NEW line was
     * plain prose and the dock's button went dark, which read as the editor
     * dropping the tool mid-sentence (user report: "when i have a tool selected
     * from the tool nbar and i tap enter it deselects the tool"). Now the whole
     * line's own tools — bold, a quote, a title, a bullet, a checklist row —
     * carry to the new line, and an ARMED tool (nothing typed yet) carries too,
     * because that is the same promise the editor makes everywhere else: what is
     * switched on applies to what comes next.
     *
     * A checklist page keeps its own manner: Enter at the end of a row makes the
     * next row (the row's own checkbox is one of the line's tools, so it carries),
     * and Enter on an EMPTY row ends the list.
     */
    fun splitAtCaret(id: String) {
        val block = blocks[id] ?: return
        val blockMask = mask(id)
        // What the line is wearing decides what the new one inherits; an armed
        // tool (an empty line) inherits itself.
        val headFlags = lineFlags(block.text, blockMask)
        val carried = when {
            // The TO-DO page's own manner, kept: Enter on an EMPTY row ends the
            // list instead of arming the next row for ever (a page of checklists
            // has to stop somewhere).
            keepsChecklistRows && block.text.isEmpty() -> 0
            headFlags != 0 -> headFlags
            else -> armed
        }
        val caretIndex = (selections[id]?.start ?: block.text.length)
            .coerceIn(0, block.text.length)
        val index = order.indexOf(id)
        if (index < 0) return
        val head = block.copy(
            text = block.text.take(caretIndex),
            runs = maskToRuns(blockMask.copyOfRange(0, caretIndex))
        )
        val tailText = block.text.drop(caretIndex)
        // A whole-line tool is a WHOLE-LINE tool on both sides of the break; a
        // partly-styled line just keeps its own characters' styles.
        val tailMask = if (headFlags != 0 && tailText.isNotEmpty()) {
            IntArray(tailText.length) { headFlags }
        } else {
            blockMask.copyOfRange(caretIndex, block.text.length)
        }
        val tail = PersonalBlock(
            id = newBlockId(),
            text = tailText,
            runs = maskToRuns(tailMask),
            align = if (tailText.isEmpty()) block.align else PersonalAlign.START
        )
        masks[id] = runsToMask(head.text.length, head.runs)
        blocks[id] = head
        order.add(index + 1, tail.id)
        blocks[tail.id] = tail
        masks[tail.id] = runsToMask(tail.text.length, tail.runs)
        selections[tail.id] = TextRange(0)
        focusedId = tail.id
        caret = PersonalCaret(tail.id, 0)
        armed = carried
        onDocChanged(doc())
    }

    /**
     * v389 — BACKSPACE AT THE START OF A LINE takes that line into the one above
     * it, the way every editor does.
     *
     * The split used to be one-way: Enter made a new line and NOTHING could take
     * it back, so one accidental Enter left an empty paragraph the writer could
     * never remove (user report: "when i use enter to create a new line it
     * create the new line but when i type back it doesnt delete it"). An EMPTY
     * line is dropped outright; a line with words in it hands them to the end of
     * the line above. Returns false when there is nowhere to merge into (the
     * first line, or a block above that is a photo or a voice note), so the key
     * falls through to the field.
     */
    fun mergeWithPrevious(id: String): Boolean {
        val index = order.indexOf(id)
        if (index <= 0) return false
        val block = blocks[id] ?: return false
        val previousId = order[index - 1]
        val previous = blocks[previousId] ?: return false
        if (previous.isPhoto || previous.isAudio) return false
        val ownMask = masks[id] ?: emptyMask(0)
        val previousMask = mask(previousId)
        val previousText = previous.text
        val at = previousText.length
        val mergedText = previousText + block.text
        val mergedMask = IntArray(mergedText.length) { i ->
            if (i < at) previousMask.getOrElse(i) { 0 }
            else ownMask.getOrElse(i - at) { 0 }
        }
        order.removeAt(index)
        blocks.remove(id)
        masks.remove(id)
        selections.remove(id)
        compositions.remove(id)
        blocks[previousId] = previous.copy(text = mergedText, runs = maskToRuns(mergedMask))
        masks[previousId] = mergedMask
        // Where the two halves met is where the caret belongs — the exact
        // position the Enter was pressed at, one keystroke ago.
        selections[previousId] = TextRange(at)
        focusedId = previousId
        caret = PersonalCaret(previousId, at)
        onDocChanged(doc())
        return true
    }

    /**
     * v389 — "ADD CHAPTER": a marker lands on the page as its own TITLE line,
     * with the caret on it and the line ARMED as a title, so the chapter's name
     * arrives as the heading it is (the same mechanism the dock's title button
     * uses).
     *
     * It is an ordinary block in the ordinary order — which is exactly what
     * lets a book's whole review stay ONE page: the markers are prose, and the
     * read view folds a chapter's own review in under the marker that names it.
     */
    fun insertTitleLine() {
        val after = focusedId?.let { order.indexOf(it) }?.takeIf { it >= 0 }
            ?: order.indexOfLast { id -> !(blocks[id]?.isPhoto ?: false) }
        val at = if (after < 0) order.size else (after + 1).coerceAtMost(order.size)
        val block = PersonalBlock(id = newBlockId())
        order.add(at, block.id)
        blocks[block.id] = block
        masks[block.id] = emptyMask(0)
        caret = PersonalCaret(block.id, 0)
        focusedId = block.id
        armed = FLAG_TITLE
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
    // v389 — one drag for the whole list: the to-do page's rows share it, so
    // the row under the finger and the rows it passes agree about one gesture.
    val rowDrag = remember { PersonalRowDragState() }
    Column(
        modifier = modifier.clickable(enabled = enabled) { state.focusLastLine() },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        state.blockIds.forEachIndexed { index, id ->
            val block = state.block(id) ?: return@forEachIndexed
            // v389 — the blocks are KEYED by their own id. The to-do page can
            // now reorder them, and without the key Compose would hand each
            // slot's remembered state to whichever block slid into it — the
            // caret, the focus requester and the field's own scroll would all
            // follow the POSITION instead of the row.
            key(id) {
                // v389 — whether a QUOTE panel has a quoted neighbour decides
                // how far it reaches into the gap, so a quotation typed over
                // several Enters draws as one continuous panel (see
                // personalQuoteWash).
                val quoteAbove = index > 0 && state.isQuoteLine(state.blockIds[index - 1])
                val quoteBelow = index < state.blockIds.lastIndex &&
                    state.isQuoteLine(state.blockIds[index + 1])
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
                } else if (block.isAudio) {
                    // v389 — a voice note in the page: the waveform is the block, and
                    // the writing carries on under it.
                    PersonalVoicePageBlock(
                        path = block.audio.orEmpty(),
                        seconds = block.audioSeconds,
                        bars = block.audioBars,
                        ink = ink,
                        accent = accent,
                        enabled = enabled,
                        onRemove = { state.removeBlock(id) }
                    )
                } else if (state.keepsChecklistRows) {
                    // A to-do page: every text row can be picked up (long press),
                    // carried to another place, and swiped sideways off the list.
                    PersonalTodoRow(
                        id = id,
                        index = index,
                        state = state,
                        drag = rowDrag,
                        enabled = enabled
                    ) {
                        PersonalTextBlock(
                            id = id,
                            state = state,
                            ink = ink,
                            accent = accent,
                            enabled = enabled,
                            quoteJoinAbove = quoteAbove,
                            quoteJoinBelow = quoteBelow
                        )
                    }
                } else {
                    PersonalTextBlock(
                        id = id,
                        state = state,
                        ink = ink,
                        accent = accent,
                        enabled = enabled,
                        quoteJoinAbove = quoteAbove,
                        quoteJoinBelow = quoteBelow
                    )
                }
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
    enabled: Boolean,
    /** The gap above/below holds another quoted line — see QUOTE_JOIN_EDITOR. */
    quoteJoinAbove: Boolean = false,
    quoteJoinBelow: Boolean = false
) {
    val text = state.text(id)
    val mask = state.mask(id)
    val align = state.align(id)
    val quoteRule = personalQuoteRule()
    val quoteWash = personalQuoteWash()
    val quoteInk = personalQuoteColor().copy(alpha = 0.92f)
    val bulletInk = personalBulletColor()
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
    //
    // v389 — and it belongs to a PRISTINE page, not to every empty line: after
    // Enter the FIRST line's hint stayed while the new line showed its own, so
    // two empty paragraphs read as two "Write…" placeholders (user report).
    // It is the first line's, and it goes for good once anything is written.
    val showHint = text.isEmpty() && !state.hasText() && state.blockIds.firstOrNull() == id
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
                            val join = QUOTE_JOIN_EDITOR.toPx()
                            val top = if (quoteJoinAbove) -join else 0f
                            val bottom = if (quoteJoinBelow) join else 0f
                            val panelHeight = size.height + (bottom - top)
                            drawRoundRect(
                                color = quoteWash,
                                topLeft = Offset(0f, top),
                                size = Size(size.width, panelHeight),
                                cornerRadius = CornerRadius(9.dp.toPx())
                            )
                            drawRoundRect(
                                color = quoteRule,
                                topLeft = Offset(0f, top),
                                size = Size(barWidth, panelHeight),
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
                if (!enabled || event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                // Enter makes a NEW line (a block), so a line tool can point
                // at the line the caret is on. Shift+Enter keeps the plain
                // newline inside the paragraph.
                if (
                    (event.key == Key.Enter || event.key == Key.NumPadEnter) &&
                    !event.isShiftPressed
                ) {
                    state.splitAtCaret(id)
                    return@onPreviewKeyEvent true
                }
                // BACKSPACE AT THE START OF A LINE takes the line back into the
                // one above it — the key beside Enter has to be able to undo
                // what Enter did (user report: "when i type back it doesnt
                // delete it"). The live values are read here rather than
                // captured, because this runs between two compositions.
                if (event.key == Key.Backspace) {
                    val live = state.text(id)
                    val selection = state.selection(id)
                    val atLineStart = live.isEmpty() ||
                        (selection != null && selection.collapsed && selection.start == 0)
                    if (atLineStart) {
                        return@onPreviewKeyEvent state.mergeWithPrevious(id)
                    }
                }
                false
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

/**
 * v389 — A PAGE'S PHOTO, DECODED FOR THE BOX IT IS DRAWN IN.
 *
 * Coil's default decode FITS the source inside the target box, so a landscape
 * photo in a wide, short page thumbnail came back as a bitmap SMALLER than the
 * box — and `ContentScale.Crop` then scaled it back UP, which is exactly the
 * blurry picture the member reported ("the image preview isnt good … so low
 * quality"). This asks for a size that COVERS the box ([Scale.FILL]) at the
 * composable's own pixel size, so nothing is ever upscaled, and draws it with
 * `FilterQuality.High`.
 *
 * The size is measured rather than guessed: one frame at zero, then the real
 * request — cheaper than decoding a full-resolution bitmap per photo, which a
 * page with four of them would feel.
 */
@Composable
internal fun PersonalPagePhoto(
    uri: String,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var box by remember(uri) { mutableStateOf(IntSize.Zero) }
    val request = remember(uri, box) {
        ImageRequest.Builder(context)
            .data(uri)
            .scale(Scale.FILL)
            .apply { if (box.width > 0 && box.height > 0) size(box.width, box.height) }
            .build()
    }
    // The PAINTER overload is the one this project already uses for a page's
    // photos (see PersonalPhotoOverlay): it carries no `filterQuality`, so the
    // crispness has to come from the DECODE — which is exactly what the request
    // above asks for (a size that covers the box, never upscaled afterwards).
    androidx.compose.foundation.Image(
        painter = rememberAsyncImagePainter(request),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .onSizeChanged { box = it }
    )
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
            PersonalPagePhoto(uri = uri, height = 172.dp)
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
    onOpenPhoto: (String, Rect?) -> Unit = { _, _ -> },
    /**
     * v389 — a TITLE line can carry a FOLD. The book's whole-book review uses
     * its title lines as chapter markers, so it folds each chapter's own
     * review in right under the marker that names it; every other page passes
     * nothing and reads exactly as it did.
     */
    afterTitle: (@Composable (String) -> Unit)? = null
) {
    val quoteRule = personalQuoteRule()
    val quoteWash = personalQuoteWash()
    val quoteInk = personalQuoteColor().copy(alpha = 0.92f)
    val bulletInk = personalBulletColor()
    // v389 — a quoted line is ONE thing with its quoted neighbour, so which of
    // the two sides leads into another quoted line is decided once, here, and
    // the panels reach into the gap to meet (see QUOTE_JOIN_VIEW).
    fun isQuoteRun(block: PersonalBlock): Boolean =
        !block.isPhoto && !block.isAudio && block.text.isNotBlank() &&
            personalBlockIsQuote(block.text, runsToMask(block.text.length, block.runs))
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        doc.blocks.forEachIndexed { index, block ->
            val quoteAbove = index > 0 && isQuoteRun(doc.blocks[index - 1])
            val quoteBelow = index < doc.blocks.lastIndex && isQuoteRun(doc.blocks[index + 1])
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
                    PersonalPagePhoto(
                        uri = block.photo.orEmpty(),
                        height = 156.dp
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
            } else if (block.isAudio) {
                // v389 — a saved voice note reads as the waveform it was
                // recorded into: play it, or tap a moment to jump there.
                PersonalVoiceBar(
                    path = block.audio.orEmpty(),
                    seconds = block.audioSeconds,
                    bars = block.audioBars,
                    ink = ink,
                    accent = accent
                )
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
                                        val join = QUOTE_JOIN_VIEW.toPx()
                                        val top = if (quoteAbove) -join else 0f
                                        val bottom = if (quoteBelow) join else 0f
                                        val panelHeight = size.height + (bottom - top)
                                        drawRoundRect(
                                            color = quoteWash,
                                            topLeft = Offset(0f, top),
                                            size = Size(size.width, panelHeight),
                                            cornerRadius = CornerRadius(9.dp.toPx())
                                        )
                                        drawRoundRect(
                                            color = quoteRule,
                                            topLeft = Offset(0f, top),
                                            size = Size(barWidth, panelHeight),
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
                if (isTitle) afterTitle?.invoke(text)
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
                TodoGlyph(active = active and FLAG_CHECKBOX != 0)
            }
            PersonalToolButton(
                label = "Small text",
                active = active and FLAG_SMALL != 0,
                accent = accentInk, ink = ink,
                onClick = { state.toggle(FLAG_SMALL) }
            ) {
  CurioIcon(CurioIcons.TextDecrease, null, size = 20.dp)
            }
            // v389 — THE MARKER MENU. The bullet tool gives the line the FIRST
            // marker on its first tap (a dot — the common case, one tap) and
            // opens the menu of styles on the tap after that, once the line is
            // already a list (user request: "by default add the 1st bulletpoint
            // tapping it again should show the drop down"). The button itself
            // wears the focused line's own marker, so the dock always echoes
            // what the line is wearing.
            Box {
                var markerMenuOpen by remember { mutableStateOf(false) }
                val focusedMarker = state.markerOfFocused()
                val bulletOn = active and FLAG_BULLET != 0
                PersonalToolButton(
                    label = "Bullet style",
                    active = bulletOn,
                    accent = accentInk, ink = ink,
                    onClick = {
                        if (bulletOn) markerMenuOpen = true
                        else state.applyMarker(PersonalMarker.entries.first())
                    }
                ) {
                    MarkerGlyph(focusedMarker)
                }
                DropdownMenu(
                    expanded = markerMenuOpen,
                    onDismissRequest = { markerMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { MarkerMenuLabel("Remove list") },
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

/**
 * v389 — THE TO-DO TOOL'S OWN GLYPH: the page's CHECKBOX, drawn.
 *
 * The bundled icon was a struck-through task glyph that read as a finished item
 * rather than as the thing the button MAKES — and it shared no shape with the
 * boxes the page draws down the margin (user request: "the tool bar check box
 * icon change it"). This is that box and that tick, at dock size: the same
 * rounded square, the same tick, filled with the accent when the line is
 * already a row.
 */
@Composable
private fun TodoGlyph(active: Boolean) {
    val ink = LocalContentColor.current
    val onFill = MaterialTheme.colorScheme.surface
    androidx.compose.foundation.Canvas(modifier = Modifier.size(19.dp)) {
        val stroke = 1.7f.dp.toPx()
        val side = size.minDimension * 0.80f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val corner = CornerRadius(side * 0.30f)
        drawRoundRect(
            color = ink,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = corner,
            style = if (active) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = stroke)
        )
        val tick = Path().apply {
            moveTo(left + side * 0.24f, top + side * 0.53f)
            lineTo(left + side * 0.43f, top + side * 0.73f)
            lineTo(left + side * 0.78f, top + side * 0.30f)
        }
        drawPath(
            path = tick,
            color = if (active) onFill else ink,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
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
