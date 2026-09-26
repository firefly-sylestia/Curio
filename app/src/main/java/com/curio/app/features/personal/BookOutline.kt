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
    /**
     * v389c — WHERE INSIDE THAT DOCUMENT.
     *
     * The half of a contents link that used to be thrown away. It matters for
     * two things: a chapter that begins partway down a file (a book that keeps
     * all its chapters in one XHTML) otherwise lands at the top of it, and an
     * EPUB's own PRINTED PAGE NUMBERS are nothing BUT anchors — `#page42` is the
     * whole of what a page-list entry says. Matched against the anchor the
     * parser records for the book's own page-break markers (see `ReaderBlock`).
     */
    val anchor: String = "",
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

/**
 * v475 — THE BOOK'S OWN READING ORDER.
 *
 * An EPUB's reading order is the OPF **spine**: the ordered list of `<itemref>`
 * ids that names which content document follows which. The order the files
 * happen to sit in inside the ZIP archive is a packer's business and nothing to
 * do with the book — several real EPUBs store `chapter10.xhtml` before
 * `chapter2.xhtml`, or keep a whole appendix ahead of the text.
 *
 * The reader used to walk `zip.entries()` and call that the book, so those
 * EPUBs opened with their sections scrambled — the contents list was right
 * while the reading was not (member: *"the app isnt showing the contents
 * properly. they are not arranged correctly. look into that, in some epubs
 * only"*). It also poisoned the progress the member saw: a chapter's section
 * number came from archive position, so "pages left in this chapter" (which
 * walks from a section's first to its last block) counted across whatever
 * happened to sit between them — a random-looking number on every turn.
 *
 * Answers the content documents' archive paths in the order the BOOK says they
 * read. Empty when the OPF cannot be read, so the caller keeps the archive
 * order as its fallback — a malformed file must never hide a document.
 */
internal fun epubReadingOrder(zip: ZipFile): List<String> = epubPackage(zip).spine

/**
 * v475 — THE OPF, READ ONCE: the BOOK's order and the two documents it declares.
 *
 * `epubReadingOrder`, `epubOutline` and `epubPageList` all need something out
 * of the same package document, and the two navigation lists have to be FOUND
 * the way the format says rather than by scanning — the archive order is a
 * packer's business (see [epubReadingOrder]). So the OPF is resolved here
 * (`META-INF/container.xml` → the rootfile), its manifest becomes an id→path
 * map, and the two declared documents are picked out of the SAME pass:
 *
 *  · the **nav document** — the manifest item whose `properties` list holds
 *    `nav` (that is what makes it the EPUB 3 navigation document, whatever it
 *    is called and wherever in the archive it sits);
 *  · the **NCX** — the item the manifest types `application/x-dtbncx+xml`
 *    (the EPUB 2 contents).
 *
 * Both are `""` when the package does not declare one, so every caller keeps
 * its own archive scan as the fallback — a book that breaks the rules must
 * still open.
 */
private class EpubPackage(
    /** Content documents in the order the BOOK reads them. */
    val spine: List<String>,
    /** The nav document the manifest declares, or `""`. */
    val nav: String,
    /** The NCX the manifest declares, or `""`. */
    val ncx: String
)

