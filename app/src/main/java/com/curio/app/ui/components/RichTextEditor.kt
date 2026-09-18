package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import com.curio.app.data.NotePaperColor
import com.curio.app.data.NotePaperStyle
import com.curio.app.data.TextSpan
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.notePaperHighlight
import com.curio.app.ui.theme.notePaperInk
import com.curio.app.ui.theme.notePaperSurface
import com.curio.app.ui.theme.paperControlAccent
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.paperHighlight
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import com.curio.app.ui.theme.PatrickHandFontFamily
import com.curio.app.ui.theme.WritingFontFamily

/**
 * v391 — WHICH FIELDS ARE BEING WRITTEN IN, in ROOT coordinates.
 *
 * [Modifier.clearWritingOnOutsideTap] asks this before it takes a tap away from
 * somebody: a tap that landed on a field's own paper is that field's business,
 * and only a tap on the page AROUND the paper is "I am done with this" (user
 * request: "when i've something selected in the text … and i click the blank
 * area below it should auto deselect"). Every editor registers the paper it is
 * written on while it is in the tree and takes it back when it leaves.
 */
internal val richTextWritingRects = mutableMapOf<Any, Rect>()

/**
 * v391 — TAP THE PAGE, AND THE WRITING LETS GO.
 *
 * The host wraps a screen (or a full-screen editor sheet) whose text fields sit
 * on note-paper cards. A tap that some CHILD did not consume — the field itself,
 * a button, a swatch — and that landed outside every registered paper folds the
 * writing away: the caret goes, the selection with it, and a floating dock
 * standing under the field drops with the keyboard. Scrolling is untouched: only
 * a press that lifts without a child claiming it and without travelling counts.
 */
@Composable
internal fun Modifier.clearWritingOnOutsideTap(): Modifier {
    val focusManager = LocalFocusManager.current
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { coords -> hostOrigin = coords.boundsInRoot().topLeft }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val up = waitForUpOrCancellation()
                if (up != null && !down.isConsumed && !up.isConsumed) {
                    val root = up.position + hostOrigin
                    val onPaper = richTextWritingRects.values.any { it.contains(root) }
                    if (!onPaper) focusManager.clearFocus()
                }
            }
        }
}

/**
 * The rich-text flags the toolbar can apply. [TextSpan] stores each as a
 * boolean so saved captures stay plain data + offsets.
 */
// v375 — made internal so the share card's full-screen selection bar can
// reuse the same span toggles as Save your take's editor.
internal enum class RichFlag { BOLD, ITALIC, HIGHLIGHT }

// Fixed letter-size options offered by the A+/A− dropdown — 2sp steps
// above/below the field's default bodyLarge size (16sp), clamped to a
// notebook-sane range that still fits the paper's 24sp ruled-line cadence.
// Picking one applies it to the selection (if any) AND arms it so the next
// text typed carries that size.
private const val BASE_FONT_SP = 16f
private const val MIN_FONT_SP = 12f
private const val MAX_FONT_SP = 24f
// The field default (BASE_FONT_SP) is offered separately as "Default".
private val SIZE_OPTIONS: List<Float> =
    (MIN_FONT_SP.toInt()..MAX_FONT_SP.toInt() step 2).map { it.toFloat() }
        .filterNot { it == BASE_FONT_SP }

/** How the formatting toolbar is presented. */
/**
 * v389 — HOW LONG A BLUR IS ALLOWED TO LAST before the dock believes the member
 * has left the field.
 *
 * A tap on a dock button can blur the field for an instant, and a floating dock
 * that folded away the moment it was touched would be a dock nobody could use.
 * Only a blur that sticks — moving to another note, tapping the page — is long
 * enough to mean "stopped writing", and a quarter of a second is the shortest
 * delay that survives a tap without feeling laggy when it is real.
 */
private const val DOCK_BLUR_GRACE_MS = 250L

enum class RichTextToolbarMode {
    /** The Marginalia journal + quote cards (main option). */
    MAIN,

    /** Other text fields (Field Notes sections, Reel Notes review, …). */
    TOGGLE,

    /**
     * THE JOURNAL'S OWN DOCK, worn by the app's full-screen rich-text editors
     * (the Share Hub's card editor and the book sheet's note expand): a floating
     * rounded strip at the FOOT of the field, every tool its own button, the
     * active one in the accent — the same dock the journal page writes on. The
     * compact capture-format strips (MAIN / TOGGLE) are untouched.
     *
     * The IMAGE tool is deliberately not here: a rich-text field holds text, and
     * the dock's photo door belongs to the page that has a page to put it on.
     */
    DOCK
}

/**
 * Builds an [AnnotatedString] from plain [text] + [spans] — the shared
 * render path for the editor AND the saved-entry detail view, so bold /
 * italic / highlight look identical while editing and after saving.
 */
fun buildRichAnnotated(text: String, spans: List<TextSpan>, highlightColor: Color): AnnotatedString =
    buildAnnotatedString {
        append(text)
        for (sp in spans) {
            val s = sp.start.coerceIn(0, text.length)
            val e = sp.end.coerceIn(s, text.length)
            if (e > s) {
                // v389 — a run's FAMILY, when it has one (the dock's font tool).
                // The text stack takes it as a span style, so only the letters
                // in the run change hand.
                val family = richFontFamily(sp.fontKey)
                if (family != null) {
                    addStyle(SpanStyle(fontFamily = family, fontSynthesis = FontSynthesis.All), s, e)
                }
                // v389 — a run's ALIGNMENT is a PARAGRAPH property: the stack
                // aligns whole lines, so any run reaching into a line sets it
                // for that line. Applied as its own style, so a bold run and an
                // alignment run can cover the same words without either
                // dropping the other.
                val align = richTextAlign(sp.alignKey)
                if (align != null) addStyle(ParagraphStyle(textAlign = align), s, e)
                addStyle(
                    SpanStyle(
                        fontWeight = if (sp.bold) FontWeight.Bold else null,
                        fontStyle = if (sp.italic) FontStyle.Italic else null,
                        // Per-letter size (sp) — only spans the styled letters.
                        // SpanStyle.fontSize is NON-null TextUnit in this
                        // Compose version, so the nullable Float must resolve
                        // to TextUnit.Unspecified when the span has no size.
                        fontSize = sp.fontSizeSp?.sp ?: TextUnit.Unspecified,
                        // Patrick Hand ships ONE regular file (no bold/italic
                        // TTF exists), so bold/italic only render because the
                        // text stack SYNTHESIZES them. The platform default
                        // may not apply synthesis, so request it explicitly:
                        // with the family declaring just the regular face, a
                        // Bold/Italic request mismatches the loaded font and
                        // FontSynthesis.All turns that mismatch into fake
                        // bold / oblique in the editor AND the saved view.
                        fontSynthesis = FontSynthesis.All,
                        // Non-null Color in this Compose version — the "no
                        // highlight" sentinel is Color.Unspecified.
                        background = if (sp.highlight) highlightColor else Color.Unspecified,
                        // v379 — per-letter UNDERLINE (the share card's
                        // full-screen selection bar): rendered as a text
                        // decoration so card text (and the export) carry it.
                        textDecoration = if (sp.underline) TextDecoration.Underline else null
                    ),
                    s, e
                )
            }
        }
    }

/**
 * v332 — @Composable wrapper of [buildRichAnnotated] for READ-ONLY render
 * sites (the saved-entry detail pages): remembers the built [AnnotatedString]
 * keyed on (text, spans, highlight), so parent recompositions / scroll
 * invalidations reuse the SAME instance instead of rebuilding a fresh
 * AnnotatedString — with its full span-buffer allocations — on every pass
 * (one of the long-note detail lag sources from the logcat triage: a giant
 * note rebuilt its whole annotated string per recomposition, churning
 * large-object allocations). Only for STATIC text whose inputs change
 * rarely; the live editor keeps calling [buildRichAnnotated] directly
 * because its spans legitimately change on every keystroke.
 */
@Composable
fun rememberRichAnnotated(
    text: String,
    spans: List<TextSpan>,
    highlightColor: Color
): AnnotatedString =
    remember(text, spans, highlightColor) {
        buildRichAnnotated(text, spans, highlightColor)
    }

/**
 * Extracts the styled [TextSpan]s carried by an [AnnotatedString] — used to
 * read the editor's spans back out after Compose merges them while typing
 * (BasicTextField preserves span styles across edits, so no manual diffing).
 */
fun extractRichSpans(annotated: AnnotatedString): List<TextSpan> {
    val styled = annotated.spanStyles.mapNotNull { range ->
        val bold = range.item.fontWeight == FontWeight.Bold
        val italic = range.item.fontStyle == FontStyle.Italic
        val highlight = range.item.background != Color.Unspecified
        // v379 — underline survives AnnotatedString round-trips too.
        val underline = range.item.textDecoration == TextDecoration.Underline
        val size = range.item.fontSize
        val sizeSp = if (size.isSpecified) size.value else null
        // v389 — and so do the family and the alignment: the family rides the
        // span styles, the alignment rides the PARAGRAPH styles below.
        val fontKey = richFontKey(range.item.fontFamily)
        if (!bold && !italic && !highlight && !underline && sizeSp == null && fontKey == null) null
        else TextSpan(
            range.start, range.end, bold, italic, highlight, sizeSp, underline,
            fontKey = fontKey
        )
    }
    val aligned = annotated.paragraphStyles.mapNotNull { range ->
        richAlignKey(range.item.textAlign)?.let { key ->
            TextSpan(range.start, range.end, alignKey = key)
        }
    }
    return (styled + aligned).merged()
}

