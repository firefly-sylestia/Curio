package com.curio.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

/**
 * Loads Curio topic catalogs from `assets/topics/{categoryId}.json`.
 *
 * The JSON schema is intentionally flat (array of topic objects) so a
 * single category file at 1000+ topics stays parseable on cold start.
 *
 * Each file is loaded lazily on first request and cached while memory is
 * available. Android memory-pressure callbacks may clear the cache; topics
 * are immutable and reload safely on demand. Phase 4 (Room persistence)
 * will replace the loader with a DB-backed source using the same
 * [CurioTopic] schema.
 *
 * Schema (per file):
 * ```
 * [
 *   {
 *     "id": "artist-bowie",
 *     "categoryId": "ARTISTS",
 *     "subtype": "Artist",
 *     "name": "David Bowie",
 *     "teaser": "...",
 *     "imageUrl": "",
 *     "exploreAction": {
 *       "verb": "Listen",
 *       "targetName": "Ziggy Stardust (1972)",
 *       "durationMinutes": 38,
 *       "instruction": "..."
 *     },
 *     "tags": ["Rock", "Glam", "1970s"],
 *     "tier": 1
 *   },
 *   ...
 * ]
 * ```
 *
 * Concurrency: parsing runs on [Dispatchers.IO]. The cache itself is
 * guarded by a [Mutex] so concurrent first-access requests don't both
 * parse the same file (an earlier version of this loader had a race
 * that double-parsed 1MB+ JSON files at startup).
 *
 * Failure handling: missing or malformed files throw [TopicLoadException]
 * with the offending path + reason. We deliberately fail loud rather
 * than return an empty list — a missing JSON file is a build / data
 * bug, and silent empty pools would let Spin land on no topic at all.
 */
object TopicJsonLoader {

    private const val ASSET_DIR = "topics"

    /** Per-category caches. Writes are guarded by [cacheWriteLock] + the
     *  generation counter (a memory callback can never be undone by a
     *  stale in-flight parse). */
    private val cache: MutableMap<CategoryId, List<CurioTopic>> = ConcurrentHashMap()
    /** Dedupes in-flight first loads: one shared parse per lane. DIFFERENT
     *  lanes parse in parallel, while the same lane's concurrent callers
     *  share a single parse. The old single global mutex held through the
     *  whole parse, so the cold-start prewarm queue blocked Spin's load of
     *  an unrelated lane until every category finished — and the screen's
     *  index load double-parsed the big merged index alongside the prewarm. */
    private val inFlight = ConcurrentHashMap<CategoryId, Deferred<List<CurioTopic>>>()
    /** Short-held only: guards the in-flight slot map, NEVER the parse body. */
    private val inFlightMutex = Mutex()
    /** Parses run on this scope so a shared parse survives its creator's
     *  cancellation — other awaiters still receive the result. */
    private val loadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheWriteLock = Any()
    @Volatile private var cacheGeneration: Long = 0L

    /**
     * Installs the [android.content.res.AssetManager] used to read
     * topic JSON files. Must be called once at app startup (typically
     * from [com.curio.app.FieldMindApplication.onCreate]) BEFORE any
     * Compose code runs. Throws [TopicLoadException] if a load is
     * attempted before [install].
     */
    @Volatile private var assets: android.content.res.AssetManager? = null
    fun install(context: Context) {
        assets = context.applicationContext.assets
    }

    /**
     * Returns the topic pool for [id], loading + parsing the JSON file
     * on first access. Subsequent calls return the cached list.
     *
     * Suspends to load on [Dispatchers.IO]. Safe to call from any
     * coroutine context — including the main thread (it will hop to IO
     * automatically).
     *
     * @throws TopicLoadException if the file is missing or malformed,
     *   or if [install] hasn't been called yet.
     */
    suspend fun load(id: CategoryId): List<CurioTopic> {
        // Fast path: already resident.
        cache[id]?.let { return it }
        // Fast path: a cold-start prewarm (or another screen) is already
        // parsing this lane — share their parse instead of double-parsing
        // the asset.
        inFlight[id]?.let { return it.await() }
        // Create the shared parse under a SHORT mutex (never held during the
        // parse): the same lane's concurrent callers share one parse, while
        // different lanes parse in parallel.
        val deferred: Deferred<List<CurioTopic>> = inFlightMutex.withLock {
            cache[id]?.let { hit -> return hit }
            inFlight[id] ?: loadScope.async(Dispatchers.IO) { parseAndCache(id) }
                .also { inFlight[id] = it }
        }
        return try {
            deferred.await()
        } finally {
            // Compare-and-remove so only the CREATOR clears the slot — a
            // waiter that got cancelled can never evict the shared parse.
            inFlight.remove(id, deferred)
        }
    }

