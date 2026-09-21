package com.curio.app.features.personal

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.text.LineBreaker
import android.net.Uri
import android.text.Layout
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.curio.app.R
import com.curio.app.data.PersonalAlign
import com.curio.app.data.PersonalAudioBars
import com.curio.app.data.PersonalBlock
import com.curio.app.data.PersonalDoc
import com.curio.app.data.PersonalRun
import com.curio.app.ui.components.formatRecordingTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * v424 — A PAGE THAT CAN LEAVE.
 *
 * The member: *"also a export format for the journal as pdf or supported format
 * with exact view in file"* — so a journal page can be carried out of Curio as a
 * file that looks like the page it came from, in the three shapes that matter:
 *
 *  · **PDF** — the page DRAWN, not dumped: the journal's own paper, its ink, its
 *    accent, the editorial serif, the heading sizes, the marker pens, the
 *    alignment, the photographs with their captions, the voice notes with their
 *    wave and their clock, and the checkboxes — paginated onto A4 at 150dpi with
 *    a foot that names the page and numbers it. It is built with Android's own
 *    [PdfDocument], so the words are REAL TEXT in the file (selectable, findable,
 *    copyable) rather than a picture of words, and Curio takes on no dependency
 *    for it.
 *  · **Text** — the words alone, with a photograph and a recording NAMED rather
 *    than silently dropped.
 *  · **Markdown** — the same words with their styling kept (bold, italic,
 *    underline, strike, headings, quotes, bullets, checkboxes), so a page can be
 *    read, pasted and versioned somewhere else.
 *
 * Nothing here is a screen: the exporter takes a document, writes a file into the
 * app's own export folder and hands it to Android's share sheet — because which
 * app a file goes to is the member's call, and Curio has no business guessing.
 */

/** The three files a page can leave as. */
internal enum class PersonalExportFormat(val label: String, val extension: String, val mime: String) {
    PDF("PDF", "pdf", "application/pdf"),
    TEXT("Text", "txt", "text/plain"),
    MARKDOWN("Markdown", "md", "text/markdown")
}

/** The file's own name, before a date and an extension are added. */
internal fun PersonalDoc.exportName(): String {
    val heading = blocks
        .firstOrNull { block -> block.text.isNotBlank() && block.runs.any { it.title } }
        ?.text
        ?.trim()
        .orEmpty()
    val name = heading.ifBlank { "Curio note" }
    return name
        .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(48)
        .ifBlank { "Curio note" }
}

/** The line a block contributes to a text or Markdown export. */
private fun exportLine(block: PersonalBlock): String = when {
    block.isPhoto -> block.caption.trim()
        .takeIf { it.isNotEmpty() }
        ?.let { "[Photo — $it]" }
        ?: "[Photo]"

    block.isAudio -> "[Voice note ${formatRecordingTime(block.audioSeconds)}]"

    else -> block.text
}

/**
 * THE WORDS, AS PLAIN TEXT.
 *
 * A photograph and a recording are NAMED rather than dropped: a page that talks
 * about its own pictures reads as nonsense without them, and a member who asked
 * for a text export asked for the page and not only for its text blocks.
 */
internal fun PersonalDoc.toExportText(): String {
    val parts = blocks.map { block -> exportLine(block) }
    return parts.filter { it.isNotBlank() }.joinToString("\n\n").trimEnd() + "\n"
}

/**
 * THE WORDS, WITH THEIR STYLING — Markdown.
 *
 * Every run the page carries maps onto Markdown: bold and italic are `**` and
 * `*`, strike is `~~`, underline is `<u>` (Markdown never had one of its own), a
 * heading is `#`, a quote is `>`, a bullet is `-`, and a checklist row is `- [ ]`
 * / `- [x]`.
 */
internal fun PersonalDoc.toExportMarkdown(): String {
    val out = StringBuilder()
    blocks.forEach { block ->
        when {
            block.isPhoto -> {
                val caption = block.caption.trim()
                out.append("![")
                    .append(caption.ifBlank { "photo" })
                    .append("](")
                    .append(block.photo.orEmpty())
                    .append(")\n\n")
            }

            block.isAudio -> {
                out.append("**Voice note** · ")
                    .append(formatRecordingTime(block.audioSeconds))
                    .append("\n\n")
            }

            block.text.isBlank() -> Unit

            else -> {
                val prefix = when {
                    block.runs.any { it.title } -> "# "
                    block.runs.any { it.checkbox } -> if (block.checked) "- [x] " else "- [ ] "
                    block.runs.any { it.bullet } -> "- "
                    block.runs.any { it.quote } -> "> "
                    else -> ""
                }
                out.append(prefix)
                    .append(markdownInline(block.text, block.runs))
                    .append("\n\n")
            }
        }
    }
    return out.toString().trimEnd() + "\n"
}