/** Sorts and merges adjacent/overlapping spans with identical flags. */
private fun List<TextSpan>.merged(): List<TextSpan> {
    if (isEmpty()) return emptyList()
    val sorted = sortedWith(compareBy<TextSpan> { it.start }.thenBy { it.end })
    val out = mutableListOf<TextSpan>()
    for (sp in sorted) {
        val last = out.lastOrNull()
        if (last != null && last.end >= sp.start &&
            last.bold == sp.bold && last.italic == sp.italic &&
            last.highlight == sp.highlight && last.fontSizeSp == sp.fontSizeSp &&
            last.underline == sp.underline &&
            last.alignKey == sp.alignKey && last.fontKey == sp.fontKey
        ) {
            out[out.size - 1] = last.copy(end = maxOf(last.end, sp.end))
        } else {
            out.add(sp)
        }
    }
    return out
}

// ── v389 — the dock's family and alignment tables ──────────────────────
//
// Both directions live here: key → what to draw with, and what the text stack
// handed back → key. The keys are what the saved JSON carries.

/** The hands a run can be set in. `null` = the field's own default. */
private val RICH_FONTS: List<Pair<String, FontFamily>> = listOf(
    "default" to FontFamily.Default,
    "book" to LoraFontFamily,
    "writing" to WritingFontFamily,
    "display" to FrauncesFontFamily
)

internal fun richFontFamily(key: String?): FontFamily? =
    RICH_FONTS.firstOrNull { it.first == key }?.second

internal fun richFontKey(family: FontFamily?): String? {
    if (family == null) return null
    return RICH_FONTS.firstOrNull { it.second == family }?.first
}

internal fun richTextAlign(key: String?): TextAlign? = when (key) {
    "start" -> TextAlign.Start
    "center" -> TextAlign.Center
    "end" -> TextAlign.End
    "justify" -> TextAlign.Justify
    else -> null
}

internal fun richAlignKey(align: TextAlign?): String? = when (align) {
    TextAlign.Center -> "center"
    TextAlign.End, TextAlign.Right -> "end"
    TextAlign.Justify -> "justify"
    TextAlign.Start, TextAlign.Left -> "start"
    else -> null
}

/** True when a run still says something after a property was cleared from it. */
private val TextSpan.hasAnyStyle: Boolean
    get() = bold || italic || highlight || underline || fontSizeSp != null ||
        alignKey != null || fontKey != null

/**
 * Clears whatever [drop] takes off every run overlapping [s, e), splitting the
 * runs at the edges exactly as the flag toggles do — the shape that lets a run
 * carry ONE property at a time without disturbing its neighbours.
 */
private fun clearRunOver(
    spans: List<TextSpan>,
    s: Int,
    e: Int,
    drop: (TextSpan) -> TextSpan
): List<TextSpan> {
    if (e <= s) return spans
    val out = mutableListOf<TextSpan>()
    for (sp in spans) {
        if (sp.end <= s || sp.start >= e) {
            out.add(sp)
            continue
        }
        if (sp.start < s) out.add(sp.copy(end = s))
        val middle = drop(sp.copy(start = maxOf(sp.start, s), end = minOf(sp.end, e)))
        if (middle.hasAnyStyle) out.add(middle)
        if (sp.end > e) out.add(sp.copy(start = e))
    }
    return out
}

/** v389 — sets (or clears, with `null`) the ALIGNMENT of [s, e). */
internal fun setSpanAlign(spans: List<TextSpan>, s: Int, e: Int, key: String?): List<TextSpan> {
    val cleared = clearRunOver(spans, s, e) { it.copy(alignKey = null) }
    return if (key == null) cleared.merged()
    else (cleared + TextSpan(start = s, end = e, alignKey = key)).merged()
}

/** v389 — sets (or clears, with `null`) the FAMILY of [s, e). */
internal fun setSpanFont(spans: List<TextSpan>, s: Int, e: Int, key: String?): List<TextSpan> {
    val cleared = clearRunOver(spans, s, e) { it.copy(fontKey = null) }
    return if (key == null) cleared.merged()
    else (cleared + TextSpan(start = s, end = e, fontKey = key)).merged()
}

/** The alignment in force at [pos] — the innermost run that carries one. */
internal fun alignKeyAt(spans: List<TextSpan>, pos: Int): String? =
    spans.filter { it.alignKey != null && it.start <= pos && pos < it.end }
        .minByOrNull { it.end - it.start }
        ?.alignKey

/** The family in force at [pos] — the innermost run that carries one. */
internal fun fontKeyAt(spans: List<TextSpan>, pos: Int): String? =
    spans.filter { it.fontKey != null && it.start <= pos && pos < it.end }
        .minByOrNull { it.end - it.start }
        ?.fontKey

/**
 * The paragraph the caret is in, as a range — what an alignment or a font
 * applies to when the member has selected nothing ("justify this line").
 */
private fun paragraphRangeAt(text: String, caret: Int): IntRange {
    val at = caret.coerceIn(0, text.length)
    val from = text.lastIndexOf('\n', (at - 1).coerceAtLeast(0))
    val start = if (from < 0 || at == 0) 0 else from + 1
    val to = text.indexOf('\n', at)
    val end = if (to < 0) text.length else to
    return start until end
}

private fun TextSpan.has(flag: RichFlag): Boolean = when (flag) {
    RichFlag.BOLD -> bold
    RichFlag.ITALIC -> italic
    RichFlag.HIGHLIGHT -> highlight
}

/** True when every character of [s, e) is covered by a span carrying [flag]. */
internal fun spansFullyCovered(spans: List<TextSpan>, s: Int, e: Int, flag: RichFlag): Boolean {
    var pos = s
    // Only flag-carrying spans can cover the flag — a size-only span (which
    // coexists with flag spans after an A+/A− resize) must not make the
    // toolbar report the flag as missing just because it sorts first.
    for (sp in spans.filter { it.end > s && it.start < e && it.has(flag) }.sortedBy { it.start }) {
        if (sp.start > pos) return false
        pos = maxOf(pos, sp.end)
        if (pos >= e) return true
    }
    return pos >= e
}

/**
 * Finds the range of characters that changed between [oldText] and
 * [newText] (common-prefix / common-suffix diff, reported in NEW-text
 * coordinates). Used to apply an armed (sticky) format to exactly the
 * characters the user just typed — including typing over a selection
 * (replace) — while pure deletions and unchanged text return null.
 */
private fun findInsertedRange(oldText: String, newText: String): IntRange? {
    if (oldText == newText) return null
    var prefix = 0
    while (prefix < oldText.length && prefix < newText.length &&
        oldText[prefix] == newText[prefix]
    ) prefix++
    var suffix = 0
    while (suffix < oldText.length - prefix && suffix < newText.length - prefix &&
        oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
    ) suffix++
    val start = prefix
    val end = newText.length - suffix
    return if (end > start) start until end else null
}

/**
 * Rebases [spans] (in OLD-text coordinates) onto [newText] after an edit,
 * using the same common-prefix / common-suffix diff as [findInsertedRange].
 * A span fully before the changed region keeps its offsets; a span fully
 * after it shifts by the length delta; a span overlapping the changed region
 * is clipped to its untouched head/tail parts (the replaced text inside the
 * diff is dropped — the armed sticky format re-applies to exactly the typed
 * range). The result is what OUR editor state should carry; BasicTextField's
 * own reported AnnotatedString is NOT used because it can silently drop the
 * styles we set programmatically.
 */
// v375 — internal: the share-card inline fact field rebases its spans on
// plain-text edits exactly like the Save-your-take editor.
internal fun rebaseSpans(oldText: String, newText: String, spans: List<TextSpan>): List<TextSpan> {
    if (spans.isEmpty()) return emptyList()
    if (oldText == newText) return spans
    var prefix = 0
    while (prefix < oldText.length && prefix < newText.length &&
        oldText[prefix] == newText[prefix]
    ) prefix++
    var suffix = 0
    while (suffix < oldText.length - prefix && suffix < newText.length - prefix &&
        oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
    ) suffix++
    val oldEnd = oldText.length - suffix
    val newEnd = newText.length - suffix
    val delta = newEnd - oldEnd
    val out = mutableListOf<TextSpan>()
    for (sp in spans) {
        val s = sp.start.coerceIn(0, oldText.length)
        val e = sp.end.coerceIn(s, oldText.length)
        when {
            // Fully before the changed region — same coordinates.
            e <= prefix -> out.add(sp)
            // Fully after the changed region — shift by the length delta.
            // (A COPY, so the run keeps every attribute it had — a positional
            // rebuild would quietly drop the ones it did not name.)
            s >= oldEnd -> out.add(sp.copy(start = s + delta, end = e + delta))
            // Overlaps the changed region — keep only the untouched parts.
            else -> {
                if (s < prefix) out.add(sp.copy(start = s, end = prefix))
                if (e > oldEnd) out.add(sp.copy(start = maxOf(s, oldEnd) + delta, end = e + delta))
            }
        }
    }
    return out.merged()
}

/**
 * Adds or removes [flag] over [s, e). Adding merges a new span in; removing
 * splits every overlapping span so the un-styled middle drops its flag while
 * the parts outside the selection keep theirs.
 */
// v375 — internal: shared with the share card's floating selection bar.
internal fun toggleSpanFlag(spans: List<TextSpan>, s: Int, e: Int, flag: RichFlag, add: Boolean): List<TextSpan> {
    if (add) {
        return (spans + TextSpan(
            start = s,
            end = e,
            bold = flag == RichFlag.BOLD,
            italic = flag == RichFlag.ITALIC,
            highlight = flag == RichFlag.HIGHLIGHT
        )).merged()
    }
    val out = mutableListOf<TextSpan>()
    for (sp in spans) {
        if (sp.end <= s || sp.start >= e) {
            out.add(sp)
            continue
        }
        if (sp.start < s) out.add(sp.copy(end = s))
        val midStart = maxOf(sp.start, s)
        val midEnd = minOf(sp.end, e)
        val mid = sp.copy(
            start = midStart,
            end = midEnd,
            bold = if (flag == RichFlag.BOLD) false else sp.bold,
            italic = if (flag == RichFlag.ITALIC) false else sp.italic,
            highlight = if (flag == RichFlag.HIGHLIGHT) false else sp.highlight
        )
        if (mid.bold || mid.italic || mid.highlight) out.add(mid)
        if (sp.end > e) out.add(sp.copy(start = e))
    }
    return out.merged()
}

