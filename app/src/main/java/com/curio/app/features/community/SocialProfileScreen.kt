package com.curio.app.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.BAN_CONTENT
import com.curio.app.data.supabase.CommunityAdminRow
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityReportReasons
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.ModerationReasons
import com.curio.app.data.supabase.ModerationRecord
import com.curio.app.data.supabase.banTierLabel
import com.curio.app.data.supabase.communityMessage
import com.curio.app.data.supabase.KIND_CARD
import com.curio.app.data.supabase.KIND_QUOTE
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.SocialApi
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionInfoRow
import com.curio.app.features.settings.SettingsOptionRow
import com.curio.app.features.settings.SettingsSectionHeading
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.settingsHeroPillFill
import com.curio.app.features.settings.settingsHeroTotalHeight
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.data.StreakTracker
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
import com.curio.app.ui.theme.curioFillInk
import com.curio.app.ui.theme.isCurioDarkTheme
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
    // Reporting a member, and a moderator's BAN — the ladder's sheet replaced
    // the old yes/no hide, so a moderator picks how far the ban reaches from
    // the page they are looking at.
    var reporting by remember { mutableStateOf(false) }
    var banSheet by remember { mutableStateOf(false) }
    var liftingBan by remember { mutableStateOf(false) }
    var moderationBusy by remember { mutableStateOf(false) }
    // The tier in force on this member (blank = not banned), read from their
    // own profile row. The menu, the ban chip and the sheet all read it, so a
    // moderator can never act on a stale guess about what is already set.
    var targetKind by remember { mutableStateOf("") }
    var myAdmin by remember { mutableStateOf<CommunityAdminRow?>(null) }
    // The moderation record: the team sees anyone's, and a member sees their
    // own (the server allows exactly those two readers).
    var history by remember { mutableStateOf<List<ModerationRecord>>(emptyList()) }
    // The follow tie: read once with the profile, flipped optimistically by
    // the button and corrected by the server answer.
    var following by remember { mutableStateOf(false) }

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
            // Which TIER is in force on this member right now? Read from their
            // own profile row, so a moderator's menu can offer Ban or Change
            // ban or Lift rather than a one-way door. Best-effort: not
            // discoverable simply reads as not banned, and no action is lost.
            SocialApi.moderationStatus(active, userId).onSuccess { status ->
                targetKind = status.kind.ifBlank { if (status.hidden) BAN_CONTENT else "" }
            }
            CommunityApi.myAdminRow(active, myUserId).onSuccess { myAdmin = it }
            // Are we already friends? Decides Message vs Add friend.
            SocialApi.friends(active, myUserId).fold(
                onSuccess = { list ->
                    friendRequestId = list.firstOrNull { it.person.userId == userId }?.requestId
                },
                onFailure = { /* friendship stays unknown; the pill offers the ask */ }
            )
            // And do we already follow them? Decides Follow vs Following.
            CommunityApi.followingIds(active, myUserId).onSuccess { ids ->
                following = userId in ids
            }
        }
        // The record: anyone may read their OWN, and the team may read this
        // member's. Anything else is refused by the server, so the call is made
        // only when one of those two is true.
        if (isMe || myAdmin?.allows("bans") == true) {
            CommunityApi.memberHistory(active, userId).onSuccess { history = it }
        }
        loading = false
    }

    LaunchedEffect(userId, token) { if (token != null) load() }

    val hasBio = person?.bio?.isNullOrBlank() == false

    /**
     * THE TEAR, built once and handed to the hero by both layouts — the wide
     * list item and the pinned phone banner — so a tablet and a phone can never
     * drift apart on what a profile's header contains.
     */
    val profileTear: @Composable (Color) -> Unit = { ink ->
        SocialProfileHeroBlock(
            ink = ink,
            height = profileTearHeight(hasBio),
            person = person,
            isMe = isMe,
            loading = loading,
            cards = cards,
            friendRequestId = friendRequestId,
            asked = asked,
            following = following,
            onFollow = onFollow@{
                val active = token
                val me = myUserId
                if (active == null || me == null) return@onFollow
                // Optimistic: the pill flips NOW, the server is told after.
                // A refused call rolls the pill back — a wrong pill is a
                // small lie, but it is still a lie.
                following = !following
                scope.launch {
                    val call = if (following) CommunityApi.follow(active, userId, me)
                               else CommunityApi.unfollow(active, userId, me)
                    call.fold(
                        onSuccess = {},
                        onFailure = {
                            following = !following
                            error = it.message
                        }
                    )
                }
            },
            bannedKind = targetKind,
            canModerate = myAdmin?.allows("bans") == true,
            onAsk = {
                val active = token
                if (active != null && myUserId != null) {
                    scope.launch {
                        SocialApi.ask(active, userId, myUserId).fold(
                            onSuccess = { asked = true },
                            onFailure = { error = it.message }
                        )
                    }
                }
            },
            onMessage = {
                navController.navigate(CurioRoutes.directMessage(userId.orEmpty())) {
                    launchSingleTop = true
                }
            },
            onEdit = {
                // The profile's own door back into the editor — the same dialog
                // "You" opens, without a detour through Settings.
                navController.navigate(CurioRoutes.PROFILE) { launchSingleTop = true }
            },
            onReport = { reporting = true },                            onBan = {
                                error = null
                                banSheet = true
                            },
            onLiftBan = { liftingBan = true },
            onBlock = { confirmBlock = true }
        )
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

        LazyVerticalGrid(
            // Two columns of small previews on a phone; a wide window fits a
            // third without the previews turning into full cards again.
            columns = if (wide) GridCells.Fixed(3) else GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = wideContentEdgePadding(),
                end = wideContentEdgePadding(),
                // The identity block lives INSIDE the hero now, so the tear is
                // extended by its height and the grid starts below it — the
                // same reservation the Social wall does for its doors row.
                top = if (wide) 0.dp else settingsHeroTotalHeight(profileTearHeight(hasBio)),
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
                        title = person?.label ?: "Profile",
                        subtitle = person?.handleLabel.orEmpty(),
                        onBack = { navController.popBackStack() },
                        footer = profileTear,
                        footerHeight = profileTearHeight(hasBio)
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

            error?.let { message ->
                item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // The record, above the posts: a member who has been moderated
            // should find the reason on their own page without asking, and a
            // moderator should find it before they act rather than after.
            if (history.isNotEmpty()) {
                item(key = "history-heading", span = { GridItemSpan(maxLineSpan) }) {
                    SettingsSectionHeading(
                        if (isMe) "Your moderation history" else "Moderation history"
                    )
                }
                item(key = "history", span = { GridItemSpan(maxLineSpan) }) {
                    SocialModerationHistoryCard(records = history, isMe = isMe)
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
                title = person?.label ?: "Profile",
                subtitle = person?.handleLabel.orEmpty(),
                onBack = { navController.popBackStack() },
                footer = profileTear,
                footerHeight = profileTearHeight(hasBio),
                glassBackdrop = glassBackdrop
            )
        }
    }

    if (reporting) {
        val active = token
        if (active != null) {
            ReportTargetDialog(
                title = "Report this member",
                subtitle = "Reports go to the moderation team, with your reason.",
                reasons = CommunityReportReasons.MEMBER,
                onDismiss = { reporting = false },
                onReport = { reason, note ->
                    scope.launch {
                        CommunityApi.report(active, "user", userId, reason, note).fold(
                            onSuccess = { reporting = false },
                            onFailure = { error = it.message }
                        )
                    }
                }
            )
        }
    }

    if (banSheet) {
        val active = token
        if (active != null) {
            ModerationBanDialog(
                memberName = person?.label ?: "this member",
                currentKind = targetKind,
                busy = moderationBusy,
                // What the server said, INSIDE the sheet. A refusal used to be
                // written on the page behind it, where nobody reads it while a
                // dialog is up.
                error = error,
                onDismiss = { if (!moderationBusy) banSheet = false },
                onConfirm = { kind, reason, hours ->
                    moderationBusy = true
                    scope.launch {
                        CommunityApi.banMember(active, userId, kind, reason, hours).fold(
                            onSuccess = {
                                targetKind = kind
                                banSheet = false
                                load()
                            },
                            onFailure = { error = communityMessage(it) }
                        )
                        moderationBusy = false
                    }
                },
                onLift = if (targetKind.isNotBlank()) {
                    {
                        // The ban sheet steps aside for the lift: two dialogs on
                        // the same subject stacked on each other is one too many.
                        banSheet = false
                        liftingBan = true
                    }
                } else null
            )
        }
    }

    if (liftingBan) {
        val active = token
        if (active != null) {
            ModerationReasonDialog(
                title = "Lift the ban on ${person?.label ?: "this member"}?",
                subtitle = "Nothing stays paused for them. The record of the ban remains in their " +
                    "moderation history, which is what stops a lift from erasing what happened.",
                reasons = ModerationReasons.LIFT,
                confirmLabel = "Lift ban",
                destructive = false,
                busy = moderationBusy,
                error = error,
                onDismiss = { if (!moderationBusy) liftingBan = false },
                onConfirm = { reason, _ ->
                    moderationBusy = true
                    scope.launch {
                        CommunityApi.liftBan(active, userId, reason).fold(
                            onSuccess = {
                                targetKind = ""
                                liftingBan = false
                                banSheet = false
                                load()
                            },
                            onFailure = { error = communityMessage(it) }
                        )
                        moderationBusy = false
                    }
                }
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
 * THE PROFILE IN THE TEAR — the identity block, drawn INSIDE the hero banner.
 *
 * It used to be a card UNDER the banner ("Profile · A member of the
 * community", then a surface carrying the portrait), which made the top of a
 * person's page read as a header that belonged to nobody. The Social wall's
 * doors row had already proved the better shape: content that belongs to the
 * header rides INSIDE the torn banner, on the hero's own glass. The identity
 * does exactly that now — the tear is extended by [profileTearHeight], the
 * banner's own title carries the display name and the @handle, and the block
 * beneath it carries the three honest counts, the bio and the one action that
 * fits the relationship.
 *
 * Everything here paints in the hero's readable [ink] on hero glass, never in
 * page colours: a block sitting on a coloured banner has exactly one palette
 * that can be read on it, and this is it.
 *
 * @param height the SAME number the caller passed as `footerHeight`. The block
 *   is GIVEN its space rather than measuring itself, so the banner and its
 *   content can never disagree by a pixel — the reservation is the layout.
 */
@Composable
private fun SocialProfileHeroBlock(
    ink: Color,
    height: Dp,
    person: CurioPerson?,
    isMe: Boolean,
    loading: Boolean,
    cards: List<CommunityCard>,
    friendRequestId: String?,
    asked: Boolean,
    /** True when this account already follows the member. */
    following: Boolean,
    /** Follow / unfollow — never offered on your own page (you cannot follow yourself). */
    onFollow: () -> Unit,
    /** The tier in force on this member, blank when they are not banned. */
    bannedKind: String,
    /** True for a moderator with the 'bans' permission. */
    canModerate: Boolean,
    /** Asked when there is no friendship yet. */
    onAsk: () -> Unit,
    onMessage: () -> Unit,
    onEdit: () -> Unit = {},
    onReport: () -> Unit = {},
    onBan: () -> Unit = {},
    onLiftBan: () -> Unit = {},
    onBlock: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val likes = remember(cards) { cards.sumOf { it.likeCount } }
    val replies = remember(cards) { cards.sumOf { it.commentCount } }
    // A streak is device-local, so it can only ever be YOURS: another member's
    // flame would be this phone's habit wearing their name.
    val streak = remember(isMe) { if (isMe) StreakTracker.getStreak(context) else 0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Row one: the portrait, and the numbers beside it ───────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SocialAvatar(
                style = person?.avatarStyle ?: 0,
                avatarSize = 60.dp,
                onClick = null,
                // On the banner the disc's own rim would double the tear's
                // edge, so the portrait drops it and lets the hero frame it.
                ring = false
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SocialProfileStat(value = cards.size, label = "posts", ink = ink)
                SocialProfileStat(value = likes, label = "likes", ink = ink)
                SocialProfileStat(value = replies, label = "replies", ink = ink)
                if (isMe) SocialProfileStreak(value = streak, ink = ink)
            }
        }

        if (loading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = ink,
                modifier = Modifier.size(14.dp)
            )
        }

        // A ban in force is stated ON THE PAGE, in the tier's own words: a
        // moderator arriving here has to know what is already set before they
        // touch anything. It takes the bio's line while it is up — a ban is
        // the more urgent sentence, and one line is what there is room for.
        if (bannedKind.isNotBlank()) {
            Text(
                text = "Banned: ${banTierLabel(bannedKind)}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            // Their own words, and NOTHING when they wrote none — a profile
            // must never pad itself with a placeholder line.
            person?.bio?.trim()?.takeIf { it.isNotEmpty() }?.let { bio ->
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Row two: the one action that fits, and the ⋮ ───────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.weight(1f)) {
                when {
                    isMe -> SocialProfileHeroAction(
                        label = "Edit profile",
                        glyph = CurioIcons.Edit,
                        ink = ink,
                        onClick = onEdit
                    )
                    friendRequestId != null -> SocialProfileHeroAction(
                        label = "Message",
                        glyph = CurioIcons.Chats,
                        ink = ink,
                        onClick = onMessage
                    )
                    asked -> SocialProfileHeroAction(
                        label = "Request sent",
                        glyph = CurioIcons.TaskAlt,
                        ink = ink,
                        onClick = {}
                    )
                    else -> SocialProfileHeroAction(
                        label = "Add friend",
                        glyph = CurioIcons.Person,
                        ink = ink,
                        onClick = onAsk
                    )
                }
            }
            if (!isMe) {
                // FOLLOW — the lightest tie the app offers, and the wall's
                // Following filter is its point: you follow a member so their
                // posts survive the wall's 24-hour churn at a glance. It is a
                // second pill beside the friend one, because a friendship ask
                // and a follow are two different questions.
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (following) settingsHeroPillFill()
                            else settingsRoseAccent().copy(alpha = 0.85f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onFollow)
                ) {
                    Text(
                        text = if (following) "Following" else "Follow",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        // v412 — the ink ASKS the pill fill (see [curioFillInk]):
                        // the accent pill is the bright pale primary at night.
                        color = if (following) ink else curioFillInk(settingsRoseAccent().copy(alpha = 0.85f)),
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
            if (!isMe) {
                // Blocking and banning are rare and irreversible, so they live
                // behind the page's own ⋮ rather than under a finger that is
                // only reading.
                Box {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = settingsHeroPillFill(),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { menuOpen = true }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CurioIcon(
                                name = CurioIcons.MoreVert,
                                contentDescription = "More profile actions",
                                tint = ink,
                                size = 20.dp
                            )
                        }
                    }
                    CurioDropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        accent = settingsRoseAccent()
                    ) {
                        CurioDropdownItem(
                            text = { Text("Report member", style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                menuOpen = false
                                onReport()
                            }
                        )
                        if (canModerate) {
                            CurioDropdownItem(
                                text = {
                                    Text(
                                        text = if (bannedKind.isBlank()) "Ban member…" else "Change ban…",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onBan()
                                },
                                danger = bannedKind.isBlank()
                            )
                            if (bannedKind.isNotBlank()) {
                                CurioDropdownItem(
                                    text = { Text("Lift ban", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        menuOpen = false
                                        onLiftBan()
                                    }
                                )
                            }
                        }
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
    }
}

/**
 * The tear's height for a member WITH or WITHOUT a bio.
 *
 * One function because two numbers must agree: the height the banner extends by
 * and the height the grid reserves. A bio adds exactly one line's worth.
 */
private fun profileTearHeight(hasBio: Boolean): Dp =
    if (hasBio) 176.dp else 134.dp

/**
 * THE MODERATION RECORD — what was done to this member, when, why and by whom.
 *
 * Shown to the team on any profile and to a member on their own, because the
 * server allows exactly those two readers. Only drawn when there IS a record:
 * an empty "you have never been moderated" card would be a promise nobody asked
 * for.
 */
@Composable
internal fun SocialModerationHistoryCard(records: List<ModerationRecord>, isMe: Boolean) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isMe) {
                    "Nothing here is a surprise: this is what the moderation team has on you."
                } else {
                    "The team's record for this member. Only moderators can see it."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            records.forEach { record ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = moderationActionLabel(record.action),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val tail = listOfNotNull(
                        record.reason?.let { "“$it”" },
                        record.actorName.trim().takeIf { it.isNotEmpty() }?.let { "by $it" },
                        record.createdAtMillis.takeIf { it > 0L }?.let { stamp(it) }
                    ).joinToString(" · ")
                    if (tail.isNotBlank()) {
                        Text(
                            text = tail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * A moderation verb, said the way a person would say it. The database stores
 * the machine name (`ban_read_only`, `unban`) — the screen is not the place to
 * make somebody decode it.
 */
private fun moderationActionLabel(action: String): String = when (action) {
    "ban_content" -> "Content hidden"
    "ban_read_only" -> "Set to view only"
    "ban_social" -> "Banned from friends and messages"
    "ban_account" -> "Account banned"
    "unban" -> "Ban lifted"
    "hide_member" -> "Content hidden"
    "unhide_member" -> "Ban lifted"
    "remove_card" -> "A post was removed"
    "remove_comment" -> "A reply was removed"
    "dismiss" -> "A report was closed"
    else -> action.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/** "12 Sep, 14:03" — short, local, and only what a record needs. */
private fun stamp(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm"))


/** One count in the tear: the number OVER its label, centred — a column reads
 *  at a glance where a run of inline numbers does not. Both lines wear the
 *  HERO's own ink, because the counts now sit on the banner: page colours on a
 *  rose banner were the "can't read the post counts" failure all over again. */
@Composable
private fun SocialProfileStat(value: Int, label: String, ink: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = ink
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ink.copy(alpha = 0.78f)
        )
    }
}

/** The streak column: the flame over the days, the icon doing the labelling. */
@Composable
private fun SocialProfileStreak(value: Int, ink: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CurioIcon(
                name = CurioIcons.LocalFire,
                contentDescription = null,
                tint = ink,
                size = 16.dp
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = ink
            )
        }
        Text(
            text = "streak",
            style = MaterialTheme.typography.labelSmall,
            color = ink.copy(alpha = 0.78f)
        )
    }
}

/**
 * The profile's one primary action, ON THE BANNER: the hero's ink as the FILL
 * and the hero's own colour as the content — the exact inverse of the banner,
 * which is the strongest contrast a button on it can have. It fills the row's
 * width, the way a profile's primary action should.
 *
 * The same door every other surface uses, so "Message" and "Add friend" can
 * never look like two different kinds of thing depending on where you are.
 */
@Composable
private fun SocialProfileHeroAction(
    label: String,
    glyph: String,
    ink: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    val content = settingsRoseAccent()
    Surface(
        shape = shape,
        color = ink,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            CurioIcon(
                name = glyph,
                contentDescription = null,
                tint = content,
                size = 16.dp
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = content,
                maxLines = 1
            )
        }
    }
}

/**
 * ONE preview in the profile grid — a square, Instagram-style.
 *
 * A TOPIC post previews as the TOPIC, not as card art: the tile wears the
 * lane's own colour wash, that lane's mark in a chip, and the topic's name.
 * A share card squeezed into a square tile is a cropped thumbnail of a poster
 * nobody can read, and the full card already has a page of its own one tap
 * away. A note or a quote has no topic — the words ARE the post — so the tile
 * shows them on the same square canvas. Nothing but the post: a preview is a
 * door, and the counts and actions live on the post's own page.
 */
@Composable
private fun SocialProfileTile(card: CommunityCard, onClick: () -> Unit) {
    val tap = Modifier.curioPressClickable(
        pressedScale = 0.96f,
        hapticOnPress = false,
        onClickLabel = "Open post",
        onClick = onClick
    )
    val topic = card.kind == KIND_CARD
    // The lane's colour, as remembered on the post itself ([CommunityCard
    // .accentHex]). In dark mode the deep accent reads muddy on a midnight
    // page, so the wash takes the lifted twin — the same rule the category
    // backgrounds follow everywhere else in Curio.
    val accent = remember(card.accentHex) { parseAccent(card.accentHex) }
    val dark = isCurioDarkTheme()
    val tint = if (dark) lerp(accent, Color.White, 0.55f) else accent
    val base = MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (topic) lerp(base, tint, if (dark) 0.18f else 0.13f) else base,
        modifier = Modifier
            .fillMaxWidth()
            // The square is the grid's rhythm: every tile the same height,
            // whatever the post inside it is.
            .aspectRatio(1f)
    ) {
        if (topic) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(tap)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = tint.copy(alpha = if (dark) 0.24f else 0.16f)
                ) {
                    Box(
                        modifier = Modifier.size(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CurioIcon(
                            name = card.categoryGlyph.ifBlank { CurioIcons.Notes },
                            contentDescription = null,
                            tint = tint,
                            size = 17.dp
                        )
                    }
                }
                Text(
                    text = card.topicName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                Spacer(Modifier.weight(1f))
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
