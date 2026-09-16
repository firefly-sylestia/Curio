package com.curio.app.features.personal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.PersonalNoteEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.rememberCurioPressSource
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v387 — THE JOURNAL COLLECTION.
 *
 * Every page the member has written, newest day first, in the journal's own
 * small view: the day, how it felt, the title they gave it and the first line
 * of the writing. It is deliberately NOT the saved-entry list — no topic, no
 * format badge, no capture card.
 */
@Composable
fun JournalListScreen(navController: NavController) {
    val journals by produceState(initialValue = emptyList<PersonalNoteEntity>()) {
        runCatching {
            PersonalRepositoryHolder.repo.observeJournals().collect { value = it }
        }
    }
    var pendingDelete by remember { mutableStateOf<PersonalNoteEntity?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
        PersonalHeader(
            title = "Journals",
            subtitle = when (journals.size) {
                0 -> "A page for a day"
                1 -> "1 page"
                else -> "${journals.size} pages"
            },
            onBack = { navController.popBackStack() },
            action = {
                PersonalHeaderAction(
                    glyph = CurioIcons.Add,
                    label = "Write today",
                    onClick = {
                        navController.navigate(
                            CurioRoutes.journalEditor(CurioRoutes.PERSONAL_NEW)
                        ) { launchSingleTop = true }
                    }
                )
            }
        )

        if (journals.isEmpty()) {
            PersonalEmptyCard(
                glyph = CurioIcons.Note,
                title = "Nothing written yet",
                body = "A journal page is a day you decided to keep: how it felt, " +
                    "what happened, what you want to remember.",
                actionLabel = "Write today's page",
                onAction = {
                    navController.navigate(
                        CurioRoutes.journalEditor(CurioRoutes.PERSONAL_NEW)
                    ) { launchSingleTop = true }
                }
            )
        } else {
            // Grouped by MONTH: a journal is a run of days, and a month
            // heading turns a flat list of entries into a book's chapters.
            val months = journals.groupBy { journal ->
                journal.dateMillis.toLocalDate().withDayOfMonth(1)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                months.forEach { (month, pages) ->
                    item(key = "month-$month") { MonthHead(month = month) }
                    items(items = pages, key = { it.id }) { journal ->
                        JournalRow(
                            journal = journal,
                            onClick = {
                                navController.navigate(CurioRoutes.journalEditor(journal.id)) {
                                    launchSingleTop = true
                                }
                            },
                            onLongPress = { pendingDelete = journal }
                        )
                    }
                }
                item("tail") { Spacer(Modifier.height(60.dp)) }
            }
        }
        PersonalCreateLauncher(
            visible = true,
            onClick = {
                navController.navigate(CurioRoutes.journalEditor(CurioRoutes.PERSONAL_NEW)) {
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 18.dp)
        )
    }

    pendingDelete?.let { journal ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove this page?") },
            text = {
                Text(
                    "The ${journal.dateMillis.prettyDate()} entry and everything written " +
                        "in it leaves your journals. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = journal.id
                    pendingDelete = null
                    scope.launch { withContext(Dispatchers.IO) { runCatching { PersonalRepositoryHolder.repo.deleteNote(id) } } }
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Keep") }
            }
        )
    }
}

/** A month heading — the serif month with the year behind it, and a hairline
 *  running to the edge so the list reads as one bound volume. */
@Composable
private fun MonthHead(month: java.time.LocalDate) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            month.month.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = personalIconTint(personalAccent())
        )
        Text(
            month.year.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ink.copy(alpha = 0.4f)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(ink.copy(alpha = 0.08f))
        )
    }
}

/** One journal in the collection: the day, the mood, the title, the opening
 *  line and how much is on the page. Tap opens it, a long press offers the
 *  removal — the same habits as every other Curio list. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JournalRow(
    journal: PersonalNoteEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    val mood = journal.moodEnum
    // Decoding a body is only for the row that is actually measuring words:
    // `doc` re-parses the stored JSON on every access, so it is read ONCE per
    // version of the entry rather than on every recomposition of the list.
    val words = remember(journal.id, journal.updatedAtMillis) { journal.doc.wordsLabel() }
    val press = rememberCurioPressSource(pressedScale = 0.985f)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .then(press.modifier)
            .combinedClickable(
                interactionSource = press.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                // The margin rule: every page in the collection wears the same
                // accent spine, so the list reads as one notebook.
                .drawBehind {
                    val barWidth = 3.dp.toPx()
                    drawRoundRect(
                        color = accent,
                        size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                    )
                }
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(min = 54.dp)
            ) {
                Text(
                    journal.dateMillis.toLocalDate().dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = personalAccentInk()
                )
                Text(
                    journal.dateMillis.toLocalDate()
                        .month.name.lowercase().take(3)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.55f)
                )
                if (mood != null) {
                    Spacer(Modifier.height(4.dp))
                    CurioIcon(
                        personalMoodGlyph(mood),
                        mood.label,
                        tint = ink.copy(alpha = 0.6f),
                        size = 15.dp
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    journal.title.ifBlank { "Untitled day" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (journal.title.isBlank()) ink.copy(alpha = 0.5f) else ink
                )
                if (journal.preview.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        journal.preview,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LoraFontFamily),
                        color = ink.copy(alpha = 0.68f),
                        maxLines = 2
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    words,
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.45f)
                )
            }
        }
    }
}

/**
 * The personal family's header — shared by journals, the shelf and a book,
 * so the whole system wears one identity that is nobody else's: the page
 * background, a serif title, one pill action.
 */
@Composable
internal fun PersonalHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    action: @Composable (() -> Unit)? = null
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(42.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CurioIcon(CurioIcons.ArrowBack, "Back", tint = ink, size = 20.dp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = ink
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = ink.copy(alpha = 0.5f)
            )
        }
        action?.invoke()
    }
}

@Composable
internal fun PersonalHeaderAction(
    glyph: String,
    label: String,
    onClick: () -> Unit
) {
    val accent = personalAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = accent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(glyph, null, tint = personalOnAccent(), size = 17.dp)
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = personalOnAccent()
            )
        }
    }
}

@Composable
internal fun PersonalEmptyCard(
    glyph: String,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = personalAccent().copy(alpha = 0.24f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CurioIcon(glyph, null, tint = personalIconTint(personalAccent()), size = 24.dp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = ink
            )
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LoraFontFamily, fontSize = 14.sp),
                color = ink.copy(alpha = 0.66f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            PersonalHeaderAction(glyph = CurioIcons.Add, label = actionLabel, onClick = onAction)
        }
    }
}
