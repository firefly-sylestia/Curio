package com.curio.app.features.personal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import com.curio.app.ui.components.CurioMenuToggle
import com.curio.app.ui.components.TextHistoryBrowser
import com.curio.app.ui.components.TextHistoryRestoreMode
import com.curio.app.ui.components.rememberTextHistoryCapture
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
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
import com.curio.app.ui.theme.GeomFontFamily
import com.curio.app.ui.theme.SpaceMonoFontFamily
import com.curio.app.ui.theme.WritingFontFamily
import com.curio.app.data.openSearchUrl
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

/**
 * THE SIZE OF A PAGE WHOSE ROWS ARE THE CONTENT — the to-do list (v389e).
 *
 * A to-do row is not prose: it is a line you act on with a thumb, and it read as
 * one more paragraph (user request: "make the todo list fonts overall page font
 * and checkboxsize fonts etc they should be larger"). Both sides of the page —
 * the pen and the eye — take this size, and the row's box grows with it (see
 * [ROW_MARKER_SIZE]), so the box stays level with the words it labels.
 */
internal val ROW_VIEW_SIZE = 22.sp
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
 * v389 — THE FOUR MARKER PENS.
 *
 * Keyed, not coloured, in storage (see [PersonalRun.highlight]) so the palette
 * can be tuned without touching a single saved note. They are the READER's own
 * four highlighters, which means a page marked in the journal and a passage
 * marked in a book are the same four inks.
 *
 * Painted at a wash alpha by [personalAnnotated], so the ink under them is
 * always the page's own — a marker is a pen held over the text, not a fill.
 */
internal fun personalHighlightInk(key: String): Color = when (key) {
    "rose" -> Color(0xFFD98A8A)
    "sage" -> Color(0xFF8FB08A)
    "sky" -> Color(0xFF7FA8C9)
    else -> Color(0xFFE0A33C)
}

/** The pen's name as the menu shows it. */
/**
 * v389 — AN ALIGNMENT, AS THE TEXT ENGINE WANTS IT.
 *
 * One mapping, in one place, because the four kinds are drawn by four callers
 * (both canvas surfaces, both document views) and a fifth kind added in three
 * of them would silently lay out as left.
 */
internal fun PersonalAlign.toTextAlign(): TextAlign = when (this) {
    PersonalAlign.START -> TextAlign.Start
    PersonalAlign.CENTER -> TextAlign.Center
    PersonalAlign.END -> TextAlign.End
    PersonalAlign.JUSTIFY -> TextAlign.Justify
}

/**
 * v389 — A FACE, AS THE TEXT ENGINE WANTS IT.
 *
 * The four are what [PERSONAL_FONT_KEYS] names, and each is a family the app
 * ALREADY bundles, chosen so the menu is four genuinely different voices rather
 * than four weights of one: the page's own writing serif (Lora, by far the best
 * reading face at writing size, and what every note written before this menu
 * already is), a geometric sans for notes that are lists and fragments, a mono
 * for anything with code or numbers in it, and the display serif the titles
 * already wear.
 *
 * A TITLE still forces the display serif when no face was picked, because that
 * is what the title tool has always meant; an explicit face wins, because a
 * member who chose one meant it.
 */
internal fun personalFontFamilyOf(flags: Int): FontFamily? = when (fontKeyOf(flags)) {
    "sans" -> GeomFontFamily
    "mono" -> SpaceMonoFontFamily
    "display" -> FrauncesFontFamily
    else -> if (flags and FLAG_TITLE != 0) FrauncesFontFamily else null
}

// ── v392 ── CLICKABLE LINKS ──────────────────────────────────────────────
private const val PERSONAL_LINK_TAG = "personal_url"
private val URL_REGEX = Regex(
    "https?://[\u0021-\u007E]+" // ASCII printable URL characters
)

/**
 * Adds clickable URL annotations to an existing AnnotatedString.
 * Each match is tagged [PERSONAL_LINK_TAG] so the read-only view can open it
 * on tap with the coffee-dark underline style.
 */
private fun personalAnnotateLinks(base: AnnotatedString): AnnotatedString {
    val text = base.text
    val matches = URL_REGEX.findAll(text).toList()
    if (matches.isEmpty()) return base
    val annotated = androidx.compose.ui.text.buildAnnotatedString {
        append(base)
        for (match in matches) {
            addStyle(
                SpanStyle(
                    color = Color(0xFF5C3A20),
                    textDecoration = TextDecoration.Underline
                ),
                match.range.first,
                match.range.last + 1
            )
            addStringAnnotation(
                PERSONAL_LINK_TAG,
                match.value,
                match.range.first,
                match.range.last + 1
            )
        }
    }
    return annotated
}
/** The face's name as the menu shows it. */
internal fun personalFontLabel(key: String): String = when (key) {
    "sans" -> "Sans"
    "mono" -> "Mono"
    "display" -> "Display"
    else -> "Serif"
}

/** The family the menu previews that name in — the menu is the four faces, so
 *  the row itself is set in the face it is offering. */
internal fun personalFontPreview(key: String): FontFamily = when (key) {
    "sans" -> GeomFontFamily
    "mono" -> SpaceMonoFontFamily
    "display" -> FrauncesFontFamily
    else -> WritingFontFamily
}

internal fun personalHighlightLabel(key: String): String = when (key) {
    "rose" -> "Rose"
    "sage" -> "Sage"
    "sky" -> "Sky"
    "amber" -> "Amber"
    else -> key.replaceFirstChar { it.uppercase() }
}

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
 * v389e — THE TO-DO BOX: a bigger mark for a bigger row.
 *
 * The box has always been [PERSONAL_MARKER_SIZE] whatever the line height around
 * it — which was right while every page wrote at one size, and too small once the
 * to-do list's rows grew (a tick a thumb is aiming at). A row page draws this one
 * instead, and indents its words by [ROW_MARKER_LEAD] so the writing still lines
 * up under the first word.
 */
internal val ROW_MARKER_SIZE = 22.dp
internal val ROW_MARKER_LEAD = ROW_MARKER_SIZE + PERSONAL_MARKER_GAP

/**
 * v389 — A CHECKLIST ROW CAN BE TICKED WHILE THE PAGE IS BEING READ.
 *
 * The tick lived in the editor alone, so a to-do list had to be opened with the
 * pen down to finish anything — while a list is exactly the page whose READING
 * side you act on. [PersonalDocView] draws the box; the page that owns the
 * document (see `PersonalWritingPage`) provides the write here, because a view
 * renders a document it must never edit itself. The index is the BLOCK's own
 * position in the document, which is what the view has to hand.
 */