/**
 * ONE BLOCK'S WORDS, WITH ITS RUNS WRAPPED.
 *
 * The text is cut at every run boundary and each piece is wrapped in the markers
 * of the run that covers it — the only shape that can turn a per-character mask
 * into nested Markdown without inventing an order the document never had.
 */
private fun markdownInline(text: String, runs: List<PersonalRun>): String {
    if (text.isEmpty() || runs.isEmpty()) return text
    val bounds = sortedSetOf(0, text.length)
    runs.forEach { run ->
        bounds.add(run.start.coerceIn(0, text.length))
        bounds.add(run.end.coerceIn(0, text.length))
    }
    val points = bounds.toList()
    val out = StringBuilder()
    for (index in 0 until points.size - 1) {
        val from = points[index]
        val to = points[index + 1]
        if (to <= from) continue
        var piece = text.substring(from, to)
        val run = runs.firstOrNull { it.start <= from && it.end >= to }
        if (run == null) {
            out.append(piece)
            continue
        }
        if (run.strike) piece = "~~$piece~~"
        if (run.underline) piece = "<u>$piece</u>"
        if (run.italic) piece = "*$piece*"
        if (run.bold) piece = "**$piece**"
        out.append(piece)
    }
    return out.toString()
}

// ────────────────────────────────────────────────────────────────────────────
// The PDF
// ────────────────────────────────────────────────────────────────────────────

/** A4 at 150dpi, in the file's own units. */
private const val PDF_PAGE_WIDTH = 1240
private const val PDF_PAGE_HEIGHT = 1754
private const val PDF_MARGIN = 110f

/**
 * THE JOURNAL COLUMN THE SHEET IS A FACSIMILE OF (v427).
 *
 * The read view's own page: a 360dp phone (`JournalReadView`), less the 22dp
 * gutter it keeps down each side.
 */
private const val PDF_PAGE_MEASURE_DP = 316f

/**
 * HOW MUCH OF THE SHEET ONE OF THE CANVAS' `sp` — OR `dp` — BECOMES (v427).
 *
 * Every size in this file is a size the CANVAS sets its page in — the body, a
 * heading, a small line, a quoted line, a print's label — multiplied by this one
 * number, every leading is the canvas' own line height for that size, and every
 * space (the air between two rows) is the canvas' own dp.
 *
 * AND THE NUMBER IS THE PAGE'S OWN MEASURE: the sheet's text column IS the
 * journal's column, one canvas dp to one sheet unit ([PDF_PAGE_MEASURE_DP]), so a
 * line on paper breaks where the same line breaks on screen, at the same size
 * against the column, and an entry takes as many sheets as its words need. That
 * is the member's own decision (asked and answered: "make the PDF a facsimile of
 * the journal column — same measure, bigger type, more pages"), and it is the
 * whole of the sheet's type scale — the sheet keeps only what is about PAPER: its
 * A4 edges, the margin around the column, the foot that names and numbers the
 * sheet, and how tall a picture may be.
 *
 * So the measure above is the one number that decides what the export IS. If the
 * sheet should read as a PRINTED PAGE instead of as the page — a 30-unit body,
 * ~68 characters a line, about half the sheets — that is the same column answer
 * for a 544dp page.
 */
private val PDF_UNITS_PER_SP =
    (PDF_PAGE_WIDTH - 2 * PDF_MARGIN) / PDF_PAGE_MEASURE_DP

/** One of the canvas' sizes, in the sheet's own units. */
private fun pdfPx(sp: TextUnit): Float = sp.value * PDF_UNITS_PER_SP

/** The sheet's body — the page's read-back body, at the sheet's measure. */
private val PDF_BODY_SIZE = pdfPx(BODY_VIEW_SIZE)

/** A heading, a small line and a quoted line, exactly as the page sets them. */
private val PDF_TITLE_SIZE = pdfPx(TITLE_VIEW_SIZE)
private val PDF_SMALL_SIZE = pdfPx(SMALL_VIEW_SIZE)
private val PDF_QUOTE_SIZE = pdfPx(QUOTE_VIEW_SIZE)

/**
 * THE LEADING, TAKEN THE SAME WAY — as the canvas' own ratio of line height to
 * size, so a paragraph on paper is set as openly as the same paragraph on the
 * page. A quoted line keeps the BODY's leading: on the page a quotation is set
 * inside the prose's own line, only a shade smaller.
 */
