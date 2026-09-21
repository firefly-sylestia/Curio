package com.curio.app.features.personal

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.text.LineBreaker
import android.media.ExifInterface
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
import androidx.compose.ui.graphics.Color
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

/**
 * One canvas DP, in the sheet's own units.
 *
 * v427 — the sheet used to hold two scales: TEXT came from the canvas (`pdfPx`)
 * while the pictures and the voice notes were drawn at sizes the sheet invented
 * (`PDF_BODY_SIZE * 2.3` for a strip, a fitted box for a print). Everything a page
 * draws in dp — a print's frame height, the pad inside it, the wave's own strip
 * — is read through here now, so one canvas dp is one sheet unit for a drawing as
 * well as for a word (see [PDF_PAGE_MEASURE_DP]).
 */
private fun pdfDp(dp: Float): Float = dp * PDF_UNITS_PER_SP

/** The pad inside a print's paper frame — [PersonalCanvas]'s own 7dp. */
private val PDF_PRINT_PAD = pdfDp(7f)

/**
 * The pad inside the frame between the picture and its label — the page's own
 * 5dp band (`PersonalPhotoBlock`: `padding(top = 5.dp, bottom = 5.dp)`).
 */
private val PDF_PRINT_BAND = pdfDp(5f)

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

    /**
     * PUT THE CURSOR BACK — v427, for a ROW OF PRINTS. A row's cells share one
     * line, so the row draws them where it decided and moves the sheet once, by
     * the line's own height; `advance` can only add.
     */
    fun setCursor(at: Float) {
        cursor = at
    }

    /** The ink this sheet was opened with — a cell's label reads it too. */
    fun ink(): Int = ink

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

    // v427 — WHICH PRINTS ARE ONE ROW, decided the way the PAGE decides it (see
    // exportPrintPlan). A row is drawn once, by its first print; every later cell
    // and the blank rows the run stepped over are left out of the walk, exactly as
    // the page's own drawing pass leaves them out.
    val plan = exportPrintPlan(doc)

    for (block in doc.blocks) {
        when {
            plan.rowCells.contains(block.id) -> Unit

            // The line of a beside pair is drawn INSIDE the pair's own row (see
            // drawExportPrintBeside), so the column draws nothing for it.
            plan.besideLines.contains(block.id) -> Unit

            plan.rows.containsKey(block.id) -> drawExportPrintRow(
                context = context,
                fonts = fonts,
                run = run,
                doc = doc,
                ids = plan.rows.getValue(block.id)
            )

            plan.beside.containsKey(block.id) -> drawExportPrintBeside(
                context = context,
                fonts = fonts,
                run = run,
                doc = doc,
                block = block,
                lineId = plan.beside.getValue(block.id),
                accent = accent,
                paper = paper,
                highlightInk = highlightInk
            )

            block.isPhoto -> drawExportPhoto(context, fonts, run, block, ink)

            block.isAudio -> drawExportVoice(fonts, run, block, paper, ink, accent)

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

/**
 * A LONE PRINT: the page's own frame for the size it wears, at the page's own
 * place (the measure's left edge, its own share of the width).
 */
private fun drawExportPhoto(
    context: Context,
    fonts: PdfFonts,
    run: PdfRun,
    block: PersonalBlock,
    ink: Int
) {
    val size = PersonalPhotoSize.fromKey(block.photoSize)
    drawExportPrint(
        context = context,
        fonts = fonts,
        run = run,
        block = block,
        ink = ink,
        left = run.left(),
        frameWidth = (run.contentWidth.toFloat() * size.fraction).coerceAtLeast(1f),
        frameHeight = pdfDp(personalPrintHeight(size).value)
    )
}

/**
 * ONE PRINT, AT A PLACE AND A SIZE THE CALLER OWNS — as the page draws one.
 *
 * v427 — THE SHEET USED TO FIT THE PICTURE WHERE THE PAGE CROPS IT.
 *
 * A print on the page is a FRAME of two fixed numbers — the size's own share of
 * the measure as its width ([PersonalPhotoSize.fraction]) and the size's own
 * height ([personalPrintHeight]) — and the photograph is CROPPED to fill it
 * (`ContentScale.Crop`, see `PersonalPagePhoto`). The sheet read the photograph
 * the other way round: it decoded the whole file, fitted all of it inside the
 * box and drew it whole. So the same print the member looked at — a letterbox
 * band of a photograph, or an upright frame of one — left the journal as a small
 * complete picture, at its own aspect, at a height nothing on the page had ever
 * given it: their "in pdf export the photos are not visible as they are in the
 * preview of journal".
 *
 * The print is the page's own geometry now: the frame's width and height, the
 * picture cropped to that frame exactly as `Crop` crops it, the frame's own pad,
 * the label inside the frame — and the photograph arrives UPRIGHT, because the
 * page renders it through Coil, which reads EXIF, while a bare `BitmapFactory`
 * decode does not ([decodeExportBitmap]).
 *
 * It is also a PLACE, not a cursor move: the row decides how wide and how tall
 * each of its cells is and where it stands (see [drawExportPrintRow]), so the
 * sizing is the caller's and this function only draws the print.
 */
private fun drawExportPrint(
    context: Context,
    fonts: PdfFonts,
    run: PdfRun,
    block: PersonalBlock,
    ink: Int,
    left: Float,
    frameWidth: Float,
    frameHeight: Float
) {
    val uri = block.photo?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() } ?: return
    val innerWidth = (frameWidth - PDF_PRINT_PAD * 2f).coerceAtLeast(1f)
    val bitmap = decodeExportBitmap(context, uri, innerWidth.toInt(), frameHeight.toInt())
        ?: return
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
        TextUtils.ellipsize(caption, captionPaint, innerWidth, TextUtils.TruncateAt.END).toString()
    }
    val hasCaption = captionLine.isNotEmpty()
    val hasStamp = stamp.isNotEmpty()
    val captionRoom = when {
        hasCaption && hasStamp -> captionPaint.textSize * 1.7f + stampPaint.textSize * 1.9f
        hasCaption -> captionPaint.textSize * 1.7f
        hasStamp -> stampPaint.textSize * 1.9f
        else -> 0f
    }
    // The frame's own height IS the picture plus its band — the page draws a print
    // as a picture with a caption strip under it, so the strip is part of the
    // print and not air the sheet adds afterwards.
    val height = frameHeight + PDF_PRINT_BAND + captionRoom
    if (run.want(height + PDF_ROW_GAP)) {
        drawExportCell(
            run = run,
            bitmap = bitmap,
            top = run.cursor,
            left = left,
            innerWidth = innerWidth,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            captionLine = captionLine,
            captionPaint = captionPaint,
            stamp = stamp,
            stampPaint = stampPaint
        )
        run.advance(height + PDF_ROW_GAP)
    }
    bitmap.recycle()
}

