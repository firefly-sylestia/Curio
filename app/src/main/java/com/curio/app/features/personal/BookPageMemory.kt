package com.curio.app.features.personal

import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalChapter
import com.curio.app.data.PersonalNoteEntity

/**
 * v389c — WHAT A BOOK PAGE LOOKED LIKE, LAST TIME IT WAS OPEN.
 *
 * Every read a book page makes is a FLOW from Room, and a flow's first value
 * arrives a frame or two after the page is composed — so a book opened for the
 * second time still started from nothing and then filled itself in: the head
 * without its title, the chapter rows as numbered placeholders instead of the
 * names and page ranges the file gave them, and a chapter that has a review
 * briefly showing as unwritten (user report: "the chapter view well sometimes it
 * reload like it fetches the chapter notes etc then when i close and open again
 * for a berif moment i see pages").
 *
 * A database read cannot be made faster by wanting it more; what CAN be done is
 * to start the second visit where the first one ended. This is a small
 * process-level memory of the three things that page is built from, keyed by the
 * book, and it is used ONLY as the first value of each flow — the moment Room
 * emits (which is always, and always authoritative), the real answer replaces it.
 * So nothing here can show anything the database would not; it can only stop the
 * page from forgetting for two frames.
 *
 * It is bounded on purpose: an app is opened on a handful of books, the entries
 * are small (a chapter list, one row, a note list), and the map is cleared by
 * the process dying like every other in-memory cache in the app. Clearing the
 * library, deleting a book or signing out does not need to touch it — the flows
 * win on the first emission and a deleted book's page simply stops composing.
 */
internal object BookPageMemory {

    /**
     * A chapter list is only reusable for the SAME source: a catalog id or a
     * remade table of contents (a re-lookup) changes what the list should be, so
     * the answer is stamped with what produced it and a changed fingerprint
     * simply misses.
     */
    private class Chapters(val fingerprint: String, val list: List<PersonalChapter>)

    private val chapters = HashMap<String, Chapters>()
    private val rows = HashMap<String, PersonalBookEntity>()
    private val notes = HashMap<String, List<PersonalNoteEntity>>()

    /** What a book's chapters were, when the book has not changed underneath. */
    fun chapters(book: PersonalBookEntity?): List<PersonalChapter> {
        val id = book?.id ?: return emptyList()
        val remembered = chapters[id] ?: return emptyList()
        return if (remembered.fingerprint == chapterFingerprint(book)) remembered.list else emptyList()
    }

    fun rememberChapters(book: PersonalBookEntity?, list: List<PersonalChapter>) {
        val id = book?.id ?: return
        if (list.isEmpty()) return
        chapters[id] = Chapters(chapterFingerprint(book), list)
    }

    private fun chapterFingerprint(book: PersonalBookEntity?): String =
        "${book?.catalogId.orEmpty()}|${book?.chaptersJson.orEmpty()}"

    /** The book's own row, as the last visit saw it. */
    fun row(bookId: String): PersonalBookEntity? = rows[bookId]

    fun rememberRow(book: PersonalBookEntity?) {
        val id = book?.id ?: return
        rows[id] = book
    }

    /** The book's notes, as the last visit saw them. */
    fun notes(bookId: String): List<PersonalNoteEntity> = notes[bookId].orEmpty()

    fun rememberNotes(bookId: String, value: List<PersonalNoteEntity>) {
        if (bookId.isBlank()) return
        notes[bookId] = value
    }
}
