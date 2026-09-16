package com.curio.app.features.community

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput
import com.curio.app.data.AppPreferences
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.SocialApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * THE SHADE'S OWN DOORS (v389) — reply, like and mute, without opening Curio.
 *
 * A message notification carries three actions, and each one lands here:
 *
 *  · **Reply** — the shade's inline message box ([RemoteInput]); the text is
 *    sent as a reply TO THE LAST MESSAGE of that conversation, so answering a
 *    friend never costs a trip through the app. The entry is then taken off
 *    the shade (its job is done).
 *  · **Like** — the app's own reaction ([SocialReactions.LIKE]) on that same
 *    last message, and the entry re-posts itself settled ("You liked this").
 *  · **Mute / Unmute** — this conversation's notifications, kept on the device
 *    ([AppPreferences.mutedConversations]). Muting dismisses the entry;
 *    unmuting brings it back.
 *
 * The receiver runs with the app in the BACKGROUND, so it restores the stored
 * session itself ([OnlineAccount.restore]) instead of relying on a screen
 * having done it — a notification must work on a cold process too. Nothing is
 * carried in the intent but WHO and WHICH: the token is never put in a
 * PendingIntent's extras.
 */
class SocialNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val peerId = intent.getStringExtra(EXTRA_PEER_ID) ?: return
        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID).orEmpty()
        val label = intent.getStringExtra(EXTRA_PEER_LABEL).orEmpty()
        val handle = intent.getStringExtra(EXTRA_PEER_HANDLE).orEmpty()
        val avatar = intent.getIntExtra(EXTRA_PEER_AVATAR, 0)
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val replyText = replyTextOf(intent)

        // The actions are network calls, and a BroadcastReceiver's window is
        // measured in seconds: goAsync keeps the process alive until the work
        // is done, and finish() releases it whatever the outcome.
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handle(appContext, action, peerId, label, handle, avatar, messageId, body, replyText)
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }

    private suspend fun handle(
        context: Context,
        action: String,
        peerId: String,
        label: String,
        handle: String,
        avatar: Int,
        messageId: String,
        body: String,
        replyText: String?
    ) {
        val person = CurioPerson(userId = peerId, displayName = label, username = handle, avatarStyle = avatar)

        when (action) {
            ACTION_MUTE -> {
                AppPreferences.setConversationMuted(context, peerId, true)
                SocialNotifications.cancelMessage(context, peerId)
                return
            }
            ACTION_UNMUTE -> {
                AppPreferences.setConversationMuted(context, peerId, false)
                SocialNotifications.message(
                    context = context,
                    person = person,
                    preview = body,
                    lastMessageId = messageId
                )
                return
            }
        }

        // The two remaining actions talk to the server, so they need the
        // account: restore() loads the stored session synchronously, which is
        // what makes this work on a process the user has not opened.
        OnlineAccount.restore(context)
        val session = OnlineAccount.state.session ?: return
        val token = session.accessToken
        val me = session.userId

        when (action) {
            ACTION_REPLY -> {
                val text = replyText?.trim().orEmpty()
                if (text.isEmpty()) return
                SocialApi.sendPlaintext(
                    accessToken = token,
                    toUserId = peerId,
                    body = text,
                    myUserId = me,
                    // "reply attached with the last message": the shade's
                    // answer quotes the line it is answering.
                    replyTo = messageId.takeIf { it.isNotBlank() }
                ).onSuccess {
                    SocialNotifications.cancelMessage(context, peerId)
                }
            }

            ACTION_LIKE -> {
                if (messageId.isBlank()) return
                SocialApi.react(token, messageId, SocialReactions.LIKE).onSuccess {
                    SocialNotifications.message(
                        context = context,
                        person = person,
                        preview = body,
                        lastMessageId = messageId,
                        liked = true
                    )
                }
            }
        }
    }

    /** The typed reply, or null when the shade handed nothing back. */
    private fun replyTextOf(intent: Intent): String? {
        val results: Bundle = RemoteInput.getResultsFromIntent(intent) ?: return null
        return results.getCharSequence(EXTRA_TEXT)?.toString()
    }

    companion object {
        const val ACTION_REPLY = "com.curio.app.social.action.REPLY"
        const val ACTION_LIKE = "com.curio.app.social.action.LIKE"
        const val ACTION_MUTE = "com.curio.app.social.action.MUTE"
        const val ACTION_UNMUTE = "com.curio.app.social.action.UNMUTE"

        const val EXTRA_PEER_ID = "peer_id"
        const val EXTRA_PEER_LABEL = "peer_label"
        const val EXTRA_PEER_HANDLE = "peer_handle"
        const val EXTRA_PEER_AVATAR = "peer_avatar"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_BODY = "message_body"

        /** The RemoteInput's key — where the shade puts the typed text. */
        const val EXTRA_TEXT = "reply_text"
    }
}
