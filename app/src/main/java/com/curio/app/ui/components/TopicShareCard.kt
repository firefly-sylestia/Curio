package com.curio.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.AppPreferences
import com.curio.app.ui.theme.ChangaOneFontFamily
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.toHsl

/** v292 — share-card aspect options. */
enum class ShareCardAspect(val label: String, val widthDp: Int, val heightDp: Int) {
    PORTRAIT("9:16", 405, 720),
    CLASSIC("3:4", 450, 600)
}

/**
 * v292e — ONE content source for the share hub's frosted pane. The sheet
 * always offers Quick fact + Custom fact; saved-entry callers pass their
 * own [ShareCardContent]s in between (Quote / Note / Review — whatever the
 * entry's capture format actually produced). A review source carries its
 * star [rating], which the card renders as a star row.
 */
data class ShareCardContent(
    val id: String,
    val label: String,
    val text: String,
    val rating: Int? = null
)

/**
 * v292e — THE TOPIC SHARE CARD, redesigned:
 *  - full-bleed deep→light category GRADIENT only (the torn-paper footer is
 *    gone — user call: keep it clean);
 *  - the WATERMARK now tiles the CATEGORY GLYPH itself (the same icon the
 *    reveal hero and every page watermark wear) instead of the generic ✦
 *    character that didn't match the app;
 *  - a cleaner simulated-frost pane (soft wash + hairline rim, no heavy
 *    offset shadow) holding the chosen content;
 *  - an optional STAR RATING row for review shares;
 *  - a cut-proof "via Curio" footer (single line, ellipsized name).
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
    modifier: Modifier = Modifier,
    ratingStars: Int? = null
) {
    val base = toHsl(accent)
    val deep = fromHsl(base.h, base.s, (base.l * 0.55f).coerceIn(0f, 0.5f))
    val ink = Color.White
    val display = topicName.substringBeforeLast(" (")

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(listOf(deep, accent)),
                RoundedCornerShape(4.dp)
            )
    ) {
        // ── Category-glyph watermark tile ───────────────────────────────
        GlyphWatermark(glyph = categoryGlyph, tint = ink.copy(alpha = 0.07f), seed = topicName.hashCode())

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

            // Middle: topic name + frosted content pane (+ optional stars).
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                if (ratingStars != null && ratingStars > 0) {
                    // Review share: the star row rides above the pane so the
                    // rating reads instantly, matching the Reel Notes editor.
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(5) { i ->
                            CurioIcon(
                                name = if (i < ratingStars) CurioIcons.Star else CurioIcons.StarOutline,
                                contentDescription = null,
                                tint = if (i < ratingStars) Color(0xFFFFC94D) else ink.copy(alpha = 0.35f),
                                size = 26.dp
                            )
                        }
                    }
                }
                // Simulated-frost pane holding the chosen content.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Soft grounded shadow directly under the pane…
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.12f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                                topLeft = Offset(0f, 4.dp.toPx())
                            )
                            // …a gentle top-lit glass wash…
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.10f))
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
                            )
                            // …and one crisp hairline rim (the glass edge).
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.30f),
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
                            ShareCardAspect.PORTRAIT -> 8
                            ShareCardAspect.CLASSIC -> 6
                        },
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Footer: sparkle + branding — single line, never clipped.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CurioIcon(
                    name = CurioIcons.AutoAwesome,
                    contentDescription = null,
                    tint = ink.copy(alpha = 0.45f),
                    size = 18.dp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (sharerName.isNotBlank()) "$sharerName · via Curio" else "via Curio",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ink.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * v292e — seeded TILE of the category's own glyph across the card. Same
 * language as the app's page watermarks (CurioWatermarkBackdrop): the real
 * category icon, softly rotated per cell by a deterministic wobble.
 */