/**
 * THE BAND A PRINT'S LABEL NEEDS, for a caller that must know a cell's full height
 * before it draws anything (a row: its line is as tall as its tallest cell). The
 * same numbers [drawExportPrint] measures its own caption with.
 */
private fun exportCaptionRoom(fonts: PdfFonts, block: PersonalBlock): Float {
    val labelSize =
        personalCaptionSizeSp(CAPTION_VIEW_SIZE, block.captionSize).value * PDF_UNITS_PER_SP
    val face = personalCaptionFace(block.captionFace)
    val hasCaption = block.caption.isNotBlank()
    val hasStamp = block.captionDateMillis > 0L
    val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fonts.label(face)
        textSize = labelSize
        textAlign = Paint.Align.CENTER
    }
    return when {
        hasCaption && hasStamp -> captionPaint.textSize * 1.7f + captionPaint.textSize * 0.76f * 1.9f
        hasCaption -> captionPaint.textSize * 1.7f
        hasStamp -> captionPaint.textSize * 0.76f * 1.9f
        else -> 0f
    }
}

/**
 * THE PRINT ITSELF, AT [top] — the picture, then the label and its stamp inside
 * the frame's own width. It draws; it does NOT move the sheet's cursor.
 *
 * That is the whole point of it being separate: a ROW's cells share one line, so
 * the row decides where they stand and how far the page moves under them (see
 * [drawExportPrintRow]) — a per-print cursor move cannot place the second half of
 * a pair on the same line as the first.
 */
