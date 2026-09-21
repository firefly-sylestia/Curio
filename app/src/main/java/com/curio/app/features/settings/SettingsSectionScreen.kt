package com.curio.app.features.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import com.curio.app.ui.components.liquidGlassCapsule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.AudioQuality
import com.curio.app.data.AudioQualitySettings
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.MusicService
import com.curio.app.data.SearchEngine
import com.curio.app.data.VoskModelDownloads
import com.curio.app.data.VoskModels
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.curioPressClickable
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.CurioCardHeader
import com.curio.app.ui.components.formatHour
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.FrauncesFontFamily
import com.curio.app.ui.theme.CurioNamedTheme
import com.curio.app.ui.theme.LocalCurioThemeTransition
import com.curio.app.ui.theme.curioTintOn
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.headerAccent
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.toHsl
import com.curio.app.ui.theme.switchThemeWithReveal
import com.curio.app.ui.theme.CurioThemeTransitionState
import com.curio.app.ui.theme.switchVisualThemeWithReveal
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/** Settings destination selected from the compact hub. */
enum class SettingsPage(val title: String, val subtitle: String) {
    APPEARANCE("Appearance", "Theme, tint and mood"),
    // v26 — Preferences: the behavioral settings that aren't about how the
    // app LOOKS — search engine, explore sessions and the floating bubble,
    // the pet's chatter/games personality, and (v27) every notification
    // control: the daily shuffle reminder + hour chips and the explore
    // dialog's bubble opt-in row (the Notifications section is gone).
    PREFERENCES("Preferences", "Search, explore, and pet behavior"),
    RECORDING("Recording", "Voice-note quality, dictation and offline transcription"),
    DATA("Backup & restore", "Keep your captures safe")
}

/**
 * v27t — the rows of one settings page, standalone. Rendered by
 * [SettingsSectionScreen] behind its hero, and reused by the wide two-pane
 * hub ([SettingsHubScreen]) so the tablet Settings screen shows the nav list
 * on the left and the selected page's options on the right.
 */
@Composable
internal fun SettingsPageContent(
    page: SettingsPage,
    navController: NavController,
    highlightKey: String? = null
) {
    // v115 — every sub-page's options sit in the same paper card as the
    // hub rows, so the section screens read as proper settings options
    // instead of transparent rows floating on the backdrop.
    SettingsOptionCard {
        when (page) {
            SettingsPage.APPEARANCE -> AppearanceSection(highlightKey)
            SettingsPage.PREFERENCES -> PreferencesSection(highlightKey)
            SettingsPage.RECORDING -> RecordingSection(highlightKey)
            SettingsPage.DATA -> DataSection(navController, highlightKey)
        }
    }
}

