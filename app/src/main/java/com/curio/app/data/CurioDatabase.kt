package com.curio.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CaptureEntity::class, TopicEntity::class, CachedTopicEntity::class],
    version = 11,
    exportSchema = false
)
abstract class CurioDatabase : RoomDatabase() {

    abstract fun captureDao(): CaptureDao
    abstract fun topicDao(): TopicDao
    abstract fun cachedTopicDao(): CachedTopicDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
