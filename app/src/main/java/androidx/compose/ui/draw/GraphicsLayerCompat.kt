package androidx.compose.ui.draw

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer as graphicsLayerImpl

/** Compatibility shim for the DM screen's historical draw-package import. */
fun Modifier.graphicsLayer(block: GraphicsLayerScope.() -> Unit): Modifier =
    graphicsLayerImpl(block)
