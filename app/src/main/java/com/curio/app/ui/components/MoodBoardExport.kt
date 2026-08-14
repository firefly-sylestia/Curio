package com.curio.app.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.curio.app.data.CaptureData
import com.curio.app.data.CurioCategory
import com.curio.app.ui.theme.categorySurfaceMoodBoard
import com.curio.app.ui.theme.themedAccent
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-quality mood-board PNG export.
 *
 * Renders the FULL saved board — tinted surface + watermark backdrop + the
 * tile collage + floating on-board quote cards, and below it the caption and
 * below-board quote boxes — into a large off-screen [ComposeView] captured
 * to an ARGB PNG at a high pixel size, so the exported image stays crisp
 * even when zoomed in. The collage tiles are preloaded as full-resolution
 * [Bitmap]s BEFORE the capture (never async Coil painters), so the exported
 * image is deterministic — no blank tiles from an in-flight load.
 *
 * Two actions share one render: [saveMoodBoardPng] writes it to the gallery
 * via MediaStore (API 29+) or app-external Pictures + media scan (26–28);
 * [shareMoodBoardPng] hands it to the system share sheet via FileProvider.
 */
object MoodBoardExport {

    /**
     * v57 — which arrangement a Save/Share PNG renders: the INLINE layout
     * (what the small saved card shows) or the FULL-SCREEN layout (what the
     * expanded dialog shows). Each is saved separately on the entry.
     */
    enum class MoodBoardLayout { INLINE, FULL }

    /** Requested PNG long side in px — capped below by the device pixel budget. */
    private const val EXPORT_LONG_SIDE = 4096

    /** Maximum output bitmap budget; the runtime budget may reduce this further. */
    private const val MAX_EXPORT_BITMAP_BYTES = 64L * 1024L * 1024L

    /** Short-side floor for the full-bleed canvas — a very wide/short board
     *  can't collapse the other axis below this. */
    private const val MIN_EXPORT_SIDE = 1000

