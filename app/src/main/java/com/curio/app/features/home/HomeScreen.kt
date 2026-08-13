package com.curio.app.features.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.BuildConfig
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioPet
import com.curio.app.data.CategoryFamily
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.PinnedTopic
import com.curio.app.data.PromoMode
import com.curio.app.data.TopicCatalog
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.SavedQuote
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.ExploreReminderScheduler
import com.curio.app.data.ExploreSession
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.StreakTracker
import com.curio.app.data.TourController
import com.curio.app.data.formatElapsed
import com.curio.app.ui.components.TornStatPaperShape
import com.curio.app.data.formatSessionShort
import com.curio.app.features.onboarding.CurioOnboardingState
import com.curio.app.infrastructure.ExploreSessionService
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToTab
import com.curio.app.features.recent.RecentFeedItem
import com.curio.app.features.recent.buildRecentFeed
import com.curio.app.ui.adaptive.WideContentMaxWidth
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioForwardArrow
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.PaperTitleLines
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.SoftTornSheetShape
import com.curio.app.ui.pet.CurioPetHome
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.curioGoldInk
import com.curio.app.ui.theme.curioRoseInk
import com.curio.app.ui.theme.curioSageInk
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.pastelAccent
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.toHsl
import com.curio.app.ui.theme.themedAccent
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import java.util.Calendar

/**
 * Home — clean, minimal, personalized.
 *
 * Layout (top to bottom), tuned for 360×800 dp:
 *   1. **Quest hero** — the detail screen's torn-banner language, extended
 *      to the very top: the solid rose-wood banner runs up BEHIND the
 *      status bar, and the menu / avatar pills overlay it. Same seeded
 *      soft tear + white under-sheet (the identical EntryDetail
 *      construction, so the tear style stays UNIFORM — no blur). Inside:
 *      the greeting (one line) with the name beneath, and a Streak ·
 *      Cabinet · Recent bar pinned just above the tear on a soft rose
 *      gradient pane (streak in fire orange). The banner itself is NOT
 *      tappable.
 *   2. **Quest block** — "TODAY'S QUEST" eyebrow + the big solid Shuffle
 *      button, sitting between the hero tear and the content below. The
 *      button picks a random category (or a random mix) and opens that
 *      deck on the Shuffle tab.
 *   3. **Currently exploring / Queued** — the live session card and any
 *      paused sessions set aside for later.
 *   4. **Saved** — bookmarked quotes + pinned topics (hidden when empty),
 *      each row tappable through to its entry / topic.
 *   5. **Recents** — explored topics, unexplored topics (tagged
 *      "Unexplored"), and the latest saved entries as solid category-
 *      tinted cards (View all → Cabinet), or a beautiful empty-state card
 *      prompting the first spin.
 *   6. **Reminder CTA** (only when reminder is OFF) — a subtle ghost-style
 *      card suggesting the user try a daily shuffle reminder, navigating to
 *      Settings.
 *
 *  The screen still hosts the `ModalNavigationDrawer` for secondary
 *  navigation (Quests, History, Manage Categories, Browse Topics, Support)
 *  — v7.89: the drawer wears the torn-rose hero family.
 */
/** The quest hero's solid body height — the torn banner. Tall enough for
 *  the greeting + the Streak · Cabinet · Recent bar (pinned just above the
 *  tear) and generous at large font scales. */
private val HomeQuestHeroHeight = 300.dp
/** Extra layout space reserved for the white sheet below the torn banner. */
private val HomeQuestSheetExtent = 24.dp
/** Scroll distance (dp) before the menu + profile pills fully pin as
 *  frosted floating pills. */
private val StickyBarThreshold = 90.dp
/** Fixed tear seed — Home's tear never re-rolls and matches the detail
 *  hero's SoftTorn construction exactly (uniform tear style). */
// v7.37 — Home's hero tears in its OWN pattern: a different fixed seed
// than before AND the bolder tear personality, so the home banner reads as
// a rougher, more hand-torn seam than the detail hero's. Fixed → never
// re-rolls.
private const val HOME_TEAR_SEED = 0xC0FEE

/** One mirrored hero watermark pair — the left glyph mirrors the right
 *  (the saved-entry hero's construction, adapted for Home). */
