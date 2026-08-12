package com.curio.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CaptureEntity::class],
    version = 4,
    exportSchema = false
)
abstract class CurioDatabase : RoomDatabase() {

    abstract fun captureDao(): CaptureDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
