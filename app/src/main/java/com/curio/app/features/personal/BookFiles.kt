package com.curio.app.features.personal

import android.content.Context
import android.net.Uri
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
