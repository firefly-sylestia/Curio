package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.themedAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * v186 — the "Your Curiosity" constellation, SHARED between the Stats page
 * and the Home drawer (the drawer used to draw its own grid-web "map" —
 * the user: "show the your constellation from the your curiocity page not
 * another thing"). The exact same component renders in both places so the
 * two can never drift apart.
 *
 * Deterministic brain-shaped layout: explored lanes split into two lobes
 * (left/right hemisphere), each star sized by its saved count, glowing
 * when recently explored. Every star links to its 2 closest neighbours
 * (a nearest-neighbour web — reads as one connected map in light AND dark)
 * with a gold fissure bridging the hemispheres. Tap a star to select it;
 * tap empty sky to clear.
 *
 * The caller owns the selection state + the detail panel below.
 */
@Composable
fun CurioConstellation(
    explored: List<CategoryId>,
    laneCounts: Map<CategoryId, Int>,
    laneRecent: Map<CategoryId, Long>,
    recentCutoff: Long,
    selected: CategoryId?,
    onSelect: (CategoryId?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Deterministic node positions: two lobes (left / right hemisphere).
    val nodes = remember(explored) {
        explored.mapIndexed { index, id ->
            val total = explored.size
            val side = if (index < (total + 1) / 2) -1 else 1
            val idxInSide = if (side < 0) index else index - (total + 1) / 2
            val nInSide = if (side < 0) (total + 1) / 2 else total / 2
            val t = if (nInSide <= 1) 0.5f else idxInSide.toFloat() / (nInSide - 1)
            val angle = (0.28f + t * 0.44f) * PI.toFloat()
            val rnd = Random(id.name.hashCode())
            val jx = (rnd.nextFloat() - 0.5f) * 0.05f
            val jy = (rnd.nextFloat() - 0.5f) * 0.05f
            val cx = 0.5f + side * 0.12f
            val cy = 0.52f
            val pos = Offset(
                cx + side * cos(angle.toDouble()).toFloat() * 0.21f + jx,
                cy + sin(angle.toDouble()).toFloat() * 0.33f + jy
            )
            id to pos
        }
    }
    // Accents must resolve in COMPOSITION (themedAccent is @Composable and
    // can't run inside the Canvas draw lambda below; recomputing the small
    // map per recomposition is cheap).
    val accents = nodes.associate { (id, _) -> id to CurioCategories.byId(id).themedAccent() }
    // Theme-aware link inks (audit fix: the old steel-blue at 0.22 alpha
    // vanished on the light card surface). Resolved in COMPOSITION —
    // isCurioDarkTheme can't run in the Canvas draw lambda below.
    val linkColor = if (isCurioDarkTheme()) Color(0xFF7FAFD8).copy(alpha = 0.32f)
                    else Color(0xFF5F7E9A).copy(alpha = 0.50f)
    val fissureColor = if (isCurioDarkTheme()) Color(0xFFD9A85C).copy(alpha = 0.30f)
                       else Color(0xFFA97F3C).copy(alpha = 0.45f)

    Canvas(
        modifier = modifier.pointerInput(explored, laneCounts, laneRecent) {
            detectTapGestures { tap ->
                // Only stars within the touch radius register; tapping empty
                // sky clears the selection.
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
        val pts = nodes.map { (_, n) -> Offset(n.x * w, n.y * h) }
        // Nearest-neighbour WEB: every star links to its 2 closest stars,
        // so the constellation reads as one connected map (the old
        // lane-order chain + single fissure were nearly invisible in light
        // mode). Each pair is deduped; the gold fissure below bridges the
        // two hemispheres.
        val links = LinkedHashSet<Pair<Int, Int>>()
        nodes.indices.forEach { i ->
            val nearest = nodes.indices
                .filter { it != i }
                .sortedBy { j ->
                    val dx = pts[i].x - pts[j].x
                    val dy = pts[i].y - pts[j].y
                    dx * dx + dy * dy
                }
                .take(2)
            nearest.forEach { j ->
                links.add(if (i < j) i to j else j to i)
            }
        }
        links.forEach { (a, b) ->
            drawLine(
                color = linkColor,
                start = pts[a],
                end = pts[b],
                strokeWidth = 1.dp.toPx()
            )
        }
        if (nodes.size >= 3) {
            val mid = nodes.size / 2
            drawLine(
                color = fissureColor,
                start = pts[mid - 1],
                end = pts[mid],
                strokeWidth = 1.dp.toPx()
            )
        }
        nodes.forEachIndexed { i, (id, n) ->
            val p = pts[i]
            val accent = accents[id] ?: Color(0xFF7FAFD8)
            val count = laneCounts[id] ?: 0
            val recent = (laneRecent[id] ?: 0L) >= recentCutoff
            val r = (5.5f + kotlin.math.min(count, 60).toFloat() * 0.30f).dp.toPx()
            val isSel = selected == id
            drawCircle(
                color = accent.copy(alpha = if (recent) 0.20f else 0.10f),
                radius = r * (if (recent || isSel) 2.6f else 2.2f),
                center = p
            )
            drawCircle(color = accent, radius = r, center = p)
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = r * 0.42f, center = p)
        }
    }
}
