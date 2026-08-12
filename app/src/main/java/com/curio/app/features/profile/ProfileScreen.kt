package com.curio.app.features.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryFamily
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioQuests
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.PromoMode
import com.curio.app.data.StreakTracker
import com.curio.app.infrastructure.CurioCrashReporter
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioBadgeStrip
import com.curio.app.ui.components.CurioCardHeader
import com.curio.app.ui.components.CurioForwardArrow
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioSettingsRow
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.SoftTornSheetShape
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioGradients
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.themedAccent
import com.curio.app.ui.theme.toHsl
import kotlinx.coroutines.launch

/**
 * Profile hub — identity + stats only (v7.38 — Home torn-banner redesign).
 *
 * Personalization lives entirely in Settings (appearance, notifications,
 * categories, backup — plus the Experimental section). Profile opens with
 * the Home quest family's TORN rose banner: solid rose fill with the same *  bold soft tear and a theme-matched under-sheet, a mirrored watermark
 *  collage of your last-explored lane's symbols, and the Level · Saved ·
 *  Lanes stats pinned INSIDE the banner above the tear (no standalone strip
 *  below). Behind everything sits the shared watermark backdrop (glyphs kept
 *  below the banner). Below the hero: XP progress, quests, achievements,
 *  your lanes, a

 * single Settings entry row, and the support/diagnostics rows — flat
 * content sitting directly on the watermark background (no card shells).
 */

/** The torn banner's solid body height — tall enough for the top pills,
 *  identity row, edit pill and the stat bar pinned above the tear, with
 *  flex slack held against large font scales. */
private val ProfileHeroHeight = 372.dp
/** Extra layout space reserved for the under-sheet below the torn banner. */
private val ProfileHeroSheetExtent = 24.dp
/** Total hero footprint — the torn banner plus its under-sheet extent. */
private val ProfileHeroTotalHeight = ProfileHeroHeight + ProfileHeroSheetExtent
/** Fixed tear seed — Profile tears in the SAME bold pattern as Home's quest
 *  hero (same seed + personality), so both banners read as one family. */
private const val PROFILE_TEAR_SEED = 0xC0FEE
/** Scroll distance (dp) before the Back + Settings pills fully pin as
 *  frosted floating pills (Home's StickyBarThreshold, so the pop + color
 *  morph feel identical). */
private val ProfilePillThreshold = 90.dp

/** One mirrored hero watermark pair — the left glyph mirrors the right
 *  (the Home quest hero's construction, adapted for Profile). */
