package com.curio.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.util.concurrent.atomic.AtomicReference

/**
 * INCURSION — the viewing order.
 *
 * A hidden page holding every Marvel, Sony and X-Men title in the order they are
 * meant to be watched, with the essentials marked and a status on each row. The
 * data is NOT authored here: it is the dataset of the
 * `mcu-viewing-order` project, converted straight from that repo's own modules
 * into the three JSON files under `assets/incursion/` by
 * `scripts/import_incursion.mjs`. Nothing in this file invents a title, a year
 * or a synopsis; the shape below mirrors the importer's output exactly.
 *
 * Two things about it are worth knowing before reading on:
 *
 *  - The three studios are THREE LISTS, not one. Upstream keeps them apart
 *    because their orders are separate questions ("what order do I watch the
 *    MCU in" is not the same question as "what order do I watch Spider-Man in"),
 *    so they stay apart here too.
 *  - `order` is the whole point. It is the chronological viewing sequence, and
 *    it is deliberately NOT the list index: it repeats across a series' seasons
 *    and skips where upstream renumbered, so every screen sorts by it and reads
 *    it as a number, never as a position.
 */
data class IncursionStudio(
    val id: String = "",
    val name: String = "",
    val blurb: String = "",
    /** What this studio calls a group: "Phase" for Marvel, "Era" for the rest. */
    val groupLabel: String = "Phase",
    val groups: List<IncursionGroup> = emptyList(),
    val entries: List<IncursionEntry> = emptyList()
)

data class IncursionGroup(
    val id: Int = 0,
    /** The studio's own short name for it where it has one ("Era 1"). */
    val label: String? = null,
    val name: String = "",
    val tagline: String? = null,
    val summary: String? = null
)

data class IncursionEntry(
    val id: Int = 0,
    /**
     * Position in the viewing order — see the class note above.
     *
     * A DOUBLE, because upstream's sequence genuinely has halves: X-Men '97's
     * first season sits at 154.1, between two films numbered 154 and 155. It was
     * an Int here, Gson's int reader throws on a decimal, and that single value
     * silently cost the whole 130-row studio its place on the page.
     */
    val order: Double = 0.0,
    /** The [IncursionGroup] this falls under. */
    val group: Int = 0,
    val type: String = "film",
    val title: String = "",
    val year: Int? = null,
    /** Upstream's essentials flag: the row a first-time viewer should not skip. */
    val essential: Boolean = false,
    val ageRating: String? = null,
    val prereq: String? = null,
    val desc: String? = null,
    val tmdbId: Int? = null,
    val seriesGroup: String? = null,
    val season: Int? = null,
    val episodes: Int? = null,
    /**
     * The episodes this row covers, where upstream states them (a season, or a
     * block of one). Seven of the X-Men rows — every Legion, The Gifted and
     * X-Men '97 season — carry ONLY these and have no episode count at all, so
     * without them those rows said nothing about how much they are.
     */
    val epStart: Int? = null,
    val epEnd: Int? = null,
    val runtime: Int? = null,
    val releaseDate: String? = null,
    val releaseLabel: String? = null,
    /** "released" / "upcoming" / "TBA" / "announced" — absent means released. */
    val releaseStatus: String? = null
) {
    /**
     * The id this entry keeps in storage, qualified by studio.
     *
     * Entry ids come from three independent tables, so they collide across
     * studios by construction (Marvel 1 is Captain America, X-Men 1 is X-Men);
     * a status has to be keyed by both halves or marking one studio's row would
     * mark another studio's.
     */
    @Transient
    var storageKey: String = ""

    /**
     * The order as a row shows it: `154`, or `154.1` where upstream inserted a
     * title between two numbers. The sequence is upstream's, so the label is
     * too; nothing here rounds it into a position it does not have.
     */
    val orderLabel: String
        get() = if (order % 1.0 == 0.0) order.toInt().toString() else order.toString()

    /** `8-12`, or `16` for a single episode, when upstream states the range. */
    val episodeRange: String?
        get() {
            val start = epStart
            val end = epEnd
            return when {
                start != null && end != null && start != end -> "$start-$end"
                end != null -> end.toString()
                start != null -> start.toString()
                else -> null
            }
        }

    /** A film / series / short, as the row should name it. */
    val typeLabel: String get() = when (type.lowercase()) {
        "series" -> "Series"
        "short" -> "Short"
        else -> "Film"
    }

    /** Still to come — a placeholder row upstream has no release date for. */
    val upcoming: Boolean get() = releaseStatus != null &&
        releaseStatus.lowercase() != "released"
}

/**
 * The catalog, read once per process from the app's own assets.
 *
 * Small enough to hold (163 rows) and immutable once parsed, so the load is
 * cached in an [AtomicReference] rather than re-read on every composition —
 * `assets.open` is cheap but it is not free, and the Incursion screen re-reads
 * the catalog on every recomposition that touches it.
 */
object IncursionCatalog {

    private val gson = Gson()

    /** The studios in the order the page presents them. */
    val assetNames: List<String> = listOf("marvel", "sony", "xmen")

    private val cache = AtomicReference<List<IncursionStudio>?>(null)

