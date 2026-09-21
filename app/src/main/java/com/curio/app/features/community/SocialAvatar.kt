package com.curio.app.features.community

import android.provider.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
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
 * **v437 — and the faces are ALIVE.** Upstream ships the idle motion as a CSS
 * layer (`animate: "always"`), which a Compose surface has no stylesheet for, so
 * it is evaluated from the same seeds and numbers instead — see [BlobatarIdle]
 * and [BlobatarIdleClock]. Every face breathes, bobs, blinks and glances on its
 * own seeded periods, so a wall of them reads as a crowd rather than as one
 * heartbeat, and the platform's own "remove animations" switch turns the whole
 * layer off (see [rememberCurioMotionEnabled]).
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
 * v437 — ONE FRAME CLOCK FOR EVERY ANIMATED FACE IN THE APP.
 *
 * The idle motion (see [BlobatarIdle]) is a function of ELAPSED time, so the
 * faces do not each need an animation of their own — they need one number that
 * moves. A per-avatar `rememberInfiniteTransition` would put an animation, a
 * composition slot and a frame callback on every face in a scrolling wall of
 * them; this is one clock, read by every draw.
 *
 * The value is read INSIDE the draw lambda (see [SocialAvatar]), so a tick
 * invalidates the draw phase of each avatar and never its composition: a wall of
 * faces re-draws sixty times a second and re-composes zero times.
 */
internal object BlobatarClock {
    /** Milliseconds since the clock started; 0 while it has never run. */
    internal val elapsed = mutableLongStateOf(0L)

    /**
     * How many faces are on screen and want to move.
     *
     * The loop runs only while this is above zero (see [BlobatarIdleClock]): a
     * clock that ran forever would keep the app's frame loop alive for a face
     * nobody is looking at, which is a battery cost for nothing — the same
     * mistake as an infinite animation parked off screen.
     */
    internal val watchers = mutableIntStateOf(0)

    /** The time in milliseconds, or null when nothing is animating. */
    internal val elapsedMs: Double?
        get() = if (watchers.intValue <= 0) null else elapsed.longValue.toDouble()
}

/**
 * v437 — THE IDLE CLOCK'S HOST. Composed ONCE, at the app's root.
 *
 * It is a host rather than something each avatar starts for itself for the
 * reason a frame callback is not free: N avatars must not mean N loops. It draws
 * nothing and does nothing at all until a face asks to move, and it stops the
 * moment the last one leaves — so an app sitting on a page with no avatars on it
 * is exactly as idle as it was before this layer existed.
 */
@Composable
internal fun BlobatarIdleClock() {
    val context = LocalContext.current
    val wanted = BlobatarClock.watchers.intValue
    val enabled = rememberCurioMotionEnabled(context)
    LaunchedEffect(enabled, wanted > 0) {
        if (!enabled || wanted <= 0) return@LaunchedEffect
        var start = -1L
        while (true) {
            withFrameNanos { now ->
                if (start < 0L) start = now
                BlobatarClock.elapsed.longValue = (now - start) / 1_000_000L
            }
        }
    }
}

/**
 * Whether the platform wants motion at all.
 *
 * Android's "Remove animations" accessibility switch zeroes the animator scale,
 * which is the same flag the floating pet reads for its own wandering (see
 * `CurioFloatingPet`) — one answer to "does this device want things moving",
 * asked once here and obeyed by every face. Upstream honours
 * `prefers-reduced-motion` for the same reason and with the same conclusion: an
 * idle loop is decoration, so removing it costs the member nothing.
 */
@Composable
private fun rememberCurioMotionEnabled(context: android.content.Context): Boolean =
    remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
    }

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
 * @param animated v437 — the idle motion (see [BlobatarIdle]). On by default;
 *                 off for a face that must hold still, and always off when the
 *                 platform's "remove animations" switch is set.
 */
@Composable
internal fun SocialAvatar(
    seed: String,
    avatarSize: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
    ring: Boolean = true,
    online: Boolean = false,
    animated: Boolean = true
) {
    // The face is resolved and traced ONCE per seed, not once per frame: the
    // hash, the palette, the trait reads and the Bézier control points all happen
    // inside this value, so a scroll that redraws a row costs two `drawPath`s.
    val art = remember(seed) { BlobatarArt(seed) }
    val context = LocalContext.current
    val moves = animated && rememberCurioMotionEnabled(context)
    // The face only counts as a watcher while it is actually moving, which is
    // what lets the clock stop when the last one leaves (see [BlobatarIdleClock]).
    if (moves) {
        DisposableEffect(Unit) {
            BlobatarClock.watchers.intValue += 1
            onDispose { BlobatarClock.watchers.intValue -= 1 }
        }
    }

    Box(
        modifier = Modifier
            .size(avatarSize)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(avatarSize)) {
            // The clock is read HERE, in the draw phase, and that is the whole
            // performance story of the animation: the read is tracked by the
            // draw scope, so a tick re-draws this canvas and re-composes nothing
            // — no state hoisted into composition, no recomposition per frame.
            drawBlobatar(art, ring, if (moves) BlobatarClock.elapsedMs else null)
        }

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
internal fun DrawScope.drawBlobatar(
    art: BlobatarArt,
    ring: Boolean = true,
    /** v437 — elapsed milliseconds of idle time; null paints the still pose. */
    elapsedMs: Double? = null
) {
    art.draw(this, ring, elapsedMs)
}
