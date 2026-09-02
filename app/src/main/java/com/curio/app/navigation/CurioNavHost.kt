package com.curio.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioUpdatePrompt
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioPet
import com.curio.app.data.TourController
import com.curio.app.data.ExploreReminderScheduler
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.formatElapsed
import com.curio.app.infrastructure.ExploreSessionService
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.curio.app.features.bugreport.BugReportScreen
import com.curio.app.features.database.TopicDatabaseScreen
import com.curio.app.features.support.PromoModeScreen
import com.curio.app.features.support.SupportScreen
import com.curio.app.features.updates.UpdatesScreen
import com.curio.app.features.crash.CurioCrashScreen
import com.curio.app.features.lightbox.LightboxScreen
import com.curio.app.features.managecategories.ManageCategoriesScreen
import com.curio.app.features.onboarding.OnboardingScreen
import com.curio.app.features.profile.ProfileScreen
import com.curio.app.features.quests.QuestsScreen
import com.curio.app.features.stats.StatsScreen
import com.curio.app.features.settings.BackupToolsScreen
import com.curio.app.features.settings.BookCoverHubScreen
import com.curio.app.features.settings.ExperimentsScreen
import com.curio.app.features.settings.UserExperimentsScreen
import com.curio.app.features.settings.SettingsHubScreen
import com.curio.app.features.settings.SettingsPage
import com.curio.app.features.settings.SettingsSectionScreen
import com.curio.app.features.settings.ShareHubScreen
import com.curio.app.features.topichistory.TopicHistoryScreen
import com.curio.app.features.recent.RecentScreen
import com.curio.app.features.recyclebin.RecycleBinScreen
import com.curio.app.features.cabinet.CabinetScreen
import com.curio.app.features.capture.SaveCaptureScreen
import com.curio.app.features.detail.EntryDetailScreen
import com.curio.app.features.outfits.OutfitShopScreen
import com.curio.app.features.petdesigner.PetDesignerScreen
import com.curio.app.features.picker.CategoryPickerBrowseScreen
import com.curio.app.features.picker.CategoryPickerScreen
import com.curio.app.features.reveal.TopicRevealScreen
import com.curio.app.features.spin.SpinScreen
import com.curio.app.features.home.HomeDrawerContent
import com.curio.app.features.home.HomeScreen
import com.curio.app.features.splash.SplashScreen
import com.curio.app.features.fieldmind.FieldMindObservationScreen
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.adaptive.LocalRevealSharedScope
import com.curio.app.ui.adaptive.LocalRevealVisibilityScope
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioDrawerState
import com.curio.app.ui.components.CurioFloatingNavBar
import com.curio.app.ui.components.CurioGlassPills
import com.curio.app.ui.components.FloatingNavCollapseHoldMillis
import com.curio.app.ui.components.curioFloatingNavContainer
import com.curio.app.ui.components.curioGlassCaptureDraw
import com.curio.app.ui.components.CurioNavigationRail
import com.curio.app.ui.components.isLiquidGlassPillsActive
import com.curio.app.ui.components.liquidglass.CurioLegacyBlur
import com.curio.app.ui.components.liquidglass.CurioLegacyBlurSnapshotter
import com.curio.app.ui.components.liquidglass.curioLegacyCapture
import com.curio.app.ui.components.liquidglass.curioLegacyCaptureGeometry
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.pet.CurioFloatingPet
import com.curio.app.ui.pet.PetPointer
import com.curio.app.ui.theme.CurioMotion

/**
 * Decodes a nav-argument string safely — malformed percent-escapes or
 * unpaired surrogates fall back to the raw value instead of crashing
 * with IllegalArgumentException.
 */
private fun safeDecode(raw: String?): String =
    runCatching { Uri.decode(raw.orEmpty()) }.getOrDefault(raw.orEmpty())

/**
 * True when a navigation is a bottom-nav TAB switch — both the screen being
 * left and the screen being shown are tab routes. Tab switches crossfade
 * (no directional slide): the tabs are peer screens that restore saved
 * state, and sliding them (worse, with the old underdamped spring) read as
 * the page-switch glitch.
 */
private fun isRevealRoute(entry: NavBackStackEntry): Boolean =
    entry.destination.route == CurioRoutes.REVEAL

/** v142 — the Pet Designer opens with the reveal's clean fade (see below). */
private fun isPetDesignerRoute(entry: NavBackStackEntry): Boolean =
    entry.destination.route?.substringBefore("/") == CurioRoutes.PET_DESIGNER

/** True when the route is the saved-entry detail page (any entry id). */
private fun isDetailRoute(entry: NavBackStackEntry): Boolean =
    entry.destination.route?.substringBefore("/") == CurioRoutes.ENTRY_DETAIL.substringBefore("/")

/**
 * True when the destination is the Topic Reveal page opened from the Browse
 * Topics database (browse=1). Browse-mode reveals are read-only and are
 * pushed from the topic browser — they are NOT part of the Spin morph flow,
 * so the bottom navigation bar stays hidden on them.
 */
private fun isBrowseRevealRoute(entry: NavBackStackEntry?): Boolean =
    entry?.destination?.route == CurioRoutes.REVEAL &&
        entry.arguments?.getString("browse") == "1"

/**
 * Push destinations that use the detail page's center pop-up (scale + fade)
 * instead of the generic horizontal slide — v8.4x: Save/Capture (+ its edit
 * routes), Profile, Quests, Settings (hub + every section), Pet Designer,
 * Topic History, Manage Categories, Recents, Support/Bug Report, the
 * Topic Database, and the Updates page. Lightbox, Category Picker, Reveal,
 * and the boot gates keep their own treatments. Values are route PREFIXES
 * (substringBefore("/")) so parameterised routes like capture/{...}, the
 * edit-* family, and settings sub-pages all match by prefix.
 */
