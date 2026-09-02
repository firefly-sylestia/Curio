package com.curio.app.ui.theme

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelCopy
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.drawToBitmap
import com.curio.app.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.hypot

// ============================================================================
// Telegram / mpvRx-style theme transition — a soft, feathered circular reveal.
//
// When the user flips light<->dark, the current (old) frame is frozen and a
// faithful snapshot of it stays on screen while the new theme recomposes
// underneath. The frozen frame is then peeled away by an iris wipe that
// mutates into a feathered radial alpha mask, exactly like mpvRx's blur-based
// theme switch and Telegram's light/dark flip. It is always-on (per the user,
// matching the existing always-on sun/moon quick toggle) — no Settings gate.
// ============================================================================

/** Holds the state of an in-flight theme-switch reveal and drives its timing. */
class CurioThemeTransitionState {
    var isAnimating by mutableStateOf(false)
        private set
    var revealCenter by mutableStateOf(Offset.Zero)
        private set
    var screenshotBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var progress = Animatable(0f)
        private set

    private var captureView: View? = null
    /**
     * Optional Compose GraphicsLayer that mirrors the host content (recorded
     * each frame via [captureLayerModifier]). Preferred over [captureView]
     * because [GraphicsLayer.toImageBitmap] uses hardware rendering and
     * preserves RenderEffect / blur (liquid-glass) layers, whereas
     * `View.drawToBitmap` forces a software pass that drops those effects
     * and can return a blank frame — the root cause of the theme reveal
     * silently skipping in liquid-glass mode.
     */
    internal var captureLayer: GraphicsLayer? = null

    /** Attach the host view used to snapshot the old frame. Null detaches. */
    fun attachView(view: View?) {
        captureView = view
    }

    /**
     * Freeze the current frame and arm a reveal expanding from [center].
     * Ignores a start while an animation is already in flight. Captures the
     * REAL window frame first via [PixelCopy] (works with liquid-glass
     * RenderEffect blurs), then the hardware [GraphicsLayer], then falls
     * back to [View.drawToBitmap]. If all fail or come back blank we bail
     * out silently so the theme switch still happens — the animation is
     * pure polish and must never block it.
     *
     * Suspending because [GraphicsLayer.toImageBitmap] may need to await a
     * frame; callers run it in a coroutine.
     */
    suspend fun startTransition(center: Offset): Boolean {
        if (isAnimating) return false
        // v3xx — REAL-FRAME capture first: PixelCopy reads the pixels the
        // user actually sees (liquid-glass blur included) straight from the
        // window surface, without re-running the Compose draw chain. The
        // GraphicsLayer fallback below re-invokes the whole app draw (nested
        // layer records over the kyant backdrop layer), which read back blank
        // in liquid-glass mode on some devices — so the reveal silently
        // skipped. Falls through to the view fallback, then bails to the
        // instant flip.
        val bitmap = windowFrame(captureView)
            ?: captureLayer?.let { captureLayerFrame(it) }
            ?: captureView?.let { captureViewFrame(it) }
            ?: return false
        if (bitmap.isBlank()) return false
        // Recreate (rather than snapTo, which is suspend) so the reveal
        // always begins from 0 on a new capture.
        progress = Animatable(0f)
        screenshotBitmap = bitmap
        revealCenter = center
        isAnimating = true
        return true
    }

