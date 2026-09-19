package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.LaneKnowledge
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.themedAccent

/**
 * v409 — THE LANE GRID: the curiosity map as REAL UI.
 *
 * The constellation used to be a painted Canvas (star circles + hairlines on a
 * deep-space sky). It read as artwork, not as something you can use: the stars
 * were 5px dots, the tap targets guessed, and on the drawer surface the whole
 * thing was a picture sitting in a navigation panel. The user's call —
 * "it should not be a drawing anymore, but proper ui interactive style and
 * make it clean" — is this: every lane is a real card (icon, name, knowledge
 * bar) in a tiled grid, laid out by the layout system, themed by the app's own
 * roles, tappable with a real ripple, and selected with an animated accent
 * ring. Nothing is drawn: no Canvas, no SVG, no star math.
 *
 * The knowledge bar is the constellation's old "star size" (see
 * [LaneKnowledge.score]) expressed as a fraction of the member's strongest
 * lane, so the grid reads as progress you can compare lane to lane. Lanes sort
 * explored-first (by knowledge), then the rest in their catalog order.
 */
data class LaneGridItem(
    val id: CategoryId,
    val name: String,
    val icon: String,
    val accent: Color,
    /** [LaneKnowledge.score] — what the bar shows. */
    val knowledge: Int,
    val saved: Int,
    val explored: Boolean
)

/**
 * Builds the grid's items from the member's real per-lane knowledge, in the
 * member's own lane order (hidden lanes excluded), explored lanes first.
 */
@Composable
fun laneGridItems(knowledge: Map<CategoryId, LaneKnowledge>): List<LaneGridItem> =
    CurioCategories.visible
        .map { cat ->
            val k = knowledge[cat.id]
            LaneGridItem(
                id = cat.id,
                name = cat.displayName,
                icon = cat.iconGlyph,
                accent = cat.themedAccent(),
                knowledge = k?.score ?: 0,
                saved = k?.saves ?: 0,
                explored = k?.explored == true
            )
        }
        .sortedWith(
            compareByDescending<LaneGridItem> { it.explored }
                .thenByDescending { it.knowledge }
        )

/**
 * The tiled lane grid. [columns] tiles per row; [onSelect] gets the tapped
 * lane, or null when the already-selected tile is tapped again (deselect).
 *
 * [detail] renders the selected lane's own line UNDER ITS OWN ROW — a tap
 * near the bottom of a long grid opens the readout next to the tile that was
 * tapped instead of dropping it somewhere off-screen at the end of the grid.
 */
@Composable
fun CurioLaneGrid(
    lanes: List<LaneGridItem>,
    selected: CategoryId?,
    onSelect: (CategoryId?) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    tileHeight: Dp = 86.dp,
    detail: (@Composable (LaneGridItem) -> Unit)? = null
) {
    if (lanes.isEmpty()) return
    val strongest = lanes.maxOf { it.knowledge }.coerceAtLeast(1)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lanes.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    LaneTile(
                        item = item,
                        fraction = item.knowledge.toFloat() / strongest,
                        selected = item.id == selected,
                        height = tileHeight,
                        onClick = { onSelect(if (item.id == selected) null else item.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Keep the last row's tiles the same width as every other
                // row's instead of letting them stretch across the gap.
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
            val rowSelection = rowItems.firstOrNull { it.id == selected }
            if (rowSelection != null && detail != null) {
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(180)),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(120))
                ) {
                    detail(rowSelection)
                }
            }
        }
    }
}

/** One lane tile: icon, name, and its knowledge bar. */
@Composable
private fun LaneTile(
    item: LaneGridItem,
    fraction: Float,
    selected: Boolean,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val fill by animateColorAsState(
        targetValue = if (selected) lerp(base, item.accent, 0.22f) else base,
        animationSpec = tween(180),
        label = "laneTileFill"
    )
    val ring by animateColorAsState(
        targetValue = if (selected) item.accent.copy(alpha = 0.75f)
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(180),
        label = "laneTileRing"
    )
    // The bar grows in: a lane the member has built reads as a real meter
    // instead of a static chip, and the grow-in tells the eye which tiles are
    // the ones with knowledge on them.
    val grown by animateFloatAsState(
        targetValue = if (item.explored) fraction.coerceIn(0.05f, 1f) else 0f,
        animationSpec = tween(600),
        label = "laneTileBar"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = fill,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, ring),
        modifier = modifier.height(height)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CurioIcon(
                name = item.icon,
                contentDescription = item.name,
                tint = if (item.explored) item.accent
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                size = 22.dp
            )
            Text(
                item.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (item.explored) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                if (grown > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(grown)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(item.accent)
                    )
                }
            }
        }
    }
}

/**
 * The selected lane's own line, shown under the grid: which lane, how much
 * knowledge it holds, and — through [action] — the one door the caller wants
 * to offer for it.
 */
@Composable
fun CurioLaneDetailStrip(
    item: LaneGridItem,
    modifier: Modifier = Modifier,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = lerp(MaterialTheme.colorScheme.surfaceContainerHigh, item.accent, 0.12f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = item.accent.copy(alpha = 0.16f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(item.icon, null, tint = item.accent, size = 19.dp)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (item.explored) "${item.knowledge} knowledge · ${item.saved} saved"
                    else "Not explored yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Invoked through a null check (not `action?.invoke`) so a
            // nullable composable lambda stays a plain composable call site.
            if (action != null) action(this)
        }
    }
}
