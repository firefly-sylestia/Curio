package com.curio.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategory
import com.curio.app.data.TopicJsonLoader
import kotlinx.coroutines.CancellationException
import com.curio.app.ui.theme.CurioGradients
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.themedAccent

/**
 * Compact category card shared by the standalone category picker and the
 * Spin page picker sheet — category name, live topic count, a subtle ghost
 * watermark of the category glyph on the right edge, and an optional
 * selected state. One component so the two pickers can never drift apart
 * visually.
 *
 * Interaction contract (both pickers agree on this):
 *  - **Tap** — the picker's default action: open that category in the Spin
 *    page (single-select launch).
 *  - **Tap + hold (long-press)** — enter multi-select mode and select this
 *    card; further taps toggle selection. The Done button only appears in
 *    this mode.
 *
 * The selected state is a distinct raised treatment (crisp white rule + a
 * soft inner glow sheen) — deliberately NOT a check badge, so active vs
 * inactive read as two different card looks.
 *
 * Idle cards wear the category's tinted surface — the page wash's stronger
 * sibling — so the grid reads as tints of the background ("the tint, but a
 * little different") and the card pops to the full bright gradient only
 * when selected.
 */
@Composable
fun CurioCategoryCard(
    category: CurioCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    // v27i — New-lane tile that has no shipped content yet: dimmed,
    // non-interactive, and labelled "Coming soon" instead of a topic count.
    comingSoon: Boolean = false
) {
    // True press tracking (not a sticky click flag): the card returns to
    // rest scale after every tap — important now that cards are toggle
    // targets in multi-select mode and get tapped repeatedly.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.96f
            isSelected -> 1.03f
            else -> 1f
        },
        animationSpec = CurioMotion.Springs.Press,
        label = "categoryCardScale"
    )

    val isWildcard = category.id == CategoryId.WILDCARD
    // v28 — the SELECTED card blooms to the SATURATED raw category accent:
    // the old cardGradient start was black-DARKENED (10% light / 28% dark),
    // so tapping a tile made it read DARKER than the idle tint — the exact
    // complaint. The selected crown is now the full-saturation researched
    // accent melting into the page: brighter and more vivid, never darker.
    val saturated = category.accent
    val selectedGradient = listOf(
        saturated,
        lerp(saturated, MaterialTheme.colorScheme.background, 0.30f)
    )
    // Selected content reads WHITE on the saturated crown (the pastel-aware
    // cardContentInk is a deep ink designed for the airy pastel fills).
    val selectedInk = if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED)
        MaterialTheme.colorScheme.onSurface else Color.White
    val cardColor = CurioGradients.categoryCardFill(
        category.themedAccent(),
        isCurioDarkTheme()
    )
    // Idle cards wear the category's tinted surface — the page wash, but a
    // touch stronger — so unselected tiles sit on the washed page as soft
    // tints of their own color instead of shouting in full brightness.
    // v27n — AMOLED idle tiles wear the scheme's faint container step as
    // their elevation (the old pitch-black tile + hairline outline read flat;
    // the container step + accent-tinted edge shine keep the tile defined).
    val idleSurface = when (AppPreferences.themeStyleState) {
        AppPreferences.THEME_STYLE_AMOLED -> MaterialTheme.colorScheme.surfaceContainerLow
        // v12 — Material: a soft category-tinted tile on the hue-neutral
        // page — the old plain device-grey tile read dull and disconnected
        // from the deck's category colors.
        AppPreferences.THEME_STYLE_MATERIAL -> {
            if (isCurioDarkTheme()) {
                lerp(MaterialTheme.colorScheme.surfaceContainerHigh, category.themedAccent(), 0.12f)
            } else {
                lerp(MaterialTheme.colorScheme.surfaceContainerHigh, category.themedAccent(), 0.14f)
            }
        }
        else -> if (isCurioDarkTheme()) {
            // v46 — BLACKISH idle tiles in dark mode: the categorySurface
            // mid-tone still read lighter than the page (low contrast).
            // Idle tiles now hug the midnight surface, pushed further
            // toward black — the SELECTED tile keeps its vivid full-accent
            // gradient unchanged, so idle-vs-active contrast is the story.
            lerp(MaterialTheme.colorScheme.surfaceContainerLow, Color.Black, 0.55f)
        } else {
            category.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow)
        }
    }
    val idleInk = category.categoryInk()
    // v27q — elevation is a flat 2dp in both states: the selected tile
    // already wears the full solid-accent gradient, so it never needs to
    // raise (the old 8/3 raise was the blurry-shadow bug class).
    val cardElevation = 2.dp
    // Live topic count — reads the warm cache immediately, then reloads (a
    // cache hit) if the pool was ever cleared (e.g. onTrimMemory) so the
    // card never latches a stale "0 topics". With the catalog warmed during
    // splash this resolves on the first frame from cache.
    val topicCount by produceState(
        initialValue = TopicJsonLoader.cached(category.id)?.size ?: 0,
        category.id
    ) {
        value = try {
            TopicJsonLoader.load(category.id).size
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            0
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = cardElevation,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .scale(scale)
            // v28 — dark mode elevation visibility (glow + hairline).
            .curioDarkGlow(cardElevation, RoundedCornerShape(22.dp))
            // v9.x — the theme-style edge shine: AMOLED black-glass and
            // Material both wear the category-colored rim light. Coming-soon
            // tiles wear a much fainter rim so they read as "locked".
            .categoryEdgeShine(
                RoundedCornerShape(22.dp),
                accent = category.themedAccent(),
                intensity = if (comingSoon) 0.25f else 1f
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isSelected) Brush.verticalGradient(selectedGradient)
                    // v27n — coming-soon tile fill is OPAQUE (was 50% alpha,
                    // which let the elevation shadow bleed through as a blurry
                    // disc; the faint edge shine + label carry the "locked"
                    // dimness now).
                    else if (comingSoon) SolidColor(
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    else SolidColor(idleSurface),
                    RoundedCornerShape(22.dp)
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick?.let { long ->
                        {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            long()
                        }
                    },
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !comingSoon
                )
        ) {
            // ── Active-state sheen — soft white glow over the gradient so
            //    the selected tile reads as clearly raised, distinct from the
            //    idle tile (no check badge).
            if (isSelected) {
                // v28 — a TRUE light glow over the saturated crown so the
                // selected tile reads raised and bright (the old cardInk
                // sheen was a deep ink in pastel light — it DARKENED the
                // tile). White 14% reads as a highlight on the vivid fill.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.White.copy(alpha = 0.14f),
                            RoundedCornerShape(22.dp)
                        )
                )
            }

            // Ghost icon — tinted with the card's gradient accent color
            // (echoed softly toward the onAccent ink) so the watermark
            // carries the same palette as the main-card gradient, not a
            // flat white ghost. onAccent resolves to white off pastel mode.
            CurioIcon(
                name = category.iconGlyph,
                contentDescription = null,
                tint = when {
                    isSelected -> lerp(cardColor, selectedInk, 0.55f).copy(alpha = 0.18f)
                    comingSoon -> idleInk.copy(alpha = 0.08f)
                    else -> idleInk.copy(alpha = 0.16f)
                },
                size = 64.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = when {
                            isSelected -> selectedInk
                            comingSoon -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            comingSoon -> "Coming soon"
                            isWildcard -> "Surprise mix"
                            else -> "$topicCount topics"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) selectedInk.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (comingSoon) 0.8f else 1f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
