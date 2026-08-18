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
 * size = knowledge score; explored lanes glow when recently active. Tap a
 * neuron to select it; tap empty sky to clear.
 *
 * v202 — REDESIGNED to the HUMAN-BRAIN SIDE PROFILE (user: "why it doesnt
 * look like a brain like the human brain design it should follow that and
 * the dots should be random not some in left and some in right", and "the
 * mesh is too much"). The old two side-by-side ellipses read as generic
 * blobs; the new silhouette is the classic anatomy profile — frontal pole,
 * smooth cerebrum dome, occipital pole and the cerebellum bump at the
 * back-bottom — drawn as a faint outline so the shape reads instantly.
 * EVERY dot (decorative fillers AND real lane neurons) is now scattered
 * RANDOMLY inside that silhouette via deterministic seeded rejection
 * sampling (no left/right partition, no per-lobe rings), and the web is a
 * light NEAREST-NEIGHBOUR graph (one synapse per dot, ~60% fewer lines)
 * instead of the dense 2-nearest + cross-bridge mesh. The gold midline
 * fissure is gone with the two-lobe layout. Real lane neurons keep their
 * per-id deterministic spots (stable as lanes are added), stay tappable
 * and keep the saved-count sizing + recent glow.
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
    // Deterministic brain-shaped neuron positions: every explored lane gets
    // a RANDOM spot inside the human-brain silhouette, seeded by its id so
    // positions are stable across recompositions and as lanes are added —
    // no left/right hemisphere partition (user: "the dots should be random
    // not some in left and some in right"). v202 — the old two-lobe
    // ellipse math is gone; [randomInBrain] rejection-samples inside the
    // side-profile silhouette.
    val nodes = remember(explored) {
        explored.map { id ->
            id to randomInBrain(Random(id.name.hashCode()))
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
    // v202 — a faint outline of the brain silhouette (same steel, dimmer
    // than the links) so the profile reads as a brain at a glance.
    val outlineColor = linkColor.copy(alpha = linkColor.alpha * 0.45f)
    // v195 — decorative filler dots wear the same neutral steel ink as the
    // links. Resolved in COMPOSITION (isCurioDarkTheme can't run in the
    // Canvas draw lambda below).
    val fillerColor = if (isCurioDarkTheme()) Color(0xFF8FA6BC).copy(alpha = 0.55f)
                      else Color(0xFF5F7E9A).copy(alpha = 0.55f)

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
            // v202 — light NEAREST-NEIGHBOUR web: one synapse per dot (the
            // densest-to-sparsest chain), ~60% fewer lines than the old
            // 2-nearest + cross-bridge mesh (user: "the mesh is too much").
            // The web spans the fillers AND the real neurons together so it
            // outlines the whole brain even when few lanes are explored.
            val pts = nodes.map { (_, n) -> Offset(n.x * w, n.y * h) }
            val allPts = fillers.map { Offset(it.x * w, it.y * h) } + pts
            val links = LinkedHashSet<Pair<Int, Int>>()
            allPts.indices.forEach { i ->
                val nearest = allPts.indices
                    .filter { it != i }
                    .minByOrNull { j -> sqDist(allPts[i], allPts[j]) }
                if (nearest != null) links.add(norm(i, nearest))
            }
            // The brain silhouette outline — drawn FIRST so it sits behind
            // the links and dots (the faintest element on the canvas).
            drawBrainOutline(w, h, outlineColor, 1.dp.toPx())
            links.forEach { (a, b) ->
                drawCurvedLink(allPts[a], allPts[b], linkColor, 1.dp.toPx())
            }
            // Decorative fillers — small neutral dots, drawn UNDER the real
            // neurons. They complete the mesh but carry no data: dim, no
            // accent, no glow, no white core, never tappable.
            fillers.forEach { f ->
                drawCircle(
                    color = fillerColor,
                    radius = 2.2.dp.toPx(),
                    center = Offset(f.x * w, f.y * h)
                )
            }
            nodes.forEachIndexed { i, (id, n) ->
                val p = pts[i]
                val accent = accents[id] ?: Color(0xFF7FAFD8)
                val count = laneCounts[id] ?: 0
                val recent = (laneRecent[id] ?: 0L) >= recentCutoff
                // v208 — stars are sized by KNOWLEDGE score now (explores +
                // saves + words written in the lane), which runs far higher
                // than raw saved counts. A sqrt scale keeps small scores
                // distinguishable and saturates gently past ~60 instead of
                // pinning everything at max.
                // v208f — the dots were too big: the sqrt×7 ramp hit 60dp
                // radius (~120dp across) and every explored star ballooned.
                // Now a gentler ramp capped at 12dp radius (24dp across):
                // score 0 → 4.5dp, 1 → 6.9, 4 → 9.3, 9 → 11.7, 10+ → 12
                // (user: "make the costeellation dots smaller they are too
                // big give it a size limit").
                val r = (4.5f + kotlin.math.sqrt(count.coerceAtLeast(0).toFloat()) * 2.4f)
                    .coerceAtMost(12f).dp.toPx()
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
 * v202 — the human-brain side-profile silhouette in normalized coordinates
 * (x: front → back, y: top → bottom). The classic anatomy outline: the
 * frontal pole, the smooth cerebrum dome (highest around the middle), the
 * occipital pole and the cerebellum bump at the back-bottom. Used both for
 * the faint outline stroke and as the rejection-sampling region for every
 * dot.
 */
private val BRAIN_SILHOUETTE = listOf(
    Offset(0.16f, 0.42f),   // frontal pole
    Offset(0.19f, 0.22f),   // front-top rise
    Offset(0.30f, 0.09f),
    Offset(0.46f, 0.04f),   // highest dome
    Offset(0.63f, 0.05f),
    Offset(0.79f, 0.10f),
    Offset(0.90f, 0.19f),
    Offset(0.96f, 0.33f),   // occipital pole
    Offset(0.99f, 0.50f),   // cerebellum bulge
    Offset(0.90f, 0.61f),
    Offset(0.80f, 0.65f),
    Offset(0.68f, 0.71f),
    Offset(0.54f, 0.75f),   // bottom
    Offset(0.40f, 0.74f),
    Offset(0.28f, 0.68f),
    Offset(0.20f, 0.56f)
)

/** Ray-casting point-in-polygon test against [BRAIN_SILHOUETTE]. */
private fun pointInBrain(p: Offset): Boolean {
    var inside = false
    var j = BRAIN_SILHOUETTE.size - 1
    for (i in BRAIN_SILHOUETTE.indices) {
        val a = BRAIN_SILHOUETTE[i]
        val b = BRAIN_SILHOUETTE[j]
        if ((a.y > p.y) != (b.y > p.y) &&
            p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

/** Uniform random point inside the brain silhouette (rejection sampling
 *  against the bounding box — deterministic for a given seed). */
private fun randomInBrain(rnd: Random): Offset {
    val minX = BRAIN_SILHOUETTE.minOf { it.x }
    val maxX = BRAIN_SILHOUETTE.maxOf { it.x }
    val minY = BRAIN_SILHOUETTE.minOf { it.y }
    val maxY = BRAIN_SILHOUETTE.maxOf { it.y }
    repeat(200) {
        val p = Offset(
            minX + rnd.nextFloat() * (maxX - minX),
            minY + rnd.nextFloat() * (maxY - minY)
        )
        if (pointInBrain(p)) return p
    }
    return Offset(0.5f, 0.42f) // fallback (never expected)
}

/**
 * v202 — deterministic decorative filler dots that complete the brain: a
 * handful of small neutral dots scattered RANDOMLY inside the silhouette
 * (no left/right rings — user: "the dots should be random"), so the brain
 * reads as a filled neural mass even when few lanes are explored. Fixed
 * seed → the decoration never moves between recompositions.
 */
private fun brainFillerDots(): List<Offset> {
    val rnd = Random(0x5EEDC0DE) // fixed — decoration never re-rolls
    return List(16) { randomInBrain(rnd) }
}

/** Draws the smooth closed brain outline (quadratic curves through the
 *  silhouette's midpoints) so the profile reads as a brain behind the web. */
private fun DrawScope.drawBrainOutline(w: Float, h: Float, color: Color, stroke: Float) {
    val pts = BRAIN_SILHOUETTE.map { Offset(it.x * w, it.y * h) }
    val mid = { a: Offset, b: Offset -> Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f) }
    val path = Path().apply {
        moveTo(mid(pts[0], pts[1]).x, mid(pts[0], pts[1]).y)
        for (i in 1..pts.size) {
            val prev = pts[i - 1]
            val cur = pts[i % pts.size]
            val next = pts[(i + 1) % pts.size]
            quadraticBezierTo(prev.x, prev.y, mid(cur, next).x, mid(cur, next).y)
        }
        close()
    }
    drawPath(path, color, style = Stroke(stroke))
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
