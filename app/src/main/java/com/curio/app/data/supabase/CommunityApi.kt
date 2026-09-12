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
    /** The author's account id (`owner`) — the door to their profile. */
    val authorId: String,
    /** The handle SNAPSHOT taken when the card was posted. Only a fallback:
     *  the live username is [authorName], so renaming yourself renames your
     *  old cards too. */
    val authorHandle: String,
    /** The author's CURRENT username, resolved from `profiles` on every load
     *  (blank when their profile is not readable — they turned Online Mode
     *  off, or the lookup failed). */
    val authorName: String = "",
    /** The author's chosen portrait (0–15). */
    val authorAvatar: Int = 0,
    /** CARD (a topic share card), NOTE (a text-only post) or QUOTE. */
    val kind: String = "CARD",
    val topicName: String,
    val categoryName: String,
    val categoryGlyph: String,
    val accentHex: String,
    val factText: String,
    /** The poster's own line above the card (blank when they wrote none). */
    val caption: String,
    val style: String,
    val aspect: String,
    val bodyScale: Float,
    val byline: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val likeCount: Int,
    val likedByMe: Boolean,
    /** How many replies hang under the card. */
    val commentCount: Int,
    /** True when this device's account posted the card. */
    val mine: Boolean
) {
    /** Hours left before the card disappears, floored at 0. */
    val hoursLeft: Long
        get() = expiresAtMillis.takeIf { it > 0 }
            ?.let { ((it - System.currentTimeMillis()).coerceAtLeast(0L)) / 3_600_000L }
            ?: 0L

    /**
     * What to PRINT for the author: the live username first, the post-time
     * snapshot second. Never a product label — a member's card must always
     * carry a real human identity.
     */
    val authorLabel: String
        get() = authorName.trim().removePrefix("@").ifBlank {
            authorHandle.trim().removePrefix("@").ifBlank { "a curious soul" }
        }
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
    /** The poster's own line above the card — optional, never media. */
    val caption: String = "",
    /** CARD, NOTE or QUOTE — the renderer a card is rebuilt with. */
    val kind: String = KIND_CARD,
    val style: String = "PAPER",
    val aspect: String = "CLASSIC",
    val bodyScale: Float = 1f,
    val byline: String = ""
) {
    /** True for the text-only posts that carry no topic and no card art. */
    val isTextOnly: Boolean get() = kind == KIND_NOTE || kind == KIND_QUOTE
}

/** A topic share card. */
const val KIND_CARD = "CARD"

/** A tweet-style text post — words and nothing else. */
const val KIND_NOTE = "NOTE"

/** A line someone else said, credited to them. */
const val KIND_QUOTE = "QUOTE"

