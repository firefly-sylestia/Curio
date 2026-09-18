package com.curio.app.data

import android.content.Context
import com.google.gson.Gson
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
    /** Position in the viewing order — see the class note above. */
    val order: Int = 0,
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
        val studio = gson.fromJson(json, IncursionStudio::class.java) ?: IncursionStudio()
        // The storage key is stamped here rather than in the JSON: it is a fact
        // about the app's own storage, not about upstream's data, and stamping
        // it at the one place the data enters the app means no screen can
        // forget to.
        return studio.copy(
            entries = studio.entries.map { it.apply { storageKey = "${studio.id}:$id" } }
        )
    }

    /** Every studio's essentials, for the page's Essentials tab. */
    fun essentials(studios: List<IncursionStudio>): List<Pair<IncursionStudio, IncursionEntry>> =
        studios.flatMap { studio -> studio.entries.filter { it.essential }.map { studio to it } }
}
