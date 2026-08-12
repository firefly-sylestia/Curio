package com.curio.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room entity for saved captures.
 *
 * Stores topic metadata + format + timestamp as columns, and the
 * format-specific data as a JSON blob. This single-table design
 * lets us add new formats without migrations.
 */
@Entity(tableName = "captures")
@TypeConverters(CaptureConverters::class)
data class CaptureEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val categoryId: String,
    val topicName: String,
    val topicSubtype: String,
    val topicTeaser: String,
    val format: String,          // CaptureFormat enum name
    val capturedAtMillis: Long,
    val title: String?,
    val formatDataJson: String,    // JSON-serialized CaptureData
    // v7.17 — user tags, stored as a Gson JSON array string ("[\"a\",\"b\"]").
    // Room migration v1→v2 adds this column with DEFAULT '[]' so existing
    // rows read as empty; backup restore normalizes nulls defensively.
    val tagsJson: String = "[]",
    // v3 — explicit FieldMind restore provenance. Native Curio saves remain false.
    val isLegacy: Boolean = false,
    // v4 — explore-session duration at save time (ms). 0 = no session
    // recorded. Room migration v3→v4 adds this column with DEFAULT 0 so
    // existing rows read as no-session; backup restore normalizes defensively.
    val sessionTimeMillis: Long = 0L,
    // v5 — recycle bin: nullable soft-delete timestamp (NULL = live). Room
    // migration v4→v5 adds the column (nullable, no default) so existing rows
    // read as live; the main list queries filter `deletedAt IS NULL`.
    val deletedAt: Long? = null
)

/**
 * Type converters for Room — handles enum-to-string and Gson serialization.
 */
class CaptureConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromCaptureFormat(format: CaptureFormat): String = format.name

    @TypeConverter
    fun toCaptureFormat(value: String): CaptureFormat = CaptureFormat.valueOf(value)

    @TypeConverter
    fun fromCaptureData(data: CaptureData): String = gson.toJson(data)

    @TypeConverter
    fun toCaptureData(json: String): CaptureData = deserializeCaptureData(json)

    companion object {
        /**
         * Shared deserializer for [CaptureData] from JSON.
         *
         * Uses field-based detection to determine the concrete subclass,
         * then delegates to Gson for the final deserialization.
         * OpenNotebook's nested [CaptureData.subData] is handled by
         * recursively calling this function on the sub-object.
         */
        fun deserializeCaptureData(json: String): CaptureData {
            val gson = Gson()
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson(json, Map::class.java) as Map<String, Any?>
            return when (map["subFormat"]) {
                null -> {
                    when {
                        // Multi-section Portfolio: each section carries its own
                        // format + nested data — reconstruct recursively.
                        map.containsKey("sections") -> {
                            @Suppress("UNCHECKED_CAST")
                            val rawSections = map["sections"] as List<Map<String, Any?>>
                            val sections = rawSections.mapNotNull { raw ->
                                runCatching {
                                    val format = CaptureFormat.valueOf(raw["format"] as String)
                                    val title = raw["title"] as? String
                                    val dataJson = gson.toJson(raw["data"])
                                    CaptureData.CaptureSection(
                                        format = format,
                                        data = deserializeCaptureData(dataJson),
                                        title = title
                                    )
                                }.getOrNull()
                            }
                            CaptureData.Portfolio(sections)
                        }
                        map.containsKey("durationSeconds") ->
                            gson.fromJson(json, CaptureData.SoundBite::class.java)
                        map.containsKey("rating") ->
                            gson.fromJson(json, CaptureData.ReelNotes::class.java)
                        // GalleryWall BEFORE "quotes": the mood board also
                        // carries a quotes list, so a GalleryWall blob (which
                        // serializes `quotes:[]`) must not fall through to the
                        // Marginalia branch — that misdetection decoded newly
                        // saved mood boards as Marginalia, and the detail page
                        // silently rendered a blank body (2026-08-03).
                        map.containsKey("caption") ->
                            gson.fromJson(json, CaptureData.GalleryWall::class.java)
                        map.containsKey("quotes") ->
                            gson.fromJson(json, CaptureData.Marginalia::class.java)
                        map.containsKey("learnNext") ->
                            gson.fromJson(json, CaptureData.FieldNotes::class.java)
                        else -> throw IllegalArgumentException("Unknown CaptureData type: $json")
                    }
                }
                else -> {
                    // OpenNotebook: manually reconstruct to handle nested CaptureData
                    val subFormat = CaptureFormat.valueOf(map["subFormat"] as String)
                    @Suppress("UNCHECKED_CAST")
                    val subDataMap = map["subData"] as Map<String, Any?>
                    val subDataJson = gson.toJson(subDataMap)
                    val subData = deserializeCaptureData(subDataJson)
                    CaptureData.OpenNotebook(subFormat, subData)
                }
            }
        }
    }
}

