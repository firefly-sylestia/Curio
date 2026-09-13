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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.KIND_CARD
import com.curio.app.data.supabase.KIND_QUOTE
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
import com.curio.app.ui.components.CurioDropdownItem
import com.curio.app.ui.components.CurioDropdownMenu
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.curioPressClickable
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/**
 * ONE MEMBER'S PUBLIC PROFILE.
 *
 * The social layer's destination for a person: a card's author, a reply's
 * author, a friend row and a conversation header all lead here, so the app
 * shows PEOPLE rather than handles. Only the public half of the profile is ever
 * read — `profiles` holds display identity, the portrait index and the Online
 * Mode switch, and the server policy exposes it only to signed-in members who
 * kept themselves discoverable (see `supabase/schema.sql` §5b). No email,
 * capture, card or message is reachable from here.
 *
 * The page reads like a person's page rather than a list of rows:
 *
 *  - **The identity block** — a big portrait, the display name, the @handle,
 *    and three honest counts (posts, likes received, replies received) drawn
 *    from the SAME cards the grid below shows.
 *  - **The bio**, when they wrote one, under the counts — and nothing at all
 *    when they did not, because a profile must never pad itself.
 *  - **One action that fits the relationship**: Message for a friend, Add
 *    friend for a stranger, a plain note on your own page. Blocking lives
 *    behind the ⋮, where a profile's rare destructive move belongs.
 *  - **The grid** — a two- (three on a wide window) column wall of small post
 *    previews. A topic card keeps its own art; a note or a quote is the words
 *    it is. Tapping one opens the post.
 *
 * Their live cards are the same 24-hour wall: expired cards are simply not in
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
    var confirmBlock by remember { mutableStateOf(false) }
    var blocking by remember { mutableStateOf(false) }

    val token = account.session?.accessToken
    val myUserId = account.session?.userId
    val isMe = myUserId != null && myUserId == userId

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    suspend fun load() {
        val active = token ?: return
        loading = true
        // The profile first — everything on the page hangs off who this is.
        // `profile` (not `people`) also carries the privacy columns, so a
        // friends-only profile is explained the same way by every door into it.
        SocialApi.profile(active, userId).fold(
            onSuccess = { found ->
                person = found
                // A missing row means they are not readable (Online Mode off,
                // hidden, blocked or friends-only). That is not an error the
                // user caused, so it is shown as an empty profile.
                if (found == null) error = null
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

        LazyVerticalGrid(
            // Two columns of small previews on a phone; a wide window fits a
            // third without the previews turning into full cards again.
            columns = if (wide) GridCells.Fixed(3) else GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = wideContentEdgePadding(),
                end = wideContentEdgePadding(),
                top = if (wide) 0.dp else SettingsHeroTotalHeight,
                bottom = 28.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .layerBackdrop(glassBackdrop)
                .fillMaxSize()
        ) {
            if (wide) {
                item(key = "hero", span = { GridItemSpan(maxLineSpan) }, contentType = "hero") {
                    SettingsHeroHeader(
                        title = "Profile",
                        subtitle = "A member of the community",
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            if (!account.signedIn || token == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SettingsOptionCard {
                        when {
                            !OnlineAccount.configured -> SettingsOptionInfoRow(
                                CurioIcons.Warning,
                                "Not set up in this build",
                                "This build has no Curio online project, so there are no profiles to read."
                            )
                            else -> {
                                SettingsOptionInfoRow(
                                    CurioIcons.Info,
                                    "Sign in to see this profile",
                                    "Profiles are visible to signed-in members with Online mode on."
                                )
                                SettingsOptionRow(
                                    icon = CurioIcons.Person,
                                    title = "Sign in or create an account",
                                    subtitle = "Settings → Online mode",
                                    onClick = { navController.navigate(CurioRoutes.SETTINGS_ONLINE) }
                                )
                            }
                        }
                    }
                }
                return@LazyVerticalGrid
            }

            item(key = "identity", span = { GridItemSpan(maxLineSpan) }, contentType = "identity") {
                SocialProfileHeader(
                    person = person,
                    isMe = isMe,
                    loading = loading,
                    cards = cards,
                    friendRequestId = friendRequestId,
                    asked = asked,
                    onBlock = { confirmBlock = true },
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
                item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item(key = "posts-heading", span = { GridItemSpan(maxLineSpan) }) {
                SettingsSectionHeading("Posts")
            }

            if (cards.isEmpty() && !loading) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    SettingsOptionCard {
                        SettingsOptionInfoRow(
                            CurioIcons.Info,
                            "No live posts",
                            "Topic cards, notes and quotes only last a day."
                        )
                    }
                }
            }

            items(cards, key = { it.id }) { card ->
                SocialProfileTile(
                    card = card,
                    onClick = {
                        navController.navigate(CurioRoutes.communityCard(card.id)) {
                            launchSingleTop = true
                        }
                    }
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

    if (confirmBlock) {
        SocialConfirmDialog(
            title = "Block ${person?.label ?: "this member"}?",
            body = "Neither of you can message, send requests or see each other's cards, and " +
                "their profile closes to you. You can lift it any time in Settings → Privacy.",
            confirmLabel = "Block",
            busy = blocking,
            onDismiss = { if (!blocking) confirmBlock = false },
            onConfirm = {
                val active = token
                if (active == null) {
                    confirmBlock = false
                    return@SocialConfirmDialog
                }
                blocking = true
                scope.launch {
                    SocialApi.block(active, userId).fold(
                        onSuccess = { navController.popBackStack() },
                        onFailure = { error = it.message }
                    )
                    blocking = false
                    confirmBlock = false
                }
            }
        )
    }
}

/**
 * The identity block — who this is, what they have posted in the last day, and
 * the one action that fits the relationship.
 *
 * The counts are computed from the SAME list the grid renders, so the page can
 * never claim a number it is not showing: "posts" is what is on screen, "likes"
 * and "replies" are what those posts received. The portrait is the same
 * code-drawn disc used everywhere else, at the largest size in the app, so a
 * member looks identical in the wall, in a reply and here.
 */
