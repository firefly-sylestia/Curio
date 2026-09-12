package com.curio.app.features.community

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioTopic
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityCardDraft
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.SettingsNavRail
import com.curio.app.features.settings.SettingsOptionCard
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
import com.curio.app.ui.components.ShareCardAspect
import com.curio.app.ui.components.ShareCardStyle
import com.curio.app.ui.components.TopicShareCard
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
    var error by remember { mutableStateOf<String?>(null) }
    var composing by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf<CommunityCard?>(null) }
    var commentsFor by remember { mutableStateOf<CommunityCard?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    val eligible = account.signedIn && onlineMode && token != null

    suspend fun load() {
        val active = token ?: return
        loading = true
        CommunityApi.feed(active, account.session?.userId).fold(
            onSuccess = {
                cards = it
                error = null
            },
            onFailure = { error = it.message }
        )
        loading = false
    }

    LaunchedEffect(eligible, token) {
        if (eligible) load() else cards = emptyList()
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
                        title = "Community",
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
                                "This build has no Curio online project, so there is no community to load."
                            )
                            !account.signedIn -> {
                                SettingsOptionInfoRow(
                                    CurioIcons.Info,
                                    "Sign in to see the community",
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
                item {
                    // The wall's actions. Posting lives on the floating button
                    // at the bottom (one clear door), so this row only carries
                    // the two places you go FROM the wall.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        TextButton(onClick = { navController.navigate(CurioRoutes.FRIENDS) }) {
                            CurioIcon(
                                name = CurioIcons.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 16.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Friends")
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            // Guarded: an unmatched person/ route would throw.
                            val me = account.session?.userId
                            if (!me.isNullOrBlank()) {
                                navController.navigate(CurioRoutes.socialProfile(me)) {
                                    launchSingleTop = true
                                }
                            }
                        }) {
                            CurioIcon(
                                name = CurioIcons.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 16.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("You")
                        }
                        if (loading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            TextButton(onClick = { scope.launch { load() } }) {
                                Text("Refresh")
                            }
                        }
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
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
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
                        onLike = {
                            val active = token ?: return@CommunityCardItem
                            val userId = account.session?.userId
                            scope.launch {
                                val call = if (card.likedByMe && userId != null) {
                                    CommunityApi.unlike(active, card.id, userId)
                                } else {
                                    CommunityApi.like(active, card.id)
                                }
                                call.fold(
                                    onSuccess = { load() },
                                    onFailure = { error = it.message }
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
                    "Share a topic",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        if (!wide) {
            SettingsHeroHeader(
                title = "Community",
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
                        AppPreferences.getDisplayName(context)
                    ).fold(
                        onSuccess = {
                            composing = false
                            notice = "Posted — it disappears in 24 hours."
                            load()
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
                onChanged = { scope.launch { load() } },
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
     * feed stops feeling like a feed. The card's own view and a profile page
     * pass 1f and show it whole.
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

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(widthFraction)) {
        val density = LocalDensity.current
        val scale = with(density) {
            (maxWidth.toPx() / cardWidth.toPx()).coerceAtMost(1f)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight * scale)
        ) {
            Box(
                modifier = Modifier
                    .size(cardWidth, cardHeight)
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
                    Text(
                        text = "@${card.authorLabel}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = if (card.hoursLeft <= 0L) "Expiring now"
                        else "${card.hoursLeft}h left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

            CommunityCardCanvas(
                card = card,
                modifier = Modifier
                    .clipToBounds()
                    .clickable(onClick = onOpen),
                widthFraction = FEED_CARD_WIDTH
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CommunityAction(
                    glyph = CurioIcons.ThumbUp,
                    label = if (card.likeCount > 0) card.likeCount.toString() else "Like",
                    tinted = card.likedByMe,
                    onClick = onLike
                )
                CommunityAction(
                    glyph = CurioIcons.FormatQuote,
                    label = if (card.commentCount > 0) card.commentCount.toString() else "Reply",
                    tinted = false,
                    onClick = onComments
                )
                Spacer(Modifier.weight(1f))
                if (card.mine) {
                    CommunityAction(CurioIcons.Delete, "Take down", false, onDelete)
                }
                CommunityAction(CurioIcons.Flag, "Report", false, onReport)
            }
        }
    }
}

/** How much of the wall's width one card claims (centred). */
private const val FEED_CARD_WIDTH = 0.74f

@Composable
internal fun CommunityAction(
    glyph: String,
    label: String,
    tinted: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        CurioIcon(
            name = glyph,
            contentDescription = null,
            tint = if (tinted) curioDialogActionColor() else MaterialTheme.colorScheme.onSurfaceVariant,
            size = 16.dp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (tinted) curioDialogActionColor() else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The composer: pick a TOPIC, write the words, choose a card style.
 *
 * The topic is chosen from the app's own catalog rather than typed, which is
 * the whole point of the flow — a card IS a topic being passed on, so its
 * name, lane, glyph and accent come from the catalog entry and can never
 * disagree with the card that renders. Searching covers every lane through
 * the loader's lightweight index (the same one the Topic Database searches),
 * with the warm lane pools as the fallback on a cold install.
 *
 * Lane chips are gone with the free-text field: a topic already knows its
 * lane, so there is nothing left to get wrong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityComposerSheet(
    onDismiss: () -> Unit,
    onPost: (CommunityCardDraft) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var picked by remember { mutableStateOf<CurioTopic?>(null) }
    var query by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var fact by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(ShareCardStyle.PAPER) }
    // The whole-catalog index, loaded once. It is the prebuilt lightweight
    // index (name/byline keys only), so searching never parses a lane.
    var index by remember { mutableStateOf<List<com.curio.app.data.TopicIndexEntry>?>(null) }
    LaunchedEffect(Unit) { index = TopicJsonLoader.loadIndex() }

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
    val draft = CommunityCardDraft(
        topicName = picked?.name.orEmpty(),
        categoryName = lane?.displayName.orEmpty(),
        categorySlug = lane?.id?.name?.lowercase().orEmpty(),
        categoryGlyph = lane?.iconGlyph.orEmpty(),
        accentHex = lane?.let { hexOf(it.accent) }.orEmpty(),
        factText = fact,
        caption = caption
    )
    val problem = CommunityApi.draftProblem(draft)

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
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Share a topic",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Pick a topic and write the words that go on its card. It shows on the wall for 24 hours, " +
                    "and only this text is ever posted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (picked == null) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Search topics") },
                    placeholder = { Text("A book, a film, a dish…") },
                    modifier = Modifier.fillMaxWidth()
                )
                when {
                    q.length < 2 -> Text(
                        text = "Type at least two letters to search the catalog.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    results.isEmpty() -> Text(
                        text = "Nothing matches that. Try another spelling.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(results, key = { "${it.categoryId.name}|${it.name}" }) { topic ->
                            TopicPickRow(
                                topic = topic,
                                onClick = {
                                    picked = topic
                                    query = ""
                                }
                            )
                        }
                    }
                }
            } else {
                // The chosen topic, wearing its lane — tap to pick again.
                Surface(
                    onClick = { picked = null },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        CurioIcon(
                            name = lane?.iconGlyph ?: CurioIcons.Wildcard,
                            contentDescription = null,
                            tint = lane?.accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 20.dp
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        ) {
                            Text(
                                text = picked?.name.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = lane?.displayName.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = curioDialogActionColor()
                        )
                    }
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = {
                    if (it.length <= CommunityApi.MAX_CAPTION_CHARS) caption = it
                },
                singleLine = true,
                label = { Text("A caption above the card (optional)") },
                supportingText = { Text("${caption.length}/${CommunityApi.MAX_CAPTION_CHARS}") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = fact,
                onValueChange = { if (it.length <= CommunityApi.MAX_FACT_CHARS) fact = it },
                minLines = 3,
                label = { Text("The words on the card") },
                supportingText = { Text("${fact.length}/${CommunityApi.MAX_FACT_CHARS}") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "STYLE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                styles.forEach { option ->
                    FilterChip(
                        selected = option == style,
                        onClick = { style = option },
                        label = {
                            Text(option.label, style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onPost(draft.copy(style = style.name)) },
                    enabled = problem == null,
                    shape = RoundedCornerShape(50),
                    colors = curioDialogActionButtonColors()
                ) {
                    Text("Post")
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
