package com.curio.app.features.community

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.curio.app.data.AppPreferences
import com.curio.app.data.TopicIndexEntry
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityCardDraft
import com.curio.app.data.supabase.KIND_NOTE
import com.curio.app.data.supabase.KIND_QUOTE
import com.curio.app.ui.components.ShareCardAspect
import com.curio.app.ui.components.ShareCardStyle
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.themedAccent

/**
 * Full-screen social composer.  The topic mode intentionally delegates the
 * visual preview to CommunityCardCanvas, so the editor never grows a second
 * approximation of the share-card renderer.
 */
@Composable
internal fun CommunityPostScreen(
    onDismiss: () -> Unit,
    onPost: (CommunityCardDraft) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val index = remember { TopicJsonLoader.loadIndex() ?: emptyList() }

    var kind by remember { mutableStateOf("NOTE") }
    var query by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf<TopicIndexEntry?>(null) }
    var fact by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var credit by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(ShareCardStyle.PAPER) }
    var aspect by remember { mutableStateOf(ShareCardAspect.CLASSIC) }
    var bodyScale by remember { mutableStateOf(1f) }
    var showTopicPicker by remember { mutableStateOf(false) }

    val selectedCategory = selectedTopic?.category
    val accentHex = remember(selectedCategory) {
        selectedCategory?.let { category ->
            category.themedAccent().toArgb().let { argb ->
                String.format("#%08X", argb)
            }
        } ?: "#FF7A6B"
    }

    val draft = remember(
        kind, selectedTopic, fact, caption, credit, style, aspect, bodyScale, accentHex
    ) {
        CommunityCardDraft(
            topicName = selectedTopic?.topic.orEmpty(),
            categoryName = selectedTopic?.category?.displayName.orEmpty(),
            categorySlug = selectedTopic?.category?.id.orEmpty(),
            categoryGlyph = selectedTopic?.category?.iconGlyph.orEmpty(),
            accentHex = accentHex,
            factText = if (kind == "QUOTE") credit else fact,
            caption = caption,
            kind = when (kind) {
                "QUOTE" -> KIND_QUOTE
                "NOTE" -> KIND_NOTE
                else -> "CARD"
            },
            style = style.name,
            aspect = aspect.name,
            bodyScale = bodyScale,
            byline = if (kind == "QUOTE") credit else ""
        )
    }

    val canPost = when (kind) {
        "TOPIC" -> selectedTopic != null && (fact.isNotBlank() || caption.isNotBlank())
        "QUOTE" -> fact.isNotBlank() && credit.isNotBlank()
        else -> fact.isNotBlank()
    }
    val postScale by animateFloatAsState(
        targetValue = if (canPost) 1f else .96f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "postReady"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        CurioIcon(CurioIcons.Close, null, size = 22.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Create", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Make something worth keeping", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            if (canPost) {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                keyboard?.hide()
                                onPost(draft)
                            }
                        },
                        enabled = canPost,
                        modifier = Modifier.graphicsLayer(scaleX = postScale, scaleY = postScale),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) { Text("Post") }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("NOTE", "TOPIC", "QUOTE").forEach { option ->
                                FilterChip(
                                    selected = kind == option,
                                    onClick = {
                                        kind = option
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    label = { Text(option.lowercase().replaceFirstChar { it.uppercase() }) },
                                    leadingIcon = if (kind == option) ({ CurioIcon(CurioIcons.Check, null, size = 16.dp) }) else null
                                )
                            }
                        }
                    }

                    item {
                        AnimatedContent(
                            targetState = kind,
                            transitionSpec = { fadeIn() + scaleIn(initialScale = .98f) togetherWith fadeOut() },
                            label = "composerMode"
                        ) { mode ->
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                if (mode == "TOPIC") {
                                    if (selectedTopic == null) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                                            shape = RoundedCornerShape(28.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Text("Choose a topic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("The share card will be rebuilt from the same data in the feed and export.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                OutlinedTextField(
                                                    value = query,
                                                    onValueChange = { query = it },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    placeholder = { Text("Search topics") },
                                                    leadingIcon = { CurioIcon(CurioIcons.Search, null, size = 18.dp) }
                                                )
                                                val results = remember(query, index) {
                                                    val q = query.trim().lowercase()
                                                    if (q.isBlank()) index.take(8) else index.filter {
                                                        it.topic.lowercase().contains(q) || it.category.displayName.lowercase().contains(q)
                                                    }.take(12)
                                                }
                                                results.forEach { topic ->
                                                    TextButton(
                                                        onClick = {
                                                            selectedTopic = topic
                                                            showTopicPicker = false
                                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.fillMaxWidth()) {
                                                            Text(topic.topic, fontWeight = FontWeight.SemiBold)
                                                            Text(topic.category.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            CommunityCardCanvas(
                                                card = draftPreviewCard(draft),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(selectedTopic!!.topic, fontWeight = FontWeight.Bold)
                                                    Text(selectedTopic!!.category.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                AssistChip(onClick = { selectedTopic = null; query = "" }, label = { Text("Change") })
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = if (mode == "QUOTE") "A quote, kept clean." else "A thought, without the noise.",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            BasicTextField(
                                value = fact,
                                onValueChange = { fact = it },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = 26.sp),
                                decorationBox = { inner ->
                                    Box {
                                        if (fact.isBlank()) Text(if (kind == "QUOTE") "Write the quote…" else "What do you want to say?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        inner()
                                    }
                                }
                            )
                            if (kind == "QUOTE") {
                                OutlinedTextField(value = credit, onValueChange = { credit = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Who said it?") })
                            }
                            OutlinedTextField(value = caption, onValueChange = { caption = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Caption · optional") })
                        }
                    }

                    if (kind == "TOPIC" && selectedTopic != null) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Card design", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ShareCardStyle.entries.forEach { option ->
                                        FilterChip(selected = style == option, onClick = { style = option }, label = { Text(option.label) })
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ShareCardAspect.entries.forEach { option ->
                                        FilterChip(selected = aspect == option, onClick = { aspect = option }, label = { Text(option.label) })
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Compact" to .88f, "Auto" to 1f, "Large" to 1.12f).forEach { (label, value) ->
                                        FilterChip(selected = bodyScale == value, onClick = { bodyScale = value }, label = { Text(label) })
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

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
