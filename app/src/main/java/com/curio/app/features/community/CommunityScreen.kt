package com.curio.app.features.community

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioContentFilter
import com.curio.app.data.CurioTopic
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityCardDraft
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.KIND_CARD
import com.curio.app.data.supabase.KIND_NOTE
import com.curio.app.data.supabase.KIND_QUOTE
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionInfoRow
import com.curio.app.features.settings.SettingsOptionRow
import com.curio.app.features.settings.SettingsSectionHeading
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.components.curioPressClickable
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ShareCardAspect
import com.curio.app.ui.components.ShareCardStyle
import com.curio.app.ui.components.TopicShareCard
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.data.supabase.RealtimeWatch
import com.curio.app.data.supabase.SupabaseRealtime
import com.curio.app.ui.theme.curioDialogContainerColor
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var cards by remember { mutableStateOf<List<CommunityCard>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
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
    var commentsFor by remember { mutableStateOf<CommunityCard?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // A server push bumps this; the wall then refreshes QUIETLY (no spinner, no
    // scroll reset, no offline flag) so a post landing while you read is simply
    // there the next time you look up.
    var pushed by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    val eligible = account.signedIn && onlineMode && token != null

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

    LaunchedEffect(eligible, token) {
        if (eligible) {
            // The device's copy FIRST — an offline open is a wall, not a blank
            // screen — then the server's answer replaces it in place.
            val cached = SocialFeedCache.read(context)
            if (cards.isEmpty()) cards = cached
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
                top = if (wide) 0.dp else SettingsHeroTotalHeight,
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
                        subtitle = "Text cards — gone in 24 hours",
                        onBack = if (asTab) null else ({ navController.popBackStack() })
                    )
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
                item {
                    // The wall's doors. Posting lives on the floating button
                    // at the bottom (one clear door); these are the two places
                    // you GO from the wall, each as a proper tile — icon first,
                    // label under it — instead of three bare text buttons that
                    // read as leftover links.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CommunityDoorTile(
                            icon = CurioIcons.Chats,
                            label = "Chats",
                            onClick = { navController.navigate(CurioRoutes.CHATS) },
                            modifier = Modifier.weight(1f)
                        )
                        CommunityDoorTile(
                            // Groups: two people side by side — the mark a
                            // friends list deserves; the chats tile takes the
                            // speech bubble so the two never blur.
                            icon = CurioIcons.Friends,
                            label = "Friends",
                            onClick = { navController.navigate(CurioRoutes.FRIENDS) },
                            modifier = Modifier.weight(1f)
                        )
                        CommunityDoorTile(
                            icon = CurioIcons.Person,
                            label = "You",
                            onClick = {
                                // Guarded: an unmatched person/ route would throw.
                                val me = account.session?.userId
                                if (!me.isNullOrBlank()) {
                                    navController.navigate(CurioRoutes.socialProfile(me)) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

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

                if (cards.isEmpty() && !loading && error == null) {
                    item {
                        SettingsOptionCard {
                            SettingsOptionInfoRow(
                                CurioIcons.Info,
                                "Nothing here yet",
                                "Cards only last a day — be the first to pin one up."
                            )
                        }
                    }
                }

                items(cards, key = { it.id }) { card ->
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
                            onDelete = {
                                val active = token ?: return@CommunityCardItem
                                scope.launch {
                                    CommunityApi.delete(active, card.id).fold(
                                        onSuccess = {
                                            notice = "Your card was taken down."
                                            load()
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

        // The one way to post: a floating pill above the wall, labelled with
        // what it actually does (sharing a TOPIC as a card). It clears the
        // floating nav bar when this page is a tab root.
        if (eligible) {
            ExtendedFloatingActionButton(
                onClick = { composing = true },
                containerColor = curioDialogActionColor(),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(50),
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

        if (!wide) {
            SettingsHeroHeader(
                title = "Social",
                subtitle = "Text cards — gone in 24 hours",
                onBack = if (asTab) null else ({ navController.popBackStack() }),
                glassBackdrop = glassBackdrop
            )
        }
    }

    if (composing && token != null) {
        CommunityComposerSheet(
            onDismiss = { composing = false },
            onPost = { draft ->
                scope.launch {
                    CommunityApi.post(
                        token,
                        draft,
                        AppPreferences.getDisplayName(context),
                        account.session?.userId
                    ).fold(
                        onSuccess = { posted ->
                            composing = false
                            notice = "Posted — it disappears in 24 hours."
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
                onDismiss = { commentsFor = null },
                onChanged = { scope.launch { refreshQuietly() } },
                onOpenProfile = { id ->
                    navController.navigate(CurioRoutes.socialProfile(id)) { launchSingleTop = true }
                }
            )
        }
    }

    reporting?.let { card ->
        ReportCardDialog(
            onDismiss = { reporting = null },
            onReport = { reason ->
                val active = token ?: return@ReportCardDialog
                scope.launch {
                    CommunityApi.report(active, card.id, reason).fold(
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
}

/**
 * A community card drawn at whatever width it is given.
 *
 * The REAL share card is rendered (never a simplified lookalike), scaled as a
 * LAYER: its own geometry is 405×720 / 450×600 dp, so scaling keeps the
 * internal layout and text wrapping identical to the exported image while the
 * feed and the card view can show it at their own width.
 */
@Composable
internal fun CommunityCardCanvas(
    card: CommunityCard,
    modifier: Modifier = Modifier,
    /**
     * How much of the available width the card claims, centred.
     *
     * The wall uses less than the full width: a share card is a TALL portrait
     * (405×720dp), so at full width one card fills the whole screen and the
     * feed stops feeling like a feed. The card's own view passes 1f and shows
     * it whole.
     *
     * A caller that passes **0f** asks for the FILL mode used by the profile
     * grid: the scale is driven by the available HEIGHT instead of the width,
     * so the card is cropped to whatever box it is given (top-aligned, where
     * its title lives) rather than floating small inside that box.
     */
    widthFraction: Float = 1f
) {
    val style = runCatching { ShareCardStyle.valueOf(card.style) }
        .getOrDefault(ShareCardStyle.PAPER)
    val aspect = runCatching { ShareCardAspect.valueOf(card.aspect) }
        .getOrDefault(ShareCardAspect.CLASSIC)
    val cardWidth = aspect.widthDp.dp
    val cardHeight = aspect.heightDp.dp
    val accent = remember(card.accentHex) { parseAccent(card.accentHex) }
    val fillHeight = widthFraction == 0f

    Box(
        modifier = modifier
            .then(if (fillHeight) Modifier else Modifier.fillMaxWidth()),
        // Fill mode crops the card to the box: the overflow hangs off the
        // BOTTOM (TopStart), so the card's title always stays visible.
        contentAlignment = if (fillHeight) Alignment.TopStart else Alignment.Center
    ) {
    BoxWithConstraints(
        modifier = if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth(widthFraction),
        // The painted footprint is CENTERED in the room it has: a card whose
        // scale clamps (a wide phone where the item outgrows the 405dp card)
        // must never hug the left edge of its row, and a fill-mode crop keeps
        // its overflow symmetric about the vertical axis.
        contentAlignment = if (fillHeight) Alignment.TopCenter else Alignment.Center
    ) {
        val density = LocalDensity.current
        val scale = with(density) {
            if (fillHeight) {
                // COVER semantics: the larger of the two ratios, so the card
                // overflows the box in ONE direction only and the box is
                // completely filled — a share card in a square tile is a
                // cropped preview, never a letterboxed miniature.
                maxOf(
                    maxWidth.toPx() / cardWidth.toPx(),
                    maxHeight.toPx() / cardHeight.toPx()
                )
            } else {
                (maxWidth.toPx() / cardWidth.toPx()).coerceAtMost(1f)
            }
        }
        // The PAINTED FOOTPRINT leads: a box exactly the size the scaled card
        // will cover, which the parent centers. Inside it the UNSCALED card
        // draws through the layer transform, so the visible art always sits
        // where the footprint says — never pinned to a corner by the layout
        // box outliving the scale.
        Box(
            modifier = Modifier
                .requiredSize(cardWidth * scale, cardHeight * scale),
            contentAlignment = Alignment.TopStart
        ) {
            Box(
                modifier = Modifier
                    // REQUIRED size, not `size`: a measured size is coerced
                    // into the incoming constraints, and inside a square tile
                    // that coerced box was then scaled DOWN again — the card
                    // shrank to roughly a quarter of the tile. requiredSize
                    // keeps the nominal card geometry, the graphicsLayer does
                    // the shrinking, and the footprint above clips the rest.
                    .requiredSize(cardWidth, cardHeight)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )
            ) {
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
                    bodyScale = card.bodyScale
                )
            }
        }
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
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                Text(
                    text = card.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }

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
 * How much of the wall's width one card claims.
 *
 * FULL width: the card IS the post, and a share card shrunk to three quarters
 * of the page made every post read like a thumbnail. The list's own edge
 * padding is the only gutter a card needs.
 */
private const val FEED_CARD_WIDTH = 1f

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
 * One of the wall's three doors — Chats, Friends, You — as a compact TILE:
 * an icon in a rounded well beside the label, no narration under it. The
 * press squish every other surface on this screen wears. Equal weights keep
 * the row balanced on any width.
 */
@Composable
internal fun CommunityDoorTile(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.curioPressClickable(
            pressedScale = 0.96f,
            hapticOnPress = false,
            onClickLabel = label,
            onClick = onClick
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = curioDialogActionColor().copy(alpha = 0.14f)
            ) {
                CurioIcon(
                    name = icon,
                    contentDescription = null,
                    tint = curioDialogActionColor(),
                    size = 18.dp,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * The composer: a WRITE-FIRST canvas, then the wall.
 *
 * The sheet opens on a borderless writer (a Note, the fastest post) with the
 * live post rendering above it as it is typed — there is no separate preview
 * step, because the preview IS the page. A quiet kind switch (Note / Topic /
 * Quote) recolors the canvas in place; everything that attaches to a post
 * (topic, style, shape, credit) lives in compact pill rows that only appear
 * when they mean something, so the surface never reads like a form.
 *
 * Three kinds, one flow. A **Card** is a topic being passed on — the topic is
 * chosen from the app's own catalog rather than typed, and its own quick fact
 * seeds the words, so a card can never disagree with the topic it is about. A
 * **Note** is a tweet-style text post: words, no topic, no art. A **Quote** is
 * a line someone else said, credited on the card.
 *
 * Searching covers every lane through the loader's lightweight index (the same
 * one the Topic Database searches), with the warm lane pools as the fallback on
 * a cold install.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommunityComposerSheet(
    onDismiss: () -> Unit,
    onPost: (CommunityCardDraft) -> Unit,
    /**
     * What this composer opens AS. The wall's floating button opens a NOTE
     * (the fastest post, the new default); the reveal page's note/quote doors
     * open straight into their own kind, which keeps those flows out of the
     * topic picker.
     */
    seedKind: String = KIND_NOTE,
    seedTopic: CurioTopic? = null,
    seedFact: String = "",
    seedCredit: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // CARD / NOTE / QUOTE — what is being posted. Note is the default: the
    // composer is first a place to write, and a thought that is NOT about a
    // topic never has to borrow one.
    var kind by remember { mutableStateOf(seedKind) }
    var picked by remember { mutableStateOf(seedTopic) }
    var query by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var fact by remember { mutableStateOf(seedFact) }
    // Who said it — the QUOTE's credit, stored in the card's byline.
    var credit by remember { mutableStateOf(seedCredit) }
    var style by remember { mutableStateOf(ShareCardStyle.PAPER) }
    // The card's shape (story 9:16 or classic 3:4), toggled from the pills.
    var aspect by remember { mutableStateOf(ShareCardAspect.PORTRAIT) }
    // The inline tool rows: which pill is expanded (null = none). One at a
    // time, so the sheet stays a canvas rather than growing a settings page.
    var openTool by remember { mutableStateOf<String?>(null) }
    // The whole-catalog index, loaded once. It is the prebuilt lightweight
    // index (name/byline keys only), so searching never parses a lane.
    var index by remember { mutableStateOf<List<com.curio.app.data.TopicIndexEntry>?>(null) }
    LaunchedEffect(Unit) { index = TopicJsonLoader.loadIndex() }

    // The writer takes focus as the sheet settles — write-first means the
    // keyboard is already up and the placeholder is already blinking.
    val writerFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { writerFocus.requestFocus() }

    val q = query.trim()
    val results: List<CurioTopic> = remember(index, q) {
        if (q.length < 2) emptyList()
        else {
            val pool = index?.map { it.topic }.orEmpty().ifEmpty {
                // No index asset (a very cold install): fall back to whatever
                // lane pools are already warm.
                CurioCategories.all.filter { !it.isHidden }
                    .flatMap { TopicJsonLoader.cached(it.id).orEmpty() }
            }
            pool.filter { topic ->
                topic.name.contains(q, ignoreCase = true) ||
                    topic.byline?.contains(q, ignoreCase = true) == true
            }
                .distinctBy { "${it.categoryId.name}|${it.name}" }
                .sortedBy { it.name.lowercase() }
                .take(30)
        }
    }
    val lane = picked?.let { CurioCategories.byId(it.categoryId) }
    val styles = remember {
        listOf(
            ShareCardStyle.PAPER,
            ShareCardStyle.MINIMAL,
            ShareCardStyle.EDITORIAL,
            ShareCardStyle.COLLAGE,
            ShareCardStyle.VINYL,
            ShareCardStyle.SIGNATURE
        )
    }
    val topicCard = kind == KIND_CARD
    val draft = CommunityCardDraft(
        kind = kind,
        // A note and a quote have NO topic by design — the card renderer is
        // not involved at all for them, so there is nothing to disagree with.
        topicName = if (topicCard) picked?.name.orEmpty() else "",
        categoryName = if (topicCard) lane?.displayName.orEmpty() else "",
        categorySlug = if (topicCard) lane?.id?.name?.lowercase().orEmpty() else "",
        categoryGlyph = if (topicCard) lane?.iconGlyph.orEmpty() else "",
        accentHex = if (topicCard) lane?.let { hexOf(it.accent) }.orEmpty() else "",
        factText = fact,
        caption = caption,
        byline = if (kind == KIND_QUOTE) credit.trim() else ""
    )
    // What the live canvas draws: a CommunityCard rebuilt from the draft,
    // wearing its chosen shape — the same object the wall will render.
    val previewCard = CommunityCard(
        id = "preview",
        authorId = "",
        authorHandle = "",
        kind = draft.kind,
        topicName = draft.topicName,
        categoryName = draft.categoryName,
        categoryGlyph = draft.categoryGlyph,
        accentHex = draft.accentHex,
        factText = draft.factText,
        caption = draft.caption,
        style = if (topicCard) style.name else ShareCardStyle.PAPER.name,
        aspect = if (topicCard) aspect.name else ShareCardAspect.CLASSIC.name,
        byline = draft.byline,
        createdAtMillis = System.currentTimeMillis(),
        expiresAtMillis = System.currentTimeMillis() + 24L * 3_600_000L,
        bodyScale = draft.bodyScale,
        likeCount = 0,
        likedByMe = false,
        dislikeCount = 0,
        dislikedByMe = false,
        commentCount = 0,
        mine = true
    )
    val problem = CommunityApi.draftProblem(draft)
        // v3xx53 — the app-level filter, shown on the sheet as you write (the
        // API refuses it again on the way out; the schema's CHECK is the third
        // gate for a modified client).
        ?: CurioContentFilter.problemIn(draft.factText, draft.caption, draft.byline)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp)
                .animateContentSize(
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.85f,
                        stiffness = 380f
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header: kind switch + Post ──────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The ghost kind switch: one rounded rail, the active kind a
                // quiet pill inside it. No labels explaining what a note is.
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(3.dp)
                    ) {
                        listOf(
                            KIND_NOTE to "Note",
                            KIND_CARD to "Topic",
                            KIND_QUOTE to "Quote"
                        ).forEach { (value, label) ->
                            val active = kind == value
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                                onClick = {
                                    kind = value
                                    // Switching away from a topic card drops
                                    // the topic: a note and a quote carry no
                                    // topic, and a stale one must not leak.
                                    if (value != KIND_CARD) picked = null
                                    openTool = null
                                }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (active) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                // The counter only speaks near the limit — a form counts every
                // keystroke; a canvas speaks up when the room runs out.
                val budget = if (kind == KIND_NOTE) CommunityApi.MAX_FACT_CHARS else CommunityApi.MAX_FACT_CHARS
                val used = fact.length
                if (used > budget * 4 / 5) {
                    Text(
                        text = "${CommunityApi.MAX_FACT_CHARS - used}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (used >= CommunityApi.MAX_FACT_CHARS) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                }
                // POST, top right where a send belongs. It wakes with the
                // draft: a scale pop the moment the post becomes valid.
                val ready = problem == null
                val postPop = remember { androidx.compose.animation.core.Animatable(if (ready) 1f else 0.92f) }
                LaunchedEffect(ready) {
                    postPop.snapTo(if (ready) 1.12f else 0.92f)
                    postPop.animateTo(
                        if (ready) 1f else 0.92f,
                        androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 600f)
                    )
                }
                Button(
                    onClick = {
                        // For a note or a quote the card style is irrelevant —
                        // the renderer is not involved — so it is only carried
                        // on a topic card.
                        onPost(
                            draft.copy(
                                style = if (topicCard) style.name else ShareCardStyle.PAPER.name
                            )
                        )
                    },
                    enabled = ready,
                    shape = RoundedCornerShape(50),
                    colors = curioDialogActionButtonColors(),
                    modifier = Modifier.graphicsLayer(
                        scaleX = postPop.value,
                        scaleY = postPop.value
                    )
                ) {
                    Text(
                        "Post",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // ── The live canvas: the post AS it will appear ─────────────
            // Crossfaded between kinds so switching Note / Topic / Quote
            // re-dresses the same page instead of jumping.
            Crossfade(targetState = topicCard, label = "composerCanvas") { asCard ->
                if (asCard) {
                    CommunityCardCanvas(
                        card = previewCard,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .fillMaxWidth(),
                        widthFraction = 1f
                    )
                } else {
                    SocialTextPost(card = previewCard, onClick = null)
                }
            }

            // ── The writer ──────────────────────────────────────────────
            // Borderless: the placeholder is the only chrome. This is the
            // surface the keyboard writes into, not a field on a form.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                if (topicCard && picked != null && caption.isEmpty()) {
                    // A topic card's caption line: small, optional, out of the
                    // way until used. It rides above the body writer.
                    BasicTextField(
                        value = caption,
                        onValueChange = {
                            if (it.length <= CommunityApi.MAX_CAPTION_CHARS) caption = it
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(curioDialogActionColor()),
                        decorationBox = { inner ->
                            Box {
                                if (caption.isEmpty()) Text(
                                    text = "Caption (optional)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                                inner()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }
                BasicTextField(
                    value = fact,
                    onValueChange = { if (it.length <= CommunityApi.MAX_FACT_CHARS) fact = it },
                    textStyle = when (kind) {
                        KIND_QUOTE -> MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        else -> MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(curioDialogActionColor()),
                    decorationBox = { inner ->
                        Box(modifier = Modifier.padding(vertical = 12.dp)) {
                            if (fact.isEmpty()) Text(
                                text = when (kind) {
                                    KIND_NOTE -> "What is catching your eye?"
                                    KIND_QUOTE -> "The line worth keeping"
                                    else -> "Say something about this topic"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                            inner()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(writerFocus)
                )
            }

            // ── The pill rows ───────────────────────────────────────────
            // Everything that attaches to the post, one pill per concern,
            // shown only when it means something. A pill opens a quiet inline
            // row beneath it; nothing ever grows into a labeled section.
            if (topicCard) {
                // TOPIC — attach or swap the topic the card passes on.
                if (picked == null) {
                    ComposerPill(
                        label = if (q.length >= 2) "Searching \"$q\""
                        else "Attach a topic",
                        icon = CurioIcons.Wildcard,
                        expanded = openTool == "topic",
                        onClick = { openTool = if (openTool == "topic") null else "topic" }
                    )
                } else {
                    ComposerPill(
                        label = picked?.name.orEmpty(),
                        icon = lane?.iconGlyph ?: CurioIcons.Wildcard,
                        iconTint = lane?.accent,
                        trailing = "Change",
                        expanded = false,
                        onClick = { openTool = "topic"; picked = null }
                    )
                }
                // The topic search lives INSIDE the pill's shadow: a quiet
                // rounded tray that grows under the pill while it is open.
                androidx.compose.animation.AnimatedVisibility(visible = openTool == "topic" && picked == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(curioDialogActionColor()),
                            decorationBox = { inner ->
                                Box {
                                    if (query.isEmpty()) Text(
                                        text = "Search the catalog — a book, a film, a dish",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    )
                                    inner()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        when {
                            q.length < 2 -> {}
                            results.isEmpty() -> Text(
                                text = "Nothing matches that. Try another spelling.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(results, key = { "${it.categoryId.name}|${it.name}" }) { topic ->
                                    TopicPickRow(
                                        topic = topic,
                                        onClick = {
                                            picked = topic
                                            query = ""
                                            openTool = null
                                            // The topic's OWN quick fact seeds
                                            // the card — a card is that topic
                                            // being passed on, and it stays
                                            // fully editable.
                                            if (fact.isBlank()) fact = quickFactOf(topic)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // STYLE + SHAPE — two compact pills, one row.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComposerPill(
                        label = style.label,
                        icon = CurioIcons.Notes,
                        expanded = openTool == "style",
                        onClick = { openTool = if (openTool == "style") null else "style" },
                        modifier = Modifier.weight(1f)
                    )
                    ComposerPill(
                        label = if (aspect == ShareCardAspect.PORTRAIT) "Story" else "Classic",
                        icon = CurioIcons.Wildcard,
                        expanded = openTool == "shape",
                        onClick = { openTool = if (openTool == "shape") null else "shape" },
                        modifier = Modifier.weight(1f)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(visible = openTool == "style") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
                            .padding(10.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        styles.forEach { option ->
                            FilterChip(
                                selected = option == style,
                                onClick = { style = option; openTool = null },
                                label = {
                                    Text(option.label, style = MaterialTheme.typography.labelSmall)
                                }
                            )
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(visible = openTool == "shape") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
                            .padding(10.dp)
                    ) {
                        listOf(ShareCardAspect.PORTRAIT, ShareCardAspect.CLASSIC).forEach { option ->
                            FilterChip(
                                selected = aspect == option,
                                onClick = { aspect = option; openTool = null },
                                label = {
                                    Text(
                                        if (option == ShareCardAspect.PORTRAIT) "Story 9:16" else "Classic 3:4",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            }
            if (kind == KIND_QUOTE) {
                ComposerPill(
                    label = if (credit.isBlank()) "Who said it" else credit,
                    icon = CurioIcons.FormatQuote,
                    expanded = openTool == "credit",
                    onClick = { openTool = if (openTool == "credit") null else "credit" }
                )
                androidx.compose.animation.AnimatedVisibility(visible = openTool == "credit") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        BasicTextField(
                            value = credit,
                            onValueChange = { if (it.length <= 80) credit = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(curioDialogActionColor()),
                            decorationBox = { inner ->
                                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                                    if (credit.isEmpty()) Text(
                                        text = "The name under the line",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    )
                                    inner()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            problem?.let { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * One compact pill in the composer's attach rail: a glyph, a few words, and
 * either a chevron (closed) or nothing (expanded — the tray under it is the
 * affordance). It is the whole "form" the composer offers; everything else is
 * canvas.
 */
@Composable
private fun ComposerPill(
    label: String,
    icon: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
    trailing: String? = null
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (expanded) curioDialogActionColor().copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        modifier = modifier.curioPressClickable(
            pressedScale = 0.96f,
            hapticOnPress = false,
            onClickLabel = label,
            onClick = onClick
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
        ) {
            CurioIcon(
                name = icon,
                contentDescription = null,
                tint = iconTint ?: curioDialogActionColor(),
                size = 15.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = curioDialogActionColor()
                )
            }
        }
    }
}

/**
 * The topic's own quick fact, trimmed to what a card can hold.
 *
 * This is what makes "sharing a topic" mean something: the words start from
 * the fact the app already has for that topic (its teaser), and the writer
 * edits it. A teaser longer than the card's budget is cut at the last full
 * sentence that fits, never mid-word.
 */
private fun quickFactOf(topic: CurioTopic): String {
    val teaser = topic.teaser.trim()
    if (teaser.length <= CommunityApi.MAX_FACT_CHARS) return teaser
    val slice = teaser.take(CommunityApi.MAX_FACT_CHARS)
    val stop = slice.lastIndexOf(". ")
    return if (stop > 60) slice.take(stop + 1) else slice.trimEnd()
}

/**
 * One catalog hit in the composer's picker: the topic's lane glyph, its name
 * and its byline, so two similarly named topics (a book and its film) can be
 * told apart before they are picked.
 */
@Composable
private fun TopicPickRow(topic: CurioTopic, onClick: () -> Unit) {
    val lane = CurioCategories.byId(topic.categoryId)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            CurioIcon(
                name = lane.iconGlyph,
                contentDescription = null,
                tint = lane.accent,
                size = 18.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                val byline = topic.byline.orEmpty()
                if (byline.isNotBlank()) {
                    Text(
                        text = byline,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = lane.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The report reasons the dialog offers — one tap, no free text. */
@Composable
internal fun ReportCardDialog(
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    val reasons = listOf(
        "spam" to "Spam or a repeated card",
        "offensive" to "Offensive or harmful",
        "off_topic" to "Not what it claims to be"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        title = { Text("Report this card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                reasons.forEach { (id, label) ->
                    TextButton(onClick = { onReport(id) }) { Text(label) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        dismissButton = {}
    )
}

/** `#RRGGBB` from a Color, and back again (the card stores the hex). */
private fun hexOf(color: Color): String = "#%06X".format(0xFFFFFF and color.toArgb())

internal fun parseAccent(hex: String): Color =
    runCatching { Color(0xFF000000 or hex.removePrefix("#").toLong(16)) }
        .getOrDefault(Color(0xFF8E8E93))
