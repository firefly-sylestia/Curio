package com.curio.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CaptureEntity::class, TopicEntity::class, CachedTopicEntity::class,
        // v387 — the personal writing store (journals + books + chapter
        // reviews). Its own tables, never the capture archive's.
        PersonalNoteEntity::class, PersonalBookEntity::class
    ],
    version = 17,
    exportSchema = false
)
abstract class CurioDatabase : RoomDatabase() {

    abstract fun captureDao(): CaptureDao
    abstract fun topicDao(): TopicDao
    abstract fun cachedTopicDao(): CachedTopicDao
    abstract fun personalDao(): PersonalDao

    companion object {
        @Volatile
        private var INSTANCE: CurioDatabase? = null

        /**
         * v1 → v2 (v7.17): custom user tags. Adds the `tagsJson` column to
         * every saved capture with an empty-array default so existing entries
         * read as tag-less (the entity's Kotlin default matches this string).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE captures ADD COLUMN tagsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /**
         * v2 → v3: persist explicit FieldMind restore provenance. The
         * backfill keeps entries imported by older Curio builds marked as
         * legacy while all native captures default to false.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE captures ADD COLUMN isLegacy INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE captures SET isLegacy = 1 WHERE topicSubtype = 'Legacy'")
            }
        }

        /**
         * v3 → v4 (v17): explore-session duration per capture. Adds the
         * `sessionTimeMillis` column with a zero default so existing entries
         * read as no-session (the entity's Kotlin default matches this).
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE captures ADD COLUMN sessionTimeMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v4 → v5 (v26): recycle bin. Adds the nullable `deletedAt` column —
         * NULL means live, a timestamp means the capture sits in the recycle
         * bin. Existing rows stay live (NULL), so no backfill is needed.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE captures ADD COLUMN deletedAt INTEGER")
            }
        }

        /**
         * v5 → v6 (v27): explore-session attachments per capture. Adds the
         * nullable `sessionNote` column (NULL = no note) and the
         * `sessionScreenshotsJson` array with an empty-array default so
         * existing entries read as attachment-less (the entity's Kotlin
         * defaults match these).
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE captures ADD COLUMN sessionNote TEXT")
                db.execSQL("ALTER TABLE captures ADD COLUMN sessionScreenshotsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /**
         * v6 → v7 (v52b): per-topic progress metadata. Adds the nullable
         * `pageCount` / `episodeCount` columns (NULL = no progress tracking)
         * so saved Books/Anime entries can reconstruct their progress target
         * even before the topic catalog cache is loaded — the Cabinet thin
         * progress line and the detail progress pill read them. The entity's
         * Kotlin defaults match (null); existing rows read as no-progress.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE captures ADD COLUMN pageCount INTEGER")
                db.execSQL("ALTER TABLE captures ADD COLUMN episodeCount INTEGER")
            }
        }


        /** v7 → v8 (v294): Room topics table for pre-populated topic database. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS topics (
                        id TEXT NOT NULL PRIMARY KEY,
                        categoryId TEXT NOT NULL,
                        subtype TEXT NOT NULL,
                        name TEXT NOT NULL,
                        teaser TEXT NOT NULL,
                        imageUrl TEXT NOT NULL DEFAULT '',
                        byline TEXT NOT NULL DEFAULT '',
                        tags TEXT NOT NULL DEFAULT '',
                        tier INTEGER NOT NULL DEFAULT 1,
                        exploreVerb TEXT NOT NULL DEFAULT '',
                        exploreTargetName TEXT NOT NULL DEFAULT '',
                        exploreDurationMinutes INTEGER NOT NULL DEFAULT 0,
                        exploreInstruction TEXT NOT NULL DEFAULT '',
                        pageCount INTEGER,
                        episodeCount INTEGER,
                        altPageLabel TEXT NOT NULL DEFAULT '',
                        altPageCount INTEGER
                    )
                """)
            }
        }

        /** v8 → v9: repair topic defaults and remove undeclared legacy indexes. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE topics_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        categoryId TEXT NOT NULL,
                        subtype TEXT NOT NULL,
                        name TEXT NOT NULL,
                        teaser TEXT NOT NULL,
                        imageUrl TEXT NOT NULL DEFAULT '',
                        byline TEXT NOT NULL DEFAULT '',
                        tags TEXT NOT NULL DEFAULT '',
                        tier INTEGER NOT NULL DEFAULT 1,
                        exploreVerb TEXT NOT NULL DEFAULT '',
                        exploreTargetName TEXT NOT NULL DEFAULT '',
                        exploreDurationMinutes INTEGER NOT NULL DEFAULT 0,
                        exploreInstruction TEXT NOT NULL DEFAULT '',
                        pageCount INTEGER DEFAULT 0,
                        episodeCount INTEGER DEFAULT 0,
                        altPageLabel TEXT NOT NULL DEFAULT '',
                        altPageCount INTEGER DEFAULT 0
                    )
                """)
                db.execSQL("""
                    INSERT INTO topics_new (
                        id, categoryId, subtype, name, teaser, imageUrl, byline, tags,
                        tier, exploreVerb, exploreTargetName, exploreDurationMinutes,
                        exploreInstruction, pageCount, episodeCount, altPageLabel, altPageCount
                    )
                    SELECT id, categoryId, subtype, name, teaser, imageUrl, byline,
                        tags,
                        tier, exploreVerb, exploreTargetName, exploreDurationMinutes,
                        exploreInstruction, pageCount, episodeCount, altPageLabel, altPageCount
                    FROM topics
                """)
                db.execSQL("DROP TABLE topics")
                db.execSQL("ALTER TABLE topics_new RENAME TO topics")
            }
        }

        /** v9 → v10 (v294): cached_topics table for durable topic data on save. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_topics (
                        id TEXT NOT NULL PRIMARY KEY,
                        categoryId TEXT NOT NULL,
                        subtype TEXT NOT NULL,
                        name TEXT NOT NULL,
                        teaser TEXT NOT NULL,
                        imageUrl TEXT NOT NULL DEFAULT '',
                        byline TEXT NOT NULL DEFAULT '',
                        tags TEXT NOT NULL DEFAULT '',
                        tier INTEGER NOT NULL DEFAULT 1,
                        exploreVerb TEXT NOT NULL DEFAULT '',
                        exploreTargetName TEXT NOT NULL DEFAULT '',
                        exploreDurationMinutes INTEGER NOT NULL DEFAULT 0,
                        exploreInstruction TEXT NOT NULL DEFAULT '',
                        pageCount INTEGER DEFAULT 0,
                        episodeCount INTEGER DEFAULT 0,
                        altPageLabel TEXT NOT NULL DEFAULT '',
                        altPageCount INTEGER DEFAULT 0
                    )
                """)
            }
        }

        /** v10 → v11: add synopsis + chapters columns for book topics. */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE topics ADD COLUMN synopsis TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE topics ADD COLUMN chapters TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cached_topics ADD COLUMN synopsis TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cached_topics ADD COLUMN chapters TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v11 → v12: add tracks column for album topics (track-list sheet). */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE topics ADD COLUMN tracks TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cached_topics ADD COLUMN tracks TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v12 → v13: add geniusUrl column for album topics (Genius link). */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE topics ADD COLUMN geniusUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cached_topics ADD COLUMN geniusUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v13 → v14 (v348): per-episode guide column (series only). */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE topics ADD COLUMN episodes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cached_topics ADD COLUMN episodes TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * v14 → v15 (v387): the personal writing store. Two brand-new tables
         * — nothing existing is touched, so the capture archive, the topic
         * catalog and the cached topics migrate by simply being left alone.
         * `personal_notes` holds journals (bookId NULL) and a book's chapter
         * reviews / notes (bookId set); `personal_books` is the shelf.
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS personal_notes (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT,
                        chapterIndex INTEGER,
                        title TEXT NOT NULL DEFAULT '',
                        bodyJson TEXT NOT NULL DEFAULT '',
                        preview TEXT NOT NULL DEFAULT '',
                        dateMillis INTEGER NOT NULL DEFAULT 0,
                        mood TEXT NOT NULL DEFAULT '',
                        chapterTitle TEXT NOT NULL DEFAULT '',
                        createdAtMillis INTEGER NOT NULL DEFAULT 0,
                        updatedAtMillis INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_notes_bookId ON personal_notes (bookId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_notes_dateMillis ON personal_notes (dateMillis)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS personal_books (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL DEFAULT '',
                        author TEXT NOT NULL DEFAULT '',
                        coverUrl TEXT NOT NULL DEFAULT '',
                        totalChapters INTEGER NOT NULL DEFAULT 0,
                        currentChapter INTEGER NOT NULL DEFAULT 0,
                        blurb TEXT NOT NULL DEFAULT '',
                        createdAtMillis INTEGER NOT NULL DEFAULT 0,
                        updatedAtMillis INTEGER NOT NULL DEFAULT 0,
                        finishedAtMillis INTEGER
                    )
                    """
                )
            }
        }

        /**
         * v15 → v16 — the shelf learns where a book CAME from.
         *
         * A book added from Curio's own catalog keeps its topic id and the
         * catalog's page count, which is what lets its page show the real
         * chapter names, page ranges and summaries the topic JSON already
         * carries instead of numbering the rows by hand. Both columns are plain
         * defaults, so every existing shelf row survives the upgrade as a book
         * the catalog does not know — which is exactly what it is.
         */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE personal_books ADD COLUMN catalogId TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE personal_books ADD COLUMN pageCount INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * v16 → v17 (v389) — what a hand-added book learned about itself, and
         * the two new kinds of personal page.
         *
         *  · `personal_books.chaptersJson` — the chapter list read from Open
         *    Library's table of contents, so a book Curio's own catalog does
         *    not have still opens with real chapter names and page ranges
         *    instead of "Chapter 7".
         *  · `personal_notes.kind` — "" for a journal day, "todo" for a
         *    checklist page, so ONE store serves both without guessing which
         *    is which from an empty body.
         *  · `personal_notes.topicId` / `topicName` / `categoryId` — the page
         *    written ABOUT a topic (the "+" sheet's note on a topic), which
         *    is what lets the topic's own page offer it back.
         *
         * Every column is added with a non-null default, so an existing
         * library reads exactly as it did before the update.
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE personal_books ADD COLUMN chaptersJson TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE personal_notes ADD COLUMN kind TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE personal_notes ADD COLUMN topicId TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE personal_notes ADD COLUMN topicName TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE personal_notes ADD COLUMN categoryId TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getInstance(context: Context): CurioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CurioDatabase::class.java,
                    "curio_database"
                )
                    // TRUNCATE journal mode (not WAL): Android Auto Backup can
                    // restore a WAL-mode database in an inconsistent state because
                    // the -wal/-shm files aren't guaranteed to be backed up in sync
                    // with the main .db file. Curio's DB is a small single-table
                    // text store, so the write-throughput tradeoff is negligible —
                    // backup integrity wins.
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
