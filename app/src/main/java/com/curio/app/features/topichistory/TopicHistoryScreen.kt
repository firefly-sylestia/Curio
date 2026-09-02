package com.curio.app.features.topichistory

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CategoryFamily
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.CurioTopic
import com.curio.app.data.PinnedTopic
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.formatSessionShort
// settingsRoseAccent/settingsReadableInk live in the settings package
// (SettingsHubScreen) — the same shared helpers the Onboarding/Cabinet heroes use.
import com.curio.app.features.settings.settingsReadableInk
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioCategoryChip
import com.curio.app.ui.components.CurioEmptyState
import com.curio.app.ui.components.CurioSearchField
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.SoftTornSheetShape
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.curioPillTintLift
import com.curio.app.ui.theme.curioRoseInk
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor

/**
 * Topic History — see Curio topic-history contract.
 *
 * Lists every topic the user has ever spun, grouped by day ("Today" /
 * "Yesterday" / "This week" / "Earlier" headers). Each row shows the topic
 * name + the category accent + a small format glyph + relative time.
 *
 * v8.4x redesign:
 *  - Header matches the shared push-screen style (ExtraBold title + a muted
 *    subtitle, like Recents/Profile).
 *  - A search field + category filter chips narrow the list live (the same
 *    search-field visual language as the Settings hub).
 *  - Day-group headers show their entry count, spacing is breathing-roomier,
 *    and an empty filter result gets its own "No matches — Clear filters"
 *    state.
 *
 * Backed by saved capture persistence so the history reflects real topics
 * the user has captured instead of placeholder samples.
 */