    /**
     * Renders the board and saves the PNG to the gallery. [onDone] is invoked
     * on the main thread with the saved file path (or null on failure).
     */
    fun saveMoodBoardPng(
        context: Context,
        authority: String,
        data: CaptureData.GalleryWall,
        category: CurioCategory,
        boardSeed: Int,
        entryId: String,
        layout: MoodBoardLayout = MoodBoardLayout.INLINE,
        onDone: (String?) -> Unit
    ) {
        // exportBoard is suspend (preloads bitmaps off-thread, renders on the
        // main thread), so the save must run inside its own scope like the
        // share path — plain callers just get [onDone] on the main thread.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope.launch {
            val path = exportBoard(context, data, category, boardSeed, entryId, layout) { bitmap, fileName ->
                withContext(Dispatchers.IO) {
                    val p = saveBitmapToGallery(context, bitmap, fileName)
                    bitmap.recycle()
                    p
                }
            }
            onDone(path)
        }
    }

    /**
     * Renders the board and launches the share sheet with the PNG attached.
     * [onDone] fires on the main thread after the chooser opens.
     */
    fun shareMoodBoardPng(
        context: Context,
        authority: String,
        data: CaptureData.GalleryWall,
        category: CurioCategory,
        boardSeed: Int,
        entryId: String,
        layout: MoodBoardLayout = MoodBoardLayout.INLINE,
        onDone: () -> Unit
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope.launch {
            val path = exportBoard(context, data, category, boardSeed, entryId, layout) { bitmap, fileName ->
                withContext(Dispatchers.IO) {
                    try {
                        val file = File(context.cacheDir, "share").apply { mkdirs() }
                        val dest = File(file, fileName)
                        FileOutputStream(dest).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        dest.absolutePath
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
            if (path != null) {
                val uri = FileProvider.getUriForFile(context, authority, File(path))
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching {
                    context.startActivity(Intent.createChooser(intent, "Share mood board"))
                }
            }
            onDone()
        }
    }

    /**
     * Shared pipeline: preload tile bitmaps off-thread → render the board
     * composable off-screen at export resolution → hand the bitmap + file name
     * to [emit]. Returns the [emit] result on the main thread.
     */
    private suspend fun exportBoard(
        context: Context,
        data: CaptureData.GalleryWall,
        category: CurioCategory,
        boardSeed: Int,
        entryId: String,
        layout: MoodBoardLayout,
        emit: suspend (Bitmap, String) -> String?
    ): String? {
        // v57 — render the chosen arrangement: the FULL layout when asked
        // (falling back to the inline one for legacy entries that only ever
        // saved a single board). Bitmaps preload against the SAME tile list
        // the render draws, so indices always line up.
        // v60 — Gson bypasses Kotlin defaults, so these fields can decode to
        // null: guard every isEmpty()/map with orEmpty() to avoid the
        // Collection.isEmpty() crash on legacy boards.
        val renderLayouts = if (layout == MoodBoardLayout.FULL && data.tileLayoutsFull.orEmpty().isNotEmpty())
            data.tileLayoutsFull.orEmpty() else data.tileLayouts.orEmpty()
        // Preload every collage image as a full-size software bitmap so the
        // off-screen capture never races an async Coil load. Always recycled
        // when the export finishes — including on any failure/early-return
        // path — so a bad render never leaks ARGB memory.
        // v7.27 — memory cache DISABLED: the preloaded bitmaps must NOT be
        // the same instances Coil's memory cache hands to the UI's
        // AsyncImagePainters. The old code preloaded straight from the shared
        // cache and then recycled them in the finally — poisoning the cache,
        // so the next time a board was expanded (or any tile re-drew), Coil
        // served the recycled bitmap and the app crashed with "Canvas:
        // trying to use a recycled bitmap". A fresh decode owned by the
        // export is safe to recycle without touching the UI's cache.
        val bitmaps = withContext(Dispatchers.IO) {
            renderLayouts.map { t ->
                runCatching {
                    val request = ImageRequest.Builder(context)
                        .data(t.uri)
                        // v8.2 — cap the preload at the export canvas size
                        // (EXPORT_LONG_SIDE): the output can never exceed
                        // this, so a 48MP photo no longer allocates a
                        // multi-hundred-MB bitmap per tile (a full board
                        // could OOM before the render). Detail above the cap
                        // is invisible anyway. Keep the zoom overlay's
                        // MoodBoardZoomDecodePx >= this value so the on-screen
                        // magnifier never shows less detail than the PNG.
                        .size(EXPORT_LONG_SIDE, EXPORT_LONG_SIDE)
                        .allowHardware(false)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build()
                    (context.imageLoader.execute(request).drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                }.getOrNull()
            }
        }

        return try {
            withContext(Dispatchers.Main) {
                val fileName = "moodboard_${entryId.take(8)}.png"
                val bitmap = renderBoardBitmap(context, data, category, boardSeed, bitmaps, layout)
                    ?: return@withContext null
                emit(bitmap, fileName)
            }
        } finally {
            bitmaps.forEach { it?.recycle() }
        }
    }

    /**
     * Renders the whole saved mood board into an ARGB bitmap at export
     * resolution. The board is drawn by a real composition pass in an
     * off-screen [ComposeView] (same technique as [shareComposableCard]) so
     * the exported image is a faithful, theme-accurate picture of the saved
     * entry — surface color, watermark backdrop, collage and floating quote
     * cards included, full-bleed edge to edge.
     */
    private suspend fun renderBoardBitmap(
        context: Context,
        data: CaptureData.GalleryWall,
        category: CurioCategory,
        boardSeed: Int,
        bitmaps: List<Bitmap?>,
        layout: MoodBoardLayout
    ): Bitmap? {
        // v7.27 — FULL-BLEED export: the canvas mirrors the BOARD's own
        // aspect ratio (not a fixed 3:4 card), so the board fills the image
        // edge to edge — exactly the expanded full-screen view — with no
        // header, caption or below-board quote boxes around it.
        // v57 — the extent comes from the SAME arrangement being rendered.
        val renderLayouts = if (layout == MoodBoardLayout.FULL && data.tileLayoutsFull.orEmpty().isNotEmpty())
            data.tileLayoutsFull.orEmpty() else data.tileLayouts.orEmpty()
        val maxX = renderLayouts.maxOfOrNull { it.offsetXPx + it.widthPx } ?: 0f
        val maxY = renderLayouts.maxOfOrNull { it.offsetYPx + it.heightPx } ?: 0f
        val (width, height) = exportCanvasSize(maxX, maxY)

        return withContext(Dispatchers.Main) {
            val lifecycleOwner = SyntheticLifecycleOwner()
            val composeView = ComposeView(context).apply {
                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                setContent {
                    // Fixed export density (4x = 1dp → 4px): the same dp values
                    // resolve to the same relative size on EVERY device, so the
                    // exported image's proportions are identical everywhere —
                    // not dependent on the phone's own density.
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalDensity provides androidx.compose.ui.unit.Density(4f, 1f)
                    ) {
                        MoodBoardShareCard(
                            data = data,
                            category = category,
                            boardSeed = boardSeed,
                            bitmaps = bitmaps,
                            layout = layout
                        )
                    }
                }
            }
            // v7.26 — Android 16 crash fix: a ComposeView that is never
            // attached to a window cannot resolve a windowRecomposer, and
            // measure() → ensureCompositionCreated → getWindowRecomposer
            // throws IllegalStateException ("Cannot locate windowRecomposer").
            // Host the off-screen view inside an INVISIBLE FrameLayout added
            // to the activity's decor so it IS window-attached, then tear the
            // host down right after the capture — no flicker, no leak.
            val host = android.widget.FrameLayout(context).apply {
                visibility = View.INVISIBLE
                addView(composeView, ViewGroup.LayoutParams(width, height))
            }
            val decor = (context as? android.app.Activity)?.window?.decorView as? ViewGroup
            if (decor == null) {
                // No window to attach to (non-Activity context) — the measure()
                // below would throw "Cannot locate windowRecomposer" again, so
                // fail the export gracefully instead of crashing.
                return@withContext null
            }
            decor.addView(host, ViewGroup.LayoutParams(width, height))
            val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            composeView.measure(widthSpec, heightSpec)
            composeView.layout(0, 0, width, height)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            // suspendCoroutine lives in the Kotlin STDLIB (kotlin.coroutines),
            // so it is identical in every kotlinx-coroutines version — unlike
            // suspendCancellableCoroutine whose onCancellation parameter
            // flips between optional (<1.9) and required (1.9+) and broke the
            // CI build on both forms.
            // Plain suspendCoroutine: it has NO onCancellation parameter in ANY
            // coroutines version (unlike suspendCancellableCoroutine, whose
            // onCancellation is optional in <1.9 but REQUIRED in 1.9+ — which
            // is why the CI build failed on both forms). The capture always
            // runs to completion in the posted frame, so there is nothing to
            // cancel. resumeWith is the Continuation INTERFACE method (stdlib,
            // every version) — no kotlinx extension import needed.
            val result = kotlin.coroutines.suspendCoroutine<Bitmap?> { cont ->
                composeView.post {
                    var bmp: Bitmap? = null
                    try {
                        val allocated = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bmp = allocated
                        val canvas = android.graphics.Canvas(allocated)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        composeView.draw(canvas)
                        cont.resumeWith(Result.success(allocated))
                        bmp = null // ownership moves to the save/share emitter
                    } catch (t: Throwable) {
                        // Never crash the caller — a capture failure reports
                        // as a failed save/share instead. Recycle a bitmap
                        // allocated before a draw failure so a large failed
                        // export cannot remain resident in the heap.
                        bmp?.recycle()
                        cont.resumeWith(Result.success(null))
                    } finally {
                        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                        // Detach the invisible host so the window is left
                        // exactly as we found it.
                        runCatching { decor.removeView(host) }
                    }
                }
            }
            result
        }
    }

    /**
     * Full-bleed canvas for the board's raw extent: the LONG side targets
     * [EXPORT_LONG_SIDE] and the other axis follows the board's own aspect.
     * The final ARGB bitmap is constrained by a device-aware memory budget,
     * reserving heap for preloaded tile bitmaps and Compose so the largest
     * practical output is used without making a 4096px capture an avoidable
     * out-of-memory failure. Empty/legacy layouts fall back to a
     * square.
     */
    private fun exportCanvasSize(maxX: Float, maxY: Float): Pair<Int, Int> {
        if (maxX <= 0f || maxY <= 0f) return safeExportSize(EXPORT_LONG_SIDE, EXPORT_LONG_SIDE)
        val aspect = maxX / maxY
        val ideal = if (aspect >= 1f) {
            EXPORT_LONG_SIDE to (EXPORT_LONG_SIDE / aspect).toInt().coerceAtLeast(MIN_EXPORT_SIDE)
        } else {
            (EXPORT_LONG_SIDE * aspect).toInt().coerceAtLeast(MIN_EXPORT_SIDE) to EXPORT_LONG_SIDE
        }
        return safeExportSize(ideal.first, ideal.second)
    }

    private fun safeExportSize(width: Int, height: Int): Pair<Int, Int> {
        val maxBytes = minOf(
            MAX_EXPORT_BITMAP_BYTES,
            // Leave most of the app heap available for preloaded tile bitmaps,
            // Compose, and the temporary encoder buffers.
            Runtime.getRuntime().maxMemory() / 8L
        ).coerceAtLeast(4L)
        val maxPixels = (maxBytes / 4L).coerceAtLeast(1L)
        val requestedPixels = width.toLong() * height.toLong()
        if (requestedPixels <= maxPixels) return width to height

        val scale = sqrt(maxPixels.toDouble() / requestedPixels.toDouble())
        return (width * scale).toInt().coerceAtLeast(1) to
            (height * scale).toInt().coerceAtLeast(1)
    }

    /**
     * Writes [bitmap] to the gallery. MediaStore insert on API 29+ (no
     * permission needed); app-external Pictures + media scan on 26–28.
     */
    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Curio"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri: Uri? = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
                uri ?: return@runCatching null
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                } catch (t: Throwable) {
                    // Never leave a ghost IS_PENDING row hidden from the
                    // gallery — delete the entry on any write failure.
                    runCatching { context.contentResolver.delete(uri, null, null) }
                    throw t
                }
                uri.toString()
            }.getOrNull()
        } else {
            runCatching {
                val dir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Curio"
                ).apply { mkdirs() }
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                MediaScannerConnection.scanFile(
                    context, arrayOf(file.absolutePath), arrayOf("image/png"), null
                )
                file.absolutePath
            }.getOrNull()
        }

    /**
     * Minimal [LifecycleOwner] for off-screen [ComposeView] instances — same
     * synthetic registry [shareComposableCard] uses to drive composition.
     */
    private class SyntheticLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
        fun handleLifecycleEvent(event: Lifecycle.Event) {
            registry.handleLifecycleEvent(event)
        }
    }
}