@Composable
fun SettingsSectionScreen(navController: NavController, page: SettingsPage) {
    // ── Deep-search highlight (v8.0) — the hub's search hands over the exact
    // row key when a result points inside this sub-section; scroll to it and
    // pulse it once on entry.
    val highlightKey = remember { SettingsHighlightTarget.takeIf { it.page == page }?.rowKey }
    LaunchedEffect(Unit) {
        SettingsHighlightTarget.page = null
        SettingsHighlightTarget.rowKey = null
    }
    val listState = rememberLazyListState()
val glassBackdrop = rememberLayerBackdrop()
    // v-tablet — the torn hero is NOT sticky on wide windows (landscape
    // tablet): it leads the list as its first item and scrolls away with it;
    // the pinned glass overlay stays phone-only.
    val wide = windowWidthSizeClass().isWide
    // v255 — the hero is now item 0 of the list; the highlight target is
    // the page-content item that follows the section label.
    LaunchedEffect(highlightKey) {
        if (highlightKey != null) listState.scrollToItem(2)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            // v31 — settings sub-pages wear the same soft page tint as the
            // Settings hub and Profile (a small rose-lean of the background
            // shade, in every theme; the spin-lane wash when Adaptive Hero
            // is on) instead of the plain cream background.
            .background(heroPageBackground(androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)))
    ) {
        // ── Watermark backdrop — muted category glyphs behind the content
        // (wildcard sparkle leads; settings is category-neutral).
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
        // its own status-bar inset for the back pill) — Profile/Home style.
        // The hero is drawn LAST (on top of the scroll content): the rows
        // scroll UP and disappear behind the ragged tear instead of clipping
        // at a straight line.
        LazyColumn(
            state = listState,
            modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
            // v255 — SCROLLING HERO (the Home/Profile construction): the
            // banner lives INSIDE the list and scrolls away with the page.
            contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = if (wide) 0.dp else SettingsHeroTotalHeight, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (wide) {
                item(key = "hero", contentType = "hero") {
                    SettingsHeroHeader(
                        title = page.title,
                        subtitle = page.subtitle,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            // v3xx — the shared settings nav rail: switch sections without
            // going back to the hub (the open page sits in the 2nd slot).
            item(key = "settings-nav", contentType = "settings-nav") {
                SettingsNavRail(
                    active = when (page) {
                        SettingsPage.APPEARANCE -> "appearance"
                        SettingsPage.PREFERENCES -> "preferences"
                        SettingsPage.RECORDING -> "recording"
                        SettingsPage.DATA -> "backup"
                    },
                    onSelect = { navigateToSettingsSection(navController, it) },
                    navController = navController
                )
            }
                        item { SettingsSectionHeading(page.title) }
            item {
                SettingsPageContent(page, navController, highlightKey)
            }
        }
        // Side scroll indicator — thin overlay knob, grows on touch.
        CurioVerticalScrollIndicator(
            state = listState.scrollIndicatorState,
            onScrollBy = { listState.dispatchRawDelta(it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = 16.dp)
        )
                // RESTORED (user request) — STICKY HERO drawn on TOP of the scroll
        // content: rows slide under the ragged tear as they scroll up, and
        // the back pill refracts them through REAL liquid glass.
        // v-tablet — pinned overlay is phone-only; wide windows scroll the
        // hero as the list's first item instead.
        if (!wide) {
            SettingsHeroHeader(title = page.title, subtitle = page.subtitle, onBack = { navController.popBackStack() }, glassBackdrop = glassBackdrop)
        }

    }
}

@Composable
private fun AppearanceSection(highlightKey: String? = null) {
    val context = LocalContext.current
    // v411 — the Color theme sheet (Material / hero / Adaptive Hero folded
    // into ONE picker with previews).
    var colorThemeSheet by remember { mutableStateOf(false) }
    // v412b — the sheet's reveal scope and transition state live HERE, in the
    // section's always-composed body: a rememberCoroutineScope is bound to the
    // composition position it is remembered at, so one remembered INSIDE the
    // `if (colorThemeSheet)` block (and one inside the sheet itself) is
    // CANCELLED the moment the sheet leaves composition — which killed the
    // pref write mid-reveal and made every pick a no-op. From here the scope
    // survives the sheet's whole lifecycle.
    val sheetTransition = LocalCurioThemeTransition.current
    val sheetScope = rememberCoroutineScope()
    // v81 — the Theme picker (Light / Dark / System) is back: dark mode is
    // the reimagined pitch-black + glow design (no AMOLED/Material styles).
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsRowPulse(highlightKey == "appearance-theme") {
            // v(theme switch) — selecting a theme plays the Telegram-style
            // circular reveal from the tapped segment. Resolved in composition
            // (the onSelected lambda isn't @Composable).
            val themeTransition = LocalCurioThemeTransition.current
            val transitionScope = rememberCoroutineScope()
            var themeRowBounds by remember { mutableStateOf(Rect.Zero) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { themeRowBounds = it.boundsInWindow() }
            ) {
                ThemeModeSwitch(
                    selected = when (AppPreferences.themeModeState) {
                        AppPreferences.THEME_DARK -> 1
                        AppPreferences.THEME_SYSTEM -> 2
                        else -> 0
                    }
                ) { index ->
                    val mode = when (index) {
                        1 -> AppPreferences.THEME_DARK
                        2 -> AppPreferences.THEME_SYSTEM
                        else -> AppPreferences.THEME_LIGHT
                    }
                    if (mode == AppPreferences.themeModeState) return@ThemeModeSwitch
                    val center = if (themeRowBounds == Rect.Zero) Offset.Zero
                        else Offset(
                            themeRowBounds.left + (index + 0.5f) * themeRowBounds.width / 3f,
                            themeRowBounds.center.y
                        )
                    switchThemeWithReveal(themeTransition, transitionScope, context, center, mode)
                }
            }
        }
        SettingsOptionDivider()
        // v411 — ONE COLOR THEME DOOR. The Material theme, the rose/azure
        // hero and Adaptive Hero used to be three separate switches on this
        // page (three halves of one question: what colour is the app). They
        // are one row now, and tapping it opens the sheet of previews.
        SettingsRowPulse(highlightKey == "appearance-color-theme") {
            ColorThemeRow(onClick = { colorThemeSheet = true })
        }
        SettingsOptionDivider()
        SettingsRowPulse(highlightKey == "appearance-tint") {
            CompactSwitchRow(CurioIcons.Palette, "Category tint", "Colorful page backgrounds", AppPreferences.tintWashEffective()) {
                AppPreferences.setTintWashEnabled(context, it)
            }
        }
        SettingsOptionDivider()
        SettingsRowPulse(highlightKey == "appearance-pastel") {
            CompactSwitchRow(CurioIcons.AutoAwesome, "Pastel colors", "Soft category accents and page tints", AppPreferences.pastelColorsState) {
                AppPreferences.setPastelColorsEnabled(context, it)
            }
        }
        SettingsOptionDivider()
        // v407 — how loud the page's category glyph backdrop is. Subtle is the
        // shipped default (the deep alphas read as a busy scatter behind flat
        // content); Deep restores exactly the pre-v407 collage.
        SettingsRowPulse(highlightKey == "appearance-glyph-backdrop") {
            CompactSegmentedRow(
                CurioIcons.Wallpaper,
                "Glyph backdrop",
                listOf("Subtle", "Deep"),
                if (AppPreferences.glyphBackdropDeepState) 1 else 0
            ) { index ->
                AppPreferences.setGlyphBackdropDeepEnabled(context, index == 1)
            }
        }
        SettingsOptionDivider()
        // v409 — WHICH SURFACE LEADS, for the theme that still has the choice:
        // Adaptive Hero. "White page" is a white page with cream cards;
        // "Cream page" reverses it to the pre-v409 pair. Both are the same
        // design read in two directions — a card always separates from its page
        // by lightness — so this is a straight visual preference, not a mode.
        // Dark mode is untouched: its page is black and its plates step up.
        // The stored choice is kept, so returning to Adaptive Hero restores
        // exactly what was picked.
        // v421 — THE PAPER ROW BELONGS TO ADAPTIVE HERO.
        //
        // Curio rose and Azure no longer have a paper choice: both wear the
        // fixed SILVER page (see `CurioSilverLightScheme`), because the cream
        // was what made the two default themes read heavy and the flip was one
        // decision too many on the themes a member lands on (user request:
        // "remove that paper cream color and its option of paper from the
        // default rose and azure"). Adaptive Hero still tints only the page
        // wash and the torn hero — the card ladder under them is still this
        // cream/white pair, so the flip has a real effect there and keeps its
        // row. Everywhere else the row is not shown at all: a control that can
        // never do anything is not a choice, it is furniture.
        val paperApplies = !isCurioDarkTheme() &&
            AppPreferences.colorThemeState == AppPreferences.COLOR_THEME_LANE
        if (paperApplies) {
            SettingsRowPulse(highlightKey == "appearance-paper") {
                CompactSegmentedRow(
                    CurioIcons.Contrast,
                    "Paper",
                    listOf("White page", "Cream page"),
                    if (AppPreferences.paperCreamCardsState) 0 else 1
                ) { index ->
                    AppPreferences.setPaperCreamCardsEnabled(context, index == 0)
                }
            }
            SettingsOptionDivider()
        }
        // v8.5 — the Curio pet companion (spec §10): pixel pet + rule-based
        // dialogue + passport/discovery on Quests and Home. Default ON.
        SettingsRowPulse(highlightKey == "appearance-pet") {
            CompactSwitchRow(CurioIcons.Pets, "Curie", "Pixel companion that grows with your XP", AppPreferences.petEnabledState) {
                AppPreferences.setPetEnabled(context, it)
            }
        }
        // v26 — pet chatter + pet games moved to Preferences (the pet's
        // behavior personality is a preference, not a look).
        // v23 — auto-open landed topic is always on now (its toggle was
        // removed) and custom reaction lines are permanently off (their
        // editor is no longer reachable, so the toggle was removed too).
    }
    if (colorThemeSheet) {
        ColorThemeSheet(
            onDismiss = { colorThemeSheet = false },
            transition = sheetTransition,
            transitionScope = sheetScope
        )
    }
}

// ════════════════════════════════════════════════════════════════════════
// v411 — THE THEME CONTROLS
// ════════════════════════════════════════════════════════════════════════

/**
 * LIGHT · DARK · SYSTEM as ONE moving toggle (v411).
 *
 * The member's direction: "they became one row, light gets sun icon, night
 * gets moon icon, system gets half moon half sun style icon. no tick and make
 * it proper toggle style which moves smoothly on switching". So: three icon
 * segments in one capsule, a filled thumb that SLIDES under the chosen one
 * (spring, no snap), no checkmarks, and the glyphs carry the labels (the sun
 * for Light, the crescent for Dark, and the bundled `contrast` mark for
 * System — v412 swapped the hand-drawn half-sun/half-moon for that font glyph,
 * which is the same half-lit circle drawn properly and is what the onboarding's
 * System chip already wore).
 */
@Composable
private fun ThemeModeSwitch(
    selected: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val accent = settingsAccentInk()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val dark = isCurioDarkTheme()
    val trackFill = if (dark) MaterialTheme.colorScheme.surfaceContainerHigh
                    else MaterialTheme.colorScheme.surfaceContainer
    val thumbFill = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, if (dark) 0.30f else 0.16f)
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            SettingsOptionIconTile(CurioIcons.DarkMode, dark)
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            // The current mode said in words, so the icons never have to be
            // guessed at (“no tick” → the words carry the confirmation).
            Text(
                text = listOf("Light", "Dark", "System")[selected.coerceIn(0, 2)],
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = accent
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(CircleShape)
                .background(trackFill)
        ) {
            val segment = maxWidth / 3
            val offset by animateDpAsState(
                targetValue = segment * selected.coerceIn(0, 2),
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
                label = "theme-thumb"
            )
            // The thumb — one filled pill that MOVES to the picked segment.
            Box(
                modifier = Modifier
                    .offset(x = offset)
                    .padding(4.dp)
                    .width(segment - 8.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(thumbFill)
            )
            Row(modifier = Modifier.fillMaxSize()) {
                ThemeModeSegment(
                    selected = selected == 0,
                    accent = accent,
                    muted = muted,
                    base = trackFill,
                    label = "Light",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelected(0) }
                ) { tint -> CurioIcon(CurioIcons.LightMode, null, tint = tint, size = 20.dp) }
                ThemeModeSegment(
                    selected = selected == 1,
                    accent = accent,
                    muted = muted,
                    base = trackFill,
                    label = "Dark",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelected(1) }
                ) { tint -> CurioIcon(CurioIcons.DarkMode, null, tint = tint, size = 20.dp) }
                ThemeModeSegment(
                    selected = selected == 2,
                    accent = accent,
                    muted = muted,
                    base = trackFill,
                    label = "System",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelected(2) }
                // v412 — the bundled `contrast` mark, the same glyph the
                // onboarding's System chip has always worn. The hand-drawn
                // half-sun/half-moon it replaces read as a smudge at 20dp
                // (member: "for the device in theme option change the icon
                // please it looks bad"), and the font glyph is a half-lit
                // circle — the same idea, drawn properly.
                ) { tint -> CurioIcon(CurioIcons.Contrast, "System", tint = tint, size = 20.dp) }
            }
        }
    }
}

