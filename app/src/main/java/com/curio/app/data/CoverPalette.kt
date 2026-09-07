package com.curio.app.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * v339 — the raw Palette swatch slots lifted from book/album COVER ARTWORK.
 * All six classic swatches are captured so the theme layer can build a FULL
 * sheet palette (background, cards, chips, text) from the actual art instead
 * of a single dominant tint.
 */
data class CoverSwatches(
    val vibrant: Color?,
    val muted: Color?,
    val darkVibrant: Color?,
    val darkMuted: Color?,
    val lightVibrant: Color?,
    val lightMuted: Color?,
    // v371 — the TRUE majority colour of the cover artwork, computed by
    // direct pixel votes (see [extractCoverSwatches]) instead of Palette's
    // median-cut guess. The sheet wash keys off this so the tint you see
    // on the notes sheet is the colour you actually SEE on the cover.
    val dominant: Color? = null
) {
    /** The most "cover-like" swatch for single-colour consumers. */
    val primary: Color?
        get() = dominant ?: vibrant ?: darkVibrant ?: muted ?: darkMuted ?: lightVibrant ?: lightMuted
}

/** Fixed slot order for the disk cache (see [coverSwatchesToArgbs]). */
private val CoverSwatchesSlotOrder =
    listOf(
        "vibrant", "muted", "darkVibrant", "darkMuted",
        "lightVibrant", "lightMuted", "dominant"
    )

/**
 * v375 — flattens [swatches] into the 7-ARGB slot list the prefs cache
 * stores (null → 0). Slot order matches [CoverSwatchesSlotOrder]; reusing a
 * Color from a real cover as a sentinel is impossible because extracted
 * pixels are always opaque, while Color(0) is fully transparent.
 */
fun coverSwatchesToArgbs(swatches: CoverSwatches): List<Int> {
    val slots = mapOf(
        "vibrant" to swatches.vibrant,
        "muted" to swatches.muted,
        "darkVibrant" to swatches.darkVibrant,
        "darkMuted" to swatches.darkMuted,
        "lightVibrant" to swatches.lightVibrant,
        "lightMuted" to swatches.lightMuted,
        "dominant" to swatches.dominant
    )
    return CoverSwatchesSlotOrder.map { slots[it]?.toArgb() ?: 0 }
}

/**
 * v375 — rebuilds [CoverSwatches] from the 7-ARGB cache list (0 = absent).
 * Returns null when the list is empty/malformed so callers fall back to the
 * category tint exactly as if nothing were cached.
 */
fun coverSwatchesFromArgbs(argbs: List<Int>): CoverSwatches? {
    if (argbs.size < 7) return null
    fun c(i: Int): Color? = argbs[i].takeIf { it != 0 }?.let { Color(it) }
    return CoverSwatches(
        vibrant = c(0),
        muted = c(1),
        darkVibrant = c(2),
        darkMuted = c(3),
        lightVibrant = c(4),
        lightMuted = c(5),
        dominant = c(6)
    )
}

/**
 * v339/v371 — extract the full colour set from book/album COVER ARTWORK so
 * the notes sheets can wear a palette derived from the actual cover (the
 * classic album-art-colours approach: vibrant pops, dark/muted shades anchor
 * the background, light shades lift the cards). v371 — extraction is a
 * direct pixel-vote histogram ([extractCoverSwatches]) on a small decode of
 * the artwork, so the swatches (and the true majority [CoverSwatches.dominant])
 * match the colours the user actually SEES instead of androidx Palette's
 * median-cut guess.
 *
 * [networkAllowed] mirrors the caller's consent gate (the Book-cover fetch
 * toggle): when false the request never reaches the network — it only serves
 * what Coil already cached, matching BookCoverPoster's behaviour.
 *
 * Returns null when there is no URL, nothing is cached/loadable, or the
 * decode fails — callers then fall back to the category tint.
 */
suspend fun fetchCoverSwatches(context: Context, url: String?, networkAllowed: Boolean = true): CoverSwatches? {
    if (url.isNullOrBlank()) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                // Small decode: palette work is on the hue, not the pixels.
                // 256 keeps enough pixel detail for the histogram to separate
                // the cover's real hues without a huge sample.
                .size(256)
                .allowHardware(false)
                .networkCachePolicy(
                    if (networkAllowed) CachePolicy.ENABLED else CachePolicy.DISABLED
                )
                .build()
            val result = context.imageLoader.execute(request)
            // Coil 2.7: success is the top-level SuccessResult type.
            val drawable = (result as? SuccessResult)?.drawable ?: return@runCatching null
            val bitmap = drawable.toBitmap(256, 256)
            extractCoverSwatches(bitmap)
        }.getOrNull()
    }
}

