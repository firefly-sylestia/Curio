package com.curio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import com.kyant.backdrop.backdrops.LayerBackdrop
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryFamily
import com.curio.app.data.CurioQuests
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioGlassToolbar
import com.curio.app.ui.components.CurioCardHeader
import com.curio.app.ui.components.CurioSearchField
import com.curio.app.ui.components.curioSearchFill
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioSectionLabel
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioSettingsRow
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.components.curioGlassGlow
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.components.PaperTitleLines
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.SoftTornSheetShape
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.PlayfairDisplayFontFamily
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.components.isInScreenGlassActive
import com.curio.app.ui.components.liquidGlassCapsule
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioPillTintLift
import com.curio.app.ui.theme.curioRoseInk
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.heroHeaderInk
import com.curio.app.ui.theme.headerAccent
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.readableLightInk
import com.curio.app.ui.theme.themedAccent
import com.curio.app.ui.theme.toHsl
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

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
/**
 * v3xx — the reserved scroll height under the settings hero. The torn
 * banner is a fixed 204dp; the GLASS toolbar style is content-height
 * (status bar + pills row + title/subtitle block ≈ 160dp idle — the
 * search field morphs in place of the title, so the footprint never
 * changes and the consumers' fixed reservation stays correct).
 */
val SettingsHeroTotalHeight: Dp
    get() = if (AppPreferences.headerStyleState == AppPreferences.HeaderStyle.GLASS) {
        160.dp
    } else {
        SettingsHeroBannerHeight + SettingsHeroSheetExtent
    }
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
    searchPlaceholder: String = "Search…",
    // v33 — pin the title block to the TOP of the banner instead of just
    // above the tear (screens whose action pills moved below the hero — the
    // Topic Database — read as a clean title header with controls beneath).
    titleAtTop: Boolean = false,
    // v42 — an optional control that rides INSIDE the banner beside the
    // title (directly under the trailing pills): the Topic Database's
    // Category pill. Screens that don't pass it render the plain title.
    titleTrailing: (@Composable (ink: Color) -> Unit)? = null,
    // v263 — RESTORED sticky-hero architecture: the scroll content records
    // into this LOCAL capture; the hero sits OUTSIDE it (drawn on top), so
    // its back pill can refract the rows scrolling behind it with REAL
    // liquid glass — no self-sample cycle. Null → classic opaque pill.
    glassBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null
) {
    // v3xx — GLASS TOOLBAR style: the app-wide "Glass toolbar header"
    // option swaps the torn paper banner for the content-height glass bar
    // (the old Cabinet v2 toolbar look, more blurry + its own tint). Every
    // param maps 1:1; screens that never pass search/trailing just render
    // the plain title bar.
    if (AppPreferences.headerStyleState == AppPreferences.HeaderStyle.GLASS) {
        CurioGlassToolbar(
            title = title,
            subtitle = subtitle,
            onBack = onBack,
            trailing = trailing,
            searchActive = searchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onCloseSearch = onCloseSearch,
            searchFocus = searchFocus,
            searchPlaceholder = searchPlaceholder,
            titleTrailing = titleTrailing,
            glassBackdrop = glassBackdrop
        )
        return
    }
    // v31 — the extraRow slot (the Topic Database's Category pill) is gone:
    // that pill now rides its own row BELOW the hero so the banner keeps
    // its original height and the header text never moves down.
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
    // v68 — theme-aware: the symbols ride the hero's READABLE ink (which
    // already resolves per-theme and per spin-lane) instead of forcing the
    // rose, so a lane-colored hero never wears mismatched rose icons.
    val symbolTint = ink
    Box(
        modifier = Modifier
            .fillMaxWidth()            .height(totalHeight)
        ) {
            // ── Under-sheet — the shared white paper layer, so the tear stays
            // bright beneath the rose hero in EVERY theme (light + dark). This
            // matches the app-wide hero pattern (Home uses the same warm
            // cream [0xFFFDFCF9]) — the Settings hero was the only one using
            // the theme surface, so in dark mode its tear read midnight-dark
            // while every other screen's tear stayed white paper. AMOLED:
            // the sheet turns a soft rose so the torn edge keeps reading
            // through the up-bites of the pure-black banner (black-on-black
            // would hide the seam), carrying the accent of the color.
            // v108 — OFF by default (Settings → Experiments → Paper &
            // headers); the toggle restores this extra paper layer.
            if (AppPreferences.heroTearSheetState) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .offset(y = bannerHeight - 18.dp)                    .clip(sheetShape)                    .background(
                    // v68 — the paper under the tear picks up a whisper of
                    // the hero's own color instead of a flat cream, so the
                    // lip always reads tinted with the banner. v81 — dark:
                    // a subtle lighter lip off the dark hero.
                    if (isCurioDarkTheme()) lerp(fill, Color.White, 0.10f)
                    else lerp(Color(0xFFFDFCF9), fill, 0.10f)
                )

        )
            }
        // ── Torn-edge shadow — hairline dark rim under the seam (same
        // black rim as Home's hero, in every theme — NOT the theme
        // onSurface, which resolves white-ish in dark mode).
        Box(
            modifier = Modifier
                .fillMaxWidth()                .height(bannerHeight)
                .offset(y = 1.dp)
                .clip(heroTornShape)
                .background(Color.Black.copy(alpha = 0.20f))
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
                // Mirrored watermark collage — screen-matched symbols pop
                // around the banner edges (settings is category-neutral).
                // v59.2 — the Settings hero scatters gear/slider/appearance
                // glyphs that match the screen instead of the generic
                // wildcard set.
                val symbols = CurioIcons.settingsHeroSymbols()
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
                        val backInteraction = remember { MutableInteractionSource() }
                        CurioBackButton(
                            onClick = onBack,
                            modifier = Modifier.then(
                                // v263 — GLASSY BACK BUTTON: when a local
                                // capture is provided and Liquid glass is
                                // active, the pill becomes refraction over
                                // the scrolling rows behind the tear.
                                // v292g — alwaysClear removed to match other
                                // UI liquid-glass pills (same frosted look).
                                if (glassBackdrop != null && isInScreenGlassActive())
                                    Modifier.liquidGlassCapsule(
                                        if (isCurioDarkTheme()) {
                                            lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Color.Black, 0.15f)
                                        } else {
                                            lerp(fill, curioPillTintLift(), 0.38f)
                                        },
                                        washAlpha = 0.45f,
                                        backdrop = glassBackdrop,
                                        interactionSource = backInteraction
                                    )
                                else Modifier
                            ),
                            // v76 — OPAQUE theme-aware pill, the same fill the
                            // hero action pills wear ([SettingsHeroActionPill]'s
                            // v27n opaque conversion): the old 18% ink glass
                            // read transparent on the banner; the opaque lerp
                            // of the banner fill toward the theme-aware lift
                            // resolves to the same perceived tint with a clean
                            // elevation shadow.
                            // v108 — dark: the same filter-chip glass as the
                            // action pills so the back pill matches its
                            // siblings on the dark banner.
                            containerColor = if (isCurioDarkTheme()) {
                                lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Color.Black, 0.15f)
                            } else {
                                lerp(fill, curioPillTintLift(), 0.38f)
                            },
                            contentColor = symbolTint,
                            shadowElevation = 3.dp,
                            disableRipple = true,
                            pillInteraction = backInteraction
                        )
                        if (searchActive) {
                            // v294 — Cancel pill removed; back button handles closing search.
                        } else if (trailing != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                trailing(ink)
                            }
                        }
                    }
                    // Flex spacer — pins the title/search block just above
                    // the tear (skipped with [titleAtTop], where the title
                    // sits at the top of the banner).
                    if (!titleAtTop) Spacer(Modifier.weight(1f))
                    // v294 — when titleAtTop, add breathing room between
                    // the top row (back + search/category pills) and the
                    // title row so the pills don't touch.
                    if (titleAtTop) Spacer(Modifier.height(8.dp))

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
                            // v90 — unified One UI search bar: banner ink +
                            // frosted glass through the shared component.
                            // v100 — search-text audit: THEME text color on
                            // the frosted glass (banner ink washed out).
                            CurioSearchField(
                                query = searchQuery,
                                onQueryChange = onSearchQueryChange,
                                placeholder = searchPlaceholder,
                                ink = MaterialTheme.colorScheme.onSurface,
                                // v108 — dark: the filter chips' near-black
                                // raised glass instead of the mid-tone lift.
                                fill = curioSearchFill(fill),
                                onCancel = onCloseSearch,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (searchFocus != null) Modifier.focusRequester(searchFocus)
                                        else Modifier
                                    )
                            )
                        } else {
                            // v42 — the optional control (Topic Database's
                            // Category pill) rides beside the title, directly
                            // under the trailing pills.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = ink,
                                        maxLines = 1
                                    )
                                    // v27 — experimental paper-title underline (two
                                    // short lines under the title text; OFF by default).
                                    if (AppPreferences.paperHeaderCutsState) {
                                        PaperTitleLines(
                                            ink = ink,
                                            title = title,
                                            fontSize = MaterialTheme.typography.headlineSmall.fontSize
                                        )
                                    }
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ink.copy(alpha = 0.82f),
                                        maxLines = 1
                                    )
                                }
                                if (titleTrailing != null) {
                                    titleTrailing(ink)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * v257 — full-bleed wrapper for the settings-family heroes that now live
 * INSIDE their scrolling lists: the list's edge padding would inset the
 * banner from both sides ("cut from the sides"). This measures the padded
 * width, offsets left by the edge padding and forces the viewport width —
 * the tear reaches both screen edges again (the Pet Designer v179 trick,
 * now shared).
 */
@Composable
internal fun FullBleedHeroItem(edgePad: Dp, hero: @Composable () -> Unit) {
    // v261 — MEASURED full bleed: instead of guessing the inset arithmetic
    // (offset -edgePad + requiredWidth(maxWidth + 2·edgePad)), read the
    // slot's REAL distance from the window's left edge and the window's
    // real width, then shift/resize by exactly those values. Pixel-perfect
    // under any nesting (list content padding, wide-window centering,
    // future outer paddings) — fixes the tear sitting left-shifted with a
    // gap on the right.
    var shiftLeftPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var viewportWidthDp by remember { androidx.compose.runtime.mutableStateOf(Dp.Unspecified) }
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val root = coords.findRootCoordinates()
                val leftInset = coords.positionInRoot().x - root.positionInRoot().x
                shiftLeftPx = leftInset
                viewportWidthDp = with(density) { root.size.width.toFloat().toDp() }
            }
    ) {
        if (viewportWidthDp != Dp.Unspecified) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(-shiftLeftPx.roundToInt(), 0) }
                    .requiredWidth(viewportWidthDp)
            ) { hero() }
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
    // v30 — optional trailing glyph (the Category pill's up/down chevron).
    trailingGlyph: String? = null,
    trailingContentDescription: String? = null,
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
    // v29 — the fills are now a LIGHT frosted glass (the banner lifted
    // toward white): the v27r ink-lean fills (lerp toward the ink at
    // 0.35/0.55) read too dark in light + pastel themes. Lifting toward
    // white keeps the same visible-pill look with full-ink glyphs that pop
    // in every mode — creamy in light/pastel, brighter glass on the deep
    // dark banner. The glyph stays 20dp.
    val backdrop = backdropOverride ?: settingsRoseAccent()
    // v42 — the glass lifts toward the COLOR-TINTED page background
    // ([curioPillTintLift] — a whisper of the brand rose instead of plain
    // cream) so settings/profile buttons stop reading as flat cream blocks;
    // AMOLED gets a soft grey glass instead of pitch black. Dark keeps the
    // white lift so the pill stays a brighter glass on the deep banner.
    // v108 — dark mode swaps to the FILTER CHIPS' dark raised glass
    // (near-black tinted surface) so the hero pills read as part of the
    // same chip family at night instead of bright glass on the dark banner.
    val fill = if (isCurioDarkTheme()) {
        lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Color.Black, 0.15f)
    } else {
        lerp(backdrop, curioPillTintLift(), if (emphasized) 0.24f else 0.38f)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = fill,
        shadowElevation = 3.dp,
        modifier = modifier
            // v28 — dark mode elevation visibility (glow).
            // v85 — same One UI glass glow as the sort dropdown, so the
            // search / select / cancel pills render as its identical sibling
            // in dark (before, the sort pill glowed and these stayed flat).
            .curioDarkGlow(3.dp, RoundedCornerShape(50))
            .curioGlassGlow(RoundedCornerShape(50), ink)
    ) {
        Row(
            // v29 — bigger hit areas (was 11/8dp + 20dp glyph) so the hero
            // controls read as substantial buttons, not tiny chips.
            // v30 — uniform 42dp height so label-only pills match glyph
            // pills and the sort dropdown (which reads the same 42dp).
            // v79 — middle-size unification with the sort pill: height
            // 42 → 46dp and glyph 22 → 20dp so the icon-only Search pill
            // reads the same size as the sort dropdown beside it.
            modifier = Modifier
                .heightIn(min = 46.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (glyph != null) {
                CurioIcon(
                    name = glyph,
                    contentDescription = contentDescription,
                    tint = ink,
                    size = 20.dp
                )
            }
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ink
                )
            }
            if (trailingGlyph != null) {
                CurioIcon(
                    name = trailingGlyph,
                    contentDescription = trailingContentDescription,
                    tint = ink.copy(alpha = 0.85f),
                    size = 18.dp
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

/**
 * The category driving the shared hero + page background when the
 * "Hero follows Spin lane" Appearance option is on — the single lane last
 * picked on Spin, or null (mix / no lane / toggle off) to keep the
 * default rose/azure. The same resolver feeds every shared rose-wood hero
 * (Home / Profile / Settings / Cabinet / Quests / the drawer) and the
 * [heroPageBackground] wash, mirroring the Cabinet's active-filter hero.
 */
@Composable
fun heroLaneCategory(): CurioCategory? {
    if (!AppPreferences.heroFollowLaneState) return null
    // Hoist LocalContext.current out of the runCatching lambdas — its
    // @Composable accessor can't be invoked inside a non-composable
    // lambda (rule: non-composable callbacks are NOT @Composable scopes).
    val context = LocalContext.current
    val lane = runCatching { AppPreferences.getLastSpinCategories(context).singleOrNull() }
        .getOrNull() ?: return null
    return runCatching { CurioCategories.byId(lane) }.getOrNull()
}

/**
 * The shared-hero family's page background: the Spin lane's category wash
 * when "Hero follows Spin lane" is on (the Cabinet's page language), else
 * [default] — so screens that never wore a tint keep their exact current
 * look until the toggle is flipped.
 */
@Composable
fun heroPageBackground(default: Color = MaterialTheme.colorScheme.background): Color =
    heroLaneCategory()?.categoryBackgroundWash() ?: default

/**
 * v223 — whether the torn shared heroes wear the Material theme's
 * primaryContainer ("Material hero tears" Appearance option). Needs the
 * Material theme itself on — the Appearance row greys out otherwise.
 */
fun materialHeroTearsOn(): Boolean =
    AppPreferences.materialThemeState && AppPreferences.materialHeroTearsState

/** The settings hero's rose-wood fill — the SAME treatment as Home/Profile
 *  (the muted rose-wood base, its airy pastel twin in pastel mode) so
 *  Settings reads as part of the same torn-banner family. Shared (public)
 *  so the Cabinet's hero banner wears the identical rose. */
@Composable
fun settingsRoseAccent(): Color {
    // v223 — "Material hero tears": when the Material theme AND this
    // option are both on, the torn hero wears the scheme's
    // primaryContainer instead of the app-default rose/azure (or a lane).
    if (materialHeroTearsOn()) return MaterialTheme.colorScheme.primaryContainer
    // v30 — "Hero follows Spin lane": the shared hero wears the Spin
    // lane's category accent (the Cabinet's filtered-hero language) instead
    // of the rose/azure.
    heroLaneCategory()?.let { cat -> return cat.headerAccent() }
    // v81 — dark mode: the torn hero wears a NEW SHADE of the same spectrum
    // — the deep rose/azure twins (never the light shade).
    if (isCurioDarkTheme()) {
        if (AppPreferences.heroBlueState) return CurioColors.HomeAzureDark
        val base = toHsl(CurioColors.HomeRosewood)
        if (AppPreferences.pastelColorsState) {
            val pinkHue = (base.h - 15f + 360f) % 360f
            return fromHsl(pinkHue, ((base.s * 0.90f).coerceIn(0f, 0.80f) + 0.05f).coerceAtMost(0.85f), 0.40f)
        }
        return CurioColors.HomeRosewoodDark
    }
    // v27l — optional sky-azure hero: when enabled, the shared hero wears
    // the airy pastel azure (Science/Sky twin) instead of the rose-wood.
    if (AppPreferences.heroBlueState) {
        return CurioColors.HomeAzure
    }
    val base = toHsl(CurioColors.HomeRosewood)
    return if (AppPreferences.pastelColorsState) {
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
fun settingsReadableInk(fill: Color): Color {
    // v223 — Material hero tears: readable ink on primaryContainer.
    if (materialHeroTearsOn()) return MaterialTheme.colorScheme.onPrimaryContainer
    // v32 — when the shared hero wears the SPIN LANE's accent (Adaptive
    // Hero), the text must be accent-aware: white/cream on the deep accent
    // (never the fixed dark onSurface, which was invisible on a vivid
    // lane banner in non-pastel). The lane branch resolves like every
    // category hero ([heroHeaderInk]); the plain rose keeps the old ink.
    heroLaneCategory()?.let { return it.heroHeaderInk() }
    // v81 — dark mode: crisp light ink on the dark rose banner (the scheme's
    // soft cream-white — never pure white, per the dark-mode research).
    if (isCurioDarkTheme()) return MaterialTheme.colorScheme.onBackground
    return if (!AppPreferences.pastelColorsState) MaterialTheme.colorScheme.onSurface
    else pastelFillInk(fill)
}

/**
 * v72 — the option-card ACCENT INK for Profile/Settings icon glyphs,
 * matched to the hero the page wears: when the shared hero follows a Spin
 * lane (Adaptive Hero) this resolves the lane's readable category ink;
 * when the sky-azure hero is on, an azure twin; otherwise the brand rose —
 * so the option cards' icons always match the banner above them instead of
 * staying fixed coral. Material/AMOLED keep the rose (their banners wear
 * scheme roles, and the option cards' coral identity stays as today).
 */
@Composable
fun settingsCardAccentInk(): Color {
    // v78 — light Curio only (the Material/AMOLED rose fallback is gone
    // with those styles).
    heroLaneCategory()?.let { return it.categoryInk() }
    if (AppPreferences.heroBlueState) {
        if (isCurioDarkTheme()) return CurioColors.HomeAzure
        return readableLightInk(CurioColors.HomeAzure)
    }
    return curioRoseInk()
}

/** Jump to a settings rail destination, collapsing the stack above the hub:
 *  switching sections REPLACES the current page, so the hub stays one
 *  back-press away (never a growing stack of visited sections). "all" (the
 *  null-route entry) targets the hub itself. */
internal fun navigateToSettingsSection(navController: NavController, entry: SettingsNavEntry) {
    val route = entry.route ?: CurioRoutes.SETTINGS
    navController.navigate(route) {
        popUpTo(CurioRoutes.SETTINGS) { inclusive = false }
        launchSingleTop = true
    }
}

/**
 * v72 — the option-card CHIP hue (the icon-chip fill + card-tint family),
 * matched to the hero the page wears (lane accent / sky-azure / brand
 * coral). Dark mode resolves the lane's light twin so chips stay visible
 * pale glass on midnight, mirroring how [CurioColors.CoralBlush] is used in
 * dark today. The fill twin of [settingsCardAccentInk].
 */
@Composable
fun settingsCardChipTint(): Color {
    // v78 — light Curio only (the Material/AMOLED coral fallback is gone
    // with those styles).
    heroLaneCategory()?.let { return it.themedAccent() }
    if (AppPreferences.heroBlueState) return CurioColors.HomeAzure
    return CurioColors.CoralBlush
}

/**
 * v72 — hero-aware twin of [curioPillTintLift] for the option cards: same
 * construction and strength (AMOLED grey glass, dark white lift, light a
 * whisper of the page background) but the tint resolves the HERO's accent
 * ink instead of the fixed rose, so the card fill follows the banner hue.
 */
@Composable
fun settingsCardTintLift(): Color {
    // v81 — dark mode: a whisper of the hero's LIGHT ink pulled into the
    // near-black card, so the option cards read as dark hue-tinted glass on
    // the pitch-black page (never bright glass).
    if (isCurioDarkTheme()) {
        return lerp(Color.Black, settingsCardAccentInk(), 0.20f)
    }
    // Light: a whisper of the page background in the hero hue.
    return lerp(
        MaterialTheme.colorScheme.background,
        settingsCardAccentInk(),
        0.08f
    )
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
    // v3xx — the JSX nav rail's active chip ("all" = the hub itself).
    var activeNav by rememberSaveable { mutableStateOf("all") }
    val needle = query.trim()
    val sections = remember(needle) { filterSettingsSections(SettingsSections, needle) }
    val searchResults = remember(needle) { collectSearchResults(SettingsSections, needle) }
    val searching = needle.isNotEmpty()
    Box(
        modifier = Modifier
            .fillMaxSize()
            // v30 — "Hero follows Spin lane": the page wears the Spin lane's
            // wash (the Cabinet language); otherwise the soft rose tint.
            .background(heroPageBackground(androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)))
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
        val gridState = rememberLazyGridState()
        // RESTORED (user request) — STICKY HERO: the grid records into a
        // local capture; the hero is pinned ON TOP of it after the grid,
        // so rows scroll UP BEHIND the ragged tear and the hero's back
        // pill refracts them with REAL liquid glass.
        val glassBackdrop = rememberLayerBackdrop()
        // v27t — wide windows (tablet / landscape) render the two-pane
        // master-detail layout ([SettingsTwoPaneHub]): the full settings nav
        // list stays on the left while the selected page's options show on
        // the right. Compact phones keep the familiar single-column grid.
        if (!wide) {
        ScreenEntrance {
            LazyVerticalGrid(
                state = gridState,
                // v3xx — the JSX redesign: cards sit 2-up like the design
                // (search, nav rail, headings and the footer span full width).
                columns = GridCells.Fixed(2),
                modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
                contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = SettingsHeroTotalHeight, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── JSX nav rail — All Settings / Appearance / … ──
                item(key = "nav", span = { GridItemSpan(maxLineSpan) }) {
                    SettingsNavRail(active = activeNav, onSelect = { entry ->
                        activeNav = entry.id
                        navigateToSettingsSection(navController, entry)
                    })
                }
                // ── Search — filters every section below as you type ──
                item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                    SettingsJsxSearchField(query = query, onQueryChange = { query = it })
                }
                if (searching) {
                    if (searchResults.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { SettingsNoResults(needle) }
                    } else {
                        val grouped = searchResults.groupBy { it.sectionLabel }
                        grouped.forEach { (sectionLabel, results) ->
                            item(span = { GridItemSpan(maxLineSpan) }) { CurioSectionLabel(sectionLabel) }
                            item(span = { GridItemSpan(maxLineSpan) }) {
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
                    // v3xx — the JSX groups + cards (the flat rows are gone
                    // from the hub; the search index + two-pane still use
                    // them underneath).
                    settingsDesignGroups.forEach { group ->
                        item(key = "g|${group.label}", span = { GridItemSpan(maxLineSpan) }) {
                            SettingsGroupHeading(group)
                        }
                        group.cards.forEach { card ->
                            item(key = "card|${card.id}") {
                                if (card.id == "appearance") {
                                    // v8.xx — the Appearance card stays a pet
                                    // landmark: the pet pokes it, and the
                                    // tour's Settings stop points at it.
                                    PetLandmark(
                                        id = "appearance",
                                        kind = PetLandmarks.Kind.FUN,
                                        screen = "settings"
                                    ) { lm ->
                                        SettingsDesignCardView(
                                            card = card,
                                            onClick = { navController.navigate(card.route) { launchSingleTop = true } },
                                            modifier = lm
                                        )
                                    }
                                } else {
                                    SettingsDesignCardView(
                                        card = card,
                                        onClick = { navController.navigate(card.route) { launchSingleTop = true } }
                                    )
                                }
                            }
                        }
                    }
                    settingsSecondaryCards.forEach { card ->
                        item(key = "sec|${card.id}") {
                            SettingsSecondaryCardView(
                                card = card,
                                onClick = { navController.navigate(card.route) { launchSingleTop = true } }
                            )
                        }
                    }
                    item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                        SettingsFooterNote()
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
                .padding(top = 10.dp, bottom = 16.dp)
        )
        // Pinned hero, drawn on TOP of the grid — cards slide under the
        // ragged tear as they scroll up.
        SettingsHeroHeader(
            title = "Settings",
            subtitle = "Tune Curio your way",
            onBack = { navController.popBackStack() },
            compact = wide,
            glassBackdrop = glassBackdrop
        )
        } else {
            SettingsTwoPaneHub(
                query = query,
                onQueryChange = { query = it },
                navController = navController,
                sections = sections,
                searching = searching,
                searchResults = searchResults,
                needle = needle
            )
        }
    }
}

/**
 * v27t — the tablet/landscape two-pane Settings: a fixed-width nav list on
 * the left (every settings entry, search-filtered) with the selected page's
 * options on the right — no more pushing a full-screen section over the hub
 * on big windows.
 */
@Composable
private fun SettingsTwoPaneHub(
    query: String,
    onQueryChange: (String) -> Unit,
    navController: NavController,
    sections: List<SettingsSectionEntry>,
    searching: Boolean,
    searchResults: List<SettingsSearchResult>,
    needle: String
) {
    var selectedPageName by rememberSaveable { mutableStateOf(SettingsPage.APPEARANCE.name) }
    val selectedPage =
        runCatching { SettingsPage.valueOf(selectedPageName) }.getOrDefault(SettingsPage.APPEARANCE)
    // Deep-search highlight: when a search result points inside a section,
    // the target is set before the page switches, so the right pane pulses
    // the exact row (mirrors [SettingsSectionScreen]'s handoff).
    val paneHighlight = remember(selectedPage) {
        SettingsHighlightTarget.takeIf { it.page == selectedPage }?.rowKey
    }
    LaunchedEffect(selectedPage) {
        SettingsHighlightTarget.page = null
        SettingsHighlightTarget.rowKey = null
    }

    fun handleRow(row: SettingsRowEntry, deep: SettingsDeepRow? = null) {
        val page = sectionPageFor(row.route)
        if (page != null) {
            // A section row (or a deep row pointing into one) selects the
            // page in the right pane instead of navigating.
            if (deep?.page != null && deep.rowKey != null) {
                SettingsHighlightTarget.page = deep.page
                SettingsHighlightTarget.rowKey = deep.rowKey
            }
            selectedPageName = page.name
        } else {
            navController.navigate(row.route) { launchSingleTop = true }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Compact hero at the top — the panes sit below it, so the menu
        // never scrolls under the tear in the two-pane layout.
        SettingsHeroHeader(
            title = "Settings",
            subtitle = "Tune Curio your way",
            onBack = { navController.popBackStack() },
            compact = true
        )
        Row(modifier = Modifier.fillMaxSize()) {
            // ── Left pane — the full settings nav list (search-filtered) ──
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            ) {
                CurioSearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    placeholder = "Search settings",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (searching) {
                        if (searchResults.isEmpty()) {
                            item { SettingsNoResults(needle) }
                        } else {
                            val grouped = searchResults.groupBy { it.sectionLabel }
                            grouped.forEach { (sectionLabel, results) ->
                                item { CurioSectionLabel(sectionLabel) }
                                results.forEach { result ->
                                    item {
                                        SettingsNavRow(
                                            icon = result.row.icon,
                                            title = result.row.title,
                                            subtitle = result.row.subtitle,
                                            selected = sectionPageFor(result.row.route)?.name == selectedPageName
                                        ) { handleRow(result.row, result.deep) }
                                    }
                                }
                            }
                        }
                    } else {
                        sections.forEach { section ->
                            item { CurioSectionLabel(section.label) }
                            section.cards.forEach { card ->
                                card.rows.forEach { row ->
                                    item {
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
                                                    SettingsNavRow(
                                                        icon = row.icon,
                                                        title = row.title,
                                                        subtitle = row.subtitle,
                                                        selected = sectionPageFor(row.route)?.name == selectedPageName
                                                    ) { handleRow(row) }
                                                }
                                            }
                                        } else {
                                            SettingsNavRow(
                                                icon = row.icon,
                                                title = row.title,
                                                subtitle = row.subtitle,
                                                selected = sectionPageFor(row.route)?.name == selectedPageName
                                            ) { handleRow(row) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Hairline between the panes.
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            )
            // ── Right pane — the selected page's options ────────────────
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { CurioSectionLabel(selectedPage.title) }
                item {
                    SettingsPageContent(selectedPage, navController, paneHighlight)
                }
            }
        }
    }
}

/** The settings page a hub row opens — the four in-app sections, or null
 *  when the row navigates to its own screen (Pet designer, History, …). */
private fun sectionPageFor(route: String): SettingsPage? = when (route) {
    CurioRoutes.SETTINGS_APPEARANCE -> SettingsPage.APPEARANCE
    CurioRoutes.SETTINGS_PREFERENCES -> SettingsPage.PREFERENCES
    CurioRoutes.SETTINGS_RECORDING -> SettingsPage.RECORDING
    CurioRoutes.SETTINGS_DATA -> SettingsPage.DATA
    CurioRoutes.EXPERIMENTS -> null // standalone screen, not a section page
    CurioRoutes.USER_EXPERIMENTS -> null // standalone screen
    else -> null
}

/** A nav-list row for the two-pane hub: icon + label, with the selected
 *  page's row wearing a soft action tint so the active section reads at a
 *  glance. */
@Composable
private fun SettingsNavRow(
    icon: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) curioDialogActionColor().copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CurioIcon(
                icon,
                null,
                tint = if (selected) curioDialogActionColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 21.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(curioDialogActionColor())
                )
            }
        }
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
                    SettingsRowEntry(CurioIcons.Mic, "Recording", "Voice-note quality, dictation and offline transcription", CurioRoutes.SETTINGS_RECORDING),
                    SettingsRowEntry(CurioIcons.Pets, "Pet designer", "Draw your own Curie", CurioRoutes.PET_DESIGNER),
                    // v26 — Experiments is hidden from Settings (it opens via
                    // the five-tap version trick in Support); these two moved
                    // in here from the old Explore section so they stay one
                    // tap away next to Appearance.
                    SettingsRowEntry(CurioIcons.DragHandle, "Manage categories", "Show, hide, or reorder lanes", CurioRoutes.MANAGE_CATEGORIES),
                    SettingsRowEntry(CurioIcons.History, "Topic history", "Revisit what you explored", CurioRoutes.TOPIC_HISTORY),
                    SettingsRowEntry(CurioIcons.Share, "Share hub", "Browse every design, pick a topic, share a card", CurioRoutes.SHARE_HUB),
                    SettingsRowEntry(CurioIcons.AutoAwesome, "Experiments", "Try features before they ship", CurioRoutes.USER_EXPERIMENTS)
                    // Dev page hidden — accessible via 5-tap version number in Support
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
                    // v112 — the dedicated Updates sub-page (its own UI,
                    // replaces the old update card inside Support).
                    SettingsRowEntry(CurioIcons.Download, "Updates", "Your build, release notes & update checker", CurioRoutes.UPDATES),
                    // v24 — merged into the shared Support & diagnostics page
                    // (same screen Profile's "Support & diagnostics" opens).
                    SettingsRowEntry(CurioIcons.Info, "Support & diagnostics", "Reports, help & app details", CurioRoutes.SUPPORT)
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
    SettingsDeepRow(CurioIcons.AutoAwesome, "Adaptive Hero", "Shared hero + page take the category you last picked on Spin", CurioRoutes.SETTINGS_APPEARANCE, SettingsPage.APPEARANCE, "appearance-hero-lane"),
    // ── Preferences (v26) — search engine, explore behavior, pet personality ──
    // v19 — which search engine the "Explore in browser" button opens.
    SettingsDeepRow(CurioIcons.Search, "Search engine", "Which engine Explore opens in the browser", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-search-engine"),
    SettingsDeepRow(CurioIcons.Timer, "Explore sessions", "Timer, reminder, and done prompt", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-sessions"),
    SettingsDeepRow(CurioIcons.Notifications, "Live explore notification", "Ongoing timer with pause and stop", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-live"),
    SettingsDeepRow(CurioIcons.BubbleChart, "Floating explore bubble", "Timer bubble over other apps (asks for the overlay permission)", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-bubble"),
    SettingsDeepRow(CurioIcons.Pets, "Pet chatter", "Quiet, cozy, or talkative pet dialogue", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-pet-chatter"),
    SettingsDeepRow(CurioIcons.Pets, "Pet games", "How often the pet starts games", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-pet-games"),
    SettingsDeepRow(CurioIcons.Notifications, "Daily shuffle reminder", "A daily nudge to spin the deck", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-reminder"),
    // v23 — re-shows the bubble opt-in row inside the Explore now dialog.
    SettingsDeepRow(CurioIcons.BubbleChart, "Explore bubble option in Explore dialog", "Show the bubble choice in the Explore now dialog", CurioRoutes.SETTINGS_PREFERENCES, SettingsPage.PREFERENCES, "pref-bubble-dialog"),
    // ── Recording ────────────────────────────────────────────────────
    SettingsDeepRow(CurioIcons.Mic, "Audio quality", "Voice-note recording quality", CurioRoutes.SETTINGS_RECORDING, SettingsPage.RECORDING, "recording-quality"),
    SettingsDeepRow(CurioIcons.Edit, "Voice-to-text", "Live dictation while typing, and transcription of recordings", CurioRoutes.SETTINGS_RECORDING, SettingsPage.RECORDING, "recording-voice"),
    SettingsDeepRow(CurioIcons.Download, "Offline model", "Offline model for pre-recorded voice-to-text", CurioRoutes.SETTINGS_RECORDING, SettingsPage.RECORDING, "recording-offline-model"),
    // ── Backup & restore (own screen — no row pulse) ─────────────────
    SettingsDeepRow(CurioIcons.Backup, "Open backup tools", "Export, restore, or import FieldMind data", CurioRoutes.SETTINGS_DATA),
    SettingsDeepRow(CurioIcons.History, "Backup workspace", "Full backup tools remain in the data workspace", CurioRoutes.SETTINGS_DATA),
    // ── Updates (v112 — dedicated sub-page) ─────────────────────────
    SettingsDeepRow(CurioIcons.Info, "Version", "App version and build number", CurioRoutes.UPDATES),
    SettingsDeepRow(CurioIcons.Download, "Check for updates", "See the latest release", CurioRoutes.UPDATES),
    SettingsDeepRow(CurioIcons.Notifications, "Update checker", "Opt-in background update checks", CurioRoutes.UPDATES),
    // ── About ────────────────────────────────────────────────────────
    // v24 — About content lives on the shared Support & diagnostics page.
    SettingsDeepRow(CurioIcons.Replay, "Replay intro", "See the welcome screens again", CurioRoutes.SUPPORT)
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

// ─────────────────────────────────────────────────────────────────────────────
// v3xx — the JSX "Settings redesign" hub: grouped tone CARDS with decorative
// visuals + a nav rail + search + a footer note — exactly the
// CurioSettings_Redesign-3.jsx look (the app's own header stays untouched).
// The existing rows/search/deep-index stay; these cards are the new face of
// the hub (search still falls back to the row results).
// ─────────────────────────────────────────────────────────────────────────────

/** The JSX card tones (light pastel gradient + deep dark twin). */
private enum class SettingsDesignTone {
    CORAL, SAGE, BLUE, LAVENDER, YELLOW, MINT, PINK, VIOLET, SLATE, STEEL
}

/** The JSX decorative foot visuals each card wears. */
private enum class SettingsDesignVisual {
    SWATCHES, PET, COMPASS, WAVE, CARDS, PHOTOS, SHARE, FLASK, CLOUD, IMAGE
}

/** One big JSX-style setting card. */
private data class SettingsDesignCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val tone: SettingsDesignTone,
    val visual: SettingsDesignVisual,
    val route: String
)

/** One labelled group of cards (JSX `group`). */
private data class SettingsDesignGroup(
    val label: String,
    val glyph: String,
    val cards: List<SettingsDesignCard>
)

/** One nav-rail entry (JSX `sideNav` → horizontal chip rail on phones). */
internal data class SettingsNavEntry(
    val id: String,
    val label: String,
    val icon: String,
    val route: String? = null
)

/** The JSX side-nav, in order. "all" is the hub itself. */
private val settingsNavRail = listOf(
    SettingsNavEntry("all", "All Settings", CurioIcons.Home, null),
    SettingsNavEntry("appearance", "Appearance", CurioIcons.DarkMode, CurioRoutes.SETTINGS_APPEARANCE),
    SettingsNavEntry("pet", "Pet designer", CurioIcons.Pets, CurioRoutes.PET_DESIGNER),
    SettingsNavEntry("preferences", "Preferences", CurioIcons.Tune, CurioRoutes.SETTINGS_PREFERENCES),
    SettingsNavEntry("recording", "Recording", CurioIcons.Mic, CurioRoutes.SETTINGS_RECORDING),
    SettingsNavEntry("categories", "Categories", CurioIcons.DragHandle, CurioRoutes.MANAGE_CATEGORIES),
    SettingsNavEntry("history", "Topic history", CurioIcons.History, CurioRoutes.TOPIC_HISTORY),
    SettingsNavEntry("share", "Share hub", CurioIcons.Share, CurioRoutes.SHARE_HUB),
    SettingsNavEntry("experiments", "Experiments", CurioIcons.AutoAwesome, CurioRoutes.USER_EXPERIMENTS),
    SettingsNavEntry("backup", "Backup", CurioIcons.Backup, CurioRoutes.SETTINGS_DATA),
    SettingsNavEntry("support", "Support", CurioIcons.SupportAgent, CurioRoutes.SUPPORT)
)

/** The four JSX groups (plus the data & privacy group the user asked to
 *  slot the book-fetching etc. into) — every card maps to a real screen. */
private val settingsDesignGroups = listOf(
    SettingsDesignGroup("Personalize", "\u2726", listOf(
        SettingsDesignCard("appearance", "Appearance", "Theme, tint, and pastel color", CurioIcons.DarkMode, SettingsDesignTone.CORAL, SettingsDesignVisual.SWATCHES, CurioRoutes.SETTINGS_APPEARANCE),
        SettingsDesignCard("pet", "Pet designer", "Draw your own Curie", CurioIcons.Pets, SettingsDesignTone.SAGE, SettingsDesignVisual.PET, CurioRoutes.PET_DESIGNER)
    )),
    SettingsDesignGroup("How it works", "\u2727", listOf(
        SettingsDesignCard("preferences", "Preferences", "Search engine, explore, and pet behavior", CurioIcons.Tune, SettingsDesignTone.BLUE, SettingsDesignVisual.COMPASS, CurioRoutes.SETTINGS_PREFERENCES),
        SettingsDesignCard("recording", "Recording", "Voice-note quality, dictation and offline transcripts", CurioIcons.Mic, SettingsDesignTone.LAVENDER, SettingsDesignVisual.WAVE, CurioRoutes.SETTINGS_RECORDING)
    )),
    SettingsDesignGroup("Organize your world", "\u2261", listOf(
        SettingsDesignCard("categories", "Manage categories", "Show, hide, or reorder lanes", CurioIcons.DragHandle, SettingsDesignTone.YELLOW, SettingsDesignVisual.CARDS, CurioRoutes.MANAGE_CATEGORIES),
        SettingsDesignCard("history", "Topic history", "Revisit what you explored", CurioIcons.History, SettingsDesignTone.MINT, SettingsDesignVisual.PHOTOS, CurioRoutes.TOPIC_HISTORY)
    )),
    SettingsDesignGroup("Share & explore", "\u25C7", listOf(
        SettingsDesignCard("share", "Share hub", "Browse every design, pick a topic, share a card", CurioIcons.Share, SettingsDesignTone.PINK, SettingsDesignVisual.SHARE, CurioRoutes.SHARE_HUB),
        SettingsDesignCard("experiments", "Experiments", "Try features before they ship", CurioIcons.AutoAwesome, SettingsDesignTone.VIOLET, SettingsDesignVisual.FLASK, CurioRoutes.USER_EXPERIMENTS)
    )),
    SettingsDesignGroup("Your data & privacy", "\u25C8", listOf(
        SettingsDesignCard("backup", "Backup & restore", "Keep captures and settings safe", CurioIcons.Backup, SettingsDesignTone.SLATE, SettingsDesignVisual.CLOUD, CurioRoutes.SETTINGS_DATA),
        SettingsDesignCard("bookcovers", "Book covers", "Cover-art fetching and providers", CurioIcons.Image, SettingsDesignTone.STEEL, SettingsDesignVisual.IMAGE, CurioRoutes.SETTINGS_BOOK_COVER)
    ))
)

/** The JSX secondary cards — smaller horizontal rows under the groups. */
private data class SettingsSecondaryCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val route: String
)

private val settingsSecondaryCards = listOf(
    SettingsSecondaryCard("recycle", "Recycle bin", "Restore recently deleted captures", CurioIcons.Delete, CurioRoutes.RECYCLE_BIN),
    SettingsSecondaryCard("updates", "Updates", "Your build, release notes & update checker", CurioIcons.Download, CurioRoutes.UPDATES),
    SettingsSecondaryCard("support", "Help & feedback", "Get support or suggest a feature", CurioIcons.SupportAgent, CurioRoutes.SUPPORT)
)

/** Light + dark gradient pair for a tone (JSX `.coral` … `.violet` + the
 *  two cool data tones). */
private fun settingsToneGradient(tone: SettingsDesignTone, dark: Boolean): Pair<Color, Color> = when (tone) {
    SettingsDesignTone.CORAL -> if (dark) Color(0xFF743F42) to Color(0xFF693A42) else Color(0xFFF4B6A8) to Color(0xFFE7A08F)
    SettingsDesignTone.SAGE -> if (dark) Color(0xFF3C5140) to Color(0xFF34483A) else Color(0xFFD0E1C9) to Color(0xFFB7D0B4)
    SettingsDesignTone.BLUE -> if (dark) Color(0xFF345363) to Color(0xFF314B59) else Color(0xFFC0DEEB) to Color(0xFFA5CADE)
    SettingsDesignTone.LAVENDER -> if (dark) Color(0xFF4A4164) to Color(0xFF40385B) else Color(0xFFD6CFEB) to Color(0xFFBCAED9)
    SettingsDesignTone.YELLOW -> if (dark) Color(0xFF62502F) to Color(0xFF57452A) else Color(0xFFF9DFA6) to Color(0xFFF1C875)
    SettingsDesignTone.MINT -> if (dark) Color(0xFF385345) to Color(0xFF324A3E) else Color(0xFFCFE4D5) to Color(0xFFB5D3C0)
    SettingsDesignTone.PINK -> if (dark) Color(0xFF693F4D) to Color(0xFF603946) else Color(0xFFF2C2C8) to Color(0xFFE5A6B1)
    SettingsDesignTone.VIOLET -> if (dark) Color(0xFF50416B) to Color(0xFF45385E) else Color(0xFFD3C4E7) to Color(0xFFB9A5D5)
    SettingsDesignTone.SLATE -> if (dark) Color(0xFF3A424C) to Color(0xFF333A44) else Color(0xFFCCD6DF) to Color(0xFFB8C6D1)
    SettingsDesignTone.STEEL -> if (dark) Color(0xFF2F4A57) to Color(0xFF2B4350) else Color(0xFFC3D8E4) to Color(0xFFAECBDA)
}

/** The warm readable ink on a tone card (JSX `--ink` + the muted twin). */
private fun settingsCardInk(dark: Boolean) =
    if (dark) Color(0xFFF3EAE2) else Color(0xFF52383C)

/** The JSX decorative foot visual — drawn minimally in Compose. Every art
 *  stays inside the 92×62 visual box (nothing is cut by the card's rounded
 *  corner), keeps the bottom-right corner clear, and prefers a bundled icon
 *  glyph over extra drawing where one exists. */
@Composable
private fun SettingsCardVisual(visual: SettingsDesignVisual, modifier: Modifier = Modifier) {
    when (visual) {
        SettingsDesignVisual.SWATCHES -> Box(modifier) {
            // Four paint swatches, fanned — fully inside the box.
            val colors = listOf(0xFFE8B0A0, 0xFFB8C9B0, 0xFFB7A9CF, 0xFFD6B1C1)
            val offsets = listOf(androidx.compose.ui.unit.IntOffset(2, 12), androidx.compose.ui.unit.IntOffset(28, 2), androidx.compose.ui.unit.IntOffset(44, 6), androidx.compose.ui.unit.IntOffset(24, 22))
            val rots = listOf(-11f, 2f, 12f, -2f)
            colors.forEachIndexed { i, c ->
                Box(
                    modifier = Modifier
                        .offset { offsets[i] }
                        .rotate(rots[i])
                        .size(width = 28.dp, height = 38.dp)
                        .border(2.5.dp, Color(0xFFFFF9F3).copy(alpha = 0.85f), RoundedCornerShape(5.dp))
                        .background(Color(c))
                )
            }
        }
        SettingsDesignVisual.PET -> Box(modifier) {
            // Minimal pet — ears, head, eyes + nose, body; nothing cut.
            Box(
                modifier = Modifier.offset(x = 32.dp, y = 6.dp).rotate(20f).size(width = 11.dp, height = 13.dp)
                    .background(Color(0xFFEEE5D8), RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 6.dp))
            )
            Box(
                modifier = Modifier.offset(x = 54.dp, y = 6.dp).rotate(-20f).size(width = 11.dp, height = 13.dp)
                    .background(Color(0xFFEEE5D8), RoundedCornerShape(topStart = 8.dp, topEnd = 4.dp, bottomStart = 6.dp, bottomEnd = 4.dp))
            )
            Box(
                modifier = Modifier.offset(x = 28.dp, y = 20.dp).size(width = 36.dp, height = 24.dp)
                    .background(Color(0xFFF4EEE3), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
            )
            Box(
                modifier = Modifier.offset(x = 38.dp, y = 28.dp).size(width = 3.dp, height = 3.dp).background(Color(0xFF4B3A35), CircleShape)
            )
            Box(
                modifier = Modifier.offset(x = 54.dp, y = 28.dp).size(width = 3.dp, height = 3.dp).background(Color(0xFF4B3A35), CircleShape)
            )
            Box(
                modifier = Modifier.offset(x = 45.dp, y = 32.dp).size(width = 2.5.dp, height = 2.5.dp).background(Color(0xFFA36F65), CircleShape)
            )
            Box(
                modifier = Modifier.offset(x = 14.dp, y = 40.dp).size(width = 58.dp, height = 18.dp)
                    .background(Color(0xFF9C735F).copy(alpha = 0.85f), RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomStart = 5.dp, bottomEnd = 5.dp))
            )
        }
        SettingsDesignVisual.COMPASS -> Box(modifier) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                // Two soft mountains, bottom-left.
                drawPath(Path().apply { moveTo(w * 0.00f, h); lineTo(w * 0.30f, h * 0.52f); lineTo(w * 0.58f, h); close() }, color = Color(0xFF46656C).copy(alpha = 0.55f))
                drawPath(Path().apply { moveTo(w * 0.28f, h); lineTo(w * 0.56f, h * 0.26f); lineTo(w * 0.78f, h); close() }, color = Color(0xFF495E74).copy(alpha = 0.40f))
                // Compass ring over them, clear of the corner.
                val cx = w * 0.60f; val cy = h * 0.40f; val r = w * 0.18f
                drawCircle(Color.White.copy(alpha = 0.45f), radius = r, center = androidx.compose.ui.geometry.Offset(cx, cy))
                // Needle — solid north, pale south, pivot dot.
                drawPath(Path().apply { moveTo(cx, cy - r * 0.78f); lineTo(cx - r * 0.34f, cy + r * 0.16f); lineTo(cx + r * 0.34f, cy + r * 0.16f); close() }, color = Color(0xFF687A91).copy(alpha = 0.85f))
                drawPath(Path().apply { moveTo(cx, cy + r * 0.78f); lineTo(cx - r * 0.34f, cy + r * 0.16f); lineTo(cx + r * 0.34f, cy + r * 0.16f); close() }, color = Color(0xFF687A91).copy(alpha = 0.35f))
                drawCircle(Color(0xFF687A91), radius = 1.6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
            }
        }
        SettingsDesignVisual.WAVE -> Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.5.dp),
            modifier = modifier
        ) {
            listOf(10, 18, 30, 22, 38, 16, 28, 12, 32, 18).forEach { h ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(h.dp)
                        .background(Color(0xFF6E628E).copy(alpha = 0.7f), RoundedCornerShape(50))
                )
            }
        }
        SettingsDesignVisual.CARDS -> Box(modifier) {
            // Fanned card stack, drawn with proper shapes — fully inside.
            val colors = listOf(0xFF8DA993, 0xFFD2A87B, 0xFF9EB8C0, 0xFFF4EADC)
            val offsets = listOf(androidx.compose.ui.unit.IntOffset(2, 32), androidx.compose.ui.unit.IntOffset(20, 26), androidx.compose.ui.unit.IntOffset(32, 20), androidx.compose.ui.unit.IntOffset(48, 12))
            val rots = listOf(12f, 4f, -4f, -12f)
            colors.forEachIndexed { i, c ->
                Box(
                    modifier = Modifier
                        .offset { offsets[i] }
                        .rotate(rots[i])
                        .size(width = 40.dp, height = 24.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                        .background(Color(c))
                )
            }
        }
        SettingsDesignVisual.PHOTOS -> Box(modifier) {
            // Three polaroid snaps, fanned — nothing cut.
            val colors = listOf(0xFF7E9C86, 0xFFC79F86, 0xFF9BB5A1)
            val offsets = listOf(androidx.compose.ui.unit.IntOffset(2, 28), androidx.compose.ui.unit.IntOffset(26, 18), androidx.compose.ui.unit.IntOffset(50, 8))
            val rots = listOf(10f, -4f, -14f)
            colors.forEachIndexed { i, c ->
                Box(
                    modifier = Modifier
                        .offset { offsets[i] }
                        .rotate(rots[i])
                        .size(width = 34.dp, height = 26.dp)
                        .border(2.5.dp, Color(0xFFF8F0E8), RoundedCornerShape(4.dp))
                        .background(Color(c))
                )
            }
        }
        SettingsDesignVisual.SHARE -> Box(modifier) {
            // The mini share card only — the floating share bubble is gone.
            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = 8.dp)
                    .rotate(-6f)
                    .size(width = 68.dp, height = 46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF6E9D5))
                    .padding(6.dp)
            ) {
                Text(
                    text = "CURIO",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp),
                    color = Color(0xFF6B4F45)
                )
                Text(
                    text = "stay curious",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF6B4F45)
                )
            }
        }
        SettingsDesignVisual.FLASK -> Box(modifier) {
            // The science flask glyph — identifying and simple.
            CurioIcon(
                name = CurioIcons.Science,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                size = 44.dp,
                modifier = Modifier.offset(x = 20.dp, y = 8.dp)
            )
        }
        SettingsDesignVisual.CLOUD -> Box(modifier) {
            // The backup cloud glyph — identifying and simple.
            CurioIcon(
                name = CurioIcons.Backup,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                size = 44.dp,
                modifier = Modifier.offset(x = 22.dp, y = 8.dp)
            )
        }
        SettingsDesignVisual.IMAGE -> Box(modifier) {
            // The image glyph — identifying and simple.
            CurioIcon(
                name = CurioIcons.Image,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                size = 44.dp,
                modifier = Modifier.offset(x = 22.dp, y = 8.dp)
            )
        }
    }
}

/** The JSX setting card — tone gradient, blob shapes, texture dots, frosted
 *  icon tile, round arrow, title/subtitle and the decorative visual. */
@Composable
private fun SettingsDesignCardView(
    card: SettingsDesignCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isCurioDarkTheme()
    val (start, end) = settingsToneGradient(card.tone, dark)
    val ink = settingsCardInk(dark)
    val muted = ink.copy(alpha = if (dark) 0.74f else 0.72f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(204.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(start, end)))
            .clickable(onClick = onClick)
    ) {
        // ── Blobs + texture (the JSX ::before/::after + cardTexture) ──
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawCircle(Color.White.copy(alpha = 0.20f), radius = w * 0.62f, center = androidx.compose.ui.geometry.Offset(w * 1.08f, h * 0.02f))
            drawCircle(Color.White.copy(alpha = 0.13f), radius = w * 0.55f, center = androidx.compose.ui.geometry.Offset(-w * 0.12f, h * 1.18f))
            // Bubble dots (JSX .cardTexture) — outlined bubbles + a light
            // speckle; the diagonal sheen line is gone.
            drawCircle(Color.White.copy(alpha = 0.16f), radius = w * 0.16f, center = androidx.compose.ui.geometry.Offset(w * 0.94f, h * 0.16f), style = Stroke(width = 1.4.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.13f), radius = w * 0.07f, center = androidx.compose.ui.geometry.Offset(w * 0.34f, h * 0.88f), style = Stroke(width = 1.2.dp.toPx()))
            listOf(
                androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.80f),
                androidx.compose.ui.geometry.Offset(w * 0.20f, h * 0.88f),
                androidx.compose.ui.geometry.Offset(w * 0.46f, h * 0.78f),
                androidx.compose.ui.geometry.Offset(w * 0.60f, h * 0.86f),
                androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.70f),
                androidx.compose.ui.geometry.Offset(w * 0.84f, h * 0.78f)
            ).forEach { c ->
                drawCircle(Color.White.copy(alpha = 0.30f), radius = 1.5.dp.toPx(), center = c)
            }
        }
        // ── Decorative visual, bottom-right — drawn FIRST so it sits BEHIND
        //    the title/subtitle (the art never covers the text). ──
        SettingsCardVisual(
            visual = card.visual,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 7.dp)
                .size(width = 92.dp, height = 62.dp)
                .alpha(0.92f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Frosted icon tile.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.33f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(name = card.icon, contentDescription = null, tint = ink, size = 21.dp)
                }
                Spacer(Modifier.weight(1f))
                // Round arrow.
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF63423A).copy(alpha = if (dark) 0.42f else 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(name = CurioIcons.ChevronRight, contentDescription = null, tint = Color(0xFFFFF9F1), size = 17.dp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.2).sp),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.76f)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = card.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = muted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.80f)
            )
        }
    }
}

