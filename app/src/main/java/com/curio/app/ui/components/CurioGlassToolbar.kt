package com.curio.app.ui.components

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioPillTintLift
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * v3xx — the GLASS TOOLBAR header (the app-wide "Glass toolbar header"
 * style option, the old Cabinet v2 toolbar look): a CONTENT-HEIGHT
 * liquid-glass bar — its own rose-tinted frost, noticeably more blurry
 * than the small pills — holding a back pill + title/subtitle (+ optional
 * action pills) and a search field that morphs in place of the title when
 * opened. It replaces the torn paper banner on every Settings-family and
 * Cabinet screen (and Home/Profile) when the style is selected.
 *
 * Height is content-driven: status bar + one title row + the optional
 * action pills; opening search swaps the title block for the search field
 * in place (the bar keeps its height, like the torn hero's morph).
 */
@Composable
fun CurioGlassToolbar(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    // v3xx — action pills riding the top row beside the back pill. Receives
    // the toolbar's readable ink for the pill glass.
    trailing: (@Composable (ink: Color) -> Unit)? = null,
    searchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    searchFocus: FocusRequester? = null,
    searchPlaceholder: String = "Search…",
    // v3xx — optional control that rides INSIDE the bar beside the title
    // (directly under the trailing pills): the Topic Database's Category
    // pill. Screens that don't pass it render the plain title.
    titleTrailing: (@Composable (ink: Color) -> Unit)? = null,
    // v3xx — optional extra content row below the title/subtitle block
    // (Home's stat segments, Profile's stats) — the bar grows to fit it.
    content: (@Composable (ink: Color) -> Unit)? = null,
    // v3xx — the local liquid-glass capture (sibling overlay architecture)
    // so the back pill refracts the rows scrolling behind the bar.
    glassBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null
) {
    val dark = isCurioDarkTheme()
    // The toolbar's OWN tint: the rose hero accent pushed into the surface
    // glass at a soft weight so the bar reads tinted (not plain gray) while
    // the theme's text colors stay clearly visible on it.
    val rose = settingsRoseAccent()
    val container = lerp(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        rose,
        if (dark) 0.14f else 0.20f
    )
    val ink = MaterialTheme.colorScheme.onSurface

    val glassMod = when {
        isLiquidGlassPillsActive() && glassBackdrop != null ->
            Modifier.liquidGlassCapsule(
                container = container.copy(alpha = 0.92f),
                washAlpha = 0.62f,
                backdrop = glassBackdrop,
                shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp),
                // v3xx — the glass-toolbar bar is deliberately MORE blurry
                // than the small pills (1.6× the standard 8dp frost).
                blurMultiplier = 1.6f
            )
        isLiquidGlassRequested() -> Modifier.fauxGlassCapsule(container, corner = 26.dp)
        else -> Modifier.background(container.copy(alpha = 0.96f))
    }

    Column(
        modifier = glassMod
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (onBack != null) {
                CurioBackButton(
                    onClick = onBack,
                    modifier = Modifier
                        .then(
                            if (glassBackdrop != null && isInScreenGlassActive())
                                Modifier.liquidGlassCapsule(
                                    if (dark) {
                                        lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Color.Black, 0.15f)
                                    } else {
                                        lerp(container, curioPillTintLift(), 0.38f)
                                    },
                                    washAlpha = 0.45f,
                                    backdrop = glassBackdrop,
                                    blurMultiplier = 1.6f
                                )
                            else Modifier
                        ),
                    containerColor = if (dark) {
                        lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Color.Black, 0.15f)
                    } else {
                        lerp(container, curioPillTintLift(), 0.38f)
                    },
                    contentColor = ink,
                    shadowElevation = 3.dp,
                    disableRipple = true
                )
                Spacer(Modifier.width(10.dp))
            }
            if (searchActive) {
                // v3xx — the search field owns the row while open (mirrors
                // the torn hero: back pill stays, title block swaps out).
            } else if (trailing != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    trailing(ink)
                }
            }
        }
        // ── Title + subtitle OR the morph-open search field — the search
        // bar scales in from the pills' position when opened (the same
        // scale/fade morph as the torn heroes).
        AnimatedContent(
            targetState = searchActive,
            transitionSpec = {
                if (targetState) {
                    (scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.92f)
                        + fadeIn(tween(280, easing = FastOutSlowInEasing)))
                        .togetherWith(fadeOut(tween(200)))
                } else {
                    (fadeIn(tween(280, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            scaleOut(tween(200, easing = FastOutSlowInEasing), targetScale = 0.92f)
                                + fadeOut(tween(200))
                        )
                }
            },
            label = "glassToolbarSearchExpand"
        ) { active ->
            if (active) {
                CurioSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = searchPlaceholder,
                    ink = MaterialTheme.colorScheme.onSurface,
                    fill = curioSearchFill(container),
                    onCancel = onCloseSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                        .then(
                            if (searchFocus != null) Modifier.focusRequester(searchFocus)
                            else Modifier
                        )
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = ink.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (titleTrailing != null) {
                        titleTrailing(ink)
                    }
                }
                if (content != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    ) {
                        content(ink)
                    }
                }
            }
        }
    }
}

