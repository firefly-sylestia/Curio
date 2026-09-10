package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * Curio's doodle-art empty state — the JSX "Nothing here yet" scene
 * (stacked books + note card + twinkles) drawn in the app's flask doodle
 * language (white outlines + pastel fills), with a headline, muted subtext
 * and an optional large primary CTA pill. Used app-wide for empty pages:
 * Cabinet shelves (classic + v2), Everything, Recents, Recycle bin, Topic
 * history and the Book browser.
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
                // Book stack — bottom cream book, top terracotta book
                // (the old leaf sprig read as a lopsided tree — removed;
                // the stack breathes wider with the space).
                val bw = w * 0.64f; val bh = h * 0.16f
                rotate(-2f, androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.74f)) {
                    drawRoundRect(paper.copy(alpha = 0.92f), androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.68f), androidx.compose.ui.geometry.Size(bw, bh), androidx.compose.ui.geometry.CornerRadius(bh * 0.3f))
                    drawRoundRect(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.68f), androidx.compose.ui.geometry.Size(bw, bh), androidx.compose.ui.geometry.CornerRadius(bh * 0.3f), style = Stroke(width = stroke * 0.7f))
                    // Bottom book's spine line — a short cream tick near the
                    // left edge so the stack reads as real books.
                    drawLine(Color.White.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.71f), androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.81f), strokeWidth = 1.2f)
                }
                rotate(3f, androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.58f)) {
                    drawRoundRect(book.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.53f), androidx.compose.ui.geometry.Size(bw * 0.88f, bh), androidx.compose.ui.geometry.CornerRadius(bh * 0.3f))
                    drawRoundRect(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.53f), androidx.compose.ui.geometry.Size(bw * 0.88f, bh), androidx.compose.ui.geometry.CornerRadius(bh * 0.3f), style = Stroke(width = stroke * 0.7f))
                    // Title ticks.
                    drawLine(Color.White.copy(alpha = 0.85f), androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.595f), androidx.compose.ui.geometry.Offset(w * 0.46f, h * 0.595f), strokeWidth = 1.1f)
                    drawLine(Color.White.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.625f), androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.625f), strokeWidth = 1.1f)
                }
                // Note card — a small white index card with writing lines,
                // leaning against the stack on the right.
                val cw = w * 0.26f; val ch = h * 0.38f
                rotate(5f, androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.52f)) {
                    drawRoundRect(Color(0xFFFFFBF2).copy(alpha = 0.95f), androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.32f), androidx.compose.ui.geometry.Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(cw * 0.07f))
                    drawRoundRect(Color.White.copy(alpha = 0.95f), androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.32f), androidx.compose.ui.geometry.Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(cw * 0.07f), style = Stroke(width = stroke * 0.6f))
                    // Corner fold — a small triangle clipped from the card's
                    // top-right, with the fold line drawn in.
                    val fold = Path().apply {
                        moveTo(w * 0.62f + cw - cw * 0.20f, h * 0.32f)
                        lineTo(w * 0.62f + cw, h * 0.32f + cw * 0.20f)
                        lineTo(w * 0.62f + cw, h * 0.32f)
                        close()
                    }
                    drawPath(fold, paper.copy(alpha = 0.9f))
                    drawLine(Color.White.copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(w * 0.62f + cw - cw * 0.20f, h * 0.32f), androidx.compose.ui.geometry.Offset(w * 0.62f + cw, h * 0.32f + cw * 0.20f), strokeWidth = stroke * 0.45f)
                    for (i in 0..2) {
                        val ly = h * 0.42f + i * h * 0.085f
                        drawLine(Color(0xFF9A715D).copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(w * 0.66f, ly), androidx.compose.ui.geometry.Offset(w * 0.66f + cw * 0.62f, ly), strokeWidth = 1.0f)
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