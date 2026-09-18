package com.curio.app.features.personal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.curio.app.data.BookChapter
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipFile

/**
 * ONE BOOK'S CHAPTER LIST, whichever way the book came in by.
 *
 * Two doors, one shape: a book added from Curio's own lane carries a topic id
 * and its chapters live in the topic JSON (`BookCatalog` — real names, pages
 * and summaries, the lane's own list), while a book from Open Library carries
 * the table of contents the shelf read for it
 * ([com.curio.app.data.PersonalChapter] in `personal_books.chaptersJson`).
 *
 * Everything downstream (the book page's chapter rows, the chapter page's
 * header and summary) reads [PersonalChapter] through here, so neither screen
 * has to know which door the book came in by — and a catalog book always wins
 * when both could answer, because the catalog is Curio's own curated text.
 */

/** The catalog's chapter as the personal shape the screens render. */
internal fun BookChapter.asPersonal(): PersonalChapter = PersonalChapter(
    number = number,
    title = title,
    pageStart = pageStart,
    pageEnd = pageEnd,
    summary = summary
)

/**
 * The chapters to show for [book]: the MEMBER'S OWN FILE first when the book is
 * wired to one, then the catalog's own list, then the list learned from Open
 * Library, else nothing (the book page then offers numbered rows instead).
 *
 * v389e — AND THE FILE WINS.
 *
 * An attached document is the book itself, so what IT says about its chapters
 * and its pages is the truth about the book on the phone, while a catalog's list
 * is the truth about Curio's edition and a lookup's is about someone's
 * (user request: "when i add a books own file it takes over the fetched file, and
 * takes info from the book if there is one. also the page number from the file").
 */
@Composable
internal fun rememberBookChapters(book: PersonalBookEntity?): List<PersonalChapter> {
    val catalogId = book?.catalogId.orEmpty()
    val stored = book?.chapters.orEmpty()
    val document = book?.documentPath.orEmpty()
    val context = LocalContext.current
    // ── THE ANSWER THE LAST VISIT ENDED ON (v389c) ────────────────────────
    //
    // This can only answer once the book's ROW has arrived, and that is a Room
    // read — so a book page opened for the second time still began with no
    // chapters at all and then filled them in: numbered placeholder rows that
    // turned into real names and page ranges a couple of frames later (user
    // report: "when i close and open again for a berif moment i see pages").
    // The last resolved list is kept per book (see BookPageMemory) and used as
    // the FIRST value, so the reopen's first frame is the answer its last visit
    // ended on; the list below is still what actually decides, and it wins the
    // moment the book is readable.
    val remembered = BookPageMemory.chapters(book)
    // Re-reads only when the book (or the list it learned, or the file it is
    // wired to) actually changes.
    val state = produceState(
        initialValue = stored.ifEmpty { remembered },
        catalogId,
        book?.chaptersJson,
        document
    ) {
        // The file's own contents cost a parse, so they are read OFF the main
        // thread and only when there is a file to read.
        val fromFile = if (document.isBlank()) emptyList() else withContext(Dispatchers.IO) {
            documentChapters(context, document)
        }
        val resolved = when {
            fromFile.isNotEmpty() -> fromFile
            catalogId.isNotBlank() -> BookCatalog.chapters(catalogId).map { it.asPersonal() }
            else -> stored
        }
        val answer = resolved.ifEmpty { remembered }
        BookPageMemory.rememberChapters(book, answer)
        value = answer
    }
    return state.value
}

/**
 * THE MEMBER'S OWN FILE, AS A CHAPTER LIST.
 *
 * An EPUB carries its contents in its nav document or NCX and a PDF in its own
 * outline; both are already read for the reader's chapter sheet (see
 * [epubOutline] / [pdfOutline]), so this is the same reading in the shape the
 * BOOK PAGE renders. A PDF's entries name the page each chapter OPENS, so a
 * chapter's own range runs to where the next one starts — and the last one to the
 * file's last page, which is the file's own page count and not a lookup's guess.
 *
 * A file with no contents (a scanned novel, a plain text) answers nothing, and
 * the fetched list stands.
 *
 * Internal rather than private because the page that ATTACHES a file uses it
 * too: the moment a document is wired, the row is rewritten from it (see
 * `PersonalRepository.adoptDocumentFacts`).
 */
internal fun documentChapters(
    context: android.content.Context,
    document: String
): List<PersonalChapter> {
    val lower = document.lowercase()
    val isPdf = lower.endsWith(".pdf")
    val outline = when {
        lower.endsWith(".epub") -> runCatching {
            ZipFile(document).use { zip -> epubOutline(zip) }
        }.getOrDefault(emptyList())
        isPdf -> runCatching { pdfOutline(context, document) }.getOrDefault(emptyList())
        else -> emptyList()
    }
    if (outline.isEmpty()) return emptyList()
    val lastPage = if (isPdf) pdfPageCount(context, document) else 0
    return outline.mapIndexed { index, entry ->
        val start = if (entry.isPage) entry.page else 0
        val end = if (start <= 0) 0 else {
            val next = outline.drop(index + 1).firstOrNull { it.isPage && it.page > start }?.page
            ((next ?: (lastPage + 1)) - 1).coerceAtLeast(start)
        }
        PersonalChapter(
            number = index + 1,
            title = entry.title,
            pageStart = start,
            pageEnd = end
        )
    }
}
