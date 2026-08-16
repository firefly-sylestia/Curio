package com.curio.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * v115 — the avatar crop editor. A fixed SQUARE crop frame (the avatar is
 * always 1:1) with the photo panning and pinching behind it: drag to
 * reposition, pinch to zoom (never smaller than cover-fit, up to 5×), with
 * a reset that returns to the auto center crop. Apply hands back the exact
 * source-pixel [IntRect] under the frame; Cancel discards.
 *
 * No third-party crop library (the app is dependency-light): the frame,
 * grid and gestures are plain Compose, so the dialog wears the app's
 * dialog styling instead of a foreign crop screen.
 *
 * v116 — the crop state is hoisted into the dialog body and the actions
 * use the classic AlertDialog confirmButton/dismissButton overload, so
 * Apply can read the LIVE crop rect while the buttons stay in the dialog
 * bar (the first version put Cancel/Apply inside `text` and called
 * AlertDialog without confirmButton, which matched no overload).
 *
 * v117 — the buttons are a MATCHED pill pair (the dialog language):
 * Cancel is a calm surface pill next to the accent Apply pill, instead of
 * a flat TextButton next to a lone filled pill. The dialog is now opened
 * by every photo pick BEFORE the avatar is saved — Apply saves the crop,
 * Cancel discards the pick.
 */
@Composable
fun AvatarCropDialog(
    bitmap: Bitmap,
    onConfirm: (IntRect) -> Unit,
    onDismiss: () -> Unit
) {
    val imgW = bitmap.width
    val imgH = bitmap.height
    val maxZoom = 5f
    // Hoisted out of the draw block — allocating the ImageBitmap wrapper on
    // every frame during pinch/pan would jank the gesture.
    val image = remember(bitmap) { bitmap.asImageBitmap() }

    // Crop state hoisted here so the Apply button (the dialog's
    // confirmButton) can compute the live crop rect.
    var scale by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale0 by remember { mutableFloatStateOf(0f) }
    var framePx by remember { mutableFloatStateOf(0f) }
    // The dialog accent, resolved in the composable scope — the Canvas draw
    // lambda is NOT a @Composable context, so this can't be called in there.
    val dialogAccent = curioDialogActionColor()

    fun clampAxis(v: Float, displayDim: Float, frameDim: Float): Float =
        if (displayDim <= frameDim) 0f
        else v.coerceIn(-(displayDim - frameDim) / 2f, (displayDim - frameDim) / 2f)

    // The source-pixel square currently under the frame.
    fun currentCropRect(): IntRect {
        val dw = imgW * scale
        val dh = imgH * scale
        val topLeft = offset + Offset(framePx / 2f, framePx / 2f) -
            Offset(dw / 2f, dh / 2f)
        val left = ((-topLeft.x) / scale).roundToInt().coerceIn(0, imgW)
        val top = ((-topLeft.y) / scale).roundToInt().coerceIn(0, imgH)
        val right = ((framePx - topLeft.x) / scale).roundToInt().coerceIn(0, imgW)
        val bottom = ((framePx - topLeft.y) / scale).roundToInt().coerceIn(0, imgH)
        return IntRect(left, top, right, bottom)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        title = { Text("Crop photo", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // ── Square crop canvas — the visible area IS the crop ──
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val fp = with(LocalDensity.current) { maxWidth.toPx() }
                    LaunchedEffect(fp) {
                        // Start = cover-fit (the photo's smaller side fills
                        // the frame → the auto CENTER crop, matching the
                        // default pick behavior). Resets on a size change
                        // (rotation).
                        framePx = fp
                        scale0 = if (fp > 0f && imgW > 0 && imgH > 0) fp / min(imgW, imgH) else 0f
                        scale = scale0
                        offset = Offset.Zero
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .pointerInput(fp) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    if (scale0 <= 0f || scale <= 0f) return@detectTransformGestures
                                    val newScale = (scale * zoom).coerceIn(scale0, scale0 * maxZoom)
                                    // Keep the point under the centroid fixed while zooming.
                                    val dw = imgW * scale
                                    val dh = imgH * scale
                                    val displayTopLeft = offset + Offset(fp / 2f, fp / 2f) -
                                        Offset(dw / 2f, dh / 2f)
                                    val p = centroid - displayTopLeft
                                    val newDw = imgW * newScale
                                    val newDh = imgH * newScale
                                    val newOffset = centroid - p * (newScale / scale) -
                                        Offset(fp / 2f, fp / 2f) +
                                        Offset(newDw / 2f, newDh / 2f) + pan
                                    scale = newScale
                                    offset = Offset(
                                        clampAxis(newOffset.x, newDw, fp),
                                        clampAxis(newOffset.y, newDh, fp)
                                    )
                                }
                            }
                    ) {
                        if (scale > 0f && framePx > 0f) {
                            val dw = imgW * scale
                            val dh = imgH * scale
                            val topLeft = offset + Offset(framePx / 2f, framePx / 2f) -
                                Offset(dw / 2f, dh / 2f)
                            clipRect {
                                drawImage(
                                    image = image,
                                    dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                                    dstSize = IntSize(dw.roundToInt(), dh.roundToInt())
                                )
                            }
                        }
                        // Rule-of-thirds grid + corner brackets in the dialog
                        // accent so the frame reads as a crop window.
                        val accent = dialogAccent
                        for (i in 1..2) {
                            val t = framePx * i / 3f
                            drawLine(accent.copy(alpha = 0.45f), Offset(t, 0f), Offset(t, framePx), strokeWidth = 1f)
                            drawLine(accent.copy(alpha = 0.45f), Offset(0f, t), Offset(framePx, t), strokeWidth = 1f)
                        }
                        val corner = 22f
                        val stroke = 3f
                        // Top-left
                        drawLine(Color.White, Offset(0f, corner), Offset(0f, 0f), stroke)
                        drawLine(Color.White, Offset(0f, 0f), Offset(corner, 0f), stroke)
                        // Top-right
                        drawLine(Color.White, Offset(framePx, corner), Offset(framePx, 0f), stroke)
                        drawLine(Color.White, Offset(framePx, 0f), Offset(framePx - corner, 0f), stroke)
                        // Bottom-left
                        drawLine(Color.White, Offset(0f, framePx - corner), Offset(0f, framePx), stroke)
                        drawLine(Color.White, Offset(0f, framePx), Offset(corner, framePx), stroke)
                        // Bottom-right
                        drawLine(Color.White, Offset(framePx, framePx - corner), Offset(framePx, framePx), stroke)
                        drawLine(Color.White, Offset(framePx, framePx), Offset(framePx - corner, framePx), stroke)
                    }
                    // Hint + reset row.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Pinch to zoom · drag to move",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Reset",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = curioDialogActionColor()
                            ),
                            modifier = Modifier
                                .clickable {
                                    scale = scale0
                                    offset = Offset.Zero
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Surface(
                onClick = { onConfirm(currentCropRect()) },
                shape = RoundedCornerShape(50),
                color = curioDialogActionColor(),
                contentColor = Color.White
            ) {
                Text(
                    "Apply",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        },
        dismissButton = {
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    )
}
