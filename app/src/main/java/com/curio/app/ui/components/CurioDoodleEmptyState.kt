package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * Curio's doodle-art empty state — the JSX "Nothing here yet" scene
 * (stacked books + leaf sprig + note card + twinkles) drawn in the app's
 * flask doodle language (white outlines + pastel fills), with a headline,
 * muted subtext and an optional large primary CTA pill. Used app-wide for
 * empty pages: Cabinet shelves (classic + v2), Everything, Recents,
 * Recycle bin, Topic history and the Book browser.
 */
@Composable
fun CurioDoodleEmptyState(
    headline: String,
    subtext: String,
    modifier: Modifier = Modifier,
    ctaLabel: String? = null,
    ctaGlyph: String? = null,
    onCtaClick: () -> Unit = {}
) {
    val dark = isCurioDarkTheme()
    val paper = if (dark) Color(0xFFE8DCC8) else Color(0xFFF9F2E6)
    val book = if (dark) Color(0xFFB98D79) else Color(0xFFD9A887)
    val leaf = if (dark) Color(0xFF9DB58F) else Color(0xFF8FA07E)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)
    ) {
        Box(modifier = Modifier.width(230.dp).height(152.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val stroke = 1.8.dp.toPx()
                // Ground shadow.
                drawOval(
                    if (dark) Color.White.copy(alpha = 0.06f) else Color(0xFF553E42).copy(alpha = 0.10f),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.82f),
                    size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.10f)
                )
                // Leaf sprig — a stem with four leaves, left side.
                drawLine(leaf.copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.78f), androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.30f), strokeWidth = 1.6f)
                val lw = w * 0.075f; val lh = w * 0.045f
                listOf(
                    0.72f to -24f,
                    0.56f to 20f,
                    0.40f to -26f,
                    0.26f to 22f
                ).forEachIndexed { i, (t, rot) ->
                    val cy = h * (0.30f + 0.46f * (1f - t))
                    val cx = w * 0.18f + (if (i % 2 == 0) -1f else 1f) * w * 0.07f
                    rotate(rot, androidx.compose.ui.geometry.Offset(cx, cy)) {
                        val tl = androidx.compose.ui.geometry.Offset(cx - lw / 2f, cy - lh / 2f)
                        drawOval(leaf.copy(alpha = 0.85f), topLeft = tl, size = androidx.compose.ui.geometry.Size(lw, lh))
                        drawOval(Color.White.copy(alpha = 0.8f), topLeft = tl, size = androidx.compose.ui.geometry.Size(lw, lh), style = Stroke(width = stroke * 0.5f))
                    }
                }
                // Book stack — bottom cream book, top terracotta book.
                val bw = w * 0.52f; val bh = h * 0.15f
                rotate(-2f, androidx.compose.ui.geometry.Offset(w * 0.52f, h * 0.74f)) {
                    drawRoundRect(paper.copy(alpha = 0.92f), androidx.compose.ui.geometry.Offset(w * 0.26f, h * 0.70f), androidx.compose.ui.geometry.Size(bw, bh), androidx.compose.ui.geometry.CornerRadius(bh * 0.3f))
                    drawRoundRect(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(w * 0.26f, h * 0.70f), androidx.compose.ui.geometry.Size(bw, bh), androidx.compose.ui.geometry.CornerRadius(bh * 0.3f), style = Stroke(width = stroke * 0.7f))
                }
                rotate(3f, androidx.compose.ui.geometry.Offset(w * 0.52f, h * 0.60f)) {
                    drawRoundRect(book.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.56f), androidx.compose.ui.geometry.Size(bw * 0.92f, bh), androidx.compose.ui.geometry.CornerRadius(bh * 0.3f))
                    drawRoundRect(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.56f), androidx.compose.ui.geometry.Size(bw * 0.92f, bh), androidx.compose.ui.geometry.CornerRadius(bh * 0.3f), style = Stroke(width = stroke * 0.7f))
                    // Title ticks.
                    drawLine(Color.White.copy(alpha = 0.85f), androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.615f), androidx.compose.ui.geometry.Offset(w * 0.56f, h * 0.615f), strokeWidth = 1.1f)
                    drawLine(Color.White.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.645f), androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.645f), strokeWidth = 1.1f)
                }
                // Note card — a small white index card with writing lines.
                val cw = w * 0.30f; val ch = h * 0.34f
                rotate(4f, androidx.compose.ui.geometry.Offset(w * 0.80f, h * 0.52f)) {
                    drawRoundRect(Color(0xFFFFFBF2).copy(alpha = 0.95f), androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.36f), androidx.compose.ui.geometry.Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(cw * 0.06f))
                    drawRoundRect(Color.White.copy(alpha = 0.95f), androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.36f), androidx.compose.ui.geometry.Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(cw * 0.06f), style = Stroke(width = stroke * 0.6f))
                    for (i in 0..2) {
                        val ly = h * 0.42f + i * h * 0.085f
                        drawLine(Color(0xFF9A715D).copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(w * 0.72f, ly), androidx.compose.ui.geometry.Offset(w * 0.72f + cw * 0.66f, ly), strokeWidth = 1.0f)
                    }
                }
                // Twinkles.
                drawPath(fourStar(w * 0.84f, h * 0.18f, w * 0.030f), Color.White.copy(alpha = 0.9f))
                drawPath(fourStar(w * 0.13f, h * 0.22f, w * 0.022f), Color.White.copy(alpha = 0.75f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtext,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )
        if (ctaLabel != null) {
            Spacer(Modifier.height(18.dp))
            // The large primary pill (the labeled "+ Add" style, app-wide).
            Surface(
                onClick = onCtaClick,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.padding(horizontal = 26.dp)
                ) {
                    if (ctaGlyph != null) {
                        CurioIcon(
                            name = ctaGlyph,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            size = 20.dp
                        )
                    }
                    Text(
                        text = ctaLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/** A 4-point twinkle star centred at (cx, cy) — the ✦ mark drawn as a
 *  path so it scales with the scene. */
private fun fourStar(cx: Float, cy: Float, r: Float): Path = Path().apply {
    for (i in 0 until 8) {
        val ang = -Math.PI / 2.0 + i * Math.PI / 4.0
        val rad = if (i % 2 == 0) r else r * 0.30f
        val x = cx + (rad * kotlin.math.cos(ang)).toFloat()
        val y = cy + (rad * kotlin.math.sin(ang)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}