@Composable
private fun SocialProfileHeader(
    person: CurioPerson?,
    isMe: Boolean,
    loading: Boolean,
    cards: List<CommunityCard>,
    friendRequestId: String?,
    asked: Boolean,
    onAsk: () -> Unit,
    onMessage: () -> Unit,
    /** Blocks this member — the one destructive move a profile offers. */
    onBlock: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val likes = remember(cards) { cards.sumOf { it.likeCount } }
    val replies = remember(cards) { cards.sumOf { it.commentCount } }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SocialAvatar(
                    style = person?.avatarStyle ?: 0,
                    avatarSize = 84.dp,
                    onClick = null
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // The DISPLAY name LEADS; the @username reads beneath it.
                    // Presence is deliberately NOT drawn here — Curio's
                    // activity status is only ever shown inside a direct chat
                    // (see `docs/ONLINE_PRIVACY.md`).
                    Text(
                        text = person?.label
                            ?: if (loading) "Loading…" else "This profile isn't visible",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = person?.handleLabel?.ifBlank { "@…" } ?: "@…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        SocialProfileStat(value = cards.size, label = "posts")
                        SocialProfileStat(value = likes, label = "likes")
                        SocialProfileStat(value = replies, label = "replies")
                    }
                }
                if (loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Their own words, and NOTHING when they wrote none — a profile
            // must never pad itself with a placeholder line.
            person?.bio?.trim()?.takeIf { it.isNotEmpty() }?.let { bio ->
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // The action (or the line that replaces it) claims the row, so
                // the ⋮ below always sits at the end of it.
                Box(Modifier.weight(1f)) {
                    when {
                        isMe -> Text(
                            text = "This is your own profile — your name and portrait live in Edit profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        friendRequestId != null -> SocialProfileAction(
                            label = "Message",
                            glyph = CurioIcons.Notes,
                            onClick = onMessage
                        )
                        asked -> Text(
                            text = "Friend request sent. They will see it in Friends.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> SocialProfileAction(
                            label = "Add friend",
                            glyph = CurioIcons.Person,
                            onClick = onAsk
                        )
                    }
                }
                if (!isMe) {
                    // Blocking is rare and irreversible, so it sits behind the
                    // page's own ⋮ rather than under a finger that is only
                    // reading.
                    Box {
                        CurioIcon(
                            name = CurioIcons.MoreVert,
                            contentDescription = "More profile actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 20.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { menuOpen = true }
                                .padding(6.dp)
                        )
                        CurioDropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            accent = settingsRoseAccent()
                        ) {
                            CurioDropdownItem(
                                text = { Text("Block", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    menuOpen = false
                                    onBlock()
                                },
                                danger = true
                            )
                        }
                    }
                }
            }

            // The privacy promise, in one line: what this page exposes, and
            // what it deliberately never does.
            Text(
                text = "A profile shows a name, a portrait and the last 24 hours of posts. " +
                    "Your activity status stays inside direct chats; email, saved entries " +
                    "and messages are never part of it.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One count in the identity block: the number, then what it counts. */
@Composable
private fun SocialProfileStat(value: Int, label: String) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The profile's primary action, as a filled pill — the same door the wall and a
 * conversation use, so "Message" and "Add friend" can never look like two
 * different kinds of thing depending on where you are.
 */
@Composable
private fun SocialProfileAction(label: String, glyph: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = curioDialogActionButtonColors()
    ) {
        CurioIcon(
            name = glyph,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            size = 16.dp
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

/**
 * ONE preview in the profile grid.
 *
 * A topic card is its own art, rendered at the tile's width (the real share
 * card, never a lookalike, so the preview matches the exported image). A note
 * or a quote has no art — the words ARE the post, so the tile shows them,
 * clipped to a tidy handful of lines with the credit under a quote. Nothing but
 * the post: a preview is a door, and the counts and actions live on the post's
 * own page.
 */
@Composable
private fun SocialProfileTile(card: CommunityCard, onClick: () -> Unit) {
    val tap = Modifier.curioPressClickable(
        pressedScale = 0.96f,
        hapticOnPress = false,
        onClickLabel = "Open post",
        onClick = onClick
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (card.kind == KIND_CARD) {
            // Clipped to the tile's own rounding: the share card brings its own
            // full-bleed art, and an unclipped preview would square off the
            // corners of the tile it sits in.
            CommunityCardCanvas(
                card = card,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .then(tap),
                widthFraction = 1f
            )
        } else {
            Column(
                modifier = Modifier
                    .heightIn(min = 118.dp)
                    .then(tap)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurioIcon(
                    name = if (card.kind == KIND_QUOTE) CurioIcons.FormatQuote else CurioIcons.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 14.dp
                )
                Text(
                    text = card.factText.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 7,
                    overflow = TextOverflow.Ellipsis
                )
                card.byline.trim().takeIf { it.isNotEmpty() }?.let { credit ->
                    Text(
                        text = "— $credit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
