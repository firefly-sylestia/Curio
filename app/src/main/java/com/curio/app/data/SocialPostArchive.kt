package com.curio.app.data

import android.content.Context
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityCardDraft
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.KIND_CARD
import com.curio.app.data.supabase.KIND_NOTE
import com.curio.app.data.supabase.KIND_QUOTE
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * THE COMPOSER'S OWN MEMORY — the drafts you have not posted yet, and the posts
 * you took down.
 *
 * Both are things the SERVER must never hold: an unfinished sentence is not a
 * post, and a deleted post is exactly what the wall was asked to stop showing.
 * So this is a single small JSON file in the app's own files dir, written
 * best-effort like every other local store in Curio — losing it costs a draft,
 * never an account.
 *
 * Two kinds of entry live here because they are the same need seen twice:
 *
 *  - **A DRAFT** per composer kind (a card, a note, a quote — one each). Leaving
 *    the composer mid-sentence used to destroy the sentence: closing the sheet,
 *    an app switch that the system reclaimed, a stray back tap. Writing on a
 *    phone happens in interruptions, so the draft is kept as you type and put
 *    back when you return.
 *  - **A DELETED POST**, kept whole — the same draft shape the composer posts,
 *    so one tap puts it back on the wall. Deleting your own post was final
 *    before this: the row left the server, the text left the phone, and there
 *    was no undo even a second later. The text is the member's own, so it stays
 *    with them; it is never re-uploaded unless they ask for it.
 *
 * Nothing here is part of the online layer, nothing is synced, and sign-out
 * clears the lot — the next account on the device must not read the previous
 * one's unfinished words.
 */
object SocialPostArchive {

    /** An unfinished composer, one per [kind]. */
    data class Draft(
        val kind: String,
        val text: String = "",
        val caption: String = "",
        val credit: String = "",
        /** The chosen topic's id, "" when the draft never picked one. */
        val topicId: String = "",
        val savedAtMillis: Long = 0L
    ) {
        /** True when there is nothing worth keeping. */
        val isBlank: Boolean
            get() = text.isBlank() && caption.isBlank() && credit.isBlank() && topicId.isBlank()
    }

    /** A post the member deleted, kept on the device so it can go back up. */
    data class DeletedPost(
        /** The id the post had on the server (used to dedupe and to forget). */
        val id: String,
        /** What the archive row is titled: the topic, or the first few words. */
        val title: String,
        /** Exactly what the composer would post — a re-post is one tap. */
        val draft: CommunityCardDraft,
        val postedAtMillis: Long,
        val deletedAtMillis: Long
    )

    /** How many deleted posts one device keeps before the oldest is dropped. */
    private const val DELETED_CAP = 25

    private const val FILE_NAME = "curio_post_archive.json"
    private const val VERSION = 1

    /**
     * A warm copy per file, keyed by the file's path: the composer reads a draft
     * on every kind switch, and re-parsing a file per keystroke would be work
     * for nothing.
     */
    private val memory = ConcurrentHashMap<String, JSONObject>()

    private fun fileOf(context: Context) =
        File(context.applicationContext.filesDir, FILE_NAME)

    private fun root(context: Context): JSONObject {
        val path = fileOf(context).absolutePath
        memory[path]?.let { return it }
        val loaded = runCatching {
            val file = fileOf(context)
            if (file.isFile) JSONObject(file.readText()) else JSONObject()
        }.getOrDefault(JSONObject())
        val usable = if (loaded.optInt("v", 0) == VERSION) loaded else JSONObject()
        memory[path] = usable
        return usable
    }

    private fun save(context: Context, root: JSONObject) {
        runCatching {
            root.put("v", VERSION)
            memory[fileOf(context).absolutePath] = root
            fileOf(context).writeText(root.toString())
        }
    }

    // ── drafts ──────────────────────────────────────────────────────────────

    /** The draft kept for [kind], or null when there is none. */
    fun draft(context: Context, kind: String): Draft? = runCatching {
        val row = root(context).optJSONObject("drafts")?.optJSONObject(key(kind)) ?: return null
        Draft(
            kind = kind,
            text = row.optString("text"),
            caption = row.optString("caption"),
            credit = row.optString("credit"),
            topicId = row.optString("topic"),
            savedAtMillis = row.optLong("at", 0L)
        ).takeIf { !it.isBlank }
    }.getOrNull()

    /** Keeps the draft. A blank draft CLEARS the entry instead. */
    fun saveDraft(context: Context, draft: Draft) {
        runCatching {
            val root = root(context)
            val drafts = root.optJSONObject("drafts") ?: JSONObject()
            if (draft.isBlank) {
                drafts.remove(key(draft.kind))
            } else {
                drafts.put(
                    key(draft.kind),
                    JSONObject()
                        .put("text", draft.text)
                        .put("caption", draft.caption)
                        .put("credit", draft.credit)
                        .put("topic", draft.topicId)
                        .put("at", System.currentTimeMillis())
                )
            }
            root.put("drafts", drafts)
            save(context, root)
        }
    }

    /** Forgets the draft for [kind] — the composer calls this once it is posted. */
    fun clearDraft(context: Context, kind: String) {
        runCatching {
            val root = root(context)
            val drafts = root.optJSONObject("drafts") ?: return
            drafts.remove(key(kind))
            root.put("drafts", drafts)
            save(context, root)
        }
    }

    // ── deleted posts ───────────────────────────────────────────────────────

