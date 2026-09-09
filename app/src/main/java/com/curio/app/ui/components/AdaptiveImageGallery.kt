package com.curio.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * v7.38 — adaptive, aspect-ratio-true gallery for saved-entry attachments.
 *
 * Every image keeps its real shape and packs into a 4-CELL grid: a
 * portrait takes ONE cell, a landscape / square takes TWO, and a row
 * never exceeds FOUR cells — so you get at most 4 verticals per row and
 * at most 2 horizontals (mixed rows pack the same way: one horizontal +
 * one vertical fills three cells, then three verticals fill the next).
 * Full rows stretch to exactly fill the container width (heights scale
 * with it, so nothing distorts); a lone leftover image renders at its
 * natural size capped at two cells and centered — it never stretches
 * to fill the row.
 *
 * Tapping an image zooms it IN PLACE over the page — the same no-dark-scrim
 * [MoodBoardZoomOverlay] the mood boards use: the image glides from its
 * gallery spot to the center (arc), pinch/pan refine, tap closes. No
 * Lightbox page.
 *
 * Aspect ratios are measured off the main thread with header-only decodes
 * (content URIs via ContentResolver, file paths directly), EXIF-rotated to
 * match Coil's rendering; images that can't be read fall back to square.
 */
@Composable
fun AdaptiveImageGallery(
    uris: List<String>,
    modifier: Modifier = Modifier,
    rowHeight: Dp = 152.dp,
    gap: Dp = 8.dp,
    cornerRadius: Dp = 16.dp
) {
    if (uris.isEmpty()) return
    val context = LocalContext.current
    // The window metrics the visible-slice math reads at tap time.
    val windowInfo = LocalWindowInfo.current
    // Aspect ratio (w/h) per uri — measured off-thread; empty until loaded,
    // so the first frame renders as square fallbacks and recomposes once the
    // real shapes arrive.
    val aspects by produceState<Map<String, Float>>(initialValue = emptyMap(), uris) {
        value = uris.associateWith { uri -> imageAspectOf(context, uri) }
    }
    val zoomState = rememberMoodBoardZoomState()
    // The gallery's top-left in WINDOW coordinates. Written by
    // onGloballyPositioned while scrolling — a plain array, deliberately
    // NOT snapshot state, so scroll frames don't recompose the gallery —
    // and read live at tap time to aim the zoom at the visible screen.
    val galleryOrigin = remember { FloatArray(2) }
    BoxWithConstraints(
        // While zoomed the gallery (and its overlay) must draw ABOVE later
        // siblings in the detail column — the same zIndex trick the saved
        // mood board uses — so the gliding image never slides under the
        // audio bar or the next section.
        modifier = modifier
            .onGloballyPositioned { coords ->
                val p = coords.positionInWindow()
                galleryOrigin[0] = p.x
                galleryOrigin[1] = p.y
            }
            .zIndex(if (zoomState.zoomedUri != null) 1000f else 0f)
    ) {
        val density = LocalDensity.current
        val containerW = with(density) { maxWidth.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val rowHPx = with(density) { rowHeight.toPx() }
        val safeRowHPx = rowHPx.takeIf { it.isFinite() && it > 0f }
            ?.coerceAtMost(2048f) ?: 1f
        // BoxWithConstraints can report a zero-width probe during an
        // intermediate measure. Do not hand that width to SizeNode, and do
        // not calculate a gallery height until the real viewport exists.
        if (containerW <= 0f || !containerW.isFinite()) return@BoxWithConstraints
        val layout = remember(uris, aspects, containerW, gapPx, rowHPx) {
            computeGalleryLayout(uris, aspects, containerW, gapPx, safeRowHPx)
        }
        val safeHeight = layout.heightPx.takeIf { it.isFinite() && it > 0f }
            ?.coerceAtMost(safeRowHPx * uris.size.coerceAtLeast(1) * 3f)
            ?.coerceAtLeast(1f)
            ?: safeRowHPx
        Box(
            modifier = Modifier
                .width(with(density) { containerW.toDp() })
                .height(with(density) { safeHeight.toDp() })
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                layout.rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        // Lone leftover rows (a single image) center at their
                        // natural capped size; multi-image rows justify.
                        horizontalArrangement = if (row.tiles.size == 1) Arrangement.Center
                        else Arrangement.spacedBy(gap)
                    ) {
                        row.tiles.forEach { tile ->
                            Surface(
                                onClick = {
                                    // Aim the zoom at the VISIBLE slice of the
                                    // window (see [visibleViewportOf]) — with
                                    // many images the gallery is taller than
                                    // the screen, and centering in the whole
                                    // gallery would land the magnified image
                                    // mid-gallery, off the visible screen and
                                    // clipped by the page scroll.
                                    val win = windowInfo.containerSize
                                    val vp = visibleViewportOf(
                                        galleryLeft = galleryOrigin[0],
                                        galleryTop = galleryOrigin[1],
                                        galleryW = containerW,
                                        galleryH = safeHeight,
                                        windowW = win.width.toFloat(),
                                        windowH = win.height.toFloat()
                                    )
                                    zoomState.zoomIn(
                                        uri = tile.uri,
                                        centerX = tile.xPx + tile.widthPx / 2f,
                                        centerY = tile.yPx + tile.heightPx / 2f,
                                        tileW = tile.widthPx,
                                        tileH = tile.heightPx,
                                        viewW = vp.widthPx,
                                        viewH = vp.heightPx
                                    )
                                },
                                shape = RoundedCornerShape(cornerRadius),
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .width(with(density) { tile.widthPx.toDp() })
                                    .height(with(density) { tile.heightPx.toDp() })
                            ) {
                                // Tiles are sized to the photo's own aspect
                                // ratio, so Fit fills the rounded box edge to
                                // edge — no bars, no cropping.
                                Image(
                                    // v8.2 — the zoom overlay's hi-res layer
                                    // (moodBoardPainter, 4096px cap) is what
                                    // keeps the magnified tile sharp; Coil 2 has
                                    // no request-level filter quality, and
                                    // foundation 1.4+ removed the painter
                                    // overload's filterQuality parameter.
                                    painter = rememberAsyncImagePainter(tile.uri),
                                    contentDescription = "Attached image",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(cornerRadius))
                                )
                            }
                        }
                    }
                }
            }
            // In-place zoom overlay — LAST child (same as the mood boards):
            // glides the tapped image from its gallery spot to the VISIBLE
            // screen center (arc), pinch/pan refine, tap closes. No dark
            // scrim. The overlay is laid out at the gallery's visible slice
            // of the window (see [visibleViewportOf]) so the magnified image
            // always lands ON the screen, even when the gallery is taller
            // than it.
            layout.rows.flatMap { it.tiles }.firstOrNull { it.uri == zoomState.zoomedUri }
                ?.let { tile ->
                    val win = windowInfo.containerSize
                    val vp = visibleViewportOf(
                        galleryLeft = galleryOrigin[0],
                        galleryTop = galleryOrigin[1],
                        galleryW = containerW,
                        galleryH = safeHeight,
                        windowW = win.width.toFloat(),
                        windowH = win.height.toFloat()
                    )
                    MoodBoardZoomOverlay(
                        zoomState = zoomState,
                        tileUri = tile.uri,
                        tileX = tile.xPx - vp.leftInGallery,
                        tileY = tile.yPx - vp.topInGallery,
                        widthPx = tile.widthPx,
                        heightPx = tile.heightPx,
                        viewW = vp.widthPx,
                        viewH = vp.heightPx,
                        viewportLeft = vp.leftInGallery,
                        viewportTop = vp.topInGallery
                    )
                }
        }
    }
}