/**
 * v379 — UNDERLINE twin of [toggleSpanFlag]: the share card's full-screen
 * selection bar toggles a per-letter underline over [s, e) WITHOUT touching
 * the bold / italic / highlight flags (Save-your-take's dock has no U
 * button, so its toolbar keeps its own RichFlag-driven code). Mirrors the
 * add / split-remove shape exactly so underline runs coexist with every
 * other style.
 */
internal fun toggleSpanUnderline(spans: List<TextSpan>, s: Int, e: Int, add: Boolean): List<TextSpan> {
    if (e <= s) return spans
    if (add) {
        return (spans + TextSpan(start = s, end = e, underline = true)).merged()
    }
    val out = mutableListOf<TextSpan>()
    for (sp in spans) {
        if (sp.end <= s || sp.start >= e) {
            out.add(sp)
            continue
        }
        if (sp.start < s) out.add(sp.copy(end = s))
        val midStart = maxOf(sp.start, s)
        val midEnd = minOf(sp.end, e)
        val mid = sp.copy(start = midStart, end = midEnd, underline = false)
        if (mid.bold || mid.italic || mid.highlight || mid.underline) out.add(mid)
        if (sp.end > e) out.add(sp.copy(start = e))
    }
    return out.merged()
}

/** v379 — true when every character of [s, e) sits inside an underline span
 *  (the selection bar's U active state). */
internal fun spansUnderlineCovered(spans: List<TextSpan>, s: Int, e: Int): Boolean {
    var pos = s
    for (sp in spans.filter { it.end > s && it.start < e && it.underline }.sortedBy { it.start }) {
        if (sp.start > pos) return false
        pos = maxOf(pos, sp.end)
        if (pos >= e) return true
    }
    return pos >= e
}

/**
 * Sets the font size of [s, e) to exactly [targetSp] sp, splitting every
 * overlapping span so ONLY the selection's letters change size. The new
 * size-only span coexists with any bold/italic/highlight spans ([merged]
 * keeps spans with different flags separate, and both styles render
 * together), so enlarging letters never strips their other formatting.
 */
private fun setSpanSize(spans: List<TextSpan>, s: Int, e: Int, targetSp: Float): List<TextSpan> {
    if (e <= s) return spans
    val out = mutableListOf<TextSpan>()
    for (sp in spans) {
        if (sp.end <= s || sp.start >= e) {
            out.add(sp)
            continue
        }
        if (sp.start < s) out.add(sp.copy(end = s))
        val midStart = maxOf(sp.start, s)
        val midEnd = minOf(sp.end, e)
        val mid = sp.copy(start = midStart, end = midEnd, fontSizeSp = null)
        if (mid.bold || mid.italic || mid.highlight) out.add(mid)
        if (sp.end > e) out.add(sp.copy(start = e))
    }
    out.add(TextSpan(start = s, end = e, fontSizeSp = targetSp))
    return out.merged()
}

/** Removes any per-letter size from [s, e), restoring the field default. */
private fun clearSpanSize(spans: List<TextSpan>, s: Int, e: Int): List<TextSpan> {
    if (e <= s) return spans
    val out = mutableListOf<TextSpan>()
    for (sp in spans) {
        if (sp.end <= s || sp.start >= e) {
            out.add(sp)
            continue
        }
        if (sp.start < s) out.add(sp.copy(end = s))
        val midStart = maxOf(sp.start, s)
        val midEnd = minOf(sp.end, e)
        val mid = sp.copy(start = midStart, end = midEnd, fontSizeSp = null)
        if (mid.bold || mid.italic || mid.highlight) out.add(mid)
        if (sp.end > e) out.add(sp.copy(start = e))
    }
    return out.merged()
}

/**
 * Rich-text editor shared by the capture formats — bold / italic / highlight
 * over the current selection. v7.98 — every tool lives in ONE theme-aware
 * tool dock above the field ("Paper" for the note-paper style + color
 * pickers — wearing a live dot of the current sheet color — "Format" for
 * the B / I / highlight / size tools), and opening one collapses the
 * other. The mode enum is kept for callers but the tool rows no longer
 * stay visible all the time.
 *
 * Edits flow through `BasicTextField`'s AnnotatedString value, so span styles
 * are preserved while typing; a small common-prefix/suffix diff re-applies an
 * armed (sticky) format to newly typed characters. Each change reports the
 * plain text + spans back via [onRichTextChange].
 */
