package com.curio.app.infrastructure

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.view.WindowManager
import com.curio.app.data.SessionShots

/**
 * v27 — captures ONE screen frame via MediaProjection and saves it as a
 * PNG in app-private storage.
 *
 * The explore bubble's screenshot button runs the system consent dialog
 * ([ScreenCaptureRequestActivity]) and hands the granted token to
 * [ExploreSessionService]; this helper does the actual capture on the
 * service's background thread: a VirtualDisplay mirrored to an
 * [ImageReader], one frame acquired, converted to a bitmap and stored via
 * [SessionShots]. The projection is stopped immediately after the single
 * frame — the bubble only ever needs one still per tap.
 *
 * NOTE — Android 14+ requires the capturing app to have a foreground
 * service running with the mediaProjection type (and a registered
 * [MediaProjection.Callback]) before `createVirtualDisplay`; the explore
 * service is already foreground and [ExploreSessionService] promotes its
 * FGS type for the duration of the capture.
 */
object ScreenFrameCapturer {

    private const val TAG = "ScreenFrameCapturer"
    // How long to wait for the first mirrored frame before giving up.
    private const val FRAME_TIMEOUT_MS = 2_500L

    /**
     * Blocks the calling (background) thread until one frame is captured.
     * Returns the saved absolute path, or null when consent/capture fails.
     */
    fun capture(context: Context, resultCode: Int, data: Intent): String? {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = try {
            mpm.getMediaProjection(resultCode, data)
        } catch (e: Exception) {
            Log.e(TAG, "getMediaProjection failed", e)
            null
        }
        if (projection == null) return null
        var virtualDisplay: VirtualDisplay? = null
        var reader: ImageReader? = null
        return try {
            // Android 14+ requirement — register a callback before
            // createVirtualDisplay; we stop the projection right after the
            // frame so no lifecycle beyond onStop is needed.
            projection.registerCallback(object : MediaProjection.Callback() {}, null)

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val bounds = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                wm.maximumWindowMetrics.bounds
            } else {
                val dm = context.resources.displayMetrics
                android.graphics.Rect(0, 0, dm.widthPixels, dm.heightPixels)
            }
            val width = bounds.width()
            val height = bounds.height()
            val density = context.resources.displayMetrics.densityDpi

            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = projection.createVirtualDisplay(
                "curio-session-shot",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null
            )

            // Poll for the first mirrored frame (ImageReader fills asynchronously).
            var image: Image? = null
            val deadline = System.currentTimeMillis() + FRAME_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline && image == null) {
                image = reader.acquireLatestImage()
                if (image == null) Thread.sleep(40)
            }
            val path = image?.use { img ->
                toBitmap(img)?.let { bmp -> SessionShots.save(context, bmp) }
            }
            path
        } catch (e: Exception) {
            Log.e(TAG, "screen capture failed", e)
            null
        } finally {
            runCatching { virtualDisplay?.release() }
            runCatching { reader?.close() }
            runCatching { projection.stop() }
        }
    }

    /** Converts an RGBA_8888 [Image] plane into a cropped [Bitmap]. */
    private fun toBitmap(image: Image): Bitmap? = try {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val padded = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        padded.copyPixelsFromBuffer(buffer)
        Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
    } catch (e: Exception) {
        Log.e(TAG, "bitmap conversion failed", e)
        null
    }
}
