package com.curio.app.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.SocialApi
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionInfoRow
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
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/**
 * ONE MEMBER'S PUBLIC PROFILE.
 *
 * The social layer's missing destination: a card's author, a reply's author, a
 * friend row and a conversation all lead here, so the app shows PEOPLE rather
 * than handles. Only the public half of the profile is ever read — `profiles`
 * holds display identity, the portrait index and the Online Mode switch, and
 * the server policy only exposes it to signed-in members who kept themselves
 * discoverable (see `supabase/schema.sql` §5b). No email, capture, card or
 * message is reachable from here.
 *
 * Their live cards are the same 24-hour wall: expired cards simply are not in
 * the answer, so this page can never outlive the content it shows.
 */
@Composable
fun SocialProfileScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state
    val wide = windowWidthSizeClass().isWide
    val glassBackdrop = rememberLayerBackdrop()

    var person by remember { mutableStateOf<CurioPerson?>(null) }
    var cards by remember { mutableStateOf<List<CommunityCard>>(emptyList()) }
    var friendRequestId by remember { mutableStateOf<String?>(null) }
    var asked by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val token = account.session?.accessToken
    val myUserId = account.session?.userId
    val isMe = myUserId != null && myUserId == userId

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    suspend fun load() {
        val active = token ?: return
        loading = true
        // The profile first — everything on the page hangs off who this is.
        SocialApi.people(active, listOf(userId)).fold(
            onSuccess = { people ->
                person = people[userId]
                // A missing row means they are not readable (Online Mode off or
                // hidden). That is not an error the user caused, so it is shown
                // as an empty profile rather than a failure.
                if (people[userId] == null) error = null
            },
            onFailure = { error = it.message }
        )
        CommunityApi.cardsByAuthor(active, userId, myUserId).fold(
            onSuccess = { cards = it },
            onFailure = { error = it.message }
        )
        if (!isMe && myUserId != null) {
            // Are we already friends? Decides Message vs Add friend.
            SocialApi.friends(active, myUserId).fold(
                onSuccess = { list ->
                    friendRequestId = list.firstOrNull { it.person.userId == userId }?.requestId
                },
                onFailure = { /* friendship stays unknown; the pill offers the ask */ }
            )
        }
        loading = false
    }

    LaunchedEffect(userId, token) { if (token != null) load() }

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
            modifier = Modifier
                .layerBackdrop(glassBackdrop)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = wideContentEdgePadding(),
                end = wideContentEdgePadding(),
                top = if (wide) 0.dp else SettingsHeroTotalHeight,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (wide) {
                item(key = "hero", contentType = "hero") {
                    SettingsHeroHeader(
                        title = "Profile",
                        subtitle = "A member of the community",
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            if (!account.signedIn || token == null) {
                item {
                    SettingsOptionCard {
                        SettingsOptionInfoRow(
                            CurioIcons.Info,
                            "Sign in to see this profile",
                            "Profiles are visible to signed-in members with Online mode on."
                        )
                    }
                }
                return@LazyColumn
            }

            item(key = "identity") {
                SocialProfileHeader(
                    person = person,
                    isMe = isMe,
                    loading = loading,
                    friendRequestId = friendRequestId,
                    asked = asked,
                    onAsk = {
                        if (myUserId != null) {
                            scope.launch {
                                SocialApi.ask(token, userId, myUserId).fold(
                                    onSuccess = { asked = true },
                                    onFailure = { error = it.message }
                                )
                            }
                        }
                    },
                    onMessage = {
                        navController.navigate(CurioRoutes.directMessage(userId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            error?.let { message ->
                item(key = "error") {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item { SettingsSectionHeading("Cards from the last 24 hours") }

            if (cards.isEmpty() && !loading) {
                item(key = "empty") {
                    SettingsOptionCard {
                        SettingsOptionInfoRow(
                            CurioIcons.Info,
                            "No live cards",
                            "Cards only last a day, and nothing is kept after that."
                        )
                    }
                }
            }

            items(cards, key = { it.id }) { card ->
                // The same card canvas the wall uses: their cards look exactly
                // like they did where you found them.
                CommunityCardCanvas(
                    card = card,
                    modifier = Modifier.clickable {
                        navController.navigate(CurioRoutes.communityCard(card.id)) {
                            launchSingleTop = true
                        }
                    },
                    widthFraction = 0.86f
                )
            }
        }

        if (!wide) {
            SettingsHeroHeader(
                title = "Profile",
                subtitle = "A member of the community",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop
            )
        }
    }
}

/**
 * The identity block — the portrait, the handle, and the one action that fits
 * the relationship: message a friend, ask a stranger, or stand down on your
 * own page. The portrait is the same code-drawn disc used everywhere else, so
 * a member looks identical in the wall, in a reply and here.
 */
@Composable
private fun SocialProfileHeader(
    person: CurioPerson?,
    isMe: Boolean,
    loading: Boolean,
    friendRequestId: String?,
    asked: Boolean,
    onAsk: () -> Unit,
    onMessage: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SocialAvatar(
                    style = person?.avatarStyle ?: 0,
                    avatarSize = 64.dp,
                    onClick = null
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = person?.let { "@${it.handle}" } ?: "@…",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = person?.let { it.label }
                            ?: if (loading) "Loading…" else "This profile isn't visible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            when {
                isMe -> Text(
                    text = "This is your own profile. You can change your name and portrait in Edit profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                friendRequestId != null -> Button(
                    onClick = onMessage,
                    shape = RoundedCornerShape(50),
                    colors = curioDialogActionButtonColors()
                ) {
                    CurioIcon(
                        name = CurioIcons.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 16.dp
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "Message",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                asked -> Text(
                    text = "Friend request sent. They will see it in Friends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Button(
                    onClick = onAsk,
                    shape = RoundedCornerShape(50),
                    colors = curioDialogActionButtonColors()
                ) {
                    CurioIcon(
                        name = CurioIcons.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 16.dp
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "Add friend",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Text(
                text = "A profile shows only your username and portrait. Your email, saved entries, " +
                    "cards and messages are never part of it.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