/** One segment of [ThemeModeSwitch] — the glyph, centred, in the accent ink
 *  when it is the live one and the muted ink otherwise. */
@Composable
private fun ThemeModeSegment(
    selected: Boolean,
    accent: Color,
    muted: Color,
    /** The capsule's own fill — the colour the muted ink is mixed into
     *  ([curioTintOn]). */
    base: Color,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    glyph: @Composable (Color) -> Unit
) {
    val ink by animateColorAsState(
        targetValue = if (selected) accent else curioTintOn(base, muted, 0.85f),
        animationSpec = tween(220),
        label = "theme-segment-ink"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(onClickLabel = label, onClick = onClick)
    ) {
        glyph(ink)
    }
}

/** One color-theme entry as the sheet shows it: the id the pref stores, the
 *  name, a plain hint, and the three colours the
 *  row previews — page, hero/cards, ink. */
private data class ColorThemeChoice(
    val id: String,
    val label: String,
    val hint: String,
    val page: Color,
    val hero: Color,
    val ink: Color
)

/** The label of a stored color-theme id (for the row's subtitle). */
private fun colorThemeLabel(id: String): String = when (id) {
    AppPreferences.COLOR_THEME_CURIO -> "Curio rose"
    AppPreferences.COLOR_THEME_AZURE -> "Azure hero"
    AppPreferences.COLOR_THEME_MATERIAL -> "Material"
    AppPreferences.COLOR_THEME_LANE -> "Adaptive Hero"
    AppPreferences.COLOR_THEME_JADE -> "Jade"
    AppPreferences.COLOR_THEME_ORCHID -> "Orchid"
    AppPreferences.COLOR_THEME_OCEAN -> "Ocean"
    AppPreferences.COLOR_THEME_SAND -> "Sand"
    AppPreferences.COLOR_THEME_EMBER -> "Ember"
    else -> "Curio"
}

/** The Color theme row: the name of the live theme, a three-swatch hint of
 *  its colours and the door into the sheet. */
@Composable
private fun ColorThemeRow(onClick: () -> Unit) {
    val current = AppPreferences.colorThemeState
    val accent = settingsCardAccentInk()
    val swatch = colorThemeChoices().firstOrNull { it.id == current }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),                    modifier = Modifier
                        .fillMaxWidth()
                        // v427 — the door squishes like the sheet's own rows, so
                        // the touch that opens the picker and the touch inside it
                        // are the same gesture.
                        .curioPressClickable(pressedScale = 0.972f, onClick = onClick)
                        .padding(vertical = 8.dp)
                ) {
                    SettingsOptionIconTile(CurioIcons.Palette, isCurioDarkTheme())
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Color theme",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = colorThemeLabel(current),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        swatch?.let { ColorThemeSwatch(it) }
        CurioIcon(
            CurioIcons.ChevronRight,
            null,
            tint = curioTintOn(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.70f),
            size = 18.dp
        )
    }
}

/** Three overlapping swatches — the page, the hero and the ink of one theme. */
@Composable
private fun ColorThemeSwatch(choice: ColorThemeChoice) {
    Box(modifier = Modifier.size(width = 46.dp, height = 24.dp)) {
        listOf(choice.page, choice.hero, choice.ink).forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .offset(x = (index * 11).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    }
}

/**
 * THE COLOR THEME SHEET (v411).
 *
 * One row per theme, each with its own colours shown before it is picked —
 * the member's ask ("it shows its preview with a hint of colors and each theme
 * gets a row"). Each theme previews the accent it would paint the hero with,
 * so a row's swatches are a real hint of what picking it does. Picking one
 * writes the pref (and the legacy switches in step) and closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorThemeSheet(
    onDismiss: () -> Unit,
    /** v412 — the reveal is driven by a scope that OUTLIVES the sheet: the
     *  pref write runs inside `switchVisualThemeWithReveal`'s coroutine, and
     *  a scope remembered IN HERE dies the moment `onDismiss` leaves this
     *  composable — which cancelled the write mid-flight, so picking a theme
     *  closed the sheet and changed nothing. The caller owns the scope now
     *  (it stays composed after the sheet is gone). */
    transition: CurioThemeTransitionState?,
    transitionScope: CoroutineScope,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val current = AppPreferences.colorThemeState
    val accent = settingsCardAccentInk()
    val choices = colorThemeChoices()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "Color theme",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FrauncesFontFamily
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            // v427 — ONE LINE. The sheet's own blurb was two sentences that told
            // the member what the page under it already says (member: "too many
            // texts and erm dashes").
            Text(
                "Page, hero and ink",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            choices.forEach { choice ->
                val live = choice.id == current
                // The reveal expands from the row that was tapped.
                var rowBounds by remember { mutableStateOf(Rect.Zero) }
                // v427 — a TIGHTER ROW: the sheet is a list of nine, so every
                // row gives back the air it was not using (see the padding).
                //
                // AND THE LIVE ROW CHANGES ITS MIND IN PLACE. The picked row's
                // wash used to SNAP on the instant the pref landed; it animates
                // now, so picking a theme reads as one move (the row lights, then
                // the page reveals) instead of a flicker followed by a paint.
                val rowFill by animateColorAsState(
                    targetValue = if (live) lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.14f)
                                  else Color.Transparent,
                    animationSpec = tween(220),
                    label = "color-theme-row"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { rowBounds = it.boundsInWindow() }
                        .clip(RoundedCornerShape(18.dp))
                        .background(rowFill)
                        // A deeper squish than the section rows wear: this is a
                        // picker, and the finger should feel it take the choice.
                        .curioPressClickable(pressedScale = 0.965f, onClick = {
                            if (!live) {
                                // The whole scheme repaints, so the transition is
                                // FORCED (like the Material toggle's used to be):
                                // the circular reveal plays from the tapped row.
                                val center = if (rowBounds == Rect.Zero) Offset.Zero
                                else Offset(rowBounds.center.x, rowBounds.center.y)
                                switchVisualThemeWithReveal(transition, transitionScope, center) {
                                    AppPreferences.setColorTheme(context, choice.id)
                                }
                            }
                            onDismiss()
                        })
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                ) {
                    ColorThemeSwatch(choice)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            choice.label,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            choice.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (live) {
                        CurioIcon(CurioIcons.Check, null, tint = accent, size = 18.dp)
                    }
                }
            }
        }
    }
}

