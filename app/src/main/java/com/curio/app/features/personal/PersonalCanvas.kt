package com.curio.app.features.personal

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.PathEffect
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
import androidx.compose.ui.layout.positionInRoot
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

/**
 * THE PAGE'S TYPE — ONE TABLE, BOTH SIDES OF THE SWITCH (v427).
 *
 * Every size a page is written or read at lives here as a SIZE and the LEADING
 * it is set on, because the two surfaces that draw the same words have to agree:
 * the PEN (the canvas' `BasicTextField`, which takes the *BODY numbers) and the
 * EYE (the read-only view, which takes the *VIEW ones). A word set larger on one
 * side than the other is a page that changes shape the moment the member stops
 * writing.
 *
 * They are `internal` for one more reader: the PDF export draws this same page
 * onto a sheet of paper and takes EVERY number from here — its body, its
 * headings, a quoted line, its small print, the leading under each and the air
 * between its rows — rather than carrying numbers of its own that could drift
 * (v427, user request: "line the PDF's type up with the journal view as well —
 * body size, leading and the caption face taken from the canvas instead of the
 * export's own numbers"). The one thing the export does NOT take from here is
 * HOW MUCH OF A PAGE one of these becomes — that is the sheet's own measure,
 * see `PDF_UNITS_PER_SP`.
 */
internal val BODY_BODY_SIZE = 17.sp
internal val BODY_BODY_LINE = 29.sp
internal val BODY_VIEW_SIZE = 16.sp
internal val BODY_VIEW_LINE = 27.sp

/** The quote size inside the canvas' 17sp body and the read-only 16sp body —
 *  a quoted line reads as a quotation, a touch smaller than the prose. */
private val QUOTE_BODY_SIZE = 16.sp
internal val QUOTE_VIEW_SIZE = 15.sp

/** A TITLE line and a SMALL line, in the editor and in the read-only view. */
private val TITLE_BODY_SIZE = 24.sp
private val TITLE_BODY_LINE = 34.sp
internal val TITLE_VIEW_SIZE = 22.sp
internal val TITLE_VIEW_LINE = 31.sp

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
private val SMALL_BODY_LINE = 22.sp
internal val SMALL_VIEW_SIZE = 12.5.sp
internal val SMALL_VIEW_LINE = 21.sp

/**
 * A PRINT'S CAPTION, READ BACK.
 *
 * The pen sizes a label by the frame it sits in (13sp under a page-wide print,
 * 10sp under a small one — see `PersonalPhotoBlock`), while the eye reads every
 * label at this one, and a label's own size setting (`PersonalCaptionLabelSize`)
 * multiplies whichever it is. This is the read-back number, and the one the PDF
 * export takes, because a sheet of paper is the page READ.
 */
internal val CAPTION_VIEW_SIZE = 13.sp

/**
 * THE AIR BETWEEN TWO ROWS OF A PAGE READ BACK.
 *
 * The pen leaves 6dp between the rows it writes (see `PersonalCanvas`), the eye
 * leaves this — and the PDF export, which draws the page read back, takes it as
 * its own paragraph gap, so a sheet's rhythm is the journal's rhythm.
 */
internal val VIEW_ROW_GAP = 8.dp

/**
 * THE QUOTE'S OWN FRAME — the rule down its side, and how far past the rule the
 * quoted words begin (the panel the editor draws and the panel the read view
 * draws are ONE drawing, and they ask for both numbers here).
 *
 * They are named for a third reader: the PDF export draws the same quote as a
 * leading margin, and at the sheet's measure a title-small indent would have
 * been a quotation pressed against its own rule.
 */
internal val QUOTE_RULE_WIDTH = 3.dp
internal val QUOTE_LEAD = 13.dp

/**
 * THE QUOTE'S OWN COLOUR — COFFEE, never the app's accent (user decision: a
 * quotation has to read as ink on paper, and an accent-tinted quote looked
 * like a highlight someone forgot to finish). The dark theme takes the milky
 * coffee twin, because the deep one would vanish into a dark page.
 *
 * v426 — THE TWIN WAS PROMISED BUT NEVER PAINTED. This returned the light
 * coffee unconditionally, so on the journal's dark paper (a near-black page) a
 * quoted passage was drawn at ~2.5:1 — the member's "in dark mode the texts
 * have black or dark color" in the journal. It wears [personalQuoteDeepColor]'s
 * milky tone at night now, which is the same coffee, one shade up, and clears
 * 4.5:1 on the paper it is written on.
 */
@Composable
internal fun personalQuoteColor(): Color =
    if (isCurioDarkTheme()) Color(0xFFC09263) else Color(0xFF9A6A43)

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
 * v428 — THE SAME FOUR, AS THE DOCK DRAWS THEM.
 *
 * The alignment tool's glyph ([AlignGlyph]) takes an [AlignKind], the model
 * takes a [PersonalAlign], and the dock now has ONE align button that has to
 * show the focused line's own alignment — so the two enums need one mapping, in
 * one place, rather than a `when` repeated at each of the four menu rows.
 */
