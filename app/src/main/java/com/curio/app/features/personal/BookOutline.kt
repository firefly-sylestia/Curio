package com.curio.app.features.personal

import android.content.Context
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.util.zip.ZipFile

/**
 * v389 — THE BOOK'S OWN CONTENTS.
 *
 * The reader's chapter sheet used to be built out of what the reader could see
 * for itself: the headings it happened to find in an EPUB, and — for a PDF,
 * which has no headings at all in its text layer unless the publisher bothered —
 * a 4-per-row grid of every page number in the file. That is not a table of
 * contents, it is a phone book.
 *
 * A real one is almost always already inside the file. An EPUB carries a
 * navigation document (EPUB 3) or an NCX (EPUB 2, still shipped by older books),
 * and a PDF carries an outline — the bookmarks a publisher's tools wrote into
 * the file itself, with proper titles and the page each one opens. Both are read
 * here, and the reader falls back to what it can see when a file has neither,
 * because a scanned novel genuinely has no contents to offer.
 *
 * [target] is the EPUB's own file name for a chapter (what a block's section is
 * matched against) and [page] is a PDF's 1-based page; exactly one of the two is
 * meaningful, which [isPage] says.
 */
internal data class ReaderOutlineEntry(
    val title: String,
    /** EPUB: the document the chapter lives in, fragment stripped. */
    val target: String = "",
    /** PDF: 1-based page the entry opens. */
    val page: Int = -1,
    /** How deep in the contents this sits — 1 is a chapter, 2 a part of one. */
    val depth: Int = 1,
    /**
     * Reflowable text: the BLOCK the entry opens, filled in by the reader once
     * it has matched the EPUB's own file name to one of its sections. -1 when
     * the entry names something the reader could not find.
     */
    val block: Int = -1
) {
    val isPage: Boolean get() = page > 0
}

// ── EPUB ─────────────────────────────────────────────────────────────────────

/**
 * An EPUB's own contents: the EPUB 3 nav document first (it is what the format
 * requires), then the NCX that older books still ship. Empty when the book has
 * neither, which the caller answers for by falling back to its own headings.
 */
internal fun epubOutline(zip: ZipFile): List<ReaderOutlineEntry> {
    navDocumentOutline(zip).takeIf { it.isNotEmpty() }?.let { return it }
    return ncxOutline(zip)
}

private fun navDocumentOutline(zip: ZipFile): List<ReaderOutlineEntry> {
    val candidates = zip.entries().asSequence().filter { candidate ->
        !candidate.isDirectory && (candidate.name.endsWith(".xhtml", true) ||
            candidate.name.endsWith(".html", true) || candidate.name.endsWith(".htm", true))
    }
    candidates.forEach { document ->
        val raw = runCatching {
            zip.getInputStream(document).bufferedReader().use { it.readText() }
        }.getOrNull() ?: return@forEach
        val at = raw.indexOf("epub:type=\"toc\"", ignoreCase = true)
            .takeIf { it >= 0 }
            ?: raw.indexOf("epub:type='toc'", ignoreCase = true)
        if (at < 0) return@forEach
        // From the nav's own marker to the end of that nav element — anchors
        // outside it are the page list or the landmarks, not the contents.
        val body = raw.substring(at).substringBefore("</nav>", "")
        val base = document.name.substringBeforeLast('/', "")
        val found = ArrayList<ReaderOutlineEntry>()
        // ── THE LIST'S OWN NESTING IS THE HIERARCHY (v389c) ──────────────
        //
        // A contents list is a list of LISTS: a part's chapters are nested
        // inside it, which is what makes "Part Two" read as a heading with its
        // chapters under it rather than as one more sibling. The first pass
        // flattened every entry to depth 1, so the sheet could show a contents
        // but not the book's own shape — the user's note was "make the chapter
        // points more broader with proper hirarcy". Depth is therefore counted
        // from the markup as it is walked: `<ol>` opens a level, `</ol>` closes
        // one, and each anchor takes the level it sits at (capped at three,
        // because a fourth indent on a phone is off the side of the sheet).
        var depth = 0
        Regex(
            "<ol\\b|</ol\\s*>|<a[^>]*?href\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>(.*?)</a>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).findAll(body).forEach { match ->
            val whole = match.value
            when {
                whole.startsWith("</ol", ignoreCase = true) ->
                    depth = (depth - 1).coerceAtLeast(0)
                whole.startsWith("<ol", ignoreCase = true) -> depth++
                else -> {
                    val title = plain(match.groupValues.getOrNull(2).orEmpty())
                    if (title.isBlank()) return@forEach
                    found.add(
                        ReaderOutlineEntry(
                            title = title,
                            target = resolveTarget(base, match.groupValues.getOrNull(1).orEmpty()),
                            depth = depth.coerceIn(1, 3)
                        )
                    )
                }
            }
        }
        if (found.isNotEmpty()) return found
    }
    return emptyList()
}

