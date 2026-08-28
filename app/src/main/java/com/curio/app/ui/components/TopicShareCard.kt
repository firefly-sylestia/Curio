package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryFamily
import com.curio.app.ui.theme.ChangaOneFontFamily
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.GeomFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import com.curio.app.ui.theme.PatrickHandFontFamily
import kotlin.math.sin

// ─── Style enum ────────────────────────────────────────────────────────
/** v310 — Share card visual style. */
enum class ShareCardStyle(val label: String, val glyph: String) {
    PAPER("Paper", "description"),
    VINYL("Vinyl", "album"),
    COLLAGE("Collage", "collections_bookmark")
}

// ─── Aspect + Content ──────────────────────────────────────────────────
enum class ShareCardAspect(val label: String, val widthDp: Int, val heightDp: Int) {
    PORTRAIT("9:16", 405, 720),
    CLASSIC("3:4", 450, 600)
}

data class ShareCardContent(
    val id: String,
    val label: String,
    val text: String,
    val rating: Int? = null
)

// ─── Shared constants ──────────────────────────────────────────────────
const val QUICK_FACT_ID = "quick_fact"
const val CUSTOM_FACT_ID = "custom_fact"

private val ParchmentLight = Color(0xFFF5E6C8)
private val ParchmentMid = Color(0xFFE8D5A8)
private val ParchmentDark = Color(0xFFD4BC82)
private val ParchmentEdge = Color(0xFFC4A86A)
private val SepiaInk = Color(0xFF3B2510)
private val SepiaLight = Color(0xFF6B4E2F)
private val SepiaFaint = Color(0xFF8B7355)

private fun quoteFontSize(length: Int): TextUnit = when {
    length > 900 -> 15.sp
    length > 650 -> 17.sp
    length > 420 -> 19.sp
    length > 260 -> 21.sp
    else -> 24.sp
}

