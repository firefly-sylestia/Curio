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
    // Re-reads only when the book (or the list it learned) actually changes.
    val state = produceState(initialValue = stored, catalogId, book?.chaptersJson) {
        value = if (catalogId.isBlank()) stored
        else BookCatalog.chapters(catalogId).map { it.asPersonal() }
    }
    return state.value
}
