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
 * Someone else's PUBLIC identity — the only thing another account can ever see
 * about you in the social layer. There is no email, no capture, no card and no
 * message in here: the profile row the app may read holds display identity and
 * nothing else (see `supabase/schema.sql` §5b).
 */
data class CurioPerson(
    val userId: String,
    val displayName: String,
    val username: String = "",
    val avatarStyle: Int = 0
) {
    /** Stable identity shown everywhere social actions are available. */
    val handle: String get() = username.trim().removePrefix("@").ifBlank { "curious_soul" }
    // A profile may intentionally omit a display name. In that case its
    // username is still a real, stable identity — never replace it with a
    // product label or a generic "explorer" placeholder in Friends/DMs.
    val label: String get() = displayName.trim()
        .takeUnless { it.isBlank() || it.equals("null", true) }
        ?: username.trim().removePrefix("@").takeIf { it.isNotBlank() }
        ?: "A curious soul"
    val identityLabel: String get() = "${label} @${handle}"
}

/** One accepted friendship, with the person on the other side. */
data class CurioFriend(
    val requestId: String,
    val person: CurioPerson,
    val sinceMillis: Long
)

/**
 * One pending request. [incoming] is the only thing that decides what the UI
 * offers: an incoming one can be accepted or declined, an outgoing one can
 * only be cancelled.
 */
data class CurioFriendRequest(
    val id: String,
    val person: CurioPerson,
    val incoming: Boolean,
    val createdAtMillis: Long
)

/** One direct message in a conversation. */
data class CurioDirectMessage(
    val id: String,
    val senderId: String,
    val body: String,
    val createdAtMillis: Long,
    val readAtMillis: Long?,
    val mine: Boolean
)

/**
 * One reaction somebody left on one message. [kind] is a glyph NAME from
 * Curio's own icon set — the caller decides how it renders and whether the
 * reacting [userId] is the reader.
 */
data class CurioDmReaction(
    val messageId: String,
    val userId: String,
    val kind: String
)

/** One row in the inbox: the person, the last line, and how many are unread. */
data class CurioDmThread(
    val person: CurioPerson,
    val preview: String,
    val lastAtMillis: Long,
    val unread: Int
)

/**
 * The social layer's REST boundary — friend requests, friendships and direct
 * messages, and the ONLY place the app touches `friend_requests` /
 * `dm_messages` / the public half of `profiles`.
 *
 * Every rule the UI shows is also in `supabase/schema.sql`: discovery needs
 * Online Mode on both sides, a request can only be answered by its addressee,
 * and a message can only be sent between ACCEPTED friends. The client never
 * assumes it is the guard.
 */
object SocialApi {

    private const val PROFILES = "/rest/v1/profiles"
    private const val REQUESTS = "/rest/v1/friend_requests"
    private const val MESSAGES = "/rest/v1/dm_messages"
    private const val TYPING = "/rest/v1/dm_typing"
    private const val REACTIONS = "/rest/v1/dm_reactions"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** The server's own ceiling for one message (also a DB check constraint). */
    const val MAX_MESSAGE_CHARS = 2000

    /**
     * Every id that is pasted into a PostgREST query string must look like an
     * id. Without this a value carrying `&`, `,` or `.` could rewrite the
     * query it is dropped into (a hand-modified client, or a route argument),
     * so an id is validated before it is used rather than trusted.
     */
    private val ID_PATTERN = Regex("[A-Za-z0-9_-]{1,64}")

    private fun id(value: String): String {
        require(value.matches(ID_PATTERN)) { "That reference is not valid." }
        return value
    }

    /**
     * A local cooldown between writes.
     *
     * PostgREST cannot rate limit a signed-in user by itself, and a chat that
     * fires a request per keystroke or a bot that posts in a loop is a real
     * cost and abuse vector. This is the client half of the guard (the server
     * half is Supabase's own rate limiting): one message per second, one
     * request/report per two seconds, one username change per three seconds.
     * It never blocks a legitimate user — a human cannot type faster than it.
     */
    private const val WRITE_GAP_MS = 1_000L
    private const val SOCIAL_WRITE_GAP_MS = 2_000L
    private const val RENAME_GAP_MS = 3_000L
    private var lastMessageAt = 0L
    private var lastSocialWriteAt = 0L
    private var lastRenameAt = 0L

    private fun throttle(lastAt: Long, gap: Long, message: String): Long {
        val now = System.currentTimeMillis()
        if (now - lastAt < gap) throw IllegalStateException(message)
        return now
    }

    /** How many messages one thread load asks for. */
    private const val THREAD_LIMIT = 200

