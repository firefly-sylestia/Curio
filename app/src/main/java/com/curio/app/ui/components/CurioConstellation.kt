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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * v190 — the pattern is now a BRAIN NEURAL WEB: explored lanes are neurons
 * arranged inside two hemisphere lobes (a fill-ellipse layout — the old
 * arc scatter sat every star in a flat bottom band), linked by curved
 * nearest-neighbour synapses plus inter-hemispheric "corpus callosum"
 * bridges across the midline, with the gold fissure down the centre. Star
 * size = saved count; explored lanes glow when recently active. Tap a
 * neuron to select it; tap empty sky to clear.
 *
 * When [popoverContent] is provided, the selection shows as a small
 * FLOATING card anchored to the neuron (clamped inside the canvas, tap to
 * dismiss) instead of the caller drawing a panel below. The drawer passes
 * a compact name + saved-count chip (user: "a floating small thing and
 * also less data"); the Stats page passes null and keeps its own panel.
 *
 * NOTE (future): the neurons are fed by [CategoryId] data today, but the
 * user plans to replace category lanes with real knowledge-based nodes —
 * the component only reads the id list + count maps, so feeding it a
 * different node model later is a caller-side change.
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
    // Deterministic brain-shaped neuron positions: explored lanes split
    // into two hemisphere lobes. Each lobe is an ellipse that bulges
    // outward at mid-height and tapers toward the midline at top/bottom
    // (the fissure gap) — the classic brain silhouette. The radial scatter
    // fills the lobe interior so the brain reads as a neural mass, not a
    // ring, and the per-lane deterministic jitter keeps positions stable.
    // Real neurons = explored lanes (deterministic per-lane positions inside
    // the two hemisphere lobes, filled via radial scatter so the brain reads
    // as a neural mass). v194 — the DECORATIVE FILLER dots join them: a
    // fixed deterministic ring of small neutral dots per lobe that completes
    // the brain mesh even when few lanes are explored (user: "the neurons
    // dot doesnt create the brain mesh… i told you to add extra dots for
    // decoration and completion of the neuron mesh"). Fillers participate in
    // the link web (so the mesh reads whole) but are NOT tappable and never
    // show a popover — the real neurons stay the interactive data.
    val nodes = remember(explored) {
        explored.mapIndexed { index, id ->
            val total = explored.size
            val side = if (index < (total + 1) / 2) -1 else 1
            val idxInSide = if (side < 0) index else index - (total + 1) / 2
            val nInSide = if (side < 0) (total + 1) / 2 else total / 2
            val t = if (nInSide <= 1) 0.5f else idxInSide.toFloat() / (nInSide - 1)
            val rnd = Random(id.name.hashCode())
            val phi = (-0.5f + t) * PI.toFloat()          // -π/2 (top) .. π/2 (bottom)
            val radial = 0.30f + rnd.nextFloat() * 0.70f  // fill the lobe interior
            val cx = 0.5f + side * 0.155f
            val cy = 0.50f
            val pos = Offset(
                cx + side * cos(phi).toFloat() * 0.135f * radial,
                cy + sin(phi).toFloat() * 0.24f * radial
            )
            id to pos
        }
    }
    val fillers = remember { brainFillerDots() }
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

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(explored, laneCounts, laneRecent) {
                    detectTapGestures { tap ->
                        // Only neurons within the touch radius register;
                        // tapping empty sky clears the selection.
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
            // The neural web spans ALL dots — the decorative fillers and the
            // real neurons together — so the mesh outlines the whole brain
            // even when few lanes are explored. Fillers carry a side flag;
            // real nodes split by index (first half = left lobe).
            val pts = nodes.map { (_, n) -> Offset(n.x * w, n.y * h) }
            val allPts = fillers.map { Offset(it.pos.x * w, it.pos.y * h) } + pts
            val allLeft = fillers.map { it.left } + nodes.indices.map { it < (nodes.size + 1) / 2 }
            val total = allPts.size
            val links = LinkedHashSet<Pair<Int, Int>>()
            allPts.indices.forEach { i ->
                val nearest = allPts.indices
                    .filter { it != i }
                    .sortedBy { j -> sqDist(allPts[i], allPts[j]) }
                    .take(2)
                nearest.forEach { j -> links.add(norm(i, j)) }
                val otherSide = allPts.indices
                    .filter { it != i && allLeft[it] != allLeft[i] }
                    .minByOrNull { j -> sqDist(allPts[i], allPts[j]) }
                if (otherSide != null) links.add(norm(i, otherSide))
            }
            links.forEach { (a, b) ->
                drawCurvedLink(allPts[a], allPts[b], linkColor, 1.dp.toPx())
            }
            // The midline fissure — a soft curve down the brain's centre
            // (the old straight line between two mid nodes is gone).
            if (total >= 3) {
                drawCurvedLink(
                    from = Offset(w * 0.5f, h * 0.24f),
                    to = Offset(w * 0.5f, h * 0.76f),
                    color = fissureColor,
                    stroke = 1.dp.toPx(),
                    sag = 0.04f
                )
            }
            // Decorative fillers — small neutral dots, drawn UNDER the real
            // neurons. They complete the mesh but carry no data: dim, no
            // accent, no glow, no white core, never tappable.
            val fillerColor = if (isCurioDarkTheme()) Color(0xFF8FA6BC).copy(alpha = 0.55f)
                              else Color(0xFF5F7E9A).copy(alpha = 0.55f)
            fillers.forEach { f ->
                drawCircle(
                    color = fillerColor,
                    radius = 2.2.dp.toPx(),
                    center = Offset(f.pos.x * w, f.pos.y * h)
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

        // v190 — floating popover: a small card anchored just above the
        // selected neuron (below it when it sits near the top), clamped
        // inside the canvas. Tapping the card dismisses it.
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

/**
 * Deterministic decorative filler dots that complete the brain mesh: a
 * fixed ring layout per hemisphere lobe (radial 0.35 → 0.9, 5 → 9 dots per
 * ring) with a tiny fixed-seed jitter, so the lobes read as a filled neural
 * mass even when the user has explored few lanes. Same lobe-ellipse math as
 * the real neurons (bulge + taper), so fillers and neurons sit on the same
 * silhouette. Fixed seed → the decoration never moves between recompositions.
 */
private data class BrainFiller(val pos: Offset, val left: Boolean)

private fun brainFillerDots(): List<BrainFiller> {
    val rnd = Random(0x5EEDC0DE) // fixed — decoration never re-rolls
    val out = mutableListOf<BrainFiller>()
    for (side in intArrayOf(-1, 1)) {
        val cx = 0.5f + side * 0.155f
        val cy = 0.50f
        val rings = intArrayOf(5, 7, 9)
        rings.forEachIndexed { ring, count ->
            val radial = 0.35f + ring * 0.27f // 0.35, 0.62, 0.89
            for (i in 0 until count) {
                val t = (i + 0.5f) / count
                val phi = (-0.5f + t) * PI.toFloat() // -π/2 (top) .. π/2 (bottom)
                val jx = (rnd.nextFloat() - 0.5f) * 0.05f
                val jy = (rnd.nextFloat() - 0.5f) * 0.05f
                out += BrainFiller(
                    pos = Offset(
                        cx + side * cos(phi).toFloat() * 0.135f * radial + jx,
                        cy + sin(phi).toFloat() * 0.24f * radial + jy
                    ),
                    left = side < 0
                )
            }
        }
    }
    return out
}

/** Squared distance — the link-pairing comparator. */
private fun sqDist(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy
}

/** Order-independent pair key so each link is drawn once. */
private fun norm(i: Int, j: Int): Pair<Int, Int> = if (i < j) i to j else j to i

/** Draws a gentle curved link — a quadratic bezier sagging perpendicular to
 *  the chord, so the neural web reads as wiring instead of straight grid
 *  lines. The sag side is deterministic per link. */
private fun DrawScope.drawCurvedLink(
    from: Offset,
    to: Offset,
    color: Color,
    stroke: Float,
    sag: Float = 0.12f
) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = sqrt(dx * dx + dy * dy)
    if (len <= 0.001f) return
    val mid = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f)
    // Perpendicular control point: mid ± (dy, -dx) * sag.
    val control = Offset(mid.x - dy * sag, mid.y + dx * sag)
    val path = Path().apply {
        moveTo(from.x, from.y)
        quadraticBezierTo(control.x, control.y, to.x, to.y)
    }
    drawPath(path, color, style = Stroke(stroke))
}
