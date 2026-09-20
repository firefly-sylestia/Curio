package com.curio.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * v387 — the PERSONAL WRITING store's queries.
 *
 * Everything the member writes themselves lives in these two tables:
 * journals (`bookId IS NULL`), a book's chapter reviews / notes
 * (`bookId = …`), and the books' own shelf rows. No capture table is touched,
 * so the Cabinet's archive and the personal collection can never contaminate
 * each other's reads.
 */
@Dao
interface PersonalDao {

    // ── Journals ────────────────────────────────────────────────────────

    /** Every live journal, newest day first (an entry with no date sorts by
     *  when it was last written). */
    @Query(
        "SELECT * FROM personal_notes WHERE deletedAt IS NULL AND bookId IS NULL " +
            "ORDER BY dateMillis DESC, updatedAtMillis DESC"
    )
    fun observeJournals(): Flow<List<PersonalNoteEntity>>

    @Query(
        "SELECT * FROM personal_notes WHERE deletedAt IS NULL AND bookId IS NULL " +
            "ORDER BY dateMillis DESC, updatedAtMillis DESC"
    )
    suspend fun journals(): List<PersonalNoteEntity>

    // ── A book's reviews ───────────────────────────────────────────────

    @Query("SELECT * FROM personal_notes WHERE deletedAt IS NULL AND bookId = :bookId ORDER BY chapterIndex ASC")
    fun observeBookNotes(bookId: String): Flow<List<PersonalNoteEntity>>

