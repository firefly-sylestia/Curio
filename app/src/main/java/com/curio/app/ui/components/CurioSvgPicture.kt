package com.curio.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.curio.app.data.SvgPicture

/**
 * A POST'S PICTURE, drawn from the SVG TEXT the server stored (v513).
 *
 * One composable for every surface that shows a picture a member attached, so a
 * card, a message and a profile picture can never be drawn by two different
 * rules. Two forms exist and both are text:
 *
 * - a **traced** document (vector `<rect>`s) is rendered by Coil's `SvgDecoder`,
 *   which `MainActivity` already registers for the app's own SVG art;
 * - an **embedded** document (a downscaled JPEG in a data URI) is decoded
 *   directly to a bitmap, because pulling the payload out of a string we wrote
 *   is both simpler and exact.
 *
 * Rendering nothing for a blank string is deliberate: a post with no picture
 * must not leave an empty box taking up space on the wall.
 */
@Composable
fun CurioSvgPicture(
    svg: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    if (!SvgPicture.isPicture(svg)) return

    val bitmap = remember(svg) { SvgPicture.embeddedBitmap(svg) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
        return
    }

    val context = LocalContext.current
    val request = remember(svg, context) {
        ImageRequest.Builder(context)
            .data(svg.toByteArray())
            .decoderFactory(SvgDecoder.Factory())
            .crossfade(120)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

/**
 * THE COMPOSER'S PICTURE DOOR (v513).
 *
 * One control that states what is attached and offers the two things a poster
 * needs: put a picture on the post, or take it off again. It lives here, beside
 * [CurioSvgPicture], so the surface that SHOWS a picture and the surface that
 * ATTACHES one can never drift apart.
 */
@Composable
fun CurioPictureDoor(
    svg: String,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val attached = SvgPicture.isPicture(svg)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth().clickable(onClick = onPick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (attached) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    CurioSvgPicture(
                        svg = svg,
                        contentDescription = "The picture on this post",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Text(
                text = if (attached) "Picture attached" else "Add a picture",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            if (attached) {
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onClear)
                )
            }
        }
    }
}