private val PDF_BODY_LEADING = BODY_VIEW_LINE.value / BODY_VIEW_SIZE.value
private val PDF_TITLE_LEADING = TITLE_VIEW_LINE.value / TITLE_VIEW_SIZE.value
private val PDF_SMALL_LEADING = SMALL_VIEW_LINE.value / SMALL_VIEW_SIZE.value

/** The air between two rows of the page read back, on paper. */
private val PDF_ROW_GAP = VIEW_ROW_GAP.value * PDF_UNITS_PER_SP

/** How much of the page's own ink a label's words carry, and its stamp. */
private const val PDF_LABEL_ALPHA = 158
private const val PDF_STAMP_ALPHA = 102

/**
 * THE SHEET'S FACES, RESOLVED ONCE FOR THE WHOLE FILE.
 *
 * The four a run can name, plus a print's LABEL — which is the print's own
 * typography ([PersonalCaptionFace]) and not a sheet decision at all (v427, user
 * request: "…and the caption face taken from the canvas"). Resolved once because
 * a page can carry a print every few rows and each one asks for its own face.
 */
private class PdfFonts(private val context: Context) {
    val body: Typeface? = ResourcesCompat.getFont(context, R.font.lora)
    val display: Typeface? = ResourcesCompat.getFont(context, R.font.fraunces)
    val sans: Typeface? = ResourcesCompat.getFont(context, R.font.geom)
    val mono: Typeface? = ResourcesCompat.getFont(context, R.font.space_mono)

    private val labels = mutableMapOf<PersonalCaptionFace, Typeface?>()

    /** A label's face — the same six bundled files the page draws it in. */
    fun label(face: PersonalCaptionFace): Typeface? = labels.getOrPut(face) {
        ResourcesCompat.getFont(
            context,
            when (face) {
                PersonalCaptionFace.PRINT, PersonalCaptionFace.SERIF -> R.font.lora
                PersonalCaptionFace.HAND -> R.font.patrick_hand_regular
                PersonalCaptionFace.DISPLAY -> R.font.playfair_display
                PersonalCaptionFace.MONO -> R.font.space_mono
                PersonalCaptionFace.POSTER -> R.font.bebas_neue
                PersonalCaptionFace.MODERN -> R.font.space_grotesk
            }
        )
    }
}

/**
 * ONE SHEET OF THE JOURNAL'S PAPER, AND THE CURSOR THAT FILLS IT.
 *
 * The foot names the page and numbers it, drawn as the sheet is CLOSED, so every
 * page carries one no matter what the last block left behind.
 */
private class PdfRun(
    private val document: PdfDocument,
    private val paper: Int,
    private val ink: Int,
    private val footer: String
) {
    private val contentLeft = PDF_MARGIN
    private val contentRight = PDF_PAGE_WIDTH - PDF_MARGIN
    private val contentTop = PDF_MARGIN
    private val contentBottom = PDF_PAGE_HEIGHT - PDF_MARGIN - 30f
    val contentWidth: Int = (contentRight - contentLeft).toInt()

    private var page: PdfDocument.Page? = null
    private var drawing: Canvas? = null
    private var number = 0
    var cursor = contentTop
        private set

    private fun openPage() {
        number += 1
        val info = PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, number).create()
        val opened = document.startPage(info)
        opened.canvas.drawColor(paper)
        page = opened
        drawing = opened.canvas
        cursor = contentTop
    }

    fun close() {
        val open = page ?: return
        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            alpha = 110
            textSize = 20f
        }
        val foot = open.canvas.height - 46f
        open.canvas.drawText(footer, contentLeft, foot, footPaint)
        footPaint.textAlign = Paint.Align.RIGHT
        open.canvas.drawText("$number", contentRight, foot, footPaint)
        document.finishPage(open)
        page = null
        drawing = null
    }

    /** A page, when there is not one already — never a blank page of its own. */
    fun ensurePage() {
        if (page == null) openPage()
    }

    /** True when the sheet has room for [height]; opens a page as needed. */
    fun want(height: Float): Boolean {
        if (page == null) openPage()
        if (cursor + height <= contentBottom) return true
        if (cursor <= contentTop) return false
        close()
        openPage()
        return cursor + height <= contentBottom
    }

    /**
     * A FRESH SHEET, and whether one was available: a page that has not been
     * written on has nowhere else to go, which is what stops a block taller than
     * a sheet from spinning between two pages forever.
     */
    fun breakPage(): Boolean {
        if (page == null) openPage()
        if (cursor <= contentTop) return false
        close()
        openPage()
        return true
    }

    fun surface(): Canvas {
        if (page == null) openPage()
        return drawing ?: Canvas()
    }

    fun advance(by: Float) {
        cursor += by
    }

    fun room(): Float = contentBottom - cursor

    fun left(): Float = contentLeft

    fun right(): Float = contentRight

    fun bottom(): Float = contentBottom
}

