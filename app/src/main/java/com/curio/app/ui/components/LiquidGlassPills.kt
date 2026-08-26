package com.curio.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.ui.components.liquidglass.CurioLegacyBlur
import com.curio.app.ui.components.liquidglass.curioLegacyGlassCapsule
import com.curio.app.ui.theme.isCurioDarkTheme
import androidx.compose.ui.util.lerp
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

/**
 * Whether the user WANTS liquid glass (the toggle alone). On Android 12+
 * this means real refracting glass; below it drives the SIMULATED glass
 * recipe ([fauxGlassCapsule]) so older devices get the look instead of
 * silently nothing.
 */
fun isLiquidGlassRequested(): Boolean =
    AppPreferences.liquidGlassPillsState

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
 *
 * v256 — API-GATED again: below Android 12 there is no RenderEffect, and
 * the simulated recipe read badly on these small floating pills (the user
 * call). Only the BOTTOM NAV and TOPIC REVEAL keep simulated glass on old
 * devices — every in-screen pill falls back to its classic solid path.
 */
fun isInScreenGlassActive(): Boolean =
    // v242 — merged into the single Liquid glass toggle (Appearance).
    isLiquidGlassRequested() && android.os.Build.VERSION.SDK_INT >= 31

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
/**
 * v243 — SIMULATED glass for pre-Android-12 devices (no RenderEffect, so
 * no real backdrop blur/refraction). Draws a theme-aware frosted veil, a
 * top-down sheen and a bright rim over the capsule so the look still reads
 * "liquid glass" — just without live refraction of the content behind it.
 */