private fun drawExportCell(
    run: PdfRun,
    bitmap: Bitmap,
    top: Float,
    left: Float,
    innerWidth: Float,
    frameWidth: Float,
    frameHeight: Float,
    captionLine: String,
    captionPaint: TextPaint,
    stamp: String,
    stampPaint: TextPaint
) {
    // THE CROP IS THE PAGE'S CROP: the picture is scaled so it COVERS the
    // frame's inner box, then the middle of it is the part that shows — the
    // same window `ContentScale.Crop` opens on the page, so a print keeps the
    // shape it wears there whether the photograph is a panorama or upright.
    val cover = maxOf(
        innerWidth / bitmap.width.toFloat(),
        frameHeight / bitmap.height.toFloat()
    )
    val sourceWidth = (innerWidth / cover).toInt().coerceIn(1, bitmap.width)
    val sourceHeight = (frameHeight / cover).toInt().coerceIn(1, bitmap.height)
    val sourceX = ((bitmap.width - sourceWidth) / 2f).toInt().coerceAtLeast(0)
    val sourceY = ((bitmap.height - sourceHeight) / 2f).toInt().coerceAtLeast(0)
    val source = Rect(sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight)
    val innerLeft = left + PDF_PRINT_PAD
    run.surface().drawBitmap(
        bitmap,
        source,
        RectF(innerLeft, top, innerLeft + innerWidth, top + frameHeight),
        Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    )
    val centre = left + frameWidth / 2f
    var line = top + frameHeight + PDF_PRINT_BAND
    if (captionLine.isNotEmpty()) {
        run.surface().drawText(captionLine, centre, line + captionPaint.textSize, captionPaint)
        line += captionPaint.textSize * 1.7f
    }
    if (stamp.isNotEmpty()) {
        run.surface().drawText(stamp, centre, line + stampPaint.textSize, stampPaint)
    }
}

// ── A ROW OF PRINTS, AS THE PAGE LAYS ONE OUT (v427) ───────────────────────
//
// The sheet drew every print alone, full measure, whatever row it stood in on the
// page (the member: "make the PDF draw print stacks and rows as the journal lays
// them out"). The page builds ROWS — two, three or four prints, each cell taking
// its own size's share of the measure and its own size's height (see
// `PersonalPrintArrangement`) — so the sheet builds the same rows, from the same
// numbers, with the canvas' own `PRINT_ROW_GAP` and `printBesideShare` rather than
// gaps and shares of its own invention.

/**
 * WHICH PRINTS ARE ONE ROW, and which line sits BESIDE which print.
 *
 * The same pass the page runs (see the editor's row builder and the read view's
 * twin): a run of prints up to `PRINT_ROW_LIMIT`, stepping over blank rows that
 * are nobody's place, and a lone SMALL print taking the line under it as its
 * companion. The sheet has no caret, so it takes the READ VIEW's version of the
 * air rule (a blank row with no voice note in it) — the one condition the two
 * passes must never disagree about is which pictures are one row, and the caret
 * is not a fact a file can carry.
 */