/**
 * v424 — THE PAGE, AS A PDF FILE.
 *
 * Everything is measured in the file's own pixels and laid out top to bottom: a
 * paragraph is a real [StaticLayout], so it wraps and can be SPLIT across pages;
 * a photograph is decoded no larger than it is drawn (never a full-size bitmap
 * for a thumbnail); a recording is its own small row. The colours come from the
 * caller — the journal's paper, ink and accent as they are on screen right now —
 * so the file looks like the page the member was reading.
 */
internal fun writePersonalPdf(
    context: Context,
    doc: PersonalDoc,
    paper: Int,
    ink: Int,
    accent: Int,
    highlightInk: (String) -> Int
): File {
    val fonts = PdfFonts(context)

    val document = PdfDocument()
    val run = PdfRun(document, paper, ink, doc.exportName())
    val gap = PDF_ROW_GAP

    for (block in doc.blocks) {
        when {
            block.isPhoto -> drawExportPhoto(context, fonts, run, block, ink)

            block.isAudio -> drawExportVoice(run, block, paper, ink, accent)

            block.text.isBlank() -> Unit

            else -> drawExportLayout(
                run = run,
                layout = exportLayout(
                    block = block,
                    width = run.contentWidth,
                    ink = ink,
                    accent = accent,
                    fonts = fonts,
                    paper = paper,
                    highlightInk = highlightInk
                ),
                gap = gap
            )
        }
    }

    // A page is OPENED only when there is not one — an empty note still leaves a
    // sheet of the journal's own paper, and a full one is never followed by a
    // blank page that carries nothing but its own foot.
    run.ensurePage()
    run.close()
    return writeExportFile(context, doc, PersonalExportFormat.PDF) { stream ->
        document.writeTo(stream)
        document.close()
    }
}

/** One paragraph as a real, wrappable, split-able layout. */
private fun exportLayout(
    block: PersonalBlock,
    width: Int,
    ink: Int,
    accent: Int,
    fonts: PdfFonts,
    /** The page's own colour — what a tick inside a ticked box is drawn in. */
    paper: Int,
    highlightInk: (String) -> Int
): StaticLayout {
    val titled = block.runs.any { it.title }
    val small = block.runs.any { it.small }
    // v427 — THE SIZE AND THE LEADING ARE THE CANVAS'.
    //
    // A heading takes the page's heading size, a small line its small size and
    // everything else the page's body, each on the leading the page gives that
    // size. The sheet used to carry its own multipliers (a heading was the body
    // "×1.45", a small line "×0.84") and one leading ratio for all three (1.42,
    // against the page's 27/16), which is why a paragraph on paper read tighter
    // than the same paragraph on the page.
    val size = when {
        titled -> PDF_TITLE_SIZE
        small -> PDF_SMALL_SIZE
        else -> PDF_BODY_SIZE
    }
    val leading = when {
        titled -> PDF_TITLE_LEADING
        small -> PDF_SMALL_LEADING
        else -> PDF_BODY_LEADING
    }
    // A FACE IS THE LINE'S OWN: the first run that named one wins, a heading falls
    // back to the display serif, and everything else is the page's writing face —
    // the same three rules the canvas follows.
    val face = when (block.runs.firstOrNull { it.font.isNotEmpty() }?.font) {
        "sans" -> fonts.sans
        "mono" -> fonts.mono
        "display" -> fonts.display
        else -> if (titled) fonts.display else fonts.body
    }
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = face ?: Typeface.SERIF
        textSize = size
        color = ink
    }
    val text = SpannableString(block.text)
    block.runs.forEach { run ->
        val from = run.start.coerceIn(0, text.length)
        val to = run.end.coerceIn(0, text.length)
        if (to <= from) return@forEach
        // A QUOTED SPAN IS SET AT THE PAGE'S QUOTATION SIZE — the page draws a
        // quotation inside the prose's own LINE, a shade smaller, so a line that
        // is a quotation throughout comes out as one and a line with a quoted
        // phrase in it keeps its prose and shrinks exactly that phrase. The
        // leading is the line's own either way, as it is on the page.
        if (run.quote) text.setSpan(AbsoluteSizeSpan(PDF_QUOTE_SIZE.toInt()), from, to, 0)
        if (run.bold) text.setSpan(StyleSpan(Typeface.BOLD), from, to, 0)
        if (run.italic) text.setSpan(StyleSpan(Typeface.ITALIC), from, to, 0)
        if (run.underline) text.setSpan(UnderlineSpan(), from, to, 0)
        if (run.strike) text.setSpan(StrikethroughSpan(), from, to, 0)
        if (run.highlight.isNotEmpty()) {
            val pen = highlightInk(run.highlight)
            if (pen != 0) text.setSpan(BackgroundColorSpan(pen), from, to, 0)
        }
    }
    // THE ROW'S OWN MARK, at the line's own height: a checkbox carries its tick, a
    // bullet its dot, a quote the rule down its side — the same three things the
    // page draws, as a leading margin, which is why the words wrap around it
    // exactly as they do on screen.
    val marker = block.runs.firstOrNull { it.checkbox }
        ?.let { ExportMarker.BOX }
        ?: block.runs.firstOrNull { it.bullet }?.let { ExportMarker.DOT }
        ?: block.runs.firstOrNull { it.quote }?.let { ExportMarker.RULE }
    if (marker != null) {
        text.setSpan(
            ExportMarkerSpan(
                kind = marker,
                checked = block.checked,
                ink = ink,
                accent = accent,
                paper = paper
            ),
            0,
            text.length,
            0
        )
    }

    val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(
            when (block.align) {
                PersonalAlign.START, PersonalAlign.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
                PersonalAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
                PersonalAlign.END -> Layout.Alignment.ALIGN_OPPOSITE
            }
        )
        .setLineSpacing(0f, leading)
        .setIncludePad(false)
    if (block.align == PersonalAlign.JUSTIFY) {
        builder.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD)
    }
    return builder.build()
}

