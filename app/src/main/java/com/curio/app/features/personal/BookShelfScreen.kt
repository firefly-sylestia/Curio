package com.curio.app.features.personal

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
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
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.newPersonalBookId
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
    val books by produceState(initialValue = emptyList<PersonalBookEntity>()) {
        runCatching {
            PersonalRepositoryHolder.repo.observeBooks().collect { value = it }
        }
    }
    var addOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PersonalBookEntity?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        PersonalHeader(
            title = "My shelf",
            subtitle = when (books.size) {
                0 -> "Books you are reading with a reason to write"
                1 -> "1 book"
                else -> "${books.size} books"
            },
            onBack = { navController.popBackStack() },
            action = {
                PersonalHeaderAction(
                    glyph = CurioIcons.Add,
                    label = "Add a book",
                    onClick = { addOpen = true }
                )
            }
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
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = books, key = { it.id }) { book ->
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

/** One book on the shelf: cover, name, progress. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookShelfCard(
    book: PersonalBookEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
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
        BookCover(
            title = book.title,
            author = book.author,
            coverUrl = book.coverUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.66f)
                .clip(RoundedCornerShape(14.dp)),
            corner = 14.dp
        )
        Spacer(Modifier.height(9.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (book.author.isNotBlank()) {
            Text(
                book.author,
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(
            progress = { book.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50)),
            color = accent,
            trackColor = accent.copy(alpha = 0.16f)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            when {
                book.isFinished -> "Finished"
                book.totalChapters <= 0 -> "Chapters not set"
                book.currentChapter <= 0 -> "${book.totalChapters} chapters"
                else -> "Ch ${book.currentChapter} of ${book.totalChapters}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = ink.copy(alpha = 0.62f)
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
        if (coverUrl.isNotBlank()) {
            // The generated cover stays UNDER the artwork: it is the loading
            // and the error state at once, and the spine shows through a
            // cover that has not arrived yet.

            androidx.compose.foundation.Image(
                painter = rememberAsyncImagePainter(coverUrl),
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
    val coverUrl: String
)

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
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = personalAccent()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
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

    /**
     * Puts the book on the shelf.
     *
     * [catalogId] and [pages] are filled when the book came from Curio's own
     * catalog: the id is what lets the book's page read the real chapter names,
     * page ranges and summaries back out of the topic JSON, and the page count
     * gives "how long is this book" an answer that did not come from a guess.
     */
    fun addBook(
        title: String,
        author: String,
        cover: String,
        chapters: Int,
        catalogId: String = "",
        pages: Int = 0
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
                            pageCount = pages.coerceAtLeast(0)
                        )
                    )
                }
            }
            onAdded(id)
        }
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
            Text(
                if (manual) "Add it yourself" else "Find a book",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = ink
            )

            if (manual) {
                BookField(value = manualTitle, onValueChange = { manualTitle = it }, placeholder = "Title", ink = ink, accent = accent)
                BookField(value = manualAuthor, onValueChange = { manualAuthor = it }, placeholder = "Author", ink = ink, accent = accent)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "How many chapters?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )
                    ChapterStepper(count = manualChapters, onChange = { manualChapters = it }, accent = accent, ink = ink)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { manual = false }) { Text("Search instead") }
                    TextButton(onClick = {
                        addBook(manualTitle, manualAuthor, "", manualChapters)
                    }) { Text("Add to shelf", color = accent) }
                }
                return@Column
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
                    scope.launch {
                        // CURIOS'S OWN CATALOG FIRST: instant, offline, and the
                        // only source that also carries the chapter list — so
                        // the book arrives with its real table of contents.
                        val local = withContext(Dispatchers.IO) { BookCatalog.search(text) }
                        catalogHits = local
                        // …THEN the wider catalogue, for anything Curio does not
                        // have. Its failure only matters when the app's own
                        // shelf came up empty, so an offline phone still gets a
                        // useful answer instead of an apology.
                        val remote = withContext(Dispatchers.IO) { searchOpenLibrary(text) }
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
                            addBook(hit.title, hit.author, hit.coverUrl, 0)
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
                    }
                    CurioIcon(CurioIcons.Add, "Add ${hit.title}", tint = accent, size = 18.dp)
                }
            }
            if (searched && !searching && !failed && hits.isEmpty() && catalogHits.isEmpty()) {
                Text(
                    "Nothing in the catalogue. Add the book yourself instead.",
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
                Text("Add the book myself", color = accent)
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
            color = accent,
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
