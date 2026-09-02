package com.curio.app.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File

/** Read-only access to the bundled catalog without parsing or materializing it at startup. */
object TopicAssetStore {
    private const val DATABASE_NAME = "topics.db"
    @Volatile private var database: SQLiteDatabase? = null

    @Synchronized
    fun open(context: Context): SQLiteDatabase {
        database?.takeIf { it.isOpen }?.let { return it }
        val file = File(context.applicationContext.noBackupFilesDir, DATABASE_NAME)
        if (!file.exists()) {
            context.assets.open(DATABASE_NAME).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            .also { database = it }
    }

    fun count(context: Context): Int = open(context).rawQuery("SELECT COUNT(*) FROM topics", null).use {
        it.moveToFirst(); it.getInt(0)
    }

    fun byCategory(context: Context, categoryId: String, limit: Int, offset: Int): List<TopicEntity> =
        query(context, "categoryId = ?", arrayOf(categoryId), limit, offset)

    fun all(context: Context, limit: Int, offset: Int): List<TopicEntity> =
        query(context, null, null, limit, offset)

    fun search(context: Context, query: String, categoryId: String? = null, limit: Int = 50): List<TopicEntity> {
        val like = "%${query.trim()}%"
        val where = if (categoryId == null) {
            "(name LIKE ? OR byline LIKE ? OR teaser LIKE ? OR tags LIKE ?)"
        } else {
            "categoryId = ? AND (name LIKE ? OR byline LIKE ? OR teaser LIKE ? OR tags LIKE ?)"
        }
        val args = if (categoryId == null) arrayOf(like, like, like, like)
        else arrayOf(categoryId, like, like, like, like)
        return query(context, where, args, limit, 0)
    }

    fun findById(context: Context, id: String): TopicEntity? =
        query(context, "id = ?", arrayOf(id), 1, 0).firstOrNull()

    private fun query(context: Context, where: String?, args: Array<String>?, limit: Int, offset: Int): List<TopicEntity> {
        val sql = buildString {
            append("SELECT id, categoryId, subtype, name, teaser, imageUrl, byline, tags, tier, exploreVerb, exploreTargetName, exploreDurationMinutes, exploreInstruction, pageCount, episodeCount, altPageLabel, altPageCount, synopsis, chapters FROM topics")
            if (where != null) append(" WHERE ").append(where)
            append(" ORDER BY name COLLATE NOCASE ASC LIMIT ").append(limit).append(" OFFSET ").append(offset)
        }
        return open(context).rawQuery(sql, args).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toTopicEntity()) }
        }
    }

    private fun Cursor.toTopicEntity() = TopicEntity(
        id = getString(0), categoryId = getString(1), subtype = getString(2), name = getString(3),
        teaser = getString(4), imageUrl = getString(5), byline = getString(6), tags = getString(7),
        tier = getInt(8), exploreVerb = getString(9), exploreTargetName = getString(10),
        exploreDurationMinutes = getInt(11), exploreInstruction = getString(12),
        pageCount = if (isNull(13)) null else getInt(13), episodeCount = if (isNull(14)) null else getInt(14),
        altPageLabel = getString(15), altPageCount = if (isNull(16)) null else getInt(16),
        synopsis = getString(17), chapters = getString(18)
    )
}