/**
 * A LAYOUT, DRAWN ACROSS AS MANY PAGES AS IT NEEDS.
 *
 * A paragraph is split by LINES rather than kept whole, so a long entry cannot
 * push a sheet of blank paper onto the next page: the clip and the translation
 * are what draw the tail of a layout at the head of the new sheet.
 */
private fun drawExportLayout(run: PdfRun, layout: StaticLayout, gap: Float) {
    var line = 0
    while (line < layout.lineCount) {
        val available = run.room()
        val firstTop = layout.getLineTop(line)
        var lastFit = line
        while (lastFit + 1 < layout.lineCount &&
            layout.getLineBottom(lastFit + 1) - firstTop <= available
        ) {
            lastFit += 1
        }
        // Not even one line fits: a fresh sheet first, and when the sheet we are
        // on has already been written on there is nowhere left to go.
        if (layout.getLineBottom(lastFit) - firstTop > available) {
            if (!run.breakPage()) break
            continue
        }
        val canvas = run.surface()
        canvas.save()
        canvas.clipRect(0f, run.cursor, PDF_PAGE_WIDTH.toFloat(), run.cursor + available)
        canvas.translate(run.left(), run.cursor - firstTop)
        layout.draw(canvas)
        canvas.restore()
        run.advance((layout.getLineBottom(lastFit) - firstTop) + gap)
        line = lastFit + 1
    }
}