@Composable
private fun GlyphWatermark(glyph: String, tint: Color, seed: Int) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cell = 118.dp
        val iconSize = 54.dp
        var row = 0
        var y = -cell / 2
        while (y.value < maxHeight.value + cell.value) {
            val xOff = if (row % 2 == 0) 0f else cell.value / 2f
            var col = 0
            var x = -cell.value / 2f + xOff
            while (x < maxWidth.value + cell.value) {
                val wobble = kotlin.math.sin((seed + row * 13 + col * 7).toFloat()) * 12f
                CurioIcon(
                    name = glyph,
                    contentDescription = null,
                    tint = tint,
                    size = iconSize,
                    modifier = Modifier
                        .offset(x = x.dp, y = y)
                        .rotate(wobble)
                )
                x += cell.value
                col++
            }
            y += cell
            row++
        }
    }
}

/**
 * v292e — THE TOPIC SHARE SHEET (the single share hub). Live preview +
 * customization before sharing: pick the aspect (9:16 story / 3:4 classic),
 * what the frosted pane shows — the topic's quick fact, any SAVED content
 * passed via [savedSources] (quote / note / review with its star rating),
 * or your own custom fact line — edit inline, then share the rendered PNG.
 *
 * v292f PREVIEW ACCURACY: the preview renders the card at [previewWidth] dp
 * (the same width the export captures at), centered in the sheet. The
 * exported image uses the SAME dp → px math as the preview, so text
 * wrapping and placement match exactly — no scaling mismatch.
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
    savedSources: List<ShareCardContent> = emptyList(),
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var aspect by rememberSaveable { mutableStateOf(ShareCardAspect.PORTRAIT) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var customText by rememberSaveable { mutableStateOf("") }
    val sharer = AppPreferences.getDisplayName(context).ifBlank { "" }

    // Source order: Quick fact → saved content (quote/note/review…) → Custom.
    val quick = ShareCardContent(QUICK_FACT_ID, "Quick fact", quickFact)
    val custom = ShareCardContent(CUSTOM_FACT_ID, "Custom fact", "")
    val activeId = selectedId ?: quick.id
    val activeSource = when (activeId) {
        CUSTOM_FACT_ID -> custom.copy(text = customText.ifBlank { "Add your own fact about this discovery…" })
        else -> (savedSources.firstOrNull { it.id == activeId } ?: quick)
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
                sources = listOf(quick) + savedSources + listOf(custom),
                activeSource = activeSource,
                onSelectSource = { selectedId = it },
                customEditing = activeId == CUSTOM_FACT_ID,
                customText = customText,
                onCustomTextChange = { customText = it },
                onShared = onDismiss
            )
        }
    }
}

/** Shared ids for the always-present sources. */
const val QUICK_FACT_ID = "quick_fact"
const val CUSTOM_FACT_ID = "custom_fact"

/**
 * v292e — the hub body shared by the topic sheet AND the detail-entry
 * sheet: preview (accurate full-size-scaled render) + aspect pills +
 * content-source pills + optional custom editor + share button.
 */
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
    onShared: () -> Unit
) {
    // v292f — preview renders at EXACTLY the same dp width the export
    // captures at, centered in the sheet. No graphicsLayer scaling.
    val previewWidth = 280.dp
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
            ratingStars = activeSource.rating
        )
    }

    // ── Aspect picker ──
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

    // ── Content source picker ──
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        sources.forEach { option ->
            ShareOptionPill(
                label = option.label + (option.rating?.takeIf { it > 0 }
                    ?.let { r -> " · " + "★".repeat(r) } ?: ""),
                icon = CurioIcons.FormatText,
                selected = option.id == activeSource.id
            ) { onSelectSource(option.id) }
        }
    }

    // ── Editable text for Custom fact ──
    if (customEditing) {
        OutlinedTextField(
            value = customText,
            onValueChange = onCustomTextChange,
            placeholder = {
                Text("Your custom fact", style = MaterialTheme.typography.bodyMedium)
            },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // v292f — export at the EXACT dp size the preview renders at, so
    // the saved PNG is pixel-perfect match of what you saw.
    val exportCardHeight = previewWidth * aspect.heightDp.toFloat() / aspect.widthDp.toFloat()

    // ── Share action ──
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
                        ratingStars = activeSource.rating
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

/** One option pill in the share hub. */
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