@Composable
fun RichTextEditor(
    text: String,
    spans: List<TextSpan>,
    onRichTextChange: (text: String, spans: List<TextSpan>) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minHeight: Dp = 96.dp,
    /** Optional hard character cap for compact fields such as quote cards. */
    maxCharacters: Int? = null,
    /** Optional visual line cap; the field never grows beyond this many lines. */
    maxLines: Int? = null,
    toolbarMode: RichTextToolbarMode = RichTextToolbarMode.MAIN,
    /**
     * v389 — HOLD THE DOCK AT THE FOOT OF THE SCREEN.
     *
     * In [RichTextToolbarMode.DOCK] the tools normally sit at the foot of the
     * FIELD, so they travel with the words. The app's full-screen editors want
     * them still — the journal's own dock does not move while a page is written
     * — so a pinned editor scrolls the writing INSIDE itself and keeps the dock
     * below it, which is what lets the call site hand the editor a weighted
     * height instead of wrapping it in a scroll of its own.
     *
     * Only meaningful with [RichTextToolbarMode.DOCK] (every other mode keeps its
     * strip above the field, exactly as before).
     */
    dockPinned: Boolean = false,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
    ink: Color = MaterialTheme.colorScheme.onSurface,
    surface: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    /** Inner padding of the text field — zero when the editor sits directly
     *  on note-paper (the surrounding card owns the margins). */
    fieldPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    /** Draws the field's hairline border — off when the editor sits on paper. */
    showFieldBorder: Boolean = true,
    /** Highlighter marker for non-paper fields (default translucent amber).
     *  On note-paper the marker follows the SHEET's color instead — see
     *  [notePaperHighlight]. */
    highlightColor: Color = paperHighlight(),
    /** Renders the field on a note-paper card with the toolbar OUTSIDE the
     *  card, so the ruled lines line up under the field text while typing —
     *  matching the saved detail view's paper pages. [paperStyle] chooses
     *  the slip: [NotePaperStyle.RULED] classic ruled page,
     *  [NotePaperStyle.TORN] torn note, [NotePaperStyle.TORN_RULED] torn
     *  note with ruled lines. When [onPaperStyleChange] is provided, a
     *  compact Ruled/Torn/rules toggle appears in this field's own toolbar.
     *  Default false keeps the plain surface field. */
    paper: Boolean = false,
    paperStyle: NotePaperStyle = NotePaperStyle.RULED,
    onPaperStyleChange: (NotePaperStyle) -> Unit = {},
    /** Note-paper COLOR of the slip when [paper] — chosen per text box via
     *  the swatch picker next to the Ruled/Torn toggle. The ink follows the
     *  sheet so text stays readable on every pastel. */
    paperColor: NotePaperColor = NotePaperColor.CREAM,
    onPaperColorChange: (NotePaperColor) -> Unit = {},
    /** Content inset of the paper card when [paper] is true. */
    paperContentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    /** v391 — WEAR THE JOURNAL PAGE'S OWN INK. The journal writes its lines in
     *  the app's warm writing hand on the theme's page background rather than on
     *  a note-paper slip, and the two full-screen note editors are journal pages
     *  now (user request: "use the journal exact page screen style for the full
     *  screen editor in the add note expand editor") — this was the one thing
     *  still different about them. Note-paper fields keep their own hand. */
    journalInk: Boolean = false,
    /** Optional trailing action (e.g. a small dictation button) rendered at
     *  the END of the field's own toolbar row, opposite the format toggle. */
    trailingAction: (@Composable () -> Unit)? = null,
    /** v125 — reports the field's focus state to the caller (e.g. to show a
     *  floating dictation mic only while the user is typing in this box). */
    onFocusChanged: ((Boolean) -> Unit)? = null,
    /** v7.19 — hides the note-paper COLOR swatch picker behind the paper
     *  style toggle (the mood board's quote boxes keep the color tool
     *  hidden while text formatting + paper style stay available). */
    showColorTool: Boolean = true,
    /** v3xx — TEXT HISTORY: when set, this editor joins the global text
     *  history feed (captures on pause / 10-word boundaries / when the
     *  editor leaves) and shows a small history pill in its tool dock.
     *  Restoring writes straight back into this field — a field that
     *  already has text gets the Replace / Add-above / Add-below chooser.
     *  [historyResetKey] changes whenever the edited subject changes so
     *  captures never carry across different cards/fields (defaults to the
     *  field label itself). */
    historyField: String? = null,
    historyResetKey: Any? = null
) {
    // NOTE: NOT keyed on [text] — the parent echoes our edits back, so a
    // keyed remember would rebuild the field (and drop the cursor) on every
    // keystroke. Hold the value unkeyed and reseed only when the parent
    // pushes a DIFFERENT text (e.g. editing a different saved entry).
    // On note-paper the highlighter marker follows the SHEET — each paper
    // color gets its own matching marker tone (see [notePaperHighlight]), so
    // a colored note's highlight reads as a marker that belongs to that page.
    // Non-paper fields keep the caller's [highlightColor] (default amber).
    val effectiveHighlight = if (paper) notePaperHighlight(paperColor) else highlightColor
    var tfv by remember {
        mutableStateOf(TextFieldValue(buildRichAnnotated(text, spans, effectiveHighlight)))
    }
    var toolbarExpanded by remember { mutableStateOf(false) }
    // Paper style + color controls sit behind their own toggle button (the
    // palette icon, mirroring the FormatText button) so fields that don't
    // need paper styling don't look complicated.
    var styleExpanded by remember { mutableStateOf(false) }
    // Text layout of the field — anchors the floating format bar to the
    // current selection so formatting existing text is discoverable.
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    // Armed (sticky) formats: tapping a toolbar button without a selection
    // arms it so the NEXT characters typed carry the format; applying a
    // format to a selection also arms it, so typing continues in that style.
    var pendingBold by remember { mutableStateOf(false) }
    var pendingItalic by remember { mutableStateOf(false) }
    var pendingHighlight by remember { mutableStateOf(false) }
    // Armed font-size target (sp) — picking a size from the A+/A− dropdown
    // arms a FIXED size so the next characters typed carry it (and the
    // dropdown icons stay lit — their true "active" state).
    var pendingSizeSp by remember { mutableStateOf<Float?>(null) }
    // v389 — armed UNDERLINE. The model has carried the attribute since v379
    // (the share card picks it up on a selection), but no toolbar ever offered
    // it: the journal's dock does, and the dock is what these editors now wear.
    var pendingUnderline by remember { mutableStateOf(false) }
    // Paper mode: the field floats directly on the card's paper — no inner
    // padding of its own (the card owns the margins). The toolbar + cursor
    // also switch to the warm paper accent: these controls sit on cream in
    // BOTH themes, so a theme-aware accent (e.g. the dark-mode tertiary) can
    // read washed-out against the paper. The ink follows the chosen sheet
    // color so text stays readable on every pastel.
    //
    // The paper CONTROL accent is theme-aware ([paperControlAccent]): the
    // slips stay cream in both themes, but the toolbar row + cursor render
    // OUTSIDE the slip on the page background, and the warm amber brown
    // vanished against midnight/AMOLED. Dark mode swaps to a brighter amber
    // so the B / I / highlight / palette icons actually read.
    val effectiveFieldPadding = if (paper) PaddingValues(0.dp) else fieldPadding
    val effectiveAccent = if (paper) paperControlAccent() else accent
    val effectiveInk = if (paper) notePaperInk(paperColor) else ink
    // v3xx — text history: capture + pill + browser live inside the editor
    // so every text box (capture formats, journal, quote cards) gets the
    // global feed without per-caller wiring. Restoring an ADD keeps the
    // field's rich spans (rebased across the insert); Replace clears them.
    val historyContext = LocalContext.current
    var historyOpen by remember(historyField) { mutableStateOf(false) }
    if (historyField != null) {
        rememberTextHistoryCapture(historyContext, historyField, text, historyResetKey ?: historyField)
    }
    LaunchedEffect(text, spans) {
        if (tfv.text != text) {
            tfv = TextFieldValue(buildRichAnnotated(text, spans, effectiveHighlight))
            // Different content loaded (e.g. editing another saved entry) —
            // drop any armed format from the previous text.
            pendingBold = false
            pendingItalic = false
            pendingHighlight = false
            pendingUnderline = false
            pendingSizeSp = null
        }
    }
    // Sheet-color change (swatch tap): spans only carry the highlight FLAG,
    // the marker color is baked into the AnnotatedString at build time. When
    // the paper color changes, repaint existing highlights in the NEW marker
    // tone without disturbing the text or cursor.
    LaunchedEffect(paper, paperColor, effectiveHighlight) {
        if (paper && tfv.text == text) {
            tfv = TextFieldValue(
                buildRichAnnotated(tfv.text, extractRichSpans(tfv.annotatedString), effectiveHighlight),
                selection = tfv.selection,
                composition = tfv.composition
            )
        }
    }
    fun emit(new: TextFieldValue) {
        // Quote cards use hard input caps. Trim pasted/IME content rather
        // than dropping the entire edit, while keeping the selection valid.
        val cappedText = new.text
            .take(maxCharacters ?: Int.MAX_VALUE)
            .let { candidate ->
                if (maxLines == null) candidate
                else candidate.split('\n').take(maxLines).joinToString("\n")
            }
        val accepted = if (cappedText == new.text) new else TextFieldValue(
            cappedText,
            selection = TextRange(
                new.selection.start.coerceIn(0, cappedText.length),
                new.selection.end.coerceIn(0, cappedText.length)
            ),
            // A capped edit can invalidate the IME's old composition range;
            // let the IME establish a fresh composition on the next event.
            composition = if (cappedText == new.text) new.composition else null
        )
        val oldText = tfv.text
        // The text itself changed (a real user edit) — NEVER trust what
        // BasicTextField reports back as its AnnotatedString: it can silently
        // drop the styles we set programmatically, which made bold/italic/
        // highlight vanish moments after applying. Instead rebase OUR OWN
        // spans (from tfv, which we always build ourselves) across the edit,
        // then merge in any caret-inherited styles the field DID report for
        // the new characters (e.g. typing inside an existing bold span keeps
        // inheriting bold without an explicit arm).
        val textChanged = accepted.text != oldText
        var spans = if (textChanged) {
            rebaseSpans(oldText, accepted.text, extractRichSpans(tfv.annotatedString))
        } else {
            // Text unchanged — a caret/selection move or an IME re-report
            // (e.g. the extra event that follows committing a space). NEVER
            // trust the field's reported AnnotatedString here: BasicTextField
            // can silently drop the styles we set programmatically, which is
            // exactly how bold/italic/highlight used to vanish right after
            // typing a space. Keep OUR spans — tfv is always built by us.
            extractRichSpans(tfv.annotatedString)
        }
        // Caret inheritance from OUR spans: typing INSIDE an already-styled
        // run (e.g. mid-bold word, or at the very start of one so the new
        // char joins the word) keeps that style on the new characters.
        // BasicTextField's reported AnnotatedString can silently drop the
        // styles we set programmatically, so we can't rely on it to re-add
        // them — emulate inheritance from our own tracked spans instead (the
        // caret sits at the diff's start == old/new common prefix, which is
        // unchanged by the edit). The end boundary is EXCLUSIVE (caret <
        // sp.end): typing right AFTER a styled run starts a NEW un-styled
        // run, so toggling a format off actually stops it — an inclusive end
        // made highlight/bold stick forever to everything typed next to a
        // styled word even when the toolbar was turned off. Continuing a
        // style after an explicit apply is handled by the armed (sticky)
        // pending flags, which the user can toggle off.
        val insertedRange = if (textChanged) findInsertedRange(oldText, accepted.text) else null
        if (insertedRange != null) {
            val caret = insertedRange.first
            val inherited = extractRichSpans(tfv.annotatedString).filter { sp ->
                sp.start <= caret && caret < sp.end
            }
            for (sp in inherited) {
                if (sp.bold) {
                    spans = toggleSpanFlag(spans, caret, insertedRange.last + 1, RichFlag.BOLD, true)
                }
                if (sp.italic) {
                    spans = toggleSpanFlag(spans, caret, insertedRange.last + 1, RichFlag.ITALIC, true)
                }
                if (sp.highlight) {
                    spans = toggleSpanFlag(spans, caret, insertedRange.last + 1, RichFlag.HIGHLIGHT, true)
                }
                if (sp.underline) {
                    spans = toggleSpanUnderline(spans, caret, insertedRange.last + 1, true)
                }
                sp.fontSizeSp?.let { size ->
                    spans = setSpanSize(spans, caret, insertedRange.last + 1, size)
                }
            }
        }
        // Sticky format: when a format is armed and the user just typed
        // (or typed over a selection), apply it to exactly the changed
        // characters so typing continues in that style (BasicTextField only
        // inherits the style under the caret, so an armed format needs
        // explicit application). Pure deletions diff to null and are skipped.
        if (pendingBold || pendingItalic || pendingHighlight || pendingUnderline ||
            pendingSizeSp != null
        ) {
            insertedRange?.let { range ->
                if (pendingBold) {
                    spans = toggleSpanFlag(spans, range.first, range.last + 1, RichFlag.BOLD, true)
                }
                if (pendingItalic) {
                    spans = toggleSpanFlag(spans, range.first, range.last + 1, RichFlag.ITALIC, true)
                }
                if (pendingHighlight) {
                    spans = toggleSpanFlag(spans, range.first, range.last + 1, RichFlag.HIGHLIGHT, true)
                }
                if (pendingUnderline) {
                    spans = toggleSpanUnderline(spans, range.first, range.last + 1, true)
                }
                pendingSizeSp?.let { size ->
                    spans = setSpanSize(spans, range.first, range.last + 1, size)
                }
            }
        }
        val result = TextFieldValue(
            buildRichAnnotated(accepted.text, spans, effectiveHighlight),
            selection = accepted.selection,
            composition = accepted.composition
        )
        tfv = result
        // `text` is plain String in this Compose version; the styled
        // AnnotatedString lives on `annotatedString`.
        onRichTextChange(result.text, extractRichSpans(result.annotatedString))
    }

    // BasicTextField's maxLines limits its viewport but does not reject
    // wrapped text. Trim at the fifth measured visual line as a second
    // defensive layer, keeping the stored quote from growing beyond its
    // ruled paper.
    LaunchedEffect(layoutResult, maxLines) {
        val layout = layoutResult ?: return@LaunchedEffect
        if (maxLines != null && layout.lineCount > maxLines && tfv.text.isNotEmpty()) {
            val end = layout.getLineEnd(maxLines - 1)
            if (end < tfv.text.length) {
                emit(TextFieldValue(
                    tfv.text.take(end),
                    selection = TextRange(
                        tfv.selection.start.coerceIn(0, end),
                        tfv.selection.end.coerceIn(0, end)
                    )
                ))
            }
        }
    }

    fun applyFlag(flag: RichFlag) {
        val sel = tfv.selection
        if (sel.collapsed) {
            // No selection — arm the format so the next characters typed
            // carry it (and the toolbar shows it as active).
            when (flag) {
                RichFlag.BOLD -> pendingBold = !pendingBold
                RichFlag.ITALIC -> pendingItalic = !pendingItalic
                RichFlag.HIGHLIGHT -> pendingHighlight = !pendingHighlight
            }
            return
        }
        val s = minOf(sel.start, sel.end)
        val e = maxOf(sel.start, sel.end)
        val current = extractRichSpans(tfv.annotatedString)
        val add = !spansFullyCovered(current, s, e, flag)
        val updated = toggleSpanFlag(current, s, e, flag, add)
        // Apply directly (not via emit): emit derives spans from OUR tracked
        // tfv, which isn't updated yet at this point — we built the styled
        // value ourselves, so set it and report it here. This also avoids
        // trusting any field-reported AnnotatedString for span content.
        val styled = TextFieldValue(
            buildRichAnnotated(tfv.text, updated, effectiveHighlight),
            selection = sel
        )
        tfv = styled
        onRichTextChange(styled.text, extractRichSpans(styled.annotatedString))
        // v3xx — applying a format to a SELECTION is one-shot: the tool
        // turns off afterwards (no sticky arm), so the toolbar never stays
        // lit after a single change. Tapping a tool with a collapsed caret
        // still arms it for the next characters typed (see the collapsed
        // branch above).
    }

    /** Applies already-built spans, keeping the caret exactly where it was. */
    fun applyRun(updated: List<TextSpan>) {
        val caret = tfv.selection
        val styled = TextFieldValue(
            buildRichAnnotated(tfv.text, updated, effectiveHighlight),
            selection = caret
        )
        tfv = styled
        onRichTextChange(styled.text, extractRichSpans(styled.annotatedString))
    }

    /**
     * v389 — JUSTIFY THIS PARAGRAPH. Alignment belongs to LINES, not to letters,
     * so with nothing selected it lands on the line the caret is in ("centre
     * this line") and with a selection it lands on every line the selection
     * touches. `null` takes the alignment off and hands the line back to the
     * field's own default.
     */
    fun applyAlign(key: String?) {
        val sel = tfv.selection
        val range = if (sel.collapsed) paragraphRangeAt(tfv.text, sel.start)
        else minOf(sel.start, sel.end) until maxOf(sel.start, sel.end)
        if (range.isEmpty()) return
        applyRun(setSpanAlign(extractRichSpans(tfv.annotatedString), range.first, range.last + 1, key))
    }

    /**
     * v389 — SET THE HAND this line is written in (or this selection). Same
     * paragraph rule as [applyAlign], so the tool always does something visible
     * even with the caret just sitting in a line.
     */
    fun applyFont(key: String?) {
        val sel = tfv.selection
        val range = if (sel.collapsed) paragraphRangeAt(tfv.text, sel.start)
        else minOf(sel.start, sel.end) until maxOf(sel.start, sel.end)
        if (range.isEmpty()) return
        applyRun(setSpanFont(extractRichSpans(tfv.annotatedString), range.first, range.last + 1, key))
    }

    /**
     * UNDERLINE, the dock's own addition — same manners as [applyFlag]: a
     * collapsed caret ARMS it for the next characters typed, a selection is
     * applied once (so the tool never stays lit after a single change).
     */
    fun applyUnderline() {
        val sel = tfv.selection
        if (sel.collapsed) {
            pendingUnderline = !pendingUnderline
            return
        }
        val s = minOf(sel.start, sel.end)
        val e = maxOf(sel.start, sel.end)
        val current = extractRichSpans(tfv.annotatedString)
        val add = !spansUnderlineCovered(current, s, e)
        val updated = toggleSpanUnderline(current, s, e, add)
        val styled = TextFieldValue(
            buildRichAnnotated(tfv.text, updated, effectiveHighlight),
            selection = sel
        )
        tfv = styled
        onRichTextChange(styled.text, extractRichSpans(styled.annotatedString))
    }

    /** Applies the picked [targetSp] to the selection (if any) and arms it. */
    fun applyExactSize(targetSp: Float) {
        val sel = tfv.selection
        if (!sel.collapsed) {
            val s = minOf(sel.start, sel.end)
            val e = maxOf(sel.start, sel.end)
            val updated = if (targetSp == BASE_FONT_SP) {
                clearSpanSize(extractRichSpans(tfv.annotatedString), s, e)
            } else {
                setSpanSize(extractRichSpans(tfv.annotatedString), s, e, targetSp)
            }
            val styled = TextFieldValue(
                buildRichAnnotated(tfv.text, updated, effectiveHighlight),
                selection = sel
            )
            tfv = styled
            onRichTextChange(styled.text, extractRichSpans(styled.annotatedString))
            // v3xx — size applied to a SELECTION is one-shot: no arm, so the
            // size tool doesn't stay lit after a single change.
            return
        }
        // No selection — picking a size arms it for the next typed
        // characters; picking the field default un-arms.
        pendingSizeSp = if (targetSp == BASE_FONT_SP) null else targetSp
    }

    /** The effective font size (sp) at the caret / over the selection. */
    fun currentSizeSp(): Float {
        val sel = tfv.selection
        val current = extractRichSpans(tfv.annotatedString)
        if (sel.collapsed) {
            val pos = sel.start
            return pendingSizeSp
                ?: current.filter { it.start <= pos && pos < it.end }
                    .mapNotNull { it.fontSizeSp }
                    .maxOrNull() ?: BASE_FONT_SP
        }
        val s = minOf(sel.start, sel.end)
        val e = maxOf(sel.start, sel.end)
        return current.filter { it.end > s && it.start < e }
            .mapNotNull { it.fontSizeSp }
            .maxOrNull() ?: BASE_FONT_SP
    }

    fun hasFlagAt(flag: RichFlag): Boolean {
        val sel = tfv.selection
        val s = minOf(sel.start, sel.end)
        val e = maxOf(sel.start, sel.end)
        val current = extractRichSpans(tfv.annotatedString)
        if (sel.collapsed) {
            // Caret: an armed (sticky) format wins so the toolbar shows what
            // the next typed characters will look like; otherwise fall back
            // to the char under the caret.
            val pos = s
            val underCaret = current.any { sp -> sp.start <= pos && pos < sp.end && sp.has(flag) }
            return when (flag) {
                RichFlag.BOLD -> pendingBold || underCaret
                RichFlag.ITALIC -> pendingItalic || underCaret
                RichFlag.HIGHLIGHT -> pendingHighlight || underCaret
            }
        }
        return spansFullyCovered(current, s, e, flag)
    }

    /** Underline's own sibling of [hasFlagAt] — the dock's lit state. */
    fun hasUnderlineAt(): Boolean {
        val sel = tfv.selection
        val s = minOf(sel.start, sel.end)
        val e = maxOf(sel.start, sel.end)
        val current = extractRichSpans(tfv.annotatedString)
        if (sel.collapsed) {
            val pos = s
            val underCaret = current.any { sp -> sp.start <= pos && pos < sp.end && sp.underline }
            return pendingUnderline || underCaret
        }
        return spansUnderlineCovered(current, s, e)
    }

    Column(modifier = modifier) {
        // ── Tool dock — one theme-aware strip above the field ───────────
        // v7.98 — redesigned: a single rounded dock (theme surface)
        // replaces the old floating bordered text buttons. Collapsed it is
        // ONE slim row — [Paper] / [Format] toggles (Paper wears a LIVE dot
        // of the current paper color) plus the trailing action; opening a
        // toggle expands its tools INSIDE the dock behind a hairline
        // divider. Every color comes from theme tokens (surface container,
        // outline variant, accent), so the dock is properly theme-aware in
        // light, dark, AMOLED and pastel — no hardcoded alpha bumps.
        // The journal-style dock (DOCK mode) carries the format tools at the
        // FOOT of the field, so this head strip appears only when it still has
        // something of its own to say — the paper tools, a trailing action or
        // the text history. In the other modes it is the dock, as before.
        // ── v389 — THE DOCK IS A TYPING INSTRUMENT, NOT FURNITURE ───────
        //
        // In DOCK mode the tools used to stand at the foot of every note field
        // for as long as the note existed, whether or not anyone was writing in
        // it — a row of formatting buttons parked under a filled-in note, and
        // the same row under the next one, and the next (user request: "in save
        // your take where the tool bar is for every notes, remove it and use a
        // floating bottom tool bar style … remember it only appears when i open
        // the keyboard to type in that text box … so the tool bar doesnt always
        // stay when im not typing").
        //
        // So it follows the FIELD's focus, which is what "typing in this box"
        // means — and the fleeting part is deliberate: a tap on a dock button
        // can blur the field for an instant, and a dock that vanished the moment
        // it was touched would be a dock nobody could use. Only a blur that
        // STICKS (the member moved to another note, or tapped the page) hides it.
        var fieldFocused by remember { mutableStateOf(false) }
        var dockVisible by remember { mutableStateOf(false) }
        // ── v391 — "TYPING IN THIS BOX" MEANS THE KEYBOARD IS UP ──────────
        //
        // Focus alone used to hold the dock up, and a dismissed keyboard left a
        // row of formatting buttons standing under a note nobody was writing in
        // (user request: "hide the tool bar when keyboard is closed"). The IME
        // inset is read HERE, in composition, the way the rest of the app reads
        // it ([WindowInsets.ime] on a live window), and the same blur grace still
        // covers a tap on the dock's own buttons — which blurs the field for an
        // instant — so only a keyboard that is really gone folds the tools away.
        val keyboardUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        LaunchedEffect(fieldFocused, keyboardUp) {
            if (fieldFocused && keyboardUp) {
                dockVisible = true
            } else {
                delay(DOCK_BLUR_GRACE_MS)
                dockVisible = false
            }
        }
        // ── v391 — IN DOCK MODE THE STRIP ABOVE THE FIELD IS GONE ─────────
        //
        // The dock IS the toolbar: the paper style toggle, its colour swatches,
        // the text-history pill and the field's own trailing action have all
        // moved down into it (user request: "remove the above tools from save
        // your take express yourself notes text field … in the floating buttom
        // tool bar of save your take, show the paper and its color changing tool
        // too"). The compact capture strips (MAIN / TOGGLE) keep their row, so
        // nothing about the other fields on those screens changes.
        val showTopStrip = toolbarMode != RichTextToolbarMode.DOCK
        if (showTopStrip) Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (paper) {
                        ToolToggleButton(
                            icon = CurioIcons.Palette,
                            label = "Paper",
                            expanded = styleExpanded,
                            accent = effectiveAccent,
                            dot = notePaperSurface(paperColor),
                            enabled = enabled,
                            onToggle = {
                                val next = !styleExpanded
                                if (next) toolbarExpanded = false
                                styleExpanded = next
                            }
                        )
                    }
                    if (toolbarMode != RichTextToolbarMode.DOCK) ToolToggleButton(
                        icon = CurioIcons.FormatText,
                        label = "Format",
                        expanded = toolbarExpanded,
                        accent = effectiveAccent,
                        enabled = enabled,
                        onToggle = {
                            val next = !toolbarExpanded
                            if (next) styleExpanded = false
                            toolbarExpanded = next
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    trailingAction?.invoke()
                    if (historyField != null) {
                        Spacer(Modifier.width(2.dp))
                        TextHistoryPill(onClick = { historyOpen = true }, size = 32.dp)
                    }
                }
                AnimatedVisibility(
                    visible = paper && styleExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = effectiveAccent.copy(alpha = 0.16f))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            NotePaperStyleToggle(
                                style = paperStyle,
                                onStyleChange = onPaperStyleChange,
                                accent = effectiveAccent,
                                enabled = enabled,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (showColorTool) {
                                NotePaperColorToggle(
                                    color = paperColor,
                                    onColorChange = onPaperColorChange,
                                    accent = effectiveAccent,
                                    enabled = enabled
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = toolbarExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = effectiveAccent.copy(alpha = 0.16f))
                        FormatToolbar(
                            boldActive = hasFlagAt(RichFlag.BOLD),
                            italicActive = hasFlagAt(RichFlag.ITALIC),
                            highlightActive = hasFlagAt(RichFlag.HIGHLIGHT),
                            sizeActive = pendingSizeSp != null,
                            accent = effectiveAccent,
                            enabled = enabled,
                            currentSp = currentSizeSp(),
                            onBold = { applyFlag(RichFlag.BOLD) },
                            onItalic = { applyFlag(RichFlag.ITALIC) },
                            onHighlight = { applyFlag(RichFlag.HIGHLIGHT) },
                            onSizePick = { applyExactSize(it) },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            paper = paper
                        )
                    }
                }
            }
        }
        // Small air gap so the dock reads as one unit above the field/paper.
        Spacer(Modifier.height(4.dp))

        // ── The field ───────────────────────────────────────────────────
        // On note-paper ([paper]) the field renders inside a PaperCard with
        // the toolbar OUTSIDE the card, so the ruled lines line up under the
        // text while typing — matching the saved detail view's paper pages.
        //
        // v391 — AND THIS PAPER TELLS THE HOST WHERE IT IS. A tap that lands
        // here is this field's business; a tap on the page around it is the
        // writer saying they are done (see [clearWritingOnOutsideTap]).
        val writingKey = remember { Any() }
        val paperMeasured = Modifier.onGloballyPositioned { coords ->
            richTextWritingRects[writingKey] = coords.boundsInRoot()
        }
        DisposableEffect(writingKey) {
            onDispose { richTextWritingRects.remove(writingKey) }
        }
        val fieldBlock: @Composable () -> Unit = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    // Paper mode: a SQUARE shape. M3 Surface clips its
                    // content to the shape, and in paper mode the field
                    // padding is 0 — a rounded corner would slice the first
                    // characters' tops (the "text hides behind the corner"
                    // bug during entry). The paper card already owns the
                    // margins; the field must not clip at all.
                    shape = if (paper) RoundedCornerShape(0.dp) else RoundedCornerShape(14.dp),
                    color = if (paper) Color.Transparent else surface,
                    shadowElevation = if (paper || !showFieldBorder) 0.dp else 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (paper) Modifier else paperMeasured)
                ) {
                    BasicTextField(
                        value = tfv,
                        onValueChange = { emit(it) },
                        enabled = enabled,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            // Paper notes wear the handwritten Patrick Hand, a
                            // journal page wears the journal's writing hand, and
                            // plain (non-paper) fields keep the neutral sans.
                            fontFamily = when {
                                paper -> PatrickHandFontFamily
                                journalInk -> WritingFontFamily
                                else -> FontFamily.Default
                            },
                            color = effectiveInk
                        ),
                        cursorBrush = SolidColor(effectiveAccent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default
                        ),
                        onTextLayout = { layoutResult = it },
                        // Keep the layout unrestricted when a visual cap is
                        // requested so onTextLayout can see the real wrapped
                        // line count and trim the stored text. BasicTextField's
                        // own maxLines only clips the viewport; it does not
                        // reject overflow from a paste or IME commit.
                        maxLines = Int.MAX_VALUE,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = minHeight)
                            .padding(effectiveFieldPadding)
                            .onFocusChanged {
                                fieldFocused = it.isFocused
                                onFocusChanged?.invoke(it.isFocused)
                            }
                    )
                }

                // ── Floating format bar — appears above the text selection so
                // formatting EXISTING text is discoverable: select words, then
                // tap B / I / highlight right there (the main toolbar still
                // works too — this is an extra, selection-local entry point).
                val selection = tfv.selection
                val layout = layoutResult
                if (enabled && !selection.collapsed && layout != null) {
                    val density = LocalDensity.current
                    val caretRect = runCatching { layout.getCursorRect(selection.max) }.getOrNull()
                    if (caretRect != null) {
                        val padLeft = with(density) { effectiveFieldPadding.calculateLeftPadding(LayoutDirection.Ltr).toPx() }
                        val padTop = with(density) { effectiveFieldPadding.calculateTopPadding().toPx() }
                        val barHeight = with(density) { 40.dp.toPx() }
                        // 4 buttons (B / I / highlight / text size) — narrower
                        // than the old 5-button bar.
                        val barWidth = with(density) { 150.dp.toPx() }
                        val gap = with(density) { 8.dp.toPx() }
                        // Float above the selection; drop below it when the
                        // selection is at the very top of the field.
                        val aboveY = padTop + caretRect.top - barHeight - gap
                        val y = if (aboveY >= 0f) aboveY else padTop + caretRect.bottom + gap
                        // Center the bar on the selection end, clamped so it
                        // never runs off the field's left/right edge.
                        val maxX = (with(density) { maxWidth.toPx() } - barWidth).coerceAtLeast(0f)
                        val x = (padLeft + caretRect.left - barWidth / 2f).coerceIn(0f, maxX)
                        Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(x.roundToInt(), y.roundToInt()),
                            properties = PopupProperties(focusable = false)
                        ) {
                            SelectionFormatBar(
                                boldActive = hasFlagAt(RichFlag.BOLD),
                                italicActive = hasFlagAt(RichFlag.ITALIC),
                                highlightActive = hasFlagAt(RichFlag.HIGHLIGHT),
                                sizeActive = pendingSizeSp != null,
                                accent = effectiveAccent,
                                enabled = enabled,
                                currentSp = currentSizeSp(),
                                onBold = { applyFlag(RichFlag.BOLD) },
                                onItalic = { applyFlag(RichFlag.ITALIC) },
                                onHighlight = { applyFlag(RichFlag.HIGHLIGHT) },
                                onSizePick = { applyExactSize(it) },
                                paper = paper
                            )
                        }
                    }
                }
            }
            if (tfv.text.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = when {
                            paper -> PatrickHandFontFamily
                            journalInk -> WritingFontFamily
                            else -> FontFamily.Default
                        },
                        color = effectiveInk.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
        // The field and its paper wrapper as ONE unit: where it goes depends on
        // the dock below it (a pinned editor scrolls the writing, so the area
        // has to be something a Box can own).
        val fieldArea: @Composable () -> Unit = {
        if (paper) {
            // v7.16 — universal style model: the base decides torn vs sharp
            // ruled paper and the style's flags drive every decoration, so
            // ALL combinations (incl. the new torn+red-margin / torn+rules+
            // decoration) render here — same flags as [NotePaperCard].
            if (paperStyle.torn) {
                TornPaperCard(
                    modifier = Modifier.fillMaxWidth().then(paperMeasured),
                    ruled = paperStyle.ruled,
                    coffeeStains = paperStyle.coffee,
                    folded = paperStyle.folded,
                    redMargin = paperStyle.redMargin,
                    // v7.39 — forward the style's WATERMARK so every paper
                    // box (not just the single-line title fields that route
                    // through [NotePaperCard]) wears the faint glyph scatter.
                    watermark = paperStyle.watermark,
                    paperColor = paperColor,
                    contentPadding = paperContentPadding
                ) {
                    fieldBlock()
                }
            } else {
                PaperCard(
                    modifier = Modifier.fillMaxWidth().then(paperMeasured),
                    ruled = true,
                    // v7.39 — the rounded-top + watermark options of the
                    // style apply here too (previously only [NotePaperCard]
                    // callers got them).
                    roundedTop = paperStyle.roundedTop,
                    watermark = paperStyle.watermark,
                    paperColor = paperColor,
                    contentPadding = paperContentPadding,
                    coffeeStains = paperStyle.coffee,
                    folded = paperStyle.folded,
                    redMargin = paperStyle.redMargin
                ) {
                    fieldBlock()
                }
            }
        } else {
            fieldBlock()
        }
        }
        // PINNED: the writing scrolls on its own and the dock keeps the foot
        // (the call site hands this editor a weighted height for exactly this).
        if (dockPinned && toolbarMode == RichTextToolbarMode.DOCK) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Column { fieldArea() }
            }
        } else {
            fieldArea()
        }
        // ── The journal's dock (DOCK mode) ─────────────────────────────
        // At the FOOT of the field, where a thumb already is: the same shape,
        // tokens and manners as the journal page's own tool dock, with every
        // tool its own button (no grouped menus) — so the app's full-screen
        // editors and the journal read as ONE writing surface.
        if (toolbarMode == RichTextToolbarMode.DOCK) {
            // The dock rises out of the field's own foot as the writing starts
            // and folds away when it stops, which is also what keeps it clear of
            // the save page's own buttons while a note is being READ rather
            // than written.
            AnimatedVisibility(
                visible = dockVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    RichTextDock(
                        boldActive = hasFlagAt(RichFlag.BOLD),
                        italicActive = hasFlagAt(RichFlag.ITALIC),
                        underlineActive = hasUnderlineAt(),
                        highlightActive = hasFlagAt(RichFlag.HIGHLIGHT),
                        sizeActive = pendingSizeSp != null,
                        accent = effectiveAccent,
                        ink = MaterialTheme.colorScheme.onSurfaceVariant,
                        enabled = enabled,
                        currentSp = currentSizeSp(),
                        onBold = { applyFlag(RichFlag.BOLD) },
                        onItalic = { applyFlag(RichFlag.ITALIC) },
                        onUnderline = { applyUnderline() },
                        onHighlight = { applyFlag(RichFlag.HIGHLIGHT) },
                        onSizePick = { applyExactSize(it) },
                        // The line's own justification and its hand, both read
                        // from the run under the caret (the dock echoes what the
                        // line is wearing).
                        alignKey = alignKeyAt(
                            extractRichSpans(tfv.annotatedString), tfv.selection.start
                        ),
                        fontKey = fontKeyAt(
                            extractRichSpans(tfv.annotatedString), tfv.selection.start
                        ),
                        onAlign = { applyAlign(it) },
                        onFont = { applyFont(it) },
                        // The rest of the universal toolbar — the tools that used
                        // to sit on the strip above the field.
                        paper = paper,
                        paperStyle = paperStyle,
                        onPaperStyleChange = onPaperStyleChange,
                        paperColor = paperColor,
                        onPaperColorChange = onPaperColorChange,
                        showColorTool = showColorTool,
                        onHistory = if (historyField != null) {
                            { historyOpen = true }
                        } else null,
                        trailingAction = trailingAction,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // The text-history browser for this field — self-contained: pill in
        // the dock → this sheet → restore straight back into the editor.
        if (historyOpen && historyField != null) {
            TextHistoryBrowser(
                ctx = historyContext,
                activeField = historyField,
                currentText = text,
                onRestore = { restored, mode ->
                    val combined = when (mode) {
                        TextHistoryRestoreMode.REPLACE -> restored
                        TextHistoryRestoreMode.ADD_TOP ->
                            if (text.isBlank()) restored else "$restored\n$text"
                        TextHistoryRestoreMode.ADD_BOTTOM ->
                            if (text.isBlank()) restored else "$text\n$restored"
                    }
                    val mergedSpans = if (mode == TextHistoryRestoreMode.REPLACE) emptyList()
                    else rebaseSpans(text, combined, spans)
                    onRichTextChange(combined, mergedSpans)
                },
                onDismiss = { historyOpen = false }
            )
        }
    }
}

/**
 * Floating mini-toolbar shown at the text selection — B / I / highlight
 * that apply to the selected characters. Mirrors [FormatToolbar]'s buttons
 * in a compact floating chip so formatting existing text is discoverable.
 */
@Composable
// v375 — internal so the full-screen card editor can float the same
// selection-local formatting bar over its inline fact field.
internal fun SelectionFormatBar(
    boldActive: Boolean,
    italicActive: Boolean,
    highlightActive: Boolean,
    sizeActive: Boolean,
    accent: Color,
    enabled: Boolean,
    currentSp: Float,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onHighlight: () -> Unit,
    onSizePick: (Float) -> Unit,
    paper: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
        modifier = Modifier.padding(bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormatToolButton(CurioIcons.FormatBold, "Bold", boldActive, accent, enabled, onBold, paper = paper)
            FormatToolButton(CurioIcons.FormatItalic, "Italic", italicActive, accent, enabled, onItalic, paper = paper)
            FormatToolButton(CurioIcons.FormatHighlight, "Highlight", highlightActive, accent, enabled, onHighlight, paper = paper)
            // One text-size button — the A+/A− pair both opened the same
            // size-picker dropdown, so they collapsed into a single button.
            SizePickerButton(
                icon = CurioIcons.TextIncrease,
                label = "Text size",
                active = sizeActive,
                accent = accent,
                enabled = enabled,
                currentSp = currentSp,
                onPick = onSizePick,
                paper = paper
            )
        }
    }
}

