package com.curio.app.data.tools

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

/**
 * v294 — Build-time tool that creates a pre-populated topics.db
 * from the JSON assets. Run via Gradle task:
 *
 *   ./gradlew buildTopicDatabase
 *
 * The output file goes to app/src/main/assets/topics.db and is
 * shipped with the APK so topics are available instantly without
 * JSON parsing at runtime.
 */
object BuildTopicDb {

    data class RawTopic(
        val id: String,
        val categoryId: String,
        val subtype: String,
        val name: String,
        val teaser: String,
        val imageUrl: String = "",
        val byline: String = "",
        val tags: List<String> = emptyList(),
        val tier: Int = 1,
        val exploreAction: ExploreActionRaw? = null,
        val pageCount: Int? = null,
        val episodeCount: Int? = null,
        val altPageCount: Int? = null,
        val altPageLabel: String = ""
    )

    data class ExploreActionRaw(
        val verb: String = "",
        val targetName: String = "",
        val durationMinutes: Int = 0,
        val instruction: String = ""
    )

    fun build(assetsDir: File, outputFile: File) {
        val gson = Gson()
        val topicType = object : TypeToken<List<RawTopic>>() {}.type

        // Create SQLite database
        Class.forName("org.sqlite.JDBC")
        val url = "jdbc:sqlite:${outputFile.absolutePath}"
        val conn: Connection = DriverManager.getConnection(url)

        // Create table
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS topics (
                id TEXT NOT NULL PRIMARY KEY,
                categoryId TEXT NOT NULL,
                subtype TEXT NOT NULL,
                name TEXT NOT NULL,
                teaser TEXT NOT NULL,
                imageUrl TEXT NOT NULL DEFAULT '',
                byline TEXT NOT NULL DEFAULT '',
                tags TEXT NOT NULL DEFAULT '[]',
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
        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS index_topics_categoryId ON topics(categoryId)")
        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS index_topics_name ON topics(name)")

        val insertSql = """
            INSERT OR REPLACE INTO topics 
            (id, categoryId, subtype, name, teaser, imageUrl, byline, tags, tier,
             exploreVerb, exploreTargetName, exploreDurationMinutes, exploreInstruction,
             pageCount, episodeCount, altPageLabel, altPageCount)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        val stmt: PreparedStatement = conn.prepareStatement(insertSql)

        var totalCount = 0

        // Process each JSON file in assets/topics/
        val topicFiles = assetsDir.listFiles { file -> file.extension == "json" } ?: emptyArray()
        for (file in topicFiles) {
            if (file.name == "topic_index.json") continue // skip index file

            val categoryId = file.nameWithoutExtension.uppercase()
            val jsonText = file.readText()
            val topics: List<RawTopic> = try {
                gson.fromJson(jsonText, topicType) ?: emptyList()
            } catch (_: Exception) {
                println("  Skipping ${file.name} — parse error")
                continue
            }

            for (topic in topics) {
                val explore = topic.exploreAction
                stmt.setString(1, topic.id)
                stmt.setString(2, if (topic.categoryId.isBlank()) categoryId else topic.categoryId)
                stmt.setString(3, topic.subtype)
                stmt.setString(4, topic.name)
                stmt.setString(5, topic.teaser)
                stmt.setString(6, topic.imageUrl)
                stmt.setString(7, topic.byline)
                stmt.setString(8, gson.toJson(topic.tags))
                stmt.setInt(9, topic.tier)
                stmt.setString(10, explore?.verb ?: "")
                stmt.setString(11, explore?.targetName ?: "")
                stmt.setInt(12, explore?.durationMinutes ?: 0)
                stmt.setString(13, explore?.instruction ?: "")
                if (topic.pageCount != null) stmt.setInt(14, topic.pageCount) else stmt.setNull(14, java.sql.Types.INTEGER)
                if (topic.episodeCount != null) stmt.setInt(15, topic.episodeCount) else stmt.setNull(15, java.sql.Types.INTEGER)
                stmt.setString(16, topic.altPageLabel)
                if (topic.altPageCount != null) stmt.setInt(17, topic.altPageCount) else stmt.setNull(17, java.sql.Types.INTEGER)
                stmt.addBatch()
                totalCount++
            }

            stmt.executeBatch()
            println("  ${file.name}: ${topics.size} topics")
        }

        conn.close()
        println("Built topics.db: $totalCount topics from ${topicFiles.size} files")
        println("Output: ${outputFile.absolutePath}")
    }
}
