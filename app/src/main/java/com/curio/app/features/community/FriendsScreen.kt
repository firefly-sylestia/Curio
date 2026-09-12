package com.curio.app.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CurioDmThread
import com.curio.app.data.supabase.CurioFriend
import com.curio.app.data.supabase.CurioFriendRequest
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.SocialApi
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
import com.curio.app.ui.theme.CurioIcons
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * FRIENDS — the social half of the community: find people, ask them, answer
 * them, and open a private conversation.
 *
 * Gated exactly like the wall is — signed in with Online Mode on — and the
 * server agrees: discovery, asking and messaging are all enforced by RLS, so
 * an offline client or a hand-modified one gets the same nothing back.
 *
 * Text only, like the rest of the online layer: no field anywhere in the
 * schema could carry a photo or a recording.
 *
 * The screen is written as four questions the app can actually answer, in the
 * order a person asks them:
 *
 *  1. **Who is this?** — every face is a [SocialPersonCard]: portrait, live
 *     name, @username, and the pills for where the relationship stands.
 *  2. **What do they want from me?** — incoming requests come FIRST, because a
 *     request that is waiting is the only thing here with a deadline.
 *  3. **What did they say?** — open conversations are [SocialThreadCard]s with
 *     the last line and an unread count.
 *  4. **Who are my people?** — the friends list last, as the calm baseline.
 */
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
    var threads by remember { mutableStateOf<List<CurioDmThread>>(emptyList()) }
    var requests by remember { mutableStateOf<List<CurioFriendRequest>>(emptyList()) }
    var results by remember { mutableStateOf<List<CurioPerson>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // Removal is the one destructive move here, so it asks first — naming the
    // person it is about to remove.
    var removing by remember { mutableStateOf<CurioFriend?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    val token = account.session?.accessToken
    val myUserId = account.session?.userId
    val eligible = account.signedIn && onlineMode && token != null && myUserId != null

    suspend fun load(active: String, me: String) {
        loading = true
        // Independent calls: a failure in one must not blank the other lists.
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
        SocialApi.threads(active, me).fold(
            onSuccess = { threads = it },
            onFailure = { error = it.message }
        )
        // Every identity this screen just resolved is REMEMBERED, so opening a
        // conversation from here can draw the real name and portrait on the
        // first frame instead of waiting for the network to answer again.
        SocialPeopleCache.remember(
            context,
            friends.map { it.person } +
                requests.map { it.person } +
                threads.map { it.person }
        )
        loading = false
    }

    LaunchedEffect(eligible, token, myUserId) {
        if (eligible && token != null && myUserId != null) {
            load(token, myUserId)
        } else {
            friends = emptyList()
            requests = emptyList()
            threads = emptyList()
        }
    }

    // THE INBOX KEEPS UP WHILE YOU WATCH — the list used to be a snapshot from
    // the moment it composed, so a message (or a request) that arrived while
    // it was open only appeared after leaving the screen and coming back.
    // This asks for the same three lists on a slow tick and swaps them in
    // WITHOUT touching the loading state: no spinner, no flicker, just a new
    // line and a fresh unread badge where there is one.
    LaunchedEffect(eligible, token, myUserId) {
        if (!eligible || token == null || myUserId == null) return@LaunchedEffect
        while (true) {
            delay(INBOX_TICK_MS)
            SocialApi.threads(token, myUserId).onSuccess { if (it != threads) threads = it }
            SocialApi.requests(token, myUserId).onSuccess { if (it != requests) requests = it }
        }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (wide) {
                item(key = "hero", contentType = "hero") {
                    SettingsHeroHeader(
                        title = "Friends",
                        subtitle = "Requests and messages",
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

            // From here on both are non-null (checked above), so the callbacks
            // below capture plain Strings instead of re-testing — a null check
            // inside every one-click lambda is how stale-state bugs hide.
            val activeToken = token
            val activeUserId = myUserId

            notice?.let { message -> item(key = "notice") { SocialNote(message, false) } }
            error?.let { message -> item(key = "error") { SocialNote(message, true) } }

            // ── 1. waiting on you ────────────────────────────────────────
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

            // ── 2. what did they say ─────────────────────────────────────
            if (threads.isNotEmpty()) {
                item { SettingsSectionHeading("Messages") }
                items(threads, key = { "dm-${it.person.userId}" }) { thread ->
                    SocialThreadCard(
                        thread = thread,
                        onOpen = {
                            navController.navigate(
                                CurioRoutes.directMessage(
                                    thread.person.userId,
                                    thread.person.label
                                )
                            )
                        }
                    )
                }
            }

            // ── 3. find someone ──────────────────────────────────────────
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

            // ── 4. you asked ─────────────────────────────────────────────
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

            // ── 5. your people ───────────────────────────────────────────
            item { SettingsSectionHeading("Friends") }
            if (friends.isEmpty() && !loading) {
                item(key = "no-friends") {
                    SocialEmptyCard(
                        icon = CurioIcons.Hub,
                        title = "No friends yet",
                        body = "Search a name above. Adding someone sends them a request they can accept."
                    )
                }
            } else {
                items(friends, key = { "friend-${it.requestId}" }) { friend ->
                    SocialPersonCard(
                        person = friend.person,
                        relation = SocialRelation.FRIEND,
                        onOpenProfile = {
                            navController.navigate(CurioRoutes.socialProfile(friend.person.userId)) {
                                launchSingleTop = true
                            }
                        },
                        onAdd = {},
                        onMessage = {
                            navController.navigate(
                                CurioRoutes.directMessage(
                                    friend.person.userId,
                                    friend.person.label
                                )
                            )
                        },
                        onRemove = { removing = friend }
                    )
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
                subtitle = "Requests and messages",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop
            )
        }
    }
}

/**
 * How often an OPEN inbox asks whether anything moved. Slower than a thread's
 * tick on purpose: the list is a directory, not a conversation, and a reply
 * the user is waiting on lives in the thread they will open anyway.
 */
private const val INBOX_TICK_MS = 12_000L
