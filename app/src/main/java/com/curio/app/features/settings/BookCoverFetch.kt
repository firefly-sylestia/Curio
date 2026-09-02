package com.curio.app.features.settings

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.curio.app.data.CategoryId
import com.curio.app.data.TopicJsonLoader
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * v314 — one-by-one "fetch all book covers" action for Settings.
 *
 * The reveal's book poster already resolves each cover to a URL (the topic's
 * own `imageUrl`, else an Open Library title-cover fallback) and Coil serves
 * it from the shared disk cache installed in `MainActivity`. Tapping the
 * Settings row pre-fetches EVERY unique book cover into that disk cache so
 * posters render instantly (and offline) — one at a time, with a live
 * "12 / 301" counter on the row.
 */
object BookCoverFetch {

    const val TITLE = "Book covers"
    const val IDLE_SUBTITLE = "Tap to fetch cover images so books show offline"

    /** The URL the reveal would show for this book (authored URL, else the
     *  same Open Library title-cover fallback the reveal uses). */
    fun coverUrlFor(bookName: String, imageUrl: String): String =
        imageUrl.takeIf { it.isNotBlank() }
            ?: "https://covers.openlibrary.org/b/title/${Uri.encode(bookName)}-M.jpg"

    /**
     * Fetch every unique book cover into the shared Coil disk cache, one by
     * one. [onProgress] fires after each book with (done, total, failed).
     */
    suspend fun fetchAll(
        context: Context,
        onProgress: (done: Int, total: Int, failed: Int) -> Unit
    ) {
        val books = withContext(Dispatchers.Default) {
            runCatching { TopicJsonLoader.load(CategoryId.BOOKS) }.getOrNull().orEmpty()
        }
        val urls = books.map { coverUrlFor(it.name, it.imageUrl) }.distinct()
        val loader = Coil.imageLoader(context)
        val total = urls.size
        var done = 0
        var failed = 0
        onProgress(0, total, 0)
        for (url in urls) {
            val started = SystemClock.elapsedRealtime()
            val ok = runCatching {
                suspendCancellableCoroutine<Boolean> { cont ->
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        // Bulk pass: skip the memory cache (300 decoded covers
                        // would bloat heap) — the disk cache is what we're
                        // filling, and the reveal re-decodes from it on demand.
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .listener(
                            onSuccess = { _, _ -> cont.resume(true) },
                            onError = { _, _ -> cont.resume(false) }
                        )
                        .build()
                    val disposable = loader.enqueue(request)
                    cont.invokeOnCancellation { disposable.dispose() }
                }
            }.getOrDefault(false)
            if (ok) done++ else failed++
            onProgress(done, total, failed)
            // Be polite: at least ~150ms between fetches, so already-cached
            // covers (instant) don't blast through all 300 URLs in one burst.
            val took = SystemClock.elapsedRealtime() - started
            if (took < 150L) delay(150L - took)
        }
    }
}

/** The Settings row (rendered in Experiments) — tap to start the one-by-one
 *  fetch; live counter + progress bar while it runs, and a Cancel pill to
 *  stop it early (whatever was already cached stays cached). */
@Composable
fun BookCoverFetchRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var running by remember { mutableStateOf(false) }
    var stopped by remember { mutableStateOf(false) }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var failed by remember { mutableIntStateOf(0) }

    val subtitle = when {
        running -> if (total > 0) "Fetching $done / $total…" else "Fetching covers…"
        stopped -> if (failed > 0) "Stopped · $done cached · $failed failed"
            else "Stopped · $done covers cached"
        done > 0 -> if (failed > 0) "✓ $done covers cached · $failed failed"
            else "✓ $done covers cached"
        else -> BookCoverFetch.IDLE_SUBTITLE
    }

    Surface(
        onClick = {
            if (running) return@Surface
            stopped = false
            running = true
            done = 0
            total = 0
            failed = 0
            job = scope.launch {
                try {
                    BookCoverFetch.fetchAll(context) { d, t, f ->
                        done = d; total = t; failed = f
                    }
                } catch (_: kotlinx.coroutines.CancellationException) {
                    stopped = true
                } finally {
                    running = false
                    job = null
                }
            }
        },
        color = Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 13.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CurioIcon(
                    CurioIcons.Image, null,
                    tint = settingsCardAccentInk(),
                    size = 21.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(BookCoverFetch.TITLE, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (running) {
                    // v316b — cancel the in-flight fetch early; whatever was
                    // already cached stays on disk (tap again to continue).
                    Surface(
                        onClick = { job?.cancel() },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                } else if (done > 0) {
                    Text(
                        "$done cached",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (running && total > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