    /**
     * Parses [id]'s pool (merging every lane for WILDCARD) and caches it.
     * The shared body behind the in-flight dedupe in [load]; runs on IO.
     */
    private suspend fun parseAndCache(id: CategoryId): List<CurioTopic> {
        val generation = cacheGeneration
        val parsed = if (id == CategoryId.WILDCARD) {
            // Wildcard = merge ALL categories into one big pool.
            // Use cache for already-loaded categories, parse + cache
            // the rest so subsequent per-category loads are free.
            val merged = mutableListOf<CurioTopic>()
            val seenIds = mutableSetOf<String>()
            // 1. Collect from every non-wildcard category.
            CategoryId.values()
                .filter { it != CategoryId.WILDCARD }
                .forEach { otherId ->
                    val topics = cache[otherId]
                        ?: parseAsset("$ASSET_DIR/${otherId.routeSlug}.json", otherId)
                            .also {
                                synchronized(cacheWriteLock) {
                                    if (cacheGeneration == generation) cache[otherId] = it
                                }
                            }
                    topics.forEach { t ->
                        if (seenIds.add(t.id)) merged.add(t)
                    }
                }
            // 2. Also pull in wildcard.json for any hand-curated
            //    topics not already covered by the category files.
            runCatching {
                parseAsset("$ASSET_DIR/${CategoryId.WILDCARD.routeSlug}.json", CategoryId.WILDCARD)
            }.onFailure {
                android.util.Log.w("TopicJsonLoader", "wildcard.json skipped: ${it.message}")
            }.getOrNull()?.forEach { t ->
                if (seenIds.add(t.id)) merged.add(t)
            }
            merged
        } else {
            parseAsset("$ASSET_DIR/${id.routeSlug}.json", id)
        }
        // A memory callback can clear the concurrent map while this
        // parse is in progress. Never let an old parse refill a cache
        // that Android has just asked us to release.
        synchronized(cacheWriteLock) {
            if (cacheGeneration == generation) cache[id] = parsed
        }
        return parsed
    }

    /**
     * Synchronous accessor — returns the cached pool for [id] if it
     * has already been loaded, or null otherwise. Use this from
     * Compose state that already knows the data is loaded (e.g.
     * SpinScreen after a LaunchedEffect has called [load]).
     */
    fun cached(id: CategoryId): List<CurioTopic>? = cache[id]

    /**
     * Eagerly loads + caches the ten canonical category JSON files.
     * The derived WILDCARD pool is intentionally excluded: it duplicates
     * references to every canonical topic and can be built on demand by
     * [load] when the wildcard lane is actually used.
     *
     * Callers should prefer loading only the category they need. This helper
     * remains for exhaustive tooling and compatibility, but is not used on
     * the splash path.
     *
     * Returns successfully even if individual categories fail to load
     * — those exceptions are swallowed and logged via the [cache]'s
     * "not present" state. Callers can re-attempt per-category via
     * [load].
     */
    suspend fun preloadAll() {
        CategoryId.values()
            .filter { it != CategoryId.WILDCARD }
            .forEach { id -> runCatching { load(id) } }
    }

    /**
     * Counts canonical topics without constructing or caching CurioTopic
     * objects. This is used by promotional UI that needs a truthful count
     * but does not need every catalog resident in the heap.
     *
     * v31 — the count is CACHED in memory: Home's Topics stat re-runs this
     * every time Home re-enters composition (each tab switch back parses
     * the whole ~14k-topic catalog again), which made Home feel like it
     * "opened late". One parse per process, instant afterwards.
     */
    @Volatile private var canonicalTopicCount: Int = -1

    suspend fun countCanonicalTopics(): Int = withContext(Dispatchers.IO) {
        canonicalTopicCount.takeIf { it >= 0 }?.let { return@withContext it }
        val am = assets ?: return@withContext 0
        val count = CategoryId.values()
            .filter { it != CategoryId.WILDCARD }
            .sumOf { id ->
                runCatching {
                    am.open("$ASSET_DIR/${id.routeSlug}.json").bufferedReader().use {
                        JSONArray(it.readText()).length()
                    }
                }.getOrDefault(0)
            }
        canonicalTopicCount = count
        count
    }

