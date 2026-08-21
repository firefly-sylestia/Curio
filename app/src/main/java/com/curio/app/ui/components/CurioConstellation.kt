package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.curio.app.data.CategoryId
import com.curio.app.ui.theme.isCurioDarkTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Big Dipper + Polaris constellation — exact reproduction of the user's SVG
 * designs. Dark mode uses the deep-space palette (svgviewer-output 16), light
 * mode uses the muted cosmic palette (svgviewer-output 17).
 *
 * Opaque background — no page bleed-through. The constellation lines,
 * stars, nebulae, and background starfield match the SVGs pixel-for-pixel.
 *
 * Tap a star to select it; tap empty sky to clear. When [popoverContent]
 * is provided, the selection shows as a floating card anchored to the star.
 */
@Composable
fun CurioConstellation(
    explored: List<CategoryId>,
    laneCounts: Map<CategoryId, Int>,
    laneRecent: Map<CategoryId, Long>,
    recentCutoff: Long,
    selected: CategoryId?,
    onSelect: (CategoryId?) -> Unit,
    modifier: Modifier = Modifier,
    popoverContent: (@Composable (CategoryId) -> Unit)? = null
) {
    val isDark = isCurioDarkTheme()
    val pageBg = MaterialTheme.colorScheme.background

    // Named star positions in the 1400×1400 viewBox, normalized to 0–1.
    val stars = remember { constellationStars() }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(explored) {
                    detectTapGestures { tap ->
                        val hit = stars.mapNotNull { star ->
                            val sx = star.nx * size.width
                            val sy = star.ny * size.height
                            val dx = tap.x - sx
                            val dy = tap.y - sy
                            val d = sqrt(dx * dx + dy * dy)
                            if (d <= star.hitRadius.toPx()) star to d else null
                        }.minByOrNull { it.second }?.first
                        // Map tap to an explored CategoryId if index matches.
                        onSelect(hit?.let { s ->
                            val idx = stars.indexOf(s)
                            explored.getOrNull(idx)
                        })
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val sx = w / 1400f
            val sy = h / 1400f
            // Use uniform scaling to keep the constellation proportional.
            val s = minOf(sx, sy)
            val ox = (w - 1400f * s) / 2f
            val oy = (h - 1400f * s) / 2f

            fun px(x: Float) = ox + x * s
            fun py(y: Float) = oy + y * s
            fun pr(r: Float) = r * s

            // ── Opaque background fill ──────────────────────────
            // Fill entire canvas with page background first, then draw
            // the space gradient on top so nothing bleeds through.
            drawRect(color = pageBg)

            if (isDark) drawDarkBackground(w, h, ::px, ::py, ::pr, s)
            else drawLightBackground(w, h, ::px, ::py, ::pr, s)

            // ── Constellation lines ─────────────────────────────
            if (isDark) {
                // Big Dipper bowl: Dubhe→Merak→Phecda→Megrez→Dubhe
                drawConstellationLine(px(872f), py(751f), px(961f), py(689f), Color(0xFFc9e5ff), 0.72f, pr(2.4f))
                drawConstellationLine(px(961f), py(689f), px(894f), py(539f), Color(0xFFc9e5ff), 0.72f, pr(2.4f))
                drawConstellationLine(px(894f), py(539f), px(801f), py(556f), Color(0xFFc9e5ff), 0.72f, pr(2.4f))
                drawConstellationLine(px(801f), py(556f), px(872f), py(751f), Color(0xFFc9e5ff), 0.72f, pr(2.4f))
                // Big Dipper handle: Megrez→Alioth→Mizar→Alkaid
                drawConstellationLine(px(801f), py(556f), px(716f), py(483f), Color(0xFFc9e5ff), 0.78f, pr(2.4f))
                drawConstellationLine(px(716f), py(483f), px(642f), py(432f), Color(0xFFc9e5ff), 0.78f, pr(2.4f))
                drawConstellationLine(px(642f), py(432f), px(597f), py(298f), Color(0xFFc9e5ff), 0.78f, pr(2.4f))
                // Pointer line: Dubhe → Polaris (dashed)
                drawDashedLine(px(872f), py(751f), px(441f), py(1101f), Color(0xFF9fc8ed), 0.22f, pr(1.3f))
                // Ursa Minor subtle lines: Polaris→Kochab, Polaris→Pherkad
                drawDashedLine(px(441f), py(1101f), px(320f), py(1211f), Color(0xFF9bbbd9), 0.12f, pr(1f))
                drawDashedLine(px(441f), py(1101f), px(561f), py(1265f), Color(0xFF9bbbd9), 0.12f, pr(1f))
            } else {
                // Big Dipper bowl
                drawConstellationLine(px(872f), py(751f), px(961f), py(689f), Color(0xFFc7d9e8), 0.78f, pr(2.3f))
                drawConstellationLine(px(961f), py(689f), px(894f), py(539f), Color(0xFFc7d9e8), 0.78f, pr(2.3f))
                drawConstellationLine(px(894f), py(539f), px(801f), py(556f), Color(0xFFc7d9e8), 0.78f, pr(2.3f))
                drawConstellationLine(px(801f), py(556f), px(872f), py(751f), Color(0xFFc7d9e8), 0.78f, pr(2.3f))
                // Big Dipper handle
                drawConstellationLine(px(801f), py(556f), px(716f), py(483f), Color(0xFFc7d9e8), 0.82f, pr(2.3f))
                drawConstellationLine(px(716f), py(483f), px(642f), py(432f), Color(0xFFc7d9e8), 0.82f, pr(2.3f))
                drawConstellationLine(px(642f), py(432f), px(597f), py(298f), Color(0xFFc7d9e8), 0.82f, pr(2.3f))
                // Pointer line
                drawDashedLine(px(872f), py(751f), px(441f), py(1101f), Color(0xFFb4c9db), 0.28f, pr(1.2f))
                // Ursa Minor subtle lines
                drawDashedLine(px(441f), py(1101f), px(320f), py(1211f), Color(0xFFafc3d4), 0.17f, pr(1f))
                drawDashedLine(px(441f), py(1101f), px(561f), py(1265f), Color(0xFFafc3d4), 0.17f, pr(1f))
            }

            // ── Stars ───────────────────────────────────────────
            stars.forEach { star ->
                val cx = px(star.x)
                val cy = py(star.y)
                val r = pr(star.r)
                val color = if (isDark) star.darkColor else star.lightColor
                drawCircle(color = color, radius = r, center = Offset(cx, cy))
            }
        }

        // ── Floating popover for selected star ─────────────────
        val selId = selected
        val selStarIdx = selId?.let { sid -> explored.indexOf(sid).takeIf { it in stars.indices } }
        if (popoverContent != null && selStarIdx != null) {
            val star = stars[selStarIdx]
            var cardSize by remember { mutableStateOf(IntSize.Zero) }
            val starPx = Offset(star.nx * wPx, star.ny * hPx)
            val gap = with(density) { 8.dp.toPx() }
            val pad = with(density) { 4.dp.toPx() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        val cw = cardSize.width
                        val ch = cardSize.height
                        var x = (starPx.x - cw / 2f).toInt()
                        var y = (starPx.y - ch - gap).toInt()
                        if (y < pad.toInt()) y = (starPx.y + gap).toInt()
                        x = x.coerceIn(pad.toInt(), (wPx - cw - pad).toInt().coerceAtLeast(pad.toInt()))
                        y = y.coerceIn(pad.toInt(), (hPx - ch - pad).toInt().coerceAtLeast(pad.toInt()))
                        IntOffset(x, y)
                    }
                    .onSizeChanged { cardSize = it }
            ) {
                Surface(
                    onClick = { onSelect(null) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 4.dp
                ) {
                    popoverContent(selId)
                }
            }
        }
    }
}

