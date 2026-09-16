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
    /**
     * The author's DISPLAY NAME, resolved the same way. The display name LEADS
     * on the wall and the `@username` reads underneath it — a name and a
     * handle are two different things, and a member's card should carry both.
     */
    val authorDisplayName: String = "",
    /** The author's chosen portrait (0–15). */
    val authorAvatar: Int = 0,
    /** CARD (a topic share card), NOTE (a text-only post), QUOTE, or REPOST
     *  (the member's words above somebody else's post). */
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
  val dislikeCount: Int = 0,
  val dislikedByMe: Boolean = false,
  /** How many replies hang under the card. */
    val commentCount: Int,
    /** True when this device's account posted the card. */
    val mine: Boolean,
    /** For a REPOST: the id of the post being quoted (null otherwise). */
    val quoteSourceId: String? = null,
    /** For a REPOST: the member's own words above the quoted post. */
    val quoteWords: String = "",
    /** For a REPOST: the quoted post itself, fully joined at read time. */
    val quoteSource: CommunityCard? = null
) {
    /** Hours left before the card disappears, floored at 0. */
    val hoursLeft: Long
        get() = expiresAtMillis.takeIf { it > 0 }
            ?.let { ((it - System.currentTimeMillis()).coerceAtLeast(0L)) / 3_600_000L }
            ?: 0L

    /**
     * What to PRINT for the author: the display name first, then the live
     * username, then the post-time snapshot. Never a product label — a
     * member's card must always carry a real human identity.
     */
    val authorLabel: String
        get() {
            val name = authorDisplayName.trim().removePrefix("@")
            if (name.isNotBlank() && !name.equals("null", true)) return name
            val live = authorName.trim().removePrefix("@")
            return live.ifBlank { authorHandle.trim().removePrefix("@") }
                .ifBlank { "a curious soul" }
        }

    /**
     * The handle as it is printed on the SECOND line, `@` included. The live
     * username wins over the post-time snapshot, and the snapshot is what a
     * member keeps when their profile stops being readable.
     */
    val authorHandleLabel: String
        get() {
            val live = authorName.trim().removePrefix("@")
            val snapshot = authorHandle.trim().removePrefix("@")
            return "@" + live.ifBlank { snapshot }.ifBlank { "a curious soul" }
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
data class CommunityReport(
    val id: String,
    val cardId: String?,
    val commentId: String?,
    val targetUserId: String?,
    val reporterId: String,
    val reason: String,
    val note: String?,
    /** open | dismissed | resolved */
    val status: String,
    /** What the moderator decided, in one line. */
    val resolution: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    /** card | comment | user — what this report names. */
    val targetKind: String
        get() = when {
            cardId != null -> "card"
            commentId != null -> "comment"
            else -> "user"
        }

    /** The reported row's id (a member report names the member). */
    val targetId: String get() = cardId ?: commentId ?: targetUserId.orEmpty()

    val open: Boolean get() = status == "open"
}

/**
 * One moderator's row in `community_admins`.
 *
 * [role] is 'owner' (everything, protected) or 'admin' (the five switches). The
 * client reads this to decide what to OFFER; the database asks
 * `curio_admin_can` again for every action, so a hidden button is never the
 * guard.
 */
data class CommunityAdminRow(
    val userId: String,
    val role: String,
    val canDeletePosts: Boolean,
    val canDeleteReplies: Boolean,
    val canHandleReports: Boolean,
    val canManageAdmins: Boolean,
    val canBanMembers: Boolean
) {
    val owner: Boolean get() = role == "owner"

    /** 'posts' | 'replies' | 'reports' | 'admins' | 'bans'. */
    fun allows(permission: String): Boolean = when {
        owner -> true
        permission == "posts" -> canDeletePosts
        permission == "replies" -> canDeleteReplies
        permission == "reports" -> canHandleReports
        permission == "admins" -> canManageAdmins
        permission == "bans" -> canBanMembers
        else -> false
    }
}

/**
 * The report reasons the app offers, one per target kind.
 *
 * A short label (the schema stores it in one column, ≤40 chars) — the NOTE is
 * where a member adds anything specific, and every one of these is shown to a
 * moderator beside the content it names.
 */
object CommunityReportReasons {
    /** A card or a reply. */
    val CONTENT = listOf(
        "Spam or scam",
        "Harassment or hate",
        "Sexual content",
        "False or harmful claim",
        "Not what it claims to be",
        "Something else"
    )

    /** A member. */
    val MEMBER = listOf(
        "Harassment or bullying",
        "Impersonation",
        "Hateful name or portrait",
        "Spam account",
        "Something else"
    )
}

/** The reasons a moderator can put on a removal. */
object ModerationReasons {
    val REMOVAL = listOf(
        "Harassment or hate",
        "Sexual content",
        "Spam or scam",
        "False or harmful claim",
        "Off-topic",
        "Duplicate",
        "Something else"
    )

    val HIDE = listOf(
        "Repeated harassment",
        "Hate speech",
        "Spam or scam account",
        "Impersonation",
        "Repeated policy breaks",
        "Something else"
    )

    /**
     * The reasons a BAN is set at a tier. The ladder is what makes these
     * different from [HIDE]: a view-only week and a locked account are the same
     * paperwork, so the list is one list and the tier carries the severity.
     */
    val BAN = listOf(
        "Repeated harassment",
        "Hate speech",
        "Threats or doxxing",
        "Sexual content involving minors",
        "Spam or scam account",
        "Ban evasion",
        "Repeated policy breaks",
        "Something else"
    )

    /**
     * The reasons a ban comes OFF. A lift is a decision too — "time served",
     * "appeal accepted", "set by mistake" — and it is the one a member is most
     * likely to ask about months later, when the ban itself is only history.
     */
    val LIFT = listOf(
        "Time served",
        "Appeal accepted",
        "Set by mistake",
        "Context was missing",
        "Something else"
    )
}

data class CommunityCardDraft(
    val topicName: String,
    val categoryName: String,
    val categorySlug: String,
    val categoryGlyph: String,
    val accentHex: String,
    val factText: String,
    /** The poster's own line above the card — optional, never media. */
    val caption: String = "",
    /** CARD, NOTE, QUOTE or REPOST — the renderer a card is rebuilt with. */
    val kind: String = KIND_CARD,
    val style: String = "PAPER",
    val aspect: String = "CLASSIC",
    val bodyScale: Float = 1f,
    val byline: String = "",
    /** REPOST only: the id of the post being quoted. */
    val quoteSourceId: String? = null,
    /** REPOST only: the member's own words above the quoted post. */
    val quoteWords: String = ""
) {
    /** True for the text-only posts that carry no topic and no card art. */
    val isTextOnly: Boolean get() = kind == KIND_NOTE || kind == KIND_QUOTE || kind == KIND_REPOST
}

/** A topic share card. */
const val KIND_CARD = "CARD"    /** A tweet-style text post — words and nothing else. */
const val KIND_NOTE = "NOTE"

/** The member's words ABOVE somebody else's post, kept in [CommunityCard.quoteWords];
 *  the quoted post itself rides in [CommunityCard.quoteSource]. */
const val KIND_REPOST = "REPOST"

/** A line someone else said, credited to them. */
const val KIND_QUOTE = "QUOTE"

// ── the ban ladder ──────────────────────────────────────────────────────────
// The four tiers a moderator can pick, mirroring `ban_kind` in schema.sql.
// They are strings rather than an enum because the value round-trips through
// the database and an older client must never fail to READ a tier that a newer
// server wrote.

/** Their content is hidden and they cannot post. The classic ban. */
const val BAN_CONTENT = "content"

/** View only: the wall stays readable, but no posting, reactions, friends or
 *  messages — and no profile edits. */
const val BAN_READ_ONLY = "read_only"

/** The wall works; friends, requests and messages are paused. */
const val BAN_SOCIAL = "social"

/** The whole account is locked: hidden content and no write anywhere. */
const val BAN_ACCOUNT = "account"

/** The tiers in the order the picker offers them, mildest first. */
val BAN_TIERS = listOf(BAN_CONTENT, BAN_READ_ONLY, BAN_SOCIAL, BAN_ACCOUNT)

/** What a tier is CALLED — the label its row wears everywhere. */
fun banTierLabel(kind: String): String = when (kind) {
    BAN_CONTENT -> "Hide their content"
    BAN_READ_ONLY -> "View only"
    BAN_SOCIAL -> "Social features"
    BAN_ACCOUNT -> "Account"
    else -> "Not banned"
}

/**
 * One sentence on what a tier actually does, in the member's own terms. Written
 * once and read by the picker, the ban list and the member's own notice, so the
 * promise made when banning is the same sentence they are told afterwards.
 */
fun banTierBlurb(kind: String): String = when (kind) {
    BAN_CONTENT ->
        "Their posts and replies disappear from the wall and they cannot post, " +
            "reply or react. Friends and messages keep working, and it can be lifted."
    BAN_READ_ONLY ->
        "The wall stays up and readable, but they cannot post, reply, react, edit " +
            "their profile or use friends and messages."
    BAN_SOCIAL ->
        "The wall works exactly as before; friends, friend requests and messages " +
            "are paused."
    BAN_ACCOUNT ->
        "The whole account is locked: content hidden, and no post, reply, reaction, " +
            "message or edit is accepted anywhere online."
    else -> "Nothing is paused."
}

/** One member carrying a ban, as the moderation page's ban list reads them. */
data class CommunityBan(
    val userId: String,
    val displayName: String,
    val username: String,
    val avatarStyle: Int,
    /** The tier the ban was set at; blank once it has been lifted. */
    val kind: String,
    val reason: String?,
    val bannedAtMillis: Long,
    /** When a timed ban runs out — null means permanent. */
    val untilMillis: Long?,
    val bannedByName: String,
    /** False for a ban that has lapsed or been lifted: kept as history. */
    val active: Boolean
) {
    val label: String get() = displayName.trim().ifBlank { username.trim() }

    val permanent: Boolean get() = untilMillis == null
}

/** One line of a member's moderation record. */
data class ModerationRecord(
    val id: String,
    /** The `moderation_actions.action` verb, e.g. `ban_social`, `unban`. */
    val action: String,
    val reason: String?,
    val note: String?,
    val createdAtMillis: Long,
    val actorName: String,
    val actorUsername: String
)

/** One reply under a card. Text only, and it dies with the card. */
data class CommunityComment(
    val id: String,
    val authorId: String,
    val authorHandle: String,
    /** The author's CURRENT username (blank when unreadable) — see
     *  [CommunityCard.authorName]. */
    val authorName: String = "",
    /** The author's display name (blank when unreadable or unset). */
    val authorDisplayName: String = "",
    /** The author's chosen portrait (0–15). */
    val authorAvatar: Int = 0,
    val body: String,
    /** The reply this one answers — null at the top level. */
    val parentId: String? = null,
    val createdAtMillis: Long,
    /** Server-stamped the last time the author changed the body (null = never). */
    val editedAtMillis: Long? = null,
    val mine: Boolean,
    /** How many members have hearted this reply. */
    val likes: Int = 0,
    /** True when THIS account's heart is one of them. */
    val likedByMe: Boolean = false
) {
    /** Display name first, live username second, post-time snapshot last. */
    val authorLabel: String
        get() {
            val name = authorDisplayName.trim().removePrefix("@")
            if (name.isNotBlank() && !name.equals("null", true)) return name
            val live = authorName.trim().removePrefix("@")
            return live.ifBlank { authorHandle.trim().removePrefix("@") }
                .ifBlank { "a curious soul" }
        }

    /** The handle printed on the reply's second line, `@` included. */
    val authorHandleLabel: String
        get() {
            val live = authorName.trim().removePrefix("@")
            val snapshot = authorHandle.trim().removePrefix("@")
            return "@" + live.ifBlank { snapshot }.ifBlank { "a curious soul" }
        }
}

/** A community failure whose [message] is already safe to show the user. */
/**
 * A social failure, already worded for the member.
 *
 * [transport] is the important flag for a WRITE: an IOException family failure
 * (a read timeout, a dropped socket) means the outcome is UNKNOWN, not refused
 * - the request may have been delivered and only the answer lost. Callers use
 * it to reconcile instead of telling the member their message never sent.
 */
class CommunityError(message: String, val transport: Boolean = false) : Exception(message)

/** True when the failure says nothing about the OUTCOME, only the wire. */
internal fun Throwable.isTransportFailure(): Boolean = this is java.io.IOException

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
    /** The reply-side heart. Keyed by reply + member — see §4c of the schema. */
    private const val COMMENT_REACTIONS = "/rest/v1/community_comment_reactions"
    private const val REPORTS = "/rest/v1/community_reports"
    private const val ADMINS = "/rest/v1/community_admins"

    /** The moderator row's columns, in one place. */
    private const val ADMIN_COLUMNS =
        "user_id,role,can_delete_posts,can_delete_replies,can_handle_reports," +
            "can_manage_admins,can_ban_members"

    /**
     * The reply SELECT the sheet and the moderation queue share.
     *
     * The embedded `community_comment_reactions` is the reply's own heart (keyed
     * by reply + member) — the same shape as a card's reactions, one level down,
     * counted and "mine" resolved in [parseComments].
     */
    private const val COMMENT_COLUMNS =
        "$COMMENTS?select=id,author,author_handle,body,parent_id,created_at,edited_at," +
            "community_comment_reactions(user_id)"

    /**
     * The same SELECT for a server that predates §4c (no heart table yet).
     *
     * A missing embedded table is a 400 from PostgREST (`PGRST200`), and the
     * thread must not be the heart's hostage — [readComments] falls back to this
     * shape so replies keep working until the schema is re-pasted.
     */
    private const val COMMENT_COLUMNS_NO_HEARTS =
        "$COMMENTS?select=id,author,author_handle,body,parent_id,created_at,edited_at"

    // The moderation doors. Every one of them is a server function: the CHECK
    // of who may do what lives in the database, not in the client.
    private const val RPC_REPORT = "/rest/v1/rpc/curio_file_report"
    private const val RPC_HANDLE_REPORT = "/rest/v1/rpc/curio_handle_report"
    private const val RPC_REMOVE_CARD = "/rest/v1/rpc/curio_moderate_remove_card"
    private const val RPC_REMOVE_COMMENT = "/rest/v1/rpc/curio_moderate_remove_comment"
    private const val RPC_HIDE_MEMBER = "/rest/v1/rpc/curio_moderate_hide_member"
    private const val RPC_BAN_MEMBER = "/rest/v1/rpc/curio_moderate_ban_member"
    private const val RPC_LIFT_BAN = "/rest/v1/rpc/curio_moderate_lift_ban"
    private const val RPC_LIST_BANS = "/rest/v1/rpc/curio_moderate_list_bans"
    private const val RPC_MEMBER_HISTORY = "/rest/v1/rpc/curio_moderate_member_history"
    private const val RPC_SET_ADMIN = "/rest/v1/rpc/curio_set_community_admin"
    private const val RPC_REMOVE_ADMIN = "/rest/v1/rpc/curio_remove_community_admin"

    /**
     * The columns one card needs, including the two embedded children the
     * parser counts (likes and replies). Kept in one place so the feed and the
     * single-card fetch can never drift apart.
     */
    private const val CARD_COLUMNS =
        "id,owner,author_handle,kind,topic_name,category_name,category_glyph,accent_hex," +
            "fact_text,caption,style,aspect,body_scale,byline,created_at,expires_at," +
            "quote_source_id,quote_words," +
            "quote_source:community_cards!community_cards_quote_source_id_fkey(" +
            "id,owner,author_handle,kind,topic_name,category_name,category_glyph,accent_hex," +
            "fact_text,caption,style,aspect,body_scale,byline,created_at,expires_at)," +
            "community_reactions(user_id,kind),community_comments(id)"
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
     * How many replies one thread read asks for. A page, not the whole thread:
     * the request asks for the NEWEST [REPLY_LIMIT] and re-sorts them, so the
     * reply that was just written is always inside the window.
     */
    private const val REPLY_LIMIT = 200

    /**
     * Why this draft cannot be posted, or null when it is fine. Media-backed
     * content never reaches here at all: [CommunityCardDraft] has no field to
     * carry it, which is the whole point of the draft shape.
     */
    fun draftProblem(draft: CommunityCardDraft): String? = when {
        // A text-only post has no topic BY DESIGN (that is the point of it),
        // so the topic requirement applies to topic cards alone.
        draft.kind == KIND_CARD && draft.topicName.isBlank() -> "What is your card about?"
        draft.kind == KIND_REPOST && draft.quoteSourceId.isNullOrBlank() ->
            "The post you were quoting is gone."
        draft.factText.isBlank() -> when (draft.kind) {
            KIND_QUOTE -> "Write the quote first."
            KIND_NOTE -> "Write something first."
            KIND_REPOST -> "Add your words above the post you're quoting."
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

    /**
     * The replies under a card, oldest first (a conversation reads downwards).
     *
     * The read asks for the NEWEST page and re-orders it in memory. `order=asc`
     * with a limit returns the OLDEST replies, so on a busy card a reply that
     * had just been written fell off the end of the page — the thread looked
     * like it had swallowed it, which is the "my new reply vanished" report.
     * Newest-first always contains the one every reader is looking for, and the
     * sort restores reading order for the sheet.
     */
    suspend fun comments(
        accessToken: String,
        cardId: String,
        myUserId: String?
    ): Result<List<CommunityComment>> = withContext(Dispatchers.IO) {
        val parsed = mapped { readComments(accessToken, cardId, myUserId, withHearts = true) }
        // A server that predates §4c refuses the embedded heart table, so the
        // read is tried once more WITHOUT it: the thread keeps working (and the
        // failures it reports stay the first, honest one) until the schema is
        // re-pasted, after which the hearts appear by themselves.
        val replies = parsed.getOrNull()
            ?: mapped { readComments(accessToken, cardId, myUserId, withHearts = false) }
                .getOrNull()
            ?: return@withContext parsed.asFailure()
        // A reply shows the author's CURRENT name and portrait too, so a
        // rename never leaves an old handle stranded in a thread.
        Result.success(withCommentAuthors(accessToken, replies))
    }

    /**
     * One blocking reply read — the newest [REPLY_LIMIT], oldest first.
     *
     * Blocking (not `suspend`) on purpose: it is called from inside [mapped],
     * which is a plain function, and this layer's HTTP calls are synchronous
     * calls on an IO dispatcher rather than suspend points.
     */
    private fun readComments(
        accessToken: String,
        cardId: String,
        myUserId: String?,
        withHearts: Boolean
    ): List<CommunityComment> {
        val columns = if (withHearts) COMMENT_COLUMNS else COMMENT_COLUMNS_NO_HEARTS
        val path = "$columns&card_id=eq.$cardId&order=created_at.desc&limit=$REPLY_LIMIT"
        val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
        return parseComments(SupabaseClient.executeBody(request), myUserId)
            .sortedBy { it.createdAtMillis }
    }

    /**
     * Posts a reply and hands back the row the server stored.
     *
     * `return=representation` is what lets the sheet show the reply the moment
     * Send is released — with the id the server assigned and the branch it was
     * posted into — instead of waiting for a whole thread read to come back to
     * prove it exists. That read is what made a just-written reply appear late
     * (or not at all, before the page-window fix above).
     */
    suspend fun comment(
        accessToken: String,
        cardId: String,
        body: String,
        handle: String,
        /** The reply this answers, for a branched thread — null at the top. */
        parentId: String? = null,
        /** Only used to mark the returned row as MINE. */
        myUserId: String? = null
    ): Result<CommunityComment> = withContext(Dispatchers.IO) {
        mapped {
            val text = body.trim()
            if (text.isEmpty()) throw IllegalArgumentException("Write something first.")
            if (text.length > MAX_COMMENT_CHARS) {
                throw IllegalArgumentException(
                    "Keep it under $MAX_COMMENT_CHARS characters (it's ${text.length})."
                )
            }
            // v3xx53 — the app-level filter. It runs HERE, on the way out, so
            // no screen can post around it; the same fold is a CHECK in
            // supabase/schema.sql, so a modified client can't either.
            CurioContentFilter.problem(text)?.let { throw IllegalArgumentException(it) }
            val payload = JSONObject()
                .put("card_id", cardId)
                .put("body", text)
                .put("author_handle", handle.trim().ifBlank { "A curious soul" })
            // Absent rather than null: PostgREST treats a JSON null as an
            // explicit NULL only if the column is nullable, and omitting it
            // keeps a top-level reply from touching the branch column at all.
            parentId?.takeIf { it.isNotBlank() }?.let { payload.put("parent_id", it) }
            val request = SupabaseClient.requestBuilder(COMMENTS, accessToken)
                .header("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            parseComments(SupabaseClient.executeBody(request), myUserId).firstOrNull()
                ?: throw CommunityError("The reply could not be posted.")
        }
    }

    /**
     * Hearts one reply. Idempotent — hearting twice leaves one heart.
     *
     * Its own table (§4c), because a card's reactions are keyed by card + member
     * and cannot carry a per-reply heart.
     */
    suspend fun likeComment(
        accessToken: String,
        commentId: String,
        myUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val payload = JSONObject()
                .put("comment_id", commentId)
                .put("user_id", myUserId)
            val request = SupabaseClient.requestBuilder(
                "$COMMENT_REACTIONS?on_conflict=comment_id%2Cuser_id",
                accessToken
            )
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** Takes MY heart back off a reply. */
    suspend fun unlikeComment(
        accessToken: String,
        commentId: String,
        myUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val request = SupabaseClient.requestBuilder(
                "$COMMENT_REACTIONS?comment_id=eq.$commentId&user_id=eq.$myUserId",
                accessToken
            ).delete().build()
            SupabaseClient.executeBody(request)
        }
    }

    /**
     * Edits one of MY replies, through the server function.
     *
     * A PUT on a filtered table route goes through PostgREST's upsert path,
     * which is what answered "column pgrst_body.id does not exist" every time
     * a reply was edited. `curio_edit_comment` checks the author, re-runs the
     * public-text rules, stamps `edited_at` server-side and either writes the
     * row or raises — an edit can no longer half-happen behind a 204. The
     * checks below stay, because the API layer is the app's own last door.
     */
    suspend fun editComment(accessToken: String, commentId: String, body: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val text = body.trim()
                if (text.isEmpty()) throw IllegalArgumentException("Write something first.")
                if (text.length > MAX_COMMENT_CHARS) {
                    throw IllegalArgumentException(
                        "Keep it under $MAX_COMMENT_CHARS characters (it's ${text.length})."
                    )
                }
                CurioContentFilter.problem(text)?.let { throw IllegalArgumentException(it) }
                val payload = JSONObject()
                    .put("p_comment_id", commentId)
                    .put("p_body", text)
                val request = SupabaseClient
                    .requestBuilder("/rest/v1/rpc/curio_edit_comment", accessToken)
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

    /**
     * Posts a card and hands back the row the server stored.
     *
     * `return=representation` is what lets the wall put the card up the moment
     * the sheet closes: the poster sees their own post instantly, wearing the
     * id the server assigned, instead of waiting for a whole feed read to come
     * back and find it. The owner column and the 24-hour expiry are set by the
     * DB, so the returned row is the only place those exist before a refresh.
     */
    suspend fun post(
        accessToken: String,
        draft: CommunityCardDraft,
        handle: String,
        myUserId: String? = null
    ): Result<CommunityCard> = withContext(Dispatchers.IO) {
        mapped {
            draftProblem(draft)?.let { throw IllegalArgumentException(it) }
            // v3xx53 — the app-level filter (see CommunityApi.comment): the
            // words a person typed, plus the handle and byline that ride with
            // them.
            CurioContentFilter.problemIn(draft.factText, draft.caption, draft.byline, handle)
                ?.let { throw IllegalArgumentException(it) }
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
            if (draft.kind == KIND_REPOST) {
                payload.put("quote_source_id", draft.quoteSourceId)
                payload.put("quote_words", draft.quoteWords.trim())
            }
            val request = SupabaseClient.requestBuilder(CARDS, accessToken)
                .header("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            val body = SupabaseClient.executeBody(request)
            parseCards(body, myUserId).firstOrNull()
                ?: throw CommunityError("The card could not be posted.")
        }
    }

    /** Idempotent like — reacting twice leaves one reaction. */
    suspend fun like(accessToken: String, cardId: String, myUserId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val payload = JSONObject()
                    .put("card_id", cardId)
                    .put("user_id", myUserId)
                    .put("kind", "like")
                val request = SupabaseClient.requestBuilder("$REACTIONS?on_conflict=card_id%2Cuser_id", accessToken)
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

    suspend fun dislike(accessToken: String, cardId: String, myUserId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val payload = JSONObject()
                    .put("card_id", cardId)
                    .put("user_id", myUserId)
                    .put("kind", "dislike")
                val request = SupabaseClient.requestBuilder("$REACTIONS?on_conflict=card_id%2Cuser_id", accessToken)
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    suspend fun undislike(accessToken: String, cardId: String, myUserId: String): Result<Unit> =
        unlike(accessToken, cardId, myUserId)

    /**
     * Files a report against a CARD, a REPLY or a MEMBER.
     *
     * Through the server function, which keeps ONE row per reporter per target
     * and REFRESHES it when the same thing is reported again. The old table
     * route hit the (card_id, reporter) unique constraint, so a second report
     * answered with an error instead of being filed — that is the "it doesn't
     * let me report again" bug.
     */
    suspend fun report(
        accessToken: String,
        kind: String,
        targetId: String,
        reason: String,
        note: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val payload = JSONObject()
                .put("p_kind", kind)
                .put("p_target", targetId)
                .put("p_reason", reason)
            note?.trim()?.takeIf { it.isNotEmpty() }?.let { payload.put("p_note", it) }
            val request = SupabaseClient.requestBuilder(RPC_REPORT, accessToken)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** Reads the moderation queue, newest first. RLS exposes it to handlers only. */
    suspend fun reported(accessToken: String): Result<List<CommunityReport>> =
        withContext(Dispatchers.IO) {
            mapped {
                val request = SupabaseClient.requestBuilder(
                    "$REPORTS?select=id,card_id,comment_id,target_user,reporter,reason,note," +
                        "status,resolution,created_at,updated_at&order=created_at.desc&limit=120",
                    accessToken
                ).get().build()
                val rows = JSONArray(SupabaseClient.executeBody(request))
                buildList(rows.length()) {
                    for (index in 0 until rows.length()) {
                        val row = rows.optJSONObject(index) ?: continue
                        add(CommunityReport(
                            id = row.optString("id"),
                            cardId = row.optString("card_id")
                                .takeIf { it.isNotBlank() && it != "null" },
                            commentId = row.optString("comment_id")
                                .takeIf { it.isNotBlank() && it != "null" },
                            targetUserId = row.optString("target_user")
                                .takeIf { it.isNotBlank() && it != "null" },
                            reporterId = row.optString("reporter"),
                            reason = row.optString("reason"),
                            note = row.optString("note").takeIf { it.isNotBlank() && it != "null" },
                            status = row.optString("status").ifBlank { "open" },
                            resolution = row.optString("resolution")
                                .takeIf { it.isNotBlank() && it != "null" },
                            createdAtMillis = epochMillis(row.optString("created_at")),
                            updatedAtMillis = epochMillis(row.optString("updated_at"))
                        ))
                    }
                }
            }
        }

    /**
     * Cards by id, for the moderation queue. The admin read policy keeps a
     * reported card readable even after its own 24 hours are up, which is
     * usually exactly why it was reported.
     */
    suspend fun cardsByIds(
        accessToken: String,
        ids: List<String>,
        myUserId: String?
    ): Result<List<CommunityCard>> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext Result.success(emptyList())
        val parsed = mapped {
            val path = "$CARDS?select=$CARD_COLUMNS&id=in.(${ids.distinct().joinToString(",")})&limit=100"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            parseCards(SupabaseClient.executeBody(request), myUserId)
        }
        val cards = parsed.getOrNull() ?: return@withContext parsed.asFailure()
        Result.success(withAuthors(accessToken, cards))
    }

    /** Replies by id — the queue's other content kind. */
    suspend fun commentsByIds(
        accessToken: String,
        ids: List<String>,
        myUserId: String?
    ): Result<List<CommunityComment>> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext Result.success(emptyList())
        val parsed = mapped {
            val path = "$COMMENT_COLUMNS&id=in.(${ids.distinct().joinToString(",")})&limit=100"
            val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
            parseComments(SupabaseClient.executeBody(request), myUserId)
        }
        val replies = parsed.getOrNull() ?: return@withContext parsed.asFailure()
        Result.success(withCommentAuthors(accessToken, replies))
    }

    /**
     * MY OWN moderation row — null for a member who is not on the team. The
     * UI reads this to decide which tools to offer; every action is checked
     * again in the database.
     */
    suspend fun myAdminRow(accessToken: String, userId: String): Result<CommunityAdminRow?> =
        withContext(Dispatchers.IO) {
            mapped {
                val path = "$ADMINS?select=$ADMIN_COLUMNS&user_id=eq.$userId&limit=1"
                val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                parseAdmins(SupabaseClient.executeBody(request)).firstOrNull()
            }
        }

    /** The whole moderation team. RLS exposes the list to the team itself. */
    suspend fun admins(accessToken: String): Result<List<CommunityAdminRow>> =
        withContext(Dispatchers.IO) {
            mapped {
                val path = "$ADMINS?select=$ADMIN_COLUMNS&order=created_at.asc&limit=100"
                val request = SupabaseClient.requestBuilder(path, accessToken).get().build()
                parseAdmins(SupabaseClient.executeBody(request))
            }
        }

    /** Grants or edits one admin. Only a manager may call it; the server agrees. */
    suspend fun setAdmin(
        accessToken: String,
        userId: String,
        role: String,
        canDeletePosts: Boolean,
        canDeleteReplies: Boolean,
        canHandleReports: Boolean,
        canManageAdmins: Boolean,
        canBanMembers: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val payload = JSONObject()
                .put("p_user_id", userId)
                .put("p_role", role)
                .put("p_can_delete_posts", canDeletePosts)
                .put("p_can_delete_replies", canDeleteReplies)
                .put("p_can_handle_reports", canHandleReports)
                .put("p_can_manage_admins", canManageAdmins)
                .put("p_can_ban_members", canBanMembers)
            val request = SupabaseClient.requestBuilder(RPC_SET_ADMIN, accessToken)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** Takes one admin off the team. The owner can never be removed. */
    suspend fun removeAdmin(accessToken: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val payload = JSONObject().put("p_user_id", userId)
                val request = SupabaseClient.requestBuilder(RPC_REMOVE_ADMIN, accessToken)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    /**
     * Works one queue row: `dismiss`, `remove_content`, `hide_author` or
     * `reopen`. The reason is required by the server for anything that acts on
     * content or a member.
     */
    suspend fun handleReport(
        accessToken: String,
        reportId: String,
        action: String,
        reason: String? = null,
        note: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val payload = JSONObject()
                .put("p_report", reportId)
                .put("p_action", action)
            reason?.trim()?.takeIf { it.isNotEmpty() }?.let { payload.put("p_reason", it) }
            note?.trim()?.takeIf { it.isNotEmpty() }?.let { payload.put("p_note", it) }
            val request = SupabaseClient.requestBuilder(RPC_HANDLE_REPORT, accessToken)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** Removes one post with a reason, from the card page or the queue. */
    suspend fun removeCardWithReason(
        accessToken: String,
        cardId: String,
        reason: String,
        note: String? = null
    ): Result<Unit> = moderate(RPC_REMOVE_CARD, mapOf("p_card_id" to cardId, "p_reason" to reason, "p_note" to note), accessToken)

    /** Removes one reply with a reason. */
    suspend fun removeCommentWithReason(
        accessToken: String,
        commentId: String,
        reason: String,
        note: String? = null
    ): Result<Unit> = moderate(RPC_REMOVE_COMMENT, mapOf("p_comment_id" to commentId, "p_reason" to reason, "p_note" to note), accessToken)

    /** Hides or restores a member. A ban never touches their account. */
    suspend fun hideMember(
        accessToken: String,
        userId: String,
        hidden: Boolean,
        reason: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val payload = JSONObject()
                .put("p_user_id", userId)
                .put("p_hidden", hidden)
            reason?.trim()?.takeIf { it.isNotEmpty() }?.let { payload.put("p_reason", it) }
            val request = SupabaseClient.requestBuilder(RPC_HIDE_MEMBER, accessToken)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /**
     * Bans a member at ONE tier, for `hours` or for good (null = permanent).
     *
     * The server validates the tier, refuses a ban without a reason, refuses a
     * self-ban and refuses the community's owner — the client's own copy of
     * those rules only decides what to SHOW.
     */
    suspend fun banMember(
        accessToken: String,
        userId: String,
        kind: String,
        reason: String,
        hours: Int? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val payload = JSONObject()
                .put("p_user_id", userId)
                .put("p_kind", kind)
                .put("p_reason", reason.trim())
            if (hours != null && hours > 0) payload.put("p_hours", hours)
            val request = SupabaseClient.requestBuilder(RPC_BAN_MEMBER, accessToken)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** Lifts whatever tier is in force on a member. */
    suspend fun liftBan(
        accessToken: String,
        userId: String,
        note: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val payload = JSONObject().put("p_user_id", userId)
            note?.trim()?.takeIf { it.isNotEmpty() }?.let { payload.put("p_note", it) }
            val request = SupabaseClient.requestBuilder(RPC_LIFT_BAN, accessToken)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /**
     * The ban list: live bans first, then the ones that lapsed or were lifted.
     * Only a moderator with the 'bans' permission is answered at all.
     */
    suspend fun bans(accessToken: String): Result<List<CommunityBan>> =
        withContext(Dispatchers.IO) {
            mapped {
                val request = SupabaseClient.requestBuilder(RPC_LIST_BANS, accessToken)
                    .post("{}".toRequestBody(jsonMediaType))
                    .build()
                val rows = JSONArray(SupabaseClient.executeBody(request))
                buildList(rows.length()) {
                    for (index in 0 until rows.length()) {
                        val row = rows.optJSONObject(index) ?: continue
                        val until = row.optString("banned_until")
                            .takeIf { it.isNotBlank() && it != "null" }
                            ?.let { epochMillis(it) }
                        add(
                            CommunityBan(
                                userId = row.optString("user_id"),
                                displayName = row.optString("display_name")
                                    .takeIf { it != "null" }.orEmpty(),
                                username = row.optString("username")
                                    .takeIf { it != "null" }.orEmpty(),
                                avatarStyle = row.optInt("avatar_style", 0),
                                kind = row.optString("kind").takeIf { it != "null" }.orEmpty(),
                                reason = row.optString("reason")
                                    .takeIf { it.isNotBlank() && it != "null" },
                                bannedAtMillis = epochMillis(row.optString("banned_at")),
                                untilMillis = until,
                                bannedByName = row.optString("banned_by_name")
                                    .takeIf { it != "null" }.orEmpty(),
                                active = row.optBoolean("active", false)
                            )
                        )
                    }
                }
            }
        }

    /**
     * One member's moderation record, newest first. Any signed-in member can
     * read their OWN; the team can read anyone's.
     */
    suspend fun memberHistory(
        accessToken: String,
        userId: String
    ): Result<List<ModerationRecord>> = withContext(Dispatchers.IO) {
        mapped {
            val request = SupabaseClient.requestBuilder(RPC_MEMBER_HISTORY, accessToken)
                .post(JSONObject().put("p_user_id", userId).toString().toRequestBody(jsonMediaType))
                .build()
            val rows = JSONArray(SupabaseClient.executeBody(request))
            buildList(rows.length()) {
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    add(
                        ModerationRecord(
                            id = row.optString("id"),
                            action = row.optString("action"),
                            reason = row.optString("reason")
                                .takeIf { it.isNotBlank() && it != "null" },
                            note = row.optString("note")
                                .takeIf { it.isNotBlank() && it != "null" },
                            createdAtMillis = epochMillis(row.optString("created_at")),
                            actorName = row.optString("actor_name")
                                .takeIf { it != "null" }.orEmpty(),
                            actorUsername = row.optString("actor_username")
                                .takeIf { it != "null" }.orEmpty()
                        )
                    )
                }
            }
        }
    }

    /** The follow table, addressed directly (RLS owns the rules server-side). */
    private const val FOLLOWS = "/rest/v1/member_follows"

    /**
     * FOLLOWS. A Follow is one row; an unfollow deletes it; `followingIds` is
     * the one read the wall's Following filter needs, and `followerCounts`
     * covers a profile's follower/following numbers in a single request pair.
     * Every rule (no self-follow, no following someone who blocked you, ban
     * tiers) is enforced by RLS and the ban guard — the client only decides
     * what to OFFER.
     */
    suspend fun follow(accessToken: String, userId: String, myUserId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val request = SupabaseClient.requestBuilder(FOLLOWS, accessToken)
                    .post(
                        JSONObject()
                            .put("follower", myUserId)
                            .put("followed", userId)
                            .toString().toRequestBody(jsonMediaType)
                    )
                    .build()
                SupabaseClient.executeBody(request)
            }
        }

    suspend fun unfollow(accessToken: String, userId: String, myUserId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mappedUnit {
                val request = SupabaseClient.requestBuilder(
                    "$FOLLOWS?follower=eq.$myUserId&followed=eq.$userId", accessToken
                ).delete().build()
                SupabaseClient.executeBody(request)
            }
        }

    /** The ids this account follows — the wall's Following filter reads it. */
    suspend fun followingIds(accessToken: String, myUserId: String): Result<Set<String>> =
        withContext(Dispatchers.IO) {
            mapped {
                val request = SupabaseClient.requestBuilder(
                    "$FOLLOWS?follower=eq.$myUserId&select=followed", accessToken
                ).get().build()
                val array = JSONArray(SupabaseClient.executeBody(request))
                buildSet(array.length()) {
                    for (i in 0 until array.length()) {
                        array.optJSONObject(i)?.optString("followed")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { add(it) }
                    }
                }
            }
        }

    /** How many members follow this account — one row per follower. */
    suspend fun followerCount(accessToken: String, userId: String): Result<Int> =
        withContext(Dispatchers.IO) {
            mapped {
                val request = SupabaseClient.requestBuilder(
                    "$FOLLOWS?followed=eq.$userId&select=follower", accessToken
                ).get().build()
                JSONArray(SupabaseClient.executeBody(request)).length()
            }
        }

    /** Shared shape for the two removal calls: same payload rules, same errors. */
    private suspend fun moderate(
        rpc: String,
        values: Map<String, String?>,
        accessToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mappedUnit {
            val payload = JSONObject()
            values.forEach { (key, value) ->
                value?.trim()?.takeIf { it.isNotEmpty() }?.let { payload.put(key, it) }
            }
            val request = SupabaseClient.requestBuilder(rpc, accessToken)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            SupabaseClient.executeBody(request)
        }
    }

    /** True when I am anywhere on the team (queue access, admin row visible). */
    suspend fun isAdmin(accessToken: String, userId: String): Result<Boolean> =
        myAdminRow(accessToken, userId).map { it != null }

    /** Admin-only deletion; the database function/policy is the final authority. */
    suspend fun deleteAny(accessToken: String, cardId: String): Result<Unit> =
        delete(accessToken, cardId)

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

    // ── internals ──────���─────────────────────────────────────────────────

    /** One reply row, parsed in exactly one place (sheet and queue share it). */
    private fun parseComments(body: String, myUserId: String?): List<CommunityComment> {
        val array = JSONArray(body)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val row = array.optJSONObject(index) ?: continue
                // The reply's hearts, embedded in the same read: how many there
                // are, and whether one of them is mine.
                val hearts = row.optJSONArray("community_comment_reactions")
                var mineHeart = false
                if (hearts != null && myUserId != null) {
                    for (heart in 0 until hearts.length()) {
                        if (hearts.optJSONObject(heart)?.optString("user_id") == myUserId) {
                            mineHeart = true
                            break
                        }
                    }
                }
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
                        editedAtMillis = row.optString("edited_at")
                            .takeIf { it.isNotBlank() && it != "null" }
                            ?.let(::epochMillis),
                        mine = myUserId != null && row.optString("author") == myUserId,
                        likes = hearts?.length() ?: 0,
                        likedByMe = mineHeart
                    )
                )
            }
        }
    }

    /** One moderator row. */
    private fun parseAdmins(body: String): List<CommunityAdminRow> {
        val array = JSONArray(body)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val row = array.optJSONObject(index) ?: continue
                add(
                    CommunityAdminRow(
                        userId = row.optString("user_id"),
                        role = row.optString("role").ifBlank { "admin" },
                        canDeletePosts = row.optBoolean("can_delete_posts", true),
                        canDeleteReplies = row.optBoolean("can_delete_replies", true),
                        canHandleReports = row.optBoolean("can_handle_reports", true),
                        canManageAdmins = row.optBoolean("can_manage_admins", false),
                        canBanMembers = row.optBoolean("can_ban_members", false)
                    )
                )
            }
        }
    }

    private fun <T> mapped(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (failure: Throwable) {
        Result.failure(
            CommunityError(communityMessage(failure), transport = failure.isTransportFailure())
        )
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
            parseOneCard(row, myUserId)?.let { cards += it }
        }
        return cards
    }

    /** One REST row → one card. Null when the row is not a card at all. */
    private fun parseOneCard(row: JSONObject, myUserId: String?): CommunityCard? {
            val reactions = row.optJSONArray("community_reactions")
  var likes = 0
  var dislikes = 0
  var likedByMe = false
  var dislikedByMe = false
  if (reactions != null) {
  for (r in 0 until reactions.length()) {
  val reaction = reactions.optJSONObject(r) ?: continue
  val kind = reaction.optString("kind", "like")
  if (kind == "dislike") dislikes++ else likes++
  if (myUserId != null && reaction.optString("user_id") == myUserId) {
  if (kind == "dislike") dislikedByMe = true else likedByMe = true
  }
  }
  }
            return CommunityCard(
                id = row.optString("id"),
                authorId = row.optString("owner"),
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
                dislikeCount = dislikes,
                dislikedByMe = dislikedByMe,
                commentCount = row.optJSONArray("community_comments")?.length() ?: 0,
                mine = myUserId != null && row.optString("owner") == myUserId,
                quoteSourceId = row.optString("quote_source_id")
                    .takeIf { it.isNotBlank() && it != "null" },
                quoteWords = row.optString("quote_words"),
                quoteSource = row.optJSONObject("quote_source")?.let { q ->
                    // The quoted post is parsed with an EMPTY viewer id: its
                    // reaction counts arrive with the join but the viewer's
                    // own state belongs to the outer row's reader, and a
                    // nested parse with `myUserId` would claim reactions
                    // that were never this member's.
                    parseOneCard(q, myUserId = null)
                }
            )
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
            card.copy(
                authorName = person.username,
                authorDisplayName = person.displayName,
                authorAvatar = person.avatarStyle
            )
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
            reply.copy(
                authorName = person.username,
                authorDisplayName = person.displayName,
                authorAvatar = person.avatarStyle
            )
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
        // CONDITIONS first, then the pass-through of text that is already
        // written for the member. A stale session and a missing server
        // function arrive as plain IllegalStateExceptions, so before this
        // order was fixed the screen rendered the server's own words for both:
        // "JWT expired" naming a mechanism, and "Could not find the function
        // public.curio_edit_dm_message(...)" naming an internal it was told to
        // paste. Each one has an action attached now.
        raw.contains("jwt", true) || raw.contains("token is expired", true) ||
            raw.contains("invalid claim", true) ->
            "That session has expired. Reopen Curio, or sign in again in Settings → Online mode."
        // The server is older than the app. A function or a TABLE this build
        // needs may simply not be there yet — a new table arrives as
        // PostgREST's PGRST205 ("could not find the table") and a new embedded
        // relationship as PGRST200 ("could not find a relationship"), and
        // either one used to surface as PostgREST's own internal sentence.
        raw.contains("could not find the function", true) ||
            raw.contains("permission denied for function", true) ||
            raw.contains("could not find the table", true) ||
            raw.contains("could not find a relationship", true) ||
            raw.contains("pgrst200", true) || raw.contains("pgrst205", true) ->
            "Curio needs its server update. Paste supabase/schema.sql, then try again."
        failure is IllegalArgumentException && raw.isNotBlank() -> raw
        failure is IllegalStateException && raw.isNotBlank() -> raw
        raw.contains("rate limit", true) || raw.contains("too many", true) ->
            "Too much just now — try again in a minute."
        // A unique-index collision arrives as a 23505 duplicate-key error, and
        // whether the server names the column, the index or neither depends on
        // how the constraint was created — so the identity-shaped answer is
        // chosen whenever the duplicate could only BE an identity, and the
        // plain "already done that" is kept for everything else.
        (raw.contains("duplicate key", true) || raw.contains("23505", true)) &&
            (raw.contains("username", true) || raw.contains("profiles", true) ||
                raw.contains("already exists", true)) ->
            "That username is already taken. Try another one."
        raw.contains("duplicate key", true) || raw.contains("23505", true) ->
            "You've already done that."
        raw.contains("row-level security", true) ->
            "Turn Online mode on (Settings → Online mode) and try again."
        raw.contains("unable to resolve host", true) ||
            raw.contains("failed to connect", true) ||
            raw.contains("timeout", true) ->
            "No connection — check your network and try again."
        else -> "Something went wrong. Try again in a moment."
    }
}