    /** How many recent messages the inbox groups into conversations. */
    private const val INBOX_SCAN = 200

    // ── people ───────────────────────────────────────────────────────────

    /**
     * Finds accounts by display name. Only accounts that turned Online Mode on
     * and left themselves discoverable can come back, and the caller is never
     * included — asking yourself for friendship is refused by the schema too.
     */
    suspend fun searchPeople(
        accessToken: String,
        query: String,
        myUserId: String?
    ): Result<List<CurioPerson>> = withContext(Dispatchers.IO) {
        mapped {
            val text = query.trim()
            if (text.length < 2) return@mapped emptyList()
            val path = "$PROFILES?select=id,display_name,username,avatar_style" +
                "&online_mode_enabled=is.true&discoverable=is.true" +
                "&or=(display_name.ilike.*${encode(text)}*,username.ilike.*${encode(text)}*)" +
                (myUserId?.let { "&id=neq.$it" } ?: "") +
                "&order=display_name.asc&limit=20"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            parsePeople(SupabaseClient.executeBody(request)).values.toList()
        }
    }

    /**
     * Resolves ids to display names. A friend who turned Online Mode off is no
     * longer readable, so they come back missing rather than failing the call —
     * the UI falls back to a neutral label instead of an empty screen.
     */
    suspend fun people(
        accessToken: String,
        ids: Collection<String>
    ): Result<Map<String, CurioPerson>> = withContext(Dispatchers.IO) {
        mapped { namesOf(accessToken, ids.toList()) }
    }

    /** Saves a normalized handle; the unique index turns duplicates into a safe failure. */
    suspend fun updateUsername(accessToken: String, username: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mapped {
                val normalized = username.trim().removePrefix("@").lowercase()
                require(normalized.matches(Regex("[a-z0-9_]{3,24}"))) {
                    "Usernames use 3 to 24 letters, numbers or underscores."
                }
                lastRenameAt = throttle(
                    lastRenameAt,
                    RENAME_GAP_MS,
                    "Give it a moment before changing your username again."
                )
                val userId = SupabaseClient.userIdFromAccessToken(accessToken)
                val body = JSONObject().put("username", normalized)
                val request = SupabaseClient.requestBuilder(
                    "$PROFILES?id=eq.$userId",
                    accessToken
                )
                    .patch(body.toString().toRequestBody(jsonMediaType))
                    .header("Prefer", "return=minimal")
                    .build()
                SupabaseClient.executeBody(request)
                Unit
            }
        }

    suspend fun updateAvatarStyle(accessToken: String, style: Int): Result<Unit> = withContext(Dispatchers.IO) {
        mapped {
            require(style in 0..15) { "Choose a valid avatar." }
            val userId = SupabaseClient.userIdFromAccessToken(accessToken)
            val request = SupabaseClient.requestBuilder("$PROFILES?id=eq.$userId", accessToken)
                .patch(JSONObject().put("avatar_style", style).toString().toRequestBody(jsonMediaType))
                .header("Prefer", "return=minimal")
                .build()
            SupabaseClient.executeBody(request)
            Unit
        }
    }

    // ── friend requests ──────────────────────────────────────────────────

