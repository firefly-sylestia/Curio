package com.curio.app.features.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.SocialPostArchive
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityReportReasons
import com.curio.app.data.supabase.CurioModerationStatus
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityCardDraft
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.KIND_CARD
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
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.components.curioPressClickable
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioNavTint
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ShareCardAspect
import com.curio.app.ui.components.ShareCardStyle
import com.curio.app.ui.components.TopicShareCard
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.data.supabase.RealtimeWatch
import com.curio.app.data.supabase.SupabaseRealtime
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.curioFillInk
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * COMMUNITY — the 24-hour wall of text share cards.
 *
 * A card here is TEXT + TOPIC + STYLE data only (see `CommunityCardDraft`):
 * it is rebuilt on every device with the app's own share-card renderer, and
 * the schema has no column that could carry an image, audio or screenshot.
 * Cards expire 24 hours after posting and the feed asks only for live ones.
 *
 * Entry is gated twice — the account must be signed in AND Online Mode must
 * be on — and the server enforces the same gate through RLS, so a client
 * that lied about either would still get nothing back.
 */
@Composable
fun CommunityScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // v389d — THE SESSION IS RESTORED BEFORE THE FIRST READ of it, not in an
    // effect that runs after the first frame. With a stored session the page
    // painted the signed-OUT copy first ("Sign in to see the social wall") and
    // then swapped in "Last 24 hours" — the section the user reported arriving
    // late every time Social opened. [OnlineAccount.restore] is idempotent and
    // only touches storage when it has no session yet, so the effect that used
    // to sit further down is gone with it.
    //
    // v390 — AND A `remember` MAY NOT RETURN Unit (lint's RememberReturnType
    // check, which fails the release lint job outright: "remember calls must not
    // return Unit"). The call still has to happen exactly here — during the first
    // composition and BEFORE the account is read on the next line, which is what
    // puts the signed-in wall on the first frame — so it is wrapped so the
    // composition has a value to hold instead of a bare side effect.
    remember(context) {
        OnlineAccount.restore(context)
        true
    }
    val account = OnlineAccount.state
    val token = account.session?.accessToken
    val wide = windowWidthSizeClass().isWide
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()

    // The observable mirror, not a local copy: the Online Mode switch on the
    // account page and the bottom-bar opt-in both feed this page, so it has to
    // follow whatever they last wrote.
    val onlineMode = AppPreferences.onlineModeEnabledState
    // True while the opt-in Community tab is on the bottom bar: then this page
    // IS a tab root, so it drops the settings rail and the back pill (tapping
    // a tab must not offer a "back" — the bar is the navigation) and clears
    // the floating bar at the bottom of the list.
    val asTab = AppPreferences.communityTabVisible
    // v389d — THE WALL IS THE DEVICE'S OWN PAGE ON THE FIRST FRAME. `cards`
    // started empty and was filled from the cache inside a LaunchedEffect (which
    // runs AFTER the first frame), so every open painted "Nothing here yet" for
    // a beat and then swapped in the wall the phone already had (user report:
    // "in community the last 24 hours nothing here yet appears late"). The read
    // is the one the effect already made — its own memory map answers every
    // repeat — it just happens before the frame instead of after it.
    var cards by remember { mutableStateOf(SocialFeedCache.read(context)) }
    var loading by remember { mutableStateOf(false) }
    // True once an answer has actually been seen: the cache's page, or the
    // server's. An empty wall and an unread wall are not the same thing, so the
    // "Nothing here yet" card waits for one.
    var answered by remember { mutableStateOf(false) }
    // The FOLLOWING filter (v389): off shows the whole wall; on shows only the
    // posts of members this account follows. The ids are read once when the
    // wall opens (and after each refresh), so the filter costs the feed
    // nothing extra.
    var followingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var followingOnly by remember { mutableStateOf(false) }
    // ONLY a pull-down sets this: the wall's own refresh indicator belongs to
    // the user's gesture, never to a background read (a like used to flash a
    // spinner because every action funnelled through `load()`).
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // True while the WALL on screen is the device's own copy (the first
    // request failed), so the page can say so instead of pretending.
    var offlineCopy by remember { mutableStateOf(false) }
    var composing by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf<CommunityCard?>(null) }
    var deleteTarget by remember { mutableStateOf<CommunityCard?>(null) }
    var commentsFor by remember { mutableStateOf<CommunityCard?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // A server push bumps this; the wall then refreshes QUIETLY (no spinner, no
    // scroll reset, no offline flag) so a post landing while you read is simply
    // there the next time you look up.
    var pushed by remember { mutableStateOf(0) }
    // v389d — seeded from the right the server LAST confirmed (see
    // AppPreferences.KEY_COMMUNITY_ADMIN), so the Moderation section is on the
    // page the moment it opens instead of a round trip later. This open's own
    // check below still corrects it; the database is the authority.
    var isCommunityAdmin by remember { mutableStateOf(AppPreferences.communityAdminState) }
    // May THIS account remove someone else's reply — the sheet's one moderator
    // move. The database asks again; this only decides what is offered.
    var canModerateReplies by remember { mutableStateOf(AppPreferences.communityAdminRepliesState) }
    // My OWN moderation state — only ever used to explain a hidden account to
    // the person it applies to.
    var moderation by remember { mutableStateOf<CurioModerationStatus?>(null) }

    // The post button slips away while the wall is being scrolled DOWN and
    // comes back the moment the finger goes up or the wall reaches the top —
    // the Home "+" behaviour, applied to the wall's own list. A wall is a long
    // read, and a button nobody asked for must not sit on top of it.
    var postVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var previous = listState.firstVisibleItemIndex * 100_000 + listState.firstVisibleItemScrollOffset
        snapshotFlow {
            listState.firstVisibleItemIndex * 100_000 + listState.firstVisibleItemScrollOffset
        }.collect { now ->
            val delta = now - previous
            if (delta > 6) postVisible = false
            else if (delta < -6 || now <= 0) postVisible = true
            previous = now
        }
    }

    // The follow map the Following filter reads: one small request when the
    // wall opens, refreshed alongside the feed so a follow made on a profile
    // is honoured the next time the wall reloads.
    LaunchedEffect(token, account.session?.userId) {
        val active = token
        val me = account.session?.userId
        if (active != null && me != null) {
            CommunityApi.followingIds(active, me).onSuccess { followingIds = it }
        } else {
            followingIds = emptySet()
        }
    }
    LaunchedEffect(token, account.session?.userId) {
        val active = token
        val userId = account.session?.userId
        if (active != null && userId != null) {
            CommunityApi.myAdminRow(active, userId).onSuccess { row ->
                isCommunityAdmin = row != null
                canModerateReplies = row?.allows("replies") == true
                // Remembered, so the next open has the door already.
                AppPreferences.setCommunityAdmin(context, row != null, canModerateReplies)
            }
            SocialApi.moderationStatus(active, userId).onSuccess { moderation = it }
        } else {
            isCommunityAdmin = false
            canModerateReplies = false
            moderation = null
            AppPreferences.setCommunityAdmin(context, false, false)
        }
    }

    val eligible = account.signedIn && onlineMode && token != null
    // A ban with a wall tier takes the compose door away: the server refuses
    // the write anyway, and offering a button that can only fail is worse than
    // not offering it. Until my own status has been read, the door is open.
    val canWriteWall = moderation?.let { !it.readOnly && !it.locked } ?: true

    suspend fun load() {
        val active = token ?: return
        loading = true
        CommunityApi.feed(active, account.session?.userId).fold(
            onSuccess = {
                cards = it
                error = null
                offlineCopy = false
                // The page the device just saw is kept, so the next open (in a
                // lift, in a tunnel, in airplane mode) still shows a wall.
                SocialFeedCache.write(context, it)
                // Every author on the wall is remembered too, so a profile or
                // a conversation opened from a card names them at once.
                SocialPeopleCache.remember(
                    context,
                    it.map { card ->
                        CurioPerson(
                            userId = card.authorId,
                            displayName = card.authorName,
                            username = card.authorName.ifBlank { card.authorHandle },
                            avatarStyle = card.authorAvatar
                        )
                    }
                )
            },
            onFailure = { failure ->
                // The device's own copy is already on screen: say nothing at
                // all unless there was nothing to fall back on.
                if (cards.isEmpty()) error = failure.message else offlineCopy = true
            }
        )
        loading = false
        answered = true
    }

    /**
     * The background refresh a realtime push triggers: the same read as [load],
     * but it never touches the loading state, the error, or the offline flag —
     * a wall that is already on screen must not flash or move because somebody
     * posted.
     */
    suspend fun refreshQuietly() {
        val active = token ?: return
        CommunityApi.feed(active, account.session?.userId).onSuccess { fresh ->
            cards = fresh
            error = null
            offlineCopy = false
            SocialFeedCache.write(context, fresh)
        }
    }

    /**
     * Moves ONE card on screen without asking the server anything.
     *
     * This is what makes the wall feel instant: a like, a dislike or a count
     * change answers the tap immediately and the network catches up behind it.
     * A failed call resyncs through [refreshQuietly], so the screen is never
     * left disagreeing with the row.
     */
    fun patchCard(cardId: String, transform: (CommunityCard) -> CommunityCard) {
        cards = cards.map { if (it.id == cardId) transform(it) else it }
    }

    // v412b — the Social tab PUBLISHES ITS NAV ACCENT: it was the one bottom
    // tab that published none, so its active pill fell back to the theme's
    // secondaryContainer — in dark mode a butter wash with a pale-butter ink
    // on top, which read as a yellow tab whose label was invisible.
    // v413 — AND IT PUBLISHES THE APP'S OWN ACCENT, NOT THE DIALOG INK.
    // v412b filled the pill with `curioDialogActionColor()` — the accent at
    // its ACTION depth (saturation 0.35–0.60 at lightness 0.36), which is
    // right for a button's words and wrong for an indicator: the other three
    // tabs publish a page FILL (Home's hero rose, Spin's deck lane, the
    // Cabinet's filter lane), so their pills are soft pale accents with a deep
    // ink, while the Social pill came out a heavy deep-pink slab wearing white
    // text (member: "fix the social tab active indicator not following the app
    // active indicator and its text color"). The theme's own primary IS the
    // app's active accent — the same value every unpublished page and
    // [curioActivePillFill]'s fallback use — so the pill and its label ink
    // (pastelFillInk, the family's own pairing) now resolve like the other
    // three. Resolved IN COMPOSITION because the effect's body is not.
    val socialNavAccent = MaterialTheme.colorScheme.primary
    DisposableEffect(socialNavAccent) {
        CurioNavTint.publishSocialAccent(socialNavAccent)
        onDispose { CurioNavTint.publishSocialAccent(null) }
    }

    LaunchedEffect(eligible, token) {
        if (eligible) {
            // The device's copy is ALREADY on screen (seeded at composition),
            // so this read only runs when the page opened with nothing; either
            // way the server's answer replaces it in place.
            if (cards.isEmpty()) cards = SocialFeedCache.read(context)
            load()
        } else {
            cards = emptyList()
        }
    }

    // REALTIME — the wall is no longer a snapshot from the moment it composed.
    // The server announces a new (or deleted) card and the wall quietly pulls
    // the live page again, so a post appears while you are looking at it.
    DisposableEffect(eligible, token) {
        val active = token
        val owner = "wall"
        if (eligible && active != null) {
            SupabaseRealtime.watch(
                owner = owner,
                accessToken = active,
                watches = listOf(
                    RealtimeWatch(table = "community_cards", events = listOf("INSERT", "DELETE"))
                )
            ) {
                scope.launch { pushed++ }
            }
        }
        onDispose { SupabaseRealtime.unwatch(owner) }
    }

    LaunchedEffect(pushed, eligible, token) {
        if (!eligible || token == null || pushed == 0) return@LaunchedEffect
        // One refresh per burst: the push is a hint, and a lively wall would
        // otherwise rebuild all forty cards once per post.
        delay(400)
        refreshQuietly()
    }

    // v386 — the wall's doors (Chats / Friends / You), rendered INSIDE the
    // hero banner through SettingsHeroHeader's footer slot so they paint WITH
    // the header. They used to be a list item under “Last 24 hours”, which
    // meant they only showed up once the account check landed — the row was
    // missing every time the wall opened. Defined once here because both hero
    // placements (the pinned phone overlay, the wide in-list hero) wear it.
    val doorsFooter: @Composable (Color) -> Unit = { ink ->
        SocialDoorsRow(
            ink = ink,
            onChats = { navController.navigate(CurioRoutes.CHATS) },
            onFriends = { navController.navigate(CurioRoutes.FRIENDS) },
            onYou = {
                // Guarded: an unmatched person/ route would throw.
                val me = account.session?.userId
                if (!me.isNullOrBlank()) {
                    navController.navigate(CurioRoutes.socialProfile(me)) {
                        launchSingleTop = true
                    }
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                heroPageBackground(
                    androidx.compose.ui.graphics.lerp(
                        MaterialTheme.colorScheme.background,
                        settingsRoseAccent(),
                        0.10f
                    )
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
                refreshing = true
                scope.launch {
                    load()
                    refreshing = false
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
                // v386 — the hero now carries the doors row (see [SocialDoorsRow]),
                // so the wall starts below the EXTENDED banner.
                top = if (wide) 0.dp else settingsHeroTotalHeight(SocialDoorRowHeight),
                // As a tab root the last card has to clear the floating pill
                // bar (the NavHost drops the system nav inset on tab routes
                // because the bar carries it) — same 84dp the Cabinet uses.
                bottom = 28.dp + if (asTab) {
                    84.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                } else {
                    0.dp
                }
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (wide) {
                item(key = "hero", contentType = "hero") {
                    SettingsHeroHeader(
                        title = "Social",
                        subtitle = "",
                        onBack = if (asTab) null else ({ navController.popBackStack() }),
                        footer = doorsFooter,
                        footerHeight = SocialDoorRowHeight
                    )
                }
            }
            if (isCommunityAdmin) {
                item { SettingsSectionHeading("Moderation") }
                item {
                    SettingsOptionCard {
                        SettingsOptionRow(
                            icon = CurioIcons.Warning,
                            title = "Moderation",
                            subtitle = "Work the reports, and manage the team",
                            onClick = { navController.navigate(CurioRoutes.MODERATION) }
                        )
                    }
                }
            }
            // A hidden member is told WHY, in their own words: the ban lives on
            // their profile row, so the app can explain it instead of letting
            // posting fail with a raw server error.
            // A ban, explained in the TIER's own words. The member is told
            // exactly how far it reaches and — when it is on a clock — when it
            // lifts itself, instead of being left to guess at a raw refusal.
            moderation?.takeIf { it.banned }?.let { status ->
                item(key = "hidden-notice") {
                    SettingsOptionCard {
                        SettingsOptionInfoRow(
                            if (status.locked) CurioIcons.Lock else CurioIcons.VisibilityOff,
                            when {
                                status.locked -> "This account is banned"
                                status.readOnly -> "Your account is read-only"
                                status.socialOnly -> "Friends and messages are paused"
                                else -> "Your account is hidden"
                            },
                            buildString {
                                append(status.reason ?: "A moderator acted on this account.")
                                append(' ')
                                append(
                                    when {
                                        status.locked ->
                                            "No post, reply, reaction, message or edit is accepted " +
                                                "while it stands — reach out if you think this is a mistake."
                                        status.readOnly ->
                                            "You can still read the wall, but posting, replying, " +
                                                "reacting, friends and messages are paused meanwhile."
                                        status.socialOnly ->
                                            "The wall works as usual; friends, requests and messages " +
                                                "are paused meanwhile."
                                        else ->
                                            "Your posts and replies are hidden and you cannot post " +
                                                "meanwhile — reach out if you think this is a mistake."
                                    }
                                )
                                status.untilMillis?.let { until ->
                                    append(" It lifts by itself on ")
                                    append(
                                        java.time.Instant.ofEpochMilli(until)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm"))
                                    )
                                    append('.')
                                }
                            }
                        )
                    }
                }
            }
            if (!eligible) {
                item { SettingsSectionHeading("Before you look") }
                item {
                    SettingsOptionCard {
                        when {
                            !OnlineAccount.configured -> SettingsOptionInfoRow(
                                CurioIcons.Warning,
                                "Not set up in this build",
                                "This build has no Curio online project, so there is no social wall to load."
                            )
                            !account.signedIn -> {
                                SettingsOptionInfoRow(
                                    CurioIcons.Info,
                                    "Sign in to see the social wall",
                                    "Cards are only shown to accounts with Online mode on."
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
                                    "Nothing here loads while Online mode is off — nothing of yours is shared either."
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
            } else {
                item { SettingsSectionHeading("Last 24 hours") }

                // The wall's one filter: EVERYTHING, or only the members you
                // follow. A follow is how a member's posts survive the wall's
                // 24-hour churn at a glance, so the chip is the filter's whole
                // interface — one tap on, one tap off.
                if (followingIds.isNotEmpty()) {
                    item(key = "follow-filter") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = wideContentEdgePadding())
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (followingOnly) settingsRoseAccent()
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { followingOnly = !followingOnly }
                            ) {
                                Text(
                                    text = if (followingOnly) "Following" else "Everyone",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    // v412 — the ink ASKS the chip fill (see
                                    // [curioFillInk]): the active filter wears the
                                    // accent, which in dark mode is the bright pale
                                    // primary white vanished on.
                                    color = if (followingOnly) curioFillInk(settingsRoseAccent())
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
                if (offlineCopy) {
                    // Honest, quiet, and only when it is true: the wall below is
                    // the device's own copy because the request failed.
                    item(key = "offline-copy") {
                        SocialNote(
                            "Offline — showing the wall as you last saw it.",
                            false
                        )
                    }
                }
                // v386 — the doors row (Chats / Friends / You) is gone from
                // here: it rides the hero itself now, so it is on screen the
                // moment the wall opens instead of after the account check.

                notice?.let { message ->
                    item {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                error?.let { message ->
                    item {
                        // Quiet on-surface ink — a red slab atop the wall read
                        // as a tester build's log, not a sentence.
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // The list actually shown: the whole live wall, or only the
                // followed members' slice of it. The filter is applied HERE
                // (not on the fetch) so a fresh post by a followed member
                // still lands through the same feed read.
                val visibleCards = if (followingOnly) {
                    cards.filter { it.authorId in followingIds }
                } else cards

                if (visibleCards.isEmpty() && !loading && error == null && answered) {
                    item {
                        SettingsOptionCard {
                            SettingsOptionInfoRow(
                                CurioIcons.Info,
                                if (followingOnly) "Nothing from who you follow" else "Nothing here yet",
                                if (followingOnly) {
                                    "Nobody you followed has posted in the last 24 hours."
                                } else {
                                    "Cards only last a day — be the first to pin one up."
                                }
                            )
                        }
                    }
                }

                items(visibleCards, key = { it.id }) { card ->
                    // Each row rises into place as the feed builds, so a
                    // refresh reads as cards arriving rather than a page
                    // blinking in.
                    Box(Modifier.animateItem()) {
                        CommunityCardItem(
                            card = card,
                            onOpen = { navController.navigate(CurioRoutes.communityCard(card.id)) },
                            onAuthor = {
                                if (card.authorId.isNotBlank()) {
                                    navController.navigate(CurioRoutes.socialProfile(card.authorId)) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onComments = { commentsFor = card },
                            // The pill answers the tap NOW and the server is told
                            // afterwards: a like used to wait for a round trip AND
                            // a full feed rebuild before the count moved, which is
                            // exactly the "slow" the wall was feeling. A rejected
                            // call resyncs the wall from the server instead of
                            // leaving the tap on screen as a lie.
                            onLike = {
                                val active = token ?: return@CommunityCardItem
                                val userId = account.session?.userId ?: return@CommunityCardItem
                                val liking = !card.likedByMe
                                patchCard(card.id) { it.toggleLike() }
                                scope.launch {
                                    val call = if (liking) {
                                        CommunityApi.like(active, card.id, userId)
                                    } else {
                                        CommunityApi.unlike(active, card.id, userId)
                                    }
                                    call.fold(
                                        onSuccess = { SocialFeedCache.write(context, cards) },
                                        onFailure = { failure ->
                                            error = failure.message
                                            refreshQuietly()
                                        }
                                    )
                                }
                            },
                            onDislike = {
                                val active = token ?: return@CommunityCardItem
                                val userId = account.session?.userId ?: return@CommunityCardItem
                                val disliking = !card.dislikedByMe
                                patchCard(card.id) { it.toggleDislike() }
                                scope.launch {
                                    val call = if (disliking) {
                                        CommunityApi.dislike(active, card.id, userId)
                                    } else {
                                        CommunityApi.undislike(active, card.id, userId)
                                    }
                                    call.fold(
                                        onSuccess = { SocialFeedCache.write(context, cards) },
                                        onFailure = { failure ->
                                            error = failure.message
                                            refreshQuietly()
                                        }
                                    )
                                }
                            },
                            onReport = { reporting = card },
                            onDelete = { deleteTarget = card }
                        )
                    }
                }
            }
        }
    }

        // The one way to post: a floating pill above the wall, labelled with
        // what it actually does (sharing a TOPIC as a card). It clears the
        // floating nav bar when this page is a tab root, and it leaves while
        // the wall is scrolling down.
        if (eligible && canWriteWall) {
            AnimatedVisibility(
                visible = postVisible,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = wideContentEdgePadding(),
                        bottom = 20.dp + if (asTab) {
                            84.dp + WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding()
                        } else 0.dp
                    )
            ) {
            ExtendedFloatingActionButton(
                onClick = { composing = true },
                containerColor = curioDialogActionColor(),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(50)
            ) {
                CurioIcon(
                    name = CurioIcons.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    size = 18.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Post",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            }
        }

        if (!wide) {
            SettingsHeroHeader(
                title = "Social",
                subtitle = "",
                onBack = if (asTab) null else ({ navController.popBackStack() }),
                footer = doorsFooter,
                footerHeight = SocialDoorRowHeight,
                glassBackdrop = glassBackdrop
            )
        }
    }

    if (composing && token != null) {
        CommunityPostScreen(
            onDismiss = { composing = false },
            onPost = { draft, repostOf, draftKey ->
                scope.launch {
                    CommunityApi.post(
                        token,
                        draft,
                        AppPreferences.getDisplayName(context),
                        account.session?.userId
                    ).fold(
                        onSuccess = { posted ->
                            composing = false
                            notice = if (repostOf != null) "Posted again." else "Posted."
                            // The composer's own memory follows the post: the
                            // draft is finished with, and a kept post that has
                            // gone back up leaves the local archive (it is on
                            // the wall again — keeping it would be a duplicate
                            // waiting to happen).
                            //
                            // v389e — CLEARED BY THE COMPOSER'S OWN KIND, which
                            // is the key those words were kept under. Clearing by
                            // the POSTED kind instead missed the entry whenever a
                            // topic was presented as a note (a note post written
                            // in the card composer), so the note and its topic
                            // were still there on the next open. A re-post passes
                            // no key: the member's own draft was not what went up.
                            withContext(Dispatchers.IO) {
                                if (draftKey.isNotBlank()) {
                                    SocialPostArchive.clearDraft(context, draftKey)
                                }
                                repostOf?.let { SocialPostArchive.forgetDeleted(context, it) }
                            }
                            // On the wall before the sheet is even gone, in
                            // this device's own name and portrait: the row the
                            // server stored IS the card. The rest of the feed
                            // is reconciled quietly behind it, so nobody waits
                            // on a full-page read to see their own post.
                            val shown = posted.copy(
                                authorDisplayName = AppPreferences.getDisplayName(context),
                                authorName = AppPreferences.getUsername(context),
                                authorAvatar = AppPreferences.getSocialAvatarStyle(context)
                            )
                            cards = listOf(shown) + cards.filterNot { it.id == shown.id }
                            SocialFeedCache.write(context, cards)
                            refreshQuietly()
                        },
                        onFailure = { error = it.message }
                    )
                }
            }
        )
    }

    commentsFor?.let { open ->
        token?.let { active ->
            CommunityCommentsSheet(
                card = open,
                accessToken = active,
                myUserId = account.session?.userId,
                canModerateReplies = canModerateReplies,
                onDismiss = { commentsFor = null },
                onChanged = { scope.launch { refreshQuietly() } },
                onOpenProfile = { id ->
                    navController.navigate(CurioRoutes.socialProfile(id)) { launchSingleTop = true }
                }
            )
        }
    }

    reporting?.let { card ->
        ReportTargetDialog(
            title = "Report this post",
            subtitle = "Reports go to the moderation team, with your reason.",
            reasons = CommunityReportReasons.CONTENT,
            onDismiss = { reporting = null },
            onReport = { reason, note ->
                val active = token ?: return@ReportTargetDialog
                scope.launch {
                    CommunityApi.report(active, "card", card.id, reason, note).fold(
                        onSuccess = {
                            reporting = null
                            notice = "Thanks — we'll take a look."
                        },
                        onFailure = { error = it.message }
                    )
                }
            }
        )
    }

    deleteTarget?.let { card ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete post?") },
            text = {
                Text(
                    "It leaves the community wall right away. A copy is kept on this phone, so " +
                        "you can post it again from the composer if you change your mind."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val active = token ?: return@TextButton
                    deleteTarget = null
                    scope.launch {
                        CommunityApi.delete(active, card.id).fold(
                            onSuccess = {
                                // Deleting a post used to be final the instant the
                                // server was told. The words are the member's
                                // own, so a copy stays on this phone — never
                                // re-uploaded unless they ask — and the composer
                                // offers it back at the foot of the page.
                                withContext(Dispatchers.IO) {
                                    SocialPostArchive.rememberDeleted(
                                        context,
                                        SocialPostArchive.DeletedPost(
                                            id = card.id,
                                            title = "",
                                            draft = SocialPostArchive.draftOf(card),
                                            postedAtMillis = card.createdAtMillis,
                                            deletedAtMillis = System.currentTimeMillis()
                                        )
                                    )
                                }
                                notice = "Taken down — it is kept on this phone if you change your mind."
                                load()
                            },
                            onFailure = { error = it.message }
                        )
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

/**
 * A community card drawn at whatever width it is given.
 *
 * The REAL share card is rendered (never a simplified lookalike), laid out
 * DIRECTLY at the width its row offers — the same way the card editor draws
 * its own preview (a 280dp base). The card's own layout and smart fit size the
 * title, the fact box and the text for the size they are handed, so a post
 * shows the WHOLE card, crisp: nothing is scaled as a layer, nothing is
 * cropped, and no row reserves height the art does not use.
 */
@Composable
internal fun CommunityCardCanvas(
    card: CommunityCard,
    modifier: Modifier = Modifier,
    /**
     * How much of the available width the card claims, centred.
     *
     * The wall claims less than the full width: a share card is a tall poster,
     * and one drawn edge to edge stops reading as a post among others. The
     * card's own view passes 1f and fills the page it owns.
     */
    widthFraction: Float = 1f
) {
    val style = runCatching { ShareCardStyle.valueOf(card.style) }
        .getOrDefault(ShareCardStyle.PAPER)
    val aspect = runCatching { ShareCardAspect.valueOf(card.aspect) }
        .getOrDefault(ShareCardAspect.CLASSIC)
    val accent = remember(card.accentHex) { parseAccent(card.accentHex) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(widthFraction.coerceIn(0.2f, 1f)),
            contentAlignment = Alignment.Center
        ) {
            // The card keeps its own aspect ratio and is capped at its design
            // width, so a wide window shows it at its natural size instead of
            // inflating the art.
            val targetWidth = minOf(maxWidth, aspect.widthDp.dp)
            TopicShareCard(
                topicName = card.topicName,
                categoryName = card.categoryName,
                categoryGlyph = card.categoryGlyph,
                accent = accent,
                factText = card.factText,
                // The card wears the author's CURRENT username, so renaming
                // yourself updates everything you ever posted.
                sharerName = card.authorLabel,
                aspect = aspect,
                style = style,
                byline = card.byline,
                bodyScale = card.bodyScale,
                modifier = Modifier
                    .width(targetWidth)
                    .aspectRatio(aspect.widthDp.toFloat() / aspect.heightDp.toFloat())
            )
        }
    }
}

/**
 * One card in the wall — a proper BOX card now: the author's portrait and live
 * username at the top (tap to open their profile), the caption, the card art
 * at a fraction of the width so more of the wall fits on screen, and the
 * actions as pills underneath.
 */
@Composable
private fun CommunityCardItem(
    card: CommunityCard,
    onOpen: () -> Unit,
    onAuthor: () -> Unit,
  onComments: () -> Unit,
  onLike: () -> Unit,
  onDislike: () -> Unit,
  onReport: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        // The ACCENT's own tint, never the plain cream container: the wall is
        // the community's surface, and it should wear the app's colour the way
        // every other surface does.
        color = communityCardFill(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ── Who posted it ────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAuthor)
                    .padding(vertical = 2.dp)
            ) {
                SocialAvatar(style = card.authorAvatar, avatarSize = 36.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    // The DISPLAY name leads and the @username rides the line
                    // beneath it, beside the card's age: a name and a handle
                    // are two different things, and a card wears both.
                    Text(
                        text = card.authorLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = if (card.hoursLeft <= 0L) {
                            "${card.authorHandleLabel} · expiring now"
                        } else {
                            "${card.authorHandleLabel} · ${card.hoursLeft}h left"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                CurioIcon(
                    name = CurioIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 18.dp
                )
            }

            if (card.caption.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = card.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            if (card.kind == KIND_CARD) {
                CommunityCardCanvas(
                    card = card,
                    modifier = Modifier
                        .clipToBounds()
                        .clickable(onClick = onOpen),
                    widthFraction = FEED_CARD_WIDTH
                )
            } else {
                // A NOTE or a QUOTE has no topic and no card art: the words ARE
                // the post, so they get the room the art would have taken.
                SocialTextPost(card = card, onClick = onOpen)
            }

            // The art and its actions are ONE object: a tight seam keeps the
            // like/dislike row attached to the card instead of floating in an
            // empty band under it.
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CommunityAction(
                    glyph = CurioIcons.ThumbUp,
                    label = if (card.likeCount > 0) card.likeCount.toString() else "",
                    tinted = card.likedByMe,
                    animate = true,
                    onClick = onLike
                )
                CommunityAction(
                    glyph = CurioIcons.ThumbDown,
                    label = if (card.dislikeCount > 0) card.dislikeCount.toString() else "",
                    tinted = card.dislikedByMe,
                    animate = true,
                    onClick = onDislike
                )
                CommunityAction(
                    glyph = CurioIcons.FormatQuote,
                    label = if (card.commentCount > 0) card.commentCount.toString() else "",
                    tinted = false,
                    onClick = onComments
                )
                Spacer(Modifier.weight(1f))
                // The rare actions are ICONS, not words: a take-down or a
                // report is a decision, not a reading task, and two worded
                // pills crowded the row's tail. The icon keeps its label for
                // accessibility, so the tap target never loses its meaning.
                if (card.mine) {
                    CommunityAction(CurioIcons.Delete, "", false, onDelete)
                }
                CommunityAction(CurioIcons.Flag, "", false, onReport)
            }
        }
    }
}

/**
 * The fill one WALL card wears.
 *
 * The wall's rows used to sit on the plain `surfaceContainerLow` — the app's
 * warm cream — which made the community's own surface the only one in the app
 * that ignored the accent entirely. The accent now tints the container: a
 * whisper in light mode (the cream is still the base, so text contrast is
 * untouched), a little more in the dark, where the surface needs the lift to
 * read as a card at all.
 *
 * The SAME fill is used by every community surface that draws a panel, so the
 * wall, a post's page and a profile's cards stay one family.
 */
@Composable
internal fun communityCardFill(): Color {
    val accent = settingsRoseAccent()
    val base = MaterialTheme.colorScheme.surfaceContainerLow
    return if (isCurioDarkTheme()) lerp(base, accent, 0.16f) else lerp(base, accent, 0.10f)
}

/**
 * How much of the wall's width one card claims.
 *
 * A card is drawn just under the editor's own 280dp base, so the wall and the
 * card editor agree on what a post looks like, and the row's box keeps a small
 * even gutter around the art instead of framing a full-bleed poster.
 */
private const val FEED_CARD_WIDTH = 0.78f

/**
 * One action on a card, as a PILL: an icon and its count on one rounded
 * surface, accent-filled while it is the state you are in (a like you left),
 * quiet when it is a door (reply, share, report). TextButton's bare label was
 * the "bad" action row — a pill reads as one touchable thing, and the press
 * squish is the feedback that says the tap landed.
 */
@Composable
internal fun CommunityAction(
    glyph: String,
    label: String,
    tinted: Boolean,
    onClick: () -> Unit,
    /** When true, a state change (a like landing) pops the icon. */
    animate: Boolean = false
) {
    val ink = if (tinted) curioDialogActionColor() else MaterialTheme.colorScheme.onSurfaceVariant
    // The POP: whenever `tinted` flips (a like or dislike landing), the icon
    // springs past its resting size and settles — the tactile answer to "did
    // that count?". Driven by one Animatable so repeats restart cleanly.
    val pop = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(tinted) {
        if (animate) {
            pop.snapTo(1.35f)
            pop.animateTo(
                1f,
                androidx.compose.animation.core.spring(
                    dampingRatio = 0.45f,
                    stiffness = 700f
                )
            )
        }
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = if (tinted) {
            curioDialogActionColor().copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
        },
        modifier = Modifier
            .graphicsLayer(scaleX = pop.value, scaleY = pop.value)
            .curioPressClickable(
                pressedScale = 0.94f,
                hapticOnPress = false,
                onClickLabel = label.ifBlank { glyph },
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (label.isBlank()) 0.dp else 5.dp),
            // An icon-only action is a SQUARE tap target, not a squat pill:
            // equal padding all round keeps it thumb-sized and calm.
            modifier = Modifier.padding(
                horizontal = if (label.isBlank()) 8.dp else 10.dp,
                vertical = 7.dp
            )
        ) {
            CurioIcon(
                name = glyph,
                contentDescription = label.ifBlank { null }
                    ?: when (glyph) {
                        CurioIcons.Delete -> "Take down this card"
                        CurioIcons.Flag -> "Report this card"
                        else -> "Community action"
                    },
                tint = ink,
                size = 18.dp
            )
            if (label.isNotBlank()) Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (tinted) FontWeight.Bold else FontWeight.Medium
                ),
                color = ink                    )
        }
    }
}

/**
 * v386 — the height the Social hero's doors row ADDS to the banner: the row's
 * own height plus the gap the header leaves above it. Passed to
 * [SettingsHeroHeader] as `footerHeight` and to [settingsHeroTotalHeight] as
 * the reservation, so the wall always starts below the extended banner. Keep
 * it in step with [SocialDoorsRow]'s natural height (a 31dp icon well plus 7dp
 * of padding either side = 45dp, plus the 12dp gap, plus a little slack).
 */
private val SocialDoorRowHeight = 58.dp

/**
 * The wall's three doors (Chats / Friends / You) as ONE row for the hero
 * banner: an icon in a rounded well beside the label, no narration under it.
 *
 * On the banner by design — as a list item under “Last 24 hours” the row only
 * appeared once the account check landed, so it was missing every time the
 * wall opened. Here it paints with the header itself.
 */
@Composable
private fun SocialDoorsRow(
    ink: Color,
    onChats: () -> Unit,
    onFriends: () -> Unit,
    onYou: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CommunityDoorTile(
            icon = CurioIcons.Chats,
            label = "Chats",
            ink = ink,
            onClick = onChats,
            modifier = Modifier.weight(1f)
        )
        CommunityDoorTile(
            // Groups: two people side by side — the mark a friends list
            // deserves; the chats tile takes the speech bubble so the two
            // never blur.
            icon = CurioIcons.Friends,
            label = "Friends",
            ink = ink,
            onClick = onFriends,
            modifier = Modifier.weight(1f)
        )
        CommunityDoorTile(
            icon = CurioIcons.Person,
            label = "You",
            ink = ink,
            onClick = onYou,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * One of the wall's three doors as a compact TILE: an icon in a rounded well
 * beside the label, wearing the hero's readable [ink] on the hero's own pill
 * glass so it reads as part of the torn paper. The press squish every other
 * surface on this screen wears; equal weights keep the row balanced on any
 * width.
 */
@Composable
internal fun CommunityDoorTile(
    icon: String,
    label: String,
    ink: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        // v27n — OPAQUE hero glass: a translucent fill lets the elevation
        // shadow bleed through the tile as a blurry smudge.
        color = settingsHeroPillFill(),
        shadowElevation = 3.dp,
        modifier = modifier
            .curioDarkGlow(3.dp, RoundedCornerShape(18.dp))
            .curioPressClickable(
                pressedScale = 0.96f,
                hapticOnPress = false,
                onClickLabel = label,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 7.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ink.copy(alpha = 0.14f)
            ) {
                CurioIcon(
                    name = icon,
                    contentDescription = null,
                    tint = ink,
                    size = 17.dp,
                    modifier = Modifier.padding(7.dp)
                )
            }
            Text(
                text = label,
                // labelMedium: three tiles share one phone's width and
                // “Friends” must not ellipsize on a 320dp screen — labelLarge
                // was already tight before the row moved onto the banner.
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = ink
            )
        }
    }
}

internal fun parseAccent(hex: String): Color =
    runCatching { Color(0xFF000000 or hex.removePrefix("#").toLong(16)) }
        .getOrDefault(Color(0xFF8E8E93))
