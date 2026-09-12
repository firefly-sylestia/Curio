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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
import com.curio.app.features.settings.SettingsNavRail
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionDivider
import com.curio.app.features.settings.SettingsOptionInfoRow
import com.curio.app.features.settings.SettingsOptionRow
import com.curio.app.features.settings.SettingsSectionHeading
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.navigateToSettingsSection
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    val token = account.session?.accessToken
    val myUserId = account.session?.userId
    val eligible = account.signedIn && onlineMode && token != null && myUserId != null

    suspend fun load(active: String, me: String) {
        loading = true
        // Independent calls: a failure in one must not blank the other list.
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
                                    "Friends are tied to a Curio account."
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

            notice?.let { message -> item { SocialNote(message, false) } }
            error?.let { message -> item { SocialNote(message, true) } }

            // ── find someone ────────────────────────────────────────────
            item { SettingsSectionHeading("Find people") }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Search by name or @username") },
                    supportingText = {
                        Text(
                            if (searching) "Searching…"
                            else "Only accounts with Online mode on can be found."
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (results.isNotEmpty()) {
                item {
                    SettingsOptionCard {
                        results.forEachIndexed { index, person ->
                            if (index > 0) SettingsOptionDivider()
                            PersonRow(
                                person = person,
                                action = "Add",
                                onAction = {
                                    scope.launch {
                                        SocialApi.ask(activeToken, person.userId, activeUserId).fold(
                                            onSuccess = {
                                                notice = "Request sent to ${person.label}."
                                                results = results.filterNot {
                                                    it.userId == person.userId
                                                }
                                                load(activeToken, activeUserId)
                                            },
                                            onFailure = { error = it.message }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ── requests to answer ──────────────────────────────────────
            val incoming = requests.filter { it.incoming }
            if (incoming.isNotEmpty()) {
                item { SettingsSectionHeading("Waiting on you") }
                item {
                    SettingsOptionCard {
                        incoming.forEachIndexed { index, request ->
                            if (index > 0) SettingsOptionDivider()
                            RequestRow(
                                request = request,
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
                }
            }

            val outgoing = requests.filterNot { it.incoming }
            if (outgoing.isNotEmpty()) {
                item { SettingsSectionHeading("You asked") }
                item {
                    SettingsOptionCard {
                        outgoing.forEachIndexed { index, request ->
                            if (index > 0) SettingsOptionDivider()
                            PersonRow(
                                person = request.person,
                                action = "Cancel",
                                onAction = {
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
                }
            }

            // ── conversations already open ───────────────────────────────
            if (threads.isNotEmpty()) {
                item { SettingsSectionHeading("Messages") }
                item {
                    SettingsOptionCard {
                        threads.forEachIndexed { index, thread ->
                            if (index > 0) SettingsOptionDivider()
                            ThreadRow(
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
                }
            }

            // ── friends ─────────────────────────────────────────────────
            item { SettingsSectionHeading("Friends") }
            item {
                if (friends.isEmpty() && !loading) {
                    SettingsOptionCard {
                        SettingsOptionInfoRow(
                            CurioIcons.Hub,
                            "No friends yet",
                            "Search for someone above — adding them sends a request."
                        )
                    }
                } else {
                    SettingsOptionCard {
                        friends.forEachIndexed { index, friend ->
                            if (index > 0) SettingsOptionDivider()
                            PersonRow(
                                person = friend.person,
                                action = "Message",
                                onAction = {
                                    navController.navigate(
                                        CurioRoutes.directMessage(
                                            friend.person.userId,
                                            friend.person.label
                                        )
                                    )
                                },
                                secondaryAction = "Remove",
                                onSecondaryAction = {
                                    scope.launch {
                                        SocialApi.remove(activeToken, friend.requestId).fold(
                                            onSuccess = {
                                                notice = "Removed ${friend.person.label}."
                                                load(activeToken, activeUserId)
                                            },
                                            onFailure = { error = it.message }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
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
 * `14:32` for today, `Aug 12` for anything older.
 *
 * One short line by design: an inbox row and a message bubble both need a
 * timestamp that never wraps or pushes the content around.
 */
internal fun socialStamp(millis: Long): String {
    if (millis <= 0L) return ""
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(millis).atZone(zone)
    return if (time.toLocalDate() == LocalDate.now(zone)) {
        time.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        time.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}

/** One line of feedback under a section — [isError] picks the ink. */
@Composable
internal fun SocialNote(message: String, isError: Boolean) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * The person disc every social row leads with: first letter on the theme's
 * muted container. No avatars are uploaded anywhere in the online layer, so a
 * letter is the honest identity mark rather than a placeholder photo.
 */
@Composable
internal fun PersonBadge(name: String, size: androidx.compose.ui.unit.Dp = 38.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * One person as a settings-style row: disc, name, and the row's action.
 * [secondaryAction] is the destructive one and renders FIRST so it never sits
 * under the thumb that just tapped the primary.
 */
@Composable
private fun PersonRow(
    person: CurioPerson,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        PersonBadge(person.label)
        Spacer(Modifier.width(12.dp))
        Text(
            text = person.identityLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (secondaryAction != null && onSecondaryAction != null) {
            TextButton(onClick = onSecondaryAction) {
                Text(
                    text = secondaryAction,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onAction) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = curioDialogActionColor()
            )
        }
    }
}

/**
 * One conversation in the inbox: who, the last line, when, and an unread
 * badge when there is something new.
 */
@Composable
private fun ThreadRow(
    thread: CurioDmThread,
    onOpen: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        PersonBadge(thread.person.label)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = thread.person.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = thread.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (thread.lastAtMillis > 0L) {
                Text(
                    text = socialStamp(thread.lastAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (thread.unread > 0) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = thread.unread.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/** An incoming request: who it is, then Accept / Decline. */
@Composable
private fun RequestRow(
    request: CurioFriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            PersonBadge(request.person.label)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.person.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Wants to be friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            TextButton(onClick = onDecline) {
                Text(
                    text = "Decline",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onAccept,
                shape = RoundedCornerShape(50),
                colors = curioDialogActionButtonColors()
            ) {
                Text("Accept", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
