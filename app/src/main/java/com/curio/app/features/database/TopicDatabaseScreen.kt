package com.curio.app.features.database

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioTopic
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.openSilentExplore
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Browse Topics — the whole Curio database in one place.
 *
 * Opened from the Home drawer ("Browse Topics"). Renders every topic across
 * the ten real categories (wildcard is a merge, so it isn't a separate lane)
 * with a search bar, per-category filter chips, and a small "explored"
 * badge on topics already marked done. Tapping any topic opens its full
 * Topic Reveal page, exactly like spinning it.
 *
 * Sits in the settings family: torn rose hero on a muted watermark
 * backdrop, content scrolling under the ragged tear, ScreenEntrance
 * entrance animation.
 */
@Composable
fun TopicDatabaseScreen(navController: NavController) {
    // Hoisted (v8.13) — the silent Explore chip needs a Context, but
    // LocalContext.current is @Composable and cannot be called inside the
    // row's non-composable onExplore lambda.
    val context = LocalContext.current
    // v7.97 — SAVEABLE state: the search query, the selected category filter
    // and the scroll position survive leaving the screen (Topic Reveal
    // round-trips, tab switches, rotation, process death) instead of
    // resetting to a fresh blank list every time you come back.
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCat by rememberSaveable { mutableStateOf<CategoryId?>(null) }
    // v7.98 — the scroll position is saved EXPLICITLY (index + offset), not
    // via LazyListState.Saver: the catalog loads asynchronously, so on return
    // the list first composes with zero rows and a restored LazyListState gets
    // clamped to 0 before the topics arrive — scrolling back to the top.
    // Saving the raw numbers and scrolling again once rows exist restores the
    // exact spot reliably.
    var savedScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    // Reactive done-set — reading the value registers the dependency so the
    // list refreshes when the user marks a topic done (e.g. after returning
    // from a Topic Reveal) or a session records an exploration.
    val doneTopics = ExploreSessionStore.doneTopicsState

    // Load + cache every category pool once; carry the topics alongside so
    // the filtered list stays a pure derivation. v7.94 — keyed on the
    // Manage Categories state so hidden/reordered lanes refresh on revisit.
    val catalog by produceState<List<Pair<CurioCategory, List<CurioTopic>>>>(
        initialValue = emptyList(),
        AppPreferences.hiddenCategoriesState,
        AppPreferences.categoryOrderState
    ) {
        value = withContext(Dispatchers.Default) {
            TopicJsonLoader.preloadAll()
            CurioCategories.visible
                .filter { it.id != CategoryId.WILDCARD }
                .map { cat -> cat to TopicJsonLoader.cached(cat.id).orEmpty() }
        }
    }
    val totalTopics = catalog.sumOf { it.second.size }

    // v7.97 — the persisted filter can outlive its lane (a category hidden in
    // Manage Categories drops out of the catalog). Fall back to All instead
    // of leaving an invisible "no topics" state with no visible chip.
    val effectiveCat = remember(catalog, selectedCat) {
        if (selectedCat != null && catalog.none { it.first.id == selectedCat }) null else selectedCat
    }

    // Filtered rows — section headers while browsing All, topic rows always.
    // Keyed on the done-set snapshot (structural equality) so badges refresh.
    val needle = query.trim().lowercase()
    val rows = remember(catalog, effectiveCat, needle, doneTopics) {
        buildList {
            catalog.forEach { (cat, topics) ->
                if (effectiveCat != null && effectiveCat != cat.id) return@forEach
                val shown = if (needle.isEmpty()) topics else topics.filter { t ->
                    t.name.lowercase().contains(needle) ||
                        t.subtype.lowercase().contains(needle) ||
                        t.byline.lowercase().contains(needle) ||
                        t.teaser.lowercase().contains(needle) ||
                        t.tags.any { it.lowercase().contains(needle) }
                }
                if (shown.isEmpty()) return@forEach
                if (effectiveCat == null) {
                    add(DatabaseRow(key = "sec-${cat.id.name}", section = cat, sectionCount = shown.size))
                }
                shown.forEach { t ->
                    add(
                        DatabaseRow(
                            key = t.id,
                            topic = t,
                            done = "${cat.id.name}::$t.name" in doneTopics
                        )
                    )
                }
            }
        }
    }

    // ── Scroll restore + persist ─────────────────────────────────────
    // The catalog loads asynchronously (produceState), so the first frames
    // after returning compose an EMPTY list. Restoring a LazyListState there
    // clamps to 0 before the topics arrive. Instead: remember the exact
    // index+offset, scroll back once rows actually exist, and keep the
    // numbers fresh while the user scrolls (only when rows exist, so the
    // empty flash can't overwrite them).
    val hasRows = rows.isNotEmpty()
    LaunchedEffect(hasRows) {
        if (hasRows && (savedScrollIndex > 0 || savedScrollOffset > 0)) {
            listState.scrollToItem(savedScrollIndex, savedScrollOffset)
        }
    }
    LaunchedEffect(listState, hasRows) {
        if (!hasRows) return@LaunchedEffect
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            savedScrollIndex = index
            savedScrollOffset = offset
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Watermark backdrop — muted category glyphs (settings family).
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        // ── Scroll content — fills the screen, runs under the ragged tear.
        ScreenEntrance {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    top = SettingsHeroTotalHeight + 10.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ── Search + category filter chips ─────────────────────────
                item("controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    if (totalTopics > 0) "Search $totalTopics topics…"
                                    else "Search topics…",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                CurioIcon(
                                    CurioIcons.Search, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 20.dp
                                )
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    Surface(
                                        onClick = { query = "" },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            CurioIcon(
                                                CurioIcons.Close, null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                size = 16.dp
                                            )
                                        }
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item("all") {
                                DatabaseFilterChip(
                                    label = "All",
                                    count = totalTopics,
                                    selectedInk = MaterialTheme.colorScheme.onPrimaryContainer,
                                    accent = MaterialTheme.colorScheme.primary,
                                    tint = MaterialTheme.colorScheme.primaryContainer,
                                    selected = effectiveCat == null,
                                    onClick = { selectedCat = null }
                                )
                            }
                            items(catalog, key = { it.first.id.name }) { (cat, list) ->
                                DatabaseFilterChip(
                                    label = cat.displayName,
                                    count = list.size,
                                    selectedInk = cat.accent,
                                    accent = cat.accent,
                                    tint = cat.tint,
                                    selected = effectiveCat == cat.id,
                                    onClick = { selectedCat = cat.id }
                                )
                            }
                        }
                    }
                }

                // ── Loading / empty / list states ──────────────────────────
                if (catalog.isEmpty()) {
                    item("loading") {
                        Text(
                            "Loading topics…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (rows.isEmpty()) {
                    item("empty") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 56.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.SearchOff, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                size = 44.dp
                            )
                            Text(
                                "No topics match",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Try a different search or pick another category.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(rows, key = { it.key }) { row ->
                        when {
                            row.section != null -> DatabaseSectionHeader(
                                cat = row.section,
                                count = row.sectionCount
                            )
                            row.topic != null -> DatabaseTopicRow(
                                cat = CurioCategories.byId(row.topic.categoryId),
                                topic = row.topic,
                                done = row.done,
                                // v8.12/8.13 — a silent Explore chip: opens the
                                // topic's search page without quest chains,
                                // dailies, recents or done-marks; it still
                                // feeds the passport + awards the tiny
                                // exploration XP (browsing shouldn't inflate
                                // quest progress, but the pet knows you were
                                // there).
                                onExplore = { openSilentExplore(context, row.topic) },
                                onClick = {
                                    // Browse-Topics mode: the reveal opens
                                    // read-only (no explore, no recents
                                    // recording) and Back always returns here
                                    // with the scroll position restored.
                                    navController.navigate(
                                        CurioRoutes.revealForBrowse(
                                            row.topic.categoryId.routeSlug,
                                            row.topic.name
                                        )
                                    ) { launchSingleTop = true }
                                }
                            )
                        }
                    }
                }
            }
        }
        // ── Torn rose hero on top — rows disappear under the tear.
        SettingsHeroHeader(
            title = "Topic Database",
            subtitle = if (totalTopics > 0) "$totalTopics topics across ${catalog.size} lanes" else "Every topic, one place",
            onBack = { navController.popBackStack() }
        )
    }
}