internal val LocalPersonalCheckToggle = staticCompositionLocalOf<((Int) -> Unit)?> { null }

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
    lineHeight: Float,
    /** v389e — the box's own width. A to-do row asks for the bigger mark
     *  ([ROW_MARKER_SIZE]); every other page keeps the shared one. */
    box: Dp = PERSONAL_MARKER_SIZE
) {
    val size = box.toPx()
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
            // The pen rides in the same int as the flags, so a marker-only
            // stretch (flags == its colour bits) has to paint a background even
            // though no flag is set — which is why this is inside `flags != 0`
            // rather than in a branch of its own.
            val pen = highlightKeyOf(flags)
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
                    fontFamily = personalFontFamilyOf(flags),
                    fontWeight = when {
                        flags and FLAG_BOLD != 0 -> FontWeight.Bold
                        flags and FLAG_TITLE != 0 -> FontWeight.SemiBold
                        else -> null
                    },
                    fontStyle = if (flags and FLAG_ITALIC != 0) FontStyle.Italic else null,
                    // A WASH, so the words stay the page's ink — and unspecified
                    // rather than transparent, which would punch a hole through
                    // a quote panel it sat inside.
                    background = if (pen.isEmpty()) {
                        Color.Unspecified
                    } else {
                        personalHighlightInk(pen).copy(alpha = 0.40f)
                    },
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
 * v389 — WHERE A PAGE'S TITLE LINES SIT, for a page that cannot be handed the
 * callback.
 *
 * A writing page owns both halves of itself, but the READING half is the
 * CALLER's view (`readView`), so the page has nothing to pass it. It provides
 * this local instead, and both [PersonalCanvas] and [PersonalDocView] fall back
 * to it when their own parameter is null — which is what lets the journal's
 * pinned section work on the reading side as well as the writing one.
 */
/**
 * v389g — A DOUBLE TAP ON A PAGE BEING READ PUTS THE PEN BACK, ANYWHERE ON IT.
 *
 * The host page already answers a double tap with the pen (see the read side of
 * `PersonalWritingPage`), but it answers it from a detector UNDER the reading
 * view — and a tap that lands on WORDS never reaches it: the read view's text
 * consumes its own taps, because it has to (a URL in a page opens the browser).
 * So the one place a reader most wants the pen — on the writing they are looking
 * at — was the one place the gesture did nothing (user report: "double tap on the
 * whole page too not just blank area, but also over the text works too").
 *
 * The host provides the action here; the read view's own text detector consults
 * it on a double tap. A host that provides nothing keeps today's behaviour, and a
 * surface that is not a writing page (a share preview, an export) is unaffected.
 */
internal val LocalPersonalTapToEdit = compositionLocalOf<(() -> Unit)?> { null }

internal val LocalPersonalTitleReport =
    staticCompositionLocalOf<
        ((id: String, label: String, top: Float, bottom: Float) -> Unit)?
        > { null }

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

    /**
     * v389e — THE PAGE'S ONE DRAG.
     *
     * Which block is being carried, which rows have made room and where the
     * finger is. It belongs to the EDITOR because two things need it and they
     * live on either side of the writing surface: the canvas runs the gesture,
     * and the PAGE (which owns the scroll) follows a carried block to the fold
     * and scrolls it along (see PersonalWritingPage). One per page either way, so
     * a to-do list and a voice note on the same page still share a single
     * gesture.
     */
    val rowDrag = PersonalRowDragState()

    /** The block the keyboard is in — the target of every tool. */
    var focusedId by mutableStateOf<String?>(null)
        private set
    var focusRequestToken by mutableIntStateOf(0)
        private set

    fun requestFocusOnEmptyLine() {
        focusRequestToken++
    }

    /**
     * v389 — THE PAGE WAS TAPPED (see [focusLastLine]).
     *
     * [focusedId] says which line the keyboard is in and [caret] says where the
     * caret should go, but neither of them can say WHEN: the caret is consumed
     * the moment the line it names honours it, so a tap on the blank part of a
     * page whose caret is already in that line changed NOTHING (user report:
     * "add proper tap to start writing in blank always even when the cursor was
     * there"). This is the proof that a finger landed, and it names the line it
     * asked for.
     */
    var tapTarget by mutableStateOf<String?>(null)
        private set
    var tapTick by mutableIntStateOf(0)
        private set

    /** Called by the line the tap named, so no other line answers it twice. */
    fun consumeTap(id: String) {
        if (tapTarget == id) tapTarget = null
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
        // ── A TAP ELSEWHERE LETS GO OF WHAT WAS SELECTED (v389d) ────────
        //
        // A selected passage used to survive a tap on the blank part of the
        // page: the only way to drop it was to tap the SAME line again and put
        // the caret somewhere in it, which is not how any editor behaves (user
        // report: "when I've something selected in the text for journal book
        // review and all and i click the blank area below it should auto
        // deselect. instead i have to tap that exact line to deselect it").
        // Tapping the page is a deliberate "not that" — so every range is
        // collapsed and the page-wide wash goes with it.
        if (selections.any { (_, range) -> !range.collapsed } || pageSelected) {
            selections.keys.toList().forEach { key ->
                selections[key] = TextRange(text(key).length)
            }
            pageSelected = false
        }
        focusedId = id
        caret = PersonalCaret(id, text(id).length)
        // A pending OFF belongs to the place the caret was, not to this one.
        armedOff = 0
        // …and the TAP itself, so the line takes the caret (and the keyboard)
        // again even when both of the lines above are already true of it.
        tapTarget = id
        tapTick++
    }

    fun armCheckboxOnEmptyLine() {
        armed = (armed and (FLAG_BULLET or FLAG_CHECKBOX).inv()) or FLAG_CHECKBOX
    }

    /** Tools switched on with nothing to apply them to (an empty line). */
    var armed by mutableIntStateOf(0)

    /**
     * v389 — TOOLS SWITCHED **OFF** FOR WHAT IS TYPED NEXT.
     *
     * With no selection the dock is an INPUT STYLE, not a command that rewrites
     * the line (user report: "the bold italic underline strikethough … size
     * quotes etc they should not work for the whole line when i tap to active
     * them it should only work after the text written just like in rich text
     * editor"). [armed] is the tools switched ON; this is the tools switched
     * OFF — the only way to write plain words in the middle of a bold sentence,
     * which is what tapping Bold with the caret inside a bold run now means.
     * The two are always disjoint ([toggle] keeps them so).
     */
    var armedOff by mutableIntStateOf(0)
        private set

    /**
     * v389 — THE PEN SWITCHED ON FOR WHAT IS TYPED NEXT.
     *
     * `null` is "whatever pen the caret already sits in" — the state a marker
     * spends almost all its life in, and the reason this is nullable: the pen
     * being TAKEN OFF is a real, distinct setting (0), and a plain Int could not
     * tell it apart from "unchanged". Cleared as soon as a keystroke consumes
     * it, the same as [armed].
     */
    var armedHighlight by mutableStateOf<Int?>(null)
        private set

    /** v389 — THE FACE SWITCHED ON FOR WHAT IS TYPED NEXT, with [armedHighlight]'s
     *  own meaning of null ("unchanged"), so the page's own face can be chosen
     *  deliberately as well as inherited. */
    var armedFont by mutableStateOf<Int?>(null)
        private set

    /** The face the caret sits in — "" for the page's own. What the dock's font
     *  menu ticks. */
    fun fontOfFocused(): String {
        // v393 — THE ARMED FACE LIT AT ONCE. The button reads this to decide
        // whether it is lit, and the old version only read the caret's
        // neighbours — so a face chosen with the caret standing in plain text
        // stayed dark until the next keystroke moved the mask (user report:
        // "i select a tool such as highlight or font change its not showing as
        // active but when i type then it shows active"). The armed face wins,
        // exactly as [activeFlags] lets the armed tools win — and the page's
        // own face, chosen on purpose, reads back as "" so the button correctly
        // goes dark again.
        armedFont?.let { return fontKeyOf(it) }
        val id = focusedId ?: return ""
        return fontKeyOf(caretFlags(id))
    }

    /**
     * v393 — THE FACE IS THE PARAGRAPH'S, NOT WORD-BASED (user report: "the
     * font chnage should act for the whole paragraph not word based"). A block
     * is the page's unit of writing — a paragraph and its wrapped lines are
     * one — so the chosen face goes over every character of it in one sweep.
     * An empty line arms the face instead, so the first words typed arrive in
     * it. A selection no longer narrows the tool: the paragraph is the unit.
     */
    fun applyFont(key: String) {
        // v389d — NO LAMBDA, AND SO NO NON-LOCAL RETURN. `focusedId ?: run { …
        // return }` makes the compiler emit its `$$$$$NON_LOCAL_RETURN$$$$$`
        // synthetic class, and R8 could not dex it ("Method name '<anonymous>'
        // in class '$$$$$NON_LOCAL_RETURN$$$$$' cannot be represented in dex
        // format") — which broke the RELEASE build while the debug build was
        // perfectly happy. A plain branch does the same thing without asking
        // the compiler for a synthetic class.
        val focused = focusedId
        if (focused == null) {
            armedFont = fontMaskFor(key)
            return
        }
        val id = focused
        val block = blocks[id] ?: return
        // v393 — THE FACE IS THE PARAGRAPH'S (user report: "the font chnage
        // should act for the whole paragraph not word based"). A block is the
        // page's unit of writing — a paragraph and its wrapped lines are one —
        // so the chosen face goes over every character of it in one sweep, the
        // same territory the align tool already treats as the page's own.
        if (block.text.isEmpty()) {
            // An empty line has nothing to set: the face arms, and the first
            // words typed arrive in it — the manner the title tool uses when
            // "Add chapter" opens a chapter name to be written into.
            armedFont = fontMaskFor(key)
            return
        }
        masks[id] = maskApplyFont(mask(id), 0, block.text.length, key)
        armedFont = null
        onDocChanged(doc())
    }

    /** The pen the caret sits in right now — what the dock's marker button
     *  lights from and names. "" for none. */
    fun highlightOfFocused(): String {
        // v393 — same rule as the face above: the standing pen lights the
        // marker the moment it is armed, not after the next keystroke.
        armedHighlight?.let { return highlightKeyOf(it) }
        val id = focusedId ?: return ""
        return highlightKeyOf(caretFlags(id))
    }

    /**
     * v389 — THE MARKER BUTTON.
     *
     * A selection is marked outright (and the selection is kept, so the writer
     * can see what they just did). With nothing selected the pen becomes an
     * INPUT STYLE — the next words typed wear it — except when the caret is
     * already in that exact pen, which takes it OFF, which is how a rich text
     * editor's highlighter toggle behaves.
     */
    fun applyHighlight(key: String) {
        // Same as applyFont above: the standing pen is set without a non-local
        // return, so the compiler never has to generate that synthetic class.
        val focused = focusedId
        if (focused == null) {
            armedHighlight = highlightMaskFor(key)
            return
        }
        val id = focused
        val block = blocks[id] ?: return
        val selection = selections[id]
        if (selection != null && !selection.collapsed) {
            val start = selection.min.coerceIn(0, block.text.length)
            val end = selection.max.coerceIn(0, block.text.length)
            // Tapping the pen the stretch already wears takes it off.
            val every = (start until end).all { highlightKeyOf(mask(id).getOrElse(it) { 0 }) == key }
            masks[id] = maskApplyHighlight(mask(id), start, end, if (every) null else key)
            armedHighlight = null
            onDocChanged(doc())
            return
        }
        val current = highlightOfFocused()
        armedHighlight = if (current == key) 0 else highlightMaskFor(key)
    }

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
        armedOff = 0
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

    /**
     * v389d — A PASTE WAITING TO BE CUT INTO LINES.
     *
     * Several lines pasted into a ROW arrive as ONE value with newlines inside
     * it, and the page's shape has to change to match (a row per line). That
     * shape change must not happen inside the keyboard's own edit batch — see
     * [onFieldChange] — so the words land first and this is what the canvas runs
     * on its next frame. Prose is never cut this way: a pasted paragraph keeps
     * its newlines and stays ONE field.
     */
    var pendingSplit by mutableStateOf<PersonalCaret?>(null)
        private set

    /** Runs the deferred cut, once the IME's batch is behind us. */
    fun runPendingSplit(caret: PersonalCaret) {
        pendingSplit = null
        splitOnNewlines(caret.blockId, caret.index)
        onDocChanged(doc())
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

    /**
     * v389 — REPLACE A LINE'S WORDS OUTRIGHT.
     *
     * The dock's text-history browser restores a whole version over whichever
     * line the caret was on. That is an edit like any other, so it goes through
     * [maskAfterEdit] rather than swapping the text in: a mask shorter than its
     * text paints the wrong characters on the tail of the line, and a restore
     * is exactly the case where the two lengths nearly always differ.
     */
    fun setBlockText(id: String, text: String) {
        val block = blocks[id] ?: return
        if (text == block.text) return
        masks[id] = maskAfterEdit(block.text, text, mask(id), 0, 0)
        blocks[id] = block.copy(text = text)
        selections[id] = TextRange(text.length)
        onDocChanged(doc())
    }

    fun mask(id: String): IntArray = masks[id] ?: emptyMask(text(id).length)

    fun photo(id: String): String? = blocks[id]?.photo

    fun caption(id: String): String = blocks[id]?.caption.orEmpty()

    fun align(id: String): PersonalAlign = blocks[id]?.align ?: PersonalAlign.START

    /** A checklist line's tick — stored with the block (v389). */
    fun checked(id: String): Boolean = blocks[id]?.checked == true

    /** v389 — an attached photo's size on the column (page-wide by default). */
    fun photoSize(id: String): PersonalPhotoSize =
        PersonalPhotoSize.fromKey(blocks[id]?.photoSize)

    /** v389 — how big that photo sits. The block is the unit, so this is a
     *  property of the block, saved with the rest of the page. */
    fun setPhotoSize(id: String, size: PersonalPhotoSize) {
        val block = blocks[id] ?: return
        if (block.isPhoto.not()) return
        blocks[id] = block.copy(photoSize = size.key)
        onDocChanged(doc())
    }

    /** The line's bullet marker (the default dot when it never picked one). */
    fun marker(id: String): PersonalMarker = blocks[id]?.markerStyle ?: PersonalMarker.DOT

    /**
     * v389d — THE CARET, FITTED TO THE TEXT IT IS ABOUT TO SIT IN.
     *
     * These ranges arrive from the IME (its selection, and its COMPOSING region
     * while a keyboard is mid-word) and were being handed straight back to
     * Compose by whatever the block's text happened to be a moment later — and a
     * block's text does not only change by typing: a tool rewrites a line, a
     * paste splits it into new lines, a merge puts two together, the page
     * reloads. When the text got SHORTER than a composing range, the next
     * `TextFieldValue` carried a region past the end of its own string, and the
     * IME's own batch-edit then fell over on it (crash report: 
     * `IndexOutOfBoundsException: toIndex (776) is greater than size (768)` out
     * of `endBatchEdit`, while deleting text in a book review — the 776 was the
     * composing region, the 768 the text it no longer fitted).
     *
     * So the two ranges are read FITTED: a composition that no longer fits is
     * gone (it is over — the words it belonged to are not there any more), and a
     * selection that no longer fits collapses to the end, which is where a caret
     * that was past the new end belongs.
     */
    fun selection(id: String): TextRange? {
        val length = blocks[id]?.text?.length ?: return null
        val range = selections[id] ?: return null
        return range.takeIf { it.min >= 0 && it.max <= length } ?: TextRange(length)
    }

    fun composition(id: String): TextRange? {
        val length = blocks[id]?.text?.length ?: return null
        val range = compositions[id] ?: return null
        return range.takeIf { it.min >= 0 && it.min <= it.max && it.max <= length }
    }

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
            // Typing ends a page-wide selection: the member is editing a row
            // again, and a wash over the whole page beside a live caret reads
            // as a bug.
            pageSelected = false
            // ── A ROW IS A LINE; A PARAGRAPH IS NOT (v389e) ────────────
            //
            // A newline inside a field used to become a BLOCK, so Enter and a
            // paste cut an entry into one field per line and the writing stopped
            // being connected: the platform's own Select all, its drag handles,
            // its cut and its replace-by-typing all stop at the edge of the
            // text field they started in (user report: "the problem is when i do
            // enter or paste something it creates a totally new text block and
            // for that reason the select all works only for that text block …
            // and this issue isnt on the save your take notes text blocks, the
            // enter works fine").
            //
            // The cut is now made only where a line really IS the page's unit: a
            // LIST line (a bullet or a checklist row) and the to-do page, whose
            // dot or box is drawn once at the row's own height and whose Enter
            // means "the next item". Everywhere else the newline stays in the
            // paragraph, so a journal entry is ONE field and the keyboard's own
            // selection, cut, undo and word movement behave like ordinary
            // writing — exactly as they already did in a save-your-take note.
            if (newText.indexOf('\n') >= 0 && lineStartsNewRow(id)) {
                // ── THE PASTE LANDS AS TEXT FIRST (v389d) ──────────────
                //
                // A pasted paragraph used to be cut into lines RIGHT HERE —
                // inside the IME's own commit/`endBatchEdit` batch. Whatever the
                // page did to its shape at that moment (a block removed, a
                // block added, the focus handed to a field that did not exist a
                // frame ago) was done underneath a keyboard still holding the
                // edit open, which is why a long paste could not be made to
                // stick at all (user report: "i wasn't able to paste long larger
                // paragraph"). The words are therefore accepted first — the
                // field's own value is never invalidated under the IME — and the
                // line-cutting is deferred to the next frame ([pendingSplit]).
                masks[id] = maskAfterEdit(
                    old.text, newText, mask(id), armed, armedOff, armedHighlight, armedFont
                )
                blocks[id] = old.copy(text = newText)
                if (armed != 0) armed = 0
                if (armedOff != 0) armedOff = 0
                if (armedHighlight != null) armedHighlight = null
                if (armedFont != null) armedFont = null
                selections[id] = value.selection
                compositions[id] = value.composition
                caret = PersonalCaret(id, value.selection.start.coerceIn(0, newText.length))
                pendingSplit = caret
                onDocChanged(doc())
                return
            }
            masks[id] = maskAfterEdit(
                old.text, newText, mask(id), armed, armedOff, armedHighlight, armedFont
            )
            blocks[id] = old.copy(text = newText)
            // A pending tool has now been used: what follows continues in the
            // style just typed, so the buttons stop being "pending".
            if (armed != 0) armed = 0
            if (armedOff != 0) armedOff = 0
            // The pen is consumed the same way — the words just typed wear it
            // and the pen after them is whatever they wear.
            if (armedHighlight != null) armedHighlight = null
            if (armedFont != null) armedFont = null
            // v393 — A TO-DO ROW KEEPS ITS BOX. The flag lived on the row's
            // CHARACTERS, so deleting every word deleted the checkbox with
            // them — and the next word typed came out plain, because there was
            // nothing left to inherit from (user report: "in todo the checkbox
            // deletes when i delete all the text after writing something").
            // A row emptied on the list page arms the box again: the box stands
            // while the row is empty, and the first word typed wears it.
            if (keepsChecklistRows && newText.isBlank()) {
                armed = armed or FLAG_CHECKBOX
                armedOff = armedOff and FLAG_CHECKBOX.inv()
            }
        }
        selections[id] = value.selection
        compositions[id] = value.composition
        onDocChanged(doc())
    }

    /**
     * v389d — THE LINE THE TOOLS ACT ON STAYS THE LINE.
     *
     * This used to forget the focused block the moment it lost focus — and a
     * block loses focus for all sorts of momentary reasons: a tap on a dock
     * button, the photo picker coming up, the eye/pen switch. Every tool that
     * falls back to "the focused line, else the first one" then quietly acted on
     * the FIRST LINE of the page instead, which is exactly what a broken align
     * button looks like (user report: "the left side format doesn't work in
     * journal and all" — it was setting the first line's alignment, not the one
     * being written). The last line the caret was in is remembered and used as
     * that fallback; the caret itself still moves where the member puts it.
     */
    fun onFocusChanged(id: String, focused: Boolean) {
        if (focused) focusedId = id
    }

    /** The flags of whatever the focused tool bar would act on right now —
     *  drives which buttons read as switched on. */
    /**
     * The style the CARET sits in: the character just left of it, else the one
     * just right (typing at the start of a styled run continues that run). This
     * is what a rich text editor lights its buttons from when nothing is
     * selected — what the next keystroke will wear.
     */
    private fun caretFlags(id: String): Int {
        val blockMask = mask(id)
        val at = (selections[id]?.start ?: blockMask.size).coerceIn(0, blockMask.size)
        return when {
            at - 1 in blockMask.indices && blockMask[at - 1] != 0 -> blockMask[at - 1]
            at in blockMask.indices && blockMask[at] != 0 -> blockMask[at]
            else -> 0
        }
    }

    /** The tools as the dock should light them right now. */
    fun activeFlags(): Int {
        val id = focusedId ?: return armed
        val selection = selections[id]
        if (selection == null || selection.collapsed) {
            // NO SELECTION: the buttons are an INPUT STYLE, exactly like a rich
            // text editor's — they report what the NEXT keypress wears, which is
            // the style the caret already sits in unless the member has switched
            // something on or off (v389). Before this, a tool lit only when the
            // WHOLE LINE carried it, so the dock described the line instead of
            // the typing, and tapping a button rewrote everything already
            // written.
            return (caretFlags(id) and armedOff.inv()) or armed
        }
        val range = selection.min to selection.max
        val blockMask = mask(id)
        var flags = 0
        ALL_FLAGS.forEach { flag ->
            if (maskCovers(blockMask, range.first, range.second, flag)) flags = flags or flag
        }
        return flags
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
        val listFlags = FLAG_BULLET or FLAG_CHECKBOX
        val other = if (flag == FLAG_BULLET) FLAG_CHECKBOX else FLAG_BULLET
        // A page-wide selection applies the tool to the ROWS, in one sweep —
        // all on or all off, the rule [toggle] follows for the same case.
        if (pageSelected) {
            val rows = order.filter { row -> blocks[row]?.let { !it.isPhoto && it.audio == null } == true }
            val allOn = rows.isNotEmpty() && rows.all { row ->
                val block = blocks[row] ?: return@all false
                maskCovers(mask(row), 0, block.text.length, flag)
            }
            rows.forEach { row ->
                val block = blocks[row] ?: return@forEach
                var updated = maskApply(mask(row), 0, block.text.length, flag, !allOn)
                if (!allOn) updated = maskApply(updated, 0, block.text.length, other, false)
                masks[row] = updated
            }
            armed = armed and listFlags.inv()
            onDocChanged(doc())
            return
        }
        val selection = selections[id]
        if (selection != null && !selection.collapsed) {
            val chosen = mask(id)
            val current = maskCovers(chosen, selection.min, selection.max, flag)
            var updated = maskApply(chosen, selection.min, selection.max, flag, !current)
            if (!current) updated = maskApply(updated, selection.min, selection.max, other, false)
            masks[id] = updated
            armed = armed and listFlags.inv()
            onDocChanged(doc())
            return
        }
        // v389e — NOTHING SELECTED: THE LIST DRESSES THE LINE UNDER THE CARET,
        // and that line goes on its own block first. A list item IS a row — its
        // dot or its box is drawn once, at the row's own height — so isolating
        // the line is also what makes the next Enter mean "another item"
        // instead of "another line of this paragraph".
        val target = isolateCaretLine(id)
        val lineText = text(target)
        if (lineText.isEmpty()) {
            armed = if (armed and flag != 0) armed and flag.inv()
            else (armed and listFlags.inv()) or flag
        } else {
            val lineMask = mask(target)
            val current = maskCovers(lineMask, 0, lineText.length, flag)
            var updated = maskApply(lineMask, 0, lineText.length, flag, !current)
            if (!current) updated = maskApply(updated, 0, lineText.length, other, false)
            masks[target] = updated
            armed = armed and listFlags.inv()
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
        val focused = focusedId ?: order.firstOrNull() ?: return
        val listFlags = FLAG_BULLET or FLAG_CHECKBOX
        // v389e — a marker dresses a LINE, so a plain tap gives the line the
        // caret is on the bullet and its marker (the line goes on its own block
        // first, see [isolateCaretLine]; a chosen RANGE still means exactly the
        // words the member chose).
        val selection = selections[focused]
        val id = if (selection != null && !selection.collapsed) focused else isolateCaretLine(focused)
        val block = blocks[id] ?: return
        val text = block.text
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

    /**
     * v389e — TRUE WHEN THE PAGE'S WRITING IS ONE FIELD.
     *
     * The ordinary journal entry is one field now (Enter writes a newline into
     * the paragraph), and on a one-field page the platform's OWN Select all
     * already means the whole entry — with real handles, real cut and real
     * replace-by-typing. The page-wide wash is only worth taking over for a page
     * that really is several fields (photos, voice notes, list rows), so both
     * doors to "select all" ask this first.
     */
    fun pageIsOneField(): Boolean = order.count { id ->
        blocks[id]?.let { !it.isPhoto && it.audio == null } == true
    } == 1

    /** The focused line's marker — the dock's bullet button wears it. */
    fun markerOfFocused(): PersonalMarker {
        val id = focusedId ?: order.firstOrNull() ?: return PersonalMarker.DOT
        return marker(id)
    }

    /**
     * v389 — THE WHOLE PAGE, SELECTED.
     *
     * A journal page is MANY text fields (one per paragraph), so the platform's
     * own "Select all" could only ever reach the line the caret happened to sit
     * in — exactly what a member saw ("when i do select all it only selects one
     * line … its same for all journals book review chapter review and all"). A
     * page-wide selection is therefore the PAGE's own state rather than a range
     * inside one field: every row wears the selection wash, and the dock's tools
     * then apply to all of them, which is what selecting a page and pressing
     * Bold is supposed to do.
     */
    var pageSelected by mutableStateOf(false)
        private set

    fun selectPage() {
        if (order.isEmpty()) return
        pageSelected = true
        // The caret lands at the END of the page, so the keyboard keeps
        // inserting where the writing left off if the member carries on.
        order.lastOrNull { id -> blocks[id]?.let { !it.isPhoto && it.audio == null } == true }
            ?.let { id ->
                focusedId = id
                caret = PersonalCaret(id, text(id).length)
            }
    }

    fun clearPageSelection() {
        pageSelected = false
    }

    /** Every row's words, top to bottom — what Copy puts on the clipboard. */
    fun pageText(): String = order
        .mapNotNull { blocks[it] }
        .filter { !it.isPhoto && it.audio == null }
        .joinToString("\n") { it.text }
        .trimEnd()

    fun toggle(flag: Int) {
        if (pageSelected) {
            // A page-wide selection means the tool applies to the ROWS, not to
            // a range inside one of them: the flag goes on for every row it is
            // missing from, and off for every row that already has it (the same
            // "toggle the whole thing" rule the dock's buttons follow).
            val rows = order.filter { id -> blocks[id]?.let { !it.isPhoto && it.audio == null } == true }
            val allOn = rows.isNotEmpty() && rows.all { id ->
                val block = blocks[id] ?: return@all false
                maskCovers(mask(id), 0, block.text.length, flag)
            }
            rows.forEach { id ->
                val block = blocks[id] ?: return@forEach
                masks[id] = maskApply(mask(id), 0, block.text.length, flag, !allOn)
            }
            armed = armed and flag.inv()
            onDocChanged(doc())
            return
        }
        val id = focusedId ?: return
        val selection = selections[id]
        // ── A LINE TOOL POINTS AT THE LINE (v389e) ─────────────────────
        //
        // A HEADING (and a small note) is the LINE'S OWN: it describes the line
        // rather than the letters on it, so tapping it with nothing selected has
        // to set the line the caret is on — automatically, which is what the
        // member asked for ("for the title tool that particular line should
        // automatically get the title format when title is selected"). Before
        // this the tool only ARMED itself, so the line already written never
        // changed and the button read as one that does nothing.
        //
        // The line goes on its own block first: a heading is ONE line of the
        // page, and the block is the unit every view reads a heading from (the
        // read-only page's display serif, the book review's pinned chapter).
        if (flag == FLAG_TITLE || flag == FLAG_SMALL) {
            if (selection != null && !selection.collapsed) {
                // The words the member actually chose: exactly those, as ever.
                val chosen = mask(id)
                val on = !maskCovers(chosen, selection.min, selection.max, flag)
                masks[id] = maskApply(chosen, selection.min, selection.max, flag, on)
                armed = armed and flag.inv()
                armedOff = armedOff and flag.inv()
                onDocChanged(doc())
                return
            }
            val target = isolateCaretLine(id)
            val lineText = text(target)
            if (lineText.isEmpty()) {
                // An empty line has nothing to set, so the tool arms instead and
                // the first words typed arrive as the heading — which is how
                // "Add chapter" opens a chapter name to be written into.
                val on = ((caretFlags(target) and armedOff.inv()) or armed) and flag != 0
                if (!on) {
                    armed = armed or flag
                    armedOff = armedOff and flag.inv()
                } else {
                    armed = armed and flag.inv()
                    armedOff = armedOff or flag
                }
                onDocChanged(doc())
                return
            }
            val lineMask = mask(target)
            val on = !maskCovers(lineMask, 0, lineText.length, flag)
            masks[target] = maskApply(lineMask, 0, lineText.length, flag, on)
            armed = armed and flag.inv()
            armedOff = armedOff and flag.inv()
            onDocChanged(doc())
            return
        }
        val blockMask = mask(id)
        if (selection == null || selection.collapsed) {
            // NO SELECTION: the tool is an INPUT STYLE. Tapping it changes what
            // the NEXT keystroke wears — never the line already written, which
            // is the whole point of the user report above. Inside a bold phrase
            // that means turning bold OFF for what follows it; on a plain line
            // it means switching bold ON for the words still to come.
            val on = ((caretFlags(id) and armedOff.inv()) or armed) and flag != 0
            if (!on) {
                armed = armed or flag
                armedOff = armedOff and flag.inv()
            } else {
                armed = armed and flag.inv()
                armedOff = armedOff or flag
            }
            onDocChanged(doc())
            return
        }
        val range = selection.min to selection.max
        val on = !maskCovers(blockMask, range.first, range.second, flag)
        masks[id] = maskApply(blockMask, range.first, range.second, flag, on)
        armed = armed and flag.inv()
        armedOff = armedOff and flag.inv()
        onDocChanged(doc())
    }

    /**
     * v389e — ALIGNMENT IS THE PAGE'S.
     *
     * The member's own rule: "for the align it should work for the whole page
     * not just paragraph". It has to be the page's anyway — a text field has ONE
     * alignment, so a block of several lines cannot hold a centred line above a
     * left-aligned one, and pretending otherwise is what made the tool look
     * broken (it used to set the FIRST line's alignment rather than the one
     * being written). The tool now sets every line the page has when it is
     * tapped. A line typed afterwards joins the block it is written in, so it
     * keeps that block's alignment; a NEW block starts at the left, which is the
     * member's own answer for what comes next ("lines you add afterwards start
     * at the left again").
     */
    fun setAlign(align: PersonalAlign) {
        order.forEach { id ->
            val block = blocks[id] ?: return@forEach
            blocks[id] = block.copy(align = align)
        }
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
        // v389d — removeAt shifts every index past the hole, so the target
        // for a downward move has to be adjusted: [to] was counted against
        // the ORIGINAL list, but the list is one shorter now. Without this,
        // a row dragged one step down always landed two positions away (user
        // report: "the todo rearrange works but also sometimes buggy" —
        // "other items shuffle wrongly").
        val adjustedTo = if (from < to) to - 1 else to
        order.add(adjustedTo, id)
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
        // Every bit to start with, then AND each visible character's own bits
        // into it: what survives covers the whole line ([ALL_FLAGS] is the
        // toolbar's ARRAY, so the mask has its own name — ALL_FLAGS_MASK).
        var flags = ALL_FLAGS_MASK
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
     * v389e — THE LINE THE CARET IS ON, inside one block's text.
     *
     * A block is a PARAGRAPH again (Enter writes a newline into it), so a line
     * is an offset pair inside the text rather than a block of its own. This is
     * the range a line-level tool points at: from the newline before the caret
     * to the newline after it. An EMPTY line answers an empty range.
     */
    private fun lineRange(text: String, caret: Int): IntRange {
        val at = caret.coerceIn(0, text.length)
        var start = at
        while (start > 0 && text[start - 1] != '\n') start--
        var end = at
        while (end < text.length && text[end] != '\n') end++
        return start until end
    }

    /** Where the caret sits inside [id]'s text (its end when nothing is
     *  selected). */
    private fun caretIn(id: String): Int {
        val length = blocks[id]?.text?.length ?: return 0
        return (selections[id]?.start ?: length).coerceIn(0, length)
    }

    /**
     * v389e — DOES ENTER START A NEW ROW HERE?
     *
     * True on the to-do page (a row per line is what that page IS) and on a
     * line that carries one of [LINE_FLAGS] — a bullet item, a checklist row, a
     * heading, a small note — because one of those is a LINE of the page rather
     * than prose: its furniture is drawn once, at the row's own height, and a
     * heading is one line by definition. An EMPTY line with a list tool armed on
     * it counts too, so Enter under a just-made bullet gives the next item.
     *
     * False for ordinary prose, which is the point: Enter then writes a newline
     * into the paragraph instead of cutting the entry into another field.
     */
    fun lineStartsNewRow(id: String): Boolean {
        if (keepsChecklistRows) return true
        val block = blocks[id] ?: return false
        if (block.isPhoto || block.isAudio) return false
        val text = block.text
        val range = lineRange(text, caretIn(id))
        if (range.isEmpty()) return armed and LINE_FLAGS_MASK != 0
        val lineMask = mask(id)
        return LINE_FLAGS.any { flag -> lineCarries(text, lineMask, range, flag) }
    }

    /**
     * True when every VISIBLE character of one line carries [flag].
     *
     * The same rule [personalBlockCarries] uses for a whole block (whitespace is
     * not part of a line's style, and an empty line carries nothing), so the
     * question "is this line a heading / a bullet item?" is answered the same way
     * here as it is where the line is drawn.
     */
    private fun lineCarries(text: String, mask: IntArray, range: IntRange, flag: Int): Boolean {
        var seen = false
        for (i in range.first..range.last) {
            val character = text.getOrNull(i) ?: break
            if (character.isWhitespace()) continue
            if (mask.getOrElse(i) { 0 } and flag == 0) return false
            seen = true
        }
        return seen
    }

    /**
     * v389e — ONE LINE, ON ITS OWN.
     *
     * A prose block holds a whole paragraph now, so a tool that describes a LINE
     * (a heading, a small note, a bullet item, a checklist row) has to be able
     * to point at ONE line of it. This cuts the block so the caret's line is a
     * block of its own: what came before it and what comes after it keep their
     * own blocks, the newlines at the two seams belong to neither side (they
     * were only ever the break), and the page's ORDER is untouched — the three
     * pieces stand exactly where the one block stood. The caret keeps its place
     * INSIDE the line, so setting a heading from the dock never moves the
     * writing.
     *
     * A line that already sits alone — a one-line block, the to-do page's rows,
     * a photo or a voice note — comes back as it was.
     */
    private fun isolateCaretLine(id: String): String {
        val block = blocks[id] ?: return id
        if (block.isPhoto || block.isAudio) return id
        val text = block.text
        val caretIndex = caretIn(id)
        val range = lineRange(text, caretIndex)
        if (range.isEmpty()) return id
        if (range.first == 0 && range.last == text.length - 1) return id
        val offsetInLine = caretIndex - range.first
        // WHAT COMES AFTER THE LINE GOES FIRST, so the block left behind still
        // holds the line at its head and every offset computed below is still
        // the one the member's caret came in with.
        val afterStart = range.last + 2
        if (afterStart < text.length) {
            val afterId = splitBlock(id, afterStart, mask(id))
            if (afterId.isBlank()) return id
            dropBreakCharacter(id)
            blocks[afterId] = blocks[afterId]?.copy(align = block.align) ?: return id
        } else if (range.last < text.length - 1) {
            // Nothing but the break follows the line: take it off this block.
            dropBreakCharacter(id)
        }
        if (range.first > 0) {
            val lineId = splitBlock(id, range.first, mask(id))
            if (lineId.isBlank()) return id
            dropBreakCharacter(id)
            blocks[lineId] = blocks[lineId]?.copy(align = block.align) ?: return id
            selections[lineId] = TextRange(offsetInLine)
            focusedId = lineId
            caret = PersonalCaret(lineId, offsetInLine)
            onDocChanged(doc())
            return lineId
        }
        // The line already starts the block, so what is left of it IS the line.
        selections[id] = TextRange(offsetInLine)
        focusedId = id
        caret = PersonalCaret(id, offsetInLine)
        onDocChanged(doc())
        return id
    }

    /**
     * v389e — ENTER ON A ROW: the line splits at the caret and the caret lands
     * at the start of the new one. Only a line that really IS the page's unit
     * gets here (see [lineStartsNewRow]) — prose keeps its newline inside the
     * paragraph instead, so a journal entry stays ONE text field.
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
        splitBlock(id, selections[id]?.start ?: block.text.length, mask(id))
    }

    /**
     * v389 — THE ONE SPLIT. Enter, a held-down Enter, and a pasted paragraph all
     * end up here: the text splits at [at], the line's own whole-line tools
     * cross the break, and the new block's id comes back so a caller splitting
     * in a loop can carry on with the remainder.
     *
     * [maskBefore] is the mask of the text being split — passed in rather than
     * read here, because a paste splits a paragraph that does not exist in the
     * page yet.
     */
    private fun splitBlock(id: String, at: Int, maskBefore: IntArray): String {
        val block = blocks[id] ?: return ""
        val caretIndex = at.coerceIn(0, block.text.length)
        val index = order.indexOf(id)
        if (index < 0) return ""
        // What the line is wearing decides what the new one inherits; an armed
        // tool (an empty line) inherits itself. A TITLE never crosses the break
        // (v389): a title is ONE line, so Enter at the end of a title starts
        // prose — which is exactly what the title button has to mean for the
        // member to be able to write a body under it (user request: "for the
        // title format in tool bar it should not work if we use enter to go to
        // a new line i mean it should auto select just the title format").
        val headFlags = lineFlags(block.text, maskBefore) and FLAG_TITLE.inv()
        val carried = when {
            // The TO-DO page's own manner, kept: Enter on an EMPTY row ends the
            // list instead of arming the next row for ever (a page of checklists
            // has to stop somewhere).
            keepsChecklistRows && block.text.isEmpty() -> 0
            headFlags != 0 -> headFlags
            else -> armed and FLAG_TITLE.inv()
        }
        val before = maskBefore.copyOfRange(0, caretIndex)
        val afterText = block.text.drop(caretIndex)
        // A whole-line tool is a WHOLE-LINE tool on both sides of the break; a
        // partly-styled line just keeps its own characters' styles.
        val after = if (headFlags != 0 && afterText.isNotEmpty()) {
            // v389 — the wholesale re-stamp is about the FLAGS; the pen each
            // character was written with is its own and rides across untouched,
            // or a marked line would come out of an Enter with its marker gone.
            IntArray(afterText.length) { i ->
                headFlags or
                    (maskBefore.getOrElse(caretIndex + i) { 0 } and (HIGHLIGHT_BITS or FONT_BITS))
            }
        } else {
            maskBefore.copyOfRange(caretIndex, block.text.length)
        }
        val tailMask = if (afterText.isEmpty()) after
        else IntArray(after.size) { after[it] and FLAG_TITLE.inv() }
        val head = block.copy(text = block.text.take(caretIndex), runs = maskToRuns(before))
        val tail = PersonalBlock(
            id = newBlockId(),
            text = afterText,
            runs = maskToRuns(tailMask),
            align = if (afterText.isEmpty()) block.align else PersonalAlign.START
        )
        blocks[id] = head
        masks[id] = runsToMask(head.text.length, head.runs)
        order.add(index + 1, tail.id)
        blocks[tail.id] = tail
        masks[tail.id] = runsToMask(tail.text.length, tail.runs)
        selections[tail.id] = TextRange(0)
        focusedId = tail.id
        caret = PersonalCaret(tail.id, 0)
        armed = carried
        armedOff = 0
        onDocChanged(doc())
        return tail.id
    }

    /**
     * v389 — A FIELD'S NEWLINES BECOME THE PAGE'S OWN LINES.
     *
     * Runs [splitBlock] once per newline, so a pasted paragraph and a held-down
     * Enter arrive at the same result: a block per line, each wearing the style
     * its own characters had. The caret then lands where the writer's cursor
     * actually was — in whichever line of the paste it belongs to — so carrying
     * on typing does what the member expects.
     *
     * v389d — THE BREAK IS THE NEWLINE, NOT A CHARACTER OF A LINE. The split
     * used to be made AT the newline, which left that character at the front of
     * the tail block — so the next pass found a newline at offset 0 and split
     * again, and every pasted paragraph came in with a phantom EMPTY line for
     * each real one (user report: "why does it create like a separate line i mean
     * enter should behave like enter but it creates some disconnected line"). The
     * split is now made AFTER the break and the break is taken off the head, so a
     * paste of N lines is N lines. The loop's own guard is generous on purpose: a
     * long paste is exactly the case this exists for.
     */
    private fun splitOnNewlines(id: String, caretAt: Int) {
        val block = blocks[id] ?: return
        if (block.text.indexOf('\n') < 0) return
        val at = caretAt.coerceIn(0, block.text.length)
        // Where each line begins: what the caret's own line and the offset
        // inside it are read from at the end.
        val lineStarts = block.text.indices.filter { block.text[it] == '\n' }.map { it + 1 }
        val lineIds = ArrayList<String>()
        lineIds.add(id)
        var remaining = block
        var guard = 0
        while (guard++ < 500) {
            val breakAt = remaining.text.indexOf('\n')
            if (breakAt < 0) break
            val tailId = splitBlock(remaining.id, breakAt + 1, mask(remaining.id))
            if (tailId.isBlank()) break
            dropBreakCharacter(remaining.id)
            val tail = blocks[tailId] ?: break
            lineIds.add(tailId)
            remaining = tail
        }
        // Where the writer's cursor was: the line it falls in, at the offset it
        // was at within that line.
        val line = lineStarts.count { it <= at }.coerceIn(0, (lineIds.size - 1).coerceAtLeast(0))
        val lineId = lineIds.getOrNull(line) ?: return
        val from = if (line == 0) 0 else lineStarts[line - 1]
        val into = (at - from).coerceIn(0, text(lineId).length)
        focusedId = lineId
        caret = PersonalCaret(lineId, into)
    }

    /**
     * The newline a break is made on belongs to neither line — take it off the
     * head, words and mask together, so the two stay the same length.
     */
    private fun dropBreakCharacter(id: String) {
        val block = blocks[id] ?: return
        if (!block.text.endsWith("\n")) return
        val shorter = block.text.dropLast(1)
        val trimmed = mask(id).copyOf(shorter.length)
        masks[id] = trimmed
        blocks[id] = block.copy(text = shorter, runs = maskToRuns(trimmed))
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
     * v389 — "ADD CHAPTER": a marker lands on the page as its own TITLE line.
     *
     * With no [label] the line arrives EMPTY and ARMED as a title, so the
     * chapter's name arrives as the heading it is (the same mechanism the dock's
     * title button uses). With a [label] — the chapter the member PICKED out of
     * the book's own chapter list — the line arrives already written and already
     * a title, because re-typing a name the book already knows is an errand (user
     * request: "in book review add chapter it should give option to add which
     * chapter from the fetched or catalog chapter names or number").
     *
     * It is an ordinary block in the ordinary order — which is exactly what
     * lets a book's whole review stay ONE page: the markers are prose, and the
     * read view folds a chapter's own review in under the marker that names it.
     */
    fun insertTitleLine(label: String = "") {
        val after = focusedId?.let { order.indexOf(it) }?.takeIf { it >= 0 }
            ?: order.indexOfLast { id -> !(blocks[id]?.isPhoto ?: false) }
        val at = if (after < 0) order.size else (after + 1).coerceAtMost(order.size)
        val text = label.trim()
        val block = if (text.isEmpty()) {
            PersonalBlock(id = newBlockId())
        } else {
            PersonalBlock(
                id = newBlockId(),
                text = text,
                runs = maskToRuns(IntArray(text.length) { FLAG_TITLE })
            )
        }
        order.add(at, block.id)
        blocks[block.id] = block
        masks[block.id] = runsToMask(text.length, block.runs)
        caret = PersonalCaret(block.id, text.length)
        focusedId = block.id
        // A picked chapter is already written, so nothing is armed: the next
        // thing typed is prose under the heading.
        armed = if (text.isEmpty()) FLAG_TITLE else 0
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
    enabled: Boolean = true,
    /**
     * v389 — WHERE THE PAGE'S OWN TITLE LINES SIT (the book review's pinned
     * chapter). A TITLE line is a chapter marker there, and a page that has
     * scrolled past one should be able to say which chapter the words below it
     * belong to — see BookReviewScreen. Reported as the line's own y WITHIN the
     * scrolling content, so the caller can subtract its scroll offset (a
     * position in the window would need the scroll to re-report itself).
     *
     * v389 — the line's BOTTOM is reported with its top, because "scrolled
     * past" means the whole line, not its first pixel: a pinned bar that lit up
     * the moment a heading touched the top edge named a chapter the member was
     * still reading the heading of (user report: "it shows that title at the
     * same position even though the title isnt scrolled awasy yet").
     */
    onTitlePosition: ((id: String, label: String, top: Float, bottom: Float) -> Unit)? = null
) {
    // v389d — THE DEFERRED PASTE. A pasted paragraph is accepted as TEXT inside
    // the keyboard's own edit batch and cut into the page's lines one frame
    // later, so the block list never changes shape while the IME still holds the
    // edit open (see PersonalEditorState.onFieldChange).
    val pendingSplit = state.pendingSplit
    LaunchedEffect(pendingSplit) {
        val waiting = pendingSplit ?: return@LaunchedEffect
        withFrameNanos { }
        state.runPendingSplit(waiting)
    }
    // The page's own reporter, or the one its host provided (see
    // [LocalPersonalTitleReport]).
    val titleReport = onTitlePosition ?: LocalPersonalTitleReport.current
    // v389 — one drag for the whole list: the to-do page's rows share it, so
    // the row under the finger and the rows it passes agree about one gesture.
    //
    // v389e — AND IT LIVES ON THE EDITOR, not on this canvas. The page that
    // HOSTS the canvas is the thing that owns the scroll, so a drag that needs
    // the page to follow it (the auto-scroll at the fold) has to be visible from
    // outside the writing surface. One state per page, reachable by both.
    val rowDrag = state.rowDrag
    val selectionWash = LocalTextSelectionColors.current.backgroundColor
    // v389 — SELECT ALL MEANS THE PAGE (see [PersonalEditorState.selectPage]).
    // The platform's toolbar keeps its own look and every one of its actions;
    // only what "Select all" DOES changes, and Copy is re-pointed with it so the
    // gesture carries through to the clipboard instead of copying one line.
    val clipboard = LocalClipboardManager.current
    val platformToolbar = LocalTextToolbar.current
    val pageToolbar = remember(platformToolbar, clipboard) {
        object : TextToolbar {
            override val status: TextToolbarStatus get() = platformToolbar.status

            override fun hide() = platformToolbar.hide()

            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                platformToolbar.showMenu(
                    rect = rect,
                    onCopyRequested = {
                        val whole = state.pageText()
                        if (state.pageSelected && whole.isNotBlank()) {
                            clipboard.setText(AnnotatedString(whole))
                        } else {
                            onCopyRequested?.invoke()
                        }
                    },
                    onPasteRequested = onPasteRequested,
                    onCutRequested = onCutRequested,
                    // v389e — a ONE-FIELD page keeps the platform's own Select
                    // all: the whole entry is that field, so the native
                    // selection (handles, cut, replace) is already what the
                    // words mean. The page wash is for the pages that really are
                    // several fields.
                    onSelectAllRequested = {
                        if (state.pageIsOneField()) {
                            onSelectAllRequested?.invoke()
                        } else {
                            state.selectPage()
                        }
                    }
                )
            }
        }
    }
    CompositionLocalProvider(LocalTextToolbar provides pageToolbar) {
    Column(
        modifier = modifier.clickable(enabled = enabled) { state.focusLastLine() },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // v389d — TWO SMALL OR HALF PHOTOS SHARE A ROW.
        //
        // Two consecutive non-PAGE photos that sit next to each other split
        // the text wrapper's width between them, like the "side by side" the
        // member asked for. The SECOND of each pair is flagged so the loop
        // skips it as a standalone block.
        //
        // v389d — NOT REMEMBERED, deliberately. Both answers are read straight
        // from the page every time it composes (a photo's size and the words
        // under it are exactly the things the member changes while looking at
        // them), which is also what keeps the two passes from disagreeing: a
        // remembered pair set flipped a resized print out of the page for good.
        val pairSkips = mutableSetOf<String>()
        val besideSkips = mutableSetOf<String>()
        run {
            val ids = state.blockIds
            var i = 0
            while (i < ids.size - 1) {
                val a = state.block(ids[i])
                val b = state.block(ids[i + 1])
                val paired = a?.isPhoto == true && b?.isPhoto == true &&
                    state.photoSize(ids[i]) != PersonalPhotoSize.PAGE &&
                    state.photoSize(ids[i + 1]) != PersonalPhotoSize.PAGE
                val beside = !paired && a?.isPhoto == true &&
                    state.photoSize(ids[i]) == PersonalPhotoSize.SMALL &&
                    b != null && !b.isPhoto && !b.isAudio &&
                    b.text.isNotBlank() && b.photo.isNullOrBlank()
                when {
                    paired -> {
                        pairSkips.add(ids[i + 1])
                        i += 2
                    }
                    beside -> {
                        besideSkips.add(ids[i + 1])
                        i += 2
                    }
                    else -> i += 1
                }
            }
        }
        // v389e — THE ROWS THAT DRAW, CHOSEN OUTSIDE THE COMPOSABLE LAMBDAS.
        //
        // A `return` out of a COMPOSABLE lambda is a non-local return, and the
        // compiler has only one way to implement one: throw. It emits its
        // `$$$$$NON_LOCAL_RETURN$$$$$` class for it, and R8 refuses to dex that
        // class's method name — "Method name '<anonymous>' in class
        // '$$$$$NON_LOCAL_RETURN$$$$$' cannot be represented in dex format" —
        // which killed the RELEASE build while debug builds were perfectly
        // happy. So nothing is stepped over INSIDE a composable lambda here:
        // the second photo of a pair (see [pairSkips]) and an id the page no
        // longer holds are left out of the list instead of returned past.
        val rows = state.blockIds.mapIndexedNotNull { index, id ->
            val block = state.block(id)
            if (block != null && !pairSkips.contains(id)) Triple(index, id, block) else null
        }
        rows.forEach { (index, id, block) ->
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

                // v389 — EVERY BLOCK SHIFT makes room when a voice note is
                // carried (see PersonalMovableBlock), so the gap says exactly
                // where it will land — the same "make room" the to-do rows
                // have, extended to the rest of the page.
                val isDragged = rowDrag.draggedId == id
                // v389 — a PHOTO is carried now too, and a carried block never
                // shifts on the outer layer: PersonalMovableBlock moves the one
                // under the finger itself, and shifting it here as well would
                // double its travel. The same exemption the voice note has.
                val ownCarry = block.isAudio || block.isPhoto
                val blockShift by animateFloatAsState(
                    targetValue = if (
                        isDragged || ownCarry || state.keepsChecklistRows
                    ) 0f else rowDrag.shiftFor(index, state.blockIds.lastIndex),
                    animationSpec = if (
                        !rowDrag.isDragging || isDragged ||
                            ownCarry || state.keepsChecklistRows
                    ) snap()
                    else spring(dampingRatio = 0.82f, stiffness = 700f),
                    label = "canvasBlockShift-$index"
                )
                // The DROP-LINE: a thin accent bar at the top of the block
                // that will be right after the drop, so the carried voice note
                // says where it is going to land.
                val targetIdx = rowDrag.targetIndex(state.blockIds.lastIndex)
                val showDropLine = rowDrag.isDragging && !isDragged &&
                    !block.isAudio && !state.keepsChecklistRows &&
                    index == targetIdx && rowDrag.fromIndex != targetIdx

                // All blocks share the same shift-and-drop-line wrapper.
                // Photo and text blocks slide to make room when a voice note
                // is carried; audio and checklist blocks handle their own
                // shift inside PersonalMovableBlock / PersonalTodoRow.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // v389e — EVERY ROW REPORTS ITS OWN HEIGHT, whether it can
                        // be carried or not. The order's arithmetic is measured
                        // against the rows the finger passes (see
                        // PersonalRowDragState.dragBy), and until now only the
                        // blocks that carry themselves (a print, a voice note, a
                        // to-do row) reported one — so on a journal page every
                        // paragraph was charged the carried note's own height,
                        // and the drop-line ran ahead of the finger. A row that is
                        // drawn is a row that is measured.
                        .onSizeChanged { measured ->
                            rowDrag.measure(id, measured.height.toFloat())
                        }
                        .graphicsLayer { translationY = blockShift }
                        .then(
                            if (showDropLine) Modifier.drawWithContent {
                                // A thin accent bar at the top of the target
                                // block — this is where the voice note will land.
                                // drawWithContent so it sits ON TOP of the text,
                                // not behind it.
                                drawContent()
                                drawLine(
                                    color = accent,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 2.dp.toPx()
                                )
                            } else Modifier
                        )
                ) {
                if (block.isPhoto) {
                    // v389d — SIDE-BY-SIDE PHOTOS.
                    //
                    // Two consecutive small or half photos share the row,
                    // splitting the text wrapper between them. The SECOND of
                    // each pair was flagged by [pairSkips] and never reached
                    // this block at all; the FIRST renders both photos in a
                    // Row. Two PAGE-size photos never pair — each is the width
                    // of the page.
                    val nextIndex = index + 1
                    val nextId = state.blockIds.getOrNull(nextIndex)
                    val nextBlock = nextId?.let { state.block(it) }
                    val isPaired = nextBlock?.isPhoto == true &&
                        state.photoSize(id) != PersonalPhotoSize.PAGE &&
                        state.photoSize(nextId) != PersonalPhotoSize.PAGE
                    if (isPaired && nextBlock != null && nextId != null) {
                        // The two photos share the text measure. Each keeps
                        // its own carry — a held photo lifts out of the pair
                        // and the other stays — and the Row's spacing keeps
                        // them from touching.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(Modifier.weight(1f)) {
                                PersonalMovableBlock(
                                    id = id, index = index, state = state,
                                    drag = rowDrag, enabled = enabled
                                ) {
                                    PersonalPhotoBlock(
                                        uri = block.photo.orEmpty(),
                                        caption = state.caption(id),
                                        size = state.photoSize(id),
                                        paired = true,
                                        ink = ink, accent = accent,
                                        enabled = enabled,
                                        onCaption = { state.setCaption(id, it) },
                                        onSize = { state.setPhotoSize(id, it) },
                                        onRemove = { state.removeBlock(id) },
                                        onOpen = { bounds -> onOpenPhoto(block.photo.orEmpty(), bounds) }
                                    )
                                }
                            }
                            Box(Modifier.weight(1f)) {
                                PersonalMovableBlock(
                                    id = nextId, index = nextIndex, state = state,
                                    drag = rowDrag, enabled = enabled
                                ) {
                                    PersonalPhotoBlock(
                                        uri = nextBlock.photo.orEmpty(),
                                        caption = state.caption(nextId),
                                        size = state.photoSize(nextId),
                                        paired = true,
                                        ink = ink, accent = accent,
                                        enabled = enabled,
                                        onCaption = { state.setCaption(nextId, it) },
                                        onSize = { state.setPhotoSize(nextId, it) },
                                        onRemove = { state.removeBlock(nextId) },
                                        onOpen = { bounds -> onOpenPhoto(nextBlock.photo.orEmpty(), bounds) }
                                    )
                                }
                            }
                        }
                    } else if (nextBlock != null && nextId != null &&
                        besideSkips.contains(nextId)
                    ) {
                        // v389d — THE PRINT BESIDE THE WRITING. The print keeps
                        // its own column, its own carry and its own size menu;
                        // the words sit next to it on the same baseline, with
                        // the row's own gap between them so a finger can still
                        // reach the print's corner to resize it.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(Modifier.weight(0.42f)) {
                                PersonalMovableBlock(
                                    id = id, index = index, state = state,
                                    drag = rowDrag, enabled = enabled
                                ) {
                                    PersonalPhotoBlock(
                                        uri = block.photo.orEmpty(),
                                        caption = state.caption(id),
                                        size = state.photoSize(id),
                                        // v389e — THE PRINT FILLS ITS OWN COLUMN.
                                        // A print takes a FRACTION of the measure it
                                        // is given (see [PersonalPhotoSize]), which
                                        // is right on the page and wrong here: the
                                        // column is already the narrow share of the
                                        // row, so a 44 % print inside a 42 % column
                                        // was a sliver of a picture — a squeezed
                                        // frame with a cropped image and a caption
                                        // with no room to be read (user report: "the
                                        // photo in journal and the text along side it
                                        // its kind of glitchy sometimes").
                                        paired = true,
                                        ink = ink, accent = accent,
                                        enabled = enabled,
                                        onCaption = { state.setCaption(id, it) },
                                        onSize = { state.setPhotoSize(id, it) },
                                        onRemove = { state.removeBlock(id) },
                                        onOpen = { bounds -> onOpenPhoto(block.photo.orEmpty(), bounds) }
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(0.58f)
                                    // The drag's own arithmetic counts a slot's
                                    // height, so the line beside the print has
                                    // to report the height it actually takes.
                                    .onSizeChanged { size ->
                                        if (size.height > 0) {
                                            rowDrag.measure(nextId, size.height.toFloat())
                                        }
                                    }
                            ) {
                                PersonalTextBlock(
                                    id = nextId,
                                    state = state,
                                    ink = ink,
                                    accent = accent,
                                    enabled = enabled,
                                    onTitlePosition = titleReport
                                )
                            }
                        }
                    } else {
                        // v389 — A PHOTO CAN BE CARRIED TOO (user request:
                        // "similiar to voive note reorder add for photo reorder
                        // too"). Exactly the voice note's manner: press and hold,
                        // then drag — the rows make room, a drop-line says where
                        // it lands, and the photo's own tap-to-open stands down
                        // while it is in the air (it reads
                        // LocalPersonalBlockCarried).
                        PersonalMovableBlock(
                            id = id, index = index, state = state,
                            drag = rowDrag, enabled = enabled
                        ) {
                            PersonalPhotoBlock(
                                uri = block.photo.orEmpty(),
                                caption = state.caption(id),
                                size = state.photoSize(id),
                                ink = ink, accent = accent,
                                enabled = enabled,
                                onCaption = { state.setCaption(id, it) },
                                onSize = { state.setPhotoSize(id, it) },
                                onRemove = { state.removeBlock(id) },
                                onOpen = { bounds -> onOpenPhoto(block.photo.orEmpty(), bounds) }
                            )
                        }
                    }
                } else if (block.isAudio) {
                    // v389 — a voice note in the page: the waveform is the block, and
                    // the writing carries on under it. It can also be CARRIED to
                    // another place on the page (user request: "add drag to move
                    // the voice note too"): press and hold it, then drag, exactly
                    // like a to-do row — a note belongs under the thought it is
                    // about, and where the recording happened is not that place.
                    PersonalMovableBlock(
                        id = id,
                        index = index,
                        state = state,
                        drag = rowDrag,
                        enabled = enabled
                    ) {
                        PersonalVoicePageBlock(
                            path = block.audio.orEmpty(),
                            seconds = block.audioSeconds,
                            bars = block.audioBars,
                            ink = ink,
                            accent = accent,
                            enabled = enabled,
                            onRemove = { state.removeBlock(id) }
                        )
                    }
                } else if (state.keepsChecklistRows) {
                    // A to-do page: every text row can be picked up (long press),
                    // carried to another place, and swiped sideways off the list.
                    PersonalTodoRow(
                        id = id,
                        index = index,
                        state = state,
                        drag = rowDrag,
                        enabled = enabled,
                        ink = ink
                    ) {
                        PersonalTextBlock(
                            id = id,
                            state = state,
                            ink = ink,
                            accent = accent,
                            enabled = enabled,
                            quoteJoinAbove = quoteAbove,
                            quoteJoinBelow = quoteBelow,
                            onTitlePosition = titleReport,
                            // A list's rows are the page: they wear its own size,
                            // so the pen and the eye read the same list.
                            rowSize = ROW_VIEW_SIZE
                        )
                    }
                } else if (besideSkips.contains(id)) {
                    // v389d — drawn inside the print above it (see [besideSkips]).
                } else {
                    PersonalTextBlock(
                        id = id,
                        state = state,
                        ink = ink,
                        accent = accent,
                        enabled = enabled,
                        quoteJoinAbove = quoteAbove,
                        quoteJoinBelow = quoteBelow,
                        onTitlePosition = titleReport,
                        selectionWash = if (state.pageSelected) selectionWash else Color.Transparent
                    )
                }
                }
            }
        }
    }
    }
}

