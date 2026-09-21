package com.curio.app.features.community

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v435 — THE SOCIAL PORTRAIT, DERIVED FROM THE MEMBER.
 *
 * This file used to BE the cast: 28 hand-drawn avatars (`PORTRAITS` twenty
 * characters plus `ICONS` eight cozy objects, ~1,300 lines of `Canvas` art) with
 * `profiles.avatar_style` an index into them, plus the picker that chose one. It
 * is now a THIN WRAPPER around [BlobatarArt], and that is the whole of the
 * change the member asked for — "can we use this profile avatar style for social
 * instead of those bad drawing?".
 *
 * What it fixed, in one shape:
 *
 *  · **Every member is now their own face.** The old set meant the wall showed
 *    the same 28 pictures at random, so two members were often the same person.
 *    A face derived from the handle cannot collide by construction.
 *  · **A rename re-faces you**, which is the honest behaviour for a portrait
 *    that stands for a name rather than a choice.
 *  · **The picker is gone** (member: "Remove the picker entirely"). There is
 *    nothing to choose: your username is your face. That also removes the
 *    write-in-flight state, the "couldn't save that icon" failure path and the
 *    row of tiles from the account page, and it means the app never has to ask
 *    a member to pick a portrait again.
 *
 * `profiles.avatar_style` and `SOCIAL_AVATAR_STYLE_COUNT` are deliberately LEFT
 * IN PLACE — no schema change, no migration, and the column still round-trips.
 * Removing a column is irreversible and needs the member's word; leaving a small
 * unused integer is not a mess. See `supabase/AGENTS.md` before touching it.
 *
 * The drawing itself lives in `Blobatar.kt` so that the notification's
 * off-screen bitmap and this canvas share ONE renderer rather than two drifting
 * copies — see [BlobatarArt].
 */

/**
 * The live-presence dot's colour.
 *
 * Green on every ground in both themes, and always drawn as a CUT-OUT (see
 * [SocialAvatar]): the ring of page colour is what keeps it readable against a
 * pale face, a dark one, and the ink tone alike.
 */
private val ActiveDot = Color(0xFF2FBF71)

/**
 * The face for [seed] — a member's handle, or their account id when they have
 * not claimed one (see [blobatarSeed]).
 *
 * @param avatarSize the disc's diameter.
 * @param onClick  when set the disc is tappable (used by author rows that open a
 *                 profile).
 * @param ring     the soft inner rim. Keep it on for avatars that sit on a card
 *                 or a page background; turn it off inside a busy composed row
 *                 — and on the profile banner, where the hero already frames the
 *                 disc and a second edge reads as a doubled line.
 * @param online   draws the live-presence dot in the lower-right corner. Only
 *                 ever set from a last-active stamp that is honestly fresh
 *                 (`CurioPerson.isActiveNow`) — an indicator that guesses is
 *                 worse than no indicator.
 */
@Composable
internal fun SocialAvatar(
    seed: String,
    avatarSize: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
    ring: Boolean = true,
    online: Boolean = false
) {
    // The face is resolved and traced ONCE per seed, not once per frame: the
    // hash, the palette, the trait reads and the Bézier control points all happen
    // inside this value, so a scroll that redraws a row costs two `drawPath`s.
    val art = remember(seed) { BlobatarArt(seed) }

    Box(
        modifier = Modifier
            .size(avatarSize)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(avatarSize)) { drawBlobatar(art, ring) }

        if (online) {
            // CUT OUT of the face, not painted over it: the ring of page colour
            // separates the dot from the art, which is what keeps it readable on
            // every tone in the set and in both themes.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(avatarSize * 0.30f)
                    .clip(CircleShape)
                    .background(ActiveDot)
                    .border(
                        width = (avatarSize * 0.05f).coerceAtLeast(1.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Paints an already-resolved face.
 *
 * Kept as a `DrawScope` extension rather than inlined into the composable for
 * ONE reason: the wallpaper of a notification needs the same face as an
 * `android.graphics.Bitmap`, and a notification is posted from a receiver that
 * has no composition to draw in — so `NotificationAvatars` (`SocialNotifications.kt`)
 * draws into an off-screen `ImageBitmap` through this exact function.
 */
internal fun DrawScope.drawBlobatar(art: BlobatarArt, ring: Boolean = true) {
    art.draw(this, ring)
}
