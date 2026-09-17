package com.curio.app.features.personal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import com.curio.app.data.BookChapter
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalChapter

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
 * The chapters to show for [book]: the catalog's own list when the book is a
 * catalog book, else the list learned from Open Library, else nothing (the
 * book page then offers numbered rows instead).
 */
@Composable
internal fun rememberBookChapters(book: PersonalBookEntity?): List<PersonalChapter> {
    val catalogId = book?.catalogId.orEmpty()
    val stored = book?.chapters.orEmpty()
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
    // Re-reads only when the book (or the list it learned) actually changes.
    val state = produceState(
        initialValue = stored.ifEmpty { remembered },
        catalogId,
        book?.chaptersJson
    ) {
        val resolved = if (catalogId.isBlank()) stored
        else BookCatalog.chapters(catalogId).map { it.asPersonal() }
        val answer = resolved.ifEmpty { remembered }
        BookPageMemory.rememberChapters(book, answer)
        value = answer
    }
    return state.value
}
