package com.curio.app.features.community

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.curio.app.MainActivity
import com.curio.app.R
import com.curio.app.data.AppPreferences
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.SocialApi
import com.curio.app.data.supabase.SocialPresence
import com.curio.app.navigation.PendingCommunityOpen
import com.curio.app.navigation.PendingDirectMessageOpen
import kotlinx.coroutines.delay

/**
 * CURIO'S SOCIAL NOTIFICATIONS — "a friend wrote" and "someone posted".
 *
 * The social layer has no push service (there is no server of ours to push
 * from, and no Firebase in this build), so announcing an arrival is an
 * in-app job: [SocialNotificationWatcher] polls the inbox and the wall while
 * the app is alive and tells the shade about anything NEW. Both switches are
 * one user-facing toggle (Settings → Notifications → Messages and community),
 * and the whole feature is off the moment Online Mode is off.
 *
 * A MESSAGE NOTIFICATION IS A MESSENGER NOTIFICATION (v389, user decision):
 *
 *  · the sender's own PORTRAIT is the notification's large icon — the real
 *    code-drawn avatar, not a stand-in (see [NotificationAvatars]);
 *  · it is a [NotificationCompat.MessagingStyle] conversation, so the shade
 *    reads as a chat rather than a bulletin;
 *  · REPLY opens the shade's own message box ([RemoteInput]) and sends what
 *    was typed as a reply TO THE LAST MESSAGE of that conversation — the
 *    thread never has to be opened to answer it;
 *  · LIKE reacts to that last message with the app's own reaction
 *    ([SocialReactions.LIKE]) — the same thing the chat's reaction row sends;
 *  · MUTE silences THIS conversation (device-side, [AppPreferences]) and the
 *    action flips to Unmute in place.
 *
 * All three are handled by [SocialNotificationReceiver] so they work with the
 * app in the background, and they are ALWAYS ON: they ride the existing
 * Notifications switch instead of adding a second thing to find (user
 * decision, v389).
 *
 * Nothing here is media: a message notification is the sender's name and the
 * line they typed, exactly like the thread itself.
 */
internal object SocialNotifications {

    private const val MESSAGE_CHANNEL = "curio_messages"
    private const val COMMUNITY_CHANNEL = "curio_community"
    private const val MESSAGE_NOTIFICATION_BASE = 7311
    private const val COMMUNITY_NOTIFICATION_BASE = 7312

    /** Stable, distinct shade entries per peer/card without unbounded ids. */
    private fun notificationId(base: Int, stableKey: String): Int {
        val hash = stableKey.hashCode() and 0x7fffffff
        return base + (hash % 10_000)
    }

    /** The entry one conversation owns (used to update and to cancel it). */
    fun messageIdFor(userId: String): Int = notificationId(MESSAGE_NOTIFICATION_BASE, userId)