private data class ProfileHeroPair(
    val biasX: Float,
    val biasY: Float,
    val size: Dp,
    val rotation: Float,
    val alpha: Float
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var displayName by remember { mutableStateOf(AppPreferences.getDisplayName(context)) }
    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember(displayName) { mutableStateOf(displayName) }
    var crashCount by remember { mutableIntStateOf(0) }
    var totalSaved by remember { mutableIntStateOf(0) }
    var categoryCounts by remember { mutableStateOf<Map<CategoryId, Int>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    // Hoisted LazyList state — the pinned Back/Settings pills read it to
    // pop out of the hero and color-morph into frosted pills on scroll
    // (the Home sticky-bar construction).
    val listState = rememberLazyListState()

    // Reloads stats on composition entry (nav return) AND on ON_RESUME
    // (returning from the app switcher), so the hero, pills, and lanes
    // always match the journal.
    fun refreshStats() {
        scope.launch {
            runCatching {
                val entries = CurioRepositoryHolder.repo.getAll()
                totalSaved = entries.size
                categoryCounts = entries.groupingBy { it.topic.categoryId }.eachCount()
            }.onFailure { android.util.Log.e("ProfileScreen", "Failed to load entries", it) }
            crashCount = CurioCrashReporter.getCrashHistory(context).size
        }
    }

    LaunchedEffect(Unit) {
        refreshStats()
        // Feed the quests system — visiting Profile completes the journey quest.
        CurioQuests.onProfileVisited(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                displayName = AppPreferences.getDisplayName(context)
                refreshStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val streakDays = StreakTracker.getStreak(context)
    // v7.40 — the level tracker is now the shared XP system (quests/levels):
    // level + progress come from earned XP instead of raw saved counts.
    // v7.107 — promo/demo-content mode swaps in promotional sample values
    // (real rank math via the shared quests API, not junk numbers); turning
    // it off reverts instantly through the reactive state.
    val promoOn = AppPreferences.promoModeState
    val displayStreak = if (promoOn) PromoMode.DEMO_STREAK else streakDays
    val displaySaved = if (promoOn) PromoMode.DEMO_SAVED else totalSaved
    val displayXp = if (promoOn) PromoMode.DEMO_XP else CurioQuests.xpState
    val level = CurioQuests.levelForXp(displayXp)
    val progress = CurioQuests.xpProgress(displayXp)

    // The hero wears the Home quest family's rose torn banner — the LAST
    // explored category personalizes the page (v7.101): its family's
    // symbols scatter across the banner and its glyph leads the watermark
    // backdrop. Reads the REACTIVE explore recents so the hero changes the
    // moment you explore something; falls back to your most-saved lane,
    // then wildcard sparkles before the first explore/save.
    val lastExploredCat = ExploreSessionStore.recentlyExploredState.firstOrNull()?.categoryId
    val topLane = categoryCounts.maxByOrNull { it.value }?.key
    val heroCat = lastExploredCat ?: topLane
    val heroFamily = heroCat?.let { CategoryFamily.of(it) } ?: CategoryFamily.WILDCARD
    val backdropActiveCat = heroCat?.let { CurioCategories.byId(it) }
        ?: CurioCategories.byId(CategoryId.WILDCARD)
    val heroFill = profileRoseAccent()
    val heroInk = profileReadableInk(heroFill)

    ProfileDialogs(
        showNameDialog = showNameDialog,
        nameInput = nameInput,
        onNameInputChange = { nameInput = it },
        onDismissName = { showNameDialog = false },
        onSaveName = {
            displayName = nameInput.trim().ifBlank { "Curious Explorer" }
            AppPreferences.setDisplayName(context, displayName)
            showNameDialog = false
        }
    )

    // v7.38 — Profile joins the Home torn-banner family: the rose banner
    // tears at the bottom (same bold soft tear + theme under-sheet), wears
    // the mirrored watermark collage, and the Level · Saved · Lanes stats
    // now live INSIDE the banner above the tear — the standalone strip
    // below is gone.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Watermark backdrop — muted category glyphs behind the content
        // (the Home/Spin language). Full-page collage like Home: the glyphs
        // scatter across the WHOLE background, hiding behind the opaque hero
        // banner and the torn paper cards, showing through the gutters and
        // the tears. The active glyph is your last-explored lane (wildcard
        // sparkles before the first explore/save).
        // v7.76 — the flat content below the hero sits directly on this
        // backdrop, so the glyphs drop to a faint whisper and the rows,
        // headers and chips always read first.
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = backdropActiveCat,
                alphaScale = 0.45f
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ProfileHero(
                    name = displayName,
                    displayStreak = displayStreak,
                    level = level,
                    saved = displaySaved,
                    lanes = if (categoryCounts.isEmpty()) CurioCategories.visible.size else categoryCounts.size,
                    family = heroFamily,
                    fill = heroFill,
                    ink = heroInk,
                    onEditName = {
                        nameInput = displayName
                        showNameDialog = true
                    }
                )
            }
            // Breathing room below the torn seam (≈ Home's quest-block gap).
            item { Spacer(Modifier.height(6.dp)) }
            item {
                Box(Modifier.padding(horizontal = wideContentEdgePadding())) {
                    // Keep the whole gamification story together: XP explains
                    // the current level, the quest row opens the full journey,
                    // and the badge preview shows the immediate payoff.
                    CurioSettingsCard(border = null) {
                        ProgressAndAchievementsCard(
                            xp = displayXp,
                            progress = progress.first,
                            nextThreshold = progress.second,
                            isMaxLevel = level >= CurioQuests.maxLevel,
                            onOpenQuests = {
                                navController.navigate(CurioRoutes.QUESTS) { launchSingleTop = true }
                            }
                        )
                    }
                }
            }
            if (categoryCounts.isNotEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = wideContentEdgePadding())) {
                        CurioSettingsCard(border = null) {
                            LanesCard(
                                counts = categoryCounts,
                                onCabinet = { navController.navigate(CurioRoutes.CABINET) { launchSingleTop = true } }
                            )
                        }
                    }
                }
            }
            item {
                Box(Modifier.padding(horizontal = wideContentEdgePadding())) {
                    CurioSettingsCard(border = null) {
                        SettingsNavCard(
                            onOpenSettings = { navController.navigate(CurioRoutes.SETTINGS) { launchSingleTop = true } }
                        )
                    }
                }
            }
            item {
                Box(Modifier.padding(horizontal = wideContentEdgePadding())) {
                    CurioSettingsCard(border = null) {
                        SupportCard(
                            crashCount = crashCount,
                            onOpenSupport = { navController.navigate(CurioRoutes.SUPPORT) { launchSingleTop = true } }
                        )
                    }
                }
            }
            item { Spacer(Modifier.navigationBarsPadding().height(4.dp)) }
        }

        // Side scroll indicator — thin overlay knob, grows on touch.
        CurioVerticalScrollIndicator(
            state = listState.scrollIndicatorState,
            onScrollBy = { listState.scrollBy(it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = ProfileHeroTotalHeight + 8.dp, bottom = 16.dp)
        )

        // ── Pinned Back + Settings pills — Home's scroll-reactive sticky
        // bar, adapted for Profile: resting on the hero they wear the SOLID
        // hero-card color (opaque, like Home's pills); as the hero scrolls
        // away they POP (scale up from 0.97) and continuously color-morph
        // into solid frosted floating pills. The scale is tied directly to
        // the same eased scroll progress (no post-pop bounce), and the
        // colors are animated paint values (no ripple flash) — the exact
        // Home mechanism.
        val stickyThresholdPx = with(LocalDensity.current) { ProfilePillThreshold.toPx() }
        val stickyProgress by remember {
            derivedStateOf {
                if (listState.firstVisibleItemIndex >= 1) 1f
                else (listState.firstVisibleItemScrollOffset / stickyThresholdPx).coerceIn(0f, 1f)
            }
        }
        val frostShift = FastOutSlowInEasing.transform(stickyProgress)
        val pillScale = androidx.compose.ui.util.lerp(0.97f, 1f, frostShift)
        val stickyDark = isCurioDarkTheme()
        // Resting state = SOLID hero-card-color pills — the banner's own
        // fill at full opacity with a rim blended toward the readable ink,
        // so the back + settings pills read as part of the hero (Home's
        // exact construction); scrolled state = solid frosted pills.
        val restPillBg = heroFill
        val restPillRim = lerp(heroFill, heroInk, 0.42f)
        val frostPillBg = if (stickyDark) Color(0xFF23242C) else Color.White
        val frostPillRim = if (stickyDark) Color.White else Color(0xFFD9DEE6)
        val frostPillIcon = if (stickyDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        // Resolve solid target colors from scroll, then animate the paint.
        val targetPillBg = lerp(restPillBg, frostPillBg, frostShift)
        val targetPillRim = lerp(restPillRim, frostPillRim, frostShift)
        val targetPillIcon = lerp(heroInk, frostPillIcon, frostShift)
        val pillBg by animateColorAsState(
            targetValue = targetPillBg,
            animationSpec = tween(CurioMotion.Durations.Quick),
            label = "profilePillBackground"
        )
        val pillRim by animateColorAsState(
            targetValue = targetPillRim,
            animationSpec = tween(CurioMotion.Durations.Quick),
            label = "profilePillRim"
        )
        val pillIcon by animateColorAsState(
            targetValue = targetPillIcon,
            animationSpec = tween(CurioMotion.Durations.Quick),
            label = "profilePillIcon"
        )
        // The hairline rim rides the pills the whole way — at rest it lifts
        // the solid hero-color pill off the banner, and it eases into the
        // frost rim as the pill pops out (Home's TopBarPill always wears its
        // border).
        val pillBorder = BorderStroke(1.dp, pillRim)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .graphicsLayer {
                    scaleX = pillScale
                    scaleY = pillScale
                    // Lifts off the hero as the frost deepens (eased).
                    translationY = -2.dp.toPx() * frostShift
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CurioBackButton(
                onClick = { navController.popBackStack() },
                containerColor = pillBg,
                contentColor = pillIcon,
                border = pillBorder,
                shadowElevation = 6.dp * frostShift,
                disableRipple = true
            )
            ProfileSearchPill(
                onClick = { navController.navigate(CurioRoutes.SETTINGS) { launchSingleTop = true } },
                bg = pillBg,
                iconTint = pillIcon,
                elevation = 6.dp * frostShift,
                border = pillBorder
            )
        }
    }
}

