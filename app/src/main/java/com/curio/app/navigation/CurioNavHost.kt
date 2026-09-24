package com.curio.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import androidx.navigation.NavController
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
import com.curio.app.data.markCompleted
import com.curio.app.data.formatElapsed
import com.curio.app.infrastructure.ExploreSessionService
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioAccentInk
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.curio.app.features.bugreport.BugReportScreen
import com.curio.app.features.database.TopicDatabaseScreen
import com.curio.app.features.support.SupportScreen
import com.curio.app.BuildConfig
import com.curio.app.features.feedback.FeedbackFormSheet
import com.curio.app.features.feedback.FeedbackFormState
import com.curio.app.features.updates.UpdatesScreen
import com.curio.app.features.updates.WhatsNewScreen
import com.curio.app.features.updates.WhatsNewSheet
import com.curio.app.features.updates.whatsNewRelease
import com.curio.app.features.crash.CurioCrashScreen
import com.curio.app.features.lightbox.LightboxScreen
import com.curio.app.features.managecategories.ManageCategoriesScreen
import com.curio.app.features.onboarding.CurioOnboardingState
import com.curio.app.features.onboarding.OnboardingScreen
import com.curio.app.features.personal.BookDetailScreen
import com.curio.app.features.personal.BookShelfScreen
import com.curio.app.features.personal.BookReaderScreen
import com.curio.app.features.personal.BookReviewScreen
import com.curio.app.features.personal.ChapterScreen
import com.curio.app.features.personal.JournalEditorScreen
import com.curio.app.features.personal.JournalListScreen
import com.curio.app.features.personal.TodoScreen
import com.curio.app.features.personal.TopicNoteScreen
import com.curio.app.features.personal.PersonalPhotoOverlay
import com.curio.app.features.personal.rememberPersonalPhotoOverlayState
import com.curio.app.features.profile.ProfileEditScreen
import com.curio.app.features.profile.ProfileScreen
import com.curio.app.features.quests.QuestsScreen
import com.curio.app.features.stats.StatsScreen
import com.curio.app.features.settings.BackupToolsScreen
import com.curio.app.features.settings.BookCoverHubScreen
import com.curio.app.features.settings.ExperimentsScreen
import com.curio.app.features.settings.UserExperimentsScreen
import com.curio.app.features.community.ChatsScreen
import com.curio.app.features.community.CommunityCardScreen
import com.curio.app.features.community.CommunityScreen
import com.curio.app.features.community.ModerationScreen
import com.curio.app.features.community.DirectMessageScreen
import com.curio.app.features.community.FriendsScreen
import com.curio.app.features.community.SocialProfileScreen
import com.curio.app.features.settings.OnlineModeScreen
import com.curio.app.features.settings.PrivacyScreen
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
import com.curio.app.features.petdesigner.PetDesignerScreen
import com.curio.app.features.picker.CategoryPickerBrowseScreen
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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.features.personal.PersonalVoicePill
import com.curio.app.ui.pet.CurioFloatingPet
import com.curio.app.ui.pet.PetPointer
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.CurioRevealHost

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

/**
 * Routes that render the shared settings chrome (hero + nav rail) — the
 * hub, every settings sub-page, and the rail destinations (share hub,
 * topic history, experiments, categories, pet designer, support, recycle
 * bin, updates). Navigation that STAYS inside this family crossfades
 * (pure fade, no scale, no slide): the header sits in the same place on
 * both screens, so a directional slide or scale re-reads as the header
 * jumping while the content text and lower pages fade — the calm,
 * stable settings handoff the rail glide was fighting.
 */
private val settingsFamilyRoutePrefixes: Set<String> = setOf(
    CurioRoutes.SETTINGS, // hub + every settings sub-page (prefix match)
    CurioRoutes.EXPERIMENTS,
    CurioRoutes.USER_EXPERIMENTS,
    CurioRoutes.MANAGE_CATEGORIES,
    CurioRoutes.TOPIC_HISTORY,
    CurioRoutes.SHARE_HUB,
    CurioRoutes.PET_DESIGNER,
    CurioRoutes.SUPPORT,
    CurioRoutes.RECYCLE_BIN,
    CurioRoutes.UPDATES,
    CurioRoutes.WHATS_NEW
)

