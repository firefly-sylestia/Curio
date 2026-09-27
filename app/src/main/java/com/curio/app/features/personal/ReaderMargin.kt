package com.curio.app.features.personal

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.ReaderMarginInk
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion

/**
 * v492 — THE PENCIL MARGIN (agenda §5.5, kept from the second round).
 *
 * A strip over the page's right edge that the member scrawls in with a finger,
 * kept per page of the book. The point of a margin is that the mark lands where
 * the words are, at the moment they land — no sheet, no dialog, no keyboard.
 *
 * ## Why a strip and NOT an edge-drag on the page
 *
 * The idea was offered as *"drag in from the right edge"*, and the honest reason
 * it does not ship that way is that **the reader's right edge is already a tap
 * zone** (`ReaderLook.tapZones` — page-forward, and the motion lock freezes
 * gestures over the page on purpose). A hidden drag competing with a tap zone on
 * the most delicate surface in the app is two gestures fighting for one thumb,
 * and the loser would be something the member already relies on. So the margin
 * opens from the reader's own **⋯ menu** — a visible door, which is also what the
 * research requires of any gesture (a gesture must accelerate a task, never
 * become the only route to it). The scrawl itself is exactly as asked for: a
 * finger, live ink, on the page.
 *
 * ## How the ink is stored
 *
 * Points live in the writing area's own box as FRACTIONS (`0f…1f`) and are
 * multiplied back up only at draw time, so a scrawl survives a rotation, a
 * different strip width, a tablet and a re-open. [ReaderMarginInk] owns the caps
 * and the file. The writing area's measured size is what makes the conversion
 * honest, which is why it is captured (`onSizeChanged`) rather than assumed.
 *
 * ## Why the working copy lives here
 *
 * A stroke is written when the finger LIFTS: not on every move (a JSON write per
 * frame) and not on close (which loses the page if the reader dies first). The
 * whole page is handed over on each write — one writer, one shape.
 */
