package com.curio.app.features.community

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioTopic
import com.curio.app.data.TopicIndexEntry
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityCardDraft
import com.curio.app.data.supabase.KIND_CARD
import com.curio.app.data.supabase.KIND_NOTE
import com.curio.app.data.supabase.KIND_QUOTE
import com.curio.app.ui.components.ShareCardAspect
import com.curio.app.ui.components.ShareCardStyle
import com.curio.app.ui.theme.ChevronDown
import com.curio.app.ui.theme.ChevronUp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.themedAccent
import kotlinx.coroutines.launch

/**
 * Full-screen social composer. It deliberately behaves like a creation
 * destination rather than a bottom sheet: topic, preview, writing and
 * publishing are one continuous flow.
 *
 * Shape of the flow (v349):
 *  - [PostKindRail] picks what is being made: a Note, a Topic or a Quote.
 *  - A TOPIC post puts the topic chooser at the TOP — what the post is about
 *    is the first decision, everything else follows it.
 *  - The preview is ALWAYS on (it is the point of this composer: a post is
 *    written against what it will look like). For a topic it carries the
 *    Card | Note toggle — the same topic can ship as the share card or as a
 *    plain text post.
 *  - The card LOOK controls (paper/vinyl/minimal, shape, text size) exist
 *    only while the topic is presented as a card: they are the card's
 *    settings, so they hide the moment the Note presentation is chosen.
 *  - A Quote keeps its credit field; a Note stays words-only. The caption is
 *    available to every kind — the wall prints it above the post body.
 */
