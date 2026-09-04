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
 * v338 — extract a dominant swatch from book/album COVER ARTWORK so the
 * notes sheets can tint themselves from the actual cover instead of the
 * static category accent. Uses androidx Palette (the classic "album-art
 * colours" algorithm) on a small decode of the authored imageUrl.
 *
 * [networkAllowed] mirrors the caller's consent gate (the Book-cover fetch
 * toggle): when false the request never reaches the network — it only
 * serves what Coil already cached, matching BookCoverPoster's behaviour.
 *
 * Returns null when there is no URL, nothing is cached/loadable, or the
 * decode fails — callers then fall back to the category tint.
 */
suspend fun fetchCoverSwatch(context: Context, url: String?, networkAllowed: Boolean = true): Color? {
    if (url.isNullOrBlank()) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                // Small decode: palette work is on the hue, not the pixels.
                .size(96)
                .allowHardware(false)
                .networkCachePolicy(
                    if (networkAllowed) CachePolicy.ENABLED else CachePolicy.DISABLED
                )
                .build()
            val result = context.imageLoader.execute(request)
            // Coil 2.7: success is the top-level SuccessResult type.
            val drawable = (result as? SuccessResult)?.drawable ?: return@runCatching null
            val bitmap = drawable.toBitmap(96, 96)
            val palette = Palette.from(bitmap).generate()
            val swatch = palette.vibrantSwatch
                ?: palette.darkVibrantSwatch
                ?: palette.mutedSwatch
                ?: palette.darkMutedSwatch
                ?: palette.lightVibrantSwatch
                ?: return@runCatching null
            Color(swatch.rgb)
        }.getOrNull()
    }
}
