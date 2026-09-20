package com.curio.app.features.personal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.curio.app.data.AppPreferences
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalKinds
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.newPersonalBookId
import com.curio.app.features.cabinet.CabinetCoverCache
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.components.rememberCurioPressSource
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * v387 — THE BOOK SHELF.
 *
 * Books the member is reading with a reason to write: a cover, the name, and
 * the progress underneath — the shelf IS the collection view ("the book reader
 * should be like shelves with just book cover and below book name with
 * progress shown"). Opening one lands on the book page, where a chapter's
 * review is written and read in the same place.
 *
 * Adding a book searches Open Library (covers, authors, chapter counts) and
 * always keeps a manual door, so a book that is not in any catalogue — or a
 * member with no connection — can still be added by typing it.
 */
@Composable
fun BookShelfScreen(navController: NavController) {
    val context = LocalContext.current
    val books by produceState(initialValue = emptyList<PersonalBookEntity>()) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBooks().collect { value = it }
        }
    }
    var addOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PersonalBookEntity?>(null) }
    val scope = rememberCoroutineScope()

    // ── THE COVERS COME TO THE SHELF (v410) ────────────────────────────
    // Every book on the shelf carries its own cover URL once it has one, and
    // books that came in without any (shelved from a topic reveal before v410,
    // typed in by hand, imported from a file) are resolved here — quietly, one
    // at a time, on the same verified-bytes machinery the Cabinet uses (see
    // [BookCoverWarmup]) — so the shelf fills with real artwork instead of a
    // grid of generated plates. Each result is written onto the book's row, so
    // the shelf, the book page, the Cabinet and Home all pick it up at once.
    val coverConsent = AppPreferences.coverFetchEnabledState
    val coverless = remember(books) {
        books.filter { it.coverUrl.isBlank() && it.title.isNotBlank() }
    }
    LaunchedEffect(coverless.map { it.id }, coverConsent) {
        if (coverless.isEmpty()) return@LaunchedEffect
        var fetched = 0
        withContext(Dispatchers.IO) {
            for (book in coverless) {
                if (BookCoverWarmup.ensureCover(context, book) != null) fetched++
            }
        }
        // ONE version bump for the whole batch, so the grid re-checks the
        // local files once instead of recomposing per cover (the Cabinet's own
        // warmer batching rule, v3xx37).
        if (fetched > 0) CabinetCoverCache.version.intValue++
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
        PersonalHeader(
            title = "My shelf",
            subtitle = when (books.size) {
                0 -> "Books you are reading with a reason to write"
                1 -> "1 book"
                else -> "${books.size} books"
            },
            onBack = { navController.popBackStack() },
            // v389 — the head wears TODAY instead of an "Add a book" pill: the
            // floating + already opens the add-book sheet (and the empty state
            // carries its own action), so the pill was a third door in a head
            // that has two (user request).
            action = { PersonalHeaderDate() }
        )

        if (books.isEmpty()) {
            PersonalEmptyCard(
                glyph = CurioIcons.MenuBook,
                title = "The shelf is empty",
                body = "Pick a book and write it as you go: a review for any chapter, " +
                    "and the progress kept for you.",
                actionLabel = "Add a book",
                onAction = { addOpen = true }
            )
        } else {
            LazyVerticalGrid(
                // THREE across, like a real shelf: covers read as spines and a
                // library fits without scrolling past a wall of half-empty
                // rows. The grid is SECTIONED, so what is being read and what
                // is done are never mixed into one anonymous block.
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                val reading = books.filterNot { it.isFinished }
                val finished = books.filter { it.isFinished }
                if (reading.isNotEmpty()) {
                    item(
                        key = "shelf-head-reading",
                        span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
                    ) {
                        ShelfSectionHead(label = "READING", count = reading.size)
                    }
                }
                items(items = reading, key = { it.id }) { book ->
                    BookShelfCard(
                        book = book,
                        onClick = {
                            navController.navigate(CurioRoutes.bookDetail(book.id)) {
                                launchSingleTop = true
                            }
                        },
                        onLongPress = { pendingDelete = book }
                    )
                }
                if (finished.isNotEmpty()) {
                    item(
                        key = "shelf-head-finished",
                        span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
                    ) {
                        ShelfSectionHead(label = "FINISHED", count = finished.size)
                    }
                }
                items(items = finished, key = { it.id }) { book ->
                    BookShelfCard(
                        book = book,
                        onClick = {
                            navController.navigate(CurioRoutes.bookDetail(book.id)) {
                                launchSingleTop = true
                            }
                        },
                        onLongPress = { pendingDelete = book }
                    )
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.height(70.dp))
                }
            }
        }
        }
        PersonalCreateLauncher(
            visible = true,
            onClick = { addOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 18.dp)
        )
    }

    if (addOpen) {
        AddBookSheet(
            onDismiss = { addOpen = false },
            onAdded = { bookId ->
                addOpen = false
                navController.navigate(CurioRoutes.bookDetail(bookId)) { launchSingleTop = true }
            }
        )
    }

    pendingDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${book.title}?") },
            text = {
                Text("The book and every chapter review written in it leaves your shelf.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = book.id
                    pendingDelete = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching { PersonalRepositoryHolder.repo.deleteBook(id) }
                        }
                    }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep") } }
        )
    }
}

