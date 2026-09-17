package com.curio.app.features.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.PAGE_KIND_JOURNAL
import com.curio.app.data.PersonalDoc
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.searchTopicIndex
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.WritingFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * v389 — A NOTE ON A TOPIC, as its OWN page.
 *
 * This used to be a journal day wearing topic route-params: the same date bar,
 * the same mood pill, the same "Title this day" — a page about, say, the
 * Voyager probes that opened asking how the DAY felt. A note about a topic is
 * not a day, so it is not a journal: it has a TOPIC at its head (its lane, its
 * name, and a way through to the topic's own page) and the writing underneath.
 *
 * The topic is picked on the page itself — from Curio's own catalog (the merged
 * index, ranked exactly like the composer's chooser and the Share Hub) or, for
 * anything the catalog does not have, typed in by hand. Either way the page
 * stores the topic id, name and lane: that is what puts the topic at the head
 * of the saved page, tells the journals list what a page is about, and lets the
 * page offer a way through to the topic's own reveal page.
 */
@Composable
fun TopicNoteScreen(
    navController: NavController,
    entryIdArg: String,
    initialTopicId: String = "",
    initialTopicName: String = "",
    initialCategoryId: String = "",
    photos: PersonalPhotoOverlayState = rememberPersonalPhotoOverlayState()
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = personalAccent()

    // The topic the page is about. Seeded by the route when the note was
    // started FROM a topic (the reveal page's "write about this"), and picked on
    // the page otherwise — including later, on a page that started blank.
    // v389 — SAVED, not merely remembered. Tapping the topic in the header
    // navigates to that topic's page; coming back disposed this composition, so
    // the picked topic was forgotten and the page asked for a new one while the
    // first choice had already been written to its row (user report: "when i
    // select one and i tap the look the topic from the header and i press back
    // the previous topic gets saved and it asks me again to choose a new").
    var topicId by rememberSaveable { mutableStateOf(initialTopicId) }
    var topicName by rememberSaveable { mutableStateOf(initialTopicName) }
    var categoryId by rememberSaveable { mutableStateOf(initialCategoryId) }
    // A note about a topic is not a DAY, but it keeps the day it was written:
    // the journals list groups pages by date, and a page with no date would land
    // in January 1970. Set once, never moved.
    // A BOXED Long on purpose: `rememberSaveable` saves a MutableState, not the
    // primitive holders (mutableLongStateOf / Int / Float are never used with it
    // anywhere in this app, and they are not what its state overload expects).
    var dateMillis by rememberSaveable { mutableStateOf(startOfToday()) }
    var pickerOpen by rememberSaveable { mutableStateOf(initialTopicName.isBlank()) }

    val category = categoryId.takeIf { it.isNotBlank() }
        ?.let { runCatching { CurioCategories.byId(CategoryId.valueOf(it)) }.getOrNull() }

    PersonalWritingPage(
        entryIdArg = entryIdArg,
        photos = photos,
        meta = {
            PersonalPageMeta(
                title = topicName,
                // A note about a topic is not a day: no mood, no day of its own
                // (it carries the day it was written, which is all the list
                // needs to group it).
                mood = "",
                dateMillis = dateMillis,
                kind = PAGE_KIND_JOURNAL,
                topicId = topicId,
                topicName = topicName,
                categoryId = categoryId
            )
        },
        onLoaded = { existing ->
            // A saved note brings its own topic back, and an existing row wins
            // over the route (the route seeds a NEW page).
            if (existing.hasTopic) {
                topicId = existing.topicId
                topicName = existing.topicName
                categoryId = existing.categoryId
                pickerOpen = false
            }
            if (existing.dateMillis > 0L) dateMillis = existing.dateMillis
        },
        // The page's own way out (the core guards it: a live voice recording is
        // asked about before a back gesture can drop it — see PersonalVoice).
        onExit = { navController.popBackStack() },
        voiceRoute = { CurioRoutes.topicNote(it) },
        header = { editing, saving, onEditing, onBack ->
            TopicNoteTopBar(
                topicName = topicName,
                categoryName = category?.displayName.orEmpty(),
                saving = saving,
                editing = editing,
                onBack = onBack,
                onToggleMode = onEditing,
                onOpenTopic = topicName.takeIf { it.isNotBlank() }?.let { name ->
                    {
                        // The route slug lives on the category ID, not on the
                        // category: a lane's id is what the routes speak.
                        val slug = category?.id?.routeSlug
                            ?: CurioCategories.byId(CategoryId.WILDCARD).id.routeSlug
                        navController.navigate(CurioRoutes.revealFor(slug, name)) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        },
        readView = { doc ->
            TopicNoteReadView(
                topicName = topicName,
                categoryName = category?.displayName.orEmpty(),
                glyph = category?.iconGlyph ?: CurioIcons.TravelExplore,
                accent = accent,
                doc = doc,
                onOpenPhoto = { uri, bounds -> photos.open(uri, bounds) }
            )
        },
        // v389 — the topic head is PINNED above the writing instead of scrolling
        // with it: the subject of a note is not a field on it, and a scrolled
        // head is what hid the "choose the topic" door under the fold (user
        // report). It stays put while the words move under it.
        pinnedHead = {
            TopicHead(
                topicName = topicName,
                categoryName = category?.displayName.orEmpty(),
                glyph = category?.iconGlyph ?: CurioIcons.TravelExplore,
                accent = accent,
                ink = ink,
                onChoose = { pickerOpen = true }
            )
            Spacer(Modifier.height(12.dp))
        }
    )

    if (pickerOpen) {
        TopicPickerSheet(
            currentName = topicName,
            onDismiss = { pickerOpen = false },
            onPick = { pickedId, pickedName, pickedCategory ->
                topicId = pickedId
                topicName = pickedName
                categoryId = pickedCategory
                pickerOpen = false
            }
        )
    }
}

/**
 * The page's head: the topic as a card. Tapping it (or the pill) opens the
 * picker, so a page started blank can get its topic at any point.
 */
@Composable
private fun TopicHead(
    topicName: String,
    categoryName: String,
    glyph: String,
    accent: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color,
    onChoose: () -> Unit
) {
    val hasTopic = topicName.isNotBlank()
    Surface(
        onClick = onChoose,
        shape = RoundedCornerShape(20.dp),
        color = if (hasTopic) accent.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (hasTopic) accent.copy(alpha = 0.28f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(38.dp)
            ) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    CurioIcon(
                        glyph,
                        null,
                        tint = personalAccentInk(),
                        size = 19.dp
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (hasTopic) "ABOUT THIS TOPIC" else "A NOTE ON A TOPIC",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    ),
                    color = personalAccentInk().copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (hasTopic) topicName else "Choose the topic",
                    style = TextStyle(
                        fontFamily = FrauncesFontFamily,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasTopic) ink else ink.copy(alpha = 0.55f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasTopic && categoryName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.copy(alpha = 0.55f)
                    )
                }
            }
            CurioIcon(
                if (hasTopic) CurioIcons.ChevronRight else CurioIcons.Search,
                null,
                tint = personalAccentInk().copy(alpha = 0.8f),
                size = 18.dp
            )
        }
    }
}

/** The page's own bar: back, the topic's lane, the write/read switch. */
@Composable
private fun TopicNoteTopBar(
    topicName: String,
    categoryName: String,
    saving: Boolean,
    editing: Boolean,
    onBack: () -> Unit,
    onToggleMode: (Boolean) -> Unit,
    onOpenTopic: (() -> Unit)?
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(38.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CurioIcon(CurioIcons.ArrowBack, "Back", tint = ink, size = 19.dp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                "Note on a topic",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(
                    topicName.ifBlank { null },
                    categoryName.ifBlank { null }
                ).joinToString(" · ").ifBlank { "Pick a topic to write about" },
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onOpenTopic != null) {
            Surface(
                onClick = onOpenTopic,
                shape = RoundedCornerShape(50),
                color = personalAccent().copy(alpha = 0.20f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    CurioIcon(
                        CurioIcons.TravelExplore,
                        null,
                        tint = personalAccentInk(),
                        size = 15.dp
                    )
                    Text(
                        "The topic",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = personalAccentInk()
                    )
                }
            }
        }
        // The pen/eye pair, the same habit as a journal page: the page opens as
        // what it is and the pen brings the tools.
        PersonalModeSwitch(editing = editing, onToggleMode = onToggleMode)
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(
                    color = if (saving) personalAccentInk()
                    else ink.copy(alpha = 0.20f),
                    shape = CircleShape
                )
        )
    }
}

