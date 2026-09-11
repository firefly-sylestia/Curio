package com.curio.app.data.supabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime

/**
 * ONE live community card, as it comes back from PostgREST. Everything here
 * is enough to rebuild the card with the app's own share-card renderer —
 * there is no image, audio or screenshot field anywhere in the schema, and
 * there is no way to add one.
 */
data class CommunityCard(
    val id: String,
    val authorHandle: String,
    val topicName: String,
    val categoryName: String,
    val categoryGlyph: String,
    val accentHex: String,
    val factText: String,
    val style: String,
    val aspect: String,
    val bodyScale: Float,
    val byline: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val likeCount: Int,
    val likedByMe: Boolean,
    /** True when this device's account posted the card. */
    val mine: Boolean
) {
    /** Hours left before the card disappears, floored at 0. */
    val hoursLeft: Long
        get() = expiresAtMillis.takeIf { it > 0 }
            ?.let { ((it - System.currentTimeMillis()).coerceAtLeast(0L)) / 3_600_000L }
            ?: 0L
}

/**
 * What a user may post: TEXT + TOPIC + STYLE data only.
 *
 * The share-card renderer needs exactly these fields
 * ([topicName]/[categoryName]/[categoryGlyph]/[accentHex]/[factText] +
 * style/aspect/scale), which is why a card can be rebuilt on another device
 * without ever uploading an image. Anything media-backed stays local.
 */
data class CommunityCardDraft(
    val topicName: String,
    val categoryName: String,
    val categorySlug: String,
    val categoryGlyph: String,
    val accentHex: String,
    val factText: String,
    val style: String = "PAPER",
    val aspect: String = "CLASSIC",
    val bodyScale: Float = 1f,
    val byline: String = ""
)

/** A community failure whose [message] is already safe to show the user. */
class CommunityError(message: String) : Exception(message)

/**
 * The community feed's REST layer — the only place the app talks to the
 * `community_cards` / `community_reactions` / `community_reports` tables.
 *
 * Every call runs with the signed-in access token, and every rule the UI
 * enforces (sign in, Online Mode on, text only, 24-hour life) is ALSO
 * enforced by row-level security in `supabase/schema.sql`. This layer never
 * assumes the client is the guard.
 */
object CommunityApi {

    private const val CARDS = "/rest/v1/community_cards"
    private const val REACTIONS = "/rest/v1/community_reactions"
    private const val REPORTS = "/rest/v1/community_reports"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** The server's own ceiling for a card's text (also a DB check constraint). */
    const val MAX_FACT_CHARS = 600

    /** How many live cards one feed load asks for. */
    private const val FEED_LIMIT = 40

    /**
     * Why this draft cannot be posted, or null when it is fine. Media-backed
     * content never reaches here at all: [CommunityCardDraft] has no field to
     * carry it, which is the whole point of the draft shape.
     */
    fun draftProblem(draft: CommunityCardDraft): String? = when {
        draft.topicName.isBlank() -> "What is your card about?"
        draft.factText.isBlank() -> "Add the words you want on the card."
        draft.factText.length > MAX_FACT_CHARS ->
            "Keep it under $MAX_FACT_CHARS characters (it's ${draft.factText.length})."
        else -> null
    }

    /** The live feed — newest first, expired cards already filtered out. */
    suspend fun feed(accessToken: String, myUserId: String?): Result<List<CommunityCard>> =
        withContext(Dispatchers.IO) {
            mapped {
                val select = listOf(
                    "id", "owner", "author_handle", "topic_name", "category_name",
                    "category_glyph", "accent_hex", "fact_text", "style", "aspect",
                    "body_scale", "byline", "created_at", "expires_at",
                    "community_reactions(user_id)"
                ).joinToString(",")
                val path = "$CARDS?select=$select" +
                    "&expires_at=gt.${Instant.now()}" +
                    "&order=created_at.desc&limit=$FEED_LIMIT"
                val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                parseCards(SupabaseClient.executeBody(request), myUserId)
            }
        }