    @Query("SELECT * FROM personal_notes WHERE deletedAt IS NULL AND bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun bookNotes(bookId: String): List<PersonalNoteEntity>

    // ── The reader's marks (v389) ─────────────────────────────────────
    //
    // One table for both the marks a member MADE and where they stopped
    // reading: "last read" is a row of its own kind ([ReaderMarkKind.POSITION]),
    // so the reader's memory arrived with the same migration as the marks it
    // sits beside and the two can never disagree about the book or the file.

    /** Every mark the member made in THIS file of this book, in reading order. */
    @Query(
        "SELECT * FROM reader_marks WHERE bookId = :bookId AND sourceKey = :sourceKey " +
            "AND kind != 'position' ORDER BY positionIndex ASC, createdAtMillis ASC"
    )
    fun observeReaderMarks(bookId: String, sourceKey: String): Flow<List<ReaderMarkEntity>>

    @Query(
        "SELECT * FROM reader_marks WHERE bookId = :bookId AND sourceKey = :sourceKey " +
            "AND kind != 'position' ORDER BY positionIndex ASC, createdAtMillis ASC"
    )
    suspend fun readerMarks(bookId: String, sourceKey: String): List<ReaderMarkEntity>

    /**
     * Every mark in the BOOK, whichever file it was read in — what the book page
     * shows as its margins. A highlight is kept with the passage's own WORDS, so
     * the page can quote it without opening the file again.
     */
    @Query(
        "SELECT * FROM reader_marks WHERE bookId = :bookId AND kind != 'position' " +
            "ORDER BY chapter ASC, positionIndex ASC, createdAtMillis ASC"
    )
    fun observeBookMarks(bookId: String): Flow<List<ReaderMarkEntity>>

    /** Where the member stopped in THIS file (null when they never opened it). */
    @Query(
        "SELECT * FROM reader_marks WHERE bookId = :bookId AND sourceKey = :sourceKey " +
            "AND kind = 'position' LIMIT 1"
    )
    suspend fun readerPosition(bookId: String, sourceKey: String): ReaderMarkEntity?

    /**
     * v408 — THE SAME ROW, REACTIVELY. The position row is deliberately
     * excluded from every mark query (it is not a mark), so the book page's
     * progress card — which shows the page the reader last had open — could
     * only see it through a one-shot read and went stale the moment the
     * reader moved. This flow emits on every position write, so the card
     * updates while the member reads in the other tab and comes back.
     */
    @Query(
        "SELECT * FROM reader_marks WHERE bookId = :bookId AND sourceKey = :sourceKey " +
            "AND kind = 'position' LIMIT 1"
    )
    fun observeReaderPosition(bookId: String, sourceKey: String): Flow<ReaderMarkEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReaderMark(mark: ReaderMarkEntity)

    @Query("DELETE FROM reader_marks WHERE id = :id")
    suspend fun deleteReaderMark(id: String)

    // ── Books ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM personal_books ORDER BY finishedAtMillis IS NOT NULL, updatedAtMillis DESC")
    fun observeBooks(): Flow<List<PersonalBookEntity>>

    @Query("SELECT * FROM personal_books ORDER BY finishedAtMillis IS NOT NULL, updatedAtMillis DESC")
    suspend fun books(): List<PersonalBookEntity>

    @Query("SELECT * FROM personal_books WHERE id = :id")
    suspend fun book(id: String): PersonalBookEntity?

    @Query("SELECT * FROM personal_books WHERE id = :id")
    fun observeBook(id: String): Flow<PersonalBookEntity?>

    /**
     * The shelf row for a book that came from Curio's own catalog.
     *
     * This is the BRIDGE between the topic page and the personal shelf: the
     * reveal page's book sheets look their book up through its topic id, so a
     * chapter note written there is the SAME row as the chapter review written
     * here — one store, two screens.
     */
    @Query("SELECT * FROM personal_books WHERE catalogId = :catalogId AND catalogId != '' LIMIT 1")
    suspend fun bookForCatalog(catalogId: String): PersonalBookEntity?

    @Query("SELECT * FROM personal_books WHERE catalogId = :catalogId AND catalogId != '' LIMIT 1")
    fun observeBookForCatalog(catalogId: String): Flow<PersonalBookEntity?>

    // ── Single notes ───────────────────────────────────────────────────

    @Query("SELECT * FROM personal_notes WHERE id = :id")
    suspend fun note(id: String): PersonalNoteEntity?

    @Query("SELECT * FROM personal_notes WHERE id = :id")
    fun observeNote(id: String): Flow<PersonalNoteEntity?>

    // ── Pages by kind (a to-do list is a personal page too) ────────────

    /** Every page of one [kind] (`""` = journalling, `"todo"` = a list). */
    @Query(
        "SELECT * FROM personal_notes WHERE deletedAt IS NULL AND bookId IS NULL " +
            "AND topicId = '' AND kind = :kind ORDER BY dateMillis DESC, updatedAtMillis DESC"
    )
    fun observePagesOfKind(kind: String): Flow<List<PersonalNoteEntity>>

    // ── Writes ─────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: PersonalNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBook(book: PersonalBookEntity)

    @Query("UPDATE personal_books SET currentChapter = :chapter, updatedAtMillis = :now WHERE id = :id")
    suspend fun setProgress(id: String, chapter: Int, now: Long)

    /**
     * v409 — the book row's own PAGE mark (see [PersonalBookEntity.currentPage]).
     * Column-scoped on purpose: the reader's position row is the reader's, and
     * a hand move of the page must never rewrite it.
     */
    @Query("UPDATE personal_books SET currentPage = :page, updatedAtMillis = :now WHERE id = :id")
    suspend fun setPage(id: String, page: Int, now: Long)

    /**
     * v413 — the book's own LENGTH (how many chapters it has). Column-scoped
     * for the same reason [setPage] is: the reading card's book-length stepper
     * REPEATS while it is held, and the whole-row `upsertBook` it used to run
     * rewrote every other column from the composition's snapshot of the book on
     * each tick — an out-of-order pair could then land the old length over the
     * new one, and any other edit made in the same window was reverted.
     */
    @Query("UPDATE personal_books SET totalChapters = :total, updatedAtMillis = :now WHERE id = :id")
    suspend fun setTotalChapters(id: String, total: Int, now: Long)

    // `now` is a SEPARATE parameter on purpose: when the book is being marked
    // un-finished, `at` is NULL, and `updatedAtMillis = NULL` on a NOT NULL
    // column makes SQLite reject the whole update — which is why the Mark
    // finished pill appeared to do nothing at all.
    @Query("UPDATE personal_books SET finishedAtMillis = :at, updatedAtMillis = :now WHERE id = :id")
    suspend fun setFinished(id: String, at: Long?, now: Long)

    @Query("UPDATE personal_books SET blurb = :blurb, updatedAtMillis = :now WHERE id = :id")
    suspend fun setBlurb(id: String, blurb: String, now: Long)

    /**
     * The book's OWN FILE, and nothing else (v389). It is a column-scoped
     * write on purpose: attaching a document must never rewrite a title, a
     * blurb, a chapter list or a progress mark — which is exactly what the
     * import path used to do when it named the book after the picked file.
     */
    @Query("UPDATE personal_books SET documentPath = :path, updatedAtMillis = :now WHERE id = :id")
    suspend fun setDocument(id: String, path: String, now: Long)

    /**
     * v410 — the book's own COVER URL, and nothing else. Column-scoped like the
     * other single-fact writes: resolving a cover for a book that had none is
     * not a reason to rewrite its progress, its chapters or its notes. A book
     * added before the shelf carried its topic's cover gets one through here.
     */
    @Query("UPDATE personal_books SET coverUrl = :url, updatedAtMillis = :now WHERE id = :id")
    suspend fun setCoverUrl(id: String, url: String, now: Long)

    @Query("DELETE FROM personal_notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("DELETE FROM personal_books WHERE id = :id")
    suspend fun deleteBook(id: String)

    @Query("DELETE FROM personal_notes WHERE bookId = :bookId")
    suspend fun deleteNotesForBook(bookId: String)

    /** A shelf book by its exact title (the catalog matcher's second pass). */
    @Query("SELECT * FROM personal_books WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun bookByTitle(title: String): PersonalBookEntity?

    // ── A page's own topic ─────────────────────────────────────────────

    /** The member's notes written ABOUT a topic (the "note on a topic" page). */
    @Query(
        "SELECT * FROM personal_notes WHERE deletedAt IS NULL AND topicId = :topicId " +
            "ORDER BY updatedAtMillis DESC"
    )
    fun observeTopicNotes(topicId: String): Flow<List<PersonalNoteEntity>>

    @Query(
        "SELECT * FROM personal_notes WHERE deletedAt IS NULL AND topicId = :topicId " +
            "ORDER BY updatedAtMillis DESC LIMIT 1"
    )
    suspend fun latestTopicNote(topicId: String): PersonalNoteEntity?

    @Query("SELECT COUNT(*) FROM personal_notes WHERE deletedAt IS NULL AND bookId IS NULL")
    suspend fun journalCount(): Int

    @Query("SELECT COUNT(*) FROM personal_books")
    suspend fun bookCount(): Int
}

/**
 * v387 — the single door to the personal writing store.
 *
 * Deliberately a plain class over the DAO (no flow caches, no decode caches):
 * a personal library is dozens of rows, not sixteen thousand, so the work
 * that the capture repository needs to survive a big archive would be pure
 * overhead here. Every write is an upsert of one row, which is what makes
 * auto-save cheap enough to run on every pause in typing.
 */
class PersonalRepository(private val dao: PersonalDao) {

    fun observeJournals(): Flow<List<PersonalNoteEntity>> = dao.observeJournals()
    fun observeBooks(): Flow<List<PersonalBookEntity>> = dao.observeBooks()
    fun observeBook(id: String): Flow<PersonalBookEntity?> = dao.observeBook(id)
    fun observeBookForCatalog(catalogId: String): Flow<PersonalBookEntity?> =
        dao.observeBookForCatalog(catalogId)
    fun observeBookNotes(bookId: String): Flow<List<PersonalNoteEntity>> = dao.observeBookNotes(bookId)
    fun observeNote(id: String): Flow<PersonalNoteEntity?> = dao.observeNote(id)

    suspend fun journals(): List<PersonalNoteEntity> = dao.journals()
    suspend fun books(): List<PersonalBookEntity> = dao.books()
    suspend fun note(id: String): PersonalNoteEntity? = dao.note(id)
    suspend fun book(id: String): PersonalBookEntity? = dao.book(id)
    suspend fun bookNotes(bookId: String): List<PersonalNoteEntity> = dao.bookNotes(bookId)

    /** The shelf row for a catalog book (the topic page's bridge). */
    suspend fun bookForCatalog(catalogId: String): PersonalBookEntity? =
        if (catalogId.isBlank()) null else dao.bookForCatalog(catalogId)

    // ── The reader's marks (v389) ─────────────────────────────────────

    fun observeReaderMarks(bookId: String, sourceKey: String): Flow<List<ReaderMarkEntity>> =
        dao.observeReaderMarks(bookId, sourceKey)

    suspend fun readerMarks(bookId: String, sourceKey: String): List<ReaderMarkEntity> =
        dao.readerMarks(bookId, sourceKey)

    suspend fun readerPosition(bookId: String, sourceKey: String): ReaderMarkEntity? =
        dao.readerPosition(bookId, sourceKey)

    /** v408 — the reactive twin, for the book page's progress card. */
    fun observeReaderPosition(bookId: String, sourceKey: String): Flow<ReaderMarkEntity?> =
        dao.observeReaderPosition(bookId, sourceKey)

    /** The book's margins: every mark in it, across every file it was read in. */
    fun observeBookMarks(bookId: String): Flow<List<ReaderMarkEntity>> =
        dao.observeBookMarks(bookId)

    /**
     * Adds or replaces one mark. An upsert on purpose: re-highlighting the same
     * passage is the SAME mark in a new colour, not a second one stacked on it.
     */
    suspend fun saveReaderMark(mark: ReaderMarkEntity) {
        val now = System.currentTimeMillis()
        dao.upsertReaderMark(
            mark.copy(
                updatedAtMillis = now,
                createdAtMillis = if (mark.createdAtMillis == 0L) now else mark.createdAtMillis
            )
        )
    }

    suspend fun deleteReaderMark(id: String) = dao.deleteReaderMark(id)

    /**
     * The member stopped reading here.
     *
     * ONE row per book + file, rewritten rather than appended: "where was I" is
     * a fact about the book, not a history of it — and it is what makes the
     * reader open where it was left, and what an auto bookmark IS (the marks
     * sheet shows this row as "Last read · auto").
     */
    suspend fun saveReaderPosition(
        bookId: String,
        sourceKey: String,
        index: Int,
        fraction: Float
    ) {
        val now = System.currentTimeMillis()
        val existing = dao.readerPosition(bookId, sourceKey)
        dao.upsertReaderMark(
            (existing ?: ReaderMarkEntity(
                id = newReaderMarkId(),
                bookId = bookId,
                sourceKey = sourceKey,
                kind = ReaderMarkKind.POSITION.key,
                createdAtMillis = now
            )).copy(
                positionIndex = index,
                positionFraction = fraction.coerceIn(0f, 1f),
                updatedAtMillis = now
            )
        )
    }

    suspend fun bookByTitle(title: String): PersonalBookEntity? = dao.bookByTitle(title.trim())

    /**
     * Persists one note. [preview] is recomputed HERE from the document, so
     * a caller can never save a preview that disagrees with the words (the
     * journal list, the Home chips and the book page all read it).
     */
    suspend fun saveNote(note: PersonalNoteEntity): PersonalNoteEntity {
        val stamped = note.copy(
            bodyJson = PersonalDocCodec.encode(note.doc),
            preview = note.doc.plainText.let { text ->
                text.replace('\n', ' ').trim().take(PREVIEW_CHARS)
            },
            updatedAtMillis = System.currentTimeMillis(),
            createdAtMillis = if (note.createdAtMillis == 0L) System.currentTimeMillis()
            else note.createdAtMillis
        )
        dao.upsertNote(stamped)
        return stamped
    }

    /**
     * v389 — THE BOOK'S OWN REVIEW: one note per book with NO chapter index,
     * the whole-book page the member writes in instead of going chapter by
     * chapter. It keeps its identity the same way a chapter's review does (a
     * re-open edits the same row), and it deliberately does NOT move the
     * reading progress: a whole-book review is not a chapter you finished.
     */
    suspend fun saveBookReview(bookId: String, document: PersonalDoc): PersonalNoteEntity {
        val existing = dao.bookNotes(bookId).firstOrNull { it.chapterIndex == null }
        return saveNote(
            PersonalNoteEntity(
                id = existing?.id ?: newNoteId(),
                bookId = bookId,
                chapterIndex = null,
                title = existing?.title.orEmpty(),
                bodyJson = PersonalDocCodec.encode(document),
                preview = "",
                dateMillis = existing?.dateMillis ?: System.currentTimeMillis(),
                mood = existing?.mood.orEmpty(),
                createdAtMillis = existing?.createdAtMillis ?: 0L
            )
        )
    }

    /**
     * Writes one chapter's review — the ONE writer behind both screens: the
     * shelf's chapter page and the topic page's book sheet. It keeps the note's
     * identity (so a review written in one view is edited, never duplicated by
     * the other), and it moves the book's progress forward, because writing
     * about a chapter is what reading it looks like.
     */
    suspend fun saveChapterNote(
        bookId: String,
        chapter: Int,
        document: PersonalDoc,
        chapterTitle: String = ""
    ): PersonalNoteEntity {
        val existing = dao.bookNotes(bookId).firstOrNull { it.chapterIndex == chapter }
        val saved = saveNote(
            PersonalNoteEntity(
                id = existing?.id ?: newNoteId(),
                bookId = bookId,
                chapterIndex = chapter,
                title = existing?.title ?: "Chapter $chapter",
                bodyJson = PersonalDocCodec.encode(document),
                preview = "",
                dateMillis = existing?.dateMillis ?: System.currentTimeMillis(),
                mood = existing?.mood.orEmpty(),
                chapterTitle = when {
                    chapterTitle.isNotBlank() -> chapterTitle
                    else -> existing?.chapterTitle.orEmpty()
                },
                createdAtMillis = existing?.createdAtMillis ?: 0L,
                updatedAtMillis = 0L
            )
        )
        val book = dao.book(bookId)
        // A finished book's chapters are all closed (see [setFinished]); writing
        // about chapter 4 afterwards must not re-open the list behind it.
        if (book != null && !book.isFinished && chapter > book.currentChapter) {
            setProgress(bookId, chapter)
        }
        return saved
    }

    suspend fun saveBook(book: PersonalBookEntity) {
        dao.upsertBook(
            book.copy(
                updatedAtMillis = System.currentTimeMillis(),
                createdAtMillis = if (book.createdAtMillis == 0L) System.currentTimeMillis()
                else book.createdAtMillis
            )
        )
    }

    suspend fun setProgress(bookId: String, chapter: Int) =
        dao.setProgress(bookId, chapter.coerceAtLeast(0), System.currentTimeMillis())

    /** v409 — the book row's own page mark (a hand move on the book page). */
    suspend fun setPage(bookId: String, page: Int) =
        dao.setPage(bookId, page.coerceAtLeast(0), System.currentTimeMillis())

    /**
     * v413 — the book row's own LENGTH (a hand move on the reading card's book
     * length rail). A column-scoped write like [setPage] and [setProgress]: the
     * stepper repeats while held, so the row's other columns must not travel
     * with each tick.
     */
    suspend fun setTotalChapters(bookId: String, total: Int) =
        dao.setTotalChapters(bookId, total.coerceAtLeast(0), System.currentTimeMillis())

    /**
     * v409 — FINISHING CLOSES THE CHAPTERS, UN-FINISHING PUTS THEM BACK.
     *
     * A book that is finished has read all of it, so its chapter list closes
     * with it (the member asked for exactly that: "when marked finished they
     * should be automatically finished too"). The place they had really
     * reached is stashed first, so "Reading again" restores the previous marks
     * instead of dropping the reader back to the beginning ("when unmark
     * restoring the previous marked").
     *
     * Read-modify-write rather than a column-scoped UPDATE because both moves
     * touch four columns and have to agree with each other.
     */
    suspend fun setFinished(bookId: String, finished: Boolean) {
        val book = dao.book(bookId) ?: return
        val now = System.currentTimeMillis()
        if (finished) {
            dao.upsertBook(
                book.copy(
                    finishedAtMillis = now,
                    chapterBeforeFinish = book.currentChapter,
                    pageBeforeFinish = book.currentPage,
                    currentChapter = if (book.totalChapters > 0) book.totalChapters
                    else book.currentChapter,
                    currentPage = if (book.pageCount > 0) book.pageCount else book.currentPage,
                    updatedAtMillis = now
                )
            )
        } else {
            dao.upsertBook(
                book.copy(
                    finishedAtMillis = null,
                    currentChapter = if (book.chapterBeforeFinish >= 0) book.chapterBeforeFinish
                    else book.currentChapter,
                    currentPage = if (book.pageBeforeFinish >= 0) book.pageBeforeFinish
                    else book.currentPage,
                    chapterBeforeFinish = -1,
                    pageBeforeFinish = -1,
                    updatedAtMillis = now
                )
            )
        }
    }

    suspend fun setBlurb(bookId: String, blurb: String) =
        dao.setBlurb(bookId, blurb, System.currentTimeMillis())

    /**
     * v389 — points the book at its own file. Only [documentPath] moves: the
     * title, the blurb, the chapters and the progress are the member's, and
     * attaching a PDF is not a reason to touch any of them.
     */
    suspend fun setDocument(bookId: String, path: String) =
        dao.setDocument(bookId, path, System.currentTimeMillis())

    /**
     * v410 — writes the cover URL a shelf book finally resolved. Nothing but
     * [PersonalBookEntity.coverUrl] moves: the row may be mid-read, and finding
     * its artwork is not a reason to touch its progress or its notes.
     */
    suspend fun setCoverUrl(bookId: String, url: String) {
        if (url.isBlank()) return
        dao.setCoverUrl(bookId, url, System.currentTimeMillis())
    }

    /**
     * Stores the chapter list a book learned (Open Library's table of
     * contents). The book's length follows the list when it was unknown, so
     * the progress stepper and the chapter rows agree from the same fact.
     */
    suspend fun setBookChapters(bookId: String, chapters: List<PersonalChapter>) {
        if (chapters.isEmpty()) return
        val current = dao.book(bookId) ?: return
        dao.upsertBook(
            current.copy(
                chaptersJson = PersonalChapterCodec.encode(chapters),
                totalChapters = if (current.totalChapters <= 0) chapters.size
                else current.totalChapters,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    /**
     * v389e — THE MEMBER'S OWN FILE, AS THE BOOK'S OWN FACTS.
     *
     * Called the moment a document is wired to a book (see BookDetailScreen).
     * The file is the book: what IT says about its chapters and its length is the
     * truth about the copy on this phone, while a catalog's chapter list is the
     * truth about Curio's edition and a lookup's is about somebody else's (user
     * request: "when i add a books own file it takes over the fetched file, and
     * takes info from the book if there is one. also the page number from the
     * file").
     *
     * So this is the one write allowed to REPLACE a fetched chapter list — and it
     * still refuses to invent: a file with no contents of its own (a scan, a plain
     * text) leaves what the lookup found exactly where it was, and a file whose
     * length cannot be read leaves the page count alone.
     */
    suspend fun adoptDocumentFacts(
        bookId: String,
        chapters: List<PersonalChapter>,
        pageCount: Int
    ) {
        val current = dao.book(bookId) ?: return
        if (chapters.isEmpty() && pageCount <= 0) return
        dao.upsertBook(
            current.copy(
                chaptersJson = if (chapters.isEmpty()) current.chaptersJson
                else PersonalChapterCodec.encode(chapters),
                totalChapters = if (chapters.isEmpty()) current.totalChapters else chapters.size,
                pageCount = if (pageCount > 0) pageCount else current.pageCount,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    fun observeTopicNotes(topicId: String): Flow<List<PersonalNoteEntity>> =
        dao.observeTopicNotes(topicId)

    suspend fun latestTopicNote(topicId: String): PersonalNoteEntity? =
        dao.latestTopicNote(topicId)

    fun observePagesOfKind(kind: String): Flow<List<PersonalNoteEntity>> =
        dao.observePagesOfKind(kind)

    suspend fun deleteNote(id: String) = dao.deleteNote(id)

    /** Removing a book takes its reviews with it (they have nowhere to live
     *  once the shelf row is gone). */
    suspend fun deleteBook(bookId: String) {
        dao.deleteNotesForBook(bookId)
        dao.deleteBook(bookId)
    }

    suspend fun journalCount(): Int = dao.journalCount()
    suspend fun bookCount(): Int = dao.bookCount()

    companion object {
        /** How much plain text a stored preview keeps (list rows only ever
         *  draw one or two lines). */
        const val PREVIEW_CHARS = 180
    }
}

/**
 * v387 — process-wide holder for [PersonalRepository], mirroring
 * [CurioRepositoryHolder]: [MainActivity] installs it right after the Room
 * database is built, so a screen can ask for it without touching the
 * database singleton itself.
 */
object PersonalRepositoryHolder {
    @Volatile
    private var _repo: PersonalRepository? = null

    val repo: PersonalRepository
        get() = _repo ?: error(
            "PersonalRepository not initialized. Call PersonalRepositoryHolder.init() " +
                "in MainActivity.onCreate() before any personal screen reads it."
        )

    fun init(dao: PersonalDao) {
        if (_repo == null) {
            synchronized(this) {
                if (_repo == null) _repo = PersonalRepository(dao)
            }
        }
    }
}