private val popScreenRoutePrefixes: Set<String> = setOf(
    CurioRoutes.CAPTURE.substringBefore("/"),
    CurioRoutes.EDIT_MOODBOARD.substringBefore("/"),
    CurioRoutes.EDIT_ENTRY.substringBefore("/"),
    CurioRoutes.PROFILE,
    CurioRoutes.QUESTS,
    CurioRoutes.SETTINGS,
    CurioRoutes.EXPERIMENTS,
    CurioRoutes.PET_DESIGNER,
    CurioRoutes.OUTFIT_SHOP,
    CurioRoutes.TOPIC_HISTORY,
    CurioRoutes.MANAGE_CATEGORIES,
    CurioRoutes.RECENTS_ALL,
    CurioRoutes.RECYCLE_BIN,
    CurioRoutes.SUPPORT,
    CurioRoutes.BUG_REPORT,
    CurioRoutes.DATABASE,
    CurioRoutes.UPDATES
)

/** True when the destination is one of the center-pop push screens. */
private fun isPopScreenRoute(entry: NavBackStackEntry): Boolean =
    entry.destination.route?.substringBefore("/") in popScreenRoutePrefixes

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
): Boolean =
    // Browse-mode Reveal is a pushed read-only page, not a tab — it never
    // crossfades like a tab switch.
    !isBrowseRevealRoute(targetState) && !isBrowseRevealRoute(initialState) &&
        initialState.destination.route?.substringBefore("/") in CurioRoutes.bottomNavRoutePrefixes &&
        targetState.destination.route?.substringBefore("/") in CurioRoutes.bottomNavRoutePrefixes

/**
 * The Curio NavHost — single-NavHost scaffold for the active app.
 *
 * All routes are flat. The bottom nav is a floating overlay bar
 * (v129 — no Scaffold slot; see [CurioFloatingNavBar]) and is
 * conditionally visible based on the current route (see
 * [CurioRoutes.bottomNavRoutes]). Topic Reveal hides the bar and floats
 * its own Like/Dislike pill over the page (see TopicRevealScreen);
 * other push destinations like Picker/Capture/Detail/Settings/Lightbox
 * omit it.
 *
 * Each tab uses the standard Compose Navigation pattern when navigated to:
 *   navigate(route) { popUpTo(startDestination) { saveState = true }; ... }
 * — see CurioBottomNav for the actual call site. This preserves each tab's
 * back stack across switches.
 *
 * Upgraded navigation transitions:
 *  - Forward navigations: slide left + fade (matched tweens)
 *  - Back navigations: slide right + fade (matched tweens)
 *  - Modal push screens (detail + Capture/Profile/Quests/Settings/Pet
 *    Designer/Topic History/Manage Categories/Recents/Support/Bug Report/
 *    Database): center pop — scale up + fade in, shrink + fade out (v8.4x)
 *  - Tab switches (bottom nav): subtle scale-fade (no directional slide)
 *  - Splash → Home / Onboarding: fade-only reveal
 * v7.17 — the old exit/pop-enter slides used underdamped springs that
 * overshot and bounced (and never matched their paired fade) — the
 * page-switch glitch. All transitions now use matched tweens, and tab
 * switches crossfade.
 */