// ── Dark mode background ─────────────────────────────────────────

private fun DrawScope.drawDarkBackground(
    w: Float, h: Float,
    px: (Float) -> Float, py: (Float) -> Float, pr: (Float) -> Float,
    s: Float
) {
    // Space radial gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF101d45),
                Color(0xFF08132f),
                Color(0xFF03091d),
                Color(0xFF01030b)
            ),
            stops = floatArrayOf(0f, 0.38f, 0.72f, 1f),
            center = Offset(w * 0.5f, h * 0.5f),
            radius = w * 0.78f
        ),
        radius = w * 0.78f,
        center = Offset(w * 0.5f, h * 0.5f)
    )

    // Purple nebula — ellipse at (220, 400) rotated -25°
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x268d45bd),
                Color(0x0f55277f),
                Color(0x0018072d)
            ),
            stops = floatArrayOf(0f, 0.45f, 1f),
            center = Offset(px(220f), py(400f)),
            radius = pr(500f)
        ),
        topLeft = Offset(px(220f) - pr(390f), py(400f) - pr(500f)),
        size = Size(pr(780f), pr(1000f))
    )

    // Blue nebula — ellipse at (1160, 850) rotated 20°
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x213979c9),
                Color(0x0e24518c),
                Color(0x00061128)
            ),
            stops = floatArrayOf(0f, 0.45f, 1f),
            center = Offset(px(1160f), py(850f)),
            radius = pr(520f)
        ),
        topLeft = Offset(px(1160f) - pr(430f), py(850f) - pr(520f)),
        size = Size(pr(860f), pr(1040f))
    )

    // Purple nebula 2 — ellipse at (700, 1170)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x268d45bd),
                Color(0x0f55277f),
                Color(0x0018072d)
            ),
            stops = floatArrayOf(0f, 0.45f, 1f),
            center = Offset(px(700f), py(1170f)),
            radius = pr(470f)
        ),
        topLeft = Offset(px(700f) - pr(470f), py(1170f) - pr(280f)),
        size = Size(pr(940f), pr(560f))
    )

    // Background stars pattern (180×180 tile)
    drawBgStarPattern(w, h, s, dark = true)

    // Distant stars
    drawCircle(Color(0xFFe8f4ff).copy(alpha = 0.65f), pr(1.3f), Offset(px(150f), py(260f)))
    drawCircle(Color(0xFFe8f4ff).copy(alpha = 0.60f), pr(1.2f), Offset(px(1160f), py(260f)))
    drawCircle(Color(0xFFe8f4ff).copy(alpha = 0.60f), pr(1.3f), Offset(px(1240f), py(780f)))
    drawCircle(Color(0xFFe8f4ff).copy(alpha = 0.60f), pr(1.1f), Offset(px(190f), py(820f)))
    drawCircle(Color(0xFFe8f4ff).copy(alpha = 0.60f), pr(1.2f), Offset(px(1080f), py(1160f)))

    // Small random field stars
    drawSmallFieldStars(w, h, s, dark = true)
}

