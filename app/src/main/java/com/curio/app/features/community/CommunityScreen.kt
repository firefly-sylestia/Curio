package com.curio.app.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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

    var onlineMode by remember { mutableStateOf(AppPreferences.isOnlineModeEnabled(context)) }
    var cards by remember { mutableStateOf<List<CommunityCard>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var composing by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf<CommunityCard?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }
    LaunchedEffect(account.session) { onlineMode = AppPreferences.isOnlineModeEnabled(context) }

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
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (wide) {
                item(key = "hero", contentType = "hero") {
                    SettingsHeroHeader(
                        title = "Community",
                        subtitle = "Text cards — gone in 24 hours",
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            item(key = "settings-nav", contentType = "settings-nav") {
                SettingsNavRail(
                    active = null,
                    onSelect = { navigateToSettingsSection(navController, it) },
                    navController = navController
                )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Button(
                            onClick = { composing = true },
                            shape = RoundedCornerShape(50),
                            colors = curioDialogActionButtonColors()
                        ) {
                            Text(
                                "Share a card",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Spacer(Modifier.width(10.dp))
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

        if (!wide) {
            SettingsHeroHeader(
                title = "Community",
                subtitle = "Text cards — gone in 24 hours",
                onBack = { navController.popBackStack() },
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
 * One card in the wall: the REAL share card, rebuilt from the stored
 * text/style data and scaled into the feed's width (the card's own geometry
 * is 405×720 / 450×600 dp, too tall for a list at 1×).
 */
@Composable
private fun CommunityCardItem(
    card: CommunityCard,
    onLike: () -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit
) {
    val style = runCatching { ShareCardStyle.valueOf(card.style) }
        .getOrDefault(ShareCardStyle.PAPER)
    val aspect = runCatching { ShareCardAspect.valueOf(card.aspect) }
        .getOrDefault(ShareCardAspect.CLASSIC)
    val cardWidth = aspect.widthDp.dp
    val cardHeight = aspect.heightDp.dp
    val accent = remember(card.accentHex) { parseAccent(card.accentHex) }

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
        ) {
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
                        sharerName = card.authorHandle,
                        aspect = aspect,
                        style = style,
                        byline = card.byline,
                        bodyScale = card.bodyScale
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            CommunityAction(
                glyph = CurioIcons.ThumbUp,
                label = if (card.likeCount > 0) card.likeCount.toString() else "Like",
                tinted = card.likedByMe,
                onClick = onLike
            )
            CommunityAction(CurioIcons.Flag, "Report", false, onReport)
            if (card.mine) {
                CommunityAction(CurioIcons.Delete, "Take down", false, onDelete)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (card.hoursLeft <= 0L) "Expiring" else "${card.hoursLeft}h left",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CommunityAction(
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
 * The composer: a topic, the words, and a card style — nothing else, because
 * a community card can only ever be text. Lane + style chips reuse the app's
 * existing catalogs so a posted card renders exactly like a shared one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityComposerSheet(
    onDismiss: () -> Unit,
    onPost: (CommunityCardDraft) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var topic by remember { mutableStateOf("") }
    var fact by remember { mutableStateOf("") }
    var lane by remember { mutableStateOf(CurioCategories.byId(CategoryId.WILDCARD)) }
    var style by remember { mutableStateOf(ShareCardStyle.PAPER) }
    val lanes = remember { CurioCategories.all.filter { !it.isHidden } }
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
        topicName = topic,
        categoryName = lane.displayName,
        categorySlug = lane.id.name.lowercase(),
        categoryGlyph = lane.iconGlyph,
        accentHex = hexOf(lane.accent),
        factText = fact
    )
    val problem = CommunityApi.draftProblem(draft)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = curioDialogContainerColor()
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
                text = "Share a card",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Text only — it disappears after 24 hours.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                singleLine = true,
                label = { Text("What is it about?") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = fact,
                onValueChange = { if (it.length <= CommunityApi.MAX_FACT_CHARS) fact = it },
                minLines = 3,
                label = { Text("Your card") },
                supportingText = { Text("${fact.length}/${CommunityApi.MAX_FACT_CHARS}") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "LANE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(lanes, key = { it.id.name }) { item ->
                    FilterChip(
                        selected = item.id == lane.id,
                        onClick = { lane = item },
                        label = { Text(item.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
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

/** The report reasons the sheet offers — one tap, no free text. */
@Composable
private fun ReportCardDialog(
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

private fun parseAccent(hex: String): Color =
    runCatching { Color(0xFF000000 or hex.removePrefix("#").toLong(16)) }
        .getOrDefault(Color(0xFF8E8E93))
