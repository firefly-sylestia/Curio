package com.curio.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * v294 — Separate topic cache table. Stores the full [CurioTopic] data at
 * save-time so entries survive even if the topic is later deleted from the
 * bundled JSON assets or the Room `topics` table. This enables future
 * custom topic entries where user-created topics live only in this cache.
 *
 * Keyed by topic [id] — INSERT OR REPLACE ensures idempotent saves.
 * The `topics` table is the canonical catalog (populated from JSON);
 * `cached_topics` is the durable entry-linked backup.
 */
@Entity(tableName = "cached_topics")
data class CachedTopicEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val subtype: String,
    val name: String,
    val teaser: String,
    @ColumnInfo(defaultValue = "")
    val imageUrl: String = "",
    @ColumnInfo(defaultValue = "")
    val byline: String = "",
    @ColumnInfo(defaultValue = "")
    val tags: String = "", // JSON array stored as string
    @ColumnInfo(defaultValue = "1")
    val tier: Int = 1,
    @ColumnInfo(defaultValue = "")
    val exploreVerb: String = "",
    @ColumnInfo(defaultValue = "")
    val exploreTargetName: String = "",
    @ColumnInfo(defaultValue = "0")
    val exploreDurationMinutes: Int = 0,
    @ColumnInfo(defaultValue = "")
    val exploreInstruction: String = "",
    @ColumnInfo(defaultValue = "0")
    val pageCount: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val episodeCount: Int? = null,
    @ColumnInfo(defaultValue = "")
    val altPageLabel: String = "",
    @ColumnInfo(defaultValue = "0")
    val altPageCount: Int? = null
) {
    /** Convert to [CurioTopic]. */
    fun toCurioTopic(): CurioTopic {
        val catId = try { CategoryId.valueOf(categoryId) } catch (_: Exception) { CategoryId.WILDCARD }
        return CurioTopic(
            id = id,
            categoryId = catId,
            subtype = subtype,
            name = name,
            teaser = teaser,
            imageUrl = imageUrl,
            exploreAction = ExploreAction(
                verb = exploreVerb,
                targetName = exploreTargetName,
                durationMinutes = exploreDurationMinutes,
                instruction = exploreInstruction
            ),
            tags = try {
                if (tags.isBlank()) emptyList()
                else Gson().fromJson(tags, object : TypeToken<List<String>>() {}.type)
            } catch (_: Exception) { emptyList() },
            tier = tier,
            byline = byline,
            pageCount = pageCount,
            episodeCount = episodeCount,
            altPageCount = altPageCount,
            altPageLabel = altPageLabel
        )
    }

    companion object {
        /** Convert a [CurioTopic] to a [CachedTopicEntity]. */
        fun fromCurioTopic(topic: CurioTopic): CachedTopicEntity {
            return CachedTopicEntity(
                id = topic.id,
                categoryId = topic.categoryId.name,
                subtype = topic.subtype,
                name = topic.name,
                teaser = topic.teaser,
                imageUrl = topic.imageUrl,
                byline = topic.byline,
                tags = try { Gson().toJson(topic.tags) } catch (_: Exception) { "[]" },
                tier = topic.tier,
                exploreVerb = topic.exploreAction.verb,
                exploreTargetName = topic.exploreAction.targetName,
                exploreDurationMinutes = topic.exploreAction.durationMinutes,
                exploreInstruction = topic.exploreAction.instruction,
                pageCount = topic.pageCount,
                episodeCount = topic.episodeCount,
                altPageLabel = topic.altPageLabel,
                altPageCount = topic.altPageCount
            )
        }
    }
}