@Composable
fun CurioNavHost(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val routePrefix = remember(currentRoute) {
        currentRoute?.substringBefore("/")
    }
    // v147 — the Home drawer lives HERE at the NavHost root so it renders
    // ABOVE the floating pill bar (which stays composed underneath — no
    // more hide-and-reappear). Home's hamburger requests it via
    // [CurioDrawerState.requestOpen]; this owns the real DrawerState and
    // keeps [CurioDrawerState.isOpen] in sync for anything reading it.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    LaunchedEffect(CurioDrawerState.openRequest) {
        if (CurioDrawerState.openRequest > 0) {
            drawerScope.launch { drawerState.open() }
        }
    }
    LaunchedEffect(drawerState.isOpen) {
        CurioDrawerState.publishOpen(drawerState.isOpen)
    }
    // ── Adaptive window layout (tablet & landscape) ────────────────────
    // Medium/Expanded windows (>= 600dp wide) move the three tabs into a
    // left-edge NavigationRail and center page content in a comfortable
    // max-width column ([CurioContentMaxWidth]) with the theme background
    // filling the gutters. Compact phones keep the bottom bar and full-width
    // content exactly as before. Always-on — no Settings toggle.
    val wide = windowWidthSizeClass().isWide

    // Topic Reveal hides the bottom navigation bar — both from the Spin
    // main card and from the topic browser, the reveal never shows the bar
    // (it floats its own Like/Dislike pill instead, see
    // TopicRevealScreen).
    val isRevealRoutePrefix = routePrefix == CurioRoutes.REVEAL.substringBefore("/")
    val showBottomBar =
        routePrefix in CurioRoutes.bottomNavRoutePrefixes && !isRevealRoutePrefix
    // v193 — the floating pill bar stays composed briefly after the route
    // leaves the tab set so the previously-selected pill COLLAPSES with the
    // same spring it expands with. The old `showBottomBar` gate unmounted
    // the bar the instant the route changed (e.g. Home → Profile), so the
    // expanded pill just vanished instead of gliding closed — user report:
    // "the home nav pill should collapse just the way it expands when i
    // back from home… it still just vanishes instead of collapse vanishing".
    // When the route is a tab again the bar remounts immediately (the pill
    // expands as before); the rail keeps the instant `showBottomBar` gate
    // (rail items never expand/collapse).
    // v194 — the hold is the collapse spring's settle time (~380ms, the
    // 240-stiffness critically-damped family), not a fixed half-second: the
    // pill glides fully closed and then the bar unmounts — no dead pause
    // with the bar sitting there (user: "it stays for too long").
    // v201 — the pill family slowed to 150 stiffness (smooth, deeper
    // collapse), so the hold extends to ~420ms — still exactly the spring's
    // settle time, so the cinch finishes and the bar unmounts with no dead
    // pause.
    // v206 — family slowed to 120 (even smoother), hold → ~460ms.
    var barVisible by remember { mutableStateOf(showBottomBar) }
    LaunchedEffect(showBottomBar) {
        if (showBottomBar) {
            barVisible = true
        } else {
            // Let the collapse spring + label retract finish before unmount.
            // v208e — the hold is [FloatingNavCollapseHoldMillis], tuned to
            // the reveal's Like/Dislike entrance (220ms slide + a hair), so
            // the bar VANISHES right as the pill lands — the pill keeps its
            // natural start time; the nav pill syncs TO it (user: "the like
            // and dislike starting time was fine i just asked you to tune the
            // navpil home one to sync properly").
            delay(FloatingNavCollapseHoldMillis)
            barVisible = false
        }
    }
    // v142 — full-bleed-bottom routes: like the tab pages and the Topic
    // Reveal, these pages paint their own backgrounds to the very bottom
    // edge and clear the gesture bar themselves — no reserved nav-bar slot
    // from the NavHost (the reveal's old 80dp band was removed in v132;
    // Manage Categories gets the same edge-to-edge treatment).
    val fullBleedBottomRoutePrefixes = setOf(
        CurioRoutes.MANAGE_CATEGORIES.substringBefore("/"),
        // v256 — the Pet Designer paints to the bottom edge too; the old
        // reserved nav-bar inset showed as a bare background STRIP behind
        // the floating studio capsule.
        CurioRoutes.PET_DESIGNER.substringBefore("/")
    )
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDoneDialog by rememberSaveable { mutableStateOf(false) }
    // v7.31 — two-step "Cancel session": the first tap flips the done-now
    // dialog into a confirm step, the second tap actually ends the explore.
    var confirmSessionCancel by rememberSaveable { mutableStateOf(false) }
    // Survives rotation so the startup prompt only fires on a truly fresh
    // process (an active session left behind by a killed app).
    var startupPromptDone by rememberSaveable { mutableStateOf(false) }
    // v226 - the done prompt shows ONCE per session: after the user
    // dismisses it (Keep exploring / back), returning to the foreground
    // must not nag again for the SAME session (keyed by startMillis). A
    // different session re-arms it naturally.
    var dialogDismissedFor by rememberSaveable { mutableStateOf(0L) }

    // Ask "are you done exploring?" whenever the app returns to the
    // foreground while an explore session is active — mid-session, after
    // the browser search, or after the app was killed in the background.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (AppPreferences.isExploreSessionsEnabled(context)) {
                    val resumed = ExploreSessionStore.getActiveSession(context)
                    // v226 - once per session (see dialogDismissedFor).
                    showDoneDialog = resumed != null &&
                        dialogDismissedFor != resumed.startMillis
                    // A background/foreground cycle must not reopen the dialog
                    // already sitting in the cancel-confirm step.
                    confirmSessionCancel = false
                    // If the user hid the bubble but no other controller
                    // exists (live notifications off) and the bubble is
                    // still enabled, bring it back on return — otherwise
                    // there'd be no visible timer controller at all.
                    if (resumed != null && resumed.pillHidden &&
                        !AppPreferences.liveNotificationsEnabledState &&
                        AppPreferences.isOverlayBubbleEnabled(context)
                    ) {
                        ExploreSessionStore.setPillHidden(context, false)
                    }
                    // Re-arm the explore service (live notification + bubble)
                    // after returning to the app — covers permissions granted
                    // mid-session, Settings toggles, and the restore above.
                    if (resumed != null && AppPreferences.exploreServiceShouldRun(context)) {
                        ExploreSessionService.start(context, resumed)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // Startup restore: the observer above is added after the activity is
    // already RESUMED on launch, so a persisted session from a killed
    // process surfaces here instead (dialog + re-armed service).
    LaunchedEffect(Unit) {
        if (!startupPromptDone) {
            startupPromptDone = true
            if (AppPreferences.isExploreSessionsEnabled(context)) {
                val session = ExploreSessionStore.getActiveSession(context)
                // v226 - once per session (see dialogDismissedFor).
                showDoneDialog = session != null &&
                    dialogDismissedFor != session.startMillis
                confirmSessionCancel = false
                if (session != null && AppPreferences.exploreServiceShouldRun(context)) {
                    ExploreSessionService.start(context, session)
                }
            }
        }
    }

    // ── Notification deep-link handoffs ────────────────────────────────
    // The "Done exploring" action stashes the topic (category slug + name)
    // via PendingEntryOpen and launches the activity; the daily-reminder tap
    // stashes a spin-deck request via PendingSpinOpen. Once this NavHost is
    // on a stable root (a bottom-nav tab), act on the pending target: the
    // entry target opens the write-it-down entry page with HOME anchored
    // beneath it (so Back returns to the app instead of exiting it), and the
    // spin target opens the Spin deck with the standard tab switch. During
    // the boot gates (splash/onboarding/crash) the effect returns WITHOUT
    // consuming; it re-runs when the splash lands on HOME (keyed on
    // currentRoute).
    LaunchedEffect(currentRoute, PendingEntryOpen.trigger, PendingSpinOpen.trigger) {
        val prefix = currentRoute?.substringBefore("/")
        // Wait for a stable root: null (first frame) and the boot gates own
        // navigation until the splash lands on HOME — the effect re-runs
        // there (keyed on currentRoute) and consumes the target once.
        if (prefix == null || prefix in CurioRoutes.bootGatePrefixes) return@LaunchedEffect
        // Daily-reminder tap — land on the Spin deck (the shuffle page the
        // notification nudges toward), with the tab switch's popUpTo-HOME
        // back stack so Back returns to Home.
        if (PendingSpinOpen.take()) {
            navController.navigateToTab(CurioRoutes.SPIN)
            return@LaunchedEffect
        }
        val target = PendingEntryOpen.take() ?: return@LaunchedEffect
        if (prefix != CurioRoutes.HOME) {
            navController.popBackStack(CurioRoutes.HOME, inclusive = false)
        }
        navController.navigate(CurioRoutes.captureFor(target.first, target.second)) {
            launchSingleTop = true
        }
    }

    // The floating explore bubble now lives in the explore service's overlay
    // window (over other apps), so the root Box simply fills the screen.
    // v27t — the root also tracks the pointer (hover / press / wheel) so the
    // pet's eyes follow the cursor anywhere on screen (Chromebook / desktop).
    // v129 — no Scaffold: the root Box hosts the page Row directly and the
    // floating pill bar as an overlay on top (see below).
    // v131 — the root paints the THEME background again: the Scaffold used
    // to paint `colorScheme.background` behind the content, and removing it
    // left the root transparent — so the window's dark-navy bootstrap color
    // showed through the NavHost page transitions (the "dim flash" mid-fade
    // on every page switch). The pages paint their own full-bleed
    // backgrounds, so this only ever shows during transitions + gutters.
    // v147 — the drawer wraps the WHOLE NavHost root (page + rail + the
    // floating pill bar + the tour dock): it draws ABOVE the nav bar, which
    // stays composed underneath, so opening the drawer slides the sheet and
    // scrim over the bar instead of making it vanish and pop back.
    // v227 — liquid-glass pills experiment: one LayerBackdrop records
    // everything the page Row draws (marked below); the glass capsules
    // refract that recording. Published via [CurioGlassPills] (the
    // CurioNavTint handoff pattern) so all three pill sites read it.
    // v228 — the capture onDraw flags the record pass (see
    // [curioGlassCaptureDraw]) so glass pills INSIDE this subtree — the
    // Reveal bar, the Pet Designer studio bar — paint a plain fallback
    // during recording instead of sampling the layer into themselves
    // (that cycle crashed HWUI with a RenderThread stack overflow).
    val navGlassBackdrop = rememberLayerBackdrop(onDraw = { curioGlassCaptureDraw() })
    SideEffect {
        CurioGlassPills.backdrop = navGlassBackdrop
        // v292i — cache context for non-composable capability checks.
        CurioGlassPills.appContext = context
    }

    // v264 — LEGACY GLASS BLUR: on pre-Android-12 devices with the opt-in
    // experiment on, the same pages-only Box is ALSO recorded into our own
    // Compose GraphicsLayer; a throttled software snapshotter reads it back,
    // downscales and stack-blurs it, and the nav/reveal pills draw that as a
    // REAL frosted backdrop (no RenderEffect needed). Same sibling
    // architecture — the pills never record themselves.
    val legacyBlurActive = AppPreferences.legacyGlassBlurState &&
        !CurioLegacyBlur.readbackBroken &&
        android.os.Build.VERSION.SDK_INT in 26 until 31 &&
        AppPreferences.liquidGlassPillsState
    val legacyCaptureLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    if (legacyBlurActive) {
        CurioLegacyBlurSnapshotter(legacyCaptureLayer)
    }
    // v270 — glass parallax tilt experiment REMOVED (sensor + toggle gone).

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawerContent(
                onNavigate = { route ->
                    drawerScope.launch { drawerState.close() }
                    navController.navigate(route) { launchSingleTop = true }
                }
            )
        },
        gesturesEnabled = drawerState.isOpen || drawerState.isAnimationRunning
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(PetPointer.trackerModifier())
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (wide && showBottomBar) {
                // Wide windows: replaced side rail with bottom disappearing
                // capsule — content fills full width, nav floats at bottom.
            }
            Box(
                // v129 — no Scaffold: page content runs full-bleed and the
                // floating pill bar is a true overlay drawn ON TOP of the
                // page (no painted bottom slot, so no strip behind the
                // pill). Tab pages paint their own backgrounds to the very
                // bottom and clear the pill themselves; every other route
                // keeps the nav-bar inset the Scaffold's contentWindowInsets
                // used to deliver.
                modifier = Modifier
                    .then(if (wide) Modifier.fillMaxWidth() else Modifier.weight(1f))
                    .fillMaxHeight()
                    // v227 — the liquid-glass capture layer: pages only.
                    // The floating bar / sentiment pill / tour dock
                    // overlays are SIBLINGS of this Box, so they never
                    // record themselves into their own blurred backdrop.
                    .then(if (isLiquidGlassPillsActive()) Modifier.layerBackdrop(navGlassBackdrop) else Modifier)
                    .then(
                        if (legacyBlurActive) {
                            Modifier
                                .curioLegacyCapture(legacyCaptureLayer)
                                .curioLegacyCaptureGeometry()
                        } else Modifier
                    )
                    .then(
                        if ((showBottomBar && !wide) || routePrefix in fullBleedBottomRoutePrefixes) Modifier
                        else Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ),
                contentAlignment = Alignment.Center
            ) {
        // Wide windows (tablet / landscape / desktop): ONE continuous
        // full-bleed watermark collage fills the gutters around the centered
        // column so the page never floats in dead background. Each screen
        // gates its own backdrop off on wide (see the screens) so there is a
        // single collage instead of a double.
        if (wide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.55f
            )
        }
        SharedTransitionLayout(
            // The shared-transition root for the whole NavHost: the Spin
            // front ticket and the Topic Reveal hero are matched
            // "reveal-hero" shared elements, so opening a landed topic
            // morphs the reveal hero OUT of the ticket's position instead
            // of the page sliding in from the side.
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = CurioContentMaxWidth)
        ) {
            val sharedTransitionScope = this
        NavHost(
            navController = navController,
            startDestination = CurioRoutes.SPLASH,
            modifier = Modifier.fillMaxSize(),
            // ── Animated screen transitions ────────────────────────────────
            // v7.17 — page-switch glitch fix. The old exit/pop-enter slides
            // used an UNDERDAMPED spring (damping 0.9): it overshot past the
            // target and bounced back, and its timing never matched the
            // paired fade — the "weird glitchy" look on page switches. All
            // slides are now matched tweens (slide + fade finish together,
            // no overshoot), and bottom-nav TAB switches crossfade instead
            // of sliding (peer tabs restore saved state; sliding them reads
            // glitchy — this was promised in the header doc but never
            // implemented).
            enterTransition = {
                when {
                    // Reveal is the continuation of the landed Spin ticket:
                    // fade instead of the generic horizontal page slide — the
                    // shared "reveal-hero" element (Spin ticket → Reveal
                    // hero) owns the expansion, so the route stays a clean
                    // fade and the screen does not double-zoom around it.
                    // Paced to the 320ms bounds morph so the content below
                    // the hero (its staggered entrance) reads cleanly.
                    isRevealRoute(targetState) ->
                        fadeIn(animationSpec = tween(CurioMotion.Durations.Morph))
                    // v142 — Pet Designer opens with the reveal's clean fade
                    // (the scale-pop read as a mechanical zoom beside it).
                    isPetDesignerRoute(targetState) ->
                        fadeIn(animationSpec = tween(CurioMotion.Durations.Morph))
                    // Entry Detail and the modal-style push screens pop up
                    // from the screen center (scale + fade) like a modal — they
                    // never slide in from the side (v8.38 detail; v8.4x the
                    // same pop for the screens in popScreenRoutePrefixes).
                    // v166 — the modal pop is GENTLER: 0.94 instead of 0.88
                    // (half the zoom) so the screen lifts in with the fade
                    // instead of springing from 12% smaller — the "violent
                    // page opening" the user flagged. The exit mirrors it
                    // below so the pop language stays symmetric.
                    isDetailRoute(targetState) || isPopScreenRoute(targetState) ->
                        scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(CurioMotion.Durations.Morph, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(CurioMotion.Durations.Morph))
                    // Splash → Home / Onboarding: special elastic morph
                    initialState.destination.route == CurioRoutes.SPLASH ->
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = CurioMotion.Durations.Reveal,
                                delayMillis = 0
                            )
                        )
                    // Tab switches: clean crossfade (no directional slide and
                    // no scale) — the old scale-fade read as a slight zoom/old
                    // animation when opening the Cabinet from Profile; a pure
                    // fade is the smoothest peer-tab handoff.
                    isTabSwitch(initialState, targetState) ->
                        fadeIn(animationSpec = tween(CurioMotion.Durations.Standard))
                    // Other forward navigations: slide left + fade
                    else -> slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 4 },
                        animationSpec = tween(CurioMotion.Durations.Morph, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(CurioMotion.Durations.Morph))
                }
            },
            exitTransition = {
                when {
                    // Leave the Spin ticket in place while Reveal expands:
                    // the fade is paced to the shared-element morph so the
                    // source card stays visible for the whole expansion
                    // instead of winking out 150ms in (a Quick fade would
                    // vanish under the ~450ms morph).
                    isRevealRoute(targetState) ->
                        fadeOut(animationSpec = tween(CurioMotion.Durations.Morph))
                    isPetDesignerRoute(targetState) ->
                        fadeOut(animationSpec = tween(CurioMotion.Durations.Morph))
                    // The screen under the detail pop-up / modal push dims out
                    // over the SAME 450ms as the pop — no slide, and the longer
                    // fade masks the bottom-bar space release as a gentle dim
                    // instead of a snap (v8.38 detail; v8.4x pop screens).
                    isDetailRoute(targetState) || isPopScreenRoute(targetState) ->
                        fadeOut(animationSpec = tween(CurioMotion.Durations.Morph))
                    // Navigating away from splash: no exit needed
                    initialState.destination.route == CurioRoutes.SPLASH ->
                        fadeOut(animationSpec = tween(CurioMotion.Durations.Quick))
                    // A pop screen that opens a NON-pop push (e.g. Settings →
                    // Lightbox) shrinks away the same way it popped in, so the
                    // modal language stays consistent (v8.4x).
                    isPopScreenRoute(initialState) ->
                        scaleOut(
                            targetScale = 0.94f,
                            animationSpec = tween(CurioMotion.Durations.Morph, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(CurioMotion.Durations.Morph))
                    isTabSwitch(initialState, targetState) ->
                        fadeOut(animationSpec = tween(CurioMotion.Durations.Standard))
                    // Other exits: slide out slightly + fade
                    else -> slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 6 },
                        animationSpec = tween(CurioMotion.Durations.Morph, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(CurioMotion.Durations.Quick))
                }
            },
            popEnterTransition = {
                when {
                    // Popping back from Topic Reveal: fade only — the shared
                    // element morph reverses the hero into the card, and a
                    // directional slide would fight it.
                    initialState.destination.route == CurioRoutes.REVEAL ->
                        fadeIn(animationSpec = tween(CurioMotion.Durations.Morph))
                    isPetDesignerRoute(initialState) ->
                        fadeIn(animationSpec = tween(CurioMotion.Durations.Morph))
                    // Popping back from Entry Detail / a pop screen: the page
                    // below fades back in while the modal shrinks away (v8.38
                    // detail; v8.4x pop screens).
                    isDetailRoute(initialState) || isPopScreenRoute(initialState) ->
                        fadeIn(animationSpec = tween(CurioMotion.Durations.Morph))
                    // Tab switch back: clean crossfade to match the forward
                    // tab switch (no scale, no directional slide).
                    isTabSwitch(initialState, targetState) ->
                        fadeIn(animationSpec = tween(CurioMotion.Durations.Standard))
                    else -> {
                        // Back navigation: slide right + fade
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 6 },
                            animationSpec = tween(CurioMotion.Durations.Morph, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(CurioMotion.Durations.Quick))
                    }
                }
            },
            popExitTransition = {
                when {
                    // Popping Topic Reveal: fade the page out under the
                    // reversing morph instead of sliding it sideways.
                    initialState.destination.route == CurioRoutes.REVEAL ->
                        fadeOut(animationSpec = tween(CurioMotion.Durations.Morph))
                    isPetDesignerRoute(initialState) ->
                        fadeOut(animationSpec = tween(CurioMotion.Durations.Morph))
                    // The detail page / pop screen shrinks back down as it
                    // pops away — the matched fade keeps the shrink smooth over
                    // the same duration as the page beneath fading back in
                    // (v8.38 detail; v8.4x pop screens).
                    isDetailRoute(initialState) || isPopScreenRoute(initialState) ->
                        scaleOut(
                            targetScale = 0.94f,
                            animationSpec = tween(CurioMotion.Durations.Morph, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(CurioMotion.Durations.Morph))
                    isTabSwitch(initialState, targetState) ->
                        fadeOut(animationSpec = tween(CurioMotion.Durations.Standard))
                    else -> {
                        // Pop exit: slide right + fade out
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth / 4 },
                            animationSpec = tween(CurioMotion.Durations.Morph, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(CurioMotion.Durations.Morph))
                    }
                }
            }
        ) {
            // ── Splash + Onboarding (no bottom nav) ──────────────────────────
            composable(CurioRoutes.SPLASH) {
                SplashScreen(navController = navController)
            }
            composable(CurioRoutes.ONBOARDING) {
                OnboardingScreen(navController = navController)
            }

            // ── Bottom-nav tabs ──────────────────────────────────────────────
            composable(CurioRoutes.HOME) {
                HomeScreen(navController = navController)
            }
            composable(CurioRoutes.SPIN) {
                val animatedVisibilityScope = this
                CompositionLocalProvider(
                    LocalRevealSharedScope provides sharedTransitionScope,
                    LocalRevealVisibilityScope provides animatedVisibilityScope
                ) {
                    SpinScreen(categorySlug = null, navController = navController)
                }
            }
            composable(CurioRoutes.CABINET) {
                val animatedVisibilityScope = this
                CompositionLocalProvider(
                    LocalRevealSharedScope provides sharedTransitionScope,
                    LocalRevealVisibilityScope provides animatedVisibilityScope
                ) {
                    CabinetScreen(navController = navController)
                }
            }

            // ── Spin flow (no bottom nav) ──────────────────────────────────
            composable(
                route = CurioRoutes.PICKER,
            ) {
                // v3xx — the NEW category picker (Browse page with bottom
                // nav) is the default; the old glass-pill picker returns via
                // Settings → Experiments → "Classic category picker".
                if (AppPreferences.classicPickerEnabledState) {
                    CategoryPickerScreen(navController = navController)
                } else {
                    CategoryPickerBrowseScreen(navController = navController)
                }
            }
            composable(
                route = CurioRoutes.SPIN_WITH_CATEGORY,
                arguments = listOf(navArgument("categorySlug") { type = NavType.StringType })
            ) { entry ->
                val animatedVisibilityScope = this
                CompositionLocalProvider(
                    LocalRevealSharedScope provides sharedTransitionScope,
                    LocalRevealVisibilityScope provides animatedVisibilityScope
                ) {
                    SpinScreen(
                        categorySlug = entry.arguments?.getString("categorySlug"),
                        navController = navController
                    )
                }
            }
            composable(
                route = CurioRoutes.REVEAL,
                arguments = listOf(
                    navArgument("categorySlug") { type = NavType.StringType },
                    navArgument("topicName")     { type = NavType.StringType },
                    navArgument("browse")        { type = NavType.StringType; defaultValue = "0" }
                )
            ) { entry ->
                val animatedVisibilityScope = this
                CompositionLocalProvider(
                    LocalRevealSharedScope provides sharedTransitionScope,
                    LocalRevealVisibilityScope provides animatedVisibilityScope
                ) {
                    TopicRevealScreen(
                        categorySlug = entry.arguments?.getString("categorySlug").orEmpty(),
                        topicName = safeDecode(entry.arguments?.getString("topicName")),
                        navController = navController,
                        // Browse-Topics mode: read-only reveal (see CurioRoutes).
                        browseMode = entry.arguments?.getString("browse") == "1"
                    )
                }
            }
            composable(
                route = CurioRoutes.CAPTURE,
                arguments = listOf(
                    navArgument("categorySlug") { type = NavType.StringType },
                    navArgument("topicName")     { type = NavType.StringType }
                )
            ) { entry ->
                SaveCaptureScreen(
                    categorySlug = entry.arguments?.getString("categorySlug").orEmpty(),
                    topicName    = safeDecode(entry.arguments?.getString("topicName")),
                    navController = navController
                )
            }

            // ── Push destinations (no bottom nav) ──────────────────────────
            composable(
                route = CurioRoutes.ENTRY_DETAIL,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { entry ->
                val animatedVisibilityScope = this
                CompositionLocalProvider(
                    LocalRevealSharedScope provides sharedTransitionScope,
                    LocalRevealVisibilityScope provides animatedVisibilityScope
                ) {
                    EntryDetailScreen(
                        entryId = entry.arguments?.getString("entryId").orEmpty(),
                        navController = navController
                    )
                }
            }
            // Both edit routes reopen a saved entry (a single mood board or a
            // whole multi-section Portfolio) in the universal editor — the
            // screen preloads the entry, lets the user rearrange any take,
            // and re-saves in place (same id → Room REPLACE).
            composable(
                route = CurioRoutes.EDIT_MOODBOARD,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { entry ->
                SaveCaptureScreen(
                    categorySlug = "",
                    topicName = "",
                    navController = navController,
                    editEntryId = entry.arguments?.getString("entryId").orEmpty()
                )
            }
            composable(
                route = CurioRoutes.EDIT_ENTRY,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { entry ->
                SaveCaptureScreen(
                    categorySlug = "",
                    topicName = "",
                    navController = navController,
                    editEntryId = entry.arguments?.getString("entryId").orEmpty()
                )
            }
            composable(CurioRoutes.PROFILE) {
                ProfileScreen(navController = navController)
            }
            composable(CurioRoutes.QUESTS) {
                QuestsScreen(navController = navController)
            }
            composable(CurioRoutes.STATS) {
                StatsScreen(navController = navController)
            }
            composable(CurioRoutes.SETTINGS) {
                SettingsHubScreen(navController = navController)
            }
            composable(CurioRoutes.SETTINGS_APPEARANCE) {
                SettingsSectionScreen(navController = navController, page = SettingsPage.APPEARANCE)
            }
            composable(CurioRoutes.SETTINGS_PREFERENCES) {
                SettingsSectionScreen(navController = navController, page = SettingsPage.PREFERENCES)
            }
            composable(CurioRoutes.SETTINGS_RECORDING) {
                SettingsSectionScreen(navController = navController, page = SettingsPage.RECORDING)
            }
            composable(CurioRoutes.SETTINGS_DATA) {
                BackupToolsScreen(navController = navController)
            }
            composable(CurioRoutes.SETTINGS_BOOK_COVER) {
                BookCoverHubScreen(navController = navController)
            }
            composable(CurioRoutes.SHARE_HUB) {
                ShareHubScreen(navController = navController)
            }
            composable(CurioRoutes.EXPERIMENTS) {
                ExperimentsScreen(navController = navController)
            }
            composable(CurioRoutes.USER_EXPERIMENTS) {
                UserExperimentsScreen(navController = navController)
            }
            composable(CurioRoutes.GLASS_WIDGET_LAB) {
                com.curio.app.features.settings.GlassWidgetLabScreen(navController = navController)
            }
            composable(CurioRoutes.GLASS_WIDGET_EDITOR) {
                com.curio.app.features.settings.WidgetEditorScreen(navController = navController)
            }
            composable(CurioRoutes.MANAGE_CATEGORIES) {
                ManageCategoriesScreen(navController = navController)
            }
            composable(CurioRoutes.TOPIC_HISTORY) {
                TopicHistoryScreen(navController = navController)
            }
            composable(CurioRoutes.RECYCLE_BIN) {
                RecycleBinScreen(navController = navController)
            }
            composable(CurioRoutes.RECENTS_ALL) {
                RecentScreen(navController = navController)
            }
            composable(CurioRoutes.CRASH) {
                CurioCrashScreen(navController = navController)
            }
            composable(CurioRoutes.BUG_REPORT) {
                BugReportScreen(navController = navController)
            }
            composable(CurioRoutes.SUPPORT) {
                SupportScreen(navController = navController)
            }
            composable(CurioRoutes.UPDATES) {
                UpdatesScreen(navController = navController)
            }
            composable(CurioRoutes.PROMO) {
                PromoModeScreen(navController = navController)
            }
            composable(CurioRoutes.DATABASE) {
                TopicDatabaseScreen(navController = navController)
            }
            composable(CurioRoutes.FIELDMIND_OBSERVATION) {
                FieldMindObservationScreen(navController = navController)
            }
            composable(CurioRoutes.PET_DESIGNER) {
                PetDesignerScreen(navController = navController)
            }
            composable(CurioRoutes.OUTFIT_SHOP) {
                OutfitShopScreen(navController = navController)
            }
            composable(route = CurioRoutes.LIGHTBOX) {
                // The image URI is handed off out-of-band via LightboxTarget
                // (see CurioRoutes.lightbox) — no route arg, so no encoding/
                // decoding round-trip that could corrupt content URIs.
                LightboxScreen(navController = navController)
            }
        }
        }
            }
        }
        // v129 — the floating pill bar is now a true overlay on the page
        // (Scaffold removed): it sits above the NavHost content, aligned to
        // the bottom center, so no painted slot / strip sits behind it. It
        // draws over the page's own full-bleed background; the tab pages
        // clear it themselves (see Home / Spin / Cabinet bottom padding).
        // v147 — the drawer now lives at the NavHost root and draws OVER
        // this bar (which stays composed underneath) — no more yielding.
        // v144 — the bar YIELDS while the tour is running: the tour's
        // floating pill dock floats at the same bottom-center spot, and the
        // old opaque dock covered the bar anyway, so the bar must not show
        // behind/around the tour pill on tab stops.
        if (barVisible && TourController.currentStep == null) {
            CurioFloatingNavBar(
                navController = navController,
                // While the bar lingers after leaving the tab set, force the
                // collapse: NO pill stays selected (they all glide closed),
                // so the reveal route can't keep the Spin pill popped open.
                collapsing = !showBottomBar,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        // v208e — the reveal's Like/Dislike pill renders in THIS overlay,
        // composed AFTER the bar, so it draws ON TOP of the collapsing nav
        // pill during the handoff (z-index above the nav pill — user
        // request). The reveal registers its pill via [SentimentPillHost];
        // the wrapper Box has no pointer input, so touches pass through
        // everywhere except the pill itself.
        // v208f — gated on the reveal route so the pill VANISHES the moment
        // you tap back (the route flips before the screen finishes its exit
        // transition — the old gate waited for the screen to fully dispose,
        // so the pill lingered: "why the like and dislike pill now staying
        // longer… make it vanish like before just when i tap back").
        if (isRevealRoutePrefix) {
            SentimentPillHost.content?.let { pill ->
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.align(Alignment.BottomCenter)) { pill() }
                }
            }
        }

    // Keep the tour controls inside the existing root Box. The Row is a
    // small direct child, not a full-screen transparent hit-test layer.
    val tourStep = TourController.currentStep
    if (tourStep != null && routePrefix == tourStep.routePrefix) {
        fun advanceTourAndNavigate() {
            val wasLastStep = TourController.isLastStep
            TourController.advance()
            val nextRoute = TourController.routeForCurrentStep()
            if (nextRoute != null && nextRoute != currentRoute) {
                // v123 — tab steps (Spin / Cabinet) must navigate like REAL
                // tab switches (navigateToTab), never a plain push. A plain
                // `navigate("spin")` left HOME out of the NavController's
                // saved-state map, so the next Home-tab tap ran
                // popUpTo(HOME){saveState} (which maps the popped stack to
                // HOME) + restoreState (which then RESTORED that stack) —
                // landing back on Spin and making "Home" look dead after
                // skipping the tour there. navigateToTab plants HOME's
                // null mapping on its first popUpTo, so the later Home tap
                // restores nothing (see CurioRoutes.navigateToTab).
                navController.navigateToQuestRoute(nextRoute)
            } else if (wasLastStep) {
                // Tour finished — the tour always starts on the Home hub, so
                // pop the whole tour stack back to Home (a clean finish
                // instead of leaving the user stranded on the last stop).
                navController.popBackStack(CurioRoutes.HOME, inclusive = false)
            }
        }
        // v9.x — tap ANYWHERE to advance the tour. A full-screen transparent
        // hit layer sits behind the bottom dock, so every tap on the screen
        // (the demonstrated control included) acts as "Next" without ever
        // firing the real action — the tour stays a pure demo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { advanceTourAndNavigate() }
        )
        // v144 — the tour controls are now a FLOATING PILL BAR, the same
        // recipe as CurioFloatingNavBar: a rounded-50 surfaceContainerHigh
        // capsule floating above the gesture bar (12dp air gap) instead of a
        // full-width opaque dock — the page shows through around it. The
        // buttons are content-sized capsules inside (Skip = soft secondary,
        // Next/Done = solid primary CTA). The full-screen tap-to-advance
        // layer below is untouched.
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(50),
            // v149 — same dynamic container as the floating nav bar: the
            // pill follows the page tint while staying elevated.
            // v160 — the dark-mode hairline rim is gone (see v157).
            color = curioFloatingNavContainer(routePrefix),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { TourController.skip() },
                    modifier = Modifier.height(52.dp),
                    // v114 — full capsule to match the app's pill language
                    // (the old 16dp boxy corners read stock M3 next to the
                    // custom pill/chip family).
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        "Skip",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // The final stop labels the control "Done" — advancing past
                // it properly closes the tour instead of silently stopping.
                Button(
                    onClick = { advanceTourAndNavigate() },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        if (TourController.isLastStep) "Done" else "Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    }
    } // v147 — ModalNavigationDrawer close (the drawer floats above the bar)
    // ── Pet-led Tour offer and controls ─────────────────────────────────
    // The offer is intentionally rendered on Home after onboarding; the Tour
    // itself has no scrim and leaves every demonstrated control tappable.
    if (routePrefix == CurioRoutes.HOME && TourController.offerPending) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { TourController.declineOffer() },
            title = { Text("Take a tiny tour?") },
            text = { Text("Curie can walk you through the main controls. Nothing will start, open, or be saved while you tour.") },
            confirmButton = {
                TextButton(onClick = { TourController.start() }, colors = curioDialogActionButtonColors()) { Text("Take the tour") }
            },
            dismissButton = {
                TextButton(onClick = { TourController.declineOffer() }, colors = curioDialogActionButtonColors()) { Text("Maybe later") }
            }
        )
    }

    // v8.8 — the floating Curio pet: a global overlay drawn above the whole
    // NavHost (over the floating pill bar too). Renders only while the pet layer,
    // the floating toggle and the pet's awake state are on; it wanders, can
    // be dragged anywhere, long-pressed home into its house, and naps back
    // after a long idle. v10 — it stays out only during splash/crash gates.
    if (
        routePrefix != CurioRoutes.SPLASH &&
        routePrefix != CurioRoutes.CRASH &&
        routePrefix != CurioRoutes.ONBOARDING
    ) {
        CurioFloatingPet(routePrefix = routePrefix)
    }

    // ── Done-exploring prompt (app return while a session is active) ────
    val activeSession = ExploreSessionStore.activeSessionState
    if (showDoneDialog && activeSession != null) {
        // Live elapsed time — ticks every second while the dialog is open
        // (pause-aware: session.elapsedMillis banks paused time, so a paused
        // session shows a frozen reading). Cancels on dismiss.
        var elapsedMillis by remember(activeSession.startMillis) {
            mutableStateOf(activeSession.elapsedMillis())
        }
        LaunchedEffect(activeSession.startMillis, activeSession.paused) {
            while (true) {
                elapsedMillis = activeSession.elapsedMillis()
                delay(1_000)
            }
        }
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = {
                showDoneDialog = false
                confirmSessionCancel = false
                activeSession.let { dialogDismissedFor = it.startMillis }
            },
            title = {
                Text(
                    if (confirmSessionCancel) "Cancel this explore?"
                    else "Done exploring ${activeSession.topicName}?"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (confirmSessionCancel) {
                        // The double-confirmation step — make the cost of
                        // cancelling explicit before the session is dropped.
                        Text(
                            "This ends the session now. The ${formatElapsed(elapsedMillis)} isn't saved and you won't be asked to write about ${activeSession.topicName}. You can explore it again anytime.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurioIcon(
                                name = if (activeSession.paused) CurioIcons.Pause else CurioIcons.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                size = 18.dp
                            )
                            Text(
                                if (activeSession.paused)
                                    "Paused at ${formatElapsed(elapsedMillis)}. Tap Resume on the bubble or notification to continue"
                                else
                                    "You've been exploring for ${formatElapsed(elapsedMillis)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Text(
                            "You started ${activeSession.verb.lowercase()} ${activeSession.targetName}. If you're done, write it down while it's fresh. Or keep exploring, no rush.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (confirmSessionCancel) {
                    // Second tap — the actual end. Quiet teardown, same as
                    // the notification's Cancel action (no write-it-down
                    // page, no done prompt on the next return).
                    TextButton(onClick = {
                        showDoneDialog = false
                        confirmSessionCancel = false
                        // v226 — stash the cancelled session so Home can
                        // offer it back (recovery card) instead of the
                        // banked time vanishing.
                        ExploreSessionStore.stashCancelledSession(context, activeSession)
                        ExploreSessionStore.clearSession(context)
                        ExploreReminderScheduler.cancel(context)
                        ExploreSessionService.stop(context)
                    }) {
                        Text("Yes, cancel session", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(onClick = {
                        showDoneDialog = false
                        confirmSessionCancel = false
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
                        // Anchor HOME beneath the entry page so Back returns to
                        // the app instead of exiting from a deep-opened page.
                        val routePrefix = currentRoute?.substringBefore("/")
                        if (routePrefix != null &&
                            routePrefix != CurioRoutes.HOME &&
                            routePrefix !in CurioRoutes.bootGatePrefixes
                        ) {
                            navController.popBackStack(CurioRoutes.HOME, inclusive = false)
                        }
                        navController.navigate(
                            CurioRoutes.captureFor(activeSession.categoryId.routeSlug, activeSession.topicName)
                        ) { launchSingleTop = true }
                    },
                        colors = curioDialogActionButtonColors()
                    ) { Text("Express yourself") }
                }
            },
            dismissButton = {
                if (confirmSessionCancel) {
                    // Back out of the cancel — keep exploring.
                    TextButton(
                        onClick = { confirmSessionCancel = false },
                        colors = curioDialogActionButtonColors()
                    ) { Text("Keep exploring") }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { confirmSessionCancel = true }) {
                            Text("Cancel session", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = {
                            showDoneDialog = false
                            activeSession.let { dialogDismissedFor = it.startMillis }
                        }, colors = curioDialogActionButtonColors()) { Text("Keep exploring") }
                    }
                }
            }
        )
    }

    // v227d — the update notice is now a proper themed DIALOG (the old
    // corner toast pill is fully removed). Rendered at the NavHost root so
    // it floats above every screen; "Open Updates" navigates to the
    // Updates page, "Later" just dismisses (the once-per-version gate in
    // UpdateChecker means it never nags again for the same release).
    CurioUpdatePrompt.pending?.let { pendingVersion ->
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = { CurioUpdatePrompt.dismiss() },
            title = { Text("Curio $pendingVersion is available") },
            text = {
                Text(
                    "A newer version of Curio is ready. See what changed and install it from the Updates page.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        CurioUpdatePrompt.dismiss()
                        navController.navigate(CurioRoutes.UPDATES) { launchSingleTop = true }
                    },
                    colors = curioDialogActionButtonColors()
                ) { Text("Open Updates") }
            },
            dismissButton = {
                TextButton(
                    onClick = { CurioUpdatePrompt.dismiss() },
                    colors = curioDialogActionButtonColors()
                ) { Text("Later") }
            }
        )
    }
}