private class ExportPrintPlan(
    /** The first print of a row -> every print in it, in order. */
    val rows: Map<String, List<String>>,
    /** Every print of a row after the first, and every blank row it stepped over. */
    val rowCells: Set<String>,
    /** A print's id -> the line drawn beside it. */
    val beside: Map<String, String>
) {
    /** The lines drawn INSIDE a beside pair's row, so they are not drawn twice. */
    val besideLines: Set<String> = beside.values.toSet()
}

private fun exportPrintPlan(doc: PersonalDoc): ExportPrintPlan {
    val rows = LinkedHashMap<String, List<String>>()
    val rowCells = linkedSetOf<String>()
    val beside = LinkedHashMap<String, String>()
    val blocks = doc.blocks
    var i = 0
    while (i < blocks.size) {
        val first = blocks[i]
        if (first.isPhoto) {
            val run = ArrayList<String>()
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
                rows[run.first()] = run
                run.drop(1).forEach { rowCells.add(it) }
                gaps.forEach { rowCells.add(it) }
                i = j
                continue
            }
            val next = blocks.getOrNull(i + 1)
            if (next != null &&
                PersonalPhotoSize.fromKey(first.photoSize) == PersonalPhotoSize.SMALL &&
                !next.isPhoto && !next.isAudio && next.text.isNotBlank()
            ) {
                beside[first.id] = next.id
                i += 2
                continue
            }
        }
        i += 1
    }
    return ExportPrintPlan(rows, rowCells, beside)
}

/** The size a row's member wears, and the three readings of it the page uses. */
private fun rowSizeOf(doc: PersonalDoc, id: String): PersonalPhotoSize =
    PersonalPhotoSize.fromKey(doc.blocks.firstOrNull { it.id == id }?.photoSize)

private fun rowWeightOf(doc: PersonalDoc, id: String): Float =
    rowSizeOf(doc, id).fraction.coerceIn(0.3f, 1f)

private fun rowHeightOf(doc: PersonalDoc, id: String): Float =
    pdfDp(personalPrintHeight(rowSizeOf(doc, id)).value)

/**
 * ONE ROW, THE SHAPE THE PAGE GIVES IT: a pair side by side, an upright frame
 * with two stacked beside it, or four as a square — each cell placed and sized by
 * its own size, exactly as `PersonalPrintArrangement` places them.
 *
 * The row is decided and drawn as ONE block: it either fits the sheet it is on or
 * it starts the next one, which is what a page's own Column does for a row it
 * cannot split.
 */