    /**
     * Counts the topics in one category's file without constructing or
     * caching CurioTopic objects. Used for the "Mixed · N topics" deck
     * labels, where only a truthful total is needed. Wildcard resolves to
     * the canonical total across all categories.
     */
    suspend fun countFor(id: CategoryId): Int = withContext(Dispatchers.IO) {
        if (id == CategoryId.WILDCARD) return@withContext countCanonicalTopics()
        val am = assets ?: return@withContext 0
        runCatching {
            am.open("$ASSET_DIR/${id.routeSlug}.json").bufferedReader().use {
                JSONArray(it.readText()).length()
            }
        }.getOrDefault(0)
    }

    // ── Prebuilt Topic Database index (v29) ───────────────────────────────
    // scripts/build_topic_index.py merges every assets/topics/*.json into
    // one topic_index.json with the search keys (lowercased) and the sort
    // YEAR precomputed at BUILD time. The Topic Database reads this single
    // file — parsed once and prewarmed at app start — so it renders with
    // ZERO loading and the per-topic runtime work (lowercasing, year regexes)
    // stays flat as the catalog grows past 20k. Falls back to the live
    // per-category load when the asset is absent.
    private const val INDEX_ASSET = "topic_index.json"

    @Volatile private var indexCache: List<TopicIndexEntry>? = null
    /** Guards the index parse so the cold-start prewarm and the Topic
     *  Database's own load share ONE parse instead of double-parsing the
     *  merged 16k-topic index on a slow device. */
    private val indexMutex = Mutex()

    /**
     * Loads (once) and caches the prebuilt database index. Null when the
     * asset is missing or malformed — callers fall back to per-category
     * loads. Suspends on [Dispatchers.IO]. Concurrent callers (the app
     * prewarm + a screen) share a single parse.
     */
    suspend fun loadIndex(): List<TopicIndexEntry>? = withContext(Dispatchers.IO) {
        indexCache?.let { return@withContext it }
        indexMutex.withLock {
            indexCache?.let { return@withContext it }
            val am = assets ?: return@withContext null
            val entries = runCatching {
                val root = JSONObject(
                    am.open(INDEX_ASSET).bufferedReader().use { it.readText() }
                )
                val arr = root.getJSONArray("topics")
                List(arr.length()) { i ->
                    val obj = arr.getJSONObject(i)
                    val topic = parseTopic(obj)
                    val year = if (obj.has("year") && !obj.isNull("year"))
                        obj.optInt("year", 0).takeIf { it > 0 } else null
                    val keys = obj.optJSONObject("keys")
                    fun key(name: String, fallback: String): String {
                        val k = keys?.optString(name, "")?.takeIf { it.isNotEmpty() }
                        return k ?: fallback.lowercase()
                    }
                    val tagsArr = keys?.optJSONArray("tags")
                    val tagKeys = if (tagsArr != null)
                        List(tagsArr.length()) { j -> tagsArr.getString(j) }
                    else topic.tags.map(String::lowercase)
                    TopicIndexEntry(
                        topic = topic,
                        year = year,
                        nameKey = key("name", topic.name),
                        subtypeKey = key("subtype", topic.subtype),
                        bylineKey = key("byline", topic.byline),
                        teaserKey = key("teaser", topic.teaser),
                        tagKeys = tagKeys
                    )
                }
            }.getOrNull()
            indexCache = entries
            entries
        }
    }

    /** Synchronous accessor — null until [loadIndex] first succeeds. */
    fun cachedIndex(): List<TopicIndexEntry>? = indexCache

    /** Clears the cache. Safe to call from Android memory callbacks. */
    fun clearCache() {
        // Advance the generation and clear under the same monitor used by
        // load() insertion. A clear can never land between the generation
        // check and an old parse being written back into the cache.
        synchronized(cacheWriteLock) {
            cacheGeneration += 1L
            cache.clear()
        }
        // v51 — the memory-pressure shed is now COMPLETE: the prebuilt index
        // (16k TopicIndexEntry objects + the lowercased key copies of every
        // name/subtype/byline/teaser string) and the canonical count are
        // dropped too. Previously only the per-category pools were released,
        // so ~30-60MB stayed resident after a TRIM_MEMORY callback — the
        // heap sat near-full and background GCs fired every second on
        // mid-range devices (the reported "constantly GC-ing" lag). All
        // three are rebuilt lazily on next use.
        indexCache = null
        canonicalTopicCount = -1
    }

