package com.curio.app.features.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryFamily
import com.curio.app.data.CurioQuests
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioCardHeader
import com.curio.app.ui.components.CurioSearchField
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioSettingsRow
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.components.PaperTitleLines
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.SoftTornSheetShape
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.toHsl

/** Fixed tear seed — every settings header tears in the SAME bold pattern
 *  (Settings's own pattern; Profile wears 0xC0FEE). Never re-rolls. */
private const val SETTINGS_HERO_TEAR_SEED = 0x5EED
/** The hero header's solid body height — compact ("just at the header"):
 *  back pill on top, title + subtitle pinned just above the tear. Held
 *  with flex slack so the title block clears the tear even at large font
 *  scales. */
private val SettingsHeroBannerHeight = 180.dp
/** Extra layout space reserved for the under-sheet below the torn banner. */
private val SettingsHeroSheetExtent = 24.dp
/** Total header footprint — the torn banner plus its under-sheet extent.
 *  Public so every settings screen can start its scroll content just below
 *  the hero (the hero overlays the content, letting rows disappear under
 *  the ragged tear as they scroll). */
val SettingsHeroTotalHeight = SettingsHeroBannerHeight + SettingsHeroSheetExtent

/** One mirrored hero watermark pair — the left glyph mirrors the right
 *  (the Profile/Home quest hero construction, adapted for Settings). */
private data class SettingsHeroPair(
    val biasX: Float,
    val biasY: Float,
    val size: Dp,
    val rotation: Float,
    val alpha: Float
)

/**
 * The Settings hero header — the PROFILE hero's style, compact: a solid
 * rose torn banner (the same bold SoftTorn tear + theme under-sheet as
 * Profile/Home), the mirrored watermark collage of the wildcard family's
 * symbols, a back pill over the banner, and the title + subtitle pinned
 * just above the tear. Shared by every settings screen so the whole
 * Settings family wears the same hero-style header.
 *
 * v26 — optional hero action pills (the [trailing] slot rides the top row
 * next to the back pill, Cabinet-style ink-glass pills) and an optional
 * morph-open search field that replaces the title block while active
 * (the same scale/fade morph as the Cabinet hero). When [searchActive] the
 * trailing pills are swapped for a single Cancel pill.
 */