private fun drawExportPrintRow(
    context: Context,
    fonts: PdfFonts,
    run: PdfRun,
    doc: PersonalDoc,
    ids: List<String>
) {
    if (ids.size < 2) return
    val gap = pdfDp(PRINT_ROW_GAP.value)
    val measure = run.contentWidth.toFloat()
    // The lines of the row, placed before anything is drawn, so the row's own
    // height can be asked of the sheet first.
    val lines = ArrayList<List<ExportPrintCell>>()
    if (ids.size == 3) {
        // ── A THREE: the frame, and the two stacked beside it ──────────────
        //
        // The page's own rule: whichever member's size asked to stand up takes
        // the frame (the first print does when none asked), the frame takes at
        // least the widest print beside it, and each stacked print keeps its own
        // share of its column.
        val tall = ids.firstOrNull { PersonalPhotoSize.isUpright(rowSizeOf(doc, it)) } ?: ids[0]
        val stacked = ids.filterNot { it == tall }
        val stackedShare = stacked.maxOf { rowWeightOf(doc, it) }
        val tallShare = maxOf(rowWeightOf(doc, tall), stackedShare)
        val total = tallShare + stackedShare
        val room = (measure - gap).coerceAtLeast(1f)
        val tallWidth = tallShare / total * room
        val columnWidth = stackedShare / total * room
        val tallHeight = exportCellHeight(doc, fonts, tall)
        // The frame and the column are ONE line — the column's two prints stack
        // inside it — so the line is as tall as the taller of the two, and the
        // stacked pair carries its own offset down that line.
        val columnHeight = stacked.sumOf { exportCellHeight(doc, fonts, it).toDouble() }
            .toFloat() + gap * (stacked.size - 1)
        val cells = ArrayList<ExportPrintCell>(3)
        cells.add(ExportPrintCell(tall, run.left(), tallWidth, rowHeightOf(doc, tall), 0f))
        var y = 0f
        stacked.forEach { id ->
            cells.add(
                ExportPrintCell(
                    id,
                    run.left() + tallWidth + gap,
                    rowWeightOf(doc, id) / stackedShare * columnWidth,
                    rowHeightOf(doc, id),
                    y
                )
            )
            y += exportCellHeight(doc, fonts, id) + gap
        }
        drawExportLines(
            context = context,
            fonts = fonts,
            run = run,
            doc = doc,
            lines = listOf(cells),
            lineHeights = listOf(maxOf(tallHeight, columnHeight))
        )
        return
    }
    val chunked = if (ids.size == 2) listOf(ids) else ids.chunked(2)
    chunked.forEach { line ->
        // One line of the row: its cells split the measure by their OWN sizes'
        // shares, with the page's own gap between them. (A four is two lines of
        // two, and each line weighs itself — exactly as the page's own Column of
        // Rows does, so a four reads as the square the member built.)
        val total = line.sumOf { rowWeightOf(doc, it).toDouble() }.toFloat()
        val room = (measure - gap * (line.size - 1)).coerceAtLeast(1f)
        var x = run.left()
        lines.add(
            line.map { id ->
                val width = rowWeightOf(doc, id) / total * room
                ExportPrintCell(id, x, width, rowHeightOf(doc, id), 0f).also { x += width + gap }
            }
        )
    }
    drawExportLines(
        context = context,
        fonts = fonts,
        run = run,
        doc = doc,
        lines = lines,
        lineHeights = lines.map { line -> line.maxOf { exportCellHeight(doc, fonts, it.id) } }
    )
}

/**
 * A PRINT WITH ITS LINE BESIDE IT — the page's beside pair.
 *
 * The page draws a lone SMALL print with the line under it as one row: the print
 * takes `printBesideShare(size)` of the measure (Small 44/56, Small portrait
 * 36/64, Half and up an even half), the line takes the rest, and 10dp sits between
 * them (see the read view's beside branch). The sheet drew the print at the full
 * measure and then the line under it, so the pair the member built came out as two
 * stacked rows on paper.
 */
private fun drawExportPrintBeside(
    context: Context,
    fonts: PdfFonts,
    run: PdfRun,
    doc: PersonalDoc,
    block: PersonalBlock,
    lineId: String,
    accent: Int,
    paper: Int,
    highlightInk: (String) -> Int
) {
    val lineBlock = doc.blocks.firstOrNull { it.id == lineId } ?: return
    val size = PersonalPhotoSize.fromKey(block.photoSize)
    val gap = pdfDp(10f)
    val measure = run.contentWidth.toFloat()
    val share = printBesideShare(size)
    val printWidth = (measure - gap) * share
    val lineWidth = (measure - gap) * (1f - share)
    val layout = exportLayout(
        block = lineBlock,
        width = lineWidth.toInt().coerceAtLeast(1),
        ink = run.ink(),
        accent = accent,
        fonts = fonts,
        paper = paper,
        highlightInk = highlightInk
    )
    val rowHeight = maxOf(
        rowHeightOf(doc, block.id) + PDF_PRINT_BAND + exportCaptionRoom(fonts, block),
        layout.height.toFloat()
    )
    if (!run.want(rowHeight + PDF_ROW_GAP)) return
    val top = run.cursor
    drawExportRowCell(
        context = context,
        fonts = fonts,
        run = run,
        block = block,
        left = run.left(),
        frameWidth = printWidth,
        pictureHeight = rowHeightOf(doc, block.id),
        top = top
    )
    // The line, drawn at the column beside the print and CLIPPED to the row: the
    // pair is one row on the page, so it is one row here (the layout was measured
    // against this very column, so it wraps where the page wraps it).
    val canvas = run.surface()
    val lineLeft = run.left() + printWidth + gap
    canvas.save()
    canvas.clipRect(lineLeft, top, lineLeft + lineWidth, top + rowHeight)
    canvas.translate(lineLeft, top)
    layout.draw(canvas)
    canvas.restore()
    run.setCursor(top + rowHeight + PDF_ROW_GAP)
}

