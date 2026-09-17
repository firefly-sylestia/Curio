package com.curio.app.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CurioFriend
import com.curio.app.data.supabase.CurioFriendRequest
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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * FRIENDS — the contacts sidebar: find people, ask them, answer them.
 *
 * This screen answers ONE question — "who are my people?" — and the inbox
 * answers the other one ("what was said?") on its own screen ([ChatsScreen]).
 * Splitting them is what lets the list be what a contacts list should be: a
 * dense, alphabetical sidebar you can scan, not a feed of conversations with
 * the friends buried under them.
 *
 * The order is the order a person asks the questions:
 *
 *  1. **Is anything waiting on me?** — incoming requests first, because a
 *     request is the only thing here with a deadline.
 *  2. **Who am I looking for?** — search, which also answers "add someone new".
 *  3. **Who did I ask?** — outgoing requests, so a pending ask is visible.
 *  4. **My people** — the A–Z sidebar, filed under a letter.
 *
 * Tapping a row opens the CHAT with that person (the thing you want from a
 * contact), and a long press offers the one destructive action: removing them.
 * Everything is enforced by RLS — discovery, asking and messaging all need
 * Online Mode on both sides.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state
    val wide = windowWidthSizeClass().isWide
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()

    val onlineMode = AppPreferences.onlineModeEnabledState
    var friends by remember { mutableStateOf<List<CurioFriend>>(emptyList()) }
    var requests by remember { mutableStateOf<List<CurioFriendRequest>>(emptyList()) }
    var results by remember { mutableStateOf<List<CurioPerson>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // Removal is the one destructive move here, so it asks first — naming the
    // person it is about to remove.
    var removing by remember { mutableStateOf<CurioFriend?>(null) }
    var busy by remember { mutableStateOf(false) }
    // A server push bumps this: a request that arrived (or was answered) while
    // the list is open appears immediately instead of on the next tick.
    var pushed by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    val token = account.session?.accessToken
    val myUserId = account.session?.userId
    val eligible = account.signedIn && onlineMode && token != null && myUserId != null

    suspend fun load(active: String, me: String) {
        loading = true
        // Independent calls: a failure in one must not blank the others.
        SocialApi.friends(active, me).fold(
            onSuccess = {
                friends = it
                error = null
            },
            onFailure = { error = it.message }
        )
        SocialApi.requests(active, me).fold(
            onSuccess = { requests = it },
            onFailure = { error = it.message }
        )
        // Every identity this screen resolved is REMEMBERED, so opening a
        // conversation (or a profile) draws the real name and portrait on the
        // first frame instead of waiting for the network to answer again.
        SocialPeopleCache.remember(
            context,
            friends.map { it.person } + requests.map { it.person }
        )
        // Conversations are NOT this screen's to write — [SocialInboxCache]
        // keeps one snapshot for both screens and the chats screen owns the
        // thread half of it.
        SocialInboxCache.replaceContacts(context, requests, friends)
        loading = false
    }

    suspend fun refreshQuietly(active: String, me: String) {
        SocialApi.requests(active, me).onSuccess { if (it != requests) requests = it }
        SocialInboxCache.replaceContacts(context, requests, friends)
    }

    LaunchedEffect(eligible, token, myUserId) {
        if (eligible && token != null && myUserId != null) {
            // The device's copy FIRST — the list and the requests are on screen
            // before the network is asked anything. The empty guards keep a
            // re-entry from clobbering state the live tick already refreshed.
            SocialInboxCache.read(context)?.let { cached ->
                if (friends.isEmpty()) friends = cached.friends
                if (requests.isEmpty()) requests = cached.requests
                SocialPeopleCache.remember(
                    context,
                    cached.friends.map { it.person } + cached.requests.map { it.person }
                )
            }
            load(token, myUserId)
        } else {
            friends = emptyList()
            requests = emptyList()
        }
    }

    // THE SAFETY NET. Realtime is the mechanism now; this only runs at the old
    // fast cadence while the push channel is NOT linked.
    LaunchedEffect(eligible, token, myUserId) {
        if (!eligible || token == null || myUserId == null) return@LaunchedEffect
        while (true) {
            delay(if (SupabaseRealtime.isLinked) FRIENDS_SAFETY_TICK_MS else FRIENDS_TICK_MS)
            refreshQuietly(token, myUserId)
        }
    }

    // REALTIME — a request waiting on me, and my own request being answered
    // (or cancelled). Both directions, because an accepted request has to
    // become a friend row without a reload.
    DisposableEffect(eligible, token, myUserId) {
        val active = token
        val me = myUserId
        val owner = "friends"
        if (eligible && active != null && me != null) {
            SupabaseRealtime.watch(
                owner = owner,
                accessToken = active,
                watches = listOf(
                    RealtimeWatch(
                        table = "friend_requests",
                        filter = "addressee=eq.$me",
                        events = listOf("INSERT", "UPDATE")
                    ),
                    RealtimeWatch(
                        table = "friend_requests",
                        filter = "requester=eq.$me",
                        events = listOf("UPDATE", "DELETE")
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
        delay(300)
        SocialApi.friends(token, myUserId).onSuccess { friends = it }
        refreshQuietly(token, myUserId)
    }

    // Search runs off the typed text; [SocialApi.searchPeople] ignores anything
    // shorter than two characters, so this needs no debounce to stay quiet.
    LaunchedEffect(query, eligible, token, myUserId) {
        if (!eligible || token == null || myUserId == null || query.trim().length < 2) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        SocialApi.searchPeople(token, query, myUserId).fold(
            onSuccess = {
                results = it
                error = null
            },
            onFailure = { error = it.message }
        )
        searching = false
    }

    // ── WHERE EACH RELATIONSHIP STANDS ──────────────────────────────────
    // One derived answer, read by every list. This is what stops "Add friend"
    // from appearing on someone who is already a friend, has already been
    // asked, or has already asked you — the bug that made an obviously wrong
    // tap possible.
    val friendIds = remember(friends) { friends.map { it.person.userId }.toSet() }
    val incomingIds = remember(requests) {
        requests.filter { it.incoming }.map { it.person.userId }.toSet()
    }
    val outgoingIds = remember(requests) {
        requests.filterNot { it.incoming }.map { it.person.userId }.toSet()
    }
    val requestIdOf = remember(requests, friends) {
        buildMap<String, String> {
            friends.forEach { put(it.person.userId, it.requestId) }
            requests.forEach { put(it.person.userId, it.id) }
        }
    }

    fun relationOf(userId: String): SocialRelation = when {
        myUserId != null && userId == myUserId -> SocialRelation.SELF
        userId in friendIds -> SocialRelation.FRIEND
        userId in incomingIds -> SocialRelation.INCOMING
        userId in outgoingIds -> SocialRelation.OUTGOING
        else -> SocialRelation.NONE
    }

    // ── THE SIDEBAR SECTIONS ────────────────────────────────────────────
    // Built ONCE per friends list, sorted by the name as it is DISPLAYED (so
    // the letters match what the eye reads), and keyed by the same first letter
    // the header shows.
    val sections = remember(friends) {
        friends
            .sortedBy { it.person.label.lowercase() }
            .groupBy { socialLetterOf(it.person.label) }
            .toSortedMap()
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
                    bottom = 24.dp +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (wide) {
                    item(key = "hero", contentType = "hero") {
                        SettingsHeroHeader(
                            title = "Friends",
                            subtitle = "Your people",
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
                                    "This build has no Curio online project, so there is nobody to add."
                                )
                                !account.signedIn -> {
                                    SettingsOptionInfoRow(
                                        CurioIcons.Info,
                                        "Sign in to add friends",
                                        "Friends are tied to a Curio account — profile, username and all."
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
                                        "Friends and messages stay off while Online mode is off."
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
                    return@LazyColumn
                }

                // From here on both are non-null (checked above), so the
                // callbacks below capture plain Strings instead of re-testing —
                // a null check inside every one-click lambda is how stale-state
                // bugs hide.
                val activeToken = token
                val activeUserId = myUserId

                // The inbox lives on its own screen; this is the one line that
                // says so, so a person on Friends is never stuck without a way
                // to reach what was said.
                item(key = "to-chats") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp)
                    ) {
                        TextButton(onClick = { navController.navigate(CurioRoutes.CHATS) }) {
                            CurioIcon(
                                name = CurioIcons.BubbleChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 16.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Chats")
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }

                notice?.let { message -> item(key = "notice") { SocialNote(message, false) } }
                error?.let { message -> item(key = "error") { SocialNote(message, true) } }

                // ── 1. waiting on you ────────────────────────────────────
                val incoming = requests.filter { it.incoming }
                if (incoming.isNotEmpty()) {
                    item { SettingsSectionHeading("Waiting on you") }
                    items(incoming, key = { "in-${it.id}" }) { request ->
                        SocialPersonCard(
                            person = request.person,
                            relation = SocialRelation.INCOMING,
                            onOpenProfile = {
                                navController.navigate(CurioRoutes.socialProfile(request.person.userId)) {
                                    launchSingleTop = true
                                }
                            },
                            onAdd = {},
                            onMessage = {},
                            onRemove = {},
                            onAccept = {
                                scope.launch {
                                    SocialApi.respond(activeToken, request.id, true).fold(
                                        onSuccess = {
                                            notice = "You and ${request.person.label} are friends."
                                            load(activeToken, activeUserId)
                                        },
                                        onFailure = { error = it.message }
                                    )
                                }
                            },
                            onDecline = {
                                scope.launch {
                                    SocialApi.respond(activeToken, request.id, false).fold(
                                        onSuccess = { load(activeToken, activeUserId) },
                                        onFailure = { error = it.message }
                                    )
                                }
                            }
                        )
                    }
                }

                // ── 2. find someone ──────────────────────────────────────
                item { SettingsSectionHeading("Find people") }
                item(key = "search") {
                    SocialSearchField(
                        value = query,
                        placeholder = "Search a name or @username",
                        busy = searching,
                        onValueChange = { query = it }
                    )
                }
                if (results.isNotEmpty()) {
                    items(results, key = { "hit-${it.userId}" }) { person ->
                        SocialPersonCard(
                            person = person,
                            relation = relationOf(person.userId),
                            onOpenProfile = {
                                navController.navigate(CurioRoutes.socialProfile(person.userId)) {
                                    launchSingleTop = true
                                }
                            },
                            onAdd = {
                                scope.launch {
                                    SocialApi.ask(activeToken, person.userId, activeUserId).fold(
                                        onSuccess = {
                                            notice = "Request sent to ${person.label}."
                                            load(activeToken, activeUserId)
                                        },
                                        onFailure = { error = it.message }
                                    )
                                }
                            },
                            onMessage = {
                                navController.navigate(
                                    CurioRoutes.directMessage(person.userId, person.label)
                                )
                            },
                            onRemove = {
                                requestIdOf[person.userId]?.let { id ->
                                    scope.launch {
                                        SocialApi.remove(activeToken, id).fold(
                                            onSuccess = { load(activeToken, activeUserId) },
                                            onFailure = { error = it.message }
                                        )
                                    }
                                }
                            }
                        )
                    }
                } else if (query.trim().length >= 2 && !searching) {
                    item(key = "no-hits") {
                        SocialEmptyCard(
                            icon = CurioIcons.SearchOff,
                            title = "Nobody matched",
                            body = "Only accounts with Online mode on can be found. Try their @username."
                        )
                    }
                }

                // ── 3. you asked ─────────────────────────────────────────
                val outgoing = requests.filterNot { it.incoming }
                if (outgoing.isNotEmpty()) {
                    item { SettingsSectionHeading("You asked") }
                    items(outgoing, key = { "out-${it.id}" }) { request ->
                        SocialPersonCard(
                            person = request.person,
                            relation = SocialRelation.OUTGOING,
                            onOpenProfile = {
                                navController.navigate(CurioRoutes.socialProfile(request.person.userId)) {
                                    launchSingleTop = true
                                }
                            },
                            onAdd = {},
                            onMessage = {},
                            onRemove = {
                                scope.launch {
                                    SocialApi.remove(activeToken, request.id).fold(
                                        onSuccess = { load(activeToken, activeUserId) },
                                        onFailure = { error = it.message }
                                    )
                                }
                            }
                        )
                    }
                }

                // ── 4. your people, A–Z ──────────────────────────────────
                item { SettingsSectionHeading("Friends") }
                if (friends.isEmpty() && !loading) {
                    item(key = "no-friends") {
                        SocialEmptyCard(
                            icon = CurioIcons.Friends,
                            title = "No friends yet",
                            body = "Search a name above. Adding someone sends them a request " +
                                "they can accept."
                        )
                    }
                } else {
                    sections.forEach { (letter, people) ->
                        item(key = "letter-$letter") { SocialLetterHeader(letter) }
                        items(people, key = { "friend-${it.requestId}" }) { friend ->
                            SocialSidebarRow(
                                person = friend.person,
                                subtitle = friend.person.handleLabel,
                                onOpen = {
                                    navController.navigate(
                                        CurioRoutes.directMessage(
                                            friend.person.userId,
                                            friend.person.label
                                        )
                                    )
                                },
                                onLongClick = { removing = friend }
                            )
                        }
                    }
                }

                // ── 5. start a new chat ──────────────────────────────────
                // A–Z is the right order to LOOK someone up in and the wrong
                // one to START talking in: the person a new chat is most likely
                // to be with is the one just added. So the bottom of the screen
                // offers exactly those, newest friendship first (the server's
                // own order), and a tap opens the conversation.
                if (friends.isNotEmpty()) {
                    item { SettingsSectionHeading("Start a chat") }
                    item(key = "start-chat") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                friends.take(RECENT_FRIEND_LIMIT),
                                key = { "recent-${it.requestId}" }
                            ) { friend ->
                                RecentFriendTile(
                                    person = friend.person,
                                    onClick = {
                                        navController.navigate(
                                            CurioRoutes.directMessage(
                                                friend.person.userId,
                                                friend.person.label
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        removing?.let { friend ->
            // This dialog sits OUTSIDE the list, where the session the list
            // smart-casts above is only nullable — resolve it once here so
            // every callback below captures a plain String.
            val activeToken = token ?: return@let
            val activeUserId = myUserId ?: return@let
            SocialConfirmDialog(
                title = "Remove ${friend.person.label}?",
                body = "They stop being a friend and neither of you can message the other " +
                    "until you add each other again. Your conversation stays on this " +
                    "device.",
                confirmLabel = "Remove",
                busy = busy,
                onDismiss = { if (!busy) removing = null },
                onConfirm = {
                    busy = true
                    scope.launch {
                        SocialApi.remove(activeToken, friend.requestId).fold(
                            onSuccess = {
                                notice = "Removed ${friend.person.label}."
                                removing = null
                                load(activeToken, activeUserId)
                            },
                            onFailure = { error = it.message }
                        )
                        busy = false
                    }
                }
            )
        }

        if (!wide) {
            SettingsHeroHeader(
                title = "Friends",
                subtitle = "Your people",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop
            )
        }
    }
}

/** How many recently added friends the "Start a chat" strip offers. */
private const val RECENT_FRIEND_LIMIT = 12

/**
 * One face in the "Start a chat" strip: the portrait with the name under it,
 * tap either to open the conversation. Deliberately bare — this is a launch
 * pad, not another list.
 */
@Composable
private fun RecentFriendTile(person: CurioPerson, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(66.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        SocialAvatar(style = person.avatarStyle, avatarSize = 44.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = person.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** The open-contacts cadence used only while realtime is NOT linked. */
private const val FRIENDS_TICK_MS = 5_000L

/** The safety-net cadence once realtime is linked. */
private const val FRIENDS_SAFETY_TICK_MS = 30_000L
