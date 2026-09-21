package com.curio.app.ui.components

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.curio.app.data.AppPreferences
import com.curio.app.features.community.SocialAvatar
import com.curio.app.features.community.blobatarSeed
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

/**
 * v439 — THE MEMBER'S OWN PICTURE, WHICHEVER KIND THEY CHOSE.
 *
 * The member: *"let user set that blob as their pfp in app profile too"*. The
 * app's profile had exactly one kind of picture — a photo cropped on this device
 * — while Social drew every member a face derived from their handle, so the two
 * halves of the same app showed the same member as two different people.
 *
 * **The blob wins while it is chosen, and the photo is not deleted to make that
 * true**: turning the blob off restores the exact photo that was there before
 * (see [AppPreferences.isProfileAvatarBlob]). That is why this is a composable
 * that RESOLVES between the two rather than a preference that overwrites the
 * other one.
 *
 * **Whoever draws this must also ask [hasOwnPicture]** before choosing between
 * this and their own initial: a member with a blob and no photo has a picture,
 * and a fallback that only tested the photo's path would leave them looking at a
 * letter while their blob existed.
 *
 * @param path the photo's path, or blank.
 * @param modifier the box the caller has ALREADY sized and circle-clipped. The
 *                 blob is sized from it rather than given a number, so a 44dp
 *                 drawer disc and an 84dp dialog preview both come out right
 *                 without this knowing either one.
 */
@Composable
fun CurioMemberAvatar(
    path: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (AppPreferences.profileAvatarBlobState) {
        // The seed is the member's own handle, EXACTLY as Social derives it
        // ([blobatarSeed]): the whole point of wearing the blob here is that it
        // is the same face the wall shows, so it must never be seeded from
        // anything local like the display name.
        val seed = blobatarSeed(null, AppPreferences.getUsername(context))
        BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            SocialAvatar(
                seed = seed,
                avatarSize = minOf(maxWidth, maxHeight)
            )
        }
    } else {
        ProfileAvatarImage(path, modifier)
    }
}

/**
 * v439 — DOES THIS MEMBER HAVE A PICTURE AT ALL?
 *
 * True for a chosen blob, or for a photo on this device. Every surface that
 * draws the profile picture pairs this with [CurioMemberAvatar] and keeps its
 * own initial for the `false` case — the initial is the only part of the
 * fallback that differs between the hero, the dialog preview and the drawer, so
 * it stays where it is written rather than being guessed at here.
 */
@Composable
fun hasOwnPicture(path: String?): Boolean =
    AppPreferences.profileAvatarBlobState || !path.isNullOrBlank()