/**
 * Convert [CurioEntry] to [CaptureEntity] for Room storage.
 */
fun CurioEntry.toEntity(): CaptureEntity = CaptureEntity(
    id = id,
    topicId = topic.id,
    categoryId = topic.categoryId.name,
    topicName = topic.name,
    topicSubtype = topic.subtype,
    topicTeaser = topic.teaser,
    format = format.name,
    capturedAtMillis = capturedAtMillis,
    title = title,
    formatDataJson = Gson().toJson(captureData),
    tagsJson = Gson().toJson(tags),
    isLegacy = isLegacy,
    sessionTimeMillis = sessionTimeMillis
)

/**
 * Reconstruct [CurioEntry] from a [CaptureEntity] stored in Room.
 * 
 * Fixed to properly handle movie entries and other formats by ensuring
 * the topic is fully reconstructed with all available data, including
 * fallback exploreAction and tags when the cached topic isn't found.
 */
fun CaptureEntity.toEntry(): CurioEntry {
    val categoryId = CategoryId.valueOf(this.categoryId)
    
    // Try to find the full topic data from cache
    val cachedTopic = TopicJsonLoader.cached(categoryId)?.find { 
        it.id == topicId || it.name == topicName 
    }
    
    // Use cached topic if available, otherwise create a complete fallback
    val topic = cachedTopic ?: CurioTopic(
        id = topicId,
        categoryId = categoryId,
        subtype = topicSubtype,
        name = topicName,
        teaser = topicTeaser,
        imageUrl = "",
        exploreAction = ExploreAction(
            verb = when (categoryId) {
                CategoryId.FILMS, CategoryId.DIRECTORS, CategoryId.SERIES,
                CategoryId.ANIME, CategoryId.SPORTS, CategoryId.INTERNET -> "Watch"
                CategoryId.ALBUMS, CategoryId.ARTISTS, CategoryId.SONGS -> "Listen"
                CategoryId.BOOKS, CategoryId.AUTHORS, CategoryId.MANGA,
                CategoryId.MANHWA, CategoryId.MYTHOLOGY -> "Read"
                CategoryId.PAINTERS, CategoryId.ARTWORKS -> "View"
                CategoryId.SCIENTISTS, CategoryId.DISCOVERIES -> "Explore"
                CategoryId.GAMES -> "Watch"
                CategoryId.FOOD -> "Read"
                CategoryId.WILDCARD -> "Discover"
            },
            targetName = topicName,
            durationMinutes = 30,
            instruction = "Revisit this saved topic: $topicName"
        ),
        tags = emptyList(),
        tier = 1
    )
    
    val captureData = try {
        CaptureConverters.deserializeCaptureData(formatDataJson)
    } catch (e: Exception) {
        // Fallback for malformed data - create empty ReelNotes for movies
        when (CaptureFormat.valueOf(format)) {
            CaptureFormat.ReelNotes -> CaptureData.ReelNotes(0, "", 0)
            CaptureFormat.SoundBite -> CaptureData.SoundBite(0, "")
            CaptureFormat.Marginalia -> CaptureData.Marginalia("", emptyList())
            CaptureFormat.GalleryWall -> CaptureData.GalleryWall(0, "")
            CaptureFormat.FieldNotes -> CaptureData.FieldNotes("", "", "")
            CaptureFormat.OpenNotebook -> CaptureData.OpenNotebook(
                CaptureFormat.ReelNotes,
                CaptureData.ReelNotes(0, "", 0)
            )
        }
    }
    
    return CurioEntry(
        id = id,
        topic = topic,
        format = CaptureFormat.valueOf(format),
        captureData = captureData,
        title = title,
        capturedAtMillis = capturedAtMillis,
        tags = deserializeTags(tagsJson),
        isLegacy = this.isLegacy,
        sessionTimeMillis = this.sessionTimeMillis,
        deletedAt = this.deletedAt
    )
}

/**
 * Parse the stored [CaptureEntity.tagsJson] array defensively — legacy rows,
 * null blobs (Gson decodes a missing field to null), or malformed JSON all
 * degrade to an empty list instead of crashing the Cabinet grid.
 */
private fun deserializeTags(tagsJson: String?): List<String> {
    if (tagsJson.isNullOrBlank()) return emptyList()
    return try {
        val type = object : TypeToken<List<String>>() {}.type
        (Gson().fromJson<List<String>>(tagsJson, type) ?: emptyList())
            .filter { it.isNotBlank() }
            .distinct()
    } catch (_: Exception) {
        emptyList()
    }
}