    // ── Internal ───────────────────────────────────────────────────────────

    /**
     * Strips em dashes (U+2014) and en dashes (U+2013) from display text,
     * replacing them with a standard hyphen. Also collapses any resulting
     * double-hyphens or leading/trailing whitespace.
     */
    private fun cleanText(raw: String): String =
        raw.replace('\u2014', '-')   // em dash → hyphen
           .replace('\u2013', '-')   // en dash → hyphen
           .replace(Regex("-{2,}"), "-")
           .trim()

    private fun parseAsset(path: String, id: CategoryId): List<CurioTopic> {
        val am = assets
            ?: throw TopicLoadException(path, id, "TopicJsonLoader.install(context) not called")
        val raw = try {
            am.open(path).bufferedReader().use { it.readText() }
        } catch (t: Throwable) {
            throw TopicLoadException(path, id, "open/read failed: ${t.message}", t)
        }
        return try {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                parseTopic(obj)
            }
        } catch (t: Throwable) {
            throw TopicLoadException(path, id, "parse failed: ${t.message}", t)
        }
    }

    private fun parseTopic(obj: JSONObject): CurioTopic {
        val id = obj.getString("id")
        val catRaw = obj.getString("categoryId")
        val categoryId = runCatching { CategoryId.valueOf(catRaw) }
            .getOrElse {
                throw IllegalStateException(
                    "topic '$id' has unknown categoryId '$catRaw' " +
                    "(expected one of ${CategoryId.values().joinToString { it.name }})"
                )
            }
        val subtype = obj.optString("subtype", "")
        val name = cleanText(obj.getString("name"))
        val teaser = cleanText(obj.getString("teaser"))
        val imageUrl = obj.optString("imageUrl", "")
        val eaObj = obj.getJSONObject("exploreAction")
        val exploreAction = ExploreAction(
            verb            = eaObj.getString("verb"),
            targetName      = cleanText(eaObj.getString("targetName")),
            durationMinutes = eaObj.optInt("durationMinutes", 30),
            instruction     = cleanText(eaObj.getString("instruction"))
        )
        val tagsArr = obj.optJSONArray("tags")
        val tags: List<String> = if (tagsArr != null) {
            List(tagsArr.length()) { i -> tagsArr.getString(i) }
        } else emptyList()
        val tier = obj.optInt("tier", 1)
        val byline = obj.optString("byline", "")
        // v29 — optional progress metadata: books carry `pageCount`,
        // anime carries `episodeCount` (absent/null → no progress tracking).
        val pageCount = if (obj.has("pageCount")) obj.optInt("pageCount", 0).takeIf { it > 0 } else null
        val episodeCount = if (obj.has("episodeCount")) obj.optInt("episodeCount", 0).takeIf { it > 0 } else null
        return CurioTopic(
            id            = id,
            categoryId    = categoryId,
            subtype       = subtype,
            name          = name,
            teaser        = teaser,
            imageUrl      = imageUrl,
            exploreAction = exploreAction,
            tags          = tags,
            tier          = tier,
            byline        = byline,
            pageCount     = pageCount,
            episodeCount  = episodeCount
        )
    }
}

/**
 * One entry of the prebuilt Topic Database index (v29): the full topic
 * plus the fields the browser used to derive at runtime — the lowercased
 * search keys and the sort year — now precomputed at build time by
 * scripts/build_topic_index.py, so the database renders instantly at any
 * catalog size.
 */
data class TopicIndexEntry(
    val topic: CurioTopic,
    val year: Int?,
    val nameKey: String,
    val subtypeKey: String,
    val bylineKey: String,
    val teaserKey: String,
    val tagKeys: List<String>
)

/** Thrown when a topic JSON file is missing or malformed. */
class TopicLoadException(
    val path: String,
    val categoryId: CategoryId,
    reason: String,
    cause: Throwable? = null
) : RuntimeException("Failed to load $path for ${categoryId.name}: $reason", cause)