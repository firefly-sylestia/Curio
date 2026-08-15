package com.curio.app.ui.components

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

/**
 * v103 — loads the saved profile avatar photo (a user-picked image copied
 * into the app's private files dir) as a downscaled bitmap, cached per
 * [path]. Returns null when no avatar is set or the file is unreadable.
 * Each new pick gets a fresh filename, so a changed path reloads.
 */
@Composable
fun rememberProfileAvatar(path: String?): ImageBitmap? = remember(path) {
    if (path.isNullOrBlank()) null
    else runCatching {
        val file = File(path)
        if (!file.isFile) null
        else if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(file)
            ) { decoder, _, _ -> decoder.setTargetSize(512, 512) }.asImageBitmap()
        } else {
            // decodeFile returns null on an unreadable file — safe-call so
            // a broken avatar reads as "none" instead of crashing.
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }.getOrNull()
}

/**
 * v103 — draws the profile avatar photo inside the caller's already sized
 * and circle-clipped [modifier]. Renders nothing when no avatar is set, so
 * the caller keeps its fallback (the name initial) visible.
 */
@Composable
fun ProfileAvatarImage(
    path: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap = rememberProfileAvatar(path)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Profile avatar",
            contentScale = contentScale,
            modifier = modifier.fillMaxSize()
        )
    }
}
