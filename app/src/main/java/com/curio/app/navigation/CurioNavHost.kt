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
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.ExploreReminderScheduler
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.QuestGuide
import com.curio.app.data.formatElapsed
import com.curio.app.infrastructure.ExploreSessionService
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import kotlinx.coroutines.delay
import com.curio.app.features.bugreport.BugReportScreen
import com.curio.app.features.database.TopicDatabaseScreen
import com.curio.app.features.support.PromoModeScreen
import com.curio.app.features.support.SupportScreen
import com.curio.app.features.crash.CurioCrashScreen
import com.curio.app.features.lightbox.LightboxScreen
import com.curio.app.features.managecategories.ManageCategoriesScreen
import com.curio.app.features.onboarding.OnboardingScreen
import com.curio.app.features.profile.ProfileScreen
import com.curio.app.features.quests.QuestsScreen
import com.curio.app.features.settings.BackupToolsScreen
import com.curio.app.features.settings.ExperimentsScreen
import com.curio.app.features.settings.SettingsHubScreen
import com.curio.app.features.settings.SettingsPage
import com.curio.app.features.settings.SettingsSectionScreen
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.topichistory.TopicHistoryScreen
import com.curio.app.features.recent.RecentScreen
import com.curio.app.features.cabinet.CabinetScreen
import com.curio.app.features.capture.SaveCaptureScreen
import com.curio.app.features.detail.EntryDetailScreen
import com.curio.app.features.picker.CategoryPickerScreen
import com.curio.app.features.reveal.TopicRevealScreen
import com.curio.app.features.spin.SpinScreen
import com.curio.app.features.home.HomeScreen
import com.curio.app.features.splash.SplashScreen
import com.curio.app.features.fieldmind.FieldMindObservationScreen
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.adaptive.LocalRevealSharedScope
import com.curio.app.ui.adaptive.LocalRevealVisibilityScope
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioBottomBar
import com.curio.app.ui.components.CurioNavigationRail
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.GuidePointer
import com.curio.app.ui.components.QuestGuideToast
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

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
): Boolean =
    initialState.destination.route?.substringBefore("/") in CurioRoutes.bottomNavRoutePrefixes &&
        targetState.destination.route?.substringBefore("/") in CurioRoutes.bottomNavRoutePrefixes

/**
 * The Curio NavHost — single-NavHost scaffold for the active app.
 *
 * All routes are flat. The bottom nav is rendered by a [Scaffold] wrapper
 * and is conditionally visible based on the current route (see
 * [CurioRoutes.bottomNavRoutes]). When the user is on a non-bottom-nav
 * route (push destinations like Picker/Reveal/Capture/Detail/Settings/
 * ManageCategories/TopicHistory/Lightbox), the bottom bar is omitted.
 *
 * Each tab uses the standard Compose Navigation pattern when navigated to:
 *   navigate(route) { popUpTo(startDestination) { saveState = true }; ... }
 * — see CurioBottomNav for the actual call site. This preserves each tab's
 * back stack across switches.
 *
 * Upgraded navigation transitions:
 *  - Forward navigations: slide left + fade (matched tweens)
 *  - Back navigations: slide right + fade (matched tweens)
 *  - Tab switches (bottom nav): simple crossfade (no directional slide)
 *  - Splash → Home / Onboarding: fade-only reveal
 * v7.17 — the old exit/pop-enter slides used underdamped springs that
 * overshot and bounced (and never matched their paired fade) — the
 * page-switch glitch. All transitions now use matched tweens, and tab
 * switches crossfade.
 */
