package com.curio.app.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import java.io.File
import java.io.FileOutputStream

/**
 * Captures the [card] composable as a PNG bitmap and launches an
 * [Intent.ACTION_SEND] chooser to share it.
 *
 * The composable is rendered off-screen in a temporary [ComposeView] that
 * is given its own [LifecycleOwner] so Compose can drive composition.
 * After a layout pass, the view is drawn to a [Bitmap], saved as PNG in
 * the app's share cache directory, and shared via [FileProvider].
 *
 * Old share images are cleaned up before creating a new one.
 *
 * Export density defaults to the device density so existing entry share cards
 * keep their established output. Promo exports opt into a fixed 4x density,
 * which keeps their logical dp layout stable while producing a large, sharp
 * PNG on low-density and high-density devices alike.

 * @param exportDensity Optional pixel density for the export. Leave null to
 *                      use the device density; promo art uses 4x.
 *
 * @param context   Android context (Activity recommended for lifecycle).
 * @param cardSize  Fixed dimensions for the rendered card (e.g. 400×400 dp).
 * @param authority FileProvider authority string (usually `package.fileprovider`).
 * @param card      @Composable lambda that renders the self-contained share card.
 * @param onShared  Optional callback invoked after the chooser is launched.
 */
fun shareComposableCard(
    context: Context,
    cardSize: DpSize,
    authority: String,
    card: @Composable () -> Unit,
    onShared: () -> Unit = {},
    exportDensity: Float? = null
) {
    val density = exportDensity
        ?.coerceAtLeast(1f)
        ?: context.resources.displayMetrics.density.coerceAtLeast(1f)
    val fontScale = context.resources.configuration.fontScale
    val widthPx  = dpToExportPx(cardSize.width, density)
    val heightPx = dpToExportPx(cardSize.height, density)

    // Clean up old share PNGs so the cache doesn't grow unbounded.
    val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
    shareDir.listFiles()?.forEach { it.delete() }

    // Create a synthetic LifecycleOwner so ComposeView has the lifecycle
    // it needs to drive composition even though it's off-screen.
    val lifecycleOwner = SyntheticLifecycleOwner()

    val composeView = ComposeView(context).apply {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale)
            ) {
                card()
            }
        }
    }

    // v7.26 — Android 16 crash fix (same as MoodBoardExport): a ComposeView
    // that is never attached to a window cannot resolve a windowRecomposer,
    // and measure() → getWindowRecomposer throws IllegalStateException.
    // Host the off-screen view inside an INVISIBLE FrameLayout attached to
    // the activity's decor so it IS window-attached, then remove the host
    // right after the capture — no flicker, no leak.
    val host = android.widget.FrameLayout(context).apply {
        visibility = android.view.View.INVISIBLE
        addView(composeView, ViewGroup.LayoutParams(widthPx, heightPx))
    }
    val decor = (context as? android.app.Activity)?.window?.decorView as? ViewGroup
    if (decor == null) {
        // No window to attach to (non-Activity context) — measure() would
        // throw "Cannot locate windowRecomposer" again, so bail out.
        return
    }
    decor.addView(host, ViewGroup.LayoutParams(widthPx, heightPx))
    val widthSpec  = MeasureSpec.makeMeasureSpec(widthPx,  MeasureSpec.EXACTLY)
    val heightSpec = MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY)
    composeView.measure(widthSpec, heightSpec)
    composeView.layout(0, 0, widthPx, heightPx)

    // Move lifecycle to STARTED so Compose begins composition.
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

    // Post the capture to run after the layout/render pass completes.
    composeView.post {
        try {
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            // v292g — no white fill: the card's gradient fills the entire
            // canvas so no transparent area leaks. Removing the white
            // ensures the exported PNG has no white corners or border.
            composeView.draw(canvas)

            // Save PNG to share cache.
            val file = File(shareDir, "curio_share_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            bitmap.recycle()

            // Build and launch share intent.
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share your Curio card"))
            onShared()
        } finally {
            // Clean up the synthetic lifecycle + detach the invisible host so
            // the window is left exactly as we found it.
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            runCatching { decor.removeView(host) }
        }
    }
}

private fun dpToExportPx(dp: Dp, density: Float): Int =
    (dp.value * density).toInt().coerceAtLeast(1)

/**
 * Minimal [LifecycleOwner] for off-screen [ComposeView] instances.
 *
 * Compose requires a lifecycle to drive recomposition. This synthetic owner
 * provides the minimum lifecycle events needed to compose a view tree,
 * capture it to a bitmap, and tear it down.
 */
private class SyntheticLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = registry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        registry.handleLifecycleEvent(event)
    }
}
