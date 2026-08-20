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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import kotlin.random.Random

/**
 * v211 — NORTH STAR CONSTELLATION: Ursa Minor (the Little Dipper), with
 * Polaris — the Pole Star — as the guiding light. Seven stars form the
 * iconic ladle shape, anchored by the brightest star in the handle.
 *
 * GALAXY AESTHETIC: vibrant deep-space nebula background with overlapping
 * purple, blue, and teal washes. Explored-lane stars are drawn as 4-pointed
 * star shapes, not circles. Background scatter stars are tiny star glyphs
 * for a consistent starfield feel.
 *
 * Light mode: the galaxy palette stays but the edges fade to meet the page
 * background. Dark mode: full-bleed deep space.
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
    // Ursa Minor anchor positions (real star coordinates, normalized).
    // Polaris sits top-left; the handle curves down-right into the bowl.
    val ursaMinorAnchors = remember {
        listOf(
            Offset(0.28f, 0.20f), // Polaris — the Pole Star (α UMi)
            Offset(0.36f, 0.32f), // η UMi — first handle star
            Offset(0.42f, 0.40f), // ζ UMi — second handle star
            Offset(0.50f, 0.48f), // δ UMi — third handle star (bowl junction)
            Offset(0.58f, 0.38f), // ε UMi — bowl top-right
            Offset(0.66f, 0.50f), // β UMi — Kochab (bright bowl star)
            Offset(0.54f, 0.58f)  // γ UMi — Pherkad (bowl bottom)
        )
    }

    // Lane star positions: explored lanes get deterministic spots scattered
    // around the Ursa Minor pattern. Unexplored lanes are not shown.
    val nodes = remember(explored) {
        explored.map { id ->
            id to randomAroundUrsaMinor(Random(id.name.hashCode()))
        }
    }

    // Dim background stars for depth — scattered across the whole canvas.
    val bgStars = remember { backgroundStars() }

    // Theme-aware colors — lane stars use the same neutral palette as the
    // constellation anchors, not category accent colors.
    val isDark = isCurioDarkTheme()
    val linkColor = if (isDark) Color(0xFF6B8CAA).copy(alpha = 0.18f)
                    else Color(0xFF5A7898).copy(alpha = 0.25f)
    val anchorColor = if (isDark) Color(0xFFD8E8F8).copy(alpha = 0.80f)
                      else Color(0xFF7090B0).copy(alpha = 0.65f)
    val polarisColor = if (isDark) Color(0xFFFFF8E8).copy(alpha = 0.95f)
                       else Color(0xFFD0A848).copy(alpha = 0.80f)
    val bgStarColor = if (isDark) Color(0xFF8899BB).copy(alpha = 0.22f)
                      else Color(0xFF8899BB).copy(alpha = 0.15f)
    // Page background for light-mode edge blending (hoisted from Canvas).
    val pageBg = MaterialTheme.colorScheme.background

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(explored, laneCounts, laneRecent) {
                    detectTapGestures { tap ->
                        val hit = nodes.mapNotNull { (id, n) ->
                            val dx = tap.x - n.x * size.width
                            val dy = tap.y - n.y * size.height
                            val d = sqrt(dx * dx + dy * dy)
                            if (d <= 34.dp.toPx()) id to d else null
                        }.minByOrNull { it.second }?.first
                        onSelect(hit)
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // ── Galaxy background: layered nebula washes ───────────
            // Deep space base: dark radial gradient from center.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A0E3A).copy(alpha = if (isDark) 0.65f else 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.40f, h * 0.38f),
                    radius = w * 0.8f
                ),
                radius = w * 0.8f,
                center = Offset(w * 0.40f, h * 0.38f)
            )

            // Purple nebula cloud.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6B2FA0).copy(alpha = if (isDark) 0.22f else 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.55f, h * 0.50f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.55f, h * 0.50f)
            )

            // Teal-blue nebula cloud.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A6090).copy(alpha = if (isDark) 0.20f else 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.30f, h * 0.65f),
                    radius = w * 0.50f
                ),
                radius = w * 0.50f,
                center = Offset(w * 0.30f, h * 0.65f)
            )

            // Warm magenta accent cloud.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFA03070).copy(alpha = if (isDark) 0.16f else 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.70f, h * 0.30f),
                    radius = w * 0.40f
                ),
                radius = w * 0.40f,
                center = Offset(w * 0.70f, h * 0.30f)
            )

            // Faint gold dust near Polaris.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFB89040).copy(alpha = if (isDark) 0.10f else 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.28f, h * 0.20f),
                    radius = w * 0.25f
                ),
                radius = w * 0.25f,
                center = Offset(w * 0.28f, h * 0.20f)
            )

            // ── Light-mode edge blend: soften galaxy into page bg ──
            if (!isDark) {
                // Top edge fade.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            pageBg.copy(alpha = 0.70f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = h * 0.18f
                    ),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(w, h * 0.18f)
                )
                // Bottom edge fade.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            pageBg.copy(alpha = 0.70f)
                        ),
                        startY = h * 0.82f,
                        endY = h
                    ),
                    topLeft = Offset(0f, h * 0.82f),
                    size = androidx.compose.ui.geometry.Size(w, h * 0.18f)
                )
                // Left edge fade.
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            pageBg.copy(alpha = 0.55f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = w * 0.12f
                    ),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(w * 0.12f, h)
                )
                // Right edge fade.
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            pageBg.copy(alpha = 0.55f)
                        ),
                        startX = w * 0.88f,
                        endX = w
                    ),
                    topLeft = Offset(w * 0.88f, 0f),
                    size = androidx.compose.ui.geometry.Size(w * 0.12f, h)
                )
            }

            // ── Background stars: tiny star glyphs for depth ────────
            // Two layers: main starfield + a second pass of even tinier
            // faint dots for a real cosmic feel.
            bgStars.forEach { s ->
                drawStar(
                    center = Offset(s.first.x * w, s.first.y * h),
                    outerRadius = s.second.dp.toPx(),
                    color = bgStarColor,
                    alpha = bgStarColor.alpha
                )
            }
            // Extra tinies — a second scatter at half-size for depth.
            bgStars.forEachIndexed { i, s ->
                if (i % 3 == 0) {
                    // Offset each tiny from its parent to avoid stacking.
                    val ox = ((i * 0.17f) % 1f)
                    val oy = ((i * 0.23f) % 1f)
                    drawStar(
                        center = Offset(
                            ((s.first.x + ox * 0.05f) % 1f) * w,
                            ((s.second.y + oy * 0.05f) % 1f) * h
                        ),
                        outerRadius = (s.second * 0.4f).dp.toPx(),
                        color = bgStarColor,
                        alpha = bgStarColor.alpha * 0.5f
                    )
                }
            }

            // ── Ursa Minor constellation lines: gossamer thin ──────
            // Handle: Polaris → η → ζ → δ (bowl junction)
            // Bowl: δ → ε → β → γ → δ (closing the quadrilateral)
            val anchorPx = ursaMinorAnchors.map { Offset(it.x * w, it.y * h) }
            val ursaEdges = listOf(
                0 to 1, 1 to 2, 2 to 3,  // handle
                3 to 4, 4 to 5, 5 to 6, 6 to 3  // bowl
            )
            ursaEdges.forEach { (a, b) ->
                drawGossamerLink(anchorPx[a], anchorPx[b], linkColor, 0.8.dp.toPx())
            }

            // ── Lane-to-nearest-anchor links: faint connection ──────
            val pts = nodes.map { (_, n) -> Offset(n.x * w, n.y * h) }
            pts.forEach { p ->
                val nearestAnchor = anchorPx.minByOrNull { sqDist(p, it) }
                if (nearestAnchor != null && sqDist(p, nearestAnchor) < 0.08f) {
                    drawGossamerLink(p, nearestAnchor, linkColor.copy(alpha = linkColor.alpha * 0.5f), 0.5.dp.toPx())
                }
            }

            // ── Lane-to-nearest-lane links: sparse web ─────────────
            val drawn = mutableSetOf<Pair<Int, Int>>()
            pts.forEachIndexed { i, pi ->
                val nearest = pts.indices
                    .filter { it != i }
                    .minByOrNull { sqDist(pi, pts[it]) }
                if (nearest != null) {
                    val key = norm(i, nearest)
                    if (drawn.add(key)) {
                        drawGossamerLink(pi, pts[nearest], linkColor.copy(alpha = linkColor.alpha * 0.4f), 0.5.dp.toPx())
                    }
                }
            }

            // ── Polaris: the Pole Star — prominent, warm glow ───────
            val polaris = anchorPx[0]
            // Warm golden halo.
            drawStar(
                center = polaris,
                outerRadius = 10.dp.toPx(),
                color = polarisColor.copy(alpha = 0.20f),
                alpha = 0.20f
            )
            // Core star.
            drawStar(
                center = polaris,
                outerRadius = 4.5.dp.toPx(),
                color = polarisColor,
                alpha = polarisColor.alpha
            )
            // Bright center point.
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = 1.5.dp.toPx(),
                center = polaris
            )

            // ── Ursa Minor anchor stars: slightly smaller, cool ─────
            anchorPx.drop(1).forEach { p ->
                // Soft outer halo.
                drawStar(
                    center = p,
                    outerRadius = 7.dp.toPx(),
                    color = anchorColor.copy(alpha = 0.14f),
                    alpha = 0.14f
                )
                // Core star.
                drawStar(
                    center = p,
                    outerRadius = 3.dp.toPx(),
                    color = anchorColor,
                    alpha = anchorColor.alpha
                )
                // Tiny bright center.
                drawCircle(
                    color = Color.White.copy(alpha = 0.88f),
                    radius = 1.0.dp.toPx(),
                    center = p
                )
            }

            // ── Lane stars: neutral star colors, star shapes ────────
            // These represent explored categories as real stars in the
            // constellation — white/blue-white like the anchor stars,
            // not colorful category accents.
            val laneStarColor = if (isDark) Color(0xFFD0E0F8)
                               else Color(0xFF6080A0)
            nodes.forEachIndexed { i, (id, n) ->
                val p = pts[i]
                val count = laneCounts[id] ?: 0
                val recent = (laneRecent[id] ?: 0L) >= recentCutoff
                val isSel = selected == id

                // Knowledge-sized radius: gentle sqrt ramp, capped at 7dp.
                val r = (2.5f + sqrt(count.coerceAtLeast(0).toFloat()) * 1.6f)
                    .coerceAtMost(7f).dp.toPx()

                // Soft halo — larger when recently active or selected.
                val haloAlpha = if (recent || isSel) 0.18f else 0.08f
                val haloScale = if (recent || isSel) 2.2f else 1.8f
                // Star halo.
                drawStar(
                    center = p,
                    outerRadius = r * haloScale,
                    color = laneStarColor.copy(alpha = haloAlpha),
                    alpha = haloAlpha
                )
                // Star core.
                drawStar(
                    center = p,
                    outerRadius = r,
                    color = laneStarColor,
                    alpha = laneStarColor.alpha
                )
                // Bright center point.
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = r * 0.35f,
                    center = p
                )
            }
        }

        // ── Floating popover for selected star ─────────────────────
        val selId = selected
        val selNode = selId?.let { id -> nodes.firstOrNull { (nid, _) -> nid == id } }
        if (popoverContent != null && selNode != null) {
            var cardSize by remember { mutableStateOf(IntSize.Zero) }
            val nodePx = Offset(selNode.second.x * wPx, selNode.second.y * hPx)
            val gap = with(density) { 8.dp.toPx() }
            val pad = with(density) { 4.dp.toPx() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        val cw = cardSize.width
                        val ch = cardSize.height
                        var x = (nodePx.x - cw / 2f).toInt()
                        var y = (nodePx.y - ch - gap).toInt()
                        if (y < pad.toInt()) y = (nodePx.y + gap).toInt()
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

// ── Ursa Minor geometry ──────────────────────────────────────────────

/**
 * Deterministic position for a lane star, scattered around the Ursa Minor
 * constellation. The star lands in a ring around the constellation center
 * with some angular variance — close enough to feel part of the pattern,
 * far enough to not overlap the anchor stars.
 */
