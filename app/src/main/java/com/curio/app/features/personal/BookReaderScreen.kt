package com.curio.app.features.personal

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalRepositoryHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/**
 * v389 — THE BOOK'S READER.
 *
 * The page renders whatever FILE the book carries: a PDF through Android's own
 * `PdfRenderer` (no large PDF SDK rides in the APK), an EPUB through its own
 * zip of XHTML, and plain text as paragraphs. The file is read from the book's
 * own storage path ([BookFiles]) — a path the app owns and can open offline,
 * not a `content://` handle that dies with the permission that came with it.
 */
@Composable
fun BookReaderScreen(navController: NavController, bookId: String) {
    val book by produceState<PersonalBookEntity?>(null, bookId) {
        PersonalRepositoryHolder.repo.observeBook(bookId).collect { value = it }
    }
    val context = LocalContext.current
    var pages by remember(bookId) { mutableStateOf<List<ReaderPage>>(emptyList()) }
    var error by remember(bookId) { mutableStateOf<String?>(null) }
    var page by remember(bookId) { mutableIntStateOf(0) }
    // The document the book carries: its own file, or the legacy `content://`
    // handle an import parked in `coverUrl` before it had a column of its own.
    val document = book?.let { BookFiles.documentOf(it.documentPath, it.coverUrl) }.orEmpty()
    LaunchedEffect(document) {
        if (document.isBlank()) {
            error = "This book has no file yet \u2014 add its PDF or EPUB from the book's page."
            return@LaunchedEffect
        }
        error = null
        val result = withContext(Dispatchers.IO) { runCatching { readBook(context, document) } }
        result.onSuccess { pages = it }
            .onFailure { error = "This file could not be opened in Curio." }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Text("\u2039", style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                book?.title ?: "Reader",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            if (pages.isNotEmpty()) {
                Text("${page + 1}/${pages.size}", style = MaterialTheme.typography.labelMedium)
            }
        }
        when {
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    error.orEmpty(),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            pages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(24.dp)
                ) {
                    item {
                        val current = pages[page]
                        if (current.bitmap != null) {
                            Image(
                                bitmap = current.bitmap.asImageBitmap(),
                                contentDescription = "Page ${page + 1}",
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(current.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { page = (page - 1).coerceAtLeast(0) },
                        enabled = page > 0,
                        modifier = Modifier.weight(1f)
                    ) { Text("Previous") }
                    Button(
                        onClick = { page = (page + 1).coerceAtMost(pages.lastIndex) },
                        enabled = page < pages.lastIndex,
                        modifier = Modifier.weight(1f)
                    ) { Text("Next") }
                }
            }
        }
    }
}

private data class ReaderPage(val text: String = "", val bitmap: Bitmap? = null)

/** A plain filesystem path (the app's own copy), or null for a `content://`. */
private fun localFile(value: String): File? {
    if (value.startsWith("content://")) return null
    return File(value.removePrefix("file://"))
}

private fun readBook(context: android.content.Context, value: String): List<ReaderPage> {
    val file = localFile(value)
    val name = (file?.name ?: value).lowercase()
    val type = if (file == null) {
        context.contentResolver.getType(android.net.Uri.parse(value)).orEmpty()
    } else {
        ""
    }
    return when {
        type == "application/pdf" || name.endsWith(".pdf") -> readPdf(context, value, file)
        type.startsWith("text/") || name.endsWith(".txt") -> readText(context, value, file)
        else -> readEpub(context, value, file)
    }
}

private fun readPdf(context: android.content.Context, value: String, file: File?): List<ReaderPage> {
    val descriptor = if (file != null) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    } else {
        context.contentResolver.openFileDescriptor(android.net.Uri.parse(value), "r")
            ?: error("No PDF")
    }
    val renderer = PdfRenderer(descriptor)
    return try {
        (0 until renderer.pageCount).map { index ->
            val source = renderer.openPage(index)
            val bitmap = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            source.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            source.close()
            ReaderPage(bitmap = bitmap)
        }
    } finally {
        renderer.close()
        descriptor.close()
    }
}

private fun readText(context: android.content.Context, value: String, file: File?): List<ReaderPage> {
    val text = if (file != null) {
        file.readText()
    } else {
        context.contentResolver.openInputStream(android.net.Uri.parse(value))
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
    }
    return text.split(Regex("\\n{2,}"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { ReaderPage(text = it) }
}

private fun readEpub(context: android.content.Context, value: String, file: File?): List<ReaderPage> {
    val temp = if (file == null) {
        File.createTempFile("curio-reader", ".epub", context.cacheDir).also { target ->
            context.contentResolver.openInputStream(android.net.Uri.parse(value)).use { input ->
                target.outputStream().use { output -> input?.copyTo(output) }
            }
        }
    } else {
        null
    }
    val source = file ?: temp ?: error("No file")
    return try {
        ZipFile(source).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.endsWith(".xhtml", true) || it.name.endsWith(".html", true) }
                .map { entry ->
                    zip.getInputStream(entry).bufferedReader().use { it.readText() }
                        .replace(Regex("<[^>]+>"), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                }
                .filter { it.isNotBlank() }
                .map { ReaderPage(text = it) }
                .toList()
        }
    } finally {
        temp?.delete()
    }
}