/** One laid-out tile: its spot and size in the gallery's viewport pixels. */
private data class GalleryTile(
    val uri: String,
    val xPx: Float,
    val yPx: Float,
    val widthPx: Float,
    val heightPx: Float
)

/** One justified row of tiles, all sharing the row's rendered height. */
private data class GalleryRow(
    val tiles: List<GalleryTile>,
    val heightPx: Float
)

/** The full gallery layout: rows + total height (px). */
private data class GalleryLayout(
    val rows: List<GalleryRow>,
    val heightPx: Float
)

/**
 * Packs [uris] into the adaptive gallery. A 4-column grid where each
 * portrait (aspect < 1) occupies ONE cell and each landscape / square
 * (aspect >= 1) occupies TWO — a row never exceeds FOUR cells, so at most
 * 4 verticals or 2 horizontals fit per row. Full rows justify to the
 * container width (heights scale with the stretch so aspect ratios hold);
 * a lone leftover image renders at natural size capped at two cells and
 * centered, never stretched. A single image overall is shown at natural
 * aspect, capped so an extreme portrait doesn't tower.
 */
private fun computeGalleryLayout(
    uris: List<String>,
    aspects: Map<String, Float>,
    containerW: Float,
    gapPx: Float,
    rowHPx: Float
): GalleryLayout {
    if (uris.isEmpty() || containerW <= 0f || !containerW.isFinite()) {
        return GalleryLayout(emptyList(), 0f)
    }
    if (uris.size == 1) {
        val a = (aspects[uris[0]] ?: 1f).coerceIn(0.25f, 4f)
        // Cap the single-image height so a portrait can't tower off the page.
        val maxH = rowHPx * 2.6f
        var w = containerW
        var h = w / a
        if (h > maxH) {
            h = maxH
            w = h * a
        }
        val tile = GalleryTile(uris[0], (containerW - w) / 2f, 0f, w, h)
        return GalleryLayout(listOf(GalleryRow(listOf(tile), h)), h)
    }

    // Cell geometry of the 4-column grid (a landscape spans two cells).
    val cellW = (containerW - 3f * gapPx) / 4f
    val twoCellW = 2f * cellW + gapPx

    // One attachment: its aspect + how many grid cells it occupies.
    data class Item(val uri: String, val aspect: Float, val cells: Int)
    val items = uris.map { uri ->
        val a = (aspects[uri] ?: 1f).coerceIn(0.25f, 4f)
        Item(uri, a, if (a >= 1f) 2 else 1)
    }

    // Pack greedily — a row holds at most 4 cells.
    val rawRows = mutableListOf<MutableList<Item>>()
    var cur = mutableListOf<Item>()
    var curCells = 0
    items.forEach { item ->
        if (cur.isNotEmpty() && curCells + item.cells > 4) {
            rawRows.add(cur)
            cur = mutableListOf()
            curCells = 0
        }
        cur.add(item)
        curCells += item.cells
    }
    if (cur.isNotEmpty()) rawRows.add(cur)

    // Lay each row out.
    var y = 0f
    val rows = rawRows.map { row ->
        if (row.size == 1) {
            // Leftover image — natural size capped at two cells, centered;
            // it never stretches to fill the row.
            val item = row[0]
            var w = (rowHPx * item.aspect).coerceAtMost(twoCellW)
            var h = w / item.aspect
            if (h > rowHPx * 2.2f) {
                h = rowHPx * 2.2f
                w = h * item.aspect
            }
            val tile = GalleryTile(item.uri, (containerW - w) / 2f, y, w, h)
            y += h + gapPx
            GalleryRow(listOf(tile), h)
        } else {
            // Multi-image row — justify to the container width; heights
            // scale with the stretch so every aspect ratio holds.
            val avail = (containerW - (row.size - 1) * gapPx).coerceAtLeast(0f)
            // The row is justified by its TOTAL aspect width. The previous
            // formula multiplied the width scale by rowHeight again, so a
            // narrow portrait row could become hundreds of thousands of px
            // tall (the reported 537636px crash). Derive height directly:
            // totalWidth = rowHeight * sum(aspects) = avail.
            val sum = row.fold(0f) { acc, item -> acc + item.aspect }
            val h = if (sum > 0f) avail / sum else rowHPx
            var x = 0f
            val tiles = row.map { item ->
                val w = h * item.aspect
                val tile = GalleryTile(item.uri, x, y, w, h)
                x += w + gapPx
                tile
            }
            y += h + gapPx
            GalleryRow(tiles, h)
        }
    }
    val totalH = (y - gapPx).coerceAtLeast(0f)
    return GalleryLayout(rows, totalH)
}

