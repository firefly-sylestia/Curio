package com.curio.app.features.personal

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalRepositoryHolder
import com.curio.app.data.newPersonalBookId
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.personalAccent
import com.curio.app.ui.theme.personalIconTint
import com.curio.app.ui.theme.personalOnAccent
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import com.google.gson.JsonParser

/**
 * ISBN SCANNER — add a book by its cover.
 *
 * The member taps "Scan ISBN" in the add-book sheet, the device camera opens,
 * a barcode is detected, and the ISBN is looked up on Open Library. If the
 * book is found, it lands on the shelf immediately. If not, the member can
 * try again or type the title manually.
 *
 * This screen is a COMPOSABLE, not a route — it is shown as a full-screen
 * overlay inside the add-book sheet, so the shelf stays behind it.
 */
@Composable
fun IsbnScannerSheet(
    onDismiss: () -> Unit,
    onAdded: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = personalAccent()
    val ink = MaterialTheme.colorScheme.onSurface

    var status by remember { mutableStateOf<ScanStatus>(ScanStatus.Ready) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // The temp file for the camera capture.
    val tempFile = remember {
        val dir = context.cacheDir.resolve("isbn").also { it.mkdirs() }
        dir.resolve("capture_${System.currentTimeMillis()}.jpg")
    }
    val fileUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    // Camera launcher: takes a photo and returns a URI.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri = fileUri
            status = ScanStatus.Scanning
            scope.launch { scanIsbn(context, fileUri, onAdded) { status = it } }
        } else {
            status = ScanStatus.Ready
        }
    }

    // Camera permission launcher.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(fileUri)
        } else {
            status = ScanStatus.PermissionDenied
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val s = status) {
            is ScanStatus.Ready -> {                    CurioIcon(
                    CurioIcons.Screenshot,
                    null,
                    tint = accent,
                    size = 64.dp
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Scan a book's ISBN",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Point the camera at the barcode on the back cover",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))
                Surface(
                    onClick = {
                        status = ScanStatus.RequestingPermission
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    shape = RoundedCornerShape(50),
                    color = accent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                CurioIcon(
                    CurioIcons.Screenshot,
                    null,
                    tint = personalOnAccent(),
                    size = 20.dp
                )
                        Text(
                            "Open camera",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = personalOnAccent()
                        )
                    }
                }
            }

            is ScanStatus.RequestingPermission, is ScanStatus.Scanning -> {
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(48.dp)
                        .clip(RoundedCornerShape(50)),
                    color = accent
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (s is ScanStatus.Scanning) "Looking up ISBN\u2026" else "Requesting camera access\u2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.copy(alpha = 0.6f)
                )
            }

            is ScanStatus.Found -> {                    CurioIcon(
                    CurioIcons.TaskAlt,
                    null,
                    tint = Color(0xFF4CAF50),
                    size = 64.dp
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Found it!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    s.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ink.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                if (s.author.isNotBlank()) {
                    Text(
                        s.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            is ScanStatus.NotFound -> {
                CurioIcon(
                    CurioIcons.SearchOff,
                    null,
                    tint = ink.copy(alpha = 0.4f),
                    size = 64.dp
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "ISBN not found",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "No book matched that barcode. Try another cover or type the title.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = {
                            status = ScanStatus.Ready
                            cameraUri = null
                        },
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "Try again",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = personalIconTint(accent),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Text(
                            "Type instead",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = ink,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            is ScanStatus.PermissionDenied -> {
                CurioIcon(
                    CurioIcons.Screenshot,
                    null,
                    tint = ink.copy(alpha = 0.4f),
                    size = 64.dp
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Camera access needed",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Allow camera access in Settings to scan barcodes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            is ScanStatus.Error -> {
                Text(
                    "Something went wrong: ${s.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    }
}

// ── Status ────────────────────────────────────────────────────────────────

private sealed class ScanStatus {
    data object Ready : ScanStatus()
    data object RequestingPermission : ScanStatus()
    data object Scanning : ScanStatus()
    data class Found(val title: String, val author: String, val coverUrl: String) : ScanStatus()
    data object NotFound : ScanStatus()
    data object PermissionDenied : ScanStatus()
    data class Error(val message: String) : ScanStatus()
}

// ── ISBN lookup ───────────────────────────────────────────────────────────

private val http = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .build()

/**
 * Scans the captured photo for a barcode (ISBN), then looks up the book on
 * Open Library and adds it to the shelf. All network work happens off the
 * main thread; the caller's status callback runs on the main thread.
 */
private suspend fun scanIsbn(
    context: Context,
    uri: Uri,
    onAdded: (String) -> Unit,
    onStatus: (ScanStatus) -> Unit
) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        val scanner = BarcodeScanning.getClient()
        val barcodes = scanner.process(image).await()
        scanner.close()

        // Find the first ISBN barcode (format ISBN_10 or ISBN_13).
        val isbn = barcodes.firstNotNullOfOrNull { barcode ->
            when (barcode.format) {
                Barcode.FORMATISBN_13 -> barcode.rawValue
                Barcode.FORMATISBN_10 -> barcode.rawValue
                else -> null
            }
        }

        if (isbn == null) {
            onStatus(ScanStatus.NotFound)
            return
        }

        // Look up the ISBN on Open Library.
        onStatus(ScanStatus.Scanning)
        val book = lookupIsbn(isbn)

        if (book == null) {
            onStatus(ScanStatus.NotFound)
            return
        }

        // Add to the shelf.
        val id = newPersonalBookId()
        withContext(Dispatchers.IO) {
            runCatching {
                PersonalRepositoryHolder.repo.saveBook(
                    PersonalBookEntity(
                        id = id,
                        title = book.title,
                        author = book.author,
                        coverUrl = book.coverUrl,
                        pageCount = book.pages
                    )
                )
            }
        }

        onStatus(ScanStatus.Found(book.title, book.author, book.coverUrl))
        // Give the user a moment to see "Found it!" before navigating.
        kotlinx.coroutines.delay(1200)
        onAdded(id)
    } catch (e: Exception) {
        onStatus(ScanStatus.Error(e.message ?: "Unknown error"))
    }
}

/**
 * Open Library ISBN lookup: the edition's own title, author, cover and page
 * count. The edition is the authoritative source — the work can be a
 * different edition entirely.
 */
private suspend fun lookupIsbn(isbn: String): IsbnBookResult? = withContext(Dispatchers.IO) {
    runCatching {
        val url = "https://openlibrary.org/isbn/$isbn.json"
        val request = Request.Builder().url(url).get().build()
        val body = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            response.body?.string().orEmpty()
        }
        if (body.isBlank()) return@runCatching null
        val json = JsonParser.parseString(body).asJsonObject

        val title = json.get("title")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
        if (title.isBlank()) return@runCatching null

        val authors = json.getAsJsonArray("authors")?.mapNotNull { it.asJsonObject?.get("key")?.asString }.orEmpty()
        val authorName = if (authors.isNotEmpty()) {
            val authorJson = getJson("https://openlibrary.org${authors.first()}.json")
            authorJson?.get("name")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
        } else ""

        val covers = json.getAsJsonArray("covers")?.mapNotNull {
            it.takeIf { !it.isJsonNull }?.asLong
        }.orEmpty()
        val coverUrl = if (covers.isNotEmpty()) {
            "https://covers.openlibrary.org/b/id/${covers.first()}-L.jpg"
        } else ""

        val pages = json.get("number_of_pages")?.takeIf { !it.isJsonNull }?.asInt ?: 0

        IsbnBookResult(title, authorName, coverUrl, pages)
    }.getOrNull()
}

private fun getJson(url: String): com.google.gson.JsonObject? = runCatching {
    val request = Request.Builder().url(url).get().build()
    val body = http.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@use ""
        response.body?.string().orEmpty()
    }
    if (body.isBlank()) null else JsonParser.parseString(body).asJsonObject
}.getOrNull()

private data class IsbnBookResult(
    val title: String,
    val author: String,
    val coverUrl: String,
    val pages: Int
)