/** One reply under a card. Text only, and it dies with the card. */
data class CommunityComment(
    val id: String,
    val authorId: String,
    val authorHandle: String,
    /** The author's CURRENT username (blank when unreadable) — see
     *  [CommunityCard.authorName]. */
    val authorName: String = "",
    /** The author's chosen portrait (0–15). */
    val authorAvatar: Int = 0,
    val body: String,
    /** The reply this one answers — null at the top level. */
    val parentId: String? = null,
    val createdAtMillis: Long,
    val mine: Boolean
) {
    /** Live username first, post-time snapshot second. */
    val authorLabel: String
        get() = authorName.trim().removePrefix("@").ifBlank {
            authorHandle.trim().removePrefix("@").ifBlank { "a curious soul" }
        }
}

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
    private const val COMMENTS = "/rest/v1/community_comments"
    private const val REPORTS = "/rest/v1/community_reports"

    /**
     * The columns one card needs, including the two embedded children the
     * parser counts (likes and replies). Kept in one place so the feed and the
     * single-card fetch can never drift apart.
     */
    private const val CARD_COLUMNS =
        "id,owner,author_handle,kind,topic_name,category_name,category_glyph,accent_hex," +
            "fact_text,caption,style,aspect,body_scale,byline,created_at,expires_at," +
            "community_reactions(user_id),community_comments(id)"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** The server's own ceiling for a card's text (also a DB check constraint). */
    const val MAX_FACT_CHARS = 600

    /** Ceiling for the poster's caption (mirrors the DB check constraint). */
    const val MAX_CAPTION_CHARS = 180

    /** Ceiling for one reply (mirrors the DB check constraint). */
    const val MAX_COMMENT_CHARS = 400

    /** How many live cards one feed load asks for. */
    private const val FEED_LIMIT = 40

    /** How many live cards one profile asks for. */
    private const val PROFILE_LIMIT = 24

    /**
     * Why this draft cannot be posted, or null when it is fine. Media-backed
     * content never reaches here at all: [CommunityCardDraft] has no field to
     * carry it, which is the whole point of the draft shape.
     */
    fun draftProblem(draft: CommunityCardDraft): String? = when {
        // A text-only post has no topic BY DESIGN (that is the point of it),
        // so the topic requirement applies to topic cards alone.
        draft.kind == KIND_CARD && draft.topicName.isBlank() -> "What is your card about?"
        draft.factText.isBlank() -> when (draft.kind) {
            KIND_QUOTE -> "Write the quote first."
            KIND_NOTE -> "Write something first."
            else -> "Add the words you want on the card."
        }
        draft.factText.length > MAX_FACT_CHARS ->
            "Keep the card under $MAX_FACT_CHARS characters (it's ${draft.factText.length})."
        draft.caption.length > MAX_CAPTION_CHARS ->
            "Keep your caption under $MAX_CAPTION_CHARS characters (it's ${draft.caption.length})."
        else -> null
    }

    /** The live feed — newest first, expired cards already filtered out. */
    suspend fun feed(accessToken: String, myUserId: String?): Result<List<CommunityCard>> =
        withContext(Dispatchers.IO) {
            val parsed = mapped {
                val path = "$CARDS?select=$CARD_COLUMNS" +
                    "&expires_at=gt.${Instant.now()}" +
                    "&order=created_at.desc&limit=$FEED_LIMIT"
                val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                parseCards(SupabaseClient.executeBody(request), myUserId)
            }
            // Identity is resolved after the parse (one extra request for
            // every author on screen) — see [withAuthors]. The parsed result
            // is unwrapped by hand so the suspend resolution call is
            // unambiguous at the call site.
            val cards = parsed.getOrNull() ?: return@withContext parsed.asFailure()
            Result.success(withAuthors(accessToken, cards))
        }

    /**
     * Every live card by ONE author — what their profile shows. Reads the same
     * live window as the feed (a profile can never show an expired card) and
     * asks for the newest page of them.
     */
    suspend fun cardsByAuthor(
        accessToken: String,
        authorId: String,
        myUserId: String?
    ): Result<List<CommunityCard>> = withContext(Dispatchers.IO) {
        val parsed = mapped {
            val path = "$CARDS?select=$CARD_COLUMNS" +
                "&owner=eq.$authorId" +
                "&expires_at=gt.${Instant.now()}" +
                "&order=created_at.desc&limit=$PROFILE_LIMIT"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            parseCards(SupabaseClient.executeBody(request), myUserId)
        }
        val cards = parsed.getOrNull() ?: return@withContext parsed.asFailure()
        Result.success(withAuthors(accessToken, cards))
    }

    /**
     * One card by id — what the card's own view opens with. A card that has
     * expired (or an RLS refusal) answers with an empty list, so the caller
     * gets the same "it's gone" message either way instead of a raw 404.
     */
    suspend fun card(accessToken: String, cardId: String, myUserId: String?): Result<CommunityCard> =
        withContext(Dispatchers.IO) {
            val parsed = mapped {
                val path = "$CARDS?select=$CARD_COLUMNS&id=eq.$cardId&limit=1"
                val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                parseCards(SupabaseClient.executeBody(request), myUserId).firstOrNull()
                    ?: throw IllegalStateException("That card has expired — cards only last 24 hours.")
            }
            val one = parsed.getOrNull() ?: return@withContext parsed.asFailure()
            Result.success(withAuthors(accessToken, listOf(one)).first())
        }

    // ── replies ──────────────────────────────────────────────────────────

    /** The replies under a card, oldest first (a conversation reads downwards). */
    suspend fun comments(
        accessToken: String,
        cardId: String,
        myUserId: String?
    ): Result<List<CommunityComment>> = withContext(Dispatchers.IO) {
        val parsed = mapped {
            val path = "$COMMENTS?select=id,author,author_handle,body,parent_id,created_at" +
                "&card_id=eq.$cardId&order=created_at.asc&limit=200"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            val array = JSONArray(SupabaseClient.executeBody(request))
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val row = array.optJSONObject(index) ?: continue
                    add(
                        CommunityComment(
                            id = row.optString("id"),
                            authorId = row.optString("author"),
                            authorHandle = row.optString("author_handle")
                                .ifBlank { "A curious soul" },
                            body = row.optString("body"),
                            parentId = row.optString("parent_id")
                                .takeIf { it.isNotBlank() && it != "null" },
                            createdAtMillis = epochMillis(row.optString("created_at")),
                            mine = myUserId != null && row.optString("author") == myUserId
                        )
                    )
                }
            }
        }
        // A reply shows the author's CURRENT name and portrait too, so a
        // rename never leaves an old handle stranded in a thread.
        val replies = parsed.getOrNull() ?: return@withContext parsed.asFailure()
        Result.success(withCommentAuthors(accessToken, replies))
    }

    suspend fun comment(
        accessToken: String,
        cardId: String,
        body: String,
        handle: String,
        /** The reply this answers, for a branched thread — null at the top. */
        parentId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val text = body.trim()
            if (text.isEmpty()) throw IllegalArgumentException("Write something first.")
            if (text.length > MAX_COMMENT_CHARS) {
                throw IllegalArgumentException(
                    "Keep it under $MAX_COMMENT_CHARS characters (it's ${text.length})."
                )
            }
            val payload = JSONObject()
                .put("card_id", cardId)
                .put("body", text)
                .put("author_handle", handle.trim().ifBlank { "A curious soul" })
            // Absent rather than null: PostgREST treats a JSON null as an
            // explicit NULL only if the column is nullable, and omitting it
            // keeps a top-level reply from touching the branch column at all.
            parentId?.takeIf { it.isNotBlank() }?.let { payload.put("parent_id", it) }
            val request = SupabaseClient.requestBuilder(COMMENTS, accessToken)
                .header("Prefer", "return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** Only a comment's own author may remove it. */
    suspend fun deleteComment(accessToken: String, commentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val request = SupabaseClient
                    .requestBuilder("$COMMENTS?id=eq.$commentId", accessToken)
                    .delete()
                    .build()
                SupabaseClient.executeBody(request)
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
                .put("kind", draft.kind)
                .put("topic_name", draft.topicName.trim())
                .put("category_slug", draft.categorySlug)
                .put("category_name", draft.categoryName)
                .put("category_glyph", draft.categoryGlyph)
                .put("accent_hex", draft.accentHex)
                .put("fact_text", draft.factText.trim())
                .put("caption", draft.caption.trim())
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

    /**
     * A failed result re-cast to the requested type. Needed because a failed
     * parse has to be returned from a suspend block that never produced its
     * own value, and unwrapping by hand keeps the suspend identity resolution
     * unambiguous at each call site.
     */
    private fun <T> Result<*>.asFailure(): Result<T> =
        Result.failure(exceptionOrNull() ?: CommunityError("Unavailable"))

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
                authorId = owner,
                authorHandle = row.optString("author_handle").ifBlank { "A curious soul" },
                kind = row.optString("kind")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?.uppercase()
                    ?: KIND_CARD,
                topicName = row.optString("topic_name"),
                categoryName = row.optString("category_name"),
                categoryGlyph = row.optString("category_glyph"),
                accentHex = row.optString("accent_hex"),
                factText = row.optString("fact_text"),
                caption = row.optString("caption"),
                style = row.optString("style").ifBlank { "PAPER" },
                aspect = row.optString("aspect").ifBlank { "CLASSIC" },
                bodyScale = row.optDouble("body_scale", 1.0).toFloat(),
                byline = row.optString("byline"),
                createdAtMillis = epochMillis(row.optString("created_at")),
                expiresAtMillis = epochMillis(row.optString("expires_at")),
                likeCount = likes,
                likedByMe = likedByMe,
                commentCount = row.optJSONArray("community_comments")?.length() ?: 0,
                mine = myUserId != null && owner == myUserId
            )
        }
        return cards
    }

    /**
     * Fills in each card's author identity from `profiles`.
     *
     * WHY this exists: the row's `author_handle` is stamped at INSERT time by a
     * database trigger (so a modified client can never forge someone else's
     * name), which means the stored handle freezes the poster's name at the
     * moment they posted. Resolving the LIVE profile on read is what makes a
     * username change show up on older cards — and it is also the only source
     * of the author's portrait, because a card row carries no avatar.
     *
     * One request covers every distinct author on screen. A profile that is no
     * longer readable (Online Mode off) simply leaves the row's snapshot in
     * place rather than failing the whole feed.
     */
    private suspend fun withAuthors(
        accessToken: String,
        cards: List<CommunityCard>
    ): List<CommunityCard> {
        if (cards.isEmpty()) return cards
        val authors = SocialApi.people(accessToken, cards.map { it.authorId })
            .getOrDefault(emptyMap())
        if (authors.isEmpty()) return cards
        return cards.map { card ->
            val person = authors[card.authorId] ?: return@map card
            card.copy(authorName = person.username, authorAvatar = person.avatarStyle)
        }
    }

    /** The reply-side twin of [withAuthors]. */
    private suspend fun withCommentAuthors(
        accessToken: String,
        replies: List<CommunityComment>
    ): List<CommunityComment> {
        if (replies.isEmpty()) return replies
        val authors = SocialApi.people(accessToken, replies.map { it.authorId })
            .getOrDefault(emptyMap())
        if (authors.isEmpty()) return replies
        return replies.map { reply ->
            val person = authors[reply.authorId] ?: return@map reply
            reply.copy(authorName = person.username, authorAvatar = person.avatarStyle)
        }
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
        failure is IllegalStateException && raw.isNotBlank() -> raw
        raw.contains("rate limit", true) || raw.contains("too many", true) ->
            "Too much just now — try again in a minute."
        // The username handle has a case-insensitive unique index, so a
        // collision arrives as a 23505 duplicate-key error. Saying so is the
        // whole difference between a user knowing the name is taken and a
        // Save button that appears to do nothing.
        raw.contains("duplicate key", true) && raw.contains("username", true) ->
            "That username is already taken. Try another one."
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
