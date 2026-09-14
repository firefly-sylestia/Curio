package com.curio.app.features.community

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
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
import com.curio.app.data.supabase.CommunityCardDraft
import com.curio.app.data.supabase.KIND_CARD
import com.curio.app.data.supabase.KIND_NOTE
import com.curio.app.data.supabase.KIND_QUOTE
import com.curio.app.ui.components.ShareCardAspect
import com.curio.app.ui.components.ShareCardStyle
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionColor
import kotlinx.coroutines.launch

/**
 * Full-screen social composer. This intentionally behaves like a destination,
 * not a bottom sheet: the writing surface, preview, topic search and publish
 * action all belong to one focused creation flow.
 */
@Composable
internal fun CommunityPostScreen(
    onDismiss: () -> Unit,
    onPost: (CommunityCardDraft) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var kind by remember { mutableStateOf(KIND_NOTE) }
    var text by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var credit by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf<CurioTopic?>(null) }
    var index by remember { mutableStateOf<List<TopicIndexEntry>>(emptyList()) }
    var topicOpen by remember { mutableStateOf(false) }
    var style by remember { mutableStateOf(ShareCardStyle.PAPER) }
    var aspect by remember { mutableStateOf(ShareCardAspect.CLASSIC) }
    var bodyScale by remember { mutableStateOf(1f) }
    var posting by remember { mutableStateOf(false) }
    var justSelected by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val writerRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        index = runCatching { TopicJsonLoader.loadIndex() }.getOrDefault(emptyList())
        writerRequester.requestFocus()
    }

    BackHandler(onBack = onDismiss)

    val topicResults = remember(index, query) {
        val q = query.trim()
        if (q.isBlank()) index.take(12)
        else index.asSequence()
            .sortedWith(
                compareByDescending<TopicIndexEntry> { it.topic.name.contains(q, true) }
                    .thenBy { it.topic.name }
            )
            .filter {
                it.topic.name.contains(q, true) ||
                    it.topic.byline.contains(q, true) ||
                    it.topic.tags.any { tag -> tag.contains(q, true) }
            }
            .take(18)
            .toList()
    }

    val canPost = when (kind) {
        KIND_CARD -> topic != null && (text.isNotBlank() || caption.isNotBlank())
        KIND_QUOTE -> text.isNotBlank() && credit.isNotBlank()
        else -> text.isNotBlank()
    }

    val draft = remember(kind, topic, text, caption, credit, style, aspect, bodyScale) {
        val selected = topic
        CommunityCardDraft(
            topicName = if (kind == KIND_CARD) selected?.name.orEmpty() else "",
            categoryName = if (kind == KIND_CARD) selected?.categoryId?.let { CurioCategories.byId(it).displayName }.orEmpty() else "",
            categorySlug = if (kind == KIND_CARD) selected?.categoryId?.name.orEmpty().lowercase() else "",
            categoryGlyph = if (kind == KIND_CARD) selected?.categoryId?.let { CurioCategories.byId(it).iconGlyph }.orEmpty() else "",
            accentHex = if (kind == KIND_CARD) selected?.categoryId?.let { CurioCategories.byId(it).themedAccent().toArgbHex() }.orEmpty() else "",
            factText = if (kind == KIND_CARD) text else "",
            caption = caption,
            kind = kind,
            style = style.name,
            aspect = aspect.name,
            bodyScale = bodyScale,
            byline = credit
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ComposerIconButton(CurioIcons.Close, "Close", onDismiss)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Create", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(
                            when (kind) {
                                KIND_CARD -> "A topic, made yours"
                                KIND_QUOTE -> "Keep someone else's words close"
                                else -> "Put the thought somewhere"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ComposerPostButton(
                        enabled = canPost && !posting,
                        posting = posting,
                        onClick = {
                            if (!canPost || posting) return@ComposerPostButton
                            posting = true
                            focusManager.clearFocus()
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            scope.launch {
                                onPost(draft)
                                posting = false
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ComposerModeChip("Note", kind == KIND_NOTE) {
                        kind = KIND_NOTE; topicOpen = false; credit = ""
                    }
                    ComposerModeChip("Topic", kind == KIND_CARD) {
                        kind = KIND_CARD; topicOpen = true
                    }
                    ComposerModeChip("Quote", kind == KIND_QUOTE) {
                        kind = KIND_QUOTE; topicOpen = false
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "preview") {
                        AnimatedContent(targetState = kind, label = "post-kind") { mode ->
                            when (mode) {
                                KIND_CARD -> {
                                    Surface(
                                        shape = RoundedCornerShape(28.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            if (topic != null) {
                                                CommunityCardCanvas(
                                                    card = draftPreviewCard(draft),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(20.dp)),
                                                    widthFraction = 1f
                                                )
                                            } else {
                                                EmptyTopicPreview()
                                            }
                                            Text(
                                                if (topic == null) "Choose a topic to build the card" else "Live share-card preview",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                else -> SocialComposerTextPreview(
                                    kind = mode,
                                    body = text,
                                    credit = credit
                                )
                            }
                        }
                    }

                    item(key = "writer") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                when (kind) {
                                    KIND_CARD -> "Your fact"
                                    KIND_QUOTE -> "The quote"
                                    else -> "Your note"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            BasicTextField(
                                value = text,
                                onValueChange = { text = it.take(if (kind == KIND_CARD) 700 else 1200) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(16.dp)
                                    .focusRequester(writerRequester),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                cursorBrush = SolidColor(curioDialogActionColor()),
                                decorationBox = { inner ->
                                    Box {
                                        if (text.isBlank()) {
                                            Text(
                                                when (kind) {
                                                    KIND_CARD -> "Write the version of this fact you want people to remember…"
                                                    KIND_QUOTE -> "Write the quote…"
                                                    else -> "What are you thinking about?"
                                                },
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                            Text(
                                "${text.length} / ${if (kind == KIND_CARD) 700 else 1200}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }

                    if (kind == KIND_CARD) {
                        item(key = "topic-picker") {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Topic", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                                            Text(
                                                topic?.name ?: "Pick what this is about",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (topic == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        TextButton(onClick = { topicOpen = !topicOpen }) {
                                            Text(if (topicOpen) "Done" else "Change")
                                        }
                                    }
                                    AnimatedVisibility(topicOpen, enter = fadeIn() + slideInVertically { -it / 3 }, exit = fadeOut()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            BasicTextField(
                                                value = query,
                                                onValueChange = { query = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                textStyle = MaterialTheme.typography.bodyMedium,
                                                singleLine = true,
                                                decorationBox = { inner ->
                                                    Box {
                                                        if (query.isBlank()) Text("Search topics…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        inner()
                                                    }
                                                }
                                            )
                                            Column(
                                                modifier = Modifier.height(260.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                topicResults.forEach { entry ->
                                                    TopicResultRow(entry, entry.topic == topic, onClick = {
                                                        topic = entry.topic
                                                        query = ""
                                                        topicOpen = false
                                                        justSelected++
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "style") {
                            ComposerChoiceRow(
                                title = "Look",
                                values = ShareCardStyle.entries.take(6).map { it.label },
                                selected = style.label,
                                onSelected = { style = ShareCardStyle.entries.firstOrNull { it.label == it } ?: style }
                            )
                        }
                        item(key = "shape") {
                            ComposerChoiceRow(
                                title = "Shape",
                                values = ShareCardAspect.entries.map { it.label },
                                selected = aspect.label,
                                onSelected = { selected -> aspect = ShareCardAspect.entries.first { it.label == selected } }
                            )
                        }
                    }

                    if (kind == KIND_QUOTE) {
                        item(key = "credit") {
                            ComposerField("Credit", credit, "Who said it?", { credit = it.take(120) })
                        }
                    }

                    item(key = "caption") {
                        ComposerField(
                            title = if (kind == KIND_CARD) "Caption" else "Add a caption",
                            value = caption,
                            placeholder = "Optional",
                            onChange = { caption = it.take(180) }
                        )
                    }

                    if (kind == KIND_CARD) {
                        item(key = "scale") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Text size", style = MaterialTheme.typography.labelMedium)
                                listOf(0.9f, 1f, 1.1f).forEach { value ->
                                    FilterChip(selected = bodyScale == value, onClick = { bodyScale = value }, label = { Text(if (value == 1f) "Auto" else if (value < 1f) "Compact" else "Large") })
                                }
                            }
                        }
                    }

                    item(key = "tip") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurioIcon(CurioIcons.Info, null, MaterialTheme.colorScheme.onSurfaceVariant, 18.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (kind == KIND_CARD) "The card is rebuilt from these exact fields on every device." else "Short, text-first posts stay lightweight and disappear with the wall.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun ComposerPostButton(enabled: Boolean, posting: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(enabled) {
        if (enabled) {
            scale.snapTo(0.94f)
            scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 700f))
        }
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = if (enabled) curioDialogActionColor() else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(if (posting) "Posting…" else "Post", modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ComposerIconButton(icon: String, label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.size(42.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(icon, label, MaterialTheme.colorScheme.onSurface, 20.dp)
        }
    }
}

@Composable
private fun SocialComposerTextPreview(kind: String, body: String, credit: String) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (kind == KIND_QUOTE) "QUOTE" else "NOTE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                body.ifBlank { "Your words will appear here" },
                style = if (kind == KIND_QUOTE) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                color = if (body.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            if (kind == KIND_QUOTE && credit.isNotBlank()) Text("— $credit", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyTopicPreview() {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().height(330.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CurioIcon(CurioIcons.Search, null, MaterialTheme.colorScheme.onSurfaceVariant, 28.dp)
            Spacer(Modifier.height(10.dp))
            Text("Pick a topic", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun TopicResultRow(entry: TopicIndexEntry, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) curioDialogActionColor().copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.topic.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(entry.topic.byline.ifBlank { entry.topic.subtype }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) CurioIcon(CurioIcons.Check, null, curioDialogActionColor(), 18.dp)
        }
    }
}

@Composable
private fun ComposerChoiceRow(title: String, values: List<String>, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value -> FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(value) }) }
        }
    }
}

@Composable
private fun ComposerField(title: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            }
        )
    }
}

private fun draftPreviewCard(draft: CommunityCardDraft): CommunityCard {
    return CommunityCard(
        id = "preview",
        topicName = draft.topicName,
        categoryName = draft.categoryName,
        categorySlug = draft.categorySlug,
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
        authorDisplayName = AppPreferences.getDisplayName(LocalContext.current),
        authorName = AppPreferences.getUsername(LocalContext.current),
        authorAvatar = AppPreferences.getSocialAvatarStyle(LocalContext.current),
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

private fun com.curio.app.data.CategoryId.themedAccentHex(): String = themedAccent().toArgbHex()

private fun androidx.compose.ui.graphics.Color.toArgbHex(): String =
    "#%08X".format(toArgb())
