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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * v389 — WHERE A ROW'S CONTENT MAY BEGIN: past the pinned door and the air
 * around it. The door holds the left edge of its row (see [PinnedDoorRow]), so
 * the chips start HERE and slide back under the door as the row is dragged.
 */
private val DOOR_GUTTER = 16.dp + CHIP_WIDTH + 10.dp

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
    val progress = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(visible) {
        progress.animateTo(
            if (visible) 1f else 0f,
            animationSpec = tween(if (visible) 200 else 160)
        )
    }
    // v389 — the old AnimatedVisibility painted the shadow on a separate render
    // node from the scale transform, so the shadow's box popped out as a stale
    // rectangle during the scale-out instead of shrinking with the circle. This
    // drives both scale AND shadowElevation from the same progress float inside
    // one graphicsLayer, so the shadow is always the circle's own shadow at
    // every moment of the animation.
    if (progress.value > 0.01f) {
        val p = progress.value
        Surface(
            onClick = onClick,
            enabled = p > 0.9f,
            shape = CircleShape,
            color = accent,
            shadowElevation = (10 * p).dp,
            modifier = modifier
                .size(56.dp)
                .graphicsLayer {
                    val s = 0.82f + 0.18f * p
                    scaleX = s
                    scaleY = s
                    alpha = p
                }
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
            // v389 — the door wears the CHECKBOX the to-do page itself draws
            // (the same mark the journal's to-do tool wears), instead of the
            // `task_alt` icon, which reads as "task added", not "a list".
            CreateEntryOption(
                glyph = null,
                title = "A to-do list",
                body = "Check off tasks as you go",
                accent = personalAccent(),
                onClick = onTodoList,
                drawn = { TodoGlyph(active = false, iconSize = 20.dp) }
            )
        }
    }
}

@Composable
private fun CreateEntryOption(
    glyph: String?,
    title: String,
    body: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    /** A drawn door mark, for the doors whose icon the bundled font subset
     *  cannot say — the to-do list wears the page's OWN checklist box. */
    drawn: (@Composable () -> Unit)? = null
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
                // v412 — opaque: the accent is mixed into the card fill instead
                // of tinting it translucently.
                color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.24f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    if (drawn != null) {
                        CompositionLocalProvider(LocalContentColor provides glyphTint) { drawn() }
                    } else {
                        CurioIcon(glyph.orEmpty(), null, tint = glyphTint, size = 20.dp)
                    }
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
    /**
     * v389d — THE PAGE'S OWN BACKDROP, for the doors' plate.
     *
     * The two doors ride on an opaque fill so the chips can slide under them —
     * and that fill was the theme's plain `background`, which on Home is NOT
     * what the page is painted with (Home wears a lane wash, or its own rose
     * tint). The doors therefore sat on a pale plate of their own, a visible
     * seam in every theme that is not plain white (user report: "in home screen
     * the stikky pages and your my shelf. they have a white background which
     * creates weird theme issues with background"). The caller hands over what
     * it actually painted with.
     */
    backdrop: Color = MaterialTheme.colorScheme.background,
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
        PinnedDoorRow(
            backdrop = backdrop,
            door = {
                DoorChip(
                    glyph = CurioIcons.Note,
                    label = "Pages",
                    caption = "All journals",
                    onClick = { navController.navigate(CurioRoutes.JOURNALS) { launchSingleTop = true } }
                )
            }
        ) {
            // ── v406 — A DOOR'S ROW WITH NOTHING IN IT SAYS SO ──
            //
            // A first-time member's Home had two doors and two rows of nothing:
            // a LazyRow draws no chip until a journal or a book exists, so the
            // strip collapsed to the door alone and read as part of the page
            // that had failed to load (member's report: "in home screen the
            // doors and its list so when user opens the page for the first time
            // those place feels empty"). The door now offers the first step
            // itself, at a chip's own size, so the row keeps its shape.
            if (journals.isEmpty()) {
                item("empty-journals") {
                    EmptyDoorChip(
                        glyph = CurioIcons.Add,
                        label = "No pages yet",
                        caption = "Start your first one",
                        onClick = onWrite
                    )
                }
            }
            // v389d — MORE THAN THREE (user question: "why only 3 books and 3
            // journal shows. add more keeping scroll too"). The row has always
            // been a LazyRow — it scrolls — so the cap was the only reason the
            // rest of the library could not be reached from here.
            items(items = journals.take(CHIP_ROW_LIMIT), key = { it.id }) { journal ->
                JournalChip(journal = journal, onClick = {
                    // v389 — the chip opens the page's OWN screen (a to-do list is
                    // not a journal day; see personalRouteFor).
                    navController.navigate(personalRouteFor(journal)) { launchSingleTop = true }
                })
            }
        }
        PinnedDoorRow(
            backdrop = backdrop,
            door = {
                DoorChip(
                    glyph = CurioIcons.MenuBook,
                    label = "My shelf",
                    caption = "Books",
                    onClick = { navController.navigate(CurioRoutes.BOOKS) { launchSingleTop = true } }
                )
            }
        ) {
            // The shelf's own first step, for the same reason as the row above.
            if (books.isEmpty()) {
                item("empty-books") {
                    EmptyDoorChip(
                        glyph = CurioIcons.Add,
                        label = "No books yet",
                        caption = "Open the shelf",
                        onClick = {
                            navController.navigate(CurioRoutes.BOOKS) { launchSingleTop = true }
                        }
                    )
                }
            }
            items(items = books.take(CHIP_ROW_LIMIT), key = { it.id }) { book ->
                BookChip(book = book, onClick = {
                    navController.navigate(CurioRoutes.bookDetail(book.id)) { launchSingleTop = true }
                })
            }
        }
    }
}