/**
 * Self-contained FULL-BLEED render of the saved mood board — exactly the
 * expanded full-screen board view, composed for off-screen capture:
 *
 *  1. the category-tinted board surface with the seeded watermark backdrop
 *     BEHIND everything (visible through the gaps, never covered by an
 *     opaque collage box — the old card put an opaque background on the
 *     collage container, which is why the watermark didn't render),
 *  2. the tile collage laid out at the board's OWN coordinates, scaled so
 *     it fills the canvas edge to edge (the canvas mirrors the board's
 *     aspect ratio — see [MoodBoardExport.exportCanvasSize]),
 *  3. floating on-board quote cards at their saved spots.
 *
 * No header, no caption slip, no below-board quote boxes — the board and
 * only the board, edge to edge. Tiles draw from [bitmaps] (preloaded) so
 * the capture never waits on Coil.
 */
@Composable
private fun MoodBoardShareCard(
    data: CaptureData.GalleryWall,
    category: CurioCategory,
    boardSeed: Int,
    bitmaps: List<Bitmap?>,
    layout: MoodBoardExport.MoodBoardLayout
) {
    val density = LocalDensity.current
    // v57 — the export renders the chosen arrangement: the FULL layout when
    // asked (falling back to the inline one for legacy entries), with the
    // full-screen quote placements alongside.
    val renderLayouts = if (layout == MoodBoardExport.MoodBoardLayout.FULL && data.tileLayoutsFull.orEmpty().isNotEmpty())
        data.tileLayoutsFull.orEmpty() else data.tileLayouts.orEmpty()
    val renderQuotePositions = if (layout == MoodBoardExport.MoodBoardLayout.FULL && data.quotePositionsFull.orEmpty().isNotEmpty())
        data.quotePositionsFull.orEmpty() else data.quotePositions.orEmpty()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(category.categorySurfaceMoodBoard())
    ) {
        // ── Seeded watermark backdrop — the whole collage background, kept
        // BEHIND the tiles (no opaque container on top of it).
        CurioMoodBoardBackdrop(
            seed = boardSeed,
            accent = category.themedAccent(),
            modifier = Modifier.fillMaxSize()
        )

        // ── Full-bleed collage — the board's own coordinates scaled to the
        // canvas (aspect-matched, so the board fills it edge to edge).
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val viewW = maxWidth.value * density.density
            val viewH = maxHeight.value * density.density
            if (renderLayouts.isNotEmpty() && viewW > 0f && viewH > 0f) {
                val maxX = renderLayouts.maxOfOrNull { it.offsetXPx + it.widthPx } ?: 0f
                val maxY = renderLayouts.maxOfOrNull { it.offsetYPx + it.heightPx } ?: 0f
                val scale = if (maxX > 0f && maxY > 0f) {
                    (viewW / maxX).coerceAtMost(viewH / maxY)
                } else 1f
                val boardW = maxX * scale
                val boardH = maxY * scale
                val offsetX = (viewW - boardW) / 2f
                val offsetY = (viewH - boardH) / 2f

                Box(
                    modifier = Modifier.offset {
                        IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
                    }
                ) {
                    renderLayouts.forEachIndexed { i, tile ->
                        val bmp = bitmaps.getOrNull(i)
                        if (bmp == null) return@forEachIndexed
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (tile.offsetXPx * scale).roundToInt(),
                                        (tile.offsetYPx * scale).roundToInt()
                                    )
                                }
                                .size(
                                    width = with(density) { (tile.widthPx * scale).toDp() },
                                    height = with(density) { (tile.heightPx * scale).toDp() }
                                )
                                .rotate(tile.rotationDeg)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                // v8.2 — smooth upscaling when a tile is
                                // rendered larger than its bitmap; the default
                                // Low filter made small tiles look pixelated in
                                // the saved PNG.
                                filterQuality = FilterQuality.High,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                    }

                    // Floating on-board quote cards at their saved spots.
                    if (data.quotes.orEmpty().isNotEmpty()) {
                        MoodBoardFloatingCards(
                            quotes = data.quotes.orEmpty(),
                            styles = data.quoteStyles.orEmpty(),
                            colors = data.quoteColors.orEmpty(),
                            tilts = data.quoteTilts.orEmpty(),
                            positions = renderQuotePositions,
                            onBoard = data.quoteOnBoard.orEmpty(),
                            canvasWPx = maxX,
                            canvasHPx = maxY,
                            boardScale = scale
                        )
                    }
                }
            }
        }
    }
}