/** One row in the database list — a category section header or a topic. */
private data class DatabaseRow(
    val key: String,
    val section: CurioCategory? = null,
    val sectionCount: Int = 0,
    val topic: CurioTopic? = null,
    val done: Boolean = false
)

/** Small category filter chip with a live count. */
@Composable
private fun DatabaseFilterChip(
    label: String,
    count: Int,
    selectedInk: Color,
    accent: Color = MaterialTheme.colorScheme.primary,
    tint: Color = MaterialTheme.colorScheme.primaryContainer,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) tint else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) null
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = if (count > 0) "$label $count" else label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) selectedInk else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/** Category section header shown while browsing All. */
@Composable
private fun DatabaseSectionHeader(cat: CurioCategory, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(cat.accent, CircleShape)
        )
        Text(
            text = cat.displayName.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

/** One tappable topic row — category glyph chip, name, meta, teaser, badge. */
@Composable
private fun DatabaseTopicRow(
    cat: CurioCategory,
    topic: CurioTopic,
    done: Boolean,
    onClick: () -> Unit,
    onExplore: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 9.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cat.accent.copy(alpha = 0.14f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        cat.iconGlyph, null,
                        tint = cat.accent,
                        size = 22.dp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = topic.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (done) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CurioColors.Sage.copy(alpha = 0.16f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                CurioIcon(
                                    CurioIcons.Check, null,
                                    tint = CurioColors.Sage,
                                    size = 12.dp
                                )
                                Text(
                                    "explored",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CurioColors.Sage
                                )
                            }
                        }
                    }
                }
                val meta = listOfNotNull(
                    topic.byline.takeIf { it.isNotBlank() },
                    topic.subtype.takeIf { it.isNotBlank() }
                )
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = topic.teaser,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // v8.12 — silent explore chip: open the topic's search page with
            // no tracking. Nested inside the clickable row, so its own tap
            // consumes the event instead of opening the read-only reveal.
            if (onExplore != null) {
                Surface(
                    onClick = onExplore,
                    shape = RoundedCornerShape(50),
                    color = cat.accent.copy(alpha = 0.14f),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        CurioIcon(
                            CurioIcons.AutoAwesome, null,
                            tint = cat.accent,
                            size = 14.dp
                        )
                        Text(
                            text = "Explore",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = cat.accent
                        )
                    }
                }
            }
            CurioIcon(
                CurioIcons.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                size = 20.dp
            )
        }
    }
}
