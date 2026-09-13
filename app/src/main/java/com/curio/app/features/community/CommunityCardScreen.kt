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
import androidx.compose.material3.Surface
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
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.DisposableEffect
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioContentFilter
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityComment
import com.curio.app.data.supabase.KIND_CARD
import com.curio.app.data.supabase.KIND_QUOTE
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.RealtimeWatch
import com.curio.app.data.supabase.SupabaseRealtime
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionInfoRow
import com.curio.app.features.settings.heroPageBackground
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
import com.curio.app.ui.theme.curioDialogActionButtonColors
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
 *
 * This is a SOCIAL page, not a settings one: it carries the app's torn hero
 * (the established Curio header) and none of the settings chrome — the
 * settings rail that used to ride this list is gone, so opening a card from
 * the wall stays inside the community.
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
    // Taking a card down is irreversible — it asks first, like every other
    // destructive move in the social layer.
    var takingDown by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

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
        if (current.kind != KIND_CARD) {
            // A note or a quote has no card art of its own, so it is shared as
            // the TEXT it is (the credit riding under a quote) instead of as a
            // card for a topic it was never about.
            val body = buildString {
                append(current.factText)
                if (current.kind == KIND_QUOTE && current.byline.isNotBlank()) {
                    append("\n— ").append(current.byline)
                }
            }
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(send, "Share"))
            return
        }
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
            when {
                token == null -> item {
                    SettingsOptionCard {
                        SettingsOptionInfoRow(
                            CurioIcons.Info,
                            "Sign in to open cards",
                            "Social cards need an account with Online mode on."
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
                        if (current.kind == KIND_CARD) {
                            CommunityCardCanvas(card = current)
                        } else {
                            // A note or a quote is words: it is rendered as
                            // text here exactly as it is on the wall.
                            SocialTextPost(card = current, onClick = {})
                        }
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
                            // The portrait, the DISPLAY name and the LIVE
                            // username: renaming either updates every card you
                            // ever posted.
                            SocialAvatar(style = current.authorAvatar, avatarSize = 38.dp)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            ) {
                                // The display name LEADS; the @username opens
                                // the meta line beneath it.
                                Text(
                                    text = current.authorLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = listOfNotNull(
                                        current.authorHandleLabel,
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
                                animate = true,
                                // The count moves with the tap and the server
                                // is told afterwards: waiting for a round trip
                                // AND a page reload made a like feel broken.
                                // A failed call no longer hides — the page says
                                // so and the next read puts the truth back.
                                onClick = {
                                    val liking = !current.likedByMe
                                    card = current.toggleLike()
                                    scope.launch {
                                        val call = if (liking) {
                                            CommunityApi.like(token, current.id, myUserId ?: return@launch)
                                        } else {
                                            CommunityApi.unlike(token, current.id, myUserId ?: return@launch)
                                        }
                                        call.onFailure { failure -> error = failure.message }
                                    }
                                }
                            )
                            // The card's own page gains the dislike the wall
                            // has: one reaction per person, so disliking takes
                            // back a like in the same tap, optimistically.
                            CommunityAction(
                                glyph = CurioIcons.ThumbDown,
                                label = if (current.dislikeCount > 0) current.dislikeCount.toString() else "",
                                tinted = current.dislikedByMe,
                                animate = true,
                                onClick = {
                                    val disliking = !current.dislikedByMe
                                    card = current.toggleDislike()
                                    scope.launch {
                                        val call = if (disliking) {
                                            CommunityApi.dislike(token, current.id, myUserId ?: return@launch)
                                        } else {
                                            CommunityApi.undislike(token, current.id, myUserId ?: return@launch)
                                        }
                                        call.onFailure { failure -> error = failure.message }
                                    }
                                }
                            )
                            // The replies sit directly under the post, so
                            // this count is a LABEL, not a door any more.
                            CommunityAction(
                                glyph = CurioIcons.FormatQuote,
                                label = if (current.commentCount > 0) current.commentCount.toString()
                                else "Comment",
                                tinted = false,
                                onClick = { }
                            )
                            CommunityAction(CurioIcons.Share, "Share", false, onClick = { share(current) })
                            // Report and take-down are ICONS here, exactly as
                            // they are on the wall: the actions are decided,
                            // not read, and the worded pills crowded the row.
                            CommunityAction(CurioIcons.Flag, "", false, onClick = { reporting = true })
                            if (current.mine) {
                                CommunityAction(CurioIcons.Delete, "", false, onClick = {
                                    takingDown = true
                                })
                            }
                        }
                    }

                    // ── The replies, INLINE under the post ──────────────
                    // The comments used to open as a separate sheet over the
                    // card, which buried the thing being discussed. Reading
                    // and writing replies now lives on the card's own page,
                    // in the same list, under the same hero.
                    item(key = "replies") {
                        CommunityInlineReplies(
                            card = current,
                            accessToken = token,
                            myUserId = myUserId,
                            onOpenProfile = { id ->
                                navController.navigate(CurioRoutes.socialProfile(id)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }

            error?.takeIf { current != null }?.let { message ->
                item(key = "error") {
                    // Quiet on-surface ink: a red slab under a post read as a
                    // tester build, not as a sentence for a person.
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

    if (takingDown) {
        // `?.let` hands the lambda a definitely non-null token, so the delete
        // never leans on a smart cast across a lambda boundary.
        token?.let { active ->
            SocialConfirmDialog(
                title = "Take this card down?",
                body = "It disappears from the wall for everyone right away. " +
                    "Its replies go with it.",
                confirmLabel = "Take down",
                busy = busy,
                onDismiss = { if (!busy) takingDown = false },
                onConfirm = {
                    busy = true
                    scope.launch {
                        CommunityApi.delete(active, cardId).fold(
                            onSuccess = {
                                takingDown = false
                                busy = false
                                navController.popBackStack()
                            },
                            onFailure = {
                                error = it.message
                                busy = false
                            }
                        )
                    }
                }
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

    // (Replies are inline in the list now — no sheet over the card.)
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

/**
 * The card's replies, INLINE under the post.
 *
 * The sheet that used to open over the card buried the thing being discussed:
 * reading and writing replies belongs on the card's own page. This section
 * reuses the wall's reply rows and composer semantics, loads from the device
 * cache first, follows the same realtime channel, and counts towards nothing
 * but this card.
 */
@Composable
private fun CommunityInlineReplies(
    card: CommunityCard,
    accessToken: String?,
    myUserId: String?,
    onOpenProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var replies by remember(card.id) { mutableStateOf<List<CommunityComment>>(emptyList()) }
    var loading by remember(card.id) { mutableStateOf(false) }
    var error by remember(card.id) { mutableStateOf<String?>(null) }
    var text by remember(card.id) { mutableStateOf("") }
    var replyTo by remember(card.id) { mutableStateOf<CommunityComment?>(null) }
    var editing by remember(card.id) { mutableStateOf<CommunityComment?>(null) }
    var expandedRoots by remember(card.id) { mutableStateOf(setOf<String>()) }
    var pushed by remember(card.id) { mutableStateOf(0) }

    suspend fun load() {
        val active = accessToken ?: return
        loading = true
        CommunityApi.comments(active, card.id, myUserId).fold(
            onSuccess = {
                replies = it
                SocialCommentsCache.write(context, card.id, it)
                error = null
            },
            onFailure = { error = it.message }
        )
        loading = false
    }

    LaunchedEffect(card.id, accessToken) {
        if (accessToken == null) return@LaunchedEffect
        if (replies.isEmpty()) {
            val cached = SocialCommentsCache.read(context, card.id, myUserId)
            if (cached.isNotEmpty()) replies = cached
        }
        load()
    }

    // Live replies: the same socket the sheet listened on, so a new reply
    // lands while the card is open.
    DisposableEffect(card.id, accessToken) {
        if (accessToken == null) return@DisposableEffect onDispose { }
        val owner = "card-replies:${card.id}"
        SupabaseRealtime.watch(
            owner = owner,
            accessToken = accessToken,
            watches = listOf(
                RealtimeWatch(
                    table = "community_comments",
                    filter = "card_id=eq.${card.id}",
                    events = listOf("INSERT", "UPDATE", "DELETE")
                )
            )
        ) { scope.launch { pushed++ } }
        onDispose { SupabaseRealtime.unwatch(owner) }
    }
    LaunchedEffect(pushed) { if (pushed > 0) load() }

    val branch = remember(replies, expandedRoots) { branchRenderList(replies, expandedRoots) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Replies",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                }
            }

            if (replies.isEmpty() && !loading && error == null) {
                Text(
                    text = "No replies yet. Say something.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            branch.filterIsInstance<BranchRender.Reply>().forEach { node ->
                val reply = node.comment
                CommunityReplyRow(
                    reply = reply,
                    depth = node.depth,
                    onAuthor = { if (reply.authorId.isNotBlank()) onOpenProfile(reply.authorId) },
                    onReply = { replyTo = if (replyTo?.id == reply.id) null else reply },
                    onEdit = if (reply.mine) {
                        {
                            editing = reply
                            text = reply.body
                            replyTo = null
                        }
                    } else null,
                    canAddFriend = false,
                    onAddFriend = { },
                    onDelete = {
                        val active = accessToken
                        if (active != null) scope.launch {
                            CommunityApi.deleteComment(active, reply.id).fold(
                                onSuccess = {
                                    if (editing?.id == reply.id) {
                                        editing = null
                                        text = ""
                                    }
                                    load()
                                },
                                onFailure = { error = it.message }
                            )
                        }
                    }
                )
            }
            branch.filterIsInstance<BranchRender.More>().forEach { more ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .padding(start = 18.dp)
                        .clickable { expandedRoots = expandedRoots + more.rootId }
                ) {
                    Text(
                        text = "Show ${more.hidden} more",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = curioDialogActionColor(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CurioContentFilter.problem(text)?.let { problem ->
                Text(
                    text = problem,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            replyTo?.let { target ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = "Replying to @${target.authorLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        CurioIcon(
                            name = CurioIcons.Close,
                            contentDescription = "Stop replying",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 14.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { replyTo = null }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= CommunityApi.MAX_COMMENT_CHARS) text = it
                    },
                    label = {
                        Text(
                            when {
                                editing != null -> "Edit your reply"
                                replyTo == null -> "Add a reply"
                                else -> "Reply to @${replyTo?.authorLabel}"
                            }
                        )
                    },
                    maxLines = 3,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val active = accessToken ?: return@Button
                        val editTarget = editing
                        val parent = replyTo
                        when {
                            editTarget != null -> scope.launch {
                                CommunityApi.editComment(active, editTarget.id, text).fold(
                                    onSuccess = {
                                        editing = null
                                        text = ""
                                        load()
                                    },
                                    onFailure = { error = it.message }
                                )
                            }
                            else -> scope.launch {
                                val handle = AppPreferences.getUsername(context).ifBlank {
                                    AppPreferences.getDisplayName(context)
                                }
                                CommunityApi.comment(
                                    active,
                                    card.id,
                                    text,
                                    handle,
                                    parentId = parent?.id
                                ).fold(
                                    onSuccess = {
                                        text = ""
                                        replyTo = null
                                        load()
                                    },
                                    onFailure = { error = it.message }
                                )
                            }
                        }
                    },
                    enabled = accessToken != null && text.isNotBlank() && CurioContentFilter.isClean(text),
                    shape = RoundedCornerShape(50),
                    colors = curioDialogActionButtonColors()
                ) {
                    Text(
                        text = if (editing != null) "Save" else "Send",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
