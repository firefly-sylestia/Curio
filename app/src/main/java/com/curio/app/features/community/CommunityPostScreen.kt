package com.curio.app.features.community

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.curio.app.ui.theme.themedAccent
import kotlinx.coroutines.launch

@Composable
internal fun CommunityPostScreen(
    onDismiss: () -> Unit,
    onPost: (CommunityCardDraft) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val writerRequester = remember { FocusRequester() }
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
    val intro = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        index = TopicJsonLoader.loadIndex() ?: emptyList()
        intro.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 420f))
        writerRequester.requestFocus()
    }
    BackHandler(onBack = onDismiss)

    val selectedCategory = topic?.categoryId?.let(CurioCategories::byId)
    val accent = selectedCategory?.themedAccent() ?: MaterialTheme.colorScheme.primary
    val accentSoft = lerp(MaterialTheme.colorScheme.surface, accent, 0.10f)

    val topicResults = remember(index, query) {
        val q = query.trim()
        if (q.isBlank()) index.take(18) else index.asSequence()
            .sortedWith(compareByDescending<TopicIndexEntry> { it.topic.name.contains(q, true) }.thenBy { it.topic.name })
            .filter { it.topic.name.contains(q, true) || it.topic.byline.contains(q, true) || it.topic.tags.any { tag -> tag.contains(q, true) } }
            .take(18)
            .toList()
    }

    val accentHex = selectedCategory?.let { "%08X".format(it.themedAccent().toArgb()) }.orEmpty()
    val draft = CommunityCardDraft(
        topicName = if (kind == KIND_CARD) topic?.name.orEmpty() else "",
        categoryName = if (kind == KIND_CARD) selectedCategory?.displayName.orEmpty() else "",
        categorySlug = if (kind == KIND_CARD) topic?.categoryId?.name.orEmpty().lowercase() else "",
        categoryGlyph = if (kind == KIND_CARD) selectedCategory?.iconGlyph.orEmpty() else "",
        accentHex = if (kind == KIND_CARD) accentHex else "",
        factText = text,
        caption = caption,
        kind = kind,
        style = style.name,
        aspect = aspect.name,
        bodyScale = bodyScale,
        byline = credit
    )
    val canPost = when (kind) {
        KIND_CARD -> topic != null && (text.isNotBlank() || caption.isNotBlank())
        KIND_QUOTE -> text.isNotBlank() && credit.isNotBlank()
        else -> text.isNotBlank()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(WindowInsets.statusBars.asPaddingValues())) {
                ComposerHeader(kind, posting, canPost, accent, onDismiss) {
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
                ComposerModeRail(kind, accent) { selected ->
                    kind = selected
                    topicOpen = selected == KIND_CARD
                    if (selected != KIND_QUOTE) credit = ""
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().imePadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "hero") { ComposerHero(kind, topic, selectedCategory, accent, accentSoft, intro.value) }
                    item(key = "writer") { ComposerWriter(kind, text, accent, writerRequester) { text = it.take(if (kind == KIND_CARD) 700 else 1200) } }
                    item(key = "topic") {
                        AnimatedVisibility(
                            visible = kind == KIND_CARD,
                            enter = fadeIn() + slideInVertically { it / 5 },
                            exit = fadeOut() + slideOutVertically { -it / 5 }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TopicPickerCard(topic, topicOpen, query, accent, topicResults, { topicOpen = !topicOpen }, { query = it }) { picked ->
                                    topic = picked
                                    if (text.isBlank()) text = picked.teaser
                                    query = ""
                                    topicOpen = false
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                CaptionCard(caption, accent) { caption = it.take(180) }
                                CardStyleControls(style, aspect, bodyScale, accent, { style = it }, { aspect = it }, { bodyScale = it })
                            }
                        }
                    }
                    item(key = "credit") {
                        AnimatedVisibility(
                            visible = kind == KIND_QUOTE,
                            enter = fadeIn() + slideInVertically { it / 5 },
                            exit = fadeOut() + slideOutVertically { -it / 5 }
                        ) {
                            ComposerTextFieldCard("Credit", credit, "Who said it?", accent, 120) { credit = it }
                        }
                    }
                    item(key = "hint") { ComposerFooterHint(kind) }
                }
            }
        }
    }
}

