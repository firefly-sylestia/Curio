package com.curio.app.features.recent

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.ExploredTopic
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.UnexploredTopic
import com.curio.app.data.formatSessionShort
import com.curio.app.navigation.CurioRoutes
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioDoodleEmptyState
import com.curio.app.ui.components.CurioForwardArrow
import com.curio.app.ui.components.CurioHoldPill
import com.curio.app.ui.components.CurioPatientHold
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.themedAccent
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.curio.app.features.settings.SettingsHeroTotalHeight

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
 *
 * v472 — [finished] is the store's done record, handed in already shaped as
 * explored rows (see `ExploreSessionStore.doneRecents`), so a topic the member
 * completed before completing recorded anything, or one the 12-entry recents cap
 * had pushed out, is still listed as explored. They carry no timestamp, so the
 * dedupe above lets a row with a real time supersede them and they sort to the
 * end of the feed rather than over a recent discovery.
 */
internal fun buildRecentFeed(
    entries: List<CurioEntry>,
    explored: List<ExploredTopic>,
    unexplored: List<UnexploredTopic>,
    finished: List<ExploredTopic> = emptyList()
): List<RecentFeedItem> = buildList {
    addAll(explored.map { RecentFeedItem.Explored(it) })
    addAll(finished.map { RecentFeedItem.Explored(it) })
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
    val context = LocalContext.current
    val entries by produceState<List<CurioEntry>>(initialValue = emptyList()) {
        try {
            CurioRepositoryHolder.repo.observeAll().collect { value = it }
        } catch (_: Exception) {
            value = emptyList()
        }
    }
    val explored = ExploreSessionStore.recentlyExploredState
    val unexplored = ExploreSessionStore.recentlyUnexploredState
    // v472 — every topic the member has finished (the store's done record), so
    // the page shows the topics completed before completing wrote a recents row
    // too. Read as a state key, so marking one done updates the page live.
    val finished = remember(ExploreSessionStore.doneTopicsState) {
        ExploreSessionStore.doneRecents()
    }
    val feed = remember(entries, explored, unexplored, finished) {
        buildRecentFeed(entries, explored, unexplored, finished)
    }
    // v3xx — LONG-PRESS a row for more (default tap opens the TOPIC now):
    // the option pill offers the alternative actions (write about it, open
    // the saved entry, remove from Recents).
    var optionItem by remember { mutableStateOf<RecentFeedItem?>(null) }
    val listState = rememberLazyListState()
val glassBackdrop = rememberLayerBackdrop()
    // v-tablet — the torn hero is NOT sticky on wide windows (landscape
    // tablet): it leads the list as its first item and scrolls away with it;
    // the pinned glass overlay stays phone-only.
    val wide = windowWidthSizeClass().isWide

    Box(
        modifier = Modifier
            .fillMaxSize()
            // v30 — "Hero follows Spin lane": the page wears the lane wash.
            .background(heroPageBackground())
    ) {
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                modifier = Modifier.fillMaxSize()
            )
        }

        // v26 — the full page now wears the settings-family torn-rose hero
        // (same as Manage Categories / Topic Database / Topic History): the
        // feed scrolls up and disappears under the ragged tear instead of a
        // plain back-button + title row.
        ScreenEntrance {
            // v255 — SCROLLING HERO (the Home/Profile construction): the
            // banner leads the page — as the empty state's top block or the
            // list's first item — and scrolls away with it.
            if (feed.isEmpty()) {
                Column {
                    SettingsHeroHeader(
                        title = "Recents",
                        subtitle = "Your latest discoveries, all in one place",
                        onBack = { navController.popBackStack() }
                    )
                    // v3xx — the app-wide doodle empty state.
                    CurioDoodleEmptyState(
                        headline = "No discoveries yet",
                        subtext = "Explore a topic or save a capture. Your recent finds will show up here.",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                // v407 — every row's hold waits CurioHoldMillis (2s) instead
                // of the platform's short press, so scrolling a long feed can
                // no longer arm a row's option pill on the way past.
                CurioPatientHold {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = wideContentEdgePadding(),
                            end = wideContentEdgePadding(),
                            top = if (wide) 0.dp else SettingsHeroTotalHeight,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (wide) {
                            item(key = "hero", contentType = "hero") {
                                SettingsHeroHeader(
                                    title = "Recents",
                                    subtitle = "Your latest discoveries, all in one place",
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        items(feed, key = { it.key }) { item ->
                            RecentFeedRow(
                                item = item,
                                navController = navController,
                                onLongPress = { optionItem = item }
                            )
                        }
                        item { Spacer(Modifier.size(12.dp)) }
                    }
                }
            }
        }

        if (feed.isNotEmpty()) {
            // Side scroll indicator — thin overlay knob, grows on touch.
            CurioVerticalScrollIndicator(
                state = listState.scrollIndicatorState,
                onScrollBy = { listState.dispatchRawDelta(it) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 10.dp, bottom = 16.dp)
            )
        }

        // v3xx — the long-press option pill: default tap opens the TOPIC;
        // the hold pill carries the write / open-entry / remove actions.
        optionItem?.let { target ->
            // (label, action, destructive)
            val actions = buildList<Triple<String, () -> Unit, Boolean>> {
                when (target) {
                    is RecentFeedItem.Explored -> {
                        add(Triple("Write about it", {
                            navController.navigate(
                                CurioRoutes.captureFor(target.topic.categoryId.routeSlug, target.topic.topicName)
                            ) { launchSingleTop = true }
                        }, false))
                        // v472 — this reaches the v472 finished rows too (a
                        // `doneRecents` topic carries `exploredAtMillis == 0`),
                        // and it means what it means on every explored row:
                        // `removeExplored` rolls back the recents entry AND the
                        // done mark together, because that is the app's own door
                        // for "not finished after all" — the row is only ever
                        // listed because of that mark. The Reveal's star is the
                        // other way back, and it leaves the history alone.
                        add(Triple("Remove from Recents", {
                            ExploreSessionStore.removeExplored(context, target.topic.categoryId, target.topic.topicName)
                        }, true))
                    }
                    is RecentFeedItem.Unexplored -> {
                        add(Triple("Write about it", {
                            navController.navigate(
                                CurioRoutes.captureFor(target.topic.categoryId.routeSlug, target.topic.topicName)
                            ) { launchSingleTop = true }
                        }, false))
                    }
                    is RecentFeedItem.SavedEntry -> {
                        add(Triple("Open saved entry", {
                            navController.navigate(CurioRoutes.entryDetail(target.entry.id)) { launchSingleTop = true }
                        }, false))
                        add(Triple("Write about it", {
                            navController.navigate(
                                CurioRoutes.captureFor(target.entry.topic.categoryId.routeSlug, target.entry.topic.name)
                            ) { launchSingleTop = true }
                        }, false))
                    }
                }
            }
            val pillTitle = when (target) {
                is RecentFeedItem.Explored -> target.topic.topicName
                is RecentFeedItem.Unexplored -> target.topic.topicName
                is RecentFeedItem.SavedEntry -> target.entry.topic.name
            }
            CurioHoldPill(
                title = pillTitle,
                actions = actions.map { it.first to it.second },
                destructiveIndexes = actions.mapIndexedNotNull { i, t -> if (t.third) i else null }.toSet(),
                onDismiss = { optionItem = null }
            )
        }
                // RESTORED (user request) — STICKY HERO drawn on TOP of the scroll
        // content: rows slide under the ragged tear as they scroll up, and
        // the back pill refracts them through REAL liquid glass.
        // v-tablet — pinned overlay is phone-only; wide windows scroll the
        // hero as the list's first item instead.
        if (!wide) {
            SettingsHeroHeader(title = "Recents", subtitle = "Your latest discoveries, all in one place", onBack = { navController.popBackStack() }, glassBackdrop = glassBackdrop)
        }

    }
}

@Composable
private fun RecentFeedRow(
    item: RecentFeedItem,
    navController: NavController,
    onLongPress: (RecentFeedItem) -> Unit
) {
    // v407 — the hold plays the long-press haptic Compose does not play for
    // `combinedClickable` itself (the app's own hold rows all do), and every
    // row shares the patient timeout provided by the screen.
    val haptics = LocalHapticFeedback.current
    val hold: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onLongPress(item)
    }
    when (item) {
        is RecentFeedItem.Explored -> {
            val topic = item.topic
            RecentTopicRow(
                categoryId = topic.categoryId,
                topicName = topic.topicName,
                // v3xx — the default tap now opens the TOPIC (keeps the
                // discovery open); the write/save flow moved to long-press.
                // v474 — "Resumed" is already the tag pill on the title line, and
                // the chevron already says tap; this line is the state.
                label = "Explored",
                tag = if (topic.wasUnexplored) "Resumed" else null,
                onClick = {
                    navController.navigate(
                        CurioRoutes.revealFor(topic.categoryId.routeSlug, topic.topicName)
                    ) { launchSingleTop = true }
                },
                onLongClick = hold
            )
        }
        is RecentFeedItem.Unexplored -> {
            val topic = item.topic
            RecentTopicRow(
                categoryId = topic.categoryId,
                topicName = topic.topicName,
                label = "Not explored",
                tag = "Unexplored",
                onClick = {
                    navController.navigate(
                        CurioRoutes.revealFor(topic.categoryId.routeSlug, topic.topicName)
                    ) { launchSingleTop = true }
                },
                onLongClick = hold
            )
        }
        is RecentFeedItem.SavedEntry -> {
            val entry = item.entry
            val category = CurioCategories.byId(entry.topic.categoryId)
            // v22 — the explore-session duration joins the meta line when one
            // was recorded ("Films · 2d ago · explored 12m"), matching the
            // detail hero's language.
            val meta = if (entry.sessionTimeMillis > 0L) {
                "${category.displayName} · ${entry.capturedAtDaysAgoLabel()} · explored ${formatSessionShort(entry.sessionTimeMillis)}"
            } else {
                "${category.displayName} · ${entry.capturedAtDaysAgoLabel()}"
            }
            Surface(
                // v3xx — default tap opens the TOPIC (the saved entry and
                // the write flow live behind the long-press pill).
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate(
                                CurioRoutes.revealFor(entry.topic.categoryId.routeSlug, entry.topic.name)
                            ) { launchSingleTop = true }
                        },
                        onLongClick = hold
                    ),
                shape = RoundedCornerShape(22.dp),
                color = category.categorySurface(),
                shadowElevation = 0.dp
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
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    CurioForwardArrow(
                        contentDescription = "Open topic",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentTopicRow(
    categoryId: CategoryId,
    topicName: String,
    label: String,
    tag: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val category = CurioCategories.byId(categoryId)
    val accent = category.themedAccent()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(22.dp),
        color = category.categorySurface(),
        shadowElevation = 0.dp
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
                    // v27 — the icon wears the category's deep ink (Home's
                    // explore-topic rows) instead of the pale accent, which
                    // washed out against the tinted surface.
                    CurioIcon(
                        name = category.iconGlyph,
                        contentDescription = null,
                        tint = category.categoryInk(),
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
                        // v198 — the same shaded category pill as Home's
                        // explore-topic rows: the accent pulled toward the
                        // card surface (~30% light / ~38% dark) so the pill
                        // reads as a solid shaded chip on the tinted card in
                        // light and a visibly tinted pill on the dark card
                        // (the old 14% blend read transparent). Pastel light
                        // uses the deep same-hue ink as the shade.
                        val tagShade = if (AppPreferences.pastelColorsState && !isCurioDarkTheme())
                            category.categoryInk() else accent
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = lerp(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                tagShade,
                                if (isCurioDarkTheme()) 0.38f else 0.30f
                            ),
                            // Same hairline rim as Home's explore-topic rows —
                            // the deep ink text + pastel fill alone read
                            // muddy on the tinted card.
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = category.categoryInk(),
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
            CurioForwardArrow(contentDescription = label, tint = category.categoryInk())
        }
    }
}

private fun CurioEntry.capturedAtDaysAgoLabel(): String = when (val days = capturedAtDaysAgo) {
    0 -> "today"
    1 -> "yesterday"
    else -> "${days}d ago"
}