    /** Every pending request, incoming and outgoing, newest first. */
    suspend fun requests(
        accessToken: String,
        myUserId: String
    ): Result<List<CurioFriendRequest>> = withContext(Dispatchers.IO) {
        mapped {
            val path = "$REQUESTS?select=id,requester,addressee,created_at" +
                "&status=eq.pending" +
                "&or=(requester.eq.${id(myUserId)},addressee.eq.${id(myUserId)})" +
                "&order=created_at.desc&limit=100"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            val rows = JSONArray(SupabaseClient.executeBody(request))
            val otherIds = ArrayList<String>(rows.length())
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                otherIds += otherSideId(row, myUserId) ?: continue
            }
            val names = namesOf(accessToken, otherIds)
            buildList(rows.length()) {
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val otherId = otherSideId(row, myUserId) ?: continue
                    add(
                        CurioFriendRequest(
                            id = row.optString("id"),
                            person = names[otherId] ?: CurioPerson(otherId, ""),
                            incoming = row.optString("addressee") == myUserId,
                            createdAtMillis = epochMillis(row.optString("created_at"))
                        )
                    )
                }
            }
        }
    }

    /** Every accepted friendship, newest first. */
    suspend fun friends(accessToken: String, myUserId: String): Result<List<CurioFriend>> =
        withContext(Dispatchers.IO) {
            mapped {
                val path = "$REQUESTS?select=id,requester,addressee,responded_at,created_at" +
                    "&status=eq.accepted" +
                    "&or=(requester.eq.${id(myUserId)},addressee.eq.${id(myUserId)})" +
                    "&order=responded_at.desc.nullslast&limit=200"
                val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                val rows = JSONArray(SupabaseClient.executeBody(request))
                val otherIds = ArrayList<String>(rows.length())
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    otherIds += otherSideId(row, myUserId) ?: continue
                }
                val names = namesOf(accessToken, otherIds)
                buildList(rows.length()) {
                    for (index in 0 until rows.length()) {
                        val row = rows.optJSONObject(index) ?: continue
                        val otherId = otherSideId(row, myUserId) ?: continue
                        val at = row.optString("responded_at").ifBlank { row.optString("created_at") }
                        add(
                            CurioFriend(
                                requestId = row.optString("id"),
                                person = names[otherId] ?: CurioPerson(otherId, ""),
                                sinceMillis = epochMillis(at)
                            )
                        )
                    }
                }
            }
        }

    /** Asks [userId] to be friends. The DB refuses a duplicate or self-ask. */
    suspend fun ask(accessToken: String, userId: String, myUserId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                if (userId == myUserId) throw IllegalArgumentException("That's you.")
                id(userId)
                lastSocialWriteAt = throttle(
                    lastSocialWriteAt,
                    SOCIAL_WRITE_GAP_MS,
                    "One at a time — try again in a moment."
                )
                val payload = JSONObject().put("addressee", userId)
                val request = SupabaseClient.requestBuilder(REQUESTS, accessToken)
                    .header("Prefer", "return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    /**
     * Accepts or declines. Only the addressee can move a pending request — the
     * server enforces it, so an unexpected caller simply matches zero rows.
     */
    suspend fun respond(accessToken: String, requestId: String, accept: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                id(requestId)
                val payload = JSONObject().put("status", if (accept) "accepted" else "declined")
                val request = SupabaseClient
                    .requestBuilder("$REQUESTS?id=eq.$requestId", accessToken)
                    .header("Prefer", "return=minimal")
                    .patch(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    /** Cancels a request you sent, or unfriends someone you are friends with. */
    suspend fun remove(accessToken: String, requestId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                id(requestId)
                val request = SupabaseClient
                    .requestBuilder("$REQUESTS?id=eq.$requestId", accessToken)
                    .delete()
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    // ── direct messages ──────────────────────────────────────────────────

    /**
     * The inbox: the latest message per counterpart, newest conversation
     * first, with the unread count for each. Built from the most recent
     * [INBOX_SCAN] messages — a conversation whose last line is older than
     * that many messages simply moves down the list, never disappears.
     */
    suspend fun threads(accessToken: String, myUserId: String): Result<List<CurioDmThread>> =
        withContext(Dispatchers.IO) {
            mapped {
                val path = "$MESSAGES?select=id,sender,recipient,body,created_at,read_at" +
                    "&or=(sender.eq.${id(myUserId)},recipient.eq.${id(myUserId)})" +
                    "&order=created_at.desc&limit=$INBOX_SCAN"
                val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                val rows = JSONArray(SupabaseClient.executeBody(request))

                val latest = LinkedHashMap<String, JSONObject>()
                val unread = HashMap<String, Int>()
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val other = otherSideId(row, myUserId) ?: continue
                    latest.putIfAbsent(other, row)
                    val mine = row.optString("sender") == myUserId
                    if (!mine && row.blankOrNull("read_at")) {
                        unread[other] = (unread[other] ?: 0) + 1
                    }
                }
                if (latest.isEmpty()) return@mapped emptyList()
                val names = namesOf(accessToken, latest.keys.toList())
                latest.map { (otherId, row) ->
                    CurioDmThread(
                        person = names[otherId] ?: CurioPerson(otherId, ""),
                        preview = row.optString("body"),
                        lastAtMillis = epochMillis(row.optString("created_at")),
                        // A row older than the scan window counts as read;
                        // the UI only needs enough to badge the row.
                        unread = unread[otherId] ?: 0
                    )
                }.sortedByDescending { it.lastAtMillis }
            }
        }

    /** One conversation, oldest first (a chat reads downwards). */
    suspend fun messages(
        accessToken: String,
        otherUserId: String,
        myUserId: String
    ): Result<List<CurioDirectMessage>> = withContext(Dispatchers.IO) {
        mapped {
            val path = "$MESSAGES?select=id,sender,recipient,body,created_at,read_at" +
                "&or=(and(sender.eq.${id(myUserId)},recipient.eq.${id(otherUserId)})," +
                "and(sender.eq.${id(otherUserId)},recipient.eq.${id(myUserId)}))" +
                "&order=created_at.asc&limit=$THREAD_LIMIT"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            val rows = JSONArray(SupabaseClient.executeBody(request))
            buildList(rows.length()) {
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    add(
                        CurioDirectMessage(
                            id = row.optString("id"),
                            senderId = row.optString("sender"),
                            body = row.optString("body"),
                            createdAtMillis = epochMillis(row.optString("created_at")),
                            readAtMillis = row.optString("read_at")
                                .takeIf { it.isNotBlank() }
                                ?.let(::epochMillis),
                            mine = row.optString("sender") == myUserId
                        )
                    )
                }
            }
        }
    }

    /** Sends one text message. Only an accepted friend can be messaged. */
    suspend fun send(
        accessToken: String,
        toUserId: String,
        body: String,
        myUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val text = body.trim()
            if (text.isEmpty()) throw IllegalArgumentException("Write something first.")
            if (text.length > MAX_MESSAGE_CHARS) {
                throw IllegalArgumentException(
                    "Keep it under $MAX_MESSAGE_CHARS characters (it's ${text.length})."
                )
            }
            if (toUserId == myUserId) throw IllegalArgumentException("You can't message yourself.")
            id(toUserId)
            lastMessageAt = throttle(lastMessageAt, WRITE_GAP_MS, "Slow down a moment.")
            val payload = JSONObject().put("recipient", toUserId).put("body", text)
            val request = SupabaseClient.requestBuilder(MESSAGES, accessToken)
                .header("Prefer", "return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /**
     * Stamps a read receipt on everything [otherUserId] sent this account.
     * The server only accepts the update on rows where the caller is the
     * recipient, so this can never mark someone else's inbox.
     */
    suspend fun markRead(
        accessToken: String,
        otherUserId: String,
        myUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val payload = JSONObject().put("read_at", Instant.now().toString())
            val path = "$MESSAGES?recipient=eq.${id(myUserId)}" +
                "&sender=eq.${id(otherUserId)}&read_at=is.null"
            val request = SupabaseClient.requestBuilder(path, accessToken)
                .header("Prefer", "return=minimal")
                .patch(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** Deletes one of your own messages. */
    suspend fun deleteMessage(accessToken: String, messageId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val request = SupabaseClient
                    .requestBuilder("$MESSAGES?id=eq.${id(messageId)}", accessToken)
                    .delete()
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    // ── typing ───────────────────────────────────────────────────────────

    /**
     * How long an "is typing…" row stays meaningful. The writer refreshes its
     * row every couple of seconds while there is text in the composer, so a
     * row older than this means the other person stopped, sent, or closed the
     * app — the composer should go quiet rather than lie.
     */
    private const val TYPING_FRESH_MS = 7_000L

    /**
     * Upserts YOUR "is typing…" row for [toUserId], or clears it.
     *
     * Best-effort by design: this fires on every keystroke pause and on every
     * send, so a failure must never surface as an error — the ticker simply
     * stops, which is exactly what "no longer typing" looks like anyway.
     */
    suspend fun setTyping(
        accessToken: String,
        toUserId: String,
        typing: Boolean
    ): Unit = withContext(Dispatchers.IO) {
        runCatching {
            if (!toUserId.matches(ID_PATTERN)) return@runCatching
            if (typing) {
                val payload = JSONObject().put("recipient", toUserId)
                val request = SupabaseClient.requestBuilder(TYPING, accessToken)
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                SupabaseClient.executeBody(request)
            } else {
                val request = SupabaseClient
                    .requestBuilder("$TYPING?recipient=eq.$toUserId", accessToken)
                    .delete()
                    .build()
                SupabaseClient.executeBody(request)
            }
            Unit
        }
    }

    /**
     * True when [otherUserId] has a fresh typing row addressed to this account.
     *
     * A missing table (a project that has not been re-pasted since the typing
     * feature landed) answers false instead of failing the poll.
     */
    suspend fun isTyping(
        accessToken: String,
        myUserId: String,
        otherUserId: String
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (!otherUserId.matches(ID_PATTERN) || !myUserId.matches(ID_PATTERN)) {
                return@runCatching false
            }
            val path = "$TYPING?select=updated_at&sender=eq.$otherUserId" +
                "&recipient=eq.$myUserId&limit=1"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            val rows = JSONArray(SupabaseClient.executeBody(request))
            val stamp = rows.optJSONObject(0)?.optString("updated_at").orEmpty()
            if (stamp.isBlank()) return@runCatching false
            val at = epochMillis(stamp)
            at > 0L && System.currentTimeMillis() - at <= TYPING_FRESH_MS
        }.getOrDefault(false)
    }

    // ── reactions ────────────────────────────────────────────────────────

    /**
     * Every reaction on the given messages, keyed by message id.
     *
     * The glyph is stored as a NAME from Curio's own icon set, so a reaction
     * row is a few bytes on the server and nothing is ever uploaded.
     */
    suspend fun reactions(
        accessToken: String,
        messageIds: Collection<String>
    ): Result<Map<String, List<CurioDmReaction>>> = withContext(Dispatchers.IO) {
        mapped {
            val wanted = messageIds.filter { it.matches(ID_PATTERN) }.distinct()
            if (wanted.isEmpty()) return@mapped emptyMap()
            val path = "$REACTIONS?select=message_id,user_id,kind" +
                "&message_id=in.(${wanted.joinToString(",")})&limit=500"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            val rows = JSONArray(SupabaseClient.executeBody(request))
            buildMap<String, MutableList<CurioDmReaction>> {
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val messageId = row.optString("message_id").takeIf { it.isNotBlank() } ?: continue
                    val kind = row.optString("kind").trim().takeIf { it.isNotBlank() } ?: continue
                    getOrPut(messageId) { mutableListOf() } += CurioDmReaction(
                        messageId = messageId,
                        userId = row.optString("user_id"),
                        kind = kind
                    )
                }
            }
        }
    }

    /**
     * Leaves [kind] on one of your own thread's messages. A second reaction
     * from the same account REPLACES the first (the primary key is the
     * message + the person), which is the documented behaviour of the UI.
     */
    suspend fun react(accessToken: String, messageId: String, kind: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                id(messageId)
                require(kind.length in 1..32) { "Choose a reaction." }
                val payload = JSONObject().put("message_id", messageId).put("kind", kind)
                val request = SupabaseClient.requestBuilder(REACTIONS, accessToken)
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    /** Takes your reaction off a message. */
    suspend fun clearReaction(accessToken: String, messageId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val request = SupabaseClient
                    .requestBuilder("$REACTIONS?message_id=eq.${id(messageId)}", accessToken)
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

    /** See [CommunityApi]: writes discard the response body and answer Unit. */
    private fun mappedUnit(block: () -> Unit): Result<Unit> = mapped(block)

    private fun parsePeople(body: String): Map<String, CurioPerson> {
        val array = JSONArray(body)
        val people = LinkedHashMap<String, CurioPerson>(array.length())
        for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            val id = row.optString("id").takeIf { it.isNotBlank() } ?: continue
            people[id] = CurioPerson(
                userId = id,
displayName = row.optString("display_name", "").takeUnless { it == "null" }.orEmpty(),
            username = row.optString("username", "").takeUnless { it == "null" }.orEmpty(),
            avatarStyle = row.optInt("avatar_style", 0).coerceIn(0, 15)
            )
        }
        return people
    }

    /** Names for [ids], best-effort: an unreadable profile stays unnamed. */
    private fun namesOf(accessToken: String, ids: List<String>): Map<String, CurioPerson> {
        if (ids.isEmpty()) return emptyMap()
        val wanted = ids.filter { it.isNotBlank() }.distinct()
        if (wanted.isEmpty()) return emptyMap()
        val path = "$PROFILES?select=id,display_name,username,avatar_style&id=in.(${wanted.joinToString(",")})" +
            "&limit=${wanted.size}"
        return runCatching {
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            parsePeople(SupabaseClient.executeBody(request))
        }.getOrDefault(emptyMap())
    }

    /**
     * True when a timestamp column is absent, JSON null, or blank.
     *
     * NOT named `isNull`: `JSONObject` already has a member of that name, and
     * a member always wins over an extension — the helper would silently never
     * run.
     */
    private fun JSONObject.blankOrNull(key: String): Boolean =
        !has(key) || get(key) == JSONObject.NULL || optString(key).isBlank()

    private fun otherSideId(row: JSONObject, myUserId: String): String? {
        val sender = row.optString("sender", row.optString("requester"))
        val recipient = row.optString("recipient", row.optString("addressee"))
        return when {
            sender == myUserId -> recipient.takeIf { it.isNotBlank() }
            recipient == myUserId -> sender.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    /** PostgREST answers with an offset timestamp — 0 when it cannot be read. */
    private fun epochMillis(iso: String): Long =
        runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrDefault(0L)

    /** Query strings can't carry the raw text a user typed. */
    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")
}