/** A photograph, at its own size, with its own label under it. */
private fun drawExportPhoto(
    context: Context,
    fonts: PdfFonts,
    run: PdfRun,
    block: PersonalBlock,
    ink: Int
) {
    val uri = block.photo?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() } ?: return
    val bitmap = decodeExportBitmap(context, uri, run.contentWidth) ?: return
    // v427 — THE PRINT KEEPS THE SIZE THE PAGE GAVE IT.
    //
    // Every print was decoded at the page's OWN measure and scaled down only
    // until it fitted, so a picture the member had set to Small, or to a portrait
    // frame, left the journal as a page-wide photograph — the member's "the pdf
    // isnt accurate, it doesnt show exactly as the journal view have". The box is
    // the print's own share of the measure now ([PersonalPhotoSize]'s fraction —
    // the same number the canvas splits a row of prints with), and the picture
    // keeps its own aspect inside that box, so nothing is distorted and a Small
    // print reads as small on paper exactly as it does on the page.
    //
    // The old clamp at 1f is gone with it: a small bitmap had to stay small, so a
    // low-resolution picture printed as a stamp no matter which size it wore.
    val size = PersonalPhotoSize.fromKey(block.photoSize)
    val boxWidth = (run.contentWidth.toFloat() * size.fraction).coerceAtLeast(1f)
    val maxHeight = (run.bottom() - PDF_MARGIN) * 0.68f
    val scale = minOf(
        boxWidth / bitmap.width.toFloat(),
        maxHeight / bitmap.height.toFloat()
    )
    val drawnWidth = bitmap.width * scale
    val drawnHeight = bitmap.height * scale
    // v427 — THE LABEL IS THE PAGE'S LABEL.
    //
    // The face is the print's own ([PersonalCaptionFace]) and the size is the
    // label's own (a member's Small / Standard / Large multiplies the page's
    // caption size), and the print's stamp rides under the words when it carries
    // one — where the sheet used to set every caption in Lora at a size of its
    // own. Both lines are cut to the PRINT's own measure, one line and then an
    // ellipsis, exactly as the page cuts them; the sheet ran them as wide as the
    // paper, so a sentence on a Small print arrived wider than its picture.
    val labelFace = personalCaptionFace(block.captionFace)
    val caption = block.caption.trim()
    val stamp = personalCaptionDateText(
        block.captionDateMillis,
        PersonalCaptionDates.order(context, block.captionOrder)
    )
    val labelSize =
        personalCaptionSizeSp(CAPTION_VIEW_SIZE, block.captionSize).value * PDF_UNITS_PER_SP
    val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fonts.label(labelFace)
        textSize = labelSize
        color = ink
        alpha = PDF_LABEL_ALPHA
        textAlign = Paint.Align.CENTER
    }
    // The stamp is the label's small print, so it rides the label's own size
    // instead of a size of its own — and it is spaced as the page spaces it.
    val stampPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = captionPaint.typeface
        textSize = labelSize * 0.76f
        color = ink
        alpha = PDF_STAMP_ALPHA
        letterSpacing = 0.6f * PDF_UNITS_PER_SP
        textAlign = Paint.Align.CENTER
    }
    val captionLine = if (caption.isEmpty()) {
        ""
    } else {
        TextUtils.ellipsize(caption, captionPaint, boxWidth, TextUtils.TruncateAt.END).toString()
    }
    val hasCaption = captionLine.isNotEmpty()
    val hasStamp = stamp.isNotEmpty()
    val captionRoom = when {
        hasCaption && hasStamp -> captionPaint.textSize * 1.7f + stampPaint.textSize * 1.9f
        hasCaption -> captionPaint.textSize * 1.7f
        hasStamp -> stampPaint.textSize * 1.9f
        else -> 0f
    }
    if (run.want(drawnHeight + captionRoom + 40f)) {
        val left = run.left() + (run.contentWidth - drawnWidth) / 2f
        val top = run.cursor
        run.surface().drawBitmap(
            bitmap,
            null,
            RectF(left, top, left + drawnWidth, top + drawnHeight),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        )
        run.advance(drawnHeight + 26f)
        val centre = run.left() + run.contentWidth / 2f
        if (hasCaption) {
            run.surface().drawText(captionLine, centre, run.cursor + captionPaint.textSize, captionPaint)
            run.advance(captionPaint.textSize * 1.7f)
        }
        if (hasStamp) {
            run.surface().drawText(stamp, centre, run.cursor + stampPaint.textSize, stampPaint)
            run.advance(stampPaint.textSize * 1.9f)
        }
        run.advance(18f)
    }
    bitmap.recycle()
}

/** A voice note as the page draws one: the play pill, the wave, the clock. */
private fun drawExportVoice(run: PdfRun, block: PersonalBlock, paper: Int, ink: Int, accent: Int) {
    val height = PDF_BODY_SIZE * 2.3f
    if (!run.want(height + 22f)) return
    val canvas = run.surface()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val top = run.cursor
    val pillWidth = height * 1.7f
    paint.color = accent
    canvas.drawRoundRect(
        RectF(run.left(), top, run.left() + pillWidth, top + height),
        height / 2f,
        height / 2f,
        paint
    )
    // The mark in the paper's own colour: a play triangle is the one control a
    // reader knows without being told.
    val centreX = run.left() + pillWidth / 2f
    val centreY = top + height / 2f
    val mark = Path().apply {
        moveTo(centreX - height * 0.11f, centreY - height * 0.19f)
        lineTo(centreX + height * 0.19f, centreY)
        lineTo(centreX - height * 0.11f, centreY + height * 0.19f)
        close()
    }
    paint.color = paper
    canvas.drawPath(mark, paint)

    val bars = PersonalAudioBars.decode(block.audioBars)
    val clock = formatRecordingTime(block.audioSeconds)
    val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        alpha = 190
        textSize = PDF_BODY_SIZE * 0.85f
        textAlign = Paint.Align.RIGHT
    }
    val waveLeft = run.left() + pillWidth + 24f
    val waveRight = run.right() - clockPaint.measureText(clock) - 24f
    if (bars.isNotEmpty() && waveRight > waveLeft) {
        val step = (waveRight - waveLeft) / bars.size
        val barWidth = (step * 0.42f).coerceAtLeast(1.6f)
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            alpha = 150
        }
        bars.forEachIndexed { index, level ->
            val reach = (level.coerceIn(0f, 1f) * 0.44f + 0.06f) * height * 0.72f
            val x = waveLeft + step * index
            canvas.drawRoundRect(
                RectF(x, centreY - reach, x + barWidth, centreY + reach),
                barWidth / 2f,
                barWidth / 2f,
                wavePaint
            )
        }
    }
    canvas.drawText(clock, run.right(), centreY + clockPaint.textSize * 0.36f, clockPaint)
    run.advance(height + 22f)
}

