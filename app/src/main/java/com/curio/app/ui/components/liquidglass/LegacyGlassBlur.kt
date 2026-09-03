package com.curio.app.ui.components.liquidglass

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.ui.theme.isCurioDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min

/**
 * v264 — LEGACY GLASS BLUR: an APP-SIDE blur engine for pre-Android-12
 * devices, where the real glass recipe can't run (no RenderEffect). Instead
 * of the static faux veil, the pill's backdrop becomes the REAL page content:
 *
 *   1. [curioLegacyCapture] records the pages-only Box into our own Compose
 *      GraphicsLayer (same architecture as the Kyant LayerBackdrop — the
 *      floating nav / reveal pills are SIBLINGS of this Box, so they never
 *      record themselves into their own backdrop).
 *   2. [CurioLegacyBlurSnapshotter] reads that layer back to pixels on a
 *      throttle (~5 updates/s, v331 — coalesced from ~8/s, see Tuning) and
 *      downscales it hard to ≤128px (a blurred image needs no resolution)
 *      and stack-blurs it in software. One pass costs ~1–3ms — nothing on
 *      any device that runs this app.
 *   3. [curioLegacyGlassCapsule] draws the blurred snapshot region under the
 *      pill (mapped through root coordinates), then layers the same veil /
 *      sheen / rim finish as the simulated glass so both paths read alike.
 *
 * What you get is real FROSTED glass — the content behind the bar genuinely
 * blurs and moves as you scroll. The per-pixel lens refraction of the API-31+
 * shader path is not reproducible without RenderEffect; everything else
 * carries over, including the Appearance Blur slider.
 *
 * If pixel readback fails on a given device (GraphicsLayer.toImageBitmap is
 * hardware-dependent), the engine latches off once and every capsule falls
 * back to the simulated veil — never a crash, never a blank pill.
 */
object CurioLegacyBlur {

    /** Latest blurred snapshot of the recorded page layer. Null = not ready. */
    var snapshot by mutableStateOf<ImageBitmap?>(null)
        internal set

    /** Pixel size of the area the snapshot covers (the captured Box). */
    var captureSize by mutableStateOf(IntSize.Zero)
        internal set

    /** The captured Box's origin in root coordinates (for pill mapping). */
    var captureOrigin by mutableStateOf(IntOffset.Zero)
        internal set

    /** Set once pixel readback has failed repeatedly — stop trying. */
    internal var readbackBroken = false

    /** v268 — how many consecutive failures before latching off. */
    internal var failureCount = 0

    /** Whether the engine can run at all right now (toggles + API + health). */
    fun isActive(): Boolean =
        AppPreferences.legacyGlassBlurState &&
            AppPreferences.liquidGlassPillsState &&
            Build.VERSION.SDK_INT in 26 until 31 && // 26+: Bitmap/Canvas paths used below
            !readbackBroken
}

/**
 * Records whatever content this modifier wraps into [layer] — attach it to
 * the SAME pages-only Box the Kyant capture marks (the floating bar stays
 * excluded by being a sibling of that Box).
 */
fun Modifier.curioLegacyCapture(layer: GraphicsLayer): Modifier =
    drawWithContent {
        if (CurioLegacyBlur.isActive()) {
            layer.record { this@drawWithContent.drawContent() }
        }
        drawContent()
    }

/**
 * Tracks the captured Box's geometry so pills can map themselves into the
 * snapshot; attach alongside [curioLegacyCapture].
 */
fun Modifier.curioLegacyCaptureGeometry(): Modifier =
    onGloballyPositioned { coords ->
        CurioLegacyBlur.captureOrigin = IntOffset(
            coords.positionInRoot().x.toInt(),
            coords.positionInRoot().y.toInt()
        )
        CurioLegacyBlur.captureSize = coords.size
    }

/**
 * The throttled snapshot loop. Each tick reads the recorded layer,
 * downscales to ≤[SNAPSHOT_MAX_DIM], applies two rounds of box blur
 * (≈ gaussian) and publishes the result.
 */