/**
 * Every color theme the sheet offers, with the three colours each one paints.
 *
 * Each entry previews the accent that theme would give the shared hero (the
 * rose or azure twin, the Material container, the lane accent), so a row's
 * swatches are a real hint of what picking it does rather than a decoration.
 */
@Composable
private fun colorThemeChoices(): List<ColorThemeChoice> {
    val page = MaterialTheme.colorScheme.background
    val ink = MaterialTheme.colorScheme.onSurface
    val dark = isCurioDarkTheme()
    val lane = heroLaneCategory()
    val roseHero = remember(dark) {
        val base = toHsl(CurioColors.HomeRosewood)
        val pinkHue = (base.h - 15f + 360f) % 360f
        if (dark) fromHsl(pinkHue, 0.55f, 0.40f)
        else fromHsl(pinkHue, 0.74f, 0.82f)
    }
    return buildList {
        add(
            ColorThemeChoice(
                id = AppPreferences.COLOR_THEME_CURIO,
                label = "Curio rose",
                hint = "The brand rose hero on the app's own page",
                page = page, hero = roseHero, ink = ink
            )
        )
        add(
            ColorThemeChoice(
                id = AppPreferences.COLOR_THEME_AZURE,
                label = "Azure hero",
                hint = "The airy sky-azure hero",
                page = page, hero = CurioColors.HomeAzure, ink = ink
            )
        )
        add(
            ColorThemeChoice(
                id = AppPreferences.COLOR_THEME_MATERIAL,
                label = "Material",
                hint = "Material 3: one primary, neutral surfaces",
                page = page,
                hero = MaterialTheme.colorScheme.primaryContainer,
                ink = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        add(
            ColorThemeChoice(
                id = AppPreferences.COLOR_THEME_LANE,
                label = "Adaptive Hero",
                hint = "The hero and the page take the category you last picked on Spin",
                page = page,
                hero = lane?.headerAccent() ?: roseHero,
                ink = ink
            )
        )
        // v420 — THE NAMED THEMES. Five full themes, each previewing its own
        // page, hero and ink in the mode the app is in (see `CurioNamedTheme`).
        CurioNamedTheme.entries.forEach { named ->
            add(
                ColorThemeChoice(
                    id = named.id,
                    label = named.label,
                    hint = named.hint,
                    page = named.pageFor(dark),
                    hero = named.heroFor(dark),
                    ink = named.accentFor(dark)
                )
            )
        }
    }
}

/**
 * Preferences — the behavioral settings that aren't about how the app LOOKS
 * (Appearance) or when it REMINDS you (Notifications): which search engine
 * Explore opens, how explore sessions behave (timer / live notification /
 * floating bubble), and the pet's personality (chatter + games). v26 — the
 * rows moved here from Notifications and Appearance so Preferences is the
 * home for "how Curio behaves" choices.
 */
@Composable
private fun PreferencesSection(highlightKey: String? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var overlayEnabled by remember { mutableStateOf(AppPreferences.overlayBubbleEnabledState) }
    // v8.1 — live "Display over other apps" grant state + a flag that the
    // system special-access page was opened (so ON_RESUME knows whether a
    // grant — or a decline — just happened).
    var overlayUsable by remember { mutableStateOf(AppPreferences.overlayActuallyUsable(context)) }
    var overlaySettingsOpened by remember { mutableStateOf(false) }
    // v30 — the bubble and the overlay permission are ONE option now; this
    // flags the "Remove overlay permission" trip so the return only refreshes
    // the grant state instead of re-enabling the bubble.
    var overlayRevokeOpened by remember { mutableStateOf(false) }
    // v3xx — the "Live explore notification" experiment concluded: always
    // on, no toggle (the local state + row were removed).
    var exploreSessionsEnabled by remember { mutableStateOf(AppPreferences.exploreSessionsEnabledState) }
    // v27 — the daily shuffle reminder + its hour chips moved in from the
    // removed Notifications section. v3xx51 — the reminder now carries a
    // MINUTE too, set from a real clock picker (the chips stay as quick
    // hour presets).
    var reminderHour by remember { mutableStateOf(AppPreferences.getReminderHour(context)) }
    var reminderMinute by remember { mutableStateOf(AppPreferences.getReminderMinute(context)) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    // v440 — the journal's own daily goal, and its own nudge beside it.
    var journalGoal by remember { mutableStateOf(AppPreferences.getJournalGoal(context)) }
    var journalGoalReminder by remember {
        mutableStateOf(AppPreferences.isJournalGoalReminderEnabled(context))
    }
    // v440 — where the journal's own door lands (see `isJournalOpenToday`).
    var journalOpenToday by remember { mutableStateOf(AppPreferences.isJournalOpenToday(context)) }
    var showBubbleOptInDialogEnabled by remember { mutableStateOf(AppPreferences.showBubbleOptInDialogState) }
    // v3xx54 — messages + community notifications (default ON).
    var socialNotificationsEnabled by remember {
        mutableStateOf(AppPreferences.socialNotificationsState)
    }
    // v19 — the explore search-engine picker (which engine the "Explore in
    // browser" button opens).
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    // v27s — the explore music-service picker (which service the "Watch in"
    // button opens for albums, artists & songs).
    var showMusicServiceDialog by remember { mutableStateOf(false) }
    // v26 — shared notification-permission gate (live-notification row).
    val enableNotifications = rememberNotificationPermissionGate()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayEnabled = AppPreferences.isOverlayBubbleEnabled(context)
                overlayUsable = AppPreferences.overlayActuallyUsable(context)
                exploreSessionsEnabled = AppPreferences.isExploreSessionsEnabled(context)
                reminderHour = AppPreferences.getReminderHour(context)
                reminderMinute = AppPreferences.getReminderMinute(context)
                showBubbleOptInDialogEnabled = AppPreferences.isShowBubbleOptInDialog(context)
                socialNotificationsEnabled = AppPreferences.socialNotificationsState
                // v8.1 — returning from the system overlay-settings page: a
                // grant re-enables the bubble and clears the declined flag;
                // coming back without granting records the "no" so automatic
                // prompts stop (Settings toggles still work anytime).
                if (overlaySettingsOpened) {
                    overlaySettingsOpened = false
                    val revoking = overlayRevokeOpened
                    overlayRevokeOpened = false
                    overlayUsable = AppPreferences.overlayActuallyUsable(context)
                    if (revoking) {
                        // v30 — the "Remove overlay permission" row opened the
                        // system page: the bubble stays OFF; only the live
                        // grant state refreshes in the subtitle.
                    } else if (overlayUsable) {
                        // Grant: re-enable the bubble + clear the declined flag.
                        AppPreferences.setOverlayAskDeclined(context, false)
                        AppPreferences.setOverlayBubbleEnabled(context, true)
                        overlayEnabled = true
                    } else {
                        AppPreferences.setOverlayAskDeclined(context, true)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // The result callback can fire while the system page is STILL open (the
    // permission not yet granted), so the grant/decline decision lives in
    // the ON_RESUME observer below, guarded by [overlaySettingsOpened].
    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* no-op — ON_RESUME is the source of truth */ }
    Column(modifier = Modifier.fillMaxWidth()) {
        // v19 — which search engine the "Explore in browser" button opens.
        // A row that opens the engine picker; the subtitle shows the choice.
        SettingsRowPulse(highlightKey == "pref-search-engine") {
            SettingsOptionRow(
                CurioIcons.Search,
                "Search engine",
                "Explore in browser opens ${SearchEngine.fromId(AppPreferences.searchEngineState).displayName}"
            ) {
                showSearchEngineDialog = true
            }
        }
        SettingsOptionDivider()
        // v27s — which music service the "Watch in" explore button opens for
        // albums, artists and songs (next to the search-engine picker).
        SettingsRowPulse(highlightKey == "pref-music-service") {
            SettingsOptionRow(
                CurioIcons.MusicNote,
                "Music service",
                "Watch in opens ${MusicService.fromId(AppPreferences.musicServiceState).displayName} for albums, artists & songs"
            ) {
                showMusicServiceDialog = true
            }
        }
        SettingsOptionDivider()
        SettingsRowPulse(highlightKey == "pref-sessions") {
            CompactSwitchRow(CurioIcons.TravelExplore, "Explore sessions", "Timer, reminder, and done prompt", exploreSessionsEnabled) {
                exploreSessionsEnabled = it
                AppPreferences.setExploreSessionsEnabled(context, it)
            }
        }

        SettingsOptionDivider()
        // v30 — the floating bubble and the overlay permission are ONE
        // option (the bubble IS the overlay). Enabling without the permission
        // opens the system page to ask for it; the subtitle shows the live
        // grant state. When the bubble is OFF and the permission is still
        // granted, an inline row below offers to remove it.
        SettingsRowPulse(highlightKey == "pref-bubble") {
            CompactSwitchRow(
                CurioIcons.BubbleChart,
                "Floating explore bubble",
                if (overlayUsable) "Timer bubble over other apps · overlay permission granted"
                else "Timer bubble over other apps · needs the overlay permission",
                overlayEnabled
            ) { enabled ->
                if (enabled && !AppPreferences.overlayActuallyUsable(context)) {
                    // Enabling without the permission: ask for it — stop
                    // suppressing the prompt, open the system page, and let
                    // the ON_RESUME observer decide grant vs decline.
                    AppPreferences.setOverlayAskDeclined(context, false)
                    val launched = runCatching {
                        overlaySettingsOpened = true
                        overlaySettingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                    if (launched.isFailure) overlaySettingsOpened = false
                } else {
                    overlayEnabled = enabled
                    AppPreferences.setOverlayBubbleEnabled(context, enabled)
                }
            }
        }
        if (!overlayEnabled && overlayUsable) {
            SettingsRowPulse(highlightKey == "pref-bubble-revoke") {
                SettingsOptionRow(
                    CurioIcons.Layers,
                    "Remove overlay permission",
                    "Open system settings to revoke the floating bubble's permission"
                ) {
                    // A fresh trip with no intent to enable: only refresh the
                    // grant state on return — never re-enable the bubble.
                    AppPreferences.setOverlayAskDeclined(context, false)
                    overlayRevokeOpened = true
                    overlaySettingsOpened = true
                    val launched = runCatching {
                        overlaySettingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                    if (launched.isFailure) {
                        overlaySettingsOpened = false
                        overlayRevokeOpened = false
                    }
                }
            }
        }

        SettingsOptionDivider()
        // v27 — the daily shuffle reminder + its hour chips moved in from the
        // removed Notifications section: Preferences is now the one home for
        // notification controls.
        SettingsRowPulse(highlightKey == "pref-reminder") {
            CompactSwitchRow(CurioIcons.Notifications, "Daily shuffle reminder", if (AppPreferences.reminderEnabledState) "Every day at ${formatReminderTime(AppPreferences.getReminderHour(context), AppPreferences.getReminderMinute(context))}" else "Off", AppPreferences.reminderEnabledState) { enabled ->
                if (enabled) enableNotifications { AppPreferences.setReminderEnabled(context, true) } else AppPreferences.setReminderEnabled(context, false)
            }
        }

        // ── v440 — WHERE THE JOURNAL'S OWN DOOR LANDS ───────────────
        //
        // The member's own pick from the settings list: *"'First page of the day'
        // preference (today's page vs the last one you opened)"*. One switch rather
        // than a two-option row because there are exactly two answers and one of
        // them is the default the app has always had — and the subtitle names BOTH
        // states, so the row says where the door goes whichever way it is set.
        SettingsOptionDivider()
        SettingsRowPulse(highlightKey == "pref-journal-open") {
            CompactSwitchRow(
                CurioIcons.CalendarToday,
                "Open today's page",
                if (journalOpenToday) "Today, or a new page if the day is blank"
                else "The last page you were writing",
                journalOpenToday
            ) { enabled ->
                journalOpenToday = enabled
                AppPreferences.setJournalOpenToday(context, enabled)
            }
        }

        // ── v440 — THE JOURNAL'S OWN TWO: A GOAL, AND A NUDGE ────────
        //
        // The member's own pick from the settings list: *"Word count goal with a
        // daily reminder"*. They sit with the shuffle reminder because they are the
        // same KIND of setting (a number, and a time to be reminded of it) — and
        // the goal leads, because the nudge is meaningless without one: switching
        // the goal off disarms the nudge with it, and the switch's own subtitle
        // says so rather than leaving a toggle that appears to work.
        SettingsOptionDivider()
        SettingsRowPulse(highlightKey == "pref-journal-goal") {
            CompactSwitchRow(
                CurioIcons.Note,
                "Writing goal",
                if (journalGoal > 0) "$journalGoal words a day" else "Off",
                journalGoal > 0
            ) { enabled ->
                // Turning it on has to choose a number: 300 is a page of writing
                // on a phone, which is the unit the member already thinks in (see
                // the length filters in the journals list).
                val next = if (enabled) DEFAULT_JOURNAL_GOAL else 0
                journalGoal = next
                AppPreferences.setJournalGoal(context, next)
                if (next == 0 && journalGoalReminder) {
                    journalGoalReminder = false
                    AppPreferences.setJournalGoalReminderEnabled(context, false)
                }
            }
        }
        if (journalGoal > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                JOURNAL_GOAL_CHOICES.forEach { words ->
                    val live = journalGoal == words
                    Surface(
                        onClick = {
                            journalGoal = words
                            AppPreferences.setJournalGoal(context, words)
                            if (journalGoalReminder) {
                                val (hour, minute) = AppPreferences.getJournalGoalTime()
                                com.curio.app.data.JournalGoalReminderScheduler
                                    .schedule(context, hour, minute)
                            }
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        color = if (live) settingsRoseAccent()
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (live) Color.White else settingsRoseAccent()
                    ) {
                        Text(
                            "$words",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            SettingsRowPulse(highlightKey == "pref-journal-goal-nudge") {
                CompactSwitchRow(
                    CurioIcons.Schedule,
                    "Writing goal reminder",
                    if (journalGoalReminder) {
                        val (hour, minute) = AppPreferences.getJournalGoalTime()
                        "Every day at ${formatReminderTime(hour, minute)}"
                    } else "Off",
                    journalGoalReminder
                ) { enabled ->
                    // The preference is written WITH the permission, never before
                    // it: a nudge whose alarm was armed by a member who then
                    // declined notifications would be a setting that cannot work
                    // (the same rule the shuffle reminder follows).
                    if (enabled) {
                        enableNotifications {
                            journalGoalReminder = true
                            AppPreferences.setJournalGoalReminderEnabled(context, true)
                        }
                    } else {
                        journalGoalReminder = false
                        AppPreferences.setJournalGoalReminderEnabled(context, false)
                    }
                }
            }
        }
        if (AppPreferences.reminderEnabledState) {
            // v3xx51 — the hour presets stay as one-tap picks (they snap the
            // minute back to the top of the hour), followed by the CLOCK chip:
            // it shows the reminder's real time and opens a proper time
            // picker, so any hour AND minute can be chosen.
            val presetHours = listOf(9, 12, 15, 18, 21)
            val customTime = reminderMinute != 0 || reminderHour !in presetHours
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                // v3xx58 — the CLOCK LEADS (user request: it used to sit last,
                // after the hour presets). It is the one control that can pick
                // ANY time, so it opens the row — and it wears the screen's
                // rose accent instead of the presets' plain fill, so it reads
                // as a different kind of choice at a glance.
                item {
                    val rose = settingsRoseAccent()
                    Surface(
                        onClick = { showReminderTimePicker = true },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        // v389 — when the time is a preset the chip sits between the
                        // one-tap hour pills and the clock picker; it was 16% alpha
                        // rose ("transparent") until a custom time was set, so it read
                        // as a placeholder rather than a solid door (user request).
                        color = if (customTime) rose else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (customTime) Color.White else rose,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            CurioIcon(
                                name = CurioIcons.Schedule,
                                contentDescription = null,
                                tint = if (customTime) Color.White else rose,
                                size = 15.dp
                            )
                            Text(
                                formatReminderTime(reminderHour, reminderMinute),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                items(presetHours) { hour ->
                    val selected = hour == reminderHour && reminderMinute == 0
                    // AMOLED: the selected chip swaps to pitch-black glass
                    // (white text + hairline rim) to match the switches and
                    // the app's AMOLED control language.
                    // v78 — light only (the AMOLED pitch-black chip is gone
                    // with dark mode).
                    Surface(
                        onClick = {
                            reminderHour = hour
                            reminderMinute = 0
                            AppPreferences.setReminderHour(context, hour)
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        // v27q — flat 2dp: selection reads through the solid
                        // primary/black fill, not a raise.
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            formatHour(hour),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
        SettingsOptionDivider()
        // v23 — the explore dialog's bubble opt-in row is hidden by default;
        // this re-shows it there as a single-line choice (no subtext).
        SettingsRowPulse(highlightKey == "pref-bubble-dialog") {
            CompactSwitchRow(
                CurioIcons.BubbleChart,
                "Explore bubble option in Explore dialog",
                "Show the bubble choice as one line when you start an explore",
                showBubbleOptInDialogEnabled
            ) {
                showBubbleOptInDialogEnabled = it
                AppPreferences.setShowBubbleOptInDialog(context, it)
            }
        }
        SettingsOptionDivider()
        // v3xx54 — one switch for the whole social layer: a friend's message
        // and a new community post. With it off nothing else changes — sync,
        // unread badges and the wall stay exactly as they are.
        SettingsRowPulse(highlightKey == "pref-social-notify") {
            CompactSwitchRow(
                CurioIcons.Hub,
                "Messages and community",
                "Notify when a friend messages you or someone posts",
                socialNotificationsEnabled
            ) {
                socialNotificationsEnabled = it
                AppPreferences.setSocialNotifications(context, it)
            }
        }
    }
    // v3xx51 — both pickers are BOTTOM SHEETS now. The sheet runs its own
    // swipe-down close after a pick, so `onSelected` only writes the pref and
    // lets the sheet animate itself away (dismissing here would yank it out
    // of composition mid-gesture).
    if (showSearchEngineDialog) {
        SearchEngineDialog(
            current = SearchEngine.fromId(AppPreferences.searchEngineState),
            onDismiss = { showSearchEngineDialog = false },
            onSelected = { engine -> AppPreferences.setSearchEngine(context, engine) }
        )
    }
    if (showMusicServiceDialog) {
        MusicServiceDialog(
            current = MusicService.fromId(AppPreferences.musicServiceState),
            onDismiss = { showMusicServiceDialog = false },
            onSelected = { service -> AppPreferences.setMusicService(context, service) }
        )
    }
    // v3xx51 — the reminder's clock picker (any hour + minute).
    if (showReminderTimePicker) {
        ReminderTimeSheet(
            hour = reminderHour,
            minute = reminderMinute,
            onDismiss = { showReminderTimePicker = false },
            onTimeSelected = { h, m ->
                reminderHour = h
                reminderMinute = m
                AppPreferences.setReminderTime(context, h, m)
            }
        )
    }
}

/**
 * v3xx51 — the daily-reminder CLOCK picker: a settings-styled bottom sheet
 * with a real Material time picker (dial) so the nudge can sit at any minute,
 * not only the preset hours. "Set time" applies + glides the sheet away.
 */
@Composable
private fun ReminderTimeSheet(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = hour.coerceIn(0, 23),
        initialMinute = minute.coerceIn(0, 59),
        is24Hour = false
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Reminder time",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 2.dp)
            )
            Text(
                "The daily shuffle nudge fires at this exact time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
            )
            TimePicker(state = pickerState, modifier = Modifier.padding(vertical = 8.dp))
            Surface(
                onClick = {
                    onTimeSelected(pickerState.hour, pickerState.minute)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onDismiss()
                    }
                },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 20.dp)
            ) {
                Text(
                    "Set time",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp)
                )
            }
        }
    }
}

/** "6:30 PM" — the reminder's exact time (the bare-hour formatter for the
 *  preset chips stays [formatHour]). */
/**
 * v440 — WHAT A WRITING GOAL CAN BE.
 *
 * Four numbers rather than a free field: a daily writing goal is a habit, and a
 * habit is picked in the same spirit as the shuffle reminder's hour presets — one
 * tap, no keyboard, and a set that spans "a paragraph" to "a proper session"
 * (see the length filters in the journals list, which use the same three
 * hundred-word page as their unit).
 */
private val JOURNAL_GOAL_CHOICES = listOf(100, 300, 500, 1000)

/** The goal a member gets when they first switch the row on — a page of writing. */
private const val DEFAULT_JOURNAL_GOAL = 300

private fun formatReminderTime(hour: Int, minute: Int): String {
    if (minute <= 0) return formatHour(hour)
    val h = hour.coerceIn(0, 23)
    val suffix = if (h < 12) "AM" else "PM"
    val display = when (val n = h % 12) { 0 -> 12 else -> n }
    return "$display:${minute.coerceIn(0, 59).toString().padStart(2, '0')} $suffix"
}

@Composable
private fun RecordingSection(highlightKey: String? = null) {
    val context = LocalContext.current
    var quality by remember { mutableStateOf(AudioQualitySettings.get(context)) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    // v125 — the selected offline model's subtitle ("Small · English (US) ·
    // ~40 MB", or a prompt to download one). Reads the reactive state + the
    // model-version bump so it updates right after a download/delete/select
    // inside the dialog.
    AppPreferences.offlineModelVersionState
    val offlineModelId = AppPreferences.offlineModelIdState
    val offlineModel = VoskModels.byId(offlineModelId)
    // v137 — the download manager outlives the picker sheet, so the row's
    // subtitle reports a background transfer still running (with %).
    val downloadStates by VoskModelDownloads.states.collectAsState()
    val activeDownload = downloadStates.entries.firstOrNull { (_, s) ->
        s.status == VoskModelDownloads.Status.Downloading || s.status == VoskModelDownloads.Status.Paused
    }
    val offlineModelSubtitle = when {
        activeDownload != null -> {
            val m = VoskModels.byId(activeDownload.key)
            val pct = (activeDownload.value.progress * 100).toInt()
            if (m != null) "Downloading ${m.displayName} · $pct%"
            else "Downloading a model · $pct%"
        }
        offlineModel != null && VoskModels.isDownloaded(context, offlineModelId) ->
            "${offlineModel.displayName} · ${offlineModel.sizeLabel}"
        else -> "Offline model for pre-recorded voice-to-text"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsRowPulse(highlightKey == "recording-quality") {
            SettingsOptionRow(CurioIcons.Mic, "Audio quality", quality.label) {
                showQualityDialog = true
            }
        }
        SettingsOptionDivider()
        SettingsRowPulse(highlightKey == "recording-offline-model") {
            SettingsOptionRow(CurioIcons.Download, "Offline model", offlineModelSubtitle) {
                showModelDialog = true
            }
        }
    }
    if (showQualityDialog) {
        AudioQualityDialog(
            currentQuality = quality,
            onDismiss = { showQualityDialog = false },
            onSelected = {
                quality = it
                AudioQualitySettings.set(context, it)
                showQualityDialog = false
            }
        )
    }
    if (showModelDialog) {
        OfflineModelDialog(
            currentModelId = AppPreferences.offlineModelIdState,
            onDismiss = { showModelDialog = false }
        )
    }
}

@Composable
private fun DataSection(navController: NavController, highlightKey: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsRowPulse(highlightKey == "data-tools") {
            SettingsOptionRow(CurioIcons.Backup, "Open backup tools", "Export, restore, or import FieldMind data") {
                navController.navigate(CurioRoutes.SETTINGS_DATA) { launchSingleTop = true }
            }
        }
        SettingsOptionDivider()
        SettingsRowPulse(highlightKey == "data-workspace") {
            SettingsOptionInfoRow(CurioIcons.History, "Backup workspace", "Full backup tools remain in the data workspace")
        }
    }
}

/**
 * v427 — A TWO-OPTION ROW, IN C U R I O'S OWN SWITCH.
 *
 * The member: ".in stack those sizes options are not accurate" was a journal
 * report, but the settings half of the same complaint — "redesign the 2 option
 * choosing things … more compact & premium" — was the Glyph backdrop and Paper
 * rows, which were wearing Material's `SegmentedButton` (a tall outlined box that
 * reads as a stock control on a page of hand-made cards).
 *
 * A two-option row is the SAME question the theme switch asks (one of two), so
 * it answers with the same control: a soft capsule with a filled thumb that
 * SLIDES under the choice, the live label in the accent ink and the other in the
 * muted ink. The icons, the disabled states and the hint line stay the shared
 * row's business — a row that can disable an option (or is off entirely) falls
 * back to [SettingsOptionSegmentedRow], where those states already work.
 */
@Composable
private fun CompactSegmentedRow(
    icon: String? = null,
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    enabled: Boolean = true,
    disabledIndices: Set<Int> = emptySet(),
    disabledHint: String? = null,
    onSelected: (Int) -> Unit
) {
    if (!enabled || disabledIndices.isNotEmpty() || labels.size != 2 || disabledHint != null) {
        SettingsOptionSegmentedRow(icon, title, labels, selectedIndex, enabled, disabledIndices, disabledHint, onSelected = onSelected)
        return
    }
    val dark = isCurioDarkTheme()
    val accent = settingsCardAccentInk()
    val trackFill = if (dark) MaterialTheme.colorScheme.surfaceContainerHigh
                    else MaterialTheme.colorScheme.surfaceContainer
    val thumbFill = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, if (dark) 0.26f else 0.14f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            SettingsOptionIconTile(icon, dark)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(CircleShape)
                .background(trackFill)
        ) {
            val picked = selectedIndex.coerceIn(0, 1)
            val half = maxWidth / 2
            val offset by animateDpAsState(
                targetValue = half * picked,
                animationSpec = spring(dampingRatio = 0.80f, stiffness = 340f),
                label = "option-thumb"
            )
            Box(
                modifier = Modifier
                    .offset(x = offset)
                    .padding(3.dp)
                    .width(half - 6.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(thumbFill)
            )
            Row(modifier = Modifier.fillMaxSize()) {
                labels.forEachIndexed { index, label ->
                    val live = index == picked
                    val ink by animateColorAsState(
                        targetValue = if (live) accent else curioTintOn(trackFill, MaterialTheme.colorScheme.onSurfaceVariant, 0.85f),
                        animationSpec = tween(200),
                        label = "option-ink"
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(onClickLabel = label) { onSelected(index) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = ink
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSwitchRow(
    icon: String? = null,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsOptionSwitchRow(icon, title, subtitle, checked, enabled, onCheckedChange = onCheckedChange)
}

/** v242 — compact settings slider: label + live value, used by the Liquid
 *  glass tuning rows in Appearance. `value` is 0f..2f (1f = default).
 *  v292b — `maxValue`/`steps` overridable so bounded values (like indicator
 *  opacity, 0f..1f) get a correct range instead of the 200% scale. */
@Composable
private fun CompactSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    // v292b FIX — maxValue/steps must sit BEFORE onValueChange: a trailing
    // lambda only binds to the LAST parameter, so any default-valued params
    // after the callback break every existing `row(...) { }` call site.
    maxValue: Float = 2f,
    steps: Int = 7,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.coerceIn(0f, maxValue),
            onValueChange = onValueChange,
            valueRange = 0f..maxValue,
            steps = steps
        )
    }
}

/**
 * The shared POST_NOTIFICATIONS permission gate (v26 — extracted so the
 * Notifications and Preferences sections don't duplicate it).
 *
 * Returns an `enable(action)` function: if the notification permission is
 * already granted (or the OS doesn't require it, pre-Android 13) the action
 * runs immediately; otherwise the system permission dialog is requested
 * first and the action runs only on grant (a declined request drops it —
 * the switch stays off, the user can retry anytime).
 */
@Composable
private fun rememberNotificationPermissionGate(): (() -> Unit) -> Unit {
    val context = LocalContext.current
    val permissionMissing = Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    var pendingEnable by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingEnable?.invoke()
        pendingEnable = null
    }
    // Deliberately NOT remembered: a fresh lambda each recomposition reads the
    // current [permissionMissing], so a grant on return is seen immediately
    // (remembering would capture the pre-grant value).
    return { action ->
        if (!permissionMissing) action() else {
            pendingEnable = action
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * Deep-search highlight pulse — wraps the row the hub's search jumped to.
 * On entry the matched row flashes a soft primary wash that fades out over
 * ~1.4s, so the user sees exactly which setting the search found.
 */
@Composable
private fun SettingsRowPulse(
    active: Boolean,
    content: @Composable () -> Unit
) {
    val pulseAlpha = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            pulseAlpha.snapTo(0.55f)
            pulseAlpha.animateTo(0f, tween(1400, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha.value * 0.35f)
            )
            .padding(horizontal = 2.dp)
    ) {
        content()
    }
}

/**
 * v252 — LIQUID GLASS TUNING DIALOG. The three recipe sliders
 * (Reflection / Refraction / Blur) with a LIVE PREVIEW capsule above them:
 * the preview draws over a colorful collage and re-renders on every slider
 * tick using the exact same preference values the real capsules read, so
 * what you see is what the nav pill will do.
 */
@Composable
fun GlassTuningDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        CurioSettingsCard(shadowElevation = 0.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CurioCardHeader(CurioIcons.Info, "Tune liquid glass", "Drag a slider · the capsule previews it live")
                // v258 — REAL PREVIEW: an actual [liquidGlassCapsule] pill
                // you can DRAG over a colorful collage. Every slider writes
                // the same preference state the real capsules read, so the
                // pill under your finger IS how the nav pills will render.
                // v263 — REAL PREVIEW FIXED: the gradient card records into a
                // LOCAL backdrop; the draggable capsule sits as a SIBLING
                // OUTSIDE that capture (no self-sample cycle) and refracts
                // the colorful collage behind it — exactly how real in-app
                // pills behave. Removing .clip() lets the pill float beyond
                // the card bounds.
                var previewOffset by remember { mutableStateOf(Offset.Zero) }
                val dialogBackdrop = rememberLayerBackdrop()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)                       // tall enough for the pill to roam freely
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // ── Gradient card — the content the pill refracts ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .layerBackdrop(dialogBackdrop)     // records into this layer
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF7E57C2),
                                        Color(0xFFEF9A9A),
                                        Color(0xFF80DEEA),
                                        Color(0xFFFFD54F)
                                    )
                                ),
                                RoundedCornerShape(18.dp)       // clip background only, not children
                            )
                    ) {
                        Text(
                            text = "Aa Bb Cc\n123 456",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.Black.copy(alpha = 0.35f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                        Text(
                            text = "Curio",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        )
                    }
                    // ── Draggable capsule — sibling overlay, NOT clipped ──
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset(previewOffset.x.roundToInt(), previewOffset.y.roundToInt()) }
                            .size(width = 132.dp, height = 48.dp)
                            // v263 — PROPER glass: real refraction over the
                            // captured gradient + text content behind it.
                            .liquidGlassCapsule(
                                MaterialTheme.colorScheme.surfaceVariant,
                                washAlpha = 0.45f,
                                backdrop = dialogBackdrop,
                                alwaysClear = true
                            )
                            .pointerInput(Unit) {
                                detectDragGestures { change, amount ->
                                    change.consume()
                                    previewOffset += amount
                                }
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                "Preview",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                CompactSliderRow("Reflection", "Light sheen strength", AppPreferences.glassReflectionScaleState) {
                    AppPreferences.setGlassReflectionScale(context, it)
                }
                // v292 — ACTIVE INDICATOR COLOUR: what the nav bar's resting
                // active pill wears. Auto follows the theme (Material →
                // scheme primary, azure hero → azure, rose → rose); White
                // and Black are fixed picks.
                Text(
                    "Indicator colour",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "Auto" to AppPreferences.NAV_INDICATOR_AUTO,
                        "White" to AppPreferences.NAV_INDICATOR_WHITE,
                        "Black" to AppPreferences.NAV_INDICATOR_BLACK
                    ).forEach { (label, value) ->
                        val selected = AppPreferences.navIndicatorColorState == value
                        Surface(
                            onClick = { AppPreferences.setNavIndicatorColor(context, value) },
                            shape = RoundedCornerShape(50),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
                // v292b — INDICATOR OPACITY: how transparent the resting
                // active pill's frosted wash is. The touch blob is unaffected —
                // pressing always fades the fill away for the glass effect.
                CompactSliderRow(
                    "Indicator opacity",
                    "Frosted pill transparency",
                    AppPreferences.navIndicatorOpacityState,
                    maxValue = 1f,
                    steps = 4 // 0 / 25 / 50 / 75 / 100%
                ) {
                    AppPreferences.setNavIndicatorOpacity(context, it)
                }
                CompactSliderRow("Refraction", "Edge bending strength", AppPreferences.glassRefractionScaleState) {
                    AppPreferences.setGlassRefractionScale(context, it)
                }
                CompactSliderRow("Blur", "Frostiness", AppPreferences.glassBlurScaleState) {
                    AppPreferences.setGlassBlurScale(context, it)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
            }
        }
    }
}
