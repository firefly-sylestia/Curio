package com.curio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.ChangaOneFontFamily
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.toHsl
import kotlin.math.sin

/** v292 — share-card aspect options. */
enum class ShareCardAspect(val label: String, val widthDp: Int, val heightDp: Int) {
    PORTRAIT("9:16", 405, 720),
    CLASSIC("3:4", 450, 600)
}

/** v292 — what the card's frosted pane shows. */
enum class ShareFactSource(val label: String) {
    QUICK_FACT("Quick fact"),
    CUSTOM_FACT("Custom fact"),
    REVIEW("Review")
}

/**
 * v292 — THE TOPIC SHARE CARD. A self-contained composable designed for
 * off-screen bitmap capture via [shareComposableCard] (single-frame draw,
 * so everything here is synchronous: no async painters, no backdrop APIs).
 *
 * Look, top to bottom:
 *  - full-bleed deep→light category gradient,
 *  - a seeded WATERMARK PATTERN of the category glyph tiled across the
 *    card (the app's page-watermark language, baked into the bitmap),
 *  - the topic name in the big Changa One display face,
 *  - a FROSTED glass pane (simulated frost — layered white washes + rim +
 *    soft shadow, the fauxGlass recipe) holding the fact text,
 *  - a TORN-PAPER footer strip with "via Curio ✦" branding.
 *
 * Frost is simulated on purpose: the capture draws through a software
 * Canvas where RenderEffect blur is unavailable.
 */