    fun load(context: Context): List<IncursionStudio> {
        cache.get()?.let { return it }
        val loaded = assetNames.mapNotNull { name -> runCatching { read(context, name) }.getOrNull() }
        cache.set(loaded)
        return loaded
    }

    private fun read(context: Context, name: String): IncursionStudio {
        val json = context.assets.open("incursion/$name.json").use { stream ->
            stream.bufferedReader().readText()
        }
        // A strict parse first, then the same file read field by field. The
        // fallback exists because of how this failed once: ONE `order` written
        // 154.1 made Gson's int reader throw, the exception was swallowed by the
        // caller's `runCatching`, and the whole 130-row studio disappeared from
        // the page with nothing said about it. A single unexpected value must
        // cost at most that value, never the list.
        val parsed = runCatching { gson.fromJson(json, IncursionStudio::class.java) }.getOrNull()
        val studio = parsed?.takeIf { it.entries.isNotEmpty() } ?: lenient(json, name)
        // The storage key is stamped here rather than in the JSON: it is a fact
        // about the app's own storage, not about upstream's data, and stamping
        // it at the one place the data enters the app means no screen can
        // forget to.
        return studio.copy(
            entries = studio.entries.map { it.apply { storageKey = "${studio.id}:$id" } }
        )
    }

    /**
     * The same file, read field by field with nothing that can throw.
     *
     * Reached only when the strict parse throws or comes back empty, so the
     * working path pays nothing for it. Every read answers a VALUE whatever the
     * JSON holds — a missing key, a null, a number where a string was expected —
     * so a single odd row decides that row, not the page.
     */
    private fun lenient(json: String, name: String): IncursionStudio {
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()
            ?: return IncursionStudio(id = name, name = name)
        return IncursionStudio(
            id = root.string("id").ifBlank { name },
            name = root.string("name").ifBlank { name },
            blurb = root.string("blurb"),
            groupLabel = root.string("groupLabel").ifBlank { "Phase" },
            groups = root.objects("groups").map { row ->
                IncursionGroup(
                    id = row.int("id"),
                    label = row.string("label").takeIf { it.isNotBlank() },
                    name = row.string("name"),
                    tagline = row.string("tagline").takeIf { it.isNotBlank() },
                    summary = row.string("summary").takeIf { it.isNotBlank() }
                )
            },
            entries = root.objects("entries").map { row ->
                IncursionEntry(
                    id = row.int("id"),
                    order = row.number("order"),
                    group = row.int("group"),
                    type = row.string("type").ifBlank { "film" },
                    title = row.string("title"),
                    year = row.int("year").takeIf { it > 0 },
                    essential = row.bool("essential"),
                    ageRating = row.string("ageRating").takeIf { it.isNotBlank() },
                    prereq = row.string("prereq").takeIf { it.isNotBlank() },
                    desc = row.string("desc").takeIf { it.isNotBlank() },
                    tmdbId = row.int("tmdbId").takeIf { it > 0 },
                    seriesGroup = row.string("seriesGroup").takeIf { it.isNotBlank() },
                    season = row.int("season").takeIf { it > 0 },
                    episodes = row.int("episodes").takeIf { it > 0 },
                    epStart = row.int("epStart").takeIf { it > 0 },
                    epEnd = row.int("epEnd").takeIf { it > 0 },
                    runtime = row.int("runtime").takeIf { it > 0 },
                    releaseDate = row.string("releaseDate").takeIf { it.isNotBlank() },
                    releaseLabel = row.string("releaseLabel").takeIf { it.isNotBlank() },
                    releaseStatus = row.string("releaseStatus").takeIf { it.isNotBlank() }
                )
            }
        )
    }

    // ── The lenient readers ─────────────────────────────────────────────
    // Each answers a value for anything at all: a missing key, an explicit null,
    // a value of the wrong shape. They are what makes the fallback above
    // impossible to break with bad data.

    private fun JsonObject.string(key: String): String =
        runCatching { (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.asString.orEmpty() }
            .getOrDefault("")

    private fun JsonObject.int(key: String): Int =
        runCatching { (get(key) as? JsonPrimitive)?.asInt ?: 0 }.getOrDefault(0)

    /** A number in an int's place (upstream's `154.1`) is still the number it is. */
    private fun JsonObject.number(key: String): Double =
        runCatching { (get(key) as? JsonPrimitive)?.asDouble ?: 0.0 }.getOrDefault(0.0)

    private fun JsonObject.bool(key: String): Boolean =
        runCatching { (get(key) as? JsonPrimitive)?.asBoolean ?: false }.getOrDefault(false)

    private fun JsonObject.objects(key: String): List<JsonObject> =
        runCatching { (get(key) as? JsonArray)?.mapNotNull { it as? JsonObject } }
            .getOrNull()
            .orEmpty()

    /** Every studio's essentials, for the page's Essentials tab. */
    fun essentials(studios: List<IncursionStudio>): List<Pair<IncursionStudio, IncursionEntry>> =
        studios.flatMap { studio -> studio.entries.filter { it.essential }.map { studio to it } }
}
