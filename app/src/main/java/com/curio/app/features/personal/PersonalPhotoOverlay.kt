package com.curio.app.features.personal

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

/**
 * THE PAGE'S OWN IMAGE VIEWER.
 *
 * Tapping a photo in a journal, a chapter review or a saved page used to hand
 * the URI to the app-wide Lightbox ROUTE, which slid a whole new screen in
 * over the page. What a photo in a page wants instead is the app's own move:
 * the picture GROWS out of the spot it was tapped in, over the page it came
 * from, and shrinks back into it when it is done — no screen change, no lost
 * reading position, the page still sitting there behind it.
 *
 * So this is an OVERLAY, not a route:
 *
 *  · [rememberPersonalPhotoOverlayState] is created by the screen (or by the
 *    route that hosts it), a thumbnail calls [PersonalPhotoOverlayState.open]
 *    with the bounds it occupies, and [PersonalPhotoOverlay] draws on top of
 *    the page. The state is a plain holder — the same shape as the mood
 *    board's zoom state — so a screen can hand the opener to its canvases
 *    without any composition-local plumbing.
 *
 *  · THE MORPH is one value: how far open the viewer is. The picture is laid
 *    out at the size it will END at (the image fitted inside the viewport,
 *    which is why the intrinsic size is read off the painter) and the layer is
 *    scaled and translated back to the TAPPED BOUNDS at 0 — so at no point is
 *    the image re-laid-out, it is one continuous move. The shape's corners
 *    round off over the same value, so the thumbnail's own corner reads as the
 *    picture's corner at the start of the move.
 *
 *  · PINCH AND PAN refine the magnified picture on top of the morph (the
 *    gestures the Lightbox screen already taught this app), the close button
 *    or Back settles it back into the page, and tap-anywhere-once-open closes
 *    it. Nothing outside this file needs to know how it works.
 */
@Stable
class PersonalPhotoOverlayState {

    /** The picture on top of the page, or null when the page is clear. */
    var uri by mutableStateOf<String?>(null)
        internal set

    /** Where it was tapped, in window coordinates — the morph's other end. */
    var from by mutableStateOf<Rect?>(null)
        internal set

    /** Set by [close]; the overlay leaves once its close move has settled. */
    var dismissed by mutableStateOf(false)
        internal set

    fun open(uri: String, from: Rect? = null) {
        if (uri.isBlank()) return
        this.uri = uri
        this.from = from
        this.dismissed = false
    }

    fun close() {
        if (uri != null) dismissed = true
    }
}

/** One of these lives with the page that shows the photos. */
@Composable
fun rememberPersonalPhotoOverlayState(): PersonalPhotoOverlayState =
    remember { PersonalPhotoOverlayState() }

/** Room around the picture when it is fully open. */
private val OPEN_PADDING = 10.dp

/** The corner a thumbnail shows, kept at the start of the move. */
private const val FROM_CORNER_DP = 18f

@Composable
fun PersonalPhotoOverlay(state: PersonalPhotoOverlayState) {
    val uri = state.uri ?: return
    val from = state.from
    val density = LocalDensity.current

    // How far open the viewer is: 0 = the tapped thumbnail, 1 = settled.
    val open = remember(uri) { Animatable(0f) }
    var container by remember { mutableStateOf(IntSize.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember(uri) { mutableFloatStateOf(1f) }
    var pan by remember(uri) { mutableStateOf(Offset.Zero) }

    val painter = rememberAsyncImagePainter(uri)
    val intrinsic = painter.intrinsicSize

    // The size the picture ENDS at: the image fitted inside the viewport. The
    // layer morphs from the tapped bounds to this rect, so this has to be the
    // laid-out size and not an animated one.
    val fit = remember(intrinsic, container) {
        val padding = with(density) { OPEN_PADDING.toPx() }
        val vw = container.width - padding * 2f
        val vh = container.height - padding * 2f
        if (vw <= 0f || vh <= 0f || intrinsic.width <= 0f || intrinsic.height <= 0f) {
            null
        } else {
            val scale = minOf(vw / intrinsic.width, vh / intrinsic.height)
            Size(intrinsic.width * scale, intrinsic.height * scale)
        }
    }

    // The move starts only once the image has a size to move INTO (before
    // that there would be nothing to scale and the picture would pop).
    LaunchedEffect(uri, fit) {
        if (fit == null) return@LaunchedEffect
        open.snapTo(0f)
        open.animateTo(1f, spring(dampingRatio = 0.88f, stiffness = 430f))
    }

    LaunchedEffect(state.dismissed) {
        if (!state.dismissed) return@LaunchedEffect
        open.animateTo(0f, tween(190))
        state.uri = null
        state.from = null
        state.dismissed = false
    }

    BackHandler { state.close() }

    val progress = open.value
    // Where the tapped thumbnail sits inside this overlay's own coordinates —
    // the viewer can be hosted anywhere, so the page's window bounds are moved
    // into its space rather than assumed to share an origin.
    val target = if (fit != null && from != null) from.translate(-origin) else null
    val startScale = target?.let {
        maxOf(it.width / fit!!.width, it.height / fit.height)
    } ?: 0.92f
    val startShift = target?.let {
        it.center - Offset(container.width / 2f, container.height / 2f)
    } ?: Offset.Zero
    val scale = (startScale + (1f - startScale) * progress) * zoom
    val shift = Offset(
        x = startShift.x * (1f - progress) + pan.x,
        y = startShift.y * (1f - progress) + pan.y
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                origin = it.positionInWindow()
                container = it.size
            }
            // Tap once open, like every other viewer in the app; a tap during
            // the move is ignored so the morph can't be cut in half.
            .pointerInput(uri) {
                detectTapGestures(
                    onTap = { if (open.value > 0.98f) state.close() }
                )
            }
            .pointerInput(uri) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    if (open.value < 0.98f) return@detectTransformGestures
                    zoom = (zoom * zoomChange).coerceIn(1f, 6f)
                    pan = if (zoom <= 1.02f) Offset.Zero else pan + panChange
                }
            }
    ) {
        // The page stays visible behind the picture while it grows, so the
        // move reads as the photo opening, not as a screen appearing.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f * progress))
        )

        if (fit != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(
                        width = with(density) { fit.width.toDp() },
                        height = with(density) { fit.height.toDp() }
                    )
                    .clip(RoundedCornerShape((FROM_CORNER_DP * (1f - progress)).coerceAtLeast(0f).dp))
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = shift.x
                        translationY = shift.y
                    }
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    // High quality decoding (via the request below) keeps the
                    // enlarged bitmap crisp; the Image composable's painter
                    // overload does not expose a filterQuality parameter.
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Surface(
            onClick = { state.close() },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.44f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .graphicsLayer { alpha = progress }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CurioIcon(CurioIcons.Close, "Close", tint = Color.White, size = 19.dp)
            }
        }
    }
}