// ────────────────────────────────────────────────────────────────────────────
// The door: the dock's own export tool
// ────────────────────────────────────────────────────────────────────────────

/** What a dock needs from the exporter: whether it is working, and how to run. */
internal class PersonalExportRequest(
    val busy: Boolean,
    val failure: String?,
    val dismissFailure: () -> Unit,
    val run: (PersonalExportFormat) -> Unit
)

/**
 * v424 — THE DOCK'S EXPORT DOOR.
 *
 * Resolved in the composition because the colours ARE the composition's (the
 * journal's paper, its ink, its accent and each marker pen's own ink), while the
 * write happens off the UI thread: a long page is a few hundred milliseconds of
 * layout and drawing, and the page must not stutter for a file it is making.
 */
@Composable
internal fun rememberPersonalExporter(state: PersonalEditorState): PersonalExportRequest {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val paper = journalPaper().toArgb()
    val ink = journalInk().toArgb()
    val accent = personalAccentInk().toArgb()
    val penInks = PERSONAL_HIGHLIGHT_KEYS.associateWith { key ->
        personalHighlightInk(key).toArgb()
    }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    val run = remember(state, context, paper, ink, accent, penInks) {
        { format: PersonalExportFormat ->
            val doc = state.doc()
            if (!doc.isEmpty && !busy) {
                busy = true
                scope.launch {
                    val file = withContext(Dispatchers.IO) {
                        runCatching {
                            writePersonalExport(context, doc, format, paper, ink, accent, penInks)
                        }.getOrNull()
                    }
                    busy = false
                    if (file != null) {
                        sharePersonalExport(context, file, format)
                    } else {
                        failure = "This page could not be exported."
                    }
                }
            }
        }
    }
    return PersonalExportRequest(
        busy = busy,
        failure = failure,
        dismissFailure = { failure = null },
        run = run
    )
}

/**
 * THE THREE SHAPES, WRITTEN TO ONE FILE.
 *
 * The name is the page's own heading plus the day, so a folder of exports reads
 * as a shelf of pages rather than as a pile of `document (3).pdf`.
 */
internal fun writePersonalExport(
    context: Context,
    doc: PersonalDoc,
    format: PersonalExportFormat,
    paper: Int,
    ink: Int,
    accent: Int,
    penInks: Map<String, Int>
): File {
    if (format == PersonalExportFormat.PDF) {
        return writePersonalPdf(context, doc, paper, ink, accent) { key -> penInks[key] ?: 0 }
    }
    return writeExportFile(context, doc, format) { stream ->
        val body = if (format == PersonalExportFormat.MARKDOWN) {
            doc.toExportMarkdown()
        } else {
            doc.toExportText()
        }
        stream.write(body.toByteArray())
    }
}

/** The file itself, in the app's own export folder, named for the page and the day. */
private fun writeExportFile(
    context: Context,
    doc: PersonalDoc,
    format: PersonalExportFormat,
    write: (java.io.OutputStream) -> Unit
): File {
    val folder = File(context.cacheDir, "exports").apply { mkdirs() }
    val stamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val file = File(folder, "${doc.exportName()} $stamp.${format.extension}")
    file.outputStream().use { stream -> write(stream) }
    return file
}

/**
 * THE FILE LEAVES THROUGH ANDROID'S OWN SHEET.
 *
 * A chooser rather than a fixed target: the member decides whether a page goes to
 * their files, their mail or their cloud, and Curio never has to guess. The URI
 * is granted for reading only, for as long as the share lasts.
 */