private data class HomeHeroPair(
    val biasX: Float,
    val biasY: Float,
    val size: Dp,
    val rotation: Float,
    val alpha: Float
)

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val displayName = remember { AppPreferences.getDisplayName(context) }
    // Saved-shelf unsave confirmation — set when the user taps the remove
    // bookmark on a saved quote row; the dialog confirms before removal.
    var pendingUnsave by remember { mutableStateOf<SavedQuote?>(null) }
    // Unpin-topic confirmation — set when the user taps unpin on a pinned
    // topic row; the dialog confirms before the pin is dropped.
    var pendingUnpin by remember { mutableStateOf<PinnedTopic?>(null) }
    val streakDays = StreakTracker.getStreak(context)
    val reminderEnabled = AppPreferences.reminderEnabledState
    // v8.8 — the pet's flower bed at Home (spec §10.3): the pet naps here
    // when the app opens and stays asleep until tapped; once awake the bed
    // sits vacant while the pet floats around the app.
    val homePetSprite: (@Composable () -> Unit)? = if (AppPreferences.petEnabledState) {
        {
            // v8.17 — the flower bed is the pet's PLAY landmark: while it
            // floats, the pet sometimes dashes back home and does a little
            // jig at its own (vacant) bed. Bounds-only, like every landmark
            // — the bed's layout never changes, it just springs a beat.
            PetLandmark(
                id = "bed",
                kind = PetLandmarks.Kind.PLAY,
                screen = "home"
            ) { m ->
                CurioPetHome(
                    petInside = !CurioPet.awake || CurioPet.atHome ||
                        !AppPreferences.floatingPetEnabledState,
                    sleeping = !CurioPet.awake,
                    homeSize = 52.dp,
                    onTap = {
                        when {
                            !CurioPet.awake -> CurioPet.wake()
                            CurioPet.atHome -> CurioPet.comeOut()
                            else -> Unit // already floating — the bed is vacant
                        }
                    },
                    contentDescription = when {
                        !CurioPet.awake -> "Curie asleep in its flower bed. Tap to wake"
                        CurioPet.atHome -> "Curie sitting in its flower bed. Tap to come out"
                        else -> "Curie's flower bed"
                    },
                    modifier = m
                )
            }
        }
    } else null
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val recentEntries by produceState<List<CurioEntry>>(initialValue = emptyList()) {
        try {
            value = CurioRepositoryHolder.repo.getAll().take(5)
        } catch (_: Exception) {
            value = emptyList()
        }
    }
    val exploredTopics = ExploreSessionStore.recentlyExploredState
    val unexploredTopics = ExploreSessionStore.recentlyUnexploredState
    val recentFeed = remember(recentEntries, exploredTopics, unexploredTopics) {
        buildRecentFeed(recentEntries, exploredTopics, unexploredTopics)
    }
    // v7.107 — promo/demo-content mode (hidden 5-tap unlock in Support):
    // while ON, the hero stats and the recents feed are replaced with
    // promotional SAMPLE data — real topics + all six capture formats, so
    // screenshots look rich. Every row stays tappable; turning the mode
    // off reverts instantly (all of this keys off the reactive state).
    val promoOn = AppPreferences.promoModeState
    val promoEntries by produceState<List<CurioEntry>>(initialValue = emptyList(), promoOn) {
        value = if (promoOn) PromoMode.demoEntries() else emptyList()
    }
    val promoFeed = remember(promoEntries, promoOn) {
        if (promoOn) {
            buildRecentFeed(promoEntries, PromoMode.demoExplored(promoEntries), emptyList())
        } else {
            emptyList()
        }
    }
    var totalSaved by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            totalSaved = CurioRepositoryHolder.repo.count()
        } catch (_: Exception) {}
    }

    val navInsets = WindowInsets.navigationBars.asPaddingValues()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawerContent(
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) { launchSingleTop = true }
                }
            )
        },
        gesturesEnabled = drawerState.isOpen || drawerState.isAnimationRunning
    ) {
        // v6.7 — Home sits on the plain theme background: the category tint
        // wash is removed from Home (other screens still tint via Settings).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Watermark backdrop — muted category glyphs behind all ──
            //    content (same treatment as the Spin page). The quest is
            //    always the wildcard Surprise now (no category chips), so
            //    the wildcard die stays highlighted.
            // Wide windows: the NavHost's full-bleed collage replaces the
            // page's own backdrop so there is ONE continuous collage.
            if (!windowWidthSizeClass().isWide) {
                CurioWatermarkBackdrop(
                    activeCat = CurioCategories.byId(CategoryId.WILDCARD)
                )
            }
            // Hoisted scroll state — the sticky top bar (menu + profile
            // pills) reads it to pop out of the hero into frosted pills.
            val homeScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(homeScroll)
            ) {
            // ── 1. Quest hero — the detail screen's torn-banner language,
            // extended to the very top: the solid rose-wood banner runs up
            // BEHIND the status bar, and the menu / avatar pills overlay it
            // (added at the end of this Box, so they sit on the banner).
            // Same seeded SOFT tear (SoftTornBottomShape) + white under-
            // sheet (SoftTornSheetShape — same seed → aligned pixel-
            // perfect): the identical EntryDetail construction, so the tear
            // style stays UNIFORM across the app. No blur on the banner:
            // flat color + a real torn seam. Fixed seed → never re-rolls.
            // Inside: the greeting (one line) + name beneath, and the
            // Streak · Cabinet · Recent bar pinned just above the tear on a
            // soft rose gradient pane. The banner itself is NOT tappable —
            // the Shuffle deck CTA lives below the hero.
            // v7.37 — bold = the rougher Home tear personality (deeper,
            // toothier seam); the under-sheet passes the SAME flag so both
            // edges stay pixel-aligned.
            val heroTornShape = remember(HOME_TEAR_SEED) { SoftTornBottomShape(HOME_TEAR_SEED, bold = true) }
            val sheetShape = remember(HOME_TEAR_SEED) {
                SoftTornSheetShape(HOME_TEAR_SEED, lip = 10.dp, baseline = 14.dp, bold = true)
            }
            // The quest is always the wildcard Surprise now (the category
            // chip row is gone). The banner wears the muted rose-wood hero
            // accent — in pastel mode (the shipped default) it resolves to
            // the airy rose-wood pastel twin, otherwise the calm base.
            val heroFill = homeRoseAccent()
            // Use the actual pastel fill as the ink source too, so the
            // cleaner pink-rose hue carries through the greeting, stat icons
            // and hero watermark instead of falling back to a brown raw accent.
            val questInk = homeReadableInk(heroFill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HomeQuestHeroHeight + HomeQuestSheetExtent)
            ) {
                // ── White under-sheet — same as the detail hero's: the
                // sheet's torn top hides behind the opaque banner while its
                // uneven lip reads white below the tear, and the page wash
                // starts right after it.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .offset(y = HomeQuestHeroHeight - 18.dp)
                        .clip(sheetShape)
                        .background(Color(0xFFFDFCF9))
                )
                // ── Torn-edge shadow — a hairline dark rim just below the
                // hero's torn seam (the SAME seeded torn shape, nudged down
                // ~1dp) so the tear reads as a real paper edge casting a
                // thin ~0.1 mm shadow onto the white sheet. Hidden behind
                // the opaque banner everywhere except the sliver under the
                // tear; through the up-bites the rim hugs the bite's bottom
                // edge while the white still reads above it.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HomeQuestHeroHeight)
                        .offset(y = 1.dp)
                        .clip(heroTornShape)
                        .background(Color.Black.copy(alpha = 0.20f))
                )
                // ── Solid rose-wood banner, torn bottom edge. The banner is
                // NOT tappable — only the Shuffle button below the hero
                // drives the deck.
                Surface(
                    shape = heroTornShape,
                    color = heroFill,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HomeQuestHeroHeight)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // v27 — experimental paper accents (OFF by default;
                        // toggle in Settings → Experiments → Paper & headers).
                        if (AppPreferences.paperHeaderCutsState || AppPreferences.paperHeaderHolesState) {
                        }
                        // v7.33 — detail-style mirrored watermark collage: the
                        // quest family's symbols (casino, star, sparkle, …)
                        // scatter around the banner edges in mirrored pairs —
                        // the EXACT construction of the saved-entry hero, so
                        // Home and Detail read as one torn-banner family. The
                        // ink is the banner's own readable ink at a soft alpha
                        // (the old fixed category glyphs wore dark category
                        // inks that read muddy against the rose banner).
                        val heroSymbols = CurioIcons.heroWatermarkSymbols(CategoryFamily.WILDCARD)
                        val heroPairs = listOf(
                            HomeHeroPair(biasX = 0.93f, biasY = -0.85f, size = 44.dp, rotation = 12f, alpha = 0.11f),
                            HomeHeroPair(biasX = 0.55f, biasY = -0.64f, size = 48.dp, rotation = 8f, alpha = 0.13f),
                            HomeHeroPair(biasX = 0.94f, biasY = -0.12f, size = 56.dp, rotation = 14f, alpha = 0.14f),
                            HomeHeroPair(biasX = 0.56f, biasY = 0.54f, size = 50.dp, rotation = 10f, alpha = 0.13f),
                            HomeHeroPair(biasX = 0.94f, biasY = 0.80f, size = 44.dp, rotation = 6f, alpha = 0.11f)
                        )
                        heroPairs.forEachIndexed { i, pair ->
                            HomeHeroSymbol(heroSymbols[i * 2], BiasAlignment(-pair.biasX, pair.biasY), pair.size, -pair.rotation, pair.alpha, questInk)
                            HomeHeroSymbol(heroSymbols[i * 2 + 1], BiasAlignment(pair.biasX, pair.biasY), pair.size, pair.rotation, pair.alpha, questInk)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(start = 20.dp, end = 20.dp, top = 64.dp, bottom = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Greeting — one line, left-aligned, with the
                            // name beneath it (the quest CTA moved below the
                            // hero). Proper hierarchy: the greeting reads as a
                            // compact kicker, and the NAME is the star —
                            // bigger and bolder than the greeting above it.
                            // v8.16 — the greeting is a CURIOUS pet landmark:
                            // the pet sometimes tiptoes over and reads it
                            // (the text itself just pulses — no layout move).
                            PetLandmark(
                                id = "greeting",
                                kind = PetLandmarks.Kind.CURIOUS,
                                screen = "home"
                            ) { m ->
                                Text(
                                    text = greetingWordForNow(),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = questInk.copy(alpha = 0.92f),
                                    textAlign = TextAlign.Start,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = m.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            // v7.105 — the hero NAME is the hero now: larger
                            // than the greeting (36sp ExtraBold vs the 24sp
                            // kicker), full-strength ink, with tall leading
                            // so the name block fills the dead space below
                            // the greeting instead of reading as a small
                            // caption. The leading is held to a FIXED ~48dp
                            // box (glyphs still scale with the system font),
                            // so the fill works at the default scale while
                            // the stat bar keeps fitting when fonts enlarge.
                            val nameFontScale = LocalDensity.current.fontScale
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 36.sp,
                                    // Fixed ~48sp leading box held against font scaling (min 44sp).
                                    // Plain Float math: TextUnit has no coerceAtLeast (it only
                                    // exposes an operator compareTo, not the Comparable bound).
                                    lineHeight = (48f / nameFontScale.coerceAtLeast(1f)).coerceAtLeast(44f).sp
                                ),
                                color = questInk,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            // v27 — experimental paper-title underline (two
                            // short lines under the name; OFF by default).
                            if (AppPreferences.paperHeaderCutsState) {
                                PaperTitleLines(ink = questInk)
                            }
                            // Flex spacer — pins the stat card to the bottom
                            // of the banner, just above the tear.
                            Spacer(Modifier.weight(1f))
                            // ── Streak · Cabinet · Recent — the detail bar's
                            // icon/value/label design, sitting just above the
                            // torn seam on a soft rose gradient pane (the
                            // banner's own color, not white frost).
                            // v27 — experimental: the same bar can wear a
                            // solid paper card instead (soft rose-cream in
                            // light, a warm rose-brown in dark) when the
                            // "Paper stat card" experiment is on.
                            val paperStatsOn = AppPreferences.paperStatCardsState
                            val paperStatBg = if (isCurioDarkTheme())
                                lerp(heroFill, Color(0xFF2A211C), 0.50f)
                            else
                                lerp(heroFill, Color(0xFFFFF6EB), 0.62f)
                            // v27h — the Topics stat always shows the TRUE
                            // catalog total: the splash warm-cache seeds the
                            // first frame, then a lightweight IO count of the
                            // JSON assets refreshes it — so the number never
                            // reads 0 just because the database/catalog hasn't
                            // finished loading, and it tracks content drops.
                            val topicsTotal by produceState(initialValue = TopicCatalog.totalTopicCount()) {
                                value = TopicJsonLoader.countCanonicalTopics()
                            }
                            // v27h — torn paper edges (separate experiment):
                            // when on, the paper card wears a real torn-paper
                            // outline — an EXTENDED tear on the top edge and
                            // sharper ragged tears on the other three — instead
                            // of the rounded card.
                            val tearOn = paperStatsOn && AppPreferences.paperStatTearState
                            val statShape: Shape = remember(tearOn) {
                                if (tearOn) TornStatPaperShape(0x5A7E4D) else RoundedCornerShape(20.dp)
                            }
                            // v27 — the paper card can carry REAL punch holes
                            // (Stamped pin holes experiment): a vertical column
                            // of holes down the LEFT edge, drawn as an EvenOdd
                            // path so the holes stay transparent and the hero
                            // banner shows through.
                            val holesOn = paperStatsOn && AppPreferences.paperHeaderHolesState
                            Surface(
                                shape = statShape,
                                color = if (holesOn) Color.Transparent else if (paperStatsOn) paperStatBg else Color.Transparent,
                                shadowElevation = if (paperStatsOn) 3.dp else 0.dp
                            ) {
                                // The fill must wear the card's own shape —
                                // Surface does not clip its content, so a plain
                                // background() would bleed square corners past
                                // the torn/rounded border.
                                Box(
                                    modifier = when {
                                        holesOn -> Modifier.drawWithCache {
                                            val holeR = 5.dp.toPx()
                                            val holeX = 13.dp.toPx()
                                            // Punch through the SAME outline the
                                            // Surface wears (torn or rounded), so
                                            // the card edge and the holes read as
                                            // one piece of paper.
                                            val outline = statShape.createOutline(size, LayoutDirection.Ltr, this)
                                            val basePath = (outline as? Outline.Generic)?.path
                                            val path = Path().apply {
                                                if (basePath != null) {
                                                    addPath(basePath)
                                                } else {
                                                    addRoundRect(
                                                        RoundRect(Rect(Offset.Zero, size), CornerRadius(20.dp.toPx()))
                                                    )
                                                }
                                                repeat(3) { i ->
                                                    val cy = size.height * (i + 1) / 4f
                                                    addOval(Rect(Offset(holeX, cy), holeR), Path.Direction.Clockwise)
                                                }
                                                fillType = PathFillType.EvenOdd
                                            }
                                            onDrawBehind {
                                                drawPath(path, paperStatBg)
                                                // A faint pressed rim around each
                                                // hole — the paper edge catching light.
                                                repeat(3) { i ->
                                                    val cy = size.height * (i + 1) / 4f
                                                    drawCircle(
                                                        color = questInk.copy(alpha = 0.15f),
                                                        radius = holeR + 1.5.dp.toPx(),
                                                        center = Offset(holeX, cy),
                                                        style = Stroke(width = 1.5.dp.toPx())
                                                    )
                                                }
                                            }
                                        }
                                        paperStatsOn -> Modifier.background(paperStatBg, statShape)
                                        else -> Modifier.background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    heroFill.copy(alpha = 0.12f),
                                                    lerp(heroFill, Color.White, 0.26f).copy(alpha = 0.55f)
                                                )
                                            ),
                                            RoundedCornerShape(20.dp)
                                        )
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Icons wear the HERO ink (not the
                                        // pastel tints) so they stay visible
                                        // on the rose pane — deeper, same
                                        // family as the banner text.
                                        HeroStatSegment(
                                            glyph = "local_fire_department",
                                            value = if (promoOn) PromoMode.DEMO_STREAK.toString() else "$streakDays",
                                            label = "Streak",
                                            tint = questInk,
                                            ink = questInk,
                                            modifier = Modifier.weight(1f)
                                        )
                                        VerticalDivider(
                                            modifier = Modifier.height(34.dp),
                                            color = questInk.copy(alpha = 0.22f)
                                        )
                                        HeroStatSegment(
                                            glyph = CurioIcons.Inventory2,
                                            value = if (promoOn) PromoMode.DEMO_SAVED.toString() else "$totalSaved",
                                            label = "Cabinet",
                                            tint = questInk,
                                            ink = questInk,
                                            modifier = Modifier.weight(1f)
                                        )
                                        VerticalDivider(
                                            modifier = Modifier.height(34.dp),
                                            color = questInk.copy(alpha = 0.22f)
                                        )
                                        // v13 — the stat now shows the app's
                                        // TOTAL topic count (the catalog is
                                        // warmed during splash, so the sync
                                        // read is ready on the first frame)
                                        // instead of the recent-feed size.
                                        HeroStatSegment(
                                            glyph = CurioIcons.AutoAwesome,
                                            value = "$topicsTotal",
                                            label = "Topics",
                                            tint = questInk,
                                            ink = questInk,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // The menu + profile pills no longer live here — they moved
                // to a scroll-reactive STICKY bar outside the hero (they pop
                // out of the coral into frosted floating pills on scroll).
            }

            // Give the quest block a deliberate breathing room below the
            // hero's white sheet so the shuffle deck never feels pinned to
            // the torn edge.
            Spacer(Modifier.height(26.dp))

            // ── Quest block — below the hero tear, above the content ────
            // "TODAY'S QUEST" eyebrow (no indicator) + the big solid Shuffle
            // button. The button picks a random category — or a random mix —
            // persists it (the plain Shuffle tab is authoritative from
            // prefs) and opens the deck.
            Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        // Wide windows: keep the section in the comfortable
                        // centered column so rows never stretch into
                        // disconnected plates (phone layout untouched).
                        .widthIn(max = if (windowWidthSizeClass().isWide) WideContentMaxWidth else Dp.Infinity)
                        .align(Alignment.CenterHorizontally)
                ) {
                // v8.25 — the quest block is the tour's HOME landmark: the
                // First Journey's welcome step highlights the real
                // TODAY'S QUEST card instead of a guessed bottom zone.
                PetLandmark(
                    id = "quest",
                    kind = PetLandmarks.Kind.FUN,
                    screen = "home"
                ) { m ->
                    QuestShuffleCard(
                        accent = homeRoseAccent(),
                        pet = homePetSprite,
                        onShuffle = {
                            if (TourController.consumeTap("quest")) {
                                TourController.routeForCurrentStep()?.let { nextRoute ->
                                    navController.navigate(nextRoute) { launchSingleTop = true }
                                }
                            } else {
                                // v7.94 — shuffle only VISIBLE lanes: hidden
                                // categories (Manage Categories) never get dealt.
                                val all = CurioCategories.visible
                                val pickMix = Random.nextBoolean()
                                val chosen =
                                    if (pickMix) all.shuffled().take(2 + Random.nextInt(2))
                                    else listOf(all.random())
                                AppPreferences.setLastSpinCategories(context, chosen.map { it.id })
                                // Keep the random single/mix selection intact, but
                                // bypass the generic tab restore here. Restoring a
                                // previous Spin composition can hide this newly chosen
                                // deck and make every tap look like the same category.
                                navController.navigate(
                                    CurioRoutes.spinWithCategories(chosen.map { it.id.routeSlug })
                                ) {
                                    popUpTo(CurioRoutes.HOME) { saveState = true }
                                    // This is an explicit fresh shuffle, so even an
                                    // identical random draw must create a new deck.
                                    launchSingleTop = false
                                    restoreState = false
                                }
                            }
                        },
                        modifier = m
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // ── 2. Currently exploring — live session card ──────────────
            val activeSession = ExploreSessionStore.activeSessionState
            if (activeSession != null) {
                CurrentlyExploringCard(
                    session = activeSession,
                    onDone = {
                        // v17/v27 — hand the session's write package (elapsed
                        // time + shared note + screenshots) to the capture page
                        // before clearing (the save screen can't read it once
                        // the session is gone).
                        ExploreSessionStore.handoffWriteSession(
                            context,
                            activeSession.categoryId,
                            activeSession.topicName,
                            activeSession.elapsedMillis(),
                            note = activeSession.note,
                            screenshots = activeSession.screenshotPaths
                        )
                        ExploreSessionStore.clearSession(context)
                        ExploreReminderScheduler.cancel(context)
                        ExploreSessionService.stop(context)
                        navController.navigate(
                            CurioRoutes.captureFor(activeSession.categoryId.routeSlug, activeSession.topicName)
                        ) { launchSingleTop = true }
                    },
                    onKeepExploring = {
                        // Re-open the search page (the chosen search engine —
                        // YouTube for music) — the session keeps ticking in
                        // the background.
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activeSession.searchUrl)))
                        }
                    },
                    onStop = {
                        // Top-corner stop — quiet teardown, same as the
                        // notification's Cancel action (no write-it-down
                        // page, no done prompt on the next return).
                        ExploreSessionStore.clearSession(context)
                        ExploreReminderScheduler.cancel(context)
                        ExploreSessionService.stop(context)
                    }
                )
                Spacer(Modifier.height(20.dp))
            }

            // ── 3. Queued explores — sessions set aside for later ──────
            // When a new explore replaced the running one, the old session is
            // paused (time banked) and queued here. Tap a row to swap it back
            // into the active slot; the ✕ discards it.
            val queuedSessions = ExploreSessionStore.queuedSessionsState
            if (queuedSessions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        // Wide windows: keep the section in the comfortable
                        // centered column so rows never stretch into
                        // disconnected plates (phone layout untouched).
                        .widthIn(max = if (windowWidthSizeClass().isWide) WideContentMaxWidth else Dp.Infinity)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        "Queued explores",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        queuedSessions.forEachIndexed { index, queued ->
                            QueuedExploreRow(
                                session = queued,
                                onResume = {
                                    // Cancel the running session's reminder
                                    // (it's about to be queued), swap the
                                    // queues, then re-arm everything for the
                                    // resumed session.
                                    ExploreReminderScheduler.cancel(context)
                                    ExploreSessionStore.resumeQueuedSession(context, index)
                                    ExploreSessionStore.getActiveSession(context)?.let { resumed ->
                                        ExploreReminderScheduler.schedule(
                                            context, resumed.startMillis, resumed.durationMinutes
                                        )
                                        // Same gate as every other re-arm: the
                                        // service only runs when a notification
                                        // or the bubble wants it.
                                        if (AppPreferences.exploreServiceShouldRun(context)) {
                                            ExploreSessionService.start(context, resumed)
                                        }
                                    }
                                },
                                onDiscard = { ExploreSessionStore.removeQueued(context, index) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Spacer(Modifier.height(20.dp))

            // ── 4. Saved — bookmarked quotes + pinned topics ───────────
            val savedQuotes = AppPreferences.savedQuotesState
            val pinnedTopics = AppPreferences.pinnedTopicsState
            if (savedQuotes.isNotEmpty() || pinnedTopics.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        // Wide windows: keep the section in the comfortable
                        // centered column so rows never stretch into
                        // disconnected plates (phone layout untouched).
                        .widthIn(max = if (windowWidthSizeClass().isWide) WideContentMaxWidth else Dp.Infinity)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Saved",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        // v21 — View all opens Topic History (liked, disliked,
                        // pinned & day-grouped spins). Promo mode hides it: it
                        // would lead to the real (empty) history page.
                        if (!promoOn) {
                            Surface(
                                onClick = { navController.navigate(CurioRoutes.TOPIC_HISTORY) { launchSingleTop = true } },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        "View all",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    CurioIcon(
                                        CurioIcons.History,
                                        "Open Topic History",
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 14.dp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        savedQuotes.forEach { quote ->
                            SavedQuoteRow(
                                quote = quote,
                                onClick = {
                                    navController.navigate(CurioRoutes.entryDetail(quote.entryId)) {
                                        launchSingleTop = true
                                    }
                                },
                                onRemove = { pendingUnsave = quote }
                            )
                        }
                        pinnedTopics.forEach { pinned ->
                            PinnedTopicRow(
                                pinned = pinned,
                                onClick = {
                                    navController.navigate(
                                        CurioRoutes.revealFor(pinned.categoryId.routeSlug, pinned.topicName)
                                    ) { launchSingleTop = true }
                                },
                                onUnpin = { pendingUnpin = pinned }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 5. Recents — explored + unexplored topics and recent entries ──
            Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        // Wide windows: keep the section in the comfortable
                        // centered column so rows never stretch into
                        // disconnected plates (phone layout untouched).
                        .widthIn(max = if (windowWidthSizeClass().isWide) WideContentMaxWidth else Dp.Infinity)
                        .align(Alignment.CenterHorizontally)
                ) {
                // Promo mode swaps in the demo feed; otherwise the real one.
                val recentPreview = (if (promoOn) promoFeed else recentFeed).take(5)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recents",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    // v7.107 — promo mode hides View all: it would lead to
                    // the real (empty) Recents page, breaking the demo flow.
                    if (!promoOn && recentPreview.isNotEmpty()) {
                        Surface(
                            onClick = { navController.navigate(CurioRoutes.RECENTS_ALL) { launchSingleTop = true } },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    "View all",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                CurioForwardArrow(
                                    "Open Recents",
                                    tint = MaterialTheme.colorScheme.primary,
                                    size = 16.dp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                if (recentPreview.isEmpty()) {
                    FirstTimeEmpty(
                        surface = MaterialTheme.colorScheme.surfaceContainerLow,
                        onPickCategory = { navController.navigate(CurioRoutes.PICKER) { launchSingleTop = true } },
                        onShuffleSurprise = { navController.navigateToTab(CurioRoutes.SPIN) }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Home keeps this as a five-item preview; the full
                        // feed is available through View all → Recents.
                        recentPreview.forEach { item ->
                            when (item) {
                                is RecentFeedItem.Explored -> {
                                    val explored = item.topic
                                    ExploreTopicRow(
                                        category = CurioCategories.byId(explored.categoryId),
                                        topicName = explored.topicName,
                                        tag = if (explored.wasUnexplored) "Resumed" else null,
                                        subtitle = "Explored · tap to write about it",
                                        onClick = {
                                            navController.navigate(
                                                CurioRoutes.captureFor(explored.categoryId.routeSlug, explored.topicName)
                                            ) { launchSingleTop = true }
                                        }
                                    )
                                }
                                is RecentFeedItem.Unexplored -> {
                                    val unexplored = item.topic
                                    ExploreTopicRow(
                                        category = CurioCategories.byId(unexplored.categoryId),
                                        topicName = unexplored.topicName,
                                        tag = "Unexplored",
                                        subtitle = "Left without exploring · tap to resume",
                                        onClick = {
                                            navController.navigate(
                                                CurioRoutes.revealFor(unexplored.categoryId.routeSlug, unexplored.topicName)
                                            ) { launchSingleTop = true }
                                        }
                                    )
                                }
                                is RecentFeedItem.SavedEntry -> {
                                    RecentEntryRow(
                                        entry = item.entry,
                                        onClick = {
                                            navController.navigate(CurioRoutes.entryDetail(item.entry.id)) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Add breathing room before the bottom card / nav bar
                Spacer(Modifier.height(12.dp))
            }

            // ── 6. Reminder nudge (when reminders off) ─────────────────
            if (!reminderEnabled) {
                Spacer(Modifier.height(16.dp))
                ReminderNudgeCard(
                    surface = MaterialTheme.colorScheme.surfaceContainerLow,
                    onTap = { navController.navigate(CurioRoutes.SETTINGS) { launchSingleTop = true } }
                )
            }

            Spacer(Modifier.height(32.dp))
            Spacer(Modifier.height(navInsets.calculateBottomPadding()))
            }

            // ── Sticky top bar — menu + profile pills ─────────────────
            // Pinned OUTSIDE the scroll content so they stay on screen.
            // Resting on the hero they use a solid accent fill; as the hero
            // scrolls away they continuously fade into solid floating
            // frosted pills. The scale is tied directly to the same eased
            // progress, so there is no post-pop bounce or rotation wobble.
            val stickyThresholdPx = with(LocalDensity.current) { StickyBarThreshold.toPx() }
            val stickyProgress by remember {
                derivedStateOf { (homeScroll.value / stickyThresholdPx).coerceIn(0f, 1f) }
            }
            // One scroll-linked clock drives color, scale, lift and shadow.
            // FastOutSlowIn gives the fade a gentle start and finish while
            // keeping it perfectly scrubable with the user's finger.
            val frostShift = FastOutSlowInEasing.transform(stickyProgress)
            val pillScale = androidx.compose.ui.util.lerp(0.97f, 1f, frostShift)
            val stickyDark = isCurioDarkTheme()
            // Re-resolve the hero ink here — the original questInk lives in
            // the scroll Column's scope; the sticky bar is OUTSIDE it.
            val heroPillBg = homeRoseAccent()
            // In default light mode the old shared pastel helper returned
            // white, which made the menu/profile glyphs disappear into the
            // pale floating pill. Keep pastel and dark behavior intact, but
            // use the theme's readable dark ink for the default light state.
            val heroPillIcon = homeReadableInk(heroPillBg)
            val heroPillRim = lerp(heroPillBg, heroPillIcon, 0.42f)
            // Both morph endpoints are fully opaque. The old hero endpoint
            // used a translucent ink wash, which let the banner show through
            // the pills and made them read like circular visual artifacts.
            val frostBg = if (stickyDark) Color(0xFF23242C) else Color.White
            val frostRim = if (stickyDark) Color.White else Color(0xFFD9DEE6)
            val frostIcon = if (stickyDark) Color.White else homeReadableInk(frostBg)
            // Resolve solid target colors from scroll, then animate the paint
            // itself. The short tween gives a true color fade without adding
            // another geometric transition or ripple-like flash.
            val targetPillBg = lerp(heroPillBg, frostBg, frostShift)
            val targetPillRim = lerp(heroPillRim, frostRim, frostShift)
            val targetPillIcon = lerp(heroPillIcon, frostIcon, frostShift)
            val pillBg by animateColorAsState(
                targetValue = targetPillBg,
                animationSpec = tween(CurioMotion.Durations.Quick),
                label = "homeStickyPillBackground"
            )
            val pillRim by animateColorAsState(
                targetValue = targetPillRim,
                animationSpec = tween(CurioMotion.Durations.Quick),
                label = "homeStickyPillRim"
            )
            val pillIcon by animateColorAsState(
                targetValue = targetPillIcon,
                animationSpec = tween(CurioMotion.Durations.Quick),
                label = "homeStickyPillIcon"
            )
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
                TopBarPill(
                    onClick = { scope.launch { drawerState.open() } },
                    glyph = CurioIcons.Menu,
                    contentDescription = "Open menu",
                    shape = RoundedCornerShape(50),
                    bg = pillBg,
                    rim = pillRim,
                    iconTint = pillIcon,
                    elevation = 6.dp * frostShift
                )
                TopBarPill(
                    onClick = { navController.navigate(CurioRoutes.PROFILE) { launchSingleTop = true } },
                    glyph = CurioIcons.Person,
                    contentDescription = "Profile",
                    shape = CircleShape,
                    bg = pillBg,
                    rim = pillRim,
                    iconTint = pillIcon,
                    elevation = 6.dp * frostShift
                )
            }
        }
    }

    // ── Unsave-quote confirmation — never remove a bookmark silently ──
    pendingUnsave?.let { quote ->
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { pendingUnsave = null },
            title = { Text("Remove saved quote?") },
            text = { Text("This removes \u201C${quote.quoteText}\u201D from your Saved shelf. The entry itself stays in the Cabinet.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppPreferences.removeSavedQuote(context, quote.entryId, quote.quoteText)
                        pendingUnsave = null
                    },
                    colors = curioDialogActionButtonColors()
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnsave = null }, colors = curioDialogActionButtonColors()) { Text("Keep") }
            }
        )
    }

    // ── Unpin-topic confirmation — never drop a pin silently ──
    pendingUnpin?.let { pinned ->
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { pendingUnpin = null },
            title = { Text("Unpin ${pinned.topicName}?") },
            text = { Text("This removes ${pinned.topicName} from your Saved shelf. The topic stays in the deck. You can pin it again anytime.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppPreferences.unpinTopic(context, pinned.categoryId, pinned.topicName)
                        pendingUnpin = null
                    },
                    colors = curioDialogActionButtonColors()
                ) { Text("Unpin") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnpin = null }, colors = curioDialogActionButtonColors()) { Text("Keep") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Hero stat segment — the detail bar's icon/value/label design, on the
// home banner (Streak · Cabinet · Recent). No blur, per the home spec.
// ══════════════════════════════════════════════���════════════════════════

/** One mirrored hero watermark glyph — the banner's readable ink at a soft
 *  alpha (the saved-entry hero's HeroWatermarkGlyph role, adapted for Home:
 *  the banner ink instead of solid white). */
@Composable
private fun BoxScope.HomeHeroSymbol(
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

@Composable
private fun HeroStatSegment(
    glyph: String,
    value: String,
    label: String,
    tint: Color,
    ink: Color,
    modifier: Modifier = Modifier
) {
    // Colored icon accent, extra-bold value, soft label — mirrors
    // EntryDetail's FrostedSegment, with the icon wearing the color accent.
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        CurioIcon(
            name = glyph,
            contentDescription = null,
            tint = tint,
            size = 18.dp
        )
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
            color = ink.copy(alpha = 0.85f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Sticky top-bar pill — one circular menu / profile button for the
// scroll-linked frosted bar that pops out of the hero.
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun TopBarPill(
    onClick: () -> Unit,
    glyph: String,
    contentDescription: String,
    shape: Shape,
    bg: Color,
    rim: Color,
    iconTint: Color,
    elevation: Dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        shape = shape,
        color = bg,
        shadowElevation = elevation,
        modifier = Modifier
            .size(42.dp)
            // Material's default indication is a circular ripple. On these
            // small floating pills it expands beyond the color fade and reads
            // as a circular visual glitch, so remove the ripple and let the
            // animated colors provide the transition instead.
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CurioIcon(
                name = glyph,
                contentDescription = contentDescription,
                tint = iconTint,
                size = 22.dp,
                // The shared icon renderer already applies the standard 1dp
                // optical lift, but the menu/person glyphs' ink still reads a
                // hair low inside the small 42dp pill — nudge it up a touch
                // more (the same extra correction the casino glyph wears).
                modifier = Modifier.offset(y = (-0.5f).dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Quest block — the big solid Shuffle CTA that lives between the hero
// tear and the content below.
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun QuestShuffleCard(
    accent: Color,
    pet: (@Composable () -> Unit)? = null,
    onShuffle: () -> Unit,
    // v8.25 — the tour's home landmark modifier (bounds tracking only).
    modifier: Modifier = Modifier
) {
    // Deep ink twin for the eyebrow — the airy pastel accent reads too
    // light against the page, so the eyebrow wears the darker ink instead
    // (the button keeps the solid accent fill).
    val ink = homeReadableInk(accent)
    // v7.32 — the quest is backgroundless: bare text + the shuffle button
    // sitting on the page (no card fill, no leading icon). The whole row
    // stays tappable so a tap on the copy shuffles too.
    Surface(
        onClick = onShuffle,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // v8.5 — the pet sits at the head of the daily quest summary
            // (spec §10.3). Never intercepts taps: the row's shuffle click
            // still fires.
            pet?.let {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    it()
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TODAY'S QUEST",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp
                    ),
                    color = ink
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Shuffle the deck",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "A fresh mix of ideas, picked for you",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = CircleShape,
                color = accent,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        CurioIcons.Casino,
                        "Shuffle a random deck",
                        tint = ink,
                        size = 25.dp,
                        // The shared icon renderer already applies the
                        // standard 1dp optical lift; this extra half-dp is
                        // only for the casino glyph's heavier visible base.
                        modifier = Modifier.offset(y = (-0.5f).dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ── Saved shelf rows — bookmarked quotes + pinned topics ───────────────

@Composable
private fun SavedQuoteRow(
    quote: SavedQuote,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val cat = CurioCategories.byId(quote.categoryId)
    // Backgroundless row — the Saved shelf is a plain list now: no card
    // fill, no icon box — just a bare category glyph, the quote text and
    // the remove affordance.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CurioIcon(
            name = CurioIcons.FormatQuote,
            contentDescription = null,
            tint = cat.categoryInk(),
            size = 22.dp
        )
        Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\u201C${quote.quoteText}\u201D",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "from ${quote.topicName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                CurioIcon(
                    CurioIcons.BookmarkBorder, "Remove bookmark",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 18.dp,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
}

@Composable
private fun PinnedTopicRow(
    pinned: PinnedTopic,
    onClick: () -> Unit,
    onUnpin: () -> Unit
) {
    val cat = CurioCategories.byId(pinned.categoryId)
    // Backgroundless row — matches the plain Saved-shelf list style.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CurioIcon(
            name = CurioIcons.Bookmark,
            contentDescription = null,
            tint = cat.categoryInk(),
            size = 22.dp
        )
        Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pinned.topicName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cat.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                onClick = onUnpin,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                CurioIcon(
                    CurioIcons.BookmarkBorder, "Unpin topic",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 18.dp,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
}

// Recent entry row (compact)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun RecentEntryRow(entry: CurioEntry, onClick: () -> Unit) {
    val cat = CurioCategories.byId(entry.topic.categoryId)
    // Solid category-tinted card — matches the recents topic rows.
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = cat.categorySurface(),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CurioIcon(
                cat.iconGlyph, null, tint = cat.categoryInk(), size = 24.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.topic.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // v22 — the explore-session duration joins the meta line
                // when one was recorded ("Films · 2d ago · explored 12m").
                Text(
                    if (entry.sessionTimeMillis > 0L) {
                        "${cat.displayName} · ${entry.capturedAtDaysAgoLabel()} · explored ${formatSessionShort(entry.sessionTimeMillis)}"
                    } else {
                        "${cat.displayName} · ${entry.capturedAtDaysAgoLabel()}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            CurioForwardArrow(
                "Open capture",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

private fun CurioEntry.capturedAtDaysAgoLabel(): String = when (val d = capturedAtDaysAgo) {
    0 -> "today"
    1 -> "yesterday"
    else -> "${d}d ago"
}

// ═══════════════════════════════════════════════════════════════════════
// First-time empty state
// ═══════════════════════════════════════════════════════════════════════

/**
 * The Home accent, resolved like the hero banner: the muted rose-wood base
 * normally, its airy pastel twin when pastel mode (the shipped default) is
 * on — so the hero, empty state and drawer all wear the SAME rose-wood.
 */
@Composable
private fun homeReadableInk(fill: Color): Color = when {
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

@Composable
private fun homeRoseAccent(): Color {
    // v9.x — hero headers stay coherent with Settings: Material and AMOLED
    // use the active scheme's semantic roles instead of the legacy rose.
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_MATERIAL) {
        return MaterialTheme.colorScheme.primary
    }
    if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED) {
        return lerp(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primary, 0.16f)
    }
    // v27l — optional sky-azure hero: when enabled, the shared hero wears
    // the airy pastel azure (Science/Sky twin) instead of the rose-wood.
    if (AppPreferences.heroBlueState) {
        return if (isCurioDarkTheme()) CurioColors.HomeAzureDark
        else CurioColors.HomeAzure
    }
    val base = toHsl(CurioColors.HomeRosewood)
    return if (isCurioDarkTheme()) {
        // One shared deep companion keeps this hero family atmospheric in
        // dark mode, including when pastel mode is enabled.
        CurioColors.HomeRosewoodDark
    } else if (AppPreferences.pastelColorsState) {
        // Home keeps its own softer rose treatment: nudge the rosewood hue
        // toward pink and lift it slightly so the pastel reads clean and airy,
        // not brown or terracotta. The small saturation lift keeps the pastel
        // lively without turning it neon. Other category pastels stay unchanged.
        // v26 — about +5% more saturation so the pastel headers pop a little.
        val pinkHue = (base.h - 15f + 360f) % 360f
        fromHsl(pinkHue, ((base.s * 0.90f).coerceIn(0f, 0.80f) + 0.05f).coerceAtMost(0.85f), 0.82f)
    } else {
        // v7.36 — the base is a soft dusty rose now; lift it a touch and
        // hold saturation modestly so the non-pastel Home banner reads as a
        // beautiful calm rose instead of brownish terracotta.
        fromHsl(base.h, (base.s * 0.80f).coerceAtMost(0.40f), (base.l * 1.06f).coerceAtMost(0.70f))
    }
}

@Composable
private fun FirstTimeEmpty(
    onPickCategory: () -> Unit,
    onShuffleSurprise: () -> Unit,
    surface: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    val roseAccent = homeRoseAccent()
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = surface,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CurioIcon(
                CurioIcons.AutoAwesome, null,
                tint = roseAccent,
                size = 36.dp
            )
            Text(
                "Your journey starts here",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                "Shuffle the deck to discover your first topic. Capture what you find and it'll land here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Surface(
                    onClick = onShuffleSurprise,
                    shape = RoundedCornerShape(50),
                    color = roseAccent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurioIcon(
                            CurioIcons.Casino,
                            null,
                            tint = CurioColors.DeepPlum,
                            size = 16.dp,
                            // Match the shared icon lift plus the casino
                            // glyph's half-dp extra correction.
                            modifier = Modifier.offset(y = (-0.5f).dp)
                        )
                        Text(
                            "Surprise me",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = CurioColors.DeepPlum
                        )
                    }
                }
                Surface(
                    onClick = onPickCategory,
                    shape = RoundedCornerShape(50),
                    // v6.6 — derive from the tinted card surface so this
                    // secondary button never reads as a foreign cream pill
                    // on the tinted first-run card.
                    color = lerp(surface, MaterialTheme.colorScheme.surfaceContainerLow, 0.5f),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        "Pick a lane",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Reminder nudge card (only when reminder OFF)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ReminderNudgeCard(onTap: () -> Unit, surface: Color = MaterialTheme.colorScheme.surfaceContainerLow) {
    val fg = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(20.dp),
        color = surface,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CurioColors.ButterYellow.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        CurioIcons.Notifications, null,
                        tint = fg,
                        size = 18.dp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Try a daily shuffle reminder",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = fg
                    )
                    Text(
                        "Pick a time → we nudge you to discover",
                        style = MaterialTheme.typography.bodySmall,
                        color = fg.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                CurioForwardArrow(
                    "Open settings",
                    tint = fg.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Drawer - torn-banner family (v7.89): the rose hero wears the same seeded
// ragged tear as Home/Profile/Settings, the menu rows scroll UNDER the seam,
// and every row is flat (no card shell) with an icon chip + chevron.
// ================================================================

// v7.96 — the hero grew (168 → 186dp) so the torn banner covers a little
// more of the area below it: the menu rows start lower and more of them
// disappear under the ragged seam when scrolling.
private val HomeDrawerHeroHeight = 186.dp
private val HomeDrawerSheetExtent = 22.dp
private const val HOME_DRAWER_TEAR_SEED = 0xD2A7E

@Composable
private fun HomeDrawerContent(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val displayName = AppPreferences.getDisplayName(context)
    val heroFill = homeRoseAccent()
    val drawerInk = homeReadableInk(heroFill)
    val heroTornShape = remember(HOME_DRAWER_TEAR_SEED) {
        SoftTornBottomShape(HOME_DRAWER_TEAR_SEED, bold = true)
    }
    val sheetShape = remember(HOME_DRAWER_TEAR_SEED) {
        SoftTornSheetShape(HOME_DRAWER_TEAR_SEED, lip = 10.dp, baseline = 14.dp, bold = true)
    }
    val heroSymbols = CurioIcons.heroWatermarkSymbols(CategoryFamily.WILDCARD)
    val heroPairs = listOf(
        HomeHeroPair(biasX = 0.93f, biasY = -0.85f, size = 44.dp, rotation = 12f, alpha = 0.11f),
        HomeHeroPair(biasX = 0.55f, biasY = -0.64f, size = 48.dp, rotation = 8f, alpha = 0.13f),
        HomeHeroPair(biasX = 0.94f, biasY = -0.12f, size = 56.dp, rotation = 14f, alpha = 0.14f),
        HomeHeroPair(biasX = 0.56f, biasY = 0.34f, size = 50.dp, rotation = 10f, alpha = 0.13f),
        HomeHeroPair(biasX = 0.94f, biasY = 0.62f, size = 44.dp, rotation = 6f, alpha = 0.11f)
    )

    ModalDrawerSheet(
        modifier = Modifier.width(320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        // The hero banner tears from the very top edge — run the sheet
        // content up behind the status bar (the hero draws its own
        // top spacing, and the footer adds its own nav-bar inset).
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // -- Menu rows - drawn first so they scroll UNDER the tear ------
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = HomeDrawerHeroHeight + HomeDrawerSheetExtent + 14.dp,
                    bottom = 64.dp
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item("quests") {
                    DrawerNavItem(
                        icon = CurioIcons.WorkspacePremium,
                        label = "Quests & Levels",
                        iconTint = curioGoldInk()
                    ) { onNavigate(CurioRoutes.QUESTS) }
                }
                item("history") {
                    DrawerNavItem(
                        icon = CurioIcons.History,
                        label = "Topic History",
                        iconTint = CurioColors.DustyBlue
                    ) { onNavigate(CurioRoutes.TOPIC_HISTORY) }
                }
                item("manage") {
                    DrawerNavItem(
                        icon = CurioIcons.DragHandle,
                        label = "Manage Categories",
                        iconTint = curioSageInk()
                    ) { onNavigate(CurioRoutes.MANAGE_CATEGORIES) }
                }
                item("database") {
                    DrawerNavItem(
                        icon = CurioIcons.Database,
                        label = "Browse Topics",
                        iconTint = CurioColors.CategorySky
                    ) { onNavigate(CurioRoutes.DATABASE) }
                }
                item("support") {
                    DrawerNavItem(
                        icon = CurioIcons.SupportAgent,
                        label = "Support & diagnostics",
                        iconTint = curioRoseInk()
                    ) { onNavigate(CurioRoutes.SUPPORT) }
                }
                item("replay") {
                    DrawerNavItem(
                        icon = CurioIcons.Replay,
                        label = "Replay intro",
                        iconTint = if (isCurioDarkTheme()) {
                            CurioColors.HomeRosewoodDark
                        } else {
                            CurioColors.HomeRosewood
                        }
                    ) {
                        // Re-show the welcome screens: reset the completed
                        // flag, then open onboarding like Settings' replay.
                        CurioOnboardingState.reset(context)
                        onNavigate(CurioRoutes.ONBOARDING)
                    }
                }
            }

            // -- Torn rose hero - drawn on top, rows vanish at the seam -----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HomeDrawerHeroHeight + HomeDrawerSheetExtent)
            ) {
                // White paper under-sheet (same seed -> pixel-aligned seam).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .offset(y = HomeDrawerHeroHeight - 18.dp)
                        .clip(sheetShape)
                        .background(CurioColors.CreamWhite)
                )
                // Torn-edge shadow - hairline rim under the ragged seam.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HomeDrawerHeroHeight)
                        .offset(y = 1.dp)
                        .clip(heroTornShape)
                        .background(Color.Black.copy(alpha = 0.20f))
                )
                // Solid rose banner with the bold torn bottom edge.
                Surface(
                    shape = heroTornShape,
                    color = heroFill,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HomeDrawerHeroHeight)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Mirrored watermark collage - each pair scatters one
                        // glyph on the LEFT (-biasX, mirrored rotation) and
                        // one on the RIGHT (+biasX), exactly like the Home /
                        // Profile / Settings heroes. The old drawer code
                        // placed every glyph at +biasX (all right) and cycled
                        // pairs with i % size, so glyphs overlapped.
                        heroPairs.forEachIndexed { i, pair ->
                            CurioIcon(
                                name = heroSymbols[i * 2],
                                contentDescription = null,
                                tint = drawerInk.copy(alpha = pair.alpha),
                                size = pair.size,
                                modifier = Modifier
                                    .align(BiasAlignment(-pair.biasX, pair.biasY))
                                    .padding(10.dp)
                                    .graphicsLayer { rotationZ = -pair.rotation }
                            )
                            CurioIcon(
                                name = heroSymbols[i * 2 + 1],
                                contentDescription = null,
                                tint = drawerInk.copy(alpha = pair.alpha),
                                size = pair.size,
                                modifier = Modifier
                                    .align(BiasAlignment(pair.biasX, pair.biasY))
                                    .padding(10.dp)
                                    .graphicsLayer { rotationZ = pair.rotation }
                            )
                        }
                        // Brand + greeting pinned just above the tear.
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp)
                        ) {
                            Text(
                                "CURIO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp
                                ),
                                color = drawerInk.copy(alpha = 0.85f)
                            )
                            Text(
                                "Hi $displayName",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = drawerInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Spin it. Explore it. Capture it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = drawerInk.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // -- Pinned footer - accurate build version + tagline -----------
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    "Made with curiosity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: String,
    label: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    // Flat row (no card shell) - icon chip + label + chevron on the page.
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.16f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        icon, null,
                        tint = iconTint,
                        size = 22.dp
                    )
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            CurioIcon(
                CurioIcons.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                size = 20.dp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Greeting helpers
// ═══════════════════════════════════════════════════════════════════════

private fun greetingWordForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Welcome back"
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Explore-session topic row (recently explored / recently unexplored)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ExploreTopicRow(
    category: CurioCategory,
    topicName: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String? = null
) {
    val accent = category.themedAccent()
    // Solid category-tinted card — the recents topics wear a solid
    // background in their category's color family (matching the gradient
    // identity), instead of a backgroundless row.
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = category.categorySurface(),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CurioIcon(category.iconGlyph, null, tint = category.categoryInk(), size = 24.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        topicName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (tag != null) {
                        // Small accent pill — signals a topic the user left
                        // unexplored earlier and came back to (resumed).
                        Surface(
                            shape = RoundedCornerShape(50),
                            // v27n — opaque tinted pill (was 14% alpha, which
                            // let the elevation shadow bleed through).
                            color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.14f),
                            // Same hairline rim as the detail page's #tag
                            // chips — the deep ink text + pastel fill alone
                            // read muddy on the tinted card (v7.32).
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = category.categoryInk(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CurioForwardArrow(
                contentDescription = subtitle,
                tint = category.categoryInk(),
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Currently exploring — live session card
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CurrentlyExploringCard(
    session: ExploreSession,
    onDone: () -> Unit,
    onKeepExploring: () -> Unit,
    onStop: () -> Unit
) {
    val accent = CurioCategories.byId(session.categoryId).themedAccent()
    val cat = CurioCategories.byId(session.categoryId)
    // Use the category's resolved deep ink for the active-session controls.
    // The pastel fill is intentionally soft; the label, timer glyph and
    // secondary action should read with a firm, darker edge against it.
    val exploreInk = cat.categoryInk()
    // Live elapsed time — pause-aware (session.elapsedMillis banks paused
    // time, so a paused session shows a frozen reading) and recomputed from
    // the persisted session start so it survives process restarts; the tick
    // cancels when the card leaves composition.
    var elapsedMillis by remember(session.startMillis) {
        mutableStateOf(session.elapsedMillis())
    }
    LaunchedEffect(session.startMillis, session.paused) {
        if (session.paused) return@LaunchedEffect
        while (true) {
            elapsedMillis = session.elapsedMillis()
            delay(1_000)
        }
    }

    // Same design language as the rest of Home: a solid category-tinted
    // card (matching the recents rows) with a faint category glyph
    // watermark echoing the hero, and a quest-style eyebrow.
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cat.categorySurface(),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box {
            // Watermark glyph — the session's category, like the hero's.
            CurioIcon(
                name = cat.iconGlyph,
                contentDescription = null,
                tint = accent.copy(alpha = 0.10f),
                size = 96.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
            )
            // ── End session — the card's top corner ─────────────────
            // The floating bubble no longer carries a Stop button; the end
            // control lives here, at the session card's top-end corner,
            // where it's reachable the moment a session starts.
            Surface(
                onClick = onStop,
                shape = CircleShape,
                // v27n — opaque tinted stop button (was 14% alpha).
                color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.14f),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.Stop,
                    contentDescription = "End explore session",
                    tint = exploreInk,
                    size = 15.dp,
                    modifier = Modifier.padding(7.dp)
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                // End padding keeps the header text clear of the corner Stop
                // button (which floats at the card's TopEnd).
                Row(
                    modifier = Modifier.padding(end = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CurioIcon(
                            CurioIcons.Timer, null,
                            tint = exploreInk,
                            size = 22.dp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "CURRENTLY EXPLORING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.4.sp
                            ),
                            color = exploreInk
                        )
                        Text(
                            session.topicName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val overRecommended = elapsedMillis >= session.durationMinutes * 60_000L
                Text(
                    when {
                        session.paused ->
                            "Paused at ${formatElapsed(elapsedMillis)}: ${session.verb.lowercase()} ${session.targetName}"
                        overRecommended ->
                            "${session.verb.lowercase()} ${session.targetName} · ${formatElapsed(elapsedMillis)} so far, past the ~${session.durationMinutes} min mark"
                        else ->
                            "${session.verb.lowercase()} ${session.targetName} · ${formatElapsed(elapsedMillis)} so far · ~${session.durationMinutes} min recommended"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (session.paused) exploreInk else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDone,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = pastelFillInk(accent)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done and write about it", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onKeepExploring,
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                        border = BorderStroke(0.dp, Color.Transparent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = exploreInk),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Keep exploring", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Queued explore row — a paused session saved for later (tap to resume)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun QueuedExploreRow(
    session: ExploreSession,
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    // Deep category ink for the icon — the pastel accent reads washed out
    // on the plain page (v7.32).
    val ink = CurioCategories.byId(session.categoryId).categoryInk()
    // Plain backgroundless row, matching the Recents / Saved list style —
    // the frozen elapsed readout comes from the session's banked pause.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onResume)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CurioIcon(CurioIcons.Schedule, null, tint = ink, size = 22.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.topicName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Paused at ${formatElapsed(session.elapsedMillis())} · tap to resume",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            onClick = onDiscard,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            CurioIcon(
                CurioIcons.Close, "Discard queued explore",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 16.dp,
                modifier = Modifier.padding(5.dp)
            )
        }
    }
}
