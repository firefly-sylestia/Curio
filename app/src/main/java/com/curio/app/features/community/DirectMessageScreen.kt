package com.curio.app.features.community

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.snap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CommunityError
import com.curio.app.data.supabase.CurioDirectMessage
import com.curio.app.data.supabase.CurioDmReaction
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.RealtimeWatch
import com.curio.app.data.supabase.SocialApi
import com.curio.app.data.supabase.SupabaseRealtime
import com.curio.app.ui.components.rememberCurioPressSource
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionInfoRow
import com.curio.app.features.settings.SettingsOptionRow
import com.curio.app.features.settings.SettingsSectionHeading
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.curioFillInk
import com.curio.app.ui.theme.isCurioDarkTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.async
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A DIRECT CONVERSATION — one thread with one friend.
 *
 * Private by construction: `dm_messages` gives its two participants the only
 * read policy, and the INSERT policy additionally requires an ACCEPTED friend
 * request, so a stranger cannot be messaged even with the right id. Text only —
 * there is no media column to fill.
 *
 * What the surface does now, top to bottom:
 *
 *  - **It opens instantly and survives losing signal.** The thread renders the
 *    last messages kept on the device ([SocialMessageCache]) before the network
 *    is asked anything, then the server's copy replaces it. A message you
 *    already saw is never a blank screen again.
 *  - **A header you can act on** — portrait, live username, and one honest
 *    line about what "private" means here — plus a tap through to the profile.
 *  - **A read conversation**: day rules, grouped runs from one person, a single
 *    timestamp per run, and a read receipt on your last line.
 *  - **Reactions**: tap a bubble and a palette slides in under it. The emoji
 *    itself is what the server stores, so nothing is uploaded.
 *  - **"is typing…"**: a real, server-backed row that expires on its own, shown
 *    as a live line in the header and as a breathing bubble in the thread.
 *  - **A composer that sends the moment you tap**: the message appears
 *    immediately on a spring, the field clears, and the network catches up.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DirectMessageScreen(
    navController: NavController,
    otherUserId: String,
    handle: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state
    val wide = windowWidthSizeClass().isWide
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()

    val onlineMode = AppPreferences.onlineModeEnabledState
    val token = account.session?.accessToken
    val myUserId = account.session?.userId
    val eligible = account.signedIn && onlineMode && token != null && myUserId != null

    // Opening the conversation ANSWERS its shade entry: a message notification
    // still sitting there for a chat you are reading is noise (v389).
    LaunchedEffect(otherUserId) { SocialNotifications.cancelMessage(context, otherUserId) }

    // Filled from the device's own copy the instant the account resolves, so
    // a conversation you have already had is never a blank screen.
    var messages by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
    // Sent-but-not-yet-confirmed messages, drawn exactly like real ones.
    var pending by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
    // Sent bubbles waiting for their server row: rendered exactly like a
    // delivered message (same text, same clock), dropped the moment the real
    // row arrives. This is what keeps a send from vanishing and returning.
    var sentShadow by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
    var person by remember {
        // On screen from the FIRST frame: the device remembers every identity
        // it has resolved, and the route carries the handle the caller already
        // had (Friends, a thread row, a profile). The network only refines
        // both — a conversation never opens on a placeholder.
        mutableStateOf(
            SocialPeopleCache.read(context, otherUserId)
                ?: handle.trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { CurioPerson(userId = otherUserId, displayName = it) }
        )
    }
    var reactions by remember { mutableStateOf<Map<String, List<CurioDmReaction>>>(emptyMap()) }
    var draft by remember { mutableStateOf("") }
    var peerTyping by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadedOnce by remember { mutableStateOf(false) }
    // A server push bumps this, which re-runs the delta fetch below. It is a
    // COUNTER rather than a flag so two pushes in a row are two fetches, and it
    // is keyed on the thread so opening another conversation starts fresh.
    var pushed by remember(otherUserId) { mutableStateOf(0) }
    // Set by a realtime UPDATE/DELETE push: the next delta pull re-reads the
    // whole page once, so a peer's edit or recall is reflected even though it
    // happened to a row OLDER than the newest one on screen.
    var pendingRealtimeRevisions by remember(otherUserId) { mutableStateOf(false) }

    /**
     * Reacting to one message, from the floating action sheet (the palette is
     * the sheet's first row): optimistic glyph first, server confirm after,
     * revert on failure. Guarded so a call without a live session is simply a
     * no-op.
     */
    fun pickReactionScreen(messageId: String, kind: String, activeToken: String?, activeUserId: String?) {
        if (activeToken == null || activeUserId == null) return
        scope.launch {
            val mine = reactions[messageId]?.firstOrNull { it.userId == activeUserId }
            val optimistic = if (mine?.kind == kind) {
                reactions[messageId].orEmpty().filterNot { it.userId == activeUserId }
            } else {
                reactions[messageId].orEmpty()
                    .filterNot { it.userId == activeUserId } +
                    CurioDmReaction(messageId, activeUserId, kind)
            }
            reactions = reactions + (messageId to optimistic)
            val result = if (mine?.kind == kind) {
                SocialApi.clearReaction(activeToken, messageId)
            } else {
                SocialApi.react(activeToken, messageId, kind)
            }
            result.fold(
                onSuccess = {
                    SocialApi.reactions(activeToken, listOf(messageId))
                        .onSuccess { fresh ->
                            reactions = reactions + (messageId to fresh.getOrElse(messageId) { emptyList<CurioDmReaction>() })
                        }
                },
                onFailure = {
                    reactions = reactions + (messageId to (mine?.let { listOf(it) } ?: emptyList()))
                    error = it.message
                }
            )
        }
    }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    suspend fun load(active: String, me: String) {
        loading = true
        SocialApi.messages(active, otherUserId, me).fold(
            onSuccess = { raw ->
                // Encryption is gone: every row carries its own words, so
                // there is nothing to unwrap and no placeholder to substitute.
                val fresh = raw
                // v3xx53 — the SERVER keeps 24 hours; the DEVICE keeps what it
                // received. Merging (rather than replacing) is what makes "gone
                // from the server" and "gone from Curio" two different things:
                // opening a conversation can never lose a message this phone
                // already had.
                //
                // The SERVER's copy wins wherever both hold the same id: it is
                // the only one that knows about an edit made on another device,
                // and the only one that carries `edited_at` and `reply_to` at
                // all. The device's copy is therefore merged in SECOND, and it
                // is what keeps a message the server has already forgotten on
                // screen. (The old order let a cached row shadow every fresh
                // row, which is why an edit showed as "edited" here and then
                // quietly lost its marker the next time the thread opened.)
                val hidden = SocialMessageCache.hiddenIds(context, otherUserId)
                val known = SocialMessageCache.read(context, otherUserId, me).filterNot { it.id in hidden }
                val merged = (fresh.filterNot { it.id in hidden } + known)
                    .distinctBy { it.id }
                    .sortedBy { it.createdAtMillis }
                messages = merged
                SocialMessageCache.write(context, otherUserId, merged)
                // Shadows whose words are now covered by a real server row
                // (same text, mine, within ten seconds) retire here — the
                // swap is invisible because both render the same bubble.
                sentShadow = sentShadow.filterNot { shadow ->
                    merged.any { real ->
                        real.mine && real.body == shadow.body &&
                            kotlin.math.abs(real.createdAtMillis - shadow.createdAtMillis) < 10_000L
                    }
                }
                error = null
                loadedOnce = true
            },
            onFailure = { failure ->
                // The cache already put something on screen; only speak up
                // when there was nothing to fall back on.
                if (messages.isEmpty()) error = failure.message
            }
        )
        loading = false
        // Reactions ride the same refresh, best-effort: a failure hides a
        // glyph, it never blanks the conversation.
        val ids = messages.map { it.id }
        if (ids.isNotEmpty()) {
            SocialApi.reactions(active, ids).onSuccess { reactions = it }
        }
    }

    // Who this conversation is with — resolved here so the header shows a
    // portrait and the LIVE username rather than the name the route carried.
    suspend fun loadPerson(active: String) {
        // `profile` rather than `people`: the peer card also draws the presence
        // line, which only the wider privacy read carries — and it falls back
        // to the base identity read on a project that has not been re-pasted.
        SocialApi.profile(active, otherUserId).onSuccess { fresh ->
            if (fresh != null) {
                person = fresh
                // Remembered so the NEXT open draws the real name instantly.
                SocialPeopleCache.remember(context, fresh)
            }
        }
    }

    // The message whose floating action sheet is up (Instagram style): the
    // palette, Copy, Edit and Remove ride ON the thread — no dialog, no scrim.
    // A second hold on the same bubble drops it.
    var actionTarget by remember(otherUserId) { mutableStateOf<CurioDirectMessage?>(null) }
    // The message being ANSWERED: the swipe raised it. Its words ride above
    // the composer until the answer is sent or the banner is dismissed.
    var quoteTarget by remember(otherUserId) { mutableStateOf<CurioDirectMessage?>(null) }
    // Quotes for replies whose parent fell out of the loaded window, fetched
    // OUTSIDE composition (a suspend read must never run inside a composable).
    var replyQuotes by remember(otherUserId) { mutableStateOf<Map<String, String>>(emptyMap()) }
    val haptics = LocalHapticFeedback.current
    // The message being EDITED: its words load into the composer and the
    // send button becomes Save until the edit is done or dropped.
    var editing by remember(otherUserId) { mutableStateOf<CurioDirectMessage?>(null) }

    /** Applies an edit from the composer; plaintext rows only. */
    suspend fun commitEdit(active: String, message: CurioDirectMessage) {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        SocialApi.editMessage(active, message.id, text).fold(
onSuccess = {
  messages = messages.map { current ->
    if (current.id == message.id) current.copy(body = text, editedAtMillis = System.currentTimeMillis()) else current
  }
  SocialMessageCache.write(context, otherUserId, messages)
  editing = null
  draft = ""
},
            onFailure = { failure -> error = failure.message ?: "Couldn't edit that message." }
        )
        sending = false
    }

    // Tracks outgoing messages so the auto-scroll LaunchedEffect can
    // scroll to bottom after every send (even when the user has scrolled
    // up to read history).
    var pendingSends by remember(otherUserId) { mutableStateOf(0) }
    // When the read stamp was last attempted, so an offline device retries it
    // on a slow cadence instead of on every tick.
    var readStampAt by remember(otherUserId) { mutableStateOf(0L) }

    /**
     * Stamps this conversation read, on the server and on screen.
     *
     * Entering a thread IS reading it: the inbox badge and the double tick the
     * sender sees are both built from this one stamp. The old rule (stamp only
     * when a NEW message arrived) left a thread the member had just read
     * flagged as unread in Chats, with the other side still on a single tick,
     * until somebody happened to send something else.
     */
    suspend fun markThreadRead(active: String, me: String) {
        if (messages.none { !it.mine && it.readAtMillis == null }) return
        // A failed stamp has to be retried (see below), but not once per tick
        // against a network that is simply down: the retry is paced.
        val now = System.currentTimeMillis()
        if (now - readStampAt < READ_STAMP_RETRY_MS) return
        readStampAt = now
        // The SERVER is what matters: the inbox badge and the other side's
        // second tick are both read from `read_at`. The device's copy is
        // therefore stamped only once the server agreed, so a stamp that failed
        // is not remembered as done while the other side still shows a single
        // tick - a later tick (or the next entry) simply tries again.
        SocialApi.markRead(active, otherUserId, me).onSuccess {
            val now = System.currentTimeMillis()
            messages = messages.map { message ->
                if (!message.mine && message.readAtMillis == null) message.copy(readAtMillis = now)
                else message
            }
            SocialMessageCache.write(context, otherUserId, messages)
        }
    }

    /**
     * Folds a delta into the thread IN PLACE: rows that are new are appended,
     * rows already on screen are replaced by the server's copy when they differ
     * (an edit, a read receipt). Answers true when something genuinely new
     * arrived, which is the caller's cue to stamp the conversation read.
     *
     * No page re-read is involved, which is the point: an edit or a receipt on
     * a row from an hour ago costs one small delta, not 200 rows.
     */
    fun applyMoved(moved: List<CurioDirectMessage>): Boolean {
        val byId = moved.associateBy { it.id }
        val known = messages.map { it.id }.toSet()
        val arrived = moved.any { it.id !in known }
        val changed = messages.any { held -> byId[held.id]?.let { it != held } == true }
        if (!arrived && !changed) return false
        messages = (messages.map { held -> byId[held.id] ?: held } + moved.filterNot { it.id in known })
            .sortedBy { it.createdAtMillis }
        SocialMessageCache.write(context, otherUserId, messages)
        // A stand-in whose words are now a real server row retires here too, so
        // the same reconciliation the initial load does also happens live.
        sentShadow = sentShadow.filterNot { shadow ->
            messages.any { real ->
                real.mine && real.body == shadow.body &&
                    kotlin.math.abs(real.createdAtMillis - shadow.createdAtMillis) < 10_000L
            }
        }
        return arrived
    }

    /**
     * Did a send that failed on the wire actually arrive?
     *
     * A read timeout is the CLIENT giving up, not the server refusing: the row
     * may be sitting in the conversation already. One small delta read answers
     * it, and a match is treated as the success it was, which is what stops a
     * slow connection from making the member leave the chat and reopen it to
     * find the message they were told had failed.
     */
    suspend fun confirmDelivered(
        active: String,
        me: String,
        optimisticText: String,
        sentAtMillis: Long
    ): Boolean {
        val newest = messages
            .filterNot { it.id.startsWith(LOCAL_ID_PREFIX) }
            .maxOfOrNull { it.createdAtMillis } ?: return false
        val known = messages.map { it.id }.toSet()
        val landed = SocialApi
            .messagesSince(active, otherUserId, me, newest)
            .getOrNull()
            .orEmpty()
            .firstOrNull { row ->
                row.mine && row.body == optimisticText && row.id !in known &&
                    kotlin.math.abs(row.createdAtMillis - sentAtMillis) < DELIVERED_WINDOW_MS
            } ?: return false
        applyMoved(listOf(landed))
        return true
    }

    /**
     * One full page of the conversation, applied as the server's truth.
     *
     * The only paths that need it are the two a stamp window cannot express:
     * a RECALL (the row is gone, so nothing moved to find) and a delta the
     * server refused. It is deliberately not the normal tick.
     */
    suspend fun rereadPage(active: String, me: String) {
        SocialApi.messages(active, otherUserId, me).getOrNull()?.let { server ->
            val hidden = SocialMessageCache.hiddenIds(context, otherUserId)
            messages = messages.mapNotNull { held ->
                when {
                    held.id in hidden -> null
                    held.id.startsWith(LOCAL_ID_PREFIX) -> held
                    // Gone from the server: they recalled it.
                    server.none { it.id == held.id } -> null
                    // Changed on the server: their edit wins.
                    else -> server.first { it.id == held.id }
                }
            }
            SocialMessageCache.write(context, otherUserId, messages)
        }
    }

    suspend fun send(active: String, me: String) {
        // An edit in flight takes the composer over: Send IS Save until the
        // edit is committed or dropped.
        editing?.let { target ->
            commitEdit(active, target)
            return
        }
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        error = null
        // A reply binds to the message raised above the composer; the banner
        // drops the moment the answer is on its way.
        // Replies are displayed against the exact bubble the user selected, but
        // the database stores every reply against the conversation root.
        val replyTo = quoteTarget?.replyTo ?: quoteTarget?.id
        quoteTarget = null

        // Optimistic: the bubble is on screen before the request leaves, and
        // its id is local-only so a refresh can never show it twice.
        val optimistic = CurioDirectMessage(
            id = "local-${System.currentTimeMillis()}",
            senderId = me,
            body = text,
            createdAtMillis = System.currentTimeMillis(),
            readAtMillis = null,
            mine = true,
            replyTo = replyTo
        )
        pending = pending + optimistic
        draft = ""
        pendingSends++

        SocialApi.sendPlaintext(active, otherUserId, text, me, replyTo).fold(
                onSuccess = { sent ->
                    SocialApi.setTyping(active, otherUserId, false)
                    pending = pending.filterNot { it.id == optimistic.id }
                    if (sent != null) {
                        // The server's own row takes the stand-in's place at
                        // once: its real id, its own clock, its receipt column.
                        // No page re-read, so a send costs one small request and
                        // what the member sees is the actual message. (The old
                        // path re-read 200 rows plus reactions on every tap,
                        // and on a slow connection the bubble sat as a stand-in
                        // for as long as that took.)
                        messages = (messages.filterNot { it.id == sent.id } + sent)
                            .sortedBy { it.createdAtMillis }
                        SocialMessageCache.write(context, otherUserId, messages)
                    } else {
                        // A project that would not return the row keeps the
                        // stand-in until a pull finds it. It never vanishes meanwhile.
                        sentShadow = sentShadow + optimistic
                    }
                },
                onFailure = { failure ->
                    pending = pending.filterNot { it.id == optimistic.id }
                    // A read timeout is OUR deadline, not the server's refusal:
                    // the row may be in the conversation already. Ask once
                    // before telling the member it failed and handing the words
                    // back - that ambiguity is what made a slow send look like
                    // a lost message until the chat was reopened.
                    if ((failure as? CommunityError)?.transport == true &&
                        confirmDelivered(active, me, text, optimistic.createdAtMillis)
                    ) {
                        error = null
                        return@fold
                    }
                    draft = text
                    error = failure.message ?: "That message didn't send."
                }
            )
        sending = false
    }

    LaunchedEffect(eligible, token, myUserId) {
        if (!eligible || token == null || myUserId == null) {
            messages = emptyList()
            pending = emptyList()
            sentShadow = emptyList()
            reactions = emptyMap()
            peerTyping = false
            return@LaunchedEffect
        }
        // The device's copy first — the account id is known here, so "mine"
        // is labelled correctly — then the server's own record.
        SocialMessageCache.migrateIfNeeded(context)
        val cached = SocialMessageCache.read(context, otherUserId, myUserId)
        if (cached.isNotEmpty()) messages = cached
        load(token, myUserId)
        loadPerson(token)
        // The thread is on screen, which IS the act of reading it: everything
        // received is stamped here, and arrivals use the same rule in
        // pullDelta below. Never mark a SENDER's messages read merely because
        // the sender refreshed their own thread.
        markThreadRead(token, myUserId)
    }

    // "is typing…" — pushed by a `dm_typing` frame, and re-read on a timer as
    // the safety net (the push shows it the instant the other side starts; the
    // timer is what CLEARS a row whose writer stopped refreshing it). Fast only
    // while the push channel is down.
    LaunchedEffect(eligible, token, myUserId) {
        if (!eligible || token == null || myUserId == null) return@LaunchedEffect
        while (true) {
            peerTyping = SocialApi.isTyping(token, myUserId, otherUserId)
            delay(if (SupabaseRealtime.isLinked) TYPING_SAFETY_TICK_MS else TYPING_TICK_MS)
        }
    }

    // THE THREAD'S DELTA — one small pull, shared by the push and the timer.
    //
    // It asks for every row that MOVED since the newest one on screen: a new
    // message, an edit by either side, or a read receipt (see
    // SocialApi.messagesSince). What comes back is merged IN PLACE, so an edit
    // lands as a new body on the bubble that is already there and a receipt
    // flips a tick, both without a page re-read.
    suspend fun pullDelta(active: String, me: String) {
        // Only CONFIRMED messages anchor the window: an optimistic bubble
        // carries the phone's own clock, and a fast phone would otherwise push
        // the window past the very rows this pull exists to find.
        val anchor = messages
            .filterNot { it.id.startsWith(LOCAL_ID_PREFIX) }
            .maxOfOrNull { it.createdAtMillis }
            ?: return
        val delta = SocialApi.messagesSince(active, otherUserId, me, anchor)
        val moved = delta.getOrNull().orEmpty()
        val arrived = if (moved.isEmpty()) false else applyMoved(moved)
        // A realtime hint can also be a DELETE, which no stamp window can see
        // (the row is simply gone). One page read per hint is what keeps a
        // recall honest; edits and arrivals no longer need it. A delta the
        // server REFUSED degrades to the same read instead of leaving the
        // thread silent - the chat must never depend on one query shape.
        if (pendingRealtimeRevisions || delta.isFailure) {
            pendingRealtimeRevisions = false
            rereadPage(active, me)
        }
        if (arrived) peerTyping = false
        markThreadRead(active, me)
    }

    /**
     * The live bits that are NOT part of the message delta: the other side's
     * "is typing…" row and their reactions on the messages already on screen.
     *
     * Both are tiny RLS-protected reads, driven by a realtime hint instead of a
     * tick. Reactions are re-read whole (rather than merged) because a REMOVED
     * reaction has no row to merge from — the other person taking their glyph
     * back is exactly as live as them leaving one.
     */
    suspend fun refreshLiveBits(active: String, me: String) {
        peerTyping = SocialApi.isTyping(active, me, otherUserId)
        val ids = messages.map { it.id }.filterNot { it.startsWith(LOCAL_ID_PREFIX) }
        if (ids.isEmpty()) return
        SocialApi.reactions(active, ids).onSuccess { fresh -> reactions = fresh }
    }

    // REALTIME — the server tells this screen when the thread moved, instead of
    // a timer asking. The bindings are all SERVER-filtered: their new messages
    // and my own message being read (dm_messages), their "is typing…" row, and
    // a reaction from them (the row's primary key carries the reactor, so
    // INSERT/UPDATE/DELETE all match the filter).
    //
    // They ride TWO owners on purpose. The message bindings are the ones that
    // can mean "a row you hold was revised" (their edit, their recall), which
    // is what the revision sweep exists for; the typing and reaction bindings
    // can never mean that, and letting them raise the same flag made every
    // keystroke of theirs buy a full page read. The subscription is released
    // the moment the screen goes away.
    DisposableEffect(eligible, token, myUserId, otherUserId) {
        val active = token
        val me = myUserId
        val owner = "dm:$otherUserId"
        val liveOwner = "dm:$otherUserId:live"
        if (eligible && active != null && me != null) {
            SupabaseRealtime.watch(
                owner = owner,
                accessToken = active,
                watches = listOf(
                    RealtimeWatch(
                        table = "dm_messages",
                        // INSERT is their new message; UPDATE catches their
                        // edit; DELETE their recall. The next delta pull reads
                        // the moved rows, and a revision flag asks for the whole
                        // page once so a RECALL (a row that is simply gone) lands.
                        filter = "sender=eq.$otherUserId",
                        events = listOf("INSERT", "UPDATE", "DELETE")
                    ),
                    RealtimeWatch(
                        table = "dm_messages",
                        filter = "recipient=eq.$otherUserId",
                        events = listOf("UPDATE")
                    )
                )
            ) {
                // Compose state is written on the composition's own scope, never
                // from the socket thread.
                scope.launch { pendingRealtimeRevisions = true }
                scope.launch { pushed++ }
            }
            SupabaseRealtime.watch(
                owner = liveOwner,
                accessToken = active,
                watches = listOf(
                    RealtimeWatch(
                        table = "dm_typing",
                        filter = "sender=eq.$otherUserId",
                        events = listOf("INSERT", "UPDATE")
                    ),
                    RealtimeWatch(
                        table = "dm_reactions",
                        filter = "user_id=eq.$otherUserId",
                        events = listOf("INSERT", "UPDATE", "DELETE")
                    )
                )
            ) {
                // The live bits that are not part of the message delta — the
                // typing row and their reactions — are re-read right away.
                scope.launch { refreshLiveBits(active, me) }
            }
        }
        onDispose {
            SupabaseRealtime.unwatch(owner)
            SupabaseRealtime.unwatch(liveOwner)
        }
    }

    // A push means "fetch now". The very first run is skipped (pushed == 0):
    // entry already loaded the thread, and this effect exists for arrivals.
    LaunchedEffect(pushed, eligible, token, myUserId, otherUserId) {
        if (!eligible || token == null || myUserId == null || pushed == 0) return@LaunchedEffect
        pullDelta(token, myUserId)
    }

    // THE FALLBACK TIMER. With realtime linked this is only a safety net — a
    // missed frame, a socket that dropped silently — so it stays deliberately
    // slow. Without realtime it is the whole mechanism, at the original
    // cadence, which is why a blocked WebSocket never freezes the thread.
    LaunchedEffect(eligible, token, myUserId, otherUserId) {
        if (!eligible || token == null || myUserId == null) return@LaunchedEffect
        while (true) {
            delay(if (SupabaseRealtime.isLinked) SAFETY_TICK_MS else LIVE_TICK_MS)
            pullDelta(token, myUserId)
        }
    }

    // Tell the other side when this side is writing: a short debounce stops a
    // request per keystroke, and the row clears itself after a pause.
    LaunchedEffect(draft, eligible, token) {
        if (!eligible || token == null) return@LaunchedEffect
        if (draft.isBlank()) {
            SocialApi.setTyping(token, otherUserId, false)
            return@LaunchedEffect
        }
        delay(700)
        SocialApi.setTyping(token, otherUserId, true)
        delay(6_000)
        SocialApi.setTyping(token, otherUserId, false)
    }

    val thread = remember(messages, pending, sentShadow) {
        // Shadows sit BETWEEN the confirmed rows and the still-sending tail:
        // they are delivered as far as anyone can see, and they sort by their
        // own (phone) clock like the optimistic rows do.
        messages + (sentShadow + pending).sortedBy { it.createdAtMillis }
    }        // The rows that sit ABOVE the messages in the same LazyColumn. Scrolling
    // needs the message's real index, not its index within the conversation.
    val headerRows = (if (wide) 1 else 0) +
        1 + // the peer card
        (if (error != null) 1 else 0) +
        (if (thread.isEmpty() && !loading && loadedOnce) 1 else 0)

    // Keep the newest line in view — on open, after every send, on an edit
    // or a reaction (both change a row's height), and when the other side
    // starts typing under us. The jump is suppressed while the member has
    // scrolled UP to read history; a message arriving mid-scroll must never
    // yank the list out from under their finger.
    var hasPresentedThread by remember(otherUserId) { mutableStateOf(false) }
    val pinnedToNewest = !listState.canScrollForward || !hasPresentedThread
    LaunchedEffect(thread.size, peerTyping, headerRows) {
        if (thread.isEmpty()) return@LaunchedEffect
        val newest = headerRows + thread.lastIndex + if (peerTyping) 1 else 0
        if (!hasPresentedThread) {
            listState.scrollToItem(newest)
            hasPresentedThread = true
        } else if (pinnedToNewest) {
            listState.animateScrollToItem(newest)
        }
    }
    // v-fix — SENDING always scrolls to bottom, even when the member has
    // scrolled up to read history. The main LaunchedEffect above skips the
    // scroll when pinnedToNewest is false (reading history), so a send
    // while scrolled up used to drop the optimistic bubble off-screen.
    LaunchedEffect(pendingSends) {
        if (pendingSends == 0) return@LaunchedEffect
        // +1 for the optimistic message that is about to enter [thread]
        listState.animateScrollToItem(headerRows + thread.lastIndex + 1)
    }

    // The DISPLAY name wins over the name the route carried, so a rename shows
    // up in the conversation too. A locally remembered or route-carried person
    // has no @username yet — the peer card then shows their name alone rather
    // than a made-up handle.
    val fallback = person?.label?.takeIf { it.isNotBlank() }
        ?: handle.ifBlank { "Message" }
    // The hero (and the collapsed bar) carry the display name; the @username
    // and the presence line sit on the peer card beneath it.
    val title = fallback

    // Read receipts are per-row now: a bubble shows ticks only when THAT row
    // carries the other side's read stamp — no newest-row inference that can
    // claim a reading that never happened.

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                heroPageBackground(
                    lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)
                )
            )
    ) {
        if (!wide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }

        Column(
            modifier = Modifier
                .layerBackdrop(glassBackdrop)
                .fillMaxSize()

        ) {
            if (eligible && token != null && myUserId != null) {
                val activeToken = token
                val activeUserId = myUserId

                fun pickReaction(messageId: String, kind: String) {
                    pickReactionScreen(messageId, kind, activeToken, activeUserId)
                }

                // A double-tap IS a heart (Instagram's gesture): the first
                // palette emoji, picked on the spot — and cleared again by the
                // same double-tap when it is already the active reaction.
                fun doubleTapReaction(messageId: String) {
                    pickReaction(messageId, SocialReactions.PALETTE.first().first)
                }

                fun beginReply(message: CurioDirectMessage) {
                    editing = null
                    quoteTarget = message
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                // A reply whose parent is no longer in the window still shows
                // its words: fetch them once, outside composition, into the
                // small quote map. RLS limits the read to this conversation.
                val unresolvedQuoteIds = thread
                    .mapNotNull { it.replyTo }
                    .filter { id -> thread.none { it.id == id } && replyQuotes[id] == null }
                    .distinct()
                LaunchedEffect(unresolvedQuoteIds) {
                    if (unresolvedQuoteIds.isEmpty()) return@LaunchedEffect
                    val active = activeToken
                    val me = activeUserId
                    if (active == null || me == null) return@LaunchedEffect
                    unresolvedQuoteIds.forEach { id ->
                        SocialApi.replyPreview(active, id, me).getOrNull()?.let { row ->
                            if (row.body.isNotBlank()) {
                                replyQuotes = replyQuotes + (id to row.body)
                            }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = wideContentEdgePadding(),
                        end = wideContentEdgePadding(),
                        top = if (wide) 0.dp else SettingsHeroTotalHeight,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (wide) {
                        item(key = "hero", contentType = "hero") {
                            SettingsHeroHeader(
                                title = title,
                                subtitle = if (peerTyping) "Typing…" else person?.handleLabel.orEmpty(),
                                onBack = { navController.popBackStack() },
                                trailing = { ink -> PeerMutePill(userId = otherUserId, ink = ink) }
                            )
                        }
                    }

                    item(key = "peer") {
                        MessagePeerHeader(
                            person = person,
                            fallback = fallback,
                            typing = peerTyping,
                            activityAtMillis = thread.lastOrNull { !it.mine }?.createdAtMillis,
                            onOpenProfile = {
                                navController.navigate(CurioRoutes.socialProfile(otherUserId)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    error?.let { message -> item(key = "error") { SocialNote(message, true) } }

                    if (thread.isEmpty() && !loading && loadedOnce) {
                        item(key = "empty") {
                            SocialEmptyCard(
                                icon = CurioIcons.Notes,
                                title = "Nothing said yet",
                                body = "This thread is only the two of you — start it with a hello."
                            )
                        }
                    }

                    itemsIndexedWithDays(thread) { index, message, _, firstOfRun, lastOfRun ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MessageEntry(
                                message = message,
                                firstOfRun = firstOfRun,
                                lastOfRun = lastOfRun,
                                // Per-row truth: a tick pair only when THIS
                                // row carries the other side's read stamp.
                                receipt = message.readAtMillis,
                                actionSheet = actionTarget?.id == message.id,
                                reactions = reactions[message.id].orEmpty(),
                                myUserId = activeUserId,
                                onDoubleTap = { doubleTapReaction(message.id) },
                                // A second hold on the same bubble drops the sheet.
                                onHold = {
                                    actionTarget = if (actionTarget?.id == message.id) null else message
                                },
                                onSwipeReply = { beginReply(message) },
                                onReply = { beginReply(message) },
                                onPick = { kind -> pickReaction(message.id, kind) },
                                onCopy = {
                                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                        as? android.content.ClipboardManager
                                    clip?.setPrimaryClip(
                                        android.content.ClipData.newPlainText("message", message.body)
                                    )
                                    actionTarget = null
                                },
                                onEdit = {
                                    editing = message
                                    draft = message.body
                                    actionTarget = null
                                },
                                onRemove = if (message.mine && !message.id.startsWith(LOCAL_ID_PREFIX)) {
                                    {
                                        actionTarget = null
                                        scope.launch {
                                            SocialApi.deleteMessage(activeToken, message.id).fold(
                                                onSuccess = {
                                                    messages = messages.filterNot { it.id == message.id }
                                                    SocialMessageCache.write(context, otherUserId, messages)
                                                },
                                                onFailure = { error = it.message ?: "Couldn't remove that message." }
                                            )
                                        }
                                    }
                                } else null,
                                quoteOf = message.replyTo?.let { target ->
                                    (thread + pending + sentShadow).firstOrNull { it.id == target }?.body
                                        ?: replyQuotes[target]
                                },
                                animateIn = message.id.startsWith(LOCAL_ID_PREFIX)
                            )
                        }
                    }

                    if (peerTyping && thread.isNotEmpty()) {
                        item(key = "typing") { TypingBubble(person) }
                    }
                }

                MessageComposer(
                    modifier = Modifier,
                    draft = draft,
                    title = fallback,
                    sending = sending,
                    editTarget = editing,
                    quoteTarget = quoteTarget,
                    onDropEdit = {
                        editing = null
                        draft = ""
                    },
                    onDropQuote = { quoteTarget = null },
                    onDraftChange = { if (it.length <= SocialApi.MAX_MESSAGE_CHARS) draft = it },
                    onSend = { scope.launch { send(activeToken, activeUserId) } }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = wideContentEdgePadding(),
                        end = wideContentEdgePadding(),
                        top = if (wide) 0.dp else SettingsHeroTotalHeight,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (wide) {
                        item(key = "hero", contentType = "hero") {
                            SettingsHeroHeader(
                                title = fallback,
                                subtitle = "Private messages",
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    item { SettingsSectionHeading("Before you start") }
                    item {
                        SettingsOptionCard {
                            when {
                                !OnlineAccount.configured -> SettingsOptionInfoRow(
                                    CurioIcons.Warning,
                                    "Not set up in this build",
                                    "This build has no Curio online project, so messages can't load."
                                )
                                !account.signedIn -> {
                                    SettingsOptionInfoRow(
                                        CurioIcons.Info,
                                        "Sign in to message",
                                        "Messages are tied to a Curio account."
                                    )
                                    SettingsOptionRow(
                                        icon = CurioIcons.Person,
                                        title = "Sign in or create an account",
                                        subtitle = "",
                                        onClick = { navController.navigate(CurioRoutes.SETTINGS_ONLINE) }
                                    )
                                }
                                else -> {
                                    SettingsOptionInfoRow(
                                        CurioIcons.Info,
                                        "Online mode is off",
                                        "Nothing loads while Online mode is off."
                                    )
                                    SettingsOptionRow(
                                        icon = CurioIcons.Refresh,
                                        title = "Turn Online mode on",
                                        subtitle = "",
                                        onClick = { navController.navigate(CurioRoutes.SETTINGS_ONLINE) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!wide) {
            // The conversation's hero IS the peer: their portrait rides in the
            // title slot and the @username (or a live Typing… line) rides as
            // the subtitle, so the person you are writing to is named at the
            // top — not stated as a standing label with the person listed
            // below the fold.
            SettingsHeroHeader(
                title = person?.label ?: fallback,
                subtitle = if (peerTyping) "Typing…" else person?.handleLabel.orEmpty(),
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop,
                // The conversation's own bell: the SAME device-side mute the
                // shade's Mute action writes, so the two can never disagree.
                titleTrailing = { ink -> PeerMutePill(userId = otherUserId, ink = ink) },
                // The person LEADS the header — avatar first, then the name,
                // exactly like a messenger. The old titleTrailing slot put
                // them on the right edge, past the (empty) title.
                titleLeading = { ink ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                navController.navigate(CurioRoutes.socialProfile(otherUserId)) {
                                    launchSingleTop = true
                                }
                            }
                            .padding(end = 2.dp)
                    ) {
                        SocialAvatar(
                            seed = blobatarSeed(person?.userId, person?.username),
                            avatarSize = 38.dp,
                            online = person?.isActiveNow == true
                        )
                    }
                }
            )
        }
    }
}

/**
 * Walks a conversation once and hands each row its framing: the day rule it
 * opens, and whether it starts or ends a run from one person.
 *
 * Grouping is what makes a long thread readable — without it every line wears
 * its own timestamp and portrait and the conversation reads as a log rather
 * than as people talking. A run breaks on a change of sender, a gap of more
 * than five minutes, or a new day.
 */
private fun dmTimeStamp(millis: Long): String {
    if (millis <= 0L) return ""
    return java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedWithDays(
    thread: List<CurioDirectMessage>,
    row: @Composable (index: Int, message: CurioDirectMessage, dayLabel: String?, first: Boolean, last: Boolean) -> Unit
) {
    items(thread.size, key = { thread[it].id }) { index ->
        val message = thread[index]
        val previous = thread.getOrNull(index - 1)
        val next = thread.getOrNull(index + 1)
        val sameAsPrevious = previous != null &&
            previous.mine == message.mine &&
            message.createdAtMillis - previous.createdAtMillis <= 5 * 60 * 1000
        val sameAsNext = next != null &&
            next.mine == message.mine &&
            next.createdAtMillis - message.createdAtMillis <= 5 * 60 * 1000
        row(
            index,
            message,
            null,
            !sameAsPrevious,
            !sameAsNext
        )
    }
}

/**
 * THE CONVERSATION'S BELL (v389) — whether THIS friend's messages are
 * announced on the shade.
 *
 * It is not a second setting: it writes exactly the same device-side flag the
 * notification's own Mute action writes ([AppPreferences.mutedConversations]),
 * so muting from the shade lights this bell and muting here silences the
 * shade. The glyph never changes (the bundled symbol subset has no bell-off,
 * and a missing glyph renders as its literal name) — the MUTED state is the
 * quiet one: dim ink, no container.
 */
@Composable
private fun PeerMutePill(userId: String, ink: Color) {
    val context = LocalContext.current
    val muted = userId in AppPreferences.mutedConversationsState
    Surface(
        onClick = { AppPreferences.setConversationMuted(context, userId, !muted) },
        shape = CircleShape,
        color = if (muted) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(38.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CurioIcon(
                CurioIcons.Notifications,
                if (muted) "Muted — tap to unmute" else "Mute this conversation",
                tint = if (muted) ink.copy(alpha = 0.32f) else ink.copy(alpha = 0.8f),
                size = 18.dp
            )
        }
    }
}

/**
 * Who you are talking to: the portrait, the live username and a tap that opens
 * their profile. When they are writing the @username gives way to a live
 * "Typing…" line, so the header answers the question the thread is about to
 * ask without any standing paragraph about it.
 */
@Composable
private fun MessagePeerHeader(
    person: CurioPerson?,
    fallback: String,
    typing: Boolean,
    activityAtMillis: Long?,
    onOpenProfile: () -> Unit
) {
    val dark = isCurioDarkTheme()
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenProfile)
                .padding(14.dp)
        ) {
            SocialAvatar(
                seed = blobatarSeed(person?.userId, person?.username),
                avatarSize = 46.dp,
                // The dot and the presence line under the name are the SAME
                // fact, so they can never disagree.
                online = person?.isActiveNow == true
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = person?.label ?: fallback,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (typing) {
                    Text(
                        text = "Typing…",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = curioDialogActionColor()
                    )
                } else {
                    // The @username, and — only when the other member left
                    // activity visible — a quiet presence line beside it.
                    Text(
text = listOfNotNull(
                            person?.handleLabel,
                            activityAtMillis?.let { sentAt ->
                                val ageMinutes = ((System.currentTimeMillis() - sentAt).coerceAtLeast(0L) / 60_000L)
                                when {
                                    ageMinutes < 5L -> "Active now"
                                    ageMinutes < 60L -> "Active ${ageMinutes}m ago"
                                    ageMinutes < 24L * 60L -> "Active ${ageMinutes / 60L}h ago"
                                    else -> null
                                }
                            }
                        ).joinToString(" · ").ifBlank { "Open profile" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            CurioIcon(
                name = CurioIcons.ChevronRight,
                contentDescription = "Open profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 18.dp
            )
        }
    }
}

/**
 * One message, its framing and — when tapped — the reaction palette under it.
 *
 * [animateIn] is set only for messages this device just sent, so a bubble
 * a user wrote arrives on a spring while history arrives still.
 */
@Composable
private fun MessageEntry(
    message: CurioDirectMessage,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
    receipt: Long?,
    actionSheet: Boolean,
    reactions: List<CurioDmReaction>,
    myUserId: String,
    onDoubleTap: () -> Unit,
    onHold: () -> Unit,
    onSwipeReply: () -> Unit,
    onReply: () -> Unit,
    onPick: (String) -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRemove: (() -> Unit)?,
    quoteOf: String?,
    animateIn: Boolean
) {
    // `initial = !animateIn` is what makes this safe to use for EVERY row: a
    // historic message starts already visible (no animation at all), while a
    // message this device just sent starts hidden and springs in. A row never
    // animates OUT — the optimistic bubble is replaced by the server's copy of
    // the same text in the same frame, and an exit animation there would read
    // as the message being taken away.
    val visible = remember {
        MutableTransitionState(!animateIn).apply { if (animateIn) targetState = true }
    }
    AnimatedVisibility(
        visibleState = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) +
            fadeIn(animationSpec = tween(CurioMotion.Durations.Quick)),
        exit = ExitTransition.None
    ) {
        // The action sheet floats ABOVE the bubble (Instagram-style).
        // A full-width invisible tap target sits above it so tapping
        // anywhere outside the pills dismisses the sheet.
        Box(modifier = Modifier.fillMaxWidth()) {
            if (actionSheet) {
                Popup(
                    alignment = if (message.mine) Alignment.TopEnd else Alignment.TopStart,
                    offset = IntOffset(0, with(LocalDensity.current) { (-96).dp.roundToPx() }),
                    properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
                    onDismissRequest = onHold
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(CurioMotion.Durations.Quick)) +
                            slideInVertically { -it / 3 }
                    ) {
                        MessageActionSheet(
                            mine = message.mine,
                            current = reactions.firstOrNull { it.userId == myUserId }?.kind,
                            canEdit = AppPreferences.socialTextEditingState && message.mine && message.editableText &&
                                !message.id.startsWith(LOCAL_ID_PREFIX),
                            canRemove = onRemove != null,
                            onPick = onPick,
                            onCopy = onCopy,
                            onEdit = onEdit,
                            onRemove = onRemove,
                            onReply = onReply
                        )
                    }
                }
            }
            MessageBubble(
                message = message,
                firstOfRun = firstOfRun,
                lastOfRun = lastOfRun,
                receipt = receipt,
                reactions = reactions,
                myUserId = myUserId,
                onDoubleTap = onDoubleTap,
                onHold = onHold,
                onSwipeReply = onSwipeReply,
                quoteOf = quoteOf
            )
        }
    }
}

/**
 * One message. Yours sits on the right in the brand rose, theirs on the left
 * on the raised surface — the reading direction of every messenger, so who
 * said what needs no label. Corners open up on the first line of a run and
 * only the last line of a run gets the full rounding and the compact time.
 * Both fills are OPAQUE: a translucent bubble let the background bleed
 * through and read as unfinished.
 *
 * Gestures: a DOUBLE-TAP is the heart (Instagram's), a HOLD raises the
 * floating action sheet (reactions, copy, edit, remove), and a horizontal
 * SWIPE answers the message — the bubble leans with the finger and snaps
 * back when the reply is armed.
 */
@Composable
private fun MessageBubble(
    message: CurioDirectMessage,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
    receipt: Long?,
    reactions: List<CurioDmReaction>,
    myUserId: String,
    onDoubleTap: () -> Unit,
    onHold: () -> Unit,
    onSwipeReply: () -> Unit,
    quoteOf: String?
) {
    val mine = message.mine
    val dark = isCurioDarkTheme()
    val haptics = LocalHapticFeedback.current
    val mineGlyph = reactions.firstOrNull { it.userId == myUserId }?.kind
    val others = reactions.filterNot { it.userId == myUserId }
    // ── v453 — THE BUBBLE'S INK ASKS THE BUBBLE, ON BOTH SIDES ─────────
    //
    // The member: *"in chats the theme dark chat bubble color is a little bad
    // sometimes the texts blend"*. v412 fixed exactly this — a fill's ink must be
    // MEASURED, never assumed — but it fixed it for MY bubble only: a received
    // message still took plain `onSurface` whatever its fill was, and its quote,
    // its timestamp and its "edited" mark took `onSurfaceVariant` further down the
    // alpha ramp. On a theme whose container ladder carries colour (or in dark
    // mode, where the received fill is the HIGHEST neutral step) the words could
    // land close enough to the fill to blend — and "sometimes" is what a theme's
    // own colours do to a hardcoded ink.
    //
    // So the fill is one value that both the surface and its ink read, theirs
    // included, and every piece of text in the bubble asks it. In dark mode the
    // received fill is nudged a hair toward the ink as well, so the bubble reads as
    // a bubble against the dark page instead of as a hole in it — an opaque lerp of
    // two opaque colours, never an alpha (a translucent fill is what lets a shadow
    // bleed through, see the root rail's rule).
    val bubbleFill = when {
        // Mine: the brand rose, both themes — no more dark bubble that read as
        // the other person's.
        mine -> curioDialogActionColor()
        // Theirs: a raised neutral, clearly not the accent.
        dark -> lerp(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurface,
            0.06f
        )
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val bubbleInk = curioFillInk(bubbleFill)
    val shape = if (mine) {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = if (firstOfRun) 20.dp else 7.dp,
            bottomStart = 7.dp,
            bottomEnd = if (lastOfRun) 20.dp else 7.dp
        )
    } else {
        RoundedCornerShape(
            topStart = if (firstOfRun) 20.dp else 7.dp,
            topEnd = 20.dp,
            bottomStart = if (lastOfRun) 20.dp else 7.dp,
            bottomEnd = 7.dp
        )
    }

    // The swipe-to-answer gesture: the bubble follows the finger up to a
    // short travel, then the reply banner raises. The lean tracks the finger
    // exactly while dragging (snap) and springs back to rest on release.
    var replyDrag by remember { mutableStateOf(0f) }
    val dragLimit = with(LocalDensity.current) { 44.dp.toPx() }
    val settle = animateFloatAsState(
        targetValue = replyDrag,
        animationSpec = if (replyDrag == 0f) spring(dampingRatio = 0.6f, stiffness = 500f) else snap(),
        label = "replySettle"
    )
    // The bubble follows the finger in the same direction for both sides;
    // only the allowed drag direction differs between sent and received rows.
    val leanX = settle.value

    // The side IS the label: mine right (Arrangement.End), theirs left
    // (Arrangement.Start). A weighted spacer used to sit before a received
    // bubble, which ate the free space and pushed every short reply of theirs
    // to the RIGHT — the alignment arrangement alone is what puts a bubble on
    // its side.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start
        ) {
            val press = rememberCurioPressSource(pressedScale = 0.96f)
            Box(modifier = Modifier.offset { IntOffset(leanX.roundToInt(), 0) }) {
                Surface(
                    shape = shape,
                    // One fill, and the ink above read it (v453 — see the note at
                    // `bubbleFill`).
                    color = bubbleFill,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .then(press.modifier)
                        // HORIZONTAL ONLY: the thread must stay scrollable
                        // UNDER a long message, so the bubble watches for a
                        // sideways drag and lets a vertical one fall through to
                        // the list (a plain drag detector consumed every drag,
                        // which is what made a long bubble impossible to scroll
                        // past).
                        .pointerInput(message.id, mine) {
                            detectHorizontalDragGestures(
                                onDragStart = { replyDrag = 0f },
                                onDragEnd = {
                                    if (kotlin.math.abs(replyDrag) >= dragLimit * 0.85f) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSwipeReply()
                                    }
                                    replyDrag = 0f
                                },
                                onDragCancel = { replyDrag = 0f }
                            ) { _, amount ->
                                val raw = replyDrag + amount * 0.45f
                                replyDrag = if (mine) raw.coerceIn(-dragLimit, 0f)
                                else raw.coerceIn(0f, dragLimit)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .combinedClickable(
                                interactionSource = press.interactionSource,
                                indication = LocalIndication.current,
                                onClickLabel = "Message options",
                                onLongClickLabel = "Message options",
                                // The HOLD is the message's decision point: it
                                // raises the floating sheet (reactions, copy,
                                // edit, remove). A second hold drops it.
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onHold()
                                },
                                onDoubleClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDoubleTap()
                                },
                                onClick = {}                            )
                            .padding(horizontal = 14.dp, vertical = 9.dp)) {
                        if (message.replyTo != null) {
                            // An answer never silently loses its quote: a parent
                            // that has fallen out of the 24-hour window says so
                            // instead of the row just vanishing from the bubble.
                            ReplyQuoteRow(
                                quoted = quoteOf ?: "This message is no longer available",
                                ink = bubbleInk
                            )
                        }
                        Text(
                            text = message.body,
                            style = MaterialTheme.typography.bodyMedium,
                            // v412/v453 — the ink ASKS the bubble fill (see
                            // [curioFillInk]), whichever side the bubble is on.
                            color = bubbleInk
                        )
                        if (message.editedAtMillis != null) {
                            Text(
                                text = "edited",
                                style = MaterialTheme.typography.labelSmall,
                                // v453 — it was a HARDCODED white on my bubble (the
                                // one line v412 missed), which is exactly what
                                // vanishes on dark mode's pale rose; it asks the fill
                                // like everything else here now.
                                color = bubbleInk.copy(alpha = 0.7f)
                            )
                        }
                        if (lastOfRun) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = dmTimeStamp(message.createdAtMillis),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        lineHeight = 8.sp
                                    ),
                                    color = bubbleInk.copy(alpha = 0.72f)
                                )
                                if (mine) {
                                    Text(
                                        text = if (receipt != null) "\u2713\u2713" else "\u2713",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            lineHeight = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        // v412/v453 — asks the bubble fill (deep-ink path).
                                        color = bubbleInk.copy(alpha = if (receipt != null) 0.86f else 0.62f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // The reaction chips sit OUTSIDE the bubble, on its tail corner, so
            // the bubble keeps its own shape and a reaction never reflows the
            // text it is attached to.
            if (mineGlyph != null || others.isNotEmpty()) {
                ReactionChips(mineGlyph = mineGlyph, others = others)
            }
        }

    }
}

/** The reaction chips already on a message — mine first, then everyone else's. */
@Composable
private fun ReactionChips(mineGlyph: String?, others: List<CurioDmReaction>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 3.dp)
    ) {
        if (mineGlyph != null) {
            ReactionChip(kind = mineGlyph, count = 1, mine = true)
        }
        others.groupBy { it.kind }.forEach { (kind, group) ->
            ReactionChip(kind = kind, count = group.size, mine = false)
        }
    }
}

@Composable
private fun ReactionChip(kind: String, count: Int, mine: Boolean) {
    val ink = if (mine) curioDialogActionColor() else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(50),
        color = if (mine) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = SocialReactions.emojiFor(kind),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = ink
            )
            if (count > 1) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ink
                )
            }
        }
    }
}

/**
 * The quick quote above an answer: the parent's words, one soft bar. Instagram
 * renders this INSIDE the bubble and Curio does too — the bar is deliberately
 * quiet so the answer stays the loudest line.
 *
 * The ink follows the BUBBLE, not the brand: white on my rose bubble, the
 * page's own muted ink on their neutral one. Hardcoded white made a received
 * answer's quote invisible on the light theme.
 *
 * v385 — IT HUGS ITS TEXT. `fillMaxWidth()` here made the quote the widest
 * thing in the bubble, and a fill-width child forces its parent to that width:
 * every answer ballooned to the full thread width around one small line of
 * quoted words ("the reply box looks too big, too wide even though the text
 * was small"). The 34dp bar was sized for a two-line quote as well, so the
 * block towered over the single line it framed. The bar is now one line tall,
 * the quote is capped so a long parent cannot stretch the bubble either, and
 * the bubble is free to be as wide as its own message.
 */
@Composable
private fun ReplyQuoteRow(quoted: String, ink: Color) {
    Row(
        modifier = Modifier.padding(bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                // v453 — the quote's rule and its words wear the BUBBLE's own ink
                // (it used to be a fixed onSurfaceVariant on a received bubble,
                // which is one more tone that could land on the fill).
                .background(ink.copy(alpha = 0.55f))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = quoted,
            style = MaterialTheme.typography.labelMedium,
            color = ink.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // No weight/fill: a bounded width keeps the row wrap-content, so
            // the bubble stays as wide as its own words.
            modifier = Modifier.widthIn(max = 208.dp)
        )
    }
}

/**
 * The Instagram-style action bar: floating pills attached to the held bubble —
 * the emoji palette first, then Copy, Edit (mine, plaintext), Remove (mine)
 * and Reply. No dialog and no scrim: the thread stays readable, and a second
 * hold anywhere drops the bar.
 */
@Composable
private fun MessageActionSheet(
    mine: Boolean,
    current: String?,
    canEdit: Boolean,
    canRemove: Boolean,
    onPick: (String) -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRemove: (() -> Unit)?,
    onReply: () -> Unit
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = ExitTransition.None
    ) {
        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            // Row 1: emoji reaction palette — a frosted pill with reaction
            // emojis. Tapping the active one clears it (toggle).
            Surface(
                shape = RoundedCornerShape(50),
                color = curioDialogContainerColor(),
                shadowElevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    SocialReactions.PALETTE.forEach { (emoji, meaning) ->
                        val chosen = current != null && SocialReactions.emojiFor(current) == emoji
                        Surface(
                            onClick = { onPick(emoji) },
                            shape = CircleShape,
                            color = if (chosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent,
                            modifier = Modifier
                                .size(36.dp)
                                .semantics { contentDescription = meaning }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            // Row 2: text action chips — Reply, Copy, Edit, Remove. Each is
            // its own frosted pill so the destructive Remove stands out.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                ActionChip("Reply", onReply)
                ActionChip("Copy", onCopy)
                if (canEdit) ActionChip("Edit", onEdit)
                if (canRemove) ActionChip("Delete for everyone", { onRemove?.invoke() }, destructive = true)
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit, destructive: Boolean = false) {
    // A destructive action is a SOLID fill, not a wash: a 12%-alpha pill read
    // as a disabled button floating over the thread instead of as the one
    // decision that cannot be undone.
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (destructive)
            MaterialTheme.colorScheme.error
        else
            curioDialogContainerColor(),
        contentColor = if (destructive)
            MaterialTheme.colorScheme.onError
        else
            MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

/** Three breathing dots — the other person is writing, shown in the thread. */
@Composable
private fun TypingBubble(person: CurioPerson?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SocialAvatar(
            seed = blobatarSeed(person?.userId, person?.username),
            avatarSize = 26.dp
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 7.dp, topEnd = 18.dp, bottomStart = 7.dp, bottomEnd = 18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                (0..2).forEach { dot -> TypingDot(delayMillis = dot * 160L) }
            }
        }
    }
}

/** One dot of [TypingBubble], pulsing on its own offset so the row breathes. */
@Composable
private fun TypingDot(delayMillis: Long) {
    var up by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        while (true) {
            up = true
            delay(420)
            up = false
            delay(420)
        }
    }
    val lift by animateFloatAsState(
        targetValue = if (up) 1f else 0.55f,
        animationSpec = tween(400),
        label = "typingDot"
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .scale(lift)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f + 0.5f * lift))
    )
}

/**
 * The composer: a frosted pill you write in and one circular accent button
 * that sends. The send control does not appear — it grows the moment there is
 * something to send, which is the feedback that says "this is ready".
 */
@Composable
private fun MessageComposer(
  modifier: Modifier = Modifier,
  draft: String,
    title: String,
    sending: Boolean,
    /** When set, the composer is EDITING this message: Save replaces Send. */
    editTarget: CurioDirectMessage?,
    /** When set, the next send ANSWERS this message: the quote banner rides. */
    quoteTarget: CurioDirectMessage?,
    onDropEdit: () -> Unit,
    onDropQuote: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val dark = isCurioDarkTheme()
    // A direct message is deliberately NOT run through CurioContentFilter —
    // this is a private conversation between two friends, and the filter
    // guards the public surfaces instead. See SocialApi.send.
    val armed = draft.isNotBlank() && !sending
    val sendScale by animateFloatAsState(
        targetValue = if (armed) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "sendScale"
    )
    val nearLimit = draft.length > SocialApi.MAX_MESSAGE_CHARS * 8 / 10

  Column(
  modifier = modifier
  .fillMaxWidth()
  // The keyboard sits ON TOP of the composer otherwise: the app is
  // edge-to-edge (`setDecorFitsSystemWindows(false)`) and the NavHost only
  // delivers the navigation-bar inset, never the IME one. This lifts the pill
  // clear of the keyboard the moment it opens and returns it to the gesture
  // bar when it closes.
  .imePadding()
  .padding(
                start = wideContentEdgePadding(),
                end = wideContentEdgePadding(),
                bottom = 10.dp
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // The REPLY banner: the message being answered, with a focused tint
        // so it reads as "on the record" rather than as decoration.
        androidx.compose.animation.AnimatedVisibility(visible = quoteTarget != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(curioDialogActionColor())
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replying to ${if (quoteTarget?.mine == true) "you" else title}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = curioDialogActionColor(),
                            maxLines = 1
                        )
                        Text(
                            text = quoteTarget?.body.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelMedium,
                        color = curioDialogActionColor(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onDropQuote)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
        // The edit banner: what is being changed, and the way out. It sits
        // above the field so the composer's own height never jumps.
        androidx.compose.animation.AnimatedVisibility(visible = editTarget != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    CurioIcon(
                        name = CurioIcons.Edit,
                        contentDescription = null,
                        tint = curioDialogActionColor(),
                        size = 14.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Editing message",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelMedium,
                        color = curioDialogActionColor(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onDropEdit)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    // v385 — GROWS A ROW AT A TIME. One line stays exactly the
                    // pill it always was; Enter adds rows (up to five), then
                    // the field scrolls inside that height so the send button
                    // can never be pushed off the screen.
                    .heightIn(min = 52.dp, max = 140.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
                        else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(curioDialogActionColor()),
                    // v385 — ENTER MAKES A LINE, IT DOES NOT SEND. A message is
                    // often several rows (an address, a list, a thought in
                    // two beats), and ImeAction.Send turned the key people
                    // press at the end of a sentence into an accidental send.
                    // The round button is the only way to send.
                    singleLine = false,
                    maxLines = 5,
                    // A message is a sentence, not a word: capitals and
                    // sentence punctuation are the default here (the field is
                    // unlabelled, so this is the only cue it needs).
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    modifier = Modifier.weight(1f)
                ) { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (draft.isEmpty()) {
                            Text(
                                text = "Message $title",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        inner()
                    }
                }
            }
            Surface(
                onClick = { if (armed) onSend() },
                shape = CircleShape,
                color = if (armed) curioDialogActionColor()
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(52.dp)
                    .scale(sendScale)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        // An edit in flight turns the arrow into a check: the
                        // same button, a different verb.
                        name = if (editTarget != null) CurioIcons.Check else CurioIcons.ArrowForward,
                        contentDescription = if (editTarget != null) "Save edit" else "Send",
                        // v412 — asks the send disc's fill (see [curioFillInk]).
                        tint = if (armed) curioFillInk(curioDialogActionColor())
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 20.dp
                    )
                }
            }
        }
        if (nearLimit) {
            Text(
                text = "${draft.length}/${SocialApi.MAX_MESSAGE_CHARS}",
                style = MaterialTheme.typography.labelSmall,
                color = if (draft.length >= SocialApi.MAX_MESSAGE_CHARS) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/**
 * How often an OPEN conversation asks whether anything new arrived.
 *
 * 1.2s is deliberately tighter than a "poll every few seconds" cadence: each
 * tick asks only for messages newer than the newest one on screen, so it is a
 * few hundred bytes, and the point of the live thread is that a reply lands
 * while you are looking at it rather than a beat later. The very first tick
 * after an arrival is also what turns the typing row off and stamps the read
 * receipt, so the whole exchange moves at this cadence.
 */
private const val LIVE_TICK_MS = 1_200L

/**
 * The safety-net cadence used once realtime is linked: a missed frame or a
 * socket that died without saying so is still picked up.
 *
 * 4s rather than the old 20s: a channel that reports itself linked while its
 * bindings deliver nothing (a publication without the table, a policy the
 * server evaluates differently for the WAL than for REST) used to leave an
 * open conversation up to twenty seconds behind, which is indistinguishable
 * from "it never updates — I have to reopen the chat". The delta is now one
 * small request for every way a thread can move, so a safety tick is cheap.
 */
private const val SAFETY_TICK_MS = 4_000L

/**
 * The pacing of a RETRIED read stamp. The first stamp of a conversation is
 * immediate (nothing has been attempted yet); after a failure the retry waits
 * this long, so a device with no signal is not asked to stamp a receipt several
 * times a minute.
 */
private const val READ_STAMP_RETRY_MS = 10_000L

/**
 * How long after a send a timed-out row may still be recognised as delivered.
 *
 * A read timeout can land well after the tap (the request may have sat on a
 * slow connection), so the reconciliation window is wider than the 10s a
 * stand-in uses.
 */
private const val DELIVERED_WINDOW_MS = 120_000L

/**
 * How often the "is typing…" row is re-read when realtime is NOT linked.
 *
 * 1.5s is the old whole mechanism: the row is one tiny read of a single
 * server-stamped timestamp, and an indicator that arrives after the message it
 * was announcing is worse than none. Once the push channel is linked a
 * `dm_typing` frame shows it immediately, so this becomes a slow safety tick —
 * it also has to clear a writer that vanished without deleting its row.
 */
private const val TYPING_TICK_MS = 1_500L

/** The typing row's safety-net cadence once realtime is linked. */
private const val TYPING_SAFETY_TICK_MS = 5_000L

/** The id prefix of a bubble that is sent but not yet confirmed. */
private const val LOCAL_ID_PREFIX = "local-"