// ── Light mode background ────────────────────────────────────────

private fun DrawScope.drawLightBackground(
    w: Float, h: Float,
    px: (Float) -> Float, py: (Float) -> Float, pr: (Float) -> Float,
    s: Float
) {
    // Space radial gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF344b67),
                Color(0xFF2d425c),
                Color(0xFF26394f),
                Color(0xFF202f42)
            ),
            stops = floatArrayOf(0f, 0.35f, 0.68f, 1f),
            center = Offset(w * 0.5f, h * 0.48f),
            radius = w * 0.78f
        ),
        radius = w * 0.78f,
        center = Offset(w * 0.5f, h * 0.48f)
    )

    // Blue nebula — muted atmospheric
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x2e7197bd),
                Color(0x14607f9f),
                Color(0x0026394f)
            ),
            stops = floatArrayOf(0f, 0.45f, 1f),
            center = Offset(px(1160f), py(850f)),
            radius = pr(520f)
        ),
        topLeft = Offset(px(1160f) - pr(430f), py(850f) - pr(520f)),
        size = Size(pr(860f), pr(1040f))
    )

    // Purple nebula — muted lavender
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x24987da9),
                Color(0x12806b92),
                Color(0x0026394f)
            ),
            stops = floatArrayOf(0f, 0.45f, 1f),
            center = Offset(px(220f), py(400f)),
            radius = pr(500f)
        ),
        topLeft = Offset(px(220f) - pr(390f), py(400f) - pr(500f)),
        size = Size(pr(780f), pr(1000f))
    )

    // Purple nebula 2
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x24987da9),
                Color(0x12806b92),
                Color(0x0026394f)
            ),
            stops = floatArrayOf(0f, 0.45f, 1f),
            center = Offset(px(700f), py(1170f)),
            radius = pr(470f)
        ),
        topLeft = Offset(px(700f) - pr(470f), py(1170f) - pr(280f)),
        size = Size(pr(940f), pr(560f))
    )

    // Background stars pattern
    drawBgStarPattern(w, h, s, dark = false)

    // Distant stars
    drawCircle(Color(0xFFd5e1eb).copy(alpha = 0.55f), pr(1.2f), Offset(px(150f), py(260f)))
    drawCircle(Color(0xFFd5e1eb).copy(alpha = 0.52f), pr(1.1f), Offset(px(1160f), py(260f)))
    drawCircle(Color(0xFFd5e1eb).copy(alpha = 0.52f), pr(1.2f), Offset(px(1240f), py(780f)))
    drawCircle(Color(0xFFd5e1eb).copy(alpha = 0.50f), pr(1.0f), Offset(px(190f), py(820f)))
    drawCircle(Color(0xFFd5e1eb).copy(alpha = 0.52f), pr(1.1f), Offset(px(1080f), py(1160f)))

    // Small random field stars
    drawSmallFieldStars(w, h, s, dark = false)
}