/** The search pill on Profile's sticky bar — a rippleless circle that
 *  wears the same animated background/rim/icon as the back pill (Home's
 *  TopBarPill construction). Opens Settings, whose hub now carries a
 *  search box that filters every settings section as you type (v7.100). */
@Composable
private fun ProfileSearchPill(
    onClick: () -> Unit,
    bg: Color,
    iconTint: Color,
    elevation: Dp,
    border: BorderStroke?
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        shape = CircleShape,
        color = bg,
        border = border,
        shadowElevation = elevation,
        modifier = Modifier
            .size(42.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CurioIcon(
                name = CurioIcons.Search,
                contentDescription = "Search settings",
                tint = iconTint,
                size = 22.dp
            )
        }
    }
}

@Composable
private fun ProfileDialogs(
    showNameDialog: Boolean,
    nameInput: String,
    onNameInputChange: (String) -> Unit,
    onDismissName: () -> Unit,
    onSaveName: () -> Unit
) {
    if (showNameDialog) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = onDismissName,
            title = { Text("Display name", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This is how Curio greets you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = onNameInputChange,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onSaveName, colors = curioDialogActionButtonColors()) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = onDismissName, colors = curioDialogActionButtonColors()) { Text("Cancel") }
            }
        )
    }
}

/**
 * v7.38 — Profile's hero joins the Home torn-banner family. The solid rose
 * banner tears at the bottom with the SAME bold soft tear + theme under-
 * sheet as Home (so the tear reads as a real paper edge), wears the
 * mirrored watermark collage of your last-explored lane's symbols, and
 *  carries the identity row (avatar, name, tagline) plus the Edit, streak,
 *  and level pills. The Level · Saved · Lanes stats now live INSIDE the banner, pinned just
 *  above the torn seam on a soft rose gradient pane — the exact Home stat
 *  bar. Back + Settings are NOT rendered here anymore: they moved to the
 *  scroll-reactive sticky bar in [ProfileScreen] (Home's pop + color-morph
 *  pills); the hero keeps a spacer so its content clears the pinned pills.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProfileHero(
    name: String,
    displayStreak: Int,
    level: Int,
    saved: Int,
    lanes: Int,
    family: CategoryFamily,
    fill: Color,
    ink: Color,
    onEditName: () -> Unit
) {
    val initial = name.firstOrNull()?.uppercase().orEmpty()
    val heroTornShape = remember(PROFILE_TEAR_SEED) { SoftTornBottomShape(PROFILE_TEAR_SEED, bold = true) }
    val sheetShape = remember(PROFILE_TEAR_SEED) {
        SoftTornSheetShape(PROFILE_TEAR_SEED, lip = 10.dp, baseline = 14.dp, bold = true)
    }
    // v12 — AMOLED: the pure-black banner carries the rose accent through the
    // watermark collage (the black-glass language); content stays white.
    val symbolTint = if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED)
        CurioColors.HomeRosewood else ink
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProfileHeroTotalHeight)
    ) {
        // ── Under-sheet — the shared white paper layer. The hero can stay
        // dark in dark mode, but the paper beneath the tear remains bright.
        // AMOLED: the sheet turns a soft rose so the torn edge keeps reading
        // through the up-bites of the pure-black banner, carrying the accent
        // of the color instead of a bright white punch.
        // Same seeded torn top as the banner,
        // hidden behind it except through the up-bites.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .offset(y = ProfileHeroHeight - 18.dp)
                .clip(sheetShape)
                .background(
                    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED)
                        CurioColors.HomeRosewood.copy(alpha = 0.45f)
                    else CurioColors.CreamWhite
                )
        )
        // ── Torn-edge shadow — hairline dark rim under the seam so the
        // tear reads as a real paper edge (the Home construction).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ProfileHeroHeight)
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
                .height(ProfileHeroHeight)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mirrored watermark collage — your last-explored lane's
                // family symbols pop around the banner edges (the Home
                // quest hero's exact construction; wildcard before the
                // first save).
                val symbols = CurioIcons.heroWatermarkSymbols(family)
                val pairs = listOf(
                    ProfileHeroPair(biasX = 0.93f, biasY = -0.85f, size = 44.dp, rotation = 12f, alpha = 0.11f),
                    ProfileHeroPair(biasX = 0.55f, biasY = -0.64f, size = 48.dp, rotation = 8f, alpha = 0.13f),
                    ProfileHeroPair(biasX = 0.94f, biasY = -0.12f, size = 56.dp, rotation = 14f, alpha = 0.14f),
                    ProfileHeroPair(biasX = 0.56f, biasY = 0.54f, size = 50.dp, rotation = 10f, alpha = 0.13f),
                    ProfileHeroPair(biasX = 0.94f, biasY = 0.80f, size = 44.dp, rotation = 6f, alpha = 0.11f)
                )
                pairs.forEachIndexed { i, pair ->
                    ProfileHeroSymbol(symbols[i * 2], BiasAlignment(-pair.biasX, pair.biasY), pair.size, -pair.rotation, pair.alpha, symbolTint)
                    ProfileHeroSymbol(symbols[i * 2 + 1], BiasAlignment(pair.biasX, pair.biasY), pair.size, pair.rotation, pair.alpha, symbolTint)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)
                ) {
                    // ── Back + Settings moved to the pinned sticky bar in
                    // ProfileScreen — this spacer keeps the hero content
                    // clear of the overlaid pills (same footprint as the
                    // 42dp pills + top padding).
                    Spacer(Modifier.height(52.dp))
                    // ── Kicker — mirrors the quest card's "TODAY'S QUEST" ──
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(ink.copy(alpha = 0.60f), RoundedCornerShape(2.dp))
                        )
                        Text(
                            "YOUR PROFILE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            ),
                            color = ink.copy(alpha = 0.88f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    // ── Avatar + name + tagline ────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // v8.16 — the avatar is a FUN pet landmark: the pet
                        // sometimes dashes over and boops it (the avatar just
                        // pulses — no layout change).
                        PetLandmark(
                            id = "avatar",
                            kind = PetLandmarks.Kind.FUN,
                            screen = "profile"
                        ) { m ->
                            Box(
                                modifier = m
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(fill)
                                    .border(BorderStroke(1.dp, ink.copy(alpha = 0.30f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    initial,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ink
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                taglineForStreak(displayStreak),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ink.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // ── Edit + streak + level — compact glass pills ───────
                    // FlowRow keeps all three actions beside one another when
                    // they fit, while allowing the level pill to wrap cleanly
                    // on narrow phones instead of clipping the title.
                    // Equal-width cells keep Edit, streak, and level aligned
                    // on narrow phones. The labels are deliberately compact
                    // and ellipsized inside their own cell rather than being
                    // allowed to push the neighboring control off the edge.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileHeroAction(
                            icon = CurioIcons.Edit,
                            label = "Edit profile",
                            ink = ink,
                            onClick = onEditName,
                            modifier = Modifier.weight(1f)
                        )
                        if (displayStreak > 0) {
                            ProfileHeroAction(
                                icon = CurioIcons.LocalFire,
                                label = "$displayStreak-day streak",
                                ink = ink,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        ProfileHeroAction(
                            icon = CurioIcons.WorkspacePremium,
                            label = CurioQuests.levelTitle(level),
                            ink = ink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Flex spacer — pins the stat bar just above the tear.
                    Spacer(Modifier.weight(1f))
                    // ── Level · Saved · Lanes — the stats INSIDE the hero,
                    // pinned above the torn seam on a soft rose gradient pane.
                    // The streak remains in the action pill above.
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, ink.copy(alpha = 0.28f)),
                        shadowElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier.background(
                                Brush.verticalGradient(
                                    listOf(
                                        fill.copy(alpha = 0.12f),
                                        if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED)
                                            CurioColors.HomeRosewood.copy(alpha = 0.30f)
                                        else lerp(fill, Color.White, 0.26f).copy(alpha = 0.55f)
                                    )
                                ),
                                RoundedCornerShape(20.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileHeroStat(
                                    glyph = CurioIcons.WorkspacePremium,
                                    value = "$level",
                                    label = "Level",
                                    ink = ink,
                                    modifier = Modifier.weight(1f)
                                )
                                VerticalDivider(
                                    modifier = Modifier.height(34.dp),
                                    color = ink.copy(alpha = 0.22f)
                                )
                                ProfileHeroStat(
                                    glyph = CurioIcons.Inventory2,
                                    value = "$saved",
                                    label = "Saved",
                                    ink = ink,
                                    modifier = Modifier.weight(1f)
                                )
                                VerticalDivider(
                                    modifier = Modifier.height(34.dp),
                                    color = ink.copy(alpha = 0.22f)
                                )
                                ProfileHeroStat(
                                    glyph = CurioIcons.Palette,
                                    value = "$lanes",
                                    label = "Lanes",
                                    ink = ink,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One aligned action cell in the hero. A two-line label is intentional:
 * it keeps the full action name readable on narrow screens instead of
 * clipping the right side of a pill or making neighboring controls jump. */