private fun ncxOutline(zip: ZipFile): List<ReaderOutlineEntry> {
    val ncx = zip.entries().asSequence().firstOrNull {
        !it.isDirectory && it.name.endsWith(".ncx", true)
    } ?: return emptyList()
    val raw = runCatching {
        zip.getInputStream(ncx).bufferedReader().use { it.readText() }
    }.getOrNull() ?: return emptyList()
    val base = ncx.name.substringBeforeLast('/', "")
    val found = ArrayList<ReaderOutlineEntry>()
    // An NCX says its own shape the same way the nav document does — navPoints
    // nested inside navPoints — so the depth is counted from the markup rather
    // than assumed (see navDocumentOutline for why it matters).
    var depth = 0
    Regex(
        "<navPoint\\b|</navPoint\\s*>|<text[^>]*>(.*?)</text>|<content[^>]*?src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).findAll(raw).forEach { match ->
        val whole = match.value
        when {
            whole.startsWith("</navPoint", ignoreCase = true) ->
                depth = (depth - 1).coerceAtLeast(0)
            whole.startsWith("<navPoint", ignoreCase = true) -> depth++
            whole.startsWith("<text", ignoreCase = true) -> {
                val title = plain(match.groupValues.getOrNull(1).orEmpty())
                if (title.isBlank()) return@forEach
                found.add(
                    ReaderOutlineEntry(
                        title = title,
                        depth = depth.coerceIn(1, 3)
                    )
                )
            }
            else -> {
                // The `<content src>` belongs to the navPoint that was opened
                // most recently, which is the last entry added.
                if (found.isNotEmpty()) {
                    val src = match.groupValues.getOrNull(2).orEmpty()
                    found[found.size - 1] = found[found.size - 1]
                        .copy(target = resolveTarget(base, src))
                }
            }
        }
    }
    return found
}

/** `chapter3.xhtml#s2` beside `OEBPS/` becomes `OEBPS/chapter3.xhtml`. */
private fun resolveTarget(base: String, href: String): String {
    val clean = href.substringBefore('#').substringBefore('?').trim()
    if (clean.isEmpty()) return ""
    val full = if (clean.startsWith("/")) clean.drop(1) else "$base/$clean"
    val parts = ArrayList<String>()
    full.split('/').forEach { part ->
        when (part) {
            "", "." -> Unit
            ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.size - 1)
            else -> parts.add(part)
        }
    }
    return parts.joinToString("/")
}

private fun plain(html: String): String = html
    .replace(Regex("<[^>]+>"), " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace(Regex("\\s+"), " ")
    .trim()

// ── PDF ──────────────────────────────────────────────────────────────────────

/**
 * A PDF's outline — the contents the publisher's tools wrote into the file.
 * Read on demand (it is a parse, like everything else PDFBox does), so the
 * chapters sheet opens on a background dispatcher.
 */
internal fun pdfOutline(context: Context, document: String): List<ReaderOutlineEntry> =
    withPdfDocument(context, document) { loaded ->
        val outline = loaded.documentCatalog.documentOutline ?: return@withPdfDocument emptyList()
        val out = ArrayList<ReaderOutlineEntry>()

        fun walk(first: com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem?, depth: Int) {
            var node = first
            var guard = 0
            while (node != null && guard++ < 600) {
                val title = node.title.orEmpty().trim()
                if (title.isNotEmpty()) {
                    // `findDestinationPage` resolves BOTH a direct page
                    // destination and the named one a published PDF usually
                    // uses, which `retrievePageNumber` alone does not.
                    val number = runCatching {
                        val target = node.findDestinationPage(loaded)
                        if (target == null) -1 else loaded.pages.indexOf(target) + 1
                    }.getOrDefault(-1)
                    out.add(
                        ReaderOutlineEntry(
                            title = title,
                            page = number,
                            depth = depth.coerceIn(1, 3)
                        )
                    )
                }
                node.firstChild?.let { walk(it, depth + 1) }
                node = node.nextSibling
            }
        }

        walk(outline.firstChild, 1)
        out
    } ?: emptyList()

// ── Does the book number its own pages? ──────────────────────────────────────

private val OWN_PAGE_MARKER = Regex(
    "^\\s*[\\[\\u2014\\u2013-]?\\s*(?:(?:page|pg|p)\\.?\\s*)?" +
        "(?:[0-9]{1,4}|[ivxlcdmIVXLCDM]{1,7})\\s*[\\]\\u2014\\u2013-]?\\s*$"
)

/**
 * v389 — DOES THE BOOK ALREADY SAY ITS OWN PAGE NUMBERS?
 *
 * A scanned-and-reflowed EPUB very often carries the print edition's page
 * numbers as literal lines between the paragraphs. When a reader then counts
 * pages for itself as well, the member reads the same number twice — once in the
 * book's own words and once in Curio's (user report: "sometimes the epub already
 * says the page in text can u auto detect that and dont show duplicate page").
 *
 * Three or more short lines that are nothing but a number (with or without a
 * "page"/"p." in front) is not something prose does by accident, so the reader
 * stops numbering that book's pages itself and lets the file do the talking.
 */
internal fun carriesOwnPageMarkers(blocks: List<String>): Boolean {
    var found = 0
    for (block in blocks) {
        val line = block.trim()
        if (line.isEmpty() || line.length > 14) continue
        if (OWN_PAGE_MARKER.matches(line)) found++
        if (found >= 3) return true
    }
    return false
}