/** True when the entry is inside the settings family (shared chrome). */
private fun isSettingsFamilyRoute(entry: NavBackStackEntry): Boolean =
    entry.destination.route?.substringBefore("/") in settingsFamilyRoutePrefixes

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
): Boolean =
    // Browse-mode Reveal is a pushed read-only page, not a tab — it never
    // crossfades like a tab switch.
    !isBrowseRevealRoute(targetState) && !isBrowseRevealRoute(initialState) &&
        initialState.destination.route?.substringBefore("/") in CurioRoutes.liveTabPrefixes() &&
        targetState.destination.route?.substringBefore("/") in CurioRoutes.liveTabPrefixes()

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

/**
 * v3xx — wraps a settings-family destination with the shared-transition
 * scopes (the same locals Spin/Reveal use for the "reveal-hero" morph), so
 * the settings nav rail's active pill can be a SHARED ELEMENT: switching
 * sections morphs the highlight from the old screen's chip to the new
 * screen's chip while the pages crossfade. Every rail-bearing destination
 * is wrapped with this so both sides of any switch can participate.
 */
@Composable
private fun SettingsSharedScope(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalRevealSharedScope provides sharedTransitionScope,
        LocalRevealVisibilityScope provides animatedVisibilityScope
    ) { content() }
}

/**
 * The SOCIAL layer's own scopes — the community, a member's profile, one
 * card's view and a conversation.
 *
 * It provides exactly the same shared-transition locals as the settings
 * wrapper, because a social page's hero morphs the same way; what matters is
 * that the community is NOT a settings destination. Its screens draw their
 * own header and never mount the settings rail, so the social layer reads as
 * people rather than as a corner of Settings.
 */
@Composable
private fun SocialSharedScope(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalRevealSharedScope provides sharedTransitionScope,
        LocalRevealVisibilityScope provides animatedVisibilityScope
    ) { content() }
}

