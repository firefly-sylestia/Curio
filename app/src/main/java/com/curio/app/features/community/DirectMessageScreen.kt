package com.curio.app.features.community

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CurioDirectMessage
import com.curio.app.data.supabase.CurioDmReaction
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.RealtimeWatch
import com.curio.app.data.supabase.SocialApi
import com.curio.app.data.supabase.SupabaseRealtime
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
import com.curio.app.ui.theme.isCurioDarkTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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

    // Filled from the device's own copy the instant the account resolves, so
    // a conversation you have already had is never a blank screen.
    var messages by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
    // Sent-but-not-yet-confirmed messages, drawn exactly like real ones.
    var pending by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
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
    var reactionTarget by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadedOnce by remember { mutableStateOf(false) }
    // A server push bumps this, which re-runs the delta fetch below. It is a
    // COUNTER rather than a flag so two pushes in a row are two fetches, and it
    // is keyed on the thread so opening another conversation starts fresh.
    var pushed by remember(otherUserId) { mutableStateOf(0) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    suspend fun load(active: String, me: String) {
        loading = true
        SocialApi.messages(active, otherUserId, me).fold(
            onSuccess = { fresh ->
                messages = fresh
                SocialMessageCache.write(context, otherUserId, fresh)
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

    suspend fun send(active: String, me: String) {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        error = null

        // Optimistic: the bubble is on screen before the request leaves, and
        // its id is local-only so a refresh can never show it twice.
        val optimistic = CurioDirectMessage(
            id = "local-${System.currentTimeMillis()}",
            senderId = me,
            body = text,
            createdAtMillis = System.currentTimeMillis(),
            readAtMillis = null,
            mine = true
        )
        pending = pending + optimistic
        draft = ""

        SocialApi.send(active, otherUserId, text, me).fold(
            onSuccess = {
                SocialApi.setTyping(active, otherUserId, false)
                pending = pending.filterNot { it.id == optimistic.id }
                load(active, me)
            },
            onFailure = { failure ->
                // The bubble must not linger as if it were delivered.
                pending = pending.filterNot { it.id == optimistic.id }
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
        // A receipt is a courtesy, not a requirement: a failure here must never
        // blank a thread that loaded fine.
        SocialApi.markRead(token, otherUserId, myUserId)
    }

    // "is typing…" — polled, not pushed (Curio's online layer has no realtime
    // socket by design). A second and a half: the row is one tiny read of a
    // single server-stamped timestamp, and a typing indicator that arrives
    // after the message it was announcing is worse than none.
    LaunchedEffect(eligible, token, myUserId) {
        if (!eligible || token == null || myUserId == null) return@LaunchedEffect
        while (true) {
            peerTyping = SocialApi.isTyping(token, myUserId, otherUserId)
            delay(1_500)
        }
    }

    // THE THREAD'S DELTA — one small pull, shared by the push and the timer.
    //
    // It asks only for what is NEWER than the newest message already on screen
    // (a strict `created_at >` window, so a pull is a few hundred bytes),
    // merges it in, keeps the device's copy current and stamps the receipt.
    suspend fun pullDelta(active: String, me: String) {
        // Only CONFIRMED messages anchor the window: an optimistic bubble
        // carries the phone's own clock, and a fast phone would otherwise push
        // the window past the very messages this pull exists to find.
        val anchor = messages
            .filterNot { it.id.startsWith(LOCAL_ID_PREFIX) }
            .maxOfOrNull { it.createdAtMillis }
            ?: return
        val fresh = SocialApi.messagesSince(active, otherUserId, me, anchor)
            .getOrNull()
            .orEmpty()
            .filter { row -> (messages + pending).none { it.id == row.id } }
        // The receipts ride the same pull, whether or not anything new arrived:
        // they change when the other side READS, which is a different moment
        // from when they write.
        SocialApi.readStamps(active, otherUserId, me).onSuccess { stamps ->
            if (stamps.isNotEmpty()) {
                messages = messages.map { message ->
                    val at = stamps[message.id] ?: return@map message
                    if (message.readAtMillis == null || message.readAtMillis < at) {
                        message.copy(readAtMillis = at)
                    } else {
                        message
                    }
                }
            }
        }
        if (fresh.isEmpty()) return
        val merged = (messages + fresh)
            .distinctBy { it.id }
            .sortedBy { it.createdAtMillis }
        messages = merged
        SocialMessageCache.write(context, otherUserId, merged)
        // An arrival means the other side stopped writing.
        peerTyping = false
        if (fresh.any { !it.mine }) {
            SocialApi.markRead(active, otherUserId, me)
        }
        SocialApi.reactions(active, merged.map { it.id }).onSuccess { reactions = it }
    }

    // REALTIME — the server tells this screen when the thread moved, instead of
    // a timer asking. Two bindings, both SERVER-filtered: their new messages
    // (INSERT), and my own message being read (UPDATE on a row I sent them).
    // The subscription is released the moment the screen goes away.
    DisposableEffect(eligible, token, myUserId, otherUserId) {
        val active = token
        val me = myUserId
        val owner = "dm:$otherUserId"
        if (eligible && active != null && me != null) {
            SupabaseRealtime.watch(
                owner = owner,
                accessToken = active,
                watches = listOf(
                    RealtimeWatch(
                        table = "dm_messages",
                        filter = "sender=eq.$otherUserId",
                        events = listOf("INSERT")
                    ),
                    RealtimeWatch(
                        table = "dm_messages",
                        filter = "recipient=eq.$otherUserId",
                        events = listOf("UPDATE")
                    )
                )
            ) {
                // The push arrives on a socket thread; the counter is Compose
                // state, so the bump is posted to the composition's own scope.
                scope.launch { pushed++ }
            }
        }
        onDispose { SupabaseRealtime.unwatch(owner) }
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

    val thread = remember(messages, pending) { messages + pending }

    // The rows that sit ABOVE the messages in the same LazyColumn. Scrolling
    // needs the message's real index, not its index within the conversation.
    val headerRows = (if (wide) 1 else 0) +
        1 + // the peer card
        (if (error != null) 1 else 0) +
        (if (thread.isEmpty() && !loading && loadedOnce) 1 else 0)

    // Keep the newest line in view — on open, after every send, and when the
    // other side starts typing under us.
    LaunchedEffect(thread.size, peerTyping, headerRows) {
        if (thread.isEmpty()) return@LaunchedEffect
        val newest = headerRows + thread.lastIndex + if (peerTyping) 1 else 0
        listState.animateScrollToItem(newest)
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

    // "Seen" belongs on the newest of MY messages the other person actually
    // READ — not simply on my newest one. Stamping the latest line whatever
    // the receipt said is how a thread ends up claiming a message was seen
    // when it never was; a message with no receipt shows no receipt.
    val seenIndex = thread.indexOfLast { it.mine && it.readAtMillis != null }

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
                // ONE inset consumer for the whole screen, and it is the
                // UNION rather than a sum: the bottom inset is
                // max(navigation bar, keyboard). Chaining
                // `.windowInsetsPadding(navigationBars).imePadding()` CONSUMES
                // the bar but does not shrink the IME inset — the keyboard's
                // inset already spans the bar — so the composer ended up a
                // bar's height ABOVE the keyboard with an empty strip under
                // it. The union says exactly what the layout means.
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.ime)
                )
        ) {
            if (eligible && token != null && myUserId != null) {
                val activeToken = token
                val activeUserId = myUserId

                fun openReactions(messageId: String) {
                    reactionTarget = if (reactionTarget == messageId) null else messageId
                }

                fun pickReaction(messageId: String, kind: String) {
                    val mine = reactions[messageId]?.firstOrNull { it.userId == activeUserId }
                    reactionTarget = null
                    scope.launch {
                        val result = if (mine?.kind == kind) {
                            SocialApi.clearReaction(activeToken, messageId)
                        } else {
                            SocialApi.react(activeToken, messageId, kind)
                        }
                        result.fold(
                            onSuccess = {
                                SocialApi.reactions(activeToken, listOf(messageId))
                                    .onSuccess { fresh ->
                                        reactions = reactions - messageId + fresh
                                    }
                            },
                            onFailure = { error = it.message }
                        )
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
                                subtitle = "Private messages",
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    item(key = "peer") {
                        MessagePeerHeader(
                            person = person,
                            fallback = fallback,
                            typing = peerTyping,
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

                    itemsIndexedWithDays(thread) { index, message, dayLabel, firstOfRun, lastOfRun ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (dayLabel != null) SocialDayDivider(dayLabel)
                            MessageEntry(
                                message = message,
                                firstOfRun = firstOfRun,
                                lastOfRun = lastOfRun,
                                // Only the newest of MY messages can be seen:
                                // an older receipt would be a lie if a newer
                                // message was still unread.
                                receipt = if (index == seenIndex) message.readAtMillis else null,
                                accent = reactionTarget == message.id,
                                reactions = reactions[message.id].orEmpty(),
                                myUserId = activeUserId,
                                onTap = { openReactions(message.id) },
                                onPick = { kind -> pickReaction(message.id, kind) },
                                animateIn = message.id.startsWith(LOCAL_ID_PREFIX)
                            )
                        }
                    }

                    if (peerTyping && thread.isNotEmpty()) {
                        item(key = "typing") { TypingBubble(person) }
                    }
                }

                MessageComposer(
                    draft = draft,
                    title = fallback,
                    sending = sending,
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
                                        subtitle = "Settings → Online mode",
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
                                        subtitle = "Settings → Online mode",
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
            SettingsHeroHeader(
                title = title,
                subtitle = if (peerTyping) "Typing…" else "Private messages",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop
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
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedWithDays(
    thread: List<CurioDirectMessage>,
    row: @Composable (index: Int, message: CurioDirectMessage, dayLabel: String?, first: Boolean, last: Boolean) -> Unit
) {
    items(thread.size, key = { thread[it].id }) { index ->
        val message = thread[index]
        val previous = thread.getOrNull(index - 1)
        val next = thread.getOrNull(index + 1)
        val newDay = previous == null ||
            socialDayLabel(previous.createdAtMillis) != socialDayLabel(message.createdAtMillis)
        val sameAsPrevious = !newDay && previous != null &&
            previous.mine == message.mine &&
            message.createdAtMillis - previous.createdAtMillis <= 5 * 60 * 1000
        val sameAsNext = next != null &&
            socialDayLabel(next.createdAtMillis) == socialDayLabel(message.createdAtMillis) &&
            next.mine == message.mine &&
            next.createdAtMillis - message.createdAtMillis <= 5 * 60 * 1000
        row(
            index,
            message,
            if (newDay) socialDayLabel(message.createdAtMillis) else null,
            !sameAsPrevious,
            !sameAsNext
        )
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
    onOpenProfile: () -> Unit
) {
    val dark = isCurioDarkTheme()
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
        else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.72f),
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
            SocialAvatar(style = person?.avatarStyle ?: 0, avatarSize = 46.dp)
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
                        text = listOfNotNull(person?.handleLabel, person?.presenceLabel)
                            .joinToString(" · ")
                            .ifBlank { "Open profile" },
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
    accent: Boolean,
    reactions: List<CurioDmReaction>,
    myUserId: String,
    onTap: () -> Unit,
    onPick: (String) -> Unit,
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
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MessageBubble(
                message = message,
                firstOfRun = firstOfRun,
                lastOfRun = lastOfRun,
                receipt = receipt,
                reactions = reactions,
                myUserId = myUserId,
                onTap = onTap
            )
            if (accent) {
                ReactionBar(
                    current = reactions.firstOrNull { it.userId == myUserId }?.kind,
                    mine = message.mine,
                    onPick = onPick
                )
            }
        }
    }
}

/**
 * One message. Yours sits on the right in the accent container, theirs on the
 * left on the raised surface — the reading direction of every messenger, so
 * who said what needs no label. Corners open up on the first line of a run and
 * only the last line of a run gets the full rounding and the timestamp.
 */
@Composable
private fun MessageBubble(
    message: CurioDirectMessage,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
    receipt: Long?,
    reactions: List<CurioDmReaction>,
    myUserId: String,
    onTap: () -> Unit
) {
    val mine = message.mine
    val shape = if (mine) {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = if (firstOfRun) 20.dp else 7.dp,
            bottomStart = if (lastOfRun) 20.dp else 7.dp,
            bottomEnd = 7.dp
        )
    } else {
        RoundedCornerShape(
            topStart = if (firstOfRun) 20.dp else 7.dp,
            topEnd = 20.dp,
            bottomStart = 7.dp,
            bottomEnd = if (lastOfRun) 20.dp else 7.dp
        )
    }

    val mineGlyph = reactions.firstOrNull { it.userId == myUserId }?.kind
    val others = reactions.filterNot { it.userId == myUserId }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (mine && lastOfRun) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 6.dp, bottom = 2.dp)
            ) {
                Text(
                    text = socialStamp(message.createdAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (receipt != null) {
                    Text(
                        text = "Seen ${socialStamp(receipt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = curioDialogActionColor()
                    )
                }
            }
        }

        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(
                        if (mine) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable(onClick = onTap)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (mine) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
            }

            // The reaction chips sit OUTSIDE the bubble, on its tail corner, so
            // the bubble keeps its own shape and a reaction never reflows the
            // text it is attached to.
            if (mineGlyph != null || others.isNotEmpty()) {
                ReactionChips(mineGlyph = mineGlyph, others = others)
            }
        }

        if (!mine && lastOfRun) {
            Text(
                text = socialStamp(message.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
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
 * The palette under a tapped message. Every glyph is a name from Curio's own
 * icon set, so a reaction is a word on the server and never an upload.
 */
@Composable
private fun ReactionBar(
    current: String?,
    mine: Boolean,
    onPick: (String) -> Unit
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(animationSpec = tween(CurioMotion.Durations.Quick)) +
            slideInVertically(initialOffsetY = { it / 3 }),
        exit = ExitTransition.None
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (mine) 0.dp else 2.dp)
        ) {
            if (mine) Spacer(Modifier.weight(1f))
            SocialReactions.PALETTE.forEach { (emoji, meaning) ->
                // The emoji IS the reaction: it is drawn as text, so what the
                // picker shows is exactly what the server stores — and a
                // legacy row's icon name still resolves to its emoji.
                val chosen = current != null && SocialReactions.emojiFor(current) == emoji
                Surface(
                    onClick = { onPick(emoji) },
                    shape = RoundedCornerShape(50),
                    color = if (chosen) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.semantics { contentDescription = meaning }
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        color = if (chosen) curioDialogActionColor()
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                    )
                }
            }
            if (!mine) Spacer(Modifier.weight(1f))
        }
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
        SocialAvatar(style = person?.avatarStyle ?: 0, avatarSize = 26.dp)
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
    draft: String,
    title: String,
    sending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val dark = isCurioDarkTheme()
    val armed = draft.isNotBlank() && !sending
    val sendScale by animateFloatAsState(
        targetValue = if (armed) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "sendScale"
    )
    val nearLimit = draft.length > SocialApi.MAX_MESSAGE_CHARS * 8 / 10

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = wideContentEdgePadding(),
                end = wideContentEdgePadding(),
                bottom = 10.dp
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
                        else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 16.dp)
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(curioDialogActionColor()),
                    // A message is a sentence, not a word: capitals and
                    // sentence punctuation are the default here (the field is
                    // unlabelled, so this is the only cue it needs).
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
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
                        name = CurioIcons.ArrowForward,
                        contentDescription = "Send",
                        tint = if (armed) androidx.compose.ui.graphics.Color.White
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
 * socket that died without saying so is still picked up, but the timer is no
 * longer the thing that makes the thread feel live.
 */
private const val SAFETY_TICK_MS = 20_000L

/** The id prefix of a bubble that is sent but not yet confirmed. */
private const val LOCAL_ID_PREFIX = "local-"