@Composable
fun CurioLegacyBlurSnapshotter(layer: GraphicsLayer) {
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(SNAPSHOT_INTERVAL_MS)
            if (!CurioLegacyBlur.isActive()) continue
            if (CurioLegacyBlur.captureSize.width < 1 || CurioLegacyBlur.captureSize.height < 1) continue
            runCatching {
                val full = layer.toImageBitmap()
                val src = full.asAndroidBitmap()
                val scale = min(
                    1f,
                    SNAPSHOT_MAX_DIM.toFloat() / max(src.width, src.height)
                )
                val sw = max(1, (src.width * scale).toInt())
                val sh = max(1, (src.height * scale).toInt())
                val small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(small)
                canvas.drawBitmap(
                    src,
                    null,
                    android.graphics.RectF(0f, 0f, sw.toFloat(), sh.toFloat()),
                    android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
                )
                stackBlur(small, BLUR_RADIUS_PX)
                // v268 — BLANK-SNAPSHOT GUARD: a readback that "succeeds" but
                // produces a fully-transparent image (some pre-12 software
                // paths replay nothing) used to be published as-is — the
                // pills then drew NOTHING behind them ("transparent but no
                // blur"). Treat blank output as a failure instead.
                val px = IntArray(sw * sh)
                small.getPixels(px, 0, sw, 0, 0, sw, sh)
                var anyOpaque = false
                for (p in px) {
                    if ((p ushr 24) > 8) { anyOpaque = true; break }
                }
                if (!anyOpaque) throw IllegalStateException("blank legacy readback")
                CurioLegacyBlur.failureCount = 0
                CurioLegacyBlur.snapshot = small.asImageBitmap()
            }.onFailure {
                // v268 — RETRY BUDGET: give the readback a few chances (an
                // early tick can race the layer's first record) before
                // latching off to the simulated veil for the session.
                CurioLegacyBlur.failureCount++
                if (CurioLegacyBlur.failureCount >= READBACK_MAX_FAILURES) {
                    CurioLegacyBlur.readbackBroken = true
                    CurioLegacyBlur.snapshot = null
                }
            }
        }
    }
}

/**
 * Draws REAL frosted glass over the capsule: the blurred page snapshot,
 * mapped through root coordinates so the pixels under the pill are exactly
 * what sits behind it, clipped to a stadium; then the shared veil + sheen +
 * rim finish so old and new devices read as one design.
 */
@Composable
fun Modifier.curioLegacyGlassCapsule(container: Color): Modifier {
    val dark = isCurioDarkTheme()
    var pillOrigin by remember { mutableStateOf(IntOffset.Zero) }
    return this
        .onGloballyPositioned { coords ->
            pillOrigin = IntOffset(
                coords.positionInRoot().x.toInt(),
                coords.positionInRoot().y.toInt()
            )
        }
        .drawWithContent {
            drawContent()
            val snap = CurioLegacyBlur.snapshot
            val capSize = CurioLegacyBlur.captureSize
            val capOrigin = CurioLegacyBlur.captureOrigin
            if (snap != null && capSize.width > 0 && capSize.height > 0) {
                clipPath(
                    Path().apply {
                        addOutline(CircleShape.createOutline(size, layoutDirection, this@drawWithContent))
                    }
                ) {
                    translate(
                        -(pillOrigin.x - capOrigin.x).toFloat(),
                        -(pillOrigin.y - capOrigin.y).toFloat()
                    ) {
                        drawImage(snap, dstSize = capSize)
                    }
                }
            } else {
                // Snapshot not ready yet (first ~200ms) — a soft container
                // wash instead of a flash of empty glass.
                drawRoundRect(
                    color = container.copy(alpha = 0.88f),
                    cornerRadius = CornerRadius(minOf(size.width, size.height) / 2f)
                )
            }
            // Shared finish — identical recipe to the simulated glass so the
            // two fallbacks read as one design language.
            val veilScale = 0.30f + 0.70f * AppPreferences.glassBlurScaleState.coerceIn(0f, 2f)
            val veilBase = if (dark) 0.05f else 0.34f
            val sheenAlpha = if (dark) 0.22f else 0.75f
            val rimAlpha = if (dark) 0.32f else 0.90f
            val r = CornerRadius(minOf(size.width, size.height) / 2f)
            drawRoundRect(color = Color.White.copy(alpha = veilBase * veilScale), cornerRadius = r)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = sheenAlpha),
                        Color.Transparent,
                        Color.Transparent,
                        Color.White.copy(alpha = sheenAlpha * 0.4f)
                    )
                ),
                cornerRadius = r
            )
            drawRoundRect(
                color = Color.White.copy(alpha = rimAlpha),
                cornerRadius = r,
                style = Stroke(width = 1.dp.toPx())
            )
        }
}