@Composable
fun CurioNavHost(
    navController: NavHostController = rememberNavController()
) {
    // v3xx45 — SCREEN REVEAL experiment (Settings ▸ Experiments, default OFF).
    // Every destination change tries to peel the frozen pre-tap frame away in
    // the same feathered iris as the light/dark flip; when no frame is armed
    // (pin/back/deep-link navigation, experiment off, failed capture) the
    // normal page transitions run untouched — see CurioRevealNav.
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { controller, destination, _ ->
            // Navigations whose motion is hand-tuned elsewhere keep it: the
            // shared-element hero morphs (Reveal / Pet Designer, either side
            // of the hand-off) and SWITCHES INSIDE the settings family, where
            // the nav-rail pill morph IS the transition. Opening Settings (or
            // Profile) FROM another screen still gets the iris — only the
            // internal rail switches opt out.
            val targetPrefix = destination.route?.substringBefore("/")
            val sourcePrefix = controller.previousBackStackEntry
                ?.destination?.route?.substringBefore("/")
            val heroNav =
                targetPrefix == CurioRoutes.REVEAL.substringBefore("/") ||
                    sourcePrefix == CurioRoutes.REVEAL.substringBefore("/") ||
                    targetPrefix == CurioRoutes.PET_DESIGNER ||
                    sourcePrefix == CurioRoutes.PET_DESIGNER
            val settingsInternal = targetPrefix in settingsFamilyRoutePrefixes &&
                sourcePrefix in settingsFamilyRoutePrefixes
            CurioRevealNav.onDestinationChanged(skipReveal = heroNav || settingsInternal)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
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
    // v3xx — [CurioRoutes.liveTabPrefixes] (not the static set) so the bar
    // shows on the Community wall only while its opt-in tab is on; with the
    // opt-in off the wall is a plain pushed page reached from Settings and
    // keeps the old chromeless look.
    // A card's own view shares the WALL's route prefix ("community/…"), so
    // the prefix check alone kept the nav bar on a post — the one place it
    // should never float over. The card route is excluded by its FULL route
    // pattern instead.
    val isCardRoute = currentRoute == CurioRoutes.COMMUNITY_CARD
    val showBottomBar =
        routePrefix in CurioRoutes.liveTabPrefixes() && !isRevealRoutePrefix && !isCardRoute
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
    // ── v457 — AND THE ONE ROUTE THAT NEEDS AN EXACT MATCH ─────────────
    //
    // The dictionary page wears the reader's own paper, and the inset below
    // stopped that paper above the gesture bar: the page's bottom carried a
    // bare strip of the APP's background in a different colour, which is
    // exactly what the member reported (*"the dictionary page is bad, bottom
    // area is covered with something"*). It cannot be added to the PREFIX set
    // above: its prefix is `reader`, which the reader's own settings page
    // shares, and that page has its own (different) bottom treatment. So the
    // dictionary is named by its full route, and it clears the gesture bar
    // itself — its own `navigationBarsPadding` (see ReaderDictionaryPage).
    val fullBleedBottomRoutes = setOf(CurioRoutes.READER_DICTIONARY)
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

    // ── What's New waits for the INTRO and the TOUR (v406) ──────────────
    //
    // The highlights used to open themselves as a whole screen the moment the
    // app settled, and the boot routes counted as a quiet start — so on a fresh
    // install the page could land DURING the intro, before the member had even
    // met the app (member's request: "the whats new should be shown only after
    // the intro and as a drop down"). It is a SHEET over Home now, and it waits
    // for both halves of the welcome: the intro complete, and the tour offer
    // ANSWERED (taken and walked, or declined), so a member who has just been
    // asked "take a tiny tour?" is never handed a second thing at once.
    //
    // The full page is untouched: Settings ▸ Updates still opens it, and "See
    // all" on the sheet goes there. Whichever way the member leaves the sheet,
    // the version is stamped, so it is offered once and once only.
    var whatsNewSheet by remember { mutableStateOf(false) }
    var whatsNewOffered by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute, TourController.offerPending, TourController.active) {
        if (whatsNewOffered) return@LaunchedEffect
        val thisVersion = BuildConfig.VERSION_CODE
        if (AppPreferences.getWhatsNewSeenVersion(context) == thisVersion) return@LaunchedEffect
        if (whatsNewRelease(thisVersion) == null) return@LaunchedEffect
        // PAST THE INTRO. The boot gates are no longer a quiet start: a deep
        // link, a notification and a half-finished intro are all left alone.
        val route = currentRoute ?: return@LaunchedEffect
        if (route == CurioRoutes.SPLASH || route == CurioRoutes.ONBOARDING) return@LaunchedEffect
        if (!CurioOnboardingState.isComplete(context)) return@LaunchedEffect
        // AND PAST THE TOUR OFFER.
        if (TourController.offerPending || TourController.active) return@LaunchedEffect
        // Let the page settle, so the sheet reads as rising over Home rather
        // than racing the splash hand-off.
        delay(650)
        whatsNewOffered = true
        whatsNewSheet = true
    }

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
                    // v3xx — the "live notifications off" controller gap is
                    // gone (live notification is always on), so the
                    // bring-the-bubble-back fallback was removed.
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
    LaunchedEffect(
        currentRoute,
        PendingEntryOpen.trigger,
        PendingSpinOpen.trigger,
        PendingDirectMessageOpen.trigger,
        PendingCommunityOpen.trigger
    ) {
        val prefix = currentRoute?.substringBefore("/")
        // Wait for a stable root: null (first frame) and the boot gates own
        // navigation until the splash lands on HOME — the effect re-runs
        // there (keyed on currentRoute) and consumes the target once.
        if (prefix == null || prefix in CurioRoutes.bootGatePrefixes) return@LaunchedEffect
        // A MESSAGE notification tap — open that conversation (the people
        // page anchored beneath, so Back returns to the app).
        PendingDirectMessageOpen.take()?.let { (userId, handle) ->
            navController.navigate(CurioRoutes.directMessage(userId, handle)) {
                launchSingleTop = true
            }
            return@LaunchedEffect
        }
        // A COMMUNITY notification tap — land on the 24-hour wall, where the
        // post the notification is about actually is.
        if (PendingCommunityOpen.take()) {
            navController.navigateToTab(CurioRoutes.COMMUNITY)
            return@LaunchedEffect
        }
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
    // v3xx — the "Real blur (older devices)" + custom blur engine experiments
    // were REMOVED (toggles + code paths gone): pre-Android-12 pills use the
    // static veil, widgets use system blur.

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawerContent(
                onNavigate = { route ->
                    drawerScope.launch { drawerState.close() }
                    navController.navigate(route) { launchSingleTop = true }
                },
                // v444/v448 — the drawer's own state, so the star map can play itself
                // in and out WITH the panel (the TARGET is what says "on its way",
                // which the settled value would only say once it had arrived).
                //
                // The parameter is `drawerOpen` and not `open` because `open` is a
                // MODIFIER KEYWORD: the compiler takes `open = open` in the hero as a
                // modifier in expression position and reports the value as an
                // unresolved reference, so the flag is named for what it is.
                drawerOpen = drawerState.targetValue != DrawerValue.Closed
            )
        },
        gesturesEnabled = drawerState.isOpen || drawerState.isAnimationRunning
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(PetPointer.trackerModifier())
            .trackRevealTaps()
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
                        if ((showBottomBar && !wide) ||
                            routePrefix in fullBleedBottomRoutePrefixes ||
                            currentRoute in fullBleedBottomRoutes
                        ) {
                            Modifier
                        } else {
                            Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        }
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
        // ── What's New is offered from the ROOT, not here (v406) ────────
        // It used to open itself as a screen from inside this layout, which is
        // also why it could arrive during the intro. The gate (intro complete,
        // tour offer answered) and the sheet both live at the top of this
        // composable; see `whatsNewSheet` above.
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
                // Screen reveal owns the motion for this navigation — the old
                // frame is already frozen over the destination.
                if (CurioRevealHost.suppressDefaultTransition) EnterTransition.None
                else when {
                    // Settings-internal switches (hub ⇄ sections ⇄ drill-in
                    // tools): the rail's active pill MORPHS between chips
                    // (shared element) while the pages crossfade with a
                    // whisper of scale (0.985, calm spring) — the shared
                    // chrome reads as staying put, but the switch gets a
                    // gentle lift instead of a flat fade (v3xx).
                    isSettingsFamilyRoute(initialState) && isSettingsFamilyRoute(targetState) ->
                        // v3xx42 — PURE crossfade (no spring scale): the
                        // nav rail's active pill morphs between chips (shared
                        // element) and is the ONLY motion. A tween fade also
                        // converges cleanly when the user taps BACK mid-
                        // transition — a spring re-targeted from a mid-flight
                        // value is what left the old page stuck on the hub.
                        // (The old stiffness-750 Calm spring settled
                        // instantly — a solid-colour pop.)
                        fadeIn(animationSpec = tween(CurioMotion.Durations.Morph))
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
                    // v3xx — SOFTER page push: less travel (1/6 of the width
                    // instead of 1/4) and a touch slower (Deliberate, 500ms
                    // instead of Morph's 450) so the new page glides in
                    // beside the old one instead of snapping across a
                    // quarter-screen gap — the settings-family opens feel
                    // calm instead of quick.
                    // v440 — AND THE PUSH HAS ITS OWN CLOCK (see
                    // [CurioMotion.Durations.Push]): the travel is the same 1/6, the
                    // tempo is a quarter of a second instead of half one. The journal
                    // editor, a chapter and every other plain push are this branch, so
                    // this is the member's "the open animation of journal is clanky"
                    // fixed at its root.
                    else -> slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 6 },
                        animationSpec = tween(CurioMotion.Durations.Push, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(CurioMotion.Durations.Push))
                }
            },
            exitTransition = {
                if (CurioRevealHost.suppressDefaultTransition) ExitTransition.None
                else when {
                    // Settings-internal switches mirror the calm fade: the
                    // outgoing page's text + lower content fades out under
                    // the incoming page's fade-in (the shared chrome reads
                    // as staying still).
                    isSettingsFamilyRoute(initialState) && isSettingsFamilyRoute(targetState) ->
                        // v3xx42 — pure fade out (mirrors the enter fade).
                        fadeOut(animationSpec = tween(CurioMotion.Durations.Morph))
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
                    // v3xx — mirrors the softer push: the outgoing page drifts
                    // a touch (1/8) over the same slower slide.
                    else -> slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 8 },
                        animationSpec = tween(CurioMotion.Durations.Push, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(CurioMotion.Durations.Push))
                }
            },
            popEnterTransition = {
                if (CurioRevealHost.suppressDefaultTransition) EnterTransition.None
                else when {
                    // Popping back inside settings (section → hub, drill-in
                    // → section): the page underneath fades back in the same
                    // gentle crossfade as the forward switch.
                    isSettingsFamilyRoute(initialState) && isSettingsFamilyRoute(targetState) ->
                        // v3xx42 — PURE crossfade (no spring scale): the
                        // nav rail's active pill morphs between chips (shared
                        // element) and is the ONLY motion. A tween fade also
                        // converges cleanly when the user taps BACK mid-
                        // transition — a spring re-targeted from a mid-flight
                        // value is what left the old page stuck on the hub.
                        // (The old stiffness-750 Calm spring settled
                        // instantly — a solid-colour pop.)
                        fadeIn(animationSpec = tween(CurioMotion.Durations.Morph))
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
                        // Back navigation: slide right + fade (the softer,
                        // slower twin of the forward push).
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 8 },
                            animationSpec = tween(CurioMotion.Durations.Pop, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(CurioMotion.Durations.Pop))
                    }
                }
            },
            popExitTransition = {
                if (CurioRevealHost.suppressDefaultTransition) ExitTransition.None
                else when {
                    // Popping back inside settings: the outgoing page fades
                    // out over the same crossfade.
                    isSettingsFamilyRoute(initialState) && isSettingsFamilyRoute(targetState) ->
                        // v3xx42 — pure fade out (mirrors the enter fade).
                        fadeOut(animationSpec = tween(CurioMotion.Durations.Morph))
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
                        // Pop exit: slide right + fade out (mirrors the
                        // softer back-slide of the page underneath).
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth / 8 },
                            animationSpec = tween(CurioMotion.Durations.Pop, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(CurioMotion.Durations.Pop))
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
                // nav) is the only picker now (the classic glass-pill picker
                // experiment was fully removed).
                CategoryPickerBrowseScreen(navController = navController)
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

            // ── v387 — the personal writing family ────────────────────────
            // Journals are a collection of their own (a page per day) and
            // books a shelf of their own; neither borrows the saved-entry
            // detail view, because neither has a topic behind it.
            composable(route = CurioRoutes.JOURNALS) {
                JournalListScreen(navController = navController)
            }
            composable(
                route = CurioRoutes.JOURNAL_EDITOR,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { entry ->
                // The photo viewer rides the page it was opened from, drawn over
                // it by this route, so a tapped picture grows out of the page
                // instead of pushing a whole Lightbox screen on top of it.
                val photos = rememberPersonalPhotoOverlayState()
                Box(Modifier.fillMaxSize()) {
                    JournalEditorScreen(
                        navController = navController,
                        entryIdArg = entry.arguments?.getString("entryId").orEmpty(),
                        photos = photos
                    )
                    PersonalPhotoOverlay(photos)
                }
            }
            // v389 — a note about a topic and a to-do list are their OWN pages:
            // a journal day wears a date bar and a mood pill, and neither of
            // those belongs on a page about a topic or on a list of things to do.
            composable(
                route = CurioRoutes.TOPIC_NOTE,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { entry ->
                val photos = rememberPersonalPhotoOverlayState()
                Box(Modifier.fillMaxSize()) {
                    TopicNoteScreen(
                        navController = navController,
                        entryIdArg = entry.arguments?.getString("entryId").orEmpty(),
                        initialTopicId = entry.arguments?.getString("topicId").orEmpty(),
                        initialTopicName = entry.arguments?.getString("topicName").orEmpty(),
                        initialCategoryId = entry.arguments?.getString("categoryId").orEmpty(),
                        photos = photos
                    )
                    PersonalPhotoOverlay(photos)
                }
            }
            composable(
                route = CurioRoutes.TODO,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { entry ->
                val photos = rememberPersonalPhotoOverlayState()
                Box(Modifier.fillMaxSize()) {
                    TodoScreen(
                        navController = navController,
                        entryIdArg = entry.arguments?.getString("entryId").orEmpty(),
                        photos = photos
                    )
                    PersonalPhotoOverlay(photos)
                }
            }
            composable(route = CurioRoutes.BOOKS) {
                BookShelfScreen(navController = navController)
            }
            // v389 — INCURSION, the hidden viewing order. Registered like any
            // other push destination so it can arrive and leave with the app's
            // own transition, and deliberately NOT a bottom-nav prefix: the
            // page draws its own bar at its own foot.
            composable(route = CurioRoutes.INCURSION) {
                com.curio.app.features.incursion.IncursionScreen(navController = navController)
            }
            composable(
                route = CurioRoutes.BOOK_DETAIL,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { entry ->
                BookDetailScreen(
                    navController = navController,
                    bookId = entry.arguments?.getString("bookId").orEmpty()
                )
            }
            // A chapter is its own page (the journal's shape), so the review
            // is written on a page rather than inside the shelf's list.
            composable(
                route = CurioRoutes.BOOK_READER,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { entry ->
                BookReaderScreen(
                    navController = navController,
                    bookId = entry.arguments?.getString("bookId").orEmpty()
                )
            }
            // v389 — a book's OWN review: one page for the whole book, with a
            // floating door that drops a chapter marker into it.
            composable(
                route = CurioRoutes.BOOK_REVIEW,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { entry ->
                val photos = rememberPersonalPhotoOverlayState()
                Box(Modifier.fillMaxSize()) {
                    BookReviewScreen(
                        navController = navController,
                        bookId = entry.arguments?.getString("bookId").orEmpty(),
                        photos = photos
                    )
                    PersonalPhotoOverlay(photos)
                }
            }
            composable(
                route = CurioRoutes.CHAPTER,
                arguments = listOf(
                    navArgument("bookId") { type = NavType.StringType },
                    navArgument("chapter") { type = NavType.IntType }
                )
            ) { entry ->
                val photos = rememberPersonalPhotoOverlayState()
                Box(Modifier.fillMaxSize()) {
                    ChapterScreen(
                        navController = navController,
                        bookId = entry.arguments?.getString("bookId").orEmpty(),
                        chapter = entry.arguments?.getInt("chapter") ?: 1,
                        photos = photos
                    )
                    PersonalPhotoOverlay(photos)
                }
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
            // v444 — the identity editor, as a PAGE of its own (it was a dialog
            // on the profile page). A plain forward push, so it arrives with the
            // nav host's own calm slide + fade and needs nothing special here.
            composable(CurioRoutes.PROFILE_EDIT) {
                ProfileEditScreen(navController = navController)
            }
            composable(CurioRoutes.QUESTS) {
                QuestsScreen(navController = navController)
            }
            composable(CurioRoutes.STATS) {
                StatsScreen(navController = navController)
            }
            composable(CurioRoutes.SETTINGS) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    SettingsHubScreen(navController = navController)
                }
            }
            composable(CurioRoutes.SETTINGS_APPEARANCE) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    SettingsSectionScreen(navController = navController, page = SettingsPage.APPEARANCE)
                }
            }
            composable(CurioRoutes.SETTINGS_PREFERENCES) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    SettingsSectionScreen(navController = navController, page = SettingsPage.PREFERENCES)
                }
            }
            composable(CurioRoutes.SETTINGS_RECORDING) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    SettingsSectionScreen(navController = navController, page = SettingsPage.RECORDING)
                }
            }
            // v461 — Advanced, registered like every other settings section so it
            // arrives with the settings family's own shared-element motion (see
            // `SettingsSharedScope`).
            composable(CurioRoutes.SETTINGS_ADVANCED) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    SettingsSectionScreen(navController = navController, page = SettingsPage.ADVANCED)
                }
            }
            composable(CurioRoutes.SETTINGS_DATA) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    BackupToolsScreen(navController = navController)
                }
            }
            composable(CurioRoutes.SETTINGS_ONLINE) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    OnlineModeScreen(navController = navController)
                }
            }
            composable(CurioRoutes.SETTINGS_PRIVACY) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    PrivacyScreen(navController = navController)
                }
            }