/** The saved note as it reads: the topic, then the writing. */
@Composable
private fun TopicNoteReadView(
    topicName: String,
    categoryName: String,
    glyph: String,
    accent: androidx.compose.ui.graphics.Color,
    doc: PersonalDoc,
    onOpenPhoto: (String, androidx.compose.ui.geometry.Rect?) -> Unit
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .widthIn(max = 680.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CurioIcon(glyph, null, tint = personalAccentInk(), size = 15.dp)
            Text(
                if (categoryName.isBlank()) "NOTE" else categoryName.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp
                ),
                color = personalAccentInk()
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            topicName.ifBlank { "A note" },
            style = TextStyle(
                fontFamily = FrauncesFontFamily,
                fontSize = 27.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink
            )
        )
        Spacer(Modifier.height(16.dp))
        if (doc.isEmpty) {
            Text(
                "Nothing written about this topic yet — tap the pen to start.",
                style = TextStyle(
                    fontFamily = FrauncesFontFamily,
                    fontSize = 15.sp,
                    color = ink.copy(alpha = 0.5f)
                )
            )
        } else {
            PersonalDocView(doc = doc, accent = accent, onOpenPhoto = onOpenPhoto)
        }
        Spacer(Modifier.height(120.dp))
    }
}

/**
 * THE TOPIC PICKER: Curio's own catalog first (the merged index, ranked like
 * every other chooser in the app) and a typed name as the door for anything the
 * catalog does not carry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicPickerSheet(
    currentName: String,
    onDismiss: () -> Unit,
    onPick: (id: String, name: String, categoryId: String) -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    var query by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // The merged index: warm from the app prewarm, so the first keystroke is
    // answered without a wait (see TopicJsonLoader.loadIndex).
    val index by produceState(initialValue = TopicJsonLoader.cachedIndex().orEmpty()) {
        if (value.isEmpty()) {
            value = withContext(Dispatchers.Default) {
                runCatching { TopicJsonLoader.loadIndex() }.getOrNull().orEmpty()
            }
        }
    }
    val results = remember(index, query) {
        if (query.isBlank()) emptyList() else searchTopicIndex(index, query, 24)
    }
    val typed = query.trim()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Write about a topic",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = ink
            )
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = WritingFontFamily,
                    fontSize = 16.sp,
                    color = ink
                ),
                cursorBrush = SolidColor(accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search the catalog…",
                                style = TextStyle(
                                    fontFamily = WritingFontFamily,
                                    fontSize = 16.sp,
                                    color = ink.copy(alpha = 0.42f)
                                )
                            )
                        }
                        inner()
                    }
                }
            )

            // The typed door comes first: it is how a topic the catalog does
            // not have is written about.
            if (typed.isNotBlank()) {
                PickerRow(
                    glyph = CurioIcons.Edit,
                    label = "Write about \"$typed\"",
                    meta = "Your own words for it",
                    accent = accent,
                    onClick = {
                        onPick(
                            typed.lowercase().replace(' ', '-'),
                            typed,
                            ""
                        )
                    }
                )
            }

            if (index.isEmpty()) {
                PickerRow(
                    glyph = CurioIcons.Note,
                    label = "Write about \"$currentName\"",
                    meta = "Start typing above",
                    accent = accent,
                    onClick = onDismiss
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { entry ->
                        PickerRow(
                            glyph = CurioCategories.byId(entry.categoryId).iconGlyph,
                            label = entry.name,
                            meta = listOfNotNull(
                                entry.byline.ifBlank { null },
                                CurioCategories.byId(entry.categoryId).displayName
                            ).joinToString(" · "),
                            accent = accent,
                            onClick = {
                                onPick(entry.id, entry.name, entry.categoryId.name)
                            }
                        )
                    }
                    if (results.isEmpty()) {
                        item {
                            Text(
                                if (query.isBlank()) "Start typing to search the catalog"
                                else "Nothing in the catalog matches \"$typed\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = ink.copy(alpha = 0.55f),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One row of the picker: the lane's glyph, the name, and where it comes from. */
@Composable
private fun PickerRow(
    glyph: String,
    label: String,
    meta: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = accent.copy(alpha = 0.22f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    CurioIcon(glyph, null, tint = personalIconTint(accent), size = 16.dp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            CurioIcon(CurioIcons.ChevronRight, null, tint = ink.copy(alpha = 0.35f), size = 17.dp)
        }
    }
}