/** The shelf's own section label ("READING", "FINISHED"). */
@Composable
private fun ShelfSectionHead(label: String, count: Int) {
    val ink = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            ),
            color = personalIconTint(personalAccent())
        )
        Text(
            count.toString(),
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

/**
 * One book on the shelf: cover, name, progress. At three across there is no
 * room for a paragraph under every cover, so the card says one thing well —
 * where the member is — and the finished ones simply wear their tick.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookShelfCard(
    book: PersonalBookEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    val accentInk = personalAccentInk()
    val press = rememberCurioPressSource(pressedScale = 0.97f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(press.modifier)
            .combinedClickable(
                interactionSource = press.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Box {
            BookCover(
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.66f)
                    .clip(RoundedCornerShape(12.dp)),
                corner = 12.dp
            )
            if (book.isFinished) {
                Surface(
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CurioIcon(
                            CurioIcons.Check,
                            "Finished",
                            tint = personalOnAccent(),
                            size = 13.dp
                        )
                    }
                }
            }
            // v426 — WHAT IT IS, ON ITS OWN COVER. A manga says Manga, a manhwa
            // says Manhwa, a light novel says so: the member asked for these to
            // keep their own name rather than be filed as books, and a cover is
            // where a glance at a shelf always lands. A plain book wears none —
            // the absence is the label.
            if (book.kind != PersonalKinds.BOOK) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        PersonalKinds.label(book.kind),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ink,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                book.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                ),
                color = ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(6.dp))
        // The rail is the card's quiet sentence: how far in, out of how long.
        LinearProgressIndicator(
            progress = {
                if (book.isFinished) 1f
                else if (book.totalChapters <= 0) 0f
                else (book.currentChapter.toFloat() / book.totalChapters.toFloat())
                    .coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50)),
                color = accentInk,
                trackColor = accentInk.copy(alpha = 0.16f)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            when {
                book.isFinished -> "Finished"
                book.totalChapters <= 0 -> "Not started"
                book.currentChapter <= 0 -> "${book.totalChapters} chapters"
                else -> "Ch ${book.currentChapter} of ${book.totalChapters}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = accentInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A book's cover: the real artwork when there is one, and a generated cover
 * (the title set on the book's own tint) otherwise, so a shelf never shows a
 * row of empty frames while covers load or when a book has no artwork.
 */
@Composable
internal fun BookCover(
    title: String,
    author: String,
    coverUrl: String,
    modifier: Modifier = Modifier,
    corner: androidx.compose.ui.unit.Dp = 14.dp
) {
    val tones = remember(title) { generatedCoverTones(title) }
    val top = tones.first
    val bottom = tones.second
    // v410 — THE ART THE APP ALREADY FETCHED WINS. A cover resolved by the
    // shelf's warm pass is on disk (see [BookCoverWarmup] / CabinetCoverCache)
    // while the book's row may still be blank, so the cached file is read
    // first and the row's URL second — and it is read through the cache's
    // version, so a cover that lands while this plate is on screen appears
    // without waiting for the next visit.
    val context = LocalContext.current
    val cacheVersion = CabinetCoverCache.version.intValue
    val cachedArt = remember(title, cacheVersion) {
        CabinetCoverCache.localCoverFile(context, CabinetCoverCache.CoverKind.BOOK, title)
    }
    val art: Any? = cachedArt ?: coverUrl.takeIf { it.isNotBlank() }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(top, bottom)))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                title.ifBlank { "Untitled" },
                style = TextStyle(
                    fontFamily = FrauncesFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (author.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    author,
                    style = TextStyle(
                        fontFamily = LoraFontFamily,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.82f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // The spine, so a generated cover reads as a book rather than a tile.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.22f),
                        0.045f to Color.Black.copy(alpha = 0.22f),
                        0.075f to Color.Transparent,
                        1f to Color.Transparent
                    )
                )
        )
        if (art != null) {
            // The generated cover stays UNDER the artwork: it is the loading
            // and the error state at once, and the spine shows through a
            // cover that has not arrived yet.

            androidx.compose.foundation.Image(
                painter = rememberAsyncImagePainter(art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Adding a book
// ────────────────────────────────────────────────────────────────────────────

/** One Open Library hit, ready to become a book. */
private data class BookHit(
    val title: String,
    val author: String,
    val coverUrl: String,
    /** v426 — what the COMICS sources bring with a hit: how long it is, in the
     *  source's own units, plus its own words for the synopsis. */
    val chapters: Int = 0,
    val volumes: Int = 0,
    val description: String = "",
    val source: String = ""
)

/**
 * v408 — A PICKED FILE, WAITING TO BE NAMED.
 *
 * Holds the picker's URI plus what its file name suggested, so the
 * confirmation panel can be edited field by field before anything is written
 * to the shelf.
 */
private data class PendingImport(
    val uri: Uri,
    val title: String,
    val author: String
)

/**
 * v408 — CONFIRM THE IMPORT.
 *
 * Two editable fields and one decision. [PendingImport.title] arrives as the
 * guess taken from the file's name — often right, sometimes not — and the
 * sentence above the fields says plainly which of the two this is, so a member
 * who sees rubbish knows why and can fix it without wondering whether Curio
 * read the wrong file.
 */
@Composable
private fun BookImportConfirm(
    pending: PendingImport,
    ink: Color,
    accent: Color,
    onChange: (PendingImport) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Text(
        "Is this the book?",
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.SemiBold
        ),
        color = ink
    )
    Text(
        if (pending.title.isNotBlank()) {
            "Read from the file's name — fix it if the guess is off."
        } else {
            "The file's name did not say — give it a title."
        },
        style = MaterialTheme.typography.bodySmall,
        color = ink.copy(alpha = 0.6f)
    )
    BookField(
        value = pending.title,
        onValueChange = { onChange(pending.copy(title = it)) },
        placeholder = "Title",
        ink = ink,
        accent = accent
    )
    BookField(
        value = pending.author,
        onValueChange = { onChange(pending.copy(author = it)) },
        placeholder = "Author (optional)",
        ink = ink,
        accent = accent
    )
    Text(
        "Its pages and chapters are read from the file itself.",
        style = MaterialTheme.typography.labelSmall,
        color = ink.copy(alpha = 0.5f)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onCancel) { Text("Cancel") }
        TextButton(
            onClick = onConfirm,
            enabled = pending.title.isNotBlank()
        ) { Text("Add to shelf", color = personalAccentInk()) }
    }
}

/**
 * The add-a-book sheet: a search field, the catalogue's answers, and a manual
 * door underneath. Picking a result creates the shelf row immediately and
 * opens it, so the member lands on the book they just chose (rather than
 * having to find it again in the grid).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBookSheet(
    onDismiss: () -> Unit,
    onAdded: (String) -> Unit
) {
    val context = LocalContext.current
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    // v426 — WHICH KIND IS BEING ADDED. Chosen BEFORE the search, because the
    // question changes the sources: "book" asks Curio's own lane and Open
    // Library, while a manga/manhwa/manhua/light novel asks AniList, MangaDex,
    // MAL and Kitsu ([MangaFetch]) — a manga is simply not in a books catalogue.
    var kind by remember { mutableStateOf(PersonalKinds.BOOK) }
    var hits by remember { mutableStateOf<List<BookHit>>(emptyList()) }
    // The hits from Curio's OWN catalog, kept apart from the network ones: they
    // carry a chapter list and a page count, they never fail, and they lead.
    var catalogHits by remember { mutableStateOf<List<BookCatalog.Hit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(false) }
    var manualTitle by remember { mutableStateOf("") }
    var manualAuthor by remember { mutableStateOf("") }
    var manualChapters by remember { mutableIntStateOf(0) }
    // A file that has been picked but NOT yet become a book — see the
    // confirmation panel in the sheet below.
    var pendingImport by remember { mutableStateOf<PendingImport?>(null) }

    fun addBook(
        title: String,
        author: String,
        cover: String,
        chapters: Int,
        catalogId: String = "",
        pages: Int = 0,
        /** v426 — the row's own kind ([PersonalKinds]); a book unless said. */
        kind: String = PersonalKinds.BOOK,
        /** A file the member picked while adding (Import a file) — COPIED into
         *  the app's own storage, never kept as the picker's URI. */
        document: Uri? = null
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val id = newPersonalBookId()
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    PersonalRepositoryHolder.repo.saveBook(
                        PersonalBookEntity(
                            id = id,
                            title = trimmed,
                            author = author.trim(),
                            coverUrl = cover,
                            totalChapters = chapters.coerceAtLeast(0),
                            currentChapter = 0,
                            catalogId = catalogId,
                            pageCount = pages.coerceAtLeast(0),
                            kind = PersonalKinds.idOf(kind)
                        )
                    )
                    // v389 — the picked file belongs on the book's OWN column.
                    // It used to be parked in `coverUrl`, so the shelf tried to
                    // paint a PDF as a picture AND the document died with the
                    // picker's permission. The copy is the app's now.
                    if (document != null) {
                        val path = BookFiles.import(context, id, document)
                        if (!path.isNullOrBlank()) {
                            runCatching {
                                PersonalRepositoryHolder.repo.setDocument(id, path)
                            }
                            // v408 — AND THE FILE'S OWN FACTS.
                            //
                            // A book added from a file used to keep whatever
                            // the guesswork left on it (no pages, no
                            // chapters), so its page showed "Set how long the
                            // book is" for a file that knows exactly how long
                            // it is, and its reading progress had nothing to
                            // count against. The same adoption the attach drop
                            // does on an existing book runs here: the file's
                            // page count and its own table of contents are
                            // read out and written onto the row, so a book
                            // added straight from a PDF is measured by that
                            // PDF from the first frame.
                            runCatching {
                                val fromFile = documentChapters(context, path)
                                // v425 — every kind of file answers its own
                                // length now, not just a PDF: see
                                // `documentPageCount`.
                                val pages = documentPageCount(context, path)
                                PersonalRepositoryHolder.repo.adoptDocumentFacts(id, fromFile, pages)
                            }
                        }
                    }
                }
            }
            onAdded(id)
        }
    }

    // v408 — THE FILE'S NAME, THEN THE MEMBER'S SAY-SO.
    //
    // Importing used to create the book outright from `lastPathSegment`, which
    // for a document provider is an opaque id (`msf:1000000042`) — so a shelf
    // of imported files arrived as a column of numbers, and the name the member
    // could see in their own file manager was never read. Now the provider's
    // DISPLAY_NAME is parsed ([detectBookFromFileName]) and the guess is put in
    // FRONT of the member in editable fields: a book they can name is a book
    // they can find again, and a wrong guess costs a tap instead of a rename.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val detected = detectBookFromFileName(BookFiles.displayName(context, uri))
        pendingImport = PendingImport(
            uri = uri,
            title = detected.title,
            author = detected.author
        )
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // A picked file takes the whole sheet: there is nothing else to
            // decide until the member has said what the book is.
            pendingImport?.let { pending ->
                BookImportConfirm(
                    pending = pending,
                    ink = ink,
                    accent = accent,
                    onChange = { pendingImport = it },
                    onCancel = { pendingImport = null },
                    onConfirm = {
                        val ready = pending
                        pendingImport = null
                        // The picker's permission is not kept: the file is
                        // COPIED into the app's own storage by `addBook`, which
                        // is why the book still reads after it would expire.
                        addBook(ready.title, ready.author, "", 0, document = ready.uri)
                    }
                )
                return@Column
            }
            Text(
                if (manual) "Add it yourself" else "Find a book",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = ink
            )

            // Three doors: search by title, scan the ISBN barcode, or type it
            // yourself. The scan door is the fastest when the book is at hand.
            var scannerOpen by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScanDoor(
                    glyph = CurioIcons.Search,
                    label = "Search",
                    selected = !manual && !scannerOpen,
                    accent = accent,
                    ink = ink,
                    onClick = { manual = false; scannerOpen = false }
                )
                ScanDoor(
                    glyph = CurioIcons.Screenshot,
                    label = "Scan ISBN",
                    selected = scannerOpen,
                    accent = accent,
                    ink = ink,
                    onClick = { scannerOpen = true }
                )
                ScanDoor(
                    glyph = CurioIcons.Edit,
                    label = "Type",
                    selected = manual,
                    accent = accent,
                    ink = ink,
                    onClick = { manual = true; scannerOpen = false }
                )
            }

            if (scannerOpen) {
                IsbnScannerSheet(
                    onDismiss = { scannerOpen = false },
                    onAdded = onAdded
                )
                return@Column
            }

            if (manual) {
                BookField(value = manualTitle, onValueChange = { manualTitle = it }, placeholder = "Title", ink = ink, accent = accent)
                BookField(value = manualAuthor, onValueChange = { manualAuthor = it }, placeholder = "Author", ink = ink, accent = accent)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        // A manga's own unit is the volume as often as the
                        // chapter, so the question says so rather than asking a
                        // light novel how many chapters it has.
                        if (PersonalKinds.asksComicSources(kind)) "How many chapters or volumes?"
                        else "How many chapters?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )
                    ChapterStepper(count = manualChapters, onChange = { manualChapters = it }, accent = accent, ink = ink)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { manual = false }) { Text("Search instead") }
                    TextButton(onClick = {
                        addBook(manualTitle, manualAuthor, "", manualChapters, kind = kind)
                    }) { Text("Add to shelf", color = personalAccentInk()) }
                }
                return@Column
            }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/epub+zip", "application/pdf", "text/plain")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import an EPUB, PDF, or text file")
            }            // ── v426 — WHAT IS BEING ADDED ─────────────────────────────
            //
            // One row of kinds, beside the search it changes: a manga's cover,
            // author and length live in the comics sources and a book's live in
            // Open Library, so the question has to be asked before the search
            // rather than after it. Every kind stays a SHELF row (the member's
            // own ask: "keep them as manga or whatever they are called, but add
            // them to be able to add in my shelf") — this only decides where the
            // facts come from and what the row calls itself.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                PersonalKinds.all.forEach { option ->
                    val on = option == kind
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (on) accent else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                kind = option
                                // The last search answered for the OLD kind, so
                                // its hits are cleared rather than left sitting
                                // under a label they do not belong to.
                                hits = emptyList()
                                catalogHits = emptyList()
                                searched = false
                                failed = false
                            }
                    ) {
                        Text(
                            PersonalKinds.label(option),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (on) personalAccentInk()
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            BookField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Title or author",
                ink = ink,
                accent = accent,
                imeAction = ImeAction.Search,
                onSearch = {
                    val text = query.trim()
                    if (text.isEmpty()) return@BookField
                    searching = true
                    failed = false


                    // Read BEFORE the coroutine starts, so a kind tapped while
                    // the search is in flight cannot relabel its own results.
                    val comics = PersonalKinds.isComics(kind)
                    // v426b — a WESTERN COMIC asks the comics sources too, and
                    // for the same reason a manga does: the catalogue that holds
                    // a volume of *Watchmen* is Comic Vine, not Open Library. It
                    // keeps the books catalogue as its FALLBACK though — Open
                    // Library really does carry trade paperbacks and graphic
                    // novels, and a keyless build has no Comic Vine at all.
                    val comicBook = PersonalKinds.isComicBook(kind)
                    val wanted = kind
                    scope.launch {
                        // CURIOS'S OWN CATALOG FIRST, for a BOOK: instant,
                        // offline, and the only source that also carries the
                        // chapter list — so the book arrives with its real table
                        // of contents. A manga is not in it, so a comics kind
                        // asks nobody but its own sources.
                        val local = if (comics) emptyList()
                        else withContext(Dispatchers.IO) { BookCatalog.search(text) }
                        catalogHits = local
                        // …THEN the wider sources. A plain book goes to Open
                        // Library; a manga, manhwa, manhua or light novel goes
                        // to the COMICS sources ([MangaFetch] — its keyless four
                        // in order, AniList → MangaDex → MAL → Kitsu); and a
                        // WESTERN COMIC (v426b) asks those same sources with
                        // Comic Vine FIRST, falling back to Open Library only
                        // when they answer nothing, so a keyless build keeps
                        // exactly what it had. A failure only matters when
                        // nothing else answered, so an offline phone still gets
                        // a useful sentence instead of an apology.
                        val remote = withContext(Dispatchers.IO) {
                            if (comics || comicBook) {
                                val viaComics = MangaFetch.search(text, wanted)?.map { found ->
                                    BookHit(
                                        title = found.title,
                                        author = found.author,
                                        coverUrl = found.coverUrl,
                                        chapters = found.chapters,
                                        volumes = found.volumes,
                                        description = found.description,
                                        source = found.source
                                    )
                                }
                                if (comicBook && viaComics.isNullOrEmpty()) searchOpenLibrary(text)
                                else viaComics
                            } else {
                                searchOpenLibrary(text)
                            }
                        }
                        searching = false
                        searched = true
                        failed = remote == null && local.isEmpty()
                        hits = remote.orEmpty()
                    }
                }
            )

            if (searching) {
                Text(
                    "Searching…",
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.copy(alpha = 0.55f)
                )
            }
            if (failed) {
                Text(
                    "The catalogue could not be reached. Add the book yourself instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.copy(alpha = 0.62f)
                )
            }
            // The app's own books, with what they bring: chapters and pages.
            catalogHits.forEach { hit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            addBook(
                                title = hit.title,
                                author = hit.author,
                                cover = hit.coverUrl,
                                chapters = hit.chapterCount,
                                catalogId = hit.topicId,
                                pages = hit.pageCount
                            )
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BookCover(
                        title = hit.title,
                        author = "",
                        coverUrl = hit.coverUrl,
                        corner = 8.dp,
                        modifier = Modifier
                            .width(42.dp)
                            .height(62.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            hit.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hit.author.isNotBlank()) {
                            Text(
                                hit.author,
                                style = MaterialTheme.typography.labelSmall,
                                color = ink.copy(alpha = 0.58f),
                                maxLines = 1
                            )
                        }
                        // What the catalog adds over a bare search result: the
                        // book's own length and its chapters, in advance.
                        val facts = listOfNotNull(
                            hit.genre.takeIf { it.isNotBlank() },
                            hit.chapterCount.takeIf { it > 0 }?.let { "$it chapters" },
                            hit.pageCount.takeIf { it > 0 }?.let { "$it pp." }
                        ).joinToString(" · ")
                        if (facts.isNotBlank()) {
                            Text(
                                facts,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                    CurioIcon(CurioIcons.Add, "Add ${hit.title}", tint = accent, size = 18.dp)
                }
            }

            hits.take(8).forEach { hit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            // v426 — a comics hit brings its own length and its
                            // own kind with it, so the row lands on the shelf
                            // already saying what it is (a volume count is what
                            // a manga's progress is measured in).
                            addBook(
                                title = hit.title,
                                author = hit.author,
                                cover = hit.coverUrl,
                                chapters = hit.chapters.takeIf { it > 0 } ?: hit.volumes,
                                // v426b — the COMIC kind is carried onto the row
                                // too (it used to be saved as a plain book, so a
                                // comic added from a search lost the one label
                                // the member had just chosen for it).
                                kind = if (PersonalKinds.asksComicSources(kind)) kind
                                else PersonalKinds.BOOK
                            )
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BookCover(
                        title = hit.title,
                        author = "",
                        coverUrl = hit.coverUrl,
                        corner = 8.dp,
                        modifier = Modifier
                            .width(42.dp)
                            .height(62.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            hit.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hit.author.isNotBlank()) {
                            Text(
                                hit.author,
                                style = MaterialTheme.typography.labelSmall,
                                color = ink.copy(alpha = 0.58f),
                                maxLines = 1
                            )
                        }
                        // What the comics sources add over a bare result: which
                        // database answered, and how long the series is.
                        val facts = listOfNotNull(
                            hit.volumes.takeIf { it > 0 }?.let { "$it volumes" },
                            hit.chapters.takeIf { it > 0 }?.let { "$it chapters" },
                            hit.source.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (facts.isNotBlank()) {
                            Text(
                                facts,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                    CurioIcon(CurioIcons.Add, "Add ${hit.title}", tint = accent, size = 18.dp)
                }
            }
            if (searched && !searching && !failed && hits.isEmpty() && catalogHits.isEmpty()) {
                Text(
                    if (PersonalKinds.asksComicSources(kind))
                        "Nothing found in the comics sources. Add it yourself instead."
                    else "Nothing in the catalogue. Add the book yourself instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.copy(alpha = 0.62f)
                )
            }

            TextButton(onClick = {
                manual = true
                manualTitle = query
                catalogHits = emptyList()
                hits = emptyList()
            }) {
                Text(
                    if (PersonalKinds.asksComicSources(kind)) "Add it myself" else "Add the book myself",
                    color = accent
                )
            }
        }
    }
}

/** A quiet input for the sheet (the app's own field styling, no M3 outline). */
@Composable
private fun BookField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    ink: Color,
    accent: Color,
    imeAction: ImeAction = ImeAction.Next,
    onSearch: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = LoraFontFamily,
                fontSize = 15.sp,
                color = ink
            ),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = imeAction
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSearch?.invoke() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = TextStyle(
                                fontFamily = LoraFontFamily,
                                fontSize = 15.sp,
                                color = ink.copy(alpha = 0.4f)
                            )
                        )
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
internal fun ChapterStepper(
    count: Int,
    onChange: (Int) -> Unit,
    accent: Color,
    ink: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StepperButton(glyph = CurioIcons.ChevronLeft, label = "Fewer chapters", ink = ink) {
            onChange((count - 1).coerceAtLeast(0))
        }
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = personalAccentInk(),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(38.dp)
        )
        StepperButton(glyph = CurioIcons.ChevronRight, label = "More chapters", ink = ink) {
            onChange((count + 1).coerceAtMost(999))
        }
    }
}

