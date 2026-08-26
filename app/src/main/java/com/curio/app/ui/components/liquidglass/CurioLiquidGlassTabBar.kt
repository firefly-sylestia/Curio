/*
 * Adapted from vFlow (https://github.com/ChaoMixian/vFlow),
 * ui/main/glass/LiquidGlassBottomBar.kt — GPL-2.0-or-later.
 * The full liquid-glass tab bar: three stacked layers over one real-time
 * backdrop capture —
 *   1. the VISIBLE glass capsule (vibrancy + blur + lens refraction),
 *   2. an INVISIBLE accent-tinted copy of the tab row recorded into a
 *      second backdrop layer (so the moving pill refracts COLORED icons),
 *   3. the draggable active pill that refracts both layers, with press
 *      scale, velocity squash/stretch, inner shadow and the specular
 *      interactive highlight.
 */
package com.curio.app.ui.components.liquidglass

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.curio.app.data.AppPreferences
import com.curio.app.ui.components.drawGlassTiltEdgeGlow
import com.curio.app.ui.components.tiltGlowOffset
import androidx.compose.ui.unit.DpOffset
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

val LocalLiquidGlassTabScale = staticCompositionLocalOf { { 1f } }

/**
 * v247 — marks the CRISP OVERLAY copy of the tab row (drawn above the
 * solid active pill). Overlay items drop their clickable so touches fall
 * through to the real tabs underneath — the overlay exists purely to keep
 * the icons/labels visible on top of the now-opaque idle pill.
 */
val LocalLiquidGlassTabOverlay = staticCompositionLocalOf { false }

/**
 * v232 — PER-TAB METRICS. Each item reports its measured width so the bar
 * can lay the draggable active indicator over REAL tab widths instead of
 * assuming equal division — required now that tabs expand/collapse
 * classic-style (icon-only ↔ icon+side-label).
 */
val LocalLiquidGlassTabMetrics =
    staticCompositionLocalOf<(index: Int, widthPx: Float) -> Unit> { { _, _ -> } }