@Composable
private fun RevealBottomBarPlaceholder(
    bottomBarHeightPx: Int,
    density: androidx.compose.ui.unit.Density
) {
    val reserve = if (bottomBarHeightPx > 0) {
        with(density) { bottomBarHeightPx.toDp() }
    } else null
    Spacer(
        modifier = Modifier.fillMaxWidth().then(
            if (reserve != null) Modifier.height(reserve)
            else Modifier
                .heightIn(min = 80.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        )
    )
}

@Composable
fun CurioNavHost(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val routePrefix = remember(currentRoute) {
        currentRoute?.substringBefore("/")
    }
    val showBottomBar = routePrefix in CurioRoutes.bottomNavRoutePrefixes
    // Push destinations that host a shared-element morph TARGET whose
    // source is a bottom-bar tab (the Reveal hero grows out of the Spin
    // ticket). The bottom chrome must still RESERVE the bottom bar's height
    // (an invisible placeholder below) so innerPadding never changes the
    // moment the bar hides — otherwise the exiting Spin screen re-lays-out
    // mid-transition and the morph source card visibly dips down before
    // expanding (the "moves down, then animates" artifact), and the reveal
    // watermark shifts down into the vacated bar strip.
    // v8.2 — bug fix: this compared the route PREFIX ("reveal"/"detail")
    // against the FULL route patterns ("reveal/{categorySlug}/{topicName}?
    // browse={browse}", "detail/{entryId}"), so it was ALWAYS false and the
    // placeholder never rendered — the bar hid mid-morph, innerPadding grew
    // by the bar's height, and the watermark visibly shifted down. Compare
    // the prefixes so the reserve actually engages.
    // v8.4 — the Entry Detail route is dropped from the reserve: its morph
    // source (Cabinet) reads fine without the bar reserved, so no
    // placeholder is needed there.
    val reserveBarSpace = routePrefix != null && routePrefix in setOf(
        CurioRoutes.REVEAL.substringBefore("/")
    )
    // The reveal page paints its own category wash over the whole content
    // area, and its action dock wears the SAME wash (see RevealActionDock),
    // so the Scaffold's default background never shows as a strip behind
    // the transparent dock while the reveal is open — and the Scaffold's
    // containerColor stays CONSTANT across the whole route transition.
    // (v8.5 regression fix: painting the Scaffold container with a
    // dynamically-computed revealWash here restarted/disrupted the
    // shared-element route transition — the morph froze and the entire
    // reveal page stayed invisible except the dock, which lives outside
    // the SharedTransitionLayout. The dock now carries the wash itself.)
    // The bottom bar's exact measured height (px) — captured from the real
    // bar so the invisible morph-transition placeholder can reserve IDENTICAL
    // space. M3's NavigationBar consumes the nav-bar inset inside its 80dp
    // min height, so a naive "80dp + nav inset" placeholder is TALLER than
    // the bar by the inset: the moment the bar is swapped for the placeholder
    // (Spin → Reveal), Scaffold innerPadding changes and the
    // SharedTransitionLayout resizes mid-morph, re-laying out the exiting
    // screen — the watermark re-aligns and the shared source bounds shift
    // (the "morph starts a little down" artifact). Reserving the measured
    // height keeps innerPadding constant across the whole transition.
    var bottomBarHeightPx by rememberSaveable { mutableStateOf(0) }
    var revealBottomBarContent by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    var showDoneDialog by rememberSaveable { mutableStateOf(false) }
    // v7.31 — two-step "Cancel session": the first tap flips the done-now
    // dialog into a confirm step, the second tap actually ends the explore.
    var confirmSessionCancel by rememberSaveable { mutableStateOf(false) }
    // Survives rotation so the startup prompt only fires on a truly fresh
    // process (an active session left behind by a killed app).
    var startupPromptDone by rememberSaveable { mutableStateOf(false) }

    // ── Quest tour (v8.1/v8.2) — the tour overlay + auto-navigating runner
    //    below. The tour itself is STARTED only from the Quests page (the
    //    one-time offer on the first quest — see QuestsScreen); the old
    //    auto-showing "next quest" guide pill was removed in v8.2 so nothing
    //    pops up from other screens.
    // ── Quest tour runner — auto-navigate to the current step's screen so
    //    every overlay tap advances the walkthrough to the next place.
    LaunchedEffect(QuestGuide.active, QuestGuide.index, routePrefix) {
        if (!QuestGuide.active) return@LaunchedEffect
        val step = QuestGuide.current ?: return@LaunchedEffect
        if (step.route.isEmpty()) return@LaunchedEffect
        if (routePrefix != step.route) {
            navController.navigateToQuestRoute(step.route)
        }
    }
    // When the tour ends (Finish / the overlay's X), land the user back on a
    // stable tab instead of leaving the pushed tour screens stacked.
    var tourWasActive by remember { mutableStateOf(false) }
    LaunchedEffect(QuestGuide.active) {
        val now = QuestGuide.active
        if (tourWasActive && !now) {
            val prefix = routePrefix
            if (prefix != null && prefix !in CurioRoutes.bottomNavRoutePrefixes) {
                navController.popBackStack(CurioRoutes.HOME, inclusive = false)
            }
        }
        tourWasActive = now
    }

    // Ask "are you done exploring?" whenever the app returns to the
    // foreground while an explore session is active — mid-session, after
    // the browser search, or after the app was killed in the background.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (AppPreferences.isExploreSessionsEnabled(context)) {
                    val resumed = ExploreSessionStore.getActiveSession(context)
                    showDoneDialog = resumed != null
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
                showDoneDialog = session != null
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

    // ── Adaptive window layout (tablet & landscape) ────────────────────
    // Medium/Expanded windows (>= 600dp wide) move the three tabs into a
    // left-edge NavigationRail and center page content in a comfortable
    // max-width column ([CurioContentMaxWidth]) with the theme background
    // filling the gutters. Compact phones keep the bottom bar and full-width
    // content exactly as before. Always-on — no Settings toggle.
    val wide = windowWidthSizeClass().isWide

    // The floating explore bubble now lives in the explore service's overlay
    // window (over other apps), so the Scaffold simply fills the screen.
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!wide && showBottomBar) {
                    CurioBottomBar(
                        navController = navController,
                        // Measure the bar's real height so the morph
                        // placeholder below can reserve exactly this much
                        // (see bottomBarHeightPx).
                        modifier = Modifier.onSizeChanged { bottomBarHeightPx = it.height }
                    )
                } else if (!wide && reserveBarSpace) {
                    // The reveal actions occupy the same bottom strip that
                    // used to be only an invisible morph placeholder. This
                    // keeps the Scaffold height stable while giving the
                    // reserved area the reveal category tint.
                    revealBottomBarContent?.invoke()
                        ?: RevealBottomBarPlaceholder(
                            bottomBarHeightPx = bottomBarHeightPx,
                            density = density
                        )
                }
            },
            // Every screen applies its own statusBarsPadding().  This Scaffold
            // has no topBar, so without pinning the insets to the bottom only
            // M3 would add the status-bar inset to innerPadding AND the screens
            // would add it again — a double top gap (huge empty space above the
            // status bar).  Screens without a bottom bar still get the nav-bar
            // inset from here.
            contentWindowInsets = WindowInsets.navigationBars,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize()) {
            if (wide && showBottomBar) {
                // Wide windows: the rail sits at the left edge, full height,
                // and applies its own system-bar insets.
                CurioNavigationRail(
                    navController = navController,
                    modifier = Modifier.fillMaxHeight()
                )
            }
            Box(
                // The content area keeps the scaffold insets (bottom bar
                // height + nav-bar inset) exactly as before; wide windows
                // just add the centered max-width cap.
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(innerPadding),
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
                    // Splash → Home / Onboarding: special elastic morph
                    initialState.destination.route == CurioRoutes.SPLASH ->
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = CurioMotion.Durations.Reveal,
                                delayMillis = 0
                            )
                        )
                    // Tab switches: simple crossfade (no directional slide)
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
                    // Navigating away from splash: no exit needed
                    initialState.destination.route == CurioRoutes.SPLASH ->
                        fadeOut(animationSpec = tween(CurioMotion.Durations.Quick))
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
                    // Tab switch back: crossfade too (no directional slide).
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
                CategoryPickerScreen(navController = navController)
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
                        topicName    = safeDecode(entry.arguments?.getString("topicName")),
                    navController = navController,
                    // Browse-Topics mode: read-only reveal (see CurioRoutes).
                    browseMode = entry.arguments?.getString("browse") == "1",
                    onBottomBarContentChanged = { revealBottomBarContent = it },
                    onBottomBarContentCleared = { revealBottomBarContent = null }
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
            composable(CurioRoutes.SETTINGS) {
                SettingsHubScreen(navController = navController)
            }
            composable(CurioRoutes.SETTINGS_APPEARANCE) {
                SettingsSectionScreen(navController = navController, page = SettingsPage.APPEARANCE)
            }
            composable(CurioRoutes.SETTINGS_NOTIFICATIONS) {
                SettingsSectionScreen(navController = navController, page = SettingsPage.NOTIFICATIONS)
            }
            composable(CurioRoutes.SETTINGS_RECORDING) {
                SettingsSectionScreen(navController = navController, page = SettingsPage.RECORDING)
            }
            composable(CurioRoutes.SETTINGS_DATA) {
                BackupToolsScreen(navController = navController)
            }
            composable(CurioRoutes.SETTINGS_ABOUT) {
                SettingsSectionScreen(navController = navController, page = SettingsPage.ABOUT)
            }
            composable(CurioRoutes.EXPERIMENTS) {
                ExperimentsScreen(navController = navController)
            }
            composable(CurioRoutes.MANAGE_CATEGORIES) {
                ManageCategoriesScreen(navController = navController)
            }
            composable(CurioRoutes.TOPIC_HISTORY) {
                TopicHistoryScreen(navController = navController)
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
            composable(CurioRoutes.PROMO) {
                PromoModeScreen(navController = navController)
            }
            composable(CurioRoutes.DATABASE) {
                TopicDatabaseScreen(navController = navController)
            }
            composable(CurioRoutes.FIELDMIND_OBSERVATION) {
                FieldMindObservationScreen(navController = navController)
            }
            composable(route = CurioRoutes.LIGHTBOX) {
                // The image URI is handed off out-of-band via LightboxTarget
                // (see CurioRoutes.lightbox) — no route arg, so no encoding/
                // decoding round-trip that could corrupt content URIs.
                LightboxScreen(navController = navController)
            }
        }
        }
        // ── Quest tour overlay (v8.1/v8.3) — a compact IN-APP floating pill,
        //    NOT a dialog. MOVES WITH THE STEP (v8.3): bottom of the screen
        //    for the tab steps, below the settings-family hero for Quests /
        //    Settings, centered on the final step — with a pointer arrow at
        //    the content it describes and progress dots. Rendered only while
        //    a tour is active; the tour itself is started from the Quests
        //    page (v8.2), never auto-shown.
        if (QuestGuide.active) {
            QuestGuide.current?.let { step ->
                QuestGuideToast(
                    title = step.title,
                    message = step.message,
                    stepIndex = QuestGuide.index + 1,
                    stepCount = QuestGuide.steps.size,
                    pointer = when (step.position) {
                        QuestGuide.Position.BOTTOM -> GuidePointer.UP
                        QuestGuide.Position.TOP -> GuidePointer.DOWN
                        QuestGuide.Position.CENTER -> null
                    },
                    actionLabel = if (QuestGuide.isLast) "Finish" else "Next",
                    onClick = { if (QuestGuide.isLast) QuestGuide.stop() else QuestGuide.next() },
                    onClose = { QuestGuide.stop() },
                    modifier = Modifier
                        .align(
                            when (step.position) {
                                QuestGuide.Position.BOTTOM -> Alignment.BottomCenter
                                QuestGuide.Position.TOP -> Alignment.TopCenter
                                QuestGuide.Position.CENTER -> Alignment.Center
                            }
                        )
                        // TOP steps (Quests / Settings) sit below the screen
                        // hero instead of floating over it.
                        .padding(
                            start = 16.dp,
                            top = if (step.position == QuestGuide.Position.TOP)
                                SettingsHeroTotalHeight + 8.dp else 10.dp,
                            end = 16.dp,
                            bottom = 10.dp
                        )
                )
            }
        }
            }
        }
    }
    }

    // (The v8.0 full-dialog guide and the v8.1 auto-showing "next quest"
    // overlay were replaced in v8.2: the tour is offered ONCE on the Quests
    // page when the user taps the first quest and accepts the prompt — see
    // QuestsScreen. The active-tour overlay is rendered above; the
    // QuestGuide state lives at the top of this composable.)

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
            onDismissRequest = {
                showDoneDialog = false
                confirmSessionCancel = false
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
                            "This ends the session now — the ${formatElapsed(elapsedMillis)} isn't saved and you won't be asked to write about ${activeSession.topicName}. You can explore it again anytime.",
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
                                    "Paused at ${formatElapsed(elapsedMillis)} — tap Resume on the bubble or notification to continue"
                                else
                                    "You've been exploring for ${formatElapsed(elapsedMillis)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Text(
                            "You started ${activeSession.verb.lowercase()} ${activeSession.targetName} — if you're done, write it down while it's fresh. Or keep exploring, no rush.",
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
                    }) { Text("Done — write about it") }
                }
            },
            dismissButton = {
                if (confirmSessionCancel) {
                    // Back out of the cancel — keep exploring.
                    TextButton(onClick = { confirmSessionCancel = false }) { Text("Keep exploring") }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { confirmSessionCancel = true }) {
                            Text("Cancel session", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { showDoneDialog = false }) { Text("Keep exploring") }
                    }
                }
            }
        )
    }
}


