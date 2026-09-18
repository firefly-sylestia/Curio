package com.curio.app.features.personal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
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
    // ── v389e — DAYS ONLY ─────────────────────────────────────────────
    //
    // The personal store keeps journals, notes on topics and to-do lists in one
    // table, and this list used to show all three — a to-do list sitting among
    // the days it is not, and a note about a topic in a list about a day's
    // feelings. The list is the JOURNAL's: the pages that are neither live in the
    // Notes collection in the Cabinet (user request: "the journal in personal
    // keep sthe journals only … also todo goes inside notes too, no more in
    // journa").
    val pages by produceState(initialValue = emptyList<PersonalNoteEntity>()) {
        runCatching {
            PersonalRepositoryHolder.repo.observeJournals().collect { value = it }
        }
    }
    val journals = remember(pages) {
        pages.filter { !it.isTodo && !it.hasTopic }
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
            // v389 — no "Write today" pill: the floating + already writes today's
            // page, and a second door in the head only crowded the title. The
            // head wears TODAY instead, which is the one date a journal is
            // about (user request).
            action = { PersonalHeaderDate() }
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
                            // v389 — a row opens ITS OWN page: a journal day the
                            // editor, a to-do list the checklist page, a note on a
                            // topic the topic page. See personalRouteFor.
                            onClick = {
                                navController.navigate(personalRouteFor(journal)) {
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
    val accentInk = personalAccentInk()
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
            color = accentInk,
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
                // v389e — the day is the page's own ink, not the accent's lighter
                // shade: it is the one figure a row exists to say (user report:
                // "for journal number date its too accent color and very light
                // colored so fix it by making it dark").
                Text(
                    journal.dateMillis.toLocalDate().dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink
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
                    color = if (journal.title.isBlank()) ink.copy(alpha = 0.5f) else ink,
                    // Two lines at most: a long title turned a row into a wall
                    // of words and pushed the preview and the count off it
                    // (user request).
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // v389 — a LIST is previewed as a list: its own rows with their
                // ticks, rather than its text run into a paragraph where a
                // finished row and an unfinished one read alike (user request:
                // "its preview as a separate todo preview not inside the
                // journal"). Everything else previews as prose exactly as before.
                if (journal.isTodo) {
                    Spacer(Modifier.height(6.dp))
                    ChecklistPreview(doc = journal.doc, ink = ink)
                } else if (journal.preview.isNotBlank()) {
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
    action: @Composable (() -> Unit)? = null,
    /**
     * v389 — THE ROLLING TITLE (a book's page).
     *
     * A page that repeats its own title in the head AND in the body shows the
     * same words twice before anything is scrolled (user report: "in book also
     * in header it shows the title and then below too"). So the head's title is
     * a ROLL-UP: it is invisible while the page's own title is on screen and
     * comes in as that one scrolls under the head — which is what a reader
     * means by "where am I" once the page's own title is gone. The line is
     * always LAID OUT (only its ink moves), so nothing below it can jump.
     */
    titleRevealed: Boolean = true,
    /**
     * v389 — WHAT THE HEAD SAYS WHILE THE PAGE'S OWN TITLE IS STILL ON SCREEN.
     *
     * Empty (the default) is right for a page with no context to name. A book's
     * page names its SHELF: until the book's own title and author have scrolled
     * under the head there is nothing about the book for the head to add, and
     * "Your shelf" is where the member actually is (user request: "instead of
     * initial blank say your shelf").
     */
    idleTitle: String = ""
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
        // ONE, not two: the head says one thing at a time, and it ROLLS — the
        // idle line and the rolled one are the same two rows, so the swap is
        // the words changing and the head never changes size. The AUTHOR comes
        // in with the title (user request: "when i scroll away the title appears
        // in header make the same for title author too not just title"): before
        // the roll the page is still saying both of them itself, in the middle
        // of the screen, at a size the head cannot match.
        AnimatedContent(
            targetState = titleRevealed,
            transitionSpec = {
                (
                    fadeIn(tween(230)) +
                        slideInVertically(tween(270)) { height -> -height / 3 }
                    ) togetherWith (
                    fadeOut(tween(150)) +
                        slideOutVertically(tween(190)) { height -> -height / 3 }
                    )
            },
            label = "personal-header-roll",
            modifier = Modifier.weight(1f)
        ) { rolled ->
            Column(Modifier.fillMaxWidth()) {
                Text(
                    if (rolled) title else idleTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (rolled) ink else ink.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    // The line HOLDS ITS HEIGHT while it is out of the way, so
                    // the head is one size whatever it is saying.
                    color = if (rolled) ink.copy(alpha = 0.5f) else Color.Transparent,
                    maxLines = 1
                )
            }
        }
        action?.invoke()
    }
}

/**
 * v389 — the personal family's head wears TODAY where a pill action used to be.
 * A journal collection and a book shelf are both "what I am doing now", and the
 * date says that without a button: the doors to a new page are the floating `+`
 * (journals, shelf) and the empty state's own action (user request — the
 * "Write today" / "Add a book" pills left both heads).
 */
@Composable
internal fun PersonalHeaderDate(
    dateMillis: Long = System.currentTimeMillis()
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                CurioIcons.CalendarToday,
                null,
                tint = personalAccentInk(),
                size = 15.dp
            )
            Text(
                dateMillis.prettyDate(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ink.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
