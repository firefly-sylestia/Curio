package com.curio.app.features.personal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * v448 — WHAT IS ON THE PAGE, AS A PICTURE YOU CAN SEND.
 *
 * The member: *"for share, when sharing from 3 dot open a crop selection to be able
 * to only select the cropped image of that part of the book keeping the color as it
 * is for the selected. like the screenshort look understand? and it hides the dock
 * and header too. and from there user can share it"*.
 *
 * So the ⋯ menu's share is joined by a **Snapshot**: the reader puts its chrome
 * away, takes the picture of what it is showing, and hands it to a crop frame the
 * member drags over the part they want — corners to size it, anywhere inside to
 * move it — and the cropped piece goes to the system's share sheet as a PNG.
 *
 * Three rules make it honest:
 *
 *  - **The picture is the SCREEN's, not the app's idea of the page.** Nothing here
 *    re-renders the book: the window is copied pixel for pixel, so what the member
 *    sends is what they were looking at — the page's own paper and ink (the
 *    member's "keeping the color as it is"), a photograph of a PDF page, an image an
 *    EPUB dropped in, a highlight's wash, at the type size and margins they read at.
 *    [captureReaderScreen] tries `PixelCopy` first (the platform's own screenshot
 *    door, which is the only one that sees a hardware-accelerated surface) and falls
 *    back to drawing the view into a bitmap if the copy is refused.
 *  - **The chrome is OUT of the picture.** The reader hides its own head and foot
 *    (and the selection's dock) BEFORE the copy is taken and waits for them to
 *    leave, so no pill can land in the middle of the member's crop.
 *  - **The crop is a PICTURE, never a re-layout.** The rectangle is kept as
 *    FRACTIONS of the capture, so a phone, a tablet and a landscape window all crop
 *    the same part, and the result is one `Bitmap.createBitmap` — no scaling, no
 *    filter, no round trip through a composable that could redraw the page
 *    differently.
 */
internal suspend fun captureReaderScreen(view: View): Bitmap? {
    val width = view.width
    val height = view.height
    if (width <= 0 || height <= 0) return null
    val bitmap = runCatching {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }.getOrNull() ?: return null
    val window = (view.context as? Activity)?.window
    val copied = window != null && runCatching {
        suspendCancellableCoroutine { continuation ->
            PixelCopy.request(
                window,
                bitmap,
                { result -> continuation.resume(result == PixelCopy.SUCCESS) },
                Handler(Looper.getMainLooper())
            )
        }
    }.getOrElse { false }
    if (copied) return bitmap
    // ── THE FALLBACK: DRAW THE VIEW ITSELF ─────────────────────────────
    //
    // A refused copy (no window yet, a surface the platform will not hand over) is
    // not "no picture": the view can still draw itself into the same bitmap, which
    // is what this app's own share cards do. It is a shade less faithful — anything
    // drawn in a hardware layer of its own (a video, a SurfaceView) is missing from
    // it — and for a page of words it is exactly the same picture.
    return runCatching {
        view.draw(AndroidCanvas(bitmap))
        bitmap
    }.getOrNull()
}

/**
 * The cropped piece, as a PNG in the app's share cache, handed to the system.
 *
 * Same door the app's share cards use (a `FileProvider` URI flagged read-only, one
 * `ACTION_SEND`), because a picture written to a private folder and named with a
 * `file://` path is exactly what a modern Android refuses to share.
 */
internal fun shareReaderSnapshot(context: Context, bitmap: Bitmap): Boolean {
    val folder = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(folder, "reader-page-${System.currentTimeMillis()}.png")
    val saved = runCatching {
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    }.isSuccess
    if (!saved) return false
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull() ?: return false
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching {
        context.startActivity(Intent.createChooser(send, "Share this page"))
    }.isSuccess
}

/**
 * THE CROP FRAME.
 *
 * The capture is drawn full-bleed under a scrim, and the member drags a rectangle
 * over the part they want: the four corners size it, anywhere inside moves it, and
 * nothing else on this surface answers a touch (so the page under it cannot scroll
 * or turn while the frame is up).
 */
@Composable
internal fun ReaderSnapshotCrop(
    bitmap: Bitmap,
    palette: ReaderPalette,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // The crop, in FRACTIONS of the picture (see the file's note). It opens as a
    // generous frame in the upper half — where a page's words are — rather than as
    // the whole screen, because "crop this" is the reason the member is here.
    var frame by remember {
        mutableStateOf(RectF(0.06f, 0.10f, 0.94f, 0.62f))
    }
    var grab by remember { mutableStateOf<SnapshotGrab?>(null) }
    var sharing by remember { mutableStateOf(false) }
    val handleReach = with(density) { 30.dp.toPx() }
    val minFraction = 0.12f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.matchParentSize()
        )
        // ── THE SCRIM, AND THE FRAME'S OWN EDGES ─────────────────────────
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(bitmap) {
                    detectDragGestures(
                        onDragStart = { at ->
                            grab = snapshotGrab(at, frame, size.width, size.height, handleReach)
                        },
                        onDragEnd = { grab = null },
                        onDragCancel = { grab = null }
                    ) { change, delta ->
                        change.consume()
                        val dx = delta.x / size.width.toFloat()
                        val dy = delta.y / size.height.toFloat()
                        val held = grab ?: return@detectDragGestures
                        frame = snapshotDrag(frame, held, dx, dy, minFraction)
                    }
                }
        ) {
            val left = frame.left * size.width
            val top = frame.top * size.height
            val right = frame.right * size.width
            val bottom = frame.bottom * size.height
            val scrim = palette.ink.copy(alpha = 0.55f)
            // Four rectangles rather than a clip: what is INSIDE the frame stays
            // untouched, which is the whole promise of a crop.
            drawRect(scrim, Offset.Zero, Size(size.width, top))
            drawRect(scrim, Offset(0f, bottom), Size(size.width, size.height - bottom))
            drawRect(scrim, Offset(0f, top), Size(left, bottom - top))
            drawRect(scrim, Offset(right, top), Size(size.width - right, bottom - top))
            val edge = palette.paper
            drawRect(
                color = edge,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(2.dp.toPx())
            )
            val radius = 6.dp.toPx()
            listOf(
                Offset(left, top),
                Offset(right, top),
                Offset(left, bottom),
                Offset(right, bottom)
            ).forEach { corner ->
                drawCircle(edge, radius, corner)
                drawCircle(palette.accent, radius * 0.55f, corner)
            }
        }

        // ── AND THE ONE THING TO DO WITH IT ─────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onDone,
                shape = RoundedCornerShape(50),
                color = palette.surface,
                contentColor = palette.ink,
                shadowElevation = 8.dp,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Cancel",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                )
            }
            Surface(
                onClick = {
                    if (sharing) return@Surface
                    sharing = true
                    scope.launch {
                        val cropped = snapshotCropOf(bitmap, frame)
                        withContext(Dispatchers.IO) { shareReaderSnapshot(context, cropped) }
                        cropped.recycle()
                        sharing = false
                        onDone()
                    }
                },
                shape = RoundedCornerShape(50),
                color = palette.accent,
                // The ink that reads on that fill, MEASURED (the same helper the
                // highlight discs use) rather than assumed.
                contentColor = journalInkOn(palette.accent),
                shadowElevation = 8.dp,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (sharing) "Sharing\u2026" else "Share",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                )
            }
        }
    }
}

