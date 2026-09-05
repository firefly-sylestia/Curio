package com.curio.app.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
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
    val lightMuted: Color?
) {
    /** The most "cover-like" swatch for single-colour consumers. */
    val dominant: Color?
        get() = vibrant ?: darkVibrant ?: muted ?: darkMuted ?: lightVibrant ?: lightMuted
}

/**
 * v339 — extract the full colour set from book/album COVER ARTWORK so the
 * notes sheets can wear a palette derived from the actual cover (the classic
 * album-art-colours approach: vibrant pops, dark/muted shades anchor the
 * background, light shades lift the cards). Uses androidx Palette on a small
 * decode of the authored imageUrl.
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
                .size(192)
                .allowHardware(false)
                .networkCachePolicy(
                    if (networkAllowed) CachePolicy.ENABLED else CachePolicy.DISABLED
                )
                .build()
            val result = context.imageLoader.execute(request)
            // Coil 2.7: success is the top-level SuccessResult type.
            val drawable = (result as? SuccessResult)?.drawable ?: return@runCatching null
            val bitmap = drawable.toBitmap(192, 192)
            val palette = Palette.from(bitmap).generate()
            CoverSwatches(
                vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) },
                muted = palette.mutedSwatch?.rgb?.let { Color(it) },
                darkVibrant = palette.darkVibrantSwatch?.rgb?.let { Color(it) },
                darkMuted = palette.darkMutedSwatch?.rgb?.let { Color(it) },
                lightVibrant = palette.lightVibrantSwatch?.rgb?.let { Color(it) },
                lightMuted = palette.lightMutedSwatch?.rgb?.let { Color(it) }
            )
        }.getOrNull()
    }
}

/**
 * v338 — single dominant swatch (the classic album-art colour) for callers
 * that only need one tint. Delegates to [fetchCoverSwatches].
 */
suspend fun fetchCoverSwatch(context: Context, url: String?, networkAllowed: Boolean = true): Color? =
    fetchCoverSwatches(context, url, networkAllowed)?.dominant