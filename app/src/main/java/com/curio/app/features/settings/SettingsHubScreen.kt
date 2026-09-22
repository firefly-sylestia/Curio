package com.curio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.curio.app.ui.adaptive.LocalRevealSharedScope
import com.curio.app.ui.adaptive.LocalRevealVisibilityScope
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioGlassToolbar
import com.curio.app.ui.components.CurioSearchField
import com.curio.app.ui.components.curioSearchFill
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.glyphWatermarkDepthScale
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
import com.curio.app.ui.theme.activeNamedTheme
import com.curio.app.ui.theme.curioCardShadow
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
import androidx.compose.ui.platform.LocalConfiguration
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
/** The breathing room between the title block and an optional banner FOOTER
 *  (the Social wall's door row). Counted into the footer's height budget. */
private val SettingsHeroFooterGap = 12.dp
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
    get() = settingsHeroTotalHeight()

/**
 * The hero's footprint for a screen whose banner carries a FOOTER — the
 * reserved scroll height a caller must use when it passes [footerHeight] to
 * [SettingsHeroHeader], so the screen's content starts below the extended
 * banner instead of under it. Defaults to the plain header, i.e. exactly
 * [SettingsHeroTotalHeight].
 */
fun settingsHeroTotalHeight(footerHeight: Dp = 0.dp): Dp =
    if (AppPreferences.headerStyleState == AppPreferences.HeaderStyle.GLASS) {
        // v3xx22 — 176dp: the bar's real footprint (status bar + pills row
        // + title block ≈ 172dp on a modern phone). The old 160dp left the
        // settings nav rail peeking from under the header (user fix).
        176.dp + footerHeight
    } else {
        SettingsHeroBannerHeight + SettingsHeroSheetExtent + footerHeight
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
    // v3xx — NULLABLE: a tab root has nothing to go back to (the Community
    // wall's opt-in bottom-nav entry), so it omits the back pill entirely
    // instead of showing a dead one. Every other caller still passes a
    // lambda, so the pill renders exactly as before.
    onBack: (() -> Unit)? = null,
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
    // An optional control that rides BEFORE the title column (a chat peer's
    // avatar + name — messenger headers lead with the person, not with a
    // title that would read first). Rendered left of the title text.
    titleLeading: (@Composable (ink: Color) -> Unit)? = null,
    // v386 — an optional FOOTER that rides INSIDE the banner: content that
    // belongs to the header itself (the Social wall's Chats / Friends / You
    // doors) and must be there the moment the hero paints instead of popping
    // in below once the page's data lands. [footerHeight] is that row's own
    // height plus [SettingsHeroFooterGap] — it EXTENDS the banner (and the
    // caller's reservation, see [settingsHeroTotalHeight]) so the title block
    // above it never moves.
    footer: (@Composable (ink: Color) -> Unit)? = null,
    footerHeight: Dp = 0.dp,
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
        // v386 — the glass style is content-height, so a footer simply stacks
        // UNDER the bar (inside one Column, since callers place this header as
        // an overlay: two siblings would paint on top of each other).
        Column(modifier = Modifier.fillMaxWidth()) {
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
                titleLeading = titleLeading,
                glassBackdrop = glassBackdrop
            )
            if (footer != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = SettingsHeroFooterGap)
                ) {
                    footer(MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        return
    }
    // v31 — the extraRow slot (the Topic Database's Category pill) is gone:
    // that pill now rides its own row BELOW the hero so the banner keeps
    // its original height and the header text never moves down.
    val bannerHeight = (if (compact) 140.dp else SettingsHeroBannerHeight) + footerHeight
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
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 10.dp,
                            // v386 — a footer sits on the paper just above the
                            // torn edge, so it needs a little more clearance
                            // than the title's descenders did.
                            bottom = if (footer != null) 22.dp else 16.dp
                        )
                ) {
                    // ── Top row — back pill + optional hero action pills ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val backInteraction = remember { MutableInteractionSource() }
                        if (onBack != null) {
                        CurioBackButton(
                            onClick = onBack,
                            // It is handed its own glass just below (v263).
                            ambientGlass = false,
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
                        } else {
                            // No back pill (a tab root): keep any trailing
                            // pills pinned to the trailing edge instead of
                            // letting SpaceBetween slide them to the start.
                            Spacer(Modifier.weight(1f))
                        }
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
                                if (titleLeading != null) {
                                    titleLeading(ink)
                                }
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
                    if (footer != null) {
                        Spacer(Modifier.height(SettingsHeroFooterGap))
                        footer(ink)
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
/**
 * v386 — THE hero-pill fill: the single recipe behind the back pill, every
 * hero action pill, and any other furniture riding a torn banner (the Social
 * wall's Chats / Friends / You doors). OPAQUE on purpose — a translucent fill
 * lets the elevation shadow bleed through as a blurry dark smudge (v27n), and
 * the lerp resolves to the same perceived tint the old ink glass had. Light
 * and pastel lift the banner toward the brand rose; dark wears the filter
 * chips' raised near-black glass instead of bright glass on a deep banner.
 */
@Composable
fun settingsHeroPillFill(
    backdrop: Color = settingsRoseAccent(),
    emphasized: Boolean = false
): Color = if (isCurioDarkTheme()) {
    lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Color.Black, 0.15f)
} else {
    lerp(backdrop, curioPillTintLift(), if (emphasized) 0.24f else 0.38f)
}

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
    // (see [settingsHeroPillFill] — one recipe, shared with the hero's own
    // furniture so nothing riding the banner can drift off the pill look).
    // ([curioPillTintLift] — a whisper of the brand rose instead of plain
    // cream) so settings/profile buttons stop reading as flat cream blocks;
    // AMOLED gets a soft grey glass instead of pitch black. Dark keeps the
    // white lift so the pill stays a brighter glass on the deep banner.
    // v108 — dark mode swaps to the FILTER CHIPS' dark raised glass
    // (near-black tinted surface) so the hero pills read as part of the
    // same chip family at night instead of bright glass on the dark banner.
    val fill = settingsHeroPillFill(backdrop, emphasized)
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
        // v407 — the hero's mirrored glyph collage tones down with the page
        // backdrop and the mood board (Appearance → "Glyph backdrop").
        tint = tint.copy(alpha = alpha * glyphWatermarkDepthScale()),
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
 * v414 — the lane the CATEGORY TINT switch tints Home/Profile pages with: the
 * single lane last picked on Spin, independent of the "Adaptive Hero" theme
 * (which owns the HERO fill, not the page tint). Returns null when the tint
 * switch is off, so an OFF switch keeps Home/Profile on their rose/cream page.
 *
 * Before v414 the Category tint row only drove the category-washed screens
 * (Spin, Reveal, Cabinet); on Home it looked dead because Home's page falls
 * back to the rose lerp unless the Adaptive Hero theme is on — this is the
 * gate that makes the switch work there (member: "the category tint option
 * isnt working, for home etc").
 */
@Composable
fun categoryTintLane(): CurioCategory? {
    if (!AppPreferences.tintWashEffective()) return null
    val context = LocalContext.current
    val lane = runCatching { AppPreferences.getLastSpinCategories(context).singleOrNull() }
        .getOrNull() ?: return null
    return runCatching { CurioCategories.byId(lane) }.getOrNull()
}

/**
 * The shared-hero family's page background: the Spin lane's category wash
 * when "Hero follows Spin lane" is on (the Cabinet's page language), else the
 * CATEGORY TINT lane's wash when that switch is on, else [default] — so
 * screens that never wore a tint keep their exact current look until a toggle
 * is flipped.
 */
@Composable
fun heroPageBackground(default: Color = MaterialTheme.colorScheme.background): Color {
    heroLaneCategory()?.let { return it.categoryBackgroundWash() }
    categoryTintLane()?.let { return it.categoryBackgroundWash() }
    return default
}

/**
 * v223/v3xx51 — whether the torn shared heroes wear the Material theme's
 * primaryContainer. The "Material hero tears" Appearance option was removed
 * per user request: the Material container hero is now PART of the Material
 * theme (on whenever the theme is on), so this simply reads that theme.
 * The dormant `materialHeroTearsState` pref API stays for compatibility.
 */
fun materialHeroTearsOn(): Boolean = AppPreferences.materialThemeState

/** The settings hero's rose-wood fill — the SAME treatment as Home/Profile
 *  (the muted rose-wood base, its airy pastel twin in pastel mode) so
 *  Settings reads as part of the same torn-banner family. Shared (public)
 *  so the Cabinet's hero banner wears the identical rose. */
@Composable
fun settingsRoseAccent(): Color {
    // v420 — a named theme IS the hero: its own primary fill answers first,
    // before the lane, the azure and the rose-wood.
    activeNamedTheme()?.let { return MaterialTheme.colorScheme.primary }
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
            // v421 — MUTED. The dark banner was carrying the light hero's own
            // vibrancy at a night lightness, and on a black page that read as a
            // neon rose (member: "in dark mode the curio rose is too vibrant in
            // dark mode maybe mute it"). The hue and the depth stay; the hold
            // drops from ~0.59 to ~0.37, which is a rose in the dark rather
            // than a rose under a spotlight.
            return fromHsl(
                pinkHue,
                (base.s * 0.62f).coerceIn(0f, 0.52f),
                0.40f
            )
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

/**
 * THE ACCENT'S INK — the SAME hue as [settingsRoseAccent], at a shade that
 * can actually be READ.
 *
 * [settingsRoseAccent] is a HERO FILL: airy on purpose (the pale rose-wood,
 * the pastel twin, a lane's banner tone), which is exactly what a torn banner
 * wants and exactly wrong for a line of text or a 16dp glyph on a light page
 * — the pale fill as ink is the "washed out / cheap" look. So the accent has
 * three shades, and a surface picks by ROLE:
 *
 *  · [settingsRoseAccent] — FILLS: banners, buttons, pills, rails, borders.
 *  · [settingsAccentInk] — TEXT and GLYPHS: a deep shade of the same hue on a
 *    light page, a lifted one on a dark page (where "darker" would vanish).
 *  · `accent.copy(alpha = 0.12f)` — HIGHLIGHTS and tracks, never a foreground.
 *
 * Nothing here is a second palette: it is the same hue with the lightness the
 * role needs, derived from whatever accent the member's hero is wearing
 * (rose, azure, Material's container, or a Spin lane).
 */
@Composable
fun settingsAccentInk(): Color {
    if (materialHeroTearsOn()) return MaterialTheme.colorScheme.onPrimaryContainer
    val fill = settingsRoseAccent()
    val base = toHsl(fill)
    return if (isCurioDarkTheme()) {
        // A deep fill on a dark page: the ink is the SAME hue, lifted — a
        // darker shade here would simply disappear into the surface.
        fromHsl(
            base.h,
            (base.s * 0.92f).coerceAtMost(0.70f),
            (base.l + 0.30f).coerceAtMost(0.80f)
        )
    } else {
        // A light fill on a light page: the ink is the same hue, DROPPED to a
        // deep, saturated shade — this is the darker tone, never a paler one.
        fromHsl(
            base.h,
            (base.s * 1.10f).coerceAtMost(0.62f),
            (base.l * 0.48f).coerceAtMost(0.36f)
        )
    }
}

/** Readable ink for content sitting on the settings rose banner (Home's
 *  helper, shared so the Cabinet hero uses the same ink). */
@Composable
fun settingsReadableInk(fill: Color): Color {
    // v420 — the ink on a named theme's hero is its own onPrimary pair.
    activeNamedTheme()?.let { return MaterialTheme.colorScheme.onPrimary }
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
    // v420 — a named theme's option cards wear ITS accent ink, never the
    // app's coral identity.
    activeNamedTheme()?.let { return it.accentFor(isCurioDarkTheme()) }
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
 *  back-press away (never a growing stack of visited sections). A
 *  null-route entry targets the hub itself. */
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
    // v420 — a named theme's own hero colour paints the icon chips.
    activeNamedTheme()?.let { return MaterialTheme.colorScheme.primary }
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
    // v408 — LIGHT: WHITE, breathing the hero accent. It used to be the
    // PAGE BACKGROUND itself, and [CurioSettingsCard] then lerps its fill
    // 30% of the way toward this — so a card resolved to a paler cream laid
    // on a cream page, i.e. the very "the cards blend too much" the member
    // reported on Profile, quests and Settings. Basing the lift on white
    // keeps the card's hue tie to the page while letting the card separate
    // from it by LIGHTNESS (see the theme's "card ladder").
    return lerp(Color.White, settingsCardAccentInk(), 0.08f)
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
    // v408 — THE RAIL IS NOT ON THE HUB ANY MORE (member request: "hide the
    // rail in settings the top rail, when im in all settings only show when
    // inside some settings, also remove the all settings option from the
    // rail"). The rail belongs to the SECTION pages, where it switches
    // between them; the hub itself is the plain list (see below), so there
    // is no "active chip" state to keep here and no "All Settings" chip in
    // the rail to point back at.
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
                // v408 — ONE column: the hub is a list now, and every item
                // in it spans the full width (the rows inside a section card
                // share one card, so a 2-up grid would only break the list
                // into disconnected plates).
                columns = GridCells.Fixed(1),
                modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
                contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = SettingsHeroTotalHeight, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            item(span = { GridItemSpan(maxLineSpan) }) { SettingsSectionHeading(sectionLabel) }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SettingsOptionCard {
                                    results.forEachIndexed { index, result ->
                                        if (index > 0) SettingsOptionDivider()
                                        SettingsOptionRow(result.row.icon, result.row.title, result.row.subtitle) {
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
                    // ── v408 — THE PLAIN LIST ────────────────────────────────
                    //
                    // The hub is a SETTINGS PAGE, so it now reads like one:
                    // a heading per section, then a white card holding that
                    // section's rows (icon, title, subtitle, chevron) — the
                    // same row language the section pages themselves use.
                    //
                    // The designed 2-up cards (tone gradients, decorative
                    // doodles, oversized "big title" tiles) are GONE, on
                    // request: "in appearance while keeping the rail system
                    // instead of the big cards with design, add a simpler
                    // list based simpler all settings look the main settings
                    // page but a simpler and list view". The rail survives
                    // where it belongs — inside the section pages.
                    //
                    // The four "front door" entries (Online mode, Recycle
                    // bin, Updates, Support & diagnostics) ride as PLAIN
                    // rows: no icon tile, and their copy is free to wrap to
                    // three lines instead of being cut off mid-sentence the
                    // way the small designed cards cut it.
                    SettingsSections.forEach { section ->
                        item(key = "sec|${section.label}", span = { GridItemSpan(maxLineSpan) }) {
                            SettingsSectionHeading(section.label)
                        }
                        item(key = "rows|${section.label}", span = { GridItemSpan(maxLineSpan) }) {
                            SettingsOptionCard {
                                val rows = section.cards.flatMap { it.rows }
                                rows.forEachIndexed { index, row ->
                                    // The divider insets to whatever the row
                                    // ABOVE it starts its own text at: past
                                    // the icon tile for an icon row, flush
                                    // for a plain one.
                                    if (index > 0) {
                                        SettingsOptionDivider(
                                            startInset = if (row.plain) 0.dp else 53.dp
                                        )
                                    }
                                    val open = {
                                        navController.navigate(row.route) { launchSingleTop = true }
                                    }
                                    if (row.route == CurioRoutes.SETTINGS_APPEARANCE) {
                                        // The Appearance row stays the pet's
                                        // Settings landmark: the pet pokes it,
                                        // and the tour's Settings stop points
                                        // at it. (This used to live on the
                                        // designed Appearance card.)
                                        PetLandmark(
                                            id = "appearance",
                                            kind = PetLandmarks.Kind.FUN,
                                            screen = "settings"
                                        ) { lm ->
                                            SettingsOptionRow(
                                                icon = row.icon,
                                                title = row.title,
                                                subtitle = row.subtitle,
                                                modifier = lm,
                                                onClick = open
                                            )
                                        }
                                    } else {
                                        SettingsOptionRow(
                                            icon = row.icon,
                                            title = row.title,
                                            subtitle = row.subtitle,
                                            plain = row.plain,
                                            onClick = open
                                        )
                                    }
                                }
                            }
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
                                item { SettingsSectionHeading(sectionLabel) }
                                results.forEach { result ->
                                    item {
                                        SettingsNavRow(
                                            icon = result.row.icon,
                                            title = result.row.title,
                                            subtitle = result.row.subtitle,
                                            selected = sectionPageFor(result.row.route)?.name == selectedPageName,
                                            enabled = settingsNavEntryEnabled(rowIdForRoute(result.row.route))
                                        ) { handleRow(result.row, result.deep) }
                                    }
                                }
                            }
                        }
                    } else {
                        sections.forEach { section ->
                            item { SettingsSectionHeading(section.label) }
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
                                                        selected = sectionPageFor(row.route)?.name == selectedPageName,
                                                        enabled = settingsNavEntryEnabled(rowIdForRoute(row.route))
                                                    ) { handleRow(row) }
                                                }
                                            }
                                        } else {
                                            SettingsNavRow(
                                                icon = row.icon,
                                                title = row.title,
                                                subtitle = row.subtitle,
                                                selected = sectionPageFor(row.route)?.name == selectedPageName,
                                                enabled = settingsNavEntryEnabled(rowIdForRoute(row.route))
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
                item { SettingsSectionHeading(selectedPage.title) }
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
    CurioRoutes.SETTINGS_ADVANCED -> SettingsPage.ADVANCED
    CurioRoutes.EXPERIMENTS -> null // standalone screen, not a section page
    CurioRoutes.USER_EXPERIMENTS -> null // standalone screen
    else -> null
}

/** v3xx51 — the gate id for a settings row route: only the Pet designer is
 *  conditioned (see [settingsNavEntryEnabled]). */
private fun rowIdForRoute(route: String): String =
    if (route == CurioRoutes.PET_DESIGNER) "pet" else ""

/** A nav-list row for the two-pane hub: icon + label, with the selected
 *  page's row wearing a soft action tint so the active section reads at a
 *  glance. */
@Composable
private fun SettingsNavRow(
    icon: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    /** v3xx51 — greyed + un-tappable when its screen is gated (Pet designer
     *  while Curie is off). */
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        color = if (selected) curioDialogActionColor().copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.42f)
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

/** One tappable settings row.
 *
 *  v408 — [plain] is the roomy variant: no icon tile, and the copy may run
 *  to three lines. It exists for the four "front door" rows (Online mode,
 *  Recycle bin, Updates, Support & diagnostics), whose one-line subtitles
 *  the old designed cards truncated mid-sentence. */
private data class SettingsRowEntry(
    val icon: String,
    val title: String,
    val subtitle: String,
    val route: String,
    val plain: Boolean = false
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
                    SettingsRowEntry(CurioIcons.Tune, "Preferences", "Search engine, explore, and notification", CurioRoutes.SETTINGS_PREFERENCES),
                    // ── v461 — RECORDING, EXPERIMENTS AND THE PET DESIGNER MOVED ──
                    //
                    // The member: *"maybe simplifying settings, like yk some are
                    // realy confusing to find"*, and to the scope question
                    // **"re-cut, move rarely-used rows into one Advanced page"**.
                    // These three were the offenders: two of them are setup rather
                    // than settings (you choose a voice-note quality once, you draw
                    // your pet once) and Experiments is a try-before-ship door —
                    // and all three sat BETWEEN the member and the rows they do
                    // change, so Appearance and Preferences were the only rows near
                    // the top. They live on the Advanced page now, one row away
                    // (see the Safety & support card below and `AdvancedSection`).
                    // Nothing was dropped: every door still exists, one tap deeper.
                    // v26 — Experiments is hidden from Settings (it opens via
                    // the five-tap version trick in Support); these two moved
                    // in here from the old Explore section so they stay one
                    // tap away next to Appearance.
                    SettingsRowEntry(CurioIcons.DragHandle, "Manage categories", "Show, hide, or reorder lanes", CurioRoutes.MANAGE_CATEGORIES),
                    SettingsRowEntry(CurioIcons.History, "Topic history", "Revisit what you explored", CurioRoutes.TOPIC_HISTORY),
                    // v456 — THE SHARE HUB IS A DEV DOOR NOW. The member:
                    // *"make the share hub hide from settings and its only
                    // accessible from the dev settings"*. It is a gallery for
                    // looking at every share-card design at once, which is a
                    // thing you want while a design is being worked on — not a
                    // row a member needs beside Appearance. The page itself is
                    // untouched and still one tap away: see the Dev page (Support
                    // → five taps on Version), under "Sharing".
                    // v461 — the door to the Advanced page (Recording, Experiments
                    // and the Pet designer moved inside it — see "Personalize").
                    SettingsRowEntry(CurioIcons.Settings, "Advanced", "Recording, experiments and the pet designer", CurioRoutes.SETTINGS_ADVANCED)
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
                    // v3xx — the account + Online Mode page. v408 — a PLAIN
                    // row, and its copy is the full sentence the designed
                    // secondary card carried: the row is the place for it
                    // now, with room to wrap.
                    SettingsRowEntry(CurioIcons.Refresh, "Online mode", "Sign in, keep your account in sync, and set your privacy rules", CurioRoutes.SETTINGS_ONLINE, plain = true),
                    // v26 — recycle bin for soft-deleted captures.
                    SettingsRowEntry(CurioIcons.Delete, "Recycle bin", "Restore recently deleted captures", CurioRoutes.RECYCLE_BIN, plain = true),
                    // v112 — the dedicated Updates sub-page (its own UI,
                    // replaces the old update card inside Support).
                    SettingsRowEntry(CurioIcons.Download, "Updates", "Your build, release notes & update checker", CurioRoutes.UPDATES, plain = true),
                    // v403 — What's New: this version's highlights, each with a
                    // door straight to the thing it describes. It also opens
                    // itself once per version; this row is the way back.
                    SettingsRowEntry(CurioIcons.AutoAwesome, "What's New", "The highlights of this version, and where to find them", CurioRoutes.WHATS_NEW),
                    // v24 — merged into the shared Support & diagnostics page
                    // (same screen Profile's "Support & diagnostics" opens).
                    // v408 — a PLAIN row (it was the "Help & feedback" card).
                    SettingsRowEntry(CurioIcons.Info, "Support & diagnostics", "Reports, help & app details", CurioRoutes.SUPPORT, plain = true)
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
    SettingsDeepRow(CurioIcons.Wallpaper, "Glyph backdrop", "Subtle or deep background glyphs", CurioRoutes.SETTINGS_APPEARANCE, SettingsPage.APPEARANCE, "appearance-glyph-backdrop"),
    SettingsDeepRow(CurioIcons.Bolt, "Lite mode", "Skips glass and idle effects for a smoother app on slower phones", CurioRoutes.SETTINGS_APPEARANCE, SettingsPage.APPEARANCE, "appearance-lite-mode"),
    SettingsDeepRow(CurioIcons.Contrast, "Paper", "White page with cream cards, or the reverse", CurioRoutes.SETTINGS_APPEARANCE, SettingsPage.APPEARANCE, "appearance-paper"),
    // v411 — the three old rows (Material theme / Hero / Adaptive Hero) are
    // ONE door now: the Color theme sheet. The deep search points at that
    // row, so "material", "azure" and "hero" all still land in
    // Appearance — see the sheet's own keywords below.
    SettingsDeepRow(CurioIcons.Palette, "Color theme", "Curio rose, Azure, Material, Adaptive Hero or a named theme (Jade, Orchid, Ocean, Sand, Ember)", CurioRoutes.SETTINGS_APPEARANCE, SettingsPage.APPEARANCE, "appearance-color-theme"),
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
    // ── Online mode (own screen — no row pulse) ──────────────────────
    SettingsDeepRow(CurioIcons.Refresh, "Online mode", "Sign in to sync your account", CurioRoutes.SETTINGS_ONLINE),
    // Privacy is not a section of its own any more: the rules belong to the
    // account, so they are stated on the Online mode page next to it.
    // ── Updates (v112 — dedicated sub-page) ─────────────────────────
    SettingsDeepRow(CurioIcons.Info, "Version", "App version and build number", CurioRoutes.UPDATES),
    SettingsDeepRow(CurioIcons.AutoAwesome, "What's New", "The highlights of this version", CurioRoutes.WHATS_NEW),
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
// v408 — THE HUB IS A PLAIN LIST NOW.
//
// It used to be the JSX "Settings redesign": 2-up tone cards with
// decorative foot visuals, "big title" tiles and four secondary cards,
// plus a nav rail of its own on top. The member asked for the simpler
// thing — "a list based simpler all settings look the main settings
// page but a simpler and list view" — so the hub renders the
// [SettingsSections] data as plain [SettingsOptionRow]s, and the rail
// belongs to the SECTION pages where switching between them is what it
// is for. The search box, the deep row index and the footer note stay.
// ─────────────────────────────────────────────────────────────────────────────

/** v408 — THE DESIGNED-CARD MACHINERY IS GONE.
 *
 *  The hub's tone cards, their decorative foot visuals, the "big title"
 *  tiles and the four secondary cards — with `settingsToneGradient`,
 *  `settingsCardInk`, `SettingsCardTexture`, `SettingsDesignCardView` and
 *  `SettingsSecondaryCardView` behind them — were deleted with the list
 *  rewrite. [settingsCardChipTint] / [settingsCardTintLift] above SURVIVE:
 *  they are still the shared card-tint family the option cards, pills and
 *  dialogs are built from. */
/** One nav-rail entry (JSX `sideNav` → horizontal chip rail on phones). */
internal data class SettingsNavEntry(
    val id: String,
    val label: String,
    val icon: String,
    val route: String? = null
)

/** The settings side-nav, in order. It lives on the SECTION pages only
 *  (v408 — the hub is a plain list with no rail of its own), and the old
 *  leading "All Settings" chip is gone with it: every section's back pill
 *  and its own chip already say where you are and how to leave. */
private val settingsNavRail = listOf(
    SettingsNavEntry("appearance", "Appearance", CurioIcons.DarkMode, CurioRoutes.SETTINGS_APPEARANCE),
    SettingsNavEntry("pet", "Pet designer", CurioIcons.Pets, CurioRoutes.PET_DESIGNER),
    SettingsNavEntry("preferences", "Preferences", CurioIcons.Tune, CurioRoutes.SETTINGS_PREFERENCES),
    SettingsNavEntry("recording", "Recording", CurioIcons.Mic, CurioRoutes.SETTINGS_RECORDING),
    SettingsNavEntry("categories", "Categories", CurioIcons.DragHandle, CurioRoutes.MANAGE_CATEGORIES),
    SettingsNavEntry("history", "Topic history", CurioIcons.History, CurioRoutes.TOPIC_HISTORY),
    SettingsNavEntry("experiments", "Experiments", CurioIcons.AutoAwesome, CurioRoutes.USER_EXPERIMENTS),
    SettingsNavEntry("backup", "Backup", CurioIcons.Backup, CurioRoutes.SETTINGS_DATA),
    SettingsNavEntry("online", "Online", CurioIcons.Refresh, CurioRoutes.SETTINGS_ONLINE),
    // v461 — the Advanced page is a page, so it gets its own chip: the rail is how
    // a member crosses between sections, and a section that is not on it cannot
    // be reached from any other one. (The nested doors keep their own chips too —
    // they still are their own screens, one tap from the rail.)
    SettingsNavEntry("advanced", "Advanced", CurioIcons.Settings, CurioRoutes.SETTINGS_ADVANCED),
    SettingsNavEntry("support", "Support", CurioIcons.SupportAgent, CurioRoutes.SUPPORT)
)

/** Whether a settings entry is tappable right now. Only the Pet designer
 *  has a live gate: it edits the companion, so with Curie switched off the
 *  hub card / rail chip / two-pane row grey out and do nothing. */
@Composable
private fun settingsNavEntryEnabled(id: String): Boolean =
    if (id == "pet") AppPreferences.petEnabledState else true


/** The shared-element key for the settings nav rail's active pill — every
 *  rail-bearing screen marks its ACTIVE chip with this same key, so
 *  switching sections morphs the highlight from the old screen's chip to
 *  the new screen's chip across the page transition. */
private const val SettingsRailActiveKey = "settings-rail-active"

/** The active rail chip's pill colour — painted in the chip's own layer AND
 *  by the shared element that glides between chips (see [SettingsNavRail]). */
private val SettingsRailAccent = Color(0xFF815947)

/** Bounds animation for the rail morph — a snappy, VISIBLE glide. v3xx44:
 *  0.9 / 320 settles in ~200ms, so the highlight lands with the tap instead
 *  of lagging behind it (v3xx42's 0.8 / 140 took ~350ms and read as "too
 *  slow"), while still being a real travel rather than the old stiffness-500
 *  instant snap (~150ms, no moving highlight).
 *  v3xx50 — CRITICALLY damped (1.0 / 420): the old 0.9 overshoot left the
 *  pill visibly "coming to rest" after it had already arrived, which the
 *  user read as the rail being slow. No overshoot, so it lands and STOPS. */
private val SettingsRailBoundsTransform = BoundsTransform { _, _ ->
    spring(dampingRatio = 1f, stiffness = 420f)
}

/**
 * The JSX nav rail — horizontal chips on phones (the desktop sidebar's
 * mobile twin). "All Settings" returns to the hub itself.
 *
 * [active] is the currently open rail page: it is highlighted in its
 * NATURAL slot (the rail never reorders) and the row composes ALREADY at
 * that chip — the header stays perfectly still across section switches
 * (the old animated auto-scroll glided the row from index 0 on every
 * page open, which read as a jump on top of the page fade). Pass null on
 * settings-family screens that aren't a rail destination (drill-in tool
 * pages): nothing is highlighted. Shared by the hub AND every settings
 * sub-page.
 *
 * v433 — [navController] is CARRIED but not used: every settings-family screen
 * hands it in, and the quick-tool row it used to feed was removed on the
 * member's instruction ("from the settings sub pages remove the quick row and
 * its suggestions"). It is kept because the alternative is an edit in eighteen
 * screens for a parameter nobody reads; a future door that really needs the rail
 * to navigate should use it rather than adding a second one.
 */
@Composable
internal fun SettingsNavRail(
    active: String?,
    onSelect: (SettingsNavEntry) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavController? = null
) {
    val dark = isCurioDarkTheme()
    // v3xx — the rail keeps its FIXED order: the opened page is highlighted
    // in its NATURAL slot (never rotated next to "All Settings"). The row
    // starts AT the active chip via the initial index, so no big scroll runs
    // on composition — the header never glides. (The old animateScrollToItem
    // re-animated from index 0 on every page open: the rail visibly jumped
    // on top of the page transition.)
    // v3xx44 — the row composes ALREADY SCROLLED to centre the active chip
    // (v3xx40 asked for the active chip mid-viewport). The rail's geometry is
    // fixed (82dp chips on a 7dp rhythm), so the offset can be computed up
    // front and handed to the list as its INITIAL scroll — nothing scrolls
    // after composition. The old post-composition `scrollToItem` correction
    // snapped the whole header sideways one frame AFTER every section switch,
    // right underneath the gliding highlight — that is what read as broken.
    val railChipWidth = 82.dp
    val railChipPitch = 89.dp
    val railViewport = (LocalConfiguration.current.screenWidthDp.dp - 40.dp)
        .coerceAtLeast(railChipPitch)
    val railActiveIndex = active?.let { id -> settingsNavRail.indexOfFirst { it.id == id } } ?: -1
    val railInitialOffset = with(LocalDensity.current) {
        if (railActiveIndex <= 0) 0
        // Dp arithmetic: the pitch must be the receiver (Int * Dp has no
        // operator — Dp.times(Int) does).
        else (railChipPitch * railActiveIndex - (railViewport - railChipWidth) / 2)
            .coerceAtLeast(0.dp)
            .roundToPx()
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
        initialFirstVisibleItemScrollOffset = railInitialOffset
    )
    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(settingsNavRail, key = { it.id }) { entry ->
                val selected = active != null && active == entry.id
                // v3xx51 — the Pet designer chip greys out while Curie is off
                // (the designer edits the companion, so there is nothing to
                // design) — the whole entry is un-tappable, not just hidden.
                val entryEnabled = settingsNavEntryEnabled(entry.id)
                // v3xx — the ACTIVE chip's pill is a SHARED ELEMENT: every
                // settings-family screen marks its active chip with the same
                // key, so switching sections morphs the highlight from the
                // old screen's chip to the new screen's chip while the pages
                // crossfade (the iOS-style rail glide). Falls back to a plain
                // pill when the shared scopes are absent.
                // v3xx50 — the chip ALSO paints its own fill the instant it
                // becomes active. The shared element is only drawn in the
                // transition overlay (the arriving chip's own instance is
                // hidden while the glide runs), so with a transparent chip
                // the active label sat on the pale frosted tile — cream on
                // near-white — until the pill landed: the reported "the text
                // disappears for a moment on the active indicator". The
                // layer below and the overlay are the same colour and shape,
                // so once the glide lands they are indistinguishable.
                val sharedScope = LocalRevealSharedScope.current
                val visScope = LocalRevealVisibilityScope.current
                val activeState = if (selected && sharedScope != null && visScope != null)
                    sharedScope.rememberSharedContentState(SettingsRailActiveKey)
                else null
                // v3xx51 — THE FIX for "the active pill's text doesn't show
                // while it moves": the shared element used to carry ONLY the
                // accent fill, so while it glided (and through the settle)
                // the opaque overlay paint sat ON TOP of the arriving chip's
                // icon + label — the text was hidden for the whole flight.
                // The shared element now carries the chip's COMPLETE look
                // (fill + icon + label), so the label travels WITH the pill
                // and is readable the entire time; it lands pixel-identical
                // over the chip's own copy.
                val chipContent: @Composable BoxScope.(Boolean) -> Unit = { isSelected ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .matchParentSize()
                            .padding(horizontal = 6.dp, vertical = 9.dp)
                    ) {
                        CurioIcon(
                            name = entry.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFFFFF9F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 19.dp
                        )
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp
                            ),
                            color = if (isSelected) Color(0xFFFFF9F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(82.dp)
                        .heightIn(min = 60.dp)
                        .clip(RoundedCornerShape(17.dp))
                        // Unselected chips keep their frosted tile; the
                        // selected chip paints the pill in its own layer (see
                        // above) with the shared element gliding on top.
                        .background(
                            when {
                                selected -> SettingsRailAccent
                                // v408 — the rail chips are TABS and they were
                                // blending into the page: a 62%-white tile over
                                // the section's hero wash resolved to the wash
                                // itself, so an unselected tab read as bare
                                // page with an icon on it. Opaque white (the
                                // card ladder's card step) separates by
                                // lightness; dark keeps its raised step.
                                dark -> MaterialTheme.colorScheme.surfaceContainerHigh
                                else -> Color.White
                            }
                        )
                        // v411 — NO TAB EDGE. This tab used to draw a hairline
                        // (and a transparent one when selected). The app's cards
                        // carry a soft shadow instead of borders now, and an
                        // unselected tab is an OPAQUE tile sitting on the card,
                        // so it separates by lightness without either.
                        .clickable(enabled = entryEnabled) { onSelect(entry) }
                        .alpha(if (entryEnabled) 1f else 0.42f)
                ) {
                    if (selected && activeState != null && sharedScope != null && visScope != null) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .then(
                                    sharedScope.run {
                                        Modifier.sharedElement(
                                            activeState,
                                            visScope,
                                            boundsTransform = SettingsRailBoundsTransform
                                        )
                                    }
                                )
                                .clip(RoundedCornerShape(17.dp))
                                .background(SettingsRailAccent)
                        ) { chipContent(true) }
                    } else {
                        chipContent(selected)
                    }
                }
            }
        }
        // ── v433 — AND THE QUICK ROW IS GONE ────────────────────────────
        //
        // A rotating row of deep settings used to sit under these chips on every
        // settings page ("QUICK", four suggestions that changed on each visit).
        // The member: *"from the settings sub pages remove the quick row and its
        // suggestions"*. The rail is the settings PAGES now and nothing else — a
        // shortcut row under a list of pages was a second, shallower index of the
        // same thing, and one that moved every time it was looked at.
    }
}

// ── v433 — REMOVED: the quick-tool row (QuickTool + its rotation, the per-show
// cycle and the chip row itself). See the note in [SettingsNavRail].
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
            // v408 — the search field is a CARD, so it follows the ladder:
            // opaque white in light (opaque raised step in dark) with the
            // shared hairline, instead of 70% white fading into the wash.
            // v411 — soft shadow FIRST, then the fill (no hairline: see the
            // card-edge rule). Order is the whole trick: a shadow after the
            // fill paints a blur over the card.
            .curioCardShadow(RoundedCornerShape(19.dp), 2.dp)
            .background(
                if (dark) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerLow
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

/** The JSX footer note — a soft panel under everything.
 *  v3xx40 — the ✦✧✦ dots line is GONE (read as decoration noise) and the
 *  light-mode panel is a quieter blend of the page background (the old
 *  hard beige block clashed with the wash). */
@Composable
private fun SettingsFooterNote() {
    val dark = isCurioDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                else androidx.compose.ui.graphics.lerp(
                    MaterialTheme.colorScheme.background,
                    settingsRoseAccent(),
                    0.07f
                )
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f),
                RoundedCornerShape(26.dp)
            )
            .padding(vertical = 24.dp, horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
