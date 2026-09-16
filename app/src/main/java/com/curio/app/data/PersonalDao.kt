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

    // `now` is a SEPARATE parameter on purpose: when the book is being marked
    // un-finished, `at` is NULL, and `updatedAtMillis = NULL` on a NOT NULL
    // column makes SQLite reject the whole update — which is why the Mark
    // finished pill appeared to do nothing at all.
    @Query("UPDATE personal_books SET finishedAtMillis = :at, updatedAtMillis = :now WHERE id = :id")
    suspend fun setFinished(id: String, at: Long?, now: Long)

    @Query("UPDATE personal_books SET blurb = :blurb, updatedAtMillis = :now WHERE id = :id")
    suspend fun setBlurb(id: String, blurb: String, now: Long)

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
        if (book != null && chapter > book.currentChapter) {
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
        dao.setProgress(bookId, chapter, System.currentTimeMillis())

    suspend fun setFinished(bookId: String, finished: Boolean) =
        dao.setFinished(bookId, if (finished) System.currentTimeMillis() else null, System.currentTimeMillis())

    suspend fun setBlurb(bookId: String, blurb: String) =
        dao.setBlurb(bookId, blurb, System.currentTimeMillis())

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