// ═══════════════════════════════════════════════════════════════════════
// MAIN DISPATCHER
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun TopicShareCard(
    topicName: String,
    categoryName: String,
    categoryGlyph: String,
    accent: Color,
    factText: String,
    sharerName: String,
    aspect: ShareCardAspect,
    modifier: Modifier = Modifier,
    style: ShareCardStyle = ShareCardStyle.PAPER,
    ratingStars: Int? = null,
    categoryFamily: CategoryFamily = CategoryFamily.WILDCARD,
    quoteText: String? = null,
    quoteAuthor: String? = null
) {
    val display = topicName.substringBeforeLast(" (")
    when (style) {
        ShareCardStyle.PAPER -> PaperShareCard(
            display, categoryName, categoryGlyph, accent, factText,
            sharerName, aspect, modifier, ratingStars, categoryFamily,
            quoteText, quoteAuthor
        )
        ShareCardStyle.VINYL -> VinylShareCard(
            display, categoryName, categoryGlyph, accent, factText,
            sharerName, aspect, modifier, ratingStars, categoryFamily,
            quoteText, quoteAuthor
        )
        ShareCardStyle.COLLAGE -> CollageShareCard(
            display, topicName, categoryName, categoryGlyph, accent, factText,
            sharerName, aspect, modifier, ratingStars, categoryFamily,
            quoteText, quoteAuthor
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STYLE 0 — PAPER (main / default)
// Aged parchment + torn bottom edge + frost pane + glyph watermark
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun PaperShareCard(
    display: String, categoryName: String, categoryGlyph: String,
    accent: Color, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier,
    ratingStars: Int?, categoryFamily: CategoryFamily,
    quoteText: String?, quoteAuthor: String?
) {
    val ink = SepiaInk
    val quoteSize = quoteText?.let { quoteFontSize(it.length) } ?: 0.sp

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(
                    listOf(ParchmentLight, ParchmentMid, ParchmentDark),
                    startY = 0f, endY = Float.MAX_VALUE
                ),
                RoundedCornerShape(4.dp)
            )
    ) {
        // Parchment texture noise
        Canvas(Modifier.fillMaxSize()) { drawParchmentNoise() }

        // Torn bottom edge
        Canvas(Modifier.fillMaxSize().align(Alignment.BottomCenter)) {
            drawTornBottomEdge(SepiaInk.copy(alpha = 0.08f))
        }

        // Watermark
        MultiGlyphWatermark(
            family = categoryFamily,
            fallbackGlyph = categoryGlyph,
            tint = SepiaInk.copy(alpha = 0.04f),
            seed = display.hashCode()
        )

        // Content
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: category chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SepiaInk.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurioIcon(name = categoryGlyph, contentDescription = null, tint = ink, size = 14.dp)
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ink
                        )
                    }
                }
                CurioIcon(
                    name = CurioIcons.Lightbulb, contentDescription = null,
                    tint = ink.copy(alpha = 0.35f), size = 20.dp
                )
            }

            // Middle content
            if (quoteText != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CurioIcon(
                        name = CurioIcons.FormatQuote, contentDescription = null,
                        tint = ink.copy(alpha = 0.25f), size = 36.dp
                    )
                    Text(
                        text = quoteText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LoraFontFamily, fontWeight = FontWeight.Normal,
                            fontSize = quoteSize, lineHeight = (quoteSize.value * 1.28f).sp
                        ),
                        color = ink, maxLines = Int.MAX_VALUE, overflow = TextOverflow.Clip
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = display,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = ChangaOneFontFamily, fontWeight = FontWeight.Normal,
                            lineHeight = 40.sp
                        ),
                        color = ink, maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                    if (ratingStars != null && ratingStars > 0) {
                        StarRatingRow(ratingStars, ink)
                    }
                    // Frost pane
                    FrostPane(ink) {
                        Text(
                            text = factText,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = ink,
                            maxLines = if (aspect == ShareCardAspect.PORTRAIT) 10 else 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Footer
            CardFooter(ink, quoteText, quoteAuthor, sharerName)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STYLE 1 — VINYL
// Parchment + Canvas-drawn vinyl record + text below
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun VinylShareCard(
    display: String, categoryName: String, categoryGlyph: String,
    accent: Color, factText: String, sharerName: String,
    aspect: ShareCardAspect, modifier: Modifier,
    ratingStars: Int?, categoryFamily: CategoryFamily,
    quoteText: String?, quoteAuthor: String?
) {
    val ink = SepiaInk
    val quoteSize = quoteText?.let { quoteFontSize(it.length) } ?: 0.sp
    val isPortrait = aspect == ShareCardAspect.PORTRAIT

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(listOf(ParchmentLight, ParchmentMid, ParchmentDark)),
                RoundedCornerShape(4.dp)
            )
    ) {
        Canvas(Modifier.fillMaxSize()) { drawParchmentNoise() }

        MultiGlyphWatermark(
            family = categoryFamily, fallbackGlyph = categoryGlyph,
            tint = SepiaInk.copy(alpha = 0.03f), seed = display.hashCode()
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: category chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accent.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurioIcon(name = categoryGlyph, contentDescription = null, tint = accent, size = 14.dp)
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = accent
                        )
                    }
                }
            }

            // Vinyl record — takes up ~40% of remaining space
            val recordWeight = if (isPortrait) 0.50f else 0.40f
            Box(
                modifier = Modifier.fillMaxWidth().weight(recordWeight),
                contentAlignment = Alignment.Center
            ) {
                VinylRecordCanvas(
                    modifier = Modifier.fillMaxSize(),
                    labelColor = accent
                )
            }

            // Text content below record
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (quoteText != null) {
                    Text(
                        text = quoteText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LoraFontFamily, fontWeight = FontWeight.Normal,
                            fontSize = quoteSize, lineHeight = (quoteSize.value * 1.28f).sp
                        ),
                        color = ink, maxLines = 5, overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = display,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = ChangaOneFontFamily, fontWeight = FontWeight.Normal,
                            lineHeight = 32.sp
                        ),
                        color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = factText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = LoraFontFamily, lineHeight = 18.sp
                        ),
                        color = SepiaLight,
                        maxLines = if (isPortrait) 6 else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (ratingStars != null && ratingStars > 0) {
                    StarRatingRow(ratingStars, ink)
                }
            }

            // Footer
            CardFooter(ink, quoteText, quoteAuthor, sharerName)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STYLE 2 — COLLAGE