/** The JSX secondary card — a horizontal icon + text + arrow row. */
@Composable
private fun SettingsSecondaryCardView(
    card: SettingsSecondaryCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isCurioDarkTheme()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
                else Color.White.copy(alpha = 0.68f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (dark) Color.White.copy(alpha = 0.09f) else Color(0xFFF2E8DC)),
            contentAlignment = Alignment.Center
        ) {
            CurioIcon(
                name = card.icon,
                contentDescription = null,
                tint = if (dark) Color(0xFFD7B8A9) else Color(0xFF755647),
                size = 20.dp
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = card.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CurioIcon(
            name = CurioIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            size = 18.dp
        )
    }
}

/** The JSX group heading — Playfair-ish serif label + thin rule + glyph. */
@Composable
private fun SettingsGroupHeading(group: SettingsDesignGroup) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 10.dp, start = 2.dp, end = 2.dp)
    ) {
        Text(
            text = group.glyph,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = group.label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(11.dp))
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
        )
    }
}

/**
 * The JSX nav rail — horizontal chips on phones (the desktop sidebar's
 * mobile twin). "All Settings" returns to the hub itself.
 *
 * [active] is the currently open rail page: it renders highlighted in the
 * SECOND slot (right after "All Settings") so where you are sits next to
 * the way back — every other section follows in its fixed order. Pass
 * null on settings-family screens that aren't a rail destination (drill-in
 * tool pages): nothing is highlighted and the order stays fixed. Shared by
 * the hub AND every settings sub-page so the top bar is consistent and
 * switching sections is one tap away.
 */