    /** Everything kept, newest first. */
    fun deleted(context: Context): List<DeletedPost> = runCatching {
        val array = root(context).optJSONArray("deleted") ?: return emptyList()
        buildList(array.length()) {
            for (index in 0 until array.length()) {
                val row = array.optJSONObject(index) ?: continue
                val draft = row.optJSONObject("draft")?.toDraft() ?: continue
                add(
                    DeletedPost(
                        id = row.optString("id"),
                        title = row.optString("title"),
                        draft = draft,
                        postedAtMillis = row.optLong("posted", 0L),
                        deletedAtMillis = row.optLong("at", 0L)
                    )
                )
            }
        }.sortedByDescending { it.deletedAtMillis }
    }.getOrDefault(emptyList())

    /**
     * Keeps one deleted post. The cap is small on purpose: this is an undo, not
     * an archive, and the newest [DELETED_CAP] are more than anybody reaches back
     * through.
     */
    fun rememberDeleted(context: Context, post: DeletedPost) {
        runCatching {
            val root = root(context)
            val kept = (root.optJSONArray("deleted") ?: JSONArray())
            val draftJson = draftJson(post.draft)
            val title = post.title.ifBlank { titleFor(post.draft) }
            val fresh = JSONArray()
            // Re-deleting the same post replaces its entry instead of stacking
            // duplicates, and the newest entries lead.
            fresh.put(
                JSONObject()
                    .put("id", post.id)
                    .put("title", title)
                    .put("draft", draftJson)
                    .put("posted", post.postedAtMillis)
                    .put("at", post.deletedAtMillis)
            )
            for (index in 0 until kept.length()) {
                val row = kept.optJSONObject(index) ?: continue
                if (row.optString("id") == post.id) continue
                if (fresh.length() >= DELETED_CAP) break
                fresh.put(row)
            }
            root.put("deleted", fresh)
            save(context, root)
        }
    }

    /** Drops one kept post — after it has been posted again, or forgotten. */
    fun forgetDeleted(context: Context, id: String) {
        runCatching {
            val root = root(context)
            val kept = root.optJSONArray("deleted") ?: return
            val fresh = JSONArray()
            for (index in 0 until kept.length()) {
                val row = kept.optJSONObject(index) ?: continue
                if (row.optString("id") == id) continue
                fresh.put(row)
            }
            root.put("deleted", fresh)
            save(context, root)
        }
    }

    /** Forgets EVERYTHING — sign-out, like every other local social store. */
    fun clear(context: Context) {
        memory.clear()
        runCatching { fileOf(context).delete() }
    }

    // ── wiring helpers ──────────────────────────────────────────────────────

    /**
     * One of MY OWN posts, as the draft that would post it again.
     *
     * A card does not carry its lane's slug (the wall never needs it), so it is
     * resolved from the lane table by display name — and a post whose lane
     * cannot be resolved keeps an empty slug rather than a wrong one, which the
     * server accepts exactly like the composer's own topic post.
     */
    fun draftOf(card: CommunityCard): CommunityCardDraft = CommunityCardDraft(
        topicName = card.topicName,
        categoryName = card.categoryName,
        categorySlug = card.categoryName?.let { name ->
            CurioCategories.visible.firstOrNull { it.displayName == name }?.id?.routeSlug
        }.orEmpty(),
        categoryGlyph = card.categoryGlyph,
        accentHex = card.accentHex,
        factText = card.factText,
        caption = card.caption,
        kind = card.kind,
        style = card.style,
        aspect = card.aspect,
        bodyScale = card.bodyScale,
        byline = card.byline
    )

    /**
     * The draft JSON for one post, so a deleted post is stored in exactly the
     * shape the composer posts — the re-post path can then never drift from the
     * post path.
     */
    fun draftJson(draft: CommunityCardDraft): JSONObject = JSONObject()
        .put("topic", draft.topicName)
        .put("category", draft.categoryName)
        .put("slug", draft.categorySlug)
        .put("glyph", draft.categoryGlyph)
        .put("accent", draft.accentHex)
        .put("fact", draft.factText)
        .put("caption", draft.caption)
        .put("kind", draft.kind)
        .put("style", draft.style)
        .put("aspect", draft.aspect)
        .put("scale", draft.bodyScale.toDouble())
        .put("byline", draft.byline)

    /** The title an archive row shows for a post: its topic, or its words. */
    fun titleFor(draft: CommunityCardDraft): String = when {
        draft.kind == KIND_CARD && draft.topicName.isNotBlank() -> draft.topicName
        draft.factText.isNotBlank() -> draft.factText.lineSequence().first().trim().take(60)
        draft.caption.isNotBlank() -> draft.caption.lineSequence().first().trim().take(60)
        else -> "A deleted post"
    }

    /** True for a post that carries its own card art (the wall's topic card). */
    fun carriesCard(kind: String): Boolean = kind == KIND_CARD

    /** Which composer kind a post came from, for the draft it clears. */
    fun composerKindOf(kind: String): String = when (kind) {
        KIND_CARD, KIND_NOTE, KIND_QUOTE -> kind
        else -> KIND_NOTE
    }

    private fun JSONObject.toDraft(): CommunityCardDraft = CommunityCardDraft(
        topicName = optString("topic"),
        categoryName = optString("category"),
        categorySlug = optString("slug"),
        categoryGlyph = optString("glyph"),
        accentHex = optString("accent"),
        factText = optString("fact"),
        caption = optString("caption"),
        kind = optString("kind").ifBlank { KIND_NOTE },
        style = optString("style").ifBlank { "PAPER" },
        aspect = optString("aspect").ifBlank { "CLASSIC" },
        bodyScale = optDouble("scale", 1.0).toFloat(),
        byline = optString("byline")
    )

    /** The stored key for one composer kind (lowercased: it is a JSON field). */
    private fun key(kind: String) = kind.lowercase()
}