    /**
     * "A friend wrote" — tapping it opens THAT conversation, not the front
     * door: the target rides on the launch intent and the NavHost picks it up
     * once it is on a stable root (see [PendingDirectMessageOpen]).
     *
     * [lastMessageId] is what the shade's Reply and Like actions point at (the
     * message they answer / react to). [liked] re-posts the same entry settled:
     * the like already happened, and saying so is the only receipt the shade
     * can give for a reaction.
     */
    fun message(
        context: Context,
        person: CurioPerson,
        preview: String,
        lastMessageId: String,
        lastAtMillis: Long = System.currentTimeMillis(),
        liked: Boolean = false
    ) {
        val notificationId = messageIdFor(person.userId)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PendingDirectMessageOpen.EXTRA_USER_ID, person.userId)
            putExtra(PendingDirectMessageOpen.EXTRA_HANDLE, person.label)
        }
        val avatar = NotificationAvatars.of(blobatarSeed(person.userId, person.username))
        val sender = Person.Builder()
            .setName(person.label)
            .apply { avatar?.let { setIcon(IconCompat.createWithBitmap(it)) } }
            .build()
        val me = Person.Builder()
            .setName(AppPreferences.getDisplayName(context).ifBlank { "You" })
            .build()

        post(
            context = context,
            notificationId = notificationId,
            channelId = MESSAGE_CHANNEL,
            channelName = "Messages",
            channelDescription = "New messages from your friends",
            title = person.label,
            body = preview.ifBlank { "Sent you a message" },
            whenMillis = lastAtMillis,
            style = {
                it.setStyle(
                    NotificationCompat.MessagingStyle(me)
                        .addMessage(preview.ifBlank { "Sent you a message" }, lastAtMillis, sender)
                )
                it.setLargeIcon(avatar)
                it.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                if (liked) it.setSubText("You liked this")
            },
            actions = messageActions(context, person, lastMessageId, preview),
            intent = openIntent,
            requestCode = notificationId
        )
    }

    /**
     * The three doors a message notification offers, in the order a thumb
     * expects them: answer it, react to it, or stop hearing from it.
     */
    private fun messageActions(
        context: Context,
        person: CurioPerson,
        lastMessageId: String,
        preview: String
    ): (NotificationCompat.Builder) -> Unit = { builder ->
        val replyInput = RemoteInput.Builder(SocialNotificationReceiver.EXTRA_TEXT)
            .setLabel("Reply…")
            .build()
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_notification,
                "Reply",
                actionPendingIntent(
                    context,
                    SocialNotificationReceiver.ACTION_REPLY,
                    person,
                    lastMessageId,
                    preview = preview,
                    requestCode = messageIdFor(person.userId) + 1,
                    // RemoteInput requires a MUTABLE target: the system has to
                    // hand the typed text back through it.
                    mutable = true
                )
            )
                .addRemoteInput(replyInput)
                .setAllowGeneratedReplies(true)
                .build()
        )
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_notification,
                "Like",
                actionPendingIntent(
                    context,
                    SocialNotificationReceiver.ACTION_LIKE,
                    person,
                    lastMessageId,
                    preview = preview,
                    requestCode = messageIdFor(person.userId) + 2
                )
            ).build()
        )
        val muted = person.userId in AppPreferences.mutedConversationsState
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_notification,
                if (muted) "Unmute" else "Mute",
                actionPendingIntent(
                    context,
                    if (muted) SocialNotificationReceiver.ACTION_UNMUTE
                    else SocialNotificationReceiver.ACTION_MUTE,
                    person,
                    lastMessageId,
                    preview = preview,
                    requestCode = messageIdFor(person.userId) + 3
                )
            ).build()
        )
    }

    /** One action's explicit target — the receiver, with who/which to act on. */
    private fun actionPendingIntent(
        context: Context,
        action: String,
        person: CurioPerson,
        lastMessageId: String,
        preview: String,
        requestCode: Int,
        mutable: Boolean = false
    ): PendingIntent {
        val intent = Intent(context, SocialNotificationReceiver::class.java).apply {
            this.action = action
            putExtra(SocialNotificationReceiver.EXTRA_PEER_ID, person.userId)
            putExtra(SocialNotificationReceiver.EXTRA_PEER_LABEL, person.label)
            putExtra(SocialNotificationReceiver.EXTRA_PEER_HANDLE, person.handleLabel)
            putExtra(SocialNotificationReceiver.EXTRA_PEER_AVATAR, person.avatarStyle)
            putExtra(SocialNotificationReceiver.EXTRA_MESSAGE_ID, lastMessageId)
            putExtra(SocialNotificationReceiver.EXTRA_BODY, preview)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    /**
     * Takes one conversation's entry off the shade. Called when the reply is
     * sent, when the conversation is muted, and when the thread is opened.
     */
    fun cancelMessage(context: Context, userId: String) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(messageIdFor(userId))
        }
    }

    /**
     * "Someone posted" — tapping it lands on the community wall
     * ([PendingCommunityOpen]), which is where the new card actually is.
     */
    fun community(context: Context, title: String, body: String) {
        val notificationKey = "$title|$body"
        post(
            context = context,
            notificationId = notificationId(COMMUNITY_NOTIFICATION_BASE, notificationKey),
            channelId = COMMUNITY_CHANNEL,
            channelName = "Social",
            channelDescription = "New posts on the 24-hour wall",
            title = title,
            body = body,
            whenMillis = System.currentTimeMillis(),
            style = {
                it.setStyle(NotificationCompat.BigTextStyle().bigText(body))
            },
            actions = {},
            intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(PendingCommunityOpen.EXTRA_OPEN_COMMUNITY, true)
            },
            requestCode = notificationId(COMMUNITY_NOTIFICATION_BASE, notificationKey)
        )
    }

    /** True when the OS will actually show a notification for this app. */
    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // The POST_NOTIFICATIONS check below is real, but it sits behind a helper
    // (`canNotify`) AND a `runCatching`: lint's flow analysis cannot see a
    // permission test through either, so it flags the `notify` call itself and
    // fails the build. The permission is therefore ALSO checked inline, right
    // above the call — and the suppression is here because the two guards
    // together are what actually make the call safe, which is not something
    // the linter can follow. Do not remove the inline check when removing this.
    @SuppressLint("MissingPermission")
    private fun post(
        context: Context,
        notificationId: Int,
        channelId: String,
        channelName: String,
        channelDescription: String,
        title: String,
        body: String,
        whenMillis: Long,
        style: (NotificationCompat.Builder) -> Unit,
        actions: (NotificationCompat.Builder) -> Unit,
        intent: Intent,
        requestCode: Int
    ) {
        // A notification is a courtesy, never a crash: a missing permission,
        // a locked-down shade or a dead channel is silently ignored.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            if (!canNotify(context)) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = channelDescription }
            )
            val contentIntent = PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setWhen(whenMillis)
                .setShowWhen(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            style(builder)
            actions(builder)
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }
}