@Composable
private fun StepperButton(glyph: String, label: String, ink: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(34.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CurioIcon(glyph, label, tint = ink.copy(alpha = 0.75f), size = 17.dp)
        }
    }
}


// ────────────────────────────────────────────────────────────────────────────
// Open Library
// ────────────────────────────────────────────────────────────────────────────

private val bookHttp: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
}

/**
 * Searches Open Library. Answers null when the catalogue could not be reached
 * (the sheet then offers the manual door instead of an error the member can do
 * nothing about).
 */
/** One third of the Search / Scan / Type row inside the add-book sheet. */
@Composable
private fun ScanDoor(
    glyph: String,
    label: String,
    selected: Boolean,
    accent: Color,
    ink: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        // v412 — opaque: the selected door is the accent mixed into the
        // unselected fill it replaces.
        color = if (selected) lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.22f)
        else MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                glyph,
                null,
                tint = if (selected) personalIconTint(accent) else ink.copy(alpha = 0.55f),
                size = 16.dp
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) personalIconTint(accent) else ink.copy(alpha = 0.7f)
            )
        }
    }
}

private fun searchOpenLibrary(query: String): List<BookHit>? = runCatching {
    val url = "https://openlibrary.org/search.json?q=" +
        java.net.URLEncoder.encode(query, "UTF-8") +
        "&limit=12&fields=title,author_name,cover_i"
    val request = Request.Builder().url(url).get().build()
    val body = bookHttp.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        response.body?.string().orEmpty()
    }
    if (body.isBlank()) return null
    val docs = JsonParser.parseString(body).asJsonObject
        .getAsJsonArray("docs") ?: return emptyList()
    docs.mapNotNull { element ->
        val obj = element.asJsonObject
        val title = obj.get("title")?.asString?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val author = obj.getAsJsonArray("author_name")
            ?.firstOrNull()?.asString.orEmpty()
        val coverId = obj.get("cover_i")?.takeIf { !it.isJsonNull }?.asLong
        BookHit(
            title = title,
            author = author,
            coverUrl = if (coverId != null && coverId > 0)
                "https://covers.openlibrary.org/b/id/$coverId-M.jpg" else ""
        )
    }
}.getOrNull()

/**
 * A generated cover's two tones: a small curated set of deep, literary
 * gradients (chosen so white title text is always legible), picked from the
 * title's own hash — so the same book always gets the same cover, and a
 * shelf of them looks hand-bound instead of random.
 */
private fun generatedCoverTones(title: String): Pair<Color, Color> {
    val index = (title.hashCode() and 0x7fffffff) % COVER_TONES.size
    return COVER_TONES[index]
}

private val COVER_TONES: List<Pair<Color, Color>> = listOf(
    Color(0xFF6B4E71) to Color(0xFF3B2A46),
    Color(0xFF2F4858) to Color(0xFF182C3A),
    Color(0xFF3F5E4A) to Color(0xFF243A2C),
    Color(0xFF8A4B39) to Color(0xFF5A2F24),
    Color(0xFF7C4A2E) to Color(0xFF4E2C1B),
    Color(0xFF40566B) to Color(0xFF26343F),
    Color(0xFF6D3B4A) to Color(0xFF452330),
    Color(0xFF5A5340) to Color(0xFF37331F)
)
