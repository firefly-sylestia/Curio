package com.curio.app.features.community

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
 * Nothing here is media: a message notification is the sender's name and the
 * line they typed, exactly like the thread itself.
 */
internal object SocialNotifications {

    private const val MESSAGE_CHANNEL = "curio_messages"
    private const val COMMUNITY_CHANNEL = "curio_community"
    private const val MESSAGE_NOTIFICATION_ID = 7311
    private const val COMMUNITY_NOTIFICATION_ID = 7312

    /**
     * "A friend wrote" — tapping it opens THAT conversation, not the front
     * door: the target rides on the launch intent and the NavHost picks it up
     * once it is on a stable root (see [PendingDirectMessageOpen]).
     */
    fun message(context: Context, person: CurioPerson, preview: String) {
        post(
            context = context,
            notificationId = MESSAGE_NOTIFICATION_ID,
            channelId = MESSAGE_CHANNEL,
            channelName = "Messages",
            channelDescription = "New messages from your friends",
            title = person.label,
            body = preview.ifBlank { "Sent you a message" },
            intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(PendingDirectMessageOpen.EXTRA_USER_ID, person.userId)
                putExtra(PendingDirectMessageOpen.EXTRA_HANDLE, person.label)
            },
            requestCode = MESSAGE_NOTIFICATION_ID
        )
    }

    /**
     * "Someone posted" — tapping it lands on the community wall
     * ([PendingCommunityOpen]), which is where the new card actually is.
     */
    fun community(context: Context, title: String, body: String) {
        post(
            context = context,
            notificationId = COMMUNITY_NOTIFICATION_ID,
            channelId = COMMUNITY_CHANNEL,
            channelName = "Community",
            channelDescription = "New posts on the 24-hour wall",
            title = title,
            body = body,
            intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(PendingCommunityOpen.EXTRA_OPEN_COMMUNITY, true)
            },
            requestCode = COMMUNITY_NOTIFICATION_ID
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

    private fun post(
        context: Context,
        notificationId: Int,
        channelId: String,
        channelName: String,
        channelDescription: String,
        title: String,
        body: String,
        intent: Intent,
        requestCode: Int
    ) {
        // A notification is a courtesy, never a crash: a missing permission,
        // a locked-down shade or a dead channel is silently ignored.
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
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}

/**
 * The arrival watcher, mounted once by the Activity.
 *
 * Two slow loops while the app is alive:
 *
 *  1. **the inbox**, every [INBOX_MS] — a conversation whose unread count went
 *     UP since the previous tick is announced (the FIRST tick is a baseline
 *     only, so launching Curio never fires a notification for mail that was
 *     already waiting).
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
                // Identities are remembered here too, so the inbox and any
                // thread opened from a notification draw a real name at once.
                SocialPeopleCache.remember(context, threads.map { it.person })
                if (before == null) return@onSuccess
                threads
                    .filter { thread ->
                        (counts[thread.person.userId] ?: 0) > (before[thread.person.userId] ?: 0)
                    }
                    .maxByOrNull { it.lastAtMillis }
                    ?.let { SocialNotifications.message(context, it.person, it.preview) }
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
                // The feed is newest-first, so everything ahead of the card we
                // already saw is new. A `null` previous is the baseline pass.
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
                            "New cards on the community wall — they disappear after 24 hours."
                        subject.isNotBlank() -> "$subject — on the wall for 24 hours."
                        else -> "A new line on the community wall."
                    }
                    SocialNotifications.community(context, title, body)
                }
            }
            delay(WALL_MS)
        }
    }

    // ── presence ────────────────────────────────────────────────────────
    // NOT a notification: this publishes the member's OWN last-active stamp,
    // the courtesy line a profile can draw. Gated on Online Mode + a session
    // alone (the notifications switch has nothing to do with it), and skipped
    // entirely when the member hid activity — SocialPresence checks that, and
    // turning hiding ON clears the stamp through SocialApi.updatePrivacy.
    LaunchedEffect(token, onlineMode) {
        if (!onlineMode || token == null) return@LaunchedEffect
        while (true) {
            SocialPresence.publish(context)
            delay(PRESENCE_MS)
        }
    }
}

/**
 * How often the inbox and the wall are checked while the app is alive. Both
 * are deliberately unhurried: a message the user is actually waiting on lands
 * in the open thread instantly (the thread polls itself), so these only cover
 * "the phone is in your pocket and the app is still warm".
 */
private const val INBOX_MS = 8_000L
private const val WALL_MS = 30_000L

/** How often the presence tick runs — the write itself backs off further. */
private const val PRESENCE_MS = 5L * 60 * 1000