/**
 * v371 — REAL swatch extraction from the cover's actual pixels. The old
 * androidx Palette route was unreliable: on many covers the median-cut
 * quantizer returned swatches that were absent, near-grey, or simply not
 * the colours the user SEES (busy artwork, white/black text plates and
 * hard-edged layouts misled it), so the notes sheets wore a muddy or wrong
 * tint and the "dominant" colour was often a secondary one.
 *
 * This samples a grid of the bitmap, buckets pixels by HSL, and computes
 * each classic slot from the pixel votes directly:
 *  - dominant  — the single MOST-VOTED colour (the cover's true majority),
 *  - vibrant   — the most saturated non-grey colour bucket (the pop),
 *  - muted     — the most-voted muted (mid-saturation) bucket,
 *  - dark/light variants — the most-voted buckets in those lightness bands.
 * Votes are weighted by saturation so washed-out pixels don't out-vote the
 * artwork's real hues, and identical buckets merge across near-neighbours.
 */
fun extractCoverSwatches(bitmap: android.graphics.Bitmap): CoverSwatches {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return CoverSwatches(null, null, null, null, null, null)
    val px = IntArray(w * h)
    bitmap.getPixels(px, 0, w, 0, 0, w, h)

    // Hue/luminance buckets per slot: key = quantized cell, value = weight.
    data class Bucket(val color: Int, var weight: Float)
    fun bucketFor(hsl: FloatArray, hueKey: Int): String = "$hueKey|${(hsl[1] * 4).toInt()}|${(hsl[2] * 10).toInt()}"

    val dominantBuckets = mutableMapOf<String, Bucket>()
    val vibrant = mutableMapOf<String, Bucket>()
    val muted = mutableMapOf<String, Bucket>()
    val darkVibrant = mutableMapOf<String, Bucket>()
    val darkMuted = mutableMapOf<String, Bucket>()
    val lightVibrant = mutableMapOf<String, Bucket>()
    val lightMuted = mutableMapOf<String, Bucket>()

    // Sample a stride so even a huge decode stays fast: every other pixel on
    // each axis (≈¼ of the pixels) is more than enough for a colour vote.
    var step = 1
    while ((w / step) * (h / step) > 18000) step += 1
    val hsl = FloatArray(3)
    var i = 0
    while (i < w * h) {
        val x = i % w
        val y = i / w
        if (x % step == 0 && y % step == 0) {
            val c = px[i]
            val a = (c ushr 24) and 0xFF
            if (a < 40) { i += 1; continue }
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            android.graphics.Color.RGBToHSV(r, g, b, hsl)
            val hue = hsl[0]
            val sat = hsl[1]
            val lum = hsl[2]
            // Weight = saturation influence + luminance edge: saturated,
            // mid-tone pixels are the "colour" of the art; pure black /
            // white / grey edges contribute less so they don't drown it.
            val weight = 1f + sat * 2f
            // Skip truly transparent/empty corners, keep everything else.
            val rgb = (r shl 16) or (g shl 8) or b
            val hueKey = (hue / 10f).toInt()
            // Each bucket maps to a representative colour: the FIRST pixel
            // that lands in it (pixels within one quantized cell are
            // near-identical, so the first is a fine representative).
            fun vote(map: MutableMap<String, Bucket>) {
                val key = bucketFor(hsl, hueKey)
                val bkt = map[key]
                if (bkt == null) map[key] = Bucket(rgb, weight)
                else bkt.weight += weight
            }
            vote(dominantBuckets)
            when {
                sat >= 0.35f && lum in 0.12f..0.88f -> vote(vibrant)
                sat >= 0.35f -> vote(if (lum < 0.35f) darkVibrant else lightVibrant)
                else -> vote(muted)
            }
            // Lightness-specific votes for the dark/light muted slots.
            if (lum < 0.42f && sat < 0.35f) vote(darkMuted)
            if (lum > 0.62f && sat < 0.35f) vote(lightMuted)
        }
        i += 1
    }

    fun best(map: Map<String, Bucket>): Color? = map.entries.maxByOrNull { it.value.weight }?.value?.color?.let { Color(it) }
    return CoverSwatches(
        vibrant = best(vibrant) ?: best(dominantBuckets),
        muted = best(muted),
        darkVibrant = best(darkVibrant),
        darkMuted = best(darkMuted),
        lightVibrant = best(lightVibrant),
        lightMuted = best(lightMuted),
        // The TRUE majority colour — the most-voted bucket, period. This is
        // the colour the user sees when they look at the cover, and it is
        // what the sheet wash should key off (the old vibrant-first chain
        // routinely picked a secondary colour on busy artwork).
        dominant = best(dominantBuckets)
    )
}

/**
 * v338 — single dominant swatch (the classic album-art colour) for callers
 * that only need one tint. Delegates to [fetchCoverSwatches].
 */
suspend fun fetchCoverSwatch(context: Context, url: String?, networkAllowed: Boolean = true): Color? =
    fetchCoverSwatches(context, url, networkAllowed)?.primary