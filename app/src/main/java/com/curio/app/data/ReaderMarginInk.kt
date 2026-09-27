package com.curio.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * v492 — THE PENCIL MARGIN: what the member scrawls in a book's margin.
 *
 * The idea is agenda §5.5, kept from the second round: *"drag in from the right
 * edge and scrawl in the margin with the app's ink, anchored to that page"*. What
 * a real margin is for is the thing no reading app lets you do — a mark in the
 * hand, in the place the words are, at the moment they land.
 *
 * ## What is stored, and why it is stored in FRACTIONS
 *
 * One page of margin is a list of STROKES, and each stroke is a flat list of
 * `(x, y)` pairs **normalised to the strip's own box (0f…1f)**. A fraction means
 * the same scrawl survives a rotation, a different strip width, a tablet, a font
 * size change and a re-open — none of which a pixel coordinate would. The strip
 * converts to pixels only at draw time (see `ReaderMargin.kt`).
 *
 * ## Why its own prefs file, and why the caps
 *
 * Its own file (`curio_reader_margin`, listed in [CurioBackupManager] so it ships
 * with the member's backup) for the same reason [CurioRecall] has one: no Room
 * migration, and a corrupt record cannot take the reading data with it.
 *
 * A margin is READ AND WRITTEN ON A STROKE, so the blob's size is bounded on
 * purpose rather than hoped about: [MAX_STROKES] per page and [MAX_POINTS] per
 * stroke. The cap is not a limit on how much margin a page can have — it is a
 * limit on how much can be stored **in one page of a book**, which is a book's
 * own margin: nobody scrawls past it, and a runaway finger can no longer write a
 * megabyte of JSON into SharedPreferences.
 */
object ReaderMarginInk {

    private const val PREFS = "curio_reader_margin"
    private const val KEY_PAGES = "pages"

    /** Strokes a single page's margin will hold (the oldest are dropped). */
    const val MAX_STROKES = 80

    /** Points a single stroke will hold — past this the pen is off the paper. */
    const val MAX_POINTS = 900

    /** The identity of one page of one book. */
    private fun key(bookId: String, page: Int) = "$bookId|$page"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readAll(context: Context): org.json.JSONObject {
        val raw = prefs(context).getString(KEY_PAGES, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            // An unreadable store is an empty store: losing a margin is a shrug,
            // and guessing at a corrupt one is how it gets written wrong next.
            JSONObject()
        }
    }

    /**
     * One page's strokes, each a flat `x, y, x, y…` list in 0f…1f.
     *
     * Returned as `List<FloatArray>` rather than `List<Offset>` so this file
     * stays free of Compose types — the store is data, and the strip is what
     * knows about pixels.
     */
    fun strokes(context: Context, bookId: String, page: Int): List<FloatArray> {
        val arr = readAll(context).optJSONArray(key(bookId, page)) ?: return emptyList()
        return try {
            buildList {
                for (i in 0 until arr.length()) {
                    val row = arr.optJSONArray(i) ?: continue
                    val flat = FloatArray(row.length())
                    for (j in 0 until row.length()) flat[j] = row.optDouble(j, 0.0).toFloat()
                    if (flat.size >= 4) add(flat)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Whether this page carries any margin at all — the tile's own mark. */
    fun hasInk(context: Context, bookId: String, page: Int): Boolean =
        strokes(context, bookId, page).isNotEmpty()

    /**
     * Replaces this page's margin. The caller owns the working copy and hands
     * over the whole thing, which keeps one writer and one shape: a partial
     * update path is how two strokes end up sharing a slot.
     */
    fun save(context: Context, bookId: String, page: Int, strokes: List<FloatArray>) {
        val all = readAll(context)
        val arr = JSONArray()
        strokes.takeLast(MAX_STROKES).forEach { flat ->
            val row = JSONArray()
            // An odd tail cannot draw a segment: trim to whole points first.
            val usable = flat.size - (flat.size % 2)
            var i = 0
            while (i < usable && i / 2 < MAX_POINTS) {
                row.put(flat[i].toDouble())
                row.put(flat[i + 1].toDouble())
                i += 2
            }
            if (row.length() >= 4) arr.put(row)
        }
        all.put(key(bookId, page), arr)
        prefs(context).edit().putString(KEY_PAGES, all.toString()).apply()
    }

    /** Takes a page's margin off the page. The book is never touched. */
    fun clear(context: Context, bookId: String, page: Int) {
        val all = readAll(context)
        all.remove(key(bookId, page))
        prefs(context).edit().putString(KEY_PAGES, all.toString()).apply()
    }
}