@Composable
fun TopicHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    // v6.7 — pinned-for-later topics from the Topic Reveal screen, listed
    // above the day-grouped capture history so the user can revisit them.
    val pinnedTopics = AppPreferences.pinnedTopicsState
    // v21 — favorited topics (Topic Reveal's favorite button),
    // resolved to catalog topics so the rows show real topic names. Reactive
    // off the sentiments state, so a vote flips the section instantly.
    // Topic names live in the JSON catalogs, so resolution is suspend:
    // produceState re-runs whenever the sentiments map is replaced.
    // Null until the first resolution completes, so the empty state never
    // flashes before the catalog lookups land.
    val sentimentsState = produceState<List<CurioTopic>?>(
        initialValue = null,
        AppPreferences.topicSentimentsState
    ) {
        value = resolveSentimentTopics(
            AppPreferences.topicSentimentsState,
            AppPreferences.SENTIMENT_LIKE
        )
    }
    val favoritedTopics = sentimentsState.value.orEmpty()
    val sentimentsLoaded = sentimentsState.value != null
    // v21 — unpin confirmation (mirrors Home's Saved-shelf dialog): the
    // bookmark button never drops a pin silently.
    var pendingUnpin by remember { mutableStateOf<PinnedTopic?>(null) }
    val entriesState = produceState<List<HistoryEntry>>(initialValue = emptyList()) {
        try {
            CurioRepositoryHolder.repo.observeAll().collect { savedEntries ->
                value = savedEntries.map { it.toHistoryEntry() }
            }
        } catch (_: Exception) {
            value = emptyList()
        }
    }
    val entries = entriesState.value

    // ── Search & filter (v8.4x) ─────────────────────────────────────────
    var query by rememberSaveable { mutableStateOf("") }
    var filterCategoryId by rememberSaveable { mutableStateOf<CategoryId?>(null) }
    val needle = query.trim()
    val hasFilter = needle.isNotEmpty() || filterCategoryId != null

    fun matches(categoryId: CategoryId, topicName: String): Boolean {
        val byCategory = filterCategoryId == null || filterCategoryId == categoryId
        val byQuery = needle.isEmpty() ||
            topicName.contains(needle, ignoreCase = true) ||
            // The search also matches the category's display name, so typing
            // "films" surfaces every film you've explored.
            CurioCategories.byId(categoryId).displayName.contains(needle, ignoreCase = true)
        return byCategory && byQuery
    }

    val filteredPinned = remember(pinnedTopics, needle, filterCategoryId) {
        pinnedTopics.filter { matches(it.categoryId, it.topicName) }
    }
    val filteredEntries = remember(entries, needle, filterCategoryId) {
        entries.filter { matches(it.categoryId, it.topicName) }
    }
    val filteredFavorited = remember(favoritedTopics, needle, filterCategoryId) {
        favoritedTopics.filter { matches(it.categoryId, it.name) }
    }
    val grouped = remember(filteredEntries) { filteredEntries.groupBy { it.dayLabel } }
    // Chips only for categories that actually appear in the history.
    val availableCats = remember(entries, pinnedTopics, favoritedTopics) {
        (entries.map { it.categoryId } + pinnedTopics.map { it.categoryId } +
            favoritedTopics.map { it.categoryId })
            .distinct()
            .map { CurioCategories.byId(it) }
    }
    // v5.8 — saveable-backed: the history list keeps its scroll position
    // across rotation and nav-away/back.
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Torn hero header — Home's construction (rose banner, bold tear,
        //    white under-sheet, mirrored watermark pairs) with History's own
        //    tear seed and its OWN BOOKS-family glyphs, so it reads as part
        //    of the torn-banner family without copying Home's wildcard scatter.
        HistoryHeroHeader(onBack = { navController.popBackStack() })

        if (sentimentsLoaded && entries.isEmpty() && pinnedTopics.isEmpty() &&
            favoritedTopics.isEmpty()
        ) {
            CurioEmptyState(
                glyph = CurioIcons.History,
                headline = "No shuffles yet",
                subtext = "Shuffle the deck and your picks will appear here, grouped by day.",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            return
        }

        // ── Search + category filter ────────────────────────────────────
        CurioSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Search your history",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
        )
        if (availableCats.size > 1) {
            HistoryCategoryFilterRow(
                categories = availableCats,
                selected = filterCategoryId,
                onSelect = { filterCategoryId = it },
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
            )
        }

        if (filteredEntries.isEmpty() && filteredPinned.isEmpty() &&
            filteredFavorited.isEmpty()
        ) {
            // Filters narrowed everything away — offer a one-tap reset.
            // (Same ScreenEntrance fade-up as the list, so the whole results
            // region shares one entrance language.)
            ScreenEntrance {
                val filterCatName = filterCategoryId?.let { CurioCategories.byId(it).displayName }
                CurioEmptyState(
                    glyph = CurioIcons.Search,
                    headline = "No matches",
                    subtext = when {
                        needle.isNotEmpty() && filterCatName != null ->
                            "Nothing in your $filterCatName history matches \"$needle\"."
                        needle.isNotEmpty() ->
                            "Nothing in your history matches \"$needle\"."
                        filterCatName != null ->
                            "No $filterCatName topics in your history yet."
                        else -> "Nothing in your history yet."
                    },
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    ctaLabel = "Clear filters",
                    onCtaClick = {
                        query = ""
                        filterCategoryId = null
                    }
                )
            }
        } else {
            ScreenEntrance {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = wideContentEdgePadding(),
                        vertical = 10.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ── Favorited topics — the Topic Reveal favorite button ──
                    if (filteredFavorited.isNotEmpty()) {
                        item(key = "favorited_header") {
                            HistorySectionHeader(
                                glyph = CurioIcons.Star,
                                label = "Favorite",
                                count = filteredFavorited.size,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(filteredFavorited, key = { "fav_${it.categoryId.name}_${it.id}" }) { topic ->
                            SentimentTopicRow(
                                topic = topic,
                                glyph = CurioIcons.Star,
                                onClick = {
                                    navController.navigate(
                                        com.curio.app.navigation.CurioRoutes.revealFor(
                                            topic.categoryId.routeSlug,
                                            topic.name
                                        )
                                    ) { launchSingleTop = true }
                                }
                            )
                        }
                    }

                    // ── Pinned for later (pin button on Topic Reveal) ────
                    if (filteredPinned.isNotEmpty()) {
                        item(key = "pinned_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CurioIcon(
                                    CurioIcons.Bookmark, null,
                                    tint = curioRoseInk(),
                                    size = 16.dp
                                )
                                Text(
                                    text = "Pinned for later",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(filteredPinned, key = { "pin_${it.categoryId.name}_${it.topicName}" }) { pinned ->
                            PinnedRow(
                                pinned = pinned,
                                onClick = {
                                    navController.navigate(
                                        com.curio.app.navigation.CurioRoutes.revealFor(
                                            pinned.categoryId.routeSlug,
                                            pinned.topicName
                                        )
                                    ) { launchSingleTop = true }
                                },
                                onUnpin = { pendingUnpin = pinned }
                            )
                        }
                        if (filteredEntries.isNotEmpty()) {
                            item(key = "pinned_divider") {
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }

                    grouped.forEach { (dayLabel: String, dayEntries: List<HistoryEntry>) ->
                        item(key = "header_$dayLabel") {
                            Text(
                                text = "$dayLabel · ${dayEntries.size}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(dayEntries, key = { historyEntry: HistoryEntry -> historyEntry.id }) { entry ->
                            HistoryRow(
                                entry = entry,
                                onClick = {
                                    navController.navigate(
                                        com.curio.app.navigation.CurioRoutes.revealFor(
                                            entry.categoryId.routeSlug,
                                            entry.topicName
                                        )
                                    ) { launchSingleTop = true }
                                }
                            )
                        }
                    }
                }
                // Side scroll indicator — thin overlay knob, grows on touch.
                CurioVerticalScrollIndicator(
                    state = listState.scrollIndicatorState,
                    onScrollBy = { listState.dispatchRawDelta(it) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                )
                }
            }
        }
    }

    // ── Unpin-topic confirmation — never drop a pin silently (mirrors
    // Home's Saved-shelf dialog): the bookmark button asks before removing.
    pendingUnpin?.let { pinned ->
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { pendingUnpin = null },
            title = { Text("Unpin ${pinned.topicName}?") },
            text = { Text("This removes ${pinned.topicName} from Pinned for later. The topic stays in the deck. You can pin it again anytime.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppPreferences.unpinTopic(context, pinned.categoryId, pinned.topicName)
                        pendingUnpin = null
                    },
                    colors = curioDialogActionButtonColors()
                ) { Text("Unpin") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnpin = null }, colors = curioDialogActionButtonColors()) { Text("Keep") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Torn hero header — Home's rose-banner construction (bold tear, white
// under-sheet, mirrored watermark collage) with History's OWN tear seed
// and its own BOOKS-family glyphs, so the screen reads as part of the
// torn-banner family without copying Home's wildcard scatter.
// ═══════════════════════════════════════════════════════════════════════

/** The torn banner's solid body height. */
private val HistoryHeroBannerHeight = 186.dp
/** Extra layout space for the white sheet below the torn banner. */
private val HistoryHeroSheetExtent = 24.dp
/** Total header footprint — banner plus its under-sheet extent. */
private val HistoryHeroTotalHeight = HistoryHeroBannerHeight + HistoryHeroSheetExtent
/** Fixed tear seed — History tears in its own pattern, never re-rolls. */
private const val HISTORY_TEAR_SEED = 0xAB1E5

/** One mirrored hero watermark pair (the settings/profile collage). */
private data class HistoryHeroPair(
    val biasX: Float,
    val biasY: Float,
    val size: Dp,
    val rotation: Float,
    val alpha: Float
)

@Composable
private fun HistoryHeroHeader(onBack: () -> Unit) {
    val heroTornShape = remember(HISTORY_TEAR_SEED) { SoftTornBottomShape(HISTORY_TEAR_SEED, bold = true) }
    val sheetShape = remember(HISTORY_TEAR_SEED) {
        SoftTornSheetShape(HISTORY_TEAR_SEED, lip = 10.dp, baseline = 14.dp, bold = true)
    }
    // The same rose-wood banner family as Home/Settings/Cabinet — theme
    // aware (Material primary, pure black in AMOLED, rose in pastel).
    val fill = settingsRoseAccent()
    val ink = settingsReadableInk(fill)
    // History's own watermark set: clock/history glyphs instead of a
    // category family.
    // v59.2 — the Topic History hero scatters clock/history glyphs that
    // match the screen instead of the books family.
    val heroSymbols = CurioIcons.historyHeroSymbols()
    val heroPairs = listOf(
        HistoryHeroPair(biasX = 0.93f, biasY = -0.85f, size = 44.dp, rotation = 12f, alpha = 0.11f),
        HistoryHeroPair(biasX = 0.55f, biasY = -0.64f, size = 48.dp, rotation = 8f, alpha = 0.13f),
        HistoryHeroPair(biasX = 0.94f, biasY = -0.12f, size = 56.dp, rotation = 14f, alpha = 0.14f),
        HistoryHeroPair(biasX = 0.56f, biasY = 0.54f, size = 50.dp, rotation = 10f, alpha = 0.13f),
        HistoryHeroPair(biasX = 0.94f, biasY = 0.80f, size = 44.dp, rotation = 6f, alpha = 0.11f)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HistoryHeroTotalHeight)
    ) {
        // ── White under-sheet — the tear's uneven lip reads white below
        // the opaque banner (shared paper layer in every theme).
        // v108 — OFF by default (Settings → Experiments → Paper & headers);
        // the toggle restores this extra paper layer.
        if (AppPreferences.heroTearSheetState) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .offset(y = HistoryHeroBannerHeight - 18.dp)
                .clip(sheetShape)
                // v81 — dark: a subtle lighter lip off the dark hero.
                .background(
                    if (isCurioDarkTheme()) lerp(fill, Color.White, 0.10f)
                    else CurioColors.CreamWhite
                )
        )
        }
        // ── Torn-edge shadow — hairline dark rim just below the seam.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HistoryHeroBannerHeight)
                .offset(y = 1.dp)
                .clip(heroTornShape)
                .background(Color.Black.copy(alpha = 0.20f))
        )
        // ── Solid rose banner, torn bottom edge.
        Surface(
            shape = heroTornShape,
            color = fill,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(HistoryHeroBannerHeight)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mirrored watermark collage — History's own glyphs around
                // the banner edges in mirrored pairs.
                heroPairs.forEachIndexed { i, pair ->
                    HistoryHeroSymbol(heroSymbols[i * 2], BiasAlignment(-pair.biasX, pair.biasY), pair.size, -pair.rotation, pair.alpha, ink)
                    HistoryHeroSymbol(heroSymbols[i * 2 + 1], BiasAlignment(pair.biasX, pair.biasY), pair.size, pair.rotation, pair.alpha, ink)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)
                ) {
                    // ── Top row — back pill (Cabinet style, ink-tinted) ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurioBackButton(
                            onClick = onBack,
                            // v76 — OPAQUE theme-aware pill, the same fill the
                            // settings-family hero pills wear: the old 18% ink
                            // glass read transparent; the opaque lerp of the
                            // banner fill toward the theme-aware lift keeps
                            // the same perceived tint with a clean shadow.
                            // v108 — dark: the filter-chip glass so the back
                            // pill matches the action-pill family at night.
                            containerColor = if (isCurioDarkTheme()) {
                                lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Color.Black, 0.15f)
                            } else {
                                lerp(fill, curioPillTintLift(), 0.38f)
                            },
                            contentColor = ink,
                            shadowElevation = 3.dp,
                            disableRipple = true
                        )
                    }
                    // Flex spacer — pins the title block just above the tear.
                    Spacer(Modifier.weight(1f))
                    // ── Title block — Cabinet's headline + subtitle ─────
                    Column {
                        Text(
                            "Topic History",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ink,
                            maxLines = 1
                        )
                        Text(
                            "Favorites & every spin you've explored",
                            style = MaterialTheme.typography.labelMedium,
                            color = ink.copy(alpha = 0.82f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** One mirrored hero watermark glyph — the banner's readable ink at a soft
 *  alpha (Home's HomeHeroSymbol construction, adapted for History). */
@Composable
private fun BoxScope.HistoryHeroSymbol(
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
            .padding(10.dp)
            .graphicsLayer { rotationZ = rotation }
    )
}

/** Horizontally scrolling category filter — single-select; tapping the
 *  active chip clears the filter. Only categories present in the history
 *  are offered (no dead chips). */
@Composable
private fun HistoryCategoryFilterRow(
    categories: List<CurioCategory>,
    selected: CategoryId?,
    onSelect: (CategoryId?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            CurioCategoryChip(
                category = cat,
                selected = selected == cat.id,
                onClick = { onSelect(if (selected == cat.id) null else cat.id) }
            )
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val cat = CurioCategories.byId(entry.categoryId)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Category accent dot ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(cat.tint, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = cat.iconGlyph,
                    contentDescription = null,
                    tint = cat.categoryInk(),
                    size = 22.dp
                )
            }

            Spacer(Modifier.size(12.dp))

            // ── Topic name + format glyph + category ──────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.topicName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = cat.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = cat.categoryInk()
                )
            }

            // ── Relative time + session duration + format glyph ───────────
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = entry.relativeTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // v22 — the explore-session duration rides the meta line when
                // one was recorded (Timer glyph + "12m"), beside the format
                // glyph, so each row shows how long the topic was explored.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (entry.sessionTimeMillis > 0L) {
                        CurioIcon(
                            name = CurioIcons.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 13.dp
                        )
                        Text(
                            text = formatSessionShort(entry.sessionTimeMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    CurioIcon(
                        name = entry.formatGlyph,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 14.dp
                    )
                }
            }
        }
    }
}