@Composable
fun Modifier.fauxGlassCapsule(
    container: Color,
    // v260 — optional explicit corner radius: the simulated recipe defaults
    // to a stadium (min(w,h)/2), which mismatches surfaces whose shape
    // animates (the explore bubble's expanding panel).
    corner: Dp? = null
): Modifier {
    val dark = isCurioDarkTheme()
    // v245 — CRISPER, not frosty: real per-frame blur is impossible below
    // Android 12, so the recipe leans clear-pane (light veil, strong sheen +
    // rim) instead of milk. The Appearance Blur slider drives the veil too —
    // at the default 25% the capsule reads near-clear.
    val veilScale = 0.30f + 0.70f * AppPreferences.glassBlurScaleState.coerceIn(0f, 2f)
    val veilBase = if (dark) 0.05f else 0.34f
    val veil = Color.White.copy(alpha = veilBase * veilScale)
    val sheen = if (dark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.75f)
    val rim = if (dark) Color.White.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.90f)
    return this.drawWithContent {
        drawContent()
        val r = CornerRadius(corner?.toPx() ?: minOf(size.width, size.height) / 2f)
        // Frosted veil — the "blur" stand-in.
        drawRoundRect(color = veil, cornerRadius = r)
        // Top-down sheen — light catching the pane.
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(sheen, Color.Transparent, Color.Transparent, sheen.copy(alpha = sheen.alpha * 0.4f))
            ),
            cornerRadius = r
        )
        // Bright rim.
        drawRoundRect(
            color = rim,
            cornerRadius = r,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * v243 — a LIGHTER faux-glass coat for surfaces that keep their own fill
 * (the classic nav bar on old devices): sheen + rim only, no veil, so the
 * bar's dynamic container color still reads.
 */
@Composable
fun Modifier.curioFauxGlassSheen(corner: Dp? = null): Modifier {
    val dark = isCurioDarkTheme()
    val sheen = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.40f)
    val rim = if (dark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.70f)
    return this.drawWithContent {
        drawContent()
        val r = CornerRadius(corner?.toPx() ?: minOf(size.width, size.height) / 2f)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(sheen, Color.Transparent)),
            cornerRadius = r
        )
        drawRoundRect(color = rim, cornerRadius = r, style = Stroke(width = 1.dp.toPx()))
    }
}

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
    shape: Shape = CircleShape,
    // v260 — CRASH FIX: opt-in for the GLOBAL NavHost capture. Only callers
    // that sit OUTSIDE the captured subtree (the floating bottom nav) may
    // sample it — an in-screen caller with backdrop == null used to fall
    // through to the global capture, sampled a layer containing ITSELF, and
    // produced the v228 cyclic render node again: RenderThread SIGSEGV
    // (stack overflow in prepareTreeImpl) on Samsung A35 / Android 16.
    // In-screen callers without a local capture now get the SAFE simulated
    // recipe instead — never the global layer.
    useGlobalCapture: Boolean = false,
    // v246 — TOUCH FEEL returns: pass the pill's click InteractionSource
    // and holding it springs the whole capsule slightly SMALLER while the
    // lens refraction blooms at the corners under the finger — the tactile
    // press behavior from before the blob reverts.
    interactionSource: InteractionSource? = null,
    // v291 — COMPACT glass for small surfaces (chip bars, small pills):
    // skips lens refraction (invisible on <50dp capsules) and reduces
    // blur to a minimum, cutting drawBackdrop cost by ~60% per chip.
    compact: Boolean = false,
    // v292g — force FROST regardless of the Clear-glass toggle: chips
    // should always read as frosted glass even when glassClarity is ON.
    forceFrost: Boolean = false
): Modifier {
    if (!isLiquidGlassRequested()) return this
    // v243 — pre-Android-12: no RenderEffect → serve the simulated glass
    // recipe so those users get the look instead of nothing.
    // v264 — LEGACY GLASS BLUR (opt-in experiment): when the app-side blur
    // engine has a snapshot, serve REAL frosted glass (the blurred page
    // content) instead of the static veil. Only the bottom nav + Topic
    // Reveal reach this branch on old devices (in-screen pills are gated to
    // 12+ by [isInScreenGlassActive]), which is exactly the agreed scope.
    if (android.os.Build.VERSION.SDK_INT < 31) {
        return if (CurioLegacyBlur.isActive() && CurioLegacyBlur.snapshot != null) {
            this.curioLegacyGlassCapsule(container)
        } else {
            this.fauxGlassCapsule(container)
        }
    }
    val effectiveBackdrop = backdrop
        ?: (if (useGlobalCapture) CurioGlassPills.backdrop else null)
        ?: return this.fauxGlassCapsule(container)
    // Hoisted — isCurioDarkTheme() is @Composable and the shadow lambda
    // below is NOT a composable context.
    val dark = isCurioDarkTheme()
    // v233 — CLEAR GLASS (experiment): drop the heavy frost so the capsule
    // reads like the bright refraction blob under a finger press rather
    // than milky frosted glass.
    // v292g — forceFrost overrides glassClarity so chips always frost.
    val clear = (AppPreferences.glassClarityState || alwaysClear) && !forceFrost
    // v242 — user tuning (Appearance → Liquid glass): multipliers around
    // the tuned defaults. Hoisted here; the draw lambdas are plain scopes.
    val blurScale = AppPreferences.glassBlurScaleState
    val refrScale = AppPreferences.glassRefractionScaleState
    val reflScale = AppPreferences.glassReflectionScaleState.coerceIn(0f, 2f)
    // v246 — spring-driven press progress (0 at rest → 1 while held).
    // v247 — Apple-style feel: the press-IN is fast and crisp (high
    // stiffness, near-critically damped — no wobble on the way down), the
    // RELEASE is softer and slightly underdamped so the capsule springs
    // back with one gentle overshoot, like iOS control centers.
    val press = remember(interactionSource) { Animatable(0f) }
    if (interactionSource != null) {
        val pressed by interactionSource.collectIsPressedAsState()
        LaunchedEffect(pressed) {
            press.animateTo(
                if (pressed) 1f else 0f,
                spring(
                    dampingRatio = if (pressed) 0.85f else 0.55f,
                    stiffness = if (pressed) 900f else 380f
                )
            )
        }
    }
    return this
        // v246 — press shrink: the capsule eases ~4% down toward its middle
        // while held, releasing with the same spring.
        .graphicsLayer {
            val s = lerp(1f, 0.96f, press.value)
            scaleX = s
            scaleY = s
        }
        // v228 — outer guard: during a capture record pass, skip the
        // backdrop-drawing inner node entirely and paint a plain capsule,
        // so the pill never samples its own recording layer. The guard also
        // carries the v233 PARALLAX EDGE GLOW (after drawContent, so it
        // rides on top of the rendered glass).
        .drawWithContent {
            // v243 — the guard must ONLY flatten pills that sample the
            // GLOBAL NavHost capture (backdrop == null): during its record
            // pass such a pill would sample a layer containing itself.
            // Local-backdrop pills sit OUTSIDE their captured subtree, so
            // the global re-records that fire on every scroll frame must
            // not touch them — this condition is why the in-screen pills
            // never showed glass (they were flattened permanently).
            // v260 — only GLOBAL samplers (useGlobalCapture) reference the
            // layer being recorded here, so only they must flatten. Local-
            // backdrop pills sit outside their captured subtree and must
            // keep drawing through global re-records (the v243 fix).
            if (CurioGlassPills.isCapturingBackdrop && useGlobalCapture) {
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
                backdrop = effectiveBackdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    // v291 — compact mode: no lens (invisible on <50dp
                    // capsules but each call adds a per-pixel distortion
                    // pass). v292 — user call: chips read too flat — the
                    // frost blur went back UP to 3dp so small capsules get
                    // the same milky-frosted depth as the big panes.
                    if (compact) {
                        // v292c — REVERTED the frost boost (user call: chips
                        // must read as LIQUID GLASS again, not milky plastic).
                        // Back to the light clear-refraction recipe: minimal
                        // blur, no lens — crisp refraction at a fraction of
                        // the per-pixel cost.
                        blur(0.5f.dp.toPx() * blurScale)
                    } else {
                        blur((if (clear) 1f.dp else 8f.dp).toPx() * blurScale)
                        // v246 — refraction blooms under the finger: the lens
                        // deepens with press progress, so the corners visibly
                        // bend the content while the pill is held.
                        val lensR = 24f.dp.toPx() * refrScale * (1f + 0.45f * press.value)
                        lens(lensR, lensR)
                    }
                },
                highlight = { Highlight.Default.copy(alpha = reflScale) },
                shadow = {
                    Shadow.Default.copy(
                        color = Color.Black.copy(alpha = if (dark) 0.20f else 0.10f),
                        // v245 — the outer glow shifts with the phone's tilt.
                        offset = tiltGlowOffset()
                    )
                },
                // The translucent wash over the refracted backdrop — 40% like
                // the reference glass bar, so the tint reads but content shows.
                // Clear-glass cuts it to roughly a third. v292 — compact
                // surfaces take a ~35% STRONGER wash so the small chips read
                // properly frosty instead of clear-plastic.
                onDrawSurface = {
                    // v292c — wash back to the standard recipe (the v292/
                    // v292b compact multipliers made chips read milky-frosted
                    // instead of clear liquid glass).
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
/**
 * v245 — TILT-REACTIVE OUTER GLOW. The soft outer shadow every glass
 * capsule already wears IS the glow the user means; its offset now shifts
 * AGAINST the device tilt (the pane leans toward the light), so tilting
 * moves the glow around the capsule. Reads the tilt snapshot state
 * directly — draw-only invalidation, zero recomposition.
 */
internal fun tiltGlowOffset(): DpOffset {
    // v270 — glass parallax tilt REMOVED (user call: useless / not working).
    // The sensor listener is gone; the glow rests at its default offset.
    return DpOffset(0f.dp, 4f.dp)
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlassTiltEdgeGlow() {
    // v245 — RETIRED. The in-capsule light arc read as a wrong, painted-on
    // effect. The tilt cue is now the capsule's OUTER GLOW: the soft outer
    // shadow shifts against the device tilt via [tiltGlowOffset], so the
    // pane visibly floats as you move the phone. Kept as a no-op so the
    // call sites stay untouched.
    return
}