// Torn paper layers + polaroid frame + tape + handwritten topic name
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun CollageShareCard(
    display: String, topicName: String, categoryName: String,
    categoryGlyph: String, accent: Color, factText: String,
    sharerName: String, aspect: ShareCardAspect, modifier: Modifier,
    ratingStars: Int?, categoryFamily: CategoryFamily,
    quoteText: String?, quoteAuthor: String?
) {
    val ink = Color(0xFF2C1810)
    val isPortrait = aspect == ShareCardAspect.PORTRAIT

    // Paper layer colors
    val layerTop = Color(0xFFF2E8D5)    // warm cream
    val layerMid = Color(0xFFE0D2B8)    // kraft tan
    val layerBot = Color(0xFFB8C4C0)    // slate teal

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(layerTop, RoundedCornerShape(4.dp))
    ) {
        // Draw torn paper layers
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Mid layer (kraft tan) — starts at ~55%
            val midStart = h * 0.52f
            drawRect(color = layerMid, topLeft = Offset(0f, midStart), size = Size(w, h - midStart))

            // Bottom layer (slate teal) — starts at ~72%
            val botStart = h * 0.70f
            drawRect(color = layerBot, topLeft = Offset(0f, botStart), size = Size(w, h - botStart))

            // Torn edges between layers
            drawTornEdgeLine(y = midStart, w = w, color = layerTop, aboveColor = layerMid)
            drawTornEdgeLine(y = botStart, w = w, color = layerMid, aboveColor = layerBot)
        }

        // Polaroid frame with handwritten topic name
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cardW = maxWidth.value
            val cardH = maxHeight.value

            // Polaroid: top-right, ~48% width
            val polaroidW = cardW * 0.46f
            val polaroidH = polaroidW * 1.2f
            val polaroidX = cardW * 0.50f
            val polaroidY = cardH * 0.06f

            // Tape strip (slightly above polaroid, angled)
            Canvas(
                modifier = Modifier
                    .offset(x = (polaroidX + polaroidW * 0.15f).dp, y = (polaroidY - 4f).dp)
                    .size(width = (polaroidW * 0.55f).dp, height = 18.dp)
                    .drawBehind {
                        // Tape: translucent warm beige
                        drawRoundRect(
                            color = Color(0xFFD9BE8A).copy(alpha = 0.65f),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height)
                        )
                        // Subtle wrinkles
                        drawLine(
                            Color.White.copy(alpha = 0.15f),
                            Offset(size.width * 0.1f, size.height * 0.35f),
                            Offset(size.width * 0.9f, size.height * 0.3f),
                            strokeWidth = 0.8.dp.toPx()
                        )
                        drawLine(
                            Color(0xFF9B7847).copy(alpha = 0.10f),
                            Offset(size.width * 0.2f, size.height * 0.65f),
                            Offset(size.width * 0.85f, size.height * 0.7f),
                            strokeWidth = 0.6.dp.toPx()
                        )
                    }
            )

            // Polaroid frame
            Canvas(
                modifier = Modifier
                    .offset(x = polaroidX.dp, y = polaroidY.dp)
                    .size(width = polaroidW.dp, height = polaroidH.dp)
            ) {
                val frameW = size.width
                val frameH = size.height
                val border = frameW * 0.075f
                val innerW = frameW - border * 2
                val innerH = frameH * 0.72f

                // White frame
                drawRoundRect(
                    color = Color.White,
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    topLeft = Offset.Zero,
                    size = Size(frameW, frameH)
                )
                // Photo area (transparent — shows paper behind)
                drawRoundRect(
                    color = Color(0xFFEDE8DF),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    topLeft = Offset(border, border),
                    size = Size(innerW, innerH)
                )
                // Subtle inner shadow on photo area
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.06f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    topLeft = Offset(border, border),
                    size = Size(innerW, innerH),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Handwritten topic name inside polaroid
            val textMeasurer = rememberTextMeasurer()
            val handwrittenStyle = TextStyle(
                fontFamily = PatrickHandFontFamily,
                fontSize = (polaroidW * 0.062f).sp.coerceIn(11.sp, 16.sp),
                color = ink,
                lineHeight = (polaroidW * 0.075f).sp
            )
            val textLayout = textMeasurer.measure(
                text = display,
                style = handwrittenStyle,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Canvas(
                modifier = Modifier
                    .offset(
                        x = (polaroidX + polaroidW * 0.09f).dp,
                        y = (polaroidY + polaroidH * 0.76f).dp
                    )
                    .size(
                        width = (polaroidW * 0.82f).dp,
                        height = (polaroidH * 0.20f).dp
                    )
            ) {
                drawText(textLayout, topLeft = Offset.Zero)
            }
        }

        // Content on left side
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: category chip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF2C1810).copy(alpha = 0.75f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurioIcon(name = categoryGlyph, contentDescription = null, tint = Color.White, size = 14.dp)
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom content
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Glyph stamp/seal
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .drawBehind {
                            drawCircle(
                                color = accent.copy(alpha = 0.15f),
                                radius = size.minDimension / 2f
                            )
                            drawCircle(
                                color = accent.copy(alpha = 0.30f),
                                radius = size.minDimension / 2f,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(name = categoryGlyph, contentDescription = null, tint = accent, size = 20.dp)
                }

                if (quoteText != null) {
                    val quoteSize = quoteFontSize(quoteText.length)
                    Text(
                        text = quoteText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PatrickHandFontFamily,
                            fontSize = quoteSize, lineHeight = (quoteSize.value * 1.3f).sp
                        ),
                        color = ink, maxLines = 6, overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = factText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = GeomFontFamily, lineHeight = 17.sp
                        ),
                        color = Color(0xFF2C1810).copy(alpha = 0.80f),
                        maxLines = 5, overflow = TextOverflow.Ellipsis
                    )
                }
                if (ratingStars != null && ratingStars > 0) {
                    StarRatingRow(ratingStars, Color(0xFF2C1810))
                }

                // Footer
                Text(
                    text = if (sharerName.isNotBlank()) "$sharerName · via Curio" else "via Curio",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = GeomFontFamily, fontWeight = FontWeight.Medium
                    ),
                    color = SepiaFaint, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// CANVAS DRAWING HELPERS