@Composable
fun SettingsHeroHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    // Narrow the torn banner on landscape/tablet so it doesn't cover
    // most of the already-short vertical space.
    compact: Boolean = false,
    // v26 — optional action pills riding the top row beside the back pill.
    // Receives the hero's readable ink for the pill glass. Passed as a NAMED
    // argument (not trailing-lambda syntax): the @Composable slot isn't the
    // last parameter, and the trailing form fails to bind under K2.
    trailing: (@Composable (ink: Color) -> Unit)? = null,
    searchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    searchFocus: FocusRequester? = null,
    searchPlaceholder: String = "Search…"
) {
    val bannerHeight = if (compact) 140.dp else SettingsHeroBannerHeight
    val totalHeight = bannerHeight + SettingsHeroSheetExtent
    val heroTornShape = remember(SETTINGS_HERO_TEAR_SEED) { SoftTornBottomShape(SETTINGS_HERO_TEAR_SEED, bold = true) }
    val sheetShape = remember(SETTINGS_HERO_TEAR_SEED) {
        SoftTornSheetShape(SETTINGS_HERO_TEAR_SEED, lip = 10.dp, baseline = 14.dp, bold = true)
    }
    val fill = settingsRoseAccent()
    val ink = settingsReadableInk(fill)
    // v12 — AMOLED: the pure-black banner carries the rose accent through the
    // watermark collage + back pill (the black-glass language); the title
    // stays white for readability.
    val symbolTint = if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED)
        CurioColors.HomeRosewood else ink
    Box(
        modifier = Modifier
            .fillMaxWidth()            .height(totalHeight)
        ) {
            // ── Under-sheet — the shared white paper layer, so the tear stays
            // bright beneath the rose hero in every theme. AMOLED: the sheet
            // turns a soft rose so the torn edge keeps reading through the
            // up-bites of the pure-black banner (black-on-black would hide
            // the seam), carrying the accent of the color.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .offset(y = bannerHeight - 18.dp)
                .clip(sheetShape)                    .background(
                    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED)
                        CurioColors.HomeRosewood.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surface
                )

        )
        // ── Torn-edge shadow — hairline dark rim under the seam.
        Box(
            modifier = Modifier
                .fillMaxWidth()                .height(bannerHeight)
                .offset(y = 1.dp)
                .clip(heroTornShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f))
            )
            // ── Solid rose banner, torn bottom edge ────────────────────────
            Surface(
                shape = heroTornShape,
                color = fill,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mirrored watermark collage — the wildcard family's symbols
                // pop around the banner edges (settings is category-neutral;
                // the Profile hero's exact collage construction).
                val symbols = CurioIcons.heroWatermarkSymbols(CategoryFamily.WILDCARD)
                val pairs = listOf(
                    SettingsHeroPair(biasX = 0.93f, biasY = -0.85f, size = 44.dp, rotation = 12f, alpha = 0.11f),
                    SettingsHeroPair(biasX = 0.55f, biasY = -0.64f, size = 48.dp, rotation = 8f, alpha = 0.13f),
                    SettingsHeroPair(biasX = 0.94f, biasY = -0.12f, size = 56.dp, rotation = 14f, alpha = 0.14f),
                    SettingsHeroPair(biasX = 0.56f, biasY = 0.54f, size = 50.dp, rotation = 10f, alpha = 0.13f),
                    SettingsHeroPair(biasX = 0.94f, biasY = 0.80f, size = 44.dp, rotation = 6f, alpha = 0.11f)
                )
                pairs.forEachIndexed { i, pair ->
                    SettingsHeroSymbol(symbols[i * 2], BiasAlignment(-pair.biasX, pair.biasY), pair.size, -pair.rotation, pair.alpha, symbolTint)
                    SettingsHeroSymbol(symbols[i * 2 + 1], BiasAlignment(pair.biasX, pair.biasY), pair.size, pair.rotation, pair.alpha, symbolTint)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)
                ) {
                    // ── Top row — back pill + optional hero action pills ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CurioBackButton(
                            onClick = onBack,
                            containerColor = symbolTint.copy(alpha = 0.18f),
                            contentColor = symbolTint,
                            disableRipple = true
                        )
                        if (searchActive) {
                            // Search is open — the trailing pills are swapped
                            // for a single Cancel pill (Cabinet's contract).
                            SettingsHeroActionPill(
                                onClick = onCloseSearch,
                                label = "Cancel",
                                glyph = CurioIcons.Close,
                                contentDescription = "Close search",
                                ink = ink
                            )
                        } else if (trailing != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                trailing(ink)
                            }
                        }
                    }
                    // Flex spacer — pins the title/search block just above the tear.
                    Spacer(Modifier.weight(1f))
                    // ── Title + subtitle OR the morph-open search field —
                    //    the search bar scales in from the pill's position
                    //    when opened, and the title fades back in when
                    //    closed (the Cabinet hero's search morph).
                    AnimatedContent(
                        targetState = searchActive,
                        transitionSpec = {
                            if (targetState) {
                                // Search opening: scale in + fade in
                                (scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.92f)
                                    + fadeIn(tween(280, easing = FastOutSlowInEasing)))
                                    .togetherWith(fadeOut(tween(200)))
                            } else {
                                // Search closing: title fades back in
                                (fadeIn(tween(280, easing = FastOutSlowInEasing)))
                                    .togetherWith(
                                        scaleOut(tween(200, easing = FastOutSlowInEasing), targetScale = 0.92f)
                                            + fadeOut(tween(200))
                                    )
                            }
                        },
                        label = "settingsSearchExpand"
                    ) { active ->
                        if (active) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = {
                                    Text(
                                        searchPlaceholder,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    CurioIcon(
                                        CurioIcons.Search, null,
                                        tint = ink,
                                        size = 20.dp
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            CurioIcon(
                                                CurioIcons.Close,
                                                "Clear search",
                                                tint = ink.copy(alpha = 0.85f),
                                                size = 20.dp
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {}),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ink.copy(alpha = 0.16f),
                                    unfocusedContainerColor = ink.copy(alpha = 0.16f),
                                    focusedBorderColor = ink.copy(alpha = 0.55f),
                                    unfocusedBorderColor = ink.copy(alpha = 0.30f),
                                    cursorColor = ink,
                                    focusedTextColor = ink,
                                    unfocusedTextColor = ink,
                                    focusedPlaceholderColor = ink.copy(alpha = 0.72f),
                                    unfocusedPlaceholderColor = ink.copy(alpha = 0.72f),
                                    focusedLeadingIconColor = ink,
                                    unfocusedLeadingIconColor = ink,
                                    focusedTrailingIconColor = ink.copy(alpha = 0.85f),
                                    unfocusedTrailingIconColor = ink.copy(alpha = 0.85f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (searchFocus != null) Modifier.focusRequester(searchFocus)
                                        else Modifier
                                    )
                            )
                        } else {
                            Column {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ink,
                                    maxLines = 1
                                )
                                // v27 — experimental paper-title underline (two
                                // short lines under the title text; OFF by default).
                                if (AppPreferences.paperHeaderCutsState) {
                                    PaperTitleLines(ink = ink)
                                }
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ink.copy(alpha = 0.82f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One ink-glass action pill on the hero — the banner's readable ink at a
 *  soft alpha (the Cabinet hero pill language), so hero action pills like
 *  sort / search read on the rose in every theme. [emphasized] deepens the
 *  fill for the active/primary state. Public so settings-family screens can
 *  pass their own pills into [SettingsHeroHeader]'s trailing slot. */
@Composable
fun SettingsHeroActionPill(
    onClick: () -> Unit,
    ink: Color,
    label: String? = null,
    glyph: String? = null,
    contentDescription: String? = null,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier,
    // v27n — the banner fill behind the pill (the opaque-fill conversion
    // needs it to resolve the same perceived tint); defaults to the shared
    // settings hero rose since every call site rides that banner.
    backdropOverride: Color? = null
) {
    // v27 — deepen the ink-glass: the old 18% fill vanished on the rose
    // banner (especially in light mode), so hero actions like search / sort
    // read as invisible. The glass stays frosted but clearly visible.
    // v27n — the pill fill is now OPAQUE (ink lerped into the hero banner
    // fill at the old glass alpha): a translucent fill let the elevation
    // shadow bleed through as a blurry broken background, and the opaque
    // lerp resolves to the exact same perceived tint on the banner.
    val backdrop = backdropOverride ?: settingsRoseAccent()
    val fill = if (emphasized) lerp(ink, backdrop, 0.45f) else lerp(ink, backdrop, 0.70f)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = fill,
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (glyph != null) {
                CurioIcon(
                    name = glyph,
                    contentDescription = contentDescription,
                    tint = ink,
                    size = 18.dp
                )
            }
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ink
                )
            }
        }
    }
}

