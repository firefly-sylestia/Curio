package com.curio.app.features.personal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily

/**
 * v387 — THE PERSONAL FAMILY ON HOME.
 *
 * Two pieces live here, both belonging to Home rather than to the journals:
 *
 *  1. [PersonalCreateLauncher] — the floating "+" that opens the writing
 *     sheet. It is a fixed-size accent disc that slips away while the page is
 *     being scrolled DOWN and comes back the moment the finger goes up (or
 *     the page reaches the top), so a long read is never covered by a button
 *     nobody asked for mid-scroll.
 *  2. [PersonalChipsRow] — the journals and books as small, fixed-shape
 *     chips in a horizontal row, each one opening its own page directly.
 *     They wear the app's own accent tokens (never the capture paper's
 *     creams), so the personal writing reads as part of Home's furniture
 *     rather than as a saved capture.
 */

/** The fixed chip geometry — the chips never grow, so the row is stable
 *  whatever the writing contains. */
private val CHIP_WIDTH = 96.dp
private val CHIP_HEIGHT = 118.dp

/**
 * The floating create button. [visible] is owned by the caller (the page's
 * scroll direction), the tap is the sheet's.
 */
@Composable
fun PersonalCreateLauncher(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = personalAccent()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(tween(220), initialScale = 0.82f),
        exit = fadeOut(tween(150)) + scaleOut(tween(160), targetScale = 0.82f),
        modifier = modifier
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = accent,
            shadowElevation = 10.dp,
            modifier = Modifier.size(56.dp)
        ) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                CurioIcon(
                    CurioIcons.Add,
                    "Write something",
                    tint = personalOnAccent(),
                    size = 26.dp
                )
            }
        }
    }
}

/**
 * What the "+" opens: the four things a member can start writing here — a
 * journal page for a day, a book they are reading with somewhere to put the
 * chapters, a note about one topic, and a to-do list. Each door opens the
 * page's OWN screen (see [personalRouteFor]); none of them is a journal day
 * wearing route params.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEntrySheet(
    onDismiss: () -> Unit,
    onJournal: () -> Unit,
    onBook: () -> Unit,
    onTopicNote: () -> Unit,
    onTodoList: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Start writing",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            CreateEntryOption(
                glyph = CurioIcons.Note,
                title = "A journal page",
                body = "Today, how it felt, what you want to keep",
                accent = personalAccent(),
                onClick = onJournal
            )
            CreateEntryOption(
                glyph = CurioIcons.MenuBook,
                title = "A book",
                body = "Pick a book, review it chapter by chapter",
                accent = personalAccent(),
                onClick = onBook
            )
            CreateEntryOption(
                glyph = CurioIcons.TravelExplore,
                title = "A note on a topic",
                body = "Write about something you are exploring",
                accent = personalAccent(),
                onClick = onTopicNote
            )
            CreateEntryOption(
                glyph = CurioIcons.TaskAlt,
                title = "A to-do list",
                body = "Check off tasks as you go",
                accent = personalAccent(),
                onClick = onTodoList
            )
        }
    }
}

@Composable
private fun CreateEntryOption(
    glyph: String,
    title: String,
    body: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    // The glyph tone is NOT the raw accent: in light mode the accent is too
    // pale to read on its own wash, so the icon takes the deeper hero ink.
    val glyphTint = personalIconTint(accent)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.24f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    CurioIcon(glyph, null, tint = glyphTint, size = 20.dp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ink
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.copy(alpha = 0.6f)
                )
            }
            CurioIcon(CurioIcons.ChevronRight, null, tint = ink.copy(alpha = 0.4f), size = 18.dp)
        }
    }
}

/**
 * The Home row: what the member is writing, as chips. The first chip is
 * always the way in ("New"), then the newest journals, then the books being
 * read, then a door to the full journal list — a fixed-height row that never
 * reflows as the library grows.
 */
@Composable
fun PersonalChipsRow(
    navController: NavController,
    onWrite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val journals by produceState(initialValue = emptyList<PersonalNoteEntity>()) {
        runCatching {
            PersonalRepositoryHolder.repo.observeJournals().collect { value = it }
        }
    }
    val books by produceState(initialValue = emptyList<PersonalBookEntity>()) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBooks().collect { value = it }
        }
    }
    val ink = MaterialTheme.colorScheme.onBackground

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item("all-journals") {
                DoorChip(
                    glyph = CurioIcons.Note,
                    label = "Pages",
                    caption = "All journals",
                    onClick = { navController.navigate(CurioRoutes.JOURNALS) { launchSingleTop = true } }
                )
            }
            items(items = journals.take(3), key = { it.id }) { journal ->
                JournalChip(journal = journal, onClick = {
                    // v389 — the chip opens the page's OWN screen (a to-do list is
                    // not a journal day; see personalRouteFor).
                    navController.navigate(personalRouteFor(journal)) { launchSingleTop = true }
                })
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item("all-books") {
                DoorChip(
                    glyph = CurioIcons.MenuBook,
                    label = "My shelf",
                    caption = "Books",
                    onClick = { navController.navigate(CurioRoutes.BOOKS) { launchSingleTop = true } }
                )
            }
            items(items = books.take(3), key = { it.id }) { book ->
                BookChip(book = book, onClick = {
                    navController.navigate(CurioRoutes.bookDetail(book.id)) { launchSingleTop = true }
                })
            }
        }
    }
}

@Composable
private fun NewChip(onClick: () -> Unit) {
    val accent = personalAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = accent.copy(alpha = 0.12f),
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = accent, modifier = Modifier.size(34.dp)) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    CurioIcon(
                        CurioIcons.Add,
                        null,
                        tint = personalOnAccent(),
                        size = 19.dp
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                "New",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = accent
            )
        }
    }
}

/** A journal as a chip: the day, how it felt, and what it is called. */
@Composable
private fun JournalChip(
    journal: PersonalNoteEntity,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    val mood = journal.moodEnum
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    journal.dateMillis.toLocalDate().dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = accent
                )
                Spacer(Modifier.width(5.dp))
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
                        size = 14.dp
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                journal.title.ifBlank { journal.preview.ifBlank { "Untitled day" } },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A book as a chip: its cover, its name, where the member is in it. */
@Composable
private fun BookChip(
    book: PersonalBookEntity,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            BookCover(
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                corner = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.height(7.dp))
            Text(
                book.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

/** A door chip: the row's quiet "there is more" (and where an empty library
 *  still finds its list). */
@Composable
private fun DoorChip(
    glyph: String,
    label: String,
    caption: String,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurioIcon(glyph, null, tint = ink.copy(alpha = 0.62f), size = 22.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ink
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.5f)
            )
        }
    }
}