// ── Section header for the sentiment lists (Favorite) ─────────────

@Composable
private fun HistorySectionHeader(
    glyph: String,
    label: String,
    count: Int,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CurioIcon(glyph, null, tint = tint, size = 16.dp)
        Text(
            text = "$label · $count",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Sentiment row — thumb glyph + category accent dot (mirrors PinnedRow) ──

@Composable
private fun SentimentTopicRow(
    topic: CurioTopic,
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cat = CurioCategories.byId(topic.categoryId)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Category accent dot with the sentiment thumb ────────────
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(cat.tint, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = glyph,
                    contentDescription = null,
                    tint = cat.categoryInk(),
                    size = 20.dp
                )
            }

            Spacer(Modifier.size(12.dp))

            // ── Topic name + category ───────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = cat.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = cat.categoryInk()
                )
            }
        }
    }
}

// ── Sentiment resolution — sentiments are stored as "CATEGORY:topicId" ────
// keys; the topic names live in the JSON catalogs, so we load each involved
// category (cached after first reveal) and match by id. A key whose category
// part doesn't parse (e.g. a wildcard-spun topic) falls back to a scan of
// every already-cached pool so the row still shows a real topic name.

private suspend fun resolveSentimentTopics(
    sentiments: Map<String, String>,
    sentiment: String
): List<CurioTopic> {
    val wanted = sentiments.filterValues { it == sentiment }
    if (wanted.isEmpty()) return emptyList()

    val result = mutableListOf<CurioTopic>()
    // Key → "CATEGORY:topicId". We load per category to keep the catalog
    // lookups cheap (cached after the first reveal of that category).
    val byCategory = wanted.keys.groupBy { key -> key.substringBefore(':') }
    byCategory.forEach { (categoryName, keys) ->
        val categoryId = CategoryId.values().firstOrNull { it.name == categoryName }
        val topics: List<CurioTopic> = when {
            categoryId != null && categoryId != CategoryId.WILDCARD ->
                runCatching { TopicJsonLoader.load(categoryId) }.getOrDefault(emptyList())
            else -> {
                // Unknown / wildcard key — scan every already-cached pool.
                CategoryId.values()
                    .filter { it != CategoryId.WILDCARD }
                    .mapNotNull { TopicJsonLoader.cached(it) }
                    .flatten()
            }
        }
        keys.forEach { key ->
            val topicId = key.substringAfter(':')
            topics.firstOrNull { it.id == topicId }?.let { result += it }
        }
    }
    return result
}