composable(CurioRoutes.COMMUNITY) {
        CommunityScreen(navController = navController)
    }
    composable(CurioRoutes.MODERATION) {
        ModerationScreen(navController = navController)
    }

            composable(
                route = CurioRoutes.COMMUNITY_CARD,
                arguments = listOf(navArgument("cardId") { type = NavType.StringType })
            ) { backStackEntry ->
                SocialSharedScope(sharedTransitionScope, this) {
                    CommunityCardScreen(
                        navController = navController,
                        cardId = backStackEntry.arguments?.getString("cardId").orEmpty()
                    )
                }
            }
            // v3xx53 — a member's public profile, reached from a card, a reply,
            // a friend row or a conversation.
            composable(
                route = CurioRoutes.SOCIAL_PROFILE,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                SocialSharedScope(sharedTransitionScope, this) {
                    SocialProfileScreen(
                        navController = navController,
                        userId = backStackEntry.arguments?.getString("userId").orEmpty()
                    )
                }
            }
            composable(CurioRoutes.FRIENDS) {
                FriendsScreen(navController = navController)
            }
            composable(CurioRoutes.CHATS) {
                ChatsScreen(navController = navController)
            }
            composable(
                route = CurioRoutes.DIRECT_MESSAGE,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    // What the CALLER already knows this person as — the header
                    // shows it on the first frame instead of a placeholder.
                    navArgument("handle") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                SocialSharedScope(sharedTransitionScope, this) {
                    DirectMessageScreen(
                        navController = navController,
                        otherUserId = backStackEntry.arguments?.getString("userId").orEmpty(),
                        handle = backStackEntry.arguments?.getString("handle").orEmpty()
                    )
                }
            }
            composable(CurioRoutes.SETTINGS_BOOK_COVER) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    BookCoverHubScreen(navController = navController)
                }
            }
            composable(CurioRoutes.SETTINGS_BOOK_BROWSER) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    com.curio.app.features.settings.BookBrowserScreen(navController = navController)
                }
            }
            composable(CurioRoutes.SHARE_HUB) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    ShareHubScreen(navController = navController)
                }
            }
            composable(CurioRoutes.EXPERIMENTS) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    ExperimentsScreen(navController = navController)
                }
            }
            // v431 — the reader's own settings (its own paper and ink; see
            // ReaderSettingsScreen.kt). Wired here for the settings side and
            // normally reached from the reader's ⋯ menu instead.
            composable(CurioRoutes.READER_SETTINGS) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    com.curio.app.features.personal.ReaderSettingsRoute(
                        onBack = { navController.popBackStack() },
                        // v469 — and its own door to the read-aloud page, which is a
                        // destination rather than a section now (see the route below).
                        onReadAloud = { navController.navigate(CurioRoutes.READ_ALOUD_SETTINGS) }
                    )
                }
            }
            // ── v469 — THE READ-ALOUD SETTINGS, ON A PAGE OF THEIR OWN ──────
            //
            // The same page the reader opens OVER the book (see
            // `ReadAloudSettingsScreen.kt`): the settings side's door is the Reading
            // page's own row, and this is where that row goes.
            composable(CurioRoutes.READ_ALOUD_SETTINGS) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    com.curio.app.features.personal.ReadAloudSettingsRoute(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            // v449 — the dictionary's own page: a lookup is not tied to the book in
            // front of the member, so the reader's ⋯ menu (and Home's "+") open a
            // PAGE rather than the reader's own sheet. Taller, search-first, and it
            // wears the reader's paper and ink like the reader's settings do.
            composable(CurioRoutes.READER_DICTIONARY) {
                com.curio.app.features.personal.ReaderDictionaryPage(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(CurioRoutes.USER_EXPERIMENTS) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    UserExperimentsScreen(navController = navController)
                }
            }
            composable(CurioRoutes.GLASS_WIDGET_EDITOR) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    com.curio.app.features.settings.WidgetEditorScreen(navController = navController)
                }
            }
            composable(CurioRoutes.MANAGE_CATEGORIES) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    ManageCategoriesScreen(navController = navController)
                }
            }
            composable(CurioRoutes.TOPIC_HISTORY) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    TopicHistoryScreen(navController = navController)
                }
            }
            composable(CurioRoutes.RECYCLE_BIN) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    RecycleBinScreen(navController = navController)
                }
            }
            composable(CurioRoutes.RECENTS_ALL) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    RecentScreen(navController = navController)
                }
            }
            composable(CurioRoutes.CRASH) {
                CurioCrashScreen(navController = navController)
            }
            composable(CurioRoutes.BUG_REPORT) {
                BugReportScreen(navController = navController)
            }
            composable(CurioRoutes.SUPPORT) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    SupportScreen(navController = navController)
                }
            }
            composable(CurioRoutes.UPDATES) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    UpdatesScreen(navController = navController)
                }
            }
            // v403 — What's New: the release's own highlights, one door each.
            composable(CurioRoutes.WHATS_NEW) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    WhatsNewScreen(navController = navController)
                }
            }
            composable(CurioRoutes.DATABASE) {
                TopicDatabaseScreen(navController = navController)
            }
            composable(CurioRoutes.FIELDMIND_OBSERVATION) {
                FieldMindObservationScreen(navController = navController)
            }
            composable(CurioRoutes.PET_DESIGNER) {
                SettingsSharedScope(sharedTransitionScope, this) {
                    PetDesignerScreen(navController = navController)
                }
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

    // ── The highlights themselves, once the welcome is over (v406) ────────
    // Rises over Home, so it reads as the version saying hello rather than as
    // a settings page opening on its own. "See all" keeps the full screen.
    if (whatsNewSheet) {
        val stampSeen = {
            AppPreferences.setWhatsNewSeenVersion(context, BuildConfig.VERSION_CODE)
            whatsNewSheet = false
        }
        WhatsNewSheet(
            versionCode = BuildConfig.VERSION_CODE,
            onOpenAll = {
                stampSeen()
                navController.navigate(CurioRoutes.WHATS_NEW) { launchSingleTop = true }
            },
            onDismiss = { stampSeen() }
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

    // v389 — a voice note still being recorded but whose page is not the one on
    // screen: a small pill at the root says so and takes the member back to it.
    // Drawn here (like the pet) so it is above every screen, and it hides
    // itself the moment its own page is composed.
    PersonalVoicePill(navController = navController)

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
                                tint = curioAccentInk(),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // ── v470 — "COMPLETED" IN THE BACK-TO-APP DIALOG ──
                        // The member: *"when going back show a completed in the dialog
                        // box"*. This is the dialog they come back to, so it now
                        // offers the answer in their own word: the topic is marked
                        // completed (the record Topic History's Completed list
                        // reads, plus the done mark) and the session ends quietly —
                        // no write-it-down page. "Express yourself" is still the door
                        // for a member who wants to write, and it completes the topic
                        // too, because finishing an explore IS completing it.
                        TextButton(
                            onClick = {
                                showDoneDialog = false
                                confirmSessionCancel = false
                                activeSession.markCompleted(context)
                                ExploreSessionStore.clearSession(context)
                                ExploreReminderScheduler.cancel(context)
                                ExploreSessionService.stop(context)
                                activeSession.let { dialogDismissedFor = it.startMillis }
                            },
                            colors = curioDialogActionButtonColors()
                        ) { Text("Completed") }
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
                            // v470 — writing it down means it is finished: the same
                            // completed mark "Completed" writes.
                            activeSession.markCompleted(context)
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

    // ── The feedback form's sheet (v403) ─────────────────────────────────
    // Mounted at the NavHost root, like the drawer, so the ONE sheet is shared
    // by every door into it (Home's card, Support's card and row) instead of
    // each screen hosting its own copy.
    if (FeedbackFormState.open) {
        FeedbackFormState.liveForm?.let { form ->
            FeedbackFormSheet(form = form, onDismiss = { FeedbackFormState.dismissSheet() })
        }
    }
}