/**
 * Pure-Kotlin fast stack blur: per-row and per-column sliding-window box
 * averages, two rounds (≈ gaussian). Operates entirely on an IntArray — no
 * RenderScript, no API-level concerns. O(pixels × passes), tiny at the
 * snapshot scale this engine uses.
 */
internal fun stackBlur(bitmap: Bitmap, radius: Int) {
    if (radius < 1) return
    val w = bitmap.width
    val h = bitmap.height
    var pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val div = radius * 2 + 1

    fun blurLine(get: (Int) -> Int, length: Int): IntArray {
        val out = IntArray(length)
        var sa = 0; var sr = 0; var sg = 0; var sb = 0
        for (i in -radius..radius) {
            val p = get(i.coerceIn(0, length - 1))
            sa += p ushr 24
            sr += (p shr 16) and 0xFF
            sg += (p shr 8) and 0xFF
            sb += p and 0xFF
        }
        for (i in 0 until length) {
            out[i] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
            val addP = get((i + radius + 1).coerceAtMost(length - 1))
            val remP = get((i - radius).coerceAtLeast(0))
            sa += (addP ushr 24) - (remP ushr 24)
            sr += ((addP shr 16) and 0xFF) - ((remP shr 16) and 0xFF)
            sg += ((addP shr 8) and 0xFF) - ((remP shr 8) and 0xFF)
            sb += (addP and 0xFF) - (remP and 0xFF)
        }
        return out
    }

    repeat(BLUR_ROUNDS) {
        val hOut = IntArray(w * h)
        for (y in 0 until h) {
            val line = blurLine({ x -> pixels[y * w + x] }, w)
            System.arraycopy(line, 0, hOut, y * w, w)
        }
        val vOut = IntArray(w * h)
        for (x in 0 until w) {
            val line = blurLine({ y -> hOut[y * w + x] }, h)
            for (y in 0 until h) vOut[y * w + x] = line[y]
        }
        pixels = vOut
    }
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
}

// ── Tuning ──────────────────────────────────────────────────────────────
// v331 — SNAPSHOT COALESCING (logcat analysis: this full-screen readback +
// downscale + stack-blur loop was the app-code hot allocator on the
// pre-12 path — one ~160px-wide pass was cheap, but at 8 ticks/s it churned
// a fresh Bitmap + IntArray per tick even while the screen sat still).
// Cadence drops to ~5/s (scroll-following lag stays imperceptible behind
// blur radii this large — the backdrop is frosted, not a video) and the
// working resolution drops to 128px (a blurred image needs none; 128²/160²
// is a ~36% smaller readback + blur pass, and the frost reads marginally
// creamier at the same radius). Together ~60% less per-second allocation
// on the only app-owned glass snapshot path.
private const val SNAPSHOT_INTERVAL_MS = 200L
private const val SNAPSHOT_MAX_DIM = 128
private const val BLUR_RADIUS_PX = 6
private const val BLUR_ROUNDS = 2
private const val READBACK_MAX_FAILURES = 4