@Composable
private fun ComposerHeader(kind: String, posting: Boolean, canPost: Boolean, accent: Color, onDismiss: () -> Unit, onPost: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        ComposerIconButton(onDismiss)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Create a post", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(when (kind) { KIND_CARD -> "Share something you discovered"; KIND_QUOTE -> "Keep someone else's words close"; else -> "Put a thought somewhere" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(onClick = onPost, enabled = canPost && !posting, shape = RoundedCornerShape(16.dp), color = if (canPost && !posting) accent else MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = if (canPost && !posting) (if (accent.luminance() > .60f) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant) {
            Row(Modifier.padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(if (posting) "Posting" else "Post", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                if (!posting) CurioIcon(name = CurioIcons.Check, contentDescription = null, tint = if (canPost) (if (accent.luminance() > .60f) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
            }
        }
    }
}

@Composable
private fun ComposerIconButton(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        CurioIcon(name = CurioIcons.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface, size = 19.dp, modifier = Modifier.padding(10.dp))
    }
}

@Composable
private fun ComposerModeRail(selected: String, accent: Color, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PostModePill("Note", KIND_NOTE, selected, accent, "A thought") { onSelect(KIND_NOTE) }
        PostModePill("Topic", KIND_CARD, selected, accent, "A discovery") { onSelect(KIND_CARD) }
        PostModePill("Quote", KIND_QUOTE, selected, accent, "Words worth keeping") { onSelect(KIND_QUOTE) }
    }
}