@Composable
fun TopicShareCard(
    topicName: String,
    categoryName: String,
    categoryGlyph: String,
    accent: Color,
    factText: String,
    sharerName: String,
    aspect: ShareCardAspect,
    modifier: Modifier = Modifier
) {
    val base = toHsl(accent)
    val deep = fromHsl(base.h, base.s, (base.l * 0.55f).coerceIn(0f, 0.5f))
    val ink = Color.White
    val display = topicName.substringBeforeLast(" (")

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(deep, accent)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx())
                )
            }
    ) {
        // ── Watermark glyph pattern (seeded tile) ─────────────────────
        WatermarkPattern(glyph = categoryGlyph, tint = ink.copy(alpha = 0.06f), seed = topicName.hashCode())

        // ── Content ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: category chip + sparkle.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = ink.copy(alpha = 0.16f)) {
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
                    name = CurioIcons.AutoAwesome,
                    contentDescription = null,
                    tint = ink.copy(alpha = 0.55f),
                    size = 20.dp
                )
            }

            // Middle: topic name + frosted fact pane.
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = display,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = ChangaOneFontFamily,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 40.sp
                    ),
                    color = ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                // Frosted glass pane holding the chosen fact.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Simulated frost: soft drop shadow + layered
                            // translucent washes + hairline rim.
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.18f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                                topLeft = Offset(0f, 8.dp.toPx())
                            )
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.12f))
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.35f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        .padding(20.dp)
                ) {
                    Text(
                        text = factText,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                        color = ink,
                        maxLines = when (aspect) {
                            ShareCardAspect.PORTRAIT -> 7
                            ShareCardAspect.CLASSIC -> 6
                        },
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Footer: torn-paper strip with branding.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TornFooterStrip(tint = ink.copy(alpha = 0.10f), seed = topicName.hashCode() + 31)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (sharerName.isNotBlank()) "$sharerName · via Curio ✦" else "via Curio ✦",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ink.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/** Seeded tiled watermark of the category glyph across the whole card. */
@Composable
private fun WatermarkPattern(glyph: String, tint: Color, seed: Int) {
    val measurer = rememberTextMeasurer()
    val glyphStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val layout = measurer.measure(AnnotatedString("\u2726"), style = glyphStyle)
        val cell = size.width / 4.2f
        var row = 0
        var y = -cell / 2f
        while (y < size.height + cell) {
            var col = 0
            val xOff = if (row % 2 == 0) 0f else cell / 2f
            var x = -cell / 2f + xOff
            while (x < size.width + cell) {
                val wobble = sin((seed + row * 13 + col * 7).toFloat()).toFloat()
                rotate(wobble * 12f, pivot = Offset(x + cell / 2f, y + cell / 2f)) {
                    drawText(
                        textLayoutResult = layout,
                        color = tint,
                        topLeft = Offset(x, y)
                    )
                }
                x += cell
                col++
            }
            y += cell
            row++
        }
    }
}

/**
 * A broad, soft torn-paper edge drawn as a filled band along its top —
 * the same visual language as the note editors' torn cards, simplified
 * for one-pass bitmap capture.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.tornPath(
    tint: Color,
    seed: Int,
    bandHeightPx: Float
): Path {
    val path = Path()
    path.moveTo(0f, size.height)
    path.lineTo(0f, size.height - bandHeightPx)
    var x = 0f
    var i = 0
    while (x < size.width) {
        val step = size.width / 14f
        val t = (x + step).coerceAtMost(size.width)
        val jag = (if ((seed + i) % 2 == 0) 1 else -1) * bandHeightPx * 0.22f *
            (0.6f + ((seed * (i + 3)) % 7) / 10f)
        path.quadraticBezierTo(
            x + step / 2f,
            size.height - bandHeightPx + jag,
            t,
            size.height - bandHeightPx + (if (i % 2 == 0) bandHeightPx * 0.08f else -bandHeightPx * 0.08f)
        )
        x += step
        i++
    }
    path.lineTo(size.width, size.height)
    path.close()
    return path
}

@Composable
private fun TornFooterStrip(tint: Color, seed: Int) {
    Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
        drawPath(tornPath(tint, seed, 22.dp.toPx()), tint)
    }
}

/**
 * v292 — the TOPIC SHARE SHEET: live preview + customization before
 * sharing. Pick the aspect (9:16 story / 3:4 classic), what the frosted
 * pane shows (the topic's quick fact / your own custom fact line / a
 * review), edit the text inline, then share the rendered PNG card.
 * Mirrors EntryDetailScreen's share-sheet structure.
 */
@Composable
fun TopicShareSheet(
    topicName: String,
    categoryName: String,
    categoryGlyph: String,
    accent: Color,
    quickFact: String,
    authority: String,
    context: android.content.Context,
    initialReview: String = "",
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var aspect by rememberSaveable { mutableStateOf(ShareCardAspect.PORTRAIT) }
    var source by rememberSaveable { mutableStateOf(ShareFactSource.QUICK_FACT.name) }
    var customText by rememberSaveable { mutableStateOf("") }
    var reviewText by rememberSaveable { mutableStateOf(initialReview) }
    val sharer = com.curio.app.data.AppPreferences.getDisplayName(context).ifBlank { "" }

    val factText = when (source) {
        ShareFactSource.CUSTOM_FACT.name -> customText.ifBlank { "Add your own fact about this discovery…" }
        ShareFactSource.REVIEW.name -> reviewText.ifBlank { "Write your review of this discovery…" }
        else -> quickFact
    }

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

            // ── Live preview — the exact exported card ──
            Box(
                modifier = Modifier
                    .width(if (aspect == ShareCardAspect.PORTRAIT) 250.dp else 280.dp)
                    .aspectRatio(aspect.widthDp.toFloat() / aspect.heightDp.toFloat())
                    .shadow(8.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
            ) {
                TopicShareCard(
                    topicName = topicName,
                    categoryName = categoryName,
                    categoryGlyph = categoryGlyph,
                    accent = accent,
                    factText = factText,
                    sharerName = sharer,
                    aspect = aspect
                )
            }

            // ── Aspect picker ──
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShareOptionPill(
                    label = ShareCardAspect.PORTRAIT.label,
                    icon = CurioIcons.Image,
                    selected = aspect == ShareCardAspect.PORTRAIT
                ) { aspect = ShareCardAspect.PORTRAIT }
                ShareOptionPill(
                    label = ShareCardAspect.CLASSIC.label,
                    icon = CurioIcons.Image,
                    selected = aspect == ShareCardAspect.CLASSIC
                ) { aspect = ShareCardAspect.CLASSIC }
            }

            // ── Fact source picker ──
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShareFactSource.entries.forEach { option ->
                    ShareOptionPill(
                        label = option.label,
                        icon = CurioIcons.FormatText,
                        selected = source == option.name
                    ) { source = option.name }
                }
            }

            // ── Editable text for Custom fact / Review ──
            if (source == ShareFactSource.CUSTOM_FACT.name || source == ShareFactSource.REVIEW.name) {
                val value = if (source == ShareFactSource.CUSTOM_FACT.name) customText else reviewText
                OutlinedTextField(
                    value = value,
                    onValueChange = { v ->
                        if (source == ShareFactSource.CUSTOM_FACT.name) customText = v else reviewText = v
                    },
                    placeholder = {
                        Text(
                            if (source == ShareFactSource.CUSTOM_FACT.name) "Your custom fact"
                            else "Your review",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Share action ──
            Button(
                onClick = {
                    shareComposableCard(
                        context = context,
                        cardSize = DpSize(aspect.widthDp.dp, aspect.heightDp.dp),
                        authority = authority,
                        card = {
                            TopicShareCard(
                                topicName = topicName,
                                categoryName = categoryName,
                                categoryGlyph = categoryGlyph,
                                accent = accent,
                                factText = factText,
                                sharerName = sharer,
                                aspect = aspect
                            )
                        }
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Share image card",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    }
}

/** One option pill in the topic share sheet. */
@Composable
private fun ShareOptionPill(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit
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
                name = icon,
                contentDescription = null,
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