private fun randomAroundUrsaMinor(rnd: Random): Offset {
    // Ursa Minor center: average of the seven anchor positions.
    val cx = 0.45f
    val cy = 0.40f
    val angle = rnd.nextFloat() * PI.toFloat() * 2f
    val dist = 0.12f + rnd.nextFloat() * 0.22f // 12–34% from center
    val x = cx + cos(angle) * dist
    val y = cy + sin(angle) * dist * 0.8f // slightly squished vertically
    return Offset(x.coerceIn(0.06f, 0.94f), y.coerceIn(0.06f, 0.94f))
}

/**
 * Dense background stars scattered across the whole canvas for a real
 * night-sky starfield. Each is (position, radius in dp). Fixed seed —
 * never re-rolls. A mix of tiny dim dots and a few slightly brighter
 * ones for depth.
 */
private fun backgroundStars(): List<Pair<Offset, Float>> {
    val rnd = Random(0x5EED)
    return List(120) {
        Offset(rnd.nextFloat(), rnd.nextFloat()) to (0.3f + rnd.nextFloat() * 0.9f)
    }
}

// ── Drawing helpers ───────────────────────────────────────────────────

/** Squared distance — the link-pairing comparator. */
private fun sqDist(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy
}

/** Order-independent pair key so each link is drawn once. */
private fun norm(i: Int, j: Int): Pair<Int, Int> = if (i < j) i to j else j to i

/**
 * A 4-pointed star shape — the classic twinkling-star glyph.
 * Four points extend outward at 0°, 90°, 180°, 270° with an
 * inner radius at 45° offsets for the characteristic diamond-star look.
 */
private fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    color: Color,
    alpha: Float = 1f
) {
    val innerRadius = outerRadius * 0.38f
    val path = Path()
    for (i in 0 until 8) {
        val angle = (i * PI / 4.0).toFloat() - (PI / 2.0).toFloat()
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val px = center.x + cos(angle) * r
        val py = center.y + sin(angle) * r
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color, alpha = alpha)
}

/**
 * A gossamer constellation link — a thin, faint line between two stars.
 * Much thinner and more transparent than old neural-web links, so the
 * constellation reads as a star chart, not a circuit board.
 */
private fun DrawScope.drawGossamerLink(
    from: Offset,
    to: Offset,
    color: Color,
    stroke: Float
) {
    drawLine(
        color = color,
        start = from,
        end = to,
        strokeWidth = stroke
    )
}
