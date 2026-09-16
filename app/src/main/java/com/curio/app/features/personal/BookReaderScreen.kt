package com.curio.app.features.personal

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.PersonalRepositoryHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipFile

@Composable
fun BookReaderScreen(navController: NavController, bookId: String) {
    val book by androidx.compose.runtime.produceState<com.curio.app.data.PersonalBookEntity?>(null, bookId) {
        PersonalRepositoryHolder.repo.observeBook(bookId).collect { value = it }
    }
    val context = LocalContext.current
    var pages by remember(bookId) { mutableStateOf<List<ReaderPage>>(emptyList()) }
    var error by remember(bookId) { mutableStateOf<String?>(null) }
    var page by remember(bookId) { mutableIntStateOf(0) }
    LaunchedEffect(book?.coverUrl) {
        val uri = book?.coverUrl ?: return@LaunchedEffect
        val result = withContext(Dispatchers.IO) { runCatching { readBook(context, uri) } }
        result.onSuccess { pages = it }.onFailure { error = "This file could not be opened in Curio." }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
            Text(book?.title ?: "Reader", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (pages.isNotEmpty()) Text("${page + 1}/${pages.size}", style = MaterialTheme.typography.labelMedium)
        }
        when {
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error!!, color = MaterialTheme.colorScheme.error) }
            pages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp)) {
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
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { page = (page - 1).coerceAtLeast(0) }, enabled = page > 0, modifier = Modifier.weight(1f)) { Text("Previous") }
                    Button(onClick = { page = (page + 1).coerceAtMost(pages.lastIndex) }, enabled = page < pages.lastIndex, modifier = Modifier.weight(1f)) { Text("Next") }
                }
            }
        }
    }
}

private data class ReaderPage(val text: String = "", val bitmap: Bitmap? = null)

private fun readBook(context: android.content.Context, value: String): List<ReaderPage> {
    val uri = android.net.Uri.parse(value)
    val type = context.contentResolver.getType(uri).orEmpty()
    return if (type == "application/pdf" || value.endsWith(".pdf", true)) readPdf(context, uri) else readEpub(context, uri)
}

private fun readPdf(context: android.content.Context, uri: android.net.Uri): List<ReaderPage> {
    val file = context.contentResolver.openFileDescriptor(uri, "r") ?: error("No PDF")
    val renderer = PdfRenderer(file)
    return try {
        (0 until renderer.pageCount).map { index ->
            val source = renderer.openPage(index)
            val bitmap = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            source.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            source.close()
            // PdfRenderer is intentionally kept lightweight here: the reader
            // uses Android's platform renderer without shipping a large PDF SDK.
            ReaderPage(bitmap = bitmap)
        }
    } finally { renderer.close(); file.close() }
}

private fun readEpub(context: android.content.Context, uri: android.net.Uri): List<ReaderPage> {
    val temp = java.io.File.createTempFile("curio-reader", ".epub", context.cacheDir)
    context.contentResolver.openInputStream(uri).use { input -> temp.outputStream().use { output -> input?.copyTo(output) } }
    return try {
        ZipFile(temp).use { zip ->
            zip.entries().asSequence().filter { it.name.endsWith(".xhtml", true) || it.name.endsWith(".html", true) }
                .map { entry -> zip.getInputStream(entry).bufferedReader().use { it.readText() }.replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim() }
                .filter { it.isNotBlank() }.map { ReaderPage(text = it) }.toList()
        }
    } finally { temp.delete() }
}

private fun readPdfPages(context: android.content.Context, uri: android.net.Uri): List<Bitmap> = emptyList()