// ── Pinned-for-later row — bookmark glyph + category accent dot ───────────

@Composable
private fun PinnedRow(pinned: PinnedTopic, onClick: () -> Unit, onUnpin: () -> Unit) {
    val cat = CurioCategories.byId(pinned.categoryId)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Category accent dot with filled bookmark ────────────────
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(cat.tint, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = CurioIcons.Bookmark,
                    contentDescription = null,
                    tint = cat.categoryInk(),
                    size = 20.dp
                )
            }

            Spacer(Modifier.size(12.dp))

            // ── Topic name + category ───────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pinned.topicName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = cat.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = cat.categoryInk()
                )
            }

            // ── Unpin affordance ────────────────────────────────────────
            Surface(
                onClick = onUnpin,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                CurioIcon(
                    CurioIcons.BookmarkBorder, "Unpin",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 16.dp,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}


// ── Mock data for the placeholder phase ────────────────────────────────────
private data class HistoryEntry(
    val id: String,
    val topicName: String,
    val categoryId: CategoryId,
    val relativeTime: String,
    val dayLabel: String,
    val formatGlyph: String,
    // v22 — the saved capture's explore-session duration (0 = none recorded).
    val sessionTimeMillis: Long = 0L
)

private fun CurioEntry.toHistoryEntry(): HistoryEntry = HistoryEntry(
    id = id,
    topicName = topic.name,
    categoryId = topic.categoryId,
    relativeTime = relativeTime(capturedAtMillis),
    dayLabel = dayLabel(capturedAtMillis),
    formatGlyph = format.toGlyph(),
    sessionTimeMillis = sessionTimeMillis
)

private fun CaptureFormat.toGlyph(): String = when (this) {
    CaptureFormat.SoundBite -> CurioIcons.Mic
    CaptureFormat.ReelNotes -> CurioIcons.Movie
    CaptureFormat.Marginalia -> CurioIcons.MenuBook
    CaptureFormat.GalleryWall -> CurioIcons.Image
    CaptureFormat.FieldNotes -> CurioIcons.Science
    CaptureFormat.OpenNotebook -> CurioIcons.Edit
}

private fun dayLabel(timestamp: Long): String {
    val elapsed = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val days = elapsed / (24L * 60L * 60L * 1000L)
    return when (days) {
        0L -> "Today"
        1L -> "Yesterday"
        in 2L..6L -> "This week"
        else -> "Earlier"
    }
}

private fun relativeTime(timestamp: Long): String {
    val elapsedMinutes = ((System.currentTimeMillis() - timestamp).coerceAtLeast(0L) / 60_000L)
    return when {
        elapsedMinutes < 1L -> "just now"
        elapsedMinutes < 60L -> "${elapsedMinutes} min ago"
        elapsedMinutes < 24L * 60L -> "${elapsedMinutes / 60L} hr ago"
        elapsedMinutes < 48L * 60L -> "yesterday"
        else -> "${elapsedMinutes / (24L * 60L)} days ago"
    }
}