/**
 * The gallery's visible slice of the window — what the user can actually
 * see on screen. [leftInGallery]/[topInGallery] are the slice's top-left
 * in GALLERY pixels (what the zoom overlay's tile coordinates live in),
 * [widthPx]/[heightPx] its size.
 *
 * With many images the gallery is taller than the window; zooming must
 * center on THIS slice, not the whole gallery, or the magnified image
 * lands mid-gallery — off the visible screen and clipped by the page
 * scroll.
 */
private data class VisibleViewport(
    val leftInGallery: Float,
    val topInGallery: Float,
    val widthPx: Float,
    val heightPx: Float
)

/**
 * Intersects the gallery's window rect with the window rect to find the
 * visible slice. Degenerate/empty intersections clamp to a 1px viewport so
 * the zoom never divides by zero.
 */
private fun visibleViewportOf(
    galleryLeft: Float,
    galleryTop: Float,
    galleryW: Float,
    galleryH: Float,
    windowW: Float,
    windowH: Float
): VisibleViewport {
    val left = maxOf(0f, galleryLeft)
    val top = maxOf(0f, galleryTop)
    val right = minOf(windowW, galleryLeft + galleryW)
    val bottom = minOf(windowH, galleryTop + galleryH)
    val w = (right - left).coerceAtLeast(1f)
    val h = (bottom - top).coerceAtLeast(1f)
    return VisibleViewport(
        leftInGallery = left - galleryLeft,
        topInGallery = top - galleryTop,
        widthPx = w,
        heightPx = h
    )
}

