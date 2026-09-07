package com.curio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * v3xx — the BOOK BROWSER: every catalogued book as a scrollable,
 * line-by-line list (cover thumbnail + name + author/year + cached Google
 * rating) — moved out of the Book covers & ratings hub's horizontal strip
 * so the full catalogue is browsable without a sideways scroll. Tapping a
 * row opens the book's own reveal.
 */
@Composable
fun BookBrowserScreen(navController: NavController) {
    val context = LocalContext.current
    // TopicJsonLoader.load is suspend — load the count off the main thread.
    val books by produceState(initialValue = emptyList<CurioTopic>()) {
        value = runCatching { TopicJsonLoader.load(CategoryId.BOOKS) }.getOrDefault(emptyList())
    }
    val ratingCounts = AppPreferences.bookRatingsCountState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        // ── Header ─────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Surface(
                onClick = { navController.popBackStack() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                CurioIcon(
                    CurioIcons.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 20.dp,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Book browser",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Every book, line by line · covers, ratings and years",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (books.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Loading books…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(books, key = { it.name }) { book ->
                val rating = AppPreferences.bookRatingsState[book.name]
                BookBrowserRow(
                    book = book,
                    rating = rating,
                    count = ratingCounts[book.name] ?: 0,
                    onClick = {
                        navController.navigate(
                            com.curio.app.navigation.CurioRoutes.revealForBrowse(
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
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