// ═══════════════════════════════════════════════════════════════════════

/** Parchment noise texture — tiny scattered dots for aged paper feel. */
private fun DrawScope.drawParchmentNoise() {
    val w = size.width
    val h = size.height
    // Deterministic pseudo-random from card dimensions
    val seed = (w * 1000 + h).toInt()
    for (i in 0 until 120) {
        val x = ((seed * (i + 1) * 7919) % 10000) / 10000f * w
        val y = ((seed * (i + 1) * 6271) % 10000) / 10000f * h
        val alpha = 0.03f + ((seed * (i + 1) * 3571) % 100) / 100f * 0.04f
        val dotSize = 1f + ((seed * (i + 1) * 4201) % 100) / 100f * 2f
        drawCircle(
            color = Color(0xFF8B7355).copy(alpha = alpha),
            radius = dotSize,
            center = Offset(x, y)
        )
    }
}

/** Torn bottom edge — organic jagged path at the bottom of the card. */
private fun DrawScope.drawTornBottomEdge(tintColor: Color) {
    val w = size.width
    val h = size.height
    val tearY = h * 0.88f
    val path = Path().apply {
        moveTo(0f, h)
        var x = 0f
        while (x <= w) {
            val jitter = sin(x * 0.03f + 1.7f) * 12f + sin(x * 0.08f + 3.2f) * 5f
            lineTo(x, tearY + jitter)
            x += w / 60f
        }
        lineTo(w, h)
        close()
    }
    drawPath(path, color = tintColor)
}

/** Torn edge line between two paper layers — jagged horizontal line with a thin shadow. */
private fun DrawScope.drawTornEdgeLine(y: Float, w: Float, color: Color, aboveColor: Color) {
    // Shadow under the tear
    val shadowPath = Path().apply {
        moveTo(0f, y + 3f)
        var x = 0f
        while (x <= w) {
            val jitter = sin(x * 0.04f + 2.1f) * 4f + sin(x * 0.11f + 0.8f) * 2f
            lineTo(x, y + jitter + 3f)
            x += w / 40f
        }
        lineTo(w, y + 12f)
        lineTo(0f, y + 12f)
        close()
    }
    drawPath(shadowPath, color = Color.Black.copy(alpha = 0.08f))

    // The tear itself
    val tearPath = Path().apply {
        moveTo(0f, y)
        var x = 0f
        while (x <= w) {
            val jitter = sin(x * 0.04f + 2.1f) * 4f + sin(x * 0.11f + 0.8f) * 2f
            lineTo(x, y + jitter)
            x += w / 40f
        }
        lineTo(w, y + 20f)
        lineTo(0f, y + 20f)
        close()
    }
    drawPath(tearPath, color = color)

    // White rough edge highlight
    val edgePath = Path().apply {
        moveTo(0f, y - 1f)
        var x = 0f
        while (x <= w) {
            val jitter = sin(x * 0.04f + 2.1f) * 4f + sin(x * 0.11f + 0.8f) * 2f
            lineTo(x, y + jitter - 1f)
            x += w / 40f
        }
    }
    drawPath(edgePath, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 1.5f))
}

