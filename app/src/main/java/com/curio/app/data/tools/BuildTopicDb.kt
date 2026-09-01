package com.curio.app.data.tools

import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

/**
 * v294 — Standalone tool that creates a pre-populated topics.db
 * from JSON files. Runs via: java -cp <classpath> BuildTopicDb <json-dir> <output.db>
 *
 * Uses only Gson (no org.json) so it works as a standalone JVM process.
 */
object BuildTopicDb {

    fun build(assetsDir: File, outputFile: File) {
        val gson = Gson()
        Class.forName("org.sqlite.JDBC")
        val conn: Connection = DriverManager.getConnection("jdbc:sqlite:${outputFile.absolutePath}")

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
        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_cat ON topics(categoryId)")
        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_name ON topics(name)")

        val stmt = conn.prepareStatement("INSERT OR REPLACE INTO topics VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")
        var totalCount = 0

        val topicFiles = assetsDir.listFiles { file -> file.extension == "json" } ?: emptyArray()
        for (file in topicFiles) {
            if (file.name == "topic_index.json") continue
            val categoryId = file.nameWithoutExtension.uppercase()
            val jsonArray = JsonParser.parseString(file.readText()).asJsonArray

            for (element in jsonArray) {
                val obj = element.asJsonObject
                stmt.setString(1, obj.get("id")?.asString ?: continue)
                stmt.setString(2, obj.get("categoryId")?.asString?.takeIf { it.isNotBlank() } ?: categoryId)
                stmt.setString(3, obj.get("subtype")?.asString ?: "")
                stmt.setString(4, obj.get("name")?.asString ?: continue)
                stmt.setString(5, obj.get("teaser")?.asString ?: "")
                stmt.setString(6, obj.get("imageUrl")?.asString ?: "")
                stmt.setString(7, obj.get("byline")?.asString ?: "")

                val tags = obj.getAsJsonArray("tags")
                stmt.setString(8, if (tags != null) tags.toString() else "[]")
                stmt.setInt(9, obj.get("tier")?.asInt ?: 1)

                val ea = obj.getAsJsonObject("exploreAction")
                stmt.setString(10, ea?.get("verb")?.asString ?: "")
                stmt.setString(11, ea?.get("targetName")?.asString ?: "")
                stmt.setInt(12, ea?.get("durationMinutes")?.asInt ?: 30)
                stmt.setString(13, ea?.get("instruction")?.asString ?: "")

                val pc = obj.get("pageCount")
                if (pc != null && !pc.isJsonNull) stmt.setInt(14, pc.asInt) else stmt.setNull(14, java.sql.Types.INTEGER)
                val ec = obj.get("episodeCount")
                if (ec != null && !ec.isJsonNull) stmt.setInt(15, ec.asInt) else stmt.setNull(15, java.sql.Types.INTEGER)
                stmt.setString(16, obj.get("altPageLabel")?.asString ?: "")
                val apc = obj.get("altPageCount")
                if (apc != null && !apc.isJsonNull) stmt.setInt(17, apc.asInt) else stmt.setNull(17, java.sql.Types.INTEGER)
                stmt.addBatch()
                totalCount++
            }
            stmt.executeBatch()
            println("  ${file.name}: ${jsonArray.size()} topics")
        }
        conn.close()
        println("✓ topics.db: $totalCount topics from ${topicFiles.size} files")
        println("  Output: ${outputFile.absolutePath}")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            System.err.println("Usage: BuildTopicDb <json-dir> <output.db>")
            System.exit(1)
        }
        build(File(args[0]), File(args[1]))
    }
}
