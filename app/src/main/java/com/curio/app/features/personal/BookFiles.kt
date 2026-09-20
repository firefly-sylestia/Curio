package com.curio.app.features.personal

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * v389 — WHERE A BOOK'S OWN FILE LIVES.
 *
 * A book the member wants to READ in Curio — a PDF or an EPUB they already
 * have — used to be pinned to its `content://` picker URI. That is a handle,
 * not a file: it works while the permission lasts and turns into a dead link
 * afterwards, and it was stored in the COVER's column, so the shelf tried to
 * paint a PDF as a picture.
 *
 * So the document is COPIED into the app's own storage, under
 * `filesDir/books/<bookId>.<ext>`, and the row keeps the PATH. Nothing can
 * revoke it, it opens offline, and it is named after the BOOK rather than
 * after the file that happened to be on the member's device.
 */
internal object BookFiles {

    /** The app's own shelf of documents. */
    private fun dir(context: Context): File =
        File(context.filesDir, "books").apply { mkdirs() }

    /**
     * Copies a picked document into the app's own storage and returns its path,
     * or null when the copy could not be made (a picker can hand back a URI
     * whose stream is already gone).
     *
     * A file the book already had is REPLACED, and the old one is deleted: one
     * book, one document — picking again is choosing again, not stacking files.
     */
    fun import(context: Context, bookId: String, uri: Uri, previousPath: String = ""): String? =
        runCatching {
            val extension = extensionFor(context, uri)
            val target = File(dir(context), "$bookId.$extension")
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "the picked file could not be opened" }
                target.outputStream().use { output -> input.copyTo(output) }
            }
            if (previousPath.isNotBlank() && previousPath != target.absolutePath) delete(previousPath)
            target.absolutePath
        }.getOrNull()

    /**
     * The file's extension, taken from the TYPE the provider reports — a
     * `content://` handle usually carries no file name at all, which is the
     * other half of why naming anything after it was wrong.
     */
    private fun extensionFor(context: Context, uri: Uri): String {
        val type = context.contentResolver.getType(uri).orEmpty().lowercase()
        return when {
            type.contains("pdf") -> "pdf"
            type.contains("epub") -> "epub"
            type.startsWith("text/") -> "txt"
            else -> uri.lastPathSegment
                ?.substringAfterLast('.', "")
                ?.lowercase()
                ?.takeIf { it in setOf("pdf", "epub", "txt") }
                ?: "bin"
        }
    }

    /**
     * THE FILE'S REAL NAME.
     *
     * A `content://` handle usually carries no usable name of its own, so the
     * old import named a book from `lastPathSegment` — which for a document
     * provider is an opaque id like `msf:1000000042`, so a library of picked
     * files all arrived as "1000000042". The provider's own DISPLAY_NAME column
     * is the name the member actually sees in their file manager.
     */
    fun displayName(context: Context, uri: Uri): String =
        runCatching {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
                }
        }.getOrNull()
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: ""

    /** Forgets a document (the book picked another one, or left the shelf). */
    fun delete(path: String) {
        if (path.isBlank()) return
        runCatching { File(path).delete() }
    }

    /**
     * The file a book should open: its own document, else the `content://`
     * handle an import left in `coverUrl` before the document had a column of
     * its own. Empty when the book has no file at all.
     */
    fun documentOf(documentPath: String, coverUrl: String): String {
        if (documentPath.isNotBlank()) return documentPath
        val legacy = coverUrl.trim()
        return if (legacy.startsWith("content://") || legacy.startsWith("file://")) legacy else ""
    }
}

/** What a file name says about the book inside it. */
internal data class DetectedBook(val title: String, val author: String)

/**
 * v408 — THE BOOK IN THE FILE'S NAME.
 *
 * A book added straight from a file has no catalogue to ask: the only thing
 * that knows what it is, is what it is called on disk. A downloaded book is
 * usually named properly — `The Odyssey - Homer.epub`, `Homer - The Odyssey
 * (Penguin).pdf` — so the name is parsed rather than thrown away, and the
 * member is shown what was understood and can correct it before the book is
 * created (a guess they can fix beats a shrug they have to retype).
 *
 * It is deliberately conservative: it strips the extension, the obvious
 * download litter (release groups, site tags, ISBN prefixes) and the bracketed
 * noise, splits a Title / Author on a dash or "by", and otherwise hands the
 * name back as the title with no author. NOTHING here is trusted silently —
 * the caller shows the result in editable fields (see BookShelfScreen's import
 * confirmation) and only ever saves what the member confirms.
 *
 * ── v426 — WHAT A PUBLISHER'S FILE NAME ACTUALLY CARRIES ────────────────
 *
 * The member's note was that the guess is "still bad", and the shapes a
 * PUBLISHED book arrives in are the reason: a store's own export is
 * `The Odyssey - Homer (Penguin Classics, 1996).epub`, where the bracket is the
 * publisher, the imprint and the year — facts about the EDITION, never part of
 * the title — and a library's export can carry a second dash with the same
 * litter after it (`Persuasion - Jane Austen - Penguin Classics`), which used to
 * make the author read "Jane Austen - Penguin Classics". So the round bracket is
 * now judged by WHAT IT NAMES ([NOISE_BRACKET]: a publisher, an imprint, an
 * edition, a format, a site or a bare year) rather than by a short list of three
 * sites, a zero-padded leading number is taken off, and a dash-separated TAIL
 * that names one of those is dropped from whichever half it landed in.
 *
 * A square bracket is still a tag whatever it holds — a release group, a site, a
 * quality — which is the one rule that has never needed refining.
 */