internal fun PersonalAlign.toAlignKind(): AlignKind = when (this) {
    PersonalAlign.START -> AlignKind.START
    PersonalAlign.CENTER -> AlignKind.CENTER
    PersonalAlign.END -> AlignKind.END
    PersonalAlign.JUSTIFY -> AlignKind.JUSTIFY
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
/** [linkInk] is passed IN because this runs outside composition — the caller
 *  resolves the theme-aware coffee (see [personalQuoteDeepColor]): the deep
 *  twin by day, the milky one at night. v426 — it used to hard-code the deep
 *  coffee, which on the journal's dark paper drew every URL at ~1.6:1: the
 *  member's "in dark mode the texts have black or dark color" (a link is the
 *  one piece of text in the journal that is not the page's own ink). */
private fun personalAnnotateLinks(base: AnnotatedString, linkInk: Color): AnnotatedString {
    val text = base.text
    val matches = URL_REGEX.findAll(text).toList()
    if (matches.isEmpty()) return base
    val annotated = androidx.compose.ui.text.buildAnnotatedString {
        append(base)
        for (match in matches) {
            addStyle(
                SpanStyle(
                    color = linkInk,
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
 *
 * v427 — AND EVERY WORD WEARS THE PAGE'S INK, ROOTED IN THE TEXT ITSELF.
 *
 * Only a FLAGGED run was coloured here, so a plain word carried NO colour at
 * all: an unstyled span renders as `TextStyle.color` and, where the style sets
 * none either, as `LocalContentColor` — which outside a `Surface` is Compose's
 * own default, **`Color.Black`**. The writing page passes a colour of its own
 * (see the editor's `bodyStyle`), so the fault only ever showed on the READING
 * side, and only where the page is dark: the member's "still in journal eye view
 * the text writing have dark black texts". The base style is laid over the whole
 * string now, before the flags, so the annotated text is self-sufficient — no
 * consumer can inherit a foreign ink again, whatever it does or does not set.
 */
internal fun personalAnnotated(
    text: String,
    mask: IntArray,
    ink: Color,
    quoteInk: Color,
    quoteSize: TextUnit,
    titleSize: TextUnit = TextUnit.Unspecified,
    smallSize: TextUnit = TextUnit.Unspecified,
    /**
     * v427 — THE BAR'S LETTER REACH, WASHED UNDER THE WORDS.
     *
     * The page bar could say "12 of 68 letters" while the page showed nothing at
     * all, which is the member's "the select tools doesnt highlight whats
     * selecting". A window of characters is a span like any other here, so it is
     * drawn with the one mechanism this file already has for a wash under words
     * (a marker pen's own background), and it is added LAST so the letters the
     * member is picking are the ones they can see.
     */
    selectionChars: TextRange? = null,
    selectionWash: Color = Color.Unspecified
): AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    append(text)
    // The page's ink under everything; a flagged run overrides it below.
    if (text.isNotEmpty()) addStyle(SpanStyle(color = ink), 0, text.length)
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
    // The bar's letter reach, over everything else — see the parameter's note.
    selectionChars?.let { reach ->
        if (selectionWash != Color.Unspecified && !reach.collapsed) {
            val from = reach.min.coerceIn(0, text.length)
            val to = reach.max.coerceIn(0, text.length)
            if (to > from) addStyle(SpanStyle(background = selectionWash), from, to)
        }
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

/**
 * v402 — WHERE A TITLE LINE SAYS IT IS.
 *
 * A page's headings report themselves here so the page can hold one at the top
 * once it has scrolled by (see `PersonalPinnedLine`). The contract is narrow and
 * it is the ONE thing every bug in this feature came from getting wrong:
 *
 * 1. **The numbers are WINDOW coordinates** ([LayoutCoordinates.boundsInWindow],
 *    the same space `Modifier.onGloballyPositioned`'s `positionInRoot` speaks).
 *    The first version reported `boundsInParent()` — the line's place inside its
 *    own little wrapper box — so EVERY heading on the page reported a top of
 *    zero and the bar pinned whichever entry the map happened to iterate last
 *    ("sometimes something random will show", "it shows when the title isnt
 *    scrolled away yet", "it doesnt show all titles"). Never report a position
 *    relative to something that is not the window.
 * 2. **`scroll` is the scroll the line was placed at.** A report is taken when
 *    the line is laid out — rarely — and the page moves between reports, so the
 *    host shifts the stored numbers by how far the page has travelled since
 *    ([PersonalSectionLine.liveTop]). The report is right whether or not the
 *    platform re-fires a layout callback on every scroll frame.
 * 3. **A blank `label` means THIS ID IS NOT A PLACE ANYMORE** — the title flag
 *    was taken off the line, or the line itself is gone. The host removes it, so
 *    a pin can never name a heading that no longer exists ("sometimes something
 *    random will show even when i deselect the title or remove it").
 * 4. **`writing` says which side of the eye / pen switch is reporting.** Both
 *    sides can be composed at once for the length of the cross-fade, and they
 *    hold different scrolls in different boxes: the host keeps one entry per side
 *    and reads only the side it is showing, so a swap can never leave the bar
 *    judging the outgoing page's numbers.
 */
internal val LocalPersonalTitleReport =
    staticCompositionLocalOf<
        ((
            id: String,
            label: String,
            top: Float,
            bottom: Float,
            writing: Boolean,
            scroll: Float
        ) -> Unit)?
        > { null }

/**
 * v402 — THE SCROLL THAT HOLDS A READ VIEW'S TITLES.
 *
 * The writing side's scroll belongs to the page ([PersonalWritingPage]), but a
 * reading side is the CALLER's lambda and owns its own — so the pinned bar could
 * not move a reader back to a heading it was naming, and its "has it gone by?"
 * was judged against the writing page's scroll while the reader was looking at
 * the reading page's (user report: "it sometimes wont take me to where the title
 * is instead fully to the top").
 *
 * A read view DROPS its scroll in here ([PersonalPinScrollHolder.scroll]) instead
 * of being asked for it: the page provides the box, every read view writes into
 * it, and the bar reads whichever scroll is on screen. A read context that never
 * writes one still gets a correct pin — it simply cannot be tapped.
 */
internal class PersonalPinScrollHolder {
    var scroll: ScrollState? = null
}

internal val LocalPersonalPinScrollHolder =
    staticCompositionLocalOf<PersonalPinScrollHolder?> { null }

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
     *
     * v398 — AND SETTING IT TURNS ITS OWN TOOL ON. The page opens with the box
     * armed, so the first row is a box waiting to be written (which is what a
     * to-do page is for) and the dock's box button reads as switched on. From
     * there the ARM is the list's own switch: turning the box off — on the row,
     * or in the dock — takes the arm with it, and the next Enter then writes
     * prose instead of growing another box (user report: "when i deselect it …
     * it still makes the next line in enter automatic reselect").
     */
    var keepsChecklistRows: Boolean = false
        set(value) {
            field = value
            if (value) armed = armed or FLAG_CHECKBOX
        }

    /**
     * v398 — THE ROWS WHOSE LIST THE WRITER ENDED (see [onFieldChange]). A row
     * remembered here wears no box while it is blank, and the row after it is
     * prose: taking the words out of a to-do row is how the member said "this
     * list is done", not how they said "give me another empty box".
     */
    private val listEnded = mutableSetOf<String>()

    /** Whether a blank row of a to-do page is still a box waiting to be written
     *  — the renderer's half of the same rule (see [listEnded] and the arm). */
    fun checklistRowWaits(id: String): Boolean =
        id !in listEnded && armed and FLAG_CHECKBOX != 0

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
            //
            // v393e — THE BOX IS THE ROW'S, NOT ITS CHARACTERS'. The first pass
            // leaned on [armed] (transient state) to remember and drew the box
            // for a row that was perfectly EMPTY — so a row left holding a
            // leftover space lost its box, and a box re-opened from storage had
            // nothing to inherit from: the first word typed rubbed it out, the
            // very report the fix meant to answer. So the rule is now stated on
            // the ROW, in both directions: a row with no words on the list page
            // IS a box waiting to be written (the renderers draw it — see
            // PersonalTextBlock / PersonalDocView), [armed] carries the box
            // across the next keystroke, and the words landing in a row that was
            // blank wear it whatever the page's memory of the arm was.
            if (keepsChecklistRows) {
                if (newText.isBlank()) {
                    // v398 — THE LIST ENDS WHERE ITS WORDS WERE TAKEN OUT. A row
                    // that HAD words and now has none is a row the writer emptied,
                    // and their own rule is that emptying it ends the list there:
                    // the row is remembered (no box, and Enter under it writes
                    // prose) instead of the box being armed back over it. A row
                    // that was ALREADY empty is the row Enter just made, which is
                    // a box waiting to be written — so it keeps the arm.
                    if (old.text.isNotBlank()) {
                        listEnded.add(id)
                        armed = armed and FLAG_CHECKBOX.inv()
                    } else if (id !in listEnded) {
                        armed = armed or FLAG_CHECKBOX
                        armedOff = armedOff and FLAG_CHECKBOX.inv()
                    }
                } else if (id !in listEnded && old.text.isBlank() &&
                    // …unless the member asked for something else on that row:
                    // a title or a bullet armed on a blank line is what those
                    // tools MEAN, and the box must not step on it.
                    !personalBlockCarries(newText, mask(id), FLAG_TITLE) &&
                    !personalBlockCarries(newText, mask(id), FLAG_BULLET)
                ) {
                    masks[id] = maskApply(mask(id), 0, newText.length, FLAG_CHECKBOX, true)
                }
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
        // ── v417 — A PAGE-WIDE SELECTION ANSWERS FOR THE PAGE ───────────
        //
        // Select all (the two-finger gesture, the platform's own Select all,
        // Ctrl+A) sets `pageSelected`, and the dock then reports what the NEXT
        // keypress would wear on the FOCUSED ROW — which `selectPage` parks at
        // the end of the page. So the tools lit up for one line while the whole
        // page was selected, which is exactly what a member saw: "the select
        // all works but it doesnt show the tool". A flag reads active now when
        // EVERY writing row carries it — the same rule [toggle] uses to decide
        // what a page-wide tap means — so the dock says "the page is bold",
        // "the page is bulleted", and one tap turns it off everywhere.
        if (pageSelected) {
            val rows = order.filter { id ->
                blocks[id]?.let { !it.isPhoto && it.audio == null } == true
            }
            if (rows.isEmpty()) return armed
            var flags = 0
            ALL_FLAGS.forEach { flag ->
                val on = rows.all { id ->
                    val block = blocks[id] ?: return@all false
                    maskCovers(mask(id), 0, block.text.length, flag)
                }
                if (on) flags = flags or flag
            }
            return flags or armed
        }
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
        pageTextMenuRequest = false
    }

    /**
     * v424 — THE DOCK'S COPY TOOL, AS A REQUEST THE CANVAS ANSWERS.
     *
     * The dock and the page's own floating bar are two different pieces of the
     * tree (the bar is Android's, installed into `LocalTextToolbar` by the canvas
     * and gone with it), so the tool ASKS rather than reaches: the canvas sees
     * the request, selects the page and raises the platform's own bar above the
     * tools. A flag rather than a callback because the request outlives the frame
     * it was made in — the dock's button and the canvas are composed by different
     * screens, and neither holds a reference to the other.
     */
    var pageTextMenuRequest by mutableStateOf(false)
        private set

    fun requestPageTextMenu() {
        if (order.isEmpty()) return
        pageTextMenuRequest = true
    }

    fun consumePageTextMenu() {
        pageTextMenuRequest = false
    }

    /** Every row's words, top to bottom — what Copy puts on the clipboard. */
    fun pageText(): String = order
        .mapNotNull { blocks[it] }
        .filter { !it.isPhoto && it.audio == null }
        .joinToString("\n") { it.text }
        .trimEnd()

    // ── v427 — THE PAGE'S TEXT BAR ─────────────────────────────────────────
    //
    // The member: "in journal the copy tools selects all but i wanted it to open
    // a tool ith cut copy paste undo tool … select all copy button cut copy paste
    // starts a selection with arrow tools to go how much select then action done
    // with cut copy or paste, extra select all button as well".
    //
    // So the dock's copy door opens a BAR on the page instead of Android's own
    // menu: the arrows set how much of the page is in the selection, one row at
    // a time, and Cut, Copy, Paste and Undo act on what they have picked. Tap Cut
    // or Copy with nothing picked and the bar OFFERS THE WHOLE PAGE first — the
    // arrow is how the member then trims it — which is the two-tap flow they
    // described rather than a menu that decides the reach for them.
    var pageEditBarOpen by mutableStateOf(false)
        private set

    /** The bar's reach: a run of rows by index. EMPTY = nothing picked yet. */
    var pageRange by mutableStateOf(IntRange.EMPTY)
        private set

    /**
     * UNDO IS THE BAR'S OWN. Each bar action remembers how to put the page back
     * the way it found it (a cut remembers every row it took, whole), newest
     * last, and only the bar's actions push onto it — a per-keystroke undo is a
     * different tool, and pretending otherwise would be a lie about what the
     * button does.
     */
    private val pageUndo = ArrayDeque<() -> Unit>()

    /** One row the bar took away, kept whole (its words, its mask and where it
     *  sat) so Undo can put it back exactly where it was. */
    private class RemovedPageRow(
        val index: Int,
        val id: String,
        val block: PersonalBlock,
        val mask: IntArray,
        val selection: TextRange?
    )

    fun togglePageEditBar() {
        pageEditBarOpen = !pageEditBarOpen
        if (!pageEditBarOpen) clearPageReach()
    }

    fun closePageEditBar() {
        pageEditBarOpen = false
        clearPageReach()
    }

    private fun clearPageReach() {
        pageRange = IntRange.EMPTY
        pageCharRange = TextRange.Zero
        pageGrowsUp = true
    }

    /**
     * The whole page, as the selection (the bar's own All rows).
     *
     * It is drawn AS a reach built from the foot (`pageGrowsUp`), because that is
     * where a selection made from the bottom begins (see [nudgePageRows]): with
     * every row already in hand, the depth the arrows have left to work in is the
     * LETTERS, and those belong to the row the member was writing in — the last
     * one on the page.
     */
    fun selectWholePage() {
        pageRange = if (order.isEmpty()) IntRange.EMPTY else 0..order.lastIndex
        pageCharRange = TextRange.Zero
        pageGrowsUp = true
    }

    /**
     * THE ROWS ARROWS — ONE AXIS, ONE DIRECTION EACH, AND THE REACH IS BUILT FROM
     * ITS FOOT.
     *
     * The member: *"let user select things from bottom"*, and (of the old switch)
     * *"no more letter row option but the arrows do the work"*. So:
     *
     *  · A FRESH REACH IS ONE ROW where the writing is — the row the caret is in,
     *    or the page's last row when the caret is not on the page — never the whole
     *    page, so a selection can begin at the FOOT of the page and climb.
     *  · THE ARROW THAT IS PRESSED FIRST DECIDES WHICH WAY THE REACH GROWS: press
     *    ↑ and it grows upward from that row, press ↓ and it grows downward. It is
     *    a SELECTION built away from a place the member chose, which is what a
     *    range is.
     *  · THE OTHER ARROW GIVES A ROW BACK (off the far end), and at a single row it
     *    WALKS that row one step that way — so the reach's foot can be moved
     *    without a fifth control, and down is never a dead end at the page's end.
     *  · EVERY ROW ARROW STARTS THE LETTER WINDOW OVER (`pageCharRange`): the
     *    window belongs to the row the reach begins at, so changing the rows is
     *    the member changing their mind about the row, not about a clause.
     */
    fun nudgePageRows(up: Boolean) {
        if (order.isEmpty()) return
        if (pageRange.isEmpty()) {
            val anchor = focusedId?.let { id -> order.indexOf(id).takeIf { it >= 0 } }
                ?: order.lastIndex
            pageGrowsUp = up
            pageRange = anchor..anchor
            pageCharRange = TextRange.Zero
            return
        }
        pageCharRange = TextRange.Zero
        val range = pageRange
        // The same direction as the reach grows = MORE. The other = one row back.
        if (up == pageGrowsUp) {
            pageRange = if (pageGrowsUp) {
                (range.first - 1).coerceAtLeast(0)..range.last
            } else {
                range.first..(range.last + 1).coerceAtMost(order.lastIndex)
            }
            return
        }
        val single = range.first == range.last
        pageRange = when {
            !single && pageGrowsUp -> range.first + 1..range.last
            !single -> range.first..range.last - 1
            range.first > 0 && pageGrowsUp -> (range.first - 1)..(range.first - 1)
            range.last < order.lastIndex -> (range.last + 1)..(range.last + 1)
            else -> range
        }
    }

    /**
     * v427 — AND THE REACH GOES DOWN TO A LETTER, WITHOUT A MODE.
     *
     * The bar's reach was rows and only rows, which cannot pick a clause out of a
     * line (the member's *"why theres only row selection i also want letter by
     * letter too"*), and the mode it grew to fix that made the member state which
     * unit they meant before an arrow could tell them anything (*"no more letter
     * row option but the arrows do the work"*). The two AXES are the four arrows
     * now: ↑ ↓ for rows, ← → for letters, and the letters switch themselves on the
     * moment ← or → is pressed.
     *
     * The window always holds ONE ROW's characters — a character range that
     * crossed rows IS those rows, and the row axis already says it — and it opens
     * where the writing is: at the caret when the caret is in the row, otherwise at
     * the END of the row for a reach built upward from the page's foot (the member
     * is reading back up through what they wrote) and at its START for one built
     * downward.
     */
    /** True once ← or → has been used: the anchor row's letters are in the reach. */
    val pageLettersPicked: Boolean get() = pageCharRange.max > pageCharRange.min

    /** Which way the reach grew — the arrow that was pressed first (see
     *  [nudgePageRows]). */
    private var pageGrowsUp by mutableStateOf(true)

    /** The window of letters, inside the anchor row. */
    var pageCharRange by mutableStateOf(TextRange.Zero)
        private set

    /** The anchor row's index: the row the reach STARTED at, i.e. its foot when it
     *  grew upward and its head when it grew downward. */
    private fun pageAnchorIndex(): Int =
        if (pageRange.isEmpty()) -1 else if (pageGrowsUp) pageRange.last else pageRange.first

    /** The row the letter reach lives in. */
    private fun pageLetterRowId(): String? =
        order.getOrNull(pageAnchorIndex())

    /** How long that row's writing is. */
    private fun pageLetterRowLength(): Int =
        pageLetterRowId()?.let { blocks[it]?.text?.length ?: 0 } ?: 0

    /**
     * THE LETTER ARROWS. [more] is → and !more is ←.
     *
     * The first press OPENS the window at one character; from there more extends
     * the window's far end and less gives a character back. At the row's far end
     * (and at a single character) the window WALKS instead of sticking, which is
     * how the old mode kept its arrows meaningful without the reach leaving the
     * line it belongs to.
     */
    fun nudgePageLetters(more: Boolean) {
        val id = pageLetterRowId() ?: return
        val text = blocks[id]?.text.orEmpty()
        if (text.isEmpty()) return
        val range = pageCharRange
        pageCharRange = if (!pageLettersPicked) {
            if (!more) return
            val caret = selections[id]?.start ?: 0
            val at = if (caret in 0 until text.length) {
                caret
            } else if (pageGrowsUp) {
                text.length - 1
            } else {
                0
            }
            TextRange(at, (at + 1).coerceAtMost(text.length))
        } else if (more) {
            if (range.max < text.length) {
                TextRange(range.min, range.max + 1)
            } else {
                TextRange((range.min - 1).coerceAtLeast(0), range.max)
            }
        } else {
            when {
                range.max - range.min > 1 -> TextRange(range.min, range.max - 1)
                range.min > 0 -> TextRange(range.min - 1, range.max)
                else -> TextRange.Zero
            }
        }
    }

    /** How many rows the bar is holding — what the arrows count out loud. */
    val pageSelectionCount: Int
        get() = if (pageRange.isEmpty()) 0 else pageRange.last - pageRange.first + 1

    /** How many LETTERS the letter reach is holding, and how many the row has. */
    val pageLetterCount: Int
        get() = (pageCharRange.max - pageCharRange.min).coerceAtLeast(0)

    val pageLetterTotal: Int get() = pageLetterRowLength()

    /**
     * WHETHER AN ARROW HAS ANYTHING TO DO. The four arrows are the whole control
     * surface (the mode switch is gone), so a dead arrow has to LOOK dead — a
     * chip that answers a press with nothing is how a bar teaches a member to
     * stop pressing it.
     */
    fun canNudgePageRows(up: Boolean): Boolean {
        if (order.isEmpty()) return false
        if (pageRange.isEmpty()) return true
        val range = pageRange
        if (up == pageGrowsUp) {
            return if (pageGrowsUp) range.first > 0 else range.last < order.lastIndex
        }
        return if (range.first == range.last) {
            if (up) range.first > 0 else range.last < order.lastIndex
        } else {
            true
        }
    }

    fun canNudgePageLetters(more: Boolean): Boolean {
        if (pageRange.isEmpty()) return false
        val length = pageLetterRowLength()
        if (length == 0) return false
        if (!pageLettersPicked) return more
        val range = pageCharRange
        return if (more) range.max < length || range.min > 0 else range.max - range.min > 1 || range.min > 0
    }

    /** The whole of the anchor row's writing, as the letter reach — the bar's
     *  "Line", the counterpart of All rows. */
    fun selectPageLineLetters() {
        val length = pageLetterRowLength()
        pageCharRange = if (length == 0) TextRange.Zero else TextRange(0, length)
    }

    /**
     * IS THIS ROW IN THE BAR'S REACH? The page WASHES a picked row (see
     * [PersonalTextBlock]'s `selectionWash`), which is the highlight the bar was
     * missing: it said "4 of 12 rows" while the page showed nothing at all.
     */
    fun pageRowPicked(index: Int): Boolean =
        !pageRange.isEmpty() && index >= pageRange.first && index <= pageRange.last

    /**
     * THE LETTERS PICKED INSIDE [index] — or null, which is every row but the one
     * the letter window sits in. Drawn as a wash behind exactly those characters
     * (see [personalAnnotated]), so the reach is visible to the letter.
     */
    fun pageRowCharRange(index: Int): TextRange? {
        if (!pageLettersPicked) return null
        if (pageRange.isEmpty() || index != pageAnchorIndex()) return null
        val text = blocks[order.getOrNull(index) ?: return null]?.text.orEmpty()
        if (text.isEmpty()) return null
        val from = pageCharRange.min.coerceIn(0, text.length)
        val to = pageCharRange.max.coerceIn(0, text.length)
        return if (to > from) TextRange(from, to) else null
    }

    private fun selectedRowIds(): List<String> =
        if (pageRange.isEmpty()) emptyList() else pageRange.mapNotNull { order.getOrNull(it) }

    /**
     * The selection's words, top to bottom (a print or a voice note holds none).
     * With letters PICKED it is the characters the window holds and nothing else —
     * the member has said which clause they mean, so that is what Cut and Copy
     * take.
     */
    fun pageSelectionText(): String = if (pageLettersPicked) {
        val id = pageLetterRowId() ?: return ""
        val text = blocks[id]?.text.orEmpty()
        val from = pageCharRange.min.coerceIn(0, text.length)
        val to = pageCharRange.max.coerceIn(0, text.length)
        if (to > from) text.substring(from, to) else ""
    } else {
        selectedRowIds()
            .mapNotNull { blocks[it] }
            .filter { !it.isPhoto && it.audio == null }
            .joinToString("\n") { it.text }
            .trim('\n')
    }

    private fun rememberPageUndo(undo: () -> Unit) {
        if (pageUndo.size >= 8) pageUndo.removeFirst()
        pageUndo.addLast(undo)
    }

    val canUndoPageEdit: Boolean get() = pageUndo.isNotEmpty()

    fun undoPageEdit() {
        pageUndo.removeLastOrNull()?.invoke()
    }

    /** Copy: the selection's words, and the page is left exactly as it is. */
    fun copyPageSelection(): String = pageSelectionText()

    /**
     * CUT, WITH LETTERS PICKED: the characters the window holds leave the LINE
     * they were picked out of, and nothing else on the page moves. Undo puts them
     * back exactly where they were taken from.
     */
    private fun cutPageLetters(): String {
        val id = pageLetterRowId() ?: return ""
        val block = blocks[id] ?: return ""
        val range = pageRowCharRange(pageAnchorIndex()) ?: return ""
        val words = block.text
        val taken = words.substring(range.min, range.max)
        val mask = mask(id)
        rememberPageUndo {
            blocks[id] = blocks[id]?.copy(text = words) ?: block
            masks[id] = mask
            pageCharRange = range
            onDocChanged(doc())
        }
        val keptMask = IntArray(words.length - taken.length) { index ->
            mask.getOrElse(if (index < range.min) index else index + taken.length) { 0 }
        }
        blocks[id] = block.copy(text = words.removeRange(range.min, range.max))
        masks[id] = keptMask
        selections[id] = TextRange(range.min)
        pageCharRange = TextRange(range.min, range.min)
        onDocChanged(doc())
        return taken
    }

    /** Cut: the selection's words go back to the caller (the clipboard) and its
     *  ROWS leave the page — prints and voice notes included, because the member
     *  picked them. */
    fun cutPageSelection(): String {
        if (pageLettersPicked) return cutPageLetters()
        val ids = selectedRowIds()
        if (ids.isEmpty()) return ""
        val text = pageSelectionText()
        val removed = ids.mapNotNull { id ->
            val block = blocks[id] ?: return@mapNotNull null
            RemovedPageRow(order.indexOf(id), id, block, mask(id), selections[id])
        }
        rememberPageUndo {
            removed.sortedBy { row -> row.index }.forEach { row ->
                order.add(row.index.coerceAtMost(order.size), row.id)
                blocks[row.id] = row.block
                masks[row.id] = row.mask
                row.selection?.let { selections[row.id] = it }
            }
            pageRange = IntRange.EMPTY
            onDocChanged(doc())
        }
        ids.forEach { id ->
            order.remove(id)
            blocks.remove(id)
            masks.remove(id)
            selections.remove(id)
            compositions.remove(id)
        }
        pageRange = IntRange.EMPTY
        onDocChanged(doc())
        return text
    }

    /** Paste: every line of [text] arrives as its own row, under the selection
     *  (or at the foot of the page when nothing is picked). With LETTERS PICKED it
     *  lands INSIDE the line the window is in, at the window's own place — which
     *  is what pasting into a picked run of letters means. */
    fun pastePageText(text: String) {
        if (text.isEmpty()) return
        if (pageLettersPicked) {
            pastePageLetters(text)
            return
        }
        val at = if (pageRange.isEmpty()) order.size
                 else (pageRange.last + 1).coerceAtMost(order.size)
        val created = ArrayList<String>(4)
        rememberPageUndo {
            created.forEach { id ->
                order.remove(id)
                blocks.remove(id)
                masks.remove(id)
            }
            pageRange = IntRange.EMPTY
            onDocChanged(doc())
        }
        text.split('\n').forEachIndexed { offset, line ->
            val block = PersonalBlock(id = newBlockId(), text = line)
            order.add((at + offset).coerceAtMost(order.size), block.id)
            blocks[block.id] = block
            masks[block.id] = emptyMask(line.length)
            created.add(block.id)
        }
        pageRange = IntRange.EMPTY
        onDocChanged(doc())
    }

    /**
     * Paste INTO the line the letter window is in: the window's characters give
     * way to what was pasted, and the window then holds exactly what arrived, so
     * the member can see where it landed and carry on from there.
     */
    private fun pastePageLetters(text: String) {
        val id = pageLetterRowId() ?: return
        val block = blocks[id] ?: return
        val words = block.text
        val mask = mask(id)
        val at = pageCharRange.min.coerceIn(0, words.length)
        val until = pageCharRange.max.coerceIn(at, words.length)
        rememberPageUndo {
            blocks[id] = blocks[id]?.copy(text = words) ?: block
            masks[id] = mask
            pageCharRange = TextRange(at, until)
            onDocChanged(doc())
        }
        val inserted = text.replace('\n', ' ')
        val next = words.substring(0, at) + inserted + words.substring(until)
        val nextMask = emptyMask(next.length).also { fresh ->
            for (index in 0 until at) if (index < mask.size) fresh[index] = mask[index]
            for (index in until until words.length) {
                val to = at + inserted.length + (index - until)
                if (to < fresh.size && index < mask.size) fresh[to] = mask[index]
            }
        }
        blocks[id] = block.copy(text = next)
        masks[id] = nextMask
        selections[id] = TextRange(at + inserted.length)
        pageCharRange = TextRange(at, at + inserted.length)
        onDocChanged(doc())
    }

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
        // ── v403 — A SEAM WITH NOTHING ON IT IS NOT A LINE BETWEEN PICTURES ──
        //
        // The tool SPLITS the line under the caret, so a picture added under
        // another picture used to arrive with an empty line between them: the
        // leading piece of the split, with no words of its own, standing where
        // the member never put one. Two pictures added one after another then
        // shared no edge and never grouped, which is why adding them meant
        // dragging them together afterwards (user request: "make two photos
        // added one after another group on their own, without a drag").
        //
        // So when the piece being left behind is EMPTY (no words of the
        // member's) and the line above it is already a picture, it is not kept:
        // the arriving picture takes that place and lands against the one above
        // it, and the two become a row by themselves. Everything else about the
        // split is untouched — a piece with words on it, a picture added at the
        // top of the page, and every voice note (which never groups) keep the
        // empty line they were always given.
        val keepHead = !(
            photoBlock.isPhoto && head.text.isBlank() && index > 0 &&
                blocks[order[index - 1]]?.isPhoto == true
            )
        // The old block is replaced in place, so the page never jumps.
        blocks.remove(id)
        masks.remove(id)
        selections.remove(id)
        compositions.remove(id)
        val arrivedAt = if (keepHead) {
            order[index] = head.id
            blocks[head.id] = head
            masks[head.id] = runsToMask(head.text.length, head.runs)
            index + 1
        } else {
            // The empty seam goes, so the picture takes its place on the page.
            order.removeAt(index)
            index
        }
        order.add(arrivedAt, photoBlock.id)
        blocks[photoBlock.id] = photoBlock
        masks[photoBlock.id] = emptyMask(0)
        // v403 — and a picture that lands against a picture wears a cell's size
        // (see normaliseRowSizesAt), so the row it just took part in is a row the
        // member can see in the same frame.
        normaliseRowSizesAt(arrivedAt)
        val tailAt = arrivedAt + 1
        if (tailText.isNotEmpty()) {
            order.add(tailAt, tail.id)
            blocks[tail.id] = tail
            masks[tail.id] = runsToMask(tail.text.length, tail.runs)
            caret = PersonalCaret(tail.id, 0)
        } else {
            val empty = PersonalBlock(id = newBlockId())
            order.add(tailAt, empty.id)
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

    // ── v401 — THE CAPTION'S OWN LABEL ─────────────────────────────────
    //
    // A caption stopped being a string under a picture and became a LABEL the
    // print wears: its own words, its own face, its own size, and a date that is
    // its own line (see PersonalBlock.captionDateMillis). The dock swaps to the
    // caption's tools while one of these fields has the caret, which is what
    // `captionFocusedId` records — it is not "which field has focus" (the page
    // answers that), it is "is the member writing a label right now".

    /** The print whose caption has the caret, or null while the prose does. */
    var captionFocusedId by mutableStateOf<String?>(null)
        private set

    /** The caption field's focus, reported by the field itself (see
     *  PersonalPhotoBlock). A focused field claims the dock; losing focus
     *  releases it — including when the words are tapped, which is the way back
     *  to the writing tools. */
    fun setCaptionFocus(id: String?) {
        if (captionFocusedId != id) captionFocusedId = id
    }

    fun captionDate(id: String): Long = blocks[id]?.captionDateMillis ?: 0L

    fun captionFace(id: String): String = blocks[id]?.captionFace.orEmpty()

    fun captionSizeKey(id: String): String = blocks[id]?.captionSize.orEmpty()

    fun captionOrder(id: String): String = blocks[id]?.captionOrder.orEmpty()

    fun setCaptionDate(id: String, millis: Long) {
        val block = blocks[id] ?: return
        if (block.captionDateMillis == millis) return
        blocks[id] = block.copy(captionDateMillis = millis)
        onDocChanged(doc())
    }

    fun setCaptionFace(id: String, key: String) {
        val block = blocks[id] ?: return
        if (block.captionFace == key) return
        blocks[id] = block.copy(captionFace = key)
        onDocChanged(doc())
    }

    fun setCaptionSize(id: String, key: String) {
        val block = blocks[id] ?: return
        if (block.captionSize == key) return
        blocks[id] = block.copy(captionSize = key)
        onDocChanged(doc())
    }

    fun setCaptionOrder(id: String, key: String) {
        val block = blocks[id] ?: return
        if (block.captionOrder == key) return
        blocks[id] = block.copy(captionOrder = key)
        onDocChanged(doc())
    }

    /** v421 — a voice note's LOOK ([PersonalVoiceStyle.key], stored with the
     *  block so a re-opened page draws the note the way it was given). */
    fun voiceStyle(id: String): PersonalVoiceStyle =
        PersonalVoiceStyle.fromKey(blocks[id]?.audioStyle)

    fun setVoiceStyle(id: String, style: PersonalVoiceStyle) {
        val block = blocks[id] ?: return
        if (block.audioStyle == style.key) return
        blocks[id] = block.copy(audioStyle = style.key)
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
        // v403 — and if that landing put a PRINT beside another print, it takes a
        // cell's size on the way in (see normaliseRowSizesAt), so the row the
        // member just built is a row they can see in the same frame.
        normaliseRowSizesAt(adjustedTo)
        onDocChanged(doc())
    }

    /**
     * v403 — WHERE A CARRIED PRINT REALLY LANDS.
     *
     * A run of consecutive prints is ONE ROW (see the drawing pass), and only
     * the run's FIRST member is drawn and measured — every other member is a
     * slot the finger's own step count glides straight through. So a print
     * dropped "on" a pair landed BETWEEN the pair's two members: the pair the
     * member was trying to build turned into a three, or came apart, which is
     * the "something the photos wont group even when i try to place it above the
     * photo … sometimes it ungroups" they reported.
     *
     * A carried print therefore SNAPS to the run it was dropped against: above
     * the run when it is taking the row's LEFT cell, just below it when it is
     * taking the right one, and nowhere near it when the block at the landing
     * slot is not a print at all. A block that is not a print is left exactly
     * where the finger put it — paragraphs, voice notes and to-do rows still
     * land where they were dropped.
     *
     * v406 — WHICH END IS NO LONGER THE VERTICAL DIRECTION'S ANSWER.
     *
     * It used to be (`goingDown`), so a member who carried a picture down onto
     * a pair had no way to ask for the other side: every drop into a row landed
     * on its head. The end comes from [PersonalRowDragState.takeLeftCell] now
     * — a sideways drag when there is one, the vertical direction when there is
     * not — and the SAME value draws the landing ghost, so the cell shown in the
     * air is the cell that is taken (see the drawing pass below).
     */
    fun printDropIndex(from: Int, to: Int, takeLeft: Boolean): Int {
        val ids = order
        if (from !in ids.indices) return to
        if (blocks[ids[from]]?.isPhoto != true) return to
        fun isPrint(index: Int): Boolean =
            index in ids.indices && blocks[ids[index]]?.isPhoto == true
        // The run the finger is pointing at: the print at the landing slot, or
        // the one just above it when the finger has already crossed it.
        val anchor = when {
            isPrint(to) -> to
            isPrint(to - 1) -> to - 1
            else -> return to
        }
        var start = anchor
        while (isPrint(start - 1)) start--
        var end = anchor
        while (isPrint(end + 1)) end++
        val below = end + 1
        // ── AND A PRINT THAT IS ALREADY IN THE ROW STAYS IN IT (v403) ────────
        //
        // The first cut snapped every drop to one of the run's two edges, so a
        // print that already shared the row got thrown to the run's head when
        // the member simply pressed on it again — "sometimes it upgroups" — and
        // a print sitting against the run from above or below was "moved" to
        // where it already stood, which is a drag with no answer.
        //
        // So: a cell of the run is re-ordered inside it, a print that already
        // touches the run from either side is left exactly where it is, and
        // only a print arriving from somewhere else is brought against the run
        // — as the row's FIRST cell when the finger was travelling down (the
        // member is dropping onto the row's head) and as its LAST cell when the
        // finger was travelling up.
        val landing = when {
            from in start..end -> to
            from == start - 1 || from == end + 1 -> from
            takeLeft -> start
            below in ids.indices -> below
            else -> start
        }
        return landing.coerceIn(0, ids.lastIndex)
    }

    /**
     * v403 — A PRINT THAT HAS JUST JOINED A ROW WEARS A ROW'S SIZE.
     *
     * PAGE is the size a print ARRIVES with, and it means "the whole measure of
     * the page" — which is the one thing a cell of a row cannot be. So when two
     * or more prints stand together and one of them is still on its arrival
     * size, that print takes [PersonalPhotoSize.HALF] and the row is a row the
     * member can see (user request: "the page style should auto adjust when im
     * holding and trying to put two images together").
     *
     * A print ALONE keeps exactly the size it has (nothing is normalised until
     * two are together), and a size the member picked themselves is never
     * touched — only the untouched arrival size is. The menu stays honest either
     * way: what it says is what the picture wears.
     *
     * It is called from [moveBlock], BEFORE the page is handed over, so a drop
     * that groups two prints is ONE change to undo (and one save), not a move
     * followed by a resize the member never asked for.
     */
    private fun normaliseRowSizesAt(at: Int) {
        val ids = order
        val moved = blocks[ids.getOrNull(at) ?: return] ?: return
        if (!moved.isPhoto) return
        var start = at
        while (start > 0 && blocks[ids[start - 1]]?.isPhoto == true) start--
        var end = at
        while (end < ids.lastIndex && blocks[ids[end + 1]]?.isPhoto == true) end++
        // One print on its own line is still its own line.
        if (end - start < 1) return
        for (i in start..end) {
            val id = ids[i]
            val block = blocks[id] ?: continue
            if (PersonalPhotoSize.fromKey(block.photoSize) != PersonalPhotoSize.PAGE) continue
            blocks[id] = block.copy(photoSize = PersonalPhotoSize.HALF.key)
        }
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
            // The TO-DO page's own manner, kept: Enter on a BLANK row ends the
            // list instead of arming the next row for ever (a page of checklists
            // has to stop somewhere; a row holding one stray space has no words
            // in it either, so it ends the list the same way).
            keepsChecklistRows && block.text.isBlank() -> 0
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

    /**
     * v413 — PLAIN WORDS AT THE CARET, ONE LINE EACH (the paste gesture).
     *
     * The insertion rules are [insertTitleLine]'s — after the line the caret is
     * in, or at the end of the writing when there is no caret — with the one
     * difference that makes it a PASTE rather than a heading: the words carry NO
     * FLAG_TITLE runs, so they arrive as the prose they were copied as.
     *
     * A clipboard string is usually several lines, and a page is one block per
     * line, so the text is SPLIT: pasting three sentences puts three lines in,
     * not one block that has newlines hidden inside it (a block's text is a
     * line — see the page's own model). The caret lands at the end of the last
     * line inserted, which is where a paste leaves you in any editor.
     */
    fun insertPlainText(clipped: String) {
        if (clipped.isEmpty()) return
        // Named `clipped`, never `text`: this class has a `text(id)` member, and
        // a parameter of that name would shadow the very call used below.
        val lines = clipped.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val after = focusedId?.let { order.indexOf(it) }?.takeIf { it >= 0 }
            ?: order.indexOfLast { id -> !(blocks[id]?.isPhoto ?: false) }
        var at = if (after < 0) order.size else (after + 1).coerceAtMost(order.size)
        lines.forEach { line ->
            val block = PersonalBlock(id = newBlockId(), text = line)
            order.add(at, block.id)
            blocks[block.id] = block
            masks[block.id] = runsToMask(line.length, block.runs)
            at++
        }
        val last = order.getOrNull((at - 1).coerceAtLeast(0))
        if (last != null) {
            caret = PersonalCaret(last, text(last).length)
            focusedId = last
        }
        // Pasted words are already written, so nothing is armed for what is
        // typed next — the same rule a picked chapter name follows.
        armed = 0
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
    onTitlePosition: ((
        id: String,
        label: String,
        top: Float,
        bottom: Float,
        writing: Boolean,
        scroll: Float
    ) -> Unit)? = null
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
    // ── v424 — THE PAGE'S OWN COPY TOOL (the dock's Copy asks for this) ──
    //
    // It is not a clipboard write of its own: it SELECTS the page and asks
    // Android for the one floating bar a member already knows — the bar with
    // Copy and Select all on it — anchored at the page's own foot, so it stands
    // ABOVE the tools that opened it instead of over the words (member: "add a
    // full text copy option but for that it opens up the copy paste cut select
    // all tool in the floating tool which stays above the tool when its active
    // as floating tools"). Cut and Paste stay with the FIELD's own bar: they
    // need a caret to act on, and a page-wide selection has none.
    var pageBounds by remember { mutableStateOf(Rect.Zero) }
    LaunchedEffect(state.pageTextMenuRequest) {
        if (!state.pageTextMenuRequest) return@LaunchedEffect
        state.selectPage()
        state.consumePageTextMenu()
        pageToolbar.showMenu(
            rect = Rect(pageBounds.left, pageBounds.bottom, pageBounds.right, pageBounds.bottom + 1f),
            onCopyRequested = {
                val whole = state.pageText()
                if (whole.isNotBlank()) clipboard.setText(AnnotatedString(whole))
            },
            onPasteRequested = null,
            onCutRequested = null,
            onSelectAllRequested = { state.selectPage() }
        )
    }

    CompositionLocalProvider(LocalTextToolbar provides pageToolbar) {
    Column(
        modifier = modifier
            .clickable(enabled = enabled) { state.focusLastLine() }
            // The bar above is anchored to this page's own foot, so the canvas
            // has to know where it is — read in the layout and written only when
            // it has actually moved.
            .onGloballyPositioned { coords ->
                val origin = coords.positionInRoot()
                val next = Rect(
                    origin.x,
                    origin.y,
                    origin.x + coords.size.width,
                    origin.y + coords.size.height
                )
                if (next != pageBounds) pageBounds = next
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // v389d — CONSECUTIVE PRINTS SHARE A ROW.
        //
        // Every run of consecutive non-PAGE prints is ONE ROW, up to four
        // prints long, and the ROW's shape is chosen by how many arrived — a
        // pair's two even halves, a three's upright frame with two stacked
        // beside it, a four's square (v396; user request: "a grid 2*2 but for 3
        // dont make it one big and all … portraight and then 2 small can be fit
        // to the other side"). The run's FIRST print draws the whole row, so
        // every other member is SKIPPED by the drawing loop below.
        //
        // v389d — NOT REMEMBERED, deliberately. Both answers are read straight
        // from the page every time it composes (a photo's size and the words
        // under it are exactly the things the member changes while looking at
        // them), which is also what keeps the two passes from disagreeing: a
        // remembered pair set flipped a resized print out of the page for good.
        val groupSkips = mutableSetOf<String>()
        val printRows = mutableMapOf<String, List<String>>()
        val besideSkips = mutableSetOf<String>()
        run {
            val ids = state.blockIds
            var i = 0
            while (i < ids.size) {
                val first = state.block(ids[i])
                // v403 — EVERY PRINT CAN JOIN A ROW, PAGE INCLUDED.
                //
                // The rule used to be "a PAGE print leaves a row entirely",
                // and PAGE is the size a NEW print arrives with — so two
                // pictures brought together never grouped until the member
                // happened to resize one, which is the "the photos wont group
                // even when i try to place it above the photo" they reported.
                // Placing a print against another print is the member SAYING
                // these two go together, so they become the row they were put
                // into, and the sizes they each carry still decide each cell's
                // own share of it. A print alone on its line is still a print
                // alone: the run only forms when two or more stand together.
                if (first?.isPhoto == true) {
                    val run = ArrayList<String>()
                    // v427 — THE BLANK LINES THE RUN STEPS OVER.
                    //
                    // A run only ever formed from CONSECUTIVE prints, so a print
                    // that ended up on a line of its own — a press of Enter
                    // between two pictures, the empty line a carried print leaves
                    // behind, a row the member nudged apart — could never rejoin
                    // the older line it belonged to (member: "a photo of a
                    // different line fails to merge with a older line"). Two
                    // prints with nothing but AIR between them are one row; the
                    // air is collected here and left out of the drawing pass, the
                    // same way the row's later cells are.
                    val gaps = ArrayList<String>()
                    var j = i
                    while (j < ids.size && run.size < PRINT_ROW_LIMIT) {
                        val member = state.block(ids[j])
                        if (member?.isPhoto == true) {
                            run.add(ids[j])
                            j += 1
                        } else if (member != null && printRowGapSteppable(state, ids[j])) {
                            gaps.add(ids[j])
                            j += 1
                        } else {
                            break
                        }
                    }
                    if (run.size > 1) {
                        printRows[run.first()] = run
                        run.drop(1).forEach { groupSkips.add(it) }
                        gaps.forEach { gap ->
                            // v428 — AIR THE MEMBER IS STANDING IN IS STILL AIR,
                            // BUT IT IS STILL DRAWN.
                            //
                            // A blank line only ever blocked a row, so two prints
                            // with nothing but a blank line between them were one
                            // row in the eye view and TWO prints in the pen view
                            // the moment the caret happened to rest in that blank
                            // line — which is where the caret lands by itself after
                            // almost any picture is added (member: "sometimes they
                            // unstack"; and the project's own rule is that the two
                            // passes must never disagree about which pictures are
                            // one row). So the caret no longer breaks the row: the
                            // prints group anyway, and the one blank line the
                            // member is in is left VISIBLE and drawn under the
                            // row (see the drawing pass) instead of being hidden
                            // with the rest of the air — a caret has to be
                            // somewhere it can be seen.
                            if (gap != state.focusedId && state.selection(gap) == null) {
                                groupSkips.add(gap)
                            }
                        }
                        i = j
                        continue
                    }
                    // A LONE SMALL PRINT still keeps room for the writing under
                    // it — the beside pair, unchanged.
                    val nextId = ids.getOrNull(i + 1)
                    val next = nextId?.let { state.block(it) }
                    if (nextId != null && next != null &&
                        state.photoSize(ids[i]) == PersonalPhotoSize.SMALL &&
                        !next.isPhoto && !next.isAudio && next.text.isNotBlank()
                    ) {
                        besideSkips.add(nextId)
                        i += 2
                        continue
                    }
                }
                i += 1
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
        // a later cell of a print row (see [printRows]) and an id the page no
        // longer holds are left out of the list instead of returned past.
        val rows = state.blockIds.mapIndexedNotNull { index, id ->
            val block = state.block(id)
            if (block != null && !groupSkips.contains(id)) Triple(index, id, block) else null
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
                // The LANDING GHOST: a bar of the carried block's own height at
                // the edge it will land on, so the page says where — and WHAT —
                // is about to arrive (v397).
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
                                // v397 — THE LANDING GHOST.
                                //
                                // A bare 2dp bar said "somewhere around here"; a
                                // ghost of the carried block's OWN height says what
                                // is landing and exactly how much room it takes —
                                // the hover preview a page needs when the thing in
                                // hand is a print being stacked among other prints,
                                // or a voice note between paragraphs (user request:
                                // "proper hover preview same pass for voice
                                // recorder in animation, and proper preview of
                                // photo stacing with snap"). The band sits at the
                                // TOP of the target on the way down and at its
                                // BOTTOM on the way up, so the preview is always on
                                // the side the block is travelling towards.
                                //
                                // drawWithContent so it sits ON TOP of the text,
                                // not behind it.
                                drawContent()
                                val band = rowDrag.carriedHeight.coerceIn(0f, size.height)
                                val lead = if (rowDrag.goingDown) 0f else size.height - band
                                if (band > 0f) {
                                    // ── v400 — THE PREVIEW IS THE THING, NOT A BAR ──
                                    //
                                    // This used to be a flat wash with a solid
                                    // rule on the landing edge, which said "about
                                    // here" and nothing more. It is now the
                                    // carried block's own SHAPE, sketched: a light
                                    // fill inside a dashed outline of exactly the
                                    // room it will take, so the page previews what
                                    // is arriving (a waveform strip, a paragraph,
                                    // a print) before it lands (user request:
                                    // "instead of that color line use proper preview
                                    // and smooth animations").
                                    val radius = CornerRadius(10.dp.toPx())
                                    val hairline = 1.2.dp.toPx()
                                    drawRoundRect(
                                        color = accent.copy(alpha = 0.09f),
                                        topLeft = Offset(0f, lead),
                                        size = Size(size.width, band),
                                        cornerRadius = radius
                                    )
                                    drawRoundRect(
                                        color = accent.copy(alpha = 0.5f),
                                        topLeft = Offset(hairline, lead + hairline),
                                        size = Size(
                                            (size.width - hairline * 2f).coerceAtLeast(0f),
                                            (band - hairline * 2f).coerceAtLeast(0f)
                                        ),
                                        cornerRadius = radius,
                                        style = Stroke(
                                            width = hairline,
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(7.dp.toPx(), 6.dp.toPx())
                                            )
                                        )
                                    )
                                    // ── v403 — NO RULE, AND A PRINT'S OWN CELL ────
                                    //
                                    // The solid rule that sat on the landing edge
                                    // is gone: a line says "somewhere on this
                                    // edge", and the member asked for the THING
                                    // instead ("stop using lines for preview").
                                    // What is left is the dashed room the block
                                    // will take.
                                    //
                                    // And when a PRINT is being brought against
                                    // another print, the room drawn is the CELL
                                    // it will take in the row the two are about
                                    // to become — the half of the measure on the
                                    // side the finger is travelling — so the page
                                    // answers "these two go together" while the
                                    // print is still in the air.
                                    //
                                    // Both are drawn INSIDE the room's own
                                    // branch, so `radius`, `hairline`, `lead`
                                    // and `band` are the room they describe.
                                    val carriedRef = rowDrag.draggedId
                                    val carriedIsPrint =
                                        carriedRef?.let { state.block(it)?.isPhoto } == true
                                    if (carriedIsPrint && carriedRef != id && block.isPhoto) {
                                        val cellWidth = (size.width * 0.5f).coerceAtLeast(1f)
                                        // v406 — the SAME value the drop uses
                                        // (see printDropIndex), so the dashed
                                        // cell in the air is the cell taken.
                                        val cellLeft =
                                            if (rowDrag.takeLeftCell) 0f
                                            else size.width - cellWidth
                                        drawRoundRect(
                                            color = accent.copy(alpha = 0.10f),
                                            topLeft = Offset(cellLeft, lead),
                                            size = Size(cellWidth, band),
                                            cornerRadius = radius
                                        )
                                        drawRoundRect(
                                            color = accent.copy(alpha = 0.55f),
                                            topLeft = Offset(
                                                cellLeft + hairline,
                                                lead + hairline
                                            ),
                                            size = Size(
                                                (cellWidth - hairline * 2f).coerceAtLeast(0f),
                                                (band - hairline * 2f).coerceAtLeast(0f)
                                            ),
                                            cornerRadius = radius,
                                            style = Stroke(
                                                width = hairline,
                                                pathEffect = PathEffect.dashPathEffect(
                                                    floatArrayOf(7.dp.toPx(), 6.dp.toPx())
                                                )
                                            )
                                        )
                                    }
                                }
                            } else Modifier
                        )
                ) {
                if (block.isPhoto) {
                    // ── THE ROW OF PRINTS (v389d, v396) ───────────────────
                    //
                    // The FIRST print of a run draws the whole row (see
                    // [PersonalPrintArrangement]); every other member was left
                    // out of `rows` entirely, so it is never drawn twice. Each
                    // CELL is still its own movable block, so one print can be
                    // picked up out of a row of four and dropped somewhere else
                    // — the row's other members simply re-flow when it leaves.
                    val row = printRows[id]
                    val nextIndex = index + 1
                    val nextId = state.blockIds.getOrNull(nextIndex)
                    val nextBlock = nextId?.let { state.block(it) }
                    if (row != null) {
                        PersonalPrintArrangement(
                            ids = row,
                            sizeOf = { member -> state.photoSize(member) }
                        ) { memberId, slotHeight, cellModifier ->
                            val member = state.block(memberId)
                            if (member != null) {
                                PersonalMovableBlock(
                                    id = memberId,
                                    index = state.blockIds.indexOf(memberId),
                                    state = state,
                                    drag = rowDrag,
                                    enabled = enabled,
                                    // v397 — a print lifts as a photograph does:
                                    // a touch bigger, tilted, with a deeper shadow.
                                    heldScale = 1.08f,
                                    heldTilt = -2.5f,
                                    heldLift = 16.dp,
                                    modifier = cellModifier
                                ) {
                                    PersonalPhotoBlock(
                                        uri = member.photo.orEmpty(),
                                        caption = state.caption(memberId),
                                        size = state.photoSize(memberId),
                                        // The row's cell fills the share it was
                                        // given, at the height the row chose.
                                        paired = true,
                                        slotHeight = slotHeight,
                                        ink = ink, accent = accent,
                                        enabled = enabled,
                                        onCaption = { state.setCaption(memberId, it) },
                                        onSize = { state.setPhotoSize(memberId, it) },
                                        onRemove = { state.removeBlock(memberId) },
                                        // v401 — the label this print wears, and
                                        // the way the member changes it (the
                                        // caption's own dock, see PersonalToolDock).
                                        captionDate = state.captionDate(memberId),
                                        captionFace = state.captionFace(memberId),
                                        captionSizeKey = state.captionSizeKey(memberId),
                                        captionOrder = state.captionOrder(memberId),
                                        onCaptionFocus = { focused ->
                                            state.setCaptionFocus(if (focused) memberId else null)
                                        },
                                        onCaptionDate = { state.setCaptionDate(memberId, it) },
                                        onCaptionFace = { state.setCaptionFace(memberId, it) },
                                        onCaptionSize = { state.setCaptionSize(memberId, it) },
                                        onCaptionOrder = { state.setCaptionOrder(memberId, it) },
                                        onOpen = { bounds ->
                                            onOpenPhoto(member.photo.orEmpty(), bounds)
                                        }
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
                            // v427 — the pair's split is the PRINT'S OWN SIZE
                            // (see [printBesideShare]).
                            Box(Modifier.weight(printBesideShare(state.photoSize(id)))) {
                                PersonalMovableBlock(
                                    id = id, index = index, state = state,
                                    drag = rowDrag, enabled = enabled,
                                    // v397 — see the row's cells.
                                    heldScale = 1.08f,
                                    heldTilt = -2.5f,
                                    heldLift = 16.dp
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
                                        // v401 — see the row's cells.
                                        captionDate = state.captionDate(id),
                                        captionFace = state.captionFace(id),
                                        captionSizeKey = state.captionSizeKey(id),
                                        captionOrder = state.captionOrder(id),
                                        onCaptionFocus = { focused ->
                                            state.setCaptionFocus(if (focused) id else null)
                                        },
                                        onCaptionDate = { state.setCaptionDate(id, it) },
                                        onCaptionFace = { state.setCaptionFace(id, it) },
                                        onCaptionSize = { state.setCaptionSize(id, it) },
                                        onCaptionOrder = { state.setCaptionOrder(id, it) },
                                        onOpen = { bounds -> onOpenPhoto(block.photo.orEmpty(), bounds) }
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    // v427 — the writing keeps the rest of the
                                    // measure, whatever share the print took.
                                    .weight(1f - printBesideShare(state.photoSize(id)))
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
                            drag = rowDrag, enabled = enabled,
                            // v397 — see the row's cells.
                            heldScale = 1.08f,
                            heldTilt = -2.5f,
                            heldLift = 16.dp
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
                                // v401 — see the row's cells.
                                captionDate = state.captionDate(id),
                                captionFace = state.captionFace(id),
                                captionSizeKey = state.captionSizeKey(id),
                                captionOrder = state.captionOrder(id),
                                onCaptionFocus = { focused ->
                                    state.setCaptionFocus(if (focused) id else null)
                                },
                                onCaptionDate = { state.setCaptionDate(id, it) },
                                onCaptionFace = { state.setCaptionFace(id, it) },
                                onCaptionSize = { state.setCaptionSize(id, it) },
                                onCaptionOrder = { state.setCaptionOrder(id, it) },
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
                        enabled = enabled,
                        // v397 — a voice note is a CARD, not a photograph: it
                        // rises evenly with no tilt (a strip has no top or bottom
                        // to tilt about) and a shade less shadow than a print.
                        heldScale = 1.03f,
                        heldLift = 12.dp
                    ) {
                        PersonalVoicePageBlock(
                            path = block.audio.orEmpty(),
                            seconds = block.audioSeconds,
                            bars = block.audioBars,
                            ink = ink,
                            accent = accent,
                            enabled = enabled,
                            onRemove = { state.removeBlock(id) },
                            // v421 — the note's own look, and the door to its
                            // picker, live on the note in the page (see
                            // PersonalVoiceStyle).
                            style = state.voiceStyle(id),
                            onStyle = { state.setVoiceStyle(id, it) }
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
                        // v427 — AND THE BAR'S REACH WEARS THE SAME WASH, so the
                        // page shows what the bar is counting (both the rows it
                        // has picked and, inside the front one, the letters).
                        selectionWash = if (state.pageSelected || state.pageRowPicked(index)) {
                            selectionWash
                        } else {
                            Color.Transparent
                        },
                        selectionChars = state.pageRowCharRange(index)
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
     *  (transparent on every ordinary page, so nothing changes there).
     *
     *  v427 — and the bar's own reach wears it too (see `pageRowPicked`), which
     *  is the highlight the text bar was missing: it said "4 of 12 rows" while
     *  the page showed nothing. */
    selectionWash: Color = Color.Transparent,
    /** The LETTERS of this row the bar is holding (see `pageRowCharRange`). */
    selectionChars: TextRange? = null,
    ink: Color,
    accent: Color,
    enabled: Boolean,
    /** The gap above/below holds another quoted line — see QUOTE_JOIN_EDITOR. */
    quoteJoinAbove: Boolean = false,
    quoteJoinBelow: Boolean = false,
    /** See [PersonalCanvas.onTitlePosition]. */
    onTitlePosition: ((
        id: String,
        label: String,
        top: Float,
        bottom: Float,
        writing: Boolean,
        scroll: Float
    ) -> Unit)? = null,
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
    val rowBody = if (rowPage) rowSize.value * 1.7f else BODY_BODY_LINE.value
    val rowMark = if (rowPage) ROW_MARKER_SIZE else PERSONAL_MARKER_SIZE
    val quoteRule = personalQuoteRule()
    val quoteWash = personalQuoteWash()
    val quoteInk = personalQuoteColor().copy(alpha = 0.92f)
    val bulletInk = personalBulletColor()
    val isQuote = personalBlockIsQuote(text, mask)
    // A line that IS a title (or a small note) is set by the BLOCK, so a
    // heading really is bigger writing and not just a bolder word.
    val isTitle = personalBlockCarries(text, mask, FLAG_TITLE)
    // v402 — A LINE THAT STOPS BEING A TITLE STOPS BEING A PLACE.
    //
    // Taking the title flag off a line (or deleting the line) used to leave its
    // entry in the page's map of headings for ever, so the pinned bar could go
    // on naming a heading that was no longer there (user report: "sometimes
    // something random will show even when i deselect the title or remove it").
    // A blank label means "not a place anymore" — see LocalPersonalTitleReport.
    if (onTitlePosition != null) {
        // Keyed on the ID as well as the flag: a line that leaves the page (or
        // stops being a title) clears its OWN entry, and never the one a
        // neighbour has since reported.
        DisposableEffect(isTitle, id) {
            onDispose { onTitlePosition?.invoke(id, "", 0f, 0f, true, 0f) }
        }
    }
    val isSmall = personalBlockCarries(text, mask, FLAG_SMALL)
    val isBullet = personalBlockCarries(text, mask, FLAG_BULLET)
    // v393 — a row with no words on the list page still draws its box: the row's
    // flag lives on its characters, and an emptied row has none — but a to-do row
    // without a box reads as a row that lost its place in the list, not as a row
    // waiting to be written (user report: "the checkbox deletes when i delete all
    // the text").
    //
    // v393e — BLANK, not only perfectly empty: a leftover space is not a task
    // either, and the box used to vanish the moment a word was deleted down to
    // one (the same report, on the row that still had a character in it).
    // v398 — AND A BLANK ROW IS ONLY A BOX WHILE THE TOOL IS ON. The box on a row
    // with WORDS is the row's own (v393e); the box on a row with none is the
    // writer being ABOUT to write a task, so it follows the arm: turning the to-do
    // tool off (or emptying the row) ends the list, and the next line is prose
    // rather than another box (user report: "when i deselect it … it still makes
    // the next line in enter automatic reselect").
    val isCheckbox = personalBlockCarries(text, mask, FLAG_CHECKBOX) ||
        (text.isBlank() && state.keepsChecklistRows && state.checklistRowWaits(id))
    val lineHeight = when {
        isTitle -> TITLE_BODY_LINE
        isSmall -> SMALL_BODY_LINE
        else -> rowBody.sp
    }
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
            smallSize = if (isSmall) TextUnit.Unspecified else SMALL_BODY_SIZE,
            // The bar's own reach — the letters it is holding, washed in the
            // same colour the page washes a whole picked row with (null on every
            // page whose bar is closed, which is every page but the one being
            // edited).
            selectionChars = selectionChars,
            selectionWash = selectionWash
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
            lineHeight = TITLE_BODY_LINE,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            textAlign = alignOf
        )
        isSmall -> TextStyle(
            fontFamily = WritingFontFamily,
            fontSize = SMALL_BODY_SIZE,
            lineHeight = SMALL_BODY_LINE,
            color = ink,
            textAlign = alignOf
        )
        else -> TextStyle(
            fontFamily = WritingFontFamily,
            fontSize = if (rowPage) rowSize else BODY_BODY_SIZE,
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
                        val bounds = coordinates.boundsInWindow()
                        // The scroll sent here is 0 on purpose: the WRITING
                        // side's scroll belongs to the page, and the page's own
                        // reporter reads it (see LocalPersonalTitleReport). Only
                        // a read view, whose scroll nobody else can see, sends
                        // one across.
                        onTitlePosition(id, text, bounds.top, bounds.bottom, true, 0f)
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
                            val barWidth = QUOTE_RULE_WIDTH.toPx()
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
                        .padding(start = QUOTE_LEAD)
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
    SMALL("small", "Small", 0.44f),
    /**
     * v396 — THE SMALL PRINT THAT STANDS UP.
     *
     * A portrait frame at the small print's own width, for the picture that
     * belongs in a row (a face beside two lines, a doorway beside a paragraph)
     * rather than on a page of its own (user request: "for portraight add one
     * more small portraight view too"). [PORTRAIT] is the page-height version;
     * this one is the size a print takes when the writing keeps its room.
     */
    SMALL_PORTRAIT("small_portrait", "Small portrait", 0.36f);

    companion object {
        fun fromKey(key: String?): PersonalPhotoSize =
            entries.firstOrNull { it.key == key } ?: PAGE

        /** True for a size that stands UP — see the print rows in PersonalCanvas. */
        fun isUpright(size: PersonalPhotoSize): Boolean =
            size == PORTRAIT || size == SMALL_PORTRAIT
    }
}

/** The height a print takes when it is the only thing in its measure. */
internal fun personalPrintHeight(size: PersonalPhotoSize): Dp = when (size) {
    PersonalPhotoSize.PAGE -> 168.dp
    PersonalPhotoSize.HALF -> 128.dp
    // The two that stand up: a portrait print is a page-height picture on a
    // narrow column, and the small one is the same shape at the width a row of
    // prints gives it (see [PersonalPhotoSize]).
    PersonalPhotoSize.PORTRAIT -> 232.dp
    PersonalPhotoSize.SMALL_PORTRAIT -> 156.dp
    PersonalPhotoSize.SMALL -> 100.dp
}

/**
 * v427 — THE SHARE A PRINT TAKES BESIDE THE WRITING.
 *
 * The print-beside-a-line pair used a fixed column (0.42 / 0.58) whatever size
 * the print was, so the size the member picked for it changed nothing they could
 * see in that pair — the third face of "in stack those sizes options are not
 * accurate" (a Small beside the writing and a Small portrait beside it were the
 * same width). The column is the print's OWN share now, capped at half so the
 * writing always keeps at least half the measure: Small 44/56, Small portrait
 * 36/64, Half and up an even half.
 */
internal fun printBesideShare(size: PersonalPhotoSize): Float =
    size.fraction.coerceIn(0.30f, 0.50f)

// ── ONE ROW OF PRINTS (v396) ──────────────────────────────────────────────
//
// Consecutive prints used to pair up — two at a time, and only ever two. The
// member asked for the rest of the shapes a run of photographs actually takes:
// FOUR as a square, and THREE as one upright frame with the other two stacked
// beside it (user request: "a grid 2*2 but for 3 dont make it one big and all
// see how we can do portraight and then 2 small can be fit to the other side
// kinda stylish, but this should be auto adjust, but user can do any size
// chnages etc manually").
//
// The shape is AUTO, by how many prints arrived — it is what a page does when
// the member simply adds pictures one after another. Their own size keys still
// decide the things that are theirs to decide: PAGE takes a print out of a row
// entirely (a page-wide picture is its own row), and an UPRIGHT size claims the
// tall slot of a three. Everything else about a print is untouched.

/**
 * How many prints one row holds before the next row starts.
 *
 * v427 — `internal` for the same reason [PRINT_ROW_GAP] is: the sheet builds the
 * page's own rows, so it must not decide for itself how long a row may be.
 */
internal const val PRINT_ROW_LIMIT = 4

/**
 * v427 — IS THIS ROW JUST AIR BETWEEN TWO PRINTS?
 *
 * True for a blank text row that is nobody's place: no words, no voice note, the
 * caret is not in it and nothing is selected in it. A print run may step over
 * one of those, which is what lets two pictures that ended up a line apart come
 * back together as the row they were meant to be — and what keeps the run from
 * swallowing a line the member is actually writing in, or a blank line they just
 * put their caret in to type.
 *
 * The read view runs its own version of the rule (it has no caret and no
 * selection), so the two passes never disagree about which pictures are one row.
 */
private fun printRowGapSteppable(state: PersonalEditorState, id: String): Boolean {
    val block = state.block(id) ?: return false
    if (block.isPhoto || block.isAudio) return false
    // v428 — and the caret no longer stops it: air with nothing on it is air,
    // wherever the caret happens to be resting. Whether the line is then HIDDEN
    // with the row is the drawing pass's own decision (a line holding the caret
    // is drawn, see the row's `gaps`), so the two questions stay separate.
    return block.text.isBlank()
}

/**
 * The row's own gap — the pair's gap, shared by every shape.
 *
 * v427 — `internal`, because the SHEET lays a row out too (see
 * `PersonalExport.drawExportPrintRow`): a row on paper with a gap of its own is a
 * second drawing of the page's own row, which is exactly what the export is not
 * allowed to be.
 */
internal val PRINT_ROW_GAP = 8.dp

// A row's cells have no heights of their own any more (v400): each print takes
// ITS OWN size's height and its own size's share of the measure, so the sizes a
// member picks in a row are sizes they can see (see [PersonalPrintArrangement]).

/**
 * ONE ROW OF PRINTS, laid out by how many there are.
 *
 * The caller supplies the CELL — the editor draws a print it can caption, carry
 * and resize, the read view draws the print it always drew — so the two pages
 * cannot drift apart about where a picture sits. (A pair already cost one round
 * of exactly that: the read view drew each print on its own line while the
 * editor drew a row, so a pair came apart the moment the member stopped
 * writing.)
 */
@Composable
private fun PersonalPrintArrangement(
    ids: List<String>,
    sizeOf: (String) -> PersonalPhotoSize,
    cell: @Composable (id: String, slotHeight: Dp, modifier: Modifier) -> Unit
) {
    if (ids.size < 2) return
    // ── A CELL'S SHAPE IS ITS OWN SIZE (v400) ────────────────────────────
    //
    // The row used to hand every cell the SAME slot (a pair's even halves, a
    // square's four) and tell the cell to fill it, so choosing Small portrait or
    // Portrait for a print inside a row changed nothing the member could see —
    // the menu was there and the picture never answered it (user report: "in side
    // by side now i cant chnge its sizes like yes page size isnt possible but i
    // cant chnage between small portraifght etc in side by side now als same for 3
    // together, let the flixibility to pick differnt size of that photo").
    //
    // A cell's HEIGHT is now its own size's height and its WIDTH is its own
    // size's share of the measure, so a portrait print stands taller and narrower
    // than the print beside it and the row is the row the member built. The
    // SHAPES are unchanged (two, the upright frame with two stacked beside it,
    // four as a square): only the slot each print takes is now the print's own
    // answer. A PAGE print still leaves a row entirely — PAGE means the whole
    // measure of the page, which is the one size a row cannot hold.
    val heightOf: (String) -> Dp = { id -> personalPrintHeight(sizeOf(id)) }
    val weightOf: (String) -> Float = { id -> sizeOf(id).fraction.coerceIn(0.3f, 1f) }
    if (ids.size == 2) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PRINT_ROW_GAP),
            verticalAlignment = Alignment.Top
        ) {
            ids.forEach { id ->
                Box(Modifier.weight(weightOf(id))) {
                    cell(id, heightOf(id), Modifier.fillMaxWidth())
                }
            }
        }
        return
    }
    // A THREE. Whichever member asked to stand up — its own size said so —
    // takes the upright frame; with none asking, the first print does, so the
    // shape is the same shape either way and nothing has to be explained.
    if (ids.size == 3) {
        val tall = ids.firstOrNull { PersonalPhotoSize.isUpright(sizeOf(it)) } ?: ids[0]
        val stacked = ids.filterNot { it == tall }
        // v427 — THE FRAME LEADS, AND THE TWO STACKED KEEP THEIR OWN WIDTHS.
        //
        // The split used to be the frame's own fraction against the SUM of the
        // two stacked ones, which inverted the shape the three is named for: a
        // Portrait frame (0.54) beside Half + Small (0.62 + 0.44 = 1.06) came
        // out at 34 % of the measure, so the two "stacked beside it" were each
        // WIDER than the frame the member had asked to make the tall one
        // (member: "in stack those sizes options are not accurate, fix the size
        // accuracy in stacks of 2 3 4"). The frame now takes at least as much
        // room as the widest print beside it — so it reads as the frame of the
        // shape — and each stacked print takes its OWN share of its column
        // instead of the column's whole width, which is what made a Small and a
        // Half beside it come out the same size.
        val stackedShare = stacked.maxOf { weightOf(it) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PRINT_ROW_GAP),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.weight(maxOf(weightOf(tall), stackedShare))) {
                cell(tall, heightOf(tall), Modifier.fillMaxWidth())
            }
            Column(
                modifier = Modifier.weight(stackedShare),
                verticalArrangement = Arrangement.spacedBy(PRINT_ROW_GAP)
            ) {
                stacked.forEach { id ->
                    cell(
                        id,
                        heightOf(id),
                        Modifier.fillMaxWidth(weightOf(id) / stackedShare)
                    )
                }
            }
        }
        return
    }
    // FOUR — two lines of two. (The row's limit is four, so nothing longer
    // arrives; a page with more prints simply starts the next row.)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PRINT_ROW_GAP)
    ) {
        ids.chunked(2).forEach { line ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PRINT_ROW_GAP),
                verticalAlignment = Alignment.Top
            ) {
                line.forEach { id ->
                    Box(Modifier.weight(weightOf(id))) {
                        cell(id, heightOf(id), Modifier.fillMaxWidth())
                    }
                }
                if (line.size == 1) Spacer(Modifier.weight(1f))
            }
        }
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
    /**
     * v396 — THE HEIGHT THIS PRINT'S OWN ROW DECIDED FOR IT.
     *
     * A print on its own takes the height its size table gives it, which is
     * right: it is the only thing in its measure. A print in a ROW of two,
     * three or four is a CELL of a shape the row chose (see
     * [PersonalPrintArrangement]) — a pair's even halves, a tall frame that is
     * exactly the two stacked beside it, a quad's four squares — so the row
     * hands the cell its height instead of each print guessing at its own.
     */
    slotHeight: Dp? = null,
    ink: Color,
    accent: Color,
    enabled: Boolean,
    onCaption: (String) -> Unit,
    onSize: (PersonalPhotoSize) -> Unit,
    onRemove: () -> Unit,
    onOpen: (Rect?) -> Unit,

    // ── v401 — THE LABEL'S OWN SETTINGS ────────────────────────────────
    //
    // A caption is drawn where it is written, so the print is handed the label
    // it wears and reports back what the member picks in the caption's own dock.
    // Every one of these has a default (the print's own face, the print's own
    // size, no date, the app's order), which is exactly how a caption read
    // before this version — so a caller that knows nothing about labels, and
    // every note written before today, draws the same print it always did.
    captionDate: Long = 0L,
    captionFace: String = "",
    captionSizeKey: String = "",
    captionOrder: String = "",
    /** The field is being written in (or has just been left) — the page uses
     *  this to swap the dock to the caption's own tools. */
    onCaptionFocus: (Boolean) -> Unit = {},
    onCaptionDate: (Long) -> Unit = {},
    onCaptionFace: (String) -> Unit = {},
    onCaptionSize: (String) -> Unit = {},
    onCaptionOrder: (String) -> Unit = {}
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
    val imageHeight = slotHeight ?: personalPrintHeight(size)
    val captionSize = when (size) {
        PersonalPhotoSize.PAGE -> 13.sp
        PersonalPhotoSize.HALF -> 12.sp
        PersonalPhotoSize.PORTRAIT -> 12.sp
        PersonalPhotoSize.SMALL_PORTRAIT -> 11.sp
        PersonalPhotoSize.SMALL -> 10.sp
    }
    // v401 — the label's own face, its own size, and its date.
    //
    // The order is asked for the caption (its own override, else the app's) and
    // read HERE in the composition: `PersonalCaptionDates` holds the app-wide
    // order in snapshot state, so re-ordering the app re-writes every label that
    // did not override it while this print sits on screen.
    val labelContext = LocalContext.current
    val labelFace = personalCaptionFace(captionFace)
    val labelSize = personalCaptionSizeSp(captionSize, captionSizeKey)
    val labelDate = personalCaptionDateText(
        captionDate,
        PersonalCaptionDates.order(labelContext, captionOrder)
    )
    Column(
        modifier = Modifier
            // A FRACTION of the wrapper's width, so the print can never be
            // wider than the words it sits among.
            .fillMaxWidth(if (paired) 1f else size.fraction)
            // Shadow BEFORE the fill, and the fill OPAQUE — a translucent one
            // lets the shadow bleed through the print (see AGENTS rule 11).
            .shadow(5.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            // v411 — the journal's own paper: a print and the page it is
            // pasted on are the same paper (see journalPaper).
            .background(journalPaper())
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
            // v401 — THE LABEL: the caption's words, then the date on a line of
            // its own under them (asked and answered: "its own line, always
            // formatted live"). The date is a LINE and not a word in the caption
            // because the caption is the member's sentence and the date is the
            // print's stamp — and a stamp the app writes is the only kind that
            // can be re-ordered when the app-wide order changes.
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (enabled) {
                    BasicTextField(
                        value = caption,
                        onValueChange = onCaption,
                        singleLine = true,
                        textStyle = TextStyle(
                            textAlign = TextAlign.Center,
                            fontFamily = labelFace.family,
                            fontSize = labelSize,
                            color = ink.copy(alpha = 0.72f)
                        ),
                        cursorBrush = SolidColor(personalAccentInk()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            // The field says when it is being written in, which
                            // is what swaps the dock to the caption's own tools
                            // (see PersonalToolDock and captionFocusedId).
                            .onFocusChanged { onCaptionFocus(it.isFocused) },
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.Center) {
                                if (caption.isEmpty()) {
                                    Text(
                                        "Add a caption",
                                        style = TextStyle(
                                            fontFamily = labelFace.family,
                                            fontSize = labelSize,
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
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            fontFamily = labelFace.family,
                            fontSize = labelSize,
                            color = ink.copy(alpha = 0.72f)
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (labelDate.isNotEmpty()) {
                    Text(
                        labelDate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 1.dp, bottom = 2.dp),
                        style = TextStyle(
                            fontFamily = labelFace.family,
                            // The stamp is the label's small print, so it rides
                            // the label's own size instead of a size of its own.
                            fontSize = (labelSize.value * 0.76f).sp,
                            color = ink.copy(alpha = 0.44f),
                            letterSpacing = 0.6.sp,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
                        expanded = sizeMenu.open,
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
    onTitlePosition: ((
        id: String,
        label: String,
        top: Float,
        bottom: Float,
        writing: Boolean,
        scroll: Float
    ) -> Unit)? = null
) {
    // The page's own reporter, or the host's — see [LocalPersonalTitleReport].
    val titleReport = onTitlePosition ?: LocalPersonalTitleReport.current
    // A read view's scroll, offered through the local so a pinned heading can be
    // tapped from the READING side too. Read HERE (a composition local cannot be
    // asked for inside a callback), and the STATE is what travels — so the
    // callback still sees the live value when it runs.
    val pinHolder = LocalPersonalPinScrollHolder.current
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
    // v389g — every print after the first of a row is MARKED here, so the
    // drawing pass knows it has already been drawn as a cell of the row above it
    // (the editor's pass does the same; the read view's used to just step over a
    // pair, which is why it came apart when the page was read back).
    //
    // v396 — AND A ROW IS TWO, THREE OR FOUR PRINTS now, the same shapes the
    // editor builds, chosen by how many arrived.
    val groupSkips = mutableSetOf<String>()
    val printRows = mutableMapOf<String, List<String>>()
    run {
        val blocks = doc.blocks
        var i = 0
        while (i < blocks.size) {
            val first = blocks[i]
            // v403 — the read view runs the editor's own rule, PAGE prints
            // included: the two passes must never disagree about which prints
            // are one row (see the editor's pass for why the rule changed).
            if (first.isPhoto) {
                val run = ArrayList<String>()
                // v427 — the same AIR rule as the editor's pass (see there): a
                // run steps over the blank lines between its prints, so the two
                // passes keep agreeing about which pictures are one row.
                val gaps = ArrayList<String>()
                var j = i
                while (j < blocks.size && run.size < PRINT_ROW_LIMIT) {
                    val member = blocks[j]
                    if (member.isPhoto) {
                        run.add(member.id)
                        j += 1
                    } else if (!member.isAudio && member.text.isBlank()) {
                        gaps.add(member.id)
                        j += 1
                    } else {
                        break
                    }
                }
                if (run.size > 1) {
                    printRows[run.first()] = run
                    run.drop(1).forEach { groupSkips.add(it) }
                    gaps.forEach { groupSkips.add(it) }
                    i = j
                    continue
                }
                val next = blocks.getOrNull(i + 1)
                if (next != null &&
                    PersonalPhotoSize.fromKey(first.photoSize) == PersonalPhotoSize.SMALL &&
                    !next.isPhoto && !next.isAudio && next.text.isNotBlank()
                ) {
                    besideSkips.add(next.id)
                    i += 2
                    continue
                }
            }
            i += 1
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
            // v402 — A LINE THAT STOPS BEING A TITLE STOPS BEING A PLACE (see
            // LocalPersonalTitleReport). Taking the flag off a line — or losing
            // the line itself — clears its entry, so the pinned bar can never go
            // on naming a heading that is not there. The side is FALSE: this is
            // the reading half of the page.
            if (titleReport != null) {
                DisposableEffect(isTitle, block.id) {
                    onDispose { titleReport?.invoke(block.id, "", 0f, 0f, false, 0f) }
                }
            }
            val isSmall = personalBlockCarries(text, mask, FLAG_SMALL)
            val isBullet = personalBlockCarries(text, mask, FLAG_BULLET)
            // v393/v393e — the read view's half of the same rule: a row with no
            // WORDS on a to-do page (the read view knows the page by its
            // [rowSize]) still draws its box, so a list re-opened after emptying
            // a row looks like the page that was written. Blank rather than
            // perfectly empty, so a row left holding one stray space keeps the
            // box its writer still sees in the editor.
            val isCheckbox = personalBlockCarries(text, mask, FLAG_CHECKBOX) ||
                (text.isBlank() && rowSize.isSpecified)
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
                                    val barWidth = QUOTE_RULE_WIDTH.toPx()
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
                                .padding(start = QUOTE_LEAD)
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
                                val bounds = coordinates.boundsInWindow()
                                titleReport(
                                    block.id,
                                    text,
                                    bounds.top,
                                    bounds.bottom,
                                    false,
                                    pinHolder?.scroll?.value?.toFloat() ?: 0f
                                )
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
                val linkText = personalAnnotateLinks(baseText, personalQuoteDeepColor())
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
                            lineHeight = TITLE_VIEW_LINE,
                            fontWeight = FontWeight.SemiBold,
                            // v427 — the read view sets its ink HERE as well as in
                            // the annotated text: a style with no colour leaves a
                            // word to `LocalContentColor`, which is Compose's
                            // black outside a `Surface` — the eye view's own
                            // "dark black texts" (see [personalAnnotated]).
                            color = ink,
                            textAlign = alignOf
                        )
                        isSmall -> TextStyle(
                            fontFamily = WritingFontFamily,
                            fontSize = SMALL_VIEW_SIZE,
                            lineHeight = SMALL_VIEW_LINE,
                            color = ink,
                            textAlign = alignOf
                        )
                        else -> TextStyle(
                            fontFamily = WritingFontFamily,
                            fontSize = if (rowSize.isSpecified) rowSize else BODY_VIEW_SIZE,
                            lineHeight = if (rowSize.isSpecified) {
                                rowSize * 1.7f
                            } else {
                                BODY_VIEW_LINE
                            },
                            color = ink,
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
    val renderPrint: @Composable (PersonalBlock, Dp?, Modifier) -> Unit = { block, slot, width ->
        var bounds by remember(block.photo) { mutableStateOf<Rect?>(null) }
        val printSize = PersonalPhotoSize.fromKey(block.photoSize)
        Column(
            modifier = width
                .onGloballyPositioned { bounds = it.boundsInWindow() }
                .shadow(5.dp, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(journalPaper())
                .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 2.dp)
                .clickable { onOpenPhoto(block.photo.orEmpty(), bounds) }
        ) {
            PersonalPagePhoto(
                uri = block.photo.orEmpty(),
                // The row's cell height when this print is part of a row (see
                // [PersonalPrintArrangement]), otherwise its own size's.
                height = slot ?: personalPrintHeight(printSize)
            )
            // ── v400 — THE WIDE BOTTOM BORDER, ALWAYS DRAWN ────────────────
            //
            // A print is the picture AND the band of paper under it that says
            // what the picture is (see PersonalPhotoBlock, which owns the shape).
            // The read view drew that band only when a caption had already been
            // written — so a page read back was a SHORTER page than the one that
            // was written, and a row of prints measured differently on the two
            // sides of the switch (which is what made a three look collapsed in
            // the eye) (user report: "keep the buttom strip here i write caption
            // for them even if theres no captaion keep it in preview").
            //
            // The empty band carries a non-breaking space rather than nothing, so
            // it always takes its own line's height.
            // v401 — the label, read back exactly as it was written: its own
            // face, its own size, and its date on its own line under the words.
            val readContext = LocalContext.current
            val readFace = personalCaptionFace(block.captionFace)
            val readSize = personalCaptionSizeSp(CAPTION_VIEW_SIZE, block.captionSize)
            val readDate = personalCaptionDateText(
                block.captionDateMillis,
                PersonalCaptionDates.order(readContext, block.captionOrder)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, bottom = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    block.caption.ifBlank { "\u00A0" },
                    style = TextStyle(
                        fontFamily = readFace.family,
                        fontSize = readSize,
                        color = ink.copy(alpha = 0.62f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (readDate.isNotEmpty()) {
                    Text(
                        readDate,
                        style = TextStyle(
                            fontFamily = readFace.family,
                            fontSize = (readSize.value * 0.76f).sp,
                            color = ink.copy(alpha = 0.40f),
                            letterSpacing = 0.6.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(VIEW_ROW_GAP)) {
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
                val printRow = printRows[block.id]
                val besideLine = nextPhoto?.takeIf {
                    nextPhotoId != null && besideSkips.contains(it.id)
                }
                when {
                    // A cell of a row the print before it already drew.
                    groupSkips.contains(block.id) -> Unit
                    printRow != null -> PersonalPrintArrangement(
                        ids = printRow,
                        sizeOf = { id ->
                            PersonalPhotoSize.fromKey(
                                doc.blocks.firstOrNull { it.id == id }?.photoSize
                            )
                        }
                    ) { id, slotHeight, cellModifier ->
                        val member = doc.blocks.firstOrNull { it.id == id }
                        if (member != null) renderPrint(member, slotHeight, cellModifier)
                    }
                    besideLine != null -> Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // v427 — the print's own share of the row, exactly as
                        // the editor splits it (see [printBesideShare]).
                        val printShare = printBesideShare(
                            PersonalPhotoSize.fromKey(block.photoSize)
                        )
                        Box(Modifier.weight(printShare)) {
                            renderPrint(block, null, Modifier.fillMaxWidth())
                        }
                        Box(Modifier.weight(1f - printShare)) {
                            // A quote's panels reach into the gap above and below
                            // to meet their quoted neighbour, which is the column's
                            // business, not a pair's — the line beside a print
                            // draws with its own edges square.
                            renderLine(index + 1, besideLine, false, false)
                        }
                    }
                    else -> {
                        val printSize = PersonalPhotoSize.fromKey(block.photoSize)
                        renderPrint(block, null, Modifier.fillMaxWidth(printSize.fraction))
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
                    accent = accent,
                    // v421 — the look the note was given, and no picker: a
                    // read-only view renders a document, it does not edit one.
                    style = PersonalVoiceStyle.fromKey(block.audioStyle)
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
/**
 * v427 — THE PAGE'S TEXT BAR.
 *
 * What the dock's copy door opens now (see [PersonalEditorState.pageEditBarOpen]
 * and [PersonalToolDock]): the page's own cut / copy / paste / undo, with the
 * arrows that say HOW MUCH of the page is in hand.
 *
 * The flow is the member's own, in their words: "cut copy paste starts a
 * selection with arrow tools to go how much select then action done with cut
 * copy or paste, extra select all button as well". So Cut or Copy with nothing
 * picked does not act — it OFFERS the whole page and lets the arrows trim the
 * reach ("◀ 4 of 12 rows ▶"), and the second tap is the one that does it. All
 * rows is the one-tap select-everything the member also asked for, and Done puts
 * the writing tools back.
 *
 * v427 — AND THE SWITCH IS GONE (member: "proper arrow up down left right arrow
 * and no more letter row option but the arrows do the work ... dont let user
 * select things starting from bottom" → "let user select things from bottom
 * proper tool of how it should behave"). The bar is now FOUR ARROWS on TWO
 * AXES, each axis beside the thing it counts:
 *
 *  · ↑ ↓ are the rows, and they are built FROM THE MEMBER'S OWN ROW — the caret's
 *    when the caret is on the page, else the page's foot — so a selection can
 *    start at the bottom of the page and climb. The arrow pressed FIRST sets
 *    which way the reach grows; the other one gives a row back, and at a single
 *    row it walks. See [PersonalEditorState.nudgePageRows].
 *  · ← → are the letters, and they SWITCH THEMSELVES ON: the first press opens a
 *    one-character window in that row, after which more extends it and less
 *    takes a character back. See [PersonalEditorState.nudgePageLetters].
 *  · The count is one line — "3 of 12 rows" and, only once reached into, "· 5 of
 *    24 letters" — so "4 of 12" is never ambiguous about what four of twelve.
 *  · Every arrow DIMS when its axis has nowhere to go, because a control that
 *    answers a press with nothing teaches a member to stop pressing it.
 *
 * Undo is the bar's own last actions (see the state's own note) — it is not a
 * keystroke undo, and nothing here pretends it is.
 */
@Composable
private fun PersonalPageEditBar(
    state: PersonalEditorState,
    accent: Color,
    ink: Color
) {
    val clipboard = LocalClipboardManager.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val picked = state.pageSelectionCount
    val rows = state.blockIds.size
    // v427 — THE REACH HAS TWO AXES AND NO MODE (see the state's own note on
    // [PersonalEditorState.nudgePageRows]). ↑ ↓ are the ROWS, ← → are the
    // LETTERS, each pair sits with the thing it counts, and the letters switch
    // themselves on the moment ← or → is pressed — so the bar never has to be
    // told which unit the member means before an arrow can tell them anything.
    val lettersPicked = state.pageLettersPicked
    val letterCount = state.pageLetterCount
    val letterTotal = state.pageLetterTotal
    val lettersHere = picked > 0 && letterTotal > 0
    // The reach as one sentence: the rows, then — only once they have been
    // reached into — the letters inside them. A count that is not true is worse
    // than no count, so the letter half appears when it has something to say.
    // v428 — THE TEXT BAR'S TWO ROWS REMEMBER THEIR PLACE. Same reason as the
    // dock's tool row: a reach is built by pressing the same arrow again and
    // again, and a row that snapped back to the left between presses made the
    // member find their arrow a second time.
    val rowScroll = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val letterScroll = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val reachLine = when {
        picked == 0 -> "Nothing picked"
        lettersPicked -> "$picked of $rows rows · $letterCount of $letterTotal letters"
        else -> "$picked of $rows rows"
    }
    val hasReach = picked > 0
    // Cut and Copy with an empty reach OFFER the whole page first — the
    // member's two-tap flow: the first tap says how much, the second acts.
    val offerReach: () -> Unit = { state.selectWholePage() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // ── THE ROWS: ↑ MORE · ↓ MORE, OR ONE STEP BACK ──────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rowScroll),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Proper arrows, not triangles: the same icon language as the rest
            // of the app, and an arrow that is dead now LOOKS dead.
            PageArrowChip(
                icon = CurioIcons.ArrowUpward,
                label = "More rows up",
                accent = accent,
                enabled = state.canNudgePageRows(up = true)
            ) { state.nudgePageRows(up = true) }
            PageArrowChip(
                icon = CurioIcons.ArrowDownward,
                label = "More rows down",
                accent = accent,
                enabled = state.canNudgePageRows(up = false)
            ) { state.nudgePageRows(up = false) }
            Text(
                text = reachLine,
                style = MaterialTheme.typography.labelSmall,
                color = if (picked > 0) ink else muted,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            PageTextChip("All rows", accent = ink) { state.selectWholePage() }
            PageTextChip("Done", accent = accent) { state.closePageEditBar() }
        }
        // ── THE LETTERS: ← ONE BACK · → ONE MORE ─────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(letterScroll),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            PageArrowChip(
                icon = CurioIcons.ArrowBack,
                label = "Fewer letters",
                accent = accent,
                enabled = state.canNudgePageLetters(more = false)
            ) { state.nudgePageLetters(more = false) }
            PageArrowChip(
                icon = CurioIcons.ArrowForward,
                label = "One more letter",
                accent = accent,
                enabled = state.canNudgePageLetters(more = true)
            ) { state.nudgePageLetters(more = true) }
            // The whole line in one tap — the counterpart of All rows, for the
            // member who wants the line rather than a clause out of it.
            PageTextChip("Line", enabled = lettersHere, accent = ink) {
                state.selectPageLineLetters()
            }
            PageTextChip("Cut", enabled = true, accent = accent) {
                if (!hasReach) {
                    offerReach()
                } else {
                    val text = state.cutPageSelection()
                    if (text.isNotEmpty()) clipboard.setText(AnnotatedString(text))
                }
            }
            PageTextChip("Copy", enabled = true, accent = accent) {
                if (!hasReach) {
                    offerReach()
                } else {
                    val text = state.copyPageSelection()
                    if (text.isNotEmpty()) {
                        clipboard.setText(AnnotatedString(text))
                        state.closePageEditBar()
                    }
                }
            }
            PageTextChip("Paste", accent = accent) {
                val text = clipboard.getText()?.text.orEmpty()
                if (text.isNotEmpty()) state.pastePageText(text)
            }
            PageTextChip("Undo", enabled = state.canUndoPageEdit, accent = accent) {
                state.undoPageEdit()
            }
        }
    }
}

/** One arrow of [PersonalPageEditBar] — an icon in a round tap target, tinted
 *  with the page's own ink, dimmed to the point of being plainly unavailable
 *  when its axis has nowhere left to go. */
@Composable
private fun PageArrowChip(
    icon: String,
    label: String,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(
            icon,
            contentDescription = label,
            tint = if (enabled) accent
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            size = 18.dp
        )
    }
}

/** One word of [PersonalPageEditBar] — a bare label in the page's own inks, so
 *  the bar reads as the dock's language (a row of small words) rather than as a
 *  second toolbar with its own furniture. */
@Composable
private fun PageTextChip(
    label: String,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = if (enabled) accent
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp)
    )
}

@Composable
internal fun PersonalToolDock(
    state: PersonalEditorState,
    onPickPhoto: () -> Unit,
    showJournalTools: Boolean = true,
    /**
     * v428 — THE PAGE'S OWN COLOUR (see [JournalAccentSheet]), offered only when
     * the page has somewhere to keep it: null draws no door at all.
     */
    journalAccent: Int = JOURNAL_ACCENT_THEME,
    onJournalAccent: ((Int) -> Unit)? = null,
    /**
     * v429 — WHETHER THE PAGE'S PAPER TAKES THE COLOUR TOO (see
     * [JournalPagePaint]). Stored with the page like the colour, and offered in
     * the same sheet; null draws no switch, exactly as a null [onJournalAccent]
     * draws no palette door.
     */
    journalPagePainted: Boolean = false,
    onJournalPagePainted: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    surface: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    val active = state.activeFlags()
    // The journal's own colour, as a door rather than a swatch: the button
    // WEARS the colour the page is wearing (its own when it has one, else the
    // app's accent ink), which is what makes the palette read as "this journal's
    // colour" instead of "a palette".
    var accentOpen by remember { mutableStateOf(false) }
    if (accentOpen && onJournalAccent != null) {
        JournalAccentSheet(
            current = journalAccent,
            onPick = onJournalAccent,
            painted = journalPagePainted,
            onPainted = onJournalPagePainted,
            onDismiss = { accentOpen = false }
        )
    }
    // ── v424 — AND THE PAGE'S EXPORT DOOR ───────────────────────────
    //
    // Resolved here rather than inside the tool, because the file a page leaves
    // as has to wear THIS page's paper, ink and pens (see [PersonalExport]).
    val exporter = rememberPersonalExporter(state)
    // The dock wears the app's own accent (the same one Home's hero uses), not
    // a hard rose — a member on the azure/hero-lane theme sees THEIR accent.
    val accent = personalAccent()
    val accentInk = personalAccentInk()
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    // v428 — THE TOOL ROW REMEMBERS WHERE IT WAS LEFT. A row of tools wider
    // than a phone is scrolled to reach the last of them, and the state used to
    // be `rememberScrollState()` INSIDE the row — so the moment the dock swapped
    // to the page's text bar and back (or the page was opened again) the row
    // snapped to its first tool and the member had to scroll for the same tool
    // twice. Hoisted above the dock's own bar swap and made saveable, so its
    // place survives the swap and a rotation alike.
    val toolScroll = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
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
        // ── v401 — WHOSE DOCK IS THIS? ──────────────────────────────────
        //
        // The dock follows whatever is being written in. While a PRINT'S
        // CAPTION has the caret the tools below are the ones that caption can
        // use — its words, its face, its size and its date — and the writing
        // tools (bold, marker, bullet, alignment…) are put away, because not one
        // of them can act on a caption and a row of buttons that do nothing is
        // worse than no row at all (user request: "only show those tools hen ive
        // caption opened and hide other tools which the caption doesnt support,
        // and make the tools appear back when i go back to writin gin canvas
        // smoothly"). The swap is a CROSSFADE, not a cut, so the dock does not
        // blink between two toolbars — it changes its mind in place.
        //
        // The caption owns a print, and a print can own a caption in any of the
        // page's shapes (its own, beside the words, or a cell of a row), so this
        // is keyed on the caption field itself (see captionFocusedId), not on
        // where on the page the print happens to sit.
        Crossfade(
            targetState = state.captionFocusedId,
            animationSpec = tween(durationMillis = 180),
            label = "personalDockTools"
        ) { writingCaptionId ->
        // v427 — THE PAGE'S TEXT BAR takes the dock's own row while it is open:
        // the tools the member is NOT using step aside for the ones they just
        // asked for, in the place their thumb already is (see
        // [PersonalPageEditBar]). A caption still wins while its caret is in it.
        if (state.pageEditBarOpen) {
            PersonalPageEditBar(state = state, accent = accentInk, ink = ink)
        } else if (writingCaptionId != null) {
            PersonalCaptionTools(
                state = state,
                captionId = writingCaptionId,
                accent = accentInk,
                ink = ink
            )
        } else {
        Row(
            modifier = Modifier
                // Nine tools in a fixed row overflowed a narrow phone and cut
                // the last icons in half; the row scrolls, so every tool is
                // always reachable and nothing is clipped — and it keeps its
                // place (see [toolScroll]).
                .horizontalScroll(toolScroll)
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
            // v428 — THIS JOURNAL'S COLOUR, right beside the history tool: both
            // are facts about the PAGE rather than about the line the caret is
            // on, and a member who wants to change how their journal looks looks
            // here first. The door carries the page's own colours (its fill is
            // its paper, its glyph the colour in hand), so the dock says what
            // the page is wearing before it is opened.
            if (onJournalAccent != null) {
                val worn = if (journalAccent == JOURNAL_ACCENT_THEME) accentInk
                           else Color(journalAccent)
                PersonalToolButton(
                    label = "This journal's colour",
                    active = journalAccent != JOURNAL_ACCENT_THEME,
                    accent = worn, ink = ink,
                    onClick = { accentOpen = true }
                ) {
                    CurioIcon(CurioIcons.Palette, null, tint = worn, size = 19.dp)
                }
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
            // ── THE ALIGNMENT: ONE TOOL, FOUR CHOICES (v428) ────────────
            //
            // Four buttons (left / centre / right / justify) held four slots in
            // this row, each saying one quarter of the same thing. The member:
            // *"collapse the alignments into one option"* — so there is ONE
            // alignment tool now, wearing the FOCUSED LINE's own alignment (the
            // button answers "what is this line doing?" before it is touched),
            // with the four choices behind it in the tap-then-menu habit the
            // bullet tool already teaches. It is lit for anything but plain
            // left, so a line moved off the margin says so in the dock.
            Box {
                val alignMenu = remember { CurioMenuToggle() }
                val choices = remember {
                    listOf(
                        PersonalAlign.START to "Align left",
                        PersonalAlign.CENTER to "Align centre",
                        PersonalAlign.END to "Align right",
                        PersonalAlign.JUSTIFY to "Justify"
                    )
                }
                val focusedAlign = state.alignOfFocused()
                PersonalToolButton(
                    label = "Alignment",
                    active = focusedAlign != PersonalAlign.START,
                    accent = accentInk, ink = ink,
                    onClick = { alignMenu.buttonClick() }
                ) {
                    AlignGlyph(focusedAlign.toAlignKind())
                }
                DropdownMenu(
                    expanded = alignMenu.open,
                    onDismissRequest = { alignMenu.dismissed() },
                    properties = MenuKeepKeyboardProperties
                ) {
                    choices.forEach { (align, label) ->
                        DropdownMenuItem(
                            text = { MarkerMenuLabel(label) },
                            leadingIcon = { AlignGlyph(align.toAlignKind()) },
                            trailingIcon = {
                                if (focusedAlign == align) {
                                    CurioIcon(CurioIcons.Check, null, tint = accentInk, size = 18.dp)
                                }
                            },
                            onClick = {
                                state.setAlign(align)
                                alignMenu.close()
                            }
                        )
                    }
                }
            }
            if (showJournalTools) PersonalToolButton(
                label = "Add a photo",
                active = false,
                accent = accentInk, ink = ink,
                onClick = onPickPhoto
            ) {
                CurioIcon(CurioIcons.Image, null, size = 18.dp)
            }
            // ── v424 — THE PAGE'S OWN TWO TOOLS ──────────────────────────
            //
            // COPY is the PAGE's copy, not the line's: it hands the request to
            // the canvas, which selects the whole page and raises Android's own
            // floating bar above these tools (see the canvas's own effect). One
            // bar for copy and select all, in the place a member already
            // expects to find them.
            PersonalToolButton(
                label = "Page text tools: cut, copy, paste, undo",
                // v427 — the door opens the page's own TEXT BAR (see
                // [PersonalPageEditBar]) instead of Android's select-all menu:
                // the member asked for cut, copy, paste and undo on the page.
                active = state.pageEditBarOpen,
                accent = accentInk, ink = ink,
                onClick = { state.togglePageEditBar() }
            ) {
                CurioIcon(CurioIcons.ContentCopy, null, size = 18.dp)
            }
            // EXPORT takes the page out as a file in its own look — the PDF the
            // member asked for, its words, or Markdown that keeps the styling
            // (see [PersonalExport]). Three shapes behind one door, because the
            // question "what am I exporting as" is asked once, when the file is
            // made, and never again.
            Box {
                val exportMenu = remember { CurioMenuToggle() }
                PersonalToolButton(
                    label = "Export this page",
                    active = exporter.busy,
                    accent = accentInk, ink = ink,
                    onClick = { exportMenu.buttonClick() }
                ) {
                    CurioIcon(CurioIcons.Download, null, size = 18.dp)
                }
                DropdownMenu(
                    expanded = exportMenu.open,
                    onDismissRequest = { exportMenu.dismissed() },
                    properties = MenuKeepKeyboardProperties
                ) {
                    PersonalExportFormat.entries.forEach { format ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    format.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                exportMenu.close()
                                exporter.run(format)
                            }
                        )
                    }
                }
            }
        }
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
    // A file that could not be written says so, rather than leaving the member
    // wondering whether the page went anywhere.
    val exportFailure = exporter.failure
    if (exportFailure != null) {
        AlertDialog(
            onDismissRequest = exporter.dismissFailure,
            title = { Text("Could not export") },
            text = { Text(exportFailure) },
            confirmButton = {
                TextButton(onClick = exporter.dismissFailure) { Text("OK") }
            }
        )
    }
}

/**
 * v401 — THE CAPTION'S OWN DOCK.
 *
 * The tools a label can use, and nothing else: the date it stamps, the face it
 * is written in and the size it is written at. It rides in the same pill as the
 * writing dock (see PersonalToolDock's Crossfade), so the member never leaves
 * the page to change the paper — the strip under the photograph is edited from
 * the strip they are already typing in.
 *
 * The BUTTONS wear what the label wears: the face button shows "Aa" set in the
 * face that is chosen, and the size button an "A" at that size, which is the
 * same trick the writing dock's own font menu uses (a menu written in one font
 * is a list of words).
 */
@Composable
private fun PersonalCaptionTools(
    state: PersonalEditorState,
    captionId: String,
    accent: Color,
    ink: Color
) {
    val context = LocalContext.current
    val date = state.captionDate(captionId)
    val face = state.captionFace(captionId)
    val sizeKey = state.captionSizeKey(captionId)
    val orderKey = state.captionOrder(captionId)
    val appOrder = PersonalCaptionDates.order(context)
    // v428 — and the caption's row keeps its place too (see the dock's note).
    val captionScroll = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    Row(
        modifier = Modifier
            .horizontalScroll(captionScroll)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // ── THE DATE ────────────────────────────────────────────────────
        //
        // First tap stamps TODAY — the common case, one tap, the same manner as
        // the marker and the bullet tool. The tap after that opens the menu,
        // which is where the day is changed and where the ORDER lives
        // (asked and answered: the order is set BOTH ways — this print's own,
        // and the one every caption follows).
        Box {
            val dateMenu = remember { CurioMenuToggle() }
            val dateSet = date > 0L
            PersonalToolButton(
                label = if (dateSet) "Date: ${personalCaptionDateText(date, appOrder)}"
                else "Add the date",
                active = dateSet,
                accent = accent, ink = ink,
                onClick = {
                    if (dateSet) dateMenu.buttonClick()
                    else state.setCaptionDate(captionId, personalCaptionToday())
                }
            ) {
                CurioIcon(CurioIcons.CalendarToday, null, size = 19.dp)
            }
            DropdownMenu(
                expanded = dateMenu.open,
                onDismissRequest = { dateMenu.dismissed() },
                properties = MenuKeepKeyboardProperties
            ) {
                val today = personalCaptionToday()
                DropdownMenuItem(
                    text = { CaptionMenuItem("Today") },
                    trailingIcon = {
                        if (date == today) {
                            CurioIcon(CurioIcons.Check, null, tint = accent, size = 18.dp)
                        }
                    },
                    onClick = {
                        state.setCaptionDate(captionId, today)
                        dateMenu.close()
                    }
                )
                DropdownMenuItem(
                    text = { CaptionMenuItem("Yesterday") },
                    onClick = {
                        state.setCaptionDate(captionId, personalCaptionDaysAgo(1))
                        dateMenu.close()
                    }
                )
                DropdownMenuItem(
                    text = { CaptionMenuItem("Remove the date") },
                    leadingIcon = { CurioIcon(CurioIcons.Close, null, tint = ink, size = 18.dp) },
                    trailingIcon = {
                        if (!dateSet) CurioIcon(CurioIcons.Check, null, tint = accent, size = 18.dp)
                    },
                    onClick = {
                        state.setCaptionDate(captionId, 0L)
                        dateMenu.close()
                    }
                )
                CurioMenuItemCaption("This print's order")
                DropdownMenuItem(
                    text = { CaptionMenuItem("Follow the app's order") },
                    trailingIcon = {
                        if (orderKey.isEmpty()) {
                            CurioIcon(CurioIcons.Check, null, tint = accent, size = 18.dp)
                        }
                    },
                    onClick = {
                        state.setCaptionOrder(captionId, "")
                        dateMenu.close()
                    }
                )
                PersonalCaptionDateOrder.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { CaptionMenuItem("${option.label}  ${option.hint}") },
                        trailingIcon = {
                            if (orderKey == option.key) {
                                CurioIcon(CurioIcons.Check, null, tint = accent, size = 18.dp)
                            }
                        },
                        onClick = {
                            state.setCaptionOrder(captionId, option.key)
                            dateMenu.close()
                        }
                    )
                }
                CurioMenuItemCaption("Every caption")
                PersonalCaptionDateOrder.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { CaptionMenuItem(option.label) },
                        trailingIcon = {
                            if (orderKey.isEmpty() && appOrder == option) {
                                CurioIcon(CurioIcons.Check, null, tint = accent, size = 18.dp)
                            }
                        },
                        onClick = {
                            // The app-wide order is what every caption that has
                            // not spoken for itself reads — so this one line
                            // re-writes the whole album's labels, live.
                            PersonalCaptionDates.setOrder(context, option)
                            dateMenu.close()
                        }
                    )
                }
            }
        }
        // ── THE FACE ────────────────────────────────────────────────────
        Box {
            val faceMenu = remember { CurioMenuToggle() }
            PersonalToolButton(
                label = "Caption face: ${personalCaptionFace(face).label}",
                active = face.isNotEmpty(),
                accent = accent, ink = ink,
                onClick = { faceMenu.buttonClick() }
            ) {
                CaptionFaceGlyph(personalCaptionFace(face))
            }
            DropdownMenu(
                expanded = faceMenu.open,
                onDismissRequest = { faceMenu.dismissed() },
                properties = MenuKeepKeyboardProperties
            ) {
                PersonalCaptionFace.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            // Set in the face it offers — the menu and the label
                            // are the same bytes.
                            Text(
                                option.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = option.family
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingIcon = {
                            if (personalCaptionFace(face) == option) {
                                CurioIcon(CurioIcons.Check, null, tint = accent, size = 18.dp)
                            }
                        },
                        onClick = {
                            state.setCaptionFace(captionId, option.key)
                            faceMenu.close()
                        }
                    )
                }
            }
        }
        // ── THE SIZE ────────────────────────────────────────────────────
        Box {
            val sizeMenu = remember { CurioMenuToggle() }
            PersonalToolButton(
                label = "Caption size: ${personalCaptionLabelSize(sizeKey).label}",
                active = sizeKey.isNotEmpty(),
                accent = accent, ink = ink,
                onClick = { sizeMenu.buttonClick() }
            ) {
                CaptionSizeGlyph(personalCaptionLabelSize(sizeKey))
            }
            DropdownMenu(
                expanded = sizeMenu.open,
                onDismissRequest = { sizeMenu.dismissed() },
                properties = MenuKeepKeyboardProperties
            ) {
                PersonalCaptionLabelSize.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingIcon = {
                            if (personalCaptionLabelSize(sizeKey) == option) {
                                CurioIcon(CurioIcons.Check, null, tint = accent, size = 18.dp)
                            }
                        },
                        onClick = {
                            state.setCaptionSize(captionId, option.key)
                            sizeMenu.close()
                        }
                    )
                }
            }
        }
        // ── BACK TO THE WORDS ───────────────────────────────────────────
        //
        // A caption is a page's smallest thing, and the way out of it is not
        // obvious (tap the prose, in the right place, and the keyboard follows).
        // This puts the writing tools back in the member's hand in one tap, so
        // nothing about the label is a trap.
        PersonalToolButton(
            label = "Writing tools",
            active = false,
            accent = accent, ink = ink,
            onClick = { state.setCaptionFocus(null) }
        ) {
            CurioIcon(CurioIcons.Edit, null, size = 18.dp)
        }
    }
}

/** A caption-tool menu row: one line of the label's own vocabulary. */
@Composable
private fun CaptionMenuItem(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = WritingFontFamily),
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** A menu's own heading — the caption tool menus carry two groups (this print,
 *  and every caption), and a heading is what keeps them apart. */
@Composable
private fun CurioMenuItemCaption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = WritingFontFamily),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 2.dp)
    )
}

/** The face button's own glyph: "Aa", set in the face it is offering. */
@Composable
private fun CaptionFaceGlyph(face: PersonalCaptionFace) {
    Text(
        "Aa",
        style = TextStyle(
            fontFamily = face.family,
            fontSize = 15.sp,
            color = LocalContentColor.current
        ),
        maxLines = 1
    )
}

/** The size button's glyph: an "A" at the size the label is written. */
@Composable
private fun CaptionSizeGlyph(size: PersonalCaptionLabelSize) {
    Text(
        "A",
        style = TextStyle(
            fontFamily = WritingFontFamily,
            fontSize = (12f * size.factor).sp,
            color = LocalContentColor.current
        ),
        maxLines = 1
    )
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
