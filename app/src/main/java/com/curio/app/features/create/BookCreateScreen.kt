package com.curio.app.features.create

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.curio.app.data.BookChapter
import com.curio.app.data.CaptureData
import com.curio.app.data.CaptureFormat
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioTopic
import com.curio.app.data.ExploreAction
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.launch

private data class DraftChapter(
    val title: String,
    val content: String
)

@Composable
fun BookCreateScreen(
    onClose: () -> Unit,
    onCreated: () -> Unit = onClose
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }
    var subtitle by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var paletteIndex by rememberSaveable { mutableStateOf(0) }
    var showChapterEditor by rememberSaveable { mutableStateOf(false) }
    var editingChapter by rememberSaveable { mutableStateOf(-1) }
    var saving by rememberSaveable { mutableStateOf(false) }
    val chapters = remember {
        mutableStateListOf(DraftChapter("Chapter 1", ""))
    }

    val palettes = listOf(
        "Dawn" to listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary),
        "Tide" to listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary),
        "Meadow" to listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary),
        "Ink" to listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
    )
    val selectedPalette = palettes[paletteIndex].second
    val canCreate = title.isNotBlank() && !saving
    val writtenChapters = chapters.count { it.content.isNotBlank() }

    fun saveBook() {
        if (!canCreate) return
        saving = true
        scope.launch {
            val chapterModels = chapters.mapIndexed { index, chapter ->
                BookChapter(
                    number = index + 1,
                    title = chapter.title.ifBlank { "Chapter ${index + 1}" },
                    pageStart = 0,
                    pageEnd = 0,
                    summary = chapter.content.trim().replace(Regex("\\s+"), " ").take(180)
                )
            }
            val chapterText = chapters.mapIndexed { index, chapter ->
                "Chapter ${index + 1}: ${chapter.title.ifBlank { "Untitled" }}\n${chapter.content.trim()}"
            }.joinToString("\n\n")
            val topic = CurioTopic(
                id = newCreatedEntryId(),
                categoryId = CategoryId.BOOKS,
                subtype = "Book",
                name = title.trim(),
                teaser = description.trim().ifBlank { "A book created in Curio." },
                imageUrl = "",
                exploreAction = ExploreAction("Read", title.trim(), 0, "Read this personal book at your own pace."),
                byline = author.trim(),
                pageCount = null,
                synopsis = description.trim().ifBlank { null },
                chapters = chapterModels
            )
            val entry = CurioEntry(
                id = newCreatedEntryId(),
                topic = topic,
                format = CaptureFormat.Marginalia,
                captureData = CaptureData.Marginalia(
                    journalText = buildString {
                        if (description.isNotBlank()) append(description.trim()).append("\n\n")
                        if (chapterText.isNotBlank()) append(chapterText)
                    },
                    quotes = emptyList()
                ),
                title = title.trim(),
                capturedAtMillis = System.currentTimeMillis()
            )
            runCatching { saveCreatedEntryToPersonalCollection(context, entry) }
                .onSuccess { onCreated() }
                .onFailure { saving = false }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CurioIcon(CurioIcons.Close, "Close book creator", tint = MaterialTheme.colorScheme.onSurface, size = 20.dp)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CurioIcon(CurioIcons.MenuBook, null, tint = MaterialTheme.colorScheme.primary, size = 19.dp)
                        Text("New book", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    }
                    Text(
                        if (writtenChapters == 0) "A book begins with a page" else "$writtenChapters chapter${if (writtenChapters == 1) "" else "s"} written",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.size(44.dp))
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // A more book-like opening spread: cover + metadata instead of a generic form card.
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Surface(shape = RoundedCornerShape(12.dp), shadowElevation = 3.dp, modifier = Modifier.size(width = 132.dp, height = 190.dp)) {
                            Box(Modifier.fillMaxSize().background(Brush.linearGradient(selectedPalette)).padding(16.dp), contentAlignment = Alignment.BottomStart) {
                                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text("CURIO EDITION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp), color = contentInk(selectedPalette[1]).copy(alpha = .68f))
                                    Spacer(Modifier.height(5.dp))
                                    Text(title.ifBlank { "Untitled" }, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp), color = contentInk(selectedPalette[1]), maxLines = 5)
                                    if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = contentInk(selectedPalette[1]).copy(alpha = .78f), maxLines = 2)
                                    Text(author.ifBlank { "Your name" }, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = contentInk(selectedPalette[1]).copy(alpha = .78f), maxLines = 1)
                                }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                            Text("The edition", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                            Text("Shape the cover first, then build the chapters inside it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                palettes.forEachIndexed { index, item ->
                                    val selected = paletteIndex == index
                                    Surface(onClick = { paletteIndex = index }, shape = RoundedCornerShape(15.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .35f)) else null, modifier = Modifier.height(38.dp)) {
                                        Row(Modifier.padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(shape = RoundedCornerShape(5.dp), color = item.second[1], modifier = Modifier.size(14.dp)) {}
                                            Text(item.first, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                        Text("Title page", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp), color = MaterialTheme.colorScheme.primary)
                        BookTextField(title, { title = it }, "Book title", "Name the world you are making", TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface), true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BookTextField(author, { author = it }, "Author", "You", MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface), true, Modifier.weight(1f))
                            BookTextField(subtitle, { subtitle = it }, "Subtitle", "Optional", MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface), true, Modifier.weight(1f))
                        }
                        BookTextField(description, { description = it }, "About this book", "A short description for the back cover", TextStyle(fontSize = 17.sp, lineHeight = 28.sp, color = MaterialTheme.colorScheme.onSurface), false, Modifier.fillMaxWidth().heightIn(min = 120.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
                    modifier = Modifier.fillMaxWidth().animateContentSize(tween(220, easing = FastOutSlowInEasing))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CurioIcon(CurioIcons.MenuBook, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, size = 19.dp) }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Chapters", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Edit the structure and write each chapter separately.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${chapters.size}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                        }

                        chapters.forEachIndexed { index, chapter ->
                            Surface(
                                onClick = { editingChapter = index; showChapterEditor = true },
                                shape = RoundedCornerShape(19.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f)) {
                                        Text(chapter.title.ifBlank { "Untitled chapter" }, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                        Text(if (chapter.content.isBlank()) "Empty · tap to write" else "${chapter.content.trim().split(Regex("\\s+")).size} words", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    CurioIcon(CurioIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                                }
                            }
                        }

                        Surface(onClick = { editingChapter = chapters.size; showChapterEditor = true }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                CurioIcon(CurioIcons.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 18.dp)
                                Spacer(Modifier.width(7.dp))
                                Text("Add chapter", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(92.dp))
            }
        }

        Surface(
            shape = RoundedCornerShape(25.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(Modifier.navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Text(if (canCreate) "Ready to shelve" else "A title is all you need", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    Text("Saved books live in your Personal collection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(onClick = ::saveBook, enabled = canCreate, shape = RoundedCornerShape(20.dp), color = if (canCreate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.height(46.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AnimatedContent(targetState = saving, transitionSpec = { fadeIn(tween(120)) + scaleIn(initialScale = .92f) togetherWith fadeOut(tween(90)) + scaleOut(targetScale = .92f) }, label = "book-save-icon") { busy ->
                            CurioIcon(if (busy) CurioIcons.HourglassEmpty else CurioIcons.Check, null, tint = MaterialTheme.colorScheme.onPrimary, size = 18.dp)
                        }
                        Text(if (saving) "Saving…" else "Save book", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showChapterEditor,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = .98f),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = .98f),
            modifier = Modifier.fillMaxSize()
        ) {
            ChapterEditorOverlay(
                chapterNumber = editingChapter + 1,
                initialTitle = chapters.getOrNull(editingChapter)?.title.orEmpty().ifBlank { "Chapter ${editingChapter + 1}" },
                initialContent = chapters.getOrNull(editingChapter)?.content.orEmpty(),
                canDelete = chapters.size > 1 && editingChapter in chapters.indices,
                onDismiss = { showChapterEditor = false },
                onDelete = {
                    if (editingChapter in chapters.indices && chapters.size > 1) chapters.removeAt(editingChapter)
                    showChapterEditor = false
                },
                onSave = { chapterTitle, content ->
                    val draft = DraftChapter(chapterTitle, content)
                    if (editingChapter in chapters.indices) chapters[editingChapter] = draft
                    else chapters.add(draft)
                    showChapterEditor = false
                }
            )
        }
    }
}

@Composable
private fun ChapterEditorOverlay(
    chapterNumber: Int,
    initialTitle: String,
    initialContent: String,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by rememberSaveable(initialTitle, chapterNumber) { mutableStateOf(initialTitle) }
    var content by rememberSaveable(initialContent, chapterNumber) { mutableStateOf(initialContent) }
    val hasText = title.isNotBlank() || content.isNotBlank()

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(onClick = onDismiss, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CurioIcon(CurioIcons.Close, "Close chapter editor", tint = MaterialTheme.colorScheme.onSurface, size = 20.dp) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Chapter $chapterNumber", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Text("Edit page", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(onClick = { onSave(title.trim(), content) }, enabled = hasText, shape = RoundedCornerShape(18.dp), color = if (hasText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.height(42.dp)) {
                    Text("Done", modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp), color = if (hasText) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("CHAPTER $chapterNumber", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.7.sp), color = MaterialTheme.colorScheme.primary)
                BasicTextField(value = title, onValueChange = { title = it }, singleLine = true, textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> Box { if (title.isBlank()) Text("Chapter title", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .2f))); inner() } })
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f)), modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(value = content, onValueChange = { content = it }, textStyle = TextStyle(fontSize = 18.sp, lineHeight = 32.sp, color = MaterialTheme.colorScheme.onSurface), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth().heightIn(min = 520.dp).padding(horizontal = 20.dp, vertical = 20.dp), decorationBox = { inner -> Box { if (content.isBlank()) Text("Write this chapter...\n\nGive it room. Let paragraphs breathe.\n\nThis is your book, so the page can sound like you.", style = TextStyle(fontSize = 18.sp, lineHeight = 32.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .28f))); inner() } })
                }
                if (canDelete) {
                    Surface(onClick = onDelete, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text("Delete chapter", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun BookTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, textStyle: TextStyle, singleLine: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(17.dp), color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f)), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                BasicTextField(value = value, onValueChange = onValueChange, singleLine = singleLine, textStyle = textStyle, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> Box { if (value.isBlank()) Text(placeholder, style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .50f))); inner() } })
            }
        }
    }
}

private fun contentInk(accent: Color): Color {
    val luminance = (0.299f * accent.red) + (0.587f * accent.green) + (0.114f * accent.blue)
    return if (luminance > 0.58f) Color(0xFF1B1B1D) else Color.White
}
