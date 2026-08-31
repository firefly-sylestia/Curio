package com.curio.app.ui.theme

import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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

    /** Attach the host view used to snapshot the old frame. Null detaches. */
    fun attachView(view: View?) {
        captureView = view
    }

    /**
     * Freeze the current frame and arm a reveal expanding from [center].
     * Ignores a start while an animation is already in flight. If the
     * snapshot fails (hardware-only view, recycled, etc.) or comes back
     * blank (a device quirk where drawToBitmap "succeeds" but replays
     * nothing) we bail out silently so the theme switch still happens —
     * the animation is pure polish and must never block it.
     */
    fun startTransition(center: Offset): Boolean {
        if (isAnimating) return false
        val view = captureView ?: return false
        val bitmap = try {
            val bmp = view.drawToBitmap()
            // A blank/transparent snapshot (all alpha 0) can't drive the wipe
            // -> treat it as a failure and fall back to the instant flip so we
            // never freeze a see-through frame on devices where drawToBitmap
            // quietly returns nothing (v269 parity).
            if (bmp.isBlank()) null else bmp
        } catch (e: Exception) {
            null
        } ?: return false
        try {
            // Recreate (rather than snapTo, which is suspend) so the reveal
            // always begins from 0 on a new capture.
            progress = Animatable(0f)
            screenshotBitmap = bitmap
            revealCenter = center
            isAnimating = true
            true
        } catch (e: Exception) {
            screenshotBitmap = null
            isAnimating = false
            false
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
        true
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

    DisposableEffect(view, state) {
        state.attachView(view)
        onDispose { state.attachView(null) }
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
        content()

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

    if (t != null && t.startTransition(center)) {
        // Freeze the old frame first, then flip the theme underneath so the
        // reveal overlays a fully-recomposed new scheme.
        scope.launch {
            delay(THEME_FLIP_DELAY_MS)
            AppPreferences.setThemeMode(context, newMode)
        }
    } else {
        // No visual change, no host, or a reveal already in flight — apply instantly.
        AppPreferences.setThemeMode(context, newMode)
    }
}