/**
 * v3xx — the MORPHING glass toolbar for Home / Profile (the "Glass toolbar
 * header" style on those two screens): a PINNED collapsing header. At the
 * top of the page it is the full content-height glass bar (leading pill +
 * title/subtitle + avatar + the stat content row); as the page scrolls it
 * COLLAPSES smoothly — scrubbed by the finger through [progress] — down to
 * a slim compact bar that keeps just [compactTitle] + the profile avatar,
 * so the identity row never scrolls away. The full content fades out
 * (rising slightly) while the compact row fades in and the bar's height
 * eases toward [compactHeight]; everything clips inside the glass capsule.
 *
 * SAME glass recipe as [CurioGlassToolbar] (rose-tinted frost, 1.6× blur).
 * Must be a SIBLING overlay of the page's glass capture (like the floating
 * nav pills) so it can sample the real backdrop — never inside the captured
 * subtree (the v228 self-capture cycle).
 */
@Composable
fun CurioGlassToolbarMorph(
    // 0 = full hero, 1 = fully collapsed to the compact identity bar.
    progress: Float,
    // The collapsed bar's height (below the status bar).
    compactHeight: Dp,
    title: String,
    subtitle: String,
    // What the collapsed bar shows beside the avatar (the display name —
    // "Curious Explorer" by default).
    compactTitle: String,
    onBack: (() -> Unit)? = null,
    // v3xx — a leading MENU pill (Home's drawer) when [onBack] is null.
    onMenuClick: (() -> Unit)? = null,
    // v3xx — right-side pills riding the FULL bar's top row (and the end of
    // the compact row): Home's avatar, Profile's Settings pill.
    trailing: (@Composable (ink: Color) -> Unit)? = null,
    // v3xx — a pill riding beside the title in the FULL bar (Profile's
    // avatar).
    titleTrailing: (@Composable (ink: Color) -> Unit)? = null,
    // v3xx — the stat content row inside the FULL bar (Home's Streak ·
    // Cabinet · Topics, Profile's Level · Saved · Lanes).
    content: (@Composable (ink: Color) -> Unit)? = null,
    // v3xx — the avatar shown in the compact row beside [compactTitle].
    compactAvatar: (@Composable () -> Unit)? = null,
    // v3xx19 — the compact bar's glass pills: the streak counter (fire +
    // days, opens the quests journey) and the Edit action (Profile). They
    // ride the collapsed bar beside the name so the streak stays visible
    // and Profile stays editable while the header is shrunk.
    streakCount: Int? = null,
    onStreakClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    glassBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null,
    modifier: Modifier = Modifier
) {
    val dark = isCurioDarkTheme()
    // The toolbar's OWN rose-tinted frost — the same recipe as
    // [CurioGlassToolbar] (see its tint note).
    val rose = settingsRoseAccent()
    val container = lerp(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        rose,
        if (dark) 0.14f else 0.20f
    )
    val ink = MaterialTheme.colorScheme.onSurface
    val eased = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
    val compactH = with(LocalDensity.current) { compactHeight.toPx() }
    // Measured once from the full column's natural height (reported through
    // [Modifier.layout] below) — drives the fade-out rise so the full
    // content lifts as it collapses instead of just clipping.
    var fullH by remember { mutableIntStateOf(0) }

    val glassMod = when {
        isLiquidGlassPillsActive() && glassBackdrop != null ->
            Modifier.liquidGlassCapsule(
                container = container.copy(alpha = 0.92f),
                washAlpha = 0.62f,
                backdrop = glassBackdrop,
                shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp),
                // v3xx — the glass-toolbar bar is deliberately MORE blurry
                // than the small pills (1.6× the standard 8dp frost).
                blurMultiplier = 1.6f
            )
        isLiquidGlassRequested() -> Modifier.fauxGlassCapsule(container, corner = 26.dp)
        else -> Modifier.background(container.copy(alpha = 0.96f))
    }

    // The leading pill (back or menu) — the same glass-capsule treatment as
    // [CurioGlassToolbar]'s back button. Rendered in BOTH the full and the
    // compact rows so it rides the morph between the two slots.
    val leadingPill: @Composable (Color) -> Unit = { pillInk ->
        if (onMenuClick != null) {
            val pillBg = if (dark) lerp(container, Color.Black, 0.15f)
            else lerp(container, curioPillTintLift(), 0.38f)
            Surface(
                onClick = onMenuClick,
                shape = CircleShape,
                color = pillBg,
                contentColor = pillInk,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .then(
                        if (glassBackdrop != null && isInScreenGlassActive())
                            Modifier.liquidGlassCapsule(
                                pillBg,
                                washAlpha = 0.45f,
                                backdrop = glassBackdrop,
                                blurMultiplier = 1.6f
                            )
                        else Modifier
                    )
                    .size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CurioIcon(
                        name = CurioIcons.Menu,
                        contentDescription = "Open menu",
                        size = 19.dp,
                        tint = pillInk
                    )
                }
            }
        } else if (onBack != null) {
            CurioBackButton(
                onClick = onBack,
                modifier = Modifier.then(
                    if (glassBackdrop != null && isInScreenGlassActive())
                        Modifier.liquidGlassCapsule(
                            if (dark) lerp(container, Color.Black, 0.15f)
                            else lerp(container, curioPillTintLift(), 0.38f),
                            washAlpha = 0.45f,
                            backdrop = glassBackdrop,
                            blurMultiplier = 1.6f
                        )
                    else Modifier
                ),
                containerColor = if (dark) lerp(container, Color.Black, 0.15f)
                else lerp(container, curioPillTintLift(), 0.38f),
                contentColor = pillInk,
                shadowElevation = 3.dp,
                disableRipple = true
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            // Measure the content once at its natural height, then REPORT
            // the animated (collapsed) height — the clip below trims the
            // overflow so the bar visibly shrinks with the scroll.
            .layout { measurable, constraints ->
                val full = measurable.measure(constraints)
                val targetH = androidx.compose.ui.util.lerp(full.height.toFloat(), compactH, eased).toInt().coerceAtLeast(1)
                layout(full.width, targetH) { full.place(0, 0) }
            }
            .clipToBounds()
            .then(glassMod)
    ) {
        // ── FULL state — fades out + rises as the bar collapses. More
        // EXPANDED than the plain content bar: the title row breathes and
        // the stat row sits in a proper rose-gradient stat CARD (glow +
        // shadow + rounded pane — the torn hero's stat-pane construction),
        // so the resting glass header reads like the hero it replaces.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { if (it.height > 0) fullH = it.height }
                .graphicsLayer {
                    alpha = 1f - eased
                    translationY = -eased * ((fullH - compactH).coerceAtLeast(0f)) * 0.4f
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                leadingPill(ink)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = ink.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (titleTrailing != null) titleTrailing(ink)
                // v3xx19 — [trailing] rides the FULL bar's top row only
                // (Profile's Settings pill): the collapsed bar keeps just
                // the avatar + name + streak + edit per the request.
                if (trailing != null) trailing(ink)
            }
            if (content != null) {
                val statPaneShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                ) {
                    // The stat CARD — the torn hero's rose gradient pane
                    // (opaque theme-aware blend, glow + shadow, 20dp
                    // rounded), built on the bar's own rose container so
                    // the pane reads part of the glass.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .curioDarkGlow(3.dp, statPaneShape)
                            .shadow(3.dp, statPaneShape, clip = false)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        lerp(container, Color.White, 0.06f),
                                        lerp(container, Color.White, 0.26f)
                                    )
                                ),
                                statPaneShape
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        content(ink)
                    }
                }
            }
        }
        // ── COMPACT state — fades in as the bar collapses.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(compactHeight)
                .padding(start = 12.dp, end = 12.dp)
                .graphicsLayer { alpha = eased },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            leadingPill(ink)
            compactAvatar?.invoke()
            Text(
                compactTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (streakCount != null && onStreakClick != null) {
                val pillBg = if (dark) lerp(container, Color.Black, 0.15f)
                else lerp(container, curioPillTintLift(), 0.38f)
                // The streak glass pill — fire + days, opens the quests.
                Surface(
                    onClick = onStreakClick,
                    shape = RoundedCornerShape(50),
                    color = pillBg,
                    contentColor = ink,
                    shadowElevation = 3.dp,
                    modifier = Modifier.then(
                        if (glassBackdrop != null && isInScreenGlassActive())
                            Modifier.liquidGlassCapsule(
                                pillBg,
                                washAlpha = 0.45f,
                                backdrop = glassBackdrop,
                                blurMultiplier = 1.6f
                            )
                        else Modifier
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        CurioIcon(
                            name = "local_fire_department",
                            contentDescription = "Streak",
                            size = 15.dp,
                            tint = ink
                        )
                        Text(
                            "$streakCount",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = ink
                        )
                    }
                }
            }
            if (onEditClick != null) {
                val pillBg = if (dark) lerp(container, Color.Black, 0.15f)
                else lerp(container, curioPillTintLift(), 0.38f)
                // The Edit glass pill — opens the profile editor (Profile).
                Surface(
                    onClick = onEditClick,
                    shape = CircleShape,
                    color = pillBg,
                    contentColor = ink,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .then(
                            if (glassBackdrop != null && isInScreenGlassActive())
                                Modifier.liquidGlassCapsule(
                                    pillBg,
                                    washAlpha = 0.45f,
                                    backdrop = glassBackdrop,
                                    blurMultiplier = 1.6f
                                )
                            else Modifier
                        )
                        .size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CurioIcon(
                            name = CurioIcons.Edit,
                            contentDescription = "Edit profile",
                            size = 19.dp,
                            tint = ink
                        )
                    }
                }
            }
        }
    }
}