package com.curio.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
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
     * v55 — bounds how many JSON files parse AT ONCE (max 2). The cold-start
     * prewarm and several screens can all request lanes
     * together; without a gate they'd parse every file in parallel and
     * saturate all cores — the lag + device heating on mid-range phones.
     * Blocking acquires are fine on Dispatchers.IO (the pool is far larger
     * than 2). Never held across another gated section, so no deadlock.
     */
    private val parseGate = Semaphore(2)

    /** Runs a blocking parse body under [parseGate] (max 2 concurrent). */
    private fun <T> gated(block: () -> T): T {
        parseGate.acquire()
        try {
            return block()
        } finally {
            parseGate.release()
        }
    }

    /**
     * Installs the [android.content.res.AssetManager] used to read
     * topic JSON files. Must be called once at app startup (typically
     * from [com.curio.app.FieldMindApplication.onCreate]) BEFORE any
     * Compose code runs. Throws [TopicLoadException] if a load is
     * attempted before [install].
     */
    @Volatile private var assets: android.content.res.AssetManager? = null
    @Volatile private var appContext: Context? = null
    fun install(context: Context) {
        val ctx = context.applicationContext
        assets = ctx.assets
        appContext = ctx
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
        // Fast path: already resident (the app-start prewarm fills this).
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
        val parsed = try {
            deferred.await()
        } finally {
            // Compare-and-remove so only the CREATOR clears the slot — a
            // waiter that got cancelled can never evict the shared parse.
            inFlight.remove(id, deferred)
        }
        if (parsed.isNotEmpty()) return parsed
        // v348 — THE ASSET IS THE READ PATH. Parsing one JSON file once is far
        // cheaper than a full-lane Room read, which has to map every row and
        // decode its chapters/tracks/episodes JSON column by column; that read
        // used to run FIRST and was itself the delay this loader was meant to
        // remove. Room is now only the fallback for a build whose assets carry
        // no topic JSON at all (see `parseAsset`), which is the one case where
        // it holds the only copy of the catalog.
        return roomFallback(id)
    }

    /**
     * v348 — the lane read back from Room, used ONLY when the bundled JSON
     * produced nothing (a build shipped without its topic JSON assets). Heavy
     * by nature, which is exactly why it is no longer on the primary path.
     *
     * NOTE: never write a glob like "star-dot-json" inside a Kotlin block
     * comment here — slash-star opens a NESTED comment (Kotlin block comments
     * nest), and the doc's own closer only closed that one, swallowing the
     * rest of the file. That is exactly how CI broke once already.
     */
    private suspend fun roomFallback(id: CategoryId): List<CurioTopic> = try {
        if (com.curio.app.data.TopicRepository.isInitialized()) {
            val roomTopics = com.curio.app.data.TopicRepository.loadFromRoom(id)
            if (roomTopics.isNotEmpty()) {
                synchronized(cacheWriteLock) { cache[id] = roomTopics }
            }
            roomTopics
        } else emptyList()
    } catch (_: Exception) { emptyList() }

    /**
     * Parses [id]'s pool and caches it. The shared body behind the in-flight
     * dedupe in [load]; runs on IO.
     *
     * WILDCARD is ONE file, not a merge (v385). It used to load every lane
     * into one pool, so selecting the Wildcard deck paid for the whole
     * catalog: ~38 files parsed behind a two-parse gate, seconds of "topic
     * loading" on a cold start, and a pool that pulled "Bowie" out of
     * Artists and Films. wildcard.json already holds the hand-curated
     * curiosities the lane is FOR, and the Topic Database has shown only
     * those in its Wildcard lane all along — the deck now matches it.
     */
    private suspend fun parseAndCache(id: CategoryId): List<CurioTopic> {
        val generation = cacheGeneration
        val parsed = parseAsset("$ASSET_DIR/${id.routeSlug}.json", id)
        // A memory callback can clear the concurrent map while this
        // parse is in progress. Never let an old parse refill a cache
        // that Android has just asked us to release.
        synchronized(cacheWriteLock) {
            if (cacheGeneration == generation) cache[id] = parsed
        }
        // v294 — Also populate Room database for fast subsequent access.
        // v347 — the mirror no longer blocks the caller: parseAndCache sits on
        // the render path of the Topic Database's cold open (its fallback used
        // to await this delete + full-lane insert — books/albums entities
        // carry chapter/track JSON, so one lane could cost a visible beat on a
        // mid device, before the browser had anything to show). The write now
        // runs fire-and-forget on the loader scope: rows render the moment the
        // JSON is parsed and Room catches up in the background. Best-effort
        // either way — [reloadFromAssets]'s callers do their own awaited
        // writes, so an authoritative refresh is unaffected.
        //
        // v385 — SKIP A LANE ROOM ALREADY MIRRORS. The in-memory pools are
        // empty after process death, so every restart re-parsed every lane and
        // this mirror then re-wrote the WHOLE catalog (delete + ~14k inserts
        // with chapter/track/episode JSON) on IO threads that were also
        // parsing — the restart slow-down that made the Topic Database feel
        // like it was still loading and the deck show its loading hint. One
        // indexed count per lane answers the same question: a lane whose Room
        // row count already equals the parsed count is in sync (content only
        // changes with an install, and TopicRepository's install-gated sync
        // owns that case), so there is nothing to rewrite.
        try {
            if (com.curio.app.data.TopicRepository.isInitialized() && parsed.isNotEmpty()) {
                val appCtx = appContext
                if (appCtx != null) {
                    val db = com.curio.app.data.CurioDatabase.getInstance(appCtx)
                    val mirrorId = id
                    loadScope.launch(Dispatchers.IO) {
                        runCatching {
                            val dao = db.topicDao()
                            if (dao.getCount(mirrorId.name) == parsed.size) {
                                return@runCatching
                            }
                            // A real asset parse is authoritative: mirror the
                            // lane so rows removed from the JSON also leave
                            // Room. Every lane is its own category in Room
                            // now that WILDCARD is a single file too.
                            dao.deleteCategory(mirrorId.name)
                            val entities = parsed.map {
                                com.curio.app.data.TopicEntity.fromCurioTopic(it)
                            }
                            dao.insertAll(entities)
                        }
                    }
                }
            }
        } catch (_: Exception) { /* Room population is best-effort */ }
        return parsed
    }

    /**
     * Synchronous accessor — returns the cached pool for [id] if it
     * has already been loaded, or null otherwise. Use this from
     * Compose state that already knows the data is loaded (e.g.
     * SpinScreen after a LaunchedEffect has called [load]).
     */
    fun cached(id: CategoryId): List<CurioTopic>? = cache[id]

    /** Forces a bundled JSON refresh after an app update. */
    fun invalidate(id: CategoryId) {
        synchronized(cacheWriteLock) {
            cacheGeneration += 1L
            cache.remove(id)
        }
    }

    /** Reads the bundled asset directly, bypassing Room's fast path. */
    suspend fun reloadFromAssets(id: CategoryId): List<CurioTopic> = withContext(Dispatchers.IO) {
        invalidate(id)
        val topics = gated { parseAsset("$ASSET_DIR/${id.routeSlug}.json", id) }
        synchronized(cacheWriteLock) { cache[id] = topics }
        topics
    }

    /**
     * Eagerly loads + caches the canonical category JSON files.
     * WILDCARD is intentionally excluded here as well: it is its own small
     * file (v385), loaded on demand by [load] when the wildcard lane is used.
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

    /**
     * Per-lane topic counts (v55): Spin's "Mixed · N" deck label and the
     * picker's totals call [countFor] on EVERY deck change — each call used
     * to re-read + re-parse the whole category file just for a length.
     * Cached once per lane (kept across a memory trim — a few Ints, v389);
     * parses run under the bounded [parseGate].
     */
    private val countsCache = ConcurrentHashMap<CategoryId, Int>()

    /**
     * v389 — the canonical total as last counted, without counting again.
     * 0 when nothing has counted it yet. Home's Topics stat reads this so it
     * never depends on which lane pools happen to be resident.
     */
    fun cachedCanonicalCount(): Int = canonicalTopicCount.takeIf { it >= 0 } ?: 0

    suspend fun countCanonicalTopics(): Int = withContext(Dispatchers.IO) {
        canonicalTopicCount.takeIf { it >= 0 }?.let { return@withContext it }
        // v55 — derive from the per-lane count cache: each lane's file is
        // parsed at most ONCE per process (shared with countFor), instead
        // of a separate full re-parse of all ten files here.
        val count = CategoryId.values()
            .filter { it != CategoryId.WILDCARD }
            .sumOf { id -> countFor(id) }
        canonicalTopicCount = count
        count
    }

    /**
     * Counts the topics in one category's file without constructing or
     * caching CurioTopic objects. Used for the "Mixed · N topics" deck
     * labels, where only a truthful total is needed. Wildcard resolves to
     * the canonical total across all categories. v55 — cached per lane so
     * repeated label recomputes never re-parse the file.
     */
    suspend fun countFor(id: CategoryId): Int = withContext(Dispatchers.IO) {
        // v385 — WILDCARD counts its OWN file: it is a lane with its own
        // pool now, so answering with the canonical total both described the
        // wrong pool and (through the deck's "Mixed · N" label) claimed a
        // count the deck could never deal.
        countsCache[id]?.let { return@withContext it }
        val am = assets ?: return@withContext 0
        val count = gated {
            runCatching {
                am.open("$ASSET_DIR/${id.routeSlug}.json").bufferedReader().use {
                    JSONArray(it.readText()).length()
                }
            }.getOrDefault(0) // returns 0 if JSON not in APK
        }
        countsCache[id] = count
        count
    }

    // ── Topic Database index ──────────────────────────────────────────────
    // v29 — scripts/build_topic_index.py used to merge every
    // assets/topics/*.json into one prebuilt topic_index.json with the
    // search keys (lowercased) and the sort YEAR precomputed at BUILD time.
    // v174f — that 23MB merged asset no longer ships in the APK (the
    // per-category files already carry every topic — it was a full
    // duplicate), so when it's absent [loadIndex] builds the SAME merged
    // index at runtime from the per-category pools (see
    // [buildIndexFromCatalog]) — parsed once, cached, and prewarmed at app
    // start — so the Topic Database still renders with ZERO loading and
    // the per-topic runtime work (lowercasing, year regexes) stays flat as
    // the catalog grows past 20k. The prebuilt asset path is kept as a
    // fallback for anyone who regenerates it with the script.
    private const val INDEX_ASSET = "topic_index.json"

    @Volatile private var indexCache: List<TopicIndexEntry>? = null
    /** Guards the index parse so the cold-start prewarm and the Topic
     *  Database's own load share ONE parse instead of double-parsing the
     *  merged 16k-topic index on a slow device. */
    private val indexMutex = Mutex()

    /**
     * Loads (once) and caches the merged database index. The prebuilt
     * topic_index.json asset is parsed when present; otherwise the same
     * index is built at runtime from the per-category pools (v174f — the
     * 23MB asset no longer ships). Null only when the whole build fails
     * — callers fall back to per-category loads. Suspends on
     * [Dispatchers.IO]. Concurrent callers (the app prewarm + a screen)
     * share a single parse.
     */
    suspend fun loadIndex(): List<TopicIndexEntry>? = withContext(Dispatchers.IO) {
        indexCache?.let { return@withContext it }
        indexMutex.withLock {
            indexCache?.let { return@withContext it }
            val am = assets ?: return@withContext null
            val entries = runCatching {
                if (am.list("")?.contains(INDEX_ASSET) == true) {
                    // v29 prebuilt path — scripts/build_topic_index.py.
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
                        // v389 — the entry is built from the topic, then the
                        // asset's own precomputed keys/year overlay it. The
                        // topic object is NOT retained (see TopicIndexEntry).
                        indexEntryOf(topic).copy(
                            year = year,
                            nameKey = key("name", topic.name),
                            subtypeKey = key("subtype", topic.subtype),
                            bylineKey = key("byline", topic.byline),
                            teaserKey = key("teaser", topic.teaser),
                            tagKeys = tagKeys
                        )
                    }
                } else {
                    buildIndexFromCatalog()
                }
            }.getOrNull()
            indexCache = entries
            entries
        }
    }

    /**
     * v389 — the ONE place an index entry is derived from a topic.
     *
     * The entry keeps the topic's IDENTITY (id + lane), the display fields the
     * search rows show, and the precomputed lowercase keys — never the topic
     * object. Holding that object was what made the catalog un-sheddable: a
     * warm index kept every pool alive, so [shedForMemory] freed nothing. Only
     * the heavy per-topic payloads (exploreAction, synopsis, chapters, tracks,
     * episodes) are left behind now, and they come back from the lane pool
     * through [topicForEntry] when a screen actually needs one.
     */
    private fun indexEntryOf(topic: CurioTopic): TopicIndexEntry = TopicIndexEntry(
        id = topic.id,
        categoryId = topic.categoryId,
        name = topic.name,
        subtype = topic.subtype,
        byline = topic.byline,
        teaser = topic.teaser,
        tags = topic.tags,
        year = topic.publicationYear(),
        nameKey = topic.name.lowercase(),
        subtypeKey = topic.subtype.lowercase(),
        bylineKey = topic.byline.lowercase(),
        teaserKey = topic.teaser.lowercase(),
        tagKeys = topic.tags.map(String::lowercase)
    )

    /** v174f — runtime mirror of scripts/build_topic_index.py. The prebuilt
     *  merged index stopped shipping (the per-category files duplicate it),
     *  so this builds the same search index once, at runtime, from the
     *  per-category pools: routes through [load] so the parses are SHARED
     *  with the per-category caches (never double-parsed), computes the
     *  lowercased keys + sort year exactly like the build script, and reads
     *  wildcard.json directly (its own lane, v385).
     *  Cached by [loadIndex]; the app prewarm runs it once at startup, so
     *  the Topic Database still renders with zero loading. */
    private suspend fun buildIndexFromCatalog(): List<TopicIndexEntry> {
        val seen = HashSet<String>()
        val out = ArrayList<TopicIndexEntry>()
        fun add(topic: CurioTopic) {
            if (!seen.add(topic.id)) return
            out += indexEntryOf(topic)
        }
        for (id in CategoryId.values()) {
            if (id == CategoryId.WILDCARD) continue
            // A failed lane is skipped, never fatal (same as the database's
            // per-category fallback) — the rest of the catalog still indexes.
            runCatching { load(id) }.getOrElse {
                android.util.Log.w("TopicJsonLoader", "index build: ${id.routeSlug} skipped: ${it.message}")
                emptyList()
            }.forEach { add(it) }
        }
        // Hand-curated wildcard curiosities (the WILDCARD lane's own file).
        runCatching {
            parseAsset("$ASSET_DIR/${CategoryId.WILDCARD.routeSlug}.json", CategoryId.WILDCARD)
        }.getOrNull()?.forEach { add(it) }
        return out
    }

    /** Synchronous accessor — null until [loadIndex] first succeeds. */
    fun cachedIndex(): List<TopicIndexEntry>? = indexCache

    /**
     * v389 — resolves an index entry back to its full topic from the entry's
     * OWN lane pool. Synchronous and parse-free: null when that lane isn't
     * resident (call [topicForEntry] to load it).
     */
    fun cachedTopicFor(entry: TopicIndexEntry): CurioTopic? =
        cached(entry.categoryId)?.firstOrNull { it.id == entry.id }

    /**
     * v389 — resolves an index entry to its full topic, loading the entry's own
     * lane when it isn't resident. The lane read goes through [load], so it
     * shares an in-flight parse with every other caller (at most ONE parse).
     *
     * This is what replaces the index's old `topic` field: the index carries
     * identity + search keys only (see [TopicIndexEntry]), so a caller that
     * needs the topic object itself asks the lane for it.
     */
    suspend fun topicForEntry(entry: TopicIndexEntry): CurioTopic? =
        cachedTopicFor(entry) ?: runCatching {
            load(entry.categoryId).firstOrNull { it.id == entry.id }
        }.getOrNull()

    /**
     * v389 — resolves ONE topic by id through the merged index, loading its
     * lane on demand. Used by screens that persisted only a topic id (the
     * composer's kept draft).
     */
    suspend fun topicById(id: String): CurioTopic? {
        if (id.isBlank()) return null
        val entry = indexCache?.firstOrNull { it.id == id } ?: return null
        return topicForEntry(entry)
    }

    /**
     * v389 — kicks a lane's parse off in the background without awaiting it.
     *
     * The reveal page resolves its topic SYNCHRONOUSLY on the first frame
     * (see `resolveRevealTopic`): the index can say the topic exists, but only
     * the lane pool can hand back the topic object. When a memory trim has
     * dropped that pool, this warms it for the next frame instead of stalling
     * the composition — [load] dedupes, so a later call shares this parse.
     */
    fun warmLane(id: CategoryId) {
        if (cache[id] != null) return
        loadScope.launch { runCatching { load(id) } }
    }

    /**
     * v55 — memory-pressure shed, TIERED so a trim never triggers a full
     * re-parse storm (the lag + heating):
     *  - RUNNING_LOW (the common mid-range case): drop the per-category
     *    pools. The prebuilt 16k-entry INDEX stays (rebuilding it is the
     *    single heaviest parse and the Topic Database re-requests it the
     *    moment it opens) — and, unlike before v389, keeping it now really
     *    does release the catalog: the index stores identity + search keys,
     *    not the topics, so the pools it used to pin are freed here.
     *  - RUNNING_CRITICAL / COMPLETE: also drop the index.
     * v389 — the per-lane counts and the canonical total are KEPT: they are a
     * handful of Ints, and dropping them bought nothing while forcing a
     * ten-file re-count the next time Home asked for its Topics stat.
     * The generation guard still stops a stale in-flight parse from
     * refilling a cache Android just asked us to release.
     */
    fun shedForMemory(level: Int) {
        synchronized(cacheWriteLock) {
            cacheGeneration += 1L
            cache.clear()
        }
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            indexCache = null
            countsCache.clear()
            canonicalTopicCount = -1
        }
    }

    /**
     * v294 — Pre-warm counts from Room so TopicJsonLoader doesn't need to
     * re-parse JSON files on every process restart. Called from
     * TopicRepository.init() after confirming Room has data.
     */
    fun warmCountsFromRoom(counts: Map<CategoryId, Int>) {
        counts.forEach { (id, count) -> countsCache[id] = count }
        canonicalTopicCount = counts.values.sum()
    }

    /**
     * v294 — Pre-warm the topic cache from Room so cached() returns data
     * immediately after a process restart. Called from TopicRepository.init()
     * after confirming Room has data.
     */
    fun warmCacheFromRoom(categoryId: CategoryId, topics: List<CurioTopic>) {
        if (topics.isNotEmpty()) {
            synchronized(cacheWriteLock) { cache[categoryId] = topics }
        }
    }

    // ── Internal ───────────────────────────────────────────────────────────

    // Compiled ONCE (v385). cleanText runs four times for EVERY topic parsed
    // (name, teaser, target name, instruction), so building these Regexes
    // inline meant ~16 compilations per topic — a quarter of a million pattern
    // compilations per cold catalog load, on top of the matching itself. That
    // was a real slice of the "topic database is laggy / slow to load" cost.
    private val DASH_SPACING = Regex("\\s*[\\u2014\\u2013]\\s*")
    private val REPEATED_SPACE = Regex("\\s{2,}")
    private val STRAY_COMMAS = Regex(",\\s*,+")
    private val SPACE_BEFORE_PUNCT = Regex("\\s+([,.!?;:])")

    /**
     * Strips em dashes (U+2014) and en dashes (U+2013) from display text,
     * replacing them with a standard hyphen. Also collapses any resulting
     * double-hyphens or leading/trailing whitespace.
     */
    private fun cleanText(raw: String): String =
        raw.replace(DASH_SPACING, ", ")
           .replace(REPEATED_SPACE, " ")
           .replace(STRAY_COMMAS, ",")
           .replace(SPACE_BEFORE_PUNCT, "$1")
           .trim(' ', ',', ';', ':')

    private fun parseAsset(path: String, id: CategoryId): List<CurioTopic> {
        val am = assets
            ?: throw TopicLoadException(path, id, "TopicJsonLoader.install(context) not called")
        // v294 — JSON files may not be in the APK (they live in data/topics/
        // for CI builds). Return empty list instead of throwing so Room can
        // serve topics as the primary data source.
        return gated {
            val raw = try {
                am.open(path).bufferedReader().use { it.readText() }
            } catch (_: Throwable) {
                // JSON not in APK — Room is the primary source
                return@gated emptyList()
            }
            try {
                val array = JSONArray(raw)
                List(array.length()) { i ->
                    val obj = array.getJSONObject(i)
                    parseTopic(obj)
                }
            } catch (t: Throwable) {
                throw TopicLoadException(path, id, "parse failed: ${t.message}", t)
            }
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
        // v126 — books with a HUGE edition page gap carry the alternative
        // edition's count + label (extra pill on the detail page).
        val altPageCount = if (obj.has("altPageCount")) obj.optInt("altPageCount", 0).takeIf { it > 0 } else null
        val altPageLabel = obj.optString("altPageLabel", "")
        // Books only: narrative synopsis for the detail overlay.
        val synopsis = obj.optString("synopsis", "").takeIf { it.isNotBlank() }
        // Books only: chapter-by-chapter breakdown.
        val chaptersArr = obj.optJSONArray("chapters")
        val chapters: List<BookChapter>? = if (chaptersArr != null && chaptersArr.length() > 0) {
            List(chaptersArr.length()) { i ->
                val ch = chaptersArr.getJSONObject(i)
                BookChapter(
                    number = ch.optInt("number", i + 1),
                    title = ch.optString("title", "Chapter ${i + 1}"),
                    pageStart = ch.optInt("pageStart", 0),
                    pageEnd = ch.optInt("pageEnd", 0),
                    summary = ch.optString("summary", "")
                )
            }
        } else null
        // Albums only: Genius album page (v333 — authored link surfaced in the
        // track-list sheet header). Null for non-album topics without a link.
        val geniusUrl = obj.optString("geniusUrl", "").takeIf { it.isNotBlank() }
        // Albums only: per-track list (number/title/duration).
        val tracksArr = obj.optJSONArray("tracks")
        val tracks: List<AlbumTrack>? = if (tracksArr != null && tracksArr.length() > 0) {
            List(tracksArr.length()) { i ->
                val tr = tracksArr.getJSONObject(i)
                AlbumTrack(
                    number = tr.optInt("number", i + 1),
                    title = tr.optString("title", "Track ${i + 1}"),
                    duration = tr.optString("duration", "")
                )
            }
        } else null
        // Series only: per-episode guide (season + episode + title + summary).
        val episodesArr = obj.optJSONArray("episodes")
        val episodes: List<SeriesEpisode>? = if (episodesArr != null && episodesArr.length() > 0) {
            List(episodesArr.length()) { i ->
                val ep = episodesArr.getJSONObject(i)
                SeriesEpisode(
                    season = ep.optInt("season", 1),
                    number = ep.optInt("number", i + 1),
                    title = ep.optString("title", "Episode ${i + 1}"),
                    summary = ep.optString("summary", ""),
                    airdate = ep.optString("airdate", ""),
                    runtime = ep.optInt("runtime", 0),
                    rating = ep.optDouble("rating", 0.0).toFloat(),
                    stillUrl = ep.optString("stillUrl", "")
                )
            }
        } else null
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
            episodeCount  = episodeCount,
            altPageCount  = altPageCount,
            altPageLabel  = altPageLabel,
            synopsis      = synopsis,
            chapters      = chapters,
            tracks        = tracks,
            geniusUrl     = geniusUrl,
            episodes      = episodes
        )
    }
}

/**
 * One entry of the Topic Database index (v29): a topic's identity plus the
 * fields the browser used to derive at runtime — the lowercased search keys
 * and the sort year — precomputed at build time by
 * scripts/build_topic_index.py, so the database renders instantly at any
 * catalog size.
 *
 * v389 — THE ENTRY NO LONGER HOLDS THE TOPIC. `topic: CurioTopic` used to ride
 * along so the browser could read the topic straight out of the index, which
 * also meant the index pinned every topic in the heap: [TopicJsonLoader
 * .shedForMemory] cleared the per-lane pools and freed almost nothing, because
 * the 16k entries still referenced every topic ("background memory climbs
 * sharply"). The entry now carries identity + display text + search keys only,
 * so dropping the pools is a real release, and callers that need the topic
 * itself resolve it through [TopicJsonLoader.topicForEntry] /
 * [TopicJsonLoader.cachedTopicFor] /
 * [TopicJsonLoader.topicById].
 */
data class TopicIndexEntry(
    val id: String,
    val categoryId: CategoryId,
    val name: String,
    val subtype: String,
    val byline: String,
    val teaser: String,
    val tags: List<String>,
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