/** One mirrored watermark glyph on the hero header — the banner's readable
 *  ink at a soft alpha (the Profile/Home collage construction). */
@Composable
private fun BoxScope.SettingsHeroSymbol(
    glyph: String,
    alignment: Alignment,
    size: Dp,
    rotation: Float,
    alpha: Float,
    tint: Color
) {
    CurioIcon(
        name = glyph,
        contentDescription = null,
        tint = tint.copy(alpha = alpha),
        size = size,
        modifier = Modifier
            .align(alignment)
            .padding(10.dp)
            .graphicsLayer { rotationZ = rotation }
    )
}

/** The settings hero's rose-wood fill — the SAME treatment as Home/Profile
 *  (the muted rose-wood base, its airy pastel twin in pastel mode) so
 *  Settings reads as part of the same torn-banner family. Shared (public)
 *  so the Cabinet's hero banner wears the identical rose. */
@Composable
fun settingsRoseAccent(): Color {
    // Material and AMOLED headers use the active scheme's semantic roles
    // instead of the legacy rose fill. This keeps hero headers coherent with
    // dynamic wallpaper colors and preserves a restrained dark AMOLED surface.
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_MATERIAL) {
        return MaterialTheme.colorScheme.primary
    }
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) {
        // Pure black — no grey/primary tint: on OLED the banner is a true
        // black plate and the rose accent comes through the watermark
        // collage + back pill instead of a tinted fill.
        return Color.Black
    }
    // v27l — optional sky-azure hero: when enabled, the shared hero wears
    // the airy pastel azure (Science/Sky twin) instead of the rose-wood.
    if (AppPreferences.heroBlueState) {
        return if (isCurioDarkTheme()) CurioColors.HomeAzureDark
        else CurioColors.HomeAzure
    }
    val base = toHsl(CurioColors.HomeRosewood)
    return if (isCurioDarkTheme()) {
        // Shared dark hero companion used by Settings, Cabinet, and Onboarding.
        CurioColors.HomeRosewoodDark
    } else if (AppPreferences.pastelColorsState) {
        val pinkHue = (base.h - 15f + 360f) % 360f
        // v26 — pastel headers get a touch more saturation (about +5%) so
        // the rose banners pop a little without leaving the airy family.
        fromHsl(pinkHue, ((base.s * 0.90f).coerceIn(0f, 0.80f) + 0.05f).coerceAtMost(0.85f), 0.82f)
    } else {
        fromHsl(base.h, (base.s * 0.80f).coerceAtMost(0.40f), (base.l * 1.06f).coerceAtMost(0.70f))
    }
}