/** A cell's own height: its picture's height plus the band its label needs. */
private fun exportCellHeight(doc: PersonalDoc, fonts: PdfFonts, id: String): Float {
    val block = doc.blocks.firstOrNull { it.id == id } ?: return 0f
    return rowHeightOf(doc, id) + PDF_PRINT_BAND + exportCaptionRoom(fonts, block)
}

/** Where a cell stands in its line (for the stacked pair of a three). */
private class ExportPrintCell(
    val id: String,
    val left: Float,
    val width: Float,
    val pictureHeight: Float,
    val topOffset: Float
)

/**
 * THE LINES OF A ROW, DRAWN — each line as tall as its tallest cell, the page's
 * own gap between lines, the whole row placed on one sheet (a row is one block on
 * a page and one block here: it either fits or it starts the next sheet).
 */
private fun drawExportLines(
    context: Context,
    fonts: PdfFonts,
    run: PdfRun,
    doc: PersonalDoc,
    lines: List<List<ExportPrintCell>>,
    lineHeights: List<Float>
) {
    val gap = pdfDp(PRINT_ROW_GAP.value)
    val total = lineHeights.sum() + gap * (lineHeights.size - 1)
    if (!run.want(total + gap)) return
    lines.forEachIndexed { index, line ->
        val lineTop = run.cursor
        line.forEach { cell ->
            val block = doc.blocks.firstOrNull { it.id == cell.id } ?: return@forEach
            drawExportRowCell(
                context = context,
                fonts = fonts,
                run = run,
                block = block,
                left = cell.left,
                frameWidth = cell.width,
                pictureHeight = cell.pictureHeight,
                top = lineTop + cell.topOffset
            )
        }
        run.setCursor(lineTop + lineHeights[index] + gap)
    }
}

/**
 * ONE CELL OF A ROW: the print's own measurements (its frame, the crop, the label
 * at the frame's width) read exactly as [drawExportPrint] reads them — the only
 * difference is that the PLACE comes from the row rather than from the cursor.
 */
private fun drawExportRowCell(
    context: Context,
    fonts: PdfFonts,
    run: PdfRun,
    block: PersonalBlock,
    left: Float,
    frameWidth: Float,
    pictureHeight: Float,
    top: Float
) {
    val uri = block.photo?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() } ?: return
    val innerWidth = (frameWidth - PDF_PRINT_PAD * 2f).coerceAtLeast(1f)
    val bitmap = decodeExportBitmap(context, uri, innerWidth.toInt(), pictureHeight.toInt())
        ?: return
    val face = personalCaptionFace(block.captionFace)
    val labelSize =
        personalCaptionSizeSp(CAPTION_VIEW_SIZE, block.captionSize).value * PDF_UNITS_PER_SP
    val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = fonts.label(face)
        textSize = labelSize
        color = run.ink()
        alpha = PDF_LABEL_ALPHA
        textAlign = Paint.Align.CENTER
    }
    val stampPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = captionPaint.typeface
        textSize = labelSize * 0.76f
        color = run.ink()
        alpha = PDF_STAMP_ALPHA
        letterSpacing = 0.6f * PDF_UNITS_PER_SP
        textAlign = Paint.Align.CENTER
    }
    val caption = block.caption.trim()
    val captionLine = if (caption.isEmpty()) {
        ""
    } else {
        TextUtils.ellipsize(caption, captionPaint, innerWidth, TextUtils.TruncateAt.END).toString()
    }
    drawExportCell(
        run = run,
        bitmap = bitmap,
        top = top,
        left = left,
        innerWidth = innerWidth,
        frameWidth = frameWidth,
        frameHeight = pictureHeight,
        captionLine = captionLine,
        captionPaint = captionPaint,
        stamp = personalCaptionDateText(
            block.captionDateMillis,
            PersonalCaptionDates.order(context, block.captionOrder)
        ),
        stampPaint = stampPaint
    )
    bitmap.recycle()
}