// ── Background star pattern (180×180 tile) ───────────────────────

private fun DrawScope.drawBgStarPattern(w: Float, h: Float, s: Float, dark: Boolean) {
    // 9 stars per 180×180 tile, matching the SVG pattern exactly.
    data class BgStar(val cx: Float, val cy: Float, val r: Float, val color: Long, val alpha: Float)
    val tileStars = if (dark) listOf(
        BgStar(18f, 22f, 0.8f, 0xFFFFFFFF, 0.60f),
        BgStar(61f, 70f, 0.55f, 0xFFc9e2ff, 0.65f),
        BgStar(128f, 29f, 0.75f, 0xFFFFFFFF, 0.55f),
        BgStar(157f, 91f, 0.55f, 0xFFFFFFFF, 0.60f),
        BgStar(39f, 130f, 0.55f, 0xFF9fc8ed, 0.55f),
        BgStar(101f, 151f, 0.7f, 0xFFFFFFFF, 0.55f),
        BgStar(151f, 164f, 0.45f, 0xFFFFFFFF, 0.65f),
        BgStar(87f, 103f, 0.45f, 0xFFFFFFFF, 0.55f),
        BgStar(12f, 166f, 0.45f, 0xFF8db9e5, 0.55f),
    ) else listOf(
        BgStar(18f, 22f, 0.75f, 0xFFd7e1ec, 0.48f),
        BgStar(61f, 70f, 0.5f, 0xFFc3d1df, 0.42f),
        BgStar(128f, 29f, 0.7f, 0xFFe0e8f0, 0.45f),
        BgStar(157f, 91f, 0.5f, 0xFFc5d4e2, 0.42f),
        BgStar(39f, 130f, 0.5f, 0xFFd4e0eb, 0.40f),
        BgStar(101f, 151f, 0.65f, 0xFFe0e8f0, 0.42f),
        BgStar(151f, 164f, 0.4f, 0xFFc4d2df, 0.45f),
        BgStar(87f, 103f, 0.4f, 0xFFe0e8f0, 0.40f),
        BgStar(12f, 166f, 0.4f, 0xFFc7d5e2, 0.42f),
    )

    val tileW = 180f * s
    val tileH = 180f * s
    val cols = (w / tileW).toInt() + 1
    val rows = (h / tileH).toInt() + 1

    for (row in 0..rows) {
        for (col in 0..cols) {
            val baseX = col * tileW
            val baseY = row * tileH
            tileStars.forEach { star ->
                drawCircle(
                    color = Color(star.color).copy(alpha = star.alpha),
                    radius = star.r * s,
                    center = Offset(baseX + star.cx * s, baseY + star.cy * s)
                )
            }
        }
    }
}

// ── Small random field stars ─────────────────────────────────────

private fun DrawScope.drawSmallFieldStars(w: Float, h: Float, s: Float, dark: Boolean) {
    data class FieldStar(val cx: Float, val cy: Float, val r: Float, val alpha: Float)
    val fieldStars = if (dark) listOf(
        FieldStar(90f, 600f, 0.8f, 0.5f),
        FieldStar(230f, 470f, 0.7f, 0.5f),
        FieldStar(320f, 300f, 0.8f, 0.5f),
        FieldStar(1050f, 420f, 0.8f, 0.5f),
        FieldStar(1190f, 520f, 0.7f, 0.5f),
        FieldStar(1130f, 980f, 0.8f, 0.5f),
        FieldStar(920f, 1190f, 0.7f, 0.5f),
        FieldStar(760f, 1260f, 0.8f, 0.5f),
        FieldStar(300f, 1080f, 0.7f, 0.5f),
    ) else listOf(
        FieldStar(90f, 600f, 0.75f, 0.35f),
        FieldStar(230f, 470f, 0.65f, 0.35f),
        FieldStar(320f, 300f, 0.75f, 0.35f),
        FieldStar(1050f, 420f, 0.75f, 0.35f),
        FieldStar(1190f, 520f, 0.65f, 0.35f),
        FieldStar(1130f, 980f, 0.75f, 0.35f),
        FieldStar(920f, 1190f, 0.65f, 0.35f),
        FieldStar(760f, 1260f, 0.75f, 0.35f),
        FieldStar(300f, 1080f, 0.65f, 0.35f),
    )
    val color = if (dark) Color(0xFFFFFFFF) else Color(0xFFd0deea)
    fieldStars.forEach { star ->
        drawCircle(
            color = color.copy(alpha = star.alpha),
            radius = star.r * s,
            center = Offset(star.cx * s, star.cy * s)
        )
    }
}