/**
 * THE JOURNAL'S DOCK, worn by the app's full-screen rich-text editors.
 *
 * Shape, tokens and manners are the journal page's own tool dock
 * (`PersonalToolDock`): a floating rounded strip in `surfaceContainerHigh`, a
 * 6dp lift, the tools scrolling in one row and the active one filled with the
 * accent at 24% and inked in the accent. A member who has written on a page
 * arrives here knowing exactly where everything is.
 */
@Composable
private fun RichTextDock(
    boldActive: Boolean,
    italicActive: Boolean,
    underlineActive: Boolean,
    highlightActive: Boolean,
    sizeActive: Boolean,
    accent: Color,
    ink: Color,
    enabled: Boolean,
    currentSp: Float,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onHighlight: () -> Unit,
    onSizePick: (Float) -> Unit,
    /** The line's own alignment / hand, and the doors that set them. */
    alignKey: String?,
    fontKey: String?,
    onAlign: (String?) -> Unit,
    onFont: (String?) -> Unit,
    // ── v391 — the tools the strip above the field used to carry ──────
    /** Paper style + colour, the field's history pill, its trailing action. */
    paper: Boolean = false,
    paperStyle: NotePaperStyle = NotePaperStyle.RULED,
    onPaperStyleChange: (NotePaperStyle) -> Unit = {},
    paperColor: NotePaperColor = NotePaperColor.CREAM,
    onPaperColorChange: (NotePaperColor) -> Unit = {},
    showColorTool: Boolean = true,
    onHistory: (() -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                // The same reason the journal's dock scrolls: a row of tools
                // that overflows a narrow phone must never clip the last one.
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // TEXT HISTORY, the dock's first tool — the same placement the
            // journal page's own dock gives it.
            if (onHistory != null) {
                TextHistoryPill(onClick = onHistory, size = 30.dp)
                Spacer(Modifier.width(3.dp))
            }
            RichTextDockButton("Bold", boldActive, accent, ink, enabled, onBold) {
                CurioIcon(CurioIcons.FormatBold, null, size = 20.dp)
            }
            RichTextDockButton("Italic", italicActive, accent, ink, enabled, onItalic) {
                CurioIcon(CurioIcons.FormatItalic, null, size = 20.dp)
            }
            RichTextDockButton("Underline", underlineActive, accent, ink, enabled, onUnderline) {
                CurioIcon(CurioIcons.FormatUnderline, null, size = 20.dp)
            }
            RichTextDockButton("Highlight", highlightActive, accent, ink, enabled, onHighlight) {
                CurioIcon(CurioIcons.FormatHighlight, null, size = 20.dp)
            }
            // One text-size door — the A+/A− pair's single button, wearing the
            // dock's own look instead of the compact strip's chip.
            SizePickerButton(
                icon = CurioIcons.TextIncrease,
                label = "Text size",
                active = sizeActive,
                accent = accent,
                enabled = enabled,
                currentSp = currentSp,
                onPick = onSizePick,
                dock = true
            )
            // The two tools the app's full-screen editors asked for: a line's
            // own JUSTIFICATION (left / centre / right / justified) and the HAND
            // it is written in. Both are drawn rather than looked up — the
            // bundled icon subset has no alignment marks.
            RichTextDockMenu(
                label = "Alignment",
                active = alignKey != null,
                accent = accent,
                ink = ink,
                enabled = enabled,
                currentKey = alignKey,
                options = RICH_ALIGN_OPTIONS,
                onPick = onAlign,
                glyph = { RichAlignGlyph(alignKey) }
            )
            RichTextDockMenu(
                label = "Font",
                active = fontKey != null,
                accent = accent,
                ink = ink,
                enabled = enabled,
                currentKey = fontKey,
                options = RICH_FONT_OPTIONS,
                onPick = onFont,
                glyph = { RichFontGlyph() }
            )
            // The sheet this field is written on, and the colour of it.
            if (paper) {
                RichTextPaperMenu(
                    style = paperStyle,
                    onStyleChange = onPaperStyleChange,
                    color = paperColor,
                    onColorChange = onPaperColorChange,
                    showColorTool = showColorTool,
                    accent = accent,
                    ink = ink,
                    enabled = enabled
                )
            }
            // …and whatever the field itself rides (the dictation mic).
            if (trailingAction != null) {
                Spacer(Modifier.width(2.dp))
                trailingAction()
            }
        }
    }
}

