package com.curio.app.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
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
 * v241 — whether IN-SCREEN glass is active: the main toggle AND the separate
 * "In-screen glass" experiment. In-screen pills each sample a LOCAL backdrop
 * layer that EXCLUDES them (the pill is a sibling overlay of the captured
 * Box — exactly the bottom-nav architecture that has never crashed), so the
 * old v228 self-capture cycle is impossible by construction; the global
 * capture guard below stays as belt-and-braces.
 */
fun isInScreenGlassActive(): Boolean =
    // v242 — merged into the single Liquid glass toggle (Appearance):
    // in-screen sites use the same crash-safe local-capture architecture,
    // so there is no reason to gate them separately anymore.
    isLiquidGlassPillsActive()

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
    // v241 — explicit LOCAL backdrop for in-screen pills. When null, falls
    // back to the NavHost's whole-page capture (bottom-bar overlay sites).
    // In-screen callers MUST pass their own local capture: one that records
    // only what sits BEHIND the pill, with the pill itself OUTSIDE the
    // captured subtree — sampling a capture that includes the pill is what
    // produced the v228 cyclic render node.
    backdrop: LayerBackdrop? = null,
    // v241 — force the CLEAR-glass recipe regardless of the Clear-glass
    // toggle: the in-screen floating pills are requested to read as fully
    // clear, refracting glass (blur 2dp, wash cut to ~a third).
    alwaysClear: Boolean = false,
    // v241 — capsule shape. CircleShape for the round pills; wide bars
    // (Pet Designer studio bar) pass RoundedCornerShape(50) so the glass
    // clips to a stadium instead of an ellipse.
    shape: Shape = CircleShape
): Modifier {
    if (!isLiquidGlassPillsActive()) return this
    val backdrop = backdrop ?: CurioGlassPills.backdrop ?: return this
    // Hoisted — isCurioDarkTheme() is @Composable and the shadow lambda
    // below is NOT a composable context.
    val dark = isCurioDarkTheme()
    // v233 — CLEAR GLASS (experiment): drop the heavy frost so the capsule
    // reads like the bright refraction blob under a finger press rather
    // than milky frosted glass.
    val clear = AppPreferences.glassClarityState || alwaysClear
    // v242 — user tuning (Appearance → Liquid glass): multipliers around
    // the tuned defaults. Hoisted here; the draw lambdas are plain scopes.
    val blurScale = AppPreferences.glassBlurScaleState
    val refrScale = AppPreferences.glassRefractionScaleState
    val reflScale = AppPreferences.glassReflectionScaleState.coerceIn(0f, 2f)
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
                shape = { shape },
                effects = {
                    vibrancy()
                    blur((if (clear) 1f.dp else 8f.dp).toPx() * blurScale)
                    lens(24f.dp.toPx() * refrScale, 24f.dp.toPx() * refrScale)
                },
                highlight = { Highlight.Default.copy(alpha = reflScale) },
                shadow = {
                    Shadow.Default.copy(
                        color = Color.Black.copy(alpha = if (dark) 0.20f else 0.10f)
                    )
                },
                // The translucent wash over the refracted backdrop — 40% like
                // the reference glass bar, so the tint reads but content shows.
                // Clear-glass cuts it to roughly a third.
                onDrawSurface = {
                    drawRect(container.copy(alpha = washAlpha * if (clear) 0.20f else 1f))
                }
            )
        )
}

/**
 * v241 — PARALLAX TILT LIGHT ARC (fixes the PERFECT CIRCLE). The v233
 * version stroked a FULL white circle whose gradient center shifted with
 * tilt — with Glass parallax ON, any tilt painted a literal perfect circle
 * on every always-glass surface. The cue is now a TOP-EDGE light arc: an
 * ~110° stroke hugging the capsule's top rim that SLIDES sideways with
 * tiltX and fades in with tilt magnitude — light catching the pane's top
 * edge as the phone moves, never a full ring, invisible when level. Call
 * INSIDE a DrawScope after drawContent(). Reads tilt snapshot state
 * directly, so sensor updates invalidate just this draw — zero
 * recomposition.
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
    // Arc bright spot slides AGAINST horizontal tilt along the top rim.
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