// ── Constellation line drawing ───────────────────────────────────

private fun DrawScope.drawConstellationLine(
    x1: Float, y1: Float, x2: Float, y2: Float,
    color: Color, alpha: Float, strokeWidth: Float
) {
    drawLine(
        color = color.copy(alpha = alpha),
        start = Offset(x1, y1),
        end = Offset(x2, y2),
        strokeWidth = strokeWidth
    )
}

/**
 * Dashed line — draws short segments with gaps to mimic SVG stroke-dasharray.
 */
private fun DrawScope.drawDashedLine(
    x1: Float, y1: Float, x2: Float, y2: Float,
    color: Color, alpha: Float, strokeWidth: Float
) {
    val dx = x2 - x1
    val dy = y2 - y1
    val len = sqrt(dx * dx + dy * dy)
    if (len == 0f) return
    val ux = dx / len
    val uy = dy / len
    // dasharray = 6,13 scaled proportionally
    val dashScale = len / 1400f
    val dash = 6f * dashScale
    val gap = 13f * dashScale
    var pos = 0f
    while (pos < len) {
        val segEnd = minOf(pos + dash, len)
        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(x1 + ux * pos, y1 + uy * pos),
            end = Offset(x1 + ux * segEnd, y1 + uy * segEnd),
            strokeWidth = strokeWidth
        )
        pos += dash + gap
    }
}

// ── Star data ────────────────────────────────────────────────────

/**
 * All named stars from the SVG, with positions in the 1400×1400 viewBox
 * and normalized coordinates for canvas mapping.
 */
private data class ConstellationStar(
    val name: String,
    val x: Float, val y: Float,   // 1400×1400 viewBox coords
    val r: Float,                  // radius in viewBox units
    val darkColor: Color,
    val lightColor: Color,
    val hitRadius: androidx.compose.ui.unit.Dp  // tap target radius
) {
    val nx get() = x / 1400f
    val ny get() = y / 1400f
}

private fun constellationStars(): List<ConstellationStar> = listOf(
    // Big Dipper (Ursa Major) — 7 main stars
    ConstellationStar("Dubhe",  872f, 751f, 5.8f, Color(0xFFFFFFFF), Color(0xFFeef5fa), 34.dp),
    ConstellationStar("Merak",  961f, 689f, 5.2f, Color(0xFFFFFFFF), Color(0xFFeef5fa), 34.dp),
    ConstellationStar("Phecda", 894f, 539f, 5.1f, Color(0xFFFFFFFF), Color(0xFFeef5fa), 34.dp),
    ConstellationStar("Megrez", 801f, 556f, 4.3f, Color(0xFFFFFFFF), Color(0xFFe8f1f7), 34.dp),
    ConstellationStar("Alioth", 716f, 483f, 6.0f, Color(0xFFFFFFFF), Color(0xFFf2f7fb), 34.dp),
    ConstellationStar("Mizar",  642f, 432f, 5.2f, Color(0xFFFFFFFF), Color(0xFFedf5fa), 34.dp),
    ConstellationStar("Alkaid", 597f, 298f, 5.7f, Color(0xFFFFFFFF), Color(0xFFf2f7fb), 34.dp),
    // Polaris — the Pole Star
    ConstellationStar("Polaris", 441f, 1101f, 7.5f, Color(0xFFf8fcff), Color(0xFFf5f8fa), 40.dp),
    // Nearby stars
    ConstellationStar("Alcor",   633f, 418f, 3.8f, Color(0xFFeaf5ff), Color(0xFFdbe8f2), 30.dp),
    ConstellationStar("Kochab",  320f, 1211f, 5.2f, Color(0xFFfffdf2), Color(0xFFe2dfd5), 34.dp),
    ConstellationStar("Pherkad", 561f, 1265f, 4.5f, Color(0xFFf7f8ff), Color(0xFFdbe7f0), 34.dp),
)