/**
 * THE PAPER TOOLS, ON THE DOCK (v391).
 *
 * The strip above the field used to unfold the style toggle and the colour
 * swatches under the tools; with that strip gone in DOCK mode they live behind
 * one Palette button here — the glyph wears the sheet's live colour, so the
 * button answers "which paper is this?" without being opened, and the swatches
 * are already out when it is.
 */
@Composable
private fun RichTextPaperMenu(
    style: NotePaperStyle,
    onStyleChange: (NotePaperStyle) -> Unit,
    color: NotePaperColor,
    onColorChange: (NotePaperColor) -> Unit,
    showColorTool: Boolean,
    accent: Color,
    ink: Color,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        RichTextDockButton("Paper", expanded, accent, ink, enabled, { expanded = !expanded }) {
            Box(contentAlignment = Alignment.Center) {
                CurioIcon(CurioIcons.Palette, null, size = 20.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(notePaperSurface(color))
                )
            }
        }
        CurioDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            accent = accent
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NotePaperStyleToggle(
                    style = style,
                    onStyleChange = onStyleChange,
                    accent = accent,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showColorTool) {
                    NotePaperColorToggle(
                        color = color,
                        onColorChange = onColorChange,
                        accent = accent,
                        enabled = enabled,
                        // The menu IS the colour's own door — one tap to the
                        // swatches rather than two.
                        startExpanded = true
                    )
                }
            }
        }
    }
}

