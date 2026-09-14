package com.curio.app.features.community

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.curio.app.ui.theme.CurioIcons

/** Local aliases used by the DM interaction surface without expanding the global icon contract. */
val CurioIcons.Reply: String
    get() = "reply"

val CurioIcons.Copy: String
    get() = "content_copy"

val CurioIcons.Delete: String
    get() = "delete"

/** Keeps the interaction surface source-compatible with Compose scale syntax. */
fun Modifier.scale(factor: Float): Modifier = graphicsLayer {
    scaleX = factor
    scaleY = factor
}