@Composable
fun RowScope.CurioLiquidGlassTabBarItem(
    // v232 — position of this tab within the bar; used to report width.
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val scale = LocalLiquidGlassTabScale.current
    val reportWidth = LocalLiquidGlassTabMetrics.current
    val isOverlay = LocalLiquidGlassTabOverlay.current
    Row(
        modifier
            .clip(CircleShape)
            .then(
                if (isOverlay) Modifier
                else Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick
                )
            )
            .onGloballyPositioned { coordinates ->
                reportWidth(index, coordinates.size.width.toFloat())
            }
            .graphicsLayer {
                val currentScale = scale()
                scaleX = currentScale
                scaleY = currentScale
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun CurioLiquidGlassTabBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    // v292 — the resting active pill's FILL (Appearance → Indicator
    // colour): auto theme / white / black. The ink that sits on it comes
    // from [curioGlassIndicatorColors] in CurioBottomNav.
    indicatorFill: Color = Color.White,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.40f),
    isBlurEnabled: Boolean = true,
    // v262 — ghost suppression scoped to the PET DESIGNER only: when true,
    // the blob samples the PAGE ONLY (no hidden tab-row copy), so its glass
    // never doubles the tab icon/label there. The HOME NAV keeps the full
    // combined sample — the small refracted capsule of tab content inside
    // the blob is the effect the user wants there.
    ghostFreeTabs: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    // v233 — CLEAR GLASS (experiment): less frost, more refraction.
    val clear = AppPreferences.glassClarityState
    // v242 — user tuning (Appearance → Liquid glass): multipliers around
    // the tuned defaults, applied to blur / lens / highlight below.
    val blurScale = AppPreferences.glassBlurScaleState
    val refrScale = AppPreferences.glassRefractionScaleState
    val reflScale = AppPreferences.glassReflectionScaleState.coerceIn(0f, 2f)
    // v243 — user tuning: strength of the draggable indicator's shadow.
    val indShadowScale = AppPreferences.glassIndicatorShadowScaleState.coerceIn(0f, 2f)
    // v248 — CLASSIC indicator experiment (Experiments): ON renders the
    // blob as fully TRANSPARENT refracting glass (the pre-v247 style);
    // OFF (default) is the solid white/black pill.
    val classicIndicator = AppPreferences.glassClassicIndicatorState
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    // v232 — REAL per-tab widths: tabs expand/collapse classic-style now
    // (icon-only ↔ icon+side-label), so the indicator can't assume an even
    // split any more. Falls back to an even division until all tabs report.
    val tabWidthsPx = remember(tabsCount) { FloatArray(tabsCount) }
    var tabMetricsVersion by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    fun evenTabWidth(): Float =
        if (tabsCount == 0) 0f
        else (totalWidthPx - with(density) { 8.dp.toPx() }) / tabsCount

    fun widthAtFraction(f: Float): Float {
        if (tabWidthsPx.any { it <= 0f }) return evenTabWidth()
        val i = f.toInt().fastCoerceIn(0, tabsCount - 1)
        val frac = (f - i).fastCoerceIn(0f, 1f)
        val next = if (i + 1 < tabsCount) tabWidthsPx[i + 1] else tabWidthsPx[i]
        return tabWidthsPx[i] + (next - tabWidthsPx[i]) * frac
    }

    /** Left edge (LTR) of the fractional tab index across the real widths. */
    fun offsetOfFraction(f: Float): Float {
        if (tabWidthsPx.any { it <= 0f }) return f * evenTabWidth()
        val i = f.toInt().fastCoerceIn(0, tabsCount - 1)
        val frac = (f - i).fastCoerceIn(0f, 1f)
        var x = 0f
        for (j in 0 until i) x += tabWidthsPx[j]
        return x + tabWidthsPx[i] * frac
    }

    // v249 — iOS-style TAB-SWITCH GLIDE: programmatic moves (tapping a
    // tab, drag release) used the class's default critically-damped 1000-
    // stiffness spring, which snaps between tabs almost instantly. This
    // softer spring glides the blob across (~350ms) with a gentle settle.
    // v292 — user call: the sideways liquid GLIDE on tap looks bad — iOS
    // style instead, where the blob shows up under the new tab FAST even
    // on quick switches. Tap switches now use a very stiff ~90ms spring;
    // drag release keeps the soft glide (the finger led it there).
    val tabGlideSpec = spring<Float>(dampingRatio = 0.82f, stiffness = 380f)
    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }
    }

    class Holder {
        var instance: DampedDragAnimation? = null
    }

    val holder = remember { Holder() }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { offset ->
                val animation = holder.instance ?: return@DampedDragAnimation true
                if (totalWidthPx == 0f) return@DampedDragAnimation false

                val currentValue = animation.value
                val indicatorX = offsetOfFraction(currentValue)
                val padding = with(density) { 4.dp.toPx() }
                val touchX = if (isLtr) {
                    padding + indicatorX + offset.x
                } else {
                    totalWidthPx - padding - widthAtFraction(currentValue) - indicatorX + offset.x
                }
                touchX in 0f..totalWidthPx
            },
            onDragStarted = {},
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                animateToValue(targetIndex.toFloat(), tabGlideSpec)
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
                onSelected(targetIndex)
            },
            onDrag = { _, dragAmount ->
                if (totalWidthPx > 0f) {
                    // v243 — UNIFORM drag sensitivity: dividing by the width
                    // under the finger made fast drags hypersensitive wherever
                    // a NARROW (collapsed) tab sat — visibly erratic on the
                    // right side of Cabinet. A fixed per-tab stride keeps the
                    // feel identical across the whole bar in both directions.
                    val tabStride = maxOf(totalWidthPx / tabsCount, 1f)
                    updateValue(
                        (targetValue + dragAmount.x / tabStride * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            }
        ).also { holder.instance = it }
    }

    // v292h — TAB SWITCH: SNAP instantly to the new tab on tap or
    // programmatic navigation. No sideways glide, no spring — the blob
    // appears under the new tab in one frame. Drag-release animations
    // are handled in onDragStopped only.
    LaunchedEffect(selectedIndex) {
        dampedDragAnimation.snapToValue(selectedIndex.toFloat())
    }

    val interactiveHighlight = remember(animationScope, totalWidthPx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) offsetOfFraction(dampedDragAnimation.value + 0.5f) + panelOffset
                        else size.width - offsetOfFraction(dampedDragAnimation.value + 0.5f) + panelOffset,
                        size.height / 2f
                    )
                }
            )
        } else {
            null
        }
    }

    // v292d — the crisp overlay's alpha: 1 at rest, drops to 0 within ~110ms
    // of a blob press so the refracted tab copy shows alone while held.
    // v292g — ghostFreeTabs (Pet Designer) hides during press/move but
    // shows at rest — the blob samples page-only so the overlay is the
    // ONLY source of tab labels (no duplication like Home's combined
    // sample).
    val inkOverlayAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (dampedDragAnimation.pressProgress > 0.04f) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween(110),
        label = "inkOverlay"
    )

    // v232 — both copies of the tab row (visible + hidden accent-tinted)
    // run inside the metrics provider so every item reports its width.
    val measuringContent: @Composable RowScope.() -> Unit = {
        CompositionLocalProvider(
            LocalLiquidGlassTabMetrics provides { i, w ->
                if (i in tabWidthsPx.indices && w > 0f && tabWidthsPx[i] != w) {
                    tabWidthsPx[i] = w
                    tabMetricsVersion++
                }
            }
        ) { content() }
    }

    // v231 — SQUISH FIX: the old `width(IntrinsicSize.Min)` + weight(1f)
    // combination collapsed the bar — the intrinsic MIN width of a Text is
    // tiny (soft wrap), so every tab shrank to a sliver and the icons and
    // labels clipped. The bar now wraps its content naturally and each tab
    // keeps a generous minimum width — always expanded, nothing cut.
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier
                .onGloballyPositioned { coordinates ->
                    totalWidthPx = coordinates.size.width.toFloat()
                }
                .graphicsLayer { translationX = panelOffset }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        if (isBlurEnabled) {
                            vibrancy()
                            blur((if (clear) 1f.dp else 8f.dp).toPx() * blurScale)
                            lens(24f.dp.toPx() * refrScale, 24f.dp.toPx() * refrScale)
                        }
                    },
                    highlight = {
                        // v260 — GLOW TONE-DOWN: this rim highlight ran at
                        // FULL reflection scale, reading as a harsh white
                        // bloom on bright pages. Capped at 55%.
                        Highlight.Default.copy(
                            alpha = (if (isBlurEnabled) 1f else 0f) * 0.55f * minOf(reflScale, 1f)
                        )
                    },
                    shadow = {
                        Shadow.Default.copy(
                            color = Color.Black.copy(alpha = 0.10f),
                            // v245 — outer glow shifts with tilt.
                            offset = tiltGlowOffset()
                        )
                    },
                    onDrawSurface = {
                        // v233 — clear-glass cuts the frost wash to ~a third.
                        drawRect(containerColor.copy(alpha = containerColor.alpha * if (clear) 0.20f else 1f))
                    },
                    layerBlock = {
                        if (isBlurEnabled) {
                            val progress = dampedDragAnimation.pressProgress
                            val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, progress)
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                )
                .then(interactiveHighlight?.modifier ?: Modifier)
                .drawWithContent {
                    drawContent()
                    // v233 — PARALLAX TILT EDGE GLOW: the bar's rim catches
                    // the light against the device tilt (iOS depth cue).
                    drawGlassTiltEdgeGlow()
                }                    .height(64.dp)
                    .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = measuringContent
        )

        CompositionLocalProvider(
            LocalLiquidGlassTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            }
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            if (isBlurEnabled) {
                                val progress = dampedDragAnimation.pressProgress
                                vibrancy()
                                blur((if (clear) 1f.dp else 8f.dp).toPx() * blurScale)
                                lens(24f.dp.toPx() * refrScale * progress, 24f.dp.toPx() * refrScale * progress)
                            }
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = (if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f) * reflScale)
                        },
                        shadow = { null },
                        onDrawSurface = {
                            // v233 — clear-glass cuts the frost wash to ~a third.
                            drawRect(containerColor.copy(alpha = containerColor.alpha * if (clear) 0.20f else 1f))
                        }
                    )
                    .then(interactiveHighlight?.modifier ?: Modifier)
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                // v246 — the hidden copy is UNTINTED now. The old accent
                // ColorFilter is what made the ink look category-colored
                // whenever the pill settled over it; sampling the plain
                // black/white row keeps the theme ink pure while still
                // letting the icons/labels show through the glass.
                verticalAlignment = Alignment.CenterVertically,
                content = measuringContent
            )
        }

        if (totalWidthPx > 0f) {
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        translationX = if (isLtr) {
                            offsetOfFraction(dampedDragAnimation.value) + panelOffset
                        } else {
                            -offsetOfFraction(dampedDragAnimation.value) + panelOffset
                        }
                    }
                    .then(interactiveHighlight?.gestureModifier ?: Modifier)
                    .then(dampedDragAnimation.modifier)
                    .drawWithContent {
                        drawContent()
                        // v233 — PARALLAX TILT EDGE GLOW: the active pill's
                        // rim catches the light against the device tilt too.
                        drawGlassTiltEdgeGlow()
                    }
                    .drawBackdrop(
                        // v260 — DUPLICATE-TEXT FIX: the default (solid idle
                        // pill) style now samples the PAGE ONLY. The v251
                        // combined sample (page + the hidden tab-row copy)
                        // made the active tab's icon/label appear TWICE while
                        // pressed — once refracted inside the blob, once in
                        // the crisp overlay that hasn't fully faded yet. The
                        // classic (transparent) experiment keeps the combined
                        // sample, since its whole point is bending the tab
                        // content under the finger.
                        // v262 — COMBINED SAMPLE RESTORED for the home nav:
                        // page + hidden untinted tab-row copy, so the blob
                        // visibly bends a small capsule of the tab content
                        // under it. Only Pet Designer opts OUT (ghostFreeTabs)
                        // because there the double-drew labels.
                        // v262 — UNCONDITIONAL for ghostFreeTabs (even with
                        // the classic-indicator experiment on): Pet Designer
                        // always samples page-only, home nav always combines.
                        backdrop = if (ghostFreeTabs) {
                            backdrop
                        } else {
                            rememberCombinedBackdrop(backdrop, tabsBackdrop)
                        },
                        shape = { CircleShape },
                        effects = {
                            // v247 — GENTLE press-gated glass again (as in
                            // d442219): the idle pill is SOLID, so heavy
                            // always-on refraction was over-bending the
                            // sampled content for nothing. While held, a soft
                            // vibrancy + blur + modest lens reveals the page
                            // and tab content bending through the glass.
                            if (isBlurEnabled) {
                                val progress = dampedDragAnimation.pressProgress
                                vibrancy()
                                if (classicIndicator) {
                                    // v248 — classic style: ALWAYS-ON full
                                    // refraction, exactly like the nav capsule
                                    // (the pre-v247 look).
                                    blur((if (clear) 1f.dp else 8f.dp).toPx() * blurScale)
                                    lens(24f.dp.toPx() * refrScale, 24f.dp.toPx() * refrScale)
                                } else {
                                    // v292c — FROST AT REST is BACK for the
                                    // indicator (user call): the idle active pill
                                    // samples the backdrop with vibrancy + blur + a
                                    // soft lens. While held (the touch blob), frost
                                    // eases OFF so the small press capsule reads
                                    // clean — the touch view itself is unchanged.
                                    val rest = 1f - progress
                                    blur((if (clear) 1f.dp else 8f.dp).toPx() * blurScale * rest)
                                    lens(
                                        10f.dp.toPx() * refrScale * rest,
                                        14f.dp.toPx() * refrScale * rest,
                                        true
                                    )
                                }
                            }
                        },
                        highlight = {
                            Highlight.Default.copy(
                                alpha = (if (isBlurEnabled) {
                                    // v260 — GLOW TONE-DOWN: the highlight was
                                    // peaking at full reflection scale, reading
                                    // as a harsh white bloom on bright pages.
                                    // Capped at 55% and softened on press.
                                    if (classicIndicator) 0.55f
                                    else 0.35f * dampedDragAnimation.pressProgress
                                } else 0f) * minOf(reflScale, 1f)
                            )
                        },
                        shadow = {
                            Shadow(
                                // v245 — outer glow shifts with tilt.
                                offset = tiltGlowOffset(),
                                // v247 — a quiet resting shadow lifts the now-
                                // solid idle pill off the page; press deepens it.
                                // v248 — classic style keeps the old fully
                                // press-gated shadow instead.
                                // v260 — resting glow 0.22 → 0.12 (too bright
                                // on light pages).
                                alpha = (if (isBlurEnabled) {
                                    if (classicIndicator) dampedDragAnimation.pressProgress
                                    else 0.12f + 0.68f * dampedDragAnimation.pressProgress
                                } else 0f) * indShadowScale
                            )
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 8.dp * dampedDragAnimation.pressProgress,
                                alpha = (if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f) *
                                    indShadowScale
                            )
                        },
                        layerBlock = {
                            if (isBlurEnabled) {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            }
                        },
                        onDrawSurface = {
                            val progress = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f
                            if (classicIndicator) {
                                // v248 — classic: FULLY transparent at rest
                                // (the pre-v247 refracting-glass look).
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            } else {
                                // v292c — FROSTY at rest again: the idle pill
                                // wears the Appearance-selected indicator fill as
                                // a translucent wash whose OPACITY IS CUSTOMIZABLE
                                // (Appearance → Indicator opacity, default 55%).
                                // Pressing still fades the fill fully away so the
                                // touch blob's press-glass effect shows unchanged.
                                drawRect(
                                    color = indicatorFill,
                                    alpha = lerp(
                                        AppPreferences.navIndicatorOpacityState.coerceIn(0f, 1f),
                                        0f,
                                        progress
                                    )
                                )
                            }
                        }
                    )
                    .height(56.dp)
                    .width(with(density) {
                        // Reads the metrics version so width changes from the
                        // per-frame expand/collapse springs re-compose us.
                        @Suppress("UNUSED_EXPRESSION") tabMetricsVersion
                        widthAtFraction(dampedDragAnimation.targetValue).toDp()
                    }),
            )
        }

        // v247 — CRISP INK OVERLAY: the solid idle pill paints OVER the
        // real tab row (it must — its glass samples the page), which hid
        // the active icon + label. This third copy of the row sits ABOVE
        // the pill with clickables stripped (touches fall through to the
        // real tabs and the blob's drag handlers below), so the ink stays
        // perfectly sharp on top of the solid fill at rest and on top of
        // the press-glass while held.
        // v292g — the overlay is the only source of tab labels for
        // ghostFreeTabs (Pet Designer); at rest it shows crisp labels,
        // during press it fades to reveal the page-only refraction.
        // v292h — always render overlay for ghostFreeTabs (Pet Designer)
        // because the blob samples page-only so the overlay is the ONLY
        // source of tab labels at rest.
        if (!classicIndicator || ghostFreeTabs) {
            CompositionLocalProvider(LocalLiquidGlassTabOverlay provides true) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .graphicsLayer {
                            translationX = panelOffset
                            // v262 — GHOST FIX done right: instead of fading
                            // the overlay linearly with press progress (both
                            // copies half-visible mid-press = double text),
                            // it disappears QUICKLY (~110ms) once a press
                            // starts, leaving ONLY the refraction showing.
                            alpha = inkOverlayAlpha
                        }
                        .height(64.dp)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = measuringContent
                )
            }
        }
    }
}