/** The four ways a line can sit, and the four hands it can be written in. */
private val RICH_ALIGN_OPTIONS: List<Pair<String?, String>> = listOf(
    null to "Left",
    "center" to "Centred",
    "end" to "Right",
    "justify" to "Justified"
)

private val RICH_FONT_OPTIONS: List<Pair<String?, String>> = listOf(
    null to "Default",
    "book" to "Book serif",
    "writing" to "Writing hand",
    "display" to "Display serif"
)

/** A dock button that opens its own choices — the shape behind the two menus. */
@Composable
private fun RichTextDockMenu(
    label: String,
    active: Boolean,
    accent: Color,
    ink: Color,
    enabled: Boolean,
    currentKey: String?,
    options: List<Pair<String?, String>>,
    onPick: (String?) -> Unit,
    glyph: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        RichTextDockButton(label, active, accent, ink, enabled, { expanded = true }) { glyph() }
        CurioDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            accent = accent
        ) {
            options.forEach { (key, text) ->
                CurioDropdownItem(
                    text = { Text(text) },
                    selected = key == currentKey,
                    accent = accent,
                    trailingIcon = if (key == currentKey) {
                        { CurioIcon(CurioIcons.Check, null, tint = accent, size = 16.dp) }
                    } else null,
                    onClick = {
                        expanded = false
                        onPick(key)
                    }
                )
            }
        }
    }
}

