package com.curio.app.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.curio.app.ui.theme.curioSheetContainerColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CurioDmThread
import com.curio.app.data.supabase.CurioFriend
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
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioFillInk
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * CHATS — the conversations, and nothing else.
 *
 * Friends and chats are two different questions ("who are my people" vs "what
 * was said"), so they are two screens: the contacts list stays in Friends, and
 * this is the inbox — newest conversation first, one dense row each, with the
 * last line, when it landed and how many are unread.
 *
 * Three things make a row honest:
 *
 *  - **the unread badge** comes from the server's read receipts, never from a
 *    count this screen guessed;
 *  - **the dot** is drawn only from a last-active stamp inside
 *    [com.curio.app.data.supabase.CurioPerson.isActiveNow], so a friend who
 *    hid activity shows no dot rather than a false one;
 *  - **a long press** opens the only two honest ways to get rid of a chat: for
 *    me (my inbox and this device) or for both of us (the conversation itself
 *    is deleted on the server).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state
    val wide = windowWidthSizeClass().isWide
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()

    val onlineMode = AppPreferences.onlineModeEnabledState
    var threads by remember { mutableStateOf<List<CurioDmThread>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // The conversation a long press opened, and which of its two deletions is
    // running — the sheet stays up, disabled, until the call answers.
    var actionsFor by remember { mutableStateOf<CurioDmThread?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pushed by remember { mutableStateOf(0) }
    var cacheHydrated by remember { mutableStateOf(false) }
    // ── NEW CHAT (v385) ─────────────────────────────────────────────────
    // The inbox used to be a dead end for a conversation that does not exist
    // yet: the only door was Friends, two screens away, and an empty inbox
    // said so in prose. The compose button opens the people you can actually
    // message, from here.
    var pickerOpen by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<CurioFriend>>(emptyList()) }
    var friendsLoading by remember { mutableStateOf(false) }
    var friendsError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    val token = account.session?.accessToken
    val myUserId = account.session?.userId
    val eligible = account.signedIn && onlineMode && token != null && myUserId != null

    // Read once per opening, not on every inbox tick: the picker is a decision
    // surface, and a list that reshuffles under the finger is worse than one
    // that was a minute old when it appeared.
    LaunchedEffect(pickerOpen, eligible, token, myUserId) {
        if (!pickerOpen || !eligible || token == null || myUserId == null) return@LaunchedEffect
        friendsLoading = true
        SocialApi.friends(token, myUserId).fold(
            onSuccess = { people ->
                friends = people
                friendsError = null
                // Names and portraits are cached like the inbox's, so the
                // thread they open draws the real identity on its first frame.
                SocialPeopleCache.remember(context, people.map { friend -> friend.person })
            },
            onFailure = { friendsError = it.message }
        )
        friendsLoading = false
    }

    suspend fun load(active: String, me: String) {
        loading = true
        SocialApi.threads(active, me).fold(
            onSuccess = {
                // A transient empty response must not erase the cached people
                // before PostgREST/realtime has finished warming up.
                if (it.isNotEmpty() || !cacheHydrated) {
                    threads = it
                    cacheHydrated = true
                    SocialInboxCache.replaceThreads(context, it)
                }
                error = null
            },
            onFailure = { error = it.message }
        )
        // Every identity this screen resolved is remembered, so opening the
        // thread draws a real name and portrait on the first frame.
        SocialPeopleCache.remember(context, threads.map { it.person })
        loading = false
    }

    /** The quiet refresh: no spinner, no flicker, just new rows where there are. */
    suspend fun refreshQuietly(active: String, me: String) {
        SocialApi.threads(active, me).onSuccess {
            // Never replace a visible inbox with a transient empty response.
            // PostgREST/realtime can briefly return no rows while a screen is
            // being resumed, which made the identity row disappear and return.
            if (it.isNotEmpty() && it != threads) {
                threads = it
                cacheHydrated = true
                SocialPeopleCache.remember(context, it.map { thread -> thread.person })
                SocialInboxCache.replaceThreads(context, it)
            }
        }
    }

    LaunchedEffect(eligible, token, myUserId) {
        if (eligible && token != null && myUserId != null) {
            // The device's copy first: the list is on screen before the
            // network is asked anything. Only an empty list is ever filled
            // from the cache, so a live tick is never clobbered by a stale one.
            if (threads.isEmpty()) {
                SocialInboxCache.read(context)?.let { cached ->
                    if (threads.isEmpty()) threads = cached.threads
                    cacheHydrated = cached.threads.isNotEmpty()
                    SocialPeopleCache.remember(context, cached.threads.map { it.person })
                }
            }
            load(token, myUserId)
        }
    }

    // THE SAFETY NET. Realtime is the mechanism now, so this only runs at the
    // old fast cadence while the push channel is NOT linked.
    LaunchedEffect(eligible, token, myUserId) {
        if (!eligible || token == null || myUserId == null) return@LaunchedEffect
        while (true) {
            delay(if (SupabaseRealtime.isLinked) CHATS_SAFETY_TICK_MS else CHATS_TICK_MS)
            refreshQuietly(token, myUserId)
        }
    }

    // REALTIME — anything addressed to me proves the conversation moved: a new
    // message, an edit, or a deletion (REPLICA IDENTITY FULL delivers the old
    // row, so the last-line preview updates instead of going stale).
    DisposableEffect(eligible, token, myUserId) {
        val active = token
        val me = myUserId
        val owner = "chats"
        if (eligible && active != null && me != null) {
            SupabaseRealtime.watch(
                owner = owner,
                accessToken = active,
                watches = listOf(
                    RealtimeWatch(
                        table = "dm_messages",
                        filter = "recipient=eq.$me",
                        events = listOf("INSERT", "UPDATE", "DELETE")
                    )
                )
            ) {
                scope.launch { pushed++ }
            }
        }
        onDispose { SupabaseRealtime.unwatch(owner) }
    }

    LaunchedEffect(pushed, eligible, token, myUserId) {
        if (!eligible || token == null || myUserId == null || pushed == 0) return@LaunchedEffect
        // A short settle window so a burst of events is ONE refresh.
        delay(250)
        refreshQuietly(token, myUserId)
    }

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

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                val active = token
                val me = myUserId
                if (active != null && me != null) {
                    refreshing = true
                    scope.launch {
                        load(active, me)
                        refreshing = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .layerBackdrop(glassBackdrop)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    top = if (wide) 0.dp else SettingsHeroTotalHeight,
                    // The compose button rides above the last row: the list
                    // keeps its own space so a chat is never under it.
                    bottom = 24.dp +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                        if (eligible) 72.dp else 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (wide) {
                    item(key = "hero", contentType = "hero") {
                        SettingsHeroHeader(
                            title = "Chats",
                            subtitle = "",
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                if (!eligible || token == null || myUserId == null) {
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
                                        "Messages stay off while Online mode is off."
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
                    return@LazyColumn
                }

                notice?.let { message -> item(key = "notice") { SocialNote(message, false) } }
                error?.let { message -> item(key = "error") { SocialNote(message, true) } }

                if (threads.isEmpty() && !loading && error == null) {
                    item(key = "empty") {
                        SocialEmptyCard(
                            icon = CurioIcons.BubbleChart,
                            title = "No conversations yet",
                            body = "Open Friends, pick someone, and say hello — messages are " +
                                "private and the server forgets them after a day."
                        )
                    }
                    item(key = "to-friends") {
                        SettingsOptionCard {
                            SettingsOptionRow(
                                icon = CurioIcons.Friends,
                                title = "Open Friends",
                                subtitle = "Find someone to talk to",
                                onClick = { navController.navigate(CurioRoutes.FRIENDS) }
                            )
                        }
                    }
                } else {
                    items(threads, key = { "chat-${it.person.userId}" }) { thread ->
                        // A long press is the ONLY way to remove a chat, and
                        // that is deliberate: the tap opens the thread, and a
                        // destructive action must never ride on the gesture
                        // people use most.
                        SocialSidebarRow(
                            person = thread.person,
                            subtitle = thread.preview.ifBlank { "No messages yet" },
                            meta = socialStamp(thread.lastAtMillis).takeIf { it.isNotBlank() },
                            badge = thread.unread,
                            onOpen = {
                                navController.navigate(
                                    CurioRoutes.directMessage(
                                        thread.person.userId,
                                        thread.person.label
                                    )
                                )
                            },
                            onLongClick = { actionsFor = thread }
                        )
                    }
                }
            }
        }

        if (!wide) {
            SettingsHeroHeader(
                title = "Chats",
                subtitle = "",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop
            )
        }

        // The compose button: the pocket of the inbox a new conversation
        // starts from. Hidden while the screen is not usable (signed out or
        // Online mode off) — the settings rows in the list own those states.
        if (eligible) {
            Surface(
                onClick = { pickerOpen = true },
                shape = CircleShape,
                color = curioDialogActionColor(),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = wideContentEdgePadding(),
                        bottom = 22.dp +
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
            ) {
                CurioIcon(
                    name = CurioIcons.Edit,
                    contentDescription = "New chat",
                    // v412 — asks the compose disc's fill (see [curioFillInk]):
                    // dark mode's accent is the bright pale primary.
                    tint = curioFillInk(curioDialogActionColor()),
                    size = 22.dp,
                    modifier = Modifier.padding(15.dp)
                )
            }
        }
    }

    actionsFor?.let { thread ->
        val active = token
        val me = myUserId
        ChatActionsSheet(
            thread = thread,
            busy = busy,
            onDismiss = { if (!busy) actionsFor = null },
            onDeleteForMe = {
                if (active != null && me != null) {
                    busy = true
                    scope.launch {
                        SocialApi.hideConversation(active, me, thread.person.userId).fold(
                            onSuccess = {
                                // The device's own copy goes too — a deleted
                                // chat must not open from its stale cache.
                                SocialMessageCache.forget(context, thread.person.userId)
                                threads = threads.filterNot {
                                    it.person.userId == thread.person.userId
                                }
                                SocialInboxCache.replaceThreads(context, threads)
                                actionsFor = null
                                notice = "Chat with ${thread.person.label} deleted for you."
                            },
                            onFailure = { error = it.message }
                        )
                        busy = false
                    }
                }
            },
            onDeleteForBoth = {
                if (active != null) {
                    busy = true
                    scope.launch {
                        SocialApi.deleteConversation(active, thread.person.userId).fold(
                            onSuccess = { removed ->
                                SocialMessageCache.forget(context, thread.person.userId)
                                threads = threads.filterNot {
                                    it.person.userId == thread.person.userId
                                }
                                SocialInboxCache.replaceThreads(context, threads)
                                actionsFor = null
                                notice = "Deleted for both of you" +
                                    if (removed > 0) " — $removed message(s) gone." else "."
                            },
                            onFailure = { error = it.message }
                        )
                        busy = false
                    }
                }
            }
        )
    }

    if (pickerOpen) {
        NewChatSheet(
            friends = friends,
            loading = friendsLoading,
            error = friendsError,
            onDismiss = { pickerOpen = false },
            onPick = { friend ->
                pickerOpen = false
                navController.navigate(
                    CurioRoutes.directMessage(friend.person.userId, friend.person.label)
                )
            },
            onFindSomeone = {
                pickerOpen = false
                navController.navigate(CurioRoutes.FRIENDS)
            }
        )
    }
}

/**
 * NEW CHAT.
 *
 * The people you can message, and nothing else: friends only (the server is
 * what decides who that is, and a direct message needs the friendship), with
 * the same row the inbox and the contacts list use, so a face is a face
 * everywhere in the social surfaces. A name that is not there yet has one
 * door out, into Friends, where a request is sent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatSheet(
    friends: List<CurioFriend>,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onPick: (CurioFriend) -> Unit,
    onFindSomeone: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = curioSheetContainerColor(MaterialTheme.colorScheme.surface),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Text(
                text = "New chat",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (friends.isEmpty()) {
                    item(key = "pick-state") {
                        SettingsOptionCard {
                            when {
                                loading -> SettingsOptionInfoRow(
                                    CurioIcons.Friends,
                                    "Looking up your friends",
                                    "One moment."
                                )
                                error != null -> SettingsOptionInfoRow(
                                    CurioIcons.Warning,
                                    "Couldn't load your friends",
                                    error
                                )
                                else -> SettingsOptionInfoRow(
                                    CurioIcons.Friends,
                                    "No friends yet",
                                    "A direct message needs a friendship first."
                                )
                            }
                        }
                    }
                } else {
                    items(friends, key = { "pick-${it.person.userId}" }) { friend ->
                        SocialSidebarRow(
                            person = friend.person,
                            subtitle = friend.person.handleLabel,
                            onOpen = { onPick(friend) }
                        )
                    }
                }
                item(key = "pick-find") {
                    SettingsOptionCard {
                        SettingsOptionRow(
                            icon = CurioIcons.Search,
                            title = "Find someone new",
                            subtitle = "Send a friend request in Friends",
                            onClick = onFindSomeone
                        )
                    }
                }
            }
        }
    }
}

/**
 * THE TWO DELETIONS.
 *
 * They are different promises and the sheet says so in full sentences: "for
 * me" leaves the other person's history alone and the thread returns if they
 * write again; "for both" deletes the conversation on the server for both
 * accounts and cannot be undone. Nothing is destroyed on the first tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatActionsSheet(
    thread: CurioDmThread,
    busy: Boolean,
    onDismiss: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForBoth: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val name = thread.person.label
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = curioSheetContainerColor(MaterialTheme.colorScheme.surface),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ChatActionRow(
                icon = CurioIcons.VisibilityOff,
                title = "Delete for me",
                body = "Removes this chat from your list and this device. " +
                    "$name keeps their copy, and the chat comes back if they write again.",
                enabled = !busy,
                onClick = onDeleteForMe
            )
            ChatActionRow(
                icon = CurioIcons.Delete,
                title = "Delete for both of us",
                body = "Deletes the whole conversation on the server for both of you. " +
                    "This cannot be undone.",
                destructive = true,
                enabled = !busy,
                onClick = onDeleteForBoth
            )
            TextButton(
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/** One row of [ChatActionsSheet] — a glyph, a name, and the whole promise. */
@Composable
private fun ChatActionRow(
    icon: String,
    title: String,
    body: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val ink = if (destructive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
            alpha = if (enabled) 0.7f else 0.35f
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            CurioIcon(
                name = icon,
                contentDescription = null,
                tint = ink,
                size = 20.dp
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ink
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** The open-inbox cadence used only while realtime is NOT linked. */
private const val CHATS_TICK_MS = 5_000L

/** The safety-net cadence once realtime is linked. */
private const val CHATS_SAFETY_TICK_MS = 30_000L
