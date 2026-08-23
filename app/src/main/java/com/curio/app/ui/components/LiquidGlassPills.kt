package com.curio.app.ui.components

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.ui.theme.isCurioDarkTheme
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * v227 — LIQUID GLASS PILLS (experiment, Experiments toggle, default OFF).
 *
 * The three floating nav-style capsules — the bottom tab bar, the Topic
 * Reveal category/favorite bar and the Pet Designer studio bar — can render
 * as real liquid-glass: a real-time frosted capture of whatever is behind
 * them (vibrancy + blur + lens refraction, the `io.github.kyant0:backdrop`
 * recipe that vFlow's glass bar popularised) instead of the solid elevated
 * fill. Requires Android 12+ (RenderEffect); [isLiquidGlassPillsActive]
 * gates every consumer so older devices silently keep the current look.
 *
 * v228 — SELF-CAPTURE GUARD. The NavHost records the whole page-content
 * subtree into [backdrop] AFTER drawing it (see `layerBackdrop`: draw, then
 * `recordLayer` re-invokes the draw chain into the GraphicsLayer). Pills
 * that live INSIDE that subtree — the Reveal bar and the Pet Designer
 * studio bar — would draw a second time during the record pass and sample
 * the very GraphicsLayer being recorded, producing a cyclic render node
 * that crashes HWUI with a RenderThread stack overflow (SIGSEGV in
 * `RenderNode.prepareTreeImpl`, ~500 alternating frames). During a record
 * pass [isCapturingBackdrop] is true and [liquidGlassCapsule] paints a
 * plain translucent capsule instead of sampling the backdrop.
 */
object CurioGlassPills {

    /**
     * The NavHost-root [LayerBackdrop] recording page content. Set once by
     * CurioNavHost (rememberLayerBackdrop + a layerBackdrop() mark on the
     * content wrapper) and read by every glass capsule — mirrors the
     * out-of-band handoff pattern of [CurioNavTint].
     */
    var backdrop by mutableStateOf<LayerBackdrop?>(null)

    /**
     * True while the NavHost's capture layer is re-recording the page
     * subtree into its GraphicsLayer. UI-thread only (draw phase), so a
     * plain var is safe — no snapshot state, it must NOT invalidate
     * composition mid-draw.
     */
    @Volatile
    var isCapturingBackdrop: Boolean = false
}

/** Whether the glass treatment is active (toggle ON and Android 12+). */
fun isLiquidGlassPillsActive(): Boolean =
    AppPreferences.liquidGlassPillsState && android.os.Build.VERSION.SDK_INT >= 31

/**
 * v234 — whether IN-SCREEN glass is active: the main toggle AND the separate
 * "In-screen glass" experiment. In-screen pills each sample a LOCAL backdrop
 * layer that excludes them (sibling-overlay architecture — see the v234 note
 * in [liquidGlassCapsule]), so they are structurally incapable of the v228
 * self-capture cycle; the global-capture guard below stays as belt-and-braces.
 */
fun isInScreenGlassActive(): Boolean =
    isLiquidGlassPillsActive() && AppPreferences.glassInScreenState

/**
 * The capture onDraw for the NavHost's [com.kyant.backdrop.backdrops.rememberLayerBackdrop]:
 * flags the record pass so in-subtree glass capsules fall back to a plain
 * fill (see the v228 note on [CurioGlassPills]).
 */
internal fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.curioGlassCaptureDraw() {
    CurioGlassPills.isCapturingBackdrop = true
    try {
        drawContent()
    } finally {
        CurioGlassPills.isCapturingBackdrop = false
    }
}

/**
 * Turn a floating capsule into liquid glass. Apply INSTEAD of the solid
 * Surface fill: the caller keeps its Surface with a TRANSPARENT color and
 * no elevation, and this modifier draws the blurred backdrop + rim
 * highlight + soft shadow + the translucent container wash itself.
 *
 * No-op when the experiment is off, on Android < 12, or before the NavHost
 * has published its capture layer — callers must keep their classic path.
 */
@Composable
fun Modifier.liquidGlassCapsule(
    container: Color,
    // v230 — scroll-morph support: the translucent wash over the refracted
    // backdrop. The floating nav pills keep the default 40%; the Home
    // menu/profile pills and the detail back/more pills pass a stronger
    // wash while the scroll morph is young (so the handoff from their
    // resting SOLID hero fill doesn't pop) easing down to ~45% when fully
    // scrolled.
    washAlpha: Float = 0.40f,
    // v234 — explicit LOCAL backdrop for in-screen pills. When null, falls
    // back to the NavHost's whole-page capture (bottom-bar overlay sites).
    // In-screen callers MUST pass their own local capture: one that records
    // only what sits BEHIND the pill, with the pill itself OUTSIDE the
    // captured subtree — the bottom-nav architecture. Sampling a capture
    // that includes the pill is what produced the v228 cyclic render node.
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null
): Modifier {
    if (!isLiquidGlassPillsActive()) return this
    val backdrop = backdrop ?: CurioGlassPills.backdrop ?: return this
    // Hoisted — isCurioDarkTheme() is @Composable and the shadow lambda
    // below is NOT a composable context.
    val dark = isCurioDarkTheme()
    // v233 — CLEAR GLASS (experiment): drop the heavy frost so the capsule
    // reads like the bright refraction blob under a finger press rather
    // than milky frosted glass.
    val clear = AppPreferences.glassClarityState
    return this
        // v228 — outer guard: during a capture record pass, skip the
        // backdrop-drawing inner node entirely and paint a plain capsule,
        // so the pill never samples its own recording layer. The guard also
        // carries the v233 PARALLAX EDGE GLOW (after drawContent, so it
        // rides on top of the rendered glass).
        .drawWithContent {
            if (CurioGlassPills.isCapturingBackdrop) {
                drawRoundRect(
                    color = container.copy(alpha = 0.88f),
                    cornerRadius = CornerRadius(
                        minOf(size.width, size.height) / 2f
                    )
                )
            } else {
                drawContent()
                drawGlassTiltEdgeGlow()
            }
        }
        .then(
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur((if (clear) 2.dp else 8.dp).toPx())
                    // v237 — SIZE-CAPPED refraction: on small ROUND pills the
                    // fixed 24dp bands wrapped the whole capsule and folded a
                    // perfect-circle ring into its center (Home menu/profile,
                    // reveal + studio bars). Cap to ~16% of the pill's size;
                    // small pills get a thin edge bend, big bars keep 24dp.
                    val rh = minOf((if (clear) 14.dp else 24.dp).toPx(), size.minDimension * 0.16f)
                    lens(rh, rh * 1.3f)
                },
                highlight = { Highlight.Default },
                shadow = {
                    Shadow.Default.copy(
                        color = Color.Black.copy(alpha = if (dark) 0.20f else 0.10f)
                    )
                },
                // The translucent wash over the refracted backdrop — 40% like
                // the reference glass bar, so the tint reads but content shows.
                // Clear-glass cuts it to roughly a third.
                onDrawSurface = {
                    drawRect(container.copy(alpha = washAlpha * if (clear) 0.35f else 1f))
                }
            )
        )
}