private fun epubPackage(zip: ZipFile): EpubPackage {
    // The OPF the container names, else the first one in the archive.
    val container = runCatching {
        zip.getEntry("META-INF/container.xml")?.let { entry ->
            zip.getInputStream(entry).bufferedReader().use { it.readText() }
        }
    }.getOrNull()
    val rootPath = container
        ?.let { raw ->
            Regex("full-path\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
                .find(raw)?.groupValues?.getOrNull(1)
        }
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: zip.entries().asSequence()
            .firstOrNull { !it.isDirectory && it.name.endsWith(".opf", true) }
            ?.name
        ?: return EpubPackage(emptyList(), "", "")
    val opf = zip.getEntry(rootPath) ?: return EpubPackage(emptyList(), "", "")
    val raw = runCatching {
        zip.getInputStream(opf).bufferedReader().use { it.readText() }
    }.getOrNull() ?: return EpubPackage(emptyList(), "", "")
    val base = rootPath.substringBeforeLast('/', "")
    // id -> archive path, from the manifest — and the two declared navigation
    // documents picked out of the same walk.
    val manifest = HashMap<String, String>()
    var nav = ""
    var ncx = ""
    Regex("<item\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(raw).forEach { match ->
        val tag = match.value
        val id = epubAttr(tag, "id") ?: return@forEach
        val href = epubAttr(tag, "href") ?: return@forEach
        val path = resolveTarget(base, href)
        manifest[id] = path
        val properties = epubAttr(tag, "properties").orEmpty().lowercase()
        if (nav.isBlank() && properties.split(' ', ',').any { it == "nav" }) nav = path
        val mediaType = epubAttr(tag, "media-type").orEmpty().lowercase()
        if (ncx.isBlank() && mediaType.contains("dtbncx")) ncx = path
    }
    // The spine, in order: each `<itemref idref>` resolves through the manifest
    // to the document it names.
    val spine = Regex("<itemref\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(raw)
        .mapNotNull { match -> epubAttr(match.value, "idref") }
        .mapNotNull { idref -> manifest[idref] }
        .filter { it.isContentDocument() }
        .distinct()
        .toList()
    return EpubPackage(spine, nav, ncx)
}

/** True for the document types a reflowable book's text lives in. */
private fun String.isContentDocument(): Boolean =
    endsWith(".xhtml", true) || endsWith(".html", true) || endsWith(".htm", true)

/** Reads one XML attribute out of a single tag's text. */
private fun epubAttr(tag: String, name: String): String? =
    Regex("\\b" + name + "\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        .find(tag)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }

/**
 * v389c — THE BOOK'S OWN PRINTED PAGE NUMBERS.
 *
 * An EPUB is reflowable, so it has no pages of its own — but a book that was
 * TYPESET has them, and EPUB 3 carries the mapping: a second navigation list
 * (`epub:type="page-list"`, `role="doc-pagelist"`) whose entries are the print
 * edition's page numbers, each one pointing at the `#page…` anchor in the text
 * where that page begins. It is the one honest answer to "what page am I on"
 * for a reflowed book, and the reader had been ignoring it entirely (user
 * request: "for epub add more detetable chapters and pages").
 *
 * The entries come back in the SAME shape as chapters, with [ReaderOutlineEntry.anchor]
 * carrying the marker, because that is exactly what they are: places in the
 * book, with the book's own name for them.
 */
internal fun epubPageList(zip: ZipFile): List<ReaderOutlineEntry> {
    // ── v475 — THE DECLARED NAV FIRST, NOT THE ARCHIVE'S FIRST ───────────
    // A page-list lives in the EPUB 3 navigation document, and WHICH document
    // that is comes from the package's manifest (`properties="nav"`) — not from
    // whichever xhtml the packer happened to write first (see [epubPackage]).
    // The declared document is tried first and the archive scan stays as the
    // fallback, so a book that breaks the rules still opens.
    val declared = epubPackage(zip).nav
    val candidates = zip.entries().asSequence().filter { candidate ->
        !candidate.isDirectory && (candidate.name.endsWith(".xhtml", true) ||
            candidate.name.endsWith(".html", true) || candidate.name.endsWith(".htm", true))
    }.sortedByDescending { candidate -> candidate.name == declared }
    candidates.forEach { document ->
        val raw = runCatching {
            zip.getInputStream(document).bufferedReader().use { it.readText() }
        }.getOrNull() ?: return@forEach
        val at = listOf(
            raw.indexOf("epub:type=\"page-list\"", ignoreCase = true),
            raw.indexOf("epub:type='page-list'", ignoreCase = true),
            raw.indexOf("role=\"doc-pagelist\"", ignoreCase = true)
        ).filter { it >= 0 }.minOrNull() ?: return@forEach
        val body = raw.substring(at).substringBefore("</nav>", "")
        val base = document.name.substringBeforeLast('/', "")
        val found = ArrayList<ReaderOutlineEntry>()
        Regex("<a[^>]*?href\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", setOf(
            RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL
        )).findAll(body).forEach { match ->
            val label = plain(match.groupValues.getOrNull(2).orEmpty())
            if (label.isBlank()) return@forEach
            val (path, anchor) = splitHref(base, match.groupValues.getOrNull(1).orEmpty())
            found.add(ReaderOutlineEntry(title = label, target = path, anchor = anchor))
        }
        if (found.isNotEmpty()) return found
    }
    return emptyList()
}

private fun navDocumentOutline(zip: ZipFile): List<ReaderOutlineEntry> {
    // v475 — the DECLARED nav document first (see [epubPackage]); the archive
    // scan is the fallback for a package that does not declare one.
    val declared = epubPackage(zip).nav
    val candidates = zip.entries().asSequence().filter { candidate ->
        !candidate.isDirectory && (candidate.name.endsWith(".xhtml", true) ||
            candidate.name.endsWith(".html", true) || candidate.name.endsWith(".htm", true))
    }.sortedByDescending { candidate -> candidate.name == declared }
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
                    val (path, anchor) = splitHref(base, match.groupValues.getOrNull(1).orEmpty())
                    found.add(
                        ReaderOutlineEntry(
                            title = title,
                            target = path,
                            anchor = anchor,
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
    // v475 — the NCX the package DECLARES first (see [epubPackage]); the archive
    // scan is the fallback for a package that does not declare one.
    val declared = epubPackage(zip).ncx
    val ncx = (declared.takeIf { it.isNotBlank() }?.let { zip.getEntry(it) })
        ?: zip.entries().asSequence().firstOrNull {
            !it.isDirectory && it.name.endsWith(".ncx", true)
        }
        ?: return emptyList()
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
                    val (path, anchor) = splitHref(base, src)
                    found[found.size - 1] = found[found.size - 1]
                        .copy(target = path, anchor = anchor)
                }
            }
        }
    }
    return found
}

/**
 * `chapter3.xhtml#s2` beside `OEBPS/` becomes (`OEBPS/chapter3.xhtml`, `s2`).
 *
 * Both halves, always: the FILE says which document to open, the FRAGMENT says
 * where inside it, and a reader needs the second one as much as the first —
 * which is why the old helper that returned only the path was the reason a
 * page-list could not work (see [epubPageList]).
 */
private fun splitHref(base: String, href: String): Pair<String, String> {
    val anchor = href.substringAfter('#', "").substringBefore('?').trim()
    return resolveTarget(base, href) to anchor
}

/** The FILE half of the href above, as an archive path. */
private fun resolveTarget(base: String, href: String): String {
    val clean = href.substringBefore('#').substringBefore('?').trim()
    // A link that is ONLY a fragment belongs to the file it was written in.
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
/**
 * v389e — HOW LONG THE FILE IS.
 *
 * The member's own PDF is the one place a page count can be trusted: the
 * catalog's count is for ITS edition and a lookup's is a guess. The last chapter
 * of the file's own contents runs to this page, and the reader's foot quotes it
 * (user request: "also the page number from the file"). 0 when it cannot be read.
 */
internal fun pdfPageCount(context: Context, document: String): Int =
    withPdfDocument(context, document) { loaded -> loaded.numberOfPages } ?: 0

/**
 * v425 — HOW LONG AN EPUB IS, OUT OF THE FILE.
 *
 * An EPUB is reflowable, so it prints no page numbers of its own — unless it was
 * TYPESET, in which case its page-list names every one of the print edition's
 * pages (see [epubPageList]) and the count is simply the last of them. The
 * member's own copy is the only honest source for that number: a catalog's count
 * belongs to its edition and a lookup's is a guess, which is exactly why a book
 * whose FILE is an EPUB could show no length at all on its page — the page only
 * ever asked a PDF (user report: "suppose it using epub for pages in detail
 * screen it doesnt show the pages count from it").
 *
 * A page list whose labels are not numbers (a book that marks its pages in roman
 * numerals, or with words) still answers with how many markers there are, and a
 * book with no page-list answers 0 — the fetched count then stays where it was
 * instead of being replaced by an invention.
 */
internal fun epubPageCount(document: String): Int {
    val pages = runCatching { ZipFile(document).use { epubPageList(it) } }
        .getOrDefault(emptyList())
    if (pages.isEmpty()) return 0
    val highest = pages.asSequence()
        .mapNotNull { entry ->
            entry.title.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toIntOrNull()
        }
        .maxOrNull()
    return highest ?: pages.size
}

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