@Composable
internal fun CommunityPostScreen(
    onDismiss: () -> Unit,
    onPost: (CommunityCardDraft) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var kind by remember { mutableStateOf(KIND_NOTE) }
    var text by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var credit by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf<CurioTopic?>(null) }
    var index by remember { mutableStateOf<List<TopicIndexEntry>>(emptyList()) }
    var topicPickerOpen by remember { mutableStateOf(false) }
    var topicPresentation by remember { mutableStateOf(TopicPresentation.CARD) }
    var style by remember { mutableStateOf(ShareCardStyle.PAPER) }
    var aspect by remember { mutableStateOf(ShareCardAspect.CLASSIC) }
    var bodyScale by remember { mutableStateOf(1f) }
    var posting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        index = TopicJsonLoader.loadIndex() ?: emptyList()
        if (kind != KIND_CARD) focusManager.clearFocus(force = false)
    }
    BackHandler(onBack = onDismiss)

    val category = topic?.categoryId?.let(CurioCategories::byId)
    val accent = category?.themedAccent() ?: MaterialTheme.colorScheme.primary
    val accentInk = if (accent.luminance() > 0.55f) Color.Black else Color.White

    val topicResults = remember(index, query) {
        val q = query.trim()
        if (q.isBlank()) {
            index.take(18)
        } else {
            index.asSequence()
                .sortedWith(compareByDescending<TopicIndexEntry> { it.topic.name.contains(q, true) }.thenBy { it.topic.name })
                .filter {
                    it.topic.name.contains(q, true) ||
                        it.topic.byline.contains(q, true) ||
                        it.topic.tags.any { tag -> tag.contains(q, true) }
                }
                .take(18)
                .toList()
        }
    }

    // A topic presented as a NOTE ships as a plain text post: the topic name
    // rides above the words and the draft carries no card art at all.
    val effectiveKind = if (kind == KIND_CARD && topicPresentation == TopicPresentation.NOTE) KIND_NOTE else kind
    val accentHex = category?.let { "%08X".format(it.themedAccent().toArgb()) }.orEmpty()
    val noteBody = if (kind == KIND_CARD && topicPresentation == TopicPresentation.NOTE) {
        buildTopicNoteBody(topic, text)
    } else {
        text
    }
    val draft = CommunityCardDraft(
        topicName = if (kind == KIND_CARD) topic?.name.orEmpty() else "",
        categoryName = if (kind == KIND_CARD) category?.displayName.orEmpty() else "",
        categorySlug = if (kind == KIND_CARD) topic?.categoryId?.name.orEmpty().lowercase() else "",
        categoryGlyph = if (kind == KIND_CARD) category?.iconGlyph.orEmpty() else "",
        accentHex = if (kind == KIND_CARD) accentHex else "",
        factText = noteBody,
        // The caption is offered to EVERY kind and the wall prints it above
        // the post body — dropping it here would silently lose what the
        // member typed on a note or a quote.
        caption = caption,
        kind = effectiveKind,
        style = style.name,
        aspect = aspect.name,
        bodyScale = bodyScale,
        byline = if (kind == KIND_QUOTE) credit else ""
    )

    val canPost = when {
        kind == KIND_CARD && topicPresentation == TopicPresentation.CARD -> topic != null && (text.isNotBlank() || caption.isNotBlank())
        kind == KIND_CARD && topicPresentation == TopicPresentation.NOTE -> topic != null && text.isNotBlank()
        kind == KIND_QUOTE -> text.isNotBlank() && credit.isNotBlank()
        else -> text.isNotBlank()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding()
            ) {
                ComposerTopBar(
                    kind = kind,
                    posting = posting,
                    canPost = canPost,
                    accent = accent,
                    accentInk = accentInk,
                    onDismiss = onDismiss,
                    onPost = {
                        if (canPost && !posting) {
                            posting = true
                            focusManager.clearFocus(force = true)
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            scope.launch {
                                onPost(draft)
                                posting = false
                            }
                        }
                    }
                )

                PostKindRail(
                    selected = kind,
                    accent = accent,
                    onSelect = { selected ->
                        kind = selected
                        if (selected == KIND_CARD) {
                            // Entering Topic opens the chooser right away: the
                            // topic is the first decision of a topic post.
                            topicPickerOpen = topic == null
                        } else {
                            topicPresentation = TopicPresentation.CARD
                        }
                        if (selected != KIND_QUOTE) credit = ""
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().imePadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ── The topic chooser leads a topic post ────────────────
                    if (kind == KIND_CARD) {
                        item(key = "topic") {
                            TopicPicker(
                                selected = topic,
                                expanded = topicPickerOpen,
                                query = query,
                                results = topicResults,
                                accent = accent,
                                onToggle = { topicPickerOpen = !topicPickerOpen },
                                onQuery = { query = it },
                                onPick = { picked ->
                                    topic = picked
                                    if (text.isBlank()) text = picked.teaser
                                    query = ""
                                    topicPickerOpen = false
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                        }
                    }

                    item(key = "preview") {
                        LivePostPreview(
                            kind = kind,
                            topicPresentation = topicPresentation,
                            topic = topic,
                            draft = draft,
                            onTopicNote = { topicPresentation = TopicPresentation.NOTE },
                            onTopicCard = { topicPresentation = TopicPresentation.CARD }
                        )
                    }

                    item(key = "editor") {
                        ComposerEditorField(
                            kind = kind,
                            value = text,
                            onValueChange = { text = it.take(if (kind == KIND_CARD) 700 else 1200) }
                        )
                    }

                    if (kind == KIND_QUOTE) {
                        item(key = "credit") {
                            LabeledTextField(
                                label = "Credit",
                                value = credit,
                                placeholder = "Who said it?",
                                singleLine = true,
                                onValueChange = { credit = it.take(120) }
                            )
                        }
                    }

                    // The card's LOOK belongs to the card presentation only:
                    // a topic shipped as text has no paper, shape or size.
                    if (kind == KIND_CARD && topicPresentation == TopicPresentation.CARD) {
                        item(key = "style") {
                            CardStylePicker(
                                style = style,
                                aspect = aspect,
                                bodyScale = bodyScale,
                                accent = accent,
                                onStyle = { style = it },
                                onAspect = { aspect = it },
                                onScale = { bodyScale = it }
                            )
                        }
                    }

                    item(key = "caption") {
                        LabeledTextField(
                            label = if (kind == KIND_CARD) "Caption" else "Add a caption",
                            value = caption,
                            placeholder = "Optional",
                            singleLine = false,
                            minHeightDp = 84,
                            onValueChange = { caption = it.take(180) }
                        )
                    }
                }
            }
        }
    }
}

private enum class TopicPresentation {
    CARD,
    NOTE
}

private fun buildTopicNoteBody(topic: CurioTopic?, text: String): String {
    val title = topic?.name.orEmpty().trim()
    return when {
        title.isBlank() -> text.trim()
        text.isBlank() -> title
        else -> "$title\n\n${text.trim()}"
    }
}