@Composable
private fun PostModePill(title: String, value: String, selected: String, accent: Color, subtitle: String, onClick: () -> Unit) {
    val active = selected == value
    Surface(onClick = onClick, shape = RoundedCornerShape(15.dp), color = if (active) accent else MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = if (active) 2.dp else 0.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = if (active) (if (accent.luminance() > .60f) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = if (active) (if (accent.luminance() > .60f) Color.Black.copy(alpha = .72f) else Color.White.copy(alpha = .78f)) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ComposerHero(kind: String, topic: CurioTopic?, category: com.curio.app.data.CurioCategory?, accent: Color, accentSoft: Color, intro: Float) {
    val scale by animateFloatAsState(targetValue = 0.96f + intro * 0.04f, animationSpec = spring(dampingRatio = .82f), label = "composerHeroScale")
    val title = when (kind) { KIND_CARD -> topic?.name ?: "A little piece of knowledge"; KIND_QUOTE -> "Words worth keeping"; else -> "Something on your mind" }
    Surface(shape = RoundedCornerShape(28.dp), color = if (kind == KIND_CARD && topic != null) accentSoft else MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = if (kind == KIND_CARD && topic != null) accent else MaterialTheme.colorScheme.surfaceContainerHighest) {
                    CurioIcon(name = when (kind) { KIND_CARD -> category?.iconGlyph ?: CurioIcons.Note; KIND_QUOTE -> CurioIcons.MenuBook; else -> CurioIcons.Note }, contentDescription = null, tint = if (kind == KIND_CARD && topic != null) (if (accent.luminance() > .60f) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurface, size = 20.dp, modifier = Modifier.padding(10.dp))
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text(when (kind) { KIND_CARD -> if (topic == null) "Topic post" else category?.displayName ?: "Topic"; KIND_QUOTE -> "Quote"; else -> "Note" }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("The preview follows your writing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AnimatedContent(targetState = title, label = "composerHeroTitle", transitionSpec = { (fadeIn() + slideInVertically { it / 6 }).togetherWith(fadeOut() + slideOutVertically { -it / 6 }) }) { value -> Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis) }
            if (kind == KIND_CARD && topic != null) Text(topic.teaser, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .88f), maxLines = 4, overflow = TextOverflow.Ellipsis)
            else Text(when (kind) { KIND_QUOTE -> "Add your quote below, then give its voice a little credit."; else -> "Start with the sentence you would actually want another curious person to read." }, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
        }
    }
}

@Composable
private fun ComposerWriter(kind: String, value: String, accent: Color, focusRequester: FocusRequester, onValueChange: (String) -> Unit) {
    val placeholder = when (kind) { KIND_CARD -> "Write the fact in your own words…"; KIND_QUOTE -> "Write the quote…"; else -> "What are you thinking about?" }
    val label = when (kind) { KIND_CARD -> "Your version"; KIND_QUOTE -> "The quote"; else -> "Your note" }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                Text("${value.length}/${if (kind == KIND_CARD) 700 else 1200}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp).focusRequester(focusRequester), textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp, color = MaterialTheme.colorScheme.onSurface), cursorBrush = SolidColor(accent), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default), decorationBox = { inner -> Box { if (value.isBlank()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); inner() } })
        }
    }
}

@Composable
private fun TopicPickerCard(topic: CurioTopic?, topicOpen: Boolean, query: String, accent: Color, results: List<TopicIndexEntry>, onToggle: () -> Unit, onQueryChange: (String) -> Unit, onPick: (CurioTopic) -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Topic", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    Text(topic?.name ?: "Choose a topic", style = MaterialTheme.typography.bodyMedium, color = if (topic == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(onClick = onToggle, shape = RoundedCornerShape(12.dp), color = if (topic != null) lerp(MaterialTheme.colorScheme.surfaceContainerHighest, accent, .14f) else accent, contentColor = if (topic != null) MaterialTheme.colorScheme.onSurface else if (accent.luminance() > .60f) Color.Black else Color.White) {
                    Text(if (topicOpen) "Done" else if (topic == null) "Choose" else "Change", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
            AnimatedVisibility(visible = topicOpen, enter = fadeIn() + slideInVertically { -it / 4 } + scaleIn(initialScale = .98f), exit = fadeOut() + slideOutVertically { -it / 4 } + scaleOut(targetScale = .98f)) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    BasicTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 14.dp, vertical = 12.dp), textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface), singleLine = true, decorationBox = { inner -> Box { if (query.isBlank()) Text("Search topics…", color = MaterialTheme.colorScheme.onSurfaceVariant); inner() } })
                    Column(Modifier.height(244.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        results.forEach { entry ->
                            Surface(onClick = { onPick(entry.topic) }, shape = RoundedCornerShape(15.dp), color = if (entry.topic == topic) lerp(MaterialTheme.colorScheme.surfaceContainerHigh, accent, .18f) else MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = lerp(MaterialTheme.colorScheme.surfaceContainerHigh, accent, .12f)) {
                                        CurioIcon(name = CurioIcons.Note, contentDescription = null, tint = accent, size = 17.dp, modifier = Modifier.padding(8.dp))
                                    }
                                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                                        Text(entry.topic.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(entry.topic.byline, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptionCard(value: String, accent: Color, onValueChange: (String) -> Unit) = ComposerTextFieldCard("Caption", value, "Give the post a little context…", accent, 180, onValueChange)

@Composable
private fun ComposerTextFieldCard(label: String, value: String, hint: String, accent: Color, maxChars: Int, onValueChange: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                Text("${value.length}/$maxChars", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(value = value, onValueChange = { onValueChange(it.take(maxChars)) }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 13.dp, vertical = 12.dp), textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface), cursorBrush = SolidColor(accent), singleLine = true, decorationBox = { inner -> Box { if (value.isBlank()) Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant); inner() } })
        }
    }
}

@Composable
private fun CardStyleControls(style: ShareCardStyle, aspect: ShareCardAspect, bodyScale: Float, accent: Color, onStyle: (ShareCardStyle) -> Unit, onAspect: (ShareCardAspect) -> Unit, onScale: (Float) -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            ChoiceRail("Look", ShareCardStyle.entries.map { it.label }, style.label, accent) { label -> onStyle(ShareCardStyle.entries.firstOrNull { it.label == label } ?: style) }
            ChoiceRail("Shape", ShareCardAspect.entries.map { it.label }, aspect.label, accent) { label -> onAspect(ShareCardAspect.entries.firstOrNull { it.label == label } ?: aspect) }
            ChoiceRail("Text", listOf("Compact", "Auto", "Large"), when { bodyScale < 1f -> "Compact"; bodyScale > 1f -> "Large"; else -> "Auto" }, accent) { label -> onScale(when (label) { "Compact" -> .9f; "Large" -> 1.1f; else -> 1f }) }
        }
    }
}

@Composable
private fun ChoiceRail(label: String, values: List<String>, selected: String, accent: Color, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            values.forEach { value ->
                val active = value == selected
                Surface(onClick = { onSelected(value) }, shape = RoundedCornerShape(13.dp), color = if (active) accent else MaterialTheme.colorScheme.surface, contentColor = if (active) (if (accent.luminance() > .60f) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurface) {
                    Text(value, modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium))
                }
            }
        }
    }
}

@Composable
private fun ComposerFooterHint(kind: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(when (kind) { KIND_CARD -> "Tip: the topic becomes the identity of the post. Keep your fact specific and readable."; KIND_QUOTE -> "Tip: short quotes tend to breathe better in the feed."; else -> "Tip: one clear thought usually makes a stronger post than three half-finished ones." }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(13.dp))
    }
}