/** Vinyl record — black disk with grooves, highlight, colored label, center hole. */
private fun DrawScope.drawVinylRecord(labelColor: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = minOf(cx, cy) * 0.88f

    // Drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.20f),
        radius = radius + 6f,
        center = Offset(cx + 2f, cy + 4f)
    )

    // Main black disc
    drawCircle(color = Color(0xFF151515), radius = radius, center = Offset(cx, cy))

    // Grooves
    val grooveAlpha = 0.07f
    for (r in 8 downTo 1) {
        drawCircle(
            color = Color.White.copy(alpha = grooveAlpha),
            radius = radius * (0.40f + r * 0.06f),
            center = Offset(cx, cy),
            style = Stroke(width = 0.8f)
        )
    }

    // Specular highlight arc
    val highlightPath = Path().apply {
        addArc(
            Rect(cx - radius * 0.85f, cy - radius * 0.85f, cx + radius * 0.85f, cy + radius * 0.85f),
            startAngleDegrees = -50f,
            sweepAngleDegrees = 70f
        )
    }
    drawPath(highlightPath, color = Color.White.copy(alpha = 0.06f), style = Stroke(width = radius * 0.25f))

    // Colored label
    val labelRadius = radius * 0.30f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(labelColor.copy(alpha = 0.95f), labelColor.copy(alpha = 0.70f)),
            center = Offset(cx - labelRadius * 0.2f, cy - labelRadius * 0.2f),
            radius = labelRadius * 1.2f
        ),
        radius = labelRadius,
        center = Offset(cx, cy)
    )

    // Label ring
    drawCircle(
        color = Color.White.copy(alpha = 0.20f),
        radius = labelRadius * 0.78f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5f)
    )

    // Center hole
    drawCircle(color = Color(0xFF171717), radius = radius * 0.04f, center = Offset(cx, cy))
    drawCircle(
        color = Color(0xFFE8E0D2), radius = radius * 0.015f,
        center = Offset(cx, cy)
    )
}

/** Frost pane — simulated translucent content holder. */
@Composable
private fun FrostPane(ink: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val corner = CornerRadius(20.dp.toPx())
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.10f),
                    cornerRadius = corner,
                    topLeft = Offset(0f, 3.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.08f))
                    ),
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.25f),
                    cornerRadius = corner,
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
            .padding(18.dp)
    ) { content() }
}

/** Star rating row. */
@Composable
private fun StarRatingRow(ratingStars: Int, ink: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { i ->
                CurioIcon(
                    name = if (i < ratingStars) CurioIcons.Star else CurioIcons.StarOutline,
                    contentDescription = null,
                    tint = if (i < ratingStars) Color(0xFFFFC94D) else ink.copy(alpha = 0.35f),
                    size = 22.dp
                )
            }
        }
        Text(
            text = when (ratingStars) {
                1 -> "Not for me"; 2 -> "It was okay"; 3 -> "Pretty good"
                4 -> "Really liked it"; 5 -> "Loved it"; else -> ""
            },
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = ink.copy(alpha = 0.70f)
        )
    }
}