internal fun detectBookFromFileName(rawName: String?): DetectedBook {
    if (rawName.isNullOrBlank()) return DetectedBook("", "")

    val withoutExtension = rawName
        .substringAfterLast('/')
        .let { if (it.contains('.')) it.substringBeforeLast('.') else it }

    val cleaned = withoutExtension
        // Underscores and dots are the separators a file name uses where a
        // title would use a space.
        .replace('_', ' ')
        .replace('.', ' ')
        // A square bracket is a tag, whatever it holds.
        .replace(Regex("\\[[^\\]]*\\]"), " ")
        // A round bracket only when it names an EDITION rather than the book.
        .replace(Regex("\\(([^)]*)\\)")) { match ->
            if (NOISE_BRACKET.containsMatchIn(match.groupValues[1])) " " else match.value
        }
        // A leading ISBN (with or without dashes) is a catalogue number, not a
        // title.
        .replace(Regex("^\\s*(97[89][- ]?)?\\d{9}[\\dXx][- ]*"), " ")
        // …and so is a zero-padded track number (`01 - The Odyssey`). Only with
        // a leading zero AND a space, so a title that IS a number keeps it:
        // `07-Ghost` is a book, and stripping its head would rename it.
        .replace(Regex("^\\s*0\\d{1,3}\\s+[-–—]\\s+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trim('-', '–', '—', ',', ':')
        .trim()

    if (cleaned.isEmpty()) return DetectedBook("", "")

    // Title before the author is the common shape of a downloaded book file
    // (`The Odyssey - Homer`). If the member's file says the other way round,
    // both halves are in the fields and either one is one tap to fix.
    val split = Regex("\\s+[-–—]\\s+").find(cleaned)
        ?: Regex("(?i)\\s+by\\s+").find(cleaned)
    if (split != null) {
        val left = dropTrailingEdition(
            cleaned.take(split.range.first).trim('-', '–', '—', ',', ' ', ':')
        )
        val right = dropTrailingEdition(
            cleaned.substring(split.range.last + 1).trim('-', '–', '—', ',', ' ', ':')
        )
        if (left.isNotEmpty() && right.isNotEmpty()) {
            return DetectedBook(
                title = tidyGuess(left),
                author = tidyGuess(right)
            )
        }
    }
    return DetectedBook(tidyGuess(dropTrailingEdition(cleaned)), "")
}

/**
 * What a bracket, or a dash-separated tail, in a book's file name usually is:
 * the publisher and its imprint, the edition, the format, the site it came from
 * or the year — facts about the EDITION rather than about the book (the member's
 * own report was that a publisher's export arrives with all of that still in its
 * title). One regex, matched anywhere inside the part being judged, in the same
 * shape the old three-site list had — just complete enough to catch what a store
 * actually writes.
 */
private val NOISE_BRACKET = Regex(
    "(?i)(" + listOf(
        // Where it came from, and how it was released.
        "z-?lib(rary)?", "libgen", "anna'?s?", "calibre", "epubor", "www\\.", "https?",
        "\\.(com|org|net|cc|io|me|ru|xyz)\\b", "download", "torrent", "retail", "ocr",
        "scann?ed?", "converted", "proof", "arc", "epub", "pdf", "mobi", "azw3?",
        "fb2", "djvu", "cbz", "cbr", "kindle", "e-?book", "digital", "print",
        "hardcover", "paperback", "boxed", "unabridged", "abridged",
        // The edition, and the house that printed it.
        "edition", "\\bed\\.?\\b", "\\bvol\\.", "volumes?", "series",
        "press", "publish(er|ers|ing)?", "classics?", "library", "books?", "imprint",
        "annotated", "illustrated", "translated", "translation", "revised", "reprint",
        "omnibus", "complete", "collection", "works", "v\\d+",
        // A year, or a bare volume/number marker.
        "\\d{4}", "\\b\\d{1,3}\\b"
    ).joinToString("|") + ")"
)

/**
 * Takes off the dash-separated TAILS of a name that say where the book came
 * from rather than what it is (`Jane Austen - Penguin Classics` → `Jane Austen`,
 * `The Odyssey - Vintage Classics 1996` → `The Odyssey`).
 *
 * A tail is only dropped when [NOISE_BRACKET] recognises it, so a name that
 * genuinely holds a dash — a title, a subtitle, an author with a hyphenated
 * surname — is handed back exactly as it was, and the loop stops at the first
 * tail it does not recognise rather than eating its way to the front.
 */
private fun dropTrailingEdition(value: String): String {
    var out = value.trim()
    while (true) {
        val dashes = Regex("\\s+[-–—]\\s+").findAll(out).toList()
        val cut = dashes.lastOrNull() ?: break
        val tail = out.substring(cut.range.last + 1).trim()
        if (cut.range.first <= 0 || tail.isEmpty() || !NOISE_BRACKET.containsMatchIn(tail)) break
        out = out.take(cut.range.first).trim('-', '–', '—', ',', ' ', ':')
    }
    return out
}

/** The first letter leads; the rest of the name is left as the member's file
 *  had it (a title is not to be shouted or title-cased into something it is
 *  not — "the lord of the rings" becomes "The lord of the rings", nothing
 *  more). */
private fun tidyGuess(value: String): String {
    val collapsed = value.replace(Regex("\\s+"), " ").trim()
    if (collapsed.isEmpty()) return ""
    return collapsed.replaceFirstChar { it.uppercase() }
}