/**
 * v389 — A ROW WHOSE DOOR CANNOT SCROLL AWAY.
 *
 * The door ("Pages" / "My shelf") holds the row's left edge and the content
 * scrolls BESIDE it, sliding UNDER it — so the way into the full list is one tap
 * away however far the row has been dragged (user request: "make the pages and
 * my shelf sticky … the content of them stays scrollable and it goes under
 * that").
 *
 * The door rides in an opaquely filled overlay because what passes beneath it
 * has to be HIDDEN: the fill is the page's own background, not a translucent
 * scrim, which would ghost the sliding chips through the door and its shadow.
 */
@Composable
private fun PinnedDoorRow(
    backdrop: Color,
    door: @Composable () -> Unit,
    content: LazyListScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            // The strip the door occupies is RESERVED, so the first chip starts
            // clear of it and only slides under once the row is dragged.
            contentPadding = PaddingValues(start = DOOR_GUTTER, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                // The plate: the page's own backdrop, solid under the door and
                // FADED at its trailing edge — an opaque rectangle laid over a
                // tinted page reads as a seam, and a bare fade would ghost the
                // chips through the door's own text.
                .drawBehind {
                    val tail = 18.dp.toPx().coerceAtMost(size.width)
                    val solid = (size.width - tail).coerceAtLeast(0.001f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to backdrop,
                                (solid / size.width).coerceIn(0f, 1f) to backdrop,
                                1f to backdrop.copy(alpha = 0f)
                            )
                        )
                    )
                }
                .padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            door()
        }
    }
}

/**
 * v389d — HOW MANY CHIPS A HOME ROW OFFERS. The row scrolls, so this is only
 * about not building a chip for a library of hundreds on first frame.
 */
private const val CHIP_ROW_LIMIT = 12

/**
 * v406 — THE FIRST STEP, WHERE A DOOR'S ROW HAS NOTHING YET.
 *
 * A chip's own width and height, so an empty row is a row and not a strip that
 * collapsed to its door. It reads as an invitation rather than as an error: the
 * glyph in the accent's own wash, the label a plain fact, the caption the thing
 * to do about it (see [PersonalChipsRow]).
 */