/**
 * THE SHADE'S FACES — a member's portrait, rendered once per seed.
 *
 * A notification's large icon is an `android.graphics.Bitmap`, and a
 * notification is posted from a receiver or a coroutine with no composition to
 * draw in — so the face is drawn into an off-screen [ImageBitmap] by the SAME
 * [`BlobatarArt`] the app's canvas uses (never a lookalike: a second drawing
 * would drift from the faces the app shows).
 *
 * A derived face is what makes this cheap: the old cast needed 28 pre-baked
 * bitmaps because each style was 1,300 lines of art, whereas one blobatar is a
 * hash and a couple of Béziers — so the map is keyed by seed, capped, and a miss
 * costs a few microseconds rather than a drawing routine.
 */
private object NotificationAvatars {

    /** 192px is the largest a notification icon is ever drawn at. */
    private const val SIZE_PX = 192

    /**
     * Bounded because the key is a member's handle and the sender is not ours to
     * trust: a chatty shade must not grow this map for the life of the process.
     * Cleared wholesale rather than trimmed — these are cheap to rebuild, and an
     * LRU would be more bookkeeping than the thing it manages.
     */
    private const val CACHE_CAP = 48

    private val cache = HashMap<String, Bitmap>(32)

    /** The face for [seed], or null when the canvas could not be drawn. */
    @Synchronized
    fun of(seed: String): Bitmap? {
        cache[seed]?.let { return it }
        val rendered = render(seed) ?: return null
        if (cache.size >= CACHE_CAP) cache.clear()
        cache[seed] = rendered
        return rendered
    }

    /** Must run off the main thread's UI work — it paints, it does not compose. */
    private fun render(seed: String): Bitmap? = runCatching {
        val pixels = SIZE_PX.toFloat()
        val image = ImageBitmap(SIZE_PX, SIZE_PX)
        val canvas = Canvas(image)
        val art = BlobatarArt(seed)
        CanvasDrawScope().draw(
            Density(1f, 1f),
            LayoutDirection.Ltr,
            canvas,
            Size(pixels, pixels)
        ) {
            // No inner rim: against the shade's own background it reads as a
            // hairline that is not in the app's avatar either.
            drawBlobatar(art, ring = false)
        }
        image.asAndroidBitmap()
    }.getOrNull()
}