/** Card footer — branding line. */
@Composable
private fun CardFooter(
    ink: Color, quoteText: String?, quoteAuthor: String?, sharerName: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        CurioIcon(
            name = CurioIcons.Lightbulb, contentDescription = null,
            tint = ink.copy(alpha = 0.35f), size = 16.dp
        )
        Spacer(Modifier.height(4.dp))
        if (quoteText != null && !quoteAuthor.isNullOrBlank()) {
            Text(
                text = "— $quoteAuthor",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = LoraFontFamily, fontWeight = FontWeight.SemiBold
                ),
                color = ink.copy(alpha = 0.75f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End
            )
            Spacer(Modifier.height(3.dp))
        }
        Text(
            text = if (quoteText != null) {
                if (sharerName.isNotBlank()) "$sharerName ~ Stay Curious" else "Stay Curious"
            } else {
                if (sharerName.isNotBlank()) "$sharerName · via Curio" else "via Curio"
            },
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = GeomFontFamily, fontWeight = FontWeight.SemiBold
            ),
            color = ink.copy(alpha = 0.80f),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// VINYL RECORD CANVAS COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun VinylRecordCanvas(modifier: Modifier, labelColor: Color) {
    Canvas(modifier = modifier) {
        drawVinylRecord(labelColor)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// WATERMARK
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun MultiGlyphWatermark(
    family: CategoryFamily, fallbackGlyph: String, tint: Color, seed: Int
) {
    val symbols = CurioIcons.heroWatermarkSymbols(family)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value
        val positions = listOf(
            0.16f to 0.18f, 0.84f to 0.18f,
            0.18f to 0.50f, 0.82f to 0.50f,
            0.16f to 0.82f, 0.84f to 0.82f
        )
        positions.forEachIndexed { i, (x, y) ->
            CurioIcon(
                name = symbols[i % symbols.size], contentDescription = null,
                tint = tint, size = 28.dp,
                modifier = Modifier.offset(x = (w * x - 14).dp, y = (h * y - 14).dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SHARE SHEET
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun TopicShareSheet(
    topicName: String,
    categoryName: String,
    categoryGlyph: String,
    accent: Color,
    quickFact: String,
    authority: String,
    context: android.content.Context,
    savedSources: List<ShareCardContent> = emptyList(),
    onDismiss: () -> Unit,
    categoryFamily: CategoryFamily = CategoryFamily.WILDCARD,
    topicByline: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var aspect by rememberSaveable { mutableStateOf(ShareCardAspect.PORTRAIT) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var customText by rememberSaveable { mutableStateOf("") }
    var styleIndex by rememberSaveable { mutableIntStateOf(0) }
    val sharer = AppPreferences.getDisplayName(context).ifBlank { "" }

    val isQuotesCategory = categoryName == "Quotes"
    val quick = ShareCardContent(QUICK_FACT_ID, "Quick fact", quickFact)
    val quote = ShareCardContent("quote", "Quote", quickFact)
    val custom = ShareCardContent(CUSTOM_FACT_ID, "Custom fact", "")
    val availableSources = if (isQuotesCategory) listOf(quote) + listOf(custom)
    else listOf(quick) + savedSources
    val defaultId = if (isQuotesCategory) quote.id
    else savedSources.firstOrNull { it.id == "quote" }?.id ?: quick.id
    val activeId = selectedId ?: defaultId
    val activeSource = when (activeId) {
        CUSTOM_FACT_ID -> custom.copy(text = customText.ifBlank { "Add your own fact about this discovery…" })
        else -> (availableSources.firstOrNull { it.id == activeId } ?: quick)
    }
    val currentStyle = ShareCardStyle.entries[styleIndex]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Share this topic",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            ShareHubBody(
                topicName = topicName,
                categoryName = categoryName,
                categoryGlyph = categoryGlyph,
                accent = accent,
                sharerName = sharer,
                authority = authority,
                context = context,
                aspect = aspect,
                onAspectChange = { aspect = it },
                style = currentStyle,
                onStyleChange = { styleIndex = it },
                sources = availableSources + listOf(custom),
                activeSource = activeSource,
                onSelectSource = { selectedId = it },
                customEditing = activeId == CUSTOM_FACT_ID,
                customText = customText,
                onCustomTextChange = { customText = it },
                onShared = onDismiss,
                categoryFamily = categoryFamily,
                topicByline = topicByline
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SHARE HUB BODY
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun ShareHubBody(
    topicName: String,
    categoryName: String,
    categoryGlyph: String,
    accent: Color,
    sharerName: String,
    authority: String,
    context: android.content.Context,
    aspect: ShareCardAspect,
    onAspectChange: (ShareCardAspect) -> Unit,
    sources: List<ShareCardContent>,
    activeSource: ShareCardContent,
    onSelectSource: (String) -> Unit,
    customEditing: Boolean,
    customText: String,
    onCustomTextChange: (String) -> Unit,
    onShared: () -> Unit,
    categoryFamily: CategoryFamily = CategoryFamily.WILDCARD,
    topicByline: String = "",
    style: ShareCardStyle = ShareCardStyle.PAPER,
    onStyleChange: (Int) -> Unit = {}
) {
    val previewWidth = 280.dp
    val isQuote = activeSource.id == "quote"

    // Card preview
    Box(
        modifier = Modifier
            .width(previewWidth)
            .aspectRatio(aspect.widthDp.toFloat() / aspect.heightDp.toFloat())
            .shadow(2.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
    ) {
        TopicShareCard(
            topicName = topicName,
            categoryName = categoryName,
            categoryGlyph = categoryGlyph,
            accent = accent,
            factText = activeSource.text,
            sharerName = sharerName,
            aspect = aspect,
            style = style,
            ratingStars = activeSource.rating,
            categoryFamily = categoryFamily,
            quoteText = if (isQuote) activeSource.text else null,
            quoteAuthor = if (isQuote) topicByline.ifBlank { null } else null
        )
    }

    // Style picker — swipeable row
    StylePickerRow(
        selectedIndex = styleIndex,
        onSelect = onStyleChange
    )

    // Aspect picker
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ShareOptionPill(
            label = ShareCardAspect.PORTRAIT.label,
            icon = CurioIcons.Image,
            selected = aspect == ShareCardAspect.PORTRAIT
        ) { onAspectChange(ShareCardAspect.PORTRAIT) }
        ShareOptionPill(
            label = ShareCardAspect.CLASSIC.label,
            icon = CurioIcons.Image,
            selected = aspect == ShareCardAspect.CLASSIC
        ) { onAspectChange(ShareCardAspect.CLASSIC) }
    }

    // Content source picker
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        sources.filter { !isQuote || it.id != QUICK_FACT_ID }.forEach { option ->
            ShareOptionPill(
                label = option.label + (option.rating?.takeIf { r -> r > 0 }
                    ?.let { r -> " · " + "★".repeat(r) } ?: ""),
                icon = CurioIcons.FormatText,
                selected = option.id == activeSource.id
            ) { onSelectSource(option.id) }
        }
    }

    // Editable text for Custom fact
    if (customEditing) {
        OutlinedTextField(
            value = customText,
            onValueChange = onCustomTextChange,
            placeholder = {
                Text("Your custom fact", style = MaterialTheme.typography.bodyMedium)
            },
            minLines = 2, maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
    }

    val exportCardHeight = previewWidth * aspect.heightDp.toFloat() / aspect.widthDp.toFloat()

    // Share button
    Button(
        onClick = {
            shareComposableCard(
                context = context,
                cardSize = androidx.compose.ui.unit.DpSize(previewWidth, exportCardHeight),
                exportDensity = 4f,
                authority = authority,
                card = {
                    TopicShareCard(
                        topicName = topicName,
                        categoryName = categoryName,
                        categoryGlyph = categoryGlyph,
                        accent = accent,
                        factText = activeSource.text,
                        sharerName = sharerName,
                        aspect = aspect,
                        style = style,
                        ratingStars = activeSource.rating,
                        categoryFamily = categoryFamily,
                        quoteText = if (isQuote) activeSource.text else null,
                        quoteAuthor = if (isQuote) topicByline.ifBlank { null } else null
                    )
                }
            )
            onShared()
        },
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text(
            text = "Share image card",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// STYLE PICKER — swipeable horizontal row
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun StylePickerRow(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val styles = ShareCardStyle.entries
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.pointerInput(selectedIndex) {
            detectHorizontalDragGestures { _, dragAmount ->
                if (dragAmount > 15f && selectedIndex > 0) {
                    onSelect(selectedIndex - 1)
                } else if (dragAmount < -15f && selectedIndex < styles.lastIndex) {
                    onSelect(selectedIndex + 1)
                }
            }
        }
    ) {
        styles.forEachIndexed { index, style ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(50),
                color = if (selected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.height(40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    CurioIcon(
                        name = style.glyph, contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 16.dp
                    )
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// OPTION PILL
// ═══════════════════════════════════════════════════════════════════════
@Composable
private fun ShareOptionPill(
    label: String, icon: String, selected: Boolean, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            CurioIcon(
                name = icon, contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 16.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