@Composable
fun ReaderMarginStrip(
    bookId: String,
    page: Int,
    /** The page's own paper, so the margin reads as part of the book. */
    paper: Color,
    /** The page's own ink — the pencil is the same hand as the text. */
    ink: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    // The page's strokes, in fractions. This strip owns the working copy while
    // it is open; ReaderMarginInk is written on every lift.
    var strokes by remember(bookId, page) {
        mutableStateOf(ReaderMarginInk.strokes(context, bookId, page).toMutableList())
    }
    // The stroke under the finger RIGHT NOW, in pixels — drawn, not stored,
    // until the finger lifts.
    var live by remember { mutableStateOf<List<Offset>>(emptyList()) }
    // The writing area's measured size: the only place pixels become fractions.
    var padSize by remember { mutableStateOf(Size.Zero) }

    // The strip slides in from the page's edge. Keyed on nothing: changing the
    // page inside an open margin must not slide it in again.
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, CurioMotion.Springs.Deliberate)
    }

    val stripWidth = 148.dp

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(stripWidth)
            .graphicsLayer {
                translationX = (1f - enter.value) * size.width
                alpha = enter.value.coerceIn(0f, 1f)
            }
    ) {
        Surface(
            color = paper,
            shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── The margin's own head ────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, top = 14.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MARGIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp
                        ),
                        color = ink.copy(alpha = 0.62f),
                        modifier = Modifier.weight(1f)
                    )
                    MarginPill(label = "Clear", ink = ink, enabled = strokes.isNotEmpty()) {
                        strokes = mutableListOf()
                        ReaderMarginInk.clear(context, bookId, page)
                    }
                }
                Text(
                    text = "page ${page + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.42f),
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )

                // ── The paper you write on ───────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                        // A whisper of the page's own ink at the fold, so the
                        // margin reads as the sheet's own edge rather than as a
                        // panel laid on top of it.
                        .background(ink.copy(alpha = 0.03f))
                        .onSizeChanged { padSize = Size(it.width.toFloat(), it.height.toFloat()) }
                        .pointerInput(bookId, page) {
                            detectDragGestures(
                                onDragStart = { start -> live = listOf(start) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    live = live + change.position
                                },
                                onDragEnd = {
                                    val committed = normalise(live, padSize)
                                    live = emptyList()
                                    if (committed != null) {
                                        val next = (strokes + committed)
                                            .takeLast(ReaderMarginInk.MAX_STROKES)
                                            .toMutableList()
                                        strokes = next
                                        ReaderMarginInk.save(context, bookId, page, next)
                                        // One light tick per stroke: the pen
                                        // touching down is the only event here,
                                        // and it should be FELT, not announced.
                                        haptics.performHapticFeedback(
                                            HapticFeedbackType.TextHandleMove
                                        )
                                    }
                                },
                                onDragCancel = { live = emptyList() }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        strokes.forEach { flat -> drawPencilStroke(flat, size, ink) }
                        // The live stroke draws LAST, so the pen is never
                        // behind the ink it has already made.
                        if (live.size >= 2) drawPencilPath(live, ink)
                    }
                    if (strokes.isEmpty() && live.isEmpty()) {
                        // Not a hint about the feature — a mark where the pen
                        // goes, the way a margin shows you its own rule.
                        Text(
                            text = "write here",
                            style = MaterialTheme.typography.labelSmall,
                            color = ink.copy(alpha = 0.28f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        )
                    }
                }

                // ── The way out ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Surface(
                        onClick = onClose,
                        shape = RoundedCornerShape(50),
                        color = ink.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurioIcon(
                                CurioIcons.ChevronRight,
                                "Put the margin away",
                                tint = ink.copy(alpha = 0.75f),
                                size = 16.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Back to the page",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = ink.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A small pill of the page's own ink — used for the margin's one action. */
@Composable
private fun MarginPill(
    label: String,
    ink: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = ink.copy(alpha = if (enabled) 0.10f else 0.05f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ink.copy(alpha = if (enabled) 0.8f else 0.3f),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/**
 * A stroke needs [MIN_STROKE_POINTS] points and must MOVE before any of it is
 * kept.
 *
 * A tap is a dot, and a margin made of accidental dots is a mess — this strip
 * sits over a reader, so a mis-touch has to leave nothing behind. Both tests
 * together are what separates a mark from a stray finger: three points crammed
 * into the same pixel is still a tap.
 */
private const val MIN_STROKE_POINTS = 3

/** Total travel (px) a stroke must cover before it is a stroke at all. */
private const val MIN_STROKE_TRAVEL = 6f

/**
 * Pixels to fractions of [box] — **the one place a margin's points change
 * space**, and null when the drag was not a stroke (see [MIN_STROKE_POINTS]).
 *
 * The values are clamped into the box: a finger that leaves the writing area
 * while the stroke is live reports positions outside it, and an unclamped point
 * would draw back inside the box at a wrong place after a rotation.
 */
private fun normalise(live: List<Offset>, box: Size): FloatArray? {
    if (box.width <= 0f || box.height <= 0f) return null
    if (live.size < MIN_STROKE_POINTS) return null
    var travel = 0f
    for (k in 1 until live.size) {
        travel += (live[k] - live[k - 1]).getDistance()
    }
    if (travel < MIN_STROKE_TRAVEL) return null
    val flat = FloatArray(live.size * 2)
    live.forEachIndexed { i, p ->
        flat[i * 2] = (p.x / box.width).coerceIn(0f, 1f)
        flat[i * 2 + 1] = (p.y / box.height).coerceIn(0f, 1f)
    }
    return flat
}

/** Draws one STORED stroke: fractions multiplied back up by the measured box. */
private fun DrawScope.drawPencilStroke(flat: FloatArray, box: Size, ink: Color) {
    if (flat.size < 4 || box.width <= 0f || box.height <= 0f) return
    val points = ArrayList<Offset>(flat.size / 2)
    var i = 0
    while (i + 1 < flat.size) {
        points.add(Offset(flat[i] * box.width, flat[i + 1] * box.height))
        i += 2
    }
    drawPencilPath(points, ink)
}

/**
 * A pencil, not a pen: a soft wide pass under a crisp narrow one.
 *
 * Two strokes along the same path at different widths and alphas is what makes
 * graphite read as graphite — a single opaque line reads as a UI stroke, which is
 * exactly what a margin must not look like.
 *
 * The path runs QUADRATICS THROUGH MIDPOINTS of consecutive samples: a finger
 * reports points and the line between two of them is a guess, so drawing those
 * literally is what makes hand-drawn ink look like a polyline.
 */
private fun DrawScope.drawPencilPath(points: List<Offset>, ink: Color) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(ink.copy(alpha = 0.7f), radius = 1.6f, center = points[0])
        return
    }
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (k in 1 until points.size) {
            val prev = points[k - 1]
            val at = points[k]
            quadraticBezierTo(prev.x, prev.y, (prev.x + at.x) / 2f, (prev.y + at.y) / 2f)
        }
        lineTo(points.last().x, points.last().y)
    }
    drawPath(
        path = path,
        color = ink.copy(alpha = 0.16f),
        style = Stroke(width = 4.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = path,
        color = ink.copy(alpha = 0.78f),
        style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