/**
 * Header-only decode of an attachment's aspect ratio (w/h), EXIF-rotated to
 * match Coil's on-screen rendering. Content URIs resolve through the
 * ContentResolver; file paths decode directly. Falls back to 1f (square)
 * when the image can't be read. Runs on [Dispatchers.IO] — metadata-only,
 * no pixel data is decoded.
 */
private suspend fun imageAspectOf(context: Context, uriString: String): Float {
    val size = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(uriString)
            val scheme = uri.scheme
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            when (scheme) {
                "content" -> context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)
                }
                "file" -> BitmapFactory.decodeFile(uri.path, opts)
                else -> BitmapFactory.decodeFile(uriString, opts)
            }
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return@runCatching null
            var w = opts.outWidth
            var h = opts.outHeight
            // Photos shot sideways carry EXIF rotation; Coil renders them
            // rotated, so swap the raw sensor bounds to match the on-screen
            // aspect (same treatment the mood-board tiles use).
            val orientation = runCatching {
                when (scheme) {
                    "content" -> context.contentResolver.openInputStream(uri)?.use { stream ->
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    }
                    // uri.path is nullable — resolve through let so the
                    // decode falls back to the normal-orientation default.
                    "file" -> uri.path?.let { path ->
                        ExifInterface(path).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    }
                    else -> null
                }
            }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
            val rotationDeg = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            if (rotationDeg == 90 || rotationDeg == 270) {
                val swap = w
                w = h
                h = swap
            }
            w.toFloat() / h.toFloat()
        }.getOrNull()
    }
    return size ?: 1f
}
