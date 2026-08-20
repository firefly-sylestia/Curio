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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.themedAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * v210 — REAL CONSTELLATION: the Corvus (The Crow) star pattern, the
 * mythological symbol of curiosity. Apollo placed the crow in the sky
 * because its curiosity led it to seek forbidden knowledge — the
 * perfect emblem for Curio.
 *
 * The four anchor stars (Gienah, Kraz, Algorab, Minkar) form the
 * constellation's characteristic quadrilateral. Explored lane stars are
 * scattered around the pattern. Decorative background stars add depth.
 *
 * SPACE AESTHETIC: deep void background with a faint nebula wash, thin
 * gossamer constellation lines, small bright stars with soft halos — no
 * brain mesh, no bright glows, no clutter.
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
    // Corvus anchor positions (real star coordinates, normalized).
    // The quadrilateral sits in the upper-center of the canvas.
    val corvusAnchors = remember {
        listOf(
            Offset(0.38f, 0.28f), // Gienah ( brightest)
            Offset(0.52f, 0.42f), // Kraz
            Offset(0.34f, 0.48f), // Algorab
            Offset(0.44f, 0.58f)  // Minkar
        )
    }

    // Lane star positions: explored lanes get deterministic spots scattered
    // around the Corvus pattern. Unexplored lanes are not shown.
    val nodes = remember(explored) {
        explored.map { id ->
            id to randomAroundCorvus(Random(id.name.hashCode()))
        }
    }

    // Dim background stars for depth — scattered across the whole canvas.
    val bgStars = remember { backgroundStars() }

    // Accent colors resolved in composition (can't call themedAccent in draw).
    val accents = nodes.associate { (id, _) -> id to CurioCategories.byId(id).themedAccent() }

    // Theme-aware colors.
    val isDark = isCurioDarkTheme()
    val linkColor = if (isDark) Color(0xFF5A7A9A).copy(alpha = 0.22f)
                    else Color(0xFF4A6A88).copy(alpha = 0.30f)
    val anchorColor = if (isDark) Color(0xFFD8E4F0).copy(alpha = 0.70f)
                      else Color(0xFF607888).copy(alpha = 0.55f)
    val bgStarColor = if (isDark) Color(0xFF8899AA).copy(alpha = 0.25f)
                      else Color(0xFF8899AA).copy(alpha = 0.18f)
    val nebulaCenter = if (isDark) Color(0xFF1A1040).copy(alpha = 0.35f)
                       else Color(0xFFD8CCE8).copy(alpha = 0.12f)
    val nebulaEdge = Color.Transparent

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

            // ── Space background: faint radial nebula ──────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(nebulaCenter, nebulaEdge),
                    center = Offset(w * 0.45f, h * 0.42f),
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.45f, h * 0.42f)
            )

            // ── Background stars: tiny dim points for depth ────────────
            bgStars.forEach { s ->
                drawCircle(
                    color = bgStarColor,
                    radius = s.second.dp.toPx(),
                    center = Offset(s.first.x * w, s.first.y * h)
                )
            }

            // ── Corvus constellation lines: gossamer thin ──────────────
            // The four edges of the quadrilateral.
            val anchorPx = corvusAnchors.map { Offset(it.x * w, it.y * h) }
            val corvusEdges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)
            corvusEdges.forEach { (a, b) ->
                drawGossamerLink(anchorPx[a], anchorPx[b], linkColor, 0.8.dp.toPx())
            }

            // ── Lane-to-nearest-anchor links: faint connection ──────────
            val pts = nodes.map { (_, n) -> Offset(n.x * w, n.y * h) }
            pts.forEach { p ->
                val nearestAnchor = anchorPx.minByOrNull { sqDist(p, it) }
                if (nearestAnchor != null && sqDist(p, nearestAnchor) < 0.08f) {
                    drawGossamerLink(p, nearestAnchor, linkColor.copy(alpha = linkColor.alpha * 0.5f), 0.5.dp.toPx())
                }
            }

            // ── Lane-to-nearest-lane links: sparse web ─────────────────
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

            // ── Corvus anchor stars: slightly larger, neutral bright ───
            anchorPx.forEach { p ->
                // Soft outer halo.
                drawCircle(
                    color = anchorColor.copy(alpha = 0.12f),
                    radius = 8.dp.toPx(),
                    center = p
                )
                // Core star.
                drawCircle(
                    color = anchorColor,
                    radius = 3.dp.toPx(),
                    center = p
                )
                // Tiny bright center.
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = 1.2.dp.toPx(),
                    center = p
                )
            }

            // ── Lane stars: category-accent colored, sized by knowledge ─
            nodes.forEachIndexed { i, (id, n) ->
                val p = pts[i]
                val accent = accents[id] ?: Color(0xFF7FAFD8)
                val count = laneCounts[id] ?: 0
                val recent = (laneRecent[id] ?: 0L) >= recentCutoff
                val isSel = selected == id

                // Knowledge-sized radius: gentle sqrt ramp, capped at 7dp.
                val r = (2.5f + sqrt(count.coerceAtLeast(0).toFloat()) * 1.6f)
                    .coerceAtMost(7f).dp.toPx()

                // Soft accent halo — larger when recently active or selected.
                val haloAlpha = if (recent || isSel) 0.18f else 0.08f
                val haloScale = if (recent || isSel) 2.2f else 1.8f
                drawCircle(
                    color = accent.copy(alpha = haloAlpha),
                    radius = r * haloScale,
                    center = p
                )
                // Star core.
                drawCircle(color = accent, radius = r, center = p)
                // Bright center point.
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = r * 0.35f,
                    center = p
                )
            }
        }

        // ── Floating popover for selected star ─────────────────────────
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

// ── Corvus geometry ───────────────────────────────────────────────────

/**
 * Deterministic position for a lane star, scattered around the Corvus
 * constellation. The star lands in a ring around the constellation center
 * with some angular variance — close enough to feel part of the pattern,
 * far enough to not overlap the anchor stars.
 */
private fun randomAroundCorvus(rnd: Random): Offset {
    // Corvus center: average of the four anchor positions.
    val cx = 0.42f
    val cy = 0.44f
    val angle = rnd.nextFloat() * PI.toFloat() * 2f
    val dist = 0.12f + rnd.nextFloat() * 0.22f // 12–34% from center
    val x = cx + cos(angle) * dist
    val y = cy + kotlin.math.sin(angle) * dist * 0.8f // slightly squished vertically
    return Offset(x.coerceIn(0.06f, 0.94f), y.coerceIn(0.06f, 0.94f))
}

/**
 * Sparse background stars scattered across the whole canvas for depth.
 * Each is (position, radius in dp). Fixed seed — never re-rolls.
 */
private fun backgroundStars(): List<Pair<Offset, Float>> {
    val rnd = Random(0x5EED)
    return List(40) {
        Offset(rnd.nextFloat(), rnd.nextFloat()) to (0.4f + rnd.nextFloat() * 0.8f)
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
 * A gossamer constellation link — a thin, faint line between two stars.
 * Much thinner and more transparent than the old neural-web links, so
 * the constellation reads as a star chart, not a circuit board.
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