/** Readable ink for content sitting on the settings rose banner (Home's
 *  helper, shared so the Cabinet hero uses the same ink). */
@Composable
fun settingsReadableInk(fill: Color): Color = when {
    AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_MATERIAL ->
        MaterialTheme.colorScheme.onPrimary
    AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED ->
        MaterialTheme.colorScheme.onSurface
    !AppPreferences.pastelColorsState && !isCurioDarkTheme() ->
        MaterialTheme.colorScheme.onSurface
    else -> pastelFillInk(fill)
}

/** Compact hub for the redesigned settings experience — the Profile-style
 *  hero header on a watermark backdrop, with clean settings cards and a
 *  search box that filters every section live (v7.100). */
@Composable
fun SettingsHubScreen(navController: NavController) {
    val context = LocalContext.current
    // Feed the quests system — opening Settings completes the journey quest.
    LaunchedEffect(Unit) { CurioQuests.onSettingsVisited(context) }
    var query by rememberSaveable { mutableStateOf("") }
    val needle = query.trim()
    val sections = remember(needle) { filterSettingsSections(SettingsSections, needle) }
    val searchResults = remember(needle) { collectSearchResults(SettingsSections, needle) }
    val searching = needle.isNotEmpty()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Watermark backdrop — muted category glyphs behind the content
        // (the Home/Profile language). Settings is category-neutral, so the
        // wildcard sparkle leads the collage.
        // v7.76 — the flat rows below the hero sit directly on this
        // backdrop, so the glyphs drop to a faint whisper and the text,
        // headers and chips always read first.
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }
        // The hero banner runs up BEHIND the status bar (the header applies
        // its own status-bar inset for the back pill) — the Profile/Home
        // construction, so Settings tears from the very top edge. The hero
        // is drawn LAST (on top of the scroll content): the rows scroll UP
        // and disappear behind the ragged tear instead of clipping at a
        // straight line.
        // Wide windows (tablets, landscape): the settings cards arrange in a
        // two-column grid so the hub reads at a glance; compact phones keep
        // the familiar single column. Search, section labels and the empty
        // state always span the full width.
        val wide = windowWidthSizeClass().isWide
        // Compact hero on tablets/landscape — 140dp instead of 180dp so
        // the torn banner doesn't dominate the short vertical space.
        val heroTotal = if (wide) 140.dp + SettingsHeroSheetExtent else SettingsHeroTotalHeight
        val gridState = rememberLazyGridState()
        ScreenEntrance {
            LazyVerticalGrid(
                state = gridState,
                columns = if (wide) GridCells.Adaptive(minSize = 300.dp) else GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = heroTotal + 10.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Search — filters every section below as you type ──
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CurioSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = "Search settings"
                    )
                }
                if (searching) {
                    if (searchResults.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { SettingsNoResults(needle) }
                    } else {
                        val grouped = searchResults.groupBy { it.sectionLabel }
                        grouped.forEach { (sectionLabel, results) ->
                            item(span = { GridItemSpan(maxLineSpan) }) { CurioSectionLabel(sectionLabel) }
                            item {
                                CurioSettingsCard(shadowElevation = 0.dp) {
                                    results.forEachIndexed { index, result ->
                                        if (index > 0) CurioSettingsDivider()
                                        CurioSettingsRow(result.row.icon, result.row.title, result.row.subtitle) {
                                            val deep = result.deep
                                            if (deep != null) {
                                                // Deep result → hand the exact row
                                                // key to the sub-section screen so
                                                // it scrolls to + pulses that row.
                                                SettingsHighlightTarget.page = deep.page
                                                SettingsHighlightTarget.rowKey = deep.rowKey
                                            }
                                            navController.navigate(result.row.route) { launchSingleTop = true }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                sections.forEach { section ->
                    item(span = { GridItemSpan(maxLineSpan) }) { CurioSectionLabel(section.label) }
                    section.cards.forEach { card ->
                        item {
                            CurioSettingsCard(shadowElevation = 0.dp) {
                                if (card.headerIcon != null && card.headerTitle != null && card.headerSubtitle != null) {
                                    CurioCardHeader(card.headerIcon, card.headerTitle, card.headerSubtitle)
                                }
                                card.rows.forEachIndexed { index, row ->
                                    if (index > 0) CurioSettingsDivider()
                                    if (row.route == CurioRoutes.SETTINGS_APPEARANCE) {
                                        // v8.xx — the Appearance row is a pet
                                        // landmark: the pet pokes it, and the
                                        // tour's Settings stop points at it.
                                        PetLandmark(
                                            id = "appearance",
                                            kind = PetLandmarks.Kind.FUN,
                                            screen = "settings"
                                        ) { lm ->
                                            Box(modifier = lm) {
                                                CurioSettingsRow(row.icon, row.title, row.subtitle) {
                                                    navController.navigate(row.route) { launchSingleTop = true }
                                                }
                                            }
                                        }
                                    } else {
                                        CurioSettingsRow(row.icon, row.title, row.subtitle) {
                                            navController.navigate(row.route) { launchSingleTop = true }
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
        // Side scroll indicator — thin overlay knob, grows on touch.
        CurioVerticalScrollIndicator(
            state = gridState.scrollIndicatorState,
            onScrollBy = { gridState.dispatchRawDelta(it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = heroTotal + 10.dp, bottom = 16.dp)
        )
        // Drawn on top of the scroll content — rows slide under the ragged
        // tear as they scroll up.
        SettingsHeroHeader(
            title = "Settings",
            subtitle = "Tune Curio your way",
            onBack = { navController.popBackStack() },
            compact = wide
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings search — the hub's rows are declared once as data (v7.100) so the
// search box can filter them live: sections keep any card that matches, cards
// keep any row that matches (a card whose header or section matches keeps ALL
// its rows), and the whole list collapses to a friendly empty state.
// ─────────────────────────────────────────────────────────────────────────────

/** One tappable settings row. */
private data class SettingsRowEntry(
    val icon: String,
    val title: String,
    val subtitle: String,
    val route: String
)

/** One grouped card of rows inside a settings section. */
private data class SettingsCardEntry(
    val headerIcon: String?,
    val headerTitle: String?,
    val headerSubtitle: String?,
    val rows: List<SettingsRowEntry>
)

/** One labelled settings section (Personalize / Explore / Safety & support). */
private data class SettingsSectionEntry(
    val label: String,
    val cards: List<SettingsCardEntry>
)

/** The full hub, declared once — the single source for both the rendered
 *  list and the search filter. */
private val SettingsSections = listOf(
    SettingsSectionEntry(
        label = "Personalize",
        cards = listOf(
            SettingsCardEntry(
                // v25 — the "How Curio feels / Appearance and color" card
                // header (icon + title + subtitle) was removed per request;
                // the rows below render directly under "Personalize". The
                // renderer skips the header when these are null.
                headerIcon = null,
                headerTitle = null,
                headerSubtitle = null,
                rows = listOf(
                    SettingsRowEntry(CurioIcons.DarkMode, "Appearance", "Theme, tint, and pastel color", CurioRoutes.SETTINGS_APPEARANCE),
                    // v26 — Preferences: search engine, explore behavior, and
                    // the pet's personality — "how Curio behaves" choices
                    // pulled out of Notifications and Appearance.
                    // v27 — Notifications is gone: every notification control
                    // (daily reminder, live notification, explore bubble)
                    // lives in Preferences now.
                    SettingsRowEntry(CurioIcons.Tune, "Preferences", "Search engine, explore, and pet behavior", CurioRoutes.SETTINGS_PREFERENCES),
                    SettingsRowEntry(CurioIcons.Mic, "Recording", "Voice-note quality and dictation", CurioRoutes.SETTINGS_RECORDING),
                    SettingsRowEntry(CurioIcons.Pets, "Pet designer", "Draw your own Curie", CurioRoutes.PET_DESIGNER),
                    // v26 — Experiments is hidden from Settings (it opens via
                    // the five-tap version trick in Support); these two moved
                    // in here from the old Explore section so they stay one
                    // tap away next to Appearance.
                    SettingsRowEntry(CurioIcons.DragHandle, "Manage categories", "Show, hide, or reorder lanes", CurioRoutes.MANAGE_CATEGORIES),
                    SettingsRowEntry(CurioIcons.History, "Topic history", "Revisit what you explored", CurioRoutes.TOPIC_HISTORY)
                )
            )
        )
    ),
    SettingsSectionEntry(
        label = "Safety & support",
        cards = listOf(
            SettingsCardEntry(
                // v25 — the "Your data" card header was removed per request;
                // the rows render directly under "Safety & support".
                headerIcon = null,
                headerTitle = null,
                headerSubtitle = null,
                rows = listOf(
                    SettingsRowEntry(CurioIcons.Backup, "Backup & restore", "Keep captures and settings safe", CurioRoutes.SETTINGS_DATA),
                    // v26 — recycle bin for soft-deleted captures.
                    SettingsRowEntry(CurioIcons.Delete, "Recycle bin", "Restore recently deleted captures", CurioRoutes.RECYCLE_BIN),
                    // v24 — merged into the shared Support & diagnostics page
                    // (same screen Profile's "Support & diagnostics" opens).
                    SettingsRowEntry(CurioIcons.Info, "Support & diagnostics", "Updates, reports, help & app details", CurioRoutes.SUPPORT)
                )
            )
        )
    )
)

/** Out-of-band handoff from the hub's search: when a result points INSIDE a
 *  settings sub-section (Appearance / Notifications / …), the section screen
 *  reads [page]/[rowKey] on entry and scrolls to + pulses that row. Cleared
 *  once consumed, mirroring [LightboxTarget]. */
object SettingsHighlightTarget {
    var page: SettingsPage? = null
    var rowKey: String? = null
}

/** One searchable row that lives INSIDE a settings sub-section screen. */
private data class SettingsDeepRow(
    val icon: String,
    val title: String,
    val subtitle: String,
    /** Route to open (the sub-section's own route). */
    val route: String,
    /** Sub-section for the highlight pulse; null when no highlight exists. */
    val page: SettingsPage? = null,
    /** Stable key identifying the exact row inside [page]. */
    val rowKey: String? = null
)

/**
 * Deep search index — every interactive row inside the sub-section screens
 * (the settings you reach by tapping Appearance / Notifications / Recording
 * / Backup & restore / About). The hub search matches these too, so typing
 * "reminder" finds the daily shuffle reminder, "voice" finds dictation, etc.
 */
private val SettingsDeepIndex: List<SettingsDeepRow> = listOf(
    // ── Appearance ───────────────────────────────────────────────────
    SettingsDeepRow(CurioIcons.AutoAwesome, "Theme style", "Curio, AMOLED, or Material", CurioRoutes.SETTINGS_APPEARANCE, SettingsPage.APPEARANCE, "appearance-style"),
    SettingsDeepRow(CurioIcons.DarkMode, "Theme", "Light, dark, or system", CurioRoutes.SETTINGS_APPEARANCE, SettingsPage.APPEARANCE, "appearance-theme"),
    SettingsDeepRow(CurioIcons.Palette, "Category tint", "Colorful page backgrounds", CurioRoutes.SETTINGS_APPEARANCE, SettingsPage.APPEARANCE, "appearance-tint"),
    SettingsDeepRow(CurioIcons.AutoAwesome, "Pastel colors", "Soft category accents and page tints", CurioRoutes.SETTINGS_APPEARANCE, SettingsPage.APPEARANCE, "appearance-pastel"),
    SettingsDeepRow(CurioIcons.Schedule, "Entry date & mood", "Date, mood, and attachments on saved entries", CurioRoutes.SETTINGS_APPEARANCE, SettingsPage.APPEARANCE, "appearance-entry"),
    // ── Preferences (v26) — search engine, explore behavior, pet personality ──
    // v19 — which search engine the "Explore in browser" button opens.
    SettingsDeepRow(CurioIcons.Search, "Search engine", "Which engine Explore opens in the browser", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-search-engine"),
    SettingsDeepRow(CurioIcons.Timer, "Explore sessions", "Timer, reminder, and done prompt", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-sessions"),
    SettingsDeepRow(CurioIcons.Notifications, "Live explore notification", "Ongoing timer with pause and stop", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-live"),
    SettingsDeepRow(CurioIcons.BubbleChart, "Floating explore bubble", "Timer bubble over other apps", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-bubble"),
    SettingsDeepRow(CurioIcons.Layers, "Display over other apps", "System permission for the floating bubble", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-overlay"),
    SettingsDeepRow(CurioIcons.Pets, "Pet chatter", "Quiet, cozy, or talkative pet dialogue", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-pet-chatter"),
    SettingsDeepRow(CurioIcons.Pets, "Pet games", "How often the pet starts games", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-pet-games"),
    SettingsDeepRow(CurioIcons.Notifications, "Daily shuffle reminder", "A daily nudge to spin the deck", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-reminder"),
    // v23 — re-shows the bubble opt-in row inside the Explore now dialog.
    SettingsDeepRow(CurioIcons.BubbleChart, "Explore bubble option in Explore dialog", "Show the bubble choice in the Explore now dialog", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-bubble-dialog"),
    // ── Recording ────────────────────────────────────────────────────
    SettingsDeepRow(CurioIcons.Mic, "Audio quality", "Voice-note recording quality", CurioRoutes.SETTINGS_RECORDING, SettingsPage.RECORDING, "recording-quality"),
    SettingsDeepRow(CurioIcons.Edit, "Voice-to-text", "Dictation buttons on voice-note fields", CurioRoutes.SETTINGS_RECORDING, SettingsPage.RECORDING, "recording-voice"),
    // ── Backup & restore (own screen — no row pulse) ─────────────────
    SettingsDeepRow(CurioIcons.Backup, "Open backup tools", "Export, restore, or import FieldMind data", CurioRoutes.SETTINGS_DATA),
    SettingsDeepRow(CurioIcons.History, "Backup workspace", "Full backup tools remain in the data workspace", CurioRoutes.SETTINGS_DATA),
    // ── About ────────────────────────────────────────────────────────
    // v24 — About content lives on the shared Support & diagnostics page.
    SettingsDeepRow(CurioIcons.Replay, "Replay intro", "See the welcome screens again", CurioRoutes.SUPPORT),
    SettingsDeepRow(CurioIcons.Info, "Version", "App version and build number", CurioRoutes.SUPPORT),
    SettingsDeepRow(CurioIcons.Download, "Check for updates", "See the latest release", CurioRoutes.SUPPORT)
)

/** One flat search result — the matching row plus its section context so
 *  the result list can show where each hit lives and navigate directly. */
private data class SettingsSearchResult(
    val sectionLabel: String,
    val row: SettingsRowEntry,
    /** Non-null when the result points inside a sub-section screen. */
    val deep: SettingsDeepRow? = null
)

/** Collects every row whose title or subtitle matches [needle] (case-
 *  insensitive, live-filtered): the hub's own navigation rows PLUS the deep
 *  index (rows inside the sub-section screens), so searching "reminder" or
 *  "voice" finds the actual setting, not just the section that holds it. */
private fun collectSearchResults(
    sections: List<SettingsSectionEntry>,
    needle: String
): List<SettingsSearchResult> {
    if (needle.isBlank()) return emptyList()
    val hub = sections.flatMap { section ->
        section.cards.flatMap { card ->
            card.rows.filter { row ->
                row.title.contains(needle, ignoreCase = true) ||
                    row.subtitle.contains(needle, ignoreCase = true)
            }.map { row -> SettingsSearchResult(section.label, row) }
        }
    }
    val deep = SettingsDeepIndex.filter { row ->
        row.title.contains(needle, ignoreCase = true) ||
            row.subtitle.contains(needle, ignoreCase = true)
    }.map { row ->
        SettingsSearchResult(
            sectionLabel = row.page?.title ?: "Backup & restore",
            row = SettingsRowEntry(row.icon, row.title, row.subtitle, row.route),
            deep = row
        )
    }
    return hub + deep
}

/** Keeps only sections/cards/rows matching [needle] (case-insensitive). A
 *  card whose header or section matches keeps ALL of its rows. */
private fun filterSettingsSections(
    sections: List<SettingsSectionEntry>,
    needle: String
): List<SettingsSectionEntry> {
    if (needle.isBlank()) return sections
    return sections.mapNotNull { section ->
        val sectionMatches = section.label.contains(needle, ignoreCase = true)
        val cards = section.cards.mapNotNull { card ->
            val headerMatches = card.headerTitle?.contains(needle, ignoreCase = true) == true
            val rows = card.rows.filter { row ->
                row.title.contains(needle, ignoreCase = true) ||
                    row.subtitle.contains(needle, ignoreCase = true)
            }
            if (!headerMatches && !sectionMatches && rows.isEmpty()) null
            else card.copy(rows = if (headerMatches || sectionMatches) card.rows else rows)
        }
        if (cards.isEmpty()) null else section.copy(cards = cards)
    }
}

/** Friendly empty state when the search matches nothing. */
@Composable
private fun SettingsNoResults(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CurioIcon(
            name = CurioIcons.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 30.dp
        )
        Text(
            text = "No settings found for \"$query\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Try a different word, like \"theme\", \"reminder\", or \"backup\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}
