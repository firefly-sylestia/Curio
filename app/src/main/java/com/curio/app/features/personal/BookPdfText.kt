package com.curio.app.features.personal

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File

/**
 * v389 — THE WORDS ON A PDF PAGE.
 *
 * A PDF page is a PICTURE. Android's own renderer (PdfRenderer) draws it
 * beautifully and knows nothing whatsoever about the letters on it, which is the
 * whole reason a PDF could be bookmarked and noted but never SELECTED, SEARCHED
 * or HIGHLIGHTED the way a reflowable book already could. This file is the one
 * place that can say what is written on the page, and where each letter sits.
 *
 * THE COST, HONESTLY. Opening a PDF through a text extractor means parsing its
 * cross-reference table, which is the one thing the native renderer does not
 * have to do — so this is deliberately NOT on the opening path:
 *
 *  · A page's words are extracted on demand, one page at a time, on the IO
 *    dispatcher, and only when the member actually asks for them (a selection,
 *    a search, or a page that is being read).
 *  · The result is cached per (file, page) in a small LRU, so a page is parsed
 *    once per session however often it is touched.
 *  · Drawing the page is untouched: PdfRenderer still renders it, exactly as
 *    fast as before, and a book with no text layer still opens at the same
 *    speed it always did.
 *
 * The positions come back in the PDF'S OWN POINTS (points are 1/72 inch, which
 * is also what PdfRenderer reports a page's size in), so a caller can map a
 * glyph to the place it was drawn with one ratio — see [PdfPageText.pageWidthPt]
 * — whatever size the page ended up on screen. The render scale cancels out of
 * that ratio entirely, which is why zooming or re-rendering at a higher
 * resolution never invalidates a selection.
 */

/** ONE run of glyphs on a page: what it says and where it was drawn, in points
 *  from the page's TOP-LEFT corner. */