/**
 * The alignment mark, DRAWN: four hairlines laid out the way the line sits.
 * The bundled icon subset carries no alignment glyphs (the journal's own dock
 * draws its too), so a justified line is the one whose hairlines all run full
 * width — right, centre and left step in on the side they lean to.
 */
@Composable
private fun RichAlignGlyph(key: String?) {
    val ink = LocalContentColor.current
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.8f.dp.toPx()
        val width = size.width
        val fractions = if (key == "justify") listOf(1f, 0.55f, 1f, 0.55f)
        else listOf(1f, 0.68f, 1f, 0.68f)
        fractions.forEachIndexed { index, fraction ->
            val y = size.height * (0.24f + index * 0.18f)
            val run = width * fraction
            val x = when (key) {
                "center" -> (width - run) / 2f
                "end" -> width - run
                else -> 0f
            }
            drawLine(
                color = ink,
                start = androidx.compose.ui.geometry.Offset(x, y),
                end = androidx.compose.ui.geometry.Offset(x + run, y),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

/** The font tool wears its own capital A — the letter IS the tool. */
@Composable
private fun RichFontGlyph() {
    val ink = LocalContentColor.current
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.8f.dp.toPx()
        val w = size.width
        val h = size.height
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(
            color = ink,
            start = androidx.compose.ui.geometry.Offset(x1, y1),
            end = androidx.compose.ui.geometry.Offset(x2, y2),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        line(w * 0.18f, h * 0.84f, w * 0.5f, h * 0.18f)
        line(w * 0.5f, h * 0.18f, w * 0.82f, h * 0.84f)
        line(w * 0.31f, h * 0.60f, w * 0.69f, h * 0.60f)
    }
}

/** One tool of the journal-style dock — the journal's `PersonalToolButton`. */
@Composable
private fun RichTextDockButton(
    label: String,
    active: Boolean,
    accent: Color,
    ink: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (active) accent.copy(alpha = 0.24f) else Color.Transparent,
        modifier = Modifier.size(36.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
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

/** Compact B / I / highlighter toolbar row. */
@Composable
private fun FormatToolbar(
    boldActive: Boolean,
    italicActive: Boolean,
    highlightActive: Boolean,
    sizeActive: Boolean,
    accent: Color,
    enabled: Boolean,
    currentSp: Float,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onHighlight: () -> Unit,
    onSizePick: (Float) -> Unit,
    modifier: Modifier = Modifier,
    paper: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FormatToolButton(CurioIcons.FormatBold, "Bold", boldActive, accent, enabled, onBold, paper = paper)
        FormatToolButton(CurioIcons.FormatItalic, "Italic", italicActive, accent, enabled, onItalic, paper = paper)
        FormatToolButton(CurioIcons.FormatHighlight, "Highlight", highlightActive, accent, enabled, onHighlight, paper = paper)
        // Text size — tapping opens a dropdown of fixed sizes; picking one
        // applies it to the selection (if any) and arms it as the sticky
        // size so the next text typed carries it. The button stays lit
        // while armed — the true "active" state (the old step buttons lit
        // from whatever size sat under the caret, armed or not). The
        // former A+/A− pair both opened this same dropdown, so there's a
        // single button now.
        SizePickerButton(
            icon = CurioIcons.TextIncrease,
            label = "Text size",
            active = sizeActive,
            accent = accent,
            enabled = enabled,
            currentSp = currentSp,
            onPick = onSizePick
        )
    }
}

@Composable
private fun ToolToggleButton(
    icon: String,
    label: String,
    expanded: Boolean,
    accent: Color,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    /** Live swatch — e.g. the current paper color, shown before the icon. */
    dot: Color? = null
) {
    // Theme-aware on the dock: collapsed sits on a tonal surface chip with
    // the theme's muted tokens, expanded blooms in the accent container —
    // no hardcoded dark-mode alphas.
    val ink = if (expanded) accent else MaterialTheme.colorScheme.onSurfaceVariant
    // v27n — the expanded fill is OPAQUE (was 18% alpha, which let the
    // elevation shadow bleed through).
    val fill = if (expanded) {
        lerp(MaterialTheme.colorScheme.surfaceContainerHighest, accent, 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val rim = if (expanded) accent.copy(alpha = 0.65f)
              else MaterialTheme.colorScheme.outlineVariant
    Surface(
        onClick = onToggle,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = fill,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dot != null) {
                Box(
                    modifier = Modifier
                        // v27n — shadow BEFORE the fill (was painted on top
                        // of the color dot).
                        .shadow(1.dp, CircleShape)
                        .size(12.dp)
                        .background(dot, CircleShape)
                )
            }
            CurioIcon(
                name = icon,
                contentDescription = if (expanded) "Hide $label" else "Show $label",
                tint = ink,
                size = 16.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ink
            )
        }
    }
}

@Composable
// v375 — internal for the share card's floating bar buttons.
internal fun FormatToolButton(
    icon: String,
    label: String,
    active: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // v27r — note-paper toolbars ride a FIXED paper control accent (amber
    // in dark / brown in light), so their active fill is a MODERATED tint
    // of the accent with the accent itself as glyph ink — a solid amber
    // block was too saturated and white-on-amber unreadable. Category-accent
    // toolbars (paper = false) keep the solid accent + on-fill ink.
    paper: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = when {
            active && paper -> lerp(MaterialTheme.colorScheme.surfaceContainerHighest, accent, 0.45f)
            active -> accent
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        CurioIcon(
            name = icon,
            contentDescription = label,
            // Theme-aware: inactive tools follow the theme's muted ink (which
            // reads on the dock surface AND the floating bar in every theme),
            // active tools flip to the on-fill ink (solid accent) or the
            // accent itself on the tinted paper fill.
            tint = if (active) (if (paper) accent else pastelFillInk(accent))
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            size = 16.dp,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
        )
    }
}

/**
 * The A+/A− letter-size control. Tapping opens a dropdown of the fixed
 * sizes in [SIZE_OPTIONS] (plus "Default"), and picking one applies it to
 * the selection (if any) AND arms it as the sticky size — so the icon stays
 * lit while armed and the next text typed carries that size. That lit state
 * is the true "active" state (the old step buttons lit from whatever size
 * happened to sit under the caret, armed or not). Picking "Default"
 * restores the field's base size and un-arms.
 */
@Composable
private fun SizePickerButton(
    icon: String,
    label: String,
    active: Boolean,
    accent: Color,
    enabled: Boolean,
    currentSp: Float,
    onPick: (Float) -> Unit,
    paper: Boolean = false,
    /** Wears the journal dock's own button instead of the compact strip's
     *  chip (the full-screen editors' dock). */
    dock: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        if (dock) {
            RichTextDockButton(
                label = label,
                active = active,
                accent = accent,
                ink = MaterialTheme.colorScheme.onSurfaceVariant,
                enabled = enabled,
                onClick = { expanded = true }
            ) {
                CurioIcon(icon, null, size = 20.dp)
            }
        } else FormatToolButton(
            icon = icon,
            label = label,
            active = active,
            accent = accent,
            enabled = enabled,
            onClick = { expanded = true },
            paper = paper
        )
        // v30 — the shared accent-themed menu: the current size row lights
        // up in the format accent with a trailing check.
        CurioDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            accent = accent
        ) {
            // "Default" first — the field's base size, checked when nothing
            // is armed (or the base size is current).
            CurioDropdownItem(
                text = {
                    Text(
                        "Default · ${BASE_FONT_SP.toInt()}sp",
                        fontWeight = FontWeight.Medium
                    )
                },
                selected = currentSp == BASE_FONT_SP,
                accent = accent,
                trailingIcon = if (currentSp == BASE_FONT_SP) {
                    {
                        CurioIcon(
                            name = CurioIcons.Check,
                            contentDescription = null,
                            tint = accent,
                            size = 16.dp
                        )
                    }
                } else null,
                onClick = {
                    expanded = false
                    onPick(BASE_FONT_SP)
                }
            )
            HorizontalDivider(color = accent.copy(alpha = 0.2f))
            SIZE_OPTIONS.forEach { sp ->
                CurioDropdownItem(
                    text = { Text("${sp.toInt()} sp", fontSize = sp.sp) },
                    selected = sp == currentSp,
                    accent = accent,
                    trailingIcon = if (sp == currentSp) {
                        {
                            CurioIcon(
                                name = CurioIcons.Check,
                                contentDescription = null,
                                tint = accent,
                                size = 16.dp
                            )
                        }
                    } else null,
                    onClick = {
                        expanded = false
                        onPick(sp)
                    }
                )
            }
        }
    }
}