/**
 * A VOICE NOTE AS THE PAGE DRAWS ONE — the page's OWN drawing.
 *
 * v427 — THE SHEET'S LOOKALIKE IS GONE. It drew a filled pill, a row of rounded
 * bars and a plain UI clock, which is a SECOND drawing of the same note: the page
 * draws a hand-drawn pulse (or one of the looks the member picked — see
 * [drawVoiceWave]), so on paper the note came out as a different object at a
 * different weight — the member's "the waves are also not visible as it is in
 * journal eye view".
 *
 * The strip is the page's own row now, in the page's own order and at the page's
 * own sizes: the note's control (a FILLED disc for the looks that carry one, a
 * plain drawn mark for the two bare looks, which is what the page shows), the
 * 34dp strip, and the clock in the editorial serif the drawn looks wear — and the
 * WAVE itself is the page's own stroke, drawn by [drawVoicePulse] /
 * [drawVoiceWave] on the sheet's canvas through a [CanvasDrawScope] sized at the
 * sheet's own scale. One drawing, two surfaces, so they can never drift again.
 */
private fun drawExportVoice(
    fonts: PdfFonts,
    run: PdfRun,
    block: PersonalBlock,
    paper: Int,
    ink: Int,
    accent: Int
) {
    val style = PersonalVoiceStyle.fromKey(block.audioStyle)
    val samples = PersonalAudioBars.decode(block.audioBars)
    val clock = formatRecordingTime(block.audioSeconds)
    val control = pdfDp(38f)
    val strip = pdfDp(34f)
    val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        alpha = if (style.drawsPulse || style == PersonalVoiceStyle.MINIMAL) 209 else 179
        typeface = if (style.drawsPulse || style == PersonalVoiceStyle.MINIMAL) {
            fonts.display
        } else {
            null
        }
        textSize = pdfDp(12f)
        textAlign = Paint.Align.RIGHT
    }
    val rowHeight = control.coerceAtLeast(strip)
    if (!run.want(rowHeight + 22f)) return
    val canvas = run.surface()
    val top = run.cursor
    val centreY = top + rowHeight / 2f
    // ── THE CONTROL, as the page's own note draws it ────────────────────
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val bare = style.drawsPulse || style == PersonalVoiceStyle.MINIMAL
    if (bare) {
        // The two bare looks draw the mark in the wave's own ink and weight: no
        // disc, because a Material disc beside a hand-drawn line is two drawings
        // in one strip (the page's own ruling).
        val stroke = (control * 0.10f).coerceAtLeast(1.6f)
        paint.color = ink
        paint.alpha = if (style.drawsPulse) 219 else 204
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        val half = control * 0.30f
        val mark = Path().apply {
            moveTo(run.left() + control * 0.28f, centreY - half * 0.78f)
            lineTo(run.left() + control * 0.28f + half * 1.30f, centreY)
            lineTo(run.left() + control * 0.28f, centreY + half * 0.78f)
            close()
        }
        canvas.drawPath(mark, paint)
        if (style == PersonalVoiceStyle.MINIMAL) {
            paint.alpha = 66
            canvas.drawCircle(
                run.left() + control / 2f,
                centreY,
                (control / 2f - stroke / 2f).coerceAtLeast(1f),
                paint
            )
        }
    } else {
        // The disc looks keep a real filled control (the pill, the bubble) — the
        // same accent-with-paper-mark pairing the page shows.
        paint.color = accent
        val pill = if (style == PersonalVoiceStyle.PILL) control * 1.7f else control
        canvas.drawRoundRect(
            RectF(run.left(), centreY - control / 2f, run.left() + pill, centreY + control / 2f),
            control / 2f,
            control / 2f,
            paint
        )
        val mark = Path().apply {
            val cx = run.left() + pill / 2f
            moveTo(cx - control * 0.10f, centreY - control * 0.17f)
            lineTo(cx + control * 0.17f, centreY)
            lineTo(cx - control * 0.10f, centreY + control * 0.17f)
            close()
        }
        paint.color = paper
        canvas.drawPath(mark, paint)
    }
    // ── THE WAVE, drawn by the page's own stroke ────────────────────────
    val waveLeft = run.left() + control + pdfDp(10f)
    val waveRight = run.right() - clockPaint.measureText(clock) - pdfDp(10f)
    if (samples.isNotEmpty() && waveRight > waveLeft) {
        // The strip is scaled the way the sheet measures everything (one canvas
        // dp, one sheet unit), so the page's own dp-based geometry lands at the
        // paper's size. Progress is 0 — a printed note has not been played.
        val stripTop = centreY - strip / 2f
        val width = waveRight - waveLeft
        canvas.save()
        canvas.translate(waveLeft, stripTop)
        val scope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
        scope.draw(
            density = androidx.compose.ui.unit.Density(PDF_UNITS_PER_SP),
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
            canvas = canvas,
            size = androidx.compose.ui.geometry.Size(width, strip)
        ) {
            if (style.drawsPulse) {
                drawVoicePulse(samples, 0f, Color(ink), Color(accent))
            } else {
                drawVoiceWave(samples, 0f, Color(ink), Color(accent), style)
            }
        }
        canvas.restore()
    }
    canvas.drawText(clock, run.right(), centreY + clockPaint.textSize * 0.36f, clockPaint)
    run.advance(rowHeight + 22f)
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