/**
 * v236 — TOUCH LIQUID-GLASS BLOB. The signature press feel of the bottom
 * nav's active pill, packaged for every other glass element: while pressed,
 * the capsule gently GROWS (spring, no overshoot) and a soft white radial
 * glow blooms under the finger and follows it — the bright refraction blob
 * iOS glass shows on touch. Non-consuming: pointer events pass straight
 * through to the child clickable, so taps keep working unchanged.
 *
 * Apply to (or around) the pill Surface; the glow is clipped to a circle,
 * which matches every capsule/circular glass pill in the app.
 */
@Composable
fun Modifier.curioGlassPressBlob(
    interactionSource: InteractionSource,
    maxScale: Float = 1.05f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "glassPressBlob"
    )
    var pressPoint by remember { mutableStateOf(Offset.Zero) }
    return this
        .graphicsLayer {
            val s = 1f + (maxScale - 1f) * progress
            scaleX = s
            scaleY = s
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    pressPoint = change.position
                    if (!change.pressed) break
                }
            }
        }
        .clip(CircleShape)
        .drawWithContent {
            drawContent()
            if (progress > 0.01f && size.minDimension > 0f) {
                val r = size.minDimension * 1.2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.38f),
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = pressPoint,
                        radius = r
                    ),
                    radius = r,
                    center = pressPoint,
                    alpha = progress
                )
            }
        }
}

/**
 * v238 — PARALLAX TILT LIGHT ARC (rewritten). The v233 version stroked a
 * FULL white circle whose gradient center shifted with tilt — on every
 * always-glass surface (bottom nav pill, Pet Designer bar, Reveal bar) that
 * read as an unwanted PERFECT CIRCLE the moment the phone tilted at all.
 *
 * The cue is now a TOP-EDGE light arc instead: an ~100° stroke hugging the
 * capsule's top rim that SLIDES sideways with tiltX and fades in with tilt
 * magnitude — light catching the pane's top edge as the phone moves, never
 * a full ring, invisible when the phone is level. Call INSIDE a DrawScope
 * after drawContent(). Reads tilt snapshot state directly, so sensor
 * updates invalidate just this draw — zero recomposition.
 */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlassTiltEdgeGlow() {
    if (!AppPreferences.glassParallaxState) return
    val tx = com.curio.app.ui.components.liquidglass.CurioGlassParallax.tiltX
    val ty = com.curio.app.ui.components.liquidglass.CurioGlassParallax.tiltY
    // Strength tracks HOW MUCH the phone is tilted — level phone = no arc.
    val strength = kotlin.math.sqrt(tx * tx + ty * ty).coerceIn(0f, 1f)
    if (strength < 0.08f) return
    val r = minOf(size.width, size.height) / 2f
    if (r <= 0f) return
    // Arc center slides AGAINST horizontal tilt along the top rim.
    val cx = size.width / 2f - tx * size.width * 0.30f
    val cy = size.height * 0.10f - ty * r * 0.35f
    drawArc(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = size.minDimension * 0.9f
        ),
        startAngle = -145f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset.Zero,
        size = this.size,
        style = Stroke(width = 1.5f.dp.toPx()),
        alpha = 0.30f + 0.45f * strength
    )
}