    /** Posts a card. The owner column and the 24-hour expiry are set by the DB. */
    suspend fun post(
        accessToken: String,
        draft: CommunityCardDraft,
        handle: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            draftProblem(draft)?.let { throw IllegalArgumentException(it) }
            val payload = JSONObject()
                .put("author_handle", handle.trim().ifBlank { "A curious soul" })
                .put("topic_name", draft.topicName.trim())
                .put("category_slug", draft.categorySlug)
                .put("category_name", draft.categoryName)
                .put("category_glyph", draft.categoryGlyph)
                .put("accent_hex", draft.accentHex)
                .put("fact_text", draft.factText.trim())
                .put("style", draft.style)
                .put("aspect", draft.aspect)
                .put("body_scale", draft.bodyScale.toDouble())
                .put("byline", draft.byline.trim())
            val request = SupabaseClient.requestBuilder(CARDS, accessToken)
                .header("Prefer", "return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** Idempotent like — reacting twice leaves one reaction. */
    suspend fun like(accessToken: String, cardId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val payload = JSONObject().put("card_id", cardId)
                val request = SupabaseClient.requestBuilder(REACTIONS, accessToken)
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    suspend fun unlike(accessToken: String, cardId: String, myUserId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val request = SupabaseClient
                    .requestBuilder("$REACTIONS?card_id=eq.$cardId&user_id=eq.$myUserId", accessToken)
                    .delete()
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    /** Files a report. One per card per user (the DB's unique constraint). */
    suspend fun report(accessToken: String, cardId: String, reason: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val payload = JSONObject().put("card_id", cardId).put("reason", reason)
                val request = SupabaseClient.requestBuilder(REPORTS, accessToken)
                    .header("Prefer", "return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    /** Pulls your own card early (the author is the only one who can). */
    suspend fun delete(accessToken: String, cardId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val request = SupabaseClient
                    .requestBuilder("$CARDS?id=eq.$cardId", accessToken)
                    .delete()
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    // ── internals ────────────────────────────────────────────────────────

    private fun <T> mapped(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (failure: Throwable) {
        Result.failure(CommunityError(communityMessage(failure)))
    }

    /**
     * The writes answer with a response body nobody wants, so they go through
     * this overload: the block returns Unit and the body is discarded (a plain
     * `mapped { executeBody(…) }` would infer `Result<String>` and fail to match
     * the call's declared `Result<Unit>`).
     */
    private fun mappedUnit(block: () -> Unit): Result<Unit> = mapped(block)

    private fun parseCards(body: String, myUserId: String?): List<CommunityCard> {
        val array = JSONArray(body)
        val cards = ArrayList<CommunityCard>(array.length())
        for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            val owner = row.optString("owner")
            val reactions = row.optJSONArray("community_reactions")
            var likes = 0
            var likedByMe = false
            if (reactions != null) {
                for (r in 0 until reactions.length()) {
                    val reaction = reactions.optJSONObject(r) ?: continue
                    likes++
                    if (myUserId != null && reaction.optString("user_id") == myUserId) likedByMe = true
                }
            }
            cards += CommunityCard(
                id = row.optString("id"),
                authorHandle = row.optString("author_handle").ifBlank { "A curious soul" },
                topicName = row.optString("topic_name"),
                categoryName = row.optString("category_name"),
                categoryGlyph = row.optString("category_glyph"),
                accentHex = row.optString("accent_hex"),
                factText = row.optString("fact_text"),
                style = row.optString("style").ifBlank { "PAPER" },
                aspect = row.optString("aspect").ifBlank { "CLASSIC" },
                bodyScale = row.optDouble("body_scale", 1.0).toFloat(),
                byline = row.optString("byline"),
                createdAtMillis = epochMillis(row.optString("created_at")),
                expiresAtMillis = epochMillis(row.optString("expires_at")),
                likeCount = likes,
                likedByMe = likedByMe,
                mine = myUserId != null && owner == myUserId
            )
        }
        return cards
    }

    /** PostgREST answers with an offset timestamp — 0 when it cannot be read. */
    private fun epochMillis(iso: String): Long =
        runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrDefault(0L)
}

/**
 * Maps a community failure to copy that is safe to render: draft problems are
 * already written for the user, rate limits and duplicate actions stay
 * explicit, an RLS refusal is explained as the Online Mode gate it is, and
 * anything unrecognised collapses to one generic line so a raw response body
 * can never reach the UI.
 */
internal fun communityMessage(failure: Throwable): String {
    val raw = failure.message.orEmpty()
    return when {
        failure is IllegalArgumentException && raw.isNotBlank() -> raw
        raw.contains("rate limit", true) || raw.contains("too many", true) ->
            "Too much just now — try again in a minute."
        raw.contains("duplicate key", true) -> "You've already done that."
        raw.contains("row-level security", true) ->
            "Turn Online mode on (Settings → Online mode) and try again."
        raw.contains("unable to resolve host", true) ||
            raw.contains("failed to connect", true) ||
            raw.contains("timeout", true) ->
            "No connection — check your network and try again."
        else -> "Something went wrong. Try again in a moment."
    }
}
