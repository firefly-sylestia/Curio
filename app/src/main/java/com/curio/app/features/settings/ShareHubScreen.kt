package com.curio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryFamily
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.TopicIndexEntry
import com.curio.app.data.TopicJsonLoader
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioSearchField
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ShareCardAspect
import com.curio.app.ui.components.ShareCardStyle
import com.curio.app.ui.components.TopicShareCard
import com.curio.app.ui.components.shareComposableCard
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.themedAccent
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * v1 — the Share Hub: browse EVERY share-card design as full previews in a
 * grid, pick a design, search + pick any topic from the catalog, then share
 * that topic as a card in the chosen design (the TopicShareSheet experience,
 * unbound from a specific reveal — the whole catalog is searchable here).
 *
 * - The design grid renders every [ShareCardStyle] (Signature twice: the
 *   current + the restored classic designs), previewed with the picked topic
 *   (or a sample "Curiosity" card before any topic is chosen).
 * - The topic search filters the full topic index ([TopicJsonLoader.loadIndex])
 *   the same way the Topic Database does (title-first ranking).
 * - Share is enabled once a design AND a topic are picked; it exports the
 *   full-size card through [shareComposableCard] exactly like the share sheet.
 */
@Composable
fun ShareHubScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val gridState = rememberLazyGridState()
    val glassBackdrop = rememberLayerBackdrop()
    var query by rememberSaveable { mutableStateOf("") }
    var pickedTopicId by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedDesign by rememberSaveable { mutableStateOf<Int?>(null) }
    var aspect by rememberSaveable { mutableStateOf(ShareCardAspect.PORTRAIT) }

    // Full catalog index — loaded once, reused by the search below.
    val index by produceState<List<TopicIndexEntry>?>(null) {
        value = runCatching { TopicJsonLoader.loadIndex() }.getOrNull()
    }

    val needle = query.trim().lowercase()
    val results = remember(index, needle) {
        if (needle.isEmpty()) emptyList()
        else index.orEmpty()
            .filter { entry ->
                entry.nameKey.contains(needle) ||
                    entry.subtypeKey.contains(needle) ||
                    entry.bylineKey.contains(needle) ||
                    entry.teaserKey.contains(needle) ||
                    entry.tagKeys.any { it.contains(needle) }
            }
            .sortedWith(
                compareBy<TopicIndexEntry>(
                    { if (it.nameKey == needle) 0 else if (it.nameKey.startsWith(needle)) 1 else 2 },
                    { it.nameKey }
                )
            )
            .take(40)
    }

    val pickedTopic = remember(index, pickedTopicId) {
        index.orEmpty().firstOrNull { it.topic.id == pickedTopicId }?.topic
    }

    // Preview parameters — the picked topic, or a sample Curiosity card so the
    // grid always renders something before a topic is chosen.
    val wildcardCat = CurioCategories.byId(CategoryId.WILDCARD)
    val preview = pickedTopic?.let { topic ->
        val cat = CurioCategories.byId(topic.categoryId)
        ShareHubPreview(
            topicName = topic.name,
            categoryName = cat.displayName,
            glyph = cat.iconGlyph,
            accent = cat.themedAccent(),
            fact = if (cat.id == CategoryId.QUOTES) topic.name else topic.teaser,
            family = cat.family,
            byline = topic.byline
        )
    } ?: ShareHubPreview(
        topicName = "Curiosity",
        categoryName = wildcardCat.displayName,
        glyph = wildcardCat.iconGlyph,
        accent = wildcardCat.themedAccent(),
        fact = "A little curiosity, every day.",
        family = CategoryFamily.WILDCARD,
        byline = ""
    )

    val sharer = AppPreferences.getDisplayName(context).ifBlank { "" }
    val authority = remember { "${context.packageName}.fileprovider" }

    val wide = windowWidthSizeClass().isWide
    val selectedDesign = pickedDesign?.takeIf { it in HubDesigns.indices }?.let { HubDesigns[it] }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                heroPageBackground(
                    androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)
                )
            )
    ) {
        if (!wide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        LazyVerticalGrid(
            state = gridState,
            columns = if (wide) GridCells.Adaptive(minSize = 170.dp) else GridCells.Fixed(2),
            modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
            contentPadding = PaddingValues(
                start = wideContentEdgePadding(),
                end = wideContentEdgePadding(),
                top = SettingsHeroTotalHeight,
                bottom = 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Topic search ────────────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                CurioSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search any topic…"
                )
            }
            // Search results while typing
            if (needle.isNotEmpty()) {
                if (results.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "No topics match \"$query\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                        )
                    }
                } else {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(vertical = 4.dp)
                        ) {
                            results.forEachIndexed { i, entry ->
                                val cat = CurioCategories.byId(entry.topic.categoryId)
                                if (i > 0) androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                                Surface(
                                    onClick = {
                                        pickedTopicId = entry.topic.id
                                        query = ""
                                    },
                                    color = Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CurioIcon(
                                            name = cat.iconGlyph,
                                            contentDescription = null,
                                            tint = cat.themedAccent(),
                                            size = 18.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                entry.topic.name,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                listOfNotNull(
                                                    entry.topic.byline.ifBlank { null },
                                                    cat.displayName
                                                ).joinToString(" · "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        CurioIcon(
                                            name = if (entry.topic.id == pickedTopicId) CurioIcons.Check else CurioIcons.ChevronRight,
                                            contentDescription = null,
                                            tint = if (entry.topic.id == pickedTopicId) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            size = 18.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Picked topic chip
            if (pickedTopic != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val cat = CurioCategories.byId(pickedTopic.categoryId)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = cat.themedAccent().copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, cat.themedAccent().copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurioIcon(name = cat.iconGlyph, contentDescription = null, tint = cat.categoryInk(), size = 16.dp)
                            Text(
                                "${pickedTopic.name} · ${cat.displayName}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = cat.categoryInk(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                onClick = { pickedTopicId = null },
                                shape = CircleShape,
                                color = Color.Transparent
                            ) {
                                CurioIcon(
                                    name = CurioIcons.Close,
                                    contentDescription = "Clear topic",
                                    tint = cat.categoryInk().copy(alpha = 0.7f),
                                    size = 18.dp,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Design grid ─────────────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                CurioSectionLabel(
                    if (pickedTopic != null) "Designs — ${pickedTopic.name}" else "Designs — preview with any topic"
                )
            }
            HubDesigns.forEachIndexed { i, design ->
                item {
                    HubDesignCell(
                        design = design,
                        selected = i == pickedDesign,
                        preview = preview,
                        sharer = sharer,
                        aspect = aspect,
                        onSelect = { pickedDesign = i }
                    )
                }
            }

            // ── Aspect + Share ──────────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HubPill("9:16", aspect == ShareCardAspect.PORTRAIT) { aspect = ShareCardAspect.PORTRAIT }
                    HubPill("3:4", aspect == ShareCardAspect.CLASSIC) { aspect = ShareCardAspect.CLASSIC }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                val canShare = selectedDesign != null && pickedTopic != null
                Button(
                    onClick = {
                        val design = selectedDesign ?: return@Button
                        val topic = pickedTopic ?: return@Button
                        val cat = CurioCategories.byId(topic.categoryId)
                        val isQuotes = cat.id == CategoryId.QUOTES
                        val pw = 280.dp
                        val eh = pw * aspect.heightDp.toFloat() / aspect.widthDp.toFloat()
                        shareComposableCard(
                            context = context,
                            cardSize = DpSize(pw, eh),
                            authority = authority,
                            exportDensity = 4f,
                            card = {
                                TopicShareCard(
                                    topicName = topic.name,
                                    categoryName = cat.displayName,
                                    categoryGlyph = cat.iconGlyph,
                                    accent = cat.themedAccent(),
                                    factText = if (isQuotes) topic.name else topic.teaser,
                                    sharerName = sharer,
                                    aspect = aspect,
                                    style = design.style,
                                    ratingStars = null,
                                    categoryFamily = cat.family,
                                    quoteText = if (isQuotes) topic.name else null,
                                    quoteAuthor = if (isQuotes) topic.byline.ifBlank { null } else null,
                                    byline = topic.byline,
                                    classicSignature = design.classic
                                )
                            }
                        )
                    },
                    enabled = canShare,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(
                        if (selectedDesign == null) "Pick a design above"
                        else if (pickedTopic == null) "Search and pick a topic"
                        else "Share ${pickedTopic?.name ?: ""} · ${selectedDesign?.label ?: ""}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        }
        CurioVerticalScrollIndicator(
            state = gridState.scrollIndicatorState,
            onScrollBy = { gridState.dispatchRawDelta(it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 10.dp, bottom = 16.dp)
        )
        SettingsHeroHeader(
            title = "Share hub",
            subtitle = "Browse every design, pick a topic, share a card",
            onBack = { navController.popBackStack() },
            compact = wide,
            glassBackdrop = glassBackdrop
        )
    }
}

/** One design cell in the grid — a full mini preview of [TopicShareCard]. */
@Composable
private fun HubDesignCell(
    design: HubDesign,
    selected: Boolean,
    preview: ShareHubPreview,
    sharer: String,
    aspect: ShareCardAspect,
    onSelect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val isQuotes = preview.categoryName == "Quotes"
        Surface(
            onClick = onSelect,
            shape = RoundedCornerShape(10.dp),
            color = Color.Transparent,
            border = BorderStroke(
                if (selected) 3.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect.widthDp.toFloat() / aspect.heightDp.toFloat())
                .shadow(if (selected) 6.dp else 2.dp, RoundedCornerShape(10.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                TopicShareCard(
                    topicName = preview.topicName,
                    categoryName = preview.categoryName,
                    categoryGlyph = preview.glyph,
                    accent = preview.accent,
                    factText = preview.fact,
                    sharerName = sharer,
                    aspect = aspect,
                    style = design.style,
                    categoryFamily = preview.family,
                    quoteText = if (isQuotes) preview.fact else null,
                    quoteAuthor = if (isQuotes) preview.byline.ifBlank { null } else null,
                    byline = preview.byline,
                    classicSignature = design.classic
                )
            }
        }
        Text(
            design.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 1
        )
    }
}

/** Small round pill (the share sheet's [Pill] language, local copy). */
@Composable
private fun HubPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Resolved preview parameters for one topic (or the sample card). */
private data class ShareHubPreview(
    val topicName: String,
    val categoryName: String,
    val glyph: String,
    val accent: Color,
    val fact: String,
    val family: CategoryFamily,
    val byline: String
)

/** One design entry in the grid — a style plus its Signature variant flag. */
private data class HubDesign(
    val label: String,
    val style: ShareCardStyle,
    val classic: Boolean = false
)

/** Every share-card design, including both Signature variants. */
private val HubDesigns = listOf(
    HubDesign("Paper", ShareCardStyle.PAPER),
    HubDesign("Vinyl", ShareCardStyle.VINYL),
    HubDesign("Collage", ShareCardStyle.COLLAGE),
    HubDesign("Clean", ShareCardStyle.NEUMORPHIC),
    HubDesign("Editorial", ShareCardStyle.EDITORIAL),
    HubDesign("Minimal", ShareCardStyle.MINIMAL),
    HubDesign("Signature", ShareCardStyle.SIGNATURE),
    HubDesign("Signature · Classic", ShareCardStyle.SIGNATURE, classic = true),
    HubDesign("Custom", ShareCardStyle.CUSTOM)
)