@Composable
private fun ProfileHeroAction(
    icon: String,
    label: String,
    ink: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(18.dp),
        color = ink.copy(alpha = 0.18f),
        contentColor = ink,
        shadowElevation = 0.dp,
        modifier = modifier.heightIn(min = 58.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CurioIcon(icon, null, tint = ink, size = 16.dp)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** One stat segment on the hero's gradient pane — icon / value / label in
 *  the banner's readable ink (the Home stat bar's design). */
@Composable
private fun ProfileHeroStat(
    glyph: String,
    value: String,
    label: String,
    ink: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        CurioIcon(name = glyph, contentDescription = null, tint = ink, size = 18.dp)
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = ink,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ink.copy(alpha = 0.85f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            // Center the label under its centered value: fillMaxWidth on
            // its own left-aligns the text, which made Level · Saved ·
            // Lanes hug the left edge of each cell.
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** One mirrored watermark glyph on the banner — the hero's readable ink at a
 *  soft alpha (the Home quest hero's collage construction). */
@Composable
private fun BoxScope.ProfileHeroSymbol(
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
 * The Profile hero's rose-wood fill — the SAME treatment as Home's quest
 * banner (the muted rose-wood base, its airy pastel twin when pastel mode
 * is on) so Profile reads as part of the Home family.
 */
@Composable
private fun profileRoseAccent(): Color {
    // v9.x — hero headers stay coherent with Settings: Material and AMOLED
    // use the active scheme's semantic roles instead of the legacy rose.
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_MATERIAL) {
        return MaterialTheme.colorScheme.primary
    }
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) {
        // Pure black — no grey/primary tint: on OLED the banner is a true
        // black plate and the rose accent comes through the watermark
        // collage + the sticky pills instead of a tinted fill.
        return Color.Black
    }
    val base = toHsl(CurioColors.HomeRosewood)
    return if (isCurioDarkTheme()) {
        // Keep Profile's hero aligned with the shared Home/Settings dark shade.
        CurioColors.HomeRosewoodDark
    } else if (AppPreferences.pastelColorsState) {
        val pinkHue = (base.h - 15f + 360f) % 360f
        fromHsl(pinkHue, (base.s * 0.90f).coerceIn(0f, 0.80f), 0.82f)
    } else {
        fromHsl(base.h, (base.s * 0.80f).coerceAtMost(0.40f), (base.l * 1.06f).coerceAtMost(0.70f))
    }
}

/** Readable ink for content sitting on the rose banner (Home's helper). */
@Composable
private fun profileReadableInk(fill: Color): Color = when {
    // v9.x — mirror Settings' ink so Material/AMOLED hero text stays
    // readable on the scheme-driven hero fills.
    AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_MATERIAL ->
        MaterialTheme.colorScheme.onPrimary
    AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED ->
        MaterialTheme.colorScheme.onSurface
    !AppPreferences.pastelColorsState && !isCurioDarkTheme() ->
        MaterialTheme.colorScheme.onSurface
    else -> pastelFillInk(fill)
}

/**
 * One compact gamification card: XP progress, the next quest, and a small
 * achievement shelf share one width and one visual rhythm instead of three
 * stacked cards competing for attention.
 */
@Composable
private fun ProgressAndAchievementsCard(
    xp: Int,
    progress: Float,
    nextThreshold: Int,
    isMaxLevel: Boolean,
    onOpenQuests: () -> Unit
) {
    val currentQuest = CurioQuests.currentQuest()
    val allStages = CurioQuests.allStages()
    val unlocked = allStages.filter { CurioQuests.isStageDone(it) }
    val total = allStages.size
    val fraction = if (total == 0) 0f else unlocked.size.toFloat() / total

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CurioIcon(CurioIcons.WorkspacePremium, null, tint = CurioColors.ButterYellow, size = 22.dp)
            Text(
                "XP progress",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (isMaxLevel) "$xp XP" else "$xp / $nextThreshold XP",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        // v15 — AMOLED: the coral progress bar and quest plate read as a
        // weird red accent on pure black; they swap to clean white glass.
        val amoled = AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = if (amoled) MaterialTheme.colorScheme.onSurface else CurioColors.CoralBlush,
            trackColor = if (amoled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                         else CurioColors.CoralBlush.copy(alpha = 0.14f)
        )
        Text(
            text = if (isMaxLevel) "Maximum level reached. Keep exploring for more XP."
            else "${(nextThreshold - xp).coerceAtLeast(0)} XP to the next level",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            onClick = onOpenQuests,
            color = if (amoled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    else CurioColors.CoralBlush.copy(alpha = 0.09f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (amoled) Brush.verticalGradient(listOf(Color(0xFF232323), Color(0xFF141414)))
                            else Brush.verticalGradient(CurioGradients.cardGradient(CurioColors.CoralBlush))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(CurioIcons.EmojiEvents, null, tint = Color.White, size = 20.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Quests & achievements",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        currentQuest?.let { "Next: ${it.title}" }
                            ?: "Journey complete. Every badge is open",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                CurioForwardArrow(
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    size = 18.dp
                )
            }
        }
        CurioCardHeader(
            CurioIcons.EmojiEvents,
            "Achievements",
            "${unlocked.size} of $total badges"
        )
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = CurioColors.Sage,
            trackColor = CurioColors.Sage.copy(alpha = 0.14f)
        )
        // v8.27 — the PINNED badge strip: earned medals first (up to five),
        // a "+N" tile when there are more, then locked silhouettes for
        // aspiration. Tapping the strip opens the full badge shelf on the
        // Quests page (spec §4.1 — earned first, locked as silhouettes).
        CurioBadgeStrip(
            earnedLimit = 5,
            lockedPreview = 2,
            medalSize = 46.dp,
            onViewAll = onOpenQuests,
            emptyText = currentQuest?.let { "Next: ${it.title}" }
                ?: "Keep exploring to unlock your first badge."
        )
    }
}

/** Single Settings entry — Profile owns identity/stats, Settings owns every preference. */
@Composable
private fun SettingsNavCard(onOpenSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onOpenSettings,
            color = Color.Transparent,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        // Blue settings block — the cog reads distinctly from
                        // the coral quests/level chips (v7.103).
                        .background(Brush.verticalGradient(CurioGradients.cardGradient(CurioColors.DustyBlue))),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(CurioIcons.Settings, null, tint = Color.White, size = 23.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Settings & preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Text(
                        "Appearance, notifications, backup & more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CurioForwardArrow(tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), size = 18.dp)
            }
        }
    }
}

@Composable
private fun LanesCard(counts: Map<CategoryId, Int>, onCabinet: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CurioCardHeader(CurioIcons.Palette, "Your lanes", "Where you've been exploring")
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(counts.entries.sortedByDescending { it.value }.take(4)) { (categoryId, count) ->
                val category = CurioCategories.byId(categoryId)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = category.themedAccent().copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, category.themedAccent().copy(alpha = 0.20f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        CurioIcon(category.iconGlyph, null, tint = category.themedAccent(), size = 20.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(category.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        Text("$count saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = onCabinet,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CurioIcon(CurioIcons.Inventory2, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text("Open the Cabinet", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                CurioForwardArrow(size = 16.dp)
            }
        }
    }
}

@Composable
private fun SupportCard(
    crashCount: Int,
    onOpenSupport: () -> Unit
) {
    // One toggle row — the whole Support & diagnostics suite (update check,
    // release notes, bug reports, crash logs) now lives on its own page.
    Column(modifier = Modifier.fillMaxWidth()) {
        CurioSettingsRow(
            icon = CurioIcons.Info,
            title = "Support & diagnostics",
            subtitle = if (crashCount > 0) {
                "$crashCount saved crash report${if (crashCount == 1) "" else "s"} · updates, reports & help"
            } else {
                "Updates, release notes, and bug reports"
            },
            onClick = onOpenSupport
        )
    }
}

private fun taglineForStreak(streakDays: Int): String = when {
    streakDays >= 30 -> "Marathon explorer · beautifully consistent."
    streakDays >= 7 -> "A strong curiosity streak is underway."
    streakDays > 0 -> "Keep the spark going today."
    else -> "Stay curious. There is always more to find."
}

// v7.40 — level math now lives in the shared quests system (CurioQuests):
// XP-based thresholds, titles, and progress. Removed the old saved-count
// levelFor / progressTowardsNextLevel / levelTitle helpers.