@Composable
internal fun SettingsNavRail(
    active: String?,
    onSelect: (SettingsNavEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isCurioDarkTheme()
    // Rotate the active page into slot 2 (right after "All Settings"); the
    // rest keeps the fixed rail order. No active page (or "all" — the hub)
    // → the plain order.
    val entries = remember(active) {
        val first = settingsNavRail.firstOrNull()
        val activeEntry = active?.let { id -> settingsNavRail.firstOrNull { it.id == id } }
        if (first == null || activeEntry == null || activeEntry.id == first.id) settingsNavRail
        else listOf(first, activeEntry) + settingsNavRail.filter { it.id != first.id && it.id != activeEntry.id }
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(entries, key = { it.id }) { entry ->
            val selected = active != null && active == entry.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .width(82.dp)
                    .heightIn(min = 60.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        when {
                            selected -> Color(0xFF815947)
                            dark -> Color.White.copy(alpha = 0.07f)
                            else -> Color.White.copy(alpha = 0.62f)
                        }
                    )
                    .clickable { onSelect(entry) }
                    .padding(horizontal = 6.dp, vertical = 9.dp)
            ) {
                CurioIcon(
                    name = entry.icon,
                    contentDescription = null,
                    tint = if (selected) Color(0xFFFFF9F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 19.dp
                )
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp
                    ),
                    color = if (selected) Color(0xFFFFF9F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** The JSX search — rounded white box with the magnifier (search still
 *  falls back to the deep row index below). */
@Composable
private fun SettingsJsxSearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val dark = isCurioDarkTheme()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(
                if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                else Color.White.copy(alpha = 0.70f)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                RoundedCornerShape(19.dp)
            )
            .padding(horizontal = 15.dp)
    ) {
        CurioIcon(
            name = CurioIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 20.dp
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f)
        ) { inner ->
            Box {
                if (query.isEmpty()) {
                    Text(
                        text = "Search settings",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = CurioIcons.Close,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 15.dp
                )
            }
        }
    }
}

/** The JSX footer note — a soft panel under everything. */
@Composable
private fun SettingsFooterNote() {
    val dark = isCurioDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                else Color(0xFFE9DFD4)
            )
            .padding(vertical = 26.dp, horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "\u2726  \u2727  \u2726",
                style = MaterialTheme.typography.bodyLarge,
                color = if (dark) Color(0xFFB48F83) else Color(0xFF8B695C),
                letterSpacing = 8.sp
            )
            Text(
                text = "Same curiosity, new horizons.",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Curio is a little better when it feels like yours.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