@Composable
private fun PersonalTextBlock(
    id: String,
    state: PersonalEditorState,
    /** v389 — the page is SELECTED: this row wears the selection's own wash
     *  (transparent on every ordinary page, so nothing changes there). */
    selectionWash: Color = Color.Transparent,
    ink: Color,
    accent: Color,
    enabled: Boolean,
    /** The gap above/below holds another quoted line — see QUOTE_JOIN_EDITOR. */
    quoteJoinAbove: Boolean = false,
    quoteJoinBelow: Boolean = false,
    /** See [PersonalCanvas.onTitlePosition]. */
    onTitlePosition: ((id: String, label: String, top: Float, bottom: Float) -> Unit)? = null,
    /**
     * v389e — THE ROW SIZE OF A PAGE WHOSE ROWS ARE ITS CONTENT (a to-do list).
     *
     * The pen used to write a to-do row at the page's own body size while the
     * eye read it back at [ROW_VIEW_SIZE] — so a list changed size when it was
     * switched, and the box a thumb aims at was the prose-sized one on the side
     * that is actually used (user request: "make the todo list fonts overall page
     * font and checkboxsize fonts etc they should be larger"). Unspecified keeps
     * every other page exactly as it was.
     */
    rowSize: TextUnit = TextUnit.Unspecified
) {
    val text = state.text(id)
    val mask = state.mask(id)
    val align = state.align(id)
    val rowPage = rowSize.isSpecified
    val rowBody = if (rowPage) rowSize.value * 1.7f else 29f
    val rowMark = if (rowPage) ROW_MARKER_SIZE else PERSONAL_MARKER_SIZE
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
    // v393 — an EMPTY row on the list page still draws its box: the row's flag
    // lives on its characters, and an emptied row has none — but a to-do row
    // without a box reads as a row that lost its place in the list, not as a
    // row waiting to be written (user report: "the checkbox deletes when i
    // delete all the text").
    val isCheckbox = personalBlockCarries(text, mask, FLAG_CHECKBOX) ||
        (text.isEmpty() && state.keepsChecklistRows)
    val lineHeight = if (isTitle) 34.sp else if (isSmall) 22.sp else rowBody.sp
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
    // One place maps an alignment to the way text lays out, so a fourth kind
    // cannot be added in three of the four spots that draw a line.
    val alignOf = align.toTextAlign()
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
            fontSize = if (rowPage) rowSize else 17.sp,
            lineHeight = rowBody.sp,
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
            .then(
                // A TITLE line reports where it is, so a page whose head names
                // the chapter can follow the writing (see onTitlePosition).
                if (isTitle && onTitlePosition != null) {
                    Modifier.onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInParent()
                        onTitlePosition(id, text, bounds.top, bounds.bottom)
                    }
                } else Modifier
            )
            .then(
                if (selectionWash == Color.Transparent) Modifier
                else Modifier.drawBehind {
                    drawRoundRect(
                        color = selectionWash,
                        cornerRadius = CornerRadius(7.dp.toPx())
                    )
                }
            )
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
                                lineHeight = lineHeight.toPx(),
                                box = rowMark
                            )
                        }
                        .clickable(enabled = enabled) { state.setChecked(id, !checked) }
                        .padding(start = if (rowPage) ROW_MARKER_LEAD else PERSONAL_MARKER_LEAD)
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
                // ── A ROW ENDS; A PARAGRAPH CARRIES ON (v389e) ───────────
                // A checklist row, a bullet item and the to-do page still begin
                // a new row on Enter, because on those pages a row IS one line —
                // its dot or its box is drawn once, at the row's own height, and
                // Enter there means "the next item". Everywhere else Enter
                // writes a NEWLINE into the paragraph being written: the entry
                // stays ONE text field, so the platform's own Select all, its
                // drag handles and its cut reach the whole entry, which is the
                // fix for "the select all works only for that text block".
                if (
                    (event.key == Key.Enter || event.key == Key.NumPadEnter) &&
                    !event.isShiftPressed &&
                    state.lineStartsNewRow(id)
                ) {
                    state.splitAtCaret(id)
                    return@onPreviewKeyEvent true
                }
                // ── v389d — SELECT ALL MEANS THE PAGE ────────────────────
                // The keyboard's own Ctrl+A selects the text of the field the
                // caret is in, and a field here IS the page's writing — so a
                // page with photos, voice notes or list rows is several fields
                // and "select all" reached only one of them (user report: "i
                // can't even do select all as it only selects one line"). On
                // such a page the keyboard's Select all is taken over and the
                // page's own selection is used instead; on the ordinary
                // one-field page the platform keeps it, because there it IS the
                // whole entry, handles and all.
                if (event.isCtrlPressed && event.key == Key.A) {
                    if (!state.pageIsOneField()) {
                        state.selectPage()
                        return@onPreviewKeyEvent true
                    }
                }
                // BACKSPACE AT THE START OF A BLOCK takes the block back into
                // the one above it — the key beside Enter has to be able to undo
                // a boundary (a row that was split off, a heading that was set,
                // a photo that was dropped in). Inside a paragraph the field's
                // own backspace does the work: it deletes the newline and the two
                // lines join, which is exactly what a writer expects. The live
                // values are read here rather than captured, because this runs
                // between two compositions.
                if (event.key == Key.Backspace) {
                    val live = state.text(id)
                    val selection = state.selection(id)
                    val atBlockStart = live.isEmpty() ||
                        (selection != null && selection.collapsed && selection.start == 0)
                    if (atBlockStart) {
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

    // …and the PAGE-TAP, which is a separate request on purpose: the caret
    // above only fires when there is a caret to take. A tap on the blank space
    // under a page whose caret is ALREADY in this line used to change nothing,
    // because the state it would set was the state it had — so the keyboard
    // never came back. The tap token says a finger landed, and the line it named
    // takes the caret and the keyboard whether or not it already had them.
    val tapTick = state.tapTick
    // Read OUTSIDE the effect: a CompositionLocal cannot be reached from a
    // LaunchedEffect body (see the project's compile-safety rules).
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(tapTick, id) {
        if (tapTick > 0 && state.tapTarget == id) {
            focusRequester.requestFocus()
            keyboard?.show()
            state.onFocusChanged(id, true)
            state.consumeTap(id)
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

/**
 * v389 — HOW BIG A PRINT SITS ON THE COLUMN.
 *
 * [fraction] is of the writing WRAPPER's own width, never a fixed dp — that is
 * what keeps a photo inside the text's measure, so a caption or a paragraph can
 * never end up underneath it (user request: "make sure it follows the text
 * wrapper style so texts doesnt overlap"). PAGE is the default and is what every
 * photo placed before this existed already is.
 *
 * v390 — and PORTRAIT is the one for a photograph that stands UP: the frame is
 * taller than it is wide, which is the shape a phone picture of a person or a
 * doorway actually is (user request: "for the photos in journal and all add
 * portrait one too"). The other three are all wider than they are tall, so a
 * vertical picture had to be cropped to fit one of them.
 */
internal enum class PersonalPhotoSize(
    val key: String,
    val label: String,
    val fraction: Float
) {
    PAGE("page", "Page", 1f),
    HALF("half", "Half", 0.62f),
    PORTRAIT("portrait", "Portrait", 0.54f),
    SMALL("small", "Small", 0.44f);

    companion object {
        fun fromKey(key: String?): PersonalPhotoSize =
            entries.firstOrNull { it.key == key } ?: PAGE
    }
}

/**
 * A PHOTO AS A PRINT.
 *
 * It used to be a full-width card with a caption field under it, which read as
 * a box of picture with a form attached (user request: "for the photo preview on
 * page, mak eit polaroid style but make sure it follows the text wrapper style …
 * when i say polaroid not the whole polaroid phot but a smal lstyle kind of, and
 * support multiple photos and also sizes of polaroid"). A print is the shape
 * itself: the picture, a narrow border, and a wider bottom border with the
 * caption written in it — and it comes in the three sizes the dock's menu
 * offers, all of them a fraction of the text's own measure.
 */
@Composable
private fun PersonalPhotoBlock(
    uri: String,
    caption: String,
    size: PersonalPhotoSize,
    /**
     * v389d — SIDE BY SIDE. A paired photo fills its own half of a shared row,
     * rather than taking the full text measure at the size's usual fraction.
     */
    paired: Boolean = false,
    ink: Color,
    accent: Color,
    enabled: Boolean,
    onCaption: (String) -> Unit,
    onSize: (PersonalPhotoSize) -> Unit,
    onRemove: () -> Unit,
    onOpen: (Rect?) -> Unit
) {
    // The preview is deliberately SMALL (it is a note in a page, not a
    // gallery) and its bounds are what the page's overlay grows out of.
    var bounds by remember(uri) { mutableStateOf<Rect?>(null) }
    // v389 — while this photo is being CARRIED (see PersonalMovableBlock), its
    // own taps stand down: a finger that is dragging a photo is not asking to
    // open it, and the remove button must not be a thing that can be pressed
    // mid-flight. The block underneath is passive until it lands.
    val carried = LocalPersonalBlockCarried.current
    val actionable = enabled && !carried
    val sizeMenu = remember(uri) { CurioMenuToggle() }
    val imageHeight = when (size) {
        PersonalPhotoSize.PAGE -> 168.dp
        PersonalPhotoSize.HALF -> 128.dp
        // The tall one: a portrait print is a page-height picture on a narrow
        // column, so its frame stands up (see the sizes' own note).
        PersonalPhotoSize.PORTRAIT -> 232.dp
        PersonalPhotoSize.SMALL -> 100.dp
    }
    val captionSize = when (size) {
        PersonalPhotoSize.PAGE -> 13.sp
        PersonalPhotoSize.HALF -> 12.sp
        PersonalPhotoSize.PORTRAIT -> 12.sp
        PersonalPhotoSize.SMALL -> 10.sp
    }
    Column(
        modifier = Modifier
            // A FRACTION of the wrapper's width, so the print can never be
            // wider than the words it sits among.
            .fillMaxWidth(if (paired) 1f else size.fraction)
            // Shadow BEFORE the fill, and the fill OPAQUE — a translucent one
            // lets the shadow bleed through the print (see AGENTS rule 11).
            .shadow(5.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCurioDarkTheme()) Color(0xFF2B2723) else Color(0xFFFCF8F1))
            .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .onGloballyPositioned { bounds = it.boundsInWindow() }
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.06f))
                .clickable(enabled = actionable) { onOpen(bounds) }
        ) {
            PersonalPagePhoto(uri = uri, height = imageHeight)
            if (actionable) {
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
        // ── THE WIDE BOTTOM BORDER, with the caption written in it ────────
        // This is the whole shape of a print: the picture, then a band of paper
        // under it carrying what the picture is. Keeping the caption INSIDE the
        // frame is also what stops it becoming a line of text loose on the page.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = 0.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (enabled) {
                BasicTextField(
                    value = caption,
                    onValueChange = onCaption,
                    singleLine = true,
                    textStyle = TextStyle(
                        textAlign = TextAlign.Center,
                        fontFamily = WritingFontFamily,
                        fontSize = captionSize,
                        color = ink.copy(alpha = 0.72f)
                    ),
                    cursorBrush = SolidColor(personalAccentInk()),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 3.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (caption.isEmpty()) {
                                Text(
                                    "Add a caption",
                                    style = TextStyle(
                                        fontFamily = WritingFontFamily,
                                        fontSize = captionSize,
                                        color = ink.copy(alpha = 0.34f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            inner()
                        }
                    }
                )
            } else if (caption.isNotBlank()) {
                Text(
                    caption,
                    modifier = Modifier.weight(1f),
                    style = TextStyle(
                        fontFamily = WritingFontFamily,
                        fontSize = captionSize,
                        color = ink.copy(alpha = 0.72f)
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (actionable) {
                // The size the print sits at — in the frame's own border, where
                // a print's own mark would be.
                Box {
                    Surface(
                        onClick = { sizeMenu.buttonClick() },
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PrintSizeGlyph(ink.copy(alpha = 0.42f))
                        }
                    }
                    DropdownMenu(
                        expanded = sizeMenu,
                        onDismissRequest = { sizeMenu.dismissed() },
                        // v389h — a print's size menu sits over a page being
                        // written in; it must not close the keyboard the way the
                        // dock's menus used to (see MenuKeepKeyboardProperties).
                        properties = MenuKeepKeyboardProperties
                    ) {
                        PersonalPhotoSize.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = WritingFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = {
                                    if (option == size) {
                                        CurioIcon(
                                            CurioIcons.Check,
                                            null,
                                            tint = personalAccentInk(),
                                            size = 17.dp
                                        )
                                    }
                                },
                                onClick = {
                                    onSize(option)
                                    sizeMenu.close()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Two prints, one big and one small — the size tool's own mark. Drawn rather
 *  than taken from the icon subset, so it says "how big" at 16dp. */
@Composable
private fun PrintSizeGlyph(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
        val stroke = 1.4f.dp.toPx()
        val big = Size(size.width * 0.62f, size.height * 0.62f)
        val small = Size(size.width * 0.44f, size.height * 0.44f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, 0f),
            size = big,
            cornerRadius = CornerRadius(1.5f.dp.toPx()),
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width - small.width, size.height - small.height),
            size = small,
            cornerRadius = CornerRadius(1.5f.dp.toPx()),
            style = Stroke(width = stroke)
        )
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
    afterTitle: (@Composable (String) -> Unit)? = null,
    /**
     * v389 — ticks a checklist row FROM THE READ VIEW. Null means the page is
     * read-only here (a chapter review, a saved detail view): the box draws and
     * does not answer. Otherwise it falls back to [LocalPersonalCheckToggle],
     * which the writing page provides, so no page has to thread it down.
     */
    onToggleChecked: ((Int) -> Unit)? = null,
    /**
     * v389 — the row size for a page whose ROWS are the content (a to-do list),
     * where the writing's own body size reads too small to act on. Unspecified
     * keeps every other page exactly as it was.
     */
    rowSize: TextUnit = TextUnit.Unspecified,
    /** See [PersonalCanvas.onTitlePosition] — the read side of the same page. */
    onTitlePosition: ((id: String, label: String, top: Float, bottom: Float) -> Unit)? = null
) {
    // The page's own reporter, or the host's — see [LocalPersonalTitleReport].
    val titleReport = onTitlePosition ?: LocalPersonalTitleReport.current
    val quoteRule = personalQuoteRule()
    val quoteWash = personalQuoteWash()
    val quoteInk = personalQuoteColor().copy(alpha = 0.92f)
    val bulletInk = personalBulletColor()
    val toggle = onToggleChecked ?: LocalPersonalCheckToggle.current
    // v389g — the read view's double-tap-to-write action, read ONCE in the
    // composable scope (see the pointerInput below for why it cannot be read
    // inside the gesture).
    val tapToEdit = LocalPersonalTapToEdit.current
    // v389 — a quoted line is ONE thing with its quoted neighbour, so which of
    // the two sides leads into another quoted line is decided once, here, and
    // the panels reach into the gap to meet (see QUOTE_JOIN_VIEW).
    fun isQuoteRun(block: PersonalBlock): Boolean =
        !block.isPhoto && !block.isAudio && block.text.isNotBlank() &&
            personalBlockIsQuote(block.text, runsToMask(block.text.length, block.runs))

    // v389d — A SMALL PRINT KEEPS ROOM FOR THE WRITING, IN THE READ VIEW TOO.
    //
    // The editor does this (see PersonalCanvas); a page READ back has to look
    // like the page that was written (user request: "make the read view draw the
    // print with the writing beside it, the same as the editor"), so the same
    // rule runs here: a SMALL print takes a narrow column and the ONE line under
    // it moves in beside it. Two non-PAGE photos still pair as they did.
    //
    // v389g — and the pair is MARKED here, the way the editor marks it, so the
    // drawing pass draws one row of two instead of two lines of one.
    val besideSkips = mutableSetOf<String>()
    val pairSkips = mutableSetOf<String>()
    run {
        val blocks = doc.blocks
        var i = 0
        while (i < blocks.size - 1) {
            val photo = blocks[i]
            val next = blocks[i + 1]
            val paired = photo.isPhoto && next.isPhoto &&
                PersonalPhotoSize.fromKey(photo.photoSize) != PersonalPhotoSize.PAGE &&
                PersonalPhotoSize.fromKey(next.photoSize) != PersonalPhotoSize.PAGE
            val beside = !paired && photo.isPhoto &&
                PersonalPhotoSize.fromKey(photo.photoSize) == PersonalPhotoSize.SMALL &&
                !next.isPhoto && !next.isAudio && next.text.isNotBlank()
            when {
                // v389g — the right-hand print of a pair is MARKED here, so the
                // drawing pass knows it has already been drawn as half of the row
                // above it (the editor's pass does the same; the read view's used
                // to just step over it, which is why a pair came apart when the
                // page was read back).
                paired -> {
                    pairSkips.add(next.id)
                    i += 2
                }
                beside -> {
                    besideSkips.add(next.id)
                    i += 2
                }
                else -> i += 1
            }
        }
    }

    /**
     * ONE LINE, DRAWN — the read view's own renderer as a lambda, so the print's
     * pair can put the writing beside the picture with EXACTLY the rendering the
     * rest of the page gets: the same spans, markers, ticks, links, sizes,
     * alignment and chapter fold. Nothing about the drawing changes; it is the
     * same body of code, reachable from one more place.
     */
    val renderLine: @Composable (Int, PersonalBlock, Boolean, Boolean) -> Unit =
        { index, block, quoteAbove, quoteBelow ->
            val text = block.text
            val mask = runsToMask(text.length, block.runs)
            val isQuote = personalBlockIsQuote(text, mask)
            val isTitle = personalBlockCarries(text, mask, FLAG_TITLE)
            val isSmall = personalBlockCarries(text, mask, FLAG_SMALL)
            val isBullet = personalBlockCarries(text, mask, FLAG_BULLET)
            // v393 — the read view's half of the same rule: an EMPTY row on a
            // to-do page (the read view knows the page by its [rowSize]) still
            // draws its box, so a list re-opened after emptying a row looks
            // like the page that was written.
            val isCheckbox = personalBlockCarries(text, mask, FLAG_CHECKBOX) ||
                (text.isEmpty() && rowSize.isSpecified)
            // v389 — the same metrics and the same renderers as the editor
            // (this view draws a checklist row that the editor ticked).
            //
            // v389e — a ROW page ([rowSize]) puts its own size under the marker
            // too, so the eye's box is the pen's box (see ROW_MARKER_SIZE).
            val rowPage = rowSize.isSpecified
            val rowMark = if (rowPage) ROW_MARKER_SIZE else PERSONAL_MARKER_SIZE
            val rowLead = if (rowPage) ROW_MARKER_LEAD else PERSONAL_MARKER_LEAD
            val lineHeight = when {
                isTitle -> 31.sp
                isSmall -> 21.sp
                rowPage -> (rowSize.value * 1.7f).sp
                else -> 27.sp
            }
            val markerFill = personalAccentInk()
            val markerOnFill = MaterialTheme.colorScheme.surface
            val markerOutline = ink.copy(alpha = 0.42f)
            val alignOf = block.align.toTextAlign()
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
                                        lineHeight = lineHeight.toPx(),
                                        box = rowMark
                                    )
                                }
                                // The BOX is the target: a tap on the mark
                                // ticks the row, a tap on the words stays a
                                // read (this view has no editing of its
                                // own, so nothing else here answers a tap).
                                .then(
                                    if (toggle == null) {
                                        Modifier
                                    } else {
                                        Modifier.pointerInput(index, block.checked) {
                                            detectTapGestures { at ->
                                                if (at.x <= rowLead.toPx() &&
                                                    at.y <= lineHeight.toPx()
                                                ) {
                                                    toggle(index)
                                                }
                                            }
                                        }
                                    }
                                )
                                .padding(start = rowLead)
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
                    // A TITLE line is a chapter marker on the book review's
                    // page: it reports its own place so the head can say
                    // which chapter is being read (see onTitlePosition).
                    .then(
                        if (isTitle && titleReport != null) {
                            Modifier.onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInParent()
                                titleReport(block.id, text, bounds.top, bounds.bottom)
                            }
                        } else Modifier
                    )
            ) {
                val baseText = personalAnnotated(
                    text, mask, ink, quoteInk, QUOTE_VIEW_SIZE,
                    titleSize = if (isTitle) TextUnit.Unspecified else TITLE_VIEW_SIZE,
                    smallSize = if (isSmall) TextUnit.Unspecified else SMALL_VIEW_SIZE
                )
                // v392 — CLICKABLE LINKS: URLs in the read-only view
                // open in the browser with a coffee-dark underline so they
                // read as ink, not as the app's accent.
                val linkText = personalAnnotateLinks(baseText)
                var linkLayout by remember(linkText) {
                    mutableStateOf<TextLayoutResult?>(null)
                }
                val linkContext = LocalContext.current
                Text(
                    text = linkText,
                    onTextLayout = { linkLayout = it },
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
                            fontSize = if (rowSize.isSpecified) rowSize else 16.sp,
                            lineHeight = if (rowSize.isSpecified) rowSize * 1.7f else 27.sp,
                            textAlign = alignOf
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(linkText, tapToEdit) {
                            // v389g — the detector that owns the words also owns the
                            // double tap, so the pen answers over text exactly as it
                            // does over blank space (see [LocalPersonalTapToEdit]).
                            // The action is captured, NOT read here: a Composition
                            // local's `.current` is a @Composable read, and this block
                            // is an ordinary suspend lambda — the compiler rejects it.
                            // Reading it into a parameter above keeps the local's
                            // value fresh (the key restarts the detector when it
                            // changes) without a composable call in the gesture.
                            detectTapGestures(
                                onDoubleTap = { tapToEdit?.invoke() }
                            ) { offset ->
                                val layout = linkLayout ?: return@detectTapGestures
                                val pos = layout.getOffsetForPosition(offset)
                                linkText.getStringAnnotations(
                                    PERSONAL_LINK_TAG, pos, pos
                                ).firstOrNull()?.let { ann ->
                                    openSearchUrl(linkContext, ann.item)
                                }
                            }
                        }
                )
            }
            if (isTitle) afterTitle?.invoke(text)
        }

    /**
     * ONE PICTURE, DRAWN — the print the editor draws, at the width the caller
     * has room for: the page's own fraction when it stands alone, or the narrow
     * column beside the writing. The size chips, the paper frame, the caption in
     * its wide bottom border and the bounds the overlay grows out of are all
     * exactly as they were.
     */
    val renderPrint: @Composable (PersonalBlock, Modifier) -> Unit = { block, width ->
        var bounds by remember(block.photo) { mutableStateOf<Rect?>(null) }
        val printSize = PersonalPhotoSize.fromKey(block.photoSize)
        Column(
            modifier = width
                .onGloballyPositioned { bounds = it.boundsInWindow() }
                .shadow(5.dp, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(if (isCurioDarkTheme()) Color(0xFF2B2723) else Color(0xFFFCF8F1))
                .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 2.dp)
                .clickable { onOpenPhoto(block.photo.orEmpty(), bounds) }
        ) {
            PersonalPagePhoto(
                uri = block.photo.orEmpty(),
                height = when (printSize) {
                    PersonalPhotoSize.PAGE -> 168.dp
                    PersonalPhotoSize.HALF -> 128.dp
                    PersonalPhotoSize.PORTRAIT -> 232.dp
                    PersonalPhotoSize.SMALL -> 100.dp
                }
            )
            if (block.caption.isNotBlank()) {
                Text(
                    block.caption,
                    style = TextStyle(
                        fontFamily = WritingFontFamily,
                        fontSize = 13.sp,
                        color = ink.copy(alpha = 0.62f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp, bottom = 5.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        doc.blocks.forEachIndexed { index, block ->
            val quoteAbove = index > 0 && isQuoteRun(doc.blocks[index - 1])
            val quoteBelow = index < doc.blocks.lastIndex && isQuoteRun(doc.blocks[index + 1])
            if (block.isPhoto) {
                // A saved page shows the picture SMALL — it is a page of
                // writing, not a gallery — and hands its bounds to the
                // overlay so tapping it grows out of exactly here.
                //
                // v389 — AND IT SHOWS THE PRINT THE EDITOR DRAWS: the size the
                // member chose (a fraction of the text's own measure, so nothing
                // can ever land on top of it), the same paper frame, and the
                // caption in the frame's wide bottom border. Reading a page back
                // has to look like the page that was written (see
                // PersonalPhotoBlock, which owns the shape).
                // v389d — THE PRINT AND THE WRITING SIDE BY SIDE, as the editor
                // draws it: the print keeps a narrow column, the one line under
                // it takes the rest, and the pair is shown with the same gap the
                // editor leaves so a finger can still reach the print's frame.
                //
                // v389g — AND TWO PRINTS PAIR IN THE READ VIEW TOO.
                //
                // The editor pairs two non-PAGE photographs into one row of two
                // halves. The read view only knew about the print-with-writing
                // pair, so a page read back drew each photograph on its own line
                // at its own fraction — the pair came apart at exactly the moment
                // the member stopped writing (user report: "the photo staying side
                // by side in editing they stay but when i switch to view mode they
                // separate"). The pairing pass above now marks the right-hand print
                // of a pair, and this branch draws the row the editor draws, so the
                // two sides of the switch agree again.
                val nextPhoto = doc.blocks.getOrNull(index + 1)
                val nextPhotoId = nextPhoto?.id
                val pairPartner = nextPhoto?.takeIf {
                    nextPhotoId != null && pairSkips.contains(it.id)
                }
                val besideLine = nextPhoto?.takeIf {
                    nextPhotoId != null && besideSkips.contains(it.id)
                }
                when {
                    // The right half of a pair the print before it already drew.
                    pairSkips.contains(block.id) -> Unit
                    pairPartner != null -> Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(Modifier.weight(1f)) {
                            renderPrint(block, Modifier.fillMaxWidth())
                        }
                        Box(Modifier.weight(1f)) {
                            renderPrint(pairPartner, Modifier.fillMaxWidth())
                        }
                    }
                    besideLine != null -> Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(Modifier.weight(0.42f)) {
                            renderPrint(block, Modifier.fillMaxWidth())
                        }
                        Box(Modifier.weight(0.58f)) {
                            // A quote's panels reach into the gap above and below
                            // to meet their quoted neighbour, which is the column's
                            // business, not a pair's — the line beside a print
                            // draws with its own edges square.
                            renderLine(index + 1, besideLine, false, false)
                        }
                    }
                    else -> {
                        val printSize = PersonalPhotoSize.fromKey(block.photoSize)
                        renderPrint(block, Modifier.fillMaxWidth(printSize.fraction))
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
            } else if (besideSkips.contains(block.id)) {
                // v389d — this line is drawn INSIDE the print above it (see the
                // beside pair in the photo branch), so the column draws nothing
                // for it and the page does not say it twice.
            } else if (block.text.isNotBlank()) {
                renderLine(index, block, quoteAbove, quoteBelow)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Toolbar
// ────────────────────────────────────────────────────────────────────────────

/**
 * v389h — THE DOCK'S MENUS DO NOT STEAL THE KEYBOARD.
 *
 * A Material `DropdownMenu` opens a FOCUSABLE popup by default, and a focusable
 * popup takes the focus off the text field the writer was in — which closes the
 * keyboard. With the keyboard gone the dock folded away with it (the dock rides
 * the IME inset), so opening the font, pen or bullet menu read as the tools
 * vanishing mid-sentence (user report: "fix the issue of the tool bar hiding when
 * i select a tool drop down while editing … opening the drop down of a tool or
 * tapping it should not hide the keyboard").
 *
 * `focusable = false` leaves the window's focus where it was: the field keeps
 * the caret and the IME stays up. This is the same fix the rich-text editor's
 * own floating bar already shipped (see RichTextEditor's non-focusable popup),
 * now applied to the writing dock's three menus. The menus are read-and-pick
 * surfaces — nothing in them needs the keyboard — so a non-focusable popup costs
 * nothing here.
 */
private val MenuKeepKeyboardProperties = PopupProperties(focusable = false)



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
    // v389 — TEXT HISTORY, the dock's FIRST tool.
    //
    // The writing pages had it everywhere else in Curio but here, so the one
    // surface that holds the longest-lived writing in the app was the one with
    // no way back to an earlier draft (user request: "in journal bottom tool bar
    // add the text history option as the first"). It snapshots the LINE the
    // caret is on — a line is what a block is, so that is the unit the member
    // was actually editing — and a restore lands back on that same line.
    val historyContext = LocalContext.current
    var historyOpen by remember { mutableStateOf(false) }
    val historyLine = state.focusedId ?: state.blockIds.firstOrNull()
    val historyText = historyLine?.let { state.text(it) }.orEmpty()
    if (historyLine != null) {
        rememberTextHistoryCapture(
            ctx = historyContext,
            field = "Journal line",
            text = historyText,
            resetKey = historyLine
        )
    }
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
            // First in the dock, deliberately: it is the tool a member reaches
            // for when something has gone wrong with the writing, and that is
            // not a tool to go hunting for.
            PersonalToolButton(
                label = "Text history",
                active = false,
                accent = accentInk, ink = ink,
                onClick = { historyOpen = true }
            ) {
                CurioIcon(CurioIcons.History, null, size = 19.dp)
            }
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
            // v389 — THE FACE. Four voices the app already bundles, one tap
            // from a menu that is set IN each of them, because a font menu
            // written in one font is a list of words (user request: "the font
            // chnage … add in the universal tool bar").
            Box {
                val fontMenu = remember { CurioMenuToggle() }
                val face = state.fontOfFocused()
                PersonalToolButton(
                    label = "Font: ${personalFontLabel(face)}",
                    // Lit only when a face was actually CHOSEN: the page's own
                    // serif is not a setting, it is where a line starts.
                    active = face.isNotEmpty(),
                    accent = accentInk, ink = ink,
                    onClick = { fontMenu.buttonClick() }
                ) {
                    FontGlyph(face)
                }
                DropdownMenu(
                    expanded = fontMenu.open,
                    onDismissRequest = { fontMenu.dismissed() },
                    properties = MenuKeepKeyboardProperties
                ) {
                    PERSONAL_FONT_KEYS.forEach { key ->
                        DropdownMenuItem(
                            text = {
                                // Set in the face it offers — the preview and the
                                // result are the same bytes.
                                Text(
                                    personalFontLabel(key),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = personalFontPreview(key)
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingIcon = {
                                if (face == key) {
                                    CurioIcon(CurioIcons.Check, null, tint = accentInk, size = 18.dp)
                                }
                            },
                            onClick = {
                                state.applyFont(key)
                                fontMenu.close()
                            }
                        )
                    }
                }
            }
            // v389 — THE MARKER PEN, the first tool in the dock that is a
            // COLOUR. It follows the bullet tool's manner exactly, because that
            // is the manner this dock already taught the member: the first tap
            // puts a pen down (the first one — the common case, one tap), and
            // the tap after that opens the palette to change it or take it off
            // (user request: "watermark with color options", "the text maker
            // highlighter colr of the word"). The button WEARS the pen it is
            // about to use, so the dock says which colour before the tap does.
            Box {
                val penMenu = remember { CurioMenuToggle() }
                val pen = state.highlightOfFocused()
                val penOn = pen.isNotEmpty()
                PersonalToolButton(
                    label = if (penOn) "Marker: ${personalHighlightLabel(pen)}" else "Marker",
                    active = penOn,
                    // A lit marker wears its OWN ink rather than the theme's
                    // accent — the same reason the menu's swatches are the pens
                    // and not the theme: a colour tool that shows the accent
                    // shows the wrong colour.
                    accent = if (penOn) personalHighlightInk(pen) else accentInk,
                    ink = ink,
                    onClick = {
                        if (penOn) penMenu.buttonClick()
                        else state.applyHighlight(PERSONAL_HIGHLIGHT_KEYS.first())
                    }
                ) {
                    MarkerPenGlyph(pen = if (penOn) personalHighlightInk(pen) else null)
                }
                DropdownMenu(
                    expanded = penMenu.open,
                    onDismissRequest = { penMenu.dismissed() },
                    properties = MenuKeepKeyboardProperties
                ) {
                    DropdownMenuItem(
                        text = { MarkerMenuLabel("Remove marker") },
                        leadingIcon = {
                            CurioIcon(CurioIcons.Close, null, tint = ink, size = 18.dp)
                        },
                        trailingIcon = {
                            if (!penOn) CurioIcon(CurioIcons.Check, null, tint = accentInk, size = 18.dp)
                        },
                        onClick = {
                            // With nothing selected this ARMS the eraser: the
                            // words typed next come out unmarked, which is the
                            // only way to write plain text inside a marked
                            // sentence. With a selection it clears it outright.
                            state.applyHighlight("")
                            penMenu.close()
                        }
                    )
                    PERSONAL_HIGHLIGHT_KEYS.forEach { key ->
                        DropdownMenuItem(
                            text = { MarkerMenuLabel(personalHighlightLabel(key)) },
                            leadingIcon = { PenSwatch(personalHighlightInk(key)) },
                            trailingIcon = {
                                if (pen == key) {
                                    CurioIcon(CurioIcons.Check, null, tint = accentInk, size = 18.dp)
                                }
                            },
                            onClick = {
                                state.applyHighlight(key)
                                penMenu.close()
                            }
                        )
                    }
                }
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
                val markerMenu = remember { CurioMenuToggle() }
                val focusedMarker = state.markerOfFocused()
                val bulletOn = active and FLAG_BULLET != 0
                PersonalToolButton(
                    label = "Bullet style",
                    active = bulletOn,
                    accent = accentInk, ink = ink,
                    onClick = {
                        if (bulletOn) markerMenu.buttonClick()
                        else state.applyMarker(PersonalMarker.entries.first())
                    }
                ) {
                    MarkerGlyph(focusedMarker)
                }
                DropdownMenu(
                    expanded = markerMenu.open,
                    onDismissRequest = { markerMenu.dismissed() },
                    properties = MenuKeepKeyboardProperties
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
                            markerMenu.close()
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
                                markerMenu.close()
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
                AlignGlyph(AlignKind.START)
            }
            PersonalToolButton(
                label = "Align centre",
                active = state.alignOfFocused() == PersonalAlign.CENTER,
                accent = accentInk, ink = ink,
                onClick = { state.setAlign(PersonalAlign.CENTER) }
            ) {
                AlignGlyph(AlignKind.CENTER)
            }
            PersonalToolButton(
                label = "Align right",
                active = state.alignOfFocused() == PersonalAlign.END,
                accent = accentInk, ink = ink,
                onClick = { state.setAlign(PersonalAlign.END) }
            ) {
                AlignGlyph(AlignKind.END)
            }
            PersonalToolButton(
                label = "Justify",
                active = state.alignOfFocused() == PersonalAlign.JUSTIFY,
                accent = accentInk, ink = ink,
                onClick = { state.setAlign(PersonalAlign.JUSTIFY) }
            ) {
                AlignGlyph(AlignKind.JUSTIFY)
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
    // The browser rides OUTSIDE the surface: it is a sheet of its own, and
    // nesting it in the dock's rounded pill would clip it to the pill.
    if (historyOpen && historyLine != null) {
        TextHistoryBrowser(
            ctx = historyContext,
            activeField = "Journal line",
            currentText = historyText,
            onRestore = { restored, mode ->
                val merged = when (mode) {
                    TextHistoryRestoreMode.REPLACE -> restored
                    TextHistoryRestoreMode.ADD_TOP -> restored + "\n" + historyText
                    // The line is ONE line, so "add below" means the line after
                    // it — the second half is dropped in as its own block by the
                    // newline rule rather than pressed into this one.
                    TextHistoryRestoreMode.ADD_BOTTOM -> historyText + "\n" + restored
                }
                state.setBlockText(historyLine, merged)
            },
            onDismiss = { historyOpen = false }
        )
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
internal fun TodoGlyph(active: Boolean, iconSize: Dp = 19.dp) {
    val ink = LocalContentColor.current
    val onFill = MaterialTheme.colorScheme.surface
    // NB: the Canvas parameter must NOT be named `size` — it would shadow
    // DrawScope.size, which the geometry below reads (see AGENTS rule 7).
    androidx.compose.foundation.Canvas(modifier = Modifier.size(iconSize)) {
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
/**
 * THE MARKER PEN as the dock draws it: a nib over a wash.
 *
 * Drawn rather than taken from the icon subset, for the same reason the marker
 * and alignment glyphs are: the pen has to show the COLOUR it will lay down, and
 * a tinted bundled icon is not a pen.
 *
 * [pen] is the ink the next words will wear, or null when no pen is down — in
 * which case the wash is drawn in the content colour so the button still reads
 * as a highlighter among the other glyphs.
 */
@Composable
private fun MarkerPenGlyph(pen: Color?) {
    val ink = LocalContentColor.current
    androidx.compose.foundation.Canvas(modifier = Modifier.size(19.dp)) {
        val wash = pen ?: ink.copy(alpha = 0.55f)
        val tip = 1.9f.dp.toPx()
        // The wash: the band the pen leaves on the page.
        drawRoundRect(
            color = wash,
            topLeft = Offset(size.width * 0.12f, size.height * 0.60f),
            size = Size(size.width * 0.70f, size.height * 0.26f),
            cornerRadius = CornerRadius(size.height * 0.13f)
        )
        // The nib: a diagonal bar rising out of the wash, cut square so it
        // reads as a chisel tip rather than a pencil.
        val nib = Path().apply {
            moveTo(size.width * 0.30f, size.height * 0.55f)
            lineTo(size.width * 0.52f, size.height * 0.16f)
            lineTo(size.width * 0.74f, size.height * 0.28f)
            lineTo(size.width * 0.50f, size.height * 0.66f)
            close()
        }
        drawPath(path = nib, color = ink, style = Stroke(width = tip, join = StrokeJoin.Round))
    }
}

/** One pen in the palette: a rounded wash in the pen's own ink, so the menu is
 *  the four colours rather than four words that mean colours. */
@Composable
private fun PenSwatch(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        drawRoundRect(
            color = color.copy(alpha = 0.85f),
            topLeft = Offset(0f, size.height * 0.16f),
            size = Size(size.width, size.height * 0.68f),
            cornerRadius = CornerRadius(size.height * 0.30f)
        )
    }
}

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

/** Which of the four alignments a glyph draws. One enum rather than a
 *  `center: Boolean`, which could never say "right" let alone "justify". */
internal enum class AlignKind { START, CENTER, END, JUSTIFY }

/**
 * THE FONT BUTTON is a specimen: "Aa" drawn in the face the next words will be
 * set in, so the dock answers "which font?" before the menu is even opened.
 *
 * The sample is deliberately short — two characters, at label size. A preview
 * long enough to be readable is also long enough to change the dock's own
 * layout as the face changes, and a dock that twitches when a font is picked
 * reads as a bug rather than as an effect.
 */
@Composable
private fun FontGlyph(key: String) {
    Text(
        text = "Aa",
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = personalFontPreview(key),
            fontWeight = FontWeight.Medium
        ),
        color = LocalContentColor.current
    )
}

/**
 * The alignment tools draw their own glyph (four rules), so the dock never
 * depends on a font subset that has no align icons. The rule lengths are what
 * tell the four apart at 18dp: left is ragged right, right is ragged left,
 * centre is ragged both ends, and justified is four FULL rules — which is the
 * only one of the four that can be drawn flush on both edges without lying
 * about what it does.
 */
@Composable
private fun AlignGlyph(kind: AlignKind) {
    val ink = LocalContentColor.current
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.8f.dp.toPx()
        val width = size.width
        // The last rule is short on every ragged style; the ones above it are
        // full, which is the shape the eye reads as an alignment at a glance.
        val fractions = when (kind) {
            AlignKind.START -> listOf(1f, 0.68f, 1f, 0.5f)
            AlignKind.END -> listOf(1f, 0.68f, 1f, 0.5f)
            AlignKind.CENTER -> listOf(1f, 0.68f, 1f, 0.5f)
            AlignKind.JUSTIFY -> listOf(1f, 1f, 1f, 1f)
        }
        fractions.forEachIndexed { index, fraction ->
            val y = size.height * (0.22f + index * 0.19f)
            val lineWidth = width * fraction
            val x = when (kind) {
                AlignKind.START -> 0f
                AlignKind.CENTER -> (width - lineWidth) / 2f
                AlignKind.END -> width - lineWidth
                // A justified rule starts at the margin on two of its four
                // lines and is centred on the others — the way justified prose
                // reads: flush, flush, and a short last line in the middle.
                AlignKind.JUSTIFY -> if (index == 3) (width - lineWidth) / 2f else 0f
            }
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
