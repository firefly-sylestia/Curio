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
import androidx.compose.ui.graphics.ColorFilter
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
import com.curio.app.ui.theme.isCurioDarkTheme
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
    Row(
        modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
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
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.40f),
    isBlurEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    // v233 — CLEAR GLASS (experiment): less frost, more refraction.
    val clear = AppPreferences.glassClarityState
    // v233 — light-mode ACTIVE-INDICATOR contrast: the old constant 14%
    // accent wash gave the active ink almost nothing to read against on a
    // bright page; light mode now gets double the bed (dark keeps 16%).
    val dark = isCurioDarkTheme()
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
                animateToValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
                onSelected(targetIndex)
            },
            onDrag = { _, dragAmount ->
                if (totalWidthPx > 0f) {
                    updateValue(
                        (targetValue + dragAmount.x /
                            maxOf(widthAtFraction(targetValue), 1f) * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            }
        ).also { holder.instance = it }
    }

    LaunchedEffect(selectedIndex, dampedDragAnimation) {
        dampedDragAnimation.animateToValue(selectedIndex.toFloat())
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
                            blur((if (clear) 2.dp else 8.dp).toPx())
                            lens(24.dp.toPx(), 24.dp.toPx())
                        }
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = if (isBlurEnabled) 1f else 0f)
                    },
                    shadow = {
                        Shadow.Default.copy(
                            color = Color.Black.copy(alpha = 0.10f)
                        )
                    },
                    onDrawSurface = {
                        // v233 — clear-glass cuts the frost wash to ~a third.
                        drawRect(containerColor.copy(alpha = containerColor.alpha * if (clear) 0.35f else 1f))
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
                                blur((if (clear) 2.dp else 8.dp).toPx())
                                lens(24.dp.toPx() * progress, 24.dp.toPx() * progress)
                            }
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f)
                        },
                        shadow = { null },
                        onDrawSurface = {
                            // v233 — clear-glass cuts the frost wash to ~a third.
                            drawRect(containerColor.copy(alpha = containerColor.alpha * if (clear) 0.35f else 1f))
                        }
                    )
                    .then(interactiveHighlight?.modifier ?: Modifier)
                    .height(56.dp)
                    .padding(horizontal = 4.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
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
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { CircleShape },
                        effects = {
                            if (isBlurEnabled) {
                                val progress = dampedDragAnimation.pressProgress
                                lens(10.dp.toPx() * progress, 14.dp.toPx() * progress, true)
                            }
                        },
                        highlight = {
                            Highlight.Default.copy(
                                alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f
                            )
                        },
                        shadow = {
                            Shadow(alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f)
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 8.dp * dampedDragAnimation.pressProgress,
                                alpha = if (isBlurEnabled) dampedDragAnimation.pressProgress else 0f
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
                            // v232 — a constant faint ACCENT wash marks this pill
                            // as the active-tab indicator even at rest (before,
                            // it only read while pressed); press deepens it.
                            // v233 — theme-aware: light mode doubles the bed so
                            // the active ink actually reads against a bright page.
                            drawRect(accentColor.copy(alpha = if (dark) 0.16f else 0.30f))
                            drawRect(
                                color = Color.Black.copy(alpha = 0.10f),
                                alpha = 1f - progress
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
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
    }
}