/** Which part of the frame the finger took hold of. */
private sealed interface SnapshotGrab {
    /** The whole frame, moved rather than sized. */
    data object Body : SnapshotGrab
    data class Corner(val x: Float, val y: Float) : SnapshotGrab
}

/**
 * WHAT THIS TOUCH IS: a corner, or the body.
 *
 * A corner wins over the body when they overlap (its reach is [reach] px), because
 * a member who is near a corner almost always means to size the frame — and a
 * frame that could not be sized without first moving it a pixel away would be a
 * fight with the tool.
 */
private fun snapshotGrab(
    at: Offset,
    frame: RectF,
    width: Int,
    height: Int,
    reach: Float
): SnapshotGrab? {
    if (width <= 0 || height <= 0) return null
    val x = at.x / width.toFloat()
    val y = at.y / height.toFloat()
    val reachX = reach / width.toFloat()
    val reachY = reach / height.toFloat()
    val corners = listOf(
        0f to 0f,
        1f to 0f,
        0f to 1f,
        1f to 1f
    )
    for ((cx, cy) in corners) {
        val px = if (cx == 0f) frame.left else frame.right
        val py = if (cy == 0f) frame.top else frame.bottom
        if (kotlin.math.abs(x - px) <= reachX && kotlin.math.abs(y - py) <= reachY) {
            return SnapshotGrab.Corner(cx, cy)
        }
    }
    return if (frame.contains(x, y)) SnapshotGrab.Body else null
}

/**
 * THE FRAME, AFTER [dx] / [dy] OF A DRAG (both in fractions of the picture).
 *
 * Everything is clamped to the picture and held to [minFraction] in each
 * direction, so the frame can never be dragged off the page or collapsed to
 * nothing: a crop of a whole page and a crop of a word are both legal, a crop of
 * zero pixels is not.
 */
private fun snapshotDrag(
    frame: RectF,
    held: SnapshotGrab,
    dx: Float,
    dy: Float,
    minFraction: Float
): RectF {
    val next = RectF(frame)
    when (held) {
        SnapshotGrab.Body -> {
            // Moved with its own size kept: the frame slides, it does not run out
            // of the page at one edge before the other.
            val shiftX = dx.coerceIn(-next.left, 1f - next.right)
            val shiftY = dy.coerceIn(-next.top, 1f - next.bottom)
            next.offset(shiftX, shiftY)
        }
        is SnapshotGrab.Corner -> {
            if (held.x == 0f) {
                next.left = (next.left + dx).coerceIn(0f, next.right - minFraction)
            } else {
                next.right = (next.right + dx).coerceIn(next.left + minFraction, 1f)
            }
            if (held.y == 0f) {
                next.top = (next.top + dy).coerceIn(0f, next.bottom - minFraction)
            } else {
                next.bottom = (next.bottom + dy).coerceIn(next.top + minFraction, 1f)
            }
        }
    }
    return next
}

/** The frame, as pixels of [bitmap] — one crop, no scaling. */
private fun snapshotCropOf(bitmap: Bitmap, frame: RectF): Bitmap {
    val x = (frame.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val y = (frame.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
    val w = ((frame.right - frame.left) * bitmap.width).toInt()
        .coerceAtLeast(1)
        .coerceAtMost(bitmap.width - x)
    val h = ((frame.bottom - frame.top) * bitmap.height).toInt()
        .coerceAtLeast(1)
        .coerceAtMost(bitmap.height - y)
    return runCatching { Bitmap.createBitmap(bitmap, x, y, w, h) }
        .getOrElse { bitmap }
}
