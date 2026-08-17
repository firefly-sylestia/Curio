package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
import com.curio.app.ui.theme.PatrickHandFontFamily

/**
 * The rich-text flags the toolbar can apply. [TextSpan] stores each as a
 * boolean so saved captures stay plain data + offsets.
 */
private enum class RichFlag { BOLD, ITALIC, HIGHLIGHT }

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
enum class RichTextToolbarMode {
    /** The Marginalia journal + quote cards (main option). */
    MAIN,

    /** Other text fields (Field Notes sections, Reel Notes review, …). */
    TOGGLE
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
                        background = if (sp.highlight) highlightColor else Color.Unspecified
                    ),
                    s, e
                )
            }
        }
    }

/**
 * Extracts the styled [TextSpan]s carried by an [AnnotatedString] — used to
 * read the editor's spans back out after Compose merges them while typing
 * (BasicTextField preserves span styles across edits, so no manual diffing).
 */
fun extractRichSpans(annotated: AnnotatedString): List<TextSpan> =
    annotated.spanStyles.mapNotNull { range ->
        val bold = range.item.fontWeight == FontWeight.Bold
        val italic = range.item.fontStyle == FontStyle.Italic
        val highlight = range.item.background != Color.Unspecified
        val size = range.item.fontSize
        val sizeSp = if (size.isSpecified) size.value else null
        if (!bold && !italic && !highlight && sizeSp == null) null
        else TextSpan(range.start, range.end, bold, italic, highlight, sizeSp)
    }.merged()

/** Sorts and merges adjacent/overlapping spans with identical flags. */
private fun List<TextSpan>.merged(): List<TextSpan> {
    if (isEmpty()) return emptyList()
    val sorted = sortedWith(compareBy<TextSpan> { it.start }.thenBy { it.end })
    val out = mutableListOf<TextSpan>()
    for (sp in sorted) {
        val last = out.lastOrNull()
        if (last != null && last.end >= sp.start &&
            last.bold == sp.bold && last.italic == sp.italic &&
            last.highlight == sp.highlight && last.fontSizeSp == sp.fontSizeSp
        ) {
            out[out.size - 1] = last.copy(end = maxOf(last.end, sp.end))
        } else {
            out.add(sp)
        }
    }
    return out
}

private fun TextSpan.has(flag: RichFlag): Boolean = when (flag) {
    RichFlag.BOLD -> bold
    RichFlag.ITALIC -> italic
    RichFlag.HIGHLIGHT -> highlight
}

/** True when every character of [s, e) is covered by a span carrying [flag]. */
private fun spansFullyCovered(spans: List<TextSpan>, s: Int, e: Int, flag: RichFlag): Boolean {
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
private fun rebaseSpans(oldText: String, newText: String, spans: List<TextSpan>): List<TextSpan> {
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
            s >= oldEnd -> out.add(
                TextSpan(s + delta, e + delta, sp.bold, sp.italic, sp.highlight, sp.fontSizeSp)
            )
            // Overlaps the changed region — keep only the untouched parts.
            else -> {
                if (s < prefix) out.add(TextSpan(s, prefix, sp.bold, sp.italic, sp.highlight, sp.fontSizeSp))
                if (e > oldEnd) out.add(
                    TextSpan(maxOf(s, oldEnd) + delta, e + delta, sp.bold, sp.italic, sp.highlight, sp.fontSizeSp)
                )
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
private fun toggleSpanFlag(spans: List<TextSpan>, s: Int, e: Int, flag: RichFlag, add: Boolean): List<TextSpan> {
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
    /** Optional trailing action (e.g. a small dictation button) rendered at
     *  the END of the field's own toolbar row, opposite the format toggle. */
    trailingAction: (@Composable () -> Unit)? = null,
    /** v125 — reports the field's focus state to the caller (e.g. to show a
     *  floating dictation mic only while the user is typing in this box). */
    onFocusChanged: ((Boolean) -> Unit)? = null,
    /** v7.19 — hides the note-paper COLOR swatch picker behind the paper
     *  style toggle (the mood board's quote boxes keep the color tool
     *  hidden while text formatting + paper style stay available). */
    showColorTool: Boolean = true
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
    LaunchedEffect(text, spans) {
        if (tfv.text != text) {
            tfv = TextFieldValue(buildRichAnnotated(text, spans, effectiveHighlight))
            // Different content loaded (e.g. editing another saved entry) —
            // drop any armed format from the previous text.
            pendingBold = false
            pendingItalic = false
            pendingHighlight = false
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
        if (pendingBold || pendingItalic || pendingHighlight || pendingSizeSp != null) {
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
        // Apply the format you just used to the selection to the NEXT text
        // typed, so "make this bold, then keep typing" works.
        when (flag) {
            RichFlag.BOLD -> pendingBold = add
            RichFlag.ITALIC -> pendingItalic = add
            RichFlag.HIGHLIGHT -> pendingHighlight = add
        }
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
        }
        // Picking the field default un-arms; any other size stays armed so
        // the next characters typed carry it (the icon stays lit).
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
        Surface(
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
                    ToolToggleButton(
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = tfv,
                        onValueChange = { emit(it) },
                        enabled = enabled,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            // Paper notes wear the handwritten Patrick Hand;
                            // plain (non-paper) fields keep the neutral sans.
                            fontFamily = if (paper) PatrickHandFontFamily else FontFamily.Default,
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
                            .onFocusChanged { onFocusChanged?.invoke(it.isFocused) }
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
                        fontFamily = if (paper) PatrickHandFontFamily else FontFamily.Default,
                        color = effectiveInk.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
        if (paper) {
            // v7.16 — universal style model: the base decides torn vs sharp
            // ruled paper and the style's flags drive every decoration, so
            // ALL combinations (incl. the new torn+red-margin / torn+rules+
            // decoration) render here — same flags as [NotePaperCard].
            if (paperStyle.torn) {
                TornPaperCard(
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
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
}

/**
 * Floating mini-toolbar shown at the text selection — B / I / highlight
 * that apply to the selected characters. Mirrors [FormatToolbar]'s buttons
 * in a compact floating chip so formatting existing text is discoverable.
 */
@Composable
private fun SelectionFormatBar(
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
private fun FormatToolButton(
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
    paper: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FormatToolButton(
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
