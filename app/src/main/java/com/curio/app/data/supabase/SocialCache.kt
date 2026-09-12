package com.curio.app.data.supabase

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * THE SOCIAL DISK CACHE — one tiny JSON file per entry, with a lifetime.
 *
 * What it replaces: the social caches used to live in a single
 * `SharedPreferences` blob, which meant every read parsed one XML file and
 * every write re-serialized the whole store, so nothing scaled past a couple
 * of conversations and nothing ever expired. A thread you read a month ago
 * was still answering instantly, and the wall could show a card the server
 * had already dropped.
 *
 * How it works now:
 *
 *  - One file per entry under `filesDir/curio_social_cache/`, named
 *    `<kind>_<key>_<hash>.json`, holding `{"v":…,"at":…,"d":[…]}`. Writing one
 *    conversation touches one small file — never the whole store.
 *  - A WARM IN-MEMORY COPY in front of it, so the second read of an identity
 *    or a thread is a map lookup. The first read after process start is the
 *    only disk touch, and it is a few KB.
 *  - A **TTL per kind** ([TTL_THREAD_MS] and friends): a stale entry is
 *    dropped on read rather than served as if it were current, which is what
 *    keeps an offline surface honest about how old its copy is.
 *  - **Eviction per kind** ([caps]): the oldest files are deleted once a kind
 *    is over its ceiling, so the directory can never grow without bound.
 *
 * It is a CACHE, not a store of record: the server owns everything, a version
 * bump forgets the lot, and [clear] (sign-out) deletes the directory so the
 * next account cannot read the previous one's conversations or names.
 *
 * Text only, like the rest of the online layer — there is no media path here
 * and no field that could carry one.
 */
internal object SocialCache {

    /** Bump when the stored shape changes, so every stale blob is dropped. */
    const val VERSION = 3

    /** The directory's name under the app's own files dir. */
    const val DIR = "curio_social_cache"

    /** How long each kind stays fresh. Zero means "until the version bumps". */
    const val TTL_THREAD_MS = 14L * 24 * 60 * 60 * 1000
    const val TTL_PERSON_MS = 30L * 24 * 60 * 60 * 1000
    const val TTL_WALL_MS = 3L * 60 * 60 * 1000
    const val TTL_INBOX_MS = 12L * 60 * 60 * 1000
    const val TTL_COMMENTS_MS = 60L * 60 * 1000

    /** How many entries one kind keeps on disk before the oldest are dropped. */
    private val caps = mapOf(
        "thread" to 80,
        "person" to 300,
        "wall" to 1,
        "inbox" to 1,
        "comments" to 40
    )

    private const val defaultCap = 40

    /**
     * Warm copies of the entries already read this session, keyed by
     * `<kind>/<key>`. Held as the raw envelope text so a read costs one
     * `JSONObject` parse and never a second file touch.
     */
    private val memory = ConcurrentHashMap<String, String>()

    private fun dir(context: Context) = File(context.applicationContext.filesDir, DIR)

    /** `<kind>/<key>` — the in-memory identity of one entry. */
    private fun cacheId(kind: String, key: String) = kind + "/" + key

    /**
     * The file for one entry. The key is a server id (letters, digits, `-`,
     * `_`), so it is sanitized for the filesystem and disambiguated with its
     * own hash — two keys that differ only in stripped characters can never
     * collide onto one file.
     */
    private fun fileOf(context: Context, kind: String, key: String): File {
        val safe = key.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(64).ifBlank { "none" }
        val hash = Integer.toHexString(key.hashCode())
        return File(dir(context), "${kind.take(16)}_${safe}_$hash.json")
    }

    /** One entry: its payload plus the wall-clock time it was written. */
    class Entry(val data: JSONArray, val atMillis: Long)

    /**
     * Reads one entry, or null when it is missing, expired, or from an older
     * shape. An expired entry is deleted on the way out.
     */
    fun read(
        context: Context,
        kind: String,
        key: String,
        ttlMillis: Long = 0L
    ): Entry? {
        if (key.isBlank()) return null
        val id = cacheId(kind, key)
        val raw = memory[id] ?: runCatching {
            val file = fileOf(context, kind, key)
            if (file.isFile) file.readText() else null
        }.getOrNull()
        if (raw.isNullOrBlank()) return null

        val envelope = runCatching { JSONObject(raw) }.getOrNull()
        if (envelope == null || envelope.optInt("v", 0) != VERSION) {
            forget(context, kind, key)
            return null
        }
        val at = envelope.optLong("at", 0L)
        if (ttlMillis > 0L && at > 0L && System.currentTimeMillis() - at > ttlMillis) {
            forget(context, kind, key)
            return null
        }
        val data = envelope.optJSONArray("d") ?: return null
        memory[id] = raw
        return Entry(data, at)
    }

    /**
     * Replaces one entry. Best-effort by design — a cache write must never
     * fail the screen that produced the data, and it happens off the main
     * thread (every caller is already in a coroutine).
     */
    fun write(
        context: Context,
        kind: String,
        key: String,
        data: JSONArray,
        ttlMillis: Long = 0L
    ) {
        if (key.isBlank()) return
        runCatching {
            val envelope = JSONObject()
                .put("v", VERSION)
                .put("at", System.currentTimeMillis())
                .put("ttl", ttlMillis)
                .put("d", data)
                .toString()
            memory[cacheId(kind, key)] = envelope
            val directory = dir(context)
            if (!directory.isDirectory && !directory.mkdirs()) return@runCatching
            fileOf(context, kind, key).writeText(envelope)
            trim(context, kind)
        }
    }

    /** Drops one entry from both layers. */
    fun forget(context: Context, kind: String, key: String) {
        memory.remove(cacheId(kind, key))
        runCatching { fileOf(context, kind, key).delete() }
    }

    /**
     * Keeps one kind inside its ceiling by deleting the oldest files. Runs
     * after a write, so the directory can only ever be one entry over.
     */
    private fun trim(context: Context, kind: String) {
        val cap = caps[kind] ?: defaultCap
        val files = dir(context).listFiles { file: File -> file.name.startsWith("${kind}_") } ?: return
        if (files.size <= cap) return
        files.sortedBy { it.lastModified() }
            .take(files.size - cap)
            .forEach { runCatching { it.delete() } }
    }

    /** Forgets EVERYTHING — used when the account signs out. */
    fun clear(context: Context) {
        memory.clear()
        runCatching { dir(context).deleteRecursively() }
    }
}