internal fun sharePersonalExport(context: Context, file: File, format: PersonalExportFormat) {
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull() ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = format.mime
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(
            Intent.createChooser(intent, "Export ${format.label}")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Drawing helpers
// ────────────────────────────────────────────────────────────────────────────

/** What a row's leading margin draws: a dot, a box, or a quote's rule. */
private enum class ExportMarker { DOT, BOX, RULE }

/**
 * THE MARKER A ROW CARRIES, DRAWN AT ITS OWN LINE'S HEIGHT.
 *
 * The same three things the page draws — a dot, a box (ticked or not), and the
 * coffee rule down the side of a quote — as a leading margin, so the words wrap
 * around it exactly as they do on screen. A margin rather than characters in the
 * text, for the same reason it is one on the page: nobody's writing should be
 * littered with glyphs they never typed.
 */
private class ExportMarkerSpan(
    private val kind: ExportMarker,
    private val checked: Boolean,
    ink: Int,
    accent: Int,
    /** The page's own colour — what a tick is drawn in inside a ticked box. */
    paper: Int
) : LeadingMarginSpan {
    /**
     * THE MARK IS THE PAGE'S MARK (v427).
     *
     * A list line's box and its bullet are the canvas' own width
     * ([PERSONAL_MARKER_SIZE]) at the sheet's measure, the lead-in its words wrap
     * to is the canvas' own ([PERSONAL_MARKER_LEAD]), and the strokes inside are
     * the canvas' own ratios (`drawPersonalCheckbox`: a 0.085 outline and a 0.135
     * tick; `drawPersonalMarker`: a 0.17 dot). The sheet used to take the box
     * from the LINE instead (0.46 of the first line's height, which made its box
     * a different shape from the page's) and draw it with a hairline that no
     * longer matched once the type was the page's.
     */
    private val mark = PERSONAL_MARKER_SIZE.value * PDF_UNITS_PER_SP
    /** A waiting box's outline is SOFT ink on the page, not full ink. */
    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(ink, 107)
        style = Paint.Style.STROKE
        strokeWidth = mark * 0.085f
        strokeCap = Paint.Cap.ROUND
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = paper
        style = Paint.Style.STROKE
        strokeWidth = mark * 0.135f
        strokeCap = Paint.Cap.ROUND
    }

    /** The quote's own frame, where the rule stands and how wide it is. */
    private val quoteLead = QUOTE_LEAD.value * PDF_UNITS_PER_SP
    private val quoteRuleWidth = QUOTE_RULE_WIDTH.value * PDF_UNITS_PER_SP

    override fun getLeadingMargin(first: Boolean): Int = (
        if (kind == ExportMarker.RULE) quoteLead
        else (PERSONAL_MARKER_SIZE.value + PERSONAL_MARKER_GAP.value) * PDF_UNITS_PER_SP
        ).toInt()

    override fun drawLeadingMargin(
        canvas: Canvas,
        paint: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout
    ) {
        if (!first) return
        if (kind == ExportMarker.RULE) {
            canvas.drawRect(
                x.toFloat(),
                top.toFloat(),
                x + quoteRuleWidth,
                bottom.toFloat(),
                accentPaint
            )
            return
        }
        val size = mark
        val left = x.toFloat()
        // Centred on the LINE, as the page centres it (see drawPersonalMarker).
        val centre = (top + bottom) / 2f
        when (kind) {
            ExportMarker.DOT -> canvas.drawCircle(left + size / 2f, centre, size * 0.17f, accentPaint)

            // A CHECKED BOX IS A FILL OF THE PAGE'S ACCENT WITH A PAPER TICK, and
            // a waiting one a soft outline — the page's own two states (see
            // drawPersonalCheckbox), where the sheet drew both as an outline.
            ExportMarker.BOX -> {
                val boxTop = centre - size / 2f
                val corner = size * 0.30f
                if (checked) {
                    canvas.drawRoundRect(
                        RectF(left, boxTop, left + size, boxTop + size),
                        corner,
                        corner,
                        accentPaint
                    )
                    canvas.drawPath(
                        Path().apply {
                            moveTo(left + size * 0.24f, boxTop + size * 0.53f)
                            lineTo(left + size * 0.42f, boxTop + size * 0.70f)
                            lineTo(left + size * 0.77f, boxTop + size * 0.30f)
                        },
                        tickPaint
                    )
                } else {
                    canvas.drawRoundRect(
                        RectF(left, boxTop, left + size, boxTop + size),
                        corner,
                        corner,
                        inkPaint
                    )
                }
            }

            ExportMarker.RULE -> Unit
        }
    }
}

/** A photograph decoded no larger than it will be drawn. */
private fun decodeExportBitmap(context: Context, uri: Uri, targetWidth: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetWidth && sample < 8) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }.getOrNull()
}
