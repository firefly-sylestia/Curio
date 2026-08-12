package com.curio.app.features.recent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.ExploredTopic
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.UnexploredTopic
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioEmptyState
import com.curio.app.ui.components.CurioForwardArrow
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.themedAccent

/** A single unified item for the Home preview and the full Recents page. */
internal sealed interface RecentFeedItem {
    val timestamp: Long
    val key: String

    data class Explored(val topic: ExploredTopic) : RecentFeedItem {
        override val timestamp: Long = topic.exploredAtMillis
        override val key: String = "explored_${topic.categoryId.name}_${topic.topicName}"
    }

    data class Unexplored(val topic: UnexploredTopic) : RecentFeedItem {
        override val timestamp: Long = topic.seenAtMillis
        override val key: String = "unexplored_${topic.categoryId.name}_${topic.topicName}"
    }

    data class SavedEntry(val entry: CurioEntry) : RecentFeedItem {
        override val timestamp: Long = entry.capturedAtMillis
        override val key: String = "entry_${entry.id}"
    }
}

/**
 * Newest-first feed shared by Home's five-item preview and the full page.
 *
 * v7.39 — one row per topic: multiple captures of the same topic collapse
 * to the newest one, and a topic's explored/unexplored row is superseded
 * by its newest saved entry (or vice-versa, by timestamp) — so the same
 * topic never appears twice on Home or in Recents.
 */
internal fun buildRecentFeed(
    entries: List<CurioEntry>,
    explored: List<ExploredTopic>,
    unexplored: List<UnexploredTopic>
): List<RecentFeedItem> = buildList {
    addAll(explored.map { RecentFeedItem.Explored(it) })
    addAll(unexplored.map { RecentFeedItem.Unexplored(it) })
    addAll(entries.map { RecentFeedItem.SavedEntry(it) })
}
    // Collapse to the newest item of each topic, then sort the feed.
    // (maxByOrNull is the non-deprecated form; groups are never empty.)
    .groupBy { it.topicIdentityKey() }
    .map { (_, items) -> items.maxByOrNull { it.timestamp } }
    .filterNotNull()
    .sortedByDescending { it.timestamp }

/**
 * Stable identity of the topic an item belongs to — what the feed dedupes
 * on, so an explored row and its saved entries count as the same topic.
 */
private fun RecentFeedItem.topicIdentityKey(): String = when (this) {
    is RecentFeedItem.Explored -> "${topic.categoryId.name}_${topic.topicName}"
    is RecentFeedItem.Unexplored -> "${topic.categoryId.name}_${topic.topicName}"
    is RecentFeedItem.SavedEntry -> "${entry.topic.categoryId.name}_${entry.topic.name}"
}

/**
 * Full Recent page opened from Home's View all action. It keeps the same
 * category-glyph watermark language as Home, Spin, and detail pages while
 * allowing the complete persisted recent feed to be browsed.
 */
@Composable
fun RecentScreen(navController: NavController) {
    val entries by produceState<List<CurioEntry>>(initialValue = emptyList()) {
        try {
            CurioRepositoryHolder.repo.observeAll().collect { value = it }
        } catch (_: Exception) {
            value = emptyList()
        }
    }
    val explored = ExploreSessionStore.recentlyExploredState
    val unexplored = ExploreSessionStore.recentlyUnexploredState
    val feed = remember(entries, explored, unexplored) {
        buildRecentFeed(entries, explored, unexplored)
    }
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CurioBackButton(onClick = { navController.popBackStack() })
                Column {
                    Text(
                        text = "Recents",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Your latest discoveries, all in one place",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (feed.isEmpty()) {
                CurioEmptyState(
                    glyph = CurioIcons.History,
                    headline = "No discoveries yet",
                    subtext = "Explore a topic or save a capture. Your recent finds will show up here.",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = wideContentEdgePadding(), vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(feed, key = { it.key }) { item ->
                        RecentFeedRow(item = item, navController = navController)
                    }
                    item { Spacer(Modifier.size(12.dp)) }
                }
                // Side scroll indicator — thin overlay knob, grows on touch.
                CurioVerticalScrollIndicator(
                    state = listState.scrollIndicatorState,
                    onScrollBy = { listState.scrollBy(it) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                )
                }
            }
        }
    }
}

@Composable
private fun RecentFeedRow(item: RecentFeedItem, navController: NavController) {
    when (item) {
        is RecentFeedItem.Explored -> {
            val topic = item.topic
            RecentTopicRow(
                categoryId = topic.categoryId,
                topicName = topic.topicName,
                label = if (topic.wasUnexplored) "Resumed · tap to write about it" else "Explored · tap to write about it",
                tag = if (topic.wasUnexplored) "Resumed" else null,
                onClick = {
                    navController.navigate(
                        CurioRoutes.captureFor(topic.categoryId.routeSlug, topic.topicName)
                    ) { launchSingleTop = true }
                }
            )
        }
        is RecentFeedItem.Unexplored -> {
            val topic = item.topic
            RecentTopicRow(
                categoryId = topic.categoryId,
                topicName = topic.topicName,
                label = "Left without exploring · tap to resume",
                tag = "Unexplored",
                onClick = {
                    navController.navigate(
                        CurioRoutes.revealFor(topic.categoryId.routeSlug, topic.topicName)
                    ) { launchSingleTop = true }
                }
            )
        }
        is RecentFeedItem.SavedEntry -> {
            val entry = item.entry
            val category = CurioCategories.byId(entry.topic.categoryId)
            Surface(
                onClick = {
                    navController.navigate(CurioRoutes.entryDetail(entry.id)) {
                        launchSingleTop = true
                    }
                },
                shape = RoundedCornerShape(22.dp),
                color = category.categorySurface(),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CurioIcon(
                        name = category.iconGlyph,
                        contentDescription = null,
                        tint = category.themedAccent(),
                        size = 26.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.topic.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${category.displayName} · ${entry.capturedAtDaysAgoLabel()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    CurioForwardArrow(
                        contentDescription = "Open capture",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentTopicRow(
    categoryId: CategoryId,
    topicName: String,
    label: String,
    tag: String?,
    onClick: () -> Unit
) {
    val category = CurioCategories.byId(categoryId)
    val accent = category.themedAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = category.categorySurface(),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.16f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        name = category.iconGlyph,
                        contentDescription = null,
                        tint = accent,
                        size = 23.dp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = topicName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (tag != null) {
                        Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.14f)) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = accent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CurioForwardArrow(contentDescription = label, tint = accent)
        }
    }
}

private fun CurioEntry.capturedAtDaysAgoLabel(): String = when (val days = capturedAtDaysAgo) {
    0 -> "today"
    1 -> "yesterday"
    else -> "${days}d ago"
}