/**
 * The arrival watcher, mounted once by the Activity.
 *
 * Two slow loops while the app is alive:
 *
 *  1. **the inbox**, every [INBOX_MS] — a conversation whose unread count went
 *     UP since the previous tick is announced (the FIRST tick is a baseline
 *     only, so launching Curio never fires a notification for mail that was
 *     already waiting). A MUTED conversation is still counted (so unmuting
 *     never dumps a backlog on the shade) but never announced.
 *  2. **the wall**, every [WALL_MS] — cards that are newer than the newest one
 *     seen before, and not written by this account, are announced once.
 *  3. **presence**, every [PRESENCE_MS] — the member's own last-active stamp.
 *     Not a notification and not gated on the switch.
 *
 * The first two are gated on `Online mode + signed in + the Notifications
 * switch`, so with any of the three off the app makes no extra request at all.
 */
@Composable
internal fun SocialNotificationWatcher() {
    val context = LocalContext.current
    val account = OnlineAccount.state
    val onlineMode = AppPreferences.onlineModeEnabledState
    val enabled = AppPreferences.socialNotificationsState
    val token = account.session?.accessToken
    val myUserId = account.session?.userId

    // ── messages ─────────────────────────────────────────────────────────
    LaunchedEffect(token, myUserId, enabled, onlineMode) {
        if (!enabled || !onlineMode || token == null || myUserId == null) return@LaunchedEffect
        var baseline: Map<String, Int>? = null
        while (true) {
            SocialApi.threads(token, myUserId).onSuccess { threads ->
                val counts = threads.associate { it.person.userId to it.unread }
                val before = baseline
                baseline = counts
                SocialPeopleCache.remember(context, threads.map { it.person })
                if (before == null) return@onSuccess
                threads
                    .filter { thread ->
                        (counts[thread.person.userId] ?: 0) > (before[thread.person.userId] ?: 0)
                    }
                    // A muted conversation is read like the others (that is
                    // what keeps the baseline honest) and then dropped.
                    .filterNot { it.person.userId in AppPreferences.mutedConversationsState }
                    .maxByOrNull { it.lastAtMillis }
                    ?.let { thread ->
                        SocialNotifications.message(
                            context = context,
                            person = thread.person,
                            preview = thread.preview,
                            lastMessageId = thread.lastMessageId,
                            lastAtMillis = thread.lastAtMillis
                        )
                    }
            }
            delay(INBOX_MS)
        }
    }

    // ── wall ─────────────────────────────────────────────────────────────
    LaunchedEffect(token, myUserId, enabled, onlineMode) {
        if (!enabled || !onlineMode || token == null || myUserId == null) return@LaunchedEffect
        var newestSeen: String? = null
        while (true) {
            CommunityApi.feed(token, myUserId).onSuccess { cards ->
                val previous = newestSeen
                newestSeen = cards.firstOrNull()?.id
                val fresh = if (previous == null) emptyList()
                else cards.takeWhile { it.id != previous }
                val fromOthers = fresh.filterNot { it.mine }
                if (fromOthers.isNotEmpty()) {
                    val first = fromOthers.first()
                    val who = first.authorLabel
                    val subject = first.topicName
                    val title = if (fromOthers.size > 1) {
                        "$who and ${fromOthers.size - 1} more posted"
                    } else {
                        "$who posted"
                    }
                    val body = when {
                        fromOthers.size > 1 ->
                            "New cards on the social wall — they disappear after 24 hours."
                        subject.isNotBlank() -> "$subject — on the wall for 24 hours."
                        else -> "A new line on the social wall."
                    }
                    SocialNotifications.community(context, title, body)
                }
            }
            delay(WALL_MS)
        }
    }

    // ── presence ────────────────────────────────────────────────────────
    LaunchedEffect(token, onlineMode) {
        if (!onlineMode || token == null) return@LaunchedEffect
        while (true) {
            SocialPresence.publish(context)
            delay(PRESENCE_MS)
        }
    }
}

/** How often the inbox and the wall are checked while the app is alive. */
private const val INBOX_MS = 8_000L
private const val WALL_MS = 30_000L

/** How often the presence tick runs — the write itself backs off further. */
private const val PRESENCE_MS = 5L * 60 * 1000