@Composable
private fun EmptyDoorChip(
    glyph: String,
    label: String,
    caption: String,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                // v412 — opaque (see the disc above).
                color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.14f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    CurioIcon(glyph, null, tint = accent, size = 18.dp)
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ink
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.55f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NewChip(onClick: () -> Unit) {
    val accent = personalAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        // v412 — opaque: the chip is the accent mixed into the card fill, never
        // a translucent wash over the page.
        color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.12f),
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
                // v389e — THE DAY IS INK, NOT ACCENT.
                //
                // The numeral wore the accent, which on a pale card is the
                // lightest thing on it — the day, the one figure a journal chip
                // exists to say, was the hardest word on the tile to read (user
                // report: "for journal number date its too accent color and very
                // light colored so fix it by making it dark"). It is the page's
                // own ink now, with the month beside it held back — so the day
                // leads and the month follows.
                Text(
                    journal.dateMillis.toLocalDate().dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink
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

/**
 * v389 — A BOOK AS A CHIP: THE WHOLE COVER, WITH ITS NAME ON A FOOTER.
 *
 * The cover used to be a 58dp band with the title printed UNDER it, which read
 * as two unrelated things (a sliver of artwork and a line of text) — and the
 * band cropped most covers to a strip. The chip is now the COVER, full height,
 * with a small footer bar laid across its bottom edge holding the name in ONE
 * line: the strip is a fixed height, so a long title is ellipsised instead of
 * pushing the cover around (user request).
 */
@Composable
private fun BookChip(
    book: PersonalBookEntity,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val footerInk = MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Box(Modifier.fillMaxSize()) {
            BookCover(
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                corner = 0.dp,
                modifier = Modifier.fillMaxSize()
            )
            // The footer: one line, its own height, OPAQUE so the cover cannot
            // bleed through the title it is naming.
            Surface(
                shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                color = footerInk,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // v389 — the footer's type is SMALL (user request: "make
                    // the title font more smaller"): it sits on a 24dp strip
                    // under the artwork, so it names the book rather than
                    // competing with its cover.
                    Text(
                        book.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            letterSpacing = 0.1.sp
                        ),
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * A door chip: the row's quiet "there is more" (and where an empty library
 * still finds its list).
 *
 * v389 — it has DEPTH now: a soft shadow plus a hairline edge, the way the
 * Cabinet's shelves read, so the two doors (Pages / My shelf) sit ABOVE the
 * chips around them instead of flat beside them (user request).
 *
 * AND IT WEARS THE ACCENT (user request: "give them the theme color accent to
 * the card of it"): an opaque wash of the member's own accent over the surface,
 * with an accent hairline and the accent's deep shade as the ink, so the door is
 * unmistakably the way in rather than a third chip. The wash is OPAQUE on
 * purpose — the row slides under this card, so a translucent fill would show the
 * chips passing beneath it and let the shadow bleed through.
 */
@Composable
private fun DoorChip(
    glyph: String,
    label: String,
    caption: String,
    onClick: () -> Unit
) {
    val accent = personalAccent()
    // ── THE DOOR IS THE ACCENT (v389e) ──────────────────────────────────
    //
    // It used to be a mostly-surface card with an accent hairline around it and
    // the words in the accent's darker ink — a tinted outline on a pale fill
    // (user request: "for the door in home screen pages and my shelf dont give it
    // accent border but make the whole card accent color and proper text color as
    // well so its readable too"). So the whole face is the accent, with no
    // border, and the words wear the app's own ink for text ON an accent fill —
    // which is what makes a label read on a filled card instead of glowing on
    // it. The fill is opaque on purpose: a translucent one lets the shadow bleed
    // through (see the shadow rule).
    val onAccent = personalOnAccent()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = accent,
        shadowElevation = 6.dp,
        modifier = Modifier
            .width(CHIP_WIDTH)
            .height(CHIP_HEIGHT)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurioIcon(glyph, null, tint = onAccent, size = 22.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = onAccent
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = onAccent.copy(alpha = 0.78f)
            )
        }
    }
}