internal data class PdfGlyph(
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/** A whole page's words, in reading order, with the page's own size in points. */
internal class PdfPageText(
    val glyphs: List<PdfGlyph>,
    val pageWidthPt: Float,
    val pageHeightPt: Float
) {
    /** The page as one string — what a search runs against and what a stored
     *  highlight's own words are taken from. */
    val text: String by lazy(LazyThreadSafetyMode.NONE) {
        glyphs.joinToString("") { it.text }
    }

    /**
     * The character range of the WORD around [offset] — the unit a long press
     * on a page selects, and what the two handles then grow by. A word is a run
     * of non-space glyphs, which is as much as a page of typesetting can promise.
     */
    fun wordAround(offset: Int): IntRange {
        if (glyphs.isEmpty()) return IntRange.EMPTY
        var index = offset.coerceIn(0, glyphs.size - 1)
        if (glyphs[index].text.isBlank() && glyphs.size > 1) {
            // A press on the gap between two words belongs to the word before
            // it, which is what a reader means when they press a space.
            while (index > 0 && glyphs[index].text.isBlank()) index--
        }
        var start = index
        while (start > 0 && !glyphs[start - 1].text.isBlank()) start--
        var end = index
        while (end < glyphs.size - 1 && !glyphs[end + 1].text.isBlank()) end++
        return start..end
    }

    /** The glyph nearest a point, given the size the page is being DRAWN at. */
    fun glyphAt(x: Float, y: Float, drawnWidth: Float): Int {
        if (glyphs.isEmpty() || pageWidthPt <= 0f) return -1
        // Where the page is drawn: the caller hands the display width, and the
        // page's own point width is what the glyphs are measured in.
        val scale = drawnWidth / pageWidthPt
        val px = x / scale
        val py = y / scale
        var best = -1
        var bestDistance = Float.MAX_VALUE
        glyphs.forEachIndexed { index, glyph ->
            val dx = (px - (glyph.x + glyph.width / 2f))
            val dy = (py - (glyph.y + glyph.height / 2f))
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }
        return best
    }

    /** The words between two glyph indices, which is what a highlight stores. */
    fun textBetween(first: Int, last: Int): String {
        val from = first.coerceIn(0, (glyphs.size - 1).coerceAtLeast(0))
        val to = last.coerceIn(from, (glyphs.size - 1).coerceAtLeast(0))
        return glyphs.subList(from, to + 1).joinToString("") { it.text }.trim()
    }

    /**
     * v389c — WHERE EACH GLYPH BEGINS IN [text].
     *
     * Almost always one character per glyph, which is why a glyph index and a
     * character offset usually agree — but a ligature or a combining mark comes
     * back as one TextPosition holding SEVERAL characters, and from that moment
     * the two numberings drift apart. Everything that turns a stored passage
     * back into places to draw has to cross between them, so the offsets are
     * worked out once rather than assumed.
     */
    private val charStarts: IntArray by lazy(LazyThreadSafetyMode.NONE) {
        val starts = IntArray(glyphs.size + 1)
        var at = 0
        glyphs.forEachIndexed { index, glyph ->
            starts[index] = at
            at += glyph.text.length
        }
        starts[glyphs.size] = at
        starts
    }

    /** The glyph that covers character [offset] of [text], or -1 if it cannot. */
    fun glyphAtChar(offset: Int): Int {
        if (glyphs.isEmpty()) return -1
        val target = offset.coerceIn(0, (charStarts[glyphs.size] - 1).coerceAtLeast(0))
        // The LAST glyph that starts at or before the offset — a binary search,
        // because this runs inside a draw pass over a page of glyphs.
        var low = 0
        var high = glyphs.size - 1
        var found = 0
        while (low <= high) {
            val mid = (low + high) / 2
            if (charStarts[mid] <= target) {
                found = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return found
    }

    /**
     * The glyphs covering [length] characters from [offset] — what a stored
     * passage becomes when the page is drawn again. Returns an empty range when
     * the words are not on this page at all (the file changed under a mark).
     */
    fun glyphRange(offset: Int, length: Int): IntRange {
        if (glyphs.isEmpty() || length <= 0) return IntRange.EMPTY
        val from = glyphAtChar(offset)
        if (from < 0) return IntRange.EMPTY
        val lastChar = (offset + length - 1).coerceAtLeast(offset)
        var to = from
        while (to < glyphs.size - 1 && charStarts[to + 1] <= lastChar) to++
        return from..to
    }
}

/**
 * The extraction itself. ONE parse per (file, page) per session: the LRU is
 * small on purpose — a handful of pages is all a reader is ever between, and a
 * page's glyphs are tiny next to the bitmap they were drawn into.
 */
private object PdfTextCache {
    private const val CAPACITY = 6
    private val pages = LinkedHashMap<String, PdfPageText>()

    fun get(key: String): PdfPageText? = pages[key]

    fun put(key: String, value: PdfPageText) {
        while (pages.size >= CAPACITY) {
            val oldest = pages.keys.firstOrNull() ?: break
            pages.remove(oldest)
        }
        pages[key] = value
    }
}

/** A plain filesystem path (the app's own copy), or null for a `content://`. */
private fun pdfLocalFile(value: String): File? {
    if (value.startsWith("content://")) return null
    return File(value.removePrefix("file://"))
}

/**
 * THE WORDS OF ONE PAGE. Returns null when the file cannot be opened or the page
 * has no text layer at all (a scanned PDF), so a caller can carry on drawing the
 * page and offer no selection rather than fail.
 */
/**
 * THE ONE WAY TO OPEN A PDF. Every reader feature that needs the file's words
 * rather than its picture comes through here — the page text, the contents, and
 * whatever else is added later — so the loader is initialised once and a
 * `content://` and a plain path are handled in exactly one place.
 */
internal fun <T> withPdfDocument(
    context: Context,
    document: String,
    block: (PDDocument) -> T
): T? = runCatching {
    // Cheap and idempotent: the loader hands PDFBox the app's assets/fonts so a
    // page that uses a standard font can be measured.
    PDFBoxResourceLoader.init(context.applicationContext)
    val file = pdfLocalFile(document)
    val pdf = if (file != null) {
        PDDocument.load(file)
    } else {
        val stream = context.contentResolver.openInputStream(Uri.parse(document))
            ?: return@runCatching null
        stream.use { PDDocument.load(it) }
    }
    pdf.use { loaded -> block(loaded) }
}.getOrNull()

internal fun extractPdfPageText(
    context: Context,
    document: String,
    index: Int
): PdfPageText? {
    val key = "$document#$index"
    PdfTextCache.get(key)?.let { return it }
    return withPdfDocument(context, document) { loaded ->
            if (index !in 0 until loaded.numberOfPages) return@withPdfDocument null
            val page = loaded.getPage(index)
            val glyphs = ArrayList<PdfGlyph>()
            val stripper = object : PDFTextStripper() {
                override fun writeString(
                    text: String?,
                    textPositions: MutableList<TextPosition>?
                ) {
                    if (textPositions.isNullOrEmpty()) return
                    textPositions.forEach { position ->
                        val word = position.unicode ?: return@forEach
                        if (word.isBlank()) return@forEach
                        glyphs.add(
                            PdfGlyph(
                                text = word,
                                x = position.xDirAdj,
                                y = position.yDirAdj,
                                width = position.widthDirAdj,
                                height = position.heightDir
                            )
                        )
                    }
                }
            }.apply {
                // Reading order, not the order the page happens to store runs
                // in: a two-column page otherwise comes back interleaved.
                sortByPosition = true
                startPage = index + 1
                endPage = index + 1
            }
            stripper.getText(loaded)
            val box = page.mediaBox
            PdfPageText(
                glyphs = glyphs,
                pageWidthPt = box.width,
                pageHeightPt = box.height
            ).also { PdfTextCache.put(key, it) }
    }
}
