package com.curio.app.features.personal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.newPersonalBookId
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.google.gson.JsonParser

private data class IsbnBookResult(val title: String, val author: String, val coverUrl: String, val pages: Int)

@Composable
fun IsbnScannerSheet(onDismiss: () -> Unit, onAdded: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var detected by remember { mutableStateOf<IsbnBookResult?>(null) }
    var detectedIsbn by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastValue by remember { mutableStateOf("") }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
    LaunchedEffect(Unit) { if (!permission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (permission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    val previewView = PreviewView(viewContext)
                    val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                        analysis.setAnalyzer(executor) { proxy ->
                            val image = proxy.image
                            if (image == null) { proxy.close(); return@setAnalyzer }
                            val input = InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees)
                            BarcodeScanning.getClient().process(input).addOnSuccessListener { codes ->
                                val value = codes.firstNotNullOfOrNull { code ->
                                    if (code.format == Barcode.FORMAT_EAN_13 || code.format == Barcode.FORMAT_EAN_8 || code.format == Barcode.FORMAT_UPC_A) normalizeIsbn(code.rawValue) else null
                                }
                                if (!value.isNullOrBlank() && value != lastValue && !loading) {
                                    lastValue = value
                                    detectedIsbn = value
                                    loading = true
                                    scope.launch {
                                        val result = lookupIsbn(value)
                                        detected = result
                                        error = if (result == null) "No book was found for ISBN $value." else null
                                        loading = false
                                    }
                                }
                            }.addOnCompleteListener { proxy.close() }
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    }, ContextCompat.getMainExecutor(viewContext))
                    previewView
                }
            )
            Box(Modifier.align(Alignment.Center).size(280.dp, 170.dp).clip(RoundedCornerShape(20.dp)).background(Color.Transparent))
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Point at the ISBN barcode", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Scanning happens live inside Curio", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera access is needed", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
            }
        }
        OutlinedButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).padding(18.dp)) { Text("Cancel") }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
    }

    detected?.let { book ->
        ModalBottomSheet(onDismissRequest = { detected = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Book detected", style = MaterialTheme.typography.titleLarge)
                Text(book.title, style = MaterialTheme.typography.headlineSmall)
                if (book.author.isNotBlank()) Text(book.author, style = MaterialTheme.typography.bodyLarge)
                Text("ISBN $detectedIsbn", style = MaterialTheme.typography.labelMedium)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        scope.launch {
                            val id = saveScannedBook(book)
                            detected = null
                            onAdded(id)
                        }
                    }) { Text("Add to shelf") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val id = saveScannedBook(book)
                            detected = null
                            onAdded(id)
                        }
                    }) { Text("Open book") }
                }
                Text("Metadata from Open Library. Availability and licensing vary by edition.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private suspend fun saveScannedBook(book: IsbnBookResult): String = withContext(Dispatchers.IO) {
    val id = newPersonalBookId()
    PersonalRepositoryHolder.repo.saveBook(PersonalBookEntity(id = id, title = book.title, author = book.author, coverUrl = book.coverUrl, pageCount = book.pages))
    id
}

private fun normalizeIsbn(raw: String?): String? {
    val value = raw?.filter { it.isDigit() || it == 'X' } ?: return null
    return when (value.length) { 10, 13 -> value; 12 -> "0$value"; else -> null }
}

private val isbnHttp = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
private suspend fun lookupIsbn(isbn: String): IsbnBookResult? = withContext(Dispatchers.IO) { runCatching {
    val body = isbnHttp.newCall(Request.Builder().url("https://openlibrary.org/isbn/$isbn.json").build()).execute().use { response -> if (!response.isSuccessful) return@runCatching null; response.body?.string().orEmpty() }
    val json = JsonParser.parseString(body).asJsonObject
    val title = json.get("title")?.asString.orEmpty().takeIf { it.isNotBlank() } ?: return@runCatching null
    val authorKey = json.getAsJsonArray("authors")?.firstOrNull()?.asJsonObject?.get("key")?.asString
    val author = authorKey?.let { key -> isbnHttp.newCall(Request.Builder().url("https://openlibrary.org$key.json").build()).execute().use { JsonParser.parseString(it.body?.string().orEmpty()).asJsonObject.get("name")?.asString.orEmpty() } }.orEmpty()
    val cover = json.getAsJsonArray("covers")?.firstOrNull()?.asLong?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }.orEmpty()
    IsbnBookResult(title, author, cover, json.get("number_of_pages")?.asInt ?: 0)
}.getOrNull() }