/**
 * A PHOTOGRAPH, DECODED NO LARGER THAN IT WILL BE DRAWN — AND THE RIGHT WAY UP.
 *
 * v427 — THE UPRIGHT HALF WAS MISSING. The page paints a photograph through Coil,
 * which reads the file's EXIF orientation, so a picture taken with the phone held
 * sideways is shown UPRIGHT. A bare `BitmapFactory` decode knows nothing about
 * EXIF: it returns the sensor's own pixels, so the same photograph left the
 * journal lying on its side — one more way the sheet did not show what the
 * preview showed.
 *
 * The sample is taken against BOTH targets, because a print's photograph is
 * CROPPED to its frame (see [drawExportPhoto]): a decode shrunk to fit the frame's
 * width alone would be too coarse to fill its height.
 */
private fun decodeExportBitmap(
    context: Context,
    uri: Uri,
    targetWidth: Int,
    targetHeight: Int
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (
        sample < 8 &&
        bounds.outWidth / (sample * 2) >= targetWidth &&
        bounds.outHeight / (sample * 2) >= targetHeight
    ) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }.getOrNull() ?: return null
    return exportUpright(context, uri, decoded)
}

/**
 * The photograph as the page shows it: the sensor's pixels turned by the file's
 * own EXIF angle (see [decodeExportBitmap]). A file with no orientation — or one
 * whose EXIF cannot be read — is handed back untouched, which is what the sheet
 * has always drawn.
 */
private fun exportUpright(context: Context, uri: Uri, decoded: Bitmap): Bitmap {
    val orientation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return decoded
    val matrix = Matrix().apply { postRotate(degrees) }
    val turned = runCatching {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }.getOrNull() ?: return decoded
    if (turned !== decoded) decoded.recycle()
    return turned
}
