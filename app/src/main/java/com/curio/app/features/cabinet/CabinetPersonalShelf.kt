package com.curio.app.features.cabinet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.features.personal.BookCover
import com.curio.app.features.personal.personalMoodGlyph
import com.curio.app.features.personal.toLocalDate
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily

/**
 * v387 — THE CABINET'S **PERSONAL** SHELF.
 *
 * The Personal shelf holds the member's own writing, and it is the ONE shelf
 * in the Cabinet that is not made of captures: journals (a page per day) and
 * books (covers with the progress under them). Both keep their OWN small
 * view here — the same tiles Home's chips row uses, at grid size — and each
 * opens the page it belongs to (the journal editor, the book page).
 *
 * The shelf's saved members are NOT replaced: when the Personal collection
 * still holds saved entries, a door tile at the foot opens the collection's
 * own grid, so nothing that was there before loses its way in.
 */
internal const val SHELF_LEVEL_PERSONAL = "shelf:personal-writing"

/**
 * Emits the Personal shelf's grid content. [searchQuery] is the Cabinet
 * hero's field, so a search here filters the writing exactly like it filters
 * a shelf of captures.
 */
internal fun LazyGridScope.v2PersonalWritingItems(
    journals: List<PersonalNoteEntity>,
    books: List<PersonalBookEntity>,
    searchQuery: String,
    savedMemberCount: Int,
    onOpenJournal: (String) -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenAllJournals: () -> Unit,
    onOpenShelf: () -> Unit,
    onOpenSavedMembers: () -> Unit
) {
    val needle = searchQuery.trim().lowercase()
    fun journalMatches(journal: PersonalNoteEntity): Boolean =
        needle.isEmpty() ||
            journal.title.lowercase().contains(needle) ||
            journal.preview.lowercase().contains(needle)
    fun bookMatches(book: PersonalBookEntity): Boolean =
        needle.isEmpty() ||
            book.title.lowercase().contains(needle) ||
            book.author.lowercase().contains(needle)

    val shownJournals = journals.filter(::journalMatches)
    val shownBooks = books.filter(::bookMatches)

    if (shownJournals.isEmpty() && shownBooks.isEmpty()) {
        item(key = "personal-empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
            PersonalShelfEmpty(
                hasAny = journals.isNotEmpty() || books.isNotEmpty(),
                onOpenAllJournals = onOpenAllJournals,
                onOpenShelf = onOpenShelf
            )
        }
        if (savedMemberCount > 0) {
            item(key = "personal-saved-door", span = { GridItemSpan(maxLineSpan) }) {
                PersonalShelfDoor(
                    glyph = CurioIcons.Bookmark,
                    title = "Saved in Personal",
                    caption = "$savedMemberCount saved " +
                        if (savedMemberCount == 1) "item" else "items",
                    onClick = onOpenSavedMembers
                )
            }
        }
        return
    }

    if (shownJournals.isNotEmpty()) {
        item(key = "personal-journals-head", span = { GridItemSpan(maxLineSpan) }, contentType = "head") {
            PersonalShelfHeading(
                title = "Journals",
                caption = if (journals.size == 1) "1 page" else "${journals.size} pages",
                onClick = onOpenAllJournals
            )
        }
        shownJournals.forEach { journal ->
            item(key = "personal-journal-${journal.id}", contentType = "journal") {
                JournalShelfTile(journal = journal, onClick = { onOpenJournal(journal.id) })
            }
        }
    }

    if (shownBooks.isNotEmpty()) {
        item(key = "personal-books-head", span = { GridItemSpan(maxLineSpan) }, contentType = "head") {
            PersonalShelfHeading(
                title = "Books",
                caption = if (books.size == 1) "1 book" else "${books.size} books",
                onClick = onOpenShelf
            )
        }
        shownBooks.forEach { book ->
            item(key = "personal-book-${book.id}", contentType = "book") {
                BookShelfTile(book = book, onClick = { onOpenBook(book.id) })
            }
        }
    }

    if (savedMemberCount > 0 || shownJournals.size < journals.size || shownBooks.size < books.size) {
        item(key = "personal-saved-door", span = { GridItemSpan(maxLineSpan) }) {
            PersonalShelfDoor(
                glyph = CurioIcons.Bookmark,
                title = if (savedMemberCount > 0) "Saved in Personal"
                else "Clear the search to see all",
                caption = if (savedMemberCount > 0)
                    "$savedMemberCount saved ${if (savedMemberCount == 1) "item" else "items"}"
                else "Filtered by your search",
                onClick = if (savedMemberCount > 0) onOpenSavedMembers else onOpenAllJournals
            )
        }
    }
}

@Composable
private fun PersonalShelfHeading(
    title: String,
    caption: String,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = ink
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                caption,
                style = MaterialTheme.typography.labelMedium,
                color = ink.copy(alpha = 0.55f)
            )
            Spacer(Modifier.width(4.dp))
            CurioIcon(
                CurioIcons.ChevronRight,
                null,
                tint = ink.copy(alpha = 0.45f),
                size = 16.dp
            )
        }
    }
}

/** A journal day as a grid tile (the Cabinet's own small view of it). */
@Composable
private fun JournalShelfTile(journal: PersonalNoteEntity, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val mood = journal.moodEnum
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    journal.dateMillis.toLocalDate().dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = accent
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    journal.dateMillis.toLocalDate()
                        .month.name.lowercase().take(3)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.55f)
                )
                Spacer(Modifier.weight(1f))
                if (mood != null) {
                    CurioIcon(
                        personalMoodGlyph(mood),
                        mood.label,
                        tint = ink.copy(alpha = 0.5f),
                        size = 15.dp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                journal.title.ifBlank { "Untitled day" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (journal.title.isBlank()) ink.copy(alpha = 0.5f) else ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (journal.preview.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    journal.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** A book as a grid tile: cover, name, progress. */
@Composable
private fun BookShelfTile(book: PersonalBookEntity, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        BookCover(
            title = book.title,
            author = book.author,
            coverUrl = book.coverUrl,
            corner = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.66f)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.height(8.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { book.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50)),
            color = accent,
            trackColor = accent.copy(alpha = 0.16f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                book.isFinished -> "Finished"
                book.totalChapters <= 0 -> "Chapters not set"
                book.currentChapter <= 0 -> "${book.totalChapters} chapters"
                else -> "Ch ${book.currentChapter} of ${book.totalChapters}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = ink.copy(alpha = 0.6f)
        )
    }
}

/** The empty shelf, in the Cabinet's own voice (two doors, no hint text). */
@Composable
private fun PersonalShelfEmpty(
    hasAny: Boolean,
    onOpenAllJournals: () -> Unit,
    onOpenShelf: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                if (hasAny) "Nothing matches that search" else "Your own writing lives here",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = ink
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PersonalShelfPill(
                    glyph = CurioIcons.Note,
                    label = "Journals",
                    onClick = onOpenAllJournals
                )
                PersonalShelfPill(
                    glyph = CurioIcons.MenuBook,
                    label = "My shelf",
                    onClick = onOpenShelf
                )
            }
        }
    }
}

@Composable
private fun PersonalShelfDoor(
    glyph: String,
    title: String,
    caption: String,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(glyph, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ink
                )
                Text(
                    caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.55f)
                )
            }
            CurioIcon(CurioIcons.ChevronRight, null, tint = ink.copy(alpha = 0.4f), size = 18.dp)
        }
    }
}

@Composable
private fun PersonalShelfPill(glyph: String, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(glyph, null, tint = MaterialTheme.colorScheme.primary, size = 16.dp)
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
