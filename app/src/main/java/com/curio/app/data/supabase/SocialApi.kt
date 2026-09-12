package com.curio.app.data.supabase

import com.curio.app.data.CurioContentFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime

/** Who may see a profile. Mirrors the schema's own check constraint (§5f). */
const val PROFILE_VISIBILITY_PUBLIC = "public"
const val PROFILE_VISIBILITY_FRIENDS = "friends"

/**
 * How many code-drawn social portraits exist (the `AVATARS` list in
 * `features/community/SocialAvatar.kt`). `profiles.avatar_style` is an index
 * into that list, so EVERY read and write clamps against this constant — a
 * style added later must never be truncated by a stale `coerceIn(0, 15)` at a
 * cache or API boundary. `supabase/schema.sql`'s `avatar_style` check must be
 * widened in the same commit (currently `between 0 and 27`).
 */
const val SOCIAL_AVATAR_STYLE_COUNT = 28

/**
 * Someone else's PUBLIC identity — the only thing another account can ever see
 * about you in the social layer. There is no email, no capture, no card and no
 * message in here: the profile row the app may read holds display identity,
 * the privacy choices that narrow who may read it, and (when the member left
 * activity visible) a last-active stamp.
 */
data class CurioPerson(
    val userId: String,
    val displayName: String,
    val username: String = "",
    val avatarStyle: Int = 0,
    /** [PROFILE_VISIBILITY_PUBLIC] or [PROFILE_VISIBILITY_FRIENDS]. */
    val visibility: String = PROFILE_VISIBILITY_PUBLIC,
    /** True when this member publishes no last-active stamp at all. */
    val hideActivity: Boolean = false,
    val presenceMode: String = com.curio.app.data.AppPreferences.PRESENCE_ACTIVE,
    /** Their own line, shown inside direct chats only. */
    val bio: String = "",
    /** Last-active stamp, 0 when unknown — or when activity is hidden. */
    val lastActiveMillis: Long = 0L
) {
    /** Stable identity shown everywhere social actions are available. */
    val handle: String get() = username.trim().removePrefix("@").ifBlank { "curious_soul" }

    /** The handle as it is printed, so no caller re-invents the `@`. */
    val handleLabel: String get() = "@$handle"

    /**
     * True when this member chose a display name. The display name LEADS
     * everywhere and the handle reads underneath it; without one, the handle
     * is the whole identity — never a product label or a generic "explorer".
     */
    val hasDisplayName: Boolean
        get() = displayName.trim().let { it.isNotBlank() && !it.equals("null", true) }

    // A profile may intentionally omit a display name. In that case its
    // username is still a real, stable identity — never replace it with a
    // product label or a generic "explorer" placeholder in Friends/DMs.
    val label: String get() = displayName.trim()
        .takeUnless { it.isBlank() || it.equals("null", true) }
        ?: username.trim().removePrefix("@").takeIf { it.isNotBlank() }
        ?: "A curious soul"
    val identityLabel: String get() = "${label} @${handle}"

    /**
     * The presence line, or null when there is nothing honest to say: a member
     * who hid activity, one whose stamp is unknown, and one who is not
     * currently online all answer null rather than a claim.
     */
    val presenceLabel: String?
        get() {
            if (hideActivity) return null
            val at = lastActiveMillis
            if (at <= 0L) return null
            val minutes = (System.currentTimeMillis() - at).coerceAtLeast(0L) / 60_000L
            return when {
                minutes < 5L -> "Active now"
                minutes < 60L -> "Active ${minutes}m ago"
                minutes < 60L * 24L -> "Active ${minutes / 60L}h ago"
                else -> null
            }
        }

    val presenceExactLabel: String?
        get() = if (hideActivity || lastActiveMillis <= 0L) null else
            java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.MEDIUM,
                java.text.DateFormat.SHORT
            ).format(java.util.Date(lastActiveMillis))
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
 * One reaction somebody left on one message. [kind] is the reaction itself —
 * an emoji character — so the server stores a few bytes and the caller only
 * decides whether the reacting [userId] is the reader.
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
    private const val BLOCKS = "/rest/v1/member_blocks"
    private const val MESSAGES = "/rest/v1/dm_messages"
    private const val TYPING = "/rest/v1/dm_typing"
    private const val REACTIONS = "/rest/v1/dm_reactions"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** The server's own ceiling for one message (also a DB check constraint). */
    const val MAX_MESSAGE_CHARS = 2000

    /** The profile bio's ceiling — mirrors the `profiles_bio_len` constraint. */
    const val MAX_BIO_CHARS = 160

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

    /** The columns a conversation read needs — one list, both paths. */
    private const val MESSAGE_COLUMNS = "id,sender,recipient,body,created_at,read_at"

    /**
     * The public identity columns, in two shapes.
     *
     * [PERSON_COLUMNS] is what EVERY identity read asks for, and it is the
     * shape every project has: adding a column to it would make a project that
     * has not been re-pasted since §5f answer 400 to every profile read, which
     * would blank the wall's author names and every friend row. The privacy
     * columns are therefore a SECOND, opt-in shape that only the profile page
     * asks for — and it falls back to the base read when they are missing.
     */
    private const val PERSON_COLUMNS = "id,display_name,username,avatar_style"