@Composable
private fun ComposerTopBar(
    kind: String,
    posting: Boolean,
    canPost: Boolean,
    accent: Color,
    accentInk: Color,
    onDismiss: () -> Unit,
    onPost: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onDismiss,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            CurioIcon(
                name = CurioIcons.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface,
                size = 19.dp,
                modifier = Modifier.padding(10.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Create a post",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = when (kind) {
                    KIND_CARD -> "Shape a discovery"
                    KIND_QUOTE -> "Keep a line worth remembering"
                    else -> "Put a thought somewhere"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            onClick = onPost,
            enabled = canPost && !posting,
            shape = RoundedCornerShape(16.dp),
            color = if (canPost && !posting) accent else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (canPost && !posting) accentInk else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                text = if (posting) "Posting" else "Post",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun PostKindRail(
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KindPill("Note", "A thought", KIND_NOTE, selected, accent, onSelect)
        KindPill("Topic", "A discovery", KIND_CARD, selected, accent, onSelect)
        KindPill("Quote", "Words worth keeping", KIND_QUOTE, selected, accent, onSelect)
    }
}

@Composable
private fun KindPill(
    title: String,
    subtitle: String,
    value: String,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit
) {
    val active = selected == value
    val ink = if (accent.luminance() > 0.55f) Color.Black else Color.White
    Surface(
        onClick = { onSelect(value) },
        shape = RoundedCornerShape(15.dp),
        color = if (active) accent else MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (active) ink else MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) ink.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LivePostPreview(
    kind: String,
    topicPresentation: TopicPresentation,
    topic: CurioTopic?,
    draft: CommunityCardDraft,
    onTopicNote: () -> Unit,
    onTopicCard: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = when {
                            kind == KIND_CARD && topicPresentation == TopicPresentation.NOTE -> "Topic note"
                            kind == KIND_CARD -> "Share card"
                            kind == KIND_QUOTE -> "Quote"
                            else -> "Note"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (kind == KIND_CARD && topic != null) {
                    PreviewToggle(
                        selected = topicPresentation,
                        onCard = onTopicCard,
                        onNote = onTopicNote
                    )
                }
            }

            AnimatedContent(
                targetState = Triple(kind, topicPresentation, draft.factText),
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 6 } + scaleIn(initialScale = 0.98f))
                        .togetherWith(fadeOut() + slideOutVertically { -it / 6 } + scaleOut(targetScale = 0.98f))
                },
                label = "communityPostPreview"
            ) { state ->
                val stateKind = state.first
                val statePresentation = state.second
                if (stateKind == KIND_CARD && statePresentation == TopicPresentation.CARD) {
                    if (topic != null) {
                        CommunityCardCanvas(
                            card = draftPreviewCard(draft),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
                            widthFraction = 1f
                        )
                    } else {
                        EmptyPreviewState(
                            title = "Choose a topic",
                            body = "The share card will appear here as soon as you pick one."
                        )
                    }
                } else if (stateKind == KIND_CARD && statePresentation == TopicPresentation.NOTE) {
                    TopicNotePreview(topic = topic, body = draft.factText)
                } else if (stateKind == KIND_QUOTE) {
                    TextPostPreview(
                        label = "QUOTE",
                        body = draft.factText,
                        credit = draft.byline
                    )
                } else {
                    TextPostPreview(
                        label = "NOTE",
                        body = draft.factText,
                        credit = ""
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewToggle(
    selected: TopicPresentation,
    onCard: () -> Unit,
    onNote: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp)
    ) {
        PreviewTogglePill("Card", selected == TopicPresentation.CARD, onCard)
        PreviewTogglePill("Note", selected == TopicPresentation.NOTE, onNote)
    }
}

@Composable
private fun PreviewTogglePill(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EmptyPreviewState(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TextPostPreview(
    label: String,
    body: String,
    credit: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = body.ifBlank { "Your words will appear here" },
                style = if (label == "QUOTE") MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                color = if (body.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                lineHeight = 29.sp
            )
            if (label == "QUOTE" && credit.isNotBlank()) {
                Text(
                    text = credit,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TopicNotePreview(
    topic: CurioTopic?,
    body: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "TOPIC",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = topic?.name ?: "Choose a topic",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    text = topicNoteCopy(topic, body),
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

private fun topicNoteCopy(topic: CurioTopic?, body: String): String {
    val title = topic?.name.orEmpty()
    val stripped = body.removePrefix(title).trim()
    return stripped.ifBlank { topic?.teaser.orEmpty().ifBlank { "Add your thought about this topic." } }
}

/**
 * The preview draws the SAME [CommunityCard] the wall will build from the
 * posted row — the canonical share-card pipeline, never a lookalike. The
 * author fields come from this device's own identity; the server stamps the
 * real ones on the posted row.
 */
@Composable
private fun draftPreviewCard(draft: CommunityCardDraft): CommunityCard {
    val context = LocalContext.current
    return CommunityCard(
        id = "preview",
        topicName = draft.topicName,
        categoryName = draft.categoryName,
        categoryGlyph = draft.categoryGlyph,
        accentHex = draft.accentHex,
        factText = draft.factText,
        caption = draft.caption,
        kind = draft.kind,
        style = draft.style,
        aspect = draft.aspect,
        bodyScale = draft.bodyScale,
        byline = draft.byline,
        authorId = "",
        authorHandle = "",
        authorDisplayName = AppPreferences.getDisplayName(context),
        authorName = AppPreferences.getUsername(context),
        authorAvatar = AppPreferences.getSocialAvatarStyle(context),
        createdAtMillis = System.currentTimeMillis(),
        expiresAtMillis = System.currentTimeMillis() + 86_400_000L,
        likeCount = 0,
        likedByMe = false,
        dislikeCount = 0,
        dislikedByMe = false,
        commentCount = 0,
        mine = true
    )
}

@Composable
private fun ComposerEditorField(
    kind: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val placeholder = when (kind) {
        KIND_CARD -> "Write the fact in your own words…"
        KIND_QUOTE -> "Write the quote…"
        else -> "What are you thinking about?"
    }
    LabeledTextField(
        label = when (kind) {
            KIND_CARD -> "Your version"
            KIND_QUOTE -> "The quote"
            else -> "Your note"
        },
        value = value,
        placeholder = placeholder,
        singleLine = false,
        minHeightDp = 130,
        onValueChange = onValueChange
    )
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    placeholder: String,
    singleLine: Boolean,
    minHeightDp: Int = 110,
    onValueChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().then(if (singleLine) Modifier else Modifier.height(minHeightDp.dp)),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                singleLine = singleLine,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = if (singleLine) ImeAction.Done else ImeAction.Default
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isBlank()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun TopicPicker(
    selected: CurioTopic?,
    expanded: Boolean,
    query: String,
    results: List<TopicIndexEntry>,
    accent: Color,
    onToggle: () -> Unit,
    onQuery: (String) -> Unit,
    onPick: (CurioTopic) -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Topic", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        selected?.name ?: "Choose something curious",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(shape = CircleShape, color = accent.copy(alpha = 0.12f)) {
                    CurioIcon(
                        name = if (expanded) CurioIcons.ChevronUp else CurioIcons.ChevronDown,
                        contentDescription = null,
                        tint = accent,
                        size = 18.dp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQuery,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
                        cursorBrush = SolidColor(accent),
                        decorationBox = { inner ->
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                                Box(Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
                                    if (query.isBlank()) Text("Search topics…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    inner()
                                }
                            }
                        }
                    )
                    results.forEach { result ->
                        Surface(
                            onClick = { onPick(result.topic) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(result.topic.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(result.topic.teaser, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardStylePicker(
    style: ShareCardStyle,
    aspect: ShareCardAspect,
    bodyScale: Float,
    accent: Color,
    onStyle: (ShareCardStyle) -> Unit,
    onAspect: (ShareCardAspect) -> Unit,
    onScale: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Card look", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareOption("Paper", style == ShareCardStyle.PAPER, accent) { onStyle(ShareCardStyle.PAPER) }
                ShareOption("Vinyl", style == ShareCardStyle.VINYL, accent) { onStyle(ShareCardStyle.VINYL) }
                ShareOption("Minimal", style == ShareCardStyle.MINIMAL, accent) { onStyle(ShareCardStyle.MINIMAL) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareOption("Classic", aspect == ShareCardAspect.CLASSIC, accent) { onAspect(ShareCardAspect.CLASSIC) }
                ShareOption("Portrait", aspect == ShareCardAspect.PORTRAIT, accent) { onAspect(ShareCardAspect.PORTRAIT) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Text size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ShareOption("Small", bodyScale < 0.95f, accent) { onScale(0.85f) }
                ShareOption("Normal", bodyScale in 0.95f..1.05f, accent) { onScale(1f) }
                ShareOption("Large", bodyScale > 1.05f, accent) { onScale(1.15f) }
            }
        }
    }
}

@Composable
private fun ShareOption(
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val ink = if (accent.luminance() > 0.55f) Color.Black else Color.White
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (active) accent else MaterialTheme.colorScheme.surface,
        contentColor = if (active) ink else MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}
