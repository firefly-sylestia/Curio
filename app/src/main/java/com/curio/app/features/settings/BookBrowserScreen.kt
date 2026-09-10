package com.curio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioTopic
import com.curio.app.data.TopicJsonLoader
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioDoodleEmptyState
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.components.isLiquidGlassPillsActive
import com.curio.app.ui.components.liquidGlassCapsule
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.isCurioDarkTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * v3xx — the BOOK BROWSER: every catalogued book as a scrollable,
 * line-by-line list (cover thumbnail + name + author/year + cached Google
 * rating) — moved out of the Book covers & ratings hub's horizontal strip
 * so the full catalogue is browsable without a sideways scroll. Tapping a
 * row opens the book's own reveal.
 *
 * v3xx2 — UI consistency pass: the plain surface shell was replaced with
 * the settings-family torn-rose hero (sticky on phones, list-first on
 * wide windows), real liquid-glass pills and a name search in the hero —
 * the same chrome Recents / Manage Categories / Topic Database wear.
 */
@Composable
fun BookBrowserScreen(navController: NavController) {
    val context = LocalContext.current
    // TopicJsonLoader.load is suspend — load the count off the main thread.
    val books by produceState(initialValue = emptyList<CurioTopic>()) {
        value = runCatching { TopicJsonLoader.load(CategoryId.BOOKS) }.getOrDefault(emptyList())
    }
    val ratingCounts = AppPreferences.bookRatingsCountState
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()
    val wide = windowWidthSizeClass().isWide

    // ── Hero search — filters the list by name / author.
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchActive) {
        if (searchActive) searchFocus.requestFocus()
    }
    val shownBooks = remember(books, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) books
        else books.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.byline.contains(q, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(heroPageBackground())
    ) {
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!wide) {
            CurioWatermarkBackdrop(
                activeCat = com.curio.app.data.CurioCategories.byId(CategoryId.WILDCARD),
                modifier = Modifier.fillMaxSize()
            )
        }

        ScreenEntrance {
            if (books.isEmpty()) {
                Column {
                    SettingsHeroHeader(
                        title = "Book browser",
                        subtitle = "Every book, line by line · covers, ratings and years",
                        onBack = { navController.popBackStack() }
                    )
                    // v3xx — the shared settings nav rail (drill-in page —
                    // no chip highlighted).
                    SettingsNavRail(
                        active = null,
                        onSelect = { navigateToSettingsSection(navController, it) },
                        navController = navController,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Loading books…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = wideContentEdgePadding(),
                        end = wideContentEdgePadding(),
                        top = if (wide) 0.dp else SettingsHeroTotalHeight + (if (searchActive) 56.dp else 0.dp),
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (wide) {
                        item(key = "hero", contentType = "hero") {
                            SettingsHeroHeader(
                                title = "Book browser",
                                subtitle = "Every book, line by line · covers, ratings and years",
                                onBack = { navController.popBackStack() },
                                trailing = { ink -> BookBrowserHeroPills(ink = ink, glassBackdrop = glassBackdrop, onSearch = { searchActive = true }) },
                                searchActive = searchActive,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onCloseSearch = { searchActive = false; searchQuery = "" },
                                searchFocus = searchFocus,
                                searchPlaceholder = "Search books…"
                            )
                        }
                    }
                    // v3xx — the shared settings nav rail (drill-in page —
                    // no chip highlighted).
                    item(key = "settings-nav", contentType = "settings-nav") {
                        SettingsNavRail(
                            active = null,
                            onSelect = { navigateToSettingsSection(navController, it) },
                            navController = navController
                        )
                    }
                    if (shownBooks.isEmpty()) {
                        item(key = "empty", contentType = "empty") {
                            // v3xx — the app-wide doodle empty state.
                            CurioDoodleEmptyState(
                                headline = "No books match",
                                subtext = "Try a different title or author.",
                                ctaLabel = "Clear search",
                                onCtaClick = { searchQuery = ""; searchActive = false }
                            )
                        }
                    } else {
                        items(shownBooks, key = { it.name }) { book ->
                            val rating = AppPreferences.bookRatingsState[book.name]
                            BookBrowserRow(
                                book = book,
                                rating = rating,
                                count = ratingCounts[book.name] ?: 0,
                                onClick = {
                                    navController.navigate(
                                        CurioRoutes.revealForBrowse(
                                            CategoryId.BOOKS.routeSlug,
                                            book.name
                                        )
                                    ) { launchSingleTop = true }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (shownBooks.size > 4) {
            CurioVerticalScrollIndicator(
                state = listState.scrollIndicatorState,
                onScrollBy = { listState.dispatchRawDelta(it) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 10.dp, bottom = 16.dp)
            )
        }

        // STICKY HERO — phone only; wide scrolls the hero as the list's
        // first item instead.
        if (!wide && books.isNotEmpty()) {
            SettingsHeroHeader(
                title = "Book browser",
                subtitle = "Every book, line by line · covers, ratings and years",
                onBack = { navController.popBackStack() },
                trailing = { ink -> BookBrowserHeroPills(ink = ink, glassBackdrop = glassBackdrop, onSearch = { searchActive = true }) },
                searchActive = searchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onCloseSearch = { searchActive = false; searchQuery = "" },
                searchFocus = searchFocus,
                searchPlaceholder = "Search books…",
                glassBackdrop = glassBackdrop
            )
        }
    }
}

/** The hero's action pills — a search pill riding beside the back pill. */
@Composable
private fun BookBrowserHeroPills(
    ink: Color,
    glassBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop?,
    onSearch: () -> Unit
) {
    val glassMod = if (isLiquidGlassPillsActive() && glassBackdrop != null)
        Modifier.liquidGlassCapsule(
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            backdrop = glassBackdrop
        )
    else Modifier
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onSearch,
            shape = CircleShape,
            color = androidx.compose.ui.graphics.lerp(
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                Color.White,
                if (isCurioDarkTheme()) 0.12f else 0.25f
            ),
            modifier = glassMod
        ) {
            CurioIcon(
                CurioIcons.Search, "Search books",
                tint = ink,
                size = 18.dp,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

/** One book row: cover thumbnail + name / author / year + cached star
 *  rating, tapping opens the book's reveal. */
@Composable
private fun BookBrowserRow(
    book: CurioTopic,
    rating: Double?,
    count: Int,
    onClick: () -> Unit
) {
    val year = book.name
        .substringAfterLast("(", "")
        .substringBeforeLast(")", "")
        .takeIf { it.all { c -> c.isDigit() } && it.length == 4 }
    // v3xx — the settings design language: frosted card rows (matching the
    // settings sub-pages) instead of the flat surface rows.
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isCurioDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
                else Color.White.copy(alpha = 0.68f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Cover thumbnail (renders from the disk cache once fetched).
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                val cover = BookCoverFetch.coverCandidates(book.name, book.imageUrl).firstOrNull()
                if (cover != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(cover)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover of ${book.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val byline = book.byline.ifBlank { null }
                if (byline != null || year != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        listOfNotNull(byline, year).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (rating != null && rating > 0.0) {
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        CurioIcon(
                            CurioIcons.Star, null,
                            tint = Color(0xFFF6B23B),
                            size = 12.dp
                        )
                        Text(
                            String.format("%.1f", rating) + if (count > 0) " · ${count}" else "",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            CurioIcon(
                CurioIcons.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                size = 18.dp
            )
        }
    }
}