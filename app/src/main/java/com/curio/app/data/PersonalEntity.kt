package com.curio.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * v387 — ONE PIECE OF PERSONAL WRITING: a journal day, a book's chapter
 * review, or the book's own note (`bookId == null` ⇔ a journal entry).
 *
 * [bodyJson] is the [PersonalDoc] block list (see [PersonalDocCodec]) and
 * [preview] is its plain-text projection, stored so the journal list, the
 * Home chips and the book page can render a line WITHOUT decoding every
 * document on every emission (the same reason [CaptureEntityLight] exists for
 * the Cabinet: a reactive query re-materialises every selected column).
 *
 * [dateMillis] is the DAY the entry belongs to — a journal's date is editable
 * (the date changer), so it is NOT the same thing as [updatedAtMillis].
 */
@Entity(
    tableName = "personal_notes",
    indices = [Index("bookId"), Index("dateMillis")]
)
data class PersonalNoteEntity(
    @PrimaryKey val id: String,
    /** The book this note belongs to; null for a journal entry. */
    val bookId: String? = null,
    /** 1-based chapter the review is about (book notes only). */
    val chapterIndex: Int? = null,
    val title: String = "",
    val bodyJson: String = "",
    val preview: String = "",
    /** The journal's own day (local midnight + millis into it). */
    val dateMillis: Long = 0L,
    /** [PersonalMood.key], or "" when no mood was picked. */
    val mood: String = "",
    /** Chapter reviews only: the progress the chapter marker stood at when it
     *  was written, so a review keeps the place it was written from. */
    val chapterTitle: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    /** Soft delete — a removed journal is recoverable inside the bin flow
     *  (the Cabinet's personal view offers restore). NULL = live. */
    val deletedAt: Long? = null,
    /**
     * WHAT KIND OF PAGE THIS IS. [PAGE_KIND_JOURNAL] is a day's journal,
     * [PAGE_KIND_TODO] a checklist — deliberately a string, so a third kind
     * is a value rather than a migration.
     */
    val kind: String = PAGE_KIND_JOURNAL,
    /**
     * Set when the page was written ABOUT a topic (the "+" sheet's *A note on
     * a topic*, or the topic picker on the note page itself): the topic's id,
     * its name and its lane. That is what puts the topic at the head of the
     * saved page, tells the journals list what a page is about, and gives the
     * page its way through to the topic's own reveal page.
     */
    val topicId: String = "",
    val topicName: String = "",
    val categoryId: String = ""
) {
    val doc: PersonalDoc get() = PersonalDocCodec.decode(bodyJson)
    val moodEnum: PersonalMood? get() = PersonalMood.fromKey(mood)

    /** True for a checklist page (its rows are tickable). */
    val isTodo: Boolean get() = kind == PAGE_KIND_TODO

    /** True when this page was written about a topic. */
    val hasTopic: Boolean get() = topicId.isNotBlank()
}

/** A page's kind. Stored in `personal_notes.kind`. */
const val PAGE_KIND_JOURNAL = ""
const val PAGE_KIND_TODO = "todo"

/**
 * v387 — ONE BOOK ON THE PERSONAL SHELF.
 *
 * A book the member is writing about: cover, author, how many chapters it
 * has and where they are in it. The book's reviews are [PersonalNoteEntity]
 * rows with [PersonalNoteEntity.bookId] set to this id — so "write a review
 * per chapter, track progress" is a book row plus N note rows, and the shelf
 * renders a cover with the progress bar under it.
 */
@Entity(tableName = "personal_books")
data class PersonalBookEntity(
    @PrimaryKey val id: String,
    val title: String = "",
    val author: String = "",
    /** Cover URL (Open Library, or any pasted URL) — empty draws a
     *  code-generated cover from the title instead. */
    val coverUrl: String = "",
    /** 0 = the member has not said how long the book is; the chapter list
     *  then offers "add a chapter" instead of numbered rows. */
    val totalChapters: Int = 0,
    /**
     * v388 — the APP CATALOG's topic id, e.g. `book-iliad`, when the book was
     * added from Curio's own ~800-book lane rather than typed in or found on
     * Open Library. It is the handle the book's page uses to read the real
     * chapter names, page ranges and summaries back out of the topic JSON.
     * Blank for a book the catalog does not have. */
    val catalogId: String = "",
    /** The catalog's page count for this edition (0 when unknown) — shown
     *  beside the chapter list so "how long is this book" has an answer that
     *  comes from Curio rather than from a guess. */
    val pageCount: Int = 0,
    /** How far they are (1-based; 0 = not started). */
    val currentChapter: Int = 0,
    /** The book's own note — why they picked it up. */
    val blurb: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    /** Non-null when the member marked the book finished. */
    val finishedAtMillis: Long? = null,
    /**
     * THE BOOK'S CHAPTER LIST when it did not come from Curio's own catalog —
     * the table of contents read out of Open Library ([PersonalChapterCodec]).
     * A catalog book leaves this empty and reads its chapters from the topic
     * JSON instead (see `BookCatalog`), which is the same list its lane shows.
     */
    val chaptersJson: String = "",
    /**
     * THE BOOK'S ABOUT-TEXT when it did not come from Curio's own catalog:
     * Open Library's description of the work (see `BookEnrichment`), kept on
     * the row so the page's "About this book" card is there offline too. A
     * catalog book leaves this empty — its card reads the catalog's own
     * synopsis through [catalogId]. The column itself has been in the schema
     * since migration 16 → 17; this is the row finally reading it.
     */
    val synopsis: String = ""
) {
    /** 0f..1f reading progress, 0 when the length is unknown. */
    val progress: Float
        get() = if (totalChapters <= 0) 0f
        else (currentChapter.toFloat() / totalChapters.toFloat()).coerceIn(0f, 1f)

    val isFinished: Boolean get() = finishedAtMillis != null

    /** The learned chapter list, in order. */
    val chapters: List<PersonalChapter> get() = PersonalChapterCodec.decode(chaptersJson)
}