private const val PERSON_COLUMNS_PRIVACY =
    "$PERSON_COLUMNS,profile_visibility,hide_activity,presence_mode,last_active_at,bio"

    /** How many NEW messages one live tick asks for. */
    private const val LIVE_TICK_LIMIT = 100

    /** How many of my own messages a receipt refresh looks back over. */
    private const val RECEIPT_LIMIT = 12

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
            val path = "$PROFILES?select=$PERSON_COLUMNS" +
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
                CurioContentFilter.problem(normalized)?.let {
                    throw IllegalArgumentException(it)
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
                try {
                    SupabaseClient.executeBody(request)
                } catch (failure: Throwable) {
                    // A unique-index collision is THE expected failure on this
                    // call, and the constraint's NAME is an implementation
                    // detail of how the schema was created — so the collision
                    // is recognised by its shape here and answered with the
                    // one thing the person needs to hear. (It used to fall
                    // through to "You've already done that.", which told the
                    // user nothing about the name they were claiming.)
                    val raw = failure.message.orEmpty()
                    if (raw.contains("duplicate key", true) ||
                        raw.contains("23505", true) ||
                        raw.contains("already exists", true) ||
                        raw.contains("unique constraint", true)
                    ) {
                        throw IllegalStateException(
                            "That username is already taken. Try another one."
                        )
                    }
                    throw failure
                }
                Unit
            }
        }

    suspend fun updateAvatarStyle(accessToken: String, style: Int): Result<Unit> = withContext(Dispatchers.IO) {
        mapped {
            require(style in 0 until SOCIAL_AVATAR_STYLE_COUNT) { "Choose a valid avatar." }
            val userId = SupabaseClient.userIdFromAccessToken(accessToken)
            val request = SupabaseClient.requestBuilder("$PROFILES?id=eq.$userId", accessToken)
                .patch(JSONObject().put("avatar_style", style).toString().toRequestBody(jsonMediaType))
                .header("Prefer", "return=minimal")
                .build()
            SupabaseClient.executeBody(request)
            Unit
        }
    }

    /**
     * ONE member's profile, with the privacy columns when the project has
     * them. A project that has not been re-pasted since §5f answers 400 to the
     * wider select, so this falls back to the base identity read rather than
     * blanking the page — a missing column must never hide a person.
     */
    suspend fun profile(accessToken: String, userId: String): Result<CurioPerson?> =
        withContext(Dispatchers.IO) {
            mapped {
                if (!userId.matches(ID_PATTERN)) return@mapped null
                val wide = runCatching {
                    val path = "$PROFILES?select=$PERSON_COLUMNS_PRIVACY" +
                        "&id=eq.$userId&limit=1"
                    val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                    parsePeople(SupabaseClient.executeBody(request)).values.firstOrNull()
                }.getOrNull()
                wide ?: namesOf(accessToken, listOf(userId))[userId]
            }
        }

    /**
     * Saves the display name — the name a member is SEEN by. The username is a
     * separate thing (the stable handle other people find you by), which is why
     * this never touches it.
     */
    suspend fun updateDisplayName(accessToken: String, displayName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val clean = displayName.trim().take(40)
                // v3xx53 — names carry the same filter as everything else, and
                // the wording says what happens to an account that ignores it.
                CurioContentFilter.problem(clean)?.let { throw IllegalArgumentException(it) }
                val userId = SupabaseClient.userIdFromAccessToken(accessToken)
                val body = JSONObject().put(
                    "display_name",
                    if (clean.isEmpty()) JSONObject.NULL else clean
                )
                val request = SupabaseClient.requestBuilder("$PROFILES?id=eq.$userId", accessToken)
                    .patch(body.toString().toRequestBody(jsonMediaType))
                    .header("Prefer", "return=minimal")
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    /**
     * Saves the member's public bio — their own line on their own profile.
     *
     * Mirrors the local value (the Edit-profile Bio field) onto the profile row
     * so other members read it, not only this device. A blank value clears the
     * column rather than storing an empty string, so "no bio" is one thing
     * everywhere.
     */
    suspend fun updateBio(accessToken: String, bio: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val clean = bio.trim().take(MAX_BIO_CHARS)
                CurioContentFilter.problem(clean)?.let { throw IllegalArgumentException(it) }
                val userId = SupabaseClient.userIdFromAccessToken(accessToken)
                val body = JSONObject().put(
                    "bio",
                    if (clean.isEmpty()) JSONObject.NULL else clean
                )
                val request = SupabaseClient.requestBuilder("$PROFILES?id=eq.$userId", accessToken)
                    .patch(body.toString().toRequestBody(jsonMediaType))
                    .header("Prefer", "return=minimal")
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    /**
     * Mirrors the member's privacy choices onto the profile row.
     *
     * Turning activity hiding ON clears any stamp already stored in the same
     * write, so a member who hid activity has nothing on the server for anyone
     * to read — the promise is kept by absence, not by a flag.
     */
    suspend fun updatePrivacy(
        accessToken: String,
        visibility: String,
        hideActivity: Boolean,
        presenceMode: String = if (hideActivity) com.curio.app.data.AppPreferences.PRESENCE_HIDDEN else com.curio.app.data.AppPreferences.PRESENCE_ACTIVE
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val clean = if (visibility == PROFILE_VISIBILITY_FRIENDS) {
                PROFILE_VISIBILITY_FRIENDS
            } else {
                PROFILE_VISIBILITY_PUBLIC
            }
            val userId = SupabaseClient.userIdFromAccessToken(accessToken)
            val body = JSONObject()
                .put("profile_visibility", clean)
                .put("hide_activity", presenceMode == com.curio.app.data.AppPreferences.PRESENCE_HIDDEN)
                .put("presence_mode", presenceMode)
            if (presenceMode == com.curio.app.data.AppPreferences.PRESENCE_HIDDEN) {
                body.put("last_active_at", JSONObject.NULL)
            }
            val request = SupabaseClient.requestBuilder("$PROFILES?id=eq.$userId", accessToken)
                .patch(body.toString().toRequestBody(jsonMediaType))
                .header("Prefer", "return=minimal")
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /**
     * Publishes this account's last-active stamp, or clears it.
     *
     * Best-effort and quiet: presence is a courtesy line, so a failed write
     * must never surface as an error, and it is only ever called while the
     * member has NOT hidden activity.
     */
    suspend fun setPresence(accessToken: String, atMillis: Long?): Unit =
        withContext(Dispatchers.IO) {
            runCatching {
                val userId = SupabaseClient.userIdFromAccessToken(accessToken)
                val body = JSONObject().put(
                    "last_active_at",
                    atMillis?.let { Instant.ofEpochMilli(it).toString() } ?: JSONObject.NULL
                )
                val request = SupabaseClient.requestBuilder("$PROFILES?id=eq.$userId", accessToken)
                    .patch(body.toString().toRequestBody(jsonMediaType))
                    .header("Prefer", "return=minimal")
                    .build()
                SupabaseClient.executeBody(request)
                Unit
            }
        }

    // ── blocks ───────────────────────────────────────────────────────────

    /**
     * Everyone this account has blocked.
     *
     * Best-effort, like the typing and reaction tables: a project that has not
     * been re-pasted since §5f has no `member_blocks`, and that answers an
     * empty list rather than failing the screen that asked.
     */
    suspend fun blocks(accessToken: String): Result<List<CurioPerson>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = "$BLOCKS?select=blocked&order=created_at.desc&limit=200"
                val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                val rows = JSONArray(SupabaseClient.executeBody(request))
                val ids = ArrayList<String>(rows.length())
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val blocked = row.optString("blocked")
                    if (blocked.isNotBlank()) ids += blocked
                }
                val names = namesOf(accessToken, ids)
                ids.distinct().map { id -> names[id] ?: CurioPerson(id, "") }
            }
        }

    /**
     * Blocks [userId]: no messages, no requests, no cards, no replies, no
     * profile, in either direction. The policy set in §5f is what actually
     * enforces it; this only records the decision.
     */
    suspend fun block(accessToken: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                id(userId)
                val payload = JSONObject().put("blocked", userId)
                val request = SupabaseClient.requestBuilder(BLOCKS, accessToken)
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    /** Lifts a block. Nothing else changes — a friendship must be re-made. */
    suspend fun unblock(accessToken: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                id(userId)
                val request = SupabaseClient.requestBuilder("$BLOCKS?blocked=eq.$userId", accessToken)
                    .delete()
                    .build()
                SupabaseClient.executeBody(request)
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
                // Treat the action as idempotent: the unique pair constraint
                // is expected to reject a second tap, so check both directions
                // before inserting and return a useful state instead of a raw
                // Postgres duplicate-key error.
                val existingPath = "$REQUESTS?select=id,status&or=(and(requester.eq.${id(myUserId)},addressee.eq.${id(userId)}),and(requester.eq.${id(userId)},addressee.eq.${id(myUserId)}))&status=in.(pending,accepted)&limit=1"
                val existingRequest = SupabaseClient.requestBuilder(existingPath, accessToken).get().build()
                if (JSONArray(SupabaseClient.executeBody(existingRequest)).length() > 0) {
                    throw IllegalStateException("You are already friends or a request is already waiting.")
                }
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
            val path = "$MESSAGES?select=$MESSAGE_COLUMNS" +
                "&or=(and(sender.eq.${id(myUserId)},recipient.eq.${id(otherUserId)})," +
                "and(sender.eq.${id(otherUserId)},recipient.eq.${id(myUserId)}))" +
                "&order=created_at.asc&limit=$THREAD_LIMIT"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            parseMessages(SupabaseClient.executeBody(request), myUserId)
        }
    }

    /**
     * ONLY what arrived after [sinceMillis] — what an open conversation polls.
     *
     * A live thread must not re-read its whole page every few seconds: this
     * asks the server for the rows newer than the newest one on screen, so a
     * tick costs a few hundred bytes and can run several times a minute. The
     * requested window backs off by a second to absorb clock skew between the
     * phone and the server, and the caller de-dupes by id (re-reading one
     * message is free; missing one is not).
     */
    suspend fun messagesSince(
        accessToken: String,
        otherUserId: String,
        myUserId: String,
        sinceMillis: Long
    ): Result<List<CurioDirectMessage>> = withContext(Dispatchers.IO) {
        mapped {
            val stamp = Instant
                .ofEpochMilli((sinceMillis - 1_000L).coerceAtLeast(0L))
                .toString()
            val path = "$MESSAGES?select=$MESSAGE_COLUMNS" +
                "&or=(and(sender.eq.${id(myUserId)},recipient.eq.${id(otherUserId)})," +
                "and(sender.eq.${id(otherUserId)},recipient.eq.${id(myUserId)}))" +
                "&created_at=gt.$stamp&order=created_at.asc&limit=$LIVE_TICK_LIMIT"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            parseMessages(SupabaseClient.executeBody(request), myUserId)
        }
    }

    /** One PostgREST page of `dm_messages` rows, oldest first. */
    private fun parseMessages(body: String, myUserId: String): List<CurioDirectMessage> {
        val rows = JSONArray(body)
        return buildList(rows.length()) {
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
            // v3xx53 — a direct message is NOT filtered. Two friends having a
            // private conversation write what they like; only the PUBLIC
            // surfaces (parts of `CommunityApi`, plus usernames, display names
            // and bios) pass CurioContentFilter. RLS — the two participants,
            // friends only — is what protects a thread, not a word list.
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

    /**
     * The read stamps on MY last messages to [otherUserId] — what a "Seen"
     * line is actually made of.
     *
     * A receipt lives on the RECIPIENT's copy of a row (`read_at`), so a
     * thread that only refreshed when it was entered showed a receipt that was
     * stale or missing entirely: the other person read the message, and the
     * sender's screen never learned. This asks for a handful of ids and stamps
     * on the same live tick as the messages themselves, so "Seen" is true when
     * it is shown and absent when it is not.
     */
    suspend fun readStamps(
        accessToken: String,
        otherUserId: String,
        myUserId: String
    ): Result<Map<String, Long>> = withContext(Dispatchers.IO) {
        mapped {
            val path = "$MESSAGES?select=id,read_at" +
                "&sender=eq.${id(myUserId)}&recipient=eq.${id(otherUserId)}" +
                "&read_at=not.is.null&order=created_at.desc&limit=$RECEIPT_LIMIT"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            val rows = JSONArray(SupabaseClient.executeBody(request))
            buildMap {
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val messageId = row.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val at = epochMillis(row.optString("read_at"))
                    if (at > 0L) put(messageId, at)
                }
            }
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
     * `kind` holds the emoji itself, so a reaction row is a few bytes on the
     * server, nothing is ever uploaded, and the renderer needs no mapping
     * table to stay in step with this file.
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
            buildMap<String, List<CurioDmReaction>> {
                val grouped = linkedMapOf<String, MutableList<CurioDmReaction>>()
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val messageId = row.optString("message_id").takeIf { it.isNotBlank() } ?: continue
                    val kind = row.optString("kind").trim().takeIf { it.isNotBlank() } ?: continue
                    grouped.getOrPut(messageId) { mutableListOf() } += CurioDmReaction(
                        messageId = messageId,
                        userId = row.optString("user_id"),
                        kind = kind
                    )
                }
                grouped.forEach { (messageId, values) -> put(messageId, values.toList()) }
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
                avatarStyle = row.optInt("avatar_style", 0)
                    .coerceIn(0, SOCIAL_AVATAR_STYLE_COUNT - 1),
                // Absent for a project that has not been re-pasted since §5f —
                // visibility then reads as the schema's own default and a
                // presence line simply never gets enough information to draw.
                visibility = row.optString("profile_visibility", "")
                    .takeUnless { it == "null" }
                    .orEmpty()
                    .ifBlank { PROFILE_VISIBILITY_PUBLIC },
                hideActivity = row.optBoolean("hide_activity", false),
                presenceMode = row.optString("presence_mode", "").takeIf {
                    it == com.curio.app.data.AppPreferences.PRESENCE_ACTIVE ||
                        it == com.curio.app.data.AppPreferences.PRESENCE_DND ||
                        it == com.curio.app.data.AppPreferences.PRESENCE_HIDDEN
                } ?: if (row.optBoolean("hide_activity", false)) {
                    com.curio.app.data.AppPreferences.PRESENCE_HIDDEN
                } else {
                    com.curio.app.data.AppPreferences.PRESENCE_ACTIVE
                },
                bio = row.optString("bio", "").takeUnless { it == "null" }.orEmpty(),
                lastActiveMillis = epochMillis(row.optString("last_active_at"))
            )
        }
        return people
    }

    /** Names for [ids], best-effort: an unreadable profile stays unnamed. */
    private fun namesOf(accessToken: String, ids: List<String>): Map<String, CurioPerson> {
        if (ids.isEmpty()) return emptyMap()
        // Every id is VALIDATED before it is pasted into the query, exactly
        // like `id()` does for a single one: some of these ids come back off
        // the server (a card's owner, a message's sender), so a response that
        // had been tampered with must not be able to rewrite the request that
        // follows it. An unexpected id is dropped, not trusted.
        val wanted = ids.filter { it.matches(ID_PATTERN) }.distinct()
        if (wanted.isEmpty()) return emptyMap()
        val path = "$PROFILES?select=$PERSON_COLUMNS&id=in.(${wanted.joinToString(",")})" +
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