    /**
     * Hardware snapshot via the Compose GraphicsLayer — preserves
     * RenderEffect / blur (liquid-glass) layers that [captureViewFrame]
     * would drop. Rejects blank readbacks.
     */
    private suspend fun captureLayerFrame(layer: GraphicsLayer): Bitmap? {
        return try {
            val bmp = layer.toImageBitmap().asAndroidBitmap()
            if (bmp.isBlank()) null else bmp
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Software snapshot via [View.drawToBitmap] — the legacy fallback when
     * no Compose GraphicsLayer is attached. Rejects failures and blank/
     * transparent captures (a device quirk where drawToBitmap "succeeds"
     * but replays nothing).
     */
    private fun captureViewFrame(view: View): Bitmap? {
        return try {
            val bmp = view.drawToBitmap()
            if (bmp.isBlank()) null else bmp
        } catch (e: Exception) {
            null
        }
    }

    /**
     * LIVE-FRAME capture via [PixelCopy]: a snapshot of the window's current
     * surface — the exact pixels on screen, liquid-glass blur included —
     * taken by the hardware compositor instead of re-running the Compose
     * draw chain. This is the PREFERRED snapshot source for the frozen
     * reveal frame; the GraphicsLayer / View paths below exist as fallbacks
     * (API < 26 or a failed copy). A failed or blank copy is rejected so the
     * caller falls back instead of freezing a see-through frame.
     */
    private suspend fun windowFrame(view: View?): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val activity = view?.context as? Activity
            ?: view?.rootView?.context as? Activity
            ?: return null
        val window = activity.window ?: return null
        val decor = window.decorView
        val width = decor.width
        val height = decor.height
        if (width <= 0 || height <= 0) return null
        return try {
            val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val frame = suspendCancellableCoroutine<Bitmap?> { cont ->
                // The callback always fires (PixelCopy has no cancellation
                // API), so the continuation can never leak.
                PixelCopy.request(
                    window,
                    dest,
                    { status ->
                        val result =
                            if (status == PixelCopy.SUCCESS && !dest.isBlank()) dest else null
                        if (!cont.isCancelled) {
                            runCatching { cont.resume(result) }
                        } else {
                            // Cancelled mid-copy: the continuation resumes
                            // with CancellationException on its own — just
                            // don't leak the allocation.
                            dest.recycle()
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            }
            if (frame != null) frame else dest.recycle().let { null }
        } catch (e: Exception) {
            null
        }
    }

    /** True when the bitmap is entirely transparent (no opaque pixels). */
    private fun Bitmap.isBlank(): Boolean {
        if (width == 0 || height == 0) return true
        // Sample an 8x8 grid — cheap (sub-millisecond) and robust to a stray
        // pixel at a single cell. The stride grid self-limits to ~64 probes.
        val strideX = (width / 8).coerceAtLeast(1)
        val strideY = (height / 8).coerceAtLeast(1)
        for (y in 0 until height step strideY) {
            for (x in 0 until width step strideX) {
                val alpha = android.graphics.Color.alpha(getPixel(x, y))
                if (alpha > 0) return false
            }
        }
        return true
    }

    /** Clear the frozen frame and release its backing pixels after Compose detaches. */
    fun finishTransition() {
        val old = screenshotBitmap
        screenshotBitmap = null
        revealCenter = Offset.Zero
        isAnimating = false
        captureView?.postDelayed(
            { old?.takeUnless { it.isRecycled }?.recycle() },
            BITMAP_RECYCLE_DELAY_MS,
        )
    }

    suspend fun resetProgress() {
        progress.snapTo(0f)
    }

    private companion object {
        const val BITMAP_RECYCLE_DELAY_MS = 96L
    }
}

private const val THEME_CONTENT_SETTLE_DELAY_MS = 80L
private const val THEME_FLIP_DELAY_MS = 60L
private const val THEME_REVEAL_DURATION_MS = 680
private val THEME_REVEAL_FEATHER = 34.dp
private const val MASK_CURVE_SEGMENTS = 24

/** CompositionLocal exposing the app-wide transition state to any theme switch site. */
val LocalCurioThemeTransition = staticCompositionLocalOf<CurioThemeTransitionState?> { null }

/**
 * Draws [content] with the in-flight theme reveal layered on top. The reveal
 * is a frozen snapshot of the old theme with a feathered radial alpha mask
 * (DstIn over an offscreen layer) that peels away from [revealCenter]; the
 * smoothstep-blended feather gives the soft, blur-like edge Telegram uses.
 */
@Composable
private fun CurioThemeTransitionOverlay(
    state: CurioThemeTransitionState,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val featherPx = with(LocalDensity.current) { THEME_REVEAL_FEATHER.toPx() }
    val bitmap = state.screenshotBitmap
    val progress = state.progress.value
    // Hardware GraphicsLayer that mirrors the host content each frame. Used
    // as the PREFERRED snapshot source so the reveal works in liquid-glass
    // mode (View.drawToBitmap forces a software pass that drops RenderEffect
    // blurs and can return a blank frame). Recorded via drawWithContent so
    // it stays in sync with the live tree.
    val captureLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()

    DisposableEffect(view, state, captureLayer) {
        state.attachView(view)
        state.captureLayer = captureLayer
        onDispose {
            state.attachView(null)
            state.captureLayer = null
        }
    }

    LaunchedEffect(state.isAnimating, bitmap) {
        if (!state.isAnimating || bitmap == null) return@LaunchedEffect
        state.resetProgress()
        // Let the freshly-selected color scheme draw one frame below the frozen snapshot.
        withFrameNanos { }
        delay(THEME_CONTENT_SETTLE_DELAY_MS)
        state.progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = THEME_REVEAL_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
        state.finishTransition()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Record the live content into the GraphicsLayer every frame so a
        // theme-switch snapshot captures the REAL (hardware-rendered, blur-
        // preserving) pixels instead of a software drawToBitmap pass.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    captureLayer.record { this@drawWithContent.drawContent() }
                    drawContent()
                }
        ) {
            content()
        }

        if (bitmap != null && state.isAnimating) {
            val frozenFrame = remember(bitmap) { bitmap.asImageBitmap() }
            Image(
                bitmap = frozenFrame,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithCache {
                        // Radial reveal centred on the trigger, grown with progress and
                        // feathered by a smoothstep mask so the wipe edge stays soft.
                        val center =
                            state.revealCenter.takeUnless { it == Offset.Zero }
                                ?: Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = maxOf(
                            hypot(center.x, center.y),
                            hypot(size.width - center.x, center.y),
                            hypot(center.x, size.height - center.y),
                            hypot(size.width - center.x, size.height - center.y),
                        )
                        val revealRadius = maxRadius * progress
                        val maskRadius = (revealRadius + featherPx).coerceAtLeast(1f)
                        val innerF = ((revealRadius - featherPx).coerceAtLeast(0f)) / maskRadius
                        val outerF = (revealRadius / maskRadius).coerceIn(innerF, 1f)

                        // Transparent core (reveals the new theme) -> smoothstep feather
                        // to fully-opaque black (the old frozen frame) beyond the wipe.
                        val stops = ArrayList<Pair<Float, Color>>(MASK_CURVE_SEGMENTS + 3)
                        stops.add(0f to Color.Transparent)
                        stops.add(innerF to Color.Transparent)
                        for (i in 0..MASK_CURVE_SEGMENTS) {
                            val t = i.toFloat() / MASK_CURVE_SEGMENTS
                            val ink = innerF + (outerF - innerF) * t
                            val feather = t * t * (3f - 2f * t) // smoothstep
                            stops.add(ink to Color.Black.copy(alpha = feather.coerceIn(0f, 1f)))
                        }
                        stops.add(1f to Color.Black)
                        val mask = Brush.radialGradient(
                            colorStops = stops.toTypedArray(),
                            center = center,
                            radius = maskRadius,
                        )

                        onDrawWithContent {
                            drawContent()
                            if (progress > 0f) {
                                drawRect(brush = mask, blendMode = BlendMode.DstIn)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        // Swallow all input for the (sub-second) reveal so the user
                        // can't stack taps on a half-morphed screen.
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
                    },
            )
        }
    }
}

/**
 * Provides the app-wide theme-transition state and stacks the reveal overlay
 * over [content]. Wrap the app's nav host with this so every theme-switch
 * call site (Home sun/moon, Settings Appearance, Onboarding) shares one
 * transition, frozen frame and timing. When absent — splash / widget config,
 * which hold no theme toggle — call sites apply instantly instead.
 */
@Composable
fun CurioThemeTransitionHost(
    content: @Composable () -> Unit,
) {
    val state = remember { CurioThemeTransitionState() }
    CompositionLocalProvider(LocalCurioThemeTransition provides state) {
        CurioThemeTransitionOverlay(state = state, content = content)
    }
}

/**
 * Flip to [newMode], wrapping the change in a circular-reveal transition when
 * it actually changes the light/dark state. Falls back to an instant apply
 * when nothing visually changes, when no transition host is present, or when
 * a reveal is already in flight.
 */
fun switchThemeWithReveal(
    transition: CurioThemeTransitionState?,
    scope: CoroutineScope,
    context: Context,
    center: Offset,
    newMode: String,
) {
    val currentlyDark = AppPreferences.isDarkTheme(context)
    val newDark = AppPreferences.isDarkMode(context, newMode)
    val visuallyChanges = currentlyDark != newDark
    val t = when {
        !visuallyChanges -> null
        transition == null || transition.isAnimating -> null
        else -> transition
    }

    if (t != null) {
        // Freeze the old frame first, then flip the theme underneath so the
        // reveal overlays a fully-recomposed new scheme. The capture runs
        // in a coroutine (GraphicsLayer.toImageBitmap is suspend).
        scope.launch {
            if (t.startTransition(center)) {
                delay(THEME_FLIP_DELAY_MS)
                AppPreferences.setThemeMode(context, newMode)
            } else {
                AppPreferences.setThemeMode(context, newMode)
            }
        }
    } else {
        // No visual change, no host, or a reveal already in flight — apply instantly.
        AppPreferences.setThemeMode(context, newMode)
    }
}

/**
 * Flip a NON-mode visual theme change (Material theme on/off, hero-tear style,
 * pastel toggle, etc.) with the SAME circular-reveal transition as
 * [switchThemeWithReveal]. These changes repaint the whole color scheme even
 * when the light/dark state stays the same, so the reveal is forced.
 *
 * [apply] is the pref write that triggers the recomposition (e.g.
 * `AppPreferences.setMaterialThemeEnabled(context, true)`); it runs AFTER the
 * old frame is frozen so the reveal overlays the recomposed new scheme.
 */
fun switchVisualThemeWithReveal(
    transition: CurioThemeTransitionState?,
    scope: CoroutineScope,
    center: Offset,
    apply: () -> Unit,
) {
    val t = when {
        transition == null || transition.isAnimating -> null
        else -> transition
    }
    if (t != null) {
        scope.launch {
            if (t.startTransition(center)) {
                delay(THEME_FLIP_DELAY_MS)
                apply()
            } else {
                apply()
            }
        }
    } else {
        apply()
    }
}