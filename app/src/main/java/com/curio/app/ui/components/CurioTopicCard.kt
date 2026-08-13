package com.curio.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CaptureData
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioEntry
import com.curio.app.data.formatSessionShort
import com.curio.app.ui.theme.CurioGradients
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.themedAccent

/**
 * Cabinet's entry card — used in the 2-col grid (Curio Cabinet contract).
 *
 * v8.0 — the compact redesign: a smaller hero header with the SAME
 * category watermark language as the big torn heroes (mirrored scatter
 * glyphs at a whisper alpha), and a minimal body — title + when-it-was-
 * saved + the format symbol. No body preview, no tags: the card stays
 * small so the Cabinet→Detail shared-element morph reads seamless (less
 * content to reconcile while the bounds animate).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CurioEntryCard(
    entry: CurioEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    val cat = CurioCategories.byId(entry.topic.categoryId)
    val accent = cat.themedAccent()
    // Cabinet cards are recomposed while the grid settles and while the
    // toolbar/search state changes. Keep the header brush stable per accent
    // so opening the Cabinet does not allocate a new gradient for every card.
    // cardGradient reads MaterialTheme, so resolve it in the composable
    // scope before remembering the non-composable Brush allocation.
    val headerGradient = CurioGradients.cardGradient(accent)
    val headerBrush = remember(headerGradient) {
        Brush.verticalGradient(headerGradient)
    }

    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = CurioMotion.Springs.Press,
        label = "cardPress"
    )

    // Reset press state after navigation
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(300)
            pressed = false
        }
    }

    Surface(
        modifier = modifier
            .scale(pressScale)
            .combinedClickable(
                onClick = {
                    pressed = true
                    onClick()
                },
                onLongClick = onLongClick
            )
            // v9.x — theme-style edge shine (hairline + top shine).
            .categoryEdgeShine(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        // v9.x — AMOLED cards are proper pitch black now: the old grey
        // surfaceContainerHigh lift is replaced by the black-glass shine
        // edge, so the cards read as boxes without sacrificing OLED black.
        color = if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
        },
        // v27q — elevation is a flat 2dp in both states: selection reads
        // through the check badge in the header corner, not a raise (the old
        // 8/3 raise smeared the card while it animated).
        shadowElevation = 2.dp,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Compact hero header — category watermark scatter at a
            // whisper alpha (the same language as the big torn heroes),
            // the category glyph as a bright focal mark, and the selection
            // / legacy badges tucked into the corners.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(headerBrush),
                contentAlignment = Alignment.Center
            ) {
                MiniHeroWatermark(cat)
                CurioIcon(
                    name = cat.iconGlyph,
                    contentDescription = null,
                    tint = cat.onAccent().copy(alpha = 0.9f),
                    size = 44.dp
                )
                if (selected) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CurioIcon(
                                name = CurioIcons.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                size = 18.dp
                            )
                        }
                    }
                }
                // Legacy badge — restored FieldMind entries wear a small
                // dark pill in the header corner so they stay recognizable
                // next to native Curio captures.
                if (entry.isLegacy) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.30f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "LEGACY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // ── Minimal body — title, when-it-was-saved, format symbol.
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.topic.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // v17 — the explore-session duration sits alongside the
                        // when-it-was-saved label (hidden when no session was
                        // recorded).
                        if (entry.sessionTimeMillis > 0L) {
                            CurioIcon(
                                name = CurioIcons.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 12.dp
                            )
                            Text(
                                text = formatSessionShort(entry.sessionTimeMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = formatTimeAgo(entry.capturedAtDaysAgo),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    EntryFormatBadges(entry)
                }
            }
        }
    }
}

/**
 * A scaled-down version of the torn-hero watermark: a mirrored pair of
 * small category glyphs at a whisper alpha, tucked into the header's
 * corners so the card carries the same visual language as the full heroes
 * without fighting the centered category mark.
 */
@Composable
private fun BoxScope.MiniHeroWatermark(cat: CurioCategory) {
    val symbols = CurioIcons.heroWatermarkSymbols(cat.family)
    val ink = cat.onAccent()
    if (symbols.size >= 2) {
        MiniHeroGlyph(
            glyph = symbols[0],
            alignment = Alignment.TopStart,
            size = 30.dp,
            rotation = -10f,
            alpha = 0.18f,
            tint = ink
        )
        MiniHeroGlyph(
            glyph = symbols[1],
            alignment = Alignment.BottomEnd,
            size = 34.dp,
            rotation = 10f,
            alpha = 0.20f,
            tint = ink
        )
    }
    if (symbols.size >= 4) {
        MiniHeroGlyph(
            glyph = symbols[3],
            alignment = Alignment.TopEnd,
            size = 24.dp,
            rotation = 8f,
            alpha = 0.14f,
            tint = ink
        )
        MiniHeroGlyph(
            glyph = symbols[2],
            alignment = Alignment.BottomStart,
            size = 26.dp,
            rotation = -7f,
            alpha = 0.16f,
            tint = ink
        )
    }
}

/** One mini watermark glyph — theme-aware ink at a whisper alpha. */
@Composable
private fun BoxScope.MiniHeroGlyph(
    glyph: String,
    alignment: Alignment,
    size: Dp,
    rotation: Float,
    alpha: Float,
    tint: Color
) {
    CurioIcon(
        name = glyph,
        contentDescription = null,
        tint = tint.copy(alpha = alpha),
        size = size,
        modifier = Modifier
            .align(alignment)
            .padding(8.dp)
            .graphicsLayer { rotationZ = rotation }
    )
}

/**
 * Bottom-right format indicator on a Cabinet card: a plain glyph for
 * single-format entries, or a small STACKED badge — one circle per take's
 * format (capped at 3, with a "+N" overflow chip) — for multi-section
 * Portfolio entries, so the whole take composition is visible at a glance.
 */
@Composable
private fun EntryFormatBadges(entry: CurioEntry) {
    val sections = (entry.captureData as? CaptureData.Portfolio)?.sections.orEmpty()
    if (sections.isEmpty()) {
        // Single-format entry (or an empty/malformed Portfolio): keep the
        // existing single-glyph look.
        CurioIcon(
            name = formatGlyph(entry.format),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 16.dp
        )
        return
    }
    val visible = sections.take(3)
    val extra = sections.size - visible.size
    Row(
        // Negative spacing makes each badge overlap the previous one, like an
        // avatar stack; later children draw on top.
        horizontalArrangement = Arrangement.spacedBy((-6).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visible.forEach { section ->
            FormatBadgeCircle(glyph = formatGlyph(section.format))
        }
        if (extra > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "+$extra",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** One circular format badge in the stacked [EntryFormatBadges] cluster. */
@Composable
private fun FormatBadgeCircle(glyph: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(18.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CurioIcon(
                name = glyph,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 12.dp
            )
        }
    }
}

internal fun formatGlyph(format: CaptureFormat): String = when (format) {
    CaptureFormat.SoundBite -> CurioIcons.Mic
    CaptureFormat.ReelNotes -> CurioIcons.Edit
    CaptureFormat.Marginalia -> CurioIcons.MenuBook
    CaptureFormat.GalleryWall -> CurioIcons.Image
    CaptureFormat.FieldNotes -> CurioIcons.AutoAwesome
    CaptureFormat.OpenNotebook -> CurioIcons.MenuBook
}

private fun formatTimeAgo(daysAgo: Int): String = when (daysAgo) {
    0 -> "Today"
    1 -> "Yesterday"
    else -> "${daysAgo}d ago"
}
