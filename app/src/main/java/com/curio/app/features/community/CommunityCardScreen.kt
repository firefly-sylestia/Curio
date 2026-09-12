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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.SettingsNavRail
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionInfoRow
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
import com.curio.app.ui.components.shareComposableCard
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionColor
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/**
 * ONE COMMUNITY CARD — the card's own view.
 *
 * The full card at full width, the poster's caption, its author and remaining
 * life, the actions (like, share as an image, report, take down your own) and
 * the replies underneath. Comments live here rather than in the feed so the
 * wall stays scannable, and they die with the card (the DB cascades), which is
 * what keeps the 24-hour promise honest.
 */
@Composable
fun CommunityCardScreen(navController: NavController, cardId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state
    val token = account.session?.accessToken
    val myUserId = account.session?.userId
    val wide = windowWidthSizeClass().isWide
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()

    var card by remember { mutableStateOf<CommunityCard?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reporting by remember { mutableStateOf(false) }
    var commentsOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    suspend fun load() {
        val active = token ?: return
        loading = true
        CommunityApi.card(active, cardId, myUserId).fold(
            onSuccess = {
                card = it
                error = null
            },
            onFailure = { error = it.message }
        )
        loading = false
    }

    LaunchedEffect(cardId, token) { if (token != null) load() }

    /** Shares the card as the same PNG the reveal page produces. */
    fun share(current: CommunityCard) {
        val aspect = runCatching { ShareCardAspect.valueOf(current.aspect) }
            .getOrDefault(ShareCardAspect.CLASSIC)
        val style = runCatching { ShareCardStyle.valueOf(current.style) }
            .getOrDefault(ShareCardStyle.PAPER)
        shareComposableCard(
            context = context,
            cardSize = DpSize(aspect.widthDp.dp, aspect.heightDp.dp),
            authority = "${context.packageName}.fileprovider",
            shareText = current.caption.ifBlank { null },
            card = {
                TopicShareCard(
                    topicName = current.topicName,
                    categoryName = current.categoryName,
                    categoryGlyph = current.categoryGlyph,
                    accent = parseAccent(current.accentHex),
                    factText = current.factText,
                    sharerName = current.authorLabel,
                    aspect = aspect,
                    style = style,
                    byline = current.byline,
                    bodyScale = current.bodyScale
                )
            }
        )
    }

    // Read the state once per composition — the list then renders this
    // snapshot instead of re-reading a state var inside the lazy scope.
    val current = card

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
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (wide) {
                item(key = "hero", contentType = "hero") {
                    SettingsHeroHeader(
                        title = "Card",
                        subtitle = "Gone in 24 hours",
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

            when {
                token == null -> item {
                    SettingsOptionCard {
                        SettingsOptionInfoRow(
                            CurioIcons.Info,
                            "Sign in to open cards",
                            "Community cards need an account with Online mode on."
                        )
                        SettingsOptionRowLink("Settings → Online mode") {
                            navController.navigate(CurioRoutes.SETTINGS_ONLINE)
                        }
                    }
                }
                current == null && loading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(strokeWidth = 2.dp) }
                }
                current == null -> item {
                    SettingsOptionCard {
                        SettingsOptionInfoRow(
                            CurioIcons.Info,
                            "This card is gone",
                            error ?: "Cards only last 24 hours — it may have expired."
                        )
                    }
                }
                else -> {
                    if (current.caption.isNotBlank()) {
                        item(key = "caption") {
                            Text(
                                text = current.caption,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    item(key = "card", contentType = "card") {
                        CommunityCardCanvas(card = current)
                    }
                    item(key = "meta") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    if (current.authorId.isNotBlank()) {
                                        navController.navigate(
                                            CurioRoutes.socialProfile(current.authorId)
                                        ) { launchSingleTop = true }
                                    }
                                }
                        ) {
                            // The portrait and the LIVE username: renaming
                            // yourself updates every card you ever posted.
                            SocialAvatar(style = current.authorAvatar, avatarSize = 38.dp)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            ) {
                                Text(
                                    text = "@${current.authorLabel}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = listOfNotNull(
                                        current.categoryName.takeIf { it.isNotBlank() },
                                        agoLabel(current.createdAtMillis),
                                        if (current.hoursLeft <= 0L) "expiring"
                                        else "${current.hoursLeft}h left"
                                    ).joinToString(" · "),
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
                            if (loading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    item(key = "actions") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CommunityAction(
                                glyph = CurioIcons.ThumbUp,
                                label = if (current.likeCount > 0) current.likeCount.toString() else "Like",
                                tinted = current.likedByMe,
                                onClick = {
                                    scope.launch {
                                        val call = if (current.likedByMe && myUserId != null) {
                                            CommunityApi.unlike(token, current.id, myUserId)
                                        } else {
                                            CommunityApi.like(token, current.id)
                                        }
                                        call.fold(
                                            onSuccess = { load() },
                                            onFailure = { error = it.message }
                                        )
                                    }
                                }
                            )
                            CommunityAction(
                                glyph = CurioIcons.FormatQuote,
                                label = if (current.commentCount > 0) current.commentCount.toString()
                                else "Comment",
                                tinted = false,
                                onClick = { commentsOpen = true }
                            )
                            CommunityAction(CurioIcons.Share, "Share", false) { share(current) }
                            CommunityAction(CurioIcons.Flag, "Report", false) { reporting = true }
                            if (current.mine) {
                                CommunityAction(CurioIcons.Delete, "Take down", false) {
                                    scope.launch {
                                        CommunityApi.delete(token, current.id).fold(
                                            onSuccess = { navController.popBackStack() },
                                            onFailure = { error = it.message }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            error?.takeIf { current != null }?.let { message ->
                item(key = "error") {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (!wide) {
            SettingsHeroHeader(
                title = "Card",
                subtitle = "Gone in 24 hours",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop
            )
        }
    }

    if (reporting) {
        // `?.let` hands the lambda a definitely non-null token, so the report
        // call never leans on a smart cast across a lambda boundary.
        token?.let { active ->
            ReportCardDialog(
                onDismiss = { reporting = false },
                onReport = { reason ->
                    scope.launch {
                        CommunityApi.report(active, cardId, reason).fold(
                            onSuccess = { reporting = false },
                            onFailure = { error = it.message }
                        )
                    }
                }
            )
        }
    }

    // The replies live in one sheet, shared with the wall, so a comment reads
    // the same wherever it was opened from.
    val shownCard = current
    val activeToken = token
    if (commentsOpen && shownCard != null && activeToken != null) {
        CommunityCommentsSheet(
            card = shownCard,
            accessToken = activeToken,
            myUserId = myUserId,
            onDismiss = { commentsOpen = false },
            onChanged = { scope.launch { load() } },
            onOpenProfile = { id ->
                navController.navigate(CurioRoutes.socialProfile(id)) { launchSingleTop = true }
            }
        )
    }
}

/** A one-line row that opens another settings surface (the locked states). */
@Composable
private fun SettingsOptionRowLink(
    title: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        TextButton(onClick = onClick) {
            CurioIcon(
                name = CurioIcons.ChevronRight,
                contentDescription = null,
                tint = curioDialogActionColor(),
                size = 16.dp
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = curioDialogActionColor()
            )
        }
    }
}
